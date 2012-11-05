//"function"===typeof define && define([
//(typeof define === "undefined" ? function(deps, def) { def(); } : define)([
define([
	'dojo',
	'dijit/Calendar',
	'module',
	'dojo/domReady!'
], function (dojo, Calendar, module) {
	    var calendar = new Calendar({}, dojo.byId("calendarNode"));
	    console.log(module.config().myconfigval);
	    return calendar;
	
});
