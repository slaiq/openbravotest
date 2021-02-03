package sa.elm.ob.scm.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.quartz.JobExecutionException;

import sa.elm.ob.utility.util.Constants;

/**
 * 
 * @author poongodi on 05/06/2020
 *
 */
public class BidOpenEnvelopDateScheduler extends DalBaseProcess {

  private ProcessLogger logger;

  public void doExecute(ProcessBundle bundle) throws Exception {

    logger = bundle.getLogger();

    try {
      String clientId = bundle.getContext().getClient();
      String alertWindow = sa.elm.ob.scm.util.AlertWindow.openEnvelopDate;
      String alertRuleId = null;
      String property = "ESCM_OEE_User";
      String windowId = "E509200618424FD099BAB1D4B34F96B8";
      JSONObject ruleList, json = null;
      JSONArray arr = null;
      String tempRole = null;
      String tempUser = null;
      String alertKey = "Scm.OEE.User";
      List queryList = null;
      ArrayList<String> includeRecipient = new ArrayList<String>();
      Boolean isSuccess = Boolean.TRUE;
      String description = "";
      // get alertruleID
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.eSCMProcessType='" + alertWindow
              + "' order by e.creationDate desc");
      queryAlertRule.setMaxResult(1);
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      // get alert recipients
      OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
          .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");
      queryList = getbidDateQuery(clientId);
      if (queryList != null && queryList.size() > 0) {
        for (Object row : queryList) {
          {
            tempRole = null;
            tempUser = null;
            Object[] o = (Object[]) row;
            ruleList = AlertUtilityDAO.getVisibleAtRoleAndUser(property, clientId, windowId, null);
            if (ruleList != null && ruleList.length() > 0) {
              arr = ruleList.getJSONArray("roleList");
              if (arr.length() > 0) {
                for (int i = 0; i < arr.length(); i++) {
                  json = arr.getJSONObject(i);

                  if (tempRole == null || (tempRole != null
                      && ((!tempRole.equals(json.getString("Role"))) || (tempRole
                          .equals(json.getString("Role")) && !tempUser.equals("0")
                          && !tempUser.equals(json.has("User") ? "0" : json.getString("User")))))) {
                    tempRole = json.getString("Role");
                    tempUser = json.has("User") ? json.getString("User") : "0";
                    description = sa.elm.ob.scm.properties.Resource
                        .getProperty("scm.oeeuser.alert", bundle.getContext().getLanguage())
                        .concat(" " + o[3].toString());
                    Alert objAlert = OBProvider.getInstance().get(Alert.class);
                    objAlert.setClient(OBDal.getInstance().get(Client.class, clientId));
                    objAlert.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
                    objAlert.setAlertRule(OBDal.getInstance().get(AlertRule.class, alertRuleId));
                    objAlert.setEutMailTmplt(Constants.GENERIC_TEMPLATE);
                    objAlert.setEutAlertKey(alertKey);
                    // imported via data set
                    objAlert.setDescription(description);
                    if (json.has("User")) {
                      objAlert.setUserContact(
                          OBDal.getInstance().get(User.class, json.getString("User")));
                    }
                    if (json.has("Role")) {
                      objAlert.setRole(OBDal.getInstance().get(Role.class, json.getString("Role")));
                    }
                    objAlert.setRecordID(o[3].toString());
                    objAlert.setReferenceSearchKey(o[0].toString());
                    objAlert.setAlertStatus("NEW");
                    OBDal.getInstance().save(objAlert);
                    OBDal.getInstance().flush();
                    includeRecipient.add(json.getString("Role"));
                  }
                }
              }
              isSuccess = Boolean.TRUE;
            }
          }
        }
      }
      if (isSuccess) {
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
          AlertUtilityDAO.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        }
      }
    } catch (Exception e) {
      throw new JobExecutionException(e.getMessage(), e);
    }

  }

  @SuppressWarnings("rawtypes")
  public static List getbidDateQuery(String clientId) {

    String sqlQuery = null;
    SQLQuery query = null;
    List visibleList = null;

    sqlQuery = "SELECT escm_biddates_id AS referencekey_id, ad_column_identifier('escm_biddates', escm_biddates_id, 'en_US') AS record_id,"
        + " 'OpenEnvelop date is reached for this bidno ' || bidno  AS description,bidno  "
        + " FROM escm_biddates dates "
        + " join escm_bidmgmt bid on dates.escm_bidmgmt_id = bid.escm_bidmgmt_id "
        + " where concat(to_char(openenvday,'yyyy-MM-dd'),' ',openenvdaytime) > "
        + "           to_char(now()- interval '00:05','yyyy-MM-dd HH24:MI') "
        + "           and  concat(to_char(openenvday,'yyyy-MM-dd'),' ',openenvdaytime)<= "
        + "           to_char(now(),'yyyy-MM-dd HH24:MI') "
        + "           and bid.Bidappstatus='ESCM_AP' and bid.ad_client_id='" + clientId + "'";

    query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
    if (query != null) {
      visibleList = query.list();
    }

    return visibleList;
  }
}