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

isc.OBWidgetMenuItem.addProperties({
  src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.myob/images/widget/edit.png',
  // Not needed anymore since OB3 MP6
  menuButtonImage: OB.Styles.skinsPath + 'Blue/org.openbravo.client.myob/images/widget/edit.png' // Introduced in OB3 MP6
});

isc.OBWidget.addProperties({
  edgeImage: OB.Styles.skinsPath + 'Blue/org.openbravo.client.myob/images/widget/window.png'
});

isc.OBWidget.changeDefaults('restoreButtonDefaults', {
  src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.myob/images/widget/restore.png'
});

isc.OBWidget.changeDefaults('closeButtonDefaults', {
  src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.myob/images/widget/close.png'
});

isc.OBWidget.changeDefaults('maximizeButtonDefaults', {
  src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.myob/images/widget/maximize.png'
});

isc.OBWidget.changeDefaults('minimizeButtonDefaults', {
  src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.myob/images/widget/minimize.png'
});

isc.OBWidget.changeDefaults('headerDefaults', {
  layoutMargin: 0,
  height: 60
});

if (isc.OBWidgetInFormItem) { // To guarantee backward compatibility: isc.OBWidgetInFormItem has been introduced in OB3 MP2
  isc.OBWidgetInFormItem.changeDefaults("widgetProperties", {
    edgeImage: OB.Styles.skinsPath + 'Blue/org.openbravo.client.myob/images/form/border.png'
  });
}