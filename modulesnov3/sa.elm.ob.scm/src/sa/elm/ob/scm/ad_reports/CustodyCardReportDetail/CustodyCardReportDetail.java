package sa.elm.ob.scm.ad_reports.CustodyCardReportDetail;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;

import sa.elm.ob.scm.BeneficiaryView;
import sa.elm.ob.scm.ad_reports.CustodyCardReport.CustodyCardReportVO;

public class CustodyCardReportDetail extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.scm/jsp/CustodyCardReportDetail/CustodyCardReportDetail.jsp";

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
      String inpRoleID = vars.getRole();
      JSONArray jsonArray = null;
      log4j.debug("action");
      if (action.equals("")) {
        List<CustodyCardReportVO> ls = new ArrayList<CustodyCardReportVO>();
        CustodyCardReportVO vo = null;
        String sql = "";
        if (vars.getLanguage().equals("ar_SA")) {
          sql = "select coalesce(tr.name,list.name) as name ,list.value ";
        } else {
          sql = "select list.name ,list.value ";
        }
        sql = sql
            + " from ad_ref_list list left join ad_ref_list_trl tr on list.ad_ref_list_id = tr.ad_ref_list_id where ad_reference_id='E585F9EEA3024736B3E30F9F6A7C9A09' and list.value  in ('D','E','S') order by list.seqno ";
        st = con.prepareStatement(sql);
        rs = st.executeQuery();
        while (rs.next()) {
          vo = new CustodyCardReportVO();
          vo.setBeneficiaryname(rs.getString("name"));
          vo.setBeneficiaryvalue(rs.getString("value"));
          ls.add(vo);
        }

        request.setAttribute("inpBeneficiaryTypeList", ls);

        // Localization support
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.getRequestDispatcher(jspPage).include(request, response);

      } else if (action.equals("Submit")) {
        String inpBeneficiaryType = request.getParameter("inpBeneficiarytype");
        String inpBeneficiaryId = request.getParameter("inpBeneficiaryid");
        BeneficiaryView objview = OBDal.getInstance().get(BeneficiaryView.class, inpBeneficiaryId);
        Client client = OBDal.getInstance().get(Client.class, vars.getClient());
        log4j.debug("inpBeneficiaryType>" + inpBeneficiaryType);
        log4j.debug("inpBeneficiaryId>" + inpBeneficiaryId);
        log4j.debug("inpClientId>" + inpClientId);
        String btype = objview.getBtype();
        String bentype = "";
        String sql = "";
        if (vars.getLanguage().equals("ar_SA")) {
          sql = "select coalesce(trl.name,list.name) as name ";
        } else {
          sql = "select list.name ";
        }
        sql = sql + " from ad_ref_list list "
            + "left join AD_Ref_List_Trl trl on list.ad_ref_list_id=trl.ad_ref_list_id and trl.ad_language='ar_SA'"
            + " where list.ad_reference_id='E585F9EEA3024736B3E30F9F6A7C9A09' and  list.value=? ";
        try {
          st = con.prepareStatement(sql);
          st.setString(1, btype);
          rs = st.executeQuery();
          if (rs.next()) {
            bentype = rs.getString("name");
          }
        } catch (SQLException e) {
          e.printStackTrace();
        }
        String name = objview.getCommercialName();
        String code = objview.getSearchKey();
        name = StringUtils.substringAfterLast(name, code);
        String benname = StringUtils.substringAfterLast(name, "-");

        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("inpBeneficiaryType", inpBeneficiaryType);
        parameters.put("inpBeneficiaryId", inpBeneficiaryId);
        parameters.put("inpBeneficiaryName", benname);
        parameters.put("inpBeneficiaryCode", code);
        parameters.put("inpBeneficiaryNameCode", code.concat(" - ").concat(benname));
        parameters.put("inpBeneficiarytypes", bentype);
        parameters.put("inpAD_Client_ID", inpClientId);
        parameters.put("inpAD_Org_ID", inpOrgId);
        parameters.put("inpClientName", client.getName());

        strReportName = "@basedesign@/sa/elm/ob/scm/ad_reports/CustodyCardReportDetail/Custody Card Report Detail.jrxml";
        String strOutput = "pdf";

        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      } else if (action.equals("getbeneficiary")) {
        JSONObject jsonResponse = null;
        String type = request.getParameter("inptype");
        jsonArray = new JSONArray();

        st = con.prepareStatement(
            " select escm_beneficiary_v_id as id ,name from escm_beneficiary_v where btype= ? and ad_org_id in( select ad_org_id from ad_role_orgaccess where ad_role_id  = ? and ad_client_id = ? )"
                + " and ad_client_id=?  order by value asc ");
        st.setString(1, type);
        st.setString(2, inpRoleID);
        st.setString(3, inpClientId);
        st.setString(4, inpClientId);
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
      log4j.error("Exception in CustodyCardReportDetail :", e);
    } finally {
      // close connection
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {
        log4j.error("Exception while closing the statement in CustodyCardReportDetail ", e);
      }
      OBContext.restorePreviousMode();
    }
  }
}
