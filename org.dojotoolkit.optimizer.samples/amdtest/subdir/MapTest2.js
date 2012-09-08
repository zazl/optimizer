define([
    'dojo/dom',    
    '../MappedModule2',    
	'dojo/domReady!'
], function (dom, MappedModule) {
	dom.byId("mapTest5Node").innerHTML = MappedModule;
    return {};
});
