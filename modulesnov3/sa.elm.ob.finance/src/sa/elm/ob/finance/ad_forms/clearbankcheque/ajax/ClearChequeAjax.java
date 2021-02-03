package sa.elm.ob.finance.ad_forms.clearbankcheque.ajax;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;

import sa.elm.ob.finance.ad_forms.clearbankcheque.dao.ClearChequeDao;
import sa.elm.ob.finance.ad_forms.clearbankcheque.vo.ClearChequeVO;

/**
 * 
 * @author Gowtham
 *
 */
public class ClearChequeAjax extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Connection con = null;
    ClearChequeDao dao = null;
    try {
      con = getConnection();
      dao = new ClearChequeDao(con);
      String action = (request.getParameter("action") == null ? ""
          : request.getParameter("action"));
      OBContext.setAdminMode();
      if (action.equals("getOrg")) {
        JSONObject result = null;
        try {
          result = dao.getOrganization(vars.getClient(), request.getParameter("searchTerm"),
              Integer.parseInt(request.getParameter("pageLimit")),
              Integer.parseInt(request.getParameter("page")));

        } catch (Exception e) {
          log4j.error("Exception in clear bank cheque -->getOrg ", e);
        } finally {
          response.setContentType("text/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(result.toString());
        }
      } else if (action.equals("getChequeStatus")) {
        JSONObject result = null;
        try {
          result = dao.getChequeStatus(vars.getClient(), request.getParameter("searchTerm"),
              Integer.parseInt(request.getParameter("pageLimit")),
              Integer.parseInt(request.getParameter("page")));

        } catch (Exception e) {
          log4j.error("Exception in clear bank cheque -->getChequeStatus ", e);
        } finally {
          response.setContentType("text/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(result.toString());
        }
      } else if (action.equals("GetPaymentList")) {
        JSONObject result = new JSONObject();
        try {
          result.put("page", "0");
          result.put("total", "0");
          result.put("records", "0");
          result.put("rows", new JSONArray());
          String searchFlag = request.getParameter("_search");
          JSONObject searchAttr = new JSONObject();
          searchAttr.put("rows", request.getParameter("rows").toString());
          searchAttr.put("page", request.getParameter("page").toString());
          searchAttr.put("search", searchFlag);
          searchAttr.put("sortName", request.getParameter("sidx").toString());
          searchAttr.put("sortType", request.getParameter("sord").toString());
          searchAttr.put("offset", "0");
          searchAttr.put("fetchAllRecord", Boolean.valueOf(request.getParameter("getAllRecords")));

          ClearChequeVO vo = assignParameters(request);
          result = dao.getPaymentList(vo, searchAttr);
        } catch (Exception e) {
          log4j.error("Exception in clear bank cheque -->GetPaymentList ", e);
        } finally {
          // Localization support
          response.setContentType("application/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(result.toString());
        }
      } else if (action.equals("getPaymentMethod")) {
        JSONObject result = null;
        try {
          result = dao.getPaymentMethod(request.getParameter("bankId"),
              request.getParameter("acctId"), request.getParameter("searchTerm"),
              Integer.parseInt(request.getParameter("pageLimit")),
              Integer.parseInt(request.getParameter("page")));

        } catch (Exception e) {
          log4j.error("Exception in clear bank cheque -->getPaymentMethod ", e);
        } finally {
          response.setContentType("text/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(result.toString());
        }
      } else if (action.equals("getBank")) {
        JSONObject result = null;
        try {
          result = dao.getBank(vars.getClient(), request.getParameter("searchTerm"),
              Integer.parseInt(request.getParameter("pageLimit")),
              Integer.parseInt(request.getParameter("page")));

        } catch (Exception e) {
          log4j.error("Exception in clear bank cheque -->getBank ", e);
        } finally {
          response.setContentType("text/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(result.toString());
        }
      } else if (action.equals("getAccountNo")) {
        JSONObject result = null;
        try {
          result = dao.getAccountNo(request.getParameter("bankId"),
              request.getParameter("searchTerm"),
              Integer.parseInt(request.getParameter("pageLimit")),
              Integer.parseInt(request.getParameter("page")));

        } catch (Exception e) {
          log4j.error("Exception in clear bank cheque -->getAccountNo ", e);
        } finally {
          response.setContentType("text/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(result.toString());
        }
      } else if (action.equals("getAcctPayMethod")) {
        JSONObject result = new JSONObject();
        try {
          String bankId = request.getParameter("inpBank");
          String acctId = request.getParameter("inpAcctNo");
          if (!StringUtils.isEmpty(bankId)) {
            // get bank acct
            JSONObject bankAcctJson = dao.getAccountNo(bankId, "", 0, 0);
            if (bankAcctJson.has("data")) {
              JSONArray jsonAcctArray = bankAcctJson.getJSONArray("data");
              if (jsonAcctArray.length() > 0) {
                result.put("acctId", jsonAcctArray.getJSONObject(0).getString("id"));
                result.put("acctNo", jsonAcctArray.getJSONObject(0).getString("recordIdentifier"));
              }
            }

            // get payment Method
            if (StringUtils.isEmpty(acctId) && result.has("acctId")) {
              acctId = result.optString("acctId");
            }
            JSONObject paymentJson = dao.getPaymentMethod(bankId, acctId, "", 0, 0);
            if (paymentJson.has("data")) {
              JSONArray jsonPaymentArray = paymentJson.getJSONArray("data");
              if (jsonPaymentArray.length() > 0) {
                result.put("paymentId", jsonPaymentArray.getJSONObject(0).getString("id"));
                result.put("paymentName",
                    jsonPaymentArray.getJSONObject(0).getString("recordIdentifier"));
              }
            }
          }
        } catch (Exception e) {
          log4j.error("Exception in clear bank cheque -->getAccountNo ", e);
        } finally {
          response.setContentType("text/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(result.toString());
        }

      } else if (action.equals("Save")) {
        String result = "error";
        JSONObject jsonResult = null;
        try {
          JSONObject json = new JSONObject(request.getParameter("inpPaymentList"));
          String bankId = request.getParameter("inpAcctNo");
          result = dao.savePaymentDetails(json, bankId);
          jsonResult = new JSONObject();
          if (result.contains("futureDate")) {
            jsonResult.put("msg", "futureDate");
            jsonResult.put("docNo", result.replace("futureDate", ""));
          } else {
            jsonResult.put("msg", result);
          }
          ClearChequeVO vo = assignParameters(request);
          dao.clearTempColumns(vo);

        } catch (Exception e) {
          log4j.error("Exception in clear bank cheque save action ", e);
        } finally {
          response.setContentType("text/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(jsonResult.toString());
        }
      } else if (action.equals("lock") || action.equals("unlock")) {
        String result = "false";
        JSONObject jsonResult = null;
        String decision = null;
        try {
          JSONObject json = new JSONObject(request.getParameter("inpPaymentList"));
          if (action.equals("lock")) {
            decision = "lock";
          }
          if (action.equals("unlock")) {
            decision = "unlock";
          }
          result = dao.lockUnlockPaymentDetails(json, decision);
          jsonResult = new JSONObject();
          if (result.contains("fieldMandatory")) {
            jsonResult.put("msg", "fieldMandatory");
            jsonResult.put("docNo", !result.isEmpty() ? result.replace("fieldMandatory", "") : "");
          } else {
            jsonResult.put("msg", result);
          }
          // jsonResult.put("msg", result);

        } catch (Exception e) {
          log4j.error("Exception in clear bank cheque lock/unlock ", e);
        } finally {
          response.setContentType("text/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(jsonResult.toString());
        }
      } else if (action.equals("applySeq")) {
        String result = "";
        JSONObject jsonResult = null;
        try {
          ClearChequeVO vo = assignParameters(request);
          result = dao.applySequenceRange(vo);
          jsonResult = new JSONObject();
          jsonResult.put("msg", result);
        } catch (Exception e) {
          log4j.error("Exception in clear bank cheque applySeq", e);
        } finally {
          response.setContentType("text/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(jsonResult.toString());
        }
      } else if (action.equals("SaveAll")) {
        String result = "";
        JSONObject jsonResult = null;
        try {
          ClearChequeVO vo = assignParameters(request);
          JSONObject json = new JSONObject(request.getParameter("inpPaymentList"));
          result = dao.saveAll(vo, json);
          jsonResult = new JSONObject();
          jsonResult.put("msg", result);
        } catch (Exception e) {
          log4j.error("Exception in clear bank cheque saveAll action ", e);
        } finally {
          response.setContentType("text/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(jsonResult.toString());
        }
      } else if (action.equals("clearTemp")) {
        String result = "";
        JSONObject jsonResult = null;
        try {
          ClearChequeVO vo = assignParameters(request);
          result = dao.clearTempColumns(vo);
          jsonResult = new JSONObject();
          jsonResult.put("msg", result);
        } catch (Exception e) {
          log4j.error("Exception in clear bank cheque clearTemp action ", e);
        } finally {
          response.setContentType("text/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(jsonResult.toString());
        }
      }

    } catch (final Exception e) {
      log4j.error("Error in clearChequeAjax : ", e);
    } finally {
      OBContext.restorePreviousMode();
      try {
        con.close();
      } catch (final SQLException e) {
        log4j.error("Error in clearChequeAjax : ", e);
      }
    }
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    doPost(request, response);
  }

  /**
   * create a vo object based on request params.
   * 
   * @param request
   * @return
   */
  public ClearChequeVO assignParameters(HttpServletRequest request) {
    ClearChequeVO vo = new ClearChequeVO();
    try {
      if (!StringUtils.isEmpty(request.getParameter("inpOrgId"))
          && !request.getParameter("inpOrgId").equals("0"))
        vo.setOrganization(request.getParameter("inpOrgId"));
      if (!StringUtils.isEmpty(request.getParameter("inpPaymentMethod"))
          && !request.getParameter("inpPaymentMethod").equals("0"))
        vo.setPaymentMethod(request.getParameter("inpPaymentMethod"));
      if (!StringUtils.isEmpty(request.getParameter("inpBank"))
          && !request.getParameter("inpBank").equals("0"))
        vo.setBank(request.getParameter("inpBank"));
      if (!StringUtils.isEmpty(request.getParameter("inpAcctNo"))
          && !request.getParameter("inpAcctNo").equals("0"))
        vo.setBankAccNo(request.getParameter("inpAcctNo"));
      if (!StringUtils.isEmpty(request.getParameter("inpFromPaySeq")))
        vo.setPaymentDocSeqNoFrom(request.getParameter("inpFromPaySeq"));
      if (!StringUtils.isEmpty(request.getParameter("inpToPaySeq")))
        vo.setPaymentDocSeqNoTo(request.getParameter("inpToPaySeq"));
      if (!StringUtils.isEmpty(request.getParameter("inpClearBankCqNo")))
        vo.setClearBankChqNo(request.getParameter("inpClearBankCqNo"));
      if (!StringUtils.isEmpty(request.getParameter("inpClearBankCqDate")))
        vo.setClearBankChqDate(request.getParameter("inpClearBankCqDate"));
      if (!StringUtils.isEmpty(request.getParameter("inpClearBank")))
        vo.setClearingBank(request.getParameter("inpClearBank"));
      if (!StringUtils.isEmpty(request.getParameter("inpSentBankDate")))
        vo.setSentBankDate(request.getParameter("inpSentBankDate"));
      if (!StringUtils.isEmpty(request.getParameter("inpFromPayDate")))
        vo.setPaymentDateFrom(request.getParameter("inpFromPayDate"));
      if (!StringUtils.isEmpty(request.getParameter("inpToPayDate")))
        vo.setPaymentDateTo(request.getParameter("inpToPayDate"));
      if (!StringUtils.isEmpty(request.getParameter("inpStartSeq")))
        vo.setStartSequence(request.getParameter("inpStartSeq"));
      if (!StringUtils.isEmpty(request.getParameter("inpEndSeq")))
        vo.setEndSequence(request.getParameter("inpEndSeq"));
      if (!StringUtils.isEmpty(request.getParameter("inpPopChqDate")))
        vo.setPopClearBankChqDate(request.getParameter("inpPopChqDate"));
      if (!StringUtils.isEmpty(request.getParameter("inpPopBank")))
        vo.setPopclearingBank(request.getParameter("inpPopBank"));
      if (!StringUtils.isEmpty(request.getParameter("inpReconciledDate")))
        vo.setReconcileDate(request.getParameter("inpReconciledDate"));
      if (!StringUtils.isEmpty(request.getParameter("inpReconcileNo")))
        vo.setReconcileNo(request.getParameter("inpReconcileNo"));
      if (!StringUtils.isEmpty(request.getParameter("inpLock")))
        vo.setLockIndicator(request.getParameter("inpLock"));
      if (!StringUtils.isEmpty(request.getParameter("inpBankSentdate")))
        vo.setPopBankSentDate(request.getParameter("inpBankSentdate"));
      if (!StringUtils.isEmpty(request.getParameter("inpChequeReceiveDate")))
        vo.setPopChequeReceiveDate(request.getParameter("inpChequeReceiveDate"));
      if (!StringUtils.isEmpty(request.getParameter("inpChequeStatus")))
        vo.setChequeStatus(request.getParameter("inpChequeStatus"));
      if (!StringUtils.isEmpty(request.getParameter("inpPopChequeStatus")))
        vo.setPopChequeStatus(request.getParameter("inpPopChequeStatus"));
    } catch (Exception e) {
      log4j.error("Error in assignParameters : ", e);
    }
    return vo;
  }
}
