/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/

/*
 * The following is copied from https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Array/Reduce
 * in support of Array.reduce required by uglifyjs
 * 
 */

if (!Array.prototype.reduce)
{
  Array.prototype.reduce = function(fun /* , initialValue */)
  {
    "use strict";

    if (this === void 0 || this === null)
      throw new TypeError();

    var t = Object(this);
    var len = t.length >>> 0;
    if (typeof fun !== "function")
      throw new TypeError();

    // no value to return if no initial value and an empty array
    if (len == 0 && arguments.length == 1)
      throw new TypeError();

    var k = 0;
    var accumulator;
    if (arguments.length >= 2)
    {
      accumulator = arguments[1];
    }
    else
    {
      do
      {
        if (k in t)
        {
          accumulator = t[k++];
          break;
        }

        // if array contains no values, no initial value to return
        if (++k >= len)
          throw new TypeError();
      }
      while (true);
    }

    while (k < len)
    {
      if (k in t)
        accumulator = fun.call(undefined, accumulator, t[k], k, t);
      k++;
    }

    return accumulator;
  };
}

escapeString = function (str) {
	return ("\"" + str.replace(/(["\\])/g, "\\$1") + "\"").replace(/[\f]/g, "\\f").replace(/[\b]/g, "\\b").replace(/[\n]/g, "\\n").replace(/[\t]/g, "\\t").replace(/[\r]/g, "\\r");
};

(function(global) {
	var modules = global.modules = global.modules || {};
	
	var require = global.require = function(id) {
		if (id.indexOf('/') !== -1) {
			if (id.charAt(0) === '/') {
				path = id.substring(0, id.lastIndexOf('/')+1);
			} else if (id.charAt(0) === '.' && id.charAt(1) === '/') {
				id = path + id.substring(2);
			}
		} else {
			id = path + id;
		}
	    if (modules[id]) {
	        return modules[id].exports;
	    }
    	var exports = {};
	    (function(exports) {
	    	modules[id] = {};
	    	var module = {id:id, path: path};
			//print("path ["+module.path+"] id ["+module.id+"]");
	    	modules[id].module = module;
	    	modules[id].exports = exports;
	    	loadJSWithExports(id+".js", exports);
	    })(exports);
	    
		return exports;
	};
})(this);