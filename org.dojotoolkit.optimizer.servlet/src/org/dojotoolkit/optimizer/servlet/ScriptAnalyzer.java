/*
    Copyright (c) 2004-2012, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.servlet;

import java.io.IOException;
import java.util.Map;

import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;

public abstract class ScriptAnalyzer {
	protected ResourceLoader resourceLoader = null;
	protected RhinoClassLoader rhinoClassLoader = null;
	
	public ScriptAnalyzer(ResourceLoader resourceLoader, RhinoClassLoader rhinoClassLoader) {
		this.resourceLoader = resourceLoader;
		this.rhinoClassLoader = rhinoClassLoader;
	}
	
	public abstract Map<String, Object> analyze(String script) throws IOException;
	
	protected static String escape(String s) {
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
