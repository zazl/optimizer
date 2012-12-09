/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.dojotoolkit.optimizer.JSAnalysisData;
import org.dojotoolkit.optimizer.Localization;
import org.dojotoolkit.optimizer.Util;

public class AMDJSHandler extends JSHandler {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer");
	
	public AMDJSHandler(String configFileName) {
		super(configFileName);
	}
	
	@SuppressWarnings("unchecked")
	protected void customHandle(HttpServletRequest request, Writer handlerWriter, JSAnalysisData analysisData, JSAnalysisData[] excludes) throws ServletException, IOException {
		if (analysisData != null) {
			CountWriter writer = new CountWriter(handlerWriter);
			writer.write("zazl.addAnalysisKey('"+analysisData.getKey()+"');\n");
			String suffixCode = (String)config.get("suffixCode");
			if (suffixCode != null) {
				writer.write(suffixCode);
			}
			writer.write("if (typeof dojoConfig === 'undefined') { dojoConfig = {};}\ndojoConfig.locale = '"+request.getLocale().toString().toLowerCase().replace('_', '-')+"';\n");
			String[] dependencies = analysisData.getDependencies();
			Map<String, List<Map<String, String>>> pluginRefs = analysisData.getPluginRefs();
			List<Localization> localizations = new ArrayList<Localization>();
			Map<String, Object> amdConfig = (Map<String, Object>)config.get("amdconfig");
			String i18nPluginId = (String)amdConfig.get("i18nPluginId");
			List<String> seen = new ArrayList<String>();
			for (JSAnalysisData exclude : excludes) {
				Map<String, List<Map<String, String>>> excludePluginRefs = exclude.getPluginRefs();
				for (String excludePluginId : excludePluginRefs.keySet()) {
					if (i18nPluginId != null && i18nPluginId.equals(excludePluginId)) {
						List<Map<String, String>> excludePluginRefInstances = excludePluginRefs.get(excludePluginId);
						for (Map<String, String> excludePluginRefInstance : excludePluginRefInstances) {
							seen.add(excludePluginRefInstance.get("normalizedName"));
						}
					}
				}
			}
			for (String pluginId : pluginRefs.keySet()) {
				List<Map<String, String>> pluginRefInstances = pluginRefs.get(pluginId);
				for (Map<String, String> pluginRefInstance : pluginRefInstances) {
					String value = pluginRefInstance.get("value");
					logger.logp(Level.FINE, getClass().getName(), "customHandle", "plugin ref ["+pluginId+"]["+pluginRefInstance.get("normalizedName")+"]");
					if (value != null) {
						logger.logp(Level.FINE, getClass().getName(), "customHandle", "plugin ref ["+pluginId+"]["+pluginRefInstance.get("normalizedName")+"] has write value["+value+"]");
						writer.write(value);
					}
				}
				if (i18nPluginId != null && i18nPluginId.equals(pluginId)) {
					for (Map<String, String> pluginRefInstance : pluginRefInstances) {
						String bundlePackage = pluginRefInstance.get("normalizedName");
						String moduleUrl = pluginRefInstance.get("moduleUrl");
						if (!seen.contains(bundlePackage)) {
							seen.add(bundlePackage);
							String modulePath = bundlePackage.substring(0, bundlePackage.lastIndexOf('/'));
							moduleUrl = moduleUrl.substring(0, moduleUrl.lastIndexOf('/'));
							String bundleName = bundlePackage.substring(bundlePackage.lastIndexOf('/')+1);	
							logger.logp(Level.FINE, getClass().getName(), "customHandle", "i18n plugin ref ["+bundlePackage+"]["+modulePath+"]["+bundleName+"]");
							Localization localization = new Localization(bundlePackage, modulePath, bundleName, moduleUrl);
							localizations.add(localization);
						}
					}
				}
			}
			if (localizations.size() > 0) {
				Util.writeAMDLocalizations(resourceLoader, writer, localizations, request.getLocale());
			}
			Map<String, Integer> offsetMap = new HashMap<String, Integer>();
			Map<String, String> shims = analysisData.getShims();
			for (String dependency : dependencies) {
				String path = Util.normalizePath(dependency);
				String content = resourceLoader.readResource(path);
				if (content != null) {
					logger.logp(Level.FINE, getClass().getName(), "customHandle", "dependency ["+path+"]");
					String uri = path.substring(0, path.indexOf(".js"));
					int missingNameIndex = lookForMissingName(uri, analysisData.getModulesMissingNames());
					if (missingNameIndex != -1) {
						StringBuffer modifiedSrc = new StringBuffer(content.substring(0, missingNameIndex));
						modifiedSrc.append("'"+getMissingNameId(uri, analysisData.getModulesMissingNames())+"', ");
						modifiedSrc.append(content.substring(missingNameIndex));
						content = modifiedSrc.toString();
					}
					if (shims != null) {
						String shim = shims.get(uri);
						if (shim != null) {
							content += shim;
						}
					}
					content = compressorContentFilter.filter(content,path);
					int offset = writer.getLineCount();
					offsetMap.put(path, new Integer(offset));
					writer.write(content);
					writer.write("\n");
				}
			}
			if (compressorContentFilter.getJSCompressor() != null) {
				try {
					compressorContentFilter.getJSCompressor().getSourceMap("");
					writer.write("//@ sourceMappingURL=_javascript?sourcemap="+analysisData.getKey()+".map\n");
					sourceMapOffsets.put(analysisData.getKey(), offsetMap);
				} catch (UnsupportedOperationException e) {}
			}
		}
	}
	
	private int lookForMissingName(String uri, List<Map<String, Object>> modulesMissingNamesList) {
		int index = -1;
		try {
			String encoded = URLEncoder.encode(uri, "UTF-8");
			encoded = encoded.replace("%2F", "/");
			for (Map<String, Object> modulesMissingNames : modulesMissingNamesList) {
				if (modulesMissingNames.get("uri").equals(uri) || modulesMissingNames.get("uri").equals(encoded)) {
					index = ((Long)modulesMissingNames.get("nameIndex")).intValue();
					break;
				}
			}
		} catch (UnsupportedEncodingException e) {
			logger.logp(Level.SEVERE, getClass().getName(), "getMissingNameId", "UTF-8 encoding not found",e);
		}
		return index;
	}

	private String getMissingNameId(String uri, List<Map<String, Object>> modulesMissingNamesList) {
		String id = null;
		try {
			String encoded = URLEncoder.encode(uri, "UTF-8");
			encoded = encoded.replace("%2F", "/");
			for (Map<String, Object> modulesMissingNames : modulesMissingNamesList) {
				if (modulesMissingNames.get("uri").equals(uri) || modulesMissingNames.get("uri").equals(encoded)) {
					id = ((String)modulesMissingNames.get("id"));
					break;
				}
			}
		} catch (UnsupportedEncodingException e) {
			logger.logp(Level.SEVERE, getClass().getName(), "getMissingNameId", "UTF-8 encoding not found",e);
		}
		return id;
	}
}
