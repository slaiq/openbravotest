package sa.elm.ob.scm.ad_process.ProposalManagement;

import java.math.BigDecimal;
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
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.OBInterceptor;
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
import org.openbravo.model.ad.utility.Attachment;
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
import sa.elm.ob.scm.ESCMProposalEvlEvent;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.Escmbidconfiguration;
import sa.elm.ob.scm.Escmbidmgmtline;
import sa.elm.ob.scm.Escmopenenvcommitee;
import sa.elm.ob.scm.actionHandler.dao.ProposalManagementDAO;
import sa.elm.ob.scm.actionHandler.dao.ProposalManagementDAOImpl;
import sa.elm.ob.scm.ad_process.BidManagement.dao.BidManagementDAO;
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
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.AttachmentProcessDao;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Priyanka Ranjan on 16/06/2017
 */

// Approval Flow of Proposal Management
public class ProposalManagementAction extends DalBaseProcess {
  private static final Logger log = LoggerFactory.getLogger(ProposalManagementAction.class);
  private static String errorMsg = null;
  private static final String REACTIVATE = "RE";
  private static final String SUBMIT_FOR_APPROVAL = "SA";
  public static final String PROPOSAL_WINDOW_ID = "CAF2D3EEF3B241018C8F65E8F877B29F";
  public static final String UNIFIEDTAB_ID = "31D9A3BF1C63488983C286C53A59196C";

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    boolean errorFlag = false, hasAgency = Boolean.TRUE;
    String proposalId = null;
    String proposalattrId = null;
    EscmProposalMgmt proposalmgmt = null;
    JSONObject resultfundsavail = null;
    Boolean chkDepflag = false, mixedencumbrance = false;
    JSONObject fromUserandRoleJson = new JSONObject();
    ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
    User objUser = Utility.getObject(User.class, vars.getUser());
    ProposalManagementProcessDAO proposalDAO = new ProposalManagementProcessDAOImpl();
    ProposalManagementDAO propDAO = new ProposalManagementDAOImpl();
    String preferenceValue = "N";
    String chkProposal = "";
    BigDecimal soucrRef_Qty = BigDecimal.ZERO;
    BigDecimal total_award_qty = BigDecimal.ZERO;
    String isbidAction = "N";
    boolean checkEncumbranceAmountZero = false;
    try {
      OBContext.setAdminMode();

      // Variable declaration
      if (bundle.getParams().get("Escm_Proposalmgmt_ID") != null) {
        proposalId = bundle.getParams().get("Escm_Proposalmgmt_ID").toString();
        proposalmgmt = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
      }

      if (bundle.getParams().get("isbidAction") != null
          && bundle.getParams().get("isbidAction") != "") {
        isbidAction = (String) bundle.getParams().get("isbidAction");
      }

      if (bundle.getParams().get("Escm_Proposal_Attr_ID") != null) {
        proposalattrId = bundle.getParams().get("Escm_Proposal_Attr_ID").toString();
        EscmProposalAttribute proposalAttr = OBDal.getInstance().get(EscmProposalAttribute.class,
            proposalattrId);
        proposalmgmt = proposalAttr.getEscmProposalmgmt();
        proposalId = proposalmgmt.getId();
      }

      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = proposalmgmt.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String docAction = proposalmgmt.getEscmDocaction();
      final String tabId = (String) bundle.getParams().get("tabId");

      String roleId = (String) bundle.getContext().getRole(), userID = "";
      User User = OBDal.getInstance().get(User.class, userId);
      Date currentDate = new Date();
      String agencyOrg = "";
      boolean isDirectProposal = proposalmgmt.getProposalType().equals("DR") ? true : false;
      String alertResource = isDirectProposal ? Resource.PROPOSAL_MANAGEMENT_DIRECT
          : Resource.PROPOSAL_MANAGEMENT;
      String fromUser = userId;
      String fromRole = roleId;
      boolean isDummyRole = false;
      boolean isPeriodOpen = true;
      List<Escmbidconfiguration> bidconfList = new ArrayList<Escmbidconfiguration>();
      List<EscmProposalsourceRef> srcrefList = new ArrayList<EscmProposalsourceRef>();
      List<EfinBudgetManencumlines> encumLnList = new ArrayList<EfinBudgetManencumlines>();
      List<EfinBudgetManencumlines> encumLinelist = new ArrayList<EfinBudgetManencumlines>();

      // Check approval is from unified proposal tab in proposal evaluation event and if so then
      // validate the awarded qty with bid qty
      if (UNIFIEDTAB_ID.equals(tabId) && proposalmgmt != null
          && proposalmgmt.getEscmBidmgmt() != null && proposalmgmt.isPartialAward()) {
        List<ESCMProposalEvlEvent> evntList = proposalmgmt.getEscmBidmgmt()
            .getESCMProposalEvlEventList();

        if (evntList.size() > 0) {
          if (evntList.get(0).isAwardfullqty()) {
            for (Escmbidmgmtline lines : proposalmgmt.getEscmBidmgmt().getEscmBidmgmtLineList()) {
              if (lines.getMovementQuantity().compareTo(lines.getAwardedqty()) != 0) {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@Escm_biqty_awardqty_notsame@");
                bundle.setResult(result);
                return;
              } else {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@Escm_appnotallowed_flagnotchecked@");
                bundle.setResult(result);
                return;
              }
            }
          }

        }
      }

      // attachment mandatory checking only for submit for approval and waiting for approval
      if ("SA".equals(proposalmgmt.getEscmDocaction())
          || proposalmgmt.getProposalappstatus().equals("INP")) {
        try {
          List<Preference> prefs = AttachmentProcessDao.getPreferences("EUT_Attachment_Process",
              true, proposalmgmt.getClient().getId(), null, null, null,
              Constants.PROPOSAL_MANAGEMENT_W, false, true, true);
          for (Preference preference : prefs) {
            if (preference.getSearchKey() != null && preference.getSearchKey().equals("Y")) {
              preferenceValue = "Y";
            }
          }

        } catch (PropertyException e) {
          preferenceValue = "N";
        } catch (Exception e) {
          // TODO Auto-generated catch block
        }
        if (preferenceValue != null && preferenceValue.equals("Y")
            || proposalmgmt.isSecondsupplier()) {
          // attachment is mandatory
          OBQuery<Attachment> file = OBDal.getInstance().createQuery(Attachment.class,
              " as e where e.record=:Record");
          file.setNamedParameter("Record", proposalId);
          if (file != null && file.list().size() == 0) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@EUT_Attachment_Mandatory@");
            bundle.setResult(result);
            return;
          }
        }
      }

      // Task no:8327 check if proposal have tax , if not then throw error
      if (proposalmgmt.getEscmDocaction().equals(SUBMIT_FOR_APPROVAL)) {
        if (!proposalmgmt.isTaxLine() || (proposalmgmt.isTaxLine()
            && proposalmgmt.getTotalTaxAmount().compareTo(BigDecimal.ZERO) == 0)) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_TEETaxMandatory@");
          bundle.setResult(result);
          return;
        }
      }

      // check proposal total amount is not zero
      if (proposalmgmt.getEscmDocaction().equals(SUBMIT_FOR_APPROVAL)
          && proposalmgmt.getTotalamount().compareTo(BigDecimal.ZERO) == 0) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Propamt_Zero@");
        bundle.setResult(result);
        return;
      }

      // Budget definition is closed validation
      if (proposalmgmt.getEfinBudgetinitial() != null
          && proposalmgmt.getEfinBudgetinitial().getStatus().equals("CL")) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_Budget_Definition_Closed@");
        bundle.setResult(result);
        return;
      }
      // Budget definition Pre Close Year validation
      if (proposalmgmt.getEfinBudgetinitial() != null
          && proposalmgmt.getEfinBudgetinitial().isPreclose()) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_PreClose_Year_Validation@");
        bundle.setResult(result);
        return;
      }

      if (isDirectProposal) {
        bidconfList = proposalDAO.getBidConfiguration(clientId, orgId);
        if (bidconfList.size() > 0) {
          Escmbidconfiguration typ = bidconfList.get(0);
          if (proposalmgmt.getTotalamount()
              .compareTo(BigDecimal.valueOf(typ.getAppmaxvalue())) > 0) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_PropTotalAmtCantExceed@");
            bundle.setResult(result);
            return;
          }
        }
        // check for references
      }
      // check Have Tax calculation in lines but its not associated in header. throw error 'Please
      // update the lines'
      if ("CO".equals(proposalmgmt.getEscmDocaction())) {
        if (!proposalmgmt.isTaxLine() && proposalmgmt.getTotalTaxAmount() != null
            && proposalmgmt.getTotalTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_TaxExists@");
          bundle.setResult(result);
          return;
        }
      }
      // Check tax cannot be zero
      if (proposalmgmt.isTaxLine()
          && proposalmgmt.getTotalTaxAmount().compareTo(BigDecimal.ZERO) == 0) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Taxcantbezero@");
        bundle.setResult(result);
        return;
      }
      // Check contract catg cannot be empty
      if (("CO".equals(proposalmgmt.getEscmDocaction())
          || ("SA".equals(proposalmgmt.getEscmDocaction()) && !proposalmgmt.isNeedEvaluation()))
          && proposalmgmt.getContractType() == null) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@ESCM_ContractCatgCantBeEmpty@");
        bundle.setResult(result);
        return;
      }
      // Check transaction period is opened or not before submitting record
      if ("CO".equals(proposalmgmt.getEscmDocaction())
          || (("SA").equals(proposalmgmt.getEscmDocaction()) && !proposalmgmt.isNeedEvaluation())) {
        isPeriodOpen = Utility.checkOpenPeriod(proposalmgmt.getSubmissiondate(),
            orgId.equals("0") ? vars.getOrg() : orgId, proposalmgmt.getClient().getId());
        if (!isPeriodOpen) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
          bundle.setResult(result);
          return;
        }
      }

      // Check whether the product belongs to that contract category or not
      if (proposalmgmt.getContractType() != null) {
        String contCatgId = proposalmgmt.getContractType().getId();
        for (EscmProposalmgmtLine line : proposalmgmt.getEscmProposalmgmtLineList()) {
          if (line.getProduct() != null) {
            if (line.getProduct().getESCMPRODCONTCATGList() != null
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
      }

      // get requisition line is already awarded
      // full quantity exceeds
      // awarding quantity ????
      if ("CO".equals(proposalmgmt.getEscmDocaction())) {
        for (EscmProposalmgmtLine obj_proposal_line : proposalmgmt.getEscmProposalmgmtLineList()) {
          for (EscmProposalsourceRef obj_sourceRef_line : obj_proposal_line
              .getEscmProposalsourceRefList()) {
            if (obj_sourceRef_line.getRequisitionLine() != null) {
              RequisitionLine objRequisition = obj_sourceRef_line.getRequisitionLine();
              Requisition obj_requisition = objRequisition.getRequisition();
              String str_docno = obj_requisition.getDocumentNo();
              soucrRef_Qty = obj_sourceRef_line.getReservedQuantity();
              total_award_qty = soucrRef_Qty.add(objRequisition.getEscmAwardedQty());
              if (total_award_qty.compareTo(objRequisition.getQuantity()) == 1) {
                String message = OBMessageUtils.messageBD("Escm_Award_Qty_NotAvailable")
                    .replace("@", str_docno).concat(",Requisition Line: ")
                    .concat(objRequisition.getLineNo().toString()).concat(" ,Requisition Quantity ")
                    .concat(objRequisition.getQuantity().toString());
                OBError result = OBErrorBuilder.buildMessage(null, "error", message);
                bundle.setResult(result);
                return;
              }
            }
          }
        }
      }

      // Check if PR quantity is greater than awarded quantity
      if ("SA".equals(proposalmgmt.getEscmDocaction()) && !proposalmgmt.isNeedEvaluation()) {
        for (EscmProposalmgmtLine obj_proposal_line : proposalmgmt.getEscmProposalmgmtLineList()) {
          for (EscmProposalsourceRef obj_sourceRef_line : obj_proposal_line
              .getEscmProposalsourceRefList()) {
            if (obj_sourceRef_line.getRequisitionLine() != null) {
              RequisitionLine objRequisitionLine = obj_sourceRef_line.getRequisitionLine();
              Requisition obj_requisition = objRequisitionLine.getRequisition();
              String str_docno = obj_requisition.getDocumentNo();
              soucrRef_Qty = obj_sourceRef_line.getReservedQuantity();
              total_award_qty = soucrRef_Qty.add(objRequisitionLine.getEscmAwardedQty());
              if (total_award_qty.compareTo(objRequisitionLine.getQuantity()) > 0) {
                String message = OBMessageUtils.messageBD("Escm_RequestedQtyGreater")
                    .replace("@", str_docno)
                    .replace("%", objRequisitionLine.getLineNo().toString());
                OBError result = OBErrorBuilder.buildMessage(null, "error", message);
                bundle.setResult(result);
                return;
              }
            }
          }
        }
      }

      // check bid proposals last day is not less than Proposal creation date
      // get the preference value
      if ("CO".equals(proposalmgmt.getEscmDocaction())
          || (("SA").equals(proposalmgmt.getEscmDocaction()) && !proposalmgmt.isNeedEvaluation())) {
        try {
          preferenceValue = Preferences.getPreferenceValue("Escm_Proposal_Creation", true,
              proposalmgmt.getClient().getId(), proposalmgmt.getOrganization().getId(),
              OBContext.getOBContext().getUser().getId(),
              OBContext.getOBContext().getRole().getId(), Constants.PROPOSAL_MANAGEMENT_W);
          if (preferenceValue == null) {
            preferenceValue = "N";
          }
        } catch (PropertyException e) {
          preferenceValue = "N";
        }
        if (proposalmgmt.getEscmBidmgmt() != null) {
          chkProposal = proposalDAO.getmaxbidproposallastdayandbidnumber(proposalmgmt);
          if (chkProposal != "Success"
              && (preferenceValue != null && preferenceValue.equals("N"))) {
            throw new OBException(chkProposal);
          }
        }
      }
      String DocStatus = proposalmgmt.getProposalappstatus(),
          ProposalStatus = proposalmgmt.getProposalstatus(),
          DocAction = proposalmgmt.getEscmDocaction(),
          documentType = isDirectProposal ? "EUT_122" : "EUT_117",
          comments = (String) bundle.getParams().get("notes").toString(), histStatus = null,
          alertRuleId = "", alertWindow = AlertWindow.ProposalManagement;
      String Lang = vars.getLanguage(),
          Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.pm.wfa", Lang) + " "
              + proposalmgmt.getCreatedBy().getName(),
          pendingapproval = null;
      String DescriptionApproved = sa.elm.ob.scm.properties.Resource.getProperty("scm.pm.approved",
          Lang) + " " + User.getName(), delegatedFromRole = null, delegatedToRole = null,
          qu_next_role_id = "";

      String budgetcontroller = "";
      NextRoleByRuleVO nextApproval = null;
      EutNextRole nextRole = null;
      Boolean fromPR = false, isAutoValid = false, isManualValid = false;
      JSONObject resultEncum = null;

      Boolean chkRoleIsInDocRul = false, headerUpdate = false, Status = false,
          allowDelegation = false, isDirectApproval = false, isBackwardDelegation = false,
          allowUpdate = false;
      // Boolean chkSubRolIsInFstRolofDR = false;
      ArrayList<String> includeRecipient = null;
      Role objCreatedRole = null;
      if (!proposalmgmt.getEscmDocaction().equals("RE")) {
        isDirectApproval = ProposalManagementActionMethod.isDirectApproval(proposalmgmt.getId(),
            roleId);
      }
      HashMap<String, String> role = null;
      EfinBudgetManencum encumbrance = null;

      Boolean allowApprove = false;
      if (proposalmgmt.getEUTForwardReqmoreinfo() != null) {
        allowApprove = forwardDao.allowApproveReject(proposalmgmt.getEUTForwardReqmoreinfo(),
            userId, roleId, documentType);
      }
      if (proposalmgmt.getEUTReqmoreinfo() != null
          || ((proposalmgmt.getEUTForwardReqmoreinfo() != null) && (!allowApprove))) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      // Task No. 7304
      if ((ProposalStatus.equals("DIS") || ProposalStatus.equals("CL")
          || ProposalStatus.equals("WD")) && DocAction.equals("SA")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }

      if (proposalmgmt.getEscmDocaction().equals(REACTIVATE)
          && proposalmgmt.getProposalstatus().equals("ANY")) {
        if (proposalmgmt.getEscmProposalAttrList() != null
            && proposalmgmt.getEscmProposalAttrList().size() > 0) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_Proposal_CantReactivate@");
          bundle.setResult(result);
          return;
        }
      }

      // Restrict to reactivate Proposal, once it is used in proposal evaluation event
      // or open envelope event
      if (proposalmgmt.getEscmDocaction().equals(REACTIVATE)
          || (proposalmgmt.getEscmDocaction().equals(SUBMIT_FOR_APPROVAL)
              && !proposalmgmt.getProposalstatus().equals("AWD")
              && !proposalmgmt.getProposalstatus().equals("PAWD")
              && proposalmgmt.isNeedEvaluation())) {
        if (!proposalmgmt.getProposalstatus().equals("SUB")) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }
      }
      if (proposalmgmt.getEscmDocaction().equals(REACTIVATE)
          && proposalmgmt.getProposalstatus().equals("SUB")) {
        if (proposalmgmt.getEscmProposalAttrList() != null
            && proposalmgmt.getEscmProposalAttrList().size() > 0) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_Proposal_CantReactivate@");
          bundle.setResult(result);
          return;
        }
      }
      /*
       * if ((!vars.getUser().equals(proposalmgmt.getCreatedBy().getId())) &&
       * (DocStatus.equals("INC") || DocStatus.equals("REJ"))) { errorFlag = true;
       * OBDal.getInstance().rollbackAndClose(); OBError result = OBErrorBuilder.buildMessage(null,
       * "error", "@Escm_AlreadyPreocessed_Approved@"); bundle.setResult(result); return; }
       */

      // Task No.5925
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      enccontrollist = ProposalManagementActionMethod.getEncControleList(proposalmgmt);

      // check lines are belongs to same department
      // if (proposalmgmt.getEscmBidmgmt() == null) {
      // /*
      // * Boolean isSameDept = ProposalManagementActionMethod.checkSameDept(proposalmgmt.getId());
      // * if (isSameDept) { errorFlag = true; OBError result = OBErrorBuilder.buildMessage(null,
      // * "error", "@ESCM_BidMultiDep@"); bundle.setResult(result); return; }
      // */
      // }

      // check quantity match only for requisition
      if (proposalmgmt.getEscmBidmgmt() == null) {
        if (proposalmgmt.getEscmProposalmgmtLineList().size() > 0) {
          List<String> lineNos = new ArrayList<String>();
          String lineno = null;
          for (EscmProposalmgmtLine line : proposalmgmt.getEscmProposalmgmtLineList()) {
            List<Object> rows = proposalDAO.getProposalLnTotalQty(line.getId());
            if (rows != null) {
              if (rows.size() > 0) {
                if (((BigDecimal) rows.get(0)) != null
                    && ((BigDecimal) rows.get(0)).compareTo((line.getMovementQuantity())) != 0) {
                  errorFlag = true;

                  lineNos.add(line.getLineNo().toString());
                  lineno = StringUtils.join(lineNos, ",");
                }
              }
            }
          }

          if (errorFlag) {
            OBDal.getInstance().rollbackAndClose();
            String message = OBMessageUtils.messageBD("ESCM_BidMgmLine_GrtSrcQty");
            message = message.replace("%", lineno);
            OBError result = OBErrorBuilder.buildMessage(null, "error", message);
            bundle.setResult(result);
            return;
          }
        }
      }
      if (!proposalmgmt.getEscmDocaction().equals("RE")) {
        for (EscmProposalmgmtLine line : proposalmgmt.getEscmProposalmgmtLineList()) {
          srcrefList = propDAO.checkLinesAddedFromPR(line.getId());
          if (srcrefList.size() > 0) {
            for (EscmProposalsourceRef srfRef : srcrefList) {
              if (srfRef.getRequisition() != null) {
                if (srfRef.getRequisition().getEscmDocStatus().equals("ESCM_CA")) {
                  errorFlag = true;
                  OBDal.getInstance().rollbackAndClose();
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@Escm_Proposal_PRCancelled@");
                  bundle.setResult(result);
                  return;
                }
              }
            }
          }
        }
      }
      if (proposalmgmt.getEscmDocaction().equals("SA")
          && proposalmgmt.getProposalstatus().equals("DR")
          && (DocStatus.equals("INC") || DocStatus.equals("REJ") || DocStatus.equals("REA"))) {
        proposalmgmt.setProposalstatus("AWD");
        OBDal.getInstance().save(proposalmgmt);
        OBDal.getInstance().flush();
        ProposalStatus = proposalmgmt.getProposalstatus();
      }
      if ((proposalmgmt.getProposalstatus().equals("AWD")
          || proposalmgmt.getProposalstatus().equals("PAWD")) && !proposalmgmt.isNeedEvaluation()
          && proposalmgmt.getEscmDocaction().equals("SA")
          && (proposalmgmt.getProposalappstatus().equals("INP")
              || proposalmgmt.getProposalappstatus().equals("INC")
              || proposalmgmt.getProposalappstatus().equals("REJ"))) {
        for (EscmProposalmgmtLine line : proposalmgmt.getEscmProposalmgmtLineList()) {
          srcrefList = propDAO.checkLinesAddedFromPR(line.getId());
          if (srcrefList.size() > 0) {
            for (EscmProposalsourceRef srfRef : srcrefList) {
              if (srfRef.getRequisition() != null) {
                if (!proposalmgmt.isNeedEvaluation()) {
                  RequisitionLine objRequisition = srfRef.getRequisitionLine();
                  Requisition obj_requisition = objRequisition.getRequisition();
                  String str_docno = obj_requisition.getDocumentNo();
                  soucrRef_Qty = srfRef.getReservedQuantity();
                  total_award_qty = soucrRef_Qty.add(objRequisition.getEscmAwardedQty());
                  objRequisition.setEscmAwardedQty(total_award_qty);
                  OBDal.getInstance().save(objRequisition);
                }
              }
            }
          }
        }
      }
      // after submit change status to submitted.
      // after reactivate change status to draft.
      if (proposalmgmt.getProposalstatus().equals("DR")
          || proposalmgmt.getProposalstatus().equals("SUB")
          || proposalmgmt.getProposalstatus().equals("ANY")) {
        if (proposalmgmt.getProposalstatus().equals("DR")) {
          // if there is no lines then should not allow to submit.
          if (proposalmgmt.getEscmProposalmgmtLineList().size() == 0) {
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_No_IR@");
            bundle.setResult(result);
            return;
          }

          mixedencumbrance = ProposalManagementActionMethod.checkmixedPREncumbrance(proposalmgmt);
          if (mixedencumbrance) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_BidPRMixedEncumbrance@");
            bundle.setResult(result);
            return;
          }

          Status = ProposalManagementActionMethod.updateproposalstatus("SUB", proposalmgmt);

        } else if ((proposalmgmt.getProposalstatus().equals("SUB")
            || proposalmgmt.getProposalstatus().equals("ANY"))) {
          if ((proposalmgmt.getEscmBidmgmt() != null
              && proposalmgmt.getEscmBidmgmt().getBidtype() != null
              && !proposalmgmt.getEscmBidmgmt().getBidtype().equals("DR")) && isbidAction != null
              && isbidAction.equals("N")) {

            List<Escmopenenvcommitee> openenvelop = proposalDAO
                .getOpenEnvCommitte(proposalmgmt.getEscmBidmgmt().getId());
            if (openenvelop.size() > 0) {
              errorFlag = true;
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@ESCM_ProCantRec_UseOEE@");
              bundle.setResult(result);
              return;
            }
          }
          if (!errorFlag)
            Status = ProposalManagementActionMethod.updateproposalstatus("DR", proposalmgmt);
        }
        if (isbidAction != null && isbidAction.equals("N"))
          OBDal.getInstance().flush();

        if (Status) {
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          return;
        } else {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ProcessFailed@");
          bundle.setResult(result);
          return;
        }
      }

      // check lines to submit for approval
      if (proposalmgmt.getEscmProposalmgmtLineList().size() == 0) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_No_Proposal_Lines@");
        bundle.setResult(result);
        return;
      }
      // check role is present in document rule or not
      if ((DocStatus.equals("INC")
          && (ProposalStatus.equals("AWD") || ProposalStatus.equals("PAWD")))
          || (DocStatus.equals("REA")
              && (ProposalStatus.equals("AWD") || ProposalStatus.equals("PAWD")))
          || (DocStatus.equals("REJ")
              && (ProposalStatus.equals("AWD") || ProposalStatus.equals("PAWD")))) {
        if (isDirectProposal) {
          chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(OBDal.getInstance().getConnection(),
              clientId, orgId, userId, roleId, alertResource, BigDecimal.ZERO);
        } else {
          chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(OBDal.getInstance().getConnection(),
              clientId, orgId, userId, roleId, alertResource, BigDecimal.ZERO);
        }
        if (!chkRoleIsInDocRul) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_RoleIsNotIncInDocRule@");
          bundle.setResult(result);
          return;
        }
      }
      if (!DocStatus.equals("INC") && !DocStatus.equals("REJ") && !DocStatus.equals("REA")) {
        if (proposalmgmt.getEUTNextRole() != null) {
          java.util.List<EutNextRoleLine> li = proposalmgmt.getEUTNextRole()
              .getEutNextRoleLineList();
          for (int i = 0; i < li.size(); i++) {
            String roleid = li.get(i).getRole().getId();
            if (roleId.equals(roleid)) {
              allowUpdate = true;
            }
          }

        }
        // get delegation role
        if (proposalmgmt.getEUTNextRole() != null) {
          DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
          allowDelegation = delagationDao.checkDelegation(currentDate, roleId, documentType);
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

      // check all line uniquecode belongs to same encumbrance if manual encumbrance.
      /*
       * if(enccontrollist.size()>0) { if (!proposalmgmt.isEfinIsbudgetcntlapp()) { errorFlag =
       * ProposalManagementActionMethod.checkAllUniquecodesameEncum(proposalmgmt); if (errorFlag) {
       * OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Unicode_Same_Encum@");
       * bundle.setResult(result); return; } } }
       */

      /*
       * // chk submitting role is in first role in document rule if (DocStatus.equals("INC")) {
       * chkSubRolIsInFstRolofDR = UtilityDAO.chkSubRolIsInFstRolofDR(OBDal.getInstance()
       * .getConnection(), clientId, orgId, userId, roleId, documentType, BigDecimal.ZERO);
       * log.debug("chkSubRolIsInFstRolofDR:" + chkSubRolIsInFstRolofDR); if
       * (!chkSubRolIsInFstRolofDR) { errorFlag = true; OBDal.getInstance().rollbackAndClose();
       * OBError result = OBErrorBuilder.buildMessage(null, "error",
       * "@Efin_Role_NotFundsReserve_submit@"); bundle.setResult(result); return; } }
       */

      if (((DocStatus.equals("INC") || DocStatus.equals("REJ"))
          && (ProposalStatus.equals("AWD") || ProposalStatus.equals("PAWD")))
          || (DocStatus.equals("REA")
              && (ProposalStatus.equals("AWD") || ProposalStatus.equals("PAWD")))) {
        histStatus = "SUB";
      } else if (DocStatus.equals("INP") && DocAction.equals("AP")) {
        histStatus = "AP";
      }
      // Task No.5768 check current role is Budget controller or not

      // try {
      // budgetcontroller = Preferences.getPreferenceValue("ESCM_BudgetControl", true,
      // vars.getClient(), proposalmgmt.getOrganization().getId(), vars.getUser(),
      // vars.getRole(), PROPOSAL_WINDOW_ID);
      // } catch (PropertyException e) {
      // budgetcontroller = "N";
      // }

      try {
        budgetcontroller = sa.elm.ob.utility.util.Preferences.getPreferenceValue(
            "ESCM_BudgetControl", Boolean.TRUE, vars.getClient(),
            proposalmgmt.getOrganization().getId(), vars.getUser(), vars.getRole(),
            PROPOSAL_WINDOW_ID, "N");
        budgetcontroller = (budgetcontroller == null) ? "N" : budgetcontroller;

      } catch (PropertyException e) {
        budgetcontroller = "N";
        // log.error("Exception in getting budget controller :", e);
      }

      if (!budgetcontroller.equals("Y") && proposalmgmt.getEUTForwardReqmoreinfo() != null) {
        // check for
        // temporary
        // preference
        String requester_user_id = proposalmgmt.getEUTForwardReqmoreinfo().getUserContact().getId();
        String requester_role_id = proposalmgmt.getEUTForwardReqmoreinfo().getRole().getId();
        budgetcontroller = forwardDao.checkAndReturnTemporaryPreference("ESCM_BudgetControl",
            vars.getRole(), vars.getUser(), vars.getClient(),
            proposalmgmt.getOrganization().getId(), PROPOSAL_WINDOW_ID, requester_user_id,
            requester_role_id);
      }
      //
      // if (budgetcontroller == null) {
      // OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_Preference_value@");
      // bundle.setResult(result);
      // return;
      // }
      // check preference is given by forward then restrict to give access while submit
      if (((ProposalStatus.equals("AWD") || ProposalStatus.equals("PAWD"))
          && (DocStatus.equals("REJ") || DocStatus.equals("INC")))
          && budgetcontroller.equals("Y")) {
        List<Preference> prefs = forwardDao.getPreferences("ESCM_BudgetControl", true,
            vars.getClient(), proposalmgmt.getOrganization().getId(), vars.getUser(),
            vars.getRole(), Constants.PROPOSAL_MANAGEMENT_W, false, true, true);
        for (Preference preference : prefs) {
          if (preference.getEutForwardReqmoreinfo() != null) {
            budgetcontroller = "N";
          }
        }
      }

      if (enccontrollist.size() > 0 && (budgetcontroller == null || budgetcontroller.equals("Y"))
          && !proposalmgmt.isEfinIsbudgetcntlapp()) {
        List<EscmProposalmgmtLine> lines = proposalDAO.getProposalLines(proposalmgmt.getId());
        if (budgetcontroller.equals("Y")) {
          if (proposalmgmt.getEFINEncumbranceMethod() == null) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_Req_Uniquecode_Mandatory@");// ESCM_RoleIsNotIncInDocRule
            bundle.setResult(result);
            return;
          }
          if (lines != null && lines.size() > 0) {
            String dept = null;
            EscmProposalmgmtLine ln = lines.get(0);
            dept = ln.getEFINUniqueCode() != null ? ln.getEFINUniqueCode().getSalesRegion().getId()
                : null;
            for (EscmProposalmgmtLine prosallin : lines) {
              if (prosallin.getEFINUniqueCode() == null) {
                errorFlag = true;
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@Efin_Req_Uniquecode_Mandatory@");// ESCM_RoleIsNotIncInDocRule
                bundle.setResult(result);
                return;
              } else if (prosallin.getEFINUniqueCode() != null
                  && !prosallin.getEFINUniqueCode().getSalesRegion().getId().equals(dept)) {
                errorFlag = true;
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@ESCM_AllLinesMustHaveSameDept@");// ESCM_RoleIsNotIncInDocRule
                bundle.setResult(result);
                return;
              }
            }
          }
        }
        // New Version Validation
        if (proposalmgmt.getEscmBaseproposal() != null && proposalmgmt.getEscmOldproposal() != null
            && proposalmgmt.getEscmOldproposal().getEfinEncumbrance() != null) {
          if (proposalmgmt.getEfinEncumbrance() != null
              && proposalmgmt.getEfinEncumbrance().getEncumMethod().equals("M")) {
            errorFlag = ProposalManagementActionMethod.chkNewVersionManualEncumbranceValidation(
                proposalmgmt, proposalmgmt.getEscmBaseproposal(), true, false, null);
            if (errorFlag) {
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
              bundle.setResult(result);
              return;
            } else {
              isManualValid = true;
            }
          } else {
            JSONObject object = ProposalManagementActionMethod.getUniquecodeListforProposalVerAuto(
                proposalmgmt, proposalmgmt.getEscmBaseproposal(), false, null);
            // funds validation.
            errorFlag = RequisitionfundsCheck.autoEncumbranceValidation(object,
                proposalmgmt.getEfinBudgetinitial(), "PM", false);
            if (errorFlag) {
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
              bundle.setResult(result);
              return;
            } else {
              isAutoValid = true;
            }
          }
        } else {
          for (EscmProposalmgmtLine line : proposalmgmt.getEscmProposalmgmtLineList()) {
            List<EscmProposalsourceRef> proposalsrcref = propDAO
                .checkLinesAddedFromPR(line.getId());
            if (proposalsrcref != null && proposalsrcref.size() > 0) {
              fromPR = true;
              break;
            }
          }
          if (!fromPR) {
            if (proposalmgmt.getEfinEncumbrance() != null
                && proposalmgmt.getEfinEncumbrance().getEncumType().equals("PAE")) {

              // check all line uniquecode belongs to same encumbrance if manual encumbrance.
              errorFlag = ProposalManagementActionMethod.checkAllUniquecodesameEncum(proposalmgmt);
              if (errorFlag) {
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@Efin_Unicode_Same_Encum@");
                bundle.setResult(result);
                return;
              }

              // manual encum validation and update.
              errorFlag = ProposalManagementActionMethod
                  .chkManualEncumbranceValidation(proposalmgmt, true);
              if (errorFlag) {
                OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
                bundle.setResult(result);
                return;
              }
            }
            // auto validation
            else if ((proposalmgmt.getEfinEncumbrance() == null
                && proposalmgmt.getEscmBidmgmt() == null)
                || (proposalmgmt.getEfinEncumbrance() == null
                    && proposalmgmt.getEscmBidmgmt() != null
                    && proposalmgmt.getEscmBidmgmt().getEncumbrance() == null)) {
              JSONObject errorresult = ProposalManagementActionMethod
                  .checkAutoEncumValidationForPR(proposalmgmt);
              if (errorresult.has("errorflag")) {
                if (errorresult.getString("errorflag").equals("0")) {
                  errorFlag = true;
                  OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                      errorresult.getString("errormsg"));
                  bundle.setResult(result1);
                  return;
                }
              }
            } else if (proposalmgmt.getEscmBidmgmt() != null
                && proposalmgmt.getEscmBidmgmt().getEncumbrance() != null) {

              // commented following lines due to goverance cost center logic in encumbrance

              // reqDepartment =
              // proposalmgmt.getEscmBidmgmt().getEncumbrance().getSalesRegion().getId();
              // OBQuery<EscmProposalmgmtLine> proposalLine = OBDal.getInstance().createQuery(
              // EscmProposalmgmtLine.class, "escmProposalmgmt.id='" + proposalmgmt.getId() + "'");
              // if (proposalLine.list() != null && proposalLine.list().size() > 0) {
              // for (EscmProposalmgmtLine prosallin : proposalLine.list()) {
              // if (!prosallin.isSummary()) {
              // proposalDepartment = prosallin.getEFINUniqueCode().getSalesRegion().getId();
              // if (!reqDepartment.equals(proposalDepartment)) {
              // String status = OBMessageUtils.messageBD("Escm_Encum_ProposalDep");
              // prosallin.setEfinFailureReason(status);
              // chkDepflag = true;
              // }
              // }
              // }
              // }

              if (chkDepflag) {
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@ESCM_ProcessFailed(Reason)@");
                bundle.setResult(result);
                return;

              }

              // encumbrance other than proposal encumbrance
              if (!proposalmgmt.getEscmBidmgmt().getEncumbrance().getEncumType().equals("PAE")) {

                if (proposalmgmt.getProposalstatus().equals("AWD")) {
                  errorFlag = ProposalManagementActionMethod.getBidtoProposalDetails(proposalmgmt,
                      true);

                  if (errorFlag) {
                    if (proposalmgmt.getEFINEncumbranceMethod().equals("A")) {
                      proposalmgmt.setEfinEncumbrance(null);
                    }
                    OBError result = OBErrorBuilder.buildMessage(null, "error",
                        "@Efin_Chk_Line_Info@");
                    bundle.setResult(result);
                    return;
                  }
                } else if (proposalmgmt.getProposalstatus().equals("PAWD")) {
                  // partial award
                  // Get proposal details
                  JSONObject result = UnifiedProposalActionMethod
                      .bidtoProposalEncumChanges(proposalmgmt, false);
                  OBError error1 = UnifiedProposalActionMethod.createSplitEncumbrance(proposalmgmt,
                      result, true);
                  if (error1.getType().equals("error")) {
                    bundle.setResult(error1);
                    return;
                  }
                }
              }
            }
          } else {
            // encumbrance validation
            resultEncum = ProposalManagementActionMethod.checkFullPRQtyUitlizeorNot(proposalmgmt);
            log.debug("resultEncum:" + resultEncum);
            if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                && resultEncum.getBoolean("isAssociatePREncumbrance")
                && resultEncum.has("isFullQtyUsed") && !resultEncum.getBoolean("isFullQtyUsed")) {
              encumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
                  resultEncum.getString("encumbrance"));
              // errorFlag = BidManagementDAO.chkFundsAvailforNewEncumbrance(null, encumbrance,
              // null,
              /// proposal);

              resultfundsavail = ProposalManagementActionMethod
                  .getUniqueCodeListforFundschk(proposalmgmt, encumbrance);
              errorFlag = resultfundsavail.getBoolean("errorflag");
              // errorFlag = true;
            } else {
              if (resultEncum.has("encumbrance")) {
                encumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
                    resultEncum.getString("encumbrance"));
                errorFlag = ProposalManagementActionMethod
                    .chkAndUpdateforProposalPRFullQty(proposalmgmt, encumbrance, true, false);
              } else {
                JSONObject errorresult = ProposalManagementActionMethod
                    .checkAutoEncumValidationForPR(proposalmgmt);
                log.debug("errorresult:" + errorresult);
                if (errorresult.has("errorflag")) {
                  if (errorresult.getString("errorflag").equals("0")) {
                    errorFlag = true;
                    OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                        errorresult.getString("errormsg"));
                    bundle.setResult(result1);
                    return;
                  }
                }
              }
            }
            if (errorFlag) {
              errorFlag = true;
              OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                  "@ESCM_ProcessFailed(Reason)@");
              bundle.setResult(result1);
              return;
            }
          }
        }

      }

      // Copy UniqueCode... task no. 6007
      // Copy UniqueCode validations only for budget controller role
      if (budgetcontroller != null && budgetcontroller.equals("Y")) {
        if (StringUtils.isNotEmpty(proposalmgmt.getId())) {
          ProposalManagementActionMethod.copyUniqueCodeValidation(proposalmgmt);
        }
      }

      // Task No.5768 manual encumbrance validation
      /*
       * if (proposalmgmt.getEfinEncumbrance() != null &&
       * proposalmgmt.getEfinEncumbrance().getEncumType().equals("PAE")) { errorFlag =
       * ProposalManagementActionMethod.chkManualEncumbranceValidation(proposalmgmt, true); if
       * (errorFlag) { OBError result = OBErrorBuilder.buildMessage(null, "error",
       * "@Efin_Chk_Line_Info@"); bundle.setResult(result); return; } }
       */

      // ------
      // Forward
      if ((proposalmgmt.getEUTNextRole() != null)) {

        fromUserandRoleJson = forwardDao.getFromuserAndFromRoleWhileApprove(
            proposalmgmt.getEUTNextRole(), userId, roleId, clientId, orgId, documentType,
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

      // ------

      // check next approver role is present or not
      if ((proposalmgmt.getEUTNextRole() == null)) {
        if (isDirectProposal) {
          // if next role is agency manager and agency organization is empty then skip the role

          List<NextRoleByRuleVO> list = NextRoleByRule.getNextRoleList(
              OBDal.getInstance().getConnection(), clientId, orgId, fromRole, fromUser,
              documentType, proposalmgmt.getTotalamount());

          if (proposalmgmt.getAgencyorg() == null) {
            hasAgency = Boolean.FALSE;
            fromRole = assignRole(list, fromRole);
          } else {
            agencyOrg = proposalmgmt.getAgencyorg().getCommercialName();
          }

          nextApproval = NextRoleByRule.getAgencyManagerBasedNextRole(
              OBDal.getInstance().getConnection(), clientId, orgId, fromRole, fromUser,
              documentType, proposalmgmt.getTotalamount(), proposalmgmt.getCreatedBy().getId(),
              false, proposalmgmt.getProposalappstatus(), proposalmgmt);
        } else {
          nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
              OBDal.getInstance().getConnection(), clientId, orgId, fromRole, fromUser,
              documentType, proposalmgmt.getTotalamount(), fromUser, false,
              proposalmgmt.getProposalappstatus());
        }

      } else {
        if (isDirectApproval) {
          if (isDirectProposal) {
            List<NextRoleByRuleVO> list = NextRoleByRule.getNextRoleList(
                OBDal.getInstance().getConnection(), clientId, orgId, fromRole, fromUser,
                documentType, proposalmgmt.getTotalamount());
            if (proposalmgmt.getAgencyorg() == null) {
              hasAgency = Boolean.FALSE;
              fromRole = assignRole(list, fromRole);
            }
            nextApproval = NextRoleByRule.getAgencyManagerBasedNextRole(
                OBDal.getInstance().getConnection(), clientId, orgId, fromRole, fromUser,
                documentType, proposalmgmt.getTotalamount(), proposalmgmt.getCreatedBy().getId(),
                false, proposalmgmt.getProposalappstatus(), proposalmgmt);
          } else {
            nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
                orgId, fromRole, fromUser, documentType, proposalmgmt.getTotalamount());
          }

          /*
           * nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
           * OBDal.getInstance().getConnection(), clientId, orgId, roleId, userId, documentType,
           * proposalmgmt.getTotalamount(), proposalmgmt.getCreatedBy().getId(), false);
           */
          if (nextApproval != null && nextApproval.hasApproval()) {
            nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
            if (nextRole.getEutNextRoleLineList().size() > 0) {
              for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
                List<UserRoles> userRole = proposalDAO
                    .getUserRole(objNextRoleLine.getRole().getId());
                role = NextRoleByRule.getbackwardDelegatedFromAndToRoles(
                    OBDal.getInstance().getConnection(), clientId, orgId,
                    userRole.get(0).getUserContact().getId(), alertResource, "");
                delegatedFromRole = role.get("FromUserRoleId");
                delegatedToRole = role.get("ToUserRoleId");
                isBackwardDelegation = NextRoleByRule.isBackwardDelegation(
                    OBDal.getInstance().getConnection(), clientId, orgId, delegatedFromRole,
                    delegatedToRole, fromUser, alertResource, proposalmgmt.getTotalamount());
                if (isBackwardDelegation)
                  break;
              }
            }
          }
          if (isBackwardDelegation) {
            nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
                orgId, delegatedFromRole, fromUser, documentType, proposalmgmt.getTotalamount());
            /*
             * nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
             * OBDal.getInstance().getConnection(), clientId, orgId, roleId, userId, documentType,
             * proposalmgmt.getTotalamount(), proposalmgmt.getCreatedBy().getId(), false);
             */
          }
        } else {
          role = NextRoleByRule.getDelegatedFromAndToRoles(OBDal.getInstance().getConnection(),
              clientId, orgId, fromUser, alertResource, qu_next_role_id);

          delegatedFromRole = role.get("FromUserRoleId");
          delegatedToRole = role.get("ToUserRoleId");

          if (delegatedFromRole != null && delegatedToRole != null)
            nextApproval = NextRoleByRule.getDelegatedNextRole(OBDal.getInstance().getConnection(),
                clientId, orgId, delegatedFromRole, delegatedToRole, fromUser, alertResource,
                proposalmgmt.getTotalamount());
        }
      }

      // if no organization manager is exist then throw error
      if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains(Constants.ESCMORGANIZATIONMANAGER)) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            OBMessageUtils.messageBD("Escm_Orgmanager_notexist").replace("@", agencyOrg));
        bundle.setResult(result);
        return;
      }

      // if organization manager does not have agency manager role
      if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains(Constants.ESCMROLENOTEXIST)) {
        OBDal.getInstance().rollbackAndClose();
        userID = nextApproval.getErrorMsg().replace("ESCMROLENOTEXIST", "");
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            OBMessageUtils.messageBD("Escm_AgencyRole_notexist").replace("@", userID));
        bundle.setResult(result);
        return;
      }

      //
      if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains(Constants.ESCMUSERNOTEXIST)) {
        OBDal.getInstance().rollbackAndClose();
        userID = nextApproval.getErrorMsg().replace("ESCMUSERNOTEXIST", "");
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            OBMessageUtils.messageBD("ESCM_USERNOTEXIST").replace("@", userID));
        bundle.setResult(result);
        return;
      }

      if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().equals("NoManagerAssociatedWithRole")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_No_LineManager@");
        bundle.setResult(result);
        return;
      }
      // if Role doesnot have any user associated then this condition will execute and return error
      else if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains("EUT_NOUser_ForRoles")) {
        errorMsg = OBMessageUtils.messageBD(nextApproval.getErrorMsg());
        errorMsg = errorMsg.replace("@", nextApproval.getRoleName());
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", errorMsg);
        bundle.setResult(result);
        return;
      }
      // get old nextrole line user and role list
      HashMap<String, String> alertReceiversMap = forwardDao
          .getNextRoleLineList(proposalmgmt.getEUTNextRole(), documentType);

      // update proposal Management header status based on next approver
      headerUpdate = ProposalManagementActionMethod.updateproposalmanagementheader(nextApproval,
          proposalmgmt);
      if (headerUpdate) {
        OBDal.getInstance().save(proposalmgmt);
        // alert process

        // get alert rule id - Task No:7618
        alertRuleId = AlertUtility.getAlertRule(clientId, alertWindow);

        if (nextApproval != null && nextApproval.hasApproval()) {
          includeRecipient = new ArrayList<String>();
          nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());

          // set alerts for next roles
          if (nextRole.getEutNextRoleLineList().size() > 0) {

            // solve approval alerts - Task No:7618
            AlertUtility.solveAlerts(proposalmgmt.getId());

            forwardDao.getAlertForForwardedUser(proposalmgmt.getId(), alertWindow, alertRuleId,
                objUser, clientId, Constants.APPROVE,
                proposalmgmt.getProposalno() + "-" + proposalmgmt.getBidName(), Lang,
                vars.getRole(), proposalmgmt.getEUTForwardReqmoreinfo(), documentType,
                alertReceiversMap);
            for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
              AlertUtility.alertInsertionRole(proposalmgmt.getId(),
                  proposalmgmt.getProposalno() + "-" + proposalmgmt.getBidName(),
                  objNextRoleLine.getRole().getId(),
                  (objNextRoleLine.getUserContact() == null ? ""
                      : objNextRoleLine.getUserContact().getId()),
                  proposalmgmt.getClient().getId(), Description, "NEW", alertWindow, "scm.pm.wfa",
                  Constants.GENERIC_TEMPLATE);

              // get user name for delegated user to insert on approval history.

              List<EutDocappDelegateln> delegationln = proposalDAO
                  .getDelegation(objNextRoleLine.getRole().getId(), currentDate, documentType);
              if (delegationln != null && delegationln.size() > 0) {
                /* Task #7742 */

                for (EutDocappDelegateln obDocAppDelegation : delegationln) {
                  AlertUtility.alertInsertionRole(proposalmgmt.getId(),
                      proposalmgmt.getProposalno() + "-" + proposalmgmt.getBidName(),
                      obDocAppDelegation.getRole().getId(),
                      obDocAppDelegation.getUserContact().getId(), proposalmgmt.getClient().getId(),
                      Description, "NEW", alertWindow, "scm.pm.wfa", Constants.GENERIC_TEMPLATE);
                  // add delegated role as recipient
                  includeRecipient.add(obDocAppDelegation.getRole().getId());
                }
                if (nextRole.getEutNextRoleLineList().size() == 1 && delegationln.size() == 1
                    && Utility
                        .getAssignedUserForRoles(
                            nextRole.getEutNextRoleLineList().get(0).getRole().getId())
                        .size() == 1) {
                  if (pendingapproval != null)
                    pendingapproval += objNextRoleLine.getRole().getName() + " ("
                        + OBMessageUtils.messageBD("ESCM_Delegated_From") + ")" + " / "
                        + delegationln.get(0).getRole().getName() + " - "
                        + delegationln.get(0).getUserContact().getName();
                  else
                    pendingapproval = String.format(Constants.sWAITINGFOR_S_APPROVAL,
                        objNextRoleLine.getRole().getName() + " ("
                            + OBMessageUtils.messageBD("ESCM_Delegated_From") + ")" + " / "
                            + delegationln.get(0).getRole().getName() + " - "
                            + delegationln.get(0).getUserContact().getName());
                }

              }
              // add next role recipient
              includeRecipient.add(objNextRoleLine.getRole().getId());
            }
          }

          // get alert recipient - Task No:7618
          List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);

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

          log.debug("docsta:" + proposalmgmt.getProposalappstatus() + "action:"
              + proposalmgmt.getEscmDocaction());

        }

        else {
          if (enccontrollist.size() > 0
              && (budgetcontroller == null || !budgetcontroller.equals("Y"))) {
            if (!proposalmgmt.isEfinIsbudgetcntlapp()) {
              errorMsg = OBMessageUtils.messageBD("Efin_No_BudgetControl");
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", errorMsg);
              bundle.setResult(result);
              return;
            }
          }
          includeRecipient = new ArrayList<String>();
          objCreatedRole = proposalmgmt.getRole();
          proposalmgmt.setVersion(false);
          OBDal.getInstance().save(proposalmgmt);

          // solving approval alerts - Task No:7618
          AlertUtility.solveAlerts(proposalmgmt.getId());

          forwardDao.getAlertForForwardedUser(proposalmgmt.getId(), alertWindow, alertRuleId,
              objUser, clientId, Constants.APPROVE,
              proposalmgmt.getProposalno() + "-" + proposalmgmt.getBidName(), Lang, vars.getRole(),
              proposalmgmt.getEUTForwardReqmoreinfo(), documentType, alertReceiversMap);

          // get alert recipient - Task No:7618
          List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);

          // check and insert recipient
          if (alrtRecList.size() > 0) {
            for (AlertRecipient objAlertReceipient : alrtRecList) {
              includeRecipient.add(objAlertReceipient.getRole().getId());
              OBDal.getInstance().remove(objAlertReceipient);
            }
          }
          includeRecipient.add(objCreatedRole.getId());

          // avoid duplicate recipient
          HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
          Iterator<String> iterator = incluedSet.iterator();
          while (iterator.hasNext()) {
            AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
          }
          // set alert for requester

          AlertUtility.alertInsertionRole(proposalmgmt.getId(),
              proposalmgmt.getProposalno() + "-" + proposalmgmt.getBidName(),
              proposalmgmt.getRole().getId(), proposalmgmt.getCreatedBy().getId(),
              proposalmgmt.getClient().getId(), DescriptionApproved, "NEW", alertWindow,
              "scm.pm.approved", Constants.GENERIC_TEMPLATE);
        }

        // chk encumbrance validation
        log.debug("budgetcontroller:" + budgetcontroller);
        // Task No.5925
        if (enccontrollist.size() > 0 && !proposalmgmt.isEfinIsbudgetcntlapp()
            && budgetcontroller != null && budgetcontroller.equals("Y")) {
          if (enccontrollist.size() > 0 && proposalmgmt.getEscmOldproposal() != null
              && proposalmgmt.getEscmOldproposal().getEfinEncumbrance() != null) {
            // New version encumbrance update
            // it will insert modification in existing encumbrance when amount is differ in new
            // version
            if (proposalmgmt.getEfinEncumbrance() != null
                && proposalmgmt.getEfinEncumbrance().getEncumMethod().equals("A")) {
              ProposalManagementActionMethod.doMofifcationInEncumbrance(proposalmgmt,
                  proposalmgmt.getEscmOldproposal());
            } else {
              ProposalManagementActionMethod.chkNewVersionManualEncumbranceValidation(proposalmgmt,
                  proposalmgmt.getEscmOldproposal(), false, false, null);
            }
          } else {
            // check lines added from pr
            for (EscmProposalmgmtLine line : proposalmgmt.getEscmProposalmgmtLineList()) {
              List<EscmProposalsourceRef> proposalsrcref = propDAO
                  .checkLinesAddedFromPR(line.getId());
              if (proposalsrcref != null && proposalsrcref.size() > 0) {
                fromPR = true;
                break;
              }
            }
            if (!fromPR) {
              // do the encumbrance validation
              if (proposalmgmt.getEscmBidmgmt() != null
                  && proposalmgmt.getEscmBidmgmt().getEncumbrance() != null) {

                if (proposalmgmt.getProposalstatus().equals("AWD")) {
                  // encumbrance other than proposal encumbrance
                  if (!proposalmgmt.getEscmBidmgmt().getEncumbrance().getEncumType()
                      .equals("PAE")) {
                    encumbrance = proposalmgmt.getEscmBidmgmt().getEncumbrance();
                    if ("PAE".equals(encumbrance.getEncumStage())) {
                      OBDal.getInstance().rollbackAndClose();
                      errorFlag = true;
                      OBError result = OBErrorBuilder.buildMessage(null, "error",
                          "@ESCM_EncumUtilizedInProposal@");
                      bundle.setResult(result);
                      return;
                    } else
                      ProposalManagementActionMethod.getBidtoProposalDetails(proposalmgmt, false);
                    encumbrance.setEncumStage("PAE");
                    OBDal.getInstance().save(encumbrance);
                  }
                } else if (proposalmgmt.getProposalstatus().equals("PAWD")) {
                  // split encumbrance or stage move
                  List<String> proposalList = new ArrayList<String>();
                  proposalList.add(proposalId);
                  boolean isFullyAwarded = UnifiedProposalActionMethod
                      .isProposalFullyAwarded(proposalList);
                  if (isFullyAwarded) {
                    OBError error1 = UnifiedProposalActionMethod.changeEncumStage(proposalmgmt,
                        vars);
                    if (error1.getType().equals("error")) {
                      OBDal.getInstance().rollbackAndClose();
                      bundle.setResult(error1);
                      return;
                    }
                  } else {
                    // Get proposal details
                    JSONObject result = UnifiedProposalActionMethod
                        .bidtoProposalEncumChanges(proposalmgmt, false);
                    OBError error1 = UnifiedProposalActionMethod
                        .createSplitEncumbrance(proposalmgmt, result, false);
                    if (error1.getType().equals("error")) {
                      OBDal.getInstance().rollbackAndClose();
                      bundle.setResult(error1);
                      return;
                    }
                  }
                }

              }
              // proposal encumbrance
              else if (proposalmgmt.getEfinEncumbrance() != null
                  && proposalmgmt.getEfinEncumbrance().getEncumType().equals("PAE")) {
                // amount update
                ProposalManagementActionMethod.chkManualEncumbranceValidation(proposalmgmt, false);
              }
              // without encumbrance
              else {
                // insert auto encumbrance.
                ProposalManagementActionMethod.insertAutoEncumbrance(proposalmgmt);
              }

              proposalmgmt.setEfinIsbudgetcntlapp(true);
              OBDal.getInstance().save(proposalmgmt);
            }
            // pr
            else {
              resultEncum = ProposalManagementActionMethod.checkFullPRQtyUitlizeorNot(proposalmgmt);
              log.debug("resultEncum:" + resultEncum);
              if (resultEncum.has("isFullQtyUsed") && resultEncum.getBoolean("isFullQtyUsed")) {
                proposalmgmt.setEfinEncumbrance(OBDal.getInstance().get(EfinBudgetManencum.class,
                    resultEncum.getString("encumbrance")));
                proposalmgmt.getEfinEncumbrance().setEncumStage("PAE");
                proposalmgmt.setEfinIsbudgetcntlapp(true);
                if (proposalmgmt.getEfinEncumbrance().getEncumMethod().equals("A")) {
                  if (proposalmgmt.getBidName() != null)
                    proposalmgmt.getEfinEncumbrance().setDescription(proposalmgmt.getBidName());
                  else
                    proposalmgmt.getEfinEncumbrance().setDescription(proposalmgmt.getProposalno());
                }
                OBDal.getInstance().save(proposalmgmt);

                ProposalManagementActionMethod.chkAndUpdateforProposalPRFullQty(proposalmgmt,
                    proposalmgmt.getEfinEncumbrance(), false, false);

                for (EscmProposalmgmtLine line : proposalmgmt.getEscmProposalmgmtLineList()) {
                  if (line.getEFINUniqueCode() != null
                      && proposalmgmt.getEfinEncumbrance() != null) {
                    encumLnList = proposalDAO.getEncumLines(
                        proposalmgmt.getEfinEncumbrance().getId(),
                        line.getEFINUniqueCode().getId());
                    if (encumLnList.size() > 0) {
                      line.setEfinBudgmanencumline(encumLnList.get(0));
                    }
                  }
                }
              } else {
                if (resultEncum.has("type") && resultEncum.getString("type").equals("SPLIT")) {
                  BidManagementDAO.splitPR(resultEncum, null, null, proposalmgmt, null);
                }
                if (resultEncum.has("type") && resultEncum.getString("type").equals("MERGE")) {
                  BidManagementDAO.splitPR(resultEncum, null, null, proposalmgmt, null);
                }
                // if pr is skip the encumbrance
                if (resultEncum.has("isAssociatePREncumbrance")
                    && !resultEncum.getBoolean("isAssociatePREncumbrance")) {
                  // insert auto encumbrance.
                  ProposalManagementActionMethod.insertAutoEncumbrance(proposalmgmt);
                }
              }
            }
          }

          if (proposalmgmt.getEfinEncumbrance() != null) {
            OBInterceptor.setPreventUpdateInfoChange(true);
            encumbrance = proposalmgmt.getEfinEncumbrance();
            encumbrance.setBusinessPartner(proposalmgmt.getSupplier());
            OBDal.getInstance().save(encumbrance);
            OBDal.getInstance().flush();
            OBInterceptor.setPreventUpdateInfoChange(false);

          }
        }
        // End Task No.5925

        // end check encumbrance
        // end alert process
        if (pendingapproval == null)
          pendingapproval = nextApproval.getStatus();
        // insert the Action history
        if (!StringUtils.isEmpty(proposalId)) {
          JSONObject historyData = new JSONObject();

          historyData.put("ClientId", clientId);
          historyData.put("OrgId", orgId);
          if (!hasAgency) {
            historyData.put("RoleId", vars.getRole());
            historyData.put("UserId", vars.getUser());
          } else {
            historyData.put("RoleId", roleId);
            historyData.put("UserId", userId);
          }
          historyData.put("HeaderId", proposalId);
          historyData.put("Comments", comments);
          historyData.put("Status", histStatus);
          historyData.put("NextApprover", pendingapproval);
          historyData.put("HistoryTable", ApprovalTables.Proposal_Management_History);
          historyData.put("HeaderColumn", ApprovalTables.Proposal_Management_History_HEADER_COLUMN);
          historyData.put("ActionColumn",
              ApprovalTables.Proposal_Management_History_DOCACTION_COLUMN);

          Utility.InsertApprovalHistory(historyData);
        }
      }
      OBDal.getInstance().flush();
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(), alertResource);

      // Forward and RMI Changes
      // after approved by forwarded user
      if (proposalmgmt.getEUTForwardReqmoreinfo() != null) {
        // REMOVE Role Access to Receiver
        // forwardDao.removeRoleAccess(clientId, objRequest.getEUTForwardReqmoreinfo().getId(),
        // con);
        // set status as "Draft" for forward record
        forwardDao.setForwardStatusAsDraft(proposalmgmt.getEUTForwardReqmoreinfo());
        // set forward_rmi id as null in record
        proposalmgmt.setEUTForwardReqmoreinfo(null);
      }

      // removing rmi
      if (proposalmgmt.getEUTReqmoreinfo() != null) {
        // REMOVE Role Access to Receiver
        // forwardDao.removeReqMoreInfoRoleAccess(clientId, objRequest.getEUTReqmoreinfo().getId(),
        // con);
        // set status as "Draft" for forward record
        forwardDao.setForwardStatusAsDraft(proposalmgmt.getEUTReqmoreinfo());
        // set forward_rmi id as null in record
        proposalmgmt.setEUTReqmoreinfo(null);
        proposalmgmt.setRequestMoreInformation("N");
      }
      // Forward and RMI Changes end

      // Check Encumbrance Amount is Zero Or Negative
      if (proposalmgmt.getEfinEncumbrance() != null)
        encumLinelist = proposalmgmt.getEfinEncumbrance().getEfinBudgetManencumlinesList();
      if (encumLinelist.size() > 0)
        checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);
      if (checkEncumbranceAmountZero) {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Encumamt_Neg@");
        bundle.setResult(result);
        return;
      }

      if (!errorFlag) {
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
        bundle.setResult(result);
        return;
      } else {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ProcessFailed@");
        bundle.setResult(result);
        return;
      }
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while insertAutoEncumbrance: " + e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      bundle.setResult(result);
      return;
    } catch (Exception e) {
      log.debug("Exeception in ProposalManagementAction:" + e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);

    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method is to Skip Agency Manager role level in document rule if agency organization is
   * empty in Proposal management
   * 
   * @param list
   * @param roleId
   * @return same roleId if nextrole list does not have agency manager role else agency manager role
   */

  private String assignRole(List<NextRoleByRuleVO> list, String roleId) {

    Role nextRole = null;
    for (NextRoleByRuleVO checkVO : list) {
      nextRole = OBDal.getInstance().get(Role.class, checkVO.getNextRoleId());
      if (nextRole.isEscmIsagencymanager() != null && nextRole.isEscmIsagencymanager()) {
        return nextRole.getId();
      }
    }
    return roleId;
  }

}
