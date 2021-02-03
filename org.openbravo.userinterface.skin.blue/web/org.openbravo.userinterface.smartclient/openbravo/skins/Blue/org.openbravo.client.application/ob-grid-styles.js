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

// Styling properties for a generic grid (ob-grid.js)
isc.OBGrid.addProperties({
  headerMenuButtonSrc: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/grid/gridHeaderMenuButton.png'
});

isc.OBGrid.changeDefaults("progressIconDefaults", {
  src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/grid/gridButton-progress.gif' /* Generated @ http://www.ajaxload.info/ */
  /* Indicator type: 'Snake' - Background color: #FFE1C0 - Transparent background - Foreground color: #333333 */
});

isc.OBGrid.changeDefaults('sorterDefaults', {
  src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/grid/gridHeader_bg.png'
});

isc.OBGrid.changeDefaults('headerButtonDefaults', {
  src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/grid/gridHeader_bg.png'
});

isc.OBGridToolStripSeparator.addProperties({
  src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/grid/gridButton-separator.png'
});

isc.OBViewGrid.addProperties({
  // note should be the same as the height of the OBGridButtonsComponent
  recordComponentHeight: 30,
  cellHeight: 30,
  bodyStyleName: 'OBViewGridBody'
});