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



isc.Canvas.addProperties({
    
    // Enabling EditMode
    // ---------------------------------------------------------------------------------------

    // A hook which subclasses can use if they need to know when they have been added to an editContext
    addedToEditContext : function (editContext, editNode, parentNode, index) {
    },

    //> @method Canvas.updateEditNode()
    // A callback invoked for each +link{EditNode.liveObject,liveObject} by
    // +link{EditContext} when the EditContext is being serialized. The liveObject
    // may make any updates needed to the +link{EditNode} (or the +link{EditContext} as
    // a whole) in order to be able to later recreate the objects.
    // @param editContext (EditContext) the EditContext
    // @param editNode (EditNode) the EditNode
    // @see EditContext.serializeAllEditNodes()
    // @see EditContext.serializeEditNodes()
    // @visibility external
    //<
    updateEditNode : function (editContext, editNode) {
    },

    // A hook called from EditContext.addNode(), allowing the liveParent to wrap a newNode in
    // some additional structure. Return the parentNode that the newNode should be added to.
    // By default, just returns the parentNode supplied.
    
    wrapChildNode : function (editContext, newNode, parentNode, index) {
        // Add an event mask if so configured
        if (newNode.useEditMask == null && this.autoMaskComponents) {
            newNode.useEditMask = true;
        }
        return parentNode;
    },

    editProxyConstructor:"EditProxy",

    useEditMask:true,
    setEditMode : function (editingOn, editContext, editNode) {
        if (editingOn == null) editingOn = true;
        if (this.editingOn == editingOn) return;
        this.editingOn = editingOn;

        if (this.editingOn) {
            this.editContext = editContext;
        }
        
        this.editNode = editNode;
        if (this.editingOn && !this.editProxy) {
            
            var defaults = { selectionType: this.selectionType };
            if (this.editNode && this.editNode.editProxyProperties) isc.addProperties(defaults, this.editNode.editProxyProperties);

            this.editProxy = this.createAutoChild("editProxy", defaults);
        }

        // Allow edit proxy to perform custom operations on edit mode change
        if (this.editProxy) {
            this.editProxy.setEditMode(editingOn);
        }

        // In case anything visual has changed, or the widget has different drag-and-drop
        // behavior in edit mode (register/unregisterDroppableItem is called from redraw)
        this.markForRedraw();
    },

    // Set to true to enable Canvas-based component selection, positioning and resizing.
    // Not used by VisualBuilder but by standalone editors. See EditPane for an example.
    // This property value is pushed onto the editProxy when EditMode is enabled.
    // It can also be specified on a paletteNode with editProxyProperties.
    editProxyDefaults: {
        enableComponentSelection: false
    },

    // Selection management
    // --------------------------------------------------------------------------------------------
    
    //> @attr canvas.selectionType        (SelectionStyle : isc.Selection.MULTIPLE : [IRW])
    // Defines a Canvas's clickable-selection behavior when in edit mode. Only two styles are supported:
    // "single" and "multiple". Multiple selection enables hoop selection.
    //
    // @group selection, appearance
    // @see type:SelectionStyle
    // @visibility internal
    //<
    selectionType: isc.Selection.MULTIPLE

    // XXX - Need to do something about Menus in the drop hierarchy - they aren't Class-based
});




isc.Class.addMethods({
    getSchema : function () {
        // NOTE: this.schemaName allows multiple classes to share a single role within editing,
        // eg the various possible implementations of tabs, section headers, etc
        if (this.schemaName) return isc.DS.get(this.schemaName);
        
        // If we have an SGWT class name, then try to get that schema
        var sgwtClassName = this.getSGWTClassName();
        if (sgwtClassName) {
            var schema = isc.DS.get(sgwtClassName);
            if (schema) return schema;
        }

        // If not available, then get the SmartClient class schema
        return isc.DS.get(this.Class);
    },
    getSchemaField : function (fieldName) {
        return this.getSchema().getField(fieldName);
    },
    getObjectField : function (type) {
        if (!isc.SGWTFactory.getFactory(type) && type.contains(".")) type = type.split(/\./).pop();

        // cache lookups, but only on Canvases.  FIXME: we should really cache lookups only for
        // framework DataSources
        var objectFields = this._objectFields;
        if (isc.isA.Canvas(this)) {
            var undef;
            if (objectFields && objectFields[type] !== undef) {
                //this.logWarn("cache hit: " + type);
                return objectFields[type];
            }
        }

        var schema = this.getSchema();
        if (!schema) {
            this.logWarn("getObjectField: no schema exists for: " + this);
            return;
        }
        var fieldName = schema.getObjectField(type);

        if (isc.isA.Canvas(this)) {
            if (!objectFields) this._objectFields = objectFields = {};
            objectFields[type] = fieldName;
        }

        return fieldName;
    },
    addChildObject : function (newChildType, child, index, parentProperty) {
        return this._doVerbToChild("add", newChildType, child, index, parentProperty);
    },
    removeChildObject : function (childType, child, parentProperty) {
        return this._doVerbToChild("remove", childType, child, parentProperty);
    },

    _doVerbToChild : function (verb, childType, child, index, parentProperty) {
        var fieldName = parentProperty || this.getObjectField(childType);
        var field = this.getSchemaField(fieldName);

        // for fields that aren't set to multiple, call setProperties to add the object, which
        // will look up and use the setter if there is one 
        // (eg field "contextMenu", "setContextMenu")
        if (!field.multiple) {
            var value = (verb == "remove" ? null : child);
            // See if there is a setter on the editProxy for the field.
            // setProperties handles setters on the base object but not the
            // editProxy.
            if (this.editingOn && this.editProxy) {
                var setter = this._getSetter(fieldName);
                if (setter && this.editProxy[setter]) {
                    this.editProxy[setter](value);
//                    if (this.propertyChanged) this.propertyChanged(fieldName, value);
                    this.logInfo(verb + "ChildObject calling set property for fieldName '" + fieldName +
                            "'", "editing");
                    return true;
                }
            }
            var props = {};
            props[fieldName] = value;
            this.logInfo(verb + "ChildObject calling setProperties for fieldName '" + fieldName +
                         "'", "editing");
            this.setProperties(props);
            return true;
        }

        // Try to call field method on editProxy first if it exists.
        var targets = [ this.editProxy, this ];
        for (var i = 0; i < targets.length; i++) {
            var target = targets[i];
            if (target == null) continue;

            var methodName = this.getFieldMethod(target, childType, fieldName, verb);
            if (methodName != null && target[methodName]) {
                this.logInfo("calling " + methodName + "(" + this.echoLeaf(child) + 
                             (index != null ? "," + index + ")" : ")"),
                             "editing");
                target[methodName](child, index);
                return true;
            }
        }

        return false;
    },

    getChildObject : function (type, id, parentProperty) {
        var fieldName = parentProperty || this.getObjectField(type), 
            field = this.getSchemaField(fieldName);

        if (field == null) {
            if (parentProperty) {
                this.logWarn("getChildObject: no such field '" + parentProperty + 
                             "' in schema: " + this.getSchema());
            } else {
                this.logWarn("getChildObject: schema for Class '" + this.Class + 
                             "' does not have a field accepting type: " + type);
            }
            return null;
        }

        // if the field is not array-valued, just use getProperty, which will auto-discover
        // getters
        if (!field.multiple) return this.getProperty(fieldName);

        // otherwise, look for a getter method and call it with the id
        var methodName;
        
        if (isc.isA.ListGrid(this) && fieldName == "fields") {
            methodName = "getSpecifiedField";
        } else {
            methodName = this.getFieldMethod(this, type, fieldName, "get");
        }

        if (methodName == null) var methodName = this.getFieldMethod(this, type, fieldName, "get");
        if (methodName && this[methodName]) {
            this.logInfo("getChildObject calling: " + methodName + "('"+id+"')", "editing");
            return this[methodName](id);
        } else {    
            // if there's no getter method, search the Array directly for something with
            // matching id
            this.logInfo("getChildObject calling getArrayItem('"+id+"',this." + fieldName + ")",
                         "editing");
            return isc.Class.getArrayItem(id, this[fieldName]);
        }
    },

    // get a method that can perform verb "verb" for an object of type "type" being added to a
    // field named "fieldName", eg, "add" (verb) a "Tab" (type) to field "tabs".
    // Uses naming conventions to auto-discover methods.  Subclasses may need to override for
    // non-discoverable methods, eg, canvas.addChild() is not discoverable from the field name
    // ("children") or type ("Canvas").
    getFieldMethod : function (target, type, fieldName, verb) {
        // NOTE: number of args checks: whether it's an add, remove or get, we're looking for
        // something takes arguments, and we don't want to be fooled by eg Class.getWindow()

        var funcName = verb+type;
        // look for add[type] method, e.g. addTab
        if (isc.isA.Function(target[funcName]) && 
            isc.Func.getArgs(target[funcName]).length > 0) 
        {
            return funcName;
        }

        // look for add[singular form of field name] method, e.g. addMember
        if (fieldName.endsWith("s")) {
            funcName = verb + this._withInitialCaps(fieldName.slice(0,-1));
            if (isc.isA.Function(target[funcName]) && 
                isc.Func.getArgs(target[funcName]).length > 0)
            {
                return funcName;
            }
        }
    },

    // Returns a copy of a string with the first character uppercased.
    _withInitialCaps : function (s) {
        // Uppercase the first letter, then add the rest.
        return s.substring(0,1).toLocaleUpperCase() + s.substring(1);
    },
    
    // EditMode OriginalValues
    // ---------------------------------------------------------------------------------------
    // When a component enters editMode it may change appearance or change interactive
    // behavior, for example, a Tab becomes closable via setting canClose.  However if the tab
    // is not intended to be closeable in the actual application, when we edit the tab we want
    // to show canClose as false and if the user changes the value, we want to track that they
    // have changed the value separately from it's temporary setting due to editMode.
    //
    // get/setEditableProperties allows the component to provide specialized properties to a
    // component editor, and saveTo/restoreFromOriginalValues are helpers for a component to
    // track it true, savable state from it's temporary editMode settings

    getEditableProperties : function (fieldNames) {
        var properties = {},
            undef;
        if (!this.editModeOriginalValues) this.editModeOriginalValues = {}; 
        if (!isc.isAn.Array(fieldNames)) fieldNames = [fieldNames];
        for (var i = 0; i < fieldNames.length; i++) {
            // Just in case we're passed fields rather than names
            var fieldName = isc.isAn.Object(fieldNames[i]) ? fieldNames[i].name : fieldNames[i];
            
            var value = null;
            if (this.editModeOriginalValues[fieldName] === undef) {
                this.logInfo("Field " + fieldName + " - value [" + this[fieldName] + "] is " + 
                        "coming from live values", "editModeOriginalValues");
                value = this[fieldName];
                // If this is an observation notification function, pick up the thing being observed,
                // not the notification function!
                
                if (isc.isA.Function(value) && value._isObservation) {
                    value = this[value._origMethodSlot];
                }

            } else {
                this.logInfo("Field " + fieldName + " - value [" + 
                        this.editModeOriginalValues[fieldName] + "] is coming from " + 
                        "original values", "editModeOriginalValues");
                value = this.editModeOriginalValues[fieldName];
            }
            properties[fieldName] = value;
        }
        
        return properties;
    },
    
    // called to apply properties to an object when it is edited in an EditContext (eg Visual
    // Builder) via EditContext.setNodeProperties().
    setEditableProperties : function (properties) {
        var undef;
        if (!this.editModeOriginalValues) this.editModeOriginalValues = {};
        for (var key in properties) {
            if (this.editModeOriginalValues[key] === undef) {
                this.logInfo("Field " + key + " - value is going to live values", 
                        "editModeOriginalValues");
                this.setProperty(key, properties[key]);
            } else {
                this.logInfo("Field " + key + " - value is going to original values", 
                        "editModeOriginalValues");
                this.editModeOriginalValues[key] = properties[key];
            }
        }
        this.editablePropertiesUpdated();
    },

    // called when a child object that is not itself an SC class is having properties applied
    // to it in an EditContext.  Enables cases like a ListGrid handling changes to it's
    // ListGridFields
    setChildEditableProperties : function (liveObject, properties, editNode, editContext) {
        isc.addProperties(liveObject, properties);
    },
    
    saveToOriginalValues : function (fieldNames) {
        var undef;
        if (!this.editModeOriginalValues) this.editModeOriginalValues = {};
        for (var i = 0; i < fieldNames.length; i++) {
            // Just in case we're passed fields rather than names
            var fieldName = isc.isAn.Object(fieldNames[i]) ? fieldNames[i].name : fieldNames[i];
            if (this[fieldName] === undef) {
                // We'll have to store it as explicit null, otherwise the downstream code won't
                // realize we took a copy
                this.editModeOriginalValues[fieldName] = null;
            } else {
                if (this[fieldName] && this[fieldName]._isObservation) {
                    // Pick up the original method, not the notification function set up by
                    // observation.
                    // If we ever restore the method we want to be restoring the underlying functionality
                    // and not restoring a notification function which may no longer be valid.
                    var origMethodName = isc._obsPrefix + fieldName;
                    this.editModeOriginalValues[fieldName] = this[origMethodName];
                } else {
                    this.editModeOriginalValues[fieldName] = this[fieldName];
                }
            }
        }
    },
    
    restoreFromOriginalValues : function (fieldNames) {
        var undef;
        if (!this.editModeOriginalValues) this.editModeOriginalValues = {};
        var logString = "Retrieving fields from original values:"
        var changes = {};
        for (var i = 0; i < fieldNames.length; i++) {
            // Just in case we're passed fields rather than names
            var fieldName = isc.isAn.Object(fieldNames[i]) ? fieldNames[i].name : fieldNames[i];
            if (this.editModeOriginalValues[fieldName] !== undef) {
                changes[fieldName] = this.editModeOriginalValues[fieldName];
                
                // Zap the editModeOriginalValues copy so that future queries will return 
                // the live value
                delete this.editModeOriginalValues[fieldName];
            } else {
            }
        }
        // Note use setProperties() rather than just hanging the attributes onto the live
        // widget blindly.
        // Required because:
        // - StringMethods need to be converted to live methods
        // - Observation will be left intact (setProperties/addProperties will correctly update
        //   the renamed underlying method rather than the notification method sitting in its slot)
        // - setProperties will fire propertyChanged which we use in some cases (For example
        //   to update "canDrag" when "canDragRecordsOut" is updated on a ListGrid)
        
        this.setProperties(changes);
    },
    
    propertyHasBeenEdited : function (fieldName) {
        var undef;
        if (!this.editModeOriginalValues) return false;
        // Just in case we're passed a field rather than a field name
        if (isc.isAn.Object(fieldName)) fieldName = fieldName.name;
        if (this.editModeOriginalValues[fieldName] !== undef) {
            if (isc.isA.Function(this.editModeOriginalValues[fieldName])) return false;
            if (this.editModeOriginalValues[fieldName] != this[fieldName]) return true;
        }
        return false;
    },
    
    // Override if you have a class that needs to be notified when editor properties have 
    // potentially changed
    editablePropertiesUpdated : function () {
        if (this.parentElement) this.parentElement.editablePropertiesUpdated();
    }

});



isc.DataSource.addClassMethods({

    // Given a parent object and child type, use schema to find out what field children
    // of that type are kept under
    // ---------------------------------------------------------------------------------------
    getSchema : function (object) {
        if (isc.isA.Class(object)) return object.getSchema();
        return isc.DS.get(object.schemaName || object._constructor || object.Class);
    },
    getObjectField : function (object, type) {
        if (object == null) return null;
        if (isc.isA.Class(object)) return object.getObjectField(type);

        var schema = isc.DS.getSchema(object);
        if (schema) return schema.getObjectField(type);
    },
    getSchemaField : function (object, fieldName) {
        var schema = isc.DS.getSchema(object);
        if (schema) return schema.getField(fieldName);
    },

    // Add/remove an object to another object, automatically detecting the appropriate field,
    // and calling add/remove functions if they exist on the parent
    // ---------------------------------------------------------------------------------------
    addChildObject : function (parent, newChildType, child, index, parentProperty) {
        return this._doVerbToChild(parent, "add", newChildType, child, index, parentProperty);
    },
    removeChildObject : function (parent, childType, child, parentProperty) {
        return this._doVerbToChild(parent, "remove", childType, child, parentProperty);
    },
    _doVerbToChild : function (parent, verb, childType, child, index, parentProperty) {
        var fieldName = parentProperty || isc.DS.getObjectField(parent, childType);

        if (fieldName == null) {
            this.logWarn("No field for child of type " + childType);
            return false;
        }

        this.logInfo(verb + " object " + this.echoLeaf(child) + 
                     " in field: " + fieldName +
                     " of parentObject: " + this.echoLeaf(parent), "editing");
        var field = isc.DS.getSchemaField(parent, fieldName);

        // if it's a Class, call doVerbToChild on it, which will look for a method that
        // modifies the field
        if (isc.isA.Class(parent)) {
            // if that worked, we're done
            if (parent._doVerbToChild(verb, childType, child, index, parentProperty)) return true;
        }

        // either it's not a Class, or no appropriate method was found, we'll just directly
        // manipulate the properties

        if (!field.multiple) {
            // simple field: "add" is assignment, "remove" is deletion
            if (verb == "add") parent[fieldName] = child;
            else if (verb == "remove") {
                // NOTE: null check avoids creating null slots on no-op removals
                if (parent[fieldName] != null) delete parent[fieldName];
            } else {
                this.logWarn("unrecognized verb: " + verb);
                return false;
            }
            return true;
        }

        this.logInfo("using direct Array manipulation for field '" + fieldName + "'", "editing");

        // Array field: add or remove at index
        var fieldArray = parent[fieldName];
        if (verb == "add") {
            if (fieldArray != null && !isc.isAn.Array(fieldArray)) {
                this.logWarn("unexpected field value: " + this.echoLeaf(fieldArray) +
                             " in field '" + fieldName + 
                             "' when trying to add child: " + this.echoLeaf(child));
                return false;
            }
            if (fieldArray == null) parent[fieldName] = fieldArray = [];
            if (index != null) fieldArray.addAt(child, index);
            else fieldArray.add(child);
        } else if (verb == "remove") {
            if (!isc.isAn.Array(fieldArray)) return false;
            if (index != null) fieldArray.removeAt(child, index);
            else fieldArray.remove(child);
        } else {
            this.logWarn("unrecognized verb: " + verb);
            return false;
        }

        return true;
    },

    getChildObject : function (parent, type, id, parentProperty) {
        if (isc.isA.Class(parent)) return parent.getChildObject(type, id, parentProperty);

        var fieldName = isc.DS.getObjectField(parent, type), 
            field = isc.DS.getSchemaField(parent, fieldName);


        var value = parent[fieldName];
        //this.logWarn("getting type: " + type + " from field: " + fieldName +
        //             ", value is: " + this.echoLeaf(value));
        if (!field.multiple) return value;

        if (!isc.isAn.Array(value)) return null;
        return isc.Class.getArrayItem(id, value);
    },

    // AutoId: field that should have some kind of automatically assigned ID to make the object
    // referenceable in a builder environment
    // ---------------------------------------------------------------------------------------
    getAutoIdField : function (object) {
        var schema = this.getNearestSchema(object);
        return schema ? schema.getAutoIdField() : "ID";
    },

    getAutoId : function (object) {
        var fieldName = this.getAutoIdField(object);
        return fieldName ? object[fieldName] : null;
    }
});

isc.DataSource.addMethods({
    getAutoIdField : function () {
        return this.getInheritedProperty("autoIdField") || "ID";
    },

    // In the Visual Builder, whether a component should be create()d before being added to
    // it's parent.
    // ---------------------------------------------------------------------------------------
    shouldCreateStandalone : function () {
        if (this.createStandalone != null) return this.createStandalone;
        if (!this.superDS()) return true;
        return this.superDS().shouldCreateStandalone();
    }
});


// Edit Mode impl for Buttons, Labels and Imgs
// -------------------------------------------------------------------------------------------
isc.StatefulCanvas.addProperties({
    editProxyConstructor: "StatefulCanvasEditProxy"
});


// Edit Mode impl for TabSet
// -------------------------------------------------------------------------------------------
if (isc.TabSet) {
    isc.TabSet.addClassProperties({
        addTabEditorHint: "Enter tab titles (comma separated)"
    });

    isc.TabSet.addProperties({
        editProxyConstructor:"TabSetEditProxy",
        defaultPaneConstructor:"VLayout",   // Also supported is defaultPaneDefaults

        // Override editablePropertiesUpdated() to invoke manageAddIcon() when things change.
        // For example the tab titles change causing the icon placement to be adjusted.
        editablePropertiesUpdated : function () {
            if (this.editingOn && this.editProxy) this.editProxy.delayCall("manageAddIcon");
            this.invokeSuper(isc.TabSet, "editablePropertiesUpdated");
        },

        // TODO Is this still useful. Caller does not seem to think so.
        tabScrolledIntoView : function () {
            if (!this.editingOn) return;
            for (var i = 0; i < this.tabs.length; i++) {
                var liveTab = this.getTab(this.tabs[i]);
                if (liveTab.titleEditor && liveTab.titleEditor.isVisible()) {
                    liveTab.repositionTitleEditor();
                }
            }
        }
    });

    isc.TabBar.addMethods({
        editProxyConstructor:"TabBarEditProxy"
    });
}

// Edit Mode impl for Layout
// -------------------------------------------------------------------------------------------
isc.Layout.addMethods({
    editProxyConstructor:"LayoutEditProxy"
});    


// Edit Mode impl for PortalLayout and friends
// -------------------------------------------------------------------------------------------
//
// Note that PortalLayout and friends have some special features with respect to EditMode.
//
// 1. Even in "live" mode (rather than just "edit" mode), you can drag nodes from a Palette to
//    a PortalLayout and it will do the right thing -- it will create the liveObject from the node,
//    and, if necessary, wrap it in a Portlet. Of course, you have to be in "edit" mode to edit
//    the contents of a Portlet.
//
// 2. The normal user interface of PortalLayout allows the user to adjust the number of columns,
//    move columns around, move Portlets around, etc. Even in "live" mode, the code will adjust
//    the editNodes so that they correspond to the user's actions. You can see this in 
//    Visual Builder, for instance, by creating a PortalLayout with some Portlets in "edit" mode,
//    and then switching to "live" mode and moving the Portlets around -- the editNodes will follow.
//
// In order to make this work, there are some bits of code in Portal.js that take account of
// edit mode, but the larger pieces that can be broken out separately are here.

isc.Portlet.addProperties({
    editProxyConstructor:"PortletEditProxy",

    updateEditNode : function (editContext, editNode) {
        if (editContext.persistCoordinates) {
            // We only save if the user has specified a width
            var width = this._percent_width || this._userWidth;
            if (width) {
                editContext.setNodeProperties(editNode, {
                    width: width
                }, true);
            } else {
                editContext.removeNodeProperties(editNode, "width");
            }
        }
    }
});

isc.PortalRow.addProperties({
    updateEditNode : function (editContext, editNode) {
        if (editContext.persistCoordinates) {
            // We only save if the user has specified a height
            var height = this._percent_height || this._userHeight;
            if (height) {
                editContext.setNodeProperties(editNode, {
                    height: height
                }, true);
            } else {
                editContext.removeNodeProperties(editNode, "height");
            }
        }
    },

    wrapChildNode : function (editContext, newNode, parentNode, index) {
        var liveObject = newNode.liveObject;

        if (isc.isA.Portlet(liveObject)) {
            // If it's a portlet, then we're fine
            return parentNode;
        } else {
            // If it's something else, we'll wrap it in a Portlet
            var portletNode = editContext.makeEditNode({
                type: "Portlet",
                defaults: {
                    title: newNode.title,
                    destroyOnClose: true
                }
            });

            editContext.addNode(portletNode, parentNode, index);
            return portletNode;
        }
    },

    // Called from getDropComponent to deal with drops from palettes
    handleDroppedEditNode : function (dropComponent, dropPosition) {
        var editContext = this.editContext;
        var editNode = this.editNode;

        if (isc.isA.Palette(dropComponent)) {
            // Drag and drop from palette
            var data = dropComponent.transferDragData(),
                component = (isc.isAn.Array(data) ? data[0] : data);
        
            if (editContext && editNode) {
                // If we have an editContext and editNode, just use them. The wrapping
                // is handled by wrapChildNode in this case. We return false to cancel the drop,
                // since addNode will have taken care of it.
                editContext.addNode(component, editNode, dropPosition);
                return false;
            } else {
                // If we don't have an editContext and editNode. then we'll wrap the liveObject
                // in a Portlet if necessary.
                if (isc.isA.Portlet(component.liveObject)) {
                    // If it's a Portlet, we're good
                    dropComponent = component.liveObject;
                } else {
                    // If not, we'll wrap it in one
                    dropComponent = isc.Portlet.create({
                        autoDraw: false,
                        title: component.title,
                        items: [component.liveObject],
                        destroyOnClose: true
                    });
                }
            }
        }

        return dropComponent;
    }
});

isc.PortalColumnBody.addProperties({
    
    // Called from getDropComponent to deal with drops from palettes
    handleDroppedEditNode : function (dropComponent, dropPosition) {
        var editContext = this.creator.editContext;
        var editNode = this.creator.editNode;

        if (isc.isA.Palette(dropComponent)) {
            // Drag and drop from palette
            var data = dropComponent.transferDragData(),
                component = (isc.isAn.Array(data) ? data[0] : data);

            if (editContext && editNode) {
                // If we have an editContext and editNode, just use them. The wrapping
                // is handled by wrapChildNode in this case. We return false to cancel the drop,
                // since addNode will have taken care of it.
                editContext.addNode(component, editNode, dropPosition);
                return false;
            } else {
                // If we don't have an editContext and editNode, then wrap the liveObject
                // in a Portlet if necessary.
                if (isc.isA.Portlet(component.liveObject)) {
                    // If it's a Portlet, we're good
                    dropComponent = component.liveObject;
                } else {
                    // If not, we'll wrap it in one
                    dropComponent = isc.Portlet.create({
                        autoDraw: false,
                        title: component.title,
                        items: [component.liveObject],
                        destroyOnClose: true
                    });
                }
            }
        }
     
        if (dropComponent) {
            // We need to check whether the dropComponent is already the only portlet
            // in an existing row. If so, we can simplify by just dropping
            // the row -- that is what the user will have meant. 
            var currentRow = dropComponent.portalRow;
            if (currentRow && currentRow.parentElement == this && currentRow.getMembers().length == 1) {
                // Check whether we need to adjust the editNodes
                if (editContext && editNode && currentRow.editNode) {
                    var currentIndex = this.getMemberNumber(currentRow);

                    // Check if we're not really changing position
                    if (dropPosition == currentIndex || dropPosition == currentIndex + 1) return;
                    editContext.removeNode(currentRow.editNode);
                    
                    // Adjust dropPosition if we are dropping after the currentIndex
                    if (currentIndex < dropPosition) dropPosition -= 1;
                    editContext.addNode(currentRow.editNode, editNode, dropPosition); 
                    
                    return null;
                }
            } else {
                // If we're not moving a whole current row, then we add the new portlet, creating a new row
                if (editContext && editNode && dropComponent.editNode) {
                    editContext.addNode(dropComponent.editNode, editNode, dropPosition);
                    return null;
                }
            }
        }

        // We'll get here if we're not doing something special with the dropComponent's editNode ...
        // in that case, we can return it and getDropComponent can handle it. 
        return dropComponent;
    }
});

isc.PortalColumn.addProperties({
    editProxyConstructor:"PortalColumnEditProxy",

    wrapChildNode : function (editContext, newNode, parentNode, index) {
        var liveObject = newNode.liveObject;

        if (isc.isA.PortalRow(liveObject) || newNode.type == "PortalRow") {
            // If it's a PortalRow, then we're fine
            return parentNode;
        } else if (isc.isA.Portlet(liveObject)) {
            // If it's a portlet, then we'll wrap it in a row
            var rowNode = editContext.makeEditNode({
                type: this.rowConstructor,
                defaults: {}
            });
            editContext.addNode(rowNode, parentNode, index);
            return rowNode;
        } else {
            // If it's something else, we'll wrap it in a Portlet
            var portletNode = editContext.makeEditNode({
                type: "Portlet",
                defaults: {
                    title: newNode.title,
                    destroyOnClose: true
                }
            });
            // Note that when we add the Portlet node, we'll eventually
            // get back here to wrap it in a PortalRow, so we don't need
            // to take care of that explicitly (though we could).
            editContext.addNode(portletNode, parentNode, index);
            return portletNode;
        }
    },
    
    updateEditNode : function (editContext, editNode) {
        if (editContext.persistCoordinates) {
            // We only save if the user has specified a width
            var width = this._percent_width || this._userWidth;
            if (width) {
                editContext.setNodeProperties(editNode, {
                    width: width
                }, true);
            } else {
                editContext.removeNodeProperties(editNode, "width");
            }
        }
    }
});

isc.PortalLayout.addProperties({
    // We need to do some special things when we learn of our EditContext and EditNode
    addedToEditContext : function (editContext, editNode) {
        // We may need to add our PortalColumns to the EditContext, since they may have already been created.
        for (var i = 0; i < this.getNumColumns(); i++) {
            var column = this.getPortalColumn(i);
            
            if (!column.editContext) {
                // Create the editNode, supplying the liveObject
                var node = editContext.makeEditNode({
                    type: this.columnConstructor,
                    liveObject: column, 
                    defaults: {
                        ID: column.ID,
                        _constructor: this.columnConstructor
                    }
                });
                
                // Add it to the EditContext, without adding the liveObject to the parent, since it's
                // already there.
                editContext.addNode(node, editNode, i, null, true); 
            }
        }

        // And we should change our defaults to specify numColumns: 0, because otherwise we'll
        // initialize the default 2 columns when restored, which isn't what will be wanted
        editNode.defaults.numColumns = 0;
    }
});


// Edit Mode impl for DynamicForm
// -------------------------------------------------------------------------------------------
if (isc.DynamicForm) {
    
    isc.DynamicForm.addProperties({
        editProxyConstructor:"FormEditProxy",

        setEditorType : function (item, editorType) {
            if (!item.editContext) return;

            var tree = item.editContext.getEditNodeTree(),
                parent = tree.getParent(item.editNode),
                index = tree.getChildren(parent).indexOf(item.editNode),
                ctx = item.editContext,
                paletteNode = { type: editorType, defaults: item.editNode.defaults }, 
                editNode = ctx.makeEditNode(paletteNode);
                
            ctx.removeNode(item.editNode);
            ctx.addNode(editNode, parent, index);
        }

    });

// Edit Mode extras for FormItem and its children
// -------------------------------------------------------------------------------------------

    isc.FormItem.addMethods({
        editProxyConstructor:"FormItemEditProxy",

        // Note: this impl contains code duplicated from EditProxy.setEditMode 
        // because FormItem does not extend Canvas.  
        setEditMode : function(editingOn, editContext, editNode) {
            if (editingOn == null) editingOn = true;
            if (this.editingOn == editingOn) return;
            this.editingOn = editingOn;

            if (this.editingOn) {
                this.editContext = editContext;
            }

            this.editNode = editNode;
            if (this.editingOn && !this.editProxy) {
                
                this.editProxy = this.createAutoChild("editProxy");
            }

            // Allow edit proxy to perform custom operations on edit mode change
            if (this.editProxy) this.editProxy.setEditMode(editingOn);
        },

        // FormItem proxy for DynamicForm.setEditorType
        setEditorType : function (editorType) {
            if (this.form) this.form.setEditorType(this, editorType);
        }

    });
}

// Edit Mode impl for SectionStack
// -------------------------------------------------------------------------------------------
isc.SectionStack.addMethods({
    editProxyConstructor:"SectionStackEditProxy"
}); 


// Edit Mode impl for ListGrid/TreeGrid
// -------------------------------------------------------------------------------------------
if (isc.ListGrid != null) {
    isc.ListGrid.addMethods({
        editProxyConstructor:"GridEditProxy"
    });
}

// Edit Mode impl for DrawPane/DrawItem
//-------------------------------------------------------------------------------------------
// Drawing module is optional and may not yet be loaded
isc._installDrawingEditMode = function () {
    isc.DrawPane.addMethods({
        editProxyConstructor: "DrawPaneEditProxy"
    });
    isc.DrawItem.addMethods({
        editProxyConstructor: "DrawItemEditProxy",

        // Note: this impl contains code duplicated from EditProxy.setEditMode 
        // because DrawItem does not extend Canvas.  
        setEditMode : function(editingOn, editContext, editNode) {
            if (editingOn == null) editingOn = true;
            if (this.editingOn == editingOn) return;
            this.editingOn = editingOn;

            if (this.editingOn) {
                this.editContext = editContext;
            }

            this.editNode = editNode;
            if (this.editingOn && !this.editProxy) {
                
                this.editProxy = this.createAutoChild("editProxy");
            }

            // Allow edit proxy to perform custom operations on edit mode change
            if (this.editProxy) this.editProxy.setEditMode(editingOn);
        }
    });
};

if (isc.DrawPane != null) {
    isc._installDrawingEditMode();
} else {
    // Register to receive notification when Drawing module (actually
    // any) is loaded. At that point the editMode additions can be
    // installed. This event is triggered by code automatically added
    // by FileAssembler at the end of each module.
    isc.Page.setEvent("moduleLoaded", function (target, eventInfo) {
        if (eventInfo.moduleName == "Drawing") {
            isc._installDrawingEditMode();
        }
    });
}

// Edit Mode impl for ServiceOperation and ValuesMap.  Both of these are non-visual classes
// that can nevertheless appear in a VB app - kind of like DataSources, but they're added to
// the project as a side effect of adding a web service binding.
// -------------------------------------------------------------------------------------------

isc.ServiceOperation.addMethods({
    editProxyConstructor:"EditProxy",

    getActionTargetTitle : function () {
        return "Operation: [" + this.operationName + "]";
    }
});

if (isc.ValuesManager != null) {
    isc.ValuesManager.addMethods({
        editProxyConstructor:"EditProxy"
    });
}
        

// EditNode
// ---------------------------------------------------------------------------------------

//> @object EditNode
// An object representing a component that is currently being edited within an
// +link{EditContext}.
// <P>
// An EditNode is essentially a copy of a +link{PaletteNode}, initially with the same properties
// as the PaletteNode from which it was generated.  However unlike a PaletteNode, an EditNode 
// always has a +link{editNode.liveObject,liveObject} - the object created from the 
// +link{paletteNode.defaults} or other properties defined on a paletteNode.
// <P>
// Like a Palette, an EditContext may use properties such as +link{paletteNode.icon} or 
// +link{paletteNode.title} to display EditNodes.
// <P>
// An EditContext generally offers some means of editing EditNodes and, as edits are made,
// updates +link{editNode.defaults} with the information required to re-create the component.
// 
// @inheritsFrom PaletteNode
// @treeLocation Client Reference/Tools
// @visibility external
//<

//> @attr editNode.defaults (Properties : null : IR)
// Properties required to recreate the current +link{editNode.liveObject}.
// @visibility external
//<

//> @attr editNode.type (SCClassName : null : IR)
// +link{SCClassName} of the <smartclient>+link{liveObject}</smartclient>
// <smartgwt>+link{canvasLiveObject}</smartgwt>, for example, "ListGrid".
// @visibility external
//<

//> @attr editNode.liveObject (Object : null : IR)
// Live version of the object created from the +link{editNode.defaults}.  For example, 
// if +link{editNode.type} is "ListGrid", <code>liveObject</code> would be a ListGrid.
// @visibility external
//<


//> @attr editNode.editDataSource (DataSource : null : IR)
// DataSource to use when editing the properties of this component.  Defaults to
// +link{editContext.dataSource}, or the DataSource named after the component's type.
//
// @visibility internal
//<


// EditContext
// --------------------------------------------------------------------------------------------

//> @interface EditContext
// An EditContext provides an editing environment for a set of components.
// <P>
// An EditContext is typically populated by adding a series of +link{EditNode,EditNodes} created via a
// +link{Palette}, either via drag and drop creation, or when loading from a saved version,
// via +link{EditContext.addFromPaletteNode(),addFromPaletteNode()} or 
// +link{EditContext.addPaletteNodesFromXML(),addPaletteNodesFromXML()}.
// <P>
// An EditContext then provides interfaces for further editing of the components represented
// by EditNodes.  
// 
// @group devTools
// @treeLocation Client Reference/Tools
// @visibility external
//<
isc.ClassFactory.defineInterface("EditContext");



//> @attr EditContext.editDataSource   (DataSource : null : IR)
// Default DataSource to use when editing any component in this context.  Defaults to the
// DataSource named after the component's type.  Can be overridden per-component via
// +link{editedItem.editDataSource}.
//
// @group devTools
//<

isc.EditContext.addClassProperties({
_dragHandleHeight: 18,
_dragHandleWidth: 18,
_dragHandleXOffset: -18,
_dragHandleYOffset: 0 
});

isc.EditContext.addClassMethods({

    // Title Editing (for various components: buttons, tabs, etc)
    // ---------------------------------------------------------------------------------------
    manageTitleEditor : function (targetComponent, left, width, top, height, initialValue, completionCallback) {
        if (!isc.isA.DynamicForm(this.titleEditor)) {
            // Craft the title edit field from built-in properties
            // and overrides provided by the editProxy
            var titleEditorConfig =  isc.addProperties(
                    { name: "title", type: "text", showTitle: false },
                        targetComponent.editProxy.titleEditorDefaults, 
                        targetComponent.editProxy.titleEditorProperties, {
                        keyPress : function (item, form, keyName) {
                            if (keyName == "Escape") {
                                form.discardUpdate = true;
                                form.hide();
                                if (completionCallback) completionCallback();
                                return;
                            }
                            if (keyName == "Enter") item.blurItem();
                        }, 
                        blur : function (form, item) {
                            // WWW this.logWarn("Blurring...");
                            //this.logWarn(this.getStackTrace());
                            if (!form.discardUpdate) {
                                var widget = form.targetComponent,
                                    ctx = widget.editContext;
                                if (ctx) {
                                    var value = item.getValue(),
                                        properties = (isc.isA.DrawLabel(widget) 
                                                ? {"contents": value}
                                                : (isc.isA.DrawItem(widget) || widget.showTitle 
                                                        ? {"title": value} 
                                                        : {"defaultValue": value}))
                                    ;
                                    ctx.setNodeProperties(widget.editNode, properties);
                                    if (ctx.nodeClick) ctx.nodeClick(ctx, widget.editNode);
                                }
                            }
                            form.hide();
                            if (!form.discardUpdate && completionCallback) completionCallback(item.getValue());
                        }
                    }
            );

            this.titleEditor = isc.DynamicForm.create({
                autoDraw: false,
                margin: 0, padding: 0, cellPadding: 0,
                fields: [
                    titleEditorConfig
                ]
            });
        }
        
        var editor = this.titleEditor;
        editor.setProperties({targetComponent: targetComponent});
        editor.discardUpdate = false;
        
        // Set default value of editor from component title or defaultValue
        // if no title is shown
        var item = editor.getItem("title"),
            value;
        if (initialValue) {
            value = initialValue;
        } else if (isc.isA.DrawLabel(targetComponent)) {
            value = targetComponent.contents;
        } else if (!isc.isA.DrawItem(targetComponent) && !targetComponent.showTitle) {
            value = targetComponent.defaultValue;
        } else {
            value = targetComponent.title;
            if (!value) {
                value = targetComponent.name;
            }
        }
        item.setValue(value);

        this.positionTitleEditor(targetComponent, left, width, top, height);
        
        editor.show();
        item.focusInItem();
        if (!initialValue) item.delayCall("selectValue", [], 100);
        else item.delayCall("setSelectionRange", [initialValue.length, initialValue.length]);
    },
    
    positionTitleEditor : function (targetComponent, left, width, top, height) {
        if (top == null) top = targetComponent.getPageTop();
        if (height == null) height = targetComponent.height;
        if (left == null) left = targetComponent.getPageLeft(); 
        if (width == null) width = targetComponent.getVisibleWidth();

        var editor = this.titleEditor;
        var item = editor.getItem("title");
        item.setHeight(height);
        item.setWidth(width);

        editor.setTop(top);
        editor.setLeft(left);
    },

    // Selection and Dragging of EditNodes
    // ---------------------------------------------------------------------------------------
    
    // Only called from VB (live/edit mode switch)
    setEditMode : function (editingOn) {
        var selectedComponent = isc.SelectionOutline.getSelectedObject();
        if (selectedComponent == null) return;
        
        if (editingOn) {
            this.setupDragProperties(selectedComponent);
            isc.SelectionOutline.showOutline();
            isc.SelectionOutline.showDragHandle();
        } else {
            this.resetDragProperties(selectedComponent);
            isc.SelectionOutline.hideOutline();
        }
    },
    
    // In editMode, we allow dragging the selected canvas using the drag-handle
    // This involves overriding some default behaviors at the widget level.
    // Only called from EditMode.setEditMode and EditMode.selectCanvasOrFormItem
    setupDragProperties : function (component) {
        // If component has no editProxy instance it isn't a canvas and
        // cannot be dragged.
        if (component.editProxy) {
            component.editProxy.overrideDragProperties();
        }
    },
    // Only called from EditMode.setEditMode and EditMode.selectCanvasOrFormItem
    resetDragProperties : function (component) {
        // If component has no editProxy instance it isn't a canvas and
        // cannot be dragged.
        if (component.editProxy) {
            component.editProxy.restoreDragProperties();
        }
    },

    selectCanvasOrFormItem : function (object, hideLabel) {
    
        // Make sure we're not being asked to select a non-visual object like a DataSource 
        // or ServiceOperation.  We also support the idea of a visual proxy for a non-widget
        // object - for example, ListGridFields are represented visually by the corresponding
        // button in the ListGrid header.
        if (!isc.isA.Canvas(object) && !isc.isA.FormItem(object) && !object._visualProxy) {
            return;
        }
        // Or a Menu (ie, a context menu which has no visibility until an appropriate object 
        // is right-clicked by the user)
        if (isc.isA.Menu(object)) {
            return;
        }

        if (this._dragHandle) this._dragHandle.hide();
        
        var selectedObject = isc.SelectionOutline.getSelectedObject();
        if (selectedObject) this.resetDragProperties(selectedObject);
        
        var underlyingObject,
            overrideLabel;
        if (object._visualProxy) {
            var type = object.type || object._constructor;
            overrideLabel = "[" + type + " " + (object.name ? "name:" : "ID");
            overrideLabel += object.name || object.ID;
            overrideLabel += "]"
            underlyingObject = object;
            object = object._visualProxy;
        }
        
        var editContext = underlyingObject ? underlyingObject.editContext : object.editContext;
        if (!editContext) return;
        
        // If parent component is a H/VLayout or Stack configure the highlight to
        // allow resizing of the component from along the length axis.
        var node = object.editNode,
            parentNode = editContext.getEditNodeTree().getParent(node),
            resizeFrom
        ;
        if (parentNode) {
            var parentLiveObject = parentNode.liveObject;
            if (parentLiveObject) {
                if (isc.isA.Layout(parentLiveObject)) {
                    var vertical = parentLiveObject.vertical,
                        fill = ((vertical ? parentLiveObject.vPolicy : parentLiveObject.hPolicy) == isc.Layout.FILL),
                        childCount = parentLiveObject.getMembers().length,
                        objectIndex = parentLiveObject.getMemberNumber(object),
                        lastMember = (objectIndex == (childCount-1)),
                        canResize = (!fill || !lastMember)
                    ;
                    if (canResize) {
                        resizeFrom = (vertical ? "B" : "R");
                    }
                }
            }
        }

        var vb = editContext.creator;
        if (editContext.showSelectionOutline) {
            isc.SelectionOutline.select(object, false, 
                                        !(hideLabel && vb && vb.hideLabelWhenSelecting),
                                        overrideLabel,
                                        resizeFrom);
        }
        
        // For conceptual objects that needed a visual proxy, now we've done the physical 
        // on-screen selection we need to flip the object back to the underlying one
        if (underlyingObject) object = underlyingObject;
        
        if (object.editingOn) {
            if (editContext.showSelectionOutline) {
                this.setupDragProperties(object);
                isc.SelectionOutline.showDragHandle();
            }

            var ctx = object.editContext;

            if (ctx.selectRecord) {
                ctx.deselectAllRecords();
                var ctxData = ctx.getEditNodeTree();
                if (isc.isA.Canvas(object)) {
                    // Canvas objects are created with reasonably friendly IDs that appear
                    // as visual identifiers in the component tree
                    // (Except for section header objects, that is...)
                    if (isc.isA.SectionHeader(object) || isc.isA.ImgSectionHeader(object)) {
                        ctx.selectRecord(ctxData.findById(object._ID));
                    } else {
                        ctx.selectRecord(ctxData.findById(object.ID));
                    }
                } else {
                    // FormItems have standard system-assigned IDs like isc_TextItem_1234.
                    // They are identified in the component tree by name.
                    ctx.selectRecord(ctxData.find({ID: object.name}));
                }
            }
            if (ctx.creator && ctx.creator.editComponent) ctx.creator.editComponent(object.editNode, object);
        }
    },
    
    // Only called from EditProxy and FormItemProxy
    hideAncestorDragDropLines : function (object) {
        while (object && object.parentElement) {
            if (object.parentElement.hideDragLine) object.parentElement.hideDragLine();
            if (object.parentElement.hideDropLine) object.parentElement.hideDropLine();
            object = object.parentElement;
            if (isc.isA.FormItem(object)) object = object.form;
        }
    },
    
    getSchemaInfo : function (editNode) {
        var schemaInfo = {},
            liveObject = editNode.liveObject;
            
        if (!liveObject) return schemaInfo;
            
        if (isc.isA.FormItem(liveObject)) {
            if (liveObject.form && liveObject.form.dataSource) {
                var form = liveObject.form;
                schemaInfo.dataSource = isc.DataSource.getDataSource(form.dataSource).ID;
                schemaInfo.serviceName = form.serviceName;
                schemaInfo.serviceNamespace = form.serviceNamespace;
            } else {
                schemaInfo.dataSource = liveObject.schemaDataSource;
                schemaInfo.serviceName = liveObject.serviceName;
                schemaInfo.serviceNamespace = liveObject.serviceNamespace;
            }
        } else if (isc.isA.Canvas(liveObject)) {
                schemaInfo.dataSource = isc.DataSource.getDataSource(liveObject.dataSource).ID;
                schemaInfo.serviceName = liveObject.serviceName;
                schemaInfo.serviceNamespace = liveObject.serviceNamespace;
        } else {
            // If it's not a FormItem or a Canvas, then we must presume it's a config object.
            // This can happen on drop of new components
            schemaInfo.dataSource = liveObject.schemaDataSource;
            schemaInfo.serviceName = liveObject.serviceName;
            schemaInfo.serviceNamespace = liveObject.serviceNamespace;
        }
        
        return schemaInfo;
    },

    clearSchemaProperties : function (node) {
        if (node && node.defaults && isc.isA.FormItem(node.liveObject)) {
            delete node.defaults.schemaDataSource;
            delete node.defaults.serviceName;
            delete node.defaults.serviceNamespace;
            var form = node.liveObject.form;
            if (form && form.inputSchemaDataSource &&
                isc.DataSource.get(form.inputSchemaDataSource).ID == node.defaults.inputSchemaDataSource &&
                form.inputServiceName == node.defaults.inputServiceName &&
                form.inputServiceNamespace == node.defaults.inputServiceNamespace)
            {
                delete node.defaults.inputSchemaDataSource;
                delete node.defaults.inputServiceName;
                delete node.defaults.inputServiceNamespace;
            }
        }
    },

    // XML source code generation
    // ---------------------------------------------------------------------------------------

    // serialize a set of component definitions to XML code, that is, essentially the
    // editNode.defaults portion ( { _constructor:"Something", prop1:value, ... } )
    serializeDefaults : function (defaults, indent) {
        if (defaults == null) return null;
    
        if (!isc.isAn.Array(defaults)) defaults = [defaults];

        var output = isc.SB.create();

        isc.Comm.omitXSI = true;
        for (var i = 0; i < defaults.length; i++) {
            var obj = defaults[i],
                tagName = obj._tagName,
                schema = isc.DS.getNearestSchema(obj),
                flags = { indent: indent }
            ;

            // The tag name outputted by the XML serialization will be tagName, if set.
            // Otherwise it will be the tag name implied by the schema.
            // Note that this effectively reserves the attribute name "_tagName".

            output.append(schema.xmlSerialize(obj, flags, null, tagName), "\n\n");
        }
        isc.Comm.omitXSI = null;

        return output.toString();
    },

    convertActions : function (node, defaults, classObj) { 
        // Convert actions defined as a raw object to StringMethods so they can be
        // serialized correctly.
        
        // This is a bit of a pain to achieve as there's nothing in the component's defaults 
        // that makes it clear that this is a StringMethod object rather than some other 
        // simple object and there are no dataSource field definitions for most stringMethods
        // - We could examine the registered stringMethod for the class, but this wouldn't
        //   work for non instance object fields, such as stringMethods on ListGridFields
        // - We could just examine the object - if it's a valid format (has target, name attrs)
        //   we could assume it's an action - but this would catch false positives in some 
        //   cases
        // For now - look at the value on the live-instance and see if it's a function produced
        // from an Action (check for function.iscAction).
        // This will work as long as the live-object has actually been instantiated (may not be
        // a valid test in all cases - EG anything that's lazily created on draw or when called
        // may not yet have converted it to a function).
        
        for (var field in defaults) {
            var value = defaults[field];
            // if it's not an object or is already a StringMethod no need to convert to one
            if (!isc.isAn.Object(value) || isc.isA.StringMethod(value)) continue;
            
            // If it has a specified field-type, other than StringMethod - we don't need 
            // to convert
            // Note: type Action doesn't need conversion to a StringMethod as when we serialize
            // to XML, the ActionDataSource will do the right thing
            var fieldType;
            if (classObj && classObj.getField) fieldType = classObj.getField(field).type;
            if (fieldType && (fieldType != "StringMethod")) continue;
            
            var liveValue = node.liveObject[field],
                liveAction = liveValue ? liveValue.iscAction : null,
                convertToSM
            ;
            if (liveAction) convertToSM = true;
            /*
            // We could add a sanity check that the value will convert to a function successfully
            // in case a function has been added since init or something.
            try {
                isc.Func.expressionToFunction("", defaults[field]);
            } catch (e) {
                convertToSM = false;
            }
            */
            if (convertToSM) defaults[field] = isc.StringMethod.create({value:value});
        }
        // no need to return anything we've modified the defaults object directly.
    }
});


isc.EditContext.addInterfaceProperties({
    //> @attr editContext.rootComponent    (PaletteNode : null : IR)
    // Root of data to edit.  Must contain the "type" property, with the name of a
    // valid +link{DataSource,schema} or nothing will be able to be dropped on this
    // EditContext. A "liveObject" property representing the rootComponent is also
    // suggested. Otherwise, a live object will be created from the palette node.
    // <P>
    // Can be retrieved at any time. Use +link{getRootEditNode} to retrieve the
    // +link{EditNode} created from the rootComponent. 
    //
    // @group devTools
    // @visibility external
    //<

    //> @attr editContext.showSelectionOutline (Boolean : null : IRW)
    // Should the selection outline and grab-handle be shown for selected components?
    //
    // @visibility external
    //<

    //> @attr editContext.defaultPalette (Palette : null : IRW)
    // +link{Palette} to use when an +link{EditNode} is being created directly by this EditContext,
    // instead of being created due to a user interaction with a palette (eg dragging from
    // a +link{TreePalette}, or clicking on +link{MenuPalette}).
    // <P>
    // If no defaultPalette is provided, the EditContext uses an automatically created
    // +link{HiddenPalette}.
    //
    // @visibility external
    //<
    // defaultPalette: null,

    //> @method editContext.getDefaultPalette()
    // @include editContext.defaultPalette
    // @return (Palette) the default Palette
    // @visibility external
    //<
    getDefaultPalette : function () {
        if (this.defaultPalette) return this.defaultPalette;
        return (this.defaultPalette = isc.HiddenPalette.create());
    },

    //> @method editContext.setDefaultPalette()
    // @include editContext.defaultPalette
    // @param palette (Palette) the default Palette
    // @visibility external
    //<
    setDefaultPalette : function (palette) {
        this.defaultPalette = palette;
        
        // If the palette has no defaultEditContext, then set it
        if (palette && !palette.defaultEditContext) {
            palette.defaultEditContext = this;
        }
    },

    //> @attr editContext.extraPalettes (Array of Palette : null : IRW)
    // Additional +link{Palette,Palettes} to consult for metadata when
    // deserializing +link{EditNode,Edit Nodes}. Note that the
    // +link{defaultPalette,defaultPalette} is always consulted and need not be
    // provided again here.
    //
    // @visibility external
    //<
    // extraPalettes: null,
    
    //> @attr EditContext.persistCoordinates (boolean : true : IRW)
    // If enabled, changes to a +link{EditNode.liveObject,liveObject}'s position
    // and size will be persisted to their +link{EditNode,EditNodes}.  This
    // applies to both programmatic calls and user interaction (drag reposition
    // or drag resize).
    // 
    // @visibility external
    //<
    persistCoordinates: true,

    // Finds a palette node in the defaultPalette or other palettes provided
    findPaletteNode : function (fieldName, value) {
        // Try the default palette first
        var paletteNode = this.getDefaultPalette().findPaletteNode(fieldName, value);
        if (paletteNode) return paletteNode;

        if (this.extraPalettes) {
            if (!isc.isAn.Array(this.extraPalettes)) this.extraPalettes = [this.extraPalettes];

            // If not found, try any other palettes provided
            for (var i = 0; i < this.extraPalettes.length; i++) {
                paletteNode = this.extraPalettes[i].findPaletteNode(fieldName, value);
                if (paletteNode) return paletteNode;
            }
        }

        // If not found anywhere, return null
        return null;
    },

    //> @method editContext.addNode()
    // Add a new +link{EditNode} to the EditContext, under the specified parent. If the parentNode
    // is not provided it will be determined by calling +link{getDefaultParent}.
    // <P>
    // The EditContext will interrogate the parent and new nodes to determine what field 
    // within the parent allows a child of this type, and to find a method to add the newNode's 
    // liveObject to the parentNode's liveObject.  The new relationship will then be stored
    // in the tree of EditNodes.
    // <P>
    // For example, when a Tab is dropped on a TabSet, the field TabSet.tabs is discovered as
    // the correct target field via naming conventions, and the method TabSet.addTab() is likewise 
    // discovered as the correct method to add a Tab to a TabSet.
    //
    // @param newNode (EditNode) new node to be added
    // @param [parentNode] (EditNode) parent to add the new node under.
    // @param [index] (integer) index within the parent's children array
    // @param [parentProperty] (string) the property of the liveParent to which the new node should
    //                                  be added, if not auto-discoverable from the schema
    // @param [skipParentComponentAdd] (Boolean) whether to skip adding the liveObject to the liveParent
    //                                           (default false)
    // @return newNode (EditNode) node added
    // @visibility external
    //<
    addNode : function (newNode, parentNode, index, parentProperty, skipParentComponentAdd) {
    	var iscClass = isc.ClassFactory.getClass(newNode.type);
    	if (iscClass && iscClass.isA(isc.DataSource)) {
            // If we're adding a datasource that must be loaded, then defer the addNode
            // until the datasource is loaded
            if (newNode.loadData && !newNode.isLoaded) {
    	        var self = this;
    	        var loadingNodeTree = isc._loadingNodeTree;
                newNode.loadData(newNode, function () {
                    
                    var isLoading = isc._loadingNodeTree;
                    if (!isLoading && loadingNodeTree) isc._loadingNodeTree = true;
                    self.addNode(newNode, parentNode, index, parentProperty, skipParentComponentAdd);
                    if (!isLoading && loadingNodeTree) delete isc._loadingNodeTree;
                });
                return;
            }
        }

        var data = this.getEditNodeTree();

        if (parentNode == null) parentNode = this.getDefaultParent(newNode);

        var liveParent = this.getLiveObject(parentNode);
        this.logInfo("addNode will add newNode of type: " + newNode.type +
                     " to: " + this.echoLeaf(liveParent), "editing");

        if (liveParent.wrapChildNode) {
            parentNode = liveParent.wrapChildNode(this, newNode, parentNode, index);
            if (!parentNode) return;
            liveParent = this.getLiveObject(parentNode);
        }

        // find what field in the parent can accommodate a child of this type (prefer the 
        // passed-in name over a looked-up one, so the user can override in the case of 
        // multiple valid parent fields)
        var fieldName = parentProperty || isc.DS.getObjectField(liveParent, newNode.type);
        var field = isc.DS.getSchemaField(liveParent, fieldName);

        if (!field) {
            this.logWarn("can't addNode: can't find a field in parent: " + liveParent +
                         " for a new child of type: " + newNode.type + ", parent property:" + fieldName + 
                         ", newNode is: " +
                         this.echo(newNode));
            return;
        }

        // for a singular field (eg listGrid.dataSource), remove the old node first
        if (!field.multiple) {
            var existingChild = isc.DS.getChildObject(liveParent, newNode.type, parentProperty);
            if (existingChild) {
                var existingChildNode = data.getChildren(parentNode).find("ID", isc.DS.getAutoId(existingChild));
                this.logWarn("destroying existing child: " + this.echoLeaf(existingChild) +
                             " in singular field: " + fieldName);
                data.remove(existingChildNode);
                if (isc.isA.Class(existingChild) && !isc.isA.DataSource(existingChild)) existingChild.destroy();
            }
        }

        // NOTE: generated components and remove/add cycles: some widgets convert config
        // objects into live objects (eg formItem properties to live FormItem, tab -> ImgTab,
        // section -> SectionHeader, etc).  When we are doing an add/remove cycle for these
        // kinds of generated objects:
        // - rebuild based on defaults, rather than trying to re-add the liveObject, which will
        //   be a generated component that the parent will have destroyed
        // - preserve Canvas children of the generated component, such as tab.pane,
        //   section.items, which have not been added to the defaults.  We do this by using
        //   part of the serialization logic (serializeChildData)
        // - ensure removal of the tab, item, or section does not destroy these Canvas children
        //   (a special flag is passed to at least TabSets to avoid this)

        // Optimization for add/remove cycles: check for methods like "reorderMember" first.
        // Note this doesn't remove the complexity discussed above because a generated
        // component might be moved between two parents.
        var childObject;
        if (newNode.generatedType) {
            // copy to avoid property scribbling that is currently done by TabSets and
            // SectionStacks at least
            childObject = isc.addProperties({}, newNode.defaults);
            this.serializeChildData(childObject, data.getChildren(newNode));
            newNode.liveObject = childObject;
        } else {
            childObject = newNode.liveObject;
        }

        // Let the liveObject know about the editContext and editNode. We used
        // to do this for some objects in addedToEditContext, but that isn't
        // sufficient for liveObjects that are in fact config blocks (since they
        // don't have the callback).
        childObject.editContext = this;
        childObject.editNode = newNode;

        if (!skipParentComponentAdd) {
            var result = isc.DS.addChildObject(liveParent, newNode.type, childObject, index, parentProperty);
            if (!result) {
                this.logWarn("addChildObject failed, returning");
                return;
            }
        }

        // fetch the liveObject back from the parent to handle it's possible conversion from
        // just properties to a live instance.
        // NOTE: fetch object by ID, not index, since on a reorder when a node is dropped after
        // itself the index is one too high
        if (!newNode.liveObject) newNode.liveObject = isc.DS.getChildObject(liveParent, newNode.type, 
                                                                            isc.DS.getAutoId(newNode.defaults), parentProperty);

        this.logDebug("for new node: " + this.echoLeaf(newNode) + 
                      " liveObject is now: " + this.echoLeaf(newNode.liveObject),
                      "editing");

        if (newNode.liveObject == null) {
            this.logWarn("wasn't able to retrieve live object after adding node of type: " +
                         newNode.type + " to liveParent: " + liveParent + 
                         ", does liveParent have an appropriate getter() method?");
        }

        // add the node representing the component to the project tree
        data.add(newNode, parentNode, index);
        // gets rid of the spurious opener icon that appears because all nodes are regarded as
        // folders and dropped node is unloaded, hence might have children
        data.openFolder(newNode);

        this.logInfo("added node " + this.echoLeaf(newNode) + 
                     " to EditTree at path: " + data.getPath(newNode) + 
                     " with live object: " + this.echoLeaf(newNode.liveObject), "editing");

        // Call hook in case the EditContext wants to do further processing ... useful to avoid
        // problem with calling Super with an interface method
        if (this.nodeAdded) this.nodeAdded(newNode, parentNode, data.getRoot());

        // Call hook in case the live object wants to know about being added
        if (newNode.liveObject.addedToEditContext) newNode.liveObject.addedToEditContext(this, newNode, parentNode, index);

        if (this.isNodeEditingOn(newNode) && newNode.liveObject.editProxy.enableComponentSelection) {
            // Hang on to the liveObject that manages the selection UI.
            // It is responsible for showing the outline or other selected state
            this._selectionLiveObject = newNode.liveObject;
        }

        return newNode;    
    },

    //>!BackCompat 2011.06.25
    addComponent : function (newNode, parentNode, index, parentProperty, skipParentComponentAdd) {
        return this.addNode(newNode, parentNode, index, parentProperty, skipParentComponentAdd);
    },
    //<!BackCompat

    //> @method editContext.getRootEditNode()
    // Returns the root +link{EditNode} of the EditContext typically created from +link{rootComponent}.
    //
    // @return (EditNode) the root EditNode
    // @visibility external
    //<
    getRootEditNode : function () {
        return (this.getEditNodeTree() ? this.getEditNodeTree().getRoot() : null);
    },

    //> @method editContext.reorderNode()
    // Moves an +link{EditNode} from one child index to another in the EditContext under the specified parent.
    // <P>
    // No changes are made to the live objects.
    //
    // @param parentNode (EditNode) parent to reorder child nodes
    // @param index (integer) index within the parent's children array to be moved
    // @param moveToIndex (integer) index within the parent's children array at which to place moved node
    // @visibility devTools
    //<
    reorderNode : function (parentNode, index, moveToIndex) {
        var data = this.getEditNodeTree();

        // Locate child node that has moved
        var childNode = data.getChildren(parentNode).get(index);

        // Remove the child node from the tree and insert it back at the new location
        data.remove(childNode);
        data.add(childNode, parentNode, moveToIndex);
    },

    //> @method editContext.nodeAdded()
    // Notification fired when an +link{EditNode} has been added to the EditContext
    //
    // @param newNode (EditNode) node that was added
    // @param parentNode (EditNode) parent node of the node that was added
    // @param rootNode (EditNode) root node of the edit context
    // @visibility external
    //<
    // Empty function in case someone wants to observe.
    nodeAdded : function (newNode, parentNode, rootNode) {},

    //> @method editContext.getDefaultParent()
    // Returns the default parent +link{EditNode} to be used when a new
    // EditNode is added to the EditContext without a specified parent. This
    // commonly occurs when a paletteNode is double-clicked in a palette.
    // <p>
    // The default implementation returns the root editNode (see
    // +link{getRootEditNode}).
    //
    // @param newNode (EditNode) node that was added
    // @param returnNullIfNoSuitableParent (boolean) should null be returned if no parent is found?
    // @return (EditNode) the default parent EditNode 
    // @visibility external
    //<
    getDefaultParent : function (newNode, returnNullIfNoSuitableParent) {
        return this.getRootEditNode();
    },

    //> @method editContext.addFromPaletteNode()
    // Creates a new EditNode from a PaletteNode, using the
    // +link{defaultPalette}.  If you have an array of possibly inter-related
    // PaletteNodes, then you should use
    // +link{addFromPaletteNodes(),addFromPaletteNodes()} on the array instead,
    // in order to preserve the relationships.
    //
    // @param paletteNode (PaletteNode) the palette node to use to create the new node
    // @param [parentNode] (EditNode) optional the parent node if the new node should appear
    //                                under a specific parent
    // @return (EditNode) the EditNode created from the paletteNode
    // @see addFromPaletteNodes()
    // @visibility external
    //< 
    addFromPaletteNode : function (paletteNode, parentNode) {
        
        var editNode = this.makeEditNode(paletteNode, parentNode);
        return this.addNode(editNode, parentNode);    
    },
    
    //> @method editContext.makeEditNode()
    // Creates and returns an EditNode using the +link{defaultPalette}.  Does not add the newly
    // created EditNode to the EditContext.
    // 
    // @param paletteNode (PaletteNode) the palette node to use to create the new node
    // @return (EditNode) the EditNode created from the paletteNode
    // @visibility external
    //<
    makeEditNode : function (paletteNode) {
        var palette = this.getDefaultPalette();
        return palette.makeEditNode(paletteNode);
    },

    // alternative to just using node.liveObject
    // exists because forms used to rebuild *all* items when any single item is added, hence
    // making the liveObject stale for siblings of an added item
    getLiveObject : function (node) {
        var data = this.getEditNodeTree();
        var parentNode = data.getParent(node);

        // at root, just use the cached liveObject (a formItem can never be at root)
        if (parentNode == null) {
            return node.liveObject; 
        }

        
        var liveParent = parentNode.liveObject;
        var liveObject = isc.DS.getChildObject(liveParent, node.type, isc.DS.getAutoId(node));
                                  
        if (liveObject) node.liveObject = liveObject;
        return node.liveObject;
    },

    // wizard handling
    requestLiveObject : function (newNode, callback, palette) {
        var _this = this;

        // handle deferred nodes (don't load or create their liveObject until they are actually
        // added).  NOTE: arguably the palette should handle this, and makeEditNode()
        // should be asynchronous in this case.
        if (newNode.loadData && !newNode.isLoaded) {
            newNode.loadData(newNode, function (loadedNode) {
                loadedNode = loadedNode || newNode
                loadedNode.isLoaded = true;
                // preserve the "dropped" flag
                loadedNode.dropped = newNode.dropped;
                _this.fireCallback(callback, "node", [loadedNode]);
            }, palette);
            return;
        }

        if (newNode.wizardConstructor) {
            this.logInfo("creating wizard with constructor: " + newNode.wizardConstructor);
            var wizard = isc.ClassFactory.newInstance(newNode.wizardConstructor,
                                                      newNode.wizardDefaults);
            // ask the wizard to go through whatever steps 
            wizard.getResults(newNode, function (results) {
                // accept either a paletteNode or editNode (detect via liveObject)
                if (!results.liveObject) {
                    results = palette.makeEditNode(results);
                }
                _this.fireCallback(callback, "node", [results]);
            }, palette);
            return;
        }

        this.fireCallback(callback, "node", [newNode]);
    },

    // Gets the tree of editNodes being edited by this editContext. We create the tree
    // here, since the interface can't do it in initWidget.
    getEditNodeTree : function () {
        if (!this.editNodeTree) {
            // NOTE: there really is no reasonable default for rootComponent, since its type
            // determines what can be dropped.  This default will create a tree that won't accept
            // any drops, but won't JS error.
            var rootComponent = isc.addProperties({}, this.rootComponent || { type: "Object" }),
                rootLiveObject = this.rootLiveObject || rootComponent
            ;
            if (!rootComponent) rootComponent = { type: "Object" };

            //>!BackCompat 2013.12.30
            if (!rootComponent.type) {
                rootComponent.type = (isc.isA.Class(rootComponent) ? rootComponent.Class : rootComponent._constructor);
            }
            if (rootLiveObject && !rootComponent.liveObject) rootComponent.liveObject = rootLiveObject;
            //<!BackCompat 2013.12.30

            var rootNode = this.makeEditNode(rootComponent);
            
            this.editNodeTree = isc.Tree.create({
                idField:"ID",
                root : rootNode,
                // HACK: so that all nodes can be targetted for D&D
                isFolder : function () { return true; }
            });
        }

        return this.editNodeTree;
    },

    getEditNodeArray : function () {
        return this.getEditNodeTree().getAllNodes();
    },

    //>!BackCompat 2011.06.25 
    getEditComponents : function () {
        return this.getEditNodeArray();
    },
    //<!BackCompat

    // tests whether the targetNode can accept a newNode of type "type"
    canAddToParent : function (targetNode, type) {
        var liveObject = targetNode.liveObject;
        if (isc.isA.Class(liveObject)) {
            return (liveObject.getObjectField(type) != null);
        }
        // still required for MenuItems and ListGridFields, where the live object is not a Class
        return (isc.DS.getObjectField(targetNode, type) != null);
    },

    //> @method EditContext.removeAll()
    // Removes all +link{EditNode,EditNodes} from the EditContext, but does not destroy 
    // the +link{EditNode.liveObject,liveObjects}.
    // @visibility external
    //<
    removeAll : function () {
        var data = this.getEditNodeTree();
        var rootChildren = data.getChildren(data.getRoot()).duplicate();
        for (var i = 0; i < rootChildren.length; i++) {
            this.removeNode(rootChildren[i]);
        }
    },

    //> @method EditContext.destroyAll()
    // Removes all +link{EditNode,EditNodes} from the EditContext, and calls
    // +link{Canvas.destroy(),destroy()} on the
    // +link{EditNode.liveObject,liveObjects}.
    // @visibility external
    //<
    destroyAll : function () {
        // Make sure nothing is selected
        this.deselectAllComponents();
        var data = this.getEditNodeTree();
        var rootChildren = data.getChildren(data.getRoot()).duplicate();
        for (var i = 0; i < rootChildren.length; i++) {
            this.destroyNode(rootChildren[i]);
        }
    },

    //> @method EditContext.removeNode()
    // Removes +link{EditNode,EditNode} from the EditContext. The editNode
    // liveObject is not destroyed.
    // @param editNode (EditNode) node to be removed
    // @visibility external
    //<
    removeNode : function (editNode, skipLiveRemoval) {
        var data = this.getEditNodeTree();
        
        // remove the corresponding component from the object model
        var parentNode = data.getParent(editNode);
        var liveChild = this.getLiveObject(editNode);
        var liveParent = this.getLiveObject(parentNode);

        // If editNode is part of editMode component selection
        // deselect it now
        if (this.isComponentSelected(liveChild)) this.deselectComponents(liveChild);

        // remove the node from the tree
        data.remove(editNode);

        if (skipLiveRemoval) return;
        
        if (liveParent && liveChild) {

            //this.logWarn("removing with defaults: " + this.echo(editNode.defaults));

            isc.DS.removeChildObject(liveParent, editNode.type, liveChild);
        }
    },

    //>!BackCompat 2011.06.25
    removeComponent : function (editNode, skipLiveRemoval) { // old name
        return this.removeNode(editNode, skipLiveRemoval);
    },
    //<!BackCompat

    // destroy an editNode in the tree, including it's liveObject
    destroyNode : function (editNode) {
        var liveObject = this.getLiveObject(editNode);
        this.removeNode(editNode);
        // if it has a destroy function, call it.  Otherwise we assume garbage collection will
        // work
        if (liveObject.destroy) liveObject.destroy();
    },
    
    //>!BackCompat 2011.06.25
    destroyComponent : function (editNode) { // old name
        return this.destroyNode(editNode);
    },
    //<!BackCompat

    // EditFields : optional lists of fields that can be edited in an EditContext
    // ---------------------------------------------------------------------------------------

    getEditDataSource : function (canvas) {
        return isc.DataSource.getDataSource(canvas.editDataSource || canvas.Class || 
                                            this.editDataSource);
    },

    // fields to edit:
    // - application-specific: two different editing applications may edit the same type of
    //   component (eg a ListViewer) exposing different sets of properties
    //   - the DataSource may not even represent the full set of properties, but regardless,
    //     can act as a default list of fields and reference properties for those fields
    // - on an application-specific basis, should be able to have a base set of fields, plus
    //   additions
    
    // get list of editable fields for a component.  May be a mix of string field names and
    // field objects
    _getEditFields : function (canvas) {
        // combine the baseEditFields and editFields properties
        var fields = [];
        fields.addList(canvas.baseEditFields);
        
        fields.addList(canvas.editFields);
 
        // HACK: set any explicitly specified fields to be visible, since many fields in the
        // current widget DataSources are set to visible=false to suppress them in editing
        // demos.  If a field is explicitly specified in editFields, we want it to be shown
        // unless they've set "visible" explicitly
        for (var i = 0; i < fields.length; i++) {
            var field = fields[i];
            if (field.visible == null) field.visible = true;
        }

        // if this is an empty list, take all the fields from the DataSource
        if (fields.length == 0) {
            fields = this.getEditDataSource(canvas).getFields();
            fields = isc.getValues(fields);
        }
        return fields;
    },
    
    // get the list of editable fields as an Array of Strings
    getEditFieldsList : function (canvas) {
        var fieldList = [],
            fields = this._getEditFields(canvas);
        // return just the name for any fields specified as objects
        for (var i = 0; i < fields.length; i++) {
            var field = fields[i];
            if (isc.isAn.Object(field)) {
                fieldList.add(field.name);
            } else {
                fieldList.add(field);
            }
        }
        return fieldList;
    },

    // get the edit fields, suitable for passing as "fields" to a dataBinding-aware component
    getEditFields : function (canvas) {
        var fields = this._getEditFields(canvas);
        // make any string fields into objects
        for (var i = 0; i < fields.length; i++) {
            var field = fields[i];
            if (isc.isA.String(field)) field = {name:field};
            // same hack as above to ensure visibility of explicitly specified fields, for
            // fields specified as just Strings
            if (field.visible == null) field.visible = true;
            fields[i] = field;
        }

        return fields;
    },

    // Serializing
    // --------------------------------------------------------------------------------------------
    // Take a tree of editNodes and produce a data structure that can be serialized to produce
    // actual XML or JSON source code

    
    
    //>!BackCompat 2013.09.27
    serializeComponents : function (serverless, includeRoot) {
        return this.serializeAllEditNodes({ serverless: serverless }, includeRoot);
    },
    //<!BackCompat

    //> @object SerializationSettings
    // Settings to control +link{EditContext} serialization.
    //
    // @group devTools
    // @treeLocation Client Reference/Tools
    // @visibility external
    //<

    //> @attr serializationSettings.serverless (Boolean : null : IR)
    // When true specify DataSources in full rather than assuming they can be
    // downloaded from the server.
    // @visibility external
    //<

    //> @attr serializationSettings.indent (Boolean : null : IR)
    // Overrides the default indention setting during serialization. XML defaults
    // to indented and JSON defaults to non-indented.
    // @visibility external
    //<

    //> @attr serializationSettings.outputComponentsIndividually (Boolean : true : IR)
    // Overrides the default component output setting during serialization. By default
    // Canvas and DrawItem components are serialized individually and referenced by their
    // containers.
    // @visibility external
    //<
    
    //> @method editContext.serializeAllEditNodes()
    // Serialize the tree of +link{EditNode,EditNodes} to an XML representation
    // of +link{PaletteNode,PaletteNodes}. The result can be supplied to 
    // +link{addPaletteNodesFromXML(),addPaletteNodesFromXML()} to recreate
    // the EditNodes.
    //
    // @param [settings] (SerializationSettings) Additional serialization settings
    // @return (String) an XML representation of PaletteNodes which can be used to
    //                  recreate the tree of EditNodes.
    // @see addPaletteNodesFromXML
    // @visibility external
    //<
    serializeAllEditNodes : function (settings, includeRoot) {
        // we flatten the Tree of objects into a flat list of top-level items
        // to serialize.  Nesting (eg grid within Layout) is accomplished by
        // having the Layout refer to the grid's ID.
        var data = this.getEditNodeTree();
        var nodes = includeRoot ? [data.root] : data.getChildren(data.root).duplicate();
        var value = this.serializeEditNodes(nodes, settings);
        return value;
    },

    //> @method editContext.serializeAllEditNodesAsJSON()
    // Encode the tree of +link{EditNode,EditNodes} to a JSON representation
    // of +link{PaletteNode,PaletteNodes}. The result can be supplied to 
    // +link{addPaletteNodesFromJSON(),addPaletteNodesFromJSON()} to recreate
    // the EditNodes.
    //
    // @param [settings] (SerializationSettings) Additional serialization settings
    // @return (String) a JSON representation of PaletteNodes which can be used to
    //                  recreate the tree of EditNodes.
    // @see addPaletteNodesFromJSON
    // @visibility external
    //<
    serializeAllEditNodesAsJSON : function (settings, includeRoot) {
        // we flatten the Tree of objects into a flat list of top-level items
        // to serialize.  Nesting (eg grid within Layout) is accomplished by
        // having the Layout refer to the grid's ID.
        var data = this.getEditNodeTree();
        var nodes = includeRoot ? [data.root] : data.getChildren(data.root).duplicate();
        return this.serializeEditNodesAsJSON(nodes, settings);
    },

    //> @method editContext.serializeEditNodes()
    // Serialize the provided +link{EditNode,EditNodes} to an XML
    // representation of +link{PaletteNode,PaletteNodes}. Note that the
    // EditNodes must have been added to this EditContext. The result can be
    // supplied to +link{addPaletteNodesFromXML(),addPaletteNodesFromXML()} to
    // recreate the EditNodes.
    //
    // @param nodes (Array of EditNode) EditNodes to be serialized 
    // @param [settings] (SerializationSettings) Additional serialization settings
    // @return (String) an XML representtion of the provided EditNodes
    // @visibility external
    //<
    // NOTE: the "nodes" passed to this function need to be part of the Tree that's available
    // as this.getEditNodeTree().  TODO: generalized this so that it takes a Tree, optional nodes, and
    // various mode flags like serverless.
    serializeEditNodes : function (nodes, settings) {
        if (!nodes) return null;

        return this._serializeEditNodes(nodes, settings);
    },

    //> @method editContext.serializeEditNodesAsJSON()
    // Serialize the provided +link{EditNode,EditNodes} to a JSON
    // representation of +link{PaletteNode,PaletteNodes}. Note that the
    // EditNodes must have been added to this EditContext. The result can be
    // supplied to +link{addPaletteNodesFromJSON(),addPaletteNodesFromJSON()} to
    // recreate the EditNodes.
    //
    // @param nodes (Array of EditNode) EditNodes to be serialized 
    // @param [settings] (SerializationSettings) Additional serialization settings
    // @return (String) a JSON representtion of the provided EditNodes
    // @visibility external
    //<
    serializeEditNodesAsJSON : function (nodes, settings) {
        if (!nodes) return null;

        return this._serializeEditNodes(nodes, settings, "json");
    },

    _serializeEditNodes : function (nodes, settings, format) {        
        if (!isc.isAn.Array(nodes)) nodes = [nodes];

        // add autoDraw to all non-hidden top-level components
        for (var i = 0; i < nodes.length; i++) {
            var node = nodes[i] = isc.addProperties({}, nodes[i]),
                iscClass = isc.ClassFactory.getClass(node.type),
                defaults = node.defaults = isc.addProperties({}, node.defaults);
    
            //this.logWarn("considering node: " + this.echo(topNode) +
            //             " with defaults: " + this.echo(defaults));
            if (iscClass && iscClass.isA("Canvas") && defaults && 
                defaults.visibility != isc.Canvas.HIDDEN && defaults.autoDraw !== false) 
            {
                defaults.autoDraw = true;
            }
        }

        // if serverless is set we will actually output DataSources in their entirety.
        // Otherwise, we'll just output a special tag that causes the DataSource to be loaded
        // as the server processes the XML format.
        this.serverless = (settings ? settings.serverless : null);

        // outputComponentsIndividually is documented to default to true.
        // That default is applied here.
        this.outputComponentsIndividually = (settings && settings.outputComponentsIndividually != null ? settings.outputComponentsIndividually : true);

        this.defaultsBlocks = [];
        this.map("getSerializeableTree", nodes);

        this.outputComponentsIndividually = null;
        this.serverless = null;

        var jsonEncodeSettings = { prettyPrint: this.indent },
            indent = (settings ? settings.indent : (format == "json" ? false : true))
        ;
        var result = (format == "json" ? isc.JSON.encode(this.defaultsBlocks, jsonEncodeSettings) : isc.EditContext.serializeDefaults(this.defaultsBlocks, indent));

        return result;
    },

    // arrange the initialization data into a structure suitable for XML serialization.  This
    // will:
    // - grab just the defaults portion of each editNode (what we serialize)
    // - flatten hierarchies: all Canvas-derived components will be at top-level,
    //   members/children arrays will contain references to these children
    // - ensure DataSources are only listed once since multiple components may refer to the
    //   same DataSource
    getSerializeableTree : function (node, dontAddGlobally) {
        // Give the liveObject a chance to update the editNode
        var liveObject = node.liveObject;
        if (liveObject && liveObject.updateEditNode && liveObject.editContext && liveObject.editNode) {
            node.liveObject.updateEditNode(node.liveObject.editContext, node.liveObject.editNode);
        }
        
        var type = node.type,
            // copy defaults for possible modification
            defaults = isc.addProperties({}, node.defaults);   
        // if this node is a DataSource (or subclass of DataSource)
        var classObj = isc.ClassFactory.getClass(type);

        this.logInfo("node: " + this.echoLeaf(node) + " with type: " + type, "editing");

        if (classObj && classObj.isA("DataSource") && !classObj.isA("MockDataSource")) {
            // check for this same DataSource already being saved out
            if (this.defaultsBlocks) {
                var existingDS = this.defaultsBlocks.find("ID", defaults.ID) ||
                                 this.defaultsBlocks.find("loadID", defaults.ID);
                if (existingDS && existingDS.$schemaId == "DataSource") return;
            }

            if (!this.serverless) {
                // when serializing a DataSource, just output the loadID tag so that the
                // server outputs the full definition during XML processing on JSP load
                defaults = {
                    _constructor: "DataSource",
                    $schemaId: "DataSource",
                    loadID: defaults.ID
                };
            } else {
                // if running serverless, we can't rely on the server to fetch the definition
                // as part of XML processing during JSP load, so we have to write out a full
                // definition.  This works only for DataSources that don't require the server
                // to fetch and update data.
                // NOTE: since all DataSources in Visual Builder are always saved to the
                // server, an alternative approach would be to load the DataSource and capture
                // its defaults, as we do when we edit an existing DataSource.  However we
                // would still depend on getSerializeableFields() being correct, as we also use
                // it to obtain clean data when we begin editing a dynamically created
                // DataSource obtained from XML Schema (eg SFDataSource)
                var liveDS = node.liveObject;
                defaults = liveDS.getSerializeableFields();
                defaults._constructor = liveDS.Class;
                defaults.$schemaId = "DataSource";
            }
        }

        // A DrawItem can have a fillGradient property. It can either be a reference to a
        // gradient defined in the DrawPane (String) or a Gradient object. During serialization
        // a reference must be serialized as ref="xxx".
        if (isc.isA.DrawItem(liveObject) && defaults.fillGradient != null && isc.isA.String(defaults.fillGradient)) {
            defaults.fillGradient = "ref:" + defaults.fillGradient;
        }

        // Actions
        // By default these will be defined as simple objects in JS, but for saving in XML 
        // we need to enclose them in <Action>...</Action> tags
        // (ensures that any specified mappings are rendered out as an array)
        // Catch these cases and store as a StringMethod object rather than the raw action
        // object - this will serialize correctly.
        isc.EditContext.convertActions(node, defaults, classObj);
        
        var treeChildren = this.getEditNodeTree().getChildren(node);
        if (!treeChildren) {
            if (this.defaultsBlocks) this.defaultsBlocks.add(defaults); // add as a top-level node
            return;
        }

        this.serializeChildData(defaults, treeChildren);
            
        // if we're not supposed to be global, return out defaults
        if (dontAddGlobally) return defaults;
        // otherwise add this node's data globally (we list top-most parents last)
        if (this.defaultsBlocks) this.defaultsBlocks.add(defaults);
    },

    //>!BackCompat 2013.09.25
    addChildData : function (parentData, childNodes) {
        return this.serializeChildData(parentData, childNodes);
    },
    //<!BackCompat

    serializeChildData : function (parentData, childNodes) {
        var ds = isc.DS.get(parentData._constructor);
        for (var i = 0; i < childNodes.length; i++) {
            var child = childNodes[i],
                childType = child.defaults._constructor,
                // copy defaults for possible modification
                childData = isc.addProperties({}, child.defaults),
                parentFieldName = childData.parentProperty || ds.getObjectField(childType),
                parentField = ds.getField(parentFieldName);

            this.logInfo("serializing: child of type: " + childType + 
                         " goes in parent field: " + parentFieldName,
                         "editing");

            // All Canvii and DrawItems can be output individually and their parents reference
            // them by ID. Alternately these child components can be output inline. Components
            // marked with _generated:true, which includes TabSet tabs and SectionStack sections,
            // are never output individually.
            //
            // DataSources are always output individually and referenced by ID.
            var isIndividualComponent = (
                    (isc.isA.Canvas(child.liveObject) || isc.isA.DrawItem(child.liveObject)) && 
                    !child.liveObject._generated);

            if ((this.outputComponentsIndividually && isIndividualComponent) || isc.isA.DataSource(child.liveObject)) {
                if (isc.isA.DataSource(child.liveObject) && parentFieldName == "dataSource") {
                    // Don't add the "ref:" if the parentFieldName is "dataSource", since
                    // the dataSource field always takes a String ID. (The "ref:" used
                    // to be stripped off later, so just don't add it).
                    childData = childData.ID;
                } else {
                    childData = "ref:" + childData.ID;
                }
                this.getSerializeableTree(child);
            } else {
                // otherwise, serialize this child without adding it globally
                childData = this.getSerializeableTree(child, true);
            }

            var existingValue = parentData[parentFieldName];
            if (parentField.multiple) {
                // force multiple fields to Arrays
                if (!existingValue) existingValue = parentData[parentFieldName] = [];
                existingValue.add(childData);
            } else {
                parentData[parentFieldName] = childData;
            }
        }
    },
   
    //>!BackCompat 2013.09.25
    serializeEditComponents : function () {
        return this.serializeLiveObjects();
    },
    //<!BackCompat

    // get serializable data as an Array of Objects for the editNodes in this context, via
    // getting properties from the liveObjects and stripping it down to editFields (fields that
    // are allowed to be edited in the context), or the DataSource fields if no editFields were
    // declared.
    
    serializeLiveObjects : function () {
        // get all the widgets being edited
        var widgets = this.getEditNodeArray(),
            output = [];

        if (!widgets) return [];

        for (var i = 0; i < widgets.length; i++) {
            var child = widgets[i].liveObject,
                // get all properties that don't have default value
                props = child.getUniqueProperties(),
                editFields = this.getEditFieldsList(child);

            // add in the Class, which will be needed to recreate the widget, but which could never
            // have non-default value
            props._constructor = child.Class;

            // limit the data to just the fields listed in the DataSource
            props = isc.applyMask(props, editFields);
            
            output.add(props);
        } 
        return output;
    },

    //>!BackCompat 2013.09.25
    loadNodeTreeFromXML : function (xmlString) {
        this.addPaletteNodesFromXML(xmlString);
    },
    //<!BackCompat 2013.09.25

    //> @method editContext.addPaletteNodesFromXML()
    // Recreate +link{EditNode,EditNodes} from an XML representation of 
    // +link{PaletteNode,PaletteNodes} (possibly created by calling
    // +link{serializeAllEditNodes()} or +link{serializeEditNodes()}.
    // <P>
    // By default, components that have +link{Canvas.ID,global IDs} will not
    // actually be allowed to take those global IDs - instead, only widgets that have one of the
    // global IDs passed as the <code>globals</code> parameter will actually receive their global
    // IDs.  To override this behavior, pass the special value +link{RPCManager.ALL_GLOBALS}
    // for the <code>globals</code> parameter.
    //
    // @param xmlString (String) XML string
    // @param [parentNode] (EditNode) parent node (defaults to the root)
    // @param [globals] (Array of String) widgets to allow to take their global IDs
    // @see serializeAllEditNodes()
    // @see serializeEditNodes()
    // @visibility external
    //<
    addPaletteNodesFromXML : function (xmlString, parentNode, globals, callback) {
        var self = this;

        //isc.logWarn(isc.echo(xmlString));
        
        isc.DMI.callBuiltin({
            methodName: "xmlToJS",
            arguments: [xmlString],
            callback: function (rpcResponse) {
                self.addPaletteNodesFromJS(rpcResponse.data, parentNode, globals, callback);
            }
        });
    },

    //> @method editContext.addPaletteNodesFromJSON()
    // Recreate +link{EditNode,EditNodes} from a JSON representation of 
    // +link{PaletteNode,PaletteNodes} (possibly created by calling
    // +link{serializeAllEditNodesAsJSON()} or +link{serializeEditNodesAsJSON()}.
    // <P>
    // By default, components that have +link{Canvas.ID,global IDs} will not
    // actually be allowed to take those global IDs - instead, only widgets that have one of the
    // global IDs passed as the <code>globals</code> parameter will actually receive their global
    // IDs.  To override this behavior, pass the special value +link{RPCManager.ALL_GLOBALS}
    // for the <code>globals</code> parameter.
    //
    // @param jsonString (String) JSON string
    // @param [parentNode] (EditNode) parent node (defaults to the root)
    // @param [globals] (Array of String) widgets to allow to take their global IDs
    // @see serializeAllEditNodesAsJSON()
    // @see serializeEditNodesAsJSON()
    // @visibility external
    //<
    addPaletteNodesFromJSON : function (jsonString, parentNode, globals, callback) {
        if (globals == null) globals = [];
        else if (!isc.isAn.Array(globals)) globals = [globals];

        isc.captureDefaults = true;

        var jsClassDefs = isc.JSON.decode(jsonString);

        
        var keepAllGlobals = (globals.length == 1 && globals[0] == isc.RPC.ALL_GLOBALS);
        for (var i = 0; i < jsClassDefs.length; i++) {
            var def = jsClassDefs[i],
                className = def._constructor
            ;
            this._replaceRefs(def, globals);
            if (className) {
                delete def._constructor;
                if (def.ID && !keepAllGlobals && !globals.contains(def.ID)) def.ID = "_" + def.ID;
                var instance = isc.ClassFactory.newInstance(className, def);
            }
        }

        isc.captureDefaults = null;
        var capturedComponents = this.getCapturedComponents();

        // Remove IDs that represent globals that should not be kept
        if (capturedComponents) this._removeIDs(capturedComponents, globals);

        if (capturedComponents) this.addFromPaletteNodes(capturedComponents, parentNode);
        this.fireCallback(callback, ["paletteNodes"], [capturedComponents]);
    },

    // Replace values of type "ref:<ID>" with actual instance
    _replaceRefs : function (def, keepGlobals) {
        var keepAllGlobals = (keepGlobals.length == 1 && keepGlobals[0] == isc.RPC.ALL_GLOBALS);

        for (var key in def) {
            var value = def[key];
            if (isc.isAn.Array(value)) {
                for (var i = 0; i < value.length; i++) {
                    if (isc.isA.String(value[i]) && value[i].startsWith("ref:")) {
                        var ref = value[i].replace("ref:", "");
                        if (!keepAllGlobals && !keepGlobals.contains(ref)) ref = "_" + ref;
                        value[i] = window[ref];
                    } else if (isc.isAn.Object(value[i])) {
                        this._replaceRefs(value[i], keepGlobals);
                    }
                }
            } else if (isc.isAn.Object(value)) {
                this._replaceRefs(value, keepGlobals);
            }
        }
    },

    // Remove ID attributes whose value is not listed in keepGlobals
    _removeIDs : function (def, keepGlobals) {
        var keepAllGlobals = (keepGlobals.length == 1 && keepGlobals[0] == isc.RPC.ALL_GLOBALS);

        if (def.ID && !keepAllGlobals && !keepGlobals.contains(def.ID)) delete def.ID;

        for (var key in def) {
            var value = def[key];
            if (isc.isAn.Array(value)) {
                for (var i = 0; i < value.length; i++) {
                    if (isc.isAn.Object(value[i])) {
                        this._removeIDs(value[i], keepGlobals);
                    }
                }
            } else if (isc.isAn.Object(value)) {
                this._removeIDs(value, keepGlobals);
            }
        }
    },

    //> @method Callbacks.PaletteNodeCallback
    // Callback fired with the +link{PaletteNode,PaletteNodes} obtained asynchronously.
    // @param paletteNodes (Array of PaletteNode) an array of PaletteNodes
    // @visibility external
    //<

    //> @method editContext.getPaletteNodesFromXML()
    // Obtain +link{PaletteNode,PaletteNodes} from an XML representation,
    // but do not add them to the EditContext.
    //
    // @param xmlString (String) XML string
    // @param callback (PaletteNodeCallback) Callback used to return the PaletteNodes
    // @see Callbacks.PaletteNodeCallback()
    // @see serializeAllEditNodes()
    // @see serializeEditNodes()
    // @visibility external
    //<
    getPaletteNodesFromXML : function (xmlString, callback) {
        var self = this;

        //isc.logWarn(isc.echo(xmlString));
        
        isc.DMI.callBuiltin({
            methodName: "xmlToJS",
            arguments: [xmlString],
            callback: function (rpcResponse) {
                self.getPaletteNodesFromJS(rpcResponse.data, callback);
            }
        });
    },

    //>!BackCompat 2013.09.25
    loadNodeTreeFromJS : function (jsString) {
        return this.addPaletteNodesFromJS(jsString);
    },
    //<!BackCompat

    //> @method editContext.addPaletteNodesFromJS()
    // Add +link{PaletteNode,PaletteNodes} from a JavaScript source representation.
    // <P>
    // By default, components that have +link{Canvas.ID,global IDs} will not
    // actually be allowed to take those global IDs - instead, only widgets that have one of the
    // global IDs passed as the <code>globals</code> parameter will actually receive their global
    // IDs.  To override this behavior, pass the special value +link{RPCManager.ALL_GLOBALS}
    // for the <code>globals</code> parameter.
    //
    // @param jsCode (String) JavaScript code to eval.
    // @param [parentNode] (EditNode) parent node (defaults to the root)
    // @param [globals] (Array of String) widgets to allow to take their global IDs
    // @visibility external
    //<
    addPaletteNodesFromJS : function (jsCode, parentNode, globals, callback) {
        if (globals == null) globals = [];
        else if (!isc.isAn.Array(globals)) globals = [globals];

        var self = this;
        this.getPaletteNodesFromJS(jsCode, function (paletteNodes) {
            // Remove IDs that represent globals that should not be kept
            if (paletteNodes) this._removeIDs(paletteNodes, globals);

            if (paletteNodes) self.addFromPaletteNodes(paletteNodes, parentNode);
            self.fireCallback(callback, ["paletteNodes"], [paletteNodes]);
        }, globals);
    },
    
    //> @method editContext.getPaletteNodesFromJS()
    // Obtain +link{PaletteNode,PaletteNodes} from a JavaScript source representation.
    // <P>
    // By default, components that have +link{Canvas.ID,global IDs} will not
    // actually be allowed to take those global IDs - instead, only widgets that have one of the
    // global IDs passed as the <code>globals</code> parameter will actually receive their global
    // IDs.  To override this behavior, pass the special value +link{RPCManager.ALL_GLOBALS}
    // for the <code>globals</code> parameter.
    //
    // @param jsCode (String) JavaScript code to eval.
    // @param callback (PaletteNodeCallback) Callback used to return the PaletteNodes
    // @param [globals] (Array of String) widgets to allow to take their global IDs
    // @see Callbacks.PaletteNodeCallback()
    // @visibility external
    //<
    getPaletteNodesFromJS : function (jsCode, callback, keepGlobals) {
        if (keepGlobals == null) keepGlobals = [];
        else if (!isc.isAn.Array(keepGlobals)) keepGlobals = [keepGlobals];

        var self = this;
        isc.captureDefaults = true;

        if (keepGlobals.length == 1 && keepGlobals[0] == isc.RPC.ALL_GLOBALS) {
            // suppress reportErrors
            isc.Class.globalEvalWithCapture(jsCode, function (globals, error) {
                // Note: this must happen first, before any other components are
                // created - otherwise we will trap them..
                isc.captureDefaults = null;
                var capturedComponents = self.getCapturedComponents(error);
                self.fireCallback(callback, ["paletteNodes"], [capturedComponents]);
            }, null, false);
        } else {
            // suppress reportErrors
            isc.Class.globalEvalAndRestore(jsCode, keepGlobals, function (globals, error) {
                // Note: this must happen first, before any other components are
                // created - otherwise we will trap them..
                isc.captureDefaults = null;
                var capturedComponents = self.getCapturedComponents(error);
                self.fireCallback(callback, ["paletteNodes"], [capturedComponents]);
            }, null, false);
        }

        isc.captureDefaults = null;
    },

    getCapturedComponents : function (error) {
        if (error) {
            isc.warn(
                "The following error occurred during loading of your view<br><br>: " + error + 
                ".<br><br>  The portion of the view that loaded succesfully will be shown."
            );
        }                

        var captured = isc.capturedComponents;
        isc.capturedComponents = null;

        var capturedIDs = (captured ? captured.getProperty("defaults").getProperty("ID") : null);
        this.logInfo("capturedComponents are: " + capturedIDs, "loadProject");

        return captured;
    },

    //>!BackCompat 2013.09.27
    addNodeTree : function (paletteNodes) {
        this.addFromPaletteNodes(paletteNodes);
    },
    //<!BackCompat

    //> @method EditContext.addFromPaletteNodes
    // Add the supplied +link{PaletteNode,PaletteNodes} to the parentNode, preserving internal
    // references from one supplied PaletteNode to another. This method should
    // be used with an array of possibly inter-related PaletteNodes (for
    // instance, those produced as a result of serialization via
    // +link{serializeAllEditNodes(),serializeAllEditNodes()}, rather than
    // calling +link{addFromPaletteNode(),addFromPaletteNode()} on each
    // individual PaletteNode.
    //
    // @param paletteNodes (Array of PaletteNode) array of PaletteNodes
    // @param [parentNode] (EditNode) parent to add to (defaults to the root)
    // @return (Array of EditNode) an array of the EditNodes added to the parentNode
    // @see addFromPaletteNode()
    // @visibility external
    //<
    addFromPaletteNodes : function (paletteNodes, parentNode) {
        //this.logWarn("paletteNodes: " + this.echoFull(paletteNodes), "loadProject");

        var data = this.getEditNodeTree();
        if (!parentNode) parentNode = data.getRoot();

        // When we evalWithCapture(), create() makes palette nodes instead of actual
        // instances.  This is a necessity so that initialization data can be captured cleanly.
        // 
        // These palette nodes are arranged in a tree just like live components would be (eg
        // layout.members contains palette nodes for children).
        //
        // We need to traverse this tree and make a series of calls to
        // Palette.makeEditNode() and EditContext.addNode() to actual create live
        // components and editNodes from this captured data.

        this.componentsToCreate = [];
        this.addComponentCalls = [];
        this.requiredDataSources = [];

        // traverse all captured components (components that called create()), finding all
        // subcomponents that need to be represented as separate tree nodes (eg Tabs, which do
        // not directly call create, but should appear in the editTree).
        for (var i = 0; i < paletteNodes.length; i++) {
            this.findChildPaletteNodes(null, paletteNodes[i], null, paletteNodes);
        }

        // second traversal: paletteNodes is a flattened list of all components that would call
        // create(), and in the previous traversal we marked any component that was found in
        // the subtree of any other component as hasParent:true.  Any remaining paletteNodes
        // with no hasParent:true marker must be children of root
        for (var i = 0; i < paletteNodes.length; i++) {
            if (!paletteNodes[i].hasParent) {
                this.findChildPaletteNodes(parentNode, paletteNodes[i], null, paletteNodes);
            }
        }

        // preserve init order for the best chance of allowing application logic to function

        var pNode, parentPNode;
        // captured components (those that directly called create) are first, in order of
        // create() calls (which is leaf nodes first)
        for (var i = 0; i < paletteNodes.length; i++) {
            pNode = paletteNodes[i];
            pNode.component = this.makeEditNode(pNode);
        }

        // create all other components in tree traversal order
        for (var i = 0; i < this.componentsToCreate.length; i++) {
            pNode = this.componentsToCreate[i];
            if (!pNode.component) {
                pNode.component = this.makeEditNode(pNode);
            }
        }

        // lastly, link components into the project tree.  Because of the way we do our
        // traversal, these are not in an order that is ready for tree adds, that is, children
        // can appear before their parents because objects that directly call create() can
        // appear before the pseudo-objects (eg Sections) that they belong to (eg ListGrid can
        // appear before the Tab it should be added to).  However in order to eg, not reverse
        // Section Stack or FormItem order, we need to generally follow the order of traversal
        // that put together the addComponentCalls.
        // Approach: keep traversing the list trying to add children to parents until all nodes
        // have been added
        var oldLength = -1,
            calls = this.addComponentCalls,
            newCallOrder = []; // just for debugging
            
        // Set a flag to indicate to the special editProxy.setDataSource() override that we are 
        // loading a node tree from disk, and should fall back to the ordinary setDataSource()
        // method - otherwise, we'll end up with duplicates in the projectComponents tree
        // Also, disables markDirty while true.
        isc._loadingNodeTree = true;

        var nodesAddedToParentNode = [];
            
        while (calls.length > 0 && oldLength != calls.length) {
            oldLength = calls.length;
            var callsToTry = calls.duplicate();
            for (var i = 0; i < callsToTry.length; i++) {
                var call = callsToTry[i],
                    parentPNode = call[1],
                    pNode = call[0],
                    parentProperty = call[2];
                if (parentPNode.name == "/") {
                    var nodedAdded = this.addNode(pNode.component, parentNode);
                    nodesAddedToParentNode.add(nodeAdded);
                    calls.remove(call);
                    newCallOrder.add(call);
                } else if (data.contains(parentPNode.component)) {
                    var childComponent = pNode.component;
                    if (data.contains(childComponent)) {
                        // we've already added this child to the tree elsewhere.  This occurs
                        // for singletons like a DataSource which are shared between multiple
                        // components.  It's valid and intended in this case that the
                        // liveObject be shared, but we need a distinct Tree node, so make a
                        // copy
                        childComponent = isc.addProperties({}, childComponent);
                    }
                    var nodeAdded = this.addNode(childComponent, parentPNode.component, null, parentProperty);
                    if (parentPNode.component == parentNode) {
                        nodesAddedToParentNode.add(nodeAdded);
                    }
                    calls.remove(call);
                    newCallOrder.add(call);
                }
            }
        }
        
        delete isc._loadingNodeTree;

        // report the order of addComponent calls
        if (this.logIsDebugEnabled("loadProject")) {
            this.logDebug("addComponent() calls during project loading:", "loadProject");
            for (var i = 0; i < newCallOrder.length; i++) {
                var call = newCallOrder[i],
                    parentPNode = call[1],
                    pNode = call[0];
                this.logDebug(
                    "addComponent(" + this.echoLeaf(pNode) + "," + this.echoLeaf(parentPNode), 
                    "loadProject"
                );
            }
        }
        
        if (calls.length > 0) {
            this.logWarn(
                "the following components could not be added to the project tree: " + 
                isc.echoAll(calls.getProperty("0"))
            );
        }

        return nodesAddedToParentNode;
    },

    // create a paletteNode that will load the named DataSource dynamically
    makeDSPaletteNode : function (dsName, dsType) {
        var node = {
            ID: dsName,

            // for controlling drag and drop
            // XXX would be good to get actual type in case a component
            // declares that it can only bind to a specific DataSource, however,
            // "getDefinedDataSources" RPC does not currently return this.
            type: "DataSource",

            // for display in DataSources palette
            dsType: dsType || "DataSource",
            
            // for display in project tree
            title: dsName,
            icon: "DataSource.png",
            iconSize: 16,

            // set up deferred loading
            loadData: function (node, callback) {
                isc.DS.get(node.ID, function (ds) {
                    node.liveObject = ds;
                    // minimal information for serializing the DataSource.  See
                    // getSerializeableTree()
                    node.defaults = {
                        _constructor: "DataSource", 
                        ID: ds.ID
                    };
                    node.isLoaded = true;
                    isc.Class.fireCallback(callback, "", [node]);
                });
            }
        };
        
        return node;
    },

    // recursively traverse a structure captured via evalWithCapture, modifying data so that it
    // is ready for addComponent().  
    // - detect anywhere that a component is being initialized with data that should be
    //   represented as a separate component in the EditTree (eg a Layout member or TabSet tab)
    // - remove the subcomponent from the initialization data and create a separate
    //   paletteNode for it.  The cases are:
    //   - palette nodes captured by evalWithCapture, from components that called create()
    //   - tabs, sectionItems and other pseudo-objects that we represent in the editTree,
    //     detected because they are in a field whose type appears in the palette
    //   - for code that *was not* generated by Visual Builder, we may find eg a Layout member
    //     represented as an object with a _constructor property, as happens when you declare
    //     nested components in XML instead of breaking all components into independant
    //     top-level declarations.  Note these subcomponents will not be paletteNodes because
    //     create() was never called for them.  Instead, their format is similar to a TabSet
    //     tab or other pseudo-object
    // - generate and store the list of addComponent() calls needed to construct the tree.  We
    //   do these later in order to detect top-level components, and to maximally preserve
    //   initialization order.
    findChildPaletteNodes : function (parent, componentData, parentProperty, paletteNodes) {
        var componentType = componentData.type || componentData.className;

        var logEnabled = this.logIsInfoEnabled("loadProject"),
            logDebugEnabled = this.logIsDebugEnabled("loadProject");
    
        if (logEnabled) {
            this.logInfo(
                "inspecting defaults of component: " + this.echoLeaf(componentData) + " of type: " + componentType, 
                "loadProject"
            );
        }

        var defaults = componentData.defaults,
        	loader = this;

        // search for child components that should also be added to the project tree
        var childComponents = [],
            singleArray = [],
            componentDS = isc.DS.get(componentType);
        for (var propName in defaults) {
            var propValues = defaults[propName];

            if (!isc.isAn.Array(propValues)) {
                singleArray[0] = propValues;
                propValues = singleArray;
            } else if (logDebugEnabled) {
                this.logDebug(
                    "checking Array property: " + propName + ", value: " + this.echoLeaf(propValues) +
                    (fieldSchema ? " with schema: " + fieldSchema : ""), 
                    "loadProject"
                );
            }

            var field = componentDS ? componentDS.getField(propName) : null,
                fieldType = field ? field.type : null,
                fieldSchema = isc.DS.get(fieldType),
                foundChildren = false;

            for (var i = 0; i < propValues.length; i++) {
                var propValue = propValues[i];
                if (logDebugEnabled) {
                    this.logDebug(
                        "checking property: " + propName + ", value: " + this.echoLeaf(propValue), 
                        "loadProject"
                    );
                }

                // found a component captured by evalWithCapture (called create())
                if (paletteNodes.contains(propValue)) {
                    if (logEnabled) {
                        this.logInfo(
                            "found capturedComponent: " + this.echoLeaf(propValue) + " under property: " + 
                            propName + " of component: " + this.echoLeaf(componentData),
                            "loadProject"
                        );
                    }
                    childComponents.add([propName, propValue]);
                    foundChildren = true;
                    continue;
                } 

                if (propValue == null) {
                    this.logInfo("null property: " + propName + " on component: " + this.echoLeaf(componentData));
                }
                                    
                // detect pseudo-objects (eg tabs):
                // if the field is declared as complex type *and* items of this class can be
                // created from the palette (so clearly it is represented in the component tree).
                // Note that this means different editors may treat different objects as tree
                // nodes, for example, fields of a ListGrid.
                var childType = (propValue ? propValue._constructor : null) || fieldType,
                    childClass = isc.ClassFactory.getClass(childType);

                if (
                    fieldSchema && (
                        (childClass && childClass.isA(isc.Canvas)) ||
                        (childClass && childClass.isA(isc.DataSource)) ||
                        (childClass && childClass.isA(isc.FormItem)) ||
                        (this.findPaletteNode("type", childType)) ||
                        (this.findPaletteNode("className", childType))
                    )
                ) {
                    if (logEnabled) {
                        this.logInfo(
                            "found palettized component: " + this.echoLeaf(propValue) + 
                            " of type: " + childType + " under property: " + propName + 
                            " of component: " + this.echoLeaf(componentData),
                            "loadProject"
                        );
                    }

                    // A String in an Object slot should be the ID of a component that was
                    // already created.  NOTE: tab.pane can be a String that refers to a
                    // component that was created *after* the TabSet, however this code does
                    // handle that case since capturedComponents contains all components that
                    // called create()
                    if (isc.isA.String(propValue)) {
                        var refComponent = paletteNodes.find("ID", propValue);
                        if (refComponent == null) {
                            // detect fields of DataSource type with String values referring to
                            // DataSources that don't exist in the file.  This can happen with
                            // code not generated by Visual Builder.  If these DataSources are
                            // known (they appear in the dataSourceList loaded from the
                            // server), create a paletteNode that will load them automagically.
                            if (isc.DataSource.isA(fieldType)) {
                                var knownDS = this.findPaletteNode("ID", propValue);
                                if (true) {
                                    refComponent = this.makeDSPaletteNode(propValue);
                                }
                            }
                            if (refComponent == null) continue;
                        }
                        childComponents.add([propName, refComponent]);
                    } else {
                        var childDefaults = propValue;
                        childComponents.add([propName, {
                            ID : childDefaults.ID,
                            name : childDefaults.name,
                            type : childType,
                            defaults : childDefaults
                        }]); 
                    }

                    foundChildren = true;
                }
            }

            if (foundChildren) delete defaults[propName];
        }
    
        // find the existing palette node for this class, if any, in order to pick up the icon
        // to use in the project tree
        var pNode = this.findPaletteNode("type", componentType) || this.findPaletteNode("className", componentType);

        if (pNode) {
            componentData.icon = componentData.icon || pNode.icon; 
            componentData.iconSize = componentData.iconSize || pNode.iconSize; 
            componentData.showDropIcon = componentData.showDropIcon || pNode.showDropIcon; 
        }
    
        // collect all the components that should be created and the calls to addComponent()
        // that need to happen
        this.componentsToCreate.add(componentData);
        if (parent != null) {
            componentData.hasParent = true;
            this.addComponentCalls.add([componentData, parent, parentProperty]);
        }

        // recurse to handle the children of this component
        if (childComponents.length > 0) {
            for (var i = 0; i < childComponents.length; i++) {
                this.findChildPaletteNodes(componentData, childComponents[i][1], childComponents[i][0], paletteNodes);
            }
        }
    },

    // ---------------------------------------------------------------------------------------

    isNodeEditingOn : function (editNode) {
        if (!editNode) return null;
        var liveObject = this.getLiveObject(editNode);

        return (liveObject ? liveObject.editingOn : false);
    },

    enableEditing : function (editNode) {
        var liveObject = editNode.liveObject;
        if (liveObject.setEditMode) {
            liveObject.setEditMode(true, this, editNode);
        } else {
            // We're trying enable editing on something that isn't a Canvas or a FormItem.
            // Assume that it needs no special logic beyond setting the editNode, editContext
            // and editingOn flag
            liveObject.editContext = this;
            liveObject.editNode = editNode;
            liveObject.editingOn = true;
        }
    },

    // Applying Properties to EditNodes
    // ---------------------------------------------------------------------------------------
   
    //> @method editContext.setNodeProperties()
    // Update an editNode's serializable "defaults" with the supplied properties. If you
    // wish to remove a property from the defaults (rather than setting it to null), then
    // use +link{removeNodeProperties(),removeNodeProperties()} instead.
    // @param editNode (EditNode) the editNode to update
    // @param properties (Canvas Properties) the properties to apply
    // @param [skipLiveObjectUpdate] (Boolean) whether to skip updating the
    //                                         +link{EditNode.liveObject,liveObject},
    //                                         e.g. if you have already updated the liveObject
    // @see removeNodeProperties
    // @visibility external
    //<
    
    setNodeProperties : function (editNode, properties, skipLiveObjectUpdate) {
        
 
        if (this.logIsDebugEnabled("editing")) {
            this.logDebug("with editNode: " + this.echoLeaf(editNode) + 
                          " applying properties: " + this.echo(properties), "editing");
        }
  
        if (!editNode.defaults) editNode.defaults = {}

        // update the initialization / serializeable data
        isc.addProperties(editNode.defaults, properties);
        
        // update the component node with the new ID
        if (editNode.defaults.ID != null) editNode.ID = editNode.defaults.ID;
    
        // update the live object, unless we're skipping that
        var targetObject = editNode.liveObject;
        if (targetObject && !skipLiveObjectUpdate) {
            // Name property changes must force a remove/add of the node (such as name
            // on a FormItem). This is specified in the "rebuildOnChange" property of the
            // parent property.
            var theTree = this.getEditNodeTree(),
                parentComponent = this.getEditNodeTree().getParent(editNode),
                parentSchema = (parentComponent ? isc.DS.get(parentComponent.type) : null),
                parentLiveObject = (parentComponent ? parentComponent.liveObject : null),
                parentFieldName = (parentLiveObject ? isc.DS.getObjectField(parentLiveObject, editNode.type) : null),
                parentField = (parentFieldName ? parentSchema.fields[parentFieldName] : null)
            ;
            if (properties.name != null && parentField && parentField.rebuildOnChange && parentField.rebuildOnChange.toLowerCase() == "true") {
                var index = theTree.getChildren(parentComponent).findIndex(editNode);

                this.logInfo("using remove/re-add cycle to modify liveObject: " +
                            isc.echoLeaf(targetObject) + " within parent node " +
                            isc.echoLeaf(parentComponent));
                    
                this.removeNode(editNode);

                // update the node with the new name and add it
                editNode.name = editNode.ID = properties.name;
                delete properties.name;

                this.addNode(editNode, parentComponent, index);

                // collect the newly created live object
                targetObject = this.getLiveObject(editNode);
            }

            // update the live object
            if (targetObject.setEditableProperties) {
                // instance of an SC class (or something else that implements a
                // setEditableProperties API)
                targetObject.setEditableProperties(properties);
                if (targetObject.markForRedraw) targetObject.markForRedraw();
                // NOTE: for FormItems, causes parent redraw
                else if (targetObject.redraw) targetObject.redraw();
            } else {
                // for objects that never become ISC classes (MenuItems, ListGrid fields), 
                // call an overridable method on the parent if it exists
                var parentComponent = this.getEditNodeTree().getParent(editNode),
                    parentLiveObject = parentComponent ? parentComponent.liveObject : null;
                if (parentLiveObject && parentLiveObject.setChildEditableProperties) 
                {
                    parentLiveObject.setChildEditableProperties(targetObject, properties, 
                                                                editNode, this);
                } else {
                    // fall back to just applying the properties
                    isc.addProperties(targetObject, properties);
                }
            }
        
            if (this.markForRedraw) this.markForRedraw();
        } // skipLiveObjectUpdate
    },

    //> @method editContext.removeNodeProperties()
    // Removes the specified properties from an editNode's serializable "defaults".
    // Note that the +link{EditNode.liveObject,liveObject} is <u>not</u> updated by this method. 
    // To set a property to null (rather than removing it), use
    // +link{setNodeProperties(),setNodeProperties()} instead.
    // @param editNode (EditNode) the editNode to update
    // @param properties (Array of String) an array of property names to remove
    // @see setNodeProperties()
    // @visibility external
    //<
    removeNodeProperties : function (editNode, properties) {
        if (!editNode.defaults) return;
        if (!isc.isAn.Array(properties)) properties = [properties];
        properties.map (function (property) {
            delete editNode.defaults[property];
        });
    },

    // ---------------------------------------------------------------------------------------

    // The "wrapperForm" is a DynamicForm that we auto-create as a container for a FormItem dropped 
    // directly onto a Canvas, Layout or whatever.  We're using autoChild-like semantics here so 
    // that you can provide your own settings for the generated form.  addWithWrapper() is also
    // used to wrap DrawItems in a DrawPane, and the third argument, wrapDrawPane, is a boolean
    // flag to distinguish the desired wrapper.
    wrapperFormDefaults: {
        _constructor: "DynamicForm"
    },
    wrapperDrawPaneDefaults: {
        _constructor: "DrawPane"
    },
    addWithWrapper : function (childNode, parentNode, wrapDrawPane) {
        var wrapForm = !wrapDrawPane,
            wrapperDefaults = (wrapDrawPane ? this.wrapperDrawPaneDefaults : this.wrapperFormDefaults),
            defaults = isc.addProperties({}, wrapperDefaults),
            paletteNode = {
                type: wrapperDefaults._constructor,
                defaults : defaults
            };

        // if this FormItem belongs to a DataSource, the wrapper form needs to use it too
        if (wrapForm && childNode.liveObject.schemaDataSource) {
            var item = childNode.liveObject;
            defaults.doNotUseDefaultBinding = true;
            defaults.dataSource = item.schemaDataSource;
            defaults.serviceNamespace = item.serviceNamespace;
            defaults.serviceName = item.serviceName;
        }
        var wrapperNode = this.makeEditNode(paletteNode);

        // add the wrapper to the parent
        this.addNode(wrapperNode, parentNode);
        // add the child node to the wrapper
        return this.addNode(childNode, wrapperNode);
    },

    // Selection management
    // --------------------------------------------------------------------------------------------
    
    selectedComponents: [],

    // START PUBLIC METHODS
    getSelectedComponents : function () {
        return this.selectedComponents.duplicate()
    },
    isComponentSelected : function (component) {
        if (!this.selectedComponents) return false;
        return this.selectedComponents.contains(component);
    },

    selectComponent : function (component) {
        if (!this.selectedComponents.contains(component)) {
            this.selectedComponents.add(component);
            var editProxy = this._getSelectionEditProxy();
            if (editProxy) editProxy.setOutline(this.selectedComponents);
            this.fireSelectedComponentsUpdated();
        }
    },
    selectSingleComponent : function (component) {
        // Ignore change to the same selection
        if (this.selectedComponents.length == 1 && this.selectedComponents.contains(component)) {
            return;
        }
        
        var changed = false,
            editProxy = this._getSelectionEditProxy()
        ;

        if (this.selectedComponents.length > 0) {
            if (editProxy) editProxy.clearOutline(this.selectedComponents);
            changed = true;
        }
        this.selectedComponents = [];
        if (component) {
            this.selectedComponents = [component];
            if (editProxy) {
                editProxy.setOutline(this.selectedComponents);
            }
            changed = true;
        }
        if (changed) this.fireSelectedComponentsUpdated();
    },
    selectAllComponents : function () {
        this.selectedComponents = [];
        var editProxy = this._getSelectionEditProxy();
        if (editProxy) {
            this.selectedComponents = editProxy.getAllSelectableComponents();
            editProxy.setOutline(this.selectedComponents);
        }
        this.fireSelectedComponentsUpdated();
    },
    deselectComponents : function (components) {
        if (!isc.isAn.Array(components)) components = [components];
        var editProxy = this._getSelectionEditProxy();
        if (editProxy) editProxy.clearOutline(components);
        if (this.selectedComponents.removeList(components)) {
            this.fireSelectedComponentsUpdated();
        }
    },
    deselectAllComponents : function () {
        if (!this.selectedComponents || this.selectedComponents.length == 0) return;
        var editProxy = this._getSelectionEditProxy();
        if (editProxy) editProxy.clearOutline(this.selectedComponents);
        this.selectedComponents = [];
        this.fireSelectedComponentsUpdated();
    },
    // END PUBLIC METHODS

    _getSelectionEditProxy : function () {
        var selectionLiveObject = this._selectionLiveObject;
        if (!selectionLiveObject) return null;
        return (selectionLiveObject.editingOn ? selectionLiveObject.editProxy : null);
    },

    fireSelectedComponentsUpdated : function () {
        var editProxy = this._getSelectionEditProxy();
        if (editProxy && (editProxy.selectedComponentsUpdated || this.selectedComponentsUpdated)) {
            var componentList = this.getSelectedComponents(),
                component = (componentList && componentList.length > 0 ? componentList[0] : null)
            ;

            if (editProxy.selectedComponentsUpdated) {
                editProxy.selectedComponentsUpdated(component, componentList);
            }
            if (this.selectedComponentsUpdated) {
                this.selectedComponentsUpdated(component, componentList);
            }
        }
    },

    //> @method editContext.selectedComponentsUpdated()
    // Called when editMode selection changes. Note this method fires exactly once for any given
    // change.
    // <P>
    // This event is fired once after selection/deselection has completed. The result is
    // one event per mouse-down event. For a drag selection there will be one event fired
    // when the range is completed.
    //
    // @param component (object)               first selected component, if any
    // @param componentList (Array of Object)  List of components that are now selected
    // @visibility internal
    //<    
    selectedComponentsUpdated : function (component, componentList) {},

    saveCoordinates : function (liveObject) {
        if (!this.persistCoordinates) return;
        if (isc.isA.DrawItem(liveObject) || isc.isA.DrawKnob(liveObject)) {
            return;
        }
        //this.logWarn("saveCoordinates for: " + liveObject + 
        //             ", editComponents are: " + this.echoAll(this.getEditNodeArray()));
        var component = this.getEditNodeArray().find("liveObject", liveObject);

        // can happen if we get a resized or moved notification while a component is being
        // added or removed
        if (!component) return; 

        this.setNodeProperties(component, {
            left: liveObject.getLeft(),
            top: liveObject.getTop(),
            // Use percentage width or "*" if supplied
            width: liveObject._percent_width || liveObject._userWidth || liveObject.getWidth(),
            height: liveObject._percent_height || liveObject._userHeight || liveObject.getHeight()
        }, true);
    }
});

//> @groupDef devTools
// The Dashboards & Tools framework enables you to build interfaces in which a set of UI
// components can be edited by end users, saved and later restored.
// <P>
// This includes interfaces such as:
// <ul> 
// <li> <b>Dashboards</b>: where a library of possible widgets can be created &
// configured then stored for future use and shared with other users
// <li> <b>Diagramming & Flowchart tools</b>: tools similar to Visio&trade; which allows users
// to use shapes and connectors to create a flowchart or diagram
// <li> <b>Form Builders &amp; Development Tools</b>: tools which enable end users to create
// new forms or entirely new screens and add them to the application on the fly
// </ul>
// <P>
// The basic building blocks of the Dashboards & Tools framework are:
// <ul>
// <li> <i>Palettes</i>: palettes create UI components on the fly from stored data.  Palettes
// come in a variety of flavors which implement different UI gestures for creating components
// (e.g. drag from tree, pick from menu, etc)
// <li> <i>EditContexts</i>: an edit context tracks a set of components that are being edited.
// Different EditContexts offer different built-in user interactions for editing the
// components they track, for example, a tree-based EditContext can provide an interface for
// managing relationships between components via drag and drop, and a Canvas-based EditContext
// can automatically store changes to position and size.
// </ul>
// <h3>Defaults vs LiveObject</h3>
// The Dashboards & Tools framework is careful to maintain a distinction between the current state
// of the live UI component and the data that should actually be persisted and used to
// re-create the component.  For example:
// <ul>
// <li> a portlet may be showing a border or brightened background color to hilite it within
// the tool, but this should not be saved as part of the portlet state
// <li> a component may have a current width of 510 pixels when viewed within a tool, but what
// should persist is the configured width of 40% of available space
// <li> in a development tool, a component such a Window automatically creates subcomponents
// such as a header - these should not persist because the Window knows how to create them
// without any additional data.  Only components specifically dragged into the Window by the
// end user should be persisted
// </ul>
// The data that will be saved and used to re-create the component is called the
// <b>defaults</b>.  A Palette, which creates a component from stored data, minimally requires
// the +link{SCClassName,type} of the component to create, and the defaults.  This
// information is captured by a +link{PaletteNode} (along with other options) - all types of
// Palettes work with a set or tree of paletteNodes.
// <P>
// When a component is created from a paletteNode, a "live object" is created from the
// defaults.  One PaletteNode may generate any number of live objects.  An +link{EditNode} 
// combines the +link{editNode.liveObject,live object} along with the type and defaults
// needed to recreate the live object, and is used to track the created live object as it is
// further edited.  Essentially an EditNode is a copy of the PaletteNode in which a live object
// has been created and the editNode.defaults may now begin to change as the user edits the
// object they have created.
//
// <h3>Module requirements</h3>
// <b>NOTE:</b> you must load the Tools +link{group:loadingOptionalModules,Optional Module} 
// for this framework.
// <P>
// Any tools that work with hierarchies of system components or derivations
// of them will also need the system schema which can be loaded by either of the
// following:
// <P>
// <i>JSP tag:</i> <pre>&lt;script&gt;&lt;isomorphic:loadSystemSchema /&gt;&lt;/script&gt;</pre>
// <P>
// <i>HTML tag:</i> <pre>&lt;SCRIPT SRC="../isomorphic/DataSourceLoader?dataSource=$systemSchema"&gt;&lt;/SCRIPT&gt;</pre>
//
// @title Dashboards & Tools Framework Overview
// @treeLocation Client Reference/Tools
// @visibility external
//<





//> @object PaletteNode
// An object representing a component which the user may create dynamically within an
// application.
// <P>
// A PaletteNode expresses visual properties for how the palette will display it (eg
// +link{paletteNode.title,title}, +link{paletteNode.icon,icon}) as well as instructions for
// creating the component the paletteNode represents (+link{paletteNode.type},
// +link{paletteNode.defaults}).
// <P>
// Various types of palettes (+link{ListPalette}, +link{TreePalette}, +link{MenuPalette},
// +link{TilePalette}) render a PaletteNode in different ways, and allow the user to trigger
// creation in different ways (eg drag and drop, or just click).  All share a common pattern
// for how components are created from palettes.
// <P>
// Note that in a TreePalette, a PaletteNode is essentially a +link{TreeNode} and can have
// properties expected for a TreeNode (eg,
// +link{TreeGrid.customIconDropProperty,showDropIcon}.  Likewise
// a PaletteNode in a MenuPalette can have the properties of a +link{MenuItem}, such as
// +link{MenuItem.enableIf}.
// 
// @treeLocation Client Reference/Tools
// @visibility external
//<

//> @attr paletteNode.icon (SCImgURL : null : IR)
// Icon for this paletteNode.
//
// @visibility external
//<

//> @attr paletteNode.title (String : null : IR)
// Textual title for this paletteNode.
//
// @visibility external
//<

//> @attr paletteNode.type (SCClassName : null : IR)
// +link{SCClassName} this paletteNode creates, for example, "ListGrid".
//
// @visibility external
//<


//> @attr paletteNode.defaults (Properties : null : IR)
// Defaults for the component to be created from this palette.  
// <P>
// For example, if +link{paletteNode.type} is "ListGrid", properties valid to pass to
// +link{Class.create,ListGrid.create()}.
//
// @visibility external
//<

//> @attr paletteNode.liveObject (Object : null : IR)
// For a paletteNode which should be a "singleton", that is, always provides the exact same
// object (==) rather than a dynamically created copy, provide the singleton object as
// <code>liveObject</code>.
// <P>
// Instead of dynamically creating an object from defaults, the <code>liveObject</code> will
// simply be assigned to +link{editNode.liveObject} for the created editNode.
//
// @visibility external
//<

//> @attr paletteNode.wizardConstructor (PaletteWizard : null : IR)
// A paletteNode that requires user input before component creation can occur 
// may provide a <code>wizardConstructor</code> and +link{wizardDefaults} for the creation of
// a "wizard" component.
// <P>
// Immediately after creation, the wizard will then have the +link{paletteWizard.getResults()}
// method called on it, dynamically produced defaults.
//
// @visibility internal
//<

//> @attr paletteNode.wizardDefaults (PaletteWizard Properties : null : IR)
// Defaults for the wizard created to gather user input before a component is created from
// this PaletteNode.  See +link{wizardConstructor}.
//
// @visibility internal
//<



// PaletteWizard
// ---------------------------------------------------------------------------------------

//> @interface PaletteWizard
// Interface to be fulfilled by a "wizard" specified on a +link{PaletteNode} via
// +link{paletteNode.wizardConstructor}.
// @visibility internal
//<

//> @method paletteWizard.getResults()
// Single function invoked on paletteWizard.  Expects defaults to be asynchronously returned,
// after user input is complete, by calling the +link{Callback} provided as a parameter.
// 
// @param callback (Callback) callback to be fired once this wizard completes.  Expects a
//                            single argument: the defaults
// @param paletteNode (PaletteNode) the paletteNode that specified this wizard
// @param palette (Palette) palette where creation is taking place
//
// @visibility internal
//<

//> @interface Palette
// An interface that provides the ability to create components from a +link{PaletteNode}.  
//
// @treeLocation Client Reference/Tools
// @group devTools
// @visibility external
//<

isc.ClassFactory.defineInterface("Palette");

isc.Palette.addInterfaceProperties({
    //> @attr palette.defaultEditContext (EditContext : null : IRW)
    // Default EditContext that this palette should use.  Palettes generally create components via
    // drag and drop, but may also support creation via double-click or other UI idioms when a
    // defaultEditContext is set.
    // @visibility external
    //<

    //> @method palette.setDefaultEditContext()
    // Sets the default EditContext that this palette should use.  Palettes generally create components via
    // drag and drop, but may also support creation via double-click or other UI idioms when a
    // defaultEditContext is set.
    // @param defaultEditContext (EditContext) the default EditContext used by this Palette
    // @visibility external
    //<
    setDefaultEditContext : function (defaultEditContext) {
        this.defaultEditContext = defaultEditContext;

        // If the defaultEditContext does not have a defaultPalette, then set it
        if (defaultEditContext && !defaultEditContext.defaultPalette) {
            defaultEditContext.defaultPalette = this;
        }
    },

    //> @method palette.makeEditNode()
    // Given a +link{PaletteNode}, make an +link{EditNode} from it by creating a 
    // +link{editNode.liveObject,liveObject} from the +link{paletteNode.defaults}
    // and copying presentation properties (eg +link{paletteNode.title,title}
    // to the editNode. If <code>editNodeProperties</code> is specified as an object on
    // on the paletteNode, each property in this object will also be copied across to
    // the editNode.
    // @param paletteNode (PaletteNode) paletteNode to create from
    // @return (EditNode) created EditNode
    //
    // @visibility external
    //<
    makeEditNode : function (paletteNode) {
        if (!paletteNode) paletteNode = this.getDragData();
        if (isc.isAn.Array(paletteNode)) paletteNode = paletteNode[0];
    
        var type = paletteNode.type || paletteNode.className;
        if (!isc.SGWTFactory.getFactory(type) && type.contains(".")) type = type.split(/\./).pop();

        var componentNode = {
            type : type,
            _constructor : type, // this is here just to match the defaults
            // for display in the target Tree
            title : paletteNode.title,
            icon : paletteNode.icon,
            iconSize : paletteNode.iconSize,
            showDropIcon : paletteNode.showDropIcon,
            useEditMask : paletteNode.useEditMask,
            autoGen : paletteNode.autoGen,
            editProxyProperties : paletteNode.editProxyProperties
        };
        
        // support arbitrary properties on the generated edit node
        // This allows 'loadData' to get at properties that might not otherwise be copied
        // across to the editNode from the paletteNode
        if (isc.isAn.Object(paletteNode.editNodeProperties)) {
            for (var prop in paletteNode.editNodeProperties) {
                componentNode[prop] = paletteNode.editNodeProperties[prop];
            }
        }

        // allow a maker function on the source data (synchronous)
        if (paletteNode.makeComponent) {
            componentNode.liveObject = paletteNode.makeComponent(componentNode);
            return componentNode;
        }

        // NOTE: IDs
        // - singletons may have an ID on the palette node.  
        // - an ID may appear in defaults because palette-based construction is used to reload
        //   views, and in this case the palette node will be used once ever
        var defaults = paletteNode.defaults;
        componentNode.ID = paletteNode.ID || 
                (defaults ? isc.DS.getAutoId(defaults) : null);
                
        var clobberDefaults = true;

        if (paletteNode.loadData) {
            // deferred load node.  No creation happens for now; whoever receives this node is
            // expected to call the loadData function
            componentNode.loadData = paletteNode.loadData;
        } else if (paletteNode.wizardConstructor) {
            // wizard-based deferred construction
            componentNode.wizardConstructor = paletteNode.wizardConstructor;
            componentNode.wizardDefaults = paletteNode.wizardDefaults;
        } else if (paletteNode.liveObject) {
            // singleton, or already created component.  This means that rather than a new
            // object being instantiated each time, the same "liveObject" should be reused,
            // because multiple components will be accessing a shared object.
            var liveObject = paletteNode.liveObject;
            // handle global IDs
            if (isc.isA.String(liveObject)) liveObject = window[liveObject];
            componentNode.liveObject = liveObject
        } else {
            // create a live object from defaults
            componentNode = this.createLiveObject(paletteNode, componentNode);
            clobberDefaults = false;
        }

        // also pass the defaults. Note that this was overwriting a more detailed set of defaults
        // derived by the createLiveObject method; hence the introduction  of the condition
        if (clobberDefaults) {
            componentNode.defaults = isc.addProperties({}, paletteNode.defaults);
            delete componentNode.defaults[isc.gwtRef];
            delete componentNode.defaults[isc.gwtModule];
            delete componentNode.defaults["xsi:type"];
        }

        return componentNode;
    },
    
    //>!BackCompat 2013.09.20
    makeNewComponent : function (sourceData) {
       return this.makeEditNode(sourceData);
    },
    //<!BackCompat
    
    //> @attr palette.generateNames   (boolean : true : IR)
    // Whether created components should have their "ID" or "name" property automatically set
    // to a unique value based on the component's type, eg, "ListGrid0".
    //
    // @group devTools
    // @visibility external
    //<
    generateNames : true,

    typeCount : {},
    // get an id for the object we're creating, by type
    getNextAutoId : function (type) {
        if (type == null) {
            type = "Object";
        } else {
            // Use short IDs for objects created via SGWT reflection
            if (type.contains(".")) type = type.split(/\./).pop(); 
        }
        var autoId;
        this.typeCount[type] = this.typeCount[type] || 0;
        while (window[(autoId = type + this.typeCount[type]++)] != null) {}
        return autoId;
    },

    findPaletteNode : function (fieldName, value) {
        return null;
    },

    createLiveObject : function (paletteNode, editNode) {

        // put together an initialization data block
        var type = paletteNode.type || paletteNode.className;
        if (type.contains(".") && !isc.SGWTFactory.getFactory(type)) type = type.split(/\./).pop();

        var classObject = isc.ClassFactory.getClass(type),
            schema = isc.DS.getNearestSchema(type),
            defaults = {},
            // assume we should create standalone if there's no schema (won't happen anyway if
            // there's no class)
            createStandalone = (schema ? schema.shouldCreateStandalone() : true),
            paletteNodeDefaults = paletteNode.defaults || {};

        // suppress drawing for widgets
        if (classObject && classObject.isA("Canvas")) defaults.autoDraw = false;

        // If a title was explicitly passed in the sourceData, use it
        if (paletteNodeDefaults.title) {
            defaults.title = paletteNodeDefaults.title;
        }

        if (this.generateNames) {
            // generate an id if one wasn't specified
            var ID = editNode.ID || paletteNodeDefaults[schema.getAutoIdField()];
            if (ID == null) {
                ID = this.getNextAutoId(type);
                
                if (isc.isA.Class(classObject)) {
                    defaults.hasStableID = function () { return false; };
                }
            }
            editNode.ID = ID;

            // give the object an autoId in defaults
            defaults[schema.getAutoIdField()] = ID;
    
            // don't supply a title for contexts where the ID or name will automatically be
            // used as a title (currently just formItems), otherwise, it will be necessary to
            // change both ID/name and title to get rid of the auto-gen'd id 
            if (
                schema && 
                schema.getField("title") && 
                !isc.isA.FormItem(classObject) &&
                !defaults.title
            ) {
                defaults.title = ID;
            }
        }

        defaults = editNode.defaults = isc.addProperties(
            defaults,
            this.componentDefaults,
            paletteNodeDefaults
        );
        delete defaults[isc.gwtRef];
        delete defaults[isc.gwtModule];
        // An xsi:type property in defaults should be dropped to avoid serializing because
        // it won't be valid without proper includes.
        delete defaults["xsi:type"];
        defaults._constructor = type;

        
        var classObject = isc.ClassFactory.getClass(type);
        for (var prop in defaults) {
            var val = defaults[prop];
            if (
                isc.isAn.Array(val) &&
                (!classObject || classObject.getInstanceProperty(prop) !== val)
            ) {
                val = defaults[prop] = val.duplicate();

                // Check for arrays of arrays.
                for (var i = val.length; i--; ) {
                    if (isc.isAn.Array(val[i])) {
                        val[i] = val[i].duplicate();
                    }
                }
            }
        }

        // create the live object from the init data
        // NOTE: components generated from config by parents (tabs, sectionStack sections,
        // formItems).  These objects:
        // - are created as an ISC Class by adding to a parent, and not before
        //   - in makeEditNode, don't create if there is no class or if the schema sets
        //     createStandalone=false
        // - destroyed by removal from the parent, then re-created by a re-add
        //   - re-add handled by addComponent by checking for destruction
        // - serialized as sub-objects rather than independent components
        //   - handled by checking for _generated during serialization
        //   - should be a default consequence of not having a class or setting
        //     createStandalone=false
        // The various checks mentioned above are redundant and should be unified and make able
        // to be declared in component schema

        // if there's no class for the item, or schema.createStandalone has been set false,
        // don't auto-create the component - assume the future parent of the component will
        // create it from data.  The explicit flag (createStandalone:false) is needed for
        // FormItems.  In particular, canvasItems require item.containerWidget to be defined
        // during init.
        var liveObject;
        if (classObject && createStandalone) {
            liveObject = isc.ClassFactory.newInstance(defaults);
        } else {
            // for the live object, just create a copy (NOTE: necessary because widgets
            // generally assume that it is okay to add properties to pseudo-objects provided as
            // init data)
            editNode.generatedType = true;
            liveObject = isc.shallowClone(defaults);
        }

        // store the new live object
        editNode.liveObject = liveObject;
        this.logInfo("palette created component, type: " + type +
                     ", ID: " + ID +
                     (this.logIsDebugEnabled("editing") ?
                         ", defaults: " + this.echo(defaults) : "") + 
                     ", liveObject: " + this.echoLeaf(liveObject), "editing");
        return editNode;
    }
});

//> @class HiddenPalette
// A Palette with no visible representation that handles programmatic creation of components.
//
// @implements Palette
// @group devTools
// @treeLocation Client Reference/Tools/Palette
// @visibility external
//<
isc.defineClass("HiddenPalette", "Class", "Palette");

isc.HiddenPalette.addMethods({
    //> @attr hiddenPalette.data (List of PaletteNode : null : IR)
    // A list of +link{PaletteNode,PaletteNodes} for component creation.
    // @visibility external
    //<

    findPaletteNode : function (fieldName, value) {
        return this.data ? this.data.find(fieldName, value) : null;
    }
});

// ---------------------------------------------------------------------------------------

//> @class TreePalette
// A TreeGrid that implements the Palette behavior, so it can be used as the source for 
// drag and drop instantiation of components when combined with an +link{EditContext} as 
// the drop target.
// <P>
// Each +link{TreeNode} within +link{treeGrid.data} can be a +link{PaletteNode}.
//
// @implements Palette
// @group devTools
// @treeLocation Client Reference/Tools/Palette
// @visibility external
//<

// Class will not work without TreeGrid
if (isc.TreeGrid) {

isc.defineClass("TreePalette", "TreeGrid", "Palette");

isc.TreePalette.addMethods({
    //> @attr treePalette.componentDefaults    (Object : null : IR)
    // Defaults to apply to all components originating from this palette.
    // @group devTools
    // @visibility external
    //<
    

    canDragRecordsOut:true,
    // add to defaultEditContext (if any) on double click 
    recordDoubleClick : function () {
        var target = this.defaultEditContext;
        if (target) {
            if (isc.isA.String(target) && this.creator) target = this.creator[target];
            if (isc.isAn.EditContext(target)) {
                var node = this.makeEditNode(this.getDragData());
                if (node) {
                    if (target.getDefaultParent(node, true) == null) {
                        isc.warn("No default parent can accept a component of this type");
                    } else {
                        target.addNode(node);
                        isc.EditContext.selectCanvasOrFormItem(node.liveObject, true);
                    }
                }
            }
        }
    },

    findPaletteNode : function (fieldName, value) {
        return this.data ? this.data.find(fieldName, value) : null;
    },

    // NOTE: we can't factor this up to the Palette interface because it wouldn't override the
    // built-in implementation of transferDragData.
    transferDragData : function (targetFolder) {
        return [this.makeEditNode(this.getDragData())];
    }
});

}

// --------------------------------------------------------------------------------------------
//> @class ListPalette
// A ListGrid that implements the +link{Palette} behavior, so it can be used as the source for 
// drag and drop instantiation of components when combined with an +link{EditContext} as 
// the drop target.
// <P>
// Each +link{ListGridRecord} can be a +link{PaletteNode}.
//
// @implements Palette
// @group devTools
// @treeLocation Client Reference/Tools/Palette
// @visibility external
//<

// Class will not work without ListGrid
if (isc.ListGrid) {

isc.defineClass("ListPalette", "ListGrid", "Palette");

isc.ListPalette.addMethods({
    canDragRecordsOut:true,
    defaultFields : [ { name:"title", title:"Title" } ],
    
    // add to defaultEditContext (if any) on double click 
    recordDoubleClick : function () {
        // NOTE: dup'd in TreePalette
        var target = this.defaultEditContext;
        if (target) {
            if (isc.isA.String(target)) target = isc.Canvas.getById(target);
            if (isc.isAn.EditContext(target)) {
                target.addNode(this.makeEditNode(this.getDragData()));
            }
        }
    },
    
    findPaletteNode : function (fieldName, value) {
        return this.data ? this.data.find(fieldName, value) : null;
    },
    
    // NOTE: we can't factor this up to the Palette interface because it wouldn't override the
    // built-in implementation of transferDragData.
    transferDragData : function () {
        return [this.makeEditNode(this.getDragData())];
    }
});

}

// --------------------------------------------------------------------------------------------
//> @class TilePalette
// A +link{TileGrid} that implements the +link{Palette} behavior, so it can be used as the source for 
// drag and drop instantiation of components when combined with an +link{EditContext} as 
// the drop target.
// <P>
// Each +link{TileGrid.tile} can be a +link{PaletteNode}.
//
// @implements Palette
// @group devTools
// @treeLocation Client Reference/Tools/Palette
// @visibility external
//<

// Class will not work without TileGrid
if (isc.TileGrid) {

isc.defineClass("TilePalette", "TileGrid", "Palette");

isc.TilePalette.addMethods({
    canDragTilesOut: true,
    defaultFields: [
        {name: "title", title: "Title"}
    ],

    // add to defaultEditContext (if any) on double click 
    recordDoubleClick : function () {
        var target = this.defaultEditContext;
        if (target) {
            if (isc.isA.String(target)) target = isc.Canvas.getById(target);
            if (isc.isAn.EditContext(target)) {
                target.addNode(this.makeEditNode(this.getDragData()));
            }
        }
    },

    findPaletteNode : function (fieldName, value) {
        return this.data ? this.data.find(fieldName, value) : null;
    },
    
    // NOTE: we can't factor this up to the Palette interface because it wouldn't override the
    // built-in implementation of transferDragData.
    transferDragData : function () {
        return [this.makeEditNode(this.getDragData())];
    }
});

}

// --------------------------------------------------------------------------------------------
//> @class MenuPalette
// A Menu that implements the +link{Palette} behavior, so it can be used as the source for 
// drag and drop instantiation of components when combined with an +link{EditContext} as 
// the drop target.
// <P>
// Each +link{MenuItem} can be a +link{PaletteNode}.
//
// @implements Palette
// @group devTools
// @treeLocation Client Reference/Tools/Palette
// @visibility external
//<

// Class will not work without Menu
if (isc.Menu) {

isc.defineClass("MenuPalette", "Menu", "Palette");

isc.MenuPalette.addMethods({
    canDragRecordsOut: true,
    
    // needed because the selection is what's dragged, and menus do not normally track a
    // selection
    selectionType: "single",

    // add to defaultEditContext (if any) on click 
    itemClick : function (item) {
        var target = this.defaultEditContext;
        if (target) {
            if (isc.isA.String(target)) target = isc.Canvas.getById(target);
            if (isc.isAn.EditContext(target)) {
                target.addNode(this.makeEditNode(this.getDragData()));
            }
        }
    },
    
    findPaletteNode : function (fieldName, value) {
        return this.data ? this.data.find(fieldName, value) : null;
    },
    
    // NOTE: we can't factor this up to the Palette interface because it wouldn't override the
    // built-in implementation of transferDragData.
    transferDragData : function () {
        return [this.makeEditNode(this.getDragData())];
    }
});

}




// ---------------------------------------------------------------------------------------

//> @class EditPane
// A container that allows drag and drop instantiation of visual components from a
// +link{Palette}, and direct manipulation of the position and size of those components.
// <P>
// Any drag onto an EditPane from a Palette will add an EditNode created from the dragged
// PaletteNode.
//
// @group devTools
// @implements EditContext
// @treeLocation Client Reference/Tools/EditContext
// @visibility external
//<

// Schema definition for the EditPane class, in case we have not loaded the system schema.

if (!isc.DataSource.get("EditPane")) {
    isc.DataSource.create({
        ID: "EditPane",
        Contructor: "EditPane",
        addGlobalId:false,
        fields: [
            {name: "children", type: "Canvas", multiple: true}
        ]
    });
}


isc.ClassFactory.defineClass("EditPane", "Canvas", "EditContext");

isc.EditPane.addProperties({
    canAcceptDrop:true,

    // Enable Canvas-based component selection, positioning and resizing support
    editProxyDefaults: {
        enableComponentSelection: true
    },

    //> @attr editPane.editMode        (Boolean : true : [IRW])
    // Enables/disables edit mode. Edit mode allows component addition, positioning and
    // resizing which is the default.
    // <P>
    // Note that a +link{PortalLayout} provides edit mode-style editing by default so 
    // <code>editMode</code> should be disabled for that case.
    //
    // @visibility internal
    //<
    editMode: true,

    initWidget : function () {
        // We'll be the live object for the root node ... this gets picked up 
        // later in getEditNodeTree
        this.rootComponent = {
            type: "EditPane",
            liveObject: this
        };
        this.Super("initWidget", arguments);

        // A normal editContext implementation is a Tree which has a selection
        // model. We need this model on an EditPane as well. The selection model
        // also requires the "data" property so it's initialized to an empty array.
        this.data = [];
        this.createSelectionModel();

        // Put pane into edit mode
        if (this.editMode) this.setEditMode(true, this);
    },

    setEditMode : function (editingOn, editContext, editNode) {
        if (editingOn == null) editingOn = true;
        if (this.editingOn == editingOn) return;

        // EditPane is it's own editContext
        if (!editContext) editContext = this;

        if (editContext != this) editContext.showSelectionOutline = this.showSelectionOutline;

        this.Super("setEditMode", [editingOn, editContext, editNode], arguments);

        // Hang on to the liveObject that manages the selection UI.
        // It is responsible for showing the outline or other selected state
        if (this.editProxy.enableComponentSelection) this._selectionLiveObject = this;

        var liveObjects = this.getEditNodeArray().getProperty("liveObject");
        liveObjects.map("setEditMode", editingOn, this);

        if (editingOn) {
            this.contextMenu = {
                autoDraw:false,
                data : [{title:"Clear", click: "target.destroyAll()"}]
            };
        } else {
            this.contextMenu = null;
        }
    },

    // Component creation
    // ---------------------------------------------------------------------------------------

    // This is needed if the system schema has not been loaded
    getObjectField : function (type) {
        var classObject = isc.ClassFactory.getClass(type);
        if (isc.isA.Canvas(classObject)) {
            return "children";
        } else {
            return null;
        }
    },

    nodeAdded : function (newNode) {
        // Flip it into edit mode depending on the setting on the EditPane instance
        if (this.editingOn) this.enableEditing(newNode);

        // Add an event mask if so configured
        if (newNode.useEditMask && newNode.liveObject.editProxy) {
            newNode.liveObject.editProxy.showEditMask(this);
        }
    },

    // Component removal / destruction
    // ---------------------------------------------------------------------------------------
    
    // if a child is removed that is being edited, remove it from the list of edit components
    removeChild : function (child, name) {
        if (isc.EditProxy.getThumbTarget() == child) isc.EditProxy.hideResizeThumbs();

        this.Super("removeChild", arguments);
        var node = this.getEditNodeArray().find("liveObject", child);
        if (node) {
            this.removeNode(node, true); // skip live removal, since that's been done
        }
    },
    
    // Serialization
    // ---------------------------------------------------------------------------------------

    //> @method editPane.getSaveData()
    // Returns an Array of +link{PaletteNode}s representing all current +link{EditNode}s in this
    // pane, suitable for saving and restoring via passing each paletteNode to +link{EditContext.addNode(),addNode()}.
    // @return (Array of PaletteNode) paletteNodes suitable for saving for subsequent restoration 
    //
    // @visibility external
    //<
    getSaveData : function () {
        // get all the components being edited
        var data = this.getEditNodeTree(),
            editComponents = data.getChildren(data.getRoot()),
            allSaveData = [];
        for (var i = 0; i < editComponents.length; i++) {
            var component = editComponents[i],
                liveObject = component.liveObject;
            // save off just types and initialization data, not the live objects themselves
            var saveData = {
                type : component.type,
                defaults : component.defaults
            };
            // let the object customize it
            if (liveObject.getSaveData) saveData = liveObject.getSaveData(saveData);
            allSaveData.add(saveData);
        }
        return allSaveData;
    }
});


//> @class EditTree
// A TreeGrid that allows drag and drop creation and manipulation of a tree of 
// objects described by DataSources.
// <P>
// Nodes can be added via drag and drop from a +link{Palette} or may be programmatically 
// added via +link{EditContext.addNode(),addNode()}.  Nodes may be dragged within the tree to reparent 
// them.
// <P>
// Eligibility to be dropped on any given node is determined by inspecting the
// DataSource of the parent node.  Drop is allowed only if the parent schema has
// a field which accepts the type of the dropped node.
// <P>
// On successful drop, the newly created component will be added to the parent node under the
// detected field.  Array fields, declared by setting
// <code>dataSourceField.multiple:true</code>, are supported.  
// <P>
// An EditTree is initialized by setting +link{EditContext.rootComponent}.  EditTree.data (the
// Tree instance) should never be directly set or looked at.
//
// @treeLocation Client Reference/Tools/EditContext
// @implements EditContext
// @group devTools
// @visibility external
//<



// Class will not work without TreeGrid
if (isc.TreeGrid) {

isc.ClassFactory.defineClass("EditTree", "TreeGrid", "EditContext");

isc.EditTree.addProperties({
    canDragRecordsOut: false,
	canAcceptDroppedRecords: true,
    canReorderRecords: true,

	fields:[
	    {name:"ID", title:"ID", width:"*"},
	    {name:"type", title:"Type", width:"*"}
	],

	selectionType:isc.Selection.SINGLE,

    // whether to automatically show parents of an added node (if applicable)
    autoShowParents:true
});

isc.EditTree.addMethods({
    initWidget : function () {
        this.Super("initWidget", arguments);
        this.setData(this.getEditNodeTree());
    },

    // Adding / Removing components in the tree
	// --------------------------------------------------------------------------------------------

    willAcceptDrop : function () {
        if (!this.Super("willAcceptDrop",arguments)) return false;
	    var recordNum = this.getEventRow(),
		    dropTarget = this.getDropFolder(),
            dragData = this.ns.EH.dragTarget.getDragData()
        ;
        
        if (dragData == null) return false;
        if (isc.isAn.Array(dragData)) {
            if (dragData.length == 0) return false;
            dragData = dragData[0];
        }
        
        if (dropTarget == null) dropTarget = this.data.getRoot();
        var dragType = dragData.type || dragData.className;

        this.logInfo("checking dragType: " + dragType + 
                     " against dropLiveObject: " + dropTarget.liveObject, "editing");

        return this.canAddToParent(dropTarget, dragType)
    },

    folderDrop : function (nodes, parent, index, sourceWidget) {
        if (sourceWidget != this && !sourceWidget.isA("Palette")) {
            // if the source isn't a Palette, do standard drop interaction
            return this.Super("folderDrop", arguments);
        }

        if (sourceWidget != this) {
            // this causes component creation since the drop is from a Palette
            nodes = sourceWidget.transferDragData();
        }

        var newNode = (isc.isAn.Array(nodes) ? nodes[0] : nodes);

        // flag that this node was dropped by a user
        newNode.dropped = true;

        this.logInfo("sourceWidget is a Palette, dropped node of type: " + newNode.type,
                     "editing");

        var editTree = this;
        this.requestLiveObject(newNode, function (node) {
            if (node == null) return;
            // self-drop: remove component from old location before re-adding
            var selfDrop = sourceWidget == editTree;
            if (selfDrop) {
                // If we're self-dropping to a slot further down in the same parent, this will
                // cause the index to become off by one
                var oldParent = this.data.getParent(newNode);
                if (parent == oldParent) {
                    var oldIndex = this.data.getChildren(oldParent).indexOf(newNode);
                    if (oldIndex != null && oldIndex <= index) index--;
                }
                editTree.removeNode(newNode);
            }
            
            editTree.addNode(node, parent, index);

            // special case tabs to add default pane
            if (
                !selfDrop && node && parent && 
                (node.type || node.className) == "Tab" && 
                (parent.type || parent.className) == "TabSet"
            ) {
                var liveTabSet = parent.liveObject;
                if (liveTabSet && liveTabSet.editProxy) liveTabSet.editProxy.addDefaultPane(node);
            }
        }, sourceWidget)
        
    },

    nodeAdded : function (newNode) {
        this.selection.selectSingle(newNode);
        if (this.autoShowParents) this.showParents(newNode);
        
        // Flip it into edit mode depending on the setting on the VB instance
        
        if (this.creator && this.creator.editingOn) this.enableEditing(newNode);
    },

    // for a node being added without a parent, find a plausible default node to add to.
    // In combination with palette.defaultEditContext, allows double-click (tree, list
    // palettes) as an alternative to drag and drop.
    getDefaultParent : function (newNode, returnNullIfNoSuitableParent) {
        // rules:
        // Start with the selected node. We select on drop / create, so this is typically
        // the last added node, but the user can select something else to take control of
        // where the double-click add goes
        // If this node accepts this type as a child, use that.
        // - handles most layout nesting, DataSource for last form, etc
        // Otherwise, go up hierarchy from this node
        // - handles a series of components that should not nest being placed adjacent instead,
        //   eg ListGrid then DynamicForm
        var type = newNode.type || newNode.className,
            node = this.getSelectedRecord();
        
        while (node && !this.canAddToParent(node, type)) node = this.data.getParent(node);
        
        var root = this.data.getRoot()
        if (returnNullIfNoSuitableParent) {
            if (!node && this.canAddToParent(root, type)) return root;
            return node;
        }
        return node || root;
    },
    
    // give a newNode, ensure all of it's parents are visible
    showParents : function (newNode) {
        // if something is dropped under a tab, ensure that tab gets selected
        var parents = this.data.getParents(newNode), 
            tabNodes = parents.findAll("type", "Tab");
        //this.logWarn("detected tab parents: " + tabNodes);
        if (tabNodes) {
            for (var i = 0; i < tabNodes.length; i++) {
                var tabNode = tabNodes[i],
                    tabSetNode = this.data.getParent(tabNode),
                    tab = this.getLiveObject(tabNode),
                    tabSet = this.getLiveObject(tabSetNode);
                tabSet.selectTab(tab);
            }
        }
    }
});

//> @groupDef visualBuilder
// The SmartClient Visual Builder tool is intended for:
// <ul>
// <li> business analysts and others doing functional application design, who want to create
// functional prototypes in a codeless, "what you see is what you get" environment
// <li> developers new to SmartClient who want to get a basic familiarity with component
// layout, component properties and SmartClient code structure
// <li> developers building simple applications that can be completed entirely within Visual
// Builder
// </ul>
// <P>
// <b>Visual Builder for Functional Design</b>
// <P>
// Visual Builder has several advantages over other tools typically used for functional design:
// <ul>
// <li> Visual Builder allows simple drag and drop manipulation of components, form-based
// editing of component properties, and simple connection of events to actions - all without
// requiring any code to be written.  It is actually simpler to use than
// DreamWeaver or other code-oriented prototyping tools
// <li> because Visual Builder generates clean code, designs will not have to be converted to
// another technology before development can proceed.  This reduces both effort and the
// potential for miscommunication
// <li> developers can add custom skinning, components with custom behaviors, and custom
// DataSources with sample datasets to Visual Builder so that the design environment is an even
// closer match to the final application.  This helps eliminate many types of unimplementable
// designs 
// <li> because Visual Builder is built in SmartClient itself, Visual Builder is simply a 
// web page, and does not require installation.  Visual Builder can be deployed to 
// an internal network to allow teams with a mixture of technical and semi-technical 
// users to collaboratively build and share prototypes of SmartClient-based applications.  
// </ul>
// <P>
// <h4>Launching &amp; Using Visual Builder</h4>
// <P>
// <smartclient>The SmartClient SDK already has Visual Builder installed - access
// it from the SDK Explorer under Tools -&gt; Visual Builder (see QuickStart Guide for how to
// access the SDK Explorer).
// </smartclient>
// <smartgwt>Instructions for launching Visual Builder are in the 
// +externalLink{http://forums.smartclient.com/showthread.php?t=8159#aVisualBuilder,Smart GWT FAQ}.
// </smartgwt>
// <P>
// Basic usage instructions are embedded in Visual Builder itself, in the "About Visual
// Builder" pane.  Click on it to open it.
// <P>
// <b>Loading and Saving</b>
// <P>
// The "Project" pane within Visual Builder allows screens to be saved and reloaded for further
// editing.  Saved screens <b>can</b> be edited outside of Visual Builder and successfully
// reloaded, however, as with any design tool that provides a drag and drop, dialog-driven
// approach to screen creation, Visual Builder cannot work with entirely free-form code.  In
// particular, when a screen is loaded and then re-saved:
// <ul>
// <li> any indenting or spacing changes are not preserved 
// <li> order of property or method definitions will revert to Visual Builder's default
// <li> while method definitions on components are preserved, any code <b>outside of</b>
//      component definitions will be dropped (in some cases adding such code will cause
//      loading to fail)
// <li> each Canvas-based component will be output separately, in the order these components
//      appear in the project tree, deepest first
// </ul>
// Generally speaking, screen definitions that you edit within Visual Builder should consist of
// purely declarative code.  Rather than appearing in screen definitions, custom components and
// JavaScript libraries should be added to Visual Builder itself via the customization
// facilities described below.
// <P>
// <smartclient>
// <!-- applies only to SmartClient since SmartGWT has a GWT module listing these resources -->
// <h4>Installing Visual Builder</h4>
// <P>
// Visual Builder comes already installed and working in the SDK, and can be used from there out
// of the box.  This is the simplest thing to do during initial prototyping.
// <P>
// Further on in the development cycle, it may be advantageous to have Visual Builder available 
// outside the SDK, for example in your test environment.  Installing Visual Builder into 
// such an environment is very easy:
// <ul>
// <li>Perform a normal installation procedure, as discussed +link{group:iscInstall,here}</li>
// <li>Copy the following .jar files from the SDK <code>lib</code> folder to the target 
// <code>WEB-INF/lib</code> folder: 
// <ul>
// <li><code>isomorphic_tools.jar</code></li>
// <li><code>isomorphic_sql.jar</code></li>
// <li><code>isomorphic_hibernate.jar</code></li>
// </ul></li>
// <li>Copy the SDK <code>tools</code> folder to the target application root</li>
// </ul>
// Note that it is safe to include Visual Builder even in a production environment, so long 
// as you ensure that the <code>tools</code> folder is protected with any normal HTTP
// authentication/authorization mechanism - for example, an authentication filter.
// </smartclient>
// <P>
// <h4>Customizing Visual Builder</h4>
// <P>
// The rest of this topic focuses on how Visual Builder can be customized and deployed by
// developers to make it more effective as a functional design tool for a particular
// organization.
// <P>
// <b>Adding Custom DataSources to Visual Builder</b>
// <P>
// DataSources placed in the project dataSources directory ([webroot]/shared/ds by default)
// will be detected by Visual Builder whenever it is started, and appear in the DataSource
// listing in the lower right-hand corner automatically.
// <P>
// If you have created a custom subclass of DataSource (eg, as a base class for several
// DataSources that contact the same web service), you can use it with Visual Builder by:
// <ul>
// <li> creating an XML version of the DataSource using the XML tag &lt;DataSource&gt; and the
// <code>constructor</code> property set to the name of your custom DataSource subclass (as
// described +link{group:componentXML} under the heading <i>Custom Components</i>)
// <li> modifying [webroot]/tools/visualBuilder/globalDependencies.xml to load the JavaScript
// code for your custom DataSource class.  See examples in that file.
// </ul>
// <P>
// <b>Adding Custom Components to Visual Builder</b>
// <P>
// The Component Library on the right hand side of Visual Builder loads component definitions
// from two XML files in the [webroot]/tools/visualBuilder directory: customComponents.xml and
// defaultComponents.xml.  customComponents.xml is empty and is intended for developers to add
// their own components.  defaultComponents.xml can also be customized, but the base version
// will change between SmartClient releases.
// <P>
// As can be seen by looking at defaultComponents.xml, components are specified using a tree
// structure similar to that shown in the 
// +explorerExample{treeLoadXML,tree XML loading example}.  The properties that can be set on
// nodes are:
// <ul>
// <li> <code>type</code>: name of the SmartClient Class on which +link{Class.create,create()} will be
// called in order to construct the component.  <code>type</code> can be omitted to create
// a folder that cannot be dropped
// <li> <code>title</code>: title for the node
// <li> <code>defaults</code>: an Object specifying defaults to be passed to
// +link{Class.create,create()}.
// For example, you could add an "EditableGrid" node by using <code>type: "ListGrid"</code>
// and specifying:
// <pre>
// &lt;defaults canEdit="true"/&gt;</pre>
// NOTE: if you set any defaults that are not Canvas properties, you need to provide explicit
// type as documented under <i>Custom Properties</i> for +link{group:componentXML}.
// <li> <code>children</code>: components that should appear as children in the tree under this
// node
// <li> <code>icon</code>: icon to show in the Visual Builder component tree (if desired)
// <li> <code>iconWidth/Height/Size</code>: dimensions of the icon in pixels ("iconSize" sets
// both)
// <li> <code>showDropIcon</code>: for components that allow children, whether to show a
// special drop icon on valid drop (like +link{treeGrid.showDropIcons}).
// </ul>
// <P>
// In order to use custom classes in Visual Builder, you must modify
// <code>[webroot]/tools/visualBuilder/globalDependencies.xml</code> to include:
// <ul>
// <li> the JavaScript class definition for the custom class (in other words, the
// +link{classMethod:isc.defineClass(),defineClass()} call)
// <li> a +link{group:componentSchema,component schema} for the custom component
// </ul>
// See globalDependencies.xml for examples.
// <P>
// <h4>Component Schema and Visual Builder</h4>
// <P>
// When you provide +link{group:componentSchema,custom schema} for a component, Visual Builder
// uses that schema to drive component editing (Component Properties pane) and to drive drag
// and drop screen building functionality.
// <P>
// <b>Component Editing</b>
// <P>
// Newly declared fields will appear in the Component Editor in the "Other" category at the
// bottom by default.  You can create your own category by simply setting field.group to the
// name of a new group and using this on multiple custom fields.
// <P>
// The ComponentEditor will pick a FormItem for a custom field by the
// +link{type:FormItemType,same rules} used for ordinary databinding, including the ability to
// set field.editorType to use a custom FormItem.
// <P>
// When the "Apply" button is clicked, Visual Builder will look for an appropriate "setter
// function" for the custom field, for example, for a field named "myProp", Visual Builder will
// look for "setMyProp".  The target component will also be +link{canvas.redraw,redrawn}.
// <P>
// <b>Event -&gt; Action Bindings</b>
// <P>
// The Component Properties pane contains an Events tab that allows you wire components events
// to actions on any other component currently in the project.
// <P>
// Events are simply +link{group:stringMethods,StringMethods} defined on the component.  In
// order to be considered events, method definitions must have been added to the class via
// +link{Class.registerStringMethods} and either be publicly documented SmartClient methods or,
// for custom classes, have a methods definition in the +link{group:componentSchema,component schema}.
// Examples of events are: +link{listGrid.recordClick} and +link{dynamicForm.itemChange}.
// <P>
// Actions are methods on any component that have a method definition in the
// +link{group:componentSchema,component schema} and specify action="true".
// <P>
// All available events (stringMethods) on a component are shown in the Events tab of the
// Component Editor.  Clicking the plus (+) sign next to the event name brings up a menu that
// shows a list of all components currently in the project and their available actions.
// Selecting an action from this submenu binds the action to the selected event.  When an event
// is bound to an action in this manner, automatic type matching is performed to pass arguments
// from the event to the action as follows:
// <ul>
// <li>Only non-optional parameters of the action are bound.
// <li>For each non-optional parameter of the action method, every parameter of the
// event method is inspected in order to either directly match the type (for non-object types)
// or to match an isAssignableFrom type check via a SmartClient schema inheritance check.
// <li>The 'type' of a parameter is determined from the type documented in the SmartClient
// reference for built-in components, or from the <code>type</code> attribute on the method
// param in the +link{group:componentSchema,component schema} definition of a custom component.
// <li>When a matching parameter is found, it is assigned to the current slot of the action and
// not considered for further parameter matching.
// <li>The above pattern is repeated until all non-optional parameters are exhausted, all
// event parameters are exhausted, or until no further type matches can be inferred.
// </ul>
// The "actionBinding" log category can be enabled in the Developer Console to troubleshoot
// issues with automatic binding for custom methods.
// <P>
// <b>Component Drag and Drop</b>
// <P>
// Visual Builder uses component schema to determine whether a given drop is allowed and what
// methods should be called to accomplish the drop.  For example, any Canvas-based component
// can be dropped on a VLayout because VLayout has a "members" field of type "Canvas", and an 
// +link{Layout.addMember,addMember()} function.
// <P>
// Because of these rules, any subclass of Canvas will be automatically eligible to be dropped
// into any container that accepts a Canvas (eg, a Layout or Tab).  Any subclass of a FormItem
// will be, likewise, automatically eligible to be dropped into a DynamicForm.
// <P>
// You can declare custom containment relations, such as a custom class "Wizard" that accepts
// instances of the custom class "Pane" by simply declaring a
// +link{group:componentSchema,component schema} that says that Wizard has a property called
// "panes" of type "Pane".  Then, provide methods that allow components to be added and removed:
// <ul>
// <li> for a +link{dataSourceField.multiple,multiple} field, provide "add" and "remove"
// functions based on the name of the field.  For example, for a field "panes" of type "Pane",
// provide "addPane()" that takes a Pane instance, and "removePane()" that takes a pane
// instance or pane ID 
// <li> for a singular field (such as +link{Canvas.contextMenu} or +link{Tab.pane}), provide a
// setter method named after the field (eg setContextMenu()) that takes either an instance of
// the component or null for removal
// </ul>
// <P>
// The "editing" log category can be enabled in the Developer Console to troubleshoot issues
// with schema-driven drag and drop and automatic lookup of getter/setter and adder/remover
// methods.
// <P>
// <B>NOTE:</B> after modifying component schema, it may be necessary to restart the servlet
// engine and reload Visual Builder
// <P>
// <b>Presenting simplified components</b>
// <P>
// SmartClient components expose many methods and properties.  For some environments, it is
// more appropriate to provide a simplified list of properties, events, and actions on either
// built-in SmartClient components or your custom components.  This can be done by providing a
// custom +link{group:componentSchema,component schema} for an existing component that exposes
// your minimal set.  You also need to provide a trivial subclass of the class you're exposing
// so that it can be instantiated.
// <P>
// For example, let's say you want to make a simplified button called EButton that exposes only
// the 'title' property and the 'click' event of a standard Button.  The following steps will
// accomplish this:
// <p>
// 1. Edit /tools/visualBuilder/customComponents.xml and add a block similar to the following
// to make your custom component appear in the Component Library:
// <pre>
// &lt;PaletteNode&gt;
//     &lt;title&gt;EButton&lt;/title&gt;
//     &lt;type&gt;EButton&lt;/type&gt;
//     &lt;icon&gt;button.gif&lt;/icon&gt;
// &lt;/PaletteNode&gt;
// </pre>
// 2. Next, create a custom schema: /isomorphic/system/schema/EButton.ds.xml as follows:
// <pre>
// &lt;DataSource ID="EButton" inheritsFrom="Button" Constructor="EButton"
//             showLocalFieldsOnly="true" showSuperClassActions="false"
//             showSuperClassEvents="false"&gt;
// 	   &lt;fields&gt;
//         &lt;field name="title"  type="HTML"/&gt;
//     &lt;/fields&gt;
//     &lt;methods&gt;
//         &lt;method name="click"&gt;
//             &lt;description&gt;Fires when this button is clicked.&lt;/description&gt;
//         &lt;/method&gt;
//     &lt;/methods&gt;
// &lt;/DataSource&gt;
// </pre>
// See documentation above and also +link{group:componentSchema,component schema} for what the
// properties above do.
// 3.  Finally, you'll need to define an EButton class as a simple subclass of Button, as
// follows:
// <pre>
// isc.defineClass("EButton", "Button");
// </pre>
// To make sure that the Visual Builder will load the above definition, you'll need to place it
// into a JavaScript file being loaded by the Visual Builder.  If you do not already have
// such a file, you can create one and add it to the list of Visual Builder dependencies by
// adding an entry in /tools/visualBuilder/globalDependencies.xml.  See examples in that file
// for specifics.
//
// @see group:toolsDeployment
//
// @treeLocation Concepts
// @title Visual Builder
// @visibility external
//<

} // end if (isc.TreeGrid)


// -----------------------------------------------------------------------------------------
// DynamicForm.rolloverControls

// INCOMPLETE IMPLEMENTATION - commented out for now
/*
isc.DynamicForm.addProperties({
    rolloverControlsLayoutDefaults: [],
    rolloverControls: []
    
});

isc.DynamicForm.addMethods({
    showRolloverControls : function (item) {
        var controls = this.getRolloverControls(item),
            layout = this.rolloverControlsLayout;
        layout.item = item;
        layout.setPageLeft();
        layout.moveTo(item.getPageLeft()+item.getPageWidth(), item.getPageTop());
    },
    hideRolloverControls : function (item) {
        this.rolloverControlsLayout.hide();
    },
    getRolloverControls : function (item) {
        if (!this.rolloverControlsLayout) {
            this.createRolloverControls(item);
        }

        return this.rolloverControls;
    },
    createRolloverControls : function (item) {
        this.addAutoChild("rolloverControlsLayout");
        this.createRolloverControls(item);
    }
});
*/

// This is a marker class for FormItem drag-and-drop in edit mode.  We use an instance of 
// this class (for efficiency, we just keep one cached against the EditContext class) so 
// that the DnD code knows we're really dragging a FormItem, which will be present on this 
// proxy canvas as property "formItem".
isc.ClassFactory.defineClass("FormItemProxyCanvas", "Canvas");

isc.FormItemProxyCanvas.addProperties({
    editProxyConstructor:"FormItemEditProxy",

    autoDraw: false,
    canDrop: true,
    setFormItem : function (formItem) {
        this.formItem = formItem;
        this.syncWithFormItemPosition();
        this.sendToBack();
        this.show();
    },
    syncWithFormItemPosition : function () {
        if (!this.formItem || !this.formItem.form) return; // formItem not yet part of a form?
        this.setPageLeft(this.formItem.getPageLeft());
        this.setPageTop(this.formItem.getPageTop());
        this.setWidth(this.formItem.getVisibleWidth());
        this.setHeight(this.formItem.getVisibleHeight());
    }
});




//> @attr paletteNode.canvasDefaults (Canvas Properties : null : IR)
// @include paletteNode.defaults
// @visibility sgwt
//<
//> @attr paletteNode.formItemDefaults (FormItem Properties : null : IR)
// @include paletteNode.defaults
// @visibility sgwt
//<
//> @attr paletteNode.drawItemDefaults (DrawItem Properties : null : IR)
// @include paletteNode.defaults
// @visibility sgwt
//<
//> @attr paletteNode.canvasLiveObject (Canvas : null : IR)
// @include paletteNode.liveObject
// @visibility sgwt
//<
//> @attr paletteNode.formItemLiveObject (FormItem : null : IR)
// @include paletteNode.liveObject
// @visibility sgwt
//<
//> @attr paletteNode.drawItemLiveObject (DrawItem : null : IR)
// @include paletteNode.liveObject
// @visibility sgwt
//<

//> @attr editNode.canvasDefaults (Canvas Properties : null : IR)
// @include editNode.defaults
// @visibility sgwt
//<
//> @attr editNode.formItemDefaults (FormItem Properties : null : IR)
// @include editNode.defaults
// @visibility sgwt
//<
//> @attr editNode.drawItemDefaults (DrawItem Properties : null : IR)
// @include editNode.defaults
// @visibility sgwt
//<
//> @attr editNode.canvasLiveObject (Canvas : null : IR)
// @include editNode.liveObject
// @visibility sgwt
//<
//> @attr editNode.formItemLiveObject (FormItem : null : IR)
// @include editNode.liveObject
// @visibility sgwt
//<
//> @attr editNode.drawItemLiveObject (DrawItem : null : IR)
// @include editNode.liveObject
// @visibility sgwt
//<



if (!(isc.licenseType == "Enterprise" || isc.licenseType == "Eval" || isc.licenseType.contains("licenseType"))) {
    
    [
        "EditContext", "Palette", "HiddenPalette", "TreePalette", "ListPalette", "TilePalette",
        "MenuPalette", "EditPane", "EditTree", "FormItemProxyCanvas"
    ].map(function (editModeClass) {
        isc[editModeClass]._vbOnly = true;
    });
}

//> @groupDef toolsDeployment
// SmartClient provides a number of tools:
// <ul> 
// <li> +link{group:adminConsole}
// <li> +link{group:visualBuilder}
// <li> +link{group:balsamiqImport}
// </ul>
// <P>
// To deploy the tools simply copy the <code>tools</code> directory into your deployment. There are no
// additional settings to configure.
// <P>
// <h4>Security</h4>
// <P>
// These tools are, by default, available to anyone and enable access to all "BuiltinRPCs"
// and the Filesystem DataSource so they should only be deployed into a trusted environment.
// Alternately, the tools can easily be restricted to administrators or end users
// by protecting the <code>tools</code> path with normal authentication and authorization
// mechanisms on the web server.
// <P>
// More fine-grained access control can be installed by updating each tool's <code>xxxOperations.jsp</code>
// file (ex. tools/adminConsoleOperations.jsp, tools/visualBuilder/vbOperations.jsp). These files are
// responsible for enabling builtinRPC and FileSystem DataSource access. Individual
// BuiltinRPC methods can be restricted, for example, such that some users are allowed to load screens but
// not save any changes. See comments within each file for an example of restricting this access.
// See the server-side Javadocs for methods provided by <code>BuiltinRPC</code>.
// <P>
// Note that the tools provides a "live" interface to the provided DataSources. In
// other words, if a DataSource supports saving and a tool enables editing, real saves will be
// initiated. 
//
// @title Tools Deployment
// @treeLocation Concepts/Deploying SmartClient
// @visibility external
//<
