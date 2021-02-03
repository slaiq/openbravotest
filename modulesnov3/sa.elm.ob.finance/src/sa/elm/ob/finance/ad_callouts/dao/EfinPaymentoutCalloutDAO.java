package sa.elm.ob.finance.ad_callouts.dao;

import java.util.List;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.BankAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Priyanka Ranjan on 29/12/2017
 */

// PaymentOut Callout DAO
public class EfinPaymentoutCalloutDAO {

  private final static Logger log = LoggerFactory.getLogger(EfinPaymentoutCalloutDAO.class);

  /**
   * 
   * @param bpartner
   * @param paymentInst
   * @return bankId
   */

  public static String getBankId(String bpartner, String paymentInst) {
    String bankId = null;
    try {
      OBContext.setAdminMode();

      OBQuery<BankAccount> bpbank = OBDal.getInstance().createQuery(BankAccount.class,
          " as e join e.efinBank as bank where e.businessPartner.id = :bPartnerID and e.bankFormat= :bankFormat ");
      bpbank.setNamedParameter("bPartnerID", bpartner);
      bpbank.setNamedParameter("bankFormat", paymentInst);
      bpbank.setMaxResult(1);
      if (bpbank != null && bpbank.list().size() == 1) {
        bankId = bpbank.list().get(0).getEfinBank().getId();
        return bankId;
      }

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in getBankId " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return bankId;

  }

  /**
   * 
   * @param bankId
   * @param bpartner
   * @param inpemEfinPayinst
   * @return SupplierBankAccount - IBAN NO./Generic Account NO.
   */
  public static String getSupplierBankAccount(String bankId, String bpartner,
      String inpemEfinPayinst) {
    try {
      OBContext.setAdminMode();

      OBQuery<BankAccount> bpbankacct = OBDal.getInstance().createQuery(BankAccount.class,
          "as e where e.efinBank.id=:bankId and e.businessPartner.id=:bpartner and e.bankFormat=:inpemEfinPayinst");
      bpbankacct.setNamedParameter("bankId", bankId);
      bpbankacct.setNamedParameter("bpartner", bpartner);
      bpbankacct.setNamedParameter("inpemEfinPayinst", inpemEfinPayinst);
      List<BankAccount> list = bpbankacct.list();
      if (list.size() > 0) {
        return list.get(0).getId();
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in getSupplierBankAccount " + e.getMessage());
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
    return null;

  }

}
