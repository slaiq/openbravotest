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

import org.apache.commons.lang.StringUtils;
import org.openbravo.client.application.FilterExpression;

import jxl.common.Logger;

public class EfinUniqueCodeFilterParamBudget implements FilterExpression {
  private final static Logger log4j = Logger.getLogger(EfinUniqueCodeFilterParamBudget.class);

  /**
   * This class is used to filter unique code by parambudget
   */
  @Override
  public String getExpression(Map<String, String> requestMap) {
    try {
      StringBuilder whereClause = new StringBuilder();
      String accountId = requestMap.get("C_Elementvalue_ID");
      String query = "";
      if (!StringUtils.isEmpty(accountId) && !accountId.equalsIgnoreCase("null")) {
        // Filter project based on account
        query = "e.id in (select distinct ac.salesCampaign.id from FinancialMgmtAccountingCombination ac where ac.account.id = '"
            + accountId + "' and ac.salesCampaign is not null)";
      }
      whereClause.append(query);
      return whereClause.toString();
    } catch (Exception e) {
      log4j.error("Exception in EfinUniqueCodeFilterParamBudget : " + e);
      return "";
    }
  }
}
