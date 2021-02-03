package sa.elm.ob.finance.modulescript;

import java.sql.PreparedStatement;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;

/**
 * 
 * @author Gowtham
 *
 *This class is used to update cheque staus to temp column
 */
public class UpdateChqStatus extends ModuleScript{
  public void execute() {  
    ConnectionProvider cp = getConnectionProvider();
    PreparedStatement st = null;
    try {
      st = cp.getPreparedStatement(" update fin_payment set em_efin_mofchqstatus = null where em_efin_mofchqstatus not in ('PISM','PIAM','PIM','PRM') ");
      st.executeUpdate();
      st = cp.getPreparedStatement(" update fin_payment set em_efin_mofchqstatus_tmp =em_efin_mofchqstatus ");
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
        new OpenbravoVersion(1,0,56));
  }
}
