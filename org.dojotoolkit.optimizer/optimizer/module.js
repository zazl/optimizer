/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
var dojo = dojo || {}
dojo.optimizer = dojo.optimizer || {}

dojo.optimizer.Module = function(id, uri) {
	this.id = id;
	this.uri = uri;
	this.dependencies = [];
	this.dependents = [];
};

dojo.optimizer.Module.prototype = {
	addDependency: function(dependency) {
		this.dependencies.push(dependency);
	},
	addDependent: function(dependent) {
		this.dependents.push(dependent);
	}
};