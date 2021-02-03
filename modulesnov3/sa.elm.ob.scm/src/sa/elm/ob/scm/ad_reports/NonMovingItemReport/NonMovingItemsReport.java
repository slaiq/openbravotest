package sa.elm.ob.scm.ad_reports.NonMovingItemReport;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Warehouse;

import sa.elm.ob.scm.ad_reports.ReturnedItemsReport.ReturnedItemsReportVO;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * 
 */
public class NonMovingItemsReport extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.scm/jsp/NonMovingItemsReport/NonMovingItemsReport.jsp";

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
      inpClientId = vars.getClient();
      roleid = vars.getRole();

      log4j.debug("action");
      if (action.equals("")) {
        List<ReturnedItemsReportVO> ls = new ArrayList<ReturnedItemsReportVO>();
        ReturnedItemsReportVO vo = null;
        st = con.prepareStatement(
            "select m_warehouse_id,name from m_warehouse where em_escm_warehouse_type  =  'MAW' and ad_client_id = ? "
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
        String inpPeriod = request.getParameter("inpPeriod");
        String inpPeriodTranslate = request.getParameter("inpPeriod");
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        String Date = "";

        HashMap<String, Object> parameters = new HashMap<String, Object>();
        // get warehouse
        Warehouse objWarehouse = OBDal.getInstance().get(Warehouse.class, inpWarehouseid);
        // calculate period Date

        inpPeriod = inpPeriod + "month";
        inpPeriodTranslate = inpPeriodTranslate + OBMessageUtils.messageBD("Month");
        st = con.prepareStatement("select date(now() - interval '" + inpPeriod + "') from dual");
        rs = st.executeQuery();
        if (rs.next()) {
          Date = rs.getDate("date").toString();
        }
        parameters.put("warehouseId", inpWarehouseid);
        parameters.put("clientId", inpClientId);
        parameters.put("today", yearFormat.format(
            dateFormat.parse(UtilityDAO.convertToHijriDate(yearFormat.format(new Date())))));
        parameters.put("period", Date);
        parameters.put("periodMonth", inpPeriodTranslate);
        parameters.put("clientName", objWarehouse.getClient().getName());
        parameters.put("orgName", objWarehouse.getOrganization().getName());
        parameters.put("warehouse", objWarehouse.getName());
        strReportName = "@basedesign@/sa/elm/ob/scm/ad_reports/NonMovingItemReport/NonMovingItemsReport.jrxml";
        String strOutput = "pdf";

        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      }
    } catch (Exception e) {
      e.printStackTrace();
      log4j.error("Exception in NonMovingItemsReport :", e);
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