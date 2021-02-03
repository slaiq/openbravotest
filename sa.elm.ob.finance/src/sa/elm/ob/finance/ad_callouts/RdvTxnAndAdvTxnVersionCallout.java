package sa.elm.ob.finance.ad_callouts;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author DivyaPrakash JS
 */

public class RdvTxnAndAdvTxnVersionCallout extends SimpleCallout {

  /**
   * Callout to update the version date (greg)
   */
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(RdvTxnAndAdvTxnVersionCallout.class);

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String versionDate = vars.getStringParameter("inptxnverDate");
    String versionDateGreg = vars.getStringParameter("inptxnverDateGreg");

    try {
      if (inpLastFieldChanged.equals("inptxnverDate")) {
        if (!StringUtils.isNotEmpty(versionDate)) {
          info.addResult("inptxndategre", "");
        }
        // To check versionDate date is valid date format or not
        if (versionDate.matches("([0-9]{2})-([0-9]{2})-([0-9]{4})")
            && UtilityDAO.Checkhijridate(versionDate)) {
          String txnGreg = UtilityDAO.convertToGregorian_tochar(versionDate);
          info.addResult("inptxnverDateGreg", txnGreg);
        } else {
          info.addResult("inptxnverDateGreg", "");
          info.addResult("ERROR", OBMessageUtils.messageBD("Efin_invalidversiondategreg"));
        }

      }
      if (inpLastFieldChanged.equals("inptxnverDateGreg")) {
        String day, month, year;
        if (!StringUtils.isNotEmpty(versionDateGreg)) {
          info.addResult("inptxnverDate", "");
        }
        // To check versionDateGreg is valid date format or not
        if (versionDateGreg.matches("([0-9]{2})-([0-9]{2})-([0-9]{4})")) {
          String[] hijiriDate = versionDateGreg.split("-");// splits the string based on "-"
          day = hijiriDate[0];
          month = hijiriDate[1];
          year = hijiriDate[2];
          if (UtilityDAO.isGregorianDateValid(day, month, year)) {
            versionDateGreg = year + "-" + month + "-" + day;
            String txnHijiri = UtilityDAO.convertTohijriDate(versionDateGreg);
            info.addResult("inptxnverDate", txnHijiri);
          } else {
            info.addResult("inptxnverDate", "");
            info.addResult("ERROR", OBMessageUtils.messageBD("Efin_invalidversiondategreg"));
          }

        } else {
          info.addResult("inptxnverDate", "");
          info.addResult("ERROR", OBMessageUtils.messageBD("Efin_invalidversiondategreg"));
        }

      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in RdvTxnAndAdvTxnVersionCallout :  " + e, e);
      }
    }

  }

}
