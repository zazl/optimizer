/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/

require("jsutil/array/reduce");
var jsp = require("uglify-js").parser;
var pro = require("uglify-js").uglify;

escapeString = function (str) {
	return ("\"" + str.replace(/(["\\])/g, "\\$1") + "\"").replace(/[\f]/g, "\\f").replace(/[\b]/g, "\\b").replace(/[\n]/g, "\\n").replace(/[\t]/g, "\\t").replace(/[\r]/g, "\\r");
};

exports.compress = function(src, escape) {
	var ast = jsp.parse(src);
	ast = pro.ast_mangle(ast);
	ast = pro.ast_squeeze(ast);
	var compressedSrc;
	if (escape) {
		compressedSrc = escapeString(pro.gen_code(ast)); 
	} else {
		compressedSrc = pro.gen_code(ast);
	}
	return compressedSrc;
}