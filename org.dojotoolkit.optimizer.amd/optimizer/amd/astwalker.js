/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
var moduleCreator = require("./module");
var resourceloader = require('zazlutil').resourceloader;
var jsp = require("uglify-js").parser;
var uglify = require("uglify-js").uglify;

function walker(uri, moduleMap, localizationList, textList, missingNamesList, aliases) {
	if (moduleMap.get(uri) === undefined) {
		var src = resourceloader.readText('/'+uri+'.js');
		if (src === null) {
			throw new Error("Unable to load src for ["+uri+"]");
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
					if (args[0][0] === "string") {
						id = args[0][1];
						dependencyArg = args[1][1];
					} else if (args[0][0] === "array") {
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
							if (dependency.match("^order!")) {
								dependency = dependency.substring(6);
							} else if (dependency.match("^i18n!")) {
								var i18nDependency = dependency.substring(5);
								var localization = {
									bundlepackage : i18nDependency,
									modpath : i18nDependency.substring(0, i18nDependency.lastIndexOf('/')),
									bundlename : i18nDependency.substring(i18nDependency.lastIndexOf('/')+1)
								};
								var add = true;
								for (var j = 0; j < localizationList.length; j++) {
									if (localizationList[j].bundlepackage === localization.bundlepackage) {
										add = false;
										break;
									}
								}
								if (add === true) {
									localizationList.push(localization);
								}
							} else if (dependency.match(".js$")) {
								module.addDependency(dependency);
							} else if (dependency.match("^text!")) {
								var textDependency = dependency.substring(5);
								var add = true;
								for (var k = 0; k < textList.length; k++) {
									if (textList[k] === textDependency) {
										add = false;
										break;
									}
								}
								if (add) {
									textList.push(textDependency);
								}
							} else if (dependency !== "require" && dependency !== "exports" && dependency.indexOf("!") === -1) {
								if (aliases[dependency] !== undefined) {
									dependency = aliases[dependency];
								}
								module.addDependency(dependency);
								walker(dependency, moduleMap, localizationList, textList, missingNamesList, aliases);
							}
						}
					}
				}
			}
		}, function(){
		    w.walk(ast);
		});
	}
}

exports.walker = walker;

function getMissingNameIndex(src) {
	var ast = jsp.parse(src, false, true);
	var w = uglify.ast_walker();
	var nameIndex = -1;
	w.with_walkers({
	    "call": function(expr, args) {
			if (expr[0] === "name" && expr[1] === "define") {
				if (args[0][0] !== "array") {
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