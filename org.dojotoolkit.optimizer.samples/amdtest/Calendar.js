define([
	'dojo',
	'dijit/Calendar'
], function (dojo, Calendar) {
    var calendar = new Calendar({}, dojo.byId("calendarNode"));
    return calendar;
});
