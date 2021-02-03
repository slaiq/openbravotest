package sa.elm.ob.finance.ad_callouts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Gopalakrishnan on 31/05/2106
 */

public class BudgetRevisionHeader extends SimpleCallout {

  /**
   * Callout to update the Year field Information in BudgetHeader Window
   */
  private static final long serialVersionUID = 1L;
  Logger logger = Logger.getLogger(BudgetRevisionHeader.class);
  private static final String windowId = "05C3944B54FE4C5DA0E735D1144DCB94";

  @SuppressWarnings("resource")
  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    Connection conn = OBDal.getInstance().getConnection();
    String clientId = vars.getStringParameter("inpadClientId");
    PreparedStatement ps = null;
    ResultSet rs = null;
    String Accdate = vars.getStringParameter("inpaccountingDate");
    String accGredate = "", budgetReferenceId = null;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    Date endDate = null;
    try {

      SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();

      // get the data as json
      String AccountingDate = (vars.getStringParameter("inpaccountingDate").equals("null")
          ? df.format(now)
          : vars.getStringParameter("inpaccountingDate"));

      boolean check = Utility.HijriDateValidion(AccountingDate);
      String[] inpd = AccountingDate.split("-");
      String EnterDate = inpd[2] + inpd[1] + inpd[0];
      if (check == false) {
        String Maxhijridate = null;
        String Minhijridate = null;

        try {
          ps = OBDal.getInstance().getConnection()
              .prepareStatement("select max(hijri_date) as maxdate from eut_hijri_dates");
          rs = ps.executeQuery();
          if (rs.next()) {
            Maxhijridate = rs.getString("maxdate");

          }
          ps = OBDal.getInstance().getConnection()
              .prepareStatement("select min(hijri_date) as mindate from eut_hijri_dates");
          rs = ps.executeQuery();
          if (rs.next()) {
            Minhijridate = rs.getString("mindate");

          }
          // check entered date is greater than maximum hijriDate then update maximum hijriDate
          if (Integer.parseInt(EnterDate) > Integer.parseInt(Maxhijridate)) {
            info.addResult("inpaccountingDate", Utility.MaxHijriDate());

          }

          // check entered date is less than minimum hijriDate then update minimum hijriDate
          if (Integer.parseInt(EnterDate) < Integer.parseInt(Minhijridate)) {
            info.addResult("inpaccountingDate", Utility.MinHijriDate());
          }

        } catch (final Exception e) {
          log4j.error("Exception in  hijiridate: ", e);
        }

      }
      if (inpLastFieldChanged.equals("inpaccountingDate")) {

        // set transaction date as accounting date
        // info.addResult("inptrxdate", vars.getStringParameter("inpaccountingDate"));

        ps = conn.prepareStatement(
            "select yr.c_year_id as year from c_period  pr  join c_year yr on yr.c_year_id= pr.c_year_id "
                + "	where eut_convertto_gregorian('" + AccountingDate
                + "')  between startdate and enddate  and pr.ad_client_id='"
                + OBContext.getOBContext().getCurrentClient().getId()
                + "' and pr.ad_org_id = '0' ");

        if (logger.isDebugEnabled()) {
          logger.debug("Accounting Date : " + AccountingDate);
          logger.debug("Query : " + ps.toString());
        }

        rs = ps.executeQuery();
        if (rs.next()) {
          info.addResult("inpcYearId", rs.getString("year"));
        } else {
          info.addResult("inpcYearId", null);
        }
        accGredate = UtilityDAO.convertToGregorian(Accdate);
        endDate = dateFormat.parse(accGredate);
        budgetReferenceId = BudgetAdjustmentCallout.getBudgetDefinitionForStartDate(endDate,
            clientId, windowId);
        if (budgetReferenceId != null) {
          info.addResult("inpefinBudgetintId", budgetReferenceId);
        } else {
          info.addResult("inpefinBudgetintId", null);
        }
      }
    } catch (Exception e) {
      logger.error("Exception in BudgetRevision Header Callout: " + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      // close connection
      try {
        if (rs != null) {
          rs.close();
        }
        if (ps != null) {
          ps.close();
        }
      } catch (Exception e) {
        logger.error("Exception in closing connection :" + e);
      }
    }
  }
}
