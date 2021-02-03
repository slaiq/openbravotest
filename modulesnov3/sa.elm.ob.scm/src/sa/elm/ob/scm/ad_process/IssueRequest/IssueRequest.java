
package sa.elm.ob.scm.ad_process.IssueRequest;

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
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMProductCategoryV;
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
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Gopalakrishnan on 09/03/2017
 */

public class IssueRequest extends DalBaseProcess {

  /**
   * This servlet class was responsible for Issue Request Submission Process with Approval
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(IssueRequest.class);
  private final OBError obError = new OBError();
  private static String errorMsg = null;
  ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();

  @SuppressWarnings("resource")
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
    boolean allowUpdate = false;
    Boolean isDirectApproval = false;
    String documentType = "EUT_112";

    log.debug("entering into Requisition Submit");
    try {
      OBContext.setAdminMode();
      String strRequisitionId = (String) bundle.getParams().get("Escm_Material_Request_ID");
      MaterialIssueRequest objRequest = OBDal.getInstance().get(MaterialIssueRequest.class,
          strRequisitionId);
      String DocStatus = objRequest.getAlertStatus();
      String DocAction = objRequest.getEscmAction();
      String LineNo = "";
      NextRoleByRuleVO nextApproval = null;
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = objRequest.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      Date currentDate = new Date();
      HashMap<String, String> delegationrole = null;
      String delegatedFromRole = null;
      String delegatedToRole = null;
      String comments = (String) bundle.getParams().get("notes").toString(), query = "";
      int count = 0;
      String preferenceValue = null;
      Boolean allowDelegation = false, chkRoleIsInDocRul = false, chkSubRolIsInFstRolofDR = false;
      isDirectApproval = IssueRequestDAO.isDirectApproval(objRequest.getId(), roleId);
      JSONObject fromUserandRoleJson = new JSONObject();
      String fromUser = userId;
      String fromRole = roleId;
      boolean isDummyRole = false, allowApprove = false, isPeriodOpen = true;

      if (!objRequest.getRole().getId().equals(roleId)) {
        OBQuery<MaterialIssueRequestHistory> history = OBDal.getInstance().createQuery(
            MaterialIssueRequestHistory.class,
            " as e where e.escmMaterialRequest.id=:reqID order by e.creationDate desc ");
        history.setNamedParameter("reqID", strRequisitionId);
        history.setMaxResult(1);
        if (history.list().size() > 0) {
          MaterialIssueRequestHistory apphistory = history.list().get(0);
          if (apphistory.getRequestreqaction().equals("REV")) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Escm_AlreadyPreocessed_Approved@");
            bundle.setResult(result);
            return;
          }
        }
      }
      // check document flow based on it product in lines
      if (DocStatus.equals("DR")) {
        OBQuery<MaterialIssueRequestLine> objLineQuery = OBDal.getInstance().createQuery(
            MaterialIssueRequestLine.class,
            "as e join e.product prd join prd.productCategory cat where cat.escmItproduct='Y' "
                + " and e.escmMaterialRequest.id=:reqID ");
        objLineQuery.setNamedParameter("reqID", objRequest.getId());
        if (objLineQuery.list().size() > 0) {
          documentType = "EUT_115";
        }
      } else {
        documentType = objRequest.getEscmDocumenttype();
      }
      objRequest.setEscmDocumenttype(documentType);
      OBDal.getInstance().save(objRequest);
      // check role is present in document rule or not
      if (DocStatus.equals("DR")) {
        chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(OBDal.getInstance().getConnection(),
            clientId, orgId, userId, roleId, documentType, BigDecimal.ZERO);
        log.debug("chkRoleIsInDocRul:" + chkRoleIsInDocRul);
        if (!chkRoleIsInDocRul) {
          OBDal.getInstance().flush();
          // OBDal.getInstance().rollbackAndClose();
          errorFlag = true;
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_MIR_RoleNotPresentIN_DocRule@");
          bundle.setResult(result);
          return;
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
            documentType);
      }
      if (objRequest.getEUTReqmoreinfo() != null
          || ((objRequest.getEUTForward() != null) && (!allowApprove))) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      // chk submitting role is in first role in document rule
      if (DocStatus.equals("DR")) {
        chkSubRolIsInFstRolofDR = UtilityDAO.chkSubRolIsInFstRolofDR(
            OBDal.getInstance().getConnection(), clientId, orgId, userId, roleId, documentType,
            BigDecimal.ZERO);
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

      if (objRequest.getWarehouse() != null
          && objRequest.getWarehouse().getEscmWarehouseType() != null
          && objRequest.getWarehouse().getEscmWarehouseType().equals("RTW")) {
        if (objRequest.getEscmMaterialReqlnList().size() > 0) {
          for (MaterialIssueRequestLine lineObj : objRequest.getEscmMaterialReqlnList()) {
            for (Escm_custody_transaction cusTranObj : lineObj.getEscmCustodyTransactionList()) {
              if (!cusTranObj.getEscmMrequestCustody().getAlertStatus().equals("RET")) {
                errorFlag = true;
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@ESCM_IssMIRRTW_TagAlrProcessed@");
                bundle.setResult(result);
                return;
              }
            }
          }
        }
      }
      if (DocStatus.equals("DR")) {

      }

      /**
       * fetching from role and user based on delegater / forwarder/ direct approver
       **/
      if (objRequest.getEUTNextRole() != null) {
        fromUserandRoleJson = forwardDao.getFromuserAndFromRoleWhileApprove(
            objRequest.getEUTNextRole(), userId, roleId, clientId, orgId, documentType, isDummyRole,
            isDirectApproval);
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

      // check final approval is warehouse keeper and preferences(warehouse keeper) is active or not
      if (objRequest.getEUTNextRole() == null) {
        nextApproval = NextRoleByRule.getRequesterNextRole(OBDal.getInstance().getConnection(),
            clientId, orgId, fromRole, fromUser, documentType, objRequest.getRole().getId());
      } else {
        if (isDirectApproval) {
          nextApproval = NextRoleByRule.getRequesterNextRole(OBDal.getInstance().getConnection(),
              clientId, orgId, fromRole, fromUser, documentType, objRequest.getRole().getId());
        } else {
          delegationrole = NextRoleByRule.getDelegatedFromAndToRoles(
              OBDal.getInstance().getConnection(), clientId, orgId, fromUser, documentType, "");

          delegatedFromRole = delegationrole.get("FromUserRoleId");
          delegatedToRole = delegationrole.get("ToUserRoleId");

          if (delegatedFromRole != null && delegatedToRole != null)
            nextApproval = NextRoleByRule.getRequesterDelegatedNextRole(
                OBDal.getInstance().getConnection(), clientId, orgId, delegatedFromRole,
                delegatedToRole, fromUser, documentType, objRequest.getRole().getId());
        }
      }
      // getting warehouse keeper preference
      try {
        preferenceValue = Preferences.getPreferenceValue("ESCM_WarehouseKeeper", true,
            OBContext.getOBContext().getCurrentClient().getId(),
            OBContext.getOBContext().getCurrentOrganization().getId(), fromUser, fromRole,
            Constants.MATERIAL_ISSUE_REQUEST_W);
      } catch (PropertyException e) {
        // Do not catch anything.
        preferenceValue = "N";
      }
      if (preferenceValue == null) {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_Preference_value@");
        bundle.setResult(result);
        return;
      }

      if (!objRequest.isSiteissuereq()) {
        if (preferenceValue != null && preferenceValue.equals("Y")) {
          if ((!objRequest.isSiteissuereq() && objRequest.getWarehouse() != null
              && objRequest.getWarehouse().getEscmWarehouseType().equals("RTW"))
              && (objRequest.getEscmIssuereason() == null
                  || objRequest.getEscmIssuereason().equals(""))) {
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_MIR_IssueReason@");
            bundle.setResult(result);
            return;
          }
        }
      }

      if (objRequest.getAlertStatus().equals("DR") && preferenceValue != null
          && preferenceValue.equals("Y")) {
        List<Preference> prefs = forwardDao.getPreferences("ESCM_WarehouseKeeper", true,
            vars.getClient(), objRequest.getOrganization().getId(), vars.getUser(), vars.getRole(),
            Constants.MATERIAL_ISSUE_REQUEST_W, false, true, true);
        for (Preference preferenceobj : prefs) {
          if (preferenceobj.getEutForwardReqmoreinfo() != null) {
            preferenceValue = "N";
          }
        }
      }

      if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains("EUT_NOUser_ForRoles")) {
        errorMsg = OBMessageUtils.messageBD(nextApproval.getErrorMsg());
        errorMsg = errorMsg.replace("@", nextApproval.getRoleName());
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", errorMsg);
        bundle.setResult(result);
        return;
      }
      if (nextApproval != null && nextApproval.getNextRoleId() == null) {

        // Task no:5140 check any one of the line issue qty should be greater than zero
        if (objRequest.getEscmMaterialReqlnList().size() > 0) {
          errorFlag = IssueRequestDAO.ChkAnyReqLinesGrtThanZero(objRequest);
          if (errorFlag) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_MatReqOneLineGrtZero@");
            bundle.setResult(result);
            return;
          }
        }
        // check generic product
        if (objRequest.getEscmMaterialReqlnList().size() > 0) {
          errorFlag = IssueRequestDAO.ChkGenericProduct(objRequest);
          if (errorFlag) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_generic_Not_issue@");
            bundle.setResult(result);
            return;
          }
        }

        /*
         * OBQuery<Preference> warehousekeeper = OBDal.getInstance().createQuery(Preference.class,
         * "as e where e.property='ESCM_WarehouseKeeper' and e.searchKey='Y' and e.visibleAtRole.id='"
         * + vars.getRole() + "' and active='Y'");
         */

        if ((preferenceValue == null || preferenceValue.equals("N"))
            && (delegatedFromRole == null && delegatedToRole == null)) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Cannot_Approve"));
        } else {
          if (objRequest.getWarehouse() != null) {
            if (objRequest.getWarehouse().getEscmWarehouseType().equals("RTW")) {
              List<String> lineNo2s = new ArrayList<String>();
              List<String> lineNos = new ArrayList<String>();
              if (objRequest.getEscmMaterialReqlnList().size() > 0) {
                for (MaterialIssueRequestLine objLineList : objRequest.getEscmMaterialReqlnList()) {
                  if (objLineList.getProduct().getEscmStockType().getSearchKey().equals("CUS")) {
                    if (objLineList.getDeliveredQantity().compareTo(BigDecimal.ZERO) == 1) {
                      if (objLineList.getEscmCustodyTransactionList().size() == 0) {
                        errorFlag = true;
                        lineNo2s.add(objLineList.getLineNo().toString());
                      }
                    }
                    if (new BigDecimal(objLineList.getEscmCustodyTransactionList().size())
                        .compareTo(objLineList.getDeliveredQantity()) != 0) {
                      errorFlag = true;
                      lineNos.add(objLineList.getLineNo().toString());
                    }
                  }
                }
                String LineNo2 = StringUtils.join(lineNo2s, ",");
                LineNo = StringUtils.join(lineNos, ",");

                if (!LineNo2.equals("") && errorFlag == true) {
                  OBDal.getInstance().rollbackAndClose();
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      OBMessageUtils.messageBD("Escm_IssueQty_EqualTo_Tag").replace("@", LineNo2));
                  bundle.setResult(result);
                  return;
                }
                if (errorFlag == true && !LineNo.equals("")) {
                  OBDal.getInstance().rollbackAndClose();
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      OBMessageUtils.messageBD("Escm_IssueQty_EqualTo_Tag").replace("@", LineNo));
                  bundle.setResult(result);
                  return;
                }
              }
            }
          }
        }

      }

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
          DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();

          allowDelegation = delagationDao.checkDelegation(currentDate, roleId, documentType);

          /*
           * sql = ""; Connection con = OBDal.getInstance().getConnection(); PreparedStatement st =
           * null; ResultSet rs1 = null; sql =
           * "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
           * + currentDate + "' and to_date >='" + currentDate + "' and document_type='EUT_112'"; st
           * = con.prepareStatement(sql); rs1 = st.executeQuery(); while (rs1.next()) { String
           * roleid = rs1.getString("ad_role_id"); if (roleid.equals(roleId)) { allowDelegation =
           * true; break; } }
           */
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
      if ((!vars.getUser().equals(objRequest.getCreatedBy().getId()))
          && objRequest.getReactivatedby() != null
          && (!vars.getUser().equals(objRequest.getReactivatedby().getId()))
          && DocStatus.equals("DR")) {
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
        /*
         * OBQuery<Preference> preQuery = OBDal.getInstance().createQuery(Preference.class,
         * "as e where e.property='ESCM_WarehouseKeeper' and e.searchKey='Y' and (e.userContact is null or e.userContact.id=:user) and e.visibleAtRole.id=:role "
         * ); preQuery.setNamedParameter("user", fromUser); preQuery.setNamedParameter("role",
         * fromRole); if (preQuery.list().size() > 0) { = preQuery.list().get(0);
         */
        //
        if (preferenceValue != null && preferenceValue.equals("Y")) {
          // check Warehouse is empty
          if (objRequest.getWarehouse() == null
              && (!DocStatus.equals("DR") || (DocStatus.equals("DR")))) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_WarehouseEmpty@");
            bundle.setResult(result);
            return;
          }
          User wrhUser = OBDal.getInstance().get(User.class, vars.getUser());
          if (wrhUser != null) {
            objRequest.setWarehouseKeeper(wrhUser.getName());
          }
          if (objRequest.getWarehouse() != null) {
            sa.elm.ob.scm.ad_process.IssueRequest.UpdateOnHandQty
                .updateOnHandQty(objRequest.getWarehouse().getId(), strRequisitionId);

            /*
             * task no 4560 hide gen tag // check custody product delivered qty // with custody
             * lines if (objRequest.getBeneficiaryType().equals("D") ||
             * objRequest.getBeneficiaryType().equals("E") ||
             * objRequest.getBeneficiaryType().equals("S")) { query =
             * "select ln.escm_material_reqln_id,coalesce(ccount,0) as ccount,prd.name as product from escm_material_reqln ln "
             * + " left join m_product prd on prd.m_product_id=ln.m_product_id  " +
             * " join (select escm_deflookups_typeln_id from escm_deflookups_type lt " +
             * " join escm_deflookups_typeln ltl on ltl.escm_deflookups_type_id=lt.escm_deflookups_type_id "
             * +
             * "  where lt.reference='PST' and ltl.value='CUS') cusref on cusref.escm_deflookups_typeln_id=prd.em_escm_stock_type "
             * +
             * " left join (select count(escm_mrequest_custody_id) as ccount,escm_material_reqln_id  from escm_mrequest_custody "
             * +
             * " group by escm_material_reqln_id ) cc on cc.escm_material_reqln_id=ln.escm_material_reqln_id "
             * + "  where ln.escm_material_request_id='" + strRequisitionId + "' " +
             * " and coalesce(ccount,0) <> coalesce(delivered_qty,0)"; ps =
             * conn.prepareStatement(query); rs = ps.executeQuery(); while (rs.next()) { if
             * (productname != null) productname += "," + rs.getString("product"); else productname
             * = rs.getString("product"); errorFlag = true; } if (errorFlag) { Status =
             * OBMessageUtils.messageBD("ESCM_CustodyQuantity_Less"); Status = Status.replace("%",
             * productname); OBDal.getInstance().rollbackAndClose(); obError.setType("Error");
             * obError.setTitle("Error"); obError.setMessage(Status); bundle.setResult(obError);
             * return;
             * 
             * }
             * 
             * }
             */
            // check product stock exceeds and update failure reason
            query = "select (coalesce(sd.qtyonhand,0)) as avstock,requested_qty,ln.m_product_id as product,ln.escm_material_reqln_id as lnid from escm_material_reqln ln "
                + " left join m_product prd on prd.m_product_id=ln.m_product_id "
                + " left join (select sum(de.qtyonhand) as qtyonhand,de.m_product_id from m_storage_detail de"
                + " join m_locator lo on lo.m_locator_id =de.m_locator_id where lo.m_warehouse_id='"
                + objRequest.getWarehouse().getId() + "'"
                + " group by m_product_id ) sd on sd.m_product_id=prd.m_product_id   "
                + " left join (select sum(requested_qty) as exqty, m_product_id from escm_material_reqln ln "
                + " join escm_material_request hr on hr.escm_material_request_id=ln.escm_material_request_id "
                + " where escm_material_reqln_id not in (select escm_material_reqln_id from escm_material_reqln "
                + " where escm_material_request_id='" + strRequisitionId
                + "') and hr.status='ESCM_IP' "
                + " group by m_product_id ) as exline on exline.m_product_id=prd.m_product_id "
                + " where (coalesce(sd.qtyonhand,0))< coalesce(delivered_qty,0) and ln.escm_material_request_id='"
                + strRequisitionId + "' ";
            ps = conn.prepareStatement(query);
            rs = ps.executeQuery();
            while (rs.next()) {
              errorFlag = true;
              MaterialIssueRequestLine line = OBDal.getInstance()
                  .get(MaterialIssueRequestLine.class, rs.getString("lnid"));
              line.setFailureReason("Available quantity is " + rs.getString("avstock"));
              OBDal.getInstance().save(line);
              OBDal.getInstance().flush();
            }
            if (errorFlag) {
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@ESCM_IssuedQty_Exceeds_Current_OnhandQty@");
              bundle.setResult(result);
              return;
            }
          }

          // check product stock exceeds and update failure reason for success
          query = "select (coalesce(sd.qtyonhand,0)) as avstock,requested_qty,ln.m_product_id as product,ln.escm_material_reqln_id as lnid from escm_material_reqln ln "
              + " left join m_product prd on prd.m_product_id=ln.m_product_id "
              + " left join (select sum(qtyonhand) as qtyonhand,m_product_id from m_storage_detail where ad_org_id='"
              + objRequest.getOrganization().getId() + "'"
              + " group by m_product_id ) sd on sd.m_product_id=prd.m_product_id   "
              + " left join (select sum(requested_qty) as exqty, m_product_id from escm_material_reqln ln "
              + " join escm_material_request hr on hr.escm_material_request_id=ln.escm_material_request_id "
              + " where escm_material_reqln_id not in (select escm_material_reqln_id from escm_material_reqln "
              + " where escm_material_request_id='" + strRequisitionId
              + "') and hr.status='ESCM_IP' "
              + " group by m_product_id ) as exline on exline.m_product_id=prd.m_product_id "
              + " where (coalesce(sd.qtyonhand,0))>= coalesce(delivered_qty,0)  and ln.escm_material_request_id='"
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
            comments, currentDate, vars, nextApproval, Lang, documentType);
        log.debug("count:" + count);
        boolean sequenceexists = false;

        if (count == 2 && (!objRequest.getWarehouse().getEscmWarehouseType().equals("RTW"))) {
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
              + " join escm_material_request req on req.escm_material_request_id=ln.escm_material_request_id and req.beneficiary_type <> 'SA' "
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
                ESCMProductCategoryV pCat = OBDal.getInstance().get(ESCMProductCategoryV.class,
                    objProduct.getProductCategory().getId());
                objCustody.setProductCategory(pCat);
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
                OBDal.getInstance().save(objCustody);
                // create Transaction
                Escm_custody_transaction objCustodyhistory = OBProvider.getInstance()
                    .get(Escm_custody_transaction.class);
                objCustodyhistory.setLineNo(Long.valueOf(10));
                objCustodyhistory.setDocumentNo(objRequest.getDocumentNo());
                objCustodyhistory.setOrganization(objCustody.getOrganization());
                objCustodyhistory.setBname(objRequest.getBeneficiaryIDName());
                objCustodyhistory.setBtype(objRequest.getBeneficiaryType());
                objCustodyhistory.setEscmMrequestCustody(objCustody);
                objCustodyhistory.setTransactionDate(objRequest.getTransactionDate());
                objCustodyhistory.setTransactiontype("IE");
                objCustodyhistory.setProcessed(true);
                objCustodyhistory.setLine2(Long.valueOf(10));
                objCustodyhistory.setDocumentNo(objRequest.getSpecNo());

                OBDal.getInstance().save(objCustodyhistory);
                existingDocNo = String.valueOf(Integer.parseInt(existingDocNo) + 1);

              }
            }
          }

          // ------------------------------------------------
          // final Approval Flow
          // entry in Transaction
          for (MaterialIssueRequestLine objRequestLine : objRequest.getEscmMaterialReqlnList()) {
            if (objRequestLine.getDeliveredQantity().compareTo(BigDecimal.ZERO) == 1) {
              MaterialTransaction trans = OBProvider.getInstance().get(MaterialTransaction.class);
              trans.setOrganization(objRequest.getOrganization());
              trans.setClient(objRequest.getClient());
              trans.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
              trans.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
              trans.setCreationDate(new java.util.Date());
              trans.setUpdated(new java.util.Date());
              trans.setMovementType("ESCM_MI");
              trans.setEscmTransactiontype("MIT");
              OBQuery<Locator> locator = OBDal.getInstance().createQuery(Locator.class,
                  " as e where e.warehouse.id=:warehouseID and e.default='Y' ");
              locator.setNamedParameter("warehouseID", objRequest.getWarehouse().getId());
              locator.setMaxResult(1);
              if (locator.list().size() > 0) {
                trans.setStorageBin(locator.list().get(0));
                // log4j.debug("getStorageBin:" + trans.getStorageBin());
              } else
                trans.setStorageBin(null);
              Product mproduct = OBDal.getInstance().get(Product.class,
                  objRequestLine.getProduct().getId());
              trans.setProduct(mproduct);
              trans.setMovementDate(objRequest.getTransactionDate());
              /*
               * int chkqty = Utility.ChkStoragedetOnhandQtyNeg(mproduct.getId(), locator.list()
               * .get(0).getId(), objRequestLine.getDeliveredQantity(), objRequest.getClient()
               * .getId()); log.debug("chkqty:" + chkqty); if (chkqty == -1) { OBError result =
               * OBErrorBuilder.buildMessage(null, "error", "@ESCM_StorageDetail_QtyonHand@");
               * bundle.setResult(result); OBDal.getInstance().rollbackAndClose(); return; }
               */
              trans.setMovementQuantity(objRequestLine.getDeliveredQantity().negate());
              trans.setEscmMaterialReqln(objRequestLine);

              // trans.setGoodsShipmentLine(ret.getGoodsShipment());
              trans.setUOM(OBDal.getInstance().get(UOM.class, objRequestLine.getUOM().getId()));
              OBDal.getInstance().save(trans);
              // change custody details
              // into In use
              log.debug("lineSize:" + objRequestLine.getEscmMrequestCustodyList().size());
            }
          }

          OBDal.getInstance().flush();
        }

        else if (count == -2) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", errorMsg);
          bundle.setResult(result);
          return;
        }
        // issue of Return Transaction From MIR
        else if (count == 2) {
          if (objRequest.getSpecNo() == null) {
            String sequence = Utility.getSpecificationSequence(objRequest.getOrganization().getId(),
                "IRT");
            if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_NoSpecSequence@");
              bundle.setResult(result);
              return;
            } else {
              sequenceexists = Utility
                  .chkSpecificationSequence(objRequest.getOrganization().getId(), "IRT", sequence);
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

          for (MaterialIssueRequestLine objRequestLine : objRequest.getEscmMaterialReqlnList()) {
            MaterialTransaction trans = OBProvider.getInstance().get(MaterialTransaction.class);
            trans.setOrganization(objRequestLine.getOrganization());
            trans.setClient(objRequestLine.getClient());
            trans.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
            trans.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
            trans.setCreationDate(new java.util.Date());
            trans.setUpdated(new java.util.Date());
            trans.setMovementType("V-");

            OBQuery<Locator> locator = OBDal.getInstance().createQuery(Locator.class,
                " as e where e.warehouse.id=:warehouseID and e.default='Y' ");
            locator.setNamedParameter("warehouseID", objRequest.getWarehouse().getId());
            locator.setMaxResult(1);
            if (locator.list().size() > 0) {
              trans.setStorageBin(locator.list().get(0));
              // log4j.debug("getStorageBin:" + trans.getStorageBin());
            } else {
              errorFlag = true;
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Locator(Empty)@");
              bundle.setResult(result);
              return;
            }

            trans.setProduct(objRequestLine.getProduct());
            trans.setMovementDate(objRequest.getTransactionDate());

            int chkqty = Utility.ChkStoragedetOnhandQtyNeg(objRequestLine.getProduct().getId(),
                locator.list().get(0).getId(), objRequestLine.getDeliveredQantity(),
                objRequestLine.getClient().getId());
            log.debug("chkqty:" + chkqty);
            if (chkqty == -1) {
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@ESCM_StorageDetail_QtyonHand@");
              bundle.setResult(result);
              OBDal.getInstance().rollbackAndClose();
              return;
            }

            trans.setMovementQuantity(objRequestLine.getDeliveredQantity().negate());
            trans.setEscmTransactiontype("IRT");

            trans.setEscmMaterialReqln(objRequestLine);
            trans.setUOM(OBDal.getInstance().get(UOM.class, objRequestLine.getUOM().getId()));

            OBDal.getInstance().save(trans);

            // update custody transaction and custody detail based on inout line

            for (Escm_custody_transaction objCustodytran : objRequestLine
                .getEscmCustodyTransactionList()) {
              // update custody detail status
              MaterialIssueRequestCustody objCustody = objCustodytran.getEscmMrequestCustody();
              if (objRequest.getBeneficiaryType().equals("S")
                  || objRequest.getBeneficiaryType().equals("D")
                  || objRequest.getBeneficiaryType().equals("E")) {
                objCustody.setBeneficiaryType(objRequest.getBeneficiaryType());
                objCustody.setBeneficiaryIDName(objRequest.getBeneficiaryIDName());
              } else {
                objCustody.setBeneficiaryType(objRequest.getBeneficiaryType());
                objCustody.setBeneficiaryIDName(null);
                objCustody.setSalesDesc(objRequest.getSalesDesc());
              }
              if (objRequest.getBeneficiaryType().equals("S")
                  || objRequest.getBeneficiaryType().equals("D")
                  || objRequest.getBeneficiaryType().equals("E")) {
                objCustody.setAlertStatus("IU");
              } else if (objRequest.getBeneficiaryType().equals("SA")) {
                objCustody.setAlertStatus("SA");
              } else if (objRequest.getBeneficiaryType().equals("GD")) {
                objCustody.setAlertStatus("RW");
              } else {
                objCustody.setAlertStatus(objRequest.getEscmIssuereason());
              }
              objCustodytran.setTransactiontype("IRT");
              objCustodytran.setTransactionDate(objRequest.getTransactionDate());
              // task no : 4856
              objCustodytran.setDocumentNo(objRequest.getSpecNo());
              if (objRequest.getBeneficiaryType().equals("S")
                  || objRequest.getBeneficiaryType().equals("D")
                  || objRequest.getBeneficiaryType().equals("E")) {
                objCustodytran.setBtype(objRequest.getBeneficiaryType());
                objCustodytran.setBname(objRequest.getBeneficiaryIDName());
              } else {
                objCustodytran.setBtype(objRequest.getBeneficiaryType());
                objCustodytran.setBname(null);
                objCustodytran.setSalesDesc(objRequest.getSalesDesc());
              }

              query = " select escm_custody_transaction_id from escm_custody_transaction where "
                  + "  escm_custody_transaction_id not in (?) and escm_mrequest_custody_id = ? "
                  + " order by created desc limit 1";
              ps = conn.prepareStatement(query);
              ps.setString(1, objCustodytran.getId());
              ps.setString(2, objCustody.getId());
              log.debug("st query:" + ps);
              rs = ps.executeQuery();
              if (rs.next()) {
                Escm_custody_transaction updCustodytran = OBDal.getInstance().get(
                    Escm_custody_transaction.class, rs.getString("escm_custody_transaction_id"));
                updCustodytran.setReturnDate(objRequest.getTransactionDate());
                OBDal.getInstance().save(updCustodytran);
              }
              objCustodytran.setProcessed(true);
              OBDal.getInstance().save(objCustodytran);
            }
          }

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
      // delete the unused nextroles in eut_next_role table.
      OBDal.getInstance().flush();
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(), documentType);
      OBDal.getInstance().commitAndClose();
    } catch (OBException e) {
      log.debug("Exeception in Issue Request Submit", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exeception in Issue Request Submit:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
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
      Date currentDate, VariablesSecureApp vars, NextRoleByRuleVO parmaNextApproval, String Lang,
      String documentType) {
    String requistionId = null, pendingapproval = null;
    int count = 0;
    Boolean isDirectApproval = false;
    String alertRuleId = "", alertWindow = AlertWindow.IssueRequest, strRoleId = "";
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    User objCreater = objRequest.getCreatedBy();
    NextRoleByRuleVO nextApproval = parmaNextApproval;
    JSONObject fromUserandRoleJson = new JSONObject();
    String fromUser = userId;
    String fromRole = roleId;
    boolean isDummyRole = false;
    List<EutDocappDelegateln> delegationLnList = new ArrayList<EutDocappDelegateln>();

    try {
      OBContext.setAdminMode();

      // NextRoleByRuleVO nextApproval = NextRoleByRule.getNextRole(con, clientId, orgId,
      // roleId,userId, Resource.PURCHASE_REQUISITION, 0.00);
      EutNextRole nextRole = null;
      boolean isBackwardDelegation = false;
      HashMap<String, String> role = null;
      String qu_next_role_id = "";
      String delegatedFromRole = null;
      String delegatedToRole = null;
      isDirectApproval = isDirectApproval(objRequest.getId(), roleId);
      strRoleId = objRequest.getRole().getId();

      // get alert rule id - Task No:7618
      alertRuleId = AlertUtility.getAlertRule(clientId, alertWindow);

      /**
       * fetching from role and user based on delegater / forwarder/ direct approver
       **/
      if (objRequest.getEUTNextRole() != null) {
        fromUserandRoleJson = forwardDao.getFromuserAndFromRoleWhileApprove(
            objRequest.getEUTNextRole(), userId, roleId, clientId, orgId, documentType, isDummyRole,
            isDirectApproval);
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
        nextApproval = NextRoleByRule.getRequesterNextRole(OBDal.getInstance().getConnection(),
            clientId, orgId, fromRole, fromUser, documentType, strRoleId);
      } else {
        if (isDirectApproval) {
          nextApproval = NextRoleByRule.getRequesterNextRole(OBDal.getInstance().getConnection(),
              clientId, orgId, fromRole, fromUser, documentType, strRoleId);

          if (nextApproval != null && nextApproval.hasApproval()) {
            nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
            if (nextRole.getEutNextRoleLineList().size() > 0) {
              for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
                OBQuery<UserRoles> userRole = OBDal.getInstance().createQuery(UserRoles.class,
                    "role.id=:roleID");
                userRole.setNamedParameter("roleID", objNextRoleLine.getRole().getId());
                role = NextRoleByRule.getbackwardDelegatedFromAndToRoles(
                    OBDal.getInstance().getConnection(), clientId, orgId,
                    userRole.list().get(0).getUserContact().getId(), documentType, "");
                delegatedFromRole = role.get("FromUserRoleId");
                delegatedToRole = role.get("ToUserRoleId");
                isBackwardDelegation = NextRoleByRule.isBackwardDelegation(
                    OBDal.getInstance().getConnection(), clientId, orgId, delegatedFromRole,
                    delegatedToRole, fromUser, documentType, 0.00);
                if (isBackwardDelegation)
                  break;
              }
            }
          }
          // if Role doesnot have any user associated then this condition will execute and return
          // error
          else if (nextApproval != null && nextApproval.getErrorMsg() != null
              && nextApproval.getErrorMsg().contains("EUT_NOUser_ForRoles")) {
            errorMsg = OBMessageUtils.messageBD(nextApproval.getErrorMsg());
            errorMsg = errorMsg.replace("@", nextApproval.getRoleName());
            count = -2;
          }
          if (isBackwardDelegation) {
            nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
                orgId, delegatedFromRole, fromUser, documentType, 0.00);
          }
        } else

        {
          role = NextRoleByRule.getDelegatedFromAndToRoles(OBDal.getInstance().getConnection(),
              clientId, orgId, fromUser, documentType, qu_next_role_id);

          delegatedFromRole = role.get("FromUserRoleId");
          delegatedToRole = role.get("ToUserRoleId");

          if (delegatedFromRole != null && delegatedToRole != null)
            nextApproval = NextRoleByRule.getRequesterDelegatedNextRole(
                OBDal.getInstance().getConnection(), clientId, orgId, delegatedFromRole,
                delegatedToRole, fromUser, documentType, objRequest.getRole().getId());
        }
      }

      if (nextApproval != null && nextApproval.hasApproval()) {
        ArrayList<String> includeRecipient = new ArrayList<String>();
        nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());

        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(objRequest.getEUTNextRole(), documentType);

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

          String Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.mir.wfa", Lang)
              + " " + objCreater.getName();

          forwardDao.getAlertForForwardedUser(objRequest.getId(), alertWindow, alertRuleId, objUser,
              clientId, Constants.APPROVE, objRequest.getDocumentNo(), Lang, vars.getRole(),
              objRequest.getEUTForward(), documentType, alertReceiversMap);
          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            AlertUtility.alertInsertionRole(objRequest.getId(), objRequest.getDocumentNo(),
                objNextRoleLine.getRole().getId(), "", objRequest.getClient().getId(), Description,
                "NEW", alertWindow, "scm.mir.wfa", Constants.GENERIC_TEMPLATE);
            // get user name for delegated user to insert on approval history.
            OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance().createQuery(
                EutDocappDelegateln.class,
                " as e left join e.eUTDocappDelegate as hd where hd.role.id =:roleID "
                    + " and hd.fromDate <=:currentdate and hd.date >=:currentdate "
                    + " and e.documentType= :documentType");
            delegationln.setNamedParameter("roleID", objNextRoleLine.getRole().getId());
            delegationln.setNamedParameter("currentdate", currentDate);
            delegationln.setNamedParameter("documentType", documentType);
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
        objRequest.setEscmAction("AP");
        if (pendingapproval == null)
          pendingapproval = nextApproval.getStatus();

        log.debug(
            "doc sts:" + objRequest.getAlertStatus() + "action:" + objRequest.getEscmAction());
        count = 1; // Waiting
                   // For
                   // Approval
                   // flow

      }
      // if Role doesnot have any user associated then this condition will execute and return
      // error
      else if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains("EUT_NOUser_ForRoles")) {
        errorMsg = OBMessageUtils.messageBD(nextApproval.getErrorMsg());
        errorMsg = errorMsg.replace("@", nextApproval.getRoleName());
        count = -2;
      } else {
        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(objRequest.getEUTNextRole(), documentType);

        ArrayList<String> includeRecipient = new ArrayList<String>();
        objRequest.setUpdated(new java.util.Date());
        objRequest.setUpdatedBy(OBContext.getOBContext().getUser());
        objRequest.setAlertStatus("ESCM_TR");
        Role objCreatedRole = null;
        if (objRequest.getWarehouse() == null) {
          // check Warehouse is empty
          OBDal.getInstance().rollbackAndClose();
          errorMsg = OBMessageUtils.messageBD("ESCM_WarehouseEmpty");
          count = -2;
          return count;
        }
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
        if (objCreatedRole != null)
          includeRecipient.add(objCreatedRole.getId());
        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        } // set alert for requester
        String Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.mir.approved", Lang)
            + " " + objUser.getName();
        AlertUtility.alertInsertionRole(objRequest.getId(), objRequest.getDocumentNo(),
            objRequest.getRole().getId(), objRequest.getCreatedBy().getId(),
            objRequest.getClient().getId(), Description, "NEW", alertWindow, "scm.mir.approved",
            Constants.GENERIC_TEMPLATE);
        objRequest.setEUTNextRole(null);
        objRequest.setEscmAction("PD");

        OBDal.getInstance().save(objRequest);
        OBDal.getInstance().flush();

        forwardDao.getAlertForForwardedUser(objRequest.getId(), alertWindow, alertRuleId, objUser,
            clientId, Constants.APPROVE, objRequest.getDocumentNo(), Lang, vars.getRole(),
            objRequest.getEUTForward(), documentType, alertReceiversMap);
        // delete the unused nextroles in eut_next_role table.
        DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(), documentType);
        for (MaterialIssueRequestLine line : objRequest.getEscmMaterialReqlnList()) {
          if (line.isGeneric() && !line.isCopy()) {
            BigDecimal sumOfPending = line.getDeliveredQantity();
            for (MaterialIssueRequestLine nestedLine : objRequest.getEscmMaterialReqlnList()) {
              if ((line.getGenericProduct() == nestedLine.getGenericProduct())
                  && !(line.getId() == nestedLine.getId())) {
                sumOfPending = sumOfPending.add(nestedLine.getDeliveredQantity());
              }
            }
            line.setPendingQty(line.getRequestedQty().subtract(sumOfPending));
          } else {
            line.setPendingQty(line.getRequestedQty().subtract(line.getDeliveredQantity()));
          }
          OBDal.getInstance().save(line);
        }
        count = 2; // Final Approval Flow
      }

      OBDal.getInstance().save(objRequest);
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
      // after approved by forwarded user removing the forward and rmi id
      if (objRequest.getEUTForward() != null) {
        // set status as "Draft" for forward record
        forwardDao.setForwardStatusAsDraft(objRequest.getEUTForward());
        // set forward_rmi id as null in record
        objRequest.setEUTForward(null);
      }
      if (objRequest.getEUTReqmoreinfo() != null) {
        // set status as "Draft" for forward record
        forwardDao.setForwardStatusAsDraft(objRequest.getEUTReqmoreinfo());
        // set forward_rmi id as null in record
        objRequest.setEUTReqmoreinfo(null);
        objRequest.setRequestMoreInformation("N");
      }

    } catch (Exception e) {
      log.error("Exception in updateHeaderStatus in IssueRequest: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  @SuppressWarnings("unused")
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

      if (query != null) {
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