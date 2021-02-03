package sa.elm.ob.scm.ad_forms.ApprovalRevoke.dao;

import java.math.BigDecimal;
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
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.finance.ad_process.purchaseRequisition.RequisitionfundsCheck;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;
import sa.elm.ob.scm.ad_process.POandContract.dao.POContractSummaryDAO;
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

public class POMassRevokeDAO extends MassRevoke {
  private Connection conn = null;
  private static Logger LOG = Logger.getLogger(POMassRevokeDAO.class);

  public POMassRevokeDAO(Connection con) {
    this.conn = con;
  }

  public int getRevokeRecordsCount(VariablesSecureApp vars, String clientId, String windowId,
      String searchFlag, ApprovalRevokeVO vo) {
    PreparedStatement st = null;
    ResultSet rs = null;
    int totalRecord = 0;
    String sqlQuery = "", fromClause = "", whereClause = "";

    try {
      fromClause = "  select count(*) as totalRecord from "
          + "  (select org.name as org, ord.documentno as documentno, ord.em_escm_appstatus, ord.c_order_id as id, rr.name as requester, "
          + " (select (coalesce(trl.name,st.name)||' - '|| ur.name) from escm_purorderacthist history, ad_user ur, ad_ref_list st "
          + " left join ad_ref_list_trl trl on trl.ad_ref_list_id=st.ad_ref_list_id and trl.ad_language=(select lang.ad_language "
          + " from ad_language lang  where lang.ad_language_id='112')  where history.c_order_id = ord.c_order_id  and st.value = history.requestreqaction "
          + " and st.ad_reference_id='9F2DC8F55FE9442895FCD3ED468B1D50' and ur.ad_user_id = history.createdby order by history.updated desc limit 1) as lastaction, "
          + " pen.pending, ord.em_eut_next_role_id   " + " from c_order ord   "
          + " left join ad_user rr on rr.ad_user_id=ord.createdby "
          + " left join ad_org org on org.ad_org_id=ord.ad_org_id  "
          + " left join (select array_to_string(array_agg(role.name),' / ') as pending, lin.eut_next_role_id  from eut_next_role rl "
          + " join eut_next_role_line lin on lin.eut_next_role_id=rl.eut_next_role_id   "
          + " join ad_role role on role.ad_role_id=lin.ad_role_id  group by lin.eut_next_role_id ) as pen on pen.eut_next_role_id=ord.em_eut_next_role_id ";

      whereClause = " where ord.ad_client_id = '" + clientId + "'";

      whereClause += " and ord.em_escm_appstatus='ESCM_IP' and ord.em_eut_next_role_id is not null "
          + " and ord.ad_org_id  in (  select org.ad_org_id from ad_user rr  "
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
          whereClause += " and ord.documentno ilike '%" + vo.getDocno() + "%'";
        if (vo.getNextrole() != null)
          whereClause += " and pen.pending ilike '%" + vo.getNextrole() + "%'";
        if (vo.getStatus() != null) {
          if (("in progress").contains(vo.getStatus().toLowerCase()) || vo.getStatus().isEmpty()) {
            whereClause += " and ord.em_escm_appstatus ='ESCM_IP'";
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
        LOG.debug("Exception While Getting Records of PO for mass revoke", e);
    } catch (final Exception e) {
      if (LOG.isDebugEnabled())
        LOG.debug("Exception While Getting Records of PO for mass revoke", e);
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
          LOG.debug("Exception While Getting Records of PO for mass revoke", e);
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

      fromClause = "  select * from "
          + "  (select org.name as org, ord.documentno as documentno, ord.em_escm_appstatus, ord.c_order_id as id, rr.name as requester, "
          + " (select (coalesce(trl.name,st.name)||' - '|| ur.name) from escm_purorderacthist history, ad_user ur, ad_ref_list st "
          + " left join ad_ref_list_trl trl on trl.ad_ref_list_id=st.ad_ref_list_id and trl.ad_language=(select lang.ad_language "
          + " from ad_language lang  where lang.ad_language_id='112')  where history.c_order_id = ord.c_order_id  and st.value = history.requestreqaction "
          + " and st.ad_reference_id='9F2DC8F55FE9442895FCD3ED468B1D50' and ur.ad_user_id = history.createdby order by history.updated desc limit 1) as lastaction, "
          + " pen.pending, ord.em_eut_next_role_id   " + " from c_order ord   "
          + " left join ad_user rr on rr.ad_user_id=ord.createdby "
          + " left join ad_org org on org.ad_org_id=ord.ad_org_id  "
          + " left join (select array_to_string(array_agg(role.name),' / ') as pending, lin.eut_next_role_id  from eut_next_role rl "
          + " join eut_next_role_line lin on lin.eut_next_role_id=rl.eut_next_role_id   "
          + " join ad_role role on role.ad_role_id=lin.ad_role_id  group by lin.eut_next_role_id ) as pen on pen.eut_next_role_id=ord.em_eut_next_role_id ";

      whereClause = " where ord.ad_client_id = '" + clientId + "'";

      whereClause += " and ord.em_escm_appstatus='ESCM_IP' and ord.em_eut_next_role_id is not null "
          + " and ord.ad_org_id  in (  select org.ad_org_id from ad_user rr  "
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
          whereClause += " and ord.documentno ilike '%" + vo.getDocno() + "%'";
        if (vo.getNextrole() != null)
          whereClause += " and pen.pending ilike '%" + vo.getNextrole() + "%'";
        if (vo.getStatus() != null) {
          if (("in progress").contains(vo.getStatus().toLowerCase()) || vo.getStatus().isEmpty()) {
            whereClause += " and ord.em_escm_appstatus='ESCM_IP' ";
          } else {
            whereClause += " and 1=2";
          }
        }

      }
      if (sortColName != null && sortColName.equals("org"))
        orderClause += " order by org.name  " + sortColType + " limit " + limit + " offset "
            + offset;
      else if (sortColName != null && sortColName.equals("docno")) {
        orderClause += " order by ord.documentno " + sortColType + " limit " + limit + " offset "
            + offset;
      } else if (sortColName != null && sortColName.equals("requester"))
        orderClause += " order by rr.name " + sortColType + " limit " + limit + " offset " + offset;
      else if (sortColName != null && sortColName.equals("nextrole"))
        orderClause += " order by pen.pending " + sortColType + " limit " + limit + " offset "
            + offset;
      else if (sortColName != null && sortColName.equals("status")) {
        orderClause += " order by ord.em_escm_appstatus='ESCM_IP' " + sortColType + " limit "
            + limit + " offset " + offset;
      } else {
        orderClause += " order by ord.documentno desc" + " limit " + limit + " offset " + offset;
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
        if (Utility.nullToEmpty(rs.getString("em_escm_appstatus")).equals("ESCM_IP")) {
          apVO.setStatus("In Progress");
        } else {
          apVO.setStatus("");
        }

        ls.add(apVO);
      }

    } catch (final SQLException e) {
      if (LOG.isDebugEnabled())
        LOG.debug("Exception While Getting Records of PO for mass revoke", e);
    } catch (final Exception e) {
      if (LOG.isDebugEnabled())
        LOG.debug("Exception While Getting Records of PO for mass revoke", e);
    } finally {
      OBContext.restorePreviousMode();
      try {
        if (st != null)
          st.close();
        if (rs != null)
          rs.close();
      } catch (final SQLException e) {
        if (LOG.isDebugEnabled())
          LOG.debug("Exception While Getting Records of PO for mass revoke", e);
      }
    }
    return ls;
  }

  @Override
  public String updateRecord(VariablesSecureApp var, String selectIds, String inpWindowId) {

    String Result = "Success";
    String alertWindow = "", alertRuleId = "", appstatus = "";
    String clientId = OBContext.getOBContext().getCurrentClient().getId();
    ArrayList<String> includeRecipient = new ArrayList<String>();
    int count = 0;
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    boolean fromPR = false, fromProposal = false;
    JSONObject resultEncum = null;
    List<EfinBudgetManencumlines> encumLinesList = null;
    List<EfinEncControl> enccontrollist = null;
    Boolean errorFlag = false;
    EfinBudgetManencum encumbrance = null;
    EfinBudgetManencum encum = null;
    try {
      List<String> result = Arrays.asList(selectIds.split("\\s*,\\s*"));

      String docNo = revokeValidation(selectIds);
      if (!docNo.isEmpty() && docNo.equals("Efin_Encum_Used_Cannot_Rej")) {
        String errormsg = docNo + ' '
            + Resource.getProperty("utility.process.checklineinfo", var.getLanguage());
        return errormsg;
      } else if (!docNo.isEmpty() && !docNo.equals("Efin_Encum_Used_Cannot_Rej")) {
        String errormsg = docNo + ' '
            + Resource.getProperty("utility.process.cantReject_Encumused", var.getLanguage());
        return errormsg;
      } else {
        for (int i = 0; i < result.size(); i++) {
          Order header = OBDal.getInstance().get(Order.class, result.get(i));

          OBQuery<EfinEncControl> encumcontrol = OBDal.getInstance().createQuery(
              EfinEncControl.class, " as e where e.encumbranceType='POE' and e.client.id='"
                  + clientId + "' and e.active='Y' ");
          encumcontrol.setFilterOnActive(true);
          encumcontrol.setMaxResult(1);
          enccontrollist = encumcontrol.list();

          if (enccontrollist.size() > 0 && !header.getEscmOrdertype().equals("PUR_AG")) {

            encumbrance = header.getEfinBudgetManencum();

            if (header.getEscmBaseOrder() != null && header.isEfinEncumbered()
                && header.getEscmOldOrder().getEfinBudgetManencum() != null) {
              if (header.getEfinBudgetManencum() != null
                  && header.getEfinBudgetManencum().getEncumMethod().equals("M")) {
                errorFlag = POContractSummaryDAO.chkNewVersionManualEncumbranceValidation(header,
                    header.getEscmBaseOrder(), true, true, null);
                if (!errorFlag) {
                  POContractSummaryDAO.chkNewVersionManualEncumbranceValidation(header,
                      header.getEscmBaseOrder(), false, true, null);
                  errorFlag = true;
                }
              } else {
                JSONObject object = POContractSummaryDAO.getUniquecodeListforPOVerAuto(header,
                    header.getEscmBaseOrder(), true, null);
                // funds validation.
                errorFlag = RequisitionfundsCheck.autoEncumbranceValidation(object,
                    header.getEfinBudgetint(), "PO", false);
                if (!errorFlag) {
                  POContractSummaryDAO.doRejectPOVersionMofifcationInEncumbrance(header,
                      header.getEscmBaseOrder(), false, null);
                  errorFlag = true;
                }
              }
            } else {
              // check from proposal line added case:
              if (header.getEscmProposalmgmt() == null) {
                // check lines added from pr
                OBQuery<OrderLine> orderLine = OBDal.getInstance().createQuery(OrderLine.class,
                    "salesOrder.id='" + header.getId()
                        + "' and efinMRequisitionline.id is not null");
                if (orderLine.list() != null && orderLine.list().size() > 0) {
                  fromPR = true;
                }
              } else {
                fromProposal = true;
              }

              // if after budget control, try to reject then check funds for negative impacts.
              if (header.isEfinEncumbered()) {
                OBInterceptor.setPreventUpdateInfoChange(true);

                // get encum line list

                OBQuery<EfinBudgetManencumlines> encumLines = OBDal.getInstance().createQuery(
                    EfinBudgetManencumlines.class,
                    " manualEncumbrance.id='" + header.getEfinBudgetManencum().getId() + "'");
                if (encumLines.list() != null && encumLines.list().size() > 0) {
                  encumLinesList = encumLines.list();
                }
                // validation
                errorFlag = POContractSummaryDAO.checkFundsForReject(header, encumLinesList);
                log.debug("errorFlag:" + errorFlag);
                if (errorFlag) {

                  if (!fromPR && !fromProposal) {

                    // manual encum
                    if (header.getEfinBudgetManencum().getEncumMethod().equals("M")) {
                      // update amount
                      POContractSummaryDAO.updateManualEncumAmountRej(header, encumLinesList, false,
                          "");
                      header.setEfinEncumbered(false);
                      header.getEfinBudgetManencum().setBusinessPartner(null);
                      OBDal.getInstance().save(header);

                    }
                    // auto encumbrance
                    else {
                      POContractSummaryDAO.updateAmtInEnquiryRej(header.getId(), encumLinesList,
                          false, "");
                      // remove encum
                      encum = header.getEfinBudgetManencum();
                      encum.setDocumentStatus("DR");
                      header.setEfinBudgetManencum(null);
                      header.setEfinEncumbered(false);
                      OBDal.getInstance().save(header);
                      // remove encum reference in lines.
                      List<OrderLine> ordLine = header.getOrderLineList();
                      for (OrderLine ordLineList : ordLine) {
                        ordLineList.setEfinBudEncumlines(null);
                        OBDal.getInstance().save(ordLineList);
                      }
                      OBDal.getInstance().flush();
                      OBDal.getInstance().remove(encum);
                    }
                  } else if (fromPR) {
                    // reactivate the merge and splitencumbrance
                    resultEncum = POContractSummaryDAO.checkFullPRQtyUitlizeorNot(header);

                    // if full qty only used then remove the encumbrance reference and change the
                    // encumencumbrance stage as PR Stage
                    if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                        && resultEncum.getBoolean("isAssociatePREncumbrance")
                        && resultEncum.has("isFullQtyUsed")
                        && resultEncum.getBoolean("isFullQtyUsed")) {
                      encumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
                          resultEncum.getString("encumbrance"));
                      errorFlag = POContractSummaryDAO.chkAndUpdateforProposalPRFullQty(header,
                          encumbrance, true, true);
                      log.debug("errorFlag:" + errorFlag);
                      if (!errorFlag) {
                        encumbrance.setEncumStage("PRE");

                        POContractSummaryDAO.chkAndUpdateforProposalPRFullQty(header, encumbrance,
                            false, true);

                        if (header.getEfinBudgetManencum() != null)
                          header.getEfinBudgetManencum().setBusinessPartner(null);
                        header.setEfinBudgetManencum(null);
                        header.setEfinEncumbered(false);
                        OBDal.getInstance().save(header);
                        OBDal.getInstance().save(encumbrance);

                        errorFlag = true;
                      }
                    }
                    // if pr is skip the encumbrance
                    else if (resultEncum.has("isAssociatePREncumbrance")
                        && !resultEncum.getBoolean("isAssociatePREncumbrance")) {

                      errorFlag = POContractSummaryDAO.checkFundsForReject(header, encumLinesList);
                      if (errorFlag) {
                        POContractSummaryDAO.updateAmtInEnquiryRej(header.getId(), encumLinesList,
                            false, "");
                        // remove encum
                        if (header.getEfinBudgetManencum() != null) {
                          encum = header.getEfinBudgetManencum();
                          encum.setDocumentStatus("DR");
                          header.setEfinBudgetManencum(null);
                          header.setEfinEncumbered(false);
                          OBDal.getInstance().save(header);
                          // remove encum reference in lines.
                          List<OrderLine> ordLine = header.getOrderLineList();
                          for (OrderLine ordLineList : ordLine) {
                            ordLineList.setEfinBudEncumlines(null);
                            OBDal.getInstance().save(ordLineList);
                          }
                          OBDal.getInstance().flush();
                          OBDal.getInstance().remove(encum);
                        }
                      }
                    }
                    // if full qty not used / manual encumbrance remaining amount and applied amount
                    // will not match / one or more encumbrance used in PO
                    else {
                      errorFlag = POContractSummaryDAO.chkFundsAvailforReactOldEncumbrance(header,
                          null);
                      if (!errorFlag) {
                        if (resultEncum.has("type")
                            && resultEncum.getString("type").equals("SPLIT")) {
                          POContractSummaryDAO.reactivateSplitPR(resultEncum, header);
                        }
                        if (resultEncum.has("type")
                            && resultEncum.getString("type").equals("MERGE")) {
                          POContractSummaryDAO.reactivateSplitPR(resultEncum, header);
                        }
                        errorFlag = true;
                      }
                    }
                  } else if (fromProposal) {
                    if (header.getEfinBudgetManencum().getEncumType().equals("POE")) {
                      // newly created so delete new and increase in old.
                      POContractSummaryDAO.reactivateSplitPR(resultEncum, header);
                      errorFlag = true;
                      POContractSummaryDAO.updateOldProposalEncum(header);
                    } else if (header.getEfinBudgetManencum().getEncumStage().equals("POE")) {
                      for (OrderLine objOrderLine : header.getOrderLineList()) {
                        // check diff between proposal and order, make impact in encumbrance
                        BigDecimal diff = objOrderLine.getLineNetAmount()
                            .subtract(objOrderLine.getEscmProposalmgmtLine().getLineTotal());
                        if (diff.compareTo(BigDecimal.ZERO) < 0) {
                          // check funds available
                          // delete modification
                          EfinBudgetManencumlines encumbranceline = objOrderLine
                              .getEfinBudEncumlines();
                          ProposalManagementRejectMethods.deleteModification(encumbranceline, diff);
                          encumbranceline.setAPPAmt(encumbranceline.getAPPAmt().add(diff.negate()));
                          OBDal.getInstance().save(encumbranceline);
                        } else if (diff.compareTo(BigDecimal.ZERO) > 0) {
                          // insert modification
                          if (header.getEfinBudgetManencum().getEncumType().equals("A")) {
                            EfinBudgetManencumlines encumbranceline = objOrderLine
                                .getEfinBudEncumlines();
                            ProposalManagementRejectMethods.deleteModification(encumbranceline,
                                diff);
                            encumbranceline
                                .setAPPAmt(encumbranceline.getAPPAmt().add(diff.negate()));
                            OBDal.getInstance().save(encumbranceline);
                          } else {
                            EfinBudgetManencumlines encumbranceline = objOrderLine
                                .getEfinBudEncumlines();
                            encumbranceline
                                .setAPPAmt(encumbranceline.getAPPAmt().add(diff.negate()));
                            OBDal.getInstance().save(encumbranceline);
                          }
                        }
                      }

                      header.getEfinBudgetManencum().setEncumStage("PAE");
                      header.setEfinEncumbered(false);
                      OBDal.getInstance().save(header);
                      errorFlag = true;
                      // POContractSummaryDAO.reactivatePOProposal(objOrder);
                      // errorFlag = true;
                    } else {
                      // old encum just reduce value.
                      header.getEfinBudgetManencum().setEncumStage("PAE");
                      header.setEfinEncumbered(false);
                      OBDal.getInstance().save(header);
                      OBDal.getInstance().flush();
                    }
                  }
                }
              }
              OBDal.getInstance().flush();
              OBInterceptor.setPreventUpdateInfoChange(false);

            }
          }
          header.setUpdated(new java.util.Date());
          header.setUpdatedBy(OBContext.getOBContext().getUser());
          header.setEscmDocaction("CO");
          header.setEscmAppstatus("DR");
          header.setEutNextRole(null);
          OBDal.getInstance().save(header);
          // delete the unused nextroles.
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.PURCHASE_ORDER_RULE);

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
            historyData.put("HistoryTable", ApprovalTables.Purchase_Order_History);
            historyData.put("HeaderColumn", ApprovalTables.Purchase_Order_History_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.Purchase_Order_History_DOCACTION_COLUMN);
            count = Utility.InsertApprovalHistory(historyData);
          }
          if (LOG.isDebugEnabled()) {
            LOG.debug("headerId:" + header.getId());
            LOG.debug("count:" + count);
          }

          if (count > 0 && !StringUtils.isEmpty(header.getId())) {
            Role objCreatedRole = null;
            if (header.getEscmAdRole() != null) {
              objCreatedRole = header.getEscmAdRole();
            } else if (header.getCreatedBy().getADUserRolesList().size() > 0) {
              objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
            }
            alertWindow = AlertWindow.PurchaseOrderContract;

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
            // delete the unused nextroles in eut_next_role table.
            DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                Resource.PURCHASE_ORDER_RULE);

            // Removing the forwardRMI id
            if (header.getEutForward() != null) {
              // Removing the Role Access given to the forwarded user
              // Update statuses draft the forward Record
              forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEutForward());
              // Removing Forward_Rmi id from transaction screens
              forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(header.getId(),
                  Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY);

            }
            if (header.getEutReqmoreinfo() != null) {
              // Update statuses draft the RMI Record
              forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEutReqmoreinfo());
              // access remove
              // Remove Forward_Rmi id from transaction screens
              forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(header.getId(),
                  Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY);

            }

            Result = "Success";
            OBDal.getInstance().save(header);
            OBDal.getInstance().flush();
            OBDal.getInstance().commitAndClose();
          }
        }
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      if (LOG.isDebugEnabled()) {
        LOG.error("Exception While Revoke PO :", e);
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
      Order ord = OBDal.getInstance().get(Order.class, result.get(i));
      if (ord.getEscmAppstatus().equals("DR") || ord.getEscmAppstatus().equals("ESCM_TR")) {
        if (ids == null) {
          ids = ord.getDocumentNo();
        } else {
          ids = ids + ", " + ord.getDocumentNo();
        }
      }
    }
    return ids;
  }

  /**
   * 
   * @param selectIds
   * @return DocumentNo if validations fails, else return blank.
   */
  public String revokeValidation(String selectIds) {
    List<String> result = Arrays.asList(selectIds.split("\\s*,\\s*"));
    String documentNo = "";
    boolean errorFlag = true;
    boolean fromPR = false, fromProposal = false;
    EfinBudgetManencum encumbrance = null;
    JSONObject resultEncum = null;
    List<EfinBudgetManencumlines> encumLinesList = null;
    try {
      for (int i = 0; i < result.size(); i++) {
        errorFlag = true;
        fromPR = false;
        fromProposal = false;
        encumLinesList = null;
        resultEncum = null;
        Order header = OBDal.getInstance().get(Order.class, result.get(i));
        documentNo = header.getDocumentNo();

        List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
        enccontrollist = POContractSummaryDAO.getEncControleList(header);
        if (enccontrollist.size() > 0 && !header.getEscmOrdertype().equals("PUR_AG")) {
          encumbrance = header.getEfinBudgetManencum();

          if (header.getEscmBaseOrder() != null && header.isEfinEncumbered()
              && header.getEscmOldOrder().getEfinBudgetManencum() != null) {
            if (header.getEfinBudgetManencum() != null
                && header.getEfinBudgetManencum().getEncumMethod().equals("M")) {
              errorFlag = POContractSummaryDAO.chkNewVersionManualEncumbranceValidation(header,
                  header.getEscmBaseOrder(), true, true, null);
              if (errorFlag) {
                return documentNo;
              }
            } else {
              JSONObject object = POContractSummaryDAO.getUniquecodeListforPOVerAuto(header,
                  header.getEscmBaseOrder(), true, null);
              // funds validation.
              errorFlag = RequisitionfundsCheck.autoEncumbranceValidation(object,
                  header.getEfinBudgetint(), "PO", false);
              if (errorFlag) {
                return documentNo;
              }
            }
          } else {
            // check from proposal line added case:
            if (header.getEscmProposalmgmt() == null) {
              // check lines added from pr
              OBQuery<OrderLine> orderLine = OBDal.getInstance().createQuery(OrderLine.class,
                  "salesOrder.id='" + header.getId() + "' and efinMRequisitionline.id is not null");
              if (orderLine.list() != null && orderLine.list().size() > 0) {
                fromPR = true;
              }
            } else {
              fromProposal = true;
            }

            // if after budget control, try to reject then check funds for negative impacts.
            if (header.isEfinEncumbered()) {
              // get encum line list

              OBQuery<EfinBudgetManencumlines> encumLines = OBDal.getInstance().createQuery(
                  EfinBudgetManencumlines.class,
                  " manualEncumbrance.id='" + header.getEfinBudgetManencum().getId() + "'");
              if (encumLines.list() != null && encumLines.list().size() > 0) {
                encumLinesList = encumLines.list();
              }
              // validation
              errorFlag = POContractSummaryDAO.checkFundsForReject(header, encumLinesList);
              log.debug("errorFlag:" + errorFlag);
              if (errorFlag) {

                if (fromPR) {
                  // reactivate the merge and splitencumbrance
                  resultEncum = POContractSummaryDAO.checkFullPRQtyUitlizeorNot(header);

                  // if full qty only used then remove the encumbrance reference and change the
                  // encumencumbrance stage as PR Stage
                  if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                      && resultEncum.getBoolean("isAssociatePREncumbrance")
                      && resultEncum.has("isFullQtyUsed")
                      && resultEncum.getBoolean("isFullQtyUsed")) {
                    encumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
                        resultEncum.getString("encumbrance"));
                    errorFlag = POContractSummaryDAO.chkAndUpdateforProposalPRFullQty(header,
                        encumbrance, true, true);
                    log.debug("errorFlag:" + errorFlag);
                    if (errorFlag) {
                      return documentNo;
                    }
                  }
                  // if pr is skip the encumbrance
                  else if (resultEncum.has("isAssociatePREncumbrance")
                      && !resultEncum.getBoolean("isAssociatePREncumbrance")) {

                    errorFlag = POContractSummaryDAO.checkFundsForReject(header, encumLinesList);
                    if (!errorFlag) {
                      return "Efin_Encum_Used_Cannot_Rej";
                    }
                  }
                  // if full qty not used / manual encumbrance remaining amount and applied amount
                  // will not match / one or more encumbrance used in PO
                  else {
                    errorFlag = POContractSummaryDAO.chkFundsAvailforReactOldEncumbrance(header,
                        null);
                    if (errorFlag) {
                      return documentNo;
                    }
                  }
                } else if (fromProposal) {
                  if (header.getEfinBudgetManencum().getEncumType().equals("POE")) {
                    // newly created so delete new and increase in old.
                    errorFlag = POContractSummaryDAO.chkFundsAvailforReactOldEncumbrance(header,
                        null);
                    if (errorFlag) {
                      return documentNo;
                    }
                  } else if (header.getEfinBudgetManencum().getEncumStage().equals("POE")) {
                    for (OrderLine objOrderLine : header.getOrderLineList()) {
                      // check diff between proposal and order, make impact in encumbrance
                      BigDecimal diff = objOrderLine.getLineNetAmount()
                          .subtract(objOrderLine.getEscmProposalmgmtLine().getLineTotal());
                      if (diff.compareTo(BigDecimal.ZERO) < 0) {
                        // check funds available
                        JSONObject fundsCheckingObject = CommonValidationsDAO.CommonFundsChecking(
                            header.getEfinBudgetint(), objOrderLine.getEFINUniqueCode(),
                            diff.negate());
                        if (fundsCheckingObject.has("errorFlag")) {
                          if ("0".equals(fundsCheckingObject.get("errorFlag"))) {
                            String status = fundsCheckingObject.getString("message");
                            objOrderLine.setEfinFailureReason(status);
                            OBDal.getInstance().save(objOrderLine);
                            errorFlag = true;
                            return documentNo;
                          }
                        }
                      }
                    }
                  }
                }
              } else {
                return "Efin_Encum_Used_Cannot_Rej";
              }
            }
          }
        }

      }
    } catch (Exception e) {
      log.error("Exception in validation mass revoke po:", e);
      if (log.isDebugEnabled())
        log.debug("Exception in validation mass revoke po:" + e);
    }
    return "";
  }
}
