/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
var moduleCreator = require("./module");
var resourceloader = require('zazlutil').resourceloader;
var jsp = require("uglify-js").parser;
var uglify = require("uglify-js").uglify;

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

function expand(uri, pathStack, aliases) {
	var isRelative = uri.search(/^\./) === -1 ? false : true;
	if (isRelative) {
		var parentPath = pathStack.length > 0 ? pathStack[pathStack.length-1] : "";
		parentPath = parentPath.substring(0, parentPath.lastIndexOf('/')+1);
		uri = parentPath + uri;
		uri = normalize(uri);
		if (aliases[uri] !== undefined) {
			uri = aliases[uri];
		}
	}
	return uri;
}

function walker(uri, exclude, moduleMap, pluginRefList, missingNamesList, aliases, pathStack) {
	uri = expand(uri, pathStack, aliases);
	if (moduleMap.get(uri) === undefined) {
		var src = resourceloader.readText('/'+uri+'.js');
		if (src === null) {
			throw new Error("Unable to load src for ["+uri+"]. Module ["+(pathStack.length > 0 ? pathStack[pathStack.length-1] : "root")+"] has a dependency on it.");
		}
		var ast = jsp.parse(src, false, true);
		var w = uglify.ast_walker();
		var id = uri;
		var module = moduleCreator.createModule(id, uri);
		moduleMap.add(uri, module);
		w.with_walkers({
		    "call": function(expr, args) {
				if (expr[0] === "name" && (expr[1] === "define" || expr[1] === "require")) {
					var dependencyArg;
					if (args[0][0].name === "string" && args[1][0].name === "array") {
						id = args[0][1];
						dependencyArg = args[1][1];
					} else if (args[0][0].name === "array") {
                        if (expr[1] === "define") {
    	                    var start = w.parent()[0].start;
    						var nameIndex = start.pos + (src.substring(start.pos).indexOf('(')+1);
                        	missingNamesList.push({uri: uri, nameIndex: nameIndex});
                        }
						dependencyArg = args[0][1];
					}
					if (dependencyArg !== undefined) {
						for (var i = 0; i < dependencyArg.length; i++) {
							var dependency = dependencyArg[i][1];
							var keepWalking = true;
							if (dependencyArg[i][0].name !== "string") {
								keepWalking = false;
							} else if (dependency.match(".+!.+")) {
								keepWalking = false;
								pathStack.push(uri);
								var pluginName = dependency.substring(0, dependency.indexOf('!'));
								var parentPath = pathStack.length > 0 ? pathStack[pathStack.length-1] : "";
								pluginName = expand(pluginName, pathStack, aliases);
								var pluginValue = dependency.substring(dependency.indexOf('!')+1);
								pluginValue = expand(pluginValue, pathStack, aliases);
								if (pluginRefList[pluginName] === undefined) {
									pluginRefList[pluginName] = [];
								}
								pluginRefList[pluginName].push(pluginValue);
								pathStack.pop();
							} else if (dependency.match(".js$")) {
								keepWalking = false;
								pathStack.push(uri);
								dependency = expand(dependency, pathStack, aliases);
								module.addDependency(dependency);
								pathStack.pop();
							}
							if (aliases[dependency] !== undefined) {
								dependency = aliases[dependency];
							}
							for (var k = 0; k < exclude.length; k++) {
								if (dependency === exclude[k]) {
									keepWalking = false;
									break;
								}
							}
							if (keepWalking && dependency !== "require" && dependency !== "exports" && dependency !== "module" && dependency.indexOf("!") === -1) {
								pathStack.push(uri);
								dependency = expand(dependency, pathStack, aliases);
								module.addDependency(dependency);
								walker(dependency, exclude, moduleMap, pluginRefList, missingNamesList, aliases, pathStack);
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