package sa.elm.ob.finance.event.SequenceHandlers;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.utility.Sequence;

public class DocumentSequence extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Sequence.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      Sequence sequence = (Sequence) event.getTargetInstance();

      if (sequence.isEfinIsgeneralseq() != null && sequence.isEfinIsgeneralseq()) {
        SQLQuery costquery = OBDal.getInstance().getSession().createSQLQuery(
            "select ad_org_id,em_efin_isgeneralseq from ad_sequence WHERE em_efin_isgeneralseq ='Y' and ad_org_id ='"
                + sequence.getOrganization().getId() + "'and ad_client_id='"
                + sequence.getClient().getId() + "'");
        if (costquery.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_isgeneralseq"));

        }
      } else if (sequence.isEfinIsacctpaymentseq() != null && sequence.isEfinIsacctpaymentseq()) {
        SQLQuery costquery = OBDal.getInstance().getSession().createSQLQuery(
            "select ad_org_id,em_efin_isacctpaymentseq from ad_sequence WHERE em_efin_isacctpaymentseq ='Y' and ad_org_id ='"
                + sequence.getOrganization().getId() + "'and ad_client_id='"
                + sequence.getClient().getId() + "'");
        if (costquery.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_isacctpaymentseq"));

        }
      } else if (sequence.isEfinIsacctnonpaymentseq() != null
          && sequence.isEfinIsacctnonpaymentseq()) {
        SQLQuery costquery = OBDal.getInstance().getSession().createSQLQuery(
            "select ad_org_id,em_efin_isacctnonpaymentseq from ad_sequence WHERE em_efin_isacctnonpaymentseq ='Y' and ad_org_id ='"
                + sequence.getOrganization().getId() + "'and ad_client_id='"
                + sequence.getClient().getId() + "'");
        if (costquery.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_isacctnonpaymentseq"));

        }
      }
    } catch (Exception e) {
      log.error("Exception in document sequence : " + e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}