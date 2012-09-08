define([
    'dojo/dom',    
    '../../MappedModule1',    
	'dojo/domReady!'
], function (dom, MappedModule) {
	dom.byId("mapTest6Node").innerHTML = MappedModule;
    return {};
});
