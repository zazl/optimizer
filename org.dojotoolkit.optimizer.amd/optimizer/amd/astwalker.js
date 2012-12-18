/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
var moduleCreator = require("./module");
var resourceloader = require('zazlutil').resourceloader;
var astcache = require('zazlutil').astcache;

var opts = Object.prototype.toString;
function isArray(it) { return opts.call(it) === "[object Array]"; };

function getParentId(pathStack) {
	return pathStack.length > 0 ? pathStack[pathStack.length-1] : "";
};

function countSegments(path) {
	var count = 0;
	for (var i = 0; i < path.length; i++) {
		if (path.charAt(i) === '/') {
			count++;
		}
	}
	return count;
};

function findMapping(path, depId, cfg) {
	var mapping;
	var segmentCount = -1;
	for (var id in cfg.map) {
		if (depId.indexOf(id) === 0) {
			var foundSegmentCount = countSegments(id);
			if (foundSegmentCount > segmentCount) {
				var mapEntry = cfg.map[id];
				if (mapEntry[path] !== undefined) {
					mapping = mapEntry[path];
					segmentCount = foundSegmentCount;
				}
			}
		}
	}
	if (mapping === undefined && cfg.map["*"] !== undefined && cfg.map["*"][path] !== undefined) {
		mapping = cfg.map["*"][path];
	}
	return mapping;
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
    if (path.charAt(0) !== '/') {
    	path = config.baseUrl + path;
    }
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
	for (pkgName in config.pkgs) {
	    if (path === pkgName) {
	    	return config.pkgs[pkgName].name + '/' + config.pkgs[pkgName].main;
	    }
	}

	var segments = path.split("/");
	for (var i = segments.length; i >= 0; i--) {
        var parent = segments.slice(0, i).join("/");
		var mapping = findMapping(parent, getParentId(pathStack), config);
    	if (mapping) {
    		segments.splice(0, i, mapping);
    		return segments.join("/");
    	}
	}

	return path;
};

exports.expand = expand;

function processPluginRef(pluginName, resourceName, pathStack, config) {
	var value;
	var normalizedName;
	var dependency;
	var moduleUrl;
	if (config.plugins[pluginName]) {
		try {
			var plugin = require(config.plugins[pluginName].proxy);
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
				normalizedName = dependency = plugin.normalize(resourceName, cfg, function(id) {
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

function scanForRequires(ast, requires) {
	for (var p in ast) {
		if (p === "type" && ast[p] === "CallExpression" && ast["callee"]) {
			var callee = ast["callee"];
			if (callee.name && callee.name === "require") {
				var arg1 = ast["arguments"][0];
				if (arg1.type === "Literal") {
					requires.push(arg1.value);
				}
			}
		}
		if (isArray(ast[p])) {
			var a = ast[p];
			for (var i = 0; i < a.length; i++) {
				if (typeof a[i] == 'object') {
					scanForRequires(a[i], requires);
				}
			}
		} else if (typeof ast[p] == 'object') {
			scanForRequires(ast[p], requires);
		}
	}
};

function getDependencies(src, expression, scanCJSRequires) {
	var dependencies = [];
	var nameIndex;

	var args = expression.arguments;
	for (var j = 0; j < args.length; j++) {
		if (j === 0 && args[j].type !== "Literal") {
			nameIndex = args[j].range[0];
		}
		if (args[j].type === "ArrayExpression" && expression.callee.name === "define") {
			var elements = args[j].elements;
			for (var k = 0; k < elements.length; k++) {
				dependencies.push({value: elements[k].value, type: elements[k].type});
			}
		} else if (args[j].type === "FunctionExpression" && expression.callee.name === "define") {
			if (scanCJSRequires) {
				var requires = [];
				scanForRequires(args[j].body, requires);
				for (var x = 0; x < requires.length; x++) {
					dependencies.push({value: requires[x], type: "Literal"});
				}
			}
		}
	}

	return {deps:dependencies, nameIndex: nameIndex};
};

function findDefine(ast) {
	var expr;
	for (var p in ast) {
		if (p === "type" && ast[p] === "CallExpression") {
			var callee = ast["callee"];
			var arguments = ast["arguments"];
			if (callee && callee.type === "Identifier" && callee.name && callee.name === "define") {
				return {arguments: arguments, callee: {name: "define"}};
			} else if (callee && callee.type === "ConditionalExpression") {
		    	var left = {type : callee.consequent.type, name : callee.consequent.name === undefined ? "" : callee.consequent.name};
		    	var right = {type : callee.alternate.type, name : callee.alternate.name === undefined ? "" : callee.alternate.name};
		    	if ((left.type === "Identifier" && left.name === "define") || (right.type === "Identifier" && right.name === "define")) {
		    		return {arguments: arguments, callee: {name: "define"}};
		    	}
			}
		} else {
			if (isArray(ast[p])) {
				var a = ast[p];
				for (var i = 0; i < a.length; i++) {
					if (typeof a[i] == 'object') {
						expr = findDefine(a[i]);
						if (expr) {
							return expr;
						}
					}
				}
			} else if (typeof ast[p] == 'object') {
				expr = findDefine(ast[p]);
				if (expr) {
					return expr;
				}
			}
		}
	}
	return expr;
};

function findShim(module, exclude, moduleMap, pluginRefList, missingNamesList, config, shims, pathStack, walker) {
	if (config.shim) {
		var shim = config.shim[module.id];
		if (shim) {
			if (isArray(shim)) {
				shim = {deps: shim};
			}
			var shimContent = "\n(function(root, cfg) {\ndefine('";
			shimContent += module.id;
			shimContent += "', ";
			if (shim.deps) {
				shimContent += "[";
				for (var i = 0; i < shim.deps.length; i++) {
					var shimDepUri = idToUrl(shim.deps[i], config);
					if (shimDepUri.charAt(0) != '/') {
						shimDepUri = '/'+shimDepUri;
					}
					var addDependency = true;
					for (var k = 0; k < exclude.length; k++) {
						if (shimDepUri === exclude[k]) {
							addDependency = false;
							break;
						}
					}
					if (addDependency) {
						module.addDependency(shim.deps[i]);
						shimContent += "'";
						shimContent += shim.deps[i];
						shimContent += "'";
						if (i < (shim.deps.length-1)) {
							shimContent += ",";
						}
						walker(shim.deps[i], exclude, moduleMap, pluginRefList, missingNamesList, config, shims, pathStack);
					}
				}
				shimContent += "], ";
			}
			shimContent += "function() {\n";
			if (shim.init) {
				shimContent += "\tvar initFunc = cfg.shim['"+module.id+"'].init;\n";
				shimContent += "\tvar initRet = initFunc.apply(root, arguments);\n";
				if (shim.exports) {
					shimContent += "\tif (initRet) { return initRet; } else { return root." + shim.exports + "; }\n";
				} else {
					shimContent += "\tif (initRet) { return initRet; } else { return {}; }\n";
				}
			} else if (shim.exports) {
				shimContent += "return root." + shim.exports + ";\n";
			}
			shimContent += "});\n}(this, zazl._getConfig()));\n";
			shims[module.uri] = shimContent;
 		}
	}
};

function esprimaWalker(uri, exclude, moduleMap, pluginRefList, missingNamesList, config, shims, pathStack) {
	if (uri === "require" || uri === "exports" || uri === "module") {
		moduleMap.add(uri, moduleCreator.createModule(uri, uri));
		return;
	}

	if (uri.match(".+!")) {
		var pluginName = uri.substring(0, uri.indexOf('!'));
		pluginName = expand(pluginName, pathStack, config);
		var pluginValue = uri.substring(uri.indexOf('!')+1);
		if (pluginRefList[pluginName] === undefined) {
			pluginRefList[pluginName] = [];
		}
		var pluginRef = processPluginRef(pluginName, pluginValue, pathStack, config);
		if (pluginRef.dependency) {
			var dependencyUri = idToUrl(pluginRef.dependency, config);
			if (dependencyUri.charAt(0) !== '/') {
				dependencyUri = '/'+dependencyUri;
			}
			var addDependency = true;
			for (var k = 0; k < exclude.length; k++) {
				if (dependencyUri === exclude[k]) {
					addDependency = false;
					break;
				}
			}
			if (addDependency) {
				esprimaWalker(pluginRef.dependency, exclude, moduleMap, pluginRefList, missingNamesList, config, shims, [uri]);
			}
		}
		pluginRefList[pluginName].push(pluginRef);
		uri = pluginName;
	} else {
		uri = expand(uri, pathStack, config);
	}
	var url = idToUrl(uri, config);
	if (url.charAt(0) !== '/') {
		url = '/'+url;
	}

	if (moduleMap.get(uri) === undefined) {
		var src = resourceloader.readText(url+'.js');
		if (src === null) {
			throw new Error("Unable to load src for ["+url+"]. Module ["+(pathStack.length > 0 ? pathStack[pathStack.length-1] : "root")+"] has a dependency on it.");
		}
		var ast = astcache.getAst(url+'.js', config.astparser);
		if (ast === null) {
			var esprima = require("esprima/esprima");
			ast = esprima.parse(src, {range: true});
		}
		var defineExpr = findDefine(ast);
		var id = uri;
		var module = moduleCreator.createModule(id, url);
		moduleMap.add(uri, module);
		if (defineExpr === undefined) {
			findShim(module, exclude, moduleMap, pluginRefList, missingNamesList, config, shims, pathStack, esprimaWalker);
			return;
		} else {
			module.defineFound = true;
		}
		var depInfo = getDependencies(src, defineExpr, config.scanCJSRequires);
		if (depInfo.nameIndex) {
			missingNamesList.push({uri: url, id: id, nameIndex: depInfo.nameIndex});
		}

		var dependency, keepWalking, type;
		for (var i = 0; i < depInfo.deps.length; i++) {
			dependency = depInfo.deps[i].value;
			type = depInfo.deps[i].type;
			keepWalking = true;

			if (type !== "Literal") {
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
					if (dependencyUri.charAt(0) !== '/') {
						dependencyUri = '/'+dependencyUri;
					}
					var addDependency = true;
					for (var k = 0; k < exclude.length; k++) {
						if (dependencyUri === exclude[k]) {
							addDependency = false;
							break;
						}
					}
					if (addDependency) {
						module.addDependency(pluginRef.dependency);
						esprimaWalker(pluginRef.dependency, exclude, moduleMap, pluginRefList, missingNamesList, config, shims, [dependency]);
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

			if (keepWalking &&
				dependency !== config.baseUrl+"require" &&
				dependency !== config.baseUrl+"exports" &&
				dependency !== config.baseUrl+"module" &&
				dependency.indexOf("!") === -1) {
				pathStack.push(uri);
				dependency = expand(dependency, pathStack, config);
				var dependencyUri = idToUrl(dependency, config);
				if (dependencyUri.charAt(0) !== '/') {
					dependencyUri = '/'+dependencyUri;
				}
				var addDependency = true;
				for (var k = 0; k < exclude.length; k++) {
					if (dependencyUri === exclude[k]) {
						addDependency = false;
						break;
					}
				}
				if (addDependency) {
					module.addDependency(dependency);
					esprimaWalker(dependency, exclude, moduleMap, pluginRefList, missingNamesList, config, shims, pathStack);
				}
				pathStack.pop();
			}
		}
	}
};

function uglifyjsWalker(uri, exclude, moduleMap, pluginRefList, missingNamesList, config, shims, pathStack) {
	if (uri === "require" || uri === "exports" || uri === "module") {
		moduleMap.add(uri, moduleCreator.createModule(uri, uri));
		return;
	}

	var jsp = require("uglify-js").parser;
	var uglify = require("uglify-js").uglify;
	
	if (uri.match(".+!")) {
		var pluginName = uri.substring(0, uri.indexOf('!'));
		pluginName = expand(pluginName, pathStack, config);
		var pluginValue = uri.substring(uri.indexOf('!')+1);
		if (pluginRefList[pluginName] === undefined) {
			pluginRefList[pluginName] = [];
		}
		var pluginRef = processPluginRef(pluginName, pluginValue, pathStack, config);
		if (pluginRef.dependency) {
			var dependencyUri = idToUrl(pluginRef.dependency, config);
			if (dependencyUri.charAt(0) !== '/') {
				dependencyUri = '/'+dependencyUri;
			}
			var addDependency = true;
			for (var k = 0; k < exclude.length; k++) {
				if (dependencyUri === exclude[k]) {
					addDependency = false;
					break;
				}
			}
			if (addDependency) {
				uglifyjsWalker(pluginRef.dependency, exclude, moduleMap, pluginRefList, missingNamesList, config, shims, [uri]);
			}
		}
		pluginRefList[pluginName].push(pluginRef);
		uri = pluginName;
	} else {
		uri = expand(uri, pathStack, config);
	}
	var url = idToUrl(uri, config);
	if (url.charAt(0) !== '/') {
		url = '/'+url;
	}

	if (moduleMap.get(uri) === undefined) {
		var src = resourceloader.readText(url+'.js');
		if (src === null) {
			throw new Error("Unable to load src for ["+url+"]. Module ["+(pathStack.length > 0 ? pathStack[pathStack.length-1] : "root")+"] has a dependency on it.");
		}
		var ast = astcache.getAst(uri+'.js', config.astparser);
		if (ast === null) {
			ast = jsp.parse(src, false, true);
		}
		var w = uglify.ast_walker();
		var id = uri;
		var module = moduleCreator.createModule(id, url);
		moduleMap.add(uri, module);
		w.with_walkers({
		    "call": function(expr, args) {
				if (expr[0] === "name" && (expr[1] === "define" || expr[1] === "require")) {
					var dependencyArg;
                    if (expr[1] === "define") {
                    	module.defineFound = true;
                    	if (args[0][0].name !== "string") {
                    		var start = w.parent()[0].start;
                    		var nameIndex = start.pos + (src.substring(start.pos).indexOf('(')+1);
                    		missingNamesList.push({uri: url, id: id, nameIndex: nameIndex});
                    	}
                    }
				    if (expr[1] === "require") { 
				    	if (args[0][0].name === "string" && config.scanCJSRequires) {
							dependencyArg = [args[0][1]];
				    	} else {
							dependencyArg = undefined;
				    	}
					} else if (args[0][0].name === "string" && args[1][0].name === "array") {
						id = args[0][1];
						dependencyArg = args[1][1];
					} else if (args[0][0].name === "array") {
                        dependencyArg = args[0][1];
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
									if (dependencyUri.charAt(0) !== '/') {
										dependencyUri = '/'+dependencyUri;
									}
									var addDependency = true;
									for (var k = 0; k < exclude.length; k++) {
										if (dependencyUri === exclude[k]) {
											addDependency = false;
											break;
										}
									}
									if (addDependency) {
										module.addDependency(pluginRef.dependency);
										uglifyjsWalker(pluginRef.dependency, exclude, moduleMap, pluginRefList, missingNamesList, config, shims, [dependency]);
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
							if (keepWalking &&
								dependency !== config.baseUrl+"require" &&
								dependency !== config.baseUrl+"exports" &&
								dependency !== config.baseUrl+"module" &&
								dependency.indexOf("!") === -1) {
								pathStack.push(uri);
								dependency = expand(dependency, pathStack, config);
								var dependencyUri = idToUrl(dependency, config);
								if (dependencyUri.charAt(0) !== '/') {
									dependencyUri = '/'+dependencyUri;
								}
								var addDependency = true;
								for (var k = 0; k < exclude.length; k++) {
									if (dependencyUri === exclude[k]) {
										addDependency = false;
										break;
									}
								}
								if (addDependency) {
									module.addDependency(dependency);
									uglifyjsWalker(dependency, exclude, moduleMap, pluginRefList, missingNamesList, config, shims, pathStack);
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
		
		if (module.defineFound === false) {
			findShim(module, exclude, moduleMap, pluginRefList, missingNamesList, config, shims, pathStack, uglifyjsWalker);
		}
	}
};

function walker(uri, exclude, moduleMap, pluginRefList, missingNamesList, config, shims, pathStack) {
	if (!config.astparser) {
		config.astparser = "uglifyjs";
	}
	if (config.excludes) {
		for (var i = 0; i < config.excludes.length; i++) {
			var excludeUri = idToUrl(config.excludes[i], config);
			exclude.push(excludeUri);
		}
	}
	if (config.astparser === "uglifyjs") {
		print("AST parsing ["+uri+"] using uglifyjs");
		uglifyjsWalker(uri, exclude, moduleMap, pluginRefList, missingNamesList, config, shims, pathStack);
	} else 	if (config.astparser === "esprima") {
		print("AST parsing ["+uri+"] using esprima");
		esprimaWalker(uri, exclude, moduleMap, pluginRefList, missingNamesList, config, shims, pathStack);
	} else {
		throw new Error("Unknown astparser value ["+config.astparser+"]");
	}
};

exports.walker = walker;

function getMissingNameIndex(src) {
	var jsp = require("uglify-js").parser;
	var uglify = require("uglify-js").uglify;
	
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