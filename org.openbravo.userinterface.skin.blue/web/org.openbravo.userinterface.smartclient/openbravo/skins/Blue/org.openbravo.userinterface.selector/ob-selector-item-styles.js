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
 * All portions are Copyright (C) 2011-2013 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

isc.OBSelectorItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultComboBox));

isc.OBSelectorItem.addProperties({
  newTabIconSrc: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/ico-to-new-tab.png',
  popupIconSrc: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/search_picker.png'
});

isc.OBSelectorLinkItem.addProperties({
  newTabIconSrc: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/ico-to-new-tab.png',
  pickerIconSrc: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/search_picker.png'
});

isc.OBSelectorLinkItem.changeDefaults('clearIcon', {
  src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/clearField.png'
});


if (isc.OBMultiSelectorItem) { // To guarantee backward compatibility: isc.OBMultiSelectorItem has been introduced in OB3 MP20
  isc.OBMultiSelectorItem.addProperties({
    popupIconSrc: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/search_picker.png'
  });
  isc.OBMultiSelectorItem.changeDefaults('buttonDefaults', {
    icon: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/clearField.png'
  });
}