/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dojotoolkit.json.JSONParser;
import org.dojotoolkit.json.JSONSerializer;
import org.dojotoolkit.server.util.resource.ResourceLoader;

public class JSAnalysisDataImpl implements JSAnalysisData {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer");
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
	private String config = null;
	private boolean checksumStale = false;
	private Map<String, String>  shims = null;
	
	public JSAnalysisDataImpl(String[] modules,
            List<String> dependencies,
            List<Localization> localizations,
            List<String> textDependencies,
            List<Map<String, Object>> modulesMissingNames,
            Map<String, List<Map<String, String>>> pluginRefs,
            ResourceLoader resourceLoader,
            String[] excludes,
            Map<String, Object> pageConfig,
            Map<String, String> shims) throws IOException {
		this(modules, dependencies, localizations, textDependencies, modulesMissingNames, pluginRefs, resourceLoader, excludes, pageConfig, shims, null, null, null);
	}

	private JSAnalysisDataImpl(String[] modules,
			                  List<String> dependencies,
			                  List<Localization> localizations,
			                  List<String> textDependencies,
			                  List<Map<String, Object>> modulesMissingNames,
			                  Map<String, List<Map<String, String>>> pluginRefs,
			                  ResourceLoader resourceLoader,
			                  String[] excludes,
			                  Map<String, Object> pageConfig,
			                  Map<String, String> shims,
			                  String checksum,
			                  String config,
			                  Map<String, Long> timestamps) throws IOException {
		this.modules = modules;
		this.dependencies = new String[dependencies.size()];
		int i = 0;
		if (timestamps == null) {
			timestampLookup = new HashMap<String, Long>();
			for (String dependency : dependencies) {
				String normalized = Util.normalizePath(dependency);
				resourceLoader.getResource(normalized);
				timestampLookup.put(normalized, resourceLoader.getTimestamp(normalized));
				this.dependencies[i++] = normalized; 
			}
		} else {
			timestampLookup = timestamps;
			for (String dependency : dependencies) {
				String normalized = Util.normalizePath(dependency);
				resourceLoader.getResource(normalized);
				this.dependencies[i++] = normalized; 
			}
		}
		this.checksum = ChecksumCreator.createChecksum(this.dependencies, resourceLoader);
		if (checksum != null && this.checksum.equals(checksum) == false) {
			checksumStale = true;
		}
		this.localizations = localizations;
		this.textDependencies = textDependencies;
		this.modulesMissingNames = modulesMissingNames;
		this.pluginRefs = pluginRefs;
		this.resourceLoader = resourceLoader;
        this.excludes = excludes;
        this.shims = shims;
        if (pageConfig != null) {
        	StringWriter sw = new StringWriter();
        	try {
				JSONSerializer.serialize(sw, pageConfig);
				config = sw.toString();
			} catch (IOException e) {
				logger.logp(Level.SEVERE, JSAnalysisDataImpl.class.getName(), "JSAnalysisDataImpl", "Failed to serialize page config", e);
			}
        }
        if (config != null) {
        	this.config = config;
        }
        key = _getKey(this.modules, excludes, config);
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
	
	public Map<String, String> getShims() {
		return shims;
	}
	
	public boolean isStale() {
		if (checksumStale) {
			return true;
		}
		for (String dependency : dependencies) {
			Long timestamp = timestampLookup.get(dependency);
			if (timestamp != null) {
				if (timestamp.longValue() != resourceLoader.getTimestamp(dependency)) {
					try {
						checksum = ChecksumCreator.createChecksum(this.dependencies, resourceLoader);
					} catch (IOException e) {
						logger.logp(Level.SEVERE, JSAnalysisDataImpl.class.getName(), "getChecksum", "Failed to calculate checksum", e);
					}
					return true;
				}
			}
		}
		return false;
	}
	
	public void save(File tempDir) {
		File implFile = new File(tempDir, key+".json");
		Writer w = null;
		
		try {
			w = new BufferedWriter(new FileWriter(implFile));
			Map<String, Object> implDetails = new HashMap<String, Object>();
			implDetails.put("modules", Arrays.asList(modules));
			implDetails.put("dependencies", Arrays.asList(dependencies));
			implDetails.put("checksum", checksum);
			implDetails.put("timestamps", timestampLookup);
			if (localizations != null) {
				List<Map<String, Object>> localizationsList = new ArrayList<Map<String, Object>>();
				for (Localization localization : localizations) {
					Map<String, Object> localizationMap = new HashMap<String, Object>();
					localizationMap.put("bundlePackage", localization.bundlePackage);
					localizationMap.put("bundleName", localization.bundleName);
					localizationMap.put("modulePath", localization.modulePath);
					localizationMap.put("moduleUrl", localization.moduleUrl);
					localizationsList.add(localizationMap);
				}
				implDetails.put("localizations", localizationsList);
			}
			if (textDependencies != null) {
				implDetails.put("textDependencies", textDependencies);
			}
			if (modulesMissingNames != null) {
				implDetails.put("modulesMissingNames", modulesMissingNames);
			}
			if (pluginRefs != null) {
				implDetails.put("pluginRefs", pluginRefs);
			}
			if (excludes != null) {
				implDetails.put("excludes", Arrays.asList(excludes));
			}
			if (config != null) {
				implDetails.put("pageConfig", config);
			}
			if (shims != null) {
				implDetails.put("shims", shims);
			}
			JSONSerializer.serialize(w, implDetails, true);
		} catch (IOException e) {
			logger.logp(Level.SEVERE, JSAnalysisDataImpl.class.getName(), "load", "Failed to save ["+implFile.getPath()+"]", e);
		} finally {
			if (w != null) { try { w.close(); } catch (IOException e) {}}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static JSAnalysisDataImpl load(String key, File tempDir, ResourceLoader resourceLoader) {
		JSAnalysisDataImpl impl = null;
		File implFile = new File(tempDir, key+".json");
		if (implFile.exists()) {
			Reader r = null;
			try {
				r = new BufferedReader(new FileReader(implFile));
				Map<String, Object> implDetails = (Map<String, Object>)JSONParser.parse(r);
				List<String> modulesList = (List<String>)implDetails.get("modules");
				String[] modules = new String[modulesList.size()];
				modules = modulesList.toArray(modules);
				List<String> dependencies = (List<String>)implDetails.get("dependencies");
				List<Localization> localizations = null;
				List<Map<String, Object>> localizationsList = (List<Map<String, Object>>)implDetails.get("localizations");
				if (localizationsList != null) {
					localizations = new ArrayList<Localization>();
					for (Map<String, Object> localizationMap : localizationsList) {
						String bundlePackage = (String)localizationMap.get("bundlePackage");
						String modulePath = (String)localizationMap.get("modulePath");
						String bundleName = (String)localizationMap.get("bundleName");
						String moduleUrl = (String)localizationMap.get("moduleUrl");
						Localization localization = new Localization(bundlePackage, modulePath, bundleName, moduleUrl);
						localizations.add(localization);
					}
				}
				List<String> textDependencies = (List<String>)implDetails.get("textDependencies");
				List<Map<String, Object>> modulesMissingNames = (List<Map<String, Object>>)implDetails.get("modulesMissingNames");
				Map<String, List<Map<String, String>>> pluginRefs = (Map<String, List<Map<String, String>>>)implDetails.get("pluginRefs");
				List<String> excludesList = (List<String>)implDetails.get("excludes");
				String[] excludes = null;
				if (excludesList != null) {
					excludes = new String[excludesList.size()];
					excludes = excludesList.toArray(excludes);
				}
				String config = (String)implDetails.get("pageConfig");
				String checksum = (String)implDetails.get("checksum");
				Map<String, String> shims = (Map<String, String>)implDetails.get("shims");
				Map<String, Long> timestamps = (Map<String, Long>)implDetails.get("timestamps");
				impl = new JSAnalysisDataImpl(modules, 
						                      dependencies, 
						                      localizations, 
						                      textDependencies, 
						                      modulesMissingNames, 
						                      pluginRefs, 
						                      resourceLoader, 
						                      excludes,
						                      null,
						                      shims,
						                      checksum,
						                      config,
						                      timestamps);
			} catch (IOException e) {
				logger.logp(Level.SEVERE, JSAnalysisDataImpl.class.getName(), "load", "Failed to load ["+implFile.getPath()+"]", e);
			} finally {
				if (r != null) { try { r.close(); } catch (IOException e) {}}
			}
		}
		return impl;
	}
	
	public static String getKey(String[] keyValues, JSAnalysisData[] exclude, Map<String, Object> pageConfig) {
		String config = null;
		if (pageConfig != null) {
        	StringWriter sw = new StringWriter();
        	try {
				JSONSerializer.serialize(sw, pageConfig);
				config = sw.toString();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return _getKey(keyValues, getExludes(exclude), config);
	}
	
	private static String _getKey(String[] keyValues, String[] excludes, String config) {
		StringBuffer key = new StringBuffer();
		key.append("keyValues:");
		for (String keyValue : keyValues) {
			key.append(keyValue);
		}
		key.append("excludeValue:");
        for (String excludeModule : excludes) {
    		key.append(excludeModule);
    	}
        if (config != null) {
        	key.append("configValue:");
        	key.append(config);
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

	public static String[] getExludes(JSAnalysisData[] exclude) {
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
