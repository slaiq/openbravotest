package sa.elm.ob.scm.modulescript;

import java.sql.PreparedStatement;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;

public class FixIssue7079DeleteUnusedNextRole extends ModuleScript {
  public void execute() {
    ConnectionProvider cp = getConnectionProvider();

    try {
      
        String sqlCondition = "";
        sqlCondition = "(select eut_next_role_id from escm_bidmgmt where eut_next_role_id is not null)";
        deleteRoles(sqlCondition,"EUT_116",cp);
        
        sqlCondition = "(select em_eut_next_role_id from c_order where em_eut_next_role_id is not null)";
        deleteRoles(sqlCondition,"EUT_108",cp);
        
        sqlCondition = "(select em_eut_next_role_id from gl_journal where em_eut_next_role_id is not null)";
        deleteRoles(sqlCondition,"EUT_106",cp);
        
        sqlCondition = "(select em_eut_next_role_id from c_invoice where em_eut_next_role_id is not null)";
        deleteRoles(sqlCondition,"EUT_101",cp);
        deleteRoles(sqlCondition,"EUT_109",cp);
        deleteRoles(sqlCondition,"EUT_110",cp);
        
        sqlCondition = "(select em_eut_next_role_id from m_requisition where em_eut_next_role_id is not null)";
        deleteRoles(sqlCondition,"EUT_111",cp);
        deleteRoles(sqlCondition,"EUT_118",cp);
        
        sqlCondition = "(select eut_next_role_id from escm_material_request where eut_next_role_id is not null)";
        deleteRoles(sqlCondition,"EUT_112",cp);
        deleteRoles(sqlCondition,"EUT_115",cp);
        
        sqlCondition = "(select em_eut_next_role_id from m_inout where em_eut_next_role_id is not null)";
        deleteRoles(sqlCondition,"EUT_113",cp);
        
        sqlCondition = "(select eut_next_role_id from m_inout where em_escm_iscustody_transfer='Y' and  eut_next_role_id is not null)";
        deleteRoles(sqlCondition,"EUT_114",cp);
        
        sqlCondition = "(select eut_next_role_id from escm_proposalmgmt where eut_next_role_id is not null)";
        deleteRoles(sqlCondition,"EUT_117",cp);
        deleteRoles(sqlCondition,"EUT_122",cp);
                
        sqlCondition = "(select eut_next_role_id from escm_technicalevl_event where eut_next_role_id is not null)";
        deleteRoles(sqlCondition,"EUT_123",cp);
               
    } catch (Exception e) {
      handleError(e);
    } 
  }

  public void deleteRoles(String sqlConn,String docType,ConnectionProvider cp) {
    PreparedStatement st = null;
    try {
    //Delete Next Role Line
    st = cp.getPreparedStatement(
        "delete from eut_next_role_line where eut_next_role_id in (select eut_next_role_id from eut_next_role where document_type = ? and eut_next_role_id not in "
            + sqlConn + ");");
    st.setString(1, docType);
    st.executeUpdate();

    // Delete Next Role
    st = cp.getPreparedStatement(
        "delete from eut_next_role where document_type = ? and eut_next_role_id not in "
            + sqlConn + ";");
    st.setString(1, docType);
    st.executeUpdate();
  } catch (Exception e) {
    handleError(e);
  } finally {
    try {
      if (st != null) {
        st.close();
      }
    } catch (Exception e) {
      handleError(e);
    }
  }
  }
  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("0B1224F8F87F4ED68E04C68BC423BC3A", null,
        new OpenbravoVersion(1, 0, 73));
  }
}