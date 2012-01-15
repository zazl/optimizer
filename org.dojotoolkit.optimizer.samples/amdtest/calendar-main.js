amdlite({
    packages: [
        {
            name: 'dojo',
            location: 'dojo',
            main:'main'
        },
        {
            name: 'dijit',
            location: 'dijit',
            main:'main'
        },
        {
            name: 'dojox',
            location: 'dojox',
            main:'main'
        }
    ]
}, 
["amdtest/Calendar"], 
function(calendar) {
    console.log("done");
});
