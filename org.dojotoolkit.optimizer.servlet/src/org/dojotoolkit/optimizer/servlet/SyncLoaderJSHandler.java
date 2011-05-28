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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.dojotoolkit.optimizer.JSAnalysisData;
import org.dojotoolkit.optimizer.Localization;
import org.dojotoolkit.optimizer.Util;

public class SyncLoaderJSHandler extends JSHandler {
	private static final String NAMESPACE_PREFIX = "dojo.registerModulePath('";
	private static final String NAMESPACE_MIDDLE = "', '";
	private static final String NAMESPACE_SUFFIX = "');\n";

	public SyncLoaderJSHandler() {
		super("syncloader.json");
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
			String suffixCode = (String)config.get("suffixCode");
			if (suffixCode != null) {
				writer.write(suffixCode);
			}
			boolean writeBootstrap = (request.getParameter("writeBootstrap") == null) ? true : Boolean.valueOf(request.getParameter("writeBootstrap"));
			if (writeBootstrap) {
				writer.write(resourceLoader.readResource("/optimizer/syncloader/localization.js"));
			}
			List<Localization> localizations = analysisData.getLocalizations();
			if (localizations.size() > 0) {
				Util.writeLocalizations(resourceLoader, writer, localizations, request.getLocale());
			}
			
			String[] dependencies = analysisData.getDependencies();
			
			for (String dependency : dependencies) {
				String contentElement = resourceLoader.readResource(Util.normalizePath(dependency));
				if (contentElement != null) {
					writer.write(contentElement);
				}
			}
		}
	}
	
	public class JSNamespace {
		public String namespace = null;
		public String prefix = null;
	}
}
