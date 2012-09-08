define([
    'dojo/dom',    
    './MappedModule',    
	'dojo/domReady!'
], function (dom, MappedModule) {
	dom.byId("mapTest2Node").innerHTML = MappedModule;
    return {};
});
