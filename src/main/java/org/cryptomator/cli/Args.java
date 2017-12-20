/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.cli;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.cryptomator.cli.pwd.PasswordFromFileStrategy;
import org.cryptomator.cli.pwd.PasswordFromStdInputStrategy;
import org.cryptomator.cli.pwd.PasswordStrategy;
import org.cryptomator.cli.pwd.PasswordFromPropertyStrategy;

/**
 * Parses program arguments. Does not validate them.
 */
public class Args {

	private static final String USAGE = "java -jar cryptomator-cli.jar" //
			+ " --bind localhost --port 8080" //
			+ " --vault mySecretVault=/path/to/vault --password mySecretVault=FooBar3000" //
			+ " --vault myOtherVault=/path/to/other/vault --password myOtherVault=BarFoo4000" //
			+ " --vault myThirdVault=/path/to/third/vault --passwordfile myThirdVault=/path/to/passwordfile";
	private static final Options OPTIONS = new Options();
	static {
		OPTIONS.addOption(Option.builder() //
				.longOpt("bind") //
				.argName("WebDAV bind address") //
				.desc("TCP socket bind address of the WebDAV server. Use 0.0.0.0 to accept all incoming connections.") //
				.hasArg() //
				.build());
		OPTIONS.addOption(Option.builder() //
				.longOpt("port") //
				.argName("WebDAV port") //
				.desc("TCP port, the WebDAV server should listen on.") //
				.hasArg() //
				.build());
		OPTIONS.addOption(Option.builder() //
				.longOpt("vault") //
				.argName("Path of a vault") //
				.desc("Format must be vaultName=/path/to/vault") //
				.valueSeparator() //
				.hasArgs() //
				.build());
		OPTIONS.addOption(Option.builder() //
				.longOpt("password") //
				.argName("Password of a vault") //
				.desc("Format must be vaultName=password") //
				.valueSeparator() //
				.hasArgs() //
				.build());
		OPTIONS.addOption(Option.builder() //
				.longOpt("passwordfile") //
				.argName("Passwordfile for a vault") //
				.desc("Format must be vaultName=passwordfile") //
				.valueSeparator() //
				.hasArgs() //
				.build());
	}

	private final String bindAddr;
	private final int port;
	private final Properties vaultPaths;
	private final Properties vaultPasswords;
	private final Properties vaultPasswordFiles;
	private final Map<String, PasswordStrategy> passwordStrategies;

	public Args(CommandLine commandLine) throws ParseException {
		this.bindAddr = commandLine.getOptionValue("bind", "localhost");
		this.port = Integer.parseInt(commandLine.getOptionValue("port", "0"));
		this.vaultPaths = commandLine.getOptionProperties("vault");
		this.vaultPasswords = commandLine.getOptionProperties("password");
		this.vaultPasswordFiles = commandLine.getOptionProperties("passwordfile");
		this.passwordStrategies = new HashMap<>();
	}

	public String getBindAddr() {
		return bindAddr;
	}

	public int getPort() {
		return port;
	}

	public Set<String> getVaultNames() {
		return vaultPaths.keySet().stream().map(String.class::cast).collect(Collectors.toSet());
	}

	public String getVaultPath(String vaultName) {
		return vaultPaths.getProperty(vaultName);
	}

	public static Args parse(String[] arguments) throws ParseException {
		CommandLine commandLine = new DefaultParser().parse(OPTIONS, arguments);
		return new Args(commandLine);
	}

	public static void printUsage() {
		new HelpFormatter().printHelp(USAGE, OPTIONS);
	}

	public PasswordStrategy addPasswortStrategy(final String vaultName) {
		PasswordStrategy passwordStrategy = new PasswordFromStdInputStrategy(vaultName);

		if (vaultPasswords.getProperty(vaultName) != null) {
			passwordStrategy = new PasswordFromPropertyStrategy(
					vaultName,
					vaultPasswords.getProperty(vaultName)
			);
		} else if (vaultPasswordFiles.getProperty(vaultName) != null) {
			passwordStrategy = new PasswordFromFileStrategy(
					vaultName,
					Paths.get(vaultPasswordFiles.getProperty(vaultName))
			);
		}

		this.passwordStrategies.put(vaultName, passwordStrategy);
		return passwordStrategy;
	}

	public PasswordStrategy getPasswordStrategy(final String vaultName) {
		return passwordStrategies.get(vaultName);
	}
}
