package sa.elm.ob.scm.ad_process.RFPSalesVoucher;

import javax.servlet.http.HttpServletRequest;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.Escmsalesvoucher;
import sa.elm.ob.utility.util.Utility;

public class RFPSalesVoucherAction extends DalBaseProcess {
  private static final Logger log = LoggerFactory.getLogger(RFPSalesVoucherAction.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    boolean status = false, isPeriodOpen = true;
    // boolean chkamountvalidation = false; //removed for Task #5961
    try {
      OBContext.setAdminMode();
      final String voucherId = (String) bundle.getParams().get("Escm_Salesvoucher_ID").toString();
      Escmsalesvoucher salesRFP = Utility.getObject(Escmsalesvoucher.class, voucherId);
      String bidno = salesRFP.getEscmBidmgmt().getId();
      // checking cdn for amount cannot be zero while complete rfp
      // removed for Task #5961

      /*
       * chkamountvalidation = RFPSalesvoucherDao.checkPaymentAmountValidation(salesRFP); if
       * (chkamountvalidation) { final OBError msg = new OBError();
       * msg.setType(OBMessageUtils.messageBD("OBUIAPP_Error"));
       * msg.setTitle(OBMessageUtils.messageBD("OBUIAPP_Error")); //
       * msg.setMessage(OBMessageUtils.messageBD("ESCM_Amount_CannotZero")); bundle.setResult(msg);
       * }
       */
      if (salesRFP.getDocumentStatus().equals("DR")) {
        // Check transaction period is opened or not
        isPeriodOpen = Utility.checkOpenPeriod(salesRFP.getSalesdate(),
            salesRFP.getOrganization().getId(), vars.getClient());
        if (!isPeriodOpen) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
          bundle.setResult(result);
          return;
        }
      }
      // performing action for compelete or reactivate.
      // and update DocumentNo while compelete

      // else {
      status = RFPSalesvoucherDao.performAction(salesRFP, bidno);
      if (status) {
        // Setting success message
        final OBError msg = new OBError();
        msg.setType(OBMessageUtils.messageBD("OBUIAPP_Success"));
        msg.setTitle(OBMessageUtils.messageBD("OBUIAPP_Success"));
        msg.setMessage(OBMessageUtils.messageBD("ESCM_Process_Success"));
        bundle.setResult(msg);
        OBDal.getInstance().flush();
      } else {
        final OBError msg = new OBError();
        msg.setType(OBMessageUtils.messageBD("OBUIAPP_Error"));
        msg.setTitle(OBMessageUtils.messageBD("OBUIAPP_Error"));
        msg.setMessage(OBMessageUtils.messageBD("Escm_Rfp_Reactivate").replaceAll("@",
            RFPSalesvoucherDao.getProposalNo(salesRFP)));
        bundle.setResult(msg);
      }
      // }
    } catch (Exception e) {
      log.error("Exeception in RFPSalesVoucherAction:", e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
