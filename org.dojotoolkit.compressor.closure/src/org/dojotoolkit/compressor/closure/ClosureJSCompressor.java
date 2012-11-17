/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.compressor.closure;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dojotoolkit.compressor.JSCompressor;
import org.dojotoolkit.server.util.resource.ResourceLoader;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;

public class ClosureJSCompressor implements JSCompressor {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.compressor");
	private Map<String, String> sourceMapMap = null;
	
	public ClosureJSCompressor(ResourceLoader resourceLoader) {
		sourceMapMap = new HashMap<String, String>();
	}
	
	public String compress(String path, String src) throws IOException {
		Compiler compiler = new Compiler();
		CompilerOptions options = new CompilerOptions();
		CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
		//CompilationLevel.WHITESPACE_ONLY.setOptionsForCompilationLevel(options);
		options.sourceMapOutputPath = "";
		options.setLineLengthThreshold(1024 * 64);
		SourceFile extern = SourceFile.fromCode("externs.js", "");
		SourceFile input = SourceFile.fromCode(path, src);
		long start = System.currentTimeMillis();
		Result result = compiler.compile(extern, input, options);
		if (!result.success) {
			StringBuffer sb = new StringBuffer();
			for (JSError error : result.errors) {
				sb.append(error.toString());
				sb.append("\n");
			}
			throw new IOException(sb.toString());
		}
		String compressedSrc = compiler.toSource();
		long end = System.currentTimeMillis();
		StringBuffer sb = new StringBuffer();
		result.sourceMap.appendTo(sb, path);
		sourceMapMap.put(path+".map", sb.toString());
		logger.logp(Level.FINE, getClass().getName(), "compress", "time : "+(end-start)+" ms");
		return compressedSrc;
	}
	
	public String getSourceMap(String path) {
		return sourceMapMap.get(path);
	}
}
