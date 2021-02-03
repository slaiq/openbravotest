package sa.elm.ob.finance.ad_forms.clearbankcheque.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;

import sa.elm.ob.finance.EfinAccount;
import sa.elm.ob.finance.EfinBank;
import sa.elm.ob.finance.ad_forms.clearbankcheque.vo.ClearChequeVO;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author Gowtham
 *
 */
public class ClearChequeDao {
  @SuppressWarnings("unused")
  private Connection conn = null;
  private static Logger log4j = Logger.getLogger(ClearChequeDao.class);
  DateFormat yearFormat = Utility.YearFormat;
  DateFormat dateFormat = Utility.dateFormat;

  public ClearChequeDao(Connection con) {
    this.conn = con;
  }

  /**
   * This method is used to get Organization.
   * 
   * @param clientId
   * @param searchTerm
   * @param pagelimit
   * @param page
   * @return
   */
  public JSONObject getOrganization(String clientId, String searchTerm, int pagelimit, int page) {
    JSONObject json = null;
    JSONArray jsonArray = new JSONArray();
    String whereClause = "";
    List<Organization> orgList = null;
    try {
      json = new JSONObject();
      json.put("totalRecords", 0);

      whereClause = whereClause.concat(
          " as e where e.id <> '0' and ehcmOrgtyp.id in (select ot.id from EHCM_org_type ot where ot.searchKey='ORG') ");
      if (searchTerm != null && !searchTerm.equals("")) {
        whereClause = whereClause.concat(" and (lower(e.name) like '%" + searchTerm.toLowerCase()
            + "%'  or lower(e.searchKey) like '%" + searchTerm.toLowerCase() + "%' )");
      }
      whereClause = whereClause.concat(" order by e.searchKey ");

      OBQuery<Organization> org = OBDal.getInstance().createQuery(Organization.class, whereClause);
      orgList = org.list();
      JSONObject jsonData = new JSONObject();
      if (orgList.size() > 0) {
        json.put("totalRecords", orgList.size());

        for (Organization organization : orgList) {
          jsonData = new JSONObject();
          jsonData.put("id", organization.getId());
          jsonData.put("recordIdentifier",
              organization.getSearchKey() + "-" + organization.getName());
          jsonArray.put(jsonData);
        }
      }

      json.put("data", jsonArray);

    } catch (final Exception e) {
      log4j.error("Exception in getOrganization :", e);
      return json;
    }
    return json;
  }

  /**
   * This method is used to get ChequeStatus.
   * 
   * @param clientId
   * @param searchTerm
   * @param pagelimit
   * @param page
   * @return
   */
  public JSONObject getChequeStatus(String clientId, String searchTerm, int pagelimit, int page) {
    JSONObject json = null;
    JSONArray jsonArray = new JSONArray();
    String whereClause = "";
    final String Reference = "2EEFBB94A5674400AB7C27B92BB2C2A9";
    try {
      json = new JSONObject();
      json.put("totalRecords", 0);
      if (searchTerm != null && !searchTerm.equals("")) {
        whereClause = whereClause.concat(
            " and (lower(coalesce(trl.name,ref.name)) like '%" + searchTerm.toLowerCase() + "%') ");
      }
      whereClause = whereClause.concat(" order by status ");
      Query query = null;
      List<Object> statusList = new ArrayList<>();
      String sqlString = "select coalesce(trl.name,ref.name) as status,ref.value from ad_ref_list ref "
          + " left join AD_Ref_List_Trl trl on trl.ad_ref_list_id= ref.ad_ref_list_id and AD_Language=:adLanguage "
          + "where ref.ad_reference_id =:refId ";
      sqlString = sqlString.concat(whereClause);
      query = OBDal.getInstance().getSession().createSQLQuery(sqlString);
      query.setParameter("refId", Reference);
      query.setParameter("adLanguage", OBContext.getOBContext().getLanguage().getLanguage());
      statusList = query.list();
      JSONObject jsonData = new JSONObject();
      if (statusList.size() > 0) {
        json.put("totalRecords", statusList.size());
        for (Object obj : statusList) {
          Object[] row = (Object[]) obj;
          jsonData = new JSONObject();
          jsonData.put("id", row[1].toString());
          jsonData.put("recordIdentifier", row[0].toString());
          jsonArray.put(jsonData);
        }
      }
      json.put("data", jsonArray);
    } catch (final Exception e) {
      log4j.error("Exception in getChequeStatus :", e);
      return json;
    }
    return json;
  }

  /**
   * This method is used to get Payment out List.
   * 
   * @param orgId
   * @param searchAttr
   * @return
   */
  @SuppressWarnings("unchecked")
  public JSONObject getPaymentList(ClearChequeVO vo, JSONObject searchAttr) {
    JSONObject result = new JSONObject(), json = null;
    JSONArray jsonArray = new JSONArray();
    List<Object> paymentList = null;
    int offset = 0, totalPage = 0, totalRecord = 0;
    SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
    String chqDate = "", paymentDateFrom = "", paymentDateTo = "", sentBankDate = "",
        reconcileDate = "";
    final String Reference = "2EEFBB94A5674400AB7C27B92BB2C2A9";
    StringBuilder orderClause = new StringBuilder(), whereClause = new StringBuilder();
    try {
      int rows = Integer.parseInt(searchAttr.getString("rows")),
          page = Integer.parseInt(searchAttr.getString("page"));

      // appending where clause
      whereClause.append(" pay.isreceipt='N' and pay.status <>'EFIN_CAN' and ");

      if (!StringUtils.isEmpty(vo.getOrganization())) {
        whereClause.append(" pay.ad_org_id=:orgId and");
      }
      if (!StringUtils.isEmpty(vo.getPaymentMethod())) {
        whereClause.append(" pay.Fin_Paymentmethod_ID=:paymentMethodId and");
      }
      if (!StringUtils.isEmpty(vo.getBank())) {
        whereClause.append(" fin.em_Efin_Bank_id=:bank and");
      }
      if (!StringUtils.isEmpty(vo.getBankAccNo())) {
        whereClause.append("  fin.EM_Efin_Account_ID=:bankAccountNo and");
      }
      if (!StringUtils.isEmpty(vo.getPaymentDocSeqNoFrom())) {
        whereClause.append(" pay.em_efin_paymentsequence >=:paymentDocSeqNoFrom and");
      }
      if (!StringUtils.isEmpty(vo.getPaymentDocSeqNoTo())) {
        whereClause.append(" pay.em_efin_paymentsequence <=:paymentDocSeqNoTo and");
      }
      if (!StringUtils.isEmpty(vo.getClearBankChqNo())) {
        whereClause.append(" pay.EM_Efin_Mofchequeno=:clearBankChqNo and");
      }
      if (!StringUtils.isEmpty(vo.getClearBankChqDate())) {
        chqDate = dateYearFormat
            .format(dateYearFormat.parse(UtilityDAO.convertToGregorian(vo.getClearBankChqDate())));
        whereClause
            .append(" (cast(:chqDate as date) = cast(pay.EM_Efin_Mofchequedate as date)) and");
      }
      if (!StringUtils.isEmpty(vo.getClearingBank())) {
        whereClause.append(" pay.EM_Efin_Mofbankname=:clearingBank and");
      }
      if (!StringUtils.isEmpty(vo.getSentBankDate())) {
        sentBankDate = dateYearFormat
            .format(dateYearFormat.parse(UtilityDAO.convertToGregorian(vo.getSentBankDate())));
        whereClause
            .append(" (cast(:sentBankDate as date) = cast(pay.em_efin_banksentdate as date))  and");
      }

      if ((!StringUtils.isEmpty(vo.getPaymentDateTo())
          && !StringUtils.isEmpty(vo.getPaymentDateFrom()))) {
        paymentDateFrom = dateYearFormat
            .format(dateYearFormat.parse(UtilityDAO.convertToGregorian(vo.getPaymentDateFrom())));
        paymentDateTo = dateYearFormat
            .format(dateYearFormat.parse(UtilityDAO.convertToGregorian(vo.getPaymentDateTo())));

        whereClause.append(
            " (cast(pay.Paymentdate as date) between cast(:paymentDateFrom as date) and cast(:paymentDateTo as date)) and ");

      }

      if (!StringUtils.isEmpty(vo.getReconcileNo())) {
        whereClause.append(" acc.em_efin_acctseq=:reconcileNo and");
      }
      if (!StringUtils.isEmpty(vo.getReconcileDate())) {
        reconcileDate = dateYearFormat
            .format(dateYearFormat.parse(UtilityDAO.convertToGregorian(vo.getReconcileDate())));
        whereClause
            .append(" (cast(:reconcileDate as date) = cast (rec.statementdate as date)) and ");
      }
      if (!StringUtils.isEmpty(vo.getLockIndicator())) {
        whereClause.append(" pay.em_efin_islocked=:islock and");
      }
      if (!StringUtils.isEmpty(vo.getChequeStatus())) {
        whereClause.append(" pay.em_efin_mofchqstatus=:chqStatus and");
      }

      int index = whereClause.lastIndexOf("and");
      whereClause.replace(index, 3 + index, "");

      // appending orderby clause
      orderClause.append(" order by ");
      if (searchAttr.getString("sortName").equals("paydocSeqNo"))
        orderClause.append(" to_number(em_efin_paymentsequence) ");
      if (searchAttr.getString("sortName").equals("sentBankDate"))
        orderClause.append(" em_efin_banksentdate_tmp ");
      if (searchAttr.getString("sortName").equals("chequereceivedate"))
        orderClause.append(" em_efin_rec_cheque_date_tmp ");
      if (searchAttr.getString("sortName").equals("paydocNo"))
        orderClause.append(" document_no ");
      if (searchAttr.getString("sortName").equals("paymentDate"))
        orderClause.append(" paymentdate ");
      if (searchAttr.getString("sortName").equals("clearBankChqNo"))
        orderClause.append(" to_number(em_efin_mofchequeno_tmp) ");
      if (searchAttr.getString("sortName").equals("clearBankChqDate"))
        orderClause.append(" EM_Efin_Mofchequedate_tmp ");
      if (searchAttr.getString("sortName").equals("clearBank"))
        orderClause.append(" em_efin_mofbankname_tmp ");
      if (searchAttr.getString("sortName").equals("reconciledate"))
        orderClause.append(" rec.statementdate ");
      if (searchAttr.getString("sortName").equals("reconcileNo"))
        orderClause.append(" acc.em_efin_acctseq ");
      if (searchAttr.getString("sortName").equals("lockedIndicator"))
        orderClause.append(" EM_Efin_Islocked ");
      if (searchAttr.getString("sortName").equals("chequeStatus"))
        orderClause.append(" chqSt ");
      orderClause.append(" ").append(searchAttr.getString("sortType"));
      Query query = null;

      String sqlString = "select  acc.em_efin_acctseq as rec_no,pay.em_efin_paymentsequence as payment_doc_seq_no,pay.EM_Efin_Banksentdate"
          + " as Bank_sent_date,pay.DocumentNo as document_no,pay.Paymentdate as payment_date,"
          + " pay.EM_Efin_Mofchequedate_tmp as clearing_bank_check_date,rec.statementdate as rec_date,pay.em_efin_mofbankname_tmp as bank_name,"
          + " pay.EM_Efin_Islocked,pay.em_efin_mofchequeno_tmp as chequeno,pay.FIN_Payment_id as id,pay.em_efin_banksentdate_tmp as bankSentDate,pay.em_efin_rec_cheque_date_tmp as receiveChequeDate,(ref.value) as cheque_status,coalesce(trl.name,ref.name) as chqSt "
          + " from FIN_Payment pay "
          + " left join FIN_Financial_Account fin on pay.FIN_Financial_Account_id = fin.FIN_Financial_Account_id "
          + " left join fin_finacc_transaction tr on tr.FIN_Financial_Account_id = fin.FIN_Financial_Account_id and pay.FIN_Payment_id = tr.fin_payment_id "
          + " left join fin_reconciliation rec on rec.fin_reconciliation_id = tr.fin_reconciliation_id "
          + " left join (select distinct em_efin_acctseq,em_efin_documentno,record_id from fact_acct) acc on "
          + " rec.fin_reconciliation_id = acc.record_id "
          + " left join ad_ref_list ref on ref.value = pay.em_efin_mofchqstatus_tmp and ref.ad_reference_id =:refId "
          + " left join AD_Ref_List_Trl trl on trl.ad_ref_list_id= ref.ad_ref_list_id and AD_Language=:adLanguage "
          + " where " + whereClause.append(orderClause).toString();

      query = OBDal.getInstance().getSession().createSQLQuery(sqlString);

      if (!StringUtils.isEmpty(vo.getOrganization())) {
        query.setParameter("orgId", vo.getOrganization());
      }
      if (!StringUtils.isEmpty(vo.getPaymentMethod())) {
        query.setParameter("paymentMethodId", vo.getPaymentMethod());
      }
      if (!StringUtils.isEmpty(vo.getBank())) {
        query.setParameter("bank", vo.getBank());
      }
      if (!StringUtils.isEmpty(vo.getBankAccNo())) {
        query.setParameter("bankAccountNo", vo.getBankAccNo());
      }
      if (!StringUtils.isEmpty(vo.getPaymentDocSeqNoFrom())) {
        query.setParameter("paymentDocSeqNoFrom", vo.getPaymentDocSeqNoFrom());
      }
      if (!StringUtils.isEmpty(vo.getPaymentDocSeqNoTo())) {
        query.setParameter("paymentDocSeqNoTo", vo.getPaymentDocSeqNoTo());
      }
      if (!StringUtils.isEmpty(vo.getClearBankChqNo())) {
        query.setParameter("clearBankChqNo", vo.getClearBankChqNo());
      }
      if (!StringUtils.isEmpty(vo.getClearBankChqDate())) {
        query.setParameter("chqDate", chqDate);
      }
      if (!StringUtils.isEmpty(vo.getClearingBank())) {
        query.setParameter("clearingBank", vo.getClearingBank());
      }
      if (!StringUtils.isEmpty(vo.getSentBankDate())) {
        query.setParameter("sentBankDate", sentBankDate);
      }
      if (!StringUtils.isEmpty(vo.getPaymentDateFrom())) {
        query.setParameter("paymentDateFrom", paymentDateFrom);
      }
      if (!StringUtils.isEmpty(vo.getPaymentDateTo())) {
        query.setParameter("paymentDateTo", paymentDateTo);
      }
      if (!StringUtils.isEmpty(vo.getReconcileNo())) {
        query.setParameter("reconcileNo", vo.getReconcileNo());
      }
      if (!StringUtils.isEmpty(vo.getReconcileDate())) {
        query.setParameter("reconcileDate", reconcileDate);
      }
      if (!StringUtils.isEmpty(vo.getLockIndicator())) {
        query.setParameter("islock", vo.getLockIndicator());
      }
      if (!StringUtils.isEmpty(vo.getChequeStatus())) {
        query.setParameter("chqStatus", vo.getChequeStatus());
      }
      query.setParameter("refId", Reference);
      query.setParameter("adLanguage", OBContext.getOBContext().getLanguage().getLanguage());

      log4j.debug(query);
      paymentList = query.list();
      totalRecord = paymentList.size();

      if (searchAttr.getBoolean("fetchAllRecord") == false) {
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
        query.setMaxResults(rows);
        query.setFirstResult(offset);
        paymentList = query.list();
      }

      result.put("page", page);
      result.put("total", totalPage);
      result.put("records", totalRecord);
      if (paymentList.size() > 0) {
        for (Object con : paymentList) {
          Object[] row = (Object[]) con;

          json = new JSONObject();
          if (row[0] != null) {
            json.put("reconcileNo", row[0].toString());
          } else {
            json.put("reconcileNo", "");
          }

          if (row[1] != null) {
            json.put("paydocSeqNo", row[1].toString());

          } else {
            json.put("paydocSeqNo", "");
          }

          if (row[3] != null) {
            json.put("paydocNo", row[3].toString());
          } else {
            json.put("paydocNo", "");
          }

          if (row[4] != null) {
            json.put("paymentDate",
                UtilityDAO.convertTohijriDate(Utility.nullToEmpty(row[4].toString())));
          } else {
            json.put("paymentDate", "");
          }

          if (row[5] != null) {
            json.put("clearBankChqDate",
                UtilityDAO.convertTohijriDate(Utility.nullToEmpty(row[5].toString())));
          } else {
            json.put("clearBankChqDate", "");
          }

          if (row[6] != null) {
            json.put("reconciledate",
                UtilityDAO.convertTohijriDate(Utility.nullToEmpty(row[6].toString())));
          } else {
            json.put("reconciledate", "");
          }

          if (row[7] != null) {
            json.put("clearBank", row[7].toString());
          } else {
            json.put("clearBank", "");
          }

          if (row[8] != null) {
            json.put("lockedIndicator", row[8].toString());
          } else {
            json.put("lockedIndicator", "");
          }

          if (row[9] != null) {
            json.put("clearBankChqNo", row[9].toString());
          } else {
            json.put("clearBankChqNo", "");
          }

          if (row[10] != null) {
            json.put("id", row[10].toString());
          }

          if (row[11] != null) {
            json.put("sentBankDate",
                UtilityDAO.convertTohijriDate(Utility.nullToEmpty(row[11].toString())));
          } else {
            json.put("sentBankDate", "");
          }

          if (row[12] != null) {
            json.put("chequereceivedate",
                UtilityDAO.convertTohijriDate(Utility.nullToEmpty(row[12].toString())));
          } else {
            json.put("chequereceivedate", "");
          }

          if (row[13] != null) {
            json.put("chequeStatus", row[13].toString());
          } else {
            json.put("chequeStatus", "");
          }
          jsonArray.put(json);

        }
      }
      result.put("rows", jsonArray);

    } catch (Exception e) {
      log4j.error("Exception in clearbankChequ getPaymentList :", e);
    }
    return result;
  }

  /**
   * This method is used to get payment method.
   * 
   * @param orgId
   * @param searchTerm
   * @param pagelimit
   * @param page
   * @return
   */
  @SuppressWarnings("unchecked")
  public JSONObject getPaymentMethod(String bankId, String acctId, String searchTerm, int pagelimit,
      int page) {
    JSONObject json = null;
    JSONArray jsonArray = new JSONArray();
    List<FIN_PaymentMethod> paymentMethodList = null;

    try {
      json = new JSONObject();
      json.put("totalRecords", 0);

      String hqlQuery = "select distinct e from FIN_PaymentMethod as e join e.financialMgmtFinAccPaymentMethodList finPay "
          + " join finPay.account acct  where (acct.efinBank.id =:bankId or acct.efinAccount.id =:acctId) and finPay.payoutAllow='Y' ";
      if (searchTerm != null && !searchTerm.equals("")) {
        hqlQuery = hqlQuery.concat(" and lower(e.name) like '%" + searchTerm.toLowerCase() + "%' ");
      }
      // whereClause = whereClause.concat(" group by e.id ");
      final Query paymentQuery = OBDal.getInstance().getSession().createQuery(hqlQuery);
      paymentQuery.setParameter("bankId", bankId);
      paymentQuery.setParameter("acctId", acctId);

      paymentMethodList = paymentQuery.list();

      JSONObject jsonData = new JSONObject();
      if (paymentMethodList.size() > 0) {
        json.put("totalRecords", paymentMethodList.size());

        for (FIN_PaymentMethod payment : paymentMethodList) {
          jsonData = new JSONObject();
          jsonData.put("id", payment.getId());
          jsonData.put("recordIdentifier", payment.getName());
          jsonArray.put(jsonData);
        }
      }
      json.put("data", jsonArray);

    } catch (final Exception e) {
      log4j.error("Exception in getOrganization :", e);
      return json;
    }
    return json;
  }

  /**
   * This method is used to get bank.
   * 
   * @param clientId
   * @param searchTerm
   * @param pagelimit
   * @param page
   * @return
   */
  public JSONObject getBank(String clientId, String searchTerm, int pagelimit, int page) {
    JSONObject json = null;
    JSONArray jsonArray = new JSONArray();
    String whereClause = "";
    List<EfinBank> bankList = null;
    try {
      json = new JSONObject();
      json.put("totalRecords", 0);
      whereClause = whereClause
          .concat(" as e where e.fINFinancialAccountEMEfinBankIDList.size > 0 ");
      if (searchTerm != null && !searchTerm.equals("")) {
        whereClause = whereClause
            .concat(" and (lower(e.bankname) like '%" + searchTerm.toLowerCase()
                + "%' or lower(e.searchKey) like '%" + searchTerm.toLowerCase() + "%') ");
      }
      whereClause = whereClause.concat(" order by e.searchKey ");

      OBQuery<EfinBank> bankObj = OBDal.getInstance().createQuery(EfinBank.class, whereClause);
      bankList = bankObj.list();
      JSONObject jsonData = new JSONObject();
      if (bankList.size() > 0) {
        json.put("totalRecords", bankList.size());

        for (EfinBank bank : bankList) {
          jsonData = new JSONObject();
          jsonData.put("id", bank.getId());
          jsonData.put("recordIdentifier", bank.getSearchKey() + "-" + bank.getBankname());
          jsonArray.put(jsonData);
        }
      }

      json.put("data", jsonArray);

    } catch (final Exception e) {
      log4j.error("Exception in getOrganization :", e);
      return json;
    }
    return json;
  }

  /**
   * This method is used to get AccountNo.
   * 
   * @param bankId
   * @param searchTerm
   * @param pagelimit
   * @param page
   * @return
   */
  public JSONObject getAccountNo(String bankId, String searchTerm, int pagelimit, int page) {
    JSONObject json = null;
    JSONArray jsonArray = new JSONArray();
    String whereClause = "";
    List<EfinAccount> AcctList = null;

    try {
      json = new JSONObject();
      json.put("totalRecords", 0);

      whereClause = whereClause.concat(
          " as e where e.efinBank.id =:bankId and e.fINFinancialAccountEMEfinAccountIDList.size > 0 ");

      if (searchTerm != null && !searchTerm.equals("")) {
        whereClause = whereClause
            .concat(" and lower(e.accountNo) like '%" + searchTerm.toLowerCase() + "%' ");
      }
      OBQuery<EfinAccount> AcctObj = OBDal.getInstance().createQuery(EfinAccount.class,
          whereClause);
      AcctObj.setNamedParameter("bankId", bankId);
      AcctList = AcctObj.list();
      JSONObject jsonData = new JSONObject();
      if (AcctList.size() > 0) {
        json.put("totalRecords", AcctList.size());

        for (EfinAccount acct : AcctList) {
          jsonData = new JSONObject();
          jsonData.put("id", acct.getId());
          jsonData.put("recordIdentifier", acct.getAccountNo());
          jsonArray.put(jsonData);
        }
      }

      json.put("data", jsonArray);

    } catch (final Exception e) {
      log4j.error("Exception in getOrganization :", e);
      return json;
    }
    return json;
  }

  /**
   * This method is used to save payment out edit values.
   * 
   * @param paymentListjson
   * @return
   */
  public String savePaymentDetails(JSONObject paymentListjson, String bankId) {
    SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");

    try {
      JSONArray paymentArray = paymentListjson.getJSONArray("List");
      if (paymentArray.length() > 0) {
        for (int i = 0; i < paymentArray.length(); i++) {
          FIN_Payment payment = null;
          JSONObject json = paymentArray.getJSONObject(i);
          if (json.has("id")) {
            payment = OBDal.getInstance().get(FIN_Payment.class, json.get("id"));
          }
          if (!payment.isEfinIslocked()) {
            // trigger will make impact in actual columns.
            if (json.has("chequeNo")) {
              if (StringUtils.isNotEmpty(json.get("chequeNo").toString())) {
                OBQuery<FIN_Payment> chkDuplicate = OBDal.getInstance().createQuery(
                    FIN_Payment.class,
                    "as e join e.account acct where acct.efinAccount.id = :acct and e.efinMofchequeno =:sequence and e.id not in(:id) ");
                chkDuplicate.setNamedParameter("acct", bankId);
                chkDuplicate.setNamedParameter("sequence", json.get("chequeNo").toString());
                chkDuplicate.setNamedParameter("id", json.get("id").toString());
                if (chkDuplicate.list().size() > 0) {
                  OBDal.getInstance().rollbackAndClose();
                  return "duplicate";
                }
                payment.setEfinMofchequeno(json.get("chequeNo").toString());
              } else {
                payment.setEfinMofchequeno(null);
              }
            } else {
              payment.setEfinMofchequeno(payment.getEfinMofchequenoTmp());
            }

            if (json.has("chequeDate")) {
              if (StringUtils.isNotEmpty(json.get("chequeDate").toString())) {
                Date chequeDate = dateYearFormat
                    .parse(UtilityDAO.convertToGregorian(json.get("chequeDate").toString()));
                // chk future
                if (chequeDate.compareTo(new Date()) > 0) {
                  return "futureDate" + payment.getDocumentNo();
                }
                payment.setEfinMofchequedate(chequeDate);
              } else {
                payment.setEfinMofchequedate(null);
              }
            } else {
              payment.setEfinMofchequedate(payment.getEfinMofchequedateTmp());
            }

            if (json.has("bank")) {
              payment.setEfinMofbankname(json.get("bank").toString());
            } else {
              payment.setEfinMofbankname(payment.getEfinMofbanknameTmp());
            }

            if (json.has("sentDate")) {
              if (StringUtils.isNotEmpty(json.get("sentDate").toString())) {
                Date sentBankDate = dateYearFormat
                    .parse(UtilityDAO.convertToGregorian(json.get("sentDate").toString()));
                // chk future
                if (sentBankDate.compareTo(new Date()) > 0) {
                  return "futureDate" + payment.getDocumentNo();
                }
                payment.setEfinBanksentdate(sentBankDate);
              } else {
                payment.setEfinBanksentdate(null);
              }
            } else {
              payment.setEfinBanksentdate(payment.getEfinBanksentdateTmp());
            }

            if (json.has("receiveDate")) {
              if (StringUtils.isNotEmpty(json.get("receiveDate").toString())) {
                Date receiveDate = dateYearFormat
                    .parse(UtilityDAO.convertToGregorian(json.get("receiveDate").toString()));
                // chk future
                if (receiveDate.compareTo(new Date()) > 0) {
                  return "futureDate" + payment.getDocumentNo();
                }
                payment.setEfinReceiveChequeDate(receiveDate);
              } else {
                payment.setEfinReceiveChequeDate(null);
              }
            } else {
              payment.setEfinReceiveChequeDate(payment.getEfinRecChequeDateTmp());
            }

            if (json.has("ChqStatus")) {
              payment.setEfinMofchqstatus(json.get("ChqStatus").toString());
            } else {
              payment.setEfinMofchqstatus(payment.getEfinMofchqstatusTmp());
            }
          }
          OBDal.getInstance().save(payment);
        }
      } else {
        return "noSelect";
      }
      return "true";
    } catch (Exception e) {
      log4j.error("Exception in savePaymentDetails :", e);
      OBDal.getInstance().rollbackAndClose();
      return "false";
    }
  }

  /**
   * This method is used to lock/unlock payment out.
   * 
   * @param paymentListjson
   * @param decision
   * @return
   */
  public String lockUnlockPaymentDetails(JSONObject paymentListjson, String decision) {
    FIN_Payment payment = null;
    String result = "true";
    try {
      JSONArray paymentArray = paymentListjson.getJSONArray("List");
      if (paymentArray.length() > 0) {
        for (int i = 0; i < paymentArray.length(); i++) {
          JSONObject json = paymentArray.getJSONObject(i);
          if (json.has("id")) {
            payment = OBDal.getInstance().get(FIN_Payment.class, json.get("id"));
          }

          if (((StringUtils.isEmpty(payment.getEfinMofbankname())
              && StringUtils.isEmpty(payment.getEfinMofbanknameTmp()))
              || (StringUtils.isNotEmpty(payment.getEfinMofbankname())
                  && StringUtils.isNotEmpty(payment.getEfinMofbanknameTmp())
                  && payment.getEfinMofbankname().equals(payment.getEfinMofbanknameTmp())))
              && ((StringUtils.isEmpty(payment.getEfinMofchequeno())
                  && StringUtils.isEmpty(payment.getEfinMofchequenoTmp()))
                  || (StringUtils.isNotEmpty(payment.getEfinMofchequeno())
                      && StringUtils.isNotEmpty(payment.getEfinMofchequenoTmp())
                      && payment.getEfinMofchequeno().equals(payment.getEfinMofchequenoTmp())))
              && ((StringUtils.isEmpty(payment.getEfinMofchqstatus())
                  && StringUtils.isEmpty(payment.getEfinMofchqstatusTmp()))
                  || (StringUtils.isNotEmpty(payment.getEfinMofchqstatus())
                      && StringUtils.isNotEmpty(payment.getEfinMofchqstatusTmp())
                      && payment.getEfinMofchqstatus().equals(payment.getEfinMofchqstatusTmp())))
              && ((payment.getEfinMofchequedate() == null
                  && payment.getEfinMofchequedateTmp() == null)
                  || (payment.getEfinMofchequedate() != null
                      && payment.getEfinMofchequedateTmp() != null
                      && payment.getEfinMofchequedate()
                          .compareTo(payment.getEfinMofchequedateTmp()) == 0))
              && ((payment.getEfinBanksentdate() == null
                  && payment.getEfinBanksentdateTmp() == null)
                  || (payment.getEfinBanksentdate() != null
                      && payment.getEfinBanksentdateTmp() != null
                      && payment.getEfinBanksentdate()
                          .compareTo(payment.getEfinBanksentdateTmp()) == 0))
              && ((payment.getEfinReceiveChequeDate() == null
                  && payment.getEfinRecChequeDateTmp() == null)
                  || (payment.getEfinReceiveChequeDate() != null
                      && payment.getEfinRecChequeDateTmp() != null
                      && payment.getEfinReceiveChequeDate()
                          .compareTo(payment.getEfinRecChequeDateTmp()) == 0))) {

            if (decision.equals("lock")) {
              payment.setEfinIslocked(true);
            } else if (decision.equals("unlock")) {
              payment.setEfinIslocked(false);
            }
          } else {
            result = "notsaved";
            OBDal.getInstance().rollbackAndClose();
            break;
          }

          // mandatory validation.
          if (decision.equals("lock")) {
            if (StringUtils.isEmpty(payment.getEfinMofchequenoTmp())
                || StringUtils.isEmpty(payment.getEfinMofchqstatusTmp())
                || payment.getEfinMofchequedateTmp() == null) {
              result = "fieldMandatory";
              if (StringUtils.isNotEmpty(payment.getEfinPaymentsequence())) {
                result = "fieldMandatory" + payment.getEfinPaymentsequence();
              }

              OBDal.getInstance().rollbackAndClose();
              break;
            }
          }
          OBDal.getInstance().save(payment);
        }
      } else {
        result = "noSelect";
      }
    } catch (Exception e) {
      log4j.error("Exception in lockUnlockPaymentDetails :", e);
      OBDal.getInstance().rollbackAndClose();
      return "error";
    }
    return result;
  }

  /**
   * This method is used to apply sequence
   * 
   * @param vo
   * @return
   */
  @SuppressWarnings({ "unchecked" })
  public String applySequenceRange(ClearChequeVO vo) {
    StringBuilder whereClause = new StringBuilder();
    List<String> paymentList = null;
    BigDecimal startSeq, endSeq, seqLength;
    String popBank = "", chqDate = "", sentBankDate = "", paymentDateFrom = "",
        paymentDateTo = "", result = "", reconcileDate = "", popChqStatus = "";
    Date chequeDate = null;
    Date bankSendDate = null;
    Date receiveChequeDate = null;
    SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");

    try {

      // getting popup parameter
      if (vo.getStartSequence() != null) {
        // BigDecimal m = new BigDecimal(vo.getStartSequence());
        if (!vo.getStartSequence().matches("[0-9]+")) {
          return "invalidSeq";
        } else {
          startSeq = new BigDecimal(vo.getStartSequence());
        }
      } else {
        startSeq = BigDecimal.ZERO;
      }
      if (vo.getEndSequence() != null) {
        if (!vo.getEndSequence().matches("[0-9]+")) {
          return "invalidSeq";
        } else {
          endSeq = new BigDecimal(vo.getEndSequence());
        }
      } else {
        endSeq = BigDecimal.ZERO;
      }

      seqLength = (endSeq.subtract(startSeq)).add(BigDecimal.ONE);

      if (!StringUtils.isEmpty(vo.getPopClearBankChqDate())) {
        chequeDate = dateYearFormat
            .parse(UtilityDAO.convertToGregorian(vo.getPopClearBankChqDate()));
      }
      if (!StringUtils.isEmpty(vo.getPopclearingBank())) {
        popBank = vo.getPopclearingBank();
      }

      if (!StringUtils.isEmpty(vo.getPopChequeStatus())) {
        popChqStatus = vo.getPopChequeStatus();
      }

      if (!StringUtils.isEmpty(vo.getPopBankSentDate())) {
        bankSendDate = dateYearFormat.parse(UtilityDAO.convertToGregorian(vo.getPopBankSentDate()));
      }
      if (!StringUtils.isEmpty(vo.getPopChequeReceiveDate())) {
        receiveChequeDate = dateYearFormat
            .parse(UtilityDAO.convertToGregorian(vo.getPopChequeReceiveDate()));
      }

      // check nothing to apply.
      if (startSeq.compareTo(BigDecimal.ZERO) == 0 && endSeq.compareTo(BigDecimal.ZERO) == 0
          && chequeDate == null && bankSendDate == null && receiveChequeDate == null
          && StringUtils.isEmpty(popBank) && StringUtils.isEmpty(popChqStatus)) {
        result = "NoChanges";
        return result;
      }

      // check future date
      if ((chequeDate != null && chequeDate.compareTo(new Date()) > 0)
          || (bankSendDate != null && bankSendDate.compareTo(new Date()) > 0)
          || (receiveChequeDate != null && receiveChequeDate.compareTo(new Date()) > 0)) {
        result = "futureDate";
        return result;
      }
      // chk sequence is valid
      if ((startSeq.compareTo(BigDecimal.ZERO) == 0 && endSeq.compareTo(BigDecimal.ZERO) > 0)
          || (startSeq.compareTo(BigDecimal.ZERO) > 0 && endSeq.compareTo(BigDecimal.ZERO) == 0)
          || endSeq.compareTo(startSeq) < 0) {
        result = "SeqNotEnough";
        return result;
      }

      // appending where clause
      whereClause.append(" pay.isreceipt='N' and pay.status <>'EFIN_CAN' and ");

      if (!StringUtils.isEmpty(vo.getOrganization())) {
        whereClause.append(" pay.ad_org_id=:orgId and");
      }
      if (!StringUtils.isEmpty(vo.getPaymentMethod())) {
        whereClause.append(" pay.Fin_Paymentmethod_ID=:paymentMethodId and");
      }
      if (!StringUtils.isEmpty(vo.getBank())) {
        whereClause.append(" fin.em_Efin_Bank_id=:bank and");
      }
      if (!StringUtils.isEmpty(vo.getBankAccNo())) {
        whereClause.append("  fin.EM_Efin_Account_ID=:bankAccountNo and");
      }
      if (!StringUtils.isEmpty(vo.getPaymentDocSeqNoFrom())) {
        whereClause.append(" pay.em_efin_paymentsequence>=:paymentDocSeqNoFrom and");
      }
      if (!StringUtils.isEmpty(vo.getPaymentDocSeqNoTo())) {
        whereClause.append(" pay.em_efin_paymentsequence<=:paymentDocSeqNoTo and");
      }
      if (!StringUtils.isEmpty(vo.getClearBankChqNo())) {
        whereClause.append(" pay.EM_Efin_Mofchequeno=:clearBankChqNo and");
      }
      if (!StringUtils.isEmpty(vo.getClearBankChqDate())) {
        chqDate = dateYearFormat
            .format(dateYearFormat.parse(UtilityDAO.convertToGregorian(vo.getClearBankChqDate())));
        whereClause
            .append(" (cast(:chqDate as date) = cast(pay.EM_Efin_Mofchequedate as date)) and");
      }
      if (!StringUtils.isEmpty(vo.getClearingBank())) {
        whereClause.append(" pay.EM_Efin_Mofbankname=:clearingBank and");
      }
      if (!StringUtils.isEmpty(vo.getSentBankDate())) {
        sentBankDate = dateYearFormat
            .format(dateYearFormat.parse(UtilityDAO.convertToGregorian(vo.getSentBankDate())));
        whereClause
            .append(" (cast(:sentBankDate as date) = cast(pay.em_efin_banksentdate as date))  and");
      }

      if ((!StringUtils.isEmpty(vo.getPaymentDateTo())
          && !StringUtils.isEmpty(vo.getPaymentDateFrom()))) {
        paymentDateFrom = dateYearFormat
            .format(dateYearFormat.parse(UtilityDAO.convertToGregorian(vo.getPaymentDateFrom())));
        paymentDateTo = dateYearFormat
            .format(dateYearFormat.parse(UtilityDAO.convertToGregorian(vo.getPaymentDateTo())));

        whereClause.append(
            " (cast(pay.Paymentdate as date) between cast(:paymentDateFrom as date) and cast(:paymentDateTo as date)) and ");

      }

      if (!StringUtils.isEmpty(vo.getReconcileNo())) {
        whereClause.append(" acc.em_efin_acctseq=:reconcileNo and");
      }
      if (!StringUtils.isEmpty(vo.getReconcileDate())) {
        reconcileDate = dateYearFormat
            .format(dateYearFormat.parse(UtilityDAO.convertToGregorian(vo.getReconcileDate())));
        whereClause
            .append(" (cast(:reconcileDate as date) = cast (rec.statementdate as date)) and ");
      }
      if (!StringUtils.isEmpty(vo.getChequeStatus())) {
        whereClause.append(" pay.em_efin_mofchqstatus=:chqStatus and");
      }

      // int index = whereClause.lastIndexOf("and");
      // whereClause.replace(index, 3 + index, "");
      whereClause.append(" em_efin_islocked = 'N' ");
      whereClause.append(" order by pay.DocumentNo ");

      Query query = null;

      String sqlString = "select pay.FIN_Payment_id as id from FIN_Payment pay "
          + " left join FIN_Financial_Account fin on pay.FIN_Financial_Account_id = fin.FIN_Financial_Account_id "
          + " left join fin_finacc_transaction tr on tr.FIN_Financial_Account_id = fin.FIN_Financial_Account_id and pay.FIN_Payment_id = tr.fin_payment_id "
          + " left join fin_reconciliation rec on rec.fin_reconciliation_id = tr.fin_reconciliation_id "
          + " left join (select distinct em_efin_acctseq,em_efin_documentno,record_id from fact_acct) acc on "
          + " rec.fin_reconciliation_id = acc.record_id where " + whereClause.toString();

      query = OBDal.getInstance().getSession().createSQLQuery(sqlString);

      if (!StringUtils.isEmpty(vo.getOrganization())) {
        query.setParameter("orgId", vo.getOrganization());
      }
      if (!StringUtils.isEmpty(vo.getPaymentMethod())) {
        query.setParameter("paymentMethodId", vo.getPaymentMethod());
      }
      if (!StringUtils.isEmpty(vo.getBank())) {
        query.setParameter("bank", vo.getBank());
      }
      if (!StringUtils.isEmpty(vo.getBankAccNo())) {
        query.setParameter("bankAccountNo", vo.getBankAccNo());
      }
      if (!StringUtils.isEmpty(vo.getPaymentDocSeqNoFrom())) {
        query.setParameter("paymentDocSeqNoFrom", vo.getPaymentDocSeqNoFrom());
      }
      if (!StringUtils.isEmpty(vo.getPaymentDocSeqNoTo())) {
        query.setParameter("paymentDocSeqNoTo", vo.getPaymentDocSeqNoTo());
      }
      if (!StringUtils.isEmpty(vo.getClearBankChqNo())) {
        query.setParameter("clearBankChqNo", vo.getClearBankChqNo());
      }
      if (!StringUtils.isEmpty(vo.getClearBankChqDate())) {
        query.setParameter("chqDate", chqDate);
      }
      if (!StringUtils.isEmpty(vo.getClearingBank())) {
        query.setParameter("clearingBank", vo.getClearingBank());
      }
      if (!StringUtils.isEmpty(vo.getSentBankDate())) {
        query.setParameter("sentBankDate", sentBankDate);
      }
      if (!StringUtils.isEmpty(vo.getPaymentDateFrom())) {
        query.setParameter("paymentDateFrom", paymentDateFrom);
      }
      if (!StringUtils.isEmpty(vo.getPaymentDateTo())) {
        query.setParameter("paymentDateTo", paymentDateTo);
      }
      if (!StringUtils.isEmpty(vo.getReconcileNo())) {
        query.setParameter("reconcileNo", vo.getReconcileNo());
      }
      if (!StringUtils.isEmpty(vo.getReconcileDate())) {
        query.setParameter("reconcileDate", reconcileDate);
      }
      if (!StringUtils.isEmpty(vo.getChequeStatus())) {
        query.setParameter("chqStatus", vo.getChequeStatus());
      }

      paymentList = query.list();
      // start validation
      // List<String> selectIds = new ArrayList<>();

      // if (paymentList.size() > 0) {
      // for (String id : paymentList) {
      // // Object[] row = (Object[]) con;
      // selectIds.add(id);
      // }
      // }

      // chk seq is duplicated.
      List<FIN_Payment> duplicateList = null;
      List<BigDecimal> duplicateChqNo = new ArrayList<>();
      long duplicatesize = 0;
      if (startSeq.compareTo(BigDecimal.ZERO) > 0 && endSeq.compareTo(BigDecimal.ZERO) > 0) {
        OBQuery<FIN_Payment> chkDuplicate = OBDal.getInstance().createQuery(FIN_Payment.class,
            "as e join e.account acct where e.receipt='N' and acct.efinAccount.id = :acct and to_number(e.efinMofchequeno) >=:startSeq and to_number(e.efinMofchequeno) <=:endSeq and e.id not in(:selectedId)");
        chkDuplicate.setNamedParameter("startSeq", startSeq);
        chkDuplicate.setNamedParameter("endSeq", endSeq);
        chkDuplicate.setNamedParameter("selectedId", paymentList);
        chkDuplicate.setNamedParameter("acct", vo.getBankAccNo());
        duplicateList = chkDuplicate.list();
        if (duplicateList.size() > 0) {
          duplicatesize = duplicateList.size();
          for (FIN_Payment payout : duplicateList) {
            duplicateChqNo.add(new BigDecimal(payout.getEfinMofchequeno()));
          }
        }
      }
      seqLength = seqLength.subtract(new BigDecimal(duplicatesize));
      // if (seqLength < 1) {
      // result = "NoSeq";
      // return result;
      // }

      if (startSeq.compareTo(BigDecimal.ZERO) > 0 && endSeq.compareTo(BigDecimal.ZERO) > 0
          && paymentList.size() > seqLength.longValue() && duplicatesize > 0) {
        result = "SeqNotEnoughDuplicate";
        return result;
      }

      if (startSeq.compareTo(BigDecimal.ZERO) > 0 && endSeq.compareTo(BigDecimal.ZERO) > 0
          && paymentList.size() > seqLength.longValue()) {
        result = "SeqNotEnough";
        return result;
      }

      if (paymentList.size() > 0) {
        for (String id : paymentList) {

          if (startSeq.compareTo(BigDecimal.ZERO) > 0 && endSeq.compareTo(BigDecimal.ZERO) > 0) {
            for (BigDecimal i = startSeq; startSeq.compareTo(endSeq) <= 1; i.add(BigDecimal.ONE)) {
              if (!duplicateChqNo.contains(startSeq)) {
                break;
              }
              startSeq = startSeq.add(BigDecimal.ONE);
            }
          }

          FIN_Payment paymentOut = OBDal.getInstance().get(FIN_Payment.class, id);
          if (startSeq.compareTo(BigDecimal.ZERO) > 0 && endSeq.compareTo(BigDecimal.ZERO) > 0) {
            paymentOut.setEfinMofchequenoTmp(startSeq.toString());
            startSeq = startSeq.add(BigDecimal.ONE);
          }
          if (chequeDate != null)
            paymentOut.setEfinMofchequedateTmp(chequeDate);
          if (StringUtils.isNotEmpty(popBank))
            paymentOut.setEfinMofbanknameTmp(popBank);
          if (bankSendDate != null)
            paymentOut.setEfinBanksentdateTmp(bankSendDate);
          if (receiveChequeDate != null)
            paymentOut.setEfinRecChequeDateTmp(receiveChequeDate);
          if (StringUtils.isNotEmpty(popChqStatus))
            paymentOut.setEfinMofchqstatusTmp(popChqStatus);
          OBDal.getInstance().save(paymentOut);
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in applySequenceRange :", e);
      OBDal.getInstance().rollbackAndClose();
      return "error";
    }
    return "true";
  }

  @SuppressWarnings("unchecked")
  /**
   * This method is used to save all applied sequences.
   * 
   * @param vo
   * @param paymentListjson
   * @return
   */
  public String saveAll(ClearChequeVO vo, JSONObject paymentListjson) {
    StringBuilder whereClause = new StringBuilder();
    List<Object> paymentList = null;
    String chqDate = "", sentBankDate = "", paymentDateFrom = "", paymentDateTo = "",
        reconcileDate = "";
    Query query = null;
    SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
    StringBuilder selectIds = new StringBuilder();
    boolean isSelected = false;

    try {
      // get selected record.
      JSONArray paymentArray = paymentListjson.getJSONArray("List");
      if (paymentArray.length() > 0) {
        isSelected = true;
        for (int i = 0; i < paymentArray.length(); i++) {
          JSONObject json = paymentArray.getJSONObject(i);
          if (json.has("id")) {
            selectIds.append("'" + json.get("id") + "',");
          }
        }
        int index = selectIds.lastIndexOf(",");
        selectIds.replace(index, 1 + index, "");
      }

      // appending where clause
      whereClause.append(" pay.isreceipt='N' and pay.status <>'EFIN_CAN' and ");

      if (!StringUtils.isEmpty(vo.getOrganization())) {
        whereClause.append(" pay.ad_org_id=:orgId and");
      }
      if (!StringUtils.isEmpty(vo.getPaymentMethod())) {
        whereClause.append(" pay.Fin_Paymentmethod_ID=:paymentMethodId and");
      }
      if (!StringUtils.isEmpty(vo.getBank())) {
        whereClause.append(" fin.em_Efin_Bank_id=:bank and");
      }
      if (!StringUtils.isEmpty(vo.getBankAccNo())) {
        whereClause.append("  fin.EM_Efin_Account_ID=:bankAccountNo and");
      }
      if (!StringUtils.isEmpty(vo.getPaymentDocSeqNoFrom())) {
        whereClause.append(" pay.em_efin_paymentsequence>=:paymentDocSeqNoFrom and");
      }
      if (!StringUtils.isEmpty(vo.getPaymentDocSeqNoTo())) {
        whereClause.append(" pay.em_efin_paymentsequence<=:paymentDocSeqNoTo and");
      }
      if (!StringUtils.isEmpty(vo.getClearBankChqNo())) {
        whereClause.append(" pay.EM_Efin_Mofchequeno=:clearBankChqNo and");
      }
      if (!StringUtils.isEmpty(vo.getClearBankChqDate())) {
        chqDate = dateYearFormat
            .format(dateYearFormat.parse(UtilityDAO.convertToGregorian(vo.getClearBankChqDate())));
        whereClause
            .append(" (cast(:chqDate as date) = cast(pay.EM_Efin_Mofchequedate as date)) and");
      }
      if (!StringUtils.isEmpty(vo.getClearingBank())) {
        whereClause.append(" pay.EM_Efin_Mofbankname=:clearingBank and");
      }
      if (!StringUtils.isEmpty(vo.getSentBankDate())) {
        sentBankDate = dateYearFormat
            .format(dateYearFormat.parse(UtilityDAO.convertToGregorian(vo.getSentBankDate())));
        whereClause
            .append(" (cast(:sentBankDate as date) = cast(pay.em_efin_banksentdate as date))  and");
      }

      if ((!StringUtils.isEmpty(vo.getPaymentDateTo())
          && !StringUtils.isEmpty(vo.getPaymentDateFrom()))) {
        paymentDateFrom = dateYearFormat
            .format(dateYearFormat.parse(UtilityDAO.convertToGregorian(vo.getPaymentDateFrom())));
        paymentDateTo = dateYearFormat
            .format(dateYearFormat.parse(UtilityDAO.convertToGregorian(vo.getPaymentDateTo())));

        whereClause.append(
            " (cast(pay.Paymentdate as date) between cast(:paymentDateFrom as date) and cast(:paymentDateTo as date)) and ");

      }

      if (!StringUtils.isEmpty(vo.getReconcileNo())) {
        whereClause.append(" acc.em_efin_acctseq=:reconcileNo and");
      }
      if (!StringUtils.isEmpty(vo.getReconcileDate())) {
        reconcileDate = dateYearFormat
            .format(dateYearFormat.parse(UtilityDAO.convertToGregorian(vo.getReconcileDate())));
        whereClause
            .append(" (cast(:reconcileDate as date) = cast (rec.statementdate as date)) and ");
      }

      if (!StringUtils.isEmpty(vo.getLockIndicator())) {
        whereClause.append(" pay.em_efin_islocked=:islock and");
      }

      if (isSelected) {
        whereClause.append(" pay.FIN_Payment_id not in (:selectId) ");
      } else {
        int index = whereClause.lastIndexOf("and");
        whereClause.replace(index, 3 + index, "");
      }
      whereClause.append(" order by pay.DocumentNo ");
      String sqlString = "select pay.FIN_Payment_id as id from FIN_Payment pay "
          + " left join FIN_Financial_Account fin on pay.FIN_Financial_Account_id = fin.FIN_Financial_Account_id "
          + " left join Efin_Bank bank on fin.em_Efin_Bank_id = bank.Efin_Bank_id  "
          + " left join fin_finacc_transaction tr on tr.FIN_Financial_Account_id = fin.FIN_Financial_Account_id and pay.FIN_Payment_id = tr.fin_payment_id "
          + " left join fin_reconciliation rec on rec.fin_reconciliation_id = tr.fin_reconciliation_id "
          + " left join (select distinct em_efin_acctseq,em_efin_documentno,record_id from fact_acct) acc on "
          + " rec.fin_reconciliation_id = acc.record_id where " + whereClause.toString();

      query = OBDal.getInstance().getSession().createSQLQuery(sqlString);

      if (!StringUtils.isEmpty(vo.getOrganization())) {
        query.setParameter("orgId", vo.getOrganization());
      }
      if (!StringUtils.isEmpty(vo.getPaymentMethod())) {
        query.setParameter("paymentMethodId", vo.getPaymentMethod());
      }
      if (!StringUtils.isEmpty(vo.getBank())) {
        query.setParameter("bank", vo.getBank());
      }
      if (!StringUtils.isEmpty(vo.getBankAccNo())) {
        query.setParameter("bankAccountNo", vo.getBankAccNo());
      }
      if (!StringUtils.isEmpty(vo.getPaymentDocSeqNoFrom())) {
        query.setParameter("paymentDocSeqNoFrom", vo.getPaymentDocSeqNoFrom());
      }
      if (!StringUtils.isEmpty(vo.getPaymentDocSeqNoTo())) {
        query.setParameter("paymentDocSeqNoTo", vo.getPaymentDocSeqNoTo());
      }
      if (!StringUtils.isEmpty(vo.getClearBankChqNo())) {
        query.setParameter("clearBankChqNo", vo.getClearBankChqNo());
      }
      if (!StringUtils.isEmpty(vo.getClearBankChqDate())) {
        query.setParameter("chqDate", chqDate);
      }
      if (!StringUtils.isEmpty(vo.getClearingBank())) {
        query.setParameter("clearingBank", vo.getClearingBank());
      }
      if (!StringUtils.isEmpty(vo.getSentBankDate())) {
        query.setParameter("sentBankDate", sentBankDate);
      }
      if (!StringUtils.isEmpty(vo.getPaymentDateFrom())) {
        query.setParameter("paymentDateFrom", paymentDateFrom);
      }
      if (!StringUtils.isEmpty(vo.getPaymentDateTo())) {
        query.setParameter("paymentDateTo", paymentDateTo);
      }
      if (!StringUtils.isEmpty(vo.getReconcileNo())) {
        query.setParameter("reconcileNo", vo.getReconcileNo());
      }
      if (!StringUtils.isEmpty(vo.getReconcileDate())) {
        query.setParameter("reconcileDate", reconcileDate);
      }
      if (!StringUtils.isEmpty(vo.getLockIndicator())) {
        query.setParameter("islock", vo.getLockIndicator());
      }
      if (isSelected) {
        query.setParameter("selectId", selectIds.toString());
      }

      paymentList = query.list();
      if (paymentList.size() > 0) {
        for (Object id : paymentList) {
          // Object[] row = (Object[]) con;

          FIN_Payment paymentOut = OBDal.getInstance().get(FIN_Payment.class, id.toString());
          if (!paymentOut.isEfinIslocked()) {
            paymentOut.setEfinMofbankname(paymentOut.getEfinMofbanknameTmp());
            paymentOut.setEfinMofchequeno(paymentOut.getEfinMofchequenoTmp());
            paymentOut.setEfinMofchequedate(paymentOut.getEfinMofchequedateTmp());
            paymentOut.setEfinBanksentdate(paymentOut.getEfinBanksentdateTmp());
            paymentOut.setEfinReceiveChequeDate(paymentOut.getEfinRecChequeDateTmp());
            OBDal.getInstance().save(paymentOut);
          }
        }
      }

      // check selected edit records are duplicate chq
      if (paymentArray.length() > 0) {
        for (int i = 0; i < paymentArray.length(); i++) {
          FIN_Payment payment = null;
          JSONObject json = paymentArray.getJSONObject(i);
          if (json.has("id")) {
            payment = OBDal.getInstance().get(FIN_Payment.class, json.get("id"));
          }
          if (!payment.isEfinIslocked()) {
            // trigger will make impact in actual columns.
            if (json.has("chequeNo")) {
              if (StringUtils.isNotEmpty(json.get("chequeNo").toString())) {
                // check edited value may be duplicated of appy seq range or in other than fitered
                // apply seq.
                OBQuery<FIN_Payment> chkDuplicate = OBDal.getInstance().createQuery(
                    FIN_Payment.class,
                    "as e join e.account acct where e.receipt='N' and acct.efinAccount.id = :acct and (e.efinMofchequenoTmp =:sequence or e.efinMofchequeno =:sequence) and e.id not in (:id)");
                chkDuplicate.setNamedParameter("sequence", json.get("chequeNo").toString());
                chkDuplicate.setNamedParameter("id", json.get("id").toString());
                chkDuplicate.setNamedParameter("acct", vo.getBankAccNo());
                if (chkDuplicate.list().size() > 0) {
                  OBDal.getInstance().rollbackAndClose();
                  return "duplicate";
                }
                payment.setEfinMofchequeno(json.get("chequeNo").toString());
              } else {
                payment.setEfinMofchequeno(null);
              }
            } else {
              payment.setEfinMofchequeno(payment.getEfinMofchequenoTmp());
            }
            if (json.has("chequeDate")) {
              if (StringUtils.isNotEmpty(json.get("chequeDate").toString())) {
                Date chequeDate = dateYearFormat
                    .parse(UtilityDAO.convertToGregorian(json.get("chequeDate").toString()));
                payment.setEfinMofchequedate(chequeDate);
              } else {
                payment.setEfinMofchequedate(null);
              }
            } else {
              payment.setEfinMofchequedate(payment.getEfinMofchequedateTmp());
            }
            if (json.has("bank")) {
              payment.setEfinMofbankname(json.get("bank").toString());
            } else {
              payment.setEfinMofbankname(payment.getEfinMofbanknameTmp());
            }
          }
          if (json.has("sentDate")) {
            if (StringUtils.isNotEmpty(json.get("sentDate").toString())) {
              Date BankDate = dateYearFormat
                  .parse(UtilityDAO.convertToGregorian(json.get("sentDate").toString()));
              payment.setEfinBanksentdate(BankDate);
            } else {
              payment.setEfinBanksentdate(null);
            }
          } else {
            payment.setEfinBanksentdate(payment.getEfinBanksentdateTmp());
          }
          if (json.has("receiveDate")) {
            if (StringUtils.isNotEmpty(json.get("receiveDate").toString())) {
              Date receiveDate = dateYearFormat
                  .parse(UtilityDAO.convertToGregorian(json.get("receiveDate").toString()));
              payment.setEfinReceiveChequeDate(receiveDate);
            } else {
              payment.setEfinReceiveChequeDate(null);
            }
          } else {
            payment.setEfinReceiveChequeDate(payment.getEfinRecChequeDateTmp());
          }
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in saveAll :", e);
      OBDal.getInstance().rollbackAndClose();
      return "error";
    }
    return "true";
  }

  @SuppressWarnings("unchecked")
  /**
   * This method is used to move values from temp column to actual column
   * 
   * @param vo
   * @return
   */
  public String clearTempColumns(ClearChequeVO vo) {
    StringBuilder whereClause = new StringBuilder();
    List<String> paymentList = null;
    String chqDate = "", sentBankDate = "", paymentDateFrom = "", paymentDateTo = "",
        reconcileDate = "";
    SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
    Query query = null;

    try {

      // appending where clause
      whereClause.append(" pay.isreceipt='N' and pay.status <>'EFIN_CAN' and ");

      if (!StringUtils.isEmpty(vo.getOrganization())) {
        whereClause.append(" pay.ad_org_id=:orgId and");
      }
      if (!StringUtils.isEmpty(vo.getPaymentMethod())) {
        whereClause.append(" pay.Fin_Paymentmethod_ID=:paymentMethodId and");
      }
      if (!StringUtils.isEmpty(vo.getBank())) {
        whereClause.append(" fin.em_Efin_Bank_id=:bank and");
      }
      if (!StringUtils.isEmpty(vo.getBankAccNo())) {
        whereClause.append("  fin.EM_Efin_Account_ID=:bankAccountNo and");
      }
      if (!StringUtils.isEmpty(vo.getPaymentDocSeqNoFrom())) {
        whereClause.append(" pay.em_efin_paymentsequence>=:paymentDocSeqNoFrom and");
      }
      if (!StringUtils.isEmpty(vo.getPaymentDocSeqNoTo())) {
        whereClause.append(" pay.em_efin_paymentsequence<=:paymentDocSeqNoTo and");
      }
      if (!StringUtils.isEmpty(vo.getClearBankChqNo())) {
        whereClause.append(" pay.EM_Efin_Mofchequeno=:clearBankChqNo and");
      }
      if (!StringUtils.isEmpty(vo.getClearBankChqDate())) {
        chqDate = dateYearFormat
            .format(dateYearFormat.parse(UtilityDAO.convertToGregorian(vo.getClearBankChqDate())));
        whereClause
            .append(" (cast(:chqDate as date) = cast(pay.EM_Efin_Mofchequedate as date)) and");
      }
      if (!StringUtils.isEmpty(vo.getClearingBank())) {
        whereClause.append(" pay.EM_Efin_Mofbankname=:clearingBank and");
      }
      if (!StringUtils.isEmpty(vo.getSentBankDate())) {
        sentBankDate = dateYearFormat
            .format(dateYearFormat.parse(UtilityDAO.convertToGregorian(vo.getSentBankDate())));
        whereClause
            .append(" (cast(:sentBankDate as date) = cast(pay.em_efin_banksentdate as date))  and");
      }

      if ((!StringUtils.isEmpty(vo.getPaymentDateTo())
          && !StringUtils.isEmpty(vo.getPaymentDateFrom()))) {
        paymentDateFrom = dateYearFormat
            .format(dateYearFormat.parse(UtilityDAO.convertToGregorian(vo.getPaymentDateFrom())));
        paymentDateTo = dateYearFormat
            .format(dateYearFormat.parse(UtilityDAO.convertToGregorian(vo.getPaymentDateTo())));

        whereClause.append(
            " (cast(pay.Paymentdate as date) between cast(:paymentDateFrom as date) and cast(:paymentDateTo as date)) and ");

      }

      if (!StringUtils.isEmpty(vo.getReconcileNo())) {
        whereClause.append(" acc.em_efin_acctseq=:reconcileNo and");
      }
      if (!StringUtils.isEmpty(vo.getReconcileDate())) {
        reconcileDate = dateYearFormat
            .format(dateYearFormat.parse(UtilityDAO.convertToGregorian(vo.getReconcileDate())));
        whereClause
            .append(" (cast(:reconcileDate as date) = cast (rec.statementdate as date)) and ");
      }
      if (!StringUtils.isEmpty(vo.getChequeStatus())) {
        whereClause.append(" pay.em_efin_mofchqstatus=:chqStatus and ");
      }

      // int index = whereClause.lastIndexOf("and");
      // whereClause.replace(index, 3 + index, "");
      // whereClause.append(" order by e.documentNo ");
      whereClause.append(" em_efin_islocked = 'N' ");

      String sqlString = "select pay.FIN_Payment_id as id " + " from FIN_Payment pay "
          + " left join FIN_Financial_Account fin on pay.FIN_Financial_Account_id = fin.FIN_Financial_Account_id "
          + " left join Efin_Bank bank on fin.em_Efin_Bank_id = bank.Efin_Bank_id  "
          + " left join fin_finacc_transaction tr on tr.FIN_Financial_Account_id = fin.FIN_Financial_Account_id and pay.FIN_Payment_id = tr.fin_payment_id "
          + " left join fin_reconciliation rec on rec.fin_reconciliation_id = tr.fin_reconciliation_id "
          + " left join (select distinct em_efin_acctseq,em_efin_documentno,record_id from fact_acct) acc on "
          + " rec.fin_reconciliation_id = acc.record_id where " + whereClause.toString();

      query = OBDal.getInstance().getSession().createSQLQuery(sqlString);

      if (!StringUtils.isEmpty(vo.getOrganization())) {
        query.setParameter("orgId", vo.getOrganization());
      }
      if (!StringUtils.isEmpty(vo.getPaymentMethod())) {
        query.setParameter("paymentMethodId", vo.getPaymentMethod());
      }
      if (!StringUtils.isEmpty(vo.getBank())) {
        query.setParameter("bank", vo.getBank());
      }
      if (!StringUtils.isEmpty(vo.getBankAccNo())) {
        query.setParameter("bankAccountNo", vo.getBankAccNo());
      }
      if (!StringUtils.isEmpty(vo.getPaymentDocSeqNoFrom())) {
        query.setParameter("paymentDocSeqNoFrom", vo.getPaymentDocSeqNoFrom());
      }
      if (!StringUtils.isEmpty(vo.getPaymentDocSeqNoTo())) {
        query.setParameter("paymentDocSeqNoTo", vo.getPaymentDocSeqNoTo());
      }
      if (!StringUtils.isEmpty(vo.getClearBankChqNo())) {
        query.setParameter("clearBankChqNo", vo.getClearBankChqNo());
      }
      if (!StringUtils.isEmpty(vo.getClearBankChqDate())) {
        query.setParameter("chqDate", chqDate);
      }
      if (!StringUtils.isEmpty(vo.getClearingBank())) {
        query.setParameter("clearingBank", vo.getClearingBank());
      }
      if (!StringUtils.isEmpty(vo.getSentBankDate())) {
        query.setParameter("sentBankDate", sentBankDate);
      }
      if (!StringUtils.isEmpty(vo.getPaymentDateFrom())) {
        query.setParameter("paymentDateFrom", paymentDateFrom);
      }
      if (!StringUtils.isEmpty(vo.getPaymentDateTo())) {
        query.setParameter("paymentDateTo", paymentDateTo);
      }
      if (!StringUtils.isEmpty(vo.getReconcileNo())) {
        query.setParameter("reconcileNo", vo.getReconcileNo());
      }
      if (!StringUtils.isEmpty(vo.getReconcileDate())) {
        query.setParameter("reconcileDate", reconcileDate);
      }
      if (!StringUtils.isEmpty(vo.getChequeStatus())) {
        query.setParameter("chqStatus", vo.getChequeStatus());
      }

      paymentList = query.list();
      boolean isChange = false;
      if (paymentList.size() > 0) {
        for (String id : paymentList) {
          // Object[] row = (Object[]) qryList;
          FIN_Payment paymentOut = OBDal.getInstance().get(FIN_Payment.class, id);
          if ((paymentOut.getEfinMofbankname() == null
              && paymentOut.getEfinMofbanknameTmp() != null)
              || (paymentOut.getEfinMofbankname() != null
                  && paymentOut.getEfinMofbanknameTmp() == null)
              || (paymentOut.getEfinMofbankname() != null
                  && paymentOut.getEfinMofbanknameTmp() != null
                  && !paymentOut.getEfinMofbankname().equals(paymentOut.getEfinMofbanknameTmp()))) {
            paymentOut.setEfinMofbanknameTmp(paymentOut.getEfinMofbankname());
            isChange = true;
          }
          if ((paymentOut.getEfinMofchequeno() == null
              && paymentOut.getEfinMofchequenoTmp() != null)
              || (paymentOut.getEfinMofchequeno() != null
                  && paymentOut.getEfinMofchequenoTmp() == null)
              || (paymentOut.getEfinMofchequeno() != null
                  && paymentOut.getEfinMofchequenoTmp() != null
                  && !paymentOut.getEfinMofchequeno().equals(paymentOut.getEfinMofchequenoTmp()))) {
            paymentOut.setEfinMofchequenoTmp(paymentOut.getEfinMofchequeno());
            isChange = true;
          }
          if ((paymentOut.getEfinMofchqstatus() == null
              && paymentOut.getEfinMofchqstatusTmp() != null)
              || (paymentOut.getEfinMofchqstatus() != null
                  && paymentOut.getEfinMofchqstatusTmp() == null)
              || (paymentOut.getEfinMofchqstatus() != null
                  && paymentOut.getEfinMofchqstatusTmp() != null && !paymentOut
                      .getEfinMofchqstatus().equals(paymentOut.getEfinMofchqstatusTmp()))) {
            paymentOut.setEfinMofchqstatusTmp(paymentOut.getEfinMofchqstatus());
            isChange = true;
          }
          if ((paymentOut.getEfinMofchequedate() == null
              && paymentOut.getEfinMofchequedateTmp() != null)
              || (paymentOut.getEfinMofchequedate() != null
                  && paymentOut.getEfinMofchequedateTmp() == null)
              || (paymentOut.getEfinMofchequedate() != null
                  && paymentOut.getEfinMofchequedateTmp() != null && !paymentOut
                      .getEfinMofchequedate().equals(paymentOut.getEfinMofchequedateTmp()))) {
            paymentOut.setEfinMofchequedateTmp(paymentOut.getEfinMofchequedate());
            isChange = true;
          }
          if ((paymentOut.getEfinBanksentdate() == null
              && paymentOut.getEfinBanksentdateTmp() != null)
              || (paymentOut.getEfinBanksentdate() != null
                  && paymentOut.getEfinBanksentdateTmp() == null)
              || (paymentOut.getEfinBanksentdate() != null
                  && paymentOut.getEfinBanksentdateTmp() != null && !paymentOut
                      .getEfinBanksentdate().equals(paymentOut.getEfinBanksentdateTmp()))) {
            paymentOut.setEfinBanksentdateTmp(paymentOut.getEfinBanksentdate());
            isChange = true;
          }
          if ((paymentOut.getEfinReceiveChequeDate() == null
              && paymentOut.getEfinRecChequeDateTmp() != null)
              || (paymentOut.getEfinReceiveChequeDate() != null
                  && paymentOut.getEfinRecChequeDateTmp() == null)
              || (paymentOut.getEfinReceiveChequeDate() != null
                  && paymentOut.getEfinRecChequeDateTmp() != null && !paymentOut
                      .getEfinReceiveChequeDate().equals(paymentOut.getEfinRecChequeDateTmp()))) {
            paymentOut.setEfinRecChequeDateTmp(paymentOut.getEfinReceiveChequeDate());
            isChange = true;
          }
        }
        if (isChange == false) {
          return "noChange";
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in clearTempColumns :", e);
      OBDal.getInstance().rollbackAndClose();
      return "error";
    }
    return "true";
  }

}