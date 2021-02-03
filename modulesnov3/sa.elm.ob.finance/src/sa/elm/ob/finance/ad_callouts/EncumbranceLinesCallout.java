package sa.elm.ob.finance.ad_callouts;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.util.CommonValidations;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 */

public class EncumbranceLinesCallout extends SimpleCallout {

  /**
   * Callout to update the uniqueCode Information in EncumbranceLines Window
   */
  private static final long serialVersionUID = 1L;
  private static final String warningMessage = "Efin_amount_greater_fund";
  final private static Logger log = Logger.getLogger(EncumbranceLinesCallout.class);

  @SuppressWarnings("unused")
  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpamount = vars.getStringParameter("inpamount").replace(",", "");
    String inporiginalamount = vars.getStringParameter("inporiginalamount").replace(",", "");
    String inpusedAmount = vars.getStringParameter("inpusedAmount").replace(",", "");
    String inpcValidCombinationID = vars.getStringParameter("inpcValidcombinationId");
    String inpefinBudgetintId = vars.getStringParameter("inpefinBudgetintId");
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String strBudgetInitializationId = vars.getStringParameter("inpefinBudgetintId");

    JSONObject resultfunds = null;
    String parsedMessage = null;
    // GLJournalLine line = null;

    if (inpLastFieldChanged.equals("inpamount")) {
      BigDecimal amount = new BigDecimal(inpamount);
      if (amount.signum() == -1) {
        info.addResult("ERROR",
            String.format(Utility.getADMessage("Efin_Manencumline_amt", vars.getLanguage())));
      } else {
        info.addResult("inporiginalamount", amount.toString());
        inporiginalamount = vars.getStringParameter("inporiginalamount").replace(",", "");
        inpusedAmount = vars.getStringParameter("inpusedAmount").replace(",", "");
        info.addResult("inprevamount", inpamount.toString());
        info.addResult("inpsystemUpdatedAmt", inpamount.toString());
        EfinBudgetIntialization initial = OBDal.getInstance().get(EfinBudgetIntialization.class,
            inpefinBudgetintId);
        AccountingCombination com = OBDal.getInstance().get(AccountingCombination.class,
            inpcValidCombinationID);
        resultfunds = CommonValidationsDAO.CommonFundsChecking(initial, com, amount);
        if (resultfunds != null) {
          try {
            if (resultfunds.getString("errorFlag").equals("0")) {
              parsedMessage = org.openbravo.erpCommon.utility.Utility.messageBD(this,
                  warningMessage, OBContext.getOBContext().getLanguage().getId());
              info.addResult("WARNING", parsedMessage);

            }
          } catch (JSONException e) {
            throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
          }
        }

      }
    }
    log.debug("inpLastFieldChanged:" + inpLastFieldChanged);

    if (inpLastFieldChanged.equals("inpcValidcombinationId")) {
      AccountingCombination combination = OBDal.getInstance().get(AccountingCombination.class,
          inpcValidCombinationID);
      BigDecimal fundsAvailable = BigDecimal.ZERO;
      JSONObject fundsCheckingObject = null;

      if (combination != null) {
        EfinBudgetIntialization budgetIntialization = Utility
            .getObject(EfinBudgetIntialization.class, strBudgetInitializationId);

        try {
          if ("E".equals(combination.getEfinDimensiontype())) {
            fundsCheckingObject = CommonValidations.getFundsAvailable(budgetIntialization,
                combination);
            fundsAvailable = new BigDecimal(fundsCheckingObject.get("FA").toString());
          }
        } catch (Exception e) {
          fundsAvailable = BigDecimal.ZERO;
        }
      }
      info.addResult("inpadOrgId", combination.getOrganization().getId());
      if (combination.getAccount() != null)
        info.addResult("inpcElementvalueId", combination.getAccount().getId());
      if (combination.getSalesRegion() != null)
        info.addResult("inpcSalesregionId", combination.getSalesRegion().getId());
      if (combination.getProject() != null)
        info.addResult("inpcProjectId", combination.getProject().getId());
      if (combination.getBusinessPartner() != null)
        info.addResult("inpcBpartnerId", combination.getBusinessPartner().getId());
      if (combination.getActivity() != null)
        info.addResult("inpcActivityId", combination.getActivity().getId());
      if (combination.getStDimension() != null)
        info.addResult("inpuser1Id", combination.getStDimension().getId());
      if (combination.getNdDimension() != null)
        info.addResult("inpuser2Id", combination.getNdDimension().getId());
      if (combination.getEfinUniquecodename() != null)
        info.addResult("inpuniquecodename", combination.getEfinUniquecodename());

      info.addResult("inpfundsAvailable", fundsAvailable);

    }
    // set blank value initially.
    if (inpLastFieldChanged.equals("inpadOrgId")) {
      info.addResult("JSEXECUTE", "form.getFieldFromColumnName('C_Elementvalue_ID').setValue('')");
      info.addResult("JSEXECUTE", "form.getFieldFromColumnName('C_Salesregion_ID').setValue('')");
      info.addResult("JSEXECUTE", "form.getFieldFromColumnName('c_project_id').setValue('')");
      info.addResult("JSEXECUTE", "form.getFieldFromColumnName('c_bpartner_id').setValue('')");
      info.addResult("JSEXECUTE", "form.getFieldFromColumnName('c_activity_id').setValue('')");
      info.addResult("JSEXECUTE", "form.getFieldFromColumnName('user1_id').setValue('')");
      info.addResult("JSEXECUTE", "form.getFieldFromColumnName('user2_id').setValue('')");
    }

    /*
     * if ((StringUtils.isEmpty(inpAccount)) && (!StringUtils.isEmpty(inpcValidCombinationID))) {
     * AccountingCombination combination = OBDal.getInstance().get(AccountingCombination.class,
     * inpcValidCombinationID); inpAccount = combination.getAccount().getId(); }
     */

    /*
     * String uniquecode = dao.getUniqueCode(inpOrgId, inpDepartment, inpAccount, inpBudgetType,
     * inpProject, inpActivity, inpUser1, inpUser2, inpEntity);
     * 
     * if (uniquecode != null && uniquecode.length() > 0) { if
     * (!StringUtils.isEmpty(vars.getStringParameter("inpcElementvalueId")))
     * info.addResult("inpuniquecode", uniquecode); else if
     * (StringUtils.isEmpty(vars.getStringParameter("inpcElementvalueId")))
     * info.addResult("inpemEfinUniquecode", uniquecode); try { OBContext.setAdminMode(); ps =
     * conn.prepareStatement(
     * "select * from efin_buget_process ( ?,?,?,?,?,?,?,?,to_date(?,'dd-MM-yyyy'), ?)");
     * 
     * // returns p_uniquecode,p_amount,p_budgetlines_id ps.setString(1,
     * vars.getStringParameter("inpadOrgId")); if
     * (!StringUtils.isEmpty(vars.getStringParameter("inpcElementvalueId"))) ps.setString(2,
     * vars.getStringParameter("inpcElementvalueId")); else if
     * (StringUtils.isEmpty(vars.getStringParameter("inpcElementvalueId")) &&
     * (!StringUtils.isEmpty(inpcValidCombinationID))) ps.setString(2, inpAccount); ps.setString(3,
     * vars.getStringParameter("inpcProjectId")); ps.setString(4,
     * vars.getStringParameter("inpcSalesregionId")); ps.setString(5,
     * vars.getStringParameter("inpcCampaignId")); ps.setString(6,
     * vars.getStringParameter("inpcActivityId")); ps.setString(7,
     * vars.getStringParameter("inpuser1Id")); ps.setString(8,
     * vars.getStringParameter("inpuser2Id")); ps.setString(9, Utility.formatDate(ActDate));
     * ps.setString(10, OBContext.getOBContext().getCurrentClient().getId()); log.debug("rs:" +
     * ps.toString()); rs = ps.executeQuery();
     * 
     * if (rs.next()) {
     * 
     * String amount = rs.getString("p_amount"); log.debug("amount:" + amount);
     * 
     * if (!StringUtils.isEmpty(vars.getStringParameter("inpcElementvalueId"))) {
     * info.addResult("inpfundsAvailable", amount); info.addResult("inpefinBudgetlinesId",
     * rs.getString("p_budgetlines_id"));
     * 
     * } else if (StringUtils.isEmpty(vars.getStringParameter("inpcElementvalueId"))) {
     * info.addResult("inpemEfinFundsAvailable", amount); }
     * 
     * } else info.addResult("inpfundsAvailable", "0.00"); OBDal.getInstance().flush(); } catch
     * (SQLException e) { // TODO Auto-generated catch block result =
     * e.getMessage().replace("ERROR: ", "").trim();
     * 
     * info.addResult("ERROR", Utility.getADMessage(result, vars.getLanguage()));
     * e.printStackTrace(); } finally { OBContext.restorePreviousMode(); } } else {
     * info.addResult("inpuniquecode", ""); }
     */

  }
}
