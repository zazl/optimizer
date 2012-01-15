/*
    Copyright (c) 2004-2012, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/

var jsp = require("uglify-js").parser;
var uglify = require("uglify-js").uglify;

exports.analyze = function (script) {
	var ast = jsp.parse(script, false, true);
	var w = uglify.ast_walker();
	var dependencies = []
	w.with_walkers({
	    "call": function(expr, args) {
			if (expr[0] === "name" && expr[1] === "amdlite") {
				if (args[1][0].name === "array") {
					var dependencyArg = args[1][1];
					for (var i = 0; i < dependencyArg.length; i++) {
						if (dependencyArg[i][0].name === "string") {
							var dependency = dependencyArg[i][1];
							dependencies.push(dependency);
						}
					}
				}
			}
		}
	}, function(){
	    w.walk(ast);
	});
	
	return dependencies;
};