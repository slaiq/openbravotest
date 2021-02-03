package sa.elm.ob.scm.ad_reports.MinMaxPlanningReport;

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
public class MinMaxPlanningReport extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.scm/jsp/MinMaxPlanningReport/MinMaxPlanningReport.jsp";

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
      String roleid = "";
      String strOutput = "";
      inpClientId = vars.getClient();
      roleid = vars.getRole();
      String inpOrgId = vars.getOrg();

      String sql = "";
      if (action.equals("")) {
        // Warehouse
        List<MinMaxPlanningReportVO> ls = new ArrayList<MinMaxPlanningReportVO>();
        List<MinMaxPlanningReportVO> orgls = new ArrayList<MinMaxPlanningReportVO>();

        MinMaxPlanningReportVO vo = null;

        st = con.prepareStatement(
            "select m_warehouse_id,name from m_warehouse where em_escm_warehouse_type  =  'MAW' and ad_client_id = ? "
                + "and ad_org_id in( select ad_org_id from AD_Role_OrgAccess where ad_role_id =? )");
        st.setString(1, inpClientId);
        st.setString(2, roleid);
        rs = st.executeQuery();
        while (rs.next()) {
          vo = new MinMaxPlanningReportVO();
          vo.setWarehouseId(rs.getString("m_warehouse_id"));
          vo.setWarehousename(rs.getString("name"));
          ls.add(vo);
        }
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
        request.setAttribute("inpwarehousename", ls);

        // Organization
        sql = "select ad_org.ad_org_id as id,ad_org.name as orgname from ad_role left join AD_Role_OrgAccess on "
            + " ad_role.ad_role_id = AD_Role_OrgAccess.ad_role_id "
            + " left join ad_org on ad_org.ad_org_id = AD_Role_OrgAccess.ad_org_id "
            + "  where ad_role.ad_role_id = ? and ad_role.isactive = 'Y' "
            + " and ad_role.ad_client_id = ? and ad_org.em_ehcm_orgtyp  in (select ehcm_org_type_id from ehcm_org_type where value='ORG')";

        if (!inpOrgId.equals("0")) {
          sql += " order by ad_org.ad_org_id = '" + inpOrgId + "' desc ";

        }
        st = con.prepareStatement(sql);
        st.setString(1, roleid);
        st.setString(2, inpClientId);
        rs = st.executeQuery();

        while (rs.next()) {
          vo = new MinMaxPlanningReportVO();
          vo.setOrganizationid(rs.getString("id"));
          vo.setOrganizationname(rs.getString("orgname"));
          orgls.add(vo);
        }

        request.setAttribute("inporganizationname", orgls);
        // Localization support
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.getRequestDispatcher(jspPage).include(request, response);

      } else if (action.equals("Submit")) {
        String inpWarehouseid = request.getParameter("inpWarehouseid");
        String inpOrgid = request.getParameter("inpOrgid");

        if (request.getParameter("inpWarehouseid").equals("0")) {
          strReportName = "@basedesign@/sa/elm/ob/scm/ad_reports/MinMaxPlanningReport/Min Max Allwarehouse.jrxml";
          strOutput = "pdf";
        } else {
          strReportName = "@basedesign@/sa/elm/ob/scm/ad_reports/MinMaxPlanningReport/Min Max Planning Report.jrxml";
          strOutput = "pdf";
        }
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("inpWarehouseid", inpWarehouseid);
        parameters.put("inpClientId", inpClientId);
        parameters.put("inpOrgid", inpOrgid);
        // String reportGeneratedDate = new SimpleDateFormat("yyyy-MM-dd")
        // .format(new java.util.Date());
        // String hijriDate =
        // sa.elm.ob.utility.util.Utility.convertTohijriDate(reportGeneratedDate);

        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      }
    } catch (Exception e) {
      e.printStackTrace();
      log4j.error("Exception in MinMaxPlanningReport :", e);
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