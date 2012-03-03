/*
    Copyright (c) 2004-2012, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/

var jsp = require("uglify-js").parser;
var uglify = require("uglify-js").uglify;

function build(ast) {
	var w = uglify.ast_walker();
	var obj;
	var processed = false;

	w.with_walkers({
        "string": function(str) {
        	if (!processed) {
	        	obj = str;
	        	processed = true;
        	}
        },
        "num": function(num) {
        	if (!processed) {
	        	obj = num;
	        	processed = true;
        	}
        },
        "name": function(name) {
        	if (!processed) {
                switch (name) {
	                case "true": {obj = true; break; }
	                case "false": {obj = false; break; }
	                case "null": {obj = null; break; }
	                default: {obj = name; break; }
	            }
	        	processed = true;
        	}
        },
        "atom": function(atom) {
        	if (!processed) {
        		print("atom:"+atom);
                switch (atom) {
	                case "true": {obj = true; break; }
	                case "false": {obj = false; break; }
	                case "null": {obj = null; break; }
	                default: {obj = name; break; }
	            }
	        	processed = true;
        	}
        },
	    "object": function(props) {
        	if (!processed) {
		    	obj = {};
	            uglify.MAP(props, function(p) {
	            	var name = p[0];
	            	var value = build(p[1]);
	            	obj[name] = value;
	            });
	        	processed = true;
        	}
		},
	    "array": function(elements) {
        	if (!processed) {
		    	obj = [];
	            uglify.MAP(elements, function(p) {
	            	var entry = build(p);
	            	obj.push(entry);
	            });
	        	processed = true;
        	}
		},
	}, function(){
        w.walk(ast);
	});
    return obj;
};

exports.analyze = function (script) {
	var ast = jsp.parse(script, false, true);
	var w = uglify.ast_walker();
	var dependencies = [];
	var config = {};

	function readDependencies(dependencyArg) {
		for (var i = 0; i < dependencyArg.length; i++) {
			if (dependencyArg[i][0].name === "string") {
				var dependency = dependencyArg[i][1];
				dependencies.push(dependency);
			}
		}
	};

	w.with_walkers({
	    "call": function(expr, args) {
			if (expr[0] === "name" && expr[1] === "zazl") {
				if (args[0][0].name === "array") {
					readDependencies(args[0][1]);
				} else if (args[0][0].name === "object" && args[1][0].name === "array") {
			        uglify.MAP(args[0][1], function(p) {
			        	config[p[0]] = build(p[1]);
			        });
					readDependencies(args[1][1]);
				}
			}
		}
	}, function(){
	    w.walk(ast);
	});
	
	return {dependencies: dependencies, config: config};
};