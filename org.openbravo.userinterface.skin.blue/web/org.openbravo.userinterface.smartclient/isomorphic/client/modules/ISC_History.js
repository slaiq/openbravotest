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
// This script will load Reference Viewer of the Isomorhic SmartClient Application Framework
// libraries
//
var libs = 
	[
		"language/Packager",
		"browser/Browser",
        "standalone/SA_Core",
        "standalone/SA_Page",
		"browser/History"
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
