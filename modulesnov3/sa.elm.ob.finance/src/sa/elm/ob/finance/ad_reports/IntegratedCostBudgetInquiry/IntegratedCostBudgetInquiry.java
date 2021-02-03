package sa.elm.ob.finance.ad_reports.IntegratedCostBudgetInquiry;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.project.Project;
import org.openbravo.model.sales.SalesRegion;

import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.ad_callouts.dao.FundsReqMangementDAO;
import sa.elm.ob.finance.ad_reports.IntegratedCostBudgetInquiry.dao.IntegratedCostBudgetInquiryDAO;
import sa.elm.ob.finance.ad_reports.IntegratedCostBudgetInquiry.dao.IntegratedCostBudgetInquiryDAOImpl;

/**
 * 
 * @author Priyanka Ranjan on 22/04/2019
 * 
 */
// servlet file for Integrated Cost Budget Inquiry Report
public class IntegratedCostBudgetInquiry extends HttpSecureAppServlet {

  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.finance/jsp/IntegratedCostBudgetInquiry/integratedcostbudgetinquiry.jsp";

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    try {
      String action = (request.getParameter("inpAction") == null ? ""
          : request.getParameter("inpAction"));
      VariablesSecureApp vars = new VariablesSecureApp(request);
      String orgId = OBContext.getOBContext().getCurrentOrganization().getId();
      String strReportName = "";
      String hq_organization_id = "D67E1FAA6B9445758EE62BAB1A211C3A";
      String budgetTypeValue = null;
      String orgName = null;
      String accountName = null;
      String deptName = null;
      String subAccountName = null;

      IntegratedCostBudgetInquiryDAO dao = new IntegratedCostBudgetInquiryDAOImpl(getConnection());
      if (action.equals("")) {
        request.setAttribute("client", dao.getClientInfo(vars.getClient()));

        request.setAttribute("acctschema", dao.getAcctSchema(
            Utility.getContext(this, vars, "#AccessibleOrgTree", "IntegratedCostBudgetInquiry"),
            Utility.getContext(this, vars, "#User_Client", "IntegratedCostBudgetInquiry"), null));

        request.setAttribute("funds",
            dao.getBudgetType(
                Utility.getContext(this, vars, "#AccessibleOrgTree", "IntegratedCostBudgetInquiry"),
                Utility.getContext(this, vars, "#User_Client", "IntegratedCostBudgetInquiry"),
                vars.getRole()));
        // set HQ org value as default in organization list
        Organization org = OBDal.getInstance().get(Organization.class, hq_organization_id);

        if (org != null) {
          orgName = org.getSearchKey() + "-" + org.getName();
          request.setAttribute("SelectedOrg", hq_organization_id);
          request.setAttribute("SelectedOrgName", orgName);
        }

        else {
          request.setAttribute("SelectedOrg", "0");
          request.setAttribute("SelectedOrgName", "select");
        }
        // Set Budget Control Unit As Default Department
        EfinBudgetControlParam obj_control_param = FundsReqMangementDAO
            .getControlParam(vars.getClient());

        if (obj_control_param != null) {
          SalesRegion dept = obj_control_param.getBudgetcontrolunit();
          deptName = dept.getSearchKey() + "-" + dept.getName();
          request.setAttribute("SelectedDept", dept.getId());
          request.setAttribute("SelectedDeptName", deptName);

        }

        // localization support
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        request.getRequestDispatcher(jspPage).include(request, response);
      } else if (action.equals("getSubAccountAgainstAccount")) {
        JSONObject jsob = new JSONObject();
        jsob = dao.getSubAccountAgainstAccount(request.getParameter("inpAccountParam"));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsob.toString());
      } else if (action.equals("getParentAccount")) {
        JSONObject jsob = new JSONObject();
        jsob = dao.getParentAccount(vars.getClient(), request.getParameter("searchTerm"),
            Integer.parseInt(request.getParameter("pageLimit")),
            Integer.parseInt(request.getParameter("page")));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsob.toString());
      } else if (action.equals("getDepartment")) {
        JSONObject jsob = new JSONObject();
        jsob = dao.getDepartment(vars.getClient(), request.getParameter("searchTerm"),
            Integer.parseInt(request.getParameter("pageLimit")),
            Integer.parseInt(request.getParameter("page")));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsob.toString());
      } else if (action.equals("getSubAccount")) {
        JSONObject jsob = new JSONObject();
        jsob = dao.getSubAccount(vars.getClient(), request.getParameter("searchTerm"),
            Integer.parseInt(request.getParameter("pageLimit")),
            Integer.parseInt(request.getParameter("page")),
            request.getParameter("inpAccountParam"));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsob.toString());
      } else if (action.equals("getOrganization")) {
        JSONObject jsob = new JSONObject();
        jsob = dao.getOrganization(orgId, vars.getClient(), request.getParameter("searchTerm"),
            Integer.parseInt(request.getParameter("pageLimit")),
            Integer.parseInt(request.getParameter("page")));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsob.toString());
      } else if (action.equals("getData")) {

        String inpOrg = (request.getParameter("inpOrg") == null
            || request.getParameter("inpOrg") == "") ? null : request.getParameter("inpOrg");

        String inpcAcctSchemaId = (request.getParameter("inpcAcctSchemaId") == null
            || request.getParameter("inpcAcctSchemaId") == "") ? null
                : request.getParameter("inpcAcctSchemaId");

        String inpcCampaignId_IN = (request.getParameter("inpcCampaignId_IN") == null
            || request.getParameter("inpcCampaignId_IN") == "") ? null
                : request.getParameter("inpcCampaignId_IN");

        String inpClient = (request.getParameter("inpClient") == null
            || request.getParameter("inpClient") == "") ? null : request.getParameter("inpClient");

        String inpAccount = (request.getParameter("inpAccount") == null
            || request.getParameter("inpAccount") == "") ? null
                : request.getParameter("inpAccount");

        String inpDept = (request.getParameter("inpDept") == null
            || request.getParameter("inpDept") == "") ? null : request.getParameter("inpDept");

        String inpSubAccount = (request.getParameter("inpSubAccount") == null
            || request.getParameter("inpSubAccount") == "") ? null
                : request.getParameter("inpSubAccount");

        String strClientId = vars.getClient();
        if (inpcCampaignId_IN != null) {
          Campaign budtypeObj = OBDal.getInstance().get(Campaign.class, inpcCampaignId_IN);
          budgetTypeValue = budtypeObj.getEfinBudgettype();
        }

        JSONObject list = dao.selectLines(vars, inpcCampaignId_IN, strClientId, inpOrg, inpAccount,
            inpDept, inpSubAccount);

        request.setAttribute("acctschema",
            dao.getAcctSchema(
                Utility.getContext(this, vars, "#AccessibleOrgTree", "IntegratedCostBudgetInquiry"),
                Utility.getContext(this, vars, "#User_Client", "IntegratedCostBudgetInquiry"),
                inpcAcctSchemaId));
        request.setAttribute("funds",
            dao.getBudgetType(
                Utility.getContext(this, vars, "#AccessibleOrgTree", "IntegratedCostBudgetInquiry"),
                Utility.getContext(this, vars, "#User_Client", "IntegratedCostBudgetInquiry"),
                vars.getRole()));
        request.setAttribute("client", dao.getClientInfo(vars.getClient()));

        if (inpOrg != null) {
          Organization org = OBDal.getInstance().get(Organization.class, inpOrg);
          orgName = org.getSearchKey() + "-" + org.getName();
        }
        if (inpAccount != null && !inpAccount.equals("0")) {
          ElementValue acct = OBDal.getInstance().get(ElementValue.class, inpAccount);
          accountName = acct.getSearchKey() + "-" + acct.getName();
        }
        if (inpDept != null && !inpDept.equals("0")) {
          SalesRegion dept = OBDal.getInstance().get(SalesRegion.class, inpDept);
          deptName = dept.getSearchKey() + "-" + dept.getName();
        }
        if (inpSubAccount != null && !inpSubAccount.equals("0")) {
          Project subacct = OBDal.getInstance().get(Project.class, inpSubAccount);
          subAccountName = subacct.getSearchKey() + "-" + subacct.getName();
        }

        request.setAttribute("LinesList", list);
        request.setAttribute("SelectedOrg", inpOrg);
        request.setAttribute("SelectedOrgName", orgName);
        request.setAttribute("SelectedClient", inpClient);
        request.setAttribute("SelectedAccount", inpAccount);
        request.setAttribute("SelectedAccountName", accountName);
        request.setAttribute("SelectedDept", inpDept);
        request.setAttribute("SelectedDeptName", deptName);
        request.setAttribute("SelectedSubAccount", inpSubAccount);
        request.setAttribute("SelectedSubAccountName", subAccountName);
        request.setAttribute("SelectedSchema", inpcAcctSchemaId);
        request.setAttribute("SelectedFunds", inpcCampaignId_IN);
        request.setAttribute("SelectedBudgetTypeValue", budgetTypeValue);
        // localization support
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.getRequestDispatcher(jspPage).include(request, response);
      } else if (action.equals("getReport")) {

        String inpOrg = (request.getParameter("inpOrg") == null
            || request.getParameter("inpOrg") == "") ? null : request.getParameter("inpOrg");

        String inpcCampaignId_IN = (request.getParameter("inpcCampaignId_IN") == null
            || request.getParameter("inpcCampaignId_IN") == "") ? null
                : request.getParameter("inpcCampaignId_IN");

        String inpClient = (request.getParameter("inpClient") == null
            || request.getParameter("inpClient") == "") ? null : request.getParameter("inpClient");

        String inpAccount = (request.getParameter("inpAccount") == null
            || request.getParameter("inpAccount") == "") ? null
                : request.getParameter("inpAccount");

        String inpDept = (request.getParameter("inpDept") == null
            || request.getParameter("inpDept") == "") ? null : request.getParameter("inpDept");

        String inpSubAccount = (request.getParameter("inpSubAccount") == null
            || request.getParameter("inpSubAccount") == "") ? null
                : request.getParameter("inpSubAccount");

        if (inpcCampaignId_IN != null) {
          Campaign budtypeObj = OBDal.getInstance().get(Campaign.class, inpcCampaignId_IN);
          budgetTypeValue = budtypeObj.getEfinBudgettype();
        }

        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("inpOrg", inpOrg);
        parameters.put("inpcCampaignId_IN", inpcCampaignId_IN);
        parameters.put("inpClient", inpClient);
        parameters.put("ParentAccountId", inpAccount);
        parameters.put("DeptId", inpDept);
        parameters.put("SubAccountId", inpSubAccount);
        parameters.put("inpBudgetType", budgetTypeValue);

        strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/IntegratedCostBudgetInquiryReport/IntegratedCostBudgetInquiryReport.jrxml";
        String strOutput = "pdf";

        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      } else if (action.equals("getReportExcel")) {

        String inpOrg = (request.getParameter("inpOrg") == null
            || request.getParameter("inpOrg") == "") ? null : request.getParameter("inpOrg");

        String inpcCampaignId_IN = (request.getParameter("inpcCampaignId_IN") == null
            || request.getParameter("inpcCampaignId_IN") == "") ? null
                : request.getParameter("inpcCampaignId_IN");

        String inpClient = (request.getParameter("inpClient") == null
            || request.getParameter("inpClient") == "") ? null : request.getParameter("inpClient");

        String inpAccount = (request.getParameter("inpAccount") == null
            || request.getParameter("inpAccount") == "") ? null
                : request.getParameter("inpAccount");

        String inpDept = (request.getParameter("inpDept") == null
            || request.getParameter("inpDept") == "") ? null : request.getParameter("inpDept");

        String inpSubAccount = (request.getParameter("inpSubAccount") == null
            || request.getParameter("inpSubAccount") == "") ? null
                : request.getParameter("inpSubAccount");

        if (inpcCampaignId_IN != null) {
          Campaign budtypeObj = OBDal.getInstance().get(Campaign.class, inpcCampaignId_IN);
          budgetTypeValue = budtypeObj.getEfinBudgettype();
        }

        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("inpOrg", inpOrg);
        parameters.put("inpcCampaignId_IN", inpcCampaignId_IN);
        parameters.put("inpClient", inpClient);
        parameters.put("ParentAccountId", inpAccount);
        parameters.put("DeptId", inpDept);
        parameters.put("SubAccountId", inpSubAccount);
        parameters.put("inpBudgetType", budgetTypeValue);

        strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/IntegratedCostBudgetInquiryReport/IntegratedCostBudgetInquiryReport.jrxml";
        String strOutput = "xls";

        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      }

    } catch (Exception e) {
      log4j.error("Exception in IntegratedCostBudgetInquiry.java :", e);
    }
  }

}