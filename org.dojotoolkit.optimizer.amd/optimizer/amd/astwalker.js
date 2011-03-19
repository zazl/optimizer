/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
var moduleCreator = require("./module");
var resourceloader = require('zazlutil').resourceloader;
var jsp = require("uglify-js").parser;
var slice = jsp.slice;

function HOP(obj, prop) {
    return Object.prototype.hasOwnProperty.call(obj, prop);
};

var MAP;

(function(){
    MAP = function(a, f, o) {
        var ret = [];
        for (var i = 0; i < a.length; ++i) {
            var val = f.call(o, a[i], i);
            if (val instanceof AtTop) ret.unshift(val.v);
            else ret.push(val);
        }
        return ret;
    };
    MAP.at_top = function(val) { return new AtTop(val) };
    function AtTop(val) { this.v = val };
})();

function ast_walker(ast) {
	function _vardefs(defs) {
	    return MAP(defs, function(def){
	        var a = [ def[0] ];
	        if (def.length > 1)
	                a[1] = walk(def[1]);
	        return a;
	    });
	};
	
	var walkers = {
	    "string": function(str) {
	        return [ "string", str ];
	    },
	    "num": function(num) {
	        return [ "num", num ];
	    },
	    "name": function(name) {
	        return [ "name", name ];
	    },
	    "toplevel": function(statements) {
	        return [ "toplevel", MAP(statements, walk) ];
	    },
	    "block": function(statements) {
	        var out = [ "block" ];
	        if (statements != null)
	            out.push(MAP(statements, walk));
	        return out;
	    },
	    "var": function(defs) {
	        return [ "var", _vardefs(defs) ];
	    },
	    "const": function(defs) {
	        return [ "const", _vardefs(defs) ];
	    },
	    "try": function(t, c, f) {
	        return [
	            "try",
	            MAP(t, walk),
	            c != null ? [ c[0], MAP(c[1], walk) ] : null,
	            f != null ? MAP(f, walk) : null
	        ];
	    },
	    "throw": function(expr) {
	        return [ "throw", walk(expr) ];
	    },
	    "new": function(ctor, args) {
	        return [ "new", walk(ctor), MAP(args, walk) ];
	    },
	    "switch": function(expr, body) {
	        return [ "switch", walk(expr), MAP(body, function(branch){
	            return [ branch[0] ? walk(branch[0]) : null,
	                 MAP(branch[1], walk) ];
	        }) ];
	    },
	    "break": function(label) {
	        return [ "break", label ];
	    },
	    "continue": function(label) {
	        return [ "continue", label ];
	    },
	    "conditional": function(cond, t, e) {
	        return [ "conditional", walk(cond), walk(t), walk(e) ];
	    },
	    "assign": function(op, lvalue, rvalue) {
	        return [ "assign", op, walk(lvalue), walk(rvalue) ];
	    },
	    "dot": function(expr) {
	        return [ "dot", walk(expr) ].concat(slice(arguments, 1));
	    },
	    "call": function(expr, args) {
	        return [ "call", walk(expr), MAP(args, walk) ];
	    },
	    "function": function(name, args, body) {
	        return [ "function", name, args.slice(), MAP(body, walk) ];
	    },
	    "defun": function(name, args, body) {
	        return [ "defun", name, args.slice(), MAP(body, walk) ];
	    },
	    "if": function(conditional, t, e) {
	        return [ "if", walk(conditional), walk(t), walk(e) ];
	    },
	    "for": function(init, cond, step, block) {
	        return [ "for", walk(init), walk(cond), walk(step), walk(block) ];
	    },
	    "for-in": function(has_var, key, hash, block) {
	        return [ "for-in", has_var, key, walk(hash), walk(block) ];
	    },
	    "while": function(cond, block) {
	        return [ "while", walk(cond), walk(block) ];
	    },
	    "do": function(cond, block) {
	        return [ "do", walk(cond), walk(block) ];
	    },
	    "return": function(expr) {
	        return [ "return", walk(expr) ];
	    },
	    "binary": function(op, left, right) {
	        return [ "binary", op, walk(left), walk(right) ];
	    },
	    "unary-prefix": function(op, expr) {
	        return [ "unary-prefix", op, walk(expr) ];
	    },
	    "unary-postfix": function(op, expr) {
	        return [ "unary-postfix", op, walk(expr) ];
	    },
	    "sub": function(expr, subscript) {
	        return [ "sub", walk(expr), walk(subscript) ];
	    },
	    "object": function(props) {
	        return [ "object", MAP(props, function(p){
	                return [ p[0], walk(p[1]) ];
	        }) ];
	    },
	    "regexp": function(rx, mods) {
	        return [ "regexp", rx, mods ];
	    },
	    "array": function(elements) {
	        return [ "array", MAP(elements, walk) ];
	    },
	    "stat": function(stat) {
	        return [ "stat", walk(stat) ];
	    },
	    "seq": function() {
	        return [ "seq" ].concat(MAP(slice(arguments), walk));
	    },
	    "label": function(name, block) {
	        return [ "label", name, walk(block) ];
	    },
	    "with": function(expr, block) {
	        return [ "with", walk(expr), walk(block) ];
	    },
	    "atom": function(name) {
	        return [ "atom", name ];
	    }
	};
	
	var user = {};
	var stack = [];
	function walk(ast) {
	    if (ast == null)
	        return null;
	    try {
	    	parent = stack[stack.length-1];
	        stack.push(ast);
	        var type = ast[0];
	        //print("type : ["+type+"]");
	        var gen = user[type];
	        if (gen) {
	            var ret = gen.apply(ast, ast.slice(1));
	            if (ret != null)
	                return ret;
	        }
	        gen = walkers[type];
	        return gen.apply(ast, ast.slice(1));
	    } finally {
	        stack.pop();
	    }
	};
	
    function with_walkers(walkers, cont){
        var save = {}, i;
        for (i in walkers) if (HOP(walkers, i)) {
                save[i] = user[i];
                user[i] = walkers[i];
        }
        var ret = cont();
        for (i in save) if (HOP(save, i)) {
                if (!save[i]) delete user[i];
                else user[i] = save[i];
        }
        return ret;
    };
	
	return {
        walk: walk,
        with_walkers: with_walkers,
        parent: function() {
            return stack[stack.length - 2]; // last one is current node
        },
        stack: function() {
            return stack;
        }
	};
};

function walker(uri, moduleMap, localizationList, textList, missingNamesList, aliases) {
	if (moduleMap.get(uri) === undefined) {
		var src = resourceloader.readText('/'+uri+'.js');
		if (src === null) {
			throw new Error("Unable to load src for ["+uri+"]");
		}
		var ast = jsp.parse(src, false, true);
		var w = ast_walker();
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
	var w = ast_walker();
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