package sa.elm.ob.scm.ad_process.ProposalManagement;

import java.math.BigDecimal;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmProposalMgmt;

/**
 * @author Kiruthika on 16/06/2020
 */

// Approval Flow of Proposal Management
public class UnifiedProposalRevokeMethods {
  private static final Logger log = LoggerFactory.getLogger(UnifiedProposalRevokeMethods.class);

  public static OBError proposalRevokeValidation(String propId, String clientId, String orgId,
      String userId, String roleId, String tabId) throws Exception {

    try {
      OBContext.setAdminMode();

      EscmProposalMgmt headerCheck = OBDal.getInstance().get(EscmProposalMgmt.class, propId);

      if (headerCheck.getProposalstatus().equals("CD")
          || headerCheck.getProposalstatus().equals("DR")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        return result;
      }

      if (headerCheck.getProposalappstatus().equals("REJ")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
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
  public static boolean updateUnifiedProposalRevoke(EscmProposalMgmt header) {
    try {
      OBContext.setAdminMode();

      header.setUpdated(new java.util.Date());
      header.setUpdatedBy(OBContext.getOBContext().getUser());
      if (header.isNeedEvaluation())
        if (header.getAwardamount().compareTo(BigDecimal.ZERO) != 0) {
          header.setProposalstatus("PAWD");
        } else {
          header.setProposalstatus("AWD");
        }
      else
        header.setProposalstatus("DR");
      if (header.getEscmBaseproposal() == null)
        header.setProposalappstatus("INC");
      else
        header.setProposalappstatus("REA");
      header.setEscmUnifiedProposalAction("SA");
      header.setEUTNextRole(null);
      OBDal.getInstance().save(header);
      OBDal.getInstance().flush();
      return true;
    } catch (final Exception e) {
      log.error("Exception in updateUnifiedProposalRevoke : ", e);
      return false;
    } finally {
      // OBContext.restorePreviousMode();
    }
  }

}
