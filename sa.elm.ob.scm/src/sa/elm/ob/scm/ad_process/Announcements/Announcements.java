package sa.elm.ob.scm.ad_process.Announcements;

import javax.servlet.http.HttpServletRequest;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.Escmannoucements;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Divya on 07/06/2017
 */

public class Announcements extends DalBaseProcess {

  /**
   * This servlet class was responsible for Annoucments Process
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(Announcements.class);

  // private final OBError obError = new OBError();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    // Connection conn = OBDal.getInstance().getConnection();
    // ResultSet rs = null;
    boolean errorFlag = false, isPeriodOpen = true;

    try {
      OBContext.setAdminMode();
      final String annId = (String) bundle.getParams().get("Escm_Annoucements_ID").toString();
      Escmannoucements annoucments = OBDal.getInstance().get(Escmannoucements.class, annId);
      // final String clientId = (String) bundle.getContext().getClient();
      // final String orgId = annoucments.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      if (annoucments.getAlertStatus().equals("DR")) {
        // Check transaction period is opened or not
        isPeriodOpen = Utility.checkOpenPeriod(annoucments.getAnnoucedate(),
            annoucments.getOrganization().getId(), vars.getClient());
        if (!isPeriodOpen) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
          bundle.setResult(result);
          return;
        }
        if (annoucments.getAlertStatus().equals("CO")) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }
        if (annoucments.getESCMAnnouSummaryMediaList().size() == 0) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_AnnoMedia_OneLine@");
          bundle.setResult(result);
          return;
        }
        if (annoucments.getESCMAnnouSummaryBidList().size() == 0) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_AnnoBids_OneLine@");
          bundle.setResult(result);
          return;
        }
        if (!errorFlag) {
          annoucments.setUpdated(new java.util.Date());
          annoucments.setUpdatedBy(OBDal.getInstance().get(User.class, userId));
          annoucments.setAlertStatus("CO");
          annoucments.setAnnaction("RE");
          OBDal.getInstance().save(annoucments);
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          return;
        }
      }

      else if (annoucments.getAlertStatus().equals("CO")) {
        if (annoucments.getAlertStatus().equals("DR")) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }
        if ((annoucments.getESCMProposalEvlEventList().size() > 0)
            || (annoucments.getEscmOpenenvcommiteeEscmAnnouncementIDList().size() > 0)
            || (annoucments.getEscmTechnicalevlEventList() != null
                && annoucments.getEscmTechnicalevlEventList().size() > 0)) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Announce_CantReact@");
          bundle.setResult(result);
          return;
        }
        annoucments.setUpdated(new java.util.Date());
        annoucments.setUpdatedBy(OBDal.getInstance().get(User.class, userId));
        annoucments.setAlertStatus("DR");
        annoucments.setAnnaction("CO");
        OBDal.getInstance().save(annoucments);
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
        bundle.setResult(result);
        return;
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exeception in Annoucment Submit:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}