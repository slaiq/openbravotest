package sa.elm.ob.finance.actionHandler;

import java.math.BigDecimal;
import java.util.Map;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.AppliedPrepaymentInvoice;

/**
 * 
 * @author gopalakrishnan on 22-08-2016
 * 
 */

public class EfinApplyPrepaymentHandler extends BaseProcessActionHandler {

  private static final Logger log = LoggerFactory.getLogger(EfinApplyPrepaymentHandler.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      String message = "";
      log.debug("params:" + jsonparams);

      JSONObject encumlines = jsonparams.getJSONObject("ApplyPrepayment");
      JSONArray selectedlines = encumlines.getJSONArray("_selection");
      for (int a = 0; a < selectedlines.length(); a++) {
        JSONObject selectedRow = selectedlines.getJSONObject(a);
        String prepaidInvoiceId = selectedRow.getString("id");
        String Amount = selectedRow.getString("amount");
        String remainAmount = selectedRow.getString("remainingAmount");
        BigDecimal bigRemianAmount = new BigDecimal(remainAmount);
        if (BigDecimal.ZERO.compareTo(new BigDecimal(Amount)) == 1) {
          throw new OBException(OBMessageUtils.messageBD("Efin_AppliedAmountNegative"));
        }
        if (new BigDecimal(Amount).compareTo(new BigDecimal(remainAmount)) == 1) {
          throw new OBException(OBMessageUtils.messageBD("Efin_AppliedAmt_isHigh"));
        }
        final String invoiceId = jsonRequest.getString("inpcInvoiceId");
        AppliedPrepaymentInvoice appliedPaymentInoice = null;
        OBQuery<AppliedPrepaymentInvoice> appliedPaymentInvQuery = OBDal.getInstance().createQuery(
            AppliedPrepaymentInvoice.class,
            "as e where e.invoice.id= :invoiceID and e.efinAppliedInvoice.id= :appliedInvoiceId");
        appliedPaymentInvQuery.setNamedParameter("invoiceID", invoiceId);
        appliedPaymentInvQuery.setNamedParameter("appliedInvoiceId", prepaidInvoiceId);
        appliedPaymentInvQuery.setMaxResult(1);
        if (appliedPaymentInvQuery.list().size() > 0) {
          appliedPaymentInoice = appliedPaymentInvQuery.list().get(0);
          if (appliedPaymentInoice.getAppliedAmount().add(new BigDecimal(Amount))
              .compareTo(bigRemianAmount) == 1) {
            throw new OBException(OBMessageUtils.messageBD("Efin_AppliedAmt_isHigh"));
          } else {
            appliedPaymentInoice.setAppliedAmount(
                appliedPaymentInoice.getAppliedAmount().add(new BigDecimal(Amount)));
            OBDal.getInstance().save(appliedPaymentInoice);
          }

        } else {
          appliedPaymentInoice = OBProvider.getInstance().get(AppliedPrepaymentInvoice.class);
          appliedPaymentInoice.setAppliedAmount(new BigDecimal(Amount));
          appliedPaymentInoice.setInvoice(OBDal.getInstance().get(Invoice.class, invoiceId));
          appliedPaymentInoice
              .setEfinAppliedInvoice(OBDal.getInstance().get(Invoice.class, prepaidInvoiceId));
          appliedPaymentInoice.setClient(OBDal.getInstance().get(Client.class, vars.getClient()));
          appliedPaymentInoice
              .setOrganization(OBDal.getInstance().get(Invoice.class, invoiceId).getOrganization());
          appliedPaymentInoice.setActive(true);
          appliedPaymentInoice.setCreatedBy(
              OBDal.getInstance().get(User.class, OBContext.getOBContext().getUser().getId()));
          appliedPaymentInoice.setCreationDate(new java.util.Date());
          appliedPaymentInoice.setCreatedBy(
              OBDal.getInstance().get(User.class, OBContext.getOBContext().getUser().getId()));
          appliedPaymentInoice.setUpdated(new java.util.Date());
          OBDal.getInstance().save(appliedPaymentInoice);
        }

      }
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
      JSONObject errormsg = new JSONObject();
      message = OBMessageUtils.parseTranslation("@Success@");
      errormsg.put("severity", "success");
      errormsg.put("text", message);
      jsonResponse.put("message", errormsg);
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception handling the Apply Pre payment", e);

      try {
        jsonResponse = new JSONObject();
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        jsonResponse.put("message", errorMessage);
      } catch (Exception ignore) {
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    log.debug(" Returned Response " + jsonResponse);
    return jsonResponse;
  }

}
