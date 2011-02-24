/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public abstract class CachingJSOptimizer implements JSOptimizer {
	protected Map<String, JSAnalysisDataImpl> cache = null;
	
	public CachingJSOptimizer() {
		cache = new HashMap<String, JSAnalysisDataImpl>();
	}
	
	public synchronized JSAnalysisData getAnalysisData(String[] modules) throws IOException {
		String key = getKey(modules);
		JSAnalysisDataImpl jsAnalysisData = cache.get(key);
		
		if (jsAnalysisData == null) {
			jsAnalysisData = _getAnalysisData(modules, true);
			cache.put(key, jsAnalysisData);
		} else if (jsAnalysisData.isStale()) {
			jsAnalysisData = _getAnalysisData(modules, false);
			cache.put(key, jsAnalysisData);
		}
		
		return jsAnalysisData;
	}
	
	protected abstract JSAnalysisDataImpl _getAnalysisData(String[] modules, boolean useCache) throws IOException;
	
	private String getKey(String[] keyValues) {
		StringBuffer key = new StringBuffer();
		for (String keyValue : keyValues) {
			key.append(keyValue);
		}
		return key.toString();
	}
}
