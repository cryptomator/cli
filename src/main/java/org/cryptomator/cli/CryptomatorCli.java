/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.cli;

import org.cryptomator.cli.frontend.FuseMount;
import org.cryptomator.cli.frontend.WebDav;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import com.google.common.base.Preconditions;
import org.apache.commons.cli.ParseException;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CryptomatorCli {

	private static final Logger LOG = LoggerFactory.getLogger(CryptomatorCli.class);

	private static final byte[] PEPPER = new byte[0];
	private static final String SCHEME = "masterkeyfile";

	public static void main(String[] rawArgs) throws IOException {
		try {
			Args args = Args.parse(rawArgs);
			validate(args);
			startup(args);
		} catch (ParseException e) {
			LOG.error("Invalid or missing arguments", e);
			Args.printUsage();
		} catch (IllegalArgumentException e) {
			LOG.error(e.getMessage());
			Args.printUsage();
		}
	}

	private static void validate(Args args) throws IllegalArgumentException {
		Set<String> vaultNames = args.getVaultNames();
		if (args.hasValidWebDavConf() && (args.getPort() < 0 || args.getPort() > 65536)) {
			throw new IllegalArgumentException("Invalid WebDAV Port.");
		}

		if (vaultNames.size() == 0) {
			throw new IllegalArgumentException("No vault specified.");
		}

		for (String vaultName : vaultNames) {
			Path vaultPath = Paths.get(args.getVaultPath(vaultName));
			if (!Files.isDirectory(vaultPath)) {
				throw new IllegalArgumentException("Not a directory: " + vaultPath);
			}
			args.addPasswortStrategy(vaultName).validate();

			Path mountPoint = args.getFuseMountPoint(vaultName);
			if (mountPoint != null && !Files.isDirectory(mountPoint)) {
				throw new IllegalArgumentException("Fuse mount point does not exist: " + mountPoint);
			}
		}
	}

	private static void startup(Args args) throws IOException {
		Optional<WebDav> server = initWebDavServer(args);
		ArrayList<FuseMount> mounts = new ArrayList<>();

		SecureRandom secureRandom;
		try {
			secureRandom = SecureRandom.getInstanceStrong();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("A strong algorithm must exist in every Java platform.", e);
		}
		MasterkeyFileAccess masterkeyFileAccess = new MasterkeyFileAccess(PEPPER, secureRandom);

		for (String vaultName : args.getVaultNames()) {
			Path vaultPath = Paths.get(args.getVaultPath(vaultName));
			LOG.info("Unlocking vault \"{}\" located at {}", vaultName, vaultPath);
			String vaultPassword = args.getPasswordStrategy(vaultName).password();
			CryptoFileSystemProperties properties = CryptoFileSystemProperties.cryptoFileSystemProperties()
					.withKeyLoader(keyId -> {
						Preconditions.checkArgument(SCHEME.equalsIgnoreCase(keyId.getScheme()), "Only supports keys with scheme " + SCHEME);
						Path keyFilePath = vaultPath.resolve(keyId.getSchemeSpecificPart());
						return masterkeyFileAccess.load(keyFilePath, vaultPassword);
					})
					.build();

			Path vaultRoot = CryptoFileSystemProvider.newFileSystem(vaultPath, properties).getPath("/");

			Path fuseMountPoint = args.getFuseMountPoint(vaultName);
			if (fuseMountPoint != null) {
				String mountFlags = args.fuseMountFlags(vaultName);
				FuseMount newMount = new FuseMount(vaultRoot, fuseMountPoint, mountFlags);
				if (newMount.mount()) {
					mounts.add(newMount);
				}
			}

			server.ifPresent(serv -> serv.addServlet(vaultRoot, vaultName));
		}

		waitForShutdown(() -> {
			LOG.info("Shutting down...");
			try {
				server.ifPresent(serv -> serv.stop());

				for (FuseMount mount : mounts) {
					mount.unmount();
				}
				LOG.info("Shutdown successful.");
			} catch (Throwable e) {
				LOG.error("Error during shutdown", e);
			}
		});
	}

	private static Optional<WebDav> initWebDavServer(Args args) {
		Optional<WebDav> server = Optional.empty();
		if (args.hasValidWebDavConf()) {
			server = Optional.of(new WebDav(args.getBindAddr(), args.getPort()));
		}
		return server;
	}

	private static void waitForShutdown(Runnable runnable) {
		Runtime.getRuntime().addShutdownHook(new Thread(runnable));
		LOG.info("Press Ctrl+C to terminate.");

		// Block the main thread infinitely as otherwise when using
		// Fuse mounts the application quits immediately.
		try {
			Object mainThreadBlockLock = new Object();
			synchronized (mainThreadBlockLock) {
				while (true) {
					mainThreadBlockLock.wait();
				}
			}
		} catch (Exception e) {
			LOG.error("Main thread blocking failed.");
		}
	}
}
