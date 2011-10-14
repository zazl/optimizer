/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/

var w = this;
var d = {};
var el = {};
var cache = {};

var has = function(name){
	return cache[name] = typeof cache[name] == "function" ? cache[name](w, d, el) : cache[name];
};

has.add = function(name, test, now){
	cache[name] = now ? test(w, d, el) : test;
};

has.add("host-browser", 1);
has.add("dom", 1);
has.add("dojo-dom-ready-api", 1);
has.add("dojo-sniff", 1);
	
exports.normalize = function(id, expand) {
	var tokens = id.match(/[\?:]|[^:\?]*/g), i = 0,
	get = function(skip){
		var term = tokens[i++];
		if(term == ":"){
			return undefined;
		}else{
			if(tokens[i++] == "?"){
				if(!skip && has(term)){
					return get();
				}else{
					get(true);
					return get(skip);
				}
			}
			return term || undefined;
		}
	};
	id = get();
	return id && expand(id);
};