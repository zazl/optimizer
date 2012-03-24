/*
    Copyright (c) 2004-2012, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/

var http = require('http');
var fs = require('fs');
var connect = require('connect');
var zazloptimizer = require('zazloptimizer');

var defaultPort = process.env.npm_package_config_port || 8080;
var defaultAppdir = process.env.npm_package_config_appdir || "./";
var defaultCompress = process.env.npm_package_config_compress || "true";

var appdirPath = process.argv.length > 2 ? process.argv[2] : defaultAppdir;
var appdir = fs.realpathSync(appdirPath);
var port = process.argv.length > 3 ? parseInt(process.argv[3]) : defaultPort;
var compress = process.argv.length > 4 ? process.argv[4] : defaultCompress;
compress = (compress === "true") ? true : false;

var connectOptimizer = zazloptimizer.createConnectOptimizer(appdir, compress);

var app = connect()
	.use("/_javascript", connectOptimizer)
	.use(connect.static(appdir))
	.use(connect.static(zazloptimizer.getLoaderDir()));

http.createServer(app).listen(port);

console.log("Zazl Server available on port "+port+" loading from ["+appdir+"] compress = "+compress);

