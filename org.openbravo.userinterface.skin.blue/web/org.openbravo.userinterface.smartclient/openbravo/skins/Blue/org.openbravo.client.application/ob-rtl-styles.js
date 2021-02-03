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

/* ob-form-styles.js */

isc.OBSectionItemButton.changeDefaults('backgroundDefaults', {
  icon: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/sectionItem-ico-RTL.png'
});


/* ob-navigation-bar-styles.js */

isc.OBQuickLaunch.addProperties({
  createNew_src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/navbar/iconCreateNew-RTL.png',
  quickLaunch_src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/navbar/iconQuickLaunch-RTL.png'
});

isc.OBLogout.addProperties({
  src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/navbar/iconClose-RTL.png'
});


/* ob-personalization-styles.js */

if (OB.Styles.Personalization && OB.Styles.Personalization.Icons) { // To guarantee backward compatibility: OB.Styles.Personalization has been introduced in OB3 MP2
  OB.Styles.Personalization.Icons.fieldGroup = OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/personalization/iconFolder-RTL.png';
}