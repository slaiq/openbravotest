package sa.elm.ob.scm.ad_callouts.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.ad_process.POandContract.dao.POContractSummaryDAO;
import sa.elm.ob.utility.util.Utility;

public class POContractSummaryTotPOChangeDAO {
  private static final Logger log = LoggerFactory.getLogger(POContractSummaryTotPOChangeDAO.class);

  /**
   * Get the POChangeType Lookup id
   * 
   * @param reference
   * @param value
   * @return LookUpId in String
   */
  public static String getPOChangeLookUpId(String reference, String value) {
    String lookupId = null;
    String whereClause = " as e where e.searchKey = ? and e.escmDeflookupsType.id in (select ltyp.id from ESCM_DefLookups_Type ltyp where ltyp.reference=? and ltyp.active='Y') ";
    List<Object> paramList = new ArrayList<Object>();
    paramList.add(value);
    paramList.add(reference);
    try {
      OBContext.setAdminMode();
      OBQuery<ESCMDefLookupsTypeLn> lookupQry = OBDal.getInstance()
          .createQuery(ESCMDefLookupsTypeLn.class, whereClause, paramList);
      if (lookupQry.list().size() > 0) {
        ESCMDefLookupsTypeLn lookLn = lookupQry.list().get(0);
        lookupId = lookLn.getId();
      }
    } catch (Exception e) {
      log.error("exception in getPOChangeLookUpId: ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return lookupId;
  }

  /**
   * Get sum of top Level Parent Amount
   * 
   * @param orderId
   * @return totPOUpdateAmt in bigdecimal
   */
  /*
   * public static BigDecimal getTopLevelParentAmt(String orderId) { BigDecimal totPOUpdateAmt =
   * BigDecimal.ZERO; StringBuffer query = null; Query lnQuery = null; try {
   * OBContext.setAdminMode(); query = new StringBuffer(); query.append(
   * "SELECT coalesce(SUM(ordLn.escmLineTotalUpdated), 0) as updatedprice FROM OrderLine ordLn where ordLn.salesOrder.id=:orderId and ordLn.escmParentline is null"
   * ); lnQuery = OBDal.getInstance().getSession().createQuery(query.toString());
   * lnQuery.setParameter("orderId", orderId); log.debug(" Query : " + query.toString()); if
   * (lnQuery != null) { if (lnQuery.list().size() > 0) { if (lnQuery.iterate().hasNext()) {
   * totPOUpdateAmt = new BigDecimal(lnQuery.iterate().next().toString()); } } } } catch
   * (OBException e) { log.error("Exception while getTopLevelParentAmt:" + e); throw new
   * OBException(e.getMessage()); } finally { OBContext.restorePreviousMode(); } return
   * totPOUpdateAmt; }
   */

  /**
   * Update parent po change type to header po change type when Line parent percentage and calc line
   * updated amount and update
   * 
   * @param orderId
   * @param poChangeValue
   * @return
   */
  /*
   * public static void updateParentPOchange(String orderId, BigDecimal poChangeValue) { String
   * whereClause = " as e where e.escmIssummarylevel = 'N' and e.salesOrder.id=? "; List<Object>
   * paramList = new ArrayList<Object>(); paramList.add(orderId); try { OBContext.setAdminMode();
   * String poChangeType = getPOChangeLookUpId("POCHGTYP", "02");
   * 
   * String hql =
   * "update OrderLine set updated=now(), escmPoChangeType=:poChangeType, escmPoChangeValue=:poChangeValue where salesOrder.id=:orderId and escmIssummarylevel = 'N' "
   * ; Query updQuery = OBDal.getInstance().getSession().createQuery(hql);
   * updQuery.setParameter("poChangeType", Utility.getObject(ESCMDefLookupsTypeLn.class,
   * poChangeType)); updQuery.setParameter("poChangeValue", poChangeValue);
   * updQuery.setParameter("orderId", orderId); updQuery.executeUpdate(); // Update child Line PO
   * Values to empty // updateChildLines(orderId); OBQuery<OrderLine> parQry =
   * OBDal.getInstance().createQuery(OrderLine.class, whereClause, paramList); if
   * (parQry.list().size() > 0) { String decFactId = getPOChangeLookUpId("TPOCHGFACT", "01"); String
   * incFactId = getPOChangeLookUpId("TPOCHGFACT", "02"); BigDecimal poUpdatedAmt = BigDecimal.ZERO;
   * for (OrderLine parLn : parQry.list()) { if (parLn.getEscmPoChangeFactor() != null) { if
   * (parLn.getEscmPoChangeFactor().getId().equals(decFactId)) { poUpdatedAmt =
   * parLn.getLineNetAmount().subtract( parLn.getLineNetAmount().multiply(poChangeValue.divide(new
   * BigDecimal("100")))); } else if (parLn.getEscmPoChangeFactor().getId().equals(incFactId)) {
   * poUpdatedAmt = parLn.getLineNetAmount().add(
   * parLn.getLineNetAmount().multiply(poChangeValue.divide(new BigDecimal("100")))); } } // Update
   * lineTotalUpdated after applying percentage parLn.setEscmLineTotalUpdated(poUpdatedAmt); } } }
   * catch (OBException e) { log.error("Exception while updateParentPOchange:" + e); throw new
   * OBException(e.getMessage()); } finally { OBContext.restorePreviousMode(); } }
   */

  /**
   * Update child po change type to null and line updated amt to line net amt when header total po
   * change type is line parent percentage
   * 
   * @param orderId
   * @return
   */
  /*
   * public static void updateChildLines(String orderId) { try { OBContext.setAdminMode();
   * OBQuery<OrderLine> chlLnQry = OBDal.getInstance().createQuery(OrderLine.class,
   * " as e where e.salesOrder.id=:orderId and escmParentline is not null ");
   * chlLnQry.setNamedParameter("orderId", orderId); if (chlLnQry.list().size() > 0) { for
   * (OrderLine chlLn : chlLnQry.list()) { String hql =
   * "update OrderLine set updated=now(), escmPoChangeType=null, escmPoChangeValue=null, escmPoChangeFactor=null, escmLineTotalUpdated=:lineUpdatedAmt  "
   * + " where id=:orderLineId "; Query updQuery =
   * OBDal.getInstance().getSession().createQuery(hql); updQuery.setParameter("lineUpdatedAmt",
   * chlLn.getLineNetAmount()); updQuery.setParameter("orderLineId", chlLn.getId());
   * updQuery.executeUpdate(); } } } catch (OBException e) {
   * log.error("Exception while updateChildLines:" + e); throw new OBException(e.getMessage()); }
   * finally { OBContext.restorePreviousMode(); } }
   */

  /**
   * Get sum of line updated amt from order line
   * 
   * @param orderLineId
   * @return lineUpdatedAmt in bigdecimal
   */
  /*
   * public static BigDecimal getLineNetAmt(String orderLineId) { StringBuffer query = null; Query
   * lnQuery = null; BigDecimal lineUpdatedAmt = BigDecimal.ZERO; try { OBContext.setAdminMode();
   * query = new StringBuffer(); query.append(
   * "SELECT coalesce(SUM(ordLn.escmLineTotalUpdated), 0) as updatedprice FROM OrderLine ordLn where ordLn.escmParentline.id=:orderLineId"
   * ); lnQuery = OBDal.getInstance().getSession().createQuery(query.toString());
   * lnQuery.setParameter("orderLineId", orderLineId);
   * 
   * log.debug(" Query : " + query.toString()); if (lnQuery != null) { if (lnQuery.list().size() >
   * 0) { if (lnQuery.iterate().hasNext()) { lineUpdatedAmt = new
   * BigDecimal(lnQuery.iterate().next().toString()); } } } } catch (OBException e) {
   * log.error("Exception while getLineNetAmt:" + e); throw new OBException(e.getMessage()); }
   * finally { OBContext.restorePreviousMode(); } return lineUpdatedAmt; }
   */

  /**
   * Update parent po change type to header po change type when Line parent percentage and calc line
   * updated amount and update
   * 
   * @param orderId
   * @param poChangeValue
   * @return
   */
  /*
   * public static void updateLinePOchange(String orderId, BigDecimal poChangeValue, String
   * poChangeFactor) { String whereClause =
   * " as e where e.escmIssummarylevel = 'N' and e.salesOrder.id=? "; List<Object> paramList = new
   * ArrayList<Object>(); paramList.add(orderId); try { OBContext.setAdminMode(); String
   * poChangeType = getPOChangeLookUpId("POCHGTYP", "02");
   * 
   * String hql =
   * "update OrderLine set updated=now(), escmPoChangeType=:poChangeType, escmPoChangeValue=:poChangeValue, escmPoChangeFactor=:poChangeFactor where salesOrder.id=:orderId and escmIssummarylevel = 'N' "
   * ; Query updQuery = OBDal.getInstance().getSession().createQuery(hql);
   * updQuery.setParameter("poChangeType", Utility.getObject(ESCMDefLookupsTypeLn.class,
   * poChangeType)); updQuery.setParameter("poChangeValue", poChangeValue);
   * updQuery.setParameter("poChangeFactor", poChangeFactor); updQuery.setParameter("orderId",
   * orderId); updQuery.executeUpdate(); // Update child Line PO Values to empty //
   * updateChildLines(orderId); OBQuery<OrderLine> parQry =
   * OBDal.getInstance().createQuery(OrderLine.class, whereClause, paramList); if
   * (parQry.list().size() > 0) { String decFactId = getPOChangeLookUpId("TPOCHGFACT", "01"); String
   * incFactId = getPOChangeLookUpId("TPOCHGFACT", "02"); BigDecimal poUpdatedAmt = BigDecimal.ZERO;
   * for (OrderLine chldLn : parQry.list()) { if (chldLn.getEscmPoChangeFactor() != null) { if
   * (chldLn.getEscmPoChangeFactor().getId().equals(decFactId)) { poUpdatedAmt =
   * chldLn.getLineNetAmount().subtract( chldLn.getLineNetAmount().multiply(poChangeValue.divide(new
   * BigDecimal("100")))); } else if (chldLn.getEscmPoChangeFactor().getId().equals(incFactId)) {
   * poUpdatedAmt = chldLn.getLineNetAmount().add(
   * chldLn.getLineNetAmount().multiply(poChangeValue.divide(new BigDecimal("100")))); } } // Update
   * lineTotalUpdated after applying percentage chldLn.setEscmLineTotalUpdated(poUpdatedAmt); } } }
   * catch (OBException e) { log.error("Exception while updateParentPOchange:" + e); throw new
   * OBException(e.getMessage()); } finally { OBContext.restorePreviousMode(); } }
   */

  /**
   * Update flag po change value is calc for all lines
   * 
   * @param orderId
   * 
   */
  public static void updatePOLines(String orderId) {
    try {
      OBContext.setAdminMode();
      String hql = "update OrderLine set updated=now(), escmIsPochgevalcalc='Y' where salesOrder.id=:orderId ";
      Query updQuery = OBDal.getInstance().getSession().createQuery(hql);

      updQuery.setParameter("orderId", orderId);
      updQuery.executeUpdate();
    } catch (Exception e) {
      log.error("Exception while updatePOLines:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Check po change value is calc already for the lines
   * 
   * @param orderId
   * @return count in int
   */
  public static int checkAlreadyValuesSet(String orderId) {
    int count = 0;
    String whereClause = " as e where e.salesOrder.id= ? and e.escmIsPochgevalcalc='Y' ";
    List<Object> paramList = new ArrayList<Object>();
    paramList.add(orderId);
    try {
      OBContext.setAdminMode();
      OBQuery<OrderLine> ordLnQry = OBDal.getInstance().createQuery(OrderLine.class, whereClause,
          paramList);
      if (ordLnQry != null && ordLnQry.list().size() > 0) {
        count = ordLnQry.list().size();
      }
    } catch (Exception e) {
      log.error("Exception while checkAlreadyValuesSet:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  /**
   * Get child lines
   * 
   * @param orderId
   * @return List<OrderLine>
   */
  public static List<OrderLine> getOrderChildLineList(String orderId) {
    String whereClause = " as e where e.salesOrder.id= ? and e.escmIssummarylevel='N' ";
    List<Object> paramList = new ArrayList<Object>();
    paramList.add(orderId);
    List<OrderLine> ordLnLs = new ArrayList<OrderLine>();
    try {
      OBContext.setAdminMode();
      OBQuery<OrderLine> ordLnQry = OBDal.getInstance().createQuery(OrderLine.class, whereClause,
          paramList);
      if (ordLnQry != null && ordLnQry.list().size() > 0) {
        ordLnLs = ordLnQry.list();
      }
    } catch (Exception e) {
      log.error("Exception while getOrderChildLineList:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return ordLnLs;
  }

  /**
   * Set po change values to all child lines and calculate line net amt
   * 
   * @param Order
   * @return int
   */
  public static int setChangeValueToAllLines(Order ord, int pricePrecision) {
    int setValue = 0;
    List<OrderLine> ordLnLs = null;
    BigDecimal lnPriceUpdatedAmt = BigDecimal.ZERO;
    String lineChangeTypeId = "";
    BigDecimal taxPercent = BigDecimal.ZERO;
    int roundoffConst = 2;
    try {
      OBContext.setAdminMode();
      if (ord.getClient().getCurrency() != null
          && ord.getClient().getCurrency().getStandardPrecision() != null)
        roundoffConst = ord.getClient().getCurrency().getStandardPrecision().intValue();
      if (ord.getEscmTaxMethod() != null) {
        taxPercent = new BigDecimal(ord.getEscmTaxMethod().getTaxpercent());
      }
      ordLnLs = getOrderChildLineList(ord.getId());

      // Get total Amount Lookup Id
      String totalAmtChangeTypeId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("TPOCHGTYP",
          "01");
      // Get total Percent Lookup Id
      String totalPercentChangeTypeId = POContractSummaryTotPOChangeDAO
          .getPOChangeLookUpId("TPOCHGTYP", "02");

      // Get Line amount Lookup Id
      String lineAmtChangeTypeId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGTYP",
          "01");
      // Get Line Percent Lookup Id
      String linePercentChangeTypeId = POContractSummaryTotPOChangeDAO
          .getPOChangeLookUpId("POCHGTYP", "02");

      String decFactId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGFACT", "01");

      BigDecimal sumOfLine = BigDecimal.ZERO;
      if (ordLnLs != null && ordLnLs.size() > 0) {
        BigDecimal lineChangeValue = BigDecimal.ZERO;
        BigDecimal totalOrderremAmt = totalRemainingAmt(ord);

        if (ord.getEscmTotPoChangeType().getId().equals(totalPercentChangeTypeId)) {
          lineChangeValue = ord.getEscmTotPoChangeValue();
          lineChangeTypeId = linePercentChangeTypeId;
        } else if (ord.getEscmTotPoChangeType().getId().equals(totalAmtChangeTypeId)) {
          // int noOfLines = ordLnLs.size();
          // BigDecimal lineCount = new BigDecimal(noOfLines);

          if (ord.getEscmTotPoChangeFactor() != null
              && ord.getEscmTotPoChangeFactor().getId().equals(decFactId)) {
            if (totalOrderremAmt.compareTo(BigDecimal.ZERO) > 0)
              lineChangeValue = (ord.getEscmTotPoChangeValue().divide(totalOrderremAmt, 20,
                  BigDecimal.ROUND_DOWN));
          } else {
            if (ord.getEscmTotPoUpdatedAmt().compareTo(BigDecimal.ZERO) > 0)
              lineChangeValue = (ord.getEscmTotPoChangeValue().divide(ord.getEscmTotPoUpdatedAmt(),
                  20, BigDecimal.ROUND_DOWN));
          }

          lineChangeTypeId = lineAmtChangeTypeId;
        }

        log.debug("lineChangeValue>" + lineChangeValue);
        for (int i = 0; i < ordLnLs.size(); i++) {
          // for (OrderLine ordLn : ordLnLs) {
          OrderLine ordLn = ordLnLs.get(i);
          BigDecimal changeValue = BigDecimal.ZERO;
          BigDecimal remAmt = remainingAmtLines(ordLn);

          if (ord.getEscmTotPoChangeType().getId().equals(totalAmtChangeTypeId)) {
            if (i == (ordLnLs.size() - 1)) {
              changeValue = ord.getEscmTotPoChangeValue().subtract(sumOfLine);
              changeValue = changeValue.setScale(2, BigDecimal.ROUND_HALF_UP);
            } else {
              if (ord.getEscmTotPoChangeFactor() != null
                  && ord.getEscmTotPoChangeFactor().getId().equals(decFactId)) {
                changeValue = lineChangeValue.multiply(remAmt);
                changeValue = changeValue.setScale(2, BigDecimal.ROUND_HALF_UP);
                sumOfLine = sumOfLine.add(changeValue);
              } else {
                changeValue = lineChangeValue.multiply(ordLn.getEscmLineTotalUpdated());
                changeValue = changeValue.setScale(2, BigDecimal.ROUND_HALF_UP);
                sumOfLine = sumOfLine.add(changeValue);
              }
            }
          } else if (ord.getEscmTotPoChangeType().getId().equals(totalPercentChangeTypeId)) {

            if (ord.getEscmTotPoChangeFactor() != null
                && ord.getEscmTotPoChangeFactor().getId().equals(decFactId)) {
              changeValue = remAmt.multiply((lineChangeValue).divide(new BigDecimal("100")));
            } else {
              changeValue = lineChangeValue;
            }
          }
          log.debug("changeValue>" + changeValue);
          changeValue = changeValue.setScale(2, BigDecimal.ROUND_DOWN);

          if (ord.getEscmTotPoChangeFactor().getId().equals(decFactId)) {
            if (totalAmtChangeTypeId.equals(ord.getEscmTotPoChangeType().getId())) {
              if (ordLn.getEscmLineTotalUpdated().compareTo(changeValue) < 0) {
                setValue = 5;// Validation Error
                return setValue;
              }
            }
            // else {
            // if (new BigDecimal("100").compareTo(changeValue) == 0) {
            // setValue = 5;// Validation Error
            // break;
            // }
            // }
          }
          if (ordLn.getEscmLineTotalUpdated().compareTo(BigDecimal.ZERO) > 0) {
            ESCMDefLookupsTypeLn lookup = Utility.getObject(ESCMDefLookupsTypeLn.class,
                lineChangeTypeId);
            ordLn.setEscmPoChangeType(lookup);
            ordLn.setEscmPoChangeFactor(ord.getEscmTotPoChangeFactor());

            if (ord.getEscmTotPoChangeType().getId().equals(totalPercentChangeTypeId)) {
              ordLn.setEscmPoChangeValue(lineChangeValue);
            } else {
              ordLn.setEscmPoChangeValue(changeValue);
            }

            /*
             * Task No. lnPriceUpdatedAmt = calculateLineUpdatedAmt(lineChangeTypeId,
             * ord.getEscmTotPoChangeFactor().getId(), changeValue,
             * ordLn.getEscmLineTotalUpdated());
             */

            POContractSummaryDAO.updateTaxAndChangeValue(true, ord, ordLn);
            /*
             * Task No. if (ordLn.getSalesOrder().getEscmTaxMethod() != null &&
             * ordLn.getSalesOrder().isEscmIstax()) { if
             * (!ordLn.getSalesOrder().getEscmTaxMethod().isPriceIncludesTax()) { JSONObject
             * taxObject = POContractSummaryTotPOChangeDAO.calculateTax( ordLn.getOrderedQuantity(),
             * ordLn.getUnitPrice(), ordLn.getEscmPoChangeType().getId(), changeValue,
             * ordLn.getEscmPoChangeFactor().getId(), ordLn.getEscmLineTaxamt(),
             * ordLn.getSalesOrder().getId()); if (taxObject.length() > 0) { if
             * (taxObject.has("lineNetAmt")) { ordLn.setLineNetAmount(new
             * BigDecimal(taxObject.getString("lineNetAmt"))); } if (taxObject.has("taxAmount")) {
             * ordLn.setEscmLineTaxamt(new BigDecimal(taxObject.getString("taxAmount"))); } if
             * (taxObject.has("calGrossPrice")) { ordLn.setEscmLineTotalUpdated( new
             * BigDecimal(taxObject.getString("calGrossPrice"))); } if
             * (taxObject.has("calUnitPrice")) { ordLn.setUnitPrice(new
             * BigDecimal(taxObject.getString("calUnitPrice"))); } }
             * ordLn.getSalesOrder().setEscmCalculateTaxlines(true); } }
             */
          } else
            ordLn.setLineNetAmount(BigDecimal.ZERO);
          OBDal.getInstance().save(ordLn);
        }
        OBDal.getInstance().flush();
      } else {
        setValue = 1;// no line
      }

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      setValue = 2;// exception
      log.error("Exception while setChangeValueToAllLines:" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      setValue = 2;
      log.error("Exception while setChangeValueToAllLines:" + e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return setValue;
  }

  /**
   * Calculate line net amt from Po Change values
   * 
   * @param poChangeType
   * @param poChangeFact
   * @param poChangeValue
   * @param lineNetAmt
   * @return BigDecimal
   */
  public static BigDecimal calculateLineUpdatedAmt(String poChangeType, String poChangeFact,
      BigDecimal poChangeValue, BigDecimal lineNetAmt) {
    BigDecimal lnPriceUpdatedAmt = BigDecimal.ZERO;
    // Get Line amount Lookup Id
    String lineAmtChangeTypeId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGTYP",
        "01");
    // Get Line Percent Lookup Id
    String linePercentChangeTypeId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGTYP",
        "02");
    try {
      OBContext.setAdminMode();
      log.debug("lineNetAmt>" + lineNetAmt);
      if (lineNetAmt.compareTo(BigDecimal.ZERO) > 0) {
        String decFactId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGFACT", "01");
        String incFactId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGFACT", "02");

        // PO Change Type - Total Amt Calc
        if (poChangeType.equals(lineAmtChangeTypeId)
            && (poChangeValue).compareTo(BigDecimal.ZERO) > 0) {
          if (poChangeFact.equals(decFactId)) {
            lnPriceUpdatedAmt = lineNetAmt.subtract((poChangeValue));
          } else if (poChangeFact.equals(incFactId)) {
            lnPriceUpdatedAmt = lineNetAmt.add((poChangeValue));
          }
        } // PO Change Type - Percentage Calc
        else if (poChangeType.equals(linePercentChangeTypeId)
            && (poChangeValue).compareTo(BigDecimal.ZERO) > 0) {

          if (poChangeFact.equals(decFactId)) {
            lnPriceUpdatedAmt = lineNetAmt.subtract(poChangeValue);
          } else if (poChangeFact.equals(incFactId)) {
            lnPriceUpdatedAmt = lineNetAmt
                .add(lineNetAmt.multiply((poChangeValue).divide(new BigDecimal("100"))));
          }
        } else {
          lnPriceUpdatedAmt = lineNetAmt;
        }
      }
      // calculate tax

      log.debug("lnPriceUpdatedAmt>" + lnPriceUpdatedAmt);
    } catch (Exception e) {
      log.error("Exception while calculateLineUpdatedAmt:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return lnPriceUpdatedAmt;
  }

  /**
   * Update line net amt and gross line amt from its parent id
   * 
   * @param orderId
   * @return
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static void updateLineTotalAmt(String orderId) {
    StringBuffer query = null;
    Query lnQuery = null;
    String allParentId = null;
    try {
      OBContext.setAdminMode();
      query = new StringBuffer();
      query.append(
          "select e.escmParentline.id from OrderLine e  where e.salesOrder.id= :orderId and e.escmIssummarylevel='N' and e.escmParentline is not null group by e.escmParentline.id ");
      lnQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      lnQuery.setParameter("orderId", orderId);

      if (lnQuery != null && lnQuery.list().size() > 0) {
        for (Iterator iterator = lnQuery.iterate(); iterator.hasNext();) {
          Object objects = iterator.next();
          allParentId = objects == null ? "" : objects.toString();
          String parentId = allParentId;
          while (parentId != null && !parentId.isEmpty()) {
            query = new StringBuffer();
            query.append(
                "SELECT coalesce(SUM(ordLn.lineNetAmount), 0) as lineprice, coalesce(SUM(ordLn.escmLineTotalUpdated), 0) as updatedprice FROM OrderLine ordLn where ordLn.escmParentline.id=:parentId");
            lnQuery = OBDal.getInstance().getSession().createQuery(query.toString());
            lnQuery.setParameter("parentId", parentId);
            List<Object> lines = lnQuery.list();
            Iterator itr = lines.iterator();
            while (itr.hasNext()) {
              Object ordln[] = (Object[]) itr.next();
              BigDecimal linePrice = (BigDecimal) ordln[0];
              BigDecimal lineUpdatePrice = (BigDecimal) ordln[1];
              String hql = "update OrderLine set updated=now(), lineNetAmount=:linePrice, escmLineTotalUpdated=:lineUpdatePrice where id =:parentId";
              Query updQuery = OBDal.getInstance().getSession().createQuery(hql);
              updQuery.setParameter("linePrice", linePrice);
              updQuery.setParameter("lineUpdatePrice", lineUpdatePrice);
              updQuery.setParameter("parentId", parentId);
              updQuery.executeUpdate();
              OrderLine nextParent = OBDal.getInstance().get(OrderLine.class, parentId);
              if (nextParent.getEscmParentline() != null)
                parentId = nextParent.getEscmParentline().getId();
              else
                parentId = null;
            }
          }
        }
      }

    } catch (Exception e) {
      log.error("Exception while updateTotalAmt:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Get sum of child line net amt
   * 
   * @param orderId
   * @return boolean
   */
  public static boolean getPOChildLinesAmt(String orderId) {
    BigDecimal linePriceUpdatedAmt = BigDecimal.ZERO;
    StringBuffer query = null;
    Query lnQuery = null;
    boolean isAmtUpdated = true;
    try {
      OBContext.setAdminMode();
      query = new StringBuffer();
      query.append(
          "SELECT coalesce(SUM(ordLn.lineNetAmount), 0) as updatedprice FROM OrderLine ordLn where ordLn.salesOrder.id=:orderId and ordLn.escmIssummarylevel='N'");
      lnQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      lnQuery.setParameter("orderId", orderId);
      log.debug(" Query : " + query.toString());
      if (lnQuery != null) {
        if (lnQuery.list().size() > 0) {
          if (lnQuery.iterate().hasNext()) {
            linePriceUpdatedAmt = new BigDecimal(lnQuery.iterate().next().toString());
          }
          isAmtUpdated = updateTotalNetAmt(linePriceUpdatedAmt, orderId);
        }
      }
    } catch (Exception e) {
      isAmtUpdated = false;
      log.error("Exception while getPOChildLinesAmt:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return isAmtUpdated;
  }

  /**
   * Update total net amount
   * 
   * @param orderId
   * @param totalUpdatedAmt
   * @return boolean
   */
  public static boolean updateTotalNetAmt(BigDecimal totalUpdatedAmt, String orderId) {
    Boolean isAmtUpdated = true;
    try {
      OBContext.setAdminMode();
      Order ord = Utility.getObject(Order.class, orderId);
      ord.setGrandTotalAmount(totalUpdatedAmt);
    } catch (Exception e) {
      isAmtUpdated = false;
      log.error("Exception while updateTotalNetAmt:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return isAmtUpdated;
  }

  /**
   * Set value to child lines from its parent id
   * 
   * @param parentId
   *          in list
   * @param ESCMDefLookupsTypeLn
   *          poChangeType, poChangeFact
   * @param lineChangeValue
   * @return
   */
  @SuppressWarnings("rawtypes")
  public static int setValuesToChildLines(List<String> parentID, ESCMDefLookupsTypeLn poChangeType,
      ESCMDefLookupsTypeLn poChangeFact, BigDecimal lineChangeValue, String lineChangeTypeId) {
    StringBuffer query = null;
    Query lnQuery = null;
    int valueSet = 0;
    List<String> parentId = parentID;
    BigDecimal lnPriceUpdatedAmt = BigDecimal.ZERO;
    // Get total Amount Lookup Id
    String totalAmtChangeTypeId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("TPOCHGTYP",
        "01");
    // Get total Percent Lookup Id
    String totalPercentChangeTypeId = POContractSummaryTotPOChangeDAO
        .getPOChangeLookUpId("TPOCHGTYP", "02");
    String decFactId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGFACT", "01");
    log.debug("lineChangeValue" + lineChangeValue);
    try {
      OBContext.setAdminMode();
      // parentId = parentId == null ? null : parentId.replaceFirst(",", "");
      while (parentId != null && !parentId.isEmpty()) {
        query = new StringBuffer();
        query.append(
            "SELECT ordLn.id FROM OrderLine ordLn where ordLn.escmParentline.id in (:parentId)");
        lnQuery = OBDal.getInstance().getSession().createQuery(query.toString());
        lnQuery.setParameterList("parentId", parentId);
        if (lnQuery != null && lnQuery.list().size() > 0) {
          parentId = new ArrayList<String>();
          for (Iterator iterator = lnQuery.iterate(); iterator.hasNext();) {
            Object objects = iterator.next();
            String chldLnId = objects == null ? "" : objects.toString();
            OrderLine orderLine = Utility.getObject(OrderLine.class, chldLnId);

            if (orderLine.isEscmIssummarylevel()) {
              parentId.add(orderLine.getId());
            } else {
              BigDecimal changeValue = BigDecimal.ZERO;
              if (poChangeType.getId().equals(totalAmtChangeTypeId)) {
                changeValue = lineChangeValue.multiply(orderLine.getEscmLineTotalUpdated());
              } else if (poChangeType.getId().equals(totalPercentChangeTypeId)) {
                changeValue = lineChangeValue;
              }
              log.debug("changeValue>" + changeValue);
              if (poChangeFact.getId().equals(decFactId)) {
                if (totalAmtChangeTypeId.equals(poChangeType.getId())) {
                  if (orderLine.getEscmLineTotalUpdated().compareTo(changeValue) < 0) {
                    valueSet = 5;// Validation Error
                    break;
                  }
                }
                // else {
                // if (new BigDecimal("100").compareTo(changeValue) == 0) {
                // valueSet = 5;// Validation Error
                // break;
                // }
                // }
              }
              if (orderLine.getEscmLineTotalUpdated().compareTo(BigDecimal.ZERO) > 0) {
                ESCMDefLookupsTypeLn lookup = Utility.getObject(ESCMDefLookupsTypeLn.class,
                    lineChangeTypeId);
                orderLine.setEscmPoChangeType(lookup);
                orderLine.setEscmPoChangeFactor(poChangeFact);
                orderLine.setEscmPoChangeValue(changeValue);

                POContractSummaryDAO.updateTaxAndChangeValue(true, orderLine.getSalesOrder(),
                    orderLine);
                /*
                 * Task No: if (objOrderLine.getSalesOrder().getEscmTaxMethod() != null &&
                 * objOrderLine.getSalesOrder().getEscmTaxMethod().isPriceIncludesTax()) {
                 */
                /*
                 * lnPriceUpdatedAmt = calculateLineUpdatedAmt(lineChangeTypeId,
                 * poChangeFact.getId(), changeValue, orderLine.getEscmLineTotalUpdated());
                 * orderLine.setLineNetAmount(lnPriceUpdatedAmt); if
                 * (orderLine.getSalesOrder().getEscmTaxMethod() != null &&
                 * orderLine.getSalesOrder().isEscmIstax()) { JSONObject taxObject =
                 * POContractSummaryTotPOChangeDAO.calculateTax( orderLine.getOrderedQuantity(),
                 * orderLine.getUnitPrice(), orderLine.getEscmPoChangeType().getId(), changeValue,
                 * orderLine.getEscmPoChangeFactor().getId(), orderLine.getEscmLineTaxamt(),
                 * orderLine.getSalesOrder().getId()); if (taxObject.length() > 0) { if
                 * (taxObject.has("lineNetAmt")) { orderLine.setLineNetAmount(new
                 * BigDecimal(taxObject.getString("lineNetAmt"))); } if (taxObject.has("taxAmount"))
                 * { orderLine.setEscmLineTaxamt(new BigDecimal(taxObject.getString("taxAmount")));
                 * } if (taxObject.has("calGrossPrice")) { orderLine.setEscmLineTotalUpdated( new
                 * BigDecimal(taxObject.getString("calGrossPrice"))); } if
                 * (taxObject.has("calUnitPrice")) { orderLine.setUnitPrice(new
                 * BigDecimal(taxObject.getString("calUnitPrice"))); } }
                 * orderLine.getSalesOrder().setEscmCalculateTaxlines(true); }
                 */
              } else
                orderLine.setLineNetAmount(BigDecimal.ZERO);
              OBDal.getInstance().save(orderLine);
            }
          }
        }
      }
    } catch (Exception e) {
      valueSet = 2;
      log.error("Exception while setValuesToChildLines:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return valueSet;
  }

  /**
   * Check apply change can be applied for the current role
   * 
   * @param Order
   * @param roleId
   * @return int
   */
  public static int checkApplyChangeAllowed(Order ord, String roleId, String userId) {
    int isAllowed = 1;// 0- Allowed, 1-Not Allowed
    try {
      OBContext.setAdminMode();
      if (ord.isEfinEncumbered()) {
        isAllowed = 1;
      } else if (ord.getEscmAppstatus().equals("ESCM_IP")) {// check for next role with cur role
        Query query = OBDal.getInstance().getSession()
            .createSQLQuery("SELECT cast('Y' as varchar) FROM C_Order "
                + " ord WHERE EXISTS (SELECT 1 FROM C_Order "
                + " Ord1 LEFT JOIN EUT_NEXT_ROLE_LINE RL ON RL.EUT_NEXT_ROLE_ID = Ord1.EM_EUT_NEXT_ROLE_ID "
                + " WHERE RL.AD_ROLE_ID=:roleId  AND Ord1.C_Order_ID=:orderId) "
                + "AND ord.C_Order_ID=:orderId");

        query.setParameter("roleId", roleId);
        query.setParameter("orderId", ord.getId());

        if (query.list() != null && query.list().size() > 0) {
          String allow = ((String) (Object) query.list().get(0));
          if (allow.equals("Y")) {
            isAllowed = 0;
          } else
            isAllowed = 1;
        } else
          isAllowed = 1;
      } else if (ord.getEscmAppstatus().equals("DR") || ord.getEscmAppstatus().equals("ESCM_REJ")
          || ord.getEscmAppstatus().equals("ESCM_RA")) {
        isAllowed = 0;
      }
    } catch (Exception e) {
      isAllowed = 0;
      log.error("Exception while checkApplyChangeAllowed:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return isAllowed;
  }

  public static JSONObject calculateTax(BigDecimal qty, BigDecimal unitPrice, String changeType,
      BigDecimal changeValue, String changeFactor, BigDecimal taxAmt, String orderId) {
    BigDecimal lineNetAmount = BigDecimal.ZERO;
    BigDecimal taxAmount = BigDecimal.ZERO;
    BigDecimal grossPrice = BigDecimal.ZERO;
    BigDecimal GrossAmtWithChgFact = BigDecimal.ZERO;
    BigDecimal AmtWithoutTax = BigDecimal.ZERO;
    Order order = null;
    JSONObject result = new JSONObject();
    BigDecimal taxRate = BigDecimal.ZERO;
    BigDecimal calUnitPrice = BigDecimal.ZERO;
    BigDecimal calGrossPrice = BigDecimal.ZERO;
    Boolean isInclusiveTax = false;
    BigDecimal changeValuediff = BigDecimal.ZERO;
    try {
      OBContext.setAdminMode();

      // final UIDefinitionController.FormatDefinition formatDef =
      // UIDefinitionController.getInstance()
      // .getFormatDefinition("euro", "Relation");
      // DecimalFormat decimal = new DecimalFormat(formatDef.getFormat());
      Integer roundoffConst = 2;
      result.put("lineNetAmt", lineNetAmount);
      result.put("taxAmount", taxAmount);

      /** check tax is inclusive or not **/
      if (orderId != null) {
        order = OBDal.getInstance().get(Order.class, orderId);
        roundoffConst = order.getClient().getCurrency().getStandardPrecision().intValue();
        if (order.isEscmIstax() != null && order.getEscmTaxMethod() != null) {
          taxRate = new BigDecimal(order.getEscmTaxMethod().getTaxpercent());
          if (order.getEscmTaxMethod().isPriceIncludesTax()) {
            isInclusiveTax = true;
          }
        }
      }

      grossPrice = qty.multiply(unitPrice);

      /** inclusive tax then add tax amt in gross price **/
      if (isInclusiveTax) {
        grossPrice = grossPrice.add(taxAmt.setScale(roundoffConst, RoundingMode.HALF_UP));
      }

      /** calculate the change factor value **/
      if (changeValue.compareTo(BigDecimal.ZERO) != 0 && changeType != null
          && changeFactor != null) {

        GrossAmtWithChgFact = POContractSummaryTotPOChangeDAO.calculateLineUpdatedAmt(changeType,
            changeFactor, changeValue, grossPrice);
        changeValuediff = GrossAmtWithChgFact.subtract(grossPrice);
      } else {
        GrossAmtWithChgFact = grossPrice;
      }

      /** calculate the line net amount and tax **/
      if (orderId != null) {
        /** inclusive **/
        if (isInclusiveTax) {
          AmtWithoutTax = GrossAmtWithChgFact.divide(
              BigDecimal.ONE
                  .add(taxRate.divide(new BigDecimal(100), roundoffConst, RoundingMode.HALF_UP)),
              roundoffConst, RoundingMode.HALF_UP);
          taxAmount = GrossAmtWithChgFact.subtract(AmtWithoutTax);
          lineNetAmount = GrossAmtWithChgFact;
          calGrossPrice = AmtWithoutTax.subtract(changeValuediff);
          calUnitPrice = calGrossPrice.divide(qty, roundoffConst, RoundingMode.HALF_UP);
          result.put("calGrossPrice", calGrossPrice);
          result.put("calUnitPrice", calUnitPrice);
        }
        /** exclusive **/
        else {
          if (taxRate.compareTo(BigDecimal.ZERO) != 0) {
            taxAmount = GrossAmtWithChgFact
                .multiply(taxRate.divide(new BigDecimal(100), roundoffConst, RoundingMode.HALF_UP))
                .setScale(roundoffConst, RoundingMode.HALF_UP);
          }
          lineNetAmount = GrossAmtWithChgFact.add(taxAmount);
        }

        result.put("lineNetAmt", lineNetAmount);
        result.put("taxAmount", taxAmount);
      } else {
        result.put("lineNetAmt", lineNetAmount);
        result.put("taxAmount", taxAmount);
      }

    } catch (Exception e) {
      log.error("Exception while calculateTax:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  public static boolean getChildForSelectedParent(String orderLineId, AccountingCombination com) {
    try {
      OBContext.setAdminMode();
      Query qry = null;
      String sql = null;

      sql = " select * from  escm_getpocontractline(?)";
      qry = OBDal.getInstance().getSession().createSQLQuery(sql);
      qry.setParameter(0, orderLineId);
      // ps.setString(2, invoice.getId());
      if (qry != null && qry.list().size() > 0) {
        String obj = qry.list().get(0).toString();
        String[] arr = obj.split(",");

        for (int i = 0; i < arr.length; i++) {
          OrderLine lineObj = OBDal.getInstance().get(OrderLine.class,
              arr[i].toString().replace("'", ""));
          if (!lineObj.isEscmIssummarylevel()) {
            lineObj.setEFINUniqueCode(com);
            lineObj.setEFINUniqueCodeName(com.getEfinUniquecodename());
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception while calculateTax:" + e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
    return true;
  }

  /**
   * Check apply change can be applied for the current role
   * 
   * @param Order
   * @return throws exception
   */
  public static void isChangeValueGreater(Order order) {

    List<OrderLine> ordLnLs = null;
    String lineChangeTypeId = "";
    Boolean isChangeValueGreater = false;

    try {

      OBContext.setAdminMode();
      ordLnLs = getOrderChildLineList(order.getId());

      String totalAmtChangeTypeId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("TPOCHGTYP",
          "01");
      String totalPercentChangeTypeId = POContractSummaryTotPOChangeDAO
          .getPOChangeLookUpId("TPOCHGTYP", "02");

      String lineAmtChangeTypeId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGTYP",
          "01");
      String linePercentChangeTypeId = POContractSummaryTotPOChangeDAO
          .getPOChangeLookUpId("POCHGTYP", "02");

      String decFactId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGFACT", "01");

      BigDecimal grossAmt = order.getEscmTotPoUpdatedAmt();

      if (order.getEscmTotPoChangeFactor() != null
          && order.getEscmTotPoChangeFactor().getId().equals(decFactId)) {

        if (ordLnLs != null && ordLnLs.size() > 0) {

          BigDecimal lineChangeValue = BigDecimal.ZERO;
          BigDecimal totalRemAmt = totalRemainingAmt(order);

          if (order.getEscmTotPoChangeType().getId().equals(totalPercentChangeTypeId)) {

            lineChangeValue = order.getEscmTotPoChangeValue();
            lineChangeTypeId = linePercentChangeTypeId;

          } else if (order.getEscmTotPoChangeType().getId().equals(totalAmtChangeTypeId)) {

            if (order.getEscmTotPoUpdatedAmt().compareTo(BigDecimal.ZERO) > 0)
              lineChangeValue = (order.getEscmTotPoChangeValue().divide(totalRemAmt, 20,
                  BigDecimal.ROUND_DOWN));
            lineChangeTypeId = lineAmtChangeTypeId;
          }

          for (OrderLine ordLn : ordLnLs) {

            BigDecimal changeValue = BigDecimal.ZERO;
            BigDecimal remAmt = remainingAmtLines(ordLn);

            if (order.getEscmTotPoChangeType().getId().equals(totalAmtChangeTypeId)) {

              changeValue = lineChangeValue.multiply(remAmt);
              changeValue = changeValue.setScale(2, BigDecimal.ROUND_HALF_UP);

            } else if (order.getEscmTotPoChangeType().getId().equals(totalPercentChangeTypeId)) {
              changeValue = lineChangeValue;
            }

            changeValue = changeValue.setScale(2, BigDecimal.ROUND_DOWN);

            if (lineChangeTypeId.equals(linePercentChangeTypeId)
                && (changeValue).compareTo(BigDecimal.ZERO) > 0) {

              changeValue = remAmt.multiply((changeValue).divide(new BigDecimal("100")));
            }

            if (remAmt.compareTo(changeValue) < 0) {
              isChangeValueGreater = true;
            }
          }

          if (isChangeValueGreater) {

            if (totalRemAmt.scale() > grossAmt.scale()) {
              totalRemAmt = totalRemAmt.setScale(grossAmt.scale(), RoundingMode.HALF_UP);
            } else {
              grossAmt = grossAmt.setScale(totalRemAmt.scale(), RoundingMode.HALF_UP);
            }

            if (totalRemAmt.compareTo(grossAmt) == 0) {
              throw new OBException(OBMessageUtils.messageBD("ESCM_POChangeValCantBeApplied"));
            } else {
              String message = OBMessageUtils.messageBD("Escm_PoRemAmtDelivered");
              message = message.replace("%", totalRemAmt.toString());
              throw new OBException(message);
            }
          }
        }
      }
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception while isChangeValueGreater:" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception while isChangeValueGreater:" + e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Get total amount remaining for particular order
   * 
   * @param Order
   * @return totalRemAmt
   */
  public static BigDecimal totalRemainingAmt(Order order) {

    BigDecimal totalRemAmt = BigDecimal.ZERO;

    for (OrderLine ordLn : order.getOrderLineList()) {

      if (!ordLn.isEscmIssummarylevel()) {

        BigDecimal remAmt = BigDecimal.ZERO;

        remAmt = remainingAmtLines(ordLn);

        totalRemAmt = totalRemAmt.add(remAmt);
      }
    }
    return totalRemAmt;
  }

  /**
   * Get remaining amount for particular orderline
   * 
   * @param Orderline
   * @return totalRemAmt
   */
  public static BigDecimal remainingAmtLines(OrderLine ordLn) {

    BigDecimal lineAmt = ordLn.getEscmLineTotalUpdated();
    BigDecimal remAmt = BigDecimal.ZERO;
    BigDecimal unitPrice = ordLn.getUnitPrice();
    BigDecimal taxPercent = BigDecimal.ZERO;
    Order ord = null;
    int roundoffConst = 2;
    Boolean isTax = false;
    try {

      OBContext.setAdminMode();
      ord = ordLn.getSalesOrder();
      if (!ordLn.isEscmIssummarylevel()) {
        if (ord.getClient().getCurrency() != null
            && ord.getClient().getCurrency().getStandardPrecision() != null)
          roundoffConst = ord.getClient().getCurrency().getStandardPrecision().intValue();

        if (ord.isEscmIstax() && ord.getEscmTaxMethod() != null) {
          isTax = true;
          taxPercent = new BigDecimal(ord.getEscmTaxMethod().getTaxpercent());
          if (ord.getEscmTaxMethod().isPriceIncludesTax()) {
            if (ordLn.getEscmInitialUnitprice().compareTo(BigDecimal.ZERO) > 0) {
              unitPrice = ordLn.getEscmInitialUnitprice();
            }
          }
        }

        if (ordLn.getSalesOrder().getEscmReceivetype() != null
            && "AMT".equals(ordLn.getSalesOrder().getEscmReceivetype())) {

          BigDecimal poReceiptAmt = ordLn.getEscmAmtporec() != null ? ordLn.getEscmAmtporec()
              : BigDecimal.ZERO;
          BigDecimal amtReturned = ordLn.getEscmAmtreturned() != null ? ordLn.getEscmAmtreturned()
              : BigDecimal.ZERO;
          BigDecimal amtCancelled = ordLn.getEscmAmtcanceled() != null ? ordLn.getEscmAmtcanceled()
              : BigDecimal.ZERO;
          BigDecimal legacyDelAmt = ordLn.getEscmLegacyAmtDelivered() != null
              ? ordLn.getEscmLegacyAmtDelivered()
              : BigDecimal.ZERO;

          BigDecimal amtDelivered = poReceiptAmt.subtract(amtReturned);
          BigDecimal receiveAmt = amtDelivered.add(legacyDelAmt);
          // remAmt = lineAmt.subtract(amtDelivered).subtract(amtCancelled).subtract(legacyDelAmt);
          if (isTax) {
            receiveAmt = (receiveAmt.divide(
                BigDecimal.ONE.add(
                    taxPercent.divide(new BigDecimal(100), roundoffConst, RoundingMode.HALF_UP)),
                roundoffConst, RoundingMode.HALF_UP));
          }
          remAmt = lineAmt.subtract(receiveAmt);

        } else if (ordLn.getSalesOrder().getEscmReceivetype() != null
            && "QTY".equals(ordLn.getSalesOrder().getEscmReceivetype())) {

          BigDecimal qtyOrdered = ordLn.getOrderedQuantity() != null ? ordLn.getOrderedQuantity()
              : BigDecimal.ZERO;
          BigDecimal poReceiptQty = ordLn.getEscmQtyporec() != null ? ordLn.getEscmQtyporec()
              : BigDecimal.ZERO;
          BigDecimal qtyIrr = ordLn.getEscmQtyirr() != null ? ordLn.getEscmQtyirr()
              : BigDecimal.ZERO;
          BigDecimal qtyRejected = ordLn.getEscmQtyrejected() != null ? ordLn.getEscmQtyrejected()
              : BigDecimal.ZERO;
          BigDecimal qtyReturned = ordLn.getEscmQtyreturned() != null ? ordLn.getEscmQtyreturned()
              : BigDecimal.ZERO;
          BigDecimal qtyCancelled = ordLn.getEscmQtycanceled() != null ? ordLn.getEscmQtycanceled()
              : BigDecimal.ZERO;
          BigDecimal legacyDelQty = ordLn.getEfinLegacyDeliveredqty() != null
              ? ordLn.getEfinLegacyDeliveredqty()
              : BigDecimal.ZERO;

          BigDecimal qtyDelivered = poReceiptQty.subtract(qtyIrr).subtract(qtyRejected)
              .subtract(qtyReturned);

          // BigDecimal remQty = qtyOrdered.subtract(qtyDelivered).subtract(qtyCancelled)
          // .subtract(legacyDelQty);
          BigDecimal receivedQty = qtyDelivered.add(legacyDelQty);
          BigDecimal receiveAmt = receivedQty.multiply(unitPrice);
          if (isTax) {
            receiveAmt = (receiveAmt.divide(
                BigDecimal.ONE.add(
                    taxPercent.divide(new BigDecimal(100), roundoffConst, RoundingMode.HALF_UP)),
                roundoffConst, RoundingMode.HALF_UP));
          }
          remAmt = lineAmt.subtract(receiveAmt);
        }

      }
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception while remainingAmtLines:" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception while remainingAmtLines:" + e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return remAmt;
  }

  public static BigDecimal remainingAmtLinesForcalculateChangeValue(OrderLine ordLn,
      BigDecimal unitPrice, Boolean initalValue) {

    BigDecimal lineAmt = ordLn.getEscmLineTotalUpdated();
    BigDecimal remAmt = BigDecimal.ZERO;
    BigDecimal taxPercent = BigDecimal.ZERO;
    int roundoffConst = 2;
    Order ord = null;
    BigDecimal calUnitPrice = BigDecimal.ZERO;
    Boolean isTax = false;
    try {

      OBContext.setAdminMode();
      ord = ordLn.getSalesOrder();
      calUnitPrice = ordLn.getUnitPrice();
      if (!ordLn.isEscmIssummarylevel()) {
        if (ord.getClient().getCurrency() != null
            && ord.getClient().getCurrency().getStandardPrecision() != null)
          roundoffConst = ord.getClient().getCurrency().getStandardPrecision().intValue();

        if (ord.isEscmIstax() && ord.getEscmTaxMethod() != null) {
          isTax = true;
          taxPercent = new BigDecimal(ord.getEscmTaxMethod().getTaxpercent());
          if (ord.getEscmTaxMethod().isPriceIncludesTax()) {
            if (initalValue) {
              lineAmt = ordLn.getOrderedQuantity().multiply(ordLn.getEscmInitialUnitprice());
            }
            if (ordLn.getEscmInitialUnitprice().compareTo(BigDecimal.ZERO) > 0) {
              calUnitPrice = ordLn.getEscmInitialUnitprice();
            }
          }
        }

        if (ordLn.getSalesOrder().getEscmReceivetype() != null
            && "AMT".equals(ordLn.getSalesOrder().getEscmReceivetype())) {

          BigDecimal poReceiptAmt = ordLn.getEscmAmtporec() != null ? ordLn.getEscmAmtporec()
              : BigDecimal.ZERO;
          BigDecimal amtReturned = ordLn.getEscmAmtreturned() != null ? ordLn.getEscmAmtreturned()
              : BigDecimal.ZERO;
          BigDecimal amtCancelled = ordLn.getEscmAmtcanceled() != null ? ordLn.getEscmAmtcanceled()
              : BigDecimal.ZERO;
          BigDecimal legacyDelAmt = ordLn.getEscmLegacyAmtDelivered() != null
              ? ordLn.getEscmLegacyAmtDelivered()
              : BigDecimal.ZERO;

          BigDecimal amtDelivered = poReceiptAmt.subtract(amtReturned);

          BigDecimal receiveAmt = amtDelivered.add(legacyDelAmt);
          // remAmt = lineAmt.subtract(amtDelivered).subtract(amtCancelled).subtract(legacyDelAmt);
          if (isTax) {
            if (!initalValue) {
              receiveAmt = (receiveAmt.divide(
                  BigDecimal.ONE.add(
                      taxPercent.divide(new BigDecimal(100), roundoffConst, RoundingMode.HALF_UP)),
                  roundoffConst, RoundingMode.HALF_UP));
            }
          }
          remAmt = lineAmt.subtract(receiveAmt);

        } else if (ordLn.getSalesOrder().getEscmReceivetype() != null
            && "QTY".equals(ordLn.getSalesOrder().getEscmReceivetype())) {

          BigDecimal qtyOrdered = ordLn.getOrderedQuantity() != null ? ordLn.getOrderedQuantity()
              : BigDecimal.ZERO;
          BigDecimal poReceiptQty = ordLn.getEscmQtyporec() != null ? ordLn.getEscmQtyporec()
              : BigDecimal.ZERO;
          BigDecimal qtyIrr = ordLn.getEscmQtyirr() != null ? ordLn.getEscmQtyirr()
              : BigDecimal.ZERO;
          BigDecimal qtyRejected = ordLn.getEscmQtyrejected() != null ? ordLn.getEscmQtyrejected()
              : BigDecimal.ZERO;
          BigDecimal qtyReturned = ordLn.getEscmQtyreturned() != null ? ordLn.getEscmQtyreturned()
              : BigDecimal.ZERO;
          BigDecimal qtyCancelled = ordLn.getEscmQtycanceled() != null ? ordLn.getEscmQtycanceled()
              : BigDecimal.ZERO;
          BigDecimal legacyDelQty = ordLn.getEfinLegacyDeliveredqty() != null
              ? ordLn.getEfinLegacyDeliveredqty()
              : BigDecimal.ZERO;

          BigDecimal qtyDelivered = poReceiptQty.subtract(qtyIrr).subtract(qtyRejected)
              .subtract(qtyReturned);

          // BigDecimal remQty = qtyOrdered.subtract(qtyDelivered).subtract(qtyCancelled)
          // .subtract(legacyDelQty);
          BigDecimal receivedQty = qtyDelivered.add(legacyDelQty);
          BigDecimal receiveAmt = receivedQty.multiply(calUnitPrice);
          if (isTax) {
            if (!initalValue) {
              receiveAmt = (receiveAmt.divide(
                  BigDecimal.ONE.add(
                      taxPercent.divide(new BigDecimal(100), roundoffConst, RoundingMode.HALF_UP)),
                  roundoffConst, RoundingMode.HALF_UP));
            }
          }
          remAmt = lineAmt.subtract(receiveAmt);
        }
      }
    } catch (

    OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception while remainingAmtLines:" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception while remainingAmtLines:" + e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return remAmt;
  }

  /**
   * Checks whether Document Number already exists in PO
   * 
   * @param docNo
   * @return isDocNoExists
   */
  public static boolean checkDocNoExists(Long docNo) {

    boolean isDocNoExists = false;
    try {
      OBQuery<Order> orderQry = OBDal.getInstance().createQuery(Order.class,
          "as e where e.documentNo = :docNo");
      orderQry.setNamedParameter("docNo", docNo.toString());
      if (orderQry.list().size() > 0) {
        isDocNoExists = true;
      }
    } catch (Exception e) {
      log.error("Exception while checkDocNoExists in POContractSummaryTotPOChangeDAO:" + e);
      return false;
    }
    return isDocNoExists;
  }

}
