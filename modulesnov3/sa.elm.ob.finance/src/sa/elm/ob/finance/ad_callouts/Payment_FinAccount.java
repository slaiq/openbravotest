
package sa.elm.ob.finance.ad_callouts;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;

import sa.elm.ob.finance.dao.PaymentSequenceDAO;

/**
 * Update currency, exchange rate and financial txn amount and parent sequence
 * 
 * Sathishkumar.P
 */
public class Payment_FinAccount extends SimpleCallout {

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    VariablesSecureApp vars = info.vars;
    String financialAccountId = vars.getStringParameter("inpfinFinancialAccountId");
    String inpadOrgId = vars.getStringParameter("inpadOrgId");
    String inpPaymentMethod = vars.getStringParameter("inpfinPaymentmethodId");
    String accountId = "";
    String bankId = "";

    FIN_FinancialAccount financialAccount = OBDal.getInstance().get(FIN_FinancialAccount.class,
        financialAccountId);
    if (financialAccount != null) {
      info.addResult("inpcCurrencyId", DalUtil.getId(financialAccount.getCurrency()).toString());
      info.addResult("inpfinancialaccountcurrencyid",
          DalUtil.getId(financialAccount.getCurrency()).toString());

      info.addResult("inpfinaccTxnConvertRate", BigDecimal.ONE);

      String strAmount = vars.getNumericParameter("inpamount");
      accountId = financialAccount.getEfinAccount() != null
          ? financialAccount.getEfinAccount().getId()
          : null;
      bankId = financialAccount.getEfinBank() != null ? financialAccount.getEfinBank().getId()
          : null;

      if (!strAmount.isEmpty()) {
        info.addResult("inpfinaccTxnAmount", new BigDecimal(strAmount));
      }
    } else {
      info.addResult("inpfinaccTxnConvertRate", "");
      info.addResult("inpfinaccTxnAmount", "");
    }

    String finIsReceipt = info.getStringParameter("inpisreceipt", null);
    boolean isPaymentOut = "N".equals(finIsReceipt);

    String srtPaymentMethodId = info.getStringParameter("inpfinPaymentmethodId",
        IsIDFilter.instance);
    FIN_PaymentMethod paymentMethod = OBDal.getInstance().get(FIN_PaymentMethod.class,
        srtPaymentMethodId);

    boolean isMultiCurrencyEnabled = false;
    if (paymentMethod != null && financialAccount != null) {
      for (FinAccPaymentMethod accPm : financialAccount.getFinancialMgmtFinAccPaymentMethodList()) {
        if (paymentMethod.getId().equals(accPm.getPaymentMethod().getId())) {
          if (isPaymentOut) {
            isMultiCurrencyEnabled = accPm.isPayoutAllow() && accPm.isPayoutIsMulticurrency();
          } else {
            isMultiCurrencyEnabled = accPm.isPayinAllow() && accPm.isPayinIsMulticurrency();
          }
          break;
        }
      }
    }

    String paymentSeqId = PaymentSequenceDAO.getPaymentSequenceHeader(inpPaymentMethod, bankId,
        accountId, inpadOrgId);
    info.addResult("inpismulticurrencyenabled", isMultiCurrencyEnabled ? "Y" : "N");
    info.addResult("inpemEfinPaymentSequencesId", paymentSeqId);

    // if two parent seq is associated to same user then parent seq is not listing, so commented.
    /*
     * FIN_FinancialAccount finAccount = OBDal.getInstance().get(FIN_FinancialAccount.class,
     * financialAccountId); if (finAccount != null) {
     * 
     * info.addResult("inpemEfinPaymentSequencesId", paymentSeqId);
     * 
     * if (paymentSeqId != null) {
     * 
     * Efin_payment_sequences paymentSeq = OBDal.getInstance().get(Efin_payment_sequences.class,
     * paymentSeqId);
     * 
     * User user = OBContext.getOBContext().getUser(); if (paymentSeq != null) { long parentCount =
     * 0; List<String> parentSeqList = new ArrayList<String>(); for (Efin_Parent_Sequence parentSeq
     * : paymentSeq.getEfinParentSequenceList()) { long childCount =
     * parentSeq.getEfinChildSequenceList().stream() .filter(a ->
     * a.getUserContact().getId().equals(user.getId()) &&
     * (a.getFINTo().compareTo(a.getNextSequence()) >= 0 || a.getEfinReturnSequenceList().size() >
     * 0)) .count(); if (childCount > 0) { parentSeqList.add(parentSeq.getId()); parentCount++; } }
     * 
     * if (parentCount == 1) { info.addResult("inpemEfinParentSequencesId", parentSeqList.get(0)); }
     * else { info.addResult("inpemEfinParentSequencesId", null); }
     * 
     * }
     * 
     * }
     * 
     * }
     */

  }
}
