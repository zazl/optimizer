(function() {
dojo.provide("test.PersonGrid");
dojo.require("dojox.grid.DataGrid");
dojo.require("dojo.data.ItemFileReadStore");
dojo.require("dojo.parser");
dojo.requireLocalization("dijit", "loading");

	console.debug("PersonGrid loaded");
	var messages = dojo.i18n.getLocalization("dijit", "loading");
	for (var id in messages) {
		console.debug(id+":"+messages[id]);
	}
})();
