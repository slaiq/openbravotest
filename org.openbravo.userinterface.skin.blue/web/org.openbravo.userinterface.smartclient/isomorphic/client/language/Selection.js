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

 





//>	@class	Selection
//
// Maintains a 'selected' subset of a List or Array of objects, such as records in a record
// set, or widgets in a selectable header.<br><br>
//
// Includes methods for selecting objects and checking which objects are selected, and also for
// selecting objects as a result of mouse events, including drag selection support.<br>
// The selection object is used automatically to handle selection APIs on +link{class:ListGrid}
// and +link{class:TreeGrid} instances.<br><br>
//
//
// @visibility external
// @treeLocation    Client Reference/System
//<


//
//	create the Selection class
//
isc.ClassFactory.defineClass("Selection");

// add default properties to the class
isc.Selection.addProperties({
    //> @attr selection.selectionProperty (String : null : [IRA])
    // Property to use to mark records as selected.
    // <P>
    // Defaults to an auto-generated property name that starts with an underscore.
    // @visibility serverSelection
    //<
    //selectionProperty:null,

    //>@attr selection.data (Array | List : null : [IRWA])
    //  The set of data for which selection is being managed.  If not specified at init time
    //  this can be set up via the <code>selection.setData()</code> method.
    // @visibility serverSelection
    //<
    
    //> @attr selection.enabledProperty (String : "enabled" : [IRA])
    // Property used to indicated records as being disabled (therefore unselectable).
    //<
    
    enabledProperty:"enabled",
    
    //> @attr selection.canSelectProperty (string : "canSelect" : [IRA])
    // If set to false on a item, selection of that item is not allowed.
    //<
    
    canSelectProperty:"canSelect",

    //> @attr selection.cascadeSelection (boolean : false : [IRA])
    // Should children be selected when parent is selected? And should parent be
    // selected when any child is selected?
    // <p>
    // Note: Unloaded children are not affected and no load-on-demand is triggered.
    //<
    cascadeSelection:false,

    // _dirty - manages whether we need to update the cache of selected records.
	_dirty:true

    
});

isc.Selection.addClassProperties({
	//>	@type	SelectionStyle
	//	Different styles of selection that a list, etc. might support
	//		@visibility external
	//		@group	selection
	//
    //	@value	isc.Selection.NONE		don't select at all
	NONE : 		"none",			
    //	@value	isc.Selection.SINGLE	select only one item at a time
    SINGLE:		"single",		
	//	@value	isc.Selection.MULTIPLE	select one or more items
	MULTIPLE:	"multiple",	
    //	@value	isc.Selection.SIMPLE	select one or more items as a toggle
	// 								  so you don't need to hold down control keys to select 
    //                                  more than one object
	SIMPLE:		"simple",		
	//<

	// for generating unique IDs for each Selection 
	_selectionID : 0			
});


isc.Selection.addMethods({

//>	@method	selection.init()	(A)
//  Initialize this selection instance.<br>
//  Note: if the <code>data</code> property is not set at init time, it should be passed to
//  the selection using the <code>selection.setData</code> method
//		@group	selection
//		@param	[all arguments]	(object)	objects with properties to override from default
// @visibility serverSelection
//<
init : function () {

	// get unique ID and selection properties
	if (!this.selectionProperty) this.selectionProperty = "_selection_"+isc.Selection._selectionID++;

        this.partialSelectionProperty = "_partial" + this.selectionProperty;

	// set the data object so we get notification for add and delete, etc.
	//	NOTE: if the data object wasn't set, use a new arrays
	this.setData((this.data ? this.data : []));
},

// override destroy to clean up pointers to this.data
destroy : function () {
    if (this.data) this.ignoreData(this.data);
    delete this.data;
    
    // selections aren't stored in global scope so no need to clear window[this.ID]
    this.Super("destroy", arguments);
},

//>	@method	selection.setData()
//			Initialize selection data.<br><br>
//			Call this method to associate the selection with a different data object.<br>
//          <i>Note: No need to call this if the contents of the selection's data is modified</i>
//		@group	selection
//		@param		newData	(array)		new data to maintain selection in
// @visibility serverSelection
//<
setData : function (newData) {		

	// if we are currently pointing to data, stop observing it
	if (this.data != null) this.ignoreData(this.data);	

	// remember the new data
	this.data = newData;

	// observe the new data so we will update automatically when it changes
	if (this.data != null) this.observeData(this.data);
    	
	// Note that any cache we have is out of date.
	this.markForRedraw();
	
},


//>	@method	selection.observeData()	(A)
//			Observe methods on the data so we change our state.
//			Called automatically by selection.setData().
//			Observes the data.dataChanged() method to invalidate the selection cache
//		@group	selection
//
//
//	@param	data	(array)		new data to be observed
// @visibility internal
//<
observeData : function (data) {
    var isRS = isc.ResultSet && isc.isA.ResultSet(data);
    if (isRS) {
        this.observe(data, "dataChanged", function (operationType, originalRecord, rowNum, updateData, filterChanged, dataFromCache) {
            this.dataChanged(operationType, originalRecord, rowNum, updateData, filterChanged, dataFromCache);
        });
    } else {
        this.observe(data, "dataChanged", function () {
            this.dataChanged();
        });
    }
    
    if (data.dataArrived) {
        if (isRS) {
            this.observe(data, "dataArrived", function (startRow, endRow, dataFromCache) {
                this.dataArrived(startRow, endRow, dataFromCache);
            });
        } else {
            this.observe(data, "dataArrived", function () {
                this.dataChanged();
            });
        }
    }
},

//>	@method	selection.ignoreData()	(A)
//			Stop observing methods on data when it goes out of scope.
//			Called automatically by setData
//		@group	selection
//
//		@param	data	(array)		old data to be ignored
// @visibility internal
//<
ignoreData : function (data) {
    if (!data) return;
    if (this.isObserving(data, "dataChanged")) this.ignore(data, "dataChanged");
    if (this.isObserving(data, "dataArrived")) this.ignore(data, "dataArrived");
},

// dataChanged implementation performs a couple of tasks:
// - reselectOnUpdate attribute set by DBC. If set, reapply selection when
//   an updated record is folded back into the ResultSet cache
// - if the dataChange is due to a local filter revealing new records that were
//   in the ResultSet's local cache but hidden due to non-matching criteria,
//   wipe any "selected" property from them so we don't have odd stale-seeming
//   selections popping up on refilter.
// - wipe our complete selection cache to account for records that may have been
//   removed from this.data
dataArrived : function (startRow,endRow,dataFromCache) {
    // This method can fire from ResultSet dataChanged doing a filterLocalData()
    // Skip calling dataChanged directly in this case - it'll fire at the end of
    // the resultSet level 'dataChanged' method.
    if (!this.data || this.data._isChangingData()) {
        return;
    }
    this.dataChanged(null, null, null, null, null, dataFromCache);
},

dataChanged : function (operationType,originalRecord,rowNum,updateData,filterChanged,dataFromCache) {

    if (this.reselectOnUpdate && operationType == "update" && originalRecord != null &&
        originalRecord[this.selectionProperty]) 
    {
        
        if (rowNum != -1) {
            var modifiedRecord = this.data.get(rowNum);
            this.performReselectOnUpdate(modifiedRecord);
        }

    // 'dataFromCache' param - only applies to resultSets:
    // Implies the change of data was satisfied from a client side cache of records
    // rather than fresh data coming from the server, and as such we (the selection) 
    // may have already seen these records and marked as selected.
    // If this is the case, the selected property should be considered stale and
    // cleared.
    // Use case: A record was selected in a resultSet, the criteria were changed
    // such that it was removed from the filtered data (but retained in 
    // the client-side "allRows" cache), then the criteria changed again such that
    // the record is re-introduced into the filtered data set
    // We have to treat as stale - otherwise it's easy to run into bugs such as
    // selectionType:"single" widgets ending up with multiple selected rows.
    } else if (dataFromCache) {
        if (this._dirty) this.cacheSelection();
        // return the cached selection list if possible
        var selection = this._cache,
            newCache = [],
            data = this.getItemList(),
            length = data.getLength();
        
        if (selection == null ||
            isc.isA.ResultSet != null && isc.isA.ResultSet(data) && !data.lengthIsKnown()) {
        } else {
            for (var i = 0; i < length; i++) {
                // getCachedRow won't trigger fetches if working with a remote dataset
                var item = data.getCachedRow(i),
                    selected = item == null ? false :
                    
                        
                        !!item[this.selectionProperty];
                
                var inCache;
                if (selected) {
                    inCache = selection.contains(item);
                    if (inCache) {
                        newCache.add(item);
                    } else {                            
                        
                        item[this.selectionProperty] = false;       
                    }
                }
                // If the record is not marked as selected, nothing to do
            }
            
            
        }
    }	        
    this.markForRedraw();
},


performReselectOnUpdate : function (modifiedRecord) {
    this.select(modifiedRecord);
},

//>	@method	selection.markForRedraw()
//			Mark the selection as dirty, so it will be recalculated automatically			
//		@group	selection
// @visibility internal
//<
markForRedraw : function () {
	this._dirty = true;
},	

//>	@method	selection.isSelected()
// Return true if a particular item is selected
//
// @param item (object) object to check	
// @return (boolean) true == object is selected, false == object is not selected
// @group selection
// @visibility external
//<
isSelected : function (item) {
    
    if (this._dirty) {
        this.cacheSelection();
    }

	if (item == null) return false;
	
	return !!item[this.selectionProperty];
},

//> @method selection.isPartiallySelected()
// When using tree-oriented selection modes like +link{treeGrid.cascadeSelection}, returns true
// if the record is considered partially selected because only some of it's children are
// selected.
//
// @param  item	(object)  object to check	
// @return (boolean)      true == object is partially selected
//                        false == object is not partially selected
// @group  selection
// @visibility external
//<
isPartiallySelected : function (item) {

    
    if (this._dirty) this.cacheSelection();
    if (item == null) return false;
    
    return !!item[this.partialSelectionProperty];
},


//>	@method	selection.anySelected()
// Whether at least one item is selected
//		@group	selection
//
//		@return		(boolean)	true == at least one item is selected
//								false == nothing at all is selected
// @visibility external
//<
anySelected : function () {
	// return if the selection is non-empty
	return this.getSelection().length > 0;
},

//>	@method	selection.getLength()
// Returns the number of selected records.
//
// @return (int) number of selected records
// @group selection
// @visibility external
//<
getLength : function () {
	return this.getSelection().length;
},


//>	@method	selection.multipleSelected()
//	Whether multiple items are selected
//		@group	selection
//
//		@return		(boolean)	true == more than one item is selected
//								false == no items are selected, or only one item is selected
// @visibility external
//<
multipleSelected : function () {
	return this.getSelection().length > 1;
},


//> @method selection.getSelection()
// Return an ordered array of all of the selected items
// @param [excludePartialSelections] (Boolean) When true, partially selected records will not be returned.
//                                   Otherwise, all fully and partially selected records are
//                                   returned.
// @return (array) list of selected items
// @group selection
// @visibility external
//<
getSelection : function (excludePartialSelections) {
    // if the selection is dirty, cache it again
    if (this._dirty) {
        if (this._cachingSelection) this._cachingSelection = false;
        this.cacheSelection();
    }

    // return the cached selection list if possible
    var selection = this._cache;
 
    // If partial selections are excluded, built a new list of full selections only.
    if (excludePartialSelections == true && selection != null && selection.length > 0) {
        var cache = this._cache;
        selection = [];

        // Cache includes both fully and partially selected nodes.
        for (var i = 0; i < cache.length; i++) {
            var item = cache[i];
            if (!this.isPartiallySelected(item)) {
                selection[selection.length] = item;
            }
        }               
    }

    return selection;
},


//>	@method	selection.getSelectedRecord()
//			Return the first item in the list that is selected.<br><br>
//
//			Note that this should only be used if you know that one only one item
//			 may be selected, or you really don't care about items after the first one.<br><br>
//
//			To get all selected objects, use <code>+link{method:selection.getSelection()}</code>
//		@group	selection
//
//		@return		(object)	first selected record, or null if nothing selected
// @visibility external
//<
getSelectedRecord : function () {
    var selection = this.getSelection();
    if (selection && selection.length > 0) return selection[0];
},


//>	@method	selection.cacheSelection()	(A)
// Cache the selected records since we operate on it so often.
// Sets an internal variable _cache to hold the selection.
// @group selection
// @visibility internal
//<

cacheSelection : function () {
    // Don't allow this method to fire recursively, or in response to 'setSelection'
    // if we're marked as dirty
    if (this._cachingSelection || this._settingSelected || this._supressCaching) {
        return;
    }
	// create a new array to hold the cached selection
	this._cache = [];

    var data = this.getItemList(),
        length = data.getLength();
 
    
    if (isc.isA.ResultSet != null && isc.isA.ResultSet(data) && !data.lengthIsKnown()) {
        this._dirty = false;
        return;
    }
    this._cachingSelection = true;

	// iterate over the records of the list, selecting those that have the selection property set
	var delayCache = false;
    for (var i = 0; i < length; i++) {

        // getCachedRow won't trigger fetches if working with a remote dataset
        var item = data.getCachedRow(i);
		if (item != null && this.isSelected(item)) {
		    // If cascadeSelection is true and new data has arrived, it may be
		    // selected. In this case we need to update the 'partial' selected state
		    // of parents, and the selected state of descendents.
		    // To handle this - call 'setSelected' with the flag to force a recalculation of
		    // cascading selection, and then loop through all records a second time, updating
		    // cache.
		    
		    if (this.cascadeSelection && !this.isPartiallySelected(item)) {
		        this.setSelected(item, true, null, true);
		        delayCache = true;
		    }
		    if (!delayCache) {
    			this._cache[this._cache.length] = item
            }
        }
    }
    // cascading selection - we may have actually manipulated our selection to mark 
    // parents as partially selected / children as entirely selected - loop through all 
    // nodes again!
    if (delayCache) {
        this._cache = [];
        for (var i = 0; i < length; i++) {
    
            // getCachedRow won't trigger fetches if working with a remote dataset
            var item = data.getCachedRow(i);
            if (item != null && this.isSelected(item)) {
                this._cache[this._cache.length] = item
            }
        }
    }

    this._cachingSelection = false;
    //this.logWarn("***** - selection re-cached - *******");

	// note that the selection is no longer dirty
	this._dirty = false;
},


_cacheSelectionAsync : function (thisArg, timerEventProp, batchSize, callback, state) {
    if (state == null || state.afterGetItemList) {
        if (state == null) {
            // Don't allow this method to fire recursively, or in response to 'setSelection'
            // if we're marked as dirty
            if (this._cachingSelection || this._settingSelected || this._supressCaching) {
                callback.call(thisArg);
                return;
            }

            var me = this;
            this._getItemListAsync(thisArg, timerEventProp, batchSize, function (data) {
                var state = { data: data, afterGetItemList: true };
                thisArg[timerEventProp] = me.delayCall(
                    "_cacheSelectionAsync", [thisArg, timerEventProp, batchSize, callback, state], 0);
            });
            return;
        }

        state.afterGetItemList = false;

        // create a new array to hold the cached selection
        state.cache = [];
        state.length = state.data.getLength();

        
        if (isc.isA.ResultSet != null && isc.isA.ResultSet(state.data) && !state.data.lengthIsKnown()) {
            this._dirty = false;
            this._cache = cache;
            callback.call(thisArg);
            return;
        }
        this._cachingSelection = true;
        state.inFirstLoop = true;
        state.delayCache = false;
        state.i = 0;
    }
    var cache = state.cache,
        data = state.data,
        length = state.length,
        inFirstLoop = state.inFirstLoop,
        delayCache = state.delayCache,
        i = state.i,
        dirty = this._dirty;

    // Prevent isSelected() from calling cacheSelection().
    this._dirty = false;

    if (inFirstLoop) {
        // iterate over the records of the list, selecting those that have the selection property set
        var maxI = Math.min(length, i + batchSize);
        for (; i < maxI; i = state.i = i + 1) {

            // getCachedRow won't trigger fetches if working with a remote dataset
            var item = data.getCachedRow(i);
            if (item != null && this.isSelected(item)) {
                // If cascadeSelection is true and new data has arrived, it may be
                // selected. In this case we need to update the 'partial' selected state
                // of parents, and the selected state of descendents.
                // To handle this - call 'setSelected' with the flag to force a recalculation of
                // cascading selection, and then loop through all records a second time, updating
                // cache.
                
                if (this.cascadeSelection && !this.isPartiallySelected(item)) {
                    this.setSelected(item, true, null, true);
                    delayCache = state.delayCache = true;
                }
                if (!delayCache) {
                    cache[cache.length] = item;
                }
            }
        }
        if (i != length) {
            this._dirty = dirty;
            this._cachingSelection = false;
            thisArg[timerEventProp] = this.delayCall(
                "_cacheSelectionAsync", [thisArg, timerEventProp, batchSize, callback, state], 0);
            return;
        }
        inFirstLoop = state.inFirstLoop = false;
    }
    if (!inFirstLoop && delayCache) {
        // cascading selection - we may have actually manipulated our selection to mark
        // parents as partially selected / children as entirely selected - loop through all
        // nodes again!

        cache = state.cache = [];
        i = state.i = 0;
        var maxI = Math.min(length, i + batchSize);
        for (; i < length; i = state.i = i + 1) {

            // getCachedRow won't trigger fetches if working with a remote dataset
            var item = data.getCachedRow(i);
            if (item != null && this.isSelected(item)) {
                cache[cache.length] = item;
            }
        }
        if (i != length) {
            this._dirty = dirty;
            this._cachingSelection = false;
            thisArg[timerEventProp] = this.delayCall(
                "_cacheSelectionAsync", [thisArg, timerEventProp, batchSize, callback, state], 0);
            return;
        }
    }

    this._cachingSelection = false;
    //this.logWarn("***** - selection re-cached - *******");

    // note that the selection is no longer dirty
    this._dirty = false;

    this._cache = cache;
    callback.call(thisArg);
    return;
},


//>	@method	selection.setSelected()	(A)
// Select or deselect a particular item.<br><br>
// All other selection routines go through this one, so by observing this routine you can
// monitor all selection changes.
//		@group	selection
//
//		@param	item		(object)	object to select
//		@param	newState	(boolean)	turn selection on or off	
// 	
//		@return			(boolean)	true == selection actually changed, false == no change
// @visibility external
//<
// We need the cascadingDirection to avoid changing direction while recursing through tree.
_$up:"up",
_$down:"down",
setSelected : function (item, newState, cascadingDirection, recalculate) {
    // if the item is null, just return
	if (item == null) return false;
	// bail if we don't have valid data
	if (this.data == null || this.data.destroyed) return false;
	
	// if the item is not enabled, just return
	if (item[this.enabledProperty] == false) return false;
    // if the item cannot be selected, just return
    if (item[this.canSelectProperty] == false) return false;
	
 	var settingSelected = this._settingSelected;
 	this._settingSelected = true;
	
    var property = this.selectionProperty,
        partialProperty = this.partialSelectionProperty,
        childProp = this.data.childrenProperty || "children",
        isNode = false;
    
    var oldPartialValue = (isNode ? item.getAttribute(partialProperty) : item[partialProperty]);

	// default to selecting the item
	if (newState == null) newState = true;

    // Set partial property as needed.
    if (this.cascadeSelection && !this.useRemoteSelection) {
	
        // If this is a parent node and we are selecting/deselecting up the tree,
        // need to determine if the selection is full or partial.
        if (cascadingDirection == this._$up) {
            var partialValue = false,
                length = item[childProp] ? item[childProp].length : 0;
            
            for (var i = 0; i < length; i++) {
                var child = item[childProp].get(i),
                    isChildNode = false;
                
                var partialChild = (isChildNode ? child.getAttribute(partialProperty)
                                                : child[partialProperty])
                ;
                if (partialChild ||
                    (newState && !this.isSelected(child)) ||
                    (!newState && this.isSelected(child)))
                {
                    partialValue = true;
                    break;
                }
            }
            
            if (isNode) {        
                item.setAttribute(partialProperty, partialValue + "");
            } else {
                item[partialProperty] = partialValue;
            }

            // If deselecting but there is a partial selection, the node must still be selected.
            if (newState != partialValue) newState = true;
        } else if (item[childProp] && item[childProp].length > 0) {
            // Make sure a left over partial selection is cleared
            if (isNode) {
                item.removeAttribute(partialProperty);
            } else {
                delete item[partialProperty];
            }
        }
    }

    // get the oldState of the item, for detecting changes
    var oldState = isNode ? item.getAttribute(property) : item[property];
    if (oldState == null) oldState = false;
	// set the state of the item
    if (isNode) {
        
    	item.setAttribute(property, (newState == true) + "");
        //this.logWarn("set attribute on: " + this.echoLeaf(item) + " to: " + newState + 
        //             ", now reads: " + item.getAttribute(property));
    } else {
    	item[property] = newState;
    }
    
    
	
    // remember that this was the last item to be selected
    this.lastSelectionItem = item;
    this.lastSelectionState = newState;
    this.lastSelectionPreviousState = oldState;
    this.lastSelectionPartialValue = partialValue;
    this.lastSelectionPreviousPartialValue = oldPartialValue;

    // if no change to state of item, simply return false
    var newPartialValue = (isNode ? item.getAttribute(partialProperty) : item[partialProperty]);
    var changed = true;
    if (newState == oldState && newPartialValue == oldPartialValue) {
        changed = false;
    }
    if (!recalculate && changed == false) {
        if (!settingSelected) this._settingSelected = false;
        return false;
    }
	
    

	// note that the selection is dirty so it can be recalculated
	this.markForRedraw();

    // Select/deselect parent and child records
    if (this.cascadeSelection &&
        !this.useRemoteSelection)
    {
        var lastItem = item,
            lastState = newState,
            lastPrevState = oldState,
            lastPartialState = partialValue,
            lastPrevPartialState = oldPartialValue;
            
        
        var cascadeSource = false;
        if (this.cascadeSyncOnly == null) {
            cascadeSource = true;
            this.cascadeSyncOnly = !changed;
        }
        
        
        // Select/deselect child records
        if (cascadingDirection != this._$up && !isNode &&
            item[childProp] && item[childProp].length > 0)
        {
            this.selectList (item[childProp], newState, this._$down);
        }
        
        if (changed || cascadeSource) {
        // Select/deselect parent records
            if (cascadingDirection != this._$down && isc.isA.Tree(this.data)) {
                var parent = this.data.getParent(item);
                // note: we do this even if isSelected == newState -- we may need
                // to set a partial selected state to fully selected or vice-versa.
                if (parent) {
                    this.setSelected (parent, newState, this._$up);
                }
            }
        }

        this.lastSelectionItem = lastItem;
        this.lastSelectionState = lastState;
        this.lastSelectionPreviousState = lastPrevState;
        this.lastSelectionPartialValue = lastPartialState;
        this.lastSelectionPreviousPartialValue = lastPrevPartialState;
        if (cascadeSource) {
            this.cascadeSyncOnly = null;
        }
    }
    if (!settingSelected) this._settingSelected = false;

    // Fire selectionChange on this.target if present
    
    if (changed && this.target && this.target.selectionChange) {
        this.target.selectionChange(item, newState);
    }
	// return true to indicate that there was a change in the selection state
	return true;
},

//>	@method	selection.select()
//			Select a particular item
//		@group	selection
//
//		@param		item	(object)	object to select	
//		@return				(boolean)	true == selection actually changed, false == no change
// @visibility external
//<
select : function (item) {
    return this.setSelected(item, true);
},

//>	@method	selection.deselect()
//			Deselect a particular item
//		@group	selection
//
//		@param		item	(object)	object to select	
//		@return				(boolean)	true == selection actually changed, false == no change
// @visibility external
//<
deselect : function (item) {
    return this.setSelected(item, false);
},


//>	@method	selection.selectSingle()
// Select a single item and deselect everything else
//		@group	selection
//
//		@param		item	(object)	object to select	
//		@return				(boolean)	true == selection actually changed, false == no change
// @visibility external
//<
  selectSingle : function (item) {
    var itemWasSelected, othersWereSelected;
    

    // deselect the item if selected (and remember whether it was)
    itemWasSelected = this.deselect(item);
    // deselect everything else
    othersWereSelected = this.deselectAll();
    // Reselect the single item
    this.select(item);

    // return true if the item became selected or others were cleared
    return !itemWasSelected || othersWereSelected;
},

//>	@method	selection.selectList()
// Select an array of items (subset of the entire list)
//		@group	selection
//
//		@param		list	(object[])	array of objects to select	
//		@return				(boolean)	true == selection actually changed, false == no change
// @visibility external
//<
selectList : function (list, newState) {
    if (newState == null) newState = true;
    if (!list) return false;

    
    this.cacheSelection();
    
    var cache = this._cache;
    var length = list.getLength(),
        selectionChanged = [];
    for (var i = 0; i < length; i++) {
        var item = list.get(i),
            selected = this.isSelected(item);
        if (selected == newState) continue;
        
        selectionChanged[selectionChanged.length] = item;
    }
    
    
    var orig_supressCaching = this._supressCaching;
    this._supressCaching = true;

    var anyChanged = false, length = selectionChanged.length;
    
    for (var i = 0; i < length; i++) {
        var item = selectionChanged[i];
        
        // incrementally update the cache before calling 'setSelected'
        
        if (newState) {
            cache[cache.length] = item;
        } else {
            cache.removeAt(cache.indexOf(item));
        }
        
        anyChanged = this.setSelected(item, newState) || anyChanged;
    }

    this._supressCaching = orig_supressCaching;
    this.cacheSelection();

    return anyChanged;
},

//>	@method	selection.deselectList()
//			Deselect an array of items (subset of the entire list)
//		@group	selection
//
//		@param		list	(object[])	array of objects to select	
//		@return				(boolean)	true == selection actually changed, false == no change
// @visibility external
//<
deselectList : function (list) {
    return this.selectList(list, false);
},


//>	@method	selection.selectAll()
//			Select ALL records of the list
//		@group	selection
//
//		@return				(boolean)	true == selection actually changed, false == no change
// @visibility external
//<
selectAll : function () {
	return this.selectRange(0, this.getItemList().getLength());
},

//>	@method	selection.deselectAll()
//			Deselect ALL records of the list
//		@group	selection
//
//		@return				(boolean)	true == selection actually changed, false == no change
// @visibility external
//<
deselectAll : function () {

    
    

    return this.deselectList(this.getSelection());
},


//>	@method	selection.selectItem()
// Select a particular item by its position in the list
//
//		@param	position	(number)	index of the item to be selected
//		@return				(boolean)	true == selection actually changed, false == no change
// @group selection
// @visibility external
//<
selectItem : function (position) {
	return this.selectRange(position, position+1);
},


//>	@method	selection.deselectItem()
// Deselect a particular item by its position in the list
//
//		@param	position	(number)	index of the item to be selected
//		@return				(boolean)	true == selection actually changed, false == no change
// @group selection
// @visibility external
//<
deselectItem : function (position) {
	return this.deselectRange(position, position+1);
},





//>	@method	selection.selectRange()
//			Select range of records from <code>start</code> to <code>end</code>, non-inclusive.
//		@group	selection
//
//		@param	start	    (number)	start index to select
//		@param	end		    (number)	end index (non-inclusive)
//      @param  [newState]  (boolean)   optional new selection state to set.  True means
//                                      selected, false means unselected.  Defaults to true.
//
//		@return				(boolean)	true == selection actually changed, false == no change
// @visibility external
//<
selectRange : function (start, end, newState) {
    if (newState == null) newState = true;

    // Use visible records for range selection
    var data = this.data;

    

    if (isc.isA.ResultSet != null && isc.isA.ResultSet(data) && 
        !data.rangeIsLoaded(start, end)) 
    {
        this.warnSelectionRangeNotLoaded();
        return false; // no change
    }

    return this.selectList(data.getRange(start, end), newState);
},

warnSelectionRangeNotLoaded : function () {
    isc.warn(this.selectionRangeNotLoadedMessage);
},

//> @attr selection.selectionRangeNotLoadedMessage (String : Can't select that many records at once.&lt;br&gt;&lt;br&gt;Please try working in smaller batches. : IRWA)
//
// Message to display to the user in a <code>warn</code> dialog if +link{selection.selectRange()} is
// called for a selection on a ResultSet, where the range of records to be selected has not been
// loaded.
// @group i18nMessages
// @visibility external
//<
selectionRangeNotLoadedMessage:"Can't select that many records at once.<br><br>" + 
                                "Please try working in smaller batches.",


//>	@method	selection.deselectRange()
//			Deselect range of records from <code>start</code> to <code>end</code>, non-inclusive
//
//		@group	selection
//
//		@param	start	(number)	start index to select
//		@param	end		(number)	end index (non-inclusive)
//
//		@return				(boolean)	true == selection actually changed, false == no change
// @visibility external
//<
deselectRange : function (start, end) {
    return this.selectRange(start, end, false);
},

// DOCFIX: this methods shouldn't require the "target", a Canvas.  Need to fix that before we make
// them public.

//>	@method	selection.selectOnMouseDown()	(A)
// Update the selection as the result of a mouseDown event.
// Handles shift, control, etc. key selection as well.
// Call this from a mouseDown handler.
//
// @group selection, mouseEvents
//
// @param target (Canvas) target object
// @param position (number) position where mouse went down on
//
// @return (boolean) true == selection was changed, false == no change
//<
selectOnMouseDown : function (target, recordNum) {
    
    // modify selection according to the specified style (defaulting to multiple selection)
    var selectionType = target.selectionType || isc.Selection.MULTIPLE;

	// if the target's selectionType is NONE, just bail
	if (selectionType == isc.Selection.NONE)	return false;
	
    // remember mouseDown location in case we start drag selecting
    this.startRow = this.lastRow = recordNum;

	//>DEBUG
	this.logDebug("selectOnMouseDown: recordNum: " + recordNum);
	//<DEBUG
	
    // Pull record based on the visible records
	var record = this.data.get(recordNum),
		recordSelected = this.isSelected(record),
		selection = this.getSelection()
    ;

    // prevent mouse-based selection of the LOADING record.  This doesn't make sense and create
    // client-side JS errors very easily.
    if (Array.isLoading(record)) return false;

	// clear flags for deselecting records on mouseUp
	// these are set in the simple and normal cases below (3 and 5)
	// see selectOnMouseUp() for details
	this.deselectRecordOnMouseUp = false;
	this.deselectOthersOnMouseUp = false;
    
    
    // In Windows ctrl-click works allows multiple independent row selection/deselection
    // In Mac the equivalent functionality occurs with the Apple key (meta key), since
    //  on that platform ctrl+click == right click
    
    var metaKeyDown = (isc.Browser.isMac ? isc.EventHandler.metaKeyDown() 
                                         : isc.EventHandler.ctrlKeyDown()),
        shiftKeyDown = isc.EH.shiftKeyDown();

	// Case 1: SINGLE selection
	if (selectionType == isc.Selection.SINGLE) {
        // On ctrl+click allow deselection
        
        if (metaKeyDown && recordSelected) this.deselect(record);
        else if (!recordSelected) this.selectSingle(record);
        else return false;

		return true;

    // Case 2: Shift-selection (select contiguous range of records)
    
	} else if (shiftKeyDown) {

		// if nothing was selected,
		if (selection.length == 0) {
			// simply select that record
			this.select(record);
			return true;				

		// otherwise since something was selected
		} else {
			// select a range of records (visible)
			var data = this.data,
				firstRecord = data.indexOf(selection[0]),
				lastRecord   = data.indexOf(selection.last())
			;
			// if the clicked record is the last record or beyond
			if (recordNum >= lastRecord) {
				// select from the firstRecord to the clicked record
				this.selectRange(firstRecord, recordNum + 1);

			// else if the clicked record is the first record or before
			} else if (recordNum <= firstRecord) {
				// select from the clicked record to the lastRecord
				this.selectRange(recordNum, lastRecord + 1);

			// otherwise it's in between the start and end record
			} else {
				// select from the firstRecord to the clicked record
				this.selectRange(firstRecord, recordNum + 1);
				// and deselect form the clickedRecord + 1 to the lastRecord
				this.deselectRange(recordNum + 1, lastRecord +1);
			}
			return true;
		}


	// Case 3: SIMPLE selection (toggle selection of this record, but defer deselection until
    // mouseUp)
	} else if (selectionType == isc.Selection.SIMPLE) {

		if (!recordSelected) {
			this.select(record);
			return true;
		} else {
			this.deselectRecordOnMouseUp = true;
			return false;
		}


	// Case 4: meta-key selection in a multiple selection range 
    // (simply toggle selection of this record) 
	} else if (metaKeyDown) {

		this.setSelected(record, !recordSelected);
		return true;

	// Case 5: normal selection (no modifier keys) in a multiple selection range
	} else {

        if (!recordSelected) {
            // if you click outside of the selection, select the new record and deselect
            // everything else
			this.selectSingle(record);
			return true;
        } else if (isc.EventHandler.rightButtonDown()) {
            // never deselect if you right click on the selection, unless you start drag
            // selecting
            this.deselectOnDragMove = true;
            return false;
        } else {
            // simpleDeselect mode: this mode is designed to make it easy to entirely get rid
            // of your selection, so you don't have to know about ctrl-clicking.  In a
            // nutshell, if you click on the existing selection, it will be entirely
            // deselected. 

            if (this.dragSelection) {
                if (this.simpleDeselect) {
                    // if you click on the selection, deselect the entire selection including
                    // the clicked-on cell.  Later, if a drag begins, select the clicked-on
                    // cell.
                    this.deselectAll();
                    this.selectOriginOnDragMove = true;
                    return true;
                }
                // for a drag selection, deselect others immediately; otherwise we'll be
                // dragging out a new selection within/overlapping with an existing selection,
                // which we only want to do on a ctrl-click.  This matches Excel.
                this.selectSingle(record);
                return true;
            } else {
                if (this.simpleDeselect) {
                    // deselect everything on mouseUp, including the cell clicked on
                    this.deselectAllOnMouseUp = true;
                } else {
                    // if we click in a multiple selection, deselect everything but the
                    // clicked-on item, but don't do it until mouseUp in order to allow
                    // dragging the current selection.  This matches Windows Explorer.
                    this.deselectOthersOnMouseUp = (selection.length > 1);
                }
                return false;
            }
        }
	}

},

//>	@method	selection.selectOnDragMove()	(A)
//			During drag selection, update the selection as a result of a dragMove event
//
//		@group	selection, mouseEvents
//
//		@param	target	(Canvas)	target object
//		@param	position (number)	position where mouse went down on
//
//		@return			(boolean)	true == selection was changed, false == no change
//
//<
selectOnDragMove : function (target, currRow) {
    var startRow = this.startRow,
        lastRow = this.lastRow;

    // If the mouse has moved further away from the start position since the last dragMove, select
    // more cells.  If it's moved closer to the start position, deselect cells.
    if (currRow < 0) {
        //>DEBUG
        this.logWarn("selectOnDragMove: got negative coordinate: " + currRow);
        //<DEBUG
        return;
    }

    if (currRow == lastRow) return; // no change

    

    if (this.selectOriginOnDragMove) {
        this.select(this.data.getItem(startRow));
        this.selectOriginOnDragMove = false;
    } else if (this.deselectOnDragMove || this.deselectAllOnMouseUp || this.deselectOthersOnMouseUp) {
        // deselect on dragMove is for right-dragging.  The others flags are failsafes in case you
        // use drag selection without setting the flag.
        this.selectSingle(this.data.getItem(startRow));
        this.deselectAllOnMouseUp = this.deselectOthersOnMouseUp = this.deselectOnDragMove = false;
    }

    if ((currRow > startRow && startRow > lastRow) || 
        (lastRow > startRow && startRow > currRow)) 
    {
        //this.logWarn("dragged from one side of start to the other");
        // dragged from one side of start to the other
        this.deselectAll();
        // select from start to current inclusive
        if (startRow > currRow) {
            this.selectRange(currRow, startRow+1);
        } else {
            this.selectRange(startRow, currRow+1);
        }
    } else if (startRow >= lastRow && lastRow > currRow) {
        //this.logWarn("increasing selection on the left of start");
        // increasing selection on the left of start
        this.selectRange(currRow, lastRow);
    } else if (startRow >= currRow && currRow > lastRow) {
        //this.logWarn("decreasing selection on the left of start");
        // decreasing selection on the left of start
        this.deselectRange(lastRow, currRow);
    } else if (startRow <= currRow && currRow < lastRow) {
        //this.logWarn("decreasing selection on the right of start");
        // decreasing selection on the right of start
        this.deselectRange(currRow+1, lastRow+1);
    } else if (startRow <= lastRow && lastRow < currRow) {
        //this.logWarn("increasing selection on the right of start");
        // increasing selection on the right of start
        this.selectRange(lastRow, currRow+1);
    //>DEBUG
    } else {
        this.logWarn("dragMove case not handled: lastRow: " + lastRow + 
                     ", currRow: " + currRow + ", startRow " + startRow);
    //<DEBUG
    }
    
    this.lastRow = currRow;
},

//>	@method	selection.selectOnMouseUp()	(A)s
// Update the selection as the result of a mouseUp event.
// We currently use this to defer deselection for drag-and-drop of multiple records.
// Call this from a mouseUp handler.
//
// @param target (Canvas) target object
// @param recordNum (number) record number mouse went down on
//
// @return (boolean) true == selection was changed, false == no change
// @see ListGrid.mouseUp()
// @group selection, mouseEvents
//<
selectOnMouseUp : function (target, recordNum) {
	// if the target's selectionType is NONE, just bail
	if (target.selectionType == isc.Selection.NONE)	return false;

	//>DEBUG
	this.logDebug("selectOnMouseUp: recordNum: " + recordNum);
	//<DEBUG

	// JMD 020828:
	//		If multiselection is on and no modifier keys are down, we need to
	// deselect any rows other than the one that is clicked. BUT, we can't do this in
	// selectOnMouseDown() because the user might be clicking on a row in a multiple selection
	// to initiate a drag operation with all of the selected rows. So in selectOnMouseDown()
	// we set a deselectOthersOnMouseUp flag that we can check here and do the deselection
	// if necessary.
	//		Similarly, if SIMPLE selection is enabled we don't want to deselect the current
	// record if the user is initiating a drag. We set a deselectRecordOnMouseUp flag for in this case.
	//
    // We never deselect anything on rightMouseUp since you would right click to show a context menu
    // to operate on the current selection.
    var returnVal = false;
	if (this.deselectOthersOnMouseUp) {
	    returnVal = this.selectSingle(this.data.getItem(recordNum));
        this.deselectOthersOnMouseUp = false;
	} else if (this.deselectRecordOnMouseUp) {
		returnVal = this.deselect(this.data.getItem(recordNum));
        this.deselectRecordOnMouseUp = false;
    } else if (this.deselectAllOnMouseUp) {
        returnVal = this.deselectAll();
        this.deselectAllOnMouseUp = false;
	}
	return returnVal;
},

getItemList : function () {
    if (this.data && isc.isA.Tree(this.data)) return this.data.getNodeList();
    return (this.data ? this.data : []);
},


_getItemListAsync : function (thisArg, timerEventProp, batchSize, callback) {
    if (this.data && isc.isA.Tree(this.data) && isc.isA.Function(this.data._getNodeListAsync)) {
        this.data._getNodeListAsync(thisArg, timerEventProp, batchSize, callback);
    } else {
        callback.call(thisArg, this.getItemList());
    }
}
});	// END isc.Selection.addMethods()


