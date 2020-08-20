package org.cryptomator.cli.frontend;

import java.nio.file.Path;
import java.util.ArrayList;

import org.cryptomator.frontend.webdav.WebDavServer;
import org.cryptomator.frontend.webdav.servlet.WebDavServletController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebDav {
	private static final Logger LOG = LoggerFactory.getLogger(WebDav.class);

	private final WebDavServer server;
	private ArrayList<WebDavServletController> servlets;

	public WebDav(String bindAddr, int port) {
		servlets = new ArrayList<>();
		server = WebDavServer.create();
		server.bind(bindAddr, port);
		server.start();
		LOG.info("WebDAV server started: {}:{}", bindAddr, port);
	}

	public void stop() {
		for (WebDavServletController controller : servlets) {
			controller.stop();
		}
		server.stop();
	}

	public void addServlet(Path vaultRoot, String vaultName) {
		WebDavServletController servlet = server.createWebDavServlet(vaultRoot, vaultName);
		servlets.add(servlet);
		servlet.start();
	}
}
