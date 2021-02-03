package sa.elm.ob.scm.ad_process.InsuranceCertificate;

import javax.servlet.http.HttpServletRequest;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmICRelease;

/**
 * 
 * @author qualian
 * 
 */
public class InsuranceCertificateProcess extends DalBaseProcess {

  /**
   * This servlet class was responsible for Ic Relese Process
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(InsuranceCertificateProcess.class);

  // private final OBError obError = new OBError();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    boolean results = false;

    try {
      OBContext.setAdminMode();

      // IC release
      if (bundle.getParams().get("Escm_Ic_Release_ID") != null) {
        final String icReleaseId = (String) bundle.getParams().get("Escm_Ic_Release_ID").toString();
        EscmICRelease icrelRelease = OBDal.getInstance().get(EscmICRelease.class, icReleaseId);

        results = InsuranceCertificateProcessDAO.updateICStatus(
            icrelRelease.getEscmInsuranceCertificate(), icrelRelease.getInsuranceCLetterReference(),
            "REL", vars.getUser(), icReleaseId, null);
        if (!results) {
          // throw new OBException(OBMessageUtils.messageBD("Escm_No_Letterref"));
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_No_Letterref@");
          bundle.setResult(result);
          return;
        }
      }
      OBDal.getInstance().flush();
      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
      bundle.setResult(result);
      return;

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exeception in ICRelese process:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}