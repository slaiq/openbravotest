package sa.elm.ob.finance.ad_process;

import java.sql.Connection;

import org.apache.log4j.Logger;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.procurement.Requisition;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.scm.EscmProposalMgmt;

public class EncumbranceCancellationImpl implements EncumbranceCancellationDAO {

  private static final Logger log4j = Logger.getLogger(EncumbranceCancellationImpl.class);

  @SuppressWarnings("unused")
  private Connection conn = null;

  public EncumbranceCancellationImpl(Connection con) {
    this.conn = con;
  }

  public EncumbranceCancellationImpl() {
  }

  @Override
  public Boolean isTransactedEncumbrance(EfinBudgetManencum encumbrance) {
    Boolean isTranscated = Boolean.FALSE;

    try {
      String strEncumbranceType = encumbrance.getEncumType();
      String strEncumbranceId = encumbrance.getId();
      EncumbranceTypeE type = null;

      for (EncumbranceTypeE typeE : EncumbranceTypeE.values()) {
        if (strEncumbranceType.equals(typeE.getEncumbranceType())) {
          type = typeE;
          break;
        }
      }

      int transactions = getTransactions(type, strEncumbranceId);
      if (transactions > 0)
        isTranscated = Boolean.TRUE;

    } catch (Exception e) {
      log4j.error("Exception while isTransactedEncumbrance :" + e);
      e.printStackTrace();
    }
    return isTranscated;
  }

  private int getTransactions(EncumbranceTypeE type, String strEncumbranceId) {
    int transactions = 0;
    try {

      switch (type) {

      case AMARSARF:
        transactions = getInvoiceTransactions(strEncumbranceId);
        break;
      case PREPAYMENT:
        transactions = getInvoiceTransactions(strEncumbranceId);
        break;
      case PROPOSALAWARD:
        transactions = getProposalTransactions(strEncumbranceId);
        break;
      case PURCHASEORDER:
        transactions = getPurchaseOrderTransactions(strEncumbranceId);
        break;
      case REQUISITION:
        transactions = getRequisitionTransactions(strEncumbranceId);
        break;

      default:
        transactions = 0;
        break;
      }
    } catch (Exception e) {
      log4j.error("Exception while getTransactions :" + e);
      e.printStackTrace();
    }
    return transactions;
  }

  private int getRequisitionTransactions(String strEncumbranceId) {
    int transactions = 0;
    try {
      OBQuery<Requisition> requisitionQuery = OBDal.getInstance().createQuery(Requisition.class,
          " where efinBudgetManencum.id = :encumbranceId ");

      requisitionQuery.setNamedParameter("encumbranceId", strEncumbranceId);

      if (requisitionQuery != null)
        transactions = requisitionQuery.list().size();

    } catch (Exception e) {
      log4j.error("Exception while getRequisitionTransactions :" + e);
      e.printStackTrace();
    }
    return transactions;
  }

  private int getPurchaseOrderTransactions(String strEncumbranceId) {
    int transactions = 0;
    try {
      OBQuery<Order> orderQuery = OBDal.getInstance().createQuery(Order.class,
          " where efinBudgetManencum.id = :encumbranceId ");

      orderQuery.setNamedParameter("encumbranceId", strEncumbranceId);

      if (orderQuery != null)
        transactions = orderQuery.list().size();

    } catch (Exception e) {
      log4j.error("Exception while getPurchaseOrderTransactions :" + e);
      e.printStackTrace();
    }
    return transactions;
  }

  private int getProposalTransactions(String strEncumbranceId) {
    int transactions = 0;
    try {
      OBQuery<EscmProposalMgmt> proposalQuery = OBDal.getInstance()
          .createQuery(EscmProposalMgmt.class, " where efinEncumbrance.id = :encumbranceId ");

      proposalQuery.setNamedParameter("encumbranceId", strEncumbranceId);

      if (proposalQuery != null)
        transactions = proposalQuery.list().size();

    } catch (Exception e) {
      log4j.error("Exception while getPurchaseOrderTransactions :" + e);
      e.printStackTrace();
    }
    return transactions;
  }

  private int getInvoiceTransactions(String strEncumbranceId) {
    int transactions = 0;
    try {
      OBQuery<Invoice> invoiceQuery = OBDal.getInstance().createQuery(Invoice.class,
          " where efinManualencumbrance.id = :encumbranceId ");

      invoiceQuery.setNamedParameter("encumbranceId", strEncumbranceId);

      if (invoiceQuery != null)
        transactions = invoiceQuery.list().size();

    } catch (Exception e) {
      log4j.error("Exception while getInvoiceTransactions :" + e);
      e.printStackTrace();
    }
    return transactions;
  }

}
