package sa.elm.ob.scm.ad_reports.CustodyCardReport;

import java.io.IOException;
import java.sql.Connection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;

import sa.elm.ob.scm.ad_reports.custodybarcodelabel.CustodyBarcodeLabelDAO;

public class CustodyCardReportAjax extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @SuppressWarnings("static-access")
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Connection con = null;
    CustodyCardReportDAO dao = null;
    CustodyBarcodeLabelDAO bdao = null;
    try {
      con = getConnection();
      dao = new CustodyCardReportDAO(con);
      bdao = new CustodyBarcodeLabelDAO(con);
      String action = (request.getParameter("action") == null ? ""
          : request.getParameter("action"));
      if (action.equals("getbeneficiary")) {
        JSONObject jsob = new JSONObject();
        jsob = dao.getBeneficiaryList(vars.getClient(), request.getParameter("searchTerm"),
            Integer.parseInt(request.getParameter("pageLimit")),
            Integer.parseInt(request.getParameter("page")), request.getParameter("inptype"),
            vars.getRole(), vars.getOrg());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsob.toString());
      } else if (action.equals("getDepartment")) {
        JSONObject jsob = new JSONObject();
        jsob = dao.getDepartmentList(vars.getClient(), request.getParameter("searchTerm"),
            Integer.parseInt(request.getParameter("pageLimit")),
            Integer.parseInt(request.getParameter("page")), request.getParameter("inptype"),
            request.getParameter("inpOrg"), vars.getRole());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsob.toString());
      } else if (action.equals("getProduct")) {
        JSONObject jsob = new JSONObject();
        jsob = dao.getProductList(vars.getClient(), request.getParameter("searchTerm"),
            Integer.parseInt(request.getParameter("pageLimit")),
            Integer.parseInt(request.getParameter("page")), vars.getRole());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsob.toString());
      } else if (action.equals("getItemProduct")) {
        JSONObject jsob = new JSONObject();
        jsob = dao.getItemCardProductList(vars.getClient(), request.getParameter("searchTerm"),
            Integer.parseInt(request.getParameter("pageLimit")),
            Integer.parseInt(request.getParameter("page")), vars.getRole());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsob.toString());
      } else if (action.equals("getwarehouse")) {
        JSONObject jsob = new JSONObject();
        jsob = dao.getWarehouse(vars.getClient(), request.getParameter("searchTerm"),
            Integer.parseInt(request.getParameter("pageLimit")),
            Integer.parseInt(request.getParameter("page")), vars.getRole());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsob.toString());
      } else if (action.equals("getwarehousename")) {
        JSONObject jsob = new JSONObject();
        jsob = dao.getWarehousename(vars.getClient(), request.getParameter("searchTerm"),
            Integer.parseInt(request.getParameter("pageLimit")),
            Integer.parseInt(request.getParameter("page")), request.getParameter("inptype"),
            vars.getRole());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsob.toString());
      } else if (action.equals("getBenfDetails")) {
        JSONObject benfObj = null;
        String mirId = request.getParameter("mirId");

        benfObj = bdao.getBenfDetails(mirId);
        log4j.debug("benfObj:" + benfObj.toString());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(benfObj.toString());
      } else if (action.equals("getTagList")) {
        JSONObject jsob = new JSONObject();
        jsob = bdao.getTagsList(vars.getRole(), request.getParameter("searchTerm"),
            Integer.parseInt(request.getParameter("pageLimit")),
            Integer.parseInt(request.getParameter("page")));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsob.toString());
      }
    } catch (final Exception e) {
      log4j.error("Exception in CustodyCardReportAjax : ", e);
    }
  }
}