define([
    'dojo/dom',    
    './MappedModule',    
	'dojo/domReady!'
], function (dom, MappedModule) {
	dom.byId("mapTest1Node").innerHTML = MappedModule;
    return {};
});
