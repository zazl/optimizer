/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
var crypto = require('crypto');
var path = require('path');
var resourceloader = require('zazlutil').resourceloader;

var cache = {};

var analyzer = require('optimizer/amd/AMDAnalyzer');

function toHash(value) {
	var md5Hash = crypto.createHash("md5");
	md5Hash.update(value);
	return md5Hash.digest('hex');
};

function getExcludes(excludeKeys) {
	var excludes = [];
	var seen = {};
	var analysisData;
	
	for (var i = 0; i < excludeKeys.length; i++) {
		analysisData = cache[excludeKeys[i]];
		for (var j = 0; j < analysisData.dependencyList.length; j++) {
			var dep = analysisData.dependencyList[j];
			if (!seen[dep]) {
				excludes.push(dep);
				seen[dep] = true;
			}
		}
	}
	return excludes;
};

function createChecksum(analysisData) {
	var js = "";
	
	for (i = 0; i < analysisData.dependencyList.length; i++) {
		js += resourceloader.readText(path.normalize(analysisData.dependencyList[i]+".js"));
	}
	return toHash(js);
};

function isStale(cacheEntry) {
	var stale = false;
	var analysisData = cacheEntry.analysisData;
	for (i = 0; i < analysisData.dependencyList.length; i++) {
        var dependencyPath = path.normalize(analysisData.dependencyList[i])+".js";
        var ts = resourceloader.getTimestamp(dependencyPath);
        if (ts !== -1 && ts !== cacheEntry.timestamps[analysisData.dependencyList[i]]) {
        	stale = true;
        	break;
        }
	}
	return stale;
};

function getTimestamps(dependencyList) {
	var timestamps = {};

	for (i = 0; i < dependencyList.length; i++) {
        var dependencyPath = path.normalize(dependencyList[i])+".js";
        timestamps[dependencyList[i]] = resourceloader.getTimestamp(dependencyPath);
	}

	return timestamps;
};

var getKey = function(modules, excludesKeys, config) {
	var excludes = getExcludes(excludesKeys);
	var key = "keyValues:";
	var i;
	
	for (i = 0; i < modules.length; i++) {
		key += modules[i];
	}
	
	key += "excludeValue:";
	
	for (i = 0; i < excludes.length; i++) {
		key += excludes[i];
	}
	
	if (config) {
		key += "configValue:";
		key += JSON.stringify(config);
	}
	return toHash(key);
};

exports.getKey = getKey;

exports.getAnalysisData = function(modules, excludes, config) {
	var key = getKey(modules, excludes, config);
	var doAnalysis = true;
	var cacheEntry = cache[key];
	
	if (cacheEntry) {
		doAnalysis = isStale(cacheEntry);
	}
	
	if (doAnalysis) {
		var analysisData = analyzer.createAnalyzer(config).getAnalysisData(modules, excludes);
		analysisData.checksum = createChecksum(analysisData);
		analysisData.key = key;
		var timestamps = getTimestamps(analysisData.dependencyList);
		cacheEntry = {analysisData: analysisData, timestamps: timestamps};
		cache[key] = cacheEntry;
	}
	return cacheEntry.analysisData;
};

exports.getAnalysisDataFromKey = function(key) {
	var cacheEntry = cache[key];
	return cacheEntry ? cacheEntry.analysisData : undefined;
};
