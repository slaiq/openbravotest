package sa.elm.ob.scm.actionHandler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;

import sa.elm.ob.scm.actionHandler.dao.BidManagementAddLinesDAO;
import sa.elm.ob.scm.actionHandler.dao.POActionChangeDAO;
import sa.elm.ob.scm.ad_callouts.dao.PurOrderSummaryDAO;

/**
 * 
 * @author kousalya on 27/05/2019
 * 
 */
public class POActionChangeHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(POActionChangeHandler.class);
  private static String RECORD_ID = "C_Order_ID";

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject json = new JSONObject();
    JSONObject jsonResponse = new JSONObject();
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    Order order = null;
    try {
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      final String orderId = jsonRequest.getString(RECORD_ID);

      final String changeStatus = jsonparams.getString("changestatus");
      final String decreeNumber = jsonparams.getString("decreenumber");
      String decreeDate = jsonparams.getString("decreedate");
      final String justification = jsonparams.getString("justification");
      String onHoldFromDate = "", periodType = "", duration = "", onHoldToDate = "", startDate = "",
          endDate = "";

      // PO cancel process
      if (changeStatus.equals("ESCM_CAN")) {
        OBError result = POActionChangeDAO.cancelPO(orderId, justification, vars);
        if (result.getType().toLowerCase().equals("success")) {
          JSONObject successMessage = new JSONObject();
          successMessage.put("severity", "success");
          successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
          json.put("message", successMessage);
          return json;
        } else {
          OBDal.getInstance().rollbackAndClose();

          String message = result.getMessage();
          message = message.replace("@", "");
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD(message));
          json.put("message", erorMessage);
          return json;
        }
      } else {

        // Check decreeNumber and Justification cannot be empty
        if ((decreeNumber.trim()).equals("") || (justification.trim()).equals("")) {
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD("ESCM_DecNoJustCantBeEmpty"));
          json.put("message", erorMessage);
          return json;
        }
        order = OBDal.getInstance().get(Order.class, orderId);

        if (changeStatus.equals("ESCM_OHLD") || changeStatus.equals("ESCM_EOHLD")) {
          onHoldFromDate = jsonparams.getString("onholdfromdate");
          periodType = jsonparams.getString("periodtype");
          duration = jsonparams.getString("duration");
          onHoldToDate = jsonparams.getString("onholdtodate");
          if (changeStatus.equals("ESCM_EOHLD")) {
            Date prevOnHoldEndDate = POActionChangeDAO.getPOAmendmentOnHoldEndDate(orderId);
            Calendar cal = Calendar.getInstance();
            cal.setTime(prevOnHoldEndDate);
            cal.add(Calendar.DAY_OF_MONTH, 1);

            String fromDate = onHoldFromDate.split("-")[0] + onHoldFromDate.split("-")[1]
                + onHoldFromDate.split("-")[2];
            fromDate = BidManagementAddLinesDAO.convertToGregorianDate(fromDate);
            Date onFrmDate = format.parse(fromDate);

            String curToDate = onHoldToDate.split("-")[0] + onHoldToDate.split("-")[1]
                + onHoldToDate.split("-")[2];
            curToDate = BidManagementAddLinesDAO.convertToGregorianDate(curToDate);
            Date cToDate = format.parse(curToDate);
            // Check onholdfromdate cannot be lesser than prev onhold end date
            if (onFrmDate.compareTo(cal.getTime()) < 0) {
              JSONObject erorMessage = new JSONObject();
              erorMessage.put("severity", "error");
              erorMessage.put("text",
                  OBMessageUtils.messageBD("ESCM_OHLDDateGreaterThanPrevOHLDE"));
              json.put("message", erorMessage);
              return json;
            }
            // Check onholdfromdate cannot be greater than onholdenddate
            if (onFrmDate.compareTo(cToDate) > 0) {
              JSONObject erorMessage = new JSONObject();
              erorMessage.put("severity", "error");
              erorMessage.put("text", OBMessageUtils.messageBD("ESCM_POOHLDToDateCantBeLess"));
              json.put("message", erorMessage);
              return json;
            }
          }
          // Check whether date calculation is correct
          if (onHoldFromDate != null && !onHoldFromDate.equals("null") && periodType != null
              && periodType != "" && !periodType.equals("null") && duration != null
              && !duration.equals("") && !duration.equals("null")) {
            startDate = onHoldFromDate.split("-")[2] + "-" + onHoldFromDate.split("-")[1] + "-"
                + onHoldFromDate.split("-")[0];
            if (periodType.equals("MT")) {
              endDate = PurOrderSummaryDAO.getContractDurationMonth(duration, periodType, startDate,
                  null, vars.getClient());
              endDate = endDate.split("-")[2] + "-" + endDate.split("-")[1] + "-"
                  + endDate.split("-")[0];

            } else {
              endDate = PurOrderSummaryDAO.getContractDurationday(duration, periodType, startDate,
                  null, vars.getClient());
              endDate = endDate.split("-")[2] + "-" + endDate.split("-")[1] + "-"
                  + endDate.split("-")[0];
            }
            if (!endDate.equals(onHoldToDate)) {
              onHoldToDate = endDate;
            }
          }
          // On Hold To Date is mandatory
          if (onHoldToDate == null || onHoldToDate.equals("null") || onHoldToDate.equals("")) {
            JSONObject erorMessage = new JSONObject();
            erorMessage.put("severity", "error");
            erorMessage.put("text", OBMessageUtils.messageBD("ESCM_OnHoldToDate_Mandatory"));
            json.put("message", erorMessage);
            return json;
          }
        }
        if (order.getEscmAppstatus().equals("ESCM_AP")) {
          if (changeStatus.equals("ESCM_WD"))
            order.setEscmAppstatus("ESCM_WD");
          else if (changeStatus.equals("ESCM_OHLD"))
            order.setEscmAppstatus("ESCM_OHLD");
          else {
            JSONObject erorMessage = new JSONObject();
            erorMessage.put("severity", "error");
            erorMessage.put("text", OBMessageUtils.messageBD("ESCM_POStatCanbeWDOHLD"));
            json.put("message", erorMessage);
            return json;
          }
        } else if (order.getEscmAppstatus().equals("ESCM_WD")) {
          if (changeStatus.equals("ESCM_RWD"))
            order.setEscmAppstatus("ESCM_AP");
          else {
            JSONObject erorMessage = new JSONObject();
            erorMessage.put("severity", "error");
            erorMessage.put("text", OBMessageUtils.messageBD("ESCM_POStatCanbeRWD"));
            json.put("message", erorMessage);
            return json;
          }
        } else if (order.getEscmAppstatus().equals("ESCM_OHLD")) {
          if (changeStatus.equals("ESCM_ROHLD"))
            order.setEscmAppstatus("ESCM_AP");
          else if (changeStatus.equals("ESCM_EOHLD"))
            order.setEscmAppstatus("ESCM_OHLD");
          else {
            JSONObject erorMessage = new JSONObject();
            erorMessage.put("severity", "error");
            erorMessage.put("text", OBMessageUtils.messageBD("ESCM_POStatCanbeROHLDEOHLD"));
            json.put("message", erorMessage);
            return json;
          }
        }
        long dur = 0L;
        decreeDate = decreeDate.split("-")[0] + decreeDate.split("-")[1] + decreeDate.split("-")[2];
        decreeDate = BidManagementAddLinesDAO.convertToGregorianDate(decreeDate);
        Date onHldFromDt = null;
        Date onHldToDt = null;
        if (changeStatus.equals("ESCM_OHLD") || changeStatus.equals("ESCM_EOHLD")) {
          if (duration != null && !duration.equals("null"))
            dur = Long.parseLong(duration);
          onHoldFromDate = onHoldFromDate.split("-")[0] + onHoldFromDate.split("-")[1]
              + onHoldFromDate.split("-")[2];
          onHoldFromDate = BidManagementAddLinesDAO.convertToGregorianDate(onHoldFromDate);
          onHldFromDt = format.parse(onHoldFromDate);

          onHoldToDate = onHoldToDate.split("-")[0] + onHoldToDate.split("-")[1]
              + onHoldToDate.split("-")[2];
          onHoldToDate = BidManagementAddLinesDAO.convertToGregorianDate(onHoldToDate);
          onHldToDt = format.parse(onHoldToDate);

          // Throw error if hold end date is lesser than hold start date
          if (onHldFromDt != null && onHldToDt != null && onHldFromDt.compareTo(onHldToDt) > 0) {
            OBDal.getInstance().rollbackAndClose();

            JSONObject erorMessage = new JSONObject();
            erorMessage.put("severity", "error");
            erorMessage.put("text", OBMessageUtils.messageBD("ESCM_POOHLDToDateCantBeLess"));
            json.put("message", erorMessage);
            return json;
          }

          // Past date is not allowed in Hold from date and Hold to date.
          Date todaydate = format.parse(format.format(new Date()));
          if (onHldFromDt != null && onHldToDt != null
              && (onHldFromDt.compareTo(todaydate) < 0 || onHldToDt.compareTo(todaydate) < 0)) {
            OBDal.getInstance().rollbackAndClose();

            JSONObject erorMessage = new JSONObject();
            erorMessage.put("severity", "error");
            erorMessage.put("text", OBMessageUtils.messageBD("Escm_HoldPastDate"));
            json.put("message", erorMessage);
            return json;
          }

        }

        int count = POActionChangeDAO.insertPOAmendmentChangeAction(order, vars.getUser(),
            changeStatus, decreeNumber, format.parse(decreeDate), justification, onHldFromDt,
            onHldToDt, dur, periodType);

        if (count == 1) {
          JSONObject successMessage = new JSONObject();
          successMessage.put("severity", "success");
          successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
          json.put("message", successMessage);
          return json;
        }
      }
    } catch (Exception e) {
      log.error("Exception in POActionChangeHandler :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonResponse;
  }
}
