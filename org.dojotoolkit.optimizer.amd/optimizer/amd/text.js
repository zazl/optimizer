/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/

(function () {
	define({
		load: function(name, parentRequire, load, config) {
			load(require('zazlutil').resourceloader.readText(name));
		},
		write: function(pluginName, moduleName, write) {
			var textContent = require('zazlutil').resourceloader.readText(moduleName);
			if (textContent) {
				write("define('text!"+moduleName+"', function () { return '"+this.jsEscape(textContent)+"';});\n");
			}
		},
	    jsEscape: function (content) {
	        return content.replace(/(['\\])/g, '\\$1')
	            .replace(/[\f]/g, "\\f")
	            .replace(/[\b]/g, "\\b")
	            .replace(/[\n]/g, "\\n")
	            .replace(/[\t]/g, "\\t")
	            .replace(/[\r]/g, "\\r");
	    }
	});
}());