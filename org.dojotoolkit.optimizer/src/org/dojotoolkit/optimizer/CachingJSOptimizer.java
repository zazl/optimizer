/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dojotoolkit.server.util.resource.ResourceLoader;

public abstract class CachingJSOptimizer implements JSOptimizer {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer");
	protected Map<String, JSAnalysisDataImpl> cache = null;
	protected Map<String, Lock> lockMap = null;
	protected File tempDir = null;
	protected ResourceLoader resourceLoader = null;
	
	private static final JSAnalysisData[] EMPTY_ARRAY = new JSAnalysisData[] {};
	
	public CachingJSOptimizer(File tempDir, ResourceLoader resourceLoader) {
		this.tempDir = tempDir;
		this.resourceLoader = resourceLoader;
		cache = Collections.synchronizedMap(new HashMap<String, JSAnalysisDataImpl>());
		lockMap = new HashMap<String, Lock>();
	}
	
	public JSAnalysisData getAnalysisData(String[] modules) throws IOException {
		return getAnalysisData(modules, EMPTY_ARRAY, null);
	}

	public JSAnalysisData getAnalysisData(String[] modules, Map<String, Object> pageConfig) throws IOException {
		return getAnalysisData(modules, EMPTY_ARRAY, pageConfig);
	}

	public JSAnalysisData getAnalysisData(String[] modules, JSAnalysisData[] exclude) throws IOException {
		return getAnalysisData(modules, exclude, null);
	}

	public JSAnalysisData getAnalysisData(String[] modules, JSAnalysisData[] exclude, Map<String, Object> pageConfig) throws IOException {
		String key = JSAnalysisDataImpl.getKey(modules, exclude, pageConfig);
		logger.logp(Level.FINE, getClass().getName(), "getAnalysisData", "modules ["+key+"] in");
		Lock lock = null;
		synchronized (lockMap) {
			lock = lockMap.get(key);
			if (lock == null) {
				lock = new Lock(false);
				lockMap.put(key, lock);
			}
		}
		JSAnalysisDataImpl jsAnalysisData = null;
		synchronized (lock) {
			logger.logp(Level.FINE, getClass().getName(), "getAnalysisData", "modules ["+key+"] in lock");
			jsAnalysisData = cache.get(key);
			if (jsAnalysisData == null) {
				jsAnalysisData = JSAnalysisDataImpl.load(key, tempDir, resourceLoader);
				if (jsAnalysisData != null) {
					cache.put(key, jsAnalysisData);
				}
			}
			if (jsAnalysisData == null || jsAnalysisData.isStale()) {
				if (logger.isLoggable(Level.FINE)) {
					boolean stale = jsAnalysisData  == null ? false :  jsAnalysisData.isStale();
					StringBuffer moduleList = new StringBuffer();
					for (String module: modules) {
						moduleList.append(module);
						moduleList.append(" ");
					}
					StringBuffer excludeList = new StringBuffer();
					for (JSAnalysisData excluded: exclude) {
				        for (String excludeModule : excluded.getDependencies()) {
				        	excludeList.append(excludeModule);
				        	excludeList.append(" ");
				        }
					}
					logger.logp(Level.FINE, getClass().getName(), "getAnalysisData", "creating Analysis Data for modules["+moduleList+"] stale["+stale+"] key["+key+"] excluded["+excludeList+"]");
				}
				lock.locked = true;
				try {
					jsAnalysisData = _getAnalysisData(modules, exclude, pageConfig);
					jsAnalysisData.save(tempDir);
					cache.put(key, jsAnalysisData);
				} finally {
					lock.locked = false;
				}
			}
			logger.logp(Level.FINE, getClass().getName(), "getAnalysisData", "modules ["+key+"] out lock");
		}
		
		logger.logp(Level.FINE, getClass().getName(), "getAnalysisData", "modules ["+key+"] out");
		return jsAnalysisData;
	}
	
	public JSAnalysisData getAnalysisData(String key) throws UnsupportedOperationException {
		logger.logp(Level.FINE, getClass().getName(), "getAnalysisData", "modules ["+key+"] in");
		Lock lock = null;
		synchronized (lockMap) {
			lock = lockMap.get(key);
			if (lock == null) {
				lock = new Lock(false);
				lockMap.put(key, lock);
			}
		}
		JSAnalysisDataImpl jsAnalysisData = null;
		synchronized (lock) {
			logger.logp(Level.FINE, getClass().getName(), "getAnalysisData", "modules ["+key+"] in lock");
			jsAnalysisData = cache.get(key);
			logger.logp(Level.FINE, getClass().getName(), "getAnalysisData", "modules ["+key+"] out lock");
		}
		
		logger.logp(Level.FINE, getClass().getName(), "getAnalysisData", "modules ["+key+"] out");
		return jsAnalysisData;
	}
	
	public boolean analysisInProcess(String key) {
		Lock lock = null;
		synchronized (lockMap) {
			lock = lockMap.get(key);
			if (lock == null) {
				return false;
			}
		}
		return lock.isLocked();
	}
	
	protected abstract JSAnalysisDataImpl _getAnalysisData(String[] modules, JSAnalysisData[] exclude, Map<String, Object> config) throws IOException;
	
	private class Lock {
		boolean locked = false;
		
		public Lock(boolean locked) {
			this.locked = locked;
		}
		
		boolean isLocked() {
			return locked;
		}
	}
}
