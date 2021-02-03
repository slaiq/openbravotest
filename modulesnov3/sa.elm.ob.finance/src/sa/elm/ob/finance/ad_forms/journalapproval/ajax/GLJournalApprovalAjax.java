package sa.elm.ob.finance.ad_forms.journalapproval.ajax;

import java.io.IOException;
import java.sql.Connection;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;

import sa.elm.ob.finance.ad_forms.journalapproval.dao.GLJournalApprovalDAO;
import sa.elm.ob.finance.ad_forms.journalapproval.header.GLjournalApproval;
import sa.elm.ob.finance.ad_forms.journalapproval.vo.GLJournalApprovalVO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

public class GLJournalApprovalAjax extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    String action = "";
    Connection con = null;
    try {
      con = getConnection();
      GLJournalApprovalDAO dao = new GLJournalApprovalDAO(con);
      VariablesSecureApp vars = new VariablesSecureApp(request);
      action = (request.getParameter("act") == null ? "" : request.getParameter("act"));
      if ("GetGlJournalList".equals(action)) {
        JSONObject result = new JSONObject();
        try {
          result.put("page", "0");
          result.put("total", "0");
          result.put("records", "0");
          result.put("rows", new JSONArray());

          String search = request.getParameter("_search").toString();
          JSONObject searchAttr = new JSONObject();
          searchAttr.put("rows", request.getParameter("rows").toString());
          searchAttr.put("page", request.getParameter("page").toString());
          searchAttr.put("search", search);
          searchAttr.put("sortName", request.getParameter("sidx").toString());
          searchAttr.put("sortType", request.getParameter("sord").toString());
          GLJournalApprovalVO searchVO = new GLJournalApprovalVO();
          if (search != null && search.equals("true")) {
            if (!StringUtils.isEmpty(request.getParameter("orgname")))
              searchVO.setOrgName(request.getParameter("orgname").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("ledger")))
              searchVO.setLedger(request.getParameter("ledger").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("documentno")))
              searchVO.setDocumentNo(request.getParameter("documentno").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("documentdate")))
              searchVO.setDocumentDate(request.getParameter("documentdate_s") + "##"
                  + request.getParameter("documentdate").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("accountdate")))
              searchVO.setAccountDate(request.getParameter("accountdate_s") + "##"
                  + request.getParameter("accountdate").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("period")))
              searchVO.setPeriod(request.getParameter("period").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("Opening")))
              searchVO.setOpening(request.getParameter("Opening").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("description")))
              searchVO.setDescription(request.getParameter("description").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("debitamount")))
              searchVO
                  .setDebitAmount(request.getParameter("debitamount").replaceAll("[^0-9\\.]", ""));
            if (!StringUtils.isEmpty(request.getParameter("creditamount")))
              searchVO.setCreditAmount(
                  request.getParameter("creditamount").replaceAll("[^0-9\\.]", ""));
            if (!StringUtils.isEmpty(request.getParameter("requester")))
              searchVO.setRequester(request.getParameter("requester").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("requestdate")))
              searchVO.setRequesterDate(request.getParameter("requestdate_s") + "##"
                  + request.getParameter("requestdate").replace("'", "''"));

            if (StringUtils.equals(searchVO.getCreditAmount(), "."))
              searchVO.setCreditAmount("0");
            if (StringUtils.equals(searchVO.getDebitAmount(), "."))
              searchVO.setDebitAmount("0");
          }

          result = dao.getGetGlJournalList(vars, vars.getClient(), vars.getOrg(), vars.getRole(),
              vars.getUser(), searchAttr, searchVO, false);
        } catch (final Exception e) {
          log4j.error("Exception in POApprovalAjax - GetPaymentOutList : ", e);
        } finally {
          response.setContentType("application/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(result.toString());
        }
      } else if (action.equals("GetJournalLines")) {
        JSONObject result = new JSONObject();
        try {
          JSONObject json = null;
          JSONArray jsonArray = null;
          String strJournalId = request.getParameter("inpJournalId");
          // Get Header Details
          GLJournalApprovalVO headerVO = dao.getGlJournalDetails(strJournalId);
          /*
           * log4j .debug("entered"); log4j .debug("ledger"+ headerVO.getLedger()); log4j
           * .debug("ledger"+( StringUtils.isEmpty(headerVO.getLedger()) ? "" :
           * headerVO.getLedger())) ;
           */
          result.put("ledger",
              StringUtils.isEmpty(headerVO.getLedger()) ? "" : headerVO.getLedger());
          result.put("Description", headerVO.getDescription());
          result.put("documentno", headerVO.getDescription());
          result.put("documentDate",
              StringUtils.isEmpty(headerVO.getDocumentDate()) ? ""
                  : (Utility.formatDate(headerVO.getDocumentDate().substring(0, 10),
                      Utility.dateFormat)));
          result.put("period", headerVO.getPeriod());
          result.put("accountingDate",
              StringUtils.isEmpty(headerVO.getAccountDate()) ? ""
                  : (Utility.formatDate(headerVO.getAccountDate().substring(0, 10),
                      Utility.dateFormat)));
          result.put("debitAmount", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, headerVO.getDebitAmount()));
          result.put("creditAmount", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, headerVO.getCreditAmount()));
          result.put("currency", headerVO.getCurSymbol());
          result.put("JournalBatch", headerVO.getApprovalId());

          // Get Line Details
          List<GLJournalApprovalVO> ls = dao.getJournalLines(strJournalId);
          if (ls != null && ls.size() > 0) {
            jsonArray = new JSONArray();
            for (GLJournalApprovalVO vo : ls) {
              json = new JSONObject();
              json.put("lineo", vo.getLineno());
              json.put("Description", vo.getLineDescription());
              json.put("account", vo.getLineAccount());
              json.put("linedebit", Utility.getNumberFormat(vars,
                  Utility.numberFormat_PriceRelation, vo.getLineDebit()));
              json.put("linecredit", Utility.getNumberFormat(vars,
                  Utility.numberFormat_PriceRelation, vo.getLineCredit()));
              json.put("uom", vo.getLineUom());
              json.put("qty", vo.getLineQty());
              jsonArray.put(json);
            }
            result.put("journalList", jsonArray);
          }

          // Get Approval List
          ls = dao.getApprovalHistory(strJournalId);
          if (ls != null && ls.size() > 0) {
            jsonArray = new JSONArray();
            boolean hasRework = false;
            String status = "";
            for (GLJournalApprovalVO vo : ls) {
              json = new JSONObject();
              log4j.debug("vostatus:" + vo.getStatus());
              json.put("userName", vo.getApproverName());
              json.put("roleName", vo.getApproverRole());
              // json.put("date", Utility.formatDate(vo.getApprovedDate().substring(0, 19),
              // Utility.dateTimeFormat));
              json.put("date", UtilityDAO.convertToHijriDate(vo.getApprovedDate()));
              if (vo.getStatus().equals("ASSREW")) {
                status = "Assigned Rework";
              } else if (vo.getStatus().equals("REACT")) {
                status = "Reactivate";
              } else if (vo.getStatus().equals("APP")) {
                status = "Approved";
              }
              json.put("status", status);
              json.put("comments", vo.getComments());
              jsonArray.put(json);
              if (hasRework == false && vo.getStatus().toLowerCase().contains("REW"))
                hasRework = true;
            }
            result.put("approvalList", jsonArray);
            if (hasRework)
              result.put("reqType", "R");
          }
        } catch (final Exception e) {
          log4j.error("Exception in GLjournalApprovalAjax - GetJournalLines : ", e);
        } finally {
          response.setContentType("application/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(result.toString());
        }
      } else if (action.equals("GetMultiPaymentOutDetails")) {
        JSONArray jsonArray = null;
        try {
          String strFinPaymentId = request.getParameter("inpFinPaymentIdList");
          jsonArray = dao.getMultiPaymentOutDetails(strFinPaymentId);
        } catch (final Exception e) {
          log4j.error("Exception in POApprovalAjax - GetPaymentOutLines : ", e);
        } finally {
          response.setContentType("application/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(jsonArray.toString());
        }
      } else if (action.equals("MultiSubmit")) {
        JSONObject result = null;
        String type = request.getParameter("type");
        JSONArray journalList = new JSONArray(request.getParameter("journalList"));
        String strComments = request.getParameter("inpComments");
        try {
          if (type.equals("A")) {
            result = dao.approveMultiJournal(vars, journalList, strComments);
          } else if (type.equals("RW")) {
            result = dao.reworkMultiJournal(vars, journalList, strComments);
          }
        } catch (final Exception e) {
          log4j.error("Exception in POApprovalAjax - MultiSubmit : ", e);
        } finally {
          response.setContentType("application/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(result.toString());
        }
      } else if (action.equals("GetPaymentOutApprovalCount")) {
        JSONObject jsonObject = new JSONObject();
        try {
          jsonObject.put("restrict", "1");
          jsonObject.put("count", "0");

          boolean checkRestriction = Boolean
              .valueOf(request.getParameter("checkRestriction") == null ? "true"
                  : request.getParameter("checkRestriction"));
          if (checkRestriction) {
            // Check Form Access
            if (Utility.checkFormAccess(vars.getClient(), vars.getRole(),
                GLjournalApproval.formGlJournalApproval))
              jsonObject.put("restrict", "0");
            // Check Form Access based on Document Rule
            if (Utility.haveAccesstoWindow(vars.getClient(), vars.getOrg(), vars.getRole(),
                vars.getUser(), Resource.PAYMENT_OUT_RULE))
              jsonObject.put("restrict", "0");
          } else {
            jsonObject.put("restrict", "0");
          }

          if (jsonObject.getInt("restrict") == 0) {
            JSONObject searchAttr = new JSONObject();
            searchAttr.put("rows", "10000");
            searchAttr.put("page", "1");
            searchAttr.put("search", "false");

            JSONObject result = dao.getGetGlJournalList(vars, vars.getClient(), vars.getOrg(),
                vars.getRole(), vars.getUser(), searchAttr, null, true);
            jsonObject.put("count", result.getInt("records"));
          }
        } catch (final Exception e) {
          log4j.error("Exception in POApprovalAjax - GetPaymentOutApprovalCount : ", e);
        } finally {
          response.setContentType("application/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(jsonObject.toString());
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in POApprovalAjax", e);
    }
  }
}