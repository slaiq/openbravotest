// The ListPalette contains components available
// for use, with default settings.
isc.ListPalette.create({
    ID: "listPalette",
    width: "25%",
  
    // The regular ListGrid property
    fields: [
        {name: "title", title: "Form Component"}
    ],

    // We are supplying the component data inline for this example.
    // However, ListPalette is a subclass of ListGrid, so you could
    // also use a dataSource.
    data: [{
        // Title as you want it to appear in the list
        title: "text",
        // type indicates the class of object to create for
        // this component
        type: "TextItem",
        // defaults specifies the properties to use when
        // creating the component
        defaults: {
        }
    },{
        title: "staticText", 
        type: "StaticTextItem", 
        defaults: {
        }
    },{
        title: "blurb", 
        type: "BlurbItem", 
        defaults: {
        }
    },{
        title: "checkbox", 
        type: "CheckboxItem", 
        defaults: {
        }
    },{
        title: "select", 
        type: "SelectItem", 
        defaults: {
        }
    },{
        title: "comboBox", 
        type: "ComboBoxItem", 
        defaults: {
        }
    },{
        title: "date", 
        type: "DateItem", 
        defaults: {
            useTextField: true
        }
    },{
        title: "time", 
        type: "TimeItem", 
        defaults: {
        }
        
    }]
});

isc.ClassFactory.defineClass("HiddenEditContext", "EditContext");


//The editCanvas is the root component in which the items can be placed.
isc.Canvas.create({
    ID: "editCanvas",
    border: "1px solid black"
});

var editContext = isc.HiddenEditContext.create({
    defaultPalette: listPalette,

    rootComponent: {
        type: "Canvas",
        liveObject: editCanvas
    },

    nodeAdded : function (newNode, parentNode, rootNode) {
        // Enable editMode on new node if parent editMode is enabled
        if (parentNode && this.isNodeEditingOn(parentNode)) {
            this.enableEditing(newNode);
        }
        // Save first node added to the tree. That node is where
        // double-clicked paletteNodes should be placed by default
        // (i.e. defaultParent).
        if (!this.defaultParent) this.defaultParent = newNode;
    },

    // Return the defaultParent saved in nodeAdded() above if available
    getDefaultParent : function (newNode, returnNullIfNoSuitableParent) {
        return this.defaultParent || this.getRootEditNode();
    }
});

// Set the defaultEditContext on palette which is used when double-clicking on components.
listPalette.setDefaultEditContext(editContext);

// Place editCanvas into editMode to allow paletteNode drops
var editCanvasEditNode = editContext.getRootEditNode();
editCanvas.setEditMode(true, editContext, editCanvasEditNode);


//The above use of an edit canvas and edit context can be replaced by
//an EditPane which combines these separate parts into a single component.


// Place base component (DynamicForm) into editContext. All paletteNodes are
// FormItems that will be added to this form
editContext.addFromPaletteNode({
    type: "DynamicForm",
    defaults: {
        // border: "1px dashed red",
        width: "100%",
        height: "100%"
    }
});

isc.VLayout.create({
    ID: "vLayout",
    width: "100%",
    height: "100%",
    membersMargin: 10,
    members: [
        isc.HLayout.create({
            ID: "hLayout",
            membersMargin: 20,
            width: "100%",
            height: "100%",
            members: [
                listPalette,
                editCanvas
            ]
        })
    ]
});


// This button will destroy the Edit Portal and then recreate it from saved state.
isc.Button.create({
    ID: "destroyAndRecreateButton",
    title: "Destroy and Recreate",
    autoFit: true,
    layoutAlign: "right",

    destroyAndRecreateEditPane : function () {
        // We save the editPane node data in a variable
        var paletteNodes = editContext.serializeAllEditNodes();

        // Animate the disappearance of the editCanvas, since otherwise
        // everything happens at once.
        editCanvas.animateFade(0, function () {
            // Once the animation is finished, destroy all the nodes
            editContext.destroyAll();

            // Then add them back from the serialized form
            editContext.addPaletteNodesFromXML(paletteNodes);

            // And make us visible again
            editCanvas.setOpacity(100);
        }, 2000, "smoothEnd");
    },

    click : function () {
        this.destroyAndRecreateEditPane();
    }
});

// This inserts the button into the overall layout for the example.
vLayout.addMember(destroyAndRecreateButton, 0);
