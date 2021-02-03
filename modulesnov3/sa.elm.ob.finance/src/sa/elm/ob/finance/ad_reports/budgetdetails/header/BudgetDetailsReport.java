package sa.elm.ob.finance.ad_reports.budgetdetails.header;

import java.io.IOException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.model.sales.SalesRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.ad_reports.budgetdetails.dao.BudgetDetailsReportDAO;
import sa.elm.ob.finance.ad_reports.budgetdetails.vo.BudgetDetailsReportVO;

public class BudgetDetailsReport extends HttpSecureAppServlet {
  private static final Logger log = LoggerFactory.getLogger(BudgetDetailsReport.class);
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.finance/jsp/budgetdetails/BudgetDetails.jsp";

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    try {
      String strReportName = "", imageFlag = "N";
      String action = (request.getParameter("inpAction") == null ? ""
          : request.getParameter("inpAction"));

      VariablesSecureApp vars = new VariablesSecureApp(request);
      Client client = OBDal.getInstance().get(Client.class, vars.getClient());
      User user = OBDal.getInstance().get(User.class, vars.getUser());
      Connection con = OBDal.getInstance().getConnection();
      BudgetDetailsReportDAO dao = new BudgetDetailsReportDAO(con, vars);

      OBContext.setAdminMode();

      if (action.equals("")) {
        log4j.debug("action");
        request.setAttribute("OrgList", dao.getOrgList(vars));
        request.setAttribute("currentYear", dao.getCurrentYear(vars));
        request.setAttribute("YearList", dao.gettingYearList(vars));
        log4j.debug("yearlist:" + request.getAttribute("YearList"));
        request.setAttribute("TypeList", dao.gettingTypeList(vars));
        // request.setAttribute("deptList", dao.getCostCenterDeptList(vars));

        // Localization support
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        // request.setAttribute("GroupList",dao.gettingGroupList(vars, inpBudgetTypeId));
        request.getRequestDispatcher(jspPage).include(request, response);

      } else if (action.equals("generateReport")) {
        // String strOutputFileName = "Site Attendance Report_" + sf.format(today).replaceAll("-",
        // "");
        log4j.debug(
            "request.getParameter(\"inpview\")" + request.getParameter("inpbudgetInitiName"));
        if (request.getParameter("inpview").equals("MoF")) {
          strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/budgetdetails/BudgetDetailReport.jrxml";
        } else {
          strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/budgetdetails/BudgetDetailReportMoTA3.jrxml";
        }
        String strOutput = "pdf";
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("inpADClient", client.getName());
        parameters.put("inpADUser", user.getName());
        parameters.put("inpRole", vars.getRole());
        String orgId = request.getParameter("inpOrg1");
        String orgParent = dao.getParentOrg(orgId, vars);
        String orgName = request.getParameter("inpOrg1name");
        parameters.put("inpOrgName", orgName);
        parameters.put("inpOrg1", orgParent);
        parameters.put("inpOrganizationId", orgId);
        String yearId = request.getParameter("inpYear1");
        parameters.put("inpYear1", yearId);
        String budgetGroup = request.getParameter("inpBudgetGroupId1");
        parameters.put("inpBudgetGroupId1", budgetGroup);
        String outputTyp = "pdf";
        parameters.put("inpoutputType", outputTyp);
        String inpHideZeroValue = request.getParameter("inpHideZeroValue");
        inpHideZeroValue = inpHideZeroValue == null ? "N" : inpHideZeroValue;
        log4j.debug("inpHideZeroValue>" + inpHideZeroValue);
        String whereCond = "";
        if (inpHideZeroValue.equals("Y"))
          whereCond = " and l.current_budget>0 ";
        else
          whereCond = " ";
        parameters.put("inpHideZeroValue", whereCond);
        Organization org = OBDal.getInstance().get(Organization.class, orgId);
        if (org.getOrganizationInformationList().size() > 0) {
          OrganizationInformation objInfo = org.getOrganizationInformationList().get(0);
          // check org have image
          if (objInfo != null) {
            if (objInfo.getYourCompanyDocumentImage() != null) {
              imageFlag = "Y";
            }
          }
        }
        // for MoT passing Cost Center values
        if (request.getParameter("inpview").equals("MoT")) {
          String fromDept = request.getParameter("inpfromdeptList");
          SalesRegion fromcostcenter = OBDal.getInstance().get(SalesRegion.class, fromDept);
          parameters.put("inpFromCostCenter", fromcostcenter.getSearchKey());
          String toDept = request.getParameter("inptodeptList");
          SalesRegion tocostcenter = OBDal.getInstance().get(SalesRegion.class, toDept);
          parameters.put("inpToCostCenter", tocostcenter.getSearchKey());

        }
        parameters.put("inpImageFlag", imageFlag);
        parameters.put("inpOrgId", org.getId());
        String budgetType = request.getParameter("inpBudgetTypeId1");
        parameters.put("inpBudgetTypeId1", budgetType);
        log4j.info("type" + budgetType + "grop" + budgetGroup + "year" + yearId);
        parameters.put("inpBudgetInit", request.getParameter("inpbudgetInitiName"));

        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      } else if (action.equals("xls")) {
        // String strOutputFileName = "Site Attendance Report_" + sf.format(today).replaceAll("-",
        // "");
        if (request.getParameter("inpview").equals("MoF")) {
          strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/budgetdetails/BudgetDetailReport.jrxml";
        } else {
          strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/budgetdetails/BudgetDetailReportMoTA3.jrxml";
        }
        log4j.debug(
            "request.getParameter(\"inpview\")" + request.getParameter("inpbudgetInitiName"));
        String strOutput = "xls";
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("inpADClient", client.getName());
        parameters.put("inpADUser", user.getName());
        parameters.put("inpRole", vars.getRole());
        String orgId = request.getParameter("inpOrg1");
        String orgParent = dao.getParentOrg(orgId, vars);
        String orgName = request.getParameter("inpOrg1name");
        parameters.put("inpOrgName", orgName);
        parameters.put("inpOrg1", orgParent);
        parameters.put("inpOrganizationId", orgId);
        String yearId = request.getParameter("inpYear1");
        parameters.put("inpYear1", yearId);
        String budgetGroup = request.getParameter("inpBudgetGroupId1");
        parameters.put("inpBudgetGroupId1", budgetGroup);
        String outputTyp = "xls";
        parameters.put("inpoutputType", outputTyp);
        String inpHideZeroValue = request.getParameter("inpHideZeroValue");
        inpHideZeroValue = inpHideZeroValue == null ? "N" : inpHideZeroValue;
        log4j.info("inpHideZeroValue>" + inpHideZeroValue);
        String whereCond = "";
        if (inpHideZeroValue.equals("Y"))
          whereCond = " and l.current_budget>0 ";
        else
          whereCond = " ";
        parameters.put("inpHideZeroValue", whereCond);
        log4j.info("whereCond>" + whereCond);
        Organization org = OBDal.getInstance().get(Organization.class, orgId);
        if (org.getOrganizationInformationList().size() > 0) {
          OrganizationInformation objInfo = org.getOrganizationInformationList().get(0);
          // check org have image
          if (objInfo != null) {
            if (objInfo.getYourCompanyDocumentImage() != null) {
              imageFlag = "Y";
            }
          }
        }
        log.debug("view :" + request.getParameter("inpview"));
        if (request.getParameter("inpview").equals("MoT")) {
          String fromDept = request.getParameter("inpfromdeptList");
          SalesRegion fromcostcenter = OBDal.getInstance().get(SalesRegion.class, fromDept);
          parameters.put("inpFromCostCenter", fromcostcenter.getSearchKey());
          String toDept = request.getParameter("inptodeptList");
          SalesRegion tocostcenter = OBDal.getInstance().get(SalesRegion.class, toDept);
          parameters.put("inpToCostCenter", tocostcenter.getSearchKey());
        }
        parameters.put("inpImageFlag", imageFlag);
        parameters.put("inpOrgId", org.getId());
        String budgetType = request.getParameter("inpBudgetTypeId1");
        parameters.put("inpBudgetTypeId1", budgetType);
        parameters.put("inpBudgetInit", request.getParameter("inpbudgetInitiName"));
        log4j.info("type" + budgetType + "grop" + budgetGroup + "year" + yearId);

        // rendJR(vars, response, strReportName, strOutputFileName, strOutput, parameters, null,
        // null);
        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      }
      /*
       * else if(action.equals("setCurrentYear")) {
       * 
       * HashMap<String, Object> parameters = new HashMap<String, Object>(); String yearId =
       * dao.settingCurrentYear(vars); request.setAttribute("TypeList", dao.gettingTypeList(vars));
       * request.getRequestDispatcher(jspPage).include(request, response); }
       */
      else if (action.equals("GetGroupTypeList")) {

        StringBuilder sb = new StringBuilder();

        try {
          List<BudgetDetailsReportVO> groupList = dao.gettingGroupList(vars,
              request.getParameter("inpBudgetTypeId"));

          sb.append("<GetGroup>");

          for (BudgetDetailsReportVO vo1 : groupList) {
            sb.append("<BudgetGroup>");
            sb.append("<ID>" + vo1.getBudgetGroupId() + "</ID>");
            sb.append("<Name><![CDATA[" + vo1.getBudgetGroupName() + "]]></Name>");
            sb.append("</BudgetGroup>");
          }
          sb.append("</GetGroup>");
        } catch (Exception e) {
          log4j.error("Exception in  Getting Group List : ", e);
        } finally {
          response.setContentType("text/xml");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(sb.toString());
        }

      } else if (action.equals("GetCostCenterList")) {

        StringBuilder sb = new StringBuilder();

        try {
          List<BudgetDetailsReportVO> deptList = dao.getCostCenterDeptList(vars,
              request.getParameter("inpOrgId"));

          sb.append("<GetDept>");

          for (BudgetDetailsReportVO vo1 : deptList) {
            sb.append("<CostCenterDept>");
            sb.append("<ID>" + vo1.getDeptId() + "</ID>");
            sb.append("<Name><![CDATA[" + vo1.getDeptName() + "]]></Name>");
            sb.append("</CostCenterDept>");
          }
          sb.append("</GetDept>");
        } catch (Exception e) {
          log4j.error("Exception in  Getting dept List : ", e);
        } finally {
          response.setContentType("text/xml");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(sb.toString());
        }

      }

    } catch (Exception e) {
      log4j.error("Exception in budgetdetail.java :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
