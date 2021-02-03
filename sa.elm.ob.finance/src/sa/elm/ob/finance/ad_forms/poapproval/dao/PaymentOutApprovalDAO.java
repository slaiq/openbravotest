package sa.elm.ob.finance.ad_forms.poapproval.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.application.Note;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FIN_Payment_Credit;
import org.openbravo.service.db.DalConnectionProvider;

import sa.elm.ob.finance.EfinPoApproval;
import sa.elm.ob.finance.ad_forms.poapproval.vo.PaymentOutApprovalVO;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.ad_forms.delegation.dao.ApprovalDelegationDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

public class PaymentOutApprovalDAO {
  private Connection conn = null;
  private static Logger log4j = Logger.getLogger(PaymentOutApprovalDAO.class);
  private static HashMap<String, String> priorityMap = null;
  ApprovalDelegationDAO delegateDAO = null;

  public PaymentOutApprovalDAO(Connection con) {
    this.conn = con;
    delegateDAO = new ApprovalDelegationDAO(conn);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public JSONObject getPaymentOutList(VariablesSecureApp vars, String clientId, String orgId,
      String roleId, String userId, JSONObject searchAttr, PaymentOutApprovalVO searchVO,
      boolean getCount) {
    JSONObject result = new JSONObject(), json = null;
    JSONArray jsonArray = new JSONArray();
    SQLQuery query = null;
    try {
      OBContext.setAdminMode();

      result.put("page", "0");
      result.put("total", "0");
      result.put("records", "0");
      result.put("rows", jsonArray);

      StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(),
          fromQuery = new StringBuilder(), whereClause = new StringBuilder(),
          orderClause = new StringBuilder();

      countQuery.append("select count(distinct p.fin_payment_id) as totalRecord ");

      selectQuery.append(
          "select distinct p.fin_payment_id, p.documentno, p.paymentdate, p.amount, p.referenceno, bp.name as payto, acct.fin_financial_account_id, acct.name as payfrom, cast(cur.iso_code as text) as iso, ");
      selectQuery.append(
          "usr.name as requester, nxtr.eut_next_role_id, nxtrln.ad_role_id, coalesce(rd.created, p.created) as requestdate, p.ad_org_id, org.name as orgname ");
      // selectQuery.append("coalesce(p.em_qpoe_priority, 'M') = 'H', coalesce(p.em_qpoe_priority,
      // 'M') = 'M', coalesce(p.em_qpoe_priority, 'M') = 'L' ");

      fromQuery.append(
          "from fin_payment p join fin_financial_account acct on acct.fin_financial_account_id = p.fin_financial_account_id ");
      fromQuery.append(
          "left join c_bpartner bp on bp.c_bpartner_id = p.c_bpartner_id join c_currency cur on cur.c_currency_id = p.c_currency_id ");
      fromQuery.append("left join ad_org org on org.ad_org_id=p.ad_org_id ");
      fromQuery.append(
          "left join (select tb1.createdby, tb1.fin_payment_id, tb1.created from efin_po_approval tb1 join (select fin_payment_id, max(created) as created from efin_po_approval where status ilike 'submitted' group by fin_payment_id ");
      fromQuery.append(
          "order by created desc, fin_payment_id) tb2 on tb1.fin_payment_id = tb2.fin_payment_id and tb1.created = tb2.created)rd on rd.fin_payment_id = p.fin_payment_id left join ad_user usr on rd.createdby = usr.ad_user_id  ");
      fromQuery.append(
          "join eut_next_role nxtr on nxtr.eut_next_role_id = p.em_eut_next_role_id join eut_next_role_line nxtrln on nxtrln.eut_next_role_id = nxtr.eut_next_role_id and ");
      fromQuery.append("(nxtrln.ad_role_id = '").append(roleId).append(
          "' or nxtrln.ad_role_id in (select edd.ad_role_id from eut_docapp_delegate edd join eut_docapp_delegateln eddl ");
      fromQuery.append(
          "on edd.eut_docapp_delegate_id = eddl.eut_docapp_delegate_id where eddl.ad_user_id = '")
          .append(userId).append("' ");
      fromQuery.append("and eddl.document_type = '").append(Resource.PAYMENT_OUT_RULE).append(
          "' and now() between edd.from_date and coalesce(edd.to_date, to_date('31-12-9999 23:59:59','dd-MM-yyyy HH24:mi:ss')))) ");

      whereClause.append("where p.status = 'EFIN_WFA' and p.em_eut_next_role_id is not null ");
      whereClause.append("and p.ad_client_id= '").append(clientId).append("' ");
      whereClause.append("and p.ad_org_id in (").append(Utility.getAccessibleOrg(vars))
          .append(") and p.isactive = 'Y' ");
      if (searchAttr != null && searchAttr.has("search")
          && searchAttr.getString("search").equals("true")) {
        if (!StringUtils.isEmpty(searchVO.getOrgName()))
          whereClause.append("and org.name ilike '%").append(searchVO.getOrgName()).append("%' ");
        if (!StringUtils.isEmpty(searchVO.getDocumentNo()))
          whereClause.append("and p.documentno ilike '%").append(searchVO.getDocumentNo())
              .append("%' ");
        if (!StringUtils.isEmpty(searchVO.getPaymentDate()))
          whereClause.append("and to_date(to_char(p.paymentdate, 'dd-MM-yyyy'), 'dd-MM-yyyy')")
              .append(searchVO.getPaymentDate().split("##")[0]).append(" to_date('")
              .append(searchVO.getPaymentDate().split("##")[1]).append("','dd-MM-yyyy') ");
        if (!StringUtils.isEmpty(searchVO.getReferenceNo()))
          whereClause.append("and p.referenceno ilike '%").append(searchVO.getReferenceNo())
              .append("%' ");
        if (!StringUtils.isEmpty(searchVO.getbPartnerName()))
          whereClause.append("and bp.name ilike '%").append(searchVO.getbPartnerName())
              .append("%' ");
        if (!StringUtils.isEmpty(searchVO.getFinAcctName()))
          whereClause.append("and acct.name ilike '%").append(searchVO.getFinAcctName())
              .append("%' ");
        if (!StringUtils.isEmpty(searchVO.getPaymentAmount())
            && Double.parseDouble(searchVO.getPaymentAmount()) > 0)
          whereClause.append("and p.amount >= ").append(searchVO.getPaymentAmount()).append(" ");
        /*
         * if(!StringUtils.isEmpty(searchVO.getPriority()))
         * whereClause.append("and coalesce(p.em_qpoe_priority, 'M') = '").append(searchVO.
         * getPriority()).append("' ");
         */
        if (!StringUtils.isEmpty(searchVO.getRequester()))
          whereClause.append("and usr.name ilike '%").append(searchVO.getRequester()).append("%' ");
        if (!StringUtils.isEmpty(searchVO.getRequesterDate()))
          whereClause.append(
              "and to_date(to_char(coalesce(rd.created, p.created), 'dd-MM-yyyy'), 'dd-MM-yyyy')")
              .append(searchVO.getRequesterDate().split("##")[0]).append(" to_date('")
              .append(searchVO.getRequesterDate().split("##")[1]).append("','dd-MM-yyyy') ");
      }

      if (searchAttr != null && searchAttr.has("sortName")
          && !StringUtils.isEmpty(searchAttr.getString("sortName"))) {
        /*
         * if(StringUtils.equals(searchAttr.getString("sortName"), "priority")) {
         * orderClause.append("order by coalesce(p.em_qpoe_priority, 'M') = 'H' ").append(searchAttr
         * .getString("sortType")).append(", coalesce(p.em_qpoe_priority, 'M') = 'M' ").append(
         * searchAttr.getString("sortType")).append(", coalesce(p.em_qpoe_priority, 'M') = 'L' ")
         * .append(searchAttr.getString("sortType"));
         * orderClause.append(", ").append("documentno desc"); } else
         */
        orderClause.append("order by ").append(searchAttr.getString("sortName")).append(" ")
            .append(searchAttr.getString("sortType"));
      }

      int offset = 0, totalPage = 0, totalRecord = 0;
      int rows = Integer.parseInt(searchAttr.getString("rows")),
          page = Integer.parseInt(searchAttr.getString("page"));
      query = OBDal.getInstance().getSession()
          .createSQLQuery(countQuery.toString() + fromQuery.toString() + whereClause.toString());
      List<Object> list = query.list();
      if (query != null && list.size() > 0) {
        for (Iterator iterator = list.iterator(); iterator.hasNext();) {
          totalRecord = Integer.parseInt(iterator.next().toString());
        }
      }

      if (getCount) {
        result.put("page", page);
        result.put("total", totalPage);
        result.put("records", totalRecord);
        return result;
      }

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
        offset = 0;
      }

      result.put("page", page);
      result.put("total", totalPage);
      result.put("records", totalRecord);

      searchAttr.put("limit", rows);
      searchAttr.put("offset", offset);

      if (priorityMap == null) {
        priorityMap = new HashMap<String, String>();
        priorityMap.put("H", "High");
        priorityMap.put("M", "Medium");
        priorityMap.put("L", "Low");
      }

      query = OBDal.getInstance().getSession()
          .createSQLQuery(selectQuery.toString() + fromQuery.toString() + whereClause.toString()
              + orderClause.toString() + " limit " + rows + " offset " + offset);
      list = query.list();
      if (query != null && list.size() > 0) {
        for (Iterator iterator = list.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();

          json = new JSONObject();
          json.put("id", row[0].toString());
          json.put("multiselect", "1");
          json.put("documentno", row[1].toString().trim());
          String gregdate = row[2].toString().substring(0, 19);
          // log4j.debug("datehijri"+UtilityDAO.convertToHijriDate(gregdate));
          // log4j.debug("formatteddate"+Utility.formatDate(UtilityDAO.convertToHijriDate(gregdate)));
          json.put("paymentdate", Utility.formatDate(UtilityDAO.convertToHijriDate(gregdate)));
          json.put("amount",
              (Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, row[3].toString())
                  + "" + (Double.parseDouble(row[3].toString()) == 0 ? ""
                      : " " + row[8].toString().trim())));
          json.put("amt", row[3].toString().trim());
          json.put("referenceno", Utility.nullToEmpty(row[4]));
          json.put("payto", Utility.nullToEmpty(row[5]));
          json.put("fin_financial_account_id", row[6].toString());
          json.put("payfrom", Utility.nullToEmpty(row[7]));
          json.put("currency", row[8].toString());
          json.put("requester", Utility.nullToEmpty(row[9]));
          json.put("quNextRoleId", Utility.nullToEmpty(row[10]));
          // json.put("priority", priorityMap.get(row[12].toString()));
          json.put("requestdate", row[12] == null ? ""
              : Utility.formatDate(row[13].toString().substring(0, 19), Utility.dateTimeFormat));
          json.put("ad_org_id", row[13].toString());
          json.put("orgname", row[14].toString().trim());
          jsonArray.put(json);
        }
      }
      result.put("rows", jsonArray);
    } catch (final Exception e) {
      log4j.error("Exception in getPaymentOutList :", e);
      return result;
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  @SuppressWarnings("rawtypes")
  public PaymentOutApprovalVO getPaymentOutDetails(String finPaymentId) {
    PaymentOutApprovalVO headerVO = new PaymentOutApprovalVO();
    SQLQuery query = null;
    try {
      OBContext.setAdminMode();

      StringBuilder sqlBuilder = new StringBuilder();
      sqlBuilder.append(
          "select p.amount, p.description,p.em_efin_notes, p.generated_credit, up.used as usedcredit,  cast(cur.iso_code as text) as iso  , paymet.name as paymentmethod from fin_payment p left join c_currency cur on cur.c_currency_id = p.c_currency_id "
              + "left join (select coalesce(sum(amount), 0) as used,fin_payment_id from fin_payment_credit group by fin_payment_id) up on up.fin_payment_id = p.fin_payment_id left join fin_paymentmethod paymet on paymet.fin_paymentmethod_id=p.fin_paymentmethod_id  "
              + "where p.fin_payment_id = '" + finPaymentId + "'");
      query = OBDal.getInstance().getSession().createSQLQuery(sqlBuilder.toString());
      if (query != null && query.list().size() > 0) {
        for (Iterator iterator = query.list().iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          headerVO.setPaymentAmount(Utility.nullToZero(row[0]));
          headerVO.setDescription(Utility.nullToEmpty(row[1]));
          headerVO.setNote(Utility.nullToEmpty(row[2]));
          headerVO.setExpectedAmount(Utility.nullToZero(row[3]));
          headerVO.setUsedCredit(Utility.nullToZero(row[4]));
          // headerVO.setPriority(row[5].toString());
          headerVO.setCurSymbol(row[5].toString());
          headerVO.setPaymentMethodName(row[6].toString());
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in getPaymentOutDetails :", e);
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
    return headerVO;
  }

  @SuppressWarnings("rawtypes")
  public JSONArray getMultiPaymentOutDetails(String finPaymentIdList) {
    JSONArray jsonArray = new JSONArray();
    SQLQuery query = null;
    try {
      OBContext.setAdminMode();

      StringBuilder sqlBuilder = new StringBuilder();
      sqlBuilder.append(
          "select p.fin_payment_id, p.amount, cast(cur.iso_code as text) as currency, p.description from fin_payment p left join c_currency cur on cur.c_currency_id = p.c_currency_id where p.fin_payment_id in ("
              + finPaymentIdList + ")");
      query = OBDal.getInstance().getSession().createSQLQuery(sqlBuilder.toString());
      if (query != null && query.list().size() > 0) {
        for (Iterator iterator = query.list().iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          JSONObject json = new JSONObject();
          json.put("id", row[0].toString());
          json.put("amount", Utility.nullToZero(row[1]));
          json.put("currency", Utility.nullToEmpty(row[2]));
          json.put("desc", Utility.nullToEmpty(row[3]));
          jsonArray.put(json);
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in getMultiPaymentOutDetails :", e);
      return jsonArray;
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonArray;
  }

  @SuppressWarnings("rawtypes")
  public List<PaymentOutApprovalVO> getPOLines(String strFinPaymentId) {
    List<PaymentOutApprovalVO> paymentOutLinesList = new ArrayList<PaymentOutApprovalVO>();
    SQLQuery query = null;
    PaymentOutApprovalVO vos = null;
    try {
      StringBuilder queryBuilder = new StringBuilder();
      queryBuilder.append(
          "select max(fv.orderno) as orderno, fv.invoiceno, ci.poreference, max(ci.dateinvoiced) as dateinvoiced, ");
      queryBuilder.append(
          "fv.duedate, max(fv.invoicedamt) as invoicedamt, max(fv.expected) as expected, sum(fv.paidamt) as paidamt, max(coalesce(fv.glitemname,'')) as glitem ");
      queryBuilder.append(
          "from fin_payment_detail_v fv left join fin_payment_schedule fpsi on fv.fin_payment_sched_inv_id = fpsi.fin_payment_schedule_id left join c_invoice ci on ci.c_invoice_id = fpsi.c_invoice_id ");
      queryBuilder.append("where fv.fin_payment_id = '").append(strFinPaymentId).append(
          "' group by ci.poreference, fv.invoiceno, fv.duedate, fv.glitemname order by fv.invoiceno, ci.poreference, fv.glitemname;");

      OBContext.setAdminMode();
      query = OBDal.getInstance().getSession().createSQLQuery(queryBuilder.toString());

      if (query != null && query.list().size() > 0) {
        for (Iterator iterator = query.list().iterator(); iterator.hasNext();) {
          Object[] rows = (Object[]) iterator.next();
          vos = new PaymentOutApprovalVO();
          vos.setOrderNo(Utility.nullToEmpty(rows[0]));
          vos.setInvoiceNo(Utility.nullToEmpty(rows[1]));
          vos.setReferenceNo(Utility.nullToEmpty(rows[2]));
          vos.setPaymentDate(rows[3] == null ? "" : Utility.nullToEmpty(rows[3]));
          vos.setDueDate(rows[4] == null ? "" : Utility.nullToEmpty(rows[4]));
          vos.setInvoiceAmount(Utility.nullToZero(rows[5]));
          vos.setExpectedAmount(Utility.nullToZero(rows[6]));
          vos.setPaidAmount(Utility.nullToZero(rows[7]));
          vos.setGlItem(Utility.nullToEmpty(rows[8]));
          paymentOutLinesList.add(vos);
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in getPOLines :", e);
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
    return paymentOutLinesList;
  }

  @SuppressWarnings("rawtypes")
  public List<PaymentOutApprovalVO> getApprovalHistory(String strFinPaymentId) {
    List<PaymentOutApprovalVO> list = new ArrayList<PaymentOutApprovalVO>();
    SQLQuery query = null;
    StringBuilder queryBuilder = null;
    try {
      queryBuilder = new StringBuilder();
      queryBuilder.append(
          "select efin_po_approval_id, poapp.ad_user_id, usr.name as approvername, poapp.ad_role_id, rol.name as approverrole, ");
      queryBuilder.append(
          "note.obuiapp_note_id, note.note as note, approveddate as approveddate, status from efin_po_approval poapp ");
      queryBuilder.append(
          "left join ad_user usr on usr.ad_user_id = poapp.ad_user_id left join ad_role rol on rol.ad_role_id = poapp.ad_role_id ");
      queryBuilder.append(
          "left join obuiapp_note note on note.obuiapp_note_id = poapp.obuiapp_note_id where fin_payment_id ='")
          .append(strFinPaymentId).append("' order by poapp.created;");
      query = OBDal.getInstance().getSession().createSQLQuery(queryBuilder.toString());
      if (query != null && query.list().size() > 0) {
        for (Iterator iterator = query.list().iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          PaymentOutApprovalVO vos = new PaymentOutApprovalVO();
          vos.setApprovalId(row[0].toString());
          vos.setUserId(row[1].toString());
          vos.setApproverName(row[2].toString());
          vos.setRoleId(row[3].toString());
          vos.setApproverRole(row[4].toString());
          vos.setComments(Utility.nullToEmpty(row[6]));
          vos.setApprovedDate(row[7].toString());
          vos.setStatus(row[8].toString());
          list.add(vos);
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in getPOrderLines :", e);
      return null;
    }
    return list;
  }

  public <T extends BaseOBObject> T getTableObject(Class<T> t, String strId) {
    return OBDal.getInstance().get(t, strId);
  }

  public OBError approveWaitingPayments(VariablesSecureApp vars, Connection con,
      String strFinPaymentId, String strNextRoleId, String strComments, String inpOrgId) {
    FIN_Payment payment = null;
    EutNextRole nextRole = null;
    org.openbravo.model.ad.domain.List documentAction = null;
    User user = null;
    ConnectionProvider connectionProvider = null;

    try {
      OBContext.setAdminMode();

      connectionProvider = new DalConnectionProvider(true);

      OBError returnResponseObject = null;

      user = getTableObject(User.class, vars.getUser());

      if (strNextRoleId != null)
        nextRole = getTableObject(EutNextRole.class, strNextRoleId);
      payment = getTableObject(FIN_Payment.class, strFinPaymentId);

      if (nextRole != null) {
        returnResponseObject = new OBError();
        returnResponseObject.setType("Success");
        returnResponseObject.setTitle(org.openbravo.erpCommon.utility.Utility.messageBD(
            connectionProvider, "Success", OBContext.getOBContext().getLanguage().getLanguage()));

        payment.setEutNextRole(nextRole);
        payment.setUpdatedBy(user);

        insertApprovalHistory(vars, "APP", payment, strComments, inpOrgId);

        returnResponseObject.setMessage(OBMessageUtils.messageBD("EFIN_messagepay"));
      } else {
        if (payment.getEfinDocumentAction() != null || payment.getEfinDocumentAction() != "")
          documentAction = getTableObject(org.openbravo.model.ad.domain.List.class,
              payment.getEfinDocumentAction());

        returnResponseObject = FIN_AddPayment.processPayment(vars, connectionProvider,
            documentAction.getSearchKey().equals("PRP")
                || documentAction.getSearchKey().equals("PPP") ? "P" : "D",
            payment);
        returnResponseObject.setMessage(OBMessageUtils.messageBD("EFIN_PayProcessed"));

        insertApprovalHistory(vars, "APP", payment, strComments, inpOrgId);
      }
      OBDal.getInstance().save(payment);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

      return returnResponseObject;

    } catch (Exception e) {
      log4j.error("Exception while approving payment : ", e);

      final OBError errorsObject = new OBError();
      errorsObject.setType("Error");
      errorsObject.setTitle("Error");
      errorsObject.setMessage(e.getMessage());

      return errorsObject;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public OBError reworkPayment(VariablesSecureApp vars, String strFinPaymentId, String strComments,
      String inpOrgId) {
    ConnectionProvider connectionProvider = null;
    FIN_Payment payment = null;
    User user = null;

    try {
      Set<String> invoiceDocNos = new HashSet<String>();
      OBError reworkResponse = new OBError();
      connectionProvider = new DalConnectionProvider(true);

      reworkResponse.setType("Success");
      reworkResponse.setTitle(org.openbravo.erpCommon.utility.Utility.messageBD(connectionProvider,
          "Success", OBContext.getOBContext().getLanguage().getLanguage()));

      OBContext.setAdminMode();

      payment = getTableObject(FIN_Payment.class, strFinPaymentId);
      user = getTableObject(User.class, vars.getUser());

      if (payment != null) {
        for (final FIN_PaymentDetail paymentDetail : payment.getFINPaymentDetailList()) {
          for (final FIN_PaymentScheduleDetail paymentScheduleDetail : paymentDetail
              .getFINPaymentScheduleDetailList()) {
            if (paymentScheduleDetail.getInvoicePaymentSchedule() != null) {
              // Related to invoices
              for (final FIN_PaymentScheduleDetail invScheDetail : paymentScheduleDetail
                  .getInvoicePaymentSchedule()
                  .getFINPaymentScheduleDetailInvoicePaymentScheduleList()) {
                invoiceDocNos
                    .add(invScheDetail.getInvoicePaymentSchedule().getInvoice().getDocumentNo());
              }
            }
          }
        }

        undoUsedCredit(payment, vars, invoiceDocNos);
        payment.setUpdatedBy(user);
        payment.setStatus("RPAP");
        payment.setEutNextRole(null);

        insertApprovalHistory(vars, "RW", payment, strComments, inpOrgId);

        OBDal.getInstance().save(payment);
        OBDal.getInstance().flush();
        OBDal.getInstance().commitAndClose();

        reworkResponse.setMessage(OBMessageUtils.messageBD("EFIN_PaysentforRework"));

      }

      return reworkResponse;
    } catch (Exception e) {
      log4j.error("Exception while rework :  ", e);

      final OBError errorsObject = new OBError();
      errorsObject.setType("Error");
      errorsObject.setTitle("Error");
      errorsObject.setMessage(e.getMessage());

      return errorsObject;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public JSONObject approveMultiPayment(VariablesSecureApp vars, JSONArray paymentList,
      String strComments) {
    ConnectionProvider connectionProvider = null;
    JSONObject resultObject = new JSONObject();
    org.openbravo.model.ad.domain.List documentAction = null;
    try {
      resultObject.put("result", "0");
      resultObject.put("resultMsg", "Exception while approving payment");
      resultObject.put("resultDesc", "");

      boolean allProcessed = true;

      OBContext.setAdminMode();
      User user = getTableObject(User.class, vars.getUser());

      for (int i = 0; i < paymentList.length(); i++) {
        JSONObject json = paymentList.getJSONObject(i);
        try {
          // Getting Next Role
          NextRoleByRuleVO roleVO = null;
          EutNextRole nextRole = null;
          FIN_Payment payment = null;
          connectionProvider = new DalConnectionProvider(true);
          String orgId = getOrgId(json.getString("id"));

          if (StringUtils.length(json.getString("quNextRoleId")) == 32)
            roleVO = NextRoleByRule.getNextRole(connectionProvider.getConnection(),
                vars.getClient(), vars.getOrg(), vars.getRole(), vars.getUser(),
                Resource.PAYMENT_OUT_RULE, json.getString("amount"));
          else
            roleVO = NextRoleByRule.getDelegatedNextRole(connectionProvider.getConnection(),
                vars.getClient(), vars.getOrg(), json.getString("fromUserRoleId"),
                json.getString("toUserRoleId"), vars.getUser(), Resource.PAYMENT_OUT_RULE,
                json.getString("amount"));

          if (isDirectApproval(json.getString("id"), vars.getRole()))
            roleVO = NextRoleByRule.getNextRole(connectionProvider.getConnection(),
                vars.getClient(), orgId, vars.getRole(), vars.getUser(), Resource.PAYMENT_OUT_RULE,
                json.getString("amount"));
          else {
            HashMap<String, String> roleId = NextRoleByRule.getDelegatedFromAndToRoles(
                connectionProvider.getConnection(), vars.getClient(), orgId, vars.getUser(),
                Resource.PAYMENT_OUT_RULE, json.getString("quNextRoleId"));
            String delegatedFromRole = roleId.get("FromUserRoleId");
            String delegatedToRole = roleId.get("ToUserRoleId");
            if (delegatedFromRole != null && delegatedToRole != null)
              roleVO = NextRoleByRule.getDelegatedNextRole(connectionProvider.getConnection(),
                  vars.getClient(), orgId, delegatedFromRole, delegatedToRole, vars.getUser(),
                  Resource.PAYMENT_OUT_RULE, json.getString("amount"));
          }

          if (roleVO.hasApproval()) {
            nextRole = getTableObject(EutNextRole.class, roleVO.getNextRoleId());
          } else {
            nextRole = null;
          }

          payment = getTableObject(FIN_Payment.class, json.getString("id"));
          boolean updatePayment = true;

          if (nextRole != null) {
            payment.setEutNextRole(nextRole);
            payment.setUpdatedBy(user);
            insertApprovalHistory(vars, "APP", payment, strComments, json.getString("orgId"));

            json.put("result", "1");
            json.put("resultMsg", "Payment approved and submitted for next approval");
            json.put("resultCoreMsg", "Payment approved and submitted for next approval");
          } else {
            if (payment.getEfinDocumentAction() != null || payment.getEfinDocumentAction() != "")
              documentAction = getTableObject(org.openbravo.model.ad.domain.List.class,
                  payment.getEfinDocumentAction());

            OBError returnVal = FIN_AddPayment.processPayment(vars, connectionProvider,
                documentAction.getSearchKey().equals("PRP")
                    || documentAction.getSearchKey().equals("PPP") ? "P" : "D",
                payment);
            if (returnVal.getType().equalsIgnoreCase("error")) {
              updatePayment = false;
              json.put("result", "0");
              json.put("resultMsg", "Exception while approving Payment");
              json.put("resultCoreMsg", returnVal.getMessage());
              allProcessed = false;
            } else {
              payment.setEutNextRole(null);
              payment.setUpdatedBy(user);
              insertApprovalHistory(vars, "APP", payment, strComments, json.getString("orgId"));

              json.put("result", "1");
              json.put("resultMsg", "Payment Processed");
              json.put("resultCoreMsg", "Payment Processed");
            }
          }
          if (updatePayment) {
            OBDal.getInstance().save(payment);
            OBDal.getInstance().flush();
            OBDal.getInstance().commitAndClose();
          }
        } catch (Exception e) {
          json.put("result", "0");
          json.put("resultMsg", "Exception while approving Payment");
          allProcessed = false;
        }
      }

      resultObject.put("result", "1");
      resultObject.put("resultMsg",
          Utility.getADMessage("Success", OBContext.getOBContext().getLanguage().getLanguage()));
      resultObject.put("resultDesc", "Payment Processed");
      resultObject.put("paymentList", paymentList);

      NextRoleByRule.deleteUnusedNextRoles(OBDal.getInstance().getConnection(true),
          Resource.PAYMENT_OUT_RULE);

      if (allProcessed == false) {
        resultObject.put("result", "2");
        resultObject.put("resultMsg", "Some Payment(s) has not been processed");
      }
      log4j.debug("approveMultiPayment resultObject:" + resultObject.toString());
      return resultObject;
    } catch (Exception e) {
      log4j.error("Exception in approveMultiPayment :  ", e);
      return resultObject;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public JSONObject reworkMultiPayment(VariablesSecureApp vars, JSONArray paymentList,
      String strComments) {
    FIN_Payment payment = null;
    User user = null;
    JSONObject resultObject = new JSONObject();
    try {
      Set<String> invoiceDocNos = new HashSet<String>();

      resultObject.put("result", "0");
      resultObject.put("resultMsg", "Exception while assigning rework for payment");
      resultObject.put("resultDesc", "");

      OBContext.setAdminMode();

      for (int i = 0; i < paymentList.length(); i++) {
        JSONObject json = paymentList.getJSONObject(i);
        try {
          payment = getTableObject(FIN_Payment.class, json.getString("id"));
          user = getTableObject(User.class, vars.getUser());

          if (payment != null) {
            for (final FIN_PaymentDetail paymentDetail : payment.getFINPaymentDetailList()) {
              for (final FIN_PaymentScheduleDetail paymentScheduleDetail : paymentDetail
                  .getFINPaymentScheduleDetailList()) {
                if (paymentScheduleDetail.getInvoicePaymentSchedule() != null) {
                  // Related to invoices
                  for (final FIN_PaymentScheduleDetail invScheDetail : paymentScheduleDetail
                      .getInvoicePaymentSchedule()
                      .getFINPaymentScheduleDetailInvoicePaymentScheduleList()) {
                    invoiceDocNos.add(
                        invScheDetail.getInvoicePaymentSchedule().getInvoice().getDocumentNo());
                  }
                }
              }
            }

            undoUsedCredit(payment, vars, invoiceDocNos);
            payment.setUpdatedBy(user);
            payment.setStatus("RPAP");
            payment.setEutNextRole(null);

            insertApprovalHistory(vars, "RW", payment, strComments, json.getString("orgId"));

            OBDal.getInstance().save(payment);
            OBDal.getInstance().flush();
            OBDal.getInstance().commitAndClose();
          }
          json.put("result", "1");
          json.put("resultMsg", "Payment sent for rework");
        } catch (Exception e) {
          json.put("result", "0");
          json.put("resultMsg", "Exception while assigning rework for Payment");

        }
      }

      resultObject.put("result", "1");
      resultObject.put("resultMsg",
          Utility.getADMessage("Success", OBContext.getOBContext().getLanguage().getLanguage()));
      resultObject.put("paymentList", paymentList);
      resultObject.put("resultDesc", "Payment sent for Rework");

      NextRoleByRule.deleteUnusedNextRoles(OBDal.getInstance().getConnection(true),
          Resource.PAYMENT_OUT_RULE);

      return resultObject;
    } catch (Exception e) {
      log4j.error("Exception in reworkMultiPayment :  ", e);
      return resultObject;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void insertApprovalHistory(VariablesSecureApp vars, String status, FIN_Payment payment,
      String strComments, String inpOrgId) {
    EfinPoApproval poApproval = null;
    Client client = null;
    Organization organization = null;
    User user = null;
    Role role = null;
    Note notes = null;
    Table table = null;
    try {
      OBContext.setAdminMode();

      client = getTableObject(Client.class, vars.getClient());
      organization = getTableObject(Organization.class, inpOrgId);
      user = getTableObject(User.class, vars.getUser());
      role = getTableObject(Role.class, vars.getRole());

      poApproval = OBProvider.getInstance().get(EfinPoApproval.class);

      poApproval.setClient(client);
      poApproval.setOrganization(organization);
      poApproval.setCreatedBy(user);
      poApproval.setUpdatedBy(user);
      poApproval.setAlertStatus(status.equals("APP") ? "APP" : "ASSREW");
      poApproval.setRole(role);
      poApproval.setUserContact(user);
      poApproval.setPayment(payment);
      poApproval.setApproveddate(new Date());
      OBDal.getInstance().save(poApproval);
      if (!strComments.isEmpty()) {
        table = getTableObject(Table.class, "D1A97202E832470285C9B1EB026D54E2");

        notes = OBProvider.getInstance().get(Note.class);

        notes.setClient(client);
        notes.setOrganization(organization);
        notes.setCreatedBy(user);
        notes.setUpdatedBy(user);
        notes.setRecord(poApproval.getId());
        notes.setNote(strComments);
        notes.setTable(table);
        OBDal.getInstance().save(notes);
      }
      poApproval.setObuiappNote(notes);

      OBDal.getInstance().save(poApproval);
      OBDal.getInstance().flush();

    } catch (Exception e) {
      log4j.error(" Exception while adding in approval ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void undoUsedCredit(FIN_Payment myPayment, VariablesSecureApp vars,
      Set<String> invoiceDocNos) {
    final List<FIN_Payment> payments = new ArrayList<FIN_Payment>();

    for (final FIN_Payment_Credit pc : myPayment.getFINPaymentCreditList()) {
      final FIN_Payment creditPaymentUsed = pc.getCreditPaymentUsed();
      creditPaymentUsed.setUsedCredit(creditPaymentUsed.getUsedCredit().subtract(pc.getAmount()));
      payments.add(creditPaymentUsed);
    }

    for (final FIN_Payment payment : payments) {
      // Update Description
      final String payDesc = payment.getDescription();

      if (payDesc != null) {
        final String invoiceDocNoMsg = org.openbravo.erpCommon.utility.Utility
            .messageBD(new DalConnectionProvider(), "APRM_CreditUsedinInvoice", vars.getLanguage());
        if (invoiceDocNoMsg != null) {
          final StringBuffer newDesc = new StringBuffer();
          for (final String line : payDesc.split("\n")) {
            boolean include = true;
            if (line.startsWith(invoiceDocNoMsg.substring(0, invoiceDocNoMsg.lastIndexOf("%s")))) {
              for (final String docNo : invoiceDocNos) {
                if (line.indexOf(docNo) > 0) {
                  include = false;
                  break;
                }
              }
            }
            if (include) {
              newDesc.append(line);
              if (!"".equals(line))
                newDesc.append("\n");
            }
          }
          // Truncate Description to keep length as 255
          payment.setDescription(
              newDesc.toString().length() > 255 ? newDesc.toString().substring(0, 255)
                  : newDesc.toString());
        }
      }
    }
    myPayment.getFINPaymentCreditList().clear();
  }

  public List<PaymentOutApprovalVO> getCommentsHistory(String paymentId) {
    PaymentOutApprovalVO VO = null;
    PreparedStatement st = null;
    ResultSet rs = null;
    List<PaymentOutApprovalVO> list = new ArrayList<PaymentOutApprovalVO>();

    try {
      String sql = " select app.ad_user_id, app.ad_role_id, to_char(app.approveddate,'dd-MM-yyyy') as approveddate, to_char(app.approveddate,'hh12:mi:ss AM') as approvedtime, status, usr.name as approvername, rol.name as approverrole, nte.note from efin_po_approval app "
          + " left join ad_user usr on usr.ad_user_id=app.ad_user_id "
          + " left join ad_role rol on rol.ad_role_id=app.ad_role_id "
          + " left join obuiapp_note nte on nte.obuiapp_note_id=app.obuiapp_note_id "
          + " where fin_payment_id=? order by app.created asc ";

      st = conn.prepareStatement(sql);
      st.setString(1, paymentId);
      rs = st.executeQuery();
      while (rs.next()) {
        String desc = rs.getString("note") == null ? "" : rs.getString("note");
        if (desc != null) {
          // if(!desc.equals("")) {
          VO = new PaymentOutApprovalVO();
          if (rs.getString("status").equals("SUB")) {
            VO.setSubmittedBy("Submitted on <br>" + rs.getString("approveddate") + " at "
                + rs.getString("approvedtime") + "<br> By: " + rs.getString("approvername") + " ("
                + rs.getString("approverrole") + ")");
          } else if (rs.getString("status").equals("APP")) {
            VO.setSubmittedBy("Approved on <br>" + rs.getString("approveddate") + " at "
                + rs.getString("approvedtime") + "<br> By: " + rs.getString("approvername") + " ("
                + rs.getString("approverrole") + ")");
          } else if (rs.getString("status").equals("ASSREW"))
            VO.setSubmittedBy("Rework Assigned on <br>" + rs.getString("approveddate") + " at "
                + rs.getString("approvedtime") + "<br> By: " + rs.getString("approvername") + " ("
                + rs.getString("approverrole") + ")");
          VO.setNote(desc);
          list.add(VO);
          // }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getApprovalHistory :", e);
    }
    return list;
  }

  public boolean isDirectApproval(String headerId, String roleId) {
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    try {
      query = "select count(*) from fin_payment req join eut_next_role enr on enr.eut_next_role_id = req.em_eut_next_role_id "
          + " join eut_next_role_line enrl on enr.eut_next_role_id = enrl.eut_next_role_id "
          + " and req.fin_payment_id='" + headerId + "' and enrl.ad_role_id ='" + roleId + "'";

      ps = conn.prepareStatement(query);
      log4j.debug(ps.toString());
      rs = ps.executeQuery();
      if (rs.next()) {
        if (rs.getInt("count") > 0)
          return true;
        else
          return false;
      } else
        return false;
    } catch (Exception e) {
      log4j.error("Exception in isDirectApproval " + e.getMessage());
      return false;
    }
  }

  public String getOrgId(String headerId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String ls = "";
    try {
      st = conn.prepareStatement("select ad_org_id from fin_payment where fin_payment_id=?");
      st.setString(1, headerId);
      rs = st.executeQuery();
      if (rs.next()) {
        ls = rs.getString("ad_org_id");
      }
    } catch (final SQLException e) {
      log4j.error("Exception in getOrgId DAO", e);
    } catch (final Exception e) {
      log4j.error("Exception in getOrgId DAO", e);
    } finally {
      try {
        rs.close();
        st.close();
      } catch (final SQLException e) {
        log4j.error("Exception in getOrgId DAO", e);
      }
    }
    return ls;
  }
}