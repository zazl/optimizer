/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
var dojo = dojo || {}
dojo.optimizer = dojo.optimizer || {}

dojo.optimizer.MapEntry = function(key, value) {
	this.key = key;
	this.value = value;
};

dojo.optimizer.Map = function() {
	this.entries = {};
};

dojo.optimizer.Map.prototype = {
	add: function(key, value) {
		this.entries[key] = new dojo.optimizer.MapEntry(key, value);
	},
	
	get: function(key) {
		if (this.entries[key] !== undefined) {
			return this.entries[key].value;
		} else {
			return this.entries[key];
		}
	},
	
	values: function() {
		var values = [];
		for (var key in this.entries) {
			values.push(this.entries[key].value);
		}
		return values;
	}
};