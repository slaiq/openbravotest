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
 * All portions are Copyright (C) 2011 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel.reference;

import org.openbravo.model.ad.ui.Field;

/**
 * Implementation of the encrypted string (password) ui definition. This UIDefinition is used for
 * both encrypted (reversible) and hashed storage of passwords.
 * 
 * @author shuehner
 * @see org.openbravo.base.model.domaintype.HashedStringDomainType
 * @see org.openbravo.base.model.domaintype.EncryptedStringDomainType
 */
public class PasswordUIDefinition extends StringUIDefinition {

  @Override
  public String getFormEditorType() {
    return "OBEncryptedItem";
  }

  @Override
  public String getGridFieldProperties(Field field) {
    return super.getGridFieldProperties(field) + ", canGroupBy: false";
  }

  // disable display of raw-value in grid mode
  @Override
  public String getTypeProperties() {
    return "shortDisplayFormatter: function(value, field, component, record) {"
        + "return new Array((value && value.length > 0 ? value.length+1 : 0)).join(\"*\");" + "},"
        + "normalDisplayFormatter: function(value, field, component, record) {"
        + "return new Array((value && value.length > 0 ? value.length+1 : 0)).join(\"*\");" + "},";
  }

  // disable hover as it would show useless raw-value
  @Override
  protected String getShowHoverGridFieldSettings(Field field) {
    return "";
  }

}
