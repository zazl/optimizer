/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.servlet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dojotoolkit.compressor.JSCompressorContentFilter;
import org.dojotoolkit.compressor.JSCompressorFactory;
import org.dojotoolkit.json.JSONParser;
import org.dojotoolkit.optimizer.CachingJSOptimizer;
import org.dojotoolkit.optimizer.JSAnalysisData;
import org.dojotoolkit.optimizer.JSAnalysisDataImpl;
import org.dojotoolkit.optimizer.JSOptimizer;
import org.dojotoolkit.optimizer.JSOptimizerFactory;
import org.dojotoolkit.optimizer.Util;
import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;

public abstract class JSHandler {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer");
	
	public static final String AMD_HANDLER_TYPE = "amd";
	public static final String SYNCLOADER_HANDLER_TYPE = "syncloader";
	private static final JSAnalysisData[] EMPTY_ARRAY = new JSAnalysisData[] {};

	protected JSOptimizer jsOptimizer = null;
	protected ResourceLoader resourceLoader = null;
	protected Map<String, Object> config = null;
	protected String[] bootstrapModules = null;
	protected String[] debugBootstrapModules = null;
	protected JSCompressorContentFilter compressorContentFilter = null;
	protected Map<String, Map<String, Integer>> sourceMapOffsets = null;
	
	public JSHandler(String configFileName) {
		try {
			config = loadHandlerConfig(configFileName);
		} catch (IOException e) {
			logger.logp(Level.SEVERE, getClass().getName(), "JSHandler", "IOException while attempting to load ["+configFileName+"]", e);
		}
	}

	public void initialize(ResourceLoader resourceLoader, 
			               RhinoClassLoader rhinoClassLoader, 
			               JSOptimizerFactory jsOptimizerFactory, 
			               JSCompressorFactory jsCompressorFactory) {
		this.initialize(resourceLoader, rhinoClassLoader, jsOptimizerFactory, null, jsCompressorFactory, new File("."));
	}
	
	@SuppressWarnings("unchecked")
	public void initialize(ResourceLoader resourceLoader, 
			               RhinoClassLoader rhinoClassLoader, 
			               JSOptimizerFactory jsOptimizerFactory, 
			               List<List<String>> warmupValues, 
			               JSCompressorFactory jsCompressorFactory, 
			               File tempDir) {
		this.resourceLoader = resourceLoader;
		jsOptimizer = jsOptimizerFactory.createJSOptimizer(resourceLoader, rhinoClassLoader, config, tempDir);
		List<String> bootstrapModuleList = (List<String>)config.get("bootstrapModules");
		bootstrapModules = new String[bootstrapModuleList.size()];
		bootstrapModules = bootstrapModuleList.toArray(bootstrapModules);
		List<String> debugBootstrapModuleList = (List<String>)config.get("debugBootstrapModules");
		debugBootstrapModules = new String[debugBootstrapModuleList.size()];
		debugBootstrapModules = debugBootstrapModuleList.toArray(debugBootstrapModules);
		if (warmupValues != null && jsOptimizer instanceof CachingJSOptimizer) {
			for (List<String> modules : warmupValues) {
				new Thread(new OptimizerRunnable(jsOptimizer, modules)).start();
			}
		}
		compressorContentFilter = new JSCompressorContentFilter(jsCompressorFactory, resourceLoader);
		sourceMapOffsets = Collections.synchronizedMap(new HashMap<String, Map<String, Integer>>());
	}
	
	public boolean handle(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (request.getParameter("sourcemap") != null) {
			return handleSourceMapRequest(request, response);
		}
		String[] modules = null;
		String modulesParam = request.getParameter("modules");
		String key = request.getParameter("key");
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
        JSAnalysisData[] exclude = EMPTY_ARRAY;

		if (modulesParam != null) {
            modules = getAsList(modulesParam);
            String excludeParam = request.getParameter("exclude");
            if (excludeParam != null) {
                String[] keys = getAsList(excludeParam);
                exclude = new JSAnalysisData[keys.length];
                int count = 0;
                for (String excludeKey : keys) {
                    exclude[count++] = jsOptimizer.getAnalysisData(excludeKey);
                }
            }
            String configString = request.getParameter("config");
            @SuppressWarnings("unchecked")
			Map<String, Object> pageConfig = (Map<String, Object>)JSONParser.parse(new StringReader(configString));
            analysisData = jsOptimizer.getAnalysisData(modules, exclude, pageConfig);
        } else if (key != null) {
			analysisData = jsOptimizer.getAnalysisData(key);
		}
		if (!debug) {
			String checksum = analysisData.getChecksum();
		    String ifNoneMatch = request.getHeader("If-None-Match");

		    if (ifNoneMatch != null && ifNoneMatch.equals(checksum)) {
		    	response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
		        return true;
		    }

 			response.setHeader("ETag", checksum);

			String version = request.getParameter("version");
			if (version != null && version.equals(checksum)) {
				Calendar calendar = Calendar.getInstance();
				calendar.add(Calendar.YEAR, 1);
				response.setDateHeader("Expires", calendar.getTimeInMillis());
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
 			boolean writeBootstrap = (request.getParameter("writeBootstrap") == null) ? true : Boolean.valueOf(request.getParameter("writeBootstrap"));
 			if (writeBootstrap) {
	 			for (String bootstrapModulePath: bootstrapModulePaths) {
		 			osw.write(compressorContentFilter.filter(resourceLoader.readResource(bootstrapModulePath), bootstrapModulePath));
		 			//osw.write(resourceLoader.readResource(bootstrapModulePath));
	 			}
 			}
 			customHandle(request, osw, analysisData, exclude);
		} catch (IOException e) {
			String msg = "Exception on request for [";
			if (key == null) {
				msg += modulesParam;
			} else {
				msg += key;
			}
			msg += "]";
			logger.logp(Level.SEVERE, getClass().getName(), "handle", msg, e);
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
	
	public void handleHeadRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String[] modules = null;
		String modulesParam = request.getParameter("modules");
        JSAnalysisData[] exclude = EMPTY_ARRAY;
		if (modulesParam != null) {
            modules = getAsList(modulesParam);
            String configString = request.getParameter("config");
            @SuppressWarnings("unchecked")
			Map<String, Object> pageConfig = (Map<String, Object>)JSONParser.parse(new StringReader(configString));
            String key = JSAnalysisDataImpl.getKey(modules, exclude, pageConfig);
            if (jsOptimizer.analysisInProcess(key)) {
            	response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            	return;
            }
            JSAnalysisData analysisData = jsOptimizer.getAnalysisData(key);
            if (analysisData != null) {
    			String checksum = analysisData.getChecksum();
    		    String ifNoneMatch = request.getHeader("If-None-Match");

    		    if (ifNoneMatch != null && ifNoneMatch.equals(checksum)) {
    		    	response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
    		        return;
    		    }

     			response.setHeader("ETag", checksum);

    			String version = request.getParameter("version");
    			if (version != null && version.equals(checksum)) {
    				Calendar calendar = Calendar.getInstance();
    				calendar.add(Calendar.YEAR, 1);
    				response.setDateHeader("Expires", calendar.getTimeInMillis());
    			}
		    	response.setStatus(HttpServletResponse.SC_OK);
		    	return;
            }
        	new Thread(new OptimizerRunnable(jsOptimizer, modules, pageConfig)).start();
		}
    	response.setStatus(HttpServletResponse.SC_NOT_FOUND);
	}

	protected boolean handleSourceMapRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String sourcemapKey = request.getParameter("sourcemap");
		sourcemapKey = sourcemapKey.substring(0, sourcemapKey.indexOf(".map"));
		JSAnalysisData analysisData = jsOptimizer.getAnalysisData(sourcemapKey);
		Map<String, Integer> sourceMapOffset = sourceMapOffsets.get(sourcemapKey);
		StringBuffer sb = new StringBuffer();
		sb.append("{\"version\": 3, \"file\": \""+sourcemapKey+".js\", \"sections\": [");
		String[] dependencies = analysisData.getDependencies();
		for (String dependency : dependencies) {
			String path = Util.normalizePath(dependency);
			Integer offset = sourceMapOffset.get(path);
			String sourcemap = compressorContentFilter.getJSCompressor().getSourceMap(path+".map");
			sb.append("{\"offset\": {\"line\": "+offset+", \"column\": 0}, \"map\": ");
			sb.append(sourcemap);
			sb.append("},");
		}
		sb.deleteCharAt(sb.length()-1);
		sb.append("]}");
		response.getWriter().write(sb.toString());
		return true;

	}
	
	protected abstract void customHandle(HttpServletRequest request, Writer writer, JSAnalysisData analysisData, JSAnalysisData[] exclude) throws ServletException, IOException;
	
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

	private String[] getAsList(String param) {
		String[] list = null;
		List<String> moduleList = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(param, ",");
		while (st.hasMoreTokens()) {
			moduleList.add(st.nextToken());
		}
		list = new String[moduleList.size()];
		list = moduleList.toArray(list);
		return list;
	}

	public class OptimizerRunnable implements Runnable {
		private JSOptimizer optimizer = null;
		private String[] modules = null;
		private String modulesAsString = "";
		private Map<String, Object> pageConfig = null;
		
		public OptimizerRunnable(JSOptimizer optimizer, List<String> modules) {
			this.optimizer = optimizer;
			this.modules = new String[modules.size()];
			this.modules = modules.toArray(this.modules);
			for (String module : modules) {
				modulesAsString	+= module;
				modulesAsString += ' ';
			}
		}
		
		public OptimizerRunnable(JSOptimizer optimizer, String[] modules, Map<String, Object> pageConfig) {
			this.optimizer = optimizer;
			this.modules = modules;
			for (String module : modules) {
				modulesAsString	+= module;
				modulesAsString += ' ';
			}
			this.pageConfig = pageConfig;
		}
		
		public void run() {
			try {
				logger.logp(Level.INFO, getClass().getName(), "run", "Obtaining Optimization Data for ["+modulesAsString+"]");
				if (config == null) {
					optimizer.getAnalysisData(modules);
				} else {
					optimizer.getAnalysisData(modules, EMPTY_ARRAY, pageConfig);
				}
				logger.logp(Level.INFO, getClass().getName(), "run", "Obtained Optimization Data for ["+modulesAsString+"]");
			} catch (IOException e) {
				logger.logp(Level.SEVERE, getClass().getName(), "OptimizerRunnable", "IOException while attempting to obtain Optimization Data for ["+modulesAsString+"]", e);
			}
		}
	}
	
}
