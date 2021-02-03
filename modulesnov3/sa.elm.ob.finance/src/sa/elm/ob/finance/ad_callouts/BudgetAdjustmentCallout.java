package sa.elm.ob.finance.ad_callouts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.ad_callouts.dao.HijiridateDAO;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Gopalakrishnan on 15/09/2017
 */

public class BudgetAdjustmentCallout extends SimpleCallout {
  /**
   * This CallOut to fill Transaction period in Budget Adjustment Header
   */
  private static final long serialVersionUID = 1L;
  public static final String strBudgetWindowID = "0D8568D5973442B6ABA7EEF7D044CF78";
  public static final String strBudgetRevisionWindowID = "05C3944B54FE4C5DA0E735D1144DCB94";
  public static final String strBudgetAdjustmentWindowID = "C23E598B7EE740A3AFACEF68F0F7C263";

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;

    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String trxdate = vars.getStringParameter("inptrxDate");
    String accdate = vars.getStringParameter("inpaccountingDate");
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String inpadClientId = vars.getStringParameter("inpadClientId");
    String accGredate = "", yearId = "", budgetReferenceId = null;
    Date endDate = null;
    // load accounting date same as transaction date ( edit on 26/10/2016) and trx period load based
    // on trx date
    if (inpLastFieldChanged.equals("inptrxDate") || inpLastFieldChanged.equals("inpadOrgId")) {
      try {
        info.addResult("inpaccountingDate", vars.getStringParameter("inptrxDate"));
        String gregorianmonth = HijiridateDAO.getGregorianPeriod(trxdate);
        info.addResult("inptransactionperiod", gregorianmonth);
        accGredate = UtilityDAO.convertToGregorian(accdate);
        endDate = dateFormat.parse(accGredate);
        yearId = HijiridateDAO.getYearId(endDate, inpadClientId);
        info.addResult("inpcYearId", yearId);
        budgetReferenceId = getBudgetDefinitionForStartDate(endDate, inpadClientId,
            strBudgetAdjustmentWindowID);
        info.addResult("inpefinBudgetintId", budgetReferenceId == null ? null : budgetReferenceId);
      } catch (Exception e) {
        log4j.info("Process failed populating accounting date from transaction date", e);
        throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      }
    }

    if (inpLastFieldChanged.equals("inpaccountingDate")) {
      try {
        accGredate = UtilityDAO.convertToGregorian(accdate);
        endDate = dateFormat.parse(accGredate);
        yearId = HijiridateDAO.getYearId(endDate, inpadClientId);
        info.addResult("inpcYearId", yearId);
        budgetReferenceId = getBudgetDefinitionForStartDate(endDate, inpadClientId,
            strBudgetAdjustmentWindowID);
        info.addResult("inpefinBudgetintId", budgetReferenceId == null ? null : budgetReferenceId);
      } catch (Exception e) {
        log4j.info("Process failed populating year based on accounting date", e);
        throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      }
    }

  }

  /**
   * 
   * @param date
   * @param clientId
   * @return budgetReferenceid
   */
  public static String getBudgetDefinitionForStartDate(Date date, String clientId,
      String windowId) {
    final Logger log = LoggerFactory.getLogger(HijiridateDAO.class);
    PreparedStatement ps = null, ps1 = null;
    ResultSet rs = null, rs1 = null;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String startingDate = "", budgetReferenceId = null,
        isPrecloseQuery = " and br.ispreclose ='N' ";
    Connection conn = OBDal.getInstance().getConnection();
    try {
      ps = conn.prepareStatement(
          "select to_char(startdate,'dd-MM-yyyy') as startingDate from c_period where to_date('"
              + dateFormat.format(date) + "','yyyy-MM-dd')"
              + " between to_date(to_char(startdate,'dd-MM-yyyy'),'dd-MM-yyyy') and to_date(to_char(enddate,'dd-MM-yyyy'),'dd-MM-yyyy') "
              + "  and ad_client_id ='" + clientId + "'");
      rs = ps.executeQuery();
      if (rs.next()) {
        startingDate = rs.getString("startingDate") == null ? "" : rs.getString("startingDate");
      }
      if (windowId.equals(strBudgetWindowID) || windowId.equals(strBudgetAdjustmentWindowID)
          || windowId.equals(strBudgetRevisionWindowID)) {
        isPrecloseQuery = " and br.ispreclose ='N' ";
      }
      ps1 = conn.prepareStatement("select br.efin_budgetint_id from efin_budgetint br "
          + " join c_period fp on fp.c_period_id =br.fromperiod "
          + " join c_period tp on tp.c_period_id =br.toperiod " + " where to_date('" + startingDate
          + "','dd-MM-yyyy') between fp.startdate and tp.enddate and br.status ='OP' "
          + isPrecloseQuery + " and br.ad_client_id ='" + clientId + "' limit 1");
      rs1 = ps1.executeQuery();
      if (rs1.next()) {
        budgetReferenceId = rs1.getString("efin_budgetint_id");
      }
    } catch (Exception e) {
      log.info("Error while getting Budget Reference in Adjustement callout", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
        if (rs1 != null)
          rs1.close();
        if (ps1 != null)
          ps1.close();

      } catch (Exception e) {
        log.debug("error while getting budget Reference", e);
        throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      }
    }
    return budgetReferenceId;
  }

}
