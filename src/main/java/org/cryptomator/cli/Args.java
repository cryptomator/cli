/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.cli;

import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Parses program arguments. Does not validate them.
 */
public class Args {

	private static final String USAGE = "java -jar cryptomator-cli.jar" //
			+ " --vault mySecretVault=/path/to/vault --password mySecretVault=FooBar3000" //
			+ " --vault myOtherVault=/path/to/other/vault --password myOtherVault=BarFoo4000";
	private static final Options OPTIONS = new Options();
	static {
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
	}

	private final int port;
	private final Properties vaultPaths;
	private final Properties vaultPasswords;

	public Args(CommandLine commandLine) throws ParseException {
		this.port = Integer.parseInt(commandLine.getOptionValue("port", "0"));
		this.vaultPaths = commandLine.getOptionProperties("vault");
		this.vaultPasswords = commandLine.getOptionProperties("password");
	}

	public int getPort() {
		return port;
	}

	public Set<String> getVaultNames() {
		return vaultPaths.keySet().stream().filter(vaultPasswords::containsKey).map(String.class::cast).collect(Collectors.toSet());
	}

	public String getVaultPath(String vaultName) {
		return vaultPaths.getProperty(vaultName);
	}

	public String getVaultPassword(String vaultName) {
		return vaultPasswords.getProperty(vaultName);
	}

	public static Args parse(String[] arguments) throws ParseException {
		CommandLine commandLine = new DefaultParser().parse(OPTIONS, arguments);
		return new Args(commandLine);
	}

	public static void printUsage() {
		new HelpFormatter().printHelp(USAGE, OPTIONS);
	}

}
