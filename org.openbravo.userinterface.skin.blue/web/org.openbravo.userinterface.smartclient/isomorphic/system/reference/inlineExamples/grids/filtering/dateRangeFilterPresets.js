// ---------------------------------------------------------------------------------------
// Date Range (Presets)


isc.DataSource.create({
    ID: "presetDateRangeDS",
    clientOnly: true,
    fields: [
        { name: "customerID" },
        { name: "customerName" },
        { name: "orderID" },
        { name: "orderDate", type: "date" },
        { name: "orderDescription" },
        { name: "orderQty" }
    ],
    testData: presetDateRangeData
});


presetDateRangeDS.addSearchOperator({
    ID: "recentDateRange",
    title: "in recent date range",
    valueType: "custom",
    editorType: "RecentDateRangeItem",
    getCriterion : function (fieldName, item) {
        return item.getCriterion();
    }
}, ["date"] );

isc.Label.create({
    contents: "RecentDateRangeItem (ListGrid FilterEditor)",
    top: 10,
    left: 10,
    width: "90%",
    height: 25,
    autoDraw: true,
    baseStyle: "exampleSeparator"
});

isc.ListGrid.create({
    ID: "grid1",
    top: 50, width: 590, height: 120, left: 10,
    dataSource: presetDateRangeDS,
    autoFetchData: true,
    useAllDataSourceFields: true,
    showFilterEditor: true,
    canGroupBy: true,
    fields: [
        { name: "orderDate", filterEditorType: "RecentDateRangeItem" }
    ]
});


// ---------------------------------------------------------------------------------------
// FilterBuilder Example 

isc.Label.create({
    contents: "RecentDateRangeItem (FilterBuilder)",
    top: 200,
    left: 10,
    width: "90%",
    height: 25,
    autoDraw: true,
    baseStyle: "exampleSeparator"
});

isc.FilterBuilder.create({
    ID: "filterBuilder",
    top: 240, width: 595, height: 70,
    dataSource: presetDateRangeDS,
    criteria: { _constructor: "AdvancedCriteria", operator: "and",
        criteria: [
            { fieldName: "orderDate" }
        ]
    }
});

isc.Button.create({
    ID: "searchButton",
    title: "Filter",
    top: 310,
    autoFit: true,
    click: function () {
        var criteria = filterBuilder.getCriteria();
        grid2.fetchData(criteria);
    }
});

isc.ListGrid.create({
    ID: "grid2",
    top: 340, width: 590, height: 120,
    dataSource: presetDateRangeDS,
    autoFetchData: true,
    showFilterEditor: true,
    useAllDataSourceFields: true,
    fields: [
        { name: "orderDate", filterEditorType: "RecentDateRangeItem" }
    ]
});

