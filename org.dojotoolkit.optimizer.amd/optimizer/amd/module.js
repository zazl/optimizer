/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
Module = function(id, uri) {
	this.id = id;
	this.uri = uri;
	this.dependencies = [];
	this.dependents = [];
};

Module.prototype = {
	addDependency: function(dependency) {
		this.dependencies.push(dependency);
	},
	addDependent: function(dependent) {
		this.dependents.push(dependent);
	}
};

exports.createModule = function(id, uri) {
	return new Module(id, uri);
};