package sa.elm.ob.scm.ad_actionbutton.ajax;

import java.io.IOException;
import java.sql.Connection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ad_actionbutton.dao.ReceiptInspectionDAO;

/**
 * Servlet implementation class Receipt Inspection Ajax
 * 
 * @author Gopalakrishnan created on 22/02/2017
 */
public class ReceiptInspectionAjax extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger log = LoggerFactory.getLogger(ReceiptInspectionDAO.class);

  /**
   * @see HttpServlet#HttpServlet()
   */
  public ReceiptInspectionAjax() {
    super();
    // TODO Auto-generated constructor stub
  }

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
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String action = vars.getStringParameter("action");
    ReceiptInspectionDAO dao = null;
    Connection con = null;

    /*
     * Action to fetch the Inspection
     */
    if (action.equals("loadReceiptInspection")) {

      JSONObject jsonResponse = new JSONObject();
      try {
        con = getConnection();
        dao = new ReceiptInspectionDAO(con);
        jsonResponse.put("page", "0");
        jsonResponse.put("total", "0");
        jsonResponse.put("records", "0");
        jsonResponse.put("rows", new JSONArray());

        String inpReceiptId = vars.getStringParameter("inpReceiptId");
        if (inpReceiptId != null && inpReceiptId.length() == 32) {
          String searchFlag = request.getParameter("_search");
          JSONObject searchAttr = new JSONObject();
          searchAttr.put("rows", request.getParameter("rows").toString());
          searchAttr.put("page", request.getParameter("page").toString());
          searchAttr.put("search", searchFlag);
          searchAttr.put("sortName", request.getParameter("sidx").toString());
          searchAttr.put("sortType", request.getParameter("sord").toString());
          searchAttr.put("limit", "0");
          searchAttr.put("offset", "0");
          // searchAttr.put("isPayrollOfficer", request.getParameter("isPayrollOfficer"));
          log4j.debug("ispayrollofficer:" + request.getParameter("isPayrollOfficer"));
          if (Boolean.valueOf(searchFlag)) {
            if (!StringUtils.isEmpty(request.getParameter("Item")))
              searchAttr.put("Item", request.getParameter("Item").replace("'", "''"));
          }
          jsonResponse = dao.getInspectedRecord(vars.getClient(), inpReceiptId, searchAttr);

        }
      } catch (final Exception e) {
        log.error("Exception in PayrollProcess - GetEmployee : ", e);
      } finally {
        try{
          if(con!=null){
            con.close();
          }
        }
        catch(Exception e){
          
        }
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-cache");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonResponse.toString());
      }

    }

  }
}
