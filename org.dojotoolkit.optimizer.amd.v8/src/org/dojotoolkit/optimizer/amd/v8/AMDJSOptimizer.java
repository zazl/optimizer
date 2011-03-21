/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.amd.v8;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dojotoolkit.json.JSONSerializer;
import org.dojotoolkit.optimizer.CachingJSOptimizer;
import org.dojotoolkit.optimizer.ChecksumCreator;
import org.dojotoolkit.optimizer.JSAnalysisDataImpl;
import org.dojotoolkit.optimizer.Localization;
import org.dojotoolkit.rt.v8.V8JavaBridge;
import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;

public class AMDJSOptimizer extends CachingJSOptimizer {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer.amd.v8");
	private ResourceLoader resourceLoader = null;
	private Map<String, Object> config = null;
	private String aliases = "{}";
	private List<Map<String, Object>> modulesMissingNames = null;

	public AMDJSOptimizer(ResourceLoader resourceLoader, RhinoClassLoader rhinoClassLoader, boolean javaChecksum, Map<String, Object> config) {
		this.resourceLoader = resourceLoader;
		this.config = config;
		AMDOptimizerScriptRunner scriptRunner = new AMDOptimizerScriptRunner(true, resourceLoader);
		scriptRunner.loadtModulesMissingNames(config); 
		StringWriter sw = new StringWriter();
		try {
			JSONSerializer.serialize(sw, config.get("aliases"));
			aliases = sw.toString();
		} catch (IOException e) {
			logger.logp(Level.SEVERE, getClass().getName(), "AMDJSOptimizer", "IOException while parsing aliases from config", e);
		}
	}
	
	public Map<String, Object> getConfig() {
		return config;
	}
	
	protected JSAnalysisDataImpl _getAnalysisData(String[] modules, boolean useCache) throws IOException {
		AMDOptimizerScriptRunner amdOptimizerScriptRunner = new AMDOptimizerScriptRunner(useCache, resourceLoader);
		return amdOptimizerScriptRunner._getAnalysisData(modules);
	}
	
	public class AMDOptimizerScriptRunner extends V8JavaBridge {
		private ResourceLoader resourceLoader = null;

		public AMDOptimizerScriptRunner(boolean useCache, ResourceLoader resourceLoader) {
			super(useCache);
			this.resourceLoader = resourceLoader;
		}
		
		public String readResource(String path, boolean useCache) throws IOException {
			try {
				URI uri = new URI(path);
				path = uri.normalize().getPath();
				if (path.charAt(0) != '/') {
					path = '/'+path;
				}
				return resourceLoader.readResource(path, useCache);
			} catch (URISyntaxException e) {
				throw new IOException(e.getMessage());
			}
		}
		
		@SuppressWarnings("unchecked")
		public JSAnalysisDataImpl _getAnalysisData(String[] modules) throws IOException {
			JSAnalysisDataImpl jsAnalysisData = null;
			
			StringBuffer moduleList = new StringBuffer();
			StringBuffer sb = new StringBuffer();
	        sb.append("loadJS('/jsutil/commonjs/loader.js');\n");
	        sb.append("var analyzer = require('optimizer/amd/AMDAnalyzer').createAnalyzer("+aliases+");\n");
	        int count = 0;
	        sb.append("var modules = [");
	        for (String module : modules) {
	        	moduleList.append(module);
	        	sb.append('\'');
	        	sb.append(module);
	        	sb.append('\'');
	        	if (++count < modules.length) {
	            	sb.append(',');
	        		moduleList.append(',');
	        	}
	        }
	        sb.append("];\n");
			sb.append("var analysisData = analyzer.getAnalysisData(modules);\n");
			sb.append("JSON.stringify(analysisData);\n");
			try {
				long start = System.currentTimeMillis();
				Map<String, Object> analysisData = (Map<String, Object>)runScript(sb.toString());
				long end = System.currentTimeMillis();
				logger.logp(Level.FINE, getClass().getName(), "getAnalysisData", "time : "+(end-start)+" ms for ["+moduleList+"]");
				List<String> dependencies = (List<String>)analysisData.get("dependencyList");
				List<Localization> localizationList = new ArrayList<Localization>();
				List<Map<String, Object>> localizations = (List<Map<String, Object>>)analysisData.get("localizations");
				for (Map<String, Object> localizationMap : localizations) {
					Localization localization = new Localization((String)localizationMap.get("bundlepackage"), (String)localizationMap.get("modpath"), (String)localizationMap.get("bundlename"));
					localizationList.add(localization);
				}
				for (ListIterator<String> itr = dependencies.listIterator(); itr.hasNext();) {
					String dependency = itr.next();
					itr.set(dependency+=".js");
				}
				List<String> textList = (List<String>)analysisData.get("textList");
				List<Map<String, Object>> missingNamesList = (List<Map<String, Object>>)analysisData.get("missingNamesList");
				missingNamesList.addAll(modulesMissingNames);
				jsAnalysisData = new JSAnalysisDataImpl(modules, dependencies, null, localizationList, textList, missingNamesList, resourceLoader);
				jsAnalysisData.setChecksum(ChecksumCreator.createChecksum(jsAnalysisData.getDependencies(), resourceLoader));
			} catch (Throwable e) {
				if (compileErrors.size() > 0) {
					for (Throwable t : compileErrors) {
						t.printStackTrace();
					}
				}
				logger.logp(Level.SEVERE, getClass().getName(), "getAnalysisData", "Exception on getAnalysisData for ["+moduleList+"]", e);
				throw new IOException("Exception on getAnalysisData for ["+moduleList+"] : "+e.getMessage());
			}
			return jsAnalysisData;
		}
		
		public void loadtModulesMissingNames(Map<String, Object> config) {
			List<Map<String, Object>> implicitDependencies = (List<Map<String, Object>>)config.get("implicitDependencies"); 
			if (implicitDependencies != null) {
				StringBuffer sb = new StringBuffer();
		        sb.append("loadJS('/jsutil/commonjs/loader.js');\n");
		        sb.append("var analyzer = require('optimizer/amd/AMDAnalyzer');\n");
		        StringWriter sw = new StringWriter();
				try {
					JSONSerializer.serialize(sw, implicitDependencies);
			        sb.append("var implicitDependencies = "+sw.toString()+";");
			        sb.append("var modulesMissingNames = [];");
			        sb.append("for (var i = 0; i < implicitDependencies.length; i++) {");
			        sb.append("var missingNameIndex = analyzer.getMissingNameIndex(readText(implicitDependencies[i].uri));");
			        sb.append("if (missingNameIndex != -1) { ");
			        sb.append("modulesMissingNames.push({uri: implicitDependencies[i].uri, nameIndex: missingNameIndex});");
			        sb.append("}");
			        sb.append("}");
					sb.append("JSON.stringify(modulesMissingNames);\n");
					
					long start = System.currentTimeMillis();
					modulesMissingNames = (List<Map<String, Object>>)runScript(sb.toString());
					long end = System.currentTimeMillis();
					config.put("modulesMissingNames", modulesMissingNames);
					logger.logp(Level.FINE, getClass().getName(), "modulesMissingNames", "time : "+(end-start)+" ms for ["+sb+"]");
				} catch(Throwable t) {
					logger.logp(Level.SEVERE, getClass().getName(), "getAnalysisData", "Exception on getAnalysisData for ["+sb+"]", t);
				}
			}
		}
	}
}
