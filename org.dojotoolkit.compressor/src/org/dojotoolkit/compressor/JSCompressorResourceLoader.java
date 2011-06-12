/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.compressor;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dojotoolkit.server.util.resource.ResourceLoader;

public class JSCompressorResourceLoader implements ResourceLoader {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.compressor");
	
	private JSCompressor jsCompressor = null;
	private Pattern[] ignorePatterns = null;
	private ResourceLoader resourceLoader = null;
	private Map<String, CacheEntry> cache = null;
	protected Map<String, Object> lockMap = null;

	public JSCompressorResourceLoader(JSCompressorFactory jsCompressorFactory, ResourceLoader resourceLoader) {
		cache = Collections.synchronizedMap(new HashMap<String, CacheEntry>());
		lockMap = new HashMap<String, Object>();
		if (jsCompressorFactory != null) {
			jsCompressor = jsCompressorFactory.createJSCompressor(resourceLoader);
			String[] ignoreList = jsCompressorFactory.getIgnoreList();
			ignorePatterns = new Pattern[ignoreList.length];
			int i = 0;
			for (String ignorePath : ignoreList) {
				ignorePatterns[i++] = Pattern.compile(ignorePath);
			}
		}
	}
	
	private boolean doCompress(String path) {
		boolean compress = false;
		if (jsCompressor != null && path.endsWith(".js")) {
			boolean ignore = false;
			if (ignorePatterns != null) {
				for (Pattern ignorePattern : ignorePatterns) {
					Matcher matcher = ignorePattern.matcher(path);
					if (matcher.lookingAt()) {
						logger.logp(Level.FINE, getClass().getName(), "doCompress", "["+path+"] will not be compressed");
						ignore = true;
						break;
					}
				}
			}
			compress = !ignore;
		}
		return compress;
	}

	public URL getResource(String path) throws IOException {
		return resourceLoader.getResource(path);
	}

	public String readResource(String path) throws IOException {
		if (doCompress(path)) {
			Object lock = null;
			synchronized (lockMap) {
				lock = lockMap.get(path);
				if (lock == null) {
					lock = new Object();
					lockMap.put(path, lock);
				}
			}
			
			synchronized (lock) {
				long currentTimestamp = resourceLoader.getTimestamp(path);
				CacheEntry cacheEntry = cache.get(path);
				if (cacheEntry == null || currentTimestamp != cacheEntry.timestamp) {
					String contents = resourceLoader.readResource(path);
					if (contents == null) {
						return contents;
					}
					logger.logp(Level.FINE, getClass().getName(), "filter", "["+path+"] is being compressed");
					cacheEntry = new CacheEntry(jsCompressor.compress(contents), currentTimestamp);
					cache.put(path, cacheEntry);
				}
				return cacheEntry.compressedContents;
			}
		} else {
			return resourceLoader.readResource(path);
		}
	}

	public long getTimestamp(String path) {
		return resourceLoader.getTimestamp(path);
	}
	
	private class CacheEntry {
		long timestamp = -1;
		String compressedContents = null;
		
		public CacheEntry(String compressedContents, long timestamp) {
			this.compressedContents = compressedContents;
			this.timestamp = timestamp;
		}
	}
}
