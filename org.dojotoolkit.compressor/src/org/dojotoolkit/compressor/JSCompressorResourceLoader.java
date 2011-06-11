/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.compressor;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dojotoolkit.server.util.resource.CachingResourceLoader;

public abstract class JSCompressorResourceLoader extends CachingResourceLoader {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.compressor");
	
	private JSCompressor jsCompressor = null;
	private Pattern[] ignorePatterns = null;
	
	public JSCompressorResourceLoader(JSCompressorFactory jsCompressorFactory) {
		if (jsCompressorFactory != null) {
			jsCompressor = jsCompressorFactory.createJSCompressor(this);
			String[] ignoreList = jsCompressorFactory.getIgnoreList();
			ignorePatterns = new Pattern[ignoreList.length];
			int i = 0;
			for (String ignorePath : ignoreList) {
				ignorePatterns[i++] = Pattern.compile(ignorePath);
			}
		}
	}

	protected StringBuffer filter(StringBuffer sb, String path) throws IOException {
		if (doCompress(path)) {
			logger.logp(Level.FINE, getClass().getName(), "filter", "["+path+"] is being compressed");
			return new StringBuffer(jsCompressor.compress(sb.toString()));
		} else {
			return sb;
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
}
