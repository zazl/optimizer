/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.servlet;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dojotoolkit.compressor.JSCompressorFactory;
import org.dojotoolkit.compressor.JSCompressorFactoryImpl;
import org.dojotoolkit.json.JSONParser;
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
	protected String jsHandlerType = null;
	protected List<List<String>> warmupValues = null;
	protected List<String> rhinoJSClasses = null;
	protected JSCompressorFactory jsCompressorFactory = null;
	
	public JSServlet() {}
	
	public JSServlet(ResourceLoader resourceLoader,
			         JSOptimizerFactory jsOptimizerFactory,
	                 RhinoClassLoader rhinoClassLoader,
	                 boolean javaChecksum,
	                 String jsHandlerType,
	                 List<List<String>> warmupValues,
	                 JSCompressorFactory jsCompressorFactory) {
		this(resourceLoader, jsOptimizerFactory, rhinoClassLoader, jsHandlerType, warmupValues, null, jsCompressorFactory);
	}

	public JSServlet(ResourceLoader resourceLoader, 
			         JSOptimizerFactory jsOptimizerFactory, 
			         RhinoClassLoader rhinoClassLoader, 
			         String jsHandlerType,
			         List<List<String>> warmupValues,
			         List<String> rhinoJSClasses,
			         JSCompressorFactory jsCompressorFactory) {
		this();
		this.jsOptimizerFactory = jsOptimizerFactory;
		this.resourceLoader = resourceLoader;
		this.rhinoClassLoader = rhinoClassLoader;
		this.jsHandlerType = jsHandlerType;
		this.warmupValues = warmupValues;
		this.rhinoJSClasses = rhinoJSClasses;
		this.jsCompressorFactory = jsCompressorFactory;
	}
	
	@SuppressWarnings("unchecked")
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		if (resourceLoader == null) {
			resourceLoader = (ResourceLoader)getServletContext().getAttribute("org.dojotoolkit.ResourceLoader");
			if (resourceLoader == null) {
				resourceLoader = new ServletResourceLoader(getServletContext());
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
		String stringRhinoJSClasses = getServletContext().getInitParameter("rhinoJSClasses");
		if (rhinoJSClasses == null && stringRhinoJSClasses != null) {
			try {
				rhinoJSClasses = (List<String>)JSONParser.parse(new StringReader(stringRhinoJSClasses));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (rhinoJSClasses != null) {
			for (String rhinoJSClass : rhinoJSClasses) {
				try {
					logger.logp(Level.INFO, getClass().getName(), "init", "Preloading ["+rhinoJSClass+"]");
					rhinoClassLoader.loadClass(rhinoJSClass);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
		if (jsOptimizerFactory == null) {
			jsOptimizerFactory = (JSOptimizerFactory)getServletContext().getAttribute("org.dojotoolkit.optimizer.JSOptimizerFactory");
			if (jsOptimizerFactory == null) {
				jsOptimizerFactory = new JSOptimizerFactoryImpl();
				logger.logp(Level.FINE, getClass().getName(), "init", "Using JSOptimizer of type ["+jsOptimizerFactory.getClass().getName()+"]");
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
			boolean inlineTemplateHTML = true;
			if (getServletContext().getInitParameter("inlineTemplateHTML") != null) {
				inlineTemplateHTML = Boolean.valueOf(getServletContext().getInitParameter("inlineTemplateHTML"));
			}
			boolean removeDojoRequires = false;
			if (getServletContext().getInitParameter("removeDojoRequires") != null) {
				removeDojoRequires = Boolean.valueOf(getServletContext().getInitParameter("removeDojoRequires"));
			}
			jsHandler = new SyncLoaderJSHandler(inlineTemplateHTML, removeDojoRequires);
		} else {
			jsHandler = new AMDJSHandler(jsHandlerType+".json");
		}
		String stringWarmupValues = getServletContext().getInitParameter("optimizerWarmup");
		if (warmupValues == null && stringWarmupValues != null) {
			try {
				warmupValues = (List<List<String>>)JSONParser.parse(new StringReader(stringWarmupValues));
			} catch (IOException e) {
				logger.logp(Level.SEVERE, getClass().getName(), "init", "IOException while parsing warmup values", e);
			}
		}
		if (jsCompressorFactory == null) {
			String compressJS = getServletContext().getInitParameter("compressJS");
			if (compressJS != null && compressJS.equalsIgnoreCase("true")) {
				jsCompressorFactory = new JSCompressorFactoryImpl();
			}
		}
		File tempDir = (File)config.getServletContext().getAttribute("javax.servlet.context.tempdir");
		jsHandler.initialize(resourceLoader, rhinoClassLoader, jsOptimizerFactory, warmupValues, jsCompressorFactory, tempDir);
		getServletContext().setAttribute("org.dojotoolkit.optimizer.JSOptimizer", jsHandler.getJSOptimizer());
	}
	
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (request.getMethod().equals("HEAD")) {
			jsHandler.handleHeadRequest(request, response);
		} else {
			jsHandler.handle(request, response);
		}
	}
}
