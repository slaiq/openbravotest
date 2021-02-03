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



isc.defineClass("GridEditProxy", "LayoutEditProxy").addProperties({

    // Attributes to control which direct grid interactions persist
    // changes into the editMode defaults
    // ---------------------------------------------------------------------------------------

    //> @attr gridEditProxy.saveFieldVisibility  (Boolean : true : IR)
    // Should live-grid field visibility changes be persisted?
    //<
    saveFieldVisibility: true,

    //> @attr gridEditProxy.saveFieldFrozenState  (Boolean : true : IR)
    // Should live-grid field freeze state be persisted?
    //<
    saveFieldFrozenState: true,

    //> @attr gridEditProxy.saveSort  (Boolean : true : IR)
    // Should live-grid sort state be persisted?
    //<
    saveSort: true,

    //> @attr gridEditProxy.saveGroupBy  (Boolean : true : IR)
    // Should live-grid grouping state be persisted?
    //<
    saveGroupBy: true,

    //> @attr gridEditProxy.saveFilterCriteria  (Boolean : true : IR)
    // Should live-grid filter editor criteria be persisted?
    //<
    saveFilterCriteria: true,


    // Attributes to control which direct grid interactions are allowed
    // in editMode even when the grid defaults for matching attributes
    // are disabled.
    // ---------------------------------------------------------------------------------------

    //> @attr gridEditProxy.canEditHilites  (Boolean : true : IR)
    // Can highlights be edited from header context menu?
    //<
    canEditHilites: true,

    //> @attr gridEditProxy.canAddFormulaFields  (Boolean : true : IR)
    // Can new formula fields be created from header context menu?
    // Overrides +link{ListGrid.canAddFormulaFields) when in edit mode.
    //<
    canAddFormulaFields: true,

    //> @attr gridEditProxy.canAddSummaryFields  (Boolean : true : IR)
    // Can new summary fields be created from header context menu?
    // Overrides +link{ListGrid.canAddSummaryFields) when in edit mode.
    //<
    canAddSummaryFields: true,

    //> @attr gridEditProxy.canGroupBy  (Boolean : true : IR)
    // Can records be grouped from header context menu?
    // Overrides +link{ListGrid.canGroupBy) when in edit mode.
    //<
    canGroupBy: true,

    //> @attr gridEditProxy.canReorderFields  (Boolean : true : IR)
    // Indicates whether fields in this listGrid can be reordered by dragging and
    // dropping header fields. 
    // Overrides +link{ListGrid.canReorderFields) when in edit mode.
    //<
    canReorderFields: true,

    //> @attr gridEditProxy.canResizeFields  (Boolean : true : IR)
    // Indicates whether fields in this listGrid can be resized by dragging header
    // fields.
    // Overrides +link{ListGrid.canResizeFields) when in edit mode.
    //<
    canResizeFields: true,


    // Attributes to control hilite and formula edit results
    // ---------------------------------------------------------------------------------------

    //> @attr gridEditProxy.generateEditableHilites  (Boolean : true : IR)
    // Are highlights created from header context menu runtime editable
    // when not in editMode?
    //
    // @see hilite.canEdit
    //<
    generateEditableHilites: true,

    //> @attr gridEditProxy.generateEditableFormulas  (Boolean : true : IR)
    // Are formula fields created from header context menu runtime editable
    // when not in edit mode?
    //
    // @see listGridField.canEditFormula
    //<
    generateEditableFormulas: true,

    //> @attr gridEditProxy.generateEditableSummaries  (Boolean : true : IR)
    // Are summary fields created from header context menu runtime editable
    // when not in edit mode?
    //
    // @see listGridField.canEditSummary
    //<
    generateEditableSummaries: true
});

isc.GridEditProxy.addMethods({

    setEditMode : function (editingOn) {
        this.Super("setEditMode", arguments);

        if (editingOn) this.observeStateChanges();
        else this.ignoreStateChanges();

        if (isc.isA.TreeGrid) {
            if (editingOn) {
                // If the TG was not databound and had an empty fieldset at initWidget time, it 
                // will have created a default treeField which now appears in its fields property
                // as if it were put there by user code.  We need to detect this circumstance and
                // create a TreeGridField node in the projectComponents tree so the user can 
                // manipulate this auto-generated field
                this.createDefaultTreeFieldEditNode();
            }
        }
    },

    getOverrideProperties : function () {
        var properties = this.Super("getOverrideProperties", arguments);
        return isc.addProperties({}, properties, {
            clearNoDropIndicator: this.clearNoDropIndicator,
            setNoDropIndicator: this.setNoDropIndicator,
            addField: this.addField,
            userAddedField: this.userAddedField
        });
    },

    // Grid state change observers
    // Used to update defaults for Component XML serialization
    // ---------------------------------------------------------------------------------------

    // User-added fields are captured by userAddedField() override

    

    observeStateChanges : function () {
        var liveObject = this.creator;
        if (this.saveSort) this.observe(liveObject, "sortChanged");
        this.observe(liveObject, "fieldStateChanged");
        if (this.saveGroupBy) this.observe(liveObject, "groupStateChanged");
        this.observe(liveObject, "editHilites");
        this.observe(liveObject, "hilitesChanged");
        if (this.saveFilterCriteria) this.observe(liveObject, "filterEditorSubmit");
    },

    ignoreStateChanges : function () {
        var liveObject = this.creator;
        if (this.saveSort) this.ignore(liveObject, "sortChanged");
        this.ignore(liveObject, "fieldStateChanged");
        if (this.saveGroupBy) this.ignore(liveObject, "groupStateChanged");
        this.ignore(liveObject, "editHilites");
        this.ignore(liveObject, "hilitesChanged");
        if (this.saveFilterCriteria) this.ignore(liveObject, "filterEditorSubmit");
    },

    sortChanged : function () {
        var liveObject = this.creator,
            sort = liveObject.getSort()
        ;

        this.addDefaultFieldEditNodes();

        if (!sort) liveObject.editContext.removeNodeProperties(liveObject.editNode, [ "initialSort" ], true);
        else liveObject.editContext.setNodeProperties(liveObject.editNode, { initialSort: sort }, true);
    },

    fieldStateChanged : function () {
        // One of the following field states changed:
        // - Order
        // - Width
        // - Visibility
        // - Frozen
        // - Sort order (handled in sortChanged)

        this.addDefaultFieldEditNodes();

        // Apply field order changes
        var liveObject = this.creator,
            allFields = liveObject.getAllFields()
        ;
        if (allFields) {
            for (var i = 0; i < allFields.length; i++) {
                var field = allFields[i],
                    editNode = this.getFieldNode(i)
                ;
                if (editNode) {
                    var gridFieldName = field[liveObject.fieldIdProperty],
                        nodeFieldName = editNode.liveObject[liveObject.fieldIdProperty]
                    ;

                    if (gridFieldName != nodeFieldName) {
                        var fieldIndex = this.getFieldNodeIndexByName(gridFieldName),
                            nodeIndex = this.getFieldNodeIndexByName(nodeFieldName)
                        ;
                        if (nodeIndex != null && nodeIndex != fieldIndex) {
                            liveObject.editContext.reorderNode (liveObject.editNode, fieldIndex, nodeIndex);
                        }
                    }
                }
            }
        }

        // Apply basic (width, visibility and frozen) properties
        if (allFields) {
            for (var i = 0; i < allFields.length; i++) {
                var field = allFields[i],
                    editNode = this.getFieldNode(i)
                ;

                // defensive null check
                if (!field || field.excludeFromState || !editNode || !editNode.defaults) continue;
                
                var fieldName = field[liveObject.fieldIdProperty],
                    fieldState = liveObject.getStateForField(fieldName, false)
                ;

                if (this.saveFieldVisibility) {
                    var hidden = (fieldState.visible == false);
                    if (!hidden) liveObject.editContext.removeNodeProperties(editNode, [ "hidden" ], true);
                    else liveObject.editContext.setNodeProperties(editNode, { hidden: hidden }, true);
                }

                if (this.saveFieldFrozenState) {
                    var frozen = fieldState.frozen;
                    if (!frozen) liveObject.editContext.removeNodeProperties(editNode, [ "frozen" ], true);
                    else liveObject.editContext.setNodeProperties(editNode, { frozen: frozen }, true);
                }

                var width = fieldState.width;
                if (!width) liveObject.editContext.removeNodeProperties(editNode, [ "width" ], true);
                else liveObject.editContext.setNodeProperties(editNode, { width: width }, true);

                
            }
        }
    },

    groupStateChanged : function ()  {
        var liveObject = this.creator,
            groupFields = liveObject.getGroupByFields()
        ;

        if (!groupFields) liveObject.editContext.removeNodeProperties(liveObject.editNode, "groupByField" );
        else liveObject.editContext.setNodeProperties(liveObject.editNode, { groupByField: groupFields }, true);

        // Apply grouping details to fields. Start by removing any existing group properties.
        this.addDefaultFieldEditNodes();
        var allFields = liveObject.getAllFields(),
            propertiesToClear = ["groupingMode", "groupGranularity", "groupPrecision"]
        ;
        for (var i = 0; i < allFields.length; i++) {
            var editNode = this.getFieldNode(i);
            liveObject.editContext.removeNodeProperties(editNode, propertiesToClear);
        }

        if (groupFields != null) {
            for (var i = 0; i < groupFields.length; i++) {
                var field = allFields.find("name", groupFields[i]);
                if (field) {
                    var fieldNum = allFields.indexOf(field),
                        editNode = this.getFieldNode(fieldNum),
                        groupProperties = {}
                    ;

                    if (field.groupingMode) groupProperties.groupingMode = field.groupingMode;
                    if (field.groupGranularity) groupProperties.groupGranularity = field.groupGranularity;
                    if (field.groupPrecision) groupProperties.groupPrecision = field.groupPrecision;
                    liveObject.editContext.setNodeProperties(editNode, groupProperties, true);
                }
            }
        }
    },

    editHilites : function () {
        // To properly mark new hilites with editable status
        // we must know which hilites are new. We handle that by grabbing
        // a copy of the existing hilites when editing starts. This list
        // can be compared against the updated list when changes are reported.
        this._origHilites = this.creator.getHilites();
    },

    hilitesChanged : function () {
        var liveObject = this.creator,
            hilites = liveObject.getHilites()
        ;
        if (!hilites) {
            liveObject.editContext.removeNodeProperties(liveObject.editNode, [ "hilites" ], true);
        } else {
            // New hilites that are cannot be user-editable outside
            // edit mode must be properly marked. To do this we must
            // determine which hilites are new.
            if (!this.generateEditableHilites) {
                for (var i = 0; i < hilites.length; i++) {
                    var hilite = hilites[i],
                        origExists = (this._origHilites ? this._origHilites.contains(hilite) : false)
                    ;
                    if (!origExists) {
                        hilite.canEdit = false;
                    }
                }
            }

            liveObject.editContext.setNodeProperties(liveObject.editNode, { hilites: hilites }, true);
        }
    },

    filterEditorSubmit : function (criteria) {
        var liveObject = this.creator;

        if (!criteria) liveObject.editContext.removeNodeProperties(liveObject.editNode, [ "initialCriteria" ], true);
        else liveObject.editContext.setNodeProperties(liveObject.editNode, { initialCriteria: criteria }, true);
    },

    // Method/Event overrides for grid
    // ---------------------------------------------------------------------------------------

    // Canvas editProxy.clearNoDropindicator no-ops if the internal _noDropIndicator flag is null.  This
    // isn't good enough in edit mode because a canvas can be dragged over whilst the no-drop
    // cursor is showing, and we want to revert to a droppable cursor regardless of whether 
    // _noDropIndicatorSet has been set on this particular canvas. 
    clearNoDropIndicator : function (type) {
        this.Super("clearNoDropIndicator", arguments);
        if (this.body.editProxy) {
            this.body.editProxy.clearNoDropIndicator();
        }
    },

    // Special editMode version of setNoDropCursor - again, because the base version no-ops in 
    // circumstances where we need it to refresh the cursor.
    setNoDropIndicator : function () {
        this.Super("setNoDropIndicator", arguments);
        if (this.body.editProxy) {
            this.body.editProxy.setNoDropIndicator();
        }
    },

    headerClick : function (fieldNum) {
        // Select the corresponding ListGridField
        var liveObject = this.creator,
            node = this.getFieldNode(fieldNum)
        ;

        if (node) {
            node.liveObject._visualProxy = liveObject.header.getButton(fieldNum);
            isc.EditContext.selectCanvasOrFormItem(node.liveObject);
        }

        liveObject._headerClickFired = true;
        return isc.EH.STOP_BUBBLING;
    },

    // HACK: We ideally want a header click to stop event bubbling at that point, but it seems 
    // that returning STOP_BUBBLING from the headerClick() method does not prevent the ListGrid's
    // click event from firing, so the object selection is superseded.  To work around this, we 
    // maintain a flag on the LG that headerClick has been fired, which this click() impl tests
    // and then clears
    click : function () {
        var liveObject = this.creator;
        if (liveObject.editNode) {
            if (liveObject._headerClickFired) delete liveObject._headerClickFired;
            else isc.EditContext.selectCanvasOrFormItem(liveObject, true);
            return isc.EH.STOP_BUBBLING;
        }
    },

    userAddedField : function (field) {
        // A new user field does not yet exist in the editTree and must
        // be created so it can be persisted as Component XML.
        var editNode = this.editProxy.getFieldNodeByName(field.name);

        if (editNode) {
            var properties = {
                title: field.title,
                userFormula: field.userFormula
            };
            
            this.editContext.setNodeProperties(editNode, properties, true);
        } else {
            // Validators are automatically applied based on field type -
            // no need to save them.
            field = isc.addProperties({}, field);
            delete field.validators;

            if (field.userFormula && !this.generateEditableFormulas) field.canEditFormula = false;
            if (field.userSummary && !this.generateEditableSummaries) field.canEditSummary = false;

            var paletteNode = { type: "ListGridField", defaults: field },
                parentNode = this.editNode
            ;
            editNode = this.editContext.makeEditNode(paletteNode);
            this.editContext.addNode(editNode, parentNode);
        }
    },

    // Returns field matching fieldNum in grid. Skips any non-field
    // nodes during search.
    getFieldNode : function (fieldNum) {
        // Select the corresponding ListGridField
        var liveObject = this.creator,
            tree = liveObject.editContext.getEditNodeTree(),
            children = tree.getChildren(tree.findById(liveObject.ID)),
            node
        ;
        // Note that a non-field object (like DataSource) can be a child
        // of the ListGrid node so we cannot just index
        // into the child array by the fieldNum. 
        var shift = 0;
        for (var i = 0; i < children.length; i++) {
            var child = children[i];
            if (child.type != "ListGridField") {
                shift--;
                continue;
            }
            if (i + shift == fieldNum) {
                node = child;
                break;
            }
        }

        return node;
    },

    // Returns field edit node for field by name
    getFieldNodeByName : function (name) {
        // Select the corresponding ListGridField
        var liveObject = this.creator,
            tree = liveObject.editContext.getEditNodeTree(),
            children = tree.getChildren(tree.findById(liveObject.ID)),
            node
        ;
        for (var i = 0; i < children.length; i++) {
            var child = children[i];
            if (child.name == name) {
                node = child;
                break;
            }
        }
        return node;
    },

    // Returns actual node index for field
    getFieldNodeIndexByName : function (name) {
        // Select the corresponding ListGridField
        var liveObject = this.creator,
            tree = liveObject.editContext.getEditNodeTree(),
            children = tree.getChildren(tree.findById(liveObject.ID)),
            index
        ;
        for (var i = 0; i < children.length; i++) {
            var child = children[i];
            if (child.name == name) {
                index = i;
                break;
            }
        }
        return index;
    },

    addDefaultFieldEditNodes : function () {
        var liveObject = this.creator;

        // If first field does not have an editNode assume none of
        // the fields do. This condition occurs when a grid is added to
        // an editMode parent by default. However, when making changes
        // that should be persisted the field editNodes are needed.
        // Create them here.
        if (!this.getFieldNode(0)) {
            var allFields = liveObject.getAllFields();
            for (var i = 0; i < allFields.length; i++) {
                var field = allFields[i],
                    fieldConfig = liveObject.editProxy.getFieldEditNode(field, liveObject.getDataSource()),
                    editNode = liveObject.editContext.makeEditNode(fieldConfig)
                ;
                liveObject.editContext.addNode(editNode, liveObject.editNode, i, null, true);
            }
        }
    },

    // TreeGrid proxy
    // ---------------------------------------------------------------------------------------
    
    createDefaultTreeFieldEditNode : function () {
    
        // If we're loading a view, the default nodeTitle is going to be destroyed before the
        // user sees it, so just bail
        if (isc._loadingNodeTree) return;

        var liveObject = this.creator;

        // If this TG is databound, we presumably haven't created a default nodeTitle; this 
        // being the case, let's bail now so that we don't remove a real user field just 
        // because it happens to be called "nodeTitle"
        if (liveObject.dataSource) return;
        
        var fields = liveObject.fields;
        if (!fields) return;
        for (var i = 0; i < fields.length; i++) {
            if (fields[i].name == "nodeTitle") {
                var config = {
                    type: "TreeGridField",
                    autoGen: true,
                    defaults: {
                        name: fields[i].name,
                        title: fields[i].title
                    }
                };
                var editNode = liveObject.editContext.makeEditNode(config);
                liveObject.editContext.addNode(editNode, liveObject.editNode, null, null, true);
                return;
            }
        }
    },
    
    // Overriding the DBC implementation because we need to treat the field being added as a
    // special case if it has treeField set - there can only be one treeField, so we must 
    // remove the extant one.  This could only really happen during Load View (unless we were
    // to change the default for treeField to true in the component palette), so we will just
    // hand the call on if we're not in loading mode
    addField : function (field, index) {
        this.creator.Super("addField", arguments);

        if (isc._loadingNodeTree) {
            if (field.treeField) {
                var fields = this.creator.getFields();
                for (var i = 0; i < fields.length; i++) {
                    if (fields[i].name != field.name && fields[i].treeField) {
                        this.creator.removeField(fields[i]);
                        break;
                    }
                }
            }
        }
    },


    // TreeGrid needs a special implementation of this method because binding a TreeGrid really 
    // means binding the one field in the DataSource that represents the tree; with other DBC's,
    // we bind all the visible fields
    setDataSource : function (dataSource, fields, forceRebind) {
//        this.logWarn("gridEditProxy.setDataSource called" + isc.Log.getStackTrace());

        // For a ListGrid just use base implementation
        if (!isc.isA.TreeGrid(liveObject)) {
            this.Super("setDataSource", arguments);
            return;
        }

        // _loadingNodeTree is a flag set by Visual Builder - its presence indicates that we are 
        // loading a view from disk.  In this case, we do NOT want to perform the special 
        // processing in this function, otherwise we'll end up with duplicate components in the
        // componentTree.  
        // However, TreeGrid needs special treatment because it auto-creates a treeField if it 
        // is not passed a list of fields to use.  Since we'll be adding the fields one at a 
        // time during View Load, we start out with no fields, so a default will be created.
        // 
        if (isc._loadingNodeTree) {
            this.creator.setDataSource(dataSource, fields);
            return;
        }

        var liveObject = this.creator;

        if (dataSource == null) return;
        if (dataSource == liveObject.dataSource && !forceRebind) return;
            
        var fields = liveObject.getFields();
        
        // remove just the field currently marked treeField: true - in many use cases, this
        // will be the only field in the TreeGrid anyway
        if (fields) {
            for (var i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (field.treeField) {
                    field.treeField = null;
                    var nodeToRemove = field.editNode;
                    break;
                }
            }
        }
        
        var existingFields = liveObject.getFields();
        existingFields.remove(field);
        
        // If this dataSource has a single complex field, use the schema of that field in lieu
        // of the schema that was dropped.
        var schema,
            fields = dataSource.fields;
        if (fields && isc.getKeys(fields).length == 1 &&
            dataSource.fieldIsComplexType(fields[isc.firstKey(fields)].name))
        {
            schema = dataSource.getSchema(fields[isc.firstKey(fields)].type);
        } else {
            schema = dataSource;
        }
        
            
        // add one editNode for the single field in the DataSource that is named as the 
        // "titleField"; if there is no such field, just use the first

        var fields = schema.getFields(),
            titleFieldName = dataSource.titleField;
        
        if (!isc.isAn.Array(fields)) fields = isc.getValues(fields);
        
        for (var ix = 0; ix < fields.length; ix++) {
            if (!this.shouldUseField(fields[ix], dataSource)) continue;
            if (titleFieldName == null || titleFieldName == fields[ix].name) {
                var titleField = fields[ix];
                break;
            }
        }

        if (titleField) existingFields.addAt(titleField, 0);
        
        this.baseSetDataSource(dataSource, existingFields);
        
        var fieldConfig = liveObject.editProxy.getFieldEditNode(titleField, schema);
        fieldConfig.defaults.treeField = true;
        var editNode = liveObject.editContext.makeEditNode(fieldConfig);
        liveObject.editContext.addNode(editNode, liveObject.editNode, 0, null);
        // Deferred node removal to here as it avoids leaving the TG with an empty fieldset,
        // because this situation triggers the creation of a default treeField in various 
        // places in the TG code
        if (nodeToRemove) liveObject.editContext.removeNode(nodeToRemove, true);
    }

});