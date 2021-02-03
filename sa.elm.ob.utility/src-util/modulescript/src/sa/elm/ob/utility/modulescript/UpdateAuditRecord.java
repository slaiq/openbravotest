package sa.elm.ob.utility.modulescript;

import java.sql.PreparedStatement;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;

public class UpdateAuditRecord extends ModuleScript {
  public void execute() {
    ConnectionProvider cp = getConnectionProvider();
    PreparedStatement st = null;
    try {
      st = cp.getPreparedStatement("update ad_table set isfullyaudited =em_eut_isfullyaudited");
      st.executeUpdate();
     
      st = cp.getPreparedStatement("ALTER TABLE ad_column disable TRIGGER ad_column_trg2");
      st = cp.getPreparedStatement("update ad_column set isexcludeaudit = em_eut_isexcludeaudit");
      st.executeUpdate();
      st = cp.getPreparedStatement("ALTER TABLE ad_column enable TRIGGER ad_column_trg2");
      
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
        new OpenbravoVersion(0, 0, 22));
  }
}