/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.amd.rhino;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dojotoolkit.json.JSONParser;
import org.dojotoolkit.json.JSONSerializer;
import org.dojotoolkit.optimizer.CachingJSOptimizer;
import org.dojotoolkit.optimizer.JSAnalysisData;
import org.dojotoolkit.optimizer.JSAnalysisDataImpl;
import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.dojotoolkit.server.util.rhino.ASTCache;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;
import org.dojotoolkit.server.util.rhino.RhinoJSMethods;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class AMDJSOptimizer extends CachingJSOptimizer {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer.amd.rhino");

	private RhinoClassLoader rhinoClassLoader = null;
	private Map<String, Object> config = null;
	private String amdconfig = "{}";
	private List<Map<String, Object>> modulesMissingNames = null;
	private ASTCache astCacheHandler = null;
	
	public AMDJSOptimizer(ResourceLoader resourceLoader, RhinoClassLoader rhinoClassLoader, Map<String, Object> config, File tempDir) {
		super(tempDir, resourceLoader);
		this.rhinoClassLoader = rhinoClassLoader;
		this.config = config;
		loadModulesMissingNames();
		StringWriter sw = new StringWriter();
		try {
			JSONSerializer.serialize(sw, config.get("amdconfig"));
			amdconfig = sw.toString();
		} catch (IOException e) {
			logger.logp(Level.SEVERE, getClass().getName(), "AMDJSOptimizer", "IOException while parsing configuration", e);
		}
		Boolean useAstCache = (Boolean)config.get("astCache");
		if (useAstCache != null && useAstCache == Boolean.TRUE) {
			astCacheHandler = new ASTCache();
		}
	}
	
	public Map<String, Object> getConfig() {
		return config;
	}
	
	@SuppressWarnings("unchecked")
	protected JSAnalysisDataImpl _getAnalysisData(String[] modules, JSAnalysisData[] exclude, Map<String, Object> pageConfig) throws IOException {
		JSAnalysisDataImpl jsAnalysisData = null;
		
		String configString = amdconfig;
		if (pageConfig != null) {
			Map<String, Object> fullConfig = new HashMap<String, Object>();
			Map<String, Object> baseConfig = (Map<String, Object>)config.get("amdconfig"); 
			fullConfig.putAll(baseConfig);
			fullConfig.putAll(pageConfig);
			StringWriter sw = new StringWriter();
			try {
				JSONSerializer.serialize(sw, fullConfig);
				configString = sw.toString();
			} catch (IOException e) {
				logger.logp(Level.SEVERE, getClass().getName(), "AMDJSOptimizer", "IOException while parsing page configuration data", e);
			}
		}
		
		StringBuffer moduleList = new StringBuffer();
		StringBuffer sb = new StringBuffer();
        sb.append("var config = "+configString+";\n");
		sb.append("loadJS('/json/json2.js');\n");
        sb.append("loadJS('/jsutil/commonjs/loader.js');\n");
        sb.append("var analyzer = require('optimizer/amd/AMDAnalyzer').createAnalyzer(config);\n");
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
        List<String> excludeList = new ArrayList<String>();
        for (JSAnalysisData analysisData : exclude) {
	        for (String excludeModule : analysisData.getDependencies()) {
	        	if (!excludeList.contains(excludeModule)) {
	        		excludeList.add(excludeModule.substring(0, excludeModule.indexOf(".js")));
	        	}
	        }
        }
        count = 0;
        sb.append("var exclude = [");
        for (String excludeModule : excludeList) {
        	sb.append('\'');
        	sb.append(excludeModule);
        	sb.append('\'');
        	if (++count < excludeList.size()) {
            	sb.append(',');
        	}
        }
        sb.append("];\n");
		sb.append("var analysisData = analyzer.getAnalysisData(modules, exclude);\n");
		sb.append("JSON.stringify(analysisData);\n");
		Context ctx = null; 
		try {
			ctx = Context.enter();
			ScriptableObject scope = ctx.initStandardObjects();
			RhinoJSMethods.initScope(scope, resourceLoader, rhinoClassLoader, false, astCacheHandler);
			long start = System.currentTimeMillis();
			Object o = ctx.evaluateString(scope, sb.toString(), "AMDJSOptimizer", 1, null);//$NON-NLS-1$
			long end = System.currentTimeMillis();
			logger.logp(Level.FINE, getClass().getName(), "getAnalysisData", "time : "+(end-start)+" ms for ["+moduleList+"]");
			Map<String, Object> analysisData = (Map<String, Object>)JSONParser.parse(new StringReader((String)o));
			List<String> dependencies = (List<String>)analysisData.get("dependencyList");
			for (ListIterator<String> itr = dependencies.listIterator(); itr.hasNext();) {
				String dependency = itr.next();
				itr.set(dependency+=".js");
			}
			List<Map<String, Object>> missingNamesList = (List<Map<String, Object>>)analysisData.get("missingNamesList");
			if (modulesMissingNames != null) {
				missingNamesList.addAll(modulesMissingNames);
			}
			Map<String, List<Map<String, String>>> pluginRefs = (Map<String, List<Map<String, String>>>)analysisData.get("pluginRefs");
			Map<String, String> shims = (Map<String, String>)analysisData.get("shims");
			jsAnalysisData = new JSAnalysisDataImpl(modules, dependencies, null, null, missingNamesList, pluginRefs, resourceLoader, JSAnalysisDataImpl.getExludes(exclude), pageConfig, shims);
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

	@SuppressWarnings("unchecked")
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
				RhinoJSMethods.initScope(scope, resourceLoader, rhinoClassLoader, false);
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
