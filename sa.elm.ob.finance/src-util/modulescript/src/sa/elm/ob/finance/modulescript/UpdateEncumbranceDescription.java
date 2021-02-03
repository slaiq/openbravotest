package sa.elm.ob.finance.modulescript;

import java.sql.PreparedStatement;

import org.openbravo.database.ConnectionProvider;  
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;

/**
 * 
 * @author Kiruthika
 */

/*
 * Task no. 7633 - Module script to update description in encumbrance.
 * 
 */
public class UpdateEncumbranceDescription extends ModuleScript {
  public void execute() {  
    ConnectionProvider cp = getConnectionProvider();
    PreparedStatement st = null;
    try {
      st = cp.getPreparedStatement(" update Efin_Budget_Manencum e set description = case when e.Encum_Type = 'POE' "
          + " then (select(coalesce((select coalesce(EM_Escm_Notes,'-') from c_order ord "
          + " where EM_Efin_Budget_Manencum_ID = e.Efin_Budget_Manencum_ID "
          + " and EM_Escm_Revision = (select max(EM_Escm_Revision) from c_order "
          + " where DocumentNo = ord.DocumentNo and EM_Escm_Appstatus = 'ESCM_AP' ) "
          + " limit 1    ),'-'))) "
          + " when e.Encum_Type = 'AEE' then (select(coalesce((select coalesce(description,'-') from c_invoice "
          + " where (EM_Efin_Manualencumbrance_ID = e.Efin_Budget_Manencum_Id "
          + " or  EM_Efin_Funds_Encumbrance_ID = e.Efin_Budget_Manencum_Id ) limit 1 ),'-'))) "
          + " when e.Encum_Type = 'PAE' then (select(coalesce((select coalesce(notes,'-') from escm_proposalmgmt "
          + " where EM_Efin_Encumbrance_ID = e.Efin_Budget_Manencum_Id limit 1 ),'-'))) "
          + " when e.Encum_Type = 'AAE' then (select(coalesce((select coalesce(description,'-') from c_invoice "
          + " where EM_Efin_Manualencumbrance_ID = e.Efin_Budget_Manencum_Id limit 1 ),'-'))) "
          + " when e.Encum_Type = 'PRE' then (select(coalesce((select coalesce(description,'-') from m_requisition "
          + " where EM_Efin_Budget_Manencum_ID = e.Efin_Budget_Manencum_Id limit 1 ),'-'))) "
          + " when e.Encum_Type = 'BE' then (select(coalesce((select coalesce(Bidpurpose,'-') from escm_bidmgmt "
          + " where Efin_Budget_Manencum_ID = e.Efin_Budget_Manencum_Id limit 1 ),'-'))) "
          + " when e.Encum_Type = 'TE' then (select(coalesce(((select coalesce(description,'-') from Efin_Budget_Transfertrx "
          + " where Efin_Budget_Manencum_ID = e.Efin_Budget_Manencum_Id limit 1 ) "
          + " union (select coalesce(description_Adjustment,Docno) from Efin_Budgetadj "
          + " where Efin_Budget_Manencum_ID = e.Efin_Budget_Manencum_Id limit 1 )),'-'))) "
          + "  else '-' end where description is null ");
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
        new OpenbravoVersion(1,0,93));
  }
  
}