package sa.elm.ob.utility.modulescript;

import java.sql.PreparedStatement;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;

public class ApprovalDelegationDropFunction extends ModuleScript {
  public void execute() {
    ConnectionProvider cp = getConnectionProvider();
    PreparedStatement st = null;
    try {
      st = cp.getPreparedStatement("DROP FUNCTION  IF EXISTS "
          + "eut_delegation_access(character varying, character varying);");
      st.executeUpdate();
      st = cp.getPreparedStatement("DROP FUNCTION  IF EXISTS "
          + "eut_delprocess_preference(character varying);");
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
        new OpenbravoVersion(0, 0, 10));
  }
}