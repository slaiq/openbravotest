package sa.elm.ob.scm.ad_process.ProposalManagement;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmProposalMgmt;

/**
 * @author Kiruthika 26/06/2020
 */

// Approval Flow of Proposal Management
public class UnifiedProposalReactivateMethods {
  private static final Logger log = LoggerFactory.getLogger(UnifiedProposalReactivateMethods.class);

  public static OBError proposalReactivateValidation(String proposalId, String clientId,
      String orgId, String userId, String roleId, String tabId) throws Exception {
    // TODO Auto-generated method stub

    try {
      OBContext.setAdminMode();

      // Variable declaration
      EscmProposalMgmt proposalmgmt = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);

      if (proposalmgmt.getOrderEMEscmProposalmgmtIDList().size() > 0) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_Proposalhasorder@");
        return result;
      }

      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
      return result;
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while UnifiedProposalRejectMethods: " + e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return result;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in UnifiedProposalRejectMethods:" + e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return error;

    } finally {
      // OBContext.restorePreviousMode();
    }
  }

  /**
   * update the proposal management header while reject
   * 
   * @param proposalmgmt
   * @return
   */
  // update proposal header status based on Reject
  public static boolean updateUnifiedProposalReactivate(EscmProposalMgmt proposalmgmt) {
    try {
      OBContext.setAdminMode();
      proposalmgmt.setUpdated(new java.util.Date());
      proposalmgmt.setUpdatedBy(OBContext.getOBContext().getUser());
      proposalmgmt.setProposalappstatus("INC");
      proposalmgmt.setEscmUnifiedProposalAction("SA");
      proposalmgmt.setProposalstatus("PAWD");
      proposalmgmt.setEUTNextRole(null);
      return true;
    } catch (final Exception e) {
      log.error("Exception in updateproposalmanagementheaderforRevert: ", e);
      return false;
    } finally {
      // OBContext.restorePreviousMode();
    }
  }

}
