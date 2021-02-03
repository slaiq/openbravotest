package sa.elm.ob.finance.ad_forms.ApprovalRevoke.dao;

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

import sa.elm.ob.finance.BudgetAdjustment;
import sa.elm.ob.finance.BudgetAdjustmentLine;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.ad_process.FundsRequest.FundsRequestActionDAO;
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
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Gowtham
 *
 */
public class AdjustmentMassRevokeDAO extends MassRevoke {
  private static final String REVOKE = "REV";
  private static Logger LOG = Logger.getLogger(AdjustmentMassRevokeDAO.class);

  String fromClause = "select main.id, main.org, main.requester,main.documentno,main.lastaction,main.eut_next_role_id,main.DocStatus from "
      + "(select org.name as org,adj.docno as documentno,adj.DocStatus,adj.Efin_Budgetadj_id as id,rr.name as requester, "
      + " (select (coalesce(trl.name,st.name)||' - '|| ur.name) from Efin_Budgetadj_Hist history, ad_user ur, ad_ref_list st "
      + " left join ad_ref_list_trl trl on trl.ad_ref_list_id=st.ad_ref_list_id and trl.ad_language=(select lang.ad_language "
      + " from ad_language lang  where lang.ad_language_id='112') where history.efin_budgetadj_id = adj.efin_budgetadj_id  and st.value = history.requestreqaction "
      + " and st.ad_reference_id='9F2DC8F55FE9442895FCD3ED468B1D50' and ur.ad_user_id = history.createdby order by history.updated desc limit 1) as lastaction,  "
      + " pen.pending, adj.eut_next_role_id from Efin_Budgetadj adj "
      + " left join ad_user rr on rr.ad_user_id=adj.createdby "
      + " left join ad_org org on org.ad_org_id=adj.ad_org_id "
      + " left join (select array_to_string(array_agg(role.name),' / ') as pending, lin.eut_next_role_id  from eut_next_role rl "
      + " join eut_next_role_line lin on lin.eut_next_role_id=rl.eut_next_role_id "
      + " join ad_role role on role.ad_role_id=lin.ad_role_id  group by lin.eut_next_role_id ) as pen on pen.eut_next_role_id=adj.eut_next_role_id ";

  @SuppressWarnings("unchecked")
  public int getRevokeRecordsCount(VariablesSecureApp vars, String clientId, String windowId,
      String searchFlag, ApprovalRevokeVO vo) {
    int totalRecord = 0;
    String sqlQuery = "", whereClause = "";

    try {

      whereClause = " where adj.ad_client_id = '" + clientId + "'";

      whereClause += " and adj.DocStatus='EFIN_IP' and adj.eut_next_role_id is not null "
          + " and adj.ad_org_id  in (  select org.ad_org_id from ad_user rr  "
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
          whereClause += " and adj.docno ilike '%" + vo.getDocno() + "%'";
        if (vo.getNextrole() != null)
          whereClause += " and pen.pending ilike '%" + vo.getNextrole() + "%'";
        if (vo.getStatus() != null) {
          if (("in progress").contains(vo.getStatus().toLowerCase()) || vo.getStatus().isEmpty()) {
            whereClause += " and adj.DocStatus='EFIN_IP' ";
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
        LOG.debug("Exception While Getting Records of adjustment for mass revoke", e);
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

      whereClause = " where adj.ad_client_id = '" + clientId + "'";

      whereClause += " and adj.DocStatus='EFIN_IP' and adj.eut_next_role_id is not null "
          + " and adj.ad_org_id  in (  select org.ad_org_id from ad_user rr  "
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
          whereClause += " and adj.docno ilike '%" + vo.getDocno() + "%'";
        if (vo.getNextrole() != null)
          whereClause += " and pen.pending ilike '%" + vo.getNextrole() + "%'";
        if (vo.getStatus() != null) {
          if (("in progress").contains(vo.getStatus().toLowerCase()) || vo.getStatus().isEmpty()) {
            whereClause += " and adj.DocStatus='EFIN_IP' ";
          } else {
            whereClause += " and 1=2";
          }
        }
      }

      if (sortColName != null && sortColName.equals("org"))
        orderClause += " order by org.name  " + sortColType + " limit " + limit + " offset "
            + offset;
      else if (sortColName != null && sortColName.equals("docno")) {
        orderClause += " order by adj.docno " + sortColType + " limit " + limit + " offset "
            + offset;
      } else if (sortColName != null && sortColName.equals("requester"))
        orderClause += " order by rr.name " + sortColType + " limit " + limit + " offset " + offset;
      else if (sortColName != null && sortColName.equals("nextrole"))
        orderClause += " order by pen.pending " + sortColType + " limit " + limit + " offset "
            + offset;
      else if (sortColName != null && sortColName.equals("status")) {
        orderClause += " order by adj.DocStatus='EFIN_IP' " + sortColType + " limit " + limit
            + " offset " + offset;
      } else {
        orderClause += " order by adj.docno desc" + " limit " + limit + " offset " + offset;
      }

      sqlQuery = fromClause + whereClause + orderClause + ") main ";
      if (searchFlag.equals("true")) {
        if (vo.getLastperfomer() != null)
          sqlQuery += " where  main.lastaction ilike '%" + vo.getLastperfomer() + "%'";
      }
      if (sortColName != null && sortColName.equals("lastperformer"))
        sqlQuery += " order by main.lastaction  " + sortColType + " limit " + limit + " offset "
            + offset;
      LOG.debug("Adjustment mass revoke sqlQuery : " + sqlQuery);

      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
      List queryList = query.list();
      if (queryList != null && queryList.size() > 0) {
        for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();

          // while (rs.next()) {
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
          if (Utility.nullToEmpty(row[6].toString()).equals("EFIN_IP")) {
            apVO.setStatus(Resource.getProperty("utility.inprogress", lang));
          } else {
            apVO.setStatus("");
          }

          ls.add(apVO);
        }
      }

    } catch (final Exception e) {
      if (LOG.isDebugEnabled())
        LOG.debug("Exception While Getting Records of adjustment for mass revoke", e);
    }
    return ls;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public String updateRecord(VariablesSecureApp var, String selectIds, String inpWindowId) {

    String Result = "Success";
    String alertWindow = "", alertRuleId = "", comments = "", Description = "";
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    ArrayList<String> includeRecipient = new ArrayList<String>();
    int count = 0;
    JSONObject historyData = new JSONObject();
    EutNextRole nextRole = null;
    EfinBudgetManencum manualId = null;
    String fundsreqId = "";
    Boolean isfundserrorFlag = true;
    try {
      List<String> result = Arrays.asList(selectIds.split("\\s*,\\s*"));
      for (int i = 0; i < result.size(); i++) {
        BudgetAdjustment header = OBDal.getInstance().get(BudgetAdjustment.class, result.get(i));
        nextRole = OBDal.getInstance().get(EutNextRole.class, header.getNextRole().getId());

        // get funds reqId
        fundsreqId = FundsRequestActionDAO.getFundsReqId(header, null, null);
        log.debug("fundsreqId" + fundsreqId);
        if (StringUtils.isNotEmpty(fundsreqId)) {
          isfundserrorFlag = FundsRequestActionDAO.reactivateBudgetInqchanges(
              OBDal.getInstance().getConnection(), fundsreqId, false, true);
          log.debug("isfundserrorFlag" + isfundserrorFlag);
          if (!isfundserrorFlag) {
            Result = "Error";
          }
        }
        if (!StringUtils.equals(Result, "Error")) {
          // update header status
          header.setUpdated(new java.util.Date());
          header.setUpdatedBy(OBContext.getOBContext().getUser());
          header.setAction("CO");
          header.setDocumentStatus("DR");
          header.setNextRole(null);
          header.setProcessed(false);
          OBDal.getInstance().save(header);

          // if invoice crossed reserved role then need to revert the encumbrance also.
          String str_budget_reference = header.getEfinBudgetint() == null ? ""
              : header.getEfinBudgetint().getId();
          String query = " select bt.efin_budgetint_id,bl.efin_budgetinquiry_id ,"
              + " cv.account_id,al.increase,al.decrease,  "
              + " cv.c_validcombination_id,al.efin_budgetadjline_id  "
              + " from efin_budgetadjline al "
              + "  left join c_validcombination cv on cv.c_validcombination_id = al.c_validcombination_id "
              + "    left join efin_budgetint bt on bt.efin_budgetint_id=:reference1  "
              + "    left join efin_budgetinquiry bl on  cv.c_validcombination_id =bl.c_validcombination_id and "
              + "    bl.efin_budgetint_id=:reference2 " + "    where al.ad_client_id =:clientId "
              + "    and al.efin_budgetadj_id =:budgetAdjId and al.fundsreserved='Y'";
          SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
          sqlQuery.setParameter("reference1", str_budget_reference);
          sqlQuery.setParameter("reference2", str_budget_reference);
          sqlQuery.setParameter("clientId", header.getClient().getId());
          sqlQuery.setParameter("budgetAdjId", header.getId());
          List queryList = sqlQuery.list();
          if (sqlQuery != null && queryList.size() > 0) {
            for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
              Object[] row = (Object[]) iterator.next();
              if (row[0] != null) {
                if (row[1] != null) {
                  BudgetAdjustmentLine objAdjLine = OBDal.getInstance()
                      .get(BudgetAdjustmentLine.class, row[6].toString());
                  EfinBudgetInquiry objInquiryLine = OBDal.getInstance()
                      .get(EfinBudgetInquiry.class, row[1].toString());
                  // objInquiryLine
                  // .setEncumbrance(objInquiryLine.getEncumbrance().subtract((BigDecimal) row[4]));
                  // objInquiryLine
                  // .setObincAmt(objInquiryLine.getObincAmt().subtract((BigDecimal) row[3]));
                  OBDal.getInstance().save(objInquiryLine);
                  objAdjLine.setBudgetInquiryLine(objInquiryLine);
                  objAdjLine.setFundsreserved(false);
                  OBDal.getInstance().save(objAdjLine);

                }
              }
            }
          }
          OBQuery<EfinBudgetManencum> chkLinePresent = OBDal.getInstance().createQuery(
              EfinBudgetManencum.class, " as e where e.sourceref = '" + header.getId() + "'");
          List<EfinBudgetManencum> checkLinePresentList = chkLinePresent.list();

          if (chkLinePresent != null && checkLinePresentList.size() > 0) {
            manualId = checkLinePresentList.get(0);
            EfinBudgetManencum manual = manualId;
            manual.setDocumentStatus("DR");
            OBDal.getInstance().save(manual);
            for (EfinBudgetManencumlines reqln : manualId.getEfinBudgetManencumlinesList()) {
              OBDal.getInstance().remove(reqln);
            }
            OBDal.getInstance().remove(manualId);
            header.setManualEncumbrance(null);
          }

          // Insert approval history
          if (!StringUtils.isEmpty(header.getId())) {
            comments = Resource.getProperty("utility.massrevoke", var.getLanguage());
            historyData = getHistoryData(header, comments);
            if (historyData != null) {
              count = Utility.InsertApprovalHistory(historyData);
            }
          }

          // alert process
          if (count > 0 && !StringUtils.isEmpty(header.getId())) {
            Role objCreatedRole = null;
            if (header.getCreatedBy().getADUserRolesList().size() > 0) {
              objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
            }
            alertWindow = AlertWindow.BudgetAdjustment;

            // get alert rule for Adjustment
            OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
                "as e where e.client.id='" + header.getClient().getId()
                    + "' and e.efinProcesstype='" + alertWindow + "'");
            List<AlertRule> ruleList = queryAlertRule.list();
            if (ruleList.size() > 0) {
              AlertRule objRule = ruleList.get(0);
              alertRuleId = objRule.getId();
            }

            // remove approval alert
            OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
                "as e where e.referenceSearchKey='" + result.get(i) + "' and e.alertStatus='NEW'");
            List<Alert> alertList = alertQuery.list();
            if (alertList.size() > 0) {
              for (Alert objAlert : alertList) {
                objAlert.setAlertStatus("SOLVED");
                OBDal.getInstance().save(objAlert);
              }
            }
            // check and insert alert recipient
            OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance().createQuery(
                AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");
            List<AlertRecipient> alertRecipientList = receipientQuery.list();
            if (alertRecipientList.size() > 0) {
              for (AlertRecipient objAlertReceipient : alertRecipientList) {
                includeRecipient.add(objAlertReceipient.getRole().getId());
                OBDal.getInstance().remove(objAlertReceipient);
              }
            }
            includeRecipient.add(objCreatedRole != null ? objCreatedRole.getId() : null);
            // avoid duplicate recipient
            HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
            Iterator<String> iterator = incluedSet.iterator();
            while (iterator.hasNext()) {
              AlertUtility.insertAlertRecipient(iterator.next(), null, var.getClient(),
                  alertWindow);
            }

            // set alert for next approver
            Description = Resource.getProperty("utility.adjustment.revoked", var.getLanguage())
                + " " + header.getCreatedBy().getName();
            for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
              AlertUtility.alertInsertionRole(header.getId(), header.getDocno(),
                  objNextRoleLine.getRole().getId(), "", header.getClient().getId(), Description,
                  "NEW", alertWindow, "utility.adjustment.revoked", Constants.GENERIC_TEMPLATE);
            }

            // delete records from next role table
            DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                Resource.BUDGET_ADJUSTMENT_RULE);

            if (header.getEUTForward() != null) {
              // Removing the Role Access given to the forwarded user
              // update status as "DR"
              forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTForward());
              // Removing Forward_Rmi id from transaction screens
              forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(header.getId(),
                  Constants.BUDGETADJUSTMENT);

            }
            if (header.getEUTReqmoreinfo() != null) {
              // access remove
              // update status as "DR"
              forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTReqmoreinfo());
              // Remove Forward_Rmi id from transaction screens
              forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(header.getId(),
                  Constants.BUDGETADJUSTMENT);

            }

            Result = "Success";
            OBDal.getInstance().save(header);
            OBDal.getInstance().flush();
          }
        } else {
          OBDal.getInstance().rollbackAndClose();
          return Result;
        }
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      if (LOG.isDebugEnabled()) {
        LOG.error("Exception While Revoke Budget Adjustment :", e);
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
      BudgetAdjustment adj = OBDal.getInstance().get(BudgetAdjustment.class, result.get(i));
      if (adj.getDocumentStatus().equals("DR") || adj.getDocumentStatus().equals("ESCM_TR")) {
        if (ids == null) {
          ids = adj.getDocno();
        } else {
          ids = ids + ", " + adj.getDocno();
        }
      }
    }
    return ids;
  }

  /**
   * To get json object to insert approval history.
   * 
   * @param adj
   * @param comments
   * @return jsonobject
   */
  public static JSONObject getHistoryData(BudgetAdjustment adj, String comments) {
    JSONObject historyData = new JSONObject();

    try {

      historyData.put("ClientId", adj.getClient().getId());
      historyData.put("OrgId", adj.getOrganization().getId());
      historyData.put("RoleId", OBContext.getOBContext().getRole().getId());
      historyData.put("UserId", OBContext.getOBContext().getUser().getId());
      historyData.put("HeaderId", adj.getId());
      historyData.put("Comments", comments);
      historyData.put("Status", REVOKE);
      historyData.put("NextApprover", "");
      historyData.put("HistoryTable", ApprovalTables.Budget_Adjustment_HISTORY);
      historyData.put("HeaderColumn", ApprovalTables.Budget_Adjustment_HEADER_COLUMN);
      historyData.put("ActionColumn", ApprovalTables.Budget_Adjustment_DOCACTION_COLUMN);

    } catch (Exception e) {
      if (LOG.isDebugEnabled())
        LOG.error("Exception while getHistoryData: " + e);
    }
    return historyData;
  }

}
