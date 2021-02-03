package sa.elm.ob.scm.ad_process.ProposalManagement;

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
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
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
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.scm.ESCMProductContCatg;
import sa.elm.ob.scm.ESCMProposalEvlEvent;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.Escmbidconfiguration;
import sa.elm.ob.scm.Escmbidmgmtline;
import sa.elm.ob.scm.actionHandler.dao.ProposalManagementDAO;
import sa.elm.ob.scm.actionHandler.dao.ProposalManagementDAOImpl;
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

public class UnifiedProposalAction extends DalBaseProcess {
  private static final Logger log = LoggerFactory.getLogger(UnifiedProposalAction.class);
  private static String errorMsg = null;
  public static final String PROPOSAL_WINDOW_ID = "CAF2D3EEF3B241018C8F65E8F877B29F";

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String Lang = vars.getLanguage();

    try {
      OBContext.setAdminMode();
      final String unifiedProposalId = (String) bundle.getParams().get("Escm_Proposalmgmt_ID")
          .toString();
      EscmProposalMgmt unifiedProposal = OBDal.getInstance().get(EscmProposalMgmt.class,
          unifiedProposalId);

      final String clientId = (String) bundle.getContext().getClient();
      String userId = (String) bundle.getContext().getUser();
      String roleId = (String) bundle.getContext().getRole();
      String orgId = unifiedProposal.getOrganization().getId();

      String DocStatus = unifiedProposal.getProposalappstatus(),
          ProposalStatus = unifiedProposal.getProposalstatus(),
          DocAction = unifiedProposal.getEscmUnifiedProposalAction();

      JSONObject fromUserandRoleJson = new JSONObject();
      ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
      boolean isDummyRole = false, isDirectApproval = false, hasAgency = true,
          isBackwardDelegation = false, headerUpdate = false;
      String fromUser = userId;
      String fromRole = roleId;
      String agencyOrg = "", qu_next_role_id = "", userID = "", budgetcontroller = "";
      NextRoleByRuleVO nextApproval = null;
      EutNextRole nextRole = null;
      ProposalManagementProcessDAO proposalDAO = new ProposalManagementProcessDAOImpl();
      HashMap<String, String> role = null;
      String delegatedFromRole = null, delegatedToRole = null;
      String alertRuleId = "", alertWindow = AlertWindow.UnifiedProposal;
      ArrayList<String> includeRecipient = null;
      User objUser = Utility.getObject(User.class, vars.getUser());
      Date currentDate = new Date();
      String pendingapproval = null;
      Role objCreatedRole = null;
      User User = OBDal.getInstance().get(User.class, userId);

      boolean isDirectProposal = unifiedProposal.getProposalType().equals("DR") ? true : false;
      String documentType = isDirectProposal ? "EUT_122" : "EUT_117";

      String Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.unifiedProposal.wfa",
          Lang) + " " + unifiedProposal.getCreatedBy().getName(),
          comments = (String) bundle.getParams().get("notes").toString(), histStatus = null;

      String DescriptionApproved = sa.elm.ob.scm.properties.Resource
          .getProperty("scm.unifiedProposal.approved", Lang) + " " + User.getName();

      isDirectApproval = ProposalManagementActionMethod.isDirectApproval(unifiedProposal.getId(),
          roleId);

      String alertResource = isDirectProposal ? Resource.PROPOSAL_MANAGEMENT_DIRECT
          : Resource.PROPOSAL_MANAGEMENT;

      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      enccontrollist = ProposalManagementActionMethod.getEncControleList(unifiedProposal);

      // Unified Proposal submit validation - start
      Boolean chkRoleIsInDocRul = false, allowDelegation = false, allowUpdate = false;
      String proposalMessage = null;
      boolean hasError = false;
      String preferenceValue = "N";
      List<Escmbidconfiguration> bidconfList = new ArrayList<Escmbidconfiguration>();
      ProposalManagementDAO propDAO = new ProposalManagementDAOImpl();

      Boolean allowApprove = false;
      if (unifiedProposal.getEUTForwardReqmoreinfo() != null) {
        allowApprove = forwardDao.allowApproveReject(unifiedProposal.getEUTForwardReqmoreinfo(),
            userId, roleId, documentType);
      }
      if (unifiedProposal.getEUTReqmoreinfo() != null
          || ((unifiedProposal.getEUTForwardReqmoreinfo() != null) && (!allowApprove))) {
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

      // Restrict to reactivate Proposal, once it is used in proposal evaluation event
      // or open envelope event
      if (unifiedProposal.getEscmDocaction().equals("SA")
          && !unifiedProposal.getProposalstatus().equals("AWD")
          && !unifiedProposal.getProposalstatus().equals("PAWD")
          && unifiedProposal.isNeedEvaluation()) {
        if (!unifiedProposal.getProposalstatus().equals("SUB")) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }
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
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_RoleIsNotIncInDocRule@");
          bundle.setResult(result);
          return;
        }
      }
      if (!DocStatus.equals("INC") && !DocStatus.equals("REJ") && !DocStatus.equals("REA")) {
        if (unifiedProposal.getEUTNextRole() != null) {
          java.util.List<EutNextRoleLine> li = unifiedProposal.getEUTNextRole()
              .getEutNextRoleLineList();
          for (int i = 0; i < li.size(); i++) {
            String roleid = li.get(i).getRole().getId();
            if (roleId.equals(roleid)) {
              allowUpdate = true;
            }
          }

        }
        // get delegation role
        if (unifiedProposal.getEUTNextRole() != null) {
          DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
          allowDelegation = delagationDao.checkDelegation(currentDate, roleId, documentType);
        }
        if (!allowUpdate && !allowDelegation) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }
      }
      // Unified Proposal submit validation - end

      Connection con = OBDal.getInstance().getConnection();
      PreparedStatement ps = null;
      ResultSet rs = null;
      String query = null;
      List<String> proposalList = new ArrayList<String>();
      List<String> propList = new ArrayList<String>();
      try {

        query = " select escm_proposalmgmt_id from escm_proposalmgmt_line "
            + " where escm_proposalmgmt_line_id in (select escm_unifiedproposalines_v_id  "
            + " from escm_unifiedproposalines_v where escm_proposalmgmt_id = ?) "
            + " group by escm_proposalmgmt_id ";

        ps = con.prepareStatement(query);
        ps.setString(1, unifiedProposalId);

        rs = ps.executeQuery();

        // Get proposal id list
        while (rs.next()) {
          proposalList.add(rs.getString("escm_proposalmgmt_id"));
          propList.add(rs.getString("escm_proposalmgmt_id"));
        }

        // Common validation for both unified and awarded proposals
        if (!propList.contains(unifiedProposalId)) {
          propList.add(unifiedProposalId);
        }

        // Budget definition is closed validation
        proposalMessage = OBMessageUtils.messageBD("Escm_proposalcommon").replace("%", "");
        hasError = false;
        for (String proposalId : propList) {
          EscmProposalMgmt proposalmgmt = OBDal.getInstance().get(EscmProposalMgmt.class,
              proposalId);
          if (proposalmgmt.getEfinBudgetinitial() != null
              && proposalmgmt.getEfinBudgetinitial().getStatus().equals("CL")) {
            hasError = true;
            proposalMessage = proposalMessage + proposalmgmt.getProposalno() + ",";
          }
        }
        if (hasError) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", proposalMessage,
              "@Efin_Budget_Definition_Closed@");
          bundle.setResult(result);
          return;
        }

        // Budget definition Pre Close Year validation
        proposalMessage = OBMessageUtils.messageBD("Escm_proposalcommon").replace("%", "");
        hasError = false;
        for (String proposalId : propList) {
          EscmProposalMgmt proposalmgmt = OBDal.getInstance().get(EscmProposalMgmt.class,
              proposalId);
          if (proposalmgmt.getEfinBudgetinitial() != null
              && proposalmgmt.getEfinBudgetinitial().isPreclose()) {
            hasError = true;
            proposalMessage = proposalMessage + proposalmgmt.getProposalno() + ", ";
          }

        }
        if (hasError) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", proposalMessage,
              "@Efin_PreClose_Year_Validation@");
          bundle.setResult(result);
          return;
        }
        // Common validation - end

        // Awarded proposals validation - start
        // Check approval is from unified proposal tab in proposal evaluation event and if so then
        // validate the awarded qty with bid qty
        proposalMessage = OBMessageUtils.messageBD("Escm_proposalcommon").replace("%", "");
        hasError = false;
        for (String proposalId : proposalList) {
          EscmProposalMgmt proposalmgmt = OBDal.getInstance().get(EscmProposalMgmt.class,
              proposalId);

          if (proposalmgmt != null && proposalmgmt.getEscmBidmgmt() != null
              && proposalmgmt.isPartialAward()) {
            List<ESCMProposalEvlEvent> evntList = proposalmgmt.getEscmBidmgmt()
                .getESCMProposalEvlEventList();

            if (evntList.size() > 0) {
              if (evntList.get(0).isAwardfullqty()) {
                for (Escmbidmgmtline lines : proposalmgmt.getEscmBidmgmt()
                    .getEscmBidmgmtLineList()) {
                  if (lines.getMovementQuantity().compareTo(lines.getAwardedqty()) != 0
                      && !lines.isSummarylevel()) {
                    OBDal.getInstance().rollbackAndClose();
                    OBError result = OBErrorBuilder.buildMessage(null, "error",
                        "@Escm_biqty_awardqty_notsame@");
                    bundle.setResult(result);
                    return;
                  }
                }
              }
            }
          }
        }

        // attachment mandatory checking only for submit for approval and waiting for approval
        proposalMessage = "";
        hasError = false;
        for (String proposalId : proposalList) {
          EscmProposalMgmt proposalmgmt = OBDal.getInstance().get(EscmProposalMgmt.class,
              proposalId);
          String docAction = proposalmgmt.getEscmDocaction();

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
            if (preferenceValue != null && preferenceValue.equals("Y") && !docAction.equals("RE")) {
              // attachment is mandatory
              OBQuery<Attachment> file = OBDal.getInstance().createQuery(Attachment.class,
                  " as e where e.record=:Record");
              file.setNamedParameter("Record", proposalId);
              if (file != null && file.list().size() == 0) {
                hasError = true;
                proposalMessage = proposalMessage + proposalmgmt.getProposalno() + ", ";
              }
            }
          }
        }
        if (hasError) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              OBMessageUtils.messageBD("@Escm_Attachment_Mandatory@"), proposalMessage,
              "@ESCM_addattachment@");
          bundle.setResult(result);
          return;
        }

        proposalMessage = OBMessageUtils.messageBD("Escm_proposalcommon").replace("%", "");
        hasError = false;
        for (String proposalId : proposalList) {
          EscmProposalMgmt proposalmgmt = OBDal.getInstance().get(EscmProposalMgmt.class,
              proposalId);
          if (isDirectProposal) {
            bidconfList = proposalDAO.getBidConfiguration(clientId, orgId);
            if (bidconfList.size() > 0) {
              Escmbidconfiguration typ = bidconfList.get(0);
              if (proposalmgmt.getTotalamount()
                  .compareTo(BigDecimal.valueOf(typ.getAppmaxvalue())) > 0) {
                hasError = true;
                proposalMessage = proposalMessage + proposalmgmt.getProposalno() + ", ";
              }
            }
          }
        }
        if (hasError) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", proposalMessage,
              "@ESCM_PropTotalAmtCantExceed@");
          bundle.setResult(result);
          return;
        }

        // Check whether the product belongs to that contract category or not
        proposalMessage = OBMessageUtils.messageBD("Escm_proposalcommon").replace("%", "");
        hasError = false;
        for (String proposalId : proposalList) {
          EscmProposalMgmt proposalmgmt = OBDal.getInstance().get(EscmProposalMgmt.class,
              proposalId);
          if (proposalmgmt.getContractType() != null) {
            String contCatgId = proposalmgmt.getContractType().getId();
            for (EscmProposalmgmtLine line : proposalmgmt.getEscmProposalmgmtLineList()) {
              if (line.getProduct() != null) {
                if (line.getProduct().getESCMPRODCONTCATGList() != null
                    && line.getProduct().getESCMPRODCONTCATGList().size() != 0) {
                  boolean contCatgMatch = true;
                  for (ESCMProductContCatg pContCatg : line.getProduct()
                      .getESCMPRODCONTCATGList()) {
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
                    hasError = true;
                    proposalMessage = proposalMessage + proposalmgmt.getProposalno() + ", ";
                  }
                }
              }
            }
          }
        }
        if (hasError) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", proposalMessage,
              "@ESCM_ItemMismatchWithContCatg@");
          bundle.setResult(result);
          return;
        }

        // check lines to submit for approval
        proposalMessage = OBMessageUtils.messageBD("Escm_proposalcommon").replace("%", "");
        hasError = false;
        for (String proposalId : proposalList) {
          EscmProposalMgmt proposalmgmt = OBDal.getInstance().get(EscmProposalMgmt.class,
              proposalId);
          if (proposalmgmt.getEscmProposalmgmtLineList().size() == 0) {
            hasError = true;
            proposalMessage = proposalMessage + proposalmgmt.getProposalno() + ", ";
          }
        }
        if (hasError) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", proposalMessage,
              "@ESCM_No_Proposal_Lines@");
          bundle.setResult(result);
          return;
        }

        for (String proposalId : proposalList) {
          EscmProposalMgmt proposalmgmt = OBDal.getInstance().get(EscmProposalMgmt.class,
              proposalId);
          // Copy UniqueCode... task no. 6007
          // Copy UniqueCode validations only for budget controller role
          if (budgetcontroller != null && budgetcontroller.equals("Y")) {
            if (StringUtils.isNotEmpty(proposalmgmt.getId())) {
              ProposalManagementActionMethod.copyUniqueCodeValidation(proposalmgmt);
            }
          }
        }
        // Awarded proposals validation - end

        // Check for unique code mandatory
        proposalMessage = OBMessageUtils.messageBD("Escm_proposalcommon").replace("%", "");
        hasError = false;
        for (String proposalId : proposalList) {
          EscmProposalMgmt proposalmgmt = OBDal.getInstance().get(EscmProposalMgmt.class,
              proposalId);

          boolean isBudgetController = UnifiedProposalActionMethod
              .checkBudgetController(proposalmgmt, vars);
          if (isBudgetController) {
            if (proposalmgmt.getEFINEncumbranceMethod() == null) {
              hasError = true;
              proposalMessage = proposalMessage + proposalmgmt.getProposalno() + ", ";
            }
          }
        }
        if (hasError) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", proposalMessage,
              "@Efin_Req_Uniquecode_Mandatory@", "@Escm_adduniquecode_proposalattr@");// ESCM_RoleIsNotIncInDocRule
          bundle.setResult(result);
          return;
        }

        // All lines uniquecode should contain same department
        proposalMessage = OBMessageUtils.messageBD("Escm_proposalcommon").replace("%", "");
        String notSameDeptMessage = OBMessageUtils.messageBD("Escm_proposalcommon").replace("%",
            "");
        hasError = false;
        boolean noUniqueCode = false, notSameDept = false;
        for (String proposalId : proposalList) {
          EscmProposalMgmt proposalmgmt = OBDal.getInstance().get(EscmProposalMgmt.class,
              proposalId);

          // Task No.5925
          List<EscmProposalmgmtLine> lines = proposalDAO.getProposalLines(proposalmgmt.getId());
          boolean isBudgetController = UnifiedProposalActionMethod
              .checkBudgetController(proposalmgmt, vars);
          if (isBudgetController) {
            if (lines != null && lines.size() > 0) {
              String dept = null;
              EscmProposalmgmtLine ln = lines.get(0);
              dept = ln.getEFINUniqueCode() != null
                  ? ln.getEFINUniqueCode().getSalesRegion().getId()
                  : null;
              for (EscmProposalmgmtLine prosallin : lines) {
                if (prosallin.getEFINUniqueCode() == null) {
                  noUniqueCode = true;
                  hasError = true;
                  proposalMessage = proposalMessage + proposalmgmt.getProposalno() + ", ";
                  break;

                } else if (prosallin.getEFINUniqueCode() != null
                    && !prosallin.getEFINUniqueCode().getSalesRegion().getId().equals(dept)) {
                  notSameDept = true;
                  hasError = true;
                  notSameDeptMessage = notSameDeptMessage + proposalmgmt.getProposalno() + ", ";
                  break;

                }
              }
            }
          }
        }
        if (hasError && noUniqueCode) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", proposalMessage,
              "@Efin_Req_Uniquecode_Mandatory@");// ESCM_RoleIsNotIncInDocRule
          bundle.setResult(result);
          return;
        }
        if (hasError && notSameDept) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", notSameDeptMessage,
              "@ESCM_AllLinesMustHaveSameDept@");// ESCM_RoleIsNotIncInDocRule
          bundle.setResult(result);
          return;
        }

        // Check line info
        boolean fromPR = false;
        String diffUniqueCodeMessage = OBMessageUtils.messageBD("Escm_proposalcommon").replace("%",
            "");
        String checkInfoMessage = OBMessageUtils.messageBD("Escm_proposalcommon").replace("%", "");
        String sameDeptMessage = OBMessageUtils.messageBD("Escm_proposalcommon").replace("%", "");
        String sameBTypeMessage = OBMessageUtils.messageBD("Escm_proposalcommon").replace("%", "");
        String errorMessageStr = "";
        hasError = false;
        boolean diffUniqueCode = false, checkLineInfo = false, isSameDept = false,
            isSameBType = false, checkAutoLineInfo = false;
        for (String proposalId : proposalList) {
          EscmProposalMgmt proposalmgmt = OBDal.getInstance().get(EscmProposalMgmt.class,
              proposalId);
          boolean isBudgetController = UnifiedProposalActionMethod
              .checkBudgetController(proposalmgmt, vars);
          if (isBudgetController) {
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
                boolean errorFlag = ProposalManagementActionMethod
                    .checkAllUniquecodesameEncum(proposalmgmt);
                if (errorFlag) {
                  diffUniqueCode = true;
                  diffUniqueCodeMessage = diffUniqueCodeMessage + proposalmgmt.getProposalno()
                      + ", ";
                }

                // If proposals are having same manual encumbrance, add all proposal amount and
                // check if there is remaining amount
                boolean errorFlag1 = UnifiedProposalActionMethod
                    .checkManualEncumRemAmt(proposalmgmt, true);
                if (errorFlag1) {
                  checkLineInfo = true;
                  checkInfoMessage = checkInfoMessage + proposalmgmt.getProposalno() + ", ";
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

                    if (errorresult.getString("errormsg")
                        .equals(OBMessageUtils.messageBD("Efin_Req_SameDept"))) {
                      isSameDept = true;
                      sameDeptMessage = sameDeptMessage + proposalmgmt.getProposalno() + ", ";
                    } else if (errorresult.getString("errormsg")
                        .equals(OBMessageUtils.messageBD("Efin_Req_SameBType"))) {
                      isSameBType = true;
                      sameBTypeMessage = sameBTypeMessage + proposalmgmt.getProposalno() + ", ";
                    } else {
                      checkAutoLineInfo = true;
                      checkInfoMessage = checkInfoMessage + proposalmgmt.getProposalno() + ", ";
                    }
                  }
                }
              }
            }
          }
        }
        if (diffUniqueCode) {
          errorMessageStr = errorMessageStr + diffUniqueCodeMessage + "<br>"
              + OBMessageUtils.messageBD("Efin_Unicode_Same_Encum") + "<br>";
        }
        if (checkLineInfo || checkAutoLineInfo) {
          errorMessageStr = errorMessageStr + checkInfoMessage + "<br>"
              + OBMessageUtils.messageBD("Efin_Chk_Line_Info") + "<br>";
        }
        if (isSameDept) {
          errorMessageStr = errorMessageStr + sameDeptMessage + "<br>"
              + OBMessageUtils.messageBD("Efin_Req_SameDept") + "<br>";
        }
        if (isSameBType) {
          errorMessageStr = errorMessageStr + sameBTypeMessage + "<br>"
              + OBMessageUtils.messageBD("Efin_Req_SameBType") + "<br>";
        }

        if (diffUniqueCode || checkLineInfo || checkAutoLineInfo || isSameDept || isSameBType) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", errorMessageStr);
          bundle.setResult(result);
          return;
        }

        // Update line info if any error occurs
        // for (String proposalId : proposalList) {
        // EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
        // // For proposal award encumbrance ( Manual and Auto )
        // OBError error1 = UnifiedProposalActionMethod.encumbranceValidation(proposal, vars);
        // if (error1.getType().equals("error")) {
        // bundle.setResult(error1);
        // return;
        // }
        // }

        // Check bid encumbrance for unified proposal
        if (unifiedProposal.getEscmBidmgmt() != null
            && unifiedProposal.getEscmBidmgmt().getEncumbrance() != null) {
          OBError error1 = UnifiedProposalActionMethod
              .getBidtoUnifiedProposalDetails(unifiedProposal, vars, true, proposalList);
          if (error1.getType().equals("error")) {
            bundle.setResult(error1);
            return;
          }
        }

        // If no error occurs, do encumbrance changes
        for (String proposalId : proposalList) {
          EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
          // For proposal award encumbrance ( Manual and Auto )
          OBError error1 = UnifiedProposalActionMethod.updateProposalEncumbrance(proposal, vars);
          if (error1.getType().equals("error")) {
            OBDal.getInstance().rollbackAndClose();
            bundle.setResult(error1);
            return;
          }
        }

        // update bid encumbrance for unified proposal
        if (unifiedProposal.getEscmBidmgmt() != null
            && unifiedProposal.getEscmBidmgmt().getEncumbrance() != null) {

          boolean isFullyAwarded = UnifiedProposalActionMethod.isProposalFullyAwarded(proposalList);
          if (isFullyAwarded) {
            OBError error1 = UnifiedProposalActionMethod.changeEncumStage(unifiedProposal, vars);
            if (error1.getType().equals("error")) {
              OBDal.getInstance().rollbackAndClose();
              bundle.setResult(error1);
              return;
            }

          } else {
            OBError error1 = UnifiedProposalActionMethod
                .getBidtoUnifiedProposalDetails(unifiedProposal, vars, false, proposalList);
            if (error1.getType().equals("error")) {
              OBDal.getInstance().rollbackAndClose();
              bundle.setResult(error1);
              return;
            }
          }
        }

      } catch (Exception e) {
        log.error("Exception in UnifiedProposalAction " + e.getMessage());
      } finally {
        // close db connection
        try {
          if (rs != null)
            rs.close();
          if (ps != null)
            ps.close();
        } catch (Exception e) {
        }
      }

      if (((DocStatus.equals("INC") || DocStatus.equals("REJ"))
          && (ProposalStatus.equals("AWD") || ProposalStatus.equals("PAWD")))
          || (DocStatus.equals("REA")
              && (ProposalStatus.equals("AWD") || ProposalStatus.equals("PAWD")))) {
        histStatus = "SUB";
      } else if (DocStatus.equals("INP") && DocAction.equals("AP")) {
        histStatus = "AP";
      }

      if ((unifiedProposal.getEUTNextRole() != null)) {

        fromUserandRoleJson = forwardDao.getFromuserAndFromRoleWhileApprove(
            unifiedProposal.getEUTNextRole(), userId, roleId, clientId, orgId, documentType,
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
      if ((unifiedProposal.getEUTNextRole() == null)) {
        if (isDirectProposal) {
          // if next role is agency manager and agency organization is empty then skip the role

          List<NextRoleByRuleVO> list = NextRoleByRule.getNextRoleList(
              OBDal.getInstance().getConnection(), clientId, orgId, fromRole, fromUser,
              documentType, unifiedProposal.getTotalamount());

          if (unifiedProposal.getAgencyorg() == null) {
            hasAgency = Boolean.FALSE;
            fromRole = assignRole(list, fromRole);
          } else {
            agencyOrg = unifiedProposal.getAgencyorg().getCommercialName();
          }

          nextApproval = NextRoleByRule.getAgencyManagerBasedNextRole(
              OBDal.getInstance().getConnection(), clientId, orgId, fromRole, fromUser,
              documentType, unifiedProposal.getTotalamount(),
              unifiedProposal.getCreatedBy().getId(), false, unifiedProposal.getProposalappstatus(),
              unifiedProposal);
        } else {
          nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
              OBDal.getInstance().getConnection(), clientId, orgId, fromRole, fromUser,
              documentType, unifiedProposal.getTotalamount(), fromUser, false,
              unifiedProposal.getProposalappstatus());
        }

      } else {
        if (isDirectApproval) {
          if (isDirectProposal) {
            List<NextRoleByRuleVO> list = NextRoleByRule.getNextRoleList(
                OBDal.getInstance().getConnection(), clientId, orgId, fromRole, fromUser,
                documentType, unifiedProposal.getTotalamount());
            if (unifiedProposal.getAgencyorg() == null) {
              hasAgency = Boolean.FALSE;
              fromRole = assignRole(list, fromRole);
            }
            nextApproval = NextRoleByRule.getAgencyManagerBasedNextRole(
                OBDal.getInstance().getConnection(), clientId, orgId, fromRole, fromUser,
                documentType, unifiedProposal.getTotalamount(),
                unifiedProposal.getCreatedBy().getId(), false,
                unifiedProposal.getProposalappstatus(), unifiedProposal);
          } else {
            nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
                orgId, fromRole, fromUser, documentType, unifiedProposal.getTotalamount());
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
                    delegatedToRole, fromUser, alertResource, unifiedProposal.getTotalamount());
                if (isBackwardDelegation)
                  break;
              }
            }
          }
          if (isBackwardDelegation) {
            nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
                orgId, delegatedFromRole, fromUser, documentType, unifiedProposal.getTotalamount());
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
                unifiedProposal.getTotalamount());
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
      // if Role doesnot have any user associated then this condition will execute and return
      // error
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
          .getNextRoleLineList(unifiedProposal.getEUTNextRole(), documentType);

      // update proposal Management header status based on next approver
      headerUpdate = UnifiedProposalActionMethod.updateUnifiedProposalHeader(nextApproval,
          unifiedProposal);
      if (headerUpdate) {
        OBDal.getInstance().save(unifiedProposal);
        // alert process

        String proposalEvlNo = "", proposalEvlId = "";
        OBQuery<EscmProposalAttribute> proposalAttrQry = OBDal.getInstance().createQuery(
            EscmProposalAttribute.class, " as e where e.escmProposalmgmt.id = :proposalId");
        proposalAttrQry.setNamedParameter("proposalId", unifiedProposal.getId());
        if (proposalAttrQry != null) {
          List<EscmProposalAttribute> proposalAttrList = proposalAttrQry.list();
          if (proposalAttrList.size() > 0) {
            EscmProposalAttribute proposalAttr = proposalAttrList.get(0);
            proposalEvlNo = proposalAttr.getEscmProposalevlEvent().getEventNo();
            proposalEvlId = proposalAttr.getEscmProposalevlEvent().getId();
          }
        }

        // get alert rule id - Task No:7618
        alertRuleId = AlertUtility.getAlertRule(clientId, alertWindow);

        if (nextApproval != null && nextApproval.hasApproval()) {
          includeRecipient = new ArrayList<String>();
          nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());

          // set alerts for next roles
          if (nextRole.getEutNextRoleLineList().size() > 0) {

            // solve approval alerts - Task No:7618
            AlertUtility.solveAlerts(proposalEvlId);

            forwardDao.getAlertForForwardedUser(proposalEvlId, alertWindow, alertRuleId, objUser,
                clientId, Constants.APPROVE, proposalEvlNo, Lang, vars.getRole(),
                unifiedProposal.getEUTForwardReqmoreinfo(), documentType, alertReceiversMap);
            for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
              AlertUtility.alertInsertionRole(proposalEvlId, proposalEvlNo,
                  objNextRoleLine.getRole().getId(),
                  (objNextRoleLine.getUserContact() == null ? ""
                      : objNextRoleLine.getUserContact().getId()),
                  unifiedProposal.getClient().getId(), Description, "NEW", alertWindow,
                  "scm.pm.wfa", Constants.GENERIC_TEMPLATE);

              // get user name for delegated user to insert on approval history.

              List<EutDocappDelegateln> delegationln = proposalDAO
                  .getDelegation(objNextRoleLine.getRole().getId(), currentDate, documentType);
              if (delegationln != null && delegationln.size() > 0) {
                /* Task #7742 */

                for (EutDocappDelegateln obDocAppDelegation : delegationln) {
                  AlertUtility.alertInsertionRole(proposalEvlId, proposalEvlNo,
                      obDocAppDelegation.getRole().getId(),
                      obDocAppDelegation.getUserContact().getId(),
                      unifiedProposal.getClient().getId(), Description, "NEW", alertWindow,
                      "scm.pm.wfa", Constants.GENERIC_TEMPLATE);
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

        }

        else {
          if (enccontrollist.size() > 0
              && (budgetcontroller == null || !budgetcontroller.equals("Y"))) {
            if (!unifiedProposal.isEfinIsbudgetcntlapp()) {
              errorMsg = OBMessageUtils.messageBD("Efin_No_BudgetControl");
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", errorMsg);
              bundle.setResult(result);
              return;
            }
          }
          includeRecipient = new ArrayList<String>();
          objCreatedRole = unifiedProposal.getRole();
          unifiedProposal.setVersion(false);
          OBDal.getInstance().save(unifiedProposal);

          // solving approval alerts - Task No:7618
          AlertUtility.solveAlerts(proposalEvlId);

          forwardDao.getAlertForForwardedUser(proposalEvlId, alertWindow, alertRuleId, objUser,
              clientId, Constants.APPROVE, proposalEvlNo, Lang, vars.getRole(),
              unifiedProposal.getEUTForwardReqmoreinfo(), documentType, alertReceiversMap);

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

          AlertUtility.alertInsertionRole(proposalEvlId, proposalEvlNo,
              unifiedProposal.getRole().getId(), unifiedProposal.getCreatedBy().getId(),
              unifiedProposal.getClient().getId(), DescriptionApproved, "NEW", alertWindow,
              "scm.pm.approved", Constants.GENERIC_TEMPLATE);
        }

        // insert the Action history
        if (pendingapproval == null)
          pendingapproval = nextApproval.getStatus();

        if (!StringUtils.isEmpty(unifiedProposalId)) {
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
          historyData.put("HeaderId", unifiedProposalId);
          historyData.put("Comments", comments);
          historyData.put("Status", histStatus);
          historyData.put("NextApprover", pendingapproval);
          historyData.put("HistoryTable", ApprovalTables.Proposal_Management_History);
          historyData.put("HeaderColumn", ApprovalTables.Proposal_Management_History_HEADER_COLUMN);
          historyData.put("ActionColumn",
              ApprovalTables.Proposal_Management_History_DOCACTION_COLUMN);

          UnifiedProposalActionMethod.InsertApprovalHistory(historyData);

        }
      }
      OBDal.getInstance().flush();
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(), alertResource);

      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
      bundle.setResult(result);
      return;

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in UnifiedProposalAction:" + e);
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
