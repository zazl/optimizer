/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.servlet;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.dojotoolkit.optimizer.JSAnalysisData;
import org.dojotoolkit.optimizer.Localization;
import org.dojotoolkit.optimizer.Util;
import org.dojotoolkit.server.util.resource.ResourceLoader;

public class SyncLoaderJSHandler extends JSHandler {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer");
	
	private static final String NAMESPACE_PREFIX = "dojo.registerModulePath('";
	private static final String NAMESPACE_MIDDLE = "', '";
	private static final String NAMESPACE_SUFFIX = "');\n";
	private static Pattern templatePattern = Pattern.compile("(((templatePath)\\s*(=|:)\\s*)dojo\\.(module)?Url\\(|dojo\\.cache\\s*\\(\\s*)\\s*?[\\\"\\']([\\w\\.\\/]+)[\\\"\\'](([\\,\\s]*)[\\\"\\']([\\w\\.\\/]*)[\\\"\\'])?(\\s*,\\s*)?([^\\)]*)?\\s*\\)");
	private static Pattern dojoRequirePattern = Pattern.compile("^\\s*dojo\\.require\\s*\\([\\w\\W]*?\\);", Pattern.MULTILINE);
	private boolean inlineTemplateHTML = true;
	private boolean removeDojoRequires = false;
	
	public SyncLoaderJSHandler() {
		super("syncloader.json");
	}
	
	public SyncLoaderJSHandler(boolean inlineTemplateHTML, boolean removeDojoRequires) {
		super("syncloader.json");
		this.inlineTemplateHTML = inlineTemplateHTML;
		this.removeDojoRequires = removeDojoRequires;
	}
	
	protected void customHandle(HttpServletRequest request, Writer writer, JSAnalysisData analysisData) throws ServletException, IOException {
		JSNamespace[] namespaces = null;
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
			for (JSNamespace jsNamespace : namespaces) {
				String s = NAMESPACE_PREFIX+jsNamespace.namespace+NAMESPACE_MIDDLE+jsNamespace.prefix+NAMESPACE_SUFFIX; 
				writer.write(s);
			}
		}
		
		if (analysisData != null) {	
			boolean writeBootstrap = (request.getParameter("writeBootstrap") == null) ? true : Boolean.valueOf(request.getParameter("writeBootstrap"));
			if (writeBootstrap) {
				String suffixCode = (String)config.get("suffixCode");
				if (suffixCode != null) {
					writer.write(suffixCode);
				}
				writer.write(resourceLoader.readResource("/optimizer/syncloader/localization.js"));
			}
			List<Localization> localizations = analysisData.getLocalizations();
			if (localizations.size() > 0) {
				Util.writeLocalizations(resourceLoader, writer, localizations, request.getLocale());
			}
			
			String[] dependencies = analysisData.getDependencies();
			
			for (String dependency : dependencies) {
				String path = Util.normalizePath(dependency);
				String contentElement = resourceLoader.readResource(path);
				if (contentElement != null) {
					if (removeDojoRequires) {
						contentElement = removeDojoRequireCalls(dependency, contentElement);
					}
					if (inlineTemplateHTML) {
						contentElement = inlineTemplateHTML(dependency, contentElement, resourceLoader);
					}
					writer.write(compressorContentFilter.filter(contentElement, path));
				}
			}
		}
	}
	
	private static String inlineTemplateHTML(String dependency, String input, ResourceLoader resourceLoader) {
		StringBuffer inlinedWithHTML = new StringBuffer();
		Matcher m = templatePattern.matcher(input);
		while (m.find()) {
			if (m.groupCount() == 11 && m.group(11).equals("")) {
				String namespace = m.group(6);
				String templateURLStr = m.group(9);
				String url = namespace.replace('.', '/') + '/' + templateURLStr;
				try {
					String templateHTML = resourceLoader.readResource(url);
					if (templateHTML != null) {
						if (m.group(1).startsWith("dojo.cache")) {
							logger.logp(Level.FINE, SyncLoaderJSHandler.class.getName(), "inlineTemplateHTML", "Inlining Template HTML(dojo.cache) for ["+dependency+"] ["+url+"]");
							m.appendReplacement(inlinedWithHTML, quoteReplacement(" dojo.cache(\""+namespace+"\", \""+templateURLStr+"\", \"" + escape(templateHTML) + "\")"));
						} else if (m.group(1).startsWith("templatePath")) {
							logger.logp(Level.FINE, SyncLoaderJSHandler.class.getName(), "inlineTemplateHTML", "Inlining Template HTML(dojo.moduleUrl) for ["+dependency+"] ["+url+"]");
							m.appendReplacement(inlinedWithHTML, quoteReplacement("templateString "+m.group(4)+" \"" + escape(templateHTML) + "\""));
						}
					} else {
						logger.logp(Level.INFO, SyncLoaderJSHandler.class.getName(), "inlineTemplateHTML", "Unable to inline Template HTML for ["+dependency+"] ["+url+"]");
					}
				} catch (IOException e) {
					logger.logp(Level.SEVERE, SyncLoaderJSHandler.class.getName(), "inlineTemplateHTML", "Exception on inlining Template HTML for ["+dependency+"] ["+url+"]", e);
				}
			}
		}
		if (inlinedWithHTML.length() > 0) {
			m.appendTail(inlinedWithHTML);
			return inlinedWithHTML.toString();
		} 
		else {
			return input;
		}
	}
	
	private static String removeDojoRequireCalls(String dependency, String input) {
		StringBuffer removedDojoProvides = new StringBuffer();
		Matcher m = dojoRequirePattern.matcher(input);
		while (m.find()) {
			logger.logp(Level.FINE, SyncLoaderJSHandler.class.getName(), "removeDojoRequireCalls", "Removing ["+m.group(0)+"] from ["+dependency+"]");
			m.appendReplacement(removedDojoProvides, "");
		}
		if (removedDojoProvides.length() > 0) {
			m.appendTail(removedDojoProvides);
			return removedDojoProvides.toString();
		} 
		else {
			return input;
		}
	}
	
	private static String escape(String s) {
		StringBuffer escaped = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '\"': {
					escaped.append("\\\""); //$NON-NLS-1$
					break;
				}
				case '\r': 
				case '\n': {
					escaped.append(" "); //$NON-NLS-1$
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
	
	private static String quoteReplacement(String s) {
		if ((s.indexOf('\\') == -1) && (s.indexOf('$') == -1))
			return s;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\\') {
				sb.append('\\');
				sb.append('\\');
			} else if (c == '$') {
				sb.append('\\');
				sb.append('$');
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
	
	public class JSNamespace {
		public String namespace = null;
		public String prefix = null;
	}
}
