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
 * All portions are Copyright (C) 2011-2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

/* =====================================================================
 * Styling properties for:
 * 1) OB Form items
 * 2) OBImageItem
 * 2) SectionItem Button Styles
 * 3) Attachments Styles
 =======================================================================*/

/* =====================================================================
 * FormItem styling properties
 =======================================================================*/

if (isc.OBSpinnerItem) { // To guarantee backward compatibility: isc.OBSpinnerItem has been introduced in OB3 MP3
  isc.OBSpinnerItem.INCREASE_ICON = isc.addProperties(isc.OBSpinnerItem.INCREASE_ICON, {
    src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/spinnerControlIncrease.png'
  });
  isc.OBSpinnerItem.DECREASE_ICON = isc.addProperties(isc.OBSpinnerItem.DECREASE_ICON, {
    src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/spinnerControlDecrease.png'
  });
}

OB.Styles.OBFormField.DefaultComboBox.pickerIconSrc = OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/comboBoxPicker.png';

isc.OBListItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultComboBox));

isc.OBFKItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultComboBox));

isc.OBFKItem.addProperties({
  newTabIconSrc: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/ico-to-new-tab.png'
});

isc.OBYesNoItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultComboBox));


OB.Styles.OBFormField.DefaultCheckbox.checkedImage = OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/checked.png';
OB.Styles.OBFormField.DefaultCheckbox.uncheckedImage = OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/unchecked.png';

isc.OBCheckboxItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultCheckbox));


OB.Styles.OBFormField.DefaultSearch.pickerIconSrc = OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/search_picker.png';
OB.Styles.OBFormField.DefaultSearch.newTabIconSrc = OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/ico-to-new-tab.png';
OB.Styles.OBFormField.DefaultSearch.clearIcon.src = OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/clearField.png';

isc.OBSearchItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultSearch));

isc.OBLinkItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultSearch));



OB.Styles.OBFormField.DefaultDateInput.pickerIconSrc = OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/date_control.png';

isc.OBDateItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultDateInput));

isc.OBDateTimeItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultDateInput));

/* =====================================================================
 * OBImageItem
 =======================================================================*/

if (isc.OBImageCanvas) { // To guarantee backward compatibility: isc.OBImageCanvas has been introduced in OB3 MP2
  isc.OBImageCanvas.addProperties({
    imageNotAvailableSrc: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/imageNotAvailable.png'
  });
}

if (isc.OBImageItemButton) { // To guarantee backward compatibility: isc.OBImageItemButton has been introduced in OB3 MP2
  isc.OBImageItemButton.addProperties({
    uploadIconSrc: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/upload_icon.png',
    eraseIconSrc: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/erase_icon.png'
  });
}

/* =====================================================================
 * Date range filter item and dialog
 =======================================================================*/

isc.OBDateRangeDialog.addProperties({
  edgeImage: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/popup/border.png'
});

isc.OBMiniDateRangeItem.changeDefaults('pickerIconDefaults', {
  src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/date_control.png'
});

if (isc.OBRelativeDateItem) { // To guarantee backward compatibility: isc.OBRelativeDateItem has been introduced in OB3 MP32
  isc.OBRelativeDateItem.changeDefaults('valueFieldDefaults', {
    pickerIconSrc: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/comboBoxPicker.png',
    calendarIconSrc: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/date_control.png'
  });
}

/* =====================================================================
 * SectionItem Button Styles
 =======================================================================*/

isc.OBSectionItemButton.changeDefaults('backgroundDefaults', {
  src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/sectionItem-bg.png',
  icon: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/sectionItem-ico.png'
});