package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.geography.City;
import org.openbravo.model.financialmgmt.calendar.Year;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.ESCMProposalMgmtLineV;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmBidmgmtLineV;
import sa.elm.ob.scm.EscmLocation;
import sa.elm.ob.scm.EscmOrgView;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.Escmbidmgmtline;
import sa.elm.ob.scm.Escmbidsourceref;
import sa.elm.ob.scm.Escmbidsuppliers;
import sa.elm.ob.scm.ad_process.ProposalManagement.ProposalManagementProcessDAO;
import sa.elm.ob.scm.ad_process.ProposalManagement.ProposalManagementProcessDAOImpl;
import sa.elm.ob.scm.event.dao.BidEventDAO;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

public class PurchaseRequisitionActionDAOImpl implements PurchaseRequisitionActionDAO {
  private static final Logger log = LoggerFactory.getLogger(PurchaseRequisitionActionDAOImpl.class);
  private static final String location_name = "رياض";

  public EscmBidMgmt createAutoBidFromPR(Requisition objRequistion, VariablesSecureApp vars,
      String contractCat) {
    EscmBidMgmt objBid = null;
    HashMap<Long, Long> childParentLineNoMap = new HashMap<Long, Long>();
    try {
      User userObj = OBDal.getInstance().get(User.class, vars.getUser());
      ProposalManagementProcessDAO proposalDAO = new ProposalManagementProcessDAOImpl();
      // get the connection
      Connection conn = OBDal.getInstance().getConnection();
      ESCMDefLookupsTypeLn conCat = OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
          contractCat);

      String yearId = proposalDAO.getFinancialYear(vars.getClient());
      objBid = Utility.getEntity(EscmBidMgmt.class);
      objBid.setClient(objRequistion.getClient());
      objBid.setOrganization(objRequistion.getOrganization());
      objBid.setCreatedBy(userObj);
      objBid.setUpdatedBy(userObj);
      if (objRequistion.getEscmProcesstype().equals("DP")) {
        objBid.setBidtype("DR");
      } else if (objRequistion.getEscmProcesstype().equals("LB")) {
        objBid.setBidtype("LD");
      } else if (objRequistion.getEscmProcesstype().equals("PB")) {
        objBid.setBidtype("TR");
      }
      objBid.setBidpreparername(userObj);
      objBid.setBidcreationdate(new Date());
      objBid.setFinanyear(OBDal.getInstance().get(Year.class, yearId));
      objBid.setBidstatus("IA");
      objBid.setBidname(objRequistion.getDescription());
      objBid.setBidpurpose(objRequistion.getDescription());
      // find location based on location riyadh
      EscmLocation objLocation = getLocaction(location_name);
      if (objLocation != null) {
        objBid.setDocumentaddress(objLocation);
        objBid.setSubmissionadd(objLocation);
        objBid.setEnvelopeadd(objLocation);
      }
      objBid.setExecutionadd(location_name);
      objBid.setBidappstatus("DR");
      objBid.setEscmDocaction("CO");
      objBid.setRole(OBDal.getInstance().get(Role.class, vars.getRole()));
      objBid.setEfinBudgetinitial(objRequistion.getEfinBudgetint());

      // Update contract category from PR
      if (objRequistion.getEscmContactType() != null) {
        objBid.setContractType(objRequistion.getEscmContactType());
      } else if (contractCat != null) {
        objBid.setContractType(conCat);
      }
      OBDal.getInstance().save(objBid);
      OBDal.getInstance().flush();

      // add the bid lines and source reference
      if (objRequistion.getProcurementRequisitionLineList().size() > 0) {
        for (RequisitionLine objLine : objRequistion.getProcurementRequisitionLineList()) {
          if (!objLine.getEscmStatus().equals("ESCM_CA")) {
            Escmbidmgmtline bidmgtline = Utility.getEntity(Escmbidmgmtline.class);
            bidmgtline.setClient(objRequistion.getClient());
            bidmgtline.setOrganization(objRequistion.getOrganization());
            bidmgtline.setCreatedBy(objRequistion.getCreatedBy());
            bidmgtline.setUpdatedBy(objRequistion.getCreatedBy());
            bidmgtline.setActive(true);
            bidmgtline.setEscmBidmgmt(objBid);
            bidmgtline.setLineNo(objLine.getLineNo());
            bidmgtline.setProduct(objLine.getProduct());
            bidmgtline.setProductCategory(objLine.getEscmProdcate());
            bidmgtline.setParentline(null);
            bidmgtline.setUOM(objLine.getUOM());
            bidmgtline.setDescription(objLine.getDescription());
            bidmgtline.setMovementQuantity(objLine.getQuantity());
            bidmgtline.setManual(false);
            bidmgtline.setAccountingCombination(objLine.getEfinCValidcombination());
            bidmgtline.setUniquecodename(objLine.getEfinUniquecodename());
            bidmgtline.setSummarylevel(objLine.isEscmIssummary());
            childParentLineNoMap.put(objLine.getLineNo(),
                objLine.getEscmParentlineno() == null ? null
                    : objLine.getEscmParentlineno().getLineNo());

            if (bidmgtline.getEscmBidmgmt() != null
                && bidmgtline.getEscmBidmgmt().getBidtype() != null
                && (bidmgtline.getEscmBidmgmt().getBidtype().equals("LD")
                    || bidmgtline.getEscmBidmgmt().getBidtype().equals("DR")))
              insertsuppliersforLimit(objRequistion, bidmgtline.getEscmBidmgmt(), conn);

            OBDal.getInstance().save(bidmgtline);
            OBDal.getInstance().flush();
            // insert a record in bid management source ref
            int count = insertsourcerefForBidmanagementLines(bidmgtline, objRequistion.getId(),
                objLine.getId(), objLine.getUnitPrice(), objLine.getNeedByDate(), "",
                objLine.getQuantity(), objLine.getDescription(), false, conn);
          }

        }
      }

      OBDal.getInstance().save(objBid);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(objBid);

      // Update parent line reference
      for (Escmbidmgmtline bidLine : objBid.getEscmBidmgmtLineList()) {
        Long parentLineNo = childParentLineNoMap.get(bidLine.getLineNo());
        if (parentLineNo != null) {
          OBQuery<Escmbidmgmtline> parentLineQry = OBDal.getInstance().createQuery(
              Escmbidmgmtline.class,
              " as e where e.lineNo=:parentlineNo and  e.escmBidmgmt.id=:bidID ");
          parentLineQry.setNamedParameter("parentlineNo", parentLineNo);
          parentLineQry.setNamedParameter("bidID", objBid.getId());

          List<Escmbidmgmtline> parentLineList = parentLineQry.list();
          if (parentLineList.size() > 0) {
            bidLine.setParentline(
                Utility.getObject(EscmBidmgmtLineV.class, parentLineList.get(0).getId()));
            OBDal.getInstance().save(bidLine);
          }
        }
      }

    } catch (Exception e) {
      log.error("Exception in createAutoBidFromPR: ", e);
      OBDal.getInstance().rollbackAndClose();
    }
    return objBid;
  }

  private EscmLocation getLocaction(String locationName) {
    EscmLocation objLocation = null;
    try {
      City objCity = null;
      OBQuery<City> city_query = OBDal.getInstance().createQuery(City.class,
          "as e where e.name=:location_name");
      city_query.setNamedParameter("location_name", locationName);
      if (city_query != null && city_query.list().size() > 0) {
        objCity = city_query.list().get(0);
        if (objCity != null && objCity.getEscmLocationList().size() > 0) {
          objLocation = objCity.getEscmLocationList().get(0);
        }
      }

    } catch (Exception e) {
      objLocation = null;
      log.error("Exception in getLocaction", e);
    }
    return objLocation;
  }

  @Override
  public EscmProposalMgmt createAutoProposalFromPR(Requisition objRequistion,
      VariablesSecureApp vars, String is_pee_required, String supplier) {
    EscmProposalMgmt objProposal = null;
    HashMap<Long, Long> childParentLineNoMap = new HashMap<Long, Long>();
    try {
      objProposal = Utility.getEntity(EscmProposalMgmt.class);
      objProposal.setClient(objRequistion.getClient());
      objProposal.setOrganization(objRequistion.getOrganization());
      if (objRequistion.getEscmProcesstype().equals("DP")) {
        objProposal.setProposalType("DR");
      }
      if (is_pee_required.equals("true")) {
        objProposal.setNeedEvaluation(Boolean.TRUE);
      } else {
        objProposal.setNeedEvaluation(Boolean.FALSE);
      }
      objProposal.setBuyername(OBDal.getInstance().get(User.class, vars.getUser()));
      objProposal.setProposalstatus("DR");
      objProposal.setProposalno(
          UtilityDAO.getTransactionSequence(objRequistion.getOrganization().getId(), "PMG"));

      objProposal.setFinancialYear(objRequistion.getEscmFinancialYear());
      objProposal.setEfinBudgetinitial(objRequistion.getEfinBudgetint());
      objProposal.setSubmissiondate(new Date());
      objProposal.setBidName(objRequistion.getDescription());
      objProposal.setSubmissiontime(new SimpleDateFormat("HH:MM").format(new Date().getTime()));
      if (!supplier.equals("null")) {
        BusinessPartner objSupplier = OBDal.getInstance().get(BusinessPartner.class, supplier);
        objProposal.setSupplier(objSupplier);
        if (objSupplier.getBusinessPartnerLocationList().size() > 0) {
          objProposal.setBranchName(objSupplier.getBusinessPartnerLocationList().get(0));
        }

      }

      objProposal.setEscmDocaction("CO");
      objProposal.setRole(OBContext.getOBContext().getRole());
      objProposal.setCurrency(objRequistion.getCurrency());

      // Update contract category from PR
      if (objRequistion.getEscmContactType() != null) {
        objProposal.setContractType(objRequistion.getEscmContactType());
      }

      OBDal.getInstance().save(objProposal);
      OBDal.getInstance().flush();
      for (RequisitionLine objReqLine : objRequistion.getProcurementRequisitionLineList()) {
        if (!objReqLine.getEscmStatus().equals("ESCM_CA")) {
          EscmProposalmgmtLine objProposalLine = OBProvider.getInstance()
              .get(EscmProposalmgmtLine.class);
          objProposalLine = OBProvider.getInstance().get(EscmProposalmgmtLine.class);
          objProposalLine.setClient(objReqLine.getClient());
          objProposalLine.setOrganization(objReqLine.getOrganization());
          objProposalLine.setCreationDate(new java.util.Date());
          objProposalLine.setCreatedBy(objReqLine.getCreatedBy());
          objProposalLine.setUpdated(new java.util.Date());
          objProposalLine.setUpdatedBy(objReqLine.getUpdatedBy());
          objProposalLine.setActive(true);
          objProposalLine.setEscmProposalmgmt(objProposal);
          objProposalLine.setLineNo(objReqLine.getLineNo());
          objProposalLine.setManual(false);

          objProposalLine.setSummary(objReqLine.isEscmIssummary());
          childParentLineNoMap.put(objReqLine.getLineNo(),
              objReqLine.getEscmParentlineno() == null ? null
                  : objReqLine.getEscmParentlineno().getLineNo());

          if (objReqLine.getProduct() != null)
            objProposalLine.setProduct(objReqLine.getProduct());
          else
            objProposalLine.setProduct(null);
          objProposalLine.setUOM(objReqLine.getUOM());
          objProposalLine.setNegotUnitPrice(objReqLine.getUnitPrice());
          objProposalLine.setGrossUnitPrice(objReqLine.getUnitPrice());
          objProposalLine.setNetprice(objReqLine.getUnitPrice());
          if (objReqLine.getEscmProdcate() != null) {
            objProposalLine.setProductCategory(objReqLine.getEscmProdcate());
          }
          if (objReqLine.getUnitPrice() != null)
            objProposalLine
                .setLineTotal(objReqLine.getUnitPrice().multiply(objReqLine.getQuantity()));
          objProposalLine.setDescription(objReqLine.getDescription());
          objProposalLine.setMovementQuantity(objReqLine.getQuantity());
          objProposalLine.setEFINUniqueCode(objReqLine.getEfinCValidcombination());
          objProposalLine.setEFINUniqueCodeName(objReqLine.getEfinUniquecodename());
          OBDal.getInstance().save(objProposalLine);
          OBDal.getInstance().flush();

          // insert a record in proposal source reference
          int count = insertsourcerefForProposal(objProposalLine, objRequistion.getId(),
              objReqLine.getId(), objReqLine.getUnitPrice(), objReqLine.getNeedByDate(), "",
              objReqLine.getQuantity(), objReqLine.getDescription(), false);
        }

      }
      OBDal.getInstance().save(objProposal);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(objProposal);

      // Update parent line reference
      for (EscmProposalmgmtLine proposalLine : objProposal.getEscmProposalmgmtLineList()) {
        Long parentLineNo = childParentLineNoMap.get(proposalLine.getLineNo());
        if (parentLineNo != null) {
          OBQuery<EscmProposalmgmtLine> parentLineQry = OBDal.getInstance().createQuery(
              EscmProposalmgmtLine.class,
              " as e where e.lineNo =:parentlineNo and  e.escmProposalmgmt.id=:proposalID ");
          parentLineQry.setNamedParameter("parentlineNo", parentLineNo);
          parentLineQry.setNamedParameter("proposalID", objProposal.getId());

          List<EscmProposalmgmtLine> parentLineList = parentLineQry.list();
          if (parentLineList.size() > 0) {
            proposalLine.setParentLineNo(
                Utility.getObject(ESCMProposalMgmtLineV.class, parentLineList.get(0).getId()));
            OBDal.getInstance().save(proposalLine);
          }
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
      objProposal = null;
      log.error("Exception in getLocaction", e);
    }
    return objProposal;
  }

  @Override
  public Boolean returnPR(Requisition objRequisition, VariablesSecureApp vars, String comments) {
    try {
      // update the Purchase Requisition as returned
      objRequisition.setEscmPrReturn(Boolean.TRUE);
      objRequisition.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
      objRequisition.setUpdated(new Date());
      OBDal.getInstance().save(objRequisition);
      OBDal.getInstance().flush();

      // solve alerts
      AlertUtility.solveAlerts(objRequisition.getId());

      // insert alert to the requester
      User retrunUser = OBDal.getInstance().get(User.class, vars.getUser());
      AlertUtility.alertInsertionRole(objRequisition.getId(),
          objRequisition.getDocumentNo() + "-" + objRequisition.getDescription(),
          objRequisition.getEscmAdRole().getId(), objRequisition.getCreatedBy().getId(),
          objRequisition.getClient().getId(),
          sa.elm.ob.scm.properties.Resource.getProperty("scm.pr.returned", vars.getLanguage())
              + " by " + retrunUser.getName(),
          "NEW", AlertWindow.PurchaseRequisition, "scm.pr.returned '" + retrunUser.getName() + "'",
          Constants.GENERIC_TEMPLATE);
      // insert action history for return process
      if (!StringUtils.isEmpty(objRequisition.getId())) {
        JSONObject historyData = new JSONObject();

        historyData.put("ClientId", vars.getClient());
        historyData.put("OrgId", vars.getOrg());
        historyData.put("RoleId", vars.getRole());
        historyData.put("UserId", vars.getUser());
        historyData.put("HeaderId", objRequisition.getId());
        if (comments != null)
          historyData.put("Comments", comments);
        else
          historyData.put("Comments", "");
        historyData.put("Status", "RETURN");
        historyData.put("NextApprover", "");
        historyData.put("HistoryTable", ApprovalTables.REQUISITION_HISTORY);
        historyData.put("HeaderColumn", ApprovalTables.REQUISITION_HEADER_COLUMN);
        historyData.put("ActionColumn", ApprovalTables.REQUISITION_DOCACTION_COLUMN);

        Utility.InsertApprovalHistory(historyData);
      } else {
        return false;
      }
      return true;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in returnPR", e);
      return false;
    }

  }

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

  private static int insertsourcerefForBidmanagementLines(Escmbidmgmtline line,
      String purchasereqId, String purLineId, BigDecimal unitprice, Date paramNeedByDate,
      String deptId, BigDecimal qty, String description, Boolean updateqtyflag, Connection conn) {
    int count = 0;
    long lineno = 10;
    Escmbidsourceref source = null;
    BigDecimal updQty = BigDecimal.ZERO, movementQtyTemp = BigDecimal.ZERO;
    Date needByDate = paramNeedByDate;
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
        if (source.getReservedQuantity().compareTo(qty) > 0) {
          updQty = (source.getReservedQuantity().subtract(qty)).negate();
        } else {
          updQty = (source.getReservedQuantity().subtract(qty)).abs();
        }
        source.setReservedQuantity(qty);
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
        source.setUnitPrice(unitprice);
        source.setReservedQuantity(qty);
        source.setEscmBidmgmtLine(line);
        source.setDescription(description);
        if (req.getEscmAgencyorg() != null) {
          EscmOrgView agencyOrg = OBDal.getInstance().get(EscmOrgView.class,
              req.getEscmAgencyorg().getId());
          source.setAgencyOrganization(agencyOrg);
        }
        if (needByDate != null) {
          source.setNeedByDate(needByDate);
        }
        if (deptId != null && !deptId.equals("") && !deptId.equals("null")) {
          source.setRequestingDepartment(OBDal.getInstance().get(Organization.class, deptId));
        }
        updQty = qty;
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
      log.error("Exception in insertsourceref in PurchaseRequisitionActionDAOImpl: ", e);
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
      e.printStackTrace();
      log.error("Exception in insertsuppliersforLimit in PurchaseRequisitionActionDAOImpl: ", e);
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
      log.error("Exception in updateReqLineBidQty in PurchaseRequisitionActionDAOImpl: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

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
  public static int insertsourcerefForProposal(EscmProposalmgmtLine Objline, String purchasereqId,
      String purLineId, BigDecimal unitprice, Date paramNeedByDate, String deptId, BigDecimal qty,
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
        if (source.getReservedQuantity().compareTo(qty) > 0) {
          updQty = (source.getReservedQuantity().subtract(qty)).negate();
        } else {
          updQty = (source.getReservedQuantity().subtract(qty)).abs();
        }
        source.setReservedQuantity(qty);
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
        source.setReservedQuantity(qty);
        source.setEscmProposalmgmtLine(line);

        updQty = qty;
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
      log.error("Exception in insertsourcerefForProposal in Purchase Requistion Action: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;

  }

  /**
   * Update the Proposal Qty in Purchase Requisition line once requisition line associated with
   * Proposal.
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
      log.error("Exception in updateReqLineOrderQty in Purchase Requisition Action: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

}
