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


/* OBPopup */

isc.OBPopup.addProperties({
  edgeImage: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/popup/border.png'
});

isc.OBPopup.changeDefaults('restoreButtonDefaults', {
  src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/popup/restore.png'
});

isc.OBPopup.changeDefaults('closeButtonDefaults', {
  src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/popup/close.png'
});

isc.OBPopup.changeDefaults('maximizeButtonDefaults', {
  src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/popup/maximize.png'
});

isc.OBPopup.changeDefaults('minimizeButtonDefaults', {
  src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/popup/minimize.png'
});