package sa.elm.ob.finance.event;

import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.invoice.Invoice;

/**
 * This class is used to handle the update event for document type . Mainly when saddad flag is
 * changed
 * 
 * If updated document type is already used some where in invoice, we should not allow to change
 * 
 * @author Sathishkumar.P
 *
 */

public class DocumentTypeSadadEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(DocumentType.ENTITY_NAME) };
  private Logger log = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      DocumentType docType = (DocumentType) event.getTargetInstance();

      Property isSaddad = entities[0].getProperty(DocumentType.PROPERTY_EFINISSADDAD);

      if (event.getCurrentState(isSaddad) != event.getPreviousState(isSaddad)) {
        List<Invoice> invoiceList = docType.getInvoiceList();
        if (invoiceList.size() > 0) {
          if (invoiceList.stream().filter(a -> !a.getDocumentStatus().equals("DR")).count() > 0) {
            throw new OBException(OBMessageUtils.messageBD("Efin_doctypeCantchange"));
          }
        }
      }

    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while updating document type", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
