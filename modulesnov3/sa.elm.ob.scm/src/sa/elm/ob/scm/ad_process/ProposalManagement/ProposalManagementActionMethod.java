package sa.elm.ob.scm.ad_process.ProposalManagement;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.finance.util.CommonValidations;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;
import sa.elm.ob.scm.ESCMBGWorkbench;
import sa.elm.ob.scm.ESCMCommRecommendation;
import sa.elm.ob.scm.ESCMProposalEvlEvent;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalRegulation;
import sa.elm.ob.scm.EscmProposalStatusHistory;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalmgmtLineVersion;
import sa.elm.ob.scm.EscmProposalmgmtVersion;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.Escmbankguaranteedetail;
import sa.elm.ob.scm.Escmopenenvcommitee;
import sa.elm.ob.scm.actionHandler.dao.ProposalManagementDAO;
import sa.elm.ob.scm.actionHandler.dao.ProposalManagementDAOImpl;
import sa.elm.ob.scm.ad_process.BidManagement.dao.BidManagementDAO;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Priyanka Ranjan on 16/06/2017
 */

// // Approval Flow methods of Proposal Management
public class ProposalManagementActionMethod {
  private static final Logger log = LoggerFactory.getLogger(ProposalManagementActionMethod.class);
  // tab Id
  private static final String ProposalManagement_LinetabID = "88E026FD2D0446048C80E9D4749AB608";
  private static final String PEE_LinetabID = "FB93C95370E049739F7460E8C60B8B9E";

  // update proposal header status based on next approver
  public static boolean updateproposalmanagementheader(NextRoleByRuleVO nextApproval,
      EscmProposalMgmt proposalmgmt) {
    EutNextRole nextRole = null;
    try {
      OBContext.setAdminMode();

      if (nextApproval.getNextRoleId() == null) {
        proposalmgmt.setUpdated(new java.util.Date());
        proposalmgmt.setUpdatedBy(OBContext.getOBContext().getUser());
        proposalmgmt.setProposalappstatus("APP");
        proposalmgmt.setEscmDocaction("PD");
        // proposalmgmt.setProposalstatus("AWD");
        proposalmgmt.setEUTNextRole(null);

        /*
         * // update the proposal attribute proposal status as AWD if
         * (proposalmgmt.getEscmProposalAttrList().size() > 0) { EscmProposalAttribute prosAttr =
         * proposalmgmt.getEscmProposalAttrList().get(0); prosAttr.setProposalstatus("AWD");
         * OBDal.getInstance().save(prosAttr); }
         */
      } else {
        nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
        proposalmgmt.setUpdated(new java.util.Date());
        proposalmgmt.setUpdatedBy(OBContext.getOBContext().getUser());
        proposalmgmt.setProposalappstatus("INP");
        proposalmgmt.setEscmDocaction("AP");
        proposalmgmt.setEUTNextRole(nextRole);
      }

      return true;
    } catch (final Exception e) {
      log.error("Exception in updateproposalmanagementheader : ", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  // update header status based on condition.
  public static boolean updateproposalstatus(String status, EscmProposalMgmt proposalmgmt) {
    try {
      OBContext.setAdminMode();
      proposalmgmt.setProposalstatus(status);
      if (status.equals("SUB")) {
        proposalmgmt.setEscmDocaction("RE");
      } else if (status.equals("DR")) {
        if (proposalmgmt.getProposalstatus().equals("DR")) {
          if (deleteProposalEventLines(proposalmgmt))
            return true;
          else
            return false;
        }
      }
      OBDal.getInstance().save(proposalmgmt);
      return true;
    } catch (final Exception e) {
      log.error("Exception in updateproposalstatus : ", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static boolean deleteProposalEventLines(EscmProposalMgmt proposalmgmt) {
    String attrId = null;
    ESCMProposalEvlEvent event = null;
    Escmopenenvcommitee openEnvComm = null;
    EscmProposalAttribute attr = null;
    StringBuffer strDelQuery = null;

    try {
      strDelQuery = new StringBuffer();
      OBQuery<EscmProposalAttribute> propAttr = OBDal.getInstance().createQuery(
          EscmProposalAttribute.class, " as e where e.escmProposalmgmt.id=:proposalId");
      propAttr.setNamedParameter("proposalId", proposalmgmt.getId());
      propAttr.setMaxResult(1);
      if (propAttr.list().size() > 0) {
        attr = propAttr.list().get(0);
        attrId = attr.getId();
        if (proposalmgmt.getProposalType().equals("DR"))
          event = OBDal.getInstance().get(ESCMProposalEvlEvent.class,
              attr.getEscmProposalevlEvent().getId());
        else
          openEnvComm = attr.getEscmOpenenvcommitee();
        attr.setRank(null);
        attr.setDiscardedReason(null);
        attr.setProposalstatus(null);
        attr.setPEETechDiscount(new BigDecimal("0"));
        attr.setPEETechDiscountamt(new BigDecimal("0"));
        if (attr.getEscmProposalRegulationList().size() > 0) {
          for (EscmProposalRegulation reg : attr.getEscmProposalRegulationList()) {
            reg.setEscmProposalAttr(null);
          }
        }
      }
      // line updation
      for (EscmProposalmgmtLine line : proposalmgmt.getEscmProposalmgmtLineList()) {
        if ((line.getPEENegotUnitPrice().compareTo(line.getNetprice()) != 0)) {

          if (!line.getEscmProposalmgmt().getProposalType().equals("DR")
              && line.getTechDiscountamt() != null) {
            line.setPEENegotUnitPrice(line.getTechUnitPrice());
          } else {
            line.setPEENegotUnitPrice(line.getGrossUnitPrice());
          }
        }

        line.setPEETechDiscount(new BigDecimal(0));
        line.setPEETechDiscountamt(new BigDecimal(0));
        if (!line.getEscmProposalmgmt().getProposalType().equals("DR")) {
          line.setDiscount(line.getTechDiscount());
          line.setDiscountmount(line.getTechDiscountamt());
        } else {
          line.setDiscount(line.getProposalDiscount());
          line.setDiscountmount(line.getProposalDiscountAmount());
        }
        line.setPeestatus(null);
        if (line.getEscmProposalmgmt().getProposalType().equals("DR")) {
          line.setPEEQty(line.getMovementQuantity());
          line.setPEELineTotal(line.getMovementQuantity().multiply(line.getGrossUnitPrice()));
        } else {
          line.setPEELineTotal(line.getTechLineTotal());
          // after delete proposal in PEE revert qty also in proposal management line
          line.setPEEQty(line.getTechLineQty());
        }
        OBDal.getInstance().flush();
      }
      OBQuery<ESCMBGWorkbench> bgworkbench = OBDal.getInstance().createQuery(ESCMBGWorkbench.class,
          " as e where e.escmProposalAttr.id=:propAttrID ");
      bgworkbench.setNamedParameter("propAttrID", attrId);
      log.debug("listsize:" + bgworkbench.list().size());
      if (bgworkbench.list().size() > 0) {
        for (ESCMBGWorkbench bg : bgworkbench.list()) {
          for (Escmbankguaranteedetail bgdet : bg.getEscmBankguaranteeDetailList()) {
            bgdet.setEscmProposalAttr(null);
            OBDal.getInstance().save(bgdet);
          }
          bg.setEscmProposalAttr(null);
          OBDal.getInstance().save(bg);
        }
        OBDal.getInstance().flush();
      }
      if ((event != null && event.getBidNo() != null)
          || (openEnvComm != null && openEnvComm.getBidNo() != null)) {
        OBQuery<ESCMCommRecommendation> comrecom = OBDal.getInstance().createQuery(
            ESCMCommRecommendation.class,
            " as e where e.escmTechnicalevlEvent.id=( select e.id from escm_technicalevl_event e where e.bidNo.id=:BidId)");
        if (event != null)
          comrecom.setNamedParameter("BidId", event.getBidNo().getId());
        else if (openEnvComm != null)
          comrecom.setNamedParameter("BidId", openEnvComm.getBidNo().getId());
        if (comrecom.list().size() > 0) {
          for (ESCMCommRecommendation com : comrecom.list()) {
            com.setEscmProposalevlEvent(null);
            OBDal.getInstance().save(com);
          }
          OBDal.getInstance().flush();
        }
      }
      // remove the proposal attribute when proposal bid id direct or without bid
      if (event != null && proposalmgmt.getProposalType().equals("DR")) {
        strDelQuery.append(" delete from escm_proposal_attr where escm_proposalevl_event_id='"
            + event.getId() + "' and escm_proposal_attr_id='" + attr.getId() + "'");
        OBDal.getInstance().getSession().createQuery(strDelQuery.toString()).executeUpdate();
      } else if (openEnvComm != null) {
        strDelQuery.append(" delete from escm_proposal_attr where escm_openenvcommitee_id='"
            + openEnvComm.getId() + "' and escm_proposal_attr_id='" + attr.getId() + "'");
        OBDal.getInstance().getSession().createQuery(strDelQuery.toString()).executeUpdate();
        if (openEnvComm.getEscmProposalAttrList().size() == 0) {
          openEnvComm.setDeleteLines(false);
        }
      }
      proposalmgmt.setEscmDocaction("CO");
    } catch (final Exception e) {
      log.error("Exception in deleteProposalEventLines : ", e);
      return false;
    }
    return true;
  }

  // take a backup of current record to allow create new version.
  public static boolean proposalmgmtbackup(VariablesSecureApp vars, EscmProposalMgmt proposalmgmt) {
    try {
      OBContext.setAdminMode();
      List<EscmProposalmgmtLine> lineList = new ArrayList<EscmProposalmgmtLine>();
      // // update header version.
      // proposalmgmt.setVersionNo(proposalmgmt.getVersionNo() + 1);
      // proposalmgmt.setProposalappstatus("REA");
      // proposalmgmt.setEscmDocaction("SA");
      // proposalmgmt.setVersion(true);
      // OBDal.getInstance().save(proposalmgmt);

      // insert header in backup table to keep old versions.
      EscmProposalmgmtVersion header = OBProvider.getInstance().get(EscmProposalmgmtVersion.class);
      header.setEscmProposalmgmt(proposalmgmt);
      header.setBuyername(proposalmgmt.getBuyername());
      header.setEffectivefrom(proposalmgmt.getEffectivefrom());
      header.setEffectiveto(proposalmgmt.getEffectiveto());
      header.setRank(proposalmgmt.getRank());
      header.setNotes(proposalmgmt.getNotes());
      header.setProposalno(proposalmgmt.getProposalno());
      OBDal.getInstance().save(header);

      // insert lines in backup table to keep old versions.
      lineList = proposalmgmt.getEscmProposalmgmtLineList();
      for (EscmProposalmgmtLine currentLines : lineList) {
        EscmProposalmgmtLineVersion oldlines = OBProvider.getInstance()
            .get(EscmProposalmgmtLineVersion.class);
        oldlines.setEscmProposalmgmtVer(header);
        oldlines.setEscmProposalmgmt(proposalmgmt);
        oldlines.setEscmProposalmgmtLine(currentLines);
        oldlines.setMovementQuantity(currentLines.getMovementQuantity().longValue());
        oldlines.setNegotiatedUnitPrice(currentLines.getNegotUnitPrice());
        oldlines.setComments(currentLines.getComments());
        oldlines.setLineNo(currentLines.getLineNo());
        OBDal.getInstance().save(oldlines);
      }
      return true;
    } catch (final Exception e) {
      log.error("Exception in taking backup of proposal : ", e);
      OBDal.getInstance().rollbackAndClose();
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  // cancel proposal lines
  public static boolean cancelline(String status, EscmProposalMgmt proposalmgmt,
      EscmProposalmgmtLine proposalmgmtline, VariablesSecureApp vars, String tabId) {
    try {
      int count = 0;
      boolean versionStatus = false;
      OBContext.setAdminMode();
      if (tabId.equals(ProposalManagement_LinetabID)) {
        proposalmgmtline.setStatus(status);
      } else if (tabId.equals(PEE_LinetabID)) {
        proposalmgmtline.setPeestatus(status);
      }
      if (proposalmgmtline.isSummary()) {
        cancelChildLines(proposalmgmtline, tabId);
      } else {
        if (proposalmgmtline.getParentLineNo() != null) {
          EscmProposalmgmtLine parent = OBDal.getInstance().get(EscmProposalmgmtLine.class,
              proposalmgmtline.getParentLineNo().getId());
          cancelParentLine(proposalmgmtline, parent, tabId);

        }
      }
      OBDal.getInstance().save(proposalmgmtline);

      int size = proposalmgmt.getEscmProposalmgmtLineList().size();
      List<EscmProposalmgmtLine> lineList = new ArrayList<EscmProposalmgmtLine>();
      lineList = proposalmgmt.getEscmProposalmgmtLineList();
      for (EscmProposalmgmtLine lines : lineList) {
        if ((lines.getStatus() != null && lines.getStatus().equals("CL"))
            || (lines.getPeestatus() != null && lines.getPeestatus().equals("CL"))) {
          count = count + 1;
        }
      }
      if (size == count) {
        if (tabId.equals(ProposalManagement_LinetabID)) {
          proposalmgmt.setProposalstatus("CL");
        }
        // update the proposal attribute proposal status as CL
        if (proposalmgmt.getEscmProposalAttrList().size() > 0) {
          EscmProposalAttribute prosAttr = proposalmgmt.getEscmProposalAttrList().get(0);
          // prosAttr.setProposalstatus("CL");
          OBDal.getInstance().save(prosAttr);
        }
        // inserting action history
        JSONObject historyData = new JSONObject();
        historyData.put("ClientId", vars.getClient());
        historyData.put("OrgId", proposalmgmt.getOrganization().getId());
        historyData.put("RoleId", vars.getRole());
        historyData.put("UserId", vars.getUser());
        historyData.put("HeaderId", proposalmgmt.getId());
        historyData.put("Comments", "");
        historyData.put("Status", "CA");
        historyData.put("NextApprover", "");
        historyData.put("HistoryTable", ApprovalTables.Proposal_Management_History);
        historyData.put("HeaderColumn", ApprovalTables.Proposal_Management_History_HEADER_COLUMN);
        historyData.put("ActionColumn",
            ApprovalTables.Proposal_Management_History_DOCACTION_COLUMN);
        Utility.InsertApprovalHistory(historyData);
      } else {
        // create new version for cancel line, then it will follow again approval flow.
        if (proposalmgmt.getProposalappstatus().equals("APP")
            && proposalmgmt.getProposalstatus().equals("AWD")) {
          proposalmgmt.setProposalappstatus("REA");
          proposalmgmt.setEscmDocaction("SA");
          versionStatus = proposalmgmtbackup(vars, proposalmgmt);
          if (!versionStatus) {
            return false;
          }
        }
      }
      OBDal.getInstance().save(proposalmgmt);
      return true;
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in cancel proposal lines : ", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Method to cancel the proposal parent lines if the child line is cancelled
   * 
   * @param proposalLine
   * @param parentLine
   * @param tabId
   * @param isDirect
   * @return
   */
  public static void cancelParentLine(EscmProposalmgmtLine proposalLine,
      EscmProposalmgmtLine parentLine, String tabId) {
    String status = "CL";
    try {
      List<EscmProposalmgmtLine> childLineList = new ArrayList<EscmProposalmgmtLine>();
      List<EscmProposalmgmtLine> cancelledLines = new ArrayList<EscmProposalmgmtLine>();

      // getting the child lines of the selected proposal line's parent
      OBQuery<EscmProposalmgmtLine> proposallnParent = OBDal.getInstance().createQuery(
          EscmProposalmgmtLine.class, " as e where e.parentLineNo.id =:parentrecordId");
      proposallnParent.setNamedParameter("parentrecordId", proposalLine.getParentLineNo().getId());
      childLineList = proposallnParent.list();

      if (childLineList.size() > 0) {

        // getting the cancelled child lines
        if (tabId.equals(ProposalManagement_LinetabID)) {
          cancelledLines = childLineList.stream()
              .filter(a -> a.getStatus() != null && a.getStatus().equals("CL"))
              .collect(Collectors.toList());
        } else if (tabId.equals(PEE_LinetabID)) {
          cancelledLines = childLineList.stream()
              .filter(a -> a.getPeestatus() != null && a.getPeestatus().equals("CL"))
              .collect(Collectors.toList());
        }
        // cancel the parent
        if (childLineList.size() != 0 && (cancelledLines.size() == childLineList.size())) {
          if (tabId.equals(ProposalManagement_LinetabID)) {
            parentLine.setStatus(status);
          } else if (tabId.equals(PEE_LinetabID)) {
            parentLine.setPeestatus(status);
          }
          OBDal.getInstance().save(parentLine);
        }

        // if the parent line has a parent then cancel that line also
        if (parentLine.getParentLineNo() != null) {
          EscmProposalmgmtLine grndparentLine = OBDal.getInstance().get(EscmProposalmgmtLine.class,
              parentLine.getParentLineNo().getId());
          cancelParentLine(parentLine, grndparentLine, tabId);
        }
      }

    } catch (Exception e) {
      log.error("Exception while cancelling parent proposal lines : ", e);
      OBDal.getInstance().rollbackAndClose();
    }
  }

  /**
   * Method to cancel the proposal child lines if the parent line is cancelled
   * 
   * @param parentLine
   * @param tabId
   */
  public static void cancelChildLines(EscmProposalmgmtLine parentLine, String tabId) {
    try {
      String status = "CL";
      List<EscmProposalmgmtLine> linechildList = new ArrayList<EscmProposalmgmtLine>();

      // getting the child lines of the selected parent line
      OBQuery<EscmProposalmgmtLine> proposallnchild = OBDal.getInstance().createQuery(
          EscmProposalmgmtLine.class, " as e where e.parentLineNo.id =:parentrecordId ");
      proposallnchild.setNamedParameter("parentrecordId", parentLine.getId());
      linechildList = proposallnchild.list();

      if (linechildList.size() > 0) {
        // cancel the child lines
        for (EscmProposalmgmtLine proln : linechildList) {
          if (tabId.equals(ProposalManagement_LinetabID)) {
            proln.setStatus(status);
          } else if (tabId.equals(PEE_LinetabID)) {
            proln.setPeestatus(status);
          }
          OBDal.getInstance().save(proln);
          // if the child line has its leaves then cancel those lines also
          if (proln.isSummary()) {
            cancelChildLines(proln, tabId);
          }
        }
      }

      // if the parent line has a parent then cancel that line also
      if (parentLine.getParentLineNo() != null) {
        EscmProposalmgmtLine parent = OBDal.getInstance().get(EscmProposalmgmtLine.class,
            parentLine.getParentLineNo().getId());
        cancelParentLine(parentLine, parent, tabId);
      }

    } catch (Exception e) {
      log.error("Exception while cancelling child proposal lines : ", e);
      OBDal.getInstance().rollbackAndClose();
    }
  }

  /**
   * Method to cancel proposal header and its lines
   * 
   * @param status
   * @param proposalmgmt
   * @return
   */
  public static boolean cancelProposal(String status, EscmProposalMgmt proposalmgmt) {
    try {
      OBContext.setAdminMode();
      proposalmgmt.setProposalstatus(status);
      OBDal.getInstance().save(proposalmgmt);
      List<EscmProposalmgmtLine> lineList = new ArrayList<EscmProposalmgmtLine>();
      lineList = proposalmgmt.getEscmProposalmgmtLineList();
      for (EscmProposalmgmtLine lines : lineList) {
        lines.setStatus(status);
        OBDal.getInstance().save(lines);
      }

      return true;
    } catch (final Exception e) {
      log.error("Exception in cancel proposal and its lines : ", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  // get delegation role
  public static String getdelegationrole() {
    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement st = null;
    ResultSet rs1 = null;
    String sql = "";
    String roleid = "";
    Date currentDate = new Date();
    try {
      sql = "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
          + currentDate + "' and to_date >='" + currentDate + "' and document_type='EUT_117'";
      st = con.prepareStatement(sql);
      rs1 = st.executeQuery();
      while (rs1.next()) {
        roleid = rs1.getString("ad_role_id");
      }
      return roleid;
    } catch (Exception e) {
      log.error("Exception in delegation " + e.getMessage());
    } finally {
      // close db connection
      try {
        if (rs1 != null)
          rs1.close();
        if (st != null)
          st.close();
      } catch (Exception e) {
      }
    }
    return roleid;
  }

  // check direct approval
  public static boolean isDirectApproval(String RequestId, String roleId) {
    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    try {
      query = "select count(promgmt.escm_proposalmgmt_id) from escm_proposalmgmt promgmt join eut_next_role rl on "
          + "promgmt.eut_next_role_id = rl.eut_next_role_id "
          + "join eut_next_role_line li on li.eut_next_role_id = rl.eut_next_role_id "
          + "and promgmt.escm_proposalmgmt_id = ? and li.ad_role_id =?";

      ps = con.prepareStatement(query);
      ps.setString(1, RequestId);
      ps.setString(2, roleId);

      rs = ps.executeQuery();

      if (rs.next()) {
        if (rs.getInt("count") > 0)
          return true;
        else
          return false;
      } else
        return false;

    } catch (Exception e) {
      log.error("Exception in isDirectApproval " + e.getMessage());
      return false;
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
        throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      }
    }
  }

  /**
   * update the proposal attribute gross price and net price while cancel the lines only for direct
   * bid / with out bid
   * 
   * @param status
   * @param proposalmgmt
   * @return
   */
  public static boolean updateProsalAtt(String status, EscmProposalMgmt proposalmgmt,
      EscmProposalmgmtLine proposalmgmtline) {
    try {
      OBContext.setAdminMode();
      List<EscmProposalAttribute> proattlist = new ArrayList<EscmProposalAttribute>();
      if (status.equals("CL")) {
        OBQuery<EscmProposalAttribute> prosalattr = OBDal.getInstance().createQuery(
            EscmProposalAttribute.class, " as e where e.escmProposalmgmt.id=:proposalID");
        prosalattr.setNamedParameter("proposalID", proposalmgmt.getId());

        proattlist = prosalattr.list();
        if (proattlist.size() > 0) {
          EscmProposalAttribute prosalAttr = proattlist.get(0);
          prosalAttr
              .setGrossPrice(prosalAttr.getGrossPrice().subtract(proposalmgmtline.getLineTotal()));
          // restricted to update openevl amt in proposalattr in PEE for Direct
          /*
           * prosalAttr
           * .setNetPrice(prosalAttr.getNetPrice().subtract(proposalmgmtline.getLineTotal()));
           */
          prosalAttr.setNegotiatedPrice(
              prosalAttr.getNegotiatedPrice().subtract(proposalmgmtline.getNegotUnitPrice()));
          if (prosalAttr.getGrossPrice().compareTo(BigDecimal.ZERO) == 0) {
            prosalAttr.setGrossPrice(BigDecimal.ONE);
            prosalAttr.setNetPrice(BigDecimal.ONE);
            prosalAttr.setNegotiatedPrice(BigDecimal.ONE);
          }
          OBDal.getInstance().save(prosalAttr);
        }

      }
      return true;
    } catch (final Exception e) {
      log.error("Exception in cancel proposal and its lines : ", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * update proposal attribute negotiated price and PEE discount amount
   * 
   * @param status
   * @param proposalmgmt
   * @param proposalmgmtline
   * @return
   */
  public static boolean updateProsalAttaftercancel(String status, EscmProposalMgmt proposalmgmt,
      EscmProposalmgmtLine proposalmgmtline) {
    try {
      OBContext.setAdminMode();
      List<EscmProposalAttribute> proattlist = new ArrayList<EscmProposalAttribute>();
      List<EscmProposalmgmtLine> prolinelist = new ArrayList<EscmProposalmgmtLine>();
      BigDecimal proplntotalamt = BigDecimal.ZERO;
      BigDecimal disamt = BigDecimal.ZERO;

      if (status.equals("CL")) {
        OBQuery<EscmProposalmgmtLine> prosalline = OBDal.getInstance().createQuery(
            EscmProposalmgmtLine.class,
            " as e where e.escmProposalmgmt.id=:proposalID and e.id !=:proposalLnID ");
        prosalline.setNamedParameter("proposalID", proposalmgmt.getId());
        prosalline.setNamedParameter("proposalLnID", proposalmgmtline.getId());

        log.debug("list :" + prosalline.list().size());
        prolinelist = prosalline.list();
        if (prolinelist.size() > 0) {
          for (EscmProposalmgmtLine proln : prolinelist) {
            if (!proln.isSummary()) {
              if (proln.getPeestatus() == null || !proln.getPeestatus().equals("CL")) {
                proplntotalamt = proplntotalamt.add(proln.getPEELineTotal());
                disamt = disamt.add(proln.getPEETechDiscountamt());
              }
            }
          }
          OBQuery<EscmProposalAttribute> prosalattr = OBDal.getInstance().createQuery(
              EscmProposalAttribute.class, " as e where e.escmProposalmgmt.id=:proposalID ");
          prosalattr.setNamedParameter("proposalID", proposalmgmt.getId());
          log.debug("list :" + prosalattr.list().size());
          proattlist = prosalattr.list();
          if (proattlist.size() > 0) {
            EscmProposalAttribute prosalAttr = proattlist.get(0);
            prosalAttr.setNegotiatedPrice(proplntotalamt);
            prosalAttr.setPEETechDiscountamt(disamt);

            OBDal.getInstance().save(prosalAttr);
          }
        }
      }
      return true;
    } catch (final Exception e) {
      log.error("Exception in cancel proposal and its lines : ", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * delete lines from proposal management.
   * 
   * @param proposalmgmt
   * @return
   */
  public static boolean deletelines(EscmProposalMgmt proposalmgmt) {
    try {
      OBContext.setAdminMode();
      ProposalManagementDAO proposalDAO = new ProposalManagementDAOImpl();
      List<EscmProposalsourceRef> srcrefList = new ArrayList<EscmProposalsourceRef>();
      BigDecimal soucrRef_Qty = BigDecimal.ZERO;
      BigDecimal total_award_qty = BigDecimal.ZERO;

      List<EscmProposalmgmtLine> proposalLine = new ArrayList<EscmProposalmgmtLine>();
      List<EscmProposalRegulation> regulationLine = new ArrayList<EscmProposalRegulation>();
      // copying current object list into local list variable.
      proposalLine = proposalmgmt.getEscmProposalmgmtLineList();
      regulationLine = proposalmgmt.getEscmProposalRegulationList();

      // // iterate lines to delete.
      for (EscmProposalmgmtLine lines : proposalLine) {
        lines.setParentLineNo(null);
        lines.setManual(true);
        OBDal.getInstance().save(lines);

        if (lines.getEscmProposalmgmtLnVerList().size() > 0) {
          for (EscmProposalmgmtLineVersion verlnObj : proposalmgmt.getEscmProposalmgmtLnVerList()) {
            OBDal.getInstance().remove(verlnObj);
          }
        }
      }
      OBDal.getInstance().flush();

      // Deletion of the Proposal Lines handled onTrigger for tender and limited
      // in Proposal Header Table by setting the bid id is null
      // For Direct Proposal with empty bid deleted from process
      if (proposalmgmt.getProposalType().equals("DR") && proposalmgmt.getEscmBidmgmt() == null) {
        proposalmgmt.getEscmProposalmgmtLineList().removeAll(proposalLine);
        for (EscmProposalmgmtLine lines : proposalLine) {
          OBDal.getInstance().remove(lines);
        }
      }
      // // iterate regulation deocument to delete.
      // proposalmgmt.getEscmProposalRegulationList().removeAll(regulationLine);
      // for (EscmProposalRegulation regulation : regulationLine) {
      // OBDal.getInstance().remove(regulation);
      // }
      // remove the bid which is added in header.
      // Task #7867
      proposalmgmt.setEscmBidmgmt(null);
      proposalmgmt.setBidType(null);
      proposalmgmt.setAgencyorg(null);
      proposalmgmt.setProposalType("DR");
      if (!proposalmgmt.isNeedEvaluation()) {
        for (EscmProposalmgmtLine line : proposalmgmt.getEscmProposalmgmtLineList()) {
          srcrefList = proposalDAO.checkLinesAddedFromPR(line.getId());
          if (srcrefList.size() > 0) {
            for (EscmProposalsourceRef srfRef : srcrefList) {
              if (srfRef.getRequisition() != null) {
                RequisitionLine objRequisition = srfRef.getRequisitionLine();
                Requisition obj_requisition = objRequisition.getRequisition();
                String str_docno = obj_requisition.getDocumentNo();
                soucrRef_Qty = srfRef.getReservedQuantity();
                total_award_qty = soucrRef_Qty.subtract(objRequisition.getEscmAwardedQty());
                objRequisition.setEscmAwardedQty(total_award_qty);
                OBDal.getInstance().save(objRequisition);
              }

            }
          }
        }
      }

      OBDal.getInstance().save(proposalmgmt);
      return true;
    } catch (final Exception e) {
      log.error("Exception in delete proposal lines : ", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * 
   * @param Proposalstatus
   * @param Comments
   * @param proposalId
   * @param clientId
   * @param roleId
   * @param userId
   * @return true if process success else false.
   */
  public static boolean changeProposalStatusAndMaintainHistory(String Proposalstatus,
      String Comments, String proposalId, String proposalAttrId, String clientId, String roleId,
      String userId) {
    try {
      OBContext.setAdminMode();
      // changing status based on user action.
      EscmProposalMgmt proposalmgmt = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
      EscmProposalAttribute attr = null;
      proposalmgmt.setProposalstatus(Proposalstatus);
      if (proposalAttrId != null) {
        attr = OBDal.getInstance().get(EscmProposalAttribute.class, proposalAttrId);
      }
      OBDal.getInstance().save(proposalmgmt);
      // inserting action history
      JSONObject historyData = new JSONObject();
      historyData.put("ClientId", clientId);
      historyData.put("OrgId", proposalmgmt.getOrganization().getId());
      historyData.put("RoleId", roleId);
      historyData.put("UserId", userId);
      historyData.put("HeaderId", proposalId);
      historyData.put("Comments", Comments);
      historyData.put("Status", Proposalstatus);
      historyData.put("NextApprover", "");
      historyData.put("HistoryTable", ApprovalTables.Proposal_Management_Status_History);
      historyData.put("HeaderColumn", ApprovalTables.Proposal_Management_History_HEADER_COLUMN);
      historyData.put("ActionColumn", ApprovalTables.Proposal_Management_History_DOCACTION_COLUMN);
      Utility.InsertApprovalHistory(historyData);
      OBDal.getInstance().flush();
      if (attr != null) {
        for (EscmProposalStatusHistory statusHist : proposalmgmt.getEscmProposalStatusHistList()) {
          statusHist.setEscmProposalAttr(attr);
        }
      }
      return true;
    } catch (final Exception e) {
      log.error("Exception in action process and inserting status history: ", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get proposal attribute id for proposal
   * 
   * @param proposalId
   * @return proposalattribute ID
   */
  public static String getAttributeId(String proposalId) {
    try {
      OBContext.setAdminMode();
      String proposalAttrId = "";
      OBQuery<EscmProposalAttribute> attr = OBDal.getInstance()
          .createQuery(EscmProposalAttribute.class, "escmProposalmgmt.id=:proposalID ");
      attr.setNamedParameter("proposalID", proposalId);
      if (attr.list() != null && attr.list().size() > 0) {
        proposalAttrId = attr.list().get(0).getId();
      }
      return proposalAttrId;
    } catch (final Exception e) {
      log.error("Exception in getting proposal attribute id: ", e);
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * 
   * @param proposal_id
   * @return if different department it will return True.
   */
  public static boolean checkSameDept(String proposal_id) {
    try {
      OBContext.setAdminMode();
      String sqlQuery = "select em_escm_department_id from m_requisition req "
          + " join escm_proposalsource_ref ref on ref.m_requisition_id = req.m_requisition_id "
          + " join escm_proposalmgmt_line line on line.escm_proposalmgmt_line_id = ref.escm_proposalmgmt_line_id "
          + " where line.escm_proposalmgmt_id = '" + proposal_id
          + "' group by em_escm_department_id ";
      SQLQuery queryList = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
      if (queryList.list() != null && queryList.list().size() > 1) {
        return true;
      }
    } catch (Exception e) {
      log.error("Exception in check whether dept is same ", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
    return false;
  }

  @SuppressWarnings("rawtypes")
  public static JSONObject getBidDetails(EscmProposalMgmt proposal) {
    JSONObject bidResult = new JSONObject(), json = null, json2 = null, json3 = null,
        UniqueCodejson = new JSONObject(), bidUniqueCodejson = new JSONObject(),
        uniquecodeResult = new JSONObject(), result = new JSONObject();
    JSONArray array = new JSONArray(), uniqueCodeListArray = new JSONArray(),
        bidUniqueCodeArray = new JSONArray();
    boolean allLineEqual = true;
    Boolean sameUniqueCode = false;
    String grtLessFlag = "";
    try {

      String sqlQuery = " select distinct ln.escm_proposalmgmt_line_id as proline,ln.escm_bidmgmt_line_id  as bidline ,  "
          + " bidln.c_validcombination_id as bidvalidcom,bid.efin_budget_manencum_id as encumbrance,  "
          + " ln.em_efin_c_validcombination_id  as provalidcomb,  "
          + " coalesce(src.amt,0) , coalesce((ln.movementqty * ln.negot_unit_price),0) as pronetmat  , "
          + "  abs(coalesce(src.amt,0)-coalesce((ln.movementqty * ln.negot_unit_price),0)) as diff , case when (ln.movementqty * ln.negot_unit_price)  < src.amt  then 'L'   "
          + " when (ln.movementqty * ln.negot_unit_price)  = src.amt then 'E' else 'G' end greatlessflag ,bidln.em_efin_budgmanencumline_id "
          + " from escm_proposalmgmt_line ln  "
          + " join escm_bidmgmt_line bidln on bidln.escm_bidmgmt_line_id= ln.escm_bidmgmt_line_id  "
          + "  join escm_bidmgmt bid on bid.escm_bidmgmt_id= bidln.escm_bidmgmt_id  "
          + " join ( select coalesce(sum(reqln.priceactual* srcref.quantity),0) as amt,escm_bidmgmt_line_id  from   "
          + " escm_bidsourceref srcref   "
          + "  join m_requisitionline reqln on reqln.m_requisitionline_id= srcref.m_requisitionline_id  group by escm_bidmgmt_line_id) src  "
          + "   on src.escm_bidmgmt_line_id= bidln.escm_bidmgmt_line_id  "
          + "where ln.escm_proposalmgmt_id =?  " + "and ln.issummarylevel='N'   ";
      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
      query.setParameter(0, proposal.getId());
      log.debug("strQuery:" + query.toString());
      List queryList = query.list();
      if (queryList != null && queryList.size() > 0) {
        for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
          sameUniqueCode = false;
          Object[] row = (Object[]) iterator.next();
          json = new JSONObject();
          if (row[8] != null) {
            if (!row[8].equals("E")) {
              allLineEqual = false;
            }
            json.put("proLineId", row[0].toString());
            json.put("bidLineId", row[1].toString());
            json.put("bidCombinId", row[2].toString());
            json.put("bidEncumId", row[3].toString());
            json.put("proCombinId", row[4].toString());
            json.put("proNetAmt", row[6].toString());
            json.put("bidNetAmt", row[5].toString());
            json.put("greaterLesserFlag", row[8].toString());
            json.put("bidEncumLineId", row[9].toString());
            array.put(json);
            log.debug("json1:" + json);
            // if (new BigDecimal(row[7].toString()).compareTo(BigDecimal.ZERO) < 0) {
            if (json.getString("proCombinId") != null && json.getString("bidCombinId") != null) {

              // case 1: if lines ( bid lines and proposal lines )unique code same
              if (json.getString("proCombinId").equals(json.getString("bidCombinId"))) {
                grtLessFlag = json.getString("greaterLesserFlag");

                // case a: if both value same(bidline amt & prline amt)
                if (grtLessFlag.equals("E")) {
                  // dont do anything
                } else {

                  // check already proposal unqiuecode is present in uniquecodeList array ( if same
                  // uniquecode present more than one in proposal lines)
                  for (int i = 0; i < uniqueCodeListArray.length(); i++) {
                    json2 = uniqueCodeListArray.getJSONObject(i);
                    if (json2.getString("proUniquecode").equals(json.getString("proCombinId"))) {

                      // if bidline amt < proposal line amt ( add decrease value in json)
                      if (grtLessFlag.equals("L")) {
                        if (json2.get("prodecrease") != null)
                          json2.put("prodecrease", new BigDecimal(json2.getString("prodecrease"))
                              .add(new BigDecimal(row[7].toString())));
                        else
                          json2.put("prodecrease", row[7].toString());
                      }
                      // if bidline amt > proposal line amt ( add decrease value in json)
                      else {
                        if (json2.get("proincrease") != null)
                          json2.put("proincrease", new BigDecimal(json2.getString("proincrease"))
                              .add(new BigDecimal(row[7].toString())));
                        else
                          json2.put("proincrease", row[7].toString());
                      }
                      sameUniqueCode = true;
                      break;
                    }
                  }
                  // if uniquecode not present then added new uniquecode jsonobject
                  if (!sameUniqueCode) {
                    UniqueCodejson = new JSONObject();
                    UniqueCodejson.put("proUniquecode", json.getString("proCombinId"));
                    UniqueCodejson.put("proLineId", json.getString("proLineId"));
                    // if bidline amt < proposal line amt ( add decrease value in json)
                    if (grtLessFlag.equals("L"))
                      UniqueCodejson.put("prodecrease", row[7].toString());
                    // if bidline amt > proposal line amt ( add decrease value in json)
                    else
                      UniqueCodejson.put("proincrease", row[7].toString());
                    uniqueCodeListArray.put(UniqueCodejson);
                  }
                }
              }
              // case 2 : if uniquecode not same in both bid line and proposal line
              else {
                for (int i = 0; i < uniqueCodeListArray.length(); i++) {
                  json2 = uniqueCodeListArray.getJSONObject(i);
                  if (json2.getString("proUniquecode").equals(json.getString("proCombinId"))) {
                    json2.put("proincrease", new BigDecimal(json2.getString("proincrease"))
                        .add(new BigDecimal(json.getString("proNetAmt"))));

                    if (json2.getJSONArray("bidUnqiueCode") != null) {
                      JSONArray bidjson = json2.getJSONArray("bidUnqiueCode");
                      for (int j = 0; j < bidjson.length(); j++) {
                        json3 = bidjson.getJSONObject(j);
                        if (json3.getString("bidUnqiueCode")
                            .equals(json.getString("bidCombinId"))) {
                          json3.put("biddecrease", new BigDecimal(json3.getString("biddecrease"))
                              .add(new BigDecimal(json.getString("bidNetAmt"))));
                        }
                      }
                    }

                    sameUniqueCode = true;
                    break;
                  }
                }
                if (!sameUniqueCode) {
                  UniqueCodejson = new JSONObject();
                  UniqueCodejson.put("proUniquecode", json.getString("proCombinId"));
                  UniqueCodejson.put("proincrease", json.getString("proNetAmt"));
                  UniqueCodejson.put("proLineId", json.getString("proLineId"));
                  bidUniqueCodeArray = new JSONArray();
                  bidUniqueCodejson = new JSONObject();
                  bidUniqueCodejson.put("bidUnqiueCode", json.getString("bidCombinId"));
                  bidUniqueCodejson.put("biddecrease", json.getString("bidNetAmt"));
                  bidUniqueCodejson.put("bidEncumLineId", json.getString("bidEncumLineId"));
                  bidUniqueCodeArray.put(bidUniqueCodejson);

                  UniqueCodejson.put("bidUniqueList", bidUniqueCodeArray);
                  uniqueCodeListArray.put(UniqueCodejson);
                }

              }
            }

          }
        }
        uniquecodeResult.put("uniquecodeArray", uniqueCodeListArray);
        bidResult.put("list", array);
        uniquecodeResult.put("EqualObj", allLineEqual);
      }
      result.put("uniqueCodeList", uniquecodeResult);
      // result.put("decrease", uniquecodeResultDec);
      result.put("list", bidResult);
      log.debug("result:" + result);
      return result;

    } catch (Exception e) {
      log.error("Exception in getBidDetails ", e);
      OBDal.getInstance().rollbackAndClose();
    }
    return result;
  }

  public static void updateEncumbranceLineInProposal(EscmProposalMgmt proposal,
      EfinBudgetManencumlines encline, AccountingCombination com) {
    List<EscmProposalmgmtLine> ln = new ArrayList<EscmProposalmgmtLine>();
    try {

      OBQuery<EscmProposalmgmtLine> prolineQry = OBDal.getInstance().createQuery(
          EscmProposalmgmtLine.class,
          " as e where e.eFINUniqueCode.id=:uniqcodeID and e.escmProposalmgmt.id=:proposalID"
              + " and e.summary='N' and (e.status != 'CL' or e.status is null) ");
      prolineQry.setNamedParameter("uniqcodeID", com.getId());
      prolineQry.setNamedParameter("proposalID", proposal.getId());

      log.debug("bidlineQry " + prolineQry.list().size());
      log.debug("bidlineQry " + prolineQry.getWhereAndOrderBy());

      if (prolineQry.list().size() > 0) {
        ln = prolineQry.list();
        for (EscmProposalmgmtLine proln : ln) {
          proln.setEfinBudgmanencumline(encline);
          OBDal.getInstance().save(proln);
        }
      }
    } catch (Exception e) {
      log.error("Exception in updateEncumbranceLineInBid " + e.getMessage());
    }
  }

  @SuppressWarnings("rawtypes")
  public static boolean chkManualEncumbranceValidation(EscmProposalMgmt proposal,
      boolean appliedamtchk) {
    Query query = null, query1 = null;
    boolean errorflag = false;
    String message = null;
    String sqlQuery;
    try {

      if ("PAWD".equals(proposal.getProposalstatus())) {
        sqlQuery = "  select manln.c_validcombination_id  ,coalesce( sum (awardedamount),0) ,  "
            + "      coalesce(manln.revamount,0)- coalesce(app_amt,0)- coalesce(used_amount,0) as unapamt   "
            + "       ,case when coalesce( sum (awardedamount),0) > coalesce(manln.revamount,0)- coalesce(app_amt,0)- coalesce(used_amount,0)  "
            + "        then 'f' else 't' end ,manln.efin_budget_manencumlines_id from escm_proposalmgmt_line  ln   "
            + "     join escm_proposalmgmt pro on pro.escm_proposalmgmt_id= ln.escm_proposalmgmt_id  "
            + "     join efin_budget_manencumlines manln on manln.efin_budget_manencum_id= pro.em_efin_encumbrance_id  "
            + "     and manln.c_validcombination_id= ln.em_efin_c_validcombination_id and ln.issummarylevel='N' and (ln.status != 'CL' or ln.status is null)  "
            + "       where pro.escm_proposalmgmt_id  =  ?   "
            + "     group by manln.c_validcombination_id  ,manln.revamount ,used_amount,app_amt,manln.efin_budget_manencumlines_id ";
      } else {
        sqlQuery = "  select manln.c_validcombination_id  ,coalesce( sum (line_total),0) ,  "
            + "      coalesce(manln.revamount,0)- coalesce(app_amt,0)- coalesce(used_amount,0) as unapamt   "
            + "       ,case when coalesce( sum (line_total),0) > coalesce(manln.revamount,0)- coalesce(app_amt,0)- coalesce(used_amount,0)  "
            + "        then 'f' else 't' end ,manln.efin_budget_manencumlines_id from escm_proposalmgmt_line  ln   "
            + "     join escm_proposalmgmt pro on pro.escm_proposalmgmt_id= ln.escm_proposalmgmt_id  "
            + "     join efin_budget_manencumlines manln on manln.efin_budget_manencum_id= pro.em_efin_encumbrance_id  "
            + "     and manln.c_validcombination_id= ln.em_efin_c_validcombination_id and ln.issummarylevel='N' and (ln.status != 'CL' or ln.status is null)  "
            + "       where pro.escm_proposalmgmt_id  =  ?   "
            + "     group by manln.c_validcombination_id  ,manln.revamount ,used_amount,app_amt,manln.efin_budget_manencumlines_id ";
      }
      query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
      query.setParameter(0, proposal.getId());
      log.debug("strQuery:" + query.toString());
      List queryList = query.list();
      if (queryList != null && queryList.size() > 0) {
        for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          if (appliedamtchk) {
            if (row[3] != null && row[0] != null && row[3].equals("f")) {
              message = OBMessageUtils.messageBD("Efin_ReqAmt_More");
              errorflag = true;
            } else
              message = null;

            String UpdateQuery = " update  escm_proposalmgmt_line  set em_efin_failure_reason= ? "
                + " where em_efin_c_validcombination_id = ? and escm_proposalmgmt_id= ?  and (status != 'CL' or status is null) ";
            query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
            query1.setParameter(0, message);
            query1.setParameter(1, row[0].toString());
            query1.setParameter(2, proposal.getId());
            query1.executeUpdate();
          } else {

            String UpdateQuery = " update  efin_budget_manencumlines  set app_amt= app_amt+? "
                + " where efin_budget_manencumlines_id = ? ";
            query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
            query1.setParameter(0, new BigDecimal(row[1].toString()));
            query1.setParameter(1, row[4].toString());
            query1.executeUpdate();

            String UpdateQuery1 = " update  escm_proposalmgmt_line  set em_efin_budgmanencumline_id= ? "
                + " where em_efin_c_validcombination_id = ?  and escm_proposalmgmt_id= ?  and (status != 'CL' or status is null) ";
            query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery1);
            query1.setParameter(0, row[4].toString());
            query1.setParameter(1, row[0].toString());
            query1.setParameter(2, proposal.getId());
            query1.executeUpdate();

          }
        }
      }

      return errorflag;
    } catch (

    Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in chkManualEncumbranceValidation " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
      return true;
    }
  }

  /**
   * check validation for manual encumbrance all the uniquceode belongs to same encum.
   * 
   * @param objRequisition
   * @param encumLines
   * @return
   */
  public static boolean checkAllUniquecodesameEncum(EscmProposalMgmt proposal) {
    boolean errorFlag = false;
    try {
      OBContext.setAdminMode();
      // checking with proposal line
      if (proposal.getEfinEncumbrance() != null) {
        OBQuery<EscmProposalmgmtLine> rline = OBDal.getInstance()
            .createQuery(EscmProposalmgmtLine.class, " eFINUniqueCode.id not in"
                + "(select e.accountingCombination.id from Efin_Budget_Manencumlines as e "
                + "where e.manualEncumbrance.id =:encumID ) and escmProposalmgmt.id =:proposalID");
        rline.setNamedParameter("encumID", proposal.getEfinEncumbrance().getId());
        rline.setNamedParameter("proposalID", proposal.getId());
        if (rline.list() != null && rline.list().size() > 0) {
          errorFlag = true;
        }
      }
      return errorFlag;
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in checkAllUniquecodesameEncum in requisition : ", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Check there is current uniqucode is also present in cost type.
   * 
   * @param objRequisition
   * @return
   */
  public static boolean checkFundsNoCostValidation(EscmProposalMgmt proposal) {
    boolean error = false;
    try {
      OBContext.setAdminMode();
      for (EscmProposalmgmtLine line : proposal.getEscmProposalmgmtLineList()) {
        if (!line.isSummary()) {
          OBQuery<AccountingCombination> uniqucode = OBDal.getInstance().createQuery(
              AccountingCombination.class,
              "account.id='" + line.getEFINUniqueCode().getAccount().getId() + "'"
                  + " and salesCampaign.efinBudgettype='C' and account.efinFundsonly='N' ");
          if (uniqucode.list() != null && uniqucode.list().size() > 0) {
            error = true;
            line.setEfinFailureReason(OBMessageUtils.messageBD("Efin_FundsNoCost_Req"));
            OBDal.getInstance().save(line);
          } else {
            line.setEfinFailureReason(null);
            OBDal.getInstance().save(line);
          }
        }
      }
      return error;
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  checkFundsNoCostValidation in requisiton " + e, e);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return error;
  }

  @SuppressWarnings("rawtypes")
  public static boolean chkAutoEncumbranceValidation(EscmProposalMgmt proposal, Boolean singleline,
      String comId, Boolean isreject, Boolean Manual, BigDecimal incordecAmt) {
    Query query = null, query1 = null;
    boolean errorflag = false;
    String message = null;
    int count = 0;
    String sqlQuery = null;
    try {

      if ("PAWD".equals(proposal.getProposalstatus())) {
        sqlQuery = "  select com.c_validcombination_id ,lncom.c_validcombination_id ,  com.em_efin_isdeptfund ,  "
            + " case when com.em_efin_isdeptfund ='N' then  "
            + "coalesce(parent.funds_available,0) else coalesce(inq.funds_available,0) end as funds_availble, coalesce(sum(ln.awardedamount),0),  "
            + "case when coalesce(sum(ln.awardedamount),0) > ( case when com.em_efin_isdeptfund ='N' then  "
            + "coalesce(parent.funds_available,0) else coalesce(inq.funds_available,0) end) then 'f' else 't' end   "
            + "from escm_proposalmgmt_line  ln   "
            + "join  escm_proposalmgmt pro on pro.escm_proposalmgmt_id=ln.escm_proposalmgmt_id   ";
      } else {
        sqlQuery = "  select com.c_validcombination_id ,lncom.c_validcombination_id ,  com.em_efin_isdeptfund ,  "
            + " case when com.em_efin_isdeptfund ='N' then  "
            + "coalesce(parent.funds_available,0) else coalesce(inq.funds_available,0) end as funds_availble, coalesce(sum(ln.line_total),0),  "
            + "case when coalesce(sum(ln.line_total),0) > ( case when com.em_efin_isdeptfund ='N' then  "
            + "coalesce(parent.funds_available,0) else coalesce(inq.funds_available,0) end) then 'f' else 't' end   "
            + "from escm_proposalmgmt_line  ln   "
            + "join  escm_proposalmgmt pro on pro.escm_proposalmgmt_id=ln.escm_proposalmgmt_id   ";
      }

      if (!isreject)
        sqlQuery += " left join c_validcombination com on com.c_validcombination_id= ln.em_efin_c_validcombination_id ";
      else if (Manual) {
        sqlQuery += " left join efin_budget_manencumlines encum on encum.efin_budget_manencum_id= pro.em_efin_encumbrance_id "
            + "           left join c_validcombination com on com.c_validcombination_id= encum.c_validcombination_id  ";
      }
      sqlQuery += "join  efin_budget_ctrl_param para on para.ad_client_id=ln.ad_client_id   "
          + " left join c_validcombination lncom on com.ad_org_id=lncom.ad_org_id  "
          + " and lncom.c_salesregion_id= para.budgetcontrol_costcenter and com.account_id= lncom.account_id   "
          + " and com.c_project_id= lncom.c_project_id and  com.c_activity_id=lncom.c_activity_id and com.user1_id=lncom.user1_id   "
          + " and com.user2_id =lncom.user2_id and com.c_campaign_id= lncom.c_campaign_id  "
          + "  and com.c_bpartner_id= lncom.c_bpartner_id  "
          + "  left join efin_budgetinquiry inq on inq.efin_budgetint_id= pro.em_efin_budgetinitial_id  "
          + "  and inq.c_validcombination_id = com.c_validcombination_id  "
          + "   left join efin_budgetinquiry parent on parent.efin_budgetint_id= pro.em_efin_budgetinitial_id  "
          + "  and parent.c_validcombination_id = lncom.c_validcombination_id  "
          + "  where ln.escm_proposalmgmt_id  = ? and ln.issummarylevel='N' and (ln.status != 'CL' or ln.status is null) ";
      if (singleline && !isreject)
        sqlQuery += "  and ln.em_efin_c_validcombination_id= ?    ";
      else if (Manual)
        sqlQuery += "  and encum.c_validcombination_id= ?  ";

      sqlQuery += " group by com.c_validcombination_id ,lncom.c_validcombination_id ,  com.em_efin_isdeptfund ,parent.funds_available,  "
          + "inq.funds_available  ";

      query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
      query.setParameter(0, proposal.getId());
      if (singleline)
        query.setParameter(1, comId);
      log.debug("strQuery1:" + query.toString());
      List queryList = query.list();
      if (queryList != null && queryList.size() > 0) {
        for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          if (!Manual) {
            if (row[5] != null && row[0] != null && row[5].equals("f")) {
              message = OBMessageUtils.messageBD("Efin_Encum_Amt_Error");
              errorflag = true;
            } else
              message = null;
          } else {
            if (row[3] != null && new BigDecimal(row[3].toString()).compareTo(incordecAmt) < 0) {
              message = OBMessageUtils.messageBD("Efin_Encum_Amt_Error");
              errorflag = true;
            } else
              message = null;
          }
          List<EscmProposalmgmtLine> prolnlist = proposal.getEscmProposalmgmtLineList().stream()
              .filter(a -> a.getEFINUniqueCode() != null
                  && a.getEFINUniqueCode().getId().equals(row[0].toString()))
              .collect(Collectors.toList());
          if (prolnlist.size() > 0) {
            for (EscmProposalmgmtLine proline : prolnlist) {
              proline.setEfinFailureReason(message);
              OBDal.getInstance().save(proline);
            }
          }
          /*
           * String UpdateQuery = " update  escm_proposalmgmt_line  set em_efin_failure_reason= ? "
           * + " where em_efin_c_validcombination_id = ? and escm_proposalmgmt_id= ? "; query1 =
           * OBDal.getInstance().getSession().createSQLQuery(UpdateQuery); query1.setParameter(0,
           * message); query1.setParameter(1, row[0].toString()); query1.setParameter(2,
           * proposal.getId()); count = query1.executeUpdate();
           */

          if (isreject) {
            if (message != null && count == 0) {
              AccountingCombination com = OBDal.getInstance().get(AccountingCombination.class,
                  comId);
              String selectQry = "  select  em_efin_failure_reason from escm_proposalmgmt_line "
                  + "  where escm_proposalmgmt_id= ?  order by created limit 1  ";
              query1 = OBDal.getInstance().getSession().createSQLQuery(selectQry);
              query1.setParameter(0, proposal.getId());
              List msgList = query1.list();
              if (query1 != null && msgList.size() > 0) {
                String msg = (String) msgList.get(0);
                if (msg != null) {
                  message = msg + "," + com.getEfinUniqueCode();
                } else {
                  message = null;
                }
              }

              message = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Cost");
              message = message.replace("@", com.getEfinUniqueCode());
              List<EscmProposalmgmtLine> prolnlist1 = proposal.getEscmProposalmgmtLineList()
                  .stream().collect(Collectors.toList());
              if (prolnlist1.size() > 0) {
                for (EscmProposalmgmtLine proline : prolnlist1) {
                  proline.setEfinFailureReason(message);
                  OBDal.getInstance().save(proline);
                }
              }
              /*
               * String UpdateQuery1 =
               * " update  escm_proposalmgmt_line  set em_efin_failure_reason= ? " +
               * " where  escm_proposalmgmt_line_id in ( select  escm_proposalmgmt_line_id from escm_proposalmgmt_line "
               * + "  where escm_proposalmgmt_id= ? order by created limit 1  )"; query1 =
               * OBDal.getInstance().getSession().createSQLQuery(UpdateQuery1);
               * query1.setParameter(0, message); query1.setParameter(1, proposal.getId()); count =
               * query1.executeUpdate(); log.debug("count2:" + count);
               */
            }
          }
        }
      }

      return errorflag;
    } catch (

    Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in chkAutoEncumbranceValidation " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
      return true;
    }
  }

  /**
   * Method to insert auto encumbrance
   * 
   * @param proposal
   */
  public static void insertAutoEncumbrance(EscmProposalMgmt proposal) {
    /* Date currentDate = new Date(); */
    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    try {
      OBContext.setAdminMode();
      List<EscmProposalmgmtLine> proLineList = null;
      OBQuery<EscmProposalmgmtLine> proLineQry = OBDal.getInstance()
          .createQuery(EscmProposalmgmtLine.class, "escmProposalmgmt.id=:proposalID "
              + " and summary='N' and (status != 'CL' or status is null)");
      proLineQry.setNamedParameter("proposalID", proposal.getId());
      if (proLineQry.list() != null && proLineQry.list().size() > 0) {
        proLineList = proLineQry.list();
      }
      EfinBudgetManencum encum = OBProvider.getInstance().get(EfinBudgetManencum.class);
      AccountingCombination com = proLineList.get(0).getEFINUniqueCode();
      encum.setSalesCampaign(com.getSalesCampaign());
      encum.setEncumType("PAE");
      encum.setSalesRegion(com.getSalesRegion());
      encum.setEncumMethod("A");
      encum.setEncumStage("PAE");
      encum.setOrganization(proposal.getOrganization());
      encum.setAccountingDate(dateFormat.parse(dateFormat.format(new Date())));
      encum.setTransactionDate(dateFormat.parse(dateFormat.format(new Date())));
      encum.setBudgetInitialization(proposal.getEfinBudgetinitial());
      encum.setAction("PD");
      encum.setBusinessPartner(proposal.getSupplier());
      if (proposal.getBidName() != null)
        encum.setDescription(proposal.getBidName());
      else
        encum.setDescription(proposal.getProposalno());
      OBDal.getInstance().save(encum);
      OBDal.getInstance().flush();
      for (EscmProposalmgmtLine proLine : proLineList) {
        OBQuery<EfinBudgetManencumlines> encumlineexists = OBDal.getInstance().createQuery(
            EfinBudgetManencumlines.class,
            "as e where e.manualEncumbrance.id=:encumID and e.accountingCombination.id =:acctID");
        encumlineexists.setNamedParameter("encumID", encum.getId());
        encumlineexists.setNamedParameter("acctID", proLine.getEFINUniqueCode().getId());

        if (encumlineexists.list() != null && encumlineexists.list().size() > 0) {
          EfinBudgetManencumlines encumLines = encumlineexists.list().get(0);
          if ("PAWD".equals(proLine.getEscmProposalmgmt().getProposalstatus())) {
            encumLines.setAmount(encumLines.getAmount().add(proLine.getAwardedamount()));
            encumLines.setRemainingAmount(BigDecimal.ZERO);
            encumLines.setAPPAmt(encumLines.getAPPAmt().add(proLine.getAwardedamount()));
            encumLines.setRevamount(encumLines.getRevamount().add(proLine.getAwardedamount()));
          } else {
            encumLines.setAmount(encumLines.getAmount().add(proLine.getLineTotal()));
            encumLines.setRemainingAmount(BigDecimal.ZERO);
            encumLines.setAPPAmt(encumLines.getAPPAmt().add(proLine.getLineTotal()));
            encumLines.setRevamount(encumLines.getRevamount().add(proLine.getLineTotal()));
          }
          OBDal.getInstance().save(encumLines);
          OBDal.getInstance().flush();

          proLine.setEfinBudgmanencumline(encumLines);
          OBDal.getInstance().save(proLine);

        } else {

          EfinBudgetManencumlines encumLines = OBProvider.getInstance()
              .get(EfinBudgetManencumlines.class);
          JSONObject fundsCheckingObject = null;
          BigDecimal fundsAvailable = BigDecimal.ZERO;
          if (com != null) {
            EfinBudgetIntialization budgetIntialization = Utility
                .getObject(EfinBudgetIntialization.class, encum.getBudgetInitialization().getId());

            try {
              if ("E".equals(com.getEfinDimensiontype())) {
                fundsCheckingObject = CommonValidations.getFundsAvailable(budgetIntialization, com);
                fundsAvailable = new BigDecimal(fundsCheckingObject.get("FA").toString());
              }
            } catch (Exception e) {
              fundsAvailable = BigDecimal.ZERO;
            }
          }
          encumLines.setManualEncumbrance(encum);
          encumLines.setLineNo(proLine.getLineNo());
          encumLines.setUsedAmount(BigDecimal.ZERO);
          encumLines.setRemainingAmount(BigDecimal.ZERO);
          encumLines.setFundsAvailable(fundsAvailable);

          // For Partial awarding we should take awarded amount instead of line total
          if ("PAWD".equals(proLine.getEscmProposalmgmt().getProposalstatus())) {
            encumLines.setAmount(proLine.getAwardedamount());
            encumLines.setAPPAmt(proLine.getAwardedamount());
            encumLines.setRevamount(proLine.getAwardedamount());
          } else {
            encumLines.setAmount(proLine.getLineTotal());
            encumLines.setAPPAmt(proLine.getLineTotal());
            encumLines.setRevamount(proLine.getLineTotal());
          }

          encumLines.setOrganization(proposal.getOrganization());
          encumLines.setSalesRegion(proLine.getEFINUniqueCode().getSalesRegion());
          encumLines.setAccountElement(proLine.getEFINUniqueCode().getAccount());
          encumLines.setSalesCampaign(proLine.getEFINUniqueCode().getSalesCampaign());
          encumLines.setProject(proLine.getEFINUniqueCode().getProject());
          encumLines.setActivity(proLine.getEFINUniqueCode().getActivity());
          encumLines.setStDimension(proLine.getEFINUniqueCode().getStDimension());
          encumLines.setNdDimension(proLine.getEFINUniqueCode().getNdDimension());
          encumLines.setBusinessPartner(proLine.getEFINUniqueCode().getBusinessPartner());
          encumLines.setAccountingCombination(proLine.getEFINUniqueCode());
          encumLines.setUniqueCodeName(proLine.getEFINUniqueCode().getEfinUniquecodename());
          OBDal.getInstance().save(encumLines);
          OBDal.getInstance().flush();
          proLine.setEfinBudgmanencumline(encumLines);
          OBDal.getInstance().save(proLine);

          // Trigger changes EfinEncumbarnceRevision.updateBudgetInquiry(encumLines, encum,
          // proLine.getLineTotal());
        }
      }
      encum.setDocumentStatus("CO");
      OBDal.getInstance().save(encum);
      proposal.setEfinEncumbrance(encum);
      proposal.setEfinIsbudgetcntlapp(true);

      if (proposal.getProposalstatus().equals("PAWD")) {
        // update encumbrance value in proposal attribute tab
        OBQuery<EscmProposalAttribute> proposalAttrQry = OBDal.getInstance().createQuery(
            EscmProposalAttribute.class, " as e where e.escmProposalmgmt.id = :proposalId");
        proposalAttrQry.setNamedParameter("proposalId", proposal.getId());

        if (proposalAttrQry != null) {
          List<EscmProposalAttribute> proposalAttrList = proposalAttrQry.list();
          if (proposalAttrList.size() > 0) {
            EscmProposalAttribute proposalAttr = proposalAttrList.get(0);
            proposalAttr.setEFINManualEncumbrance(encum);
            proposalAttr.setEFINEncumbranceMethod(encum.getEncumMethod());
            OBDal.getInstance().save(proposalAttr);
          }
        }
      }

      OBDal.getInstance().save(proposal);
    } catch (OBException e) {
      log.error(" Exception while insertAutoEncumbrance: " + e);
      throw new OBException(e.getMessage());
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in insertAutoEncumbrance in proposal : ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @SuppressWarnings("rawtypes")
  public static boolean getBidtoProposalDetails(EscmProposalMgmt proposal, boolean appliedamtchk) {
    Query query = null, query1 = null;
    boolean errorflag = false;
    String message = null;
    // BigDecimal bidAmt = BigDecimal.ZERO;
    BigDecimal proposalAmt = BigDecimal.ZERO, diff = BigDecimal.ZERO, unAppAmt = BigDecimal.ZERO,
        reqLnNetAmt = BigDecimal.ZERO;
    EfinBudgetManencumlines line = null, newencumLine = null;
    AccountingCombination com = null;
    boolean uniqueCodeChanged = false;
    try {

      if (appliedamtchk) {
        String UpdateQuery = " update  escm_proposalmgmt_line  set em_efin_failure_reason= null "
            + "  where escm_proposalmgmt_id= ? ";
        query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
        query1.setParameter(0, proposal.getId());
        query1.executeUpdate();
      }

      // String sqlQuery = " select prlln.em_efin_c_validcombination_id as procomid, "
      // + "coalesce((prlln.line_total),0) as proposalamt , "
      // + " bidln.escm_bidmgmt_id, bidln.em_efin_budgmanencumline_id, "
      // + " coalesce(manenc.revamount,0)- coalesce(manenc.app_amt,0)-
      // coalesce(manenc.used_amount,0), "
      // + " (select coalesce(sum(reqln.priceactual* srcref.quantity),0) as srcamt "
      // + " from escm_bidsourceref srcref "
      // + " join escm_bidmgmt_line ln on ln.escm_bidmgmt_line_id= srcref.escm_bidmgmt_line_id "
      // + " join m_requisitionline reqln on reqln.m_requisitionline_id= srcref.m_requisitionline_id
      // and reqln.em_escm_issummary='N' "
      // + " where ln.escm_bidmgmt_id=bidln.escm_bidmgmt_id and
      // ln.c_validcombination_id=prlln.em_efin_c_validcombination_id"
      // + " group by ln.c_validcombination_id), bidln.c_validcombination_id,
      // prlln.em_efin_c_validcombination_id, "
      // + " sum(reqln.priceactual* bidsrcref.quantity) as reqlinenetamt,
      // bid.efin_budget_manencum_id,"
      // + " prlln.escm_proposalmgmt_line_id "
      //
      // + " from escm_proposalmgmt_line prlln "
      // + " join escm_bidmgmt_line bidln on bidln.escm_bidmgmt_line_id= prlln.escm_bidmgmt_line_id
      // and bidln.issummarylevel='N' "
      // + " join escm_bidmgmt bid on bid.escm_bidmgmt_id=bidln.escm_bidmgmt_id "
      // + " left join efin_budget_manencumlines manenc on manenc.efin_budget_manencumlines_id=
      // bidln.em_efin_budgmanencumline_id "
      // + " left join escm_bidsourceref bidsrcref on
      // bidsrcref.escm_bidmgmt_line_id=bidln.escm_bidmgmt_line_id "
      // + " left join m_requisitionline reqln on
      // reqln.m_requisitionline_id=bidsrcref.m_requisitionline_id "
      // + " where prlln.escm_proposalmgmt_id = ? and prlln.issummarylevel='N' "
      // + " and (prlln.status != 'CL' or prlln.status is null) "
      // + " group by prlln.em_efin_c_validcombination_id, bidln.escm_bidmgmt_id, "
      // + " bidln.em_efin_budgmanencumline_id,manenc.revamount,manenc.app_amt,manenc.used_amount
      // ,bidln.c_validcombination_id, "
      // + " prlln.line_total, prlln.escm_proposalmgmt_line_id, bid.efin_budget_manencum_id ";
      String sqlQuery = " select  prlln.em_efin_c_validcombination_id as procomid,sum(coalesce((prlln.line_total),0)) as proposalamt,"
          + " (select coalesce(sum(reqln.priceactual* srcref.quantity),0) as srcamt   "
          + " from escm_bidsourceref srcref  join escm_bidmgmt_line ln on ln.escm_bidmgmt_line_id= srcref.escm_bidmgmt_line_id "
          + " join m_requisitionline reqln on reqln.m_requisitionline_id= srcref.m_requisitionline_id and reqln.em_escm_issummary='N' "
          + "  where ln.escm_bidmgmt_id=bidln.escm_bidmgmt_id and ln.c_validcombination_id=prlln.em_efin_c_validcombination_id "
          + "  group by ln.c_validcombination_id), bid.efin_budget_manencum_id   "
          + "  from escm_proposalmgmt_line prlln "
          + "  join escm_bidmgmt_line bidln on bidln.escm_bidmgmt_line_id= prlln.escm_bidmgmt_line_id "
          + "   join escm_bidmgmt bid on bid.escm_bidmgmt_id=bidln.escm_bidmgmt_id "
          + " left join efin_budget_manencumlines manenc on manenc.efin_budget_manencumlines_id= bidln.em_efin_budgmanencumline_id  "
          + " left join escm_bidsourceref bidsrcref on bidsrcref.escm_bidmgmt_line_id=bidln.escm_bidmgmt_line_id  "
          + " left join m_requisitionline reqln on reqln.m_requisitionline_id=bidsrcref.m_requisitionline_id "
          + "  and bidln.issummarylevel='N'  "
          + "  where prlln.escm_proposalmgmt_id = ? and prlln.issummarylevel='N' "
          + "  group by bidln.escm_bidmgmt_id,prlln.em_efin_c_validcombination_id , prlln.em_efin_c_validcombination_id "
          + " ,bid.efin_budget_manencum_id ";

      query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
      query.setParameter(0, proposal.getId());
      log.debug("strQuery:" + query.toString());
      List queryList = query.list();
      if (queryList != null && queryList.size() > 0) {
        for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          line = null;
          message = null;
          // bidAmt = BigDecimal.ZERO;
          proposalAmt = BigDecimal.ZERO;
          reqLnNetAmt = BigDecimal.ZERO;
          diff = BigDecimal.ZERO;
          // if uniquecode present in lines
          if (row[0] != null)
            com = OBDal.getInstance().get(AccountingCombination.class, row[0].toString());

          if (row[1] != null)
            proposalAmt = new BigDecimal(row[1].toString());
          // if (row[5] != null)
          // bidAmt = new BigDecimal(row[5].toString());
          if (row[3] != null) {
            line = getEncumbranceLine(row[3].toString(), row[0].toString());
            if (line != null)
              unAppAmt = (line.getRevamount().subtract(line.getAPPAmt())
                  .subtract(line.getUsedAmount()));
            // unAppAmt = new BigDecimal(row[4].toString());
          }
          if (row[2] != null)
            reqLnNetAmt = new BigDecimal(row[2].toString());
          // if (row[3] != null)
          // line = OBDal.getInstance().get(EfinBudgetManencumlines.class, row[3].toString());
          // else {
          // line = getEncumbranceLine(row[9].toString(), row[0].toString());
          // }

          // diff = proposalAmt.subtract(bidAmt);
          diff = proposalAmt.subtract(reqLnNetAmt);
          if (proposal.getEfinEncumbrance() != null
              && proposal.getEfinEncumbrance().getEncumMethod().equals("M")) {
            if (diff.compareTo(BigDecimal.ZERO) == 0) {

            }
            // increase
            else if (diff.compareTo(BigDecimal.ZERO) > 0) {
              if (diff.compareTo(unAppAmt) > 0) {
                errorflag = true;
                if (appliedamtchk && errorflag) {
                  message = OBMessageUtils.messageBD("Efin_ReqAmt_More");
                }
                // String UpdateQuery = " update escm_proposalmgmt_line set em_efin_failure_reason=
                // ? "
                // + " where em_efin_c_validcombination_id = ? and escm_proposalmgmt_id= ? and
                // (status != 'CL' or status is null) ";
                // query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
                // query1.setParameter(0, message);
                // query1.setParameter(1, row[0].toString());
                // query1.setParameter(2, proposal.getId());
                // query1.executeUpdate();

                OBQuery<EscmProposalmgmtLine> proposalLineQry = OBDal.getInstance().createQuery(
                    EscmProposalmgmtLine.class, " as e where e.eFINUniqueCode.id = :uniqueCodeID "
                        + " and e.escmProposalmgmt.id = :proposalID and (status != 'CL' or status is null) ");
                proposalLineQry.setNamedParameter("uniqueCodeID", row[0].toString());
                proposalLineQry.setNamedParameter("proposalID", proposal.getId());
                if (proposalLineQry != null) {
                  List<EscmProposalmgmtLine> proposalLineList = proposalLineQry.list();
                  if (proposalLineList.size() > 0) {
                    for (EscmProposalmgmtLine proposalLine : proposalLineList) {
                      proposalLine.setEfinFailureReason(message);
                      OBDal.getInstance().save(proposalLine);
                    }
                  }
                }

              } else {
                // in case of manual increase - unapplied amount will be exist then update applied
                // amt
                if (!appliedamtchk) {
                  String UpdateQuery = " update  efin_budget_manencumlines  set app_amt= app_amt+? "
                      + " where efin_budget_manencumlines_id = ? ";
                  query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
                  query1.setParameter(0, diff);
                  query1.setParameter(1, line.getId());
                  query1.executeUpdate();
                }
              }
            }
            // decrease
            else {
              // in case of manual increase - unapplied amount will be exist then update applied
              // amt
              if (!appliedamtchk) {
                String UpdateQuery = " update  efin_budget_manencumlines  set app_amt= app_amt-? "
                    + " where efin_budget_manencumlines_id = ? ";
                query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
                query1.setParameter(0, diff.negate());
                query1.setParameter(1, line.getId());
                query1.executeUpdate();
              }
            }
            // new uniquecode
            if (line == null) {
              errorflag = true;
              if (appliedamtchk && errorflag) {
                message = OBMessageUtils.messageBD("EFIN_PropNewUniqNotAllow");
              }
              String UpdateQuery = " update  escm_proposalmgmt_line  set em_efin_failure_reason= ? "
                  + " where em_efin_c_validcombination_id = ? and escm_proposalmgmt_id= ? and (status != 'CL' or status is null) ";
              query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
              query1.setParameter(0, message);
              query1.setParameter(1, row[0].toString());
              query1.setParameter(2, proposal.getId());
              query1.executeUpdate();
            }
          } else {
            if (line == null) {
              if (!appliedamtchk) {
                // INSERT ENCUM LINE
                if (proposal.getEscmBidmgmt() != null) {
                  // EscmProposalmgmtLine propln = proposal.getEscmProposalmgmtLineList().get(0);
                  EfinBudgetManencum encum = null;
                  for (EscmProposalmgmtLine propln : proposal.getEscmProposalmgmtLineList()) {
                    if (!propln.isSummary()) {
                      encum = propln.getEfinBudgmanencumline().getManualEncumbrance();
                      break;
                    }
                  }

                  uniqueCodeChanged = false;
                  newencumLine = insertEncumbranceLines(encum, proposalAmt, com, proposal, null);
                  // if (proposal.getEscmBidmgmt() != null) {
                  // if (row[10] != null) {
                  // EscmProposalmgmtLine propsalLn = Utility.getObject(EscmProposalmgmtLine.class,
                  // row[10].toString());
                  // // for (EscmProposalmgmtLine propsalLn :
                  // // proposal.getEscmProposalmgmtLineList())
                  // // {
                  // // if (!propsalLn.isSummary()) {
                  // // if (propsalLn.getEscmBidmgmtLine() != null) {
                  // // Escmbidmgmtline bidln = propsalLn.getEscmBidmgmtLine();
                  // // if (bidln.getAccountingCombination() != null
                  // // && !bidln.getAccountingCombination().getId()
                  // // .equals(propsalLn.getEFINUniqueCode().getId())) {
                  // // EfinBudgetManencumlines encumLn = Utility.getObject(
                  // // EfinBudgetManencumlines.class,
                  // // bidln.getEfinBudgmanencumline().getId());
                  // //
                  // // encumLn.setAPPAmt(encumLn.getAPPAmt().add(reqLnNetAmt.negate()));
                  // // OBDal.getInstance().save(encumLn);
                  // // OBDal.getInstance().flush();
                  // // // insert encumbrance modification
                  // // BidManagementDAO.insertEncumbranceModification(
                  // // bidln.getEfinBudgmanencumline(), reqLnNetAmt.negate(), null, "PRO",
                  // // null, null);
                  // // }
                  // // }
                  // // }
                  // // }
                  // }
                  // }
                  proposal.setEfinEncumbrance(encum);
                }
                // update encumbranceline in proposal management
                updateEncumbranceLineInProposal(proposal, newencumLine, com);
              }
              // funds available check
              else {
                uniqueCodeChanged = true;
                errorflag = chkAutoEncumbranceValidation(proposal, true, com.getId(), false, false,
                    BigDecimal.ZERO);
              }
            }
            // increase
            else if (diff.compareTo(BigDecimal.ZERO) > 0) {
              if (!appliedamtchk) { // Trigger changes enc_increase=enc_increase+?,
                                    // ,revamount=revamount+? ,remaining_amount=remaining_amount +?
                // insert encumbrance modification
                BidManagementDAO.insertEncumbranceModification(line, diff, null, "PRO", null, null);
                EfinBudgetManencumlines encumLn = Utility.getObject(EfinBudgetManencumlines.class,
                    line.getId());
                encumLn.setAPPAmt(encumLn.getAPPAmt().add(diff));
                OBDal.getInstance().save(encumLn);
                OBDal.getInstance().flush();
              } else {
                errorflag = chkAutoEncumbranceValidation(proposal, true, com.getId(), false, true,
                    diff);
              }
            }
            // decrease
            else if (diff.compareTo(BigDecimal.ZERO) < 0) {
              if (!appliedamtchk) { // Trigger changes enc_decrease=enc_decrease+?,
                                    // ,revamount=revamount-? ,remaining_amount=remaining_amount -?

                EfinBudgetManencumlines encumLn = Utility.getObject(EfinBudgetManencumlines.class,
                    line.getId());
                encumLn.setAPPAmt(encumLn.getAPPAmt().add(diff));
                OBDal.getInstance().save(encumLn);
                OBDal.getInstance().flush();
                // insert encumbrance modification
                BidManagementDAO.insertEncumbranceModification(line, diff, null, "PRO", null, null);
              }
            }
            if (proposal.getEfinEncumbrance() == null && !uniqueCodeChanged) {
              for (EscmProposalmgmtLine propsalLn : proposal.getEscmProposalmgmtLineList()) {
                if (!propsalLn.isSummary()) {
                  if (propsalLn.getEfinBudgmanencumline() != null) {
                    EfinBudgetManencum encum = propsalLn.getEfinBudgmanencumline()
                        .getManualEncumbrance();
                    proposal.setEfinEncumbrance(encum);
                    break;
                  }
                }
              }
            }
          }
        }
      }
      if (proposal.getEfinEncumbrance() != null
          && proposal.getEfinEncumbrance().getEncumMethod().equals("A") && !appliedamtchk
          && !uniqueCodeChanged) {
        // update amount for unused combination in encumbrance line
        String strQry2 = "  select c_validcombination_id,efin_budget_manencumlines_id,efin_budget_manencum_id, revamount"
            + "  from efin_budget_manencumlines   where efin_budget_manencum_id= ?  and (isauto  is null or isauto='N')  "
            + "               and c_validcombination_id not in ( select em_efin_c_validcombination_id from escm_proposalmgmt_line"
            + "  where escm_proposalmgmt_id= ? and issummarylevel='N' and (status != 'CL' or status is null) and em_efin_c_validcombination_id is not null  )   ";
        query = OBDal.getInstance().getSession().createSQLQuery(strQry2);
        query.setParameter(0, proposal.getEfinEncumbrance().getId());
        query.setParameter(1, proposal.getId());
        List unUsedUniqCodeList = query.list();
        if (unUsedUniqCodeList != null && unUsedUniqCodeList.size() > 0) {
          for (Iterator iterator = unUsedUniqCodeList.iterator(); iterator.hasNext();) {
            Object[] row = (Object[]) iterator.next();
            line = OBDal.getInstance().get(EfinBudgetManencumlines.class, row[1].toString());

            BidManagementDAO.insertEncumbranceModification(line,
                new BigDecimal(row[3].toString()).negate(), null, "PRO", null, null);

            // Trigger changes line.setENCDecrease(line.getENCDecrease().add(new
            // BigDecimal(row[3].toString())));
            line.setAPPAmt(line.getAPPAmt().add(new BigDecimal(row[3].toString()).negate()));
            OBDal.getInstance().save(line);

            // update budget inquiry Trigger changes
            /*
             * EfinEncumbarnceRevision.updateBudgetInquiry(line, line.getManualEncumbrance(), new
             * BigDecimal(row[3].toString()).negate());
             */
          }
        }
      }

      if (appliedamtchk) {
        // Task#7788

        // String selectQry = " select count(escm_proposalmgmt_line_id ) from escm_proposalmgmt_line
        // where em_efin_failure_reason is not null"
        // + " and escm_proposalmgmt_id= ? ";
        List<EscmProposalmgmtLine> proposalLineList = proposal.getEscmProposalmgmtLineList()
            .stream().filter(a -> a.getEfinFailureReason() != null).collect(Collectors.toList());
        int count = proposalLineList.size();
        // query1 = OBDal.getInstance().getSession().createSQLQuery(selectQry);
        // query1.setParameter(0, proposal.getId());
        // List countList = query1.list();
        // if (query1 != null && countList.size() > 0) {
        // BigInteger count = (BigInteger) countList.get(0);
        if (count > 0) {
          errorflag = true;
        } else
          errorflag = false;
      }

      return errorflag;

    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in getBidtoProposalDetails " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
      return true;
    }
  }

  /**
   * get encumbrance line with same unique code
   * 
   * @param encumId
   * @param uniqueCodeId
   * @return
   */
  public static EfinBudgetManencumlines getEncumbranceLine(String encumId, String uniqueCodeId) {
    EfinBudgetManencumlines encumLn = null;
    try {
      OBContext.setAdminMode();
      OBQuery<EfinBudgetManencumlines> encumLnqry = OBDal.getInstance().createQuery(
          EfinBudgetManencumlines.class,
          " as e where e.accountingCombination.id=:uniqueCodeId and e.manualEncumbrance.id=:encumId");
      encumLnqry.setNamedParameter("uniqueCodeId", uniqueCodeId);
      encumLnqry.setNamedParameter("encumId", encumId);
      encumLnqry.setMaxResult(1);
      if (encumLnqry.list().size() > 0) {
        encumLn = encumLnqry.list().get(0);
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in getEncumbranceLine " + e, e);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return encumLn;
  }

  /**
   * Insert Encumbrance Lines
   * 
   * @param encum
   * @param Amount
   * @param com
   * @param proposal
   * @return
   */
  public static EfinBudgetManencumlines insertEncumbranceLines(EfinBudgetManencum encum,
      BigDecimal Amount, AccountingCombination com, EscmProposalMgmt proposal,
      EscmProposalmgmtLine proposalmgmtline) {
    Long lineNo = 0L;
    EfinBudgetManencumlines encumLines = null;
    try {
      List<EfinBudgetManencumlines> encumLnList = new ArrayList<EfinBudgetManencumlines>();
      List<EscmProposalmgmtLine> proposalLnList = new ArrayList<EscmProposalmgmtLine>();

      // get the next line no based on bid management id
      OBQuery<EfinBudgetManencumlines> lines = OBDal.getInstance().createQuery(
          EfinBudgetManencumlines.class,
          " as e where e.manualEncumbrance.id=:encumID   order by e.creationDate desc ");
      lines.setNamedParameter("encumID", encum.getId());
      lines.setMaxResult(1);
      encumLnList = lines.list();
      if (encumLnList.size() > 0) {
        lineNo = encumLnList.get(0).getLineNo();
        lineNo += 10;
      }
      OBQuery<EfinBudgetManencumlines> ln = OBDal.getInstance().createQuery(
          EfinBudgetManencumlines.class,
          " as e where e.manualEncumbrance.id=:encumID and e.accountingCombination.id=:acctId"
              + " order by e.creationDate desc ");
      ln.setNamedParameter("encumID", encum.getId());
      ln.setNamedParameter("acctId", com.getId());
      ln.setMaxResult(1);
      encumLnList = ln.list();
      if (encumLnList.size() == 0) {
        encumLines = OBProvider.getInstance().get(EfinBudgetManencumlines.class);
        encumLines.setManualEncumbrance(encum);
        encumLines.setLineNo(lineNo);
        encumLines.setAmount(Amount);
        encumLines.setUsedAmount(BigDecimal.ZERO);
        encumLines.setRemainingAmount(BigDecimal.ZERO);
        encumLines.setAPPAmt(Amount);
        encumLines.setRevamount(Amount);
        encumLines.setOrganization(encum.getOrganization());
        encumLines.setSalesRegion(com.getSalesRegion());
        encumLines.setAccountElement(com.getAccount());
        encumLines.setSalesCampaign(com.getSalesCampaign());
        encumLines.setProject(com.getProject());
        encumLines.setActivity(com.getActivity());
        encumLines.setStDimension(com.getStDimension());
        encumLines.setNdDimension(com.getNdDimension());
        encumLines.setBusinessPartner(com.getBusinessPartner());
        encumLines.setAccountingCombination(com);
        encumLines.setUniqueCodeName(com.getEfinUniquecodename());
        encumLines.setAuto(true);
        OBDal.getInstance().save(encumLines);
      } else {
        encumLines = encumLnList.get(0);
        encumLines.setAmount(encumLines.getAmount().add(Amount));
        encumLines.setAPPAmt(encumLines.getAPPAmt().add(Amount));
        encumLines.setRevamount(encumLines.getRevamount().add(Amount));
        OBDal.getInstance().save(encumLines);
      }
      OBDal.getInstance().flush();
      if (proposal != null) {
        OBQuery<EscmProposalmgmtLine> prolinQry = OBDal.getInstance().createQuery(
            EscmProposalmgmtLine.class,
            " as e where e.escmProposalmgmt.id=:proposalId and e.eFINUniqueCode.id=:uniqueCode "
                + " and (e.status != 'CL' or e.status is null)");
        prolinQry.setNamedParameter("proposalId", proposal.getId());
        prolinQry.setNamedParameter("uniqueCode", com.getId());
        proposalLnList = prolinQry.list();
      } else {
        OBQuery<EscmProposalmgmtLine> prolinQry = OBDal.getInstance().createQuery(
            EscmProposalmgmtLine.class,
            " as e where e.id=:proposalLineId and e.eFINUniqueCode.id=:uniqueCode "
                + " and (e.status != 'CL' or e.status is null)");
        prolinQry.setNamedParameter("proposalLineId", proposalmgmtline.getId());
        prolinQry.setNamedParameter("uniqueCode", com.getId());
        proposalLnList = prolinQry.list();
      }
      if (proposalLnList.size() > 0) {
        for (EscmProposalmgmtLine line : proposalLnList) {
          line.setEfinBudgmanencumline(encumLines);
          OBDal.getInstance().save(line);
        }
      }

      // Trigger changes EfinEncumbarnceRevision.updateBudgetInquiry(encumLines, encum, Amount);
      return encumLines;
    }

    catch (Exception e) {
      e.printStackTrace();
      if (log.isErrorEnabled()) {
        log.error("Exception in insertEncumbranceLines " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
    }
    return encumLines;
  }

  public static EfinBudgetManencum insertEncumbranceproposal(EscmProposalMgmt proposal,
      EfinBudgetManencum encumbranceObj) {
    EfinBudgetManencum encumbrance = null;
    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    try {
      OBContext.setAdminMode();

      // insert the Encumbrance
      encumbrance = OBProvider.getInstance().get(EfinBudgetManencum.class);
      encumbrance.setClient(proposal.getClient());
      encumbrance.setOrganization(proposal.getOrganization());
      encumbrance.setActive(true);
      encumbrance.setUpdatedBy(proposal.getCreatedBy());
      encumbrance.setCreationDate(new java.util.Date());
      encumbrance.setCreatedBy(proposal.getCreatedBy());
      encumbrance.setUpdated(new java.util.Date());
      encumbrance.setAccountingDate(dateFormat.parse(dateFormat.format(new Date())));
      encumbrance.setTransactionDate(dateFormat.parse(dateFormat.format(new Date())));
      encumbrance.setEncumType("PAE");
      encumbrance.setAuto(true);
      encumbrance.setDocumentStatus("DR");
      encumbrance.setSalesCampaign(encumbranceObj.getSalesCampaign());
      encumbrance.setSalesRegion(encumbranceObj.getSalesRegion());
      encumbrance.setEncumStage("PAE");
      encumbrance.setBudgetInitialization(encumbranceObj.getBudgetInitialization());
      if (proposal.getBidName() != null)
        encumbrance.setDescription(proposal.getBidName());
      else
        encumbrance.setDescription(proposal.getProposalno());

      OBDal.getInstance().save(encumbrance);
      OBDal.getInstance().flush();

      return encumbrance;

    } catch (Exception e) {
      log.error("Exception in insertEncumbranceOrder " + e.getMessage());
    }
    return encumbrance;
  }

  public static void insertEncumbranceLinesProsal(EscmProposalMgmt proposal,
      EfinBudgetManencum encumbrancenewObj, EfinBudgetManencum oldencumbranceObj, JSONObject result,
      EscmProposalmgmtLine proposalmgmtline) {
    BigDecimal Amount = BigDecimal.ZERO;
    JSONObject json = null, jsonencum = null, result1 = null;
    JSONObject prResult = null;
    EfinBudgetManencumlines manualline = null;
    EfinBudManencumRev manEncumRev = null;
    String tempProposalLineId = null;
    BigDecimal totalAmount = BigDecimal.ZERO;
    BigDecimal amtInModification = BigDecimal.ZERO;
    try {
      OBContext.setAdminMode();

      // get PR detail based on associated Bid source ref .
      prResult = getPRDetailsBasedOnProposalQty1(proposal, proposalmgmtline);

      log.debug("prResult:" + prResult);
      if (prResult != null && prResult.getJSONObject("prListarray") != null) {
        result1 = prResult.getJSONObject("prListarray");
        JSONArray array = result1.getJSONArray("list");
        for (int i = 0; i < array.length(); i++) {
          json = array.getJSONObject(i);
          // loop the old encumbrance list and decrease the applied value,revision,remaining amount
          // and created decrease entry in revision for old encumbrance
          JSONArray encumarray = json.getJSONArray("encList");
          totalAmount = new BigDecimal(json.getString("totalamount"));
          for (int j = 0; j < encumarray.length(); j++) {
            jsonencum = encumarray.getJSONObject(j);
            if (jsonencum.has("encumId") && jsonencum.getString("encumId") != null
                && jsonencum.getString("validcomId") != null) {
              // get old encumbrance line
              OBQuery<EfinBudgetManencumlines> lines = OBDal.getInstance().createQuery(
                  EfinBudgetManencumlines.class,
                  " as e where e.manualEncumbrance.id=:encumId and e.accountingCombination.id=:accId");
              lines.setNamedParameter("encumId", jsonencum.getString("encumId"));
              lines.setNamedParameter("accId", jsonencum.getString("validcomId"));
              lines.setMaxResult(1);
              if (lines.list().size() > 0) {
                // decrease the rev amount and remaining amount
                Amount = new BigDecimal(jsonencum.getString("encamount"));
                log.debug("amount1:" + Amount);
                EfinBudgetManencumlines encumline = lines.list().get(0);

                // Task no.7749: If amount is increased in Proposal, insert modification with
                // increased amount
                amtInModification = Amount;

                BigDecimal difference = totalAmount.subtract(Amount);
                // increase
                if (encumline.getManualEncumbrance().getEncumMethod().equals("M")
                    && difference.compareTo(BigDecimal.ZERO) > 0) {
                  if (difference.compareTo(encumline.getRemainingAmount()) <= 0) {
                    encumline.setAPPAmt(encumline.getAPPAmt().add(difference));
                    amtInModification = totalAmount;
                  }
                }

                // insert the Encumbrance revision entry(-ve value)
                manEncumRev = BidManagementDAO.insertEncumbranceModification(encumline,
                    amtInModification.negate(), manualline, "PRO", null, null);
                if (tempProposalLineId == null
                    || !tempProposalLineId.equals(json.getString("bidlineId"))) {
                  manualline = insertEncumbranceLinesProposal(proposal, encumbrancenewObj,
                      oldencumbranceObj, json);
                }
                if (manualline != null && manEncumRev != null) {
                  manEncumRev.setSRCManencumline(manualline);
                  OBDal.getInstance().save(manEncumRev);
                }
                encumline.setAPPAmt(encumline.getAPPAmt().subtract(amtInModification));
                OBDal.getInstance().save(encumline);
              }
            } else {

              manualline = insertEncumbranceLinesProposal(proposal, encumbrancenewObj,
                  oldencumbranceObj, json);
            }
            tempProposalLineId = json.getString("bidlineId");
          }

        }
      }
    } catch (Exception e) {
      log.error("Exception in insertEncumbranceLines " + e.getMessage());
    }
  }

  public static EfinBudgetManencumlines insertEncumbranceLinesProposal(EscmProposalMgmt proposal,
      EfinBudgetManencum encumbrancenewObj, EfinBudgetManencum oldencumbranceObj, JSONObject json) {
    Long lineno = 10L;
    BigDecimal totalAmount = BigDecimal.ZERO;
    EfinBudgetManencumlines manualline = null;
    try {
      OBContext.setAdminMode();
      List<EfinBudgetManencumlines> encumLnList = new ArrayList<EfinBudgetManencumlines>();

      if (json != null) {
        EscmProposalmgmtLine ln = OBDal.getInstance().get(EscmProposalmgmtLine.class,
            json.getString("bidlineId"));
        totalAmount = new BigDecimal(json.getString("totalamount"));

        // check already unqiuecode exists or not
        OBQuery<EfinBudgetManencumlines> lnexistQry = OBDal.getInstance().createQuery(
            EfinBudgetManencumlines.class,
            " as e where e.manualEncumbrance.id=:encumID and e.accountingCombination.id=:acctID");
        lnexistQry.setNamedParameter("encumID", encumbrancenewObj.getId());
        lnexistQry.setNamedParameter("acctID", ln.getEFINUniqueCode().getId());
        lnexistQry.setMaxResult(1);
        encumLnList = lnexistQry.list();
        // if exists update the amount, revision amount ,applied amount
        if (encumLnList.size() > 0) {
          manualline = encumLnList.get(0);
          manualline.setAmount(manualline.getAmount().add(totalAmount));
          manualline.setRevamount(manualline.getRevamount().add(totalAmount));
          manualline.setRemainingAmount(BigDecimal.ZERO);
          manualline.setOriginalamount(manualline.getOriginalamount().add(totalAmount));
          manualline.setAPPAmt(manualline.getAPPAmt().add(totalAmount));
          OBDal.getInstance().save(manualline);
        }
        // if not exists then insert the Encumbrance lines
        else {
          manualline = OBProvider.getInstance().get(EfinBudgetManencumlines.class);
          manualline.setClient(encumbrancenewObj.getClient());
          manualline.setOrganization(encumbrancenewObj.getOrganization());
          manualline.setUpdatedBy(encumbrancenewObj.getCreatedBy());
          manualline.setCreationDate(new java.util.Date());
          manualline.setCreatedBy(encumbrancenewObj.getCreatedBy());
          manualline.setUpdated(new java.util.Date());
          manualline.setLineNo(lineno);
          if (ln.getEFINUniqueCode() != null) {
            manualline.setUniquecode(ln.getEFINUniqueCode().getEfinUniqueCode());
            manualline.setSalesRegion(ln.getEFINUniqueCode().getSalesRegion());
            manualline.setAccountElement(ln.getEFINUniqueCode().getAccount());
            manualline.setSalesCampaign(ln.getEFINUniqueCode().getSalesCampaign());
            manualline.setProject(ln.getEFINUniqueCode().getProject());
            manualline.setActivity(ln.getEFINUniqueCode().getActivity());
            manualline.setStDimension(ln.getEFINUniqueCode().getStDimension());
            manualline.setNdDimension(ln.getEFINUniqueCode().getNdDimension());
            manualline.setAccountingCombination(ln.getEFINUniqueCode());
          }

          manualline.setBudgetLines(null);
          manualline.setManualEncumbrance(encumbrancenewObj);
          manualline.setAmount(totalAmount);
          manualline.setRevamount(totalAmount);
          manualline.setRemainingAmount(BigDecimal.ZERO);
          manualline.setOriginalamount(totalAmount);
          manualline.setAPPAmt(totalAmount);
          manualline.setUsedAmount(BigDecimal.ZERO);
          lineno += 10;
          OBDal.getInstance().save(manualline);
          OBDal.getInstance().flush();

          // update encumbranceline in proposal management
          updateEncumbranceLineInProposal(proposal, manualline, ln.getEFINUniqueCode());
        }
      }
    } catch (Exception e) {
      log.error("Exception in insertEncumbranceLines " + e.getMessage());
    }
    return manualline;
  }

  @SuppressWarnings("rawtypes")
  public static JSONObject getPRDetailsBasedOnProposalQty(EscmProposalMgmt proposal) {
    String strQuery = null;
    Query query = null;
    BigDecimal Amount = BigDecimal.ZERO, totalAmount = BigDecimal.ZERO;
    JSONObject prResult = new JSONObject(), uniquecodeResult = new JSONObject(), json = null,
        json1 = null, UniqueCodejson = null, bidLineJson = null, json2 = null;
    JSONArray prlistArray = new JSONArray(), encListArray = new JSONArray(),
        uniqueCodeListArray = new JSONArray(), linearraylist = null;
    String tempbidLineId = null;
    JSONObject result = new JSONObject();
    Boolean sameUniqueCode = false;
    try {

      // calculate the qty amount corresponding PR linettoal
      strQuery = " select req.em_efin_budget_manencum_id , reqln.em_efin_c_validcombination_id,"
          + " case when coalesce(sum(reqln.qty),0) > 0 "
          + " then  sum(round((coalesce(reqln.priceactual,0)*coalesce(ref.quantity,0)),2)) "
          + " else 0 end  as amount ,ln.escm_proposalmgmt_line_id ,ln.em_efin_c_validcombination_id ,ln.issummarylevel ,ln.line_total "
          + " from  escm_proposalsource_ref ref join escm_proposalmgmt_line ln on ln.escm_proposalmgmt_line_id= ref.escm_proposalmgmt_line_id"
          + "  join m_requisitionline reqln on reqln.m_requisitionline_id= ref.m_requisitionline_id "
          + " join m_requisition req on req.m_requisition_id= reqln.m_requisition_id and req.em_efin_budget_manencum_id is not null"
          + " where ln.escm_proposalmgmt_id = ?  and ln.issummarylevel  ='N' and (ln.status != 'CL' or ln.status is null) "
          + " group by ln.escm_proposalmgmt_line_id ,req.em_efin_budget_manencum_id , reqln.em_efin_c_validcombination_id ,ln.em_efin_c_validcombination_id ";
      query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      query.setParameter(0, proposal.getId());
      log.debug("strQuery:" + query.toString());
      List queryList = query.list();
      if (queryList != null && queryList.size() > 0) {
        for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();

          // if tempbid line id is not equal to current bid lines id then create new json object
          if (tempbidLineId != null && !tempbidLineId.equals(row[3].toString())) {
            json.put("encList", encListArray);
            prlistArray.put(json);
            encListArray = new JSONArray();

            json = new JSONObject();
            json.put("bidlineId", row[3].toString());
            json.put("bidvalidcomId", row[4].toString());

            // form the encum list
            json1 = new JSONObject();
            json1.put("encumId", row[0].toString());
            json1.put("encamount", row[2].toString());
            json1.put("validcomId", row[1].toString());
            encListArray.put(json1);

            tempbidLineId = row[3].toString();
            Amount = new BigDecimal(json1.getString("encamount"));
            totalAmount = Amount;

            json.put("totalamount", totalAmount);

          }
          // if tempbid line id is equals to current bid lines id then add the amount in total
          // amount
          else if (tempbidLineId != null && tempbidLineId.equals(row[3].toString())) {

            totalAmount = totalAmount.add(new BigDecimal(row[2].toString()));
            json.put("totalamount", totalAmount);
            // form the encum list if one bid line have multiple encum list
            json1 = new JSONObject();
            json1.put("encumId", row[0].toString());
            json1.put("encamount", row[2].toString());
            json1.put("validcomId", row[1].toString());
            encListArray.put(json1);
          }
          // if tempbid line id is null then form the json
          else {
            json = new JSONObject();
            json.put("bidlineId", row[3].toString());
            json.put("bidvalidcomId", row[4].toString());
            // form the encum list
            json1 = new JSONObject();
            json1.put("encumId", row[0].toString());
            json1.put("encamount", row[2].toString());
            json1.put("validcomId", row[1].toString());
            encListArray.put(json1);

            tempbidLineId = row[3].toString();
            Amount = new BigDecimal(json1.getString("encamount")); // diff -ve reduce enc(split &
                                                                   // merge) +ve increase , -ve dec(
                                                                   // type applied amount
                                                                   // decrease ( full qty used)
            totalAmount = Amount;

            json.put("totalamount", totalAmount);// split & mereg(proposal line total)
          }
          //
          if (UniqueCodejson != null && UniqueCodejson.has("Uniquecode")) {

            for (int i = 0; i < uniqueCodeListArray.length(); i++) {
              json2 = uniqueCodeListArray.getJSONObject(i);
              if (json2.getString("Uniquecode").equals(row[4].toString())) {
                json2.put("Amount", new BigDecimal(json2.getString("Amount"))
                    .add(new BigDecimal(row[2].toString())));
                linearraylist = json2.getJSONArray("lineList");
                bidLineJson = new JSONObject();
                bidLineJson.put("lineId", row[3].toString());
                linearraylist.put(bidLineJson);
                json2.put("lineList", linearraylist);
                sameUniqueCode = true;
                break;
              } else
                continue;
            }
          }
          if (!sameUniqueCode) {
            linearraylist = new JSONArray();
            if (!row[4].toString().equals(row[1].toString())) {
              UniqueCodejson = new JSONObject();
              UniqueCodejson.put("Uniquecode", row[4].toString());

              UniqueCodejson.put("Amount", row[2].toString());
              UniqueCodejson.put("isSummary", row[5].toString());
              bidLineJson = new JSONObject();
              bidLineJson.put("lineId", row[3].toString());
              linearraylist.put(bidLineJson);
              UniqueCodejson.put("lineList", linearraylist);
              uniqueCodeListArray.put(UniqueCodejson);
            } else if (row[4].toString().equals(row[1].toString())) {
              UniqueCodejson = new JSONObject();
              UniqueCodejson.put("Uniquecode", row[4].toString());

              UniqueCodejson.put("Amount", row[2].toString());
              UniqueCodejson.put("isSummary", row[5].toString());
              bidLineJson = new JSONObject();
              bidLineJson.put("lineId", row[3].toString());
              linearraylist.put(bidLineJson);
              UniqueCodejson.put("lineList", linearraylist);
              uniqueCodeListArray.put(UniqueCodejson);
            }
          }

          //
        }
        json.put("encList", encListArray);
        prlistArray.put(json);
        prResult.put("list", prlistArray);
        //
        // UniqueCodejson.put("lineList", linearraylist);
        // uniqueCodeListArray.put(UniqueCodejson);
        uniquecodeResult.put("uniquecodeList", uniqueCodeListArray);
      }
      result.put("prListarray", prResult);
      result.put("uniquecodeListarray", uniquecodeResult);
      log.debug("result12:" + result);
    } catch (Exception e) {
      log.error("Exception in getPRAmountBasedOnBRQty " + e.getMessage());
    }
    return result;
  }

  @SuppressWarnings("rawtypes")
  public static JSONObject getPRDetailsBasedOnProposalQty1(EscmProposalMgmt proposal,
      EscmProposalmgmtLine proposalmgmtline) {
    String strQuery = null;
    Query query = null;
    BigDecimal totalAmount = BigDecimal.ZERO;
    JSONObject prResult = new JSONObject(), json = null, json1 = null;
    JSONArray prlistArray = new JSONArray(), encListArray = new JSONArray();
    String tempbidLineId = null;
    JSONObject result = new JSONObject();
    BigDecimal reqamt = BigDecimal.ZERO, proposalAmt = BigDecimal.ZERO, diff = BigDecimal.ZERO;
    try {

      // calculate the qty amount corresponding PR linettoal
      strQuery = " select req.em_efin_budget_manencum_id , reqln.em_efin_c_validcombination_id,"
          + " case when coalesce(sum(reqln.qty),0) > 0 "
          + " then  sum(round((coalesce(reqln.priceactual,0)*coalesce(ref.quantity,0)),2)) "
          + " else 0 end  as amount ,ln.escm_proposalmgmt_line_id ,ln.em_efin_c_validcombination_id as provalicombid ,ln.issummarylevel ,ln.line_total "
          + " from  escm_proposalsource_ref ref join escm_proposalmgmt_line ln on ln.escm_proposalmgmt_line_id= ref.escm_proposalmgmt_line_id"
          + "  LEFT join m_requisitionline reqln on reqln.m_requisitionline_id= ref.m_requisitionline_id "
          + " LEFT join m_requisition req on req.m_requisition_id= reqln.m_requisition_id and req.em_efin_budget_manencum_id is not null"
          + " where ln.escm_proposalmgmt_id = ?  ";
      if (proposalmgmtline != null) {
        strQuery += " and ln.escm_proposalmgmt_line_id <> ? ";
      }
      strQuery += " and ln.issummarylevel  ='N' and (ln.status != 'CL' or ln.status is null) "
          + " group by ln.escm_proposalmgmt_line_id ,req.em_efin_budget_manencum_id , reqln.em_efin_c_validcombination_id ,ln.em_efin_c_validcombination_id"
          + " order by ln.escm_proposalmgmt_line_id  ";
      query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      query.setParameter(0, proposal.getId());
      if (proposalmgmtline != null) {
        query.setParameter(1, proposalmgmtline.getId());
      }
      log.debug("strQuery:" + query.toString());
      List queryList = query.list();
      if (queryList != null && queryList.size() > 0) {
        for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          reqamt = BigDecimal.ZERO;
          proposalAmt = BigDecimal.ZERO;
          diff = BigDecimal.ZERO;

          if (row[2] != null)
            reqamt = new BigDecimal(row[2].toString());
          if (row[6] != null)
            proposalAmt = new BigDecimal(row[6].toString());
          diff = proposalAmt.subtract(reqamt);

          // if tempbid line id is equals to current bid lines id then add the amount in total
          // amount
          if (tempbidLineId != null && tempbidLineId.equals(row[3].toString())) {

            // form the encum list if one bid line have multiple encum list
            json1 = new JSONObject();
            json1.put("encumId", row[0].toString());
            json1.put("validcomId", row[1].toString());
            if (diff.compareTo(BigDecimal.ZERO) == 0) {
              json1.put("encamount", row[2].toString());
            }
            // increase
            else if (diff.compareTo(BigDecimal.ZERO) > 0) {
              json1.put("encamount", row[2].toString());
            }
            // decrease
            else {
              json1.put("encamount", row[2].toString());// proposalAmt
            }
            json1.put("totalamount", row[6].toString());
            encListArray.put(json1);
          }
          // if tempbid line id is null then form the json
          else {
            encListArray = new JSONArray();
            json = new JSONObject();
            json.put("bidlineId", row[3].toString());
            json.put("bidvalidcomId", row[4].toString());
            // form the encum list

            // equal
            json1 = new JSONObject();
            json1.put("encumId", (row[0] == null ? null : row[0].toString()));
            json1.put("validcomId", (row[1] == null ? null : row[1].toString()));
            if (diff.compareTo(BigDecimal.ZERO) == 0) {
              json1.put("encamount", row[2].toString());
            }
            // increase
            else if (diff.compareTo(BigDecimal.ZERO) > 0) {
              json1.put("encamount", row[2].toString());
            }
            // decrease
            else {
              json1.put("encamount", row[2].toString());// proposalAmt
            }
            encListArray.put(json1);
            tempbidLineId = row[3].toString();
            totalAmount = proposalAmt;
            json.put("totalamount", totalAmount);
            json.put("encList", encListArray);
            prlistArray.put(json);

          }
        }
        prResult.put("list", prlistArray);
      }
      result.put("prListarray", prResult);
      log.debug("result:" + result);
    } catch (Exception e) {
      log.error("Exception in getPRAmountBasedOnBRQty " + e.getMessage());
    }
    return result;
  }

  /**
   * Method to get unique-code list for checking funds
   * 
   * @param proposal
   * @param encumbrance
   * @return
   */
  @SuppressWarnings("rawtypes")
  public static JSONObject getUniqueCodeListforFundschk(EscmProposalMgmt proposal,
      EfinBudgetManencum encumbrance) {
    JSONObject result = new JSONObject(), UniqueCodejson = null, bidLineJson = null,
        uniquecodeResult = new JSONObject(), commonvalresult = null;
    Query query = null;
    BigDecimal proposalAmt = BigDecimal.ZERO;
    Boolean isComPresentInReq = false;
    BigDecimal reqAmount = BigDecimal.ZERO, diff = BigDecimal.ZERO;
    List<EscmProposalmgmtLine> linelist = new ArrayList<EscmProposalmgmtLine>();
    JSONArray linearraylist = new JSONArray(), uniqueCodeListArray = new JSONArray();
    Boolean errorFlag = false;
    try {

      String prosallineQry = "   select   sum(ln.line_total),ln.em_efin_c_validcombination_id  from escm_proposalmgmt_line ln  where ln.escm_proposalmgmt_id= ?   "
          + "            and ln.issummarylevel  ='N'  group by ln.em_efin_c_validcombination_id  ";
      query = OBDal.getInstance().getSession().createSQLQuery(prosallineQry);
      query.setParameter(0, proposal.getId());
      log.debug("strQuery:" + query.toString());
      List queryList = query.list();
      if (queryList != null && queryList.size() > 0) {
        for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          isComPresentInReq = false;
          AccountingCombination com = OBDal.getInstance().get(AccountingCombination.class,
              row[1].toString());

          if (row[0] != null)
            proposalAmt = new BigDecimal(row[0].toString());

          String reqlnQry = "   select case when coalesce(sum(reqln.qty),0) > 0   "
              + "          then  sum(round((coalesce(reqln.priceactual,0)*coalesce(ref.quantity,0)),2))   else 0 end  as amount ,reqln.em_efin_c_validcombination_id  from escm_proposalsource_ref ref "
              + "           join escm_proposalmgmt_line ln on ln.escm_proposalmgmt_line_id= ref.escm_proposalmgmt_line_id  and ln.issummarylevel  ='N' "
              + "           join m_requisitionline reqln on reqln.m_requisitionline_id= ref.m_requisitionline_id "
              + "          and reqln.em_escm_issummary='N' " + " where ln .escm_proposalmgmt_id= ? "
              + "    and reqln.em_efin_c_validcombination_id=  ? group by reqln.em_efin_c_validcombination_id ";
          query = OBDal.getInstance().getSession().createSQLQuery(reqlnQry);
          query.setParameter(0, proposal.getId());
          query.setParameter(1, row[1].toString());
          log.debug("strQuery1:" + query.toString());
          List reqqueryList = query.list();
          if (reqqueryList != null && reqqueryList.size() > 0) {
            for (Iterator reqiterator = reqqueryList.iterator(); reqiterator.hasNext();) {
              Object[] reqrow = (Object[]) reqiterator.next();
              linearraylist = new JSONArray();
              isComPresentInReq = true;
              if (reqrow[0] != null) {
                reqAmount = new BigDecimal(reqrow[0].toString());
              }
              diff = proposalAmt.subtract(reqAmount);

              // increase
              if (diff.compareTo(BigDecimal.ZERO) > 0) {
                UniqueCodejson = new JSONObject();
                UniqueCodejson.put("Uniquecode", row[1].toString());
                UniqueCodejson.put("Amount", diff);

                commonvalresult = CommonValidationsDAO
                    .CommonFundsChecking(encumbrance.getBudgetInitialization(), com, diff);

                if (commonvalresult.getString("errorFlag").equals("0")) {
                  errorFlag = true;
                  OBQuery<EscmProposalmgmtLine> prosallnQry = OBDal.getInstance().createQuery(
                      EscmProposalmgmtLine.class, " as e where e.escmProposalmgmt.id=:proposalID "
                          + "and e.eFINUniqueCode.id=:uniqCodeID");
                  prosallnQry.setNamedParameter("proposalID", proposal.getId());
                  prosallnQry.setNamedParameter("uniqCodeID", row[1].toString());
                  linelist = prosallnQry.list();
                  if (linelist.size() > 0) {
                    for (EscmProposalmgmtLine line : linelist) {
                      bidLineJson = new JSONObject();
                      bidLineJson.put("lineId", line.getId());
                      linearraylist.put(bidLineJson);
                      line.setEfinFailureReason(commonvalresult.getString("message"));
                      OBDal.getInstance().save(line);
                    }
                  }
                  UniqueCodejson.put("lineList", linearraylist);
                  uniqueCodeListArray.put(UniqueCodejson);
                }
              }
            }
          }
          if (!isComPresentInReq) {
            UniqueCodejson = new JSONObject();
            UniqueCodejson.put("Uniquecode", row[1].toString());
            UniqueCodejson.put("Amount", proposalAmt);

            commonvalresult = CommonValidationsDAO
                .CommonFundsChecking(encumbrance.getBudgetInitialization(), com, proposalAmt);
            if (commonvalresult.getString("errorFlag").equals("0")) {
              errorFlag = true;
              OBQuery<EscmProposalmgmtLine> prosallnQry = OBDal.getInstance().createQuery(
                  EscmProposalmgmtLine.class, " as e where e.escmProposalmgmt.id=:proposalID "
                      + " and e.eFINUniqueCode.id=:uniqCodeID ");
              prosallnQry.setNamedParameter("proposalID", proposal.getId());
              prosallnQry.setNamedParameter("uniqCodeID", row[1].toString());
              linelist = prosallnQry.list();
              if (linelist.size() > 0) {
                for (EscmProposalmgmtLine line : linelist) {
                  bidLineJson = new JSONObject();
                  bidLineJson.put("lineId", line.getId());
                  linearraylist.put(bidLineJson);
                  line.setEfinFailureReason(commonvalresult.getString("message"));
                  OBDal.getInstance().save(line);
                }
              }
              UniqueCodejson.put("lineList", linearraylist);
              uniqueCodeListArray.put(UniqueCodejson);
            }
          }
        }
        uniquecodeResult.put("uniquecodeList", uniqueCodeListArray);
      }
      result.put("uniquecodeListarray", uniquecodeResult);
      result.put("errorflag", errorFlag);
      log.debug("result:" + result);
    } catch (Exception e) {
      log.error("Exception in getUniqueCodeListforFundschk " + e.getMessage());
    }
    return result;
  }

  /**
   * check associated PR in Bid Full Qty used or partialy used or combine more than one Encumbrance
   * 
   * @param bidmgmt
   * @return Jsonobject of Encumbrance List, (Type-Split or Merge),PR is associated or Not
   */
  public static JSONObject checkFullPRQtyUitlizeorNot(EscmProposalMgmt proposal) {
    List<Requisition> req = new ArrayList<Requisition>();
    List<EfinBudgetManencum> enc = new ArrayList<EfinBudgetManencum>();
    boolean isAssociatePREncumbrance = false;
    int srcrefReqLineCount = 0, reqLineCount = 0, encReqCount = 0, reqCount = 0;
    BigDecimal srcrefLineQty = BigDecimal.ZERO, reqLineQty = BigDecimal.ZERO,
        reqlineAmt = BigDecimal.ZERO;
    Boolean isLineCountSame = true, isLineQtySame = true, ismanualLine = false,
        isEncReqCountSame = true, isEncumAppAmtZero = true;
    // Boolean isLineUniqCodeSame = true;
    JSONObject result = new JSONObject();
    try {

      if (proposal != null) {
        if (proposal.getEscmProposalmgmtLineList().size() > 0) {
          for (EscmProposalmgmtLine line : proposal.getEscmProposalmgmtLineList()) {
            if (!line.isSummary() && !line.isManual()) {
              if (line.getStatus() == null || line.getStatus().equals("null")) {
                if (line.getEscmProposalsourceRefList().size() > 0) {
                  for (EscmProposalsourceRef srcrefObj : line.getEscmProposalsourceRefList()) {
                    // chk source ref having purchase requisition and corresponding purchase
                    // requistion
                    // is PRE(Purchase Encumbrance Type)
                    if (srcrefObj.getRequisition() != null
                        && srcrefObj.getRequisition().getEfinBudgetManencum() != null
                        && srcrefObj.getRequisition().getEfinBudgetManencum().getEncumType()
                            .equals("PRE")) {
                      // if PR is associated then set the flag as true
                      if (srcrefObj.getRequisition().isEfinSkipencumbrance()) {
                        break;
                      } else {
                        isAssociatePREncumbrance = true;
                      }
                      if (isAssociatePREncumbrance) {

                        // if (srcrefObj.getRequisitionLine().getEfinCValidcombination() != null
                        // && line.getEFINUniqueCode() != null
                        // && !srcrefObj.getRequisitionLine().getEfinCValidcombination().getId()
                        // .equals(line.getEFINUniqueCode().getId())) {
                        // isLineUniqCodeSame = false;
                        // }

                        // forming encumbrance and req List based on Bid Line Source Reference
                        if (enc != null
                            && !enc.contains(srcrefObj.getRequisition().getEfinBudgetManencum()))
                          enc.add(srcrefObj.getRequisition().getEfinBudgetManencum());
                        else
                          enc.add(srcrefObj.getRequisition().getEfinBudgetManencum());
                        if (enc != null
                            && !enc.contains(srcrefObj.getRequisition().getEfinBudgetManencum()))
                          req.add(srcrefObj.getRequisition());
                        else
                          req.add(srcrefObj.getRequisition());
                      }
                    }
                  }
                }
              }
            } else if (!line.isSummary() && line.isManual()
                && (line.getStatus() == null || line.getStatus().equals("null"))) {
              ismanualLine = true;
            }
          }
        }
      }
      // avoid the encumbrance duplicate
      HashSet<EfinBudgetManencum> encumset = new HashSet<EfinBudgetManencum>(enc);
      if (encumset != null && encumset.size() == 1 && isAssociatePREncumbrance) {
        HashSet<Requisition> requisition = new HashSet<Requisition>(req);
        Iterator<Requisition> iterator = requisition.iterator();
        String encumId = encumset.iterator().next().getId();
        // itereate the Requisition List
        while (iterator.hasNext()) {
          Requisition reqObj = iterator.next();
          // get the Requisition Line count
          reqLineCount = reqObj.getProcurementRequisitionLineList().size();
          OBQuery<EscmProposalsourceRef> srcref = OBDal.getInstance()
              .createQuery(EscmProposalsourceRef.class, " as e where e.escmProposalmgmtLine.id in"
                  + " ( select b.id from Escm_Proposalmgmt_Line b where b.escmProposalmgmt.id=:proposalID and (b.status<>'CL' or b.status is null)) "
                  + " and e.requisition.id=:reqID");
          srcref.setNamedParameter("proposalID", proposal.getId());
          srcref.setNamedParameter("reqID", reqObj.getId());

          log.debug("srcref:" + srcref.getWhereAndOrderBy());

          // get the source ref Requisition Line count in Bid
          srcrefReqLineCount = srcref.list().size();

          // if count is not same set the flag of "isLineCountSame" is False
          if (srcrefReqLineCount != reqLineCount) {
            isLineCountSame = false;
          }
        }
        // if count is same then check full qty used in each Requisition Line
        if (isLineCountSame) {
          Iterator<Requisition> iteratorreq = requisition.iterator();
          while (iteratorreq.hasNext()) {
            Requisition reqObj = iteratorreq.next();
            for (RequisitionLine line : reqObj.getProcurementRequisitionLineList()) {
              if (!line.isEscmIssummary()) {
                // get the each requisition line qty
                srcrefLineQty = line.getQuantity();

                // get the source ref requisition line qty
                OBQuery<EscmProposalsourceRef> srcref = OBDal.getInstance().createQuery(
                    EscmProposalsourceRef.class,
                    " as e where e.escmProposalmgmtLine.id in "
                        + " ( select b.id from Escm_Proposalmgmt_Line b where b.escmProposalmgmt.id=:proposalID and (b.status<>'CL' or b.status is null)) "
                        + " and e.requisition.id=:reqID and e.requisitionLine.id=:reqLnID ");
                srcref.setNamedParameter("proposalID", proposal.getId());
                srcref.setNamedParameter("reqID", reqObj.getId());
                srcref.setNamedParameter("reqLnID", line.getId());
                srcref.setMaxResult(1);
                log.debug("srcrefs:" + srcref);
                if (srcref.list().size() > 0) {
                  reqLineQty = srcref.list().get(0).getReservedQuantity();
                  reqlineAmt = reqlineAmt.add(reqLineQty.multiply(line.getUnitPrice()));
                }
                // if req line qty and src ref line qty is not same then set the flag of
                // isLineQtySame
                // is "false"
                if (reqLineQty.compareTo(srcrefLineQty) != 0) {
                  isLineQtySame = false;
                }
              }
            }
          }
        } else {
          isLineQtySame = false;
        }

        if (encumId != null) {

          EfinBudgetManencum encumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
              encumId);
          BigDecimal remainigAmt = encumbrance.getEfinBudgetManencumlinesList().stream()
              .map(a -> a.getRemainingAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

          if (encumbrance.getRevamount().compareTo(encumbrance.getAppliedAmount()) != 0) {
            if (reqlineAmt.compareTo((encumbrance.getAppliedAmount().add(remainigAmt))) == 0) {
              isEncumAppAmtZero = true;
            } else {
              isEncumAppAmtZero = false;
            }
          }

          BigDecimal diff = proposal.getTotalamount().subtract(reqlineAmt);
          if (diff.compareTo(BigDecimal.ZERO) > 0 && remainigAmt.compareTo(diff) == 0) {
            isEncumAppAmtZero = true;
          }

          if (isEncumAppAmtZero) {
            OBQuery<Requisition> reqcount = OBDal.getInstance().createQuery(Requisition.class,
                " as e where e.efinBudgetManencum.id=:encumID and e.escmDocStatus  not in ('ESCM_CA') ");
            reqcount.setNamedParameter("encumID", encumId);
            if (reqcount.list().size() > 0) {
              encReqCount = reqcount.list().size();
            }
            reqCount = requisition.size();
            if (reqCount != encReqCount) {
              isEncReqCountSame = false;
            }
          }
        }

        // if line qty same set isFullQtyUsed as "true" && isLineUniqCodeSame
        if (isLineQtySame && isEncumAppAmtZero && isEncReqCountSame && !ismanualLine) {
          result.put("encumbrance", encumId);
          result.put("isFullQtyUsed", true);
          result.put("isLineCountSame", true);
          result.put("encumbrance", encumId);
        }
        // if line qty not same set isFullQtyUsed as "False" and encumbrance list is more than one
        // set the type as "MERGE" or else "SPLIT"
        else {
          result.put("isFullQtyUsed", false);
          if (isLineCountSame) {
            result.put("isLineCountSame", true);
          } else
            result.put("isLineCountSame", false);
          if (encumset != null && encumset.size() == 1) {
            result.put("encumbrance", encumId);
            result.put("type", "SPLIT");
          }
        }
      } else if (encumset != null && encumset.size() > 1) {
        String encumId = encumset.iterator().next().getId();
        result.put("isFullQtyUsed", false);
        result.put("type", "MERGE");
        result.put("encumbrance", encumId);
      }
      result.put("isAssociatePREncumbrance", isAssociatePREncumbrance);
    } catch (Exception e) {
      log.error("Exception in checkFullPRAmtUitlizeorNot " + e.getMessage());
      return result;
    }
    return result;
  }

  @SuppressWarnings("rawtypes")
  public static Boolean chkAndUpdateforProposalPRFullQty(EscmProposalMgmt proposal,
      EfinBudgetManencum encumbrance, Boolean isChkFundsAppliedAmt, Boolean isreject) {
    JSONObject commonvalresult = null;
    Query query = null;
    BigDecimal proposalAmt = BigDecimal.ZERO;
    BigDecimal reqAmount = BigDecimal.ZERO, diff = BigDecimal.ZERO, appliedAmt = BigDecimal.ZERO,
        usedAmt = BigDecimal.ZERO, revAmt = BigDecimal.ZERO, unappAmt = BigDecimal.ZERO;
    List<EscmProposalmgmtLine> linelist = new ArrayList<EscmProposalmgmtLine>();
    Boolean errorFlag = false, errorunUsedFlag = false;
    List<EfinBudgetManencumlines> enclinelist = new ArrayList<EfinBudgetManencumlines>();
    EfinBudgetManencumlines line = null;
    String encumType = null;
    if (encumbrance != null)
      encumType = encumbrance.getEncumMethod();
    try {

      if (isChkFundsAppliedAmt) {
        linelist = proposal.getEscmProposalmgmtLineList();
        for (EscmProposalmgmtLine ln : linelist) {
          ln.setEfinFailureReason(null);
          OBDal.getInstance().save(ln);
        }
        linelist = null;
      }

      String prosallineQry = "   select   sum(ln.line_total),ln.em_efin_c_validcombination_id  from escm_proposalmgmt_line ln  where ln.escm_proposalmgmt_id= ?   "
          + "            and ln.issummarylevel  ='N' and (ln.status != 'CL' or ln.status is null)  group by ln.em_efin_c_validcombination_id  ";
      query = OBDal.getInstance().getSession().createSQLQuery(prosallineQry);
      query.setParameter(0, proposal.getId());
      log.debug("strQuery:" + query.toString());
      List queryList = query.list();
      if (queryList != null && queryList.size() > 0) {
        for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          AccountingCombination com = OBDal.getInstance().get(AccountingCombination.class,
              row[1].toString());

          if (row[0] != null)
            proposalAmt = new BigDecimal(row[0].toString());

          OBQuery<EscmProposalmgmtLine> prosallnQry = OBDal.getInstance().createQuery(
              EscmProposalmgmtLine.class, " as e where e.escmProposalmgmt.id=:proposalID "
                  + " and e.eFINUniqueCode.id=:uniqcodeID and (e.status != 'CL' or e.status is null)  ");
          prosallnQry.setNamedParameter("proposalID", proposal.getId());
          prosallnQry.setNamedParameter("uniqcodeID", row[1].toString());
          linelist = prosallnQry.list();

          String reqlnQry = "   select case when coalesce(sum(reqln.qty),0) > 0   "
              + "          then  sum(round((coalesce(reqln.priceactual,0)*coalesce(ref.quantity,0)),2))   else 0 end  as amount ,reqln.em_efin_c_validcombination_id  from escm_proposalsource_ref ref "
              + "           join escm_proposalmgmt_line ln on ln.escm_proposalmgmt_line_id= ref.escm_proposalmgmt_line_id  and ln.issummarylevel  ='N' and (ln.status != 'CL' or ln.status is null)  "
              + "           join m_requisitionline reqln on reqln.m_requisitionline_id= ref.m_requisitionline_id "
              + "          and reqln.em_escm_issummary='N' where ln.escm_proposalmgmt_id= ? "
              + "    and reqln.em_efin_c_validcombination_id=  ? group by reqln.em_efin_c_validcombination_id ";
          query = OBDal.getInstance().getSession().createSQLQuery(reqlnQry);
          query.setParameter(0, proposal.getId());
          query.setParameter(1, row[1].toString());
          log.debug("strQuery:" + query.toString());
          List reqqueryList = query.list();
          if (reqqueryList != null && reqqueryList.size() > 0) {
            for (Iterator reqiterator = reqqueryList.iterator(); reqiterator.hasNext();) {
              Object[] reqrow = (Object[]) reqiterator.next();
              if (reqrow[0] != null) {
                reqAmount = new BigDecimal(reqrow[0].toString());
              }
              diff = proposalAmt.subtract(reqAmount);

              OBQuery<EfinBudgetManencumlines> enclineQry = OBDal.getInstance().createQuery(
                  EfinBudgetManencumlines.class,
                  " as e where e.manualEncumbrance.id=:encumID and e.accountingCombination.id=:uniqcodeID");
              enclineQry.setNamedParameter("encumID", encumbrance.getId());
              enclineQry.setNamedParameter("uniqcodeID", row[1].toString());
              enclineQry.setMaxResult(1);
              enclinelist = enclineQry.list();
              if (enclinelist.size() > 0) {
                enclinelist = enclineQry.list();
                line = enclinelist.get(0);
                appliedAmt = line.getAPPAmt();
                usedAmt = line.getUsedAmount();
                revAmt = line.getRevamount();
                unappAmt = (revAmt.subtract(appliedAmt)).subtract(usedAmt);
              }
              if (diff.compareTo(BigDecimal.ZERO) == 0) {
                continue;
              }
              // increase
              if (diff.compareTo(BigDecimal.ZERO) > 0) {
                // Auto- check funds available ( increase) - approve
                if (encumType.equals("A")) {
                  if (isChkFundsAppliedAmt && !isreject) {
                    commonvalresult = CommonValidationsDAO
                        .CommonFundsChecking(encumbrance.getBudgetInitialization(), com, diff);

                    if (commonvalresult.getString("errorFlag").equals("0")) {
                      errorFlag = true;
                      if (linelist.size() > 0) {
                        for (EscmProposalmgmtLine prlline : linelist) {
                          prlline.setEfinFailureReason(commonvalresult.getString("message"));
                          OBDal.getInstance().save(prlline);
                        }
                      }
                    }
                  }
                } else if (encumType.equals("M")) {
                  // Manual- check applied amount ( increase) - approve
                  if (isChkFundsAppliedAmt && !isreject) {
                    if (diff.compareTo(unappAmt) > 0) {
                      errorFlag = true;
                      if (linelist.size() > 0) {
                        for (EscmProposalmgmtLine lines : linelist) {
                          lines.setEfinFailureReason(OBMessageUtils.messageBD("Efin_ReqAmt_More"));
                          OBDal.getInstance().save(lines);
                        }
                      }
                    }
                  }
                }
                // update applied amt,rev amount in case of approve for both manual and auto (
                // reject case increase is decrease and decrease is increase)
                if (!isChkFundsAppliedAmt) {
                  if (isreject)
                    diff = diff.negate();
                  line.setAPPAmt(line.getAPPAmt().add(diff));
                  if (encumType.equals("A")) {
                    /*
                     * Trigger changes if (!isreject)
                     * line.setENCIncrease(line.getENCIncrease().add(diff));
                     * 
                     * line.setRevamount(line.getRevamount().add(diff));
                     * line.setRemainingAmount(line.getRemainingAmount().add(diff));
                     * OBDal.getInstance().save(line);
                     */

                  }
                }
              }
              // decrease
              else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                // update applied amt and revamount in case of approve both manual and auto ( reject
                // case increase is decrease and decrease is increase)
                if (!isChkFundsAppliedAmt) {
                  if (isreject)
                    diff = diff.negate();
                  line.setAPPAmt(line.getAPPAmt().add(diff));
                  if (encumType.equals("A")) {
                  }
                }
                // Auto - check while reject funds available when the time of decrease
                if (isChkFundsAppliedAmt && isreject) {
                  if (encumType.equals("A")) {
                    commonvalresult = CommonValidationsDAO.CommonFundsChecking(
                        encumbrance.getBudgetInitialization(), com, diff.negate());

                    if (commonvalresult.getString("errorFlag").equals("0")) {
                      errorFlag = true;
                      if (linelist.size() > 0) {
                        for (EscmProposalmgmtLine prlline : linelist) {
                          prlline.setEfinFailureReason(commonvalresult.getString("message"));
                          OBDal.getInstance().save(prlline);
                        }
                      }
                    }
                  } else {
                    // Manual - check while reject applied amount for encumbrance when the time of
                    // decrease
                    if (diff.negate().compareTo(unappAmt) > 0) {
                      errorFlag = true;
                      if (linelist.size() > 0) {
                        for (EscmProposalmgmtLine lines : linelist) {
                          lines.setEfinFailureReason(OBMessageUtils.messageBD("Efin_ReqAmt_More"));
                          OBDal.getInstance().save(lines);
                        }
                      }
                    }
                  }
                }
              }
              // auto means update budget inquiry and reflect the modification
              if (encumType.equals("A") && !isChkFundsAppliedAmt) {

                // while approve the auto insert the modification for both increase and decrease
                if (!isreject)
                  // insert encumbrance modification
                  BidManagementDAO.insertEncumbranceModification(line, diff, null, "PRO", null,
                      null);
                // while reject delete the modification
                if (isreject) {
                  diff = diff.negate();
                  ProposalManagementRejectMethods.deleteModification(line, diff);
                }
                // if reject take increase as decrease and decrease as increase to update the budget
                // enquiry
                if (isreject)
                  diff = diff.negate();
                // update budget inquiry Trigger changes
              }
            }
          } else {
            if (isChkFundsAppliedAmt && !isreject) {

              if (encumType.equals("A")) {
                commonvalresult = CommonValidationsDAO
                    .CommonFundsChecking(encumbrance.getBudgetInitialization(), com, proposalAmt);

                if (commonvalresult.getString("errorFlag").equals("0")) {
                  errorFlag = true;
                  if (linelist.size() > 0) {
                    for (EscmProposalmgmtLine prlline : linelist) {
                      prlline.setEfinFailureReason(commonvalresult.getString("message"));
                      OBDal.getInstance().save(prlline);
                    }
                  }
                }
              } else {
                if (!isreject) {
                  for (EscmProposalmgmtLine prlline : linelist) {
                    errorFlag = true;
                    prlline
                        .setEfinFailureReason(OBMessageUtils.messageBD("EFIN_PropNewUniqNotAllow"));
                    OBDal.getInstance().save(prlline);
                  }
                } else {
                }

              }
            } else {
              if (encumType.equals("A") && !isChkFundsAppliedAmt) {
                if (!isreject) {
                  insertEncumbranceLines(encumbrance, proposalAmt, com, proposal, null);
                } else {
                  // delete the encumbrance lines
                  ProposalManagementRejectMethods.deleteEncumLines(encumbrance, com, proposal,
                      null);
                }
              }
            }
          }
        }
      }
      if (isChkFundsAppliedAmt) {
        if (encumType.equals("A")) {
          if (isreject) {
            errorunUsedFlag = unusedEncumbranceUniquecodeUpdation(proposal, encumbrance, true, true,
                null);
          }
        } else {
          if (isreject) {
            errorunUsedFlag = unusedEncumbranceUniquecodeUpdation(proposal, encumbrance, true, true,
                null);
          }
        }
      } else {
        if (encumType.equals("A")) {
          if (!isreject) {
            unusedEncumbranceUniquecodeUpdation(proposal, encumbrance, false, false, null);
          } else {
            unusedEncumbranceUniquecodeUpdation(proposal, encumbrance, false, true, null);
          }
        }
      }
      if (errorunUsedFlag || errorFlag) {
        return true;
      }
      return errorFlag;
    } catch (Exception e) {
      log.error("Exception in getUniqueCodeListforFundschk " + e.getMessage());
    }
    return errorFlag;
  }

  @SuppressWarnings({ "rawtypes", "unused" })
  public static Boolean unusedEncumbranceUniquecodeUpdation(EscmProposalMgmt proposal,
      EfinBudgetManencum manencum, boolean appliedamtchk, boolean isreject,
      EscmProposalmgmtLine proposalmgmtline) {
    Query query = null;
    EfinBudgetManencumlines line = null;
    Boolean errorFlag = false, sameUniqueCode = false;
    JSONObject commonvalresult = null;
    String message = null;
    JSONObject uniquecodeList = new JSONObject(), uniquecodeJson = null, json1 = null,
        lineListJson = null;
    JSONArray uniqueCodeListArray = new JSONArray(), lineListArray = new JSONArray();
    try {
      if ((proposal != null && proposal.getEfinEncumbrance() != null
          && proposal.getEfinEncumbrance().getEncumMethod().equals("A"))
          || (proposalmgmtline != null
              && proposalmgmtline.getEscmProposalmgmt().getEfinEncumbrance() != null
              && proposalmgmtline.getEscmProposalmgmt().getEfinEncumbrance().getEncumMethod()
                  .equals("A"))) {
        // update amount for unused combination in encumbrance line
        /*
         * String strQry2 =
         * "  select c_validcombination_id,efin_budget_manencumlines_id,efin_budget_manencum_id, revamount"
         * +
         * "  from efin_budget_manencumlines   where efin_budget_manencum_id= ?  and (isauto  is null or isauto='N')  "
         * +
         * "               and c_validcombination_id not in ( select em_efin_c_validcombination_id from escm_proposalmgmt_line"
         * +
         * "  where escm_proposalmgmt_id= ? and issummarylevel='N'  and em_efin_c_validcombination_id is not null  )   "
         * ;
         */

        String strQry2 = " select reqln.em_efin_c_validcombination_id, ln.em_efin_c_validcombination_id as ordcombId,   "
            + "   ref.quantity*reqln.priceactual  as reqamt ,ln.line_total as proposalamt,        "
            + "   reqln.em_efin_bud_encumlines_id as reqmanlineid, ln.em_efin_budgmanencumline_id as ordmanlineid , "
            + "   ln.escm_proposalmgmt_line_id as orderlineid  from escm_proposalmgmt_line  ln  "
            + "   join escm_proposalsource_ref ref on ref.escm_proposalmgmt_line_id= ln.escm_proposalmgmt_line_id   "
            + "   join m_requisitionline reqln on reqln.m_requisitionline_id= ref.m_requisitionline_id  where ";
        if (proposal != null)
          strQry2 += " escm_proposalmgmt_id = ? ";
        else
          strQry2 += " ln.escm_proposalmgmt_line_id = ? ";
        strQry2 += "  and ln.issummarylevel='N'  and (ln.status != 'CL' or ln.status is null)  and ln.em_efin_c_validcombination_id is not null      "
            + " and reqln.em_efin_c_validcombination_id <> ln.em_efin_c_validcombination_id  "
            + " and reqln.em_efin_c_validcombination_id not in ( select em_efin_c_validcombination_id from escm_proposalmgmt_line where ";
        if (proposal != null)
          strQry2 += " escm_proposalmgmt_id= ?  ";
        else
          strQry2 += " escm_proposalmgmt_line_id = ? ";
        strQry2 += " ) order by reqln.em_efin_c_validcombination_id";
        query = OBDal.getInstance().getSession().createSQLQuery(strQry2);
        // query.setParameter(0, manencum.getId());
        if (proposal != null) {
          query.setParameter(0, proposal.getId());
          query.setParameter(1, proposal.getId());
        } else {
          query.setParameter(0, proposalmgmtline.getId());
          query.setParameter(1, proposalmgmtline.getId());
        }

        List unUsedUniqCodeList = query.list();
        if (unUsedUniqCodeList != null && unUsedUniqCodeList.size() > 0) {
          for (Iterator iterator = unUsedUniqCodeList.iterator(); iterator.hasNext();) {
            Object[] row = (Object[]) iterator.next();
            if (uniquecodeJson != null && uniquecodeJson.has("Uniquecode")) {
              for (int i = 0; i < uniqueCodeListArray.length(); i++) {
                json1 = uniqueCodeListArray.getJSONObject(i);
                if (json1.getString("Uniquecode").equals(row[0].toString())) {
                  json1.put("Amount", new BigDecimal(json1.getString("Amount"))
                      .add(new BigDecimal(row[2].toString())));
                  lineListJson = new JSONObject();
                  lineListJson.put("orderlineId", row[6].toString());
                  lineListArray.put(lineListJson);
                  json1.put("orderlinelist", lineListArray);
                  sameUniqueCode = true;
                  break;
                } else
                  continue;
              }
            }
            if (!sameUniqueCode) {
              uniquecodeJson = new JSONObject();
              uniquecodeJson.put("Uniquecode", row[0].toString());
              uniquecodeJson.put("Amount", new BigDecimal(row[2].toString()));
              uniquecodeJson.put("ManlineId", row[4].toString());
              lineListJson = new JSONObject();
              lineListJson.put("orderlineId", row[6].toString());
              lineListArray.put(lineListJson);
              uniquecodeJson.put("orderlinelist", lineListArray);
              uniqueCodeListArray.put(uniquecodeJson);
            }
          }
          uniquecodeList.put("uniquecodeList", uniqueCodeListArray);
        }

        if (uniquecodeList != null) {
          JSONArray array = uniquecodeList.getJSONArray("uniquecodeList");
          for (int i = 0; i < array.length(); i++) {
            json1 = array.getJSONObject(i);
            AccountingCombination acctcom = OBDal.getInstance().get(AccountingCombination.class,
                json1.getString("Uniquecode"));
            line = OBDal.getInstance().get(EfinBudgetManencumlines.class,
                json1.getString("ManlineId"));
            if (acctcom != null) {
              if (appliedamtchk && isreject) {
                commonvalresult = CommonValidationsDAO.CommonFundsChecking(
                    manencum.getBudgetInitialization(), acctcom,
                    new BigDecimal(json1.getString("Amount")));
                if (commonvalresult.getString("errorFlag").equals("0")) {
                  errorFlag = true;
                  JSONArray orderlineArray = json1.getJSONArray("orderlinelist");
                  for (int j = 0; j < orderlineArray.length(); j++) {
                    lineListJson = orderlineArray.getJSONObject(j);
                    message = OBMessageUtils.messageBD("EFIN_ForUniqueCode");
                    message = message.replace("%", acctcom.getEfinUniqueCode());
                    OrderLine ordln = OBDal.getInstance().get(OrderLine.class,
                        lineListJson.getString("orderlineId"));
                    ordln
                        .setEfinFailureReason(commonvalresult.getString("message") + "-" + message);
                    OBDal.getInstance().save(ordln);
                  }
                }
              }
              if (!isreject & !appliedamtchk) {
                BidManagementDAO.insertEncumbranceModification(line,
                    new BigDecimal(json1.getString("Amount")).negate(), null, "PRO", null, null);
                line.setAPPAmt(
                    line.getAPPAmt().add(new BigDecimal(json1.getString("Amount")).negate()));
              }
              if (isreject & !appliedamtchk) {
                ProposalManagementRejectMethods.deleteModification(line,
                    new BigDecimal(json1.getString("Amount")).negate());
                line.setAPPAmt(line.getAPPAmt().add(new BigDecimal(json1.getString("Amount"))));
              }
            }
          }
        }

      }
    } catch (Exception e) {
      log.error("Exception in unusedEncumbranceUniquecodeUpdation " + e.getMessage());
    }
    return errorFlag;
  }

  // Task No.5925
  /**
   * get Ecnumbrance Control "Enable" flag value value for process the Proposal Encumbrance Flow
   * 
   * @param proposalmgmt
   * @return EncumbranceControl List
   */
  public static List<EfinEncControl> getEncControleList(EscmProposalMgmt proposalmgmt) {
    List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
    Boolean fromPR = false;
    try {

      // check Proposal is associate with Purchase requisition
      for (EscmProposalmgmtLine line : proposalmgmt.getEscmProposalmgmtLineList()) {
        OBQuery<EscmProposalsourceRef> proposalsrcref = OBDal.getInstance().createQuery(
            EscmProposalsourceRef.class,
            "escmProposalmgmtLine.id=:proposalLnID and requisitionLine.id is not null");
        proposalsrcref.setNamedParameter("proposalLnID", line.getId());
        if (proposalsrcref.list() != null && proposalsrcref.list().size() > 0) {
          fromPR = true;
          break;
        }
      }

      // checking associate bid having encumbrance or proposal is associate with Purchase requistion
      // based on formPR flag
      if ((proposalmgmt.getEscmBidmgmt() != null
          && proposalmgmt.getEscmBidmgmt().getEncumbrance() != null
          && proposalmgmt.getEscmBidmgmt().getEncumbrance().getEncumType() != null) || fromPR) {

        // if proposal associate with bid then check corresponding bid encumbrance type is
        // "PRE-Purchase requistion encumbrance Type" or proposal associate with requisition
        // then check maintain encumbrance control PRE -enable is "Yes" or not. if yes then return
        // enc control list else return null
        if ((proposalmgmt.getEscmBidmgmt() != null
            && proposalmgmt.getEscmBidmgmt().getEncumbrance() != null
            && proposalmgmt.getEscmBidmgmt().getEncumbrance().getEncumType().equals("PRE"))
            || fromPR) {

          // check pr encumbrance type is enable or not
          OBQuery<EfinEncControl> encumcontrol = OBDal.getInstance().createQuery(
              EfinEncControl.class,
              " as e where e.encumbranceType='PRE' and e.client.id=:clientID and e.active='Y' ");
          encumcontrol.setNamedParameter("clientID", proposalmgmt.getClient().getId());
          encumcontrol.setFilterOnActive(true);
          encumcontrol.setMaxResult(1);
          enccontrollist = encumcontrol.list();

        }
        // if proposal associate with bid then check corresponding bid encumbrance type is "Bid
        // Encumbrance Type" then check
        // maintain encumbrance control of PAE -enable is "Yes" or not.
        else if (proposalmgmt.getEscmBidmgmt() != null
            && proposalmgmt.getEscmBidmgmt().getEncumbrance() != null
            && proposalmgmt.getEscmBidmgmt().getEncumbrance().getEncumType().equals("BE")) {

          // check proposal encumbrance type is enable or not .
          OBQuery<EfinEncControl> encumcontrol = OBDal.getInstance().createQuery(
              EfinEncControl.class,
              " as e where e.encumbranceType='PAE' and e.client.id=:clientID and e.active='Y' ");
          encumcontrol.setNamedParameter("clientID", proposalmgmt.getClient().getId());
          encumcontrol.setFilterOnActive(true);
          encumcontrol.setMaxResult(1);
          enccontrollist = encumcontrol.list();

        }
      }

      // bid is null and proposal created by manual encumbrance or auto and not created by purchase
      // requisition
      else if ((proposalmgmt.getEscmBidmgmt() != null
          && proposalmgmt.getEscmBidmgmt().getEncumbrance() == null)
          || (proposalmgmt.getEscmBidmgmt() == null && proposalmgmt.getEfinEncumbrance() != null
              && proposalmgmt.getEfinEncumbrance().getEncumType() != null
              && proposalmgmt.getEfinEncumbrance().getEncumType().equals("PAE"))
          || (proposalmgmt.getEscmBidmgmt() == null && proposalmgmt.getEfinEncumbrance() == null
              && !fromPR)) {

        // check proposal encumbrance type is enable or not ..
        OBQuery<EfinEncControl> encumcontrol = OBDal.getInstance().createQuery(EfinEncControl.class,
            " as e where e.encumbranceType='PAE' and e.client.id=:clientID and e.active='Y' ");
        encumcontrol.setNamedParameter("clientID", proposalmgmt.getClient().getId());
        encumcontrol.setFilterOnActive(true);
        encumcontrol.setMaxResult(1);
        enccontrollist = encumcontrol.list();

      }
      return enccontrollist;
    } catch (Exception e) {
      log.error("Exception in getEncControleList " + e.getMessage());
    }
    return enccontrollist;
  }

  /**
   * Method to check auto Encumbrance validation for PR
   * 
   * @param proposalmgmt
   * @return
   */
  public static JSONObject checkAutoEncumValidationForPR(EscmProposalMgmt proposalmgmt) {
    JSONObject result = new JSONObject();
    boolean errorFlag = false;
    try {
      result.put("errorflag", "1");
      result.put("errormsg", "null");
      String qurey = "select distinct val.c_salesregion_id from escm_proposalmgmt_line ln "
          + " join c_validcombination val on ln.em_efin_c_validcombination_id = val.c_validcombination_id "
          + " where escm_proposalmgmt_id =:prosId and ln.issummarylevel='N' and (ln.status != 'CL' or ln.status is null) ";
      SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(qurey);
      sqlQuery.setParameter("prosId", proposalmgmt.getId());
      if (sqlQuery.list() != null && sqlQuery.list().size() > 1) {
        result.put("errorflag", "0");
        result.put("errormsg", OBMessageUtils.messageBD("Efin_Req_SameDept"));
        return result;
      }
      // chk same budget type
      qurey = "select distinct val.c_campaign_id from escm_proposalmgmt_line ln "
          + "join c_validcombination val on ln.em_efin_c_validcombination_id = val.c_validcombination_id "
          + "where escm_proposalmgmt_id =:prosId and ln.issummarylevel='N' and (ln.status != 'CL' or ln.status is null)";
      sqlQuery = OBDal.getInstance().getSession().createSQLQuery(qurey);
      sqlQuery.setParameter("prosId", proposalmgmt.getId());
      if (sqlQuery.list() != null && sqlQuery.list().size() > 1) {
        result.put("errorflag", "0");
        result.put("errormsg", OBMessageUtils.messageBD("Efin_Req_SameBType"));
        return result;
      }
      // chk budget type validation for uniquecode-->cost means cost, funds means funds but
      // not
      // in cost.

      OBQuery<EscmProposalmgmtLine> proLine = OBDal.getInstance().createQuery(
          EscmProposalmgmtLine.class,
          "escmProposalmgmt.id=:proposalID and summary='N' and (status<>'CL' or status is null )");
      proLine.setNamedParameter("proposalID", proposalmgmt.getId());
      if (proLine.list() != null && proLine.list().size() > 0) {
        Campaign bType = proLine.list().get(0).getEFINUniqueCode().getSalesCampaign();
        if (bType.getEfinBudgettype().equals("F")) {
          errorFlag = ProposalManagementActionMethod.checkFundsNoCostValidation(proposalmgmt);
          if (errorFlag) {
            result.put("errorflag", "0");
            result.put("errormsg", OBMessageUtils.messageBD("Efin_Chk_Line_Info"));
            return result;
          }
        }
      }
      errorFlag = ProposalManagementActionMethod.chkAutoEncumbranceValidation(proposalmgmt, false,
          null, false, false, BigDecimal.ZERO);
      if (errorFlag) {
        result.put("errorflag", "0");
        result.put("errormsg", OBMessageUtils.messageBD("Efin_Chk_Line_Info"));
        return result;
      }
    } catch (Exception e) {
      log.error("Exception in checkAutoEncumValidationForPR " + e.getMessage());
    }
    return result;

  }

  /**
   * check associated pr having mixed encumbrance - means one pr having encumbrance and another pr
   * does not have encumnrance
   * 
   * @param bid
   * @return
   */
  @SuppressWarnings("finally")
  public static boolean checkmixedPREncumbrance(EscmProposalMgmt proposal) {
    List<EscmProposalmgmtLine> prosalLineList = new ArrayList<EscmProposalmgmtLine>();
    Boolean isskippedenc = false, isnotskippedenc = false, mixedencumbrance = false;
    try {
      OBContext.setAdminMode();
      prosalLineList = proposal.getEscmProposalmgmtLineList();
      if (prosalLineList.size() > 0) {
        for (EscmProposalmgmtLine lines : prosalLineList) {
          if (!lines.isSummary()) {
            for (EscmProposalsourceRef srcref : lines.getEscmProposalsourceRefList()) {
              if (srcref.getRequisition() != null) {
                if (srcref.getRequisition().isEfinSkipencumbrance()) {
                  isskippedenc = true;
                } else {
                  if (srcref.getRequisition().getEfinBudgetManencum() != null) {
                    isnotskippedenc = true;
                  } else {
                    isskippedenc = true;
                  }
                }
              }
            }
          }
        }
      }
      if (isnotskippedenc && isskippedenc) {
        mixedencumbrance = true;
        return mixedencumbrance;
      } else {
        return mixedencumbrance;
      }

    } catch (final Exception e) {
      log.error("Exception in checkmixedPREncumbrance  : ", e);
      return false;
    } finally {
      return mixedencumbrance;
    }
  }

  /**
   * copy unique code validation
   * 
   * @param proposalmgmt
   */
  public static void copyUniqueCodeValidation(EscmProposalMgmt proposalmgmt) {

    try {
      SQLQuery Query = OBDal.getInstance().getSession()
          .createSQLQuery("select  COUNT(Distinct(em_efin_c_validcombination_id)) as count,"
              + " em_efin_c_validcombination_id from escm_proposalmgmt_line "
              + " where escm_proposalmgmt_id=:proposalID and issummarylevel='N' and (status!='CL' or status is null)"
              + " group by em_efin_c_validcombination_id ");
      Query.setParameter("proposalID", proposalmgmt.getId());
      @SuppressWarnings("rawtypes")
      List prlinelist = Query.list();

      // if all line uniquecode is same
      if (Query != null && prlinelist.size() == 1) {
        Object[] prline = (Object[]) prlinelist.get(0);

        if (prline != null) {
          if (prline[1] != null) {
            String uniqueCode = prline[1].toString();

            // if all line uniquecode is same but not same as header unique code then update
            // header
            // uniquecode with line uniquecode value
            if (proposalmgmt.getEFINUniqueCode() != null) {
              if (!uniqueCode.equals(proposalmgmt.getEFINUniqueCode().getId())) {
                AccountingCombination acct = OBDal.getInstance().get(AccountingCombination.class,
                    uniqueCode);
                proposalmgmt.setEFINUniqueCode(acct);

              }
            } else {
              AccountingCombination acct = OBDal.getInstance().get(AccountingCombination.class,
                  uniqueCode);
              proposalmgmt.setEFINUniqueCode(acct);

            }
          }
        }
      }
      // if all line uniquecode is not same then make header uniquecode as null
      else if (Query != null && prlinelist.size() != 1) {
        proposalmgmt.setEFINUniqueCode(null);

      }

    } catch (Exception e) {
      log.error("Exception while copyUniqueCodeValidation", e);
      OBDal.getInstance().rollbackAndClose();
    }
  }

  /**
   * Revert Awarded to Analysis
   * 
   * @param proposalId
   * @param proposalAttrId
   * @param clientId
   * @param roleId
   * @param userId
   * @return true if process success else false.
   */
  public static boolean revertAwardedProposal(String proposalId, String proposalAttrId,
      String clientId, String roleId, String userId) {
    try {
      OBContext.setAdminMode();
      // changing status to Analysis
      EscmProposalMgmt proposalmgmt = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
      EscmProposalAttribute attr = null;
      proposalmgmt.setProposalstatus("ANY");
      proposalmgmt.setProposalappstatus("INC");
      if (proposalAttrId != null) {
        attr = OBDal.getInstance().get(EscmProposalAttribute.class, proposalAttrId);
      }
      OBDal.getInstance().save(proposalmgmt);
      // inserting action history
      JSONObject historyData = new JSONObject();
      historyData.put("ClientId", clientId);
      historyData.put("OrgId", proposalmgmt.getOrganization().getId());
      historyData.put("RoleId", roleId);
      historyData.put("UserId", userId);
      historyData.put("HeaderId", proposalId);
      historyData.put("Comments", "");
      historyData.put("Status", "ANY");
      historyData.put("NextApprover", "");
      historyData.put("HistoryTable", ApprovalTables.Proposal_Management_Status_History);
      historyData.put("HeaderColumn", ApprovalTables.Proposal_Management_History_HEADER_COLUMN);
      historyData.put("ActionColumn", ApprovalTables.Proposal_Management_History_DOCACTION_COLUMN);
      Utility.InsertApprovalHistory(historyData);
      OBDal.getInstance().flush();
      if (attr != null) {
        for (EscmProposalStatusHistory statusHist : proposalmgmt.getEscmProposalStatusHistList()) {
          statusHist.setEscmProposalAttr(attr);
        }
      }
      return true;
    } catch (final Exception e) {
      log.error("Exception in revertAwardedProposal: ", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static JSONObject getUniquecodeListforProposalVerAuto(EscmProposalMgmt proposal,
      EscmProposalMgmt baseProposal, boolean isreject, EscmProposalmgmtLine proposalLine) {
    JSONObject result = new JSONObject(), json = null, json1 = null;
    JSONArray arraylist = new JSONArray(), linearraylist = null;
    EfinBudgetManencum objEncum = proposal.getEfinEncumbrance();
    EfinBudgetManencumlines objEncumLine = null;
    List<EscmProposalmgmtLine> newProposalLineVerList = new ArrayList<EscmProposalmgmtLine>();
    OBQuery<EscmProposalmgmtLine> oldProposalLineQry = null;
    BigDecimal amount = BigDecimal.ZERO;
    String whereclause = "";
    try {
      if (proposal.getEscmOldproposal() != null) {
        whereclause = " as e where e.eFINUniqueCode is not null and e.escmProposalmgmt.id=:proposalId and e.summary ='N'  ";
        if (proposalLine != null) {
          whereclause += " and e.id=:proposallineId ";
        }
        whereclause += "  order by  e.eFINUniqueCode, e.lineNo asc ";

        OBQuery<EscmProposalmgmtLine> newProposallineQry = OBDal.getInstance()
            .createQuery(EscmProposalmgmtLine.class, whereclause);
        newProposallineQry.setNamedParameter("proposalId", proposal.getId());
        if (proposalLine != null) {
          newProposallineQry.setNamedParameter("proposallineId", proposalLine.getId());
        }
        newProposalLineVerList = newProposallineQry.list();

        // compare amount between old version and new version order line
        for (EscmProposalmgmtLine objProposalLine : newProposalLineVerList) {
          objProposalLine.setEfinFailureReason(null);
          OBDal.getInstance().save(objProposalLine);

          log.debug("desc:" + objProposalLine.getDescription());
          // get the orderline if product id is not null
          if (objProposalLine.getProduct() != null) {
            oldProposalLineQry = OBDal.getInstance().createQuery(EscmProposalmgmtLine.class,
                "as e where e.escmProposalmgmt.id=:oldProposalId and e.product.id=:productID "
                    + " and e.eFINUniqueCode.id=:uniquecodeID and e.lineNo=:lineno "
                    + " and e.cancel='N' ");

            oldProposalLineQry.setNamedParameter("oldProposalId",
                proposal.getEscmOldproposal().getId());
            oldProposalLineQry.setNamedParameter("productID", objProposalLine.getProduct().getId());
            oldProposalLineQry.setNamedParameter("uniquecodeID",
                objProposalLine.getEFINUniqueCode().getId());
            oldProposalLineQry.setNamedParameter("lineno", objProposalLine.getLineNo());
          }
          // get the orderline if description is not null
          else {
            oldProposalLineQry = OBDal.getInstance().createQuery(EscmProposalmgmtLine.class,
                "as e where e.escmProposalmgmt.id=:oldProposalId and e.description=:desc "
                    + " and e.eFINUniqueCode.id=:uniquecodeID and e.lineNo=:lineno "
                    + " and e.cancel='N'");

            oldProposalLineQry.setNamedParameter("oldProposalId",
                proposal.getEscmOldproposal().getId());
            oldProposalLineQry.setNamedParameter("desc", objProposalLine.getDescription());
            oldProposalLineQry.setNamedParameter("uniquecodeID",
                objProposalLine.getEFINUniqueCode().getId());
            oldProposalLineQry.setNamedParameter("lineno", objProposalLine.getLineNo());
            log.debug("desc:" + oldProposalLineQry.getWhereAndOrderBy());
          }
          if (oldProposalLineQry.list().size() > 0) {
            EscmProposalmgmtLine oldProposalLine = oldProposalLineQry.list().get(0);
            // get encumbrance details
            OBQuery<EfinBudgetManencumlines> manline = OBDal.getInstance().createQuery(
                EfinBudgetManencumlines.class,
                " as e where e.manualEncumbrance.id=:encumID and e.accountingCombination.id=:accID ");
            manline.setNamedParameter("encumID", objEncum.getId());
            manline.setNamedParameter("accID", oldProposalLine.getEFINUniqueCode().getId());

            manline.setMaxResult(1);
            if (manline.list().size() > 0) {
              objEncumLine = manline.list().get(0);
            }
            if (((objProposalLine.getLineTotal().compareTo(oldProposalLine.getLineTotal()) > 0)
                || (objProposalLine.getLineTotal().compareTo(oldProposalLine.getLineTotal()) < 0))
                && objEncumLine != null) {
              // do revision with increase or decrease amount
              amount = objProposalLine.getLineTotal().subtract(oldProposalLine.getLineTotal());
              if (isreject) {
                amount = amount.negate();
              }
              // json = new JSONObject();
              if (json != null && json.has("Uniquecode") && json.getString("Uniquecode")
                  .equals(objProposalLine.getEFINUniqueCode().getId())) {
                json.put("Amount", new BigDecimal(json.getString("Amount")).add(amount));
                json1 = new JSONObject();
                json1.put("lineId", objProposalLine.getId());
                linearraylist.put(json1);
                json.put("lineList", linearraylist);
              } else {
                if (json != null)
                  json.put("lineList", linearraylist);
                linearraylist = new JSONArray();
                json = new JSONObject();
                json.put("Uniquecode", objProposalLine.getEFINUniqueCode().getId());
                json.put("Amount", amount);
                json.put("isSummary", objProposalLine.isSummary());
                json1 = new JSONObject();
                json1.put("lineId", objProposalLine.getId());
                linearraylist.put(json1);
                arraylist.put(json);
              }
            }
          } else {
            // do revision with increase or decrease amount
            amount = objProposalLine.getLineTotal();
            // json = new JSONObject();
            if (json != null && json.has("Uniquecode") && json.getString("Uniquecode")
                .equals(objProposalLine.getEFINUniqueCode().getId())) {
              json.put("Amount", new BigDecimal(json.getString("Amount")).add(amount));
              json1 = new JSONObject();
              json1.put("lineId", objProposalLine.getId());
              linearraylist.put(json1);
              json.put("lineList", linearraylist);
            } else {
              if (json != null)
                json.put("lineList", linearraylist);
              linearraylist = new JSONArray();
              json = new JSONObject();
              json.put("Uniquecode", objProposalLine.getEFINUniqueCode().getId());
              json.put("Amount", amount);
              json.put("isSummary", objProposalLine.isSummary());
              json1 = new JSONObject();
              json1.put("lineId", objProposalLine.getId());
              linearraylist.put(json1);
              arraylist.put(json);
            }
          }
          if (linearraylist != null && linearraylist.length() > 0)
            json.put("lineList", linearraylist);
        }
        if (arraylist != null && arraylist.length() > 0)
          result.put("uniquecodeList", arraylist);
        log.debug("result:" + result);
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in getUniquecodeListforProposalVerAuto " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
      return result;
    }
    return result;
  }

  /**
   * This method is responsible to do modification in existing encumbrance
   * 
   * @param proposal
   * @param baseProposal
   */
  public static String doMofifcationInEncumbrance(EscmProposalMgmt proposal,
      EscmProposalMgmt baseProposal) {
    EfinBudgetManencum objEncum = proposal.getEfinEncumbrance();
    EfinBudgetManencumlines objEncumLine = null;
    OBQuery<EscmProposalmgmtLine> oldProposalLineQry = null;
    try {
      if (proposal.getEscmOldproposal() != null) {
        // compare amount between old version and new version order line
        for (EscmProposalmgmtLine objProposalLine : proposal.getEscmProposalmgmtLineList()) {
          if (!objProposalLine.isSummary() && objProposalLine.getStatus() == null) {
            // get the orderline if product id is not null
            if (objProposalLine.getProduct() != null) {
              oldProposalLineQry = OBDal.getInstance().createQuery(EscmProposalmgmtLine.class,
                  "as e where e.escmProposalmgmt.id=:oldProposalId and e.product.id=:productID "
                      + " and e.eFINUniqueCode.id=:uniquecodeID ");
              oldProposalLineQry.setNamedParameter("oldProposalId",
                  proposal.getEscmOldproposal().getId());
              oldProposalLineQry.setNamedParameter("productID",
                  objProposalLine.getProduct().getId());
              oldProposalLineQry.setNamedParameter("uniquecodeID",
                  objProposalLine.getEFINUniqueCode().getId());

            }
            // get the orderline if description is not null
            else {
              oldProposalLineQry = OBDal.getInstance().createQuery(EscmProposalmgmtLine.class,
                  "as e where e.escmProposalmgmt.id=:oldProposalId and e.description=:prdDesc "
                      + " and e.eFINUniqueCode.id=:uniquecodeID ");
              oldProposalLineQry.setNamedParameter("oldProposalId",
                  proposal.getEscmOldproposal().getId());
              oldProposalLineQry.setNamedParameter("prdDesc", objProposalLine.getDescription());
              oldProposalLineQry.setNamedParameter("uniquecodeID",
                  objProposalLine.getEFINUniqueCode().getId());

            }
            if (oldProposalLineQry.list().size() > 0) {
              EscmProposalmgmtLine oldProposalLine = oldProposalLineQry.list().get(0);
              // get encumbrance details

              OBQuery<EfinBudgetManencumlines> manline = OBDal.getInstance().createQuery(
                  EfinBudgetManencumlines.class,
                  " as e  where e.manualEncumbrance.id=:encumID and e.accountingCombination.id=:uniqID ");
              manline.setNamedParameter("encumID", objEncum.getId());
              manline.setNamedParameter("uniqID", objProposalLine.getEFINUniqueCode().getId());

              manline.setMaxResult(1);
              if (manline.list().size() > 0) {
                objEncumLine = manline.list().get(0);
              }

              if (((objProposalLine.getLineTotal().compareTo(oldProposalLine.getLineTotal()) > 0)
                  || (objProposalLine.getLineTotal().compareTo(oldProposalLine.getLineTotal()) < 0))
                  && objEncumLine != null) {
                // do revision with increase or decrease amount
                BigDecimal amount = objProposalLine.getLineTotal()
                    .subtract(oldProposalLine.getLineTotal());
                // objEncum.setRemainingamt(objEncum.getRemainingamt().add(amount));
                // OBDal.getInstance().save(objEncum);

                EfinBudManencumRev manEncumRev = OBProvider.getInstance()
                    .get(EfinBudManencumRev.class);

                if (!StringUtils.isEmpty(objEncumLine.getId())) {
                  // changes in Propsal encumbrance
                  doChangeMofifcationInProposalEncumbrance(proposal, objProposalLine, false);

                  // insert into Manual Encumbrance Revision Table
                  manEncumRev.setClient(OBContext.getOBContext().getCurrentClient());
                  manEncumRev.setOrganization(OBDal.getInstance().get(Organization.class,
                      objEncumLine.getOrganization().getId()));
                  manEncumRev.setActive(true);
                  manEncumRev.setUpdatedBy(OBContext.getOBContext().getUser());
                  manEncumRev.setCreationDate(new java.util.Date());
                  manEncumRev.setCreatedBy(OBContext.getOBContext().getUser());
                  manEncumRev.setUpdated(new java.util.Date());
                  manEncumRev.setUniqueCode(objEncumLine.getUniquecode());
                  manEncumRev.setManualEncumbranceLines(objEncumLine);
                  manEncumRev.setRevdate(new Date());
                  manEncumRev.setStatus("APP");
                  manEncumRev.setRevamount(amount);
                  manEncumRev.setEncumbranceType("MO");
                  manEncumRev.setAuto(true);
                  manEncumRev.setAccountingCombination(objEncumLine.getAccountingCombination());
                  OBDal.getInstance().save(manEncumRev);

                  objEncumLine.setAPPAmt(objEncumLine.getAPPAmt().add(amount));
                  OBDal.getInstance().save(objEncumLine);
                  proposal.setEfinIsbudgetcntlapp(true);
                  OBDal.getInstance().save(proposal);
                }
              }
            } else {
              OBQuery<EfinBudgetManencumlines> manline = OBDal.getInstance().createQuery(
                  EfinBudgetManencumlines.class,
                  " as e  where e.manualEncumbrance.id=:encumID and e.accountingCombination.id=:acctID ");
              manline.setNamedParameter("encumID", objEncum.getId());
              manline.setNamedParameter("acctID", objProposalLine.getEFINUniqueCode().getId());

              manline.setMaxResult(1);
              if (manline.list().size() > 0) {
                objEncumLine = manline.list().get(0);
              }
              EfinBudManencumRev manEncumRev = OBProvider.getInstance()
                  .get(EfinBudManencumRev.class);

              if (!StringUtils.isEmpty(objEncumLine.getId())) {
                // insert into Manual Encumbrance Revision Table
                manEncumRev.setClient(OBContext.getOBContext().getCurrentClient());
                manEncumRev.setOrganization(OBDal.getInstance().get(Organization.class,
                    objEncumLine.getOrganization().getId()));
                manEncumRev.setActive(true);
                manEncumRev.setUpdatedBy(OBContext.getOBContext().getUser());
                manEncumRev.setCreationDate(new java.util.Date());
                manEncumRev.setCreatedBy(OBContext.getOBContext().getUser());
                manEncumRev.setUpdated(new java.util.Date());
                manEncumRev.setUniqueCode(objEncumLine.getUniquecode());
                manEncumRev.setManualEncumbranceLines(objEncumLine);
                manEncumRev.setRevdate(new Date());
                manEncumRev.setStatus("APP");
                manEncumRev.setRevamount(objProposalLine.getLineTotal());
                manEncumRev.setEncumbranceType("MO");
                manEncumRev.setAuto(true);
                manEncumRev.setAccountingCombination(objEncumLine.getAccountingCombination());
                OBDal.getInstance().save(manEncumRev);

                objEncumLine
                    .setAPPAmt(objEncumLine.getAPPAmt().add(objProposalLine.getLineTotal()));
                OBDal.getInstance().save(objEncumLine);
                proposal.setEfinIsbudgetcntlapp(true);
                OBDal.getInstance().save(proposal);
              }
            }
          }
        }
        proposal.setEfinIsbudgetcntlapp(true);
        OBDal.getInstance().save(proposal);

        OBDal.getInstance().save(objEncum);
        // OBDal.getInstance().flush();
      }
    } catch (Exception e) {
      // TODO: handle exception
      log.error("Exception while creating encumbrance modification in create new po version", e);
      OBDal.getInstance().rollbackAndClose();
      return "failure";
    } finally {
    }
    return "success";
  }

  public static void doChangeMofifcationInProposalEncumbrance(EscmProposalMgmt proposal,
      EscmProposalmgmtLine objProposalLine, Boolean isReject) {
    EfinBudgetManencumlines objEncumLine = null;
    BigDecimal amount = BigDecimal.ZERO;
    BigDecimal oldAmount = BigDecimal.ZERO;
    BigDecimal revisedAmt = BigDecimal.ZERO;
    String poEncumId = null;
    try {
      OBContext.setAdminMode();
      String encumId = proposal.getEfinEncumbrance().getId();
      if (encumId != null) {
        amount = objProposalLine.getLineTotal();
        oldAmount = objProposalLine.getEscmOldProposalline().getLineTotal();
        revisedAmt = amount.subtract(oldAmount);
        if (isReject) {
          revisedAmt = revisedAmt.negate();
        }
        if (!objProposalLine.isSummary()) {
          // get encumbrance details
          OBQuery<EfinBudgetManencumlines> manline = OBDal.getInstance().createQuery(
              EfinBudgetManencumlines.class,
              " as e where e.manualEncumbrance.id=:encumID and e.accountingCombination.id=:accID ");
          manline.setNamedParameter("encumID", poEncumId);
          manline.setNamedParameter("accID", objProposalLine.getEFINUniqueCode().getId());

          manline.setMaxResult(1);
          if (manline.list().size() > 0) {
            objEncumLine = manline.list().get(0);

            EfinBudManencumRev manEncumRev = OBProvider.getInstance().get(EfinBudManencumRev.class);
            if (!StringUtils.isEmpty(objEncumLine.getId())) {
              // insert into Manual Encumbrance Revision Table
              manEncumRev.setClient(OBContext.getOBContext().getCurrentClient());
              manEncumRev.setOrganization(OBDal.getInstance().get(Organization.class,
                  objEncumLine.getOrganization().getId()));
              manEncumRev.setActive(true);
              manEncumRev.setUpdatedBy(OBContext.getOBContext().getUser());
              manEncumRev.setCreationDate(new java.util.Date());
              manEncumRev.setCreatedBy(OBContext.getOBContext().getUser());
              manEncumRev.setUpdated(new java.util.Date());
              manEncumRev.setUniqueCode(objEncumLine.getUniquecode());
              manEncumRev.setManualEncumbranceLines(objEncumLine);
              manEncumRev.setRevdate(new Date());
              manEncumRev.setStatus("APP");
              manEncumRev.setRevamount(revisedAmt.negate());
              manEncumRev.setEncumbranceType("MO");
              manEncumRev.setAuto(true);
              manEncumRev.setAccountingCombination(objEncumLine.getAccountingCombination());
              OBDal.getInstance().save(manEncumRev);

              objEncumLine.setAPPAmt(objEncumLine.getAPPAmt().add(revisedAmt.negate()));
              OBDal.getInstance().save(objEncumLine);
              proposal.setEfinIsbudgetcntlapp(true);
              OBDal.getInstance().save(proposal);
            }
          }
        }
      }
      proposal.setEfinIsbudgetcntlapp(true);
      OBDal.getInstance().save(proposal);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log.error("Exception while creating encumbrance modification in create new po version", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * check manual encumbrance validation for create proposal new version
   * 
   * @param proposal
   * @param baseProposal
   * @param appliedamtchk
   * @return
   */
  @SuppressWarnings("unchecked")
  public static boolean chkNewVersionManualEncumbranceValidation(EscmProposalMgmt proposal,
      EscmProposalMgmt baseProposal, boolean appliedamtchk, boolean isreject,
      EscmProposalmgmtLine proposalline) {
    EfinBudgetManencum objEncum = proposal.getEfinEncumbrance();
    EfinBudgetManencumlines objEncumLine = null;
    boolean errorflag = false;

    String newProposallineSql = null, oldProposallineSql = null, encumLineSql = null;
    List<Object> newProposallineList = new ArrayList<Object>(),
        oldProposallineList = new ArrayList<Object>(), encumLineList = new ArrayList<Object>();
    LinkedHashMap<String, JSONObject> newLineMap = new LinkedHashMap<String, JSONObject>(),
        oldLineMap = new LinkedHashMap<String, JSONObject>();
    LinkedHashMap<String, JSONObject> encumLineMap = new LinkedHashMap<String, JSONObject>();
    JSONObject json = null, newLineJson = null, oldLineJson = null, encumLineJson = null;
    try {
      if (proposal.getEscmOldproposal() != null) {
        List<EscmProposalmgmtLine> failureReasonList = proposal.getEscmProposalmgmtLineList()
            .stream().filter(a -> a.getEfinFailureReason() != null).collect(Collectors.toList());
        if (failureReasonList.size() > 0) {
          for (EscmProposalmgmtLine ordln : failureReasonList) {
            ordln.setEfinFailureReason(null);
            OBDal.getInstance().save(ordln);
          }
        }

        /** fetching new order line -linenet amt group by uniquecode **/
        newProposallineSql = " select ln.em_efin_c_validcombination_id "
            + ", coalesce(sum(line_total),0) as linenetamt from escm_proposalmgmt prop "
            + " join escm_proposalmgmt_line ln on ln.escm_proposalmgmt_id=prop.escm_proposalmgmt_id where prop.escm_proposalmgmt_id=? "
            + " and ln.issummarylevel='N' and ln.cancel='N' and ln.status is null";
        if (proposalline != null) {
          newProposallineSql += "  and ln.escm_proposalmgmt_line_id=? ";
        }
        newProposallineSql += "  group by ln.em_efin_c_validcombination_id  order by ln.em_efin_c_validcombination_id asc ";
        SQLQuery newLineQry = OBDal.getInstance().getSession().createSQLQuery(newProposallineSql);
        newLineQry.setParameter(0, proposal.getId());
        if (proposalline != null) {
          newLineQry.setParameter(1, proposalline.getId());
        }
        if (newLineQry != null) {
          newProposallineList = newLineQry.list();

          if (newProposallineList.size() > 0) {
            for (Object newOrderLnObj : newProposallineList) {
              Object[] lineObj = (Object[]) newOrderLnObj;
              if (lineObj[0] != null && lineObj[1] != null) {
                json = new JSONObject();
                json.put("amount", new BigDecimal(lineObj[1].toString()));
                json.put("processed", "N");
                newLineMap.put(lineObj[0].toString(), json);
              }
            }
          }
        }

        /** fetching linenet old order line - amt group by uniquecode **/
        oldProposallineSql = " select ln.em_efin_c_validcombination_id "
            + ", coalesce(sum(line_total),0) as linenetamt from escm_proposalmgmt prop "
            + " join escm_proposalmgmt_line ln on ln.escm_proposalmgmt_id=prop.escm_proposalmgmt_id where prop.escm_proposalmgmt_id=? "
            + " and ln.issummarylevel='N' and ln.cancel='N' and ln.line in ( select line from escm_proposalmgmt_line where escm_proposalmgmt_id=?"
            + " and escm_proposalmgmt_line.status is null ) ";

        if (proposalline != null) {
          oldProposallineSql += "  and ln.line=? ";
        }
        oldProposallineSql += "  group by ln.em_efin_c_validcombination_id  order by ln.em_efin_c_validcombination_id asc ";

        SQLQuery oldLineQry = OBDal.getInstance().getSession().createSQLQuery(oldProposallineSql);
        oldLineQry.setParameter(0, proposal.getEscmOldproposal().getId());
        oldLineQry.setParameter(1, proposal.getId());
        if (proposalline != null) {
          oldLineQry.setParameter(2, proposalline.getLineNo());
        }
        if (oldLineQry != null) {
          oldProposallineList = oldLineQry.list();
          if (oldProposallineList.size() > 0) {
            for (Object oldOrderLnObj : oldProposallineList) {
              Object[] lineObj = (Object[]) oldOrderLnObj;
              if (lineObj[0] != null && lineObj[1] != null) {
                json = new JSONObject();
                json.put("amount", new BigDecimal(lineObj[1].toString()));
                json.put("processed", "N");
                oldLineMap.put(lineObj[0].toString(), json);
              }
            }
          }
        }

        /** fetching encumbrance line remaining amt **/
        encumLineSql = " select c_validcombination_id ,app_amt,remaining_amount,efin_budget_manencumlines_id "
            + " from efin_budget_manencumlines where  efin_budget_manencum_id=? ";
        SQLQuery encumLineQry = OBDal.getInstance().getSession().createSQLQuery(encumLineSql);
        encumLineQry.setParameter(0, objEncum.getId());
        if (encumLineQry != null) {
          encumLineList = encumLineQry.list();
          if (encumLineList.size() > 0) {
            for (Object encumLineObj : encumLineList) {
              Object[] lineObj = (Object[]) encumLineObj;
              if (lineObj[0] != null && lineObj[1] != null && lineObj[3] != null) {
                json = new JSONObject();
                json.put("amount", new BigDecimal(lineObj[2].toString()));
                json.put("encumlineId", lineObj[3].toString());
                encumLineMap.put(lineObj[0].toString(), json);
              }
            }
          }
        }
        // if old order line uniquecode used in new orderline
        if (newLineMap.size() > 0) {
          for (Map.Entry<String, JSONObject> newLineObjMap : newLineMap.entrySet()) {
            if (oldLineMap.containsKey(newLineObjMap.getKey())) {
              newLineJson = newLineObjMap.getValue();
              oldLineJson = oldLineMap.get(newLineObjMap.getKey());
              oldLineJson.put("processed", "Y");
              BigDecimal oldLineNetAmt = new BigDecimal(oldLineJson.get("amount").toString());
              BigDecimal newLineNetAmt = new BigDecimal(newLineJson.get("amount").toString());
              BigDecimal diff = newLineNetAmt.subtract(oldLineNetAmt);
              if (isreject) {
                diff = diff.negate();
              }

              // if checking validation then chk with remaining amt and not enough remaining amt
              // throw
              // error
              if (appliedamtchk) {
                // if diff greater than chk with remaining amt
                if (diff.compareTo(BigDecimal.ZERO) > 0) {
                  // check uniquecode having in encumbrance lines
                  if (encumLineMap.containsKey(newLineObjMap.getKey())) {
                    encumLineJson = encumLineMap.get(newLineObjMap.getKey());
                    BigDecimal remainingAmount = new BigDecimal(
                        encumLineJson.get("amount").toString());
                    // check new order line net amt more than old order line net amt then take diff
                    // amt and check with remaining amount
                    if (remainingAmount.compareTo(diff) < 0) {
                      updateFailureReason(newLineObjMap.getKey(), true, proposal);
                      errorflag = true;
                    }
                  } else {
                    // if uniquecode not present in encumbrance lines throw error
                    updateFailureReason(newLineObjMap.getKey(), false, proposal);
                    errorflag = true;
                  }
                }
              }

              else if (!appliedamtchk) {
                if (encumLineMap.containsKey(newLineObjMap.getKey())) {
                  encumLineJson = encumLineMap.get(newLineObjMap.getKey());
                  if (encumLineJson.has("encumlineId")) {
                    // update the applied amt in encumbrance lines
                    objEncumLine = OBDal.getInstance().get(EfinBudgetManencumlines.class,
                        encumLineJson.getString("encumlineId"));
                    objEncumLine.setAPPAmt(objEncumLine.getAPPAmt().add(diff));
                    OBDal.getInstance().save(objEncumLine);
                  }
                }
              }
            }

            // new line uniquecode not present in old line
            else {
              newLineJson = newLineObjMap.getValue();
              BigDecimal newLineNetAmt = new BigDecimal(newLineJson.get("amount").toString());
              BigDecimal oldLineNetAmt = BigDecimal.ZERO;
              BigDecimal diff = newLineNetAmt.subtract(oldLineNetAmt);
              if (isreject) {
                diff = diff.negate();
              }
              if (appliedamtchk) {
                if (diff.compareTo(BigDecimal.ZERO) > 0) {
                  // check uniquecode having in encumbrance lines
                  if (encumLineMap.containsKey(newLineObjMap.getKey())) {
                    encumLineJson = encumLineMap.get(newLineObjMap.getKey());
                    BigDecimal remainingAmount = new BigDecimal(
                        encumLineJson.get("amount").toString());
                    // check new order line net amt more than old order line net amt then take diff
                    // amt
                    // and check with remaining amount
                    if (remainingAmount.compareTo(diff) < 0) {
                      updateFailureReason(newLineObjMap.getKey(), true, proposal);
                      errorflag = true;
                    }
                  } else {
                    // if uniquecode not present in encumbrance lines throw error
                    updateFailureReason(newLineObjMap.getKey(), false, proposal);
                    errorflag = true;
                  }
                }
              } else if (!appliedamtchk) {
                if (encumLineMap.containsKey(newLineObjMap.getKey())) {
                  encumLineJson = encumLineMap.get(newLineObjMap.getKey());
                  if (encumLineJson.has("encumlineId")) {
                    // update the applied amt in encumbrance lines
                    objEncumLine = OBDal.getInstance().get(EfinBudgetManencumlines.class,
                        encumLineJson.getString("encumlineId"));
                    objEncumLine.setAPPAmt(objEncumLine.getAPPAmt().add(diff));
                    OBDal.getInstance().save(objEncumLine);
                  }
                }
              }
            }
          }
        }

        // if old order line uniquecode does not used in new orderline
        if (oldLineMap.size() > 0) {
          for (Map.Entry<String, JSONObject> oldLineObjMap : oldLineMap.entrySet()) {
            oldLineJson = oldLineObjMap.getValue();
            BigDecimal oldLineNetAmt = new BigDecimal(oldLineJson.get("amount").toString());
            BigDecimal newLineNetAmt = BigDecimal.ZERO;
            BigDecimal diff = newLineNetAmt.subtract(oldLineNetAmt);
            if (isreject) {
              diff = diff.negate();
            }
            if (oldLineJson.has("processed") && oldLineJson.get("processed").equals("N")) {
              oldLineJson.put("processed", "Y");
              if (appliedamtchk) {
                // check uniquecode having in encumbrance lines
                if (encumLineMap.containsKey(oldLineObjMap.getKey())) {
                  encumLineJson = encumLineMap.get(oldLineObjMap.getKey());
                  BigDecimal remainingAmount = new BigDecimal(
                      encumLineJson.get("amount").toString());
                  if (remainingAmount.compareTo(diff) < 0) {
                    updateFailureReason(oldLineObjMap.getKey(), true, proposal);
                    errorflag = true;
                  }
                }
              } else if (!appliedamtchk) {
                encumLineJson = encumLineMap.get(oldLineObjMap.getKey());
                if (encumLineJson.has("encumlineId")) {
                  // update the applied amt in encumbrance lines
                  objEncumLine = OBDal.getInstance().get(EfinBudgetManencumlines.class,
                      encumLineJson.getString("encumlineId"));
                  objEncumLine.setAPPAmt(objEncumLine.getAPPAmt().add(diff));
                  OBDal.getInstance().save(objEncumLine);
                }
              }

            }
          }
        }
        if (!appliedamtchk) {
          if (isreject) {
            proposal.setEfinIsbudgetcntlapp(false);
            OBDal.getInstance().save(proposal);
          } else {
            proposal.setEfinIsbudgetcntlapp(true);
            OBDal.getInstance().save(proposal);
          }
        }
      }
      return errorflag;
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in chkManualEncumbranceValidation " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
      return true;
    }
  }

  public static void updateFailureReason(String uniqueCodeId, boolean isUniqueCodePresent,
      EscmProposalMgmt objProposal) {
    List<EscmProposalmgmtLine> proposallineList = new ArrayList<EscmProposalmgmtLine>();
    try {

      OBQuery<EscmProposalmgmtLine> proposalLineQry = OBDal.getInstance()
          .createQuery(EscmProposalmgmtLine.class, " as e where e.escmProposalmgmt.id=:proposalId"
              + " and e.eFINUniqueCode.id=:uniqueCodeId and e.summary='N' ");
      proposalLineQry.setNamedParameter("proposalId", objProposal.getId());
      proposalLineQry.setNamedParameter("uniqueCodeId", uniqueCodeId);
      proposallineList = proposalLineQry.list();
      if (proposallineList.size() > 0) {
        for (EscmProposalmgmtLine proposalLineObj : proposallineList) {
          if (isUniqueCodePresent) {
            proposalLineObj.setEfinFailureReason(OBMessageUtils.messageBD("Efin_ReqAmt_More"));
          } else {
            proposalLineObj.setEfinFailureReason("uniquecode is not present in encumbrance lines");
          }
          OBDal.getInstance().save(proposalLineObj);
        }
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in updateFailureReason " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
    }
  }

  /**
   * This method is responsible to do modification in existing encumbrance while reject
   * 
   * @param order
   * @param baseOrder
   */
  @SuppressWarnings("finally")
  public static String doRejectPOVersionMofifcationInEncumbrance(EscmProposalMgmt proposalmgmt,
      EscmProposalMgmt baseProposal, boolean iscancel, EscmProposalmgmtLine proposalLine) {
    EfinBudgetManencum objEncum = proposalmgmt.getEfinEncumbrance();
    EfinBudgetManencumlines objEncumLine = null;
    OBQuery<EscmProposalmgmtLine> oldProposalLineQry = null;
    @SuppressWarnings("unused")
    EfinBudManencumRev manEncumRev = null;
    boolean isBaseProposal = true;
    List<EscmProposalmgmtLine> proposalLineList = new ArrayList<EscmProposalmgmtLine>();
    try {
      if (proposalmgmt.getEscmOldproposal() != null) {
        // compare amount between old version and new version order line
        if (proposalLine != null) {
          proposalLineList = proposalmgmt.getEscmProposalmgmtLineList().stream()
              .filter(a -> a.getId().equals(proposalLine.getId())).collect(Collectors.toList());
        } else {
          proposalLineList = proposalmgmt.getEscmProposalmgmtLineList();
        }
        for (EscmProposalmgmtLine objProposalLine : proposalLineList) {
          if (!objProposalLine.isSummary() && objProposalLine.getStatus() == null) {
            if (objProposalLine.getProduct() != null) {
              oldProposalLineQry = OBDal.getInstance().createQuery(EscmProposalmgmtLine.class,
                  "as e where e.escmProposalmgmt.id=:oldProposalId and e.product.id=:productID "
                      + " and e.eFINUniqueCode.id=:uniquecodeID ");
              oldProposalLineQry.setNamedParameter("oldProposalId",
                  proposalmgmt.getEscmOldproposal().getId());
              oldProposalLineQry.setNamedParameter("productID",
                  objProposalLine.getProduct().getId());
              oldProposalLineQry.setNamedParameter("uniquecodeID",
                  objProposalLine.getEFINUniqueCode().getId());
            }
            // get the orderline if description is not null
            else if (objProposalLine.getDescription() != null) {
              oldProposalLineQry = OBDal.getInstance().createQuery(EscmProposalmgmtLine.class,
                  "as e where e.escmProposalmgmt.id=:oldProposalId and e.description=:prdDesc "
                      + " and e.eFINUniqueCode.id=:uniquecodeID ");
              oldProposalLineQry.setNamedParameter("oldProposalId",
                  proposalmgmt.getEscmOldproposal().getId());
              oldProposalLineQry.setNamedParameter("prdDesc", objProposalLine.getDescription());
              oldProposalLineQry.setNamedParameter("uniquecodeID",
                  objProposalLine.getEFINUniqueCode().getId());
            }
            if (oldProposalLineQry != null && oldProposalLineQry.list().size() > 0) {
              EscmProposalmgmtLine oldProposalLine = oldProposalLineQry.list().get(0);
              // get encumbrance details
              OBQuery<EfinBudgetManencumlines> manline = OBDal.getInstance().createQuery(
                  EfinBudgetManencumlines.class,
                  " as e  where e.manualEncumbrance.id=:encumID and e.accountingCombination.id=:acctID ");
              manline.setNamedParameter("encumID", objEncum.getId());
              manline.setNamedParameter("acctID", oldProposalLine.getEFINUniqueCode().getId());
              manline.setMaxResult(1);
              if (manline.list().size() > 0) {
                objEncumLine = manline.list().get(0);
              }

              if (((objProposalLine.getLineTotal().compareTo(oldProposalLine.getLineTotal()) > 0)
                  || (objProposalLine.getLineTotal().compareTo(oldProposalLine.getLineTotal()) < 0))
                  && objEncumLine != null) {
                // do revision with increase or decrease amount
                BigDecimal amount = objProposalLine.getLineTotal()
                    .subtract(oldProposalLine.getLineTotal());
                // objEncum.setRemainingamt(objEncum.getRemainingamt().add(amount));
                // OBDal.getInstance().save(objEncum);

                if (!StringUtils.isEmpty(objEncumLine.getId())) {
                  if (!iscancel) {
                    // insert into Manual Encumbrance Revision Table
                    ProposalManagementRejectMethods.deleteModification(objEncumLine, amount);
                    // for Proposal management Encumbrance
                    doChangeMofifcationInProposalEncumbrance(proposalmgmt, objProposalLine, true);
                  } else {
                    if (proposalmgmt.getEscmBaseproposal() != null
                        && proposalmgmt.isEfinIsbudgetcntlapp()
                        && proposalmgmt.getEscmOldproposal().getEfinEncumbrance() != null) {
                      isBaseProposal = false;
                    }
                    manEncumRev = insertEncumbranceModification(objEncumLine, amount.negate(), null,
                        isBaseProposal);
                    // for Proposal management Encumbrance
                    doChangeMofifcationInProposalEncumbrance(proposalmgmt, objProposalLine, true);
                  }
                  objEncumLine.setAPPAmt(objEncumLine.getAPPAmt().add(amount.negate()));
                  OBDal.getInstance().save(objEncumLine);
                  log.info("oldline encumamt>" + objEncumLine.getAPPAmt());
                }
              }
            } else {
              OBQuery<EfinBudgetManencumlines> manline = OBDal.getInstance().createQuery(
                  EfinBudgetManencumlines.class,
                  " as e  where e.manualEncumbrance.id=:encumID and e.accountingCombination.id=:acctID ");
              manline.setNamedParameter("encumID", objEncum.getId());
              manline.setNamedParameter("acctID", objProposalLine.getEFINUniqueCode().getId());
              manline.setMaxResult(1);
              if (manline.list().size() > 0) {
                objEncumLine = manline.list().get(0);
              }
              // do revision with increase or decrease amount
              BigDecimal amount = objProposalLine.getLineTotal();
              log.info("new amt>" + amount);
              if (!StringUtils.isEmpty(objEncumLine.getId())) {
                if (!iscancel) {
                  // insert into Manual Encumbrance Revision Table
                  ProposalManagementRejectMethods.deleteModification(objEncumLine, amount);
                } else {
                  if (proposalmgmt.getEscmBaseproposal() != null
                      && proposalmgmt.isEfinIsbudgetcntlapp()
                      && proposalmgmt.getEscmOldproposal().getEfinEncumbrance() != null) {
                    isBaseProposal = false;
                  }
                  manEncumRev = insertEncumbranceModification(objEncumLine, amount.negate(), null,
                      isBaseProposal);
                }
                objEncumLine.setAPPAmt(objEncumLine.getAPPAmt().add(amount.negate()));
                OBDal.getInstance().save(objEncumLine);
                log.info("new lineencumapp amt>" + objEncumLine.getAPPAmt());
              }
            }
          }
        }
        if (proposalLine == null) {
          proposalmgmt.setEfinIsbudgetcntlapp(false);
          OBDal.getInstance().save(proposalmgmt);
        }

        OBDal.getInstance().save(objEncum);
        OBDal.getInstance().flush();
      }
    } catch (Exception e) {
      // TODO: handle exception
      log.error("Exception while creating encumbrance modification in create new proposal version",
          e);
      OBDal.getInstance().rollbackAndClose();
      return "failure";
    } finally {
      return "success";
    }
  }

  /**
   * 
   * @param encumbranceline
   * @param decamount
   * @param srcrefline
   */
  public static EfinBudManencumRev insertEncumbranceModification(
      EfinBudgetManencumlines encumbranceline, BigDecimal decamount,
      EfinBudgetManencumlines srcrefline, boolean isbaseProposal) {
    EfinBudManencumRev manEncumRev = null;
    try {
      manEncumRev = OBProvider.getInstance().get(EfinBudManencumRev.class);
      // insert into Manual Encumbrance Revision Table
      manEncumRev.setClient(OBContext.getOBContext().getCurrentClient());
      manEncumRev.setOrganization(
          OBDal.getInstance().get(Organization.class, encumbranceline.getOrganization().getId()));
      manEncumRev.setActive(true);
      manEncumRev.setUpdatedBy(OBContext.getOBContext().getUser());
      manEncumRev.setCreationDate(new java.util.Date());
      manEncumRev.setCreatedBy(OBContext.getOBContext().getUser());
      manEncumRev.setUpdated(new java.util.Date());
      manEncumRev.setUniqueCode(encumbranceline.getUniquecode());
      manEncumRev.setManualEncumbranceLines(encumbranceline);
      manEncumRev.setRevdate(new java.util.Date());
      manEncumRev.setStatus("APP");
      manEncumRev.setAuto(true);
      manEncumRev.setRevamount(decamount);
      manEncumRev.setAccountingCombination(encumbranceline.getAccountingCombination());
      manEncumRev.setSRCManencumline(srcrefline);
      manEncumRev.setEncumbranceType("PAE");
      if (!isbaseProposal) {
        manEncumRev.setSystem(false);
      } else {
        manEncumRev.setSystem(true);
      }
      OBQuery<RequisitionLine> ln = OBDal.getInstance().createQuery(RequisitionLine.class,
          " as e where e.requisition.id in ( select e.id from ProcurementRequisition e where "
              + " e.efinBudgetManencum.id=:encumID ) and e.efinCValidcombination.id=:accID "
              + " and escmIssummary='N' ");
      ln.setNamedParameter("encumID", encumbranceline.getManualEncumbrance().getId());
      ln.setNamedParameter("accID", encumbranceline.getAccountingCombination().getId());
      ln.setMaxResult(1);
      if (ln.list().size() > 0) {
        manEncumRev.setRequisitionLine(ln.list().get(0));
      }
      log.debug("req:" + manEncumRev.getRequisitionLine());
      OBDal.getInstance().save(manEncumRev);

    } catch (Exception e) {
      log.error("Exception in insertEncumbranceModification " + e.getMessage());
    }
    return manEncumRev;
  }

  /**
   * Method to process single cancellation on proposal when proposal have more than one version
   * 
   * @param proposalmgmtline
   */
  public static Boolean multiVersionProposalLineCancelProcess(EscmProposalmgmtLine proposalmgmtline,
      EscmProposalMgmt baseProposalObj) {
    BigDecimal amountDiffernce = BigDecimal.ZERO;
    List<EscmProposalmgmtLine> baseProlineList;
    List<EscmProposalmgmtLine> currentProposalLineList;
    String combination_id = proposalmgmtline.getEFINUniqueCode().getId();
    Boolean is_funds_not_availed = Boolean.FALSE;
    int count = 1;
    Boolean isRevertEncumbedforNewVersion = false;
    EscmProposalMgmt currentProposal = proposalmgmtline.getEscmProposalmgmt();
    boolean isManual = false;
    try {

      int size = currentProposal.getEscmProposalmgmtLineList().size();
      List<EscmProposalmgmtLine> lineList = new ArrayList<EscmProposalmgmtLine>();
      lineList = currentProposal.getEscmProposalmgmtLineList();
      for (EscmProposalmgmtLine lines : lineList) {
        if ((lines.getStatus() != null && lines.getStatus().equals("CL"))
            || (lines.getPeestatus() != null && lines.getPeestatus().equals("CL"))) {
          count = count + 1;
        }
      }
      if (size == count) {
        isRevertEncumbedforNewVersion = true;
      }
      if (isRevertEncumbedforNewVersion) {
        for (EscmProposalmgmtLine lines : lineList) {
          if (!proposalmgmtline.getId().equals(lines.getId())) {
            EfinBudgetManencumlines encline = lines.getEscmOldProposalline()
                .getEfinBudgmanencumline();
            // if reserved then consider new proposal line total
            amountDiffernce = lines.getEscmOldProposalline().getLineTotal();
            if (encline.getManualEncumbrance().getEncumMethod().equals("M")) {
              isManual = true;
              if (encline.getRemainingAmount().compareTo(amountDiffernce) < 0) {
                is_funds_not_availed = true;
                return is_funds_not_availed;
              }

            } else {
              is_funds_not_availed = chkAutoEncumbranceValidation(
                  proposalmgmtline.getEscmProposalmgmt(), true, combination_id, false, true,
                  amountDiffernce);
              if (is_funds_not_availed) {
                return is_funds_not_availed;
              }
            }
          }
        }
      }

      currentProposalLineList = currentProposal.getEscmProposalmgmtLineList();
      for (EscmProposalmgmtLine currentProposalLine : currentProposalLineList) {
        if (!currentProposalLine.isSummary()) {
          EfinBudgetManencumlines encline = currentProposalLine.getEfinBudgmanencumline();
          if (currentProposalLine.getEscmOldProposalline() != null && proposalmgmtline.getLineNo()
              .equals(currentProposalLine.getEscmOldProposalline().getLineNo())) {

            // chk if reserved done for this line based on comparing applied amount from the
            // encumbrance line with new proposal line total
            // if (currentProposal.isEfinIsbudgetcntlapp()) {
            BigDecimal proposalTotalAmt = currentProposalLineList.stream()
                .filter(a -> a.getStatus() == null && a.getEFINUniqueCode() != null
                    && a.getEFINUniqueCode().getId()
                        .equals(proposalmgmtline.getEFINUniqueCode().getId()))
                .map(a -> a.getLineTotal()).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (proposalTotalAmt.compareTo(encline.getAPPAmt()) == 0) {
              amountDiffernce = proposalmgmtline.getLineTotal();
            } else {
              amountDiffernce = currentProposalLine.getEscmOldProposalline().getLineTotal();
            }
            // amountDiffernce = proposalmgmtline.getLineTotal()
            // .subtract(oldProposalLine.getLineTotal());
            // is_funds_not_availed = chkAutoEncumbranceValidation(
            // proposalmgmtline.getEscmProposalmgmt(), true, combination_id, false, true,
            // amountDiffernce);
            // if (!is_funds_not_availed) {
            if (encline.getManualEncumbrance().getEncumMethod().equals("A")) {
              ProposalManagementActionMethod.insertEncumbranceModification(encline,
                  amountDiffernce.negate(), null, false);
              // mark encumbrance applied amount
              EfinBudgetManencumlines encumLn = Utility.getObject(EfinBudgetManencumlines.class,
                  encline.getId());
              encumLn.setAPPAmt(encumLn.getAPPAmt().add(amountDiffernce.negate()));
              OBDal.getInstance().save(encumLn);
            } else {
              // update in remaining amount
              EfinBudgetManencumlines encumLn = Utility.getObject(EfinBudgetManencumlines.class,
                  encline.getId());
              encumLn.setAPPAmt(encumLn.getAPPAmt().add(amountDiffernce.negate()));
              encumLn.setRemainingAmount(encumLn.getRemainingAmount().add(amountDiffernce));
              OBDal.getInstance().save(encumLn);
            }
            // }
            break;
          }
        }
      }

      if (isRevertEncumbedforNewVersion) {

        for (EscmProposalmgmtLine lines : lineList) {
          if (lines.getStatus() == null
              || lines.getStatus() != null && lines.getEscmOldProposalline() != null
                  && lines.getEscmOldProposalline().getStatus() == null) {
            EfinBudgetManencumlines encline = lines.getEscmOldProposalline()
                .getEfinBudgmanencumline();
            // if reserved then consider new proposal line total
            amountDiffernce = lines.getEscmOldProposalline().getLineTotal();
            if (!isManual) {
              ProposalManagementActionMethod.insertEncumbranceModification(encline, amountDiffernce,
                  null, false);
              EfinBudgetManencumlines encumLn = Utility.getObject(EfinBudgetManencumlines.class,
                  encline.getId());
              encumLn.setAPPAmt(encumLn.getAPPAmt().add(amountDiffernce));
              OBDal.getInstance().save(encumLn);
            } else {
              // update in remaining amount
              EfinBudgetManencumlines encumLn = Utility.getObject(EfinBudgetManencumlines.class,
                  encline.getId());
              encumLn.setAPPAmt(encumLn.getAPPAmt().add(amountDiffernce));
              encumLn.setRemainingAmount(encumLn.getRemainingAmount().subtract(amountDiffernce));
              OBDal.getInstance().save(encumLn);
            }
          }
        }
      }

      // baseProlineList = baseProposalObj.getEscmProposalmgmtLineList();
      // for (EscmProposalmgmtLine oldProposalLine : baseProlineList) {
      // if (!oldProposalLine.isSummary()) {
      // EfinBudgetManencumlines encline = oldProposalLine.getEfinBudgmanencumline();
      // if (proposalmgmtline.getLineNo().equals(oldProposalLine.getLineNo())) {
      // amountDiffernce = proposalmgmtline.getLineTotal()
      // .subtract(oldProposalLine.getLineTotal());
      // is_funds_not_availed = chkAutoEncumbranceValidation(
      // proposalmgmtline.getEscmProposalmgmt(), true, combination_id, false, true,
      // amountDiffernce);
      // if (!is_funds_not_availed) {
      // ProposalManagementActionMethod.insertEncumbranceModification(encline,
      // amountDiffernce.negate(), null, false);
      // // mark encumbrance applied amount
      // EfinBudgetManencumlines encumLn = Utility.getObject(EfinBudgetManencumlines.class,
      // encline.getId());
      // encumLn.setAPPAmt(encumLn.getAPPAmt().add(amountDiffernce.negate()));
      // OBDal.getInstance().save(encumLn);
      // }
      // break;
      // }
      // }
      // }

    } catch (Exception e) {
      log.error("Exception in multiVersionProposalLineCancelProcess " + e.getMessage());
      return true;
    }
    return is_funds_not_availed;
  }

  public static JSONObject isrevertEncumbrance(EscmProposalMgmt proposal) {
    boolean errorFlag = false;
    JSONObject result = new JSONObject();
    try {

      if (proposal.getEscmBaseproposal() != null) {
        if (proposal.getEscmProposalmgmtLineList().size() > 0) {
          if (proposal.getEfinEncumbrance().getEncumMethod().equals("M")) {
            // check encumbrance used or not based on used amount - for both manual & auto
            errorFlag = ProposalManagementRejectMethods.chkManualEncumbranceRejValid(proposal);
            if (errorFlag) {
              result.put("errorFlag", errorFlag);
              result.put("message", "Efin_Encum_Used_Cannot_Canl");
            } else {
              ProposalManagementRejectMethods.updateManualEncumAppAmt(proposal, true);
            }
          } else if (proposal.getEfinEncumbrance().getEncumMethod().equals("A")) {
            errorFlag = ProposalManagementRejectMethods.chkAutoEncumbranceValid(proposal);
            if (errorFlag) {
              result.put("errorFlag", errorFlag);
              result.put("message", "Efin_Fundsavailabe_Negative");
            } else {
              ProposalManagementRejectMethods.updateAutoEncumbrancechanges(proposal, true);
            }
          }
          if (errorFlag) {
            proposal.setEfinIsbudgetcntlapp(false);
            OBDal.getInstance().save(proposal);
          }
        }
      } else {

      }

    } catch (Exception e) {
      log.debug("Exeception in isrevertEncumbrance:" + e);
    }
    return result;

  }
}
