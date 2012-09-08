define([
    'dojo/dom',    
    './subdir/subdir2/ModuleV1-6',    
	'dojo/domReady!'
], function (dom, MappedModule) {
	dom.byId("mapTest7Node").innerHTML = MappedModule;
    return {};
});
