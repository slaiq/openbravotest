package sa.elm.ob.utility.modulescript;

import java.sql.PreparedStatement;

import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;

public class updateForwardRecord extends ModuleScript {
  public void execute() {
    ConnectionProvider cp = getConnectionProvider();
    PreparedStatement st = null;
    String reqmoreInfoeId = null;
    try {
      st = cp.getPreparedStatement("update eut_forward_reqmoreinfo set processed ='Y',status ='DR' where eut_forward_reqmoreinfo_id not in " 
        +  "  (select em_eut_forward_id from m_requisition where em_eut_forward_id is not null) "
        +  "  and eut_forward_reqmoreinfo_id not in (select eut_forward_reqmoreinfo_id from escm_bidmgmt where eut_forward_reqmoreinfo_id is not null )  " 
        +  "  and eut_forward_reqmoreinfo_id not in (select em_eut_forward_id from c_order where em_eut_forward_id is not null) " 
         + "   and eut_forward_reqmoreinfo_id not in (select eut_forward_reqmoreinfo_id from escm_proposalmgmt where eut_forward_reqmoreinfo_id is not null) "  
         + "   and processed = 'N' and forward_rmi = 'F'");
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
    return new ModuleScriptExecutionLimits("E610580A91734A8EB5A88BD4AC9E19FB", null,
        new OpenbravoVersion(0, 0, 17));
  }
}