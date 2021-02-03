package sa.elm.ob.scm.ad_process.POandContract.dao;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.ad_process.purchaseRequisition.RequisitionfundsCheck;
import sa.elm.ob.finance.util.CommonValidations;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;
import sa.elm.ob.scm.ESCMBGWorkbench;
import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.ESCMPaymentSchedule;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmInitialReceipt;
import sa.elm.ob.scm.EscmOrderSourceRef;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmPurchaseOrderConfiguration;
import sa.elm.ob.scm.ad_callouts.dao.POContractSummaryTotPOChangeDAO;
import sa.elm.ob.scm.ad_process.ProposalManagement.ProposalManagementRejectMethods;
import sa.elm.ob.utility.EutForwardReqMoreInfo;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

public class POContractSummaryDAO {

  private final static Logger log = LoggerFactory.getLogger(POContractSummaryDAO.class);

  /**
   * Method to insert the Order lines based on selection
   * 
   * @param orderId
   * @return
   */
  public static String getBgworkbenchd(String orderId) {
    String bgWorkbenchId = null;
    try {
      OBContext.setAdminMode();
      OBQuery<ESCMBGWorkbench> bg = OBDal.getInstance().createQuery(ESCMBGWorkbench.class,
          " as e where e.documentNo.id=:orderID ");
      bg.setNamedParameter("orderID", orderId);
      bg.setMaxResult(1);
      if (bg.list().size() > 0) {
        bgWorkbenchId = bg.list().get(0).getId();
        return bgWorkbenchId;
      }
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log.error("Exception in getBgworkbenchd in POContractSummaryDAO: ", e);
      OBDal.getInstance().rollbackAndClose();
      return bgWorkbenchId;
    } finally {
      OBContext.restorePreviousMode();
    }
    return bgWorkbenchId;
  }

  /**
   * Update auto encumbrance from proposal
   * 
   * @param objOrder
   */
  public static void doPOChangeMofifcationInEncumbrance(Order order) {
    EfinBudgetManencumlines objEncumLine = null;
    try {
      OBContext.setAdminMode();
      for (OrderLine objOrderLine : order.getOrderLineList()) {
        if (!objOrderLine.isEscmIssummarylevel()) {
          // get encumbrance details
          OBQuery<EfinBudgetManencumlines> manline = OBDal.getInstance().createQuery(
              EfinBudgetManencumlines.class,
              " as e  where e.manualEncumbrance.id=:encumId and e.accountingCombination.id=:accId");
          manline.setNamedParameter("encumId", order.getEfinBudgetManencum().getId());
          manline.setNamedParameter("accId", objOrderLine.getEFINUniqueCode().getId());

          manline.setMaxResult(1);
          if (manline.list().size() > 0) {
            objEncumLine = manline.list().get(0);
            // do revision with increase or decrease amount
            BigDecimal amount = BigDecimal.ZERO;
            BigDecimal proposalAmt = BigDecimal.ZERO;
            if (objOrderLine.getEscmProposalmgmtLine() != null) {
              OBQuery<EscmProposalmgmtLine> propMgmtQry = OBDal.getInstance().createQuery(
                  EscmProposalmgmtLine.class,
                  "as e where e.eFINUniqueCode.id=:uniquecodeid and e.id=:propmgmtlineid");
              propMgmtQry.setNamedParameter("uniquecodeid",
                  objOrderLine.getEFINUniqueCode().getId());
              propMgmtQry.setNamedParameter("propmgmtlineid",
                  objOrderLine.getEscmProposalmgmtLine().getId());
              propMgmtQry.setMaxResult(1);
              if (propMgmtQry.list().size() > 0) {
                EscmProposalmgmtLine propMgmtLn = propMgmtQry.list().get(0);
                if (propMgmtLn.getEscmProposalmgmt().getProposalstatus().equals("PAWD")) {
                  proposalAmt = propMgmtLn.getAwardedamount();
                } else {
                  proposalAmt = propMgmtLn.getLineTotal();
                }
              }

            }
            if ((objOrderLine.getLineNetAmount().compareTo(proposalAmt) > 0)
                || objOrderLine.getLineNetAmount().compareTo(proposalAmt) < 0) {
              // inc/dec in encumbrance
              amount = objOrderLine.getLineNetAmount().subtract(proposalAmt);
            }

            EfinBudManencumRev manEncumRev = OBProvider.getInstance().get(EfinBudManencumRev.class);
            if (!StringUtils.isEmpty(objEncumLine.getId())
                && amount.compareTo(BigDecimal.ZERO) != 0) {
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
              order.setEfinEncumbered(true);
              OBDal.getInstance().save(order);
            }
          }
        }
      }
      order.setEfinEncumbered(true);
      OBDal.getInstance().save(order);
      // OBDal.getInstance().save(objEncum);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log.error("Exception while creating encumbrance modification in create new po version", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method is responsible to do modification in existing encumbrance
   * 
   * @param order
   * @param baseOrder
   */
  public static String doMofifcationInEncumbrance(Order order, Order baseOrder) {
    EfinBudgetManencum objEncum = order.getEfinBudgetManencum();
    EfinBudgetManencumlines objEncumLine = null;
    OBQuery<OrderLine> oldOrderLineQry = null;
    try {

      if (order.getEscmOldOrder() != null) {
        // compare amount between old version and new version order line
        for (OrderLine objOrderLine : order.getOrderLineList()) {

          if (!objOrderLine.isEscmIssummarylevel()) {
            // get the orderline if product id is not null
            if (objOrderLine.getProduct() != null) {
              oldOrderLineQry = OBDal.getInstance().createQuery(OrderLine.class,
                  "as e where e.salesOrder.id=:oldOrderId and e.product.id=:productID "
                      + " and e.eFINUniqueCode.id=:uniquecodeID ");
              oldOrderLineQry.setNamedParameter("oldOrderId", order.getEscmOldOrder().getId());
              oldOrderLineQry.setNamedParameter("productID", objOrderLine.getProduct().getId());
              oldOrderLineQry.setNamedParameter("uniquecodeID",
                  objOrderLine.getEFINUniqueCode().getId());

            }
            // get the orderline if description is not null
            else {
              oldOrderLineQry = OBDal.getInstance().createQuery(OrderLine.class,
                  "as e where e.salesOrder.id=:oldOrderID and e.escmProdescription=:prdDesc "
                      + " and e.eFINUniqueCode.id=:uniquecodeID ");
              oldOrderLineQry.setNamedParameter("oldOrderID", order.getEscmOldOrder().getId());
              oldOrderLineQry.setNamedParameter("prdDesc", objOrderLine.getEscmProdescription());
              oldOrderLineQry.setNamedParameter("uniquecodeID",
                  objOrderLine.getEFINUniqueCode().getId());

            }
            if (oldOrderLineQry.list().size() > 0) {
              OrderLine oldOrderLine = oldOrderLineQry.list().get(0);
              // get encumbrance details

              OBQuery<EfinBudgetManencumlines> manline = OBDal.getInstance().createQuery(
                  EfinBudgetManencumlines.class,
                  " as e  where e.manualEncumbrance.id=:encumID and e.accountingCombination.id=:uniqID ");
              manline.setNamedParameter("encumID", objEncum.getId());
              manline.setNamedParameter("uniqID", oldOrderLine.getEFINUniqueCode().getId());

              manline.setMaxResult(1);
              if (manline.list().size() > 0) {
                objEncumLine = manline.list().get(0);
              }

              if (((objOrderLine.getLineNetAmount().compareTo(oldOrderLine.getLineNetAmount()) > 0)
                  || (objOrderLine.getLineNetAmount()
                      .compareTo(oldOrderLine.getLineNetAmount()) < 0))
                  && objEncumLine != null) {
                // do revision with increase or decrease amount
                BigDecimal amount = objOrderLine.getLineNetAmount()
                    .subtract(oldOrderLine.getLineNetAmount());
                // objEncum.setRemainingamt(objEncum.getRemainingamt().add(amount));
                // OBDal.getInstance().save(objEncum);

                EfinBudManencumRev manEncumRev = OBProvider.getInstance()
                    .get(EfinBudManencumRev.class);

                if (!StringUtils.isEmpty(objEncumLine.getId())) {
                  // changes in Propsal encumbrance
                  doChangeMofifcationInPOEncumbrance(order, objOrderLine, false);

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
                  order.setEfinEncumbered(true);
                  OBDal.getInstance().save(order);
                }
              }

            } else {
              OBQuery<EfinBudgetManencumlines> manline = OBDal.getInstance().createQuery(
                  EfinBudgetManencumlines.class,
                  " as e  where e.manualEncumbrance.id=:encumID and e.accountingCombination.id=:acctID ");
              manline.setNamedParameter("encumID", objEncum.getId());
              manline.setNamedParameter("acctID", objOrderLine.getEFINUniqueCode().getId());

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
                manEncumRev.setRevamount(objOrderLine.getLineNetAmount());
                manEncumRev.setEncumbranceType("MO");
                manEncumRev.setAuto(true);
                manEncumRev.setAccountingCombination(objEncumLine.getAccountingCombination());
                OBDal.getInstance().save(manEncumRev);

                objEncumLine
                    .setAPPAmt(objEncumLine.getAPPAmt().add(objOrderLine.getLineNetAmount()));
                OBDal.getInstance().save(objEncumLine);
                order.setEfinEncumbered(true);
                OBDal.getInstance().save(order);
              }
            }
          }

        }
        order.setEfinEncumbered(true);
        OBDal.getInstance().save(order);

        OBDal.getInstance().save(objEncum);
        OBDal.getInstance().flush();
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

  public static void doChangeMofifcationInPOEncumbrance(Order order, OrderLine objOrderLine,
      Boolean isReject) {
    EfinBudgetManencumlines objEncumLine = null;
    BigDecimal amount = BigDecimal.ZERO;
    BigDecimal oldAmount = BigDecimal.ZERO;
    BigDecimal revisedAmt = BigDecimal.ZERO;
    String poEncumId = null;
    try {
      OBContext.setAdminMode();
      if (order.getEscmOrdertype().equals("PUR_REL")) {
        if (order.getEscmProposalmgmt() != null)
          poEncumId = order.getEscmProposalmgmt().getEfinEncumbrance().getId();
        String encumId = order.getEfinBudgetManencum().getId();
        if (poEncumId != null && encumId != null && !poEncumId.equals(encumId)) {
          amount = objOrderLine.getLineNetAmount();
          oldAmount = objOrderLine.getEscmOldOrderline().getLineNetAmount();
          revisedAmt = amount.subtract(oldAmount);
          if (isReject) {
            revisedAmt = revisedAmt.negate();
          }
          if (!objOrderLine.isEscmIssummarylevel()) {
            // get encumbrance details
            OBQuery<EfinBudgetManencumlines> manline = OBDal.getInstance().createQuery(
                EfinBudgetManencumlines.class,
                " as e  where e.manualEncumbrance.id=:encumID and e.accountingCombination.id=:accID ");
            manline.setNamedParameter("encumID", poEncumId);
            manline.setNamedParameter("accID", objOrderLine.getEFINUniqueCode().getId());

            manline.setMaxResult(1);
            if (manline.list().size() > 0) {
              objEncumLine = manline.list().get(0);

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
                manEncumRev.setRevamount(revisedAmt.negate());
                manEncumRev.setEncumbranceType("MO");
                manEncumRev.setAuto(true);
                manEncumRev.setAccountingCombination(objEncumLine.getAccountingCombination());
                OBDal.getInstance().save(manEncumRev);

                objEncumLine.setAPPAmt(objEncumLine.getAPPAmt().add(revisedAmt.negate()));
                OBDal.getInstance().save(objEncumLine);
                order.setEfinEncumbered(true);
                OBDal.getInstance().save(order);
              }
            }
          }
        }
      }

      order.setEfinEncumbered(true);
      OBDal.getInstance().save(order);
      // OBDal.getInstance().save(objEncum);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log.error("Exception while creating encumbrance modification in create new po version", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  // get count of pending quantity in order.
  @SuppressWarnings("unchecked")
  public static BigDecimal getPendingqtycount(String orderId) {
    BigDecimal count = BigDecimal.ZERO;
    try {
      OBContext.setAdminMode();
      String sqlQuery = " select (coalesce(sum(movementqty),0)-(coalesce(sum(quantityporec),0)- "
          + "coalesce(sum(quantityreturned),0)-coalesce(sum(quantityrejected),0)-coalesce(sum(quantityirr),0))- "
          + "coalesce(sum(quantitycanceled) ,0)) as count " + "from c_order ord "
          + "join c_orderline line on line.c_order_id=ord.c_order_id "
          + "join escm_ordershipment ship on ship.c_orderline_id=line.c_orderline_id "
          + "where ord.c_order_id =:orderID group by ord.c_order_id";
      SQLQuery queryList = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
      queryList.setParameter("orderID", orderId);
      log.debug("sqlQuery" + sqlQuery);
      if (queryList != null) {
        List<Object> rows = queryList.list();
        if (rows.size() > 0) {
          count = new BigDecimal(Integer.valueOf(rows.get(0).toString()));
        }
      }
      return count;
    } catch (Exception e) {
      log.error("Exception in count of pending qty in po refersh version: ", e);
      OBDal.getInstance().rollbackAndClose();
      return count;
    }
  }

  /* Get line qty to validate with older version */
  public static boolean checkOlderVersionQty(String newOrderId, String oldOrderId) {
    boolean hasLessQty = false;
    try {
      OBContext.setAdminMode();

      OBQuery<OrderLine> ordln = OBDal.getInstance().createQuery(OrderLine.class,
          " as e where e.salesOrder.id=:orderID ");
      ordln.setNamedParameter("orderID", newOrderId);

      if (ordln.list().size() > 0) {
        for (OrderLine ordLn : ordln.list()) {

          if (!ordLn.isEscmIssummarylevel()) {

            BigDecimal receivedQty = BigDecimal.ZERO;
            BigDecimal newOrdQty = ordLn.getOrderedQuantity();// ordln ord qty

            // get old order received quantity
            BigDecimal poReceiptQty = ordLn.getEscmQtyporec() != null ? ordLn.getEscmQtyporec()
                : BigDecimal.ZERO;
            BigDecimal qtyIrr = ordLn.getEscmQtyirr() != null ? ordLn.getEscmQtyirr()
                : BigDecimal.ZERO;
            BigDecimal qtyRejected = ordLn.getEscmQtyrejected() != null ? ordLn.getEscmQtyrejected()
                : BigDecimal.ZERO;
            BigDecimal qtyReturned = ordLn.getEscmQtyreturned() != null ? ordLn.getEscmQtyreturned()
                : BigDecimal.ZERO;
            BigDecimal qtyCancelled = ordLn.getEscmQtycanceled() != null
                ? ordLn.getEscmQtycanceled()
                : BigDecimal.ZERO;
            BigDecimal legacyDelQty = ordLn.getEscmLegacyQtyDelivered() != null
                ? ordLn.getEscmLegacyQtyDelivered()
                : BigDecimal.ZERO;

            BigDecimal qtyDelivered = poReceiptQty.subtract(qtyIrr).subtract(qtyRejected)
                .subtract(qtyReturned);

            receivedQty = qtyDelivered.add(qtyCancelled).add(legacyDelQty);

            log.debug(receivedQty + ">" + newOrdQty);
            if (receivedQty.compareTo(newOrdQty) > 0) {
              return true;
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in checkOlderVersionQty: ", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
    return hasLessQty;
  }

  /**
   * Checks whether new order amount is lesser than old order received amount
   * 
   * @param newOrderId
   * @param oldOrderId
   * @return isAmtlesser
   */
  public static boolean checkOlderVersionAmt(String newOrderId, String oldOrderId) {

    boolean isAmtlesser = false;

    try {
      OBContext.setAdminMode();

      OBQuery<OrderLine> ordln = OBDal.getInstance().createQuery(OrderLine.class,
          " as e where e.salesOrder.id=:orderID ");
      ordln.setNamedParameter("orderID", newOrderId);

      if (ordln.list().size() > 0) {

        for (OrderLine ordLn : ordln.list()) {

          if (!ordLn.isEscmIssummarylevel()) {

            BigDecimal receivedAmt = BigDecimal.ZERO;
            BigDecimal newOrdAmt = ordLn.getLineNetAmount();// ordln net line amount

            // get old order received amount
            BigDecimal poReceiptAmt = ordLn.getEscmAmtporec() != null ? ordLn.getEscmAmtporec()
                : BigDecimal.ZERO;
            BigDecimal amtReturned = ordLn.getEscmAmtreturned() != null ? ordLn.getEscmAmtreturned()
                : BigDecimal.ZERO;
            BigDecimal amtCancelled = ordLn.getEscmAmtcanceled() != null
                ? ordLn.getEscmAmtcanceled()
                : BigDecimal.ZERO;
            BigDecimal legacyDelAmt = ordLn.getEscmLegacyAmtDelivered() != null
                ? ordLn.getEscmLegacyAmtDelivered()
                : BigDecimal.ZERO;

            BigDecimal amtDelivered = poReceiptAmt.subtract(amtReturned);
            receivedAmt = amtDelivered.add(amtCancelled).add(legacyDelAmt);

            log.debug(receivedAmt + ">" + newOrdAmt);
            if (receivedAmt.compareTo(newOrdAmt) > 0) {
              isAmtlesser = true;
              return isAmtlesser;
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in checkOlderVersionAmt: ", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
    return isAmtlesser;
  }

  /**
   * check whether dept is same or not for requisition.
   * 
   * @param orderId
   * @return true, if requistion dept is not match.
   */
  public static boolean checkSameDept(String orderId) {
    try {
      OBContext.setAdminMode();
      String sqlQuery = "select em_escm_department_id from m_requisition req "
          + " join escm_ordersource_ref ref on ref.m_requisition_id = req.m_requisition_id "
          + " join c_orderline line on line.c_orderline_id = ref.c_orderline_id "
          + " where line.c_order_id =:orderID group by em_escm_department_id ";
      SQLQuery queryList = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
      queryList.setParameter("orderID", orderId);
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

  /**
   * check associated PR in Bid Full Qty used or partialy used or combine more than one Encumbrance
   * 
   * @param bidmgmt
   * @return Jsonobject of Encumbrance List, (Type-Split or Merge),PR is associated or Not
   */
  @SuppressWarnings("rawtypes")
  public static JSONObject checkFullPRQtyUitlizeorNot(Order poOrder) {
    List<Requisition> req = new ArrayList<Requisition>();
    List<EfinBudgetManencum> enc = new ArrayList<EfinBudgetManencum>();
    boolean isAssociatePREncumbrance = false;
    int srcrefReqLineCount = 0, reqLineCount = 0;
    BigDecimal srcrefLineQty = BigDecimal.ZERO, reqLineQty = BigDecimal.ZERO,
        reqlineAmt = BigDecimal.ZERO;
    Boolean isLineCountSame = true, isLineQtySame = true;
    // Boolean isLineUniqCodeSame = true;
    JSONObject result = new JSONObject();
    boolean isEncReqCountSame = true, isEncumAppAmtZero = true, isManualLine = false;
    int encReqCount = 0, reqCount = 0;
    try {

      if (poOrder != null) {
        if (poOrder.getOrderLineList().size() > 0) {
          for (OrderLine line : poOrder.getOrderLineList()) {
            if (!line.isEscmIssummarylevel() && !line.isEscmIsmanual()) {
              if (line.getEscmOrdersourceRefList().size() > 0) {
                for (EscmOrderSourceRef srcrefObj : line.getEscmOrdersourceRefList()) {
                  // chk source ref having purchase requisition and corresponding purchase
                  // requistion
                  // is PRE(Purchase Encumbrance Type)
                  if (srcrefObj.getRequisition() != null
                      && srcrefObj.getRequisition().getEfinBudgetManencum() != null && srcrefObj
                          .getRequisition().getEfinBudgetManencum().getEncumType().equals("PRE")) {
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
            } else if (!line.isEscmIssummarylevel() && line.isEscmIsmanual()) {
              isManualLine = true;
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
          OBQuery<EscmOrderSourceRef> srcref = OBDal.getInstance().createQuery(
              EscmOrderSourceRef.class,
              " as e where e.salesOrderLine.id in( select b.id from OrderLine b "
                  + " where b.salesOrder.id=:orderID ) and e.requisition.id=:reqID )");
          srcref.setNamedParameter("orderID", poOrder.getId());
          srcref.setNamedParameter("reqID", reqObj.getId());

          log.debug("srcref1:" + srcref.getWhereAndOrderBy());
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
                OBQuery<EscmOrderSourceRef> srcref = OBDal.getInstance().createQuery(
                    EscmOrderSourceRef.class,
                    " as e where e.salesOrderLine.id in( select b.id from OrderLine b where "
                        + "b.salesOrder.id=:orderID ) and e.requisition.id=:reqID and"
                        + " e.requisitionLine.id=:reqLineID ");
                srcref.setNamedParameter("orderID", poOrder.getId());
                srcref.setNamedParameter("reqID", reqObj.getId());
                srcref.setNamedParameter("reqLineID", line.getId());

                srcref.setMaxResult(1);
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

          BigDecimal diff = poOrder.getGrandTotalAmount().subtract(reqlineAmt);
          if (diff.compareTo(BigDecimal.ZERO) > 0 && remainigAmt.compareTo(diff) == 0) {
            isEncumAppAmtZero = true;
          }

          if (isEncumAppAmtZero) {
            OBQuery reqcount = OBDal.getInstance().createQuery(Requisition.class,
                " as e where e.efinBudgetManencum.id=:encumID"
                    + " and e.escmDocStatus  not in ('ESCM_CA') ");// and e.escmDocStatus
            reqcount.setNamedParameter("encumID", encumId);
            // not in ('ESCM_CA')
            if (reqcount.list().size() > 0) {
              encReqCount = reqcount.list().size();
            }
            reqCount = requisition.size();
            if (reqCount != encReqCount) {
              isEncReqCountSame = false;
            }
          }
        }
        /*
         * EfinBudgetManencum encum = poOrder.getEscmProposalmgmt().getEfinEncumbrance(); if
         * (encum.getAppliedAmount().equals(encum.getTotalAmount())) { isStageMove = true; }
         */
        // if line qty same set isFullQtyUsed as "true"
        if (isLineQtySame && isEncumAppAmtZero && isEncReqCountSame && !isManualLine) {
          result.put("isFullQtyUsed", true);
          result.put("isLineCountSame", true);
          result.put("encumbrance", encumId);
        }
        // if line qty not same set isFullQtyUsed as "False" and encumbrance list is
        // more than one
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

  /**
   * get PR detail based on Order Source ref
   * 
   * @param order
   * @return JSONObject of Sourceref details
   */
  @SuppressWarnings("rawtypes")
  public static JSONObject getPRDetailsBasedOnOrdQty(Order order) {
    String strQuery = null;
    Query query = null;
    BigDecimal Amount = BigDecimal.ZERO, totalAmount = BigDecimal.ZERO;
    JSONObject prResult = new JSONObject(), uniquecodeResult = new JSONObject(), json = null,
        json1 = null, UniqueCodejson = null, orderLineJson = null, json2 = null;
    JSONArray prlistArray = new JSONArray(), encListArray = new JSONArray(),
        uniqueCodeListArray = new JSONArray(), linearraylist = null;
    String temporderLineId = null;
    JSONObject result = new JSONObject();
    Boolean sameUniqueCode = false;
    try {

      // calculate the qty amount corresponding PR linettoal
      strQuery = " select req.em_efin_budget_manencum_id,reqln.em_efin_c_validcombination_id,ln.linenetamt,"
          + " ln.c_orderline_id,ln.em_efin_c_validcombination_id as vaildid,ln.em_escm_issummarylevel, case when coalesce(sum(reqln.qty),0) > 0 "
          + "          then sum(round((coalesce(reqln.priceactual,0)*coalesce(ref.quantity,0)),2)) "
          + "          else 0 end as amount,ln.em_efin_c_validcombination_id "
          + " from escm_ordersource_ref  ref left join c_orderline ln on ln.c_orderline_id = ref.c_orderline_id "
          + "  left join m_requisitionline reqln on reqln.m_requisitionline_id= ref.m_requisitionline_id and reqln.em_escm_issummary='N' "
          + " left join m_requisition req on req.m_requisition_id= reqln.m_requisition_id and req.em_efin_budget_manencum_id is not null "
          + " where ln.c_order_id = ?  and ln.em_escm_issummarylevel  ='N' "
          + " group by ln.c_orderline_id ,req.em_efin_budget_manencum_id , reqln.em_efin_c_validcombination_id ,ln.em_efin_c_validcombination_id ";
      query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      query.setParameter(0, order.getId());
      log.debug("strQuery:" + query.toString());
      List queryList = query.list();
      if (queryList != null && queryList.size() > 0) {
        for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
          sameUniqueCode = false;
          Object[] row = (Object[]) iterator.next();

          // if temporder line id is not equal to current order lines id then create new
          // json object
          if (temporderLineId != null && !temporderLineId.equals(row[3].toString())) {
            json.put("encList", encListArray);
            prlistArray.put(json);
            encListArray = new JSONArray();

            json = new JSONObject();
            json.put("orderlineId", row[3].toString());
            json.put("ordervalidcomId", row[4].toString());

            OrderLine line = OBDal.getInstance().get(OrderLine.class, row[3].toString());

            // form the encum list
            if (!line.isEscmIsmanual()) {
              json1 = new JSONObject();
              json1.put("encumId", row[0].toString());
              json1.put("encamount", row[2].toString());
              json1.put("validcomId", row[1].toString());
              encListArray.put(json1);

              temporderLineId = row[3].toString();
              Amount = new BigDecimal(json1.getString("encamount"));
              totalAmount = Amount;
              json.put("totalamount", totalAmount);
            } else {
              json.put("totalamount", line.getLineNetAmount());
            }

          }
          // if temporder line id is equals to current order lines id then add the amount in total
          // amount
          else if (temporderLineId != null && temporderLineId.equals(row[3].toString())) {

            totalAmount = totalAmount.add(new BigDecimal(row[2].toString()));
            json.put("totalamount", totalAmount);
            // form the encum list if one order line have multiple encum list
            json1 = new JSONObject();
            json1.put("encumId", row[0].toString());
            json1.put("encamount", row[2].toString());
            json1.put("validcomId", row[1].toString());
            encListArray.put(json1);
          }
          // if temporder line id is null then form the json
          else {
            json = new JSONObject();
            json.put("orderlineId", row[3].toString());
            json.put("ordervalidcomId", row[4].toString());
            // form the encum list
            json1 = new JSONObject();
            json1.put("encumId", row[0].toString());
            json1.put("encamount", row[2].toString());
            json1.put("validcomId", row[1].toString());
            encListArray.put(json1);

            temporderLineId = row[3].toString();
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
                orderLineJson = new JSONObject();
                orderLineJson.put("lineId", row[3].toString());
                linearraylist.put(orderLineJson);
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
              orderLineJson = new JSONObject();
              orderLineJson.put("lineId", row[3].toString());
              linearraylist.put(orderLineJson);
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
      log.debug("result:" + result);
    } catch (Exception e) {
      log.error("Exception in getPRAmountBasedOnOrderQty " + e.getMessage());
    }
    return result;
  }

  /**
   * get PR detail based on Order Source ref
   * 
   * @param order
   * @return JSONObject of Sourceref details
   */
  @SuppressWarnings("rawtypes")
  public static JSONObject getPRDetailsBasedOnOrdQty1(Order order) {
    String strQuery = null;
    Query query = null;
    BigDecimal totalAmount = BigDecimal.ZERO;
    JSONObject prResult = new JSONObject(), json = null, json1 = null;
    JSONArray prlistArray = new JSONArray(), encListArray = new JSONArray();
    String tempbidLineId = null;
    JSONObject result = new JSONObject();
    BigDecimal reqamt = BigDecimal.ZERO, orderAmt = BigDecimal.ZERO, diff = BigDecimal.ZERO;
    try {
      // calculate the qty amount corresponding PR linettoal
      strQuery = " select req.em_efin_budget_manencum_id,reqln.em_efin_c_validcombination_id,"
          + " case when coalesce(sum(reqln.qty),0) > 0 "
          + " then sum(round((coalesce(reqln.priceactual,0)*coalesce(ref.quantity,0)),2)) "
          + " else 0 end as amount,ln.c_orderline_id,ln.em_efin_c_validcombination_id as ordvalidcomid,ln.em_escm_issummarylevel,ln.linenetamt "
          + " from escm_ordersource_ref  ref right join c_orderline ln on ln.c_orderline_id = ref.c_orderline_id "
          + "  left join m_requisitionline reqln on reqln.m_requisitionline_id= ref.m_requisitionline_id and reqln.em_escm_issummary='N'  "
          + " left join m_requisition req on req.m_requisition_id= reqln.m_requisition_id and req.em_efin_budget_manencum_id is not null "
          + " where ln.c_order_id = ?  and ln.em_escm_issummarylevel  ='N'  "
          + " group by ln.c_orderline_id ,req.em_efin_budget_manencum_id , reqln.em_efin_c_validcombination_id ,ln.em_efin_c_validcombination_id "
          + " order by ln.c_orderline_id ";
      query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      query.setParameter(0, order.getId());

      List queryList = query.list();
      if (queryList != null && queryList.size() > 0) {
        for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          reqamt = BigDecimal.ZERO;
          orderAmt = BigDecimal.ZERO;
          diff = BigDecimal.ZERO;
          if (row[2] != null)
            reqamt = new BigDecimal(row[2].toString());
          if (row[6] != null)
            orderAmt = new BigDecimal(row[6].toString());
          diff = orderAmt.subtract(reqamt);
          // if tempbid line id is equals to current bid lines id then add the amount in
          // total
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
              json1.put("encamount", row[2].toString());// orderAmt
            }
            encListArray.put(json1);
          }
          // if tempbid line id is null then form the json
          else {
            encListArray = new JSONArray();
            json = new JSONObject();
            json.put("orderlineId", row[3].toString());
            json.put("ordervalidcomId", row[4].toString());
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
              json1.put("encamount", row[2].toString());// orderAmt
            }
            encListArray.put(json1);
            tempbidLineId = row[3].toString();
            totalAmount = orderAmt;
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
   * create new encum for po.
   * 
   * @param order
   * @param encumbranceObj
   * @return
   */
  public static EfinBudgetManencum insertEncumbranceOrder(Order order,
      EfinBudgetManencum encumbranceObj) {
    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    EfinBudgetManencum encumbrance = null;
    try {
      OBContext.setAdminMode();

      // insert the Encumbrance
      encumbrance = OBProvider.getInstance().get(EfinBudgetManencum.class);
      encumbrance.setClient(order.getClient());
      encumbrance.setOrganization(order.getOrganization());
      encumbrance.setActive(true);
      encumbrance.setUpdatedBy(order.getCreatedBy());
      encumbrance.setCreationDate(new java.util.Date());
      encumbrance.setCreatedBy(order.getCreatedBy());
      encumbrance.setUpdated(new java.util.Date());
      log.debug("date" + dateFormat.parse(dateFormat.format(new Date())));
      encumbrance.setAccountingDate(dateFormat.parse(dateFormat.format(new Date())));
      encumbrance.setTransactionDate(dateFormat.parse(dateFormat.format(new Date())));
      encumbrance.setEncumType("POE");
      encumbrance.setAuto(true);
      encumbrance.setDocumentStatus("DR");
      encumbrance.setSalesCampaign(encumbranceObj.getSalesCampaign());
      encumbrance.setSalesRegion(encumbranceObj.getSalesRegion());
      encumbrance.setEncumStage("POE");
      encumbrance.setBudgetInitialization(encumbranceObj.getBudgetInitialization());
      if (order.getEscmNotes() != null)
        encumbrance.setDescription(order.getEscmNotes());
      else
        encumbrance.setDescription(order.getDocumentNo());
      OBDal.getInstance().save(encumbrance);
      OBDal.getInstance().flush();

      return encumbrance;

    } catch (Exception e) {
      log.error("Exception in insertEncumbranceOrder " + e.getMessage());
    }
    return encumbrance;
  }

  /**
   * insert encum lines for po
   * 
   * @param bidmgmt
   * @param encumbrancenewObj
   * @param oldencumbranceObj
   * @param result
   */
  public static EfinBudgetManencumlines insertEncumbranceLinesOrder(Order order,
      EfinBudgetManencum encumbrancenewObj, EfinBudgetManencum oldencumbranceObj, JSONObject json) {
    Long lineno = 10L;
    BigDecimal Amount = BigDecimal.ZERO, totalAmount = BigDecimal.ZERO;
    JSONObject prResult = null;
    EfinBudgetManencumlines manualline = null;
    try {
      OBContext.setAdminMode();

      log.debug("prResult:" + prResult);
      if (json != null) {
        OrderLine ln = OBDal.getInstance().get(OrderLine.class, json.getString("orderlineId"));
        totalAmount = new BigDecimal(json.getString("totalamount"));
        log.debug("amount1:" + Amount);

        // check already unqiuecode exists or not
        OBQuery<EfinBudgetManencumlines> lnexistQry = OBDal.getInstance().createQuery(
            EfinBudgetManencumlines.class, " as e where e.manualEncumbrance.id=:encumnewID "
                + "and e.accountingCombination.id=:acctID");
        lnexistQry.setNamedParameter("encumnewID", encumbrancenewObj.getId());
        lnexistQry.setNamedParameter("acctID", ln.getEFINUniqueCode().getId());

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

          // update encumbranceline in order.
          updateEncumbranceLineInOrder(order, manualline, ln.getEFINUniqueCode());
        }
      }
    } catch (Exception e) {
      log.error("Exception in insertEncumbranceLines " + e.getMessage());
    }
    return manualline;
  }

  /**
   * insert modification for encumbrance
   * 
   * @param bidmgmt
   * @param encumbrancenewObj
   * @param oldencumbranceObj
   * @param result
   */
  public static void insertModification(Order order, EfinBudgetManencum encumbrancenewObj,
      EfinBudgetManencum oldencumbranceObj, JSONObject result) {
    BigDecimal Amount = BigDecimal.ZERO;
    JSONObject json = null, jsonencum = null, result1 = null;
    JSONObject prResult = null;
    EfinBudgetManencumlines manualline = null;
    EfinBudManencumRev manEncumRev = null;
    String tempOrderLineId = null;
    BigDecimal totalAmount = BigDecimal.ZERO;
    BigDecimal amtInModification = BigDecimal.ZERO;
    try {
      OBContext.setAdminMode();

      // get PR detail based on associated Bid source ref .
      prResult = POContractSummaryDAO.getPRDetailsBasedOnOrdQty1(order);

      log.debug("prResult:" + prResult);
      if (prResult != null && prResult.getJSONObject("prListarray") != null) {
        result1 = prResult.getJSONObject("prListarray");
        JSONArray array = result1.getJSONArray("list");
        for (int i = 0; i < array.length(); i++) {
          json = array.getJSONObject(i);
          JSONArray encumarray = json.getJSONArray("encList");
          totalAmount = new BigDecimal(json.getString("totalamount"));
          for (int j = 0; j < encumarray.length(); j++) {
            jsonencum = encumarray.getJSONObject(j);
            if (jsonencum.has("encumId") && jsonencum.getString("encumId") != null
                && jsonencum.has("validcomId") && jsonencum.getString("validcomId") != null) {

              // // new encum line
              // OBQuery<EfinBudgetManencumlines> lnexistQry = OBDal.getInstance().createQuery(
              // EfinBudgetManencumlines.class,
              // " as e where e.manualEncumbrance.id='" + encumbrancenewObj.getId()
              // + "' and e.accountingCombination.id='" + jsonencum.getString("validcomId")
              // + "'");
              // lnexistQry.setMaxResult(1);
              // if (lnexistQry.list().size() > 0)
              // manualline = lnexistQry.list().get(0);
              // get old encumbrance line

              OBQuery<EfinBudgetManencumlines> lines = OBDal.getInstance().createQuery(
                  EfinBudgetManencumlines.class,
                  " as e where e.manualEncumbrance.id=:encumID and e.accountingCombination.id=:accID ");
              lines.setNamedParameter("encumID", jsonencum.getString("encumId"));
              lines.setNamedParameter("accID", jsonencum.getString("validcomId"));

              lines.setMaxResult(1);
              if (lines.list().size() > 0) {
                // decrease the rev amount and remaining amount
                Amount = new BigDecimal(jsonencum.getString("encamount"));
                log.debug("amount1:" + Amount);
                EfinBudgetManencumlines encumline = lines.list().get(0);

                // Task no.7749: If amt is increased in PO, insert modification with increased amt
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
                manEncumRev = insertEncumbranceModification(encumline, amtInModification.negate(),
                    manualline, true);
                if (tempOrderLineId == null
                    || !tempOrderLineId.equals(json.getString("orderlineId"))) {
                  manualline = insertEncumbranceLinesOrder(order, encumbrancenewObj,
                      oldencumbranceObj, json);
                }
                if (manualline != null && manEncumRev != null) {
                  manEncumRev.setSRCManencumline(manualline);
                  manualline.setFundsAvailable(encumline.getFundsAvailable());
                  OBDal.getInstance().save(manEncumRev);
                  OBDal.getInstance().save(manualline);
                }
                encumline.setAPPAmt(encumline.getAPPAmt().subtract(amtInModification));
                OBDal.getInstance().save(encumline);
                OBDal.getInstance().flush();
              }
            } else {
              manualline = insertEncumbranceLinesOrder(order, encumbrancenewObj, oldencumbranceObj,
                  json);
            }
            tempOrderLineId = json.getString("orderlineId");
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in insertEncumbranceLines " + e.getMessage(), e);
    }
  }

  /**
   * 
   * @param order
   * @param encumbrancenewObj
   * @param oldencumbranceObj
   * @param result;
   */
  public static void insertEncumbranceLinesPAOrder(Order order,
      EfinBudgetManencum encumbrancenewObj, EfinBudgetManencum oldencumbranceObj) {
    Long lineno = 10L;
    BigDecimal Amount = BigDecimal.ZERO, totalAmount = BigDecimal.ZERO;
    EfinBudgetManencumlines manualline = null;
    EfinBudManencumRev modificationObj = null;
    BigDecimal amtInModification = BigDecimal.ZERO;
    try {
      OBContext.setAdminMode();

      for (OrderLine oline : order.getOrderLineList()) {
        if (!oline.isEscmIssummarylevel()) {
          OrderLine ln = OBDal.getInstance().get(OrderLine.class, oline.getId());
          totalAmount = ln.getLineNetAmount();

          // modifcation
          OBQuery<EfinBudgetManencumlines> lines = OBDal.getInstance().createQuery(
              EfinBudgetManencumlines.class, " as e where e.manualEncumbrance.id=:oldEncumId"
                  + " and e.accountingCombination.id=:uniquecodeID");
          lines.setNamedParameter("oldEncumId", oldencumbranceObj.getId());
          lines.setNamedParameter("uniquecodeID", oline.getEFINUniqueCode().getId());

          lines.setMaxResult(1);
          if (lines.list().size() > 0) {
            // decrease the rev amount and remaining amount
            if (oline.getEscmProposalmgmtLine() != null
                && !order.getEscmOrdertype().equals("PUR_REL")) {

              Amount = oline.getEscmProposalmgmtLine().getEscmProposalmgmt().getProposalstatus()
                  .equals("PAWD") ? oline.getEscmProposalmgmtLine().getAwardedamount()
                      : oline.getEscmProposalmgmtLine().getLineTotal();

            } else {
              // if (!oline.getEscmProposalmgmtLine().getEscmProposalmgmt().isTaxLine()
              // && order.isEscmIstax()) {
              // Amount = oline.getLineNetAmount().subtract(oline.getEscmLineTaxamt());
              // } else {
              Amount = oline.getLineNetAmount();
              // }

            }

            log.debug("amount:" + Amount);
            EfinBudgetManencumlines encumline = lines.list().get(0);

            // Task no.7749: If amt is increased in PO, insert modification with increased amt
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
            if (order.getEscmOrdertype().equals("PUR_REL") && order.getEscmProposalmgmt() != null) {
              modificationObj = insertEncumbranceModificationForPurchaseRlse(encumline,
                  amtInModification.negate(), manualline);
            } else {
              modificationObj = insertEncumbranceModification(encumline, amtInModification.negate(),
                  manualline, true);
            }

            encumline.setAPPAmt(encumline.getAPPAmt().subtract(amtInModification));
            OBDal.getInstance().save(encumline);
          }

          // check already unqiuecode exists or not
          OBQuery<EfinBudgetManencumlines> lnexistQry = OBDal.getInstance().createQuery(
              EfinBudgetManencumlines.class, " as e where e.manualEncumbrance.id=:newEncumId and "
                  + "e.accountingCombination.id=:accID ");
          lnexistQry.setNamedParameter("newEncumId", encumbrancenewObj.getId());
          lnexistQry.setNamedParameter("accID", ln.getEFINUniqueCode().getId());
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

            // update encumbranceline in order.
            updateEncumbranceLineInOrder(order, manualline, ln.getEFINUniqueCode());
          }
          if (manualline != null) {
            modificationObj.setSRCManencumline(manualline);
            OBDal.getInstance().save(modificationObj);
          }
          // update the budget Inquiry encumbrance amount value(+ve) for newly created
          // encumbrance
          // lines account Trigger changes
          // EfinEncumbarnceRevision.updateBudgetInquiry(manualline, encumbrancenewObj,
          // totalAmount);

          // loop the old encumbrance list and decrease the applied
          // value,revision,remaining amount
          // and created decrease entry in revision for old encumbrance
          // get old encumbrance line

        }
      }

    } catch (Exception e) {
      log.error("Exception in insertEncumbranceLines for PA" + e.getMessage());
    }
  }

  /**
   * 
   * @param bidmgmt
   * @param encline
   * @param com
   */
  public static void updateEncumbranceLineInOrder(Order order, EfinBudgetManencumlines encline,
      AccountingCombination com) {
    List<OrderLine> ln = new ArrayList<OrderLine>();
    try {
      OBQuery<OrderLine> orderlineqry = OBDal.getInstance().createQuery(OrderLine.class,
          " as e where e.eFINUniqueCode.id=:uniquecodeID and "
              + " e.salesOrder.id=:orderID and e.escmIssummarylevel='N'");
      orderlineqry.setNamedParameter("uniquecodeID", com.getId());
      orderlineqry.setNamedParameter("orderID", order.getId());

      log.debug("orderlineqry " + orderlineqry.list().size());
      log.debug("orderlineqry " + orderlineqry.getWhereAndOrderBy());
      if (orderlineqry.list().size() > 0) {
        ln = orderlineqry.list();
        for (OrderLine orderln : ln) {
          orderln.setEfinBudEncumlines(encline);
          OBDal.getInstance().save(orderln);
        }
      }
    } catch (Exception e) {
      log.error("Exception in updateEncumbranceLineInOrder " + e.getMessage());
    }
  }

  // remove use of bid same method
  /**
   * 
   * @param encumbranceline
   * @param decamount
   * @param srcrefline
   */
  public static EfinBudManencumRev insertEncumbranceModification(
      EfinBudgetManencumlines encumbranceline, BigDecimal decamount,
      EfinBudgetManencumlines srcrefline, boolean isbaseOrder) {
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
      manEncumRev.setEncumbranceType("POE");
      if (!isbaseOrder) {
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
   * check validation for manual encumbrance all the uniquceode belongs to same encum.
   * 
   * @param Order
   * @param encumLines
   * @return
   */
  public static boolean checkAllUniquecodesameEncum(Order order) {
    boolean errorFlag = false;
    try {
      OBContext.setAdminMode();
      // checking with requisition line
      OBQuery<OrderLine> rline = OBDal.getInstance().createQuery(OrderLine.class,
          " eFINUniqueCode.id not in(select e.accountingCombination.id from Efin_Budget_Manencumlines "
              + "as e where e.manualEncumbrance.id =:encumID ) and salesOrder.id =:orderID ");
      rline.setNamedParameter("encumID", order.getEfinBudgetManencum().getId());
      rline.setNamedParameter("orderID", order.getId());
      if (rline.list() != null && rline.list().size() > 0) {
        errorFlag = true;
      }
      return errorFlag;
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in checkAllUniquecodesameEncum in Order : ", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * update applied amt in encumbrance
   * 
   * @param objOrder
   * @param encumLines
   */
  public static void updateManualEncumAmount(Order order,
      List<EfinBudgetManencumlines> encumLines) {
    List<OrderLine> orderline = null;
    try {
      OBContext.setAdminMode();
      orderline = order.getOrderLineList();
      // checking with requisition line
      for (OrderLine ordLineup : orderline) {
        if (!ordLineup.isEscmIssummarylevel()) {
          for (EfinBudgetManencumlines enclineup : encumLines) {
            if (ordLineup.getEFINUniqueCode().equals(enclineup.getAccountingCombination())) {
              log.debug("enclineup:" + enclineup.getAPPAmt());
              enclineup.setAPPAmt(enclineup.getAPPAmt().add(ordLineup.getLineNetAmount()));
              ordLineup.setEfinBudEncumlines(enclineup);
              // enclineup.setSysremamt(enclineup.getSysremamt().subtract(reqLineup.getLineNetAmount()));
              log.debug("enclineup:" + enclineup.getAPPAmt());
              OBDal.getInstance().save(ordLineup);
              OBDal.getInstance().save(enclineup);
            }
          }
        }
      }
      if (order.getEfinBudgetManencum().getBusinessPartner() == null) {
        order.getEfinBudgetManencum().setBusinessPartner(order.getBusinessPartner());
      }
      order.setEfinEncumbered(true);
      OBDal.getInstance().save(order);
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in updateManualEncumAmount in Order : ", e);
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
  public static boolean checkFundsNoCostValidation(Order objOrder) {
    boolean error = false;
    try {
      OBContext.setAdminMode();
      Order order = OBDal.getInstance().get(Order.class, objOrder.getId());
      for (OrderLine line : order.getOrderLineList()) {
        if (!line.isEscmIssummarylevel()) {
          OBQuery<AccountingCombination> uniqucode = OBDal.getInstance().createQuery(
              AccountingCombination.class,
              "account.id=:acctID and salesCampaign.efinBudgettype='C' and account.efinFundsonly='N' ");
          uniqucode.setNamedParameter("acctID", line.getEFINUniqueCode().getAccount().getId());
          if (uniqucode.list() != null && uniqucode.list().size() > 0) {
            error = true;
            line.setEfinFailureReason(OBMessageUtils.messageBD("Efin_FundsNoCost_Req"));
            OBDal.getInstance().save(line);
          } else {
            line.setEfinFailureReason("");
            OBDal.getInstance().save(line);
          }
        }
      }
      return error;
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  checkFundsNoCostValidation in Order " + e, e);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return error;
  }

  /**
   * create auto encumbrance and associate in Order
   * 
   * @param objOrder
   */
  public static void insertAutoEncumbrance(Order objOrder) {
    /* Date currentDate = new Date(); */
    Long lineno = 10L;
    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    try {
      OBContext.setAdminMode();
      List<OrderLine> ordLineList = null;
      OBQuery<OrderLine> ordLineQry = OBDal.getInstance().createQuery(OrderLine.class,
          "salesOrder.id=:orderId and escmIssummarylevel='N'");
      ordLineQry.setNamedParameter("orderId", objOrder.getId());
      if (ordLineQry.list() != null && ordLineQry.list().size() > 0) {
        ordLineList = ordLineQry.list();
      }
      AccountingCombination com = ordLineList.get(0).getEFINUniqueCode();
      EfinBudgetManencum encum = OBProvider.getInstance().get(EfinBudgetManencum.class);
      encum.setSalesCampaign(ordLineList.get(0).getEFINUniqueCode().getSalesCampaign());
      encum.setEncumType("POE");
      encum.setSalesRegion(ordLineList.get(0).getEFINUniqueCode().getSalesRegion());
      encum.setEncumMethod("A");
      encum.setEncumStage("POE");
      encum.setOrganization(objOrder.getOrganization());
      log.debug("date" + dateFormat.parse(dateFormat.format(new Date())));
      encum.setAccountingDate(dateFormat.parse(dateFormat.format(new Date())));
      encum.setTransactionDate(dateFormat.parse(dateFormat.format(new Date())));
      encum.setBudgetInitialization(objOrder.getEfinBudgetint());
      encum.setAction("PD");
      encum.setBusinessPartner(objOrder.getBusinessPartner());
      if (objOrder.getEscmNotes() != null)
        encum.setDescription(objOrder.getEscmNotes());
      else
        encum.setDescription(objOrder.getDocumentNo());
      OBDal.getInstance().save(encum);
      OBDal.getInstance().flush();
      for (OrderLine ordLine : ordLineList) {
        OBQuery<EfinBudgetManencumlines> encumlineexists = OBDal.getInstance().createQuery(
            EfinBudgetManencumlines.class,
            "as e where e.manualEncumbrance.id=:encumID and e.accountingCombination.id =:acctID");
        encumlineexists.setNamedParameter("encumID", encum.getId());
        encumlineexists.setNamedParameter("acctID", ordLine.getEFINUniqueCode().getId());

        if (encumlineexists.list() != null && encumlineexists.list().size() > 0) {
          EfinBudgetManencumlines encumLines = encumlineexists.list().get(0);
          encumLines.setAmount(encumLines.getAmount().add(ordLine.getLineNetAmount()));
          encumLines.setRemainingAmount(BigDecimal.ZERO);
          encumLines.setAPPAmt(encumLines.getAPPAmt().add(ordLine.getLineNetAmount()));
          encumLines.setRevamount(encumLines.getRevamount().add(ordLine.getLineNetAmount()));
          ordLine.setEfinBudEncumlines(encumLines);
          OBDal.getInstance().save(ordLine);
          OBDal.getInstance().save(encumLines);
          OBDal.getInstance().flush();
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
          encumLines.setLineNo(lineno);
          encumLines.setAmount(ordLine.getLineNetAmount());
          encumLines.setUsedAmount(BigDecimal.ZERO);
          encumLines.setRemainingAmount(BigDecimal.ZERO);
          encumLines.setFundsAvailable(fundsAvailable);
          encumLines.setAPPAmt(ordLine.getLineNetAmount());
          encumLines.setRevamount(ordLine.getLineNetAmount());
          encumLines.setOrganization(ordLine.getOrganization());
          encumLines.setSalesRegion(ordLine.getEFINUniqueCode().getSalesRegion());
          encumLines.setAccountElement(ordLine.getEFINUniqueCode().getAccount());
          encumLines.setSalesCampaign(ordLine.getEFINUniqueCode().getSalesCampaign());
          encumLines.setProject(ordLine.getEFINUniqueCode().getProject());
          encumLines.setActivity(ordLine.getEFINUniqueCode().getActivity());
          encumLines.setStDimension(ordLine.getEFINUniqueCode().getStDimension());
          encumLines.setNdDimension(ordLine.getEFINUniqueCode().getNdDimension());
          encumLines.setBusinessPartner(ordLine.getEFINUniqueCode().getBusinessPartner());
          encumLines.setAccountingCombination(ordLine.getEFINUniqueCode());
          encumLines.setUniqueCodeName(ordLine.getEFINUniqueCode().getEfinUniquecodename());
          OBDal.getInstance().save(encumLines);
          OBDal.getInstance().flush();
          ordLine.setEfinBudEncumlines(encumLines);
          OBDal.getInstance().save(ordLine);
          lineno += lineno + 10;
        }
      }
      encum.setDocumentStatus("CO");
      OBDal.getInstance().save(encum);
      objOrder.setEfinBudgetManencum(encum);
      objOrder.setEfinEncumbered(true);
      if (objOrder.getEfinBudgetManencum().getBusinessPartner() == null) {
        objOrder.getEfinBudgetManencum().setBusinessPartner(objOrder.getBusinessPartner());
      }
      OBDal.getInstance().save(objOrder);
    } catch (OBException e) {
      log.error(" Exception while insertAutoEncumbrance: " + e);
      throw new OBException(e.getMessage());
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in updateManualEncumAmount in Order : ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Update encum amount in enquiry for auto encum in Order.
   * 
   * @param headerId
   */
  public static void updateAmtInEnquiry(String headerId) {
    String department = "";
    EfinBudgetInquiry allDept = null;
    List<EfinBudgetInquiry> budInqList = null;
    try {
      OBContext.setAdminMode();
      Order order = OBDal.getInstance().get(Order.class, headerId);
      // iterate each line in requisition
      for (OrderLine ordLine : order.getOrderLineList()) {
        if (!ordLine.isEscmIssummarylevel()) {
          OBQuery<EfinBudgetInquiry> budInq = OBDal.getInstance()
              .createQuery(EfinBudgetInquiry.class, "efinBudgetint.id=:budInitID ");
          budInq.setNamedParameter("budInitID", order.getEfinBudgetint().getId());
          if (ordLine.getEFINUniqueCode().isEFINDepartmentFund()) {
            if (budInq.list() != null && budInq.list().size() > 0) {
              budInqList = budInq.list();
              for (EfinBudgetInquiry Enquiry : budInqList) {
                if (ordLine.getEFINUniqueCode() == Enquiry.getAccountingCombination()) {
                  Enquiry.setEncumbrance(Enquiry.getEncumbrance().add(ordLine.getLineNetAmount()));
                  OBDal.getInstance().save(Enquiry);
                  if (Enquiry.getParent() != null) {
                    Enquiry.getParent().setEncumbrance(
                        Enquiry.getParent().getEncumbrance().add(ordLine.getLineNetAmount()));
                    allDept = Enquiry.getParent();
                    OBDal.getInstance().save(Enquiry);
                  }
                  if (allDept.getParent() != null) {
                    allDept.getParent().setEncumbrance(
                        allDept.getParent().getEncumbrance().add(ordLine.getLineNetAmount()));
                    OBDal.getInstance().save(allDept);
                  }
                }
              }
            }
          } else {
            OBQuery<EfinBudgetControlParam> bcp = OBDal.getInstance()
                .createQuery(EfinBudgetControlParam.class, "");
            if (bcp.list() != null && bcp.list().size() > 0) {
              department = bcp.list().get(0).getBudgetcontrolCostcenter().getId();

              OBQuery<AccountingCombination> accountCombination = OBDal.getInstance().createQuery(
                  AccountingCombination.class,
                  "account.id=:acctID and businessPartner.id=:bpartnerID "
                      + " and salesRegion.id=:dept and project.id=:projID "
                      + " and salesCampaign.id=:salesCampID and activity.id=:activityID"
                      + " and stDimension.id=:stDimID and ndDimension.id=:ndDimID and organization.id =:orgID");

              accountCombination.setNamedParameter("acctID",
                  ordLine.getEFINUniqueCode().getAccount().getId());
              accountCombination.setNamedParameter("bpartnerID",
                  ordLine.getEFINUniqueCode().getBusinessPartner().getId());
              accountCombination.setNamedParameter("dept", department);
              accountCombination.setNamedParameter("projID",
                  ordLine.getEFINUniqueCode().getProject().getId());
              accountCombination.setNamedParameter("salesCampID",
                  ordLine.getEFINUniqueCode().getSalesCampaign().getId());
              accountCombination.setNamedParameter("activityID",
                  ordLine.getEFINUniqueCode().getActivity().getId());
              accountCombination.setNamedParameter("stDimID",
                  ordLine.getEFINUniqueCode().getStDimension().getId());
              accountCombination.setNamedParameter("ndDimID",
                  ordLine.getEFINUniqueCode().getNdDimension().getId());
              accountCombination.setNamedParameter("orgID",
                  ordLine.getEFINUniqueCode().getOrganization().getId());

              if (accountCombination.list() != null && accountCombination.list().size() > 0) {
                AccountingCombination combination = accountCombination.list().get(0);

                if (budInq.list() != null && budInq.list().size() > 0) {
                  budInqList = budInq.list();
                  for (EfinBudgetInquiry Enquiry : budInqList) {
                    if (combination == Enquiry.getAccountingCombination()) {
                      Enquiry
                          .setEncumbrance(Enquiry.getEncumbrance().add(ordLine.getLineNetAmount()));
                      OBDal.getInstance().save(Enquiry);
                      if (Enquiry.getParent() != null) {
                        Enquiry.getParent().setEncumbrance(
                            Enquiry.getParent().getEncumbrance().add(ordLine.getLineNetAmount()));
                        OBDal.getInstance().save(Enquiry);
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  updateAmtInEnquiry " + e, e);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * While reject after reserverd role should check its encum used or not.
   * 
   * @param objOrder
   * @return
   */
  public static boolean checkFundsForReject(Order objOrder,
      List<EfinBudgetManencumlines> encumLines) {
    boolean error = true;
    try {
      OBContext.setAdminMode();
      // checking with requisition line
      for (OrderLine ordLineup : objOrder.getOrderLineList()) {
        if (!ordLineup.isEscmIssummarylevel() && encumLines != null) {
          for (EfinBudgetManencumlines enclineup : encumLines) {
            if (ordLineup.getEFINUniqueCode().equals(enclineup.getAccountingCombination())) {
              if (enclineup.getUsedAmount().compareTo(BigDecimal.ZERO) > 0) {
                error = false;
              }
            }
          }
        }
      }
      return error;
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  checkFundsForReject in Order " + e, e);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return error;
  }

  public static boolean checkFundsForCancel(Order objOrder,
      List<EfinBudgetManencumlines> encumLines, OrderLine line) {
    boolean error = false;
    try {
      OBContext.setAdminMode();
      // checking with requisition line
      if (!line.isEscmIssummarylevel()) {
        log.debug("lines:" + line.getEfinBudEncumlines().getUsedAmount());
        if (line.getEfinBudEncumlines().getUsedAmount().compareTo(BigDecimal.ZERO) > 0) {
          error = true;
          line.setEfinFailureReason(OBMessageUtils.messageBD("Efin_Encum_Used_Cannot_Canl"));
          OBDal.getInstance().save(line);
        } else {
          line.setEfinFailureReason(null);
          OBDal.getInstance().save(line);
        }
      }
      return error;
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  checkFundsForReject in Order " + e, e);
      }
    } finally {
    }
    return error;
  }

  /**
   * Update encum amount in enquiry for auto encum in order reject or order cancel.
   * 
   * @param headerId
   * @param iscancel
   * @param lineId
   *          --ord line id
   */
  public static void updateAmtInEnquiryRej(String headerId,
      List<EfinBudgetManencumlines> encumLines, boolean isCancel, String lineId) {
    // String department = "";
    // EfinBudgetInquiry allDept = null;
    // List<EfinBudgetInquiry> budInqList = null;
    try {
      OBContext.setAdminMode();
      BigDecimal amt = BigDecimal.ZERO, amtTemp = BigDecimal.ZERO;
      Order order = OBDal.getInstance().get(Order.class, headerId);
      Order baseOrder = order.getEscmBaseOrder();
      List<OrderLine> baseOrderList;
      BigDecimal diff = BigDecimal.ZERO;
      // iterate each line in requisition
      for (OrderLine ordLine : order.getOrderLineList()) {
        if (!isCancel || (isCancel && ordLine.getId().equals(lineId))) {

          if (!ordLine.isEscmIssummarylevel() && encumLines != null) {
            for (EfinBudgetManencumlines enclineup : encumLines) {
              if (ordLine.getEFINUniqueCode().equals(enclineup.getAccountingCombination())) {
                /*
                 * Trigger changes OBQuery<EfinBudgetInquiry> budInq =
                 * OBDal.getInstance().createQuery( EfinBudgetInquiry.class, "efinBudgetint.id='" +
                 * order.getEfinBudgetint().getId() + "'"); if
                 * (ordLine.getEFINUniqueCode().isEFINDepartmentFund()) { if (budInq.list() != null
                 * && budInq.list().size() > 0) { budInqList = budInq.list(); for (EfinBudgetInquiry
                 * Enquiry : budInqList) { if (ordLine.getEFINUniqueCode() ==
                 * Enquiry.getAccountingCombination()) { Enquiry.setEncumbrance(
                 * Enquiry.getEncumbrance().subtract(ordLine.getLineNetAmount()));
                 * OBDal.getInstance().save(Enquiry); if (Enquiry.getParent() != null) {
                 * Enquiry.getParent().setEncumbrance(Enquiry.getParent().getEncumbrance()
                 * .subtract(ordLine.getLineNetAmount())); allDept = Enquiry.getParent();
                 * OBDal.getInstance().save(Enquiry); } if (allDept.getParent() != null) {
                 * allDept.getParent().setEncumbrance(allDept.getParent().getEncumbrance()
                 * .subtract(ordLine.getLineNetAmount())); OBDal.getInstance().save(allDept); } } }
                 * } } else { OBQuery<EfinBudgetControlParam> bcp = OBDal.getInstance()
                 * .createQuery(EfinBudgetControlParam.class, ""); if (bcp.list() != null &&
                 * bcp.list().size() > 0) { department =
                 * bcp.list().get(0).getBudgetcontrolCostcenter().getId();
                 * 
                 * OBQuery<AccountingCombination> accountCombination = OBDal.getInstance()
                 * .createQuery(AccountingCombination.class, "account.id= '" +
                 * ordLine.getEFINUniqueCode().getAccount().getId() + "'" +
                 * " and businessPartner.id='" +
                 * ordLine.getEFINUniqueCode().getBusinessPartner().getId() + "' " +
                 * "and salesRegion.id='" + department + "' and project.id = '" +
                 * ordLine.getEFINUniqueCode().getProject().getId() + "' " +
                 * "and salesCampaign.id='" + ordLine.getEFINUniqueCode().getSalesCampaign().getId()
                 * + "' " + "and activity.id='" + ordLine.getEFINUniqueCode().getActivity().getId()
                 * + "' and stDimension.id='" + ordLine.getEFINUniqueCode().getStDimension().getId()
                 * + "' " + "and ndDimension.id = '" +
                 * ordLine.getEFINUniqueCode().getNdDimension().getId() + "' " +
                 * "and organization.id = '" + ordLine.getEFINUniqueCode().getOrganization().getId()
                 * + "'");
                 * 
                 * if (accountCombination.list() != null && accountCombination.list().size() > 0) {
                 * AccountingCombination combination = accountCombination.list().get(0);
                 * 
                 * if (budInq.list() != null && budInq.list().size() > 0) { budInqList =
                 * budInq.list(); for (EfinBudgetInquiry Enquiry : budInqList) { if (combination ==
                 * Enquiry.getAccountingCombination()) { Enquiry.setEncumbrance(
                 * Enquiry.getEncumbrance().subtract(ordLine.getLineNetAmount()));
                 * OBDal.getInstance().save(Enquiry); if (Enquiry.getParent() != null) {
                 * Enquiry.getParent().setEncumbrance(Enquiry.getParent()
                 * .getEncumbrance().subtract(ordLine.getLineNetAmount()));
                 * OBDal.getInstance().save(Enquiry); } } } } } } }
                 */
                if (isCancel) {
                  // update encum amt

                  if (baseOrder == null)
                    // insert modification encumbrance if pr cancel.
                    insertEncumbranceModification(enclineup, ordLine);
                  /*
                   * Trigger changes enclineup
                   * .setRevamount(enclineup.getRevamount().subtract(ordLine.getLineNetAmount()));
                   * enclineup.setRemainingAmount(
                   * enclineup.getRemainingAmount().subtract(ordLine.getLineNetAmount()));
                   */
                  amt = enclineup.getAPPAmt().subtract(ordLine.getLineNetAmount());
                  enclineup.setAPPAmt(amt);
                  amtTemp = amtTemp.add(ordLine.getLineNetAmount());
                  if (enclineup.getManualEncumbrance().getAppliedAmount().subtract(amtTemp)
                      .compareTo(BigDecimal.ZERO) == 0 && baseOrder == null)
                    enclineup.getManualEncumbrance().setDocumentStatus("CA");
                  OBDal.getInstance().save(enclineup);
                }
              }
            }
          }
        }
      }

      // for cancel case if there exist a previous version, then update the encumbrance based on
      // the previous version values (Adding new records in the modification tab based on the new
      // and old order)
      if (isCancel) {
        if (baseOrder != null) {
          baseOrderList = baseOrder.getOrderLineList();
          for (OrderLine neworderline : baseOrderList) {
            if (!neworderline.isEscmIssummarylevel() && encumLines != null) {
              EfinBudgetManencumlines encline = neworderline.getEfinBudEncumlines();
              for (OrderLine oldorderline : order.getOrderLineList()) {
                if (neworderline.getLineNo().equals(oldorderline.getLineNo())) {
                  diff = neworderline.getLineNetAmount().subtract(oldorderline.getLineNetAmount());
                  insertEncumbranceModification(encline, diff, null, false);
                  break;
                }
              }
            }
          }
        }
      }

    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  updateAmtInEnquiryRej Rej " + e, e);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * update Applied amount in encumbrance for requisition reject or cancel.
   * 
   * @param objRequisition
   * @param encumLines
   * @param isCancel
   * @param LineId
   */
  public static void updateManualEncumAmountRej(Order objOrder,
      List<EfinBudgetManencumlines> encumLines, boolean isCancel, String LineId) {
    List<OrderLine> objOrderLineList = null;
    try {
      OBContext.setAdminMode();
      objOrderLineList = objOrder.getOrderLineList();
      // checking with requisition line
      for (OrderLine ordLineup : objOrderLineList) {
        if (!isCancel || (isCancel && ordLineup.getId().equals(LineId))) {
          if (!ordLineup.isEscmIssummarylevel()) {
            for (EfinBudgetManencumlines enclineup : encumLines) {
              if (ordLineup.getEFINUniqueCode().equals(enclineup.getAccountingCombination())) {
                enclineup.setAPPAmt(enclineup.getAPPAmt().subtract(ordLineup.getLineNetAmount()));
                if (isCancel) {
                  /*
                   * Trigger changes enclineup.setRevamount(
                   * enclineup.getRevamount().subtract(ordLineup.getLineNetAmount()));
                   * enclineup.setRemainingAmount(
                   * enclineup.getRemainingAmount().subtract(ordLineup.getLineNetAmount()));
                   */
                } else {
                  ordLineup.setEfinBudEncumlines(null);
                }
                // enclineup.setSysremamt(enclineup.getSysremamt().subtract(reqLineup.getLineNetAmount()));
                OBDal.getInstance().save(ordLineup);
                OBDal.getInstance().save(enclineup);

                // inser encumbrance modification if cancel in PR.
                // if (isCancel) {
                // insertEncumbranceModification(enclineup, ordLineup);
                // update amt in inquiry Trigger changes
                /*
                 * EfinEncumbarnceRevision.updateBudgetInquiry(enclineup,
                 * enclineup.getManualEncumbrance(), ordLineup.getLineNetAmount().negate());
                 */
                // }
              }
            }
          }
        }
      }
    } catch (

    final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in updateManualEncumAmountrej in Order : ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static void splitPRforPA(String encumId, EscmBidMgmt bidmgmt, Order order) {
    EfinBudgetManencum newEncumbrance = null, oldEncumbrance = null;
    try {
      // split the PR

      // create Encumbrance
      oldEncumbrance = OBDal.getInstance().get(EfinBudgetManencum.class, encumId);
      // create the Encumbrane
      newEncumbrance = POContractSummaryDAO.insertEncumbranceOrder(order, oldEncumbrance);
      // insert the Encumbrance lines
      POContractSummaryDAO.insertEncumbranceLinesPAOrder(order, newEncumbrance, oldEncumbrance);
      newEncumbrance.setDocumentStatus("CO");
      newEncumbrance.setAction("PD");
      OBDal.getInstance().save(newEncumbrance);
      if (bidmgmt != null) {
        bidmgmt.setEncumbrance(newEncumbrance);
        OBDal.getInstance().save(bidmgmt);
      } else {
        order.setEfinBudgetManencum(newEncumbrance);
        order.setEfinEncumbered(true);
        if (order.getEfinBudgetManencum().getBusinessPartner() == null) {
          order.getEfinBudgetManencum().setBusinessPartner(order.getBusinessPartner());
        }
        OBDal.getInstance().save(order);
      }
    } catch (Exception e) {
      log.error("Exception in splitPR " + e.getMessage());

    }
  }

  /**
   * 
   * @param order
   * @return
   * @throws ParseException
   */
  public static boolean chkFundsAvailforReactOldEncumbrance(Order order, OrderLine objOrderLine)
      throws ParseException {
    boolean errorFlag = false;
    String message = "", Uniquecode = "", tempUniquecode = null;
    List<AccountingCombination> acctcomlist = new ArrayList<AccountingCombination>();
    OBQuery<EfinBudgetInquiry> budInq = null;
    List<String> uniquecodeList = new ArrayList<String>();

    JSONObject uniquecodeResult = new JSONObject(), json = null, UniqueCodejson = null,
        json2 = null;
    JSONArray uniqueCodeListArray = new JSONArray();
    Boolean sameUniqueCode = false;
    Boolean isFullUtilisedPOWhileApprove = false;
    String whereClause = "";
    try {
      OrderLine ordline = null;
      if (objOrderLine != null) {
        ordline = objOrderLine;
        whereClause = " as e where e.sRCManencumline.id in ('"
            + ordline.getEfinBudEncumlines().getId() + "')";

      } else {
        ordline = order.getOrderLineList().get(0);
        whereClause = " as e where e.sRCManencumline.id in ( select e.id from Efin_Budget_Manencumlines e where e.manualEncumbrance.id='"
            + order.getEfinBudgetManencum().getId() + "')";
      }
      message = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Cost");
      Uniquecode = "";

      if (order.getEfinBudgetManencum() != null) {
        OBQuery<EfinBudManencumRev> revQuery = OBDal.getInstance()
            .createQuery(EfinBudManencumRev.class, whereClause);

        if (revQuery.list().size() > 0) {
          for (EfinBudManencumRev rev : revQuery.list()) {
            EfinBudgetManencumlines lines = rev.getManualEncumbranceLines();
            EfinBudgetManencumlines newLine = rev.getSRCManencumline();
            BigDecimal diff = BigDecimal.ZERO;
            BigDecimal amount = BigDecimal.ZERO;
            diff = newLine.getAmount().subtract(rev.getRevamount().negate());
            //
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
                        new BigDecimal(json2.getString("Amount")).add(amount.negate()));
                    sameUniqueCode = true;
                    break;
                  } else
                    continue;
                }
              }
              if (!sameUniqueCode) {
                UniqueCodejson = new JSONObject();
                UniqueCodejson.put("Uniquecode", lines.getAccountingCombination().getId());
                UniqueCodejson.put("Amount", amount.negate());
                uniqueCodeListArray.put(UniqueCodejson);
              }
            }
          }
          uniquecodeResult.put("uniquecodeList", uniqueCodeListArray);

        }

        else {
          errorFlag = POContractSummaryDAO.chkAndUpdateforProposalPRFullQty(order,
              order.getEfinBudgetManencum(), true, true);
          if (errorFlag) {
            isFullUtilisedPOWhileApprove = true;
          }
        }

        if (uniquecodeResult != null && !isFullUtilisedPOWhileApprove
            && uniquecodeResult.has("uniquecodeList")) {
          JSONArray array = uniquecodeResult.getJSONArray("uniquecodeList");
          for (int i = 0; i < array.length(); i++) {
            json = array.getJSONObject(i);
            AccountingCombination acctcom = OBDal.getInstance().get(AccountingCombination.class,
                json.getString("Uniquecode"));
            if (acctcom != null) {
              budInq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
                  "efinBudgetint.id=:budInitID and accountingCombination.id=:acctID");
              budInq.setNamedParameter("budInitID",
                  order.getEfinBudgetManencum().getBudgetInitialization().getId());
              budInq.setNamedParameter("acctID", acctcom.getId());

              log.debug("budInqy:" + budInq.list().size());
              // if isdepartment fund yes, then check dept level distribution acct.
              if (acctcom.isEFINDepartmentFund()) {
                if (budInq.list() != null && budInq.list().size() > 0) {
                  for (

                  EfinBudgetInquiry Enquiry : budInq.list()) {
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
                      "efinBudgetint.id=:budInitID and accountingCombination.id=:acctID ");
                  budInq.setNamedParameter("budInitID",
                      order.getEfinBudgetManencum().getBudgetInitialization().getId());
                  budInq.setNamedParameter("acctID", combination.getId());
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
          ordline.setEfinFailureReason(null);
          OBDal.getInstance().save(ordline);
        }
        if (errorFlag && !isFullUtilisedPOWhileApprove) {
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
          ordline.setEfinFailureReason(message);
          OBDal.getInstance().save(ordline);
        }
      }

    } catch (

    Exception e) {
      log.error("Exception in chkFundsAvailforReactOldEncumbrance " + e.getMessage());
    }
    return errorFlag;
  }

  /**
   * Reactive PO Proposal
   * 
   * @param order
   */
  public static void reactivatePOProposal(Order order) {
    // List<EfinBudgetManencumlines> manenculine = new ArrayList<EfinBudgetManencumlines>();
    // EfinBudgetManencum encumbrance = null;
    try {
      if (order.getEfinBudgetManencum() != null) {
        // fetching revision record based on newly created encumbrance lines
        OBQuery<EfinBudManencumRev> revQuery = OBDal.getInstance()
            .createQuery(EfinBudManencumRev.class, " as e where e.encumbranceType='MO'"
                + "  and e.manualEncumbranceLines.id in ( select e.id from Efin_Budget_Manencumlines e "
                + "where e.manualEncumbrance.id=:encumID) and e.isauto='Y' ");
        revQuery.setNamedParameter("encumID", revQuery);
        if (revQuery.list().size() > 0) {
          for (EfinBudManencumRev rev : revQuery.list()) {
            rev.setSRCManencumline(null);
            OBDal.getInstance().save(rev);
            OBDal.getInstance().flush();
            EfinBudgetManencumlines lines = rev.getManualEncumbranceLines();
            log.debug("getAccountingCombination:" + lines.getAccountingCombination());
            lines.setAPPAmt(lines.getAPPAmt().add(rev.getRevamount().negate()));
            lines.getEfinBudManencumRevList().remove(rev);
          }
        }
        // encumbrance = order.getEfinBudgetManencum();
        // manenculine = encumbrance.getEfinBudgetManencumlinesList();
        order.setEfinEncumbered(false);
        order.getEfinBudgetManencum().setEncumStage("PAE");
        OBDal.getInstance().save(order);
      }
    } catch (Exception e) {
      log.error("Exception in reactivatePOProposal " + e.getMessage());
    }
  }

  /**
   * Method to reactivate the split PR
   * 
   * @param resultEncum
   * @param bidmgmt
   */
  public static void reactivateSplitPR(JSONObject resultEncum, Order order) {
    List<EfinBudgetManencumlines> manenculine = new ArrayList<EfinBudgetManencumlines>();
    EfinBudgetManencum encumbrance = null;
    List<EfinBudManencumRev> revlist = null;
    try {
      // reactivate split the PR

      encumbrance = order.getEfinBudgetManencum();

      if (encumbrance != null) {
        // fetching revision record based on newly created encumbrance lines
        OBQuery<EfinBudManencumRev> revQuery = OBDal.getInstance()
            .createQuery(EfinBudManencumRev.class, " as e where e.sRCManencumline.id in "
                + "( select e.id from Efin_Budget_Manencumlines e where e.manualEncumbrance.id=:encumID)");
        revQuery.setNamedParameter("encumID", encumbrance.getId());
        revlist = revQuery.list();
        if (revlist.size() > 0) {

          // From proposal
          if (order.getEscmProposalmgmt() != null) {

            Map<EfinBudgetManencumlines, Double> proposalLineMap = new HashMap<EfinBudgetManencumlines, Double>();
            if (order.getEscmProposalmgmt().getProposalstatus().equals("AWD")) {
              proposalLineMap = order.getEscmProposalmgmt().getEscmProposalmgmtLineList().stream()
                  .filter(b -> b.getEfinBudgmanencumline() != null)
                  .collect(Collectors.groupingBy(EscmProposalmgmtLine::getEfinBudgmanencumline,
                      Collectors.summingDouble(a -> a.getLineTotal().doubleValue())));
            } else {
              proposalLineMap = order.getEscmProposalmgmt().getEscmProposalmgmtLineList().stream()
                  .filter(b -> b.getEfinBudgmanencumline() != null)
                  .collect(Collectors.groupingBy(EscmProposalmgmtLine::getEfinBudgmanencumline,
                      Collectors.summingDouble(a -> a.getAwardedamount().doubleValue())));
            }

            for (Map.Entry<EfinBudgetManencumlines, Double> entry : proposalLineMap.entrySet()) {

              BigDecimal proposalAmt = BigDecimal.valueOf(entry.getValue());

              EfinBudgetManencumlines oldEncumLines = entry.getKey();

              OBQuery<EfinBudManencumRev> revAmtQuery = OBDal.getInstance().createQuery(
                  EfinBudManencumRev.class,
                  " as e where e.manualEncumbranceLines.id = :proposalEncumLineID and e.issystem = true");
              revAmtQuery.setNamedParameter("proposalEncumLineID", oldEncumLines.getId());

              if (revAmtQuery != null) {
                List<EfinBudManencumRev> revAmtList = revAmtQuery.list();
                if (revAmtList.size() > 0) {

                  BigDecimal revAmtSum = revAmtList.stream().map(a -> (a.getRevamount().negate()))
                      .reduce(BigDecimal.ZERO, BigDecimal::add);

                  if (revAmtSum.compareTo(proposalAmt) > 0) {
                    oldEncumLines.setAPPAmt(oldEncumLines.getAPPAmt().add(proposalAmt));
                    oldEncumLines.setRemainingAmount(
                        oldEncumLines.getRemainingAmount().add(revAmtSum.subtract(proposalAmt)));
                    OBDal.getInstance().save(oldEncumLines);

                  } else {
                    oldEncumLines.setAPPAmt(oldEncumLines.getAPPAmt().add(revAmtSum));
                    OBDal.getInstance().save(oldEncumLines);
                  }

                }
              }
            }
          }

          // From PR
          if (order.getEscmProposalmgmt() == null) {

            Map<EfinBudgetManencumlines, Double> orderLineMap = order.getOrderLineList().stream()
                .filter(b -> b.getEfinBudEncumlines() != null)
                .collect(Collectors.groupingBy(OrderLine::getEfinBudEncumlines,
                    Collectors.summingDouble(a -> a.getLineNetAmount().doubleValue())));

            for (Map.Entry<EfinBudgetManencumlines, Double> entry : orderLineMap.entrySet()) {

              BigDecimal requisitionLineAmt = BigDecimal.ZERO;

              for (OrderLine orderLine : order.getOrderLineList()) {

                if (orderLine.getEfinBudEncumlines() != null
                    && orderLine.getEfinBudEncumlines().getId().equals(entry.getKey().getId())) {
                  for (EscmOrderSourceRef sourceRef : orderLine.getEscmOrdersourceRefList()) {
                    RequisitionLine reqline = sourceRef.getRequisitionLine();
                    requisitionLineAmt = requisitionLineAmt
                        .add(sourceRef.getReservedQuantity().multiply(reqline.getUnitPrice()));
                  }
                }
              }

              BigDecimal revAmtSum = BigDecimal.ZERO;

              EfinBudgetManencumlines newEncumlines = entry.getKey();

              OBQuery<EfinBudManencumRev> revAmtQuery = OBDal.getInstance().createQuery(
                  EfinBudManencumRev.class, " as e where e.sRCManencumline.id = :orderEncumLineID");
              revAmtQuery.setNamedParameter("orderEncumLineID", newEncumlines.getId());

              if (revAmtQuery != null) {
                List<EfinBudManencumRev> revAmtList = revAmtQuery.list();
                if (revAmtList.size() > 0) {

                  Map<EfinBudgetManencumlines, Double> encumLineRevAmtMap = revAmtList.stream()
                      .filter(b -> b.getManualEncumbranceLines() != null)
                      .collect(Collectors.groupingBy(EfinBudManencumRev::getManualEncumbranceLines,
                          Collectors
                              .summingDouble(a -> (a.getRevamount().negate()).doubleValue())));

                  for (Map.Entry<EfinBudgetManencumlines, Double> revision : encumLineRevAmtMap
                      .entrySet()) {

                    EfinBudgetManencumlines oldEncumLines = revision.getKey();

                    revAmtSum = BigDecimal.valueOf(revision.getValue());

                    if (resultEncum.has("type") && resultEncum.getString("type").equals("SPLIT")) {
                      if (revAmtSum.compareTo(requisitionLineAmt) > 0) {
                        oldEncumLines.setAPPAmt(oldEncumLines.getAPPAmt().add(requisitionLineAmt));
                        oldEncumLines.setRemainingAmount(oldEncumLines.getRemainingAmount()
                            .add(revAmtSum.subtract(requisitionLineAmt)));
                        OBDal.getInstance().save(oldEncumLines);

                      } else {
                        oldEncumLines.setAPPAmt(oldEncumLines.getAPPAmt().add(revAmtSum));
                        OBDal.getInstance().save(oldEncumLines);
                      }
                    }
                    if (resultEncum.has("type") && resultEncum.getString("type").equals("MERGE")) {
                      oldEncumLines.setAPPAmt(oldEncumLines.getAPPAmt().add(revAmtSum));
                      OBDal.getInstance().save(oldEncumLines);
                    }
                  }

                }
              }
            }
          }

          // remove new encumbrance line link in old encumbrance modification
          for (EfinBudManencumRev rev : revlist) {
            rev.setSRCManencumline(null);
            OBDal.getInstance().save(rev);
            OBDal.getInstance().flush();
          }

          // delete new encumbrance lines and new encumbrance
          manenculine = encumbrance.getEfinBudgetManencumlinesList();
          encumbrance.setDocumentStatus("DR");
          if (manenculine.size() > 0) {
            for (EfinBudgetManencumlines line : manenculine) {
              OBDal.getInstance().remove(line);
            }
            OBDal.getInstance().remove(encumbrance);
          }
          // remove the modification for old encumbrance and update the app amt for old encumbrance

          for (EfinBudManencumRev rev : revlist) {
            EfinBudgetManencumlines lines = rev.getManualEncumbranceLines();
            // lines.setAPPAmt(lines.getAPPAmt().add(rev.getRevamount().negate()));
            lines.getEfinBudManencumRevList().remove(rev);
          }
        } else {
          POContractSummaryDAO.chkAndUpdateforProposalPRFullQty(order, encumbrance, false, true);
        }
      }

      // update encumbrance line id is null in order
      for (OrderLine ln : order.getOrderLineList()) {
        ln.setEfinBudEncumlines(null);
        OBDal.getInstance().save(ln);
      }

      // remove the encumbrance object in order
      order.setEfinBudgetManencum(null);
      order.setEfinEncumbered(false);
      OBDal.getInstance().save(order);

    } catch (Exception e) {
      log.error("Exception in reactivateSplitPR " + e.getMessage());
    }
  }

  /**
   * 
   * @param resultEncum
   * @param bidmgmt
   */
  public static void updateOldProposalEncum(Order order) {
    try {
      EfinBudgetManencum encum = order.getEscmProposalmgmt().getEfinEncumbrance();
      order.setEfinBudgetManencum(encum);
      OBDal.getInstance().save(order);
      for (OrderLine oLine : order.getOrderLineList()) {
        EfinBudgetManencumlines encLine = oLine.getEscmProposalmgmtLine().getEfinBudgmanencumline();
        oLine.setEfinBudEncumlines(encLine);
        OBDal.getInstance().save(oLine);
      }
    } catch (Exception e) {
      log.error("Exception in updateOldProposalEncum " + e.getMessage());
    }
  }

  /**
   * Insert encum modification while cancel PO.
   * 
   * @param encumbranceline
   * @param decamount
   */
  public static void insertEncumbranceModification(EfinBudgetManencumlines encumbranceline,
      OrderLine orderline) {
    try {
      EfinBudManencumRev manEncumRev = OBProvider.getInstance().get(EfinBudManencumRev.class);
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
      manEncumRev.setRevamount(orderline.getLineNetAmount().negate());
      manEncumRev.setEncumbranceType("POE");
      manEncumRev.setSystem(true);
      manEncumRev.setAccountingCombination(encumbranceline.getAccountingCombination());
      OBDal.getInstance().save(manEncumRev);
    } catch (Exception e) {
      log.error("Exception in insertEncumbranceModification " + e.getMessage());
    }
  }

  /**
   * Insert encum modification Diff while cancel PO.
   * 
   * @param encumbranceline
   * @param decamount
   */
  public static void insertEncumbranceModificationDiff(EfinBudgetManencumlines encumbranceline,
      OrderLine orderline, BigDecimal amount) {
    try {
      EfinBudManencumRev manEncumRev = OBProvider.getInstance().get(EfinBudManencumRev.class);
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
      manEncumRev.setRevamount(amount.negate());
      manEncumRev.setEncumbranceType("POE");
      manEncumRev.setSystem(false);
      manEncumRev.setAccountingCombination(encumbranceline.getAccountingCombination());
      OBDal.getInstance().save(manEncumRev);
      // if (amount.compareTo(BigDecimal.ZERO) < 0) {
      encumbranceline.setAPPAmt(encumbranceline.getAPPAmt().add(amount.negate()));
      OBDal.getInstance().save(encumbranceline);
      // }

    } catch (Exception e) {
      log.error("Exception in insertEncumbranceModification " + e.getMessage());
    }
  }

  /**
   * amt inc or dec case when full qty and full encum and full pr assigned.
   * 
   * @param order
   * @param encumbrance
   * @param isChkFundsAppliedAmt
   * @param isreject
   * @return
   */
  @SuppressWarnings("rawtypes")
  public static Boolean chkAndUpdateforProposalPRFullQty(Order order,
      EfinBudgetManencum encumbrance, Boolean isChkFundsAppliedAmt, Boolean isreject) {
    JSONObject commonvalresult = null;
    Query query = null;
    BigDecimal orderAmt = BigDecimal.ZERO;
    BigDecimal reqAmount = BigDecimal.ZERO, diff = BigDecimal.ZERO, appliedAmt = BigDecimal.ZERO,
        usedAmt = BigDecimal.ZERO, revAmt = BigDecimal.ZERO, unappAmt = BigDecimal.ZERO;
    List<OrderLine> linelist = new ArrayList<OrderLine>();
    Boolean errorFlag = false, errorUnusedFlag = false;
    List<EfinBudgetManencumlines> enclinelist = new ArrayList<EfinBudgetManencumlines>();
    EfinBudgetManencumlines line = null;
    String encumType = null;
    if (encumbrance != null)
      encumType = encumbrance.getEncumMethod();
    try {
      if (isChkFundsAppliedAmt) {
        linelist = order.getOrderLineList();
        for (OrderLine ln : linelist) {
          ln.setEfinFailureReason(null);
          OBDal.getInstance().save(ln);
        }
        linelist = null;
      }
      String prosallineQry = " select sum(ln.linenetamt),ln.em_efin_c_validcombination_id  from c_orderline ln  where ln.c_order_id= ? "
          + " and ln.em_escm_issummarylevel ='N'  group by ln.em_efin_c_validcombination_id  ";
      query = OBDal.getInstance().getSession().createSQLQuery(prosallineQry);
      query.setParameter(0, order.getId());
      log.debug("ordQuery:" + query.toString());
      List queryList = query.list();
      if (queryList != null && queryList.size() > 0) {
        for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          AccountingCombination com = OBDal.getInstance().get(AccountingCombination.class,
              row[1].toString());
          if (row[0] != null)
            orderAmt = new BigDecimal(row[0].toString());

          OBQuery<OrderLine> orderlnqry = OBDal.getInstance().createQuery(OrderLine.class,
              " as e where e.salesOrder.id=:orderId and e.eFINUniqueCode.id=:uniquecodeID ");
          orderlnqry.setNamedParameter("orderId", order.getId());
          orderlnqry.setNamedParameter("uniquecodeID", row[1].toString());
          linelist = orderlnqry.list();

          String reqlnQry = "   select case when coalesce (sum(reqln.qty),0) > 0  "
              + "          then sum(round ((coalesce(reqln.priceactual,0)* coalesce(ref.quantity,0)),2)) else 0 end as amount, reqln.em_efin_c_validcombination_id from escm_ordersource_ref ref "
              + "           join c_orderline ln on ln.c_orderline_id = ref.c_orderline_id and em_escm_issummarylevel = 'N' "
              + "           join m_requisitionline reqln on reqln.m_requisitionline_id = ref.m_requisitionline_id "
              + "          and reqln.em_escm_issummary='N' where ln.c_order_id= ? "
              + "    and reqln.em_efin_c_validcombination_id=  ? group by reqln.em_efin_c_validcombination_id ";
          query = OBDal.getInstance().getSession().createSQLQuery(reqlnQry);
          query.setParameter(0, order.getId());
          query.setParameter(1, row[1].toString());
          log.debug("strQuery:" + query.toString());
          List reqqueryList = query.list();
          if (reqqueryList != null && reqqueryList.size() > 0) {
            for (Iterator reqiterator = reqqueryList.iterator(); reqiterator.hasNext();) {
              Object[] reqrow = (Object[]) reqiterator.next();
              if (reqrow[0] != null) {
                reqAmount = new BigDecimal(reqrow[0].toString());
              }
              diff = orderAmt.subtract(reqAmount);

              OBQuery<EfinBudgetManencumlines> enclineQry = OBDal.getInstance().createQuery(
                  EfinBudgetManencumlines.class,
                  " as e where e.manualEncumbrance.id=:encumID and e.accountingCombination.id=:accID ");
              enclineQry.setNamedParameter("encumID", encumbrance.getId());
              enclineQry.setNamedParameter("accID", row[1].toString());

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
                        for (OrderLine ordline : linelist) {
                          ordline.setEfinFailureReason(commonvalresult.getString("message"));
                          OBDal.getInstance().save(ordline);
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
                        for (OrderLine lines : linelist) {
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
                     * line.setRevamount(line.getRevamount().add(diff));
                     * line.setRemainingAmount(line.getRemainingAmount().add(diff));
                     * OBDal.getInstance().save(line);
                     */
                  }
                }
              }
              // decrease
              else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                // update applied amt and revamount in case of approve both manual and auto (
                // reject
                // case increase is decrease and decrease is increase)
                if (!isChkFundsAppliedAmt) {
                  if (isreject)
                    diff = diff.negate();
                  line.setAPPAmt(line.getAPPAmt().add(diff));
                  if (encumType.equals("A")) {
                    /*
                     * Trigger changes if (!isreject)
                     * line.setENCDecrease(line.getENCDecrease().add(diff.negate()));
                     * line.setRevamount(line.getRevamount().add(diff));
                     * line.setRemainingAmount(line.getRemainingAmount().add(diff));
                     * OBDal.getInstance().save(line);
                     */
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
                        for (OrderLine ordline : linelist) {
                          ordline.setEfinFailureReason(commonvalresult.getString("message"));
                          OBDal.getInstance().save(ordline);
                        }
                      }
                    }
                  } else {
                    // Manual - check while reject applied amount for encumbrance when the time of
                    // decrease
                    if (diff.negate().compareTo(unappAmt) > 0) {
                      errorFlag = true;
                      if (linelist.size() > 0) {
                        for (OrderLine lines : linelist) {
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
                  insertEncumbranceModification(line, diff, null, true);
                // while reject delete the modification
                if (isreject) {
                  diff = diff.negate();
                  ProposalManagementRejectMethods.deleteModification(line, diff);
                }
                // if reject take increase as decrease and decrease as increase to update the
                // budget
                // enquiry
                if (isreject)
                  diff = diff.negate();
                // update budget inquiry Trigger changes
                /*
                 * EfinEncumbarnceRevision.updateBudgetInquiry(line, line.getManualEncumbrance(),
                 * diff);
                 */
              }
            }
          } else {
            if (isChkFundsAppliedAmt && !isreject) {

              if (encumType.equals("A")) {
                commonvalresult = CommonValidationsDAO
                    .CommonFundsChecking(encumbrance.getBudgetInitialization(), com, orderAmt);

                if (commonvalresult.getString("errorFlag").equals("0")) {
                  errorFlag = true;
                  if (linelist.size() > 0) {
                    for (OrderLine prlline : linelist) {
                      prlline.setEfinFailureReason(commonvalresult.getString("message"));
                      OBDal.getInstance().save(prlline);
                    }
                  }
                }
              } else {
                for (OrderLine prlline : linelist) {
                  errorFlag = true;
                  prlline
                      .setEfinFailureReason(OBMessageUtils.messageBD("EFIN_PropNewUniqNotAllow"));
                  OBDal.getInstance().save(prlline);
                }
              }
            } else {
              if (encumType.equals("A") && !isChkFundsAppliedAmt) {
                if (!isreject) {
                  insertEncumbranceLines(encumbrance, orderAmt, com, order);
                } else {
                  // delete the encumbrance lines
                  deleteEncumLines(encumbrance, com, order);
                }
              }
            }
          }
        }
      }

      if (isChkFundsAppliedAmt) {
        if (encumType.equals("A")) {
          if (isreject) {
            errorUnusedFlag = unusedEncumbranceUniquecodeUpdation(order, encumbrance, true, true);
          }
        } else {
          if (isreject) {
            errorUnusedFlag = unusedEncumbranceUniquecodeUpdation(order, encumbrance, true, true);
          }
        }
      } else {
        if (encumType.equals("A")) {
          if (!isreject) {
            unusedEncumbranceUniquecodeUpdation(order, encumbrance, false, false);
          } else {
            unusedEncumbranceUniquecodeUpdation(order, encumbrance, false, true);
          }
        }
      }
      if (errorFlag || errorUnusedFlag) {
        return true;
      }
      return errorFlag;
    } catch (Exception e) {
      log.error("Exception in getUniqueCodeListforFundschk " + e.getMessage());
    }
    return errorFlag;
  }

  /**
   * Method to get unique code List for checking funds
   * 
   * @param order
   * @param encumbrance
   * @return
   */
  @SuppressWarnings("rawtypes")
  public static JSONObject getUniqueCodeListforFundschk(Order order,
      EfinBudgetManencum encumbrance) {
    JSONObject result = new JSONObject(), UniqueCodejson = null, orderLineJson = null,
        uniquecodeResult = new JSONObject(), commonvalresult = null;
    Query query = null;
    BigDecimal orderAmt = BigDecimal.ZERO;
    Boolean isComPresentInReq = false;
    BigDecimal reqAmount = BigDecimal.ZERO, diff = BigDecimal.ZERO;
    List<OrderLine> linelist = new ArrayList<OrderLine>();
    JSONArray linearraylist = new JSONArray(), uniqueCodeListArray = new JSONArray();
    Boolean errorFlag = false;
    try {
      String prosallineQry = "   select sum(ln.linenetamt),ln.em_efin_c_validcombination_id  from c_orderline ln  where ln.c_order_id= ? "
          + "      and ln.em_escm_issummarylevel ='N'  group by ln.em_efin_c_validcombination_id ";
      query = OBDal.getInstance().getSession().createSQLQuery(prosallineQry);
      query.setParameter(0, order.getId());
      log.debug("strQuery:" + query.toString());
      List queryList = query.list();
      if (queryList != null && queryList.size() > 0) {
        for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          isComPresentInReq = false;
          AccountingCombination com = OBDal.getInstance().get(AccountingCombination.class,
              row[1].toString());
          if (row[0] != null)
            orderAmt = new BigDecimal(row[0].toString());
          String reqlnQry = "   select case when coalesce (sum(reqln.qty),0) > 0 "
              + "  then sum(round ((coalesce(reqln.priceactual,0)* coalesce(ref.quantity,0)),2)) else 0 end as amount, reqln.em_efin_c_validcombination_id from escm_ordersource_ref ref "
              + "  join c_orderline ln on ln.c_orderline_id = ref.c_orderline_id and em_escm_issummarylevel = 'N' "
              + "  join m_requisitionline reqln on reqln.m_requisitionline_id = ref.m_requisitionline_id "
              + "  and reqln.em_escm_issummary='N' where ln.c_order_id= ? "
              + "  and reqln.em_efin_c_validcombination_id=  ? group by reqln.em_efin_c_validcombination_id ";
          query = OBDal.getInstance().getSession().createSQLQuery(reqlnQry);
          query.setParameter(0, order.getId());
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
              diff = orderAmt.subtract(reqAmount);
              // increase
              if (diff.compareTo(BigDecimal.ZERO) > 0) {
                UniqueCodejson = new JSONObject();
                UniqueCodejson.put("Uniquecode", row[1].toString());
                UniqueCodejson.put("Amount", diff);
                commonvalresult = CommonValidationsDAO
                    .CommonFundsChecking(encumbrance.getBudgetInitialization(), com, diff);
                if (commonvalresult.getString("errorFlag").equals("0")) {
                  errorFlag = true;
                  OBQuery<OrderLine> orderlnQry = OBDal.getInstance().createQuery(OrderLine.class,
                      " as e where e.salesOrder.id=:orderID and e.eFINUniqueCode.id=:uniquecodeID ");
                  orderlnQry.setNamedParameter("orderID", order.getId());
                  orderlnQry.setNamedParameter("uniquecodeID", row[1].toString());

                  linelist = orderlnQry.list();
                  if (linelist.size() > 0) {
                    for (OrderLine line : linelist) {
                      orderLineJson = new JSONObject();
                      orderLineJson.put("lineId", line.getId());
                      linearraylist.put(orderLineJson);
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
            UniqueCodejson.put("Amount", orderAmt);
            commonvalresult = CommonValidationsDAO
                .CommonFundsChecking(encumbrance.getBudgetInitialization(), com, orderAmt);
            if (commonvalresult.getString("errorFlag").equals("0")) {
              errorFlag = true;
              OBQuery<OrderLine> orderlnQry = OBDal.getInstance().createQuery(OrderLine.class,
                  " as e where e.salesOrder.id=:orderID and e.eFINUniqueCode.id=:uniquecodeID ");
              orderlnQry.setNamedParameter("orderID", order.getId());
              orderlnQry.setNamedParameter("uniquecodeID", row[1].toString());
              linelist = orderlnQry.list();
              if (linelist.size() > 0) {
                for (OrderLine line : linelist) {
                  orderLineJson = new JSONObject();
                  orderLineJson.put("lineId", line.getId());
                  linearraylist.put(orderLineJson);
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
      log.debug("result1:" + result);
    } catch (Exception e) {
      log.error("Exception in getUniqueCodeListforFundschk " + e.getMessage(), e);
    }
    return result;
  }

  @SuppressWarnings("rawtypes")
  public static Boolean unusedEncumbranceUniquecodeUpdation(Order order,
      EfinBudgetManencum manencum, boolean appliedamtchk, boolean isreject) {
    Query query = null;
    EfinBudgetManencumlines line = null;
    Boolean errorFlag = false, sameUniqueCode = false;
    JSONObject commonvalresult = null;
    String message = null;
    JSONObject uniquecodeList = new JSONObject(), uniquecodeJson = null, json1 = null,
        lineListJson = null;
    JSONArray uniqueCodeListArray = new JSONArray(), lineListArray = new JSONArray();
    try {

      if (order.getEfinBudgetManencum() != null
          && order.getEfinBudgetManencum().getEncumMethod().equals("A")) {
        // update amount for unused combination in encumbrance line
        String strQry2 = "   select reqln.em_efin_c_validcombination_id, ln.em_efin_c_validcombination_id as ordcombId,"
            + "  ref.quantity*reqln.priceactual  as reqamt ,ln.linenetamt as orderamt,"
            + "       reqln.em_efin_bud_encumlines_id as reqmanlineid, ln.em_efin_bud_encumlines_id as ordmanlineid , ln.c_orderline_id as orderlineid"
            + "      from c_orderline  ln  join escm_ordersource_ref ref on ref.c_orderline_id= ln.c_orderline_id"
            + "        join m_requisitionline reqln on reqln.m_requisitionline_id= ref.m_requisitionline_id"
            + "      where c_order_id=  ?  and ln.em_escm_issummarylevel='N'  and ln.em_efin_c_validcombination_id is not null "
            + "        and reqln.em_efin_c_validcombination_id <> ln.em_efin_c_validcombination_id "
            + " and reqln.em_efin_c_validcombination_id not in ( select em_efin_c_validcombination_id from c_orderline "
            + "   where  c_order_id= ? ) " + " order by reqln.em_efin_c_validcombination_id";

        query = OBDal.getInstance().getSession().createSQLQuery(strQry2);
        query.setParameter(0, order.getId());
        query.setParameter(1, order.getId());
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
                insertEncumbranceModification(line,
                    new BigDecimal(json1.getString("Amount")).negate(), null, true);
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

  public static EfinBudgetManencumlines insertEncumbranceLines(EfinBudgetManencum encum,
      BigDecimal Amount, AccountingCombination com, Order order) {
    Long lineNo = 0L;
    EfinBudgetManencumlines encumLines = null;
    try {

      // get the next line no based on bid management id
      OBQuery<EfinBudgetManencumlines> ln = OBDal.getInstance().createQuery(
          EfinBudgetManencumlines.class,
          " as e where e.manualEncumbrance.id=:encumID order by e.creationDate desc ");
      ln.setNamedParameter("encumID", encum.getId());
      ln.setMaxResult(1);
      if (ln.list().size() > 0) {
        lineNo = ln.list().get(0).getLineNo();
        lineNo += 10;
      }

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
      OBDal.getInstance().flush();
      OBQuery<OrderLine> prolinQry = OBDal.getInstance().createQuery(OrderLine.class,
          " as e where e.salesOrder.id=:orderID and e.eFINUniqueCode.id=:uniquecodeID ");
      prolinQry.setNamedParameter("orderID", order.getId());
      prolinQry.setNamedParameter("uniquecodeID", com.getId());

      if (prolinQry.list().size() > 0) {
        for (OrderLine line : prolinQry.list()) {
          line.setEfinBudEncumlines(encumLines);
          OBDal.getInstance().save(line);
        }
      }

      // Trigger changes EfinEncumbarnceRevision.updateBudgetInquiry(encumLines,
      // encum, Amount);
      return encumLines;
    }

    catch (

    Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in insertEncumbranceLines " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
    }
    return encumLines;
  }

  public static void deleteEncumLines(EfinBudgetManencum encum, AccountingCombination com,
      Order order) {
    EfinBudgetManencumlines line = null;
    List<EfinBudgetManencumlines> lineList = new ArrayList<EfinBudgetManencumlines>();
    try {

      if (encum != null) {
        OBQuery<EfinBudgetManencumlines> delLineQry = OBDal.getInstance().createQuery(
            EfinBudgetManencumlines.class, " as e where e.manualEncumbrance.id=:encumID "
                + " and e.accountingCombination.id=:accID and e.isauto='Y' ");
        delLineQry.setNamedParameter("encumID", encum.getId());
        delLineQry.setNamedParameter("accID", com.getId());
        delLineQry.setMaxResult(1);
        lineList = delLineQry.list();
        if (lineList.size() > 0) {
          line = lineList.get(0);
          log.debug("line:" + line);
          OBQuery<OrderLine> propsalln = OBDal.getInstance().createQuery(OrderLine.class,
              " as e where e.salesOrder.id=:orderID and  e.efinBudEncumlines.id=:encumLineID ");
          propsalln.setNamedParameter("orderID", order.getId());
          propsalln.setNamedParameter("encumLineID", line.getId());

          if (propsalln.list().size() > 0) {
            for (OrderLine prosalline : propsalln.list()) {
              prosalline.setEfinBudEncumlines(null);
              OBDal.getInstance().save(prosalline);
            }
          }

          encum.getEfinBudgetManencumlinesList().remove(line);
          line.getManualEncumbrance().setDocumentStatus("DR");
          OBDal.getInstance().remove(line);
          // change the status as approved
          encum.setDocumentStatus("CO");
          OBDal.getInstance().save(encum);

        }
        OBDal.getInstance().flush();
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in deleteEncumLines " + e, e);
    }
  }

  // Task No.5925
  public static List<EfinEncControl> getEncControleList(Order order) {
    List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
    Boolean fromPR = false;
    try {
      OBQuery<OrderLine> orderLine = OBDal.getInstance().createQuery(OrderLine.class,
          "salesOrder.id=:orderId and efinMRequisitionline.id is not null");
      orderLine.setNamedParameter("orderId", order.getId());
      if (orderLine.list() != null && orderLine.list().size() > 0) {
        fromPR = true;
      }

      // associated bid - only stage movement then check Purchase encumbrance type is
      // enabled or not
      // or poposal created from purchase requisition
      if ((order.getEscmProposalmgmt() != null
          && order.getEscmProposalmgmt().getEfinEncumbrance() != null
          && order.getEscmProposalmgmt().getEfinEncumbrance().getEncumType() != null
          && order.getEscmProposalmgmt().getEfinEncumbrance().getEncumType().equals("PAE"))) {

        // check proposal encumbrance type is enable or not ..
        OBQuery<EfinEncControl> encumcontrol = OBDal.getInstance().createQuery(EfinEncControl.class,
            " as e where e.encumbranceType='PAE' and e.client.id=:clientID and e.active='Y' ");
        encumcontrol.setNamedParameter("clientID", order.getClient().getId());
        encumcontrol.setFilterOnActive(true);
        encumcontrol.setMaxResult(1);
        enccontrollist = encumcontrol.list();
      }
      // bid is null and proposal created by manual encumbrance or auto and not
      // created by purchase
      // requisition
      else if ((order.getEscmProposalmgmt() == null && order.getEfinBudgetManencum() != null
          && order.getEfinBudgetManencum().getEncumType() != null
          && order.getEfinBudgetManencum().getEncumType().equals("POE"))
          || (order.getEscmProposalmgmt() == null && order.getEfinBudgetManencum() == null
              && !fromPR)) {
        // check po encumbrance type is enable or not ..
        OBQuery<EfinEncControl> encumcontrol = OBDal.getInstance().createQuery(EfinEncControl.class,
            " as e where e.encumbranceType='POE' and e.client.id=:clientID and e.active='Y' ");
        encumcontrol.setNamedParameter("clientID", order.getClient().getId());
        encumcontrol.setFilterOnActive(true);
        encumcontrol.setMaxResult(1);
        enccontrollist = encumcontrol.list();
      } else if (fromPR || (order.getEscmProposalmgmt() != null
          && order.getEscmProposalmgmt().getEfinEncumbrance() != null
          && order.getEscmProposalmgmt().getEfinEncumbrance().getEncumType() != null
          && (order.getEscmProposalmgmt().getEfinEncumbrance().getEncumType().equals("PRE")
              || order.getEscmProposalmgmt().getEfinEncumbrance().getEncumType().equals("BE")))) {

        // check proposal encumbrance type is enable or not ..
        OBQuery<EfinEncControl> encumcontrol = OBDal.getInstance().createQuery(EfinEncControl.class,
            " as e where e.encumbranceType='PRE' and e.client.id=:clientID and e.active='Y' ");
        encumcontrol.setNamedParameter("clientID", order.getClient().getId());
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

  @SuppressWarnings("unchecked")
  public static boolean checkRDVIsCreated(Order order) {
    Boolean isRDVCreated = false;
    String sql = "";
    List<Object> invalidUniqueCodeList = new ArrayList<Object>();
    try {

      if (order.getEscmOldOrder() != null) {
        OBQuery<EfinRDVTransaction> rdvTxnQry = OBDal.getInstance()
            .createQuery(EfinRDVTransaction.class, " as e where e.efinRdv.id in"
                + " ( select a.id from Efin_RDV a  where a.salesOrder.id in ( select ord.id from Order ord where ( ord.id =:orderId or ord.escmBaseOrder.id=:orderId) )"
                + " ) and e.isadvancetransaction='N' ");
        rdvTxnQry.setNamedParameter("orderId", order.getEscmBaseOrder().getId());

        if (rdvTxnQry.list().size() > 0) {
          sql = " select count(ln.c_orderline_id) from c_orderline  ln "
              + " join c_order ord on ord.c_order_id=  ln.c_order_id"
              + " join c_orderline oldordln on oldordln.c_order_id=  ord.em_Escm_old_order and oldordln.line= ln.line "
              + " where ord.c_order_id=?  and ln.em_efin_c_validcombination_id is not null "
              + " and oldordln.em_efin_c_validcombination_id<> ln.em_efin_c_validcombination_id ";
          Query sqlQry = OBDal.getInstance().getSession().createSQLQuery(sql);
          sqlQry.setParameter(0, order.getId());
          invalidUniqueCodeList = sqlQry.list();
          if (invalidUniqueCodeList.size() > 0) {
            Object row = invalidUniqueCodeList.get(0);
            BigInteger count = (BigInteger) row;
            if (count.compareTo(BigInteger.ZERO) > 0) {
              isRDVCreated = true;
            } else {
              isRDVCreated = false;
            }
          }

        } else {
          isRDVCreated = false;
        }
      }

      return isRDVCreated;
    } catch (Exception e) {
      log.error("Exception in checkRDVIsCreated " + e.getMessage());
    }
    return isRDVCreated;
  }

  /**
   * before cancel check the validation (funds avilable) .. if function will return true then stop
   * the cancellation because of funds avilable not sufficient else proceed the cancellation
   * 
   * @param order
   * @param encumbrance
   * @param isChkFundsAppliedAmt
   *          -- if validation then this flag is true , if updation then false
   * @param isCancel
   * @param objordline
   * @return
   */
  public static Boolean chkAndUpdateforOrderCancelPRFullQty(Order order,
      EfinBudgetManencum encumbrance, Boolean isChkFundsAppliedAmt, Boolean isCancel,
      OrderLine objordline) {
    Boolean errorFlag = false;
    List<OrderLine> orderline = new ArrayList<OrderLine>();
    orderline = order.getOrderLineList();
    Boolean alllineCancel = true;
    JSONObject commonvalresult = null;
    BigDecimal diff = BigDecimal.ZERO, orderlinenetAmt = BigDecimal.ZERO;
    try {
      // before cancel check the funds availbale if uniquecode changed between PO
      // lines and PR lines
      if (isChkFundsAppliedAmt) {
        if (orderline.size() > 0) {
          // before check make all the orderline failure reason is null
          for (OrderLine ordline : orderline) {
            ordline.setEfinFailureReason(null);
            OBDal.getInstance().save(ordline);
          }

          for (OrderLine ordline : orderline) {
            EfinBudgetManencumlines lines = ordline.getEfinBudEncumlines();
            if (objordline != null && objordline.equals(ordline) || objordline == null) {

              // fetching the order sourceref record for that orderline
              OBQuery<EscmOrderSourceRef> ordsrcrefQry = OBDal.getInstance().createQuery(
                  EscmOrderSourceRef.class, " as e where  e.salesOrderLine.id=:orderLineID");
              ordsrcrefQry.setNamedParameter("orderLineID", objordline.getId());
              if (ordsrcrefQry.list().size() > 0) {
                for (EscmOrderSourceRef ref : ordsrcrefQry.list()) {
                  // if requistion line encumbranceline id does not match with order line
                  // encumbrance line id ,then uniquecode was changed in orderline
                  // so before cancel check requistion line unqiuecode having funds of orderline
                  // netamount
                  if (ref.getRequisitionLine() != null
                      && ref.getRequisitionLine().getEfinBudEncumlines() != null
                      && !ref.getRequisitionLine().getEfinBudEncumlines().equals(lines)) {
                    commonvalresult = CommonValidationsDAO.CommonFundsChecking(
                        encumbrance.getBudgetInitialization(),
                        ref.getRequisitionLine().getEfinBudEncumlines().getAccountingCombination(),
                        ref.getRequisitionLine().getLineNetAmount());
                    // if unsufficient funds return errorflag as zero;
                    if (commonvalresult.getString("errorFlag").equals("0")) {
                      errorFlag = true;
                      ordline.setEfinFailureReason(commonvalresult.getString("message"));
                      OBDal.getInstance().save(ordline);
                    }
                  }
                  // chk order line netamount and requisition line net amount , if difference is
                  // negative then before cancel need to check funds avilable
                  else if (ref.getRequisitionLine() != null) {
                    log.debug("ordline.getUnitPrice():" + ordline.getLineNetAmount());
                    orderlinenetAmt = ordline.getLineNetAmount();

                    // get the differenece of orderline net amount and requisition line net amount
                    diff = orderlinenetAmt.subtract(ref.getRequisitionLine().getLineNetAmount());
                    // if diff is less than zero , then check funds avilable for diff.negate();
                    if (diff.compareTo(BigDecimal.ZERO) < 0) {
                      commonvalresult = CommonValidationsDAO
                          .CommonFundsChecking(
                              encumbrance.getBudgetInitialization(), ref.getRequisitionLine()
                                  .getEfinBudEncumlines().getAccountingCombination(),
                              diff.negate());
                      if (commonvalresult.getString("errorFlag").equals("0")) {
                        errorFlag = true;
                        ordline.setEfinFailureReason(commonvalresult.getString("message"));
                        OBDal.getInstance().save(ordline);
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
      // make the reflection in modification of encumbrance
      else {
        if (orderline.size() > 0) {
          for (OrderLine ordline : orderline) {
            if (objordline != null && objordline.equals(ordline) || objordline == null) {
              EfinBudgetManencumlines lines = ordline.getEfinBudEncumlines();
              // fetching the order sourceref record for that orderline lines.getRevamount()
              OBQuery<EscmOrderSourceRef> ordsrcrefQry = OBDal.getInstance().createQuery(
                  EscmOrderSourceRef.class, " as e where  e.salesOrderLine.id=:orderLineID ");
              ordsrcrefQry.setNamedParameter("orderLineID", objordline.getId());
              if (ordsrcrefQry.list().size() > 0) {
                for (EscmOrderSourceRef ref : ordsrcrefQry.list()) {
                  if (ref.getRequisitionLine() != null
                      && ref.getRequisitionLine().getEfinCValidcombination() != null
                      && objordline.getEFINUniqueCode() != null
                      && ref.getRequisitionLine().getEfinBudEncumlines() != null) {
                    orderlinenetAmt = ordline.getLineNetAmount();

                    // if orderline uniquecode not equal with requisition line uniquecode
                    if (!ref.getRequisitionLine().getEfinCValidcombination().getId()
                        .equals(objordline.getEFINUniqueCode().getId())) {
                      EfinBudgetManencumlines reqEncumlines = ref.getRequisitionLine()
                          .getEfinBudEncumlines();
                      // delete modification
                      deleteEncumLines(order.getEfinBudgetManencum(),
                          objordline.getEFINUniqueCode(), order);

                      // update applied amount
                      reqEncumlines.setAPPAmt(reqEncumlines.getAPPAmt()
                          .add(ref.getRequisitionLine().getLineNetAmount()));
                      // insert modification
                      insertEncumbranceModification(reqEncumlines,
                          ref.getRequisitionLine().getLineNetAmount(), null, true);
                    } else {
                      // get the differenece of orderline net amount and requisition line net amount
                      if (!orderlinenetAmt.equals(ref.getRequisitionLine().getLineNetAmount())) {
                        diff = orderlinenetAmt
                            .subtract(ref.getRequisitionLine().getLineNetAmount());

                        // update applied amount
                        lines.setAPPAmt(lines.getAPPAmt().add(diff.negate()));
                        // insert modification
                        insertEncumbranceModification(lines, diff.negate(), null, true);
                      }

                    }
                  } else {
                    diff = orderlinenetAmt;
                    // update applied amount
                    lines.setAPPAmt(lines.getAPPAmt().add(diff.negate()));
                    // insert modification
                    insertEncumbranceModification(lines, diff.negate(), null, true);
                  }
                  ordline.setEfinBudEncumlines(null);
                  OBDal.getInstance().save(ordline);
                }
              }
            }
          }
          // check any one of the line having encumbrance line id, if yes then some of the
          // line not
          // yet cancelled
          for (OrderLine ordline : orderline) {
            if (ordline.getEfinBudEncumlines() != null) {
              alllineCancel = false;
              break;
            }
          }
          // if all line cancelled then move the stage to PRE
          if (alllineCancel) {
            encumbrance.setEncumStage("PRE");
            OBDal.getInstance().save(encumbrance);
            // make the business partner as null
            order.getEfinBudgetManencum().setBusinessPartner(null);
            order.setEfinBudgetManencum(null);
            OBDal.getInstance().save(order);
          }
        }
      }

      return errorFlag;
    } catch (Exception e) {
      log.error("Exception in chkAndUpdateforOrderCancelPRFullQty " + e.getMessage());
    }
    return errorFlag;
  }

  /**
   * 
   * @param order
   * @param orderline
   * @return
   * @throws ParseException
   */
  public static boolean chkFundsAvailforCancelOldEncumbrance(Order order, OrderLine orderline)
      throws ParseException {
    boolean errorFlag = false;
    String message = "", Uniquecode = "", tempUniquecode = null;
    List<AccountingCombination> acctcomlist = new ArrayList<AccountingCombination>();
    OBQuery<EfinBudgetInquiry> budInq = null;
    List<String> uniquecodeList = new ArrayList<String>();

    JSONObject uniquecodeResult = new JSONObject(), json = null, UniqueCodejson = null,
        json2 = null;
    JSONArray uniqueCodeListArray = new JSONArray();
    Boolean sameUniqueCode = false;
    BigDecimal diff = BigDecimal.ZERO;
    BigDecimal reqLineNetAmt = BigDecimal.ZERO;
    try {
      OrderLine ordline = orderline;
      message = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Cost");
      Uniquecode = "";
      if (order.getEfinBudgetManencum() != null) {
        OBQuery<EfinBudManencumRev> revQuery = OBDal.getInstance().createQuery(
            EfinBudManencumRev.class,
            " as e where e.sRCManencumline.id in ( select e.id from Efin_Budget_Manencumlines e where e.id=:encumLineID)");
        revQuery.setNamedParameter("encumLineID", orderline.getEfinBudEncumlines().getId());
        if (revQuery.list().size() > 0) {
          for (EfinBudManencumRev rev : revQuery.list()) {
            EfinBudgetManencumlines lines = rev.getManualEncumbranceLines();
            EfinBudgetManencumlines newLine = rev.getSRCManencumline();
            OBQuery<EscmOrderSourceRef> ordsrcrefQry = OBDal.getInstance().createQuery(
                EscmOrderSourceRef.class, " as e where e.salesOrderLine.id=:orderLineID");
            ordsrcrefQry.setNamedParameter("orderLineID", orderline.getId());
            if (ordsrcrefQry.list().size() > 0) {
              for (EscmOrderSourceRef ref : ordsrcrefQry.list()) {
                if (ref.getRequisitionLine() != null
                    && ref.getRequisitionLine().getEfinBudEncumlines() != null
                    && ref.getRequisitionLine().getEfinBudEncumlines().equals(lines)) {
                  reqLineNetAmt = ref.getReservedQuantity()
                      .multiply(ref.getRequisitionLine().getUnitPrice());
                  if (newLine.getAccountingCombination().getId()
                      .equals(lines.getAccountingCombination().getId())) {
                    diff = orderline.getLineNetAmount().subtract(reqLineNetAmt);
                  } else {
                    diff = rev.getRevamount();
                  }
                  diff = diff.negate();
                  //
                  if (UniqueCodejson != null && UniqueCodejson.has("Uniquecode")
                      && diff.compareTo(BigDecimal.ZERO) > 0) {
                    for (int i = 0; i < uniqueCodeListArray.length(); i++) {
                      json2 = uniqueCodeListArray.getJSONObject(i);
                      if (json2.getString("Uniquecode")
                          .equals(lines.getAccountingCombination().getId())) {
                        json2.put("Amount", new BigDecimal(json2.getString("Amount")).add(diff));
                        sameUniqueCode = true;
                        break;
                      } else
                        continue;
                    }
                  }
                  if (!sameUniqueCode) {
                    if ((!lines.getAccountingCombination().equals(ordline.getEFINUniqueCode()))
                        || diff.compareTo(BigDecimal.ZERO) > 0) {
                      UniqueCodejson = new JSONObject();
                      UniqueCodejson.put("Uniquecode", lines.getAccountingCombination().getId());
                      UniqueCodejson.put("Amount", diff);
                      uniqueCodeListArray.put(UniqueCodejson);
                    }
                  }
                }
                uniquecodeResult.put("uniquecodeList", uniqueCodeListArray);
              }
            }
          }
        }

        if (uniquecodeResult != null) {
          JSONArray array = uniquecodeResult.getJSONArray("uniquecodeList");
          for (int i = 0; i < array.length(); i++) {
            json = array.getJSONObject(i);
            AccountingCombination acctcom = OBDal.getInstance().get(AccountingCombination.class,
                json.getString("Uniquecode"));
            if (acctcom != null) {
              budInq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
                  "efinBudgetint.id=:budInitID and accountingCombination.id=:acctID ");
              budInq.setNamedParameter("budInitID",
                  order.getEfinBudgetManencum().getBudgetInitialization().getId());
              budInq.setNamedParameter("acctID", acctcom.getId());

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
                      "efinBudgetint.id=:budInitID and accountingCombination.id=:acctID ");
                  budInq.setNamedParameter("budInitID",
                      order.getEfinBudgetManencum().getBudgetInitialization().getId());
                  budInq.setNamedParameter("acctID", combination.getId());
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
          ordline.setEfinFailureReason(null);
          OBDal.getInstance().save(ordline);
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
          ordline.setEfinFailureReason(message);
          OBDal.getInstance().save(ordline);
        }
      }

    } catch (Exception e) {
      log.error("Exception in chkFundsAvailforReactOldEncumbrance " + e.getMessage());
    }
    return errorFlag;
  }

  /**
   * reactivate split PR/Proposal
   * 
   * @param resultEncum
   * @param order
   * @param isCancel
   * @param ordline
   * @param isProposal
   */
  public static void reactivatelineSplitPR(JSONObject resultEncum, Order order, Boolean isCancel,
      OrderLine ordline, Boolean isProposal, BigDecimal encumAmt) {

    String query = null;
    BigDecimal requisitionLineAmt = BigDecimal.ZERO;

    try {
      // reactivate split the PR
      if (order.getEfinBudgetManencum() != null) {
        // update the new Encumbrance value
        if (ordline.getEfinBudEncumlines() != null) {
          // get orderline encumbrance line id
          EfinBudgetManencumlines canordEncline = ordline.getEfinBudEncumlines();
          if (encumAmt.subtract(ordline.getLineNetAmount()).compareTo(BigDecimal.ZERO) == 0)
            canordEncline.getManualEncumbrance().setDocumentStatus("CA");
          // make the zero for newly created encumbranc while split or merge
          canordEncline
              .setAPPAmt(canordEncline.getAPPAmt().add(ordline.getLineNetAmount().negate()));
          // insert modification (line net amount negative entry for cancellation )
          insertEncumbranceModification(canordEncline, ordline.getLineNetAmount().negate(), null,
              true);
        }

        if (!isProposal) {

          for (EscmOrderSourceRef sourceRef : ordline.getEscmOrdersourceRefList()) {

            RequisitionLine reqline = sourceRef.getRequisitionLine();
            requisitionLineAmt = requisitionLineAmt
                .add(sourceRef.getReservedQuantity().multiply(reqline.getUnitPrice()));
          }
        }
        query = " as e where e.sRCManencumline.id in ( select e.id from Efin_Budget_Manencumlines e where e.id=:encumLineID) ";

        if (ordline != null) {
          query = query + " and ((e.manualEncumbranceLines.manualEncumbrance.encumMethod = 'M' and "
              + " ( e.revamount = :revAmount or e.revamount = :requisitionLineAmt) ) or "
              + " (e.manualEncumbranceLines.manualEncumbrance.encumMethod = 'A' and e.revamount = :requisitionLineAmt)) ";
        }

        // fetching revision record based on newly created encumbrance lines
        OBQuery<EfinBudManencumRev> revQuery = OBDal.getInstance()
            .createQuery(EfinBudManencumRev.class, query);
        revQuery.setNamedParameter("encumLineID", ordline.getEfinBudEncumlines().getId());

        if (ordline != null) {
          revQuery.setNamedParameter("revAmount", ordline.getLineNetAmount().negate());
          revQuery.setNamedParameter("requisitionLineAmt", requisitionLineAmt.negate());
          revQuery.setMaxResult(1);
        }

        if (revQuery.list().size() > 0) {
          for (EfinBudManencumRev rev : revQuery.list()) {
            EfinBudgetManencumlines lines = rev.getManualEncumbranceLines();
            // isProposal flag is false then fetch the order source ref for that orderline
            if (!isProposal) {

              if (!ordline.isEscmIssummarylevel() && ordline.getEFINUniqueCode() != null && ordline
                  .getEFINUniqueCode().getId().equals(lines.getAccountingCombination().getId())) {

                BigDecimal difference = BigDecimal.ZERO;

                if (lines.getManualEncumbrance().getEncumMethod().equals("M")) {

                  if (requisitionLineAmt.compareTo(ordline.getLineNetAmount()) < 0) {
                    difference = ordline.getLineNetAmount().subtract(requisitionLineAmt);
                  }
                  if (requisitionLineAmt.compareTo(rev.getRevamount().negate()) == 0) {
                    lines.setAPPAmt((lines.getAPPAmt().add(rev.getRevamount().negate())));
                    OBDal.getInstance().save(lines);
                  } else {
                    lines.setAPPAmt(
                        (lines.getAPPAmt().add(rev.getRevamount().negate())).subtract(difference));
                    lines.setRemainingAmount(lines.getRemainingAmount().add(difference));
                    OBDal.getInstance().save(lines);
                  }
                } else {
                  lines.setAPPAmt((lines.getAPPAmt().add(rev.getRevamount().negate())));
                  OBDal.getInstance().save(lines);
                }
              }

              OBQuery<EscmOrderSourceRef> ordsrcrefQry = OBDal.getInstance().createQuery(
                  EscmOrderSourceRef.class, " as e where e.salesOrderLine.id=:orderLineID ");
              ordsrcrefQry.setNamedParameter("orderLineID", ordline.getId());
              if (ordsrcrefQry.list().size() > 0) {
                for (EscmOrderSourceRef ref : ordsrcrefQry.list()) {
                  // check revision encumbrance line equal of requisition line encumbrance line id
                  // then make the srcencumbranceline id in rev as null
                  if (ref.getRequisitionLine() != null
                      && ref.getRequisitionLine().getEfinBudEncumlines() != null
                      && ref.getRequisitionLine().getEfinBudEncumlines().equals(lines)) {
                    rev.setSRCManencumline(null);
                    OBDal.getInstance().save(rev);
                    OBDal.getInstance().flush();

                    // revert the app amount for old encumbrance based on rev amount
                    // lines.setAPPAmt(lines.getAPPAmt().add(rev.getRevamount().negate()));
                    // insert modification
                    insertEncumbranceModification(lines, rev.getRevamount().negate(), null, true);
                  }

                }
              }
            }
            // if isProposal flag "true" then
            // else {

            // // revert the app amount for old encumbrance based on rev amount
            // // lines.setAPPAmt(lines.getAPPAmt().add(rev.getRevamount().negate()));
            // // insert modification
            // if (order.getEscmOrdertype() != null && order.getEscmOrdertype().equals("PUR_REL")) {
            // insertEncumbranceModification(lines, rev.getRevamount().negate(), null, false);
            // } else {
            // insertEncumbranceModification(lines, rev.getRevamount().negate(), null, true);
            // }
            // }
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in reactivateSplitPR " + e.getMessage());
    }
  }

  /**
   * check PR fully utilized in PO or not.
   * 
   * @param poOrder
   * @param orderline
   * @return JSonObject - if fullQty used then Jsonobject[isFullQtyUsed]= ture
   */
  public static JSONObject checkStageMovement(Order poOrder, OrderLine orderline) {
    JSONObject result = new JSONObject();
    List<EscmOrderSourceRef> ordlist = new ArrayList<EscmOrderSourceRef>();
    try {

      if (poOrder != null && orderline != null) {
        // fetching order source ref qty for the orderline
        OBQuery<EscmOrderSourceRef> ordsrcrefQry = OBDal.getInstance().createQuery(
            EscmOrderSourceRef.class, " as e where  e.salesOrderLine.id=:orderLineID ");
        ordsrcrefQry.setNamedParameter("orderLineID", orderline.getId());
        ordlist = ordsrcrefQry.list();
        if (ordlist.size() > 0) {
          for (EscmOrderSourceRef ref : ordlist) {
            if (ref.getRequisitionLine() != null) {
              // check requisition encumbrance is same as order encumbrance. if same then
              // fullQtyUsed=true else false
              if (ref.getRequisition().getEfinBudgetManencum() != null && ref.getRequisition()
                  .getEfinBudgetManencum().equals(poOrder.getEfinBudgetManencum())) {
                result.put("isFullQtyUsed", true);
                result.put("encumbrance", poOrder.getEfinBudgetManencum().getId());
                result.put("isAssociatePREncumbrance", true);
              } else if (ref.getRequisition().getEfinBudgetManencum() != null
                  && !ref.getRequisition().getEfinBudgetManencum()
                      .equals(poOrder.getEfinBudgetManencum())) {
                result.put("isFullQtyUsed", false);
                result.put("encumbrance", poOrder.getEfinBudgetManencum().getId());
                result.put("type", "SPLIT");
                result.put("isAssociatePREncumbrance", true);
              }
              // skipped pr encumbrance
              else {
                result.put("isAssociatePREncumbrance", false);
              }
            } else {
              if (poOrder.getEfinBudgetManencum() != null
                  && poOrder.getEfinBudgetManencum().getEncumType().equals("PRE")
                  && poOrder.getEfinBudgetManencum().getEncumStage().equals("POE")) {
                result.put("isFullQtyUsed", true);
                result.put("encumbrance", poOrder.getEfinBudgetManencum().getId());
                result.put("isAssociatePREncumbrance", true);
              } else {
                result.put("isFullQtyUsed", false);
                result.put("encumbrance", poOrder.getEfinBudgetManencum().getId());
                result.put("type", "SPLIT");
                result.put("isAssociatePREncumbrance", true);
              }
            }
          }
        } else {
          if (poOrder.getEfinBudgetManencum() != null
              && poOrder.getEfinBudgetManencum().getEncumType().equals("PRE")
              && poOrder.getEfinBudgetManencum().getEncumStage().equals("POE")) {
            result.put("isFullQtyUsed", true);
            result.put("encumbrance", poOrder.getEfinBudgetManencum().getId());
            result.put("isAssociatePREncumbrance", true);
          } else {
            result.put("isFullQtyUsed", false);
            result.put("encumbrance", poOrder.getEfinBudgetManencum().getId());
            result.put("type", "SPLIT");
            result.put("isAssociatePREncumbrance", true);
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in checkStageMovement " + e.getMessage());
      return result;
    }
    return result;

  }

  /**
   * This method is responsible to do modification in existing encumbrance while reject
   * 
   * @param order
   * @param baseOrder
   */
  @SuppressWarnings("finally")
  public static String doRejectPOVersionMofifcationInEncumbrance(Order order, Order baseOrder,
      boolean iscancel, OrderLine orderLine) {
    EfinBudgetManencum objEncum = order.getEfinBudgetManencum();
    EfinBudgetManencumlines objEncumLine = null;
    OBQuery<OrderLine> oldOrderLineQry = null;
    @SuppressWarnings("unused")
    EfinBudManencumRev manEncumRev = null;
    boolean isbaseorder = true;
    List<OrderLine> orderLineList = new ArrayList<OrderLine>();
    try {
      if (order.getEscmOldOrder() != null) {
        // compare amount between old version and new version order line
        if (orderLine != null) {
          orderLineList = order.getOrderLineList().stream()
              .filter(a -> a.getId().equals(orderLine.getId())
                  && a.getEscmQtycanceled().compareTo(BigDecimal.ZERO) == 0
                  && a.getEscmAmtcanceled().compareTo(BigDecimal.ZERO) == 0)
              .collect(Collectors.toList());
        } else {
          orderLineList = order.getOrderLineList().stream()
              .filter(a -> a.getEscmQtycanceled().compareTo(BigDecimal.ZERO) == 0
                  && a.getEscmAmtcanceled().compareTo(BigDecimal.ZERO) == 0)
              .collect(Collectors.toList());
        }
        for (OrderLine objOrderLine : orderLineList) {
          if (!objOrderLine.isEscmIssummarylevel()) {
            if (objOrderLine.getProduct() != null) {
              oldOrderLineQry = OBDal.getInstance().createQuery(OrderLine.class,
                  "as e where e.salesOrder.id=:orderID and e.product.id=:pdtID"
                      + " and e.eFINUniqueCode.id=:uniqID");
              oldOrderLineQry.setNamedParameter("orderID", order.getEscmOldOrder().getId());
              oldOrderLineQry.setNamedParameter("pdtID", objOrderLine.getProduct().getId());
              oldOrderLineQry.setNamedParameter("uniqID", objOrderLine.getEFINUniqueCode().getId());

            }
            // get the orderline if description is not null
            else if (objOrderLine.getEscmProdescription() != null) {
              oldOrderLineQry = OBDal.getInstance().createQuery(OrderLine.class,
                  "as e where e.salesOrder.id=:orderID and e.escmProdescription=:desc "
                      + " and e.eFINUniqueCode.id=:uniqID ");
              oldOrderLineQry.setNamedParameter("orderID", order.getEscmOldOrder().getId());
              oldOrderLineQry.setNamedParameter("desc", objOrderLine.getEscmProdescription());
              oldOrderLineQry.setNamedParameter("uniqID", objOrderLine.getEFINUniqueCode().getId());

            }
            if (oldOrderLineQry != null && oldOrderLineQry.list().size() > 0) {
              OrderLine oldOrderLine = oldOrderLineQry.list().get(0);
              // get encumbrance details

              OBQuery<EfinBudgetManencumlines> manline = OBDal.getInstance().createQuery(
                  EfinBudgetManencumlines.class,
                  " as e  where e.manualEncumbrance.id=:encumID and e.accountingCombination.id=:acctID ");
              manline.setNamedParameter("encumID", objEncum.getId());
              manline.setNamedParameter("acctID", oldOrderLine.getEFINUniqueCode().getId());
              manline.setMaxResult(1);
              if (manline.list().size() > 0) {
                objEncumLine = manline.list().get(0);
              }

              if (((objOrderLine.getLineNetAmount().compareTo(oldOrderLine.getLineNetAmount()) > 0)
                  || (objOrderLine.getLineNetAmount()
                      .compareTo(oldOrderLine.getLineNetAmount()) < 0))
                  && objEncumLine != null) {
                // do revision with increase or decrease amount
                BigDecimal amount = objOrderLine.getLineNetAmount()
                    .subtract(oldOrderLine.getLineNetAmount());
                // objEncum.setRemainingamt(objEncum.getRemainingamt().add(amount));
                // OBDal.getInstance().save(objEncum);

                if (!StringUtils.isEmpty(objEncumLine.getId())) {

                  if (!iscancel) {
                    // insert into Manual Encumbrance Revision Table
                    ProposalManagementRejectMethods.deleteModification(objEncumLine, amount);
                    // for Proposal management Encumbrance
                    doChangeMofifcationInPOEncumbrance(order, objOrderLine, true);
                  } else {
                    if (order.getEscmBaseOrder() != null && order.isEfinEncumbered()
                        && order.getEscmOldOrder().getEfinBudgetManencum() != null) {
                      isbaseorder = false;
                    }
                    manEncumRev = insertEncumbranceModification(objEncumLine, amount.negate(), null,
                        isbaseorder);
                    // for Proposal management Encumbrance
                    doChangeMofifcationInPOEncumbrance(order, objOrderLine, true);

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
              manline.setNamedParameter("acctID", objOrderLine.getEFINUniqueCode().getId());
              manline.setMaxResult(1);
              if (manline.list().size() > 0) {
                objEncumLine = manline.list().get(0);
              }

              // do revision with increase or decrease amount
              BigDecimal amount = objOrderLine.getLineNetAmount();
              log.info("new amt>" + amount);

              if (!StringUtils.isEmpty(objEncumLine.getId())) {

                if (!iscancel) {
                  // insert into Manual Encumbrance Revision Table
                  ProposalManagementRejectMethods.deleteModification(objEncumLine, amount);
                } else {
                  if (order.getEscmBaseOrder() != null && order.isEfinEncumbered()
                      && order.getEscmOldOrder().getEfinBudgetManencum() != null) {
                    isbaseorder = false;
                  }
                  manEncumRev = insertEncumbranceModification(objEncumLine, amount.negate(), null,
                      isbaseorder);
                }
                objEncumLine.setAPPAmt(objEncumLine.getAPPAmt().add(amount.negate()));
                OBDal.getInstance().save(objEncumLine);
                log.info("new lineencumapp amt>" + objEncumLine.getAPPAmt());

              }

            }
          }

        }
        if (orderLine != null) {
          List<OrderLine> orderLineCancelList = order.getOrderLineList().stream()
              .filter(a -> a.getEscmQtycanceled().compareTo(BigDecimal.ZERO) > 0)
              .collect(Collectors.toList());
          if (orderLineCancelList.size() + 1 == order.getOrderLineList().size()) {
            order.setEfinEncumbered(false);
            OBDal.getInstance().save(order);
          }
        } else {
          order.setEfinEncumbered(false);
          OBDal.getInstance().save(order);
        }

        OBDal.getInstance().save(objEncum);
        OBDal.getInstance().flush();
      }
    } catch (Exception e) {
      // TODO: handle exception
      log.error("Exception while creating encumbrance modification in create new po version", e);
      OBDal.getInstance().rollbackAndClose();
      return "failure";
    } finally {
      return "success";
    }
  }

  /**
   * check manual encumbrance validation for create po new version
   * 
   * @param order
   * @param baseOrder
   * @param appliedamtchk
   * @return
   */
  @SuppressWarnings("unchecked")
  public static boolean chkNewVersionManualEncumbranceValidation(Order order, Order baseOrder,
      boolean appliedamtchk, boolean isreject, OrderLine orderline) {
    EfinBudgetManencum objEncum = order.getEfinBudgetManencum();
    EfinBudgetManencumlines objEncumLine = null;
    boolean errorflag = false;

    String newOrderlineSql = null, oldOrderlineSql = null, encumLineSql = null;
    List<Object> newOrderlineList = new ArrayList<Object>(),
        oldOrderlineList = new ArrayList<Object>(), encumLineList = new ArrayList<Object>();
    LinkedHashMap<String, JSONObject> newLineMap = new LinkedHashMap<String, JSONObject>(),
        oldLineMap = new LinkedHashMap<String, JSONObject>();
    LinkedHashMap<String, JSONObject> encumLineMap = new LinkedHashMap<String, JSONObject>();
    JSONObject json = null, newLineJson = null, oldLineJson = null, encumLineJson = null;
    try {
      if (order.getEscmOldOrder() != null) {
        List<OrderLine> failureReasonList = order.getOrderLineList().stream()
            .filter(a -> a.getEfinFailureReason() != null).collect(Collectors.toList());
        if (failureReasonList.size() > 0) {
          for (OrderLine ordln : failureReasonList) {
            ordln.setEfinFailureReason(null);
            OBDal.getInstance().save(ordln);
          }
        }

        /** fetching new order line -linenet amt group by uniquecode **/
        newOrderlineSql = " select ln.em_efin_c_validcombination_id "
            + ", coalesce(sum(linenetamt),0) as linenetamt from c_order ord "
            + " join c_orderline ln on ln.c_order_id=ord.c_order_id    where ord.c_order_id=? "
            + " and ln.em_escm_issummarylevel='N'  and ln.em_escm_qtycanceled=0 and ln.em_escm_amtcanceled=0   ";
        if (orderline != null) {
          newOrderlineSql += "  and ln.c_orderline_id=? ";
        }
        newOrderlineSql += "  group by ln.em_efin_c_validcombination_id  order by ln.em_efin_c_validcombination_id asc ";
        SQLQuery newLineQry = OBDal.getInstance().getSession().createSQLQuery(newOrderlineSql);
        newLineQry.setParameter(0, order.getId());
        if (orderline != null) {
          newLineQry.setParameter(1, orderline.getId());
        }
        if (newLineQry != null) {
          newOrderlineList = newLineQry.list();

          if (newOrderlineList.size() > 0) {
            for (Object newOrderLnObj : newOrderlineList) {
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
        oldOrderlineSql = " select ln.em_efin_c_validcombination_id "
            + ", coalesce(sum(linenetamt),0) as linenetamt from c_order ord "
            + " join c_orderline ln on ln.c_order_id=ord.c_order_id    where ord.c_order_id=? "
            + " and ln.em_escm_issummarylevel='N' and ln.line in ( select line from c_orderline where c_order_id=?"
            + " and c_orderline.em_escm_qtycanceled=0 and c_orderline.em_escm_amtcanceled=0 ) ";

        if (orderline != null) {
          oldOrderlineSql += "  and ln.line=? ";
        }
        oldOrderlineSql += "  group by ln.em_efin_c_validcombination_id  order by ln.em_efin_c_validcombination_id asc ";

        SQLQuery oldLineQry = OBDal.getInstance().getSession().createSQLQuery(oldOrderlineSql);
        oldLineQry.setParameter(0, order.getEscmOldOrder().getId());
        oldLineQry.setParameter(1, order.getId());
        if (orderline != null) {
          oldLineQry.setParameter(2, orderline.getLineNo());
        }
        if (oldLineQry != null) {
          oldOrderlineList = oldLineQry.list();
          if (oldOrderlineList.size() > 0) {
            for (Object oldOrderLnObj : oldOrderlineList) {
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
                      updateFailureReason(newLineObjMap.getKey(), true, order);
                      errorflag = true;
                    }
                  } else {
                    // if uniquecode not present in encumbrance lines throw error
                    updateFailureReason(newLineObjMap.getKey(), false, order);
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
                      updateFailureReason(newLineObjMap.getKey(), true, order);
                      errorflag = true;
                    }
                  } else {
                    // if uniquecode not present in encumbrance lines throw error
                    updateFailureReason(newLineObjMap.getKey(), false, order);
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
                    updateFailureReason(oldLineObjMap.getKey(), true, order);
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
            if (orderline != null) {
              List<OrderLine> orderLineList = order.getOrderLineList().stream()
                  .filter(a -> a.getEscmQtycanceled().compareTo(BigDecimal.ZERO) > 0)
                  .collect(Collectors.toList());
              if (orderLineList.size() + 1 == order.getOrderLineList().size()) {
                order.setEfinEncumbered(false);
                OBDal.getInstance().save(order);
              }
            } else {
              order.setEfinEncumbered(false);
              OBDal.getInstance().save(order);
            }
          } else {
            order.setEfinEncumbered(true);
            OBDal.getInstance().save(order);
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
      Order objOrder) {
    List<OrderLine> orderlineList = new ArrayList<OrderLine>();
    try {

      OBQuery<OrderLine> orderLineQry = OBDal.getInstance().createQuery(OrderLine.class,
          " as e where e.salesOrder.id=:orderId"
              + " and e.eFINUniqueCode.id=:uniqueCodeId and e.escmIssummarylevel='N' ");
      orderLineQry.setNamedParameter("orderId", objOrder.getId());
      orderLineQry.setNamedParameter("uniqueCodeId", uniqueCodeId);
      orderlineList = orderLineQry.list();
      if (orderlineList.size() > 0) {
        for (OrderLine orderLineObj : orderlineList) {
          if (isUniqueCodePresent) {
            orderLineObj.setEfinFailureReason(OBMessageUtils.messageBD("Efin_ReqAmt_More"));
          } else {
            orderLineObj.setEfinFailureReason("uniquecode is not present in encumbrance lines");
          }
          OBDal.getInstance().save(orderLineObj);
        }
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in updateFailureReason " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
    }
  }

  public static JSONObject getUniquecodeListforPOVerAuto(Order order, Order baseOrder,
      boolean isreject, OrderLine orderline) {
    JSONObject result = new JSONObject(), json = null, json1 = null;
    JSONArray arraylist = new JSONArray(), linearraylist = null;
    EfinBudgetManencum objEncum = order.getEfinBudgetManencum();
    EfinBudgetManencumlines objEncumLine = null;
    List<OrderLine> newOrderLineVerList = new ArrayList<OrderLine>();
    OBQuery<OrderLine> oldOrderLineQry = null;
    BigDecimal amount = BigDecimal.ZERO;
    String whereclause = "";
    try {
      if (order.getEscmOldOrder() != null) {
        whereclause = " as e where e.eFINUniqueCode is not null and e.salesOrder.id=:orderId and e.escmIssummarylevel ='N'  ";
        if (orderline != null) {
          whereclause += " and e.id=:orderlineId ";
        }
        whereclause += "  order by  e.eFINUniqueCode,e.lineNo asc ";

        OBQuery<OrderLine> neworderlineQry = OBDal.getInstance().createQuery(OrderLine.class,
            whereclause);
        neworderlineQry.setNamedParameter("orderId", order.getId());
        if (orderline != null) {
          neworderlineQry.setNamedParameter("orderlineId", orderline.getId());
        }
        newOrderLineVerList = neworderlineQry.list();

        // compare amount between old version and new version order line
        for (OrderLine objOrderLine : newOrderLineVerList) {
          objOrderLine.setEfinFailureReason(null);
          OBDal.getInstance().save(objOrderLine);

          log.debug("desc:" + objOrderLine.getDescription());
          // get the orderline if product id is not null
          if (objOrderLine.getProduct() != null) {
            oldOrderLineQry = OBDal.getInstance().createQuery(OrderLine.class,
                "as e where e.salesOrder.id=:oldOrderId and e.product.id=:productID "
                    + " and e.eFINUniqueCode.id=:uniquecodeID and e.lineNo=:lineno "
                    + " and e.escmQtycanceled=0 and e.escmAmtcanceled=0 ");

            oldOrderLineQry.setNamedParameter("oldOrderId", order.getEscmOldOrder().getId());
            oldOrderLineQry.setNamedParameter("productID", objOrderLine.getProduct().getId());
            oldOrderLineQry.setNamedParameter("uniquecodeID",
                objOrderLine.getEFINUniqueCode().getId());
            oldOrderLineQry.setNamedParameter("lineno", objOrderLine.getLineNo());
          }
          // get the orderline if description is not null
          else {
            oldOrderLineQry = OBDal.getInstance().createQuery(OrderLine.class,
                "as e where e.salesOrder.id=:oldOrderId and e.escmProdescription=:desc "
                    + " and e.eFINUniqueCode.id=:uniquecodeID and e.lineNo=:lineno "
                    + " and e.escmQtycanceled=0 and e.escmAmtcanceled=0 ");

            oldOrderLineQry.setNamedParameter("oldOrderId", order.getEscmOldOrder().getId());
            oldOrderLineQry.setNamedParameter("desc", objOrderLine.getEscmProdescription());
            oldOrderLineQry.setNamedParameter("uniquecodeID",
                objOrderLine.getEFINUniqueCode().getId());
            oldOrderLineQry.setNamedParameter("lineno", objOrderLine.getLineNo());
            log.debug("desc:" + oldOrderLineQry.getWhereAndOrderBy());
          }
          if (oldOrderLineQry.list().size() > 0) {
            OrderLine oldOrderLine = oldOrderLineQry.list().get(0);
            // get encumbrance details
            OBQuery<EfinBudgetManencumlines> manline = OBDal.getInstance().createQuery(
                EfinBudgetManencumlines.class,
                " as e where e.manualEncumbrance.id=:encumID and e.accountingCombination.id=:accID ");
            manline.setNamedParameter("encumID", objEncum.getId());
            manline.setNamedParameter("accID", oldOrderLine.getEFINUniqueCode().getId());

            manline.setMaxResult(1);
            if (manline.list().size() > 0) {
              objEncumLine = manline.list().get(0);
            }
            if (((objOrderLine.getLineNetAmount().compareTo(oldOrderLine.getLineNetAmount()) > 0)
                || (objOrderLine.getLineNetAmount().compareTo(oldOrderLine.getLineNetAmount()) < 0))
                && objEncumLine != null) {
              // do revision with increase or decrease amount
              amount = objOrderLine.getLineNetAmount().subtract(oldOrderLine.getLineNetAmount());
              if (isreject) {
                amount = amount.negate();
              }
              // json = new JSONObject();
              if (json != null && json.has("Uniquecode") && json.getString("Uniquecode")
                  .equals(objOrderLine.getEFINUniqueCode().getId())) {
                json.put("Amount", new BigDecimal(json.getString("Amount")).add(amount));
                json1 = new JSONObject();
                json1.put("lineId", objOrderLine.getId());
                linearraylist.put(json1);
                json.put("lineList", linearraylist);
              } else {
                if (json != null)
                  json.put("lineList", linearraylist);
                linearraylist = new JSONArray();
                json = new JSONObject();
                json.put("Uniquecode", objOrderLine.getEFINUniqueCode().getId());
                json.put("Amount", amount);
                json.put("isSummary", objOrderLine.isEscmIssummarylevel());
                json1 = new JSONObject();
                json1.put("lineId", objOrderLine.getId());
                linearraylist.put(json1);
                arraylist.put(json);
              }
            }
          } else {
            // do revision with increase or decrease amount
            amount = objOrderLine.getLineNetAmount();
            // json = new JSONObject();
            if (json != null && json.has("Uniquecode")
                && json.getString("Uniquecode").equals(objOrderLine.getEFINUniqueCode().getId())) {
              json.put("Amount", new BigDecimal(json.getString("Amount")).add(amount));
              json1 = new JSONObject();
              json1.put("lineId", objOrderLine.getId());
              linearraylist.put(json1);
              json.put("lineList", linearraylist);
            } else {
              if (json != null)
                json.put("lineList", linearraylist);
              linearraylist = new JSONArray();
              json = new JSONObject();
              json.put("Uniquecode", objOrderLine.getEFINUniqueCode().getId());
              json.put("Amount", amount);
              json.put("isSummary", objOrderLine.isEscmIssummarylevel());
              json1 = new JSONObject();
              json1.put("lineId", objOrderLine.getId());
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
        log.error("Exception in chkManualEncumbranceValidation " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
      return result;
    }
    return result;
  }

  /**
   * check and validate duplicate version
   * 
   * @param orderId
   * @return true, if has duplicate
   */
  public static boolean checkDuplicateVersion(String orderId) {
    String query = " as e where e.escmOldOrder.id=? and e.escmAppstatus<>'ESCM_CA' ";
    List<Order> ordLs = null;
    List<Object> parametersList = new ArrayList<Object>();
    parametersList.add(orderId);

    try {
      OBContext.setAdminMode();
      OBQuery<Order> ord = OBDal.getInstance().createQuery(Order.class, query, parametersList);
      ordLs = ord.list();

      if (ordLs.size() > 0) {
        return true;
      }
    } catch (OBException e) {
      log.error("Exception while checkDuplicateVersion:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return false;
  }

  /**
   * Get 10 % of amount from the Base Order Total Amount and validate with revisioned Order Total
   * Amt for allowing to create new version
   * 
   * @param orderId
   * @return true, if has duplicate
   */
  public static boolean checkTotalAmtUtilized(String docNo) {
    BigDecimal baseTotAmt = BigDecimal.ZERO;
    BigDecimal allowedAmt = BigDecimal.ZERO;
    BigDecimal revAmtDiffFromBase = BigDecimal.ZERO;

    String query = " select grandtotal from c_order where documentno=? and em_escm_old_order is null ";
    String revQuery = " select grandtotal from c_order where documentno =? and em_escm_old_order is not null ";
    try {
      OBContext.setAdminMode();

      SQLQuery baseOrdLs = OBDal.getInstance().getSession().createSQLQuery(query);
      baseOrdLs.setParameter(0, docNo);
      if (baseOrdLs != null) {
        if (baseOrdLs.list().size() > 0) {
          Object row = baseOrdLs.list().get(0);
          baseTotAmt = (BigDecimal) row;
          allowedAmt = (new BigDecimal("10").divide(new BigDecimal("100"))).multiply(baseTotAmt);
        }
      }

      SQLQuery revOrdLs = OBDal.getInstance().getSession().createSQLQuery(revQuery);
      revOrdLs.setParameter(0, docNo);
      if (revOrdLs.list() != null && revOrdLs.list().size() > 0) {
        @SuppressWarnings("unchecked")
        List<Object> rows = revOrdLs.list();
        for (Object row : rows) {
          BigDecimal grandTot = (BigDecimal) row;
          revAmtDiffFromBase = (grandTot.subtract(baseTotAmt)).add(revAmtDiffFromBase);
        }
      }
      log.debug("baseTotAmt>" + baseTotAmt);
      log.debug("allowedAmt>" + allowedAmt);
      log.debug("revAmtDiffFromBase>" + revAmtDiffFromBase);
      // allowedAmt = allowedAmt.add(baseTotAmt);
      if (allowedAmt.compareTo(revAmtDiffFromBase) < 0
          || allowedAmt.compareTo(revAmtDiffFromBase) == 0) {
        return true;
      }
    } catch (OBException e) {
      log.error("Exception while checkTotalAmtUtilized:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return false;
  }

  /**
   * method to insert approval history in the specified tables
   * 
   * @param data
   *          {@link JSONObject} containing the data like the approval action and the next performer
   *          etc.
   * @return The count of the inserted lines.
   */
  public static int purchaseOrderApprovalHistory(JSONObject data) {
    int count = 0;

    try {
      StringBuilder queryBuilder = new StringBuilder();
      String historyId = SequenceIdData.getUUID();
      String strTableName = data.getString("HistoryTable");

      queryBuilder.append(" INSERT INTO  ").append(strTableName);
      queryBuilder.append(" ( ").append(strTableName.concat("_id"))
          .append(", ad_client_id, ad_org_id,");
      queryBuilder.append(" createdby, updatedby,   ").append(data.getString("HeaderColumn"))
          .append(" , approveddate, ");
      queryBuilder.append(" comments, ").append(data.getString("ActionColumn"))
          .append(" , pendingapproval, seqno,ad_role_id,revision)");
      queryBuilder.append(" VALUES (?, ?, ?, ");
      queryBuilder.append(" ?, ?,?, ?,");
      queryBuilder.append(" ?, ?, ?, ?,?,?);");

      PreparedStatement query = OBDal.getInstance().getConnection()
          .prepareStatement(queryBuilder.toString());

      query.setString(1, historyId);
      query.setString(2, data.getString("ClientId"));
      query.setString(3, data.getString("OrgId"));
      query.setString(4, data.getString("UserId"));
      query.setString(5, data.getString("UserId"));
      query.setString(6, data.getString("HeaderId"));
      query.setDate(7, new java.sql.Date(System.currentTimeMillis()));
      query.setString(8, data.getString("Comments"));
      query.setString(9, data.getString("Status"));
      query.setString(10, data.optString("NextApprover"));
      query.setInt(11, getHistorySequence(strTableName, data.getString("HeaderColumn"),
          data.getString("HeaderId")));
      query.setString(12, data.optString("RoleId"));
      query.setLong(13, data.getLong("Revision"));
      log.debug("History Query: " + query.toString());

      count = query.executeUpdate();
    } catch (Exception e) {
      count = 0;
      log.error("Exception while purchaseOrderApprovalHistory(): ", e);
    }
    return count;
  }

  /**
   * Method to get the max sequenceno from the history table.
   * 
   * @param tableName
   *          Name of the history table.
   * @param headerColumn
   *          ID column name of the header table.
   * @param headerId
   *          Column value of the header column
   * @return returns the maximum sequence number.
   */

  @SuppressWarnings("unchecked")
  private static int getHistorySequence(String tableName, String headerColumn, String headerId) {
    int sequence = 10;
    try {
      StringBuilder queryBuilder = new StringBuilder();

      queryBuilder.append(" select seqno from ").append(tableName);
      queryBuilder.append(" where ").append(headerColumn).append(" ='").append(headerId)
          .append("' order by created desc ");

      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(queryBuilder.toString());
      query.setMaxResults(1);

      log.debug("Sequence Query: " + query.getQueryString());

      if (query != null) {
        List<Object> rows = query.list();
        if (rows.size() > 0) {
          sequence = Integer.parseInt(rows.get(0).toString()) + 10;
        }
      }
      log.debug("Sequence Number: " + sequence);
    } catch (Exception e) {
      log.error("Exception while getHistorySequence(): ", e);
    }
    return sequence;
  }

  /**
   * Check document type matches with PO Configuration document type based on value set
   * 
   * @param clientId
   * @param orgId
   * @param docVal
   * @param docType
   * @return true
   */
  public static boolean checkDocTypeConfig(String clientId, String orgId, BigDecimal docVal,
      String docType) {
    String query = " as cfig where cfig.minValue<=? and cfig.organization.id=? and cfig.client.id=? order by cfig.minValue desc ";
    List<Object> parametersList = new ArrayList<Object>();
    parametersList.add(docVal);
    parametersList.add(orgId);
    parametersList.add(clientId);
    OBQuery<EscmPurchaseOrderConfiguration> cfigLs = null;
    try {
      OBContext.setAdminMode();
      cfigLs = OBDal.getInstance().createQuery(EscmPurchaseOrderConfiguration.class, query,
          parametersList);
      cfigLs.setMaxResult(1);
      if (cfigLs != null && cfigLs.list().size() > 0) {
        EscmPurchaseOrderConfiguration cfigVal = cfigLs.list().get(0);
        // check doc type are equal
        if (!docType.equals(cfigVal.getOrdertype())) {
          return false;
        }
      } else {
        parametersList.clear();
        parametersList.add(docVal);
        parametersList.add("0");
        parametersList.add(clientId);
        cfigLs = OBDal.getInstance().createQuery(EscmPurchaseOrderConfiguration.class, query,
            parametersList);
        if (cfigLs != null && cfigLs.list().size() > 0) {
          EscmPurchaseOrderConfiguration cfigVal = cfigLs.list().get(0);
          // check doc type are equal
          if (!docType.equals(cfigVal.getOrdertype())) {
            return false;
          }
        }
      }
    } catch (OBException e) {
      log.error("Exception while checkDocTypeConfig:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return true;
  }

  /**
   * Get tolerance value with key based on PO document type to validate with PO document value
   * 
   * @param documentType
   * @param clientId
   * @return Map
   */
  public static Map<String, String> getToleranceValue(String documentType, String clientId) {
    List<Object> params = new ArrayList<Object>();
    Map<String, String> map = new HashMap<String, String>();
    try {
      String query = " as lokupln left join lokupln.escmDeflookupsType lokup "
          + " where lokup.reference ='POTOL' and lokup.client.id =? ";
      if ("PUR".equals(documentType)) {
        query = query
            + " and TRIM(lokupln.searchKey) in ('PO_STD_Max_Increase', 'PO_STD_Max_Decrease')";
      } else if ("CR".equals(documentType)) {
        query = query
            + " and TRIM(lokupln.searchKey) in ('Contract_Max_Increase', 'Contract_Max_Decrease')";
      }
      params.add(clientId);
      OBQuery<ESCMDefLookupsTypeLn> lookupLn = OBDal.getInstance()
          .createQuery(ESCMDefLookupsTypeLn.class, query);
      lookupLn.setParameters(params);
      if (lookupLn.list().size() > 0) {
        for (ESCMDefLookupsTypeLn tolLookup : lookupLn.list()) {
          map.put(tolLookup.getSearchKey(), tolLookup.getToleranceValue());
          log.debug(tolLookup.getSearchKey() + "=" + tolLookup.getToleranceValue());
        }
      }
    } catch (Exception e) {
      log.error("Exception while getToleranceValue:" + e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return map;
  }

  /**
   * Get tolerance % value and Document value from the Base Order and validate with revisioned Total
   * value for allowing to create new version
   * 
   * @param docNo
   * @param isSubmitPO
   *          - to check submit or create new version process
   * @param documentType
   * @param clientId
   * @return true
   */
  @SuppressWarnings("rawtypes")
  public static boolean checkTotalAmtUtilized(String docNo, String isSubmitPO, String documentType,
      String clientId, BigDecimal revTotalValue, Order objOrder) {
    BigDecimal baseTotAmt = BigDecimal.ZERO;
    BigDecimal allowedIncAmt = BigDecimal.ZERO;
    BigDecimal allowedDecAmt = BigDecimal.ZERO;
    BigDecimal revIncAmtDiffFromBase = BigDecimal.ZERO;
    BigDecimal revDecAmtDiffFromBase = BigDecimal.ZERO;
    String toleranceInc = "0";
    String toleranceDec = "0";

    String query = " select grandtotal from c_order where documentno=? and c_order_id=? ";
    // String revQuery = " select grandtotal from c_order where documentno =? and em_escm_old_order
    // is not null order by em_escm_revision ";
    try {
      OBContext.setAdminMode();
      Map<String, String> map = getToleranceValue(documentType, clientId);
      for (Map.Entry m : map.entrySet()) {
        if ("PO_STD_Max_Increase".equals(m.getKey())
            || "Contract_Max_Increase".equals(m.getKey())) {
          toleranceInc = (String) m.getValue();
        }
        if ("PO_STD_Max_Decrease".equals(m.getKey())
            || "Contract_Max_Decrease".equals(m.getKey())) {
          toleranceDec = (String) m.getValue();
        }
      }
      log.debug("Lookup value>" + toleranceInc + "///" + toleranceDec);
      // Get Allowed inc and dec value from base value
      SQLQuery baseOrdLs = OBDal.getInstance().getSession().createSQLQuery(query);
      // changes for LegacyMigration
      // if (objOrder.getEscmLegacycontract() == null) {
      baseOrdLs.setParameter(0, docNo);
      // } else {
      // baseOrdLs.setParameter(0, objOrder.getEscmBaseOrder().getDocumentNo());
      // }
      baseOrdLs.setParameter(1,
          objOrder.getEscmBaseOrder() != null ? objOrder.getEscmBaseOrder().getId()
              : objOrder.getId());
      if (baseOrdLs != null) {
        if (baseOrdLs.list().size() > 0) {
          Object row = baseOrdLs.list().get(0);
          baseTotAmt = (BigDecimal) row;
          allowedIncAmt = (new BigDecimal(toleranceInc).divide(new BigDecimal("100")))
              .multiply(baseTotAmt);
          allowedDecAmt = (new BigDecimal(toleranceDec).divide(new BigDecimal("100")))
              .multiply(baseTotAmt);
        }
      }
      if (revTotalValue.compareTo(baseTotAmt) > 0)
        revIncAmtDiffFromBase = revTotalValue.subtract(baseTotAmt);
      else if (revTotalValue.compareTo(baseTotAmt) < 0)
        revDecAmtDiffFromBase = baseTotAmt.subtract(revTotalValue);
      /*
       * // Get how much amt is revised from base amt SQLQuery revOrdLs =
       * OBDal.getInstance().getSession().createSQLQuery(revQuery); revOrdLs.setParameter(0, docNo);
       * if (revOrdLs.list() != null && revOrdLs.list().size() > 0) { List<Object> rows =
       * revOrdLs.list(); for (Object row : rows) { BigDecimal revisedTotAmt = (BigDecimal) row;
       * log.debug("revisedTotAmt>" + revisedTotAmt); if (revisedTotAmt.compareTo(baseTotAmt) > 0) {
       * revIncAmtDiffFromBase = ((revisedTotAmt.subtract(baseTotAmt)).abs())
       * .add(revIncAmtDiffFromBase); } if (revisedTotAmt.compareTo(baseTotAmt) < 0) {
       * revDecAmtDiffFromBase = ((baseTotAmt.subtract(revisedTotAmt)).abs())
       * .add(revDecAmtDiffFromBase); } } }
       */
      log.debug("baseTotAmt>" + baseTotAmt);
      log.debug("AllowedValue>" + allowedIncAmt + "///" + allowedDecAmt);
      // log.debug("revAmtDiffFromBase>" + revIncAmtDiffFromBase + "////" + revDecAmtDiffFromBase);
      /*
       * if ("N".equals(isSubmitPO)) { if (allowedIncAmt.compareTo(revIncAmtDiffFromBase) <= 0 &&
       * allowedDecAmt.compareTo(revDecAmtDiffFromBase) <= 0) { return true; } } else if
       * (allowedIncAmt.compareTo(revIncAmtDiffFromBase) < 0 ||
       * allowedDecAmt.compareTo(revDecAmtDiffFromBase) < 0) { return true; }
       */

      if ("N".equals(isSubmitPO)) {
        if (allowedIncAmt.compareTo(BigDecimal.ZERO) == 0
            && allowedDecAmt.compareTo(BigDecimal.ZERO) == 0) {
          return true;
        }
      } else if (allowedIncAmt.compareTo(revIncAmtDiffFromBase) < 0
          || allowedDecAmt.compareTo(revDecAmtDiffFromBase) < 0) {
        return true;
      }
    } catch (OBException e) {
      log.error("Exception while checkTotalAmtUtilized:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return false;
  }

  public static JSONObject checkAutoEncumValidationForPR(JSONObject object, Order objOrder) {
    JSONObject result = new JSONObject();
    boolean errorFlag = false;
    try {
      result.put("errorflag", "1");
      result.put("errormsg", "null");

      String qurey = "select distinct val.c_salesregion_id from c_orderline  ln "
          + " join c_validcombination val on ln.em_efin_c_validcombination_id = val.c_validcombination_id "
          + " where c_order_id =:ordId and ln.em_escm_issummarylevel='N'";
      SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(qurey);
      sqlQuery.setParameter("ordId", objOrder.getId());
      if (sqlQuery.list() != null && sqlQuery.list().size() > 1) {
        result.put("errorflag", "0");
        result.put("errormsg", OBMessageUtils.messageBD("Efin_Req_SameDept"));
        return result;
      }

      // chk same budget type
      // chk same budget type
      qurey = "select distinct val.c_campaign_id from c_orderline ln "
          + "join c_validcombination val on ln.em_efin_c_validcombination_id = val.c_validcombination_id "
          + "where c_order_id =:ordId and ln.em_escm_issummarylevel='N'";
      sqlQuery = OBDal.getInstance().getSession().createSQLQuery(qurey);
      sqlQuery.setParameter("ordId", objOrder.getId());
      if (sqlQuery.list() != null && sqlQuery.list().size() > 1) {
        result.put("errorflag", "0");
        result.put("errormsg", OBMessageUtils.messageBD("Efin_Req_SameBType"));
        return result;
      }

      // chk budget type validation for uniquecode-->cost means cost, funds means funds
      // but
      // not
      // in cost.
      OBQuery<OrderLine> ordLine = OBDal.getInstance().createQuery(OrderLine.class,
          "salesOrder.id=:orderID and escmIssummarylevel='N'");
      ordLine.setNamedParameter("orderID", objOrder.getId());
      if (ordLine.list() != null && ordLine.list().size() > 0) {
        Campaign bType = ordLine.list().get(0).getEFINUniqueCode().getSalesCampaign();
        if (bType.getEfinBudgettype().equals("F")) {
          errorFlag = POContractSummaryDAO.checkFundsNoCostValidation(objOrder);
          if (errorFlag) {
            result.put("errorflag", "0");
            result.put("errormsg", OBMessageUtils.messageBD("Efin_Chk_Line_Info"));
            return result;
          }
        }
      }
      // funds validation.
      errorFlag = RequisitionfundsCheck.autoEncumbranceValidation(object,
          objOrder.getEfinBudgetint(), "PO", false);
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

  public static JSONObject getUniquecodeObject(Order objOrder) {
    JSONArray arraylist = new JSONArray(), linearraylist = null;
    JSONObject object = new JSONObject(), json = null, json1 = null;
    try {
      OBQuery<OrderLine> ln = OBDal.getInstance().createQuery(OrderLine.class,
          " salesOrder.id=:orderID order by eFINUniqueCode.id  ");
      ln.setNamedParameter("orderID", objOrder.getId());
      if (ln.list().size() > 0) {
        for (OrderLine ordLineobj : ln.list()) {
          if (ordLineobj.getEFINUniqueCode() != null) {
            if (json != null && json.has("Uniquecode")
                && json.getString("Uniquecode").equals(ordLineobj.getEFINUniqueCode().getId())) {
              json.put("Amount",
                  new BigDecimal(json.getString("Amount")).add(ordLineobj.getLineNetAmount()));
              json1 = new JSONObject();
              json1.put("lineId", ordLineobj.getId());
              linearraylist.put(json1);
              json.put("lineList", linearraylist);
            } else {
              if (json != null)
                json.put("lineList", linearraylist);
              linearraylist = new JSONArray();
              json = new JSONObject();
              json.put("Uniquecode", ordLineobj.getEFINUniqueCode().getId());
              json.put("Amount", ordLineobj.getLineNetAmount());
              json.put("isSummary", ordLineobj.isEscmIssummarylevel());
              json1 = new JSONObject();
              json1.put("lineId", ordLineobj.getId());
              linearraylist.put(json1);
              arraylist.put(json);
            }
          }
          ordLineobj.setEfinFailureReason(null);
          OBDal.getInstance().save(ordLineobj);
        }
        json.put("lineList", linearraylist);
      }
      object.put("uniquecodeList", arraylist);
    } catch (Exception e) {
      log.error("Exception in checkAutoEncumValidationForPR " + e.getMessage());
    }
    return object;
  }

  public static Long getRevisionNo(String baseOrderId) {
    Long revNo = 0L;
    StringBuffer query = null;
    Query revQuery = null;
    try {
      query = new StringBuffer();
      query.append("select max(ord.escmRevision) as rev from Order ord "
          + "where ord.escmBaseOrder.id=:baseOrderId and ord.active in ('N', 'Y')"
          + "group by ord.escmBaseOrder.id ");
      revQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      revQuery.setParameter("baseOrderId", baseOrderId);

      log.debug(" Query : " + query.toString());
      if (revQuery != null) {
        if (revQuery.list().size() > 0) {
          if (revQuery.iterate().hasNext()) {
            String rev = revQuery.iterate().next().toString();
            revNo = Long.parseLong(rev);
          }
        }
      }

    } catch (Exception e) {
      log.error("Exception while getRevisionNo", e);
      OBDal.getInstance().rollbackAndClose();
      return 0L;
    }
    return revNo;
  }

  public static Long checkBaseOrder(String orderId) {
    Long revNo = 0L;
    try {
      OBQuery<Order> order = OBDal.getInstance().createQuery(Order.class,
          "as e where e.escmBaseOrder.id=:OrderId order by created desc");
      order.setNamedParameter("OrderId", orderId);
      order.setFilterOnActive(false);
      order.setMaxResult(1);
      if (order.list().size() > 0) {
        Order ordRev = order.list().get(0);
        revNo = ordRev.getEscmRevision();
      }
    } catch (Exception e) {
      log.error("Exception while getRevisionNo", e);
      OBDal.getInstance().rollbackAndClose();
      return 0L;
    }
    return revNo;
  }

  /**
   * check alert already exist for document
   * 
   * @param roleId
   * @param recordId
   * @param clientId
   * @return
   */
  public static List<Alert> CheckAlert(String roleId, String recordId, String clientId) {
    OBQuery<Alert> alert = null;
    try {
      alert = OBDal.getInstance().createQuery(Alert.class,
          "as e where e.client.id=:clientId and e.referenceSearchKey=:recordId and e.role.id=:roleId and e.alertStatus ='NEW' and lower(e.description) like '%approval%' ");
      alert.setNamedParameter("clientId", clientId);
      alert.setNamedParameter("recordId", recordId);
      alert.setNamedParameter("roleId", roleId);
      return alert.list();
    } catch (Exception e) {
      log.error("Exception while CheckAlert", e);
      OBDal.getInstance().rollbackAndClose();
      return alert.list();
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
  public static boolean checkmixedPREncumbrance(Order order) {
    List<OrderLine> ordLineList = new ArrayList<OrderLine>();
    Boolean mixedencumbrance = false, isSkippedEnc = false, isNotSkippedEnc = false;
    // List<EfinBudgetManencum> encumlist = new ArrayList<EfinBudgetManencum>();
    try {
      OBContext.setAdminMode();
      ordLineList = order.getOrderLineList();
      if (ordLineList.size() > 0) {
        for (OrderLine lines : ordLineList) {
          if (!lines.isEscmIssummarylevel()) {
            for (EscmOrderSourceRef srcref : lines.getEscmOrdersourceRefList()) {
              if (srcref.getRequisition() != null) {
                if (srcref.getRequisition().isEfinSkipencumbrance()) {
                  // encumlist.add(srcref.getRequisition().getEfinBudgetManencum());
                  isSkippedEnc = true;
                } else {
                  if (srcref.getRequisition().getEfinBudgetManencum() != null) {
                    isNotSkippedEnc = true;
                  } else {
                    isSkippedEnc = true;
                  }
                }
              }
            }
          }
        }
      }
      /*
       * if (encumlist.size() > 0 && mixedencumbrance) { return mixedencumbrance; } else if
       * (encumlist.size() == 0 && mixedencumbrance) { mixedencumbrance = false; return
       * mixedencumbrance; } else { return mixedencumbrance; }
       */
      if (isSkippedEnc && isNotSkippedEnc) {
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
   * 
   * @param objOrder
   */
  public static void copyUniqueCodeValidation(Order objOrder) {

    try {
      SQLQuery Query = OBDal.getInstance().getSession().createSQLQuery(
          " select  COUNT(Distinct(em_efin_c_validcombination_id)) as count,em_efin_c_validcombination_id "
              + " from c_orderline where c_order_id=:orderID and em_escm_issummarylevel='N'"
              + " group by em_efin_c_validcombination_id ");
      Query.setParameter("orderID", objOrder.getId());
      @SuppressWarnings("rawtypes")
      List reqlinelist = Query.list();
      // if all line uniquecode is same
      if (Query != null && reqlinelist.size() == 1) {
        Object[] reqline = (Object[]) reqlinelist.get(0);
        if (reqline != null && reqline[1] != null) {
          String uniqueCode = reqline[1].toString();
          // if all line uniquecode is same but not same as header unique code then update
          // header
          // uniquecode with line uniquecode value
          if (objOrder.getEFINUniqueCode() != null) {
            if (!uniqueCode.equals(objOrder.getEFINUniqueCode().getId())) {
              AccountingCombination acct = OBDal.getInstance().get(AccountingCombination.class,
                  uniqueCode);
              objOrder.setEFINUniqueCode(acct);

            }
          } else {
            AccountingCombination acct = OBDal.getInstance().get(AccountingCombination.class,
                uniqueCode);
            objOrder.setEFINUniqueCode(acct);
          }
        }
      }
      // if all line uniquecode is not same then make header uniquecode as null
      else if (Query != null && reqlinelist.size() != 1) {
        objOrder.setEFINUniqueCode(null);

      }

    } catch (Exception e) {
      // TODO: handle exception
      log.error("Exception while copyUniqueCodeValidation", e);
      OBDal.getInstance().rollbackAndClose();
    }

  }

  /**
   * check po is used in invoice.
   * 
   * @param order
   * @return
   */
  public static List<Invoice> getPoUsed(String order) {
    OBQuery<Invoice> inv = null;
    try {
      inv = OBDal.getInstance().createQuery(Invoice.class,
          "efinCOrder.id=:order and documentStatus<>'EFIN_CA' ");
      inv.setNamedParameter("order", order);
      return inv.list();
    } catch (Exception e) {
      // TODO: handle exception
      log.error("Exception while poused in invoice", e);
      OBDal.getInstance().rollbackAndClose();
      return inv.list();
    }
  }

  public static boolean checkPOFactorApplied(Order ord) {
    boolean isPOFactApplied = false;
    try {
      if (ord.getOrderLineList().size() > 0) {
        OBQuery<OrderLine> ordLn = OBDal.getInstance().createQuery(OrderLine.class,
            " as e where e.escmPoChangeType is not null and e.escmIssummarylevel='N' and e.salesOrder.id=:orderId");
        ordLn.setNamedParameter("orderId", ord.getId());
        if (ordLn.list() != null && ordLn.list().size() > 0) {
          isPOFactApplied = true;
        }
      }
    } catch (Exception e) {
      log.error("exception in checkPOFactorApplied", e);
    }
    return isPOFactApplied;
  }

  public static boolean isPurchaseReleased(Order orderId) {
    boolean isPurchaseReleased = false;
    List<Order> orderList = null;
    try {
      OBQuery<Order> order = OBDal.getInstance().createQuery(Order.class,
          " as e where e.escmPurchaseagreement.id = :orderId");
      order.setNamedParameter("orderId", orderId);
      orderList = order.list();
      if (orderList != null && orderList.size() > 0) {
        isPurchaseReleased = true;
      }
    } catch (Exception e) {
      log.error("exception in isPurchaseReleased", e);
    }
    return isPurchaseReleased;
  }

  public static boolean isNewVersionCreatedAgainstPA(Order orderId) {
    boolean isNewVersion = false;
    List<Order> orderList = null;
    try {
      OBQuery<Order> order = OBDal.getInstance().createQuery(Order.class,
          " as e where e.escmOldOrder.id =:orderId");
      order.setNamedParameter("orderId", orderId);
      orderList = order.list();
      if (orderList != null && orderList.size() > 0) {
        isNewVersion = true;
      }
    } catch (Exception e) {
      log.error("exception in isNewVersionCreatedAgainstPA", e);
    }
    return isNewVersion;
  }

  /**
   * Method to update the release amount/quantity in Agreement during Purchase Release Reactivate
   * 
   * @param objOrder
   */
  public static void resetAgreementRelease(Order objOrder) {
    try {
      BigDecimal agReleaseQtyAmt = BigDecimal.ZERO;
      BigDecimal releasedQtyAmt = BigDecimal.ZERO;
      BigDecimal oldReleasedQtyAmt = BigDecimal.ZERO;
      BigDecimal oldReleasedQty = BigDecimal.ZERO;
      for (OrderLine releaseLn : objOrder.getOrderLineList()) {
        OrderLine agreementLn = releaseLn.getEscmAgreementLine();

        if (objOrder.getEscmReceivetype().equals(Constants.QTY_BASED)) {
          agReleaseQtyAmt = agreementLn.getEscmReleaseqty();
          OrderLine oldorderline = releaseLn.getEscmOldOrderline();
          if (oldorderline != null) {
            oldReleasedQty = oldorderline.getOrderedQuantity();
          }
          releasedQtyAmt = releaseLn.getOrderedQuantity().subtract(oldReleasedQty);
          // releasedQtyAmt = releaseLn.getOrderedQuantity();
          agreementLn.setEscmReleaseqty(agReleaseQtyAmt.subtract(releasedQtyAmt));

        } else if (objOrder.getEscmReceivetype().equals(Constants.AMOUNT_BASED)) {
          agReleaseQtyAmt = agreementLn.getEscmReleaseamt();
          OrderLine oldorderline = releaseLn.getEscmOldOrderline();
          if (oldorderline != null) {
            oldReleasedQtyAmt = oldorderline.getLineNetAmount();
          }
          releasedQtyAmt = releaseLn.getLineNetAmount().subtract(oldReleasedQtyAmt);
          // releasedQtyAmt = releaseLn.getLineNetAmount();
          agreementLn.setEscmReleaseamt(agReleaseQtyAmt.subtract(releasedQtyAmt));
        }
        OBDal.getInstance().save(agreementLn);
      }
    } catch (Exception e) {
      log.error("Exception in resetAgreementRelease:" + e);
    }
  }

  /**
   * Method to insert encumbrance modification for Purchase Release
   * 
   * @param encumbranceline
   * @param decamount
   * @param srcrefline
   */
  public static EfinBudManencumRev insertEncumbranceModificationForPurchaseRlse(
      EfinBudgetManencumlines encumbranceline, BigDecimal decamount,
      EfinBudgetManencumlines srcrefline) {
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
      manEncumRev.setEncumbranceType("POE");
      manEncumRev.setSystem(false);
      log.debug("req:" + manEncumRev.getRequisitionLine());
      OBDal.getInstance().save(manEncumRev);

    } catch (Exception e) {
      log.error("Exception in insertEncumbranceModificationForPurchaseRlse " + e.getMessage());
    }
    return manEncumRev;
  }

  /**
   * 
   * @param LatestAgreement
   * @param agreementLine
   * @return
   */
  public static void getLatestAgreementLine(Order LatestAgreement, OrderLine agreementLine,
      OrderLine objCloneOrderLine) {
    OrderLine latestAgreementLine = null;
    List<OrderLine> agreementLineList = null;
    try {

      OBQuery<OrderLine> agreementQryList = OBDal.getInstance().createQuery(OrderLine.class,
          " as e where e.escmOldOrderline.id=:oldlineId ");
      agreementQryList.setNamedParameter("oldlineId", agreementLine.getId());
      agreementLineList = agreementQryList.list();
      if (agreementLineList.size() > 0) {
        latestAgreementLine = agreementLineList.get(0);
        if (LatestAgreement == latestAgreementLine.getSalesOrder()) {
          objCloneOrderLine.setEscmAgreementLine(latestAgreementLine);
          OBDal.getInstance().save(objCloneOrderLine);
          return;
        }
        getLatestAgreementLine(LatestAgreement, latestAgreementLine, objCloneOrderLine);
      } else {
        objCloneOrderLine.setEscmAgreementLine(agreementLine);
        OBDal.getInstance().save(objCloneOrderLine);
      }
    } catch (Exception e) {
      log.error("Exception in getLatestAgreementLine:" + e);
    }
  }

  /**
   * Check whether the Purchase Agreement is used in Purchase Release with status Draft or wfa
   * 
   * @param Order
   * @return true
   */
  public static boolean checkAgreementRelease(Order objOrder) {
    List<Order> orderList = null;
    try {
      OBContext.setAdminMode();
      OBQuery<Order> order = OBDal.getInstance().createQuery(Order.class,
          " as e where e.escmPurchaseagreement.id=:orderId and (e.escmAppstatus='ESCM_IP' or e.escmAppstatus='DR')");
      order.setNamedParameter("orderId", objOrder.getId());
      orderList = order.list();
      if (orderList != null && orderList.size() > 0) {
        return true;
      }
    } catch (OBException e) {
      log.error("Exception while checkAgreementRelease:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return false;
  }

  /**
   * Get Total Line Quantity
   * 
   * @param Order
   * @return true
   */
  public static Boolean totalLineQty(Order objOrder) {
    BigDecimal quantity = BigDecimal.ZERO;
    String product = null;
    try {
      OBContext.setAdminMode();
      Order order = OBDal.getInstance().get(Order.class, objOrder.getEscmOldOrder().getId());
      for (OrderLine orderLn : order.getOrderLineList()) {
        if (!orderLn.isEscmIssummarylevel()) {
          product = orderLn.getId();
          quantity = orderLn.getEscmReleaseqty();
        }
        for (OrderLine releaseLn : objOrder.getOrderLineList()) {
          if (releaseLn.getEscmOldOrderline() != null) {
            if (!releaseLn.isEscmIssummarylevel()
                && releaseLn.getEscmOldOrderline().getId().equals(product)) {
              if (quantity.compareTo(releaseLn.getOrderedQuantity()) > 0) {
                return true;
              }
            }
          }
        }
      }
      return false;
    } catch (OBException e) {
      log.error("Exception while checkAgreementRelease:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * 
   * @param objOrder
   * @return
   */
  public static boolean checkAgreementNotApproved(Order objOrder) {
    Order baseAgreement = null;
    List<Order> agreementList = null;
    try {
      OBContext.setAdminMode();

      // get latest agreement version for new release.
      if (objOrder.isEscmIspurchaseagreement() && objOrder.getEscmOrdertype().equals("PUR_REL")) {
        Order agreement = objOrder.getEscmPurchaseagreement();
        if (agreement.getEscmBaseOrder() == null) {
          baseAgreement = agreement;
        } else {
          baseAgreement = agreement.getEscmBaseOrder();
        }
        OBQuery<Order> baseagreementQry = OBDal.getInstance().createQuery(Order.class,
            "as e where (e.escmBaseOrder.id=:baseOrder or e.id=:baseOrder) and e.escmAppstatus not in('ESCM_AP','ESCM_CA') ");
        baseagreementQry.setNamedParameter("baseOrder", baseAgreement.getId());
        agreementList = baseagreementQry.list();
        if (agreementList.size() > 0) {
          return true;
        }
      }
    } catch (OBException e) {
      log.error("Exception while checkAgreementRelease:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return false;
  }

  /**
   * 
   * @param order
   * @return
   */
  public static JSONObject getEncumAmount(Order order) {
    EfinBudgetManencumlines objEncumLine = null;
    JSONObject json = new JSONObject();
    JSONArray jsonArray = new JSONArray();
    try {
      for (OrderLine objOrderLine : order.getOrderLineList()) {
        if (!objOrderLine.isEscmIssummarylevel()) {
          // get encumbrance details
          OBQuery<EfinBudgetManencumlines> manline = OBDal.getInstance().createQuery(
              EfinBudgetManencumlines.class,
              " as e  where e.manualEncumbrance.id=:encumId and e.accountingCombination.id=:acctId ");
          manline.setNamedParameter("encumId", order.getEfinBudgetManencum().getId());
          manline.setNamedParameter("acctId", objOrderLine.getEFINUniqueCode().getId());

          manline.setMaxResult(1);
          if (manline.list().size() > 0) {
            objEncumLine = manline.list().get(0);
            // do revision with increase or decrease amount
            BigDecimal amount = BigDecimal.ZERO;
            BigDecimal proposalAmt = BigDecimal.ZERO;
            if (objOrderLine.getEscmProposalmgmtLine() != null) {
              OBQuery<EscmProposalmgmtLine> propMgmtQry = OBDal.getInstance().createQuery(
                  EscmProposalmgmtLine.class,
                  "as e where e.eFINUniqueCode.id=:uniquecodeid and e.id=:propmgmtlineid");
              propMgmtQry.setNamedParameter("uniquecodeid",
                  objOrderLine.getEFINUniqueCode().getId());
              propMgmtQry.setNamedParameter("propmgmtlineid",
                  objOrderLine.getEscmProposalmgmtLine().getId());
              propMgmtQry.setMaxResult(1);
              if (propMgmtQry.list().size() > 0) {
                EscmProposalmgmtLine propMgmtLn = propMgmtQry.list().get(0);
                if (propMgmtLn.getEscmProposalmgmt().getProposalstatus().equals("PAWD")) {
                  proposalAmt = propMgmtLn.getAwardedamount();
                } else {
                  proposalAmt = propMgmtLn.getLineTotal();
                }
              }
            }
            if ((objOrderLine.getLineNetAmount().compareTo(proposalAmt) > 0)
                || objOrderLine.getLineNetAmount().compareTo(proposalAmt) < 0) {
              // inc/dec in encumbrance
              amount = objOrderLine.getLineNetAmount().subtract(proposalAmt);
            }
            JSONObject jsonData = new JSONObject();
            jsonData.put("encAmount", amount);
            jsonData.put("accDimension", objOrderLine.getEFINUniqueCode().getId());
            jsonData.put("budgetInt", order.getEfinBudgetint().getId());
            jsonData.put("encumLine", objEncumLine.getId());
            jsonData.put("encumLineRem", objEncumLine.getRemainingAmount());
            jsonArray.put(jsonData);
          }
        }
      }
      if (jsonArray != null && jsonArray.length() > 0) {
        json.put("data", jsonArray);
      }
    } catch (OBException e) {
      log.error("Exception while getEncumAmount:" + e);
      throw new OBException(e.getMessage());
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      log.error("Exception while getEncumAmount:" + e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return json;
  }

  /**
   * Checks whether older version have Incomplete PO receipt
   * 
   * @param order
   * @return
   */
  public static boolean hasIncompleteReceipt(Order order) {
    boolean hasIncompleteReceipt = false;

    try {
      OBContext.setAdminMode();
      // Get PO receipt against previous version
      OBQuery<ShipmentInOut> receiptQry = OBDal.getInstance().createQuery(ShipmentInOut.class,
          "as e where e.salesOrder.id = :orderId");
      receiptQry.setNamedParameter("orderId", order.getId());
      if (receiptQry != null) {
        List<ShipmentInOut> recList = receiptQry.list();
        if (recList.size() > 0) {
          for (ShipmentInOut receipt : recList) {

            // For Site Receiving and Project Receiving check record is in Draft
            if (("PROJ".equals(receipt.getEscmReceivingtype())
                || "SR".equals(receipt.getEscmReceivingtype()))
                && "DR".equals(receipt.getEscmDocstatus())) {

              hasIncompleteReceipt = true;
              return hasIncompleteReceipt;

            } else {

              OBQuery<EscmInitialReceipt> linesQry = OBDal.getInstance().createQuery(
                  EscmInitialReceipt.class, "as e where e.goodsShipment.id = :poReceiptId");
              linesQry.setNamedParameter("poReceiptId", receipt.getId());
              if (linesQry != null) {
                List<EscmInitialReceipt> linesList = linesQry.list();
                if (linesList.size() > 0) {
                  for (EscmInitialReceipt line : linesList) {
                    if (!line.isSummaryLevel()) {
                      hasIncompleteReceipt = isReceiptAmtQtyNotEqual(receipt.getEscmReceivingtype(),
                          receipt.getEscmDocstatus(), line);
                      if (hasIncompleteReceipt) {
                        return hasIncompleteReceipt;
                      }
                    }
                  }
                }
                if (linesList.size() == 0 && "IR".equals(receipt.getEscmReceivingtype())) {
                  hasIncompleteReceipt = true;
                  return hasIncompleteReceipt;
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in hasIncompleteReceipt: ", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }

    return hasIncompleteReceipt;
  }

  public static boolean isReceiptAmtQtyNotEqual(String receivingType, String docStatus,
      EscmInitialReceipt line) {

    boolean isReceiptAmtQtyNotEqual = false;

    if ("IR".equals(receivingType)) {

      // Quantity based
      BigDecimal receiveQty = line.getQuantity();
      BigDecimal receiptQty = BigDecimal.ZERO;

      BigDecimal rejectedQty = line.getRejectedQty() != null ? line.getRejectedQty()
          : BigDecimal.ZERO;

      BigDecimal deliveredQty = line.getDeliveredQty() != null ? line.getDeliveredQty()
          : BigDecimal.ZERO;

      BigDecimal returnQty = line.getReturnQty() != null ? line.getReturnQty() : BigDecimal.ZERO;

      BigDecimal irReturnQty = line.getReturnQuantity() != null ? line.getReturnQuantity()
          : BigDecimal.ZERO;

      receiptQty = rejectedQty.add(deliveredQty).add(returnQty).add(irReturnQty);

      if (receiveQty.compareTo(receiptQty) != 0) {
        isReceiptAmtQtyNotEqual = true;
        return isReceiptAmtQtyNotEqual;
      }
    }

    return isReceiptAmtQtyNotEqual;
  }

  /**
   * @param newOrderId
   * 
   * @param oldOrderId
   * 
   * @return 1 if new qty and unit price less than old po "receipt" qty and unit price , if unit
   *         price only have changed and qty are still same in Earlier PO Receipt then return 2
   * 
   */
  public static int checkOlderVersionQtyAndUnitPrice(String newOrderId, String oldOrderId) {
    int valid_change = 0;
    try {
      OBContext.setAdminMode();

      OBQuery<OrderLine> ordln = OBDal.getInstance().createQuery(OrderLine.class,
          " as e where e.salesOrder.id=:orderID ");
      ordln.setNamedParameter("orderID", newOrderId);

      if (ordln.list().size() > 0) {
        for (OrderLine ordLn : ordln.list()) {

          if (!ordLn.isEscmIssummarylevel()) {

            BigDecimal receivedQty = BigDecimal.ZERO;
            BigDecimal newOrdQty = ordLn.getOrderedQuantity();// ordln ord qty
            // line net amount unit price and qty
            BigDecimal oldLineReceiptNetAmount = BigDecimal.ZERO;
            BigDecimal newOrderLineAmount = BigDecimal.ZERO;
            // Old Orderline
            OrderLine oldOrderLine = ordLn.getEscmOldOrderline();

            // get old order received quantity
            BigDecimal poReceiptQty = ordLn.getEscmQtyporec() != null ? ordLn.getEscmQtyporec()
                : BigDecimal.ZERO;
            BigDecimal qtyIrr = ordLn.getEscmQtyirr() != null ? ordLn.getEscmQtyirr()
                : BigDecimal.ZERO;
            BigDecimal qtyRejected = ordLn.getEscmQtyrejected() != null ? ordLn.getEscmQtyrejected()
                : BigDecimal.ZERO;
            BigDecimal qtyReturned = ordLn.getEscmQtyreturned() != null ? ordLn.getEscmQtyreturned()
                : BigDecimal.ZERO;
            BigDecimal qtyCancelled = ordLn.getEscmQtycanceled() != null
                ? ordLn.getEscmQtycanceled()
                : BigDecimal.ZERO;
            BigDecimal legacyDelQty = ordLn.getEscmLegacyQtyDelivered() != null
                ? ordLn.getEscmLegacyQtyDelivered()
                : BigDecimal.ZERO;
            BigDecimal qtyDelivered = poReceiptQty.subtract(qtyIrr).subtract(qtyRejected)
                .subtract(qtyReturned);

            receivedQty = qtyDelivered.add(qtyCancelled).add(legacyDelQty);

            oldLineReceiptNetAmount = receivedQty.multiply(
                (oldOrderLine != null ? oldOrderLine.getEscmNetUnitprice() : BigDecimal.ZERO));
            newOrderLineAmount = newOrdQty.multiply(ordLn.getEscmNetUnitprice());
            log.debug(oldLineReceiptNetAmount + ">" + newOrderLineAmount);
            if (oldLineReceiptNetAmount.compareTo(newOrderLineAmount) > 0) {
              valid_change = 1;
            }
            if (newOrdQty.compareTo(receivedQty) == 0 && ordLn.getEscmNetUnitprice()
                .compareTo((oldOrderLine != null ? oldOrderLine.getEscmNetUnitprice()
                    : BigDecimal.ZERO)) > 0) {
              valid_change = 2;
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in checkOlderVersionQtyAndUnitPrice: ", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
    return valid_change;
  }

  public static BigDecimal calculateChangeValue(String poChangeType, String poChangeFact,
      BigDecimal poChangeValue, BigDecimal quantity, BigDecimal unitPrice, OrderLine ordline,
      boolean initalValue) {
    BigDecimal changeValue = BigDecimal.ZERO;
    BigDecimal grossLineNetAmt = BigDecimal.ZERO, taxPercent = BigDecimal.ZERO;
    // Get Line amount Lookup Id
    String lineAmtChangeTypeId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGTYP",
        "01");
    // Get Line Percent Lookup Id
    String linePercentChangeTypeId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGTYP",
        "02");
    try {

      OBContext.setAdminMode();

      String decFactId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGFACT", "01");
      grossLineNetAmt = quantity.multiply(unitPrice);
      BigDecimal remAmt = BigDecimal.ZERO;
      remAmt = POContractSummaryTotPOChangeDAO.remainingAmtLinesForcalculateChangeValue(ordline,
          unitPrice, initalValue);

      if (poChangeType.equals(lineAmtChangeTypeId)) {
        if (poChangeFact.equals(decFactId)) {
          changeValue = poChangeValue.negate();
        } else {
          changeValue = poChangeValue;
        }
      } else if (poChangeType.equals(linePercentChangeTypeId)) {
        if (poChangeFact.equals(decFactId)) {
          changeValue = remAmt.multiply((poChangeValue).divide(new BigDecimal("100"))).negate();
        } else {
          changeValue = grossLineNetAmt.multiply((poChangeValue).divide(new BigDecimal("100")));
        }
      }

      log.debug("lnPriceUpdatedAmt>" + changeValue);
    } catch (Exception e) {
      log.error("Exception while calculateChangeValue:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return changeValue;
  }

  public static void updateTaxAndChangeValue(Boolean isChange, Order ord, OrderLine ordLn) {
    BigDecimal lineChgValue = BigDecimal.ZERO, lineChgValueWithoutTax = BigDecimal.ZERO,
        unitPriceAfterChange = BigDecimal.ZERO, taxPercent = BigDecimal.ZERO,
        grossLineAmt = BigDecimal.ZERO, calLineNetAmt = BigDecimal.ZERO,
        lineTaxAmt = BigDecimal.ZERO;
    int roundoffConst = 2;
    try {
      unitPriceAfterChange = ordLn.getEscmUnitpriceAfterchag();
      if (ord.getClient().getCurrency() != null
          && ord.getClient().getCurrency().getStandardPrecision() != null)
        roundoffConst = ord.getClient().getCurrency().getStandardPrecision().intValue();
      if (ord.getEscmTaxMethod() != null) {
        taxPercent = new BigDecimal(ord.getEscmTaxMethod().getTaxpercent());
      }

      if (ordLn.getEscmPoChangeType() != null && ordLn.getEscmPoChangeFactor() != null
          && ordLn.getEscmPoChangeValue() != null) {
        lineChgValue = calculateChangeValue(ordLn.getEscmPoChangeType().getId(),
            ordLn.getEscmPoChangeFactor().getId(), ordLn.getEscmPoChangeValue(),
            ordLn.getOrderedQuantity(), ordLn.getUnitPrice(), ordLn, false);
      }

      if (isChange) {
        unitPriceAfterChange = ordLn.getUnitPrice()
            .add(lineChgValue.divide(ordLn.getOrderedQuantity(), 2, RoundingMode.HALF_UP));
        grossLineAmt = ordLn.getUnitPrice().multiply(ordLn.getOrderedQuantity());
        calLineNetAmt = grossLineAmt.add(lineChgValue);
      }

      if (ordLn.getSalesOrder().getEscmTaxMethod() != null && ordLn.getSalesOrder().isEscmIstax()) {
        if (ordLn.getSalesOrder().getEscmTaxMethod().isPriceIncludesTax()) {
          if (ordLn.getEscmInitialUnitprice() == null
              || ordLn.getEscmInitialUnitprice().compareTo(BigDecimal.ZERO) == 0) {
            ordLn.setEscmInitialUnitprice(ordLn.getUnitPrice());
          }
          // unitPriceAfterChange = ordLn.getEscmInitialUnitprice()
          // .add(lineChgValue.divide(ordLn.getOrderedQuantity(), 2, RoundingMode.HALF_UP));
          // ordLn.setEscmUnitpriceAfterchag(unitPriceAfterChange);

          BigDecimal NegUnitPrice = (ordLn.getEscmInitialUnitprice().divide(
              BigDecimal.ONE
                  .add(taxPercent.divide(new BigDecimal(100), roundoffConst, RoundingMode.HALF_UP)),
              roundoffConst, RoundingMode.HALF_UP));
          if (ordLn.getEscmPoChangeType() != null && ordLn.getEscmPoChangeFactor() != null
              && ordLn.getEscmPoChangeValue() != null) {
            lineChgValueWithoutTax = calculateChangeValue(ordLn.getEscmPoChangeType().getId(),
                ordLn.getEscmPoChangeFactor().getId(), ordLn.getEscmPoChangeValue(),
                ordLn.getOrderedQuantity(), ordLn.getEscmInitialUnitprice(), ordLn, true);
            if (isChange) {
              lineChgValue = calculateChangeValue(ordLn.getEscmPoChangeType().getId(),
                  ordLn.getEscmPoChangeFactor().getId(), ordLn.getEscmPoChangeValue(),
                  ordLn.getOrderedQuantity(), NegUnitPrice, ordLn, false);
            }
          }

          /**
           * While doing tax calculation for initial version consider initial price for tax
           * calculation
           */
          if (ord.getEscmRevision() == 0) {
            grossLineAmt = (ordLn.getEscmInitialUnitprice().multiply(ordLn.getOrderedQuantity()))
                .divide(BigDecimal.ONE.add(
                    taxPercent.divide(new BigDecimal(100), roundoffConst, RoundingMode.HALF_UP)),
                    roundoffConst, RoundingMode.HALF_UP);

            lineTaxAmt = (ordLn.getEscmInitialUnitprice().multiply(ordLn.getOrderedQuantity()))
                .subtract(grossLineAmt);

            calLineNetAmt = NegUnitPrice.multiply(ordLn.getOrderedQuantity()).add(lineTaxAmt)
                .add(lineChgValue);
          }

          else {
            grossLineAmt = NegUnitPrice.multiply(ordLn.getOrderedQuantity());

            // lineTaxAmt = (grossLineAmt.add(lineChgValue))
            // .multiply(taxPercent.divide(new BigDecimal(100)))
            // .setScale(roundoffConst, RoundingMode.HALF_UP);
            lineTaxAmt = (grossLineAmt).multiply(taxPercent.divide(new BigDecimal(100)))
                .setScale(roundoffConst, RoundingMode.HALF_UP);
            calLineNetAmt = NegUnitPrice.multiply(ordLn.getOrderedQuantity()).add(lineTaxAmt)
                .add(lineChgValue);
          }

          BigDecimal expectedLineNetAmt = ordLn.getOrderedQuantity()
              .multiply(ordLn.getEscmInitialUnitprice()).add(lineChgValueWithoutTax);
          ordLn.setUnitPrice(NegUnitPrice);
          ordLn.setEscmLineTaxamt(lineTaxAmt);
          ordLn.setLineNetAmount(calLineNetAmt);
          ordLn.setEscmRounddiffTax(calLineNetAmt.subtract(expectedLineNetAmt));
          ordLn.getSalesOrder().setEscmCalculateTaxlines(true);
          OBDal.getInstance().save(ordLn);
        } else {
          ordLn.setEscmUnitpriceAfterchag(unitPriceAfterChange);
          grossLineAmt = ordLn.getUnitPrice().multiply(ordLn.getOrderedQuantity());
          calLineNetAmt = grossLineAmt.add(lineChgValue);
          // TAX AMOUNT CALCULATION
          lineTaxAmt = calLineNetAmt
              .multiply(taxPercent.divide(new BigDecimal(100), roundoffConst, RoundingMode.HALF_UP))
              .setScale(roundoffConst, RoundingMode.HALF_UP);
          ordLn.setEscmLineTaxamt(lineTaxAmt);
          ordLn.setLineNetAmount(calLineNetAmt.add(lineTaxAmt));
          ordLn.getSalesOrder().setEscmCalculateTaxlines(true);
          OBDal.getInstance().save(ordLn);
        }
      } else {
        if (isChange) {
          ordLn.setEscmUnitpriceAfterchag(unitPriceAfterChange);
          ordLn.setLineNetAmount(calLineNetAmt);
        }
      }
    } catch (Exception e) {
      log.error("exception in updateTaxAndChangeValue: ", e);
    } finally {
    }
  }

  public static JSONObject getTaxandChangeValue(Boolean isChange, Order ord, OrderLine ordLn) {
    BigDecimal lineChgValue = BigDecimal.ZERO, unitPriceAfterChange = BigDecimal.ZERO,
        taxPercent = BigDecimal.ZERO;
    int roundoffConst = 2;
    JSONObject result = new JSONObject();
    BigDecimal calLineNetAmt = BigDecimal.ZERO, lineChgValueWithoutTax = BigDecimal.ZERO,
        grossLineAmt = BigDecimal.ZERO, initialUnitPrice = BigDecimal.ZERO,
        lineTaxAmt = BigDecimal.ZERO;
    try {
      result.put("unitPriceAfterChange", BigDecimal.ZERO);
      result.put("initialUnitPrice", BigDecimal.ZERO);
      result.put("negUnitPrice", BigDecimal.ZERO);
      result.put("lineTaxAmt", BigDecimal.ZERO);
      result.put("lineNetAmt", BigDecimal.ZERO);
      result.put("roundOfTaxDiff", BigDecimal.ZERO);

      if (ord.getClient().getCurrency() != null
          && ord.getClient().getCurrency().getStandardPrecision() != null)
        roundoffConst = ord.getClient().getCurrency().getStandardPrecision().intValue();
      if (ord.getEscmTaxMethod() != null) {
        taxPercent = new BigDecimal(ord.getEscmTaxMethod().getTaxpercent());
      }

      if (ordLn.getEscmPoChangeType() != null && ordLn.getEscmPoChangeFactor() != null
          && ordLn.getEscmPoChangeValue() != null) {
        lineChgValue = calculateChangeValue(ordLn.getEscmPoChangeType().getId(),
            ordLn.getEscmPoChangeFactor().getId(), ordLn.getEscmPoChangeValue(),
            ordLn.getOrderedQuantity(), ordLn.getUnitPrice(), ordLn, false);
      }

      if (isChange) {
        unitPriceAfterChange = ordLn.getUnitPrice()
            .add(lineChgValue.divide(ordLn.getOrderedQuantity(), 2, RoundingMode.HALF_UP));

      }
      grossLineAmt = ordLn.getUnitPrice().multiply(ordLn.getOrderedQuantity());
      calLineNetAmt = grossLineAmt.add(lineChgValue);

      if (ordLn.getSalesOrder().getEscmTaxMethod() != null && ordLn.getSalesOrder().isEscmIstax()) {
        if (ordLn.getSalesOrder().getEscmTaxMethod().isPriceIncludesTax()) {
          if (ordLn.getEscmInitialUnitprice() == null
              || ordLn.getEscmInitialUnitprice().compareTo(BigDecimal.ZERO) == 0) {
            result.put("initialUnitPrice", ordLn.getUnitPrice());
            initialUnitPrice = ordLn.getUnitPrice();
          } else {
            initialUnitPrice = ordLn.getEscmInitialUnitprice();
            result.put("initialUnitPrice", ordLn.getEscmInitialUnitprice());
          }
          // if (isChange) {
          // unitPriceAfterChange = initialUnitPrice
          // .add(lineChgValue.divide(ordLn.getOrderedQuantity(), 2, RoundingMode.HALF_UP));
          // result.put("unitPriceAfterChange", unitPriceAfterChange);
          // }

          BigDecimal NegUnitPrice = (initialUnitPrice.divide(
              BigDecimal.ONE
                  .add(taxPercent.divide(new BigDecimal(100), roundoffConst, RoundingMode.HALF_UP)),
              roundoffConst, RoundingMode.HALF_UP));

          if (ordLn.getEscmPoChangeType() != null && ordLn.getEscmPoChangeFactor() != null
              && ordLn.getEscmPoChangeValue() != null) {
            lineChgValueWithoutTax = calculateChangeValue(ordLn.getEscmPoChangeType().getId(),
                ordLn.getEscmPoChangeFactor().getId(), ordLn.getEscmPoChangeValue(),
                ordLn.getOrderedQuantity(), initialUnitPrice, ordLn, true);
            if (isChange) {
              lineChgValue = calculateChangeValue(ordLn.getEscmPoChangeType().getId(),
                  ordLn.getEscmPoChangeFactor().getId(), ordLn.getEscmPoChangeValue(),
                  ordLn.getOrderedQuantity(), NegUnitPrice, ordLn, false);
            }
          }

          /**
           * Added for task 8109
           * 
           * while doing tax calculation for initial version consider the initial unit price
           **/
          if (ord.getEscmRevision() == 0) {
            grossLineAmt = (ordLn.getEscmInitialUnitprice().multiply(ordLn.getOrderedQuantity()))
                .divide(BigDecimal.ONE.add(
                    taxPercent.divide(new BigDecimal(100), roundoffConst, RoundingMode.HALF_UP)),
                    roundoffConst, RoundingMode.HALF_UP);

            lineTaxAmt = (ordLn.getEscmInitialUnitprice().multiply(ordLn.getOrderedQuantity()))
                .subtract(grossLineAmt);

            calLineNetAmt = NegUnitPrice.multiply(ordLn.getOrderedQuantity()).add(lineTaxAmt)
                .add(lineChgValue);
          } else {
            grossLineAmt = NegUnitPrice.multiply(ordLn.getOrderedQuantity());

            lineTaxAmt = (grossLineAmt.add(lineChgValue))
                .multiply(taxPercent.divide(new BigDecimal(100)))
                .setScale(roundoffConst, RoundingMode.HALF_UP);
            calLineNetAmt = grossLineAmt.add(lineTaxAmt).add(lineChgValue);
          }

          /** end task 8109 **/

          /** Removed from task 8109 **/

          // grossLineAmt = NegUnitPrice.multiply(ordLn.getOrderedQuantity());

          // lineTaxAmt = (grossLineAmt.add(lineChgValue))
          // .multiply(taxPercent.divide(new BigDecimal(100)))
          // .setScale(roundoffConst, RoundingMode.HALF_UP);
          // calLineNetAmt = grossLineAmt.add(lineTaxAmt).add(lineChgValue);

          /** End for task 8109 **/

          BigDecimal expectedLineNetAmt = ordLn.getOrderedQuantity().multiply(initialUnitPrice)
              .add(lineChgValueWithoutTax);

          result.put("negUnitPrice", NegUnitPrice);
          result.put("lineTaxAmt", lineTaxAmt);
          result.put("lineNetAmt", calLineNetAmt);
          result.put("roundOfTaxDiff", calLineNetAmt.subtract(expectedLineNetAmt));
        } else {
          result.put("unitPriceAfterChange", unitPriceAfterChange);
          grossLineAmt = ordLn.getUnitPrice().multiply(ordLn.getOrderedQuantity());
          calLineNetAmt = grossLineAmt.add(lineChgValue);
          // TAX AMOUNT CALCULATION
          lineTaxAmt = calLineNetAmt
              .multiply(taxPercent.divide(new BigDecimal(100), roundoffConst, RoundingMode.HALF_UP))
              .setScale(roundoffConst, RoundingMode.HALF_UP);
          result.put("negUnitPrice", ordLn.getUnitPrice());
          result.put("lineTaxAmt", lineTaxAmt);
          result.put("lineNetAmt", calLineNetAmt.add(lineTaxAmt));

        }
      } else {
        if (isChange) {
          result.put("unitPriceAfterChange", unitPriceAfterChange);
          result.put("lineNetAmt", calLineNetAmt);
        }
      }
    } catch (Exception e) {
      log.error("exception in getTaxandChangeValue: ", e);
    } finally {
    }
    return result;
  }

  /**
   * 
   * @param record
   *          Id
   * @return latest forward record
   */
  public static EutForwardReqMoreInfo findForwardReferenceAgainstTheRecord(String recordId,
      String userId, String roleId) {
    EutForwardReqMoreInfo objForwardReqMoreInfo = null;
    try {
      // get the next line no based on bid management id
      OBQuery<EutForwardReqMoreInfo> query = OBDal.getInstance().createQuery(
          EutForwardReqMoreInfo.class,
          " as e where e.recuser.id=:userId and e.rECRole.id=:roleId and e.forwardRmi=:forward and e.recordid=:recordId "
              // + " and e.processed=:processed "
              + " order by e.creationDate desc ");
      query.setNamedParameter("userId", userId);
      query.setNamedParameter("roleId", roleId);
      query.setNamedParameter("forward", "F");
      query.setNamedParameter("recordId", recordId);
      // query.setNamedParameter("processed", false);
      query.setMaxResult(1);
      if (query.list().size() > 0) {
        objForwardReqMoreInfo = query.list().get(0);
      }
    }

    catch (Exception e) {
      log.error("exception in findForwardReferenceAgainstTheRecord: ", e);
    } finally {
    }
    return objForwardReqMoreInfo;
  }

  public static String getFinalBgworkbenchd(String orderId) {
    String bgWorkbenchId = null;
    try {
      OBContext.setAdminMode();
      OBQuery<ESCMBGWorkbench> bg = OBDal.getInstance().createQuery(ESCMBGWorkbench.class,
          " as e where e.documentNo.id=:orderID and type='FBG'");
      bg.setNamedParameter("orderID", orderId);
      bg.setMaxResult(1);
      if (bg.list().size() > 0) {
        bgWorkbenchId = bg.list().get(0).getId();
        return bgWorkbenchId;
      }
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log.error("Exception in getBgworkbenchd in POContractSummaryDAO: ", e);
      OBDal.getInstance().rollbackAndClose();
      return bgWorkbenchId;
    } finally {
      OBContext.restorePreviousMode();
    }
    return bgWorkbenchId;
  }

  /**
   * if no payment schedule exists for old version then create the new payment schedule by sum of
   * the invoiced amount against the order
   * 
   * @param purchase
   *          order object
   * @param new
   *          purchase order object
   * @param vars
   * @return true or false based on the insertion
   */

  public static Boolean copyPaymentSchedule(Order objOrder, Order objCloneOrder,
      VariablesSecureApp vars) {
    Boolean iscopied = Boolean.TRUE;
    try {
      String description = "old contract invoiced details";
      if (objOrder.getEscmPaymentScheduleList().size() == 0) {
        ESCMPaymentSchedule objNewPaymentSchedule = OBProvider.getInstance()
            .get(ESCMPaymentSchedule.class);
        objNewPaymentSchedule.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
        objNewPaymentSchedule.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
        objNewPaymentSchedule.setOLDContract(Boolean.TRUE);
        objNewPaymentSchedule.setAmount(objOrder.getGrandTotalAmount());
        objNewPaymentSchedule.setInvoicedAmt(objCloneOrder.getEfinInvoiceAmt());
        objNewPaymentSchedule.setValuePer(new BigDecimal(100));
        objNewPaymentSchedule.setLine((long) 10);
        objNewPaymentSchedule.setPAYNature("PP");
        objNewPaymentSchedule.setType("M");
        objNewPaymentSchedule.setDescription(description);
        objNewPaymentSchedule.setDocumentNo(objCloneOrder);
        OBDal.getInstance().save(objNewPaymentSchedule);
        iscopied = objNewPaymentSchedule != null ? Boolean.TRUE : Boolean.FALSE;
      } else {
        for (ESCMPaymentSchedule oldPaymentScheduler : objOrder.getEscmPaymentScheduleList()) {
          ESCMPaymentSchedule objNewPaymentSchedule = (ESCMPaymentSchedule) DalUtil
              .copy(oldPaymentScheduler, false);
          objNewPaymentSchedule.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
          objNewPaymentSchedule.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
          objNewPaymentSchedule.setCreationDate(new Date());
          objNewPaymentSchedule.setUpdated(new Date());
          objNewPaymentSchedule.setParent(oldPaymentScheduler);
          objNewPaymentSchedule.setDocumentNo(objCloneOrder);
          OBDal.getInstance().save(objNewPaymentSchedule);
          iscopied = objNewPaymentSchedule != null ? Boolean.TRUE : Boolean.FALSE;

        }
      }
    } catch (Exception e) {
      log.error("Exception while copy the paymentSchedule on create new version process", e);
      OBDal.getInstance().rollbackAndClose();
      iscopied = Boolean.FALSE;
    }
    return iscopied;
  }

  public static boolean encumUsedInOtherProposals(EscmProposalMgmt proposalMgmt) {
    boolean isEncumUsed = false;
    try {
      OBContext.setAdminMode();
      if (proposalMgmt.getEscmBidmgmt() != null) {
        OBQuery<EscmProposalMgmt> proposalQry = OBDal.getInstance()
            .createQuery(EscmProposalMgmt.class, " as e where e.escmBidmgmt.id = :bidMgmtId "
                + " and e.awardamount > 0 and e.id != :proposalId and e.efinIsbudgetcntlapp = true "
                + " and e.efinEncumbrance.id = :encumId");
        proposalQry.setNamedParameter("bidMgmtId", proposalMgmt.getEscmBidmgmt().getId());
        proposalQry.setNamedParameter("proposalId", proposalMgmt.getId());
        proposalQry.setNamedParameter("encumId", proposalMgmt.getEfinEncumbrance().getId());
        if (proposalQry != null) {
          List<EscmProposalMgmt> proposalList = proposalQry.list();
          if (proposalList.size() > 0) {
            isEncumUsed = true;
          }
        }
      }

    } catch (Exception e) {
      log.error("Exception in encumUsedInOtherProposals", e);
      OBDal.getInstance().rollbackAndClose();
      return false;
    }
    return isEncumUsed;
  }

}
