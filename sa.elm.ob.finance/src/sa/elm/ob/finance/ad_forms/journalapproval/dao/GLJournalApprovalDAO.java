package sa.elm.ob.finance.ad_forms.journalapproval.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
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
import org.openbravo.model.financialmgmt.gl.GLJournal;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalConnectionProvider;

import sa.elm.ob.finance.ad_forms.journalapproval.vo.GLJournalApprovalVO;
import sa.elm.ob.finance.process.gl_journal.FIN_AddPaymentFromJournal;
import sa.elm.ob.utility.EutJournalApproval;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.ad_forms.delegation.dao.ApprovalDelegationDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

public class GLJournalApprovalDAO {
  private Connection conn = null;
  private static Logger log4j = Logger.getLogger(GLJournalApprovalDAO.class);
  ApprovalDelegationDAO delegateDAO = null;

  public GLJournalApprovalDAO(Connection con) {
    this.conn = con;
    delegateDAO = new ApprovalDelegationDAO(conn);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public JSONObject getGetGlJournalList(VariablesSecureApp vars, String clientId, String orgId,
      String roleId, String userId, JSONObject searchAttr, GLJournalApprovalVO searchVO,
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

      countQuery.append("select count(distinct gl.gl_journal_id) as totalRecord ");

      selectQuery.append(
          "select distinct gl.gl_journal_id,glact.name as ledger,gl.description as description,gl.documentno as documentno, ");
      selectQuery.append(
          "gl.datedoc as documentdate,gl.dateacct as accountdate,per.name as period,gl.totaldr as debitamount,gl.totalcr as creditamount,cast(cur.iso_code as text) as currency,org.name as orgname, ");
      selectQuery.append(
          "usr.name as requester, coalesce(rd.created, gl.created) as requestdate, case when gl.isopening='N' then 'No' when gl.isopening='Y' then 'Yes' else '' end as Opening,nxtr.eut_next_role_id,gl.ad_org_id ,gl.gl_journalbatch_id ");

      fromQuery
          .append("from gl_journal gl left join c_period per on per.c_period_id=gl.c_period_id  ");
      fromQuery.append(
          "left join c_currency cur on cur.c_currency_id = gl.c_currency_id left join c_acctschema glact on gl.c_acctschema_id =glact.c_acctschema_id ");
      fromQuery.append("left join ad_org org on org.ad_org_id=gl.ad_org_id ");
      fromQuery.append(
          "left join (select tb1.createdby, tb1.gl_journal_id, tb1.created from eut_journal_approval tb1 join (select gl_journal_id, max(created) as created from eut_journal_approval where status ilike 'submitted' group by gl_journal_id order by created desc, gl_journal_id) tb2 on tb1.gl_journal_id = tb2.gl_journal_id and tb1.created = tb2.created)rd on rd.gl_journal_id = gl.gl_journal_id ");
      fromQuery.append(
          "left join ad_user usr on rd.createdby = usr.ad_user_id join eut_next_role nxtr on nxtr.eut_next_role_id = gl.em_eut_next_role_id  ");
      fromQuery.append(
          "join eut_next_role_line nxtrln on nxtrln.eut_next_role_id = nxtr.eut_next_role_id and (nxtrln.ad_role_id = '"
              + roleId
              + "' or nxtrln.ad_role_id in (select qdd.ad_role_id from eut_docapp_delegate qdd"
              + " join eut_docapp_delegateln qddl on qdd.eut_docapp_delegate_id = qddl.eut_docapp_delegate_id where qddl.ad_user_id = '"
              + userId + "' and qddl.document_type = '" + Resource.GLJOURNAL_RULE
              + "' and now() between qdd.from_date and coalesce(qdd.to_date, to_date('31-12-9999 23:59:59','dd-MM-yyyy HH24:mi:ss')))) ");

      whereClause.append("where gl.docstatus = 'EFIN_WFA' and gl.em_eut_next_role_id is not null ");
      whereClause.append("and gl.ad_client_id= '").append(clientId).append("' ");
      whereClause.append("and gl.ad_org_id in (").append(Utility.getAccessibleOrg(vars))
          .append(") and gl.isactive = 'Y' ");

      if (searchAttr != null && searchAttr.has("search")
          && searchAttr.getString("search").equals("true")) {
        if (!StringUtils.isEmpty(searchVO.getOrgName()))
          whereClause.append("and org.name ilike '%").append(searchVO.getOrgName()).append("%' ");
        if (!StringUtils.isEmpty(searchVO.getDocumentNo()))
          whereClause.append("and gl.documentno ilike '%").append(searchVO.getDocumentNo())
              .append("%' ");
        if (!StringUtils.isEmpty(searchVO.getDocumentDate()))
          whereClause.append("and to_date(to_char(gl.datedoc, 'dd-MM-yyyy'), 'dd-MM-yyyy')")
              .append(searchVO.getDocumentDate().split("##")[0]).append(" to_date('")
              .append(searchVO.getDocumentDate().split("##")[1]).append("','dd-MM-yyyy') ");
        if (!StringUtils.isEmpty(searchVO.getAccountDate()))
          whereClause.append("and to_date(to_char(gl.dateacct, 'dd-MM-yyyy'), 'dd-MM-yyyy')")
              .append(searchVO.getAccountDate().split("##")[0]).append(" to_date('")
              .append(searchVO.getAccountDate().split("##")[1]).append("','dd-MM-yyyy') ");
        if (!StringUtils.isEmpty(searchVO.getDescription()))
          whereClause.append("and gl.description ilike '%").append(searchVO.getDescription())
              .append("%' ");
        if (!StringUtils.isEmpty(searchVO.getLedger()))
          whereClause.append("and glact.name ilike '%").append(searchVO.getLedger()).append("%' ");
        if (!StringUtils.isEmpty(searchVO.getCreditAmount())
            && Double.parseDouble(searchVO.getCreditAmount()) > 0)
          whereClause.append("and gl.totalcr >= ").append(searchVO.getCreditAmount()).append(" ");
        if (!StringUtils.isEmpty(searchVO.getDebitAmount())
            && Double.parseDouble(searchVO.getDebitAmount()) > 0)
          whereClause.append("and gl.totaldr >= ").append(searchVO.getDebitAmount()).append(" ");
        if (!StringUtils.isEmpty(searchVO.getPeriod()))
          whereClause.append("and per.name ilike '%").append(searchVO.getPeriod()).append("%' ");
        if (!StringUtils.isEmpty(searchVO.getRequester()))
          whereClause.append("and usr.name ilike '%").append(searchVO.getRequester()).append("%' ");
        if (!StringUtils.isEmpty(searchVO.getRequesterDate()))
          whereClause.append(
              "and to_date(to_char(coalesce(rd.created, gl.created), 'dd-MM-yyyy'), 'dd-MM-yyyy')")
              .append(searchVO.getRequesterDate().split("##")[0]).append(" to_date('")
              .append(searchVO.getRequesterDate().split("##")[1]).append("','dd-MM-yyyy') ");
      }
      if (searchAttr != null && searchAttr.has("sortName")
          && !StringUtils.isEmpty(searchAttr.getString("sortName"))) {

        orderClause.append("order by ").append(searchAttr.getString("sortName")).append(" ")
            .append(searchAttr.getString("sortType"));
      }

      int offset = 0, totalPage = 0, totalRecord = 0, rows = 0, page = 0;
      if (searchAttr != null) {
        rows = Integer.parseInt(searchAttr.getString("rows"));
        page = Integer.parseInt(searchAttr.getString("page"));
      }
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
          json.put("ledger", row[1] == null ? null : row[1].toString());
          json.put("description", row[2].toString());
          json.put("documentno", row[3].toString().trim());
          json.put("documentdate",
              UtilityDAO.convertToHijriDate(row[4].toString()).substring(0, 10));
          json.put("accountdate",
              UtilityDAO.convertToHijriDate(row[5].toString()).substring(0, 10));
          json.put("period", row[6].toString());
          // json.put("debitamount", (Utility.getNumberFormat(vars,
          // Utility.numberFormat_PriceRelation, row[7].toString()) + "" +
          // (Double.parseDouble(row[7].toString()) == 0 ? "" : " " + row[9].toString().trim())));
          // json.put("creditamount", (Utility.getNumberFormat(vars,
          // Utility.numberFormat_PriceRelation, row[8].toString()) + "" +
          // (Double.parseDouble(row[8].toString()) == 0 ? "" : " " + row[9].toString().trim())));
          json.put("debitamount", (Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
              row[7].toString())));
          json.put("creditamount", (Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, row[8].toString())));
          json.put("currency", row[9].toString());
          json.put("requester", Utility.nullToEmpty(row[11]));
          json.put("eutNextRoleId", Utility.nullToEmpty(row[14]));
          json.put("requestdate",
              row[12] == null ? "" : UtilityDAO.convertToHijriTimestamp(row[12].toString()));
          json.put("ad_org_id", row[15].toString());
          json.put("Opening", row[13].toString());
          json.put("orgname", row[10].toString().trim());
          json.put("JournalBatch", row[16] == null ? null : row[16].toString());
          jsonArray.put(json);
        }
      }
      result.put("rows", jsonArray);
    } catch (final Exception e) {
      log4j.error("Exception in getGetGlJournalList :", e);
      return result;
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  @SuppressWarnings("rawtypes")
  public GLJournalApprovalVO getGlJournalDetails(String strJournalId) {
    GLJournalApprovalVO headerVO = new GLJournalApprovalVO();
    SQLQuery query = null;
    try {
      OBContext.setAdminMode();

      StringBuilder sqlBuilder = new StringBuilder();
      sqlBuilder.append(
          "select glact.name as ledger,gl.description as description,gl.documentno as documentno,gl.datedoc as documentdate,gl.dateacct as accountdate,per.name as period,gl.totaldr as debitamount,gl.totalcr as creditamount,cast(cur.iso_code as text) as currency,bat.gl_journalbatch_id "
              + " from gl_journal gl left join gl_journalbatch bat on bat.gl_journalbatch_id= gl.gl_journalbatch_id left join c_period per on per.c_period_id=gl.c_period_id  left join c_currency cur on cur.c_currency_id = gl.c_currency_id "
              + " left join c_acctschema glact on gl.c_acctschema_id =glact.c_acctschema_id left join ad_org org on org.ad_org_id=gl.ad_org_id "
              + " where gl.gl_journal_id='" + strJournalId + "'");
      query = OBDal.getInstance().getSession().createSQLQuery(sqlBuilder.toString());
      log4j.debug("st:" + sqlBuilder.toString());
      if (query != null && query.list().size() > 0) {
        for (Iterator iterator = query.list().iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          /*
           * log4j .debug("entered111"); log4j .debug("ledger"+row[0]); log4j
           * .debug("leder"+Utility.nullToZero(row[0]));
           */
          headerVO.setLedger(Utility.nullToZero(row[0]).toString());
          headerVO.setDescription(row[1].toString());
          headerVO.setDocumentNo(row[2].toString());
          headerVO.setDocumentDate(row[3].toString());
          headerVO.setAccountDate(row[4].toString());
          headerVO.setPeriod(row[5].toString());
          headerVO.setDebitAmount(Utility.nullToZero(row[6]));
          headerVO.setCreditAmount(Utility.nullToZero(row[7]));
          headerVO.setCurSymbol(row[8].toString());
          headerVO.setApprovalId(row[9] == null ? null : row[9].toString());
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
  public List<GLJournalApprovalVO> getJournalLines(String strJournalId) {
    List<GLJournalApprovalVO> GlJournalList = new ArrayList<GLJournalApprovalVO>();
    SQLQuery query = null;
    GLJournalApprovalVO vos = null;
    try {
      StringBuilder queryBuilder = new StringBuilder();
      queryBuilder.append(
          " select line.line as lineno,line.em_efin_uniquecode as account,line.description as description,line.amtsourcecr as credit, "
              + " line.amtsourcedr as debit,line.qty as qty,unit.name as uom  from gl_journalline line left join gl_journal gl on gl.gl_journal_id = line.gl_journal_id "
              + " left join c_uom unit on unit.c_uom_id = line.c_uom_id left join c_validcombination com on line.c_validcombination_id = com.c_validcombination_id left join c_elementvalue ele on com.account_id =ele.c_elementvalue_id"
              + " where line.gl_journal_id='" + strJournalId + "' order by line asc");
      OBContext.setAdminMode();
      query = OBDal.getInstance().getSession().createSQLQuery(queryBuilder.toString());

      if (query != null && query.list().size() > 0) {
        for (Iterator iterator = query.list().iterator(); iterator.hasNext();) {
          Object[] rows = (Object[]) iterator.next();
          vos = new GLJournalApprovalVO();
          vos.setLineno(Utility.nullToEmpty(rows[0]));
          vos.setLineAccount(rows[1].toString());
          vos.setLineDescription(rows[2].toString());
          vos.setLineCredit(Utility.nullToZero(rows[3]));
          vos.setLineDebit(Utility.nullToZero(rows[4]));
          vos.setLineQty(Utility.nullToZero(rows[5]));
          vos.setLineUom(Utility.nullToEmpty(rows[6]));
          GlJournalList.add(vos);
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in getJournalLines :", e);
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
    return GlJournalList;
  }

  @SuppressWarnings("rawtypes")
  public List<GLJournalApprovalVO> getApprovalHistory(String strJournalId) {
    List<GLJournalApprovalVO> list = new ArrayList<GLJournalApprovalVO>();
    SQLQuery query = null;
    StringBuilder queryBuilder = null;
    try {
      queryBuilder = new StringBuilder();
      queryBuilder.append(
          "select eut_journal_approval_id, poapp.ad_user_id, usr.name as approvername, poapp.ad_role_id, rol.name as approverrole, ");
      queryBuilder.append(
          "note.obuiapp_note_id, note.note as note, approveddate as approveddate, status from eut_journal_approval poapp ");
      queryBuilder.append(
          "left join ad_user usr on usr.ad_user_id = poapp.ad_user_id left join ad_role rol on rol.ad_role_id = poapp.ad_role_id ");
      queryBuilder.append(
          "left join obuiapp_note note on note.obuiapp_note_id = poapp.obuiapp_note_id where gl_journal_id ='")
          .append(strJournalId).append("' order by poapp.created;");
      query = OBDal.getInstance().getSession().createSQLQuery(queryBuilder.toString());
      if (query != null && query.list().size() > 0) {
        for (Iterator iterator = query.list().iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          GLJournalApprovalVO vos = new GLJournalApprovalVO();
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

  /*
   * public OBError approveWaitingPayments(VariablesSecureApp vars, Connection con, String
   * strJournalId, String strNextRoleId, String strComments, String inpOrgId) { GLJournal journal =
   * null; EutNextRole nextRole = null; org.openbravo.model.ad.domain.List documentAction = null;
   * User user = null; ConnectionProvider connectionProvider = null;
   * 
   * try { OBContext.setAdminMode();
   * 
   * connectionProvider = new DalConnectionProvider(true);
   * 
   * OBError returnResponseObject = null;
   * 
   * user = getTableObject(User.class, vars.getUser());
   * 
   * if(strNextRoleId != null) nextRole = getTableObject(EutNextRole.class, strNextRoleId); journal
   * = getTableObject(GLJournal.class, strJournalId);
   * 
   * if(nextRole != null) { returnResponseObject = new OBError();
   * returnResponseObject.setType("Success");
   * returnResponseObject.setTitle(org.openbravo.erpCommon.utility.Utility.messageBD(
   * connectionProvider, "Success", OBContext.getOBContext().getLanguage().getLanguage()));
   * 
   * //payment.setQuNextRole(nextRole); journal.setUpdatedBy(user);
   * 
   * insertApprovalHistory(vars, "APP", journal, strComments, inpOrgId);
   * 
   * returnResponseObject.setMessage("Payment approved and submitted for next approval"); } else {
   * if(payment.getQpoeDocumentAction() != null || payment.getQpoeDocumentAction() != "")
   * documentAction = getTableObject(org.openbravo.model.ad.domain.List.class,
   * payment.getQpoeDocumentAction());
   * 
   * returnResponseObject = FIN_AddPayment.processPayment(vars, connectionProvider,
   * documentAction.getSearchKey().equals("PRP") || documentAction.getSearchKey().equals("PPP") ?
   * "P" : "D", payment); returnResponseObject.setMessage("Payment Processed");
   * 
   * insertApprovalHistory(vars, "APP", journal, strComments, inpOrgId); }
   * OBDal.getInstance().save(journal); OBDal.getInstance().flush();
   * OBDal.getInstance().commitAndClose();
   * 
   * return returnResponseObject;
   * 
   * } catch (Exception e) { log4j.error("Exception while approving payment : ", e);
   * 
   * final OBError errorsObject = new OBError(); errorsObject.setType("Error");
   * errorsObject.setTitle("Error"); errorsObject.setMessage(e.getMessage());
   * 
   * return errorsObject; } finally { OBContext.restorePreviousMode(); } }
   */

  /*
   * public OBError reworkPayment(VariablesSecureApp vars, String strFinPaymentId, String
   * strComments, String inpOrgId) { ConnectionProvider connectionProvider = null; FIN_Payment
   * payment = null; User user = null;
   * 
   * try { Set<String> invoiceDocNos = new HashSet<String>(); OBError reworkResponse = new
   * OBError(); connectionProvider = new DalConnectionProvider(true);
   * 
   * reworkResponse.setType("Success");
   * reworkResponse.setTitle(org.openbravo.erpCommon.utility.Utility.messageBD(connectionProvider,
   * "Success", OBContext.getOBContext().getLanguage().getLanguage()));
   * 
   * OBContext.setAdminMode();
   * 
   * payment = getTableObject(FIN_Payment.class, strFinPaymentId); user = getTableObject(User.class,
   * vars.getUser());
   * 
   * if(payment != null) { for (final FIN_PaymentDetail paymentDetail :
   * payment.getFINPaymentDetailList()) { for (final FIN_PaymentScheduleDetail paymentScheduleDetail
   * : paymentDetail.getFINPaymentScheduleDetailList()) {
   * if(paymentScheduleDetail.getInvoicePaymentSchedule() != null) { // Related to invoices for
   * (final FIN_PaymentScheduleDetail invScheDetail :
   * paymentScheduleDetail.getInvoicePaymentSchedule().
   * getFINPaymentScheduleDetailInvoicePaymentScheduleList()) {
   * invoiceDocNos.add(invScheDetail.getInvoicePaymentSchedule().getInvoice().getDocumentNo()); } }
   * } }
   * 
   * undoUsedCredit(payment, vars, invoiceDocNos); payment.setUpdatedBy(user);
   * payment.setStatus("RPAP"); // payment.setQuNextRole(null);
   * 
   * insertApprovalHistory(vars, "RW", payment, strComments, inpOrgId);
   * 
   * OBDal.getInstance().save(payment); OBDal.getInstance().flush();
   * OBDal.getInstance().commitAndClose();
   * 
   * reworkResponse.setMessage("Payment sent for a rework"); }
   * 
   * return reworkResponse; } catch (Exception e) { log4j.error("Exception while rework :  ", e);
   * 
   * final OBError errorsObject = new OBError(); errorsObject.setType("Error");
   * errorsObject.setTitle("Error"); errorsObject.setMessage(e.getMessage());
   * 
   * return errorsObject; } finally { OBContext.restorePreviousMode(); } }
   */

  public JSONObject approveMultiJournal(VariablesSecureApp vars, JSONArray journalList,
      String strComments) {
    ConnectionProvider connectionProvider = null;
    JSONObject resultObject = new JSONObject();
    int successCont = 0;
    int errorCont = 0;
    String errorMsg = "";
    try {
      resultObject.put("result", "0");
      resultObject.put("resultMsg", "Exception while approving G/L Journal");
      resultObject.put("resultDesc", "");

      boolean allProcessed = true;

      OBContext.setAdminMode();
      User user = getTableObject(User.class, vars.getUser());
      for (int i = 0; i < journalList.length(); i++) {
        JSONObject json = journalList.getJSONObject(i);
        try {
          // Getting Next Role
          NextRoleByRuleVO roleVO = null;
          EutNextRole nextRole = null;
          GLJournal journal = null;
          connectionProvider = new DalConnectionProvider(true);
          String orgId = getOrgId(json.getString("id"));
          journal = getTableObject(GLJournal.class, json.getString("id"));
          boolean updatePayment = true;
          /*
           * if(StringUtils.length(json.getString("quNextRoleId")) == 32) roleVO =
           * NextRoleByRule.getNextRole(connectionProvider.getConnection(), vars.getClient(),
           * vars.getOrg(), vars.getRole(), vars.getUser(), PaymentOutDocumentRule.RULE_PAYMENT_OUT,
           * json.getString("amount")); else roleVO =
           * NextRoleByRule.getDelegatedNextRole(connectionProvider.getConnection(),
           * vars.getClient(), vars.getOrg(), json.getString("fromUserRoleId"),
           * json.getString("toUserRoleId"), vars.getUser(),
           * PaymentOutDocumentRule.RULE_PAYMENT_OUT, json.getString("amount"));
           */
          if (isDirectApproval(json.getString("id"), vars.getRole())) {
            // roleVO = NextRoleByRule.getNextRole(connectionProvider.getConnection(),
            // vars.getClient(), orgId, vars.getRole(), vars.getUser(), Resource.GLJOURNAL_RULE,
            // json.getString("amount"));
            roleVO = NextRoleByRule.getLineManagerBasedNextRole(OBDal.getInstance().getConnection(),
                vars.getClient(), orgId, vars.getRole(), vars.getUser(), Resource.GLJOURNAL_RULE,
                BigDecimal.ZERO, journal.getCreatedBy().getId(), false,
                journal.getDocumentStatus());
          } else {
            HashMap<String, String> roleId = NextRoleByRule.getDelegatedFromAndToRoles(
                connectionProvider.getConnection(), vars.getClient(), orgId, vars.getUser(),
                Resource.GLJOURNAL_RULE, json.getString("quNextRoleId"));
            String delegatedFromRole = roleId.get("FromUserRoleId");
            String delegatedToRole = roleId.get("ToUserRoleId");
            if (delegatedFromRole != null && delegatedToRole != null)
              roleVO = NextRoleByRule.getDelegatedNextRole(connectionProvider.getConnection(),
                  vars.getClient(), orgId, delegatedFromRole, delegatedToRole, vars.getUser(),
                  Resource.GLJOURNAL_RULE, json.getString("amount"));
          }
          if (roleVO != null && roleVO.getErrorMsg() != null
              && roleVO.getErrorMsg().equals("NoManagerAssociatedWithRole")) {
            resultObject.put("result", "0");
            resultObject.put("resultMsg", "Exception while approving G/L Journal");
            resultObject.put("resultCoreMsg", OBMessageUtils.messageBD("Escm_No_LineManager"));
            return resultObject;
          } else if (roleVO != null && roleVO.hasApproval()) {
            nextRole = getTableObject(EutNextRole.class, roleVO.getNextRoleId());
          }
          if (nextRole != null) {
            journal.setEutNextRole(nextRole);
            journal.setUpdatedBy(user);
            insertApprovalHistory(vars, "APP", journal, strComments, json.getString("orgId"),
                roleVO.getStatus());

            json.put("result", "1");
            json.put("resultMsg", "G/L Journal approved and submitted for next approval");
            json.put("resultCoreMsg", "G/L Journal approved and submitted for next approval");
          } else {
            // if(payment.getQpoeDocumentAction() != null || payment.getQpoeDocumentAction() != "")
            // documentAction = getTableObject(org.openbravo.model.ad.domain.List.class,
            // payment.getQpoeDocumentAction());

            // Process the Journal
            if (!journal.isProcessed()) {
              // Recover again the object to avoid problems with Dal
              journal = OBDal.getInstance().get(GLJournal.class, journal.getId());
              ConnectionProvider con = new DalConnectionProvider(true);
              ProcessBundle pb = new ProcessBundle("CC92F947C77D48EEAA444B7105F8C020", vars)
                  .init(con);
              HashMap<String, Object> parameters = new HashMap<String, Object>();
              parameters.put("GL_Journal_ID", journal.getId());
              parameters.put("inpdocaction", journal.getDocumentAction());
              pb.setParams(parameters);
              OBError myMessage = null;
              // Process each Joural
              FIN_AddPaymentFromJournal myProcess = new FIN_AddPaymentFromJournal();
              myProcess.execute(pb);
              myMessage = (OBError) pb.getResult();
              if (myMessage.getType().equals("Error")) {
                errorCont++;
                if (!"".equals(errorMsg)) {
                  errorMsg = errorMsg + "<br />";
                }
                errorMsg = errorMsg + "@FIN_JournalBatchErrorProcess@ " + journal.getDocumentNo()
                    + ". " + myMessage.getMessage();
              } else {
                successCont++;
              }
            }
            if (errorCont > 0 && successCont == 0) {
              updatePayment = false;
              json.put("result", "0");
              json.put("resultMsg", "Exception while approving G/L Journal");
              json.put("resultCoreMsg", errorMsg);
              allProcessed = false;
            } else {
              // payment.setQuNextRole(null);
              journal.setUpdatedBy(user);
              journal.setEutNextRole(null);
              insertApprovalHistory(vars, "APP", journal, strComments, json.getString("orgId"),
                  roleVO.getStatus());

              json.put("result", "1");
              json.put("resultMsg", "G/L Journal Processed");
              json.put("resultCoreMsg", "G/L Journal Processed");
            }
          }
          if (updatePayment) {
            OBDal.getInstance().save(journal);
            OBDal.getInstance().flush();
            OBDal.getInstance().commitAndClose();
          }
        } catch (Exception e) {
          json.put("result", "0");
          json.put("resultMsg", "Exception while approving G/L Journal");
          allProcessed = false;
        }
      }

      resultObject.put("result", "1");
      resultObject.put("resultMsg",
          Utility.getADMessage("Success", OBContext.getOBContext().getLanguage().getLanguage()));
      resultObject.put("resultDesc", "G/L Journal Processed");
      resultObject.put("journalList", journalList);

      NextRoleByRule.deleteUnusedNextRoles(OBDal.getInstance().getConnection(true),
          Resource.GLJOURNAL_RULE);

      if (allProcessed == false) {
        resultObject.put("result", "2");
        resultObject.put("resultMsg", "Some G/L Journal has not been processed");
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

  public JSONObject reworkMultiJournal(VariablesSecureApp vars, JSONArray journalList,
      String strComments) {
    GLJournal journal = null;
    User user = null;
    JSONObject resultObject = new JSONObject();
    try {

      resultObject.put("result", "0");
      resultObject.put("resultMsg", "Exception while assigning rework for G/L Journal");
      resultObject.put("resultDesc", "");

      OBContext.setAdminMode();

      for (int i = 0; i < journalList.length(); i++) {
        JSONObject json = journalList.getJSONObject(i);
        try {
          journal = getTableObject(GLJournal.class, json.getString("id"));
          user = getTableObject(User.class, vars.getUser());

          if (journal != null) {

            journal.setUpdatedBy(user);
            journal.setDocumentStatus("DR");
            journal.setEutNextRole(null);
            journal.setEfinAction("CO");

            insertApprovalHistory(vars, "RW", journal, strComments, json.getString("orgId"), null);

            OBDal.getInstance().save(journal);
            OBDal.getInstance().flush();
            OBDal.getInstance().commitAndClose();
          }
          json.put("result", "1");
          json.put("resultMsg", "Payment sent for rework");
        } catch (Exception e) {
          json.put("result", "0");
          json.put("resultMsg", "Exception while assigning rework for G/L Journal");

        }
      }

      resultObject.put("result", "1");
      resultObject.put("resultMsg",
          Utility.getADMessage("Success", OBContext.getOBContext().getLanguage().getLanguage()));
      resultObject.put("journalList", journalList);
      resultObject.put("resultDesc", "G/L Journal sent for Rework");

      NextRoleByRule.deleteUnusedNextRoles(OBDal.getInstance().getConnection(true),
          Resource.GLJOURNAL_RULE);

      return resultObject;
    } catch (Exception e) {
      log4j.error("Exception in reworkMultiPayment :  ", e);
      return resultObject;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static void insertApprovalHistory(VariablesSecureApp vars, String status,
      GLJournal payment, String strComments, String inpOrgId, String pendingApp) {
    EutJournalApproval poApproval = null;
    Client client = null;
    Organization organization = null;
    User user = null;
    Role role = null;
    Note notes = null;
    Table table = null;
    try {
      OBContext.setAdminMode();

      client = OBDal.getInstance().get(Client.class, vars.getClient());
      organization = OBDal.getInstance().get(Organization.class, vars.getOrg());
      user = OBDal.getInstance().get(User.class, vars.getUser());
      role = OBDal.getInstance().get(Role.class, vars.getRole());

      poApproval = OBProvider.getInstance().get(EutJournalApproval.class);

      poApproval.setClient(client);
      poApproval.setOrganization(organization);
      poApproval.setCreatedBy(user);
      poApproval.setUpdatedBy(user);
      poApproval.setAlertStatus(
          status.equals("APP") ? "APP" : (status.equals("RE") ? "REACT" : "ASSREW"));
      poApproval.setRole(role);
      poApproval.setUserContact(user);
      poApproval.setJournalEntry(payment);
      poApproval.setApproveddate(new Date());
      poApproval.setPendingapproval(pendingApp);
      OBDal.getInstance().save(poApproval);
      if (strComments != null && !strComments.isEmpty()) {
        table = getTableObject(Table.class, "224");
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

      OBDal.getInstance().flush();

    } catch (Exception e) {
      log4j.error(" Exception while adding in approval ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static <T extends BaseOBObject> T getTableObject(Class<T> t, String strId) {
    return OBDal.getInstance().get(t, strId);
  }

  /*
   * private void undoUsedCredit(FIN_Payment myPayment, VariablesSecureApp vars, Set<String>
   * invoiceDocNos) { final List<FIN_Payment> payments = new ArrayList<FIN_Payment>();
   * 
   * for (final FIN_Payment_Credit pc : myPayment.getFINPaymentCreditList()) { final FIN_Payment
   * creditPaymentUsed = pc.getCreditPaymentUsed();
   * creditPaymentUsed.setUsedCredit(creditPaymentUsed.getUsedCredit().subtract(pc.getAmount()));
   * payments.add(creditPaymentUsed); }
   * 
   * for (final FIN_Payment payment : payments) { // Update Description final String payDesc =
   * payment.getDescription();
   * 
   * if(payDesc != null) { final String invoiceDocNoMsg =
   * org.openbravo.erpCommon.utility.Utility.messageBD(new DalConnectionProvider(),
   * "APRM_CreditUsedinInvoice", vars.getLanguage()); if(invoiceDocNoMsg != null) { final
   * StringBuffer newDesc = new StringBuffer(); for (final String line : payDesc.split("\n")) {
   * boolean include = true; if(line.startsWith(invoiceDocNoMsg.substring(0,
   * invoiceDocNoMsg.lastIndexOf("%s")))) { for (final String docNo : invoiceDocNos) {
   * if(line.indexOf(docNo) > 0) { include = false; break; } } } if(include) { newDesc.append(line);
   * if(!"".equals(line)) newDesc.append("\n"); } } // Truncate Description to keep length as 255
   * payment.setDescription(newDesc.toString().length() > 255 ? newDesc.toString().substring(0, 255)
   * : newDesc.toString()); } } } myPayment.getFINPaymentCreditList().clear(); }
   */
  public List<GLJournalApprovalVO> getCommentsHistory(String paymentId) {
    GLJournalApprovalVO VO = null;
    PreparedStatement st = null;
    ResultSet rs = null;
    List<GLJournalApprovalVO> list = new ArrayList<GLJournalApprovalVO>();

    try {
      String sql = " select app.ad_user_id, app.ad_role_id, to_char(app.approveddate,'dd-MM-yyyy') as approveddate, to_char(app.approveddate,'hh12:mi:ss AM') as approvedtime, status, usr.name as approvername, rol.name as approverrole, nte.note from qpoe_po_approval app "
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
          VO = new GLJournalApprovalVO();
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
      query = "  select count(*) from gl_journal jou join eut_next_role_line ln on ln.eut_next_role_id = jou.em_eut_next_role_id     and jou.gl_journal_id='"
          + headerId + "'     and ln.ad_role_id ='" + roleId + "'";

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
      st = conn.prepareStatement("select ad_org_id from gl_journal where gl_journal_id=?");
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