/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
var path = require('path');
var analyzer = require('../optimizer/amd/AMDAnalyzer');
var crypto = require('crypto');
var resourceloader = require('zazlutil').resourceloader;
var utils = require('zazlutil').utils;

Handler = function(config) {
	this.config = config;
};

Handler.prototype = {
	handle: function(params, analysisData, request, response) {
		var i;
		if (this.config.implicitDependencies !== undefined) {
			for (i = 0; i < this.config.implicitDependencies.length; i++) {
				var src = resourceloader.readText(path.normalize(this.config.implicitDependencies[i].uri));
				if (src === null) {
					throw new Error("Unable to load src for ["+this.config.implicitDependencies[i].uri+"]");
				}
				var missingNameIndex = analyzer.getMissingNameIndex(src);
				if (missingNameIndex !== -1) {
					var modifiedSrc = src.substring(0, missingNameIndex);
					modifiedSrc += "'"+this.config.implicitDependencies[i].id+"', ";
					modifiedSrc += src.substring(missingNameIndex);
	                src = modifiedSrc;
				}
				response.write(src);
			}
		}
		if (this.config.suffixCode !== undefined) {
			response.write(this.config.suffixCode);
		}
		
		if (params.modules !== undefined) {
			this.writeLocalizations(response, analysisData.localizations, utils.getBestFitLocale(request.headers["accept-language"]));
			for (i = 0; i < analysisData.textList.length; i++) {
				var textPath = path.normalize(analysisData.textList[i]);
				var textContent = resourceloader.readText(textPath, false);
				response.write("define('text!");
				response.write(textPath);
				response.write("', function () { return ");
				response.write(this.escapeString(textContent));
				response.write(";});");
				response.write("\n");
			}
			for (i = 0; i < analysisData.dependencyList.length; i++) {
	            var dependencyPath = path.normalize(analysisData.dependencyList[i])+".js";
	            var content = resourceloader.readText(dependencyPath, false);
	            var missingNameIndex = this.lookForMissingName(analysisData.dependencyList[i], analysisData.missingNamesList);
	            
	            if (missingNameIndex !== -1) {
	                var modifiedContent = content.substring(0, missingNameIndex);
	                var dependency = analysisData.dependencyList[i];
	                for (var alias in this.config.aliases) {
	                	if (this.config.aliases[alias] === dependency) {
	                		dependency = alias;
	                		break;
	                	}
	                }
	                modifiedContent += "'"+dependency+"', ";
	                modifiedContent += content.substring(missingNameIndex);
	                content = modifiedContent;
	            }
	            response.write(content);
			}
		}
	},
	
	getAnalysisData: function(modules) {
		analysisData = analyzer.createAnalyzer(this.config.aliases).getAnalysisData(modules);
		var js = "";
		
		for (i = 0; i < analysisData.dependencyList.length; i++) {
			js += resourceloader.readText(path.normalize(analysisData.dependencyList[i]));
		}
		var md5Hash = crypto.createHash("md5");
		md5Hash.update(js);
		analysisData.checksum = md5Hash.digest('hex');
		return analysisData;
	},

	writeLocalizations: function(response, localizations, locale) {
		var intermediateLocale = null;
		if (locale.indexOf('-') !== -1) {
			intermediateLocale = locale.split('-')[0];
		}
		for (var i = 0; i < localizations.length; i++) {
			var rootModule = path.normalize(localizations[i].bundlepackage);
			var fullModule = path.normalize(localizations[i].modpath+'/'+locale+'/'+localizations[i].bundlename);
			if (intermediateLocale !== null) {
				var intermediateModule = path.normalize(localizations[i].modpath+'/'+intermediateLocale+'/'+localizations[i].bundlename);
			}
			var root = resourceloader.readText(rootModule+".js");
			if (root !== null) {
				this.writeLocalization(response, root, rootModule);
			}
			var lang = (intermediateModule === null) ? null : resourceloader.readText(intermediateModule+".js");
			if (lang !== null) {
				this.writeLocalization(response, lang, intermediateModule);
			}
			var langCountry = resourceloader.readText(fullModule+".js");
			if (langCountry !== null) {
				this.writeLocalization(response, langCountry, fullModule);
			}
		}
	},
	
	writeLocalization: function(response, content, moduleName) {
		response.write(content.substring(0, content.indexOf('(')+1));
		response.write("'");
		response.write(moduleName);
		response.write("',");
		response.write(content.substring(content.indexOf('(')+1));
		response.write("\n");
	}, 
	
	lookForMissingName: function(uri, missingNamesList) {
	    var missingNameIndex = -1;
	    for (var i = 0; i < missingNamesList.length; i++) {
	        if (uri === missingNamesList[i].uri) {
	            missingNameIndex = missingNamesList[i].nameIndex;
	            break;
	        }
	    }
	    return missingNameIndex;
	},
	
	escapeString: function (str) {
		return ("\"" + str.replace(/(["\\])/g, "\\$1") + "\"").replace(/[\f]/g, "\\f").replace(/[\b]/g, "\\b").replace(/[\n]/g, "\\n").replace(/[\t]/g, "\\t").replace(/[\r]/g, "\\r");
	}	
};

exports.createHandler = function(config) {
	return new Handler(config);
};


