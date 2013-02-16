/*
    Copyright (c) 2004-2013, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/

var zazlWarmup;

(function () {
	var opts = Object.prototype.toString;
	
    function isFunction(it) { return opts.call(it) === "[object Function]"; };
    function isArray(it) { return opts.call(it) === "[object Array]"; };
    function isString(it) { return (typeof it == "string" || it instanceof String); };

	function clone(obj) {
		if (null == obj || "object" != typeof obj) return obj;
		if (obj instanceof Array) {
	        var copy = [];
	        var len = obj.length;
	        for (var i = 0; i < len; ++i) {
	            copy[i] = clone(obj[i]);
	        }
	        return copy;
	    }
	    if (obj instanceof Object) {
			var copy = {};
			for (var attr in obj) {
				if (obj.hasOwnProperty(attr)) {
					if (isFunction(obj[attr])) {
						copy[attr] = "function";
					} else {
						copy[attr] = clone(obj[attr]);
					}
	            }
	        }
	        return copy;
	    }
	    throw new Error("Unable to clone");
	};
	
	function normalize(path) {
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
	
	function createConfig() {
		var i;
		var cfg = clone(zazlConfig);
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
				cfg.packages[i] = pkg;
			}
		}
		cfg.baseUrl = cfg.baseUrl || "./";

		if (cfg.baseUrl.charAt(0) !== '/' && !cfg.baseUrl.match(/^[\w\+\.\-]+:/)) {
			cfg.baseUrl = normalize(window.location.pathname.substring(0, window.location.pathname.lastIndexOf('/')) + '/'+ cfg.baseUrl);
		}

		cfg.injectUrl = cfg.injectUrl || "_javascript";
		cfg.scanCJSRequires = cfg.scanCJSRequires || false;
		cfg.debug = cfg.debug || false;
		cfg.config = cfg.config || {};
		cfg.map = cfg.map || {};
		return cfg;
	};
	
	function createScriptTag(url, cb) {
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
	
	function createZazlUrl(url, modules) {
		var locale = "en-us";
		if (window.dojoConfig && window.dojoConfig.locale) {
			locale = dojoConfig.locale;
		}
		var configString = JSON.stringify(createConfig());
		var zazlUrl = url+"?modules=";
		for (var i = 0; i < modules.length; i++) {
			zazlUrl += modules[i];
			zazlUrl += i < (modules.length - 1) ? "," : "";
		}
		zazlUrl += "&writeBootstrap=false&locale="+locale+"&config="+encodeURIComponent(configString)+"&exclude=";
		return zazlUrl;
	};
	
	zazlWarmup = function(zazlscripturl, altscripturl, modules, cb) {
		if (!zazlConfig) {
			throw new Error("A zazlConfig object must be defined");
		}
		var xhr = new XMLHttpRequest();
		var url = zazlConfig.injectUrl || "_javascript";
		var zazlUrl = createZazlUrl(url, modules);
		xhr.open("HEAD", zazlUrl, true);
		xhr.onreadystatechange = function() {
			if (xhr.readyState == 4) {
				if (xhr.status == 200 || xhr.status == 304) {
					createScriptTag(zazlscripturl, function() {
						console.log("loaded zazl script tag");
						cb();
					});
				} else if (xhr.status == 404) {
					createScriptTag(altscripturl, function() {
						console.log("loaded alternative script tag");
						cb();
					});
				} else {
					throw new Error("Zazl Servlet Head request failed : "+xhr.status);
				}
			}
		};
		xhr.send(null);
	}
}());