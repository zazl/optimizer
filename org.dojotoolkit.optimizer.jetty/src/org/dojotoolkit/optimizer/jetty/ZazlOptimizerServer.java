/*
    Copyright (c) 2004-2012, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.jetty;

import java.io.File;
import java.net.URL;

import org.dojotoolkit.compressor.JSCompressorFactory;
import org.dojotoolkit.compressor.JSCompressorFactoryImpl;
import org.dojotoolkit.optimizer.JSOptimizerFactory;
import org.dojotoolkit.server.util.resource.MultiRootResourceLoader;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.resource.FileResource;

public class ZazlOptimizerServer {
	private Server server = new Server();
	private File root = null;
	private File lib = null;
	private int port = 8080;
	
	public ZazlOptimizerServer(File root, File lib, int port) {
		this.root = root;
		this.lib = lib;
		this.port = port;
	}

	public void start() throws Exception {
		server = new Server();
		Connector connector = new SocketConnector();
		connector.setPort(port);
		server.setConnectors(new Connector[]{connector});

		HandlerList handlerList = new HandlerList(); 
		
		ResourceHandler rootHandler = new ResourceHandler();
		rootHandler.setBaseResource(new FileResource(new URL("file:"+root.getCanonicalPath())));
		ResourceHandler libHandler = new ResourceHandler();
		libHandler.setBaseResource(new FileResource(new URL("file:"+lib.getCanonicalPath())));
		
		boolean useV8 = Boolean.valueOf(System.getProperty("V8", "false"));
		JSOptimizerFactory jsOptimizerFactory = null;
		if (useV8) {
			jsOptimizerFactory = new org.dojotoolkit.optimizer.amd.v8.AMDJSOptimizerFactory();
		} else {
			jsOptimizerFactory = new org.dojotoolkit.optimizer.amd.rhino.AMDJSOptimizerFactory();
		}
		JSCompressorFactory jsCompressorFactory = new JSCompressorFactoryImpl();
		MultiRootResourceLoader resourceLoader = new MultiRootResourceLoader(new File[] {root, lib});
		RhinoClassLoader rhinoClassLoader = new RhinoClassLoader(resourceLoader);

		File tempDir = new File("./tmp");
		if (!tempDir.exists()) {
			tempDir.mkdirs();
		}
		JSContentHandler jsContentHandler = new JSContentHandler(resourceLoader, jsOptimizerFactory, rhinoClassLoader, jsCompressorFactory, new File("./tmp"));
		handlerList.addHandler(jsContentHandler);
		handlerList.addHandler(rootHandler);
		handlerList.addHandler(libHandler);
		
		server.setHandler(handlerList);
		server.start();
		System.out.println("Server started and available on port : "+port);
		System.out.println("Loading from ["+root+"]");
	}
	
	public void stop() throws Exception {
		server.stop();
	}

	public static void main(String[] args) {
		if (args.length > 0) {
			try {
				File root = new File(args[0]);
				if (!root.exists()) {
					System.out.println("Unable to locate root directory ["+root.getPath()+"]");
					return;
				}
				int port = 8080;
				if (args.length > 1) {
					try {
						port = Integer.valueOf(args[1]);
					} catch (Exception e) {
						System.out.println("Unable to use port number ["+args[1]+"] it must be a valid number");
					}
				}
				File lib = null;
				if (args.length > 2) {
					lib = new File(args[2]);
				} else {
					lib = new File("./server/jslib");
				}
				if (!lib.exists()) {
					System.out.println("Unable to locate the jslib directory ["+lib.getPath()+"]");
					return;
				}
				ZazlOptimizerServer server = new ZazlOptimizerServer(root, lib, port);
				server.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.println("optimizerserver <resource directory> <port> <jslib directory>");
		}
	}
}