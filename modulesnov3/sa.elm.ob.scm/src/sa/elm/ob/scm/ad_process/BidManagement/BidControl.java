package sa.elm.ob.scm.ad_process.BidManagement;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;
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

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.ad_process.BidManagement.dao.BidManagementDAO;
import sa.elm.ob.utility.tabadul.AttachmentVO;
import sa.elm.ob.utility.tabadul.TabadulAuthenticationService;
import sa.elm.ob.utility.tabadul.TabadulAuthenticationServiceImpl;
import sa.elm.ob.utility.tabadul.TabadulIntegrationDAO;
import sa.elm.ob.utility.tabadul.TabadulIntegrationDAOImpl;
import sa.elm.ob.utility.tabadul.TabadulIntegrationService;
import sa.elm.ob.utility.tabadul.TabadulIntegrationServiceImpl;
import sa.elm.ob.utility.tabadul.TenderStatusE;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Utility;

public class BidControl extends DalBaseProcess {

  /**
   * This servlet class was responsible for Bid Control Action.
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(BidControl.class);
  private static String BID_MGMT_TABLE_ID = "9500BBBFB8584B3783C1C9B9492FD7FE";
  private static String TABADUL_ERROR = "ESCM_TABADUL_CANCEL_ERROR";
  private static String EXTEND_TENDER_OPENENV_ERROR_MSG = "ESCM_EXTD_TR_OPENENV_ERROR_MSG";

  // private final OBError obError = new OBError();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    try {
      OBContext.setAdminMode();
      final String bidId = (String) bundle.getParams().get("Escm_Bidmgmt_ID").toString();
      String bidStatus = (String) bundle.getParams().get("bidstatus").toString();
      final String Comments = (String) bundle.getParams().get("comments").toString();
      EscmBidMgmt header = Utility.getObject(EscmBidMgmt.class, bidId);
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = header.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      Boolean errorFlag = false;
      boolean isTabadulError = false;
      JSONObject resultEncum = null;
      EfinBudgetManencum encumbrance = null;
      // Task No.5925
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();

      // check pr encumbrance type is enable or not .. Task No.5925
      enccontrollist = BidManagementDAO.getPREncumTypeList(clientId);

      if (bidStatus != null) {
        List<EscmProposalMgmt> proposal = BidManagementDAO.checkProposalStatus(header.getId());
        if (proposal.size() > 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_PROCreated@");
          bundle.setResult(result);
          return;
        }
      }

      // should not allow to resubmit the bid, once it is used in proposal
      if (bidStatus.equals(BidStatusE.RESUBMITTED.getStatus())) {
        List<EscmProposalMgmt> proposal = BidManagementDAO.checkProposalCreated(header.getId());
        if (proposal.size() > 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_PROCreatedCantResubmit@");
          bundle.setResult(result);
          return;
        }
      }

      if (enccontrollist.size() > 0) {
        if (!bidStatus.equals("CD") && header.getBidstatus().equals("ACT")) {
          if (header.getEncumbrance() != null) {
            encumbrance = header.getEncumbrance();

            errorFlag = BidManagementDAO.chkManualEncumbranceRejValid(header);
            if (errorFlag) {
              OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                  "@Efin_Encum_Used_Cannot_Canl@");
              bundle.setResult(result1);
              return;
            }

            // reactivate the merge and splitencumbrance
            resultEncum = BidManagementDAO.checkFullPRQtyUitlizeorNot(header);

            // if full qty only used then remove the encumbrance reference and change the
            // encumencumbrance stage as PR Stage
            if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                && resultEncum.getBoolean("isAssociatePREncumbrance")
                && resultEncum.has("isFullQtyUsed") && resultEncum.getBoolean("isFullQtyUsed")) {
              errorFlag = BidManagementDAO.chkFundsAvailforReactOldEncumbrance(header, null, null);
              log.debug("errorFlag:" + errorFlag);
              if (errorFlag) {
                OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                    "@ESCM_ProcessFailed(Reason)@");
                bundle.setResult(result1);
                return;
              } else {

                BidManagementDAO.reactivateStageUniqueCodeChg(resultEncum, header, bidStatus);
                encumbrance.setEncumStage("PRE");

                if (!bidStatus.equals("CL")) {
                  header.setEfinIsbudgetcntlapp(false);
                  header.setEncumbrance(null);
                }
              }
            } else {

              errorFlag = BidManagementDAO.chkFundsAvailforReactOldEncumbrance(header, null, null);
              if (errorFlag) {
                OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                    "@ESCM_ProcessFailed(Reason)@");
                bundle.setResult(result1);
                return;
              } else {
                if (resultEncum.has("type") && resultEncum.getString("type").equals("SPLIT")) {
                  BidManagementDAO.reactivateSplitPR(resultEncum, header, bidStatus);
                }
                if (resultEncum.has("type") && resultEncum.getString("type").equals("MERGE")) {
                  BidManagementDAO.reactivateSplitPR(resultEncum, header, bidStatus);
                }

                // Task No.7749 : Note 20767 - Set encumbrance stage as PR after canceling bid
                BidManagementDAO.setEncumStagePR(header);
              }
            }
          }

          if (bidStatus.equals("CL")) {
            BidManagementDAO.updatePRQtyCancelBid(header);
          }
        }

        // other than inactive status
        else {
          if (header.getEncumbrance() != null && header.getBidstatus().equals("ACT")
              && (bidStatus.equals("CD"))) {
            encumbrance = header.getEncumbrance();
            // reactivate the merge and splitencumbrance
            resultEncum = BidManagementDAO.checkFullPRQtyUitlizeorNot(header);
            // if full qty only used then remove the encumbrance reference and change the
            // encumencumbrance stage as PR Stage
            // amount reduced
            if (resultEncum != null) {
              for (EfinBudgetManencumlines lines : encumbrance.getEfinBudgetManencumlinesList()) {
                BidManagementDAO.insertEncumbranceModification(lines, lines.getRevamount().negate(),
                    null, "BIDCAN", null, null);
                lines.setAPPAmt(BigDecimal.ZERO);
                OBDal.getInstance().save(lines);
              }
            }
          }
        }
      }
      // End Task No.5925

      boolean isBidEnvelopedOpened = false;
      try {
        if (bidStatus.equals(BidStatusE.CANCELLED.getStatus())
            || bidStatus.equals(BidStatusE.POSTPONED.getStatus())
            || bidStatus.equals(BidStatusE.CLOSED.getStatus())
            || bidStatus.equals(BidStatusE.RESUBMITTED.getStatus())
            || bidStatus.equals(BidStatusE.WITHDRAWN.getStatus())) {

          // Check here that the open evnvelope completed or not
          if (BidManagementDAO.isOpenEnvelopeCompleted(bidId)) {
            isBidEnvelopedOpened = true;
          }
          cancelTender(bidId);
        } else if (bidStatus.equals(BidStatusE.EXTEND.getStatus())) {
          // extendTenderDates(bidManagementId);
        }
      } catch (Exception e) {
        isTabadulError = true;
        log.debug("Exeception in Bid Control popup process:" + e);
      }
      if (bidStatus.equals("CL") && (header.getBidtype().equals("TR"))) {
        if (BidManagementDAO.getSalesVoucher(bidId)) {
          errorFlag = true;
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_Bid_SalesVoucherCreated@");
          bundle.setResult(result);
          return;
        }
        if (BidManagementDAO.getAnnouncements(bidId)) {
          errorFlag = true;
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_Bid_AnnouncementCreated@");
          bundle.setResult(result);
          return;
        }
      }

      header.setBidstatus(bidStatus);
      // If withdrawn, postpone, resubmit go to approval
      if (bidStatus.equals("WD") || bidStatus.equals("PP") || bidStatus.equals("RES")) {
        header.setBidappstatus("ESCM_RA");
        header.setEscmDocaction("CO");
      } else if (bidStatus.equals("CL") || bidStatus.equals("CD")) {
        header.setBidappstatus("ESCM_AP");
      }
      if (bidStatus.equals("IA")) {
        if (BidManagementDAO.getSalesVoucher(bidId)) {
          errorFlag = true;
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Bid_CannotInact@");
          bundle.setResult(result);
          return;
        }
      }

      if (!errorFlag && bidStatus.equals("IA")) {
        header.setBidstatus(bidStatus);
        header.setBidappstatus("DR");
        header.setEscmDocaction("CO");
      }

      if (!bidStatus.equals("IA")) {
        header.setBidstatus(bidStatus);

      }
      OBDal.getInstance().save(header);
      JSONObject historyData = new JSONObject();
      historyData.put("ClientId", clientId);
      historyData.put("OrgId", orgId);
      historyData.put("RoleId", roleId);
      historyData.put("UserId", userId);
      historyData.put("HeaderId", bidId);
      historyData.put("Comments", Comments);
      if (bidStatus.equals("CL"))
        bidStatus = "CA";
      historyData.put("Status", bidStatus);
      historyData.put("NextApprover", "");
      historyData.put("HistoryTable", ApprovalTables.Bid_Management_History);
      historyData.put("HeaderColumn", ApprovalTables.Bid_Management_History_HEADER_COLUMN);
      historyData.put("ActionColumn", ApprovalTables.Bid_Management_History_DOCACTION_COLUMN);

      Utility.InsertApprovalHistory(historyData);
      OBDal.getInstance().flush();
      OBError result = null;
      if (isTabadulError) {
        result = OBErrorBuilder.buildMessage(null, "warning", "@" + TABADUL_ERROR + "@");
        result.setMessage(result.getMessage());
      } else if (isBidEnvelopedOpened) {
        OBDal.getInstance().rollbackAndClose();
        result = OBErrorBuilder.buildMessage(null, "error",
            "@" + EXTEND_TENDER_OPENENV_ERROR_MSG + "@");
        result.setMessage(result.getMessage());
      } else {
        result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
      }
      bundle.setResult(result);
      return;

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exeception in Bid Control popup process:", e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /**
   * Cancel the tender
   * 
   * @param bidManagementId
   * @throws Exception
   */
  private void cancelTender(String bidManagementId) throws Exception {
    TabadulIntegrationService tabadulIntegrationService = new TabadulIntegrationServiceImpl();
    TabadulAuthenticationService tabadulAuthenticationService = new TabadulAuthenticationServiceImpl();
    TabadulIntegrationDAO tabadulIntegrationDAO = new TabadulIntegrationDAOImpl();

    String tenderId = tabadulIntegrationDAO.getTenderIdByBidManagementId(bidManagementId);

    if (null == tenderId) {
      return;
    }

    // Update the tabadul bid no and status to DR
    tabadulIntegrationDAO.updateTenderIdInBid(null, TenderStatusE.DRAFT.getStatus(),
        bidManagementId);
    // Get all attachments for Tender
    List<AttachmentVO> attachmentList = tabadulIntegrationDAO.getAttachments(bidManagementId,
        BID_MGMT_TABLE_ID, null);
    for (AttachmentVO attachmentVO : attachmentList) {
      tabadulIntegrationDAO.updateTabadulFileStatus(TenderStatusE.DRAFT.getStatus(), null,
          attachmentVO.getTabadulAttachmentId());
    }
    String sessionToken = tabadulAuthenticationService.authenticateAuditUser();
    tabadulIntegrationService.cancelTender(tenderId, sessionToken);

  }

  /* *//**
        * Extend the tender dates
        * 
        * @param bidManagementId
        * @throws Exception
        */
  /*
   * private void extendTenderDates(String bidManagementId) throws Exception {
   * TabadulIntegrationService tabadulIntegrationService = new TabadulIntegrationServiceImpl();
   * TabadulAuthenticationService tabadulAuthenticationService = new
   * TabadulAuthenticationServiceImpl();
   * 
   * TabadulIntegrationDAO tabadulIntegrationDAO = new TabadulIntegrationDAOImpl(); // Get the
   * session token String sessionToken = tabadulAuthenticationService.authenticateAuditUser(); //
   * Get the tender id TenderVO tenderVO =
   * tabadulIntegrationDAO.getTenderInformation(bidManagementId, null); // Extend the tender dates
   * tabadulIntegrationService.extendTenderDates(tenderVO, sessionToken);
   * 
   * }
   */
}