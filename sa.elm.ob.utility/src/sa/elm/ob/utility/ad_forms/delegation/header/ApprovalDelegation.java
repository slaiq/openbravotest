package sa.elm.ob.utility.ad_forms.delegation.header;

import java.io.IOException;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.model.ad.access.User;

import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.header.DocumentRule;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.vo.DocumentRuleVO;
import sa.elm.ob.utility.ad_forms.delegation.dao.ApprovalDelegationDAO;
import sa.elm.ob.utility.ad_forms.delegation.vo.ApprovalDelegationVO;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

public class ApprovalDelegation extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private String includeIn = "";
  public static HashMap<String, LinkedHashMap<String, String>> mapLangDocType = new HashMap<String, LinkedHashMap<String, String>>();

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    Connection con = null;
    ApprovalDelegationDAO dao = null;
    String hijiriDate = "";
    VariablesSecureApp vars = new VariablesSecureApp(request);
    List<JSONObject> getLoggedUserRole = new ArrayList<JSONObject>();
    try {
      con = getConnection();
      if (con != null) {
        dao = new ApprovalDelegationDAO(con);
      }
      String action = (request.getParameter("inpAction") == null ? ""
          : request.getParameter("inpAction"));
      String headerId = (request.getParameter("headerId") == null ? ""
          : request.getParameter("headerId").equals("null") ? ""
              : request.getParameter("headerId"));
      if (mapLangDocType.get(vars.getLanguage()) != null) {
        mapLangDocType.remove(vars.getLanguage());
      }
      List<DocumentRuleVO> docTypeList = DocumentRule.getDocumentTypeList(vars,
          DocumentRule.ACCESSTYPE_DELEGATION, new DocumentRuleDAO(con, vars));
      LinkedHashMap<String, String> mapDocType = new LinkedHashMap<String, String>();
      for (DocumentRuleVO vo : docTypeList) {
        mapDocType.put(vo.getNo(), vo.getName());
      }
      mapLangDocType.put(vars.getLanguage(), mapDocType);

      if (request.getParameter("inpSave") != null
          && request.getParameter("inpSave").equals("Save")) {
        ApprovalDelegationVO hdVO = new ApprovalDelegationVO();
        hijiriDate = Utility.convertToGregorian(request.getParameter("inpFromDate"));
        String fromDate = hijiriDate;
        hijiriDate = Utility.convertToGregorian(request.getParameter("inpToDate"));
        String toDate = hijiriDate;
        String userId = request.getParameter("inpFromUserId");
        String roleId = request.getParameter("inpFromRoleId");

        hdVO.setFromDate(fromDate);
        hdVO.setToDate(toDate);
        hdVO.setUserId(userId);
        hdVO.setRoleId(roleId);

        int exist = 0;
        String doctypLs = "";

        String inpDocList = request.getParameter("inpDocList");
        List<ApprovalDelegationVO> docLs = null;
        if (inpDocList != null && !inpDocList.equals("")) {
          docLs = new ArrayList<ApprovalDelegationVO>();
          JSONObject json = new JSONObject(request.getParameter("inpDocList"));
          JSONArray arr = json.getJSONArray("Doctype");

          for (int i = 0; i < arr.length(); i++) {
            ApprovalDelegationVO docVO = new ApprovalDelegationVO();
            JSONObject jsob = arr.getJSONObject(i);
            docVO.setDocType(jsob.getString("DocNo"));
            docVO.setUserId(jsob.getString("ToUserId"));
            docVO.setRoleId(jsob.getString("ToRoleId"));
            doctypLs += "," + "'" + docVO.getDocType() + "'";
            docLs.add(docVO);
          }
        }
        // check already exist
        doctypLs = doctypLs.replaceFirst(",", "");
        if (dao.checkUserExist(hdVO, headerId, doctypLs))
          exist = 1;
        boolean save = false;
        if (exist == 0) {
          if (headerId.equals(""))
            headerId = dao.insertReqAppDelegate(hdVO, vars.getClient(), vars.getOrg(),
                vars.getUser());
          else
            headerId = dao.updateReqAppDelegate(hdVO, headerId, vars.getClient(), vars.getOrg(),
                vars.getUser());
          if (headerId != null && !headerId.equals("")) {
            save = dao.insertReqAppDelegateLn(docLs, vars.getClient(), vars.getOrg(),
                vars.getUser(), headerId);
          }
        }

        if (exist == 0) {
          if (save)
            request.setAttribute("Save", "1");
          else
            request.setAttribute("Save", "0");
        } else
          request.setAttribute("Save", "2");
      } else if (request.getParameter("inpSave") != null
          && request.getParameter("inpSave").equals("Delete")) {
        boolean delete = dao.deleteReqAppDelegate(headerId);
        if (delete)
          request.setAttribute("Delete", "1");
        else
          request.setAttribute("Delete", "0");
      }
      if (action.equals("") || action.equals("GridView")) {
        includeIn = "../web/sa.elm.ob.utility/jsp/delegation/ApprovalDelegationList.jsp";
      } else if (action.equals("EditView")) {
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
        String date = df.format(new Date());
        date = dateYearFormat.format(df.parse(date));
        date = UtilityDAO.convertTohijriDate(date);
        boolean isAdminUser = dao.checkAdminUser(vars.getClient(), vars.getOrg(), vars.getUser(),
            vars.getRole());
        if (headerId.equals("")) {
          request.setAttribute("inpNowDate", date);
          request.setAttribute("inpFromDate", date);
          request.setAttribute("inpToDate", date);
          User loggedUser = Utility.getObject(User.class, vars.getUser());
          if (loggedUser != null && loggedUser.getBusinessPartner() != null) {
            request.setAttribute("selectUsr", vars.getUser());
          } else {
            request.setAttribute("selectUsr", null);
          }
          List<JSONObject> userList = dao.getUsers(vars.getClient(), vars.getOrg(), true);
          if (userList != null && isAdminUser) {
            request.setAttribute("selectUsr", vars.getUser());
            request.setAttribute("RoleLs", dao.getRoles(vars.getUser(), true));
          } else {
            request.setAttribute("RoleLs", dao.getRoles(vars.getUser(), true));
            getLoggedUserRole = dao.getRoles(vars.getUser(), true);
            if (getLoggedUserRole.size() == 0) {
              request.setAttribute("selectUsr", null);
            }
          }
          request.setAttribute("selectRol", vars.getRole());
          request.setAttribute("Processed", "N");
        } else {
          DateFormat df1 = new SimpleDateFormat("dd-MM-yyyy");
          SimpleDateFormat dateYearFormat1 = new SimpleDateFormat("yyyy-MM-dd");
          String date1 = df1.format(new Date());
          date1 = dateYearFormat1.format(df.parse(date1));
          date1 = UtilityDAO.convertTohijriDate(date1);
          List<ApprovalDelegationVO> delLs = dao.getDelegateList(vars.getClient(), vars.getOrg(),
              headerId, null, null, 1, 1, null, vars.getUser(), vars.getRole());
          if (delLs != null) {
            if (delLs.size() > 0) {
              for (int i = 0; i < delLs.size(); i++) {
                ApprovalDelegationVO delVO = (ApprovalDelegationVO) delLs.get(0);
                request.setAttribute("inpNowDate", date1);
                request.setAttribute("inpFromDate", delVO.getFromDate());
                request.setAttribute("inpToDate", delVO.getToDate());
                request.setAttribute("selectUsr", delVO.getUserId());
                request.setAttribute("RoleLs", dao.getRoles(delVO.getUserId(), true));
                request.setAttribute("selectRol", delVO.getRoleId());
                request.setAttribute("Processed", delVO.getProcessed());
              }
            }
          }
        }
        request.setAttribute("isAdminUser", isAdminUser);
        request.setAttribute("headerId", headerId);
        request.setAttribute("UserLs", dao.getUsers(vars.getClient(), vars.getOrg(), true));
        request.setAttribute("ToUserLs", dao.getUsers(vars.getClient(), vars.getOrg(), false));

        includeIn = "../web/sa.elm.ob.utility/jsp/delegation/ApprovalDelegation.jsp";
      }
    } catch (Exception e) {
      log4j.error("Exception in ApprovalDelegation : ", e);
    } finally {
      try {
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.getRequestDispatcher(includeIn).include(request, response);
      } catch (Exception e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        log4j.error("Exception in ApprovalDelegation : ", e);
      }
    }
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    doPost(request, response);
  }

  public String getServletInfo() {
    return "ApprovalDelegation Header Servlet.";
  }
}