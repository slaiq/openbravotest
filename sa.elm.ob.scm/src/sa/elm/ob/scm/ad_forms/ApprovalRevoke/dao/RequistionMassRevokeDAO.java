package sa.elm.ob.scm.ad_forms.ApprovalRevoke.dao;

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
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.procurement.Requisition;

import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.scm.ad_process.Requisition.RequisitionDao;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.ApprovalRevoke.massrevoke.ApprovalRevokeVO;
import sa.elm.ob.utility.ad_forms.ApprovalRevoke.massrevoke.MassRevoke;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Sathishkumar on 13/11/2017
 */

public class RequistionMassRevokeDAO extends MassRevoke {
  private Connection conn = null;
  private static Logger LOG = Logger.getLogger(RequistionMassRevokeDAO.class);

  public RequistionMassRevokeDAO(Connection con) {
    this.conn = con;
  }

  public int getRevokeRecordsCount(VariablesSecureApp vars, String clientId, String windowId,
      String searchFlag, ApprovalRevokeVO vo) {
    PreparedStatement st = null;
    ResultSet rs = null;
    int totalRecord = 0;
    String sqlQuery = "", fromClause = "", whereClause = "";

    try {
      fromClause = "select count(*) as totalRecord from (select org.name as org ,req.documentno,req.em_escm_doc_status,req.m_requisition_id as id ,rr.name as requester, "
          + "  (select (coalesce(trl.name,st.name)||' - '|| ur.name) "
          + "  from escm_purchasereq_app_hist  history,ad_user ur,ad_ref_list st  "
          + "  left join ad_ref_list_trl trl on trl.ad_ref_list_id=st.ad_ref_list_id and  "
          + "  trl.ad_language=(select lang.ad_language from ad_language lang where lang.ad_language_id='112') "
          + "  where history.m_requisition_id  = req.m_requisition_id "
          + "  and st.value  =history.purchasereqaction and st.ad_reference_id='9F2DC8F55FE9442895FCD3ED468B1D50'  "
          + "  and ur.ad_user_id = history.createdby order by history.updated desc limit 1) as lastaction,pen.pending,req.em_eut_next_role_id  "
          + "  from m_requisition req " + "  left join ad_user rr on rr.ad_user_id=req.createdby "
          + "  left join ad_org org on org.ad_org_id=req.ad_org_id "
          + "  left join (select array_to_string(array_agg(role.name),' / ') as pending,lin.eut_next_role_id from eut_next_role rl "
          + "  join eut_next_role_line lin on lin.eut_next_role_id=rl.eut_next_role_id "
          + "  join ad_role role on role.ad_role_id=lin.ad_role_id "
          + "  group by lin.eut_next_role_id ) as pen on pen.eut_next_role_id=req.em_eut_next_role_id ";

      whereClause = " where req.ad_client_id = '" + clientId + "'";
      whereClause += " and req.em_escm_doc_status='ESCM_IP' and req.em_eut_next_role_id is not null and req.ad_org_id  in (  select org.ad_org_id from ad_user rr  "
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
          whereClause += " and req.documentno ilike '%" + vo.getDocno() + "%'";
        if (vo.getNextrole() != null)
          whereClause += " and pen.pending ilike '%" + vo.getNextrole() + "%'";
        if (vo.getStatus() != null) {
          if (("in progress").contains(vo.getStatus().toLowerCase()) || vo.getStatus().isEmpty()) {
            whereClause += " and req.em_escm_docstatus ='ESCM_IP'";
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
      if (LOG.isDebugEnabled())
        LOG.debug("Exception While Getting Records of requistion for mass revoke", e);
    } catch (final Exception e) {
      if (LOG.isDebugEnabled())
        LOG.debug("Exception While Getting Records of requistion for mass revoke", e);
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (st != null) {
          st.close();
        }
      } catch (final SQLException e) {
        if (LOG.isDebugEnabled())
          LOG.debug("Exception While Getting Records of requistion for mass revoke", e);
      }
    }
    return totalRecord;
  }

  public List<ApprovalRevokeVO> getRevokeRecordsList(VariablesSecureApp vars, String clientId,
      String windowId, ApprovalRevokeVO vo, int limit, int offset, String sortColName,
      String sortColType, String searchFlag, String lang) {
    LOG.debug("sort" + sortColType);
    PreparedStatement st = null;
    ResultSet rs = null;
    List<ApprovalRevokeVO> ls = new ArrayList<ApprovalRevokeVO>();

    String sqlQuery = "", fromClause = "", whereClause = "", orderClause = "";
    OBContext.setAdminMode();

    try {

      fromClause = "select * from (select org.name as org ,req.documentno,req.em_escm_doc_status,req.m_requisition_id as id ,rr.name as requester, "
          + "  (select (coalesce(trl.name,st.name)||' - '|| ur.name) "
          + "  from escm_purchasereq_app_hist  history,ad_user ur,ad_ref_list st  "
          + "  left join ad_ref_list_trl trl on trl.ad_ref_list_id=st.ad_ref_list_id and  "
          + "  trl.ad_language=(select lang.ad_language from ad_language lang where lang.ad_language_id='112') "
          + "  where history.m_requisition_id  = req.m_requisition_id "
          + "  and st.value  =history.purchasereqaction and st.ad_reference_id='9F2DC8F55FE9442895FCD3ED468B1D50'  "
          + "  and ur.ad_user_id = history.createdby order by history.updated desc limit 1) as lastaction,pen.pending,req.em_eut_next_role_id  "
          + "  from m_requisition req " + "  left join ad_user rr on rr.ad_user_id=req.createdby "
          + "  left join ad_org org on org.ad_org_id=req.ad_org_id "
          + "  left join (select array_to_string(array_agg(role.name),' / ') as pending,lin.eut_next_role_id from eut_next_role rl "
          + "  join eut_next_role_line lin on lin.eut_next_role_id=rl.eut_next_role_id "
          + "  join ad_role role on role.ad_role_id=lin.ad_role_id "
          + "  group by lin.eut_next_role_id ) as pen on pen.eut_next_role_id=req.em_eut_next_role_id ";

      whereClause = " where req.ad_client_id = '" + clientId + "'";
      whereClause += " and req.em_escm_doc_status='ESCM_IP' and req.em_eut_next_role_id is not null and req.ad_org_id  in (  select org.ad_org_id from ad_user rr  "
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
          whereClause += " and req.documentno ilike '%" + vo.getDocno() + "%'";
        if (vo.getNextrole() != null)
          whereClause += " and pen.pending ilike '%" + vo.getNextrole() + "%'";
        if (vo.getStatus() != null) {
          if (("in progress").contains(vo.getStatus().toLowerCase()) || vo.getStatus().isEmpty()) {
            whereClause += " and req.em_escm_docstatus ='ESCM_IP'";
          } else {
            whereClause += " and 1=2";
          }
        }

      }
      if (sortColName != null && sortColName.equals("org"))
        orderClause += " order by org.name  " + sortColType + " limit " + limit + " offset "
            + offset;
      else if (sortColName != null && sortColName.equals("docno")) {
        orderClause += " order by req.documentno " + sortColType + " limit " + limit + " offset "
            + offset;
      } else if (sortColName != null && sortColName.equals("requester"))
        orderClause += " order by rr.name " + sortColType + " limit " + limit + " offset " + offset;
      else if (sortColName != null && sortColName.equals("nextrole"))
        orderClause += " order by pen.pending " + sortColType + " limit " + limit + " offset "
            + offset;
      else if (sortColName != null && sortColName.equals("status")) {
        orderClause += " order by req.em_escm_docstatus " + sortColType + " limit " + limit
            + " offset " + offset;
      } else {
        orderClause += " order by req.documentno desc " + " limit " + limit + " offset " + offset;
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
            Utility.nullToEmpty(rs.getString("em_eut_next_role_id")));
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
        if (Utility.nullToEmpty(rs.getString("em_escm_doc_status")).equals("ESCM_IP")) {
          apVO.setStatus("In Progress");
        } else {
          apVO.setStatus("");
        }

        ls.add(apVO);
      }

    } catch (final SQLException e) {
      if (LOG.isDebugEnabled())
        LOG.debug("Exception While Getting Records of requistion for mass revoke", e);
    } catch (final Exception e) {
      if (LOG.isDebugEnabled())
        LOG.debug("Exception While Getting Records of requistion for mass revoke", e);
    } finally {
      OBContext.restorePreviousMode();
      try {
        // close connection
        if (rs != null) {
          rs.close();
        }
        if (st != null) {
          st.close();
        }
      } catch (final SQLException e) {
        if (LOG.isDebugEnabled())
          LOG.debug("Exception While Getting Records of requistion for mass revoke", e);
      }
    }
    return ls;
  }

  @Override
  public String updateRecord(VariablesSecureApp var, String selectIds, String inpWindowId) {

    String Result = "Success";
    String alertWindow = "", alertRuleId = "", appstatus = "";
    ArrayList<String> includeRecipient = new ArrayList<String>();
    List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
    int count = 0;
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();

    try {
      List<String> result = Arrays.asList(selectIds.split("\\s*,\\s*"));
      for (int i = 0; i < result.size(); i++) {
        Requisition header = OBDal.getInstance().get(Requisition.class, result.get(i));

        // check pr encumbrance type is enable or not .. Task No.5925
        OBQuery<EfinEncControl> encumcontrol = OBDal.getInstance().createQuery(EfinEncControl.class,
            " as e where e.encumbranceType='PRE' and e.client.id='" + var.getClient()
                + "' and e.active='Y' ");
        encumcontrol.setFilterOnActive(true);
        encumcontrol.setMaxResult(1);
        enccontrollist = encumcontrol.list();

        // Task No.5925
        if (enccontrollist.size() > 0) {
          if (header.isEfinEncumbered()) {
            RequisitionDao.revertencumimpact(header);
          }
        }

        header.setUpdated(new java.util.Date());
        header.setUpdatedBy(OBContext.getOBContext().getUser());
        header.setEscmDocaction("CO");
        header.setEscmDocStatus("DR");
        header.setEutNextRole(null);
        OBDal.getInstance().save(header);
        OBDal.getInstance().flush();
        DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
            Resource.PURCHASE_REQUISITION);
        DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
            Resource.PURCHASE_REQUISITION_LIMITED);

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
          historyData.put("HistoryTable", ApprovalTables.REQUISITION_HISTORY);
          historyData.put("HeaderColumn", ApprovalTables.REQUISITION_HEADER_COLUMN);
          historyData.put("ActionColumn", ApprovalTables.REQUISITION_DOCACTION_COLUMN);
          count = Utility.InsertApprovalHistory(historyData);
        }
        if (LOG.isDebugEnabled()) {
          LOG.debug("headerId:" + header.getId());
          LOG.debug("count:" + count);
        }

        if (count > 0 && !StringUtils.isEmpty(header.getId())) {
          Role objCreatedRole = null;
          if (header.getCreatedBy().getADUserRolesList().size() > 0) {
            objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
          }
          alertWindow = AlertWindow.PurchaseRequisition;

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
          // delete the unused nextroles in eut_next_role table.
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.PURCHASE_REQUISITION);
          // delete the unused nextroles in eut_next_role table.
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.PURCHASE_REQUISITION_LIMITED);

          // Removing forwardRMI id
          if (header.getEutForward() != null) {
            // Removing the Role Access given to the forwarded user
            // forwardReqMoreInfoDAO.removeRoleAccess(clientId, forwardId, conn);
            // update status as "DR"
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEutForward());
            // Removing Forward_Rmi id from transaction screens
            forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(header.getId(),
                Constants.PURCHASE_REQUISITION);

          }
          if (header.getEutReqmoreinfo() != null) {
            // access remove
            // forwardReqMoreInfoDAO.removeReqMoreInfoRoleAccess(clientId,
            // header.getEutReqmoreinfo().getId(), conn);

            // update status as "DR"
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEutReqmoreinfo());

            // Remove Forward_Rmi id from transaction screens
            forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(header.getId(),
                Constants.PURCHASE_REQUISITION);

          }
          Result = "Success";
          OBDal.getInstance().save(header);
          OBDal.getInstance().flush();
          OBDal.getInstance().commitAndClose();
        }
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      if (LOG.isDebugEnabled()) {
        LOG.error("Exception While Revoke Purchase requistion Revoke :", e);
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
      Requisition header = OBDal.getInstance().get(Requisition.class, result.get(i));
      if (header.getEscmDocStatus().equals("DR") || header.getEscmDocStatus().equals("ESCM_TR")) {
        if (ids == null) {
          ids = header.getDocumentNo();
        } else {
          ids = ids + ", " + header.getDocumentNo();
        }
      }
    }
    return ids;
  }
}
