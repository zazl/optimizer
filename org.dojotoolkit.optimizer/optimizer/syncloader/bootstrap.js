/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
djConfig = {
	isDebug: false,
	usePlainJson: true,
	baseUrl: "/dojo/"
};
loadJS("/dojo/_base/_loader/bootstrap.js");
dojo._hasResource = {};
loadJS("/dojo/_base/_loader/loader.js");
loadJS("/optimizer/syncloader/hostenv_optimizer.js");
loadJS("/dojo/_base.js");
loadJS("/json/json2.js");
loadJS("/optimizer/module.js");
loadJS("/optimizer/map.js");
	
