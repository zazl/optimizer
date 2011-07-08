/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/

var require; 
var define;
var config;

(function () {
	var pageLoaded = false;
	var readyCallbacks = [];
	var moduleRegistry = {};
	
	var opts = Object.prototype.toString;
	
    function isFunction(it) { return opts.call(it) === "[object Function]"; };
    function isArray(it) { return it && (it instanceof Array || typeof it == "array"); };
    function isString(it) { return (typeof it == "string" || it instanceof String); };
    
	define = function (id, dependencies, factory) {
		var prefix;
		var plugin;
		if (!isString(id)) { throw new Error("A string id must be the first parameter of the define statement"); }
		
		if (moduleRegistry[id] !== undefined) { throw new Error("A module with an id of ["+id+"] has already be provided");}
		
		if (isArray(dependencies)) {
			for (var i = 0; i < dependencies.length; i++) {
				var dependency = dependencies[i];
				if (dependency.indexOf('!') !== -1) {
					prefix = dependency.substring(0, dependency.indexOf('!'));
					if (moduleRegistry[prefix] === undefined) {
						moduleRegistry[prefix] = {};
						moduleRegistry[prefix].dependencies = [];
						plugin = _loadModule(prefix, []);
					} else {
						plugin = moduleRegistry[prefix].module;
					}
					plugin.load(dependency.substring(dependency.indexOf('!')+1), require, function(module) {
						moduleRegistry[dependency] = {};
						moduleRegistry[dependency].module = module;
					}, config);
				} else if (moduleRegistry[dependency] === undefined) {
					throw new Error("Dependency ["+dependency+"] has not been yet been loaded");
				}
			}
			moduleRegistry[id] = {};
			if (factory === undefined) { throw new Error("No factory has been provided for ["+id+"]"); }
			if (!isFunction(factory)) { throw new Error("Factory parameter is not a function for ["+id+"]"); }
			moduleRegistry[id].factory = factory;
			moduleRegistry[id].dependencies = dependencies;
		} else if (isFunction(dependencies)) {
			moduleRegistry[id] = {};
			moduleRegistry[id].factory = dependencies; 
			moduleRegistry[id].dependencies = [];
		} else {
			moduleRegistry[id] = {};
			moduleRegistry[id].dependencies = [];
			moduleRegistry[id].module = dependencies;
		}
	};
	
    define.amd = {
    	plugins: true
    };

    if (typeof require !== undefined) {
    	config = require;
    	if (config.ready !== undefined && isFunction(config.ready)) {
    		readyCallbacks.push(config.ready);
    	}
    }
    
	require = function (dependencies, callback) {
		var args = [];
		if (isString(dependencies)) {
			return _loadModule(dependencies, []);
		} else if (isArray(dependencies)) {
			args = _loadModules(dependencies, []);
			if (callback !== undefined) {
				callback.apply(null, args);
			}
			return undefined;
		}
	};
	
	require.ready = function(callback) {
		if (pageLoaded) {
			callback();
		} else {
			readyCallbacks.push(callback);
		}
	};
	
	require.nameToUrl = function(moduleName, ext, relModuleMap) {
		return moduleName + ext;
    };
	
	moduleRegistry["require"] = {};
	moduleRegistry["require"].module = require;

	_loadModules = function (ids, pathStack) {
		var args = [];
		for (var i = 0; i < ids.length; i++) {
			args.push(_loadModule(ids[i], pathStack));
		}
		return args;
	};
	
	_loadModule = function(id, pathStack) {
		id = _expand(id, pathStack);
		if (moduleRegistry[id] !== undefined) {
			if (moduleRegistry[id].module === undefined) {
				pathStack.push(id);
				var dependencyArgs = _loadModules(moduleRegistry[id].dependencies);
				pathStack.pop();
				moduleRegistry[id].module = moduleRegistry[id].factory.apply(null, dependencyArgs);
			}
			return moduleRegistry[id].module;
		} else {
			throw new Error("Unable to locate dependency ["+id+"]");
		}
	};
	
	_normalize = function(path) {
		var segments = path.split('/');
		var skip = 0;

		for (var i = segments.length; i >= 0; i--) {
			var segment = segments[i];
			if (segment === '.') {
				segments.splice(i, 1);
			} else if (segment === '..') {
				segments.splice(i, 1);
				skip++;
			} else if (skip) {
				segments.splice(i, 1);
				skip--;
			}
		}
		return segments.join('/');
	};

	_expand = function(uri, pathStack) {
		var isRelative = uri.search(/^\./) === -1 ? false : true;
		if (isRelative) {
			var parentPath = pathStack.length > 0 ? pathStack[pathStack.length-1] : "";
			parentPath = parentPath.substring(0, parentPath.lastIndexOf('/')+1);
			uri = parentPath + uri;
			uri = _normalize(uri);
		}
		return uri;
	};
	
	document.addEventListener("DOMContentLoaded", function() {
		pageLoaded = true;
		for (var i = 0; i < readyCallbacks.length; i++) {
			readyCallbacks[i]();
		}
	}, false);
	
}());
