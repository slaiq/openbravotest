package sa.elm.ob.scm.event;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.ESCMProposalMgmtLetter;

public class ProposalMgmtLetterEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ESCMProposalMgmtLetter.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    ESCMProposalMgmtLetter letter = (ESCMProposalMgmtLetter) event.getTargetInstance();
    validateDMSFields(letter);
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
  }

  public void validateDMSFields(ESCMProposalMgmtLetter letter) {
    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    Date now = new Date();
    Date todaydate = null;
    try {
      todaydate = dateFormat.parse(dateFormat.format(now));
      String dmsId = letter.getDMSID();

      // check DMSID has special characters
      boolean isValid = false;
      Pattern p = Pattern.compile("[^A-Za-z0-9]");
      java.util.regex.Matcher m = p.matcher(dmsId);
      isValid = m.find();
      if (isValid) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_SplCharNotAllowed"));
      }
      // Past date is not allowed for DMS Date
      if (letter.getDMSDateH() != null) {
        if (dateFormat.parse(dateFormat.format(letter.getDMSDateH())).compareTo(todaydate) < 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_NoPastDate"));
        }
      }

    } catch (OBException e) {
      logger.error("exception while saving announcement", e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      logger.error("exception while saving announcement", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      logger.error("exception while saving announcement", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
  }
}