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

package org.openbravo.actionHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.util.FinanceUtils;
import sa.elm.ob.utility.util.UtilityDAO;

public class AddPaymentOnProcessConversionActionHandler extends BaseActionHandler {
	private static Logger log = LoggerFactory.getLogger(AddPaymentOnProcessConversionActionHandler.class);

	@Override
	protected JSONObject execute(Map<String, Object> parameters, String data) {
		JSONObject result = new JSONObject();
		JSONObject errorMessage = new JSONObject();
		OBContext.setAdminMode(true);
		try {
			final JSONObject jsonData = new JSONObject(data);
			boolean isReceipt = "true".equals(jsonData.getString("issotrx"));

			String scheduledetailId = null, sql = "", sql1 = "";
			PreparedStatement ps = null, ps1 = null;
			ResultSet rs = null, rs1 = null;
			Connection conn = OBDal.getInstance().getConnection();
			BigDecimal conversionrate = BigDecimal.ZERO;
			Currency currency = null;
			Date invoiceDate = null;
			boolean checkconversionexist = false;
			SimpleDateFormat timeYearFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String paymentId = jsonData.getString("payment");
			FIN_Payment pay = OBDal.getInstance().get(FIN_Payment.class, paymentId);

			if(pay != null) {
				if(log.isDebugEnabled()) {
					log.debug("paymentcurrency:" + pay.getCurrency().getSymbol());
					log.debug("financurrency:" + pay.getAccount().getCurrency().getSymbol());
				}

				if(!pay.getCurrency().getId().equals(pay.getAccount().getCurrency().getId())) {
					if(!isReceipt) {
						JSONArray selectedPSDs = jsonData.getJSONArray("selectedRecords");
						for (int i = 0; i < selectedPSDs.length(); i++) {
							JSONObject psdRow = selectedPSDs.getJSONObject(i);
							scheduledetailId = psdRow.getString("id");
							if(scheduledetailId != null) {
								FIN_PaymentScheduleDetail schedetail = OBDal.getInstance().get(FIN_PaymentScheduleDetail.class, scheduledetailId);
								FIN_PaymentSchedule sched = OBDal.getInstance().get(FIN_PaymentSchedule.class, schedetail.getInvoicePaymentSchedule().getId());

								//GET PARENT ORG CURRENCY
								currency = FinanceUtils.getCurrency(sched.getInvoice().getOrganization().getId(), sched.getInvoice());
								invoiceDate = sched.getInvoice().getInvoiceDate();
								//get conversion rate 
								sql = " SELECT rate as rate  FROM C_Conversion_Rate_Document  WHERE C_Invoice_ID ='" + sched.getInvoice().getId() + "'   AND C_Currency_ID ='" + sched.getInvoice().getCurrency().getId() + "'  AND C_Currency_Id_To ='" + currency.getId() + "'";
								ps = conn.prepareStatement(sql);
								log.debug("stconvers:" + ps.toString());
								rs = ps.executeQuery();
								if(rs.next()) {

									conversionrate = rs.getBigDecimal("rate");
									checkconversionexist = true;
									break;
								}
								else {
									sql1 = " SELECT multiplyrate as rate  FROM C_Conversion_Rate   WHERE C_Currency_ID ='" + sched.getInvoice().getCurrency().getId() + "'    AND C_Currency_ID_To ='" + currency.getId() + "'    AND ConversionRateType = 'S'    AND ('"
											+ sched.getInvoice().getInvoiceDate() + "') BETWEEN ValidFrom AND ValidTo    " + "     AND AD_Client_ID IN ('0', '" + sched.getInvoice().getClient().getId() + "')      AND AD_Org_ID IN ('0', '" + sched.getInvoice().getOrganization().getId()
											+ "')    AND IsActive = 'Y'    ORDER BY AD_Client_ID DESC,  AD_Org_ID DESC,ValidFrom DESC";
									ps1 = conn.prepareStatement(sql1);
									log.debug("conver:" + ps1.toString());
									rs1 = ps1.executeQuery();
									if(rs1.next()) {
										conversionrate = rs1.getBigDecimal("rate");
										checkconversionexist = true;
										break;
									}
								}

							}
						}
						if(!checkconversionexist) {
							errorMessage.put("severity", "error");
							errorMessage.put("title", "Error");
							errorMessage.put("text",
									OBMessageUtils.messageBD("NoConversionRate") + " (" + pay.getCurrency().getISOCode() + "-" + pay.getCurrency().getSymbol() + " ) " + OBMessageUtils.messageBD("to") + " ( " + currency.getISOCode() + "-" + currency.getSymbol() + ") "
											+ OBMessageUtils.messageBD("ForDate") + " ' " + UtilityDAO.convertTohijriDate(timeYearFormat.format(invoiceDate)) + "' " + OBMessageUtils.messageBD("Client") + " '" + pay.getClient().getName() + "' " + OBMessageUtils.messageBD("And") + " "
											+ OBMessageUtils.messageBD("ACCS_AD_ORG_ID_D") + " '" + pay.getOrganization().getName() + "'.");
							result.put("message", errorMessage);
							return result;
						}
						else {
							String message = "Ok";
							errorMessage.put("severity", "success");
							errorMessage.put("text", message);
							result.put("message", errorMessage);
							result.put("conversionrate", conversionrate);
							return result;
						}
					}
				}
			}
			else {
				if(isReceipt) {
					String message = "Ok";
					errorMessage.put("severity", "success");
					errorMessage.put("text", message);
					result.put("message", errorMessage);
					result.put("conversionrate", BigDecimal.ONE);
					return result;
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			OBDal.getInstance().rollbackAndClose();
			try {
				Throwable ex = DbUtility.getUnderlyingSQLException(e);
				String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
				errorMessage = new JSONObject();
				errorMessage.put("severity", "error");
				errorMessage.put("title", "Error");
				errorMessage.put("text", message);
				result.put("message", errorMessage);
			}
			catch (Exception e2) {
				e2.printStackTrace();
				log.error(e.getMessage(), e2);
				// do nothing, give up
			}
		}
		finally {
			OBContext.restorePreviousMode();
		}
		return result;
	}
}