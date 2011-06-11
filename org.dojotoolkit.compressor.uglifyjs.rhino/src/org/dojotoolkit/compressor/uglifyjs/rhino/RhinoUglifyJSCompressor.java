/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.compressor.uglifyjs.rhino;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dojotoolkit.compressor.JSCompressor;
import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;
import org.dojotoolkit.server.util.rhino.RhinoJSMethods;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class RhinoUglifyJSCompressor implements JSCompressor {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.compressor");
	private static final String READ_SRC = "readSrc"; //$NON-NLS-1$
	private static final String SRC = "src"; //$NON-NLS-1$

	private RhinoClassLoader rhinoClassLoader = null;
	private ResourceLoader resourceLoader = null;

	public RhinoUglifyJSCompressor(ResourceLoader resourceLoader) {
		this(resourceLoader, new RhinoClassLoader(resourceLoader, RhinoClassLoader.class.getClassLoader()));
	}

	public RhinoUglifyJSCompressor(ResourceLoader resourceLoader, RhinoClassLoader rhinoClassLoader) {
		this.resourceLoader = resourceLoader;
		this.rhinoClassLoader = rhinoClassLoader;
	}

	public String compress(String src) throws IOException {
		String compressedSrc = null;
		Context ctx = null; 
		StringBuffer sb = new StringBuffer();
        sb.append("loadJS('/jsutil/commonjs/loader.js');\n");
        sb.append("var compressor = require('uglifyjs/compressor');\n");
        sb.append("compressor.compress(readSrc(), false);\n");
		try {
			ctx = Context.enter();
			ScriptableObject scope = ctx.initStandardObjects();
			initScope(scope, resourceLoader, src);
			long start = System.currentTimeMillis();
			compressedSrc = Context.toString(ctx.evaluateString(scope, sb.toString(), "RhinoJSCompressor", 1, null));//$NON-NLS-1$
			long end = System.currentTimeMillis();
			logger.logp(Level.FINE, getClass().getName(), "compress", "time : "+(end-start)+" ms");
		}
		catch(Throwable t) {
			logger.logp(Level.SEVERE, getClass().getName(), "compress", "Exception on compress for ["+sb+"]", t);
			throw new IOException("Exception on compress for ["+sb+"] : "+t.getMessage());
		}
		finally {
			Context.exit();
		}
        if (compressedSrc.charAt(compressedSrc.length()-1) == ')') {
        	compressedSrc += ";";
        }
		return compressedSrc;
	}

	private void initScope(ScriptableObject scope, ResourceLoader resourceLoader, String src) {
		RhinoJSMethods.initScope(scope, resourceLoader, rhinoClassLoader, false);
    	Method[] methods = getClass().getMethods();
    	for (int i = 0; i < methods.length; i++) {
    		if (methods[i].getName().equals(READ_SRC)) {
    			FunctionObject f = new FunctionObject(READ_SRC, methods[i], scope);
    			scope.defineProperty(READ_SRC, f, ScriptableObject.DONTENUM);
    		}    	
    	}
	    scope.associateValue(SRC, src);
	}
	
	public static Object readSrc(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
    	String src = (String)((ScriptableObject)thisObj).getAssociatedValue(SRC);
		return src;
	}
}
