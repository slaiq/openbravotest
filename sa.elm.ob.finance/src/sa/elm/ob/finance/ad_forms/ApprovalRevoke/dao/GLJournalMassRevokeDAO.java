package sa.elm.ob.finance.ad_forms.ApprovalRevoke.dao;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.financialmgmt.gl.GLJournal;

import sa.elm.ob.finance.ad_process.simpleGlJournal.SimpleGlJournalDAO;
import sa.elm.ob.finance.util.AlertUtility;
import sa.elm.ob.finance.util.AlertWindow;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.ApprovalRevoke.massrevoke.ApprovalRevokeVO;
import sa.elm.ob.utility.ad_forms.ApprovalRevoke.massrevoke.MassRevoke;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Poongodi on 15/04/2019
 *
 */
public class GLJournalMassRevokeDAO extends MassRevoke {
  private static Logger LOG = Logger.getLogger(GLJournalMassRevokeDAO.class);
  private static final String REVOKE = "REV";

  String fromClause = "select main.id, main.org, main.requester,main.documentno,main.lastaction,main.em_eut_next_role_id,main.docstatus from "
      + "(select org.name as org,gl.documentno as documentno,gl.docstatus,gl.gl_journal_id as id,rr.name as requester, "
      + "(select (coalesce(trl.name,st.name)||' - '|| ur.name) from eut_journal_approval history, ad_user ur, ad_ref_list st "
      + " left join ad_ref_list_trl trl on trl.ad_ref_list_id=st.ad_ref_list_id and trl.ad_language=(select lang.ad_language "
      + " from ad_language lang  where lang.ad_language_id='112') where history.gl_journal_id = gl.gl_journal_id and st.value = history.Requestreqaction "
      + " and st.ad_reference_id='01FE5CDA506845E6B9F4D7B280D13265' and ur.ad_user_id = history.createdby order by history.updated desc limit 1) as lastaction, "
      + " pen.pending,gl.em_eut_next_role_id from gl_journal gl "
      + " left join ad_user rr on rr.ad_user_id=gl.createdby "
      + " left join ad_org org on org.ad_org_id=gl.ad_org_id "
      + " left join (select array_to_string(array_agg(role.name),' / ') as pending, lin.eut_next_role_id  from eut_next_role rl "
      + " join eut_next_role_line lin on lin.eut_next_role_id=rl.eut_next_role_id "
      + " join ad_role role on role.ad_role_id=lin.ad_role_id  group by lin.eut_next_role_id ) as pen on pen.eut_next_role_id=gl.em_eut_next_role_id ";

  @SuppressWarnings("unchecked")
  public int getRevokeRecordsCount(VariablesSecureApp vars, String clientId, String windowId,
      String searchFlag, ApprovalRevokeVO vo) {
    int totalRecord = 0;
    String sqlQuery = "", whereClause = "";

    try {

      whereClause = " where gl.ad_client_id = '" + clientId + "'";

      whereClause += " and gl.docstatus='EFIN_WFA' and gl.em_eut_next_role_id is not null "
          + " and gl.ad_org_id in ( select org.ad_org_id from ad_user rr  "
          + " left join ad_user_roles usrrole on usrrole.ad_user_id = rr.ad_user_id "
          + " left join ad_role role on role.ad_role_id = usrrole.ad_role_id "
          + " left join ad_role_orgaccess orgrole on orgrole.ad_role_id= role.ad_role_id "
          + " left join ad_org org on org.ad_org_id= orgrole.ad_org_id where rr.ad_user_id='"
          + vars.getUser() + "')";

      if (searchFlag.equals("true")) {
        if (vo.getOrgName() != null)
          whereClause += " and org.name ilike '%" + vo.getOrgName() + "%'";
        if (vo.getRequester() != null)
          whereClause += " and rr.name ilike '%" + vo.getRequester() + "%'";
        if (vo.getDocno() != null)
          whereClause += " and gl.documentno ilike '%" + vo.getDocno() + "%'";
        if (vo.getNextrole() != null)
          whereClause += " and pen.pending ilike '%" + vo.getNextrole() + "%'";
        if (vo.getStatus() != null) {
          if (("Waiting For Approval").contains(vo.getStatus().toLowerCase())
              || vo.getStatus().isEmpty()) {
            whereClause += " and gl.DocStatus='EFIN_WFA' ";
          } else {
            whereClause += " and 1=2";
          }
        }
      }

      sqlQuery += fromClause + whereClause + " ) main ";
      if (searchFlag.equals("true")) {
        if (vo.getLastperfomer() != null)
          sqlQuery += " where  main.lastaction ilike '%" + vo.getLastperfomer() + "%'";
      }

      SQLQuery queryList = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
      LOG.debug("sqlQuery" + sqlQuery);
      if (queryList != null) {
        List<Object> rows = queryList.list();
        if (rows.size() > 0) {
          totalRecord = rows.size();
        }
      }

    } catch (final Exception e) {
      if (LOG.isDebugEnabled())
        LOG.debug("Exception While Getting Records of Encumbrance for mass revoke", e);
    }
    return totalRecord;
  }

  @SuppressWarnings("rawtypes")
  public List<ApprovalRevokeVO> getRevokeRecordsList(VariablesSecureApp vars, String clientId,
      String windowId, ApprovalRevokeVO vo, int limit, int offset, String sortColName,
      String sortColType, String searchFlag, String lang) {
    LOG.debug("sort" + sortColType);
    List<ApprovalRevokeVO> ls = new ArrayList<ApprovalRevokeVO>();

    String sqlQuery = "", whereClause = "", orderClause = "";
    OBContext.setAdminMode();

    try {

      whereClause = " where gl.ad_client_id = '" + clientId + "'";

      whereClause += " and gl.docstatus='EFIN_WFA' and gl.em_eut_next_role_id is not null "
          + " and gl.ad_org_id in ( select org.ad_org_id from ad_user rr  "
          + " left join ad_user_roles usrrole on usrrole.ad_user_id = rr.ad_user_id "
          + " left join ad_role role on role.ad_role_id = usrrole.ad_role_id "
          + " left join ad_role_orgaccess orgrole on orgrole.ad_role_id= role.ad_role_id "
          + " left join ad_org org on org.ad_org_id= orgrole.ad_org_id where rr.ad_user_id='"
          + vars.getUser() + "')";

      if (searchFlag.equals("true")) {
        if (vo.getOrgName() != null)
          whereClause += " and org.name ilike '%" + vo.getOrgName() + "%'";
        if (vo.getRequester() != null)
          whereClause += " and rr.name ilike '%" + vo.getRequester() + "%'";
        if (vo.getDocno() != null)
          whereClause += " and gl.documentno ilike '%" + vo.getDocno() + "%'";
        if (vo.getNextrole() != null)
          whereClause += " and pen.pending ilike '%" + vo.getNextrole() + "%'";
        if (vo.getStatus() != null) {
          if (("Waiting For Approval").contains(vo.getStatus().toLowerCase())
              || vo.getStatus().isEmpty()) {
            whereClause += " and gl.DocStatus='EFIN_WFA' ";
          } else {
            whereClause += " and 1=2";
          }
        }
      }

      if (sortColName != null && sortColName.equals("org"))
        orderClause += " order by org.name  " + sortColType + " limit " + limit + " offset "
            + offset;
      else if (sortColName != null && sortColName.equals("docno")) {
        orderClause += " order by gl.documentno " + sortColType + " limit " + limit + " offset "
            + offset;
      } else if (sortColName != null && sortColName.equals("requester"))
        orderClause += " order by rr.name " + sortColType + " limit " + limit + " offset " + offset;
      else if (sortColName != null && sortColName.equals("nextrole"))
        orderClause += " order by pen.pending " + sortColType + " limit " + limit + " offset "
            + offset;
      else if (sortColName != null && sortColName.equals("status")) {
        orderClause += " order by gl.DocStatus='EFIN_WFA' " + sortColType + " limit " + limit
            + " offset " + offset;
      } else {
        orderClause += " order by gl.documentno desc" + " limit " + limit + " offset " + offset;
      }

      sqlQuery = fromClause + whereClause + orderClause + ") main ";
      if (searchFlag.equals("true")) {
        if (vo.getLastperfomer() != null)
          sqlQuery += " where  main.lastaction ilike '%" + vo.getLastperfomer() + "%'";
      }
      if (sortColName != null && sortColName.equals("lastperformer"))
        sqlQuery += " order by main.lastaction  " + sortColType + " limit " + limit + " offset "
            + offset;
      LOG.debug("encum mass revoke sqlQuery : " + sqlQuery);

      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
      List queryList = query.list();
      if (queryList != null && queryList.size() > 0) {
        for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();

          String nextRole = "";
          ApprovalRevokeVO apVO = new ApprovalRevokeVO();
          apVO.setRecordId(Utility.nullToEmpty(row[0].toString()));
          apVO.setOrgName(Utility.nullToEmpty(row[1].toString()));
          apVO.setRequester(Utility.nullToEmpty(row[2].toString()));
          apVO.setDocno(Utility.nullToEmpty(row[3].toString()));
          apVO.setLastperfomer(Utility.nullToEmpty(row[4].toString()));

          EutNextRole objNxtRole = OBDal.getInstance().get(EutNextRole.class,
              Utility.nullToEmpty(row[5].toString()));
          if (objNxtRole != null && objNxtRole.getEutNextRoleLineList().size() > 0) {
            if (objNxtRole.getEutNextRoleLineList().size() == 1) {
              OBQuery<UserRoles> ur = OBDal.getInstance().createQuery(UserRoles.class,
                  "role.id='" + objNxtRole.getEutNextRoleLineList().get(0).getRole().getId() + "'");
              if (ur != null && ur.list().size() > 0) {
                if (ur.list().size() == 1) {
                  if (StringUtils.isEmpty(nextRole))
                    nextRole += (ur.list().get(0).getUserContact().getName());
                  else
                    nextRole += (" / " + ur.list().get(0).getUserContact().getName());
                } else {
                  if (StringUtils.isEmpty(nextRole))
                    nextRole += (ur.list().get(0).getRole().getName());
                  else
                    nextRole += (" / " + ur.list().get(0).getRole().getName());
                }

              }
            } else {
              for (EutNextRoleLine objLine : objNxtRole.getEutNextRoleLineList()) {
                OBQuery<UserRoles> ur = OBDal.getInstance().createQuery(UserRoles.class,
                    "role.id='" + objLine.getRole().getId() + "'");
                if (ur != null && ur.list().size() > 0) {
                  if (ur.list().size() == 1) {
                    if (StringUtils.isEmpty(nextRole))
                      nextRole += (ur.list().get(0).getUserContact().getName());
                    else
                      nextRole += (" / " + ur.list().get(0).getUserContact().getName());
                  } else {
                    if (StringUtils.isEmpty(nextRole))
                      nextRole += (ur.list().get(0).getRole().getName());
                    else
                      nextRole += (" / " + ur.list().get(0).getRole().getName());
                  }

                }

              }
            }
          }
          apVO.setNextrole(nextRole);
          if (Utility.nullToEmpty(row[6].toString()).equals("EFIN_WFA")) {
            apVO.setStatus(Resource.getProperty("utility.inprogress", lang));
          } else {
            apVO.setStatus("");
          }

          ls.add(apVO);
        }
      }

    } catch (final Exception e) {
      if (LOG.isDebugEnabled())
        LOG.debug("Exception While Getting Records of gljournal for mass revoke", e);
    }
    return ls;
  }

  @Override
  public String updateRecord(VariablesSecureApp var, String selectIds, String inpWindowId) {

    String Result = "Success";
    String alertWindow = AlertWindow.GlJournal, alertRuleId = "", Description = "";
    ArrayList<String> includeRecipient = new ArrayList<String>();
    EutNextRole nextRole = null;
    Connection conn = OBDal.getInstance().getConnection();
    JSONObject historyData = new JSONObject();
    try {
      List<String> result = Arrays.asList(selectIds.split("\\s*,\\s*"));
      for (int i = 0; i < result.size(); i++) {
        GLJournal header = OBDal.getInstance().get(GLJournal.class, result.get(i));
        nextRole = OBDal.getInstance().get(EutNextRole.class, header.getEutNextRole().getId());

        // update header status
        header.setUpdated(new java.util.Date());
        header.setUpdatedBy(OBContext.getOBContext().getUser());
        header.setDocumentStatus("DR");
        header.setEfinAction("CO");
        header.setEutNextRole(null);

        OBDal.getInstance().save(header);

        // insert approval history
        if (!StringUtils.isEmpty(header.getId())) {
          historyData = getHistoryData(header);
          SimpleGlJournalDAO.glJournalHistory(historyData);
        }

        // alert process
        if (!StringUtils.isEmpty(header.getId())) {
          Role objCreatedRole = null;
          if (header.getCreatedBy().getADUserRolesList().size() > 0) {
            objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
          }

          // get alert rule id
          OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
              "as e where e.client.id= :clientID and e.efinProcesstype= :Processtype");
          queryAlertRule.setNamedParameter("clientID", header.getClient().getId());
          queryAlertRule.setNamedParameter("Processtype", alertWindow);
          if (queryAlertRule.list().size() > 0) {
            AlertRule objRule = queryAlertRule.list().get(0);
            alertRuleId = objRule.getId();
          }

          // remove approval alert
          OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
              "as e where e.referenceSearchKey= :searchKey and e.alertStatus='NEW'");
          alertQuery.setNamedParameter("searchKey", result.get(i));
          if (alertQuery.list().size() > 0) {
            for (Alert objAlert : alertQuery.list()) {
              objAlert.setAlertStatus("SOLVED");
              OBDal.getInstance().save(objAlert);
            }
          }

          // check and insert alert recipient
          OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
              .createQuery(AlertRecipient.class, "as e where e.alertRule.id= :alertRuleID");
          receipientQuery.setNamedParameter("alertRuleID", alertRuleId);
          if (receipientQuery.list().size() > 0) {
            for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
              includeRecipient.add(objAlertReceipient.getRole().getId());
              OBDal.getInstance().remove(objAlertReceipient);
            }
          }
          if (includeRecipient != null)
            includeRecipient.add(objCreatedRole.getId());

          // avoid duplicate recipient
          HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
          Iterator<String> iterator = incluedSet.iterator();
          while (iterator.hasNext()) {
            AlertUtility.insertAlertRecipient(iterator.next(), null, var.getClient(), alertWindow);
          }

          // set alert for next approver
          Description = Resource.getProperty("utility.gljournal.revoked", var.getLanguage()) + " "
              + header.getCreatedBy().getName();

          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(),
                objNextRoleLine.getRole().getId(), "", header.getClient().getId(), Description,
                "NEW", alertWindow, "utility.gljournal.revoked", Constants.GENERIC_TEMPLATE);
          }

          // delete next role records for revoke
          DocumentRuleDAO.deleteUnusedNextRoles(conn, Resource.GLJOURNAL_RULE);

          // Remove temporary encumbrance created
          if (header.getEFINBudgetManencum() != null) {
            SimpleGlJournalDAO.removeTemporaryEncumbrance(header);
          }

          Result = "Success";
          OBDal.getInstance().save(header);
          OBDal.getInstance().flush();
        }
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      if (LOG.isDebugEnabled()) {
        LOG.error("Exception While Revoke Encumbrance :", e);
      }
      Result = "Error";
    }
    return Result;
  }

  @Override
  public String validateRecord(String selectIds, String inpWindowId) {
    String ids = null;
    List<String> result = Arrays.asList(selectIds.split("\\s*,\\s*"));
    for (int i = 0; i < result.size(); i++) {
      GLJournal enc = OBDal.getInstance().get(GLJournal.class, result.get(i));
      if (enc.getDocumentStatus().equals("DR")) {
        if (ids == null) {
          ids = enc.getDocumentNo();
        } else {
          ids = ids + ", " + enc.getDocumentNo();
        }
      }
    }
    return ids;
  }

  public static JSONObject getHistoryData(GLJournal journal) {
    JSONObject historyData = new JSONObject();

    try {

      historyData.put("ClientId", journal.getClient().getId());
      historyData.put("OrgId", journal.getOrganization().getId());
      historyData.put("RoleId", OBContext.getOBContext().getRole().getId());
      historyData.put("UserId", OBContext.getOBContext().getUser().getId());
      historyData.put("HeaderId", journal.getId());
      historyData.put("Status", REVOKE);
      historyData.put("NextApprover", "");
      historyData.put("HistoryTable", ApprovalTables.GL_JOURNAL_HISTORY);
      historyData.put("HeaderColumn", ApprovalTables.GL_JOURNAL_HEADER_COLUMN);
      historyData.put("ActionColumn", ApprovalTables.GL_JOURNAL_DOCACTION_COLUMN);

    } catch (Exception e) {
      if (LOG.isDebugEnabled())
        LOG.error("Exception while getHistoryData: " + e);
    }
    return historyData;
  }

}
