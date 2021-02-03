package sa.elm.ob.scm.actionHandler.dao;

import java.util.List;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinEncControl;

/**
 * 
 * @author Gowtham
 * 
 */
// CopyUniquecodeDAO handler file
public class CopyUniqueCodeDao {
  private static final Logger log = LoggerFactory.getLogger(CopyUniqueCodeDao.class);

  /**
   * This method is used to check poencum is active or not.
   * 
   * @return
   */
  public static boolean isPoEncumEnabled() {
    List<EfinEncControl> encumList = null;
    try {
      OBContext.setAdminMode();
      OBQuery<EfinEncControl> encControl = OBDal.getInstance().createQuery(EfinEncControl.class,
          "encumbranceType = 'POE'");
      encumList = encControl.list();
      if (encumList != null && encumList.size() > 0) {
        return true;
      }
    } catch (Exception e) {
      log.error("Exception in copyuniquecodedao while cheking PO encum control is active or not: ",
          e);
      OBDal.getInstance().rollbackAndClose();
      return false;
    }
    return false;
  }

}