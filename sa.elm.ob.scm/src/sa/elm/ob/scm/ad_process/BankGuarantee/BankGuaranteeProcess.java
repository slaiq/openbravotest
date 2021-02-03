package sa.elm.ob.scm.ad_process.BankGuarantee;

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

import sa.elm.ob.scm.ESCMBGConfiscation;
import sa.elm.ob.scm.ESCMBGExtension;
import sa.elm.ob.scm.ESCMBGRelease;

/**
 * 
 * @author qualian
 * 
 */
public class BankGuaranteeProcess extends DalBaseProcess {

  /**
   * This servlet class was responsible for Annoucments Process
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(BankGuaranteeProcess.class);

  // private final OBError obError = new OBError();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    boolean errorFlag = false;

    try {
      OBContext.setAdminMode();
      // bg extension
      if (bundle.getParams().get("Escm_Bg_Extension_ID") != null) {
        final String bgExtensionId = (String) bundle.getParams().get("Escm_Bg_Extension_ID")
            .toString();
        ESCMBGExtension bgExension = OBDal.getInstance().get(ESCMBGExtension.class, bgExtensionId);

        BankGuaranteeProcessDAO.updateBGStatus(bgExension.getEscmBankguaranteeDetail(),
            bgExension.getBankLetterRef(), "EXT", vars.getUser(), bgExtensionId, null);

      }
      // bg release
      if (bundle.getParams().get("Escm_Bg_Release_ID") != null) {
        final String bgReleaseId = (String) bundle.getParams().get("Escm_Bg_Release_ID").toString();
        ESCMBGRelease bgrelRelease = OBDal.getInstance().get(ESCMBGRelease.class, bgReleaseId);

        BankGuaranteeProcessDAO.updateBGStatus(bgrelRelease.getEscmBankguaranteeDetail(),
            bgrelRelease.getBankLetterReference(), "REL", vars.getUser(), bgReleaseId, null);

      }

      // bg confiscat
      if (bundle.getParams().get("Escm_Bg_Confiscation_ID") != null) {
        final String bgConfiscatedId = (String) bundle.getParams().get("Escm_Bg_Confiscation_ID")
            .toString();
        ESCMBGConfiscation bgConfiscate = OBDal.getInstance().get(ESCMBGConfiscation.class,
            bgConfiscatedId);

        BankGuaranteeProcessDAO.updateBGStatus(bgConfiscate.getEscmBankguaranteeDetail(),
            bgConfiscate.getBankLetterReference(), "CON", vars.getUser(), bgConfiscatedId, null);
      }
      if (!errorFlag) {
        OBDal.getInstance().flush();
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
        bundle.setResult(result);
        return;
      }

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in BankGuaranteeProcess Submit:" + e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}