package sa.elm.ob.scm.webservice.dao;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import sa.elm.ob.finance.EfinPenalty;
import sa.elm.ob.finance.EfinPenaltyAction;
import sa.elm.ob.finance.EfinPenaltyHeader;
import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.EfinRdvHold;
import sa.elm.ob.finance.EfinRdvHoldAction;
import sa.elm.ob.finance.EfinRdvHoldHeader;
import sa.elm.ob.scm.EscmInitialReceipt;
import sa.elm.ob.scm.webservice.constants.WebserviceConstants;
import sa.elm.ob.scm.webservice.dto.POHeaderDTO;
import sa.elm.ob.scm.webservice.dto.PoContractAttributesDTO;
import sa.elm.ob.scm.webservice.dto.PoLinesDTO;
import sa.elm.ob.scm.webservice.dto.PoReceiptHeaderResponseDTO;
import sa.elm.ob.scm.webservice.dto.PoReceiptLinesResponseDTO;
import sa.elm.ob.scm.webservice.dto.RDVHeaderResponseDTO;
import sa.elm.ob.scm.webservice.dto.RDVHoldActionDTO;
import sa.elm.ob.scm.webservice.dto.RDVHoldAttributesDTO;
import sa.elm.ob.scm.webservice.dto.RDVHoldHeaderDTO;
import sa.elm.ob.scm.webservice.dto.RDVPenaltyActionDTO;
import sa.elm.ob.scm.webservice.dto.RDVPenaltyAttributesDTO;
import sa.elm.ob.scm.webservice.dto.RDVPenaltyHeaderDTO;
import sa.elm.ob.scm.webservice.dto.RDVTxnHeaderResponseDTO;
import sa.elm.ob.scm.webservice.dto.RDVTxnLineResponseDTO;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * This class is used to get all approved POs based on different filter
 * 
 * @author Sathishkumar.P
 *
 */

@Repository
public class GetApprovedPoDAO {

  /**
   * This method is used to get all approved POs with latest version
   * 
   * @return
   */

  private final static Logger log = LoggerFactory.getLogger(GetApprovedPoDAO.class);

  public static List<POHeaderDTO> getApprovedPO(int start, int end) {

    final String query = " as e where e.id in (select e.id from Order e where  e.escmAppstatus='ESCM_AP' and e.escmRevision = "
        + "(select max(a.escmRevision) from Order a where a.documentNo =e.documentNo))";
    List<POHeaderDTO> poHeaderList = new ArrayList<POHeaderDTO>();
    List<Object> parametersList = new ArrayList<Object>();

    try {

      OBContext.setAdminMode();
      OBQuery<Order> orderQry = OBDal.getInstance().createQuery(Order.class, query, parametersList);
      orderQry.setFilterOnActive(true);
      orderQry.setFilterOnReadableOrganization(false);
      orderQry.setFilterOnReadableClients(false);
      if (start != 0 && end != 0) {
        orderQry.setFirstResult(start);
        orderQry.setMaxResult(end);
      } else if (start != 0) {
        orderQry.setFirstResult(start);
      } else if (end != 0) {
        orderQry.setMaxResult(end);
      }

      log.debug("orderqry-whereclause" + orderQry.getWhereAndOrderBy());
      log.debug("orderqry-parameters" + orderQry.getNamedParameters());
      poHeaderList = getHeaderDTOList(orderQry.list());

    } catch (OBException e) {
      log.debug("error while getting approved po", e.getMessage());
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

    return poHeaderList;

  }

  /**
   * This method is used to get all approved POs with latest version and filtered with given
   * Contract number
   * 
   * @return
   */

  public static List<POHeaderDTO> getApprovedPOByContractNo(String contractNo) {

    final String query = " as e where e.escmAppstatus='ESCM_AP' and  e.documentNo=:docNo and e.id in (select e.id from Order  e where  "
        + " e.escmAppstatus='ESCM_AP' and e.escmRevision = (select max(a.escmRevision) from Order a where a.documentNo =e.documentNo  ) )";
    List<POHeaderDTO> poHeaderList = new ArrayList<POHeaderDTO>();

    try {
      OBContext.setAdminMode();
      OBQuery<Order> orderQry = OBDal.getInstance().createQuery(Order.class, query);

      orderQry.setNamedParameter("docNo", contractNo);

      orderQry.setFilterOnActive(true);
      orderQry.setFilterOnReadableOrganization(false);
      orderQry.setFilterOnReadableClients(false);
      log.debug("orderQry-whereclause" + orderQry.getWhereAndOrderBy());
      log.debug("orderQry-parameters" + orderQry.getNamedParameters());

      poHeaderList = getHeaderDTOList(orderQry.list());

    } catch (OBException e) {
      log.debug("error while getting approved po contract no", e.getMessage());
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

    return poHeaderList;

  }

  /**
   * This method is used to get all approved POs with latest version and filtered with given
   * Contract Date
   * 
   * @return
   */

  public static List<POHeaderDTO> getApprovedPOByContractDate(String date) {

    final String query = "as e where e.escmAppstatus='ESCM_AP' and to_char(to_date(e.orderDate))=:orderDate and e.id in(select e.id from Order  e where  e.escmAppstatus='ESCM_AP' and e.escmRevision = "
        + "(select max(a.escmRevision) from Order a where a.documentNo =e.documentNo))";
    List<POHeaderDTO> poHeaderList = new ArrayList<POHeaderDTO>();
    try {
      OBContext.setAdminMode();

      OBQuery<Order> orderQry = OBDal.getInstance().createQuery(Order.class, query);
      orderQry.setNamedParameter("orderDate", UtilityDAO.convertToGregorian_tochar(date));
      orderQry.setFilterOnActive(true);
      orderQry.setFilterOnReadableOrganization(false);
      orderQry.setFilterOnReadableClients(false);
      log.debug("orderQry-whereclause" + orderQry.getWhereAndOrderBy());
      log.debug("orderQry-parameters" + orderQry.getNamedParameters());
      poHeaderList = getHeaderDTOList(orderQry.list());

    } catch (OBException e) {
      log.debug("error while getting approved po contract no contract type", e.getMessage());
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

    return poHeaderList;

  }

  /**
   * This method is used to get all approved POs with latest version and filtered with given
   * Contract Type
   * 
   * @return
   */

  public static List<POHeaderDTO> getApprovedPOByContractType(String type) {
    final String query = " as e where e.escmAppstatus='ESCM_AP' and e.escmOrdertype=:type and e.id in (select e.id from Order  e where  e.escmAppstatus='ESCM_AP' and "
        + "e.escmRevision = (select max(a.escmRevision) from Order a where a.documentNo =e.documentNo ))";
    List<POHeaderDTO> poHeaderList = new ArrayList<POHeaderDTO>();

    try {
      OBContext.setAdminMode();

      OBQuery<Order> orderQry = OBDal.getInstance().createQuery(Order.class, query);
      orderQry.setNamedParameter("type", type);
      orderQry.setFilterOnActive(true);
      orderQry.setFilterOnReadableOrganization(false);
      orderQry.setFilterOnReadableClients(false);
      log.debug("orderQry-whereclause" + orderQry.getWhereAndOrderBy());
      log.debug("orderQry-parameters" + orderQry.getNamedParameters());
      poHeaderList = getHeaderDTOList(orderQry.list());

    } catch (OBException e) {
      log.debug("error while getting approved po contract no contract type", e.getMessage());
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

    return poHeaderList;

  }

  /*
   * This method is used to get the order type of Purchase order from List Reference
   * 
   * @return ordertype map(Key,value)
   */

  private static Map<String, String> getOrderType() {

    List<org.openbravo.model.ad.domain.List> referenceList = null;
    Map<String, String> ordertypeMap = new HashMap<>();

    try {
      final String query = " as o where o.reference.id = ? ";
      List<Object> parametersList = new ArrayList<Object>();
      parametersList.add(WebserviceConstants.ORDERTYPE_REFERENCE);

      OBQuery<org.openbravo.model.ad.domain.List> listQry = OBDal.getInstance()
          .createQuery(org.openbravo.model.ad.domain.List.class, query, parametersList);
      listQry.setFilterOnActive(true);
      listQry.setFilterOnReadableOrganization(false);
      listQry.setFilterOnReadableClients(false);
      log.debug("listQry" + listQry);
      referenceList = listQry.list();

      for (org.openbravo.model.ad.domain.List list : referenceList) {
        ordertypeMap.put(list.getSearchKey(), list.getName());
      }

      return ordertypeMap;

    } catch (OBException e) {
      log.debug("Error while getting ordertype", e.getMessage());
      throw new OBException(e.getMessage());
    }
  }

  /**
   * This method is used to form the headerDTO from order list
   * 
   * @return list of POHeaderDTO
   * 
   */

  private static List<POHeaderDTO> getHeaderDTOList(List<Order> orderList) {

    Map<String, String> orderTypeMap = getOrderType();
    List<POHeaderDTO> poHeaderList = new ArrayList<POHeaderDTO>();
    try {

      for (Order order : orderList) {
        POHeaderDTO headerDTO = new POHeaderDTO();
        PoContractAttributesDTO contractAttributesDTO = new PoContractAttributesDTO();
        List<PoReceiptHeaderResponseDTO> poReceiptList = new ArrayList<PoReceiptHeaderResponseDTO>();
        List<RDVHeaderResponseDTO> rdvList = new ArrayList<RDVHeaderResponseDTO>();
        List<PoLinesDTO> lineListDTO = new ArrayList<>();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String strDate = dateFormat.format(order.getOrderDate());

        headerDTO.setOrderId(order.getId());
        headerDTO.setContractDate(strDate);
        headerDTO.setContractNo(order.getDocumentNo());
        headerDTO.setContractType(orderTypeMap.get(order.getEscmOrdertype()));
        headerDTO.setGrossPoAmount(order.getEscmTotPoUpdatedAmt());
        headerDTO.setNetPoAmount(order.getGrandTotalAmount());
        if ("AMT".equals(order.getEscmReceivetype())) {
          headerDTO.setReceiveType(WebserviceConstants.AMOUNT);
        } else {
          headerDTO.setReceiveType(WebserviceConstants.QTY);
        }
        headerDTO.setSupplierName(order.getBusinessPartner().getName());

        for (OrderLine line : order.getOrderLineList()) {
          PoLinesDTO lineDTO = new PoLinesDTO();
          lineDTO.setLineId(line.getId());
          lineDTO.setAmountPending(line.getLineNetAmount().subtract(
              line.getEscmAmtDelivered() != null ? line.getEscmAmtDelivered() : BigDecimal.ZERO));
          lineDTO.setAmountReleased(line.getEscmAmtDelivered());
          lineDTO.setDescription(line.getEscmProdescription());
          lineDTO.setItem(line.getProduct() != null ? line.getProduct().getName() : "");
          lineDTO.setParentItem(
              line.getEscmParentline() != null
                  ? (line.getEscmParentline().getProduct() != null
                      ? line.getEscmParentline().getProduct().getName()
                      : "")
                  : "");
          lineDTO.setQtyOrdered(line.getOrderedQuantity());
          lineDTO.setQtyPending(line.getOrderedQuantity().subtract(line.getDeliveredQuantity()));
          lineDTO.setQtyReleaseed(line.getDeliveredQuantity());
          lineDTO.setUnitPrice(line.getUnitPrice());
          lineDTO.setUOM(line.getUOM() != null ? line.getUOM().getName() : "");
          lineListDTO.add(lineDTO);
        }
        headerDTO.setPoLines(lineListDTO);

        // contract attributes

        contractAttributesDTO.setAdvanceAmount(order.getEscmAdvpaymntAmt());
        contractAttributesDTO.setAdvancePercentage(order.getEscmAdvpaymntPercntge());
        contractAttributesDTO.setContractStartDate(order.getEscmContractstartdate() != null
            ? dateFormat.format(order.getEscmContractstartdate())
            : null);
        contractAttributesDTO.setContractOnBoardDate(
            order.getEscmOnboarddateh() != null ? dateFormat.format(order.getEscmOnboarddateh())
                : null);
        contractAttributesDTO.setContractEndDate(order.getEscmContractenddate() != null
            ? dateFormat.format(order.getEscmContractenddate())
            : null);
        headerDTO.setContractAttribute(contractAttributesDTO);

        // PO Receipt
        OBQuery<ShipmentInOut> poReceiptQry = OBDal.getInstance().createQuery(ShipmentInOut.class,
            "as e where e.salesOrder.id=:orderId");
        poReceiptQry.setNamedParameter("orderId", order.getId());
        poReceiptQry.setFilterOnActive(true);
        poReceiptQry.setFilterOnReadableOrganization(false);
        poReceiptQry.setFilterOnReadableClients(false);

        if (poReceiptQry.list().size() > 0) {
          poReceiptList = getPOReceiptHeaderDTOList(poReceiptQry.list());
        }
        headerDTO.setPoReceipt(poReceiptList);

        // RDV
        OBQuery<EfinRDV> rdvQry = OBDal.getInstance().createQuery(EfinRDV.class,
            "as e where e.salesOrder.id=:orderId");
        rdvQry.setNamedParameter("orderId", order.getId());
        rdvQry.setFilterOnActive(true);
        rdvQry.setFilterOnReadableOrganization(false);
        rdvQry.setFilterOnReadableClients(false);

        if (rdvQry.list().size() > 0) {
          rdvList = getRdvHeaderDTOList(rdvQry.list());
        }
        headerDTO.setRdv(rdvList);

        poHeaderList.add(headerDTO);
      }

    } catch (Exception e) {
      log.debug("Error while getting DTO Objects", e.getMessage());
      throw new OBException(e.getMessage());
    }
    return poHeaderList;
  }

  /**
   * This method is used to form the PO Receipt headerDTO from order list
   * 
   * @return list of POReceiptHeaderDTO
   * 
   */

  private static List<PoReceiptHeaderResponseDTO> getPOReceiptHeaderDTOList(
      List<ShipmentInOut> poReceiptList) {

    List<PoReceiptHeaderResponseDTO> poReceiptHeaderList = new ArrayList<PoReceiptHeaderResponseDTO>();
    try {

      for (ShipmentInOut poReceipt : poReceiptList) {
        PoReceiptHeaderResponseDTO headerDTO = new PoReceiptHeaderResponseDTO();
        List<PoReceiptLinesResponseDTO> lineListDTO = new ArrayList<>();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String transactionDate = dateFormat.format(poReceipt.getMovementDate());
        String movementDate = dateFormat.format(poReceipt.getAccountingDate());

        headerDTO.setReceiptId(poReceipt.getId());
        headerDTO.setSpecNo(poReceipt.getDocumentNo());
        headerDTO.setTransactionType(poReceipt.getEscmTranstypevalue());
        headerDTO.setTransactionDate(transactionDate);
        headerDTO.setSupplierName(
            poReceipt.getBusinessPartner() != null ? poReceipt.getBusinessPartner().getName()
                : null);
        headerDTO.setDocumentNo(poReceipt.getOrderReference());
        headerDTO.setDocumentDate(movementDate);
        headerDTO.setWarehouse(
            poReceipt.getWarehouse() != null ? poReceipt.getWarehouse().getName() : null);
        headerDTO.setReceivetype(poReceipt.getEscmReceivetype());
        headerDTO.setSite(
            poReceipt.getEscmSite() != null ? poReceipt.getEscmSite().getCommercialName() : null);

        for (EscmInitialReceipt line : poReceipt.getEscmInitialReceiptList()) {
          PoReceiptLinesResponseDTO lineDTO = new PoReceiptLinesResponseDTO();
          lineDTO.setReceiptLineId(line.getId());
          lineDTO.setItem(line.getProduct() != null ? line.getProduct().getName() : "");
          lineDTO.setDescription(line.getDescription());
          lineDTO.setUom(line.getUOM() != null ? line.getUOM().getName() : "");
          lineDTO.setQuantity(line.getQuantity());
          lineDTO.setUnitprice(line.getUnitprice());
          lineDTO.setReceiveAmount(line.getReceivedAmount());
          lineDTO.setPercentageAchieved(line.getPercentageAchieved());
          lineDTO.setTotalLineAmount(line.getTOTLineAmt());

          lineListDTO.add(lineDTO);
        }
        headerDTO.setLineDTO(lineListDTO);

        poReceiptHeaderList.add(headerDTO);
      }

    } catch (Exception e) {
      log.debug("Error while getting PO Receipt Header DTO Objects", e.getMessage());
      throw new OBException(e.getMessage());
    }
    return poReceiptHeaderList;
  }

  /**
   * This method is used to form the RDVheaderDTO from order list
   *
   * @return list of RDVDTO
   *
   */

  private static List<RDVHeaderResponseDTO> getRdvHeaderDTOList(List<EfinRDV> rdvList) {

    List<RDVHeaderResponseDTO> rdvHeaderList = new ArrayList<RDVHeaderResponseDTO>();
    try {

      for (EfinRDV rdv : rdvList) {
        RDVHeaderResponseDTO headerDTO = new RDVHeaderResponseDTO();
        List<RDVTxnHeaderResponseDTO> txnHdrDTO = new ArrayList<>();
        List<RDVPenaltyAttributesDTO> penaltyDTOList = new ArrayList<RDVPenaltyAttributesDTO>();
        List<RDVHoldAttributesDTO> holdDTOList = new ArrayList<RDVHoldAttributesDTO>();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String txnDate = dateFormat.format(rdv.getTXNDate());

        headerDTO.setRdvId(rdv.getId());
        headerDTO.setTxnHeaderNo(rdv.getDocumentNo());
        headerDTO.setTxnType(rdv.getTXNType());
        headerDTO.setTxnDate(txnDate);
        headerDTO.setOrderNo(rdv.getSalesOrder().getDocumentNo());
        if (rdv.getGoodsShipment() != null)
          headerDTO.setPoReceiptNo(
              rdv.getGoodsShipment() != null ? rdv.getGoodsShipment().getDocumentNo() : null);
        headerDTO.setSupplierNo(
            rdv.getBusinessPartner() != null ? rdv.getBusinessPartner().getName() : null);
        headerDTO.setPoEncumbranceNo(
            rdv.getManualEncumbrance() != null ? rdv.getManualEncumbrance().getDocumentNo() : null);
        headerDTO.setBpBankNo(
            rdv.getPartnerBankAccount() != null ? rdv.getPartnerBankAccount().getAccountNo()
                : null);
        headerDTO.setBankName(
            rdv.getPartnerBankAccount() != null ? rdv.getPartnerBankAccount().getName() : null);
        headerDTO.setIban(rdv.getIBAN());
        headerDTO.setPenaltyStatus(rdv.getPenaltyStatus());
        headerDTO.setTotalContractAmount(rdv.getContractAmt());
        headerDTO.setTotalPenaltyLevied(rdv.getPenaltyAmt());
        headerDTO.setTotalAdvance(rdv.getTOTAdv());
        headerDTO.setLegacyHoldAmt(rdv.getLegacyHoldAmount());
        headerDTO.setTotalDeduction(rdv.getDeductionAmt());
        headerDTO.setLegacyTotalAdvanceDeducted(rdv.getLegacyTotalAdvDeduc());
        headerDTO.setTotalPenaltyDeducted(rdv.getLegacyTotalPenDeduc());
        headerDTO.setOpeningAdvanceBalance(rdv.getLegacyAdvanceBalance());
        headerDTO.setTotalPaidAmtLegacy(rdv.getLegacyTotaladvPaid());
        headerDTO.setTotalReceivedAmt(rdv.getLegacyReceivedAmount());
        headerDTO.setTotalHoldAmt(rdv.getTotalHoldAmount());
        headerDTO.setApplicationHoldAmt(rdv.getApplicationHoldAmount());
        headerDTO.setTotalContractAmountRemaining(rdv.getContractamtRem());
        headerDTO.setTotalPaymentsMade(rdv.getTotpayment());
        headerDTO.setAdvancePercentage(rdv.getADVDeductPercent());
        headerDTO.setAdvanceMethod(rdv.getADVDeductMethod());
        headerDTO.setTotalAdvanceDeducted(rdv.getADVAmt());
        headerDTO.setTotalUninvoicedTxnRaised(rdv.getUninvoicedAmt());
        headerDTO.setTotalUnpaidInvoiceRaised(rdv.getUnpaidinvAmt());

        penaltyDTOList = getRdvPenaltyDTOList(rdv.getId());
        headerDTO.setPenaltyDTO(penaltyDTOList);

        holdDTOList = getRdvHoldDTOList(rdv.getId());
        headerDTO.setHoldDTO(holdDTOList);

        txnHdrDTO = getRdvTxnHeaderDTOList(rdv.getEfinRDVTxnList());
        headerDTO.setTxnVersion(txnHdrDTO);
        rdvHeaderList.add(headerDTO);
      }

    } catch (Exception e) {
      log.debug("Error while getting DTO Objects", e.getMessage());
      throw new OBException(e.getMessage());
    }
    return rdvHeaderList;
  }

  /**
   * This method is used to form the RDVTxnheaderDTO from order list
   *
   * @return list of RDVDTO
   *
   */

  private static List<RDVTxnHeaderResponseDTO> getRdvTxnHeaderDTOList(
      List<EfinRDVTransaction> rdvList) {

    List<RDVTxnHeaderResponseDTO> rdvTxnHeaderList = new ArrayList<RDVTxnHeaderResponseDTO>();
    try {

      for (EfinRDVTransaction hdr : rdvList) {
        RDVTxnHeaderResponseDTO headerDTO = new RDVTxnHeaderResponseDTO();
        List<RDVTxnLineResponseDTO> txnLineDTO = new ArrayList<>();
        List<RDVPenaltyHeaderDTO> penaltyHeaderDTOList = new ArrayList<RDVPenaltyHeaderDTO>();
        List<RDVHoldHeaderDTO> holdHeaderDTOList = new ArrayList<RDVHoldHeaderDTO>();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String cerDate = dateFormat.format(hdr.getCertificateDate());
        String verDate = dateFormat.format(hdr.getTxnverDate());

        headerDTO.setVersionNo(hdr.getTXNVersion() != null ? hdr.getTXNVersion().toString() : null);
        headerDTO.setCertificateNo(hdr.getCertificateNo());
        headerDTO.setCertificateDate(cerDate);
        headerDTO.setVersionDate(verDate);
        headerDTO.setPenalty(hdr.getEfinPenalty());
        if (hdr.getInvoice() != null)
          headerDTO.setInvoiceNo(hdr.getInvoice().getDocumentNo());
        headerDTO.setAmarsarafStatus(hdr.getAmarsarafStatus());
        headerDTO.setPenaltyApplied(hdr.isPenalty());
        headerDTO.setAdvance(hdr.isAdvanced());
        headerDTO.setLegacy(hdr.isLegacy());
        headerDTO.setMatchAmt(hdr.getMatchAmt());
        headerDTO.setPenaltyDeduction(hdr.getPenaltyAmt());
        headerDTO.setAdvanceDeduction(hdr.getADVDeduct());
        headerDTO.setHoldAmount(hdr.getHoldamount());
        headerDTO.setTotalDeduction(hdr.getTOTDeduct());
        headerDTO.setNetMatchAmt(hdr.getNetmatchAmt());
        headerDTO.setAdvanceMethod(hdr.getAdvamtRem());
        headerDTO.setTaxAmt(hdr.getLineTaxamt());

        penaltyHeaderDTOList = getPenaltyHeaderDTOList(hdr.getId());
        headerDTO.setPenaltyHeaderDTO(penaltyHeaderDTOList);

        holdHeaderDTOList = getHoldHeaderDTOList(hdr.getId());
        headerDTO.setHoldHeaderDTO(holdHeaderDTOList);

        txnLineDTO = getRdvTxnLineDTOList(hdr.getEfinRDVTxnlineList());
        headerDTO.setTxnLine(txnLineDTO);
        rdvTxnHeaderList.add(headerDTO);
      }

    } catch (Exception e) {
      log.debug("Error while getting DTO Objects", e.getMessage());
      throw new OBException(e.getMessage());
    }
    return rdvTxnHeaderList;
  }

  /**
   * This method is used to form the RDVTxnLineDTO from order list
   *
   * @return list of RDVDTO
   *
   */

  private static List<RDVTxnLineResponseDTO> getRdvTxnLineDTOList(List<EfinRDVTxnline> rdvList) {

    List<RDVTxnLineResponseDTO> rdvTxnLineList = new ArrayList<RDVTxnLineResponseDTO>();
    try {

      for (EfinRDVTxnline line : rdvList) {
        RDVTxnLineResponseDTO lineDTO = new RDVTxnLineResponseDTO();
        List<RDVPenaltyActionDTO> linePenaltyActionList = new ArrayList<RDVPenaltyActionDTO>();
        List<RDVHoldActionDTO> lineHoldActionList = new ArrayList<RDVHoldActionDTO>();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String matchDate = dateFormat.format(line.getMatchDate());

        if (line.getProductCategory() != null)
          lineDTO.setProductCategory(line.getProductCategory().getCommercialName());
        lineDTO.setMatchDate(matchDate);
        lineDTO.setApplicationNo(line.getTrxappNo());
        if (line.getProduct() != null)
          lineDTO.setItem(line.getProduct().getName());
        lineDTO.setItemDescription(line.getItemDesc());
        if (line.getUOM() != null) {
          lineDTO.setUom(line.getUOM().getName());
        } else {
          lineDTO.setUom(null);
        }
        lineDTO.setUniquecode(line.getAccountingCombination() != null
            ? line.getAccountingCombination().getEfinUniqueCode()
            : null);
        lineDTO.setUniquecodeName(line.getAccountingCombination() != null
            ? line.getAccountingCombination().getEfinUniquecodename()
            : null);
        lineDTO.setIsadvance(line.isAdvance());
        lineDTO.setMatchedAmt(line.getMatchAmt());
        lineDTO.setMatchedQty(line.getMatchQty());
        lineDTO.setExistingMatchedAmt(line.getEximatchAmt());
        lineDTO.setExistingMatchedQty(line.getEximatchQty());
        lineDTO.setUnitPrice(line.getUnitCost());
        lineDTO.setShippedDeliveredQty(line.getDELQty());
        lineDTO.setDeliverAmt(line.getDeliverAmt());
        lineDTO.setCompletionPercentage(line.getCompletionPer());
        lineDTO.setExistingPenalty(line.getExipenaltyAmt());
        lineDTO.setPenaltyAmt(line.getPenaltyAmt());
        lineDTO.setAdvanceAmountRem(line.getAdvamtRem());
        lineDTO.setExistingAdvDeduction(line.getExiadvDeduct());
        lineDTO.setAdvanceMethod(line.getApplicableMethod());
        lineDTO.setAdvanceDeduction(line.getADVDeduct());
        lineDTO.setHoldAmt(line.getHoldamt());
        lineDTO.setTotalDeduction(line.getTotalDeduct());
        lineDTO.setNetMatchAmt(line.getNetmatchAmt());
        lineDTO.setExistingHoldAmt(line.getExistingHoldAmount());
        lineDTO.setTaxAmt(line.getLineTaxamt());

        linePenaltyActionList = getRDVPenaltyActionDTOList(line.getId());
        lineDTO.setPenaltyActionDTO(linePenaltyActionList);

        lineHoldActionList = getRDVHoldActionDTOList(line.getId());
        lineDTO.setHoldActionDTO(lineHoldActionList);

        rdvTxnLineList.add(lineDTO);
      }

    } catch (Exception e) {
      log.debug("Error while getting DTO Objects", e.getMessage());
      throw new OBException(e.getMessage());
    }
    return rdvTxnLineList;
  }

  /**
   * This method is used to form the RDVPenaltyActionDTO from RDV Line ID
   *
   * @return list of RDVPenaltyActionDTO
   *
   */
  public static List<RDVPenaltyActionDTO> getRDVPenaltyActionDTOList(String lineID) {

    List<RDVPenaltyActionDTO> linePenaltyActionList = new ArrayList<RDVPenaltyActionDTO>();

    OBQuery<EfinPenaltyAction> penaltyActionQry = OBDal.getInstance()
        .createQuery(EfinPenaltyAction.class, "as e where e.efinRdvtxnline.id = :txnLineId");
    penaltyActionQry.setNamedParameter("txnLineId", lineID);
    penaltyActionQry.setFilterOnActive(true);
    penaltyActionQry.setFilterOnReadableOrganization(false);
    penaltyActionQry.setFilterOnReadableClients(false);

    if (penaltyActionQry != null) {

      List<EfinPenaltyAction> penaltyActionList = penaltyActionQry.list();

      if (penaltyActionList.size() > 0) {
        for (EfinPenaltyAction penaltyAction : penaltyActionList) {

          RDVPenaltyActionDTO penaltyActionDTO = new RDVPenaltyActionDTO();
          penaltyActionDTO.setPenaltyActionID(penaltyAction.getId());

          penaltyActionDTO.setPenaltyHeaderID(penaltyAction.getEfinPenaltyHeader() != null
              ? penaltyAction.getEfinPenaltyHeader().getId()
              : null);

          penaltyActionDTO.setTxnAppNo(penaltyAction.getTRXAppNo());
          penaltyActionDTO.setMatchAmount(penaltyAction.getAmount());

          penaltyActionDTO.setPenaltyTypeID(penaltyAction.getEfinPenaltyTypes() != null
              ? penaltyAction.getEfinPenaltyTypes().getId()
              : null);

          penaltyActionDTO.setPenaltyPercentage(penaltyAction.getPenaltyPercentage());
          penaltyActionDTO.setPenaltyAmt(penaltyAction.getPenaltyAmount());
          penaltyActionDTO.setBpName(penaltyAction.getName());
          penaltyActionDTO.setAmarsarfAmt(penaltyAction.getAmarsarfAmount());
          penaltyActionDTO.setPenaltyAcctType(penaltyAction.getPenaltyAccountType());

          penaltyActionDTO.setPenaltyUniqueCode(penaltyAction.getPenaltyUniquecode() != null
              ? penaltyAction.getPenaltyUniquecode().getId()
              : null);

          penaltyActionDTO.setRdvTxnLineID(penaltyAction.getEfinRdvtxnline().getId());

          penaltyActionDTO.setBpID(penaltyAction.getBusinessPartner() != null
              ? penaltyAction.getBusinessPartner().getId()
              : null);

          penaltyActionDTO.setInvoiceID(
              penaltyAction.getInvoice() != null ? penaltyAction.getInvoice().getId() : null);

          penaltyActionDTO.setPenaltyID(
              penaltyAction.getEfinPenalty() != null ? penaltyAction.getEfinPenalty().getId()
                  : null);

          penaltyActionDTO.setIsReleased(penaltyAction.isReleased());

          penaltyActionDTO.setPenaltyRelID(
              penaltyAction.getPenaltyRel() != null ? penaltyAction.getPenaltyRel().getId() : null);

          penaltyActionDTO.setReleasedAmt(penaltyAction.getReleasedamt());
          penaltyActionDTO.setEnteredAmt(penaltyAction.getEnteredamt());

          linePenaltyActionList.add(penaltyActionDTO);
        }
      }
    }
    return linePenaltyActionList;
  }

  /**
   * This method is used to form the RDVHoldActionDTO from RDV Line ID
   *
   * @return list of RDVHoldActionDTO
   *
   */
  public static List<RDVHoldActionDTO> getRDVHoldActionDTOList(String lineID) {

    List<RDVHoldActionDTO> lineHoldActionList = new ArrayList<RDVHoldActionDTO>();

    OBQuery<EfinRdvHoldAction> holdActionQry = OBDal.getInstance()
        .createQuery(EfinRdvHoldAction.class, "as e where e.efinRdvtxnline.id = :txnLineId");
    holdActionQry.setNamedParameter("txnLineId", lineID);
    holdActionQry.setFilterOnActive(true);
    holdActionQry.setFilterOnReadableOrganization(false);
    holdActionQry.setFilterOnReadableClients(false);

    if (holdActionQry != null) {

      List<EfinRdvHoldAction> holdActionList = holdActionQry.list();

      if (holdActionList.size() > 0) {
        for (EfinRdvHoldAction holdAction : holdActionList) {

          RDVHoldActionDTO holdActionDTO = new RDVHoldActionDTO();
          holdActionDTO.setHoldActionID(holdAction.getId());

          holdActionDTO.setHoldHeaderID(
              holdAction.getEfinRdvHoldHeader() != null ? holdAction.getEfinRdvHoldHeader().getId()
                  : null);

          holdActionDTO.setTxnAppNo(holdAction.getTxnApplicationNo());
          holdActionDTO.setMatchAmount(holdAction.getAmount());

          holdActionDTO.setHoldTypeID(
              holdAction.getEfinRdvHoldTypes() != null ? holdAction.getEfinRdvHoldTypes().getId()
                  : null);

          holdActionDTO.setHoldPercentage(holdAction.getRDVHoldPercentage());
          holdActionDTO.setHoldAmt(holdAction.getRDVHoldAmount());
          holdActionDTO.setBpName(holdAction.getName());
          holdActionDTO.setAmarsarfAmt(holdAction.getAmrasarfAmount());
          holdActionDTO.setHoldAcctType(holdAction.getRDVHoldAccountType());

          holdActionDTO.setHoldUniqueCode(
              holdAction.getRDVHoldUniquecode() != null ? holdAction.getRDVHoldUniquecode().getId()
                  : null);

          holdActionDTO.setRdvTxnLineID(
              holdAction.getEfinRdvtxnline() != null ? holdAction.getEfinRdvtxnline().getId()
                  : null);

          holdActionDTO.setBpID(
              holdAction.getBusinessPartner() != null ? holdAction.getBusinessPartner().getId()
                  : null);

          holdActionDTO.setInvoiceID(
              holdAction.getInvoice() != null ? holdAction.getInvoice().getId() : null);

          holdActionDTO.setHoldID(
              holdAction.getEfinRdvHold() != null ? holdAction.getEfinRdvHold().getId() : null);

          holdActionDTO.setIsReleased(holdAction.isReleased());

          holdActionDTO.setHoldRelID(
              holdAction.getRDVHoldRel() != null ? holdAction.getRDVHoldRel().getId() : null);

          holdActionDTO.setReleasedAmt(holdAction.getReleasedAmount());
          holdActionDTO.setEnteredAmt(holdAction.getEnteredamt());
          holdActionDTO.setIsTxn(holdAction.isTxn());
          holdActionDTO.setTxnGroupRef(holdAction.getTxngroupref());

          lineHoldActionList.add(holdActionDTO);
        }
      }
    }
    return lineHoldActionList;
  }

  /**
   * This method is used to form the RDVPenaltyHeaderDTO from RDV Txn ID
   *
   * @return list of RDVPenaltyHeaderDTO
   *
   */
  public static List<RDVPenaltyHeaderDTO> getPenaltyHeaderDTOList(String txnID) {

    List<RDVPenaltyHeaderDTO> penaltyHeaderDTOList = new ArrayList<RDVPenaltyHeaderDTO>();

    OBQuery<EfinPenaltyHeader> penaltyHeaderQry = OBDal.getInstance()
        .createQuery(EfinPenaltyHeader.class, "as e where e.efinRdvtxn.id = :txnId");
    penaltyHeaderQry.setNamedParameter("txnId", txnID);
    penaltyHeaderQry.setFilterOnActive(true);
    penaltyHeaderQry.setFilterOnReadableOrganization(false);
    penaltyHeaderQry.setFilterOnReadableClients(false);

    if (penaltyHeaderQry != null) {

      List<EfinPenaltyHeader> penaltyHeaderList = penaltyHeaderQry.list();

      if (penaltyHeaderList.size() > 0) {
        for (EfinPenaltyHeader penaltyHeader : penaltyHeaderList) {

          RDVPenaltyHeaderDTO penaltyHeaderDTO = new RDVPenaltyHeaderDTO();

          penaltyHeaderDTO.setPenaltyHeaderID(penaltyHeader.getId());
          penaltyHeaderDTO.setTxnVersion(penaltyHeader.getTXNVersion());
          penaltyHeaderDTO.setLineNo(penaltyHeader.getLineNo());
          penaltyHeaderDTO.setExistingPenalty(penaltyHeader.getExistingPenalty());
          penaltyHeaderDTO.setPenaltyAmt(penaltyHeader.getPenaltyAmount());
          penaltyHeaderDTO.setUpdatedPenaltyAmt(penaltyHeader.getUpdatedPenaltyAmt());

          penaltyHeaderDTO.setRdvTxnID(
              penaltyHeader.getEfinRdvtxn() != null ? penaltyHeader.getEfinRdvtxn().getId() : null);

          penaltyHeaderDTO.setPenaltyType(
              penaltyHeader.getPenaltyType() != null ? penaltyHeader.getPenaltyType().getId()
                  : null);

          penaltyHeaderDTO.setRdvTxnLineID(
              penaltyHeader.getEfinRdvtxnline() != null ? penaltyHeader.getEfinRdvtxnline().getId()
                  : null);

          penaltyHeaderDTOList.add(penaltyHeaderDTO);
        }
      }
    }
    return penaltyHeaderDTOList;
  }

  /**
   * This method is used to form the RDVHoldHeaderDTO from RDV Txn ID
   *
   * @return list of RDVHoldHeaderDTO
   *
   */
  public static List<RDVHoldHeaderDTO> getHoldHeaderDTOList(String txnID) {

    List<RDVHoldHeaderDTO> holdHeaderDTOList = new ArrayList<RDVHoldHeaderDTO>();

    OBQuery<EfinRdvHoldHeader> holdHeaderQry = OBDal.getInstance()
        .createQuery(EfinRdvHoldHeader.class, "as e where e.efinRdvtxn.id = :txnId");
    holdHeaderQry.setNamedParameter("txnId", txnID);
    holdHeaderQry.setFilterOnActive(true);
    holdHeaderQry.setFilterOnReadableOrganization(false);
    holdHeaderQry.setFilterOnReadableClients(false);

    if (holdHeaderQry != null) {

      List<EfinRdvHoldHeader> holdHeaderList = holdHeaderQry.list();

      if (holdHeaderList.size() > 0) {
        for (EfinRdvHoldHeader holdHeader : holdHeaderList) {

          RDVHoldHeaderDTO holdHeaderDTO = new RDVHoldHeaderDTO();

          holdHeaderDTO.setHoldHeaderID(holdHeader.getId());
          holdHeaderDTO.setTxnVersion(holdHeader.getTxnVersion());
          holdHeaderDTO.setLineNo(holdHeader.getLineNo());
          holdHeaderDTO.setExistingHold(holdHeader.getExistingRdvHold());
          holdHeaderDTO.setHoldAmt(holdHeader.getRDVHoldAmount());
          holdHeaderDTO.setUpdatedHoldAmt(holdHeader.getUpdatedRdvHoldAmt());

          holdHeaderDTO.setRdvTxnID(
              holdHeader.getEfinRdvtxn() != null ? holdHeader.getEfinRdvtxn().getId() : null);

          holdHeaderDTO.setHoldType(
              holdHeader.getRDVHoldType() != null ? holdHeader.getRDVHoldType().getId() : null);

          holdHeaderDTO.setRdvTxnLineID(
              holdHeader.getEfinRdvtxnline() != null ? holdHeader.getEfinRdvtxnline().getId()
                  : null);

          holdHeaderDTOList.add(holdHeaderDTO);
        }
      }
    }
    return holdHeaderDTOList;
  }

  /**
   * This method is used to form the RDVPenaltyAttributesDTO from RDV ID
   *
   * @return list of RDVPenaltyAttributesDTO
   *
   */
  public static List<RDVPenaltyAttributesDTO> getRdvPenaltyDTOList(String rdvID) {

    List<RDVPenaltyAttributesDTO> penaltyDTOList = new ArrayList<RDVPenaltyAttributesDTO>();

    OBQuery<EfinPenalty> penaltyQry = OBDal.getInstance().createQuery(EfinPenalty.class,
        "as e where e.efinRdv.id = :rdvId");
    penaltyQry.setNamedParameter("rdvId", rdvID);
    penaltyQry.setFilterOnActive(true);
    penaltyQry.setFilterOnReadableOrganization(false);
    penaltyQry.setFilterOnReadableClients(false);

    if (penaltyQry != null) {

      List<EfinPenalty> penaltyList = penaltyQry.list();

      if (penaltyList.size() > 0) {
        for (EfinPenalty penalty : penaltyList) {

          RDVPenaltyAttributesDTO penaltyDTO = new RDVPenaltyAttributesDTO();

          penaltyDTO.setPenaltyID(penalty.getId());
          penaltyDTO.setPenaltyPercentage(penalty.getPenaltyPercentage());
          penaltyDTO.setOpeningPenaltyAmt(penalty.getOpeningPenAmount());
          penaltyDTO.setPenaltyApplied(penalty.getPenaltyApplied());
          penaltyDTO.setPenaltyRemaining(penalty.getPenaltyRemaining());
          penaltyDTO.setPenaltyStatus(penalty.getAlertStatus());

          penaltyDTO.setRdvID(penalty.getEfinRdv() != null ? penalty.getEfinRdv().getId() : null);

          penaltyDTO.setPenaltyType(
              penalty.getPenaltyType() != null ? penalty.getPenaltyType().getCode() : null);

          penaltyDTOList.add(penaltyDTO);
        }
      }
    }
    return penaltyDTOList;
  }

  /**
   * This method is used to form the RDVHoldAttributesDTO from RDV ID
   *
   * @return list of RDVHoldAttributesDTO
   *
   */
  public static List<RDVHoldAttributesDTO> getRdvHoldDTOList(String rdvID) {

    List<RDVHoldAttributesDTO> holdDTOList = new ArrayList<RDVHoldAttributesDTO>();

    OBQuery<EfinRdvHold> holdQry = OBDal.getInstance().createQuery(EfinRdvHold.class,
        "as e where e.efinRdv.id = :rdvId");
    holdQry.setNamedParameter("rdvId", rdvID);
    holdQry.setFilterOnActive(true);
    holdQry.setFilterOnReadableOrganization(false);
    holdQry.setFilterOnReadableClients(false);

    if (holdQry != null) {

      List<EfinRdvHold> holdList = holdQry.list();

      if (holdList.size() > 0) {
        for (EfinRdvHold hold : holdList) {

          RDVHoldAttributesDTO holdDTO = new RDVHoldAttributesDTO();

          holdDTO.setHoldID(hold.getId());
          holdDTO.setHoldPercentage(hold.getRDVHoldPercentage());
          holdDTO.setOpeningHoldAmt(hold.getOpeningHoldAmount());
          holdDTO.setHoldApplied(hold.getRDVHoldApplied());
          holdDTO.setHoldRemaining(hold.getRDVHoldRemaining());
          holdDTO.setHoldStatus(hold.getAlertStatus());

          holdDTO.setRdvID(hold.getEfinRdv() != null ? hold.getEfinRdv().getId() : null);

          holdDTO.setHoldType(hold.getRDVHoldType() != null ? hold.getRDVHoldType().getId() : null);

          holdDTOList.add(holdDTO);
        }
      }
    }
    return holdDTOList;
  }
}
