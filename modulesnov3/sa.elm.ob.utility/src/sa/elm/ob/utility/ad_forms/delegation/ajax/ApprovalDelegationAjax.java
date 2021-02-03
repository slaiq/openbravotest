package sa.elm.ob.utility.ad_forms.delegation.ajax;

import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;

import sa.elm.ob.utility.ad_forms.delegation.dao.ApprovalDelegationDAO;
import sa.elm.ob.utility.ad_forms.delegation.vo.ApprovalDelegationVO;
import sa.elm.ob.utility.util.Utility;

public class ApprovalDelegationAjax extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Connection con = null;
    ApprovalDelegationDAO dao = null;

    try {
      con = getConnection();
      if (con != null) {
        dao = new ApprovalDelegationDAO(con);
      }
      String action = (request.getParameter("inpAction") == null ? ""
          : request.getParameter("inpAction"));

      if (action.equals("getDocList")) {
        List<ApprovalDelegationVO> docLs = null;

        try {
          String sortColName = request.getParameter("sidx");
          String sortType = request.getParameter("sord");
          String headerId = request.getParameter("headerId");
          String type = request.getParameter("type");
          String role = request.getParameter("roleId");
          String user = request.getParameter("userId");

          int rows, page;
          try {
            page = Integer.parseInt(request.getParameter("page"));
          } catch (Exception nullexp) {
            page = 1;
          }
          try {
            rows = Integer.parseInt(request.getParameter("rows"));
          } catch (Exception nullexp) {
            rows = 20;
          }
          docLs = dao.getDocuments(vars, headerId, vars.getClient(), vars.getOrg(), sortColName,
              sortType, rows, page, type, role, user);
          int pages = 0, totalpage = 0, count = 0;
          response.setContentType("text/xml");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          StringBuffer xmlData = new StringBuffer("<?xml version='1.0' encoding='utf-8'?><rows>");

          if (docLs != null) {
            if (docLs.size() > 0) {
              for (ApprovalDelegationVO vo : docLs) {
                pages = vo.getPage();
                totalpage = vo.getTotalpages();
                count = vo.getCount();

                xmlData.append("<row id = '" + vo.getDocType() + "' >");
                xmlData.append("<cell><![CDATA[" + vo.getDocName() + "]]></cell>");
                xmlData.append("<cell><![CDATA[" + vo.getUserName() + "]]></cell>");
                xmlData.append("<cell><![CDATA[" + "" + "]]></cell>");
                xmlData.append("<cell><![CDATA[" + vo.getUserId() + "]]></cell>");
                xmlData.append("<cell><![CDATA[" + vo.getType() + "]]></cell>");
                if (vo.getUserId().equals(""))
                  xmlData.append("<cell><![CDATA[" + "<select name='" + vo.getDocType() + "' id='"
                      + vo.getDocType()
                      + "_torole' style='width: 400px' class='editable ui-jqgrid-celledit' onchange='onChangeField();'></select>"
                      + "]]></cell>");
                else {
                  xmlData.append("<cell><![CDATA[" + "<select name='" + vo.getDocType() + "' id='"
                      + vo.getDocType()
                      + "_torole' style='width: 400px' class='editable ui-jqgrid-celledit' onchange='onChangeField();'>");
                  List<JSONObject> rolLs = dao.getRoles(vo.getUserId(), false);
                  if (rolLs != null) {
                    if (rolLs.size() > 0) {
                      for (JSONObject rolVO : rolLs) {
                        xmlData.append("<option value='" + rolVO.getString("id")
                            + "' role='option'>" + rolVO.getString("name") + "</option>");
                      }
                    }
                  }
                  xmlData.append("</select>" + "]]></cell>");
                }
                xmlData.append("<cell><![CDATA[" + vo.getRoleId() + "]]></cell>");
                xmlData.append("<cell><![CDATA[" + vo.getRoleName() + "]]></cell>");
                xmlData.append("</row>");

              }
              xmlData.append("<page>" + pages + "</page>");
              xmlData.append("<total>" + totalpage + "</total>");
              xmlData.append("<records>" + count + "</records>");
            } else {
              xmlData.append("<page>" + pages + "</page>");
              xmlData.append("<total>" + totalpage + "</total>");
              xmlData.append("<records>" + count + "</records>");
            }
            xmlData.append("</rows>");
            response.getWriter().write(xmlData.toString());
          }
        } catch (Exception e) {
          log4j.error(" Exception in action : getDocList ", e);
        }
      } else if (action.equals("getHeaderList")) {
        try {
          String sortColName = request.getParameter("sidx");
          String sortType = request.getParameter("sord");
          String hijiriDate = "";
          ApprovalDelegationVO searchVO = null;
          if (request.getParameter("_search").equals("true")) {
            searchVO = new ApprovalDelegationVO();
            if (request.getParameter("from_date") != null
                && !request.getParameter("from_date").equals("")) {
              hijiriDate = Utility.convertToGregorian(request.getParameter("from_date"));
              searchVO.setFromDate(request.getParameter("from_date_s") + "##" + hijiriDate);
            }
            if (request.getParameter("to_date") != null
                && !request.getParameter("to_date").equals("")) {
              hijiriDate = Utility.convertToGregorian(request.getParameter("to_date"));
              searchVO.setToDate(request.getParameter("to_date_s") + "##" + hijiriDate);
            }
            if (request.getParameter("name") != null && !request.getParameter("name").equals(""))
              searchVO.setUserName(request.getParameter("name"));
            if (request.getParameter("rolename") != null
                && !request.getParameter("rolename").equals(""))
              searchVO.setRoleName(request.getParameter("rolename"));
          }

          int rows, page;
          try {
            page = Integer.parseInt(request.getParameter("page"));
          } catch (Exception nullexp) {
            page = 1;
          }
          try {
            rows = Integer.parseInt(request.getParameter("rows"));
          } catch (Exception nullexp) {
            rows = 20;
          }
          List<ApprovalDelegationVO> delLs = dao.getDelegateList(vars.getClient(), vars.getOrg(),
              null, sortColName, sortType, rows, page, searchVO, vars.getUser(), vars.getRole());
          int pages = 0, totalpage = 0, count = 0;
          response.setContentType("text/xml");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          StringBuffer xmlData = new StringBuffer("<?xml version='1.0' encoding='utf-8'?><rows>");

          if (delLs != null) {
            if (delLs.size() > 0) {
              for (ApprovalDelegationVO vo : delLs) {
                pages = vo.getPage();
                totalpage = vo.getTotalpages();
                count = vo.getCount();

                xmlData.append("<row id = '" + vo.getHeaderId() + "' >");
                xmlData.append("<cell><![CDATA[" + vo.getFromDate() + "]]></cell>");
                xmlData.append("<cell><![CDATA[" + vo.getToDate() + "]]></cell>");
                xmlData.append("<cell><![CDATA[" + vo.getUserName() + "]]></cell>");
                xmlData.append("<cell><![CDATA[" + vo.getRoleName() + "]]></cell>");
                xmlData.append("<cell><![CDATA[" + vo.getProcessed() + "]]></cell>");
                xmlData.append("</row>");

              }
              xmlData.append("<page>" + pages + "</page>");
              xmlData.append("<total>" + totalpage + "</total>");
              xmlData.append("<records>" + count + "</records>");
            } else {
              xmlData.append("<page>" + pages + "</page>");
              xmlData.append("<total>" + totalpage + "</total>");
              xmlData.append("<records>" + count + "</records>");
            }
            xmlData.append("</rows>");
            response.getWriter().write(xmlData.toString());
          }
        } catch (Exception e) {
          log4j.error(" Exception in action : getHeaderList ", e);
        }
      } else if (action.equals("validateToUserDelegated")) {
        JSONObject responseObject = null;
        String doctypLs = "", toUserId = "";
        String inpFromDate = Utility.convertToGregorian(request.getParameter("inpFromDate"));
        log4j.debug("inpFromDate" + inpFromDate);
        String inpToDate = Utility.convertToGregorian(request.getParameter("inpToDate"));
        log4j.debug("inpToDate" + inpToDate);
        String inpDocList = request.getParameter("inpDocList");
        String inpHeaderId = request.getParameter("inpHeaderId");
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
            toUserId += "," + "'" + docVO.getUserId() + "'";
            doctypLs += "," + "'" + docVO.getDocType() + "'";
            docLs.add(docVO);
          }
        }
        doctypLs = doctypLs.replaceFirst(",", "");
        toUserId = toUserId.replaceFirst(",", "");
        String inpUserId = request.getParameter("inpFromUserId");
        responseObject = dao.touserDelegatedDetails(inpFromDate, inpToDate, toUserId, doctypLs,
            inpUserId, inpHeaderId);
        log4j.debug("responseObject.toString():" + responseObject.toString());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(responseObject.toString());

      } else if (action.equals("validateUser")) {
        JSONObject responseObject = null;
        String doctypLs = "", toUserId = "";
        String inpFromDate = Utility.convertToGregorian(request.getParameter("inpFromDate"));
        log4j.debug("inpFromDate" + inpFromDate);
        String inpToDate = Utility.convertToGregorian(request.getParameter("inpToDate"));
        log4j.debug("inpToDate" + inpToDate);
        String inpUserId = request.getParameter("inpFromUserId");
        String inpRoleId = request.getParameter("inpFromRoleId");
        String inpDocList = request.getParameter("inpDocList");
        String inpHeaderId = request.getParameter("inpHeaderId");
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
            toUserId += "," + "'" + docVO.getUserId() + "'";
            doctypLs += "," + "'" + docVO.getDocType() + "'";
            docLs.add(docVO);
          }
        }
        doctypLs = doctypLs.replaceFirst(",", "");
        toUserId = toUserId.replaceFirst(",", "");
        responseObject = dao.userdetails(inpFromDate, inpToDate, inpUserId, inpRoleId, doctypLs,
            inpHeaderId);
        log4j.debug("responseObject.toString():" + responseObject.toString());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(responseObject.toString());

      } else if (action.equals("validateRecord")) {
        JSONObject responseObject = null;
        String inpDelegatId = request.getParameter("inpDelegatId");

        responseObject = dao.getRecordDetails(inpDelegatId);
        log4j.debug("responseObject.toString():" + responseObject.toString());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(responseObject.toString());

      } else if (action.equals("getToUsers")) {
        JSONObject jsonResponse = new JSONObject();
        JSONArray jsonArray = null;
        try {
          jsonArray = new JSONArray();
          List<JSONObject> toUserLs = dao.getUsers(vars.getClient(), vars.getOrg(), false);
          if (toUserLs != null && toUserLs.size() > 0) {
            for (JSONObject toUsrVO : toUserLs) {
              jsonResponse = new JSONObject();
              jsonResponse.put("UserId", toUsrVO.getString("id"));
              jsonResponse.put("UserName", toUsrVO.getString("name"));
              jsonArray.put(jsonResponse);
            }
          }
          response.setContentType("application/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(jsonArray.toString());
        } catch (Exception e) {
          log4j.error("Exception in AddNotes", e);
        }
      } else if (action.equals("getRoles")) {
        JSONObject jsonResponse = new JSONObject();
        JSONArray jsonArray = null;
        try {
          jsonArray = new JSONArray();
          boolean includeDocRule = true;
          if (request.getParameter("type").equals("l"))
            includeDocRule = false;
          List<JSONObject> toUserLs = dao.getRoles(request.getParameter("userId"), includeDocRule);
          if (toUserLs != null && toUserLs.size() > 0) {
            for (JSONObject toUsrVO : toUserLs) {
              jsonResponse = new JSONObject();
              jsonResponse.put("RoleId", toUsrVO.getString("id"));
              jsonResponse.put("RoleName", toUsrVO.getString("name"));
              jsonArray.put(jsonResponse);
            }
          }
          response.setContentType("application/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(jsonArray.toString());
        } catch (Exception e) {
          log4j.error("Exception in AddNotes", e);
        }
      } else if (action.equals("processedRecord")) {
        int status = 0;
        JSONObject object = new JSONObject();

        try {
          status = dao.processedRecord(request.getParameter("inpHeaderId"));
          log4j.debug("St" + status);
          object.put("count", status);
          response.setContentType("application/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(object.toString());
        } catch (Exception e) {
          log4j.error("Exception in updateAsProcessed", e);
        }
      } else if (action.equals("isProcessed")) {
        JSONObject isProcessedRcd;
        isProcessedRcd = dao.checkIsProcessed(request.getParameter("inpheaderID"));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(isProcessedRcd.toString());
      }

    } catch (Exception e) {
      log4j.error("", e);
    }
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    doPost(request, response);
  }

  public String getServletInfo() {
    return "Ajax Servlet. This Servlet was made by Qualian";
  }
}
