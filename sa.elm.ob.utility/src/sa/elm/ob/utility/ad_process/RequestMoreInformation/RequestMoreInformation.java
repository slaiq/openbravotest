package sa.elm.ob.utility.ad_process.RequestMoreInformation;

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
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.procurement.Requisition;

import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.utility.EutForwardReqMoreInfo;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.PrimaryKeyDocumentTypeE;

public class RequestMoreInformation extends HttpSecureAppServlet {

  /**
   * Servlet implementation class used for request more information
   */
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.utility/jsp/EutRequestMoreInformation.jsp";
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

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String windowReference = null, doctype = null, tabId = null, type = null;
    try {
      OBContext.setAdminMode();
      // Localization support
      ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
      VariablesSecureApp vars = new VariablesSecureApp(request);
      String action = (request.getParameter("inpAction") == null ? ""
          : request.getParameter("inpAction"));
      /*
       * String inpRecordId = (request.getParameter("Escm_Bidmgmt_ID") == null ?
       * (request.getParameter("inpRecordId") == null ? "" : request.getParameter("inpRecordId")) :
       * request.getParameter("Escm_Bidmgmt_ID"));
       */
      String inpwindowId = (request.getParameter("inpwindowId") == null
          ? (request.getParameter("inpwindowId") == null ? "" : request.getParameter("inpwindowId"))
          : request.getParameter("inpwindowId"));
      Window windowObj = OBDal.getInstance().get(Window.class, inpwindowId);
      String windowName = forwardReqMoreInfoDAO.getWindowTranslationName(windowObj,
          vars.getLanguage());
      if (windowName == null) {
        windowName = windowObj.getName();
      }
      @SuppressWarnings("unused")
      String alertWindowType = null, alertRuleId = "";
      final String clientId = vars.getClient();
      final String userId = vars.getUser();
      final String roleId = vars.getRole();

      User usr = OBDal.getInstance().get(User.class, vars.getUser());
      String Lang = vars.getLanguage();
      Connection conn = OBDal.getInstance().getConnection();
      Requisition req = null;
      EscmProposalMgmt proMgmt = null;
      MaterialIssueRequest mir = null;
      EfinRDVTransaction rdv = null;
      Invoice purchaseinv = null;
      EutForwardReqMoreInfo forReqMoreInfo = null;
      EFINFundsReq fundsReqMgmt = null;
      @SuppressWarnings("unused")
      EutNextRole eutNextRole = null;
      PrimaryKeyDocumentTypeE e = PrimaryKeyDocumentTypeE.getWindowType(inpwindowId);
      e.getPrimaryKeyColumnName();
      String inpRecordId = (request.getParameter(e.getPrimaryKeyColumnName()) == null
          ? (request.getParameter("inpRecordId") == null ? "" : request.getParameter("inpRecordId"))
          : request.getParameter(e.getPrimaryKeyColumnName()));

      // For Receipt Delivery Verification both Transaction Version and Advance Version
      if (inpwindowId.equals(Constants.RDV_W)) {
        rdv = OBDal.getInstance().get(EfinRDVTransaction.class, inpRecordId);
        e = PrimaryKeyDocumentTypeE.getRdvSpecialCase(rdv.getEfinRdv().isAdvance());
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
      Boolean errorMessage = false;
      String rmiUserId = null;
      EutForwardReqMoreInfo rmi = forwardReqMoreInfoDAO.getReqMoreInfo(inpRecordId,
          windowReference);
      Boolean isRmiorForwardToOriginalUser = false;
      Boolean chkRevokeCdn = false;
      EutForwardReqMoreInfo responseObj = null;
      if (action.equals("")) {
        // Localization support
        if (rmi != null) {
          List<RequestMoreInformationVO> ls = ((ForwardRequestMoreInfoDAOImpl) forwardReqMoreInfoDAO)
              .getRequestDetails(inpRecordId, windowReference);
          for (RequestMoreInformationVO vo1 : ls) {
            if (vo1.getrmiRequest() != null) {
              request.setAttribute("inpUserId", vo1.getUserId());
              request.setAttribute("inpUserName", vo1.getUserName());
              request.setAttribute("inpRoleId", vo1.getRoleId());
              request.setAttribute("inpRoleName", vo1.getRoleName());
              request.setAttribute("inpRmiRequest", vo1.getrmiRequest());
              responseObj = forwardReqMoreInfoDAO.getRmiIdReqRes(inpRecordId, windowReference);
              if (responseObj != null && responseObj.getREQResponse().equals("RES")) {
                request.setAttribute("inpRmiReponse",
                    responseObj.getMessage().replace("\n", "\\n").replace("\r", "\\r"));
              }

              JSONObject jsob = new JSONObject(), posJsob = new JSONObject();
              jsob = forwardReqMoreInfoDAO.getUserDepartment(vo1.getUserId());
              if (jsob != null && jsob.length() > 0) {
                if (jsob.has("DeptId")) {
                  request.setAttribute("inpDeptId", jsob.getString("DeptId"));
                }
                if (jsob.has("DeptName")) {
                  request.setAttribute("inpDeptName", jsob.getString("DeptName"));
                }
              } else {
                request.setAttribute("inpDeptId", "");
                request.setAttribute("inpDeptName", "");
              }
              posJsob = forwardReqMoreInfoDAO.getUserPosition(vo1.getUserId());
              if (posJsob != null && posJsob.length() > 0) {
                if (posJsob.has("PositionName")) {
                  request.setAttribute("inpPositionName", posJsob.getString("PositionName"));
                }
              } else {
                request.setAttribute("inpPositionName", "");
              }
              break;
            }
          }
        }
        request.setAttribute("inpRecordId", inpRecordId);
        request.setAttribute("inpwindowId", inpwindowId);
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");

        request.getRequestDispatcher(jspPage).include(request, response);
      } else if (action.equals("checkRmiIdIsNull")) {
        try {
          JSONObject jsonResponse = null;
          boolean checkRmiId = true;
          EutForwardReqMoreInfo reqMoreinfo = forwardReqMoreInfoDAO.getReqMoreInfo(inpRecordId,
              windowReference);
          if (reqMoreinfo != null) {
            checkRmiId = false;
          }
          // EutNextRole nextRole = forwardReqMoreInfoDAO.getNextRole(inpRecordId, windowReference);
          // boolean checkRmiId = forwardReqMoreInfoDAO.isForwardRMIIdIsNull(nextRole,
          // vars.getRole(),
          // vars.getClient());

          jsonResponse = new JSONObject();
          jsonResponse.put("checkRmiId", checkRmiId);
          response.setCharacterEncoding("UTF-8");
          response.getWriter().write(jsonResponse.toString());

        } catch (final Exception e1) {
          log4j.error("Exception in check Rmi IdIsNull : ", e1);
        }
      } else if (action.equals("checkPreValidation")) {
        try {
          JSONObject jsonResponse = null;
          Boolean checkValidation = null, isResponse = false;
          String cUserId = vars.getUser();
          String corgId = vars.getOrg();
          String strResponse = request.getParameter("strRes");
          if (strResponse.length() > 0) {
            isResponse = true;
          }
          checkValidation = forwardReqMoreInfoDAO.responsePreValidation(inpRecordId, clientId,
              cUserId, roleId, corgId, doctype, windowReference, isResponse);
          jsonResponse = new JSONObject();
          jsonResponse.put("checkValidation", checkValidation);
          response.setCharacterEncoding("UTF-8");
          response.getWriter().write(jsonResponse.toString());

        } catch (final Exception e1) {
          log4j.error("Exception in check Pre Validation : ", e1);
        }
      } else if (action.equals("checkIsRmiAlreadyRevoked")) {
        try {
          JSONObject jsonResponse = null;
          String corgId = vars.getOrg();
          Boolean isRevoked = false;
          String strResponse = request.getParameter("strRes");
          if (strResponse.length() > 0) {
            isRevoked = forwardReqMoreInfoDAO.isRmiRevoked(inpRecordId, clientId, userId, roleId,
                corgId, doctype);
          }
          jsonResponse = new JSONObject();
          jsonResponse.put("checkIsRevoked", isRevoked);
          response.setCharacterEncoding("UTF-8");
          response.getWriter().write(jsonResponse.toString());
        } catch (final Exception e1) {
          log4j.error("checkIsRmiAlreadyRevoked : ", e1);
        }
      } else if (action.equals("checkRmiToRoleOrgAccess")) {
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
          log4j.error("checkRmiToRoleOrgAccess : ", e1);
        }
      }

      else if (action.equals("getUserList")) {
        JSONObject jsob = new JSONObject();
        if (rmi != null)
          rmiUserId = rmi.getUserContact().getId();
        jsob = forwardReqMoreInfoDAO.getUserList(vars.getClient(),
            request.getParameter("searchTerm"), Integer.parseInt(request.getParameter("pageLimit")),
            Integer.parseInt(request.getParameter("page")), vars.getUser(), rmiUserId, inpRecordId,
            windowReference, doctype, roleId, Constants.REQUEST_MORE_INFORMATION);
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
          Boolean errorMsg = false;
          // Throw error when preference is disabled for the selected user and role
          List<Preference> prefs = forwardDao.getPreferenceObj(vars.getClient(), true);
          String toUserId = request.getParameter("inpUser");
          String toRoleId = request.getParameter("inpRole");
          for (Preference preference : prefs) {
            if (preference.getUserContact() != null && preference.getVisibleAtRole() == null) {
              if (preference.getUserContact().getId().equals(toUserId)) {
                errorMsg = true;
                break;
              }
            }
            if (preference.getVisibleAtRole() != null) {
              if (preference.getVisibleAtRole().getId().equals(toRoleId)) {
                errorMsg = true;
                break;
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
          if (rmi != null) {
            userFwdRmiId = rmi.getUserContact().getId();
            roleFwdRmiId = rmi.getRole().getId();
            String forwardRmiUserId = forwardReqMoreInfoDAO.checkIsNestedForwardRmi(inpRecordId,
                rmi.getId());
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
          log4j.error("Exception in check checkUserValidation : ", e1);
        }
      } else if (action.equals("getResponseUser")) {
        JSONObject jsonResponse = new JSONObject();
        if (rmi != null) {
          jsonResponse = forwardReqMoreInfoDAO.getResponseUserAndRoles(rmi, false, inpRecordId,
              request.getParameter("inpUser").toString());
        }
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonResponse.toString());
      } else if (action.equals("getResponseUserRoles")) {
        JSONObject jsonResponse = new JSONObject();
        if (rmi != null) {
          jsonResponse = forwardReqMoreInfoDAO.getResponseUserAndRoles(rmi, true, inpRecordId,
              request.getParameter("inpUser").toString());
        }
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonResponse.toString());
      } else if (action.equals("getRmiRequestDetails")) {
        String reqMessage = "";
        JSONObject jsonResponse = new JSONObject();
        try {
          reqMessage = forwardReqMoreInfoDAO.getRmiRequestDetail(rmi, inpRecordId,
              request.getParameter("inpUser").toString(),
              request.getParameter("inpRole").toString());

          jsonResponse = new JSONObject();
          jsonResponse.put("reqMessage", reqMessage);
          response.setContentType("text/json");
          response.setCharacterEncoding("UTF-8");
          response.getWriter().write(jsonResponse.toString());

        } catch (final Exception e1) {
          log4j.error("Exception in getRmiRequestDetails in RequestMoreInformation : ", e1);
        }
      }

      else if (action.equals("insertRecord")) {
        String userFwdRmiId = null, roleFwdRmiId = null;

        forwardReqMoreInfoDAO.getForwardObj(inpRecordId, windowReference);

        /*
         * if (forward != null) { OBError myMessage = new OBError();
         * myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(),
         * "@Escm_AlreadyPreocessed_Approved@")); myMessage.setType(Utility.parseTranslation(this,
         * vars, vars.getLanguage(), "Error")); vars.setMessage(tabId, myMessage);
         * printPageClosePopUp(response, vars, ""); return; }
         */
        OBError myMessage = new OBError();
        eutNextRole = forwardReqMoreInfoDAO.getNextRole(inpRecordId, windowReference);

        if (!errorMessage) {
          String UserName = usr.getName();
          // insert record in forward_rmi table
          if (rmi == null) {
            forReqMoreInfo = forwardReqMoreInfoDAO.insertOrUpdateRecordRMI(request, vars,
                inpRecordId, Constants.REQUEST, windowReference, doctype);
          }
          if (rmi != null) {
            EutForwardReqMoreInfo initialForward = OBDal.getInstance()
                .get(EutForwardReqMoreInfo.class, rmi.getRequest());
            userFwdRmiId = initialForward.getUserContact().getId();
            roleFwdRmiId = initialForward.getRole().getId();
            // request send back to original forwarded user
            // check original requester user and selected user equal or not
            if (request.getParameter("inpToRole").equals(roleFwdRmiId)
                && request.getParameter("inpToUser").equals(userFwdRmiId)) {
              chkRevokeCdn = true;
            }
            if (request.getParameter("inpnestedRmi").equals("Y")) {
              // request send back to original forwarded user
              if (chkRevokeCdn) {
                // Update record status as 'DR' - Revoke operation
                forReqMoreInfo = forwardReqMoreInfoDAO.revokeForwardUpdateRecordRMI(windowReference,
                    vars, inpRecordId, request.getParameter("inprequest"), true, doctype,
                    chkRevokeCdn, Constants.REQUEST);
                isRmiorForwardToOriginalUser = true;
              } else {
                forReqMoreInfo = forwardReqMoreInfoDAO.insertOrUpdateRecordRMI(request, vars,
                    inpRecordId, Constants.REQUEST, windowReference, doctype);
              }
            } else {
              forReqMoreInfo = forwardReqMoreInfoDAO.insertOrUpdateRecordRMI(request, vars,
                  inpRecordId, Constants.RESPONSE, windowReference, doctype);
            }
          }
          // insert record in eut_next_role_line for to user, to whom forwarded the record and
          // update
          // forward_rmi id in table
          if (forReqMoreInfo != null && forReqMoreInfo.getREQResponse().equals(Constants.REQUEST)) {
            // request send back to original forwarded user
            if (chkRevokeCdn) {
              // Delete forwarded EutNextRoleLine
              forwardReqMoreInfoDAO.revokeForwardDeleteEutNextRoleLineRMI(request, forReqMoreInfo,
                  vars, inpRecordId, windowReference);
            } else {

              EutNextRoleLine eutnextroleline = forwardReqMoreInfoDAO.insertEutNextRoleLineRMI(
                  request, vars, inpRecordId, forReqMoreInfo, windowReference);

            }
            // Find original alert and set status as solved and add next role reference in alert
            forwardReqMoreInfoDAO.setOriginalAlert(inpRecordId,
                forwardReqMoreInfoDAO.getNextRole(inpRecordId, windowReference));

            forwardReqMoreInfoDAO.insertActionHistoryRMI(request, vars, inpRecordId, forReqMoreInfo,
                windowReference, Constants.REQUEST_MORE_INFORMATION, isRmiorForwardToOriginalUser);

            // request send back to original forwarded user
            if (chkRevokeCdn) {
              // Remove Forward_Rmi id from transaction screens
              forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(inpRecordId, windowReference);
            }

            // Give Role Access to Receiver
            forwardReqMoreInfoDAO.giveReqMoreInfoRoleAccess(clientId, userId,
                forReqMoreInfo.getId(), doctype, inpwindowId, conn);
            // alert process
            if (alertWindowType.split("_").length > 1) {
              alertWindowType = alertWindowType.split("_")[1].toString();
            }

            forwardReqMoreInfoDAO.alertprocess(clientId, alertWindowType, forReqMoreInfo,
                Constants.REQUEST_MORE_INFORMATION, Lang, UserName, windowName, doctype,
                isRmiorForwardToOriginalUser, request, windowReference);

          }
          if (forReqMoreInfo != null
              && forReqMoreInfo.getREQResponse().equals(Constants.RESPONSE)) {

            // Delete forwarded EutNextRoleLine
            forwardReqMoreInfoDAO.revokeForwardDeleteEutNextRoleLine(request, rmi, vars,
                inpRecordId, windowReference);

            forwardReqMoreInfoDAO.insertActionHistoryRMI(request, vars, inpRecordId, forReqMoreInfo,
                windowReference, Constants.REQUEST_MORE_INFORMATION, false);

            // forwardReqMoreInfoDAO.removeReqMoreInfoRoleAccess(clientId, rmi.getId(), conn);

            // alert process
            forwardReqMoreInfoDAO.alertprocess(clientId, alertWindowType, forReqMoreInfo,
                Constants.REQUEST_MORE_INFORMATION, Lang, UserName, windowName, doctype, false,
                request, windowReference);

            forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(inpRecordId, windowReference);

            // adding eut_nextrole if response not back to original user
            if (forReqMoreInfo != null) {
              EutForwardReqMoreInfo previousRmiReqObj = forwardReqMoreInfoDAO
                  .getPreviousRMIRecordBasedOnRequestId(request.getParameter("inpToUser"),
                      request.getParameter("inpToRole"), forReqMoreInfo, doctype, false);
              if (previousRmiReqObj != null
                  && !previousRmiReqObj.getRequest().equals(forReqMoreInfo.getRequest())) {
                forwardReqMoreInfoDAO.insertEutNextRoleLineRMI(request, vars, inpRecordId,
                    previousRmiReqObj, windowReference);
              } else {

                // // Find original alert and set status as solved and add next role reference in
                // alert
                // forwardReqMoreInfoDAO.setOriginalAlert(inpRecordId,
                // forwardReqMoreInfoDAO.getNextRole(inpRecordId, windowReference));

              }
            }

            forwardReqMoreInfoDAO.setRMIRecordStatusAsDraft(rmi,
                request.getParameter("inpToUser").toString(),
                request.getParameter("inpToRole").toString());
          }

          if (forReqMoreInfo != null && forReqMoreInfo.getREQResponse().equals(Constants.REQUEST)) {
            myMessage.setMessage(
                Utility.parseTranslation(this, vars, vars.getLanguage(), "@ESCM_RMIREQ_Success@"));
          }
          if (forReqMoreInfo != null
              && forReqMoreInfo.getREQResponse().equals(Constants.RESPONSE)) {
            myMessage.setMessage(
                Utility.parseTranslation(this, vars, vars.getLanguage(), "@ESCM_RMIRES_Success@"));
          }
          myMessage.setType(Utility.parseTranslation(this, vars, vars.getLanguage(), "Success"));
          vars.setMessage(tabId, myMessage);
          printPageClosePopUp(response, vars, "");
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in RequestMoreInformation :", e);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

}
