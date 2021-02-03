package sa.elm.ob.finance.ad_callouts;

import java.util.List;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.ESCM_Certificates;

public class CertificateNumber extends SimpleCallout {

  /**
   * This callout is used to set ID No in Order to Receive Window based on selected ID Type
   */

  Logger log = Logger.getLogger(CertificateNumber.class);
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpemEfinCustomeridtype = vars.getStringParameter("inpemEfinCustomeridtype");
    String inpcBpartnerId = vars.getStringParameter("inpcBpartnerId");
    try {
      OBContext.setAdminMode();
      log.debug("LastChanged:" + inpemEfinCustomeridtype);

      if (inpLastFieldChanged.equals("inpemEfinCustomeridtype")) {

        OBQuery<ESCM_Certificates> certificates = OBDal.getInstance().createQuery(
            ESCM_Certificates.class,
            "id = :certificateID and businessPartner.id = :businessPartner");
        certificates.setNamedParameter("certificateID", inpemEfinCustomeridtype);
        certificates.setNamedParameter("businessPartner", inpcBpartnerId);
        log.debug("inpemEfinCustomeridtype:" + inpemEfinCustomeridtype);
        log.debug("inpcBpartnerId:" + inpcBpartnerId);

        List<ESCM_Certificates> certificatesList = certificates.list();

        if (!certificatesList.isEmpty()) {
          info.addResult("inpemEfinIdno", certificatesList.get(0).getCertificateNumber());
        } else {
          info.addResult("inpemEfinIdno", "");
        }

      }

    } catch (Exception e) {
      log.debug("Exception in CertificateNumber Callout:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}