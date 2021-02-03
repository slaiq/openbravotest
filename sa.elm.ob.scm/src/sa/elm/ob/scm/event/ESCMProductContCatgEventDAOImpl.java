package sa.elm.ob.scm.event;

import java.util.List;

import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.procurement.Requisition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.event.dao.ESCMProductContCatgEventDAO;

public class ESCMProductContCatgEventDAOImpl implements ESCMProductContCatgEventDAO {

  private static final Logger log = LoggerFactory.getLogger(ESCMProductContCatgEventDAOImpl.class);

  @SuppressWarnings("unchecked")
  public Boolean isCntrctCtgryUsed(String cntrctCtgryId, String productId) {
    try {
      OBContext.setAdminMode();
      List<Order> orderList = null;
      List<Requisition> prchseReqList = null;
      List<EscmProposalMgmt> propMgmtList = null;
      List<EscmBidMgmt> bidList = null;
      Query query = null;

      // If Contract Category is used in PO then we should not allow to update.
      String order = "select * from c_orderline ln"
          + " join c_order hdr on ln.c_order_id = hdr.c_order_id "
          + " where hdr.em_escm_contact_type =:contractCategoryId and ln.m_product_id =:productId ";
      query = OBDal.getInstance().getSession().createSQLQuery(order);
      query.setParameter("contractCategoryId", cntrctCtgryId);
      query.setParameter("productId", productId);
      orderList = query.list();
      if (orderList.size() > 0) {
        return true;
      }

      // If Contract Category is used in PR then we should not allow to update.
      String prchseReq = "select * from m_requisitionline ln"
          + " join m_requisition hdr on ln.m_requisition_id = hdr.m_requisition_id "
          + " where hdr.em_escm_contact_type =:contractCategoryId and ln.m_product_id =:productId ";
      query = OBDal.getInstance().getSession().createSQLQuery(prchseReq);
      query.setParameter("contractCategoryId", cntrctCtgryId);
      query.setParameter("productId", productId);
      prchseReqList = query.list();
      if (prchseReqList.size() > 0) {
        return true;
      }

      // If Contract Category is used in PropMgmt then we should not allow to update.
      String propMgmt = "select * from escm_proposalmgmt_line ln"
          + " join escm_proposalmgmt hdr on ln.escm_proposalmgmt_id = hdr.escm_proposalmgmt_id "
          + " where hdr.contract_type =:contractCategoryId and ln.m_product_id =:productId ";
      query = OBDal.getInstance().getSession().createSQLQuery(propMgmt);
      query.setParameter("contractCategoryId", cntrctCtgryId);
      query.setParameter("productId", productId);
      propMgmtList = query.list();
      if (propMgmtList.size() > 0) {
        return true;
      }

      // If Contract Category is used in BID then we should not allow to update.
      String bid = "select * from escm_bidmgmt_line ln"
          + " join escm_bidmgmt hdr on ln.escm_bidmgmt_id = hdr.escm_bidmgmt_id "
          + " where hdr.contract_type =:contractCategoryId and ln.m_product_id =:productId ";
      query = OBDal.getInstance().getSession().createSQLQuery(bid);
      query.setParameter("contractCategoryId", cntrctCtgryId);
      query.setParameter("productId", productId);
      bidList = query.list();
      if (bidList.size() > 0) {
        return true;
      }

      return false;

    } catch (Exception e) {
      log.error("Exception in isCntrctCtgryUsed :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

}
