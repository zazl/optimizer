/*
    Copyright (c) 2004-2012, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/

var http = require('http');
var fs = require('fs');
var nodeStatic = require('node-static');
var zazloptimizer = require('zazloptimizer');

var appdirPath = process.argv.length > 2 ? process.argv[2] : process.cwd();
var appdir = fs.realpathSync(appdirPath);
var port = parseInt(process.argv.length > 3 ? process.argv[3] : "8080");
var compress = process.argv.length > 4 ? process.argv[4] : "true";
compress = (compress === "true") ? true : false;

var optimizerHandler = zazloptimizer.createOptimizer(appdir, compress);
var fileServer = new nodeStatic.Server(appdir);
var loaderServer = new nodeStatic.Server(zazloptimizer.getLoaderDir());

http.createServer(function (request, response) {
	optimizerHandler
	.handle(request, response)
	.otherwise(function(){
        fileServer.serve(request, response, function(evt) {
        	if (evt !== null && evt.status === 404) {
        		loaderServer.serve(request, response);
        	}
        });
	})
	.error(function(state, msg){
		console.log("error : "+state+" : "+msg);
	});
}).listen(port);

console.log("Zazl Server available on port "+port+" loading from ["+appdir+"] compress = "+compress);

