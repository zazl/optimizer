/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
var http = require('http');
var url = require('url');
var path = require('path');
var fs = require('fs');
var qs = require('querystring');
var sandbox = require('zazlutil').sandbox;
var resourceloader = require('zazlutil').resourceloader;
var utils = require('zazlutil').utils;
var jsdom = require('jsdom');

resourceloader.addProvider(path.dirname(module.filename));

var bootstrapModules = ["dojo/dojo.js", "dojo/i18n.js"];
var debugBootstrapModules = ["dojo/dojo.js.uncompressed.js", "dojo/i18n.js"];

var NAMESPACE_PREFIX = "dojo.registerModulePath('";
var NAMESPACE_MIDDLE = "', '";
var NAMESPACE_SUFFIX = "');\n";

getAnalysisData = exports.getAnalysisData = function(modules) {
	var dojoSandbox = {
		document: jsdom.jsdom("<html><head></head><body>hello world</body></html>"),
		resourceloader: resourceloader,
		modules: modules,
		hostfile: "optimizer/syncloader/hostenv_optimizer.js"
	};
	var sb = sandbox.createSandbox(dojoSandbox);
	sb.loadJS("dojosandbox.js");
	sb.loadJS("optimizer/module.js");
	sb.loadJS("optimizer/map.js");
	sb.loadJS("optimizer/syncloader/analyzer.js");
	return sb.loadJS("analyzersandbox.js");
}

exports.handle = function(request, response) {
	requestURL = url.parse(request.url);
	if (requestURL.pathname.match("^/_javascript")) {
		var params = qs.parse(requestURL.query);
		var debug = params.debug === undefined ? false : true;
		var bootstrapModulePaths = debug ? debugBootstrapModules : bootstrapModules;
		var namespaces = [];
		if (params.namespace !== undefined) {
			var namespaceArray = params.namespace.split(',');
			for (var i = 0; i < namespaceArray.length; i++) {
				var bits = namespaceArray[i].split(':');
				namespaces.push({namespace: bits[0], prefix: bits[1]});
			}
		}
		if (params.modules !== undefined) {
			var modules = params.modules.split(',');
			var analysisData = getAnalysisData(modules);
			if (!debug) {
				var ifNoneMatch = request.headers["if-none-match"];
				if (ifNoneMatch !== undefined  && ifNoneMatch === analysisData.checksum) {
				    response.writeHead(304, {'Content-Type': 'text/javascript; charset=UTF-8'}); 
				    response.end();
				    return true;
				}
				response.setHeader('ETag', analysisData.checksum);
				if (params.version !== undefined && params.version === analysisData.checksum) {
					var expires = new Date();
					expires.setDate(expires.getDate()+365);
					response.setHeader('Expires', expires.toUTCString());
				}
			}
		}
		response.setHeader('Content-Type', 'text/javascript; charset=UTF-8');
		for (var i = 0; i < bootstrapModulePaths.length; i++) {
			response.write(resourceloader.readText(path.normalize(bootstrapModulePaths[i])));
		}
		
		for (i = 0; i < namespaces.length; i++) {
			response.write(NAMESPACE_PREFIX+namespaces[i].namespace+NAMESPACE_MIDDLE+namespaces[i].prefix+NAMESPACE_SUFFIX);
		}
		
		if (params.modules !== undefined) {
			writeLocalizations(response, analysisData.localizations, utils.getBestFitLocale(request.headers["accept-language"]));
			
			for (i = 0; i < analysisData.dependencyList.length; i++) {
				response.write(resourceloader.readText(path.normalize(analysisData.dependencyList[i]), true));
			}
		}
		response.end();
		return true;
	} else {
		return false;
	}
}

writeLocalizations = function(response, localizations, locale) {
	response.write(resourceloader.readText("optimizer/localization.js"));
	var intermediateLocale = null;
	if (locale.indexOf('-') !== -1) {
		intermediateLocale = locale.split('-')[0];
	}
	var lineSeparator = /\n/g;
	for (var i = 0; i < localizations.length; i++) {
		var rootModule = path.normalize(localizations[i].modpath+'/'+localizations[i].bundlename+".js");
		var fullModule = path.normalize(localizations[i].modpath+'/'+locale+'/'+localizations[i].bundlename+".js");
		if (intermediateLocale !== null) {
			var intermediateModule = path.normalize(localizations[i].modpath+'/'+intermediateLocale+'/'+localizations[i].bundlename+".js");
		}
		var langId = (intermediateLocale === null) ? null : "'"+intermediateLocale+"'";
		var root = resourceloader.readText(rootModule);
		if (root === null) {
			root = "null";
		} else {
			root = root.replace(lineSeparator, " ");
			root = "'"+root+"'";
		}
		var lang = (intermediateModule === null) ? null : resourceloader.readText(intermediateModule);
		if (lang === null) {
			lang = "null";
		} else {
			lang = lang.replace(lineSeparator, " ");
			lang = "'"+lang+"'";
		}
		var langCountry = resourceloader.readText(fullModule);
		if (langCountry === null) {
			langCountry = "null";
		} else {
			langCountry = langCountry.replace(lineSeparator, " ");
			langCountry = "'"+langCountry+"'";
		}
		response.write("dojo.optimizer.localization.load('"+localizations[i].bundlepackage+"', "+langId+", '"+locale+"', "+root+", "+lang+", "+langCountry+");\n");
	}
};
