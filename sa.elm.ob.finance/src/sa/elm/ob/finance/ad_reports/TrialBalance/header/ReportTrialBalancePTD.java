package sa.elm.ob.finance.ad_reports.TrialBalance.header;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.businessUtility.Tree;
import org.openbravo.erpCommon.businessUtility.TreeData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.Utility;

import sa.elm.ob.finance.ad_reports.TrialBalance.dao.ReportTrialBalancePTDDAO;

public class ReportTrialBalancePTD extends HttpSecureAppServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.finance/jsp/TrialBalance/ReportTrialBalancePTD.jsp";

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    try {
      Date today = new Date();
      SimpleDateFormat sf = new SimpleDateFormat("dd-MM-yyyy");
      String action = (request.getParameter("inpAction") == null ? ""
          : request.getParameter("inpAction"));
      VariablesSecureApp vars = new VariablesSecureApp(request);
      String orgId = OBContext.getOBContext().getCurrentOrganization().getId();
      ReportTrialBalancePTDDAO dao = new ReportTrialBalancePTDDAO(getConnection());
      if (action.equals("")) {

        request.setAttribute("today", sf.format(today));
        request.setAttribute("organization", dao.getOrganization(null,
            Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD")));
        request.setAttribute("acctschema",
            dao.getAcctSchema(
                Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
                Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"), null));
        request.setAttribute("period",
            dao.getPeriod(
                Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
                Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"), null));
        String currentPeriodId = dao.getCurrentPeriod(vars,
            Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
            Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"));
        request.setAttribute("SelectedPeriod", currentPeriodId);
        request.setAttribute("SelectedToPeriod", currentPeriodId);
        request.setAttribute("funds",
            dao.getFunds(
                Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
                Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"), null,
                vars.getRole()));
        // localization support
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        request.getRequestDispatcher(jspPage).include(request, response);
      } else if (action.equals("getPTDData") || action.equals("getPTDDataLink")) {
        String inpOrg = (request.getParameter("inpOrg") == null
            || request.getParameter("inpOrg") == "") ? null : request.getParameter("inpOrg");
        String inpcFromPeriodId = (request.getParameter("inpcFromPeriodId") == null
            || request.getParameter("inpcFromPeriodId") == "") ? null
                : request.getParameter("inpcFromPeriodId");
        String inpcToPeriodId = (request.getParameter("inpcToPeriodId") == null
            || request.getParameter("inpcToPeriodId") == "") ? null
                : request.getParameter("inpcToPeriodId");
        String inpcAcctSchemaId = (request.getParameter("inpcAcctSchemaId") == null
            || request.getParameter("inpcAcctSchemaId") == "") ? null
                : request.getParameter("inpcAcctSchemaId");
        String inpNotInitialBalance = "Y";
        String inpPageNo = request.getParameter("inpPageNo");
        String inpcElementValueIdFrom = (request.getParameter("inpcElementValueIdFrom") == null
            || request.getParameter("inpcElementValueIdFrom") == "") ? null
                : request.getParameter("inpcElementValueIdFrom");
        String inpElementValueIdFrom_DES = (request
            .getParameter("inpElementValueIdFrom_DES") == null
            || request.getParameter("inpElementValueIdFrom_DES") == "") ? null
                : request.getParameter("inpElementValueIdFrom_DES");
        String inpcElementValueIdTo = (request.getParameter("inpcElementValueIdTo") == null
            || request.getParameter("inpcElementValueIdTo") == "") ? null
                : request.getParameter("inpcElementValueIdTo");
        String inpElementValueIdTo_DES = (request.getParameter("inpElementValueIdTo_DES") == null
            || request.getParameter("inpElementValueIdTo_DES") == "") ? null
                : request.getParameter("inpElementValueIdTo_DES");
        String inpcCampaignId_IN = (request.getParameter("inpcCampaignId_IN") == null
            || request.getParameter("inpcCampaignId_IN") == "") ? null
                : request.getParameter("inpcCampaignId_IN");
        String inpGroupBy = request.getParameter("inpGroupBy");
        String inpcAccountId = (request.getParameter("inpcAccountId") == null
            || request.getParameter("inpcAccountId") == "") ? null
                : request.getParameter("inpcAccountId");
        String CSalesRegionId_IN = (request.getParameter("CSalesRegionId_IN") == null
            || request.getParameter("CSalesRegionId_IN") == "")
                ? ((request.getParameter("inpcSalesregionId_IN") == null
                    || request.getParameter("inpcSalesregionId_IN") == "") ? null
                        : request.getParameter("inpcSalesregionId_IN"))
                : request.getParameter("CSalesRegionId_IN");
        if (CSalesRegionId_IN != null && !CSalesRegionId_IN.equals("")) {
          int count = dao.getdepartment(
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"),
              CSalesRegionId_IN.replaceFirst(",", ""));
          if (count > 0)
            CSalesRegionId_IN = null;
        }
        String CBPartnerId_IN = (request.getParameter("CBPartnerId_IN") == null
            || request.getParameter("CBPartnerId_IN") == "") ? null
                : request.getParameter("CBPartnerId_IN");
        String CActivityId_IN = (request.getParameter("CActivityId_IN") == null
            || request.getParameter("CActivityId_IN") == "") ? null
                : request.getParameter("CActivityId_IN");
        String CProjectId_IN = (request.getParameter("CProjectId_IN") == null
            || request.getParameter("CProjectId_IN") == "") ? null
                : request.getParameter("CProjectId_IN");
        String CUser1Id_IN = (request.getParameter("CUser1Id_IN") == null
            || request.getParameter("CUser1Id_IN") == "") ? null
                : request.getParameter("CUser1Id_IN");
        String CUser2Id_IN = (request.getParameter("CUser2Id_IN") == null
            || request.getParameter("CUser2Id_IN") == "") ? null
                : request.getParameter("CUser2Id_IN");
        String uniqueCode = (request.getParameter("inpUniqueCode") == null
            || request.getParameter("inpUniqueCode") == "") ? null
                : request.getParameter("inpUniqueCode");
        String strFrmDate = "", strFrmPerStDate = "", strStrDateFC = "", strDateTo = "",
            strAccountFromValue = null, strAccountToValue = null;
        try {

          strFrmDate = dao.getPedStrEndDate(
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"),
              inpcFromPeriodId, null);

          strFrmDate = new SimpleDateFormat("dd-MM-yyyy")
              .format(new SimpleDateFormat("yyyy-MM-dd").parse(strFrmDate));
          strDateTo = dao.getPedStrEndDate(
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"), null,
              inpcToPeriodId);
          strDateTo = new SimpleDateFormat("dd-MM-yyyy")
              .format(new SimpleDateFormat("yyyy-MM-dd").parse(strDateTo));
          // get the start date of selected From Period
          strFrmPerStDate = dao.getPedStrDate(
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"),
              inpcFromPeriodId);
          strFrmPerStDate = new SimpleDateFormat("dd-MM-yyyy")
              .format(new SimpleDateFormat("yyyy-MM-dd").parse(strFrmPerStDate));

          // Get the Last Opening and closing Balance Entry Date
          // If closing and opening is not happened then assign financial year start date

          // strStrDateFC = dao.selectLastOpeningBalanceDate(
          // Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
          // Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"),
          // inpcFromPeriodId);
          // if (StringUtils.isNotEmpty(strStrDateFC)) {
          // strStrDateFC = new SimpleDateFormat("dd-MM-yyyy")
          // .format(new SimpleDateFormat("yyyy-MM-dd").parse(strStrDateFC));
          // } else {
          // get start date of Fiscal Calendar
          strStrDateFC = dao.getFCStartDate(
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"));
          strStrDateFC = new SimpleDateFormat("dd-MM-yyyy")
              .format(new SimpleDateFormat("yyyy-MM-dd").parse(strStrDateFC));

          SimpleDateFormat in = new SimpleDateFormat("dd-MM-yyyy");
          Date formatdate = in.parse(strFrmDate);
          Date todate = in.parse(strDateTo);
          if (formatdate.after(todate)) {
            request.setAttribute("InvalidDate", "Y");
          }
        } catch (ParseException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        String strTreeOrg = TreeData.getTreeOrg(this, vars.getClient());
        if (inpcElementValueIdFrom != null)
          strAccountFromValue = dao.getAccountValue(inpcElementValueIdFrom);
        if (inpcElementValueIdTo != null)
          strAccountToValue = dao.getAccountValue(inpcElementValueIdTo);
        String strOrgFamily = getFamily(strTreeOrg, inpOrg);
        String strClientId = "("
            + Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance") + ")";

        JSONObject list = dao.selectLines(vars, strFrmDate,
            (inpNotInitialBalance.equals("Y") ? "O" : null),
            (inpNotInitialBalance.equals("Y") ? null : "O"), strClientId, null, CBPartnerId_IN,
            null, CProjectId_IN, CSalesRegionId_IN, inpcCampaignId_IN, CActivityId_IN, CUser1Id_IN,
            CUser2Id_IN, inpcAcctSchemaId, strOrgFamily,
            Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"),
            Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
            DateTimeData.nDaysAfter(this, strDateTo, "1"), strAccountFromValue, strAccountToValue,
            strStrDateFC, strFrmPerStDate, uniqueCode, inpcElementValueIdFrom);

        request.setAttribute("organization", dao.getOrganization(inpOrg,
            Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD")));
        request.setAttribute("acctschema",
            dao.getAcctSchema(
                Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
                Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"),
                inpcAcctSchemaId));
        request.setAttribute("period",
            dao.getPeriod(
                Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
                Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"),
                inpcFromPeriodId));
        request.setAttribute("funds",
            dao.getFunds(
                Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
                Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"),
                inpcCampaignId_IN, vars.getRole()));
        request.setAttribute("LinesList", list);
        request.setAttribute("SelectedOrg", inpOrg);
        request.setAttribute("SelectedSchema", inpcAcctSchemaId);
        request.setAttribute("SelectedPeriod", inpcFromPeriodId);
        request.setAttribute("SelectedFunds", inpcCampaignId_IN);
        request.setAttribute("SelectedToPeriod", inpcToPeriodId);
        request.setAttribute("SelectedToPeriod", inpcToPeriodId);
        request.setAttribute("inpcElementValueIdFrom", inpcElementValueIdFrom);
        request.setAttribute("inpcElementValueIdTo", inpcElementValueIdTo);
        request.setAttribute("inpElementValueIdFrom_DES",
            dao.getAccountDescription(inpcElementValueIdFrom));
        request.setAttribute("inpElementValueIdTo_DES",
            dao.getAccountDescription(inpcElementValueIdTo));
        if (action.equals("getPTDData")) {
          if (CBPartnerId_IN != null)
            request.setAttribute("bpList",
                dao.getEntity(
                    Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
                    Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"),
                    CBPartnerId_IN.replaceFirst(",", "")));
          if (CSalesRegionId_IN != null)
            request.setAttribute("deptList",
                dao.getDepartment(
                    Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
                    Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"),
                    CSalesRegionId_IN.replaceFirst(",", "")));
          if (CProjectId_IN != null)
            request.setAttribute("projList",
                dao.getProject(
                    Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
                    Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"),
                    CProjectId_IN.replaceFirst(",", "")));
          if (CActivityId_IN != null)
            request.setAttribute("actList",
                dao.getActivity(
                    Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
                    Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"),
                    CActivityId_IN.replaceFirst(",", "")));
          if (CUser1Id_IN != null)
            request.setAttribute("user1List",
                dao.getUser1(
                    Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
                    Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"),
                    CUser1Id_IN.replaceFirst(",", "")));
          if (CUser2Id_IN != null)
            request.setAttribute("user2List",
                dao.getUser2(
                    Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
                    Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"),
                    CUser2Id_IN.replaceFirst(",", "")));
          // localization support
          response.setContentType("text/html; charset=UTF-8");
          response.setCharacterEncoding("UTF-8");

          request.getRequestDispatcher(jspPage).include(request, response);
        } else {
          request.setAttribute("bpList", dao.getEntityfromuniquecode(
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"), uniqueCode));
          request.setAttribute("deptList", dao.getDepartmentfromuniquecode(
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"), uniqueCode));
          request.setAttribute("projList", dao.getProjectfromuniquecode(
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"), uniqueCode));
          request.setAttribute("actList", dao.getActivityfromuniquecode(
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"), uniqueCode));
          request.setAttribute("user1List", dao.getUser1fromuniquecode(
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"), uniqueCode));
          request.setAttribute("user2List", dao.getUser2fromuniquecode(
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalancePTD"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalancePTD"), uniqueCode));
          // localization support
          response.setContentType("text/html; charset=UTF-8");
          response.setCharacterEncoding("UTF-8");

          request.getRequestDispatcher(jspPage).include(request, response);
        }

      }
    } catch (Exception e) {
      log4j.error("Exception in SOA.java :", e);
    }
  }

  private String getFamily(String strTree, String strChild) throws IOException, ServletException {
    return Tree.getMembers(this, strTree, strChild);
  }
}