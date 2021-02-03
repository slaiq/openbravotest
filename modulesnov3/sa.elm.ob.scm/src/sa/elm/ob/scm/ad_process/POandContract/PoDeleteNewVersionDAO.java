package sa.elm.ob.scm.ad_process.POandContract;

import java.sql.PreparedStatement;
import java.util.List;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmProposalAttribute;

/**
 * 
 * @author Gokul 02/01/2019
 *
 */
public class PoDeleteNewVersionDAO {
  private static final Logger log = LoggerFactory.getLogger(PoDeleteNewVersionDAO.class);

  @SuppressWarnings("resource")
  public static boolean deleteNewVersion(String order) {
    PreparedStatement st = null;
    try {
      OBContext.setAdminMode();
      @SuppressWarnings("unused")
      int rs = 0;
      // usrDeptQuery = OBDal.getInstance().getSession().createSQLQuery(query);

      Order objOrder = OBDal.getInstance().get(Order.class, order);
      if (objOrder != null) {
        if (objOrder.getDocumentStatus().equals("DR") && objOrder.getEscmProposalmgmt() != null
            && objOrder.getEscmOldOrder() != null) {

          // Updating the PO reference in Proposal
          objOrder.getEscmProposalmgmt().setDocumentNo(objOrder.getEscmOldOrder());
          OBDal.getInstance().save(objOrder);

          // Updating the PO reference in PEE(Proposal Attribute)
          // Fetching the PEE irrespective of Proposal Version
          OBQuery<EscmProposalAttribute> proposalAttr = OBDal.getInstance().createQuery(
              EscmProposalAttribute.class,
              " as a  join a.escmProposalevlEvent b where b.status='CO' and a.escmProposalmgmt.proposalno= :proposalID ");
          proposalAttr.setNamedParameter("proposalID",
              objOrder.getEscmProposalmgmt().getProposalno());
          List<EscmProposalAttribute> proposalAttrList = proposalAttr.list();
          if (proposalAttrList.size() > 0) {
            EscmProposalAttribute proposalAttrObj = proposalAttrList.get(0);
            proposalAttrObj.setOrder(objOrder.getEscmOldOrder());
            OBDal.getInstance().save(proposalAttrObj);
          }
          OBDal.getInstance().flush();
        }
        objOrder = null;
      }

      st = OBDal.getInstance().getConnection().prepareStatement(
          " delete from escm_ordersource_ref where c_orderline_id in (select c_orderline_id from c_orderline where "
              + " c_order_id=?)");
      st.setString(1, order);
      rs = st.executeUpdate();

      st = OBDal.getInstance().getConnection()
          .prepareStatement(" delete from c_orderline where c_order_id=?");
      st.setString(1, order);
      rs = st.executeUpdate();

      st = OBDal.getInstance().getConnection()
          .prepareStatement(" delete from escm_purorderacthist where c_order_id=?");
      st.setString(1, order);
      rs = st.executeUpdate();

      st = OBDal.getInstance().getConnection()
          .prepareStatement(" delete from escm_poamendment where c_order_id=?");
      st.setString(1, order);
      rs = st.executeUpdate();

      st = OBDal.getInstance().getConnection()
          .prepareStatement("delete from c_file where ad_record_id=?");
      st.setString(1, order);
      rs = st.executeUpdate();

      st = OBDal.getInstance().getConnection()
          .prepareStatement("delete from escm_payment_schedule where c_order_id=?");
      st.setString(1, order);
      rs = st.executeUpdate();

      st = OBDal.getInstance().getConnection()
          .prepareStatement("delete from c_order where c_order_id=?");
      st.setString(1, order);
      rs = st.executeUpdate();

      return true;
    } catch (final Exception e) {
      log.error("Exception in PoDeleteNewVersion : ", e);
      return false;
    } finally {
      try {
        if (st != null) {
          st.close();
        }
      } catch (Exception e) {
        log.error("Exception while closing the statement in PoDeleteNewVersion ", e);
      }
      // OBContext.restorePreviousMode();
    }
  }
}
