import java.sql.PreparedStatement;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;

/**
 * 
 * @author Divya 26-03-2019
 *
 *This class is used to update the encumbrance amount field  as 0 if its null
 */

public class UpdateEncumAmtFieldZeroIfNull extends ModuleScript{
  public void execute() {  
    ConnectionProvider cp = getConnectionProvider();
    PreparedStatement st = null;
    try {
      //app amount
      st = cp.getPreparedStatement(" update efin_budget_manencumlines set app_amt=0 where app_amt is null ");
      st.executeUpdate();

      //enc_decrease
      st = cp.getPreparedStatement(" update efin_budget_manencumlines set enc_decrease=0 where enc_decrease is null ");
      st.executeUpdate();

       //enc_increase
      st = cp.getPreparedStatement(" update efin_budget_manencumlines set enc_increase=0 where enc_increase is null ");
      st.executeUpdate();

       //remaining_amount
      st = cp.getPreparedStatement(" update efin_budget_manencumlines set remaining_amount=0 where remaining_amount is null ");
      st.executeUpdate();

       //system_increase
      st = cp.getPreparedStatement(" update efin_budget_manencumlines set system_increase=0 where system_increase is null ");
      st.executeUpdate();

      //system_decrease
      st = cp.getPreparedStatement(" update efin_budget_manencumlines set system_decrease=0 where system_decrease is null ");
      st.executeUpdate();

      //system_updated_amt
      st = cp.getPreparedStatement(" update efin_budget_manencumlines set system_updated_amt=0 where system_updated_amt is null ");
      st.executeUpdate();


      //used_amount
      st = cp.getPreparedStatement(" update efin_budget_manencumlines set used_amount=0 where used_amount is null ");
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
        new OpenbravoVersion(1,0,79));
  }
}