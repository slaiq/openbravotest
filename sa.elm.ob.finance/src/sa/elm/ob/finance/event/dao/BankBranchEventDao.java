package sa.elm.ob.finance.event.dao;

import java.util.List;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBankBranch;

/**
 * 
 * This class is used to handle dao activities of BankBranchEvent.
 */
public class BankBranchEventDao {
  private static final Logger log = LoggerFactory.getLogger(BankBranchEventDao.class);

  /**
   * Check Branch_Code already exists
   * 
   * @param Bankbranch
   * @return true if exists
   */
  public static boolean checkBankCodeExists(EfinBankBranch branch) {
    try {
      OBContext.setAdminMode();
      OBQuery<EfinBankBranch> code = OBDal.getInstance().createQuery(EfinBankBranch.class,
          "branchCode = :branchCode and efinBank.id= :bankID ");
      code.setNamedParameter("branchCode", branch.getBranchCode());
      code.setNamedParameter("bankID", branch.getEfinBank().getId());
      List<EfinBankBranch>codeList = code.list();
      if (codeList != null && codeList.size() > 0) {
        return true;
      }
      return false;
    } catch (OBException e) {
      log.error("Exception while checking Branch_Code already exists:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}