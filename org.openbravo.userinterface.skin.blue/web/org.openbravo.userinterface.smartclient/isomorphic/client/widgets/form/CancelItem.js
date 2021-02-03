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

 






//>	@class	CancelItem
// Button that cancels any changes in the form, by calling +link{DynamicForm.cancelEditing()} 
// when clicked.
// See +link{DynamicForm.cancelEditing()} for details on what happens when a form editing is 
// cancelled.
//
// @visibility external
//<
isc.ClassFactory.defineClass("CancelItem", "ButtonItem");
isc.CancelItem.addProperties({
    //>@attr    CancelItem.title    (String : "Cancel" : IRW)
    // CancelItems show a title of <code>"Cancel"</code> by default. May be overridden.
    // @visibility external
    //<
    title:"Cancel"
});

isc.CancelItem.addMethods({

    handleClick : function () {
        if (this.Super("handleClick", arguments) == false) return false;
        this.form.cancelEditing();
    }
});

