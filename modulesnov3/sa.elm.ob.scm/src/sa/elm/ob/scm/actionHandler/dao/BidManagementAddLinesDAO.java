package sa.elm.ob.scm.actionHandler.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmBidmgmtLineV;
import sa.elm.ob.scm.EscmOrderSourceRef;
import sa.elm.ob.scm.EscmOrgView;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.EscmRequisitionlineV;
import sa.elm.ob.scm.Escmbidmgmtline;
import sa.elm.ob.scm.Escmbidsourceref;
import sa.elm.ob.scm.Escmbidsuppliers;
import sa.elm.ob.scm.event.dao.BidEventDAO;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author qualian-Divya
 * 
 */
// BidManagementAddLinesDAO handler file
public class BidManagementAddLinesDAO {
  private static final Logger log = LoggerFactory.getLogger(BidManagementAddLinesDAO.class);

  /**
   * insert/update a record in bid Source ref
   * 
   * @param Bid
   *          manangment line Object
   * @param purchasereqId
   * @param purreqLineId
   * @param unitprice
   * @param paramNeedByDate
   * @param deptId
   * @param qty
   * @param description
   * @return count if successfully inserted return the count as 1 otherwise 0
   */

  public static int insertsourceref(Escmbidmgmtline line, String purchasereqId, String purLineId,
      String unitprice, String paramNeedByDate, String deptId, String qty, String description,
      Boolean updateqtyflag, Connection conn) {
    int count = 0;
    long lineno = 10;
    Escmbidsourceref source = null;
    DateFormat dateyearFormat = new SimpleDateFormat("yyyy-MM-dd");
    BigDecimal updQty = BigDecimal.ZERO, movementQtyTemp = BigDecimal.ZERO;
    String needByDate = paramNeedByDate;
    try {
      OBContext.setAdminMode();
      List<Escmbidsourceref> ref = BidEventDAO.getSourRefList(line.getId());

      if (ref.size() > 0) {
        for (Escmbidsourceref reference : ref) {
          movementQtyTemp = movementQtyTemp.add(reference.getReservedQuantity());
        }
      }

      // get the next line no to insert the record in bid management source ref
      lineno = Utility.getLineNo("escm_bidsourceref", line.getId(), "lineNo", "escmBidmgmtLine.id");
      // check already line is exists or not in bid management soucre ref based on bid management
      // line with Purchase req line. If exists then update the source qty otherwise insert a new
      // record.
      OBQuery<Escmbidsourceref> chklineexistQry = OBDal.getInstance().createQuery(
          Escmbidsourceref.class,
          "as e where e.escmBidmgmtLine.id=:bidLineId and e.requisitionLine.id=:reqLineId");
      chklineexistQry.setNamedParameter("bidLineId", line.getId());
      chklineexistQry.setNamedParameter("reqLineId", purLineId);

      chklineexistQry.setMaxResult(1);
      // update the existing line of bid management source ref
      if (chklineexistQry.list().size() > 0) {
        source = chklineexistQry.list().get(0);
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
      // insert a new record in Bid Management Soruce Ref
      else {
        Requisition req = OBDal.getInstance().get(Requisition.class, purchasereqId);
        source = OBProvider.getInstance().get(Escmbidsourceref.class);
        source.setClient(line.getClient());
        source.setOrganization(line.getOrganization());
        source.setCreationDate(new java.util.Date());
        source.setCreatedBy(line.getCreatedBy());
        source.setUpdated(new java.util.Date());
        source.setUpdatedBy(line.getUpdatedBy());
        source.setLineNo(lineno);
        source.setRequisitionLine(OBDal.getInstance().get(RequisitionLine.class, purLineId));
        source.setRequisition(req);
        if (!unitprice.isEmpty())
          source.setUnitPrice(new BigDecimal(unitprice));
        source.setReservedQuantity(new BigDecimal(qty));
        source.setEscmBidmgmtLine(line);
        source.setDescription(description);
        if (req.getEscmAgencyorg() != null) {
          EscmOrgView agencyOrg = OBDal.getInstance().get(EscmOrgView.class,
              req.getEscmAgencyorg().getId());
          source.setAgencyOrganization(agencyOrg);
        }
        if (needByDate != null && !needByDate.equals("") && !needByDate.equals("null")
            && StringUtils.isEmpty(needByDate)) {
          needByDate = convertToGregorianDate(
              (needByDate.split("-")[0] + needByDate.split("-")[1] + needByDate.split("-")[2]));
          source.setNeedByDate(dateyearFormat.parse(needByDate));
        }
        if (deptId != null && !deptId.equals("") && !deptId.equals("null")) {
          source.setRequestingDepartment(OBDal.getInstance().get(Organization.class, deptId));
        }
        updQty = new BigDecimal(qty);
        OBDal.getInstance().save(source);
        OBDal.getInstance().flush();
        if (line.getEscmBidmgmt() != null && line.getEscmBidmgmt().getBidtype() != null
            && (line.getEscmBidmgmt().getBidtype().equals("LD")
                || line.getEscmBidmgmt().getBidtype().equals("DR")))
          insertsuppliersforLimit(source.getRequisition(), line.getEscmBidmgmt(), conn);
      }
      // update reqline bid qty
      updateReqLineBidQty(line, purLineId, updQty);
      //
      if (updateqtyflag) {
        line.setUpdated(new java.util.Date());
        line.setUpdatedBy(OBContext.getOBContext().getUser());
        line.setMovementQuantity(movementQtyTemp.add(updQty));
        OBDal.getInstance().save(line);

      }
      count = 1;
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
   * convert the hijiri date to Gregorian Date
   * 
   * @param hijriDate
   *          format( yyyymmdd)
   * @return Gregorian date return format(YYYY-MM-DD)
   */

  public static String convertToGregorianDate(String hijriDate) {
    String gregDate = "";
    try {
      SQLQuery Query = OBDal.getInstance().getSession().createSQLQuery(
          "select to_char(gregorian_date,'YYYY-MM-DD')  from eut_hijri_dates where hijri_date =:hijridate");
      Query.setParameter("hijridate", hijriDate);
      log.debug("Query:" + Query.toString());
      if (Query.list().size() > 0) {
        Object row = Query.list().get(0);
        gregDate = (String) row;
        log.debug("ConvertedDate:" + (String) row);
      }
    }

    catch (final Exception e) {
      log.error("Exception in convertToGregorianDate() Method in BidManagementAddLinesHandler : ",
          e);
      return "0";
    }
    return gregDate;
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
  private static int updateReqLineBidQty(Escmbidmgmtline line, String purLineId, BigDecimal qty) {
    int count = 0;
    try {
      OBContext.setAdminMode();
      // update the requisition line bid managment Qty
      RequisitionLine reqline = OBDal.getInstance().get(RequisitionLine.class, purLineId);
      reqline.setEscmBidmgmtQty(reqline.getEscmBidmgmtQty().add(qty));
      reqline.setUpdated(new java.util.Date());
      reqline.setUpdatedBy(line.getUpdatedBy());
      OBDal.getInstance().save(reqline);
      count = 1;

    } catch (Exception e) {
      log.error("Exception in updateReqLineBidQty in BidManagementAddLinesHandler: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  /**
   * Method to insert suppliers for the Bid Type - Limited
   * 
   * @param req
   * @param bid
   * @param conn
   */
  private static void insertsuppliersforLimit(Requisition req, EscmBidMgmt bid, Connection conn) {
    long lineno = 10;
    PreparedStatement ps = null;
    ResultSet rs = null;
    String supplierName = "";

    try {
      OBContext.setAdminMode();
      // get the next line no to insert the record in bid suppliers
      lineno = Utility.getLineNo("escm_bidsuppliers", bid.getId(), "lineNo", "escmBidmgmt.id");
      ps = conn.prepareStatement(
          "select suppliernumber,suppliername,branchname, c_location_id from escm_prsuppliers where concat(suppliernumber,'_',branchname) "
              + "  not in ( select concat(suppliernumber,'_',branchname) "
              + "  from escm_bidsuppliers  where   escm_bidmgmt_id =? )"
              + "  and m_requisition_id =?");
      ps.setString(1, bid.getId());
      ps.setString(2, req.getId());
      rs = ps.executeQuery();
      while (rs.next()) {

        Escmbidsuppliers bidsuppliers = OBProvider.getInstance().get(Escmbidsuppliers.class);
        bidsuppliers.setClient(bid.getClient());
        bidsuppliers.setOrganization(bid.getOrganization());
        bidsuppliers.setCreationDate(new java.util.Date());
        bidsuppliers.setCreatedBy(bid.getCreatedBy());
        bidsuppliers.setUpdated(new java.util.Date());
        bidsuppliers.setUpdatedBy(bid.getUpdatedBy());
        if (rs.getString("suppliernumber") != null) {
          bidsuppliers.setSuppliernumber(
              OBDal.getInstance().get(BusinessPartner.class, rs.getString("suppliernumber")));

          // Task No. 7728: Getting internal error while adding PR in Bid
          // if supplier name length is greater than 60
          supplierName = bidsuppliers.getSuppliernumber().getName();
          if (supplierName.length() > 60) {
            supplierName = supplierName.substring(0, 60);
          }
          bidsuppliers.setSupplier(supplierName);

          bidsuppliers
              .setBranchname(OBDal.getInstance().get(Location.class, rs.getString("branchname")));
          bidsuppliers.setLocationAddress(bidsuppliers.getBranchname().getLocationAddress());
          bidsuppliers.setSupplierphone(bidsuppliers.getBranchname().getPhone());
          bidsuppliers.setSupplierfax(bidsuppliers.getBranchname().getFax());
        } else {
          bidsuppliers.setSupplier(rs.getString("suppliername"));
          bidsuppliers.setLocationAddress(OBDal.getInstance().get(
              org.openbravo.model.common.geography.Location.class, rs.getString("c_location_id")));
        }
        bidsuppliers.setEscmBidmgmt(bid);
        bidsuppliers.setLineNo(lineno);
        OBDal.getInstance().save(bidsuppliers);
        OBDal.getInstance().flush();
        lineno = lineno + 10;
      }

    } catch (Exception e) {
      log.error("Exception in insertsuppliersforLimit: ", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      // close connection
      try {
        if (ps != null) {
          ps.close();
        }
        if (rs != null) {
          rs.close();
        }
      } catch (Exception e) {
        log.error(
            "An error has ocurred when trying to close the statement in insertsuppliersforLimit : ",
            e);
      }
    }
  }

  /**
   * Method to check whether the selected line already exists or not
   * 
   * @param reqline
   * @param bidmgmt
   * @param selectedRow
   */

  public static boolean checkSelectedLineAlreadyExists(RequisitionLine reqline, EscmBidMgmt bidmgmt,
      JSONObject selectedRow, Connection conn) {

    // While inserting the tree hierarchy, check whether the line is already exist.
    // if exists then check qty, if qty is zero then remove that source ref line and bidmgmtline
    // if bidmgmtline's parent has child
    Escmbidmgmtline bidMgmtLine = null;

    OBQuery<Escmbidsourceref> chkLineExists = OBDal.getInstance()
        .createQuery(Escmbidsourceref.class, "as e where e.escmBidmgmtLine.escmBidmgmt.id =:bidId "
            + " and e.requisitionLine.id =:reqLineId ");
    chkLineExists.setNamedParameter("bidId", bidmgmt.getId());
    chkLineExists.setNamedParameter("reqLineId", reqline.getId());

    chkLineExists.setMaxResult(1);
    List<Escmbidsourceref> bidSourceList = chkLineExists.list();
    if (bidSourceList != null && bidSourceList.size() > 0) {
      bidMgmtLine = bidSourceList.get(0).getEscmBidmgmtLine();
      try {
        if (new BigDecimal(selectedRow.getString("quantity")).compareTo(BigDecimal.ZERO) == 0) {
          Escmbidsourceref bidSourceRef = bidSourceList.get(0);
          bidMgmtLine = bidSourceRef.getEscmBidmgmtLine();

          Escmbidmgmtline parentMgmtLine = Utility.getObject(Escmbidmgmtline.class,
              bidMgmtLine.getParentline().getId());
          if (!checkparentHasLeaf(parentMgmtLine, bidmgmt)) {
            parentMgmtLine.setSummarylevel(false);
            OBDal.getInstance().save(parentMgmtLine);
          }
          OBDal.getInstance().remove(bidSourceRef);
          OBDal.getInstance().remove(bidMgmtLine);
          OBDal.getInstance().flush();
        } else {
          BidManagementAddLinesDAO.insertsourceref(bidMgmtLine,
              selectedRow.getString("requisition"), selectedRow.getString("id"),
              selectedRow.getString("unitPrice"), selectedRow.getString("needByDate"),
              selectedRow.getString("department"), selectedRow.getString("quantity"),
              selectedRow.getString("linedescription"), true, conn);
        }
      } catch (JSONException e) {
        log.error("Exception in BidManagementAddLinesHandler :", e);
        OBDal.getInstance().rollbackAndClose();
      }
      return true;
    }
    return false;
  }

  /**
   * Method to check whether the parent has leaf
   * 
   * @param parentMgmtLine
   * @param bidmgmt
   * @return true or false
   */

  public static boolean checkparentHasLeaf(Escmbidmgmtline parentMgmtLine, EscmBidMgmt bidmgmt) {

    OBQuery<Escmbidmgmtline> chkLineExists = OBDal.getInstance().createQuery(Escmbidmgmtline.class,
        "as e where e.escmBidmgmt.id =:bidId  and e.parentline.id =:parentLineId ");
    chkLineExists.setNamedParameter("bidId", bidmgmt.getId());
    chkLineExists.setNamedParameter("parentLineId", parentMgmtLine.getId());
    chkLineExists.setMaxResult(1);

    if (chkLineExists.list().size() > 0) {
      return true;
    }
    return false;
  }

  /**
   * 
   * @param parentList
   * @param bidmgt
   * @param line
   * @param conn
   * @return
   */
  public static void insertParentLines(ArrayList<String> parentList, EscmBidMgmt bidmgt, long line,
      Connection conn, JSONObject selectedRow, Escmbidmgmtline parentBidLine,
      boolean updateqtyflag) {
    try {
      OBContext.setAdminMode();

      Long lineNo = line;
      String qty = null;

      Escmbidmgmtline originalLine = parentBidLine;
      Escmbidmgmtline oldBidmgtline = null;
      Escmbidmgmtline bidmgtline = null;
      EscmBidmgmtLineV parentLine = null;

      for (int i = parentList.size() - 1; i >= 0; i--) {

        RequisitionLine parentReqLine = OBDal.getInstance().get(RequisitionLine.class,
            parentList.get(i));

        bidmgtline = OBProvider.getInstance().get(Escmbidmgmtline.class);
        bidmgtline.setClient(bidmgt.getClient());
        bidmgtline.setOrganization(bidmgt.getOrganization());
        bidmgtline.setCreationDate(new java.util.Date());
        bidmgtline.setCreatedBy(bidmgt.getCreatedBy());
        bidmgtline.setUpdated(new java.util.Date());
        bidmgtline.setUpdatedBy(bidmgt.getUpdatedBy());
        bidmgtline.setActive(true);
        bidmgtline.setEscmBidmgmt(bidmgt);
        bidmgtline.setLineNo(lineNo);
        bidmgtline.setProduct(parentReqLine.getProduct());
        bidmgtline.setProductCategory(parentReqLine.getEscmProdcate());
        bidmgtline.setAccountingCombination(parentReqLine.getEfinCValidcombination());
        if (oldBidmgtline == null && parentBidLine == null) {
          bidmgtline.setParentline(null);
        } else {
          if (originalLine == null) {
            parentLine = OBDal.getInstance().get(EscmBidmgmtLineV.class, oldBidmgtline.getId());
            bidmgtline.setParentline(parentLine);
          } else {
            parentLine = OBDal.getInstance().get(EscmBidmgmtLineV.class, originalLine.getId());
            bidmgtline.setParentline(parentLine);
            originalLine = null;
          }
          /*
           * if (i == 0) { if (selectedRow.getString("uniqueCode") != null)
           * bidmgtline.setAccountingCombination(OBDal.getInstance()
           * .get(AccountingCombination.class, selectedRow.getString("uniqueCode"))); }
           */
        }
        bidmgtline.setUOM(parentReqLine.getUOM());
        bidmgtline.setDescription(parentReqLine.getDescription());
        try {
          if (selectedRow.getString("id").equals(parentReqLine.getId())) {
            qty = selectedRow.getString("quantity");
            bidmgtline.setMovementQuantity(new BigDecimal(selectedRow.getString("quantity")));
          } else {
            qty = parentReqLine.getQuantity().toPlainString();
            bidmgtline.setMovementQuantity(parentReqLine.getQuantity());
          }
        } catch (JSONException e) {
          if (log.isDebugEnabled()) {
            log.debug("Error while getting id in bidmgmt" + e, e);
          }
        }
        bidmgtline.setManual(false);
        OBDal.getInstance().save(bidmgtline);
        OBDal.getInstance().flush();
        oldBidmgtline = bidmgtline;

        lineNo = lineNo + 10;

        // insert a record in bid management source ref
        BidManagementAddLinesDAO.insertsourceref(bidmgtline, parentReqLine.getRequisition().getId(),
            parentReqLine.getId(),
            parentReqLine.getUnitPrice() == null ? "0"
                : parentReqLine.getUnitPrice().toPlainString(),
            parentReqLine.getNeedByDate() == null ? "" : parentReqLine.getNeedByDate().toString(),
            parentReqLine.getRequisition().getEscmDepartment().getId(), qty,
            parentReqLine.getDescription(), updateqtyflag, conn);
      }
    } catch (Exception e) {
      log.error("Exception in insertParentLines:", e);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /**
   * 
   * @param reqline
   * @param parentList
   * @param bidmgt
   * @param line
   * @param conn
   * @return
   */

  public static void getParentLines(RequisitionLine reqline, ArrayList<String> parentList,
      EscmBidMgmt bidmgt, long line, Connection conn, JSONObject selectedRow) {
    EscmRequisitionlineV parentLine = reqline.getEscmParentlineno();
    if (parentLine != null) {
      String parentId = parentLine.getId();
      RequisitionLine parentReqLine = OBDal.getInstance().get(RequisitionLine.class, parentId);

      OBQuery<Escmbidsourceref> chkLineExists = OBDal.getInstance().createQuery(
          Escmbidsourceref.class, "as e where e.escmBidmgmtLine.escmBidmgmt.id =:bidId "
              + " and e.requisitionLine.id =:parentLineId ");
      chkLineExists.setNamedParameter("bidId", bidmgt.getId());
      chkLineExists.setNamedParameter("parentLineId", parentId);

      chkLineExists.setMaxResult(1);
      // check its parent is already exist if exists then insert the selected line in parent tree
      if (chkLineExists.list().size() > 0) {
        insertParentLines(parentList, bidmgt, line, conn, selectedRow,
            chkLineExists.list().get(0).getEscmBidmgmtLine(), false);
      } else {
        parentList.add(parentId);
        getParentLines(parentReqLine, parentList, bidmgt, line, conn, selectedRow);
      }
    } else {
      insertParentLines(parentList, bidmgt, line, conn, selectedRow, null, false);
    }

  }

  /**
   * To check whether all Selected PR lines having same Agency
   * 
   * @param selectedlines
   * @param objbid
   * @return true if it doesn't have same Agency, false it has same
   */

  public static boolean checkSameAgency(JSONArray selectedlines, EscmBidMgmt objbid) {
    try {
      JSONObject firstRecord = selectedlines.getJSONObject(0);
      RequisitionLine reqline = OBDal.getInstance().get(RequisitionLine.class,
          firstRecord.getString("id"));
      String agencyOrgId = "";
      String selectedAgencyOrgId = "";

      Organization agencyOrg = reqline.getRequisition().getEscmAgencyorg();
      if (agencyOrg != null) {
        agencyOrgId = agencyOrg.getId();
      }

      for (int a = 0; a < selectedlines.length(); a++) {
        JSONObject selectedRow = selectedlines.getJSONObject(a);
        reqline = OBDal.getInstance().get(RequisitionLine.class, selectedRow.getString("id"));
        if (reqline.getRequisition().getEscmAgencyorg() != null) {
          selectedAgencyOrgId = reqline.getRequisition().getEscmAgencyorg().getId();
        } else {
          selectedAgencyOrgId = "";
        }
        if (!agencyOrgId.equals(selectedAgencyOrgId)) {
          return true;
        }
      }

      StringBuffer whereClause = new StringBuffer();

      whereClause.append(" as bid ");
      whereClause.append(" join bid.escmBidmgmtLineList line ");
      whereClause.append(" join line.escmBidsourcerefList ref ");
      whereClause.append(
          " where (ref.agencyOrganization is null  or ref.agencyOrganization.id !=:agencyOrg) and bid.id = :bidId");

      OBQuery<EscmBidMgmt> query = OBDal.getInstance().createQuery(EscmBidMgmt.class,
          whereClause.toString());
      query.setNamedParameter("agencyOrg", agencyOrgId);
      query.setNamedParameter("bidId", objbid.getId());
      query.setFilterOnActive(false);
      if (log.isDebugEnabled()) {
        log.debug("Query" + query);
      }
      List<EscmBidMgmt> bidMgmtList = query.list();
      if (bidMgmtList != null && bidMgmtList.size() > 0) {
        return true;
      }

    } catch (Exception e) {
      log.error("Exception while checking all records belongs to same dept ", e);
    }
    return false;
  }

  public static String getBidSourcDescription(String bidLineId) {
    String description = "";
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      OBContext.setAdminMode();
      ps = OBDal.getInstance().getConnection().prepareStatement(
          " select array_to_string(array_agg( distinct description),',') as description "
              + " from escm_bidsourceref where escm_bidmgmt_line_id=? "
              + "and description is not null and description <>''");

      ps.setString(1, bidLineId);
      rs = ps.executeQuery();
      if (rs.next()) {
        description = rs.getString("description");
      }
    } catch (Exception e) {
      log.error("exception while getBidSourcDescription", e);
    } finally {
      // close connection
      try {
        if (ps != null) {
          ps.close();
        }
        if (rs != null) {
          rs.close();
        }
      } catch (Exception e) {
        log.error("An error has ocurred when trying to close the statement: " + e.getMessage(), e);
      }
    }
    return description;
  }

  /**
   * Check selected PR lines are already added in Proposal
   * 
   * @param requisitionId
   * @return
   */
  public static Boolean prAlreadyExistProposal(String requisitionId) {
    Boolean prAlreadyExistProposal = false;

    try {

      // Check PR is already used in Proposal
      OBQuery<EscmProposalsourceRef> proposalSourceRef = OBDal.getInstance().createQuery(
          EscmProposalsourceRef.class,
          " as e where e.requisition.id = :purReqId and e.escmProposalmgmtLine.escmProposalmgmt.proposalstatus != 'CL' "
              + " and e.escmProposalmgmtLine.escmProposalmgmt.proposalstatus != 'DIS' "
              + " and e.escmProposalmgmtLine.escmProposalmgmt.proposalstatus != 'WD' ");
      proposalSourceRef.setNamedParameter("purReqId", requisitionId);

      if (proposalSourceRef.list().size() > 0) {
        prAlreadyExistProposal = true;
      }

      return prAlreadyExistProposal;
    } catch (Exception e) {
      log.error("Exception in prAlreadyExistProposal " + e.getMessage());
      return prAlreadyExistProposal;
    }
  }

  /**
   * Check selected PR lines are already added in PO
   * 
   * @param requisitionId
   * @return
   */
  public static Boolean prAlreadyExistPO(String requisitionId) {
    Boolean prAlreadyExistPO = false;

    try {

      // Check PR is already used in PO
      OBQuery<EscmOrderSourceRef> poSourceRef = OBDal.getInstance().createQuery(
          EscmOrderSourceRef.class,
          " as e where e.requisition.id = :purReqId and e.salesOrderLine.salesOrder.escmAppstatus != 'ESCM_CA' ");
      poSourceRef.setNamedParameter("purReqId", requisitionId);

      if (poSourceRef.list().size() > 0) {
        prAlreadyExistPO = true;
      }

      return prAlreadyExistPO;
    } catch (Exception e) {
      log.error("Exception in prAlreadyExistPO " + e.getMessage());
      return prAlreadyExistPO;
    }
  }
}