/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.servlet;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
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
	
	@SuppressWarnings("unchecked")
	protected void customHandle(HttpServletRequest request, Writer writer, JSAnalysisData analysisData) throws ServletException, IOException {
		if (analysisData != null) {	
			String suffixCode = (String)config.get("suffixCode");
			if (suffixCode != null) {
				writer.write(suffixCode);
			}
			String[] dependencies = analysisData.getDependencies();
			Map<String, List<Map<String, String>>> pluginRefs = analysisData.getPluginRefs();
			List<Map<String, Object>> modulesMissingNames = analysisData.getModulesMissingNames();
			List<Localization> localizations = new ArrayList<Localization>();
			String i18nPluginId = (String)config.get("i18nPluginId");
			for (String pluginId : pluginRefs.keySet()) {
				boolean writePlugin = true;
				for (String dependency : dependencies) {
					if (dependency.equals(pluginId+".js")) {
						writePlugin = false;
						break;
					}
				}
				if (writePlugin) {
					String pluginContent = resourceLoader.readResource(pluginId+".js");
					if (pluginContent != null) {
						int missingNameIndex = lookForMissingName(pluginId, modulesMissingNames);
						if (missingNameIndex != -1) {
							StringBuffer modifiedSrc = new StringBuffer(pluginContent.substring(0, missingNameIndex));
							modifiedSrc.append("'"+pluginId+"', ");
							modifiedSrc.append(pluginContent.substring(missingNameIndex));
							pluginContent = modifiedSrc.toString();
						}
						
						writer.write(compressorContentFilter.filter(pluginContent, pluginId+".js"));
					}
				}
				List<Map<String, String>> pluginRefInstances = pluginRefs.get(pluginId);
				for (Map<String, String> pluginRefInstance : pluginRefInstances) {
					String value = pluginRefInstance.get("value");
					if (value != null) {
						writer.write(value);
					}
				}
				if (i18nPluginId != null && i18nPluginId.equals(pluginId)) {
					for (Map<String, String> pluginRefInstance : pluginRefInstances) {
						String bundlePackage = pluginRefInstance.get("normalizedName");
						String modulePath = bundlePackage.substring(0, bundlePackage.lastIndexOf('/'));
						String bundleName = bundlePackage.substring(bundlePackage.lastIndexOf('/')+1);	
						Localization localization = new Localization(bundlePackage, modulePath, bundleName);
						localizations.add(localization);
					}
				}
			}
			if (localizations.size() > 0) {
				Util.writeAMDLocalizations(resourceLoader, writer, localizations, request.getLocale());
			}
			Map<String, Object> aliases = (Map<String, Object>)config.get("aliases");
			for (String dependency : dependencies) {
				String path = Util.normalizePath(dependency);
				String content = resourceLoader.readResource(path);
				if (content != null) {
					String id = dependency.substring(0, dependency.indexOf(".js"));
					int missingNameIndex = lookForMissingName(id, analysisData.getModulesMissingNames());
					if (missingNameIndex != -1) {
		                for (Iterator<String> itr = aliases.keySet().iterator(); itr.hasNext();) {
		                	String aliasKey = itr.next();
		                	String alias = (String)aliases.get(aliasKey);
		                	if (alias.equals(id)) {
		                		id = aliasKey;
		                		break;
		                	}
		                }
						StringBuffer modifiedSrc = new StringBuffer(content.substring(0, missingNameIndex));
						modifiedSrc.append("'"+id+"', ");
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
}
