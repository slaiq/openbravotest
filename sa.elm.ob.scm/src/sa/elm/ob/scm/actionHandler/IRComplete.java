package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinRdvTxnLineRef;
import sa.elm.ob.scm.EscmInitialReceipt;
import sa.elm.ob.scm.webservice.constants.WebserviceConstants;
import sa.elm.ob.utility.util.Utility;

public class IRComplete implements Process {
  private final OBError obError = new OBError();
  private static Logger log4j = Logger.getLogger(IRComplete.class);

  @SuppressWarnings("resource")
  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    Connection connection = null;
    Connection conn = null;
    VariablesSecureApp vars = null;
    final String clientId = (String) bundle.getContext().getClient();
    HttpServletRequest request = RequestContext.get().getRequest();
    if (request != null) {
      vars = new VariablesSecureApp(request);
    } else {
      vars = new VariablesSecureApp(WebserviceConstants.DEFAULT_USER_ID, clientId,
          WebserviceConstants.DEFAULT_ORG_ID, WebserviceConstants.DEFAULT_ROLE_ID);
    }
    try {
      ConnectionProvider provider = bundle.getConnection();
      connection = provider.getConnection();
      conn = OBDal.getInstance().getConnection();
    } catch (NoConnectionAvailableException e) {
      log4j.error("No Database Connection Available.Exception:" + e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
      /* throw new RuntimeException(e); */
    }

    final String receiptId = (String) bundle.getParams().get("M_InOut_ID").toString();
    // final String clientId = (String) bundle.getContext().getClient();
    final String userId = (String) bundle.getContext().getUser();
    final String orgId = (String) bundle.getContext().getOrganization();
    log4j.debug("receiptId:" + receiptId);
    ShipmentInOutLine inoutline = null;
    String p_instance_id = null, sql = null;
    PreparedStatement ps = null;
    ResultSet rs = null, rs1 = null;
    boolean delflag = true;
    int count = 0;
    List<EscmInitialReceipt> irList = null;
    String receivingType = null;
    Boolean poIsAlreadyUsed = false;
    List<Locator> locList = new ArrayList<Locator>();
    boolean isNewVersionCreated = false;

    try {
      OBContext.setAdminMode(true);
      ShipmentInOut header = OBDal.getInstance().get(ShipmentInOut.class, receiptId);
      // Task#8180 : Not allow to complete the process when PO/Contract closed

      if (header.getSalesOrder() != null) {
        Order order = OBDal.getInstance().get(Order.class, header.getSalesOrder().getId());
        if ("ESCM_CL".equals(order.getEscmAppstatus())) {
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(OBMessageUtils.messageBD("ESCM_PO_Closed"));
          bundle.setResult(obError);
          return;
        }
      }

      OBQuery<EscmInitialReceipt> Ir = OBDal.getInstance().createQuery(EscmInitialReceipt.class,
          "goodsShipment.id=:receiptID");
      Ir.setNamedParameter("receiptID", receiptId);
      if ((Ir.list() == null || Ir.list().size() < 1)
          && header.getEscmInitialReceiptList().size() == 0) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(OBMessageUtils.messageBD("Escm_No_IR"));
        bundle.setResult(obError);
        return;
      } else {
        irList = header.getEscmInitialReceiptList();
      }

      // Initially update failure reason as empty

      for (EscmInitialReceipt intialReceipt : header.getEscmInitialReceiptList()) {
        intialReceipt.setFailurereason("");
        OBDal.getInstance().save(intialReceipt);
      }

      OBDal.getInstance().flush();

      // Check whether PO is already used in some other transaction type.
      if ((header.getEscmReceivingtype().equals("PROJ")
          || header.getEscmReceivingtype().equals("SR")
          || header.getEscmReceivingtype().equals("IR"))
          && header.getDocumentType().isEscmIsporeceipt() && header.getSalesOrder() != null) {
        poIsAlreadyUsed = IRCompleteDAO.checkPOIsAleardyUsed(header);
        if (poIsAlreadyUsed) {
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(OBMessageUtils.messageBD("ESCM_PO_AlreadyUsed"));
          bundle.setResult(obError);
          return;
        }
      }

      if (!header.getEscmReceivingtype().equals("SR")
          && !header.getEscmReceivingtype().equals("PROJ")
          && !header.getEscmReceivingtype().equals("RET")) {
        for (EscmInitialReceipt irLines : Ir.list()) {
          if (irLines.getProduct() == null && !irLines.isSummaryLevel()) {
            obError.setType("Error");
            obError.setTitle("Error");
            obError.setMessage(
                OBMessageUtils.messageBD("Escm_No_Item") + " at line no" + irLines.getLineNo());
            bundle.setResult(obError);
            return;
          }
        }
      }

      if (header.getEscmReceivingtype().equals("RET")) {
        for (EscmInitialReceipt irLines : Ir.list()) {
          if (irLines.getSourceRef() != null
              && irLines.getSourceRef().getGoodsShipment().getEscmReceivetype().equals("QTY")
              && irLines.getProduct() == null && !irLines.isSummaryLevel()) {
            obError.setType("Error");
            obError.setTitle("Error");
            obError.setMessage(
                OBMessageUtils.messageBD("Escm_No_Item") + " at line no" + irLines.getLineNo());
            bundle.setResult(obError);
            return;
          }
        }
      }
      if (header.getEscmReceivingtype().equals("SR")) {
        for (EscmInitialReceipt irLines : irList) {
          if (irLines.getProduct() == null && !irLines.isSummaryLevel() && irLines.isCustodySr()) {
            obError.setType("Error");
            obError.setTitle("Error");
            obError.setMessage(
                OBMessageUtils.messageBD("Escm_No_Item") + " at line no " + irLines.getLineNo());
            bundle.setResult(obError);
            return;
          }
        }
      }
      if (header.getEscmReceivingtype().equals("IR")) {
        for (EscmInitialReceipt irLines : Ir.list()) {
          if (irLines.getProduct() != null && irLines.getProduct().getProductType().equals("S")) {
            obError.setType("Error");
            obError.setTitle("Error");
            obError.setMessage(OBMessageUtils.messageBD("ESCM_Service_Product(Not Allowed)")
                + irLines.getLineNo());
            bundle.setResult(obError);
            return;
          }
        }
      }

      // Task No : 7541 - Throw error if PO is in status Withdrawn / Hold
      if (header.getSalesOrder() != null) {
        Order order = OBDal.getInstance().get(Order.class, header.getSalesOrder().getId());
        if ("ESCM_WD".equals(order.getEscmAppstatus())
            || "ESCM_OHLD".equals(order.getEscmAppstatus())) {
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(OBMessageUtils.messageBD("EUT_HoldWithdrawnPO"));
          bundle.setResult(obError);
          return;
        }
      }

      // Throw error if new version is created for selected PO and status is in Draft, In progress,
      // Require re-approval, Reject
      if (header.getSalesOrder() != null && ("IR".equals(header.getEscmReceivingtype())
          || "PROJ".equals(header.getEscmReceivingtype())
          || "SR".equals(header.getEscmReceivingtype()))) {
        isNewVersionCreated = IRCompleteDAO.isNewVersionCreated(header.getSalesOrder().getId());
        if (isNewVersionCreated) {
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(OBMessageUtils.messageBD("Escm_PoVersionCreated"));
          bundle.setResult(obError);
          return;
        }
      }

      // Check the period control is opened (only if it is legal entity with accounting)
      // Gets the BU or LE of the document

      /*
       * #Task No : 5183 removed Doc Date Validation
       */
      /*
       * sql = " SELECT AD_GET_DOC_LE_BU('M_INOUT', '" + receiptId +
       * "', 'M_INOUT_ID', 'LE') as org_blue_id FROM DUAL "; ps = conn.prepareStatement(sql);
       * log4j.debug("ps:" + ps.toString()); rs = ps.executeQuery(); if (rs.next()) { sql =
       * "   SELECT AD_OrgType.IsAcctLegalEntity       as  isacctle       FROM AD_OrgType, AD_Org  "
       * + "     WHERE AD_Org.AD_OrgType_ID = AD_OrgType.AD_OrgType_ID " +
       * "      AND AD_Org.AD_Org_ID=? "; ps1 = conn.prepareStatement(sql); ps1.setString(1,
       * rs.getString("org_blue_id")); log4j.debug("ps:" + ps1.toString()); rs1 =
       * ps1.executeQuery(); if (rs1.next()) { if (rs1.getString("isacctle").equals("Y")) { sql1 =
       * "     SELECT C_CHK_OPEN_PERIOD( '" + header.getOrganization().getId() + "', '" +
       * header.getAccountingDate() + "', NULL, '" + header.getDocumentType().getId() +
       * "') as avialbleperiod   FROM DUAL  "; ps2 = conn.prepareStatement(sql1); log4j.debug("ps:"
       * + ps2.toString()); rs = ps2.executeQuery(); if (rs.next()) { if
       * (rs.getInt("avialbleperiod") != 1) { throw new OBException("@PeriodNotAvailable@"); } } } }
       * }
       */

      // check po receipt have po reference.
      if (header.getEscmReceivingtype().equals("DEL")) {
        if (header.getDocumentType().isEscmIsporeceipt()) {
          if (header.getSalesOrder() == null) {
            obError.setType("Error");
            obError.setTitle("Error");
            obError.setMessage(OBMessageUtils.messageBD("Escm_Po_mandatory"));
            bundle.setResult(obError);
            return;
          }
        }
      }
      // if amount received greater than total line net amount
      // if quantity received greater than total line net quantity
      if (header.getEscmReceivingtype().equals("PROJ")) {
        if (header.getEscmReceivetype().equals("AMT")) {
          for (EscmInitialReceipt irLines : irList) {
            if (!irLines.isSummaryLevel() && irLines.getSalesOrderLine() != null) {
              OrderLine orderline = OBDal.getInstance().get(OrderLine.class,
                  irLines.getSalesOrderLine().getId());
              BigDecimal totalLineamount = orderline.getLineNetAmount()
                  .subtract((orderline.getEscmAmtporec() == null ? BigDecimal.ZERO
                      : orderline.getEscmAmtporec()
                          .subtract(orderline.getEscmAmtreturned() == null ? BigDecimal.ZERO
                              : orderline.getEscmAmtreturned())));

              BigDecimal amtCancelled = orderline.getEscmAmtcanceled() != null
                  ? orderline.getEscmAmtcanceled()
                  : BigDecimal.ZERO;
              BigDecimal legacyDelAmt = orderline.getEscmLegacyAmtDelivered() != null
                  ? orderline.getEscmLegacyAmtDelivered()
                  : BigDecimal.ZERO;

              totalLineamount = totalLineamount.subtract(amtCancelled).subtract(legacyDelAmt);
              if (irLines.getReceivedAmount().compareTo(totalLineamount) > 0) {
                obError.setType("Error");
                obError.setTitle("Error");
                obError.setMessage(OBMessageUtils.messageBD("Escm_RecAmt_Greater"));
                bundle.setResult(obError);
                return;
              }
            }
          }
        } else if (header.getEscmReceivingtype().equals("PROJ")) {
          for (EscmInitialReceipt irLines : irList) {
            if (!irLines.isSummaryLevel() && irLines.getSalesOrderLine() != null) {
              OrderLine orderline = OBDal.getInstance().get(OrderLine.class,
                  irLines.getSalesOrderLine().getId());
              BigDecimal qtyreceived = orderline.getEscmQtyporec()
                  .subtract(orderline.getEscmQtyreturned()
                      .add(orderline.getEscmQtyirr().add(orderline.getEscmQtyrejected())));

              BigDecimal qtyCancelled = orderline.getEscmQtycanceled() != null
                  ? orderline.getEscmQtycanceled()
                  : BigDecimal.ZERO;
              BigDecimal legacyDelQty = orderline.getEscmLegacyQtyDelivered() != null
                  ? orderline.getEscmLegacyQtyDelivered()
                  : BigDecimal.ZERO;

              BigDecimal totalLinequantity = orderline.getOrderedQuantity().subtract(qtyreceived)
                  .subtract(qtyCancelled).subtract(legacyDelQty);

              if (irLines.getQuantity().compareTo(totalLinequantity) > 0) {
                obError.setType("Error");
                obError.setTitle("Error");
                obError.setMessage(OBMessageUtils.messageBD("Escm_RecQty_Greater"));
                bundle.setResult(obError);
                return;
              }
            }
          }
        }
      } else {
        if (header.getEscmReceivingtype().equals("SR") || header.getEscmReceivingtype().equals("IR")
            || header.getEscmReceivingtype().equals("DEL")) {
          for (EscmInitialReceipt irLines : irList) {

            if (!irLines.isSummaryLevel() && irLines.getSalesOrderLine() != null) {
              OrderLine orderline = OBDal.getInstance().get(OrderLine.class,
                  irLines.getSalesOrderLine().getId());
              BigDecimal qtyreceived = orderline.getEscmQtyporec()
                  .subtract(orderline.getEscmQtyreturned()
                      .add(orderline.getEscmQtyirr().add(orderline.getEscmQtyrejected())));

              BigDecimal qtyCancelled = orderline.getEscmQtycanceled() != null
                  ? orderline.getEscmQtycanceled()
                  : BigDecimal.ZERO;
              BigDecimal legacyDelQty = orderline.getEscmLegacyQtyDelivered() != null
                  ? orderline.getEscmLegacyQtyDelivered()
                  : BigDecimal.ZERO;

              BigDecimal totalLinequantity = orderline.getOrderedQuantity().subtract(qtyreceived)
                  .subtract(qtyCancelled).subtract(legacyDelQty);

              if ((irLines.getQuantity().compareTo(totalLinequantity) > 0
                  && !header.getEscmReceivingtype().equals("DEL"))
                  || (header.getEscmReceivingtype().equals("DEL")
                      && totalLinequantity.compareTo(BigDecimal.ZERO) < 0)) {
                obError.setType("Error");
                obError.setTitle("Error");
                obError.setMessage(OBMessageUtils.messageBD("Escm_RecQty_Greater"));
                bundle.setResult(obError);
                return;
              }
            }
          }
        }
      }

      OBQuery<EscmInitialReceipt> receipt = OBDal.getInstance()
          .createQuery(EscmInitialReceipt.class, "goodsShipment.id=:receiptID");
      receipt.setNamedParameter("receiptID", receiptId);
      List<EscmInitialReceipt> receiptList = new ArrayList<EscmInitialReceipt>();
      receiptList = receipt.list();
      if (header.isEscmIscreatedfromws()) {
        receiptList = header.getEscmInitialReceiptList();
      }
      if (header.getEscmReceivingtype().equals("SR")
          || header.getEscmReceivingtype().equals("PROJ")) {
        for (EscmInitialReceipt receipts : header.getEscmInitialReceiptList()) {
          List<EfinRdvTxnLineRef> ref = receipts.getEfinRdvTxnLineRefList();
          for (EfinRdvTxnLineRef poRef : ref) {
            poRef.setAvailableQty(poRef.getAvailableQty().add(receipts.getQuantity()));
            OBDal.getInstance().save(poRef);
          }
        }
      }

      if (header.getEscmReceivingtype().equals("SR")
          || header.getEscmReceivingtype().equals("PROJ")) {

        IRCompleteDAO.updateRemainingQtyAndAmt(header);

        for (EscmInitialReceipt ir : receiptList) {

          if (ir.getSalesOrderLine() != null) {
            OrderLine ordship = OBDal.getInstance().get(OrderLine.class,
                ir.getSalesOrderLine().getId());
            if (header.getEscmReceivingtype().equals("SR")) {
              ordship.setEscmQtyporec(ordship.getEscmQtyporec().add(ir.getQuantity()));
            }
            if (header.getEscmReceivingtype().equals("PROJ")) {
              if (header.getEscmReceivetype().equals("AMT")) {
                ordship.setEscmAmtporec(ordship.getEscmAmtporec() == null ? BigDecimal.ZERO
                    : ordship.getEscmAmtporec().add(
                        ir.getReceivedAmount() == null ? BigDecimal.ZERO : ir.getReceivedAmount()));
              } else {
                ordship.setEscmQtyporec(ordship.getEscmQtyporec().add(ir.getQuantity()));
              }
            }

          }
          if (header.getEscmReceivingtype().equals("SR")) {// changes
            if (header.getEscmReceivetype().equals("AMT")) {
              ir.setDeliveredQty(ir.getQuantity());
              ir.setDeliveredAmt(ir.getTOTLineAmt());
            } else {
              ir.setDeliveredQty(ir.getQuantity());
            }
          }
          if (header.getEscmReceivingtype().equals("PROJ")) {
            if (header.getEscmReceivetype().equals("AMT")) {
              OrderLine ordline = OBDal.getInstance().get(OrderLine.class,
                  ir.getSalesOrderLine().getId());
              ir.setDeliveredQty(ordline.getOrderedQuantity());
              ir.setDeliveredAmt(ir.getTOTLineAmt());
            } else {
              ir.setDeliveredQty(ir.getQuantity());
            }
          }
          OBDal.getInstance().save(ir);
        }
      } else if (header.getEscmReceivingtype().equals("IR")) {

        // check quantity receiving is less than or equal to quantity due
        String error = checkPendingQuantity(connection, header.getClient().getId(),
            header.getOrganization().getId(), header, bundle, vars);

        if (error != null) {
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(OBMessageUtils.messageBD("ESCM_Porec_Qtygreaterthandue")
              .replace("@", error).replace("{", "").replace("}", ""));
          bundle.setResult(obError);
          return;
        }
        // update remaining qty and remaining amount
        IRCompleteDAO.updateRemainingQtyAndAmt(header);

        for (EscmInitialReceipt ir : receiptList) {
          if (ir.getSalesOrderLine() != null) {
            OrderLine ordship = OBDal.getInstance().get(OrderLine.class,
                ir.getSalesOrderLine().getId());

            ordship.setEscmQtyporec(ordship.getEscmQtyporec().add(ir.getQuantity()));
            OBDal.getInstance().save(ordship);
          }
          if (ir.getProduct() != null) {
            if (ir.getProduct().isStocked()) {
              if (ir.getProduct().isEscmNoinspection()) {
                ir.setAcceptedQty(ir.getQuantity());
              }
            } else {
              ir.setDeliveredQty(ir.getQuantity());
            }
          } else {
            ir.setAcceptedQty(ir.getQuantity());
          }

          OBDal.getInstance().save(ir);
        }
      } else if (header.getEscmReceivingtype().equals("INS")) {
        int inscount = updateInitialRecipt(connection, header.getClient().getId(),
            header.getOrganization().getId(), header, bundle, vars);
        log4j.debug("count:" + inscount);
        if (inscount == 2) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_ProcessFailed(Reason)@");
          bundle.setResult(result);
          return;
        } else if (inscount == 1) {
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          header.setEscmDocstatus("CO");
          header.setDocumentStatus("CO");
          header.setProcessed(true);
          header.setDocumentAction("--");
          OBDal.getInstance().save(header);
          OBDal.getInstance().flush();
          return;
        } else {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_ProcessFailed@");
          bundle.setResult(result);
          return;
        }
      } else if (header.getEscmReceivingtype().equals("DEL")) {

        sql = " select coalesce(sum(mr.accepted_qty),0) as accqty,coalesce(sum(ur.deliveredqty),0) as deliveredqty, "
            + " coalesce(sum(cr.crqty),0) as crqty ,cr.source_ref  from escm_initialreceipt mr "
            + " left join (select source_ref,sum(quantity)as deliveredqty  from escm_initialreceipt ur join m_inout io on io.m_inout_id=ur.m_inout_id "
            + " where io.em_escm_receivingtype ='DEL' and  io.em_escm_docstatus='CO' and ur.issummary ='N'  group by source_ref ) as ur on ur.source_ref=mr.escm_initialreceipt_id "
            + " left join (select sum(quantity) as crqty,source_ref from escm_initialreceipt  where m_inout_id= ? "
            + " and issummary ='N' group by source_ref) as cr on cr.source_ref=mr.escm_initialreceipt_id "
            + " where mr.escm_initialreceipt_id in (select distinct source_ref from escm_initialreceipt  where m_inout_id= ? ) "
            + " and mr.issummary ='N' group by  cr.source_ref having (coalesce(sum(cr.crqty),0))  > coalesce(sum(mr.accepted_qty),0) ";
        ps = conn.prepareStatement(sql);
        ps.setString(1, header.getId());
        ps.setString(2, header.getId());
        log4j.debug("ps:" + ps.toString());
        rs = ps.executeQuery();
        while (rs.next()) {
          OBQuery<EscmInitialReceipt> erroreceiptQry = OBDal.getInstance().createQuery(
              EscmInitialReceipt.class,
              "as e where e.sourceRef.id=:srcRefId and e.goodsShipment.id=:headerId ");
          erroreceiptQry.setNamedParameter("srcRefId", rs.getString("source_ref"));
          erroreceiptQry.setNamedParameter("headerId", header.getId());

          if (erroreceiptQry.list().size() > 0) {
            for (EscmInitialReceipt lineObject : erroreceiptQry.list()) {
              lineObject.setFailurereason(
                  "Quantity Exceed,Availabe Quantity to Delivery:'" + rs.getInt("accqty") + "'");
              OBDal.getInstance().save(lineObject);
            }
          }
          delflag = false;
        }
        log4j.debug("count:" + count);
        Map<ShipmentInOutLine, EscmInitialReceipt> deliveryMapper = new HashMap<ShipmentInOutLine, EscmInitialReceipt>();
        if (delflag) {
          for (EscmInitialReceipt initial : header.getEscmInitialReceiptList()) {
            // update stock only for deliverable(issummary='N')

            if (!initial.isSummaryLevel()) {
              inoutline = OBProvider.getInstance().get(ShipmentInOutLine.class);
              inoutline.setClient(header.getClient());

              inoutline.setOrganization(header.getOrganization());
              inoutline.setCreationDate(new java.util.Date());
              inoutline.setCreatedBy(OBDal.getInstance().get(User.class, userId));
              inoutline.setUpdated(new java.util.Date());
              inoutline.setUpdatedBy(OBDal.getInstance().get(User.class, userId));
              inoutline.setActive(true);
              inoutline.setShipmentReceipt(header);
              inoutline.setLineNo(initial.getLineNo());
              inoutline.setProduct(initial.getProduct());
              inoutline.setEscmUnitprice(initial.getUnitprice());
              inoutline.setUOM(initial.getUOM());
              inoutline.setDescription(initial.getDescription());
              inoutline.setMovementQuantity(initial.getQuantity());
              inoutline.setEscmCustodyItem(initial.isCustodyItem());
              inoutline.setEscmTransaction("A");
              OBQuery<Locator> locator = OBDal.getInstance().createQuery(Locator.class,
                  " as e where e.warehouse.id=:warehouseId and e.default='Y' ");
              locator.setNamedParameter("warehouseId", header.getWarehouse().getId());
              locator.setMaxResult(1);
              locList = locator.list();
              if (locator.list().size() > 0) {
                inoutline.setStorageBin(locList.get(0));
                log4j.debug("getStorageBin:" + inoutline.getStorageBin());

              } else
                inoutline.setStorageBin(null);
              inoutline.setEscmInitialreceipt(initial);
              OBDal.getInstance().save(inoutline);
              OBDal.getInstance().flush();

              EscmInitialReceipt updinitial = OBDal.getInstance().get(EscmInitialReceipt.class,
                  initial.getSourceRef().getId());
              deliveryMapper.put(inoutline, updinitial);

            }
          }

          if (inoutline.getId() != null) {
            log4j.debug("entering into instance Id:");
            p_instance_id = SequenceIdData.getUUID();
            String error = "", s = "";

            log4j.debug("p_instance_id:" + p_instance_id);
            sql = " INSERT INTO ad_pinstance (ad_pinstance_id, ad_process_id, record_id, isactive, ad_user_id, ad_client_id, ad_org_id, created, createdby, updated, updatedby,isprocessing)  "
                + "  VALUES ('" + p_instance_id + "', '708D305269134EBF8A92E107D7EB6443','"
                + header.getId() + "', 'Y','" + userId + "','" + clientId + "','" + orgId
                + "', now(),'" + userId + "', now(),'" + userId + "','Y')";
            ps = conn.prepareStatement(sql);
            log4j.debug("ps:" + ps.toString());
            count = ps.executeUpdate();
            log4j.debug("count:" + count);

            String instanceqry = "select ad_pinstance_id from ad_pinstance where ad_pinstance_id=?";
            PreparedStatement pr = conn.prepareStatement(instanceqry);
            pr.setString(1, p_instance_id);
            ResultSet set = pr.executeQuery();

            if (set.next()) {

              sql = " select * from  m_inout_post(?,?)";
              ps = conn.prepareStatement(sql);
              ps.setString(1, p_instance_id);
              ps.setString(2, header.getId());

              // ps.setString(2, invoice.getId());
              ps.executeQuery();

              log4j.debug("count12:" + set.getString("ad_pinstance_id"));

              sql = " select result, errormsg from ad_pinstance where ad_pinstance_id='"
                  + p_instance_id + "'";
              ps = conn.prepareStatement(sql);
              log4j.debug("ps12:" + ps.toString());
              rs = ps.executeQuery();
              if (rs.next()) {
                log4j.debug("result:" + rs.getString("result"));

                if (rs.getString("result").equals("0")) {
                  error = rs.getString("errormsg").replace("@ERROR=", "");
                  log4j.debug("error:" + error);
                  s = error;
                  /*
                   * int start = s.indexOf("@"); int end = s.lastIndexOf("@");
                   * 
                   * if (log4j.isDebugEnabled()) { log4j.debug("start:" + start); log4j.debug("end:"
                   * + end); }
                   * 
                   * if (end != 0) { sql = " select  msgtext from ad_message where value ='" +
                   * s.substring(start + 1, end) + "'"; ps = conn.prepareStatement(sql);
                   * log4j.debug("ps12:" + ps.toString()); rs = ps.executeQuery(); if (rs.next()) {
                   * if (rs.getString("msgtext") != null) throw new OBException(error); } }
                   */

                  String[] errMsgArray = s.split(" ");
                  StringBuilder errorBuilder = new StringBuilder();
                  int i = 0;
                  for (String str : errMsgArray) {
                    if (str.startsWith("@") && str.endsWith("@")) {
                      sql = " select  msgtext from ad_message where value ='"
                          + errMsgArray[i].substring(1, errMsgArray[i].length() - 1) + "'";
                      ps = conn.prepareStatement(sql);
                      log4j.debug("ps12:" + ps.toString());
                      rs = ps.executeQuery();
                      if (rs.next()) {
                        if (rs.getString("msgtext") != null)
                          str = rs.getString("msgtext");
                      }
                    }
                    errorBuilder.append(" ").append(str);
                    i++;
                  }
                  /* throw new OBException(errorBuilder.toString()); */

                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      errorBuilder.toString());
                  bundle.setResult(result);
                  return;

                } else if (rs.getString("result").equals("1")) {
                  IRCompleteDAO.updateDeliveredQty(deliveryMapper);

                  for (ShipmentInOutLine line : header.getMaterialMgmtShipmentInOutLineList()) {
                    OBQuery<MaterialTransaction> transaction = OBDal.getInstance().createQuery(
                        MaterialTransaction.class, " goodsShipmentLine.id='" + line.getId() + "'");
                    if (transaction.list().size() > 0) {
                      MaterialTransaction trans = transaction.list().get(0);
                      trans.setEscmTransactiontype("RDR");
                      trans.setEscmInitialreceipt(line.getEscmInitialreceipt());
                      OBDal.getInstance().save(trans);
                    }
                  }

                  OBDal.getInstance().flush();
                }
              }
            }
          }

          log4j.debug("inoutline:" + inoutline.getMovementQuantity());
        } else {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_ProcessFailed(Reason)@");
          bundle.setResult(result);
          return;
        }
      } else if (header.getEscmReceivingtype().equals("RET")) {

        OBQuery<EscmInitialReceipt> IR = OBDal.getInstance().createQuery(EscmInitialReceipt.class,
            "goodsShipment.id =:receiptID ");
        IR.setNamedParameter("receiptID", header.getId());
        Boolean ErrorFlag = false;
        String defaultBin = "";
        // Date currentDate = new Date();
        List<EscmInitialReceipt> returnvendor = new ArrayList<EscmInitialReceipt>();
        List<EscmInitialReceipt> errorlist = new ArrayList<EscmInitialReceipt>();
        List<EscmInitialReceipt> successlist = new ArrayList<EscmInitialReceipt>();
        returnvendor = IR.list();
        for (EscmInitialReceipt ret : returnvendor) {
          EscmInitialReceipt initial = OBDal.getInstance().get(EscmInitialReceipt.class,
              ret.getSourceRef().getId());
          receivingType = initial.getGoodsShipment().getEscmReceivetype();
          if (ret.getAlertStatus().equals("I")) {
            // validaiton
            if (!ret.isSummaryLevel()) {
              if ((initial.getQuantity().subtract(initial.getReturnQuantity()))
                  .compareTo(ret.getQuantity()) < 0) {
                errorlist.add(ret);
                ErrorFlag = true;
              } else {
                successlist.add(ret);
              }
            }
          } else if (ret.getAlertStatus().equals("A")) {
            // validaiton
            if (!ret.isSummaryLevel()) {
              if (initial.getAcceptedQty().compareTo(ret.getQuantity()) < 0) {
                errorlist.add(ret);
                ErrorFlag = true;
              } else {
                successlist.add(ret);
              }
            }
          } else if (ret.getAlertStatus().equals("R")) {
            // validaiton
            if (!ret.isSummaryLevel()) {
              if (initial.getRejectedQty().compareTo(ret.getQuantity()) < 0) {
                errorlist.add(ret);
                ErrorFlag = true;
              } else {
                successlist.add(ret);
              }
            }
          } else if (ret.getAlertStatus().equals("D")) {
            // get sum of deliverqty
            BigDecimal delQty = BigDecimal.ZERO;
            BigDecimal delAmt = BigDecimal.ZERO;
            OBQuery<EscmInitialReceipt> lines = OBDal.getInstance().createQuery(
                EscmInitialReceipt.class,
                " goodsShipment.id=:headerId and alertStatus='D' and sourceRef.id=:srcref and description=:desc ");
            lines.setNamedParameter("headerId", header.getId());
            lines.setNamedParameter("srcref", initial.getId());
            lines.setNamedParameter("desc", ret.getDescription());
            if (!receivingType.equals("AMT")) {
              for (EscmInitialReceipt iniRec : lines.list()) {
                delQty = delQty.add(iniRec.getQuantity());
              }
            } else if (receivingType.equals("AMT")) {
              for (EscmInitialReceipt iniRec : lines.list()) {
                delAmt = delAmt.add(iniRec.getTOTLineAmt());
              }
            }

            if (!receivingType.equals("AMT")) {
              // validation
              if (!ret.isSummaryLevel()) {
                if (initial.getDeliveredQty().compareTo(ret.getQuantity()) < 0) {
                  errorlist.add(ret);
                  ErrorFlag = true;
                } else {
                  successlist.add(ret);
                }
              }
              if (!ErrorFlag) {
                // chk receipt used in RDV -( based on lines reference)
                if (!ret.isSummaryLevel()) {
                  if (initial.getDeliveredQty().subtract(initial.getMatchQty())
                      .compareTo(delQty) < 0) {
                    OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_UsedinRdv@");
                    bundle.setResult(result);
                    return;
                  }
                }
              }

            } else if (receivingType.equals("AMT")) {
              // chk receipt used in RDV -( based on lines reference)
              if (!ret.isSummaryLevel()) {
                if (initial.getDeliveredAmt().subtract(initial.getMatchAmt())
                    .compareTo(delAmt) < 0) {
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@ESCM_AmtRet_GreaterAmtDel@");
                  bundle.setResult(result);
                  return;
                }
              }

              // validaiton
              if (!ret.isSummaryLevel()) {
                if (initial.getDeliveredAmt().compareTo(ret.getTOTLineAmt()) < 0) {
                  errorlist.add(ret);
                  ErrorFlag = true;
                } else {
                  successlist.add(ret);
                }
              }
            }

            // update available qty in po reference rdv
            for (EfinRdvTxnLineRef ref : initial.getEfinRdvTxnLineRefList()) {
              if (!receivingType.equals("AMT")) {
                ref.setAvailableQty(ref.getAvailableQty().subtract(ret.getQuantity()));
              } else if (receivingType.equals("AMT")) {
                ref.setAvailableAmt(ref.getAvailableAmt().subtract(ret.getTOTLineAmt()));
              }
              OBDal.getInstance().save(ref);
            }

          }
        }
        if (ErrorFlag) {
          for (EscmInitialReceipt ret : errorlist) {
            if (ret.getAlertStatus().equals("I")) {
              ret.setFailurereason(
                  OBMessageUtils.messageBD("Escm_Qty_available").replace("xxx", "Initial receipt"));
            } else if (ret.getAlertStatus().equals("A")) {
              ret.setFailurereason(
                  OBMessageUtils.messageBD("Escm_Qty_available").replace("xxx", "Accepted"));
            } else if (ret.getAlertStatus().equals("R")) {
              ret.setFailurereason(
                  OBMessageUtils.messageBD("Escm_Qty_available").replace("xxx", "Rejected"));
            } else if (ret.getAlertStatus().equals("D")) {
              if (!receivingType.equals("AMT")) {
                ret.setFailurereason(
                    OBMessageUtils.messageBD("Escm_Qty_available").replace("xxx", "Delivered"));
              } else if (receivingType.equals("AMT")) {
                ret.setFailurereason(
                    OBMessageUtils.messageBD("Escm_Amount_available").replace("xxx", "Delivered"));
              }
            }
            OBDal.getInstance().save(ret);
          }
          OBDal.getInstance().flush();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_ProcessFailed(Reason)@");
          bundle.setResult(result);
          return;
        } else {
          for (EscmInitialReceipt ret : successlist) {
            EscmInitialReceipt initial = OBDal.getInstance().get(EscmInitialReceipt.class,
                ret.getSourceRef().getId());

            if (ret.getAlertStatus().equals("I")) {
              initial.setReturnQuantity((initial.getReturnQuantity().add(ret.getQuantity())));
              // initial.setAcceptedQty(initial.getAcceptedQty().subtract(ret.getQuantity()));
            } else if (ret.getAlertStatus().equals("A")) {
              initial.setReturnQty((initial.getReturnQty().add(ret.getQuantity())));
              initial.setAcceptedQty(initial.getAcceptedQty().subtract(ret.getQuantity()));
            } else if (ret.getAlertStatus().equals("R")) {
              initial.setReturnQty((initial.getReturnQty().add(ret.getQuantity())));
              initial.setRejectedQty(initial.getRejectedQty().subtract(ret.getQuantity()));
            } else if (ret.getAlertStatus().equals("D")) {
              if (initial.getGoodsShipment().getEscmReceivingtype().equals("SR")
                  || (initial.getGoodsShipment().getEscmReceivingtype().equals("PROJ"))) {
                if (initial.getGoodsShipment().getEscmReceivetype() != null
                    && (initial.getGoodsShipment().getEscmReceivetype().equals("AMT"))) {
                  initial.setReturnAmt((initial.getReturnAmt().add(ret.getTOTLineAmt())));
                  initial.setDeliveredAmt(initial.getDeliveredAmt().subtract(ret.getTOTLineAmt()));
                } else if (initial.getGoodsShipment().getEscmReceivetype() != null
                    && (initial.getGoodsShipment().getEscmReceivetype().equals("QTY"))) {
                  initial.setReturnQty((initial.getReturnQty().add(ret.getQuantity())));
                  initial.setDeliveredQty(initial.getDeliveredQty().subtract(ret.getQuantity()));
                } else {
                  initial.setReturnQty((initial.getReturnQty().add(ret.getQuantity())));
                  initial.setDeliveredQty(initial.getDeliveredQty().subtract(ret.getQuantity()));
                }
              } else if (initial.getGoodsShipment().getEscmReceivingtype().equals("PROJ")) {
                if (initial.getGoodsShipment().getEscmReceivetype().equals("AMT")) {
                  initial.setReturnAmt((initial.getReturnAmt().add(ret.getTOTLineAmt())));
                  initial.setDeliveredAmt(initial.getDeliveredAmt().subtract(ret.getTOTLineAmt()));
                } else {
                  initial.setReturnQty((initial.getReturnQty().add(ret.getQuantity())));
                  initial.setDeliveredQty(initial.getDeliveredQty().subtract(ret.getQuantity()));
                }
              } else {
                initial.setReturnQty((initial.getReturnQty().add(ret.getQuantity())));
                initial.setDeliveredQty(initial.getDeliveredQty().subtract(ret.getQuantity()));

                Product product = OBDal.getInstance().get(Product.class,
                    initial.getProduct().getId());
                if (product.isStocked()) {
                  // checking the stock of product.
                  ps = conn.prepareStatement(" select * from M_Check_Stock(?,?,?)");
                  ps.setString(1, product.getId());
                  ps.setString(2, initial.getClient().getId());
                  ps.setString(3, initial.getOrganization().getId());
                  rs = ps.executeQuery();
                  if (rs.next()) {
                    if (rs.getInt("p_result") == 0) {
                      String errormsg = rs.getString("p_message") + " "
                          + OBMessageUtils.messageBD("@line@") + " "
                          + initial.getSourceRef().getLineNo() + " " + product.getName();
                      OBError result = OBErrorBuilder.buildMessage(null, "error", errormsg);
                      bundle.setResult(result);
                      OBDal.getInstance().rollbackAndClose();
                      return;
                    }
                  }
                  // doubt to reduce qunts in ir or return data.
                  /*
                   * OBQuery<ShipmentInOutLine> finalreceipt = OBDal.getInstance().createQuery(
                   * ShipmentInOutLine.class, "shipmentReceipt.id='" +
                   * initial.getGoodsShipment().getId() + "'");
                   */

                  // insert into m_inoutline
                  ShipmentInOutLine sline = OBProvider.getInstance().get(ShipmentInOutLine.class);
                  sline.setClient(ret.getClient());
                  sline.setLineNo(ret.getLineNo());
                  sline.setOrganization(ret.getOrganization());
                  sline.setCreationDate(new java.util.Date());
                  sline.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
                  sline.setUpdated(new java.util.Date());
                  sline.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
                  sline.setActive(true);
                  sline.setShipmentReceipt(header);
                  sline.setUOM(ret.getUOM());
                  sline.setProduct(ret.getProduct());
                  sline.setMovementQuantity(ret.getQuantity());
                  sline.setEscmInitialreceipt(ret);
                  OBDal.getInstance().save(sline);
                  OBDal.getInstance().flush();
                  // insert in product transaction to update storage details
                  MaterialTransaction trans = OBProvider.getInstance()
                      .get(MaterialTransaction.class);
                  trans.setOrganization(ret.getOrganization());
                  trans.setClient(ret.getClient());
                  trans.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
                  trans.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
                  trans.setCreationDate(new java.util.Date());
                  trans.setUpdated(new java.util.Date());
                  trans.setMovementType("V-");
                  trans.setEscmTransactiontype("RER");
                  trans.setEscmInitialreceipt(ret);
                  defaultBin = Utility.GetDefaultBin(header.getWarehouse().getId());
                  if (defaultBin.equals("")) {
                    OBError result = OBErrorBuilder.buildMessage(null, "error",
                        "@Escm_No_DefaultBin@");
                    bundle.setResult(result);
                    OBDal.getInstance().rollbackAndClose();
                    return;
                  } else {
                    Locator locator = OBDal.getInstance().get(Locator.class, defaultBin);
                    trans.setStorageBin(locator);
                  }
                  /*
                   * OBQuery<Locator> locator = OBDal.getInstance().createQuery( Locator.class,
                   * " as e where e.warehouse.id='" + header.getWarehouse().getId() +
                   * "' and e.default='Y' "); locator.setMaxResult(1); if (locator.list().size() >
                   * 0) { trans.setStorageBin(locator.list().get(0)); log4j.debug("getStorageBin:" +
                   * trans.getStorageBin()); } else { OBError result =
                   * OBErrorBuilder.buildMessage(null, "error", "@Escm_No_DefaultBin@");
                   * bundle.setResult(result); OBDal.getInstance().rollbackAndClose(); return; }
                   */
                  int chkqty = Utility.ChkStoragedetOnhandQtyNeg(initial.getProduct().getId(),
                      defaultBin, ret.getQuantity(), ret.getClient().getId());
                  log4j.debug("chkqty:" + chkqty);
                  if (chkqty == -1) {

                    OBError result = OBErrorBuilder.buildMessage(null, "error",
                        "@ESCM_StorageDetail_QtyonHand@");
                    bundle.setResult(result);
                    OBDal.getInstance().rollbackAndClose();
                    String msg = OBMessageUtils.messageBD("ESCM_AvaQtyZero");
                    log4j.debug("msg:" + msg);
                    ret.setFailurereason(msg);
                    OBDal.getInstance().save(ret);
                    OBDal.getInstance().flush();
                    return;
                  }
                  trans.setProduct(initial.getProduct());
                  trans.setMovementDate(header.getMovementDate());
                  trans.setMovementQuantity(ret.getQuantity().negate());
                  // insert in m_inoutline and use refernece here
                  trans.setGoodsShipmentLine(sline);
                  trans.setUOM(OBDal.getInstance().get(UOM.class, initial.getUOM().getId()));
                  OBDal.getInstance().save(trans);
                  OBDal.getInstance().flush();
                }
              }
            }
            OBDal.getInstance().save(initial);
          }
          OBDal.getInstance().flush();
        }

      }
      log4j.debug("header:" + header);
      header.setEscmDocstatus("CO");
      header.setDocumentStatus("CO");
      header.setProcessed(true);
      header.setDocumentAction("--");
      OBDal.getInstance().save(header);
      OBDal.getInstance().flush();
      obError.setType("Success");
      obError.setTitle("Success");
      obError.setMessage(OBMessageUtils.messageBD("Escm_Ir_complete_success"));
      bundle.setResult(obError);
      if (!header.isEscmIscreatedfromws()) {
        OBDal.getInstance().commitAndClose();
      }
      return;
    } catch (Exception e) {
      log4j.error("Exeception in IRComplete:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      // close connection
      try {
        if (rs != null) {
          rs.close();
        }
        if (rs1 != null) {
          rs1.close();
        }
        if (ps != null) {
          ps.close();
        }
      } catch (Exception e) {

      }
      OBContext.restorePreviousMode();
    }
  }

  private static int updateInitialRecipt(Connection con, String clientId, String orgId,
      ShipmentInOut objInout, ProcessBundle bundle, VariablesSecureApp vars) {
    String objId = objInout.getId();
    int count = 1;
    String query = "", query1 = "", query2 = "";
    PreparedStatement ps1 = null, ps = null, ps2 = null;
    Boolean isProceed = true;
    ResultSet rs = null, rs1 = null, rs2 = null;

    try {
      OBContext.setAdminMode(true);
      query = " select coalesce((sum(mr.quantity)-sum(mr.ir_return_qty)-sum(mr.return_qty)),0) as irqty, coalesce(sum(mr.accepted_qty),0)+coalesce(sum(mr.rejected_qty),0)  as insqty ,"
          + " coalesce(sum(ur.insqty),0) as inspecedqty, "
          + " coalesce(sum(cr.crqty),0) as crqty ,cr.source_ref from escm_initialreceipt mr "
          + " left join (select source_ref,sum(quantity) as insqty "
          + " from escm_initialreceipt ur join m_inout io on io.m_inout_id=ur.m_inout_id "
          + " where io.em_escm_receivingtype ='INS' and io.em_escm_docstatus='CO' and issummary='N' "
          + " group by source_ref) as ur on ur.source_ref=mr.escm_initialreceipt_id "
          + " left join (select sum(quantity) as crqty,source_ref from escm_initialreceipt  "
          + " where m_inout_id='" + objId
          + "' and issummary='N'  group by source_ref) as cr on cr.source_ref=mr.escm_initialreceipt_id "
          + " where mr.escm_initialreceipt_id in (select distinct source_ref from escm_initialreceipt "
          + " where m_inout_id='" + objId + "') and  mr.issummary='N' group by cr.source_ref "
          + "  having  (coalesce((sum(mr.quantity)-sum(mr.ir_return_qty)-sum(mr.return_qty)),0) ) < (coalesce(sum(mr.accepted_qty),0)+coalesce(sum(mr.rejected_qty),0) +coalesce(sum(cr.crqty),0))";
      ps = con.prepareStatement(query);
      log4j.debug("QtyCheck:" + ps.toString());
      rs = ps.executeQuery();
      while (rs.next()) {
        OBQuery<EscmInitialReceipt> erroreceiptQry = OBDal.getInstance()
            .createQuery(EscmInitialReceipt.class, "as e where e.sourceRef.id=:sourceRefID "
                + " and e.goodsShipment.id=:inoutID and e.summaryLevel='N'");
        erroreceiptQry.setNamedParameter("sourceRefID", rs.getString("source_ref"));
        erroreceiptQry.setNamedParameter("inoutID", objId);
        if (erroreceiptQry.list().size() > 0) {
          for (EscmInitialReceipt lineObject : erroreceiptQry.list()) {
            lineObject.setFailurereason("Quantity Exceed,Availabe Quantity to inspect:'"
                + (rs.getInt("irqty") - rs.getInt("insqty")) + "'");
            OBDal.getInstance().save(lineObject);
          }
          isProceed = false;
          count = 2;
        }
      }
      OBDal.getInstance().flush();

      if (isProceed) {
        // update AcceptQty in Initial receipt
        query1 = " select sum(quantity) as qty,source_ref from escm_initialreceipt "
            + " where m_inout_id= ? and status='A' group by source_ref ";
        ps1 = con.prepareStatement(query1);
        ps1.setString(1, objId);
        rs1 = ps1.executeQuery();
        while (rs1.next()) {
          EscmInitialReceipt objIrReceipt = OBDal.getInstance().get(EscmInitialReceipt.class,
              rs1.getString("source_ref"));
          objIrReceipt
              .setAcceptedQty(objIrReceipt.getAcceptedQty().add((rs1.getBigDecimal("qty"))));
          OBDal.getInstance().save(objIrReceipt);
        }

        // update RejectQty in Initial Receipt
        query2 = " select sum(quantity) as qty,source_ref from escm_initialreceipt "
            + " where m_inout_id= ? and status='R' group by source_ref ";
        ps2 = con.prepareStatement(query2);
        ps2.setString(1, objId);
        rs2 = ps2.executeQuery();
        while (rs2.next()) {
          EscmInitialReceipt objIrReceipt = OBDal.getInstance().get(EscmInitialReceipt.class,
              rs2.getString("source_ref"));
          objIrReceipt
              .setRejectedQty(objIrReceipt.getRejectedQty().add((rs2.getBigDecimal("qty"))));
          OBDal.getInstance().save(objIrReceipt);
        }
        count = 1;
        // update failure reason as empty once success
        OBDal.getInstance().getConnection()
            .prepareStatement(
                "update escm_initialreceipt set failurereason='' where m_inout_id='" + objId + "'")
            .executeUpdate();

      }

    } catch (Exception e) {
      count = 0;
      OBDal.getInstance().rollbackAndClose();
      log4j.error("Exception in updateInitialRecipt: ", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      try {
        // close connection
        if (rs1 != null) {
          rs1.close();
        }
        if (rs != null) {
          rs.close();
        }
        if (rs2 != null) {
          rs2.close();
        }
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();
    }
    return count;
  }

  /**
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param objInout
   * @return null if no line quantity is greater than quantity due else return line no
   */
  private String checkPendingQuantity(Connection con, String clientId, String orgId,
      ShipmentInOut objInout, ProcessBundle bundle, VariablesSecureApp vars) {
    String objId = objInout.getId();
    String query = "";
    PreparedStatement ps = null;
    ResultSet rs = null;
    String result = null;

    try {
      OBContext.setAdminMode(true);
      query = " select array_agg(poline.line) from escm_initialreceipt poline "
          + "  join escm_ordershipment ship on poline.escm_ordershipment_id = ship.escm_ordershipment_id "
          + "  where poline.quantity >  (ship.movementqty-(ship.quantityporec -ship.quantityirr- ship.quantityreturned - ship.quantityrejected)- ship.quantitycanceled) "
          + "  and poline.m_inout_id =? ";
      ps = con.prepareStatement(query);
      log4j.debug("QtyCheck:" + ps.toString());
      ps.setString(1, objId);
      rs = ps.executeQuery();
      while (rs.next()) {
        result = rs.getString(1) == null ? null : rs.getString(1).toString();
      }
    } catch (Exception e) {
      log4j.debug("Exception in checkPendingQuantity:" + e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();
    }
    return result;
  }
}
