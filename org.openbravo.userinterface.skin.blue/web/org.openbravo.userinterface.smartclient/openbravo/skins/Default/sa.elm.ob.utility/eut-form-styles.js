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
 * All portions are Copyright (C) 2010-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

/* =====================================================================
 * Styling properties for:
 * 1) OB Hijri Date Item
 =======================================================================*/

/* =====================================================================
 * FormItem styling properties
 =======================================================================*/

isc.OBHijriDateChooser.addProperties({
  className: 'OBHijriDateChooser',
  headerStyle: 'OBDateChooserButton',
  weekendHeaderStyle: 'OBDateChooserWeekendButton',
  baseNavButtonStyle: 'OBDateChooserNavButton',
  baseWeekdayStyle: 'OBDateChooserWeekday',
  baseWeekendStyle: 'OBDateChooserWeekend',
  baseBottomButtonStyle: 'OBDateChooserBottomButton',
  disabledWeekdayStyle: "OBDateChooserWeekdayDisabled",
  disabledWeekendStyle: "OBDateChooserWeekendDisabled",

  alternateWeekStyles: false,
  firstDayOfWeek: 1,

  showEdges: true,

  edgeImage: OB.Styles.skinsPath + 'Default/sa.elm.ob.utility/images/form/dateChooser-popup.png',
  edgeSize: 3,
  edgeTop: 26,
  edgeBottom: 3,
  edgeOffsetTop: 1,
  edgeOffsetRight: 3,
  edgeOffsetLeft: 3,
  edgeOffsetBottom: 5,

  todayButtonHeight: 20,

  headerHeight: 24,

  edgeCenterBackgroundColor: '#E5E5E5',
  backgroundColor: null,

  showShadow: false,
  shadowDepth: 6,
  shadowOffset: 5,

  showDoubleYearIcon: false,
  prevYearIcon: OB.Styles.skinsPath + 'Default/sa.elm.ob.utility/images/form/dateChooser-doubleArrow_left.png',
  prevYearIconWidth: 16,
  prevYearIconHeight: 16,
  nextYearIcon: OB.Styles.skinsPath + 'Default/sa.elm.ob.utility/images/form/dateChooser-doubleArrow_right.png',
  nextYearIconWidth: 16,
  nextYearIconHeight: 16,
  prevMonthIcon: OB.Styles.skinsPath + 'Default/sa.elm.ob.utility/images/form/dateChooser-arrow_left.png',
  prevMonthIconWidth: 16,
  prevMonthIconHeight: 16,
  nextMonthIcon: OB.Styles.skinsPath + 'Default/sa.elm.ob.utility/images/form/dateChooser-arrow_right.png',
  nextMonthIconWidth: 16,
  nextMonthIconHeight: 16
});

OB.Styles.OBFormField.DefaultDateInput = {
  cellStyle: 'OBFormField',
  titleStyle: 'OBFormFieldLabel',
  textBoxStyle: 'OBFormFieldDateInput',
  errorOrientation: 'left',

  pickerIconHSpace: '3',

  height: 30,

  pickerIconWidth: 30,
  pickerIconHeight: 30,
  pickerIconSrc: OB.Styles.skinsPath + 'Default/sa.elm.ob.utility/images/form/date_control.png'
};

isc.OBHijriDateItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultDateInput));
isc.OBHijriDateItem.addProperties({
  textAlign: 'left'
});
