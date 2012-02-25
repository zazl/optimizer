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
	var cjsVarPrefixRegExp = /^~#/;
	var pluginRegExp = /.+!/;
    
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
	var cblist = {};
	var injectQueue = [];
	var injectInProcess = false;

	var opts = Object.prototype.toString;
	
    function isFunction(it) { return opts.call(it) === "[object Function]"; };
    function isArray(it) { return opts.call(it) === "[object Array]"; };
    function isString(it) { return (typeof it == "string" || it instanceof String); };
    
    function _getCurrentId() {
    	return moduleStack.length > 0 ? moduleStack[moduleStack.length-1] : "";
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
            if ((pkg = pkgs[_getCurrentId()])) {
                path = pkg.name + "/" + path;
            } else {
                path = _getCurrentId() + "/../" + path;
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
		var dependentId = _getCurrentId();
		for (var i = 0; i < moduleStack.length; i++) {
			if (moduleStack[i] === expandedId) {
				cb(modules[expandedId].exports);
				return;
			}
		}
		if (cblist[expandedId] === undefined) {
			cblist[expandedId] = [];
		}
		if (modules[expandedId] && modules[expandedId].cjsreq) {
			cblist[expandedId].push({cb:cb, mid:dependentId});
			return;
		}
    	function _load() {
    		moduleStack.push(expandedId);
    		_loadModuleDependencies(expandedId, function(exports){
    			moduleStack.pop();
				cblist[expandedId].push({cb:cb, mid:dependentId});
            });
    	};
		
		if (modules[expandedId] === undefined) {
			injectQueue.push({id:expandedId, cb: _load});
		} else {
			_load();
		}
	};
	
	function _inject(moduleId, cb) {
		var locale = "en-us";
		if (window.dojoConfig && window.dojoConfig.locale) {
			locale = dojoConfig.locale;
		}
		var configString = JSON.stringify(cfg);
		var url = cfg.injectUrl+"?modules="+moduleId+"&writeBootstrap=false&locale="+locale+"&config="+configString+"&exclude=";
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
		var m = modules[id];
		m.args = [];
		m.deploaded = {};
		var idx = 0;
		var iterate = function(itr) {
			if (itr.hasMore()) {
				var dependency = itr.next();
				var argIdx = idx++;
				var depname;
				if (dependency.match(pluginRegExp)) {
					var add = true;
					if (dependency.match(cjsVarPrefixRegExp)) {
						dependency = dependency.substring(2);
						add = false;
					}
					var pluginName = dependency.substring(0, dependency.indexOf('!'));
					pluginName = _expand(pluginName);
					var pluginModuleName = dependency.substring(dependency.indexOf('!')+1);
					if (add) {
						m.dependencies[argIdx] = pluginName + "!"+pluginModuleName;
						m.args[argIdx] = undefined;
						depname = pluginName + "!"+pluginModuleName;
					} else {
						depname = "~#"+pluginName + "!"+pluginModuleName;
					}
					m.deploaded[depname] = false;
					_loadPlugin(pluginName, pluginModuleName, function(pluginInstance) {
						if (add) {
							m.args[argIdx] = pluginInstance;
						}
						m.deploaded[depname] = true;
					});
					iterate(itr);
				} else if (dependency === 'require') {
					m.args[argIdx] = _createRequire(_getCurrentId());
					m.deploaded['require'] = true;
					iterate(itr);
				} else if (dependency === 'module') {
					m.args[argIdx] = m;
					m.deploaded['module'] = true;
					iterate(itr);
				} else if (dependency === 'exports') {
					m.args[argIdx] = m.exports;
					m.deploaded['exports'] = true;
					iterate(itr);
				} else {
					var add = true;
					if (dependency.match(cjsVarPrefixRegExp)) {
						dependency = dependency.substring(2);
						add = false;
					}
					var expandedId = _expand(dependency);
					if (add) {
						m.dependencies[argIdx] = expandedId;
						m.args[argIdx] = modules[expandedId] === undefined ? undefined : modules[expandedId].exports;
						depname = expandedId;
					} else {
						depname = "~#"+expandedId;
					}
					m.deploaded[depname] = false;
					_loadModule(dependency, function(module){
						if (add) {
							m.args[argIdx] = module;
						}
						m.deploaded[depname] = true;
					});
					iterate(itr);
				}
			} else {
				m.cjsreq = _createRequire(_getCurrentId());
				cb();
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
				if (pluginInstance === undefined) {
					pluginInstance = null;
				}
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
			moduleStack = [id];
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
		modules[id] = {id: id, exports: {}};

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
        plugins: true,
        jQuery: true
    };

	_require = function (dependencies, callback) {
		if (isString(dependencies)) {
			var id = dependencies;
			id = _expand(id);
			if (id.match(pluginRegExp)) {
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
			if (modules[id] === undefined) {
				throw new Error("Module ["+id+"] has not been loaded");
			}
			return modules[id].exports;
		} else if (isArray(dependencies)) {
			var args = [];
			var iterate = function(itr) {
				if (itr.hasMore()) {
					var dependency = itr.next();
					var id = _expand(dependency);
					if (dependency.match(pluginRegExp)) {
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
	modules["require"].loaded = true;
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

		for (var id in cache) {
			var resolvedId = _idToUrl(id);
			var cacheValue = cache[id];
			delete cache[id];
			cache[resolvedId] = cacheValue;
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
		var complete = queueProcessor();
		if (!complete) {
			var poller = function() {
				complete = queueProcessor();
				if (complete) { return; }
				setTimeout(poller, 0);
			};
			poller();
		}
	};
	
	amdlite.addToCache = function(id, value) {
		cache[id] = value;
	};
	
	amdlite.addAnalysisKey = function(key) {
		analysisKeys.push(key);
	};
	
	var pageLoaded = false;
	var modulesLoaded = false;
	var domLoaded = false;
	var readyCallbacks = [];
    
	document.addEventListener("DOMContentLoaded", function() {
		domLoaded = true;
	}, false);
	
	if (!require) {
		require = _require;
		require.toUrl = function(moduleResource) {
			var url = _idToUrl(_expand(moduleResource)); 
			return url;
		};
	}

	function isComplete(module) {
		var complete = false;
		if (module.cjsreq) {
			complete = true;
			for (var dep in module.deploaded) {
				if (module.deploaded[dep] === false) {
					complete = false;
					break;
				}
			}
		}
		return complete;
	};

	function processInjectQueue() {
		if (!injectInProcess && injectQueue.length > 0) {
			injectInProcess = true;
			var injection = injectQueue.shift();
			_inject(injection.id, function() {
				injection.cb();
				injectInProcess = false;
			});
		}
	}

	function queueProcessor() {
		var allLoaded = true, timeout = 100, mid, m, ret;

		try {
			for (mid in modules) {
				if (mid === "require" || mid.match(pluginRegExp)) {
					continue;
				}
				m = modules[mid];
				if (!m || m.loaded !== true) {
					allLoaded = false;
				}
				if (m.loaded !== true && isComplete(m)) {
					if (m.factory !== undefined) {
						if (m.args.length < 1) {
							m.args = m.args.concat(m.cjsreq, m.exports, m);
						}
						ret = m.factory.apply(null, m.args);
						if (ret) {
							m.exports = ret;
						}
					} else {
						m.exports = m.literal;
					}
					m.loaded = true;
				}
			}

			if (allLoaded) {
				modulesLoaded = true;
			} else {
				timeout = 0;
			}

			var savedStack;

			var cbiterate = function(exports, itr) {
				if (itr.hasMore()) {
					var cbinst = itr.next();
					if (cbinst.mid !== "") {
						var root = modules[cbinst.mid];
						savedStack = moduleStack;
						moduleStack = [cbinst.mid];
					}
					cbinst.cb(exports);
					if (cbinst.mid !== "") {
						moduleStack = savedStack;
					}
					cbiterate(exports, itr);
				} else {
					delete cblist[mid];
				}
			};
			for (mid in cblist) {
				if (modules[mid] && modules[mid].loaded) {
					cbiterate(modules[mid].exports, new Iterator(cblist[mid]));
				}
			}
			if (!pageLoaded && domLoaded && modulesLoaded) {
				pageLoaded = true;
				for (var i = 0; i < readyCallbacks.length; i++) {
					readyCallbacks[i]();
				}
			}
			processInjectQueue();
		} catch (e) {
			console.log("queueProcessor error : "+e);
		}
		return allLoaded;
	};
}());
