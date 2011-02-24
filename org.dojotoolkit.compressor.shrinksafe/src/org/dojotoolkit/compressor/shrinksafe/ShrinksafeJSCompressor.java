/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.compressor.shrinksafe;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dojotoolkit.compressor.JSCompressor;
import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.dojotoolkit.shrinksafe.Compressor;
import org.mozilla.javascript.Context;

public class ShrinksafeJSCompressor implements JSCompressor {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.compressor");
	
	public ShrinksafeJSCompressor(ResourceLoader resourceLoader) {}
	
	public String compress(String src) throws IOException {
		long start = System.currentTimeMillis();
		Context ctx = null; 
		try {
			ctx = Context.enter();
			ctx.initStandardObjects();
			String compressedSrc = Compressor.compressScript(src, 0, 1, null);
			long end = System.currentTimeMillis();
			logger.logp(Level.FINE, getClass().getName(), "compress", "time : "+(end-start)+" ms");
			return compressedSrc;
		} finally {
			Context.exit();
		}
	}
}
