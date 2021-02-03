package sa.elm.ob.scm.actionHandler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.hcm.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Poongodi on 19/02/2019
 */

public class POReceiptAddLinesAmount extends BaseActionHandler {

  private static Logger log4j = Logger.getLogger(POReceiptAddLinesAmount.class);

  protected JSONObject execute(Map<String, Object> parameters, String data) {
    JSONObject json = new JSONObject();
    try {

      OBContext.setAdminMode();

      final JSONObject jsonData = new JSONObject(data);

      if (jsonData.has("action")) {
        final String action = jsonData.getString("action");
        if ("setexeStartDateGre".equals(action)) {
          DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
          String exeStartdateGre = "";
          String exeEnddateGre = "";
          if (jsonData.getString("recordId").matches("\\d{2}-\\d{2}-\\d{4}")) {
            exeStartdateGre = jsonData.getString("recordId");
          } else {
            // String to Date
            Date exeStartdateGreDate = new SimpleDateFormat("yyyy-MM-dd")
                .parse(jsonData.getString("recordId"));
            exeStartdateGre = df.format(exeStartdateGreDate);
          }
          if (jsonData.getString("endDateH").matches("\\d{2}-\\d{2}-\\d{4}")) {
            exeEnddateGre = jsonData.getString("endDateH");
          } else {
            // String to Date
            Date exeEnddateGreDate = new SimpleDateFormat("yyyy-MM-dd")
                .parse(jsonData.getString("endDateH"));
            exeEnddateGre = df.format(exeEnddateGreDate);
          }

          // calculate executedDays
          int noofday = Utility.calculatetheDays(exeStartdateGre, exeEnddateGre);
          // calculate exeStartDateGre based on exeStartDatehijiri
          exeStartdateGre = UtilityDAO.convertToGregorian(exeStartdateGre);
          Date convertexeStartdate = new SimpleDateFormat("yyyy-MM-dd").parse(exeStartdateGre);
          String convertexeStartdates = df.format(convertexeStartdate);

          json.put("exeStartDateGre", convertexeStartdates);
          json.put("executedDays", noofday);
        }
        if ("setexeEndDateGre".equals(action)) {
          String exeEnddateGre = "";
          String exeStartdateGre = "";

          DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
          if (jsonData.getString("calExEndDateH").matches("\\d{2}-\\d{2}-\\d{4}")) {
            exeEnddateGre = jsonData.getString("calExEndDateH");
          } else {
            // String to Date
            Date exeEnddateGreDate = new SimpleDateFormat("yyyy-MM-dd")
                .parse(jsonData.getString("calExEndDateH"));
            // dd-MM-yyyy
            exeEnddateGre = df.format(exeEnddateGreDate);
          }
          if (jsonData.getString("Exestartdateh").matches("\\d{2}-\\d{2}-\\d{4}")) {
            exeStartdateGre = jsonData.getString("Exestartdateh");
          } else {
            // String to Date
            Date Exestartdateh = new SimpleDateFormat("yyyy-MM-dd")
                .parse(jsonData.getString("Exestartdateh"));
            // dd-MM-yyyy
            exeStartdateGre = df.format(Exestartdateh);
          }

          Date needbyDate = new SimpleDateFormat("yyyy-MM-dd")
              .parse(jsonData.getString("needbyDate"));
          String needyDate_String = df.format(needbyDate);
          // calculate executedDays
          int noofday = Utility.calculatetheDays(exeStartdateGre, exeEnddateGre);
          // calculate needbydate
          int Contractdelaydays = Utility.calculatetheDays(needyDate_String, exeEnddateGre);

          // calculate exeEndDateGre based on exeEndDatehijiri
          exeEnddateGre = UtilityDAO.convertToGregorian(exeEnddateGre);
          Date convertexeEnddate = new SimpleDateFormat("yyyy-MM-dd").parse(exeEnddateGre);
          String convertexeEnddates = df.format(convertexeEnddate);

          json.put("exeEndDateGre", convertexeEnddates);
          json.put("executedDays", noofday);
          json.put("Contractdelaydays", Contractdelaydays);
        }

        if ("setexeStartDateH".equals(action)) {
          final String ex = jsonData.getString("Exestartdategre");
          // DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
          Date exestartdategre_date = new SimpleDateFormat("dd-MM-yyyy").parse(ex);
          // Date exeEnddateGreDate = new SimpleDateFormat("yyyy-MM-dd")
          // .parse(jsonData.getString("Exeenddateh"));
          String exestartdategre = new SimpleDateFormat("yyyy-MM-dd").format(exestartdategre_date);
          // String exeEnddateGre = df.format(exeEnddateGreDate);
          // // calculate executedDays
          // int noofday = Utility.calculatetheDays(exestartdategre, exeEnddateGre);
          String convert_hijiriDate = UtilityDAO.convertToHijriDate(exestartdategre);
          Date convertexeStartDate = new SimpleDateFormat("dd-MM-yyyy").parse(convert_hijiriDate);
          String convertexeStartDateH = new SimpleDateFormat("dd-MM-yyyy")
              .format(convertexeStartDate);
          json.put("convertexeStartDateH", convertexeStartDateH);
          // json.put("executedDays", noofday);
        }
        if ("setexeEndDateH".equals(action)) {
          final String ex = jsonData.getString("Exeenddategre");
          Date exeEnddategre_date = new SimpleDateFormat("dd-MM-yyyy").parse(ex);
          String exeEnddategre = new SimpleDateFormat("yyyy-MM-dd").format(exeEnddategre_date);
          String convert_hijiriDate = UtilityDAO.convertToHijriDate(exeEnddategre);
          Date convertexeEndDate = new SimpleDateFormat("dd-MM-yyyy").parse(convert_hijiriDate);
          String convertexeEndDateH = new SimpleDateFormat("dd-MM-yyyy").format(convertexeEndDate);
          json.put("convertexeEndDateH", convertexeEndDateH);
        }

      }

    } catch (

    Exception e) {
      log4j.error("Exception in POReceiptAddLinesAmount :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
    return json;
  }
}
