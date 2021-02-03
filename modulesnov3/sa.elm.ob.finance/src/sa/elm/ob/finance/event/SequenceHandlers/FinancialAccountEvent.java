package sa.elm.ob.finance.event.SequenceHandlers;

import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetailV;

public class FinancialAccountEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(FIN_FinaccTransaction.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      FIN_FinaccTransaction financial = (FIN_FinaccTransaction) event.getTargetInstance();
      Property documentNo = entities[0].getProperty(FIN_FinaccTransaction.PROPERTY_EFINDOCUMENTNO);

      // For financial account tranasaction we have to bring invoice document no as sequence.
      OBQuery<FIN_PaymentDetailV> detail = OBDal.getInstance().createQuery(FIN_PaymentDetailV.class,
          "payment.id='" + financial.getFinPayment().getId()
              + "' and paymentPlanInvoice is not null");
      List<FIN_PaymentDetailV> detailList = detail.list();
      if (detailList != null && detailList.size() > 0) {
        FIN_PaymentDetailV view = detailList.get(0);
        String SequenceNo = view.getInvoiceno();
        event.setCurrentState(documentNo, SequenceNo);
      }

    } catch (Exception e) {
      log.error(" Exception while Adding Transactions Lines in Financial Account: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
