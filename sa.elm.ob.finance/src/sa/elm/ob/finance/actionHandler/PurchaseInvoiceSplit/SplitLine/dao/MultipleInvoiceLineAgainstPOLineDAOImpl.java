package sa.elm.ob.finance.actionHandler.PurchaseInvoiceSplit.SplitLine.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;

import sa.elm.ob.finance.EFIN_TaxMethod;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;

/**
 * 
 * @author Priyanka Ranjan on 04/05/2019
 * 
 */
// Implementation file for Split Invoice Line for a PO

public class MultipleInvoiceLineAgainstPOLineDAOImpl
    implements MultipleInvoiceLineAgainstPOLineDAO {

  private static Logger LOG = Logger.getLogger(MultipleInvoiceLineAgainstPOLineDAOImpl.class);
  Long increment = Long.parseLong("10");

  @Override
  public void cloneInvoiceLine(InvoiceLine invoiceline) throws Exception {
    Long nextLineNo = 0l;
    try {

      Invoice invoice = invoiceline.getInvoice();
      Order order = OBDal.getInstance().get(Order.class, invoice.getEfinCOrder().getId());
      String receivingType = "AMT";
      if (order != null) {
        receivingType = order.getEscmReceivetype();
      }
      InvoiceLine cloneInvoiceLn = (InvoiceLine) DalUtil.copy(invoiceline, false);
      nextLineNo = getNextLineNo(invoiceline, invoiceline.getClient().getId());
      if (nextLineNo != null) {
        cloneInvoiceLn.setLineNo(nextLineNo);
      }

      if ("QTY".equals(receivingType) && invoice.getEfinTaxMethod() != null
          && invoice.getEfinTaxMethod().isPriceIncludesTax()) {
        cloneInvoiceLn.setUnitPrice(invoiceline.getSalesOrderLine().getEscmNetUnitprice());
        cloneInvoiceLn.setGrossUnitPrice(invoiceline.getSalesOrderLine().getEscmNetUnitprice());
      }

      if (order.getEscmTaxMethod() != null
          && order.getEscmTaxMethod() != invoice.getEfinTaxMethod()) {
        /*
         * // v_netamt= //
         * round(cur_lines.em_escm_net_unitprice/1.05,2)*((v_taxpercent/100))+round(cur_lines.
         * em_escm_net_unitprice/1.05,2); BigDecimal orderNetUnitprice =
         * invoiceline.getSalesOrderLine().getEscmNetUnitprice(); Long orderTaxPercent =
         * order.getEscmTaxMethod().getTaxpercent();
         * 
         * BigDecimal baseAmount = orderNetUnitprice.divide( BigDecimal.ONE.add(new
         * BigDecimal(orderTaxPercent).divide(new BigDecimal("100")))); BigDecimal
         */

        cloneInvoiceLn.setUnitPrice(invoiceline.getTaxableAmount()
            .divide(invoiceline.getInvoicedQuantity(), 2, RoundingMode.HALF_DOWN));
        cloneInvoiceLn.setGrossUnitPrice(invoiceline.getTaxableAmount()
            .divide(invoiceline.getInvoicedQuantity(), 2, RoundingMode.HALF_DOWN));

      }

      cloneInvoiceLn.setEfinSecondaryBeneficiary(null);
      cloneInvoiceLn.setBusinessPartner(null);
      cloneInvoiceLn.setLineNetAmount(BigDecimal.ZERO);
      cloneInvoiceLn.setEfinAmtinvoiced(BigDecimal.ZERO);
      cloneInvoiceLn.setEfinIssplitedLine(true);
      cloneInvoiceLn.setEfinCInvoiceline(invoiceline);
      cloneInvoiceLn.setTaxAmount(BigDecimal.ZERO);
      cloneInvoiceLn.setEfinCalculateTax(false);
      cloneInvoiceLn.setTaxableAmount(BigDecimal.ZERO);

      OBDal.getInstance().save(cloneInvoiceLn);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      LOG.error("Exception in cloneInvoiceLine()", e);
    }
  }

  @Override
  public Long getNextLineNo(InvoiceLine invoiceline, String clientId) throws Exception {
    StringBuffer query = null;
    Query delQuery = null;
    Long nextLineNo = 0l;
    BigDecimal currentLineNo = BigDecimal.ZERO;
    try {
      query = new StringBuffer();
      query.append(
          " select max(line) as currentline from c_invoiceline where c_invoice_id=:invoiceId and ad_client_id=:clientId ");
      delQuery = OBDal.getInstance().getSession().createSQLQuery(query.toString());
      delQuery.setParameter("invoiceId", invoiceline.getInvoice().getId());
      delQuery.setParameter("clientId", clientId);
      delQuery.setMaxResults(1);
      if (delQuery != null) {
        if (delQuery.list().size() > 0) {
          currentLineNo = (BigDecimal) delQuery.list().get(0);
          nextLineNo = currentLineNo.longValue() + increment;
        }
      }

    } catch (final Exception e) {
      LOG.error("Exception in getNextLineNo() ", e);
    }
    return nextLineNo;

  }

  @Override
  public Boolean checkAmountValidationForSplit(String clientId, Invoice invoice) throws Exception {
    boolean isError = false;
    BigDecimal invoiceLnAmt = BigDecimal.ZERO;
    BigDecimal poLnAmt = BigDecimal.ZERO;
    BigDecimal currInvAmt = BigDecimal.ZERO;
    BigDecimal comInvAmt = BigDecimal.ZERO;
    Order latestOrder = null, invoiceOrder = null;
    Boolean isInvoiceTax = false;
    Boolean isExclusiveTax = false;
    EFIN_TaxMethod taxmethod = null;
    boolean isTax = false;
    try {
      // fetch distribution org

      if (invoice.getEfinCOrder() != null) {

        // get latest order
        invoiceOrder = OBDal.getInstance().get(Order.class, (invoice.getEfinCOrder().getId()));
        latestOrder = PurchaseInvoiceSubmitUtils.getLatestOrderComplete(invoiceOrder);
        // check any one of the invoice having tax
        if (invoiceOrder != null) {
          Order baseOrder = invoiceOrder.getEscmBaseOrder() != null
              ? invoiceOrder.getEscmBaseOrder()
              : invoiceOrder;
          OBQuery<Invoice> invoiceQry = OBDal.getInstance().createQuery(Invoice.class,
              " as e where e.efinCOrder.id in( select a.id from Order a where ((a.escmBaseOrder.id =:basedOrder and a.escmBaseOrder is not null)"
                  + "  or a.id=:basedOrder)  ) and e.efinIstax='Y' ");
          invoiceQry.setNamedParameter("basedOrder", baseOrder.getId());
          List<Invoice> invoiceList = invoiceQry.list();
          if (invoiceList.size() > 0) {
            isInvoiceTax = true;
            taxmethod = invoiceList.get(0).getEfinTaxMethod();
            if (taxmethod != null && !taxmethod.isPriceIncludesTax()) {
              isExclusiveTax = true;
            }
          }
        }

        if (latestOrder == null) {
          latestOrder = invoiceOrder;
        }
        if (latestOrder != null && !latestOrder.isEscmIstax() && isInvoiceTax && isExclusiveTax) {
          isTax = true;
        }
        // get sum of invoice amount from current invoice
        String query1 = "select sum(coalesce(inln.linenetamt,0)) as currinvamt,inln.c_orderline_id,poln.linenetamt as polinenetamt "
            + "  from c_invoiceline inln "
            + " join c_orderline poln on inln.c_orderline_id=poln.c_orderline_id "
            + " left join c_invoice inv on inv.c_invoice_id=inln.c_invoice_id "
            + "where inln.ad_client_id= :clientID  and inv.c_invoice_id=:invoiceID and inln.em_efin_istax='N' ";
        // if order does not have a tax and invoice having tax then dont consider tax line in
        // invoice for checking invoice amt should not greater than order amt
        if (isTax) {
          query1 += " and inln.em_efin_istax='N' ";
        }
        query1 += " group by inln.c_orderline_id,poln.linenetamt  having sum(coalesce(inln.linenetamt,0)) > poln.linenetamt";
        SQLQuery currentInvQuery = OBDal.getInstance().getSession().createSQLQuery(query1);
        currentInvQuery.setParameter("clientID", clientId);
        currentInvQuery.setParameter("invoiceID", invoice.getId());
        List<Object> currInv = currentInvQuery.list();
        if (currInv != null && currInv.size() > 0) {
          isError = true;
        }
        // get po line net amt

        String query = " select sum(coalesce(inln.linenetamt,0)) as totalinvoicelineamt,inln.c_orderline_id,poln.linenetamt as polinenetamt"
            + " from c_invoiceline inln "
            + " left join c_invoice inv on inv.c_invoice_id=inln.c_invoice_id "
            + " join c_orderline poln on inln.c_orderline_id=poln.c_orderline_id "
            + " where inv.em_efin_c_order_id= :orderID and inln.ad_client_id= :clientID  and inv.docstatus <>'EFIN_CA' and inv.docstatus <>'DR' ";

        // if order does not have a tax and invoice having tax then dont consider tax line in
        // invoice for checking invoice amt should not greater than order amt
        if (isTax) {
          query += " and inln.em_efin_istax='N' ";
        }
        query += " group by inln.c_orderline_id,poln.linenetamt ";

        SQLQuery distOrgQuery = OBDal.getInstance().getSession().createSQLQuery(query);
        distOrgQuery.setParameter("orderID", invoice.getEfinCOrder().getId());
        distOrgQuery.setParameter("clientID", clientId);
        @SuppressWarnings("unchecked")
        List<Object> distOrgList = distOrgQuery.list();
        if (distOrgList != null && distOrgList.size() > 0) {
          for (Object obj : distOrgList) {
            Object[] objDistOrg = (Object[]) obj;
            if (objDistOrg != null) {

              invoiceLnAmt = new BigDecimal(objDistOrg[0].toString());

              BigDecimal currentInvWithoutTaxAmt = invoice.getInvoiceLineList().stream()
                  .filter(a -> a.getSalesOrderLine() != null
                      && a.getSalesOrderLine().getId().equals(objDistOrg[1].toString())
                      && !a.isEFINIsTaxLine()
                      && a.getLineNetAmount().compareTo(BigDecimal.ZERO) > 0)
                  .map(a -> a.getLineNetAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

              BigDecimal currentInvTaxAmt = invoice.getInvoiceLineList().stream()
                  .filter(a -> a.getSalesOrderLine() != null
                      && a.getSalesOrderLine().getId().equals(objDistOrg[1].toString())
                      && a.isEFINIsTaxLine() && a.getLineNetAmount().compareTo(BigDecimal.ZERO) > 0)
                  .map(a -> a.getLineNetAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

              if (isTax) {
                invoiceLnAmt = invoiceLnAmt.add(currentInvWithoutTaxAmt);
              } else {
                invoiceLnAmt = invoiceLnAmt.add(currentInvWithoutTaxAmt.add(currentInvTaxAmt));
              }

              poLnAmt = new BigDecimal(objDistOrg[2].toString());
              if ((invoiceLnAmt != null && poLnAmt != null)
                  && invoiceLnAmt.compareTo(poLnAmt) > 0) {

                isError = true;
              }
            }
          }
        }
      }
    } catch (

    final Exception e) {
      LOG.error("Exception in checkAmountValidationForSplit() ", e);
    }
    return isError;

  }

  @Override
  public Boolean checkLineHavingZeroAmt(Invoice invoice) throws Exception {
    boolean isError = false;
    try {
      for (InvoiceLine objLines : invoice.getInvoiceLineList()) {
        if (objLines.getLineNetAmount().compareTo(BigDecimal.ZERO) == 0) {
          isError = true;
        }
      }
    } catch (final Exception e) {
      LOG.error("Exception in checkAmountValidationForSplit() ", e);
    }
    return isError;
  }

  @Override
  public Boolean checkAlreadyBPCombinationExist(String paymtbeni, String secbene, String clientId,
      String orderLineId, String invoiceId, String invoicelineId) throws Exception {
    boolean isError = false;
    try {
      String query = " select c_invoiceline_id from c_invoiceline where c_orderline_id = :orderLineID "
          + " and c_invoice_id= :invoiceID and c_invoiceline_id <> :invoiceLineID "
          + " and c_bpartner_id= :paymentBeneficiary "
          + " and em_efin_beneficiary2_id= :secondaryBeneficiary and ad_client_id= :clientID and em_efin_istax<>'Y' ";

      SQLQuery distOrgQuery = OBDal.getInstance().getSession().createSQLQuery(query);
      distOrgQuery.setParameter("orderLineID", orderLineId);
      distOrgQuery.setParameter("invoiceID", invoiceId);
      distOrgQuery.setParameter("invoiceLineID", invoicelineId);
      distOrgQuery.setParameter("paymentBeneficiary", paymtbeni);
      distOrgQuery.setParameter("secondaryBeneficiary", secbene);
      distOrgQuery.setParameter("clientID", clientId);
      @SuppressWarnings("unchecked")
      List<Object> distOrgList = distOrgQuery.list();
      if (distOrgList != null && distOrgList.size() > 0) {
        isError = true;
      }
    } catch (final Exception e) {
      LOG.error("Exception in checkAmountValidationForSplit() ", e);
    }
    return isError;

  }
}
