package sa.elm.ob.scm.ad_callouts;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;

/**
 * 
 * @author Mouli K
 *
 */

public class POConfigurationCallout extends SimpleCallout {

  private static Logger log = Logger.getLogger(POConfigurationCallout.class);

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpcBpartnerId = vars.getStringParameter("inpcBpartnerId");
    BusinessPartner businessPartner = OBDal.getInstance().get(BusinessPartner.class,
        inpcBpartnerId);

    try {

      OBContext.setAdminMode();

      // To get the Position for corresponding Business Partner
      if (inpLastFieldChanged.equals("inpcBpartnerId")) {
        if (businessPartner != null)
          info.addResult("inpmotConPos", businessPartner.getEhcmPosition());
        else
          info.addResult("inpmotConPos", "");
      }

    } catch (Exception e) {
      log.error("Exception in POConfigurationCallout:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
