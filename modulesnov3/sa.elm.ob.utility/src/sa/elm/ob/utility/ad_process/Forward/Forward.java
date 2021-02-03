package sa.elm.ob.utility.ad_process.Forward;

import java.io.IOException;
import java.sql.Connection;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.procurement.Requisition;

import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.utility.EutForwardReqMoreInfo;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.PrimaryKeyDocumentTypeE;

public class Forward extends HttpSecureAppServlet {

  /**
   * Servlet implementation class to used to forward the access
   */
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.utility/jsp/EutForward.jsp";
  ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */

  @SuppressWarnings("unused")
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    String windowReference = null;
    String alertWindowType = null;
    String tabId = null;
    String type = null, doctype = null;
    try {
      OBContext.setAdminMode();
      String action = (request.getParameter("inpAction") == null ? ""
          : request.getParameter("inpAction"));
      request.getParameterMap();
      VariablesSecureApp vars = new VariablesSecureApp(request);
      final String clientId = vars.getClient();
      final String userId = vars.getUser();
      final String roleId = vars.getRole();
      User usr = OBDal.getInstance().get(User.class, vars.getUser());
      String Lang = vars.getLanguage();
      String inpwindowId = (request.getParameter("inpwindowId") == null
          ? (request.getParameter("inpwindowId") == null ? "" : request.getParameter("inpwindowId"))
          : request.getParameter("inpwindowId"));
      OBDal.getInstance();
      ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
      Window windowObj = OBDal.getInstance().get(Window.class, inpwindowId);
      String windowName = forwardReqMoreInfoDAO.getWindowTranslationName(windowObj,
          vars.getLanguage());
      if (windowName == null) {
        windowName = windowObj.getName();
      }
      EutForwardReqMoreInfo forReqMoreInfo = null;
      EutNextRoleLine eutnextroleline = null;
      Requisition req = null;
      EscmProposalMgmt proMgmt = null;
      MaterialIssueRequest mir = null;
      Order purchaseOrder = null;
      ShipmentInOut returnTransaction = null;
      Connection conn = OBDal.getInstance().getConnection();
      String forwardedUserId = null;
      String errMessage = null;
      boolean isRevokeDelegated = false;
      EutNextRole eutNextRole = null;
      EFINFundsReq fundsReqMgmt = null;
      EfinRDVTransaction rdv = null;
      Invoice purchaseinv = null;

      PrimaryKeyDocumentTypeE e = PrimaryKeyDocumentTypeE.getWindowType(inpwindowId);
      String inpRecordId = (request.getParameter(e.getPrimaryKeyColumnName()) == null
          ? (request.getParameter("inpRecordId") == null ? "" : request.getParameter("inpRecordId"))
          : request.getParameter(e.getPrimaryKeyColumnName()));

      // For Receipt Delivery Verification both Transaction Version and Advance Version
      if (inpwindowId.equals(Constants.RDV_W)) {
        OBQuery<EfinRDVTxnline> rdvtxn = OBDal.getInstance().createQuery(EfinRDVTxnline.class,
            "as e where e.efinRdvtxn.id =:headerId");
        rdvtxn.setNamedParameter("headerId", inpRecordId);
        EfinRDVTxnline rdvtxnln = rdvtxn.list().get(0);
        e = PrimaryKeyDocumentTypeE.getRdvSpecialCase(rdvtxnln.isAdvance());
      }

      // logic for the windows which have more than one document type
      if (inpwindowId.equals(Constants.PURCHASE_REQUISITION_W)) {
        req = OBDal.getInstance().get(Requisition.class, inpRecordId);
        type = req.getEscmProcesstype();
        e = PrimaryKeyDocumentTypeE.getWindowTypeSpecialCase(type);
      } else if (inpwindowId.equals(Constants.PROPOSAL_MANAGEMENT_W)) {
        proMgmt = OBDal.getInstance().get(EscmProposalMgmt.class, inpRecordId);
        type = proMgmt.getProposalType();
        e = PrimaryKeyDocumentTypeE.getWindowTypeSpecialCase(type);
      } else if (inpwindowId.equals(Constants.MATERIAL_ISSUE_REQUEST_W)) {
        mir = OBDal.getInstance().get(MaterialIssueRequest.class, inpRecordId);
        type = mir.getEscmDocumenttype();
        e = PrimaryKeyDocumentTypeE.getWindowTypeSpecialCase(type);
      } else if (inpwindowId.equals(Constants.FUNDS_REQ_MGMT_W)) {
        fundsReqMgmt = OBDal.getInstance().get(EFINFundsReq.class, inpRecordId);
        type = fundsReqMgmt.getTransactionType();
        e = PrimaryKeyDocumentTypeE.getWindowTypeSpecialCase(type);
      } else if (inpwindowId.equals(Constants.PURCHASE_INVOICE_W)) {
        purchaseinv = OBDal.getInstance().get(Invoice.class, inpRecordId);
        if (purchaseinv.getTransactionDocument().isEfinIsprepayinv())
          type = "PPI";
        else if (purchaseinv.getTransactionDocument().isEfinIsprepayinvapp())
          type = "PPA";
        else
          type = "API";
        e = PrimaryKeyDocumentTypeE.getWindowTypeSpecialCase(type);
      }

      doctype = e.getStrDocumentType();
      windowReference = e.getwindowReference();
      alertWindowType = e.getAlertWindowType();
      tabId = e.getTabId();

      EutForwardReqMoreInfo forward = forwardReqMoreInfoDAO.getForwardObj(inpRecordId,
          windowReference);
      if (action.equals("")) {
        // Localization support
        request.setAttribute("inpRecordId", inpRecordId);
        request.setAttribute("inpwindowId", inpwindowId);
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.getRequestDispatcher(jspPage).include(request, response);
      } else if (action.equals("getUserList")) {
        JSONObject jsob = new JSONObject();
        if (forward != null)
          forwardedUserId = forward.getUserContact().getId();
        jsob = forwardReqMoreInfoDAO.getUserList(vars.getClient(),
            request.getParameter("searchTerm"), Integer.parseInt(request.getParameter("pageLimit")),
            Integer.parseInt(request.getParameter("page")), vars.getUser(), forwardedUserId,
            inpRecordId, windowReference, doctype, roleId, Constants.FORWARD);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsob.toString());
      } else if (action.equals("getUserRoles")) {
        JSONObject jsob = new JSONObject();
        jsob = forwardReqMoreInfoDAO.getUserRole(vars.getClient(),
            request.getParameter("searchTerm"), Integer.parseInt(request.getParameter("pageLimit")),
            Integer.parseInt(request.getParameter("page")), request.getParameter("inpUserId"),
            windowReference, inpRecordId);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsob.toString());

      } else if (action.equals("getDefaultRole")) {
        JSONObject jsob = new JSONObject(), result = new JSONObject();
        try {
          jsob = forwardReqMoreInfoDAO.getDefaultRole(request.getParameter("inpUserId"),
              windowReference, inpRecordId);
          if (jsob != null && jsob.length() > 0) {

            result.put("defaultRoleId", jsob.getString("defaultRoleId"));
            result.put("defaultRoleName", jsob.getString("defaultRoleName"));
          }

        } catch (final Exception exception) {
          log4j.error("Exception in getDefaultRole : ", exception);
        } finally {
          response.setContentType("text/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(result.toString());
        }
      } else if (action.equals("getUserDept")) {
        JSONObject jsob = new JSONObject(), result = new JSONObject();
        try {
          jsob = forwardReqMoreInfoDAO.getUserDepartment(request.getParameter("inpUserId"));
          if (jsob != null && jsob.length() > 0) {
            result.put("DeptId", jsob.getString("DeptId"));
            result.put("DeptName", jsob.getString("DeptName"));
          }
        } catch (final Exception exception) {
          log4j.error("Exception in getUserDept : ", exception);
        } finally {
          response.setContentType("text/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(result.toString());
        }

      } else if (action.equals("getUserPosition")) {
        JSONObject jsob = new JSONObject(), result = new JSONObject();
        try {
          jsob = forwardReqMoreInfoDAO.getUserPosition(request.getParameter("inpUserId"));
          if (jsob != null && jsob.length() > 0) {
            result.put("PositionName", jsob.getString("PositionName"));
          }

        } catch (final Exception exception) {
          log4j.error("Exception in getUserPosition : ", exception);
        } finally {
          response.setContentType("text/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(result.toString());
        }
      } else if (action.equals("checkPreference")) {
        try {
          JSONObject jsonResponse = null;
          OBError myMessage = new OBError();
          Boolean errorMsg = false;
          // Throw error when preference is disabled for the selected user and role
          List<Preference> prefs = forwardDao.getPreferenceObj(vars.getClient(), false);
          String toUserId = request.getParameter("inpUser");
          String toRoleId = request.getParameter("inpRole");
          for (Preference preference : prefs) {
            if (preference.getUserContact() != null && preference.getVisibleAtRole() == null) {
              if (preference.getUserContact().getId().equals(toUserId)) {
                errorMsg = true;
              }
            }
            if (preference.getVisibleAtRole() != null) {
              if (preference.getVisibleAtRole().getId().equals(toRoleId)) {
                errorMsg = true;
              }
            }
          }
          jsonResponse = new JSONObject();
          jsonResponse.put("errorMsg", errorMsg);
          response.setCharacterEncoding("UTF-8");
          response.getWriter().write(jsonResponse.toString());
        } catch (final Exception e1) {
          log4j.error("Exception in check Preference : ", e1);
        }
      } else if (action.equals("checkUserValidation")) {
        try {
          JSONObject jsonResponse = null;
          Boolean errorMsg = false;
          Boolean isDelegated = false;
          Date currentDate = new Date();
          String toUserId = request.getParameter("inpUser");
          String userFwdRmiId = null, roleFwdRmiId = null;

          // Throw error when user is delegated or it is nested forwarded user
          if (forward != null) {
            userFwdRmiId = forward.getUserContact().getId();
            roleFwdRmiId = forward.getRole().getId();
            String forwardRmiUserId = forwardReqMoreInfoDAO.checkIsNestedForwardRmi(inpRecordId,
                forward.getId());
            if ((toUserId).equals(forwardRmiUserId)) {
              errorMsg = true;
            }

            isDelegated = forwardReqMoreInfoDAO.checkFwdandRmiDelegatedUserOrRole(currentDate,
                doctype, null, toUserId, roleFwdRmiId, userFwdRmiId);
            if (isDelegated) {
              errorMsg = true;
            }
          }
          jsonResponse = new JSONObject();
          jsonResponse.put("errorMsg", errorMsg);
          response.setCharacterEncoding("UTF-8");
          response.getWriter().write(jsonResponse.toString());
        } catch (final Exception e1) {
          log4j.error("Exception in checkUserValidation : ", e1);
        }
      } else if (action.equals("checkForwardToUserOrgAccess")) {
        try {
          JSONObject jsonResponse = null;
          Boolean orgAccess = false;
          String toRoleId = request.getParameter("inpRole");
          orgAccess = forwardReqMoreInfoDAO.checkFrwdRmiToRoleHaveOrgAccess(inpRecordId,
              windowReference, toRoleId);
          jsonResponse = new JSONObject();
          jsonResponse.put("haveOrgAccess", orgAccess);
          response.setCharacterEncoding("UTF-8");
          response.getWriter().write(jsonResponse.toString());
        } catch (final Exception e1) {
          log4j.error("checkForwardToUserOrgAccess : ", e1);
        }
      } else if (action.equals("insertRecord")) {
        String toUserId = request.getParameter("inpToUser");
        String toRoleId = request.getParameter("inpToRole");
        String userFwdRmiId = null, roleFwdRmiId = null;

        EutForwardReqMoreInfo rmi = forwardReqMoreInfoDAO.getReqMoreInfo(inpRecordId,
            windowReference);
        Boolean checkValidation = false;
        String corgId = vars.getOrg();
        Boolean revoke = false;
        Boolean chkRevokeCdn = false;

        errMessage = forwardReqMoreInfoDAO.checkTransactionValidation(windowReference, inpRecordId);
        if (errMessage != null) {
          OBError myMessage = new OBError();
          myMessage
              .setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), errMessage));
          myMessage.setType(Utility.parseTranslation(this, vars, vars.getLanguage(), "Error"));
          vars.setMessage(tabId, myMessage);
          printPageClosePopUp(response, vars, "");
          return;
        }
        checkValidation = forwardReqMoreInfoDAO.forwardPreValidation(inpRecordId, clientId, userId,
            roleId, corgId, doctype, windowReference);
        if (!checkValidation || rmi != null) {
          OBError myMessage = new OBError();
          myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(),
              "@Escm_AlreadyPreocessed_Approved@"));
          myMessage.setType(Utility.parseTranslation(this, vars, vars.getLanguage(), "Error"));
          vars.setMessage(tabId, myMessage);
          printPageClosePopUp(response, vars, "");
          return;
        }

        if (rmi != null) {
          // Update record status as 'DR'
          forReqMoreInfo = forwardReqMoreInfoDAO.revokeForwardUpdateRecordRMI(windowReference, vars,
              inpRecordId, null, false, doctype, false, Constants.FORWARD_REVOKE);

          forwardReqMoreInfoDAO.revokeForwardDeleteEutNextRoleLineRMI(request, forReqMoreInfo, vars,
              inpRecordId, windowReference);

          forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(inpRecordId, windowReference);
        }
        if (forward != null) {
          EutForwardReqMoreInfo initialForward = OBDal.getInstance()
              .get(EutForwardReqMoreInfo.class, forward.getRequest());
          userFwdRmiId = initialForward.getUserContact().getId();
          roleFwdRmiId = initialForward.getRole().getId();
        }
        // ForwardRevoke
        if (request.getParameter("inpToRole").equals(roleFwdRmiId)
            && request.getParameter("inpToUser").equals(userFwdRmiId)) {
          chkRevokeCdn = true;
        }
        if (chkRevokeCdn) {

          String comments = "";
          if (request.getParameter("inpcomments") != null) {
            comments = request.getParameter("inpcomments");
          }
          // Update record status as 'DR'
          forReqMoreInfo = forwardReqMoreInfoDAO.revokeForwardUpdateRecord(windowReference, vars,
              inpRecordId, comments, chkRevokeCdn);
          // Delete forwarded EutNextRoleLine
          forwardReqMoreInfoDAO.revokeForwardDeleteEutNextRoleLine(request, forReqMoreInfo, vars,
              inpRecordId, windowReference);
          // Remove Forward_Rmi id from transaction screens
          forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(inpRecordId, windowReference);
          revoke = true;
        }

        else {
          // insert record in forward_rmi table
          forReqMoreInfo = forwardReqMoreInfoDAO.insertOrUpdateRecord(request, vars, inpRecordId,
              windowReference, doctype);
          // insert record in eut_next_role_line for to user, to whom forwarded the record and
          // update
          // forward_rmi id in table
          if (forReqMoreInfo != null) {
            eutnextroleline = forwardReqMoreInfoDAO.insertEutNextRoleLine(request, vars,
                inpRecordId, forReqMoreInfo, windowReference);

            // Find original alert and set status as solved and add next role reference in alert
            forwardReqMoreInfoDAO.setOriginalAlert(inpRecordId, eutnextroleline.getEUTNextRole());
          }
        }
        // insert record in action history
        forwardReqMoreInfoDAO.insertActionHistory(request, vars, inpRecordId, windowReference,
            Constants.FORWARD, forReqMoreInfo, revoke);

        // Give Role Access to Receiver
        if (forReqMoreInfo != null) {
          forwardReqMoreInfoDAO.giveRoleAccess(clientId, userId, forReqMoreInfo.getId(), doctype,
              inpwindowId, conn);
        }
        // alert process
        String UserName = usr.getName();
        forwardReqMoreInfoDAO.alertprocess(clientId, alertWindowType, forReqMoreInfo,
            Constants.FORWARD, Lang, UserName, windowName, doctype, revoke, request,
            windowReference);

        if (revoke) {
          List<Alert> originalList = forwardReqMoreInfoDAO.findOriginalAlert(inpRecordId,
              forwardReqMoreInfoDAO.getNextRole(inpRecordId, windowReference), windowReference,
              true);

          for (Alert alert : originalList) {
            alert.setAlertStatus("NEW");
            OBDal.getInstance().save(alert);
          }
          OBDal.getInstance().flush();
        }

        // -----------------
        // request.getRequestDispatcher(jspPage).include(request, response);

        OBError myMessage = new OBError();
        myMessage.setMessage(
            Utility.parseTranslation(this, vars, vars.getLanguage(), "@ESCM_Forward_Success@"));
        myMessage.setType(Utility.parseTranslation(this, vars, vars.getLanguage(), "Success"));
        vars.setMessage(tabId, myMessage);
        printPageClosePopUp(response, vars, "");
      }
    } catch (Exception e) {
      log4j.error("Exception in Forward :", e);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

}
