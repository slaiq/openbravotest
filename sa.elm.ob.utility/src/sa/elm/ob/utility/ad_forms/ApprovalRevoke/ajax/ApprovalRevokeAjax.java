package sa.elm.ob.utility.ad_forms.ApprovalRevoke.ajax;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;

import sa.elm.ob.utility.ad_forms.ApprovalRevoke.massrevoke.ApprovalRevokeVO;
import sa.elm.ob.utility.ad_forms.ApprovalRevoke.massrevoke.GetFactoryMassRevoke;
import sa.elm.ob.utility.ad_forms.ApprovalRevoke.massrevoke.MassRevoke;
import sa.elm.ob.utility.ad_forms.ApprovalRevoke.massrevoke.ProcessMassRevoke;

public class ApprovalRevokeAjax extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Inject
  @Any
  private Instance<ProcessMassRevoke> hooks;

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String lang = vars.getLanguage();
    Connection con = null;
    MassRevoke dao = null;
    try {
      con = getConnection();
      GetFactoryMassRevoke massRevoke = new GetFactoryMassRevoke(hooks);

      dao = massRevoke.getRevoke(request.getParameter("inpWindowId"), con);
      String action = (request.getParameter("action") == null ? ""
          : request.getParameter("action"));
      if (action.equals("GetRevokeRecords")) {
        log4j.debug("getting revokeRecords Ajax");
        String windowId = request.getParameter("inpWindowId");
        String sortColName = request.getParameter("sidx");
        String sortColType = request.getParameter("sord");
        String searchFlag = request.getParameter("_search");
        int rows = Integer.parseInt(request.getParameter("rows"));
        int page = Integer.parseInt(request.getParameter("page"));
        int totalPage = 1;
        int offset = 0;
        ApprovalRevokeVO vo = new ApprovalRevokeVO();
        if (searchFlag != null && searchFlag.equals("true")) {
          if (request.getParameter("org") != null)
            vo.setOrgName(request.getParameter("org").replace("'", "''"));
          if (request.getParameter("docno") != null)
            vo.setDocno(request.getParameter("docno").replace("'", "''"));
          if (request.getParameter("requester") != null)
            vo.setRequester(request.getParameter("requester").replace("'", "''"));
          if (request.getParameter("nextrole") != null)
            vo.setNextrole(request.getParameter("nextrole").replace("'", "''"));
          if (request.getParameter("lastperformer") != null)
            vo.setLastperfomer(request.getParameter("lastperformer").replace("'", "''"));
          if (request.getParameter("status") != null)
            vo.setStatus(request.getParameter("status").replace("'", "''"));
        }
        int totalRecord = dao.getRevokeRecordsCount(vars, vars.getClient(), windowId, searchFlag,
            vo);
        log4j.debug("totalRecord:" + totalRecord);
        if (totalRecord > 0) {
          totalPage = totalRecord / rows;
          if (totalRecord % rows > 0)
            totalPage += 1;
          offset = ((page - 1) * rows);

          if (page > totalPage) {
            page = totalPage;
            offset = ((page - 1) * rows);

          }
        } else {
          page = 0;
          totalPage = 0;
        }
        List<ApprovalRevokeVO> list = dao.getRevokeRecordsList(vars, vars.getClient(), windowId, vo,
            rows, offset, sortColName, sortColType, searchFlag, lang);
        response.setContentType("text/xml");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        StringBuffer xmlData = new StringBuffer("<?xml version='1.0' encoding='utf-8'?><rows>");
        log4j.debug("totalRecord:" + totalRecord + ">> list size" + list.size());
        if (list.size() > 0) {
          for (int i = 0; i < list.size(); i++) {
            ApprovalRevokeVO VO = (ApprovalRevokeVO) list.get(i);
            xmlData.append("<row id='" + VO.getRecordId() + "'>");
            xmlData.append("<cell><![CDATA[" + VO.getOrgName() + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + VO.getDocno() + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + VO.getRequester() + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + VO.getNextrole() + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + VO.getLastperfomer() + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + VO.getStatus() + "]]></cell>");

            xmlData.append("</row>");
          }
          xmlData.append("<page>" + page + "</page><total>" + totalPage + "</total><records>"
              + totalRecord + "</records>");
        } else
          xmlData
              .append("<page>" + 0 + "</page><total>" + 0 + "</total><records>" + 0 + "</records>");
        xmlData.append("</rows>");
        response.getWriter().write(xmlData.toString());
      }

    } catch (final Exception e) {
      log4j.error("Error in ApprovalRevokeAjax : ", e);
    } finally {
      try {
        if(con != null) {
          con.close();
        }
        
      } catch (final SQLException e) {
        log4j.error("Error in ApprovalRevokeAjax : ", e);
      }
    }
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    doPost(request, response);
  }

  public String getServletInfo() {
    return "ApprovelRevokeAjax Servlet";
  }
}
