/*
    Copyright (c) 2004-2012, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.servlet;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dojotoolkit.json.JSONParser;
import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;
import org.dojotoolkit.server.util.rhino.RhinoJSMethods;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class ScriptAnalyzer {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer.servlet");
	
	private ResourceLoader resourceLoader = null;
	private RhinoClassLoader rhinoClassLoader = null;
	
	public ScriptAnalyzer(ResourceLoader resourceLoader, RhinoClassLoader rhinoClassLoader) {
		this.resourceLoader = resourceLoader;
		this.rhinoClassLoader = rhinoClassLoader;
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> analyze(String script) throws IOException {
		Map<String, Object> results = null;
		StringBuffer sb = new StringBuffer();
        sb.append("loadJS('/jsutil/commonjs/loader.js');\n");
		sb.append("loadJS('/json/json2.js');\n");
		sb.append("var analyzer = require('taganalyzer/analyzer');\n");
		sb.append("var results = analyzer.analyze('");
		sb.append(escape(script));
		sb.append("');\n");
		sb.append("JSON.stringify(results, null, '\t');\n");

		Context ctx = null; 
		try {
			long start = System.currentTimeMillis();
			ctx = Context.enter();
			ScriptableObject scope = ctx.initStandardObjects();
			RhinoJSMethods.initScope(scope, resourceLoader, rhinoClassLoader, false);
			Object o = ctx.evaluateString(scope, sb.toString(), "ScriptAnalyzer", 1, null);
			results = (Map<String, Object>)JSONParser.parse(new StringReader((String)o));
			long end = System.currentTimeMillis();
			logger.logp(Level.FINE, getClass().getName(), "analyze", "time : "+(end-start)+" ms for ["+sb+"]");
		}
		catch(Throwable t) {
			logger.logp(Level.SEVERE, getClass().getName(), "analyze", "Exception on analyze", t);
			throw new IOException("Exception on analyze for ["+sb+"] : "+t.getMessage());
		}
		finally {
			Context.exit();
		}
		return results;
	}
	
	private static String escape(String s) {
		StringBuffer escaped = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '\'': {
					escaped.append("\\'");
					break;
				}
				case '\r': 
				case '\n': {
					escaped.append(" ");
					break;
				}
				default: {
					escaped.append(c);
					break;
				}
			}
		}
		return escaped.toString();
	}
}
