isc.DynamicForm.create({
    width: 400,
    fields: [
        {title:"Text", type:"text", hint: "<nobr>A plain text field</nobr>"},
        {title:"Color Picker", type:"color"},
        {title:"TextArea", type:"textArea"},
        {title: "Spinner", editorType: "spinner", 
         defaultValue: 5, min: 0, max: 10, step: 0.5},
        {title: "Slider", name: "slider", editorType: "slider", width: 180,
         minValue: 1, maxValue: 5, numValues: 5, height: 36},
        {title: "LinkItem", type: "link", name: "link", height: 36, 
         click: "isc.say('Hello world!')", linkTitle: "<br>Click Me<br>"},
        {title: "Checkbox", type: "checkbox", height: 25},
        {title: "Radio Group", type: "radioGroup",
         valueMap: ["Option 1", "Option 2"]}
    ],
    values: { slider: 4 }
});

isc.DynamicForm.create({
    top: 400,
    width: 500,
    numCols: 4,
    isGroup: true,
    groupTitle: "List - ComboBox",
    fields : [{
        name: "bugStatus", title: "Bug Status", 
        editorType: "comboBox",
        valueMap : {
            "new" : "New",
            "active" : "Active",
            "revisit" : "Revisit",
            "fixed" : "Fixed",
            "delivered" : "Delivered",
            "resolved" : "Resolved",
            "reopened" : "Reopened"
        }
    },{
        name: "itemName", title: "Item Name", editorType: "comboBox", 
        optionDataSource: "supplyItem", pickListWidth: 250
    }]
});
