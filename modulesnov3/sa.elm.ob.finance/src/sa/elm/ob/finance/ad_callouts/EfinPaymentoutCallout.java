package sa.elm.ob.finance.ad_callouts;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.invoice.Invoice;

import sa.elm.ob.finance.ad_callouts.dao.EfinPaymentoutCalloutDAO;

/**
 * @author Qualian
 */

public class EfinPaymentoutCallout extends SimpleCallout {

  /**
   * Callout to update the supplierbank in payment out Window
   */
  private static final long serialVersionUID = 1L;
  private static final Logger log = Logger.getLogger(EfinPaymentoutCallout.class);

  @Override
  protected void execute(final CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;

    final String paymentBeneficiary = vars.getStringParameter("inpemEfinBpartnerId");
    final String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    final String inpInvoiceId = vars.getStringParameter("inpemEfinInvoiceId");
    final String inpemEfinBankId = vars.getStringParameter("inpemEfinBankId");
    final String inpemEfinPayinst = vars.getStringParameter("inpemEfinPayinst");
    final String inpcBpartnerId = vars.getStringParameter("inpcBpartnerId");
    String bankId = null;
    String SupplierBankAccount = null;

    log.debug("payment out callout paymentBeneficiary:" + paymentBeneficiary);
    /*
     * if(inpLastFieldChanged.equals("inpemEfinBpartnerId")){ OBQuery<EfinPaymentoutBankV> payment =
     * OBDal.getInstance().createQuery(EfinPaymentoutBankV.class, "as e where e.bpartner='"+
     * paymentBeneficiary +"'"); if(payment.list().size()>0 && payment!=null ){
     * info.addResult("inpemEfinBpBankaccountId",payment.list().get(0).getId()); }else{
     * info.addResult("inpemEfinBpBankaccountId", null);
     * 
     * } }
     */

    // If user changes the invoice field, set paying to based on invoice businesspartner
    if (inpLastFieldChanged.equals("inpemEfinInvoiceId")) {
      if (StringUtils.isNotEmpty(inpInvoiceId)) {
        Invoice inv = OBDal.getInstance().get(Invoice.class, inpInvoiceId);
        info.addResult("inpcBpartnerId",
            inv.getBusinessPartner() == null ? "" : inv.getBusinessPartner().getId());

        if (!StringUtils.isNotEmpty(paymentBeneficiary)) {

          if (inv.getEfinIban() != null && inv.getEfinIban().getEfinBank() != null) {
            info.addResult("inpemEfinBankId",
                inv.getEfinIban().getId() == null ? "" : inv.getEfinIban().getEfinBank().getId());

            info.addResult("inpemEfinSupbankacct",
                inv.getEfinIban().getId() == null ? "" : inv.getEfinIban().getId());
          }
        }

        DocumentType doctyp = OBDal.getInstance().get(DocumentType.class,
            inv.getTransactionDocument().getId());
        if (doctyp.isEfinIsprepayinv()) {
          info.addResult("inpemEfinPrepayment", "Y");
        } else {
          info.addResult("inpemEfinPrepayment", "N");
        }

        // if Invoice is changed, set curreny based on invoice currency
        if (inv != null) {
          if (inv.getCurrency() != null) {
            info.addResult("inpcCurrencyId", inv.getCurrency().getId());
          }
        }
      }
    }
    // while changing Payment Instruction set Supplier Bank and Supplier Bank Account
    else if (inpLastFieldChanged.equals("inpemEfinPayinst")) {
      // info.addResult("inpemEfinSupbankacct", "");
      // info.addResult("JSEXECUTE", "form.getFieldFromColumnName('EM_Efin_Bank_ID').setValue('')");

      if (!StringUtils.isNotEmpty(paymentBeneficiary)) {
        if (StringUtils.isNotEmpty(inpInvoiceId)) {
          Invoice inv = OBDal.getInstance().get(Invoice.class, inpInvoiceId);
          if (inv.getEfinIban() != null && inv.getEfinIban().getEfinBank() != null) {
            info.addResult("inpemEfinBankId",
                inv.getEfinIban().getId() == null ? "" : inv.getEfinIban().getEfinBank().getId());

            info.addResult("inpemEfinSupbankacct",
                inv.getEfinIban().getId() == null ? "" : inv.getEfinIban().getId());
          }
        }
      } else {

        bankId = EfinPaymentoutCalloutDAO.getBankId(
            StringUtils.isEmpty(paymentBeneficiary) ? inpcBpartnerId : paymentBeneficiary,
            inpemEfinPayinst);
        info.addResult("inpemEfinBankId", bankId);

        SupplierBankAccount = EfinPaymentoutCalloutDAO.getSupplierBankAccount(bankId,
            StringUtils.isEmpty(paymentBeneficiary) ? inpcBpartnerId : paymentBeneficiary,
            inpemEfinPayinst);
        if (SupplierBankAccount != null) {
          info.addResult("inpemEfinSupbankacct", SupplierBankAccount);
        } else {
          info.addResult("inpemEfinSupbankacct", "");
        }

      }

    }
    // while changing Suppiler bank set Supplier Bank Account based on Payment Instruction
    // if IBAN Transfer then IBAN No. if Generic Account Transfer then AccountNo. from BP Bank
    // Account
    else if (inpLastFieldChanged.equals("inpemEfinBankId")) {
      SupplierBankAccount = EfinPaymentoutCalloutDAO.getSupplierBankAccount(inpemEfinBankId,
          StringUtils.isEmpty(paymentBeneficiary) ? inpcBpartnerId : paymentBeneficiary,
          inpemEfinPayinst);
      if (SupplierBankAccount != null) {
        info.addResult("inpemEfinSupbankacct", SupplierBankAccount);
      } else {
        info.addResult("inpemEfinSupbankacct", "");
      }

    }

    // while changing Payment Beneficiary set Supplier Bank and Supplier Bank Account
    else if (inpLastFieldChanged.equals("inpemEfinBpartnerId")) {
      if (!StringUtils.isNotEmpty(paymentBeneficiary)) {
        Invoice inv = OBDal.getInstance().get(Invoice.class, inpInvoiceId);
        info.addResult("inpemEfinBankId",
            inv.getEfinIban().getId() == null ? "" : inv.getEfinIban().getEfinBank().getId());

        info.addResult("inpemEfinSupbankacct",
            inv.getEfinIban().getId() == null ? "" : inv.getEfinIban().getId());
      } else {
        bankId = EfinPaymentoutCalloutDAO.getBankId(
            StringUtils.isEmpty(paymentBeneficiary) ? inpcBpartnerId : paymentBeneficiary,
            inpemEfinPayinst);
        info.addResult("inpemEfinBankId", bankId);

        SupplierBankAccount = EfinPaymentoutCalloutDAO.getSupplierBankAccount(bankId,
            StringUtils.isEmpty(paymentBeneficiary) ? inpcBpartnerId : paymentBeneficiary,
            inpemEfinPayinst);
        if (SupplierBankAccount != null) {
          info.addResult("inpemEfinSupbankacct", SupplierBankAccount);
        } else {
          info.addResult("inpemEfinSupbankacct", "");
        }

        info.addResult("JSEXECUTE",
            "form.getFieldFromInpColumnName('inpemEfinBeneficiary2Id').setValue('')");
      }

    }

  }
}
