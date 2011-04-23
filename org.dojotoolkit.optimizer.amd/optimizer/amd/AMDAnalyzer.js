/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/

var map = require("./map");
var astwalker = require('./astwalker');

AMDAnalyzer = function(aliases) {
	if (aliases === undefined) {
		this.aliases = {};
	} else {
		this.aliases = aliases;
	}
};

AMDAnalyzer.prototype = {
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
			dependencyList.push(module.uri);
		}
	},
		
	_analyze: function(modules, exclude) {
		this.dependencyStack = [];
		this.localizationList = [];
		this.textList = [];
		this.missingNamesList = [];
		this.moduleMap = map.createMap();
		for (var i = 0; i < modules.length; i++) {
			astwalker.walker(modules[i], exclude, this.moduleMap, this.localizationList, this.textList, this.missingNamesList, this.aliases, []);
		}
	},
	
	getDependencyList: function(modules, exclude) {
		this._analyze(modules, exclude);
		var dependencyList = [];
		for (i = 0; i < modules.length; i++) {
			var module = this.moduleMap.get(modules[i]);
			this._buildDependencyList(module, dependencyList, {});
		}
		return dependencyList;
	},
	
	getAnalysisData: function(modules, exclude) {
		var dependencyList = this.getDependencyList(modules, exclude);
		return ({dependencyList: dependencyList, localizations: this.localizationList, textList: this.textList, missingNamesList: this.missingNamesList});
	}
};

exports.createAnalyzer = function(aliases) {
	return new AMDAnalyzer(aliases);
};

exports.getMissingNameIndex = astwalker.getMissingNameIndex;
