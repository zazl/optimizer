/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
var moduleCreator = require("./module");
var resourceloader = require('zazlutil').resourceloader;
var jsp = require("uglify-js").parser;
var uglify = require("uglify-js").uglify;

function getParentId(pathStack) {
	return pathStack.length > 0 ? pathStack[pathStack.length-1] : "";
};

function idToUrl(path, config) {
	var segments = path.split("/");
	for (var i = segments.length; i >= 0; i--) {
		var pkg;
        var parent = segments.slice(0, i).join("/");
        if (config.paths[parent]) {
        	segments.splice(0, i, config.paths[parent]);
            break;
        }else if ((pkg = config.pkgs[parent])) {
        	var pkgPath;
            if (path === pkg.name) {
                pkgPath = pkg.location + '/' + pkg.main;
            } else {
                pkgPath = pkg.location;
            }
			segments.splice(0, i, pkgPath);
			break;
        }
	}
	path = segments.join("/");
	path = normalize(path);
	return path;
};

function normalize(path) {
	var segments = path.split('/');
	var skip = 0;

	for (var i = segments.length; i >= 0; i--) {
		var segment = segments[i];
		if (segment === '.') {
			segments.splice(i, 1);
		} else if (segment === '..') {
			segments.splice(i, 1);
			skip++;
		} else if (skip) {
			segments.splice(i, 1);
			skip--;
		}
	}
	return segments.join('/');
};

function expand(path, pathStack, config) {
	var isRelative = path.search(/^\./) === -1 ? false : true;
	if (isRelative) {
        var pkg;
        if ((pkg = config.pkgs[getParentId(pathStack)])) {
            path = pkg.name + "/" + path;
        } else {
            path = getParentId(pathStack) + "/../" + path;
        }
		path = normalize(path);
	}
	return path;
};

function processPluginRef(pluginName, resourceName, pathStack, config) {
	var value;
	var normalizedName;
	var dependency;
	var moduleUrl;
	if (config.plugins[pluginName]) {
		try {
			var plugin = require(config.plugins[pluginName]);
			if (plugin.write) {
				normalizedName = expand(resourceName, pathStack, config);
				moduleUrl = idToUrl(normalizedName, config);
				plugin.write(pluginName, normalizedName, function(writeOutput){
					value = writeOutput;
				}, moduleUrl);
			} 
			if (plugin.normalize) {
				var cfg = config;
				var stack = pathStack;
				normalizedName = dependency = plugin.normalize(resourceName, function(id) {
					return expand(id, stack, cfg);
				});
				if (normalizedName === undefined) {
					normalizedName = expand(resourceName, pathStack, config);
				}
				moduleUrl = idToUrl(normalizedName, config);
			}
		} catch (exc) {
			print("Unable to process plugin ["+pluginName+"]:"+exc);
		}
	} else {
		normalizedName = expand(resourceName, pathStack, config);
		moduleUrl = idToUrl(normalizedName, config);
	}
	return {name:resourceName, normalizedName: normalizedName, value: value, dependency: dependency, moduleUrl : moduleUrl};
};

function walker(uri, exclude, moduleMap, pluginRefList, missingNamesList, config, pathStack) {
	uri = expand(uri, pathStack, config);
	var url = idToUrl(uri, config);
	if (moduleMap.get(uri) === undefined) {
		var src = resourceloader.readText('/'+url+'.js');
		if (src === null) {
			throw new Error("Unable to load src for ["+url+"]. Module ["+(pathStack.length > 0 ? pathStack[pathStack.length-1] : "root")+"] has a dependency on it.");
		}
		var ast = jsp.parse(src, false, true);
		var w = uglify.ast_walker();
		var id = uri;
		var module = moduleCreator.createModule(id, url);
		moduleMap.add(uri, module);
		w.with_walkers({
		    "call": function(expr, args) {
				if (expr[0] === "name" && (expr[1] === "define" || expr[1] === "require")) {
					var dependencyArg;
				    if (expr[1] === "require") { 
				    	if (args[0][0].name === "string") {
							dependencyArg = [args[0][1]];
				    	} else {
							dependencyArg = undefined;
				    	}
					} else if (args[0][0].name === "string" && args[1][0].name === "array") {
						id = args[0][1];
						dependencyArg = args[1][1];
					} else if (args[0][0].name === "array" || args[0][0].name === "function") {
                        if (expr[1] === "define") {
    	                    var start = w.parent()[0].start;
    						var nameIndex = start.pos + (src.substring(start.pos).indexOf('(')+1);
                        	missingNamesList.push({uri: url, id: id, nameIndex: nameIndex});
                        }
                        if (args[0][0].name === "array") {
                        	dependencyArg = args[0][1];
                        }
					}
					if (dependencyArg !== undefined) {
						for (var i = 0; i < dependencyArg.length; i++) {
							var dependency = dependencyArg[i][1];
							var keepWalking = true;
							if (dependencyArg[i][0].name !== "string") {
								keepWalking = false;
							} else if (dependency.match(".+!")) {
								pathStack.push(uri);
								var pluginName = dependency.substring(0, dependency.indexOf('!'));
								pluginName = expand(pluginName, pathStack, config);
								var pluginValue = dependency.substring(dependency.indexOf('!')+1);
								if (pluginRefList[pluginName] === undefined) {
									pluginRefList[pluginName] = [];
								}
								var pluginRef = processPluginRef(pluginName, pluginValue, pathStack, config);
								if (pluginRef.dependency) {
									var dependencyUri = idToUrl(pluginRef.dependency, config);
									var addDependency = true;
									for (var k = 0; k < exclude.length; k++) {
										if (dependencyUri === exclude[k]) {
											addDependency = false;
											break;
										}
									}
									if (addDependency) {
										module.addDependency(pluginRef.dependency);
										walker(pluginRef.dependency, exclude, moduleMap, pluginRefList, missingNamesList, config, [dependency]);
									}
								}
								pluginRefList[pluginName].push(pluginRef);
								pathStack.pop();
								dependency = pluginName;
							} else if (dependency.match(".js$")) {
								keepWalking = false;
								pathStack.push(uri);
								dependency = expand(dependency, pathStack, config);
								module.addDependency(dependency);
								pathStack.pop();
							}
							if (keepWalking && dependency !== "require" && dependency !== "exports" && dependency !== "module" && dependency.indexOf("!") === -1) {
								pathStack.push(uri);
								dependency = expand(dependency, pathStack, config);
								var dependencyUri = idToUrl(dependency, config);
								var addDependency = true;
								for (var k = 0; k < exclude.length; k++) {
									if (dependencyUri === exclude[k]) {
										addDependency = false;
										break;
									}
								}
								if (addDependency) {
									module.addDependency(dependency);
									walker(dependency, exclude, moduleMap, pluginRefList, missingNamesList, config, pathStack);
								}
								pathStack.pop();
							}
						}
					}
				}
			}
		}, function(){
		    w.walk(ast);
		});
	}
};

exports.walker = walker;

function getMissingNameIndex(src) {
	var ast = jsp.parse(src, false, true);
	var w = uglify.ast_walker();
	var nameIndex = -1;
	w.with_walkers({
	    "call": function(expr, args) {
			if (expr[0] === "name" && expr[1] === "define") {
				if (args[0][0].name !== "string") {
					nameIndex = w.parent()[0].start.pos + (src.substring(w.parent()[0].start.pos).indexOf('(')+1);
				}
			}
		}
	}, function(){
	    w.walk(ast);
	});
	return nameIndex;
};

exports.getMissingNameIndex = getMissingNameIndex;