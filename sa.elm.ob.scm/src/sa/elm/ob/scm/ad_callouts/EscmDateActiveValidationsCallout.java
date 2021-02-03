package sa.elm.ob.scm.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

import sa.elm.ob.scm.ad_callouts.dao.EscmDateActiveValidationsCalloutDAO;

/**
 * @author Priyanka Ranjan on 06/03/2018
 */
// this CallOut is for common validations of start date,end date and active validations
public class EscmDateActiveValidationsCallout extends SimpleCallout {

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;

    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpisactive = vars.getStringParameter("inpisactive");
    String inpenddate = vars.getStringParameter("inpenddate");

    try {
      OBContext.setAdminMode();
      // update the "End Date" by session date if we uncheck the active flag
      if (inpLastFieldChanged.equals("inpisactive")) {
        if (inpisactive.equals("N")) {
          String date = EscmDateActiveValidationsCalloutDAO.getCurrentHijriDate();
          if (!date.isEmpty()) {
            info.addResult("inpenddate", date);
          }
        } else {
          info.addResult("inpenddate", null);
        }
      }
      // while changing end date set active flag with 'Y' or 'N'
      if (inpLastFieldChanged.equals("inpenddate")) {
        if (inpenddate == null || inpenddate.equals("")) {
          info.addResult("inpisactive", true);
        } else {
          info.addResult("inpisactive", false);
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in EscmDateActiveValidationsCallout :", e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
