package sa.elm.ob.utility.util;

import java.math.BigDecimal;
import java.sql.Connection;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.service.db.DalConnectionProvider;

import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.utility.EUTDeflookupsTypeLn;
import sa.elm.ob.utility.EutDocappDelegateln;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;

//import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;

/**
 * @auther Qualian
 */
public class Utility {
  private static final Logger log4j = Logger.getLogger(Utility.class);

  /**
   * Get Editable Org
   * 
   * @param vars
   * @return Editable Org List
   */
  public static String getEditableOrg(VariablesSecureApp vars) {
    return org.openbravo.erpCommon.utility.Utility.getContext(new DalConnectionProvider(), vars,
        "#AccessibleOrgTree", "");
  }

  /**
   * Get Accessible Org
   * 
   * @param vars
   * @return Accessible Org List
   */
  public static String getAccessibleOrg(VariablesSecureApp vars) {
    return org.openbravo.erpCommon.utility.Utility.getContext(new DalConnectionProvider(), vars,
        "#User_Org", "");
  }

  /**
   * Get Accessible Org By List<UtilityVO>
   * 
   * @param vars
   * @return Accessible Org by List<UtilityVO>
   */
  public static List<UtilityVO> getAccessibleOrgByList(VariablesSecureApp vars) {
    return UtilityDAO.getAccessibleOrgByList(vars);
  }

  /**
   * Getting Child Organization
   * 
   * @param clientId
   * @param orgId
   * @param *
   *          organization
   * @return Organization List wrapped by Single Quotes, Separated by Comma
   */
  public static List<UtilityVO> getOrganizationList(String clientId, String orgId,
      boolean include0) {
    return UtilityDAO.getOrganizationList(clientId, orgId, include0);
  }

  /**
   * 
   * @param vars
   * @return Organization with type ='ORG'
   */
  public static String getOrgTypeOrganizationList(VariablesSecureApp vars) {
    return UtilityDAO.getOrgTypeOrganizationList(vars);
  }

  /**
   * Getting Organization Tree
   * 
   * @param clientId
   * @param orgId
   * @return Organization List wrapped by Single Quotes, Separated by Comma
   */
  public static String getOrganizationTree(String clientId, String orgId) {
    String orgList = "";
    try {
      orgList = Utility.getParentOrg(clientId, orgId) + "," + Utility.getChildOrg(clientId, orgId);
    } catch (final Exception e) {
      log4j.error("Exception in getOrganizationTree():", e);
      return "'" + orgId + "'";
    }
    return orgList;
  }

  /**
   * Getting Child Organization
   * 
   * @param clientId
   * @param orgId
   * @return Organization List wrapped by Single Quotes, Separated by Comma
   */
  public static String getChildOrg(String clientId, String orgId) {
    return UtilityDAO.getChildOrg(clientId, orgId);
  }

  /**
   * Getting Role having access Organization
   * 
   * 
   * @param clientId
   * @param orgId
   * @return Organization List wrapped by Single Quotes, Separated by Comma
   */
  public static String getRoleaccOrg(String clientId, String roleId) {
    return UtilityDAO.getRoleaccOrg(clientId, roleId);
  }

  /**
   * Getting Child Organization By HashSet<String>
   * 
   * @param clientId
   * @param orgId
   * @return Organization List by HashSet<String>
   */
  public static HashSet<String> getChildOrgBySet(String clientId, String orgId) {
    return UtilityDAO.getChildOrgBySet(clientId, orgId);
  }

  /**
   * Getting Parent Organization
   * 
   * @param clientId
   * @param orgId
   * @return Organization List wrapped by Single Quotes, Separated by Comma
   */
  public static String getParentOrg(String clientId, String orgId) {
    return UtilityDAO.getParentOrg(clientId, orgId);
  }

  /**
   * Get Document Sequence
   * 
   * @param clientId
   * @param seqName
   * @param update
   * @return encoded String
   */
  public static String getSequenceNo(Connection conn, String clientId, String seqName,
      boolean update) {
    try {
      return UtilityDAO.getSequenceNo(conn, clientId, seqName, update);
    } catch (final Exception e) {
      log4j.error("Exception in getSequenceNo() ", e);
      return "";
    }
  }

  /**
   * Check Document Sequence exists
   * 
   * @param clientId
   * @param seqName
   * @return boolean
   */
  public static boolean checkDocumentSequence(String clientId, String seqName) {
    return UtilityDAO.checkDocumentSequence(clientId, seqName);
  }

  /**
   * Checking Role - Form Access
   * 
   * @param clientId
   * @param roleId
   * @param formId
   * @return boolean based on Role - Form Access
   */
  public static boolean checkFormAccess(String clientId, String roleId, String formId) {
    return UtilityDAO.checkFormAccess(clientId, roleId, formId);
  }

  /**
   * Check Access to Window
   * 
   * @param clientId
   * @param orgId
   * @param roleID
   * @param documentType
   * @return boolean
   */
  public static boolean haveAccesstoWindow(String pClientId, String pOrgId, String pRoleId,
      String pUserId, String pDocumentType) {
    return NextRoleByRule.haveAccesstoWindow(pClientId, pOrgId, pRoleId, pUserId, pDocumentType);
  }

  /**
   * Escape String for HTML
   * 
   * @param value
   * @return escaped String
   */
  public static String escapeHTML(Object value) {
    try {
      return UtilityFn.escapeHTML(value.toString());
    } catch (Exception e) {
      return "";
    }
  }

  /**
   * Escape String for HTML
   * 
   * @param value
   * @return escaped String
   */
  public static String unescapeHTML(Object value) {
    try {
      return UtilityFn.unescapeHTML(value.toString());
    } catch (Exception e) {
      return "";
    }
  }

  /**
   * Encode String for Treegrid
   * 
   * @param value
   * @return encoded String
   */
  public static String escapeTreeGridHTML(Object value) {
    try {
      return UtilityFn.escapeTreeGridHTML(value.toString());
    } catch (Exception e) {
      return "";
    }
  }

  /**
   * Escape String for HTML
   * 
   * @param value
   * @return escaped String
   */
  public static String escapeQuote(Object value) {
    try {
      return UtilityFn.escapeQuote(value);
    } catch (Exception e) {
      return "";
    }
  }

  /**
   * * createTooltipEle
   * 
   * @param value
   * @param len
   * @return encoded String
   */
  public static String createTooltipEle(Object value, int len) {
    try {
      return UtilityFn.createTooltipEle(value, len);
    } catch (Exception e) {
      return "";
    }
  }

  /**
   * Changes Null to Zero
   * 
   * @param value
   * @return encoded String
   */
  public static String nullToZero(Object value) {
    return UtilityFn.nullToZero(value);
  }

  /**
   * Throw EventHandler Exception
   * 
   * @param value
   * @return throw OBException
   */
  public static String throwEventHandlerException(String value) {
    return UtilityFn.throwEventHandlerException(value);
  }

  /**
   * Convert Message from AD_Message
   * 
   * @param value
   * @param lang
   * @return Message
   */
  public static String getADMessage(String value, String lang) {
    return UtilityDAO.getADMessage(value, lang);
  }

  /**
   * Check AD message with Code
   * 
   * @param conn
   * @param value
   * @param lang
   * @return String
   */
  public static String parseADMessage(String value, String lang) {
    return UtilityFn.parseADMessage(value, lang);
  }

  /**
   * Escape String for Query
   * 
   * @param value
   * @return escaped String
   */
  public static String escapeQrySearchStr(Object value) {
    return UtilityFn.escapeQrySearchStr(value);
  }

  /**
   * Escape String for Query
   * 
   * @param value
   * @return escaped String
   */
  public static String nullToEmpty(Object value) {
    return UtilityFn.nullToEmpty(value);
  }

  /**
   * Convert Number Format
   * 
   * @param vars
   * @param type
   * @param obj
   * @return formatted string
   */
  public static String numberFormat_QuantityEdition = "QE";
  public static String numberFormat_QuantityRelation = "QR";
  public static String numberFormat_PriceEdition = "PE";
  public static String numberFormat_PriceRelation = "PR";
  public static String numberFormat_IntergerEdition = "IE";
  public static String numberFormat_IntegerRelation = "IR";

  public static String getNumberFormat(VariablesSecureApp vars, String type, Object obj) {
    return UtilityFn.getNumberFormat(vars, type, obj);
  }

  /* ----------------------------------- Date function ----------------------------------- */

  public static final String TIME24HOURS_PATTERN = "([01]?[0-9]|2[0-3]):[0-5][0-9]:00";
  public static final String TIME24HOURSMIN_PATTERN = "([01]?[0-9]|2[0-3]):[0-5][0-9]";
  public static final Pattern pattern24HOURS = Pattern.compile(TIME24HOURS_PATTERN);
  public static final Pattern pattern24HOURSMIN = Pattern.compile(TIME24HOURSMIN_PATTERN);
  public static final DecimalFormat timeDecimalFormat = new DecimalFormat("00");

  /*
   * Initialized in UtilityCallout
   */
  public static String strDateFormatJS = null;
  public static String strDateTimeFormatJS = null;
  public static String strDateFormat = "dd-MM-yyyy";
  public static String strDateTimeFormat = "dd-MM-yyyy HH:mm:ss";
  public static String strDateFormatSQL = null;
  public static String strDateTimeFormatSQL = null;
  public static String strTimeFormat = null;
  public static String strYearFormat = "yyyy-MM-dd";

  public static SimpleDateFormat dateFormatJS = null;
  public static SimpleDateFormat dateTimeFormatJS = null;
  public static SimpleDateFormat dateFormat = new SimpleDateFormat(strDateFormat);
  public static SimpleDateFormat dateTimeFormat = new SimpleDateFormat(strDateTimeFormat);
  public static SimpleDateFormat YearFormat = new SimpleDateFormat(strYearFormat);
  public static SimpleDateFormat timeFormat = null;

  /*
   * Used in Initial BG validation
   */
  public static String twoDecimalCheck = "^[0-9]+(.([0-9]{1,2})?)?$";
  public static String zeroToTwoDecimalCheck = "^(([1](.[0-9]{1,2})?)|([0](.[1-9]{1,2}))|([2](.[0]{1,2})?)|([0](.[0][1-9])))$";

  public static String emailFormat = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

  /**
   * Parse Date
   * 
   * @param date
   * @return Calendar
   */
  public static Calendar parseDate(Object date) {
    return DateUtils.parseDate(date);
  }

  /**
   * Format Date
   * 
   * @param date
   * @return Formatted Date in String
   */
  public static String formatDate(Object date) {
    return formatDate(date, null);
  }

  /**
   * Format Date
   * 
   * @param date
   * @param formatter
   * @return Formatted Date in String
   */
  public static String formatDate(Object date, Object formatter) {
    return DateUtils.formatDate(date, formatter);
  }

  /**
   * Calculates duration between two date
   * 
   * @param startDate
   * @param endDate
   * @return Duration between dates in String
   */
  public static String calculateDuration(Object startDate, Object endDate) {
    return DateUtils.calculateDuration(startDate, endDate);
  }

  /**
   * Add duration with date
   * 
   * @param date
   * @param duration
   * @return Date in String
   */
  public static Calendar addDate(Object date, int duration) {
    return DateUtils.addDate(date, duration);
  }

  /**
   * Convert Gregorian Date to Hijri Date
   * 
   * @param vars
   * @param date
   * @return Date in String
   */
  public static Calendar convertGregoriantoHijri(Object gregorianDate) {
    return DateUtils.convertGregoriantoHijri(gregorianDate);
  }

  /**
   * Convert Hijri Date to Gregorian Date
   * 
   * @param vars
   * @param date
   * @return Date in String
   */
  public static Calendar convertHijritoGregorian(Object hijriDate) {
    return DateUtils.convertHijritoGregorian(hijriDate);
  }

  /**
   * Getting HijriDateAdjustment
   * 
   * @param vars
   * @return Days in Integer
   */
  public static int getHijriDateAdjustment() {
    return UtilityDAO.getHijriDateAdjustment();
  }

  /**
   * Getting Calculate End Date
   * 
   * @param vars
   * @param endDate
   * @param duration
   * @return Date in String
   */
  public static String calculateEndDate(Object startDate, String duration) {
    return DateUtils.calculateEndDate(startDate, duration);
  }

  /**
   * Validate 24 Hours Format with minute and second
   * 
   * @param time
   * @return boolean
   */
  public static boolean validate24Hours(final String time) {
    return DateUtils.validate24Hours(time);
  }

  /**
   * Convert Time to Minutes
   * 
   * @param value
   * @return Minutes
   */
  public static long convertTimetoMinutes(Object value, boolean format24Hrs) {
    return DateUtils.convertTimetoMinutes(value, format24Hrs);
  }

  /**
   * Remove Time from Util Date
   * 
   * @param date
   * @return Date
   */
  public static Calendar removeTimeFromDate(final Date date) {
    return DateUtils.removeTimeFromDate(date);
  }

  /**
   * Remove Time from Util Date
   * 
   * @param date
   * @return Date
   */
  public static String convertMinutetoTimeString(final Object obj) {
    return DateUtils.convertMinutetoTimeString(obj);
  }

  public static <T extends BaseOBObject> T getObject(Class<T> t, String strId) {
    return UtilityDAO.getObject(t, strId);
  }

  public static <T extends BaseOBObject> T getEntity(Class<T> t) {
    return UtilityDAO.getEntity(t);
  }

  public static long getLineNo(String tableName, String recordId, String colName,
      String whereClause) {
    return UtilityDAO.getLineNo(tableName, recordId, colName, whereClause);
  }

  public static boolean isNumber(String numberString) {
    return UtilityDAO.isNumber(numberString);
  }

  public static String convertToHijriDate(String gregDate) {
    return UtilityDAO.convertToHijriDate(gregDate);
  }

  public static String convertTohijriDate(String gregDate) {
    return UtilityDAO.convertTohijriDate(gregDate);
  }

  public static String convertToHijriTimestamp(String gregDate) {
    return UtilityDAO.convertToHijriTimestamp(gregDate);
  }

  public static String convertToGregorian(String hijriDate) {
    return UtilityDAO.convertToGregorian(hijriDate);
  }

  public static Boolean HijriDateValidion(String inpDate) {
    return UtilityDAO.Checkhijridate(inpDate);
  }

  public static String MaxHijriDate() {
    return UtilityDAO.HijriMaxDate();
  }

  public static String MinHijriDate() {
    return UtilityDAO.HijriMinDate();
  }

  public static String getValidCombination(JSONObject dimensions) {
    return UtilityDAO.getValidCombination(dimensions);
  }

  public static String getAccountingSchema(String strOrgId) {
    return UtilityDAO.getAccountingSchema(strOrgId);
  }

  public static String getAccountingConfig(String strOrgId, String strFinAccountId) {
    return UtilityDAO.getAccountingConfig(strOrgId, strFinAccountId);
  }

  public static String getGeneralSequence(String AccountDate, String Type, String CalendarId,
      String OrgId, boolean action) {
    return UtilityDAO.getGeneralSequence(AccountDate, Type, CalendarId, OrgId, action);
  }

  public static String getCalendar(String strOrgId) {
    return UtilityDAO.getCalendar(strOrgId);
  }

  public static String getPeriod(String strDate, String strOrgId) {
    return UtilityDAO.getPeriod(strDate, strOrgId);
  }

  /**
   * This method only for Purchase Requisition Alert Process With Preference Configuration
   * 
   * @param property
   *          preference
   * @param clientId
   * @param description
   * @param status
   * @return True --Alert Created, False --Error
   */
  public static Boolean alertInsertionPreference(String DocumentId, String DocumentNo,
      String property, String clientId, String description, String status) {
    return UtilityDAO.alertInsertionPreference(DocumentId, DocumentNo, property, clientId,
        description, status);
  }

  /**
   * This method only for Purchase Requisition Alert Process
   * 
   * @param property
   *          preference
   * @param clientId
   * @param description
   * @param status
   *          NEW-new Alert,SOLVED-alert Solved
   * @return True --Alert Created, False --Error
   */
  public static Boolean alertInsertionRole(String DocumentId, String DocumentNo, String roleId,
      String userId, String clientId, String description, String status) {
    return UtilityDAO.alertInsertionRole(DocumentId, DocumentNo, roleId, userId, clientId,
        description, status);
  }

  /**
   * 
   * @param roleId
   * @param clientId
   * @return True --Alert recipient Created, False --Error
   */
  public static Boolean insertAlertRecipient(String roleId, String clientId) {
    return UtilityDAO.insertAlertRecipient(roleId, clientId);
  }

  /**
   * Common method to insert approval history in the specified tables
   * 
   * @param data
   *          {@link JSONObject} containing the data like the approval action and the next performer
   *          etc.
   * @return returns the count of the inserted lines.
   */
  public static int InsertApprovalHistory(JSONObject data) {
    return UtilityDAO.InsertApprovalHistory(data);
  }

  /**
   * Common method to check the storage detail qty going to negative or not
   * 
   * @param ProductId
   *          ,LocatorId,NeedToRedQty,ClientId
   * @return returns -1 if storage detail of particular product qty going to negative otherwise
   *         return 1.
   */
  public static int ChkStoragedetOnhandQtyNeg(String ProductId, String LocatorId,
      BigDecimal NeedToRedQty, String ClientId) {
    return UtilityDAO.ChkStoragedetOnhandQtyNeg(ProductId, LocatorId, NeedToRedQty, ClientId);
  }

  /**
   * Common method to find a default bin, if there is no default bin then return any one bin.
   * 
   * @param WarehouseId
   * @return
   */
  public static String GetDefaultBin(String WarehouseId) {
    return UtilityDAO.GetDefaultBin(WarehouseId);
  }

  /**
   * 
   * @param strorgId
   *          Organization to filter sequence
   * 
   * @param strTransactionType
   *          Transaction Process Type
   * 
   * @return returns false if no sequence exists
   */
  public static String getTransactionSequence(String strorgId, String strTransactionType) {
    return UtilityDAO.getTransactionSequence(strorgId, strTransactionType);
  }

  /**
   * 
   * @param strorgId
   *          Organization to filter sequence
   * @param strclientId
   *          client to filter sequence
   * @param strTransactionType
   *          Transaction Process Type
   * 
   * @return returns false if no sequence exists
   */
  public static String getTransactionSequencewithclient(String strorgId, String clientId,
      String strTransactionType) {
    return UtilityDAO.getTransactionSequencewithclient(strorgId, clientId, strTransactionType);
  }

  /**
   * 
   * @param strorgId
   *          Organization to filter sequence
   * 
   * @param strTransactionType
   *          Transaction Process Type
   * 
   * @return returns false if no Specification sequence exists
   */
  public static String getSpecificationSequence(String strorgId, String strTransactionType) {
    return UtilityDAO.getSpecificationSequence(strorgId, strTransactionType);
  }

  /**
   * 
   * @param strorgId
   *          Organization to filter sequence
   * 
   * @param strTransactionType
   *          Transaction Process Type
   * 
   * @return returns false if no Specification sequence exists
   */
  public static String getProcessSpecificationSequence(String strorgId, String strTransactionType) {
    return UtilityDAO.getProcessSpecificationSequence(strorgId, strTransactionType);
  }

  /**
   * 
   * @param strorgId
   * @param strTransactionType
   * @param sequence
   * @return false if duplicate sequence is created.
   */
  public static Boolean chkTransactionSequence(String strorgId, String strTransactionType,
      String sequence) {
    return UtilityDAO.chkTransactionSequence(strorgId, strTransactionType, sequence);
  }

  /**
   * 
   * @param strorgId
   * @param clientId
   * @param strTransactionType
   * @param sequence
   * @return false if duplicate sequence is created.
   */
  public static Boolean chkTransactionSequencewithclient(String strorgId, String clientId,
      String strTransactionType, String sequence) {
    return UtilityDAO.chkTransactionSequencewithclient(strorgId, clientId, strTransactionType,
        sequence);
  }

  /**
   * 
   * @param strorgId
   * @param strTransactionType
   * @param sequence
   * @return false if duplicate sequence is created.
   */
  public static Boolean chkSpecificationSequence(String strorgId, String strTransactionType,
      String sequence) {
    return UtilityDAO.chkSpecificationSequence(strorgId, strTransactionType, sequence);
  }

  /**
   * 
   * @param value
   * @param lookupLine
   * @return false if it is not between the range defined in initial BG refernce lookup, else true
   */
  public static Boolean isValidInitialBGNumber(String value, ESCMDefLookupsTypeLn lookupLine) {
    return UtilityDAO.isValidInitialBGNumber(value, lookupLine);
  }

  /**
   * 
   * @param value
   * 
   * @return false if it is not valid hijri date, else trie
   */
  public static Boolean isValidDate(String value) {
    return UtilityDAO.isValidDate(value);
  }

  /**
   * check mobile no is valid sauid format or not
   * 
   * @param phoneno
   * @return
   */

  public static Boolean chkPhonenoisSaudiFormat(String phoneno) {
    return UtilityDAO.chkPhonenoisSaudiFormat(phoneno);
  }

  /**
   * 
   * @param value
   * 
   * @return false if it not equal "N" or "Y" else true
   */
  public static Boolean isBoolean(String value) {
    return UtilityDAO.isBoolean(value);
  }

  /**
   * 
   * @param value
   * @param name
   * @param lookupline
   * @return false if it not equal "N" or "Y" else true
   */
  public static void validateInitialBGValue(String value, String name, String fieldName,
      ESCMDefLookupsTypeLn lookupLine) {
    UtilityDAO.validateInitialBGValue(value, name, fieldName, lookupLine);
  }

  public static String getUserLineManager(User user) {
    return UtilityDAO.getUserLineManager(user);
  }

  public static String getEmployeeLineManager(String strBeneficiaryId) {
    return UtilityDAO.getEmployeeLineManager(strBeneficiaryId);
  }

  public static String getPreferenceValue(String strPreference, String strWindowId) {
    return UtilityDAO.getPreferenceValue(strPreference, strWindowId);
  }

  /**
   * 
   * @param strOrdelineId
   * @param shipQty
   * @return if PR quantity Released Successfully then true else false;
   */
  public static boolean releasePROrderQty(String strOrdelineId, BigDecimal shipQty) {
    return UtilityDAO.releasePROrderQty(strOrdelineId, shipQty);
  }

  public static OrganizationInformation getOrgInfo(String strOrgId) {
    return UtilityDAO.getOrgInfo(strOrgId);
  }

  /**
   * 
   * @param AccountDate
   * @param tableName
   * @param CalendarId
   * @param OrgId
   * @param action
   * @return document sequence for table name
   */
  public static String getDocumentSequence(String AccountDate, String tableName, String CalendarId,
      String OrgId, boolean action) {
    return UtilityDAO.getDocumentSequence(AccountDate, tableName, CalendarId, OrgId, action);
  }

  /**
   * Common method to check direct approval on the specified tables
   * 
   * @param data
   *          {@link JSONObject} containing the data like the table details etc.
   * @return returns the true when it is direct approval on document rule
   */
  public static boolean isDirectApproval(JSONObject data) {
    return UtilityDAO.isDirectApproval(data);
  }

  /**
   * Convert the
   * 
   * @param pattern
   * @param gregorianDate
   * @return
   */
  public static String convertGregToHijriTabadulPattern(Date gregorianDate) {
    String dateStringGreg = convertGregDatePattern(strYearFormat, gregorianDate);

    String dateStringHijri = convertTohijriDate(dateStringGreg);

    String[] dateParts = dateStringHijri.split("-");

    return dateParts[2] + "/" + dateParts[1] + "/" + dateParts[0];

  }

  /**
   * Converts date to specific format string
   * 
   * @param pattern
   * @param date
   * @return
   */
  public static String convertGregDatePattern(String pattern, Date date) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

    return simpleDateFormat.format(date);
  }

  /**
   * Convert the date in String from one format to another
   * 
   * @param fromDateFormat
   * @param toDateFormat
   * @param dateString
   * @return
   * @throws Exception
   */
  public static String convertStringDateFormat(String fromDateFormat, String toDateFormat,
      String dateString) throws Exception {
    SimpleDateFormat fromFormat = new SimpleDateFormat(fromDateFormat);
    Date date = fromFormat.parse(dateString);

    return convertGregDatePattern(toDateFormat, date);

  }

  /**
   * Convert Unix Time Stamp to date
   * 
   * @param javaTimeStamp
   * @return
   */
  public static Date unixTimeStampToJava(double javaTimeStamp) {

    Date date = new Date((long) javaTimeStamp * 1000);

    return date;
  }

  /**
   * Get period List<Period>
   * 
   * @param vars
   * @return period List<Period>
   */
  public static List<Period> getPeriodList(Object acctDate, String calender) {
    return UtilityDAO.getPeriodList(acctDate, calender);
  }

  /**
   * This method will return list of sub look up values based on lookup
   * 
   * @param strClientId
   * @param lookupCode
   * @return list of sub lookup values
   */
  public static List<EUTDeflookupsTypeLn> getSubLookupByLookupList(String strClientId,
      String lookupCode) {
    return UtilityDAO.getSubLookupByLookupList(strClientId, lookupCode);

  }

  /**
   * This method will return sub look up value based on lookup code
   * 
   * @param strClientId
   * @param lookupCode
   * @return sub lookup code
   */
  public static String findSubLookupByCode(String code, String lookupCode) {
    return UtilityDAO.findSubLookupByCode(code, lookupCode);

  }

  /**
   * This method will return sub look up value based on lookup code
   * 
   * @param strClientId
   * @param lookupCode
   * @return sub lookup code
   */
  public static EUTDeflookupsTypeLn findSubLookup(String code, String lookupCode) {
    return UtilityDAO.findSubLookup(code, lookupCode);

  }

  /**
   * Check whether the transaction period is open or not in open/close period control
   * 
   * @param transactionDate
   * @param orgId
   * @param clientId
   * @return
   */
  public static Boolean checkOpenPeriod(Date transactionDate, String orgId, String clientId) {
    return UtilityDAO.checkOpenPeriod(transactionDate, orgId, clientId);
  }

  /**
   * Check whether the transaction period is open or not in open/close period control for core
   * windows
   * 
   * @param transactionDate
   * @param orgId
   * @param docbaseType
   * @param docTypeId
   * @return
   */
  public static Boolean checkOpenPeriodCore(Date transactionDate, String orgId, String docbaseType,
      String docTypeId) {
    return UtilityDAO.checkOpenPeriodCore(transactionDate, orgId, docbaseType, docTypeId);
  }

  /**
   * This Method will return the child organization having calendarId
   * 
   * @param clientId
   * @param orgId
   * @return
   */
  public static String getChildOrgwithCalenderId(String clientId, String orgId) {
    return UtilityDAO.getChildOrgwithCalenderId(clientId, orgId);

  }

  /**
   * this method will update previous seq number in document sequence while delete the recent
   * created record - for reusing document sequence
   * 
   * @param AccountDate
   * @param tableName
   * @param OrgId
   * @param documentNo
   * @return
   */
  public static void setDocumentSequenceAfterDeleteRecord(String AccountDate, String tableName,
      String OrgId, Long documentNo, String Type, Boolean isyearbased) {
    UtilityDAO.setDocumentSequenceAfterDeleteRecord(AccountDate, tableName, OrgId, documentNo, Type,
        isyearbased);
  }

  /**
   * this method will update previous seq number in transaction sequence while delete the recent
   * created record - for reusing transaction sequence
   * 
   * @param transactionType
   * @param OrgId
   * @param documentNo
   */
  public static void setTransactionSequenceAfterDeleteRecord(String transactionType, String OrgId,
      String documentNo) {
    UtilityDAO.setTransactionSequenceAfterDeleteRecord(transactionType, OrgId, documentNo);
  }

  /**
   * 
   * @param LatestAgreement
   * @param agreementLine
   * @return
   */
  public static String getConCatTypeOther() {
    return UtilityDAO.getConCatTypeOther();
  }

  public static JSONObject getSubmitterDetail(String windowReference, String recordId) {
    return UtilityDAO.getSubmitterDetail(windowReference, recordId);
  }

  /**
   * get delegation
   * 
   * @param roleID
   * @param currentDate
   * @param documentType
   * @return list
   */
  public static List<EutDocappDelegateln> getDelegation(String roleID, Date currentDate,
      String documentType) {
    return UtilityDAO.getDelegation(roleID, currentDate, documentType);
  }

  /**
   * This method is used to get user details of particular role
   * 
   * @param roleId
   * @return list of assigned users
   */
  public static List<UtilityVO> getAssignedUserForRoles(String roleId) {
    return UtilityDAO.getAssignedUserForRoles(roleId);

  }

  /**
   * get budget initial obj based on accounting date in the gregorian format
   * 
   * @param acctdate
   *          - gregorian date format dd-MM-yyy
   * @param clientId
   * @return Budget initial object
   */
  public static EfinBudgetIntialization getBudgetInitialByUsingDateFormatGreg(String acctdate,
      String clientId) {
    return UtilityDAO.getBudgetInitialByUsingDateFormatGreg(acctdate, clientId);

  }

  /**
   * if the table has line number return true else false
   * 
   * @param tableId
   * @return true
   */
  public static Boolean checkTableHasLineNo(String tableId) {
    return UtilityDAO.checkTableHasLineNo(tableId);
  }

  /**
   * Get Property value from property file
   * 
   * @param key
   * @return
   */
  public static String getProperty(String key) {
    return UtilityDAO.getProperty(key);
  }
}
