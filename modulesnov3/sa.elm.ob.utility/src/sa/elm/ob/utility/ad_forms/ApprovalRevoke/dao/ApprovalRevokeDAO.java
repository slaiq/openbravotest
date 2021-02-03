package sa.elm.ob.utility.ad_forms.ApprovalRevoke.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.ApprovalRevoke.massrevoke.ApprovalRevokeVO;
import sa.elm.ob.utility.ad_forms.ApprovalRevoke.massrevoke.MassRevoke;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

public class ApprovalRevokeDAO extends MassRevoke {
  private Connection conn = null;
  private static Logger log4j = Logger.getLogger(ApprovalRevokeDAO.class);

  public ApprovalRevokeDAO(Connection con) {
    this.conn = con;
  }

  /**
   * 
   * @param clientId
   * @param windowId
   * @param searchFlag
   * @param vo
   * @return records waiting for approval records
   */
  public int getRevokeRecordsCount(VariablesSecureApp vars, String clientId, String windowId,
      String searchFlag, ApprovalRevokeVO vo) {
    PreparedStatement st = null;
    ResultSet rs = null;
    int totalRecord = 0;
    String sqlQuery = "", fromClause = "", whereClause = "";

    try {
      // for material issue transaction and site material transaction
      if (windowId.equals("MIR") || windowId.equals("SIR")) {
        fromClause = " select count(*) as totalRecord from (select org.name as org ,req.documentno,req.status,req.escm_material_request_id as id ,rr.name as requester, "
            + " (select (coalesce(trl.name,st.name)||' - '|| ur.name) "
            + "  from escm_materialrequest_hist  history,ad_user ur,ad_ref_list st  "
            + " left join ad_ref_list_trl trl on trl.ad_ref_list_id=st.ad_ref_list_id and  "
            + " trl.ad_language=(select lang.ad_language from ad_language lang where lang.ad_language_id='112') "
            + " where history.escm_material_request_id  = req.escm_material_request_id "
            + " and st.value  =history.requestreqaction and st.ad_reference_id='9F2DC8F55FE9442895FCD3ED468B1D50'  "
            + " and ur.ad_user_id = history.createdby order by history.updated desc limit 1) as lastaction,pen.pending,req.eut_next_role_id  "
            + " from escm_material_request req "
            + " left join ad_user rr on rr.ad_user_id=req.createdby "
            + " left join ad_org org on org.ad_org_id=req.ad_org_id "
            + " left join (select array_to_string(array_agg(role.name),' / ') as pending,lin.eut_next_role_id from eut_next_role rl "
            + " join eut_next_role_line lin on lin.eut_next_role_id=rl.eut_next_role_id "
            + " join ad_role role on role.ad_role_id=lin.ad_role_id "
            + " group by lin.eut_next_role_id ) as pen on pen.eut_next_role_id=req.eut_next_role_id ";

        whereClause = " where req.ad_client_id = '" + clientId + "'";

        whereClause += " and req.status='ESCM_IP' and req.eut_next_role_id is not null and req.ad_org_id  in (  select org.ad_org_id from ad_user rr  "
            + " left join ad_user_roles usrrole on usrrole.ad_user_id = rr.ad_user_id "
            + " left join ad_role role on role.ad_role_id = usrrole.ad_role_id "
            + " left join ad_role_orgaccess orgrole on orgrole.ad_role_id= role.ad_role_id "
            + " left join ad_org org on org.ad_org_id= orgrole.ad_org_id where rr.ad_user_id='"
            + vars.getUser() + "')";
        if (windowId.equals("MIR")) {
          whereClause += " and req.issiteissuereq <> 'Y'";
        } else if (windowId.equals("SIR")) {
          whereClause += " and req.issiteissuereq ='Y'";
        }
      } else {
        // for custody transfer and return transaction
        fromClause = "  select count(*) as totalRecord  from (select org.name as org ,tran.documentno,tran.em_escm_docstatus,tran.m_inout_id as id ,rr.name as requester, "
            + " (select (coalesce(trl.name,st.name)||' - '|| ur.name)   from escm_custodytransfer_hist  history,ad_user ur,ad_ref_list st   "
            + " left join ad_ref_list_trl trl on trl.ad_ref_list_id=st.ad_ref_list_id and  "
            + " trl.ad_language=(select lang.ad_language from ad_language lang where lang.ad_language_id='112')  "
            + " where history.m_inout_id  = tran.m_inout_id  "
            + " and st.value  =history.requestreqaction and st.ad_reference_id='9F2DC8F55FE9442895FCD3ED468B1D50'   "
            + " and ur.ad_user_id = history.createdby order by history.updated desc limit 1) as lastaction,pen.pending,tran.em_eut_next_role_id  "
            + " from m_inout tran  left join ad_user rr on rr.ad_user_id=tran.createdby  "
            + " left join ad_org org on org.ad_org_id=tran.ad_org_id  "
            + " left join (select array_to_string(array_agg(role.name),' / ') as pending,lin.eut_next_role_id from eut_next_role rl  "
            + " join eut_next_role_line lin on lin.eut_next_role_id=rl.eut_next_role_id  join ad_role role on role.ad_role_id=lin.ad_role_id  "
            + " group by lin.eut_next_role_id ) as pen on pen.eut_next_role_id=tran.em_eut_next_role_id   ";

        whereClause = " where tran.ad_client_id = '" + clientId + "'";

        whereClause += " and tran.em_escm_docstatus='ESCM_IP'"
            + "  and tran.em_eut_next_role_id is not null and tran.ad_org_id  in (  select org.ad_org_id from ad_user rr   "
            + "  left join ad_user_roles usrrole on usrrole.ad_user_id = rr.ad_user_id  "
            + "  left join ad_role role on role.ad_role_id = usrrole.ad_role_id  "
            + "  left join ad_role_orgaccess orgrole on orgrole.ad_role_id= role.ad_role_id  "
            + "  left join ad_org org on org.ad_org_id= orgrole.ad_org_id where rr.ad_user_id='"
            + vars.getUser() + "')";

        if (windowId.equals("CT")) {
          whereClause += " and tran.em_escm_iscustody_transfer ='Y'";

        } else {
          whereClause += " and tran.em_escm_iscustody_transfer ='N'";

        }

      }

      if (searchFlag.equals("true")) {
        if (vo.getOrgName() != null)
          whereClause += " and org.name ilike '%" + vo.getOrgName() + "%'";
        if (vo.getRequester() != null)
          whereClause += " and rr.name ilike '%" + vo.getRequester() + "%'";
        if (vo.getDocno() != null)
          if (windowId.equals("MIR") || windowId.equals("SIR")) {
            whereClause += " and req.documentno ilike '%" + vo.getDocno() + "%'";
          } else {
            whereClause += " and tran.documentno ilike '%" + vo.getDocno() + "%'";
          }
        if (vo.getNextrole() != null)
          whereClause += " and pen.pending ilike '%" + vo.getNextrole() + "%'";
        if (vo.getStatus() != null) {
          if (("in progress").contains(vo.getStatus().toLowerCase()) || vo.getStatus().isEmpty()) {
            if (windowId.equals("MIR") || windowId.equals("SIR")) {
              whereClause += " and req.status ='ESCM_IP'";
            } else {
              whereClause += " and tran.em_escm_docstatus ='ESCM_IP'";
            }
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
      st = conn.prepareStatement(sqlQuery);
      rs = st.executeQuery();
      if (rs.next())
        totalRecord = rs.getInt("totalRecord");
    } catch (final SQLException e) {
      log4j.error("", e);
    } catch (final Exception e) {
      log4j.error("", e);
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (st != null) {
          st.close();
        }
      } catch (final SQLException e) {
        log4j.error("", e);
      }
    }
    return totalRecord;
  }

  /**
   * 
   * @param clientId
   * @param windowId
   * @param vo
   * @param limit
   * @param offset
   * @param sortColName
   * @param sortColType
   * @param searchFlag
   * @return getRevokeRecordsList
   */
  public List<ApprovalRevokeVO> getRevokeRecordsList(VariablesSecureApp vars, String clientId,
      String windowId, ApprovalRevokeVO vo, int limit, int offset, String sortColName,
      String sortColType, String searchFlag, String lang) {
    log4j.debug("sort" + sortColType);
    PreparedStatement st = null;
    ResultSet rs = null;
    List<ApprovalRevokeVO> ls = new ArrayList<ApprovalRevokeVO>();
    // DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
    // SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
    String sqlQuery = "", fromClause = "", whereClause = "", orderClause = "";
    OBContext.setAdminMode();

    try {

      if (windowId.equals("MIR") || windowId.equals("SIR")) {
        fromClause = " select * from (select org.name as org ,req.documentno,req.status,req.escm_material_request_id as id ,rr.name as requester, "
            + " (select (coalesce(trl.name,st.name)||' - '|| ur.name) "
            + "  from escm_materialrequest_hist  history,ad_user ur,ad_ref_list st  "
            + " left join ad_ref_list_trl trl on trl.ad_ref_list_id=st.ad_ref_list_id and  "
            + " trl.ad_language=(select lang.ad_language from ad_language lang where lang.ad_language_id='112') "
            + " where history.escm_material_request_id  = req.escm_material_request_id "
            + " and st.value  =history.requestreqaction and st.ad_reference_id='9F2DC8F55FE9442895FCD3ED468B1D50'  "
            + " and ur.ad_user_id = history.createdby order by history.updated desc limit 1) as lastaction,pen.pending,req.eut_next_role_id  "
            + " from escm_material_request req "
            + " left join ad_user rr on rr.ad_user_id=req.createdby "
            + " left join ad_org org on org.ad_org_id=req.ad_org_id "
            + " left join (select array_to_string(array_agg(role.name),' / ') as pending,lin.eut_next_role_id from eut_next_role rl "
            + " join eut_next_role_line lin on lin.eut_next_role_id=rl.eut_next_role_id "
            + " join ad_role role on role.ad_role_id=lin.ad_role_id "
            + " group by lin.eut_next_role_id ) as pen on pen.eut_next_role_id=req.eut_next_role_id ";

        whereClause = " where req.ad_client_id = '" + clientId + "'";

        whereClause += " and req.status='ESCM_IP' and req.eut_next_role_id is not null and req.ad_org_id  in (  select org.ad_org_id from ad_user rr  "
            + " left join ad_user_roles usrrole on usrrole.ad_user_id = rr.ad_user_id "
            + " left join ad_role role on role.ad_role_id = usrrole.ad_role_id "
            + " left join ad_role_orgaccess orgrole on orgrole.ad_role_id= role.ad_role_id "
            + " left join ad_org org on org.ad_org_id= orgrole.ad_org_id where rr.ad_user_id='"
            + vars.getUser() + "')";
        if (windowId.equals("MIR")) {
          whereClause += " and req.issiteissuereq <> 'Y'";
        } else if (windowId.equals("SIR")) {
          whereClause += " and req.issiteissuereq ='Y'";
        }
      } else {
        fromClause = "  select * from (select org.name as org ,tran.documentno,tran.em_escm_docstatus,tran.m_inout_id as id ,rr.name as requester, "
            + " (select (coalesce(trl.name,st.name)||' - '|| ur.name)   from escm_custodytransfer_hist  history,ad_user ur,ad_ref_list st   "
            + " left join ad_ref_list_trl trl on trl.ad_ref_list_id=st.ad_ref_list_id and  "
            + " trl.ad_language=(select lang.ad_language from ad_language lang where lang.ad_language_id='112')  "
            + " where history.m_inout_id  = tran.m_inout_id  "
            + " and st.value  =history.requestreqaction and st.ad_reference_id='9F2DC8F55FE9442895FCD3ED468B1D50'   "
            + " and ur.ad_user_id = history.createdby order by history.updated desc limit 1) as lastaction,pen.pending,tran.em_eut_next_role_id  "
            + " from m_inout tran  left join ad_user rr on rr.ad_user_id=tran.createdby  "
            + " left join ad_org org on org.ad_org_id=tran.ad_org_id  "
            + " left join (select array_to_string(array_agg(role.name),' / ') as pending,lin.eut_next_role_id from eut_next_role rl  "
            + " join eut_next_role_line lin on lin.eut_next_role_id=rl.eut_next_role_id  join ad_role role on role.ad_role_id=lin.ad_role_id  "
            + " group by lin.eut_next_role_id ) as pen on pen.eut_next_role_id=tran.em_eut_next_role_id   ";

        whereClause = " where tran.ad_client_id = '" + clientId + "'";

        whereClause += " and tran.em_escm_docstatus='ESCM_IP'"
            + "  and tran.em_eut_next_role_id is not null and tran.ad_org_id  in (  select org.ad_org_id from ad_user rr   "
            + "  left join ad_user_roles usrrole on usrrole.ad_user_id = rr.ad_user_id  "
            + "  left join ad_role role on role.ad_role_id = usrrole.ad_role_id  "
            + "  left join ad_role_orgaccess orgrole on orgrole.ad_role_id= role.ad_role_id  "
            + "  left join ad_org org on org.ad_org_id= orgrole.ad_org_id where rr.ad_user_id='"
            + vars.getUser() + "')";

        if (windowId.equals("CT")) {
          whereClause += " and tran.em_escm_iscustody_transfer ='Y'";

        } else {
          whereClause += " and tran.em_escm_iscustody_transfer ='N'";
        }
      }

      if (searchFlag.equals("true")) {
        if (vo.getOrgName() != null)
          whereClause += " and org.name ilike '%" + vo.getOrgName() + "%'";
        if (vo.getRequester() != null)
          whereClause += " and rr.name ilike '%" + vo.getRequester() + "%'";
        if (vo.getDocno() != null)
          if (windowId.equals("MIR") || windowId.equals("SIR")) {
            whereClause += " and req.documentno ilike '%" + vo.getDocno() + "%'";
          } else {
            whereClause += " and tran.documentno ilike '%" + vo.getDocno() + "%'";
          }
        if (vo.getNextrole() != null)
          whereClause += " and pen.pending ilike '%" + vo.getNextrole() + "%'";
        if (vo.getStatus() != null) {
          if (("in progress").contains(vo.getStatus().toLowerCase()) || vo.getStatus().isEmpty()) {
            if (windowId.equals("MIR") || windowId.equals("SIR")) {
              whereClause += " and req.status ='ESCM_IP'";
            } else {
              whereClause += " and tran.em_escm_docstatus ='ESCM_IP'";
            }
          } else {
            whereClause += " and 1=2";
          }
        }

      }
      if (sortColName != null && sortColName.equals("org"))
        orderClause += " order by org.name  " + sortColType + " limit " + limit + " offset "
            + offset;
      else if (sortColName != null && sortColName.equals("docno")) {
        if (windowId.equals("MIR") || windowId.equals("SIR")) {
          orderClause += " order by req.documentno " + sortColType + " limit " + limit + " offset "
              + offset;
        } else {
          orderClause += " order by tran.documentno " + sortColType + " limit " + limit + " offset "
              + offset;
        }

      } else if (sortColName != null && sortColName.equals("requester"))
        orderClause += " order by rr.name " + sortColType + " limit " + limit + " offset " + offset;
      else if (sortColName != null && sortColName.equals("nextrole"))
        orderClause += " order by pen.pending " + sortColType + " limit " + limit + " offset "
            + offset;
      else if (sortColName != null && sortColName.equals("status")) {
        if (windowId.equals("MIR") || windowId.equals("SIR")) {
          orderClause += " order by req.status " + sortColType + " limit " + limit + " offset "
              + offset;
        } else {
          orderClause += " order by tran.em_escm_docstatus " + sortColType + " limit " + limit
              + " offset " + offset;
        }
      } else {
        if (windowId.equals("MIR") || windowId.equals("SIR")) {
          orderClause += " order by req.documentno desc " + " limit " + limit + " offset " + offset;
        } else {
          orderClause += " order by tran.documentno desc " + " limit " + limit + " offset "
              + offset;
        }
      }

      sqlQuery = fromClause + whereClause + orderClause + ") main ";
      if (searchFlag.equals("true")) {
        if (vo.getLastperfomer() != null)
          sqlQuery += " where  main.lastaction ilike '%" + vo.getLastperfomer() + "%'";
      }
      if (sortColName != null && sortColName.equals("lastperformer"))
        sqlQuery += " order by main.lastaction  " + sortColType + " limit " + limit + " offset "
            + offset;

      st = conn.prepareStatement(sqlQuery);
      rs = st.executeQuery();
      while (rs.next()) {
        String nextRole = "";
        ApprovalRevokeVO apVO = new ApprovalRevokeVO();
        apVO.setRecordId(Utility.nullToEmpty(rs.getString("id")));
        apVO.setOrgName(Utility.nullToEmpty(rs.getString("org")));
        apVO.setRequester(Utility.nullToEmpty(rs.getString("requester")));
        apVO.setDocno(Utility.nullToEmpty(rs.getString("documentno")));
        apVO.setLastperfomer(Utility.nullToEmpty(rs.getString("lastaction")));

        EutNextRole objNxtRole = OBDal.getInstance().get(EutNextRole.class,
            Utility.nullToEmpty((windowId.equals("MIR") || windowId.equals("SIR"))
                ? rs.getString("eut_next_role_id")
                : rs.getString("em_eut_next_role_id")));
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
        if (Utility
            .nullToEmpty((windowId.equals("MIR") || windowId.equals("SIR") ? rs.getString("status")
                : rs.getString("em_escm_docstatus")))
            .equals("ESCM_IP")) {
          apVO.setStatus("In Progress");
        } else {
          apVO.setStatus("");
        }

        ls.add(apVO);
      }

    } catch (final SQLException e) {
      log4j.error("", e);
    } catch (final Exception e) {
      log4j.error("", e);
    } finally {
      OBContext.restorePreviousMode();
      try {
        if (st != null) {
          st.close();
        }
        if (rs != null) {
          rs.close();
        }
      } catch (final SQLException e) {
        log4j.error("", e);
      }
    }
    return ls;
  }

  /**
   * This method is used to update MIR revoke
   * 
   * @param var
   * @param selectIds
   * @param inpWindowId
   * @return
   */
  public String updateMIRRevoke(VariablesSecureApp var, String selectIds, String inpWindowId) {
    String Result = "Success";
    String alertWindow = "", alertRuleId = "", appstatus = "", Description = "";
    ArrayList<String> includeRecipient = new ArrayList<String>();
    int count = 0;
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();

    try {
      if (inpWindowId.equals("MIR")) {
        alertWindow = AlertWindow.IssueRequest;
      } else {
        alertWindow = AlertWindow.SiteIssueRequest;
      }
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + var.getClient() + "' and e.eSCMProcessType='" + alertWindow
              + "'");
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      List<String> result = Arrays.asList(selectIds.split("\\s*,\\s*"));
      for (int i = 0; i < result.size(); i++) {

        MaterialIssueRequest header = OBDal.getInstance().get(MaterialIssueRequest.class,
            result.get(i));

        header.setUpdated(new java.util.Date());
        header.setUpdatedBy(OBContext.getOBContext().getUser());
        header.setAlertStatus("DR");
        if (header.isSiteissuereq()) {
          header.setEscmSmirAction("CO");

        } else {
          header.setEscmAction("CO");
        }
        header.setEUTNextRole(null);
        OBDal.getInstance().save(header);

        if (!StringUtils.isEmpty(header.getId())) {
          appstatus = "REV";
          JSONObject historyData = new JSONObject();

          historyData.put("ClientId", var.getClient());
          historyData.put("OrgId", var.getOrg());
          historyData.put("RoleId", var.getRole());
          historyData.put("UserId", var.getUser());
          historyData.put("HeaderId", header.getId());
          historyData.put("Comments", "Mass Revoke");
          historyData.put("Status", appstatus);
          historyData.put("NextApprover", "");
          historyData.put("HistoryTable", ApprovalTables.ISSUE_REQUEST_HISTORY);
          historyData.put("HeaderColumn", ApprovalTables.ISSUE_REQUEST_HEADER_COLUMN);
          historyData.put("ActionColumn", ApprovalTables.ISSUE_REQUEST_DOCACTION_COLUMN);
          count = Utility.InsertApprovalHistory(historyData);
        }
        log4j.debug("headerId:" + header.getId());
        log4j.debug("count:" + count);

        if (count > 0 && !StringUtils.isEmpty(header.getId())) {
          Role objCreatedRole = null;
          if (header.getCreatedBy().getADUserRolesList().size() > 0) {
            objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
          }
          if (header.isSiteissuereq()) {
            alertWindow = AlertWindow.SiteIssueRequest;
          } else {
            alertWindow = AlertWindow.IssueRequest;
          }
          // remove approval alert
          OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
              "as e where e.referenceSearchKey='" + result.get(i) + "' and e.alertStatus='NEW'");
          if (alertQuery.list().size() > 0) {
            for (Alert objAlert : alertQuery.list()) {
              OBDal.getInstance().remove(objAlert);
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
          NextRoleByRuleVO nextApproval = NextRoleByRule.getRequesterNextRole(
              OBDal.getInstance().getConnection(), var.getClient(),
              header.getOrganization().getId(), header.getRole().getId(), var.getUser(),
              Resource.MATERIAL_ISSUE_REQUEST, header.getRole().getId());
          EutNextRole nextRole = null;
          nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
          // set alert for next approver
          if (header.isSiteissuereq()) {
            Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.smir.revoked",
                var.getLanguage()) + " " + header.getCreatedBy().getName();
          } else {
            Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.mir.revoked",
                var.getLanguage()) + " " + header.getCreatedBy().getName();
          }

          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(),
                objNextRoleLine.getRole().getId(), "", header.getClient().getId(), Description,
                "NEW", alertWindow, "scm.mir.revoked",
                sa.elm.ob.utility.util.Constants.GENERIC_TEMPLATE);
          }
          // Removing Forward and RMI Id
          if (header.getEUTForward() != null) {
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTForward());
            forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(header.getId(),
                Constants.MATERIAL_ISSUE_REQUEST);
          }
          if (header.getEUTReqmoreinfo() != null) {
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTReqmoreinfo());
            forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(header.getId(),
                Constants.MATERIAL_ISSUE_REQUEST);
          }
          Result = "Success";
          OBDal.getInstance().save(header);
          OBDal.getInstance().flush();
          OBDal.getInstance().commitAndClose();

        }
      }
      // delete the unused nextroles.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.MATERIAL_ISSUE_REQUEST);
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log4j.error("", e);
      Result = "Error";
    }
    return Result;
  }

  /**
   * This method is used to update INOUT revoke
   * 
   * @param var
   * @param selectIds
   * @param inpWindowId
   * @return
   */
  public String updateINOUTRevoke(VariablesSecureApp var, String selectIds, String inpWindowId) {
    String Result = "Success";
    String alertWindow = "", alertRuleId = "", appstatus = "", Description = "",
        lastWaitingRoleId = "";

    @SuppressWarnings("unused")
    NextRoleByRuleVO nextApproval = null;
    ArrayList<String> includeRecipient = new ArrayList<String>();
    int count = 0;
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    try {
      if (inpWindowId.equals("CT")) {
        alertWindow = AlertWindow.CustodyTransfer;
      } else {
        alertWindow = AlertWindow.ReturnTransaction;
      }
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + var.getClient() + "' and e.eSCMProcessType='" + alertWindow
              + "'");
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      List<String> result = Arrays.asList(selectIds.split("\\s*,\\s*"));
      for (int i = 0; i < result.size(); i++) {

        ShipmentInOut header = OBDal.getInstance().get(ShipmentInOut.class, result.get(i));

        header.setUpdated(new java.util.Date());
        header.setUpdatedBy(OBContext.getOBContext().getUser());
        if (inpWindowId.equals("CT")) {
          header.setEscmCtdocaction("CO");
          header.setEscmCtapplevel(Long.valueOf(1));
        }
        header.setEscmDocaction("CO");
        header.setEscmDocstatus("DR");
        OBDal.getInstance().save(header);

        if (!StringUtils.isEmpty(header.getId())) {
          appstatus = "REV";
          JSONObject historyData = new JSONObject();

          historyData.put("ClientId", var.getClient());
          historyData.put("OrgId", var.getOrg());
          historyData.put("RoleId", var.getRole());
          historyData.put("UserId", var.getUser());
          historyData.put("HeaderId", header.getId());
          historyData.put("Comments", "Mass Revoke");
          historyData.put("Status", appstatus);
          historyData.put("NextApprover", "");
          historyData.put("HistoryTable", ApprovalTables.CUSTODYTRANSFER_HISTORY);
          historyData.put("HeaderColumn", ApprovalTables.CUSTODYTRANSFER_HEADER_COLUMN);
          historyData.put("ActionColumn", ApprovalTables.CUSTODYTRANSFER_DOCACTION_COLUMN);
          count = Utility.InsertApprovalHistory(historyData);
        }
        log4j.debug("headerId:" + header.getId());
        log4j.debug("count:" + count);

        if (count > 0 && !StringUtils.isEmpty(header.getId())) {
          Role objCreatedRole = null;
          if (header.getCreatedBy().getADUserRolesList().size() > 0) {
            objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
          }
          alertWindow = AlertWindow.ReturnTransaction;
          // remove approval alert
          OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
              "as e where e.referenceSearchKey='" + result.get(i) + "' and e.alertStatus='NEW'");
          if (alertQuery.list().size() > 0) {
            for (Alert objAlert : alertQuery.list()) {
              objAlert.setAlertStatus("SOLVED");
              lastWaitingRoleId = objAlert.getRole().getId();
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

          if (inpWindowId.equals("CT")) {
            String NextUserId = null, strNextRoldId = null;
            // get Current ApproverUser Id for a record &&
            // !vars.getUser().equals(inout.getEscmCtreclinemng().getId())
            if (header.getEutNextRole() != null && header.getEscmCtapplevel() != 4) { // !vars.getUser().equals(inout.getEscmCtreclinemng().getId())
              OBQuery<EutNextRoleLine> line = OBDal.getInstance().createQuery(EutNextRoleLine.class,
                  " as line where line.eUTNextRole.id='" + header.getEutNextRole().getId() + "'");
              if (line.list().size() > 0) {
                NextUserId = line.list().get(0).getUserContact().getId();
                strNextRoldId = line.list().get(0).getRole().getId();
              }
            }
            nextApproval = NextRoleByRule.getCustTranNextRole(OBDal.getInstance().getConnection(),
                var.getClient(), header.getOrganization().getId(), header.getEscmAdRole().getId(),
                var.getUser(), Resource.CUSTODY_TRANSFER, header, NextUserId, "1",
                header.getEscmCtapplevel());

            User user = OBDal.getInstance().get(User.class, var.getUser());

            Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.ct.revoked",
                var.getLanguage()) + " " + user.getName();

            AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(), strNextRoldId,
                NextUserId, header.getClient().getId(), Description, "NEW", alertWindow,
                "scm.ct.revoked", Constants.GENERIC_TEMPLATE);
            // Removing forwardRMI id
            if (header.getEutForward() != null) {
              // Removing the Role Access given to the forwarded user
              // forwardReqMoreInfoDAO.removeRoleAccess(clientId, forwardId, conn);
              // update status as "DR"
              forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEutForward());
              // Removing Forward_Rmi id from transaction screens
              forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(header.getId(),
                  Constants.Custody_Transfer);

            }
            if (header.getEutReqmoreinfo() != null) {
              // access remove
              // forwardReqMoreInfoDAO.removeReqMoreInfoRoleAccess(clientId,
              // header.getEutReqmoreinfo().getId(), conn);

              // update status as "DR"
              forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEutReqmoreinfo());

              // Remove Forward_Rmi id from transaction screens
              forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(header.getId(),
                  Constants.Custody_Transfer);

            }

          } else {
            nextApproval = NextRoleByRule.getMIRRevokeRequesterNextRole(
                OBDal.getInstance().getConnection(), var.getClient(),
                header.getOrganization().getId(), header.getEscmAdRole().getId(), var.getUser(),
                Resource.Return_Transaction, header.getEscmAdRole().getId());

            User user = OBDal.getInstance().get(User.class, var.getUser());

            Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.rt.revoked",
                var.getLanguage()) + " " + user.getName();

            // set alert for next approver
            AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(),
                lastWaitingRoleId, "", header.getClient().getId(), Description, "NEW", alertWindow,
                "scm.rt.revoked", Constants.GENERIC_TEMPLATE);
          }
          header.setEutNextRole(null);
          Result = "Success";
          OBDal.getInstance().save(header);
          OBDal.getInstance().flush();
          OBDal.getInstance().commitAndClose();
        }
      }
      // delete the unused nextroles.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.CUSTODY_TRANSFER);
      // delete the unused nextroles.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.Return_Transaction);
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log4j.error("", e);
      Result = "Error";
    }
    return Result;
  }

  /**
   * This method is used to validate record
   * @param selectIds
   * @param inpWindowsId
   */
  public String validateRecord(String selectIds, String inpWindowId) {
    String ids = null;
    List<String> result = Arrays.asList(selectIds.split("\\s*,\\s*"));
    for (int i = 0; i < result.size(); i++) {
      if (inpWindowId.equals("MIR") || inpWindowId.equals("SIR")) {
        MaterialIssueRequest header = OBDal.getInstance().get(MaterialIssueRequest.class,
            result.get(i));
        if (header.getAlertStatus().equals("DR") || header.getAlertStatus().equals("ESCM_TR")) {
          if (ids == null) {
            ids = header.getDocumentNo();
          } else {
            ids = ids + ", " + header.getDocumentNo();
          }
        }
      } else {
        ShipmentInOut header = OBDal.getInstance().get(ShipmentInOut.class, result.get(i));
        if (header.getEscmDocstatus().equals("DR") || header.getEscmDocstatus().equals("ESCM_TR")) {
          if (ids == null) {
            ids = header.getDocumentNo();
          } else {
            ids = ids + ", " + header.getDocumentNo();
          }
        }
      }
    }
    return ids;
  }
/**
 * This method is used to update record
 * @param var
 * @param selectIds
 * @param inpWindowsId
 */
  @Override
  public String updateRecord(VariablesSecureApp var, String selectIds, String inpWindowId) {
    String success = "";
    if (inpWindowId.equals("MIR") || inpWindowId.equals("SIR")) {
      success = updateMIRRevoke(var, selectIds, inpWindowId);
    } else {
      success = updateINOUTRevoke(var, selectIds, inpWindowId);
    }
    return success;
  }

}
