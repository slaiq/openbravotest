package sa.elm.ob.finance.ad_forms.ApprovalRevoke.dao;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
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

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.ad_process.dao.ManualEncumbaranceRevokeDAO;
import sa.elm.ob.finance.ad_process.dao.ManualEncumbaranceSubmitDAO;
import sa.elm.ob.finance.util.AlertUtility;
import sa.elm.ob.finance.util.AlertWindow;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.ApprovalRevoke.massrevoke.ApprovalRevokeVO;
import sa.elm.ob.utility.ad_forms.ApprovalRevoke.massrevoke.MassRevoke;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Gowtham V
 *
 */
public class EncumMassRevokeDAO extends MassRevoke {
  private static Logger LOG = Logger.getLogger(EncumMassRevokeDAO.class);
  private static final String REVOKE = "REV";

  String fromClause = "select main.id, main.org, main.requester,main.documentno,main.lastaction,main.eut_next_role_id,main.docstatus from "
      + "(select org.name as org,enc.documentno as documentno,enc.docstatus,enc.efin_budget_manencum_id as id,rr.name as requester, "
      + "(select (coalesce(trl.name,st.name)||' - '|| ur.name) from efin_budget_encum_app_hist history, ad_user ur, ad_ref_list st "
      + " left join ad_ref_list_trl trl on trl.ad_ref_list_id=st.ad_ref_list_id and trl.ad_language=(select lang.ad_language "
      + " from ad_language lang  where lang.ad_language_id='112') where history.efin_budget_manencum_id = enc.efin_budget_manencum_id and st.value = history.encum_action "
      + " and st.ad_reference_id='9F2DC8F55FE9442895FCD3ED468B1D50' and ur.ad_user_id = history.createdby order by history.updated desc limit 1) as lastaction, "
      + " pen.pending,enc.eut_next_role_id from efin_budget_manencum enc "
      + " left join ad_user rr on rr.ad_user_id=enc.createdby "
      + " left join ad_org org on org.ad_org_id=enc.ad_org_id "
      + " left join (select array_to_string(array_agg(role.name),' / ') as pending, lin.eut_next_role_id  from eut_next_role rl "
      + " join eut_next_role_line lin on lin.eut_next_role_id=rl.eut_next_role_id "
      + " join ad_role role on role.ad_role_id=lin.ad_role_id  group by lin.eut_next_role_id ) as pen on pen.eut_next_role_id=enc.eut_next_role_id ";

  @SuppressWarnings("unchecked")
  public int getRevokeRecordsCount(VariablesSecureApp vars, String clientId, String windowId,
      String searchFlag, ApprovalRevokeVO vo) {
    int totalRecord = 0;
    String sqlQuery = "", whereClause = "";

    try {

      whereClause = " where enc.ad_client_id = '" + clientId + "'";

      whereClause += " and enc.docstatus='WFA' and enc.eut_next_role_id is not null "
          + " and enc.ad_org_id in ( select org.ad_org_id from ad_user rr  "
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
          whereClause += " and enc.documentno ilike '%" + vo.getDocno() + "%'";
        if (vo.getNextrole() != null)
          whereClause += " and pen.pending ilike '%" + vo.getNextrole() + "%'";
        if (vo.getStatus() != null) {
          if (("in progress").contains(vo.getStatus().toLowerCase()) || vo.getStatus().isEmpty()) {
            whereClause += " and enc.DocStatus='WFA' ";
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
      e.printStackTrace();
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

      whereClause = " where enc.ad_client_id = '" + clientId + "'";

      whereClause += " and enc.docstatus='WFA' and enc.eut_next_role_id is not null "
          + " and enc.ad_org_id in ( select org.ad_org_id from ad_user rr  "
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
          whereClause += " and enc.documentno ilike '%" + vo.getDocno() + "%'";
        if (vo.getNextrole() != null)
          whereClause += " and pen.pending ilike '%" + vo.getNextrole() + "%'";
        if (vo.getStatus() != null) {
          if (("in progress").contains(vo.getStatus().toLowerCase()) || vo.getStatus().isEmpty()) {
            whereClause += " and enc.DocStatus='WFA' ";
          } else {
            whereClause += " and 1=2";
          }
        }
      }

      if (sortColName != null && sortColName.equals("org"))
        orderClause += " order by org.name  " + sortColType + " limit " + limit + " offset "
            + offset;
      else if (sortColName != null && sortColName.equals("docno")) {
        orderClause += " order by enc.documentno " + sortColType + " limit " + limit + " offset "
            + offset;
      } else if (sortColName != null && sortColName.equals("requester"))
        orderClause += " order by rr.name " + sortColType + " limit " + limit + " offset " + offset;
      else if (sortColName != null && sortColName.equals("nextrole"))
        orderClause += " order by pen.pending " + sortColType + " limit " + limit + " offset "
            + offset;
      else if (sortColName != null && sortColName.equals("status")) {
        orderClause += " order by enc.DocStatus='WFA' " + sortColType + " limit " + limit
            + " offset " + offset;
      } else {
        orderClause += " order by enc.documentno desc" + " limit " + limit + " offset " + offset;
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
          if (Utility.nullToEmpty(row[6].toString()).equals("WFA")) {
            apVO.setStatus(Resource.getProperty("utility.inprogress", lang));
          } else {
            apVO.setStatus("");
          }

          ls.add(apVO);
        }
      }

    } catch (final Exception e) {
      e.printStackTrace();
      if (LOG.isDebugEnabled())
        LOG.debug("Exception While Getting Records of Encumbrance for mass revoke", e);
    }
    return ls;
  }

  @Override
  public String updateRecord(VariablesSecureApp var, String selectIds, String inpWindowId) {

    String Result = "Success";
    String alertWindow = AlertWindow.Encumbrance, alertRuleId = "", comments = "", Description = "";
    ArrayList<String> includeRecipient = new ArrayList<String>();
    EutNextRole nextRole = null;
    Connection conn = OBDal.getInstance().getConnection();
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    try {
      List<String> result = Arrays.asList(selectIds.split("\\s*,\\s*"));
      for (int i = 0; i < result.size(); i++) {
        EfinBudgetManencum header = OBDal.getInstance().get(EfinBudgetManencum.class,
            result.get(i));
        nextRole = OBDal.getInstance().get(EutNextRole.class, header.getNextRole().getId());

        // update header status
        header.setUpdated(new java.util.Date());
        header.setUpdatedBy(OBContext.getOBContext().getUser());
        header.setAction("CO");
        header.setDocumentStatus("DR");
        header.setNextRole(null);
        OBDal.getInstance().save(header);

        // if reserve funds crossed then revert the impacts of budget enquiry.
        if (header.isReservedfund())
          ManualEncumbaranceRevokeDAO.revokeBudgetEnquiryImpact(header);

        // insert approval history
        if (!StringUtils.isEmpty(header.getId())) {
          ManualEncumbaranceSubmitDAO.insertManEncumHistory(conn, header.getClient().getId(),
              header.getOrganization().getId(), var.getRole(), var.getUser(), header.getId(),
              comments, REVOKE, null);
          // ManualEncumbranceRevoke.RemoveEncumRecord(conn, var.getClient(),
          // header.getOrganization().getId(), var.getRole(), var.getUser(), header.getId());
        }

        // alert process
        if (!StringUtils.isEmpty(header.getId())) {
          Role objCreatedRole = null;
          if (header.getCreatedBy().getADUserRolesList().size() > 0) {
            objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
          }

          // get alert rule id
          OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
              "as e where e.client.id='" + header.getClient().getId() + "' and e.efinProcesstype='"
                  + alertWindow + "'");
          if (queryAlertRule.list().size() > 0) {
            AlertRule objRule = queryAlertRule.list().get(0);
            alertRuleId = objRule.getId();
          }

          // remove approval alert
          OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
              "as e where e.referenceSearchKey='" + result.get(i) + "' and e.alertStatus='NEW'");
          if (alertQuery.list().size() > 0) {
            for (Alert objAlert : alertQuery.list()) {
              objAlert.setAlertStatus("SOLVED");
              OBDal.getInstance().save(objAlert);
            }
          }

          // check and insert alert recipient
          OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
              .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");
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
          Description = Resource.getProperty("utility.encumbrance.revoked", var.getLanguage()) + " "
              + header.getCreatedBy().getName();

          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(),
                objNextRoleLine.getRole().getId(), "", header.getClient().getId(), Description,
                "NEW", alertWindow, "utility.encumbrance.revoked", Constants.GENERIC_TEMPLATE);
          }

          // delete next role records for revoke
          DocumentRuleDAO.deleteUnusedNextRoles(conn, Resource.MANUAL_ENCUMBRANCE_RULE);

          // Removing the Forward and RMI id
          if (header.getEUTForwardReqmoreinfo() != null) {
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTForwardReqmoreinfo());
            forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(header.getId(),
                Constants.ENCUMBRANCE);
          }
          if (header.getEUTReqmoreinfo() != null) {
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTReqmoreinfo());
            forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(header.getId(), Constants.ENCUMBRANCE);
          }
          Result = "Success";
          OBDal.getInstance().save(header);
          OBDal.getInstance().flush();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
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
      EfinBudgetManencum enc = OBDal.getInstance().get(EfinBudgetManencum.class, result.get(i));
      if (enc.getDocumentStatus().equals("DR") || enc.getDocumentStatus().equals("ESCM_TR")) {
        if (ids == null) {
          ids = enc.getDocumentNo();
        } else {
          ids = ids + ", " + enc.getDocumentNo();
        }
      }
    }
    return ids;
  }

}
