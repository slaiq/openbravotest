package sa.elm.ob.scm.ad_reports.itemcontrolcard;

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

import sa.elm.ob.scm.ad_reports.CustodyCardReport.CustodyCardReportVO;

public class ItemControlCard extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.scm/jsp/itemcontrolcard/ItemControlCard.jsp";

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
      String inpClientId = vars.getClient();
      String inpRoleId = vars.getRole();
      if (action.equals("")) {
        List<CustodyCardReportVO> ls = new ArrayList<CustodyCardReportVO>();
        List<CustodyCardReportVO> prdls = new ArrayList<CustodyCardReportVO>();
        CustodyCardReportVO vo = null;

        st = con.prepareStatement(
            " select name, m_warehouse_id from m_warehouse where em_escm_warehouse_type='MAW' "
                + " and ad_org_id in ( select ad_org_id  from ad_role_orgaccess   where ad_role_id  = ? and ad_client_id = ? ) and isactive='Y' order by name asc  ");
        st.setString(1, inpRoleId);
        st.setString(2, inpClientId);
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
            " select (prd.value||'-'||prd.name||'-'||stk.name) as name, m_product_id from m_product prd "
                + " join escm_deflookups_typeln stk on stk.escm_deflookups_typeln_id=prd.em_escm_stock_type "
                + " where prd.isactive='Y' and prd.ad_org_id in ( select ad_org_id  from ad_role_orgaccess   where ad_role_id  = ? and ad_client_id = ? )  and (((select count(m_product_category_id) from escm_usergroups join "
                + "                             escm_usergrp_role on escm_usergroups.escm_usergroups_id =escm_usergrp_role."
                + "                             escm_usergroups_id join escm_usergrp_procat on escm_usergrp_procat. "
                + "                             escm_usergroups_id =escm_usergroups.escm_usergroups_id "
                + "                             where ad_role_id =?) =0) or (prd.m_product_category_id in "
                + "                                                           (select m_product_category_id from    "
                + "                        escm_usergroups join escm_usergrp_role on escm_usergroups.escm_usergroups_id = escm_usergrp_role."
                + "                                                            escm_usergroups_id   "
                + "                                                            join escm_usergrp_procat on escm_usergrp_procat.escm_usergroups_id = "
                + "                                                            escm_usergroups.escm_usergroups_id where ad_role_id =?))) and prd.ad_client_id =?  order by name asc");
        st.setString(1, inpRoleId);
        st.setString(2, inpClientId);
        st.setString(3, inpRoleId);
        st.setString(4, inpRoleId);
        st.setString(5, inpClientId);
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
        String inpProductId = request.getParameter("inpProductid");
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("inpProductId", inpProductId);

        if (inpWarehouseId.equals("0"))
          parameters.put("inpWarehouseId", " and 1=1 ");
        else
          parameters.put("inpWarehouseId",
              " and warehouse.m_warehouse_id = '" + inpWarehouseId + "' ");

        strReportName = "@basedesign@/sa/elm/ob/scm/ad_reports/itemcontrolcard/ItemControlCard.jrxml";
        String strOutput = "pdf";

        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      }
    } catch (Exception e) {
      e.printStackTrace();
      log4j.error("Exception in ItemControlCardReport :", e);
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