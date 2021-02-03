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



//
// This script will load all of the Isomorhic SmartClient Application Framework libraries for you
//
// The idea is that in your app file you can just load the script "Isomorphic_SmartClient.js" which
// in a production situation would be all of the scripts jammed together into a single file.
//
// However, it's easier to work on the scripts as individual files, this file will load all of the
// scripts individually for you (with a speed penalty).
//		
var libs = 
	[

		//>DEBUG
		"language/Packager",			// packager, only used for making sure you have everything you need
		//<DEBUG

		"browser/Browser",				// browser detection and bail if not supported
		
		// core library stuff -- everybody needs all of these
 		// packages assume that all of this is already loaded
		"language/Object",				// core object extensions
		"language/IsA",					// provides identity and typing services
		"language/ClassFactory",		// creates classes for you
		"language/Class",				// base of our class system
		
		// language stuff -- used by many sub-systems
		"language/Function",			// extensions to the native Function object
		"language/Array",				// extensions to the array object -- used heavily

		"language/NumberUtil",	        // Number-related utilities
		"language/Number",				// extensions to the native Number object
		"language/Math",				// Math helpers
		"language/Date",				// extensions to the native Date object / DateUtil class
		"language/RelativeDate",        // APIs for working with relative date values
		"language/String",				// extensions to the native String object
		"language/StringBuffer",		// provides an efficient way of concatenating strings
        "language/StringMethod",        // provides xmlSerialize method for expressions/functions

        "language/URIBuilder",

		"browser/Cookie",				// processing browser cookies - used during loading of ISC (by Log.js at least)

		//>DEBUG
        "debug/StackTrace",             // transform native stack traces into more readable traces
		"debug/debug",					// debug utilities and stack walking
        //<DEBUG

        

		"debug/Log",					// log package (NOTE: contains stubs necessary in production build)

		//>DEBUG
        //"debug/Debugger",             // debugger package (with the exception of
                                        // getStackTrace / getCallTrace)
		//<DEBUG

		// optional language stuff -- use only if needed by your app
		"language/Array_sort",			// sort arrays of objects easily
		"language/Array_math",			// math operations on arrays
		//"language/Reflection",		// provides for reflection or inspection of any Class or instance
		//"language/Array_util",		// utility array methods, not commonly used
		//"language/List",			    // equivalent functionality to a native Array, as a isc.Class
		"language/Map",					// map of name->value pairs
		"language/Time",				// time object, including parsing rules
		//"language/Tree",				// generic isc.Tree implementation
		//"language/Tree_util",			// additional, not commonly used Tree routines
		//"language/ObjectTree",		// wrapper so you can treat an arbitrary object as a tree
        "language/SGWTFactory",        // Enables reflection in SGWT

        

		"browser/Page",					// characteristics of the browser window
		"browser/Params",				// OPTIONAL: processing URL parameters
		//"browser/UI",					// misc. UI helper functions
	

		// client-server communications
		"communications/Comm",			// simple client-server communication channel and protocols
		"communications/HiddenFrame",	// hidden frame for doing 'invisible' c-s communications
    
		// event handling
		"event/Timer",						// consolidated timing functions
		"event/EventRegistry",			// global event trapping mechanism
		"event/EventHandler",			// cross-browser event handling framework

		//"language/Selection",			// provides a selection of a list, including selecting based
                                        // on mouse events
        
		
		// drawable, positionable elements
        "widgets/Element",              // helper methods for DOM element manipulation
		"widgets/Canvas",				// base class of all widgets, very extensive
        // printing
        "widgets/PrintCanvas",
        
        
        "application/DataBoundComponent", // DataBoundComponent interface APIs applied to the Canvas class
        
        //>RoundCorners
		"widgets/EdgedCanvas",				// base class of all widgets, very extensive
        //<RoundCorners

		"widgets/Hover",				// singleton that manages hover (e.g. tooltip) timing and window

		"language/Serialize",			// serialize an object as a js literal so it can be re-instantiated
		"language/Clone",				// make a isc.clone (duplicate) of an object

        "tools/AutoTest",               // Module for simplified integration with automated
                                        // testing tools

        
       
		"debug/DoneLoading"				// code to be executed when the libraries are done loading
	];

//<STOP PARSING 

// The following code only executes if the script is being dynamically loaded.

// the following statement allows a page that is not in the standard location to take advantage of
// dynamically loaded scripts by explicitly setting the window.isomorphiDir variable itself.
if (! window.isomorphicDir) window.isomorphicDir = "../isomorphic/";

// dynamic loading
(function () {
    function loadLib(lib) {
        document.write("<"+"script src='" + window.isomorphicDir + "client/" + lib + ".js' type='text/javascript' charset='UTF-8'><"+"/script>");
    }

    // load Packager.js to define the `isc' global
    loadLib("language/Packager");
    if (libs[0] == "language/Packager") libs[0] = null;

    document.write("<"+"script type='text/javascript'>isc.definingFramework = true;<"+"/script>");
    for (var i = 0, l = libs.length; i < l; ++i) {
        if (!libs[i]) continue;
        if (window.UNSUPPORTED_BROWSER_DETECTED == true) break;
        loadLib(libs[i]);
    }
    document.write("<"+"script type='text/javascript'>delete isc.definingFramework;<"+"/script>");
})();
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



//
// This script will load all of the Isomorhic SmartClient Application Framework libraries for you
//
// The idea is that in your app file you can just load the script "Isomorphic_SmartClient.js" which
// in a production situation would be all of the scripts jammed together into a single file.
//
// However, it's easier to work on the scripts as individual files, this file will load all of the
// scripts individually for you (with a speed penalty).
//		
var libs = 
	[
        "debug/version",  // check for module version mismatches

        //>Animation
		"widgets/Animation",	// Animation subsystem
        //<Animation

		"widgets/StatefulCanvas",		// minor variant on Canvas allowing for statefulness.

		"widgets/Layout",				// automatically arranges its children
		//"widgets/DetailViewer",			// show attributes of one or more objects as a vertical table
		"widgets/Button",				// button with special up, down, disabled, etc. look
		"widgets/Img",					// positionable image

//		"widgets/ButtonTable",			// table of cheapie buttons (very low resource use) -- in Nav, they look like links
//		"widgets/DateChooser",			// a date picker

// evaluation tracker image loader


		"widgets/StretchImg",			// composite image, composed of many individual images
//        "widgets/Slider",             	// graphical slider widget (uses isc.Img and isc.StretchImg)

		"widgets/Label",				// moveable, changeable bit of text
        //>Progressbar
		"widgets/Progressbar",			// stretch image for showing progress of lengthy operations
        //<Progressbar
        //>Rangebar
        "widgets/Rangebar",            	// graphical rangebar widget
        //<Rangebar
		"widgets/Toolbar",				// collection of buttons
		//"widgets/Border",				// platform-independent border
		"widgets/ImgButton",			// image with button behaviors
		"widgets/StretchImgButton",		// stretch image with button behaviors			
		//"widgets/ImgTab",				// stretch image with tab behaviors
		//"widgets/TabBar",				// collection of tabs
		
		"widgets/ToolStrip",			// a narrow strip with a mixed set of controls
		"widgets/ToolStripGroup",		// a "panel" for grouping controls in a toolstrip

        //>SectionStack
        "widgets/SectionStack",         // container similar to Outlook left-hand Nav (subclass of Layout, uses Label)
        //<SectionStack

		"widgets/Scrollbar",			// horizontal and vertical scroll bars
		"widgets/NativeScrollbar",      // horizontal and vertical scrollbars based on native CSS scrollbars
        
		//"widgets/GridRenderer",			// high speed, flexible, feature-rich table
		//"widgets/ListGrid",			// multi-column viewer for a list of objects
        //"widgets/TreeGrid",			// viewer for a tree of objects
        
        //"widgets/RecordEditor",         // specialized listViewer for editing a single record

        "widgets/Splitbar",              // default resizer for layouts

		//"widgets/Finder",				// specialized tree viewer that resembles the Macintosh isc.Finder
		//"widgets/Explorer",			// specialized tree viewer that resembles the left part of a Windows Explorer
		//"widgets/ExplorerList",		// specialized tree viewer that resembles the right part of a Windows Explorer

        //"widgets/ScrollingMenu",        // specialized listViewer with menu type event-handling 
                                        // behaviour, but scrollable and ready for data-binding
        		
		//"widgets/Menu",					// pull-down or context menus
		//"widgets/MenuButton",			// button that shows a menu on click
		//"widgets/Menubar",				// set of menus shown as a menubar
		//"widgets/Window",				// window class
		//"widgets/Dialog",				// movable, modal dialog

        

		"widgets/StretchResizePolicy",	// code to resize a set of elements in a single dimension
		//"widgets/TableResizePolicy",	// code to resize a set of elements in two dimensions
		//"widgets/Hover",				// singleton that manages hover (e.g. tooltip) timing and window
		//"widgets/TabSet", 				// composite of TabBar and tab panes

		//"widgets/form/DynamicForm",		// dynamically redrawable form
		//"widgets/form/FormItem",		// abstract sub-item of a form
		//"widgets/form/FormItemFactory",	// singleton object that creates FormItems from object literals
		//"widgets/form/Validators",		// validators for form fields
		//"widgets/form/ContainerItem",	// abstract form item that can contain other formItems
		
		//"widgets/form/TextItem",		// single-line text field
		//"widgets/form/BlurbItem",		// static text display
		//"widgets/form/ButtonItem",		// button form item
		//"widgets/form/SelectItem",		// select item -- drop-down list
		//"widgets/form/CheckboxItem",	// checkbox item
		//"widgets/form/HeaderItem",		// section header
		//"widgets/form/SectionItem",		// section header for group that shows/hides group
		//"widgets/form/HiddenItem",		// hidden field
		//"widgets/form/StaticTextItem",	// static text (label)
		//"widgets/form/PasswordItem",	// password-entry field (masked characters)        
		//"widgets/form/RadioGroupItem",	// set of radio buttons acting as a group
		//"widgets/form/RadioItem",		// single radio button
		//"widgets/form/ResetItem",		// reset button
		//"widgets/form/DateItem",		// multi-part Date editor
		//"widgets/form/SpacerItem",		// spacer
		//"widgets/form/RowSpacerItem",	// separator
		//"widgets/form/SubmitItem",		// submit button
        //"widgets/form/CancelItem",        // cancel button
		//"widgets/form/TextAreaItem",	// multi-line text field
		//"widgets/form/TimeItem",		// edit a isc.Time value
		//"widgets/form/ToolbarItem",		// collection of form buttons
		//"widgets/form/UploadItem",		// file-upload widget
		//"widgets/form/ComboBoxItem",	// combobox (text field + button + filtered listViewer)
		
		//"widgets/Editor",				// editor Interface and a couple of implementations for editing listViewer cells
		//"widgets/form/SearchForm",		// simple subclass of dynamicForm to be used in filters and 
                                        // search forms in applications

		//"widgets/form/AdvancedFilter",	// advanced search form that allows the user to specify
                                        // individual fields and operators

        //>ValuesManager
        //"widgets/form/ValuesManager",   // values manager for values from multiple member forms
        //<ValuesManager

		//"language/Dictionary",		// message dictionary class
		
		//"application/DataSource",		// representation of a server data source (databse table, etc)
		//"application/RPCManager",	// framework for editing/interacting with datasources
		//"application/ResultSet",        // data model for Lists loaded incrementally from a server
		//"application/ResultTree",       // data model for Trees loaded incrementally from a server
		//"application/ActionMethods",      // flow methods for databinding-capable components

		//"widgets/EditMode",             // support for an editing mode and editing container

        //"widgets/RecordScrollbar",      
        //"widgets/MultiRecordForm",      // use a scrollbar to page through a ResultSet of records,
                                        // showing the records in a form

		//"widgets/MultiView",            // presents multiple views of a datasource and standard
                                        // actions
//        "widgets/DataPrefetch",         // interface for parallel init/draw/data load
        

        //>SimpleType
        "language/SimpleType",          // loads map of built-in types and their validators, SimpleType
        //<SimpleType
        
        "widgets/NavigationBar",        // iPhone/iPad -like navigationBar
        "widgets/SplitPane"             // manages a two-pane layout according to hardware type
                                        // and orientation
                                        
	];

//<STOP PARSING 

// The following code only executes if the script is being dynamically loaded.

// the following statement allows a page that is not in the standard location to take advantage of
// dynamically loaded scripts by explicitly setting the window.isomorphiDir variable itself.
if (! window.isomorphicDir) window.isomorphicDir = "../isomorphic/";

// dynamic loading
(function () {
    function loadLib(lib) {
        document.write("<"+"script src='" + window.isomorphicDir + "client/" + lib + ".js' type='text/javascript' charset='UTF-8'><"+"/script>");
    }

    document.write("<"+"script type='text/javascript'>isc.definingFramework = true;<"+"/script>");
    for (var i = 0, l = libs.length; i < l; ++i) {
        if (!libs[i]) continue;
        if (window.UNSUPPORTED_BROWSER_DETECTED == true) break;
        loadLib(libs[i]);
    }
    document.write("<"+"script type='text/javascript'>delete isc.definingFramework;<"+"/script>");
})();
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



//
// This script will load all of the Isomorhic SmartClient Application Framework libraries for you
//
// The idea is that in your app file you can just load the script "Isomorphic_SmartClient.js" which
// in a production situation would be all of the scripts jammed together into a single file.
//
// However, it's easier to work on the scripts as individual files, this file will load all of the
// scripts individually for you (with a speed penalty).
//		
var libs = 
	[
        "debug/version",  // check for module version mismatches

		"widgets/ImgTab",				// stretch image with tab behaviors
		"widgets/TabBar",				// collection of tabs
		
		"widgets/Window",				// window class
        "widgets/Portal",               // Set of widgets for portals
		"widgets/Dialog",				// movable, modal dialog
        "application/MultiSortDialog",
		"widgets/TabSet" 				// composite of TabBar and tab panes
	];

//<STOP PARSING 

// The following code only executes if the script is being dynamically loaded.

// the following statement allows a page that is not in the standard location to take advantage of
// dynamically loaded scripts by explicitly setting the window.isomorphiDir variable itself.
if (! window.isomorphicDir) window.isomorphicDir = "../isomorphic/";

// dynamic loading
(function () {
    function loadLib(lib) {
        document.write("<"+"script src='" + window.isomorphicDir + "client/" + lib + ".js' type='text/javascript' charset='UTF-8'><"+"/script>");
    }

    document.write("<"+"script type='text/javascript'>isc.definingFramework = true;<"+"/script>");
    for (var i = 0, l = libs.length; i < l; ++i) {
        if (!libs[i]) continue;
        if (window.UNSUPPORTED_BROWSER_DETECTED == true) break;
        loadLib(libs[i]);
    }
    document.write("<"+"script type='text/javascript'>delete isc.definingFramework;<"+"/script>");
})();
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



//
// This script will load all of the Isomorhic SmartClient Application Framework libraries for you
//
// The idea is that in your app file you can just load the script "Isomorphic_SmartClient.js" which
// in a production situation would be all of the scripts jammed together into a single file.
//
// However, it's easier to work on the scripts as individual files, this file will load all of the
// scripts individually for you (with a speed penalty).
//		
var libs = 
	[
        "debug/version",  // check for module version mismatches

		//"language/Array_util",		// utility array methods, not commonly used
		"language/List",			    // equivalent functionality to a native Array, as a isc.Class
		"language/Tree",				// generic isc.Tree implementation
		//"language/Tree_util",			// additional, not commonly used Tree routines
		//"language/ObjectTree",		// wrapper so you can treat an arbitrary object as a tree

		"language/Selection",			// provides a selection of a list, including selecting based
                                        // on mouse events
        //>DetailViewer
		"widgets/DetailViewer",			// show attributes of one or more objects as a vertical table
        //<DetailViewer

//		"widgets/Toolbar",				// collection of buttons

		"widgets/GridRenderer",			// high speed, flexible, feature-rich table
		"widgets/ListGrid",			// multi-column viewer for a list of objects
        "widgets/TreeGrid",			// viewer for a tree of objects
        "widgets/GridToolStrip",
        "widgets/FieldPicker",          // allows DBC field selection
                
        

        "widgets/RecordEditor",         // specialized listViewer for editing a single record

		//"widgets/Finder",				// specialized tree viewer that resembles the Macintosh isc.Finder
		//"widgets/Explorer",			// specialized tree viewer that resembles the left part of a Windows Explorer
		//"widgets/ExplorerList",		// specialized tree viewer that resembles the right part of a Windows Explorer

        //"widgets/ScrollingMenu",        // specialized listViewer with menu type event-handling 
                                        // behaviour, but scrollable and ready for data-binding

        //>Menu        		
		"widgets/Menu",					// pull-down or context menus
        //<Menu
        //>MenuButton
		"widgets/MenuButton",			// button that shows a menu on click
        //<MenuButton

        //>TreeMenuButton
        "widgets/TreeMenuButton",        // Button/Menu with hierachichal, selectable data
        //<TreeMenuButton

        
        "widgets/TileLayout",
        "widgets/TileGrid",               // displays a tiled list of items
        

        //>ColumnTree
        "widgets/ColumnTree",             // displays a tree structure as Miller Columns, like iTunes
        //<ColumnTree
        
        //>TableView
        "widgets/TableView",             // displays an iPhone-style table for selection
        //<TableView

        //>DOMGrids
        "language/DOMTree",             // Tree model that understands DOMs (XML and HTML)
        "widgets/DOMGrid",              // TreeGrid subclass specialized to show DOMs
        //<DOMGrids
    
        //>Menubar
		"widgets/Menubar",				// set of menus shown as a menubar
        //<Menubar
		"language/CellSelection",		// provides a selection of a grid, including selecting based 
                                        // on mouse events
        "widgets/FieldEditor",
        
        "widgets/FormulaBuilder",

        "widgets/HiliteEditor",

        //"widgets/ReportChooser",
        "application/MultiGroupDialog"
	];

//<STOP PARSING 

// The following code only executes if the script is being dynamically loaded.

// the following statement allows a page that is not in the standard location to take advantage of
// dynamically loaded scripts by explicitly setting the window.isomorphiDir variable itself.
if (! window.isomorphicDir) window.isomorphicDir = "../isomorphic/";

// dynamic loading
(function () {
    function loadLib(lib) {
        document.write("<"+"script src='" + window.isomorphicDir + "client/" + lib + ".js' type='text/javascript' charset='UTF-8'><"+"/script>");
    }

    document.write("<"+"script type='text/javascript'>isc.definingFramework = true;<"+"/script>");
    for (var i = 0, l = libs.length; i < l; ++i) {
        if (!libs[i]) continue;
        if (window.UNSUPPORTED_BROWSER_DETECTED == true) break;
        loadLib(libs[i]);
    }
    document.write("<"+"script type='text/javascript'>delete isc.definingFramework;<"+"/script>");
})();
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



//
// This script will load all of the Isomorhic SmartClient Application Framework libraries for you
//
// The idea is that in your app file you can just load the script "Isomorphic_SmartClient.js" which
// in a production situation would be all of the scripts jammed together into a single file.
//
// However, it's easier to work on the scripts as individual files, this file will load all of the
// scripts individually for you (with a speed penalty).
//		
var libs = 
	[
        "debug/version",  // check for module version mismatches

		"widgets/TableResizePolicy",	// code to resize a set of elements in two dimensions
		"widgets/ButtonTable",			// table of cheapie buttons (very low resource use) -- in Nav, they look like links
		"widgets/DateGrid",			    // grid-based calendar-portion for DateChooser
		"widgets/DateChooser",			// a date picker

        "widgets/Slider",             	// graphical slider widget (uses isc.Img and isc.StretchImg)
        "widgets/RangeSlider",		// range slider

        "widgets/ScrollingMenu",        // specialized listViewer with menu type event-handling 
                                        // behaviour, but scrollable and ready for data-binding
        		
		"widgets/form/DynamicForm",		// dynamically redrawable form
		"widgets/form/FormItem",		// abstract sub-item of a form
		"widgets/form/FormItemFactory",	// singleton object that creates FormItems from object literals
		"widgets/form/Validators",		// validators for form fields
		"widgets/form/ContainerItem",	// abstract form item that can contain other formItems
		
		"widgets/form/CanvasItem",		// FormItem that contains a Canvas

		"widgets/form/TextItem",		// single-line text field
        "widgets/form/IntegerItem",     // single-line text field to display an integer value
        "widgets/form/FloatItem",       // single-line text field to display a floating point value
        "widgets/form/DoubleItem",      // single-line text field to display a double value
		"widgets/form/BlurbItem",		// static text display
		"widgets/form/ButtonItem",		// button form item

        "widgets/form/PickList",        // Pick-List for use in select
		"widgets/form/NativeSelectItem",// select item rendered using native select element
		"widgets/form/SelectItem",		// select item -- drop-down list
        
		"widgets/form/CycleItem",	// item for moving through a valueMap via single clicks
		"widgets/form/CheckboxItem",	// checkbox item
        "widgets/form/NativeCheckboxItem", // checkbox item rendered using native checkbox element

		"widgets/form/HeaderItem",		// section header
		"widgets/form/SectionItem",		// section header for group that shows/hides group
		"widgets/form/HiddenItem",		// hidden field
		"widgets/form/StaticTextItem",	// static text (label)
		"widgets/form/LinkItem",	    // HTML link
		"widgets/form/PasswordItem",	// password-entry field (masked characters)        
		"widgets/form/RadioGroupItem",	// set of radio buttons acting as a group
		"widgets/form/RadioItem",		// single radio button
		"widgets/form/ResetItem",		// reset button

		"widgets/form/DateItem",		// multi-part Date editor
        "widgets/form/DateTimeItem",    // modified subclass of DateItem for editing datetimes
		"widgets/form/SpacerItem",		// spacer
		"widgets/form/RowSpacerItem",	// separator
		"widgets/form/SubmitItem",		// submit button
		"widgets/form/CancelItem",      // cancel button
		"widgets/form/TextAreaItem",	// multi-line text field
        
        "widgets/form/AutoFitTextAreaItem",    // Text area item which expands to fit its content
        
		"widgets/form/TimeItem",		// edit a isc.Time value
		"widgets/form/ToolbarItem",		// collection of form buttons
		"widgets/form/UploadItem",		// file-upload widget

		"widgets/form/ComboBoxItem",	// combobox (text field + button + filtered listViewer)
		"widgets/form/MultiComboBoxItem",  // multiple selection combobox (combobox + buttons)

        "widgets/form/FileItem",        // fileitem based on canvasItem that creates a new form with
                                        // uploadItem                                        
        // INFA		
		"widgets/form/RelationItem",	// canvasItem-based relation item
        
        "widgets/form/MultiFileItem",	// relationItem-based multi-upload widget

        "widgets/form/DialogUploadItem",
        "widgets/form/SOAPUploadItem",
        // INFA

        "widgets/form/SpinnerItem",      // Form item for Number type data - includes icons to
                                         // increase / decrease the values.
        "widgets/form/SliderItem",      // Form item that containing a slider to manage values

        "widgets/form/ColorItem",      // Form item used to modify colors

		"widgets/form/ValueMapItem",    // Form item used to modify valueMaps, tools only

        "widgets/form/PickTreeItem",    // This form item shows a tree-menu so you can pick from a hierachy of choices

		"widgets/form/PopUpTextAreaItem",	// Shows a floating textArea.  Can be used in ListGrid editing.
        "widgets/form/ExpressionItem",  // string methods or functions
        

        //"widgets/Editor",				// deprecated old-style inline editing: Editor
                                        // Interface and a couple of implementations

		"widgets/form/SearchForm",       // simple subclass of dynamicForm to be used in filters and 
                                        // search forms in applications

        //>ValuesManager
        "widgets/form/ValuesManager",   // values manager for values from multiple member forms
        //<ValuesManager

    	
        "widgets/ColorPicker",     // Helper for picking colors - supersedes the old ColorChooser

        "widgets/form/NestedEditorItem",     // Item for auto-editing a single complex sub-object
        "widgets/form/NestedListEditorItem", // Item for auto-editing a list of complex sub-objects

        "widgets/form/ViewFileItem", // Item for showing the download/view UI for binary/imageFile fields

                
		
		"widgets/Panel",
        
        "widgets/form/DataPathItem",     // Item for managing a dataPath

        "widgets/form/RelativeDateItem",    // Item for managing relative dates
        "widgets/form/DateRangeItem",     // Item for managing a pair of Date or RelativeDateItems
                                         //  also includes isc.DateRangeDialog
		"widgets/EntityEditor",  // widget for auto-editing an entire Entity structure from the DB 
		//"widgets/form/ReportChooserItem",  // Item that allows stored formatting to be applied 
                                                // to LGs - also shells a widget for creating/editing
                                                // the format information
        "widgets/form/PresetCriteriaItem"     // Item for managing a preset-criteria with
                                         //  readable titles

	];

//<STOP PARSING 

// The following code only executes if the script is being dynamically loaded.

// the following statement allows a page that is not in the standard location to take advantage of
// dynamically loaded scripts by explicitly setting the window.isomorphiDir variable itself.
if (! window.isomorphicDir) window.isomorphicDir = "../isomorphic/";

// dynamic loading
(function () {
    function loadLib(lib) {
        document.write("<"+"script src='" + window.isomorphicDir + "client/" + lib + ".js' type='text/javascript' charset='UTF-8'><"+"/script>");
    }

    document.write("<"+"script type='text/javascript'>isc.definingFramework = true;<"+"/script>");
    for (var i = 0, l = libs.length; i < l; ++i) {
        if (!libs[i]) continue;
        if (window.UNSUPPORTED_BROWSER_DETECTED == true) break;
        loadLib(libs[i]);
    }
    document.write("<"+"script type='text/javascript'>delete isc.definingFramework;<"+"/script>");
})();
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



//
// This script will load all of the Isomorhic SmartClient Application Framework libraries for you
//
// The idea is that in your app file you can just load the script "Isomorphic_SmartClient.js" which
// in a production situation would be all of the scripts jammed together into a single file.
//
// However, it's easier to work on the scripts as individual files, this file will load all of the
// scripts individually for you (with a speed penalty).
//		
var libs = 
	[
        "debug/version",  // check for module version mismatches

        "language/TextSettings",        // Settings related to ListGrid/DataSource text import/export
        "language/TextExportSettings",
        "language/TextImportSettings",

		"language/XMLSerialize",		// serialize an object as an xml string

        "language/XMLTools",

		"application/DataSource",		// representation of a server data source (databse table, etc)
        "application/WebService",       // WebService / WSDL 
		"application/RPCManager",	    // framework for editing/interacting with datasources
		"application/DMI",	            // Direct Method Invocation
		"application/ResultSet",        // data model for Lists loaded incrementally from a server
		"application/ResultTree",       // data model for Trees loaded incrementally from a server
		"application/ActionMethods",      // flow methods for databinding-capable components
        
        "application/MockDataSource",   // a client-only DataSource supporting data expressed
                                        // in a wiki-like text format

        "application/DataView",         // Self-contained application element, capable of loading 
                                        // its own config, components and data from webservices
        "application/ServiceOperation", // A webservice operation
        
        "application/Offline",          // Offline support

        
        
        "application/RulesEngine",      // Support for validation rules across multiple databound components
        
		"widgets/EditMode",             // support for an editing mode and editing container
        "widgets/editMode/EditProxy",
        "widgets/editMode/FormEditProxy",
        "widgets/editMode/GridEditProxy",
        "widgets/editMode/PortalEditProxy",
        "widgets/editMode/DrawingEditProxy",
		"widgets/PropertySheet",        // specialized, compact form

        "widgets/ListEditor",           // combination grid and form for editing a list of
                                        // records

		"widgets/ViewLoader",	    // manages components dynamically loaded from server
		"widgets/HTMLFlow",	        // a block of free-flowing HTML, with dynaload facilities

        "application/FacadeDataSource",
        "application/WSDataSource", // DataSource that works through ISC Web Service
        "application/RestDataSource", // Generic DataSource for arbitrary web servers (PHP / etc)

        // load schema needed to perform client-side XML to JS just for WSDL/XMLSchema
        // definitions produced by the schemaTranslator
//        "schema/DataSource.ds.xml",
//        "schema/DataSourceField.ds.xml",
//        "schema/Validator.ds.xml",
//        "schema/SimpleType.ds.xml",
//        "schema/XSComplexType.ds.xml",
//        "schema/XSElement.ds.xml",
//        "schema/SchemaSet.ds.xml",
//        "schema/WSDLMessage.ds.xml",
//        "schema/WebService.ds.xml",
//        "schema/WebServiceOperation.ds.xml",
//        "schema/WSOperationHeader.ds.xml",


        "application/Operators",        // i18n naming object for AdvancedCriteria operators
		"widgets/form/FilterBuilder",	// advanced search form that allows the user to specify
                                        // individual fields and operators
        "widgets/MockupElement",       // placeholder for non-translatable widgets used by Reify
        "widgets/RuleEditor",          // widget for editing rules
        
                                        
        //>S3
        //"application/S3",
        //<S3
        
        "widgets/ScreenReader",
        
        "widgets/DataSourceEditor"
        
	];

//<STOP PARSING 

// The following code only executes if the script is being dynamically loaded.

// the following statement allows a page that is not in the standard location to take advantage of
// dynamically loaded scripts by explicitly setting the window.isomorphiDir variable itself.
if (! window.isomorphicDir) window.isomorphicDir = "../isomorphic/";

// dynamic loading
(function () {
    function loadLib(lib) {
        document.write("<"+"script src='" + window.isomorphicDir + "client/" + lib + ".js' type='text/javascript' charset='UTF-8'><"+"/script>");
    }

    document.write("<"+"script type='text/javascript'>isc.definingFramework = true;<"+"/script>");
    for (var i = 0, l = libs.length; i < l; ++i) {
        if (!libs[i]) continue;
        if (window.UNSUPPORTED_BROWSER_DETECTED == true) break;
        loadLib(libs[i]);
    }
    document.write("<"+"script type='text/javascript'>delete isc.definingFramework;<"+"/script>");
})();
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



var libs = 
	[
        "debug/version",  // check for module version mismatches

        "widgets/CalendarView",
        "widgets/Calendar",
        "widgets/Timeline"
	];

//<STOP PARSING 

// The following code only executes if the script is being dynamically loaded.

// the following statement allows a page that is not in the standard location to take advantage of
// dynamically loaded scripts by explicitly setting the window.isomorphiDir variable itself.
if (! window.isomorphicDir) window.isomorphicDir = "../isomorphicSDK/smartclient/";

// dynamic loading
(function () {
    function loadLib(lib) {
        document.write("<"+"script src='" + window.isomorphicDir + "client/" + lib + ".js' type='text/javascript' charset='UTF-8'><"+"/script>");
    }

    document.write("<"+"script type='text/javascript'>isc.definingFramework = true;<"+"/script>");
    for (var i = 0, l = libs.length; i < l; ++i) {
        if (!libs[i]) continue;
        if (window.UNSUPPORTED_BROWSER_DETECTED == true) break;
        loadLib(libs[i]);
    }
    document.write("<"+"script type='text/javascript'>delete isc.definingFramework;<"+"/script>");
})();
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



//
// This script will load all of the Isomorhic SmartClient Application Framework libraries for you
//
// The idea is that in your app file you can just load the script "Isomorphic_SmartClient.js" which
// in a production situation would be all of the scripts jammed together into a single file.
//
// However, it's easier to work on the scripts as individual files, this file will load all of the
// scripts individually for you (with a speed penalty).
//		
var libs = 
	[
        "debug/version",  // check for module version mismatches

        //>BrowserPlugin
		"widgets/BrowserPlugin",		// generic browser plugin management
        //>Visualization
        //>Applet
		"widgets/Applet",		        // applet support
        //<Applet
        //>Flash
        "widgets/Flashlet",             // flash support
        //<Flash
        //>SVG
		"widgets/SVG",					// svg plugin
        //<SVG
        //>ActiveX
        "widgets/ActiveXControl"       // ActiveX support
        //<ActiveX
        //<Visualization
        //<BrowserPlugin
	];

//<STOP PARSING 

// The following code only executes if the script is being dynamically loaded.

// the following statement allows a page that is not in the standard location to take advantage of
// dynamically loaded scripts by explicitly setting the window.isomorphiDir variable itself.
if (! window.isomorphicDir) window.isomorphicDir = "../isomorphic/";

// dynamic loading
(function () {
    function loadLib(lib) {
        document.write("<"+"script src='" + window.isomorphicDir + "client/" + lib + ".js' type='text/javascript' charset='UTF-8'><"+"/script>");
    }

    document.write("<"+"script type='text/javascript'>isc.definingFramework = true;<"+"/script>");
    for (var i = 0, l = libs.length; i < l; ++i) {
        if (!libs[i]) continue;
        if (window.UNSUPPORTED_BROWSER_DETECTED == true) break;
        loadLib(libs[i]);
    }
    document.write("<"+"script type='text/javascript'>delete isc.definingFramework;<"+"/script>");
})();
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



//
// This script will load all of the Isomorhic SmartClient Application Framework libraries for you
//
// The idea is that in your app file you can just load the script "Isomorphic_SmartClient.js" which
// in a production situation would be all of the scripts jammed together into a single file.
//
// However, it's easier to work on the scripts as individual files, this file will load all of the
// scripts individually for you (with a speed penalty).
//		
var libs = 
	[
        "debug/version",  // check for module version mismatches

        "widgets/ListPropertiesDialog",
        "widgets/RichTextCanvas",   // Rich Text Editing area
        "widgets/RichTextEditor",   // Rich Text Editing ui component
		"widgets/form/RichTextItem"    // RichTextEditor in a form item.
        
	];

//<STOP PARSING 

// The following code only executes if the script is being dynamically loaded.

// the following statement allows a page that is not in the standard location to take advantage of
// dynamically loaded scripts by explicitly setting the window.isomorphiDir variable itself.
if (! window.isomorphicDir) window.isomorphicDir = "../isomorphic/";

// dynamic loading
(function () {
    function loadLib(lib) {
        document.write("<"+"script src='" + window.isomorphicDir + "client/" + lib + ".js' type='text/javascript' charset='UTF-8'><"+"/script>");
    }

    document.write("<"+"script type='text/javascript'>isc.definingFramework = true;<"+"/script>");
    for (var i = 0, l = libs.length; i < l; ++i) {
        if (!libs[i]) continue;
        if (window.UNSUPPORTED_BROWSER_DETECTED == true) break;
        loadLib(libs[i]);
    }
    document.write("<"+"script type='text/javascript'>delete isc.definingFramework;<"+"/script>");
})();
