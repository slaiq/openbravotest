package sa.elm.ob.scm.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import sa.elm.ob.scm.EscmInsuranceCertificate;
import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

public class InsuranceCertWrkBnch extends IRTabIconVariables {
  Logger log = Logger.getLogger(InsuranceCertWrkBnch.class);

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {
      OBContext.setAdminMode(true);
      final String recordId = jsonData.getString("recordId");

      /* Insurance Certificate Workbench-Extention, Release */
      if (!recordId.equals("")) {
        EscmInsuranceCertificate insCertificate = OBDal.getInstance().get(
            EscmInsuranceCertificate.class, recordId);
        if ((insCertificate.getStatus() != null && insCertificate.getStatus().equals("REL"))
            || (insCertificate.getStatus() != null && insCertificate.getStatus().equals("EXP"))) {
          enable = 1;
        }
      }
    } catch (Exception e) {
      log.error("Exception in getIconVariables(): " + e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
