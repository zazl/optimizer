/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
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

public class ServletResourceLoader extends CachingResourceLoader {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer");
	private ServletContext servletContext = null;
	private String contextPath = null;

	public ServletResourceLoader(ServletContext servletContext) {
		this.servletContext = servletContext;
		this.contextPath = servletContext.getContextPath();
	}
	
	protected URL _getResource(String path) throws IOException {
		if (path.charAt(0) != '/') {
			path = '/'+path;
		}
		URL url = servletContext.getResource(path);
		if (url != null) {
			logger.logp(Level.FINE, getClass().getName(), "_getResource", "servlet context["+path+"] ["+url+"]");
			return url;
		}
		url = getClass().getClassLoader().getResource(path);	
		if (url != null) {
			logger.logp(Level.FINE, getClass().getName(), "_getResource", "classloader getResource["+path+"] ["+url+"]");
			return url;
		}
		url = getClass().getClassLoader().getResource(path.substring(1));
		if (url != null) {
			logger.logp(Level.FINE, getClass().getName(), "_getResource", "classloader getResource + 1["+path.substring(1)+"] ["+url+"]");
			return url;
		}
		if (path.startsWith(contextPath)) {
			path = path.substring(contextPath.length());
			url = servletContext.getResource(path);
			if (url != null) {
				logger.logp(Level.FINE, getClass().getName(), "_getResource", "servlet context - context path["+path+"] ["+url+"]");
			}
		}
		return url;
	}
}
