/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
(function() {
dojo.provide("dojo.optimizer.localization");

dojo.optimizer.localization.load = function(bundlePackage, langId, langCountryId, root, lang, langCountry) {
	var createBundle = function (properties, parent, loc, bundle) {
		var jsLoc = loc.replace(/-/g, "_");
		var clazz = function () {
		};
		clazz.prototype = parent;
		bundle[jsLoc] = new clazz();
		for (var j in properties) {
			bundle[jsLoc][j] = properties[j];
		}
		dojo['provide'](bundlePackage + "." + jsLoc);
		return bundle[jsLoc];
	}	
	var bundle = dojo['provide'](bundlePackage);
	bundle._built = true;
	var rootProperties = dojo['eval'](root);
	
	var parent = createBundle(rootProperties, null, "ROOT", bundle);
	
	if (lang !== null) {
		var langProperties = dojo['eval'](lang);
		parent = createBundle(langProperties, parent, langId, bundle);
	}
	
	if (langCountry !== null) {
		var langCountryProperties = dojo['eval'](langCountry);
		createBundle(langCountryProperties, parent, langCountryId, bundle);
	}
};

})();