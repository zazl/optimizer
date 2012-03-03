/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/

function jsEscape(content) {
    return content.replace(/(['\\])/g, '\\$1')
        .replace(/[\f]/g, "\\f")
        .replace(/[\b]/g, "\\b")
        .replace(/[\n]/g, "\\n")
        .replace(/[\t]/g, "\\t")
        .replace(/[\r]/g, "\\r");
};

exports.write = function(pluginName, moduleName, write, moduleUrl) {
	var textContent = require('zazlutil').resourceloader.readText(moduleUrl);
	if (textContent) {
		write("zazl.addToCache('"+moduleName+"', '"+jsEscape(textContent)+"');\n");
	}
};
