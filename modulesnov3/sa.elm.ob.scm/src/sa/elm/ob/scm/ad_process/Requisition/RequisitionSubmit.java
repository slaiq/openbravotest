package sa.elm.ob.scm.ad_process.Requisition;

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
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.finance.ad_process.purchaseRequisition.RequisitionfundsCheck;
import sa.elm.ob.scm.ESCMProductContCatg;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutDocappDelegateln;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Gopalakrishnan on 13/02/2017
 */

public class RequisitionSubmit extends DalBaseProcess {

  /**
   * This servlet class was responsible for Requisition Submission Process with Approval
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(RequisitionSubmit.class);
  private final OBError obError = new OBError();
  private static String errorMsg = null;
  private String currentRoleId = null;
  private static String documentType = "";
  private static String requisitionWindowId = "800092";
  HttpServletRequest request = RequestContext.get().getRequest();
  VariablesSecureApp vars = new VariablesSecureApp(request);
  String Lang = vars.getLanguage();
  ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();

  @SuppressWarnings({ "resource", "unchecked" })
  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    Connection conn1 = OBDal.getInstance().getConnection();
    ConnectionProvider conn = bundle.getConnection();
    String appstatus = "";
    boolean errorFlag = false, chkSubRolIsInFstRolofDR = false;
    boolean allowUpdate = false;
    PreparedStatement st = null;
    ResultSet rs1 = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    boolean isDummyRole = false;
    boolean checkEncumbranceAmountZero = false;
    List<EfinBudgetManencumlines> encumLinelist = new ArrayList<EfinBudgetManencumlines>();
    log.debug("entering into Requisition Submit");
    try {
      OBContext.setAdminMode();
      String strRequisitionId = (String) bundle.getParams().get("M_Requisition_ID");
      Requisition objRequisition = OBDal.getInstance().get(Requisition.class, strRequisitionId);
      String DocStatus = objRequisition.getEscmDocStatus();
      String DocAction = objRequisition.getEscmDocaction(), p_instance_id = null, sql = "";
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = objRequisition.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      String roleId = (String) bundle.getContext().getRole();
      currentRoleId = (String) bundle.getContext().getRole();

      String preferenceValue = "";
      Date currentDate = new Date();
      Date transactionDate = objRequisition.getEscmTransactionDate();
      String comments = (String) bundle.getParams().get("notes").toString();
      boolean isPeriodOpen = false;

      Boolean allowDelegation = false, chkRoleIsInDocRul = false;
      int count = 0;

      // Checking contract category is empty
      if (DocStatus.equals("DR") || DocStatus.equals("ESCM_RJD")) {
        if (objRequisition.getEscmContactType() == null) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_ContractCatgCantBeEmpty@");
          bundle.setResult(result);
          return;
        }
      }

      // Attachment is mandatory if preference added
      String preferenceAttachValue = UtilityDAO.getAttachmentPref(Constants.PURCHASE_REQUISITION_W,
          objRequisition.getClient().getId());

      if (preferenceAttachValue != null && preferenceAttachValue.equals("Y")) {
        // attachment is mandatory
        OBQuery<Attachment> file = OBDal.getInstance().createQuery(Attachment.class,
            " as e where e.record=:Record");
        file.setNamedParameter("Record", strRequisitionId);
        if (file != null && file.list().size() == 0) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@EUT_Attachment_Mandatory@");
          bundle.setResult(result);
          return;
        }
      }

      // Budget Definition closed validation
      if (objRequisition.getEfinBudgetint().getStatus().equals("CL")) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_Budget_Definition_Closed@");
        bundle.setResult(result);
        return;

      }
      // Budget Definition Pre closed validation
      if (objRequisition.getEfinBudgetint().isPreclose()) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_PreClose_Year_Validation@");
        bundle.setResult(result);
        return;

      }

      // check pr encumbrance type is enable or not .. Task No.5925
      OBQuery<EfinEncControl> encumcontrol = OBDal.getInstance().createQuery(EfinEncControl.class,
          " as e where e.encumbranceType='PRE' and e.client.id=:clientID and "
              + " e.active='Y' and e.typeList =:processType ");
      encumcontrol.setNamedParameter("clientID", objRequisition.getClient().getId());
      encumcontrol.setNamedParameter("processType", objRequisition.getEscmProcesstype());
      encumcontrol.setFilterOnActive(true);
      encumcontrol.setMaxResult(1);
      // End Task No.5925

      // if next role has dummy role , then current role may not be available in document role
      if (objRequisition.getEutNextRole() != null) {
        List<EutNextRoleLine> eutNextRoleLine = objRequisition.getEutNextRole()
            .getEutNextRoleLineList();
        if (eutNextRoleLine != null && eutNextRoleLine.size() > 0) {
          roleId = (objRequisition.getEutNextRole().getEutNextRoleLineList().get(0)
              .getDummyRole() == null ? roleId
                  : objRequisition.getEutNextRole().getEutNextRoleLineList().get(0).getDummyRole());
          if (objRequisition.getEutNextRole().getEutNextRoleLineList().get(0)
              .getDummyRole() != null) {
            isDummyRole = true;
          }
        }
      }

      // assign document rule
      if (objRequisition.getEscmProcesstype() != null
          && objRequisition.getEscmProcesstype().equals("DP")) {
        documentType = Resource.PURCHASE_REQUISITION;
      } else {
        documentType = Resource.PURCHASE_REQUISITION_LIMITED;
      }
      // check lines to submit
      if (objRequisition.getProcurementRequisitionLineList().size() == 0) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_No_Requisition_Lines@");
        bundle.setResult(result);
        return;
      }

      // Check transaction period is opened or not
      if (DocAction.equals("CO")) {
        isPeriodOpen = Utility.checkOpenPeriodCore(transactionDate, orgId,
            Constants.PURCHASE_REQUISIION_DOC, null);
        if (!isPeriodOpen) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
          bundle.setResult(result);
          return;
        }
      }
      // Check whether the product belongs to that contract category or not
      if (objRequisition.getEscmContactType() != null) {
        String contCatgId = objRequisition.getEscmContactType().getId();
        for (RequisitionLine line : objRequisition.getProcurementRequisitionLineList()) {
          if (line.getProduct() != null && line.getProduct().getESCMPRODCONTCATGList() != null
              && line.getProduct().getESCMPRODCONTCATGList().size() != 0) {
            boolean contCatgMatch = true;
            for (ESCMProductContCatg pContCatg : line.getProduct().getESCMPRODCONTCATGList()) {
              if (pContCatg.getContractCategory().getId().equals(contCatgId)) {
                contCatgMatch = true;
                break;
              } else {
                if (contCatgId.equals(Utility.getConCatTypeOther())) {
                  contCatgMatch = true;
                  break;
                } else {
                  contCatgMatch = false;
                }
              }
            }
            if (!contCatgMatch) {
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@ESCM_ItemMismatchWithContCatg@");
              bundle.setResult(result);
              return;
            }
          }
        }
      }
      Boolean allowApprove = false;

      if (objRequisition.getEutForward() != null) {
        allowApprove = forwardDao.allowApproveReject(objRequisition.getEutForward(), userId,
            currentRoleId, documentType);
      }
      if (objRequisition.getEutReqmoreinfo() != null
          || ((objRequisition.getEutForward() != null) && (!allowApprove))) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      // UniqueCode Changes... task no. 6007
      if (DocStatus.equals("DR")) {
        SQLQuery Query = OBDal.getInstance().getSession()
            .createSQLQuery("select  COUNT(Distinct(em_efin_c_validcombination_id)) as count,"
                + "em_efin_c_validcombination_id from m_requisitionline where m_requisition_id=:reqID "
                + " and em_escm_issummary ='N' group by em_efin_c_validcombination_id ");
        Query.setParameter("reqID", objRequisition.getId());
        @SuppressWarnings("rawtypes")
        List reqlinelist = Query.list();
        // if all line uniquecode is same
        if (Query != null && reqlinelist.size() == 1) {
          Object[] reqline = (Object[]) reqlinelist.get(0);
          if (reqline != null && reqline[1] != null) {
            String uniqueCode = reqline[1].toString();
            // if all line uniquecode is same but not same as header unique code then update header
            // uniquecode with line uniquecode value
            if (objRequisition.getEFINUniqueCode() != null) {
              if (!uniqueCode.equals(objRequisition.getEFINUniqueCode().getId())) {
                AccountingCombination acct = OBDal.getInstance().get(AccountingCombination.class,
                    uniqueCode);
                objRequisition.setEFINUniqueCode(acct);

              }
            } else {
              AccountingCombination acct = OBDal.getInstance().get(AccountingCombination.class,
                  uniqueCode);
              objRequisition.setEFINUniqueCode(acct);

            }
          }
        }
        // if all line uniquecode is not same then make uniquecode as null
        else if (Query != null && reqlinelist.size() != 1) {
          objRequisition.setEFINUniqueCode(null);

        }
      }

      // check current role associated with document rule for approval flow
      if (!DocStatus.equals("DR") && !DocStatus.equals("ESCM_RJD")) {
        if (objRequisition.getEutNextRole() != null) {
          java.util.List<EutNextRoleLine> li = objRequisition.getEutNextRole()
              .getEutNextRoleLineList();
          for (int i = 0; i < li.size(); i++) {
            String role = li.get(i).getRole().getId();
            Role objRole = OBDal.getInstance().get(Role.class, currentRoleId);
            if (objRole.isEscmIsspecializeddept() != null && objRole.isEscmIsspecializeddept()) {
              NextRoleByRuleDAO nxtRoleDao = new NextRoleByRuleDAO();
              List<String> roles = nxtRoleDao.getRolebasedOnPreference(objRequisition);
              for (String preRole : roles) {
                if (preRole.equals(role)) {
                  allowUpdate = true;
                }
              }
            } else if (currentRoleId.equals(role)) {
              allowUpdate = true;
            }
          }
        }
        if (objRequisition.getEutNextRole() != null) {
          DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
          allowDelegation = delagationDao.checkDelegation(currentDate, currentRoleId, documentType);
          /*
           * sql = ""; Connection con = OBDal.getInstance().getConnection(); sql =
           * "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
           * + currentDate + "' and to_date >='" + currentDate +
           * "' and document_type='"+documentType+"'"; st = con.prepareStatement(sql); rs1 =
           * st.executeQuery(); while (rs1.next()) { String roleid = rs1.getString("ad_role_id"); if
           * (roleid.equals(currentRoleId)) { allowDelegation = true; break; } }
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
      if ((!vars.getUser().equals(objRequisition.getCreatedBy().getId()))
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
      if ((!vars.getUser().equals(objRequisition.getCreatedBy().getId()))
          && DocStatus.equals("DR")) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      // check role is present in document rule or not
      if (objRequisition.getEscmDocStatus().equals("DR")) {
        chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(OBDal.getInstance().getConnection(),
            clientId, orgId, userId, roleId, documentType, BigDecimal.ZERO);
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

      // chk atleast one supplier is present when processtype is Limited
      // if (DocStatus.equals("DR")) {
      if (objRequisition.getEscmProcesstype().equals("LB")) {
        if (!(objRequisition.getESCMPrsuppliersList().size() > 0)) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_pr_suppliersnotfound@");
          bundle.setResult(result);
          return;
        }
      }
      // }
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
      // Task No.5925
      if (encumcontrol.list().size() > 0 && !objRequisition.isEfinSkipencumbrance()) {
        // check all line uniquecode belongs to same encumbrance if manual encumbrance.
        if (!objRequisition.isEfinEncumbered()) {
          if (objRequisition.getEfinBudgetManencum() != null) {
            errorFlag = RequisitionDao.checkAllUniquecodesameEncum(objRequisition);
          }
          if (errorFlag) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_Unicode_Same_Encum@");
            bundle.setResult(result);
            return;
          }
        }

        // try {
        // preferenceValue = Preferences.getPreferenceValue("ESCM_BudgetControl", true,
        // vars.getClient(), objRequisition.getOrganization().getId(), vars.getUser(),
        // vars.getRole(), "800092");
        // } catch (PropertyException e) {
        // // do not catch anything.
        // preferenceValue = "N";
        // }

        /// check for Budget
        // Controller preference
        try {
          preferenceValue = sa.elm.ob.utility.util.Preferences.getPreferenceValue(
              "ESCM_BudgetControl", Boolean.TRUE, vars.getClient(),
              objRequisition.getOrganization().getId(), vars.getUser(), roleId, "800092", "N");
          preferenceValue = (preferenceValue == null) ? "N" : preferenceValue;

        } catch (PropertyException e) {
          preferenceValue = "N";
          // log.error("Exception in getting budget controller :", e);
        }

        if (!preferenceValue.equals("Y") && objRequisition.getEutForward() != null) {// check for
          // temporary
          // preference
          String requester_user_id = objRequisition.getEutForward().getUserContact().getId();
          String requester_role_id = objRequisition.getEutForward().getRole().getId();
          preferenceValue = forwardDao.checkAndReturnTemporaryPreference("ESCM_BudgetControl",
              roleId, vars.getUser(), vars.getClient(), objRequisition.getOrganization().getId(),
              "800092", requester_user_id, requester_role_id);
        }

        if (preferenceValue == null) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_Preference_value@");
          bundle.setResult(result);
          return;
        }
        // check preference is given by forward then restrict to give access while submit
        if ((DocStatus.equals("DR") || DocStatus.equals("ESCM_REJ"))
            && preferenceValue.equals("Y")) {
          List<Preference> prefs = forwardDao.getPreferences("ESCM_BudgetControl", true,
              vars.getClient(), objRequisition.getOrganization().getId(), vars.getUser(),
              vars.getRole(), requisitionWindowId, false, true, true);
          for (Preference preference : prefs) {
            if (preference.getEutForwardReqmoreinfo() != null) {
              preferenceValue = "N";
            }
          }
        }

        if (preferenceValue != null && preferenceValue.equals("Y")
            && !objRequisition.isEfinEncumbered()) {
          // encum mandatory for budget ctrl/reserved role
          /*
           * if (objRequisition.getEfinBudgetManencum() == null) { errorFlag = true;
           * OBDal.getInstance().rollbackAndClose(); OBError result =
           * OBErrorBuilder.buildMessage(null, "error", "@Escm_ManualencumNo@");//
           * ESCM_RoleIsNotIncInDocRule bundle.setResult(result); return; }
           */
          OBQuery<RequisitionLine> lines = OBDal.getInstance().createQuery(RequisitionLine.class,
              "requisition.id =:reqID and escmIssummary = 'N'");
          lines.setNamedParameter("reqID", objRequisition.getId());
          if (lines.list() != null && lines.list().size() > 0) {
            for (RequisitionLine RLines : lines.list()) {
              if (RLines.getEfinCValidcombination() == null) {
                errorFlag = true;
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@Efin_Req_Uniquecode_Mandatory@");// ESCM_RoleIsNotIncInDocRule
                bundle.setResult(result);
                return;
              }
              /*
               * if (RLines.getEscmAccountnoAmt() == null ||
               * RLines.getEscmAccountnoAmt().equals("")) { errorFlag = true;
               * OBDal.getInstance().rollbackAndClose(); OBError result =
               * OBErrorBuilder.buildMessage(null, "error", "@Escm_AccountNoAmt@");//
               * ESCM_RoleIsNotIncInDocRule bundle.setResult(result); return; }
               */
            }
          }

          // new validation with common method.
          JSONArray arraylist = new JSONArray(), linearraylist = null;
          JSONObject object = new JSONObject(), json = null, json1 = null;
          List<EfinBudgetManencumlines> encumLineList = null;
          List<RequisitionLine> reqLineList = new ArrayList<RequisitionLine>();
          OBQuery<RequisitionLine> ln = OBDal.getInstance().createQuery(RequisitionLine.class,
              " requisition.id=:reqID order by efinCValidcombination.id  ");
          ln.setNamedParameter("reqID", strRequisitionId);
          reqLineList = ln.list();
          if (reqLineList.size() > 0) {
            for (RequisitionLine reqLineobj : reqLineList) {
              if (reqLineobj.getEfinCValidcombination() != null) {
                if (json != null && json.has("Uniquecode") && json.getString("Uniquecode")
                    .equals(reqLineobj.getEfinCValidcombination().getId())) {
                  json.put("Amount",
                      new BigDecimal(json.getString("Amount")).add(reqLineobj.getLineNetAmount()));
                  json1 = new JSONObject();
                  json1.put("lineId", reqLineobj.getId());
                  linearraylist.put(json1);
                  json.put("lineList", linearraylist);
                } else {
                  if (json != null)
                    json.put("lineList", linearraylist);
                  linearraylist = new JSONArray();
                  json = new JSONObject();
                  json.put("Uniquecode", reqLineobj.getEfinCValidcombination().getId());
                  json.put("Amount", reqLineobj.getLineNetAmount());
                  json.put("isSummary", reqLineobj.isEscmIssummary());
                  json1 = new JSONObject();
                  json1.put("lineId", reqLineobj.getId());
                  linearraylist.put(json1);
                  arraylist.put(json);
                }
              }
              reqLineobj.setEscmCancelReason(null);
              OBDal.getInstance().save(reqLineobj);
            }
            json.put("lineList", linearraylist);
          }
          object.put("uniquecodeList", arraylist);
          // manual encum validation and update.
          if (objRequisition.getEfinBudgetManencum() != null) {
            EfinBudgetManencum manualEncum = OBDal.getInstance().get(EfinBudgetManencum.class,
                objRequisition.getEfinBudgetManencum().getId());
            encumLineList = manualEncum.getEfinBudgetManencumlinesList();
            errorFlag = RequisitionfundsCheck.manualEncumbranceValidation(encumLineList, object,
                "PR", false);
            if (errorFlag) {
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
              bundle.setResult(result);
              return;
            } else {
              // amount update
              OBInterceptor.setPreventUpdateInfoChange(true);
              RequisitionDao.updateManualEncumAmount(objRequisition, encumLineList);
              OBDal.getInstance().flush();
              OBInterceptor.setPreventUpdateInfoChange(false);

            }
          }
          // auto encum validation and update.
          else {
            // chk same dept.
            String qurey = "select distinct val.c_salesregion_id from m_requisitionline ln "
                + "join c_validcombination val on ln.em_efin_c_validcombination_id = val.c_validcombination_id "
                + "where m_requisition_id =:encId and ln.em_escm_issummary='N'";
            SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(qurey);
            sqlQuery.setParameter("encId", objRequisition.getId());
            if (sqlQuery.list() != null && sqlQuery.list().size() > 1) {
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Req_SameDept@");
              bundle.setResult(result);
              return;
            }
            // chk same budget type
            qurey = "select distinct val.c_campaign_id from m_requisitionline ln "
                + "join c_validcombination val on ln.em_efin_c_validcombination_id = val.c_validcombination_id "
                + "where m_requisition_id =:encId and ln.em_escm_issummary='N'";
            sqlQuery = OBDal.getInstance().getSession().createSQLQuery(qurey);
            sqlQuery.setParameter("encId", objRequisition.getId());
            if (sqlQuery.list() != null && sqlQuery.list().size() > 1) {
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Req_SameBType@");
              bundle.setResult(result);
              return;
            }
            // chk budget type validation for uniquecode-->cost means cost, funds means funds but
            // not
            // in cost.
            OBQuery<RequisitionLine> reqLine = OBDal.getInstance()
                .createQuery(RequisitionLine.class, "requisition.id=:reqID and escmIssummary='N'");
            reqLine.setNamedParameter("reqID", objRequisition.getId());
            if (reqLine.list() != null && reqLine.list().size() > 0) {
              Campaign bType = reqLine.list().get(0).getEfinCValidcombination().getSalesCampaign();
              if (bType.getEfinBudgettype().equals("F")) {
                errorFlag = RequisitionDao.checkFundsNoCostValidation(objRequisition);
                if (errorFlag) {
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@Efin_Chk_Line_Info@");
                  bundle.setResult(result);
                  return;
                }
              }
            }
            // funds validation.
            errorFlag = RequisitionfundsCheck.autoEncumbranceValidation(object,
                objRequisition.getEfinBudgetint(), "PR", false);
            if (errorFlag) {
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
              bundle.setResult(result);
              return;
            } else {
              // insert auto encumbrance.
              RequisitionDao.insertAutoEncumbrance(objRequisition);
              // after that should imapct in inquiry
              // Trigger changes RequisitionDao.updateAmtInEnquiry(objRequisition.getId());
            }
          }

          /*
           * // new validations // manual encumbrance if (objRequisition.getEfinBudgetManencum() !=
           * null) { // get encum line list List<EfinBudgetManencumlines> encumLinesList = null;
           * OBQuery<EfinBudgetManencumlines> encumLines = OBDal.getInstance().createQuery(
           * EfinBudgetManencumlines.class, " manualEncumbrance.id='" +
           * objRequisition.getEfinBudgetManencum().getId() + "'"); if (encumLines.list() != null &&
           * encumLines.list().size() > 0) { encumLinesList = encumLines.list(); } errorFlag =
           * RequisitionDao.checkManualEncumValidation(objRequisition, encumLinesList); if
           * (errorFlag) { OBError result = OBErrorBuilder.buildMessage(null, "error",
           * "@Efin_Chk_Line_Info@"); bundle.setResult(result); return; } else { // amount update
           * RequisitionDao.updateManualEncumAmount(objRequisition, encumLinesList); } } // auto
           * encumbrance else { // chk same dept. String qurey =
           * "select distinct val.c_salesregion_id from m_requisitionline ln " +
           * "join c_validcombination val on ln.em_efin_c_validcombination_id = val.c_validcombination_id "
           * + "where m_requisition_id =:encId and ln.em_escm_issummary='N'"; SQLQuery sqlQuery =
           * OBDal.getInstance().getSession().createSQLQuery(qurey); sqlQuery.setParameter("encId",
           * objRequisition.getId()); if (sqlQuery.list() != null && sqlQuery.list().size() > 1) {
           * OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Req_SameDept@");
           * bundle.setResult(result); return; } // chk same budget type qurey =
           * "select distinct val.c_campaign_id from m_requisitionline ln " +
           * "join c_validcombination val on ln.em_efin_c_validcombination_id = val.c_validcombination_id "
           * + "where m_requisition_id =:encId and ln.em_escm_issummary='N'"; sqlQuery =
           * OBDal.getInstance().getSession().createSQLQuery(qurey); sqlQuery.setParameter("encId",
           * objRequisition.getId()); if (sqlQuery.list() != null && sqlQuery.list().size() > 1) {
           * OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Req_SameBType@");
           * bundle.setResult(result); return; } // chk budget type validation for uniquecode-->cost
           * means cost, funds means funds but not // in cost.
           * 
           * OBQuery<RequisitionLine> reqLine =
           * OBDal.getInstance().createQuery(RequisitionLine.class, "requisition.id='" +
           * objRequisition.getId() + "' and escmIssummary='N'"); if (reqLine.list() != null &&
           * reqLine.list().size() > 0) { Campaign bType =
           * reqLine.list().get(0).getEfinCValidcombination().getSalesCampaign(); if
           * (bType.getEfinBudgettype().equals("F")) { errorFlag =
           * RequisitionDao.checkFundsNoCostValidation(objRequisition); if (errorFlag) { OBError
           * result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
           * bundle.setResult(result); return; } } }
           * 
           * // check funds avialble in inquiry once. count =
           * RequisitionDao.checkFundsAvailableInEnquiry(objRequisition.getId()); if (count > 0) {
           * OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
           * bundle.setResult(result); return; } // insert auto encumbrance.
           * RequisitionDao.insertAutoEncumbrance(objRequisition); // after that should imapct in
           * inquiry RequisitionDao.updateAmtInEnquiry(objRequisition.getId()); }
           */
        }
      }
      // End Task No.5925

      // check mandatory field for procurement Director
      String procurementDirector = "";
      try {
        procurementDirector = Preferences.getPreferenceValue("ESCM_ProcurementDirector", true,
            vars.getClient(), objRequisition.getOrganization().getId(), vars.getUser(), roleId,
            "800092");
      } catch (PropertyException e) {
        // Do not catch anything.
      }
      if (procurementDirector == null) {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_Preference_value@");
        bundle.setResult(result);
        return;
      }
      if (procurementDirector != null && procurementDirector.equals("Y")) {
        if (objRequisition.getEscmProcesstype() == null
            || objRequisition.getEscmProcesstype().equals("")) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_ProcessType@");// ESCM_RoleIsNotIncInDocRule
          bundle.setResult(result);
          return;
        }
      }

      // Check Encumbrance Amount is Zero Or Negative
      if (objRequisition.getEfinBudgetManencum() != null)
        encumLinelist = objRequisition.getEfinBudgetManencum().getEfinBudgetManencumlinesList();
      if (encumLinelist.size() > 0)
        checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

      if (checkEncumbranceAmountZero) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Encumamt_Neg@");
        bundle.setResult(result);
        return;
      }

      if (!errorFlag) {

        // If process type is direct, update specialized dept as NA
        if (objRequisition.getEscmProcesstype().equals("DP")) {
          if (objRequisition.getEscmSpecializeddept() == null) {
            objRequisition.setEscmSpecializeddept("NA");
            OBDal.getInstance().save(objRequisition);
          }
        }

        if ((DocStatus.equals("DR") || DocStatus.equals("ESCM_RJD")) && DocAction.equals("CO")) {
          appstatus = "SUB";
        } else if (DocStatus.equals("ESCM_IP") && DocAction.equals("AP")) {
          appstatus = "AP";
        }
        count = updateHeaderStatus(conn1, clientId, orgId, roleId, userId, objRequisition,
            appstatus, comments, currentDate, vars, bundle, preferenceValue, encumcontrol.list(),
            isDummyRole, Lang);
        log.debug("count:" + count);
        if (count == 3) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_No_LineManager@");
          bundle.setResult(result);
          return;
        } else if (count == 6) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_No_UserForRole@");
          bundle.setResult(result);
          return;
        } else if (count == 4) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_No_RoleForSpclDept@");
          bundle.setResult(result);
          return;
        } else if (count == 7) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_NoRoleDefineForUser@");
          bundle.setResult(result);
          return;
        } else if (count == 8) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_ReqDefMangDetail@");
          bundle.setResult(result);
          return;
        } else if (count == -2) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", errorMsg);
          bundle.setResult(result);
          return;
        }

        else if (count == 2) {
          if (objRequisition.getEscmSpecNo() == null) {
            String sequence = Utility
                .getProcessSpecificationSequence(objRequisition.getOrganization().getId(), "PR");
            if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
              OBDal.getInstance().rollbackAndClose();
              errorFlag = true;
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_NoSpecSequence@");
              bundle.setResult(result);
              return;
            } else {
              Boolean sequenceexists = Utility.chkSpecificationSequence(
                  objRequisition.getOrganization().getId(), "PR", sequence);
              if (!sequenceexists) {

                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@Escm_Duplicate_SpecNo@");
                bundle.setResult(result);
                return;
              }
              objRequisition.setEscmSpecNo(sequence);
            }
          }
          OBDal.getInstance().save(objRequisition);
          OBDal.getInstance().commitAndClose();
          log.debug("entering into instance Id:");
          p_instance_id = SequenceIdData.getUUID();
          String error = "", s = "";

          log.debug("p_instance_id:" + p_instance_id);
          sql = " INSERT INTO ad_pinstance (ad_pinstance_id, ad_process_id, record_id, isactive, ad_user_id, ad_client_id, ad_org_id, created, createdby, updated, updatedby,isprocessing)  "
              + "  VALUES ('" + p_instance_id + "', '1004400003','" + objRequisition.getId()
              + "', 'Y','" + userId + "','" + clientId + "','" + orgId + "', now(),'" + userId
              + "', now(),'" + userId + "','Y')";
          ps = conn.getPreparedStatement(sql);
          log.debug("ps:" + ps.toString());
          count = ps.executeUpdate();
          log.debug("count:" + count);

          String instanceqry = "select ad_pinstance_id from ad_pinstance where ad_pinstance_id=?";
          PreparedStatement pr = conn.getPreparedStatement(instanceqry);
          pr.setString(1, p_instance_id);
          ResultSet set = pr.executeQuery();

          if (set.next()) {

            sql = " select * from  m_requisition_post(?)";
            ps = conn.getPreparedStatement(sql);
            ps.setString(1, p_instance_id);
            // ps.setString(2, invoice.getId());
            ps.executeQuery();
            log.debug("count12:" + set.getString("ad_pinstance_id"));

            sql = " select result, errormsg from ad_pinstance where ad_pinstance_id='"
                + p_instance_id + "'";
            ps = conn.getPreparedStatement(sql);
            log.debug("ps12:" + ps.toString());
            rs = ps.executeQuery();
            if (rs.next()) {
              log.debug("result:" + rs.getString("result"));

              if (rs.getString("result").equals("0")) {
                error = rs.getString("errormsg").replace("@ERROR=", "");
                log.debug("error:" + error);
                s = error;
                int start = s.indexOf("@");
                int end = s.lastIndexOf("@");

                if (log.isDebugEnabled()) {
                  log.debug("start:" + start);
                  log.debug("end:" + end);
                }

                if (end != 0) {
                  sql = " select  msgtext from ad_message where value ='"
                      + s.substring(start + 1, end) + "'";
                  ps = conn.getPreparedStatement(sql);
                  log.debug("ps12:" + ps.toString());
                  rs = ps.executeQuery();
                  if (rs.next()) {
                    if (rs.getString("msgtext") != null)
                      throw new OBException(error);
                  }
                }
              } else if (rs.getString("result").equals("1")) {

              }
            }
          }
        }

        else if (encumcontrol.list().size() > 0 && !objRequisition.isEfinSkipencumbrance()
            && count == 5) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_No_BudgetControl@");
          bundle.setResult(result);
          return;
        }

        if (count > 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Requisition_Submit@");
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
      OBDal.getInstance().save(objRequisition);
      OBDal.getInstance().flush();
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.PURCHASE_REQUISITION);

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while insertAutoEncumbrance: " + e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      bundle.setResult(result);
      return;
    } catch (Exception e) {
      log.debug("Exeception in Requisition Submit:" + e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBDal.getInstance().commitAndClose();
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (rs1 != null)
          rs1.close();
        if (st != null)
          st.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
        final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
            vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
        bundle.setResult(error);
      }
      OBContext.restorePreviousMode();
    }

  }

  @SuppressWarnings("hiding")
  private int updateHeaderStatus(Connection con, String clientId, String orgId, String roleId,
      String userId, Requisition objRequisition, String appstatus, String comments,
      Date currentDate, VariablesSecureApp vars, ProcessBundle bundle, String preferenceValue,
      List<EfinEncControl> enccontrollist, boolean isDummyRole, String Lang) {
    String requistionId = null, pendingapproval = null;
    int count = 0;
    Boolean isDirectApproval = false;
    String alertRuleId = "", alertWindow = AlertWindow.PurchaseRequisition;
    User objUser = Utility.getObject(User.class, vars.getUser());
    try {
      OBContext.setAdminMode(true);
      Organization agencyOrg = UtilityDAO.getAgencyOrg(clientId);

      // NextRoleByRuleVO nextApproval = NextRoleByRule.getNextRole(con, clientId, orgId,
      // roleId,userId, Resource.PURCHASE_REQUISITION, 0.00);
      NextRoleByRuleVO nextApproval = null;
      EutNextRole nextRole = null;
      boolean isBackwardDelegation = false;
      boolean hasUsersAssociatedForRoles = true;
      BigDecimal requsitionamt = BigDecimal.ZERO;
      HashMap<String, String> role = null;
      String qu_next_role_id = "";
      String delegatedFromRole = null;
      String delegatedToRole = null;
      List<String> roles = new ArrayList<String>();
      JSONObject fromUserandRoleJson = new JSONObject();
      String fromUser = userId;
      String fromRole = roleId;

      isDirectApproval = isDirectApproval(objRequisition.getId(), currentRoleId, bundle, vars);
      log.debug("chkDirectApprover" + isDirectApproval);

      // get alert rule id - Task No:7618
      alertRuleId = AlertUtility.getAlertRule(clientId, alertWindow);

      if (objRequisition.getProcurementRequisitionLineList().size() > 0) {
        for (RequisitionLine a : objRequisition.getProcurementRequisitionLineList()) {
          requsitionamt = requsitionamt.add(a.getLineNetAmount());
        }
      }

      /**
       * fetching from role and user based on delegater / forwarder/ direct approver
       **/
      if (objRequisition.getEutNextRole() != null) {
        fromUserandRoleJson = forwardDao.getFromuserAndFromRoleWhileApprove(
            objRequisition.getEutNextRole(), userId, roleId, clientId, orgId, documentType,
            isDummyRole, isDirectApproval);
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

      List<NextRoleByRuleVO> list = NextRoleByRule.getNextRequesterRoleList(con, clientId, orgId,
          fromRole, fromUser, documentType, objRequisition.getEscmAdRole().getId());
      if ((objRequisition.getEutNextRole() == null)) {

        if (agencyOrg != null
            && agencyOrg.getId().equals(objRequisition.getOrganization().getId())) {
          if (objRequisition.getEscmProcesstype().equals("DP")) {
            nextApproval = NextRoleByRule.getSpecializedDeptBasedNextRole(
                OBDal.getInstance().getConnection(), clientId, orgId,
                assignRole(list, objRequisition, currentRoleId, fromRole), fromUser, documentType,
                objRequisition.getEscmAdRole().getId(), objRequisition.getCreatedBy().getId(), true,
                objRequisition);
          } else {
            nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
                OBDal.getInstance().getConnection(), clientId, orgId, fromRole, fromUser,
                documentType, objRequisition.getEscmAdRole().getId(), fromUser, true,
                objRequisition.getEscmDocStatus());
          }
        } else {
          nextApproval = NextRoleByRule.getUserManagerBasedNextRole(
              OBDal.getInstance().getConnection(), clientId, orgId, fromRole, fromUser,
              documentType, objRequisition.getEscmAdRole().getId(), fromUser, true,
              objRequisition.getEscmDocStatus());
        }
      } else {
        if (isDirectApproval) {

          if (objRequisition.getEscmProcesstype().equals("DP")) {

            nextApproval = NextRoleByRule.getSpecializedDeptBasedNextRole(
                OBDal.getInstance().getConnection(), clientId, orgId,
                assignRole(list, objRequisition, currentRoleId, fromRole), fromUser, documentType,
                objRequisition.getEscmAdRole().getId(), objRequisition.getCreatedBy().getId(), true,
                objRequisition);
          } else {
            nextApproval = NextRoleByRule.getRequesterNextRole(OBDal.getInstance().getConnection(),
                clientId, orgId, fromRole, fromUser, documentType,
                objRequisition.getEscmAdRole().getId());
          }
          if (nextApproval != null && nextApproval.hasApproval()) {
            nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
            if (nextRole.getEutNextRoleLineList().size() > 0) {
              for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
                OBQuery<UserRoles> userRole = OBDal.getInstance().createQuery(UserRoles.class,
                    "role.id='" + objNextRoleLine.getRole().getId() + "'");
                if (userRole.list().size() > 0) {
                  role = NextRoleByRule.getbackwardDelegatedFromAndToRoles(
                      OBDal.getInstance().getConnection(), clientId, orgId,
                      userRole.list().get(0).getUserContact().getId(), documentType, "");
                  delegatedFromRole = role.get("FromUserRoleId");
                  delegatedToRole = role.get("ToUserRoleId");
                  isBackwardDelegation = NextRoleByRule.isBackwardDelegation(
                      OBDal.getInstance().getConnection(), clientId, orgId, delegatedFromRole,
                      delegatedToRole, fromUser, documentType, requsitionamt);
                  if (isBackwardDelegation)
                    break;
                } else {
                  hasUsersAssociatedForRoles = false;
                  break;
                }
              }
            }
          }
          if (isBackwardDelegation) {
            nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
                OBDal.getInstance().getConnection(), clientId, orgId, delegatedFromRole, fromUser,
                documentType, objRequisition.getEscmAdRole().getId(), fromUser, true,
                objRequisition.getEscmDocStatus());
          }

        } else {
          role = NextRoleByRule.getDelegatedFromAndToRoles(OBDal.getInstance().getConnection(),
              clientId, orgId, fromUser, documentType, qu_next_role_id);
          delegatedFromRole = role.get("FromUserRoleId");
          delegatedToRole = role.get("ToUserRoleId");
          Role objRole = OBDal.getInstance().get(Role.class, fromRole);
          if ((objRole.isEscmIsspecializeddept() != null && objRole.isEscmIsspecializeddept())) {
            delegatedFromRole = fromRole;
          }

          if (delegatedFromRole != null && delegatedToRole != null) {
            if (objRequisition.getEscmProcesstype().equals("DP")) {
              list = NextRoleByRule.getNextRequesterRoleList(con, clientId, orgId,
                  delegatedFromRole, fromUser, documentType,
                  objRequisition.getEscmAdRole().getId());
              nextApproval = NextRoleByRule.getSpecializedDeptBasedNextRole(
                  OBDal.getInstance().getConnection(), clientId, orgId,
                  assignRole(list, objRequisition, currentRoleId, delegatedFromRole), fromUser,
                  documentType, objRequisition.getEscmAdRole().getId(),
                  objRequisition.getCreatedBy().getId(), true, objRequisition);
            } else {
              nextApproval = NextRoleByRule.getRequesterDelegatedNextRole(
                  OBDal.getInstance().getConnection(), clientId, orgId, delegatedFromRole,
                  delegatedToRole, fromUser, documentType, objRequisition.getEscmAdRole().getId());
            }
          }
        }
      }
      if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().equals("NoManagerAssociatedWithRole")) {
        count = 3;
      } else if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().equals("ESCM_NoRoleAssociated")) {
        count = 4;
      } else if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains("EUT_NoRoleDefineForUser")) {
        count = 7;
      } else if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains("ESCM_ReqDefMangDetail")) {
        count = 8;
      }

      // if Role doesnt has any user associated then this condition will execute and return error
      else if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains("EUT_NOUser_ForRoles")) {
        errorMsg = OBMessageUtils.messageBD(nextApproval.getErrorMsg());
        errorMsg = errorMsg.replace("@", nextApproval.getRoleName());
        count = -2;
      } else if (!hasUsersAssociatedForRoles) {
        count = 6;
      } else if (nextApproval != null && nextApproval.hasApproval()) {
        ArrayList<String> includeRecipient = new ArrayList<String>();
        nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());

        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(objRequisition.getEutNextRole(), documentType);

        objRequisition.setUpdated(new java.util.Date());
        objRequisition.setUpdatedBy(OBContext.getOBContext().getUser());
        objRequisition.setEscmDocStatus("ESCM_IP");
        objRequisition.setEutNextRole(nextRole);
        // get alert recipient - Task No:7618
        List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);

        // set alerts for next roles
        if (nextRole.getEutNextRoleLineList().size() > 0) {

          // delete alert for approval alerts - Task No:7618
          AlertUtility.deleteAlerts(objRequisition.getId());

          forwardDao.getAlertForForwardedUser(objRequisition.getId(), alertWindow, alertRuleId,
              objUser, clientId, Constants.APPROVE,
              objRequisition.getDocumentNo() + "-" + objRequisition.getDescription(), Lang,
              vars.getRole(), objRequisition.getEutForward(), documentType, alertReceiversMap);
          String Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.pr.wfa",
              vars.getLanguage());
          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            OBQuery<UserRoles> userRole = OBDal.getInstance().createQuery(UserRoles.class,
                "role.id=:roleID");
            userRole.setNamedParameter("roleID", objNextRoleLine.getRole().getId());
            if (userRole.list().size() > 0) {
              AlertUtility.alertInsertionRole(objRequisition.getId(),
                  objRequisition.getDocumentNo() + "-" + objRequisition.getDescription(),
                  objNextRoleLine.getRole().getId(),
                  (objNextRoleLine.getUserContact() == null ? ""
                      : objNextRoleLine.getUserContact().getId()),
                  objRequisition.getClient().getId(), Description, "NEW", alertWindow, "scm.pr.wfa",
                  Constants.GENERIC_TEMPLATE);
              // get user name for delegated user to insert on approval history.

              // OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance().createQuery(
              // EutDocappDelegateln.class,
              // " as e left join e.eUTDocappDelegate as hd where hd.role.id =:roleID "
              // + "and hd.fromDate <=:currentdate and hd.date >=:currentdate and
              // e.documentType=:docType ");
              // delegationln.setNamedParameter("roleID", objNextRoleLine.getRole().getId());
              // delegationln.setNamedParameter("currentdate", currentDate);
              // delegationln.setNamedParameter("docType", documentType);

              /* Task #7742 */

              List<EutDocappDelegateln> delegationlnList = UtilityDAO
                  .getDelegation(objNextRoleLine.getRole().getId(), currentDate, documentType);

              if (delegationlnList.size() > 0) {
                for (EutDocappDelegateln obDocAppDelegation : delegationlnList) {
                  AlertUtility.alertInsertionRole(objRequisition.getId(),
                      objRequisition.getDocumentNo() + "-" + objRequisition.getDescription(),
                      obDocAppDelegation.getRole().getId(),
                      obDocAppDelegation.getUserContact().getId(),
                      objRequisition.getClient().getId(), Description, "NEW", alertWindow,
                      "scm.pr.wfa", Constants.GENERIC_TEMPLATE);
                  includeRecipient.add(obDocAppDelegation.getRole().getId());
                }
                // IF next document rule have only one role and the same delegated to one user
                // then display role name with user
                if (nextRole.getEutNextRoleLineList().size() == 1 && delegationlnList.size() == 1
                    && Utility
                        .getAssignedUserForRoles(
                            nextRole.getEutNextRoleLineList().get(0).getRole().getId())
                        .size() == 1) {
                  if (pendingapproval != null)
                    pendingapproval += objNextRoleLine.getRole().getName() + " ("
                        + OBMessageUtils.messageBD("ESCM_Delegated_From") + ")" + " / "
                        + delegationlnList.get(0).getRole().getName() + "-"
                        + delegationlnList.get(0).getUserContact().getName();
                  else
                    pendingapproval = String.format(Constants.sWAITINGFOR_S_APPROVAL,
                        objNextRoleLine.getRole().getName() + " ("
                            + OBMessageUtils.messageBD("ESCM_Delegated_From") + ")" + " / "
                            + delegationlnList.get(0).getRole().getName() + "-"
                            + delegationlnList.get(0).getUserContact().getName());
                }
              }
              // add next role recipient
              includeRecipient.add(objNextRoleLine.getRole().getId());
            } else {
              hasUsersAssociatedForRoles = false;
              break;
            }
          }

          if (!hasUsersAssociatedForRoles) {
            count = 6;
          } else {
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

            objRequisition.setEscmDocaction("AP");
            if (pendingapproval == null)
              pendingapproval = nextApproval.getStatus();

            log.debug("doc sts:" + objRequisition.getEscmDocStatus() + "action:"
                + objRequisition.getEscmDocaction());
            count = 1; // Waiting For Approval flow
          }

        }

      } else {
        // in final approval should check budget controller already processed. else error.
        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(objRequisition.getEutNextRole(), documentType);

        if (enccontrollist.size() > 0 && !objRequisition.isEfinSkipencumbrance()
            && (preferenceValue == null || !preferenceValue.equals("Y"))) {
          if (!objRequisition.isEfinEncumbered()) {
            count = 5;
            return count;
          }
        }
        ArrayList<String> includeRecipient = new ArrayList<String>();
        objRequisition.setUpdated(new java.util.Date());
        objRequisition.setUpdatedBy(OBContext.getOBContext().getUser());
        objRequisition.setEscmDocStatus("ESCM_AP");
        OBQuery<RequisitionLine> lines = OBDal.getInstance().createQuery(RequisitionLine.class,
            "requisition.id=:reqID");
        lines.setNamedParameter("reqID", objRequisition.getId());
        for (RequisitionLine RLines : lines.list()) {
          RLines.setEscmStatus("ESCM_AP");
        }
        Role objCreatedRole = null;
        if (objRequisition.getCreatedBy().getADUserRolesList().size() > 0) {
          objCreatedRole = objRequisition.getEscmAdRole();
        }
        // delete alert for approval alerts - Task No:7618
        AlertUtility.deleteAlerts(objRequisition.getId());

        // get alert recipient - Task No:7618
        List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);

        // check and insert recipient
        if (alrtRecList.size() > 0) {
          for (AlertRecipient objAlertReceipient : alrtRecList) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }
        forwardDao.getAlertForForwardedUser(objRequisition.getId(), alertWindow, alertRuleId,
            objUser, clientId, Constants.APPROVE,
            objRequisition.getDocumentNo() + "-" + objRequisition.getDescription(), Lang,
            vars.getRole(), objRequisition.getEutForward(), documentType, alertReceiversMap);
        if (includeRecipient != null)
          includeRecipient.add(objCreatedRole.getId());

        // set alert based on preference created for ProcessType
        if (objRequisition.getEscmProcesstype() != null) {
          roles = getRolesBasedOnPreference(objRequisition.getEscmProcesstype());
        }
        for (String recipient : roles) {
          includeRecipient.add(recipient);
        }

        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        }

        // set alert for requester
        String Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.pr.approved",
            vars.getLanguage());
        AlertUtility.alertInsertionRole(objRequisition.getId(),
            objRequisition.getDocumentNo() + "-" + objRequisition.getDescription(),
            objRequisition.getEscmAdRole().getId(), objRequisition.getCreatedBy().getId(),
            objRequisition.getClient().getId(), Description, "NEW", alertWindow, "scm.pr.approved",
            Constants.GENERIC_TEMPLATE);

        for (String alertRole : roles) {
          AlertUtility.alertInsertionRole(objRequisition.getId(),
              objRequisition.getDocumentNo() + "-" + objRequisition.getDescription(), alertRole, "",
              objRequisition.getClient().getId(), Description, "NEW", alertWindow,
              "scm.pr.approved", Constants.GENERIC_TEMPLATE);
        }

        objRequisition.setEutNextRole(null);
        objRequisition.setEscmDocaction("PD");
        objRequisition.setEscmFinalapprover(userId);
        count = 2; // Final Approval Flow
        OBDal.getInstance().flush();
        // delete the unused nextroles in eut_next_role table.
        DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
            Resource.PURCHASE_REQUISITION);
        DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
            Resource.PURCHASE_REQUISITION_LIMITED);
      }

      // check current role exists in document rule ,if it is not there then delete Delete it
      // why ??? current user only already approved
      /* Bug 6969 - Delegated recipient is getting removed */
      /*
       * String checkQuery =
       * "as a join a.eutNextRole r join r.eutNextRoleLineList l where l.role.id = '" +
       * vars.getRole() + "' and a.escmDocStatus ='ESCM_IP'";
       * 
       * OBQuery<Requisition> checkRecipientQry = OBDal.getInstance().createQuery(Requisition.class,
       * checkQuery); if (checkRecipientQry.list().size() == 0) { OBQuery<AlertRecipient>
       * currentRoleQuery = OBDal.getInstance() .createQuery(AlertRecipient.class,
       * "as e where e.alertRule.id='" + alertRuleId + "' and e.role.id='" + vars.getRole() + "'");
       * if (currentRoleQuery.list().size() > 0) { for (AlertRecipient delObject :
       * currentRoleQuery.list()) { OBDal.getInstance().remove(delObject); } } }
       */

      // after approved by forwarded user
      if (objRequisition.getEutForward() != null) {
        // Remove Role Access to Receiver
        // forwardDao.removeRoleAccess(clientId, objRequisition.getEutForward().getId(), con);
        // set status as "Draft" for forward record
        forwardDao.setForwardStatusAsDraft(objRequisition.getEutForward());
        // set forward_rmi id as null in record
        objRequisition.setEutForward(null);
      }

      // removing rmi
      if (objRequisition.getEutReqmoreinfo() != null) {

        // Remove Role Access to Receiver
        // forwardDao.removeReqMoreInfoRoleAccess(clientId,
        // objRequisition.getEutReqmoreinfo().getId(),
        // con);
        // set status as "Draft" for forward record
        forwardDao.setForwardStatusAsDraft(objRequisition.getEutReqmoreinfo());
        // set forward_rmi id as null in record
        objRequisition.setEutReqmoreinfo(null);
        objRequisition.setEscmReqMoreInfo("N");
      }

      OBDal.getInstance().save(objRequisition);
      requistionId = objRequisition.getId();
      if (!StringUtils.isEmpty(requistionId)) {
        JSONObject historyData = new JSONObject();

        historyData.put("ClientId", clientId);
        historyData.put("OrgId", orgId);
        historyData.put("RoleId", currentRoleId);
        historyData.put("UserId", userId);
        historyData.put("HeaderId", requistionId);
        historyData.put("Comments", comments);
        historyData.put("Status", appstatus);
        historyData.put("NextApprover", pendingapproval);
        historyData.put("HistoryTable", ApprovalTables.REQUISITION_HISTORY);
        historyData.put("HeaderColumn", ApprovalTables.REQUISITION_HEADER_COLUMN);
        historyData.put("ActionColumn", ApprovalTables.REQUISITION_DOCACTION_COLUMN);

        Utility.InsertApprovalHistory(historyData);

        /*
         * insertRequistionApprover(OBDal.getInstance().getConnection(), clientId, orgId, roleId,
         * userId, requistionId, comments, appstatus, pendingapproval);
         */
      }

    } catch (Exception e) {
      log.error("Exception in updateHeaderStatus in Requisition: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      // OBContext.restorePreviousMode();
    }
    return count;
  }

  @SuppressWarnings("hiding")
  private boolean isDirectApproval(String RequsitionId, String roleId, ProcessBundle bundle,
      VariablesSecureApp vars) {

    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    try {
      query = "select count(*) from m_requisition req join eut_next_role rl on "
          + "req.em_eut_next_role_id = rl.eut_next_role_id "
          + "join eut_next_role_line li on li.eut_next_role_id = rl.eut_next_role_id "
          + "and req.m_requisition_id = ? and li.ad_role_id =?";

      ps = con.prepareStatement(query);
      ps.setString(1, RequsitionId);
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
        final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
            vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
        bundle.setResult(error);
      }
    }
  }

  /**
   * This method is to assign role based on different scenario
   * 
   * Scenario 1: If Redirected Role and Current Role is same , then assign role as current role
   * instead of specialized role
   * 
   * Scenario 2: If Specialized dept is NA then we should assign next role as specialized role to
   * skip Specialized role in document role
   * 
   * else assign default role
   * 
   * 
   * @param list
   * @param objRequisition
   * @param currentRoleId2
   * @param roleId
   * @return String(RoleId)
   */
  private String assignRole(List<NextRoleByRuleVO> list, Requisition objRequisition,
      String currentRoleId2, String roleId) {

    // Scenario-1
    if (!currentRoleId.equals(roleId)) {// && objRequisition.getEutForward() == null
      for (NextRoleByRuleVO checkVO : list) {
        if (checkVO.getNextRoleId().equals(currentRoleId)) {
          return currentRoleId;
        }
      }
    }
    // Scenario-2
    if (objRequisition.getEscmSpecializeddept().equals("NA")) {
      Role role = null;
      for (NextRoleByRuleVO checkVO : list) {
        role = OBDal.getInstance().get(Role.class, checkVO.getNextRoleId());
        if (role.isEscmIsspecializeddept() != null && role.isEscmIsspecializeddept()) {
          return role.getId();
        }

      }
    }
    return roleId;
  }

  /**
   * This method is used to get the role assigned for Purchase Requisition
   * 
   * Roles are assigned to process Type based on Preference(ESCM_DirectPO- Direct, ESCM_PublicPO-
   * Punlic Bid, ESCM_LimitedPO- Limited Bid)
   * 
   * @param Process
   *          Type of Requisition
   * @return roles as list
   */

  private List<String> getRolesBasedOnPreference(String escmProcesstype) {
    List<String> roles = new ArrayList<String>();
    String property = null;

    if (escmProcesstype.equals("DP")) {
      property = "ESCM_DirectPO";
    } else if (escmProcesstype.equals("LB")) {
      property = "ESCM_LimitedPO";
    } else {
      property = "ESCM_PublicPO";
    }

    final OBQuery<Preference> preference = OBDal.getInstance().createQuery(Preference.class,
        "client.id=:clientID and property=:prop and searchKey = 'Y' ");
    preference.setNamedParameter("clientID", OBContext.getOBContext().getCurrentClient().getId());
    preference.setNamedParameter("prop", property);

    List<Preference> prefList = preference.list();
    if (prefList.size() > 0) {
      roles = prefList.stream().filter(a -> a.getVisibleAtRole() != null)
          .map(a -> a.getVisibleAtRole().getId()).collect(Collectors.toList());
    }

    return roles;
  }
}
