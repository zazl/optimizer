/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.servlet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dojotoolkit.optimizer.JSAnalysisData;
import org.dojotoolkit.optimizer.JSOptimizer;
import org.dojotoolkit.optimizer.Localization;
import org.dojotoolkit.optimizer.Util;
import org.dojotoolkit.server.util.resource.ResourceLoader;

public class JSHandler {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer");
	private static final String NAMESPACE_PREFIX = "dojo.registerModulePath('";
	private static final String NAMESPACE_MIDDLE = "', '";
	private static final String NAMESPACE_SUFFIX = "');\n";
	
	protected JSOptimizer jsOptimizer = null;
	protected ResourceLoader resourceLoader = null;
	protected String[] bootstrapModules = null;
	protected String[] debugBootstrapModules = null;
	
	public JSHandler() {
	}

	public void initialize(ResourceLoader resourceLoader, JSOptimizer jsOptimizer, String[] bootstrapModules, String[] debugBootstrapModules) {
		this.resourceLoader = resourceLoader;
		this.jsOptimizer = jsOptimizer;
		this.bootstrapModules = bootstrapModules;
		this.debugBootstrapModules = debugBootstrapModules;
	}
	
	public boolean handle(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String[] modules = null;
		JSNamespace[] namespaces = null;
		String modulesParam = request.getParameter("modules");
		String url = request.getPathInfo();
		if (url != null && url.startsWith("/_javascript")) {
			url = url.substring("/_javascript".length());
		}
		String[] bootstrapModulePaths = bootstrapModules;
		boolean debug = (request.getParameter("debug") == null) ? false : Boolean.valueOf(request.getParameter("debug"));
		if (debug) {
			bootstrapModulePaths = debugBootstrapModules;
		}
		
		boolean gzip = false;
		
		String encoding = request.getHeader("Accept-Encoding");
		if (encoding!=null && encoding.indexOf("gzip") >= 0) {
			gzip = true;
		}
		
		String namespacesParam = request.getParameter("namespaces");
		if (namespacesParam != null) {
			List<JSNamespace> namespaceList = new ArrayList<JSNamespace>();
			StringTokenizer st = new StringTokenizer(namespacesParam, ",");
			while (st.hasMoreTokens()) {
				String[] namespace = st.nextToken().split(":");
				if (namespace.length > 1) {
					JSNamespace jsNamespace = new JSNamespace();
					jsNamespace.namespace = namespace[0];
					jsNamespace.prefix = namespace[1];
					namespaceList.add(jsNamespace);
				}
			}
			namespaces = new JSNamespace[namespaceList.size()];
			namespaces = namespaceList.toArray(namespaces);
		}
		
		response.setContentType("text/javascript; charset=UTF-8");
		
		if (modulesParam != null) {
			modules = getModuleList(modulesParam);
			JSAnalysisData analysisData;
			try {
				analysisData = jsOptimizer.getAnalysisData(modules);
				if (logger.getLevel() == Level.FINE) {
					logger.logp(Level.FINE, getClass().getName(), "handle", "checksum for ["+modulesParam+"] = "+analysisData.getChecksum());
					for (String dependency : analysisData.getDependencies()) {
						logger.logp(Level.FINER, getClass().getName(), "handle", "dependency for ["+modulesParam+"] = ["+dependency+"]");
					}
					for (Localization localization : analysisData.getLocalizations()) {
						logger.logp(Level.FINER, getClass().getName(), "handle", "localization for ["+modulesParam+"] = ["+localization.modulePath+"]");
					}
				}
			} catch (IOException e) {
				logger.logp(Level.SEVERE, getClass().getName(), "handle", "Exception on request for ["+modulesParam+"]", e);
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
				return true;
			}
			if (!debug) {
				String checksum = analysisData.getChecksum();
			    String ifNoneMatch = request.getHeader("If-None-Match");
	
			    if (ifNoneMatch != null && ifNoneMatch.equals(checksum)) {
			    	response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			        return true;
			    }
			    
				if (modules == null) {
					modules = getModuleList(modulesParam);
				}
	 			response.setHeader("ETag", checksum);
	 			
				String version = request.getParameter("version");
				if (version != null && version.equals(checksum)) {
					Calendar calendar = Calendar.getInstance();
					calendar.add(Calendar.YEAR, 1);
					response.setDateHeader("Expires", calendar.getTimeInMillis());
				}
			}
 			Writer osw = null;
 			GZIPOutputStream gz = null;
 			try {
	 			if (gzip) {
	 				response.setHeader("Content-Encoding","gzip");
	                gz = new GZIPOutputStream(response.getOutputStream());
		 			osw = new BufferedWriter(new OutputStreamWriter(gz, "UTF-8"));
	 			} else {
		 			osw = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
	 			}
	 			for (String bootstrapModulePath: bootstrapModulePaths) {
		 			osw.write(resourceLoader.readResource(bootstrapModulePath));
	 			}
	 			if (namespaces != null) {
	 				for (JSNamespace jsNamespace : namespaces) {
	 					String s = NAMESPACE_PREFIX+jsNamespace.namespace+NAMESPACE_MIDDLE+jsNamespace.prefix+NAMESPACE_SUFFIX; 
	 					osw.write(s);
	 				}
	 			}
				
				List<Localization> localizations = analysisData.getLocalizations();
				if (localizations.size() > 0) {
					try {
						Util.writeLocalizations(resourceLoader, osw, localizations, request.getLocale());
					} catch (IOException e) {
						logger.logp(Level.SEVERE, getClass().getName(), "handle", "Exception on request for ["+modulesParam+"]", e);
						response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
						return true;
					}
				}
				
				String[] dependencies = analysisData.getDependencies();
				
				for (String dependency : dependencies) {
					String contentElement = resourceLoader.readResource(Util.normalizePath(dependency));
					if (contentElement != null) {
						osw.write(contentElement);
					}
				}
 			} catch (IOException e) {
				logger.logp(Level.SEVERE, getClass().getName(), "handle", "Exception on request for ["+modulesParam+"]", e);
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
				return true;
 			} finally {
 				osw.flush();
 	 			if (gzip) {
 	 				gz.close();
 	 			}
 			}
		} else {
 			if (gzip) {
 				response.setHeader("Content-Encoding","gzip");
 				OutputStream os = response.getOutputStream();
                GZIPOutputStream gz=new GZIPOutputStream(os);
	 			for (String bootstrapModulePath: bootstrapModulePaths) {
	 				gz.write(resourceLoader.readResource(bootstrapModulePath).getBytes("UTF-8"));
	 			}
	 			if (namespaces != null) {
	 				for (JSNamespace jsNamespace : namespaces) {
	 					String s = NAMESPACE_PREFIX+jsNamespace.namespace+NAMESPACE_MIDDLE+jsNamespace.prefix+NAMESPACE_SUFFIX; 
	 					gz.write(s.getBytes("UTF-8"));
	 				}
	 			}
                gz.close();
 			} else {
	 			for (String bootstrapModulePath: bootstrapModulePaths) {
	 				response.getWriter().write(resourceLoader.readResource(bootstrapModulePath));
	 			}
	 			if (namespaces != null) {
	 				for (JSNamespace jsNamespace : namespaces) {
	 					response.getWriter().write(NAMESPACE_PREFIX+jsNamespace.namespace+NAMESPACE_MIDDLE+jsNamespace.prefix+NAMESPACE_SUFFIX); 
	 				}
	 			}
 			}
		}
        return true;
	}
	
	private String[] getModuleList(String modulesParam) {
		String[] modules = null;
		List<String> moduleList = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(modulesParam, ",");
		while (st.hasMoreTokens()) {
			moduleList.add(st.nextToken());
		}
		modules = new String[moduleList.size()];
		modules = moduleList.toArray(modules);
		return modules;
	}
	
	public class JSNamespace {
		public String namespace = null;
		public String prefix = null;
	}
}
