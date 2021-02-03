package sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.header;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;

import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.vo.DocumentRuleVO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.Utility;

public class DocumentRule extends HttpSecureAppServlet {

  private static final long serialVersionUID = 1L;
  private static Logger log4j = Logger.getLogger(DocumentRule.class);
  public static final String servletPath = "/sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.header/DocumentRule";

  public static final String ACCESSTYPE_RULE = "1";
  public static final String ACCESSTYPE_PROJECT_RULE = "2";
  public static final String ACCESSTYPE_PROJECT_ACCESS = "3";
  public static final String ACCESSTYPE_DELEGATION = "4";
  public static final String ACCESSTYPE_EMAILSUBSCRIPTION = "5";
  public static HashMap<String, String> mapRuleClass = null;
  public static HashMap<String, Method> mapRuleMethod = null;

  /**
   * This class is used for Document rule process
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    doPost(request, response);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = null;
    RequestDispatcher dispatch = null;
    DocumentRuleDAO dao = null;
    Connection con = null;
    String action = null;
    boolean isAjaxReq = false;
    String lang = "";
    try {
      vars = new VariablesSecureApp(request);
      con = getConnection();
      dao = new DocumentRuleDAO(con, vars);
      action = (request.getParameter("inpAction") == null ? "" : request.getParameter("inpAction"));
      lang = vars.getLanguage().toString();
      isAjaxReq = ((request.getParameter("requestType") == null
          || !"A".equals(request.getParameter("requestType").toString())) ? false
              : ("A".equals(request.getParameter("requestType").toString()) ? true : false));
      if ("".equals(action)) {

      } else if ("Submit".equals(action)) {
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-cache");
        JSONObject result = new JSONObject();
        DocumentRuleVO ruleVO = null;
        try {
          List<DocumentRuleVO> list = new ArrayList<DocumentRuleVO>();
          String inpDocumentType = request.getParameter("inpDocumentType");
          String inpOrganization = request.getParameter("inpOrganization");
          String inpIsMultiRule = request.getParameter("inpMultiRule");
          JSONObject inpRuleData = new JSONObject(request.getParameter("inpRuleData"));
          JSONArray arr = inpRuleData.getJSONArray("DT" + inpDocumentType), jarr = null,
              jarr1 = null;
          for (int i = 0; i < arr.length(); i++) {
            JSONObject jsob = arr.getJSONObject(i);
            BigDecimal ruleValue = new BigDecimal(jsob.getString("value"));
            int ruleSeqNo = jsob.getInt("row");

            jarr = jsob.getJSONArray("roleList");
            for (int j = 0; j < jarr.length(); j++) {
              JSONObject jsob1 = jarr.getJSONObject(j);
              String key = "";
              int roleSeqNo = 0;
              @SuppressWarnings("unchecked")
              Iterator<String> keys = jsob1.keys();
              while (keys.hasNext()) {
                key = (String) keys.next();
              }
              roleSeqNo = Integer.parseInt(key.replace("role", ""));
              jarr1 = jarr.getJSONObject(j).getJSONArray(key);
              for (int k = 0; k < jarr1.length(); k++) {
                JSONObject jsob2 = jarr1.getJSONObject(k);
                int roleOrderNo = jsob2.getInt("no");
                String roleId = jsob2.getString("role");
                boolean allowReservation = jsob2.getBoolean("reservation");
                boolean contractcategoryCheck = jsob2.optBoolean("contractcategory");

                ruleVO = new DocumentRuleVO();
                ruleVO.setRuleValue(ruleValue);
                ruleVO.setRuleSequenceNo(ruleSeqNo);
                ruleVO.setId(roleId);
                ruleVO.setRoleSequenceNo(roleSeqNo);
                ruleVO.setRoleOrderNo(roleOrderNo);
                ruleVO.setAllowReservation(allowReservation ? "Y" : "N");
                ruleVO.setIsMultiRule(inpIsMultiRule);
                ruleVO.setIscontractcategory(contractcategoryCheck ? "Y" : "N");
                if (jsob.has("Requester_Role") && inpIsMultiRule.equals("Y")) {
                  ruleVO.setRequester(jsob.getString("Requester_Role"));
                }
                list.add(ruleVO);
              }
            }
          }
          int res = dao.insertDocumentRule(inpDocumentType, list, inpOrganization, inpIsMultiRule);
          if (res == 1) {
            result.put("result", "1");
            result.put("msgtype", "S");
            result.put("msgtypetitle", Resource.getProperty("utility.success", lang));
            result.put("msg",
                Resource.getProperty("utility.accesscontrol.documentrule.submit.success", lang));
            result.put("ruleList", getRuleList(vars, dao, inpDocumentType, inpOrganization));
          } else if (res == -1) {
            result.put("result", "1");
            result.put("msgtype", "W");
            result.put("msgtypetitle", Resource.getProperty("utility.warning", lang));
            result.put("msg", Resource
                .getProperty("utility.accesscontrol.documentrule.submit.error.approve", lang));
            result.put("ruleList", getRuleList(vars, dao, inpDocumentType, inpOrganization));
          } else if (res == 0) {
            result.put("result", "0");
            result.put("msgtype", "E");
            result.put("msgtypetitle", Resource.getProperty("utility.error", lang));
            result.put("msg",
                Resource.getProperty("utility.accesscontrol.documentrule.submit.error", lang));
          } else if (res == 4) {
            result.put("result", "0");
            result.put("msgtype", "E");
            result.put("msgtypetitle", Resource.getProperty("utility.error", lang));
            result.put("msg", Resource.getProperty(
                "utility.accesscontrol.documentrule.validation.error.duplicatemultirole", lang));
          }
        } catch (final Exception e) {
          log4j.error("Exception in DocumentRule Submit : ", e);
          result.put("result", "0");
          result.put("msgtype", "E");
          result.put("msgtypetitle", Resource.getProperty("utility.error", lang));
          result.put("msg",
              Resource.getProperty("utility.accesscontrol.documentrule.submit.error", lang));
        } finally {
          if (isAjaxReq)
            response.getWriter().write(result.toString());
        }
      } else if ("GetRuleList".equals(action)) {
        String ruleList = "";
        try {
          String inpDocumentType = request.getParameter("inpDocumentType");
          String inpOrganization = request.getParameter("inpOrganization");
          ruleList = getRuleList(vars, dao, inpDocumentType, inpOrganization);
        } catch (final Exception e) {
          log4j.error("Exception in DocumentRule GetRuleList : ", e);
        } finally {
          response.setContentType("application/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(ruleList);
        }
      }
      if (isAjaxReq == false) {
        getRoleList(request, vars);
        getOrganizationList(request, dao);
        request.setAttribute("DocumentTypeList",
            DocumentRule.getDocumentTypeList(vars, DocumentRule.ACCESSTYPE_RULE, dao));
        request.setAttribute("ServletPath", servletPath);
        request.setAttribute("RuleApprovalLength", 9);
        dispatch = request
            .getRequestDispatcher("../web/sa.elm.ob.utility/jsp/accesscontrol/DocumentRule.jsp");
      }
    } catch (final Exception e) {
      log4j.error("Exception in DocumentRule doPost : ", e);
    } finally {
      try {
        if (dispatch != null && isAjaxReq == false) {
          response.setContentType("text/html; charset=UTF-8");
          response.setCharacterEncoding("UTF-8");
          dispatch.include(request, response);
        }
      } catch (final Exception e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        log4j.error("Exception in DocumentRule doPost : ", e);
      }
    }
  }

  public String getServletInfo() {
    return "DocumentRule Servlet";
  }

  /**
   * This method is used to get Rule List
   * 
   * @param vars
   * @param dao
   * @param inpDocumentType
   * @param inpOrganization
   * @return list
   */
  private String getRuleList(VariablesSecureApp vars, DocumentRuleDAO dao, String inpDocumentType,
      String inpOrganization) {
    JSONObject jsOB = new JSONObject();
    String strMultiRule = "N";

    try {
      List<DocumentRuleVO> list = dao.getDocumentRuleList(inpDocumentType, inpOrganization);
      JSONObject ruleOBJ = null, roleOBJ = null, roleOrderOBJ = null;
      JSONArray ruleArray = new JSONArray(), roleArray = null, roleOrderArray = null;

      if (inpDocumentType.equals("EUT_112") || inpDocumentType.equals("EUT_113")
          || inpDocumentType.equals("EUT_115") || inpDocumentType.equals("EUT_111")
          || inpDocumentType.equals("EUT_118") || inpDocumentType.equals("EUT_116"))
        strMultiRule = "Y";

      if (list.size() > 0) {
        int lastRuleNo = 0, lastRoleSeqNo = 0;
        for (DocumentRuleVO ruleVO : list) {
          strMultiRule = ruleVO.getIsMultiRule();
          if (lastRuleNo == 0) {
            ruleOBJ = new JSONObject();
            ruleOBJ.put("id", "RuleID" + ruleVO.getRuleSequenceNo());
            ruleOBJ.put("value", Utility.getNumberFormat(vars, Utility.numberFormat_PriceEdition,
                ruleVO.getRuleValue()));
            ruleOBJ.put("row", ruleVO.getRuleSequenceNo());
            ruleOBJ.put("Reservation_role", ruleVO.getReservation_role());
            ruleOBJ.put("Requester_Role", ruleVO.getRequester());
            ruleOBJ.put("contractcategory_role", ruleVO.getContractcategory_role());
            ruleOBJ.put("contractcategory", ruleVO.getIscontractcategory());

            roleArray = new JSONArray();
            roleOrderArray = new JSONArray();
          } else if (ruleVO.getRuleSequenceNo() == lastRuleNo) {
            if (ruleVO.getRoleSequenceNo() != lastRoleSeqNo) {
              roleOBJ = new JSONObject();
              roleOBJ.put("role" + lastRoleSeqNo, roleOrderArray);
              roleArray.put(roleOBJ);
              roleOrderArray = new JSONArray();
            }
          } else if (ruleVO.getRuleSequenceNo() != lastRuleNo) {
            roleOBJ = new JSONObject();
            roleOBJ.put("role" + lastRoleSeqNo, roleOrderArray);
            roleArray.put(roleOBJ);
            ruleOBJ.put("roleList", roleArray);
            ruleArray.put(ruleOBJ);

            ruleOBJ = new JSONObject();
            ruleOBJ.put("id", "RuleID" + ruleVO.getRuleSequenceNo());
            ruleOBJ.put("value", Utility.getNumberFormat(vars, Utility.numberFormat_PriceEdition,
                ruleVO.getRuleValue()));
            ruleOBJ.put("row", ruleVO.getRuleSequenceNo());
            ruleOBJ.put("Reservation_role", ruleVO.getReservation_role());
            ruleOBJ.put("Requester_Role", ruleVO.getRequester());
            ruleOBJ.put("contractcategory_role", ruleVO.getContractcategory_role());
            ruleOBJ.put("contractcategory", ruleVO.getIscontractcategory());

            roleArray = new JSONArray();
            roleOrderArray = new JSONArray();
          }
          roleOrderOBJ = new JSONObject();
          roleOrderOBJ.put("no", ruleVO.getRoleOrderNo());
          roleOrderOBJ.put("role", ruleVO.getId());
          roleOrderOBJ.put("reservation",
              ruleVO.getAllowReservation().equals("N") ? Boolean.FALSE : Boolean.TRUE);
          if (ruleVO.getId() != null) {
            Role role = OBDal.getInstance().get(Role.class, ruleVO.getId());
            if (role != null) {
              boolean isDummy = (role.isEscmIshrlinemanager() == null ? false
                  : role.isEscmIshrlinemanager())
                  || (role.isEscmIsspecializeddept() == null ? false
                      : role.isEscmIsspecializeddept());

              roleOrderOBJ.put("isDummy", isDummy);
            }
          }
          roleOrderArray.put(roleOrderOBJ);

          lastRuleNo = ruleVO.getRuleSequenceNo();
          lastRoleSeqNo = ruleVO.getRoleSequenceNo();
        }
        {
          roleOBJ = new JSONObject();
          roleOBJ.put("role" + lastRoleSeqNo, roleOrderArray);
          roleArray.put(roleOBJ);
        }
        ruleOBJ.put("roleList", roleArray);
        ruleArray.put(ruleOBJ);
      } else {
        ruleOBJ = new JSONObject();
        ruleOBJ.put("id", "RuleID1");
        ruleOBJ.put("value", "0.00");
        ruleOBJ.put("row", "1");
        ruleOBJ.put("Requester_Role", "");
        ruleOBJ.put("Reservation_role", "");
        ruleOBJ.put("contractcategory_role", "");

        ruleOBJ.put("roleList", new JSONArray());
        ruleArray.put(ruleOBJ);
      }
      jsOB.put("DT" + inpDocumentType, ruleArray);
      jsOB.put("MultiRule", strMultiRule);
    } catch (final Exception e) {
      log4j.error("Exception in DocumentRule getRoleList : ", e);
      return "";
    }
    return jsOB.toString();
  }

  /**
   * This method is used to get role list
   * 
   * @param request
   * @param vars
   * @return boolean
   */
  private boolean getRoleList(HttpServletRequest request, VariablesSecureApp vars) {
    JSONObject jsOB = null, jsOB1 = null;
    JSONArray jsAR = null;
    try {
      jsOB = new JSONObject();
      jsAR = new JSONArray();
      List<DocumentRuleVO> ruleList = DocumentRuleDAO.getRoleList(vars);
      for (DocumentRuleVO ruleVO : ruleList) {
        jsOB1 = new JSONObject();
        jsOB1.put("id", ruleVO.getId());
        jsOB1.put("name", ruleVO.getName());
        jsOB1.put("isDummyRole", ruleVO.getIsDummyRole());
        jsAR.put(jsOB1);
      }
      jsOB.put("RoleList", jsAR);
      request.setAttribute("RoleList", ruleList);
      request.setAttribute("RoleListJSON", jsOB.toString());
    } catch (final Exception e) {
      log4j.error("Exception in DocumentRule getRoleList : ", e);
      return false;
    }
    return true;
  }

  /**
   * This method is used to get organization list
   * 
   * @param request
   * @param dao
   * @return boolean
   */
  private boolean getOrganizationList(HttpServletRequest request, DocumentRuleDAO dao) {
    JSONObject jsOB = null, jsOB1 = null;
    JSONArray jsAR = null;
    List<JSONObject> list = null;
    try {
      jsOB = new JSONObject();
      jsAR = new JSONArray();
      list = new ArrayList<JSONObject>();
      List<DocumentRuleVO> orgList = dao.getOrganisationList();
      for (DocumentRuleVO orgVO : orgList) {
        jsOB1 = new JSONObject();
        jsOB1.put("id", orgVO.getId());
        jsOB1.put("name", orgVO.getName());
        list.add(jsOB1);
        jsAR.put(jsOB1);
      }
      jsOB.put("OrganizationList", jsAR);
      request.setAttribute("OrganizationList", list);
      request.setAttribute("OrganizationListJSON", jsOB.toString());
    } catch (final Exception e) {
      log4j.error("Exception in DocumentRule getOrganizationList : ", e);
      return false;
    }
    return true;
  }

  /**
   * This method is used to get document type
   * 
   * @param vars
   * @param accessType
   * @param dao
   * @return
   */
  public static List<DocumentRuleVO> getDocumentTypeList(VariablesSecureApp vars, String accessType,
      DocumentRuleDAO dao) {
    List<DocumentRuleVO> list = null;
    try {
      list = new ArrayList<DocumentRuleVO>();
      list = dao.getDocumentTypeList(vars);
    } catch (final Exception e) {
      log4j.error("Exception in getDocumentTypeList : ", e);
      return list;
    }
    return list;
  }

}