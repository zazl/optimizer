/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
dojo.baseUrl = dojo.config["baseUrl"];

dojo._loadUri = function(uri, cb) {
	try{
		if (cb) {
			cb(loadJS(uri));
		} else {
			loadJS(uri);
		}
		return true;
	}catch(e){
		print("load for ('" + uri + "') failed. Exception: " + e.name + " : [" + e.message +"] at line "+e.lineNumber);
		return false;
	}
};

dojo._getText = function(/*URI*/ uri, /*Boolean*/ fail_ok) {
	try {
		var contents = readText(uri);
		if (contents === null) {
			print("Contents of ["+uri+"] is empty");
		}
		return contents;
	} catch(e) {
		print("getText('" + uri + "') failed. Exception: " + e);
		if(fail_ok){ return null; }
		throw e;
	}
};
