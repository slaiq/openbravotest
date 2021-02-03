package sa.elm.ob.scm.event.dao;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMAnnouSummaryMedia;
import sa.elm.ob.scm.Escmannoucements;

/**
 * 
 * This class is used to handle dao activities of AnnouncementMediaEvent.
 */
public class AnnouncementMediaEventDAO {
  private static final Logger log = LoggerFactory.getLogger(AnnouncementMediaEventDAO.class);

  /**
   * Check media name already exists
   * 
   * @param media
   * @return true if exists
   */
  public static boolean checkMediaExists(ESCMAnnouSummaryMedia media) {
    try {
      OBContext.setAdminMode();
      Escmannoucements announcement = OBDal.getInstance().get(Escmannoucements.class,
          media.getEscmAnnoucements().getId());
      for (ESCMAnnouSummaryMedia line : announcement.getESCMAnnouSummaryMediaList()) {
        if (line.getId() != media.getId()) {
          if (line.getMediacode().equals("OLA")) {
            if (line.getOnlinemedia().equals(media.getOnlinemedia())) {
              return true;
            }
          } else {
            if (line.getMediaName().equals(media.getMediaName())) {
              return true;
            }
          }
        }
      }
      return false;
    } catch (OBException e) {
      log.error("Exception while checking media already exists:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}