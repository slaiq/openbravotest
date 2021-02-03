package sa.elm.ob.scm.ad_callouts;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ad_callouts.dao.BGWorkbenchDAO;
import sa.elm.ob.scm.ad_callouts.dao.PurOrderSummaryDAO;
import sa.elm.ob.scm.ad_process.InsuranceCertificate.InsuranceCertificateProcessDAO;
import sa.elm.ob.scm.event.dao.InsuranceCertificateEventDAO;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author qualian
 * 
 */
public class InsuranceCertificateWorkbenchCallout extends SimpleCallout {
  private static final Logger log = LoggerFactory
      .getLogger(InsuranceCertificateWorkbenchCallout.class);
  private static final long serialVersionUID = 1L;

  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    // declare the session variables
    String inpLastFieldChanged = info.vars.getStringParameter("inpLastFieldChanged");
    String strInsuranceCertId = info.vars.getStringParameter("inpescmInsuranceCertificateId");
    String inpstartdateh = info.vars.getStringParameter("inpstartdateh");
    String inpstartdategre = info.vars.getStringParameter("inpstartdateg");
    String strExpireDate = info.vars.getStringParameter("inpexpirydateh");
    String inpexpirydategre = info.vars.getStringParameter("inpexpirydateg");
    String inpclient = info.vars.getStringParameter("inpadClientId");
    String DocumentNo = info.vars.getStringParameter("inpcOrderId");
    String inpadOrgId = info.vars.getStringParameter("inpadOrgId");
    String inpescmIcExtensionId = info.vars.getStringParameter("inpescmIcExtensionId");
    String inpextperiodMonth = info.vars.getStringParameter("inpextperiodMonth");
    String strReqExpiryDate = info.vars.getStringParameter("inpreqexpiryDate");
    String periodType = info.vars.getStringParameter("inpextperiodType");
    String perioddayenddate = "";
    
    int years, mulyear = 0;
    int months = 0;
    int days = 0;
    String strtodayDate = null, maxBGExtensionDate = null;
    SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
    BigInteger inpexpireIn = BigInteger.ZERO;
    Order order = null;
    JSONObject result = new JSONObject();
    log.debug("inpLastFieldChanged:" + inpLastFieldChanged);
    try {
      OBContext.setAdminMode();
    // change values of document no dependent fields.
    if (inpLastFieldChanged.equals("inpcOrderId")) {
      order = InsuranceCertificateEventDAO.getOrder(DocumentNo);
      info.addResult("inpcBpartnerId", order.getBusinessPartner().getId());
      if (order.getEscmCurrency() != null)
        info.addResult("inpcCurrencyId", order.getEscmCurrency().getId());
      if (order.getEscmBidmgmt() != null)
      info.addResult("inpescmBidmgmtId", order.getEscmBidmgmt().getId());
      info.addResult("inpdocumentAmount", order.getGrandTotalAmount());
    }

    if (inpLastFieldChanged.equals("inpadOrgId")) {
      info.addResult("inpcontactname", BGWorkbenchDAO.getBgSpeciallistRole(info.vars.getUser(),
          info.vars.getRole(), inpclient, inpadOrgId));
    }

    // convert the bg startdate hijiri_date to gregorian_date
    if (inpLastFieldChanged.equals("inpstartdateh")) {
      info.addResult("inpstartdateg", UtilityDAO.convertToGregorian_tochar(inpstartdateh));
    }

    // convert the bg startdate gregorian_date to hijiri_date
    if (inpLastFieldChanged.equals("inpstartdateg")) {
      info.addResult("inpexpirydateh", UtilityDAO.convertToGregorian_tochar(inpstartdategre));
    }

    // convert the bg expiry startdate gregorian_date to hijiri_date
    if (inpLastFieldChanged.equals("inpexpirydateg")) {
      info.addResult("inpexpirydateh", UtilityDAO.convertToGregorian_tochar(inpexpirydategre));
    }

    // calculate the Expire in based on IC Expiry date and Extend Expiry Date
    if (inpLastFieldChanged.equals("inpexpirydateh")) {
      // convert the bg expiry date hijiri_date to gregorian_date
      strtodayDate = UtilityDAO.convertTohijriDate(dateYearFormat.format(new Date()));
      info.addResult("inpexpirydateg", UtilityDAO.convertToGregorian_tochar(strExpireDate));
      inpexpireIn = BGWorkbenchDAO
          .getExtendPeriodDays(strtodayDate, strExpireDate, inpclient, null);
      info.addResult("inpexpireIn", inpexpireIn);
    }

    // calculate the Requested Expiry Date based on IC Expiry date and Requested Extend Period Days
    if (inpLastFieldChanged.equals("inpextperiodMonth") || inpLastFieldChanged.equals("inpextperiodType")) {
      String reqExpirydate = null;
      maxBGExtensionDate = InsuranceCertificateProcessDAO.getmaximumbgextensiondate(
          strInsuranceCertId, inpescmIcExtensionId);
      if(maxBGExtensionDate==null)
        maxBGExtensionDate=strExpireDate;
      // Task No.7624
      if (StringUtils.isNotEmpty(inpextperiodMonth)
          && !inpextperiodMonth.equals("0")) {
        if (periodType.equals("MT")) {
          reqExpirydate = PurOrderSummaryDAO.getContractDurationMonth(inpextperiodMonth, periodType,
              maxBGExtensionDate, "", inpclient);

          info.addResult("inpreqexpiryDate", reqExpirydate);
          perioddayenddate = reqExpirydate;
        } else {
          //if (Integer.parseInt(inpextperiodMonth.replace(",", "")) > 0
          //  && Integer.parseInt(inpextperiodMonth.replace(",", "")) < 999) {
         reqExpirydate = BGWorkbenchDAO.getRequestedExpirydate(strExpireDate,
            inpextperiodMonth, inpclient, maxBGExtensionDate);
        info.addResult("inpreqexpiryDate", reqExpirydate);
        perioddayenddate = reqExpirydate;
        // }
        }
      }
    }
    
    // calculate the Requested Extend Period Days based on IC Expiry date and Requested Expiry Date
    if (inpLastFieldChanged.equals("inpreqexpiryDate")) {
      maxBGExtensionDate = InsuranceCertificateProcessDAO.getmaximumbgextensiondate(
          strInsuranceCertId, inpescmIcExtensionId);
      if (maxBGExtensionDate == null)
        maxBGExtensionDate = strExpireDate;
      // Task No.7624
      if (StringUtils.isNotEmpty(maxBGExtensionDate) && StringUtils.isNotEmpty(inpextperiodMonth)) {
                                                                                                  
        if (!perioddayenddate.equals(strReqExpiryDate)) {
          result = PurOrderSummaryDAO.getContractDurationdate(inpextperiodMonth, periodType,
              maxBGExtensionDate, strReqExpiryDate, inpclient);

          years = Integer.parseInt(result.getString("years"));
          months = Integer.parseInt(result.getString("months"));
          days = Integer.parseInt(result.getString("days"));
          log4j.debug("years" + years);
          log4j.debug("months" + months);
          log4j.debug("days" + days);
          if (years == 0 && months > 0 && days == 0) {
            info.addResult("inpextperiodType", "MT");
            info.addResult("inpextperiodMonth", months);
          } else if (months == 0 && years > 0 && days == 0) {
            mulyear = years * 12;
            info.addResult("inpextperiodType", "MT");
            info.addResult("inpextperiodMonth", mulyear);
          } else if (months > 0 && years > 0 && days == 0) {
            months = years * 12 + months;
            info.addResult("inpextperiodType", "MT");
            info.addResult("inpextperiodMonth", months);
          } else if (months == 0 && years == 0 && days == 0) {
            info.addResult("inpextperiodType", "D");
            info.addResult("inpextperiodMonth", "1");
          } else if ((months >= 0 && years == 0 && (days >= 1 || days <= 1))
              || (months > 0 && years > 0 && days > 0) || (months == 0 && years > 0 && days > 0)) {
            String sql = "select count(distinct hijri_date) as total from eut_hijri_dates  where hijri_date > ? and hijri_date <= ? ";
            SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(sql);
            query.setParameter(0, maxBGExtensionDate.split("-")[2]
                + maxBGExtensionDate.split("-")[1] + maxBGExtensionDate.split("-")[0]);
            query.setParameter(1, strReqExpiryDate.split("-")[2] + strReqExpiryDate.split("-")[1]
                + strReqExpiryDate.split("-")[0]);
            if (query.list().size() > 0) {
              Object row = (Object) query.list().get(0);
              BigInteger total = (BigInteger) row;
              info.addResult("inpextperiodType", "DT");
              info.addResult("inpextperiodMonth", total);
            }

          }
        }

      } // else {
//      BigInteger reqExpirydays = BGWorkbenchDAO.getExtendPeriodDays(strExpireDate,
//          strReqExpiryDate, inpclient, maxBGExtensionDate);
//      info.addResult("inpextperiodMonth", reqExpirydays);
      // }
      
    }
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("Exception in InsuranceCertificateWorkbenchCallout:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
