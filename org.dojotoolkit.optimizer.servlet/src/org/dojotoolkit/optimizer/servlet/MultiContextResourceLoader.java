/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.servlet;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.dojotoolkit.server.util.resource.CachingResourceLoader;

public class MultiContextResourceLoader extends CachingResourceLoader {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer");
	private ContextData[] contexts = null;

	public MultiContextResourceLoader(ContextData[] contexts) {
		this.contexts = contexts;
		for (ContextData context : contexts) {
			logger.logp(Level.INFO, getClass().getName(), "MultiContextResourceLoader", "Context root ["+context.contextRoot+"] base ["+context.base+"] will be searched for resources");
		}
	}

	protected URL _getResource(String path) throws IOException {
		if (path.charAt(0) != '/') {
			path = '/'+path;
		}
		URL url = null;
		for (ContextData context : contexts) {
			url = context.servletContext.getResource(context.base + path);
			if (url != null) {
				logger.logp(Level.FINE, getClass().getName(), "_getResource", "["+context.contextRoot+"] ["+context.base+"] ["+path+"] ["+url+"]");
				return url;
			}
		}
		url = getClass().getClassLoader().getResource(path);	
		if (url != null) {
			logger.logp(Level.FINE, getClass().getName(), "_getResource", "["+path+"] ["+url+"]");
			return url;
		}
		url = getClass().getClassLoader().getResource(path.substring(1));	
		logger.logp(Level.FINE, getClass().getName(), "_getResource", "["+path.substring(1)+"] ["+url+"]");
		return url;
	}
	
	public static class ContextData {
		public ServletContext servletContext = null;
		public String contextRoot = null;
		public String base = null;
		
		public ContextData(ServletContext servletContext, String contextRoot, String base) {
			this.servletContext = servletContext;
			this.contextRoot = contextRoot;
			this.base = base;
		}
	}
}
