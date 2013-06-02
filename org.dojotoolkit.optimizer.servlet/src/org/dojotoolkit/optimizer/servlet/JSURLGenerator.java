/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.servlet;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.dojotoolkit.json.JSONSerializer;
import org.dojotoolkit.optimizer.JSAnalysisData;
import org.dojotoolkit.optimizer.JSOptimizer;
import org.dojotoolkit.optimizer.Util;

public class JSURLGenerator {
	private boolean bootstrapwritten = false;
	private JSOptimizer jsOptimizer = null;
	private Locale locale = null;
	private String contextRoot = null;
	private List<JSAnalysisData> excludedList = null;
	
	public JSURLGenerator(JSOptimizer jsOptimizer, Locale locale, String contextRoot) {
		this.jsOptimizer = jsOptimizer;
		this.locale = locale;
		this.contextRoot = contextRoot;
		excludedList = new ArrayList<JSAnalysisData>();
	}
	
	public String generateURL(String module, Map<String, Object> pageConfig) {
		return generateURL(new String[] {module}, pageConfig, null);
	}
	
	public String generateURL(String module, Map<String, Object> pageConfig, HttpServletRequest request) {
		return generateURL(new String[] {module}, pageConfig, request);
	}
	
	public String generateURL(String[] modules, Map<String, Object> pageConfig) {
		return generateURL(modules, pageConfig, null);
	}
	
	public String generateURL(String[] modules, Map<String, Object> pageConfig, HttpServletRequest request) {
		if (request != null) {
			String baseUrl = pageConfig.containsKey("baseUrl") ? (String)pageConfig.get("baseUrl") : "./";
			if (baseUrl.charAt(0) != '/') {
				String fullPath = request.getRequestURI();
				baseUrl = fullPath.substring(0, fullPath.lastIndexOf('/')) + '/'+ baseUrl;
				baseUrl = Util.normalizePath(baseUrl);
				pageConfig.put("baseUrl", baseUrl);
			}
		}
		StringBuffer url = new StringBuffer();
		try {
			JSAnalysisData[] excludes = new JSAnalysisData[excludedList.size()];
			excludes = excludedList.toArray(excludes);
			JSAnalysisData analysisData = jsOptimizer.getAnalysisData(modules, excludes, pageConfig);
			url.append(contextRoot);
			url.append("/_javascript?key="+analysisData.getKey());
            url.append("&version=");
            url.append(analysisData.getChecksum());
            url.append("&locale=");
            url.append(locale);
            url.append("&config=");
            StringWriter sw = new StringWriter();
            JSONSerializer.serialize(sw, pageConfig);
            url.append(URLEncoder.encode(sw.toString(), "UTF-8"));
			if (bootstrapwritten) {
				url.append("&writeBootstrap=false");
			} else {
				bootstrapwritten = true;
			}
			excludedList.add(analysisData);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return url.toString();
	}
	
	public String[] generateDebugURLs(String module, Map<String, Object> pageConfig) {
		String[] urls = null;
		JSAnalysisData[] excludes = new JSAnalysisData[excludedList.size()];
		excludes = excludedList.toArray(excludes);
		try {
			JSAnalysisData analysisData = jsOptimizer.getAnalysisData(new String[] {module}, excludes, pageConfig);
			excludedList.add(analysisData);
			String[] dependencies = analysisData.getDependencies(); 
			List<String> l = new ArrayList<String>();
			for (String dependency : dependencies) {
				l.add(contextRoot+dependency);
			}
			urls = l.toArray(new String[0]);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return urls;
	}
}
