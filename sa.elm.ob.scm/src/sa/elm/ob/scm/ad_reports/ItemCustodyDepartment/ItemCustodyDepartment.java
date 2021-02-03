package sa.elm.ob.scm.ad_reports.ItemCustodyDepartment;

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
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationInformation;

public class ItemCustodyDepartment extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.scm/jsp/ItemCustodyDepartment/ItemCustodyDepartment.jsp";

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
      JSONArray jsonArray = null;
      String sqlquery = "";
      String inpOrgId = vars.getOrg();
      log4j.debug("action");
      if (action.equals("")) {
        List<ItemCustodyDepartmentVO> ls = new ArrayList<ItemCustodyDepartmentVO>();
        ItemCustodyDepartmentVO vo = null;

        sqlquery = " select org.ad_org_id,org.value,org.name from ad_role_orgaccess orgaccess join ad_org org on org.ad_org_id = orgaccess.ad_org_id and org.em_ehcm_orgtyp in ( select ehcm_org_type_id from ehcm_org_type where value='ORG') where orgaccess.ad_role_id =? and orgaccess.ad_client_id=? and org.ad_org_id <> '0'";
        if (!inpOrgId.equals("0")) {
          sqlquery += " order by org.ad_org_id = '" + inpOrgId + "' desc ";

        }
        st = con.prepareStatement(sqlquery);
        st.setString(1, vars.getRole());
        st.setString(2, inpClientId);
        rs = st.executeQuery();
        while (rs.next()) {
          vo = new ItemCustodyDepartmentVO();
          vo.setOrgName(rs.getString("name"));
          vo.setOrgVaue(rs.getString("value"));
          vo.setOrgId(rs.getString("ad_org_id"));
          ls.add(vo);
        }
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();

        List<ItemCustodyDepartmentVO> lsdept = new ArrayList<ItemCustodyDepartmentVO>();
        ItemCustodyDepartmentVO vodept = null;
        String sql = "";
        if (vars.getLanguage().equals("ar_SA")) {
          sql = "select coalesce(tr.name,list.name) as name ,list.value ";
        } else {
          sql = "select list.name ,list.value ";
        }
        sql = sql
            + " from ad_ref_list list left join ad_ref_list_trl tr on list.ad_ref_list_id = tr.ad_ref_list_id "
            + "where ad_reference_id='E585F9EEA3024736B3E30F9F6A7C9A09' and list.value  in ('D') order by list.value asc";
        st = con.prepareStatement(sql);
        rs = st.executeQuery();
        while (rs.next()) {
          vodept = new ItemCustodyDepartmentVO();
          vodept.setBeneficiaryname(rs.getString("name"));
          vodept.setBeneficiaryvalue(rs.getString("value"));
          lsdept.add(vodept);
        }

        request.setAttribute("inpBeneficiaryTypeList", lsdept);
        request.setAttribute("inpOrgList", ls);

        // Localization support
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.getRequestDispatcher(jspPage).include(request, response);

      } else if (action.equals("Submit")) {
        String inpBeneficiaryid = request.getParameter("inpBeneficiaryid");
        String inpOrgid = request.getParameter("inpOrgid");
        String inpOrgName = request.getParameter("inpOrgName");
        String imgOrg = "";
        Organization org = OBDal.getInstance().get(Organization.class, inpOrgid);
        OrganizationInformation objInfo = org.getOrganizationInformationList().get(0);
        // check org have image
        if (objInfo != null) {
          if (objInfo.getYourCompanyDocumentImage() != null) {
            imgOrg = inpOrgid;
          } else {
            imgOrg = vars.getOrg();
          }
        }
        log4j.debug("inpBeneficiaryid>" + inpBeneficiaryid);
        log4j.debug("inpOrgid>" + inpOrgid);
        log4j.debug("inpClientId>" + inpClientId);
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("inpDepartmentId", inpBeneficiaryid);
        parameters.put("inpAD_Client_ID", inpClientId);
        parameters.put("inpAD_Org_ID", inpOrgid);
        parameters.put("imgOrg", imgOrg);
        parameters.put("inpOrg", inpOrgName);

        strReportName = "@basedesign@/sa/elm/ob/scm/ad_reports/ItemCustodyDepartment/ItemCustodyDepartment.jrxml";
        String strOutput = "pdf";

        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      } else if (action.equals("getbeneficiary")) {
        JSONObject jsonResponse = null;
        String orgId = request.getParameter("inporgId");
        jsonArray = new JSONArray();

        st = con.prepareStatement(
            " select escm_beneficiary_v_id as id ,name from escm_beneficiary_v where btype= 'D' and ad_client_id=? and ad_org_id in ( select ad_org_id from ad_org where em_ehcm_ad_org_id = ?)  order by value asc ");
        st.setString(1, inpClientId);
        st.setString(2, orgId);
        rs = st.executeQuery();
        log4j.debug("getbeneficiary:" + st.toString());
        while (rs.next()) {
          jsonResponse = new JSONObject();
          jsonResponse.put("id", rs.getString("id"));
          jsonResponse.put("name", rs.getString("name"));
          jsonArray.put(jsonResponse);
        }

        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonArray.toString());
      }
    } catch (Exception e) {
      e.printStackTrace();
      log4j.error("Exception in ItemCustodyDepartment :", e);
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