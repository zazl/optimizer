/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.rhino;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dojotoolkit.json.JSONParser;
import org.dojotoolkit.optimizer.Localization;
import org.dojotoolkit.optimizer.CachingJSOptimizer;
import org.dojotoolkit.optimizer.ChecksumCreator;
import org.dojotoolkit.optimizer.JSAnalysisDataImpl;
import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;
import org.dojotoolkit.server.util.rhino.RhinoJSMethods;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class RhinoJSOptimizer extends CachingJSOptimizer {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer");

	private RhinoClassLoader rhinoClassLoader = null;
	private ResourceLoader resourceLoader = null;
	private boolean javaChecksum = false;
	private Map<String, Object> config = null;
	
	public RhinoJSOptimizer(ResourceLoader resourceLoader, RhinoClassLoader rhinoClassLoader, boolean javaChecksum, Map<String, Object> config) {
		this.resourceLoader = resourceLoader;
		this.rhinoClassLoader = rhinoClassLoader;
		this.javaChecksum = javaChecksum;
		this.config = config;
	}
	
	public Map<String, Object> getConfig() {
		return config;
	}
	
	@SuppressWarnings("unchecked")
	protected JSAnalysisDataImpl _getAnalysisData(String[] modules, boolean useCache) throws IOException {
		JSAnalysisDataImpl jsAnalysisData = null;
		
		StringBuffer moduleList = new StringBuffer();
		StringBuffer sb = new StringBuffer();
        sb.append("loadJS('/optimizer/syncloader/bootstrap.js');\n"); //$NON-NLS-1$
        sb.append("loadJS('/optimizer/syncloader/analyzer.js');\n"); //$NON-NLS-1$
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
        sb.append("var analyzer = new dojo.optimizer.Analyzer();\n"); 
        
		if (javaChecksum) {
			sb.append("var analysisData = analyzer.getAnalysisData(modules, true);\n");
		} else {
			sb.append("var analysisData = analyzer.getAnalysisData(modules);\n");
		}
		sb.append("JSON.stringify(analysisData);\n");
		Context ctx = null; 
		try {
			ctx = Context.enter();
			ScriptableObject scope = ctx.initStandardObjects();
			RhinoJSMethods.initScope(scope, resourceLoader, rhinoClassLoader, useCache, false);
			long start = System.currentTimeMillis();
			Object o = ctx.evaluateString(scope, sb.toString(), "RhinoJSOptimizer", 1, null);//$NON-NLS-1$
			long end = System.currentTimeMillis();
			logger.logp(Level.FINE, getClass().getName(), "getAnalysisData", "time : "+(end-start)+" ms for ["+moduleList+"]");
			Map<String, Object> analysisData = (Map<String, Object>)JSONParser.parse(new StringReader((String)o));
			List<String> dependencies = (List<String>)analysisData.get("dependencyList");
			String checksum = (String)analysisData.get("checksum");
			List<Localization> localizationList = new ArrayList<Localization>();
			List<Map<String, Object>> localizations = (List<Map<String, Object>>)analysisData.get("localizations");
			for (Map<String, Object> localizationMap : localizations) {
				Localization localization = new Localization((String)localizationMap.get("bundlepackage"), (String)localizationMap.get("modpath"), (String)localizationMap.get("bundlename"));
				localizationList.add(localization);
			}
			jsAnalysisData = new JSAnalysisDataImpl(modules, dependencies, checksum, localizationList, null, null, resourceLoader);
		}
		catch(Throwable t) {
			logger.logp(Level.SEVERE, getClass().getName(), "getAnalysisData", "Exception on getAnalysisData for ["+moduleList+"]", t);
			throw new IOException("Exception on getAnalysisData for ["+moduleList+"] : "+t.getMessage());
		}
		finally {
			Context.exit();
		}
		if (javaChecksum) {
			jsAnalysisData.setChecksum(ChecksumCreator.createChecksum(jsAnalysisData.getDependencies(), resourceLoader));
		}
		return jsAnalysisData;
	}
}
