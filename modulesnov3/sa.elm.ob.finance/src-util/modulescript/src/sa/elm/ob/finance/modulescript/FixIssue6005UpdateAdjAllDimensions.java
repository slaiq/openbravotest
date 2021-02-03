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
 * All portions are Copyright (C) 2010 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
  
package sa.elm.ob.finance.modulescript;

import java.sql.PreparedStatement;


import org.openbravo.database.ConnectionProvider;  
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;


/**
 * 
 * @author Priyanka Ranjan 05-01-2018
 * 
 * This module script to update the all 9 Dimensions based on c_validcombination in Fund and Cost Adjusment Line
 *
 */

public class FixIssue6005UpdateAdjAllDimensions extends ModuleScript {
  public void execute() {  
    ConnectionProvider cp = getConnectionProvider();
    PreparedStatement st = null;
    try {
      st = cp
          .getPreparedStatement("update efin_budgetadjline ln set orgid=subquery.ad_org_id,c_salesregion_id=subquery.c_salesregion_id, c_elementvalue_id=subquery.account_id,c_project_id=subquery.c_project_id,c_campaign_id=subquery.c_campaign_id,c_bpartner_id=subquery.c_bpartner_id, c_activity_id=subquery.c_activity_id,user1_id=subquery.user1_id,user2_id=subquery.user2_id from (select adjln.efin_budgetadjline_id,vc.ad_org_id,vc.c_salesregion_id,vc.account_id,vc.c_project_id,vc.c_campaign_id,vc.c_bpartner_id,vc.c_activity_id,vc.user1_id,vc.user2_id from efin_budgetadjline adjln join c_validcombination vc  on adjln.c_validcombination_id=vc.c_validcombination_id) AS subquery where ln.efin_budgetadjline_id=subquery.efin_budgetadjline_id and  ln.c_salesregion_id is null ");
      st.executeUpdate();
    } catch (Exception e) {   
      handleError(e);     
    }finally{ 
      try{   
      if(st!=null){  
        st.close();  
      }
      }catch (Exception e) {
        handleError(e);
      }
  }

}
  
  
  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("B0A58AE7D0994414B2B315E0A7087044",  null, 
        new OpenbravoVersion(1,0,31));
  }
       
}
