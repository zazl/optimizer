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
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.dojotoolkit.optimizer.JSAnalysisData;
import org.dojotoolkit.optimizer.Localization;
import org.dojotoolkit.optimizer.Util;

public class AMDJSHandler extends JSHandler {
	
	public AMDJSHandler(String configFileName) {
		super(configFileName);
	}
	
	protected void customHandle(HttpServletRequest request, Writer writer, JSAnalysisData analysisData) throws ServletException, IOException {
		if (analysisData != null) {	
			String suffixCode = (String)config.get("suffixCode");
			if (suffixCode != null) {
				writer.write(suffixCode);
			}
			String[] dependencies = analysisData.getDependencies();
			Map<String, List<Map<String, String>>> pluginRefs = analysisData.getPluginRefs();
			List<Localization> localizations = new ArrayList<Localization>();
			String i18nPluginId = (String)config.get("i18nPluginId");
			for (String pluginId : pluginRefs.keySet()) {
				List<Map<String, String>> pluginRefInstances = pluginRefs.get(pluginId);
				for (Map<String, String> pluginRefInstance : pluginRefInstances) {
					String value = pluginRefInstance.get("value");
					if (value != null) {
						writer.write(value);
					}
				}
				if (i18nPluginId != null && i18nPluginId.equals(pluginId)) {
					List<String> seen = new ArrayList<String>();
					for (Map<String, String> pluginRefInstance : pluginRefInstances) {
						String bundlePackage = pluginRefInstance.get("normalizedName");
						if (!seen.contains(bundlePackage)) {
							seen.add(bundlePackage);
							String modulePath = bundlePackage.substring(0, bundlePackage.lastIndexOf('/'));
							String bundleName = bundlePackage.substring(bundlePackage.lastIndexOf('/')+1);	
							Localization localization = new Localization(bundlePackage, modulePath, bundleName);
							localizations.add(localization);
						}
					}
				}
			}
			if (localizations.size() > 0) {
				Util.writeAMDLocalizations(resourceLoader, writer, localizations, request.getLocale());
			}
			for (String dependency : dependencies) {
				String path = Util.normalizePath(dependency);
				String content = resourceLoader.readResource(path);
				if (content != null) {
					String uri = dependency.substring(0, dependency.indexOf(".js"));
					int missingNameIndex = lookForMissingName(uri, analysisData.getModulesMissingNames());
					if (missingNameIndex != -1) {
						StringBuffer modifiedSrc = new StringBuffer(content.substring(0, missingNameIndex));
						modifiedSrc.append("'"+getMissingNameId(uri, analysisData.getModulesMissingNames())+"', ");
						modifiedSrc.append(content.substring(missingNameIndex));
						content = modifiedSrc.toString();
					}
					
					writer.write(compressorContentFilter.filter(content,path));
				}
			}
		}
	}
	
	private int lookForMissingName(String uri, List<Map<String, Object>> modulesMissingNamesList) {
		int index = -1;
		for (Map<String, Object> modulesMissingNames : modulesMissingNamesList) {
			if (modulesMissingNames.get("uri").equals(uri)) {
				index = ((Long)modulesMissingNames.get("nameIndex")).intValue();
				break;
			}
		}
		return index;
	}

	private String getMissingNameId(String uri, List<Map<String, Object>> modulesMissingNamesList) {
		String id = null;
		for (Map<String, Object> modulesMissingNames : modulesMissingNamesList) {
			if (modulesMissingNames.get("uri").equals(uri)) {
				id = ((String)modulesMissingNames.get("id"));
				break;
			}
		}
		return id;
	}
}
