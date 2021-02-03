package sa.elm.ob.scm.actionHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.actionHandler.dao.POActionChangeDAO;
import sa.elm.ob.scm.ad_callouts.dao.PurOrderSummaryDAO;
import sa.elm.ob.utility.util.DateUtils;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author Kousalya.J-28-05-2019
 *
 */
public class POChangeActionDurationCalc extends BaseActionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(POChangeActionDurationCalc.class);

  /**
   * This class is used to handle PO Change Action
   */
  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject json = new JSONObject();
    JSONObject result = new JSONObject();

    PreparedStatement st = null;
    ResultSet rs = null;
    Connection conn = OBDal.getInstance().getConnection();

    try {
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();

      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      if (jsonRequest.has("action")) {
        String endDate = "", periodDayEndDate = "";
        String onHoldFromDate = null, periodType = null, duration = null, onHoldToDate = null;
        int years, mulyear = 0;
        int months = 0;
        int days = 0;
        if (jsonRequest.getString("action").equals("calcDuration")) {
          if (jsonRequest.has("onHoldFromDate")) {
            onHoldFromDate = jsonRequest.getString("onHoldFromDate");
          }
          if (jsonRequest.has("onHoldToDate")) {
            onHoldToDate = jsonRequest.getString("onHoldToDate");
          }
          if (onHoldFromDate != null && !onHoldFromDate.equals("null"))
            onHoldFromDate = onHoldFromDate.split("-")[2] + "-" + onHoldFromDate.split("-")[1] + "-"
                + onHoldFromDate.split("-")[0];
          if (onHoldToDate != null && !onHoldToDate.equals("null"))
            onHoldToDate = onHoldToDate.split("-")[2] + "-" + onHoldToDate.split("-")[1] + "-"
                + onHoldToDate.split("-")[0];

          // To get the contractduration for corresponding contract enddate
          if (onHoldToDate != null && !onHoldToDate.equals("null")
              && !periodDayEndDate.equals(onHoldToDate)) {
            result = PurOrderSummaryDAO.getContractDurationdate(duration, periodType,
                onHoldFromDate, onHoldToDate, vars.getClient());

            years = Integer.parseInt(result.getString("years"));
            months = Integer.parseInt(result.getString("months"));
            days = Integer.parseInt(result.getString("days"));
            if (years == 0 && months > 0 && days == 0) {
              json.put("PeriodType", "MT");
              json.put("Duration", months);
            } else if (months == 0 && years > 0 && days == 0) {
              mulyear = years * 12;
              json.put("PeriodType", "MT");
              json.put("Duration", mulyear);
            } else if (months > 0 && years > 0 && days == 0) {
              months = years * 12 + months;
              json.put("PeriodType", "MT");
              json.put("Duration", months);
            } else if (months == 0 && years == 0 && days == 0) {
              json.put("PeriodType", "D");
              json.put("Duration", "1");
            } else if ((months >= 0 && years == 0 && (days >= 1 || days <= 1))
                || (months > 0 && years > 0 && days > 0)) {
              st = conn.prepareStatement(
                  " select count(distinct hijri_date) as total from eut_hijri_dates  where hijri_date > ? and hijri_date <= ?");
              st.setString(1, onHoldFromDate.split("-")[2] + onHoldFromDate.split("-")[1]
                  + onHoldFromDate.split("-")[0]);
              st.setString(2, onHoldToDate.split("-")[2] + onHoldToDate.split("-")[1]
                  + onHoldToDate.split("-")[0]);
              rs = st.executeQuery();
              if (rs.next()) {
                json.put("PeriodType", "DT");
                json.put("Duration", (rs.getInt("total") + 1));
              }
            }
          }
        } else if (jsonRequest.getString("action").equals("calcOnHoldToDate")) {
          if (jsonRequest.has("onHoldFromDate")) {
            onHoldFromDate = jsonRequest.getString("onHoldFromDate");
          }
          if (jsonRequest.has("periodType")) {
            periodType = jsonRequest.getString("periodType");
          }
          if (jsonRequest.has("duration")) {
            duration = jsonRequest.getString("duration");
          }
          if (onHoldFromDate != null && !onHoldFromDate.equals("null"))
            onHoldFromDate = onHoldFromDate.split("-")[2] + "-" + onHoldFromDate.split("-")[1] + "-"
                + onHoldFromDate.split("-")[0];

          if (onHoldFromDate != null && !onHoldFromDate.equals("null") && periodType != null
              && duration != null && !duration.equals("") && !duration.equals("null")) {
            if (periodType.equals("MT")) {
              endDate = PurOrderSummaryDAO.getContractDurationMonth(duration, periodType,
                  onHoldFromDate, onHoldToDate, vars.getClient());
              json.put("OnHoldEndDate", endDate);
            } else {
              endDate = PurOrderSummaryDAO.getContractDurationday(duration, periodType,
                  onHoldFromDate, onHoldToDate, vars.getClient());
              json.put("OnHoldEndDate", endDate);
              periodDayEndDate = endDate;
            }
          }
        } else if (jsonRequest.getString("action").equals("calcFromDate")) {
          endDate = "";
          Calendar extendHoldDate = null;
          String orderId = null;
          SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
          if (jsonRequest.has("orderId")) {
            orderId = jsonRequest.getString("orderId");
            Date onHoldEndDate = POActionChangeDAO.getPOAmendmentOnHoldEndDate(orderId);
            extendHoldDate = DateUtils.addDate(onHoldEndDate, 1);

            if (onHoldEndDate != null) {
              endDate = UtilityDAO.convertTohijriDate(sdf.format(extendHoldDate.getTime()));
              json.put("OnHoldFromDate", endDate);
            }
          }
        }
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in POChangeActionDurationCalc :", e);
      }
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      try {
        if (st != null) {
          st.close();
        }
        if (rs != null) {
          rs.close();
        }
      } catch (Exception e) {
        LOG.error("Exception while closing the statement in POChangeActionDurationCalc ", e);
      }
      OBContext.restorePreviousMode();
    }
    return json;
  }
}
