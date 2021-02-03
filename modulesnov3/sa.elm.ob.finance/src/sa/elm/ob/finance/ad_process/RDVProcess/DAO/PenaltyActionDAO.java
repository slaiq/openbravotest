package sa.elm.ob.finance.ad_process.RDVProcess.DAO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.finance.EfinPenalty;
import sa.elm.ob.finance.EfinPenaltyAction;
import sa.elm.ob.finance.EfinPenaltyHeader;
import sa.elm.ob.finance.EfinPenaltyTypes;
import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.ad_process.RDVProcess.AddDefaultPenaltyDAO;
import sa.elm.ob.finance.ad_process.RDVProcess.AddDefaultPenaltyDAOImpl;
import sa.elm.ob.finance.ad_process.RDVProcess.vo.PenaltyActionVO;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

public class PenaltyActionDAO {
  private static Connection conn = null;
  VariablesSecureApp vars = null;
  private static Logger log4j = Logger.getLogger(PenaltyActionDAO.class);

  public PenaltyActionDAO(Connection con) {
    PenaltyActionDAO.conn = con;
  }

  /**
   * This method is used to get salary List
   * 
   * @param clientId
   * @param inpRDVTxnLineId
   * @param searchAttr
   * @return
   */
  public JSONObject getSalaryList(String clientId, String inpRDVTxnLineId, JSONObject searchAttr) {
    PreparedStatement ps = null, ps1 = null;
    ResultSet rs = null, rs1 = null;
    String date = "";
    DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
    SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
    JSONObject result = new JSONObject(), json = null;
    JSONArray jsonArray = new JSONArray();
    StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(),
        fromQuery = new StringBuilder(), whereClause = new StringBuilder(),
        orderClause = new StringBuilder();
    try {
      result.put("page", "0");
      result.put("total", "0");
      result.put("records", "0");
      result.put("rows", jsonArray);

      int offset = 0, totalPage = 0, totalRecord = 0;
      int rows = Integer.parseInt(searchAttr.getString("rows")),
          page = Integer.parseInt(searchAttr.getString("page"));
      if (page == 0)
        page = 1;

      countQuery.append("SELECT count(pc.efin_penalty_action_id) as count ");
      selectQuery.append(
          "select pc.efin_penalty_action_id as id, pc.seqno, pc.trx_app_no,pc.action, pc.Action_Date as actiondate, pc.Amount as amount, ");
      selectQuery.append(
          "pc.efin_penalty_types_id,pentype,pentype.deductiontype ,pc.Penalty_Percentage as penaltypercent, pc.Penalty_Amount as penaltyamt, pc.Action_Reason as actionreason, pc.Action_Justification as actionjustfication, ");
      selectQuery.append(
          "pc.c_bpartner_id, bp.name as bpnametxt, pc.Freeze_Penalty as freezepenalty, pc.c_invoice_id, pc.Amarsarf_Amount as amarsafamount, ");
      selectQuery.append(
          "pc.penalty_account_type, pc.penalty_uniquecode, com.em_efin_uniquecode ,com.em_efin_uniquecodename, (case when pc.c_bpartner_id is not null then  concat(bp.em_efin_documentno,'-',bp.name) else '' end)  as bpname ,inv.documentno,inv.grandtotal , pentype.deductiontype,pc.penalty_rel_id as penaltyrelId,coalesce(orgpen.penalty_amount,0) as orgPenaltyAmt ");

      fromQuery.append(" from efin_penalty_action pc ");
      fromQuery.append(
          "left join c_validcombination com on com.c_validcombination_id = pc.penalty_uniquecode left join efin_penalty_types pentype on pentype.efin_penalty_types_id = pc.efin_penalty_types_id  join eut_deflookups_typeln ls on ls.value=pentype.deductiontype  join eut_deflookups_type typ on typ.eut_deflookups_type_id=ls.eut_deflookups_type_id and typ.value='PENALTY_TYPE'  left join c_bpartner bp on bp.c_bpartner_id = pc.c_bpartner_id ");
      fromQuery.append(
          " left join c_invoice inv on inv.c_invoice_id = pc.c_invoice_id  left join ad_ref_list actiontype on actiontype.value=pc.action and actiontype.ad_reference_id='11538D28FC294B2AB4465B29B0B4BFF3'"
              + " left join ad_ref_list diemtype on diemtype.value=pc.penalty_account_type and diemtype.ad_reference_id='341EEDE3DC20468696320A84BF8049DF' left join efin_penalty_action orgpen on orgpen.efin_penalty_action_id=pc.penalty_rel_id  ");
      whereClause.append(" where pc.ad_client_id = '").append(clientId).append("' ");
      whereClause.append(" and pc.efin_rdvtxnline_id = '").append(inpRDVTxnLineId).append("' ");

      if (searchAttr.has("search") && searchAttr.getString("search").equals("true")) {
        if (searchAttr.has("actiontype")
            && !StringUtils.isEmpty(searchAttr.getString("actiontype")))
          whereClause.append("and actiontype.name ilike '%")
              .append(searchAttr.getString("actiontype")).append("%' ");

        if (searchAttr.has("appno") && !StringUtils.isEmpty(searchAttr.getString("appno")))
          whereClause.append("and pc.trx_app_no ilike '%").append(searchAttr.getString("appno"))
              .append("%' ");
        if (searchAttr.has("Amt") && !StringUtils.isEmpty(searchAttr.getString("Amt"))) {
          if (searchAttr.getString("Amt").equals("0")) {
            whereClause.append("and pc.Amount= ").append(searchAttr.getString("Amt"));

          } else {
            whereClause.append("and CAST(pc.Amount AS text)   ilike '%")
                .append(searchAttr.getString("Amt")).append("%' ");
          }
        }
        if (searchAttr.has("actionDate")
            && !StringUtils.isEmpty(searchAttr.getString("actionDate")))
          whereClause.append("and pc.Action_Date ilike '%")
              .append(searchAttr.getString("actionDate")).append("%' ");
        if (searchAttr.has("penaltytype")
            && !StringUtils.isEmpty(searchAttr.getString("penaltytype")))
          whereClause.append("and ( ls.englishname ilike '%")
              .append(searchAttr.getString("penaltytype")).append("%'  or ls.arabicname ilike  '% ")
              .append(searchAttr.getString("penaltytype")).append("%' )");
        if (searchAttr.has("penaltypercentage")
            && !StringUtils.isEmpty(searchAttr.getString("penaltypercentage"))) {
          if (searchAttr.getString("penaltypercentage").equals("0")) {
            whereClause.append("and pc.Penalty_Percentage= ")
                .append(searchAttr.getString("penaltypercentage"));

          } else {
            whereClause.append("and CAST(pc.Penalty_Percentage AS text) ilike '%")
                .append(searchAttr.getString("penaltypercentage")).append("%' ");
          }
        }
        if (searchAttr.has("penaltyamount")
            && !StringUtils.isEmpty(searchAttr.getString("penaltyamount"))) {
          if (searchAttr.getString("penaltyamount").equals("0")) {
            whereClause.append("and pc.Penalty_Amount= ")
                .append(searchAttr.getString("penaltyamount"));

          } else {
            whereClause.append("and  CAST(pc.Penalty_Amount AS text)  ilike '%")
                .append(searchAttr.getString("penaltyamount")).append("%' ");
          }
        }
        if (searchAttr.has("actionreason")
            && !StringUtils.isEmpty(searchAttr.getString("actionreason")))
          whereClause.append("and pc.Action_Reason ilike '%")
              .append(searchAttr.getString("actionreason")).append("%' ");
        if (searchAttr.has("actionjustfication")
            && !StringUtils.isEmpty(searchAttr.getString("actionjustfication")))
          whereClause.append("and pc.Action_Justification ilike '%")
              .append(searchAttr.getString("actionjustfication")).append("%' ");
        if (searchAttr.has("associatedbp")
            && !StringUtils.isEmpty(searchAttr.getString("associatedbp")))
          whereClause.append("and concat(bp.em_efin_documentno,'-',bp.name) ilike '%")
              .append(searchAttr.getString("associatedbp")).append("%' ");
        if (searchAttr.has("bpname") && !StringUtils.isEmpty(searchAttr.getString("bpname")))
          whereClause.append("and bp.name ilike '%").append(searchAttr.getString("bpname"))
              .append("%' ");
        if (searchAttr.has("freezepenalty")
            && !StringUtils.isEmpty(searchAttr.getString("freezepenalty")))
          whereClause.append("and pc.Freeze_Penalty ilike '%")
              .append(searchAttr.getString("freezepenalty")).append("%' ");
        if (searchAttr.has("amarsarfno")
            && !StringUtils.isEmpty(searchAttr.getString("amarsarfno")))
          whereClause.append("and inv.documentno ilike '%")
              .append(searchAttr.getString("amarsarfno")).append("%' ");
        if (searchAttr.has("amarsarfamount")
            && !StringUtils.isEmpty(searchAttr.getString("amarsarfamount"))) {
          if (searchAttr.getString("amarsarfamount").equals("0")) {
            whereClause.append("and inv.grandtotal= ")
                .append(searchAttr.getString("amarsarfamount"));

          } else {
            whereClause.append("and CAST(inv.grandtotal AS text) ilike '%")
                .append(searchAttr.getString("amarsarfamount")).append("%' ");
          }
        }
        if (searchAttr.has("accounttype")
            && !StringUtils.isEmpty(searchAttr.getString("accounttype")))
          whereClause.append("and diemtype.name ilike '%")
              .append(searchAttr.getString("accounttype")).append("%' ");
        if (searchAttr.has("uniquecode")
            && !StringUtils.isEmpty(searchAttr.getString("uniquecode")))
          whereClause.append("and com.em_efin_uniquecode ilike '%")
              .append(searchAttr.getString("uniquecode")).append("%' ");
        if (searchAttr.has("uniquename")
            && !StringUtils.isEmpty(searchAttr.getString("uniquename")))
          whereClause.append("and com.em_efin_uniquecodename ilike '%")
              .append(searchAttr.getString("uniquename")).append("%' ");

      }

      orderClause.append("order by ");
      if (searchAttr.getString("sortName").equals("actiontype"))
        orderClause.append(" pc.action ");
      else if (searchAttr.getString("sortName").equals("appno"))
        orderClause.append("pc.trx_app_no");
      else if (searchAttr.getString("sortName").equals("actiontype"))
        orderClause.append("pc.action");
      else if (searchAttr.getString("sortName").equals("actionDate"))
        orderClause.append("pc.trx_app_no");
      else if (searchAttr.getString("sortName").equals("Amt"))
        orderClause.append("pc.Amount");
      else if (searchAttr.getString("sortName").equals("penaltytype"))
        orderClause.append("pentype.deductiontype");
      else if (searchAttr.getString("sortName").equals("penaltypercentage"))
        orderClause.append("pc.Penalty_Percentage");
      else if (searchAttr.getString("sortName").equals("penaltyamount"))
        orderClause.append("pc.Penalty_Amount");
      else if (searchAttr.getString("sortName").equals("actionreason"))
        orderClause.append("pc.Action_Reason");
      else if (searchAttr.getString("sortName").equals("actionjustfication"))
        orderClause.append("pc.Action_Justification");

      else if (searchAttr.getString("sortName").equals("associatedbp"))
        orderClause.append("bp.em_efin_documentno");
      else if (searchAttr.getString("sortName").equals("bpname"))
        orderClause.append("bp.name");
      else if (searchAttr.getString("sortName").equals("amarsarfno"))
        orderClause.append("inv.documentno");
      else if (searchAttr.getString("sortName").equals("amarsarfamount"))
        orderClause.append("pc.Amarsarf_Amount");
      else if (searchAttr.getString("sortName").equals("accounttype"))
        orderClause.append("pc.penalty_account_type");

      else if (searchAttr.getString("sortName").equals("uniquecode"))
        orderClause.append("com.em_efin_uniquecode");
      else if (searchAttr.getString("sortName").equals("uniquename"))
        orderClause.append("com.em_efin_uniquecodename");
      else if (searchAttr.getString("sortName").equals("freezepenalty"))
        orderClause.append("pc.Freeze_Penalty");

      else
        orderClause.append("pc.seqno");
      orderClause.append(" ").append(searchAttr.getString("sortType"));

      // Get Row Count
      ps = conn.prepareStatement(countQuery.append(fromQuery).append(whereClause).toString());
      log4j.debug("Penalty count:" + ps.toString());
      rs = ps.executeQuery();
      if (rs.next())
        totalRecord = rs.getInt("count");
      log4j.debug("offset:" + offset);
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

      // Penalty Details
      ps1 = conn.prepareStatement((selectQuery.append(fromQuery).append(whereClause)
          .append(orderClause).append(" limit ").append(searchAttr.getInt("limit"))
          .append(" offset ").append(searchAttr.getInt("offset"))).toString());
      log4j.debug("Penalty:" + ps1.toString());
      rs1 = ps1.executeQuery();
      while (rs1.next()) {
        json = new JSONObject();
        if (rs1.getDate("actiondate") != null) {
          date = df.format(rs1.getDate("actiondate"));
          date = dateYearFormat.format(df.parse(date));
          date = UtilityDAO.convertTohijriDate(date);
        }
        json.put("id", Utility.nullToEmpty(rs1.getString("id")));
        json.put("Sequence", Utility.nullToEmpty(rs1.getString("seqno")));
        json.put("actions", "");
        json.put("appno", Utility.nullToEmpty(rs1.getString("trx_app_no")));
        json.put("actiontype", Utility.nullToEmpty(rs1.getString("action")));
        json.put("actionDate", Utility.nullToEmpty(date));
        json.put("Amt", Utility.nullToEmpty(rs1.getString("amount")));
        EfinRDVTxnline rdvTxnLine = OBDal.getInstance().get(EfinRDVTxnline.class, inpRDVTxnLineId);
        json.put("NetAmt", rdvTxnLine.getNetmatchAmt());
        json.put("penaltytype", Utility.nullToEmpty(rs1.getString("efin_penalty_types_id")));
        json.put("penaltypercentage", Utility.nullToEmpty(rs1.getString("penaltypercent")));
        json.put("penaltyamount", Utility.nullToEmpty(rs1.getString("penaltyamt")));
        json.put("actionreason", Utility.nullToEmpty(rs1.getString("actionreason")));
        json.put("actionjustfication", Utility.nullToEmpty(rs1.getString("actionjustfication")));
        json.put("bpartnerId", Utility.nullToEmpty(rs1.getString("c_bpartner_id")));
        json.put("bpartnername", Utility.nullToEmpty(rs1.getString("bpname")));
        json.put("associatedbp", Utility.nullToEmpty(rs1.getString("bpname")));
        json.put("bpname", Utility.nullToEmpty(rs1.getString("bpnametxt")));
        json.put("freezepenalty", Utility.nullToEmpty(rs1.getString("freezepenalty")));
        json.put("amarsarfno", Utility.nullToEmpty(rs1.getString("c_invoice_id")));
        json.put("amarsarfamount", Utility.nullToEmpty(rs1.getString("amarsafamount")));
        json.put("accounttype", Utility.nullToEmpty(rs1.getString("penalty_account_type")));
        json.put("uniquecodeId", Utility.nullToEmpty(rs1.getString("penalty_uniquecode")));
        json.put("uniquecode", Utility.nullToEmpty(rs1.getString("em_efin_uniquecode")));
        json.put("uniquecodehid", Utility.nullToEmpty(rs1.getString("em_efin_uniquecode")));
        json.put("uniquename", Utility.nullToEmpty(rs1.getString("em_efin_uniquecodename")));
        json.put("deductiontype", Utility.nullToEmpty(rs1.getString("deductiontype")));
        json.put("penaltyrelId", Utility.nullToEmpty(rs1.getString("penaltyrelId")));
        json.put("orgPenaltyAmt", Utility.nullToEmpty(rs1.getString("orgPenaltyAmt")));

        jsonArray.put(json);
      }
      result.put("rows", jsonArray);
    } catch (final Exception e) {
      log4j.error("Exception in getSalaryList", e);
    } finally {
      try {
        if (ps != null)
          ps.close();
        if (rs != null)
          rs.close();
      } catch (final SQLException e) {
        log4j.error("Exception in getSalaryList", e);
      }
    }
    return result;
  }

  /**
   * 
   * @param clientId
   * @param contractId
   * @param searchFlag
   * @param vo
   * @return ContractCount
   */
  public int getSalaryCount(String clientId, String inpRDVTxnLineId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    int totalRecord = 0;
    String sqlQuery = "";
    try {
      sqlQuery = " select count(*) as totalRecord from efin_penalty_action where efin_rdvtxnline_id = ? ";
      /*
       * if (searchFlag.equals("true")) { if (vo.getElement() != null) sqlQuery +=
       * " and ln.payroll ilike '%" + vo.getElement() + "%'"; if (vo.getPercentage() != null)
       * sqlQuery += " and ln.percentage ilike '%" + vo.getPercentage() + "%'"; if (vo.getValue() !=
       * null) sqlQuery += " and ln.contractvalue ='" + vo.getValue() + "'";
       * 
       * }
       */
      st = conn.prepareStatement(sqlQuery);
      st.setString(1, inpRDVTxnLineId);
      rs = st.executeQuery();
      if (rs.next())
        totalRecord = rs.getInt("totalRecord");
    } catch (final SQLException e) {
      log4j.error("", e);
    } catch (final Exception e) {
      log4j.error("", e);
    } finally {
      try {
        st.close();
      } catch (final SQLException e) {
        log4j.error("", e);
      }
    }
    return totalRecord;
  }

  /**
   * 
   * @param clientId
   * @param employeeId
   * @param vo
   * @param limit
   * @param offset
   * @param sortColName
   * @param sortColType
   * @param searchFlag
   * @return ContractList
   */
  public List<PenaltyActionVO> getSalaryList(String clientId, String inpRDVTxnLineId,
      PenaltyActionVO vo, int limit, int offset, String sortColName, String sortColType,
      String searchFlag) {
    PreparedStatement st = null;
    ResultSet rs = null;
    List<PenaltyActionVO> ls = new ArrayList<PenaltyActionVO>();
    String sqlQuery = "";
    DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
    SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
    String date = "";
    try {

      sqlQuery = " select efin_penalty_action_id,seqno,trx_app_no,action,Action_Date,Amount,efin_penalty_types_id, Penalty_Percentage, Penalty_Amount, Action_Reason, Action_Justification,"
          + "efin_penalty_action.c_bpartner_id,BP_Name, Freeze_Penalty, c_invoice_id,Amarsarf_Amount,penalty_account_type,penalty_uniquecode,com.em_efin_uniquecode ,com.em_efin_uniquecodename "
          + "from efin_penalty_action  left join c_validcombination com on com.c_validcombination_id=efin_penalty_action.penalty_uniquecode  where efin_rdvtxnline_id = ? ";
      /*
       * if (searchFlag.equals("true")) { if (vo.getElement() != null) sqlQuery +=
       * " and ln.payroll ilike '%" + vo.getElement() + "%'"; if (vo.getPercentage() != null)
       * sqlQuery += " and ln.percentage ilike '%" + vo.getPercentage() + "%'"; if (vo.getValue() !=
       * null) sqlQuery += " and ln.contractvalue ='" + vo.getValue() + "'";
       * 
       * } if (sortColName != null && sortColName.equals("element")) sqlQuery +=
       * " order by payroll " + sortColType + " limit " + limit + " offset " + offset; else if
       * (sortColName != null && sortColName.equals("Percentage")) sqlQuery +=
       * " order by percentage " + sortColType + " limit " + limit + " offset " + offset; else if
       * (sortColName != null && sortColName.equals("value")) sqlQuery += " order by contractvalue "
       * + sortColType + " limit " + limit + " offset " + offset; else sqlQuery +=
       * " order by created  " + sortColType + " limit " + limit + " offset " + offset;
       * log4j.debug("DAO select Query:" + sqlQuery + ">> contractId:" + contractId);
       */
      st = conn.prepareStatement(sqlQuery);
      st.setString(1, inpRDVTxnLineId);
      rs = st.executeQuery();
      log4j.debug("sqlQuery" + sqlQuery.toString());
      while (rs.next()) {
        PenaltyActionVO cVO = new PenaltyActionVO();
        cVO.setEfin_penalty_action_id(rs.getString("efin_penalty_action_id"));
        cVO.setSeqno(rs.getString("seqno"));
        cVO.setTrx_app_no(rs.getString("trx_app_no"));
        cVO.setAction(rs.getString("action"));
        if (rs.getDate("Action_Date") != null) {
          date = df.format(rs.getDate("Action_Date"));
          date = dateYearFormat.format(df.parse(date));
          date = UtilityDAO.convertTohijriDate(date);
          cVO.setActionDate(date);
        } else
          cVO.setActionDate(null);

        cVO.setAmount(rs.getString("Amount"));
        cVO.setPenaltyType(rs.getString("efin_penalty_types_id"));
        cVO.setPenaltyPercentage(rs.getString("Penalty_Percentage"));
        cVO.setPenaltyamount(rs.getString("Penalty_Amount"));
        cVO.setActionReason(rs.getString("Action_Reason"));
        cVO.setActionJustification(rs.getString("Action_Justification"));
        cVO.setBpartnerid(rs.getString("c_bpartner_id"));
        cVO.setBpartnername(rs.getString("BP_Name"));
        cVO.setFreezePenalty(rs.getString("Freeze_Penalty"));
        cVO.setInvoiceId(rs.getString("c_invoice_id") == null ? "" : rs.getString("c_invoice_id"));
        cVO.setAmarsarfAmount(
            rs.getString("Amarsarf_Amount") == null ? "0" : rs.getString("Amarsarf_Amount"));

        /*
         * if(rs.getString("penalty_account_type")!=null &&
         * rs.getString("penalty_account_type").equals("BJ")) cVO.setPenaltyaccount_type("Budget");
         * else cVO.setPenaltyaccount_type("Budget Adjustment");
         */

        cVO.setPenaltyaccount_type(rs.getString("penalty_account_type"));
        cVO.setPenaltyuniquecode(
            rs.getString("penalty_uniquecode") == null ? "" : rs.getString("penalty_uniquecode"));

        cVO.setUniquecodeName(rs.getString("em_efin_uniquecodename") == null ? ""
            : rs.getString("em_efin_uniquecodename"));

        ls.add(cVO);
      }
    } catch (final SQLException e) {
      log4j.error("", e);
    } catch (final Exception e) {
      log4j.error("", e);
    } finally {
      try {
        st.close();
        rs.close();
      } catch (final SQLException e) {
        log4j.error("", e);
      }
    }
    return ls;
  }

  /**
   * This method is used to get business partner name
   * 
   * @param clientId
   * @param rdvtrxlineId
   * @return
   */
  public List<PenaltyActionVO> getbpartnername(String clientId, String rdvtrxlineId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String sql = "";
    List<PenaltyActionVO> ls = new ArrayList<PenaltyActionVO>();
    try {
      sql = "select c_bpartner.c_bpartner_id,concat(em_efin_documentno,'-',name) as bpname from c_bpartner   left join efin_penalty_action act on act.c_bpartner_id = c_bpartner.c_bpartner_id \n"
          + " where c_bpartner.ad_client_id=?  and act.efin_rdvtxnline_id= ? ";
      st = conn.prepareStatement(sql);
      st.setString(1, clientId);
      st.setString(2, rdvtrxlineId);
      log4j.debug("getbpartnername:" + st.toString());
      rs = st.executeQuery();
      while (rs.next()) {
        PenaltyActionVO eVO = new PenaltyActionVO();
        eVO.setBpartnerid(Utility.nullToEmpty(rs.getString("c_bpartner_id")));
        eVO.setBpartnername(Utility.nullToEmpty(rs.getString("bpname")));
        ls.add(eVO);
      }
    } catch (final SQLException e) {
      log4j.error("Exception in getbankname", e);
      return ls;
    } catch (final Exception e) {
      log4j.error("Exception in getbankname", e);
      return ls;
    } finally {
      try {
        st.close();
        rs.close();
      } catch (final SQLException e) {
        log4j.error("Exception in getbankname", e);
        return ls;
      }
    }
    return ls;
  }

  /**
   * This method is used to get penalty type
   * 
   * @param clientId
   * @param action
   * @param lang
   * @return
   */
  public List<PenaltyActionVO> getpenaltyType(String clientId, String action, String lang) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String sql = "";
    List<PenaltyActionVO> ls = new ArrayList<PenaltyActionVO>();
    try {
      if (lang.equals("ar_SA"))
        sql = "  select efin_penalty_types_id, coalesce(lkln.arabicname,lkln.englishname) as name,typ.threshold from efin_penalty_types  typ "
            + " join eut_deflookups_typeln lkln on lkln.value= typ.deductiontype join eut_deflookups_type lkhd on lkhd.eut_deflookups_type_id=lkln.eut_deflookups_type_id and lkhd.value = 'PENALTY_TYPE' "
            + "where typ.maintain_enable='Y' " + " and typ.ad_client_id=?";
      else
        sql = "  select efin_penalty_types_id,  lkln.englishname as name,typ.threshold from efin_penalty_types  typ "
            + "join eut_deflookups_typeln lkln on lkln.value= typ.deductiontype join eut_deflookups_type lkhd on lkhd.eut_deflookups_type_id=lkln.eut_deflookups_type_id and lkhd.value = 'PENALTY_TYPE' "
            + "where typ.maintain_enable='Y' " + " and typ.ad_client_id=?";
      if (action != null && action.equals("RM"))
        sql += " and  (typ.threshold is null or typ.threshold = 0)  ";
      sql = String.format(sql);
      st = conn.prepareStatement(sql);
      st.setString(1, clientId);
      log4j.debug("getbpartnername:" + st.toString());
      rs = st.executeQuery();
      while (rs.next()) {
        PenaltyActionVO eVO = new PenaltyActionVO();
        eVO.setPenaltyId(Utility.nullToEmpty(rs.getString("efin_penalty_types_id")));
        eVO.setPenaltyname(Utility.nullToEmpty(rs.getString("name")));
        eVO.setThershold(rs.getString("threshold") == null ? "0" : rs.getString("threshold"));
        ls.add(eVO);
      }
    } catch (final SQLException e) {
      log4j.error("Exception in getbankname", e);
      return ls;
    } catch (final Exception e) {
      log4j.error("Exception in getbankname", e);
      return ls;
    } finally {
      try {
        st.close();
        rs.close();
      } catch (final SQLException e) {
        log4j.error("Exception in getbankname", e);
        return ls;
      }
    }
    return ls;
  }

  /**
   * This method is used to get budget adjustment unique code
   * 
   * @param clientId
   * @param rdvtrxlineId
   * @return
   */
  public List<PenaltyActionVO> getBudgetAdjustmentUniquecode(String clientId, String rdvtrxlineId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String sql = "";
    List<PenaltyActionVO> ls = new ArrayList<PenaltyActionVO>();
    try {
      sql = "  select com.c_validcombination_id,  com.em_efin_uniquecode from c_validcombination  com  left join efin_penalty_action act on act.penalty_uniquecode = com.c_validcombination_id "
          +

          " where act.efin_rdvtxnline_id= ?  " + " and com.ad_client_id=?";
      st = conn.prepareStatement(sql);
      st.setString(1, rdvtrxlineId);
      st.setString(2, clientId);
      log4j.debug("getbpartnername:" + st.toString());
      rs = st.executeQuery();
      while (rs.next()) {
        PenaltyActionVO eVO = new PenaltyActionVO();
        eVO.setCombId(Utility.nullToEmpty(rs.getString("c_validcombination_id")));
        eVO.setCombUniquecode(Utility.nullToEmpty(rs.getString("em_efin_uniquecode")));
        ls.add(eVO);
      }
    } catch (final SQLException e) {
      log4j.error("Exception in getbankname", e);
      return ls;
    } catch (final Exception e) {
      log4j.error("Exception in getbankname", e);
      return ls;
    } finally {
      try {
        st.close();
        rs.close();
      } catch (final SQLException e) {
        log4j.error("Exception in getbankname", e);
        return ls;
      }
    }
    return ls;
  }

  /**
   * This method is used to get invoice number
   * 
   * @param clientId
   * @return
   */
  public List<PenaltyActionVO> getinvoiceno(String clientId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String sql = "";
    List<PenaltyActionVO> ls = new ArrayList<PenaltyActionVO>();
    try {
      sql = "select c_invoice_id,documentno from c_invoice\n" + " where ad_client_id=?";
      st = conn.prepareStatement(sql);
      st.setString(1, clientId);
      log4j.debug("getinvoiceno:" + st.toString());
      rs = st.executeQuery();
      while (rs.next()) {
        PenaltyActionVO eVO = new PenaltyActionVO();
        eVO.setInvoiceId(Utility.nullToEmpty(rs.getString("c_invoice_id")));
        eVO.setInvoicedocumentno(Utility.nullToEmpty(rs.getString("documentno")));
        ls.add(eVO);
      }
    } catch (final SQLException e) {
      log4j.error("Exception in getinvoiceno", e);
      return ls;
    } catch (final Exception e) {
      log4j.error("Exception in getinvoiceno", e);
      return ls;
    } finally {
      try {
        st.close();
        rs.close();
      } catch (final SQLException e) {
        log4j.error("Exception in getinvoiceno", e);
        return ls;
      }
    }
    return ls;
  }

  /**
   * This method is used to get penalty action
   * 
   * @param seqno
   * @param trxappno
   * @param clientId
   * @param actiontype
   * @param actionDate
   * @param amount
   * @param penalty_type
   * @param penalty_per
   * @param penalty_amt
   * @param actreason
   * @param actionjus
   * @param bpartnerId
   * @param bpname
   * @param freezepenalty
   * @param invoice
   * @param amarsarfamount
   * @param RDVTrxlineId
   * @param penalty_account
   * @param uniquecode
   * @param isRdvSaveAction
   * @return
   */
  public String getPenaltyAction(String seqno, String trxappno, String clientId, String actiontype,
      String actionDate, String amount, String penalty_type, String penalty_per, String penalty_amt,
      String actreason, String actionjus, String bpartnerId, String bpname, String freezepenalty,
      String invoice, String amarsarfamount, String RDVTrxlineId, String penalty_account,
      String uniquecode, Boolean isRdvSaveAction) {
    String sql = "";
    Long number = Long.parseLong(seqno);

    try {
      OBContext.setAdminMode();
      EfinRDVTxnline rdvline = OBDal.getInstance().get(EfinRDVTxnline.class, RDVTrxlineId);
      EfinPenaltyAction action = OBProvider.getInstance().get(EfinPenaltyAction.class);
      action.setClient(OBDal.getInstance().get(Client.class, clientId));
      action.setSequenceNumber(number);
      action.setTRXAppNo(rdvline.getTrxappNo());
      action.setAction(actiontype);
      if (actionDate != null && actionDate != "")
        action.setActionDate(convertGregorian(actionDate));
      if (!isRdvSaveAction)
        action.setAmount(rdvline.getMatchAmt());
      else {
        action.setAmount(new BigDecimal(amount));
      }
      action.setEfinPenaltyTypes(OBDal.getInstance().get(EfinPenaltyTypes.class, penalty_type));
      if (penalty_per != null && penalty_per != "")
        action.setPenaltyPercentage(new BigDecimal(penalty_per));
      action.setPenaltyAmount(new BigDecimal(penalty_amt.replaceAll(",", "")));
      action.setActionReason(actreason);
      action.setActionJustification(actionjus);
      if (bpartnerId != null && bpartnerId != "")
        action.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, bpartnerId));
      action.setName(bpname);
      action.setEfinRdvtxnline(rdvline);
      if (freezepenalty.equals("Y")) {
        action.setFreezePenalty(true);
      } else {
        action.setFreezePenalty(false);
      }
      if (invoice != null) {
        action.setInvoice(
            OBDal.getInstance().get(org.openbravo.model.common.invoice.Invoice.class, invoice));
        action.setAmarsarfAmount(new BigDecimal(amarsarfamount));
      }
      action.setPenaltyAccountType(penalty_account);
      if (uniquecode != null)
        action
            .setPenaltyUniquecode(OBDal.getInstance().get(AccountingCombination.class, uniquecode));
      OBDal.getInstance().save(action);
      OBDal.getInstance().flush();

      insertPenaltyHeader(action, action.getEfinRdvtxnline(), action.getPenaltyAmount(), null,
          null);
    } catch (final Exception e) {
      log4j.error("Exception in PenaltyAction", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return sql;
  }

  /**
   * This method is used to update penalty
   * 
   * @param pentlaction
   * @param oldpenaltyTypeId
   * @param penaltyamt
   */
  public static void updatePenaltys(EfinPenaltyAction pentlaction,
      EfinPenaltyTypes oldpenaltyTypeId, BigDecimal penaltyamt) {
    BigDecimal SumofAddRemPenAmt = BigDecimal.ZERO;
    try {
      SumofAddRemPenAmt = getSumofAddRemovePenaltyAmt(pentlaction, oldpenaltyTypeId);

      OBQuery<EfinPenalty> penalty = OBDal.getInstance().createQuery(EfinPenalty.class,
          " as e where e.efinRdv.id in ( select e.efinRdv.id from Efin_RDVTxn e where e.id='"
              + pentlaction.getEfinRdvtxnline().getEfinRdvtxn().getId() + "') and e.penaltyType='"
              + pentlaction.getEfinPenaltyTypes().getDeductiontype() + "'");
      penalty.setMaxResult(1);
      log4j.debug("penalty:" + penalty.getWhereAndOrderBy());
      if (penalty.list().size() > 0) {
        EfinPenalty uppenalty = penalty.list().get(0);
        log4j.debug("getPenaltyAmount:" + penaltyamt);
        log4j.debug("getPenaltyAmount:" + uppenalty.getPenaltyApplied());
        uppenalty.setPenaltyApplied(SumofAddRemPenAmt);
        OBDal.getInstance().save(uppenalty);
      }
    } catch (final Exception e) {
      log4j.error("Exception in PenaltyAction", e);

    } finally {

    }
  }

  /**
   * This method is used to update old penalty
   * 
   * @param pentlaction
   * @param oldpenaltyTypeId
   * @param penaltyamt
   */
  public static void updateoldPenalty(EfinPenaltyAction pentlaction,
      EfinPenaltyTypes oldpenaltyTypeId, BigDecimal penaltyamt) {
    BigDecimal SumofAddRemPenAmt = BigDecimal.ZERO;

    try {
      SumofAddRemPenAmt = getSumofAddRemovePenaltyAmt(pentlaction, oldpenaltyTypeId);

      if (oldpenaltyTypeId != null && !pentlaction.getEfinPenaltyTypes().equals(oldpenaltyTypeId)) {
        OBQuery<EfinPenalty> oldpenalty = OBDal.getInstance().createQuery(EfinPenalty.class,
            " as e where e.efinRdv.id in ( select e.efinRdv.id from Efin_RDVTxn e where e.id='"
                + pentlaction.getEfinRdvtxnline().getEfinRdvtxn().getId() + "') and e.penaltyType='"
                + oldpenaltyTypeId.getDeductiontype() + "'");
        oldpenalty.setMaxResult(1);
        log4j.debug("penalty:" + oldpenalty.getWhereAndOrderBy());
        if (oldpenalty.list().size() > 0) {
          EfinPenalty upoldpenalty = oldpenalty.list().get(0);
          upoldpenalty.setPenaltyApplied(SumofAddRemPenAmt);
          OBDal.getInstance().save(upoldpenalty);
        }
      }

    } catch (final Exception e) {
      log4j.error("Exception in PenaltyAction", e);

    } finally {

    }
  }

  /**
   * This method is used to delete penalty header
   * 
   * @param peanltyaction
   */
  public static void deletepenaltyHed(EfinPenaltyAction peanltyaction) {
    List<EfinPenaltyHeader> penhedList = new ArrayList<EfinPenaltyHeader>();
    JSONObject result = new JSONObject();
    try {
      OBContext.setAdminMode();
      Boolean canDelete = false;
      EfinPenaltyHeader penhd = null;
      result = getPenaltyAmt(peanltyaction.getEfinRdvtxnline(), "del", peanltyaction);
      OBQuery<EfinPenaltyHeader> penltyhed = OBDal.getInstance().createQuery(
          EfinPenaltyHeader.class,
          " as e where e.efinRdvtxnline.id='" + peanltyaction.getEfinRdvtxnline().getId() + "'");
      penltyhed.setMaxResult(1);
      penhedList = penltyhed.list();
      if (penhedList.size() > 0) {
        penhd = penhedList.get(0);
        log4j.debug(
            "penhd.getEfinPenaltyActionList().size()" + penhd.getEfinPenaltyActionList().size());
        if (penhd.getEfinPenaltyActionList().size() == 1) {
          canDelete = true;
        } else {
          penhd.setPenaltyAmount(
              penhd.getPenaltyAmount().subtract(peanltyaction.getPenaltyAmount()));
          penhd.setUpdatedPenaltyAmt(
              penhd.getUpdatedPenaltyAmt().subtract(peanltyaction.getPenaltyAmount()));
          OBDal.getInstance().save(penhd);
        }
      }
      updatePenalty(result, peanltyaction);
      EfinPenaltyAction oldPenalty = peanltyaction.getPenaltyRel();
      if (oldPenalty != null) {
        oldPenalty.setReleased(false);
        OBDal.getInstance().save(oldPenalty);
      }
      OBDal.getInstance().remove(peanltyaction);
      if (canDelete) {
        OBDal.getInstance().remove(penhd);
      }
      OBDal.getInstance().flush();

    } catch (final Exception e) {
      log4j.error("Exception in deletepenaltyHed", e);

    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method is used to get sum of add remove penalty amount
   * 
   * @param action
   * @param oldpenaltytype
   * @return
   */
  @SuppressWarnings("rawtypes")
  public static BigDecimal getSumofAddRemovePenaltyAmt(EfinPenaltyAction action,
      EfinPenaltyTypes oldpenaltytype) {
    BigDecimal sumOfAddRemPenAmt = BigDecimal.ZERO;
    String sqlQry = null;
    Query qry = null;
    try {
      sqlQry = " select distinct   coalesce(( select coalesce(sum(pen.penalty_amount),0) from efin_penalty_action pen      where pen.efin_rdvtxnline_id=act.efin_rdvtxnline_id   and pen.action='AD'  and pen.efin_penalty_types_id=act.efin_penalty_types_id  ) -"
          + "         ( select coalesce(sum(pen.penalty_amount),0) from efin_penalty_action pen        where pen.efin_rdvtxnline_id=act.efin_rdvtxnline_id "
          + "    and pen.action='RM'  and pen.efin_penalty_types_id=act.efin_penalty_types_id   ),0) as total  from efin_penalty_action act "
          + "    where act.efin_rdvtxnline_id=  ? and act.efin_penalty_types_id= ?  ";// and
                                                                                      // pen.efin_penalty_action_id
                                                                                      // <> ?
      qry = OBDal.getInstance().getSession().createSQLQuery(sqlQry);
      qry.setParameter(0, action.getEfinRdvtxnline().getId());
      if (oldpenaltytype == null && action.getEfinPenaltyTypes() != null)
        qry.setParameter(1, action.getEfinPenaltyTypes().getId());
      else if (oldpenaltytype != null)
        qry.setParameter(1, oldpenaltytype.getId());

      List queryList = qry.list();
      if (queryList != null && queryList.size() > 0) {
        log4j.debug("get:" + queryList.get(0));
        sumOfAddRemPenAmt = (BigDecimal) queryList.get(0);
      }
    } catch (final Exception e) {
      log4j.error("Exception in getSumofAddRemovePenaltyAmt", e);

    } finally {

    }
    return sumOfAddRemPenAmt;

  }

  /**
   * This method is used to convert hijiri date to Gregorian
   * 
   * @param hijridate
   * @return
   */
  public Date convertGregorian(String hijridate) {
    log4j.debug("hi:" + hijridate);
    String gregDate = Utility.convertToGregorian(hijridate);
    log4j.debug("gregDate:" + gregDate);
    Date greDate = null;
    try {
      DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
      greDate = df1.parse(gregDate);
      log4j.debug("greDate:" + greDate);
    } catch (Exception e) {
      log4j.error("Exception creating multiple transactions from payments", e);

    }
    return greDate;

  }

  /**
   * This method is used to get penalty amount
   * 
   * @param trxline
   * @param actiontype
   * @param penaltyaction
   * @return
   */
  @SuppressWarnings("rawtypes")
  public static JSONObject getPenaltyAmt(EfinRDVTxnline trxline, String actiontype,
      EfinPenaltyAction penaltyaction) {
    JSONObject result = new JSONObject(), json = null;
    JSONArray array = new JSONArray();
    String sqlqry = null;
    Query qry = null;
    BigDecimal reduceAmt = BigDecimal.ZERO;
    try {
      result.put("totalamount", "0");
      sqlqry = "    select  act.efin_penalty_types_id,  "
          + "               SUM(CASE WHEN act.action='AD'  THEN penalty_amount ELSE 0 END) + "
          + "               SUM(CASE WHEN act.action='RM'  THEN penalty_amount ELSE 0 END) as total "
          + "   from efin_penalty_action act   "
          + "             join efin_rdvtxnline ln on ln.efin_rdvtxnline_id= act.efin_rdvtxnline_id  "
          + "            join efin_rdvtxn trx on trx.efin_rdvtxn_id=ln.efin_rdvtxn_id  "
          + "            join efin_rdv rdv on rdv.efin_rdv_id= trx.efin_rdv_id    where  rdv.efin_rdv_id= ?  ";
      // if(actiontype!=null && actiontype.equals("del"))
      // sqlqry += " and act.efin_penalty_action_id<> ?";
      sqlqry += "              group by act.efin_penalty_types_id ";
      qry = OBDal.getInstance().getSession().createSQLQuery(sqlqry);
      log4j.debug("qry1" + qry);
      qry.setParameter(0, trxline.getEfinRdvtxn().getEfinRdv().getId());
      // if(actiontype!=null && actiontype.equals("del"))
      // qry.setParameter(1, penaltyaction.getId());
      List getPenaltytyAmt = qry.list();
      if (getPenaltytyAmt != null && getPenaltytyAmt.size() > 0) {
        for (Iterator iterator = getPenaltytyAmt.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          json = new JSONObject();
          if (row[0] != null) {
            json.put("penaltytype", row[0].toString());
            if (actiontype != null && actiontype.equals("del")) {
              if (penaltyaction.getEfinPenaltyTypes() != null
                  && penaltyaction.getEfinPenaltyTypes().getId().equals(row[0].toString())) {
                reduceAmt = new BigDecimal(row[1].toString())
                    .subtract(penaltyaction.getPenaltyAmount());
                json.put("amount", reduceAmt.toString());
              } else {
                json.put("amount", row[1].toString());
              }
            } else {
              json.put("amount", row[1].toString());
            }
            array.put(json);
          }
        }
        result.put("penaltylist", array);
        result.put("totalamount", new BigDecimal(result.getString("totalamount"))
            .add(new BigDecimal(json.getString("amount"))));
      }
      log4j.debug("result;" + result);
    } catch (Exception e) {
      log4j.error("Exception in getPenaltyAmt", e);

    }
    return result;
  }

  /**
   * This method is used to get penalty header amount
   * 
   * @param trxline
   * @return
   */
  @SuppressWarnings("rawtypes")
  public static BigDecimal getPenaltyheaderAmt(EfinRDVTxnline trxline) {
    String sqlqry = null;
    Query qry = null;
    BigDecimal calPenaltyheaderAmt = BigDecimal.ZERO;
    try {
      /*
       * sqlqry =
       * "  select (coalesce(sum(act.penalty_amount),0) + ( select coalesce(sum(pen.penalty_amount),0) from efin_penalty_action pen   "
       * +
       * "      where pen.efin_rdvtxnline_id=act.efin_rdvtxnline_id  and pen.efin_penalty_types_id=act.efin_penalty_types_id "
       * +
       * "       and pen.action='RM'   )) as totpenamt , act.efin_rdvtxnline_id from efin_penalty_action act"
       * + "     where act.efin_rdvtxnline_id=  ?  " +
       * "     and  act.action='AD' group by act.efin_rdvtxnline_id,act.efin_penalty_types_id ";
       */

      sqlqry = "  select (coalesce(sum(act.penalty_amount),0)) as totpenamt , act.efin_rdvtxnline_id from efin_penalty_action act"
          + "     where act.efin_rdvtxnline_id=  ?  "
          + "      group by act.efin_rdvtxnline_id,act.efin_penalty_types_id ";
      qry = OBDal.getInstance().getSession().createSQLQuery(sqlqry);
      qry.setParameter(0, trxline.getId());
      log4j.debug("qry:" + qry);
      List getPenaltytyAmt = qry.list();
      if (getPenaltytyAmt != null && getPenaltytyAmt.size() > 0) {
        for (Iterator iterator = getPenaltytyAmt.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          calPenaltyheaderAmt = calPenaltyheaderAmt.add(new BigDecimal(row[0].toString()));
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in getPenaltyheaderAmt", e);

    }
    return calPenaltyheaderAmt;
  }

  /**
   * This method is used to update penalty
   * 
   * @param result
   * @param penaltyaction
   */
  public static void updatePenalty(JSONObject result, EfinPenaltyAction penaltyaction) {
    JSONObject json = null;
    try {

      if (result != null) {
        JSONArray array = result.getJSONArray("penaltylist");
        for (int i = 0; i < array.length(); i++) {
          json = array.getJSONObject(i);
          BigDecimal penApplied = new BigDecimal(json.getString("amount"));

          EfinPenaltyTypes penaltytype = OBDal.getInstance().get(EfinPenaltyTypes.class,
              json.getString("penaltytype"));
          OBQuery<EfinPenalty> uppenaltyQry = OBDal.getInstance().createQuery(EfinPenalty.class,
              " as e where e.efinRdv.id in ( select e.efinRdv.id from Efin_RDVTxn e where e.id='"
                  + penaltyaction.getEfinRdvtxnline().getEfinRdvtxn().getId() + "') "
                  + " and e.penaltyType='" + penaltytype.getDeductiontype().getCode() + "'");
          uppenaltyQry.setMaxResult(1);
          log4j.debug("penalty:" + uppenaltyQry.getWhereAndOrderBy());
          if (uppenaltyQry.list().size() > 0) {
            EfinPenalty upoldpenalty = uppenaltyQry.list().get(0);
            upoldpenalty.setPenaltyApplied(new BigDecimal(json.getString("amount")));

            if (!(upoldpenalty.getOpeningPenAmount().compareTo(BigDecimal.ZERO) == 0)) {
              upoldpenalty
                  .setPenaltyRemaining(upoldpenalty.getOpeningPenAmount().subtract(penApplied));
            }
            OBDal.getInstance().save(upoldpenalty);
            if (penApplied.compareTo(BigDecimal.ZERO) == 0) {
              deletePenalty(upoldpenalty.getId(), penaltytype, penaltyaction);
            }
          } else {
            insertPenalty(penaltyaction, penaltytype, penApplied);
          }
        }
      }

    } catch (final Exception e) {
      log4j.error("Exception in updatePenalty", e);
    } finally {

    }
  }

  /**
   * Method to insert penalty if penalty does not exists based on RDV header
   * 
   * @param penaltyaction
   * @param penaltytype
   * @param penApplied
   */
  public static void insertPenalty(EfinPenaltyAction penaltyaction, EfinPenaltyTypes penaltytype,
      BigDecimal penApplied) {
    try {
      BigDecimal threshold = BigDecimal.ZERO;
      BigDecimal openPenAmt = BigDecimal.ZERO;
      BigDecimal percent = new BigDecimal("0.01");

      EfinRDV rdv = penaltyaction.getEfinRdvtxnline().getEfinRdvtxn().getEfinRdv();
      if (penaltytype.getThreshold() != null) {
        threshold = penaltytype.getThreshold().multiply(percent);
      }
      openPenAmt = threshold.multiply(rdv.getContractAmt());

      // insert penalty
      EfinPenalty penalty = OBProvider.getInstance().get(EfinPenalty.class);
      penalty.setClient(penaltyaction.getClient());
      penalty.setOrganization(penaltyaction.getOrganization());
      penalty.setEfinRdv(rdv);
      penalty.setPenaltyType(penaltytype.getDeductiontype());
      penalty.setAlertStatus(rdv.getPenaltyStatus());
      penalty.setPenaltyApplied(penApplied);
      if (!(openPenAmt.compareTo(BigDecimal.ZERO) == 0)) {
        penalty.setPenaltyRemaining(openPenAmt.subtract(penApplied));
      }
      penalty.setPenaltyPercentage(penaltytype.getThreshold());
      penalty.setOpeningPenAmount(openPenAmt);
      OBDal.getInstance().save(penalty);
      OBDal.getInstance().flush();

    } catch (final Exception e) {
      log4j.error("Exception in insertPenalty", e);
    }
  }

  /**
   * Method to delete penalty if the penalty type is not used in other lines
   * 
   * @param oldPenaltyId
   * @param penaltytype
   * @param penaltyaction
   */
  public static void deletePenalty(String oldPenaltyId, EfinPenaltyTypes penaltytype,
      EfinPenaltyAction penaltyaction) {
    Boolean canRemove = Boolean.TRUE;
    try {
      EfinRDV rdv = penaltyaction.getEfinRdvtxnline().getEfinRdvtxn().getEfinRdv();
      OBQuery<EfinPenaltyAction> penaltyAct = OBDal.getInstance().createQuery(
          EfinPenaltyAction.class,
          " as e where e.efinRdvtxnline.efinRdv.id=:rdvId and e.efinPenaltyTypes.id=:deductType "
              + "and e.efinRdvtxnline.id<>:currentlineId");
      penaltyAct.setNamedParameter("rdvId", rdv.getId());
      penaltyAct.setNamedParameter("deductType", penaltytype.getId());
      penaltyAct.setNamedParameter("currentlineId", penaltyaction.getEfinRdvtxnline().getId());

      if (penaltyAct.list().size() > 0) {
        canRemove = Boolean.FALSE;
      }
      if (canRemove) {
        EfinPenalty penalty = OBDal.getInstance().get(EfinPenalty.class, oldPenaltyId);
        OBDal.getInstance().remove(penalty);
        OBDal.getInstance().flush();
      }
    } catch (final Exception e) {
      log4j.error("Exception in deletePenalty", e);
    }
  }

  /**
   * insert penalty header if penalty header does not exists (based on rdv transaction line ) insert
   * a penalty header otherwise update penalty amt and update penalty amount
   * 
   * @param penaltyaction
   * @param rdvtrxln
   * @param penaltyamt
   * @param oldpenaltyTypeId
   * @param oldAction
   */
  @SuppressWarnings("unlikely-arg-type")
  public static void insertPenaltyHeader(EfinPenaltyAction penaltyaction, EfinRDVTxnline rdvtrxln,
      BigDecimal penaltyamt, EfinPenaltyTypes oldpenaltyTypeId, String oldAction) {
    EfinRDVTransaction previousrdvtrx = null;
    EfinRDVTxnline previousrdxtrxln = null;
    BigDecimal prevpenhdAmt = BigDecimal.ZERO;
    JSONObject result = new JSONObject();
    try {
      OBContext.setAdminMode();
      // get json object of penalty amount on each penalty types based on rdv header and penalty
      // type
      result = getPenaltyAmt(penaltyaction.getEfinRdvtxnline(), null, penaltyaction);

      // get previous rdv transaction based on current rdv transaction line , created desc , not
      // inculding current version id , limit 1
      OBQuery<EfinRDVTransaction> rdvtrx = OBDal.getInstance().createQuery(EfinRDVTransaction.class,
          " as e where e.id <>'" + rdvtrxln.getEfinRdvtxn().getId() + "'" + "  and e.efinRdv.id='"
              + rdvtrxln.getEfinRdvtxn().getEfinRdv().getId() + "' " + "and e.tXNVersion < '"
              + rdvtrxln.getEfinRdvtxn().getTXNVersion() + "' order by created desc   ");
      rdvtrx.setMaxResult(1);
      if (rdvtrx.list().size() > 0) {
        previousrdvtrx = rdvtrx.list().get(0);
      }

      // get previous rdv transaction line based on previous trx version and current rdv line
      // product id
      if (previousrdvtrx != null
          && (rdvtrxln.getProduct() != null || rdvtrxln.getItemDesc() != null)) {
        OBQuery<EfinRDVTxnline> prerdvtrxln = OBDal.getInstance().createQuery(EfinRDVTxnline.class,
            " as e where e.efinRdvtxn.id='" + previousrdvtrx.getId() + "' and e.trxlnNo='"
                + rdvtrxln.getTrxlnNo() + "'");// and
                                               // e.product.id='"+rdvtrxln.getProduct().getId()+"'
        prerdvtrxln.setMaxResult(1);
        if (prerdvtrxln.list().size() > 0) {
          previousrdxtrxln = prerdvtrxln.list().get(0);
        }
      }
      // based on previous rdv transaction and rdv transaction line , get the penalty header prvious
      // penalty amount values
      if (previousrdvtrx != null && previousrdxtrxln != null) {
        OBQuery<EfinPenaltyHeader> prevpenhd = OBDal.getInstance().createQuery(
            EfinPenaltyHeader.class, " as e where e.efinRdvtxnline.id='" + previousrdxtrxln.getId()
                + "' and e.efinRdvtxn.id='" + previousrdvtrx.getId() + "' ");
        prevpenhd.setMaxResult(1);
        if (prevpenhd.list().size() > 0) {
          prevpenhdAmt = prevpenhd.list().get(0).getUpdatedPenaltyAmt();
        } else
          prevpenhdAmt = BigDecimal.ZERO;
      }

      // check if penalty header exists or not based on current rdv transaction line and rdv
      // transaction id
      OBQuery<EfinPenaltyHeader> penhdexisting = OBDal.getInstance().createQuery(
          EfinPenaltyHeader.class, " as e where e.efinRdvtxnline.id='" + rdvtrxln.getId()
              + "' and e.efinRdvtxn.id='" + rdvtrxln.getEfinRdvtxn().getId() + "'");
      penhdexisting.setMaxResult(1);
      // if exists penalty header update penalty amount and update penalty amount
      if (penhdexisting.list().size() > 0) {
        // if difference of old penalty amount and new penalty amunt not zero or action changed, or
        // penalty type changed
        if (penaltyamt.compareTo(BigDecimal.ZERO) != 0
            || (oldAction != null && !oldAction.equals(penaltyaction.getAction()))
            || (oldpenaltyTypeId != null
                && !oldpenaltyTypeId.equals(penaltyaction.getEfinPenaltyTypes().getId()))) {

          EfinPenaltyHeader penltyhd = penhdexisting.list().get(0);
          // get penalty header amount
          penaltyamt = getPenaltyheaderAmt(penaltyaction.getEfinRdvtxnline());
          log4j.debug("penaltyamt:" + penaltyamt);
          penltyhd.setPenaltyAmount(penaltyamt);
          penltyhd.setUpdatedPenaltyAmt(prevpenhdAmt.add(penaltyamt));
          OBDal.getInstance().save(penltyhd);

          penaltyaction.setEfinPenaltyHeader(penltyhd);
          OBDal.getInstance().save(penaltyaction);

          updatePenalty(result, penaltyaction);
          OBDal.getInstance().flush();

        }
      } else {
        EfinPenaltyHeader penltyhd = OBProvider.getInstance().get(EfinPenaltyHeader.class);
        penltyhd.setClient(penaltyaction.getClient());
        penltyhd.setOrganization(penaltyaction.getOrganization());
        penltyhd.setLineNo(penaltyaction.getEfinRdvtxnline().getTrxlnNo());
        penltyhd.setEfinRdvtxn(rdvtrxln.getEfinRdvtxn());
        penltyhd.setEfinRdvtxnline(rdvtrxln);
        penltyhd.setExistingPenalty(prevpenhdAmt);
        penltyhd.setPenaltyAmount(penaltyamt);
        penltyhd
            .setUpdatedPenaltyAmt(penltyhd.getExistingPenalty().add(penltyhd.getPenaltyAmount()));
        OBDal.getInstance().save(penltyhd);
        penaltyaction.setEfinPenaltyHeader(penltyhd);
        OBDal.getInstance().save(penaltyaction);

        if (result != null) {
          updatePenalty(result, penaltyaction);
        }
        OBDal.getInstance().flush();
      }

    } catch (final Exception e) {
      log4j.error("Exception in PenaltyAction", e);

    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method is used to check penalty type going down negative value
   * 
   * @param line
   * @param currentPenaltyAmt
   * @param penaltytype
   * @param actionId
   * @param actiontype
   * @return
   */
  @SuppressWarnings("rawtypes")
  public static String chkPenaltytypeGoingdownNegativeval(EfinRDVTxnline line,
      BigDecimal currentPenaltyAmt, EfinPenaltyTypes penaltytype, String actionId,
      String actiontype) {
    BigDecimal diffofexpentocurpen = BigDecimal.ZERO;
    BigDecimal existPenaltyAmt = BigDecimal.ZERO;
    String sqlqry = null;
    Query qry = null;
    try {
      if (line != null) {

        sqlqry = "  select  coalesce( sum(act.penalty_amount),0) from efin_penalty_action act "
            + " where act.efin_rdvtxnline_id in ( select efin_rdvtxnline_id from efin_rdvtxnline where efin_rdvtxn_id in ("
            + " select efin_rdvtxn_id from efin_rdvtxn  where  efin_rdv_id= ? ) and efin_rdvtxnline.trxln_no= ? )"
            + "  and act.efin_penalty_types_id= ?  ";
        if (actionId != null)
          sqlqry += " and act.efin_penalty_action_id <> ? ";
        qry = OBDal.getInstance().getSession().createSQLQuery(sqlqry);
        qry.setParameter(0, line.getEfinRdvtxn().getEfinRdv().getId());
        qry.setParameter(1, line.getTrxlnNo());
        qry.setParameter(2, penaltytype.getId());
        if (actionId != null)
          qry.setParameter(3, actionId);
        log4j.debug("qry12:" + qry.toString());
        List getPenaltytyAmt = qry.list();
        if (getPenaltytyAmt != null && getPenaltytyAmt.size() > 0) {
          existPenaltyAmt = (BigDecimal) getPenaltytyAmt.get(0);
          log4j.debug("existPenaltyAmt:" + existPenaltyAmt);
          if (actiontype.equals("del"))
            diffofexpentocurpen = existPenaltyAmt;
          else
            diffofexpentocurpen = existPenaltyAmt.add(currentPenaltyAmt);
          log4j.debug("diffofexpentocurpen:" + diffofexpentocurpen);
          if (diffofexpentocurpen.compareTo(BigDecimal.ZERO) < 0)
            return "Y";
          else
            return "N";
        } else if (currentPenaltyAmt.compareTo(BigDecimal.ZERO) < 0)
          return "Y";
        else
          return "N";

      }

    } catch (final Exception e) {
      log4j.error("Exception in chkPenaltytypeGoingdownNegativeval", e);

    } finally {

    }
    return "N";
  }

  /**
   * This method is used to get business partner list
   * 
   * @param clientId
   * @param searchTerm
   * @param pagelimit
   * @param page
   * @param roleId
   * @param orgId
   * @param bpartner
   * @return
   */
  @SuppressWarnings("resource")
  public synchronized static JSONObject getBusinessPartnerList(String clientId, String searchTerm,
      int pagelimit, int page, String roleId, String orgId, String bpartner) {
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    int totalRecords = 0;
    try {
      jsob = new JSONObject();
      StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(),
          fromQuery = new StringBuilder();

      countQuery.append(" select count(distinct bp.c_bpartner_id) as count ");
      selectQuery.append(
          " select distinct bp.c_bpartner_id as bpId, concat(bp.em_efin_documentno,'-',bp.name) as value ");
      fromQuery.append(
          " from c_bpartner bp  where isvendor= 'Y' and ad_org_id in( select ad_org_id  from ad_role_orgaccess   where ad_role_id  = ? and ad_client_id = ? )  "
              + "                and ad_client_id=?  ");
      if (bpartner != null) {
        fromQuery.append(" and bp.c_bpartner_id <> '" + bpartner + "' ");
      }
      if (searchTerm != null && !searchTerm.equals(""))
        fromQuery.append(" and ((bp.em_efin_documentno ilike '%" + searchTerm.toLowerCase()
            + "%' ) or ( bp.name ilike '%" + searchTerm.toLowerCase() + "%' ))");

      st = conn.prepareStatement(countQuery.append(fromQuery).toString());

      st.setString(1, roleId);
      st.setString(2, clientId);
      st.setString(3, clientId);

      log4j.debug("benfilist:" + st.toString());
      log4j.debug("qry>>" + st.toString());
      rs = st.executeQuery();
      if (rs.next())
        totalRecords = rs.getInt("count");
      jsob.put("totalRecords", totalRecords);

      if (totalRecords > 0) {
        st = conn.prepareStatement(
            (selectQuery.append(fromQuery)).toString() + " order by value limit ? offset ? ");

        st.setString(1, roleId);
        st.setString(2, clientId);
        st.setString(3, clientId);
        st.setInt(4, pagelimit);
        st.setInt(5, (page - 1) * pagelimit);

        log4j.debug("benfilist:" + st.toString());
        rs = st.executeQuery();

        while (rs.next()) {
          JSONObject jsonData = new JSONObject();
          jsonData.put("id", rs.getString("bpId"));
          jsonData.put("recordIdentifier", rs.getString("value"));
          jsonArray.put(jsonData);
        }
      }
      if (jsonArray.length() > 0)
        jsob.put("data", jsonArray);
      else
        jsob.put("data", "");

    } catch (final Exception e) {
      log4j.error("Exception in getBeneficiaryList :", e);
      return jsob;
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {

      }
    }
    return jsob;
  }

  /**
   * This method is used to get unique code
   * 
   * @param clientId
   * @param searchTerm
   * @param pagelimit
   * @param page
   * @param roleId
   * @param orgId
   * @param type
   * @param bpartnerId
   * @return
   */
  @SuppressWarnings("resource")
  public synchronized static JSONObject getUniquecodeList(String clientId, String searchTerm,
      int pagelimit, int page, String roleId, String orgId, String type, String bpartnerId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    int totalRecords = 0;
    try {
      jsob = new JSONObject();
      StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(),
          fromQuery = new StringBuilder();

      countQuery.append(" select count(distinct com.c_validcombination_id) as count ");
      selectQuery.append(
          " select distinct com.c_validcombination_id as comId, com.em_efin_uniquecode as value ");
      fromQuery.append(
          " from c_validcombination com  join c_elementvalue val on val.c_elementvalue_id = com.account_id  join efin_budget_ctrl_param param on param.ad_client_id= com.ad_client_id "
              + "  where com.ad_org_id in( select ad_org_id  from ad_role_orgaccess   where ad_role_id  = ? and ad_client_id = ? )  "
              + "                and com.ad_client_id=?  ");

      if (searchTerm != null && !searchTerm.equals(""))
        fromQuery
            .append(" and com.em_efin_uniquecode ilike '%" + searchTerm.toLowerCase() + "%'  ");
      if (type != null && !type.equals("null"))
        fromQuery.append(" and com.em_efin_dimensiontype='" + type
            + "' and com.c_salesregion_id<> param.budgetcontrol_costcenter\n"
            + " and  com.c_salesregion_id<> param.hq_budgetcontrolunit  ");

      if (bpartnerId != null && !bpartnerId.equals("null") && !bpartnerId.equals(""))
        fromQuery.append(
            " and case when (val.accounttype ='A' or val.accounttype='L' )   then com.c_bpartner_id='"
                + bpartnerId + "'  when val.accounttype ='R'   then  1=1 end ");

      st = conn.prepareStatement(countQuery.append(fromQuery).toString());

      st.setString(1, roleId);
      st.setString(2, clientId);
      st.setString(3, clientId);

      log4j.debug("benfilist:" + st.toString());
      log4j.debug("qry>>" + st.toString());
      rs = st.executeQuery();
      if (rs.next())
        totalRecords = rs.getInt("count");
      jsob.put("totalRecords", totalRecords);

      if (totalRecords > 0) {
        st = conn.prepareStatement(
            (selectQuery.append(fromQuery)).toString() + " order by value limit ? offset ? ");

        st.setString(1, roleId);
        st.setString(2, clientId);
        st.setString(3, clientId);
        st.setInt(4, pagelimit);
        st.setInt(5, (page - 1) * pagelimit);
      }

      log4j.debug("benfilist:" + st.toString());
      rs = st.executeQuery();

      JSONObject jsonData = new JSONObject();
      if (totalRecords > 0) {

        while (rs.next()) {
          jsonData = new JSONObject();
          jsonData.put("id", rs.getString("comId"));
          jsonData.put("recordIdentifier", rs.getString("value"));
          jsonArray.put(jsonData);
        }
      }
      if (jsonArray.length() > 0)
        jsob.put("data", jsonArray);
      else
        jsob.put("data", "");

    } catch (final Exception e) {
      log4j.error("Exception in getUniquecodeList :", e);
      return jsob;
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {

      }
    }
    return jsob;
  }

  /**
   * This method is used to get total match amount
   * 
   * @param lineId
   * @param currentPenaltyAmt
   * @param actionId
   * @return
   */
  public static String getTotalMatchAmt(String lineId, BigDecimal currentPenaltyAmt,
      String actionId) {
    BigDecimal totalpenaltyAmt = BigDecimal.ZERO;
    BigDecimal existPenaltyAmt = BigDecimal.ZERO;
    BigDecimal totalMatchAmt = BigDecimal.ZERO;

    String sqlqry = null;
    Query qry = null;
    try {
      if (lineId != null) {
        EfinRDVTxnline line = OBDal.getInstance().get(EfinRDVTxnline.class, lineId);

        sqlqry = "  select  coalesce( sum(act.penalty_amount),0) from efin_penalty_action act "
            + "  where act.efin_rdvtxnline_id = ?  ";
        if (actionId != null)
          sqlqry += " and act.efin_penalty_action_id <> ? ";

        qry = OBDal.getInstance().getSession().createSQLQuery(sqlqry);
        qry.setParameter(0, lineId);
        if (actionId != null)
          qry.setParameter(1, actionId);
        log4j.debug("qry:" + qry.toString());
        @SuppressWarnings("rawtypes")
        List getPenaltytyAmt = qry.list();
        totalMatchAmt = line.getMatchAmt().subtract(line.getADVDeduct())
            .subtract(line.getHoldamt());
        if (getPenaltytyAmt != null && getPenaltytyAmt.size() > 0) {
          existPenaltyAmt = (BigDecimal) getPenaltytyAmt.get(0);
          log4j.debug("existPenaltyAmt:" + existPenaltyAmt);

          totalpenaltyAmt = existPenaltyAmt.add(currentPenaltyAmt);
          log4j.debug("totalpenaltyAmt:" + totalpenaltyAmt);
          if (totalpenaltyAmt.compareTo(totalMatchAmt) > 0)
            return "Y";
          else
            return "N";
        } else if (currentPenaltyAmt.compareTo(totalMatchAmt) > 0)
          return "Y";
        else
          return "N";

      }

    } catch (final Exception e) {
      log4j.error("Exception in chkPenaltytypeGoingdownNegativeval", e);

    } finally {

    }
    return "N";
  }

  /**
   * This method is used to get total match amount for selected records
   * 
   * @param currentPenaltyAmt
   * @param penaltypercetage
   * @param bulkpenaltyamtlogic
   * @param request
   * @return
   */
  public static String getTotalMatchAmtforSelectedRecords(BigDecimal currentPenaltyAmt,
      String penaltypercetage, String bulkpenaltyamtlogic, HttpServletRequest request) {
    BigDecimal totalpenaltyAmt = BigDecimal.ZERO;
    String isPenaltyAmtGreater = "Y";
    JSONObject selectedRecords = new JSONObject();
    AddDefaultPenaltyDAO penaltyDAO = new AddDefaultPenaltyDAOImpl();
    BigDecimal selectedRecordTotalNetMatchAmt = BigDecimal.ZERO;
    try {
      JSONArray transactionIds = new JSONArray();
      String selectedRecord = request.getParameter("inpselectedRecordsId").toString();
      String selectedRecordArray[] = selectedRecord.split(",");
      if (selectedRecordArray.length > 0) {
        for (int i = 0; i < selectedRecordArray.length; i++) {
          transactionIds.put(selectedRecordArray[i]);
        }
      } else {
        transactionIds.put(selectedRecord);
      }
      selectedRecords = penaltyDAO.getSelectedRecordsInformation(transactionIds);
      selectedRecordTotalNetMatchAmt = (selectedRecords.has("netMatchAmt")
          ? new BigDecimal(selectedRecords.getString("netMatchAmt"))
          : BigDecimal.ZERO);

      for (int i = 0; i < transactionIds.length(); i++) {
        EfinRDVTxnline rdvTxnLine = OBDal.getInstance().get(EfinRDVTxnline.class,
            transactionIds.get(i).toString());
        if (rdvTxnLine.getMatchAmt().compareTo(BigDecimal.ZERO) > 0) {
          if (bulkpenaltyamtlogic.equals("AL")) {
            if (StringUtils.isNotEmpty(penaltypercetage)
                && new BigDecimal(penaltypercetage).compareTo(BigDecimal.ZERO) > 0) {
              totalpenaltyAmt = rdvTxnLine.getMatchAmt().multiply(new BigDecimal(penaltypercetage))
                  .divide(new BigDecimal(100));
              if (rdvTxnLine.getNetmatchAmt().compareTo(totalpenaltyAmt) < 0) {
                isPenaltyAmtGreater = "N";
              }
            }
            if (rdvTxnLine.getNetmatchAmt().compareTo(currentPenaltyAmt) < 0) {
              isPenaltyAmtGreater = "N";
            }
          } else {
            // distribute case
            if (selectedRecordTotalNetMatchAmt.compareTo(BigDecimal.ZERO) > 0) {
              BigDecimal weigtage = (((rdvTxnLine.getNetmatchAmt())
                  .divide(selectedRecordTotalNetMatchAmt, 15, BigDecimal.ROUND_HALF_EVEN))
                      .multiply(currentPenaltyAmt)).setScale(2, RoundingMode.HALF_UP);
              if (weigtage.compareTo(rdvTxnLine.getNetmatchAmt()) > 0) {
                isPenaltyAmtGreater = "N";
              }
            } else {
              isPenaltyAmtGreater = "N";
            }

          }
        }

      }

    } catch (final Exception e) {
      log4j.error("Exception in getTotalMatchAmtforSelectedRecords", e);
    } finally {
    }
    return isPenaltyAmtGreater;
  }

  /**
   * 
   * @param request
   * @param response
   * @param con
   */
  public static String bulkPenalty(HttpServletRequest request, HttpServletResponse response,
      Connection con) {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    PenaltyActionDAO dao = new PenaltyActionDAO(con);
    JSONObject selectedRecords = new JSONObject();
    AddDefaultPenaltyDAO penaltyDAO = new AddDefaultPenaltyDAOImpl();
    BigDecimal selectedRecordTotalNetMatchAmt = BigDecimal.ZERO;

    try {
      OBContext.setAdminMode();
      String operation = request.getParameter("oper");
      String seqno = request.getParameter("Sequence");
      String trxappno = request.getParameter("appno");
      String actiontype = "AD";
      String actionDate = request.getParameter("actionDate");
      String penalty_type = request.getParameter("penaltytype");
      String penalty_per = request.getParameter("penaltypercentage");
      String penalty_amt = request.getParameter("penaltyamount");
      String actionreason = request.getParameter("actionreason");
      String actionjust = request.getParameter("actionjustfication");
      String bpartnerId = request.getParameter("associatedbp");
      String bpname = request.getParameter("bpname");
      String freezepenalty = request.getParameter("freezepenalty");
      String penalty_account = request.getParameter("accounttype");
      String uniquecode = request.getParameter("uniquecode");
      String strpenaltyactId = request.getParameter("id");
      String bulkPenaltyamountlogic = request.getParameter("bulkpenaltyamountlogic");

      log4j.debug("operation" + operation);
      log4j.debug("penaltytype12s" + penalty_type);
      DecimalFormat euroRelationFmt = org.openbravo.erpCommon.utility.Utility.getFormat(vars,
          "euroRelation");
      Integer roundoffConst = euroRelationFmt.getMaximumFractionDigits();
      JSONArray transactionIds = new JSONArray();
      String selectedRecord = request.getParameter("inpselectedRecordsId").toString();

      String selectedRecordArray[] = selectedRecord.split(",");
      if (selectedRecordArray.length > 0) {
        for (int i = 0; i < selectedRecordArray.length; i++) {
          transactionIds.put(selectedRecordArray[i]);
        }
      } else {
        transactionIds.put(selectedRecord);
      }
      // get total match amount for selected records
      selectedRecords = penaltyDAO.getSelectedRecordsInformation(transactionIds);
      selectedRecordTotalNetMatchAmt = (selectedRecords.has("netMatchAmt")
          ? new BigDecimal(selectedRecords.getString("netMatchAmt"))
          : BigDecimal.ZERO);

      if (operation.equals("edit") || strpenaltyactId.length() != 32) {
        for (int i = 0; i < transactionIds.length(); i++) {
          EfinRDVTxnline rdvTxnLine = OBDal.getInstance().get(EfinRDVTxnline.class,
              transactionIds.get(i).toString());
          // for amount logic - distribute
          String penaltyAmtForDistribute = (rdvTxnLine.getNetmatchAmt()
              .multiply(new BigDecimal(penalty_amt.replaceAll(",", "")))
              .divide(selectedRecordTotalNetMatchAmt, roundoffConst, RoundingMode.HALF_UP))
                  .toString();

          trxappno = rdvTxnLine.getTrxappNo();
          String amount = rdvTxnLine.getMatchAmt().toString();
          if (penalty_account.equals("E")) {
            uniquecode = rdvTxnLine.getAccountingCombination().getId();
          }
          if (StringUtils.isNotEmpty(penalty_per)
              && new BigDecimal(penalty_per).compareTo(BigDecimal.ZERO) > 0) {
            penalty_amt = (rdvTxnLine.getMatchAmt()
                .multiply(new BigDecimal(penalty_per).divide(new BigDecimal(100)))).toString();
          } else {
            penalty_per = "0";
          }
          if (rdvTxnLine.getEfinPenaltyActionList().size() > 0) {
            seqno = Integer.toString((rdvTxnLine.getEfinPenaltyActionList().size() * 10 + 10));
          } else {
            seqno = "10";
          }
          // apply penalty in all line
          // amount logic- All Line
          if (rdvTxnLine.getMatchAmt().compareTo(BigDecimal.ZERO) > 0) {
            if (bulkPenaltyamountlogic.equals("AL")) {
              dao.getPenaltyAction(seqno, trxappno, vars.getClient(), actiontype, actionDate,
                  amount, penalty_type, penalty_per, penalty_amt, actionreason, actionjust,
                  bpartnerId, bpname, freezepenalty, null, null, rdvTxnLine.getId(),
                  penalty_account, uniquecode, false);
            }
            // amount logic- Distribute
            else {
              dao.getPenaltyAction(seqno, trxappno, vars.getClient(), actiontype, actionDate,
                  amount, penalty_type, penalty_per, penaltyAmtForDistribute, actionreason,
                  actionjust, bpartnerId, bpname, freezepenalty, null, null, rdvTxnLine.getId(),
                  penalty_account, uniquecode, false);
            }
          }
        }

      }
    } catch (final Exception e) {
      log4j.error("Exception in bulkPenalty", e);
      return "0";

    } finally {
      OBContext.restorePreviousMode();
    }
    return "1";
  }
}