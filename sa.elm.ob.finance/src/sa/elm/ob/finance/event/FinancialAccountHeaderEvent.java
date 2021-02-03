/*
 * All Rights Reserved By Qualian Technologies Pvt Ltd.
 */

package sa.elm.ob.finance.event;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.event.dao.BudgetLinesDAO;

public class FinancialAccountHeaderEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(FIN_FinancialAccount.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private static final Logger LOG = LoggerFactory.getLogger(BudgetLinesDAO.class);

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      FIN_FinancialAccount financial = (FIN_FinancialAccount) event.getTargetInstance();
      List<FIN_FinaccTransaction> finTrxnList = financial.getFINFinaccTransactionList().stream()
          .filter(a -> a.getPosted().equals("Y")).collect(Collectors.toList());
      if (finTrxnList.size() > 0) {

        throw new OBException(OBMessageUtils.messageBD("ForeignKeyViolation"));
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while delete FinancialAccountTrxn: " + e);
      }
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
