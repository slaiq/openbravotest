package sa.elm.ob.scm.webservice.dao;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.ad_process.RDVProcess.RDVMatchAll;
import sa.elm.ob.scm.EscmCOrderV;
import sa.elm.ob.scm.EscmInitialReceipt;
import sa.elm.ob.scm.EscmInitialreceiptView;
import sa.elm.ob.scm.EscmOrderlineV;
import sa.elm.ob.scm.EscmPoRcptRdvDefault;
import sa.elm.ob.scm.actionHandler.IRComplete;
import sa.elm.ob.scm.actionHandler.POReceiptDeleteLines;
import sa.elm.ob.scm.actionHandler.PoReactivate;
import sa.elm.ob.scm.webservice.constants.WebserviceConstants;
import sa.elm.ob.scm.webservice.dto.PoReceiptHeaderDTO;
import sa.elm.ob.scm.webservice.dto.PoReceiptLineDTO;
import sa.elm.ob.scm.webservice.dto.RDVHoldDTO;
import sa.elm.ob.scm.webservice.dto.RDVPenaltyDTO;
import sa.elm.ob.scm.webservice.exception.CreateReceiptException;
import sa.elm.ob.utility.WebserviceTrackerHeader;
import sa.elm.ob.utility.WebserviceTrackerLine;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * This class is used to create po receipt
 * 
 * @author Sathishkumar.P
 */
public class CreatePoReceiptDAO {

  private static final Logger log4j = LoggerFactory.getLogger(CreatePoReceiptDAO.class);

  public static ShipmentInOut insertPOReceiptHeader(PoReceiptHeaderDTO orderHdrArray)
      throws Exception, CreateReceiptException {

    // Declaration Block
    Order objOrder = null;
    Date date = null;
    User usr = null;
    String sequence = "";
    EscmPoRcptRdvDefault defaults = null;

    try {
      OBContext.setAdminMode();
      objOrder = OBDal.getInstance().get(Order.class, orderHdrArray.getOrderId());
      if (objOrder != null) {
        String receiveDate_h = orderHdrArray.getReceiveDate();
        String receiveDate_g = UtilityDAO.convertToGregorian_tochar(receiveDate_h);
        date = new SimpleDateFormat("dd-MM-yyyy").parse(receiveDate_g);

        EscmCOrderV order = OBDal.getInstance().get(EscmCOrderV.class, objOrder.getId());

        OBQuery<EscmPoRcptRdvDefault> defaultsQry = OBDal.getInstance()
            .createQuery(EscmPoRcptRdvDefault.class, "as e ");
        defaultsQry.setFilterOnReadableClients(false);
        defaultsQry.setFilterOnReadableOrganization(false);
        List<EscmPoRcptRdvDefault> defaultsQryList = defaultsQry.list();

        if (defaultsQryList.size() > 0) {
          defaults = defaultsQryList.get(0);
        } else {
          throw new CreateReceiptException(OBMessageUtils.messageBD("ESCM_Configure_defaults"),
              true);
        }

        ShipmentInOut poReceipt = OBProvider.getInstance().get(ShipmentInOut.class);
        poReceipt.setClient(objOrder.getClient());
        poReceipt.setOrganization(objOrder.getOrganization());
        poReceipt.setActive(true);
        poReceipt.setUpdatedBy(usr);
        poReceipt.setCreationDate(new Date());
        poReceipt.setCreatedBy(usr);
        poReceipt.setUpdated(new Date());
        poReceipt.setDocumentNo(objOrder.getDocumentNo());
        poReceipt.setMovementDate(date);
        poReceipt.setAccountingDate(date);
        poReceipt.setSalesOrder(order);
        poReceipt.setBusinessPartner(objOrder.getBusinessPartner());
        poReceipt.setPartnerAddress(objOrder.getPartnerAddress());
        poReceipt.setEscmReceivingtype(WebserviceConstants.DEFAULT_REC_TYPE);
        poReceipt.setDatePrinted(date);
        if (objOrder.getEscmReceivetype().equals("QTY")) {
          poReceipt.setEscmReceivetype(WebserviceConstants.REC_TYPE_QTY);
        } else {
          poReceipt.setEscmReceivetype(WebserviceConstants.REC_TYPE_AMT);
        }
        poReceipt.setWarehouse(defaults.getWarehouse());
        poReceipt.setDocumentType(defaults.getDocumentType());
        poReceipt.setOrderReference(objOrder.getDocumentNo());
        poReceipt.setEscmIscreatedfromws(true);

        sequence = Utility.getSpecificationSequence(poReceipt.getOrganization().getId(), "PROJ");
        poReceipt.setDocumentNo(sequence);
        poReceipt.setEscmSite(defaults.getSite());
        poReceipt.setEscmDocstatus("CO");
        poReceipt.setEscmTranstypevalue("PROJ");

        OBDal.getInstance().save(poReceipt);
        OBDal.getInstance().flush();

        return poReceipt;
      }
    } catch (CreateReceiptException e) {
      log4j.error("Error in insertPOReceiptHeader in CreatePoReceiptDAO" + e);
      throw new CreateReceiptException(e.getMessage());
    } catch (Exception e) {
      log4j.error("Error in insertPOReceiptHeader in CreatePoReceiptDAO" + e);
      throw new Exception(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

    return null;
  }

  public static void createPoReceiptLines(PoReceiptHeaderDTO objOrder, ShipmentInOut poreceipt)
      throws Exception {
    List<EscmInitialReceipt> poReceiptLineList = new ArrayList<>();
    try {
      OBContext.setAdminMode();
      Order order = OBDal.getInstance().get(Order.class, poreceipt.getSalesOrder().getId());
      for (OrderLine orderLine : order.getOrderLineList()) {
        insertPOReceiptLines(orderLine, poreceipt.getEscmReceivetype(), objOrder,
            objOrder.getLineDTO(), poreceipt, poReceiptLineList);
      }
      OBDal.getInstance().flush();
      updateParentLine(poReceiptLineList, objOrder);

      OBDal.getInstance().refresh(poreceipt);

    } catch (Exception e) {
      log4j.error("Error in insertporeceipt lines  in CreatePoReceiptDAO" + e.getMessage());
      throw new Exception(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method is used to update parent and child relationship in Po receipt lines
   * 
   * @param poReceiptLineList
   * @param objOrder
   */
  private static void updateParentLine(List<EscmInitialReceipt> poReceiptLineList,
      PoReceiptHeaderDTO objOrder) {
    List<EscmInitialReceipt> initialReceipt = null;
    try {
      for (EscmInitialReceipt lines : poReceiptLineList) {
        OrderLine orderline = lines.getSalesOrderLine();
        EscmOrderlineV parentLine = orderline.getEscmParentline();

        if (parentLine != null) {
          initialReceipt = poReceiptLineList.stream()
              .filter(a -> a.getSalesOrderLine().getId().equals(parentLine.getId()))
              .collect(Collectors.toList());

          if (initialReceipt != null && initialReceipt.size() > 0) {
            EscmInitialreceiptView intialReceiptView = OBDal.getInstance()
                .get(EscmInitialreceiptView.class, initialReceipt.get(0).getId());
            lines.setParentLine(intialReceiptView);
            OBDal.getInstance().save(lines);
          }
        }
      }
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log4j.error("Error in updateParentLine" + e.getMessage());
    }

  }

  private static void insertPOReceiptLines(OrderLine objOrderLine, String receiveType,
      PoReceiptHeaderDTO orderLnArray, List<PoReceiptLineDTO> lineDTO, ShipmentInOut poreceipt,
      List<EscmInitialReceipt> poReceiptLineList) throws Exception {
    BigDecimal amount = BigDecimal.ZERO;
    BigDecimal quantity = BigDecimal.ZERO;
    try {
      for (PoReceiptLineDTO line : lineDTO) {
        if (line.getPoLineId().equals(objOrderLine.getId())) {
          if (receiveType.equals("AMT")) {
            amount = line.getAmountToRelease();
          } else if (receiveType.equals("QTY")) {
            quantity = line.getQtyToRelease();
          }
        }
      }
      if ((quantity.compareTo(BigDecimal.ZERO) > 0 || amount.compareTo(BigDecimal.ZERO) > 0)
          || objOrderLine.isEscmIssummarylevel()) {
        EscmInitialReceipt poReceiptLines = OBProvider.getInstance().get(EscmInitialReceipt.class);
        poReceiptLines.setClient(objOrderLine.getClient());
        poReceiptLines.setOrganization(objOrderLine.getOrganization());
        poReceiptLines.setActive(true);
        poReceiptLines.setUpdatedBy(objOrderLine.getCreatedBy());
        poReceiptLines.setCreationDate(new java.util.Date());
        poReceiptLines.setCreatedBy(objOrderLine.getCreatedBy());
        poReceiptLines.setUpdated(new Date());
        poReceiptLines.setLineNo(objOrderLine.getLineNo());
        poReceiptLines.setSalesOrderLine(objOrderLine);
        poReceiptLines.setDescription(objOrderLine.getEscmProdescription());
        poReceiptLines.setGoodsShipment(poreceipt);
        poReceiptLines.setUOM(objOrderLine.getUOM());
        poReceiptLines.setManual(false);
        poReceiptLines.setUnitprice(
            objOrderLine.getLineNetAmount().divide(objOrderLine.getOrderedQuantity()));
        poReceiptLines.setAlertStatus("A");
        poReceiptLines.setSummaryLevel(objOrderLine.isEscmIssummarylevel());
        if (objOrderLine.getProduct() != null) {
          poReceiptLines.setProduct(objOrderLine.getProduct());
        }
        if (!objOrderLine.isEscmIssummarylevel()) {
          if (receiveType.equals("AMT")) {
            poReceiptLines.setQuantity(BigDecimal.ONE);
            poReceiptLines.setReceivedAmount(amount);
            poReceiptLines.setTOTLineAmt(amount);
          } else if (receiveType.equals("QTY")) {
            poReceiptLines.setQuantity(quantity);
            poReceiptLines.setTOTLineAmt(poReceiptLines.getUnitprice().multiply(quantity));
          }

          poReceiptLines.setOrderedQuantity(objOrderLine.getOrderedQuantity());
          poReceiptLines.setOrderedamt(objOrderLine.getLineNetAmount());
          poReceiptLines.setNegotiatedUnitprice(objOrderLine.getUnitPrice());
          poReceiptLines.setChangeFactor(objOrderLine.getEscmPoChangeFactor());
          poReceiptLines.setChangeType(objOrderLine.getEscmPoChangeType());
          poReceiptLines.setChangeValue(objOrderLine.getEscmPoChangeValue());
          poReceiptLines.setUnitpriceAfterchag(objOrderLine.getEscmUnitpriceAfterchag());
          poReceiptLines.setTaxAmount(objOrderLine.getEscmLineTaxamt());
          poReceiptLines.setUnitTax(objOrderLine.getEscmUnittax());

          poReceiptLines.setRemainingQuantity(objOrderLine.getOrderedQuantity()
              .subtract((objOrderLine.getEscmQtyporec() == null ? BigDecimal.ZERO
                  : objOrderLine.getEscmQtyporec())
                      .subtract(objOrderLine.getEscmQtyirr() == null ? BigDecimal.ZERO
                          : objOrderLine.getEscmQtyirr())
                      .subtract(objOrderLine.getEscmQtyrejected() == null ? BigDecimal.ZERO
                          : objOrderLine.getEscmQtyrejected())
                      .subtract(objOrderLine.getEscmQtyreturned() == null ? BigDecimal.ZERO
                          : objOrderLine.getEscmQtyreturned()))
              .subtract(objOrderLine.getEscmQtycanceled() == null ? BigDecimal.ZERO
                  : objOrderLine.getEscmQtycanceled())
              .subtract(objOrderLine.getEscmLegacyQtyDelivered() == null ? BigDecimal.ZERO
                  : objOrderLine.getEscmLegacyQtyDelivered()));

          poReceiptLines.setRemainingAmt(objOrderLine.getLineNetAmount()
              .subtract((objOrderLine.getEscmAmtporec() == null ? BigDecimal.ZERO
                  : objOrderLine.getEscmAmtporec())
                      .subtract(objOrderLine.getEscmAmtreturned() == null ? BigDecimal.ZERO
                          : objOrderLine.getEscmAmtreturned()))
              .subtract(objOrderLine.getEscmAmtcanceled() == null ? BigDecimal.ZERO
                  : objOrderLine.getEscmAmtcanceled())
              .subtract(objOrderLine.getEscmLegacyAmtDelivered() == null ? BigDecimal.ZERO
                  : objOrderLine.getEscmLegacyAmtDelivered()));

          poReceiptLines.setRounddiffTax(objOrderLine.getEscmRounddiffTax());
          poReceiptLines.setRounddiffInvoice(objOrderLine.getEscmRounddiffInvoice());

        }
        OBDal.getInstance().save(poReceiptLines);
        poReceiptLineList.add(poReceiptLines);
      }
    } catch (Exception e) {
      log4j.error("Error in insertPOReceiptLines in CreatePoReceiptDAO" + e);
      throw new Exception(e.getMessage());
    }
  }

  public static OBError checkIRCompleteProcess(ShipmentInOut poreceipt, VariablesSecureApp vars,
      ConnectionProvider conn) throws Exception {
    ProcessBundle pb = new ProcessBundle(WebserviceConstants.IRCOMPLETE_PROCESS, vars).init(conn);
    pb.setCloseConnection(false);

    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("M_InOut_ID", poreceipt.getId());
    pb.setParams(parameters);
    OBError error = null;
    new IRComplete().execute(pb);
    error = (OBError) pb.getResult();
    return error;

  }

  public static void reactivatePoReceipt(ShipmentInOut poreceipt, VariablesSecureApp vars)
      throws Exception {

    ProcessBundle pb = new ProcessBundle(WebserviceConstants.REACTIVATE_PROCESS, vars)
        .init(new DalConnectionProvider(true));
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("M_InOut_ID", poreceipt.getId());
    pb.setParams(parameters);
    new PoReactivate().execute(pb);
    pb.getResult();

  }

  public static void deleteLines(ShipmentInOut header, VariablesSecureApp vars) throws Exception {

    header.setDocumentStatus("DR");
    header.setEscmDocstatus("DR");
    header.setProcessed(false);
    OBDal.getInstance().save(header);
    OBDal.getInstance().flush();

    OBContext.setOBContext(
        WebserviceConstants.DEFAULT_USER_ID, "0", OBPropertiesProvider.getInstance()
            .getOpenbravoProperties().getProperty(WebserviceConstants.CLIENT_KEY),
        WebserviceConstants.DEFAULT_ORG_ID);

    ProcessBundle pb = new ProcessBundle(WebserviceConstants.DELETELINES_PROCESS, vars)
        .init(new DalConnectionProvider(true));
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("M_InOut_ID", header.getId());
    pb.setParams(parameters);
    new POReceiptDeleteLines().execute(pb);
    pb.getResult();

  }

  public static void deleteHeader(ShipmentInOut header) throws Exception {
    OBDal.getInstance().remove(header);
    OBDal.getInstance().flush();
  }

  public static OBError matchAll(EfinRDV rdv, EfinRDVTransaction rdvTxn, VariablesSecureApp vars,
      ConnectionProvider conn) throws Exception {
    OBContext.setOBContext(
        WebserviceConstants.DEFAULT_USER_ID, "0", OBPropertiesProvider.getInstance()
            .getOpenbravoProperties().getProperty(WebserviceConstants.CLIENT_KEY),
        WebserviceConstants.DEFAULT_ORG_ID);
    ProcessBundle pb = new ProcessBundle(WebserviceConstants.MATCHALL_PROCESS, vars);
    pb.setCloseConnection(false);
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("Efin_Rdvtxn_ID", rdvTxn.getId());
    parameters.put("defaultpenalty", "N");
    pb.setParams(parameters);
    new RDVMatchAll().execute(pb);
    OBError error = (OBError) pb.getResult();
    return error;
  }

  public static String getLegacyOrderId(String legacyContractNo) throws Exception {

    String legacyOrderId = null;
    String query = null;
    try {
      query = "select o.id from Order o join o.escmLegacycontract as e where o.escmRevision= "
          + " (select max(ord.escmRevision) from Order ord join ord.escmLegacycontract as leg "
          + "  where leg.contractNo = :legacyContractNo and ord.escmAppstatus = :appStatus ) and e.contractNo =:legacyContractNo";
      Query qry = OBDal.getInstance().getSession().createQuery(query);
      qry.setParameter("legacyContractNo", legacyContractNo);
      qry.setParameter("appStatus", "ESCM_AP");
      qry.setMaxResults(1);

      if (qry != null) {
        @SuppressWarnings("rawtypes")
        List idList = qry.list();
        if (idList.size() > 0) {
          Object row = (Object) idList.get(0);
          legacyOrderId = (String) row;
        } else {
          return null;
        }
      }
    } catch (Exception e) {
      log4j.error("Error in getLegacyOrderId in CreatePoReceiptDAO" + e);
      throw new Exception(e.getMessage());
    }
    return legacyOrderId;
  }

  @SuppressWarnings("rawtypes")
  public static List getOrderIdbyContractNo(String contractNo) throws Exception {
    // String orderId = null;
    String query = null;
    List idList = null;
    String rountineContractQty = "B2FEF4B9985446BE860C769D5715CDDE";
    try {
      query = "select id from Order e where e.escmMaintenanceCntrctNo=:documentNo and e.escmRevision = "
          + " (select max(o.escmRevision) from Order o where o.escmMaintenanceCntrctNo = :documentNo and o.escmAppstatus = :appStatus and o.escmContactType.id=:contractType) and e.escmContactType.id=:contractType ";
      Query qry = OBDal.getInstance().getSession().createQuery(query);
      qry.setParameter("documentNo", contractNo);
      qry.setParameter("appStatus", "ESCM_AP");
      qry.setParameter("contractType", rountineContractQty);
      qry.setMaxResults(1);

      if (qry != null) {
        idList = qry.list();
      }
      return idList;
    } catch (Exception e) {
      log4j.error("Error in getOrderIdbyContractNo in CreatePoReceiptDAO" + e);
      throw new Exception(e.getMessage());
    }
  }

  public static void validateInputRequest(PoReceiptHeaderDTO request)
      throws CreateReceiptException, Exception {

    try {

      Boolean hasorderId = StringUtils.isNotBlank(request.getOrderId());
      Boolean hasdocumentNo = StringUtils.isNotBlank(request.getPoContractNo());
      Boolean haslegacyNo = StringUtils.isNotBlank(request.getLegacyContractNo());

      String id = null;
      String orderParameter = null;
      Order order = null;

      // validate order id, contract no, legacy no
      if (!(hasorderId || hasdocumentNo || haslegacyNo)) {
        throw new CreateReceiptException(OBMessageUtils.messageBD("ESCM_orderdetails_mandatory"),
            true);
      }

      // validate order id is present
      if (hasorderId) {
        orderParameter = "OrderId";
        id = request.getOrderId();
      } else if (hasdocumentNo) {
        // get order id based on contract no
        orderParameter = "Pocontractno";
        // check multiple order is matching
        @SuppressWarnings("rawtypes")
        List idList = getOrderIdbyContractNo(request.getPoContractNo());
        if (idList.size() > 1) {
          throw new CreateReceiptException(
              String.format(OBMessageUtils.messageBD("ESCM_ordernotexists"), orderParameter), true);
        } else if (idList.size() > 0) {
          Object row = (Object) idList.get(0);
          id = (String) row;
        }
        // id = getOrderIdbyContractNo(request.getPoContractNo());
      } else if (haslegacyNo) {
        orderParameter = "Legacyno";
        // get order id based on legacy no
        id = getLegacyOrderId(request.getLegacyContractNo());
      }

      if (StringUtils.isNotBlank(id)) {
        order = OBDal.getInstance().get(Order.class, id);
        if (order == null) {
          throw new CreateReceiptException(
              String.format(OBMessageUtils.messageBD("ESCM_ordernotexists"), orderParameter), true);
        }
        request.setReceiveType(order.getEscmReceivetype());
        request.setOrderId(id);
      } else {
        throw new CreateReceiptException(
            String.format(OBMessageUtils.messageBD("ESCM_ordernotexists"), orderParameter), true);
      }

      // validate receive date
      try {
        String receiveDate_g = UtilityDAO.convertToGregorian_tochar(request.getReceiveDate());
        new SimpleDateFormat("dd-MM-yyyy").parse(receiveDate_g);
      } catch (java.text.ParseException e) {
        throw new CreateReceiptException(OBMessageUtils.messageBD("ESCM_invalidreceivedate"), true);
      } catch (Exception e) {
        throw new CreateReceiptException(OBMessageUtils.messageBD("ESCM_invalidreceivedate"), true);
      }

      // validate lines
      validateLineInputRequest(request);

      // validate BulkPenalty
      validateBulkPenalty(request);

    } catch (CreateReceiptException e) {
      throw new CreateReceiptException(e.getMessage(), e.getCause());
    } catch (Exception e) {
      throw new Exception(e.getMessage());
    }

  }

  private static void validateLineInputRequest(PoReceiptHeaderDTO request)
      throws CreateReceiptException, Exception {

    try {

      for (PoReceiptLineDTO lineRequest : request.getLineDTO()) {

        Boolean haslineId = StringUtils.isNotBlank(lineRequest.getPoLineId());
        Boolean hasitemNo = StringUtils.isNotBlank(lineRequest.getItemNo());
        Boolean hasdescription = StringUtils.isNotBlank(lineRequest.getItemDescription());

        // validate line id, item no, description
        if (!(haslineId || hasitemNo || hasdescription)) {
          throw new CreateReceiptException(OBMessageUtils.messageBD("ESCM_linedetails_mandatory"),
              true);
        }

        // validate amount to release and quantity to release
        if (request.getReceiveType().equals(WebserviceConstants.REC_TYPE_AMT)) {
          Optional<BigDecimal> amountToRelease = Optional
              .ofNullable(lineRequest.getAmountToRelease());

          if (amountToRelease.isPresent()) {
            if (lineRequest.getAmountToRelease().compareTo(BigDecimal.ZERO) <= 0) {
              throw new CreateReceiptException(OBMessageUtils.messageBD("ESCM_amounttoreleaseless"),
                  true);
            }
          } else {
            throw new CreateReceiptException(
                OBMessageUtils.messageBD("ESCM_releaseamountmandatory"), true);
          }

        } else {
          Optional<BigDecimal> qtyToRelease = Optional.ofNullable(lineRequest.getQtyToRelease());
          if (qtyToRelease.isPresent()) {
            if (lineRequest.getQtyToRelease().compareTo(BigDecimal.ZERO) <= 0) {
              throw new CreateReceiptException(OBMessageUtils.messageBD("ESCM_qtytoreleaseless"),
                  true);
            }
          } else {
            throw new CreateReceiptException(OBMessageUtils.messageBD("ESCM_releaseqtymandatory"),
                true);
          }
        }
        if (lineRequest.getPenaltyDTO() != null)
          lineRequest.setPenaltyDTO(validateLinePenalty(lineRequest.getPenaltyDTO(), request));

      }
    } catch (CreateReceiptException e) {
      throw new CreateReceiptException(e.getMessage(), e.getCause());
    } catch (Exception e) {
      throw new Exception(e.getMessage());
    }

  }

  private static List<RDVPenaltyDTO> validateLinePenalty(List<RDVPenaltyDTO> penaltyDTO,
      PoReceiptHeaderDTO request) throws CreateReceiptException, Exception {
    try {

      for (RDVPenaltyDTO penalty : penaltyDTO) {
        if (StringUtils.isNotBlank(penalty.getBpartnerId())) {

          OBQuery<org.openbravo.model.common.businesspartner.BusinessPartner> bp = OBDal
              .getInstance()
              .createQuery(org.openbravo.model.common.businesspartner.BusinessPartner.class,
                  " as e where e.searchKey=:value");
          bp.setNamedParameter("value", penalty.getBpartnerId());
          bp.setFilterOnReadableClients(false);
          bp.setFilterOnReadableOrganization(false);
          bp.setMaxResult(1);
          List<org.openbravo.model.common.businesspartner.BusinessPartner> bpList = bp.list();
          if (bpList.size() == 0) {
            throw new CreateReceiptException(String
                .format(OBMessageUtils.messageBD("ESCM_bpnotpresent"), penalty.getBpartnerId()));
          } else if (bpList.size() > 1) {
            throw new CreateReceiptException(String
                .format(OBMessageUtils.messageBD("ESCM_bpnotpresent"), penalty.getBpartnerId()));
          } else {
            penalty.setBpartnerId(bpList.get(0).getId());
          }

        } else if (!StringUtils.isEmpty(request.getReceivedBy())) {
          OBQuery<org.openbravo.model.common.businesspartner.BusinessPartner> bp = OBDal
              .getInstance()
              .createQuery(org.openbravo.model.common.businesspartner.BusinessPartner.class,
                  " as e where (e.name= :name or e.searchKey=:value)");
          bp.setNamedParameter("name", request.getReceivedBy());
          bp.setNamedParameter("value", request.getReceivedBy());
          bp.setFilterOnReadableClients(false);
          bp.setFilterOnReadableOrganization(false);
          bp.setMaxResult(1);
          List<org.openbravo.model.common.businesspartner.BusinessPartner> bpList = bp.list();
          if (bpList.size() == 0) {
            throw new CreateReceiptException(String
                .format(OBMessageUtils.messageBD("ESCM_bpnotpresent"), penalty.getBpartnerId()));
          } else if (bpList.size() > 1) {
            throw new CreateReceiptException(String
                .format(OBMessageUtils.messageBD("ESCM_bpnotpresent"), penalty.getBpartnerId()));
          } else {
            penalty.setBpartnerId(bpList.get(0).getId());
          }
        } else {
          if (!StringUtils.isEmpty(request.getOrderId())) {
            Order order = OBDal.getInstance().get(Order.class, request.getOrderId());
            penalty.setBpartnerId(order.getBusinessPartner().getId());
          }
        }
      }

      return penaltyDTO;

    } catch (CreateReceiptException e) {
      throw new CreateReceiptException(e.getMessage(), e.getCause());
    } catch (Exception e) {
      throw new Exception(e.getMessage());
    }

  }

  private static void validateBulkPenalty(PoReceiptHeaderDTO request)
      throws CreateReceiptException, Exception {
    try {
      for (RDVPenaltyDTO penalty : request.getBulkPenaltyDTO()) {
        if (StringUtils.isNotBlank(penalty.getBpartnerId())) {
          OBQuery<org.openbravo.model.common.businesspartner.BusinessPartner> bp = OBDal
              .getInstance()
              .createQuery(org.openbravo.model.common.businesspartner.BusinessPartner.class,
                  " as e where e.searchKey=:value");
          bp.setNamedParameter("value", penalty.getBpartnerId());
          bp.setFilterOnReadableClients(false);
          bp.setFilterOnReadableOrganization(false);
          bp.setMaxResult(1);
          List<org.openbravo.model.common.businesspartner.BusinessPartner> bpList = bp.list();
          if (bpList.size() == 0) {
            throw new CreateReceiptException(String
                .format(OBMessageUtils.messageBD("ESCM_bpnotpresent"), penalty.getBpartnerId()));
          } else if (bpList.size() > 1) {
            throw new CreateReceiptException(String
                .format(OBMessageUtils.messageBD("ESCM_bpnotpresent"), penalty.getBpartnerId()));
          } else {
            penalty.setBpartnerId(bpList.get(0).getId());
          }

        } else if (!StringUtils.isEmpty(request.getReceivedBy())) {
          OBQuery<org.openbravo.model.common.businesspartner.BusinessPartner> bp = OBDal
              .getInstance()
              .createQuery(org.openbravo.model.common.businesspartner.BusinessPartner.class,
                  " as e where (e.name= :name or e.searchKey=:value)");
          bp.setNamedParameter("name", request.getReceivedBy());
          bp.setNamedParameter("value", request.getReceivedBy());
          bp.setFilterOnReadableClients(false);
          bp.setFilterOnReadableOrganization(false);
          bp.setMaxResult(1);
          List<org.openbravo.model.common.businesspartner.BusinessPartner> bpList = bp.list();
          if (bpList.size() == 0) {
            throw new CreateReceiptException(String
                .format(OBMessageUtils.messageBD("ESCM_bpnotpresent"), penalty.getBpartnerId()));
          } else if (bpList.size() > 1) {
            throw new CreateReceiptException(String
                .format(OBMessageUtils.messageBD("ESCM_bpnotpresent"), penalty.getBpartnerId()));
          } else {
            penalty.setBpartnerId(bpList.get(0).getId());
          }
        } else {
          if (!StringUtils.isEmpty(request.getOrderId())) {
            Order order = OBDal.getInstance().get(Order.class, request.getOrderId());
            penalty.setBpartnerId(order.getBusinessPartner().getId());
          }
        }
      }
    } catch (CreateReceiptException e) {
      throw new CreateReceiptException(e.getMessage(), e.getCause());
    } catch (Exception e) {
      throw new Exception(e.getMessage());
    }

  }

  public static void storeRequestAndResponse(PoReceiptHeaderDTO request, ResponseDTO response)
      throws Exception {
    try {

      WebserviceTrackerHeader header = OBProvider.getInstance().get(WebserviceTrackerHeader.class);
      header.setOrganization(OBContext.getOBContext().getCurrentOrganization());
      header.setClient(OBContext.getOBContext().getCurrentClient());
      header.setRequestnumber(request.getRequestNo());
      header.setResponse(null);
      header.setResponseerrormessage(null);
      header.setWebservicename("Createreceipt");
      header.setResponse(response.toString());
      header.setResponseerrormessage(response.getErrorMsg());

      OBDal.getInstance().save(header);
      OBDal.getInstance().flush();

      insertOrderDtoDetails("OrderDTO", header, request);

      insertBulkHoldDtoDetails("bulkHoldDTO", header, "OrderDTO", request);

      insertBulkPenaltyDtoDetails("bulkPenaltyDTO", header, "OrderDTO", request);

      insertLineDtoDetails("LineDTO", header, "OrderDTO", request);

      OBDal.getInstance().flush();
      // OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      throw new Exception(e.getMessage());
    }

  }

  private static void insertLineDtoDetails(String tagname, WebserviceTrackerHeader header,
      String parentTag, PoReceiptHeaderDTO request) throws Exception {
    try {
      int lineNo = 1;
      for (PoReceiptLineDTO line : request.getLineDTO()) {

        insertChildTag(tagname + "-" + lineNo, header, "poLineId", line.getPoLineId(), parentTag);
        insertChildTag(tagname + "-" + lineNo, header, "itemNo", line.getItemNo(), parentTag);
        insertChildTag(tagname + "-" + lineNo, header, "itemDescription", line.getItemDescription(),
            parentTag);
        insertChildTag(tagname + "-" + lineNo, header, "amountToRelease",
            line.getAmountToRelease() != null ? line.getAmountToRelease().toPlainString() : null,
            parentTag);
        insertChildTag(tagname + "-" + lineNo, header, "qtyToRelease",
            line.getQtyToRelease() != null ? line.getQtyToRelease().toPlainString() : null,
            parentTag);

        insertPenaltyDtoDetails("PenaltyDTO", header, tagname + "-" + lineNo, line);
        insertHoldDtoDetails("HoldDTO", header, tagname + "-" + lineNo, line);
        lineNo++;
      }
    } catch (Exception e) {
      throw new Exception(e.getMessage());
    }

  }

  private static void insertBulkHoldDtoDetails(String tagname, WebserviceTrackerHeader header,
      String parentTag, PoReceiptHeaderDTO receiptHeader) throws Exception {
    try {
      int lineNo = 1;
      if (receiptHeader.getBulkHoldDTO() != null)
        for (RDVHoldDTO hold : receiptHeader.getBulkHoldDTO()) {
          String newTagname = tagname;
          if (receiptHeader.getBulkHoldDTO().size() > 1) {
            newTagname = tagname + "-" + lineNo;
          }
          insertChildTag(newTagname, header, "holdcode", hold.getHoldcode(), parentTag);
          insertChildTag(newTagname, header, "holdAmount",
              hold.getHoldAmount() != null ? hold.getHoldAmount().toPlainString() : "", parentTag);
          lineNo++;
        }
    } catch (Exception e) {
      throw new Exception(e.getMessage());
    }
  }

  private static void insertBulkPenaltyDtoDetails(String tagname, WebserviceTrackerHeader header,
      String parentTag, PoReceiptHeaderDTO receiptHeader) throws Exception {
    try {
      int lineNo = 1;
      if (receiptHeader.getBulkPenaltyDTO() != null)
        for (RDVPenaltyDTO penalty : receiptHeader.getBulkPenaltyDTO()) {
          String newTagname = tagname;
          if (receiptHeader.getBulkPenaltyDTO().size() > 1) {
            newTagname = tagname + "-" + lineNo;
          }
          insertChildTag(newTagname, header, "penaltyId", penalty.getPenaltyId(), parentTag);
          insertChildTag(newTagname, header, "penaltyAmount",
              penalty.getPenaltyAmount() != null ? penalty.getPenaltyAmount().toPlainString() : "",
              parentTag);
          insertChildTag(newTagname, header, "bpartnerId", penalty.getBpartnerId(), parentTag);
          lineNo++;
        }
    } catch (Exception e) {
      throw new Exception(e.getMessage());

    }
  }

  private static void insertOrderDtoDetails(String tagname, WebserviceTrackerHeader header,
      PoReceiptHeaderDTO request) throws Exception {
    try {
      insertChildTag(tagname, header, "orderId", request.getOrderId(), null);
      insertChildTag(tagname, header, "pocontractno", request.getPoContractNo(), null);
      insertChildTag(tagname, header, "legacycontractno", request.getLegacyContractNo(), null);
    } catch (Exception e) {
      throw new Exception(e.getMessage());
    }

  }

  private static void insertChildTag(String tagname, WebserviceTrackerHeader header,
      String fieldName, String value, String parenttagname) throws Exception {
    try {
      WebserviceTrackerLine line = OBProvider.getInstance().get(WebserviceTrackerLine.class);
      line.setOrganization(OBContext.getOBContext().getCurrentOrganization());
      line.setClient(OBContext.getOBContext().getCurrentClient());
      line.setEUTWebserviceTracker(header);
      line.setTagname(tagname);
      line.setFieldname(fieldName);
      line.setFieldvalue(value);
      line.setParenttagname(parenttagname);
      OBDal.getInstance().save(line);
    } catch (Exception e) {
      throw new Exception(e.getMessage());
    }
  }

  private static void insertHoldDtoDetails(String tagname, WebserviceTrackerHeader header,
      String parentTag, PoReceiptLineDTO line) throws Exception {
    try {
      int lineNo = 1;
      if (line.getHoldDTO() != null)
        for (RDVHoldDTO hold : line.getHoldDTO()) {
          String newTagname = tagname;
          if (line.getHoldDTO().size() > 1) {
            newTagname = tagname + "-" + lineNo;
          }
          insertChildTag(newTagname, header, "holdcode", hold.getHoldcode(), parentTag);
          insertChildTag(newTagname, header, "holdAmount",
              hold.getHoldAmount() != null ? hold.getHoldAmount().toPlainString() : "", parentTag);
          lineNo++;
        }
    } catch (Exception e) {
      throw new Exception(e.getMessage());
    }
  }

  private static void insertPenaltyDtoDetails(String tagname, WebserviceTrackerHeader header,
      String parentTag, PoReceiptLineDTO line) throws Exception {
    try {
      int lineNo = 1;
      if (line.getPenaltyDTO() != null)
        for (RDVPenaltyDTO penalty : line.getPenaltyDTO()) {
          String newTagname = tagname;
          if (line.getPenaltyDTO().size() > 1) {
            newTagname = tagname + "-" + lineNo;
          }

          insertChildTag(newTagname, header, "penaltyId", penalty.getPenaltyId(), parentTag);
          insertChildTag(newTagname, header, "penaltyAmount",
              penalty.getPenaltyAmount() != null ? penalty.getPenaltyAmount().toPlainString() : "",
              parentTag);
          insertChildTag(newTagname, header, "bpartnerId", penalty.getBpartnerId(), parentTag);
          lineNo++;
        }
    } catch (Exception e) {
      throw new Exception(e.getMessage());

    }

  }
}