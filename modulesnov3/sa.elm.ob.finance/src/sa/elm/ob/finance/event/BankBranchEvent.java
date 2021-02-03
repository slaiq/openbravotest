package sa.elm.ob.finance.event;

import java.text.ParseException;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.finance.EfinBankBranch;
import sa.elm.ob.finance.event.dao.BankBranchEventDao;
import sa.elm.ob.utility.Utils;

/**
 * 
 * @author qualian
 *
 */
public class BankBranchEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      EfinBankBranch.ENTITY_NAME) };
  private final Logger log = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {

    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      Boolean result = Boolean.FALSE;
      final EfinBankBranch branch = (EfinBankBranch) event.getTargetInstance();
      String phone = branch.getPhoneno();
      String fax = branch.getFax();

      // check branch code is duplicating
      result = BankBranchEventDao.checkBankCodeExists(branch);
      if (result) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Branch_Code_Duplicate"));
      }

      // phone number format (Numaric values,+,-)
      if (StringUtils.isNotBlank(phone) && StringUtils.isNotEmpty(phone)) {
        result = Utils.phoneNumberFormat(phone);
        if (!result) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_PhoneNumberFormat"));
        }
      }
      // fax number format (Numaric values,+,-,(,))
      if (StringUtils.isNotBlank(fax) && StringUtils.isNotEmpty(fax)) {
        result = Utils.faxNumberFormat(fax);
        if (!result) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_FaxNumberFormat"));
        }
      }

    } catch (OBException e) {
      log.error(" Exception while saving  Bank Branch: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) throws ParseException {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      boolean result = false;
      final Property fax = entities[0].getProperty(EfinBankBranch.PROPERTY_FAX);
      final Property phoneno = entities[0].getProperty(EfinBankBranch.PROPERTY_PHONENO);
      EfinBankBranch branch = (EfinBankBranch) event.getTargetInstance();
      final Property code = entities[0].getProperty(EfinBankBranch.PROPERTY_BRANCHCODE);
      String phone = branch.getPhoneno();
      String faxno = branch.getFax();

      // Check Branch_code is duplicating or not
      if (event.getPreviousState(code) != event.getCurrentState(code)) {
        result = BankBranchEventDao.checkBankCodeExists(branch);
        if (result) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Branch_Code_Duplicate"));
        }
      }
      // phone number format (Numaric values,+,-)
      if (StringUtils.isNotBlank(phone) && StringUtils.isNotEmpty(phone)) {
        if (!event.getCurrentState(phoneno).equals(event.getPreviousState(phoneno))) {
          result = Utils.phoneNumberFormat(phone);
          if (!result) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_PhoneNumberFormat"));
          }
        }
      }
      // fax number format (Numaric values,+,-,(,))
      if (StringUtils.isNotBlank(faxno) && StringUtils.isNotEmpty(faxno)) {
        if (!event.getCurrentState(fax).equals(event.getPreviousState(fax))) {
          result = Utils.faxNumberFormat(faxno);
          if (!result) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_FaxNumberFormat"));
          }
        }
      }
    } catch (OBException e) {
      log.debug("exception while Updating Bank Branch" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
