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




isc.defineClass("EditProxy", "Class");

isc.EditProxy.addClassProperties({
    resizeThumbConstructor:isc.Canvas,
    resizeThumbDefaults:{
        width:8, 
        height:8, 
        overflow:"hidden", 
        styleName:"resizeThumb",
        canDrag:true,
        canDragResize:true,
        // resizeEdge should be the edge of the target, not the thumb
        getEventEdge : function () { return this.edge; },
        autoDraw:false
    },

    minimumDropMargin: 2,
    minimumDropTargetSize: 10
});

isc.EditProxy.addClassMethods({
    // Resize thumbs
    // ---------------------------------------------------------------------------------------

    // NOTE: EditProxy thumbs vs one-piece mask?
    // - since we reuse the same set of thumbs, there's no real performance issue
    // - one-piece mask implementations: 
    //   - if an image with transparent regions, thumbs would scale 
    //   - if a table
    //     - event handling iffy - transparent table areas may or may not intercept
    //     - would have to redraw on resize
    //   - transparent Canvas with absolutely-positioned DIVs as content
    //     - event handling might be iffy
    // - would have bug: when thumbs are showing, should be able to click between them to hit
    //   something behind the currently selected target
    // - when thumbs are not showing, mask still needs to be there, but would need to shrink and not
    //   show thumbs
    _makeResizeThumbs : function () {
        var edgeCursors = isc.Canvas.getInstanceProperty("edgeCursorMap"),
            thumbs = {},
            thumbClass = isc.ClassFactory.getClass(this.resizeThumbConstructor);
        for (var thumbPosition in edgeCursors) {
           // NOTE: can't use standard autoChild creation because we are in static scope -
           // thumbs are globally shared
           thumbs[thumbPosition] = thumbClass.create({
                ID:"isc_resizeThumb_" + thumbPosition,
                edge:thumbPosition
           }, this.resizeThumbDefaults, this.resizeThumbProperties)
        }
        isc.EditProxy._resizeThumbs = thumbs;

        isc.EditProxy._observer = isc.Class.create();
    },

    showResizeThumbs : function (target) {
        if (!target) return;
        
        if (!isc.EditProxy._resizeThumbs) isc.EditProxy._makeResizeThumbs();

        
        var thumbSize = isc.EditProxy.resizeThumbDefaults.width,
            thumbs = isc.EditProxy._resizeThumbs;
     
        // place the thumbs along the outside of the target
        var rect = target.getPageRect(),
            left = rect[0],
            top = rect[1],
            width = rect[2],
            height = rect[3],
    
            midWidth = Math.floor(left + (width/2) - (thumbSize/2)),
            midHeight = Math.floor(top + (height/2) - (thumbSize/2));

        thumbs.T.moveTo(midWidth, top - thumbSize);
        thumbs.B.moveTo(midWidth, top + height);
        thumbs.L.moveTo(left - thumbSize, midHeight);
        thumbs.R.moveTo(left + width, midHeight);

        thumbs.TL.moveTo(left - thumbSize, top - thumbSize);
        thumbs.TR.moveTo(left + width, top - thumbSize);
        thumbs.BL.moveTo(left - thumbSize, top + height);
        thumbs.BR.moveTo(left + width, top + height);
        
        for (var thumbName in thumbs) {
            var thumb = thumbs[thumbName];
            // set all the thumbs to drag resize the canvas we're masking
            thumb.dragTarget = target;
            // show all the thumbs    
            thumb.bringToFront();        
            thumb.show();
        }

        // Observe target's destroy method so thumbs are automatically removed
        if (this._thumbTarget && this._observer.isObserving(this._thumbTarget, "destroy")) this._observer.ignore(this._thumbTarget, "destroy");
        this._observer.observe(target, "destroy", "isc.logWarn('TARGET DESTROYED'); isc.EditProxy.hideResizeThumbs()");

        this._thumbTarget = target;
    },
    
    hideResizeThumbs : function () {
        if (this._thumbTarget && this._observer.isObserving(this._thumbTarget, "destroy")) this._observer.ignore(this._thumbTarget, "destroy");

        var thumbs = this._resizeThumbs;
        for (var thumbName in thumbs) {
            thumbs[thumbName].hide();
        }
        this._thumbTarget = null;
    },
    
    getThumbTarget : function () {
        return this._thumbTarget;
    }
});

isc.EditProxy.addProperties({
    // Set to true to enable Canvas-based component selection, positioning and resizing.
    // Not used by VisualBuilder but by standalone editors. See EditPane for an example.
    enableComponentSelection: false,

    // Edit Mask
    // ---------------------------------------------------------------------------------------

    // At the Canvas level the Edit Mask provides moving, resizing, and standard context menu items.
    // The editMask should be extended on a per-widget basis to add things like drop behaviors or
    // additional context menu items.  Any such extensions should be delineated with 
    //>EditMode 
    //<EditMode
    // .. markers so it can be eliminated from normal builds.

    editMaskDefaults:{

        // Thumb handling
        // ---------------------------------------------------------------------------------------
        draw : function () {
            this.Super("draw", arguments);

            // stay above the master
            this.observe(this.masterElement, "setZIndex", "observer.moveAbove(observed)");
            // show thumbs on the master as soon as we're draw()n
            isc.EditProxy.showResizeThumbs(this);
            // Select new component. Delay to allow object to be fully created.
            this.editContext.delayCall("selectSingleComponent", [this.getTarget()], 0);

            // match the master's prompt (native tooltip).  Only actually necessary in Moz since IE
            // considers the eventMask transparent with respect to determining the prompt.
            this.observe(this.masterElement, "setPrompt", "observer.setPrompt(observed.prompt)");

            return this;
        },
        parentVisibilityChanged : function () {
            this.Super("parentVisibilityChanged", arguments);
            if (isc.EditProxy.getThumbTarget() == this) isc.EditProxy.hideResizeThumbs();
        },

        // show thumbs when clicked on.  NOTE: since there's only one set of thumbs, this implicitly
        // accomplishes the goal of having only one selected widget
        click : function () {
            isc.EditProxy.showResizeThumbs(this);
            this.editContext.selectSingleComponent(this.getTarget());
            return isc.EH.STOP_BUBBLING;
        },

        // Event Bubbling
        // ---------------------------------------------------------------------------------------

        // XXX FIXME: this is here to maintain z-order on dragReposition.  EH.handleDragStop()
        // brings the mask to the front when we stop dragging - which is not what we want, so we
        // suppress it here.
        bringToFront : function () { },
    
        // prevent bubbling to the editor otherwise we'll start a selection while trying to
        // select/move a component
        mouseDown : function () {
            this.Super("mouseDown", arguments);
            return isc.EH.STOP_BUBBLING;
        },

        mouseUp : function () {
            this.Super("mouseUp", arguments);
            return isc.EH.STOP_BUBBLING;
        },

        dragRepositionStart : function() {
            // When we start to drag a component it should be selected
            isc.EditProxy.showResizeThumbs(this);
            if (this.editPaneProxy && this.editPaneProxy.selectionType != isc.Selection.MULTIPLE ||
                !this.editContext.isComponentSelected(this.getTarget())) 
            {
                this.editContext.selectSingleComponent(this.getTarget());
            }
        },

        doubleClick : function () {
            this.getTarget().bringToFront();
            return this.click();
        },

        // Drag and drop move and resize
        // ---------------------------------------------------------------------------------------
        // D&D: some awkwardness
        // - if we set dragTarget to the masterElement, it will get the setDragTracker(), 
        //   dragRepositionMove() etc events, which it may have overridden, whereas we want just a
        //   basic reposition or resize, so we need to be the dragTarget
        // - to be in the right parental context, and to automatically respond to programmatic
        //   manipulation of the parent's size and position, we want to be a peer, but at the end of
        //   drag interactions we also need to move/resize the master, which would normally cause
        //   the master to move us, so we need to switch off automatic peer behaviors while we move
        //   the master

        // allow the mask to be moved around (only the thumbs allow resize)
        canDrag:true,
        canDragReposition:true,
    
        // don't allow setDragTracker to bubble in case some parent tries to set it inappropriately
        setDragTracker: function () { return isc.EH.STOP_BUBBLING },

        // when we're moved or resized, move/resize the master and update thumb positions
        moved : function () {
            this.Super("moved", arguments);

            var masked = this.masterElement;
            if (masked) {
                // calculate the amount the editMask was moved
                var deltaX = this.getOffsetLeft() - masked.getLeft();
                var deltaY = this.getOffsetTop() - masked.getTop();

                // relocate our master component (avoiding double notifications)
                this._moveWithMaster = false;
                masked.moveTo(this.getOffsetLeft(), this.getOffsetTop());
                this._moveWithMaster = true;
            }

            if (isc.EditProxy.getThumbTarget() == this) isc.EditProxy.showResizeThumbs(this);
        },

        resized : function () {
            this.Super("resized", arguments);

            // Recalculate dropMargin based on new visible size
            if (this.editProxy) this.editProxy.updateDropMargin();

            // don't loop if we resize master, master overflows, and we resize to overflow'd size
            if (this._resizingMaster) return;
            this._resizingMaster = true;

            var master = this.masterElement;
            if (master) {

                // resize the widget we're masking (avoiding double notifications)
                this._resizeWithMaster = false;
                master.resizeTo(this.getWidth(), this.getHeight());
                this._resizeWithMaster = true;

                // the widget we're masking may overflow, so redraw if necessary to get new size so,
                // and match its overflow'd size
                master.redrawIfDirty();
                this.resizeTo(master.getVisibleWidth(), master.getVisibleHeight());
            }

            // update thumb positions
            isc.EditProxy.showResizeThumbs(this);

            this._resizingMaster = false;
        },

        // Editing Context Menus
        // ---------------------------------------------------------------------------------------
        // standard context menu items plus the ability to add "editMenuItems" on the master
        showContextMenu : function () {
            // Showing context menu should also shift selected component unless
            // the component is part of a selection already.
            var target = this.masterElement,
                targetSelected = this.editContext.isComponentSelected(target);
            if (!targetSelected) {
                isc.EditProxy.showResizeThumbs(target);
                this.editContext.selectSingleComponent(target);
            } else if (isc.EditProxy.getThumbTarget() != target) {
                isc.EditProxy.showResizeThumbs(target);
            }

            // Show multiple-selection menu iff menu target is part of selection
            var selection = this.editContext.getSelectedComponents(),
                menuItems;
            if (selection.length > 1 && targetSelected) { 
                // multi-select
                menuItems = this.multiSelectionMenuItems;
            } else {
                menuItems = this.standardMenuItems;
            }

            if (!this.contextMenu) this.contextMenu = this.getMenuConstructor().create({});
            this.contextMenu.setData(menuItems);

            // NOTE: show the menu on the mask to allow reference to the editPane
            // and/or proxy.
            this.contextMenu.showContextMenu(this);
            return false;
        },
        // Menu actions
        componentsRemove : function () {
            this.editContext.getSelectedComponents().map("destroy");
        },
        componentsBringToFront : function () {
            this.editContext.getSelectedComponents().map("bringToFront");
        },
        componentsSendToBack : function () {
            this.editContext.getSelectedComponents().map("sendToBack");
        },
        // Single and multiple-selection menus
        standardMenuItems:[
            {title:"Remove", click:"target.componentsRemove()"},
            {title:"Bring to front", click:"target.componentsBringToFront()"},
            {title:"Send to back", click:"target.componentsSendToBack()"}
        ],
        multiSelectionMenuItems: [
            {title: "Remove selected items", click:"target.componentsRemove()"},
            {title:"Bring to front", click:"target.componentsBringToFront()"},
            {title:"Send to back", click:"target.componentsSendToBack()"}
        ]
    }
});

isc.EditProxy.addMethods({
  
    setEditMode : function (editingOn) {
        if (editingOn) {
            this.saveOverrideProperties();
            // Calculate dropMargin based on visible size
            this.updateDropMargin();
            if (this.creator.setShowSnapGrid) this.creator.setShowSnapGrid(true);
        } else {
            this.restoreOverrideProperties();
            this.hideEditMask();
            if (this.creator.setShowSnapGrid) this.creator.setShowSnapGrid(false);
        }
    },

    getOverrideProperties : function () {
        var properties = {
            canAcceptDrop: true,
            canDropComponents: true
        };

        if (this.enableComponentSelection) {
            isc.addProperties(properties, {
                canDrag: true,
                dragAppearance: "none",
                overflow: "hidden"
            });
        }
        return properties;
    },

    // Called after a new node is created by a drop
    nodeDropped : function () {
    },

    editTitle : function (liveObject, initialValue, completionCallback) {
        var liveObject = liveObject || this.creator,
            left,
            width,
            top;

        if (isc.isA.Button(liveObject)) {  // This includes Labels and SectionHeaders
            left = liveObject.getPageLeft() + liveObject.getLeftBorderSize() + liveObject.getLeftMargin() + 1 
                                                  - liveObject.getScrollLeft(); 
            width = liveObject.getVisibleWidth() - liveObject.getLeftBorderSize() - liveObject.getLeftMargin() 
                               - liveObject.getRightBorderSize() - liveObject.getRightMargin() - 1;
        } else if (isc.isA.StretchImgButton(liveObject)) {
            left = liveObject.getPageLeft() + liveObject.capSize;
            width = liveObject.getVisibleWidth() - liveObject.capSize * 2;
        } else {
            isc.logWarn("Ended up in editTitle with a StatefulCanvas of type '" + 
                    liveObject.getClass() + "'.  This is neither a Button " +
                        "nor a StretchImgButton - editor will work, but will hide the " +
                        "entire component it is editing");
            left = liveObject.getPageLeft();
            width = liveObject.getVisibleWidth();
        }

        isc.Timer.setTimeout({target: isc.EditContext,
                              methodName: "manageTitleEditor", 
                              args: [liveObject, left, width, top, null, initialValue, completionCallback]}, 0);
    },

    // This function is only called for ImgTabs that need to be scrolled into view
    repositionTitleEditor : function () {
        var liveObject = this.creator;
        var left = liveObject.getPageLeft() + liveObject.capSize,
            width = liveObject.getVisibleWidth() - liveObject.capSize * 2;
        
        isc.EditContext.positionTitleEditor(liveObject, left, width);
    },

    // Save/restore property functionality
    // ---------------------------------------------------------------------------------------

    // These methods are based on Class.saveToOriginalValues and Class.restoreFromOriginalValues.
    // This is necessary because edit values can be merged into saved values and should be
    // restored when done.
    saveOverrideProperties : function () {
        var properties = this.getOverrideProperties();
        this.overrideProperties(properties);
    },
    
    restoreOverrideProperties : function () {
        var properties = this.getOverrideProperties();
        this.restoreProperties(isc.getKeys(properties));
    },

    overrideProperties : function (properties) {
        this.creator.saveToOriginalValues(isc.getKeys(properties));
        this.creator.setProperties(properties);
    },

    restoreProperties : function (fieldNames) {
        if (fieldNames == null) return;
        this.creator.restoreFromOriginalValues(fieldNames);
    },

    // Edit Mask
    // ---------------------------------------------------------------------------------------

    showEditMask : function (editPane) {
        var liveObject = this.creator,
            svgID = liveObject.getID() + ":<br>" + liveObject.src;

        // create an edit mask if we've never created one
        if (!this._editMask) {

            // special SVG handling
            // FIXME: move all SVG-specific handling to SVG.js
            var svgProps = { };
            if (isc.SVG && isc.isA.SVG(liveObject) && isc.Browser.isIE) {
                isc.addProperties(svgProps, {
                    backgroundColor : "gray",
                    mouseOut : function () { this._maskTarget.Super("_hideDragMask"); },
                    contents : isc.Canvas.spacerHTML(10000,10000, svgID)
                });
            }
    
            var props = isc.addProperties({}, this.editMaskDefaults, this.editMaskProperties, 
                                          // assume the editContext is the parent if none is
                                          // provided
                                          {editPane:editPane,
                                           editPaneProxy:editPane.editProxy,
                                           editContext:liveObject.editContext || liveObject.parentElement, 
                                           keepInParentRect: liveObject.keepInParentRect},
                                          svgProps);
            this._editMask = isc.EH.makeEventMask(liveObject, props);
        }
        this._editMask.show();

        // SVG-specific
        if (isc.SVG && isc.isA.SVG(liveObject)) {
            if (isc.Browser.isIE) liveObject.showNativeMask();
            else {
                liveObject.setBackgroundColor("gray");
                liveObject.setContents(svgID);
            }
        }
    },
    hideEditMask : function () {
        if (this._editMask) this._editMask.hide();
    },
    setEditMaskBorder : function (style) {
        if (this._editMask) this._editMask.setBorder(style);
    },
    hasEditMask : function () {
        return (this._editMask != null);
    },


    // Thumbs, drag move and resize
    // ---------------------------------------------------------------------------------------
    // Implemented in Canvas.childResized and Canvas.childMoved.


    // Hoop selection
    // --------------------------------------------------------------------------------------------

    //> @attr editProxy.selectionType        (SelectionStyle : isc.Selection.MULTIPLE : [IRW])
    // Defines a Canvas's clickable-selection behavior. Only two styles are supported:
    // "single" and "multiple". Multiple selection enables hoop selection.
    //
    // @group selection, appearance
    // @see type:SelectionStyle
    // @visibility internal
    //<
    selectionType: isc.Selection.MULTIPLE,

    // TODO Create HoopSelectionStyle type

    //> @attr editProxy.hoopSelectionMode    (HoopSelectionStyle: "encloses" : [IRW])
    // Defines the mode of inclusion for components encountered during hoop selection.
    // <code>encloses</code> mode causes selection of components that are completely
    // enclosed by the hoop. <code>intersects</code> mode selects components that come
    // into contact with the hoop.
    //
    // @group selection, appearance
    // @see type:HoopSelectionStyle
    // @visibility internal
    //<
    hoopSelectionMode: "encloses",

    hoopSelectorDefaults: {
        _constructor:"Canvas",
        autoDraw:false,
        keepInParentRect: true,
        redrawOnResize:false,
        overflow: "hidden",
        border: "1px solid red",
        opacity:10,
        backgroundColor:"blue"
    },

    mouseDown : function () {
        if (!this.enableComponentSelection) return;
        var liveObject = this.creator,
            editContext = liveObject.editContext
        ;

        // don't start hoop selection unless the mouse went down on the Canvas itself, as
        // opposed to on one of the live objects
        if (isc.EH.getTarget() != liveObject) return;

        // Since mouse is pressed outside of a component clear current selection
        editContext.deselectAllComponents();

        if (this.selectionType != isc.Selection.MULTIPLE) return;

        var target = isc.EH.getTarget();
        if (this.hoopSelector == null) {
            // Create hoop selector as a child on our liveObject
            this.hoopSelector = liveObject.createAutoChild("hoopSelector",
                this.hoopSelectorDefaults, 
                this.hoopSelectorProperties,
                { left: isc.EH.getX(), top: isc.EH.getY() }
            );
            liveObject.addChild(this.hoopSelector);
        }
        this._hoopStartX = liveObject.getOffsetX();
        this._hoopStartY = liveObject.getOffsetY();

        // Save current selection to determine if this mouseDown is paired
        // with a mouseUp that does not change the selection. In that case
        // we should not fire the selectedComponentsUpdated event.
        this._startingSelection = editContext.getSelectedComponents();

        this.resizeHoopSelector();
        this.hoopSelector.show();
    },

    // resize hoop on dragMove
    // hide selector hoop on mouseUp or dragStop
    dragMove : function() {
        if (this.hoopSelector && this.hoopSelector.isVisible()) this.resizeHoopSelector();
    },

    dragStop : function() {
        if (this.hoopSelector && this.hoopSelector.isVisible()) {
            this.hoopSelector.hide();
            isc.EditProxy.hideResizeThumbs();
            var currentSelection = this.creator.editContext.getSelectedComponents();
            if (!this._startingSelection.equals(currentSelection)) {
                // Fire callback now that selection has completed 
                this.creator.editContext.fireSelectedComponentsUpdated();
            }
        }
    },

    mouseUp : function () {
        if (!this.enableComponentSelection) return;
        if (this.hoopSelector && this.hoopSelector.isVisible()) {
            this.hoopSelector.hide();
            isc.EditProxy.hideResizeThumbs();
            var currentSelection = this.creator.editContext.getSelectedComponents();
            if (!this._startingSelection.equals(currentSelection)) {
                // Fire callback now that selection has completed 
                this.creator.editContext.fireSelectedComponentsUpdated();
            }
        }
    },

    outlineBorderStyle : "2px dashed red",
    // add an outline, indicating selection to a set of components
    setOutline : function (components) {
        if (!components) return;
        if (!isc.isAn.Array(components)) components = [components];
        for (var i = 0; i < components.length; i++) {
            if (components[i].editProxy && components[i].editProxy.setEditMaskBorder) {
                components[i].editProxy.setEditMaskBorder(this.outlineBorderStyle);
            }
        }
    },

    // clear outline on a set of components
    clearOutline : function (components) {
        if (!components) return;
        if (!isc.isAn.Array(components)) components = [components];
        for (var i = 0; i < components.length; i++) {
            if (components[i].editProxy && components[i].editProxy._editMask) {
                components[i].editProxy._editMask.setBorder("none");
            }
        }
    },

    // figure out which components intersect the selector hoop, and show the selected outline on
    // those
    updateCurrentSelection : function () {
        var liveObject = this.creator,
            editContext = liveObject.editContext,
            isDrawPane = isc.isA.DrawPane(liveObject)
        ;

        var children = (isDrawPane ? liveObject.drawItems : liveObject.children);
        if (!children) return;
        var oldSelection = editContext.getSelectedComponents(),
            matchFunc = (this.hoopSelectionMode == "intersects" ? "intersects" : "encloses")
        ;

        // make a list of all the children which currently intersect the selection hoop.
        // Update editContext selectedComponents directly because we don't want to fire
        // the selectedComponentsUpdated event during hoop dragging.
        editContext.selectedComponents = [];
        for (var i = 0; i < children.length; i++) {
            var child = children[i],
                isKnob = (child.creator && isc.isA.DrawKnob(child.creator))
            ;
            
            if (!isKnob && this.hoopSelector[matchFunc](child)) {
                if (!isDrawPane) child = this.deriveSelectedComponent(child);
                if (child && !editContext.selectedComponents.contains(child)) {
                    editContext.selectedComponents.add(child);
                }
            }
        }

        // set outline on components currently within the hoop
        this.setOutline(editContext.selectedComponents);
    
        // de-select anything that is no longer within the hoop
        oldSelection.removeList(editContext.selectedComponents);
        this.clearOutline(oldSelection);
    },

    // given a child in the canvas, derive the editComponent if there is one
    deriveSelectedComponent : function (comp) {
        var liveObject = this.creator;

        // if the component has a master, it's either an editMask or a peer of some editComponent
        if (comp.masterElement) return this.deriveSelectedComponent(comp.masterElement);
        if (!comp.parentElement || comp.parentElement == liveObject) {
            // if it has an event mask, it's an edit component
            if (comp.editProxy && comp.editProxy.hasEditMask()) return comp;
            // otherwise it's a mask or the hoop
            return null;
        }
        // XXX does this case exist?  how can a direct child have a parent element other than its
        // parent?
        return this.deriveSelectedComponent(comp.parentElement);
    },

    // resize selector to current mouse coordinates
    resizeHoopSelector : function () {
        var liveObject = this.creator,
            x = liveObject.getOffsetX(),
            y = liveObject.getOffsetY();

        if (this.hoopSelector.keepInParentRect) {
            if (x < 0) x = 0;
            var parentHeight = this.hoopSelector.parentElement.getVisibleHeight();
            if (y > parentHeight) y = parentHeight;
        }
    
        // resize to the distances from the start coordinates
        this.hoopSelector.resizeTo(Math.abs(x-this._hoopStartX), Math.abs(y-this._hoopStartY));

        // if we are above/left of the origin set top/left to current mouse coordinates,
        // otherwise to start coordinates.
        if (x < this._hoopStartX) this.hoopSelector.setLeft(x);
        else this.hoopSelector.setLeft(this._hoopStartX);

        if (y < this._hoopStartY) this.hoopSelector.setTop(y);
        else this.hoopSelector.setTop(this._hoopStartY);

        // figure out which components are now in the selector hoop
        this.updateCurrentSelection();
    },

    getAllSelectableComponents : function () {
        var liveObject = this.creator;

        if (!liveObject.children) return null;
        var components = [];
        for (var i = 0; i < liveObject.children.length; i++) {
            var child = this.deriveSelectedComponent(liveObject.children[i]);
            if (child) components.add(child);
        }
        return components;
    },

    // Selection/Title edit handling
    // ---------------------------------------------------------------------------------------

    click : function () {
        if (this.creator.editNode) {
            isc.EditContext.selectCanvasOrFormItem(this.creator, true);
            return isc.EH.STOP_BUBBLING;
        }
    },
    
    doubleClick : function () {
        var liveObject = this.creator;

        if (isc.isA.ImgTab(liveObject) ||
            isc.isA.Button(liveObject) ||
            isc.isA.StretchImgButton(liveObject) ||
            isc.isA.SectionHeader(liveObject) ||
            isc.isA.ImgSectionHeader(liveObject) ||
            isc.isA.DrawItem(liveObject))
        {
            this.editTitle();
        }
    },

    // Drag/drop method overrides
    // ---------------------------------------------------------------------------------------

    willAcceptDrop : function (changeObjectSelection) {
        var liveObject = this.creator;
        this.logInfo("editProxy.willAcceptDrop for " + liveObject.ID, "editModeDragTarget");
        var dragData = liveObject.ns.EH.dragTarget.getDragData(),
            dragType,
            draggingFromPalette = true;
    
        // If dragData is null, this is probably because we are drag-repositioning a component
        // in a layout - the dragData is the component itself
        if (dragData == null || (isc.isAn.Array(dragData)) && dragData.length == 0) {
            draggingFromPalette = false;
            this.logInfo("dragData is null - using the dragTarget itself", "editModeDragTarget");
            dragData = liveObject.ns.EH.dragTarget;
            if (isc.isA.FormItemProxyCanvas(dragData)) {
                this.logInfo("The dragTarget is a FormItemProxyCanvas for " + dragData.formItem,
                                "editModeDragTarget");
                dragData = dragData.formItem;
            }
            dragType = dragData._constructor || dragData.Class;
        } else {
            if (isc.isAn.Array(dragData)) dragData = dragData[0];
            dragType = dragData.type || dragData.className;
        }
        this.logInfo("Using dragType " + dragType, "editModeDragTarget");
    
        if (!this.canAdd(dragType)) {
            this.logInfo(liveObject.ID + " does not accept drop of type " + dragType, "editModeDragTarget");
            // Can't drop on this widget, so check its ancestors
            var ancestor = liveObject.parentElement;
            while (ancestor && !ancestor.editorRoot) {
                if (ancestor.editingOn) {
                    var ancestorAcceptsDrop = ancestor.editProxy.willAcceptDrop();
                    if (!ancestorAcceptsDrop) {
                        this.logInfo("No ancestor accepts drop", "editModeDragTarget");
                        if (changeObjectSelection != false) {
                            isc.SelectionOutline.hideOutline();
                            this.setNoDropIndicator();
                        }
                        return false;
                    }
                    this.logInfo("An ancestor accepts drop", "editModeDragTarget");
                    return true;
                }
                // Note that the effect of the return statements in the
                // condition above is that we'll stop walking
                // the ancestor tree at the first parent where editingOn is true ...
                // at that point, we'll re-enter editProxy.willAcceptDrop
                ancestor = ancestor.parentElement;
            }
    
            // Given the return statements in the while condition above, we'll only get
            // here if no ancestor had editingOn: true
            this.logInfo(liveObject.ID + " has no parentElement in editMode", "editModeDragTarget");
            if (changeObjectSelection != false) {
                isc.SelectionOutline.hideOutline();
                this.setNoDropIndicator();
            }
            return false;
        }
        
        // This canvas can accept the drop, so select its top-level parent (in case it's a 
        // sub-component like a TabSet's PaneContainer)
        this.logInfo(liveObject.ID + " is accepting the " + dragType + " drop", "editModeDragTarget");
        var hiliteCanvas = this.findEditNode(dragType);
        if (hiliteCanvas) {
            if (changeObjectSelection != false) {
                this.logInfo(liveObject.ID + ": selecting editNode object " + hiliteCanvas.ID);
                if (liveObject.editContext.showSelectionOutline)
                    isc.SelectionOutline.select(hiliteCanvas, false);
                // TODO Should this call hiliteCanvas.editProxy?
                hiliteCanvas.clearNoDropIndicator();
            }
            return true;
        } else {
            this.logInfo("findEditNode() returned null for " + liveObject.ID, "editModeDragTarget");
        }
        
        
        if (changeObjectSelection != false) {
            this.logInfo("In editProxy.willAcceptDrop, '" + liveObject.ID + "' was willing to accept a '" + 
                     dragType + "' drop but we could not find an ancestor with an editNode");
        }
    }, 
    
    // Override to provide special editNode canvas selection (note that this impl does not 
    // care about dragType, but some special implementations - eg, TabSet - return different
    // objects depending on what is being dragged)
    findEditNode : function (dragType) {
        var liveObject = this.creator;
        if (!liveObject.editNode) {
            this.logInfo("Skipping '" + liveObject + "' - has no editNode", "editModeDragTarget");
            if (liveObject.parentElement && 
                liveObject.parentElement.editProxy && 
                liveObject.parentElement.editProxy.findEditNode) 
            {
                return liveObject.parentElement.editProxy.findEditNode(dragType);
            } else {
                return null;
            }
        }
        return liveObject;
    },
    
    // Tests whether this Canvas can accept a child of type "type".  If it can't, and "type"
    // names some kind of FormItem, then we'll accept it if this Canvas is willing to accept
    // a child of type "DynamicForm" -- we'll cope with this downstream by auto-wrapping the dropped
    // FormItem inside a DynamicForm that we create for that very purpose.  Similarly, if
    // the type represents some type of DrawItem then we'll accept the child if this Canvas
    // can a DrawPane.
    canAdd : function (type) {
        var liveObject = this.creator;
        if (liveObject.getObjectField(type) == null) {
            var clazz = isc.ClassFactory.getClass(type);
            if (clazz) {
                if (clazz.isA("FormItem")) {
                    return (liveObject.getObjectField("DynamicForm") != null);
                } else if (clazz.isA("DrawItem")) {
                    return (liveObject.getObjectField("DrawPane") != null);
                }
            }
            return false;
        } else {
            return true;
        }
    },
    
    // Canvas.clearNoDropindicator no-ops if the internal _noDropIndicator flag is null.  This
    // isn't good enough in edit mode because a canvas can be dragged over whilst the no-drop
    // cursor is showing, and we want to revert to a droppable cursor regardless of whether 
    // _noDropIndicatorSet has been set on this particular canvas. 
    clearNoDropIndicator : function (type) {
        var liveObject = this.creator;
        if (liveObject._noDropIndicatorSet) delete liveObject._noDropIndicatorSet;
        liveObject._updateCursor();
        
        // XXX May need to add support for no-drop drag tracker here if we ever implement 
        // such a thing in Visual Builder
    },
    
    // Special editMode version of setNoDropCursor - again, because the base version no-ops in 
    // circumstances where we need it to refresh the cursor.
    setNoDropIndicator : function () {
        var liveObject = this.creator;
        liveObject._noDropIndicatorSet = true;
        liveObject._applyCursor(liveObject.noDropCursor);
    },

    

    defaultDropMargin: 10,
    dropMargin: 10,
    updateDropMargin : function () {

        // Fix up the dropMargin to prevent not-very-tall canvas from passing *every* drop 
        // through to parent layouts
        var liveObject = this.creator,
            newDropMargin = this.defaultDropMargin;
        if (newDropMargin * 2 > liveObject.getVisibleHeight() - isc.EditProxy.minimumDropTargetSize) {
            newDropMargin = Math.round((liveObject.getVisibleHeight() - isc.EditProxy.minimumDropTargetSize) / 2);
            if (newDropMargin < isc.EditProxy.minimumDropMargin) newDropMargin = isc.EditProxy.minimumDropMargin; 
        }
        this.dropMargin = newDropMargin;
    },

    shouldPassDropThrough : function () {
        var liveObject = this.creator,
            source = isc.EH.dragTarget,
            paletteNode,
            dropType;

        if (!source.isA("Palette")) {
            dropType = source.isA("FormItemProxyCanvas") ? source.formItem.Class
                                                         : source.Class;
        } else {
            paletteNode = source.getDragData();
            if (isc.isAn.Array(paletteNode)) paletteNode = paletteNode[0];
            dropType = paletteNode.type || paletteNode.className;
        }
        
        this.logInfo("Dropping a " + dropType, "formItemDragDrop");
        
        if (!this.canAdd(dropType)) {
            this.logInfo("This canvas cannot accept a drop of a " + dropType, "formItemDragDrop");
            return true;
        }
        
        if (liveObject.parentElement && 
            liveObject.parentElement.editProxy &&
            !liveObject.parentElement.editProxy.willAcceptDrop(false))
        {
            this.logInfo(liveObject.ID + " is not passing drop through - no ancestor is willing to " + 
                        "accept the drop", "editModeDragTarget");
            return false;
        }
        
        var x = isc.EH.getX(),
            y = isc.EH.getY(),
            work = liveObject.getPageRect(),
            rect = {
                left: work[0], 
                top: work[1], 
                right: work[0] + work[2], 
                bottom:work[1] + work[3]
            }
            
        if (!liveObject.orientation || liveObject.orientation == "vertical") {
            if (x < rect.left + this.dropMargin  || x > rect.right - this.dropMargin) {
                this.logInfo("Close to right or left edge - passing drop through to parent for " +
                        liveObject.ID, "editModeDragTarget");
                return true;
            }
        }
        if (!liveObject.orientation || liveObject.orientation == "horizontal") {
            if (y < rect.top + this.dropMargin  || y > rect.bottom - this.dropMargin) {
                this.logInfo("Close to top or bottom edge - passing drop through to parent for " + 
                        liveObject.ID, "editModeDragTarget");
                return true;
            }
        }

        this.logInfo(liveObject.ID + " is not passing drop through", "editModeDragTarget");
        return false;
    },
    
    
    drop : function () {
        if (this.shouldPassDropThrough()) {
            return;
        }
    
        var liveObject = this.creator,
            source = isc.EH.dragTarget,
            paletteNode,
            dropType;
    
        if (!source.isA("Palette")) {
            if (source.isA("FormItemProxyCanvas")) {
                source = source.formItem;
            }
            dropType = source._constructor || source.Class;
        } else {
            paletteNode = source.transferDragData();
            if (isc.isAn.Array(paletteNode)) paletteNode = paletteNode[0];
            paletteNode.dropped = true;
            dropType = paletteNode.type || paletteNode.className;
        }
        
        // if the source isn't a Palette, we're drag/dropping an existing component, so remove the 
        // existing component and re-create it in its new position
        if (!source.isA("Palette")) {
            if (isc.EditContext._dragHandle) isc.EditContext._dragHandle.hide();
            if (source == liveObject) return;  // Can't drop a component onto itself
            var editContext = liveObject.editContext,
                editNode = liveObject.editNode,
                tree = editContext.getEditNodeTree(),
                oldParent = tree.getParent(source.editNode);
            editContext.removeNode(source.editNode);
            var node;
            if (source.isA("FormItem")) {
                if (source.isA("CanvasItem")) {
                    node = editContext.addNode(source.canvas.editNode, editNode);
                } else {
                    node = editContext.addWithWrapper(source.editNode, editNode);
                }
            } else if (source.isA("DrawItem")) {
                node = editContext.addWithWrapper(source.editNode, editNode, true);
            } else {
                node = editContext.addNode(source.editNode, editNode);
            }
            if (node && node.liveObject) {
                isc.EditContext.selectCanvasOrFormItem(node.liveObject, true);
            }
        } else {
            // loadData() operates asynchronously, so we'll have to finish the item drop off-thread
            if (paletteNode.loadData && !paletteNode.isLoaded) {
                paletteNode.loadData(paletteNode, function (loadedNode) {
                    loadedNode = loadedNode || paletteNode;
                    loadedNode.isLoaded = true;
                    liveObject.completeItemDrop(loadedNode)
                    loadedNode.dropped = paletteNode.dropped;
                });
                return isc.EH.STOP_BUBBLING;
            }

            this.completeItemDrop(paletteNode);
            return isc.EH.STOP_BUBBLING;
        }
    },

    completeItemDrop : function (paletteNode) {
        var liveObject = this.creator;

        if (!liveObject.editContext) return;
        
        var nodeType = paletteNode.type || paletteNode.className,
            wrapped = false
        ;
        var clazz = isc.ClassFactory.getClass(nodeType);
        if (clazz && clazz.isA("FormItem")) {
            liveObject.editContext.addWithWrapper(paletteNode, liveObject.editNode);
        } else if (clazz && clazz.isA("DrawItem")) {
            liveObject.editContext.addWithWrapper(paletteNode, liveObject.editNode, true);
            wrapped = true;
        } else {
            liveObject.editContext.addNode(paletteNode, liveObject.editNode);
        }
        if (this.enableComponentSelection) {
            // move new component to the current mouse position.
            // if paletteNode was wrapped, update the wrapper node position
            var node = paletteNode;
            if (wrapped) {
                var tree = liveObject.editContext.getEditNodeTree(),
                    parent = tree.getParent(paletteNode)
                ;
                if (parent) node = parent;
            }
            node.liveObject.moveTo(liveObject.getOffsetX(), liveObject.getOffsetY());
            liveObject.selectSingleComponent(node.liveObject);
        }
    },
    
    dropMove : function () {
        if (!this.willAcceptDrop()) return false;
        if (!this.shouldPassDropThrough()) {
            this.creator.Super("dropMove", arguments);
            var liveObject = this.creator,
                parentElement = liveObject.parentElement;
            if (parentElement && parentElement.hideDropLine) {
                parentElement.hideDropLine();
                if (parentElement.isA("FormItem")) {
                    parentElement.form.hideDragLine();
                } else if (parentElement.isA("DrawItem")) {
                    parentElement.drawPane.hideDragLine();
                }
            }
            return isc.EH.STOP_BUBBLING;        
        }
    },
    
    dropOver : function () {
        if (!this.willAcceptDrop()) return false;
        if (!this.shouldPassDropThrough()) {
            this.creator.Super("dropOver", arguments);        
            var liveObject = this.creator,
                parentElement = liveObject.parentElement;
            if (parentElement && parentElement.hideDropLine) {
                parentElement.hideDropLine();
                if (parentElement.isA("FormItem")) {
                    parentElement.form.hideDragLine();
                } else if (parentElement.isA("DrawItem")) {
                    parentElement.drawPane.hideDragLine();
                }
            }
            return isc.EH.STOP_BUBBLING;        
        }
    },

    // In editMode, we allow dragging the selected canvas using the drag-handle
    // This involves overriding some default behaviors at the widget level.
    overrideDragProperties : function () {
        var properties = {
            canDrop: true,
            dragAppearance: "outline",
            // These method overrides are to clobber special record-based drag handling
            // implemented by ListGrid and its children
            dragStart : function () { return true; },
            dragMove : function () { return true; },
            setDragTracker : function () {isc.EH.setDragTracker(""); return false; },
            dragStop : function () {
                isc.SelectionOutline.hideProxyCanvas();
                isc.SelectionOutline.positionDragHandle();
            }
        };
 
        this.overrideProperties(properties);
    },
    
    restoreDragProperties : function () {
        this.creator.restoreFromOriginalValues([
            "canDrag", 
            "canDrop",
            "dragAppearance",
            "dragStart",
            "dragMove",
            "dragStop",
            "setDragTracker"
        ]);
    },

    // DataBoundComponent functionality
    // ---------------------------------------------------------------------------------------

    // In editMode, when setDataSource is called, generate editNodes for each field so that the
    // user can modify the generated fields.
    // On change of DataSource, remove any auto-gen field that the user has not changed.
    
    setDataSource : function (dataSource, fields, forceRebind) {
        //this.logWarn("editProxy.setDataSource called" + isc.Log.getStackTrace());

        var liveObject = this.creator;

        // _loadingNodeTree is a flag set by Visual Builder - its presence indicates that we are 
        // loading a view from disk.  In this case, we do NOT want to perform the special 
        // processing in this function, otherwise we'll end up with duplicate components in the
        // componentTree.  So we'll just fall back to the base impl in that case.
        if (isc._loadingNodeTree) {
            liveObject.setDataSource(dataSource, fields);
            return;
        }

        if (dataSource == null) return;
        if (dataSource == liveObject.dataSource && !forceRebind) return;

        var fields = liveObject.getFields(),
            keepFields = [],
            removeNodes = [];

        // remove all automatically generated fields that have not been edited by the user
        
        if (fields) {
            var tree = liveObject.editContext.getEditNodeTree(),
                parentNode = tree.findById(liveObject.ID),
                children = tree.getChildren(parentNode)
            ;
            for (var i = 0; i < fields.length; i++) {
                var field = fields[i],
                    editNode = null
                ;
                for (var j = 0; j < children.length; j++) {
                    var child = children[j];
                    if (field.name == child.name) {
                        editNode = child;
                        break;
                    }
                }

                if (editNode && editNode.autoGen && !this.fieldEdited(liveObject, editNode)) {
                    removeNodes.add(editNode);
                } else if (editNode) {
                    keepFields.add(field);
                }
            }
            liveObject.setFields(keepFields);
            for (var i = 0; i < removeNodes.length; i++) {
                liveObject.editContext.removeNode(removeNodes[i], true);
            }
        }



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

        // add one editNode for every field in the DataSource that the component would normally
        // display or use.  
        

        var allFields = schema.getFields();
            fields = {};

        for (var key in allFields) {
            var field = allFields[key];
            if (!liveObject.shouldUseField(field, dataSource)) continue;
            fields[key] = allFields[key];
            // duplicate the field on the DataSoure - we don't want to have the live component
            // sharing actual field objects with the DataSource
            fields[key] = isc.addProperties({}, allFields[key]);
        }

        // Merge the list of fields to keep (because they were manually added, or changed after 
        // generation) with the list of fields on the new DataSource.  Of course, the "list of 
        // fields to keep" could well be the empty list (and always will be if this is the first
        // time we're binding this DataBoundComponent and the user has not manually added fields)
        keepFields.addList(isc.getValues(fields));
        liveObject.setDataSource(dataSource, keepFields);

        for (var key in fields) {
            var field = fields[key];

            // What constitutes a "field" varies by DBC type
            var fieldConfig = this.getFieldEditNode(field, schema);
            var editNode = liveObject.editContext.makeEditNode(fieldConfig);
            //this.logWarn("editProxy.setDataSource adding field: " + field.name);
            liveObject.editContext.addNode(editNode, liveObject.editNode, null, null, true);
        }
        //this.logWarn("editProxy.setDataSource done adding fields");
    },

    // whether a field has been edited
    // Strategy: An edited field will likely have more properties than just
    // the base "name" and "title". Therefore if there are more properties
    // consider the field edited. Otherwise, if the title is different from
    // the auto-generated title or from the original DataSource field title
    // then the field title has been edited.
    fieldEdited : function (parentCanvas, editNode) {
        var edited = false;
        if (editNode.defaults) {
            var defaults = editNode.defaults,
                hasNonBaseProperties = false
            ;
            for (var key in defaults) {
                if (key == "name" || key == "title" || key.startsWith("_")) continue;
                hasNonBaseProperties = true;
                break;
            }
            if (!hasNonBaseProperties) {
                var name = defaults["name"],
                    title = defaults["title"]
                ;
                if (title) {
                    var dsTitle;
                    if (parentCanvas && parentCanvas.dataSource) {
                        var ds = parentCanvas.dataSource;
                        if (isc.isA.String(ds)) ds = isc.DS.getDataSource(ds);
                        if (ds) {
                            var dsField = ds.getField(name)
                            if (dsField) dsTitle = dsField.title;
                        }
                    }
                    if ((!dsTitle && title != isc.DataSource.getAutoTitle(name)) || 
                            (dsTitle && title != dsTitle)) 
                    {
                        edited = true;
                    }
                }
            } else {
                edited = true;
            }
        }
        return edited;
    },

    // get an editNode from a DataSourceField
    getFieldEditNode : function (field, dataSource) {
        // works for ListGrid, TreeGrid, DetailViewer, etc.  DynamicForm overrides
        var fieldType = this.creator.Class + "Field";
        var editNode = {
                type: fieldType,
                autoGen: true,
                defaults: {
                    name: field.name,
                    // XXX this makes the code more verbose since the title could be left blank and be
                    // inherited from the DataSource.  However if we don't supply one here, currently
                    // the process of creating an editNode and adding to the editTree generates a title
                    // anyway, and without using getAutoTitle().
                    title: field.title || dataSource.getAutoTitle(field.name)
                }
        }

        return editNode;
    }

});


// Edit Proxy for Layout
//-------------------------------------------------------------------------------------------

isc.defineClass("LayoutEditProxy", "EditProxy").addMethods({

    drop : function () {
        var liveObject = this.creator;

        if (this.shouldPassDropThrough()) {
            liveObject.hideDropLine();
            return;
        }

        isc.EditContext.hideAncestorDragDropLines(liveObject);

        var source = isc.EH.dragTarget,
            paletteNode,
            dropType;

        if (!source.isA("Palette")) {
            if (source.isA("FormItemProxyCanvas")) {
                source = source.formItem;
            }
            dropType = source._constructor || source.Class;
        } else {
            paletteNode = source.transferDragData();
            if (isc.isAn.Array(paletteNode)) paletteNode = paletteNode[0];
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

        // modifyEditNode() is a late-modify hook for components with unusual drop requirements
        // that don't fit in with the normal scheme of things (SectionStack only, as of August 09).
        // This method can be used to modify the editNode that is going to be the parent - or 
        // replace it with a whole different one 
        if (this.modifyEditNode) {
            dropTargetNode = this.modifyEditNode(paletteNode, dropTargetNode, dropType);
            if (!dropTargetNode) {
                liveObject.hideDropLine();
                return isc.EH.STOP_BUBBLING;
            }
        }


        // if the source isn't a Palette, we're drag/dropping an existing component, so remove the 
        // existing component and re-create it in its new position
        if (!source.isA("Palette")) {
            if (isc.EditContext._dragHandle) isc.EditContext._dragHandle.hide();
            if (source == liveObject) return;  // Can't drop a component onto itself
            var tree = liveObject.editContext.getEditNodeTree(),
            oldParent = tree.getParent(source.editNode),
            oldIndex = tree.getChildren(oldParent).indexOf(source.editNode),
            newIndex = liveObject.getDropPosition(dropType);
            liveObject.editContext.removeNode(source.editNode);

            // If we've moved the child component to a slot further down in the same parent, 
            // indices will now be off by one because we've just removeed it from its old slot
            if (oldParent == this.editNode && newIndex > oldIndex) newIndex--;
            var node;
            if (source.isA("FormItem")) {
                // If the source is a CanvasItem, unwrap it and insert the canvas into this Layout
                // directly; otherwise, we would end up with teetering arrangments of Canvases in
                // inside CanvasItems inside DynamicForms inside CanvasItems inside DynamicForms...
                if (source.isA("CanvasItem")) {
                    node = liveObject.editContext.addNode(source.canvas.editNode, dropTargetNode, newIndex);
                } else {
                    // Wrap the FormItem in a DynamicForm
                    node = liveObject.editContext.addWithWrapper(source.editNode, dropTargetNode);
                }
            } else if (source.isA("DrawItem")) {
                // Wrap the DrawItem in a DrawPane
                node = liveObject.editContext.addWithWrapper(source.editNode, dropTargetNode, true);
            } else {
                node = liveObject.editContext.addNode(source.editNode, dropTargetNode, newIndex);
            }
            if (isc.isA.TabSet(dropTargetNode.liveObject)) {
                dropTargetNode.liveObject.selectTab(source);
            } else if (node && node.liveObject) {
                isc.EditContext.delayCall("selectCanvasOrFormItem", [node.liveObject, true], 200);
            }
        } else {
            var nodeAdded;
            var clazz = isc.ClassFactory.getClass(dropType);
            if (clazz && clazz.isA("FormItem")) {
                // Create a wrapper form to allow the FormItem to be added to this Canvas
                nodeAdded = liveObject.editContext.addWithWrapper(paletteNode, dropTargetNode);
            } else if (clazz && clazz.isA("DrawItem")) {
                // Create a wrapper form to allow the DrawItem to be added to this Canvas
                nodeAdded = liveObject.editContext.addWithWrapper(paletteNode, dropTargetNode, true);
            } else {
                nodeAdded = liveObject.editContext.addNode(paletteNode, dropTargetNode,
                        liveObject.getDropPosition(dropType));
            }
            // FIXME - this is almost hackery, needs to be factored more cleanly
            if (nodeAdded != null) {
                if (paletteNode.liveObject.editProxy && paletteNode.liveObject.editProxy.nodeDropped) {
                    paletteNode.liveObject.editProxy.nodeDropped();
                } else {
                    // A SectionStackSection is just an object that we want to edit the
                    // title when dropped. It has no editProxy so we trigger the editTitle
                    // from our proxy.
                    var liveObj = paletteNode.liveObject;
                    if (isc.isA.SectionHeader(liveObj) ||
                        isc.isA.ImgSectionHeader(liveObj))
                    {
                        // Give the object a chance to draw before we start the edit, otherwise the 
                        // editor co-ordinates will be wrong
                        this.delayCall("editTitle", [liveObj]);
                    }
                }
            }
        }

        liveObject.hideDropLine();
        return isc.EH.STOP_BUBBLING;

    },

    dropMove : function () {
        if (!this.willAcceptDrop()) return false;
        if (!this.shouldPassDropThrough()) {
            this.Super("dropMove", arguments);
            var liveObject = this.creator;
            if (liveObject.parentElement && liveObject.parentElement.hideDropLine) {
                liveObject.parentElement.hideDropLine();
                if (liveObject.parentElement.isA("FormItem")) {
                    liveObject.parentElement.form.hideDragLine();
                } else if (liveObject.parentElement.isA("DrawItem")) {
                    liveObject.parentElement.drawPane.hideDragLine();
                }
            }
            return isc.EH.STOP_BUBBLING;        
        } else {
            this.creator.hideDropLine();
        }
    },

    dropOver : function () {
        if (!this.willAcceptDrop()) return false;
        if (!this.shouldPassDropThrough()) {
            this.Super("dropOver", arguments);        
            var liveObject = this.creator;
            if (liveObject.parentElement && liveObject.parentElement.hideDropLine) {
                liveObject.parentElement.hideDropLine();
                if (liveObject.parentElement.isA("FormItem")) {
                    liveObject.parentElement.form.hideDragLine();
                } else if (liveObject.parentElement.isA("DrawItem")) {
                    liveObject.parentElement.drawPane.hideDragLine();
                }
            }
            return isc.EH.STOP_BUBBLING;        
        } else {
            this.creator.hideDropLine();
        }
    }

});    

// Edit Proxy for SectionStack
//-------------------------------------------------------------------------------------------

isc.defineClass("SectionStackEditProxy", "LayoutEditProxy").addMethods({

    canAdd : function (type) { 
        // SectionStack is a special case for DnD - although it is a VLayout, its schema marks
        // children, peers and members as inapplicable.  However, anything can be put into a 
        // SectionStackSection.  Therefore, we accept drop of any canvas, and handle adding it 
        // to the appropriate section in the drop method.
        // We also accept a drop of a FormItem; this will be detected downstream and handled by
        // wrapping the FormItem inside an auto-created DynamicForm.  Similarly a DrawItem
        // can be accepted because it will be wrapped inside an auto-created DrawPane.
        if (type == "SectionStackSection") return true;
        var classObject = isc.ClassFactory.getClass(type);
        if (classObject &&
                (classObject.isA("Canvas") || classObject.isA("FormItem") || classObject.isA("DrawItem")))
        {
            return true;
        }
        return false;
    },

    //  Return the modified editNode (or a completely different one); return false to abandon 
    //  the drop
    modifyEditNode : function (paletteNode, newEditNode, dropType) {
        if (dropType == "SectionStackSection") return newEditNode;
        var dropPosition = this.creator.getDropPosition();
        if (dropPosition == 0) {
            isc.warn("Cannot drop before the first section header");
            return false;
        }

        var headers = this._getHeaderPositions();
        for (var i = headers.length-1; i >= 0; i--) {
            if (dropPosition > headers[i]) {
                // Return the edit node off the section header
                return this.creator.getSectionHeader(i).editNode;
            }
        }
        // Shouldn't ever get here
        return newEditNode;
    },

    //  getDropPosition() - explicitly called from SectionStack.getDropPosition if the user isn't doing
    //  a drag reorder of sections.
    getDropPosition : function (dropType) {
        var pos = this.creator.invokeSuper(isc.SectionStack, "getDropPosition");
        if (!dropType || dropType == "SectionStackSection") {
            return pos;
        }

        var headers = this._getHeaderPositions();
        for (var i = headers.length-1; i >= 0; i--) {
            if (pos > headers[i]) {
                return pos - headers[i] - 1;
            }
        }

        return 0;
    },

    _getHeaderPositions : function () {
        var liveObject = this.creator,
            headers = [],
            j = 0;
        for (var i = 0; i < liveObject.getMembers().length; i++) {
            if (liveObject.getMember(i).isA(liveObject.sectionHeaderClass)) {
                headers[j++] = i;
            }
        }
        return headers;
    }

});


// Edit Proxy for TabSet
//-------------------------------------------------------------------------------------------

isc.defineClass("TabSetEditProxy", "EditProxy").addMethods({

    setEditMode : function(editingOn) {
        this.Super("setEditMode", arguments);

        // If we're going into edit mode, add close icons to every tab
        var liveObject = this.creator;
        if (editingOn) {
            for (var i = 0; i < liveObject.tabs.length; i++) {
                var tab = liveObject.tabs[i];
                this.saveTabProperties(tab);
                liveObject.setCanCloseTab(tab, true);
            }
            liveObject.closeClick = function(tab) {
                liveObject.editContext.removeNode(tab.editNode);
                var proxy = liveObject.editProxy;
                isc.Timer.setTimeout(function() {proxy.manageAddIcon()}, 200);
            }
        } else {
            // If we're coming out of edit mode, revert to whatever was on the init data
            for (var i = 0; i < liveObject.tabs.length; i++) {
                var tab = liveObject.tabs[i];
                this.restoreTabProperties(tab);
                var liveTab = liveObject.getTab(tab);
                liveObject.setCanCloseTab(tab, liveTab.editNode.defaults.canClose);
            }
        }
        
        // Set edit mode on the TabBar and PaneContainer.  Note that we deliberately pass null as
        // the editNode - this allows the components to pick up the special editMode method 
        // overrides, but prevents them from actually being edited
        liveObject.tabBar.setEditMode(editingOn, liveObject.editContext, null);
        liveObject.paneContainer.setEditMode(editingOn, liveObject.editContext, null);

        this.manageAddIcon();
    },

    saveTabProperties : function (tab) {
        var liveTab = this.creator.getTab(tab);
        if (liveTab) {
            liveTab.saveToOriginalValues(["closeClick", "canClose", "icon", "iconSize",
                                          "iconOrientation", "iconAlign", "disabled"]);
        }
    },

    restoreTabProperties : function (tab) {
        var liveTab = this.creator.getTab(tab);
        if (liveTab) {
            liveTab.restoreFromOriginalValues(["closeClick", "canClose", "icon", "iconSize",
                                               "iconOrientation", "iconAlign", "disabled"]);
        }
    },

    // Called after a new node is created by a drop
    nodeDropped : function () {
        var liveObject = this.creator;
        if (isc.isA.TabSet(liveObject)) {
            this.delayCall("showAddTabEditor");
        }
    },

    showAddTabEditor : function () {
        var liveObject = this.creator,
            pos = liveObject.tabBarPosition,
            align = liveObject.tabBarAlign,
            top, left, 
            height, width, 
            bar = liveObject.tabBar;
        
        if (pos == isc.Canvas.TOP || pos == isc.Canvas.BOTTOM) {
            // Horizontal tabBar
            top = liveObject.tabBar.getPageTop();
            height = liveObject.tabBar.getHeight();
            if (align == isc.Canvas.LEFT) {
                left = this.addIcon.getPageLeft();
                width = liveObject.tabBar.getVisibleWidth() - this.addIcon.left;
                if (width < 150) width = 150;
            } else {
                width = liveObject.tabBar.getVisibleWidth();
                width = width - (width - (this.addIcon.left + this.addIcon.width));
                if (width < 150) width = 150;
                left = this.addIcon.getPageLeft() + this.addIcon.width - width;
            }
        } else {
            // Vertical tabBar
            left = liveObject.tabBar.getPageLeft();
            width = 150;
            top = this.addIcon.getPageTop();
            height = 20;
        }
        
        this.manageAddTabEditor(left, width, top, height);
    },

    manageAddIcon : function () {
        var liveObject = this.creator;

        if (liveObject.editingOn) {
            if (this.addIcon == null) {
                this.addIcon = isc.Img.create({
                    autoDraw: false, width: 16, height: 16,
                    cursor: "hand",
                    tabSet: liveObject,
                    src: "[SKIN]/actions/add.png",
                    click: function() {this.tabSet.editProxy.showAddTabEditor();}
                });
                liveObject.tabBar.addChild(this.addIcon);
            }

            var lastTab = liveObject.tabs.length == 0 ? null : liveObject.getTab(liveObject.tabs[liveObject.tabs.length-1]);
            var pos = liveObject.tabBarPosition,
                align = liveObject.tabBarAlign,
                addIconLeft,
                addIconTop;

            if (lastTab == null) {
                // Empty tabBar
                if (pos == isc.Canvas.TOP || pos == isc.Canvas.BOTTOM) {
                    // Horizontal tabBar
                    if (align == isc.Canvas.LEFT) {
                        addIconLeft = liveObject.tabBar.left + 10;
                        addIconTop = liveObject.tabBar.top + (liveObject.tabBar.height/2) - (8);
                    } else {
                        addIconLeft = liveObject.tabBar.left + liveObject.tabBar.width - 10 - (16);  // 16 = icon width
                        addIconTop = liveObject.tabBar.top + (liveObject.tabBar.height/2) - (8);
                    }
                } else {
                    // Vertical tabBar
                    if (align == isc.Canvas.TOP) {
                        addIconLeft = liveObject.tabBar.left + (liveObject.tabBar.width/2) - (8);
                        addIconTop = liveObject.tabBar.top + 10;
                    } else {
                        addIconLeft = liveObject.tabBar.left + (liveObject.tabBar.width/2) - (8);
                        addIconTop = liveObject.tabBar.top + liveObject.tabBar.height - 10 - (16)
                    }
                }
            } else {
                if (pos == isc.Canvas.TOP || pos == isc.Canvas.BOTTOM) {
                    // Horizontal tabBar
                    if (align == isc.Canvas.LEFT) {
                        addIconLeft = lastTab.left + lastTab.width + 10;
                        addIconTop = lastTab.top + (lastTab.height/2) - (8);
                    } else {
                        addIconLeft = lastTab.left - 10 - (16);  // 16 = icon width
                        addIconTop = lastTab.top + (lastTab.height/2) - (8); // 8 = half icon height
                    }
                } else {
                    // Vertical tabBar
                    if (align == isc.Canvas.TOP) {
                        addIconLeft = lastTab.left + (this.width/2) - (8);
                        addIconTop = lastTab.top + (lastTab.height) + 10;
                    } else {
                        addIconLeft = lastTab.left + (this.width/2) - (8);
                        addIconTop = lastTab.top + (lastTab.height/2) - (8); 
                    }
                }
            }
        
            this.addIcon.setTop(addIconTop);
            this.addIcon.setLeft(addIconLeft);
            this.addIcon.show();
        } else {
            if (this.addIcon && this.addIcon.hide) this.addIcon.hide();
        }
    },

    manageAddTabEditor : function (left, width, top, height) {
        
        if (!isc.isA.DynamicForm(isc.TabSet.addTabEditor)) {
            isc.TabSet.addTabEditor = isc.DynamicForm.create({
                autoDraw: false,
                margin: 0, padding: 0, cellPadding: 0,
                fields: [
                    { 
                        name: "addTabString", type: "text", 
                        hint: isc.TabSet.addTabEditorHint,
                        showHintInField: true,
                        showTitle: false,
                        keyPress : function (item, form, keyName) {
                            if (keyName == "Escape") {
                                form.discardUpdate = true;
                                form.hide();
                                return
                            }
                            if (keyName == "Enter") item.blurItem();
                        }, 
                        blur : function (form, item) {
                            if (!form.discardUpdate) {
                                form.targetComponent.editProxy.addTabs(item.getValue());
                            }
                            form.hide();
                        }
                    }
                ]
            });
        }
        
        var editor = isc.TabSet.addTabEditor;
        editor.addProperties({targetComponent: this.creator});
        editor.discardUpdate = false;
        
        var item = editor.getItem("addTabString");
        item.setHeight(height);
        item.setWidth(width);
        item.setValue(item.hint);
        
        editor.setTop(top);
        editor.setLeft(left);
        editor.show();
        item.focusInItem();
        item.delayCall("selectValue", [], 100);
    },

    addTabs : function (addTabString) {
        if (!addTabString || addTabString == isc.TabSet.addTabEditorHint) return;
        var titles = addTabString.split(",");
        for (var i = 0; i < titles.length; i++) {
            var tab = {
                type: "Tab",
                defaults: {
                    title: titles[i]
                }
            };
            var node = this.creator.editContext.addNode(this.creator.editContext.makeEditNode(tab), 
                                                     this.creator.editNode);
            this.addDefaultPane(node);
        }
    },

    addDefaultPane : function (tabNode) {
        if (!tabNode) return;
        var defaultPane = isc.addProperties({}, this.creator.defaultPaneDefaults);
        if (!defaultPane.type && !defaultPane.className) {
            defaultPane.type = defaultPane._constructor || this.creator.defaultPaneConstructor;
        }
        this.creator.editContext.addNode(this.creator.editContext.makeEditNode(defaultPane), tabNode);
    },

    // Extra stuff to do when tabSet.addTabs() is called when the tabSet is in an editable context
    // (though not necessarily actually in editMode)
    addTabsEditModeExtras : function (newTabs) {

        // Put this on a delay, to give the new tab chance to draw before we start querying its 
        // drawn size and position
        this.delayCall("manageAddIcon");
        
        // If the TabSet is in editMode, put the new tab(s) into edit mode too
        if (this.creator.editingOn) {
            for (var i = 0; i < newTabs.length; i++) {
                this.saveTabProperties(newTabs[i]);
                this.creator.setCanCloseTab(newTabs[i], true);
            }
        }
    },

    // Extra stuff to do when tabSet.removeTabs() is called when the tabSet is in an editable 
    // context (though not necessarily actually in editMode)
    removeTabsEditModeExtras : function () {

        // Put this on a delay, to give the new tab chance to draw before we start querying its 
        // drawn size and position
        this.delayCall("manageAddIcon");
    },

    //Extra stuff to do when tabSet.reorderTab() is called when the tabSet is in an editable 
    //context (though not necessarily actually in editMode)
    reorderTabsEditModeExtras : function (originalPosition, moveToPosition) {
        if (this.creator.editContext && this.creator.editContext.reorderNode) {
            this.creator.editContext.reorderNode(this.creator.editNode, originalPosition, moveToPosition);
        }
    },

    // Override of EditProxy.findEditNode.  If the item being dragged is a Tab, falls back to the 
    // Canvas impl (which will return the TabSet itself).  If the item being dragged is not a 
    // Tab, returns the currently selected Tab if it has an editNode, otherwise the first Tab 
    // with an editNode, otherwise returns the result of calling the parent element's 
    // findEditNode(), because this is a TabSet with no tabs in edit mode
    findEditNode : function (dragType) {
        this.logInfo("In TabSet.findEditNode, dragType is " + dragType, "editModeDragTarget");
        if (dragType != "Tab") {
            var tab = this.creator.getTab(this.creator.getSelectedTabNumber());
            if (tab && tab.editNode) return tab;
            for (var i = 0; i < this.creator.tabs.length; i++) {
                tab = this.creator.getTab(i);
                if (tab.editNode) return tab;
            }
            if (this.creator.parentElement) return this.creator.parentElement.editProxy.findEditNode(dragType);
        }
        return this.Super("findEditNode", arguments);
    },

    // Override completeItemDrop() to add the default pane to tabs (and drop into 
    // edit-title)
    completeItemDrop : function (paletteNode, itemIndex, rowNum, colNum, side, callback) {
        this.Super("completeItemDrop", arguments);
        if (paletteNode && (paletteNode.type || paletteNode.className) == "Tab") {
            var liveObj = paletteNode.liveObject;
            this.addDefaultPane(paletteNode);
            this.creator.selectTab(liveObj);
            
            liveObj.editProxy.delayCall("editTitle"); 
        }
    }

});


isc.defineClass("TabBarEditProxy", "EditProxy").addMethods({
    findEditNode : function (dragType) {
        
        if (dragType == "Tab") {
            // Delegate to the TabSet's findEditNode()
            return this.creator.parentElement.editProxy.findEditNode(dragType);
        } else if (this.creator.parentElement && isc.isA.Layout(this.creator.parentElement.parentElement)) {
            return this.creator.parentElement.parentElement.editProxy.findEditNode(dragType);
        }
        
        return this.Super("findEditNode", arguments);
    }

});

isc.defineClass("StatefulCanvasEditProxy", "EditProxy").addMethods({
    // Called after a new node is created by a drop
    nodeDropped : function () {
        var liveObject = this.creator;

        if (isc.isA.ImgTab(liveObject) ||
            isc.isA.Button(liveObject) ||
            isc.isA.StretchImgButton(liveObject) ||
            isc.isA.SectionHeader(liveObject) ||
            isc.isA.ImgSectionHeader(liveObject))
        {
            // Give the object a chance to draw before we start the edit, otherwise the 
            // editor co-ordinates will be wrong
            this.delayCall("editTitle");
        }
    }

});
