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
 * All portions are Copyright (C) 2012 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package sa.elm.ob.finance.filterexpression;

import java.util.Map;

import org.openbravo.client.application.FilterExpression;

public class BEAccountParent implements FilterExpression {

  @Override
  public String getExpression(Map<String, String> requestMap) {
    String query = "";
    
    query = "e.id in ( select trn.reportSet from ADTreeNode trn join trn.tree tr where tr.typeArea = 'EV')";
   
    return query.toString();
  }

}
