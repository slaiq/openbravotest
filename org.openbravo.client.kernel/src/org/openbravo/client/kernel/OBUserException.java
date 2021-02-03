/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2008-2010 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OBUserException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private Collection<String> params = new ArrayList<String>();

  public OBUserException() {
    super();
  }

  public OBUserException(String message) {
    super(message);
  }

  public OBUserException(String message, List<String> params) {
    super(message);
    this.params = params;
  }

  public OBUserException(Throwable t) {
    super(t);
  }

  /**
   * Returns a javascript array with the parameters
   * 
   */
  public String getJavaScriptParams() {
    String result = "[";
    boolean firstParam = true;
    for (String param : params) {
      if (!firstParam) {
        result += ", ";
      }
      result += "'" + param + "'";
      firstParam = false;
    }
    result += "]";
    return result;
  }

}
