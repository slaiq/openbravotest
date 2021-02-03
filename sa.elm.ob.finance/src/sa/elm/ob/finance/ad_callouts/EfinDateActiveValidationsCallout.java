package sa.elm.ob.finance.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

import sa.elm.ob.finance.ad_callouts.dao.EfinDateActiveValidationsCalloutDAO;

/**
 * @author Priyanka Ranjan on 12/07/2018
 */
// this Callout is for common validations of end date and active validations in finance

public class EfinDateActiveValidationsCallout extends SimpleCallout {

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;

    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpisactive = vars.getStringParameter("inpisactive");
    String enddate = vars.getStringParameter("inpemEfinActiveTo");

    try {
      OBContext.setAdminMode();
      // update the "End Date" by session date if we uncheck the active flag
      if (inpLastFieldChanged.equals("inpisactive")) {
        if (inpisactive.equals("N")) {
          String date = EfinDateActiveValidationsCalloutDAO.getCurrentHijriDate();
          if (!date.isEmpty()) {
            info.addResult("inpemEfinActiveTo", date);
          }
        } else {
          info.addResult("inpemEfinActiveTo", null);
        }
      }
      // while changing end date set active flag with 'Y' or 'N'
      if (inpLastFieldChanged.equals("inpemEfinActiveTo")) {
        if (enddate == null || enddate.equals("")) {
          info.addResult("inpisactive", true);
        } else {
          info.addResult("inpisactive", false);
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in EfinDateActiveValidationsCallout :", e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
