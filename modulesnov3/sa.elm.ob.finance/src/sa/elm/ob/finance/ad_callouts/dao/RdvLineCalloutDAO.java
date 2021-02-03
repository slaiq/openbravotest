package sa.elm.ob.finance.ad_callouts.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * @author qualian on 20/12/2018
 */

public class RdvLineCalloutDAO {
  /**
   * This Access Layer class is responsible to do database operation in Requisition Process Class
   */
  VariablesSecureApp vars = null;

  private final static Logger log = LoggerFactory.getLogger(RdvLineCalloutDAO.class);

  /**
   * Calculate match amt from orderline
   * 
   * @param orderLineId
   * @param qty
   * @return matchAmt in BigDecimal
   */
  public static BigDecimal getMatchedAmt(String orderLineId, BigDecimal qty) {
    BigDecimal matchAmt = BigDecimal.ZERO;
    try {
      OBContext.setAdminMode();
      OrderLine ordLn = Utility.getObject(OrderLine.class, orderLineId);
      BigDecimal lineNetAmt = ordLn.getLineNetAmount();
      matchAmt = (lineNetAmt.divide(ordLn.getOrderedQuantity(), 20, BigDecimal.ROUND_DOWN))
          .multiply(qty);
    } catch (Exception e) {
      log.error("Exception in getManualEncumId", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return matchAmt;
  }

  public static BigDecimal getOtherLineAdvDeduction(String efinRdvTxnlineId) {
    BigDecimal exisitingAdvDed = BigDecimal.ZERO;
    List<EfinRDVTxnline> rdvLineList = new ArrayList<EfinRDVTxnline>();
    try {
      OBContext.setAdminMode();
      EfinRDVTxnline rdvTxnlineObj = Utility.getObject(EfinRDVTxnline.class, efinRdvTxnlineId);
      OBQuery<EfinRDVTxnline> remainigLinQry = OBDal.getInstance().createQuery(EfinRDVTxnline.class,
          " as e where e.efinRdvtxn.id=:rdvtxnId  and e.id<>:currentLineId ");
      remainigLinQry.setNamedParameter("rdvtxnId", rdvTxnlineObj.getEfinRdvtxn().getId());
      remainigLinQry.setNamedParameter("currentLineId", efinRdvTxnlineId);
      rdvLineList = remainigLinQry.list();
      if (rdvLineList.size() > 0) {
        for (EfinRDVTxnline txnlineObj : rdvLineList) {
          exisitingAdvDed = exisitingAdvDed.add(txnlineObj.getADVDeduct());
        }
      }

    } catch (Exception e) {
      log.error("Exception in getMatchedAmt", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return exisitingAdvDed;
  }

  public static BigDecimal gettotaladvDeduction(String rdvId, String rdvTxnLineId) {
    BigDecimal advDed = BigDecimal.ZERO;
    List<EfinRDVTransaction> rdvLineList = new ArrayList<EfinRDVTransaction>();
    try {
      OBContext.setAdminMode();
      EfinRDVTxnline rdvTxnlineObj = Utility.getObject(EfinRDVTxnline.class, rdvTxnLineId);

      OBQuery<EfinRDVTransaction> remainigLinQry = OBDal.getInstance().createQuery(
          EfinRDVTransaction.class,
          " as e where e.efinRdv.id=:rdvId  and e.id<>:currentVersionId ");
      remainigLinQry.setNamedParameter("rdvId", rdvId);
      remainigLinQry.setNamedParameter("currentVersionId", rdvTxnlineObj.getEfinRdvtxn().getId());
      rdvLineList = remainigLinQry.list();
      if (rdvLineList.size() > 0) {
        for (EfinRDVTransaction transaction : rdvLineList) {
          for (EfinRDVTxnline txnlineObj : transaction.getEfinRDVTxnlineList()) {
            advDed = advDed.add(txnlineObj.getADVDeduct());
          }
        }
      }

    } catch (Exception e) {
      log.error("Exception in getMatchedAmt", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return advDed;
  }

  public static JSONObject calculateTax(EfinRDVTxnline rdvTxnLine, Boolean isPriceInclOfTax,
      BigDecimal matchamt, BigDecimal quantity, BigDecimal taxPercent) {
    JSONObject taxObject = new JSONObject();
    BigDecimal taxFactor = BigDecimal.ONE;
    BigDecimal taxAmount = BigDecimal.ZERO;
    BigDecimal amtwithoutTax = BigDecimal.ZERO;
    BigDecimal lineTotal = BigDecimal.ZERO;
    BigDecimal negotiatedamt = BigDecimal.ZERO;
    Integer roundoffConst = 2;

    try {
      lineTotal = matchamt;
      taxFactor = taxFactor.add(taxPercent);
      if (isPriceInclOfTax) {

        amtwithoutTax = lineTotal.divide(taxFactor, 15, RoundingMode.HALF_UP);
        taxAmount = (lineTotal.subtract(amtwithoutTax)).setScale(roundoffConst,
            RoundingMode.HALF_UP);
        negotiatedamt = (amtwithoutTax.divide(quantity, 15, RoundingMode.HALF_UP))
            .setScale(roundoffConst, RoundingMode.HALF_UP);
        ;
        taxObject.put("LineAmount", negotiatedamt);

      } else {
        lineTotal = lineTotal.divide(taxFactor, 15, RoundingMode.HALF_UP);
        taxAmount = (lineTotal.multiply(taxPercent)).setScale(roundoffConst, RoundingMode.HALF_UP);
        taxObject.put("LineAmount",
            (lineTotal.add(taxAmount)).setScale(roundoffConst, RoundingMode.HALF_UP));
      }
      taxObject.put("TaxAmount", taxAmount);

    } catch (Exception e) {
      try {
        taxObject.put("LineAmount", BigDecimal.ZERO);
        taxObject.put("TaxAmount", BigDecimal.ZERO);
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      log.error("Exception in calculateTax() " + e);
      e.printStackTrace();
    }

    return taxObject;
  }

  public static Date getPOAmendmentExtDate(String orderId) {
    StringBuffer query = null;
    Query amdQuery = null;
    Date extEndDate = null;
    try {
      OBContext.setAdminMode();
      query = new StringBuffer();
      query.append("select max(eXTContractEndDate) as extenddate from escm_poamendment amend "
          + " where amend.salesOrder.id=:orderId "
          + " and amend.eXTContractStartDate=(select max(a.eXTContractStartDate) as startdate from escm_poamendment a "
          + " where a.salesOrder.id=:orderId )");
      amdQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      amdQuery.setParameter("orderId", orderId);
      if (amdQuery != null && amdQuery.list().size() > 0) {
        for (Object obj : amdQuery.list()) {
          if (obj != null) {
            extEndDate = (Date) obj;
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception while getPOAmendmentExtDate:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return extEndDate;
  }

  public static Boolean chkFullyMatchedOrNot(Order order, BigDecimal currentMatchQty,
      BigDecimal currentyMatchAmt, String receivetype, EfinRDVTxnline line) {
    Boolean isfullyMatched = false;
    try {
      OBContext.setAdminMode();

      BigDecimal totalOrderQty = order.getOrderLineList().stream()
          .filter(a -> !a.isEscmIssummarylevel()).map(a -> a.getOrderedQuantity())
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      BigDecimal totalOrderAmt = order.getGrandTotalAmount();

      BigDecimal totalMatchQty = line.getEfinRdv().getEfinRDVTxnlineList().stream()
          .filter(a -> a.isMatch() && !a.isAdvance() && !a.getId().equals(line.getId()))
          .map(a -> a.getMatchQty()).reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal totalMatchAmt = line.getEfinRdv().getEfinRDVTxnlineList().stream()
          .filter(a -> a.isMatch() && !a.isAdvance() && !a.getId().equals(line.getId()))
          .map(a -> a.getMatchAmt()).reduce(BigDecimal.ZERO, BigDecimal::add);

      if (receivetype.equals(Constants.QTY_BASED)) {
        if (totalOrderQty.compareTo(totalMatchQty.add(currentMatchQty)) == 0) {
          isfullyMatched = true;
        }

      } else if (receivetype.equals(Constants.AMOUNT_BASED)) {
        if (totalOrderAmt.compareTo(totalMatchAmt.add(currentyMatchAmt)) == 0) {
          isfullyMatched = true;
        }
      }

    } catch (Exception e) {
      log.error("Exception while chkFullyMatchedOrNot:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return isfullyMatched;
  }

}
