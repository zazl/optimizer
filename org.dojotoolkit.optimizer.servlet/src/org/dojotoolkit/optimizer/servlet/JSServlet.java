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
import org.dojotoolkit.optimizer.JSOptimizerFactory;
import org.dojotoolkit.optimizer.JSOptimizerFactoryImpl;
import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;

public class JSServlet extends HttpServlet {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer");
	private static final long serialVersionUID = 1L;

	protected JSHandler jsHandler = null;
	protected JSOptimizerFactory jsOptimizerFactory = null;
	protected ResourceLoader resourceLoader = null;
	protected RhinoClassLoader rhinoClassLoader = null;
	protected boolean javaChecksum = false; 
	protected String jsHandlerType = null;
	
	public JSServlet() {}
	
	public JSServlet(ResourceLoader resourceLoader, JSOptimizerFactory jsOptimizerFactory, RhinoClassLoader rhinoClassLoader, boolean javaChecksum, String jsHandlerType) {
		this();
		this.jsOptimizerFactory = jsOptimizerFactory;
		this.resourceLoader = resourceLoader;
		this.rhinoClassLoader = rhinoClassLoader;
		this.javaChecksum = javaChecksum;
		this.jsHandlerType = jsHandlerType;
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
				resourceLoader = new ServletResourceLoader(getServletContext(), jsCompressorFactory);
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
		String javaChecksumString = getServletContext().getInitParameter("javaChecksum");
		if (javaChecksumString != null) {
			javaChecksum = Boolean.valueOf(javaChecksumString);
		}
		if (jsOptimizerFactory == null) {
			jsOptimizerFactory = (JSOptimizerFactory)getServletContext().getAttribute("org.dojotoolkit.optimizer.JSOptimizerFactory");
			if (jsOptimizerFactory == null) {
				jsOptimizerFactory = new JSOptimizerFactoryImpl();
				logger.log(Level.FINE, getClass().getName(), "Using JSOptimizer of type ["+jsOptimizerFactory.getClass().getName()+"]");
			}
		}
		getServletContext().setAttribute("org.dojotoolkit.optimizer.JSOptimizerFactory", jsOptimizerFactory);
		if (jsHandlerType == null) {
			jsHandlerType = getServletContext().getInitParameter("jsHandlerType");
			if (jsHandlerType == null) {
				jsHandlerType = JSHandler.SYNCLOADER_HANDLER_TYPE;
			}
		}
		if (jsHandlerType.equals(JSHandler.SYNCLOADER_HANDLER_TYPE)) {
			jsHandler = new SyncLoaderJSHandler();
		} else if (jsHandlerType.equals(JSHandler.AMD_HANDLER_TYPE)) {
			jsHandler = new AMDJSHandler("requirejs.json");
		}
		jsHandler.initialize(resourceLoader, rhinoClassLoader, javaChecksum, jsOptimizerFactory);
		getServletContext().setAttribute("org.dojotoolkit.optimizer.JSOptimizer", jsHandler.getJSOptimizer());
	}
	
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		jsHandler.handle(request, response);
	}
}
