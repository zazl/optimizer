/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.dojotoolkit.optimizer.JSAnalysisData;
import org.dojotoolkit.optimizer.JSOptimizer;

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
	
	public String generateURL(String module) {
		StringBuffer url = new StringBuffer();
		try {
			JSAnalysisData[] excludes = new JSAnalysisData[excludedList.size()];
			excludes = excludedList.toArray(excludes);
			JSAnalysisData analysisData = jsOptimizer.getAnalysisData(new String[] {module}, excludes);
			url.append(contextRoot);
			url.append("/_javascript?modules=");
			url.append(module);
            url.append("&version=");
            url.append(analysisData.getChecksum());
            url.append("&locale=");
            url.append(locale);
			if (bootstrapwritten) {
				url.append("&writeBootstrap=false");
			} else {
				bootstrapwritten = true;
			}
			if (excludedList.size() > 0) {
				url.append("&exclude=");
				int count = 0;
				for (JSAnalysisData excluded : excludedList) {
					url.append(excluded.getKey());
					if (++count < excludedList.size()) {
						url.append(",");
					}
				}
			}
			excludedList.add(analysisData);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return url.toString();
	}
	
	public String[] generateDebugURLs(String module) {
		String[] urls = null;
		JSAnalysisData[] excludes = new JSAnalysisData[excludedList.size()];
		excludes = excludedList.toArray(excludes);
		try {
			JSAnalysisData analysisData = jsOptimizer.getAnalysisData(new String[] {module}, excludes);
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
