package sa.elm.ob.finance.ad_callouts;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

import sa.elm.ob.finance.ad_callouts.dao.HijiridateDAO;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Priyanka Ranjan on 01/09/2016
 */

public class HijriDateCallout extends SimpleCallout {
  /**
   * This CallOut check hijriDate format
   */
  private static final long serialVersionUID = 1L;

  @SuppressWarnings("resource")
  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;

    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String trxdate = vars.getStringParameter("inptrxdate");
    String clientId = vars.getStringParameter("inpadClientId");
    String Accdate = vars.getStringParameter("inpaccountingDate");
    String accGredate = "", budgetReferenceId = null;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    Date endDate = null;
    if (!inpLastFieldChanged.equals("inpadOrgId")) {
      String inpDate = vars.getStringParameter(inpLastFieldChanged);
      String[] inpd = inpDate.split("-");
      String EnterDate = inpd[2] + inpd[1] + inpd[0];

      boolean check = Utility.HijriDateValidion(inpDate);

      if (check == false) {
        PreparedStatement st = null;
        ResultSet rs = null;
        String Maxhijridate = null;
        String Minhijridate = null;

        try {
          st = OBDal.getInstance().getConnection()
              .prepareStatement("select max(hijri_date) as maxdate from eut_hijri_dates");
          rs = st.executeQuery();
          if (rs.next()) {
            Maxhijridate = rs.getString("maxdate");

          }
          st = OBDal.getInstance().getConnection()
              .prepareStatement("select min(hijri_date) as mindate from eut_hijri_dates");
          rs = st.executeQuery();
          if (rs.next()) {
            Minhijridate = rs.getString("mindate");

          }
          // check entered date is greater than maximum hijriDate then update maximum hijriDate
          if (Integer.parseInt(EnterDate) > Integer.parseInt(Maxhijridate)) {
            info.addResult(inpLastFieldChanged, Utility.MaxHijriDate());

          }

          // check entered date is less than minimum hijriDate then update minimum hijriDate
          if (Integer.parseInt(EnterDate) < Integer.parseInt(Minhijridate)) {
            info.addResult(inpLastFieldChanged, Utility.MinHijriDate());
          }

        } catch (final Exception e) {
          log4j.error("Exception in  hijiridate: ", e);
        } finally {
          // close connection
          try {
            if (rs != null) {
              rs.close();
            }
            if (st != null) {
              st.close();
            }
          } catch (Exception e) {
            log4j.error("Exception in closing connection : ", e);
          }
        }
      }
    }
    // load accounting date same as transaction date ( edit on 26/10/2016) and trx period load based
    // on trx date
    if (inpLastFieldChanged.equals("inptrxdate") || inpLastFieldChanged.equals("inpadOrgId")) {
      try {
        info.addResult("inpaccountingDate", vars.getStringParameter("inptrxdate"));
        String gregorianmonth = HijiridateDAO.getGregorianPeriod(trxdate);
        info.addResult("inptransactionperiod", gregorianmonth);

        accGredate = UtilityDAO.convertToGregorian(Accdate);
        endDate = dateFormat.parse(accGredate);
        budgetReferenceId = BudgetAdjustmentCallout.getBudgetDefinitionForStartDate(endDate,
            clientId, "");
        if (budgetReferenceId != null) {
          info.addResult("inpefinBudgetintId", budgetReferenceId);
        } else {
          info.addResult("inpefinBudgetintId", null);
        }

      } catch (Exception e) {
        log4j.info("Process failed populating accounting date from transaction date");
      }
    }

  }
}
