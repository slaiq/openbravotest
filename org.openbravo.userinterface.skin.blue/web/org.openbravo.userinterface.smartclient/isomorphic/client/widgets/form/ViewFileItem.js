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

 



//> @groupDef viewFile
//<

//>	@class ViewFileItem
//
// A simple subclass of +link{FileItem} for displaying the contents of "imageFile" fields in 
// DynamicForms. 
// <P>
// Displays one of two UIs, according to the value of 
// +link{FileItem.showFileInline, showFileInline}.  If showFileInline is false, this Item
// displays the View and Download icons and the filename.  Otherwise, it streams the image-file 
// and displays it inline.
//
// @group upload
// @treeLocation Client Reference/Forms/Form Items
// @visibility external
//<
isc.ClassFactory.defineClass("ViewFileItem", "FileItem");

isc.ViewFileItem.addProperties({

    shouldSaveValue: false,
    colSpan: "*",
    height: 20,
    width: "*",
    overflow: "visible",
    
    canEdit: false,
    type: "viewFile",

    isEditable : function () {
        return false;
    },

    formValuesChanged : function () {
        this.setCanvasContent(null);
    }
});
