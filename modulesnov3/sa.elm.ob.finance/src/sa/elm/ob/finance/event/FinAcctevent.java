package sa.elm.ob.finance.event;

import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

public class FinAcctevent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(FIN_FinancialAccount.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onUpdate(@Observes EntityUpdateEvent event) {

    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();

      // if active field is checked inactive field is mandatory validation
      FIN_FinancialAccount finacct = (FIN_FinancialAccount) event.getTargetInstance();
      String genacctno = finacct.getGenericAccountNo();
      Entity finacctprop = ModelProvider.getInstance().getEntity(FIN_FinancialAccount.ENTITY_NAME);
      Property inactdate = finacctprop.getProperty(FIN_FinancialAccount.PROPERTY_EFININACTIVEDATE);
      Property inpacctno = finacctprop.getProperty(FIN_FinancialAccount.PROPERTY_ACCOUNTNO);
      Property type = finacctprop.getProperty(FIN_FinancialAccount.PROPERTY_TYPE);
      Property bank = finacctprop.getProperty(FIN_FinancialAccount.PROPERTY_EFINBANK);
      Property accountNo = finacctprop.getProperty(FIN_FinancialAccount.PROPERTY_EFINACCOUNT);
      if (finacct.getBankFormat() != null || finacct.getBankFormat() != "")
        event.setCurrentState(inpacctno, genacctno);

      if (!finacct.isActive()) {
        if (finacct.getEFINInactiveDate() == null) {
          log.debug("exception on creating financial account inactive date is null");
          throw new OBException(OBMessageUtils.messageBD("Efin_inactivedateisempty"));
        }
      } else {

        event.setCurrentState(inactdate, null);
      }

      // if (!event.getPreviousState(type).equals(event.getCurrentState(type))
      // || event.getCurrentState(bank) == null) {
      // }

      // Bank is mandatory if type is 'bank'
      if (finacct.getType().equals("B")
          && (finacct.getEfinBank() == null || finacct.getEfinAccount() == null)) {
        throw new OBException(OBMessageUtils.messageBD("EFIN_Bank_Mandatory"));
      }

      List<FIN_Payment> paymentOutList = null;
      OBQuery<FIN_Payment> paymentOut = OBDal.getInstance().createQuery(FIN_Payment.class,
          "as e where e.account.id=:FIN_Financial_Account_ID and e.status<>'RPAP'");
      paymentOut.setNamedParameter("FIN_Financial_Account_ID", finacct.getId());
      paymentOutList = paymentOut.list();
      // Bank and Account should not allow to change when financial account is processed in payment
      // out
      if ((!event.getPreviousState(type).equals(event.getCurrentState(type))
          || finacct.getEfinBank() != null || finacct.getEfinAccount() != null)
          && event.getPreviousState(bank) != null && event.getPreviousState(accountNo) != null) {
        if (event.getPreviousState(type).equals("B")) {
          if ((!event.getPreviousState(bank).equals(event.getCurrentState(bank)))
              || (!event.getPreviousState(accountNo).equals(event.getCurrentState(accountNo)))) {
            if (paymentOutList.size() > 0) {
              throw new OBException(OBMessageUtils.messageBD("EFIN_Fin_Acc_processed"));
            }
          }
        } else {
          if (paymentOutList.size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("EFIN_Fin_Acc_processed"));
          }
        }
      }

      // to null bank and account if Type is cash
      if (!finacct.getType().equals("B")) {
        event.setCurrentState(bank, null);
        event.setCurrentState(accountNo, null);
      }

    } catch (OBException e) {
      log.error(" Exception while updating record in Financial account: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onSave(@Observes EntityNewEvent event) {

    if (!isValidEvent(event)) {
      return;
    }
    try {

      OBContext.setAdminMode();
      // if active field is checked inactive field is mandatory validation
      FIN_FinancialAccount finacct = (FIN_FinancialAccount) event.getTargetInstance();
      Entity finacctprop = ModelProvider.getInstance().getEntity(FIN_FinancialAccount.ENTITY_NAME);
      Property bank = finacctprop.getProperty(FIN_FinancialAccount.PROPERTY_EFINBANK);
      Property accountNo = finacctprop.getProperty(FIN_FinancialAccount.PROPERTY_EFINACCOUNT);
      if (!finacct.isActive()) {
        if (finacct.getEFINInactiveDate() == null) {
          log.debug("exception on creating financial account inactive date is null");
          throw new OBException(OBMessageUtils.messageBD("Efin_inactivedateisempty"));
        }
      } else {
        Property inactdate = finacctprop
            .getProperty(FIN_FinancialAccount.PROPERTY_EFININACTIVEDATE);
        event.setCurrentState(inactdate, null);

      }

      // Bank and account is mandatory if type is 'bank'
      if (finacct.getType().equals("B")) {
        if (finacct.getEfinBank() == null || finacct.getEfinAccount() == null) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_Bank_Mandatory"));
        }
      } else {
        event.setCurrentState(bank, null);
        event.setCurrentState(accountNo, null);
      }
    }

    catch (OBException e) {
      log.error(" Exception while creating record in Financial account: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
