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
		
	_scanForCircularDependencies: function(module, check) {
        check.push(module.id);
		for (var i = 0; i < module.dependencies.length; i++) {
			var moduleDependency = this.moduleMap.get(module.dependencies[i]);
            if (moduleDependency.scanned !== undefined) {
                continue;
            }
            var found = false;
            var dup;
            for (var j = 0; j < check.length; j++) {
                if (check[j] === moduleDependency.id) {
                    found = true;
                    dup = moduleDependency.id;
                    break;
                }
            }
            if (found) {
                var msg = "Circular dependency found : ";
                for (j = 0; j < check.length; j++) {
                    msg += check[j];
                    msg += "->";
                }
                print(msg+dup);
            } else {
                this._scanForCircularDependencies(moduleDependency, check);
            }
		}
        module.scanned = true;
        check.pop();
	},
	
	_analyze: function(modules, exclude) {
		this.dependencyStack = [];
		this.pluginRefList = {};
		this.missingNamesList = [];
		this.moduleMap = map.createMap();
		for (var i = 0; i < modules.length; i++) {
			astwalker.walker(modules[i], exclude, this.moduleMap, this.pluginRefList, this.missingNamesList, this.aliases, []);
		}
	},
	
	getDependencyList: function(modules, exclude) {
		this._analyze(modules, exclude);
		var dependencyList = [];
		for (i = 0; i < modules.length; i++) {
			var module = this.moduleMap.get(modules[i]);
			this._buildDependencyList(module, dependencyList, {});
			this._scanForCircularDependencies(module, []);
		}
		return dependencyList;
	},
	
	getAnalysisData: function(modules, exclude) {
		var dependencyList = this.getDependencyList(modules, exclude);
		return ({dependencyList: dependencyList, pluginRefs: this.pluginRefList, missingNamesList: this.missingNamesList});
	}
};

exports.createAnalyzer = function(aliases) {
	return new AMDAnalyzer(aliases);
};

exports.getMissingNameIndex = astwalker.getMissingNameIndex;
