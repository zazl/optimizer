/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
var dojo = dojo || {};
dojo.optimizer = dojo.optimizer || {};
dojo.optimizer.amd = {};

loadJS("/json/json2.js");
loadJS("/optimizer/module.js");
loadJS("/optimizer/map.js");
loadJS('/uglifyjs/bootstrap.js');
var astwalker = require('/optimizer/amd/astwalker');

dojo.optimizer.amd.AMDAnalyzer = function() {}

dojo.optimizer.amd.AMDAnalyzer.prototype = {
	_buildDependencyList: function(module, dependencyList, seen) {
		if (seen[module.id] === undefined) {
			seen[module.id] = module.id;
			for (var i = 0; i < module.dependencies.length; i++) {
				var moduleDependency = this.moduleMap.get(module.dependencies[i]);
				if (moduleDependency !== undefined) {
					this._buildDependencyList(moduleDependency, dependencyList, seen);
				} else {
					if (seen[module.dependencies[i]] === undefined) {
						dependencyList.push(module.dependencies[i]);
						seen[module.dependencies[i]] = module.dependencies[i];
					}
				}
			}
			dependencyList.push(module.uri+".js");
		}
	},
		
	_analyze: function(modules) {
		this.dependencyStack = [];
		this.localizationList = [];
		this.moduleMap = new dojo.optimizer.Map();
		for (var i = 0; i < modules.length; i++) {
			astwalker.walker(modules[i], this.moduleMap, this.localizationList);
		}
	},
	
	getDependencyList: function(modules, bypassAnalysis) {
		if (bypassAnalysis === undefined || bypassAnalysis === false) {
			this._analyze(modules);
		}
		var dependencyList = [];
		for (i = 0; i < modules.length; i++) {
			var module = this.moduleMap.get(modules[i]);
			this._buildDependencyList(module, dependencyList, {});
		}
		return dependencyList;
	},
	
	calculateChecksum: function(modules, bypassAnalysis) {
		var dependencyList = this.getDependencyList(modules, bypassAnalysis);
		dojo.require("dojox.encoding.digests.MD5");
		
		var js = "";
		
		for (var i = 0; i < dependencyList.length; i++) {
			js += readText(dependencyList[i]);
			
		}
		var ded = dojox.encoding.digests;
		return ded.MD5(js, ded.outputTypes.Hex);
	},
	
	getLocalizations: function(modules, bypassAnalysis) {
		if (bypassAnalysis === undefined || bypassAnalysis === false) {
			this._analyze(modules);
		}
		return this.localizationList;
	},
	
	getAnalysisData: function(modules, skipCheckSum) {
		var dependencyList = this.getDependencyList(modules);
		var checksum = null;
		if (skipCheckSum === undefined || skipCheckSum === false) {
			checksum = this.calculateChecksum(modules, true);
		}
		var localizations = this.getLocalizations(modules, true);
		return ({dependencyList: dependencyList, checksum: checksum, localizations: localizations});
	}
}
