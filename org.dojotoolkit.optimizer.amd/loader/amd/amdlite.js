/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/

var require;
var define;

(function () {
	/* These regexs are taken from requirejs */
    var commentRegExp = /(\/\*([\s\S]*?)\*\/|\/\/(.*)$)/mg;
	/* Based on the cjs regexs in requirejs, modified slightly */
    var cjsRequireRegExp = /[^\d\w\.]require\(["']([^'"\s]+)["']\)/g;
    
	Iterator = function(array) {
		this.array = array;
		this.current = 0;
	};

	Iterator.prototype = {
		hasMore: function() {
			return this.current < this.array.length;
		},
		next: function() {
			return this.array[this.current++];
		}
	};

	var modules = {};
	var moduleStack = [];
	var paths = {};
	var pkgs = {};
	var cache = {};
	var analysisKeys = [];

	var opts = Object.prototype.toString;
	
    function isFunction(it) { return opts.call(it) === "[object Function]"; };
    function isArray(it) { return opts.call(it) === "[object Array]"; };
    function isString(it) { return (typeof it == "string" || it instanceof String); };
    
    function _getParentId() {
    	return moduleStack.length > 0 ? moduleStack[moduleStack.length-1].id : "";
    }
    
	function _normalize(path) {
		var segments = path.split('/');
		var skip = 0;

		for (var i = (segments.length-1); i >= 0; i--) {
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
	
	function _expand(path) {
		var isRelative = path.search(/^\./) === -1 ? false : true;
		if (isRelative) {
            var pkg;
            if ((pkg = pkgs[_getParentId()])) {
                path = pkg.name + "/" + path;
            } else {
                path = _getParentId() + "/../" + path;
            }
			path = _normalize(path);
		}
		for (pkgName in pkgs) {
		    if (path === pkgName) {
		    	return pkgs[pkgName].name + '/' + pkgs[pkgName].main;
		    }
		}
		return path;
	};
	
	function _idToUrl(path) {
		var segments = path.split("/");
		for (var i = segments.length; i >= 0; i--) {
			var pkg;
            var parent = segments.slice(0, i).join("/");
            if (paths[parent]) {
            	segments.splice(0, i, paths[parent]);
                break;
            }else if ((pkg = pkgs[parent])) {
            	var pkgPath;
                if (path === pkg.name) {
                    pkgPath = pkg.location + '/' + pkg.main;
                } else {
                    pkgPath = pkg.location;
                }
    			segments.splice(0, i, pkgPath);
    			break;
            }
		}
		path = segments.join("/");
        if (path.charAt(0) !== '/') {
        	path = cfg.baseUrl + path; 
        }
		path = _normalize(path);
		return path;
	};
	
	function _loadModule(id, cb) {
		var expandedId = _expand(id);
		if (modules[expandedId] && modules[expandedId].exports) {
			cb(modules[expandedId].exports);
			return;
		}
    	function _load() {
    		moduleStack.push(modules[expandedId]);
    		_loadModuleDependencies(expandedId, function(exports){
    			moduleStack.pop();
                cb(exports);
            });
    	};
		
		if (modules[expandedId] === undefined) {
			_inject(expandedId, function(){
				_load();
			});
		} else {
			_load();
		}
	};
	
	function _inject(moduleId, cb) {
		var locale = dojoConfig ? dojoConfig.locale : "en-us";
		var url = cfg.injectUrl+"?modules="+moduleId+"&writeBootstrap=false&locale="+locale+"&exclude=";
		for (var i = 0; i < analysisKeys.length; i++) {
			url += analysisKeys[i];
			url += i < (analysisKeys.length - 1) ? "," : "";
		}
		var script = document.createElement('script');
		script.type = "text/javascript";
		script.src = url;
		script.charset = "utf-8";
		script.onloadDone = false;
		script.onload = function() {
			if (!script.onloadDone) {
				script.onloadDone = true;
				cb();
			}
		};
		script.onreadystatechange = function(){
			if (("loaded" === script.readyState || "complete" === script.readyState) && !script.onloadDone) {
				script.onload();
			}
		};
		document.getElementsByTagName("head")[0].appendChild(script);
	};
	
	function _loadModuleDependencies(id, cb) {
		var args = [];
		var m = modules[id];
		m.exports = {};
		var iterate = function(itr) {
			if (itr.hasMore()) {
				var dependency = itr.next();
				if (dependency.match(".+!")) {
					var add = true;
					if (dependency.match("^~#")) {
						dependency = dependency.substring(2);
						add = false;
					}
					var pluginName = dependency.substring(0, dependency.indexOf('!'));
					pluginName = _expand(pluginName);
					var pluginModuleName = dependency.substring(dependency.indexOf('!')+1);
					_loadPlugin(pluginName, pluginModuleName, function(pluginInstance) {
						if (add) {
							args.push(pluginInstance);
						}
						iterate(itr);
					});
				} else if (dependency === 'require') {
					args.push(_createRequire(_getParentId()));
					iterate(itr);
				} else if (dependency === 'module') {
					args.push(m);
					iterate(itr);
				} else if (dependency === 'exports') {
					args.push(m.exports);
					iterate(itr);
				} else {
					var add = true;
					if (dependency.match("^~#")) {
						dependency = dependency.substring(2);
						add = false;
					}
					_loadModule(dependency, function(module){
						if (add) {
							args.push(module);
						}
						iterate(itr);
					});
				}
			} else {
				if (m.factory !== undefined) {
					if (args.length < 1) {
						var req = _createRequire(_getParentId());
						args = args.concat(req, m.exports, m);
					}
					var ret = m.factory.apply(null, args);
					if (ret) {
						m.exports = ret;
					}
				} else {
					m.exports = m.literal;
				}
				cb(m.exports);
			}
		};
		iterate(new Iterator(m.dependencies));
	};
	
	function _loadPlugin(pluginName, pluginModuleName, cb) {
		_loadModule(pluginName, function(plugin){
			if (plugin.normalize) {
				pluginModuleName = plugin.normalize(pluginModuleName, _expand); 
			} else {
				pluginModuleName = _expand(pluginModuleName);
			}
			var isDynamic = plugin.dynamic || false; 
			if (modules[pluginName+"!"+pluginModuleName] !== undefined && !isDynamic) {
				cb(modules[pluginName+"!"+pluginModuleName].exports);
				return;
			}
			var req = _createRequire(pluginName);
			var load = function(pluginInstance){
		    	modules[pluginName+"!"+pluginModuleName] = {};
		    	modules[pluginName+"!"+pluginModuleName].exports = pluginInstance;
				cb(pluginInstance);
			};
			load.fromText = function(name, text) {
				_loadModule(name, function(){}, text);				
			};
			plugin.load(pluginModuleName, req, load, cfg);
		});
	};
	
	function _createRequire(id) {
		var req = function(dependencies, callback) {
			var root = modules[id];
			var savedStack = moduleStack;
			moduleStack = [root];
			if (isFunction(callback)) {
				_require(dependencies, function() {
					moduleStack = savedStack;
					callback.apply(null, arguments);
				});
			} else {
				var mod = _require(dependencies, callback);
				moduleStack = savedStack;
				return mod;
			}
		};
		req.toUrl = function(moduleResource) {
			var url = _idToUrl(_expand(moduleResource)); 
			return url;
		};
		req.defined = function(moduleName) {
			return _expand(moduleName) in modules;
		};
		req.specified = function(moduleName) {
			return _expand(moduleName) in modules;
		};
		req.ready = function(callback) {
			if (pageLoaded) {
				callback();
			} else {
				readyCallbacks.push(callback);
			}
		};
		req.nameToUrl = function(moduleName, ext, relModuleMap) {
			return moduleName + ext;
	    };
        // Dojo specific require properties and functions
        req.cache = cache;
        req.toAbsMid = function(id) {
        	return _expand(id);
        };
        req.isXdUrl = function(url) {
        	return false;
        };
		return req;
	};
	
	define = function (id, dependencies, factory) {
		if (!isString(id)) { 
			throw new Error("A string id must be the first parameter of the define statement"); 
		}
		
		if (modules[id] !== undefined) { 
			throw new Error("A module with an id of ["+id+"] has already been provided");
		}
		
		modules[id] = {};
		modules[id].id = id;

		if (!isArray(dependencies)) {
			factory = dependencies;
			dependencies = [];
		}
		if (isFunction(factory)) {
			factory.toString().replace(commentRegExp, "").replace(cjsRequireRegExp, function (match, dep) {
				dependencies.push("~#"+dep);
            });
			modules[id].factory = factory;
		} else {
			modules[id].literal = factory;
		}
		modules[id].dependencies = dependencies; 
	};
	
    define.amd = {
        plugins: true
    };

	_require = function (dependencies, callback) {
		if (isString(dependencies)) {
			var id = dependencies;
			id = _expand(id);
			if (id.match(".+!")) {
				var pluginName = id.substring(0, id.indexOf('!'));
				pluginName = _expand(pluginName);
				var plugin = modules[pluginName].exports;
				var pluginModuleName = id.substring(id.indexOf('!')+1);
				if (plugin.normalize) {
					pluginModuleName = plugin.normalize(pluginModuleName, function(path){
						return _expand(path);
					});
				} else {
					pluginModuleName = _expand(pluginModuleName);
				}
				id = pluginName+"!"+pluginModuleName;
			}
			return modules[id] === undefined ? undefined : modules[id].exports;
		} else if (isArray(dependencies)) {
			var args = [];
			var iterate = function(itr) {
				if (itr.hasMore()) {
					var dependency = itr.next();
					if (dependency.match(".+!")) {
						var pluginName = dependency.substring(0, dependency.indexOf('!'));
						pluginName = _expand(pluginName);
						var pluginModuleName = dependency.substring(dependency.indexOf('!')+1);
						_loadPlugin(pluginName, pluginModuleName, function(pluginInstance) {
							args.push(pluginInstance);
							iterate(itr);
						});
					} else {
						_loadModule(dependency, function(module){
							args.push(module);
							iterate(itr);
						});
					}
				} else if (callback !== undefined) {
					callback.apply(null, args);
				}
			};
			iterate(new Iterator(dependencies));
			return undefined;
		}
	};
	
	modules["require"] = {};
	modules["require"].exports = _require;
	var cfg = {
		baseUrl: _normalize(window.location.pathname.substring(0, window.location.pathname.lastIndexOf('/')) + "/./"),
		injectUrl: "_javascript"
	};

	amdlite = function(config, dependencies, callback) {
		if (!isArray(config) && typeof config == "object") {
			var i;
			cfg = config;
			if (cfg.paths) {
				for (var p in cfg.paths) {
					var path = cfg.paths[p];
					paths[p] = path;
				}
			}
			if (cfg.packages) {
				for (i = 0; i < cfg.packages.length; i++) {
					var pkg = cfg.packages[i];
					pkgs[pkg.name] = pkg;
				}
			}
			cfg.baseUrl = cfg.baseUrl || "./";
			cfg.injectUrl = cfg.injectUrl || "_javascript";
		} else {	
			callback = dependencies;
			dependencies = config;
		}
		if (cfg.baseUrl.charAt(0) !== '/') {
			cfg.baseUrl = _normalize(window.location.pathname.substring(0, window.location.pathname.lastIndexOf('/')) + '/'+ cfg.baseUrl);
		}

		if (!isArray(dependencies)) {
			callback = dependencies;
			dependencies = [];
		}
		if (isFunction(callback)) {
			_require(dependencies, function() {
				callback.apply(null, arguments);
			});
		} else {
			_require(dependencies);
		}
	};
	
	amdlite.addToCache = function(id, value) {
		cache[_idToUrl(id)] = value;
	};
	
	amdlite.addAnalysisKey = function(key) {
		analysisKeys.push(key);
	};
	
	var pageLoaded = false;
	var readyCallbacks = [];
    
	document.addEventListener("DOMContentLoaded", function() {
		pageLoaded = true;
		for (var i = 0; i < readyCallbacks.length; i++) {
			readyCallbacks[i]();
		}
	}, false);
	
	if (!require) {
		require = _require;
	}
}());
