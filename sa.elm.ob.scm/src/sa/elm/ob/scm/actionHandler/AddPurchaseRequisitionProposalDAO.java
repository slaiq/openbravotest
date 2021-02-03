package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMProposalMgmtLineV;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.EscmRequisitionlineV;
import sa.elm.ob.scm.Escmbidsourceref;

/**
 * 
 * @author Gopalakrishnan on 25/08/2017
 * 
 */
public class AddPurchaseRequisitionProposalDAO {
  @SuppressWarnings("unused")
  private Connection conn = null;

  public AddPurchaseRequisitionProposalDAO(Connection conn) {
    this.conn = conn;
  }

  private final static Logger log = LoggerFactory
      .getLogger(AddPurchaseRequisitionProposalDAO.class);

  /**
   * 
   * @param line
   * @param purchasereqId
   * @param purLineId
   * @param unitprice
   * @param paramNeedByDate
   * @param deptId
   * @param qty
   * @param description
   * @param updateqtyflag
   * @return success 1 else 0
   */
  public static int insertsourceref(EscmProposalmgmtLine Objline, String purchasereqId,
      String purLineId, String unitprice, String paramNeedByDate, String deptId, String qty,
      String description, Boolean updateqtyflag) {
    int count = 0;
    long lineno = 10;
    EscmProposalsourceRef source = null;
    BigDecimal updQty = BigDecimal.ZERO;
    try {
      OBContext.setAdminMode();
      EscmProposalmgmtLine line = Objline;
      // get the next line no to insert the record in Proposal source reference
      final SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(
          "select coalesce(max(line),0)+10   as lineno from escm_proposalsource_ref where escm_proposalmgmt_line_id=:proposalId");
      query.setParameter("proposalId", line.getId());
      lineno = ((BigDecimal) (Object) query.list().get(0)).longValue();

      // check already line is exists or not in proposal source reference based on proposal
      // management
      // line with Purchase req line. If exists then update the source qty otherwise insert a new
      // record.
      OBQuery<EscmProposalsourceRef> chklineexistQry = OBDal.getInstance().createQuery(
          EscmProposalsourceRef.class, "as e where e.escmProposalmgmtLine.id=:proposalLineId"
              + " and  e.requisitionLine.id=:reqLineId");
      chklineexistQry.setNamedParameter("proposalLineId", line.getId());
      chklineexistQry.setNamedParameter("reqLineId", purLineId);

      chklineexistQry.setMaxResult(1);
      List<EscmProposalsourceRef> sourceList = chklineexistQry.list();
      // update the existing line of proposal line source reference
      if (sourceList.size() > 0) {
        source = sourceList.get(0);
        source.setUpdated(new java.util.Date());
        source.setUpdatedBy(line.getUpdatedBy());
        if (source.getReservedQuantity().compareTo(new BigDecimal(qty)) > 0) {
          updQty = (source.getReservedQuantity().subtract(new BigDecimal(qty))).negate();
        } else {
          updQty = (source.getReservedQuantity().subtract(new BigDecimal(qty))).abs();
        }
        source.setReservedQuantity(new BigDecimal(qty));
        OBDal.getInstance().save(source);
      }
      // insert a new record in proposal Source Reference

      else {
        RequisitionLine objReqLine = OBDal.getInstance().get(RequisitionLine.class, purLineId);
        source = OBProvider.getInstance().get(EscmProposalsourceRef.class);
        source.setClient(line.getClient());
        source.setOrganization(line.getOrganization());
        source.setCreationDate(new java.util.Date());
        source.setCreatedBy(line.getCreatedBy());
        source.setUpdated(new java.util.Date());
        source.setUpdatedBy(line.getUpdatedBy());
        source.setLineNo(lineno);
        source.setRequisitionLine(objReqLine);
        source.setRequisition(OBDal.getInstance().get(Requisition.class, purchasereqId));
        source.setReservedQuantity(new BigDecimal(qty));
        source.setEscmProposalmgmtLine(line);

        updQty = new BigDecimal(qty);
        OBDal.getInstance().save(source);
        OBDal.getInstance().save(line);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(line);
      }
      // update Proposal Line movement Qty
      updateReqLineOrderQty(line, purLineId, updQty);
      //
      if (updateqtyflag) {
        RequisitionLine objReqLine = OBDal.getInstance().get(RequisitionLine.class, purLineId);
        // update Proposal line
        line.setUpdated(new java.util.Date());
        line.setUpdatedBy(OBContext.getOBContext().getUser());
        BigDecimal qtyCheck = (line.getMovementQuantity()).add(updQty);
        line.setMovementQuantity(qtyCheck);
        line.setUOM(objReqLine.getUOM());
        line.setNegotUnitPrice(objReqLine.getUnitPrice());
        line.setGrossUnitPrice(objReqLine.getUnitPrice());
        line.setNetprice(objReqLine.getUnitPrice());
        line.setLineTotal(objReqLine.getUnitPrice().multiply(qtyCheck));
        OBDal.getInstance().save(line);
        OBDal.getInstance().flush();

      }
      count = 1;
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log.error("Exception in insertsourceref in IssueRequest: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;

  }

  /**
   * Update the BidmgmtQty in Purchase Requisition line once requisition line associated with bid.
   * 
   * @param bid
   *          management line object
   * @param purLineId
   * @param qty
   * @return count , if successfully updated then return 1 otherwise 0
   */
  private static int updateReqLineOrderQty(EscmProposalmgmtLine line, String purLineId,
      BigDecimal qty) {
    int count = 0;
    try {
      OBContext.setAdminMode();
      // update the requisition line orderQty
      RequisitionLine reqline = OBDal.getInstance().get(RequisitionLine.class, purLineId);
      reqline.setEscmIsproposal(true);
      reqline.setUpdated(new java.util.Date());
      reqline.setUpdatedBy(line.getUpdatedBy());
      OBDal.getInstance().save(reqline);
      count = 1;

    } catch (Exception e) {
      log.error("Exception in updateReqLineOrderQty in POOrderContratcAddLinesDAO: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  /**
   * To check whether all selected record belongs to same dept.
   * 
   * @param selectedlines
   *          selected JSONArray
   * @return true, if department mismatched.
   */
  public boolean checkSameDept(JSONArray selectedlines) {
    try {
      JSONObject firstRecord = selectedlines.getJSONObject(0);
      String firstDept = firstRecord.getString("department");
      for (int a = 0; a < selectedlines.length(); a++) {
        JSONObject selectedRow = selectedlines.getJSONObject(a);
        if (!firstDept.equals(selectedRow.getString("department"))) {
          return true;
        }
      }
    } catch (Exception e) {
      log.error("Exception while checking all records belongs to same dept ", e);
    }
    return false;
  }

  /**
   * 
   * @param reqline
   * @param proposal
   * @param selectedRow
   */

  public static boolean checkSelectedLineAlreadyExists(RequisitionLine reqline,
      EscmProposalMgmt proposal, JSONObject selectedRow, Connection conn) {
    // While inserting the tree hierarchy, check whether the line is already exist.
    // if exists then check qty, if qty is zero then remove that source ref line and bidmgmtline
    // if bidmgmtline's parent has child
    EscmProposalmgmtLine proposalMgmtLine = null;
    EscmProposalsourceRef proposalSourceRef = null;

    OBQuery<EscmProposalsourceRef> proposalExistQuery = OBDal.getInstance().createQuery(
        EscmProposalsourceRef.class,
        "as e where e.escmProposalmgmtLine.escmProposalmgmt.id =:proposalId "
            + " and e.requisitionLine.id =:reqLineId ");
    proposalExistQuery.setNamedParameter("proposalId", proposal.getId());
    proposalExistQuery.setNamedParameter("reqLineId", reqline.getId());
    proposalExistQuery.setMaxResult(1);
    List<EscmProposalsourceRef> proposalExistList = proposalExistQuery.list();
    if (proposalExistList != null && proposalExistList.size() > 0) {
      proposalSourceRef = proposalExistList.get(0);
      proposalMgmtLine = proposalExistList.get(0).getEscmProposalmgmtLine();
      try {
        if (new BigDecimal(selectedRow.getString("quantity")).compareTo(BigDecimal.ZERO) == 0) {
          // If the parent has no child, set issummary to 'N'
          EscmProposalmgmtLine parentProposalMgmtLine = OBDal.getInstance()
              .get(EscmProposalmgmtLine.class, proposalMgmtLine.getParentLineNo().getId());
          if (!checkparentHasLeaf(proposal, parentProposalMgmtLine)) {
            parentProposalMgmtLine.setSummary(false);
            OBDal.getInstance().save(parentProposalMgmtLine);
          }

          // Remove Source and Line
          OBDal.getInstance().remove(proposalSourceRef);
          OBDal.getInstance().remove(proposalMgmtLine);
          OBDal.getInstance().flush();
        } else {
          insertsourceref(proposalMgmtLine, selectedRow.getString("requisition"),
              selectedRow.getString("id"), selectedRow.getString("unitPrice"),
              selectedRow.getString("needByDate"), selectedRow.getString("department"),
              selectedRow.getString("quantity"), selectedRow.getString("linedescription"), true);
        }
      } catch (JSONException e) {
        log.error("Exception in AddPurchaseRequistionProposalDAO :", e);
        OBDal.getInstance().rollbackAndClose();
      }
      return true;
    }
    return false;
  }

  /**
   * 
   * @param proposalMgmt
   * @param parentProposalMgmtLine
   * @return true or false
   */

  public static boolean checkparentHasLeaf(EscmProposalMgmt proposalMgmt,
      EscmProposalmgmtLine parentProposalMgmtLine) {

    OBQuery<EscmProposalmgmtLine> chkLineExists = OBDal.getInstance()
        .createQuery(EscmProposalmgmtLine.class, "as e where e.escmProposalmgmt.id =:proposalId "
            + "' and e.parentLineNo.id =:parentLineId ");
    chkLineExists.setNamedParameter("proposalId", proposalMgmt.getId());
    chkLineExists.setNamedParameter("parentLineId", parentProposalMgmtLine.getId());
    chkLineExists.setMaxResult(1);
    if (chkLineExists.list().size() > 0) {
      return true;
    }
    return false;
  }

  /**
   * 
   * @param conn
   * @param proposalMgmtId
   * @return lineno
   */
  public static long getLineNo(Connection conn, String proposalMgmtId) {
    PreparedStatement ps = null;
    ResultSet rs = null;
    long lineNo = 10;
    // get the next line no based on bid management id
    try {
      OBContext.setAdminMode();
      ps = conn.prepareStatement(
          " select coalesce(max(line),0)+10 as lineno from escm_proposalmgmt_line where escm_proposalmgmt_id=?");
      ps.setString(1, proposalMgmtId);
      rs = ps.executeQuery();
      if (rs.next()) {
        log.debug("st:" + ps.toString());
        return rs.getLong("lineno");
      }
    } catch (SQLException e) {
      if (log.isDebugEnabled()) {
        log.debug("Error while getting id in proposalMgmt" + e, e);
      }
    } finally {
      try {
        // close connection
        if (rs != null) {
          rs.close();
        }
        if (ps != null) {
          ps.close();
        }
      } catch (Exception e) {

      }
      OBContext.restorePreviousMode();
    }
    return lineNo;
  }

  /**
   * 
   * @param reqline
   * @param treeList
   * @param proposalMgmt
   * @param line
   * @param conn
   * @param selectedRow
   * @return
   */

  public static void getParentLines(RequisitionLine reqline, ArrayList<String> treeList,
      EscmProposalMgmt proposalMgmt, long line, Connection conn, JSONObject selectedRow) {
    EscmRequisitionlineV parentLine = reqline.getEscmParentlineno();

    if (parentLine != null) {
      String parentId = parentLine.getId();
      RequisitionLine parentReqLine = OBDal.getInstance().get(RequisitionLine.class, parentId);

      OBQuery<EscmProposalsourceRef> proposalExistQuery = OBDal.getInstance().createQuery(
          EscmProposalsourceRef.class,
          "as e where e.escmProposalmgmtLine.escmProposalmgmt.id =:proposalId  "
              + " and e.requisitionLine.id =:parentLineId");
      proposalExistQuery.setNamedParameter("proposalId", proposalMgmt.getId());
      proposalExistQuery.setNamedParameter("parentLineId", parentId);
      proposalExistQuery.setMaxResult(1);

      // check its parent is already exist if exists then insert the selected line in parent tree
      if (proposalExistQuery.list().size() > 0) {
        insertParentLines(treeList, proposalMgmt, line, conn, selectedRow,
            proposalExistQuery.list().get(0).getEscmProposalmgmtLine(), false);
      } else {
        treeList.add(parentId);
        getParentLines(parentReqLine, treeList, proposalMgmt, line, conn, selectedRow);
      }
    } else {
      insertParentLines(treeList, proposalMgmt, line, conn, selectedRow, null, false);
    }
  }

  /**
   * 
   * @param treeList
   * @param proposalMgmt
   * @param line
   * @param conn
   * @param selectedRow
   * @param proposalExistingLine
   * @param updateqtyflag
   * @return
   */
  public static void insertParentLines(ArrayList<String> treeList, EscmProposalMgmt proposalMgmt,
      long line, Connection conn, JSONObject selectedRow, EscmProposalmgmtLine proposalExistingLine,
      boolean updateqtyflag) {
    Long lineNo = line;
    String qty = null;
    EscmProposalmgmtLine proposalAvaliableLine = proposalExistingLine;
    EscmProposalmgmtLine lastProposalMgmtLine = null;
    EscmProposalmgmtLine newProposalMgmtLine = null;
    ESCMProposalMgmtLineV proposalMgmtLine = null;

    // Insert tree structure from treelist
    for (int i = treeList.size() - 1; i >= 0; i--) {
      RequisitionLine reqLine = OBDal.getInstance().get(RequisitionLine.class, treeList.get(i));

      newProposalMgmtLine = OBProvider.getInstance().get(EscmProposalmgmtLine.class);
      newProposalMgmtLine.setClient(proposalMgmt.getClient());
      newProposalMgmtLine.setOrganization(proposalMgmt.getOrganization());
      newProposalMgmtLine.setCreationDate(new java.util.Date());
      newProposalMgmtLine.setCreatedBy(proposalMgmt.getCreatedBy());
      newProposalMgmtLine.setUpdated(new java.util.Date());
      newProposalMgmtLine.setUpdatedBy(proposalMgmt.getUpdatedBy());
      newProposalMgmtLine.setActive(true);
      newProposalMgmtLine.setEscmProposalmgmt(proposalMgmt);
      newProposalMgmtLine.setLineNo(lineNo);
      newProposalMgmtLine.setProduct(reqLine.getProduct());
      newProposalMgmtLine.setProductCategory(reqLine.getEscmProdcate());
      if (lastProposalMgmtLine == null && proposalAvaliableLine == null) {
        newProposalMgmtLine.setParentLineNo(null);
      } else {
        if (proposalAvaliableLine == null) {
          proposalMgmtLine = OBDal.getInstance().get(ESCMProposalMgmtLineV.class,
              lastProposalMgmtLine.getId());
          newProposalMgmtLine.setParentLineNo(proposalMgmtLine);
        } else {
          proposalMgmtLine = OBDal.getInstance().get(ESCMProposalMgmtLineV.class,
              proposalAvaliableLine.getId());
          newProposalMgmtLine.setParentLineNo(proposalMgmtLine);
          proposalAvaliableLine = null;
        }
      }
      newProposalMgmtLine.setUOM(reqLine.getUOM());
      newProposalMgmtLine.setDescription(reqLine.getDescription());
      newProposalMgmtLine.setEFINUniqueCode(reqLine.getEfinCValidcombination());
      try {
        if (selectedRow.getString("id").equals(reqLine.getId())) {
          qty = selectedRow.getString("quantity");
          newProposalMgmtLine
              .setMovementQuantity(new BigDecimal(selectedRow.getString("quantity")));
          newProposalMgmtLine.setNegotUnitPrice(reqLine.getUnitPrice());
          newProposalMgmtLine.setGrossUnitPrice(reqLine.getUnitPrice());
          newProposalMgmtLine.setNetprice(reqLine.getUnitPrice());
          newProposalMgmtLine.setLineTotal(
              reqLine.getUnitPrice().multiply(new BigDecimal(selectedRow.getString("quantity"))));
        } else {
          qty = reqLine.getQuantity().toPlainString();
          newProposalMgmtLine.setMovementQuantity(reqLine.getQuantity());
        }
      } catch (JSONException e) {
        if (log.isDebugEnabled()) {
          log.debug("Error while getting id in bidmgmt" + e, e);
        }
      }
      newProposalMgmtLine.setManual(false);
      OBDal.getInstance().save(newProposalMgmtLine);
      OBDal.getInstance().flush();
      lastProposalMgmtLine = newProposalMgmtLine;

      lineNo = lineNo + 10;
      try {
        // insert a record in Proposal management source ref
        insertsourceref(newProposalMgmtLine, reqLine.getRequisition().getId(), reqLine.getId(),
            (reqLine.getUnitPrice() == null ? (BigDecimal.ZERO).toString()
                : reqLine.getUnitPrice().toPlainString()),
            (reqLine.getNeedByDate() == null ? "" : reqLine.getNeedByDate().toString()),
            reqLine.getRequisition().getEscmDepartment().getId(), qty, reqLine.getDescription(),
            updateqtyflag);
      } catch (Exception e) {
        log.error("Exception while insertParentLines ", e);
      }
    }
  }

  /**
   * To check whether all Selected PR lines having same Agency
   * 
   * @param selectedlines
   * @param objProposal
   * @return true if it doesn't have same Agency, false it has same
   */

  public String checkSameAgency(JSONArray selectedlines, EscmProposalMgmt objProposal) {
    String agencyId = null;
    try {
      JSONObject firstRecord = selectedlines.getJSONObject(0);
      RequisitionLine reqline = OBDal.getInstance().get(RequisitionLine.class,
          firstRecord.getString("id"));
      String agencyOrg = "", selectedAgencyId = "";
      if (reqline.getRequisition().getEscmAgencyorg() != null) {
        agencyOrg = reqline.getRequisition().getEscmAgencyorg().getId();
      } else
        return agencyId;
      for (int a = 0; a < selectedlines.length(); a++) {
        JSONObject selectedRow = selectedlines.getJSONObject(a);
        reqline = OBDal.getInstance().get(RequisitionLine.class, selectedRow.getString("id"));
        if (reqline.getRequisition().getEscmAgencyorg() != null) {
          selectedAgencyId = reqline.getRequisition().getEscmAgencyorg().getId();
        } else {
          return agencyId;
        }
        if (!agencyOrg.equals(selectedAgencyId)) {
          agencyId = null;
        } else
          agencyId = agencyOrg;
      }
      /*
       * // check selected line's agency organization is same as proposal header agency Organization
       * List<EscmProposalmgmtLine> proposalLineList = objProposal.getEscmProposalmgmtLineList(); if
       * (proposalLineList.size() > 0 && !StringUtils.isEmpty(objProposal.getAgencyorg().getId())) {
       * if (!agencyOrg.equals(objProposal.getAgencyorg().getId())) { return true; } }
       */
    } catch (Exception e) {
      log.error("Exception while checking all records belongs to same dept ", e);
    }
    return agencyId;
  }

  /**
   * Check selected PR lines are already added in Bid
   * 
   * @param requisitionId
   * @return
   */
  public static Boolean prAlreadyExistBid(String requisitionId) {
    Boolean prAlreadyExistBid = false;

    try {

      // Check PR is already used in Bid
      OBQuery<Escmbidsourceref> bidSourceRef = OBDal.getInstance().createQuery(
          Escmbidsourceref.class,
          " as e where e.requisition.id = :purReqId and e.escmBidmgmtLine.escmBidmgmt.bidstatus != 'CL' "
              + " and e.escmBidmgmtLine.escmBidmgmt.bidstatus != 'CD' ");
      bidSourceRef.setNamedParameter("purReqId", requisitionId);

      if (bidSourceRef != null && bidSourceRef.list().size() > 0) {
        prAlreadyExistBid = true;
      }

      return prAlreadyExistBid;
    } catch (Exception e) {
      log.error("Exception in prAlreadyExistBid " + e.getMessage());
      return prAlreadyExistBid;
    }
  }

}
