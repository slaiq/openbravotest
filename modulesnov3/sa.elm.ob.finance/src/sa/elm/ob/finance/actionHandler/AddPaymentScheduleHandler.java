/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package sa.elm.ob.finance.actionHandler;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinInvoicePaymentSch;
import sa.elm.ob.scm.ESCMPaymentSchedule;
import sa.elm.ob.utility.util.UtilityDAO;

public class AddPaymentScheduleHandler extends BaseProcessActionHandler {
  final private static Logger log = LoggerFactory.getLogger(AddPaymentScheduleHandler.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    OBContext.setAdminMode(true);

    try {
      log.debug("payment schedule handler");

      // Get Params
      JSONObject jsonRequest = new JSONObject(content);
      log.debug(" jsonRequest: " + jsonRequest);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      final String strInvoiceId = jsonRequest.has("C_Invoice_ID")
          ? (jsonRequest.getString("C_Invoice_ID") == null ? ""
              : jsonRequest.getString("C_Invoice_ID"))
          : "";
      Invoice obj_invoice = OBDal.getInstance().get(Invoice.class, strInvoiceId);
      JSONObject paymentSchedulesLines = jsonparams.getJSONObject("Lines");
      // insert payment schedule reference
      Boolean isInserted = addSelectedPSDs(obj_invoice, paymentSchedulesLines);
      if (!isInserted) {

        // setting error message
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "success");
        successMessage.put("text", OBMessageUtils.messageBD("EFIN_Process_Failed_Pay_Sch"));
        jsonResponse.put("message", successMessage);

      } else {
        // setting success message
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "success");
        successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
        jsonResponse.put("message", successMessage);
      }
      return jsonResponse;

    } catch (

    Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception handling the new payment scheduler invoice", e);

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

  /**
   * 
   * @param obj_invoice
   * @param jsonparams
   * @return if records are inserted successfully will return true, else false
   */
  private Boolean addSelectedPSDs(Invoice obj_invoice, JSONObject paymentSchedulesLines) {
    Boolean recordInserted = Boolean.TRUE;
    try {
      OBContext.setAdminMode();
      JSONArray selectedlines = paymentSchedulesLines.getJSONArray("_selection");
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      String gregorian_need_by_date = "";
      Date needby_date_ddMMyyyy = null;
      if (selectedlines.length() > 0) {
        for (int a = 0; a < selectedlines.length(); a++) {
          JSONObject selectedRow = selectedlines.getJSONObject(a);
          String strUniqueCode = selectedRow.getString("validcombination");
          String strPayType = selectedRow.getString("type");
          String poPaymentScheduleId = selectedRow.getString("id");
          String amount = selectedRow.getString("amount");
          System.out.println("selectedRow:" + selectedRow.toString());
          EfinInvoicePaymentSch objInvoicePaySch = OBProvider.getInstance()
              .get(EfinInvoicePaymentSch.class);
          objInvoicePaySch.setClient(obj_invoice.getClient());
          objInvoicePaySch.setOrganization(obj_invoice.getOrganization());
          objInvoicePaySch.setCreatedBy(obj_invoice.getCreatedBy());
          objInvoicePaySch.setUpdatedBy(obj_invoice.getCreatedBy());
          if (StringUtils.isNotEmpty(strUniqueCode)) {
            objInvoicePaySch.setAccountingCombination(
                OBDal.getInstance().get(AccountingCombination.class, strUniqueCode));
          }
          objInvoicePaySch.setInvoice(obj_invoice);
          objInvoicePaySch.setPAYType(strPayType);
          objInvoicePaySch.setEscmPaymentSchedule(
              OBDal.getInstance().get(ESCMPaymentSchedule.class, poPaymentScheduleId));
          objInvoicePaySch.setAmount(new BigDecimal(amount));

          String needbydate = selectedRow.getString("needbydate");
          if (StringUtils.isNotEmpty(needbydate)) {
            String date_part1 = needbydate.split("-")[0];
            String date_part2 = needbydate.split("-")[1];
            String date_part3 = needbydate.split("-")[2];
            // format dd-mm-yyyy
            String converted_need_by_date = date_part3.concat("-").concat(date_part2).concat("-")
                .concat(date_part1);
            gregorian_need_by_date = UtilityDAO.convertToGregorian(converted_need_by_date);
            needby_date_ddMMyyyy = dateFormat.parse(gregorian_need_by_date);
            objInvoicePaySch.setNeedByDate(needby_date_ddMMyyyy);
          }
          OBDal.getInstance().save(objInvoicePaySch);
          OBDal.getInstance().flush();

        }
      }
    } catch (Exception e) {
      recordInserted = Boolean.FALSE;
      OBDal.getInstance().rollbackAndClose();
      log.error("Error while adding payment schedule in invoice", e);
    } finally {
      OBContext.restorePreviousMode();
    }

    return recordInserted;
  }

}
