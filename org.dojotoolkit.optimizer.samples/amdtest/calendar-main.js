zazl({
	packages: [{name: 'dojo'}, {name: 'dijit'}, {name: 'dojox'}],
    config: { "amdtest/Calendar": {myconfigval: "My Config Value"}}
}, 
["amdtest/Calendar"], 
function(calendar) {
    console.log("done");
});
