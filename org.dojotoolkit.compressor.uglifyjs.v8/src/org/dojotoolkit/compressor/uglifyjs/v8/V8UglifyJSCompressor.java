/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.compressor.uglifyjs.v8;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dojotoolkit.compressor.JSCompressor;
import org.dojotoolkit.json.JSONSerializer;
import org.dojotoolkit.rt.v8.V8JavaBridge;
import org.dojotoolkit.server.util.resource.ResourceLoader;

public class V8UglifyJSCompressor extends V8JavaBridge implements JSCompressor {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.compressor");
	private ResourceLoader resourceLoader = null;
	private String src = null;
	
	public V8UglifyJSCompressor(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}
	
	public String compress(String src) throws IOException {
		this.src = src;
		String compressedSrc = null;
		StringBuffer sb = new StringBuffer();
        sb.append("loadJS('/jsutil/commonjs/loader.js');\n");
        sb.append("var compressor = require('uglifyjs/compressor');\n");
        sb.append("var srcObj = eval('('+readSrc({})+')');\n");
        sb.append("'{\"compressedSrc\": '+compressor.compress(srcObj.src, true)+'}';\n");
        try {
			long start = System.currentTimeMillis();
			Map<String, Object> map = (Map<String, Object>)runScript(sb.toString(), new String[]{"readSrc"});
			compressedSrc = (String)map.get("compressedSrc");
			long end = System.currentTimeMillis();
			logger.logp(Level.FINE, getClass().getName(), "compress", "time : "+(end-start)+" ms");
		} catch (Throwable e) {
			if (compileErrors.size() > 0) {
				for (Throwable t : compileErrors) {
					t.printStackTrace();
				}
			}
			logger.logp(Level.SEVERE, getClass().getName(), "compress", "Exception on compress for ["+sb+"]", e);
			throw new IOException("Exception on compress for ["+sb+"] : "+e.getMessage());
		}
        
        if (compressedSrc.charAt(compressedSrc.length()-1) == ')') {
        	compressedSrc += ";";
        }
		return compressedSrc;
	}

	public String readResource(String path) throws IOException {
		return resourceLoader.readResource(path);
	}
	
	public String readSrc(String json) {
		Map m = new HashMap();
		m.put("src", src);
		StringWriter w = new StringWriter();
		try {
			JSONSerializer.serialize(w, m);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return w.toString();
	}
}
