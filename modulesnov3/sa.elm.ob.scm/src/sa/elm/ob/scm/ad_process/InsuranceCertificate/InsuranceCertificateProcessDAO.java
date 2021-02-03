package sa.elm.ob.scm.ad_process.InsuranceCertificate;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmICExtension;
import sa.elm.ob.scm.EscmICRelease;
import sa.elm.ob.scm.EscmInsuranceCertificate;
import sa.elm.ob.scm.ad_callouts.dao.BGWorkbenchDAO;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author qualian
 * 
 */
public class InsuranceCertificateProcessDAO {
  private static final Logger log = LoggerFactory.getLogger(InsuranceCertificateProcessDAO.class);

  /**
   * get maiximum IC extension date
   * 
   * @param documentId
   * @param extensionId
   * @return
   */
  public static String getmaximumbgextensiondate(String InsCertificateId, String extensionId) {
    String maxBGExtDate = null;
    List<EscmICExtension> icExtensionList = new ArrayList<EscmICExtension>();
    try {
      OBContext.setAdminMode();

      OBQuery<EscmICExtension> icExtension = OBDal.getInstance().createQuery(EscmICExtension.class,
          " as e where e.escmInsuranceCertificate.id=:insCertifID and e.id <>:extTD order by creationDate desc ");
      // and e.letterRef is not null and
      // e.letterReferenceDateH is not null
      icExtension.setNamedParameter("insCertifID", InsCertificateId);
      icExtension.setNamedParameter("extTD", extensionId);
      icExtension.setMaxResult(1);
      icExtensionList = icExtension.list();
      if (icExtensionList.size() > 0) {
        maxBGExtDate = UtilityDAO
            .eventConvertTohijriDate(icExtensionList.get(0).getRequestedExpiryDateH().toString());

        log.debug("maxBGExtDate:" + maxBGExtDate);
        return maxBGExtDate;
      } else
        return maxBGExtDate;

    } catch (OBException e) {
      log.error("Exception while getmaximumbgextensiondate:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static boolean checkICExtensionProcessed(EscmICExtension icExtension) {
    OBQuery<EscmICExtension> icExtensionQry = null;
    List<EscmICExtension> icExtensionList = new ArrayList<EscmICExtension>();
    try {
      OBContext.setAdminMode();
      // Task No.7635 Note No:20040
      icExtensionQry = OBDal.getInstance().createQuery(EscmICExtension.class,
          " as e where e.escmInsuranceCertificate.id=:insuranceCertificateId  "
              + " order by requestedExpiryDateH desc ");
      icExtensionQry.setNamedParameter("insuranceCertificateId",
          icExtension.getEscmInsuranceCertificate().getId());
      icExtensionQry.setMaxResult(1);
      icExtensionList = icExtensionQry.list();
      if (icExtensionList.size() > 0) {
        EscmICExtension maxRequestExtendDateObj = icExtensionList.get(0);
        if (icExtension.getLineNo() != maxRequestExtendDateObj.getLineNo()) {
          return true;
        } else {
          return false;
        }

      } else {
        return false;
      }

    } catch (OBException e) {
      log.error("Exception while getmaximumbgextensiondate:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static void updateExtensionExpiryDate(EscmICExtension icExtension) {
    OBQuery<EscmICExtension> icExtensionQry = null;
    List<EscmICExtension> icExtensionList = new ArrayList<EscmICExtension>();
    try {
      OBContext.setAdminMode();
      EscmInsuranceCertificate insuranceCertifcate = icExtension.getEscmInsuranceCertificate();
      List<EscmICExtension> totalicExtensionList = new ArrayList<EscmICExtension>();
      totalicExtensionList = insuranceCertifcate.getEscmICExtensionList();

      // Task No.7635 Note No:20040
      icExtensionQry = OBDal.getInstance().createQuery(EscmICExtension.class,
          " as e where e.escmInsuranceCertificate.id=:insuranceCertificateId and e.id <> :extensionid "
              + " order by requestedExpiryDateH desc ");
      icExtensionQry.setNamedParameter("insuranceCertificateId", insuranceCertifcate.getId());
      icExtensionQry.setNamedParameter("extensionid", icExtension.getId());
      icExtensionQry.setMaxResult(1);
      icExtensionList = icExtensionQry.list();
      if (icExtensionList.size() > 0) {
        EscmICExtension icextension = icExtensionList.get(0);
        InsuranceCertificateProcessDAO.updateICStatus(insuranceCertifcate,
            icextension.getLetterRef(), "EXT", OBContext.getOBContext().getUser().getId(),
            icextension.getId(), icextension.getRequestedExpiryDateH());
      }
      totalicExtensionList.remove(icExtension);
      if (totalicExtensionList.size() == 0) {
        insuranceCertifcate.setExtendedexpirydateh(null);
        insuranceCertifcate.setExtendedexpirydateg(null);
        insuranceCertifcate.setStatus("ACT");
      }
    } catch (OBException e) {
      log.error("Exception while updateExtensionExpiryDate:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * update header status and extended expiry date in while saving extension.
   * 
   * @param insuranceCertificate
   * @param LetterRef
   * @param status
   * @param user
   * @param LineId
   * @param currICExtDate
   */
  public static boolean updateICStatus(EscmInsuranceCertificate insuranceCertificate,
      String LetterRef, String status, String user, String LineId, Date currICExtDate) {
    try {
      BigInteger inpexprireIn = BigInteger.ZERO;
      SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
      SimpleDateFormat datemonthfromat = new SimpleDateFormat("dd-MM-yyyy");

      OBContext.setAdminMode();

      if (LineId != null) {
        if (status.equals("REL")) {
          if (LetterRef != null) {

            insuranceCertificate.setUpdated(new java.util.Date());
            insuranceCertificate.setUpdatedBy(OBDal.getInstance().get(User.class, user));
            insuranceCertificate.setStatus(status);
            OBDal.getInstance().save(insuranceCertificate);

            EscmICRelease icRelease = OBDal.getInstance().get(EscmICRelease.class, LineId);
            icRelease.setUpdated(new java.util.Date());
            icRelease.setUpdatedBy(OBDal.getInstance().get(User.class, user));
            icRelease.setRelese(true);
            OBDal.getInstance().save(icRelease);
            return true;
          } else {
            return false;
          }
        } else if (status.equals("EXT")) {

          insuranceCertificate.setUpdated(new java.util.Date());
          insuranceCertificate.setUpdatedBy(OBDal.getInstance().get(User.class, user));
          insuranceCertificate.setStatus(status);
          OBDal.getInstance().save(insuranceCertificate);

          String strtodayDate = UtilityDAO
              .eventConvertTohijriDate(dateYearFormat.format(new Date()));
          inpexprireIn = BGWorkbenchDAO.getExtendPeriodDays(strtodayDate,
              UtilityDAO.eventConvertTohijriDate(dateYearFormat.format(currICExtDate)),
              insuranceCertificate.getClient().getId(), null);

          insuranceCertificate.setExtendedexpirydateh(currICExtDate);
          insuranceCertificate.setExtendedexpirydateg(datemonthfromat.format(currICExtDate));
          insuranceCertificate.setExpireIn(Long.valueOf(inpexprireIn.toString()));
          OBDal.getInstance().save(insuranceCertificate);
          return true;
        }
      }
      // //} else {
      // return false;
      // }
      return false;
    } catch (OBException e) {
      log.error("Exception while updateICStatus:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}