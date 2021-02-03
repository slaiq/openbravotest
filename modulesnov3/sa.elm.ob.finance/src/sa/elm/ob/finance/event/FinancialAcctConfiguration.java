package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;
import org.apache.log4j.Logger;
import org.openbravo.base.model.Property;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaDefault;

/**
 * @author Gopalakrishnan on 04/08/2016
 */
public class FinancialAcctConfiguration extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(FIN_FinancialAccountAccounting.ENTITY_NAME) };

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
      OBContext.setAdminMode();
      FIN_FinancialAccountAccounting finacct = (FIN_FinancialAccountAccounting) event
          .getTargetInstance();
      final Property BankGain = entities[0]
          .getProperty(FIN_FinancialAccountAccounting.PROPERTY_FINBANKREVALUATIONGAINACCT);
      Property BankLoss = entities[0]
          .getProperty(FIN_FinancialAccountAccounting.PROPERTY_FINBANKREVALUATIONLOSSACCT);
      OBQuery<AcctSchemaDefault> acctDefault = OBDal.getInstance().createQuery(
          AcctSchemaDefault.class,
          "as e where e.accountingSchema.id='" + finacct.getAccountingSchema().getId() + "'");
      if (acctDefault.list().size() > 0) {
        AcctSchemaDefault acctSchemaDefault = acctDefault.list().get(0);
        event.setCurrentState(BankGain, acctSchemaDefault.getBankRevaluationGain());
        event.setCurrentState(BankLoss, acctSchemaDefault.getBankRevaluationLoss());
      }
    }

    catch (OBException e) {
      log.error(" Exception while creating FinancialAcctConfiguration: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {

    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      FIN_FinancialAccountAccounting finacct = (FIN_FinancialAccountAccounting) event
          .getTargetInstance();
      final Property BankGain = entities[0]
          .getProperty(FIN_FinancialAccountAccounting.PROPERTY_FINBANKREVALUATIONGAINACCT);
      Property BankLoss = entities[0]
          .getProperty(FIN_FinancialAccountAccounting.PROPERTY_FINBANKREVALUATIONLOSSACCT);
      OBQuery<AcctSchemaDefault> acctDefault = OBDal.getInstance().createQuery(
          AcctSchemaDefault.class,
          "as e where e.accountingSchema.id='" + finacct.getAccountingSchema().getId() + "'");
      if (acctDefault.list().size() > 0) {
        AcctSchemaDefault acctSchemaDefault = acctDefault.list().get(0);
        event.setCurrentState(BankGain, acctSchemaDefault.getBankRevaluationGain());
        event.setCurrentState(BankLoss, acctSchemaDefault.getBankRevaluationLoss());
      }
    } catch (OBException e) {
      log.error(" Exception while updating record in FinancialAcctConfiguration: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
