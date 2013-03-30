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
    
	var Iterator = function(array) {
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
	var precache = {};
	var cache = {};
	var analysisKeys = [];
	var cblist = {};
	var injectQueue = [];
	var injectInProcess = false;
	var requireInProcess = false;
	var cfg;
	var pageLoaded = false;
	var modulesLoaded = false;
	var domLoaded = false;
	var readyCallbacks = [];
	var reqQueue = [];
	var warmupState = 0;
	
	var opts = Object.prototype.toString;
	
	var geval = window.execScript || eval;
	
	var scripts = document.getElementsByTagName('script');
	var zazlpath = scripts[scripts.length-1].src.split('?')[0];
	zazlpath = zazlpath.split('/').slice(0, -1).join('/')+'/';	

    function isFunction(it) { return opts.call(it) === "[object Function]"; }
    function isArray(it) { return opts.call(it) === "[object Array]"; }
    function isString(it) { return (typeof it === "string" || it instanceof String); }
    
    function _getCurrentId() {
		return moduleStack.length > 0 ? moduleStack[moduleStack.length-1] : "";
    }
    
	function countSegments(path) {
		var count = 0;
		for (var i = 0; i < path.length; i++) {
			if (path.charAt(i) === '/') {
				count++;
			}
		}
		return count;
	}

	function findMapping(path, depId) {
		var mapping;
		var segmentCount = -1;
		for (var id in cfg.map) {
			if (depId.indexOf(id) === 0) {
				var foundSegmentCount = countSegments(id);
				if (foundSegmentCount > segmentCount) {
					var mapEntry = cfg.map[id];
					if (mapEntry[path] !== undefined) {
						mapping = mapEntry[path];
						segmentCount = foundSegmentCount;
					}
				}
			}
		}
		if (mapping === undefined && cfg.map["*"] !== undefined && cfg.map["*"][path] !== undefined) {
			mapping = cfg.map["*"][path];
		}
		return mapping;
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
	}
	
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
		var pkgName;
		for (pkgName in pkgs) {
		    if (path === pkgName) {
				return pkgs[pkgName].name + '/' + pkgs[pkgName].main;
		    }
		}

		var segments = path.split("/");
		for (var i = segments.length; i >= 0; i--) {
            var parent = segments.slice(0, i).join("/");
			var mapping = findMapping(parent, _getCurrentId());
			if (mapping) {
				segments.splice(0, i, mapping);
				return segments.join("/");
			}
		}

		return path;
	}
	
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
	}
	
	function _loadModuleDependencies(id, cb) {
		var m = modules[id];
		var idx = 0;
		var iterate = function(itr) {
			if (itr.hasMore()) {
				var dependency = itr.next();
				var argIdx = idx++;
				var depname;
				var add;
				if (dependency.match(pluginRegExp)) {
					add = true;
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
					add = true;
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
	}
	
	function _loadModule(id, cb, scriptText) {
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
			if (scriptText) {
				geval(scriptText);
			}
			_loadModuleDependencies(expandedId, function() {
				moduleStack.pop();
				cblist[expandedId].push({cb:cb, mid:dependentId});
            });
		}

		function inInjectQueue(id) {
			for (var i = 0; i < injectQueue.length; i++) {
				if (injectQueue[i].id === id) {
					return true;
				}
			}
			return false;
		}

		if (modules[expandedId] === undefined && scriptText === undefined) {
			if  (inInjectQueue(expandedId)) {
				cblist[expandedId].push({cb:cb, mid:dependentId});
			} else {
				injectQueue.push({id:expandedId, cb: _load});
				processInjectQueue();
			}
		} else {
			_load();
		}
	}
	
	function _clone(obj) {
		if (null === obj || "object" !== typeof obj) { return obj; }
		var copy;
		if (obj instanceof Array) {
			copy = [];
	        var len = obj.length;
	        for (var i = 0; i < len; ++i) {
	            copy[i] = _clone(obj[i]);
	        }
	        return copy;
	    }
	    if (obj instanceof Object) {
			copy = {};
			for (var attr in obj) {
				if (obj.hasOwnProperty(attr)) {
					if (isFunction(obj[attr])) {
						copy[attr] = "function";
					} else {
						copy[attr] = _clone(obj[attr]);
					}
	            }
	        }
	        return copy;
	    }
	    throw new Error("Unable to clone");
	}

	function _createScriptTag(url, cb) {
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
	}
	
	function _createZazlUrl(modules) {
		var locale = "en-us";
		if (window.dojoConfig && window.dojoConfig.locale) {
			locale = dojoConfig.locale;
		}
		var configString = JSON.stringify(_clone(cfg));
		var zazlUrl = cfg.injectUrl+"?modules=";
		for (var i = 0; i < modules.length; i++) {
			zazlUrl += modules[i];
			zazlUrl += i < (modules.length - 1) ? "," : "";
		}
		zazlUrl += "&writeBootstrap=false&locale="+locale+"&config="+encodeURIComponent(configString)+"&exclude=";
		for (i = 0; i < analysisKeys.length; i++) {
			zazlUrl += analysisKeys[i];
			zazlUrl += i < (analysisKeys.length - 1) ? "," : "";
		}
		return zazlUrl;
	}
	
	function _checkForJSON(cb) {
		if (typeof JSON === 'undefined') {
			console.log("JSON is not available in this Browser. Loading JSON script");
			_createScriptTag(zazlpath+"/json2.js", function(){
				cb();
			})
		} else {
			cb();
		}
	}
	
	function _inject(moduleIds, cb) {
		var notLoaded = [];
		var i;
		for (i = 0; i < moduleIds.length; i++) {
			var id = moduleIds[i];
			if (id.match(pluginRegExp)) {
				id = id.substring(0, id.indexOf('!'));
			}
			if (modules[id] === undefined) {
				notLoaded.push(moduleIds[i]);
			}
		}
		if (notLoaded.length < 1) {
			cb();
			processQueues();
			return;
		}
		_checkForJSON(function() {
			var url = _createZazlUrl(notLoaded);
			_createScriptTag(url, function(){
				processCache();
				cb();
				queueProcessor();
			});
		});
	}
	
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
				queueProcessor();
			};
			plugin.load(pluginModuleName, req, load, cfg);
		});
	}
	
	function _createRequire(id) {
		var req = function(dependencies, callback) {
			var savedStack = moduleStack;
			moduleStack = [id];
			if (isFunction(callback)) {
				_require(dependencies, function() {
					callback.apply(null, arguments);
				});
				moduleStack = savedStack;
			} else {
				var mod = _require(dependencies, callback);
				moduleStack = savedStack;
				return mod;
			}
		};
		req.toUrl = function(moduleResource) {
			var savedStack = moduleStack;
			moduleStack = [id];
			var url = _idToUrl(_expand(moduleResource)); 
			moduleStack = savedStack;
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
        req.idle = function() {
			return pageLoaded;
        };
        req.on = function(type, callback) {
			if (type === "idle") {
				if (pageLoaded) {
					callback();
				} else {
					readyCallbacks.push(callback);
				}
			} else {
				console.log("Unsupported 'on' type ["+type+"]");
			}
        };
		return req;
	}
	
	define = function (id, dependencies, factory) {
		var simpleCJS = false;
		if (!isString(id)) { 
			factory = dependencies;
			dependencies = id;
			id = _getCurrentId();
		}

		if (modules[id] !== undefined) { 
			throw new Error("A module with an id of ["+id+"] has already been provided");
		}
		var args;
		if (!isArray(dependencies)) {
			simpleCJS = true;
			factory = dependencies;
			dependencies = [];
		} else {
			args = [];
		}
		modules[id] = {id: id, exports: {}, args: args, deploaded: {}, dependencies: dependencies, config: function() { if (!cfg.config[id]) { cfg.config[id] = {}; } return cfg.config[id]; }};
		if (isFunction(factory)) {
			var scancjs = cfg ? cfg.scanCJSRequires : false;
			if (scancjs && simpleCJS) {
				factory.toString().replace(commentRegExp, "").replace(cjsRequireRegExp, function (match, dep) {
					modules[id].dependencies.push("~#"+dep);
	            });
			}
			modules[id].factory = factory;
		} else {
			modules[id].literal = factory;
		}
	};
	
    define.amd = {
        plugins: true,
        jQuery: true
    };

	var _require = function (dependencies, callback) {
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
	modules["require"].dependencies = [];

	function processConfig(config) {
		if (!cfg) {
			var i;
			cfg = config || {};
			if (cfg.paths) {
				for (var p in cfg.paths) {
					var path = cfg.paths[p];
					paths[p] = path;
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
					} else if (pkg.main.match(/.js$/g)) {
						pkg.main = pkg.main.substring(0, pkg.main.lastIndexOf(".js"));
					}
					pkgs[pkg.name] = pkg;
				}
			}
			cfg.baseUrl = cfg.baseUrl || "./";

			if (cfg.baseUrl.charAt(0) !== '/' && !cfg.baseUrl.match(/^[\w\+\.\-]+:/)) {
				cfg.baseUrl = _normalize(window.location.pathname.substring(0, window.location.pathname.lastIndexOf('/')) + '/'+ cfg.baseUrl);
			}

			cfg.injectUrl = cfg.injectUrl || "_javascript";
			cfg.scanCJSRequires = cfg.scanCJSRequires || false;
			cfg.debug = cfg.debug || false;
			cfg.config = cfg.config || {};
			cfg.map = cfg.map || {};
		}
	}
	
	function _loadScriptViaXHR(url, cb) {
		var xhr = new XMLHttpRequest();
		if (xhr === null) {
			throw new Error("Unable to load ["+url+"] : XHR unavailable");
		}
		xhr.open("GET", url, true);
		xhr.onreadystatechange = function() {
			if (xhr.readyState == 4) {
				if (xhr.status == 200) {
					cb(xhr.responseText);
				} else {
					throw new Error("Unable to load ["+url+"]:"+xhr.status);
				}
			}
		};
		xhr.send(null);
	}
	
	function doHeadRequest(mods, cb) {
		var xhr = new XMLHttpRequest();
		var zazlUrl = _createZazlUrl(mods);
		xhr.open("HEAD", zazlUrl, true);
		xhr.onreadystatechange = function() {
			if (xhr.readyState === 4) {
				if (xhr.status === 200 || xhr.status === 304) {
					cb(true);
				} else if (xhr.status === 404) {
					cb(false);
				} else {
					throw new Error("Zazl Servlet Head request failed : "+xhr.status);
				}
			}
		};
		xhr.send(null);
	}
	
	zazl = function(config, dependencies, callback) {
		if (!isArray(config) && typeof config === "object") {
			processConfig(config);
		} else {	
			callback = dependencies;
			dependencies = config;
			processConfig(typeof zazlConfig === 'undefined' ? {} : zazlConfig);
		}

		if (!isArray(dependencies)) {
			callback = dependencies;
			dependencies = [];
		}
		
		function _callRequire(mods, cb) {
			var _cb = cb;
			_require(mods, function() {
				if (isFunction(_cb)) {
					_cb.apply(null, arguments);
				}
				fireIdleEvent();
				var qe = reqQueue.shift();
				if (qe) {
					_load(qe.mods, qe.cb);
				}
			});
		}
		
		function _loadViaWarmup(mods, cb) {
			require(mods, cb);
			requireInProcess = false;
			var qe = reqQueue.shift();
			if (qe) {
				_load(qe.mods, qe.cb);
			}
		}

		function _loadViaZazl(mods, cb) {
			if (cfg.directInject && mods.length > 0) {
				_inject(mods, function(){
					_callRequire(mods, cb);
				});
			} else {
				_callRequire(mods, cb);
				processCache();
				queueProcessor();
			}
		}
		
		function _load(mods, cb) {
			requireInProcess = true;
			if (cfg.warmupLoader) {
				if (warmupState === 0) {
					_checkForJSON(function() {
						doHeadRequest(mods, function(useZazl) {
							if (useZazl) {
								warmupState = 1;
								_loadViaZazl(mods, cb);
							} else {
								warmupState = 2;
								_loadScriptViaXHR(cfg.warmupLoader, function(scriptText) {
									window.require = undefined;
									window.define = undefined;
									geval(scriptText);
									_loadViaWarmup(mods, cb);
								});
							}
						});
					});
				} else if (warmupState === 1) {
					_loadViaZazl(mods, cb);
				} else if (warmupState === 2) {
					_loadViaWarmup(mods, cb);
				}
			} else {
				_loadViaZazl(mods, cb);
			}
		}

		if (requireInProcess) {
			reqQueue.push({mods: dependencies, cb: callback});
		} else {
			_load(dependencies, callback);
		}
	};
	
	zazl.addToCache = function(id, value) {
		precache[id] = value;
	};
	
	zazl._getConfig = function() { return cfg};

	function processCache() {
		for (var id in precache) {
			var resolvedId = _idToUrl(id);
			var cacheValue = precache[id];
			cache[resolvedId] = cacheValue;
			cache["url:"+resolvedId] = cacheValue;
		}
		precache = {};
	};

	zazl.addAnalysisKey = function(key) {
		analysisKeys.push(key);
	};
	
	function domReady() {
		if (domLoaded === false) {
			domLoaded = true;
			if (modulesLoaded) {
				processQueues();
			}
		}
	}
	
	if (document.addEventListener) {
		document.addEventListener("DOMContentLoaded", domReady, false);
	} else if (document.attachEvent)  {
		document.attachEvent( "onreadystatechange", domReady);
	} 
	
	if (!require) {
		require = zazl;
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
	}

	function processInjectQueue() {
		if (!injectInProcess && injectQueue.length > 0) {
			injectInProcess = true;
			var injection = injectQueue.shift();
			_inject([injection.id], function() {
				injection.cb();
				injectInProcess = false;
			});
		}
	}

	function processQueues() {
		var allLoaded = true, mid, m, ret;

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
						if (m.args === undefined) {
							m.args = [m.cjsreq, m.exports, m];
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
			}

			var savedStack;

			var cbiterate = function(exports, itr) {
				if (itr.hasMore()) {
					var cbinst = itr.next();
					if (!cbinst.called) {
						cbinst.called = true;
						if (cbinst.mid !== "") {
							savedStack = moduleStack;
							moduleStack = [cbinst.mid];
						}
						cbinst.cb(exports);
						if (cbinst.mid !== "") {
							moduleStack = savedStack;
						}
					}
					cbiterate(exports, itr);
				} else {
					cblist[mid] = [];
				}
			};
			for (mid in cblist) {
				if (modules[mid] && modules[mid].loaded) {
					cbiterate(modules[mid].exports, new Iterator(cblist[mid]));
				}
			}
			if (!pageLoaded && domLoaded && modulesLoaded) {
				for (mid in cblist) {
					if (modules[mid] && modules[mid].loaded) {
						cbiterate(modules[mid].exports, new Iterator(cblist[mid]));
					}
				}
				if (cfg.debug) {console.log("Page load complete");}
				pageLoaded = true;
				for (var i = 0; i < readyCallbacks.length; i++) {
					readyCallbacks[i]();
				}
			}
			processInjectQueue();
		} catch (e) {
			console.log("queueProcessor error : "+e);
			allLoaded = true;
			if (requireInProcess) { fireIdleEvent(); }
		}
		return allLoaded;
	}
	
	function queueProcessor() {
		var poller = function() {
			if (processQueues()) { return; }
			setTimeout(poller, 0);
		};
		poller();
	}
	
	function fireIdleEvent() {
		requireInProcess = false;
		if (window.addEventListener) {
			var zazlIdleEvt = document.createEvent('Event');
			zazlIdleEvt.initEvent('zazlIdle', true, true);
			window.dispatchEvent(zazlIdleEvt);
		}
	}
}());
