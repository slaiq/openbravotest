package sa.elm.ob.scm.ad_process.POandContract;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.EscmOrderSourceRef;
import sa.elm.ob.scm.EscmOrderlineV;
import sa.elm.ob.scm.Escmorderpaymentterms;
import sa.elm.ob.scm.EscmproposalDistribution;
import sa.elm.ob.scm.ad_process.POandContract.dao.POContractSummaryDAO;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;
import sa.elm.ob.utility.util.copyattachments.CopyAttachmentsImpl;
import sa.elm.ob.utility.util.copyattachments.CopyAttachmentsService;

/**
 * @author gopalakrishnan on 14/08/2017
 */

public class POContractCreateVersion extends DalBaseProcess {
  /**
   * This servlet is responsible to create new versions in Purchase order
   */
  private static final Logger log = LoggerFactory.getLogger(POContractCreateVersion.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    log.debug("Enter");
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    HashMap<Long, Long> childParentLineNoMap = new HashMap<Long, Long>();
    boolean hasIncompleteReceipt = false;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    EfinBudgetIntialization budInit = null;
    final String userId = (String) bundle.getContext().getUser();
    List<Attachment> fileList = new ArrayList<Attachment>();
    CopyAttachmentsService copyAttachmentDAO = new CopyAttachmentsImpl();
    try {
      OBContext.setAdminMode();
      final String strOrderId = (String) bundle.getParams().get("C_Order_ID").toString();

      // // check used in purchaseinvoice
      // List<Invoice> invPoList = POContractSummaryDAO.getPoUsed(strOrderId);
      // if (invPoList != null && invPoList.size() > 0) {
      // OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Po_Used_Invoice@");
      // bundle.setResult(result);
      // return;
      // }

      // check duplicate version
      if (POContractSummaryDAO.checkDuplicateVersion(strOrderId)) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_NoDupPOVersion@");
        bundle.setResult(result);
        return;
      }
      // check amt utilized for order and contract
      Order objOrder = OBDal.getInstance().get(Order.class, strOrderId);
      if (!objOrder.isEscmIspurchaseagreement()) {
        if (POContractSummaryDAO.checkTotalAmtUtilized(objOrder.getDocumentNo(), "N",
            objOrder.getEscmOrdertype(), objOrder.getClient().getId(),
            objOrder.getGrandTotalAmount(), objOrder)) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_PONewVersionNotAllowed@");
          bundle.setResult(result);
          return;
        }
      }

      // need to check amount limit for agreement new version.
      /*
       * if (objOrder.isEscmIspurchaseagreement() && objOrder.getEscmOrdertype().equals("PUR_AG")) {
       * if (objOrder.getEscmMaxRelease() == null ||
       * objOrder.getEscmMaxRelease().compareTo(BigDecimal.ZERO) == 0) { OBError result =
       * OBErrorBuilder.buildMessage(null, "error", "@ESCM_PONewVersionNotAllowed@");
       * bundle.setResult(result); return; } }
       */

      // Check Purchase Agreement has Purchase Release in status Draft or wfa
      if (objOrder.getEscmOrdertype().equals("PUR_AG")) {
        if (POContractSummaryDAO.checkAgreementRelease(objOrder)) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_PurAgHasPurRel@");
          bundle.setResult(result);
          return;
        }
      }

      // Check Purchase Agreement has Purchase Release in status Draft or wfa
      if (objOrder.getEscmOrdertype().equals("PUR_REL")) {
        if (POContractSummaryDAO.checkAgreementNotApproved(objOrder)) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_PurAginDraft@");
          bundle.setResult(result);
          return;
        }
      }

      // Checks whether older version have Incomplete PO receipt
      hasIncompleteReceipt = POContractSummaryDAO.hasIncompleteReceipt(objOrder);
      if (hasIncompleteReceipt) {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_PendingQtyOldVersion@");
        bundle.setResult(result);
        return;
      }

      Order objCloneOrder = (Order) DalUtil.copy(objOrder, false);
      objCloneOrder.setProcessed(false);
      // objCloneOrder.setEscmRevision(objOrder.getEscmRevision().longValue() + 1);
      if (objOrder.getEscmBaseOrder() == null) {
        Long revNo = POContractSummaryDAO.checkBaseOrder(objOrder.getId());
        objCloneOrder.setEscmRevision(revNo + 1);
      } else if (objOrder.getEscmBaseOrder() != null) {
        Long revNo = POContractSummaryDAO.getRevisionNo(objOrder.getEscmBaseOrder().getId());
        objCloneOrder.setEscmRevision(revNo + 1);
      }

      Order baseAgreement = null, latestAgreement = null;

      // get latest agreement version for new release.
      if (objOrder.isEscmIspurchaseagreement() && objOrder.getEscmOrdertype().equals("PUR_REL")) {
        Order agreement = objOrder.getEscmPurchaseagreement();
        if (agreement.getEscmBaseOrder() == null) {
          baseAgreement = agreement;
        } else {
          baseAgreement = agreement.getEscmBaseOrder();
        }
        List<Order> agreementList = null;
        OBQuery<Order> baseagreementQry = OBDal.getInstance().createQuery(Order.class,
            "as e where (e.escmBaseOrder.id=:baseOrder or e.id=:baseOrder) and e.escmAppstatus = 'ESCM_AP' order by e.escmRevision desc ");
        baseagreementQry.setNamedParameter("baseOrder", baseAgreement.getId());
        // baseagreementQry.getMaxResult();
        agreementList = baseagreementQry.list();
        latestAgreement = agreementList.get(0);
        objCloneOrder.setEscmPurchaseagreement(latestAgreement);
      }

      objCloneOrder.setEscmAppstatus("ESCM_RA");
      objCloneOrder.setDocumentStatus("DR");
      objCloneOrder.setEscmDocaction("CO");
      objCloneOrder.setDocumentAction("CO");
      objCloneOrder.setEscmOldOrder(objOrder);
      objCloneOrder.setEscmPaymentscheduleAmt(BigDecimal.ZERO);
      if (objOrder.getEscmBaseOrder() == null) {
        objCloneOrder.setEscmBaseOrder(objOrder);
      } else {
        objCloneOrder.setEscmBaseOrder(objOrder.getEscmBaseOrder());
      }
      // set budget definfition

      budInit = UtilityDAO.getBudgetInitialByUsingDateFormatGreg(dateFormat.format(new Date()),
          objOrder.getClient().getId());
      objCloneOrder.setGrandTotalAmount(BigDecimal.ZERO);
      objCloneOrder.setSummedLineAmount(BigDecimal.ZERO);
      objCloneOrder.setCreationDate(new Date());
      objCloneOrder.setUpdated(new Date());
      // Update session user id in Createdby field
      if (userId != null) {
        User usr = OBDal.getInstance().get(User.class, userId);
        if (usr != null) {
          objCloneOrder.setCreatedBy(usr);
        }
      }
      objCloneOrder.setEfinEncumbered(false);
      objCloneOrder.setEscmTotPoChangeType(null);
      objCloneOrder.setEscmTotPoChangeFactor(null);
      objCloneOrder.setEscmTotPoChangeValue(BigDecimal.ZERO);
      objCloneOrder.setEfinBudgetint(budInit);
      objCloneOrder.setEscmFinanyear(budInit.getYear());
      objCloneOrder.setEscmAdRole(OBContext.getOBContext().getRole());
      //copy the legacy org amount
      objCloneOrder.setEscmLegacyOrgAmount(objOrder.getEscmLegacyOrgAmount());
      OBDal.getInstance().save(objCloneOrder);

      // insert payment terms
      for (Escmorderpaymentterms objPayment : objOrder.getEscmOrderpaymenttermsList()) {
        Escmorderpaymentterms objClonePayment = (Escmorderpaymentterms) DalUtil.copy(objPayment,
            false);
        objClonePayment.setSalesOrder(objCloneOrder);
        objClonePayment.setCreationDate(new Date());
        objClonePayment.setUpdated(new Date());
        OBDal.getInstance().save(objClonePayment);
      }
      // insert distribution
      for (EscmproposalDistribution objDistribution : objOrder.getEscmPgmtDistributionList()) {
        EscmproposalDistribution objCloneDistribution = (EscmproposalDistribution) DalUtil
            .copy(objDistribution, false);
        objCloneDistribution.setDocumentNo(objCloneOrder);
        objCloneDistribution.setCreationDate(new Date());
        objCloneDistribution.setUpdated(new Date());
        OBDal.getInstance().save(objCloneDistribution);
      }
      // task no 6093
      // insert action history
      /*
       * for (EscmPurOrderActionHistory objActionHistory : objOrder.getEscmPurorderacthistList()) {
       * EscmPurOrderActionHistory objCloneActionHistory = (EscmPurOrderActionHistory) DalUtil
       * .copy(objActionHistory, false); objCloneActionHistory.setSalesOrder(objCloneOrder);
       * objCloneActionHistory.setCreationDate(new Date()); objCloneActionHistory.setUpdated(new
       * Date()); OBDal.getInstance().save(objCloneActionHistory); OBDal.getInstance().flush();
       * 
       * }
       */

      // insert orderline
      for (OrderLine objOrderLine : objOrder.getOrderLineList()) {
        if (!OBContext.getOBContext().isInAdministratorMode())
          OBContext.setAdminMode();
        BigDecimal lnNegPrice = BigDecimal.ZERO, expectedLineNetAmt = BigDecimal.ZERO,
            calGrossAmt = BigDecimal.ZERO, taxPercent = BigDecimal.ZERO;
        // BigDecimal grossPrice = BigDecimal.ZERO;
        if (objOrderLine.getEscmLineTaxamt() != null
            && objOrderLine.getEscmLineTaxamt().compareTo(BigDecimal.ZERO) != 0) {
          lnNegPrice = ((objOrderLine.getLineNetAmount().subtract(objOrderLine.getEscmLineTaxamt()))
              .divide(objOrderLine.getOrderedQuantity(), 4, BigDecimal.ROUND_HALF_UP));
          // grossPrice = lnNegPrice.multiply(objOrderLine.getOrderedQuantity());
        } else {
          lnNegPrice = objOrderLine.getEscmNetUnitprice();
          // grossPrice = objOrderLine.getUnitPrice();
        }

        childParentLineNoMap.put(objOrderLine.getLineNo(),
            objOrderLine.getEscmParentline() == null ? null
                : objOrderLine.getEscmParentline().getLineNo());

        if (objOrderLine.getESCMCancelledBy() == null) {
          /*
           * OBQuery<Escmordershipment> shipList = OBDal.getInstance()
           * .createQuery(Escmordershipment.class, " as e where e.salesOrderLine.id='" +
           * objOrderLine.getId() + "' and e.cancelledby is  null");
           */
          // if (shipList.list().size() > 0) {
          OrderLine objCloneOrderLine = (OrderLine) DalUtil.copy(objOrderLine, false);
          objCloneOrderLine.setSalesOrder(objCloneOrder);
          objCloneOrderLine.setEscmParentline(null);
          objCloneOrderLine.setCreationDate(new Date());
          objCloneOrderLine.setUpdated(new Date());
          // Update session user id in Createdby field
          if (userId != null) {
            User usr = OBDal.getInstance().get(User.class, userId);
            if (usr != null) {
              objCloneOrderLine.setCreatedBy(usr);
            }
          }
          objCloneOrderLine.setEscmOldOrderline(objOrderLine);
          objCloneOrderLine.setEscmPoChangeType(null);
          objCloneOrderLine.setEscmPoChangeFactor(null);
          objCloneOrderLine.setEscmPoChangeValue(BigDecimal.ZERO);
          objCloneOrderLine.setUnitPrice(lnNegPrice);
          objCloneOrderLine
              .setEscmLineTotalUpdated(lnNegPrice.multiply(objCloneOrderLine.getOrderedQuantity()));

          if (objOrder.getEscmOrdertype().equals("PUR_REL")) {
            POContractSummaryDAO.getLatestAgreementLine(latestAgreement,
                objOrderLine.getEscmAgreementLine(), objCloneOrderLine);
          }
          // Task No.

          objCloneOrderLine.setEscmUnitpriceAfterchag(BigDecimal.ZERO);

          if (objOrder.isEscmIstax() && objOrder.getEscmTaxMethod() != null) {
            taxPercent = new BigDecimal(objOrder.getEscmTaxMethod().getTaxpercent());
            calGrossAmt = objCloneOrderLine.getOrderedQuantity()
                .multiply(objCloneOrderLine.getEscmInitialUnitprice());

            if (objOrder.getEscmTaxMethod().isPriceIncludesTax()) {
              objCloneOrderLine.setEscmInitialUnitprice(objOrderLine.getEscmNetUnitprice());
              expectedLineNetAmt = calGrossAmt;
              objCloneOrderLine.setEscmRounddiffTax(
                  objCloneOrderLine.getLineNetAmount().subtract(expectedLineNetAmt));
            } else {
              objCloneOrderLine.setEscmInitialUnitprice(BigDecimal.ZERO);
              expectedLineNetAmt = calGrossAmt
                  .multiply(taxPercent.divide(new BigDecimal(100), 15, RoundingMode.HALF_UP));
              objCloneOrderLine.setEscmRounddiffTax(
                  objCloneOrderLine.getLineNetAmount().subtract(expectedLineNetAmt));
            }
          }

          OBDal.getInstance().save(objCloneOrderLine);
          OBDal.getInstance().flush();

          // insert shipment
          /*
           * for (Escmordershipment objOrderShipment : objOrderLine.getEscmOrdershipmentList()) { if
           * (objOrderShipment.getCancelledby() == null) { Escmordershipment objCloneOrderShipment =
           * (Escmordershipment) DalUtil .copy(objOrderShipment, false);
           * objCloneOrderShipment.setSalesOrderLine(objCloneOrderLine);
           * objCloneOrderShipment.setCreationDate(new Date()); objCloneOrderShipment.setUpdated(new
           * Date()); OBDal.getInstance().save(objCloneOrderShipment); } }
           */

          for (EscmOrderSourceRef objOrderSourceRef : objOrderLine.getEscmOrdersourceRefList()) {
            EscmOrderSourceRef objCloneOrderSourceRef = (EscmOrderSourceRef) DalUtil
                .copy(objOrderSourceRef, false);
            objCloneOrderSourceRef.setSalesOrderLine(objCloneOrderLine);
            objCloneOrderSourceRef.setCreationDate(new Date());
            objCloneOrderSourceRef.setUpdated(new Date());
            OBDal.getInstance().save(objCloneOrderSourceRef);
          }

          // }
        }
      }

      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(objCloneOrder);

      // Update header records
      for (OrderLine line : objCloneOrder.getOrderLineList()) {
        Long parentLineNo = childParentLineNoMap.get(line.getLineNo());
        if (parentLineNo != null) {
          OBQuery<OrderLine> parentLine = OBDal.getInstance().createQuery(OrderLine.class,
              " lineNo =:parentlineNo and salesOrder.id=:orderID ");
          parentLine.setNamedParameter("parentlineNo", parentLineNo);
          parentLine.setNamedParameter("orderID", objCloneOrder.getId());

          if (parentLine != null && parentLine.list().size() > 0) {
            line.setEscmParentline(
                Utility.getObject(EscmOrderlineV.class, parentLine.list().get(0).getId()));
            OBDal.getInstance().save(line);
          }
        }
      }
      String preferenceAttachValue = UtilityDAO.getAttachmentPref(
          Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY_W, objOrder.getClient().getId());

      if (preferenceAttachValue != null && preferenceAttachValue.equals("Y")) {
        OBQuery<Attachment> file = OBDal.getInstance().createQuery(Attachment.class,
            " as e where e.record=:recordId");
        file.setNamedParameter("recordId", objOrder.getId());
        fileList = file.list();
        if (fileList != null && fileList.size() > 0) {
          copyAttachmentDAO.getCopyAttachments(objOrder.getId(), objCloneOrder.getId(),
              Constants.PURCHASE_ORDER_T, objCloneOrder.getEscmRevision());
        }

      }

      // copy the payment schedule from the last version
      // when the last version po contract category lookup assigned with payment schedule
      ESCMDefLookupsTypeLn reflookuplnObj = OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
          objOrder.getEscmContactType().getId());
      if (reflookuplnObj.isPaymentschedule()) {
        Boolean isPaymentScheduledCopied = POContractSummaryDAO.copyPaymentSchedule(objOrder,
            objCloneOrder, vars);
        if (!isPaymentScheduledCopied) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_Not_Copied_PaymentSchedule@");
          bundle.setResult(result);
          return;
        }
      }

      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(objCloneOrder);

      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_version_Created@");
      bundle.setResult(result);
    } catch (Exception e) {
      log.debug("Exeception in Create New Version  Process:" + e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      if (OBContext.getOBContext().isInAdministratorMode())
        OBContext.restorePreviousMode();
    }
  }
}
