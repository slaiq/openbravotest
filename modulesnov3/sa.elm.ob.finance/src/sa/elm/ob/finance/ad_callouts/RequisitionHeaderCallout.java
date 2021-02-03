package sa.elm.ob.finance.ad_callouts;

import java.util.Date;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.ad_callouts.dao.RequisitionHeaderCalloutDAO;
import sa.elm.ob.scm.ESCMDefLookupsTypeLn;

/**
 * @author Gowtham.V
 */

public class RequisitionHeaderCallout extends SimpleCallout {

  /**
   * Callout to update the fields Information in purchase Requistion Window
   */
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(RequisitionHeaderCallout.class);

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    final String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String budgInitialId = null;
    String inpadClientId = vars.getStringParameter("inpadClientId");
    String inpadRoleId = vars.getRole();
    String encumId = vars.getStringParameter("inpemEfinBudgetManencumId");
    String encummethod = vars.getStringParameter("inpemEfinEncumMethod");
    String validCombination = vars.getStringParameter("inpemEfinEncumMethod");
    String processType = vars.getStringParameter("inpemEscmProcesstype");

    try {

      if (inpLastFieldChanged.equals("inpadOrgId")) {
        Date endDate = new Date();
        // getting budget initial id based on transaction date
        budgInitialId = BudgetAdjustmentCallout.getBudgetDefinitionForStartDate(endDate,
            inpadClientId, "");
        if (budgInitialId != null)
          info.addResult("inpemEfinBudgetintId", budgInitialId);
        else
          info.addResult("inpemEfinBudgetintId", null);
      }

      if (inpLastFieldChanged.equals("inpemEfinBudgetManencumId")) {
        // getting budget initial id based on transaction date
        if (encumId != null && !encumId.equals("")) {
          EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class, encumId);
          info.addResult("inpemEscmManualEncumNo", encum.getDocumentNo());
          String uniqueCode = RequisitionHeaderCalloutDAO.getUniqueCode(encumId, inpadClientId,
              inpadRoleId);
          if (uniqueCode != null) {
            /*
             * info.addResult("JSEXECUTE",
             * "form.getFieldFromColumnName('EM_Efin_C_Validcombination_ID').setValue('" +
             * uniqueCode + "')");
             */

            String jscode = "";
            jscode = "form.getFieldFromColumnName('EM_Efin_C_Validcombination_ID').setValue('"
                + uniqueCode + "');";
            /*
             * if (vars.getStringParameter("inpemEfinCValidcombinationId") == null ||
             * StringUtils.isEmpty(vars.getStringParameter("inpemEfinCValidcombinationId")))
             */
            jscode += "form.doChangeFICCall('EM_Efin_C_Validcombination_ID');";
            LOG.debug("jscode>" + jscode);
            info.addResult("JSEXECUTE", jscode);
          } else
            info.addResult("JSEXECUTE",
                "form.getFieldFromColumnName('EM_Efin_C_Validcombination_ID').setValue('')");
        }
      }
      // while changing encumbrance method remove value from encumbrance field
      if (inpLastFieldChanged.equals("inpemEfinEncumMethod")) {
        info.addResult("JSEXECUTE",
            "form.getFieldFromColumnName('EM_Efin_Budget_Manencum_ID').setValue('')");
        /*
         * info.addResult("inpemEfinCValidcombinationId", null);
         * info.addResult("inpemEscmManualEncumNo", null);
         */
      }
      // If encum method as manual then uniquecode field as empty
      if (encummethod.equals("M") && validCombination != null) {
        info.addResult("inpemEfinCValidcombinationId", null);
        /*
         * String enumId = RequisitionHeaderCalloutDAO.getManualEncumId(
         * BudgetAdjustmentCallout.getPeriodStartDate(new Date(), inpadClientId)); if (enumId !=
         * null) info.addResult("JSEXECUTE",
         * "form.getFieldFromColumnName('EM_Efin_Budget_Manencum_ID').setValue('" + enumId + "')");
         */
      }

      if (inpLastFieldChanged.equals("inpemEscmProcesstype")) {
        if (RequisitionHeaderCalloutDAO.getEncumbranceControlCount(processType) == 0) {
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Efin_C_Validcombination_ID').setDisabled(true))");
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Efin_Encum_Method').setDisabled(true))");
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Efin_Skipencumbrance').setDisabled(true))");
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Escm_Manual_Encum_No').setDisabled(true))");
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('Em_Escm_Accountno').setDisabled(true))");
        }

        if (RequisitionHeaderCalloutDAO.updateSecuredString(processType)) {
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Escm_Issecured').setValue(true)");
        } else {
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Escm_Issecured').setValue(false)");
        }
        // If process type is Direct, default specialized dept to NA
        if (processType != null && processType.equals("DP")) {
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Escm_Specializeddept').setValue('NA')");
        }
      }
      if (inpLastFieldChanged.equals("inpemEscmContactType")) {
        ESCMDefLookupsTypeLn cntrctCatlookup = OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
            vars.getStringParameter("inpemEscmContactType"));

        if (cntrctCatlookup != null && !cntrctCatlookup.isMaintenancecontract()) {
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Escm_Maintenance_Cntrct_No').setValue('')");
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Escm_Maintenance_Project').setValue('')");
        }
      }
      if (inpLastFieldChanged.equals("inpemEscmMaintenanceProject")) {

        ESCMDefLookupsTypeLn lookup = OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
            vars.getStringParameter("inpemEscmMaintenanceProject"));
        if (lookup != null) {
          String Cno = lookup.getSearchKey();
          String projectName = lookup.getCommercialName();
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Escm_Maintenance_Cntrct_No').setValue(" + Cno + ")");
          info.addResult("inpdescription", projectName + "-" + Cno);
        } else {
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Escm_Maintenance_Cntrct_No').setValue('')");
          info.addResult("inpdescription", null);
        }

      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error whie getting budget init in requisition callout:", e);
      }
    }
  }
}
