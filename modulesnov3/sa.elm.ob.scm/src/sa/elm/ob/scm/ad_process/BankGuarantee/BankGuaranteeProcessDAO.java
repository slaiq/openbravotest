package sa.elm.ob.scm.ad_process.BankGuarantee;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMBGConfiscation;
import sa.elm.ob.scm.ESCMBGRelease;
import sa.elm.ob.scm.Escmbankguaranteedetail;

// BankGuaranteeProcessDAO file
public class BankGuaranteeProcessDAO {
  private static final Logger log = LoggerFactory.getLogger(BankGuaranteeProcessDAO.class);

  /**
   * update BG Status
   * 
   * @param bgReleaseId
   * @param status
   * @param user
   * @return
   */

  public static void updateBGStatus(Escmbankguaranteedetail bankguarantee, String LetterRef,
      String status, String user, String LineId, Date currBgExtDate) {
    try {
      SimpleDateFormat datemonthfromat = new SimpleDateFormat("dd-MM-yyyy");

      OBContext.setAdminMode();

      if (LetterRef != null) {
        bankguarantee.setUpdated(new java.util.Date());
        bankguarantee.setUpdatedBy(OBDal.getInstance().get(User.class, user));
        bankguarantee.setBgstatus(status);
        OBDal.getInstance().save(bankguarantee);

        if (LineId != null) {
          if (status.equals("REL")) {
            ESCMBGRelease bgRelease = OBDal.getInstance().get(ESCMBGRelease.class, LineId);
            bgRelease.setUpdated(new java.util.Date());
            bgRelease.setUpdatedBy(OBDal.getInstance().get(User.class, user));
            bgRelease.setRelease(true);
            OBDal.getInstance().save(bgRelease);

          } else if (status.equals("EXT")) {
            bankguarantee.setExpirydateh(currBgExtDate);
            bankguarantee.setExpirydategre(datemonthfromat.format(currBgExtDate));
            OBDal.getInstance().save(bankguarantee);
          }

          else if (status.equals("CON")) {
            ESCMBGConfiscation bgconfiscation = OBDal.getInstance().get(ESCMBGConfiscation.class,
                LineId);
            bgconfiscation.setUpdated(new java.util.Date());
            bgconfiscation.setUpdatedBy(OBDal.getInstance().get(User.class, user));
            bgconfiscation.setConfiscate(true);
            OBDal.getInstance().save(bgconfiscation);
          }
        }
      }

    } catch (OBException e) {
      log.error("Exception while updateBGStatus:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}