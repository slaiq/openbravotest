package sa.elm.ob.scm.event.dao;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//MaterialIssueRequestEventDAO  file
public class MaterialIssueRequestEventDAO {
  private static final Logger log = LoggerFactory.getLogger(MaterialIssueRequestEventDAO.class);

  /**
   * get Stock onHand Qty for the Request Line Product
   * 
   * @param strOrgId
   * @param strMProductID
   * @return
   */
  public static BigDecimal chkNegStockOnHandQty(String strOrgId, String strMProductID) {
    String strQuery = null;
    Query query = null;
    BigDecimal stockOnhandQty = BigDecimal.ZERO;
    try {
      OBContext.setAdminMode();
      strQuery = " select coalesce(sum(qtyonhand),0) as qtyonhand,m_product_id from m_storage_detail "
          + " where ad_org_id= ?  and m_product_id= ? group by m_product_id ";
      query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      query.setParameter(0, strOrgId);
      query.setParameter(1, strMProductID);
      @SuppressWarnings("unchecked")
      List<Object> querylist = query.list();
      if (query != null && querylist.size() > 0) {
        Object[] row = (Object[]) querylist.get(0);
        stockOnhandQty = (BigDecimal) row[0];
        log.debug("stockOnhandQty:" + stockOnhandQty);
        return stockOnhandQty;
      }
      return stockOnhandQty;

    } catch (OBException e) {
      log.error("Exception while chkNegStockOnHandQty:", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}