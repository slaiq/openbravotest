package sa.elm.ob.scm.actionHandler.dao;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.Escmpoamendment;
import sa.elm.ob.scm.ad_process.POandContract.POContractSummaryCancel;

public class POActionChangeDAO {
  private static final Logger log = LoggerFactory.getLogger(POActionChangeDAO.class);

  /**
   * insert into poamendment when action changes
   * 
   * @param Order
   * @param userId
   * @param action
   * @param decreeno
   * @param decreedate
   * @param justification
   * @param onHoldFromDate
   * @param onHoldToDate
   * @param duration
   * @param periodType
   * @return int
   */
  public static int insertPOAmendmentChangeAction(Order order, String userId, String action,
      String decreeno, Date decreedate, String justification, Date onHoldFromDate,
      Date onHoldToDate, Long duration, String periodType) {
    int count = 0;
    Long lineNo = 10L;
    Escmpoamendment poAmendment = null;
    StringBuffer query = null;
    Query amdQuery = null;
    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    try {
      query = new StringBuffer();
      query.append("select max(lineNo) as lineNo from escm_poamendment amend "
          + " where amend.salesOrder.id=:orderId ");
      amdQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      amdQuery.setParameter("orderId", order.getId());
      if (amdQuery.list().size() > 0) {
        for (Object obj : amdQuery.list()) {
          if (obj != null) {
            lineNo = (Long) obj;
            lineNo = lineNo + 10L;
          }
        }
      }

      poAmendment = OBProvider.getInstance().get(Escmpoamendment.class);
      poAmendment.setClient(order.getClient());
      poAmendment.setOrganization(order.getOrganization());
      poAmendment.setCreationDate(new java.util.Date());
      poAmendment.setCreatedBy(order.getCreatedBy());
      poAmendment.setUpdated(new java.util.Date());
      poAmendment.setUpdatedBy(order.getUpdatedBy());
      poAmendment.setSalesOrder(order);

      poAmendment.setLineNo(lineNo);
      poAmendment.setRevisionno(String.valueOf(order.getEscmRevision()));
      poAmendment.setRequestdatehiriji(new Date());
      Date curDate = new Date();
      poAmendment.setRequestdategreg(dateFormat.format(curDate));

      poAmendment.setAction(action);
      poAmendment.setDecreenumber(decreeno);
      poAmendment.setDecreedate(decreedate);
      poAmendment.setJustification(justification);
      OBQuery<OrderLine> ordLnQry = OBDal.getInstance().createQuery(OrderLine.class,
          "as e left join e.eFINUniqueCode where e.escmIssummarylevel='N' and e.salesOrder.id=:orderId ");
      ordLnQry.setNamedParameter("orderId", order.getId());
      if (ordLnQry.list().size() > 0) {
        OrderLine ordLn = ordLnQry.list().get(0);
        poAmendment.setSalesRegion(
            ordLn.getEFINUniqueCode() != null ? ordLn.getEFINUniqueCode().getSalesRegion() : null);
      }

      if (action.equals("ESCM_OHLD") || action.equals("ESCM_EOHLD")) {
        poAmendment.setOnHoldFromDate(onHoldFromDate);
        poAmendment.setPeriodType(periodType.equals("null") ? null : periodType);
        poAmendment.setContractDuration(duration == 0L ? null : duration);
        poAmendment.setOnHoldToDate(onHoldToDate);
      }

      OBDal.getInstance().save(poAmendment);
      count = 1;
    } catch (Exception e) {
      log.error("Exception in insertPOAmendmentChangeAction: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    }
    return count;
  }

  /**
   * get latest onhold end date from poamendment
   * 
   * @param orderId
   * @return Date
   */
  public static Date getPOAmendmentOnHoldEndDate(String orderId) {
    StringBuffer query = null;
    Query amdQuery = null;
    Date onHoldEndDate = null;
    try {
      OBContext.setAdminMode();
      query = new StringBuffer();
      query.append("select onHoldToDate as enddate from escm_poamendment amend "
          + " where amend.salesOrder.id=:orderId and action='ESCM_OHLD' order by created desc");
      amdQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      amdQuery.setParameter("orderId", orderId);
      amdQuery.setMaxResults(1);
      @SuppressWarnings("unchecked")
      List<Date> endDateList = amdQuery.list();
      if (endDateList.size() > 0) {
        onHoldEndDate = endDateList.get(0);
      }
    } catch (Exception e) {
      log.error("Exception while getPOAmendmentOnHoldEndDate:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return onHoldEndDate;
  }

  /**
   * Call PO cancel process
   * 
   * @param orderId
   * @param reason
   * @param vars
   * @return OBError
   */
  public static OBError cancelPO(String orderId, String reason, VariablesSecureApp vars)
      throws Exception {

    String cancelProcessId = "66590FAFF644431CA350BBD39959C98D";

    ProcessBundle pb = new ProcessBundle(cancelProcessId, vars)
        .init(new DalConnectionProvider(true));
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("C_Order_ID", orderId);
    parameters.put("reason", reason);
    pb.setParams(parameters);
    new POContractSummaryCancel().execute(pb);
    OBError error = (OBError) pb.getResult();
    return error;
  }
}
