package sa.elm.ob.scm.ad_process.SiteIssueRequest;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.common.plm.Product;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMProductCategoryV;
import sa.elm.ob.scm.EscmInitialReceipt;
import sa.elm.ob.scm.Escm_custody_transaction;
import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.MaterialIssueRequestCustody;
import sa.elm.ob.scm.MaterialIssueRequestHistory;
import sa.elm.ob.scm.MaterialIssueRequestLine;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutDocappDelegateln;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Gopalakrishnan on 10/04/2017
 */

public class SiteIssueRequest extends DalBaseProcess {

  /**
   * This servlet class was responsible for Site Issue Request Submission Process with Approval
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(SiteIssueRequest.class);
  private final OBError obError = new OBError();
  private static String errorMsg = null;
  ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String Lang = vars.getLanguage();
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String appstatus = "";
    boolean errorFlag = false;
    boolean allowUpdate = false, isPeriodOpen = false;

    log.debug("entering into Requisition Submit");
    try {
      OBContext.setAdminMode();
      String strRequisitionId = (String) bundle.getParams().get("Escm_Material_Request_ID");
      MaterialIssueRequest objRequest = OBDal.getInstance().get(MaterialIssueRequest.class,
          strRequisitionId);
      String DocStatus = objRequest.getAlertStatus();
      String DocAction = objRequest.getEscmSmirAction();
      NextRoleByRuleVO nextApproval = null;
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = objRequest.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      Date currentDate = new Date();
      String comments = (String) bundle.getParams().get("notes").toString(), query = "", sql = "";
      int count = 0;
      Boolean allowDelegation = false, chkRoleIsInDocRul = false, chkSubRolIsInFstRolofDR = false;
      Boolean allowApprove = false;
      List<MaterialIssueRequestHistory> historyList = new ArrayList<MaterialIssueRequestHistory>();

      if (!objRequest.getRole().getId().equals(roleId)) {
        OBQuery<MaterialIssueRequestHistory> history = OBDal.getInstance().createQuery(
            MaterialIssueRequestHistory.class,
            " as e where e.escmMaterialRequest.id=:reqID order by e.creationDate desc ");
        history.setNamedParameter("reqID", strRequisitionId);
        history.setMaxResult(1);
        historyList = history.list();
        if (historyList.size() > 0) {
          MaterialIssueRequestHistory apphistory = historyList.get(0);
          if (apphistory.getRequestreqaction().equals("REV")) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Escm_AlreadyPreocessed_Approved@");
            bundle.setResult(result);
            return;
          }
        }
      }

      // Check transaction period is opened or not
      isPeriodOpen = Utility.checkOpenPeriod(objRequest.getTransactionDate(), orgId, clientId);
      if (!isPeriodOpen) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
        bundle.setResult(result);
        return;
      }

      // If the record is Forwarded or given RMI then throw error when any other user tries to
      // approve the record without refreshing the page
      if (objRequest.getEUTForward() != null) {
        allowApprove = forwardDao.allowApproveReject(objRequest.getEUTForward(), userId, roleId,
            Resource.MATERIAL_ISSUE_REQUEST);
      }
      if (objRequest.getEUTReqmoreinfo() != null
          || ((objRequest.getEUTForward() != null) && (!allowApprove))) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }

      // chk available remaining qty
      for (MaterialIssueRequestLine line : objRequest.getEscmMaterialReqlnList()) {
        query = " select coalesce((ln.quantity-coalesce(ln.sitereqissuedqty,0)- coalesce(ln.return_qty,0)-coalesce(inprg.inprgqty,0) ) ,0) as remainingqty,ln.escm_initialreceipt_id ,prd.name "
            + " from escm_initialreceipt  ln    left join m_product prd on prd.m_product_id= ln.m_product_id left join (  SELECT sum(reqln.requested_qty) AS inprgqty, reqln.escm_initialreceipt_id "
            + "      FROM escm_material_request req    LEFT JOIN escm_material_reqln reqln ON reqln.escm_material_request_id::text = req.escm_material_request_id::text "
            + "  WHERE req.status::text = 'ESCM_IP'::text AND req.issiteissuereq = 'Y'::bpchar  and  "
            + "  reqln.escm_material_reqln_id  <>  ?  "
            + "  GROUP BY reqln.escm_initialreceipt_id)  inprg ON inprg.escm_initialreceipt_id::text = ln.escm_initialreceipt_id::text "
            + "  where ln.escm_initialreceipt_id = ? ";
        log.debug("Check query:" + query);
        ps = conn.prepareStatement(query);
        ps.setString(1, line.getId());
        ps.setString(2, line.getEscmInitialreceipt().getId());
        rs = ps.executeQuery();
        if (rs.next()) {
          if (rs.getBigDecimal("remainingqty").compareTo(line.getRequestedQty()) < 0) {
            errorFlag = true;
            String msg = OBMessageUtils.messageBD("ESCM_SMIR_ReqQtyExcRemQty");
            msg = msg.replace("%", rs.getString("remainingqty"));
            msg = msg.replace("@", rs.getString("name"));
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", msg);
            bundle.setResult(result);
            return;
          }
        }
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      }
      // check role is present in document rule or not
      if (DocStatus.equals("DR")) {
        chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(OBDal.getInstance().getConnection(),
            clientId, orgId, userId, roleId, Resource.MATERIAL_ISSUE_REQUEST, BigDecimal.ZERO);
        log.debug("chkRoleIsInDocRul:" + chkRoleIsInDocRul);
        if (!chkRoleIsInDocRul) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_RoleIsNotIncInDocRule@");// ESCM_RoleIsNotIncInDocRule
          bundle.setResult(result);
          return;
        }
      }
      // chk submitting role is in first role in document rule
      if (DocStatus.equals("DR")) {
        chkSubRolIsInFstRolofDR = UtilityDAO.chkSubRolIsInFstRolofDR(
            OBDal.getInstance().getConnection(), clientId, orgId, userId, roleId,
            Resource.MATERIAL_ISSUE_REQUEST, BigDecimal.ZERO);
        log.debug("chkSubRolIsInFstRolofDR:" + chkSubRolIsInFstRolofDR);
        if (!chkSubRolIsInFstRolofDR) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_Role_NotFundsReserve_submit@");
          bundle.setResult(result);
          return;
        }
      }

      nextApproval = NextRoleByRule.getRequesterNextRole(OBDal.getInstance().getConnection(),
          clientId, orgId, roleId, userId, Resource.MATERIAL_ISSUE_REQUEST,
          objRequest.getRole().getId());

      // if Role doesnot have any user associated then this condition will execute and return
      // error
      if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains("EUT_NOUser_ForRoles")) {
        errorMsg = OBMessageUtils.messageBD(nextApproval.getErrorMsg());
        errorMsg = errorMsg.replace("@", nextApproval.getRoleName());
        count = -2;
      }
      /*
       * nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
       * orgId, roleId, userId, Resource.MATERIAL_ISSUE_REQUEST, BigDecimal.ZERO);
       */
      // check lines to submit
      if (objRequest.getEscmMaterialReqlnList().size() == 0) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_No_Requisition_Lines@");
        bundle.setResult(result);
        return;
      }

      // check current role associated with document rule for approval flow
      if (!DocStatus.equals("DR") && !DocStatus.equals("ESCM_RJD")) {
        if (objRequest.getEUTNextRole() != null) {
          java.util.List<EutNextRoleLine> li = objRequest.getEUTNextRole().getEutNextRoleLineList();
          for (int i = 0; i < li.size(); i++) {
            String role = li.get(i).getRole().getId();
            if (roleId.equals(role)) {
              allowUpdate = true;
            }
          }
        }
        if (objRequest.getEUTNextRole() != null) {
          sql = "";
          Connection con = OBDal.getInstance().getConnection();
          PreparedStatement st = null;
          ResultSet rs1 = null;
          sql = "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
              + currentDate + "' and to_date >='" + currentDate + "' and document_type='EUT_112'";
          st = con.prepareStatement(sql);
          rs1 = st.executeQuery();
          while (rs1.next()) {
            String roleid = rs1.getString("ad_role_id");
            if (roleid.equals(roleId)) {
              allowDelegation = true;
              break;
            }
          }
          if (rs1 != null)
            rs1.close();
          if (st != null)
            st.close();
        }
        if (!allowUpdate && !allowDelegation) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }
      }
      // throw the error message while 2nd user try to approve while 1st user already reworked that
      // record with same role
      if ((!vars.getUser().equals(objRequest.getCreatedBy().getId()))
          && DocStatus.equals("ESCM_RJD")) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      // throw an error in case if approver try to approving the record while the submit User is
      // already revoked the record
      if ((!vars.getUser().equals(objRequest.getCreatedBy().getId())) && DocStatus.equals("DR")) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      /**
       ** Final Approval Quantity Check **
       */
      if (vars.getRole() != null) {
        // check role is warehouse Keeper then check qty
        OBQuery<Preference> preQuery = OBDal.getInstance().createQuery(Preference.class,
            "as e where e.property='ESCM_WarehouseKeeper' and e.searchKey='Y' "
                + " and e.visibleAtRole.id=:roleID ");
        preQuery.setNamedParameter("roleID", vars.getRole());
        if (preQuery.list().size() > 0) {
          // check Warehouse is empty
          if (objRequest.getWarehouse() == null) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_WarehouseEmpty@");
            bundle.setResult(result);
            return;
          }
          query = "select (coalesce(mrec.quantity,0)-coalesce(sitereqissuedqty,0)) as avstock ,usedrec.uqty,mreq.escm_material_reqln_id as lnid "
              + " from escm_material_reqln mreq "
              + " left join escm_initialreceipt mrec on mreq.escm_initialreceipt_id=mrec.escm_initialreceipt_id "
              + " left join escm_material_request mmir on mmir.escm_material_request_id= mreq.escm_material_request_id "
              + " left join (select sum(req.delivered_qty) as uqty,rec.escm_initialreceipt_id from escm_material_reqln req "
              + " left join escm_initialreceipt rec on req.escm_initialreceipt_id=rec.escm_initialreceipt_id "
              + " left join escm_material_request mir on mir.escm_material_request_id= req.escm_material_request_id "
              + " where 1=1 and mir.status <>'DR' "
              + " group by rec.escm_initialreceipt_id ) usedrec on usedrec.escm_initialreceipt_id=mrec.escm_initialreceipt_id "
              + " where coalesce((usedrec.uqty),0) > coalesce(mrec.quantity,0) and  mreq.escm_material_request_id='"
              + strRequisitionId + "' ";
          ps = conn.prepareStatement(query);
          rs = ps.executeQuery();
          while (rs.next()) {
            errorFlag = true;
            MaterialIssueRequestLine line = OBDal.getInstance().get(MaterialIssueRequestLine.class,
                rs.getString("lnid"));
            line.setFailureReason("Available quantity is " + rs.getString("avstock"));
            OBDal.getInstance().save(line);
            OBDal.getInstance().flush();
          }
          if (errorFlag) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_SIMRequestQty_Exceeds@");
            bundle.setResult(result);
            return;
          }
          query = "select (coalesce(mrec.quantity,0)-coalesce(sitereqissuedqty,0)) as avstock ,usedrec.uqty,mreq.escm_material_reqln_id as lnid "
              + " from escm_material_reqln mreq "
              + " left join escm_initialreceipt mrec on mreq.escm_initialreceipt_id=mrec.escm_initialreceipt_id "
              + " left join escm_material_request mmir on mmir.escm_material_request_id= mreq.escm_material_request_id "
              + " left join (select sum(req.delivered_qty) as uqty,rec.escm_initialreceipt_id from escm_material_reqln req "
              + " left join escm_initialreceipt rec on req.escm_initialreceipt_id=rec.escm_initialreceipt_id "
              + " left join escm_material_request mir on mir.escm_material_request_id= req.escm_material_request_id "
              + " where 1=1 and mir.status <>'DR' "
              + " group by rec.escm_initialreceipt_id ) usedrec on usedrec.escm_initialreceipt_id=mrec.escm_initialreceipt_id "
              + " where coalesce((usedrec.uqty),0) <= coalesce(mrec.quantity,0) and  mreq.escm_material_request_id='"
              + strRequisitionId + "' ";
          log.debug("Check query:" + query);
          ps = conn.prepareStatement(query);
          rs = ps.executeQuery();
          while (rs.next()) {
            MaterialIssueRequestLine line = OBDal.getInstance().get(MaterialIssueRequestLine.class,
                rs.getString("lnid"));
            line.setFailureReason("");
            OBDal.getInstance().save(line);
            OBDal.getInstance().flush();
          }

        }
      }
      if (!errorFlag) {
        // remove Reactivatedby when record submitted--Reactivatedby
        objRequest.setReactivatedby(null);
        if ((DocStatus.equals("DR") || DocStatus.equals("ESCM_RJD")) && DocAction.equals("CO")) {
          appstatus = "SUB";
        } else if (DocStatus.equals("ESCM_IP") && DocAction.equals("AP")) {
          appstatus = "AP";
        }
        count = updateHeaderStatus(conn, clientId, orgId, roleId, userId, objRequest, appstatus,
            comments, currentDate, vars, nextApproval, Lang);
        log.debug("count:" + count);
        boolean sequenceexists = false;
        if (count == 2) {
          if (objRequest.getSpecNo() == null) {
            String sequence = Utility.getSpecificationSequence(objRequest.getOrganization().getId(),
                "MIR");
            if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_NoSpecSequence@");
              bundle.setResult(result);
              return;
            } else {
              sequenceexists = Utility
                  .chkSpecificationSequence(objRequest.getOrganization().getId(), "MIR", sequence);
              if (!sequenceexists) {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@Escm_Duplicate_SpecNo@");
                bundle.setResult(result);
                return;
              }
              objRequest.setSpecNo(sequence);
              OBDal.getInstance().save(objRequest);
            }
          }
          String existingDocNo = "1000000001";
          int custodyCount = 0, deliveredQty = 0;
          // final Approval Flow
          // entry in Transaction
          // get Line List
          // get recent tag number
          OBQuery<MaterialIssueRequestCustody> objCustodyQry = OBDal.getInstance().createQuery(
              MaterialIssueRequestCustody.class,
              "as e where e.organization.id=:orgID order by documentNo desc");
          objCustodyQry.setNamedParameter("orgID", objRequest.getOrganization().getId());
          objCustodyQry.setMaxResult(1);
          if (objCustodyQry.list().size() > 0) {
            MaterialIssueRequestCustody recentObj = objCustodyQry.list().get(0);
            if (recentObj.getDocumentNo() != null
                && StringUtils.isNotEmpty(recentObj.getDocumentNo()))
              existingDocNo = String.valueOf(Integer.parseInt(recentObj.getDocumentNo()) + 1);
          }

          // make custody for only custody products
          query = " select escm_material_reqln_id from escm_material_reqln ln "
              + " join m_product prd on prd.m_product_id=ln.m_product_id "
              + " join (select escm_deflookups_typeln_id from escm_deflookups_type lt "
              + " join escm_deflookups_typeln ltl on ltl.escm_deflookups_type_id=lt.escm_deflookups_type_id "
              + " where lt.reference='PST' and ltl.value='CUS') cusref on cusref.escm_deflookups_typeln_id=prd.em_escm_stock_type "
              + " where ln.escm_material_request_id='" + strRequisitionId + "'";
          ps = conn.prepareStatement(query);
          rs = ps.executeQuery();
          while (rs.next()) {
            // check already existing custody line count
            MaterialIssueRequestLine objLineList = OBDal.getInstance()
                .get(MaterialIssueRequestLine.class, rs.getString("escm_material_reqln_id"));
            custodyCount = objLineList.getEscmMrequestCustodyList().size();
            deliveredQty = objLineList.getDeliveredQantity().intValue();
            log.debug("existingDocNo:" + existingDocNo);
            log.debug("deliveredQty:" + deliveredQty);
            // no custody line insert the custodies line
            if (custodyCount == 0) {
              for (int i = 1; i <= deliveredQty; i++) {
                // get existing tag no
                MaterialIssueRequestCustody objCustody = OBProvider.getInstance()
                    .get(MaterialIssueRequestCustody.class);
                Product objProduct = OBDal.getInstance().get(Product.class,
                    objLineList.getProduct().getId());
                ESCMProductCategoryV prdcat = OBDal.getInstance().get(ESCMProductCategoryV.class,
                    objProduct.getProductCategory().getId());
                objCustody.setProductCategory(prdcat);
                objCustody.setDocumentNo(existingDocNo);
                objCustody.setQuantity(BigDecimal.ONE);
                objCustody.setDescription(objLineList.getDescription());
                objCustody.setAlertStatus("IU");
                objCustody.setOrganization(objLineList.getOrganization());
                objCustody.setEscmMaterialReqln(objLineList);
                objCustody.setProduct(objProduct);
                if (objProduct.getEscmCusattribute() != null)
                  objCustody.setAttributeSet(objProduct.getEscmCusattribute());
                objCustody.setBeneficiaryType(objRequest.getBeneficiaryType());
                objCustody.setBeneficiaryIDName(objRequest.getBeneficiaryIDName());
                objCustody.setProcurement(objLineList.getConditions());
                OBDal.getInstance().save(objCustody);
                // create Custody Transaction
                Escm_custody_transaction objCustodyhistory = OBProvider.getInstance()
                    .get(Escm_custody_transaction.class);
                objCustodyhistory.setLineNo(Long.valueOf(10));
                if (objRequest.getSpecNo() != null) {
                  objCustodyhistory.setDocumentNo(objRequest.getSpecNo());
                } else {
                  objCustodyhistory.setDocumentNo(objRequest.getDocumentNo());

                }
                objCustodyhistory.setOrganization(objCustody.getOrganization());
                objCustodyhistory.setBname(objRequest.getBeneficiaryIDName());
                objCustodyhistory.setBtype(objRequest.getBeneficiaryType());
                objCustodyhistory.setEscmMrequestCustody(objCustody);
                objCustodyhistory.setTransactionDate(objRequest.getTransactionDate());
                objCustodyhistory.setTransactiontype("IE");
                objCustodyhistory.setProcessed(true);
                objCustodyhistory.setLine2(Long.valueOf(10));
                OBDal.getInstance().save(objCustodyhistory);
                existingDocNo = String.valueOf(Integer.parseInt(existingDocNo) + 1);

              }
            }
          }

          // ----------------------------------
          for (MaterialIssueRequestLine objRequestLine : objRequest.getEscmMaterialReqlnList()) {
            if (objRequestLine.getDeliveredQantity().compareTo(BigDecimal.ZERO) == 1) {
              // ----update in po receipt
              EscmInitialReceipt objInitiaReceipt = objRequestLine.getEscmInitialreceipt();
              objInitiaReceipt.setSitereqissuedqty(
                  objInitiaReceipt.getSitereqissuedqty().add(objRequestLine.getDeliveredQantity()));
              OBDal.getInstance().save(objInitiaReceipt);
            }
          }

          OBDal.getInstance().flush();
        } else if (count == -2) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", errorMsg);
          bundle.setResult(result);
          return;
        }

        if (count > 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          return;
        } else {
          errorFlag = false;
        }
      }
      if (errorFlag) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage("Process Failed");
      }
      bundle.setResult(obError);
      OBDal.getInstance().save(objRequest);
      OBDal.getInstance().flush();
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.MATERIAL_ISSUE_REQUEST);
      OBDal.getInstance().commitAndClose();
    } catch (OBException e) {
      log.error("Exeception in Site Material Issue Request Submit:", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exeception in Site Material Issue Request Submit:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      // final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
      // vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));

    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();
    }

  }

  private int updateHeaderStatus(Connection con, String clientId, String orgId, String roleId,
      String userId, MaterialIssueRequest objRequest, String appstatus, String comments,
      Date currentDate, VariablesSecureApp vars, NextRoleByRuleVO nextApproval, String Lang) {
    String requistionId = null, pendingapproval = null;
    int count = 0;
    Boolean isDirectApproval = false;
    String alertRuleId = "", alertWindow = AlertWindow.SiteIssueRequest, strRoleId = "";
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    User objCreater = objRequest.getCreatedBy();
    boolean isDummyRole = false;

    try {
      OBContext.setAdminMode();

      // NextRoleByRuleVO nextApproval = NextRoleByRule.getNextRole(con, clientId, orgId,
      // roleId,userId, Resource.PURCHASE_REQUISITION, 0.00);
      NextRoleByRuleVO objnextApproval = nextApproval;
      EutNextRole nextRole = null;
      boolean isBackwardDelegation = false;
      HashMap<String, String> role = null;
      String qu_next_role_id = "";
      String delegatedFromRole = null;
      String delegatedToRole = null;
      isDirectApproval = isDirectApproval(objRequest.getId(), roleId);
      strRoleId = objRequest.getRole().getId();
      JSONObject fromUserandRoleJson = new JSONObject();
      String fromUser = userId;
      String fromRole = roleId;
      List<EutDocappDelegateln> delegationLnList = new ArrayList<EutDocappDelegateln>();

      // get alert rule id - Task No:7618
      alertRuleId = AlertUtility.getAlertRule(clientId, alertWindow);

      /**
       * fetching from role and user based on delegater / forwarder/ direct approver
       **/
      if ((objRequest.getEUTNextRole() != null)) {

        fromUserandRoleJson = forwardDao.getFromuserAndFromRoleWhileApprove(
            objRequest.getEUTNextRole(), userId, roleId, clientId, orgId,
            Resource.MATERIAL_ISSUE_REQUEST, isDummyRole, isDirectApproval);
        if (fromUserandRoleJson != null && fromUserandRoleJson.length() > 0) {
          if (fromUserandRoleJson.has("fromUser"))
            fromUser = fromUserandRoleJson.getString("fromUser");
          if (fromUserandRoleJson.has("fromRole"))
            fromRole = fromUserandRoleJson.getString("fromRole");
          if (fromUserandRoleJson.has("isDirectApproval"))
            isDirectApproval = fromUserandRoleJson.getBoolean("isDirectApproval");
        }
      } else {
        fromUser = userId;
        fromRole = roleId;
      }

      if ((objRequest.getEUTNextRole() == null)) {
        objnextApproval = NextRoleByRule.getRequesterNextRole(OBDal.getInstance().getConnection(),
            clientId, orgId, fromRole, fromUser, Resource.MATERIAL_ISSUE_REQUEST, strRoleId);
      } else {
        if (isDirectApproval) {
          objnextApproval = NextRoleByRule.getRequesterNextRole(OBDal.getInstance().getConnection(),
              clientId, orgId, fromRole, fromUser, Resource.MATERIAL_ISSUE_REQUEST, strRoleId);

          if (objnextApproval != null && objnextApproval.hasApproval()) {
            nextRole = OBDal.getInstance().get(EutNextRole.class, objnextApproval.getNextRoleId());
            if (nextRole.getEutNextRoleLineList().size() > 0) {
              for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
                OBQuery<UserRoles> userRole = OBDal.getInstance().createQuery(UserRoles.class,
                    "role.id=:roleID");
                userRole.setNamedParameter("roleID", objNextRoleLine.getRole().getId());
                role = NextRoleByRule.getbackwardDelegatedFromAndToRoles(
                    OBDal.getInstance().getConnection(), clientId, orgId,
                    userRole.list().get(0).getUserContact().getId(),
                    Resource.MATERIAL_ISSUE_REQUEST, "");
                delegatedFromRole = role.get("FromUserRoleId");
                delegatedToRole = role.get("ToUserRoleId");
                isBackwardDelegation = NextRoleByRule.isBackwardDelegation(
                    OBDal.getInstance().getConnection(), clientId, orgId, delegatedFromRole,
                    delegatedToRole, fromUser, Resource.MATERIAL_ISSUE_REQUEST, 0.00);
                if (isBackwardDelegation)
                  break;
              }
            }
          }
          // if Role doesnot have any user associated then this condition will execute and return
          // error
          else if (objnextApproval != null && objnextApproval.getErrorMsg() != null
              && objnextApproval.getErrorMsg().contains("EUT_NOUser_ForRoles")) {
            errorMsg = OBMessageUtils.messageBD(objnextApproval.getErrorMsg());
            errorMsg = errorMsg.replace("@", objnextApproval.getRoleName());
            count = -2;
          }

          if (isBackwardDelegation) {
            objnextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(),
                clientId, orgId, delegatedFromRole, fromUser, Resource.MATERIAL_ISSUE_REQUEST,
                0.00);
          }
        } else {
          role = NextRoleByRule.getDelegatedFromAndToRoles(OBDal.getInstance().getConnection(),
              clientId, orgId, fromUser, Resource.MATERIAL_ISSUE_REQUEST, qu_next_role_id);

          delegatedFromRole = role.get("FromUserRoleId");
          delegatedToRole = role.get("ToUserRoleId");

          if (delegatedFromRole != null && delegatedToRole != null)
            objnextApproval = NextRoleByRule.getRequesterDelegatedNextRole(
                OBDal.getInstance().getConnection(), clientId, orgId, delegatedFromRole,
                delegatedToRole, fromUser, Resource.MATERIAL_ISSUE_REQUEST,
                objRequest.getRole().getId());
        }
      }

      if (objnextApproval != null && objnextApproval.hasApproval()) {
        ArrayList<String> includeRecipient = new ArrayList<String>();
        nextRole = OBDal.getInstance().get(EutNextRole.class, objnextApproval.getNextRoleId());
        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(objRequest.getEUTNextRole(), Resource.MATERIAL_ISSUE_REQUEST);

        objRequest.setUpdated(new java.util.Date());
        objRequest.setUpdatedBy(OBContext.getOBContext().getUser());
        objRequest.setAlertStatus("ESCM_IP");
        objRequest.setEUTNextRole(nextRole);
        // get alert recipients - Task No:7618
        List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);

        // set alerts for next roles
        if (nextRole.getEutNextRoleLineList().size() > 0) {
          // solving approval alerts - Task No:7618
          AlertUtility.solveAlerts(objRequest.getId());

          String Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.smir.wfa", Lang)
              + " " + objCreater.getName();

          forwardDao.getAlertForForwardedUser(objRequest.getId(), alertWindow, alertRuleId, objUser,
              clientId, Constants.APPROVE, objRequest.getDocumentNo(), Lang, vars.getRole(),
              objRequest.getEUTForward(), Resource.MATERIAL_ISSUE_REQUEST, alertReceiversMap);
          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            AlertUtility.alertInsertionRole(objRequest.getId(), objRequest.getDocumentNo(),
                objNextRoleLine.getRole().getId(), "", objRequest.getClient().getId(), Description,
                "NEW", alertWindow, "scm.smir.wfa", Constants.GENERIC_TEMPLATE);
            // get user name for delegated user to insert on approval history.
            OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance().createQuery(
                EutDocappDelegateln.class,
                " as e left join e.eUTDocappDelegate as hd where hd.role.id =:roleID "
                    + " and hd.fromDate <=:currentdate and hd.date >=:currentdate "
                    + " and e.documentType='EUT_112'");
            delegationln.setNamedParameter("roleID", objNextRoleLine.getRole().getId());
            delegationln.setNamedParameter("currentdate", currentDate);
            delegationLnList = delegationln.list();

            if (delegationLnList.size() > 0) {
              AlertUtility.alertInsertionRole(objRequest.getId(), objRequest.getDocumentNo(),
                  delegationLnList.get(0).getRole().getId(),
                  delegationLnList.get(0).getUserContact().getId(), objRequest.getClient().getId(),
                  Description, "NEW", alertWindow, "scm.mir.wfa", Constants.GENERIC_TEMPLATE);
              log.debug("del role>" + delegationLnList.get(0).getRole().getId());
              includeRecipient.add(delegationLnList.get(0).getRole().getId());
              if (pendingapproval != null)
                pendingapproval += "/" + delegationLnList.get(0).getUserContact().getName();
              else
                pendingapproval = String.format(Constants.sWAITINGFOR_S_APPROVAL,
                    delegationLnList.get(0).getUserContact().getName());
            }
            // add next role recipient
            includeRecipient.add(objNextRoleLine.getRole().getId());
          }
        }
        // existing Recipient
        if (alrtRecList.size() > 0) {
          for (AlertRecipient objAlertReceipient : alrtRecList) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }
        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        }
        objRequest.setEscmSmirAction("AP");
        if (pendingapproval == null)
          pendingapproval = objnextApproval.getStatus();

        log.debug(
            "doc sts:" + objRequest.getAlertStatus() + "action:" + objRequest.getEscmAction());
        count = 1; // Waiting For Approval flow

      }
      // if Role doesnot have any user associated then this condition will execute and return
      // error
      else if (objnextApproval != null && objnextApproval.getErrorMsg() != null
          && objnextApproval.getErrorMsg().contains("EUT_NOUser_ForRoles")) {
        errorMsg = OBMessageUtils.messageBD(objnextApproval.getErrorMsg());
        errorMsg = errorMsg.replace("@", objnextApproval.getRoleName());
        count = -2;
      } else {
        ArrayList<String> includeRecipient = new ArrayList<String>();
        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(objRequest.getEUTNextRole(), Resource.MATERIAL_ISSUE_REQUEST);

        objRequest.setUpdated(new java.util.Date());
        objRequest.setUpdatedBy(OBContext.getOBContext().getUser());
        objRequest.setAlertStatus("ESCM_TR");
        Role objCreatedRole = null;
        if (objRequest.getCreatedBy().getADUserRolesList().size() > 0) {
          objCreatedRole = objRequest.getCreatedBy().getADUserRolesList().get(0).getRole();
        }

        // solving approval alerts - Task No:7618
        AlertUtility.solveAlerts(objRequest.getId());

        // get alert recipients - Task No:7618
        List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);

        // check and insert recipient
        if (alrtRecList.size() > 0) {
          for (AlertRecipient objAlertReceipient : alrtRecList) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }
        forwardDao.getAlertForForwardedUser(objRequest.getId(), alertWindow, alertRuleId, objUser,
            clientId, Constants.APPROVE, objRequest.getDocumentNo(), Lang, vars.getRole(),
            objRequest.getEUTForward(), Resource.MATERIAL_ISSUE_REQUEST, alertReceiversMap);
        if (objCreatedRole != null)
          includeRecipient.add(objCreatedRole.getId());
        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        } // set alert for requester
        String Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.smir.approved",
            Lang) + " " + objUser.getName();
        AlertUtility.alertInsertionRole(objRequest.getId(), objRequest.getDocumentNo(),
            objRequest.getRole().getId(), objRequest.getCreatedBy().getId(),
            objRequest.getClient().getId(), Description, "NEW", alertWindow, "scm.smir.approved",
            Constants.GENERIC_TEMPLATE);
        objRequest.setEUTNextRole(null);
        objRequest.setEscmSmirAction("PD");
        count = 2; // Final Approval Flow

      }

      OBDal.getInstance().save(objRequest);
      OBDal.getInstance().flush();
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.MATERIAL_ISSUE_REQUEST);

      requistionId = objRequest.getId();
      if (!StringUtils.isEmpty(requistionId)) {
        JSONObject historyData = new JSONObject();

        historyData.put("ClientId", clientId);
        historyData.put("OrgId", orgId);
        historyData.put("RoleId", roleId);
        historyData.put("UserId", userId);
        historyData.put("HeaderId", requistionId);
        historyData.put("Comments", comments);
        historyData.put("Status", appstatus);
        historyData.put("NextApprover", pendingapproval);
        historyData.put("HistoryTable", ApprovalTables.ISSUE_REQUEST_HISTORY);
        historyData.put("HeaderColumn", ApprovalTables.ISSUE_REQUEST_HEADER_COLUMN);
        historyData.put("ActionColumn", ApprovalTables.ISSUE_REQUEST_DOCACTION_COLUMN);

        Utility.InsertApprovalHistory(historyData);

      }
      OBDal.getInstance().flush();
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.MATERIAL_ISSUE_REQUEST);

      // after approved by forwarded user removing the forward and rmi id
      if (objRequest.getEUTForward() != null) {

        // set status as "Draft" for forward record
        forwardDao.setForwardStatusAsDraft(objRequest.getEUTForward());
        // set forward id as null in record
        objRequest.setEUTForward(null);
      }
      if (objRequest.getEUTReqmoreinfo() != null) {

        // set status as "Draft" for rmi record
        forwardDao.setForwardStatusAsDraft(objRequest.getEUTReqmoreinfo());
        // set rmi id as null in record
        objRequest.setEUTReqmoreinfo(null);
        objRequest.setRequestMoreInformation("N");
      }

    } catch (Exception e) {
      log.error("Exception in updateHeaderStatus in Site Material IssueRequest: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  private boolean isDirectApproval(String RequestId, String roleId) {

    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    try {
      query = "select count(req.escm_material_request_id) from escm_material_request req join eut_next_role rl on "
          + "req.eut_next_role_id = rl.eut_next_role_id "
          + "join eut_next_role_line li on li.eut_next_role_id = rl.eut_next_role_id "
          + "and req.escm_material_request_id = ? and li.ad_role_id =?";

      ps = con.prepareStatement(query);
      ps.setString(1, RequestId);
      ps.setString(2, roleId);

      rs = ps.executeQuery();

      if (rs.next()) {
        if (rs.getInt("count") > 0)
          return true;
        else
          return false;
      } else
        return false;

    } catch (Exception e) {
      log.error("Exception in isDirectApproval " + e.getMessage());
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
}