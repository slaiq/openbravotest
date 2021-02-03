/*
 * All Rights Reserved By Qualian Technologies Pvt Ltd.
 */
package sa.elm.ob.finance.ad_process.PurchaseOrder;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinPoAccSummary;
import sa.elm.ob.finance.util.CommonValidations;

public class OrderFundsCheck implements Process {
  /**
   * This process to check funds available status for each line. through funds check button.
   */
  private static final Logger LOG = LoggerFactory.getLogger(OrderFundsCheck.class);
  Query query = null;

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    String strOrderId = (String) bundle.getParams().get("C_Order_ID");
    Order objOrder = OBDal.getInstance().get(Order.class, strOrderId);
    try {
      OBContext.setAdminMode(true);
      fundsCheck(objOrder);
      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
      bundle.setResult(result);
      return;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in Order Funds Check " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method is used to checks the Fund available
   * 
   * @param objOrder
   */
  @SuppressWarnings("rawtypes")
  public void fundsCheck(Order objOrder) {
    try {
      deleteOldRecords(objOrder.getId());

      String sqlQuery = " select sum(ln.linenetamt),ln.em_efin_c_validcombination_id  from c_orderline ln  where ln.c_order_id= ? "
          + " and ln.em_escm_issummarylevel ='N'  group by ln.em_efin_c_validcombination_id  ";
      query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
      query.setParameter(0, objOrder.getId());
      LOG.debug("ordQuery:" + query.toString());
      List queryList = query.list();
      if (queryList != null && queryList.size() > 0) {
        for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
          BigDecimal lineNetAmount = BigDecimal.ZERO;
          BigDecimal fundsAvailable = BigDecimal.ZERO;
          AccountingCombination com = null;
          Object[] row = (Object[]) iterator.next();

          if (row[0] != null)
            lineNetAmount = new BigDecimal(row[0].toString());

          if (row[1] != null) {
            com = OBDal.getInstance().get(AccountingCombination.class, row[1].toString());
            fundsAvailable = getFundsAvailable(objOrder, fundsAvailable, com);
            insertAccountSummary(objOrder, fundsAvailable, lineNetAmount, com);
          }
        }
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in fundsCheck in Order Funds Check " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method is used to get funds available
   * 
   * @param objOrder
   * @param fundsAvailable
   * @param com
   * @return
   */
  public BigDecimal getFundsAvailable(Order objOrder, BigDecimal fundsAvailable,
      AccountingCombination com) {
    BigDecimal fundsAvail = fundsAvailable;
    JSONObject fundsCheckingObject = null;
    try {
      if (objOrder.getEFINEncumbranceMethod().equals("A")) {
        fundsCheckingObject = CommonValidations.getFundsAvailable(objOrder.getEfinBudgetint(), com);
        fundsAvail = new BigDecimal(fundsCheckingObject.get("FA").toString());
      } else if (objOrder.getEFINEncumbranceMethod().equals("M")) {
        String encumId = objOrder.getEfinBudgetManencum().getId();
        OBQuery<EfinBudgetManencumlines> encumLn = OBDal.getInstance().createQuery(
            EfinBudgetManencumlines.class, "as e where e.manualEncumbrance.id=:encumId");
        encumLn.setNamedParameter("encumId", encumId);
        if (encumLn.list().size() > 0) {
          for (EfinBudgetManencumlines encumLines : encumLn.list()) {
            if (com.getId().equals(encumLines.getAccountingCombination().getId())) {
              fundsAvail = encumLines.getRemainingAmount();
            }
          }
        }
      }
      return fundsAvail;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in getFundsAvailable in Order Funds Check " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
    }
    return fundsAvail;
  }

  /**
   * This method is used to delete old account summary if available
   * 
   * @param orderId
   */
  public void deleteOldRecords(String orderId) {
    OBQuery<EfinPoAccSummary> acc = OBDal.getInstance().createQuery(EfinPoAccSummary.class,
        "as e where e.salesOrder.id=:order");
    acc.setNamedParameter("order", orderId);
    if (acc.list().size() > 0) {
      for (EfinPoAccSummary line : acc.list()) {
        OBDal.getInstance().remove(line);
      }
    }
  }

  /**
   * This method is used to insert account summary
   * 
   * @param objOrder
   * @param fundsAvailable
   * @param lineNetAmount
   * @param com
   */
  public void insertAccountSummary(Order objOrder, BigDecimal fundsAvailable,
      BigDecimal lineNetAmount, AccountingCombination com) {
    BigDecimal differ = fundsAvailable.subtract(lineNetAmount);

    // insert Account Summary
    EfinPoAccSummary accSummary = null;
    accSummary = OBProvider.getInstance().get(EfinPoAccSummary.class);
    accSummary.setClient(objOrder.getClient());
    accSummary.setOrganization(objOrder.getOrganization());
    accSummary.setActive(true);
    accSummary.setUpdatedBy(objOrder.getCreatedBy());
    accSummary.setCreationDate(new java.util.Date());
    accSummary.setCreatedBy(objOrder.getCreatedBy());
    accSummary.setUpdated(new java.util.Date());
    accSummary.setEfinCValidcombination(com);
    accSummary.setSalesOrder(objOrder);
    accSummary.setFundsavailable(fundsAvailable);
    accSummary.setAmount(lineNetAmount);
    accSummary.setDifference(differ);
    if (differ.compareTo(BigDecimal.ZERO) >= 0)
      accSummary.setStatus("Success");
    else
      accSummary.setStatus("Failure");

    OBDal.getInstance().save(accSummary);
    OBDal.getInstance().flush();
  }
}
