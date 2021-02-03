package sa.elm.ob.finance.ad_forms.poapproval.ajax;

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

import sa.elm.ob.finance.ad_forms.poapproval.dao.PaymentOutApprovalDAO;
import sa.elm.ob.finance.ad_forms.poapproval.header.PaymentOutApproval;
import sa.elm.ob.finance.ad_forms.poapproval.vo.PaymentOutApprovalVO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.Utility;

public class PaymentOutApprovalAjax extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    String action = "";
    Connection con = null;
    try {
      con = getConnection();
      PaymentOutApprovalDAO dao = new PaymentOutApprovalDAO(con);
      VariablesSecureApp vars = new VariablesSecureApp(request);
      action = (request.getParameter("act") == null ? "" : request.getParameter("act"));
      if ("GetPaymentOutList".equals(action)) {
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
          PaymentOutApprovalVO searchVO = new PaymentOutApprovalVO();
          if (search != null && search.equals("true")) {
            if (!StringUtils.isEmpty(request.getParameter("orgname")))
              searchVO.setOrgName(request.getParameter("orgname").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("documentno")))
              searchVO.setDocumentNo(request.getParameter("documentno").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("paymentdate")))
              searchVO.setPaymentDate(request.getParameter("paymentdate_s") + "##"
                  + request.getParameter("paymentdate").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("referenceno")))
              searchVO.setReferenceNo(request.getParameter("referenceno").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("payto")))
              searchVO.setbPartnerName(request.getParameter("payto").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("payfrom")))
              searchVO.setFinAcctName(request.getParameter("payfrom").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("amount")))
              searchVO.setPaymentAmount(request.getParameter("amount").replaceAll("[^0-9\\.]", ""));
            if (!StringUtils.isEmpty(request.getParameter("usedcredit")))
              searchVO.setUsedCredit(request.getParameter("usedcredit").replace("'", "''"));
            /*
             * if(!StringUtils.isEmpty(request.getParameter("priority")))
             * searchVO.setPriority(request.getParameter("priority").replace("'", "''"));
             */
            if (!StringUtils.isEmpty(request.getParameter("requester")))
              searchVO.setRequester(request.getParameter("requester").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("requestdate")))
              searchVO.setRequesterDate(request.getParameter("requestdate_s") + "##"
                  + request.getParameter("requestdate").replace("'", "''"));

            if (StringUtils.equals(searchVO.getPaymentAmount(), "."))
              searchVO.setPaymentAmount("0");
          }

          result = dao.getPaymentOutList(vars, vars.getClient(), vars.getOrg(), vars.getRole(),
              vars.getUser(), searchAttr, searchVO, false);
        } catch (final Exception e) {
          log4j.error("Exception in POApprovalAjax - GetPaymentOutList : ", e);
        } finally {
          response.setContentType("application/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(result.toString());
        }
      } else if (action.equals("GetPaymentOutLines")) {
        JSONObject result = new JSONObject();
        try {
          JSONObject json = null;
          JSONArray jsonArray = null;
          String strFinPaymentId = request.getParameter("inpFinPaymentId");
          // Get Header Details
          PaymentOutApprovalVO headerVO = dao.getPaymentOutDetails(strFinPaymentId);
          // result.put("priority", headerVO.getPriority());
          result.put("amount", Utility.getNumberFormat(vars, Utility.numberFormat_PriceEdition,
              headerVO.getPaymentAmount()));
          result.put("desc", headerVO.getDescription());
          result.put("remarks", headerVO.getNote());
          result.put("reqType", "N");
          result.put("curSymbol", headerVO.getCurSymbol());
          result.put("generatedAmt", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, headerVO.getExpectedAmount()));
          result.put("utilizedAmt", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, headerVO.getUsedCredit()));
          result.put("paymentMethod", headerVO.getPaymentMethodName());

          // Get Line Details
          List<PaymentOutApprovalVO> ls = dao.getPOLines(strFinPaymentId);
          Double totalAmount = 0.00;
          if (ls != null && ls.size() > 0) {
            jsonArray = new JSONArray();
            JSONArray glJsonArray = new JSONArray();
            for (PaymentOutApprovalVO vo : ls) {
              json = new JSONObject();
              json.put("order", vo.getOrderNo());
              json.put("invoice", vo.getInvoiceNo());
              json.put("invoicerefno", vo.getReferenceNo());
              json.put("invoicedate", StringUtils.isEmpty(vo.getPaymentDate()) ? ""
                  : (Utility.formatDate(vo.getPaymentDate().substring(0, 10), Utility.dateFormat)));
              json.put("glitem", vo.getGlItem());
              json.put("duedate", StringUtils.isEmpty(vo.getDueDate()) ? ""
                  : (Utility.formatDate(vo.getDueDate().substring(0, 10), Utility.dateFormat)));
              json.put("invoiceAmount", Utility.getNumberFormat(vars,
                  Utility.numberFormat_PriceRelation, vo.getInvoiceAmount()));
              json.put("expectedAmount", Utility.getNumberFormat(vars,
                  Utility.numberFormat_PriceRelation, vo.getExpectedAmount()));
              json.put("amount", Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
                  vo.getPaidAmount()));
              totalAmount += Double.parseDouble(vo.getPaidAmount());
              if (StringUtils.isEmpty(vo.getGlItem()))
                jsonArray.put(json);
              else
                glJsonArray.put(json);
            }
            result.put("invoiceList", jsonArray);
            result.put("glItemList", glJsonArray);
          }

          result.put("totalAmount",
              Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, totalAmount));

          // Get Approval List
          ls = dao.getApprovalHistory(strFinPaymentId);
          if (ls != null && ls.size() > 0) {
            jsonArray = new JSONArray();
            boolean hasRework = false;
            for (PaymentOutApprovalVO vo : ls) {
              json = new JSONObject();
              json.put("userName", vo.getApproverName());
              json.put("roleName", vo.getApproverRole());
              json.put("date", Utility.formatDate(vo.getApprovedDate().substring(0, 19),
                  Utility.dateTimeFormat));
              json.put("status", vo.getStatus());
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
          log4j.error("Exception in POApprovalAjax - GetPaymentOutLines : ", e);
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
        JSONArray paymentList = new JSONArray(request.getParameter("paymentList"));
        String strComments = request.getParameter("inpComments");
        try {
          if (type.equals("A")) {
            result = dao.approveMultiPayment(vars, paymentList, strComments);
          } else if (type.equals("RW")) {
            result = dao.reworkMultiPayment(vars, paymentList, strComments);
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
                PaymentOutApproval.formPaymentOutApproval))
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

            JSONObject result = dao.getPaymentOutList(vars, vars.getClient(), vars.getOrg(),
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