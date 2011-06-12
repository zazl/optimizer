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

import org.dojotoolkit.server.util.resource.CachingContentFilter;
import org.dojotoolkit.server.util.resource.ResourceLoader;

public class JSCompressorContentFilter extends CachingContentFilter {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.compressor");
	private JSCompressor jsCompressor = null;
	private Pattern[] ignorePatterns = null;

	public JSCompressorContentFilter(JSCompressorFactory jsCompressorFactory, ResourceLoader resourceLoader) {
		super(resourceLoader);
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
	
	protected String _runFilter(String content, String path) {
		if (doCompress(path)) {
			try {
				return jsCompressor.compress(content);
			} catch (IOException e) {
				logger.logp(Level.SEVERE, getClass().getName(), "_runFilter", "Unable to compress ["+path+"]", e);
				return content;
			}
		} else {
			return content;
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
