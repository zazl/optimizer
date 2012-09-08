define([
    'dojo/dom',    
    '../MappedModule1',    
	'dojo/domReady!'
], function (dom, MappedModule) {
	dom.byId("mapTest4Node").innerHTML = MappedModule;
    return {};
});
