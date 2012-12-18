/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/

var map = require("./map");
var astwalker = require('./astwalker');
var resourceloader = require('zazlutil').resourceloader;

AMDAnalyzer = function(cfg) {
	this.config = {paths: {}, pkgs: {}, baseUrl: ""};
	if (cfg) {
		if (cfg.paths) {
			for (var p in cfg.paths) {
				var path = cfg.paths[p];
				this.config.paths[p] = path;
			}
		}
		if (cfg.packages) {
			for (i = 0; i < cfg.packages.length; i++) {
				var pkg = cfg.packages[i];
				if (!pkg.location) {
					pkg.location = pkg.name;
				}
				if (!pkg.main) {
					pkg.main = "main";
				}
				this.config.pkgs[pkg.name] = pkg;
			}
		}
		if (cfg.baseUrl) {
			this.config.baseUrl = cfg.baseUrl;
		}
		for (var p in cfg) {
			if (p !== "paths" && p !== "packages" && p !== 'baseUrl') {
				this.config[p] = cfg[p];
			}
		}
		this.config.scanCJSRequires = cfg.scanCJSRequires || false;
	}
};

AMDAnalyzer.prototype = {
	_buildDependencyList: function(module, dependencyList, seen) {
		if (seen[module.uri] === undefined) {
			seen[module.uri] = module.uri;
			for (var i = 0; i < module.dependencies.length; i++) {
				var moduleDependency = this.moduleMap.get(module.dependencies[i]);
				if (moduleDependency !== undefined) {
					this._buildDependencyList(moduleDependency, dependencyList, seen);
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
		this.pluginRefList = {};
		this.missingNamesList = [];
		this.shims = {};
		this.moduleMap = map.createMap();
		for (var i = 0; i < modules.length; i++) {
			astwalker.walker(modules[i], exclude, this.moduleMap, this.pluginRefList, this.missingNamesList, this.config, this.shims, []);
		}
	},
	
	getDependencyList: function(modules, exclude) {
		this._analyze(modules, exclude);
		var dependencyList = [];
		var seen = {require: "require", module: "module", exports: "exports"};
		for (var i = 0; i < modules.length; i++) {
			var m = modules[i];
			if (m.match(".+!")) {
				m = m.substring(0, m.indexOf('!'));
			}
			m = astwalker.expand(m, [], this.config);
			var module = this.moduleMap.get(m);
			this._buildDependencyList(module, dependencyList, seen);
			this._scanForCircularDependencies(module, []);
		}
		var allmodules = this.moduleMap.values();
		for (var i = 0; i < allmodules.length; i++) {
			var module = allmodules[i];
			if (module.defineFound === false && this.shims[module.uri] === undefined) {
				var shimContent = "\n(function(root, cfg) {\ndefine('";
				shimContent += module.id;
				shimContent += "', ";
				shimContent += "function() {\n";
				shimContent += "});\n}(this, zazl._getConfig()));\n";
				this.shims[module.uri] = shimContent;
			}
		}
		return dependencyList;
	},
	
	getAnalysisData: function(modules, exclude) {
		var dependencyList = this.getDependencyList(modules, exclude);
		return ({dependencyList: dependencyList, pluginRefs: this.pluginRefList, missingNamesList: this.missingNamesList, shims: this.shims});
	}
};

exports.createAnalyzer = function(config) {
	return new AMDAnalyzer(config);
};

exports.getMissingNameIndex = astwalker.getMissingNameIndex;
