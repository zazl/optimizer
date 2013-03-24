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
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.FileResource;

public class ZazlOptimizerServer {
	private Server server = new Server();
	private File root = null;
	private File lib = null;
	private int port = 8080;
	private boolean compress = true;
	
	public ZazlOptimizerServer(File root, File lib, int port, boolean compress) {
		this.root = root;
		this.lib = lib;
		this.port = port;
		this.compress = compress;
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
		boolean useRhinoAST = Boolean.valueOf(System.getProperty("rhinoAST", "true"));
		JSOptimizerFactory jsOptimizerFactory = null;
		if (useV8) {
			jsOptimizerFactory = new org.dojotoolkit.optimizer.amd.v8.AMDJSOptimizerFactory();
		} else if (useRhinoAST) {
			jsOptimizerFactory = new org.dojotoolkit.optimizer.amd.rhinoast.AMDJSOptimizerFactory();
		} else {
			jsOptimizerFactory = new org.dojotoolkit.optimizer.amd.rhino.AMDJSOptimizerFactory();
		}
		System.out.println("Using jsOptimizerFactory ["+jsOptimizerFactory.getClass().getName()+"]");

		JSCompressorFactory jsCompressorFactory = null;
		if (compress) {
			jsCompressorFactory = new JSCompressorFactoryImpl();
		}
		MultiRootResourceLoader resourceLoader = new MultiRootResourceLoader(new File[] {root, lib});
		RhinoClassLoader rhinoClassLoader = new RhinoClassLoader(resourceLoader);

		File tempDir = new File("./tmp");
		if (!tempDir.exists()) {
			tempDir.mkdirs();
		}
		JSContentHandler jsContentHandler = new JSContentHandler(resourceLoader, jsOptimizerFactory, rhinoClassLoader, jsCompressorFactory, tempDir);
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
				Boolean compress = Boolean.TRUE;
				if (args.length > 2) {
					compress = Boolean.valueOf(args[2]);
				}
				File lib = null;
				if (args.length > 3) {
					lib = new File(args[3]);
				} else {
					lib = new File("./server/jslib");
				}
				if (!lib.exists()) {
					System.out.println("Unable to locate the jslib directory ["+lib.getPath()+"]");
					return;
				}
				ZazlOptimizerServer server = new ZazlOptimizerServer(root, lib, port, compress);
				server.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.println("optimizerserver <resource directory> <port> <compress> <jslib directory>");
		}
	}
}
