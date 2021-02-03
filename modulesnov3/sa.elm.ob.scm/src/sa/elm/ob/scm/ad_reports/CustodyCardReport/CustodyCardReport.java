package sa.elm.ob.scm.ad_reports.CustodyCardReport;

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

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.exception.NoConnectionAvailableException;

/**
 * 
 * @author Divya on 23/03/2017
 * 
 */
public class CustodyCardReport extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.scm/jsp/CustodyCardReport/CustodyCardReport.jsp";

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
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
      PreparedStatement st = null;
      ResultSet rs = null;
      // JSONArray jsonArray = new JSONArray();
      // JSONObject jsonObject = null;
      log4j.debug("action");
      if (action.equals("")) {
        List<CustodyCardReportVO> ls = new ArrayList<CustodyCardReportVO>();
        CustodyCardReportVO vo = null;
        String sql = "";
        String isWarehouseRole = "N";
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

        isWarehouseRole = CustodyCardReportDAO.isWarehouseRole();

        request.setAttribute("inpBeneficiaryTypeList", ls);
        request.setAttribute("isWarehouseRole", isWarehouseRole);
        request.setAttribute("LoggedInEmployee", CustodyCardReportDAO.getEmployeeId());

        // Localization support
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.getRequestDispatcher(jspPage).include(request, response);

      } else if (action.equals("Submit")) {
        String inpBeneficiaryType = request.getParameter("inpBeneficiarytype");
        String inpBeneficiaryId = request.getParameter("inpBeneficiaryid");

        CustodyCardReportVO custodyCardReportVO = getBeneficiaryValueName(inpBeneficiaryId);
        // Now get the Beneficiary Type Name
        String inBeneficairyTypeName = getBeneficiaryTypeName(inpBeneficiaryType);
        log4j.debug("inpBeneficiaryType>" + inpBeneficiaryType);
        log4j.debug("inpBeneficiaryId>" + inpBeneficiaryId);
        log4j.debug("inpClientId>" + inpClientId);
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("inpBeneficiaryType", inpBeneficiaryType);
        parameters.put("inpBeneficiaryId", inpBeneficiaryId);
        parameters.put("inpAD_Client_ID", inpClientId);
        parameters.put("inpAD_Org_ID", inpOrgId);
        parameters.put("inpBeneficiary_type_name", inBeneficairyTypeName);
        parameters.put("inpBeneficiary_value", custodyCardReportVO.getBeneficiaryvalue());
        parameters.put("inpBeneficiary_name", custodyCardReportVO.getBeneficiaryname());
        parameters.put("hijri_today", custodyCardReportVO.getCurrentDateHijri());

        strReportName = "@basedesign@/sa/elm/ob/scm/ad_reports/CustodyCardReport/CustodyCardReport.jrxml";
        String strOutput = "pdf";

        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      } /*
         * else if (action.equals("getbeneficiary")) { JSONObject jsonResponse = null; String type =
         * request.getParameter("inptype"); jsonArray = new JSONArray();
         * 
         * st = con .prepareStatement(
         * " select escm_beneficiary_v_id as id ,name from escm_beneficiary_v where btype= ?  and ad_org_id in( select ad_org_id  from ad_role_orgaccess   where ad_role_id  = ? and ad_client_id = ? )  "
         * + "and ad_client_id=?  order by value asc "); st.setString(1, type); st.setString(2,
         * vars.getRole()); st.setString(3, inpClientId); st.setString(4, inpClientId); rs =
         * st.executeQuery(); log4j.debug("getbeneficiary:" + st.toString()); while (rs.next()) {
         * jsonResponse = new JSONObject(); jsonResponse.put("id", rs.getString("id"));
         * jsonResponse.put("name", rs.getString("name")); jsonArray.put(jsonResponse); }
         * 
         * response.setCharacterEncoding("UTF-8"); response.getWriter().write(jsonArray.toString());
         * }
         */

    } catch (Exception e) {
      e.printStackTrace();
      log4j.error("Exception in CustodyCardReport :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Get the Beneficiary Type Name
   * 
   * @param beneficiaryTypeId
   * @return
   */
  private CustodyCardReportVO getBeneficiaryValueName(String beneficiaryId) {
    try {
      Connection con = getConnection();
      String query = " SELECT eut_convert_to_hijri(to_char(NOW(),'YYYY-MM-DD')) as today ,VALUE,NAME FROM escm_beneficiary_v WHERE escm_beneficiary_v_id = ?";
      CustodyCardReportVO custodyCardReportVO = new CustodyCardReportVO();
      PreparedStatement st = con.prepareStatement(query);
      st.setString(1, beneficiaryId);
      ResultSet rs = st.executeQuery();
      if (rs.next()) {
        custodyCardReportVO.setBeneficiaryname(rs.getString("NAME"));
        custodyCardReportVO.setBeneficiaryvalue(rs.getString("VALUE"));
        custodyCardReportVO.setCurrentDateHijri(rs.getString("today"));
        return custodyCardReportVO;
      }
    } catch (NoConnectionAvailableException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

  private String getBeneficiaryTypeName(String beneficiaryTypeId) {
    try {
      Connection con = getConnection();
      String query = " select coalesce(tr.name,list.name) as name   "
          + "from ad_ref_list list left join ad_ref_list_trl tr on list.ad_ref_list_id = tr.ad_ref_list_id"
          + " where ad_reference_id='E585F9EEA3024736B3E30F9F6A7C9A09' and list.value  = ? ";

      PreparedStatement st = con.prepareStatement(query);
      st.setString(1, beneficiaryTypeId);
      ResultSet rs = st.executeQuery();
      while (rs.next()) {
        return rs.getString("name");
      }
    } catch (NoConnectionAvailableException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return null;
  }

  public static String getDescription(String strAttributeInstanceId) {
    final Logger log4j = Logger.getLogger(CustodyCardReport.class);
    String strDescription = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    char l2R = '\u202A';
    try {
      OBContext.setAdminMode();
      st = OBDal.getInstance().getConnection().prepareStatement(
          "select value, m_attributesetinstance_id,  em_escm_sequence, name, seqno, ordercount  "
              + " from (select distinct ainst.value, ainst.m_attributesetinstance_id,  attrb.em_escm_sequence, attrb.name, seqno, "
              + " (select 1 from dual where exists (select count(em_escm_sequence) from m_attributeinstance ainstc  "
              + " join m_attribute attrb on attrb.m_attribute_id=ainstc.m_attribute_id  where ainstc.value is not null and attrb.em_escm_iscstdycard='Y' "
              + " and ainstc.m_attributesetinstance_id=ainst.m_attributesetinstance_id  group by em_escm_sequence having count(em_escm_sequence)>1)) as ordercount "
              + " from m_attributeinstance ainst join m_attributesetinstance asetinst on asetinst.m_attributesetinstance_id=ainst.m_attributesetinstance_id  "
              + " join m_attribute attrb on attrb.m_attribute_id=ainst.m_attribute_id  join m_attributeuse mattruse on mattruse.m_attribute_id=attrb.m_attribute_id "
              + " and mattruse.m_attributeset_id=asetinst.m_attributeset_id  where ainst.value is not null and attrb.em_escm_iscstdycard='Y' "
              + " and ainst.m_attributesetinstance_id=?) attr "
              + " group by m_attributesetinstance_id, attr.ordercount, em_escm_sequence, seqno, value, name "
              + " order by case when attr.ordercount is null then em_escm_sequence else seqno end desc");
      st.setString(1, strAttributeInstanceId);
      log4j.debug("attrqry>" + st.toString());
      rs = st.executeQuery();
      while (rs.next()) {
        strDescription = strDescription + (" - ") + l2R + (rs.getString("value"));
      }
      strDescription = strDescription.replaceFirst(" - ", "");
      log4j.debug("strDescription>" + strDescription);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
    return strDescription;
  }
}