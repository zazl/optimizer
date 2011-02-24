/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.servlet;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dojotoolkit.compressor.JSCompressorFactory;
import org.dojotoolkit.compressor.JSCompressorFactoryImpl;
import org.dojotoolkit.optimizer.JSOptimizer;
import org.dojotoolkit.optimizer.JSOptimizerFactory;
import org.dojotoolkit.optimizer.JSOptimizerFactoryImpl;
import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;

public class JSServlet extends HttpServlet {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer");
	private static final long serialVersionUID = 1L;
	private static final String[] ignoreList = new String[] {"/dojo/dojo.js", "^/optimizer/", "^/uglifyjs/", ".*/nls/.*"};
	public static final String[] bootstrapModules = new String[] {"/dojo/dojo.js", "/dojo/i18n.js"};
	public static final String[] debugBootstrapModules = new String[] {"/dojo/dojo.js.uncompressed.js", "/dojo/i18n.js"};

	private JSHandler jsHandler = null;
	protected JSOptimizer jsOptimizer = null;
	protected ResourceLoader resourceLoader = null;
	protected RhinoClassLoader rhinoClassLoader = null;
	
	public JSServlet() {
		jsHandler = new JSHandler();
	}
	
	public JSServlet(ResourceLoader resourceLoader, JSOptimizer jsOptimizer, RhinoClassLoader rhinoClassLoader) {
		this();
		this.jsOptimizer = jsOptimizer;
		this.resourceLoader = resourceLoader;
		this.rhinoClassLoader = rhinoClassLoader;
	}
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		if (resourceLoader == null) {
			resourceLoader = (ResourceLoader)getServletContext().getAttribute("org.dojotoolkit.ResourceLoader");
			if (resourceLoader == null) {
				JSCompressorFactory jsCompressorFactory = null;
				String compressJS = getServletContext().getInitParameter("compressJS");
				if (compressJS != null && compressJS.equalsIgnoreCase("true")) {
					jsCompressorFactory = new JSCompressorFactoryImpl();
				}
				resourceLoader = new ServletResourceLoader(getServletContext(), jsCompressorFactory, ignoreList);
				getServletContext().setAttribute("org.dojotoolkit.ResourceLoader", resourceLoader);
			}
		}
		if (rhinoClassLoader == null) {
			rhinoClassLoader = (RhinoClassLoader)getServletContext().getAttribute("org.dojotoolkit.RhinoClassLoader");
			if (rhinoClassLoader == null) {
				rhinoClassLoader = new RhinoClassLoader(resourceLoader);
				getServletContext().setAttribute("org.dojotoolkit.RhinoClassLoader", rhinoClassLoader);
			}
		}
		boolean javaChecksum = false;
		String javaChecksumString = getServletContext().getInitParameter("javaChecksum");
		if (javaChecksumString != null) {
			javaChecksum = Boolean.valueOf(javaChecksumString);
		}
		if (jsOptimizer == null) {
			jsOptimizer = (JSOptimizer)getServletContext().getAttribute("org.dojotoolkit.optimizer.JSOptimizer");
			if (jsOptimizer == null) {
				JSOptimizerFactory jsOptimizerFactory = new JSOptimizerFactoryImpl();
				jsOptimizer = jsOptimizerFactory.createJSOptimizer(resourceLoader, rhinoClassLoader, javaChecksum);
				logger.log(Level.FINE, getClass().getName(), "Using JSOptimizer of type ["+jsOptimizer.getClass().getName()+"]");
			}
		}
		getServletContext().setAttribute("org.dojotoolkit.optimizer.JSOptimizer", jsOptimizer);
		jsHandler.initialize(resourceLoader, jsOptimizer, bootstrapModules, debugBootstrapModules);
	}
	
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		jsHandler.handle(request, response);
	}
}
