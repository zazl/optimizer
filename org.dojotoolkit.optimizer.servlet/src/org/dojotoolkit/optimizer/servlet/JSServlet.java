/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.servlet;

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
	protected boolean javaChecksum = false; 
	protected String jsHandlerType = null;
	protected List<List<String>> warmupValues = null;
	protected JSCompressorFactory jsCompressorFactory = null;
	
	public JSServlet() {}
	
	public JSServlet(ResourceLoader resourceLoader, 
			         JSOptimizerFactory jsOptimizerFactory, 
			         RhinoClassLoader rhinoClassLoader, 
			         boolean javaChecksum, 
			         String jsHandlerType,
			         List<List<String>> warmupValues,
			         JSCompressorFactory jsCompressorFactory) {
		this();
		this.jsOptimizerFactory = jsOptimizerFactory;
		this.resourceLoader = resourceLoader;
		this.rhinoClassLoader = rhinoClassLoader;
		this.javaChecksum = javaChecksum;
		this.jsHandlerType = jsHandlerType;
		this.warmupValues = warmupValues;
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
			boolean inlineTemplateHTML = true;
			if (getServletContext().getInitParameter("inlineTemplateHTML") != null) {
				inlineTemplateHTML = Boolean.valueOf(getServletContext().getInitParameter("inlineTemplateHTML"));
			}
			boolean removeDojoRequires = false;
			if (getServletContext().getInitParameter("removeDojoRequires") != null) {
				removeDojoRequires = Boolean.valueOf(getServletContext().getInitParameter("removeDojoRequires"));
			}
			jsHandler = new SyncLoaderJSHandler(inlineTemplateHTML, removeDojoRequires);
		} else if (jsHandlerType.equals(JSHandler.AMD_HANDLER_TYPE)) {
			jsHandler = new AMDJSHandler("requirejs.json");
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
		jsHandler.initialize(resourceLoader, rhinoClassLoader, javaChecksum, jsOptimizerFactory, warmupValues, jsCompressorFactory);
		getServletContext().setAttribute("org.dojotoolkit.optimizer.JSOptimizer", jsHandler.getJSOptimizer());
	}
	
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		jsHandler.handle(request, response);
	}
}
