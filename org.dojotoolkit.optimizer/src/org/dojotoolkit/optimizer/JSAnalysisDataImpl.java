/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dojotoolkit.server.util.resource.ResourceLoader;

public class JSAnalysisDataImpl implements JSAnalysisData {
	private String[] modules = null;
	private String[] dependencies = null;
	private String checksum = null;
	private List<Localization> localizations = null;
	private ResourceLoader resourceLoader = null;
	private Map<String, Long> timestampLookup = null;
	
	public JSAnalysisDataImpl(String[] modules, List<String> dependencies, String checksum, List<Localization> localizations, ResourceLoader resourceLoader) {
		timestampLookup = new HashMap<String, Long>();
		this.modules = modules;
		this.dependencies = new String[dependencies.size()];
		int i = 0;
		for (String dependency : dependencies) {
			timestampLookup.put(Util.normalizePath(dependency), resourceLoader.getTimestamp(dependency));
			this.dependencies[i++] = Util.normalizePath(dependency); 
		}
		this.checksum = checksum;
		this.localizations = localizations;
		this.resourceLoader = resourceLoader;
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
}
