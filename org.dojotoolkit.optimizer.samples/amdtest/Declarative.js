define([
    'dojo',    
	'dojo/parser',
	'i18n!amdtest/nls/messages',
	'dijit/Calendar',
	'dijit/layout/BorderContainer',
	'dijit/layout/ContentPane',
	'dijit/layout/AccordionContainer',
	'dijit/layout/TabContainer',
	'dojo/data/ItemFileReadStore',
	'dijit/tree/ForestStoreModel',
	'dijit/Tree',
	'dijit/form/Form',
	'dijit/form/Button',
	'dijit/form/DateTextBox',
	'dijit/form/ValidationTextBox',
	'dijit/layout/StackController',
	'dijit/layout/StackContainer'
], function (dojo, parser, messages) {
	parser.parse();
	dojo.byId("title").innerHTML = messages.title;
    return this;
});
