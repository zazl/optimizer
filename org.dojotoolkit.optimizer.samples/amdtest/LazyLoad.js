define(["require",
	'dijit/registry',
    'dojo/_base/connect',
	'dojo/parser',
	'dijit/form/Button',
	'dijit/layout/ContentPane',
	'dojo/domReady!'
], function (req, registry, connector, parser) {
	parser.parse();
    var colorPalette;
	connector.connect(registry.byId("lazyLoadButton"), "onClick", function(){
		if (!colorPalette) {
	    	req(["dijit/ColorPalette"], function(ColorPalette) {
	    			console.log("ColorPalette loaded");
	    			colorPalette = new ColorPalette({}, "colorPaletteNode");
	    	});
		}
	});
    return {};
});
