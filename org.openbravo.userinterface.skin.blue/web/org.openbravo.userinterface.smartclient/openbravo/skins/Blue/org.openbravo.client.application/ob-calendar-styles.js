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
 * All portions are Copyright (C) 2013-2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

if (isc.OBCalendar) { // To guarantee backward compatibility: isc.OBCalendar has been introduced in OB3 MP19
  isc.OBCalendar.changeDefaults('datePickerButtonDefaults', {
    src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/form/date_control.png'
  });

  isc.OBCalendar.changeDefaults('previousButtonDefaults', {
    src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/statusbar/iconButton-previous.png'
  });

  isc.OBCalendar.changeDefaults('nextButtonDefaults', {
    src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/statusbar/iconButton-next.png'
  });

  isc.OBCalendar.changeDefaults('addEventButtonDefaults', {
    src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/statusbar/iconButton-add.png'
  });

  isc.OBCalendar.changeDefaults('dayLanesToggleButtonDefaults', {
    src: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/calendar/iconDayLanesToggle.png'
  });
}

if (isc.OBCalendarTabSet) { // To guarantee backward compatibility: isc.OBCalendarTabSet has been introduced in OB3 MP19
  isc.OBCalendarTabSet.addProperties({
    scrollerSrc: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/tab/tabBarButtonMain_OverflowIcon.png',
    pickerButtonSrc: OB.Styles.skinsPath + 'Blue/org.openbravo.client.application/images/tab/tabBarButtonMain_OverflowIconPicker.png'
  });
}