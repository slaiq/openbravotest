package sa.elm.ob.finance.ad_process.RDVProcess.ajax;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;
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
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.finance.EfinPenaltyAction;
import sa.elm.ob.finance.EfinPenaltyTypes;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.ad_process.RDVProcess.DAO.PenaltyActionDAO;
import sa.elm.ob.finance.ad_process.RDVProcess.vo.PenaltyActionVO;
import sa.elm.ob.utility.util.Utility;

public class PenaltyActionAjax extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Connection con = null;
    PenaltyActionDAO dao = null;
    JSONArray jsonArray = null;
    JSONObject jsonResponse = null;
    BigDecimal diffpenltyAmt = BigDecimal.ZERO;
    try {
      OBContext.setAdminMode();
      con = getConnection();
      dao = new PenaltyActionDAO(con);
      String action = (request.getParameter("action") == null ? ""
          : request.getParameter("action"));
      String inpRDVTxnLineId = (request.getParameter("inpRDVTxnLineId") == null ? ""
          : request.getParameter("inpRDVTxnLineId"));
      if (action.equals("GetSalaryDetails")) {
        JSONObject result = new JSONObject();
        try {
          DecimalFormat decimalFormat = new DecimalFormat("0.#####");

          result.put("page", "0");
          result.put("total", "0");
          result.put("records", "0");
          result.put("rows", new JSONArray());
          String actiondate = "";
          String searchFlag = request.getParameter("_search");
          JSONObject searchAttr = new JSONObject();
          searchAttr.put("rows", request.getParameter("rows").toString());
          searchAttr.put("page", request.getParameter("page").toString());
          searchAttr.put("search", searchFlag);
          searchAttr.put("sortName", request.getParameter("sidx").toString());
          searchAttr.put("sortType", request.getParameter("sord").toString());
          searchAttr.put("offset", "0");

          if (Boolean.valueOf(searchFlag)) {
            if (!StringUtils.isEmpty(request.getParameter("actiontype"))) {
              searchAttr.put("actiontype", request.getParameter("actiontype").replace("'", "''"));
            }
            if (!StringUtils.isEmpty(request.getParameter("appno")))
              searchAttr.put("appno", request.getParameter("appno").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("Amt")))
              searchAttr.put("Amt",
                  decimalFormat.format(Double.valueOf(request.getParameter("Amt"))));
            if (!StringUtils.isEmpty(request.getParameter("actionDate"))) {
              actiondate = Utility.convertToGregorian(request.getParameter("StartDate"));

              searchAttr.put("actionDate", actiondate);
            }
            if (!StringUtils.isEmpty(request.getParameter("penaltytype")))
              searchAttr.put("penaltytype", request.getParameter("penaltytype").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("penaltypercentage")))
              searchAttr.put("penaltypercentage",
                  decimalFormat.format(Double.valueOf(request.getParameter("penaltypercentage"))));
            if (!StringUtils.isEmpty(request.getParameter("penaltyamount")))
              searchAttr.put("penaltyamount",
                  decimalFormat.format(Double.valueOf(request.getParameter("penaltyamount"))));
            if (!StringUtils.isEmpty(request.getParameter("actionreason")))
              searchAttr.put("actionreason",
                  request.getParameter("actionreason").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("actionjustfication")))
              searchAttr.put("actionjustfication",
                  request.getParameter("actionjustfication").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("associatedbp")))
              searchAttr.put("associatedbp",
                  request.getParameter("associatedbp").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("bpname")))
              searchAttr.put("bpname", request.getParameter("bpname").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("freezepenalty")))
              searchAttr.put("freezepenalty",
                  request.getParameter("freezepenalty").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("amarsarfno")))
              searchAttr.put("amarsarfno", request.getParameter("amarsarfno").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("amarsarfamount")))
              searchAttr.put("amarsarfamount",
                  decimalFormat.format(Double.valueOf(request.getParameter("amarsarfamount"))));
            if (!StringUtils.isEmpty(request.getParameter("accounttype")))
              searchAttr.put("accounttype", request.getParameter("accounttype").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("uniquecode")))
              searchAttr.put("uniquecode", request.getParameter("uniquecode").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("uniquename")))
              searchAttr.put("uniquename", request.getParameter("uniquename").replace("'", "''"));
          }
          result = dao.getSalaryList(vars.getClient(), request.getParameter("inpRDVTxnLineId"),
              searchAttr);
        } catch (Exception e) {
          log4j.error("Exception in PenaltyActionAjax - GetSalaryDetails : ", e);
        } finally {
          response.setContentType("application/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(result.toString());
        }
      } else if (action.equals("GetSalaryDetails")) {
        log4j.debug("getting salarydetails Ajax");
        inpRDVTxnLineId = (request.getParameter("inpRDVTxnLineId") == null ? ""
            : request.getParameter("inpRDVTxnLineId"));
        String sortColName = request.getParameter("sidx");
        String sortColType = request.getParameter("sord");
        String searchFlag = request.getParameter("_search");
        int rows = Integer.parseInt(request.getParameter("rows"));
        int page = Integer.parseInt(request.getParameter("page"));
        int totalPage = 1;
        int offset = 0;
        PenaltyActionVO vo = new PenaltyActionVO();
        if (searchFlag != null && searchFlag.equals("true")) {
          /*
           * if (request.getParameter("element") != null)
           * vo.setElement(request.getParameter("element").replace("'", "''")); if
           * (request.getParameter("value") != null)
           * vo.setValue(request.getParameter("value").replace("'", "''")); if
           * (request.getParameter("Percentage") != null)
           * vo.setPercentage(request.getParameter("Percentage").replace("'", "''"));
           */

        }
        int totalRecord = dao.getSalaryCount(vars.getClient(), inpRDVTxnLineId);
        log4j.debug("totalRecord:" + totalRecord);
        if (totalRecord > 0) {
          totalPage = totalRecord / rows;
          if (totalRecord % rows > 0)
            totalPage += 1;
          offset = ((page - 1) * rows);

          if (page > totalPage) {
            page = totalPage;
            offset = ((page - 1) * rows);

          }
        } else {
          page = 0;
          totalPage = 0;
        }
        List<PenaltyActionVO> list = dao.getSalaryList(vars.getClient(), inpRDVTxnLineId, vo, rows,
            offset, sortColName, sortColType, searchFlag);
        response.setContentType("text/xml");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        StringBuffer xmlData = new StringBuffer("<?xml version='1.0' encoding='utf-8'?><rows>");
        log4j.debug("totalRecord:" + totalRecord + ">> list size" + list.size());
        if (list.size() > 0) {
          for (int i = 0; i < list.size(); i++) {
            PenaltyActionVO VO = (PenaltyActionVO) list.get(i);
            xmlData.append("<row id='" + VO.getEfin_penalty_action_id() + "'>");
            xmlData.append("<cell><![CDATA[" + VO.getSeqno() + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + " " + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + VO.getTrx_app_no() + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + VO.getAction() + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + VO.getActionDate() + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + VO.getAmount() + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + VO.getPenaltyType() + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + VO.getPenaltyPercentage() + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + VO.getPenaltyamount() + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + VO.getActionReason() + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + VO.getActionJustification() + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + VO.getBpartnerid() + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + VO.getBpartnername() + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + VO.getFreezePenalty() + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + VO.getInvoiceId() + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + VO.getAmarsarfAmount() + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + VO.getPenaltyaccount_type() + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + VO.getPenaltyuniquecode() + "]]></cell>");
            xmlData.append("<cell><![CDATA[" + VO.getUniquecodeName() + "]]></cell>");
            xmlData.append("</row>");
          }
          xmlData.append("<page>" + page + "</page><total>" + totalPage + "</total><records>"
              + totalRecord + "</records>");
        } else
          xmlData
              .append("<page>" + 0 + "</page><total>" + 0 + "</total><records>" + 0 + "</records>");
        xmlData.append("</rows>");
        response.getWriter().write(xmlData.toString());
      } else if (action.equals("savepenaltyaction")) {
        String oldAction = null;
        String operation = request.getParameter("oper");
        String seqno = request.getParameter("Sequence");
        String trxappno = request.getParameter("appno");
        String actiontype = request.getParameter("actiontype");
        String actionDate = request.getParameter("actionDate");
        String amount = request.getParameter("Amt");
        String penalty_type = request.getParameter("penaltytype");
        String penalty_per = request.getParameter("penaltypercentage");
        String penalty_amt = request.getParameter("penaltyamount");
        String actionreason = request.getParameter("actionreason");
        String actionjust = request.getParameter("actionjustfication");
        String bpartnerId = request.getParameter("associatedbp");
        String bpname = request.getParameter("bpname");
        String freezepenalty = request.getParameter("freezepenalty");
        String invoice = request.getParameter("amarsarfno");
        String amarsarfamount = request.getParameter("amarsarfamount");
        String RDVTrxlineId = request.getParameter("inpRDVTxnLineId");
        String penalty_account = request.getParameter("accounttype");
        String uniquecode = request.getParameter("uniquecode");
        String strpenaltyactId = request.getParameter("id");
        log4j.debug("operation" + operation);
        log4j.debug("penaltytype12s" + penalty_type);
        log4j.debug("amarsarfamount" + amarsarfamount);
        if (operation.equals("add") || strpenaltyactId.length() != 32) {
          dao.getPenaltyAction(seqno, trxappno, vars.getClient(), actiontype, actionDate, amount,
              penalty_type, penalty_per, penalty_amt.replaceAll(",", ""), actionreason, actionjust,
              bpartnerId, bpname, freezepenalty, invoice, amarsarfamount, RDVTrxlineId,
              penalty_account, uniquecode, false);

          jsonResponse = new JSONObject();
          jsonResponse.put("result", "1");
          response.setCharacterEncoding("UTF-8");
          response.getWriter().write(jsonResponse.toString());

        } else if (operation.equals("edit") && strpenaltyactId.length() == 32) {
          EfinPenaltyTypes oldpenaltyTypeId = null;

          EfinPenaltyAction objLine = OBDal.getInstance().get(EfinPenaltyAction.class,
              strpenaltyactId);
          if (!penalty_type.equals(objLine.getEfinPenaltyTypes().getId()))
            oldpenaltyTypeId = objLine.getEfinPenaltyTypes();
          objLine
              .setEfinPenaltyTypes(OBDal.getInstance().get(EfinPenaltyTypes.class, penalty_type));
          if (!actiontype.equals(objLine.getAction())) {
            oldAction = objLine.getAction();
            objLine.setAction(actiontype);
          }
          if (!StringUtils.isEmpty(actionDate))
            objLine.setActionDate(dao.convertGregorian(actionDate));
          diffpenltyAmt = new BigDecimal(penalty_amt.replaceAll(",", ""))
              .subtract(objLine.getPenaltyAmount());
          if (!StringUtils.isEmpty(penalty_per))
            objLine.setPenaltyPercentage(new BigDecimal(penalty_per));
          else
            objLine.setPenaltyPercentage(new BigDecimal(0));
          objLine.setPenaltyAmount(new BigDecimal(penalty_amt.replaceAll(",", "")));
          objLine.setActionReason(actionreason == null ? null : actionreason);
          objLine.setActionJustification(actionjust == null ? null : actionjust);
          if (bpartnerId != null)
            objLine.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, bpartnerId));
          else {
            objLine.setBusinessPartner(null);
          }
          objLine.setName(bpartnerId == null ? null : bpname);
          if (freezepenalty != null && freezepenalty.equals("Y")) {
            objLine.setFreezePenalty(true);
          } else {
            objLine.setFreezePenalty(false);
          }
          objLine.setPenaltyAccountType(penalty_account == null ? null : penalty_account);
          if (uniquecode != null) {
            objLine.setPenaltyUniquecode(
                OBDal.getInstance().get(AccountingCombination.class, uniquecode));
          } else
            objLine.setPenaltyUniquecode(null);
          OBDal.getInstance().save(objLine);
          OBDal.getInstance().flush();
          PenaltyActionDAO.insertPenaltyHeader(objLine, objLine.getEfinRdvtxnline(), diffpenltyAmt,
              oldpenaltyTypeId, oldAction);

          jsonResponse = new JSONObject();
          jsonResponse.put("result", "1");
          response.setCharacterEncoding("UTF-8");
          response.getWriter().write(jsonResponse.toString());

        } else if (operation.equals("del")) {
          String penaltyactId = request.getParameter("id");
          EfinPenaltyAction peanltyaction = OBDal.getInstance().get(EfinPenaltyAction.class,
              penaltyactId);
          PenaltyActionDAO.deletepenaltyHed(peanltyaction);

          jsonResponse = new JSONObject();
          jsonResponse.put("isExists", "1");
          response.setCharacterEncoding("UTF-8");
          response.getWriter().write(jsonResponse.toString());
        }
      } else if (action.equals("savebulkpenaltyaction")) {
        PenaltyActionDAO.bulkPenalty(request, response, con);

        jsonResponse = new JSONObject();
        jsonResponse.put("result", "1");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonResponse.toString());
      }

      else if (action.equals("getbpName")) {
        StringBuffer sb = new StringBuffer();
        try {
          BusinessPartner partner = OBDal.getInstance().get(BusinessPartner.class,
              request.getParameter("inpbpId"));
          sb.append("<GetBpName>");
          if (partner != null)
            sb.append("<value>" + partner.getName() + "</value>");
          else
            sb.append("<value>" + "" + "</value>");
          sb.append("</GetBpName>");
        } catch (final Exception e) {
          log4j.error("Exception in PayrollProcess - GetPayrollYear : ", e);
        } finally {
          response.setContentType("text/xml");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(sb.toString());
        }
      }

      else if (action.equals("getUniqueCodeName")) {
        StringBuffer sb = new StringBuffer();
        try {
          AccountingCombination com = OBDal.getInstance().get(AccountingCombination.class,
              request.getParameter("inpcomId"));
          sb.append("<GetComName>");
          if (com != null && com.getEfinUniquecodename() != null)
            sb.append("<value>" + com.getEfinUniquecodename() + "</value>");
          else {
            sb.append("<value>" + "" + "</value>");
          }
          sb.append("</GetComName>");
        } catch (final Exception e) {
          log4j.error("Exception in PayrollProcess - getUniqueCodeName : ", e);
        } finally {
          response.setContentType("text/xml");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(sb.toString());
        }
      }

      else if (action.equals("getThershold")) {
        StringBuffer sb = new StringBuffer();
        try {
          EfinPenaltyTypes penaltytype = OBDal.getInstance().get(EfinPenaltyTypes.class,
              request.getParameter("inppenaltyTypeId"));
          sb.append("<GetPenalty>");
          if (penaltytype.getThreshold() != null) {
            sb.append("<value>" + penaltytype.getThreshold() + "</value>");
            sb.append("<valuepresent>" + 1 + "</valuepresent>");
          }
          sb.append("<type>" + penaltytype.getDeductiontype().getPenaltyLogic() + "</type>");
          sb.append("<edituniquecode>" + penaltytype.isEdituniqcode() + "</edituniquecode>");
          sb.append("</GetPenalty>");
        } catch (final Exception e) {
          log4j.error("Exception in PayrollProcess - GetPayrollYear : ", e);
        } finally {
          response.setContentType("text/xml");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(sb.toString());
        }
      } else if (action.equals("chkPenaltytypeGoingdownNegativeval")) {
        EfinPenaltyTypes penaltytype = null;
        try {
          String RDVTxnLineId = request.getParameter("inpRDVTxnLineId");
          log4j.debug("RDVTxnLineId" + RDVTxnLineId);
          String penaltyAmt = request.getParameter("penaltyAmt");
          log4j.debug("penaltyAmt" + penaltyAmt);
          String penaltyType = request.getParameter("penaltyType");
          log4j.debug("penaltyType" + penaltyType);
          EfinRDVTxnline line = OBDal.getInstance().get(EfinRDVTxnline.class, RDVTxnLineId);

          String id = request.getParameter("id");
          if (id.contains("jqg"))
            id = null;

          if (request.getParameter("type").equals("del")) {
            EfinPenaltyAction penaction = OBDal.getInstance().get(EfinPenaltyAction.class, id);
            penaltytype = penaction.getEfinPenaltyTypes();
            penaltyAmt = penaction.getPenaltyAmount().toString();

          } else
            penaltytype = OBDal.getInstance().get(EfinPenaltyTypes.class, penaltyType);

          String isExist = PenaltyActionDAO.chkPenaltytypeGoingdownNegativeval(line,
              new BigDecimal(penaltyAmt), penaltytype, id, request.getParameter("type"));
          log4j.debug("isExist:" + isExist);
          jsonResponse = new JSONObject();
          jsonResponse.put("isExists", isExist);
          response.setCharacterEncoding("UTF-8");
          response.getWriter().write(jsonResponse.toString());

        } catch (final Exception e) {
          log4j.error("Exception in PayrollProcess - GetPayrollYear : ", e);
        }
      } else if (action.equals("getbusinesspartner")) {
        JSONObject jsob = new JSONObject();
        String bpartner = null;
        if (request.getParameter("pType") != null && !request.getParameter("pType").equals("0")) {
          EfinPenaltyTypes penalty = OBDal.getInstance().get(EfinPenaltyTypes.class,
              request.getParameter("pType"));
          if (penalty.getDeductiontype().getPenaltyLogic().equals("ECA")) {
            EfinRDVTxnline rdvLine = OBDal.getInstance().get(EfinRDVTxnline.class,
                request.getParameter("rdvln"));
            bpartner = rdvLine.getEfinRdv().getBusinessPartner().getId();
          }
        }
        jsob = PenaltyActionDAO.getBusinessPartnerList(vars.getClient(),
            request.getParameter("searchTerm"), Integer.parseInt(request.getParameter("pageLimit")),
            Integer.parseInt(request.getParameter("page")), vars.getRole(), vars.getOrg(),
            bpartner);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsob.toString());
      } else if (action.equals("getUniquecodeList")) {
        JSONObject jsob = new JSONObject();
        jsob = PenaltyActionDAO.getUniquecodeList(vars.getClient(),
            request.getParameter("searchTerm"), Integer.parseInt(request.getParameter("pageLimit")),
            Integer.parseInt(request.getParameter("page")), vars.getRole(), vars.getOrg(),
            request.getParameter("type"), request.getParameter("bpartnerId"));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsob.toString());
      }

      else if (action.equals("getTotalMatchAmt")) {
        String actionId = request.getParameter("id");
        try {
          if (request.getParameter("id").contains("jqg")) {
            actionId = null;
          }
          String isExist = PenaltyActionDAO.getTotalMatchAmt(
              request.getParameter("inpRDVTxnLineId"),
              new BigDecimal(request.getParameter("penaltyAmt").toString().replaceAll(",", "")),
              actionId);
          log4j.debug("isExist:" + isExist);
          jsonResponse = new JSONObject();
          jsonResponse.put("isExists", isExist);
          response.setCharacterEncoding("UTF-8");
          response.getWriter().write(jsonResponse.toString());
        } catch (final Exception e) {
          log4j.error("Exception in PayrollProcess - getTotalMatchAmt : ", e);
        } finally {
        }
      } else if (action.equals("getTotalMatchAmtforSelectedRecords")) {
        try {
          String isExist = PenaltyActionDAO.getTotalMatchAmtforSelectedRecords(
              new BigDecimal(request.getParameter("penaltyAmt").toString().replaceAll(",", "")),
              request.getParameter("penaltypercentage"),
              request.getParameter("bulkpenaltyamtlogic"), request);
          log4j.debug("isExist:" + isExist);
          jsonResponse = new JSONObject();
          jsonResponse.put("isExists", isExist);
          response.setCharacterEncoding("UTF-8");
          response.getWriter().write(jsonResponse.toString());
        } catch (final Exception e) {
          log4j.error("Exception in PayrollProcess - getTotalMatchAmt : ", e);
        } finally {
        }
      } else if (action.equals("getDeductionType")) {
        try {
          EfinPenaltyTypes penaltytype = OBDal.getInstance().get(EfinPenaltyTypes.class,
              request.getParameter("inppenaltyTypeId"));
          jsonResponse = new JSONObject();
          jsonResponse.put("value", penaltytype.getThreshold());
          jsonResponse.put("valuepresent", 1);
          jsonResponse.put("type", penaltytype.getDeductiontype().getPenaltyLogic());
          jsonResponse.put("edituniquecode", penaltytype.isEdituniqcode());
          response.setCharacterEncoding("UTF-8");
          response.getWriter().write(jsonResponse.toString());

        } catch (final Exception e) {
          log4j.error("Exception in PayrollProcess - getTotalMatchAmt : ", e);
        } finally {
        }
      } else if (action.equals("getPenaltyTypeBaseAction")) {
        try {
          List<PenaltyActionVO> penlist = dao.getpenaltyType(vars.getClient(),
              request.getParameter("inpaction"), vars.getLanguage());
          jsonArray = new JSONArray();
          for (PenaltyActionVO vo : penlist) {
            jsonResponse = new JSONObject();
            jsonResponse.put("Threshold", vo.getThershold());
            if (vo.getThershold() != null)
              jsonResponse.put("Valuepresent", 1);
            else
              jsonResponse.put("Valuepresent", 0);
            jsonResponse.put("ID", vo.getPenaltyId());
            jsonResponse.put("Name", vo.getPenaltyname());
            jsonArray.put(jsonResponse);
          }
          response.setCharacterEncoding("UTF-8");
          response.getWriter().write(jsonArray.toString());

        } catch (final Exception e) {
          log4j.error("Exception in PayrollProcess - getTotalMatchAmt : ", e);
        } finally {
        }
      }

    } catch (Exception e) {
      log4j.error("Error in PenaltyActionAjax : ", e);
    } finally {
      OBContext.restorePreviousMode();
      try {
        if (con != null) {
          con.close();
        }
      } catch (final SQLException e) {
        log4j.error("Error in PenaltyActionAjax : ", e);
      }
    }
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    doPost(request, response);
  }

  public String getServletInfo() {
    return "ContractAjax Servlet";
  }
}