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
import java.util.List;


import org.openbravo.database.ConnectionProvider;  
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;




/**
 * 
 * @author Priyanka Ranjan 20-03-2018
 *
 */

public class FixIssue4335UpdateNamewithRandomNumber extends ModuleScript {
  /**
   * This class is used to update name with random number if duplicate name is exist
   */
  public void execute() {  
    ConnectionProvider cp = getConnectionProvider();
    PreparedStatement st = null; 
    try {
      st = cp.getPreparedStatement("update efin_distribution set name = name || floor(random() * 10 + 1)::int " + 
          " where name in(select name from efin_distribution group by name,ad_client_id " + 
          " having count(name) >1) ");
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
        new OpenbravoVersion(1,0,37));
  }
    
}

