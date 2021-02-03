package sa.elm.ob.scm.ad_reports.ReturnedItemsReport;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;

/**
 * 
 * 
 */
public class ReturnedItemsReport extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.scm/jsp/ReturnedItemsReport/ReturnedItemsReport.jsp";

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      OBContext.setAdminMode();
      String strReportName = "";
      String action = (request.getParameter("inpAction") == null ? "" : request
          .getParameter("inpAction"));
      VariablesSecureApp vars = new VariablesSecureApp(request);
      Connection con = getConnection();
      String inpClientId = "";
      String roleid = "";
      inpClientId = vars.getClient();
      roleid = vars.getRole();

      log4j.debug("action");
      if (action.equals("")) {
        List<ReturnedItemsReportVO> ls = new ArrayList<ReturnedItemsReportVO>();
        ReturnedItemsReportVO vo = null;

        st = con
            .prepareStatement("select m_warehouse_id,name from m_warehouse where em_escm_warehouse_type  =  'RTW' and ad_client_id = ? "
                + "and ad_org_id in( select ad_org_id from AD_Role_OrgAccess where ad_role_id =? )");
        st.setString(1, inpClientId);
        st.setString(2, roleid);
        rs = st.executeQuery();
        while (rs.next()) {
          vo = new ReturnedItemsReportVO();
          vo.setWarehouseId(rs.getString("m_warehouse_id"));
          vo.setWarehousename(rs.getString("name"));
          ls.add(vo);
        }

        request.setAttribute("inpwarehousename", ls);

        // Localization support
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.getRequestDispatcher(jspPage).include(request, response);

      } else if (action.equals("Submit")) {
        String inpWarehouseid = request.getParameter("inpWarehouseid");

        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("inpWarehouseid", inpWarehouseid);
        parameters.put("inpClientId", inpClientId);

        strReportName = "@basedesign@/sa/elm/ob/scm/ad_reports/ReturnedItemsReport/ReturnedItemsReport.jrxml";
        String strOutput = "pdf";

        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      }
    } catch (Exception e) {
      e.printStackTrace();
      log4j.error("Exception in ReturnedItemsReport :", e);
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();
    }
  }
}