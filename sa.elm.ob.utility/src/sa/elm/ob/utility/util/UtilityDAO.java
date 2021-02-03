package sa.elm.ob.utility.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Restrictions;
import org.jfree.util.Log;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.OrgTree;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.model.financialmgmt.calendar.Calendar;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;
import org.openbravo.service.db.DalConnectionProvider;

import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinMonthSequence;
import sa.elm.ob.finance.EfinYearSequence;
import sa.elm.ob.finance.efinbudgetencum;
import sa.elm.ob.scm.DocumentSequence;
import sa.elm.ob.scm.ESCMBGAmtRevision;
import sa.elm.ob.scm.ESCMBGConfiscation;
import sa.elm.ob.scm.ESCMBGWorkbench;
import sa.elm.ob.scm.ESCMCommittee;
import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.ESCMProposalEvlEvent;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmInsuranceCertificate;
import sa.elm.ob.scm.EscmOrderSourceRef;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmTechnicalevlEvent;
import sa.elm.ob.scm.Escmannoucements;
import sa.elm.ob.scm.Escmopenenvcommitee;
import sa.elm.ob.scm.Escmsalesvoucher;
import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.utility.EUTDeflookupsTypeLn;
import sa.elm.ob.utility.EutDocappDelegateln;

/**
 * @author Qualian
 */
@SuppressWarnings("deprecation")
public class UtilityDAO {
  private static final Logger log4j = Logger.getLogger(UtilityDAO.class);

  /**
   * Get Product Unit
   * 
   * @param productId
   * @return JSON tith UOM Details
   */
  public static JSONObject getProductUoM(String productId) {
    PreparedStatement ps = null;
    ResultSet rs = null;
    JSONObject result = new JSONObject();
    try {
      result.put("id", "");
      result.put("name", "");
      result.put("symbol", "");
      ps = OBDal.getInstance().getConnection().prepareStatement(
          "select uom.c_uom_id, uom.name, coalesce(uom.uomsymbol, uom.name) as uomsymbol from c_uom uom join m_product p on p.c_uom_id = uom.c_uom_id where p.m_product_id = ?;");
      ps.setString(1, productId);
      rs = ps.executeQuery();
      if (rs.next()) {
        result.put("id", rs.getString("c_uom_id"));
        result.put("name", rs.getString("name"));
        result.put("symbol", rs.getString("uomsymbol"));
      }
    } catch (final Exception e) {
      log4j.error("Exception in getUom() : ", e);
      return result;
    }
    return result;
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
    PreparedStatement st = null;
    ResultSet rs = null;
    String sequenceNo = "";
    try {
      st = conn.prepareStatement("select ad_sequence_doc(?, ?, ?);");
      st.setString(1, seqName);
      st.setString(2, clientId);
      st.setString(3, (update == true ? "Y" : "N"));
      rs = st.executeQuery();
      if (rs.next())
        sequenceNo = rs.getString("ad_sequence_doc") == null ? "" : rs.getString("ad_sequence_doc");
    } catch (final Exception e) {
      log4j.error("Exception in getSequenceNo() Method : ", e);
      return "";
    }
    return sequenceNo;
  }

  /**
   * Check Document Sequence exists
   * 
   * @param clientId
   * @param seqName
   * @return boolean
   */
  public static boolean checkDocumentSequence(String clientId, String seqName) {
    OBContext.setAdminMode();
    try {
      OBCriteria<Sequence> sCriteria = OBDal.getInstance().createCriteria(Sequence.class);
      sCriteria.add(Restrictions.eq("client.id", clientId));
      sCriteria.add(Expression.eq("name", seqName));
      sCriteria.setMaxResults(1);
      if (sCriteria.list().size() > 0)
        return true;
      else
        return false;
    } catch (Exception e) {
      log4j.error("Exception in checkDocumentSequence : ", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Convert Message from AD_Message
   * 
   * @param value
   * @param lang
   * @return Message
   */
  public static String getADMessage(String value, String lang) {
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      st = OBDal.getInstance().getConnection().prepareStatement(
          "SELECT coalesce(t.MSGTEXT, m.MSGTEXT) AS MSGTEXT FROM AD_MESSAGE m left join AD_MESSAGE_TRL t on m.AD_MESSAGE_ID = t.AD_MESSAGE_ID  and t.AD_LANGUAGE = ? WHERE m.VALUE = ?;");
      st.setString(1, lang);
      st.setString(2, value);
      rs = st.executeQuery();
      if (rs.next())
        return rs.getString("msgtext");
      rs.close();
    } catch (final Exception e) {
      log4j.error("Exception in getADMessage() Method : ", e);
      return value;
    }
    return value;
  }

  /**
   * Getting HijriDateAdjustment
   * 
   * @param vars
   * @return Days in Integer
   */
  public static int getHijriDateAdjustment() {
    PreparedStatement st = null;
    ResultSet rs = null;
    int adjustDays = 0;
    try {
      st = OBDal.getInstance().getConnection().prepareStatement(
          "select sign, days from qu_adjusthijridate where ad_client_id = ? order by updated desc limit 1;");
      st.setString(1, OBContext.getOBContext().getCurrentClient().getId());
      rs = st.executeQuery();
      if (rs.next()) {
        adjustDays = Integer.parseInt(rs.getString("sign") + rs.getString("days"));
      }
    } catch (Exception e) {
      log4j.error("Exception in getHijriDateAdjustment : ", e);
      return 0;
    }
    return adjustDays;
  }

  /**
   * Getting Child Organization
   * 
   * @param clientId
   * @param orgId
   * @return Organization List wrapped by Single Quotes, Separated by Comma
   */
  public static String getChildOrg(String clientId, String orgId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String orgList = "";
    try {
      orgList = "'0', '" + orgId + "'";
      st = OBDal.getInstance().getConnection().prepareStatement("select eut_getchildorglist(?, ?)");
      st.setString(1, clientId);
      st.setString(2, orgId);
      rs = st.executeQuery();
      if (rs.next()) {
        if ("0".equals(orgId))
          return rs.getString("eut_getchildorglist");
        else
          return "'0'," + rs.getString("eut_getchildorglist");
      }
      return orgList;
    } catch (final Exception e) {
      log4j.error("Exception in getChildOrg() Method : ", e);
      return orgList;
    }
  }

  /**
   * Getting Role having access Organization
   * 
   * @param clientId
   * @param orgId
   * @return Organization List wrapped by Single Quotes, Separated by Comma
   */
  public static String getRoleaccOrg(String clientId, String roleId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String orgList = null;
    try {
      st = OBDal.getInstance().getConnection().prepareStatement(
          "select ad_org_id from AD_Role_OrgAccess where ad_client_id=? and ad_role_id=? and isactive='Y'");
      st.setString(1, clientId);
      st.setString(2, roleId);
      rs = st.executeQuery();
      while (rs.next()) {
        if (orgList == null) {
          orgList = "'" + rs.getString("ad_org_id") + "'";
        } else {
          orgList = orgList + ",'" + rs.getString("ad_org_id") + "'";
        }
      }
      return orgList;
    } catch (final Exception e) {
      log4j.error("Exception in getRoleaccOrg() Method : ", e);
      return orgList;
    }
  }

  /**
   * Getting Child Organization By HashSet<String>
   * 
   * @param clientId
   * @param orgId
   * @return Organization List by HashSet<String>
   */
  public static HashSet<String> getChildOrgBySet(String clientId, String orgId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    HashSet<String> set = new HashSet<String>();
    try {
      set.add(orgId);
      st = OBDal.getInstance().getConnection().prepareStatement("select eut_getchildorglist(?, ?)");
      st.setString(1, clientId);
      st.setString(2, orgId);
      rs = st.executeQuery();
      if (rs.next()) {
        for (String s : rs.getString("eut_getchildorglist").replace("'", "").split(",")) {
          set.add(s);
        }
      }
      return set;
    } catch (final Exception e) {
      log4j.error("Exception in getChildOrgBySet() Method : ", e);
      return set;
    }
  }

  /**
   * Getting Parent Organization
   * 
   * @param clientId
   * @param orgId
   * @return Organization List wrapped by Single Quotes, Separated by Comma
   */
  public static String getParentOrg(String clientId, String orgId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      st = OBDal.getInstance().getConnection()
          .prepareStatement("select eut_getparentorglist(?, ?)");
      st.setString(1, clientId);
      st.setString(2, orgId);
      rs = st.executeQuery();
      if (rs.next()) {
        return rs.getString("eut_getparentorglist");
      }
      return "'" + orgId + "'";
    } catch (final Exception e) {
      log4j.error("Exception in getParentOrg() Method : ", e);
      return "'" + orgId + "'";
    }
  }

  /**
   * Getting UOM List
   * 
   * @param clientId
   * @param orgId
   * @return UOM List by List<UtilityVO>
   */
  public static List<UtilityVO> getUoMList(String clientId, String orgId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    List<UtilityVO> list = new ArrayList<UtilityVO>();
    try {
      st = OBDal.getInstance().getConnection().prepareStatement(
          "select coalesce(uomsymbol, name)as name, c_uom_id from c_uom where ad_client_id = ? or ad_client_id = '0' and isactive = 'Y' and ad_org_id in ("
              + Utility.getChildOrg(clientId, orgId) + ") order by name");
      st.setString(1, clientId);
      rs = st.executeQuery();
      while (rs.next()) {
        UtilityVO vo = new UtilityVO();
        vo.setId(rs.getString("c_uom_id"));
        vo.setName(rs.getString("name"));
        list.add(vo);
      }
    } catch (final Exception e) {
      log4j.error("Exception in getUoMList() Method : ", e);
      return list;
    }
    return list;
  }

  /**
   * Getting Organization List
   * 
   * @param clientId
   * @param orgId
   * @return Organization List by List<UtilityVO>
   */
  public static List<UtilityVO> getOrganizationList(String clientId, String orgId,
      boolean include0) {
    PreparedStatement st = null;
    ResultSet rs = null;
    List<UtilityVO> list = new ArrayList<UtilityVO>();
    UtilityVO vo = null;
    try {
      st = OBDal.getInstance().getConnection().prepareStatement(
          "select ad_org_id, name from ad_org where ad_client_id in ('0', ?) and isactive = 'Y' and isready = 'Y'"
              + (include0 ? "" : " and ad_org_id <> '0'") + " order by ad_org_id = '0' desc, name");
      st.setString(1, clientId);
      rs = st.executeQuery();
      while (rs.next()) {
        vo = new UtilityVO();
        vo.setId(rs.getString("ad_org_id"));
        vo.setName(rs.getString("name"));
        list.add(vo);
      }
    } catch (final Exception e) {
      log4j.error("Exception in getOrganizationList() Method : ", e);
      return list;
    }
    return list;
  }

  /**
   * Get Accessible Org By List<UtilityVO>
   * 
   * @param vars
   * @return Accessible Org by List<UtilityVO>
   */
  public static List<UtilityVO> getAccessibleOrgByList(VariablesSecureApp vars) {
    PreparedStatement st = null;
    ResultSet rs = null;
    List<UtilityVO> list = new ArrayList<UtilityVO>();
    UtilityVO vo = null;
    try {
      st = OBDal.getInstance().getConnection().prepareStatement(
          "select ad_org_id, name from ad_org where ad_client_id in ('0', ?) and isactive = 'Y' and isready = 'Y' and ad_org_id in ("
              + Utility.getAccessibleOrg(vars) + ") order by ad_org_id = '0' desc, name");
      st.setString(1, vars.getClient());
      rs = st.executeQuery();
      while (rs.next()) {
        vo = new UtilityVO();
        vo.setId(rs.getString("ad_org_id"));
        vo.setName(rs.getString("name"));
        list.add(vo);
      }
    } catch (final Exception e) {
      log4j.error("Exception in getAccessibleOrgByList() Method : ", e);
      return list;
    }
    return list;
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
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      st = OBDal.getInstance().getConnection().prepareStatement(
          "select ad_form_access_id from ad_form_access where ad_client_id in ('0', ?) and ad_role_id = ? and ad_form_id = ?;");
      st.setString(1, clientId);
      st.setString(2, roleId);
      st.setString(3, formId);
      rs = st.executeQuery();
      if (rs.next())
        return true;
      else
        return false;
    } catch (final Exception e) {
      log4j.error("Exception in checkFormAccess() Method : ", e);
      return false;
    }
  }

  /**
   * 
   * @param t
   * @param strId
   * @return
   */
  public static <T extends BaseOBObject> T getObject(Class<T> t, String strId) {
    try {
      OBContext.setAdminMode();
      return OBDal.getInstance().get(t, strId);
    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * 
   * @param t
   * @return
   */
  public static <T extends BaseOBObject> T getEntity(Class<T> t) {
    try {
      OBContext.setAdminMode();
      return OBProvider.getInstance().get(t);
    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } finally {
      // OBContext.restorePreviousMode();
    }
  }

  /**
   * This method is used to get Line number
   * 
   * @param tableName
   * @param recordId
   * @param colName
   * @param whereClause
   * @return
   */
  public static long getLineNo(String tableName, String recordId, String colName,
      String whereClause) {
    long lineNo = 0;
    Query LineNoQry = null;
    try {
      OBContext.setAdminMode();
      String hql = "select  coalesce(max(" + colName + "),0)+10 as lineno from " + tableName
          + " tb where tb." + whereClause + "='" + recordId + "' ";
      LineNoQry = OBDal.getInstance().getSession().createQuery(hql.toString());
      if (LineNoQry != null) {
        if (LineNoQry.list().size() > 0) {
          if (LineNoQry.iterate().hasNext()) {
            lineNo = Long.parseLong(LineNoQry.iterate().next().toString());
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getLineNo : ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return lineNo;
  }

  /**
   * Checks whether the provided string is a number.
   * 
   * @param strNumberString
   *          String to check if it is a number.
   * @return boolean true if the string is a number else returns boolean false.
   * 
   */

  @SuppressWarnings("unused")
  public static boolean isNumber(String strNumberString) {
    try {
      Double d = Double.parseDouble(strNumberString.trim());
    } catch (NumberFormatException nfe) {
      return false;
    }
    return true;
  }

  /**
   * 
   * @param hijridate
   *          (dd-MM-yyyy) format
   * @return true or false
   * 
   */
  public static boolean Checkhijridate(String inpDate) {
    PreparedStatement st = null;
    ResultSet rs = null;

    try {
      // formatted as dd-MM-yyyy
      SimpleDateFormat dateformat = new SimpleDateFormat("dd-MM-yyyy");
      // convert string to date
      Date dateDate = dateformat.parse(inpDate);
      // formatted as yyyyMMdd (hijri format)
      SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyyMMdd");
      // formatted date as yyyyMMdd format
      String stringDate = dateYearFormat.format(dateDate);
      st = OBDal.getInstance().getConnection().prepareStatement(
          "select count(hijri_date) as count from eut_hijri_dates where hijri_date='" + stringDate
              + "'");
      rs = st.executeQuery();
      if (rs.next()) {
        if (rs.getInt("count") > 0) {
          return true;
        } else {
          return false;
        }
      } else {
        return false;
      }

    } catch (final Exception e) {
      log4j.error("Exception in validation for hijiridate: ", e);
      return false;
    }
  }

  /**
   * 
   * @return max hijriDate
   */
  public static String HijriMaxDate() {
    PreparedStatement st = null;
    ResultSet rs = null;
    SimpleDateFormat yearformat = new SimpleDateFormat("yyyyMMdd");
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    String strDate = "";
    Date HijriDate = null;
    try {
      st = OBDal.getInstance().getConnection()
          .prepareStatement("select max(hijri_date) as maxdate from eut_hijri_dates");
      rs = st.executeQuery();
      if (rs.next()) {
        strDate = rs.getString("maxdate");
        HijriDate = yearformat.parse(strDate);
        strDate = dateFormat.format(HijriDate);
      }
    } catch (final Exception e) {
      log4j.error("Exception in MaxDate for hijiridate: ", e);
    }
    return strDate;

  }

  /**
   * 
   * @return Min hijriDate
   */
  public static String HijriMinDate() {
    PreparedStatement st = null;
    ResultSet rs = null;
    SimpleDateFormat yearformat = new SimpleDateFormat("yyyyMMdd");
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    String strDate = "";
    Date HijriDate = null;
    try {
      st = OBDal.getInstance().getConnection()
          .prepareStatement("select min(hijri_date) as mindate from eut_hijri_dates");
      rs = st.executeQuery();
      if (rs.next()) {
        strDate = rs.getString("mindate");
        HijriDate = yearformat.parse(strDate);
        strDate = dateFormat.format(HijriDate);
      }
    } catch (final Exception e) {
      log4j.error("Exception in MinDate for hijiridate: ", e);
    }
    return strDate;

  }

  /**
   * 
   * Converts Gregorian date to Hijri Date
   * 
   * @param gregDate
   *          (yyyy-MM-dd) format
   * @return returns hijri date in (dd-MM-yyyy HH24:MI:SS) format
   */
  public static String convertToHijriDate(String gregDate) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String hijriDate = "";
    try {
      st = OBDal.getInstance().getConnection()
          .prepareStatement("select eut_convert_to_hijri_timestamp(to_char(to_timestamp('"
              + gregDate + "','YYYY-MM-DD HH24:MI:SS'),'YYYY-MM-DD  HH24:MI:SS'))");
      rs = st.executeQuery();
      if (rs.next()) {
        hijriDate = rs.getString("eut_convert_to_hijri_timestamp");
      }
    }

    catch (final Exception e) {
      log4j.error("Exception in convertToHijriDate() Method : ", e);
      return "0";
    }
    return hijriDate;
  }

  /**
   * 
   * Converts Gregorian date to Hijri Date
   * 
   * @param gregDate
   *          (yyyy-MM-dd) format
   * @return returns hijri date in (dd-MM-yyyy) format
   */
  public static String convertTohijriDate(String gregDate) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String hijriDate = "";
    try {
      st = OBDal.getInstance().getConnection()
          .prepareStatement("select eut_convert_to_hijri(to_char(to_timestamp('" + gregDate
              + "','YYYY-MM-DD HH24:MI:SS'),'YYYY-MM-DD  HH24:MI:SS'))");
      rs = st.executeQuery();
      if (rs.next()) {
        hijriDate = rs.getString("eut_convert_to_hijri");
      }
    }

    catch (final Exception e) {
      log4j.error("Exception in convertTohijriDate() Method : ", e);
      return "0";
    }
    return hijriDate;
  }

  /**
   * Converts Gregorian date to Hijri Date in Event Handler
   * 
   * @param gregDate
   *          (yyyy-MM-dd) format
   * @return returns hijri date in (dd-MM-yyyy HH24:MI:SS) format
   */
  public static String eventConvertTohijriDate(String gregDate) {
    String hijriDate = "";
    try {
      SQLQuery Query = OBDal.getInstance().getSession()
          .createSQLQuery("select eut_convert_to_hijri(to_char(to_timestamp('" + gregDate
              + "','YYYY-MM-DD HH24:MI:SS'),'YYYY-MM-DD  HH24:MI:SS'))");
      if (Query.list().size() > 0) {
        Object row = (Object) Query.list().get(0);
        hijriDate = (String) row;
        log4j.debug("ConvertedDate:" + (String) row);
      }
    }

    catch (final Exception e) {
      log4j.error("Exception in convertTohijriDate() Method : ", e);
      return "0";
    }
    return hijriDate;
  }

  /**
   * 
   * Converts Hijri date to Gregorian Date in Event Handler
   * 
   * @param hijriDate
   *          in date format (yyyyMMdd) format
   * @return returns gregorian date in (yyyy-MM-dd HH:MI:SS) timestamp format
   */
  public static String eventConvertToGregorian(String hijriDate) {
    String gregDate = "";
    try {
      SQLQuery Query = OBDal.getInstance().getSession().createSQLQuery(
          "select to_char(gregorian_date,'YYYY-MM-DD')  from eut_hijri_dates where hijri_date ='"
              + hijriDate + "'");
      log4j.debug("Query:" + Query.toString());
      if (Query.list().size() > 0) {
        Object row = (Object) Query.list().get(0);
        gregDate = (String) row;
        log4j.debug("ConvertedDate:" + (String) row);
      }
    }

    catch (final Exception e) {
      log4j.error("Exception in convertToGregorianDate() Method : ", e);
      return "0";
    }
    return gregDate;
  }

  /**
   * 
   * Converts Gregorian date to Hijri Date
   * 
   * @param gregDate
   *          in timestamp format (yyyy-MM-dd HH24:MI:SS) format
   * @return returns hijri date in (dd-MM-yyyy HH24:MI:SS) timestamp format
   */
  public static String convertToHijriTimestamp(String gregDate) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String hijriDate = "";
    try {
      st = OBDal.getInstance().getConnection()
          .prepareStatement("select eut_convert_to_hijri_timestamp(to_char(to_timestamp('"
              + gregDate + "','YYYY-MM-DD HH24:MI:SS'),'YYYY-MM-DD HH24:MI:SS'))");
      rs = st.executeQuery();
      if (rs.next()) {

        hijriDate = rs.getString("eut_convert_to_hijri_timestamp");

      }
    }

    catch (final Exception e) {
      log4j.error("Exception in convertToHijriDate() Method : ", e);
      return "0";
    }
    return hijriDate;
  }

  /**
   * 
   * Converts Hijri date to Gregorian Date
   * 
   * @param hijriDate
   *          in date format (dd-MM-yyyy) format
   * @return returns gregorian date in (yyyy-MM-dd HH:MI:SS) timestamp format
   */
  public static String convertToGregorian(String hijriDate) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String gregDate = "";
    try {
      st = OBDal.getInstance().getConnection()
          .prepareStatement("select eut_convertto_gregorian('" + hijriDate + "')");
      rs = st.executeQuery();
      if (rs.next()) {
        gregDate = rs.getString("eut_convertto_gregorian");
      }
    }

    catch (final Exception e) {
      log4j.error("Exception in convertToGregorian() Method : ", e);
      return "0";
    }
    return gregDate;
  }

  /**
   * 
   * Converts Hijri date to Gregorian Date
   * 
   * @param hijriDate
   *          in date format (dd-MM-yyyy) format
   * @return returns gregorian date in (dd-MM-yyyy HH:MI:SS) timestamp format
   */
  public static String convertToGregTimeStamp(String hijriDate) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String gregDate = "";
    try {
      st = OBDal.getInstance().getConnection()
          .prepareStatement("select eut_convertto_greg_timestamp('" + hijriDate + "')");
      rs = st.executeQuery();
      if (rs.next()) {
        gregDate = rs.getString("eut_convertto_greg_timestamp");
      }
    }

    catch (final Exception e) {
      log4j.error("Exception in convertToGregTimeStamp() Method : ", e);
      return "0";
    }
    return gregDate;
  }

  /**
   * This method is used to convert hijiri to gregorian date
   * 
   * @param hijriDate
   * @return
   */
  public static Date convertToGregorianDate(String hijriDate) {

    Date date = null;

    String dateStr = convertToGregorian(hijriDate);
    if (StringUtils.isNotBlank(dateStr) && !StringUtils.equalsIgnoreCase(dateStr, "0")) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:SS");
      LocalDate localDate = LocalDate.parse(dateStr, formatter);
      date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    return date;
  }

  /**
   * 
   * @param hijriDate
   *          format dd-MM-yyyy
   * @return gregorian date dd-MM-yyyy
   */
  public static String convertToGregorian_tochar(String hijriDate) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String gregDate = "";
    try {
      st = OBDal.getInstance().getConnection()
          .prepareStatement("select to_char(eut_convertto_gregorian('" + hijriDate
              + "')) as eut_convertto_gregorian");
      rs = st.executeQuery();
      if (rs.next()) {
        gregDate = rs.getString("eut_convertto_gregorian");
      }
    }

    catch (final Exception e) {
      log4j.error("Exception in convertToGregorian_tochar() Method : ", e);
      return "0";
    }
    return gregDate;
  }

  /**
   * Function To get the General Sequence
   * 
   * 
   * @param AccountDate
   *          (dd-MM-yyyy) format
   * @param Doctype
   * @param Action
   *          save or post
   * @param CalendarId
   * @return If SequenceNo is there return SequenceNo else No Document Type return '1' else No
   *         Document Sequence Return '-1'
   */
  public static String getCommonGeneralSequence(String AccountDate, String Doctype, String Action,
      String CalendarId) {
    PreparedStatement ps = null;
    ResultSet rs = null;
    String yearquery = "";
    String periodQuery = "";
    String sequence = "";
    String yearId = "";
    String periodId = "";

    try {
      // OBContext.setAdminMode();
      yearquery = "	select yr.c_year_id from c_period pr"
          + " join c_year yr on pr.c_year_id=yr.c_year_id" + " where to_date('" + AccountDate
          + "','dd-MM-yyyy') between cast(pr.startdate as date) and cast(pr.enddate as date)"
          + " and c_calendar_id='" + CalendarId + "'";
      periodQuery = "select pr.c_period_id from c_period pr "
          + " join c_year yr on pr.c_year_id=yr.c_year_id " + " where to_date('" + AccountDate
          + "','dd-MM-yyyy') between cast(pr.startdate as date) and cast(cpr.enddate as date)"
          + " and c_calendar_id='" + CalendarId + "'";
      // get general sequence
      DocumentType DocType = OBDal.getInstance().get(DocumentType.class, Doctype);
      if (DocType != null) {
        Sequence DocSequence = DocType.getDocumentSequence();
        if (DocSequence != null) {
          if (Action.equals("save")) {
            if (DocSequence.isEfinIsgeneralseq()) {
              ps = OBDal.getInstance().getConnection().prepareStatement(yearquery);
              rs = ps.executeQuery();
              if (rs.next()) {
                yearId = rs.getString("c_year_id");
              }
              // get GeneralSequence number from year
              OBQuery<Sequence> genSequencelist = OBDal.getInstance().createQuery(Sequence.class,
                  "as e where e.name='General Sequence'");
              Sequence gensequence = genSequencelist.list().get(0);
              OBQuery<EfinYearSequence> yearSequencelist = OBDal.getInstance()
                  .createQuery(EfinYearSequence.class, "as e where e.sequence.id='"
                      + gensequence.getId() + "' and e.year.id='" + yearId + "'");
              EfinYearSequence yearSequence = yearSequencelist.list().get(0);
              sequence = yearSequence.getNextAssignedNumber().toString();
              yearSequence.setNextAssignedNumber(yearSequence.getNextAssignedNumber() + 1);
              OBDal.getInstance().save(yearSequence);
              OBDal.getInstance().flush();
            }
          } else if (Action.equals("posting")) {
            // get Year SequenceNumber for Account payment Sequence
            if (DocSequence.isEfinIsacctpaymentseq()) {
              ps = OBDal.getInstance().getConnection().prepareStatement(yearquery);
              rs = ps.executeQuery();
              if (rs.next()) {
                yearId = rs.getString("c_year_id");
              }
              // get GeneralSequence number from year
              OBQuery<Sequence> genSequencelist = OBDal.getInstance().createQuery(Sequence.class,
                  "as e where e.name='Account Payment Sequence'");
              Sequence gensequence = genSequencelist.list().get(0);
              OBQuery<EfinYearSequence> yearSequencelist = OBDal.getInstance()
                  .createQuery(EfinYearSequence.class, "as e where e.sequence.id='"
                      + gensequence.getId() + "' and e.year.id='" + yearId + "'");
              EfinYearSequence yearSequence = yearSequencelist.list().get(0);
              sequence = yearSequence.getNextAssignedNumber().toString();
              yearSequence.setNextAssignedNumber(yearSequence.getNextAssignedNumber() + 1);
              OBDal.getInstance().save(yearSequence);
              OBDal.getInstance().flush();
            }
            // get month SequenceNumber for Account Non-payment Sequence
            else if (DocSequence.isEfinIsacctnonpaymentseq()) {

              ps = OBDal.getInstance().getConnection().prepareStatement(yearquery);
              rs = ps.executeQuery();
              if (rs.next()) {
                yearId = rs.getString("c_year_id");
              }
              OBQuery<Sequence> genSequencelist = OBDal.getInstance().createQuery(Sequence.class,
                  "as e where e.name='Account Non Payment Sequence'");
              Sequence gensequence = genSequencelist.list().get(0);
              OBQuery<EfinYearSequence> yearSequencelist = OBDal.getInstance()
                  .createQuery(EfinYearSequence.class, "as e where e.sequence.id='"
                      + gensequence.getId() + "' and e.year.id='" + yearId + "'");
              EfinYearSequence yearSequence = yearSequencelist.list().get(0);
              ps = OBDal.getInstance().getConnection().prepareStatement(periodQuery);
              rs = ps.executeQuery();
              if (rs.next()) {
                periodId = rs.getString("c_period_id");
              }
              OBQuery<EfinMonthSequence> monthSequenceList = OBDal.getInstance()
                  .createQuery(EfinMonthSequence.class, "as e where e.efinYearSequence.id='"
                      + yearSequence.getId() + "' and e.period.id='" + periodId + "'");
              EfinMonthSequence monthSequence = monthSequenceList.list().get(0);
              // get GeneralSequence number from month
              sequence = monthSequence.getNextAssignedNumber().toString();
              monthSequence.setNextAssignedNumber(monthSequence.getNextAssignedNumber() + 1);
              OBDal.getInstance().save(monthSequence);
              OBDal.getInstance().flush();
            }
          }
        } else {
          // No Document Sequence Return -1
          sequence = "-1";
        }

      } else {
        // No Document Type return 1
        sequence = "1";
      }
    } catch (Exception e) {
      // TODO: handle exception
      log4j.error("Exception in getGeneralSequence() Method : ", e);
      return "0";
    } finally {
      // OBContext.restorePreviousMode();
    }
    return sequence;
  }

  /**
   * 
   * 
   * @param AccountDate
   *          (dd-MM-yyyy) format
   * @param Type
   *          GS/NPS/PS(General/Non Payment,Payment Sequences)
   * @param CalendarId
   * @param OrgId
   * @param Action
   *          Save- true/Complete-false
   * @return SequenceNo
   */
  public static String getGeneralSequence(String AccountDate, String Type, String CalendarId,
      String OrgId, boolean action) {
    String yearquery = "", ParentQury = "";
    String[] orgIds = null;
    String periodQuery = "";
    String sequence = "0";
    String sequenceId = "";
    String yearId = "";
    String periodId = "";
    String gsQuery = "";
    try {
      OBContext.setAdminMode();
      yearquery = "	select yr.c_year_id from c_period pr"
          + " join c_year yr on pr.c_year_id=yr.c_year_id" + " where to_date('" + AccountDate
          + "','dd-MM-yyyy') between cast(pr.startdate as date) and cast(pr.enddate as date)"
          + " and c_calendar_id='" + CalendarId + "'";
      periodQuery = "select pr.c_period_id from c_period pr "
          + " join c_year yr on pr.c_year_id=yr.c_year_id " + " where to_date('" + AccountDate
          + "','dd-MM-yyyy') between cast(pr.startdate as date) and cast(pr.enddate as date)"
          + " and c_calendar_id='" + CalendarId + "'";
      // get general sequence
      if (Type.equals("GS")) {
        Query resultset = OBDal.getInstance().getSession().createSQLQuery(yearquery);
        Object yearID = resultset.list().get(0);
        yearId = (String) yearID;
        // get GeneralSequence number from year
        gsQuery = "select yrseq.efin_year_sequence_id from ad_sequence seq "
            + "join efin_year_sequence yrseq on seq.ad_sequence_id=yrseq.ad_sequence_id "
            + " where yrseq.c_year_id= :year_id and seq.em_efin_isgeneralseq='Y' and seq.ad_org_id= :org_id ";
        Query genSequencelist = OBDal.getInstance().getSession().createSQLQuery(gsQuery);
        genSequencelist.setParameter("year_id", yearId);
        genSequencelist.setParameter("org_id", OrgId);
        if (genSequencelist.list().size() == 0) {
          ParentQury = " select eut_parent_org('" + OrgId + "','"
              + OBContext.getOBContext().getCurrentClient().getId() + "')";
          Query parentresult = OBDal.getInstance().getSession().createSQLQuery(ParentQury);
          Object parentOrg = parentresult.list().get(0);
          orgIds = ((String) parentOrg).split(",");
          for (int i = 0; i < orgIds.length; i++) {
            Query Sequencelist = OBDal.getInstance().getSession().createSQLQuery(gsQuery);
            Sequencelist.setParameter("year_id", yearId);
            Sequencelist.setParameter("org_id", orgIds[i].replace("'", ""));
            if (Sequencelist.list().size() > 0) {
              Object sequenceID = Sequencelist.list().get(0);
              sequenceId = (String) sequenceID;
              break;
            }
          }
        } else {
          Object sequenceID = genSequencelist.list().get(0);
          sequenceId = (String) sequenceID;
        }
        EfinYearSequence yearSequence = OBDal.getInstance().get(EfinYearSequence.class, sequenceId);
        if (yearSequence != null) {
          sequence = yearSequence.getNextAssignedNumber() == null ? ""
              : yearSequence.getNextAssignedNumber().toString();
          // sequence = Prefix.concat(sequence);
          if (action) {
            yearSequence.setNextAssignedNumber(yearSequence.getNextAssignedNumber() + 1);
          }
          OBDal.getInstance().save(yearSequence);
        }
      }
      // get month SequenceNumber for Account Non-payment Sequence
      else if (Type.equals("NPS")) {
        Query resultset = OBDal.getInstance().getSession().createSQLQuery(yearquery);
        Object yearID = resultset.list().get(0);
        yearId = (String) yearID;
        Query result = OBDal.getInstance().getSession().createSQLQuery(periodQuery);
        Object periodID = result.list().get(0);
        periodId = (String) periodID;
        gsQuery = "select mnseq.efin_month_sequence_id from ad_sequence seq "
            + "	join efin_year_sequence yrseq on seq.ad_sequence_id=yrseq.ad_sequence_id "
            + " join efin_month_sequence mnseq on mnseq.efin_year_sequence_id=yrseq.efin_year_sequence_id "
            + " where yrseq.c_year_id=:year_id and seq.em_efin_isacctnonpaymentseq ='Y' "
            + " and seq.ad_org_id=:org_id and mnseq.c_period_id=:period_id";

        Query genSequencelist = OBDal.getInstance().getSession().createSQLQuery(gsQuery);
        genSequencelist.setParameter("year_id", yearId);
        genSequencelist.setParameter("org_id", OrgId);
        genSequencelist.setParameter("period_id", periodId);
        if (genSequencelist.list().size() == 0) {
          ParentQury = " select eut_parent_org('" + OrgId + "','"
              + OBContext.getOBContext().getCurrentClient().getId() + "')";
          Query parentresult = OBDal.getInstance().getSession().createSQLQuery(ParentQury);
          Object parentOrg = parentresult.list().get(0);
          orgIds = ((String) parentOrg).split(",");
          for (int i = 0; i < orgIds.length; i++) {
            Query Sequencelist = OBDal.getInstance().getSession().createSQLQuery(gsQuery);
            Sequencelist.setParameter("year_id", yearId);
            Sequencelist.setParameter("org_id", orgIds[i].replace("'", ""));
            Sequencelist.setParameter("period_id", periodId);
            if (Sequencelist.list().size() > 0) {
              Object sequenceID = Sequencelist.list().get(0);
              sequenceId = (String) sequenceID;
              break;
            }
          }
        } else {
          Object sequenceID = genSequencelist.list().get(0);
          sequenceId = (String) sequenceID;
        }
        EfinMonthSequence monthSequence = OBDal.getInstance().get(EfinMonthSequence.class,
            sequenceId);
        // get GeneralSequence number from month
        if (monthSequence != null) {
          sequence = monthSequence.getNextAssignedNumber() == null ? ""
              : monthSequence.getNextAssignedNumber().toString();
          // sequence = Prefix.concat(sequence);
          if (action) {
            monthSequence.setNextAssignedNumber(monthSequence.getNextAssignedNumber() + 1);
          }
          OBDal.getInstance().save(monthSequence);
        }
      }
      // get Year SequenceNumber for Account Payment Sequence
      else if (Type.equals("PS")) {
        Query resultset = OBDal.getInstance().getSession().createSQLQuery(yearquery);
        Object yearID = resultset.list().get(0);
        yearId = (String) yearID;
        // get GeneralSequence number from year
        gsQuery = "select yrseq.efin_year_sequence_id from ad_sequence seq "
            + "join efin_year_sequence yrseq on seq.ad_sequence_id=yrseq.ad_sequence_id "
            + " where yrseq.c_year_id= :year_id and seq.em_efin_isacctpaymentseq='Y' and seq.ad_org_id= :org_id ";
        Query genSequencelist = OBDal.getInstance().getSession().createSQLQuery(gsQuery);
        genSequencelist.setParameter("year_id", yearId);
        genSequencelist.setParameter("org_id", OrgId);
        if (genSequencelist.list().size() == 0) {
          ParentQury = " select eut_parent_org('" + OrgId + "','"
              + OBContext.getOBContext().getCurrentClient().getId() + "')";
          Query parentresult = OBDal.getInstance().getSession().createSQLQuery(ParentQury);
          Object parentOrg = parentresult.list().get(0);
          orgIds = ((String) parentOrg).split(",");
          for (int i = 0; i < orgIds.length; i++) {
            Query Sequencelist = OBDal.getInstance().getSession().createSQLQuery(gsQuery);
            Sequencelist.setParameter("year_id", yearId);
            Sequencelist.setParameter("org_id", orgIds[i].replace("'", ""));
            if (Sequencelist.list().size() > 0) {
              Object sequenceID = Sequencelist.list().get(0);
              sequenceId = (String) sequenceID;
              break;
            }
          }
        } else {
          Object sequenceID = genSequencelist.list().get(0);
          sequenceId = (String) sequenceID;
        }
        EfinYearSequence yearSequence = OBDal.getInstance().get(EfinYearSequence.class, sequenceId);
        if (yearSequence != null) {
          sequence = yearSequence.getNextAssignedNumber() == null ? ""
              : yearSequence.getNextAssignedNumber().toString();
          // sequence = Prefix.concat(sequence);
          if (action) {
            yearSequence.setNextAssignedNumber(yearSequence.getNextAssignedNumber() + 1);
          }
          OBDal.getInstance().save(yearSequence);
        }

      }
    } catch (Exception e) {
      // TODO: handle exception
      log4j.error("Exception in getGeneralSequence() Method : ", e);
      return "0";
    } finally {
      OBContext.restorePreviousMode();
    }
    return sequence;

  }

  /**
   * 
   * check Particular Role is reserver or not
   * 
   * @param document
   *          Type, Current Approverr Role Id
   * 
   * @return returns "'Y','N'"
   */
  public static boolean getReserveFundsRole(String doctype, String roleId, String OrgId,
      String invoiceId, BigDecimal value) {
    String ParentQury = "";
    String[] orgIds = null;
    String strAllowReservation = "";

    try {
      OBQuery<efinbudgetencum> line = OBDal.getInstance().createQuery(efinbudgetencum.class,
          " as e where e.invoice.id='" + invoiceId + "'");
      if (line.list().size() > 0) {
        return false;
      } else {
        // String reserveQuery = "select allowreservation from eut_documentrule_lines ln "
        // + " left join eut_documentrule_header hd on hd.eut_documentrule_header_id=
        // ln.eut_documentrule_header_id "
        // + " where allowreservation='Y' and document_type =:docType and ad_role_id=:roleId and
        // hd.ad_org_id=:orgId and hd.rulevalue <= :value ";

        // check current user is present in document rule or not
        String reserveQuery = "select allowreservation from eut_documentrule_lines ln where eut_documentrule_header_id = "
            + " (select eut_documentrule_header_id from eut_documentrule_header where document_type =:docType "
            + " and ad_org_id=:orgId and rulevalue <= :value and ad_client_id =:client order by rulevalue desc limit 1) ";
        Query reservelist = OBDal.getInstance().getSession().createSQLQuery(reserveQuery);
        reservelist.setParameter("docType", doctype);
        reservelist.setParameter("orgId", OrgId);
        reservelist.setParameter("value", value);
        reservelist.setParameter("client", OBContext.getOBContext().getCurrentClient().getId());
        if (reservelist.list().size() > 0) {

          // check role is reserve fund or not
          String reserveQuery1 = "select allowreservation from eut_documentrule_lines ln where eut_documentrule_header_id = "
              + " (select eut_documentrule_header_id from eut_documentrule_header where document_type =:docType "
              + " and ad_org_id=:orgId and rulevalue <= :value and ad_client_id =:client order by rulevalue desc limit 1) "
              + " and allowreservation='Y' and ln.ad_role_id=:roleId ";
          Query reservelist1 = OBDal.getInstance().getSession().createSQLQuery(reserveQuery1);
          reservelist1.setParameter("docType", doctype);
          reservelist1.setParameter("orgId", OrgId);
          reservelist1.setParameter("value", value);
          reservelist1.setParameter("roleId", roleId);
          reservelist1.setParameter("client", OBContext.getOBContext().getCurrentClient().getId());

          log4j.debug("reservelist:" + reservelist1);
          if (reservelist1.list().size() > 0) {
            Object allowReservation = reservelist1.list().get(0);
            strAllowReservation = (String) allowReservation.toString();
          } else {
            return false;
          }
        } else {
          // current user is not present in document rule then check parent org document rule
          String reserveQuery2 = "select allowreservation from eut_documentrule_lines ln where eut_documentrule_header_id = "
              + " (select eut_documentrule_header_id from eut_documentrule_header where document_type =:docType "
              + " and ad_org_id=:orgId and rulevalue <= :value and ad_client_id =:client order by rulevalue desc limit 1) "
              + " and allowreservation='Y' and ln.ad_role_id=:roleId ";

          ParentQury = " select eut_parent_org('" + OrgId + "','"
              + OBContext.getOBContext().getCurrentClient().getId() + "')";
          Query parentresult = OBDal.getInstance().getSession().createSQLQuery(ParentQury);
          Object parentOrg = parentresult.list().get(0);
          log4j.debug("parentOrg:" + parentOrg);
          orgIds = ((String) parentOrg).split(",");
          for (int i = 0; i < orgIds.length; i++) {
            Query parentReservelist = OBDal.getInstance().getSession()
                .createSQLQuery(reserveQuery2);
            parentReservelist.setParameter("docType", doctype);
            parentReservelist.setParameter("orgId", orgIds[i].replace("'", ""));
            parentReservelist.setParameter("value", value);
            parentReservelist.setParameter("roleId", roleId);
            parentReservelist.setParameter("client",
                OBContext.getOBContext().getCurrentClient().getId());

            if (parentReservelist.list().size() > 0) {
              Object allowReservation = parentReservelist.list().get(0);
              strAllowReservation = (String) allowReservation.toString();
              break;
            }
          }
        }

        if (strAllowReservation.equals("Y")) {
          return true;
        } else {
          return false;
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in getReserveFundsRole() Method : ", e);
      return false;
    }

  }

  /**
   * 
   * @param manualEncumbranceId
   * @param invoiceId
   * @param conversionrate
   * @return If sum of invoice Lines Amount greater than encumbrance remain amount then true else
   *         false
   */
  public static boolean checkManEncumbranceAmount(String manualEncumbranceId, String invoiceId,
      BigDecimal conversionrate) {

    PreparedStatement st = null;
    ResultSet rs = null;
    StringBuilder queryBuilder = new StringBuilder();
    try {

      queryBuilder
          .append(" select count(invline.c_invoiceline_id) as count from c_invoiceline invline ");
      queryBuilder.append(
          " join efin_budget_manencumlines encline on invline.em_efin_budgmanuencumln_id=encline.efin_budget_manencumlines_id ");
      queryBuilder.append("  where (invline.linenetamt * ").append(conversionrate)
          .append(" ) > encline.remaining_amount and invline.c_invoice_id='").append(invoiceId)
          .append("'");

      st = OBDal.getInstance().getConnection().prepareStatement(queryBuilder.toString());
      rs = st.executeQuery();
      if (rs.next()) {
        if (rs.getInt("count") > 0) {
          return true;
        } else {
          return false;
        }
      } else {
        return false;
      }

    }

    catch (final Exception e) {
      log4j.error("Exception in checkManEncumbranceAmount() Method : ", e);
      return false;
    }
  }

  /**
   * @param invoiceId
   * @param conversionrate
   * @return applied PrePayment Invoices
   */

  public static String checkAppliedPrepayment(String invoiceId, BigDecimal conversionrate) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String Sql = "";
    try {

      Sql = "select string_agg(inv.documentno,',') as preinvoice from c_invoice inv "
          + " join (select sum(applied_amount) as appamount,efin_applied_invoice,c_invoice_id from efin_applied_prepayment "
          + " group by efin_applied_invoice ,c_invoice_id)  as apppay on apppay.efin_applied_invoice =inv.c_invoice_id "
          + " where apppay.appamount > (inv.em_efin_pre_remainingamount *  " + conversionrate + " )"
          + " and apppay.c_invoice_id='" + invoiceId + "'";

      st = OBDal.getInstance().getConnection().prepareStatement(Sql);
      log4j.debug("appprainvoice:" + st.toString());
      rs = st.executeQuery();
      if (rs.next()) {
        if (null != rs.getString("preinvoice") && !"".equals(rs.getString("preinvoice"))) {
          return rs.getString("preinvoice");
        } else {
          return "";
        }
      } else {
        return "";
      }
    }

    catch (final Exception e) {
      log4j.error("Exception in checkAppliedPrepayment() Method : ", e);
      return "";
    }

  }

  /**
   * 
   * 
   * @param invoiceId
   * @return if Applied Amount Exceed the Invoice Total Amount Return '0'
   */
  public static String checkApplicationInvoiceAmount(String invoiceId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String Sql = "";
    try {
      Sql = " select count(inv.c_invoice_id) as invcount from c_invoice inv "
          + " join (select sum(applied_amount) as appamount,c_invoice_id  from efin_applied_prepayment "
          + " group by c_invoice_id )  as apppay on apppay.c_invoice_id =inv.c_invoice_id "
          + " where inv.grandtotal=apppay.appamount " + " and inv.c_invoice_id='" + invoiceId + "'";
      st = OBDal.getInstance().getConnection().prepareStatement(Sql);
      log4j.debug("application invoice rainvoice:" + st.toString());
      rs = st.executeQuery();
      if (rs.next()) {
        if (rs.getInt("invcount") > 0) {
          return "1";
        } else {
          return "0";
        }
      } else {
        return "0";
      }

    }

    catch (final Exception e) {
      log4j.error("Exception in checkApplicationInvoiceAmount() Method : ", e);
      return "0";
    }

  }

  /**
   * 
   * check Particular Role is department head or not
   * 
   * @param document
   *          clientId , Role Id
   * 
   * @return returns "'Y','N'"
   */
  public static boolean chkRoleIsDeptMang(String clientId, String roleId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      st = OBDal.getInstance().getConnection()
          .prepareStatement(" select em_efin_departmenthead from ad_role " + " where  ad_role_id='"
              + roleId + "' and ad_client_id='" + clientId
              + "' and ad_role_id not in ( select ad_role_id from  eut_delegate_role_check  where "
              + "                        (eut_forward_reqmoreinfo_id is not  null or eut_docapp_Delegateln_id is not null)) ");
      log4j.debug("reser:" + st.toString());
      rs = st.executeQuery();
      if (rs.next()) {
        if (rs.getString("em_efin_departmenthead").equals("Y"))
          return true;
        else
          return false;
      } else {
        return false;
      }
    }

    catch (final Exception e) {
      log4j.error("Exception in convertToGregorian() Method : ", e);
      return false;
    }
  }

  /**
   * This method is used to check more than one department head role
   * 
   * @param clientId
   * @param OrgId
   * @return
   */
  public static boolean chkMoThanOneDeptHeadRole(String clientId, String OrgId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      st = OBDal.getInstance().getConnection()
          .prepareStatement(" select count(ad_role_id) as count from ad_role "
              + " where em_efin_departmenthead ='Y' " + " and ad_role.ad_client_id='" + clientId
              + "' and ad_role_id not in ( select ad_role_id from  eut_delegate_role_check  where "
              + "                        (eut_forward_reqmoreinfo_id is not  null or eut_docapp_Delegateln_id is not null)) ");
      log4j.debug("chkMoThanOneDeptHeadRole:" + st.toString());
      rs = st.executeQuery();
      if (rs.next()) {
        if (rs.getInt("count") > 1)
          return true;
        else
          return false;
      }
    }

    catch (final Exception e) {
      log4j.error("Exception in convertToGregorian() Method : ", e);
      return false;
    }
    return true;
  }

  /**
   * This method is used to check user is department head
   * 
   * @param clientId
   * @param OrgId
   * @param userId
   * @param costCenterId
   * @return
   */
  public static boolean chkUserIsDeptHead(String clientId, String OrgId, String userId,
      String costCenterId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      st = OBDal.getInstance().getConnection().prepareStatement(
          " select count(*) as count from c_salesregion where em_efin_user_id= ? and c_salesregion_id = ? ");
      st.setString(1, userId);
      st.setString(2, costCenterId);
      log4j.debug("chkUserIsDeptHead:" + st.toString());
      rs = st.executeQuery();
      if (rs.next()) {
        if (rs.getInt("count") > 0)
          return true;
        else
          return false;
      }
    }

    catch (final Exception e) {
      log4j.error("Exception in chkUserIsDeptHead() Method : ", e);
      return false;
    }
    return true;
  }

  /**
   * This method is used to check user is department head invoice
   * 
   * @param clientId
   * @param OrgId
   * @param userId
   * @param invoiceId
   * @return
   */
  public static boolean chkUserIsDeptHeadInvoice(String clientId, String OrgId, String userId,
      String invoiceId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      st = OBDal.getInstance().getConnection().prepareStatement(
          " select count(*) as count from c_salesregion dept join c_invoice inv on inv.EM_Efin_C_Salesregion_ID=dept.c_salesregion_id where em_efin_user_id= ? and inv.c_invoice_id= ? ");
      st.setString(1, userId);
      st.setString(2, invoiceId);
      log4j.debug("chkUserIsDeptHead:" + st.toString());
      rs = st.executeQuery();
      if (rs.next()) {
        if (rs.getInt("count") > 0)
          return true;
        else
          return false;
      }
    }

    catch (final Exception e) {
      log4j.error("Exception in chkUserIsDeptHeadInvoice() Method : ", e);
      return false;
    }
    return true;
  }

  /**
   * This method is used to get department head role
   * 
   * @param clientId
   * @param paramOrgId
   * @param DocType
   * @param pValue
   * @return
   */
  public static String getDeptHeadRole(String clientId, String paramOrgId, String DocType,
      Object pValue) {
    PreparedStatement st = null;
    ResultSet rs = null;
    boolean chkRoleIsDeptMang = false;
    String DeptHeadRoleId = null, OrgId = paramOrgId;
    try {
      st = OBDal.getInstance().getConnection()
          .prepareStatement("select eut_documentrule_parentorg(?, ?, ?);");
      st.setString(1, clientId);
      st.setString(2, OrgId);
      st.setString(3, DocType);
      rs = st.executeQuery();
      if (rs.next())
        OrgId = rs.getString("eut_documentrule_parentorg");

      st = OBDal.getInstance().getConnection().prepareStatement(
          " select ln.ad_role_id from eut_documentrule_header hd left join eut_documentrule_lines ln on ln.eut_documentrule_header_id= hd.eut_documentrule_header_id  where hd.ad_client_id =? and hd.ad_org_id=?  and hd.document_type='"
              + DocType + "' order by rolesequenceno");
      st.setString(1, clientId);
      st.setString(2, OrgId);
      log4j.debug("getdeptrole:" + st.toString());
      rs = st.executeQuery();
      while (rs.next()) {
        chkRoleIsDeptMang = chkRoleIsDeptMang(clientId, rs.getString("ad_role_id"));
        log4j.debug("chkRoleIsDeptMang12:" + chkRoleIsDeptMang);
        if (chkRoleIsDeptMang) {
          DeptHeadRoleId = rs.getString("ad_role_id");
          log4j.debug("DeptHeadRoleId:" + DeptHeadRoleId);
        }
      }
      return DeptHeadRoleId;
    }

    catch (final Exception e) {
      log4j.error("Exception in getDeptHeadRole() Method : ", e);
      return DeptHeadRoleId;
    }
  }

  /*
   * This mothod return whether passing role is in document rule or not.
   */
  public static boolean chkRoleIsInDocRul(Connection con, String clientId, String paramOrgId,

      String userId, String roleId, String documentType, Object pValue) {

    ResultSet rs = null;
    PreparedStatement st = null;
    BigDecimal value = new BigDecimal(0);
    String orgId = paramOrgId;
    String multiRule = "N";
    String sql = "";

    try {

      // Getting Organization
      st = con.prepareStatement("select eut_documentrule_parentorg(?, ?, ?);");
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      rs = st.executeQuery();
      if (rs.next())
        orgId = rs.getString("eut_documentrule_parentorg");
      if (pValue instanceof Double)
        value = new BigDecimal((Double) pValue);
      else if (pValue instanceof Float)
        value = new BigDecimal((Float) pValue);
      else
        value = new BigDecimal(pValue.toString());
      st = con.prepareStatement(
          "select distinct ismultirule from eut_documentrule_header qdrh where qdrh.ad_client_id = ? and qdrh.ad_org_id= ?"
              + " and qdrh.document_type = ? and qdrh.rulevalue <= ? ");
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      st.setBigDecimal(4, value);
      rs = st.executeQuery();
      if (rs.next()) {
        multiRule = rs.getString("ismultirule");
      }
      sql = " select count(ln.ad_role_id) as count from eut_documentrule_lines ln where ln.eut_documentrule_header_id in ("
          + "select qdrh.eut_documentrule_header_id " + "from eut_documentrule_header qdrh "
          + "where qdrh.ad_client_id = ? and qdrh.ad_org_id= ? "
          + "and qdrh.document_type = ? and qdrh.rulevalue <= ? "
          + "group by qdrh.rulevalue,qdrh.eut_documentrule_header_id "
          + "order by qdrh.rulevalue desc ";
      if (multiRule.equals("Y"))
        sql += " ) and ln.ad_role_id=? ";
      else
        sql += " limit 1) and ln.ad_role_id=? ";
      st = con.prepareStatement(sql);
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      st.setBigDecimal(4, value);
      st.setString(5, roleId);
      log4j.debug("chkRoleIsInDocRul count:" + st.toString());
      rs = st.executeQuery();
      if (rs.next()) {
        if (rs.getInt("count") > 0) {
          return true;
        }
      } else {
        return false;
      }

    } catch (final Exception e) {
      log4j.error("Exception in chkRoleIsInDocRul() Method", e);
      return false;
    }
    return false;

  }

  /*
   * This mothod return whether passing role is in document rule or not.
   */
  public static boolean chkRoleIsInDocRulBasedonAmount(Connection con, String clientId,
      String paramOrgId, String userId, String roleId, String documentType, Object pValue) {
    ResultSet rs = null;
    PreparedStatement st = null;
    BigDecimal value = new BigDecimal(0);
    String orgId = paramOrgId;
    try {
      // Getting Organization
      st = con.prepareStatement("select eut_documentrule_parentorg(?, ?, ?);");
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      rs = st.executeQuery();
      if (rs.next())
        orgId = rs.getString("eut_documentrule_parentorg");

      if (pValue instanceof Double)
        value = new BigDecimal((Double) pValue);
      else if (pValue instanceof Float)
        value = new BigDecimal((Float) pValue);
      else
        value = new BigDecimal(pValue.toString());

      st = con.prepareStatement(
          "select count(ln.ad_role_id) as count from eut_documentrule_lines ln where ln.eut_documentrule_header_id in ("
              + "select qdrh.eut_documentrule_header_id " + "from eut_documentrule_header qdrh "
              + "where qdrh.ad_client_id = ? and qdrh.ad_org_id= ? "
              + "and qdrh.document_type = ? and qdrh.rulevalue <= ? "
              + "group by qdrh.rulevalue,qdrh.eut_documentrule_header_id "
              + "order by qdrh.rulevalue desc limit 1) " + "and ln.ad_role_id=? ");
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      st.setBigDecimal(4, value);
      st.setString(5, roleId);
      log4j.debug("chkRoleIsInDocRul count:" + st.toString());
      rs = st.executeQuery();
      if (rs.next()) {
        if (rs.getInt("count") > 0) {
          return true;
        }
      } else {
        return false;
      }
    } catch (final Exception e) {
      log4j.error("Exception in chkRoleIsInDocRul() Method", e);
      return false;
    }
    return false;
  }

  /**
   * Method to return true if current role is having delegation access for invoices approval flow.
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param userId
   * @param roleId
   * @param invoiceId
   * @return true or false
   */
  public static boolean chkRoleIsInAppDelegation(Connection con, String clientId, String orgId,
      String userId, String roleId, String invoiceId) {
    ResultSet rs = null;
    PreparedStatement st = null;
    try {
      st = con.prepareStatement("select count(ln.eut_docapp_delegateln_id) as count "
          + "from eut_docapp_delegateln ln "
          + "join eut_docapp_delegate hd on hd.eut_docapp_delegate_id = ln.eut_docapp_delegate_id "
          + "where ln.ad_role_id=? and ln.ad_user_id=? and cast(now() as date) between cast(hd.from_date as date) and cast(hd.to_date as date) "
          + "and ln.document_type = (select case when doc.EM_Efin_Isprepayinv='Y' then 'EUT_110' "
          + "when doc.EM_Efin_Isprepayinvapp='Y' then 'EUT_109' else 'EUT_101' end "
          + "from c_invoice inv "
          + "join c_doctype doc on doc.c_doctype_id=inv.c_doctypetarget_id where inv.c_invoice_id=?)");
      st.setString(1, roleId);
      st.setString(2, userId);
      st.setString(3, invoiceId);
      rs = st.executeQuery();
      if (rs.next()) {
        if (rs.getInt("count") > 0) {
          return true;
        }
      } else {
        return false;
      }
    } catch (final Exception e) {
      log4j.error("Exception in chkRoleIsInAppDelegation() Method", e);
      return false;
    }
    return false;
  }

  /**
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param userId
   * @param roleId
   * @param invoiceId
   * @return
   */
  public static UtilityVO roleInAppDelegation(Connection con, String clientId, String orgId,
      String userId, String roleId, String invoiceId) {
    ResultSet rs = null;
    PreparedStatement st = null;
    UtilityVO vo = new UtilityVO();
    try {
      st = con.prepareStatement("select hd.ad_role_id as role,hd.ad_user_id as user "
          + "from eut_docapp_delegateln ln "
          + "join eut_docapp_delegate hd on hd.eut_docapp_delegate_id = ln.eut_docapp_delegate_id "
          + "where ln.ad_role_id=? and ln.ad_user_id=? and cast(now() as date) between cast(hd.from_date as date) and cast(hd.to_date as date) "
          + "and ln.document_type = (select case when doc.EM_Efin_Isprepayinv='Y' then 'EUT_110' "
          + "when doc.EM_Efin_Isprepayinvapp='Y' then 'EUT_109' else 'EUT_101' end "
          + "from c_invoice inv "
          + "join c_doctype doc on doc.c_doctype_id=inv.c_doctypetarget_id where inv.c_invoice_id=? limit 1)");
      st.setString(1, roleId);
      st.setString(2, userId);
      st.setString(3, invoiceId);
      rs = st.executeQuery();
      if (rs.next()) {
        vo.setRoleId(rs.getString("role"));
        vo.setUserId(rs.getString("user"));
      }
      return vo;
    } catch (final Exception e) {
      log4j.error("Exception in chkRoleIsInAppDelegation() Method", e);
    }
    return vo;
  }

  /**
   * Method to return a {@link AccountingCombination} ID satisying the dimensions. creates a new one
   * if no combination exists.
   * 
   * @param dimensions
   *          a {@link JSONObject} that contains all the dimensions.
   * 
   * @return {@link String} valid combination Id.
   * 
   */

  public static String getValidCombination(JSONObject dimensions) {
    String strValidCombinationId = "";
    StringBuilder whereClause = new StringBuilder();
    List<AccountingCombination> combinations = new ArrayList<AccountingCombination>();
    PreparedStatement ps = null;
    Connection con = null;
    String strOrgId = "";
    String strAccountId = "", strFuture2 = "", strAccountingSchemaId = "";
    String strDepartment = "", strBudgetType = "", strProject = "", strClassification = "",
        strFuture1 = "";
    String strBPartnerId = "";
    String strUniqueCode = "";

    try {
      OBContext.setAdminMode();

      con = OBDal.getInstance().getConnection();

      if (dimensions.has("Account") && dimensions.getString("Account").length() == 32) {
        strAccountId = dimensions.getString("Account");
        whereClause.append("account.id='").append(strAccountId).append("' ");
      }

      if (dimensions.has("Organization") && dimensions.getString("Organization").length() == 32) {
        strOrgId = dimensions.getString("Organization");
        whereClause.append("and organization.id='").append(strOrgId).append("' ");
      }

      if (dimensions.has("Department") && dimensions.getString("Department").length() == 32) {
        strDepartment = dimensions.getString("Department");
        whereClause.append("and salesRegion.id='").append(strDepartment).append("' ");
      }

      if (dimensions.has("Budget_Type") && dimensions.getString("Budget_Type").length() == 32) {
        strBudgetType = dimensions.getString("Budget_Type");
        whereClause.append("and salesCampaign.id='").append(strBudgetType).append("' ");
      }

      if (dimensions.has("Project") && dimensions.getString("Project").length() == 32) {
        strProject = dimensions.getString("Project");
        whereClause.append("and project.id='").append(strProject).append("' ");
      }

      if (dimensions.has("Classification")
          && dimensions.getString("Classification").length() == 32) {
        strClassification = dimensions.getString("Classification");
        whereClause.append("and activity.id='").append(strClassification).append("' ");
      }

      if (dimensions.has("Future1") && dimensions.getString("Future1").length() == 32) {
        strFuture1 = dimensions.getString("Future1");
        whereClause.append("and stDimension.id='").append(strFuture1).append("' ");
      }

      if (dimensions.has("Future2") && dimensions.getString("Future2").length() == 32) {
        strFuture2 = dimensions.getString("Future2");
        whereClause.append("and ndDimension.id='").append(dimensions.getString("Future1"))
            .append("' ");
      }

      if (dimensions.has("UniqueCode") && dimensions.getString("UniqueCode").length() > 0)
        strUniqueCode = dimensions.getString("UniqueCode");

      if (dimensions.has("BPartner") && dimensions.getString("BPartner").length() == 32)
        strBPartnerId = dimensions.getString("BPartner");

      log4j.debug("Wherclause: " + whereClause.toString());

      OBQuery<AccountingCombination> combination = OBDal.getInstance()
          .createQuery(AccountingCombination.class, whereClause.toString());
      if (combination != null) {
        combinations = combination.list();
        if (combinations.size() > 0) {
          strValidCombinationId = combinations.get(0).getId();
        } else {
          whereClause = new StringBuilder();

          strAccountingSchemaId = getAccountingSchema(strOrgId);

          whereClause.append(
              " INSERT INTO c_validcombination(c_validcombination_id, ad_client_id, ad_org_id, createdby, updatedby, ");
          whereClause.append(
              " c_acctschema_id, account_id, c_bpartner_id,  c_salesregion_id, c_project_id, c_campaign_id, c_activity_id, user1_id, user2_id,em_efin_uniquecode) ");
          whereClause.append(" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

          ps = con.prepareStatement(whereClause.toString());
          strValidCombinationId = SequenceIdData.getUUID();

          ps.setString(1, strValidCombinationId);
          ps.setString(2, OBContext.getOBContext().getCurrentClient().getId());
          ps.setString(3, strOrgId);
          ps.setString(4, OBContext.getOBContext().getUser().getId());
          ps.setString(5, OBContext.getOBContext().getUser().getId());
          ps.setString(6, strAccountingSchemaId);
          ps.setString(7, strAccountId);
          ps.setString(8, StringUtils.isEmpty(strBPartnerId) ? null : strBPartnerId);
          ps.setString(9, StringUtils.isEmpty(strDepartment) ? null : strDepartment);
          ps.setString(10, StringUtils.isEmpty(strProject) ? null : strProject);
          ps.setString(11, StringUtils.isEmpty(strBudgetType) ? null : strBudgetType);
          ps.setString(12, StringUtils.isEmpty(strClassification) ? null : strClassification);
          ps.setString(13, StringUtils.isEmpty(strFuture1) ? null : strFuture1);
          ps.setString(14, StringUtils.isEmpty(strFuture2) ? null : strFuture2);
          ps.setString(15, StringUtils.isEmpty(strUniqueCode) ? null : strUniqueCode);

          ps.executeUpdate();
          if (!con.getAutoCommit())
            con.commit();
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getValidCombination() Method", e);
      strValidCombinationId = "";
    } finally {
      OBContext.restorePreviousMode();
    }
    return strValidCombinationId;
  }

  /**
   * Method to get the accounting schema associated with {@link Organization} If the Organization is
   * not linked with any schema, it is fetched from the Organization Tree.
   * 
   * @param strOrgId
   *          Primary key of the Organization.
   * @return {@link String} Primary key of {@link AcctSchema}
   * 
   */
  public static String getAccountingSchema(String strOrgId) {
    String strSchemaId = "";
    OBQuery<AcctSchema> obQuery = null;

    try {
      obQuery = OBDal.getInstance().createQuery(AcctSchema.class,
          " where organization.id='" + strOrgId + "'");

      if (obQuery != null && obQuery.list().size() > 0) {
        strSchemaId = obQuery.list().get(0).getId();
        return strSchemaId;
      } else {
        strSchemaId = getAccountingSchema(
            OBContext.getOBContext().getOrganizationStructureProvider().getParentOrg(strOrgId));
        return strSchemaId;
      }
    } catch (Exception e) {
      log4j.error("Exception while getting Accounting Schema: ", e);
      return strSchemaId;
    }
  }

  /**
   * Method returns the {@link FIN_FinancialAccountAccounting} ID of the bank.
   * 
   * @param strOrgId
   *          {@link Organization} ID.
   * @param strFinAccountId
   *          {@link FIN_FinancialAccount} ID.
   * 
   * @return {@link String} Account Configuration ID.
   * 
   */

  public static String getAccountingConfig(String strOrgId, String strFinAccountId) {
    String strAccountConfigId = "";
    OBQuery<FIN_FinancialAccountAccounting> obQuery = null;

    try {
      obQuery = OBDal.getInstance().createQuery(FIN_FinancialAccountAccounting.class,
          " where account.id = '" + strFinAccountId + "' and organization.id='" + strOrgId + "'");

      if (obQuery != null && obQuery.list().size() > 0) {
        strAccountConfigId = obQuery.list().get(0).getId();
        return strAccountConfigId;
      } else {
        strAccountConfigId = getAccountingConfig(
            OBContext.getOBContext().getOrganizationStructureProvider().getParentOrg(strOrgId),
            strFinAccountId);
        return strAccountConfigId;
      }
    } catch (Exception e) {
      log4j.error("Exception while getting Accounting Schema: ", e);
      return strAccountConfigId;
    }
  }

  /**
   * This method returns the {@link Calendar} ID assoicated with the Organization. if calendar is
   * not associated, calendar is retrieved from the Organization Tree.
   * 
   * @param strOrgId
   *          {@link Organization} ID.
   * @return {@link String} Calendar ID.
   * 
   */
  public static String getCalendar(String strOrgId) {
    String strCalId = "";
    Organization org = null;
    try {
      OBContext.setAdminMode();

      org = Utility.getObject(Organization.class, strOrgId);
      if (org.getCalendar() != null) {
        return org.getCalendar().getId();
      } else {
        strCalId = getCalendar(
            OBContext.getOBContext().getOrganizationStructureProvider().getParentOrg(strOrgId));
        return strCalId;
      }
    } catch (Exception e) {
      log4j.error("Exception while getting getCalendar: ", e);
      return strCalId;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Returns the {@link Period} based the provided date.
   * 
   * @param strDate
   *          Date in the format dd-MM-yyyy
   * @param strOrgId
   *          {@link Organization} ID.
   * 
   * @return {@link String} Period Id.
   */
  public static String getPeriod(String strDate, String strOrgId) {
    String strPeriodID = "";
    String strCalId = "";
    String strQuery = "";
    Query query = null;

    try {
      OBContext.setAdminMode();

      strCalId = getCalendar(strOrgId);

      strQuery = " select p.id from FinancialMgmtPeriod p  join p.year y ";
      strQuery = strQuery + " join y.calendar c  where to_date('" + strDate
          + "','dd-MM-YYYY')  between cast(p.startingDate as date) and cast(p.endingDate as date) ";
      strQuery = strQuery + " and c.id = '" + strCalId + "'";

      query = OBDal.getInstance().getSession().createQuery(strQuery);

      if (query != null && query.list().size() > 0) {
        strPeriodID = (String) query.list().get(0);
      }

      return strPeriodID;
    } catch (Exception e) {
      log4j.error("Exception while getting getPeriod: ", e);
      return strPeriodID;
    } finally {
    }
  }

  /**
   * 
   * @param birthDate
   * @return Age
   */
  @SuppressWarnings("unused")
  public static String CalculateAge(Date birthDate, String ClientId) {

    int years = 0;
    int months = 0;
    int days = 0;
    String strNowmonth = "";

    SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
    // create calendar object for birth day
    try {

      // convert birthDate To hijriDate
      String strbirthDate = convertTohijriDate(dateYearFormat.format(birthDate));
      // create calendar object for current day
      String strtodayDate = convertTohijriDate(dateYearFormat.format(new Date()));

      int dobyear = Integer.parseInt(strbirthDate.split("-")[2]);
      int dobmonth = Integer.parseInt(strbirthDate.split("-")[1]);
      int dobdate = Integer.parseInt(strbirthDate.split("-")[0]);
      log4j.debug("dobyear>>dobmonth>>dobdate" + dobyear + "-" + dobmonth + "-" + dobdate);
      int nowyear = Integer.parseInt(strtodayDate.split("-")[2]);
      int nowmonth = Integer.parseInt(strtodayDate.split("-")[1]);
      int nowdate = Integer.parseInt(strtodayDate.split("-")[0]);

      years = nowyear - dobyear;
      months = nowmonth - dobmonth;
      if (months < 0) {
        years--;
        months = 12 - dobmonth + nowmonth;
        if (nowdate < dobdate) {
          months--;
        }
      } else if (months == 0 && nowdate < dobdate) {
        years--;
        years = 11;

      }
      if (nowdate > dobdate) {
        days = nowdate - dobdate;

      } else if (nowdate < dobdate) {
        int today = nowdate;
        nowmonth = nowmonth - 1;
        if (nowmonth < 10) {
          strNowmonth = "0" + String.valueOf(nowmonth);
        } else {
          strNowmonth = String.valueOf(nowmonth);
        }
        int maxCurrentDate = getDays(ClientId, strNowmonth);
        days = maxCurrentDate - dobdate + today;

      } else {
        days = 0;
        if (months == 12) {
          years++;
          months = 0;
        }

      }
    } catch (Exception e) {
      log4j.error("Exception in CalculateAge", e);
    }
    return String.valueOf(years) + " Years" + String.valueOf(months) + " Months";
  }

  /**
   * 
   * @param StartDate
   * @param EndDate
   * @return totalMonths
   */
  @SuppressWarnings("unused")
  public static int calculateMonths(String strstartDate, String strendDate, String ClientId) {
    int years = 0;
    int months = 0;
    int days = 0;
    String strNowmonth = "";

    // create calendar object for birth day
    try {

      // convert birthDate To hijriDate
      String strbirthDate = strstartDate;
      // create calendar object for current day
      String strtodayDate = strendDate;

      int dobyear = Integer.parseInt(strbirthDate.split("-")[2]);
      int dobmonth = Integer.parseInt(strbirthDate.split("-")[1]);
      int dobdate = Integer.parseInt(strbirthDate.split("-")[0]);
      log4j.debug("dobyear>>dobmonth>>dobdate" + dobyear + "-" + dobmonth + "-" + dobdate);
      int nowyear = Integer.parseInt(strtodayDate.split("-")[2]);
      int nowmonth = Integer.parseInt(strtodayDate.split("-")[1]);
      int nowdate = Integer.parseInt(strtodayDate.split("-")[0]);

      years = nowyear - dobyear;
      months = nowmonth - dobmonth;
      if (months < 0) {
        years--;
        months = 12 - dobmonth + nowmonth;
        if (nowdate < dobdate) {
          months--;
        }
      } else if (months == 0 && nowdate < dobdate) {
        years--;
        months = 11;

      } else if (months > 1 && nowdate < dobdate) {
        months--;
      }

      if (nowdate > dobdate) {
        days = nowdate - dobdate;

      } else if (nowdate < dobdate) {
        int today = nowdate;
        nowmonth = nowmonth - 1;
        if (nowmonth < 10) {
          strNowmonth = "0" + String.valueOf(nowmonth);
        } else {
          strNowmonth = String.valueOf(nowmonth);
        }
        int maxCurrentDate = getDays(ClientId, strNowmonth);
        days = maxCurrentDate - dobdate + today;

      } else {
        days = 0;
        if (months == 12) {
          years++;
          months = 0;
        }

      }
      months = (years * 12) + months;
    } catch (Exception e) {
      log4j.error("Exception in calculateMonths", e);
    }
    return months;
  }

  /**
   * 
   * @param clientId
   * @param monthyear
   * @return return Remaining Days
   */
  public static int getDays(String clientId, String monthyear) {
    PreparedStatement st = null;
    ResultSet rs = null;
    int total = 0;
    try {
      st = OBDal.getInstance().getConnection().prepareStatement(
          " select count(*) as total from eut_hijri_dates  where hijri_date ilike '%" + monthyear
              + "%'  group by  ad_org_id limit 1 ");
      // st.setString(1, clientId);
      log4j.debug("getDays:" + st.toString());
      rs = st.executeQuery();
      if (rs.next()) {
        if (rs.getInt("total") > 0) {
          total = rs.getInt("total");
        }
      }
    } catch (final SQLException e) {
      log4j.error("Exception in getDays", e);
      return 0;
    } catch (final Exception e) {
      log4j.error("Exception in getDays", e);
      return 0;
    } finally {
      try {
        st.close();
        rs.close();
      } catch (final SQLException e) {
        log4j.error("Exception in getDays", e);
      }
    }
    return total;
  }

  /**
   * 
   * @param strstartDate
   * @param strendDate
   * @return True if startDate and End Date period with in one year
   */
  public static Boolean yearValidation(String strstartDate, String strendDate) {
    int years = 0;
    int months = 0;
    int days = 0;
    Boolean validation = Boolean.TRUE;

    try {

      int startyear = Integer.parseInt(strstartDate.split("-")[2]);
      int startmonth = Integer.parseInt(strstartDate.split("-")[1]);
      int startdate = Integer.parseInt(strstartDate.split("-")[0]);
      log4j.debug(
          "startyear>>startmonth>>startdate" + startyear + "-" + startmonth + "-" + startdate); // 17-03-1442
      int endyear = Integer.parseInt(strendDate.split("-")[2]);
      int endmonth = Integer.parseInt(strendDate.split("-")[1]);
      int enddate = Integer.parseInt(strendDate.split("-")[0]); // 15-03-1442
      years = endyear - startyear;
      if (years == 0) {
        validation = Boolean.TRUE;
      } else if (years >= 2) {
        validation = Boolean.FALSE;
      } else if (years == 1) { // check with year diff 1
        months = endmonth - startmonth;
        if (months < 0) {// check months with in one year ,yes then allow
          validation = Boolean.TRUE;
        } else if (months > 0) {// check months with in one year ,no then dnt allow
          validation = Boolean.FALSE;
        } else if (months == 0) { // same month check dates
          days = enddate - startdate;
          if (days > 0) {
            validation = Boolean.FALSE;
          } else if (days < 0) {
            validation = Boolean.TRUE;
          } else if (days == 0) {
            validation = Boolean.FALSE;
          }
        }
      } else {
        validation = Boolean.TRUE;
      }

    } catch (Exception e) {
      log4j.error("Exception in yearValidation", e);
    }
    return validation;
  }

  /**
   * 
   * @param strstartDate
   * @param strendDate
   * @return True if startDate and End Date period with in one year
   */
  public static Boolean periodyearValidation(String strstartDate, String strendDate, int period) {
    int years = 0;
    int months = 0;
    int days = 0;
    Boolean validation = Boolean.TRUE;

    try {

      int startyear = Integer.parseInt(strstartDate.split("-")[2]);
      int startmonth = Integer.parseInt(strstartDate.split("-")[1]);
      int startdate = Integer.parseInt(strstartDate.split("-")[0]);
      log4j.debug(
          "startyear>>startmonth>>startdate" + startyear + "-" + startmonth + "-" + startdate);
      int endyear = Integer.parseInt(strendDate.split("-")[2]);
      int endmonth = Integer.parseInt(strendDate.split("-")[1]);
      int enddate = Integer.parseInt(strendDate.split("-")[0]);
      years = endyear - startyear;
      log4j.debug("years:" + years);
      if (years > period) {
        validation = Boolean.TRUE;
      } else if (years < period) {
        validation = Boolean.FALSE;
      } else if (years == period) {
        months = endmonth - startmonth;
        if (months > 0) {
          validation = Boolean.TRUE;
        } else if (months < 0) {
          validation = Boolean.FALSE;
        } else if (months == 0) {
          days = enddate - startdate;

          if (days > 0) {
            validation = Boolean.TRUE;
          } else if (days < 0) {
            validation = Boolean.FALSE;
          } else if (days == 0) {
            validation = Boolean.TRUE;
          }
        }
      }
      log4j.debug("validation:" + validation);
    } catch (Exception e) {
      log4j.error("Exception in fouryearValidation", e);
    }
    return validation;
  }

  /**
   * 
   * @param strstartDate
   * @param strendDate
   * @param ClientId
   * @param yearflag
   * @return
   */
  public static int calculateMonths(String strstartDate, String strendDate, String ClientId,
      boolean yearflag) {
    int years = 0;
    int months = 0;
    int days = 0;
    BigInteger maxCurrentDate = BigInteger.ZERO;
    String strNowmonth = "", strQuery = "";
    SQLQuery query = null;

    // create calendar object for birth day
    try {

      // convert birthDate To hijriDate
      String strbirthDate = strstartDate;
      // create calendar object for current day
      String strtodayDate = strendDate;

      int dobyear = Integer.parseInt(strbirthDate.split("-")[2]);
      int dobmonth = Integer.parseInt(strbirthDate.split("-")[1]);
      int dobdate = Integer.parseInt(strbirthDate.split("-")[0]);
      log4j.debug("dobyear>>dobmonth>>dobdate" + dobyear + "-" + dobmonth + "-" + dobdate);
      int nowyear = Integer.parseInt(strtodayDate.split("-")[2]);
      int nowmonth = Integer.parseInt(strtodayDate.split("-")[1]);
      int nowdate = Integer.parseInt(strtodayDate.split("-")[0]);
      log4j.debug("nowyear>>nowmonth>>nowdate" + nowyear + "-" + nowmonth + "-" + nowdate);

      years = nowyear - dobyear;
      log4j.debug("years" + years);
      months = nowmonth - dobmonth;
      log4j.debug("months" + months);
      if (months < 0) {
        years--;
        months = 12 - dobmonth + nowmonth;
        log4j.debug("months less than 0" + months);
        if (nowdate < dobdate) {
          months--;
        }
      } else if (months == 0 && nowdate < dobdate) {
        years--;
        months = 11;

      } else if (months > 1 && nowdate < dobdate) {
        months--;
      }

      if (nowdate > dobdate) {
        days = nowdate - dobdate;

      } else if (nowdate < dobdate) {
        int today = nowdate;
        nowmonth = nowmonth - 1;
        log4j.debug("today" + years);
        if (nowmonth < 10) {
          strNowmonth = "0" + String.valueOf(nowmonth);
        } else {
          strNowmonth = String.valueOf(nowmonth);
        }
        // int maxCurrentDate = getDays(ClientId, strNowmonth);
        strNowmonth = nowyear + strNowmonth;
        strQuery = " select count(a.hijri_date) from (select max(hijri_date) as hijri_date, gregorian_date from eut_hijri_dates  where  hijri_date ilike  '%"
            + strNowmonth + "%'  group by  gregorian_date) a ";
        query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
        // query.setParameter(1, ClientId);
        log4j.debug("strQuery:" + strQuery);
        if (query != null && query.list().size() > 0) {
          Object row = query.list().get(0);
          maxCurrentDate = (BigInteger) row;
        }
        days = maxCurrentDate.intValue() - dobdate + today;
        log4j.debug("days" + days);

      } else {
        days = 0;
        if (months == 12) {
          years++;
          months = 0;
        }

      }
      log4j.debug("years" + years);
      log4j.debug("months" + months);
      months = (years * 12) + months;
      log4j.debug("months" + months);

      if (yearflag)
        months = years;
    } catch (Exception e) {
      log4j.error("Exception in calculateMonths", e);
    }
    return months;
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
  @SuppressWarnings("unchecked")
  public static Boolean alertInsertionPreference(String DocumentId, String DocumentNo,
      String property, String clientId, String description, String status) {
    String sqlQuery = "";
    SQLQuery query = null;
    Boolean isSuccess = Boolean.TRUE;
    String alertRuleId = "";
    ArrayList<String> includeRecipient = new ArrayList<String>();
    try {
      // get Requisition Alert
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.escmIsrequisition='Y'");
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      // get alert recipients
      OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
          .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

      sqlQuery = "select visibleat_role_id  from ad_preference where property='" + property
          + "' and ad_client_id='" + clientId + "' ";
      query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);

      List<String> ruleList = (ArrayList<String>) query.list();
      if (ruleList != null && ruleList.size() > 0) {
        for (int i = 0; i < ruleList.size(); i++) {
          String object = (String) ruleList.get(i);
          Alert objAlert = OBProvider.getInstance().get(Alert.class);
          objAlert.setClient(OBDal.getInstance().get(Client.class, clientId));
          objAlert.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
          objAlert.setAlertRule(OBDal.getInstance().get(AlertRule.class, alertRuleId));
          // imported via data set
          objAlert.setDescription(description);
          objAlert.setRole(OBDal.getInstance().get(Role.class, object.toString()));
          objAlert.setRecordID(DocumentNo);
          objAlert.setReferenceSearchKey(DocumentId);
          objAlert.setAlertStatus(status);
          OBDal.getInstance().save(objAlert);
          OBDal.getInstance().flush();
          includeRecipient.add(object.toString());
        }
        isSuccess = Boolean.TRUE;
      }
      // check and insert Recipient
      if (receipientQuery.list().size() > 0) {
        for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
          includeRecipient.add(objAlertReceipient.getRole().getId());
          OBDal.getInstance().remove(objAlertReceipient);
        }
      }
      // avoid duplicate recipient
      HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
      Iterator<String> iterator = incluedSet.iterator();
      while (iterator.hasNext()) {
        Utility.insertAlertRecipient(iterator.next(), clientId);
      }

    } catch (Exception e) {
      isSuccess = Boolean.FALSE;
      log4j.error("Exception in alertInsertionPreference", e);
    }
    return isSuccess;
  }

  /**
   * This method only for Purchase Requisition Alert Process
   * 
   * @param DocumentId
   * @param DocumentNo
   * @param roleId
   * @param clientId
   * @param description
   * @param status
   * @return True --Alert Created, False --Error
   */
  public static Boolean alertInsertionRole(String DocumentId, String DocumentNo, String roleId,
      String userId, String clientId, String description, String status) {
    Boolean isSuccess = Boolean.TRUE;
    String alertRuleId = "";
    try {
      // get Requisition Alert
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.escmIsrequisition='Y'");
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }

      Alert objAlert = OBProvider.getInstance().get(Alert.class);
      objAlert.setClient(OBDal.getInstance().get(Client.class, clientId));
      objAlert.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
      objAlert.setAlertRule(OBDal.getInstance().get(AlertRule.class, alertRuleId));
      // imported via data set
      objAlert.setDescription(description);
      if (!roleId.isEmpty() && !roleId.equals("")) {
        objAlert.setRole(OBDal.getInstance().get(Role.class, roleId));
      }
      if (!userId.isEmpty() && !userId.equals("")) {
        objAlert.setUserContact(OBDal.getInstance().get(User.class, userId));
      }
      objAlert.setRecordID(DocumentNo);
      objAlert.setReferenceSearchKey(DocumentId);
      objAlert.setAlertStatus(status);
      OBDal.getInstance().save(objAlert);
      OBDal.getInstance().flush();

    } catch (Exception e) {
      isSuccess = Boolean.FALSE;
      log4j.error("Exception in alertInsertionRole", e);
    }
    return isSuccess;
  }

  /**
   * 
   * @param roleId
   * @param clientId
   * @return True --Alert Recipient Created, False --Error
   */
  public static Boolean insertAlertRecipient(String roleId, String clientId) {
    Boolean isSuccess = Boolean.TRUE;
    String alertRuleId = "";
    try {

      // get Requisition Alert
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.escmIsrequisition='Y'");
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      AlertRecipient objAlertRecipient = OBProvider.getInstance().get(AlertRecipient.class);
      objAlertRecipient.setClient(OBDal.getInstance().get(Client.class, clientId));
      objAlertRecipient.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
      objAlertRecipient.setAlertRule(OBDal.getInstance().get(AlertRule.class, alertRuleId));
      objAlertRecipient.setRole(OBDal.getInstance().get(Role.class, roleId));
      objAlertRecipient.setSendEMail(false);
      OBDal.getInstance().save(objAlertRecipient);
      OBDal.getInstance().flush();

    } catch (Exception e) {
      isSuccess = Boolean.FALSE;
      log4j.error("Exception in insertAlertRecipient", e);
    }
    return isSuccess;
  }

  /**
   * Common method to insert approval history in the specified tables
   * 
   * @param data
   *          {@link JSONObject} containing the data like the approval action and the next performer
   *          etc.
   * @return The count of the inserted lines.
   */

  public static int InsertApprovalHistory(JSONObject data) {
    int count = 0;

    try {
      OBContext.setAdminMode();
      StringBuilder queryBuilder = new StringBuilder();
      String historyId = SequenceIdData.getUUID();
      String strTableName = data.getString("HistoryTable");

      queryBuilder.append(" INSERT INTO  ").append(strTableName);
      queryBuilder.append(" ( ").append(strTableName.concat("_id"))
          .append(", ad_client_id, ad_org_id,");
      queryBuilder.append(" createdby, updatedby,   ").append(data.getString("HeaderColumn"))
          .append(" , approveddate, ");
      queryBuilder.append(" comments, ").append(data.getString("ActionColumn"))
          .append(" , pendingapproval, seqno,ad_role_id)");
      queryBuilder.append(" VALUES (?, ?, ?, ");
      queryBuilder.append(" ?, ?,?, ?,");
      queryBuilder.append(" ?, ?, ?, ?,?);");
      PreparedStatement query = OBDal.getInstance().getConnection()
          .prepareStatement(queryBuilder.toString());
      query.setString(1, historyId);
      query.setString(2, data.getString("ClientId"));
      query.setString(3, data.getString("OrgId"));
      query.setString(4, data.getString("UserId"));
      query.setString(5, data.getString("UserId"));
      query.setString(6, data.getString("HeaderId"));
      query.setTimestamp(7, new java.sql.Timestamp(System.currentTimeMillis()));
      query.setString(8, data.getString("Comments"));
      query.setString(9, data.getString("Status"));
      query.setString(10, data.optString("NextApprover"));
      query.setInt(11, getHistorySequence(strTableName, data.getString("HeaderColumn"),
          data.getString("HeaderId")));
      query.setString(12, data.optString("RoleId"));
      log4j.debug("History Query: " + query.toString());
      count = query.executeUpdate();
    } catch (Exception e) {
      count = 0;
      log4j.error("Exception while InsertApprovalHistory(): ", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
    return count;
  }

  /**
   * Method to get the max sequenceno from the history table.
   * 
   * @param tableName
   *          Name of the history table.
   * @param headerColumn
   *          ID column name of the header table.
   * @param headerId
   *          Column value of the header column
   * @return returns the maximum sequence number.
   */

  @SuppressWarnings("unchecked")
  public static int getHistorySequence(String tableName, String headerColumn, String headerId) {
    int sequence = 10;
    try {
      StringBuilder queryBuilder = new StringBuilder();

      queryBuilder.append(" select coalesce(seqno,0)  as seqno  from ").append(tableName);
      queryBuilder.append(" where ").append(headerColumn).append(" ='").append(headerId)
          .append("' order by created desc ");

      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(queryBuilder.toString());
      query.setMaxResults(1);

      log4j.debug("Sequence Query: " + query.getQueryString());

      if (query != null) {
        List<Object> rows = query.list();
        if (rows.size() > 0) {
          sequence = Integer.parseInt(rows.get(0).toString()) + 10;
        }
      }
      log4j.debug("Sequence Number: " + sequence);
    } catch (Exception e) {
      log4j.error("Exception while getHistorySequence(): ", e);
    }
    return sequence;
  }

  /**
   * 
   * @param ProductId
   * @param LocatorId
   * @param NeedToRedQty
   * @param ClientId
   * @return
   */
  public static int ChkStoragedetOnhandQtyNeg(String ProductId, String LocatorId,
      BigDecimal NeedToRedQty, String ClientId) {
    int count = 0;
    PreparedStatement st = null;
    ResultSet rs = null;
    BigDecimal onHandQty = BigDecimal.ZERO;
    BigDecimal RemQtyInStDet = BigDecimal.ZERO;
    try {
      st = OBDal.getInstance().getConnection().prepareStatement(
          " select coalesce(sum(qtyonhand),0) as qtyonhand from m_storage_detail where  m_product_id  = ?  and  m_locator_id  =  ? and ad_client_id = ? ");
      st.setString(1, ProductId);
      st.setString(2, LocatorId);
      st.setString(3, ClientId);
      log4j.debug("ChkStoragedetOnhandQtyNeg:" + st.toString());
      rs = st.executeQuery();
      if (rs.next()) {
        onHandQty = rs.getBigDecimal("qtyonhand");
        RemQtyInStDet = onHandQty.subtract(NeedToRedQty);
        if (RemQtyInStDet.compareTo(BigDecimal.ZERO) < 0) {
          return -1;
        } else
          return 1;
      }
    } catch (Exception e) {
      count = 0;
      log4j.error("Exception while ChkStoragedetOnhandQtyNeg(): ", e);
    }
    return count;
  }

  /*
   * Method to chk the submitting Role Match with First Role in Document Rule Param @ClientId
   * ,OrgId,RoleId, UserId, Document Type, Value( Grand Total) Return if Login Rol match with first
   * Role then true otherwise false
   */
  public static Boolean chkSubRolIsInFstRolofDR(Connection con, String clientId, String paramOrgId,
      String userId, String roleId, String documentType, Object pValue) {
    ResultSet rs = null;
    PreparedStatement st = null;
    BigDecimal value = new BigDecimal(0);
    Boolean chkSubRolMatWithFstRol = false;
    String orgId = paramOrgId;
    try {
      // Getting Organization
      st = con.prepareStatement("select eut_documentrule_parentorg(?, ?, ?);");
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      rs = st.executeQuery();
      if (rs.next())
        orgId = rs.getString("eut_documentrule_parentorg");

      if (pValue instanceof Double)
        value = new BigDecimal((Double) pValue);
      else if (pValue instanceof Float)
        value = new BigDecimal((Float) pValue);
      else
        value = new BigDecimal(pValue.toString());

      st = con.prepareStatement(
          " select qdrh.eut_documentrule_header_id, qdrh.rulesequenceno, qdrl.rolesequenceno, qdrl.ad_role_id from eut_documentrule_header qdrh join eut_documentrule_lines qdrl on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
              + "where qdrh.ad_client_id = ? and qdrh.ad_org_id= ? and qdrh.document_type = ? and qdrh.rulevalue <= ?   and qdrl.rolesequenceno = 1 "
              + " order by qdrh.rulevalue desc  ");
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      st.setBigDecimal(4, value);
      log4j.debug("chkSubRolIsInFstRolofDR qry:" + st.toString());
      rs = st.executeQuery();
      while (rs.next()) {
        log4j.debug("rs.getString(ad_role_id)" + rs.getString("ad_role_id"));
        log4j.debug("roleId:" + roleId);
        if (rs.getString("ad_role_id").equals(roleId)) {
          chkSubRolMatWithFstRol = true;
          break;
        }
      }
      return chkSubRolMatWithFstRol;
    } catch (final Exception e) {
      log4j.error("Exception in chkSubRolIsInFstRolofDR() Method", e);
      return null;
    }
  }

  /**
   * 
   * @param strorgId
   *          Organization to filter sequence
   * @param strTransactionType
   *          Transaction Process Type
   * @return returns false if no sequence exists
   */
  @SuppressWarnings({ "unchecked" })
  public static String getTransactionSequence(String strorgId, String strTransactionType) {
    String sequence = "false";
    String updateSequence = "";
    try {
      OBContext.setAdminMode();
      StringBuilder queryBuilder = new StringBuilder();

      queryBuilder.append(" select e.nextAssignedNumber,e.id from ").append("Escm_Sequence e");
      queryBuilder.append(" where ").append("e.organization.id").append(" ='").append(strorgId)
          .append("' and e.transactionType").append(" ='").append(strTransactionType)
          .append("' and istransaction='N' order by creationDate desc ");
      Query query = OBDal.getInstance().getSession().createQuery(queryBuilder.toString());
      query.setMaxResults(1);

      log4j.debug("Sequence Query: " + query.getQueryString());

      if (query != null) {
        List<Object> rows = query.list();
        if (rows.size() > 0) {
          Object[] objects = (Object[]) rows.get(0);
          DocumentSequence objSequence = OBDal.getInstance().get(DocumentSequence.class,
              objects[1].toString());
          sequence = (objSequence.getPrefix() == null ? "" : objSequence.getPrefix())
              + (objSequence.getPrefixtwo() == null ? "" : objSequence.getPrefixtwo())
              + objects[0].toString()
              + (objSequence.getSuffix() == null ? "" : objSequence.getSuffix());
          // get sequence and update next assigned number
          int intSequence = Integer.parseInt(objects[0].toString())
              + objSequence.getIncrementBy().intValue();
          String strFormat = "%0" + String.valueOf((objects[0].toString().length())) + "d";
          updateSequence = String.format(strFormat, intSequence);
          objSequence.setNextAssignedNumber(updateSequence);
          OBDal.getInstance().save(objSequence);
        }
      }
      log4j.debug("Sequence Number: " + sequence);
    } catch (Exception e) {
      log4j.error("Exception while getTransactionSequence(): ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return sequence;
  }

  /**
   * 
   * @param strorgId
   * @param strTransactionType
   * @return returns false if no Sequence sequence exists
   */
  @SuppressWarnings("unchecked")
  public static String getSpecificationSequence(String strorgId, String strTransactionType) {
    String sequence = "false";
    String updateSequence = "";
    try {
      OBContext.setAdminMode();
      StringBuilder queryBuilder = new StringBuilder();
      String clientId = OBContext.getOBContext().getCurrentClient().getId();

      queryBuilder.append(" select e.nextAssignedNumber,e.id from ").append("Escm_Sequence e");
      queryBuilder.append(" where ").append("e.organization.id").append(" ='").append(strorgId)
          .append("' and e.transactionType").append(" ='").append(strTransactionType)
          .append("' and istransaction='Y' and client = '").append(clientId)
          .append("' order by creationDate desc ");
      Query query = OBDal.getInstance().getSession().createQuery(queryBuilder.toString());
      query.setMaxResults(1);

      log4j.debug("Sequence Query: " + query.getQueryString());

      if (query != null) {
        List<Object> rows = query.list();
        if (rows.size() > 0) {
          Object[] objects = (Object[]) rows.get(0);
          DocumentSequence objSequence = OBDal.getInstance().get(DocumentSequence.class,
              objects[1].toString());
          sequence = (objSequence.getPrefix() == null ? "" : objSequence.getPrefix())
              + (objSequence.getPrefixtwo() == null ? "" : objSequence.getPrefixtwo())
              + objects[0].toString()
              + (objSequence.getSuffix() == null ? "" : objSequence.getSuffix());
          // get sequence and update next assigned number
          int intSequence = Integer.parseInt(objects[0].toString())
              + objSequence.getIncrementBy().intValue();
          String strFormat = "%0" + String.valueOf((objects[0].toString().length())) + "d";
          updateSequence = String.format(strFormat, intSequence);
          objSequence.setNextAssignedNumber(updateSequence);
          OBDal.getInstance().save(objSequence);
          // OBDal.getInstance().flush();
        }
      }
      log4j.debug("Sequence Number: " + sequence);
    } catch (Exception e) {
      log4j.error("Exception while getTransactionSequence(): ", e);

    } finally {
      OBContext.restorePreviousMode();
    }
    return sequence;
  }

  /**
   * 
   * @param strorgId
   * @param strTransactionType
   * @return returns false if no Sequence sequence exists
   */
  @SuppressWarnings("unchecked")
  public static String getProcessSpecificationSequence(String strorgId, String strTransactionType) {
    String sequence = "false";
    String updateSequence = "";
    try {
      OBContext.setAdminMode();
      StringBuilder queryBuilder = new StringBuilder();

      queryBuilder.append(" select e.nextAssignedNumber,e.id from ").append("Escm_Sequence e");
      queryBuilder.append(" where ").append("e.organization.id").append(" ='").append(strorgId)
          .append("' and e.transactionType").append(" ='").append(strTransactionType)
          .append("' and istransaction='Y' order by creationDate desc ");
      Query query = OBDal.getInstance().getSession().createQuery(queryBuilder.toString());
      query.setMaxResults(1);

      log4j.debug("Sequence Query: " + query.getQueryString());

      if (query != null) {
        List<Object> rows = query.list();
        if (rows.size() > 0) {
          Object[] objects = (Object[]) rows.get(0);
          DocumentSequence objSequence = OBDal.getInstance().get(DocumentSequence.class,
              objects[1].toString());
          sequence = (objSequence.getPrefix() == null ? "" : objSequence.getPrefix())
              + (objSequence.getPrefixtwo() == null ? "" : objSequence.getPrefixtwo())
              + objects[0].toString()
              + (objSequence.getSuffix() == null ? "" : objSequence.getSuffix());
          // get sequence and update next assigned number
          int intSequence = Integer.parseInt(objects[0].toString())
              + objSequence.getIncrementBy().intValue();
          String strFormat = "%0" + String.valueOf((objects[0].toString().length())) + "d";
          updateSequence = String.format(strFormat, intSequence);
          objSequence.setNextAssignedNumber(updateSequence);
          OBDal.getInstance().save(objSequence);
          OBDal.getInstance().flush();
        }
      }
      log4j.debug("Sequence Number: " + sequence);
    } catch (Exception e) {
      log4j.error("Exception while getTransactionSequence(): ", e);

    } finally {
      OBContext.restorePreviousMode();
    }
    return sequence;
  }

  /**
   * 
   * @param strorgId
   * @param clientId
   * @param strTransactionType
   * @return
   */
  @SuppressWarnings("unchecked")
  public static String getTransactionSequencewithclient(String strorgId, String clientId,
      String strTransactionType) {
    String sequence = "false";
    String updateSequence = "";
    try {
      OBContext.setAdminMode();
      StringBuilder queryBuilder = new StringBuilder();

      queryBuilder.append(" select e.nextAssignedNumber,e.id from ").append("Escm_Sequence e");
      queryBuilder.append(" where ").append("e.organization.id").append(" ='").append(strorgId)
          .append("' and e.transactionType").append(" ='").append(strTransactionType)
          .append("' and istransaction='N' and e.client.id='" + clientId
              + "' order by creationDate desc ");
      Query query = OBDal.getInstance().getSession().createQuery(queryBuilder.toString());
      query.setMaxResults(1);

      log4j.debug("Sequence Query: " + query.getQueryString());

      if (query != null) {
        List<Object> rows = query.list();
        if (rows.size() > 0) {
          Object[] objects = (Object[]) rows.get(0);
          DocumentSequence objSequence = OBDal.getInstance().get(DocumentSequence.class,
              objects[1].toString());

          if (!strTransactionType.equals("BGD")) {
            sequence = (objSequence.getPrefix() == null ? "" : objSequence.getPrefix())
                + (objSequence.getPrefixtwo() == null ? "" : objSequence.getPrefixtwo())
                + objects[0].toString()
                + (objSequence.getSuffix() == null ? "" : objSequence.getSuffix());

          } else {
            sequence = objects[0].toString();
          }
          // get sequence and update next assigned number
          int intSequence = Integer.parseInt(objects[0].toString())
              + objSequence.getIncrementBy().intValue();
          String strFormat = "%0" + String.valueOf((objects[0].toString().length())) + "d";
          updateSequence = String.format(strFormat, intSequence);
          objSequence.setNextAssignedNumber(updateSequence);
          OBDal.getInstance().save(objSequence);
        }
      }
      log4j.debug("Sequence Number: " + sequence);
    } catch (Exception e) {
      log4j.error("Exception while getTransactionSequence(): ", e);

    } finally {
      OBContext.restorePreviousMode();
    }
    return sequence;
  }

  /**
   * 
   * @param WarehouseId
   * @return
   */
  public static String GetDefaultBin(String WarehouseId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String Locator = "";
    try {
      st = OBDal.getInstance().getConnection().prepareStatement(
          " SELECT M_LOCATOR_ID FROM M_LOCATOR WHERE M_WAREHOUSE_ID = ? ORDER BY M_LOCATOR.ISDEFAULT DESC LIMIT 1");
      st.setString(1, WarehouseId);
      log4j.debug("Get defaultBin :" + st.toString());
      rs = st.executeQuery();
      if (rs.next()) {
        Locator = rs.getString("M_LOCATOR_ID");
      }
    } catch (Exception e) {
      log4j.error("Exception while Get defaultBin  ", e);

    }
    return Locator;
  }

  /**
   * This method is used to check transaction sequence
   * 
   * @param strorgId
   * @param strTransactionType
   * @param sequence
   * @return
   */
  public static Boolean chkTransactionSequence(String strorgId, String strTransactionType,
      String sequence) {

    try {
      if (strTransactionType.equals("INR") || strTransactionType.equals("LD")
          || strTransactionType.equals("IRT") || strTransactionType.equals("RET")
          || strTransactionType.equals("DEL") || strTransactionType.equals("SR")
          || strTransactionType.equals("INS") || strTransactionType.equals("IR")
          || strTransactionType.equals("CT") || strTransactionType.equals("PROJ")) {
        OBQuery<ShipmentInOut> minout = OBDal.getInstance().createQuery(ShipmentInOut.class,
            "organization.id='" + strorgId + "' and escmReceivingtype='" + strTransactionType
                + "' and documentNo='" + sequence + "'");
        if (minout.list() != null && minout.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      } else if (strTransactionType.equals("MIR")) {
        OBQuery<MaterialIssueRequest> mir = OBDal.getInstance().createQuery(
            MaterialIssueRequest.class,
            "organization.id='" + strorgId + "' and documentNo='" + sequence + "'");
        if (mir.list() != null && mir.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      } else if (strTransactionType.equals("IC")) {
        OBQuery<InventoryCount> ic = OBDal.getInstance().createQuery(InventoryCount.class,
            "organization.id='" + strorgId + "' and escmSpecno='" + sequence + "'");
        if (ic.list() != null && ic.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      } else if (strTransactionType.equals("BM")) {
        OBQuery<EscmBidMgmt> bm = OBDal.getInstance().createQuery(EscmBidMgmt.class,
            "organization.id='" + strorgId + "' and bidno='" + sequence + "'");
        if (bm.list() != null && bm.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      } else if (strTransactionType.equals("BM-DR")) {
        OBQuery<EscmBidMgmt> bmDr = OBDal.getInstance().createQuery(EscmBidMgmt.class,
            "organization.id='" + strorgId + "' and bidno='" + sequence + "'");
        if (bmDr.list() != null && bmDr.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      } else if (strTransactionType.equals("BM-LD")) {
        OBQuery<EscmBidMgmt> bmLd = OBDal.getInstance().createQuery(EscmBidMgmt.class,
            "organization.id='" + strorgId + "' and bidno='" + sequence + "'");
        if (bmLd.list() != null && bmLd.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      } else if (strTransactionType.equals("BM-TR-LD")) {
        OBQuery<EscmBidMgmt> bmTr = OBDal.getInstance().createQuery(EscmBidMgmt.class,
            "organization.id='" + strorgId + "' and bidno='" + sequence + "'");
        if (bmTr.list() != null && bmTr.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      } else if (strTransactionType.equals("SV")) {
        OBQuery<Escmsalesvoucher> sv = OBDal.getInstance().createQuery(Escmsalesvoucher.class,
            "organization.id='" + strorgId + "' and voucherno='" + sequence + "'");
        if (sv.list() != null && sv.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      } else if (strTransactionType.equals("COM")) {
        OBQuery<ESCMCommittee> committee = OBDal.getInstance().createQuery(ESCMCommittee.class,
            "organization.id='" + strorgId + "' and committee='" + sequence + "'");
        if (committee.list() != null && committee.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      } else if (strTransactionType.equals("PMG")) {
        OBQuery<EscmProposalMgmt> propsalmgmt = OBDal.getInstance().createQuery(
            EscmProposalMgmt.class,
            "organization.id = '" + strorgId + "' and proposalno = '" + sequence + "'");
        if (propsalmgmt.list() != null && propsalmgmt.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      } else if (strTransactionType.equals("ICW")) {
        OBQuery<EscmInsuranceCertificate> insuranceCer = OBDal.getInstance().createQuery(
            EscmInsuranceCertificate.class,
            "organization.id = '" + strorgId + "' and internalNo = '" + sequence + "'");
        if (insuranceCer.list() != null && insuranceCer.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      }
      log4j.debug("Sequence Number: " + sequence);
    } catch (Exception e) {
      log4j.error("Exception while chkTransactionSequence as duplicate: ", e);

    }
    return false;
  }

  /**
   * This method is used to check transaction sequence with client
   * 
   * @param strorgId
   * @param clientId
   * @param strTransactionType
   * @param sequence
   * @return
   */
  public static Boolean chkTransactionSequencewithclient(String strorgId, String clientId,
      String strTransactionType, String sequence) {
    try {
      if (strTransactionType.equals("ANN")) {
        OBQuery<Escmannoucements> annoc = OBDal.getInstance().createQuery(Escmannoucements.class,
            " client.id='" + clientId + "' and annoucementNo='" + sequence + "'");
        if (annoc.list() != null && annoc.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      } else if (strTransactionType.equals("OEC")) {

        OBQuery<Escmopenenvcommitee> opencommitee = OBDal.getInstance().createQuery(
            Escmopenenvcommitee.class,
            " client.id='" + clientId + "' and eventno='" + sequence + "'");
        if (opencommitee.list() != null && opencommitee.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      } else if (strTransactionType.equals("BGD")) {
        /*
         * OBQuery<Escmbankguaranteedetail> bankguarantee = OBDal.getInstance().createQuery(
         * Escmbankguaranteedetail.class, " client.id='" + clientId + "' and internalno='" +
         * sequence + "'");
         */
        OBQuery<ESCMBGWorkbench> bgworkbench = OBDal.getInstance().createQuery(
            ESCMBGWorkbench.class,
            " client.id='" + clientId + "' and internalno='" + sequence + "'");
        if (bgworkbench.list() != null && bgworkbench.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      } else if (strTransactionType.equals("PEE")) {
        OBQuery<ESCMProposalEvlEvent> annoc = OBDal.getInstance().createQuery(
            ESCMProposalEvlEvent.class,
            " client.id='" + clientId + "' and eventNo='" + sequence + "'");
        if (annoc.list() != null && annoc.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      } else if (strTransactionType.equals("TEE")) {
        OBQuery<EscmTechnicalevlEvent> annoc = OBDal.getInstance().createQuery(
            EscmTechnicalevlEvent.class,
            " client.id='" + clientId + "' and eventNo='" + sequence + "'");
        if (annoc.list() != null && annoc.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      } else if (strTransactionType.equals("BGCON")) {

        OBQuery<ESCMBGConfiscation> bgConfiscation = OBDal.getInstance().createQuery(
            ESCMBGConfiscation.class,
            " client.id='" + clientId + "' and letterNo='" + sequence + "'");
        if (bgConfiscation.list() != null && bgConfiscation.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      } else if (strTransactionType.equals("BGAMT")) {

        OBQuery<ESCMBGAmtRevision> bgamtrev = OBDal.getInstance().createQuery(
            ESCMBGAmtRevision.class,
            " client.id='" + clientId + "' and letterNo='" + sequence + "'");
        if (bgamtrev.list() != null && bgamtrev.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      }

      log4j.debug("Sequence Number: " + sequence);
    } catch (Exception e) {
      log4j.error("Exception while chkTransactionSequencewithclient as duplicate: ", e);

    }
    return false;
  }

  /**
   * This method is used to check specification sequence
   * 
   * @param strorgId
   * @param strTransactionType
   * @param sequence
   * @return
   */
  public static Boolean chkSpecificationSequence(String strorgId, String strTransactionType,
      String sequence) {

    try {
      if (strTransactionType.equals("INR") || strTransactionType.equals("LD")
          || strTransactionType.equals("IRT") || strTransactionType.equals("RET")
          || strTransactionType.equals("DEL") || strTransactionType.equals("SR")
          || strTransactionType.equals("INS") || strTransactionType.equals("IR")
          || strTransactionType.equals("CT")) {
        OBQuery<ShipmentInOut> minout = OBDal.getInstance().createQuery(ShipmentInOut.class,
            "organization.id='" + strorgId + "' and escmReceivingtype='" + strTransactionType
                + "' and escmSpecno='" + sequence + "'");
        if (minout.list() != null && minout.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      } else if (strTransactionType.equals("MIR")) {
        OBQuery<MaterialIssueRequest> mir = OBDal.getInstance().createQuery(
            MaterialIssueRequest.class,
            "organization.id='" + strorgId + "' and specNo='" + sequence + "'");
        if (mir.list() != null && mir.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      } else if (strTransactionType.equals("IC")) {
        OBQuery<InventoryCount> ic = OBDal.getInstance().createQuery(InventoryCount.class,
            "organization.id='" + strorgId + "' and escmSpecno='" + sequence + "'");
        if (ic.list() != null && ic.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      } else if (strTransactionType.equals("BM")) {
        OBQuery<EscmBidMgmt> bm = OBDal.getInstance().createQuery(EscmBidMgmt.class,
            "organization.id='" + strorgId + "' and bidno='" + sequence + "'");
        if (bm.list() != null && bm.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      } else if (strTransactionType.equals("SV")) {
        OBQuery<Escmsalesvoucher> sv = OBDal.getInstance().createQuery(Escmsalesvoucher.class,
            "organization.id='" + strorgId + "' and voucherno='" + sequence + "'");
        if (sv.list() != null && sv.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      } else if (strTransactionType.equals("PEE")) {
        OBQuery<ESCMProposalEvlEvent> event = OBDal.getInstance().createQuery(
            ESCMProposalEvlEvent.class,
            "organization.id='" + strorgId + "' and specNo='" + sequence + "'");
        if (event.list() != null && event.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      } else if (strTransactionType.equals("PR")) {
        OBQuery<Requisition> event = OBDal.getInstance().createQuery(Requisition.class,
            "organization.id='" + strorgId + "' and escmSpecNo='" + sequence + "'");
        if (event.list() != null && event.list().size() > 0) {
          return false;
        } else {
          return true;
        }
      }
      log4j.debug("Sequence Number: " + sequence);
    } catch (Exception e) {
      log4j.error("Exception while chkSpecificationSequence as duplicate: ", e);

    }
    return false;
  }

  /**
   * This method is used to get user role
   * 
   * @param UserId
   * @return
   */
  public static List<UtilityVO> getUserRole(String UserId) {
    String roleId = null;
    ArrayList<UtilityVO> ls = new ArrayList<UtilityVO>();

    try {
      UtilityVO vo = null;
      OBQuery<UserRoles> role = OBDal.getInstance().createQuery(UserRoles.class,
          " as e where e.userContact.id='" + UserId + "'");
      if (role.list().size() > 0) {
        for (UserRoles rol : role.list()) {
          vo = new UtilityVO();
          vo.setRoleId(rol.getRole().getId());
          vo.setUserId(rol.getUserContact().getId());
          ls.add(vo);
          log4j.debug("roleId:" + roleId);
        }
        return ls;
      }
    } catch (Exception e) {
      log4j.error("Exception while getUserRole  ", e);

    }
    return ls;
  }

  // chk any of the document rule seq record is in progress
  public static boolean chkDocRulSeqInPrg(Connection con, String clientId, String orgId,
      String documentType, String reqroleId) {
    Boolean recInPrg = false;
    String sql = "";
    String headerId = null, tablename = null, statuscolumn = null, statusvalue = null,
        rolecolumn = null;
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      // Check MIR and MIR IT
      if (documentType.equals("EUT_112") || documentType.equals("EUT_115")) {
        headerId = DocumentRuleTables.EUT_112_Table_ID;
        tablename = DocumentRuleTables.EUT_112_Table_Name;
        statuscolumn = DocumentRuleTables.EUT_112_Status_Column;
        statusvalue = DocumentRuleTables.EUT_112_Status_WFA;
        rolecolumn = DocumentRuleTables.EUT_112_Req_role;
      }
      // Check Return Transaction
      else if (documentType.equals("EUT_113")) {
        headerId = DocumentRuleTables.EUT_113_Table_ID;
        tablename = DocumentRuleTables.EUT_113_Table_Name;
        statuscolumn = DocumentRuleTables.EUT_113_Status_Column;
        statusvalue = DocumentRuleTables.EUT_113_Status_WFA;
        rolecolumn = DocumentRuleTables.EUT_113_Req_role;
      }
      // Check Bid Mgmt
      else if (documentType.equals("EUT_116")) {
        headerId = DocumentRuleTables.EUT_116_Table_ID;
        tablename = DocumentRuleTables.EUT_116_Table_Name;
        statuscolumn = DocumentRuleTables.EUT_116_Status_Column;
        statusvalue = DocumentRuleTables.EUT_116_Status_WFA;
        rolecolumn = DocumentRuleTables.EUT_116_Req_role;
      }
      // Check PR Direct and PR Limited/Tender
      else if (documentType.equals("EUT_111") || documentType.equals("EUT_118")) {
        headerId = DocumentRuleTables.EUT_111_Table_ID;
        tablename = DocumentRuleTables.EUT_111_Table_Name;
        statuscolumn = DocumentRuleTables.EUT_111_Status_Column;
        statusvalue = DocumentRuleTables.EUT_111_Status_WFA;
        rolecolumn = DocumentRuleTables.EUT_111_Req_role;
      }

      if (tablename != null) {
        sql = " select count(" + headerId + ") as count from " + tablename + " where " + rolecolumn
            + "='" + reqroleId + "' and " + statuscolumn + "='" + statusvalue
            + "' and ad_client_id='" + clientId + "' and ad_org_id in (" + orgId + ")";
        if (documentType.equals("EUT_113")) {
          sql += " and em_escm_receivingtype='INR'";
        }
        st = con.prepareStatement(sql);
        log4j.debug("st:" + st.toString());
        rs = st.executeQuery();
        if (rs.next()) {
          if (rs.getInt("count") > 0) {
            recInPrg = true;
          }
        }
      }

    } catch (Exception e) {
      log4j.error("Exception while chkDocRulSeqInPrg  ", e);

    }
    return recInPrg;
  }

  // chk any update is there in exist document rule
  public static boolean chkUpdateExistsDR(Connection con, String clientId, String orgId,
      String documentType, String newRoleList, int lastRuleSeqNos) {
    Boolean updateexists = false;
    PreparedStatement st = null;
    ResultSet rs = null;
    int existrolecount = 0, currentrolecount = 0;
    try {
      st = con.prepareStatement(
          " select count(ad_role_id) as existrolecount from eut_documentrule_lines line where eut_documentrule_header_id in ( select eut_documentrule_header_id as count from eut_documentrule_header  where ad_client_id = ? and ad_org_id = ?"
              + " and document_type = ?  and rulesequenceno= ?  ) ");
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      st.setInt(4, lastRuleSeqNos);
      rs = st.executeQuery();
      log4j.debug("st:" + st.toString());
      if (rs.next()) {
        existrolecount = rs.getInt("existrolecount");
      }
      log4j.debug("existrolecount:" + existrolecount);
      st = con.prepareStatement(
          " select count(ad_role_id) as currentrolecount from ad_role  where ad_role_id in ("
              + newRoleList + ")");
      log4j.debug("st:" + st.toString());
      rs = st.executeQuery();
      if (rs.next()) {
        currentrolecount = rs.getInt("currentrolecount");
      }
      log4j.debug("currentrolecount:" + currentrolecount);
      if (existrolecount > 0 && (existrolecount != currentrolecount)) {
        updateexists = true;
      } else {
        st = con.prepareStatement(
            "  select ad_role_id from eut_documentrule_lines line where eut_documentrule_header_id in ( select eut_documentrule_header_id as count from eut_documentrule_header  where ad_client_id = ? and ad_org_id = ?"
                + " and document_type = ?  and rulesequenceno= ?  ) and ad_role_id not in ("
                + newRoleList + ")");
        st.setString(1, clientId);
        st.setString(2, orgId);
        st.setString(3, documentType);
        st.setInt(4, lastRuleSeqNos);
        log4j.debug("st:" + st.toString());
        rs = st.executeQuery();
        if (rs.next()) {
          updateexists = true;
        }
      }

    } catch (Exception e) {
      log4j.error("Exception while chkUpdateExistsDR  ", e);

    }
    return updateexists;
  }

  /**
   * Validates value is between the min and max value defined in reference lookup
   * 
   * @param Value
   * @param lookupLine
   *          (ESCMDefLookupsTypeLn object)
   * 
   * @return true if it is between min and max value, else false
   */
  public static boolean isValidInitialBGNumber(String Value, ESCMDefLookupsTypeLn lookupLine) {

    if (lookupLine.getDatatype().toLowerCase().equals("integer")) {
      if (!StringUtils.isNumeric(Value)) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_OnlyInteger_lookup"));
      }
    }

    if (Value != null) {
      if ((lookupLine.getMaxvalue().contains(".") ? Double.parseDouble(lookupLine.getMaxvalue())
          : Integer.parseInt(lookupLine.getMaxvalue())) >= (Value.contains(".")
              ? Double.parseDouble(Value)
              : Integer.parseInt(Value))
          && (lookupLine.getMinvalue().contains(".") ? Double.parseDouble(lookupLine.getMinvalue())
              : Integer.parseInt(lookupLine.getMinvalue())) <= (Value.contains(".")
                  ? Double.parseDouble(Value)
                  : Integer.parseInt(Value))) {
        return true;
      }
    } else {
      throw new OBException(OBMessageUtils.messageBD("ESCM_AttrVal_Empty"));
    }
    return false;
  }

  /**
   * Validates whether the value is valid hijri date
   * 
   * @param Value
   * 
   * @return true if it is valid date, else false
   */
  public static boolean isValidDate(String Value) {
    SimpleDateFormat df = new SimpleDateFormat("dd-mm-yyyy");
    try {
      df.parse(Value);
      if (Value.length() > 10) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_NotValidHijriDate"));
      }

      PreparedStatement st = null;
      ResultSet rs = null;
      String gregDate = "";
      try {
        st = new DalConnectionProvider(false).getConnection()
            .prepareStatement("select eut_convertto_gregorian('" + Value + "')");
        rs = st.executeQuery();
        if (rs.next()) {
          gregDate = rs.getString("eut_convertto_gregorian");
        }
      }

      catch (final Exception e) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_NotValidHijriDate"));

      }
      if (gregDate == null) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_NotValidHijriDate"));
      }
      return true;
    } catch (ParseException e) {
      return false;
    }
  }

  /**
   * Validates whether the value is valid hijri date
   * 
   * @param Value
   * 
   * @return true if it is valid date, else false
   */
  public static boolean isBoolean(String Value) {
    if (!(Value.toUpperCase().equals("Y") || Value.toUpperCase().equals("N"))) {
      return false;
    }
    return true;
  }

  /**
   * validate the values entered for IntialBG
   * 
   * @param Value
   * @param lookup
   * 
   */
  public static void validateInitialBGValue(String value, String name, String fieldName,
      ESCMDefLookupsTypeLn lookupLine) {
    if (lookupLine.getDatatype() != null && (lookupLine.getDatatype().toLowerCase().equals("number")
        || lookupLine.getDatatype().toLowerCase().equals("integer"))) {
      if (!Utility.isValidInitialBGNumber(value, lookupLine)) {
        throw new OBException(
            OBMessageUtils.messageBD("ESCM_NotValid_Number").replace("@", fieldName));
      }
    } else if (lookupLine.getDatatype() != null
        && lookupLine.getDatatype().toLowerCase().equals("date")) {
      String dateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty("dateFormat.java");
      if (!Utility.isValidDate(value)) {
        throw new OBException(
            OBMessageUtils.messageBD("ESCM_OnlyDate").replace("@", name).replace("#", dateFormat));
      }
    } else if (lookupLine.getDatatype() != null
        && lookupLine.getDatatype().toLowerCase().equals("boolean")) {
      if (!Utility.isBoolean(value)) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_OnlyBoolean").replace("@", name));
      }
    }
  }

  /**
   * This method is used to get user line manager
   * 
   * @param user
   * @return
   */
  public static String getUserLineManager(User user) {
    String strLineManager = "";

    try {
      OBContext.setAdminMode();
      BusinessPartner bp = null;
      if (user.getBusinessPartner() != null) {
        bp = user.getBusinessPartner();
        if (StringUtils.isNotBlank(bp.getEhcmManager())) {
          strLineManager = getManager(bp.getEhcmManager(), user.getClient().getId());
        }
      }
    } catch (Exception e) {
      strLineManager = "";
      log4j.error("Exception while getUserLineManager: ", e);

    } finally {
      OBContext.restorePreviousMode();
    }
    return strLineManager;
  }

  /**
   * This method is used to get manager
   * 
   * @param strManagerCode
   * @param strClientID
   * @return
   */
  private static String getManager(String strManagerCode, String strClientID) {
    String strManagerName = "";
    try {
      OBContext.setAdminMode();

      OBQuery<BusinessPartner> bpQuery = OBDal.getInstance().createQuery(BusinessPartner.class,
          " where searchKey = :managerCode and client.id = :clientID ");

      bpQuery.setNamedParameter("managerCode", strManagerCode);
      bpQuery.setNamedParameter("clientID", strClientID);

      if (bpQuery.list() != null && bpQuery.list().size() > 0) {
        strManagerName = bpQuery.list().get(0).getName();
      }
    } catch (Exception e) {
      strManagerName = "";
      log4j.error("Exception while getManager: ", e);

    } finally {
      OBContext.restorePreviousMode();
    }
    return strManagerName;
  }

  /**
   * This method is used to get employee line manager
   * 
   * @param strBeneficiaryId
   * @return
   */
  public static String getEmployeeLineManager(String strBeneficiaryId) {
    String strManagerName = "", strManagerCode = "";
    try {
      OBContext.setAdminMode();
      BusinessPartner bp = getObject(BusinessPartner.class, strBeneficiaryId);
      strManagerCode = StringUtils.isBlank(bp.getEhcmManager()) ? "" : bp.getEhcmManager();

      strManagerName = getManager(strManagerCode, bp.getClient().getId());
    } catch (Exception e) {
      strManagerName = "";
      log4j.error("Exception while getEmployeeLineManager: ", e);

    } finally {
      OBContext.restorePreviousMode();
    }
    return strManagerName;
  }

  /**
   * @see Preferences#getPreferenceValue(String, boolean, Client, Organization, User, Role, Window)
   */
  public static String getPreferenceValue(String strPreference, String strWindowId) {
    String preferenceValue = "N";
    try {
      String clientId = OBContext.getOBContext().getCurrentClient().getId();
      String strOrgId = OBContext.getOBContext().getCurrentOrganization().getId();
      String strUserId = OBContext.getOBContext().getUser().getId();
      String roleId = OBContext.getOBContext().getRole().getId();

      preferenceValue = Preferences.getPreferenceValue(strPreference, true, clientId, strOrgId,
          strUserId, roleId, strWindowId);
    } catch (PropertyException e) {
      preferenceValue = "N";
    } catch (Exception e) {
      preferenceValue = "N";
      log4j.error("Exception while getPreferenceValue: ", e);

    }
    return preferenceValue;
  }

  /**
   * 
   * @param strOrdelineId
   * @param shipQty
   * @return if PR quantity Released Successfully then true else false;
   */
  @SuppressWarnings("unchecked")
  public static boolean releasePROrderQty(String strOrdelineId, BigDecimal shipQty) {
    String strQuery = "select ref.escm_ordersource_ref_id ,ref.quantity ,ref.m_requisitionline_id from escm_ordersource_ref ref "
        + " join c_orderline ord on ord.c_orderline_id=ref.c_orderline_id "
        + " where ord.c_orderline_id ='" + strOrdelineId
        + "' and ref.quantity > 0 order by ref.created asc ";
    BigDecimal releaseQty = shipQty;
    try {
      OBContext.setAdminMode();
      if (strQuery != null) {
        SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
        if (query != null) {
          List<Object> rows = query.list();
          if (rows.size() > 0) {
            for (int i = 0; i < rows.size(); i++) {
              Object[] objects = (Object[]) rows.get(i);
              BigDecimal reserveQty = new BigDecimal(Integer.valueOf(objects[1].toString()));
              if (releaseQty.compareTo(reserveQty) > 0) {
                if (objects[2] != null) {
                  RequisitionLine objReqLine = OBDal.getInstance().get(RequisitionLine.class,
                      objects[2].toString());
                  objReqLine.setEscmPoQty(objReqLine.getEscmPoQty().subtract(reserveQty));
                  OBDal.getInstance().save(objReqLine);
                }
                EscmOrderSourceRef objSourceRef = OBDal.getInstance().get(EscmOrderSourceRef.class,
                    objects[0].toString());
                objSourceRef
                    .setReservedQuantity(objSourceRef.getReservedQuantity().subtract(reserveQty));
                OBDal.getInstance().save(objSourceRef);
                releaseQty = releaseQty.subtract(reserveQty);
              } else {
                if (objects[2] != null) {
                  RequisitionLine objReqLine = OBDal.getInstance().get(RequisitionLine.class,
                      objects[2].toString());
                  objReqLine.setEscmPoQty(objReqLine.getEscmPoQty().subtract(releaseQty));
                  OBDal.getInstance().save(objReqLine);
                }
                EscmOrderSourceRef objSourceRef = OBDal.getInstance().get(EscmOrderSourceRef.class,
                    objects[0].toString());
                objSourceRef
                    .setReservedQuantity(objSourceRef.getReservedQuantity().subtract(releaseQty));
                OBDal.getInstance().save(objSourceRef);
              }
            }

          }

        }
      }
      return true;
    } catch (Exception e) {
      log4j.error("Exception while release PR Quantity: ", e);

      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  // Allow numbers and letters
  public static boolean chkSpecialCharpresentornot(String field) {
    Boolean errorflag = false;
    try {
      // check special characters is present or not
      Pattern p = Pattern.compile("[^A-Za-z0-9\\s]");

      if (field != null) {
        String chkfieldval = field == null ? "" : field;
        if (p.matcher(chkfieldval).find()) {
          errorflag = true;
        }
      }
      return errorflag;

    } catch (OBException e) {
      log4j.debug("exception while chkSpecialCharpresentornot", e);
      return errorflag;
    }
  }

  // Allow letters without number
  public static boolean chkSpecialCharWithoutno(String field) {
    Boolean errorflag = false;
    try {
      // check special characters is present or not
      Pattern p = Pattern.compile("[^A-Za-z\\s]");

      if (field != null) {
        String chkfieldval = field == null ? "" : field;
        if (p.matcher(chkfieldval).find()) {
          errorflag = true;
        }
      }
      return errorflag;

    } catch (OBException e) {
      log4j.debug("exception while chkSpecialCharWithoutno", e);
      return errorflag;
    }
  }

  // chk phone number is saudi format or not
  public static boolean chkPhonenoisSaudiFormat(String phonenumber) {
    try {
      // check special characters is present or not
      String regex = "(009665|9665|\\+9665|05|5|\\+966\\s5|00966\\s5)(5|0|3|6|4|9|1|8|7)([0-9]{7})";
      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(phonenumber);

      if (matcher.matches()) {
        return true;
      } else
        return false;
    } catch (OBException e) {
      log4j.debug("exception while chkPhonenoisSaudiFormat", e);
      return false;
    }
  }

  /**
   * This method is used to get organization information
   * 
   * @param strOrgId
   * @return
   */
  public static OrganizationInformation getOrgInfo(String strOrgId) {
    OrganizationInformation info = null;
    try {
      OBContext.setAdminMode();
      Organization organization = getObject(Organization.class, strOrgId);

      if (organization != null && organization.getOrganizationInformationList().size() > 0) {
        info = organization.getOrganizationInformationList().get(0);
      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgInfo():  ", e);

    } finally {
      OBContext.restorePreviousMode();
    }
    return info;
  }

  /**
   * get budget initial obj based on accounting date
   * 
   * @param acctdate
   *          - hijiri date format dd-MM-yyy
   * @param clientId
   * @return Budget initial object
   */
  @SuppressWarnings("unchecked")
  public static EfinBudgetIntialization getBudgetInitial(String acctdate, String clientId) {
    EfinBudgetIntialization budgIniti = null;
    try {
      OBContext.setAdminMode();
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

      String acctDate = UtilityDAO.convertToGregorian(acctdate);
      Date AcctDate = dateFormat.parse(acctDate);
      String strQuery = null;

      strQuery = " select init.efin_budgetint_id from efin_budgetint  init"
          + "       join c_period  frmprd on frmprd.c_period_id= init.fromperiod   "
          + "          join c_period  toprd on toprd.c_period_id= init.toperiod "
          + "          where init.ad_client_id= ? "
          + "          and to_date(?,'yyyy-MM-dd') between  to_date(to_char(frmprd.startdate ,'dd-MM-yyyy'),'dd-MM-yyyy') "
          + "          and to_date(to_char(toprd.enddate,'dd-MM-yyyy'),'dd-MM-yyyy')  and   init.status ='OP' limit 1 ";

      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      if (query != null) {
        query.setParameter(0, clientId);
        query.setParameter(1, dateFormat.format(AcctDate));
        log4j.debug("AcctDate" + dateFormat.format(AcctDate));
        log4j.debug("query" + query.toString());
        List<Object> rows = query.list();
        if (query.list().size() > 0) {
          String budgInitId = (String) rows.get(0);
          if (budgInitId != null)
            budgIniti = OBDal.getInstance().get(EfinBudgetIntialization.class, budgInitId);
        }
      }
      return budgIniti;
    } catch (Exception e) {
      log4j.error("Exception in getBudgetInitial():  ", e);

    } finally {
      OBContext.restorePreviousMode();
    }
    return budgIniti;
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
    String yearquery = "", ParentQury = "";
    String[] orgIds = null;
    String sequence = "0";
    String sequenceId = "";
    String yearId = "";
    String gsQuery = "";
    try {
      OBContext.setAdminMode();
      yearquery = "     select yr.c_year_id from c_period pr"
          + " join c_year yr on pr.c_year_id=yr.c_year_id" + " where to_date('" + AccountDate
          + "','dd-MM-yyyy') between cast(pr.startdate as date) and cast(pr.enddate as date)"
          + " and c_calendar_id='" + CalendarId + "'";

      // get document sequence
      Query resultset = OBDal.getInstance().getSession().createSQLQuery(yearquery);
      Object yearID = resultset.list().get(0);
      yearId = (String) yearID;
      // get GeneralSequence number from year
      gsQuery = "select yrseq.efin_year_sequence_id from ad_sequence seq "
          + "join efin_year_sequence yrseq on seq.ad_sequence_id=yrseq.ad_sequence_id "
          + " where yrseq.c_year_id= :year_id and seq.ad_org_id= :org_id and lower(seq.name)=lower('documentno_"
          + tableName + "')";
      Query genSequencelist = OBDal.getInstance().getSession().createSQLQuery(gsQuery);
      genSequencelist.setParameter("year_id", yearId);
      genSequencelist.setParameter("org_id", OrgId);
      if (genSequencelist.list().size() == 0) {
        ParentQury = " select eut_parent_org('" + OrgId + "','"
            + OBContext.getOBContext().getCurrentClient().getId() + "')";
        Query parentresult = OBDal.getInstance().getSession().createSQLQuery(ParentQury);
        Object parentOrg = parentresult.list().get(0);
        orgIds = ((String) parentOrg).split(",");
        for (int i = 0; i < orgIds.length; i++) {
          Query Sequencelist = OBDal.getInstance().getSession().createSQLQuery(gsQuery);
          Sequencelist.setParameter("year_id", yearId);
          Sequencelist.setParameter("org_id", orgIds[i].replace("'", ""));
          if (Sequencelist.list().size() > 0) {
            Object sequenceID = Sequencelist.list().get(0);
            sequenceId = (String) sequenceID;
            break;
          }
        }
      } else {
        Object sequenceID = genSequencelist.list().get(0);
        sequenceId = (String) sequenceID;
      }
      EfinYearSequence yearSequence = OBDal.getInstance().get(EfinYearSequence.class, sequenceId);
      if (yearSequence != null) {
        sequence = yearSequence.getNextAssignedNumber() == null ? ""
            : yearSequence.getNextAssignedNumber().toString();
        // sequence = Prefix.concat(sequence);
        if (action) {
          yearSequence.setNextAssignedNumber(yearSequence.getNextAssignedNumber() + 1);
        }
        OBDal.getInstance().save(yearSequence);
      }

    } catch (Exception e) {
      log4j.error("Exception in getGeneralSequence() Method : ", e);
      return "0";
    } finally {
      OBContext.restorePreviousMode();
    }
    return sequence;

  }

  /**
   * This method is used to check reserve is done or no
   * 
   * @param clientId
   * @param parmOrgId
   * @param roleId
   * @param userId
   * @param documentType
   * @param pValue
   * @return
   */
  public static Boolean chkReserveIsDoneorNot(String clientId, String parmOrgId, String roleId,
      String userId, String documentType, BigDecimal pValue) {
    Connection con = OBDal.getInstance().getConnection();
    ResultSet rs = null;
    PreparedStatement st = null;
    String orgId = parmOrgId;
    Boolean ischkReserveIsDoneorNot = false;
    try {
      // Getting Organization
      st = con.prepareStatement("select eut_documentrule_parentorg(?, ?, ?);");
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      rs = st.executeQuery();
      if (rs.next())
        orgId = rs.getString("eut_documentrule_parentorg");

      st = con.prepareStatement(
          "select qdrl.ad_role_id,qr.name, qdrl.rolesequenceno as roleseqno, qdrh.rulesequenceno as ruleseqno, qdrh.rolesequenceno, "
              + " qdrl.roleorderno from eut_documentrule_lines qdrl  "
              + " join (select qdrh.eut_documentrule_header_id, qdrh.rulesequenceno, qdrl.rolesequenceno,qdrh.document_type , "
              + " qdrl.ad_role_id from eut_documentrule_header qdrh  "
              + "  join eut_documentrule_lines qdrl on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id  "
              + "  where qdrh.ad_client_id = ?  and qdrh.ad_org_id= ?  "
              + "  and qdrh.document_type = ?   and qdrl.ad_role_id=? "
              + "  and qdrh.rulevalue <= ? order by qdrh.rulevalue desc limit 1) qdrh on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id  "
              + "  left join ad_role qr on qdrl.ad_role_id = qr.ad_role_id  "
              + "  where qdrl.ad_client_id = ? and qdrl.ad_org_id= ? "
              + "  and qdrh.document_type = ?  "
              + "  and qdrl.rolesequenceno < (qdrh.rolesequenceno)  and qdrl.allowreservation='Y' order by qdrl.roleorderno ");
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      st.setString(4, roleId);
      st.setBigDecimal(5, pValue);
      st.setString(6, clientId);
      st.setString(7, orgId);
      st.setString(8, documentType);
      log4j.debug("getNextRoleList qry:" + st.toString());
      rs = st.executeQuery();
      if (rs.next()) {
        ischkReserveIsDoneorNot = true;
      }
    } catch (final Exception e) {
      log4j.error("Exception in chkReserveIsDoneorNot() Method", e);
      return ischkReserveIsDoneorNot;
    }
    return ischkReserveIsDoneorNot;
  }

  /**
   * 
   * @param data
   * @return
   */
  @SuppressWarnings("unused")
  public static boolean isDirectApproval(JSONObject data) {

    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    try {
      query = " select count(head." + data.getString("headerColumn") + ") from "
          + data.getString("tableName") + " head join eut_next_role rl on "
          + " head.eut_next_role_id = rl.eut_next_role_id "
          + "  join eut_next_role_line li on li.eut_next_role_id = rl.eut_next_role_id  "
          + " and head." + data.getString("headerColumn") + " = ? and li.ad_role_id =?;";

      if (query != null) {
        ps = con.prepareStatement(query);
        ps.setString(1, data.getString("headerId"));
        ps.setString(2, data.getString("roleId"));

        rs = ps.executeQuery();

        if (rs.next()) {
          if (rs.getInt("count") > 0)
            return true;
          else
            return false;
        } else
          return false;
      } else
        return false;
    } catch (Exception e) {
      log4j.error("Exception in isDirectApproval " + e.getMessage());
      return false;
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {

      }
    }
  }

  /**
   * This method is used to get org manager or budget manager role
   * 
   * @param clientId
   * @param orgManager
   * @param orgBudgManager
   * @return
   */
  public static String getOrgMangOrBudgManagerRole(String clientId, Boolean orgManager,
      Boolean orgBudgManager) {
    String roleId = null;
    try {
      OBContext.setAdminMode();
      OBQuery<Role> roleqry = OBDal.getInstance().createQuery(Role.class,
          " as e where e.efinOrgbcumanger='" + (orgManager ? "ORGM" : "ORGBM") + "'");
      roleqry.setMaxResult(1);
      if (roleqry.list().size() > 0) {
        roleId = roleqry.list().get(0).getId();
      }
      log4j.debug("roleId:" + roleId);
      return roleId;

    } catch (Exception e) {
      log4j.error("Exception in getOrgMangOrBudgManagerRole " + e.getMessage());
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method is used to get org manager or budget manager user role
   * 
   * @param clientId
   * @param orgManager
   * @param orgBudgManager
   * @param orgId
   * @return
   */
  @SuppressWarnings("rawtypes")
  public static List getOrgMangOrBudgManagerUserRole(String clientId, Boolean orgManager,
      Boolean orgBudgManager, String orgId) {
    List<UserRoles> role = new ArrayList<UserRoles>();
    try {
      OBContext.setAdminMode();
      Organization org = OBDal.getInstance().get(Organization.class, orgId);
      if (org.getEfinOrgbudgmanager() != null) {
        OBQuery<UserRoles> usrrole = OBDal.getInstance().createQuery(UserRoles.class,
            " as e where e.userContact.id='" + (orgManager ? org.getEfinOrgmanager().getId()
                : org.getEfinOrgbudgmanager().getId()) + "'");
        if (usrrole.list().size() > 0) {
          role = usrrole.list();
        }
      }
      return role;

    } catch (Exception e) {
      log4j.error("Exception in getOrgMangOrBudgManagerRole " + e.getMessage());
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method is used to get organization type
   * 
   * @param vars
   * @return
   */
  public static String getOrgTypeOrganizationList(VariablesSecureApp vars) {

    final OrgTree tree = (OrgTree) vars.getSessionObject("#CompleteOrgTree");

    String whereClause = " as e join e.ehcmOrgtyp orgtype " + "where  e.id in (" + tree.toString()
        + ")";
    StringBuilder orgBuffer = new StringBuilder();

    final OBQuery<Organization> orgQuery = OBDal.getInstance().createQuery(Organization.class,
        whereClause.toString());

    List<Organization> orgList = orgQuery.list();

    if (orgList.size() > 0) {
      orgBuffer.append(",'0'");
      for (Organization org : orgList) {
        orgBuffer.append(",'" + org.getId() + "'");
      }
      return orgBuffer.toString().replaceFirst(",", "");
    }

    return tree.toString();

  }

  /**
   * Get Period List<Period>
   * 
   * @param vars
   * @return Period List<Period>
   */
  public static List<Period> getPeriodList(Object acctDate, String calender) {
    List<Period> list = new ArrayList<Period>();
    try {
      OBQuery<Period> period = OBDal.getInstance().createQuery(Period.class,
          " as e join e.year y where e.startingDate <= to_date(:acctDate,'YYYY-MM-DD') "
              + " and e.endingDate >= to_date(:acctDate,'YYYY-MM-DD') and y.calendar.id=:calId");
      period.setNamedParameter("acctDate", acctDate);
      period.setNamedParameter("calId", calender);
      list = period.list();
    } catch (final Exception e) {
      log4j.error("Exception in getperiodList Method : ", e);
      return list;
    }
    return list;
  }

  /**
   * This method is used to get the list of object based on table entity and parameters sent
   * 
   * @param tableobj
   * @param paramaters
   * @return list of objects(Table Entity)
   */

  public static List<BaseOBObject> getQueryList(HashMap<String, String> parameters,
      String whereClause, String id) {

    try {
      Entity entity = ModelProvider.getInstance().getEntityByTableId(id);
      final OBQuery<BaseOBObject> obQuery = OBDal.getInstance().createQuery(entity.getName(),
          whereClause);

      parameters.forEach((key, value) -> {
        obQuery.setNamedParameter(key, value);
      });

      return obQuery.list();
    } catch (Exception e) {
      Log.error("ERROR WHILE GETTING OBJECT" + e.getMessage());
      return null;
    }
  }

  /**
   * 
   * @param strClientId
   * @param lookupCode
   * @return
   */
  public static List<EUTDeflookupsTypeLn> getSubLookupByLookupList(String strClientId,
      String lookupCode) {
    List<EUTDeflookupsTypeLn> subLookupTypeList = new ArrayList<EUTDeflookupsTypeLn>();
    try {
      OBContext.setAdminMode();

      OBQuery<EUTDeflookupsTypeLn> result = OBDal.getInstance().createQuery(
          EUTDeflookupsTypeLn.class,
          "as e where e.client.id=:clientId and e.eUTDeflookupsType.searchKey =:lookupCode order by e.lineNo asc");
      result.setNamedParameter("clientId", strClientId);
      result.setNamedParameter("lookupCode", lookupCode);
      if (result.list().size() > 0) {
        subLookupTypeList = result.list();
      }

    } catch (OBException e) {

      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return subLookupTypeList;
  }

  /**
   * 
   * @param code
   * @param lookupCode
   * @return
   */
  public static String findSubLookupByCode(String code, String lookupCode) {

    final String query = " as e where e.code=:code and e.eUTDeflookupsType.searchKey =:lookupCode ";

    EUTDeflookupsTypeLn lookup = null;

    try {
      OBContext.setAdminMode();

      OBQuery<EUTDeflookupsTypeLn> result = OBDal.getInstance()
          .createQuery(EUTDeflookupsTypeLn.class, query);
      result.setNamedParameter("code", code);
      result.setNamedParameter("lookupCode", lookupCode);
      if (result.list().size() > 0)
        lookup = result.list().get(0);
      else
        return null;

    } catch (OBException e) {

      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

    return lookup.getCode();
  }

  /**
   * 
   * @param code
   * @param lookupCode
   * @return
   */
  public static EUTDeflookupsTypeLn findSubLookup(String code, String lookupCode) {

    final String query = " as e where e.code=:code and e.eUTDeflookupsType.searchKey =:lookupCode ";

    EUTDeflookupsTypeLn lookup = null;

    try {
      OBContext.setAdminMode();

      OBQuery<EUTDeflookupsTypeLn> result = OBDal.getInstance()
          .createQuery(EUTDeflookupsTypeLn.class, query);
      result.setNamedParameter("code", code);
      result.setNamedParameter("lookupCode", lookupCode);
      if (result.list().size() > 0)
        lookup = result.list().get(0);
      else
        return null;
    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return lookup;
  }

  /**
   * @to check gregorian date is valid or not
   * @param dd
   * @param mm
   * @param yyyy
   * @return isGregorianDateValid
   */
  public static boolean isGregorianDateValid(String dd, String mm, String yyyy) {
    boolean isGregorianDateValid = true;
    int days = Integer.parseInt(dd);
    int month = Integer.parseInt(mm);
    int year = Integer.parseInt(yyyy);
    int MAX_VALID_YR = 9999;
    int MIN_VALID_YR = 1800;

    try {
      OBContext.setAdminMode();
      if (year > MAX_VALID_YR || year < MIN_VALID_YR) {
        isGregorianDateValid = false;
      }
      if (month < 1 || month > 12) {
        isGregorianDateValid = false;
      }
      if (days < 1 || days > 31) {
        isGregorianDateValid = false;
      }

      if (month == 2) {
        if ((((year % 4 == 0) && (year % 100 != 0)) || (year % 400 == 0))) {
          if (days <= 29) {

          } else {
            isGregorianDateValid = false;
          }

        } else {
          if (days <= 28) {

          } else {
            isGregorianDateValid = false;
          }
        }
      }
      if (month == 4 || month == 6 || month == 9 || month == 11) {
        if (days <= 30) {

        } else {
          isGregorianDateValid = false;
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in isGregorianDateValid: ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return isGregorianDateValid;
  }

  /**
   * This method is used to check overlaping of number with in the table
   * 
   * @param columnValueMap
   * 
   * @param Tablename
   * @param from
   *          column name(HQL)
   * @param to
   *          column name (HQL)
   * @param from
   *          value
   * 
   * @param map
   *          which has extra conditions
   * @return true if overlaps
   *
   */

  public static boolean checkOverlapExists(String tableName, String fromColumnName,
      String toColumnName, BigDecimal fromValue, BigDecimal toValue,
      HashMap<String, String> columnValueMap, String extraWhereClause) {

    StringBuilder query = new StringBuilder();

    query.append("Select e.id from ");
    query.append(tableName + " as e");
    query.append(" where (e." + fromColumnName + " >=:fromValue");
    query.append(" and e." + toColumnName + " <=:toValue");
    query.append(" or e." + toColumnName + " >=:fromValue");
    query.append(" and e." + fromColumnName + " <=:toValue)");

    columnValueMap.forEach((key, value) -> {
      query.append(" and e." + key + "=:" + key.replace(".", ""));
    });

    if (StringUtils.isNotEmpty(extraWhereClause)) {
      query.append(" and " + extraWhereClause);
    }

    final Query overlapQry = OBDal.getInstance().getSession().createQuery(query.toString());
    overlapQry.setParameter("fromValue", fromValue);
    overlapQry.setParameter("toValue", toValue);

    columnValueMap.forEach((key, value) -> {
      overlapQry.setParameter(key.replace(".", ""), value);
    });

    if ((!overlapQry.list().isEmpty()) && overlapQry.list().size() > 0) {
      return true;
    }

    return false;

  }

  /**
   * This method is used to check open period date
   * 
   * @param transactionDate
   * @param paramorgId
   * @param clientId
   * @return
   */
  public static Boolean checkOpenPeriod(Date transactionDate, String paramorgId, String clientId) {
    Boolean isPeriodOpen = Boolean.FALSE;
    try {
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      String periodId = null, orgID = null;
      if (paramorgId.equals("0")) {
        orgID = getChildOrgwithCalenderId(clientId, paramorgId);
      } else {
        orgID = paramorgId;
      }
      periodId = getPeriod(dateFormat.format(transactionDate), orgID);
      OBQuery<Period> periodQuery = OBDal.getInstance().createQuery(Period.class,
          " as e where e.id=:periodID");
      periodQuery.setNamedParameter("periodID", periodId);
      if (periodQuery.list().size() > 0) {
        String periodStatus = periodQuery.list().get(0).getStatus();
        if (periodStatus.equals("M") || periodStatus.equals("O")) {
          isPeriodOpen = Boolean.TRUE;
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in checkOpenPeriod", e);
    }
    return isPeriodOpen;
  }

  /**
   * This method is used to get child organization with calendar ID
   * 
   * @param clientId
   * @param paramorgId
   * @return
   */
  public static String getChildOrgwithCalenderId(String clientId, String paramorgId) {
    String orgID = null;
    try {
      String calenderID = null, childorgIdList = null;
      int i = 1;
      childorgIdList = getChildOrg(clientId, paramorgId);
      if (childorgIdList.split(",").length > 1) {
        while (calenderID == null && i <= childorgIdList.split(",").length) {
          orgID = (childorgIdList.split(",")[i].toString().replace("'", ""));
          calenderID = getCalendar(orgID);
          i++;
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getChildOrgwithCalenderId", e);
    }
    return orgID;
  }

  /**
   * This method is used to check open period core
   * 
   * @param transactionDate
   * @param orgId
   * @param docbaseType
   * @param docTypeId
   * @return
   */
  public static Boolean checkOpenPeriodCore(Date transactionDate, String orgId, String docbaseType,
      String docTypeId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    Boolean isPeriodOpen = Boolean.FALSE;
    int count;
    try {
      Timestamp ts = new Timestamp(transactionDate.getTime());
      st = OBDal.getInstance().getConnection()
          .prepareStatement("select c_chk_open_period(?, ?, ?, ?) as chkperiod;");
      st.setString(1, orgId);
      st.setTimestamp(2, ts);
      st.setString(3, docbaseType);
      st.setString(4, docTypeId);
      rs = st.executeQuery();
      if (rs.next()) {
        count = rs.getInt("chkperiod");
        if (count > 0)
          isPeriodOpen = Boolean.TRUE;
      }

    } catch (Exception e) {
      log4j.error("Exception in checkOpenPeriodCore", e);
    }
    return isPeriodOpen;
  }

  /**
   * this method will update previous seq number in document sequence while delete the recent
   * created record - for reusing document sequence
   * 
   * @param AccountDate
   * @param tableName
   * @param OrgId
   * @param documentNo
   */
  @SuppressWarnings("unused")
  public static void setDocumentSequenceAfterDeleteRecord(String AccountDate, String tableName,
      String OrgId, Long documentNo, String Type, Boolean isyearbased) {
    String yearquery = "", ParentQury = "";
    String periodQuery = "";
    String[] orgIds = null;
    String sequenceId = "";
    String yearId = "";
    String gsQuery = "";
    String periodId = "";
    Long nextDocno = documentNo + 1;
    try {
      OBContext.setAdminMode();
      if (isyearbased) {
        OBQuery<Calendar> calendarQuery = OBDal.getInstance().createQuery(Calendar.class,
            "as e where e.organization.id ='0'");
        if (calendarQuery.list().size() == 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_NoFiscalCalendar"));
        }
        Calendar calendar = calendarQuery.list().get(0);
        yearquery = "select yr.c_year_id from c_period pr"
            + " join c_year yr on pr.c_year_id=yr.c_year_id" + " where to_date('" + AccountDate
            + "','dd-MM-yyyy') between cast(pr.startdate as date) and cast(pr.enddate as date)"
            + " and c_calendar_id='" + calendar.getId() + "'";
        periodQuery = "select pr.c_period_id from c_period pr "
            + " join c_year yr on pr.c_year_id=yr.c_year_id " + " where to_date('" + AccountDate
            + "','dd-MM-yyyy') between cast(pr.startdate as date) and cast(pr.enddate as date)"
            + " and c_calendar_id='" + calendar.getId() + "'";

        // get document sequence
        Query resultset = OBDal.getInstance().getSession().createSQLQuery(yearquery);
        Object yearID = resultset.list().get(0);
        yearId = (String) yearID;
        if (Type == null || !Type.equals("NPS")) {
          // get year based sequence (recent year seq id ) from year
          sequenceId = getCurrentYearbasedSequence(Type, yearId, OrgId, tableName);
          if (sequenceId != null && sequenceId != "") {
            EfinYearSequence yearSequence = OBDal.getInstance().get(EfinYearSequence.class,
                sequenceId);
            if (yearSequence != null) {
              if (yearSequence.getNextAssignedNumber().equals(nextDocno)) {
                // set previous document no.
                yearSequence.setNextAssignedNumber(documentNo);
              }
              OBDal.getInstance().save(yearSequence);
            }
          }
        }
        if (Type != null && Type.equals("NPS")) {
          Query result = OBDal.getInstance().getSession().createSQLQuery(periodQuery);
          Object periodID = result.list().get(0);
          periodId = (String) periodID;
          // get month based sequence (recent month seq id ) from month
          sequenceId = getCurrentMonthbasedSequence(periodId, yearId, OrgId);
          if (sequenceId != null && sequenceId != "") {
            EfinMonthSequence monthSeq = OBDal.getInstance().get(EfinMonthSequence.class,
                sequenceId);
            if (monthSeq != null) {
              if (monthSeq.getNextAssignedNumber().equals(nextDocno)) {
                // set previous document no.
                monthSeq.setNextAssignedNumber(documentNo);
              }
              OBDal.getInstance().save(monthSeq);
            }
          }
        }
      } else {
        // get document sequence (recent document seq id ) from document sequence
        sequenceId = getDocSequence(tableName, yearId, OrgId);
        if (sequenceId != null && sequenceId != "") {
          Sequence seq = OBDal.getInstance().get(Sequence.class, sequenceId);
          if (seq != null) {
            if (seq.getNextAssignedNumber().equals(nextDocno)) {
              // set previous document no.
              seq.setNextAssignedNumber(documentNo);
            }
            OBDal.getInstance().save(seq);
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getGeneralSequence() Method : ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get current year seq
   * 
   * @param Type
   * @param yearId
   * @param orgId
   * @param tableName
   * @return year sequence id
   */
  public static String getCurrentYearbasedSequence(String Type, String yearId, String orgId,
      String tableName) {
    String yearsequenceId = "";
    String yearSeqQuery = "";
    String ParentQury = "";
    String[] orgIds = null;
    try {
      yearSeqQuery = "select yrseq.efin_year_sequence_id from ad_sequence seq "
          + "join efin_year_sequence yrseq on seq.ad_sequence_id=yrseq.ad_sequence_id "
          + " where yrseq.c_year_id= :year_id and seq.ad_org_id= :org_id ";
      if (Type == null) {
        yearSeqQuery += " and lower(seq.name)=lower('" + tableName + "')";
      } else if (Type.equals("GS")) {
        yearSeqQuery += " and seq.em_efin_isgeneralseq='Y'";
      } else if (Type.equals("PS")) {
        yearSeqQuery += " and seq.em_efin_isacctpaymentseq='Y'";
      }
      Query genSequencelist = OBDal.getInstance().getSession().createSQLQuery(yearSeqQuery);
      genSequencelist.setParameter("year_id", yearId);
      genSequencelist.setParameter("org_id", orgId);
      if (genSequencelist.list().size() == 0) {
        ParentQury = " select eut_parent_org('" + orgId + "','"
            + OBContext.getOBContext().getCurrentClient().getId() + "')";
        Query parentresult = OBDal.getInstance().getSession().createSQLQuery(ParentQury);
        Object parentOrg = parentresult.list().get(0);
        orgIds = ((String) parentOrg).split(",");
        for (int i = 0; i < orgIds.length; i++) {
          Query Sequencelist = OBDal.getInstance().getSession().createSQLQuery(yearSeqQuery);
          Sequencelist.setParameter("year_id", yearId);
          Sequencelist.setParameter("org_id", orgIds[i].replace("'", ""));
          if (Sequencelist.list().size() > 0) {
            Object sequenceID = Sequencelist.list().get(0);
            yearsequenceId = (String) sequenceID;
            break;
          }
        }
      } else {
        Object sequenceID = genSequencelist.list().get(0);
        yearsequenceId = (String) sequenceID;
      }
    } catch (Exception e) {
      log4j.error("Exception in getCurrentYearbasedSequence", e);
    }
    return yearsequenceId;
  }

  /**
   * get current month seq
   * 
   * @param periodId
   * @param yearId
   * @param orgId
   * @return month sequence id
   */
  public static String getCurrentMonthbasedSequence(String periodId, String yearId, String orgId) {
    String monthsequenceId = "";
    String monthSeqQuery = "";
    String ParentQury = "";
    String[] orgIds = null;
    try {
      monthSeqQuery = "select mnseq.efin_month_sequence_id from ad_sequence seq "
          + " join efin_year_sequence yrseq on seq.ad_sequence_id=yrseq.ad_sequence_id "
          + " join efin_month_sequence mnseq on mnseq.efin_year_sequence_id=yrseq.efin_year_sequence_id "
          + " where yrseq.c_year_id=:year_id and seq.em_efin_isacctnonpaymentseq ='Y' "
          + " and seq.ad_org_id=:org_id and mnseq.c_period_id=:period_id";

      Query monSequencelist = OBDal.getInstance().getSession().createSQLQuery(monthSeqQuery);
      monSequencelist.setParameter("year_id", yearId);
      monSequencelist.setParameter("org_id", orgId);
      monSequencelist.setParameter("period_id", periodId);
      if (monSequencelist.list().size() == 0) {
        ParentQury = " select eut_parent_org('" + orgId + "','"
            + OBContext.getOBContext().getCurrentClient().getId() + "')";
        Query parentresult = OBDal.getInstance().getSession().createSQLQuery(ParentQury);
        Object parentOrg = parentresult.list().get(0);
        orgIds = ((String) parentOrg).split(",");
        for (int i = 0; i < orgIds.length; i++) {
          Query Sequencelist = OBDal.getInstance().getSession().createSQLQuery(monthSeqQuery);
          Sequencelist.setParameter("year_id", yearId);
          Sequencelist.setParameter("org_id", orgIds[i].replace("'", ""));
          Sequencelist.setParameter("period_id", periodId);
          if (Sequencelist.list().size() > 0) {
            Object sequenceID = Sequencelist.list().get(0);
            monthsequenceId = (String) sequenceID;
            break;
          }
        }
      } else {
        Object sequenceID = monSequencelist.list().get(0);
        monthsequenceId = (String) sequenceID;
      }
    } catch (Exception e) {
      log4j.error("Exception in getCurrentMonthbasedSequence", e);
    }
    return monthsequenceId;
  }

  /**
   * get current document sequence Id
   * 
   * @param tableName
   * @param yearId
   * @param orgId
   * @return document sequence id
   */
  public static String getDocSequence(String tableName, String yearId, String orgId) {
    String docsequenceId = "";
    String docSeqQuery = "";
    String ParentQury = "";
    String[] orgIds = null;
    try {
      docSeqQuery = "select seq.ad_sequence_id from ad_sequence seq "
          + " where seq.ad_org_id= :org_id and lower(seq.name)=lower('" + tableName
          + "') and seq.ad_client_id=:clientId";
      Query genSequencelist = OBDal.getInstance().getSession().createSQLQuery(docSeqQuery);
      genSequencelist.setParameter("org_id", orgId);
      genSequencelist.setParameter("clientId", OBContext.getOBContext().getCurrentClient().getId());
      if (genSequencelist.list().size() == 0) {
        ParentQury = " select eut_parent_org('" + orgId + "','"
            + OBContext.getOBContext().getCurrentClient().getId() + "')";
        Query parentresult = OBDal.getInstance().getSession().createSQLQuery(ParentQury);
        Object parentOrg = parentresult.list().get(0);
        orgIds = ((String) parentOrg).split(",");
        for (int i = 0; i < orgIds.length; i++) {
          Query Sequencelist = OBDal.getInstance().getSession().createSQLQuery(docSeqQuery);
          Sequencelist.setParameter("org_id", orgIds[i].replace("'", ""));
          Sequencelist.setParameter("clientId",
              OBContext.getOBContext().getCurrentClient().getId());
          if (Sequencelist.list().size() > 0) {
            Object sequenceID = Sequencelist.list().get(0);
            docsequenceId = (String) sequenceID;
            break;
          }
        }
      } else {
        Object sequenceID = genSequencelist.list().get(0);
        docsequenceId = (String) sequenceID;
      }
    } catch (Exception e) {
      log4j.error("Exception in getDocSequence", e);
    }
    return docsequenceId;
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
    String sequenceId = "";
    String nextDocno = "";
    String nextassignedno = "";

    try {
      // get transaction sequence id for given transaction type
      sequenceId = getTransactionSeq(transactionType, OrgId);
      if (sequenceId != null && sequenceId != "") {
        DocumentSequence transactionSeq = OBDal.getInstance().get(DocumentSequence.class,
            sequenceId);
        if (transactionSeq != null) {
          // from spec no get only next assigned number - replace the prefix
          nextassignedno = documentNo
              .replace((transactionSeq.getPrefix() == null ? "" : transactionSeq.getPrefix()), "")
              .replace((transactionSeq.getPrefixtwo() == null ? "" : transactionSeq.getPrefixtwo()),
                  "");
          // convert into integer and add 1
          int intSequence = Integer.parseInt(nextassignedno) + 1;
          // formatted the string
          String strFormat = "%0" + String.valueOf((nextassignedno.length())) + "d";
          // get formatted next assigned number with + 1
          nextDocno = String.format(strFormat, intSequence);

          if (transactionSeq.getNextAssignedNumber().equals(nextDocno)) {
            // set previous document no.
            transactionSeq.setNextAssignedNumber(nextassignedno);
          }
          OBDal.getInstance().save(transactionSeq);
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in setTransactionSequenceAfterDeleteRecord", e);
    }
  }

  /**
   * get transaction sequence id
   * 
   * @param transactionType
   * @param OrgId
   * @return
   */
  public static String getTransactionSeq(String transactionType, String OrgId) {
    String transSeqId = "";
    String transSeqQuery = "";
    try {
      transSeqQuery = "select escm_sequence_id from escm_sequence where transaction_type =:transtype "
          + " and ad_org_id=:orgId and ad_client_id=:clientId";
      Query transactionseqlist = OBDal.getInstance().getSession().createSQLQuery(transSeqQuery);
      transactionseqlist.setParameter("transtype", transactionType);
      transactionseqlist.setParameter("orgId", OrgId);
      transactionseqlist.setParameter("clientId",
          OBContext.getOBContext().getCurrentClient().getId());
      if (transactionseqlist.list().size() > 0) {

        Object sequenceID = transactionseqlist.list().get(0);
        transSeqId = (String) sequenceID;

      }
    } catch (Exception e) {
      log4j.error("Exception in getTransactionSeq", e);
    }
    return transSeqId;
  }

  /**
   * 
   * @param LatestAgreement
   * @param agreementLine
   * @return
   */
  public static String getConCatTypeOther() {
    try {
      OBQuery<ESCMDefLookupsTypeLn> defLn = OBDal.getInstance().createQuery(
          ESCMDefLookupsTypeLn.class,
          " as e where e.escmDeflookupsType.reference='POCONCATG' and searchKey='OTH' and active='Y' ");
      if (defLn.list().size() > 0)
        return defLn.list().get(0).getId();
    } catch (Exception e) {
      log4j.error("Exception in getConCatTypeOther:" + e);
    }
    return null;
  }

  /**
   * This method is used to get submitter details
   * 
   * @param windowReference
   * @param recordId
   * @return
   */
  @SuppressWarnings("unchecked")
  public static JSONObject getSubmitterDetail(String windowReference, String recordId) {
    String actionHistQry = null;
    JSONObject result = new JSONObject();
    try {

      ActionHistoryE actionHistObj = ActionHistoryE.getColumnNames(windowReference);
      if (actionHistObj != null) {
        actionHistQry = "select createdby,ad_role_id  from " + actionHistObj.getHistoryTable()
            + "  where " + actionHistObj.getHeaderColumn() + " =:recordId  and "
            + actionHistObj.getActionColumn() + "='SUB' " + " order by created desc limit 1 ";
        Query actionHistoryQry = OBDal.getInstance().getSession().createSQLQuery(actionHistQry);
        actionHistoryQry.setParameter("recordId", recordId);
        if (actionHistoryQry != null) {
          List<Object> rows = actionHistoryQry.list();
          if (rows.size() > 0) {
            Object[] objects = (Object[]) rows.get(0);
            if (objects[0] != null)
              result.put("createrUser", objects[0].toString());
            if (objects[1] != null)
              result.put("createrRole", objects[1].toString());
          }

        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getSubmitterDetail:" + e);
    }
    return result;
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
    try {
      OBContext.setAdminMode();
      OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance().createQuery(
          EutDocappDelegateln.class,
          " as e left join e.eUTDocappDelegate as hd where hd.role.id =:roleID"
              + " and hd.fromDate <=:currentdate and hd.date >=:currentdate and e.documentType=:docType");
      delegationln.setNamedParameter("roleID", roleID);
      delegationln.setNamedParameter("currentdate", currentDate);
      delegationln.setNamedParameter("docType", documentType);
      return delegationln.list();
    } catch (OBException e) {
      log4j.error("Exception while getDelegation:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Concat Pending Approval details for approval history flows
   * 
   * @param delegationOB
   * @param pendingApproval
   * @return Pending Approval details
   */
  public static String concatPendingApprovalName(EutDocappDelegateln delegationOB,
      String pendingApproval) {
    String resultPendingApproval = "";
    if (pendingApproval != null) {
      resultPendingApproval += "/" + delegationOB.getUserContact().getName();
    } else {
      resultPendingApproval = String.format(Constants.sWAITINGFOR_S_APPROVAL,
          delegationOB.getRole().getName() + " / " + delegationOB.getUserContact().getName());
    }

    return resultPendingApproval;
  }

  /**
   * This method is used to get user details of particular role
   * 
   * @param roleId
   * @return
   */
  public static List<UtilityVO> getAssignedUserForRoles(String roleId) {
    ArrayList<UtilityVO> ls = new ArrayList<UtilityVO>();

    try {
      UtilityVO vo = null;
      OBQuery<UserRoles> role = OBDal.getInstance().createQuery(UserRoles.class,
          " as e where e.role.id='" + roleId + "' and e.active='Y' ");
      if (role.list().size() > 0) {
        for (UserRoles rol : role.list()) {
          vo = new UtilityVO();
          vo.setRoleId(rol.getRole().getId());
          vo.setUserId(rol.getUserContact().getId());
          ls.add(vo);
          log4j.debug("roleId:" + roleId);
        }
        return ls;
      }
    } catch (Exception e) {
      log4j.error("Exception while getAssignedUserForRoles  ", e);

    }
    return ls;
  }

  /**
   * get budget initial obj based on accounting date in the gregorian format
   * 
   * @param acctdate
   *          - gregorian date format dd-MM-yyy
   * @param clientId
   * @return Budget initial object
   */
  @SuppressWarnings("unchecked")
  public static EfinBudgetIntialization getBudgetInitialByUsingDateFormatGreg(String acctdate,
      String clientId) {
    EfinBudgetIntialization budgIniti = null;
    try {
      OBContext.setAdminMode();
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      Date AcctDate = dateFormat.parse(acctdate);
      String strQuery = null;

      strQuery = " select init.efin_budgetint_id from efin_budgetint  init"
          + "       join c_period  frmprd on frmprd.c_period_id= init.fromperiod   "
          + "          join c_period  toprd on toprd.c_period_id= init.toperiod "
          + "          where init.ad_client_id= ? "
          + "          and to_date(?,'yyyy-MM-dd') between  to_date(to_char(frmprd.startdate ,'dd-MM-yyyy'),'dd-MM-yyyy') "
          + "          and to_date(to_char(toprd.enddate,'dd-MM-yyyy'),'dd-MM-yyyy')  and   init.status ='OP' limit 1 ";

      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      if (query != null) {
        query.setParameter(0, clientId);
        query.setParameter(1, dateFormat.format(AcctDate));
        log4j.debug("AcctDate" + dateFormat.format(AcctDate));
        log4j.debug("query" + query.toString());
        List<Object> rows = query.list();
        if (query.list().size() > 0) {
          String budgInitId = (String) rows.get(0);
          if (budgInitId != null)
            budgIniti = OBDal.getInstance().get(EfinBudgetIntialization.class, budgInitId);
        }
      }
      return budgIniti;
    } catch (Exception e) {
      log4j.error("Exception in getBudgetInitial():  ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return budgIniti;
  }

  /**
   * if the table has line number return true else false
   * 
   * @param tableId
   * @return
   */
  public static boolean checkTableHasLineNo(String tableId) {
    final String query = " as e where e.table.id=:tableId and e.dBColumnName='Line' ";
    try {
      OBContext.setAdminMode();
      OBQuery<Column> result = OBDal.getInstance().createQuery(Column.class, query);
      result.setNamedParameter("tableId", tableId);
      if (result.list().size() > 0) {
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      log4j.error("Exception in checkTableHasLineNo():  ", e);
    }
    return false;
  }

  /**
   * 
   * @param userId
   * @param clientId
   * @return User Region Details
   */
  public static Organization getUserRegion(String userId, String clientId) {
    Organization region_obj = null;
    try {
      OBContext.setAdminMode();
      // check user have default role
      // then take the region of default role
      // else take the user first role's region
      User user_obj = OBDal.getInstance().get(User.class, userId);
      if (user_obj.getDefaultRole() != null) {
        if (user_obj.getDefaultRole().getEutReg() != null) {
          region_obj = user_obj.getDefaultRole().getEutReg();
        }
      } else {
        String query = " as e where e.client.id=:clientId and e.userContact.id=userId ";

        OBQuery<UserRoles> result = OBDal.getInstance().createQuery(UserRoles.class, query);
        result.setNamedParameter("clientId", clientId);
        result.setNamedParameter("userId", userId);
        if (result.list().size() > 0) {
          UserRoles userRole_Obj = result.list().get(0);
          Role role_obj = userRole_Obj.getRole();
          if (role_obj != null) {
            if (role_obj.getEutReg() != null) {
              region_obj = role_obj.getEutReg();
            }
          }
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in getUserRegion():  ", e);
    }
    return region_obj;
  }

  public static String getAttachmentPref(String window, String clientId) {
    String preferenceValue = "N";
    try {
      List<Preference> prefs = AttachmentProcessDao.getPreferences("EUT_Attachment_Process", true,
          clientId, null, null, null, window, false, true, true);
      for (Preference preference : prefs) {
        if (preference.getSearchKey() != null && preference.getSearchKey().equals("Y")) {
          preferenceValue = "Y";
        }
      }

    } catch (PropertyException e) {
      preferenceValue = "N";
    } catch (Exception e) {
      // TODO Auto-generated catch block
    }
    return preferenceValue;
  }

  /**
   * get year_ID for greorian date.
   * 
   * @param date
   * 
   * @param clientId
   * @return year for the date
   */
  public static String getYearId(Date date, String clientId) {
    PreparedStatement ps = null;
    ResultSet rs = null;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String yearId = null;
    try {
      ps = OBDal.getInstance().getConnection()
          .prepareStatement("select c_year_id from c_period where to_date('"
              + dateFormat.format(date) + "','yyyy-MM-dd')"
              + " between to_date(to_char(startdate,'dd-MM-yyyy'),'dd-MM-yyyy') and to_date(to_char(enddate,'dd-MM-yyyy'),'dd-MM-yyyy') "
              + "  and ad_client_id ='" + clientId + "'");

      rs = ps.executeQuery();
      if (rs.next()) {
        yearId = rs.getString("c_year_id");
      }
    } catch (Exception e) {
      log4j.error("Exception in getYearId " + e.getMessage());
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
        // TODO: handle exception
      }
    }
    return yearId;
  }

  public static String eventgetYearId(Date date, String clientId) {
    PreparedStatement ps = null;
    ResultSet rs = null;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String yearId = null;
    try {
      SQLQuery Query = OBDal.getInstance().getSession()
          .createSQLQuery("select c_year_id from c_period where to_date('" + dateFormat.format(date)
              + "','yyyy-MM-dd')"
              + " between to_date(to_char(startdate,'dd-MM-yyyy'),'dd-MM-yyyy') and to_date(to_char(enddate,'dd-MM-yyyy'),'dd-MM-yyyy') "
              + "  and ad_client_id ='" + clientId + "'");
      if (Query.list().size() > 0) {
        Object row = (Object) Query.list().get(0);
        yearId = (String) row;
        log4j.debug("yearId:" + yearId);
      }

    } catch (Exception e) {
      log4j.error("Exception in getYearId " + e.getMessage());
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
        // TODO: handle exception
      }
    }
    return yearId;
  }

  /**
   * 
   * @param encumLines
   * @return true if applied amount or remaining amount goes negative
   */
  public static boolean checkEncumbranceAmountZero(List<EfinBudgetManencumlines> encumLines) {
    List<EfinBudgetManencumlines> encumLineList = new ArrayList<EfinBudgetManencumlines>();
    String encumLineId = "";
    EfinBudgetManencumlines encumLinesObj = null;
    // EfinBudgetManencumlines encumlineObj = encumLines;
    try {
      OBContext.setAdminMode();
      for (EfinBudgetManencumlines lines : encumLines) {
        if (lines.getAPPAmt().compareTo(BigDecimal.ZERO) < 0
            || lines.getRemainingAmount().compareTo(BigDecimal.ZERO) < 0) {
          return true;
        } else {
          OBQuery<EfinBudManencumRev> revAmtQuery = OBDal.getInstance().createQuery(
              EfinBudManencumRev.class, " as e where e.sRCManencumline.id = :encumLineID");
          revAmtQuery.setNamedParameter("encumLineID", lines.getId());
          List<EfinBudManencumRev> revAmtList = revAmtQuery.list();
          if (revAmtList.size() > 0) {
            encumLineList = Arrays.asList(revAmtList.get(0).getManualEncumbranceLines());
            checkEncumbranceAmountZero(encumLineList);
          }
        }
      }
      return false;
    } catch (Exception e) {
      log4j.error("Exception in checkEncumbranceAmountZero():  ", e);
    }
    return false;
  }

  /**
   * 
   * check Particular Role is contract category role check or not
   * 
   * @param document
   *          Type, Current Approverr Role Id
   * 
   * @return returns "'Y','N'"
   */
  public static boolean getContractCategoryRole(String doctype, String roleId, String OrgId,
      String invoiceId, BigDecimal value) {
    String ParentQury = "";
    String[] orgIds = null;
    String strContractCategoryRole = "";

    try {

      String contractCategoryQuery = "select iscontractcategoryrole from eut_documentrule_lines ln where eut_documentrule_header_id = "
          + " (select eut_documentrule_header_id from eut_documentrule_header where document_type =:docType "
          + " and ad_org_id=:orgId and rulevalue <= :value and ad_client_id =:client order by rulevalue desc limit 1) ";
      Query reservelist = OBDal.getInstance().getSession().createSQLQuery(contractCategoryQuery);
      reservelist.setParameter("docType", doctype);
      reservelist.setParameter("orgId", OrgId);
      reservelist.setParameter("value", value);
      reservelist.setParameter("client", OBContext.getOBContext().getCurrentClient().getId());
      if (reservelist.list().size() > 0) {

        // check role is contractCategory role or not
        String contractCategoryQuery1 = "select iscontractcategoryrole from eut_documentrule_lines ln where eut_documentrule_header_id = "
            + " (select eut_documentrule_header_id from eut_documentrule_header where document_type =:docType "
            + " and ad_org_id=:orgId and rulevalue <= :value and ad_client_id =:client order by rulevalue desc limit 1) "
            + " and iscontractcategoryrole='Y' and ln.ad_role_id=:roleId ";
        Query reservelist1 = OBDal.getInstance().getSession()
            .createSQLQuery(contractCategoryQuery1);
        reservelist1.setParameter("docType", doctype);
        reservelist1.setParameter("orgId", OrgId);
        reservelist1.setParameter("value", value);
        reservelist1.setParameter("roleId", roleId);
        reservelist1.setParameter("client", OBContext.getOBContext().getCurrentClient().getId());

        log4j.debug("iscontractcategoryrolelist:" + reservelist1);
        if (reservelist1.list().size() > 0) {
          Object contractCategoryRole = reservelist1.list().get(0);
          strContractCategoryRole = (String) contractCategoryRole.toString();
        } else {
          return false;
        }
      } else {
        // current user is not present in document rule then check parent org document rule
        String contractCategoryQuery2 = "select iscontractcategoryrole from eut_documentrule_lines ln where eut_documentrule_header_id = "
            + " (select eut_documentrule_header_id from eut_documentrule_header where document_type =:docType "
            + " and ad_org_id=:orgId and rulevalue <= :value and ad_client_id =:client order by rulevalue desc limit 1) "
            + " and iscontractcategoryrole='Y' and ln.ad_role_id=:roleId ";

        ParentQury = " select eut_parent_org('" + OrgId + "','"
            + OBContext.getOBContext().getCurrentClient().getId() + "')";
        Query parentresult = OBDal.getInstance().getSession().createSQLQuery(ParentQury);
        Object parentOrg = parentresult.list().get(0);
        log4j.debug("parentOrg:" + parentOrg);
        orgIds = ((String) parentOrg).split(",");
        for (int i = 0; i < orgIds.length; i++) {
          Query parentReservelist = OBDal.getInstance().getSession()
              .createSQLQuery(contractCategoryQuery2);
          parentReservelist.setParameter("docType", doctype);
          parentReservelist.setParameter("orgId", orgIds[i].replace("'", ""));
          parentReservelist.setParameter("value", value);
          parentReservelist.setParameter("roleId", roleId);
          parentReservelist.setParameter("client",
              OBContext.getOBContext().getCurrentClient().getId());

          if (parentReservelist.list().size() > 0) {
            Object contractCategoryRole = parentReservelist.list().get(0);
            strContractCategoryRole = (String) contractCategoryRole.toString();
            break;
          }
        }
      }

      if (strContractCategoryRole.equals("Y")) {
        return true;
      } else {
        return false;
      }

    } catch (final Exception e) {
      log4j.error("Exception in getcontractCategoryRole() Method : ", e);
      return false;
    }

  }

  public static Organization getAgencyOrg(String clientId) {
    Organization org = null;
    List<EfinBudgetControlParam> budgetContrParamList = null;
    try {
      OBQuery<EfinBudgetControlParam> budgCtrlParamQry = OBDal.getInstance()
          .createQuery(EfinBudgetControlParam.class, " as e where e.client.id=:clientId ");
      budgCtrlParamQry.setNamedParameter("clientId", clientId);
      budgCtrlParamQry.setMaxResult(1);
      budgetContrParamList = budgCtrlParamQry.list();
      if (budgetContrParamList.size() > 0) {
        org = budgetContrParamList.get(0).getAgencyHqOrg();
      }

    } catch (Exception e) {
      log4j.error("Exception in getAgencyOrg():  ", e);
    }
    return org;
  }

  public static String getProperty(String key) {

    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(key);
  }
}
