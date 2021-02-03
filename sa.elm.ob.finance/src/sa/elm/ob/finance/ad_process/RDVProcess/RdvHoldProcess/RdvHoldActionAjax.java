package sa.elm.ob.finance.ad_process.RDVProcess.RdvHoldProcess;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.EfinRdvHoldAction;
import sa.elm.ob.finance.EfinRdvHoldTypes;
import sa.elm.ob.finance.ad_process.RDVProcess.DAO.PenaltyActionDAO;
import sa.elm.ob.utility.util.Utility;

public class RdvHoldActionAjax extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Connection con = null;
    RdvHoldActionDAO dao = null;
    JSONArray jsonArray = null;
    JSONObject jsonResponse = null;
    BigDecimal diffpenltyAmt = BigDecimal.ZERO;
    try {
      OBContext.setAdminMode();
      con = getConnection();
      dao = new RdvHoldActionDAOimpl(con);
      String action = (request.getParameter("action") == null ? ""
          : request.getParameter("action"));

      if (action.equals("GetHoldDetails")) {
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
            if (!StringUtils.isEmpty(request.getParameter("holdtype")))
              searchAttr.put("holdtype", request.getParameter("holdtype").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("holdpercentage")))
              searchAttr.put("holdpercentage",
                  decimalFormat.format(Double.valueOf(request.getParameter("holdpercentage"))));
            if (!StringUtils.isEmpty(request.getParameter("holdamount")))
              searchAttr.put("holdamount",
                  decimalFormat.format(Double.valueOf(request.getParameter("holdamount"))));
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
            if (!StringUtils.isEmpty(request.getParameter("freezehold")))
              searchAttr.put("freezehold", request.getParameter("freezehold").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("amarsarfno")))
              searchAttr.put("amarsarfno", request.getParameter("amarsarfno").replace("'", "''"));
            if (!StringUtils.isEmpty(request.getParameter("amarsarfamount")))
              searchAttr.put("amarsarfamount",
                  decimalFormat.format(Double.valueOf(request.getParameter("amarsarfamount"))));
            // if (!StringUtils.isEmpty(request.getParameter("accounttype")))
            // searchAttr.put("accounttype", request.getParameter("accounttype").replace("'",
            // "''"));
            // if (!StringUtils.isEmpty(request.getParameter("uniquecode")))
            // searchAttr.put("uniquecode", request.getParameter("uniquecode").replace("'", "''"));
            // if (!StringUtils.isEmpty(request.getParameter("uniquename")))
            // searchAttr.put("uniquename", request.getParameter("uniquename").replace("'", "''"));
          }
          if (request.getParameter("inpRDVTxnLineId") != null) {
            result = dao.getHoldList(vars.getClient(), request.getParameter("inpRDVTxnLineId"),
                searchAttr);
          } else {
            result = dao.getHoldTxnList(vars.getClient(), request.getParameter("inpRDVTxnId"),
                searchAttr);
          }
        } catch (Exception e) {
          log4j.error("Exception in RdvHoldActionAjax - GetHoldDetails: ", e);
        } finally {
          response.setContentType("application/json");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(result.toString());
        }
      } else if (action.equals("saveholdaction")) {
        String oldAction = null;
        String operation = request.getParameter("oper");
        String seqno = request.getParameter("Sequence");
        String trxappno = request.getParameter("appno");
        String actiontype = request.getParameter("actiontype");
        String holdActionType = request.getParameter("holdAction");
        String actionDate = request.getParameter("actionDate");
        String amount = request.getParameter("Amt");
        String hold_type = request.getParameter("holdtype");
        String hold_per = request.getParameter("holdpercentage");
        String hold_amt = request.getParameter("holdamount");
        String actionreason = request.getParameter("actionreason");
        String actionjust = request.getParameter("actionjustfication");
        String bpartnerId = request.getParameter("associatedbp");
        String bpname = request.getParameter("bpname");
        String freezehold = request.getParameter("freezehold");
        String invoice = request.getParameter("amarsarfno");
        String amarsarfamount = request.getParameter("amarsarfamount");
        String RDVTrxlineId = request.getParameter("inpRDVTxnLineId");
        String RDVTxnId = request.getParameter("inpRDVTxnId");
        String hold_account = request.getParameter("accounttype");
        String uniquecode = request.getParameter("uniquecode");
        String strholdactId = request.getParameter("id");
        log4j.debug("operation" + operation);
        log4j.debug("holdtype12s" + hold_type);
        log4j.debug("amarsarfamount" + amarsarfamount);
        if (operation.equals("add") || strholdactId.length() != 32) {
          if (!StringUtils.isEmpty(RDVTrxlineId) && !RDVTrxlineId.equals("null")) {
            dao.getHoldAction(seqno, trxappno, vars.getClient(), actiontype, actionDate, amount,
                hold_type, hold_per, hold_amt, actionreason, actionjust, bpartnerId, bpname,
                freezehold, invoice, amarsarfamount, RDVTrxlineId, hold_account, uniquecode, false);
          } else {
            dao.getHoldTxnAction(seqno, trxappno, vars.getClient(), actiontype, actionDate, amount,
                hold_type, hold_per, hold_amt, actionreason, actionjust, bpartnerId, bpname,
                freezehold, invoice, amarsarfamount, RDVTxnId, hold_account, uniquecode, false);
          }

          jsonResponse = new JSONObject();
          jsonResponse.put("result", "1");
          response.setCharacterEncoding("UTF-8");
          response.getWriter().write(jsonResponse.toString());

        } else if (operation.equals("edit") && strholdactId.length() == 32) {
          EfinRdvHoldTypes oldholdTypeId = null;
          if (!StringUtils.isEmpty(RDVTrxlineId) && !RDVTrxlineId.equals("null")) {

            EfinRdvHoldAction objLine = OBDal.getInstance().get(EfinRdvHoldAction.class,
                strholdactId);
            if (!hold_type.equals(objLine.getEfinRdvHoldTypes().getId()))
              oldholdTypeId = objLine.getEfinRdvHoldTypes();
            objLine.setEfinRdvHoldTypes(OBDal.getInstance().get(EfinRdvHoldTypes.class, hold_type));
            if (!actiontype.equals(objLine.getAction())) {
              oldAction = objLine.getAction();
              objLine.setAction(actiontype);
            }
            if (actionDate != null && actionDate != "")
              objLine.setActionDate(dao.convertGregorian(actionDate));
            diffpenltyAmt = new BigDecimal(hold_amt.replaceAll(",", ""))
                .subtract(objLine.getRDVHoldAmount());
            if (hold_per != null && hold_per != "")
              objLine.setRDVHoldPercentage(new BigDecimal(hold_per));
            else
              objLine.setRDVHoldPercentage(new BigDecimal(0));
            objLine.setRDVHoldAmount(new BigDecimal(hold_amt.replaceAll(",", "")));
            objLine.setActionReason(actionreason == null ? null : actionreason);
            objLine.setActionJustification(actionjust == null ? null : actionjust);
            if (bpartnerId != null)
              objLine
                  .setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, bpartnerId));
            else {
              objLine.setBusinessPartner(null);
            }
            objLine.setName(bpartnerId == null ? null : bpname);
            if (freezehold != null && freezehold.equals("Y")) {
              objLine.setFreezeRdvHold(true);
            } else {
              objLine.setFreezeRdvHold(false);
            }
            // objLine.setRDVHoldAccountType(hold_account == null ? null : hold_account);
            // if (uniquecode != null) {
            // objLine.setRDVHoldUniquecode(
            // OBDal.getInstance().get(AccountingCombination.class, uniquecode));
            // } else
            // objLine.setRDVHoldUniquecode(null);
            OBDal.getInstance().save(objLine);
            OBDal.getInstance().flush();
            dao.insertHoldHeader(objLine, objLine.getEfinRdvtxnline(), diffpenltyAmt, oldholdTypeId,
                oldAction);
          } else {
            // for transaction level Edit
            EfinRDVTransaction txn = OBDal.getInstance().get(EfinRDVTransaction.class, RDVTxnId);
            // BigDecimal totalMatch = txn.getMatchAmt().subtract(txn.getADVDeduct())
            // .subtract(txn.getPenaltyAmt());
            BigDecimal totalMatch = txn.getNetmatchAmt();
            BigDecimal lineMatch = BigDecimal.ZERO;
            BigDecimal weigtage = BigDecimal.ZERO;
            BigDecimal totalHoldAmt = BigDecimal.ZERO;
            List<EfinRdvHoldAction> holdActionList = null;
            OBQuery<EfinRdvHoldAction> holdList = OBDal.getInstance()
                .createQuery(EfinRdvHoldAction.class, "e where e.txngroupref=:refId");
            holdList.setNamedParameter("refId", strholdactId);
            holdActionList = holdList.list();
            // grouping the hold amt
            for (EfinRdvHoldAction holdAction : holdActionList) {
              totalHoldAmt = totalHoldAmt.add(holdAction.getAmount());
            }
            totalMatch = totalMatch.add(totalHoldAmt);
            // for (EfinRDVTxnline line : txn.getEfinRDVTxnlineList()) {
            for (EfinRdvHoldAction holdAction : holdActionList) {
              // if (holdAction.isTxn() &&
              // holdAction.getEfinRdvHoldTypes().getId().equals(hold_type)
              // && holdAction.getAction().equals(holdActionType)) {

              if (!hold_type.equals(holdAction.getEfinRdvHoldTypes().getId()))
                oldholdTypeId = holdAction.getEfinRdvHoldTypes();
              holdAction
                  .setEfinRdvHoldTypes(OBDal.getInstance().get(EfinRdvHoldTypes.class, hold_type));
              if (!actiontype.equals(holdAction.getAction())) {
                oldAction = holdAction.getAction();
                holdAction.setAction(actiontype);
              }
              if (actionDate != null && actionDate != "")
                holdAction.setActionDate(dao.convertGregorian(actionDate));
              diffpenltyAmt = new BigDecimal(hold_amt).subtract(holdAction.getRDVHoldAmount());
              if (hold_per != null && hold_per != "")
                holdAction.setRDVHoldPercentage(new BigDecimal(hold_per));
              else
                holdAction.setRDVHoldPercentage(new BigDecimal(0));
              // calculate weightage.
              // holdAction.setRDVHoldAmount(new BigDecimal(hold_amt));
              // lineMatch = holdAction.getEfinRdvtxnline().getMatchAmt()
              // .subtract(holdAction.getEfinRdvtxnline().getADVDeduct())
              // .subtract(holdAction.getEfinRdvtxnline().getPenaltyAmt());
              lineMatch = holdAction.getEfinRdvtxnline().getNetmatchAmt()
                  .add(holdAction.getAmount());

              weigtage = ((lineMatch.divide(totalMatch, 6, BigDecimal.ROUND_HALF_EVEN))
                  .multiply(new BigDecimal(hold_amt))).setScale(2, RoundingMode.HALF_UP);
              holdAction.setRDVHoldAmount(weigtage);

              holdAction.setActionReason(actionreason == null ? null : actionreason);
              holdAction.setActionJustification(actionjust == null ? null : actionjust);
              if (bpartnerId != null)
                holdAction
                    .setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, bpartnerId));
              else {
                holdAction.setBusinessPartner(null);
              }
              holdAction.setName(bpartnerId == null ? null : bpname);
              if (freezehold != null && freezehold.equals("Y")) {
                holdAction.setFreezeRdvHold(true);
              } else {
                holdAction.setFreezeRdvHold(false);
              }
              // objLine.setRDVHoldAccountType(hold_account == null ? null : hold_account);
              // if (uniquecode != null) {
              // objLine.setRDVHoldUniquecode(
              // OBDal.getInstance().get(AccountingCombination.class, uniquecode));
              // } else
              // objLine.setRDVHoldUniquecode(null);
              OBDal.getInstance().save(holdAction);
              OBDal.getInstance().flush();
              dao.insertHoldHeader(holdAction, holdAction.getEfinRdvtxnline(), diffpenltyAmt,
                  oldholdTypeId, oldAction);

            }
            // }
            // }
          }

          jsonResponse = new JSONObject();
          jsonResponse.put("result", "1");
          response.setCharacterEncoding("UTF-8");
          response.getWriter().write(jsonResponse.toString());

        } else if (operation.equals("del")) {
          if (!StringUtils.isEmpty(RDVTrxlineId) && !RDVTrxlineId.equals("null")) {
            String holdactId = request.getParameter("id");
            EfinRdvHoldAction holdaction = OBDal.getInstance().get(EfinRdvHoldAction.class,
                holdactId);
            dao.deleteHoldHed(holdaction);
          } else {
            // String holdactId = request.getParameter("id");
            EfinRDVTransaction txn = OBDal.getInstance().get(EfinRDVTransaction.class, RDVTxnId);
            for (EfinRDVTxnline line : txn.getEfinRDVTxnlineList()) {
              for (EfinRdvHoldAction holdAction : line.getEfinRdvHoldActionList()) {
                if (holdAction.isTxn() && holdAction.getEfinRdvHoldTypes().getId().equals(hold_type)
                    && holdAction.getAction().equals(holdActionType)) {
                  dao.deleteHoldHed(holdAction);
                }
              }
            }
          }

          jsonResponse = new JSONObject();
          jsonResponse.put("isExists", "1");
          response.setCharacterEncoding("UTF-8");
          response.getWriter().write(jsonResponse.toString());
        }
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
          if (com.getEfinUniquecodename() != null)
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
          EfinRdvHoldTypes holdtype = OBDal.getInstance().get(EfinRdvHoldTypes.class,
              request.getParameter("inpholdTypeId"));
          sb.append("<GetHold>");
          if (holdtype.getThreshold() != null) {
            sb.append("<value>" + holdtype.getThreshold() + "</value>");
            sb.append("<valuepresent>" + 1 + "</valuepresent>");
          }
          sb.append("<type>" + holdtype.getDeductionType().getPenaltyLogic() + "</type>");

          sb.append("</GetHold>");
        } catch (final Exception e) {
          log4j.error("Exception in RDVHoldActionAjax - getThershold : ", e);
        } finally {
          response.setContentType("text/xml");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(sb.toString());
        }
      } else if (action.equals("chkHoldtypeGoingdownNegativeval")) {
        EfinRdvHoldTypes holdtype = null;
        String isExist = "N";
        try {

          String holdAmt = request.getParameter("holdAmt");
          log4j.debug("holdAmt" + holdAmt);
          String holdType = request.getParameter("holdType");
          log4j.debug("holdType" + holdType);
          if (!StringUtils.isEmpty(request.getParameter("inpRDVTxnLineId"))
              && !request.getParameter("inpRDVTxnLineId").equals("null")) {
            String RDVTxnLineId = request.getParameter("inpRDVTxnLineId");
            log4j.debug("RDVTxnLineId" + RDVTxnLineId);

            EfinRDVTxnline line = OBDal.getInstance().get(EfinRDVTxnline.class, RDVTxnLineId);

            String id = request.getParameter("id");
            if (id.contains("jqg"))
              id = null;

            if (request.getParameter("type").equals("del")) {
              EfinRdvHoldAction penaction = OBDal.getInstance().get(EfinRdvHoldAction.class, id);
              holdtype = penaction.getEfinRdvHoldTypes();
              holdAmt = penaction.getRDVHoldAmount().toString();

            } else
              holdtype = OBDal.getInstance().get(EfinRdvHoldTypes.class, holdType);

            isExist = RdvHoldActionDAOimpl.chkHoldtypeGoingdownNegativeval(line,
                new BigDecimal(holdAmt), holdtype, id, request.getParameter("type"));
          } else {
            String RDVTxnId = request.getParameter("inpRDVTxnId");
            String id = request.getParameter("id");
            BigDecimal totalHoldAmt = BigDecimal.ZERO;
            if (RDVTxnId != null) {

              if (request.getParameter("type").equals("del")) {
                List<EfinRdvHoldAction> holdActionList = null;
                OBQuery<EfinRdvHoldAction> holdList = OBDal.getInstance()
                    .createQuery(EfinRdvHoldAction.class, "e where e.txngroupref=:refId");
                holdList.setNamedParameter("refId", id);
                holdActionList = holdList.list();
                for (EfinRdvHoldAction hldAction : holdActionList) {
                  totalHoldAmt = totalHoldAmt.add(hldAction.getRDVHoldAmount());
                  holdtype = hldAction.getEfinRdvHoldTypes();
                }
                holdAmt = totalHoldAmt.toString();
              } else
                holdtype = OBDal.getInstance().get(EfinRdvHoldTypes.class, holdType);

              EfinRDVTransaction txn = OBDal.getInstance().get(EfinRDVTransaction.class, RDVTxnId);
              isExist = RdvHoldActionDAOimpl.chkHoldtypeGoingdownNegativevalTxn(txn,
                  new BigDecimal(holdAmt), holdtype, id, request.getParameter("type"));
            }
          }
          log4j.debug("isExist:" + isExist);
          jsonResponse = new JSONObject();
          jsonResponse.put("isExists", isExist);
          response.setCharacterEncoding("UTF-8");
          response.getWriter().write(jsonResponse.toString());

        } catch (final Exception e) {
          log4j.error("Exception in RdvHoldAction - chkHoldtypeGoingdownNegativeval : ", e);
        }
      } else if (action.equals("checkHoldTypeAlreadExistsInTxn")) {
        EfinRdvHoldTypes holdtype = null;
        String isExist = "N";
        try {

          String holdTypeId = request.getParameter("holdType");
          holdtype = OBDal.getInstance().get(EfinRdvHoldTypes.class, holdTypeId);
          log4j.debug("holdType" + holdtype);
          if (!StringUtils.isEmpty(request.getParameter("inpRDVTxnId"))
              && !request.getParameter("inpRDVTxnId").equals("null")) {
            EfinRDVTransaction txn = OBDal.getInstance().get(EfinRDVTransaction.class,
                request.getParameter("inpRDVTxnId").toString());
            String RDVTxnId = request.getParameter("inpRDVTxnId");
            String id = request.getParameter("id");
            if (id.contains("jqg"))
              id = null;
            if (RDVTxnId != null) {
              isExist = RdvHoldActionDAOimpl.checkHoldTypeAlreadExistsInTxn(txn, holdtype, id,
                  request.getParameter("actionType"));
            }
          }
          log4j.debug("isExist:" + isExist);
          jsonResponse = new JSONObject();
          jsonResponse.put("isExists", isExist);
          response.setCharacterEncoding("UTF-8");
          response.getWriter().write(jsonResponse.toString());

        } catch (final Exception e) {
          log4j.error("Exception in RdvHoldAction - chkHoldtypeGoingdownNegativeval : ", e);
        }
      }

      else if (action.equals("getbusinesspartner")) {
        JSONObject jsob = new JSONObject();
        jsob = RdvHoldActionDAOimpl.getBusinessPartnerList(vars.getClient(),
            request.getParameter("searchTerm"), Integer.parseInt(request.getParameter("pageLimit")),
            Integer.parseInt(request.getParameter("page")), vars.getRole(), vars.getOrg());
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
          String isExist = "N";
          if (request.getParameter("id").contains("jqg")) {
            actionId = null;
          }
          if (!StringUtils.isEmpty(request.getParameter("inpRDVTxnLineId"))
              && !request.getParameter("inpRDVTxnLineId").equals("null")) {
            isExist = RdvHoldActionDAOimpl.getTotalMatchAmt(request.getParameter("inpRDVTxnLineId"),
                new BigDecimal(request.getParameter("holdAmt").toString().replaceAll(",", "")),
                actionId);
          } else {
            isExist = RdvHoldActionDAOimpl.getTotalMatchAmtTxn(request.getParameter("inpRDVTxnId"),
                new BigDecimal(request.getParameter("holdAmt").toString()), actionId);
            // isExist = "N";
          }
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
          EfinRdvHoldTypes holdtype = OBDal.getInstance().get(EfinRdvHoldTypes.class,
              request.getParameter("inpholdTypeId"));
          jsonResponse = new JSONObject();
          jsonResponse.put("value", holdtype.getThreshold());
          jsonResponse.put("valuepresent", 1);
          jsonResponse.put("type", holdtype.getDeductionType().getPenaltyLogic());// need to change
          response.setCharacterEncoding("UTF-8");
          response.getWriter().write(jsonResponse.toString());

        } catch (final Exception e) {
          log4j.error("Exception in PayrollProcess - getTotalMatchAmt : ", e);
        } finally {
        }
      } else if (action.equals("getHoldTypeBaseAction")) {
        try {
          List<RdvHoldActionVO> holdlist = dao.getHoldType(vars.getClient(),
              request.getParameter("inpaction"), vars.getLanguage());
          jsonArray = new JSONArray();
          for (RdvHoldActionVO vo : holdlist) {
            jsonResponse = new JSONObject();
            jsonResponse.put("Threshold", vo.getThershold());
            if (vo.getThershold() != null)
              jsonResponse.put("Valuepresent", 1);
            else
              jsonResponse.put("Valuepresent", 0);
            jsonResponse.put("ID", vo.getHoldId());
            jsonResponse.put("Name", vo.getHoldname());
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
      log4j.error("Error in HoldActionAjax : ", e);
    } finally {
      OBContext.restorePreviousMode();
      try {
        con.close();
      } catch (final SQLException e) {
        log4j.error("Error in HoldActionAjax : ", e);
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