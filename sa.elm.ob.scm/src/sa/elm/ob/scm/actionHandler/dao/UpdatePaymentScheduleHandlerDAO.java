package sa.elm.ob.scm.actionHandler.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMPaymentSchedule;
import sa.elm.ob.utility.util.Constants;

/**
 * 
 * @author Mouli K
 *
 */

public class UpdatePaymentScheduleHandlerDAO {
  private static final Logger LOG = LoggerFactory.getLogger(UpdatePaymentScheduleHandlerDAO.class);

  /**
   * This method is used to update in Payment Schedule through Update Payment Schedule process.
   * 
   * @param selectedlines
   * @param orderId
   * @return 1,2,3,4,5,6,7,8
   */
  public static int updatePaymentScheduleLines(JSONArray selectedlines, String orderId) {
    try {
      OBContext.setAdminMode();
      Order objOrder = OBDal.getInstance().get(Order.class, orderId);
      BigDecimal amt = BigDecimal.ZERO, updatedAmtTemp, invAmt, finalPayAmt = BigDecimal.ZERO,
          retainageAmt = objOrder.getEscmRetainageAmt(), valuePer = BigDecimal.ZERO,
          amount = BigDecimal.ZERO;
      List<String> updatedPayScheduleIds = new ArrayList<String>();
      Map<String, BigDecimal> updatedPayScheduleIdAmtMap = new HashMap<String, BigDecimal>();
      Map<String, Date> updatedPayScheduleIdNBDMap = new HashMap<String, Date>();

      Map<String, BigDecimal> psUCAmtWCMap = new HashMap<String, BigDecimal>();
      Map<String, BigDecimal> psUCAmtWOCMap = new HashMap<String, BigDecimal>();
      Map<String, BigDecimal> psUCNetLineAmtCMap = new HashMap<String, BigDecimal>();
      String uniqueCode, paymentScheduleId, needbydateTemp;
      ESCMPaymentSchedule payScheduleObj = null;
      Date needbydate;
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      Date orderDate = objOrder.getOrderDate();

      String orgId = objOrder.getOrganization().getId();
      String clientId = OBContext.getOBContext().getCurrentClient().getId();
      String userId = OBContext.getOBContext().getUser().getId();
      String roleId = OBContext.getOBContext().getRole().getId();
      String windowId = Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY_W;
      String propertyName = "Escm_PurMgr";
      String pre = "N";
      try {
        pre = sa.elm.ob.utility.util.Preferences.getPreferenceValue(propertyName, true, clientId,
            orgId, userId, roleId, windowId, "N");
      } catch (Exception e) {
        // Preference Not Found
        pre = "N";
      }

      if ((objOrder.getEscmAppstatus().equals("ESCM_IP")
          || objOrder.getEscmAppstatus().equals("ESCM_AP")) && pre.equals("Y")
          && objOrder.isEscmIspaymentschedule()) {
        if (selectedlines.length() > 0) {
          for (int line = 0; line < selectedlines.length(); line++) {
            needbydate = null;
            JSONObject selectedRow = selectedlines.getJSONObject(line);
            LOG.debug("selectedRow:" + selectedRow);
            paymentScheduleId = selectedRow.getString("id");
            updatedPayScheduleIds.add(paymentScheduleId);
            updatedAmtTemp = new BigDecimal(selectedRow.getString("amount"));
            invAmt = new BigDecimal(selectedRow.getString("invoicedAmt"));

            // Payment schedule Need by date is mandatory & should be greater than PO date
            if (selectedRow.getString("needbydate") != null
                && !selectedRow.getString("needbydate").equals("null")) {
              needbydateTemp = selectedRow.getString("needbydate");
              // convert the need by date in gregorian
              needbydateTemp = needbydateTemp.split("-")[0] + needbydateTemp.split("-")[1]
                  + needbydateTemp.split("-")[2];
              needbydateTemp = BidManagementAddLinesDAO.convertToGregorianDate(needbydateTemp);
              needbydate = dateFormat.parse(needbydateTemp);
              if (needbydate.compareTo(orderDate) <= 0) {
                return 8;
              }
            } else {
              return 8;
            }

            // Payment schedule zero is not allowed
            if (updatedAmtTemp.compareTo(BigDecimal.ZERO) == 0)
              return 2;

            // Payment Schedule amount should be greater than or equal to invoiced amount
            if (updatedAmtTemp.compareTo(invAmt) < 0)
              return 4;

            if (selectedRow.getString("pAYNature").equals("FP"))
              finalPayAmt = finalPayAmt.add(updatedAmtTemp);

            uniqueCode = selectedRow.getString("validcombination");
            if (uniqueCode != null && !uniqueCode.equals("null")) {
              if (selectedRow.getString("oLDContract").equals("true")) {
                if (psUCAmtWCMap.get(uniqueCode) == null)
                  psUCAmtWCMap.put(uniqueCode, updatedAmtTemp);
                else
                  psUCAmtWCMap.put(uniqueCode, psUCAmtWCMap.get(uniqueCode).add(updatedAmtTemp));

              } else {
                if (psUCAmtWOCMap.get(uniqueCode) == null)
                  psUCAmtWOCMap.put(uniqueCode, updatedAmtTemp);
                else
                  psUCAmtWOCMap.put(uniqueCode, psUCAmtWOCMap.get(uniqueCode).add(updatedAmtTemp));

              }
            }

            amt = amt.add(updatedAmtTemp);
            updatedPayScheduleIdAmtMap.put(paymentScheduleId, updatedAmtTemp);
            updatedPayScheduleIdNBDMap.put(paymentScheduleId, needbydate);

          }

          // Payment Schedule -> sum of Amount should be equal to Net Total Amount
          List<ESCMPaymentSchedule> pslist = objOrder.getEscmPaymentScheduleList().stream()
              .filter(a -> !updatedPayScheduleIds.contains(a.getId())).collect(Collectors.toList());
          for (ESCMPaymentSchedule paymentSchedule : pslist)
            amt = amt.add(paymentSchedule.getAmount());
          if (objOrder.getGrandTotalAmount().compareTo(amt) != 0)
            return 3;

          // If Unique Code exist then summing up the Payment Schedule-> Amount based on
          // (Contract & without Contract) with Unique Code
          for (ESCMPaymentSchedule paymentSchedule : pslist) {
            if (paymentSchedule.getValidcombination() != null) {
              if (paymentSchedule.isOLDContract()) {
                if (psUCAmtWCMap.get(paymentSchedule.getValidcombination().getId()) == null)
                  psUCAmtWCMap.put(paymentSchedule.getValidcombination().getId(),
                      paymentSchedule.getAmount());
                else
                  psUCAmtWCMap.put(paymentSchedule.getValidcombination().getId(),
                      psUCAmtWCMap.get(paymentSchedule.getValidcombination().getId())
                          .add(paymentSchedule.getAmount()));

              } else {
                if (psUCAmtWOCMap.get(paymentSchedule.getValidcombination().getId()) == null)
                  psUCAmtWOCMap.put(paymentSchedule.getValidcombination().getId(),
                      paymentSchedule.getAmount());
                else
                  psUCAmtWOCMap.put(paymentSchedule.getValidcombination().getId(),
                      psUCAmtWOCMap.get(paymentSchedule.getValidcombination().getId())
                          .add(paymentSchedule.getAmount()));

              }
            }
          }

          // If Unique Code exist then summing up the OrderLine-> Net Line Amount based on the
          // Unique Code
          for (OrderLine orderLineObj : objOrder.getOrderLineList()) {
            if (orderLineObj.getEFINUniqueCode() != null) {
              if (psUCNetLineAmtCMap.get(orderLineObj.getEFINUniqueCode().getId()) == null)
                psUCNetLineAmtCMap.put(orderLineObj.getEFINUniqueCode().getId(),
                    orderLineObj.getLineNetAmount());
              else
                psUCNetLineAmtCMap.put(orderLineObj.getEFINUniqueCode().getId(),
                    psUCNetLineAmtCMap.get(orderLineObj.getEFINUniqueCode().getId())
                        .add(orderLineObj.getLineNetAmount()));
            }
          }
          for (String uniqueCodeId : psUCNetLineAmtCMap.keySet()) {
            if (psUCAmtWCMap.get(uniqueCodeId) != null) {
              // If unique code & old contract exist then amount should be less or equal to
              // uniquecode level(order line- net total amount)
              if (psUCAmtWCMap.get(uniqueCodeId)
                  .compareTo(psUCNetLineAmtCMap.get(uniqueCodeId)) > 0)
                return 6;
            }
            if (psUCAmtWOCMap.get(uniqueCodeId) != null) {
              // If unique code & no old contract then amount should be equal to uniquecode
              // level(order line- net total amount)
              if (psUCAmtWOCMap.get(uniqueCodeId)
                  .compareTo(psUCNetLineAmtCMap.get(uniqueCodeId)) != 0)
                return 7;
            }
          }

          // Validating Final Pay amount should be equal to Retainage amount
          pslist = pslist.stream().filter(a -> a.getPAYNature().equals("FP"))
              .collect(Collectors.toList());
          for (ESCMPaymentSchedule paymentSchedule : pslist)
            finalPayAmt = finalPayAmt.add(paymentSchedule.getAmount());
          if (finalPayAmt.compareTo(retainageAmt) != 0 && objOrder.isEscmIsadvancepayment())
            return 5;

          // If all the above validation are satisfied then update Amount,Need by date, Value %
          // based
          // on the Amount.
          objOrder.setEscmUpdatePaymentschedule(true);
          OBDal.getInstance().save(objOrder);
          OBDal.getInstance().flush();
          for (String payScheduleId : updatedPayScheduleIdAmtMap.keySet()) {
            payScheduleObj = OBDal.getInstance().get(ESCMPaymentSchedule.class, payScheduleId);
            if (payScheduleObj != null) {
              amount = updatedPayScheduleIdAmtMap.get(payScheduleId);
              needbydate = updatedPayScheduleIdNBDMap.get(payScheduleId);
              if (objOrder.getGrandTotalAmount().compareTo(BigDecimal.ZERO) > 0)
                valuePer = (amount.multiply(new BigDecimal(100)))
                    .divide(objOrder.getGrandTotalAmount(), 2, RoundingMode.FLOOR);

              payScheduleObj.setAmount(amount);
              payScheduleObj.setValuePer(valuePer);
              payScheduleObj.setNeedbydate(needbydate);
              OBDal.getInstance().save(payScheduleObj);
              OBDal.getInstance().flush();
            }
          }
          objOrder.setEscmUpdatePaymentschedule(false);
          OBDal.getInstance().save(objOrder);
          OBDal.getInstance().flush();
        }
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception while updating records in Update Payment Schedule process : ", e, e);
      }
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return 1;
  }

}