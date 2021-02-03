isc.FilterBuilder.create({
    ID:"advancedFilter",
    dataSource:"worldDS",
    topOperatorAppearance: "radio"
});

isc.ListGrid.create({
    ID: "countryList",
    width:550, height:224, alternateRecordStyles:true, 
    dataSource: worldDS,
    fields:[
        {name:"countryName"},
        {name:"continent"},
        {name:"population"},
        {name:"area"},
        {name:"gdp"},
        {name:"independence"}
    ],
    autoFetchData: true
})

isc.IButton.create({
    ID:"filterButton",
    title:"Filter",
    click : function () {
        countryList.filterData(advancedFilter.getCriteria());
    }
})

isc.VStack.create({
    membersMargin:10,
    members:[ advancedFilter, filterButton, countryList ]
})
