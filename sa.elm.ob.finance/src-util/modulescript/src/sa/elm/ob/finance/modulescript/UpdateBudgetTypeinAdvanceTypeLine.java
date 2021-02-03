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
import java.sql.ResultSet;

import org.openbravo.database.ConnectionProvider;  
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;


/**
 * 
 * @author Priyanka Ranjan 04-05-2018
 *
 */
/**
 * In "Advance Type" line screen
 * Update Budget type with same budget type which we have in selected UniqueCode, if both are different 
 */

public class UpdateBudgetTypeinAdvanceTypeLine extends ModuleScript {
  public void execute() {  
    ConnectionProvider cp = getConnectionProvider();
    PreparedStatement st = null;
    try {
      st = cp.getPreparedStatement(" select unicode.c_campaign_id as budtype,efin_distribution_lines_id from efin_distribution_lines ln "
          + " join c_validcombination unicode on ln.c_validcombination_id = unicode.c_validcombination_id where ln.c_campaign_id != unicode.c_campaign_id "
          + " and ln.c_validcombination_id is not null ");
      st.executeQuery();
      ResultSet rs = st.getResultSet();
      
      while(rs.next()) {     
        st = cp.getPreparedStatement(" update efin_distribution_lines set c_campaign_id=? where efin_distribution_lines_id=? ");
        st.setString(1,rs.getString("budtype"));
        st.setString(2,rs.getString("efin_distribution_lines_id"));
        st.executeUpdate();
      }
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
        new OpenbravoVersion(1,0,43));
  }
       
}
