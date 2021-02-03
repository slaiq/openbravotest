package sa.elm.ob.finance.event;

import java.text.SimpleDateFormat;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.gl.GLBatch;

import sa.elm.ob.utility.util.UtilityDAO;

public class GLBatchEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(GLBatch.ENTITY_NAME) };
  private static final Logger log = Logger.getLogger(GLBatchEvent.class);

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  @SuppressWarnings("unchecked")
  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();

      GLBatch journalBatch = (GLBatch) event.getTargetInstance();
      Property documentNo = entities[0].getProperty(GLBatch.PROPERTY_DOCUMENTNO);
      SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
      Organization org = null;
      String CalId = null;

      org = OBDal.getInstance().get(Organization.class, journalBatch.getOrganization().getId());
      if (org.getCalendar() != null) {
        CalId = org.getCalendar().getId();
      } else {
        // get parent organization list
        String[] orgIds = null;
        SQLQuery query = OBDal.getInstance().getSession()
            .createSQLQuery("select eut_parent_org ('" + journalBatch.getOrganization().getId()
                + "','" + journalBatch.getClient().getId() + "')");
        List<String> list = query.list();
        orgIds = list.get(0).split(",");
        for (int i = 0; i < orgIds.length; i++) {
          org = OBDal.getInstance().get(Organization.class, orgIds[i].replace("'", ""));
          if (org.getCalendar() != null) {
            CalId = org.getCalendar().getId();
            break;
          }
        }

      }

      String SequenceNo = UtilityDAO.getGeneralSequence(df.format(journalBatch.getAccountingDate()),
          "GS", CalId, journalBatch.getOrganization().getId(), true);
      if (SequenceNo.equals("0")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoPaymentSequence"));
      }
      log.info(" SequenceNo :" + SequenceNo);
      event.setCurrentState(documentNo, SequenceNo);

    } catch (OBException e) {

      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
