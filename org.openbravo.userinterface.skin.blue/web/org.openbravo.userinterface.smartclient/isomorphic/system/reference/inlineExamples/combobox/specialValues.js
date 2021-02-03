isc.DynamicForm.create({
//    ID:"testForm",
    width: 500,
    numCols:4,
    fields : [
    {
        name: "filteredSelect", title: "Choose an item (Select)", editorType: "SelectItem", 
        optionDataSource: "supplyItem", 
        displayField:"itemName", valueField:"itemID",
        pickListWidth:300,
        pickListProperties: {
            showFilterEditor:true
        },
        pickListFields:[
            {name:"SKU"},
            {name:"itemName"}
        ],
        specialValues: { "**EmptyValue**": "None", "-1": "Not Applicable" },
        separateSpecialValues: true
    },
    {
        name: "filteredCombo", title: "Choose an item (ComboBox)", editorType: "ComboBoxItem", 
        addUnknownValues:false,
        optionDataSource: "supplyItem", 
        displayField:"itemName", valueField:"itemID",
        filterFields:["SKU", "itemName"],
        pickListWidth:300,
        pickListFields:[
            {name:"SKU"},
            {name:"itemName"}
        ],
        specialValues: { "**EmptyValue**": "None", "-1": "Not Applicable" },
        separateSpecialValues: true
    }
    ]
});


