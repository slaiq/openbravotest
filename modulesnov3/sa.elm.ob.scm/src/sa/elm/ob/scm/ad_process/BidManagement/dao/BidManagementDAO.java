package sa.elm.ob.scm.ad_process.BidManagement.dao;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

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
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;
import org.openbravo.scheduling.ProcessBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.Escmbidconfiguration;
import sa.elm.ob.scm.Escmbidmgmthistory;
import sa.elm.ob.scm.Escmbidmgmtline;
import sa.elm.ob.scm.Escmbidsourceref;
import sa.elm.ob.scm.Escmopenenvcommitee;
import sa.elm.ob.scm.Escmsalesvoucher;
import sa.elm.ob.scm.escmannoucementsv;
import sa.elm.ob.scm.ad_process.POandContract.dao.POContractSummaryDAO;
import sa.elm.ob.scm.ad_process.ProposalManagement.ProposalManagementActionMethod;
import sa.elm.ob.utility.EutDocappDelegateln;

/**
 * 
 * @author qualian-Divya
 * 
 */
// BidManagementDAO process file
public class BidManagementDAO {
  private static final Logger log = LoggerFactory.getLogger(BidManagementDAO.class);

  public static JSONObject checkAssociatePRTypeEncumbrance(EscmBidMgmt bidmgmt) {
    boolean isAssociatePREncumbrance = false;
    List<EfinBudgetManencum> encum = new ArrayList<EfinBudgetManencum>();
    EfinBudgetManencum tempencumObj = null;
    JSONObject result = new JSONObject(), json = null;
    JSONArray array = new JSONArray();
    try {

      // check bid management line size greater than zero
      if (bidmgmt != null && bidmgmt.getEscmBidmgmtLineList().size() > 0) {
        for (Escmbidmgmtline lineObj : bidmgmt.getEscmBidmgmtLineList()) {
          // check bid management line source ref list size greater than zero
          if (lineObj.getEscmBidsourcerefList().size() > 0) {
            for (Escmbidsourceref srcrefObj : lineObj.getEscmBidsourcerefList()) {

              // chk source ref having purchase requisition and corresponding purchase requistion is
              // PRE(Purchase Encumbrance Type)
              if (srcrefObj.getRequisition() != null
                  && srcrefObj.getRequisition().getEfinBudgetManencum() != null && srcrefObj
                      .getRequisition().getEfinBudgetManencum().getEncumType().equals("PRE")) {

                // if PR is associated then set the flag as true
                isAssociatePREncumbrance = true;
                if (isAssociatePREncumbrance) {

                  // add the Encumbrance list if PR Associate with different Encumbrance
                  if (tempencumObj == null) {
                    tempencumObj = srcrefObj.getRequisition().getEfinBudgetManencum();
                    encum.add(srcrefObj.getRequisition().getEfinBudgetManencum());
                  } else if (tempencumObj != null
                      && !tempencumObj.equals(srcrefObj.getRequisition().getEfinBudgetManencum())) {
                    encum.add(srcrefObj.getRequisition().getEfinBudgetManencum());
                    log.debug("encum:" + encum.size());
                  }
                }
              }
            }
          }
        }
      }
      // avoid the encumbrance duplicate
      HashSet<EfinBudgetManencum> encumset = new HashSet<EfinBudgetManencum>(encum);

      // if encumbrance list is zero then chk same encumbrance used in anyother bid
      if (encumset != null && encumset.size() == 1 && isAssociatePREncumbrance) {
        EfinBudgetManencum encumObj = encumset.iterator().next();

        // check same encumbrance associate with bid or not
        OBQuery<EscmBidMgmt> bidQry = OBDal.getInstance().createQuery(EscmBidMgmt.class,
            " as e where e.encumbrance.id=:encumId and id<> :bidId ");
        bidQry.setNamedParameter("encumId", encumObj.getId());
        bidQry.setNamedParameter("bidId", bidmgmt.getId());
        log.debug("size:" + bidQry.list().size());
        bidQry.setMaxResult(1);

        // if same encumbrance associate with another bid set the JSONObject type is "SPLIT"
        if (bidQry.list().size() > 0) {
          result.put("type", "SPLIT");
          result.put("encumbrance", encumObj);
          result.put("bidObj", bidQry.list().get(0));
        }
        // else type "NO"
        else {
          bidmgmt.setEncumbrance(encumObj);
          OBDal.getInstance().save(bidmgmt);
          result.put("encumbrance", encumObj);
          result.put("type", "NO");

        }
      }
      // if PR is different encumbrances set the type is "MERGE"
      else if (encumset != null && encumset.size() > 1 && isAssociatePREncumbrance) {
        result.put("type", "MERGE");

        Iterator<EfinBudgetManencum> iterator = encumset.iterator();
        while (iterator.hasNext()) {
          json = new JSONObject();
          json.put("encumbrance", iterator.next());
          array.put(json);
        }
        result.put("encumList", array);

      }
      result.put("isAssociatePREncumbrance", isAssociatePREncumbrance);
      log.debug("result:" + result);
    } catch (Exception e) {
      log.error("Exception in checkAssociatePRTypeEncumbrance " + e.getMessage());
      return result;
    }
    return result;
  }

  /**
   * check associated PR in Bid Full Qty used or partialy used or combine more than one Encumbrance
   * 
   * @param bidmgmt
   * @return Jsonobject of Encumbrance List, (Type-Split or Merge),PR is associated or Not
   */
  public static JSONObject checkFullPRQtyUitlizeorNot(EscmBidMgmt bidmgmt) {
    List<Requisition> req = new ArrayList<Requisition>();
    List<EfinBudgetManencum> enc = new ArrayList<EfinBudgetManencum>();
    boolean isAssociatePREncumbrance = false;
    int srcrefReqLineCount = 0, reqLineCount = 0, encReqCount = 0, reqCount = 0;
    BigDecimal srcrefLineQty = BigDecimal.ZERO, reqlineAmt = BigDecimal.ZERO,
        reqLineQty = BigDecimal.ZERO;
    Boolean isLineCountSame = true, isLineQtySame = true, isEncReqCountSame = true,
        isEncumAppAmtZero = true;
    // Boolean isLineUniqCodeSame = true;
    JSONObject result = new JSONObject();

    try {

      if (bidmgmt != null) {
        if (bidmgmt.getEscmBidmgmtLineList().size() > 0) {
          for (Escmbidmgmtline line : bidmgmt.getEscmBidmgmtLineList()) {
            if (!line.isSummarylevel()) {
              if (line.getEscmBidsourcerefList().size() > 0) {
                for (Escmbidsourceref srcrefObj : line.getEscmBidsourcerefList()) {
                  // chk source ref having purchase requisition and corresponding purchase
                  // requistion
                  // is PRE(Purchase Encumbrance Type)
                  if (srcrefObj.getRequisition() != null
                      && srcrefObj.getRequisition().getEfinBudgetManencum() != null && srcrefObj
                          .getRequisition().getEfinBudgetManencum().getEncumType().equals("PRE")) {
                    // if PR is associated then set the flag as true
                    isAssociatePREncumbrance = true;
                    if (isAssociatePREncumbrance) {

                      // if (srcrefObj.getRequisitionLine().getEfinCValidcombination() != null
                      // && line.getAccountingCombination() != null
                      // && !srcrefObj.getRequisitionLine().getEfinCValidcombination().getId()
                      // .equals(line.getAccountingCombination().getId())) {
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
          OBQuery<Escmbidsourceref> srcref = OBDal.getInstance().createQuery(Escmbidsourceref.class,
              " as e where e.escmBidmgmtLine.id in ( select b.id from escm_bidmgmt_line b"
                  + " where b.escmBidmgmt.id=:bidId) and e.requisition.id=:reqId ");
          srcref.setNamedParameter("bidId", bidmgmt.getId());
          srcref.setNamedParameter("reqId", reqObj.getId());

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
                OBQuery<Escmbidsourceref> srcref = OBDal.getInstance().createQuery(
                    Escmbidsourceref.class,
                    " as e where e.escmBidmgmtLine.id in ( select b.id from escm_bidmgmt_line b"
                        + " where b.escmBidmgmt.id=:bidId )  and e.requisition.id=:reqId"
                        + " and e.requisitionLine.id=:reqLineId ");
                srcref.setNamedParameter("bidId", bidmgmt.getId());
                srcref.setNamedParameter("reqId", reqObj.getId());
                srcref.setNamedParameter("reqLineId", line.getId());

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
              .filter(a -> a.getAPPAmt().compareTo(BigDecimal.ZERO) > 0)
              .map(a -> a.getRemainingAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

          if (encumbrance.getRevamount().compareTo(encumbrance.getAppliedAmount()) != 0) {
            if (reqlineAmt.compareTo((encumbrance.getAppliedAmount().add(remainigAmt))) == 0) {
              isEncumAppAmtZero = true;
            } else {
              isEncumAppAmtZero = false;
            }
          }
          if (encumbrance.getRevamount().compareTo(encumbrance.getAppliedAmount()) != 0) {
            isEncumAppAmtZero = false;
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
        // if line qty same set isFullQtyUsed as "true"
        if (isLineQtySame && isEncumAppAmtZero && isEncReqCountSame) {
          result.put("encumbrance", encumId);
          result.put("isFullQtyUsed", true);
          result.put("isLineCountSame", true);
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
      log.error("Exception in checkFullPRAmtUitlizeorNot ", e.getMessage());
      return result;
    }
    return result;

  }

  /**
   * This Method is used to split the PR
   * 
   * @param result
   * @param bidmgmt
   * @param order
   * @param proposal
   */
  public static void splitPR(JSONObject result, EscmBidMgmt bidmgmt, Order order,
      EscmProposalMgmt proposal, EscmProposalmgmtLine proposalmgmtline) {
    EfinBudgetManencum newEncumbrance = null, oldEncumbrance = null;
    try {
      // split the PR

      // create Encumbrance
      if (result.getString("encumbrance") != null) {
        oldEncumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
            result.getString("encumbrance"));
        // create the Encumbrane
        if (bidmgmt != null) {
          newEncumbrance = insertEncumbrance(bidmgmt, oldEncumbrance);
        } else if (proposal != null) {
          newEncumbrance = ProposalManagementActionMethod.insertEncumbranceproposal(proposal,
              oldEncumbrance);
        } else {
          newEncumbrance = POContractSummaryDAO.insertEncumbranceOrder(order, oldEncumbrance);
        }

        // insert the Encumbrance lines
        if (bidmgmt != null) {
          // insertEncumbranceLines(bidmgmt, newEncumbrance, oldEncumbrance, result);
          insertModification(bidmgmt, newEncumbrance, oldEncumbrance, result);
        } else if (proposal != null) {
          ProposalManagementActionMethod.insertEncumbranceLinesProsal(proposal, newEncumbrance,
              oldEncumbrance, result, proposalmgmtline);
        } else {
          // POContractSummaryDAO.insertEncumbranceLinesOrder(order, newEncumbrance, oldEncumbrance,
          // result);
          POContractSummaryDAO.insertModification(order, newEncumbrance, oldEncumbrance, result);
        }
        newEncumbrance.setDocumentStatus("CO");
        newEncumbrance.setAction("PD");
        OBDal.getInstance().save(newEncumbrance);
        if (bidmgmt != null) {
          bidmgmt.setEncumbrance(newEncumbrance);
          bidmgmt.setEfinIsbudgetcntlapp(true);
          OBDal.getInstance().save(bidmgmt);
        } else if (proposal != null) {
          proposal.setEfinEncumbrance(newEncumbrance);
          proposal.setEfinIsbudgetcntlapp(true);
          OBDal.getInstance().save(proposal);
        } else {
          order.setEfinBudgetManencum(newEncumbrance);
          order.setEfinEncumbered(true);
          if (order.getEfinBudgetManencum().getBusinessPartner() == null) {
            order.getEfinBudgetManencum().setBusinessPartner(order.getBusinessPartner());
          }
          OBDal.getInstance().save(order);

        }
      }
    } catch (OBException e) {
      log.error(" Exception while insertAutoEncumbrance: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception in splitPR " + e.getMessage(), e);
    }
  }

  /**
   * insert modification for encumbrance
   * 
   * @param bidmgmt
   * @param encumbrancenewObj
   * @param oldencumbranceObj
   * @param result
   */
  public static void insertModification(EscmBidMgmt bidmgmt, EfinBudgetManencum encumbrancenewObj,
      EfinBudgetManencum oldencumbranceObj, JSONObject result) {
    BigDecimal Amount = BigDecimal.ZERO;
    JSONObject json = null, jsonencum = null, result1 = null;
    JSONObject prResult = null;
    EfinBudgetManencumlines manualline = null;
    EfinBudManencumRev manEncumRev = null;
    String tempBidLineId = null;
    try {
      OBContext.setAdminMode();

      // get PR detail based on associated Bid source ref .
      prResult = getPRDetailsBasedOnBRQty(bidmgmt);

      log.debug("prResult:" + prResult);
      if (prResult != null && prResult.getJSONObject("prListarray") != null) {
        result1 = prResult.getJSONObject("prListarray");
        JSONArray array = result1.getJSONArray("list");
        for (int i = 0; i < array.length(); i++) {
          json = array.getJSONObject(i);
          Escmbidmgmtline ln = OBDal.getInstance().get(Escmbidmgmtline.class,
              json.getString("bidlineId"));
          JSONArray encumarray = json.getJSONArray("encList");
          for (int j = 0; j < encumarray.length(); j++) {
            jsonencum = encumarray.getJSONObject(j);
            if (jsonencum.has("encumId") && jsonencum.getString("encumId") != null
                && jsonencum.has("validcomId") && jsonencum.getString("validcomId") != null) {
              // get old encumbrance line
              OBQuery<EfinBudgetManencumlines> lines = OBDal.getInstance().createQuery(
                  EfinBudgetManencumlines.class, " as e where e.manualEncumbrance.id=:encumId"
                      + " and e.accountingCombination.id=:accComId");
              lines.setNamedParameter("encumId", jsonencum.getString("encumId"));
              lines.setNamedParameter("accComId", jsonencum.getString("validcomId"));
              lines.setMaxResult(1);
              if (lines.list().size() > 0) {
                // decrease the rev amount and remaining amount
                Amount = new BigDecimal(jsonencum.getString("encamount"));
                log.debug("amount1:" + Amount);
                EfinBudgetManencumlines encumline = lines.list().get(0);

                // insert the Encumbrance revision entry(-ve value)
                manEncumRev = insertEncumbranceModification(encumline, Amount.negate(), manualline,
                    "BID", ln, null);
                if (tempBidLineId == null || !tempBidLineId.equals(json.getString("bidlineId"))) {
                  manualline = insertEncumbranceLines(bidmgmt, encumbrancenewObj, oldencumbranceObj,
                      json);
                }
                if (manualline != null && manEncumRev != null) {
                  manEncumRev.setSRCManencumline(manualline);
                  OBDal.getInstance().save(manEncumRev);
                }

                encumline.setAPPAmt(encumline.getAPPAmt().subtract(Amount));
                OBDal.getInstance().save(encumline);
                OBDal.getInstance().flush();
              }
            }
            tempBidLineId = json.getString("bidlineId");
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in insertEncumbranceLines " + e.getMessage());
    }
  }

  /**
   * This Method is used to merge the PR
   * 
   * @param result
   * @param bidmgmt
   */
  public static void MergePR(JSONObject result, EscmBidMgmt bidmgmt) {
    EfinBudgetManencum newEncumbrance = null, oldEncumbrance = null;
    try {
      // Merge the PR

      // create Encumbrance
      if (result.getString("encumbrance") != null) {
        oldEncumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
            result.getString("encumbrance"));
        newEncumbrance = insertEncumbrance(bidmgmt, oldEncumbrance);

        insertEncumbranceLines(bidmgmt, newEncumbrance, oldEncumbrance, result);
        log.debug("newEncumbrance:" + newEncumbrance);
        newEncumbrance.setDocumentStatus("CO");
        newEncumbrance.setAction("PD");
        OBDal.getInstance().save(newEncumbrance);

        bidmgmt.setEncumbrance(newEncumbrance);
        bidmgmt.setEfinIsbudgetcntlapp(true);
        OBDal.getInstance().save(bidmgmt);
      }
    } catch (Exception e) {
      log.error("Exception in MergePR " + e.getMessage());

    }
  }

  /**
   * Method to Insert the Encumbrance Header
   * 
   * @param bidmgmt
   * @param encumbranceObj
   * @return
   */
  public static EfinBudgetManencum insertEncumbrance(EscmBidMgmt bidmgmt,
      EfinBudgetManencum encumbranceObj) {
    EfinBudgetManencum encumbrance = null;
    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    try {
      OBContext.setAdminMode();

      // insert the Encumbrance
      encumbrance = OBProvider.getInstance().get(EfinBudgetManencum.class);
      encumbrance.setClient(bidmgmt.getClient());
      encumbrance.setOrganization(bidmgmt.getOrganization());
      encumbrance.setActive(true);
      encumbrance.setUpdatedBy(bidmgmt.getCreatedBy());
      encumbrance.setCreationDate(new java.util.Date());
      encumbrance.setCreatedBy(bidmgmt.getCreatedBy());
      encumbrance.setUpdated(new java.util.Date());
      encumbrance.setAccountingDate(dateFormat.parse(dateFormat.format(new Date())));
      encumbrance.setTransactionDate(dateFormat.parse(dateFormat.format(new Date())));
      encumbrance.setEncumType("BE");
      encumbrance.setAuto(true);
      encumbrance.setDocumentStatus("DR");
      encumbrance.setSalesCampaign(encumbranceObj.getSalesCampaign());
      encumbrance.setSalesRegion(encumbranceObj.getSalesRegion());
      encumbrance.setEncumStage("BE");
      encumbrance.setBudgetInitialization(encumbranceObj.getBudgetInitialization());
      encumbrance.setDescription(bidmgmt.getBidname());
      OBDal.getInstance().save(encumbrance);
      OBDal.getInstance().flush();

      return encumbrance;

    } catch (OBException e) {
      log.error(" Exception while insertAutoEncumbrance: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception in insertEncumbrance " + e.getMessage());
    }
    return encumbrance;
  }

  /**
   * Method to Insert the Encumbrance Lines
   * 
   * @param bidmgmt
   * @param encumbrancenewObj
   * @param oldencumbranceObj
   * @param json
   * @return
   */
  public static EfinBudgetManencumlines insertEncumbranceLines(EscmBidMgmt bidmgmt,
      EfinBudgetManencum encumbrancenewObj, EfinBudgetManencum oldencumbranceObj, JSONObject json) {
    Long lineno = 10L;
    BigDecimal totalAmount = BigDecimal.ZERO;
    JSONObject prResult = null;
    EfinBudgetManencumlines manualline = null;
    try {
      OBContext.setAdminMode();

      log.debug("prResult:" + prResult);
      if (json != null) {
        Escmbidmgmtline ln = OBDal.getInstance().get(Escmbidmgmtline.class,
            json.getString("bidlineId"));
        totalAmount = new BigDecimal(json.getString("totalamount"));

        // check already unqiuecode exists or not
        OBQuery<EfinBudgetManencumlines> lnexistQry = OBDal.getInstance().createQuery(
            EfinBudgetManencumlines.class,
            " as e where e.manualEncumbrance.id=:encumId and e.accountingCombination.id=:accComId ");
        lnexistQry.setNamedParameter("encumId", encumbrancenewObj.getId());
        lnexistQry.setNamedParameter("accComId", ln.getAccountingCombination().getId());

        lnexistQry.setMaxResult(1);
        // if exists update the amount, revision amount ,applied amount
        if (lnexistQry.list().size() > 0) {
          manualline = lnexistQry.list().get(0);
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
          if (ln.getAccountingCombination() != null) {
            manualline.setUniquecode(ln.getAccountingCombination().getEfinUniqueCode());
            manualline.setSalesRegion(ln.getAccountingCombination().getSalesRegion());
            manualline.setAccountElement(ln.getAccountingCombination().getAccount());
            manualline.setSalesCampaign(ln.getAccountingCombination().getSalesCampaign());
            manualline.setProject(ln.getAccountingCombination().getProject());
            manualline.setActivity(ln.getAccountingCombination().getActivity());
            manualline.setStDimension(ln.getAccountingCombination().getStDimension());
            manualline.setNdDimension(ln.getAccountingCombination().getNdDimension());
            manualline.setAccountingCombination(ln.getAccountingCombination());
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

          // update encumbranceline in bid management
          updateEncumbranceLineInBid(bidmgmt, manualline, ln.getAccountingCombination());
        }
      }
    } catch (Exception e) {
      log.error("Exception in insertEncumbranceLines " + e.getMessage());
    }
    return manualline;
  }

  /**
   * Method to insert the Encumbrance Modification
   * 
   * @param encumbranceline
   * @param decamount
   * @param srcrefline
   * @param TransactionType
   * @param bidln
   * @param prosalln
   * @return
   */
  public static EfinBudManencumRev insertEncumbranceModification(
      EfinBudgetManencumlines encumbranceline, BigDecimal decamount,
      EfinBudgetManencumlines srcrefline, String TransactionType, Escmbidmgmtline bidln,
      EscmProposalmgmtLine prosalln) {
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
      manEncumRev.setSystem(true);
      if (TransactionType.equals("BID")) {
        manEncumRev.setEncumbranceType("BE");
        OBQuery<RequisitionLine> ln = OBDal.getInstance().createQuery(RequisitionLine.class,
            " as e where e.requisition.id in ( select e.id from ProcurementRequisition e "
                + " where e.efinBudgetManencum.id=:encumId ) and e.efinCValidcombination.id=:combId "
                + "and escmIssummary='N' ");
        ln.setNamedParameter("encumId", encumbranceline.getManualEncumbrance().getId());
        ln.setNamedParameter("combId", encumbranceline.getAccountingCombination().getId());
        ln.setMaxResult(1);
        if (ln.list().size() > 0) {
          manEncumRev.setRequisitionLine(ln.list().get(0));
        }

        manEncumRev.setEscmBidmgmtLine(bidln);

      } else if (TransactionType.equals("PRO")) {
        manEncumRev.setEncumbranceType("PAE");

        // manEncumRev.setEscmBidmgmtLine(prosalln.getEscmBidmgmtLine());
        // manEncumRev.setEscmProposalmgmtLine(prosalln);
      } else if (TransactionType.equals("BIDCAN")) {
        manEncumRev.setEncumbranceType("BE");
      }
      log.debug("req:" + manEncumRev.getRequisitionLine());
      OBDal.getInstance().save(manEncumRev);

    } catch (Exception e) {
      log.error("Exception in insertEncumbranceModification " + e.getMessage());
    }
    return manEncumRev;
  }

  public static boolean chkFundsAvailforNewEncumbrance(EscmBidMgmt bidmgmt,
      EfinBudgetManencum encumbrance, Order objOrder, EscmProposalMgmt proposal,
      boolean isStageMove) throws ParseException {
    boolean errorFlag = false;
    String message = null;
    List<AccountingCombination> acctcomlist = new ArrayList<AccountingCombination>();
    JSONObject result = new JSONObject(), result1 = null, json = null, json1 = null;
    OBQuery<EfinBudgetInquiry> budInq = null;
    Escmbidmgmtline line = null;
    OrderLine poLine = null;
    try {
      // get PR detail based on associated Bid source ref .
      if (bidmgmt != null) {
        result = getPRDetailsBasedOnBRQty(bidmgmt);
      } else if (proposal != null) {
        // result = ProposalManagementActionMethod.getPRDetailsBasedOnProposalQty1(proposal);
        log.debug("result:" + result);
        result = ProposalManagementActionMethod.getPRDetailsBasedOnProposalQty1(proposal, null);
        log.debug("result:" + result);
      } else {
        result = POContractSummaryDAO.getPRDetailsBasedOnOrdQty1(objOrder);
      }
      log.debug("result:" + result);
      if (result != null && result.getJSONObject("uniquecodeListarray") != null) {

        result1 = result.getJSONObject("uniquecodeListarray");
        JSONArray array = result1.getJSONArray("uniquecodeList");
        for (int i = 0; i < array.length(); i++) {
          json = array.getJSONObject(i);
          AccountingCombination acctcom = OBDal.getInstance().get(AccountingCombination.class,
              json.getString("Uniquecode"));
          if (json != null && json.getJSONArray("lineList") != null) {
            JSONArray Uncodearray = json.getJSONArray("lineList");
            for (int j = 0; j < Uncodearray.length(); j++) {
              json1 = Uncodearray.getJSONObject(j);
              if (bidmgmt != null) {
                line = OBDal.getInstance().get(Escmbidmgmtline.class, json1.getString("lineId"));
              } else {
                poLine = OBDal.getInstance().get(OrderLine.class, json1.getString("lineId"));
              }

              if (encumbrance.getEncumMethod().equals("M") && isStageMove) {
                errorFlag = true;
                message = OBMessageUtils.messageBD("EFIN_PropNewUniqNotAllow");
                line.setFailureReason(message);
                OBDal.getInstance().save(line);
                continue;
              }

              budInq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
                  "efinBudgetint.id=:budgetInitId and accountingCombination.id=:accId ");
              budInq.setNamedParameter("budgetInitId",
                  encumbrance.getBudgetInitialization().getId());
              budInq.setNamedParameter("accId", acctcom.getId());
              log.debug("budInq123:" + budInq.list().size());
              // if isdepartment fund yes, then check dept level distribution acct.
              if (acctcom.isEFINDepartmentFund()) {
                if (budInq.list() != null && budInq.list().size() > 0) {
                  for (EfinBudgetInquiry Enquiry : budInq.list()) {
                    if (acctcom.getId().equals(Enquiry.getAccountingCombination().getId())) {
                      if (new BigDecimal(json.getString("Amount"))
                          .compareTo(Enquiry.getFundsAvailable()) > 0) {
                        // funds not available
                        errorFlag = true;
                        message = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Cost");
                        message = message.replace("@", Enquiry.getFundsAvailable().toString());
                        if (bidmgmt != null) {
                          line.setFailureReason(message);
                          OBDal.getInstance().save(line);
                        } else {
                          poLine.setEfinFailureReason(message);
                          OBDal.getInstance().save(poLine);
                        }
                      }
                    }
                  }
                } else {
                  errorFlag = true;
                  message = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Cost");
                  message = message.replace("@", "0");
                  if (bidmgmt != null) {
                    line.setFailureReason(message);
                    OBDal.getInstance().save(line);
                  } else {
                    poLine.setEfinFailureReason(message);
                    OBDal.getInstance().save(poLine);
                  }
                }
              }
              // if isdepartment fund No, then check Org level distribution acct.
              else {
                if (bidmgmt != null) {
                  acctcomlist = CommonValidationsDAO.getParentAccountCom(
                      line.getAccountingCombination(), line.getClient().getId());
                } else {
                  acctcomlist = CommonValidationsDAO.getParentAccountCom(poLine.getEFINUniqueCode(),
                      poLine.getClient().getId());
                }

                if (acctcomlist != null && acctcomlist.size() > 0) {
                  AccountingCombination combination = acctcomlist.get(0);

                  budInq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
                      "efinBudgetint.id=:budgetInitId and accountingCombination.id=:accId");
                  budInq.setNamedParameter("budgetInitId",
                      encumbrance.getBudgetInitialization().getId());
                  budInq.setNamedParameter("accId", combination.getId());
                  if (budInq.list() != null && budInq.list().size() > 0) {
                    for (EfinBudgetInquiry Enquiry : budInq.list()) {
                      if (combination.getId().equals(Enquiry.getAccountingCombination().getId())) {
                        if (new BigDecimal(json.getString("Amount"))
                            .compareTo(Enquiry.getFundsAvailable()) > 0) {
                          // funds not available
                          errorFlag = true;
                          message = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Cost");
                          message = message.replace("@", Enquiry.getFundsAvailable().toString());
                          if (bidmgmt != null) {
                            line.setFailureReason(message);
                            OBDal.getInstance().save(line);
                          } else {
                            poLine.setEfinFailureReason(message);
                            OBDal.getInstance().save(poLine);
                          }
                        }
                      }
                    }
                  } else {
                    errorFlag = true;
                    message = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Cost");
                    message = message.replace("@", "0");
                    if (bidmgmt != null) {
                      line.setFailureReason(message);
                      OBDal.getInstance().save(line);
                    } else {
                      poLine.setEfinFailureReason(message);
                      OBDal.getInstance().save(poLine);
                    }
                  }
                } else {
                  errorFlag = true;
                  message = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Cost");
                  message = message.replace("@", "0");
                  if (bidmgmt != null) {
                    line.setFailureReason(message);
                    OBDal.getInstance().save(line);
                  } else {
                    poLine.setEfinFailureReason(message);
                    OBDal.getInstance().save(poLine);
                  }
                }
              }
              if (!errorFlag) {
                if (bidmgmt != null) {
                  line.setFailureReason(message);
                  OBDal.getInstance().save(line);
                } else {
                  poLine.setEfinFailureReason(message);
                  OBDal.getInstance().save(poLine);
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in chkFundsAvailforNewEncumbrance " + e.getMessage());
    }
    return errorFlag;
  }

  /**
   * get PR detail based on Bid Source ref
   * 
   * @param bidmgmt
   * @return JSONObject of Sourceref details
   */
  @SuppressWarnings("rawtypes")
  public static JSONObject getPRDetailsBasedOnBRQty(EscmBidMgmt bidmgmt) {
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
          + " else 0 end  as amount ,ln.escm_bidmgmt_line_id ,ln.c_validcombination_id ,ln.issummarylevel, "
          + " case when reqln.em_efin_c_validcombination_id<>ln.c_validcombination_id  then  true else false end as isuniquecodechange "
          + " from  escm_bidsourceref ref join escm_bidmgmt_line ln on ln.escm_bidmgmt_line_id= ref.escm_bidmgmt_line_id"
          + "  join m_requisitionline reqln on reqln.m_requisitionline_id= ref.m_requisitionline_id "
          + " join m_requisition req on req.m_requisition_id= reqln.m_requisition_id and req.em_efin_budget_manencum_id is not null"
          + " where ln.escm_bidmgmt_id = ?  and ln.issummarylevel  ='N' "
          + " group by ln.escm_bidmgmt_line_id ,req.em_efin_budget_manencum_id , reqln.em_efin_c_validcombination_id ,ln.c_validcombination_id  order by ln.escm_bidmgmt_line_id ";
      query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      query.setParameter(0, bidmgmt.getId());
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
            json.put("isuniquecodechange", row[6].toString());

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
            json.put("isuniquecodechange", row[6].toString());
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

  /**
   * reactivate split PR
   * 
   * @param resultEncum
   * @param bidmgmt
   */
  public static void reactivateSplitPR(JSONObject resultEncum, EscmBidMgmt bidmgmt,
      String bidStatus) {
    List<EfinBudgetManencumlines> manenculine = new ArrayList<EfinBudgetManencumlines>();
    EfinBudgetManencum encumbrance = null;
    EfinBudgetManencumlines srcEnumLines = null;
    List<EfinBudManencumRev> revlist = null;
    try {

      encumbrance = bidmgmt.getEncumbrance();

      if (bidStatus == null || (bidStatus != null && !bidStatus.equals("CL"))) {
        // update encumbrance line is null
        for (Escmbidmgmtline ln : bidmgmt.getEscmBidmgmtLineList()) {
          ln.setEfinBudgmanencumline(null);
          OBDal.getInstance().save(ln);
        }
        // update the bid management header status as "In Active"
        // bidmgmt.setEscmDocaction("CO");
        // bidmgmt.setBidstatus("IA");
        // bidmgmt.setBidappstatus("DR");
        bidmgmt.setEncumbrance(null);
        bidmgmt.setEfinIsbudgetcntlapp(false);
        OBDal.getInstance().save(bidmgmt);
      }

      // reactivate split the PR
      if (encumbrance != null) {
        // fetching revision record based on newly created encumbrance lines
        OBQuery<EfinBudManencumRev> revQuery = OBDal.getInstance()
            .createQuery(EfinBudManencumRev.class, " as e where e.sRCManencumline.id in "
                + "( select e.id from Efin_Budget_Manencumlines e where e.manualEncumbrance.id=:encumId)");
        revQuery.setNamedParameter("encumId", encumbrance.getId());
        revlist = revQuery.list();
        if (revlist.size() > 0) {
          for (EfinBudManencumRev rev : revlist) {
            srcEnumLines = rev.getSRCManencumline();
            if (bidStatus == null || (bidStatus != null && !bidStatus.equals("CL"))) {
              rev.setSRCManencumline(null);
              OBDal.getInstance().save(rev);
              OBDal.getInstance().flush();
            }
          }

          if (encumbrance != null)
            manenculine = encumbrance.getEfinBudgetManencumlinesList();
          if (manenculine.size() > 0) {
            for (EfinBudgetManencumlines line : manenculine) {
              if (bidStatus == null || (bidStatus != null && !bidStatus.equals("CL"))) {
                encumbrance.setDocumentStatus("DR");
                OBDal.getInstance().remove(line);
              } else {
                line.setAPPAmt(line.getAPPAmt().add(line.getRevamount().negate()));
                BidManagementDAO.insertEncumbranceModification(line, line.getRevamount().negate(),
                    null, "BIDCAN", null, null);
              }
            }
            if (bidStatus == null || (bidStatus != null && !bidStatus.equals("CL"))) {
              OBDal.getInstance().remove(encumbrance);
            }
          }
          for (EfinBudManencumRev rev : revlist) {
            EfinBudgetManencumlines lines = rev.getManualEncumbranceLines();
            log.debug("getAccountingCombination:" + lines.getAccountingCombination());
            // revert the old encumbrance updation
            lines.setAPPAmt(lines.getAPPAmt().add(rev.getRevamount().negate()));
            if (bidStatus == null || (bidStatus != null && !bidStatus.equals("CL")))
              lines.getEfinBudManencumRevList().remove(rev);
            else {
              BidManagementDAO.insertEncumbranceModification(lines, rev.getRevamount().negate(),
                  srcEnumLines, "BIDCAN", null, null);
            }
          }
        }

      }
    } catch (Exception e) {
      log.error("Exception in reactivateSplitPR " + e.getMessage());
    }
  }

  public static void reactivateStageUniqueCodeChg(JSONObject resultEncum, EscmBidMgmt bidmgmt,
      String bidStatus) {
    List<EfinBudManencumRev> revlist = new ArrayList<EfinBudManencumRev>();
    EfinBudgetManencumlines newEncLine = null;

    try {
      // reactivate merge the PR
      if (bidmgmt.getEncumbrance() != null) {
        // fetching revision record based on newly created encumbrance lines
        OBQuery<EfinBudManencumRev> revQuery = OBDal.getInstance()
            .createQuery(EfinBudManencumRev.class, " as e where e.sRCManencumline.id in "
                + "( select e.id from Efin_Budget_Manencumlines e where e.manualEncumbrance.id=:encumId)");
        revQuery.setNamedParameter("encumId", bidmgmt.getEncumbrance().getId());

        revlist = revQuery.list();
        if (revlist.size() > 0) {
          bidmgmt.getEncumbrance().setDocumentStatus("DR");
          for (EfinBudManencumRev rev : revlist) {
            EfinBudgetManencumlines lines = rev.getManualEncumbranceLines();
            // revert the old encumbrance updation
            lines.getEfinBudManencumRevList().remove(rev);
            lines.setAPPAmt(lines.getAPPAmt().add(rev.getRevamount().negate()));
            newEncLine = rev.getSRCManencumline();
            if (bidmgmt.getEncumbrance() != null
                && bidmgmt.getEncumbrance().getEfinBudgetManencumlinesList().size() > 0) {
              bidmgmt.getEncumbrance().getEfinBudgetManencumlinesList().remove(newEncLine);
            }
            OBDal.getInstance().remove(newEncLine);

          }
          bidmgmt.getEncumbrance().setDocumentStatus("CO");
        }

        // update encumbrance line is null
        // if (bidStatus == null || (bidStatus != null && !bidStatus.equals("CL"))) {
        for (Escmbidmgmtline ln : bidmgmt.getEscmBidmgmtLineList()) {
          ln.setEfinBudgmanencumline(null);
          OBDal.getInstance().save(ln);
        }

        // update the bid management header status as "In Active"
        bidmgmt.setEncumbrance(null);
        bidmgmt.setEfinIsbudgetcntlapp(false);
        OBDal.getInstance().save(bidmgmt);
        // }
      }
    } catch (Exception e) {
      log.error("Exception in reactivateMergePR " + e.getMessage());
    }
  }

  /**
   * 
   * @param bidmgmt
   * @return
   * @throws ParseException
   */
  public static boolean chkFundsAvailforReactOldEncumbrance(EscmBidMgmt bidmgmt,
      EscmProposalMgmt proposal, EscmProposalmgmtLine proposalmgmtline) throws ParseException {
    boolean errorFlag = false;
    String message = "", Uniquecode = "", tempUniquecode = null;
    List<AccountingCombination> acctcomlist = new ArrayList<AccountingCombination>();
    OBQuery<EfinBudgetInquiry> budInq = null;
    List<String> uniquecodeList = new ArrayList<String>();

    JSONObject uniquecodeResult = new JSONObject(), json = null, UniqueCodejson = null,
        json2 = null;
    JSONArray uniqueCodeListArray = new JSONArray();
    Boolean sameUniqueCode = false;
    EfinBudgetManencum encumbrance = null;
    Escmbidmgmtline bidlines = null;
    EscmProposalmgmtLine prosalline = null;
    try {
      if (bidmgmt != null) {
        bidlines = bidmgmt.getEscmBidmgmtLineList().get(0);
        encumbrance = bidmgmt.getEncumbrance();
      } else if (proposal != null) {
        prosalline = proposal.getEscmProposalmgmtLineList().get(0);
        encumbrance = proposal.getEfinEncumbrance();
      } else if (proposalmgmtline != null) {
        prosalline = proposalmgmtline;
        encumbrance = prosalline.getEscmProposalmgmt().getEfinEncumbrance();
      }
      message = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Cost");
      Uniquecode = "";
      if (encumbrance != null) {
        OBQuery<EfinBudManencumRev> revQuery = OBDal.getInstance().createQuery(
            EfinBudManencumRev.class,
            " as e where e.sRCManencumline.id in ( select e.id from Efin_Budget_Manencumlines e"
                + " where e.manualEncumbrance.id=:encumID)");
        revQuery.setNamedParameter("encumID", encumbrance.getId());
        if (revQuery.list().size() > 0) {
          for (EfinBudManencumRev rev : revQuery.list()) {
            EfinBudgetManencumlines lines = rev.getManualEncumbranceLines();
            EfinBudgetManencumlines newLine = rev.getSRCManencumline();
            BigDecimal diff = BigDecimal.ZERO;
            BigDecimal amount = BigDecimal.ZERO;
            diff = newLine.getAmount().subtract(rev.getRevamount().negate());
            if (newLine.getAccountingCombination().getId()
                .equals(lines.getAccountingCombination().getId())) {
              if (diff.compareTo(BigDecimal.ZERO) < 0) {
                amount = diff;
              }
            } else {
              amount = rev.getRevamount();
            }
            if (!(newLine.getAccountingCombination().getId()
                .equals(lines.getAccountingCombination().getId()))
                || amount.compareTo(BigDecimal.ZERO) < 0) {
              if (UniqueCodejson != null && UniqueCodejson.has("Uniquecode")) {
                for (int i = 0; i < uniqueCodeListArray.length(); i++) {
                  json2 = uniqueCodeListArray.getJSONObject(i);
                  if (json2.getString("Uniquecode")
                      .equals(lines.getAccountingCombination().getId())) {
                    json2.put("Amount",
                        new BigDecimal(json2.getString("Amount")).add(rev.getRevamount().negate()));
                    sameUniqueCode = true;
                    break;
                  } else
                    continue;
                }
              }
              if (!sameUniqueCode) {
                UniqueCodejson = new JSONObject();
                UniqueCodejson.put("Uniquecode", lines.getAccountingCombination().getId());
                UniqueCodejson.put("Amount", rev.getRevamount().negate());
                uniqueCodeListArray.put(UniqueCodejson);
              }
            }
          }
          uniquecodeResult.put("uniquecodeList", uniqueCodeListArray);
        }
        if (uniquecodeResult != null && uniquecodeResult.has("uniquecodeList")) {
          JSONArray array = uniquecodeResult.getJSONArray("uniquecodeList");
          for (int i = 0; i < array.length(); i++) {
            json = array.getJSONObject(i);
            AccountingCombination acctcom = OBDal.getInstance().get(AccountingCombination.class,
                json.getString("Uniquecode"));
            if (acctcom != null) {
              budInq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
                  " efinBudgetint.id=:budgetInitId and accountingCombination.id=:acctcomId ");
              budInq.setNamedParameter("budgetInitId",
                  encumbrance.getBudgetInitialization().getId());
              budInq.setNamedParameter("acctcomId", acctcom.getId());

              log.debug("budInq:" + budInq.list().size());
              // if isdepartment fund yes, then check dept level distribution acct.
              if (acctcom.isEFINDepartmentFund()) {
                if (budInq.list() != null && budInq.list().size() > 0) {
                  for (EfinBudgetInquiry Enquiry : budInq.list()) {
                    if (acctcom.getId().equals(Enquiry.getAccountingCombination().getId())) {
                      log.debug("getFundsAvailable:" + Enquiry.getFundsAvailable());
                      if (new BigDecimal(json.getString("Amount"))
                          .compareTo(Enquiry.getFundsAvailable()) > 0) {
                        // funds not available
                        errorFlag = true;
                        if (!uniquecodeList.contains(acctcom.getEfinUniqueCode())) {
                          Uniquecode = acctcom.getEfinUniqueCode();
                          uniquecodeList.add(Uniquecode);
                        }
                      }
                    }
                  }
                } else {
                  errorFlag = true;
                  message = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Cost");
                  message = message.replace("@", "0");
                  if (!uniquecodeList.contains(acctcom.getEfinUniqueCode())) {
                    Uniquecode = acctcom.getEfinUniqueCode();
                    uniquecodeList.add(Uniquecode);
                  }

                }
              }
              // if isdepartment fund No, then check Org level distribution acct.
              else {
                acctcomlist = CommonValidationsDAO.getParentAccountCom(acctcom,
                    acctcom.getClient().getId());

                if (acctcomlist != null && acctcomlist.size() > 0) {
                  AccountingCombination combination = acctcomlist.get(0);

                  budInq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
                      "efinBudgetint.id=:budgetInitId and accountingCombination.id=:acctcomId ");
                  budInq.setNamedParameter("budgetInitId",
                      encumbrance.getBudgetInitialization().getId());
                  budInq.setNamedParameter("acctcomId", combination.getId());

                  if (budInq.list() != null && budInq.list().size() > 0) {
                    for (EfinBudgetInquiry Enquiry : budInq.list()) {
                      if (combination.getId().equals(Enquiry.getAccountingCombination().getId())) {
                        log.debug("getFundsAvailable:" + Enquiry.getFundsAvailable());
                        if (new BigDecimal(json.getString("Amount"))
                            .compareTo(Enquiry.getFundsAvailable()) > 0) {
                          // funds not available
                          errorFlag = true;
                          if (!uniquecodeList.contains(acctcom.getEfinUniqueCode())) {
                            Uniquecode = acctcom.getEfinUniqueCode();
                            uniquecodeList.add(Uniquecode);
                          }
                        }
                      }
                    }
                  }
                } else {
                  errorFlag = true;
                  if (!uniquecodeList.contains(acctcom.getEfinUniqueCode())) {
                    Uniquecode = acctcom.getEfinUniqueCode();
                    uniquecodeList.add(Uniquecode);
                  }
                }
              }
            }
          }
        }
        if (!errorFlag) {
          if (bidmgmt != null) {
            bidlines.setFailureReason(null);
            OBDal.getInstance().save(bidlines);
          } else if (proposal != null) {
            prosalline.setEfinFailureReason(null);
            OBDal.getInstance().save(prosalline);
          }
        }
        if (errorFlag) {
          if (uniquecodeList != null) {
            Iterator<String> iterator = uniquecodeList.iterator();
            while (iterator.hasNext()) {
              if (tempUniquecode != null)
                tempUniquecode = tempUniquecode + "," + iterator.next();

              else
                tempUniquecode = iterator.next();
            }
          }
          message = message.replace("@", tempUniquecode);
          if (bidmgmt != null) {
            bidlines.setFailureReason(message);
            OBDal.getInstance().save(bidlines);
          } else if (proposal != null) {
            prosalline.setEfinFailureReason(message);
            OBDal.getInstance().save(prosalline);
          }
        }
      }

    } catch (Exception e) {
      log.error("Exception in chkFundsAvailforReactOldEncumbrance " + e.getMessage());
    }
    return errorFlag;
  }

  public static void updateEncumbranceLineInBid(EscmBidMgmt bidmgmt,
      EfinBudgetManencumlines encline, AccountingCombination com) {
    List<Escmbidmgmtline> ln = new ArrayList<Escmbidmgmtline>();
    try {

      OBQuery<Escmbidmgmtline> bidlineQry = OBDal.getInstance().createQuery(Escmbidmgmtline.class,
          " as e where e.accountingCombination.id=:acctcomId "
              + "and e.escmBidmgmt.id=:bidId and e.issummarylevel='N'");
      bidlineQry.setNamedParameter("bidId", bidmgmt.getId());
      bidlineQry.setNamedParameter("acctcomId", com.getId());

      log.debug("bidlineQry " + bidlineQry.list().size());
      log.debug("bidlineQry " + bidlineQry.getWhereAndOrderBy());
      if (bidlineQry.list().size() > 0) {
        ln = bidlineQry.list();
        for (Escmbidmgmtline bidln : ln) {
          bidln.setEfinBudgmanencumline(encline);
          OBDal.getInstance().save(bidln);
        }
      }
    } catch (Exception e) {
      log.error("Exception in updateEncumbranceLineInBid " + e.getMessage());
    }
  }

  /*
   * Check whether Bid Configuration is done for bid type**
   * 
   * @param orgId
   * 
   * @param bidType
   * 
   * @return count in int
   */

  public static int getBidConfigCount(String orgId, String bidType) {
    int configSize = 0;
    try {
      OBContext.setAdminMode();
      OBQuery<Escmbidconfiguration> configuration = OBDal.getInstance().createQuery(
          Escmbidconfiguration.class,
          " as e where e.organization.id=:orgID and e.bidType =:bidtype ");
      configuration.setNamedParameter("orgID", orgId);
      configuration.setNamedParameter("bidtype", bidType);
      if (configuration.list().size() > 0) {
        configSize = configuration.list().size();
      }
    } catch (OBException e) {
      log.error("Exception while getBidConfigCount:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return configSize;
  }

  public static boolean StageMoveUniquecodeChanges(EscmBidMgmt bidmgmt,
      EfinBudgetManencum encumbrance) {
    boolean errorFlag = false;
    JSONObject prResult = null, result1 = null, prlistresult = null, json = null, jsonencum = null;
    BigDecimal totalAmount = BigDecimal.ZERO, Amount = BigDecimal.ZERO;
    EfinBudgetManencumlines manualline = null;
    Boolean isUniquecodeChanges = false;
    Long lineno = (long) ((encumbrance.getEfinBudgetManencumlinesList().size() * 10) + 10);
    try {
      prResult = getPRDetailsBasedOnBRQty(bidmgmt);
      if (encumbrance.getEncumMethod().equals("A")) {

        if (prResult != null && prResult.getJSONObject("uniquecodeListarray") != null) {
          result1 = prResult.getJSONObject("uniquecodeListarray");
          JSONArray uniquecodearray = result1.getJSONArray("uniquecodeList");

          if (prResult != null && prResult.getJSONObject("prListarray") != null
              && uniquecodearray.length() > 0) {
            prlistresult = prResult.getJSONObject("prListarray");
            JSONArray array = prlistresult.getJSONArray("list");
            for (int i = 0; i < array.length(); i++) {
              json = array.getJSONObject(i);
              Escmbidmgmtline ln = OBDal.getInstance().get(Escmbidmgmtline.class,
                  json.getString("bidlineId"));
              totalAmount = new BigDecimal(json.getString("totalamount"));
              isUniquecodeChanges = json.getBoolean("isuniquecodechange");

              // check already unqiuecode exists or not
              if (isUniquecodeChanges) {
                OBQuery<EfinBudgetManencumlines> lnexistQry = OBDal.getInstance().createQuery(
                    EfinBudgetManencumlines.class, " as e where e.manualEncumbrance.id=:encumId"
                        + " and e.accountingCombination.id=:acctcomId  and e.isauto='Y' ");
                lnexistQry.setNamedParameter("encumId", encumbrance.getId());
                lnexistQry.setNamedParameter("acctcomId", ln.getAccountingCombination().getId());
                lnexistQry.setMaxResult(1);
                // if exists update the amount, revision amount ,applied amount
                if (lnexistQry.list().size() == 0) {

                  manualline = OBProvider.getInstance().get(EfinBudgetManencumlines.class);
                  manualline.setClient(encumbrance.getClient());
                  manualline.setOrganization(encumbrance.getOrganization());
                  manualline.setUpdatedBy(encumbrance.getCreatedBy());
                  manualline.setCreationDate(new java.util.Date());
                  manualline.setCreatedBy(encumbrance.getCreatedBy());
                  manualline.setUpdated(new java.util.Date());
                  manualline.setLineNo(lineno);
                  if (ln.getAccountingCombination() != null) {
                    manualline.setUniquecode(ln.getAccountingCombination().getEfinUniqueCode());
                    manualline.setSalesRegion(ln.getAccountingCombination().getSalesRegion());
                    manualline.setAccountElement(ln.getAccountingCombination().getAccount());
                    manualline.setSalesCampaign(ln.getAccountingCombination().getSalesCampaign());
                    manualline.setProject(ln.getAccountingCombination().getProject());
                    manualline.setActivity(ln.getAccountingCombination().getActivity());
                    manualline.setStDimension(ln.getAccountingCombination().getStDimension());
                    manualline.setNdDimension(ln.getAccountingCombination().getNdDimension());
                    manualline.setAccountingCombination(ln.getAccountingCombination());
                  }

                  manualline.setBudgetLines(null);
                  manualline.setManualEncumbrance(encumbrance);
                  manualline.setAmount(totalAmount);
                  manualline.setRevamount(totalAmount);
                  manualline.setRemainingAmount(BigDecimal.ZERO);
                  manualline.setOriginalamount(totalAmount);
                  manualline.setAPPAmt(totalAmount);
                  manualline.setUsedAmount(BigDecimal.ZERO);
                  manualline.setAuto(true);
                  lineno += 10;
                  OBDal.getInstance().save(manualline);
                  OBDal.getInstance().flush();

                  lineno = lineno + 10;
                } else {
                  manualline = lnexistQry.list().get(0);
                  manualline.setAmount(manualline.getAmount().add(totalAmount));
                  manualline.setRevamount(manualline.getRevamount().add(totalAmount));
                  manualline.setRemainingAmount(BigDecimal.ZERO);
                  manualline.setOriginalamount(manualline.getOriginalamount().add(totalAmount));
                  manualline.setAPPAmt(manualline.getAPPAmt().add(totalAmount));
                  OBDal.getInstance().save(manualline);
                }

                JSONArray encumarray = json.getJSONArray("encList");
                for (int j = 0; j < encumarray.length(); j++) {
                  jsonencum = encumarray.getJSONObject(j);
                  if (jsonencum.getString("encumId") != null
                      && jsonencum.getString("validcomId") != null) {
                    // get old encumbrance line
                    OBQuery<EfinBudgetManencumlines> lines = OBDal.getInstance().createQuery(
                        EfinBudgetManencumlines.class,
                        " as e where e.manualEncumbrance.id=:encumId and e.accountingCombination.id=:acctcomId");
                    lines.setNamedParameter("encumId", jsonencum.getString("encumId"));
                    lines.setNamedParameter("acctcomId", jsonencum.getString("validcomId"));
                    lines.setMaxResult(1);
                    if (lines.list().size() > 0) {
                      // decrease the rev amount and remaining amount
                      Amount = new BigDecimal(jsonencum.getString("encamount"));
                      log.debug("amount1:" + Amount);

                      EfinBudgetManencumlines encumline = lines.list().get(0);
                      // insert the Encumbrance revision entry(-ve value)
                      insertEncumbranceModification(encumline, Amount.negate(), manualline, "BID",
                          ln, null);
                      encumline.setAPPAmt(encumline.getAPPAmt().subtract(Amount));
                      OBDal.getInstance().save(encumline);
                    }
                  }
                }
              }
            }
          }
        }
      }

    } catch (Exception e) {
    }
    return errorFlag;
  }

  /**
   * Check budget amount is b/w min and max from bid config
   * 
   * @param orgId
   * @param bidType
   * @param appBud
   * @return count in int
   */
  public static int getBidConfigAppBudCount(String orgId, String bidType, BigDecimal appBud) {
    int configAppBudSize = 0;
    try {
      OBContext.setAdminMode();
      OBQuery<Escmbidconfiguration> bidconfig = OBDal.getInstance().createQuery(
          Escmbidconfiguration.class,
          " as e where e.organization.id=:orgId and e.bidType = :bidType and (e.appmaxvalue is null "
              + " and e.appminvalue is not null and :appBud >=e.appminvalue) "
              + " or (e.appminvalue  is  null and e.appmaxvalue is not null  and :appBud <=e.appmaxvalue) "
              + " or (e.appminvalue is not null and e.appmaxvalue is not null and :appBud >=e.appminvalue "
              + " and :appBud<=e.appmaxvalue)");
      bidconfig.setNamedParameter("orgId", orgId);
      bidconfig.setNamedParameter("bidType", bidType);
      bidconfig.setNamedParameter("appBud", appBud);
      log.debug("bidconfig:" + bidconfig.getWhereAndOrderBy());
      if (bidconfig.list().size() > 0) {
        configAppBudSize = bidconfig.list().size();
      }
    } catch (OBException e) {
      log.error("Exception while getBidConfigAppBudCount:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return configAppBudSize;
  }

  /*
   * Get sum of qty from source ref to check qty match
   * 
   * @param bidLineId
   * 
   * @return sum of qty in BigDecimal
   */
  public static BigDecimal getBidSourRefQty(String bidLineId) {
    BigDecimal configAppBudSize = BigDecimal.ZERO;
    StringBuffer query = null;
    Query bidSrfQuery = null;
    try {
      OBContext.setAdminMode();
      query = new StringBuffer();
      query.append(
          "SELECT coalesce(SUM(bidSrf.reservedQuantity), 0) as qty FROM escm_bidsourceref bidSrf where escmBidmgmtLine.id=:lineId ");
      bidSrfQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      bidSrfQuery.setParameter("lineId", bidLineId);
      log.debug(" Query : " + query.toString());
      if (bidSrfQuery != null) {
        if (bidSrfQuery.list().size() > 0) {
          if (bidSrfQuery.iterate().hasNext()) {
            String qty = bidSrfQuery.iterate().next().toString();// itr.next().toString();
            // String qty = objects[0] == null ? "0" : objects[0].toString();//obj[0].toString();
            configAppBudSize = new BigDecimal(qty);
          }
        }
      }
      /*
       * sql =
       * " select coalesce(sum(quantity),0) as quantity from  escm_bidsourceref where escm_bidmgmt_line_id='"
       * + line.getId() + "' ";
       */

    } catch (OBException e) {
      log.error("Exception while getBidSourRefQty:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return configAppBudSize;
  }

  /**
   * Check role has delegation on the particular date for bid document
   * 
   * @param currentDate
   * @param roleId
   * @return boolean
   */
  @SuppressWarnings("rawtypes")
  public static boolean getDelegationRole(Date currentDate, String roleId) {
    StringBuffer query = null;
    Query delRoleQuery = null;
    String roleeId = "";
    boolean allowDelegation = false;
    try {
      OBContext.setAdminMode();
      query = new StringBuffer();
      query.append(
          "select dll.role.id from Eut_Docapp_Delegateln dll left join dll.eUTDocappDelegate dl ");
      query.append(
          " where dl.fromDate<=:currentDate and dl.date >=:currentDate and dll.documentType='EUT_116' ");
      delRoleQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      delRoleQuery.setParameter("currentDate", currentDate);
      log.debug(" Query : " + query.toString());
      if (delRoleQuery != null) {
        if (delRoleQuery.list().size() > 0) {
          for (Iterator iterator = delRoleQuery.iterate(); iterator.hasNext();) {
            Object objects = iterator.next();
            roleeId = objects == null ? "" : objects.toString();
            if (roleeId.equals(roleId)) {
              allowDelegation = true;
              break;
            }
          }
        }
      }
      /*
       * sql =
       * "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
       * + currentDate + "' and to_date >='" + currentDate + "' and document_type='EUT_116'";
       */
    } catch (OBException e) {
      log.error("Exception while getDelegationRole:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return allowDelegation;
  }

  /**
   * Check alert rule present for bid to get alert recipients
   * 
   * @param clientId
   * @param alertWindow
   * @return alertRuleId in String
   */
  public static String getAlertRule(String clientId, String alertWindow) {
    String alertRuleId = "";
    try {
      OBContext.setAdminMode();
      // get alert rule id
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id=:clientID and e.eSCMProcessType=:alertwindow ");
      queryAlertRule.setNamedParameter("clientID", clientId);
      queryAlertRule.setNamedParameter("alertwindow", alertWindow);

      log.debug("queryAlertRule" + queryAlertRule.getWhereAndOrderBy());
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
    } catch (OBException e) {
      log.error("Exception while getAlertRule:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return alertRuleId;
  }

  /**
   * Get User to check delegation
   * 
   * @param roleId
   * @return userId in String
   */
  public static String getUserRole(String roleId) {
    String userId = "";
    try {
      OBContext.setAdminMode();
      OBQuery<UserRoles> userRole = OBDal.getInstance().createQuery(UserRoles.class,
          "role.id=:roleID ");
      userRole.setNamedParameter("roleID", roleId);
      userId = userRole.list().get(0).getUserContact().getId();
    } catch (OBException e) {
      log.error("Exception while getUserRole:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return userId;
  }

  /**
   * Get alert recipients to send alert
   * 
   * @param alertRuleId
   * @return recipients in list
   */
  public static List<AlertRecipient> getAlertReceipient(String alertRuleId) {
    try {
      OBContext.setAdminMode();
      OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
          .createQuery(AlertRecipient.class, "as e where e.alertRule.id=:alertRuleID ");
      receipientQuery.setNamedParameter("alertRuleID", alertRuleId);
      log.debug("ls size>" + receipientQuery.list().size());
      return receipientQuery.list();
    } catch (OBException e) {
      log.error("Exception while getAlertReceipient:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * check associated pr having mixed encumbrance - means one pr having encumbrance and another pr
   * does not have encumnrance
   * 
   * @param bid
   * @return
   */
  @SuppressWarnings("finally")
  public static boolean checkmixedPREncumbrance(EscmBidMgmt bid) {
    List<Escmbidmgmtline> bidLineList = new ArrayList<Escmbidmgmtline>();
    Boolean isskippedenc = false, isnotskippedenc = false, mixedencumbrance = false;
    try {
      OBContext.setAdminMode();
      bidLineList = bid.getEscmBidmgmtLineList();
      if (bidLineList.size() > 0) {
        for (Escmbidmgmtline lines : bidLineList) {
          if (!lines.isSummarylevel()) {
            for (Escmbidsourceref srcref : lines.getEscmBidsourcerefList()) {
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
                /*
                 * if(srcref.getRequisition().getEfinBudgetManencum()!=null) {
                 * encumlist.add(srcref.getRequisition().getEfinBudgetManencum()); }
                 */
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
   * To set new alert get other new alerts and solve
   * 
   * @param alertId
   */
  public static void getAlert(String alertId) {
    try {
      OBContext.setAdminMode();
      OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
          "as e where e.referenceSearchKey=:alertID and e.alertStatus='NEW'");
      alertQuery.setNamedParameter("alertID", alertId);
      if (alertQuery.list().size() > 0) {
        for (Alert objAlert : alertQuery.list()) {
          objAlert.setAlertStatus("SOLVED");
        }
      }
    } catch (OBException e) {
      log.error("Exception while getAlert:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static void updatePRQtyCancelBid(EscmBidMgmt bid) {
    try {

      if (bid != null) {
        for (Escmbidmgmtline ln : bid.getEscmBidmgmtLineList()) {
          if (!ln.isSummarylevel()) {
            for (Escmbidsourceref srcref : ln.getEscmBidsourcerefList()) {
              if (srcref.getRequisitionLine() != null) {
                RequisitionLine line = srcref.getRequisitionLine();
                line.setEscmBidmgmtQty(
                    line.getEscmBidmgmtQty().subtract(srcref.getReservedQuantity()));
                OBDal.getInstance().save(line);
              }
            }
          }
        }
      }
    } catch (Exception e) {

    }
  }

  /**
   * Get user name of delegated user to insert on approval history.
   * 
   * @param currentDate
   * @param roleId
   * @return userName in String
   */
  public static String getDelegationUser(Date currentDate, String roleId) {
    String userName = "";
    try {
      OBContext.setAdminMode();
      OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance().createQuery(
          EutDocappDelegateln.class,
          " as e left join e.eUTDocappDelegate as hd where hd.role.id =:roleId and hd.fromDate <=:currentDate "
              + " and hd.date >=:currentDate and e.documentType='EUT_116'");
      delegationln.setNamedParameter("roleId", roleId);
      delegationln.setNamedParameter("currentDate", currentDate);
      if (delegationln != null && delegationln.list().size() > 0) {
        userName = delegationln.list().get(0).getUserContact().getName();
      }
    } catch (OBException e) {
      log.error("Exception while getDelegationUser:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return userName;
  }

  /**
   * if bid is associate with manual encumbrance or auto encumbrance( with out bid) then check used
   * amount is greater or zero
   * 
   * @param bid
   * @return if greater the used amount then return false
   */
  public static boolean chkManualEncumbranceRejValid(EscmBidMgmt bid) {
    try {
      OBContext.setAdminMode();
      OBQuery<EfinBudgetManencumlines> encline = OBDal.getInstance().createQuery(
          EfinBudgetManencumlines.class,
          " as e " + " where e.manualEncumbrance.id=:encumId and e.id in "
              + " ( select b.efinBudgmanencumline.id from escm_bidmgmt_line b "
              + " where b.escmBidmgmt.id=:bidId) and e.usedAmount > 0 ");
      encline.setNamedParameter("encumId", bid.getEncumbrance().getId());
      encline.setNamedParameter("bidId", bid.getId());
      if (encline.list().size() > 0) {
        return true;
      } else
        return false;

    } catch (final Exception e) {
      log.error("Exception in chkManualEncumbranceValidation after Reject : ", e);
      return false;
    } finally {
    }
  }

  /**
   * Check there is next role to send for approval
   * 
   * @param RequestId
   * @param roleId
   * @return boolean
   */
  public static boolean isDirectApproval(String RequestId, String roleId) {
    boolean isDirectApp = false;
    StringBuffer query = null;
    Query bidnxtrlQuery = null;
    try {
      OBContext.setAdminMode();
      query = new StringBuffer();
      query.append(
          "select count(bid.id) as count from escm_bidmgmt bid join bid.eUTNextRole nxtrl join nxtrl.eutNextRoleLineList nxtrln ");
      query.append(" where bid.id=:bidId and nxtrln.role.id=:roleId");
      bidnxtrlQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      bidnxtrlQuery.setParameter("bidId", RequestId);
      bidnxtrlQuery.setParameter("roleId", roleId);
      log.debug(" Query : " + query.toString());
      if (bidnxtrlQuery != null) {
        if (bidnxtrlQuery.list().size() > 0) {
          if (bidnxtrlQuery.iterate().hasNext()) {
            String bidCount = bidnxtrlQuery.iterate().next().toString();
            int count = Integer.parseInt(bidCount);
            if (count > 0)
              isDirectApp = true;
            else
              isDirectApp = false;
          }
        }
      }
      /*
       * query = "select count(req.escm_bidmgmt_id) from escm_bidmgmt req join eut_next_role rl on "
       * + "req.eut_next_role_id = rl.eut_next_role_id " +
       * "join eut_next_role_line li on li.eut_next_role_id = rl.eut_next_role_id " +
       * "and req.escm_bidmgmt_id = ? and li.ad_role_id =?";
       */
    } catch (OBException e) {
      log.error("Exception while isDirectApproval:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return isDirectApp;
  }

  /**
   * Check whether the open envelope status is completed for the bid
   * 
   * @param bidMgmtId
   * @return
   */
  public static Boolean isOpenEnvelopeCompleted(String bidMgmtId) {
    Boolean isCompleted = false;

    String query = " as e where e.bidNo.id = ? and e.alertStatus = 'CO' ";
    List<Escmopenenvcommitee> escmOpenEnvCommiteeList = null;
    List<Object> parametersList = new ArrayList<Object>();
    parametersList.add(bidMgmtId);

    try {
      OBContext.setAdminMode();

      OBQuery<Escmopenenvcommitee> escmOpenEnvCommitee = OBDal.getInstance()
          .createQuery(Escmopenenvcommitee.class, query, parametersList);
      escmOpenEnvCommiteeList = escmOpenEnvCommitee.list();

      if (escmOpenEnvCommiteeList.size() > 0) {
        isCompleted = true;
      }

    } catch (OBException e) {
      log.error("Exception while checksameDocNowithSameBank:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

    return isCompleted;
  }

  /**
   * 
   * @param Escmbiddates
   * @return days
   */
  @SuppressWarnings("resource")
  public static void updateDaysLeft(String bidId, VariablesSecureApp vars, ProcessBundle bundle) {
    String days = "";
    StringBuffer query = null;
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      OBContext.setAdminMode();
      query = new StringBuffer();
      query.append(
          "select (proposallastday-cast(NOW() as date)) as daysleft from escm_biddates where escm_bidmgmt_id=? "
              + " and created=(select max(created) from escm_biddates where escm_bidmgmt_id=?)");
      st = OBDal.getInstance().getConnection().prepareStatement(query.toString());
      st.setString(1, bidId);
      st.setString(2, bidId);
      rs = st.executeQuery();
      if (rs.next()) {
        days = rs.getString("daysleft");
        st = OBDal.getInstance().getConnection()
            .prepareStatement("update escm_bidmgmt set daysleft=? where escm_bidmgmt_id=?");
        st.setString(1, days);
        st.setString(2, bidId);
        st.executeUpdate();
      }
    } catch (Exception e) {
      log.debug("exception while updateDaysLeft" + e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {
        log.error("Exception while closing the statement in updateDaysLeft() ", e);
      }
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Check Sales Voucher for the bid and validate
   * 
   * @param bidId
   * @return Boolean
   */
  public static Boolean getSalesVoucher(String bidId) {
    Boolean hasRecord = false;
    try {
      OBContext.setAdminMode();
      OBQuery<Escmsalesvoucher> salesVouch = OBDal.getInstance().createQuery(Escmsalesvoucher.class,
          "as e where escmBidmgmt.id=:bidID ");
      salesVouch.setNamedParameter("bidID", bidId);
      if (salesVouch != null && salesVouch.list().size() > 0) {
        hasRecord = true;
      }
    } catch (OBException e) {
      log.error("Exception while getSalesVoucher:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

    return hasRecord;
  }

  /**
   * Check Announcements for the bid and validate
   * 
   * @param bidId
   * @return Boolean
   */
  public static Boolean getAnnouncements(String bidId) {
    Boolean hasRecord = false;
    try {
      OBContext.setAdminMode();
      OBQuery<escmannoucementsv> annnouncement = OBDal.getInstance()
          .createQuery(escmannoucementsv.class, "as e where escmBidmgmt.id=:bidID ");
      annnouncement.setNamedParameter("bidID", bidId);

      if (annnouncement != null && annnouncement.list().size() > 0) {
        hasRecord = true;
      }
    } catch (OBException e) {
      log.error("Exception while getAnnouncements:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

    return hasRecord;
  }

  /**
   * Check bid history whether the document is already processed
   * 
   * @param bidId
   * @return Escmbidmgmthistory
   */
  public static Escmbidmgmthistory getBidHistory(String bidId) {
    Escmbidmgmthistory apphistory = null;
    try {
      OBContext.setAdminMode();
      OBQuery<Escmbidmgmthistory> history = OBDal.getInstance().createQuery(
          Escmbidmgmthistory.class,
          " as e where e.escmBidmgmt.id=:bidId order by e.creationDate desc ");
      history.setNamedParameter("bidId", bidId);
      history.setMaxResult(1);
      if (history.list().size() > 0) {
        apphistory = history.list().get(0);
      }
    } catch (OBException e) {
      log.error("Exception while getBidHistory:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return apphistory;
  }

  /**
   * Check alerts and set to solve
   * 
   * @param bidId
   */
  public static void getAlerts(String bidId) {
    try {
      OBContext.setAdminMode();
      OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
          "as e where e.referenceSearchKey=:searchKey and e.alertStatus='NEW'");
      alertQuery.setNamedParameter("searchKey", bidId);
      if (alertQuery.list().size() > 0) {
        for (Alert objAlert : alertQuery.list()) {
          objAlert.setAlertStatus("SOLVED");
          OBDal.getInstance().save(objAlert);
        }
      }
    } catch (OBException e) {
      log.error("Exception while getAlerts:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Get Bid Mgmt Line Count to check next role with current role
   * 
   * @param receiptId
   * @return line count in int
   */
  public static int getBidMgmtLineCount(String receiptId) {
    int count = 0;
    try {
      OBContext.setAdminMode();
      OBQuery<Escmbidmgmtline> lines = OBDal.getInstance().createQuery(Escmbidmgmtline.class,
          "escmBidmgmt.id =:bidId");
      lines.setNamedParameter("bidId", receiptId);
      count = lines.list().size();
    } catch (OBException e) {
      log.error("Exception while getBidMgmtLineCount:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  /**
   * Check line already presented or not based on product id
   * 
   * @param bidmgtId
   * @param productId
   * @return Escmbidmgmtline
   */
  public static Escmbidmgmtline getBidManagementLineOnProductId(String bidmgtId, String productId) {
    Escmbidmgmtline bidmgtline = null;
    try {
      OBContext.setAdminMode();
      OBQuery<Escmbidmgmtline> chklineexistQry = OBDal.getInstance().createQuery(
          Escmbidmgmtline.class,
          "as e where e.escmBidmgmt.id=:bidmgmtId and e.product.id=:prdId and e.manual=:manual and e.id not in (select a.parentline.id from escm_bidmgmt_line a where a.escmBidmgmt.id= :bidmgmtId and a.parentline!=null) ");
      chklineexistQry.setNamedParameter("bidmgmtId", bidmgtId);
      chklineexistQry.setNamedParameter("prdId", productId);
      chklineexistQry.setNamedParameter("manual", false);
      chklineexistQry.setMaxResult(1);
      List<Escmbidmgmtline> chklineexistQryList = chklineexistQry.list();
      if (chklineexistQryList != null && chklineexistQryList.size() > 0) {
        bidmgtline = chklineexistQryList.get(0);
      }
    } catch (OBException e) {
      log.error("Exception while getBidManagementLineOnProductId:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return bidmgtline;
  }

  /**
   * Check line already presented or not based on product NAME
   * 
   * @param bidmgtId
   * @param prodDesc
   * @return bidmgtline
   */
  public static Escmbidmgmtline getBidManagementLineOnProductName(String bidmgtId,
      String prodDesc) {
    Escmbidmgmtline bidmgtline = null;
    try {
      OBContext.setAdminMode();
      OBQuery<Escmbidmgmtline> chklinedescexistQry = OBDal.getInstance().createQuery(
          Escmbidmgmtline.class,
          "as e where e.escmBidmgmt.id=:bidID and e.description=:desc and e.manual=:manual and e.id not in (select a.parentline.id from escm_bidmgmt_line a where a.escmBidmgmt.id= :bidID and a.parentline!=null) ");
      chklinedescexistQry.setNamedParameter("bidID", bidmgtId);
      chklinedescexistQry.setNamedParameter("desc", prodDesc.replace("'", "''"));
      chklinedescexistQry.setNamedParameter("manual", false);
      chklinedescexistQry.setMaxResult(1);
      List<Escmbidmgmtline> chklinedescexistQryList = chklinedescexistQry.list();
      if (chklinedescexistQryList != null && chklinedescexistQryList.size() > 0) {
        bidmgtline = chklinedescexistQryList.get(0);
      }
    } catch (OBException e) {
      log.error("Exception while getBidManagementLineOnProductName:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return bidmgtline;
  }

  /**
   * Get Bid Source Ref to remove
   * 
   * @param bidmgtId
   * @param productId
   * @return bidmgtline
   */
  public static Escmbidmgmtline removeBidSourceRef(String bidmgtId, String productId) {
    Escmbidmgmtline bidmgtline = null;
    try {
      OBContext.setAdminMode();
      OBQuery<Escmbidmgmtline> chklineexistQry = OBDal.getInstance().createQuery(
          Escmbidmgmtline.class,
          "as e where e.escmBidmgmt.id=:bidId and e.product.id in (:productId)");
      chklineexistQry.setNamedParameter("bidId", bidmgtId);
      chklineexistQry.setNamedParameter("productId", productId);
      chklineexistQry.setMaxResult(1);
      List<Escmbidmgmtline> chklineexistQryList = chklineexistQry.list();
      if (chklineexistQryList != null && chklineexistQryList.size() > 0) {
        bidmgtline = chklineexistQryList.get(0);
      }
    } catch (OBException e) {
      log.error("Exception while getBidManagementLineOnProductId:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return bidmgtline;
  }

  /**
   * Get Bid Source ref line
   * 
   * @param bidMgmtLineId
   * @param requisitionLineId
   * @return recipients in list
   */
  public static List<Escmbidsourceref> getBidSourcRefLine(String bidMgmtLineId,
      String requisitionLineId) {
    try {
      OBContext.setAdminMode();
      OBQuery<Escmbidsourceref> srcrefline = OBDal.getInstance().createQuery(Escmbidsourceref.class,
          "as e where e.escmBidmgmtLine.id=:bidLineId and e.requisitionLine.id=:reqLineId ");
      srcrefline.setNamedParameter("bidLineId", bidMgmtLineId);
      srcrefline.setNamedParameter("reqLineId", requisitionLineId);
      srcrefline.setMaxResult(1);
      log.debug("ls size>" + srcrefline.list().size());
      return srcrefline.list();
    } catch (OBException e) {
      log.error("Exception while getBidSourcRefLine:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * check pr encumbrance type is enable or not
   * 
   * @param clientId
   * @return list
   */
  public static List<EfinEncControl> getPREncumTypeList(String clientId) {
    List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
    try {
      OBContext.setAdminMode();
      OBQuery<EfinEncControl> encumcontrol = OBDal.getInstance().createQuery(EfinEncControl.class,
          " as e where e.encumbranceType='PRE' and e.client.id=:clientID and e.active='Y' ");
      encumcontrol.setNamedParameter("clientID", clientId);
      encumcontrol.setFilterOnActive(true);
      encumcontrol.setMaxResult(1);
      enccontrollist = encumcontrol.list();

    } catch (OBException e) {
      log.error("Exception while getPREncumTypeList:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return enccontrollist;
  }

  /**
   * check proposal status
   * 
   * @param bidid
   * @return list
   */
  public static List<EscmProposalMgmt> checkProposalStatus(String bidId) {
    try {
      OBContext.setAdminMode();
      OBQuery<EscmProposalMgmt> proposal = OBDal.getInstance().createQuery(EscmProposalMgmt.class,
          " as e where e.escmBidmgmt.id=:bidID and e.proposalstatus not in ('DR','SUB','CL') ");
      proposal.setNamedParameter("bidID", bidId);
      proposal.setMaxResult(1);
      return proposal.list();
    } catch (OBException e) {
      log.error("Exception while checkProposalStatus:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * check proposal created
   * 
   * @param bidid
   * @return list
   */
  public static List<EscmProposalMgmt> checkProposalCreated(String bidId) {
    try {
      OBContext.setAdminMode();
      OBQuery<EscmProposalMgmt> proposal = OBDal.getInstance().createQuery(EscmProposalMgmt.class,
          " as e where e.escmBidmgmt.id=:bidID and e.proposalstatus!='CL')");
      proposal.setNamedParameter("bidID", bidId);
      proposal.setMaxResult(1);
      return proposal.list();
    } catch (OBException e) {
      log.error("Exception while checkProposalCreated:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get unique code from bid lines
   * 
   * @param bidid
   * @return list
   */
  public static List<?> getUniqueCode(String bidId) {
    try {
      OBContext.setAdminMode();
      SQLQuery Query = OBDal.getInstance().getSession().createSQLQuery(
          "select  COUNT(Distinct(c_validcombination_id)) as count,c_validcombination_id "
              + " from escm_bidmgmt_line where escm_bidmgmt_id=? group by c_validcombination_id");
      Query.setParameter(0, bidId);
      return Query.list();
    } catch (OBException e) {
      log.error("Exception while getUniqueCode:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get department from bid lines
   * 
   * @param bidid
   * @return list
   */
  public static List<?> getDepartment(String bidId) {
    try {
      OBContext.setAdminMode();
      String query = "select distinct val.c_salesregion_id from escm_bidmgmt_line ln "
          + " join c_validcombination val on ln.c_validcombination_id = val.c_validcombination_id "
          + " where escm_bidmgmt_id =:bidId and ln.issummarylevel='N'";
      SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
      sqlQuery.setParameter("bidId", bidId);
      return sqlQuery.list();
    } catch (OBException e) {
      log.error("Exception while getDepartment:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get budget Type from bid lines
   * 
   * @param bidid
   * @return list
   */
  public static List<?> getBudgetType(String bidId) {
    try {
      OBContext.setAdminMode();
      String query = "select distinct val.c_campaign_id from escm_bidmgmt_line ln "
          + "join c_validcombination val on ln.c_validcombination_id = val.c_validcombination_id "
          + "where escm_bidmgmt_id =:bidId and ln.issummarylevel='N'";
      SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
      sqlQuery.setParameter("bidId", bidId);
      return sqlQuery.list();
    } catch (OBException e) {
      log.error("Exception while getBudgetType:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * check bid source reference
   * 
   * @param bidlineid
   * @return list
   */
  public static List<Escmbidsourceref> getBidSourceRef(String bidLnId) {
    try {
      OBContext.setAdminMode();
      OBQuery<Escmbidsourceref> bidsrcref = OBDal.getInstance().createQuery(Escmbidsourceref.class,
          " as e where e.escmBidmgmtLine.id=:bidLnId and e.requisitionLine.id is not null");
      bidsrcref.setNamedParameter("bidLnId", bidLnId);
      return bidsrcref.list();
    } catch (OBException e) {
      log.error("Exception while getBidSourceRef:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get Encumbrance line
   * 
   * @param encumId
   * @param acctId
   * @return list
   */
  public static List<EfinBudgetManencumlines> getEncumLine(String encumId, String acctId) {
    try {
      OBContext.setAdminMode();
      OBQuery<EfinBudgetManencumlines> manline = OBDal.getInstance().createQuery(
          EfinBudgetManencumlines.class,
          " as e where e.manualEncumbrance.id=:encumId and e.accountingCombination.id=:acctId");
      manline.setNamedParameter("encumId", encumId);
      manline.setNamedParameter("acctId", acctId);
      manline.setMaxResult(1);
      return manline.list();
    } catch (OBException e) {
      log.error("Exception while getEncumLine:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Set encumbrance stage as PR in PR encumbrance after canceling bid
   * 
   * @param EscmBidMgmt
   */
  public static void setEncumStagePR(EscmBidMgmt bid) {

    /*
     * Task No.7749 : Note 20767 - After completing bid, if we are doing modification, it takes as
     * split, since we are having some remaining amount. After canceling bid, encumbrance stage is
     * not reverting back to PR
     */
    try {

      // Multiple pr with single encumbrance
      String query = " Select distinct encum.id from escm_bidmgmt bid join bid.escmBidmgmtLineList as bidLn join "
          + " bidLn.escmBidsourcerefList as srcRef join srcRef.requisition as req join req.efinBudgetManencum as  "
          + " encum where bid.id = :bidID";
      if (bid != null) {
        Query encumbranceQry = OBDal.getInstance().getSession().createQuery(query);
        encumbranceQry.setParameter("bidID", bid.getId());
        if (encumbranceQry != null) {
          if (encumbranceQry.list().size() == 1) {
            if (encumbranceQry.iterate().hasNext()) {
              String encumID = encumbranceQry.iterate().next().toString();
              if (encumID != null) {
                EfinBudgetManencum encumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
                    encumID);
                if (encumbrance != null) {
                  if (encumbrance.getEncumStage().equals("BE")) {
                    encumbrance.setEncumStage("PRE");
                    OBDal.getInstance().save(encumbrance);
                  }
                }
              }
            }
          }
        }
      }
    } catch (OBException e) {
      log.error("Exception while setEncumStagePR:" + e);
      throw new OBException(e.getMessage());
    }
  }

}