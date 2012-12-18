/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.amd.rhinoast;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dojotoolkit.json.JSONParser;
import org.dojotoolkit.json.JSONSerializer;
import org.dojotoolkit.optimizer.CachingJSOptimizer;
import org.dojotoolkit.optimizer.JSAnalysisData;
import org.dojotoolkit.optimizer.JSAnalysisDataImpl;
import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.dojotoolkit.server.util.rhino.ASTCache;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;
import org.dojotoolkit.server.util.rhino.RhinoJSMethods;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.ast.ArrayLiteral;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.ConditionalExpression;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.ParenthesizedExpression;
import org.mozilla.javascript.ast.StringLiteral;

public class AMDJSOptimizer extends CachingJSOptimizer {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer.amd.rhinoast");

	private Map<String, Object> config = null;
	private ASTCache astCacheHandler = null;
	private RhinoClassLoader rhinoClassLoader = null;

	public AMDJSOptimizer(ResourceLoader resourceLoader, RhinoClassLoader rhinoClassLoader, Map<String, Object> config, File tempDir) {
		super(tempDir, resourceLoader);
		this.config = config;
		this.rhinoClassLoader = rhinoClassLoader;
		
		Boolean useAstCache = (Boolean)config.get("astCache");
		if (useAstCache != null && useAstCache == Boolean.TRUE) {
			astCacheHandler = new ASTCache();
		}
	}
	
	public Map<String, Object> getConfig() {
		return config;
	}
	
	@SuppressWarnings("unchecked")
	protected JSAnalysisDataImpl _getAnalysisData(String[] modules, JSAnalysisData[] exclude, Map<String, Object> pageConfig) throws IOException {
		JSAnalysisDataImpl jsAnalysisData = null;
		
		Map<String, Object> fullConfig = new HashMap<String, Object>();
		Map<String, Object> baseConfig = (Map<String, Object>)config.get("amdconfig"); 
		fullConfig.putAll(baseConfig);
		if (pageConfig != null) {
			fullConfig.putAll(pageConfig);
		}
		
		Map<String, Object> cfg = new HashMap<String, Object>();
		Map<String, Object> pkgs = new HashMap<String, Object>();
		cfg.put("pkgs", pkgs);
		cfg.put("paths", new HashMap<String, Object>());
		cfg.put("map", new HashMap<String, Object>());
		cfg.put("baseUrl", "");
		for (String key : fullConfig.keySet()) {
			if (key.equals("packages")) {
				List<Map<String, Object>> packages = (List<Map<String, Object>>)fullConfig.get("packages");
				for (Map<String, Object> p : packages) {
					String name = (String)p.get("name");
					String location = (String)p.get("location");
					if (location == null) {
						location = name;
					}
					String main = (String)p.get("main");
					if (main == null) {
						main = "main";
					}
					Map<String, Object> pkg = new HashMap<String, Object>();
					pkg.put("name", name);
					pkg.put("location", location);
					pkg.put("main", main);
					pkgs.put(name, pkg);
				}
			} else {
				cfg.put(key, fullConfig.get(key));
			}
		}
		boolean scanCJSRequires = cfg.get("scanCJSRequires") == null ? false : (Boolean)cfg.get("scanCJSRequires");

		StringWriter sw = new StringWriter();
		String pageConfigString = "";
		try {
			JSONSerializer.serialize(sw, cfg);
			pageConfigString = sw.toString();
		} catch (IOException e) {
			logger.logp(Level.SEVERE, getClass().getName(), "_getAnalysisData", "IOException while parsing page configuration data", e);
			throw new IOException("IOException while parsing page configuration data", e);
		}
		
        List<String> excludeList = new ArrayList<String>();
        for (JSAnalysisData analysisData : exclude) {
	        for (String excludeModule : analysisData.getDependencies()) {
        		excludeModule = excludeModule.substring(0, excludeModule.indexOf(".js"));
	        	if (!excludeList.contains(excludeModule)) {
	        		excludeList.add(excludeModule);
	        	}
	        }
        }
        if (fullConfig.containsKey("excludes")) {
        	List<String> configExcludes = (List<String>)fullConfig.get("excludes");
	        for (String excludeModule : configExcludes) {
	        	String excludeUri = idToUrl(excludeModule, cfg);
	        	if (!excludeList.contains(excludeUri)) {
	        		excludeList.add(excludeUri);
	        	}
	        }
        }
		Map<String, List<Map<String, String>>> pluginRefs = new HashMap<String, List<Map<String, String>>>();
		List<Map<String, Object>> missingNamesList = new ArrayList<Map<String, Object>>();
		Map<String, Module> moduleMap = new HashMap<String, Module>();
		Map<String, String> shims = new HashMap<String, String>();
		
        for (String moduleId : modules) {
        	logger.logp(Level.INFO, getClass().getName(), "_getAnalysisData", "AST parsing ["+moduleId+"] using the Rhino AST API");
        	AstVisitor visitor = new AstVisitor(moduleId, moduleMap, pluginRefs, missingNamesList, cfg, new Stack<String>(), excludeList, pageConfigString, scanCJSRequires, shims);
        	if (visitor.getError() != null) {
            	logger.logp(Level.INFO, getClass().getName(), "_getAnalysisData", "AST parsing error for ["+moduleId+"] error : "+visitor.getError());
            	throw new IOException("AST parsing error for ["+moduleId+"] error : "+visitor.getError());
        	}
        }
        
        List<String> dependencies = new ArrayList<String>();
        Map<String, Boolean> seen = new HashMap<String, Boolean>();
        seen.put("require", Boolean.TRUE);
        seen.put("module", Boolean.TRUE);
        seen.put("exports", Boolean.TRUE);
        for (String moduleId : modules) {
        	if (moduleId.indexOf('!') != -1) {
        		moduleId = moduleId.substring(0, moduleId.indexOf('!'));
        	}
        	moduleId = expand(moduleId, new Stack<String>(), cfg);
        	Module m = moduleMap.get(moduleId);
            buildDependencyList(m, moduleMap, dependencies, seen);
            scanForCircularDependencies(m, new Stack<String>(), moduleMap);
        }
        
        for (Module module : moduleMap.values()) {
        	if (module.defineFound == false && shims.containsKey(module.uri) == false) {
				StringBuffer shimContent = new StringBuffer();
				shimContent.append("\n(function(root, cfg) {\n");
				shimContent.append("define('");
				shimContent.append(module.id);
				shimContent.append("', ");
				shimContent.append("function() {\n");
				shimContent.append("});\n}(this, zazl._getConfig()));\n");
				shims.put(module.uri, shimContent.toString());
        	}
        }
		jsAnalysisData = new JSAnalysisDataImpl(modules, dependencies, null, null, missingNamesList, pluginRefs, resourceLoader, JSAnalysisDataImpl.getExludes(exclude), pageConfig, shims);
		return jsAnalysisData;
	}
	
	private static String join(List<String> l, char separator) {
		StringBuffer result = new StringBuffer();
		for (String s : l) {
			result.append(s);
			result.append(separator);
		}
		if (result.length() > 0) 
			result.deleteCharAt(result.length()-1);
		return result.toString(); 
	}
	
	private static void splice(List<String> l, int index, int howMany, String toInsert) {
		for (int i = index; i < howMany; i++) {
			l.remove(index);
		}
		l.add(index, toInsert);
	}
	
	private static String normalize(String path) {
		try {
			URI uri =  new URI(path);
			String normalized = uri.normalize().toString();
			if (normalized.charAt(normalized.length()-1) == '/') {
				normalized = normalized.substring(0, normalized.length()-1);
			}
			return normalized;
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static int countSegments(String path) {
		int count = 0;
		for (int i = 0; i < path.length(); i++) {
			if (path.charAt(i) == '/') {
				count++;
			}
		}
		return count;
	}

	private static String findMapping(String path, String depId, Map<String, Object> config) {
		Map<String, Object> map = (Map<String, Object>)config.get("map");
		String mapping = null;
		int segmentCount = -1;
		for (String key : map.keySet()) {
			if (depId.startsWith(key)) {
				int foundSegmentCount = countSegments(key);
				if (foundSegmentCount > segmentCount) {
					Map<String, Object> mapEntry = (Map<String, Object>)map.get(key);
					if (mapEntry != null && mapEntry.containsKey(path)) {
						mapping = (String)mapEntry.get(path);
						segmentCount = foundSegmentCount;
					}
				}
			}
		}
		if (mapping == null) {
			Map<String, Object> mapEntry = (Map<String, Object>)map.get("*");
			if (mapEntry != null && mapEntry.containsKey(path)) {
				mapping = (String)mapEntry.get(path);
			}
		}
		return mapping;
	}

	private static String idToUrl(String path, Map<String, Object> config) {
		List<String> segments = new ArrayList<String>(Arrays.asList(path.split("/")));
		Map<String, Object> paths = (Map<String, Object>)config.get("paths");
		Map<String, Object> packages = (Map<String, Object>)config.get("pkgs");
		String baseUrl = (String)config.get("baseUrl");
		for (int i = segments.size(); i >= 0; i--) {
			Map<String, Object> pkg;
	        String parent = join(segments.subList(0, i), '/');
	        if (paths.get(parent) != null) {
	        	splice(segments, 0, i, (String)paths.get(parent));
	            break;
	        }else if ((pkg = (Map<String, Object>)packages.get(parent)) != null) {
	        	String pkgPath;
	            if (path.equals((String)pkg.get("name"))) {
	                pkgPath = (String)pkg.get("location") + '/' + (String)pkg.get("main");
	            } else {
	                pkgPath = (String)pkg.get("location");
	            }
				splice(segments, 0, i, pkgPath);
				break;
	        }
		}
		path = join(segments, '/');
	    if (path.charAt(0) != '/') {
	    	path = baseUrl + path;
	    }
		path = normalize(path);
		return path;
	}
	
	private static String getParentId(List<String> pathStack) {
		return pathStack.size() > 0 ? pathStack.get(pathStack.size()-1) : "";
	}
	
	private static String expand(String path, List<String> pathStack, Map<String, Object> config) {
		Map<String, Object> packages = (Map<String, Object>)config.get("pkgs");
		if (path.charAt(0) == '.') {
			Map<String, Object> pkg;
	        if ((pkg = (Map<String, Object>)packages.get(getParentId(pathStack))) != null) {
	            path = (String)pkg.get("name") + "/" + path;
	        } else {
	            path = getParentId(pathStack) + "/../" + path;
	        }
			path = normalize(path);
		}
		for (String pkgName : packages.keySet()) {
		    if (path.equals(pkgName)) {
		    	return (String)((Map<String, Object>)packages.get(pkgName)).get("name") + '/' + (String)((Map<String, Object>)packages.get(pkgName)).get("main");
		    }
		}
		List<String> segments = new ArrayList<String>(Arrays.asList(path.split("/")));
		for (int i = segments.size(); i >= 0; i--) {
	        String parent = join(segments.subList(0, i), '/');
	        String mapping = findMapping(parent, getParentId(pathStack), config);
	        if (mapping != null) {
	        	splice(segments, 0, i, mapping);
	        	return join(segments, '/');
	        }
		}
		return path;
	}
	
	private void buildDependencyList(Module m, Map<String, Module> moduleMap, List<String> dependencyList, Map<String, Boolean> seen) {
		if (!seen.containsKey(m.uri)) {
			seen.put(m.uri, Boolean.TRUE);
			for (String dep : m.dependencies) {
				Module depModule = moduleMap.get(dep);
				if (depModule != null) {
					buildDependencyList(depModule, moduleMap, dependencyList, seen);
				} else {
					logger.logp(Level.SEVERE, getClass().getName(), "buildDependencyList", "Unable to locate dependency module ["+dep+"] depended on by ["+m.id+"]");
				}
			}
			dependencyList.add(m.uri+".js");
		}
	}
	
	private void scanForCircularDependencies(Module module, Stack<String> check, Map<String, Module> moduleMap) {
		check.push(module.id);
		for (String dep : module.dependencies) {
			Module moduleDependency = moduleMap.get(dep);
			if (moduleDependency == null) {
				logger.logp(Level.SEVERE, getClass().getName(), "scanForCircularDependencies", "Unable to locate dependency module ["+dep+"] depended on by ["+module.id+"]");
				continue;
			}
			if (moduleDependency.scanned) {
				continue;
			}
			boolean found = false;
			String dup = null;
			for (String s : check) {
				if (s.equals(moduleDependency.id)) {
					found = true;
					dup = moduleDependency.id;
					break;
				}
			}
			if (found) {
				StringBuffer msg = new StringBuffer("Circular dependency found : ");
				for (String s : check) {
					msg.append(s);
					msg.append("->");
				}
				msg.append(dup);
	        	logger.logp(Level.INFO, getClass().getName(), "scanForCircularDependencies", msg.toString());
			} else {
				scanForCircularDependencies(moduleDependency, check, moduleMap);
			}
		}
		module.scanned = true;
		check.pop();
	}

	private class AstVisitor implements NodeVisitor {
		private List<Map<String, Object>> missingNamesList = null;
		private String moduleId = null;
		private String url = null;
		private Map<String, Module> moduleMap = null;
		private Map<String, List<Map<String, String>>> pluginRefList = null;
		private Map<String, Object> config = null;
		private Stack<String> pathStack = null;
		private List<String> excludeList = null;
		private Module module = null;
		private String baseUrl = null;
		private String pageConfigString = null;
		private String error = null;
		private boolean scanCJSRequires = false;
		private Map<String, String> shims = null;

		public AstVisitor(String moduleId, 
				          Map<String, Module> moduleMap,
				          Map<String, List<Map<String, String>>> pluginRefList,
				          List<Map<String, Object>> missingNamesList,
				          Map<String, Object> config,
				          Stack<String> pathStack,
				          List<String> excludeList,
				          String pageConfigString,
				          boolean scanCJSRequires,
				          Map<String, String> shims) {
			
			if (moduleId.equals("require") || moduleId.equals("exports") || moduleId.equals("module")) {
				moduleMap.put(moduleId, new Module(moduleId, moduleId));
				return;
			}
			
			this.moduleId = moduleId;
			this.moduleMap = moduleMap;
			this.pluginRefList = pluginRefList;
			this.missingNamesList = missingNamesList;
			this.config = config;
			this.pathStack = pathStack;
			this.excludeList = excludeList;
			this.pageConfigString = pageConfigString;
			this.scanCJSRequires = scanCJSRequires;
			this.shims = shims;
			
			this.baseUrl = (String)config.get("baseUrl");
			
			if (this.moduleId.indexOf('!') != -1) {
				String pluginName = this.moduleId.substring(0, this.moduleId.indexOf('!'));
				pluginName = expand(pluginName, pathStack, config);
				String pluginValue = this.moduleId.substring(this.moduleId.indexOf('!')+1);
				List<Map<String, String>> l = (List<Map<String, String>>)pluginRefList.get(pluginName);
				if (l == null) {
					l = new ArrayList<Map<String, String>>();
					pluginRefList.put(pluginName, l);
				}
				Map<String, String> pluginRef = processPluginRef(pluginName, pluginValue);
				if (pluginRef.containsKey("dependency")) {
					String pluginDep = (String)pluginRef.get("dependency");
					String dependencyUri = idToUrl(pluginDep, config);
					if (dependencyUri.charAt(0) != '/') {
						dependencyUri = '/'+dependencyUri;
					}
					boolean addDep = true;
					for (String exclude : excludeList) {
						if (exclude.equals(dependencyUri)) {
							addDep = false;
							break;
						}
					}
					if (addDep) {
						Stack<String> s = new Stack<String>();
						s.push(this.moduleId);
						AstVisitor visitor = new AstVisitor(pluginDep, moduleMap, pluginRefList, missingNamesList, config, s, excludeList, pageConfigString, scanCJSRequires, shims);
						if (visitor.getError() != null) {
							error = visitor.getError();
							return;
						}
					}
				}
				l.add(pluginRef);
				this.moduleId = pluginName;
			} else {
				this.moduleId = expand(moduleId, pathStack, config);
			}
			url = idToUrl(this.moduleId, config);
			if (url.charAt(0) != '/') {
				url = "/"+url;
			}
			if (moduleMap.get(this.moduleId) == null) {
	            String source;
				try {
					source = resourceLoader.readResource(url+".js");
		            if (source != null) {
		            	module = new Module(this.moduleId, url);
						moduleMap.put(this.moduleId, module);
				        CompilerEnvirons compilerEnv = new CompilerEnvirons();
				        Parser parser = new Parser(compilerEnv, compilerEnv.getErrorReporter());
						
		                AstRoot ast;
						try {
							ast = parser.parse(source, this.moduleId, 1);
			                ast.visit(this);
			                if (!module.defineFound) {
			                	findShim();
			                }
						} catch (EvaluatorException e) {
							error = "Failed to parse ["+url+"] [line:"+e.lineNumber()+" column:"+e.columnNumber()+"] reason ["+e.details()+"]";
						}
		            } else {
		            	error = "Unable to obtain source for Module ["+url+"]";
		            }
				} catch (IOException e) {
	            	error = "Unable to obtain source for Module ["+url+"] error : "+e.getMessage();
				}
			}
		}
		
		public String getError() {
			return error;
		}
		
		public boolean visit(AstNode astNode) {
			if (astNode instanceof FunctionCall) {
				FunctionCall functionCall = (FunctionCall)astNode;
				AstNode target = functionCall.getTarget();
				String callName = getCallName(target);
				if (callName.equals("define") || callName.equals("require")) {
					List<AstNode> args = functionCall.getArguments();
					if (callName.equals("define")) {
						module.defineFound = true;
						if (args.get(0) instanceof StringLiteral == false) {
							Map<String, Object> missingName = new HashMap<String, Object>();
							missingName.put("nameIndex", new Long(functionCall.getAbsolutePosition()+functionCall.getLp()+1));
							missingName.put("uri", url);
							missingName.put("id", this.moduleId);
							missingNamesList.add(missingName);
						}
					}
					List<String> dependencies = new ArrayList<String>();
					if (callName.equals("require") && args.get(0) instanceof StringLiteral) {
						if  (scanCJSRequires) {
							System.out.println("require:"+((StringLiteral)args.get(0)).getValue());
							dependencies.add(((StringLiteral)args.get(0)).getValue());
						}
					} else if (args.get(0) instanceof StringLiteral && args.get(1) instanceof ArrayLiteral) {
						ArrayLiteral al = (ArrayLiteral) args.get(1);
						for (AstNode dependency : al.getElements()) {
							if (dependency instanceof StringLiteral) {
								dependencies.add(((StringLiteral)dependency).getValue());
							}
						}
					} else if (callName.equals("define") && args.get(0) instanceof ArrayLiteral) {
						ArrayLiteral al = (ArrayLiteral) args.get(0);
						for (AstNode dependency : al.getElements()) {
							if (dependency instanceof StringLiteral) {
								dependencies.add(((StringLiteral)dependency).getValue());
							}
						}
					}
					if (dependencies.size() > 0) {
						String dependencyId = null;
						for (String dependency : dependencies) {
							dependencyId = dependency;
							if (dependencyId.indexOf('!') != -1) {
								pathStack.push(this.moduleId);
								String pluginName = dependencyId.substring(0, dependencyId.indexOf('!'));
								pluginName = expand(pluginName, pathStack, config);
								String pluginValue = dependencyId.substring(dependencyId.indexOf('!')+1);
								List<Map<String, String>> l = (List<Map<String, String>>)pluginRefList.get(pluginName);
								if (l == null) {
									l = new ArrayList<Map<String, String>>();
									pluginRefList.put(pluginName, l);
								}
								Map<String, String> pluginRef = processPluginRef(pluginName, pluginValue);
								if (pluginRef.containsKey("dependency")) {
									String pluginDep = (String)pluginRef.get("dependency");
									String dependencyUri = idToUrl(pluginDep, config);
									if (dependencyUri.charAt(0) != '/') {
										dependencyUri = '/'+dependencyUri;
									}
									boolean addDep = true;
									for (String exclude : excludeList) {
										if (exclude.equals(dependencyUri)) {
											addDep = false;
											break;
										}
									}
									if (addDep) {
										module.dependencies.add(pluginDep);
										AstVisitor visitor = new AstVisitor(pluginDep, moduleMap, pluginRefList, missingNamesList, config, pathStack, excludeList, pageConfigString, scanCJSRequires, shims);
										if (visitor.getError() != null) {
											error = visitor.getError();
											return false;
										}
									}
								}
								l.add(pluginRef);
								pathStack.pop();
								dependencyId = pluginName;
							} else if (dependencyId.equals(this.baseUrl+"require")) {
								dependencyId = null;
							} else if (dependencyId.equals(this.baseUrl+"exports")) {
								dependencyId = null;
							} else if (dependencyId.equals(this.baseUrl+"module")) {
								dependencyId = null;
							}
							if (dependencyId != null) {
								pathStack.push(this.moduleId);
								dependencyId = expand(dependencyId, pathStack, config);
								String dependencyUri = idToUrl(dependencyId, config);
								if (dependencyUri.charAt(0) != '/') {
									dependencyUri = '/'+dependencyUri;
								}
								boolean addDep = true;
								for (String exclude : excludeList) {
									if (exclude.equals(dependencyUri)) {
										addDep = false;
										break;
									}
								}
								if (addDep) {
									module.dependencies.add(dependencyId);
									AstVisitor visitor = new AstVisitor(dependencyId, moduleMap, pluginRefList, missingNamesList, config, pathStack, excludeList, pageConfigString, scanCJSRequires, shims);
									if (visitor.getError() != null) {
										error = visitor.getError();
										return false;
									}
								}
								pathStack.pop();
							}
						}
					}
				}
			}
			return true;
		}
		
		private String getCallName(AstNode target) {
			String callName = "";
			if (target instanceof Name) {
				callName = ((Name)target).getIdentifier();
			} else if (target instanceof ParenthesizedExpression) {
				ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression)target;
				AstNode expression = parenthesizedExpression.getExpression();
				if (expression instanceof ConditionalExpression) {
					ConditionalExpression conditionalExpression = (ConditionalExpression)expression;
					AstNode trueExpression = conditionalExpression.getTrueExpression();
					AstNode falseExpression = conditionalExpression.getFalseExpression();
					if (trueExpression instanceof Name) {
						callName = ((Name)trueExpression).getIdentifier();
					} else if (falseExpression instanceof Name) {
						callName = ((Name)falseExpression).getIdentifier();
					}
				}
			}
			return callName;
		}

		private Map<String, String> processPluginRef(String pluginName, String pluginValue) {
			Map<String, String> pluginRef = new HashMap<String, String>();
			pluginRef.put("name", pluginValue);
			String value = null;
			String normalizedName = null;
			String dependency = null;
			String moduleUrl = null;
			Map<String, Object> plugins = (Map<String, Object>)config.get("plugins");
			if (plugins.containsKey(pluginName)) {
				Map<String, Object> plugin = (Map<String, Object>)plugins.get(pluginName);
				String proxy = (String)plugin.get("proxy");
				
				if (pluginValue.length() > 0) {
					normalizedName = expand(pluginValue, pathStack, config);
					moduleUrl = idToUrl(normalizedName, config);
				} else {
					moduleUrl = normalizedName = pluginValue;
				}
				
				Map<String, Object> proxyReturn = callProxy(proxy, pluginName, pluginValue, normalizedName, moduleUrl);
				if (proxyReturn.get("normalizedName") != null) {
					normalizedName = (String)proxyReturn.get("normalizedName");
					moduleUrl = idToUrl(normalizedName, config);
				}
				value = (String)proxyReturn.get("value");
				dependency = (String)proxyReturn.get("dependency");
			} else if (pluginValue.length() > 0) {
				normalizedName = expand(pluginValue, pathStack, config);
				moduleUrl = idToUrl(normalizedName, config);
			}
			
			if (normalizedName != null) { pluginRef.put("normalizedName", normalizedName); }
			if (moduleUrl != null) { pluginRef.put("moduleUrl", moduleUrl); }
			if (value != null) { pluginRef.put("value", value); }
			if (dependency != null) { pluginRef.put("dependency", dependency); }
			return pluginRef;
		}
		
		private Map<String, Object> callProxy(String proxy, String pluginName, String resourceName, String normalizedName, String moduleUrl) {
			Map<String, Object> proxyReturn = null;
			StringWriter sw = new StringWriter();
			try {
				JSONSerializer.serialize(sw, pathStack);
			} catch (IOException e) {
				logger.logp(Level.SEVERE, getClass().getName(), "callProxy", "IOException while parsing page configuration data", e);
			}
			
			StringBuffer sb = new StringBuffer();
			sb.append("loadJS('/json/json2.js');\n");
	        sb.append("loadJS('/jsutil/commonjs/loader.js');\n");
	        sb.append("var astwalker = require('optimizer/amd/astwalker');\n");
	        sb.append("var config = "+pageConfigString+";\n");
	        sb.append("var pathStack = "+sw.toString()+";\n");
	        sb.append("var plugin = require('"+proxy+"');\n");
	        sb.append("var pluginName = '"+pluginName+"';\n");
	        sb.append("var resourceName = '"+resourceName+"';\n");
	        sb.append("var normalizedName = '"+normalizedName+"';\n");
	        sb.append("var moduleUrl = '"+moduleUrl+"';\n");
	        sb.append("var value;\n");
	        sb.append("var dependency;\n");
	        sb.append("var newNormalizedName;\n");
	        sb.append("if (plugin.write) {\n");
	        sb.append("plugin.write(pluginName, normalizedName, function(writeOutput) {\n");
	        sb.append("value = writeOutput;\n");
	        sb.append("}, moduleUrl);\n");
	        sb.append("}\n");
	        sb.append("if (plugin.normalize) {\n");
	        sb.append("newNormalizedName = dependency = plugin.normalize(resourceName, config, function(id) {\n");
	        sb.append("return astwalker.expand(id, pathStack, config);\n");
	        sb.append("});\n");
	        sb.append("}\n");
	        sb.append("JSON.stringify({normalizedName: newNormalizedName, dependency: dependency, value: value});\n");
			Context ctx = null; 
			try {
				ctx = Context.enter();
				ScriptableObject scope = ctx.initStandardObjects();
				RhinoJSMethods.initScope(scope, resourceLoader, rhinoClassLoader, false, astCacheHandler);
				Object o = ctx.evaluateString(scope, sb.toString(), "AMDJSOptimizer", 1, null);
				proxyReturn = (Map<String, Object>)JSONParser.parse(new StringReader((String)o));
			}
			catch(Throwable t) {
				logger.logp(Level.SEVERE, getClass().getName(), "callProxy", "Exception on callProxy for ["+proxy+"]", t);
			}
			finally {
				Context.exit();
			}
			return proxyReturn;
		}
		
		private void findShim() {
			Map<String, Object> shimConfig = (Map<String, Object>)config.get("shim");
			if (shimConfig != null) {
				Object o = shimConfig.get(module.id);
				if (o != null) {
					Map<String, Object> shim = null;
					if (o instanceof List<?>) {
						shim = new HashMap<String, Object>();
						shim.put("deps", o);
					} else {
						shim = (Map<String, Object>)o;
					}
					StringBuffer shimContent = new StringBuffer();
					shimContent.append("\n(function(root, cfg) {\n");
					shimContent.append("define('");
					shimContent.append(moduleId);
					shimContent.append("', ");
					List<String> deps = (List<String>)shim.get("deps");
					if (deps != null && deps.size() > 0) {
						shimContent.append("[");
						for (String dep : deps) {
							String dependencyUri = idToUrl(dep, config);
							if (dependencyUri.charAt(0) != '/') {
								dependencyUri = '/'+dependencyUri;
							}
							boolean addDep = true;
							for (String exclude : excludeList) {
								if (exclude.equals(dependencyUri)) {
									addDep = false;
									break;
								}
							}
							if (addDep) {
								module.dependencies.add(dep);
								shimContent.append("'");
								shimContent.append(dep);
								shimContent.append("',");
								AstVisitor visitor = new AstVisitor(dep, moduleMap, pluginRefList, missingNamesList, config, pathStack, excludeList, pageConfigString, scanCJSRequires, shims);
								if (visitor.getError() != null) {
									error = visitor.getError();
									return;
								}
							}
						}
						shimContent.deleteCharAt(shimContent.length()-1);
						shimContent.append("], ");
					}
					shimContent.append("function() {\n");
					String exports = (String)shim.get("exports");
					if (shim.containsKey("init")) {
						shimContent.append("\tvar initFunc = cfg.shim['"+moduleId+"'].init;\n");
						shimContent.append("\tvar initRet = initFunc.apply(root, arguments);\n");
						if (exports != null) {
							shimContent.append("\tif (initRet) { return initRet; } else { return root." + exports + "; }\n");
						} else {
							shimContent.append("\tif (initRet) { return initRet; } else { return {}; }\n");
						}
					} else if (exports != null) {
						shimContent.append("return root." + exports + ";\n");
					}
					shimContent.append("});\n}(this, zazl._getConfig()));\n");
					shims.put(this.url, shimContent.toString());
				}
			}
		}
	}
	
	private class Module {
		public String id = null;
		public String uri = null;
		public boolean scanned = false;
		public boolean defineFound = false;
		public List<String> dependencies = new ArrayList<String>();
		
		public Module(String id, String uri) {
			this.id = id;
			this.uri = uri;
		}
	}
}
