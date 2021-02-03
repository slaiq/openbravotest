package sa.elm.ob.scm.ad_reports.ItemCardReport;

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

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;

import sa.elm.ob.scm.ad_reports.CustodyCardReport.CustodyCardReportVO;

/**
 * 
 * @author Divya on 29/03/2017
 * 
 */
public class ItemCardReport extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.scm/jsp/ItemCardReport/ItemCardReport.jsp";

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
      String action = (request.getParameter("inpAction") == null ? ""
          : request.getParameter("inpAction"));
      VariablesSecureApp vars = new VariablesSecureApp(request);
      Connection con = getConnection();
      String inpClientId = "";
      inpClientId = vars.getClient();
      String inpOrgId = vars.getOrg();

      JSONArray jsonArray = null;
      log4j.debug("action");
      if (action.equals("")) {
        List<CustodyCardReportVO> ls = new ArrayList<CustodyCardReportVO>();
        List<CustodyCardReportVO> prdls = new ArrayList<CustodyCardReportVO>();
        CustodyCardReportVO vo = null;

        st = con.prepareStatement(
            " select name,m_warehouse_id from m_warehouse where ad_client_id= ?  and em_escm_warehouse_type='MAW' and ad_org_id in( select ad_org_id  from ad_role_orgaccess   where ad_role_id  = ? and ad_client_id = ? )    and isactive='Y'  order by name asc  ");
        st.setString(1, inpClientId);
        st.setString(2, vars.getRole());
        st.setString(3, inpClientId);
        rs = st.executeQuery();
        while (rs.next()) {
          vo = new CustodyCardReportVO();
          vo.setBeneficiaryname(rs.getString("name"));
          vo.setBeneficiaryId(rs.getString("m_warehouse_id"));
          ls.add(vo);
        }
        request.setAttribute("inpwarehouseList", ls);
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();

        st = con.prepareStatement(
            " select (value||'-'||name) as name ,m_product_id from m_product where ad_client_id= ?    and isactive='Y'  order by name asc  ");
        st.setString(1, inpClientId);
        rs = st.executeQuery();
        while (rs.next()) {
          vo = new CustodyCardReportVO();
          vo.setBeneficiaryname(rs.getString("name"));
          vo.setBeneficiaryId(rs.getString("m_product_id"));
          prdls.add(vo);
        }

        request.setAttribute("inpproductList", prdls);

        // Localization support
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.getRequestDispatcher(jspPage).include(request, response);

      } else if (action.equals("Submit")) {
        String inpWarehouseId = request.getParameter("inpWarehouseid");
        // String inpLocatorId = request.getParameter("inpLocatorid");
        String inpProductId = request.getParameter("inpProductid");
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("inpWarehouseId", inpWarehouseId);
        parameters.put("inpProductId", inpProductId);
        parameters.put("inpAD_Client_ID", inpClientId);
        parameters.put("inpAD_Org_ID", inpOrgId);

        strReportName = "@basedesign@/sa/elm/ob/scm/ad_reports/ItemCardReport/Item Card Report.jrxml";
        String strOutput = "pdf";

        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      } else if (action.equals("getLocator")) {
        JSONObject jsonResponse = null;
        String warehouseId = request.getParameter("inpWarehouseId");
        jsonArray = new JSONArray();

        st = con.prepareStatement(
            " select m_locator_id as id ,value from m_locator where m_warehouse_id= ?  and ad_client_id=?   and isactive='Y' order by value asc ");
        st.setString(1, warehouseId);
        st.setString(2, inpClientId);
        rs = st.executeQuery();
        log4j.debug("getLocator:" + st.toString());
        while (rs.next()) {
          jsonResponse = new JSONObject();
          jsonResponse.put("id", rs.getString("id"));
          jsonResponse.put("value", rs.getString("value"));
          jsonArray.put(jsonResponse);
        }

        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonArray.toString());
      }
    } catch (Exception e) {
      e.printStackTrace();
      log4j.error("Exception in ItemCardReport :", e);
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