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

import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;  
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;


/**
 * 
 * @author Poongodi on 14/05/2019
 *
 */

public class UniqueCodeDuplicate7518 extends ModuleScript {
  PreparedStatement ps = null;
  ResultSet rs = null, rs1 = null;
  String uniqueCode=null;
  String validCombinationId=null;
  String count = null;
  int limitCount = 0;
  public void execute() {  
    try {
      ConnectionProvider cp = getConnectionProvider();
      ps = cp
         .getPreparedStatement("select count(c_validcombination_id) as Validcount,EM_Efin_Uniquecode from c_validcombination group by EM_Efin_Uniquecode,ad_client_id "  
             + " having count(EM_Efin_Uniquecode) > 1");
     
      rs = ps.executeQuery();
   
      while(rs.next())
      {           
        count = UtilSql.getValue(rs, "Validcount");
        limitCount = Integer.parseInt(count);
        uniqueCode=UtilSql.getValue(rs, "EM_Efin_Uniquecode");
        ps = cp.getPreparedStatement("select c_validcombination_id from  c_validcombination where EM_Efin_Uniquecode=? limit ?");
        ps.setString(1, uniqueCode);
        ps.setInt(2, limitCount-1);
        rs1 = ps.executeQuery();
        while(rs1.next())
       {
         validCombinationId=UtilSql.getValue(rs1, "c_validcombination_id");
         ps = cp
             .getPreparedStatement("update c_validcombination set EM_Efin_Uniquecode=EM_Efin_Uniquecode || floor(random() * 1000 + 1)::int where c_validcombination_id = ?");
         ps.setString(1, validCombinationId);
         ps.executeUpdate();
      }

      }
    } catch (Exception e) {   
      handleError(e);     
    }finally{ 
      try{   
      if(ps!=null){  
        ps.close();  
      }
      }catch (Exception e) {
        handleError(e);
      }
  }

}
  
  
  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("B0A58AE7D0994414B2B315E0A7087044",  null, 
        new OpenbravoVersion(1,0,87));
  }
       
}
