define([
	'dojo',
	'dijit/Calendar',
	'dojo/domReady!'
], function (dojo, Calendar) {
    var calendar = new Calendar({}, dojo.byId("calendarNode"));
    return calendar;
});
