package sa.elm.ob.finance.ad_callouts;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.service.db.DalConnectionProvider;

import sa.elm.ob.finance.dao.PaymentSequenceDAO;

public class EfinSEPaymentBPartnerCallout extends SimpleCallout {

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    VariablesSecureApp vars = info.vars;
    String strcBpartnerId = vars.getStringParameter("inpcBpartnerId");
    String strisreceipt = vars.getStringParameter("inpisreceipt");
    String paymentBeneficiary = vars.getStringParameter("inpemEfinBpartnerId");
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    BusinessPartner bpartner = OBDal.getInstance().get(BusinessPartner.class, strcBpartnerId);
    boolean isReceipt = "Y".equals(strisreceipt);
    String inpadOrgId = vars.getStringParameter("inpadOrgId");
    String financialAccountId = vars.getStringParameter("inpfinFinancialAccountId");
    String inpPaymentMethod = vars.getStringParameter("inpfinPaymentmethodId");
    String inpInvoiceId = vars.getStringParameter("inpemEfinInvoiceId");

    FIN_FinancialAccount finAccount = OBDal.getInstance().get(FIN_FinancialAccount.class,
        financialAccountId);
    if (finAccount != null) {

      String finBankId = finAccount.getEfinBank() != null ? finAccount.getEfinBank().getId() : null;
      String finAccountId = finAccount.getEfinAccount() != null
          ? finAccount.getEfinAccount().getId()
          : null;

      String paymentSeqId = PaymentSequenceDAO.getPaymentSequenceHeader(inpPaymentMethod, finBankId,
          finAccountId, inpadOrgId);

      info.addResult("inpemEfinPaymentSequencesId", paymentSeqId);
      /*
       * if (paymentSeqId != null) {
       * 
       * Efin_payment_sequences paymentSeq = OBDal.getInstance().get(Efin_payment_sequences.class,
       * paymentSeqId);
       * 
       * User user = OBContext.getOBContext().getUser(); if (paymentSeq != null) { long parentCount
       * = 0; List<String> parentSeqList = new ArrayList<String>(); for (Efin_Parent_Sequence
       * parentSeq : paymentSeq.getEfinParentSequenceList()) { long childCount =
       * parentSeq.getEfinChildSequenceList().stream() .filter(a ->
       * a.getUserContact().getId().equals(user.getId()) &&
       * (a.getFINTo().compareTo(a.getNextSequence()) >= 0 || a.getEfinReturnSequenceList().size() >
       * 0)) .count(); if (childCount > 0) { parentSeqList.add(parentSeq.getId()); parentCount++; }
       * }
       * 
       * if (parentCount == 1) { info.addResult("inpemEfinParentSequencesId", parentSeqList.get(0));
       * } else { info.addResult("inpemEfinParentSequencesId", null); }
       * 
       * }
       * 
       * }
       */

    }

    // Get the Payment Method and the Financial Acoount
    FIN_PaymentMethod paymentMethod;
    FIN_FinancialAccount financialAccount;

    if (bpartner != null) {
      if (isReceipt) {
        paymentMethod = bpartner.getPaymentMethod();
        financialAccount = bpartner.getAccount();
      } else {
        paymentMethod = bpartner.getPOPaymentMethod();
        financialAccount = bpartner.getPOFinancialAccount();
      }

      if (paymentMethod != null && financialAccount != null) {
        final OBCriteria<FinAccPaymentMethod> apmCriteria = OBDal.getInstance()
            .createCriteria(FinAccPaymentMethod.class);
        apmCriteria.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, paymentMethod));
        apmCriteria.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, financialAccount));
        apmCriteria.setFilterOnActive(false);
        FinAccPaymentMethod accPaymentMethod = (FinAccPaymentMethod) apmCriteria.uniqueResult();
        if (accPaymentMethod != null) {
          if (financialAccount.isActive() && accPaymentMethod.isActive()) {
            info.addResult("inpfinPaymentmethodId", paymentMethod.getId());
            info.addResult("inpfinFinancialAccountId", financialAccount.getId());
          } else if (!financialAccount.isActive() && !accPaymentMethod.isActive()) {
            info.addResult("WARNING",
                String.format(
                    Utility.messageBD(new DalConnectionProvider(), "finnac_paymet_inact",
                        vars.getLanguage()),
                    financialAccount.getIdentifier(), paymentMethod.getIdentifier()));
          } else if (!financialAccount.isActive()) {
            info.addResult("WARNING", String.format(
                Utility.messageBD(new DalConnectionProvider(), "finnac_inact", vars.getLanguage()),
                financialAccount.getIdentifier()));
          } else if (!accPaymentMethod.isActive()) {
            info.addResult("WARNING",
                String.format(
                    Utility.messageBD(new DalConnectionProvider(), "paymet_inact",
                        vars.getLanguage()),
                    paymentMethod.getIdentifier(), financialAccount.getIdentifier()));
          }
        } else {
          log4j.info("No default info for the selected business partner");
        }

      } else {
        log4j.info("No default info for the selected business partner");
      }
      if ((!strcBpartnerId.equals(""))
          && (FIN_Utility.isBlockedBusinessPartner(strcBpartnerId, "Y".equals(strisreceipt), 4))) {
        // If the Business Partner is blocked for this document, show an information message.
        info.addResult("MESSAGE", OBMessageUtils.messageBD("ThebusinessPartner") + " "
            + bpartner.getIdentifier() + " " + OBMessageUtils.messageBD("BusinessPartnerBlocked"));
      }

      // while changing Paying To set Supplier Bank and Supplier Bank Account if Payment Beneficiary
      // is null
      if (inpLastFieldChanged.equals("inpcBpartnerId")) {
        if (paymentBeneficiary.equals("")) {
          if (StringUtils.isNotEmpty(inpInvoiceId)) {
            Invoice inv = OBDal.getInstance().get(Invoice.class, inpInvoiceId);

            if (inv.getEfinIban() != null && inv.getEfinIban().getEfinBank() != null) {
              info.addResult("inpemEfinBankId",
                  inv.getEfinIban().getId() == null ? "" : inv.getEfinIban().getEfinBank().getId());

              info.addResult("inpemEfinSupbankacct",
                  inv.getEfinIban().getId() == null ? "" : inv.getEfinIban().getId());
            }

          }

          // bankId = EfinPaymentoutCalloutDAO.getBankId(inpcBpartnerId, inpemEfinPayinst);
          // info.addResult("inpemEfinBankId", bankId);
          //
          // SupplierBankAccount = EfinPaymentoutCalloutDAO.getSupplierBankAccount(bankId,
          // inpcBpartnerId, inpemEfinPayinst);
          // if (SupplierBankAccount != null) {
          // info.addResult("inpemEfinSupbankacct", SupplierBankAccount);
          // } else {
          // info.addResult("inpemEfinSupbankacct", "");
          // }
        }

      }

    }
  }
}
