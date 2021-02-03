package sa.elm.ob.scm.webservice.pki.dao;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;

public class PkiDao {

  public static User getUser(String userId) {
    try {
      OBContext.setAdminMode();
      return OBDal.getInstance().get(User.class, userId);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
