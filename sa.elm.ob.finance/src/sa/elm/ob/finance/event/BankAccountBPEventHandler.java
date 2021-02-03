
package sa.elm.ob.finance.event;

import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BankAccount;

/**
 * This Class is to handle the event[save, update] occurs in table - C_BP_BankAccount
 * 
 * Unique constraint for the iban account no
 * 
 * If Business partner is Hr Supplier, then Unique constraint should not be applied
 * 
 * @author SathishKumar.P
 * 
 */
public class BankAccountBPEventHandler extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(BankAccount.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final BankAccount bankAccount = (BankAccount) event.getTargetInstance();

    if (bankAccount != null && bankAccount.getBankFormat().equals("IBAN")
        && StringUtils.isNotEmpty(bankAccount.getIBAN())) {

      StringBuilder whereClauseBuilder = new StringBuilder();
      whereClauseBuilder.append(" as e where e.iBAN = :iban and e.id !=:id");

      OBQuery<BankAccount> bankAccountQry = OBDal.getInstance().createQuery(BankAccount.class,
          whereClauseBuilder.toString());
      bankAccountQry.setFilterOnActive(false);
      bankAccountQry.setFilterOnReadableClients(true);
      bankAccountQry.setFilterOnReadableOrganization(true);
      bankAccountQry.setNamedParameter("iban", bankAccount.getIBAN());
      bankAccountQry.setNamedParameter("id", bankAccount.getId());

      List<BankAccount> bankAccList = bankAccountQry.list();

      if (bankAccList != null && bankAccList.size() > 0) {
        if (bankAccount.getBusinessPartner().isEfinIshrsupplier()) {
          long count = bankAccList.stream()
              .filter(a -> !a.getBusinessPartner().isEfinIshrsupplier()).count();
          if (count > 0) {
            throw new OBException(OBMessageUtils.messageBD("em_efin_ibanunique"));
          }
          count = bankAccList.stream().filter(
              a -> a.getBusinessPartner().getId().equals(bankAccount.getBusinessPartner().getId()))
              .count();
          if (count > 0) {
            throw new OBException(OBMessageUtils.messageBD("em_efin_ibanunique"));
          }
        } else {
          throw new OBException(OBMessageUtils.messageBD("em_efin_ibanunique"));
        }
      }
    }
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final BankAccount bankAccount = (BankAccount) event.getTargetInstance();
    if (bankAccount != null && bankAccount.getBankFormat().equals("IBAN")
        && StringUtils.isNotEmpty(bankAccount.getIBAN())) {

      StringBuilder whereClauseBuilder = new StringBuilder();
      whereClauseBuilder.append(" as e where e.iBAN = :iban");

      OBQuery<BankAccount> bankAccountQry = OBDal.getInstance().createQuery(BankAccount.class,
          whereClauseBuilder.toString());
      bankAccountQry.setFilterOnActive(false);
      bankAccountQry.setFilterOnReadableClients(true);
      bankAccountQry.setFilterOnReadableOrganization(true);
      bankAccountQry.setNamedParameter("iban", bankAccount.getIBAN());

      List<BankAccount> bankAccList = bankAccountQry.list();

      if (bankAccList != null && bankAccList.size() > 0) {
        if (bankAccount.getBusinessPartner().isEfinIshrsupplier()) {
          long count = bankAccList.stream()
              .filter(a -> !a.getBusinessPartner().isEfinIshrsupplier()).count();
          if (count > 0) {
            throw new OBException(OBMessageUtils.messageBD("em_efin_ibanunique"));
          }
          count = bankAccList.stream().filter(
              a -> a.getBusinessPartner().getId().equals(bankAccount.getBusinessPartner().getId()))
              .count();
          if (count > 0) {
            throw new OBException(OBMessageUtils.messageBD("em_efin_ibanunique"));
          }
        } else {
          throw new OBException(OBMessageUtils.messageBD("em_efin_ibanunique"));
        }
      }
    }
  }
}
