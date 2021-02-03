/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2011-2012 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

OB.Styles.OBApplicationMenu.Icons.folderOpened = OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/application-menu/iconFolderOpened.png';
OB.Styles.OBApplicationMenu.Icons.folderClosed = OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/application-menu/iconFolderClosed.png';

isc.OBApplicationMenuButton.changeDefaults("nodeIcons", {
  Folder: OB.Styles.OBApplicationMenu.Icons.folderOpened
});
isc.OBApplicationMenuButton.changeDefaults("icon", {
  src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/navbar/iconOpenDropDown.png'
});

// console.log("test it2");
// setTimeout(testFunction, 12000);
// function testFunction() {
//   console.log("test delay");
//   if(document.getElementsByClassName("OBApplicationMenuTreeItemCellTitleField")[3].childNodes[0].childNodes[0].innerHTML == "General Setup"){
//     console.log("done test");
//   }
// }
// document.addEventListener('DOMContentLoaded', function() {
//   if(document.getElementsByClassName("OBApplicationMenuTreeItemCellTitleField")[3].childNodes[0].childNodes[0].innerHTML == "General Setup"){
//     console.log("done test");
//   }
//   else {
//     console.log("not correct");
//   }
// }, false);

// document.onreadystatechange = function(){
//   console.log("test1");
//      if(document.readyState === 'complete'){
//        console.log("test2");
//        if(document.getElementsByClassName("OBApplicationMenuTreeItemCellTitleField")[3].childNodes[0].childNodes[0].innerHTML == "General Setup"){
//          console.log("done test");
//        }
//        else {
//          console.log("not correct");
//        }
//      }
// }
