/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.v8;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dojotoolkit.optimizer.JSOptimizer;
import org.dojotoolkit.optimizer.Localization;
import org.dojotoolkit.optimizer.CachingJSOptimizer;
import org.dojotoolkit.optimizer.ChecksumCreator;
import org.dojotoolkit.optimizer.JSAnalysisDataImpl;
import org.dojotoolkit.rt.v8.V8JavaBridge;
import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;

public class V8JSOptimizer extends CachingJSOptimizer implements JSOptimizer {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer");
	protected Map<String, JSAnalysisDataImpl> cache = null;
	private ResourceLoader resourceLoader = null;
	private boolean javaChecksum = false;

	public V8JSOptimizer(ResourceLoader resourceLoader, RhinoClassLoader rhinoClassLoader, boolean javaChecksum) {
		super();
		this.resourceLoader = resourceLoader;
		this.javaChecksum = javaChecksum;
	}

	public JSAnalysisDataImpl _getAnalysisData(String[] modules, boolean useCache) throws IOException {
		V8OptimizerScriptRunner v8OptimizerScriptRunner = new V8OptimizerScriptRunner(useCache, resourceLoader);
		return v8OptimizerScriptRunner._getAnalysisData(modules);
	}
	
	public class V8OptimizerScriptRunner extends V8JavaBridge {
		private ResourceLoader resourceLoader = null;

		public V8OptimizerScriptRunner(boolean useCache, ResourceLoader resourceLoader) {
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
			try {
				long start = System.currentTimeMillis();
				Map<String, Object> analysisData = (Map<String, Object>)runScript(sb.toString());
				long end = System.currentTimeMillis();
				logger.logp(Level.FINE, getClass().getName(), "getAnalysisData", "time : "+(end-start)+" ms for ["+moduleList+"]");
				List<String> dependencies = (List<String>)analysisData.get("dependencyList");
				String checksum = (String)analysisData.get("checksum");
				List<Localization> localizationList = new ArrayList<Localization>();
				List<Map<String, Object>> localizations = (List<Map<String, Object>>)analysisData.get("localizations");
				for (Map<String, Object> localizationMap : localizations) {
					Localization localization = new Localization((String)localizationMap.get("bundlepackage"), (String)localizationMap.get("modpath"), (String)localizationMap.get("bundlename"));
					localizationList.add(localization);
				}
				jsAnalysisData = new JSAnalysisDataImpl(modules, dependencies, checksum, localizationList, resourceLoader);
			} catch (Throwable e) {
				if (compileErrors.size() > 0) {
					for (Throwable t : compileErrors) {
						t.printStackTrace();
					}
				}
				logger.logp(Level.SEVERE, getClass().getName(), "getAnalysisData", "Exception on getAnalysisData for ["+moduleList+"]", e);
				throw new IOException("Exception on getAnalysisData for ["+moduleList+"] : "+e.getMessage());
			}
			if (javaChecksum) {
				jsAnalysisData.setChecksum(ChecksumCreator.createChecksum(jsAnalysisData.getDependencies(), resourceLoader));
			}
			return jsAnalysisData;
		}
	}
}
