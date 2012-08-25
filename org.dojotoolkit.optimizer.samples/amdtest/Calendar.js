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
