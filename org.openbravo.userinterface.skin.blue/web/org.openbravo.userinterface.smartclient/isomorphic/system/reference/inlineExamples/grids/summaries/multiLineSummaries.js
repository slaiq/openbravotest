
isc.ListGrid.create({
    ID: "companyList",
    width:600, height:520,
    alternateRecordStyles:true,
    autoFetchData:true,
    dataSource:orderItemLocalDS,
    showAllRecords:true,
    groupByField:"category", groupStartOpen:"all",
    canEdit:true, editEvent:"click",
    
    showGridSummary:true,
    showGroupSummary:true,
    fields:[
        {name:"orderID", includeInRecordSummary:false, summaryFunction:"count"},
        {name:"itemDescription"},
        {name:"category", showGridSummary:true, 
            getGridSummary:function (records, summaryField) {
                var seenCategories = {};
                for (var i = 0; i < records.length; i++) {
                    seenCategories[records[i].category] = true;
                }
                var totalCategories = isc.getKeys(seenCategories).length;
                return totalCategories + " Categories";
                
            },
            summaryFunction:function (records, summaryField) {
                var seenCategories = {};
                for (var i = 0; i < records.length; i++) {
                    seenCategories[records[i].category] = true;
                }
                var totalCategories = isc.getKeys(seenCategories).length;
                return totalCategories + " Categories"; 
            }
        },
        {name:"shipDate",width:150, showGroupSummary:true, showGridSummary:false, summaryFunction:["max", "min"]},
        
        {name:"quantity", showGroupSummary:false, showGridSummary:false},
        {name:"unitPrice", showGroupSummary:false, showGridSummary:false, format: "\u00A4,0.00"},
        {name:"Total", type:"summary", recordSummaryFunction:"multiplier",
         summaryFunction:"sum",
         showGroupSummary:true, showGridSummary:true,
         align:"right",
         format: "\u00A4,0.00"
        }
    ]
})
