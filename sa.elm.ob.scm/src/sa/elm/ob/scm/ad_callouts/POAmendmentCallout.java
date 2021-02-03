package sa.elm.ob.scm.ad_callouts;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;

import sa.elm.ob.scm.ad_callouts.dao.PurOrderSummaryDAO;

/**
 * 
 * @author qualian
 *
 */

@SuppressWarnings("serial")
public class POAmendmentCallout extends SimpleCallout {

  /**
   * Callout for PO Amendment tab in Purchase Order and Contract Summary
   */

  @SuppressWarnings("resource")
  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = info.vars;
    String inprequestdatehiriji = vars.getStringParameter("inprequestdatehiriji");
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpOrderId = vars.getStringParameter("inpcOrderId");
    String inpAction = vars.getStringParameter("inpaction");
    String updatedContractAmount = vars.getStringParameter("inpupdatedContractAmount");
    // String strPeriodInMonth = vars.getStringParameter("inpperiodInMonth");
    // String strPeriodInDays = vars.getStringParameter("inpperiodInDays");
    String inpclient = vars.getStringParameter("inpadClientId");
    String contractduration = vars.getStringParameter("inpescmExtcontractduration");
    String periodtype = vars.getStringParameter("inpescmExtperiodtype");
    String inpstartdate = vars.getStringParameter("inpextContractStartDate");
    String inpenddate = vars.getStringParameter("inpextContractEndDate");
    // SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
    String perioddayenddate = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject result = new JSONObject();
    int years, mulyear = 0;
    int months = 0;
    int days = 0;
    Connection conn = OBDal.getInstance().getConnection();
    try {

      if (inpLastFieldChanged.equals("inprequestdatehiriji")) {
        st = OBDal.getInstance().getConnection()
            .prepareStatement("select to_char(eut_convertto_gregorian('" + inprequestdatehiriji
                + "')) as eut_convertto_gregorian ");
        rs = st.executeQuery();

        if (rs.next()) {
          info.addResult("inprequestdategreg", rs.getString("eut_convertto_gregorian"));

        }
        rs.close();
      }
      if (inpLastFieldChanged.equals("inpupdatedContractAmount")) {
        Order objOrder = OBDal.getInstance().get(Order.class, inpOrderId);

        if (inpAction.equals("IA") || inpAction.equals("RA")) {
          BigDecimal differenceAmt = (new BigDecimal(updatedContractAmount.replace(",", ""))
              .subtract(objOrder.getGrandTotalAmount()));
          BigDecimal percentage = (differenceAmt.abs().divide(objOrder.getGrandTotalAmount())
              .multiply(new BigDecimal(100)));
          info.addResult("inpupdatedPercentage", percentage);
          info.addResult("inppreviousContractAmount", objOrder.getGrandTotalAmount());
          info.addResult("inpnewContractAmount",
              new BigDecimal(updatedContractAmount.replace(",", "")));
          info.addResult("inpperiodInMonth", null);
          info.addResult("inpperiodInMonth", null);
          info.addResult("inpperiodInDays", null);
          info.addResult("inpextContractStartDate", null);
          info.addResult("inpextContractEndDate", null);

        }
      } else if (inpAction.equals("ED") || inpAction.equals("RD")) {
        // Order objOrder = OBDal.getInstance().get(Order.class, inpOrderId);
        info.addResult("inpupdatedPercentage", null);
        info.addResult("inppreviousContractAmount", null);
        info.addResult("inpnewContractAmount", null);
        // if the periodtype as month or day then calculate the contract enddate
        if (inpLastFieldChanged.equals("inpescmExtcontractduration")
            || inpLastFieldChanged.equals("inpescmExtperiodtype")
            || inpLastFieldChanged.equals("inpextContractStartDate")) {

          if (periodtype.equals("MT")) {
            String Contractdate = PurOrderSummaryDAO.getContractDurationMonth(contractduration,
                periodtype, inpstartdate, inpenddate, inpclient);

            info.addResult("inpextContractEndDate", Contractdate);
          } else {
            String Contractday = PurOrderSummaryDAO.getContractDurationday(contractduration,
                periodtype, inpstartdate, inpenddate, inpclient);
            info.addResult("inpextContractEndDate", Contractday);
            perioddayenddate = Contractday;
          }
        }

        // To get the contractduration for corresponding contract enddate
        if (inpLastFieldChanged.equals("inpextContractEndDate")) {
          if (!perioddayenddate.equals(inpenddate)) {
            result = PurOrderSummaryDAO.getContractDurationdate(contractduration, periodtype,
                inpstartdate, inpenddate, inpclient);

            years = Integer.parseInt(result.getString("years"));
            months = Integer.parseInt(result.getString("months"));
            days = Integer.parseInt(result.getString("days"));
            log4j.debug("years" + years);
            log4j.debug("months" + months);
            log4j.debug("days" + days);
            if (years == 0 && months > 0 && days == 0) {
              info.addResult("inpescmExtperiodtype", "MT");
              info.addResult("inpescmExtcontractduration", months);
            } else if (months == 0 && years > 0 && days == 0) {
              mulyear = years * 12;
              info.addResult("inpescmExtperiodtype", "MT");
              info.addResult("inpescmExtcontractduration", mulyear);
            } else if (months > 0 && years > 0 && days == 0) {
              months = years * 12 + months;
              info.addResult("inpescmExtperiodtype", "MT");
              info.addResult("inpescmExtcontractduration", months);
            } else if (months == 0 && years == 0 && days == 0) {
              info.addResult("inpescmExtperiodtype", "D");
              info.addResult("inpescmExtcontractduration", "1");
            } else if ((months >= 0 && years == 0 && (days >= 1 || days <= 1))
                || (months > 0 && years > 0 && days > 0)) {

              st = conn.prepareStatement(
                  " select count(distinct hijri_date) as total from eut_hijri_dates  where hijri_date > ? and hijri_date <= ?");
              st.setString(1, inpstartdate.split("-")[2] + inpstartdate.split("-")[1]
                  + inpstartdate.split("-")[0]);
              st.setString(2,
                  inpenddate.split("-")[2] + inpenddate.split("-")[1] + inpenddate.split("-")[0]);
              // st.setString(3, inpclient);
              rs = st.executeQuery();
              if (rs.next()) {
                info.addResult("inpescmExtperiodtype", "DT");
                info.addResult("inpescmExtcontractduration", (rs.getInt("total") + 1));
              }
            }
          }
        }
        // if (inpLastFieldChanged.equals("inpperiodInMonth")) {
        // info.addResult("inpperiodInDays", null);
        // // if (objOrder.getEscmOldOrder() != null
        // // && objOrder.getEscmOldOrder().getEscmContractstartdate() != null) {
        // if (objOrder.getEscmContractstartdate() != null) {
        // String previsoContractStartDate = UtilityDAO
        // .convertTohijriDate(dateYearFormat.format(objOrder.getEscmContractstartdate()));
        // String ContractStartdate = PurOrderSummaryDAO.getContractDurationMonth(strPeriodInMonth,
        // "MT", previsoContractStartDate, "", vars.getClient());
        // info.addResult("inpextContractStartDate", ContractStartdate);
        // }
        // // else if (objOrder.getEscmOldOrder() != null
        // // && objOrder.getEscmOldOrder().getEscmContractenddate() != null) {
        // else if (objOrder.getEscmContractenddate() != null) {
        // String previsoContractEndDate = UtilityDAO
        // .convertTohijriDate(dateYearFormat.format(objOrder.getEscmContractenddate()));
        // String ContractEnddate = PurOrderSummaryDAO.getContractDurationMonth(strPeriodInMonth,
        // "MT", previsoContractEndDate, "", vars.getClient());
        // info.addResult("inpextContractEndDate", ContractEnddate);
        // }
        // } else if (inpLastFieldChanged.equals("inpperiodInDays")) {
        // info.addResult("inpperiodInMonth", null);
        // if (objOrder.getEscmContractstartdate() != null) {
        // String previsoContractStartDate = UtilityDAO
        // .convertTohijriDate(dateYearFormat.format(objOrder.getEscmContractstartdate()));
        // String ContractStartdate = PurOrderSummaryDAO.getContractDurationday(strPeriodInDays,
        // "DT", previsoContractStartDate, "", vars.getClient());
        // info.addResult("inpextContractStartDate", ContractStartdate);
        // } else if (objOrder.getEscmContractenddate() != null) {
        // String previsoContractEndDate = UtilityDAO
        // .convertTohijriDate(dateYearFormat.format(objOrder.getEscmContractenddate()));
        // String ContractEnddate = PurOrderSummaryDAO.getContractDurationday(strPeriodInDays,
        // "DT", previsoContractEndDate, "", vars.getClient());
        // info.addResult("inpextContractEndDate", ContractEnddate);
        // }
        // }

      }
    } catch (Exception e) {
      log4j.error("Exception in POAmendmentCallout  :", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      try {
        // close connection
        if (rs != null) {
          rs.close();
        }
        if (st != null) {
          st.close();
        }
      } catch (Exception e) {

      }
      OBContext.restorePreviousMode();
    }

  }
}
