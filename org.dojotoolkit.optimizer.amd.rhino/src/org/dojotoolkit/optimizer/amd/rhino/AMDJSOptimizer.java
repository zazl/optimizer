/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.amd.rhino;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dojotoolkit.json.JSONParser;
import org.dojotoolkit.json.JSONSerializer;
import org.dojotoolkit.optimizer.CachingJSOptimizer;
import org.dojotoolkit.optimizer.ChecksumCreator;
import org.dojotoolkit.optimizer.JSAnalysisDataImpl;
import org.dojotoolkit.optimizer.Localization;
import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;
import org.dojotoolkit.server.util.rhino.RhinoJSMethods;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class AMDJSOptimizer extends CachingJSOptimizer {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer.amd.rhino");

	private RhinoClassLoader rhinoClassLoader = null;
	private ResourceLoader resourceLoader = null;
	private Map<String, Object> config = null;
	private String aliases = "{}";
	private List<Map<String, Object>> modulesMissingNames = null;
	
	public AMDJSOptimizer(ResourceLoader resourceLoader, RhinoClassLoader rhinoClassLoader, boolean javaChecksum, Map<String, Object> config) {
		this.resourceLoader = resourceLoader;
		this.rhinoClassLoader = rhinoClassLoader;
		this.config = config;
		loadModulesMissingNames();
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
		sb.append("loadJS('/json/json2.js');\n");
		sb.append("JSON.stringify(analysisData);\n");
		Context ctx = null; 
		try {
			ctx = Context.enter();
			ScriptableObject scope = ctx.initStandardObjects();
			RhinoJSMethods.initScope(scope, resourceLoader, rhinoClassLoader, useCache, false);
			long start = System.currentTimeMillis();
			Object o = ctx.evaluateString(scope, sb.toString(), "AMDJSOptimizer", 1, null);//$NON-NLS-1$
			long end = System.currentTimeMillis();
			logger.logp(Level.FINE, getClass().getName(), "getAnalysisData", "time : "+(end-start)+" ms for ["+sb+"]");
			Map<String, Object> analysisData = (Map<String, Object>)JSONParser.parse(new StringReader((String)o));
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
		}
		catch(Throwable t) {
			logger.logp(Level.SEVERE, getClass().getName(), "getAnalysisData", "Exception on getAnalysisData for ["+moduleList+"]", t);
			throw new IOException("Exception on getAnalysisData for ["+sb+"] : "+t.getMessage());
		}
		finally {
			Context.exit();
		}
		return jsAnalysisData;
	}

	private void loadModulesMissingNames() {
		List<Map<String, Object>> implicitDependencies = (List<Map<String, Object>>)config.get("implicitDependencies"); 
		if (implicitDependencies != null) {
			StringBuffer sb = new StringBuffer();
	        sb.append("loadJS('/jsutil/commonjs/loader.js');\n");
	        sb.append("var analyzer = require('optimizer/amd/AMDAnalyzer');\n");
	        StringWriter sw = new StringWriter();
			Context ctx = null; 
			try {
				JSONSerializer.serialize(sw, implicitDependencies);
		        sb.append("var implicitDependencies = "+sw.toString()+";\n");
		        sb.append("var modulesMissingNames = [];\n");
		        sb.append("for (var i = 0; i < implicitDependencies.length; i++) {\n");
		        sb.append("var missingNameIndex = analyzer.getMissingNameIndex(readText(implicitDependencies[i].uri));\n");
		        sb.append("if (missingNameIndex != -1) {\n");
		        sb.append("modulesMissingNames.push({uri: implicitDependencies[i].uri, nameIndex: missingNameIndex});\n");
		        sb.append("}\n");
		        sb.append("}\n");
				sb.append("loadJS('/json/json2.js');\n");
				sb.append("JSON.stringify(modulesMissingNames);\n");
				
				ctx = Context.enter();
				ScriptableObject scope = ctx.initStandardObjects();
				RhinoJSMethods.initScope(scope, resourceLoader, rhinoClassLoader, true, false);
				long start = System.currentTimeMillis();
				Object o = ctx.evaluateString(scope, sb.toString(), "AMDJSOptimizer", 1, null);
				long end = System.currentTimeMillis();
				logger.logp(Level.FINE, getClass().getName(), "modulesMissingNames", "time : "+(end-start)+" ms for ["+sb+"]");
				modulesMissingNames = (List<Map<String, Object>>)JSONParser.parse(new StringReader((String)o));
				config.put("modulesMissingNames", modulesMissingNames);
			} catch(Throwable t) {
				logger.logp(Level.SEVERE, getClass().getName(), "getAnalysisData", "Exception on getAnalysisData for ["+sb+"]", t);
			}
			finally {
				Context.exit();
			}
		}
	}
}
