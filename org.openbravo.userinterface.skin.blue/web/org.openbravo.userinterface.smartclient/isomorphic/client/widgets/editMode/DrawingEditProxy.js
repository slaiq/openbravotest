/*
 * Isomorphic SmartClient
 * Version v10.0d_2014-02-13 (2014-02-13)
 * Copyright(c) 1998 and beyond Isomorphic Software, Inc. All rights reserved.
 * "SmartClient" is a trademark of Isomorphic Software, Inc.
 *
 * licensing@smartclient.com
 *
 * http://smartclient.com/license
 */



isc.defineClass("DrawPaneEditProxy", "EditProxy").addMethods({
    setEditMode : function (editingOn) {
        this.Super("setEditMode", arguments);

        // Set editMode on all children
        var liveObjects = this.creator.editContext.getEditNodeArray().getProperty("liveObject");
        liveObjects.map("setEditMode", editingOn, this.creator.editContext);

        // Remove any selections/outlines
        if (!editingOn) this.creator.editContext.deselectAllComponents();
    },

    drop : function () {
        var liveObject = this.creator,
            source = liveObject.ns.EH.getDragTarget()
        ;

        // If the source isn't a Palette then perform the standard drop interaction.
        if (!isc.isA.Palette(source)) {
            return liveObject.drop.apply(arguments);
        }

        var data = source.transferDragData(),
            paletteNode = (isc.isAn.Array(data) ? data[0] : data)
        ;
        if (!paletteNode) return false;

        var editProxy = this;
        liveObject.editContext.requestLiveObject(paletteNode, function (editNode) {
            if (editNode) {
                // Add the new component at the current mouse position.
                var node;
                if (isc.isA.DrawPane(liveObject)) {
                    node = liveObject.editContext.addNode(editNode);
                } else {
                    // Wrap the DrawItem in a DrawPane
                    var dropType;

                    if (!source.isA("Palette")) {
                        if (source.isA("FormItemProxyCanvas")) {
                            source = source.formItem;
                        }
                        dropType = source._constructor || source.Class;
                    } else {
                        paletteNode.dropped = true;
                        dropType = paletteNode.type || paletteNode.className;
                    }

                    // Establish the actual drop node (this may not be the canvas accepting the drop - for a
                    // composite component like TabSet, the dropped-on canvas will be the tabBar or 
                    // paneContainer)
                    var dropTargetNode = this.findEditNode(dropType);
                    if (dropTargetNode) {
                        dropTargetNode = dropTargetNode.editNode;
                    }

                    node = liveObject.editContext.addWithWrapper(editNode, dropTargetNode, true);
                }
                node.liveObject.moveTo(liveObject.getOffsetX(), liveObject.getOffsetY());
                
                if (editProxy.enableComponentSelection) liveObject.editContext.selectSingleComponent(node.liveObject);
            }
        }, source);

        return isc.EventHandler.STOP_BUBBLING;
    },
    
    // Indicate selection of a set of components
    setOutline : function (components) {
        if (!components) return;
        if (!isc.isAn.Array(components)) components = [components];
        for (var i = 0; i < components.length; i++) {
            components[i].showAllKnobs();
        }
    },

    // clear outline on a set of components
    clearOutline : function (components) {
        if (!components) return;
        if (!isc.isAn.Array(components)) components = [components];
        for (var i = 0; i < components.length; i++) {
            components[i].hideAllKnobs();
        }
    },

    // Title editing for a single selected item is supported by two means:
    // - When title is null and component is selected, just start typing
    
    selectedComponentsUpdated : function (component, componentList) {
        // Handle one selection replace with another
        if (componentList != null && componentList.length == 1 && !this._keyPressEventID) {
            var value = (isc.isA.DrawLabel(component) ? component.contents : component.title);
            if (value == null) {
                this._keyPressEventID = isc.Page.setEvent("keyPress", this);
            }
        } else if (this._keyPressEventID) {
            isc.Page.clearEvent("keyPress", this._keyPressEventID);
            delete this._keyPressEventID;
        }
    },
    
    pageKeyPress : function (target, eventInfo) {
        var key = isc.EH.getKeyEventCharacter();
        if (isc.isA.AlphaNumericChar(key)) {
            var liveObject = this.creator,
                selection = liveObject.editContext.getSelectedComponents()
            ;
            if (selection.length == 1) {
                isc.Page.clearEvent("keyPress", this._keyPressEventID);
                delete this._keyPressEventID;

                var editProxy = this;
                selection[0].editProxy.editTitle(null, key, function (value) {
                    // If title value is still null, re-register keyPress handler
                    // to allow typing it again
                    if (value == null) {
                        editProxy._keyPressEventID = isc.Page.setEvent("keyPress", editProxy);
                    }
                });

            }
        }
    }
});

isc.defineClass("DrawItemEditProxy", "EditProxy").addMethods({

    getOverrideProperties : function () {
        var properties = this.Super("getOverrideProperties", arguments);

        isc.addProperties(properties, {
            canDrag: true,
            cursor: "move"
        });

        return properties;
    },

    click : function () {
        var liveObject = this.creator;

        if (liveObject.drawPane.editProxy.enableComponentSelection) {
        	liveObject.editContext.selectSingleComponent(liveObject);
	        return isc.EH.STOP_BUBBLING;
	    }
    },

    titleEditorDefaults: {
        // Use TextAreaItem instead of TextItem for title editor
        type: "textarea"
    },

    // DRAG EVENTS - Defer to DrawItem instead of EditProxy
    dragStart : function (event, info) {
        this.creator.dragStart(event, info);
    },
    dragMove : function (event, info, bubbledFromDrawItem) {
        this.creator.dragMove(event, info, bubbledFromDrawItem);
    },
    dragEnd : function (event, info) {
        this.creator.dragEnd(event, info);
    }
});