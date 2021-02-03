package sa.elm.ob.finance.ad_process;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

import sa.elm.ob.finance.Efin_payment_sequences;
import sa.elm.ob.finance.dao.PaymentSequenceDAO;

public class ApplySequence extends DalBaseProcess {

  /**
   * This process is used to apply payment sequence based on Payment-parent sequence selected in
   * payment out
   */

  private static final Logger log = Logger.getLogger(ApplySequence.class);
  private OBError obError = new OBError();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {

    try {
      OBContext.setAdminMode(true);
      final String paymentId = (String) bundle.getParams().get("Fin_Payment_ID");

      FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, paymentId);

      if (payment != null) {
        Efin_payment_sequences paymentSeq = payment.getEfinPaymentSequences();

        if (paymentSeq == null) {
          OBError result = OBErrorBuilder.buildMessage(null, "warning", "@Efin_nosequenceavail@");
          bundle.setResult(result);
          return;
        }

        if (paymentSeq != null && payment.getEfinParentSequences() == null) {
          OBError result = OBErrorBuilder.buildMessage(null, "warning", "@Efin_selectParentseq@");
          bundle.setResult(result);
          return;
        }

        if (paymentSeq != null && (payment.getEfinPaymentsequence() == null
            || StringUtils.isEmpty(payment.getEfinPaymentsequence()))) {
          if (paymentSeq.isApply()) {
            String paymentSequence = PaymentSequenceDAO.getPaymentSequence(payment);
            if (paymentSequence == null) {
              OBError result = OBErrorBuilder.buildMessage(null, "warning",
                  "@Efin_nosequenceavail@");
              bundle.setResult(result);
              return;
            }
            payment.setEfinPaymentsequence(paymentSequence);
            OBDal.getInstance().save(payment);
            OBDal.getInstance().flush();
          } else {
            OBError result = OBErrorBuilder.buildMessage(null, "warning",
                "@Efin_applyseqnotavail@");
            bundle.setResult(result);
            return;
          }
        }
      }

      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_applyseqsuccess@");
      bundle.setResult(result);
      return;

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      if (log.isDebugEnabled()) {
        log.debug("Exception in apply sequences" + e, e);
      }
      obError = OBErrorBuilder.buildMessage(null, "error", e.toString());
      bundle.setResult(obError);
      return;
    } finally {
      OBContext.restorePreviousMode();
    }

  }

}
