define([
    'dojo/dom',    
    './MappedModule',    
	'dojo/domReady!'
], function (dom, MappedModule) {
	dom.byId("mapTest3Node").innerHTML = MappedModule;
    return {};
});
