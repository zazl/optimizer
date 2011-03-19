/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
var url = require('url');
var path = require('path');
var qs = require('querystring');
var resourceloader = require('zazlutil').resourceloader;

resourceloader.addProvider(path.dirname(module.filename));

var configFile = process.argv.length > 4 ? process.argv[4] : "syncloader.json";
var config = JSON.parse(resourceloader.readText(configFile));
var handlerModule = config.type === "amd" ? "./amd/handler" : "./syncloader/handler";
var handler = require(handlerModule).createHandler(config);

exports.getAnalysisData = function(modules) {
	return handler.getAnalysisData(modules);
};

exports.handle = function(request, response) {
	requestURL = url.parse(request.url);
	if (requestURL.pathname.match("^/_javascript")) {
		var params = qs.parse(requestURL.query);
		var debug = params.debug === undefined ? false : true;
		var bootstrapModulePaths = debug ? config.debugBootstrapModules : config.bootstrapModules;
		if (params.modules !== undefined) {
			var modules = params.modules.split(',');
			var analysisData = handler.getAnalysisData(modules);
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
		handler.handle(params, analysisData, request, response);
		response.end();
		return true;
	} else {
		return false;
	}
};
