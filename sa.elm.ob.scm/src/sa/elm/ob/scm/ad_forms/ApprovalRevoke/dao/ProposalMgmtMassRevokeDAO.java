package sa.elm.ob.scm.ad_forms.ApprovalRevoke.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.ad_process.BidManagement.dao.BidManagementDAO;
import sa.elm.ob.scm.ad_process.ProposalManagement.ProposalManagementActionMethod;
import sa.elm.ob.scm.ad_process.ProposalManagement.ProposalManagementRejectMethods;
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

public class ProposalMgmtMassRevokeDAO extends MassRevoke {
  private Connection conn = null;
  private static Logger LOG = Logger.getLogger(ProposalMgmtMassRevokeDAO.class);

  public ProposalMgmtMassRevokeDAO(Connection con) {
    this.conn = con;
  }

  @Override
  public int getRevokeRecordsCount(VariablesSecureApp vars, String clientId, String windowId,
      String searchFlag, ApprovalRevokeVO vo) {
    PreparedStatement st = null;
    ResultSet rs = null;
    int totalRecord = 0;
    String sqlQuery = "", fromClause = "", whereClause = "";

    try {
      fromClause = "  select count(*) as totalRecord from "
          + " (select org.name as org, prop.proposalno as documentno, prop.proposalappstatus, prop.escm_proposalmgmt_id as id, rr.name as requester, "
          + " (select (coalesce(trl.name,st.name)||' - '|| ur.name) from escm_proposalmgmt_hist history, ad_user ur, ad_ref_list st  "
          + " left join ad_ref_list_trl trl on trl.ad_ref_list_id=st.ad_ref_list_id and trl.ad_language=(select lang.ad_language from ad_language lang "
          + " where lang.ad_language_id='112')  where history.escm_proposalmgmt_id = prop.escm_proposalmgmt_id  and st.value = history.requestreqaction "
          + " and st.ad_reference_id='9F2DC8F55FE9442895FCD3ED468B1D50' and ur.ad_user_id = history.createdby order by history.updated desc limit 1) as lastaction, "
          + " pen.pending, prop.eut_next_role_id   " + " from escm_proposalmgmt prop   "
          + " left join ad_user rr on rr.ad_user_id=prop.createdby "
          + " left join ad_org org on org.ad_org_id=prop.ad_org_id  "
          + " left join (select array_to_string(array_agg(role.name),' / ') as pending, lin.eut_next_role_id  from eut_next_role rl "
          + " join eut_next_role_line lin on lin.eut_next_role_id=rl.eut_next_role_id  "
          + " join ad_role role on role.ad_role_id=lin.ad_role_id  group by lin.eut_next_role_id ) as pen on pen.eut_next_role_id=prop.eut_next_role_id ";

      whereClause = " where prop.ad_client_id = '" + clientId + "'";

      whereClause += " and prop.proposalappstatus='INP' and prop.eut_next_role_id is not null "
          + " and prop.ad_org_id  in (  select org.ad_org_id from ad_user rr  "
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
          whereClause += " and prop.bidno ilike '%" + vo.getDocno() + "%'";
        if (vo.getNextrole() != null)
          whereClause += " and pen.pending ilike '%" + vo.getNextrole() + "%'";
        if (vo.getStatus() != null) {
          if (("in progress").contains(vo.getStatus().toLowerCase()) || vo.getStatus().isEmpty()) {
            whereClause += " and prop.proposalappstatus ='INP'";
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
        LOG.debug("Exception While Getting Records of proposal for mass revoke", e);
    } catch (final Exception e) {
      if (LOG.isDebugEnabled())
        LOG.debug("Exception While Getting Records of proposal for mass revoke", e);
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
          LOG.debug("Exception While Getting Records of proposal for mass revoke", e);
      }
    }
    return totalRecord;
  }

  @Override
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

      fromClause = "  select * from "
          + " (select org.name as org, prop.proposalno as documentno, prop.proposalappstatus, prop.escm_proposalmgmt_id as id, rr.name as requester, "
          + " (select (coalesce(trl.name,st.name)||' - '|| ur.name) from escm_proposalmgmt_hist history, ad_user ur, ad_ref_list st  "
          + " left join ad_ref_list_trl trl on trl.ad_ref_list_id=st.ad_ref_list_id and trl.ad_language=(select lang.ad_language from ad_language lang "
          + " where lang.ad_language_id='112')  where history.escm_proposalmgmt_id = prop.escm_proposalmgmt_id  and st.value = history.requestreqaction "
          + " and st.ad_reference_id='9F2DC8F55FE9442895FCD3ED468B1D50' and ur.ad_user_id = history.createdby order by history.updated desc limit 1) as lastaction, "
          + " pen.pending, prop.eut_next_role_id   " + " from escm_proposalmgmt prop   "
          + " left join ad_user rr on rr.ad_user_id=prop.createdby "
          + " left join ad_org org on org.ad_org_id=prop.ad_org_id  "
          + " left join (select array_to_string(array_agg(role.name),' / ') as pending, lin.eut_next_role_id  from eut_next_role rl "
          + " join eut_next_role_line lin on lin.eut_next_role_id=rl.eut_next_role_id  "
          + " join ad_role role on role.ad_role_id=lin.ad_role_id  group by lin.eut_next_role_id ) as pen on pen.eut_next_role_id=prop.eut_next_role_id ";

      whereClause = " where prop.ad_client_id = '" + clientId + "'";

      whereClause += " and prop.proposalappstatus='INP' and prop.eut_next_role_id is not null "
          + " and prop.ad_org_id  in (  select org.ad_org_id from ad_user rr  "
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
          whereClause += " and prop.proposalno ilike '%" + vo.getDocno() + "%'";
        if (vo.getNextrole() != null)
          whereClause += " and pen.pending ilike '%" + vo.getNextrole() + "%'";
        if (vo.getStatus() != null) {
          if (("in progress").contains(vo.getStatus().toLowerCase()) || vo.getStatus().isEmpty()) {
            whereClause += " and prop.proposalappstatus='INP' ";
          } else {
            whereClause += " and 1=2";
          }
        }

      }
      if (sortColName != null && sortColName.equals("org"))
        orderClause += " order by org.name  " + sortColType + " limit " + limit + " offset "
            + offset;
      else if (sortColName != null && sortColName.equals("docno")) {
        orderClause += " order by prop.proposalno " + sortColType + " limit " + limit + " offset "
            + offset;
      } else if (sortColName != null && sortColName.equals("requester"))
        orderClause += " order by rr.name " + sortColType + " limit " + limit + " offset " + offset;
      else if (sortColName != null && sortColName.equals("nextrole"))
        orderClause += " order by pen.pending " + sortColType + " limit " + limit + " offset "
            + offset;
      else if (sortColName != null && sortColName.equals("status")) {
        orderClause += " order by prop.proposalappstatus='INP' " + sortColType + " limit " + limit
            + " offset " + offset;
      } else {
        orderClause += " order by prop.proposalno desc" + " limit " + limit + " offset " + offset;
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
            Utility.nullToEmpty(rs.getString("eut_next_role_id")));
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
        if (Utility.nullToEmpty(rs.getString("proposalappstatus")).equals("INP")) {
          apVO.setStatus("In Progress");
        } else {
          apVO.setStatus("");
        }

        ls.add(apVO);
      }

    } catch (final SQLException e) {
      if (LOG.isDebugEnabled())
        LOG.debug("Exception While Getting Records of Proposal for mass revoke", e);
    } catch (final Exception e) {
      if (LOG.isDebugEnabled())
        LOG.debug("Exception While Getting Records of Proposal for mass revoke", e);
    } finally {
      OBContext.restorePreviousMode();
      try {
        if (st != null)
          st.close();
        if (rs != null)
          rs.close();
      } catch (final SQLException e) {
        if (LOG.isDebugEnabled())
          LOG.debug("Exception While Getting Records of Proposal for mass revoke", e);
      }
    }
    return ls;
  }

  @SuppressWarnings("unused")
  @Override
  public String updateRecord(VariablesSecureApp var, String selectIds, String inpWindowId) {

    String Result = "Success";
    String alertWindow = "", alertRuleId = "", appstatus = "";
    ArrayList<String> includeRecipient = new ArrayList<String>();
    int count = 0;
    String forwardId = null, rmiId = null;
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    try {
      List<String> result = Arrays.asList(selectIds.split("\\s*,\\s*"));

      // Task No. 6225 start
      String docNo = revokeValidation(selectIds);
      if (!docNo.isEmpty()) {
        String errormsg = docNo + ' '
            + Resource.getProperty("utility.process.checklineinfo", var.getLanguage());
        return errormsg;
      } else {
        for (int i = 0; i < result.size(); i++) {
          int a = i;
          EscmProposalMgmt header = OBDal.getInstance().get(EscmProposalMgmt.class, result.get(i));
          List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
          enccontrollist = ProposalManagementActionMethod.getEncControleList(header);

          String DocStatus = header.getProposalappstatus();
          Boolean errorFlag = false, fromPR = false;
          JSONObject resultEncum = null;
          EfinBudgetManencum encumbrance = null;

          // check lines added from pr ( direct PR- proposal)
          for (EscmProposalmgmtLine line : header.getEscmProposalmgmtLineList()) {
            OBQuery<EscmProposalsourceRef> proposalsrcref = OBDal.getInstance()
                .createQuery(EscmProposalsourceRef.class, "escmProposalmgmtLine.id='" + line.getId()
                    + "' and requisitionLine.id is not null");
            List<EscmProposalsourceRef> proposalsrcreflist = proposalsrcref.list();
            if (proposalsrcreflist != null && proposalsrcreflist.size() > 0) {
              fromPR = true;
              break;
            }
          }

          header.setUpdated(new java.util.Date());
          header.setUpdatedBy(OBContext.getOBContext().getUser());
          if (header.isNeedEvaluation())
            header.setProposalstatus("AWD");
          else
            header.setProposalstatus("DR");
          if (header.getEscmBaseproposal() == null)
            header.setProposalappstatus("INC");
          else
            header.setProposalappstatus("REA");
          header.setEscmDocaction("SA");
          header.setEUTNextRole(null);
          OBDal.getInstance().save(header);
          OBDal.getInstance().flush();
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.PROPOSAL_MANAGEMENT);
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.PROPOSAL_MANAGEMENT_DIRECT);

          // start impacts after revoke

          // get the detail about Purchase requsition fromt the Proposal line-Source ref
          resultEncum = ProposalManagementActionMethod.checkFullPRQtyUitlizeorNot(header);

          if (enccontrollist.size() > 0 && header.isEfinIsbudgetcntlapp()) {

            if (header.getEscmBaseproposal() != null && header.isEfinIsbudgetcntlapp()
                && header.getEscmOldproposal().getEfinEncumbrance() != null) {
              // New version encumbrance update
              // it will insert modification in existing encumbrance when amount is differ in new
              // version
              if (header.getEfinEncumbrance() != null
                  && header.getEfinEncumbrance().getEncumMethod().equals("A")) {
                ProposalManagementActionMethod.doRejectPOVersionMofifcationInEncumbrance(header,
                    header.getEscmBaseproposal(), false, null);
              } else {
                ProposalManagementActionMethod.chkNewVersionManualEncumbranceValidation(header,
                    header.getEscmBaseproposal(), false, true, null);
              }
            } else {
              OBInterceptor.setPreventUpdateInfoChange(true);

              // revert encum impacts
              // if associate proposal line does not have PR
              if (!fromPR) {

                // if proposal is manual encumbrance then reverse applied amount
                if (header.getEfinEncumbrance() != null) {
                  if (header.getEfinEncumbrance().getEncumType().equals("PAE")) {
                    if (header.getEfinEncumbrance().getEncumMethod().equals("M")) {
                      ProposalManagementRejectMethods.updateManualEncumAppAmt(header, false);
                      header.setEfinIsbudgetcntlapp(false);
                      OBDal.getInstance().save(header);

                    }
                    // if auto the delete the new encumbrance and update the budget inquiry funds
                    // available
                    else {

                      // remove encum
                      EfinBudgetManencum encum = header.getEfinEncumbrance();

                      ProposalManagementRejectMethods.updateAutoEncumbrancechanges(header, false);

                      // remove encum reference in proposal lines.
                      List<EscmProposalmgmtLine> proline = header.getEscmProposalmgmtLineList();
                      for (EscmProposalmgmtLine proLineList : proline) {
                        proLineList.setEfinBudgmanencumline(null);
                        OBDal.getInstance().save(proLineList);
                      }

                      OBDal.getInstance().flush();
                      OBDal.getInstance().remove(encum);
                      // update the budget controller flag and encumbrance ref
                      header.setEfinEncumbrance(null);
                      header.setEfinIsbudgetcntlapp(false);
                      OBDal.getInstance().save(header);
                    }
                  }
                  // if associate encumbrance type is not proposal award encumbrance - then
                  // encumbrance
                  // associate with bid . so need to change the encumbrance stage as "Bid
                  // Encumbrance"
                  else {
                    if (header.getEscmBidmgmt() != null) {
                      if (header.getEscmBidmgmt().getEncumbrance() != null) {
                        ProposalManagementRejectMethods.getProposaltoBidDetailsRej(header, false,
                            true, null);
                        header.setEfinIsbudgetcntlapp(false);
                        OBDal.getInstance().save(header);
                      }

                    }
                  }
                  // change the encumbrance stage as "Bid Encumbrance"
                  if (header.getEfinEncumbrance() != null) {
                    encumbrance = header.getEfinEncumbrance();
                    encumbrance.setEncumStage("BE");
                    encumbrance.setBusinessPartner(null);
                    OBDal.getInstance().save(encumbrance);
                  }
                }
              } else {
                if (header.getEfinEncumbrance() != null)
                  encumbrance = header.getEfinEncumbrance();
                else if (header.getEscmProposalmgmtLineList().get(0)
                    .getEfinBudgmanencumline() != null)
                  encumbrance = header.getEscmProposalmgmtLineList().get(0)
                      .getEfinBudgmanencumline().getManualEncumbrance();
                if (encumbrance != null) {
                  // if Proposal is associate with Purchase Requisition
                  if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                      && resultEncum.getBoolean("isAssociatePREncumbrance")
                      && resultEncum.has("isFullQtyUsed")
                      && resultEncum.getBoolean("isFullQtyUsed")) {
                    encumbrance = header.getEfinEncumbrance();
                    encumbrance.setEncumStage("PRE");
                    encumbrance.setBusinessPartner(null);
                    OBDal.getInstance().save(encumbrance);

                    ProposalManagementActionMethod.chkAndUpdateforProposalPRFullQty(header,
                        encumbrance, false, true);

                    header.setEfinIsbudgetcntlapp(false);
                    header.setEfinEncumbrance(null);

                  } else {

                    // reactive the new encumbrance changes while did split and merge
                    if (resultEncum.has("type") && resultEncum.getString("type").equals("SPLIT")) {
                      ProposalManagementRejectMethods.reactivateSplitPR(resultEncum, header, false,
                          null);
                    }
                    if (resultEncum.has("type") && resultEncum.getString("type").equals("MERGE")) {
                      ProposalManagementRejectMethods.reactivateSplitPR(resultEncum, header, false,
                          null);
                    }

                    // if pr is skip the encumbrance
                    if (resultEncum.has("isAssociatePREncumbrance")
                        && !resultEncum.getBoolean("isAssociatePREncumbrance")) {
                      // remove encum
                      EfinBudgetManencum encum = header.getEfinEncumbrance();

                      ProposalManagementRejectMethods.updateAutoEncumbrancechanges(header, false);

                      // remove encum reference in proposal lines.
                      List<EscmProposalmgmtLine> proline = header.getEscmProposalmgmtLineList();
                      for (EscmProposalmgmtLine proLineList : proline) {
                        proLineList.setEfinBudgmanencumline(null);
                        OBDal.getInstance().save(proLineList);
                      }

                      OBDal.getInstance().flush();
                      OBDal.getInstance().remove(encum);
                      // update the budget controller flag and encumbrance ref
                      header.setEfinEncumbrance(null);
                      header.setEfinIsbudgetcntlapp(false);
                      OBDal.getInstance().save(header);
                    }
                  }
                }
              }
            }
            OBDal.getInstance().flush();
            OBInterceptor.setPreventUpdateInfoChange(false);
          }
          // end impacts after revoke
          // Task No. 6225 end

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
            historyData.put("HistoryTable", ApprovalTables.Proposal_Management_History);
            historyData.put("HeaderColumn",
                ApprovalTables.Proposal_Management_History_HEADER_COLUMN);
            historyData.put("ActionColumn",
                ApprovalTables.Proposal_Management_History_DOCACTION_COLUMN);
            count = Utility.InsertApprovalHistory(historyData);
          }
          if (LOG.isDebugEnabled()) {
            LOG.debug("headerId:" + header.getId());
            LOG.debug("count :" + count);
          }

          // --------

          // delete the unused nextroles in eut_next_role table.
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.PROPOSAL_MANAGEMENT);
          // delete the unused nextroles in eut_next_role table.
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.PROPOSAL_MANAGEMENT_DIRECT);

          // Removing forwardRMI id
          if (header.getEUTForwardReqmoreinfo() != null) {
            forwardId = header.getEUTForwardReqmoreinfo().getId();
            // Removing the Role Access given to the forwarded user
            // forwardReqMoreInfoDAO.removeRoleAccess(clientId, forwardId, conn);
            // set status as DR in forward Record
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTForwardReqmoreinfo());
            // Removing Forward_Rmi id from transaction screens
            forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(header.getId(),
                Constants.PROPOSAL_MANAGEMENT);

          }
          if (header.getEUTReqmoreinfo() != null) {
            rmiId = header.getEUTReqmoreinfo().getId();
            // access remove
            // forwardReqMoreInfoDAO.removeReqMoreInfoRoleAccess(clientId, rmiId, conn);
            // set status as DR in forward Record
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTReqmoreinfo());
            // Remove Forward_Rmi id from transaction screens
            forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(header.getId(),
                Constants.PROPOSAL_MANAGEMENT);

          }

          // -------------

          if (count > 0 && !StringUtils.isEmpty(header.getId())) {
            Role objCreatedRole = null;
            if (header.getCreatedBy().getADUserRolesList().size() > 0) {
              objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
            }
            alertWindow = AlertWindow.ProposalManagement;

            // remove approval alert
            OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
                "as e where e.referenceSearchKey='" + result.get(i) + "' and e.alertStatus='NEW'");
            if (alertQuery.list().size() > 0) {
              for (Alert objAlert : alertQuery.list()) {
                OBDal.getInstance().remove(objAlert);
              }
            }
            // check and insert alert recipient
            OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance().createQuery(
                AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");
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
              AlertUtility.insertAlertRecipient(iterator.next(), null, var.getClient(),
                  alertWindow);
            }

            Result = "Success";
            OBDal.getInstance().save(header);
            OBDal.getInstance().flush();
          }
        }
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();

      LOG.error("Exception While Revoke Proposal :", e);

      Result = "Error";
    }
    return Result;
  }

  @Override
  public String validateRecord(String selectIds, String inpWindowId) {
    String ids = null;
    List<String> result = Arrays.asList(selectIds.split("\\s*,\\s*"));
    for (int i = 0; i < result.size(); i++) {
      EscmProposalMgmt prop = OBDal.getInstance().get(EscmProposalMgmt.class, result.get(i));
      if (prop.getProposalappstatus().equals("INC")) {
        if (ids == null) {
          ids = prop.getProposalno();
        } else {
          ids = ids + ", " + prop.getProposalno();
        }
      }
    }
    return ids;
  }

  /**
   * 
   * @param selectIds
   * @return DocumentNo if wrong validations else return ""
   */
  public String revokeValidation(String selectIds) {
    List<String> result = Arrays.asList(selectIds.split("\\s*,\\s*"));
    String documentNo = "";
    for (int i = 0; i < result.size(); i++) {

      EscmProposalMgmt header = OBDal.getInstance().get(EscmProposalMgmt.class, result.get(i));
      documentNo = header.getProposalno();

      Boolean errorFlag = false, fromPR = false;
      JSONObject resultEncum = null;
      EfinBudgetManencum encumbrance = null;

      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      enccontrollist = ProposalManagementActionMethod.getEncControleList(header);

      // pre validation before reject the Proposal
      if (enccontrollist.size() > 0) {
        // check budget controller approved or not , if approved do the prevalidation
        if (header.isEfinIsbudgetcntlapp()) {

          // check lines added from pr ( direct PR- proposal)
          for (EscmProposalmgmtLine line : header.getEscmProposalmgmtLineList()) {
            OBQuery<EscmProposalsourceRef> proposalsrcref = OBDal.getInstance()
                .createQuery(EscmProposalsourceRef.class, "escmProposalmgmtLine.id='" + line.getId()
                    + "' and requisitionLine.id is not null");
            List<EscmProposalsourceRef> proposalsrcreflist = proposalsrcref.list();
            if (proposalsrcreflist != null && proposalsrcreflist.size() > 0) {
              fromPR = true;
              break;
            }
          }
          // if lines not added from PR then do the further validation
          if (!fromPR) {

            // if proposal is added by using bid managment then do the further validation
            if (header.getEscmBidmgmt() != null) {
              if (header.getEscmBidmgmt().getEncumbrance() != null) {

                // check pre validation , if encumbrance lines having decrease , increase or
                // unique
                // code changes , then check with funds available
                errorFlag = ProposalManagementRejectMethods.getProposaltoBidDetailsRej(header, true,
                    true, null);

                // if error flag is true then throw the error - please check the line info.
                if (errorFlag) {
                  return documentNo;
                }
              }
            }

          }

          // if proposal line is associate with PR
          else {

            if (header.getEfinEncumbrance() != null) {
              encumbrance = header.getEfinEncumbrance();

              // get the detail about Purchase requsition fromt the Proposal line-Source ref
              resultEncum = ProposalManagementActionMethod.checkFullPRQtyUitlizeorNot(header);

              // if full qty only used then remove the encumbrance reference from the proposal and
              // change the
              // encumencumbrance stage as previous Stage

              try {
                if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                    && resultEncum.getBoolean("isAssociatePREncumbrance")
                    && resultEncum.has("isFullQtyUsed")
                    && !resultEncum.getBoolean("isFullQtyUsed")) {

                  // check if associate pr qty does not use full qty then while reject check funds
                  // available (case: if unique code is change from pr to proposal)
                  errorFlag = BidManagementDAO.chkFundsAvailforReactOldEncumbrance(null, header,
                      null);
                } else {
                  errorFlag = ProposalManagementActionMethod
                      .chkAndUpdateforProposalPRFullQty(header, encumbrance, true, true);
                }
              } catch (JSONException e) {
                // TODO Auto-generated catch block
                LOG.error("Exception While revokeValidation() in Proposal Massrevoke:", e);
              } catch (ParseException e) {
                // TODO Auto-generated catch block
                LOG.error("Exception While revokeValidation() in Proposal Massrevoke :", e);
              }
              if (errorFlag) {
                return documentNo;
              }
            }

          }

        }
      }
    }
    return "";
  }
}
