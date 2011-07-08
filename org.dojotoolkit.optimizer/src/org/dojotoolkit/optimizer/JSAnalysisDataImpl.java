/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dojotoolkit.server.util.resource.ResourceLoader;

public class JSAnalysisDataImpl implements JSAnalysisData {
	private String[] modules = null;
	private String[] dependencies = null;
	private String checksum = null;
	private List<Localization> localizations = null;
	private List<String> textDependencies = null;
	private List<Map<String, Object>> modulesMissingNames = null;
	private Map<String, List<Map<String, String>>> pluginRefs = null;
	private ResourceLoader resourceLoader = null;
	private Map<String, Long> timestampLookup = null;
	private String[] excludes = null;
	private String key = null;
	
	public JSAnalysisDataImpl(String[] modules, 
			                  List<String> dependencies, 
			                  String checksum, 
			                  List<Localization> localizations, 
			                  List<String> textDependencies, 
			                  List<Map<String, Object>> modulesMissingNames,
			                  Map<String, List<Map<String, String>>> pluginRefs,
			                  ResourceLoader resourceLoader,
			                  JSAnalysisData[] exclude) {
		timestampLookup = new HashMap<String, Long>();
		this.modules = modules;
		this.dependencies = new String[dependencies.size()];
		int i = 0;
		for (String dependency : dependencies) {
			String normalized = Util.normalizePath(dependency);
			timestampLookup.put(normalized, resourceLoader.getTimestamp(dependency));
			this.dependencies[i++] = normalized; 
		}
		this.checksum = checksum;
		this.localizations = localizations;
		this.textDependencies = textDependencies;
		this.modulesMissingNames = modulesMissingNames;
		this.pluginRefs = pluginRefs;
		this.resourceLoader = resourceLoader;
        excludes = _getExludes(exclude);
        key = _getKey(this.modules, excludes);
	}
	
	public String[] getModules() {
		return modules;
	}

	public String[] getDependencies() {
		return dependencies;
	}

	public String getChecksum() {
		return checksum;
	}
	
	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public List<Localization> getLocalizations() {
		return localizations;
	}
	
	public List<String> getTextDependencies() {
		return textDependencies;
	}
	
	public List<Map<String, Object>> getModulesMissingNames() {
		return modulesMissingNames;
	}
	
	public Map<String, List<Map<String, String>>> getPluginRefs() {
		return pluginRefs;
	}

	public String[] getExcludes() {
		return excludes;
	}
	
	public String getKey() {
		return key;
	}
	
	public boolean isStale() {
		for (String dependency : dependencies) {
			Long timestamp = timestampLookup.get(dependency);
			if (timestamp != null) {
				if (timestamp.longValue() != resourceLoader.getTimestamp(dependency)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static String getKey(String[] keyValues, JSAnalysisData[] exclude) {
		return _getKey(keyValues, _getExludes(exclude));
	}
	
	private static String _getKey(String[] keyValues, String[] excludes) {
		StringBuffer key = new StringBuffer();
		key.append("keyValues:");
		for (String keyValue : keyValues) {
			key.append(keyValue);
		}
		key.append("excludeValue:");
        for (String excludeModule : excludes) {
    		key.append(excludeModule);
    	}
		try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(key.toString().getBytes());
            BigInteger number = new BigInteger(1,messageDigest);
            return number.toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
	}

	private static String[] _getExludes(JSAnalysisData[] exclude) {
        List<String> excludeList = new ArrayList<String>();
        for (JSAnalysisData analysisData : exclude) {
	        for (String excludeModule : analysisData.getDependencies()) {
	        	if (!excludeList.contains(excludeModule)) {
	        		excludeList.add(excludeModule);
	        	}
	        }
        }
		String[] excludes = new String[excludeList.size()];
		excludes = excludeList.toArray(excludes);
		return excludes;
	}
}
