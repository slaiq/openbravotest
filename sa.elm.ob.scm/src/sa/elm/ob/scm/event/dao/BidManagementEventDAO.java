package sa.elm.ob.scm.event.dao;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.DalConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmBidMgmt;

/**
 * 
 * This class is used to handle dao activities of Bid Event.
 */
public class BidManagementEventDAO {
  private static final Logger log = LoggerFactory.getLogger(BidManagementEventDAO.class);
  static String Bid = OBMessageUtils.messageBD("Escm_Bid");

  /**
   * Get Current Date in Hijri
   * 
   * @return current date in String
   */
  public static String getCurrentDateInHijri() {
    String curDate = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      ConnectionProvider conn = new DalConnectionProvider(false);
      st = conn.getPreparedStatement(
          "select eut_convert_to_hijri(to_char(now(),'YYYY-MM-DD')) as currentdate");
      rs = st.executeQuery();
      if (rs.next()) {
        curDate = rs.getString("currentdate");
      }
    } catch (Exception e) {
      log.error("Exception while getCurrentDateInHijri:", e);
    } finally {
      try {
        if (st != null)
          st.close();
        if (rs != null)
          rs.close();
      } catch (Exception e) {
        log.error("Exception while closing the statement getCurrentDateInHijri:", e);
      }
    }
    return curDate;
  }

  /**
   * Insert alert to Department Manager once Bid Published in tabadul site
   * 
   * @param EscmBidMgmt
   * @return
   */
  public static void insertBidPublishmentAlert(EscmBidMgmt bidmgmt, String msg) {
    StringBuffer query = null;
    Query alertQuery = null;
    String alertRuleId = null;
    ArrayList<String> includeRecipient = new ArrayList<String>();
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    Date endDate = null;
    try {
      query = new StringBuffer();
      endDate = formatter.parse("9999-12-31");
      // Get alert rule id of tabadul-Bid
      alertRuleId = getAlertRule(bidmgmt.getClient().getId());
      // Solve Old Alerts
      solveOldAlerts(alertRuleId, bidmgmt.getId());
      // Get Existed Receipients
      includeRecipient = getAlertReceipients(alertRuleId, bidmgmt.getClient().getId());
      // check for Dept Manager From PR
      query.append("select distinct usrrol.role.id, usr.id, bid.bidno||'-'||bid.bidname as bid "
          + "from escm_bidsourceref bref " + "left join bref.escmBidmgmtLine bidln "
          + "left join bidln.escmBidmgmt bid "
          + "left join bref.requisition.escmBenfdept.organization.ehcmOrgManagerList dmgr "
          + "left join dmgr.businessPartner.aDUserList usr "
          + "left join usr.aDUserRolesList usrrol " + "where bidln.escmBidmgmt.id=:bidid "
          + "and usr.id is not null and :today BETWEEN dmgr.ehcmFromdate and coalesce(dmgr.ehcmTodate, :endDate) ");

      alertQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      alertQuery.setParameter("bidid", bidmgmt.getId());
      alertQuery.setDate("today", new Date());
      alertQuery.setDate("endDate", endDate);

      if (alertQuery != null) {
        log.debug("dept mgr size>" + alertQuery.list().size());
        if (alertQuery.list().size() > 0) {
          for (@SuppressWarnings("rawtypes")
          Iterator iterator = alertQuery.iterate(); iterator.hasNext();) {
            Object[] objects = (Object[]) iterator.next();
            String roleId = objects[0] == null ? "" : objects[0].toString();
            String userId = objects[1] == null ? "" : objects[1].toString();
            String bid = objects[2] == null ? "" : objects[2].toString();

            String description = Bid + " " + bid + " " + msg;
            if (alertRuleId != null) {
              log.debug("Manager role&userId>" + roleId + "," + userId);
              // Alert Receipient
              includeRecipient.add(roleId + "-" + userId);
              // insertAlertRecipient(roleId, userId, bidmgmt.getClient().getId(), alertRuleId);

              // Alert
              alertInsertionRole(bidmgmt.getId(), bidmgmt.getBidno() + "-" + bidmgmt.getBidname(),
                  roleId, userId, bidmgmt.getClient().getId(), description, "NEW", alertRuleId);
            }
          }
          HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
          Iterator<String> iterator = incluedSet.iterator();
          while (iterator.hasNext()) {
            insertAlertRecipient(iterator.next(), bidmgmt.getClient().getId(), alertRuleId);
          }
        }
      }
    } catch (OBException e) {
      log.error("Exception while insertBidPublishmentAlert:", e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      log.error("Exception while insertBidPublishmentAlert:", e);
    }
  }

  /**
   * Insert alert to PR Preparer once Bid Published in tabadul site and Bid action Changes
   * 
   * @param EscmBidMgmt
   * @return
   */
  public static void insertBidAlertToPRPreparer(EscmBidMgmt bidmgmt, String msg) {
    StringBuffer query = null;
    Query alertQuery = null;
    String alertRuleId = null;
    ArrayList<String> includeRecipient = new ArrayList<String>();
    try {
      query = new StringBuffer();

      // Get alert rule id of tabadul-Bid
      alertRuleId = getAlertRule(bidmgmt.getClient().getId());
      // Solve Old Alerts
      solveOldAlerts(alertRuleId, bidmgmt.getId());
      // Get Existed Receipients
      includeRecipient = getAlertReceipients(alertRuleId, bidmgmt.getClient().getId());

      // check for Dept Manager From PR
      query.append(
          "select distinct req.escmAdRole.id, req.userContact.id, bid.bidno||'-'||bid.bidname as bid "
              + " from escm_bidsourceref bref " + " left join bref.escmBidmgmtLine bidln "
              + " left join bidln.escmBidmgmt bid " + " left join bref.requisition req  "
              + " where req.userContact.id is not null and req.escmAdRole.id is not null and bidln.escmBidmgmt.id=:bidid ");

      alertQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      alertQuery.setParameter("bidid", bidmgmt.getId());
      if (alertQuery != null) {
        log.debug("pr preparer size>" + alertQuery.list().size());
        if (alertQuery.list().size() > 0) {
          for (@SuppressWarnings("rawtypes")
          Iterator iterator = alertQuery.iterate(); iterator.hasNext();) {
            Object[] objects = (Object[]) iterator.next();
            String roleId = objects[0] == null ? "" : objects[0].toString();
            String userId = objects[1] == null ? "" : objects[1].toString();
            String bid = objects[2] == null ? "" : objects[2].toString();
            String description = Bid + " " + bid + " " + msg;

            if (alertRuleId != null) {
              log.debug("PRPreparer role&userId>" + roleId + "," + userId);
              // Alert Receipient
              includeRecipient.add(roleId + "-" + userId);
              // insertAlertRecipient(roleId, userId, bidmgmt.getClient().getId(), alertRuleId);

              // Alert
              alertInsertionRole(bidmgmt.getId(), bidmgmt.getBidno() + "-" + bidmgmt.getBidname(),
                  roleId, userId, bidmgmt.getClient().getId(), description, "NEW", alertRuleId);
            }
          }
          HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
          Iterator<String> iterator = incluedSet.iterator();
          while (iterator.hasNext()) {
            insertAlertRecipient(iterator.next(), bidmgmt.getClient().getId(), alertRuleId);
          }
        }
      }
    } catch (OBException e) {
      log.error("Exception while insertBidAlertToPRPreparer:" + e);
      throw new OBException(e.getMessage());
    }
  }

  /**
   * Insert alert to budget controller once Bid Action Changes into WD-Withdrawn, CD-Closed,
   * PP-PostPoned, RES-ReSubmitted, CL-Cancelled
   * 
   * @param documentType
   * @param EscmBidMgmt
   * @param Object
   * @return
   */
  public static void insertBidActionChangeAlert(String documentType, EscmBidMgmt bidmgmt,
      Object pValue, String msg) {
    ResultSet rs = null;
    PreparedStatement st = null;
    BigDecimal value = new BigDecimal(0);
    String orgId = bidmgmt.getOrganization().getId();
    String preferenceValue = "";
    ConnectionProvider conn = null;
    String alertRuleId = null;
    ArrayList<String> includeRecipient = new ArrayList<String>();
    try {
      conn = new DalConnectionProvider(false);

      // Get alert rule id of tabadul-Bid
      alertRuleId = getAlertRule(bidmgmt.getClient().getId());
      // Solve Old Alerts
      solveOldAlerts(alertRuleId, bidmgmt.getId());
      // Get Existed Receipients
      includeRecipient = getAlertReceipients(alertRuleId, bidmgmt.getClient().getId());
      String description = Bid + " " + bidmgmt.getBidno() + " - " + bidmgmt.getBidname() + " "
          + msg;

      // Getting Organization which has document rule
      st = conn.getPreparedStatement("select eut_documentrule_parentorg(?, ?, ?);");
      st.setString(1, bidmgmt.getClient().getId());
      st.setString(2, orgId);
      st.setString(3, documentType);
      rs = st.executeQuery();
      if (rs.next())
        orgId = rs.getString("eut_documentrule_parentorg");
      if (rs != null)
        rs.close();
      if (st != null)
        st.close();

      if (pValue instanceof Double)
        value = new BigDecimal((Double) pValue);
      else if (pValue instanceof Float)
        value = new BigDecimal((Float) pValue);
      else
        value = new BigDecimal(pValue.toString());
      log.debug("alertRuleId>" + alertRuleId);
      // Get all roles to check budget controller from document rule of the bid
      st = conn.getPreparedStatement(
          "select ad_role_id from eut_documentrule_lines where eut_documentrule_header_id = "
              + "(select eut_documentrule_header_id from eut_documentrule_lines ln "
              + "where ln.eut_documentrule_header_id in ( "
              + "select qdrh.eut_documentrule_header_id from eut_documentrule_header qdrh "
              + "where qdrh.ad_client_id = ? and qdrh.ad_org_id= ? "
              + "and qdrh.document_type = ? and qdrh.rulevalue <= ? "
              + "group by qdrh.rulevalue, qdrh.eut_documentrule_header_id "
              + "order by qdrh.rulevalue desc) and ln.ad_role_id=? and rolesequenceno='1')");
      st.setString(1, bidmgmt.getClient().getId());
      st.setString(2, orgId);
      st.setString(3, documentType);
      st.setBigDecimal(4, value);
      st.setString(5, bidmgmt.getRole().getId());
      log.debug("chkRoleIsInDocRul count:" + st.toString());
      rs = st.executeQuery();
      while (rs.next()) {
        try {
          // check Budget Controller is in document rule to send alert
          preferenceValue = Preferences.getPreferenceValue("ESCM_BudgetControl", true,
              bidmgmt.getClient().getId(), bidmgmt.getOrganization().getId(), null,
              rs.getString("ad_role_id"), null);
        } catch (PropertyException e) {
          // e.printStackTrace();
        }
        log.debug("preferenceValue>" + preferenceValue);
        if (preferenceValue != null && !StringUtils.isEmpty(preferenceValue)
            && preferenceValue.equals("Y")) {
          log.debug("Actions roleId>" + rs.getString("ad_role_id"));
          if (alertRuleId != null) {
            // Alert Receipient
            includeRecipient.add(rs.getString("ad_role_id") + "-" + "null");
            // Alert
            alertInsertionRole(bidmgmt.getId(), bidmgmt.getBidno() + "-" + bidmgmt.getBidname(),
                rs.getString("ad_role_id"), null, bidmgmt.getClient().getId(), description, "NEW",
                alertRuleId);
          }
        }
        preferenceValue = "";
      }
      HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
      Iterator<String> iterator = incluedSet.iterator();
      while (iterator.hasNext()) {
        insertAlertRecipient(iterator.next(), bidmgmt.getClient().getId(), alertRuleId);
      }
    } catch (final Exception e) {
      log.error("Exception in insertBidActionChangeAlert() Method", e);
    } finally {
      // close connection
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {
        log.error("Exception while closing the statement in insertBidActionChangeAlert() Method ",
            e);
      }
    }
  }

  /**
   * Get AlertRule of Bid-Tabadul
   * 
   * @param clientId
   * @return alertrule id in String
   */
  public static String getAlertRule(String clientId) {
    String alertRuleId = null;
    try {
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id=:clientID and e.eSCMProcessType='TP'");
      queryAlertRule.setNamedParameter("clientID", clientId);
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
    } catch (final Exception e) {
      log.error("Exception in insertBidActionChangeAlert() Method", e);
    }
    return alertRuleId;
  }

  /**
   * Insert Alert Receipients
   * 
   * @param roleId
   * @param userId
   * @param clientId
   * @param alertRuleId
   * @return Boolean
   */
  public static Boolean insertAlertRecipient(String roleUsrId, String clientId,
      String alertRuleId) {
    Boolean isSuccess = Boolean.TRUE;
    String roleId = null;
    String userId = null;
    try {
      AlertRecipient objAlertRecipient = OBProvider.getInstance().get(AlertRecipient.class);
      objAlertRecipient.setClient(OBDal.getInstance().get(Client.class, clientId));
      objAlertRecipient.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
      objAlertRecipient.setAlertRule(OBDal.getInstance().get(AlertRule.class, alertRuleId));
      objAlertRecipient.setSendEMail(false);
      roleId = roleUsrId.split("-")[0];
      userId = roleUsrId.split("-")[1];
      if (roleId != null && !("null").equals(roleId))
        objAlertRecipient.setRole(OBDal.getInstance().get(Role.class, roleId));
      if (userId != null && !("null").equals(userId))
        objAlertRecipient.setUserContact(OBDal.getInstance().get(User.class, userId));
      OBDal.getInstance().save(objAlertRecipient);
    } catch (Exception e) {
      isSuccess = Boolean.FALSE;
      log.error("Exception in insertAlertRecipient", e);
    }
    return isSuccess;
  }

  /**
   * Insert Alerts
   * 
   * @param DocumentId
   * @param DocumentNo
   * @param roleId
   * @param userId
   * @param clientId
   * @param description
   * @param status
   * @param alertRuleId
   * @return Boolean
   */
  public static Boolean alertInsertionRole(String DocumentId, String DocumentNo, String roleId,
      String userId, String clientId, String description, String status, String alertRuleId) {
    Boolean isSuccess = Boolean.TRUE;
    try {
      Alert objAlert = OBProvider.getInstance().get(Alert.class);
      objAlert.setClient(OBDal.getInstance().get(Client.class, clientId));
      objAlert.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
      objAlert.setAlertRule(OBDal.getInstance().get(AlertRule.class, alertRuleId));
      objAlert.setDescription(description);
      if (roleId != null && !roleId.isEmpty() && !roleId.equals("")) {
        objAlert.setRole(OBDal.getInstance().get(Role.class, roleId));
      }
      if (userId != null && !userId.isEmpty() && !userId.equals("")) {
        objAlert.setUserContact(OBDal.getInstance().get(User.class, userId));
      }
      objAlert.setRecordID(DocumentNo);
      objAlert.setReferenceSearchKey(DocumentId);
      objAlert.setAlertStatus(status);
      OBDal.getInstance().save(objAlert);

    } catch (Exception e) {
      isSuccess = Boolean.FALSE;
      log.error("Exception in alertInsertionRole", e);
    }
    return isSuccess;
  }

  public static void solveOldAlerts(String alertRuleId, String searchKeyId) {
    try {
      OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
          "as e where e.referenceSearchKey=:searchKey and e.alertStatus='NEW' ");// and
                                                                                 // e.alertRule.id='"
      alertQuery.setNamedParameter("searchKey", searchKeyId); // +
      // alertRuleId
      // + "'
      if (alertQuery.list().size() > 0) {
        for (Alert objAlert : alertQuery.list()) {
          objAlert.setAlertStatus("SOLVED");
        }
      }
    } catch (Exception e) {
      log.error("Exception in solveOldAlerts", e);
    }
  }

  public static ArrayList<String> getAlertReceipients(String alertRuleId, String clientId) {
    ArrayList<String> includeRecipient = new ArrayList<String>();
    try {
      OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance().createQuery(
          AlertRecipient.class,
          "as e where e.alertRule.id=:alertRuleID and e.client.id=:clientID ");
      receipientQuery.setNamedParameter("alertRuleID", alertRuleId);
      receipientQuery.setNamedParameter("clientID", clientId);

      // check and insert recipient
      if (receipientQuery.list().size() > 0) {
        for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
          includeRecipient.add(objAlertReceipient.getRole().getId() + "-"
              + (objAlertReceipient.getUserContact() != null
                  ? objAlertReceipient.getUserContact().getId()
                  : "null"));
          OBDal.getInstance().remove(objAlertReceipient);
        }
      }
    } catch (Exception e) {
      log.error("Exception in getAlertReceipients", e);
    }
    return includeRecipient;
  }

  public static void removeDuplicateReceipients(EscmBidMgmt bidmgmt) {
    ArrayList<String> includeRecipient = new ArrayList<String>();
    try {
      includeRecipient = getAlertReceipients(getAlertRule(bidmgmt.getClient().getId()),
          bidmgmt.getClient().getId());
      HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
      Iterator<String> iterator = incluedSet.iterator();
      while (iterator.hasNext()) {
        insertAlertRecipient(iterator.next(), bidmgmt.getClient().getId(),
            getAlertRule(bidmgmt.getClient().getId()));
      }
    } catch (Exception e) {
      log.error("Exception in removeDuplicateReceipients", e);
    }
  }
}
