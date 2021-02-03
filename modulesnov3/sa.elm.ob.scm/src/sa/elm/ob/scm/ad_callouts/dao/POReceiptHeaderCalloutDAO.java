package sa.elm.ob.scm.ad_callouts.dao;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Divya
 * 
 *         This file handle the dao of PO Receipt Header Callout
 */

public class POReceiptHeaderCalloutDAO {
  private static final Logger log = LoggerFactory.getLogger(POReceiptHeaderCalloutDAO.class);

  /**
   * Get Manager(userId) for selected receiver or sender
   * 
   * @param deptCode
   * @return DeptID
   */
  public static User getUserManagerId(User user) {
    User userId = null;
    List<User> userlist = new ArrayList<User>();
    try {

      OBContext.setAdminMode();
      if (user.getBusinessPartner() != null && user.getBusinessPartner().getEhcmManager() != null) {

        OBQuery<User> userObj = OBDal.getInstance().createQuery(User.class,
            "  businessPartner.id in ( select e.id from BusinessPartner e where e.efinDocumentno =:docNo ) ");
        userObj.setNamedParameter("docNo", user.getBusinessPartner().getEhcmManager());
        log.debug("para:" + userObj.getWhereAndOrderBy());
        userlist = userObj.list();
        if (userlist != null && userlist.size() > 0) {
          userId = userlist.get(0);
        }
      }
      return userId;

    } catch (OBException e) {
      log.error("Exception while  getUserManagerId " + e, e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static User getUserId(String bPartnerId) {
    User user = null;
    try {
      OBContext.setAdminMode();
      if (bPartnerId != null) {
        OBQuery<User> userlist = OBDal.getInstance().createQuery(User.class,
            " businessPartner.id='" + bPartnerId + "'");
        if (userlist.list().size() > 0) {
          user = userlist.list().get(0);
        }
        return user;
      }
    } catch (OBException e) {
      log.error("Exception while  getUserId " + e, e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
    return user;
  }
}
