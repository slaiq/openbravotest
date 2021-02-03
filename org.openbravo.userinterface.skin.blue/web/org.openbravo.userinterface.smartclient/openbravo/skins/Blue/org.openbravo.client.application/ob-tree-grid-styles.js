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
 * All portions are Copyright (C) 2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

if (OB.Styles.OBTreeGrid) { // To guarantee backward compatibility: OB.Styles.OBTreeGrid has been introduced in OB3 PR14Q2
  OB.Styles.OBTreeGrid = OB.Styles.OBTreeGrid || {};
  OB.Styles.OBTreeGrid.iconFolder = OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/tree-grid/iconFolder.png';
  OB.Styles.OBTreeGrid.iconNode = OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/tree-grid/iconNode.png';

  isc.OBTreeGrid.addProperties({
    headerMenuButtonSrc: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/grid/gridHeaderMenuButton.png'
  });

  isc.OBTreeGrid.changeDefaults('headerButtonDefaults', {
    src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/grid/gridHeader_bg.png'
  });
}