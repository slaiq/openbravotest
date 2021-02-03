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
 * All portions are Copyright (C) 2001-2010 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package sa.elm.ob.utility.wad;

import org.openbravo.wad.controls.WADControl;
import org.openbravo.xmlEngine.XmlDocument;

public class WADFieldButton extends WADControl {

  public WADFieldButton(String _buttonName, String _columnName, String _columnNameInp,
      String _name, String _onclick) {
    setData("ButtonName", _buttonName);
    setData("ColumnName", _columnName);
    setData("ColumnNameInp", _columnNameInp);
    setData("Name", _name);
    setData("OnClick", _onclick);
  }

  public String getType() {
    return "FieldButton";
  }

  public String toString() {
    XmlDocument xmlDocument = getReportEngine().readXmlTemplate(
        "sa/elm/ob/utility/wad/WADHijriFieldButton").createXmlDocument();

    xmlDocument.setParameter("columnName", getData("ColumnName"));
    // xmlDocument.setParameter("columnNameInp", getData("ColumnNameInp"));
    xmlDocument.setParameter("name", getData("Name"));
    xmlDocument.setParameter("buttonName", getData("ButtonName"));
    xmlDocument.setParameter("onclick", "getUmmalQura('"+getData("ColumnName")+"')");

    return replaceHTML(xmlDocument.print());
  }
}
