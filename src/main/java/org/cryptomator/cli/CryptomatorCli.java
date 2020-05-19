/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.apache.commons.cli.ParseException;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.frontend.webdav.WebDavServer;
import org.cryptomator.frontend.webdav.servlet.WebDavServletController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CryptomatorCli {

	private static final Logger LOG = LoggerFactory.getLogger(CryptomatorCli.class);

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
		if (args.getPort() < 0 || args.getPort() > 65536) {
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
		}
	}

	private static void startup(Args args) throws IOException {
		WebDavServer server = WebDavServer.create();
		server.bind(args.getBindAddr(), args.getPort());
		server.start();

		for (String vaultName : args.getVaultNames()) {
			Path vaultPath = Paths.get(args.getVaultPath(vaultName));
			LOG.info("Unlocking vault \"{}\" located at {}", vaultName, vaultPath);
			String vaultPassword = args.getPasswordStrategy(vaultName).password();
			CryptoFileSystemProperties properties = CryptoFileSystemProperties.cryptoFileSystemProperties().withPassphrase(vaultPassword).build();
			Path vaultRoot = CryptoFileSystemProvider.newFileSystem(vaultPath, properties).getPath("/");
			WebDavServletController servlet = server.createWebDavServlet(vaultRoot, vaultName);
			servlet.start();
		}

		waitForShutdown(() -> {
			LOG.info("Shutting down...");
			try {
				server.stop();
				LOG.info("Shutdown successful.");
			} catch (Throwable e) {
				LOG.error("Error during shutdown", e);
			}
		});
	}

	private static void waitForShutdown(Runnable runnable) {
		Runtime.getRuntime().addShutdownHook(new Thread(runnable));
		LOG.info("Server started. Press Ctrl+C to terminate.");
	}

}
