package sa.elm.ob.finance.ad_callouts;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINFundsReqLine;
import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.ad_callouts.dao.FundsReqMangementDAO;
import sa.elm.ob.finance.ad_callouts.dao.HijiridateDAO;
import sa.elm.ob.finance.event.dao.BudgetLinesDAO;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author Divya.J
 * 
 */
public class FundsReqManagementCallout extends SimpleCallout {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(BudgetLinesDAO.class);
  private static final String strFRMwindowId = "4824ABD4AE6E49F68F2AAFE976EFFEC2";

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    VariablesSecureApp vars = info.vars;
    String inppercentage = vars.getStringParameter("inppercentage");
    String inptransactionOrg = vars.getStringParameter("inptransactionOrg");
    String inpadClientId = vars.getStringParameter("inpadClientId");
    String inptrxdate = vars.getStringParameter("inptrxdate");
    String inpdateacct = vars.getStringParameter("inpdateacct");
    String inpfromaccount = vars.getStringParameter("inpfromaccount");
    String inptoaccount = vars.getStringParameter("inptoaccount");
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpefinFundsreqId = vars.getStringParameter("inpefinFundsreqId");
    String inptrxtype = vars.getStringParameter("inptrxtype");
    String inpdistType = vars.getStringParameter("inpdistType");
    String inpincrease = vars.getStringParameter("inpincrease");
    String inpdecrease = vars.getStringParameter("inpdecrease");
    String inpreqType = vars.getStringParameter("inpreqType");
    String preferenceValue = "";
    String budgInitialId = null;
    Connection conn = OBDal.getInstance().getConnection();
    String transactionPeriod = null, yearId = null;
    JSONObject result = null;
    BigDecimal increaseAmt = BigDecimal.ZERO, percentage = BigDecimal.ZERO;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    EfinBudgetControlParam controlPrmObj = null;
    LOG.debug("inpLastFieldChanged" + inpLastFieldChanged);
    Boolean ispreference = false;

    try {
      preferenceValue = Preferences.getPreferenceValue("EFIN_BCU_DEP", true, vars.getClient(),
          vars.getOrg(), vars.getUser(), vars.getRole(), "4824ABD4AE6E49F68F2AAFE976EFFEC2");
    } catch (PropertyException e) {
    }
    if (preferenceValue != null && preferenceValue.equals("Y"))
      ispreference = true;

    if (inpLastFieldChanged.equals("inpadOrgId")) {

      if (!ispreference) {

      }
    }
    if (inpLastFieldChanged.equals("inppercentage")) {
      percentage = new BigDecimal(inppercentage);
      if (inptoaccount != null && percentage.compareTo(BigDecimal.ZERO) > 0) {
        AccountingCombination toAcctObj = OBDal.getInstance().get(AccountingCombination.class,
            inptoaccount);
        EFINFundsReqLine fromAcctLineobj = FundsReqMangementDAO
            .chkFrmAcctPrestorNot(inpefinFundsreqId, toAcctObj);
        increaseAmt = fromAcctLineobj.getDecrease()
            .multiply(percentage.divide(new BigDecimal(100)));
        info.addResult("inpincrease", increaseAmt);

      } else {
        info.addResult("inpincrease", "0.00");
      }

    }
    if (inpLastFieldChanged.equals("inptransactionOrg")) {

      controlPrmObj = FundsReqMangementDAO.getControlParam(inpadClientId);
      if (controlPrmObj.getAgencyHqOrg() != null
          && controlPrmObj.getAgencyHqOrg().getId().equals(inptransactionOrg) && ispreference) {
        info.addResult("inptrxtype", "BCUR");
        info.addResult("inporgreqFundsType", "OD");
      } else {
        info.addResult("inptrxtype", "ORGR");
        info.addResult("inporgreqFundsType", "OD");

      }
    }

    if (inpLastFieldChanged.equals("inptrxdate") || inpLastFieldChanged.equals("inpadOrgId")) {

      // change the Accounting date based on transaction date
      info.addResult("inpdateacct", inptrxdate);

      try {
        // getting transaction period based on transaction date
        transactionPeriod = HijiridateDAO.getGregorianPeriod(inptrxdate);

        if (transactionPeriod != null)
          info.addResult("inptransactionperiod", transactionPeriod);
        else
          info.addResult("inptransactionperiod", "");
        // getting yearId based on transaction date
        String transdate = UtilityDAO.convertToGregorian(inptrxdate);
        Date endDate = dateFormat.parse(transdate);
        yearId = FundsReqMangementDAO.getYearId(endDate, conn, inpadClientId);
        info.addResult("inpcYearId", yearId);

        // getting budget initial id based on transaction date
        budgInitialId = BudgetAdjustmentCallout.getBudgetDefinitionForStartDate(endDate,
            inpadClientId, strFRMwindowId);

        if (budgInitialId != null)
          info.addResult("inpefinBudgetintId", budgInitialId);
        else
          info.addResult("inpefinBudgetintId", null);

      } catch (SQLException e) {
        LOG.error("Exception in getYearId " + e.getMessage());
        throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      } catch (ParseException e) {
        LOG.error("Exception in getYearId " + e.getMessage());
        throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      }
    }
    if (inpLastFieldChanged.equals("inpdateacct")) {
      try {
        String dateacct = UtilityDAO.convertToGregorian(inpdateacct);
        Date endDate = dateFormat.parse(dateacct);
        LOG.debug("endDate:" + dateFormat.format(endDate));
        yearId = FundsReqMangementDAO.getYearId(endDate, conn, inpadClientId);
        info.addResult("inpcYearId", yearId);

        // getting budget initial id based on transaction date
        budgInitialId = BudgetAdjustmentCallout.getBudgetDefinitionForStartDate(endDate,
            inpadClientId, strFRMwindowId);

        if (budgInitialId != null)
          info.addResult("inpefinBudgetintId", budgInitialId);
        else
          info.addResult("inpefinBudgetintId", null);

      } catch (ParseException e) {
        LOG.error("Exception in getYearId " + e.getMessage());
        throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      }
    }
    if (inpLastFieldChanged.equals("inptoaccount")) {
      if (inptoaccount != null) {
        info.addResult("inpincrease", "0.00");
        info.addResult("inpdecrease", "0.00");
        info.addResult("inppercentage", "0");

      }
    }
    if (inpLastFieldChanged.equals("inpfromaccount")) {
      if (inpfromaccount != null) {
        info.addResult("inppercentage", "0");
        info.addResult("inpdecrease", "0.00");
        info.addResult("inpincrease", "0.00");

        try {
          if (inptrxtype.equals("BCUR")) {
            result = FundsReqMangementDAO.getBudgetInquiryLineDetail(inpfromaccount,
                inpefinFundsreqId, conn);
            if (result.has("fundsavailable"))
              info.addResult("inpfundsAvailable", result.get("fundsavailable"));
            else
              info.addResult("inpfundsAvailable", "0.00");
            if (result.has("currentBudget"))
              info.addResult("inpcurrentBudget", result.get("currentBudget"));
            else
              info.addResult("inpcurrentBudget", "0.00");
          } else {
            info.addResult("inpfundsAvailable", "0.00");
            info.addResult("inpcurrentBudget", "0.00");
          }
        } catch (JSONException e) {
          LOG.error("Exception in getBudgetLineDetail " + e, e);
          throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
        }

      } else {
        info.addResult("inppercentage", "");
      }
    }
    if (inpLastFieldChanged.equals("inpdecrease")) {
      if (inpdistType.equals("MAN"))
        info.addResult("inpincrease", inpdecrease);
    }
    if (inpLastFieldChanged.equals("inpincrease")) {
      if (inpdistType.equals("MAN"))
        info.addResult("inpdecrease", inpincrease);
    }
    if (inpLastFieldChanged.equals("inpreqType")) {
      info.addSelect("inpfromaccount");
      info.addSelectResult("", "", true);
      info.endSelect();

      info.addSelect("inptoaccount");
      info.addSelectResult("", "", true);
      info.endSelect();

      if (inptrxtype.equals("BCUR") && inpreqType.equals("REL")) {
        info.addResult("inpisdistribute", "N");
      } else if (inptrxtype.equals("BCUR") && inpreqType.equals("DIST")) {
        info.addResult("inpisdistribute", "Y");
      }
    }
    if (inpLastFieldChanged.equals("inpdistType")) {
      info.addResult("inpincrease", "0.00");
      info.addResult("inpdecrease", "0.00");
    }
  }
}
