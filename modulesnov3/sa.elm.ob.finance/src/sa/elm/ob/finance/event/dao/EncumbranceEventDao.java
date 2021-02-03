package sa.elm.ob.finance.event.dao;

import java.util.List;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.Efin_UserManager;

/**
 * 
 * @author Gowtham
 *
 */
public class EncumbranceEventDao {
  /**
   * 
   * This class is used to handle dao activities of Encumbrance.
   */
  private static final Logger log = LoggerFactory.getLogger(EncumbranceEventDao.class);

  /**
   * Get Usermanager costcenter
   * 
   * @param Bankbranch
   * @return true if exists
   */
  public static List<Efin_UserManager> getCostCenter(String user) {
    try {
      OBContext.setAdminMode();
      OBQuery<Efin_UserManager> usermanager = OBDal.getInstance()
          .createQuery(Efin_UserManager.class, "documentType='ENC' and userContact.id=:user");
      usermanager.setNamedParameter("user", user);
      return usermanager.list();
    } catch (OBException e) {
      log.error("Exception while getting cost center from user manager:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}