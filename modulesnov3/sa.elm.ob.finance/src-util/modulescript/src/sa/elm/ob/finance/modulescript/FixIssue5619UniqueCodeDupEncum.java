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
 * @author Divya.J
 *
 */

public class FixIssue5619UniqueCodeDupEncum extends ModuleScript {
  public void execute() {  
    ConnectionProvider cp = getConnectionProvider();
    PreparedStatement st = null;
    try {
      st = cp.getPreparedStatement(" select efin_budget_manencum_id,count(efin_budget_manencumlines_id) as count ,c_validcombination_id  from efin_budget_manencumlines   group by c_validcombination_id,efin_budget_manencum_id having count(c_validcombination_id) > 1  ");
      st.executeQuery();
      ResultSet rs = st.getResultSet();
      
      while(rs.next()) {     
        st = cp.getPreparedStatement(" update efin_budget_manencumlines set c_validcombination_id=null where efin_budget_manencumlines_id in (      select efin_budget_manencumlines_id from efin_budget_manencumlines where efin_budget_manencum_id= ? and c_validcombination_id= ? order by created desc limit ?) ");
        st.setString(1,rs.getString("efin_budget_manencum_id"));
        st.setString(2,rs.getString("c_validcombination_id"));
        st.setInt(3, (rs.getInt("count")-1));
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
        new OpenbravoVersion(1,0,22));
  }
       
}
