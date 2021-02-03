package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.scm.ESCMPaymentSchedule;
import sa.elm.ob.scm.actionHandler.dao.BidManagementAddLinesDAO;
import sa.elm.ob.scm.ad_callouts.dao.PurOrderSummaryDAO;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author poongodi 09/07/2020
 *
 */

public class DistributecontractPeriodProcess extends BaseActionHandler {
  private static Logger log = Logger.getLogger(DistributecontractPeriodProcess.class);
  private static String RECORD_ID = "Escm_Payment_Schedule_ID";

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject json = new JSONObject();
    JSONObject jsonResponse = new JSONObject();
    try {
      OBContext.setAdminMode();
      DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
      DateFormat Date_format = new SimpleDateFormat("dd-MM-yyyy");
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      final String paymentScheduleId = jsonRequest.getString(RECORD_ID);
      String payNature = jsonRequest.getString("inppayNature");
      String type = jsonRequest.getString("inptype");
      String uniqueCode = jsonRequest.getString("inpcValidcombinationId");
      String orderId = jsonRequest.getString("C_Order_ID");
      String orgId = jsonRequest.getString("inpadOrgId");
      // param values
      String amount = jsonparams.getString("amount");
      int count = jsonparams.getInt("count");
      String startDateFrom = jsonparams.getString("startDate");
      String frequency_list = jsonparams.getString("frequency");
      BigDecimal dist_amount = new BigDecimal(amount);
      BigDecimal distributionAmount = new BigDecimal(0);
      distributionAmount = (dist_amount.divide(new BigDecimal(count), 15, RoundingMode.HALF_UP))
          .setScale(2, RoundingMode.HALF_UP);
      BigDecimal value_per = new BigDecimal(0);
      Connection conn = OBDal.getInstance().getConnection();
      PreparedStatement ps = null;
      ResultSet rs = null;
      Order header = null;
      long lineno = 0;
      int loopno = 0;
      String Contractdate = null;
      BigDecimal diff = new BigDecimal(0);
      ESCMPaymentSchedule paySchedule = null;
      String monthPeriod = "0";
      String message = "";
      if (frequency_list.equals("MT")) {
        monthPeriod = "1";
      } else if (frequency_list.equals("QL")) {
        monthPeriod = "3";
      } else if (frequency_list.equals("AL")) {
        monthPeriod = "12";
      } else if (frequency_list.equals("SA")) {
        monthPeriod = "6";
      }
      if (paymentScheduleId != null) {
        paySchedule = OBDal.getInstance().get(ESCMPaymentSchedule.class, paymentScheduleId);
      }
      // convert the need by date in gregorian
      startDateFrom = startDateFrom.split("-")[0] + startDateFrom.split("-")[1]
          + startDateFrom.split("-")[2];
      startDateFrom = BidManagementAddLinesDAO.convertToGregorianDate(startDateFrom);
      // validation
      if (count == 1) {
        JSONObject erorMessage = new JSONObject();
        erorMessage.put("severity", "error");
        erorMessage.put("text", OBMessageUtils.messageBD("Escm_count_notallowed"));
        json.put("message", erorMessage);
        return json;
      }

      if (paySchedule.getNeedbydate() != null) {
        if (format.parse(startDateFrom).compareTo(paySchedule.getNeedbydate()) < 0) {
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD("Escm_Needbydate_lesser"));
          json.put("message", erorMessage);
          return json;
        }
      }

      if (orderId != null) {
        header = OBDal.getInstance().get(Order.class, orderId);
      }
      // update amount and needbydate
      paySchedule.setAmount(distributionAmount);
      value_per = (distributionAmount.multiply(new BigDecimal(100)))
          .divide(header.getGrandTotalAmount(), 15, RoundingMode.HALF_UP)
          .setScale(2, RoundingMode.HALF_UP);
      paySchedule.setValuePer(value_per);
      paySchedule.setNeedbydate(format.parse(startDateFrom));
      OBDal.getInstance().save(paySchedule);
      OBDal.getInstance().flush();

      String needbyDate = jsonparams.getString("startDate").split("-")[2] + "-"
          + jsonparams.getString("startDate").split("-")[1] + "-"
          + jsonparams.getString("startDate").split("-")[0];
      loopno = count - 1;
      // To insert the payment schedule table
      for (int i = 0; i < loopno; i++) {
        // take max line no in paymentschedule
        ps = conn.prepareStatement(
            " select coalesce(max(line),0)+10   as lineno from escm_payment_schedule where c_order_id=?");
        ps.setString(1, orderId);
        rs = ps.executeQuery();
        if (rs.next()) {
          lineno = rs.getLong("lineno");
        }
        ESCMPaymentSchedule paymentSchedule = OBProvider.getInstance()
            .get(ESCMPaymentSchedule.class);
        paymentSchedule.setOrganization(OBDal.getInstance().get(Organization.class, orgId));
        paymentSchedule.setLine(lineno);
        paymentSchedule.setPAYNature(payNature);
        paymentSchedule.setType(type);
        if (uniqueCode != null)
          paymentSchedule.setValidcombination(
              OBDal.getInstance().get(AccountingCombination.class, uniqueCode));
        // at final loop
        if (i == (loopno - 1)) {
          BigDecimal multiplyValue = distributionAmount
              .multiply(new BigDecimal(jsonparams.getInt("count")));
          if ((dist_amount.compareTo(multiplyValue) > 0)
              || (multiplyValue.compareTo(dist_amount) > 0)) {
            diff = dist_amount.subtract(multiplyValue);
            distributionAmount = distributionAmount.add(diff);
          }
        }
        paymentSchedule.setAmount(distributionAmount);
        value_per = (distributionAmount.multiply(new BigDecimal(100)))
            .divide(header.getGrandTotalAmount(), 15, RoundingMode.HALF_UP)
            .setScale(2, RoundingMode.HALF_UP);
        paymentSchedule.setValuePer(value_per);
        paymentSchedule.setDocumentNo(OBDal.getInstance().get(Order.class, orderId));
        // need by date

        Contractdate = PurOrderSummaryDAO.getContractDurationMonth(monthPeriod, "MT", needbyDate,
            "", OBContext.getOBContext().getCurrentClient().getId());
        paymentSchedule.setNeedbydate(format.parse(UtilityDAO.convertToGregorian(Contractdate)));

        needbyDate = Contractdate;
        OBDal.getInstance().save(paymentSchedule);
        OBDal.getInstance().flush();
      }

      // set success message
      JSONObject successmsg = new JSONObject();
      JSONObject refreshGrid = new JSONObject();
      message = OBMessageUtils.parseTranslation("@Escm_Ir_complete_success@");
      successmsg.put("severity", "success");
      successmsg.put("text", message);
      jsonResponse.put("message", successmsg);
      refreshGrid.put("message", successmsg);
      refreshGrid.put("refreshGrid", new JSONObject());
      jsonResponse.put("responseActions", refreshGrid);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
      return jsonResponse;

    } catch (

    Exception e) {
      e.printStackTrace();
      log.error("Exception in DistributecontractPeriodProcess :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonResponse;
  }
}
