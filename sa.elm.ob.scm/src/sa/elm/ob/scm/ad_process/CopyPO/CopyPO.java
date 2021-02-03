package sa.elm.ob.scm.ad_process.CopyPO;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.openbravo.base.provider.OBProvider;
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
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.calendar.Year;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.EscmOrderlineV;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Mouli.K
 * @implNote This process is to copy all the data from the selected PO & create new PO, except
 *           Contract Category value
 */

//
public class CopyPO implements Process {

  private static Logger log = Logger.getLogger(CopyPO.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    try {
      OBContext.setAdminMode();
      // Variable declaration
      String windowId = "2ADDCB0DD2BF4F6DB13B21BBCCC3038C";
      final String orderId = bundle.getParams().get("C_Order_ID").toString();
      Order orderObj = OBDal.getInstance().get(Order.class, orderId);
      final String clientId = bundle.getContext().getClient();
      final String userId = bundle.getContext().getUser();
      final String roleId = bundle.getContext().getRole();
      User user = OBDal.getInstance().get(User.class, userId);
      String periodId = null, orgID = orderObj.getOrganization().getId();
      PreparedStatement ps = null;
      ResultSet rs = null;
      Date date = new Date();
      SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
      String curDate = formatter.format(date);
      String budInt = "";
      // get the connection
      Connection conn = OBDal.getInstance().getConnection();

      Map<Long, Long> orderIdParRef = new HashMap<Long, Long>();

      if (orderObj != null) {
        // Inserting Order Header
        Order order = (Order) DalUtil.copy(orderObj, false);
        order.setCreationDate(new java.util.Date());
        order.setCreatedBy(user);
        order.setUpdated(new java.util.Date());
        order.setUpdatedBy(user);
        order.setSalesTransaction(false);
        order.setDocumentNo(UtilityDAO.getSequenceNo(conn, clientId,
            orderObj.getTransactionDocument().getDocumentSequence().getName(), true));
        order.setDocumentStatus("DR");
        order.setDocumentAction("CO");
        order.setProcessNow(false);
        order.setProcessed(false);
        order.setDelivered(false);
        order.setReinvoice(false);
        order.setPrint(false);
        order.setSelected(false);
        order.setSalesRepresentative(null);
        Date gregPODate = new java.util.Date();
        order.setOrderDate(gregPODate);

        // Fetching the Financial Year
        if (orderObj.getOrganization().getId().equals("0")) {
          orgID = sa.elm.ob.utility.util.UtilityDAO.getChildOrgwithCalenderId(clientId,
              orderObj.getOrganization().getId());
        }
        if (gregPODate != null) {
          periodId = sa.elm.ob.utility.util.UtilityDAO.getPeriod(formatter.format(gregPODate),
              orgID);
          OBQuery<Period> periodQuery = OBDal.getInstance().createQuery(Period.class,
              " as e where e.id=:periodID");
          periodQuery.setNamedParameter("periodID", periodId);
          if (periodQuery != null) {
            List<Period> period = periodQuery.list();
            if (period.size() > 0) {
              String yearId = period.get(0).getYear().getId();
              order.setEscmFinanyear(OBDal.getInstance().get(Year.class, yearId));
            } else {
              order.setEscmFinanyear(null);
            }
          }
        }
        // Fetching the Budget Definition
        ps = conn.prepareStatement(
            "select init.efin_budgetint_id as DefaultValue from efin_budgetint  init  "
                + " join c_period  frmprd on frmprd.c_period_id= init.fromperiod  "
                + " join c_period  toprd on toprd.c_period_id= init.toperiod  "
                + " where init.ad_client_id= ? and now()::date between  "
                + " to_date(to_char(frmprd.startdate ,'dd-MM-yyyy'),'dd-MM-yyyy') and "
                + " to_date(to_char(toprd.enddate,'dd-MM-yyyy'),'dd-MM-yyyy')  "
                + " and   init.status ='OP' limit 1 ");

        ps.setString(1, clientId);
        rs = ps.executeQuery();
        if (rs.next()) {
          budInt = rs.getString("DefaultValue");
          order.setEfinBudgetint(OBDal.getInstance().get(EfinBudgetIntialization.class, budInt));
        }

        order.setScheduledDeliveryDate(null);
        order.setDatePrinted(null);
        order.setAccountingDate(new java.util.Date());
        order.setInvoiceAddress(null);
        order.setOrderReference(null);
        order.setPrintDiscount(false);
        order.setFormOfPayment("B");
        order.setInvoiceTerms("D");
        order.setDeliveryTerms("A");
        order.setFreightCostRule("I");
        order.setFreightAmount(BigDecimal.ZERO);
        order.setDeliveryMethod("P");
        order.setShippingCompany(null);
        order.setCharge(null);
        order.setChargeAmount(BigDecimal.ZERO);
        order.setPriority("5");
        order.setSalesCampaign(null);
        order.setProject(null);
        order.setActivity(null);
        order.setPosted("N");
        order.setUserContact(null);
        order.setCopyFrom(false);
        order.setDropShipPartner(null);
        order.setDropShipLocation(null);
        order.setDropShipContact(null);
        order.setSelfService(false);
        order.setTrxOrganization(null);
        order.setStDimension(null);
        order.setNdDimension(null);
        order.setDeliveryNotes(null);
        order.setIncoterms(null);
        order.setINCOTERMSDescription(null);
        order.setGenerateTemplate(false);
        order.setDeliveryLocation(null);
        order.setCopyFromPO(false);
        order.setPaymentMethod(null);
        order.setFINPaymentPriority(null);
        order.setPickFromShipment(false);
        order.setReceiveMaterials(false);
        order.setCreateInvoice(false);
        order.setReturnReason(null);
        order.setAddOrphanLine(false);
        order.setAsset(null);
        order.setCalculatePromotions(false);
        order.setCostcenter(null);
        order.setCreateOrder(false);
        order.setRejectReason(null);
        order.setValidUntil(null);
        order.setQuotation(null);
        order.setReservationStatus(null);
        order.setCreatePOLines(false);
        order.setCashVAT(false);
        order.setPickfromreceipt(false);
        order.setAPRMAddPayment(false);
        order.setEfinBudgetManencum(null);
        order.setEfinEncumbered(false);
        order.setEFINEncumbranceMethod("A");
        order.setEFINUniqueCode(null);
        order.setEfinInvoiceAmt(BigDecimal.ZERO);
        order.setEfinPaidAmt(BigDecimal.ZERO);
        order.setEfinRemainingAmt(BigDecimal.ZERO);
        order.setEfinLegacypaidAmt(BigDecimal.ZERO);
        order.setEscmAppstatus("DR");
        order.setEscmContractstartdate(null);
        order.setEscmContractenddate(null);
        order.setEscmContractduration(null);
        order.setEscmPeriodtype(null);
        order.setEscmOnboarddateh(null);
        order.setEscmOnboarddategreg(null);
        order.setEscmProposalmgmt(null);
        order.setEscmProposaldate(null);
        order.setEscmBidmgmt(null);
        order.setEscmRate(BigDecimal.ZERO);
        order.setEscmRatedategre(curDate);
        order.setEscmPoamount(null);
        order.setEscmProjectname(null);
        order.setEscmRevision(0L);
        order.setEscmAddrequisition(false);
        order.setEscmAddproposal(false);
        order.setEscmProposalno(null);
        order.setEscmDocaction("CO");
        order.setEscmReactivate(false);
        order.setESCMCancel(false);
        order.setEscmReject(false);
        order.setEscmReceivingControl("OP");
        order.setEscmOldOrder(null);
        order.setEscmCreateversion(false);
        order.setEscmBaseOrder(null);
        order.setEscmRefreshversion(false);
        order.setEscmAdvpaymntPercntge(BigDecimal.ZERO);
        order.setEscmAdvpaymntAmt(BigDecimal.ZERO);
        order.setEscmRetainPercn(BigDecimal.ZERO.longValue());
        order.setEscmTotretainAmt(BigDecimal.ZERO.longValue());
        order.setEscmAcctno(null);
        order.setEscmAdRole(OBContext.getOBContext().getRole());
        order.setEscmContactType(null);
        order.setEscmPocopy(true);
        // if the contract category list size is one then set the value
        ps = conn.prepareStatement(
            " select ESCM_DefLookups_TypeLn_id from ESCM_DefLookups_TypeLn where "
                + " (case when (select count(ordln.c_orderline_id) from c_orderline ordln  "
                + " where ordln.c_order_id=? and ordln.m_product_id is not null)>0 then "
                + " (escm_deflookups_typeln_id in (select pcontctg.contractcategory from c_orderline ordln "
                + " left join m_product prod on prod.m_product_id=ordln.m_product_id "
                + " left join escm_prod_cont_catg pcontctg on prod.m_product_id=pcontctg.m_product_id "
                + " where ordln.c_order_id=? and pcontctg.isactive='Y' "
                + " and pcontctg.contractcategory is not null) ) "
                + " else (escm_deflookups_type_id in ( select  escm_deflookups_type_id from escm_deflookups_type "
                + " where reference ='POCONCATG' and isactive='Y')) end ) and "
                + " escm_deflookups_typeln_id in (select escm_deflookups_typeln_id from eut_lookup_access "
                + " where ad_role_id= ? and isactive='Y' and  ad_window_id=? ) ",
            ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        ps.setString(1, orderId);
        ps.setString(2, orderId);
        ps.setString(3, roleId);
        ps.setString(4, windowId);
        rs = ps.executeQuery();
        if (rs != null) {
          rs.beforeFirst();
          rs.last();
          if (rs.getRow() == 1) {
            order.setEscmContactType(OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
                rs.getString("ESCM_DefLookups_TypeLn_id")));
          }
        }

        order.setEscmLegacyContractNo(null);
        order.setEscmLegacycontract(null);
        order.setEscmForward(false);
        order.setEscmForwardRevoke(false);
        order.setEscmReqMoreInfo("N");
        order.setEscmRmiRevoke(false);
        order.setEscmDeleteNewVersion(false);
        order.setEscmLegacyAdvPaymentAmt(null);
        order.setEscmLegacyAdvDeduction(null);
        order.setEscmAdvpaymntPercntge(BigDecimal.ZERO);
        order.setEscmLOpeningAdvBalance(null);
        order.setEscmPurReleaseqty(false);
        order.setEscmPurReleaseamt(false);
        order.setEscmReleasecount(BigDecimal.ZERO.longValue());
        order.setEscmReleaseamount(BigDecimal.ZERO);
        order.setEscmPurchaseagreement(null);
        order.setEscmAddlinesqty(false);
        order.setEscmAddlinesamt(false);
        order.setEscmMinRelease(BigDecimal.ZERO);
        order.setEscmMaxRelease(BigDecimal.ZERO);
        order.setEscmChangeaction(false);
        order.setEutNextRole(null);
        order.setEutForward(null);
        order.setEutReqmoreinfo(null);
        order.setEscmIntSystemName(null);
        order.setEscmIntSystemDesc(null);
        order.setEscmMmsLegacyNo(null);
        order.setEscmMmsComputerNo(null);
        order.setEscmInitialamtinclusive(BigDecimal.ZERO);
        order.setEfinOrgLegacypaidAmt(null);
        order.setSummedLineAmount(null);
        order.setEfinRemainingAmt(BigDecimal.ZERO);
        OBDal.getInstance().save(order);

        // Inserting Order Lines
        for (OrderLine orderLineObj : orderObj.getOrderLineList()) {
          OrderLine orderLine = OBProvider.getInstance().get(OrderLine.class);
          orderLine.setSalesOrder(order);
          orderLine.setOrganization(orderLineObj.getOrganization());
          orderLine.setCreationDate(new java.util.Date());
          orderLine.setCreatedBy(user);
          orderLine.setUpdated(new java.util.Date());
          orderLine.setUpdatedBy(user);
          orderLine.setActive(true);
          orderLine.setLineNo(orderLineObj.getLineNo());
          orderLine.setWarehouse(orderLineObj.getWarehouse());
          orderLine.setCurrency(orderLineObj.getCurrency());
          orderLine.setTax(orderLineObj.getTax());
          orderLine.setOrderDate(new java.util.Date());
          orderLine.setProduct(orderLineObj.getProduct());
          orderLine.setEscmProdescription(orderLineObj.getEscmProdescription());
          orderLine.setEscmProductCategory(orderLineObj.getEscmProductCategory());
          orderLine.setUOM(orderLineObj.getUOM());
          orderLine.setOrderedQuantity(orderLineObj.getOrderedQuantity());
          orderLine.setEscmInitialUnitprice(orderLineObj.getEscmInitialUnitprice());
          orderLine.setUnitPrice(orderLineObj.getUnitPrice());
          orderLine.setEscmPoChangeType(orderLineObj.getEscmPoChangeType());
          orderLine.setEscmPoChangeValue(orderLineObj.getEscmPoChangeValue());
          orderLine.setEscmPoChangeFactor(orderLineObj.getEscmPoChangeFactor());
          orderLine.setEscmUnitpriceAfterchag(orderLineObj.getEscmUnitpriceAfterchag());
          orderLine.setEscmUnittax(orderLineObj.getEscmUnittax());
          orderLine.setEscmLineTaxamt(orderLineObj.getEscmLineTaxamt());
          orderLine.setEscmNetUnitprice(orderLineObj.getEscmNetUnitprice());
          orderLine.setLineNetAmount(orderLineObj.getLineNetAmount());
          orderLine.setEscmRounddiffTax(orderLineObj.getEscmRounddiffTax());
          orderLine.setEscmIssummarylevel(orderLineObj.isEscmIssummarylevel());
          orderLine.setEscmNeedbydate(new java.util.Date());
          orderLine.setEscmComments(orderLineObj.getEscmComments());
          orderLine.setEscmNationalproduct(orderLineObj.getEscmNationalproduct());
          orderLine.setBusinessPartner(orderLineObj.getBusinessPartner());
          orderLine.setEscmLineTotalUpdated(orderLineObj.getEscmLineTotalUpdated());
          if (orderLineObj.getEscmParentline() != null) {
            OrderLine parentReqLnNo = OBDal.getInstance().get(OrderLine.class,
                orderLineObj.getEscmParentline().getId());
            orderIdParRef.put(orderLineObj.getLineNo(), parentReqLnNo.getLineNo());
          }
          OBDal.getInstance().save(orderLine);
        }
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(order);

        // Inserting the Parent-Child relation if exist.
        int mapSize = orderIdParRef.size();
        if (orderIdParRef != null && mapSize > 0) {
          OBQuery<OrderLine> newLines = OBDal.getInstance().createQuery(OrderLine.class,
              " as e where salesOrder.id='" + order.getId() + "'");

          if (newLines != null && newLines.list().size() > 0) {
            for (int i = 0; i < newLines.list().size(); i++) {
              OrderLine lns = newLines.list().get(i);
              Long lineNo = lns.getLineNo();
              Long parentLineno = orderIdParRef.get(lineNo);
              if (parentLineno != null) {
                OBQuery<OrderLine> parentId = OBDal.getInstance().createQuery(OrderLine.class,
                    " as e where e.lineNo =:parentLineno and salesOrder.id= :orderId ");
                parentId.setNamedParameter("parentLineno", parentLineno);
                parentId.setNamedParameter("orderId", order.getId());

                if (parentId != null && parentId.list().size() > 0) {
                  OrderLine parentLn = parentId.list().get(0);
                  EscmOrderlineV objParentLine = OBDal.getInstance().get(EscmOrderlineV.class,
                      parentLn.getId());
                  if (objParentLine != null) {
                    lns.setEscmParentline(objParentLine);
                    OBDal.getInstance().save(lns);
                  }
                }
              }
            }
          }
        }

        // Setting up the success message
        String message = OBMessageUtils.messageBD("ESCM_CopyPO_Success");
        message = message.replaceFirst("%", orderObj.getDocumentNo());
        message = message.replace("%", order.getDocumentNo());
        OBError result = OBErrorBuilder.buildMessage(null, "success", message);
        bundle.setResult(result);
      }
    } catch (Exception e) {
      log.error("Exeception in CopyPO Process:", e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
