/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.servlet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dojotoolkit.json.JSONParser;
import org.dojotoolkit.optimizer.JSAnalysisData;
import org.dojotoolkit.optimizer.JSOptimizer;
import org.dojotoolkit.optimizer.JSOptimizerFactory;
import org.dojotoolkit.optimizer.Localization;
import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;

public abstract class JSHandler {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer");
	
	public static final String AMD_HANDLER_TYPE = "amd";
	public static final String SYNCLOADER_HANDLER_TYPE = "syncloader";
	
	protected JSOptimizer jsOptimizer = null;
	protected ResourceLoader resourceLoader = null;
	protected Map<String, Object> config = null;
	protected String[] bootstrapModules = null;
	protected String[] debugBootstrapModules = null;
	
	public JSHandler(String configFileName) {
		try {
			config = loadHandlerConfig(configFileName);
		} catch (IOException e) {
			logger.logp(Level.SEVERE, getClass().getName(), "JSHandler", "IOException while attempting to load ["+configFileName+"]", e);
		}
	}

	@SuppressWarnings("unchecked")
	public void initialize(ResourceLoader resourceLoader, RhinoClassLoader rhinoClassLoader, boolean javaChecksum, JSOptimizerFactory jsOptimizerFactory) {
		this.resourceLoader = resourceLoader;
		jsOptimizer = jsOptimizerFactory.createJSOptimizer(resourceLoader, rhinoClassLoader, javaChecksum, config);
		List<String> bootstrapModuleList = (List<String>)config.get("bootstrapModules");
		bootstrapModules = new String[bootstrapModuleList.size()];
		bootstrapModules = bootstrapModuleList.toArray(bootstrapModules);
		List<String> debugBootstrapModuleList = (List<String>)config.get("debugBootstrapModules");
		debugBootstrapModules = new String[debugBootstrapModuleList.size()];
		debugBootstrapModules = debugBootstrapModuleList.toArray(debugBootstrapModules);
	}
	
	public boolean handle(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String[] modules = null;
		String modulesParam = request.getParameter("modules");
		String url = request.getPathInfo();
		if (url != null && url.startsWith("/_javascript")) {
			url = url.substring("/_javascript".length());
		}
		String[] bootstrapModulePaths = bootstrapModules;
		boolean debug = (request.getParameter("debug") == null) ? false : Boolean.valueOf(request.getParameter("debug"));
		if (debug) {
			bootstrapModulePaths = debugBootstrapModules;
		}
		
		boolean gzip = false;
		
		String encoding = request.getHeader("Accept-Encoding");
		if (encoding!=null && encoding.indexOf("gzip") >= 0) {
			gzip = true;
		}
		
		response.setContentType("text/javascript; charset=UTF-8");
		
		JSAnalysisData analysisData = null;
		if (modulesParam != null) {
			modules = getModuleList(modulesParam);
			try {
				analysisData = jsOptimizer.getAnalysisData(modules);
				if (logger.getLevel() == Level.FINE) {
					logger.logp(Level.FINE, getClass().getName(), "handle", "checksum for ["+modulesParam+"] = "+analysisData.getChecksum());
					for (String dependency : analysisData.getDependencies()) {
						logger.logp(Level.FINER, getClass().getName(), "handle", "dependency for ["+modulesParam+"] = ["+dependency+"]");
					}
					for (Localization localization : analysisData.getLocalizations()) {
						logger.logp(Level.FINER, getClass().getName(), "handle", "localization for ["+modulesParam+"] = ["+localization.modulePath+"]");
					}
				}
			} catch (IOException e) {
				logger.logp(Level.SEVERE, getClass().getName(), "handle", "Exception on request for ["+modulesParam+"]", e);
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
				return true;
			}
			if (!debug) {
				String checksum = analysisData.getChecksum();
			    String ifNoneMatch = request.getHeader("If-None-Match");
	
			    if (ifNoneMatch != null && ifNoneMatch.equals(checksum)) {
			    	response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			        return true;
			    }
			    
				if (modules == null) {
					modules = getModuleList(modulesParam);
				}
	 			response.setHeader("ETag", checksum);
	 			
				String version = request.getParameter("version");
				if (version != null && version.equals(checksum)) {
					Calendar calendar = Calendar.getInstance();
					calendar.add(Calendar.YEAR, 1);
					response.setDateHeader("Expires", calendar.getTimeInMillis());
				}
			}
		}
		Writer osw = null;
		GZIPOutputStream gz = null;
		try {
 			if (gzip) {
 				response.setHeader("Content-Encoding","gzip");
                gz = new GZIPOutputStream(response.getOutputStream());
	 			osw = new BufferedWriter(new OutputStreamWriter(gz, "UTF-8"));
 			} else {
	 			osw = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
 			}
 			for (String bootstrapModulePath: bootstrapModulePaths) {
	 			osw.write(resourceLoader.readResource(bootstrapModulePath, !debug));
 			}
 			customHandle(request, osw, analysisData);
		} catch (IOException e) {
			logger.logp(Level.SEVERE, getClass().getName(), "handle", "Exception on request for ["+modulesParam+"]", e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		} finally {
			osw.flush();
 			if (gzip) {
 				gz.close();
 			}
		}
        return true;
	}
	
	public JSOptimizer getJSOptimizer() {
		return jsOptimizer;
	}
	
	protected abstract void customHandle(HttpServletRequest request, Writer writer, JSAnalysisData analysisData) throws ServletException, IOException;
	
	private String[] getModuleList(String modulesParam) {
		String[] modules = null;
		List<String> moduleList = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(modulesParam, ",");
		while (st.hasMoreTokens()) {
			moduleList.add(st.nextToken());
		}
		modules = new String[moduleList.size()];
		modules = moduleList.toArray(modules);
		return modules;
	}
	
	
	@SuppressWarnings("unchecked")
	protected static Map<String, Object> loadHandlerConfig(String handlerConfigFileName) throws IOException {
		Map<String, Object> handlerConfig = null;
		URL handlerConfigURL = JSHandler.class.getClassLoader().getResource(handlerConfigFileName);
		InputStream is = null;
		Reader r = null;
		try {
			is = handlerConfigURL.openStream();
			r = new BufferedReader(new InputStreamReader(is));
			handlerConfig = (Map<String, Object>)JSONParser.parse(r);
		} finally {
			if (is != null) { try { is.close(); } catch (IOException e) {}}
		}
		return handlerConfig;
	}
}
