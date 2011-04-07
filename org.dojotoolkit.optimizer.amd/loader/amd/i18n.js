/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
(function () {
	
// From http://requirejs.org i18n plugin	
var nlsRegExp = /(^.*(^|\/)nls(\/|$))([^\/]*)\/?([^\/]*)/;
var empty = {};

/**
 * mixin function from http://requirejs.org require.js
 */
function mixin(target, source, force) {
    for (var prop in source) {
        if (!(prop in empty) && (!(prop in target) || force)) {
            target[prop] = source[prop];
        }
    }
};

define({
	load: function(name, parentRequire, load, config) {
		var match = nlsRegExp.exec(name);
        var prefix = match[1];
        var locale = match[4];
        var suffix = match[5]; 
		var splitLocale;
		var masterName;
        var value = {};
		
		if (match[5]) {
            masterName = prefix + suffix;
		} else {
            suffix = match[4];
            locale = config.locale || (config.locale = typeof navigator === "undefined" ? "root" : (navigator.language || navigator.userLanguage || "root").toLowerCase());
            masterName = name;
		}
		splitLocale = locale.split('-');
		console.log(prefix+":"+locale+":"+suffix+":"+masterName+":"+splitLocale);
		root = parentRequire(masterName);
		mixin(value, root["root"]);
		if (root[splitLocale[0]] && root[splitLocale[0]] === true) {
			mixin(value, parentRequire(prefix + splitLocale[0] + '/' + suffix), true);
		}
		if (root[locale] && root[locale] === true) {
			mixin(value, parentRequire(prefix + locale + '/' + suffix), true);
		}
		load(value);
	}
});

}());
