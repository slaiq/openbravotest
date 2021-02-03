package sa.elm.ob.finance.ad_process.RDVProcess.RdvHoldProcess;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

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
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.BusinessPartner;

import sa.elm.ob.finance.EfinPenalty;
import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.EfinRdvHold;
import sa.elm.ob.finance.EfinRdvHoldAction;
import sa.elm.ob.finance.EfinRdvHoldHeader;
import sa.elm.ob.finance.EfinRdvHoldTypes;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

public class RdvHoldActionDAOimpl implements RdvHoldActionDAO {
  private static Connection conn = null;
  VariablesSecureApp vars = null;
  private static Logger log4j = Logger.getLogger(RdvHoldActionDAOimpl.class);

  @SuppressWarnings("static-access")
  public RdvHoldActionDAOimpl(Connection con) {
    this.conn = con;
  }

  @Override
  public JSONObject getHoldList(String clientId, String inpRDVTxnLineId, JSONObject searchAttr) {
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

      countQuery.append("SELECT count(pc.efin_rdv_hold_action_id) as count ");
      selectQuery.append(
          "select pc.efin_rdv_hold_action_id as id, pc.seqno, pc.trx_app_no,pc.action, pc.Action_Date as actiondate, pc.Amount as amount, ");
      selectQuery.append(
          "pc.efin_rdv_hold_types_id,pentype,pentype.deductiontype ,pc.rdv_Hold_Percentage as penaltypercent, pc.rdv_Hold_Amount as penaltyamt, pc.Action_Reason as actionreason, pc.Action_Justification as actionjustfication, ");
      selectQuery.append(
          "pc.c_bpartner_id, bp.name as bpnametxt, pc.Freeze_rdv_Hold as freezepenalty, pc.c_invoice_id, pc.Amarsarf_Amount as amarsafamount, ");
      selectQuery.append(
          "pc.rdv_hold_account_type, pc.rdv_hold_uniquecode, com.em_efin_uniquecode ,com.em_efin_uniquecodename, "
              + "(case when pc.c_bpartner_id is not null then  concat(bp.em_efin_documentno,'-',bp.name) else '' end)  as bpname ,"
              + "inv.documentno,inv.grandtotal , pentype.deductiontype,pc.rdv_hold_rel_id as penaltyrelId,coalesce(orgpen.rdv_hold_amount,0) "
              + "as orgHoldAmt,pc.istxn ");

      fromQuery.append(" from efin_rdv_hold_action pc ");
      fromQuery.append(
          "left join c_validcombination com on com.c_validcombination_id = pc.rdv_hold_uniquecode "
              + " left join efin_rdv_hold_types pentype on pentype.efin_rdv_hold_types_id = pc.efin_rdv_hold_types_id "
              + " left join eut_deflookups_typeln ls on ls.eut_deflookups_typeln_id=pentype.deductiontype   "
              + " left join c_bpartner bp on bp.c_bpartner_id = pc.c_bpartner_id ");
      fromQuery.append(" left join c_invoice inv on inv.c_invoice_id = pc.c_invoice_id  "
          + "left join ad_ref_list actiontype on actiontype.value=pc.action and actiontype.ad_reference_id='11538D28FC294B2AB4465B29B0B4BFF3'"
          + " left join ad_ref_list diemtype on diemtype.value=pc.rdv_hold_account_type and diemtype.ad_reference_id='341EEDE3DC20468696320A84BF8049DF' "
          + "left join efin_rdv_hold_action orgpen on orgpen.efin_rdv_hold_action_id=pc.rdv_hold_rel_id  ");
      whereClause.append(" where pc.ad_client_id = '").append(clientId).append("' ");
      whereClause.append(" and pc.efin_rdvtxnline_id = '").append(inpRDVTxnLineId).append("' ");
      whereClause.append(
          "  and pc.efin_rdv_budgholdline_id is null   and   orgpen.efin_rdv_budgholdline_id is null ");
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
        if (searchAttr.has("holdtype") && !StringUtils.isEmpty(searchAttr.getString("holdtype")))
          whereClause.append("and ( ls.englishname ilike '%")
              .append(searchAttr.getString("holdtype")).append("%'  or ls.arabicname ilike  '% ")
              .append(searchAttr.getString("holdtype")).append("%' )");
        if (searchAttr.has("holdpercentage")
            && !StringUtils.isEmpty(searchAttr.getString("holdpercentage"))) {
          if (searchAttr.getString("holdpercentage").equals("0")) {
            whereClause.append("and pc.rdv_hold_Percentage= ")
                .append(searchAttr.getString("holdpercentage"));

          } else {
            whereClause.append("and CAST(pc.rdv_Hold_Percentage AS text) ilike '%")
                .append(searchAttr.getString("holdpercentage")).append("%' ");
          }
        }
        if (searchAttr.has("holdamount")
            && !StringUtils.isEmpty(searchAttr.getString("holdamount"))) {
          if (searchAttr.getString("holdamount").equals("0")) {
            whereClause.append("and pc.rdv_Hold_Amount= ")
                .append(searchAttr.getString("holdamount"));

          } else {
            whereClause.append("and  CAST(pc.rdv_Hold_Amount AS text)  ilike '%")
                .append(searchAttr.getString("holdyamount")).append("%' ");
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
        if (searchAttr.has("freezehold")
            && !StringUtils.isEmpty(searchAttr.getString("freezehold")))
          whereClause.append("and pc.Freeze_rdv_Hold ilike '%")
              .append(searchAttr.getString("freezehold")).append("%' ");
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
        // if (searchAttr.has("accounttype")
        // && !StringUtils.isEmpty(searchAttr.getString("accounttype")))
        // whereClause.append("and diemtype.name ilike '%")
        // .append(searchAttr.getString("accounttype")).append("%' ");
        // if (searchAttr.has("uniquecode")
        // && !StringUtils.isEmpty(searchAttr.getString("uniquecode")))
        // whereClause.append("and com.em_efin_uniquecode ilike '%")
        // .append(searchAttr.getString("uniquecode")).append("%' ");
        // if (searchAttr.has("uniquename")
        // && !StringUtils.isEmpty(searchAttr.getString("uniquename")))
        // whereClause.append("and com.em_efin_uniquecodename ilike '%")
        // .append(searchAttr.getString("uniquename")).append("%' ");

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
      else if (searchAttr.getString("sortName").equals("holdtype"))
        orderClause.append("pentype.deductiontype");
      else if (searchAttr.getString("sortName").equals("holdpercentage"))
        orderClause.append("pc.rdv_Hold_Percentage");
      else if (searchAttr.getString("sortName").equals("penaltyamount"))
        orderClause.append("pc.rdv_Hold_Amount");
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
      // else if (searchAttr.getString("sortName").equals("accounttype"))
      // orderClause.append("pc.penalty_account_type");
      //
      // else if (searchAttr.getString("sortName").equals("uniquecode"))
      // orderClause.append("com.em_efin_uniquecode");
      // else if (searchAttr.getString("sortName").equals("uniquename"))
      // orderClause.append("com.em_efin_uniquecodename");
      else if (searchAttr.getString("sortName").equals("freezehold"))
        orderClause.append("pc.Freeze_rdv_Hold");

      else
        orderClause.append("pc.seqno");
      orderClause.append(" ").append(searchAttr.getString("sortType"));

      // Get Row Count
      ps = conn.prepareStatement(countQuery.append(fromQuery).append(whereClause).toString());
      log4j.debug("Hold count:" + ps.toString());
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

      // Hold Details
      ps1 = conn.prepareStatement((selectQuery.append(fromQuery).append(whereClause)
          .append(orderClause).append(" limit ").append(searchAttr.getInt("limit"))
          .append(" offset ").append(searchAttr.getInt("offset"))).toString());
      log4j.debug("Hold:" + ps1.toString());
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
        json.put("holdtype", Utility.nullToEmpty(rs1.getString("efin_rdv_hold_types_id")));
        json.put("holdpercentage", Utility.nullToEmpty(rs1.getString("penaltypercent")));
        json.put("holdamount", Utility.nullToEmpty(rs1.getString("penaltyamt")));
        json.put("actionreason", Utility.nullToEmpty(rs1.getString("actionreason")));
        json.put("actionjustfication", Utility.nullToEmpty(rs1.getString("actionjustfication")));
        json.put("bpartnerId", Utility.nullToEmpty(rs1.getString("c_bpartner_id")));
        json.put("bpartnername", Utility.nullToEmpty(rs1.getString("bpname")));
        json.put("associatedbp", Utility.nullToEmpty(rs1.getString("bpname")));
        json.put("bpname", Utility.nullToEmpty(rs1.getString("bpnametxt")));
        json.put("freezehold", Utility.nullToEmpty(rs1.getString("freezepenalty")));
        json.put("amarsarfno", Utility.nullToEmpty(rs1.getString("c_invoice_id")));
        json.put("amarsarfamount", Utility.nullToEmpty(rs1.getString("amarsafamount")));
        // json.put("accounttype", Utility.nullToEmpty(rs1.getString("penalty_account_type")));
        // json.put("uniquecodeId", Utility.nullToEmpty(rs1.getString("penalty_uniquecode")));
        // json.put("uniquecode", Utility.nullToEmpty(rs1.getString("em_efin_uniquecode")));
        // json.put("uniquecodehid", Utility.nullToEmpty(rs1.getString("em_efin_uniquecode")));
        // json.put("uniquename", Utility.nullToEmpty(rs1.getString("em_efin_uniquecodename")));
        json.put("deductiontype", Utility.nullToEmpty(rs1.getString("deductiontype")));
        json.put("holdrelId", Utility.nullToEmpty(rs1.getString("penaltyrelId")));
        json.put("orgHoldAmt", Utility.nullToEmpty(rs1.getString("orgHoldAmt")));
        json.put("isTxn", Utility.nullToEmpty(rs1.getString("istxn")));
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

  @Override
  public JSONObject getHoldTxnList(String clientId, String inpRDVTxnId, JSONObject searchAttr) {
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

      countQuery.append("SELECT count(*) as count ");
      selectQuery.append(
          "select pc.txnGroupRef as id,pc.trx_app_no,pc.action, pc.Action_Date as actiondate, sum(pc.Amount) as amount, ");
      selectQuery.append(
          "pc.efin_rdv_hold_types_id,pentype,pentype.deductiontype ,pc.rdv_Hold_Percentage as penaltypercent, sum(pc.rdv_Hold_Amount) as penaltyamt, pc.Action_Reason as actionreason, pc.Action_Justification as actionjustfication, ");
      selectQuery
          .append("pc.c_bpartner_id, bp.name as bpnametxt, pc.Freeze_rdv_Hold as freezepenalty, ");
      selectQuery.append(
          "pc.rdv_hold_account_type, pc.rdv_hold_uniquecode, com.em_efin_uniquecode ,com.em_efin_uniquecodename, "
              + "(case when pc.c_bpartner_id is not null then  concat(bp.em_efin_documentno,'-',bp.name) else '' end)  as bpname,pc.istxn ");

      fromQuery.append(" from efin_rdv_hold_action pc ");
      fromQuery.append(
          "left join c_validcombination com on com.c_validcombination_id = pc.rdv_hold_uniquecode "
              + " left join efin_rdv_hold_types pentype on pentype.efin_rdv_hold_types_id = pc.efin_rdv_hold_types_id "
              + " left join eut_deflookups_typeln ls on ls.eut_deflookups_typeln_id=pentype.deductiontype   "
              + " left join c_bpartner bp on bp.c_bpartner_id = pc.c_bpartner_id ");
      fromQuery.append(" left join c_invoice inv on inv.c_invoice_id = pc.c_invoice_id  "
          + "left join ad_ref_list actiontype on actiontype.value=pc.action and actiontype.ad_reference_id='11538D28FC294B2AB4465B29B0B4BFF3'"
          + " left join ad_ref_list diemtype on diemtype.value=pc.rdv_hold_account_type and diemtype.ad_reference_id='341EEDE3DC20468696320A84BF8049DF' "
          + "left join efin_rdv_hold_action orgpen on orgpen.efin_rdv_hold_action_id=pc.rdv_hold_rel_id "
          + "left join efin_rdvtxnline txnln on txnln.efin_rdvtxnline_id = pc.efin_rdvtxnline_id "
          + "left join efin_rdvtxn txn on txn.efin_rdvtxn_id = txnln.efin_rdvtxn_id ");

      whereClause.append(" where pc.ad_client_id = '").append(clientId).append("' ");
      whereClause.append(" and txnln.efin_rdvtxn_id = '").append(inpRDVTxnId).append(
          "' and pc.istxn='Y' and pc.efin_rdv_budgholdline_id is null   and   orgpen.efin_rdv_budgholdline_id is null ");

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
        if (searchAttr.has("holdtype") && !StringUtils.isEmpty(searchAttr.getString("holdtype")))
          whereClause.append("and ( ls.englishname ilike '%")
              .append(searchAttr.getString("holdtype")).append("%'  or ls.arabicname ilike  '% ")
              .append(searchAttr.getString("holdtype")).append("%' )");
        if (searchAttr.has("holdpercentage")
            && !StringUtils.isEmpty(searchAttr.getString("holdpercentage"))) {
          if (searchAttr.getString("holdpercentage").equals("0")) {
            whereClause.append("and pc.rdv_hold_Percentage= ")
                .append(searchAttr.getString("holdpercentage"));

          } else {
            whereClause.append("and CAST(pc.rdv_Hold_Percentage AS text) ilike '%")
                .append(searchAttr.getString("holdpercentage")).append("%' ");
          }
        }
        if (searchAttr.has("holdamount")
            && !StringUtils.isEmpty(searchAttr.getString("holdamount"))) {
          if (searchAttr.getString("holdamount").equals("0")) {
            whereClause.append("and pc.rdv_Hold_Amount= ")
                .append(searchAttr.getString("holdamount"));

          } else {
            whereClause.append("and  CAST(pc.rdv_Hold_Amount AS text)  ilike '%")
                .append(searchAttr.getString("holdyamount")).append("%' ");
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
        if (searchAttr.has("freezehold")
            && !StringUtils.isEmpty(searchAttr.getString("freezehold")))
          whereClause.append("and pc.Freeze_rdv_Hold ilike '%")
              .append(searchAttr.getString("freezehold")).append("%' ");
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
        // if (searchAttr.has("accounttype")
        // && !StringUtils.isEmpty(searchAttr.getString("accounttype")))
        // whereClause.append("and diemtype.name ilike '%")
        // .append(searchAttr.getString("accounttype")).append("%' ");
        // if (searchAttr.has("uniquecode")
        // && !StringUtils.isEmpty(searchAttr.getString("uniquecode")))
        // whereClause.append("and com.em_efin_uniquecode ilike '%")
        // .append(searchAttr.getString("uniquecode")).append("%' ");
        // if (searchAttr.has("uniquename")
        // && !StringUtils.isEmpty(searchAttr.getString("uniquename")))
        // whereClause.append("and com.em_efin_uniquecodename ilike '%")
        // .append(searchAttr.getString("uniquename")).append("%' ");

      }
      whereClause.append(
          "group by pc.txnGroupRef,txnln.efin_rdvtxn_id,pc.trx_app_no,pc.action,pc.Action_Date,pc.efin_rdv_hold_types_id,pentype,pentype.deductiontype, "
              + "pc.rdv_Hold_Percentage, pc.Action_Reason, pc.Action_Justification,pc.c_bpartner_id, bp.name, pc.Freeze_rdv_Hold,pc.rdv_hold_account_type, "
              + "pc.rdv_hold_uniquecode, com.em_efin_uniquecode ,com.em_efin_uniquecodename, pc.c_bpartner_id, bp.em_efin_documentno,bp.name,pc.istxn ");

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
      else if (searchAttr.getString("sortName").equals("holdtype"))
        orderClause.append("pentype.deductiontype");
      else if (searchAttr.getString("sortName").equals("holdpercentage"))
        orderClause.append("pc.rdv_Hold_Percentage");
      else if (searchAttr.getString("sortName").equals("penaltyamount"))
        orderClause.append("pc.rdv_Hold_Amount");
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
      // else if (searchAttr.getString("sortName").equals("accounttype"))
      // orderClause.append("pc.penalty_account_type");
      //
      // else if (searchAttr.getString("sortName").equals("uniquecode"))
      // orderClause.append("com.em_efin_uniquecode");
      // else if (searchAttr.getString("sortName").equals("uniquename"))
      // orderClause.append("com.em_efin_uniquecodename");
      else if (searchAttr.getString("sortName").equals("freezehold"))
        orderClause.append("pc.Freeze_rdv_Hold");

      else
        orderClause.append("pc.trx_app_no");
      orderClause.append(" ").append(searchAttr.getString("sortType"));

      // Get Row Count
      // SQLQuery query = null;
      // List<Object> statusList = new ArrayList<>();
      // query = OBDal.getInstance().getSession()
      // .createSQLQuery(selectQuery.append(fromQuery).append(whereClause).toString());
      // statusList = query.list();
      // statusList.size();
      // if (statusList.size() > 0)
      // totalRecord = statusList.size();

      ps = conn.prepareStatement(countQuery.append(fromQuery).append(whereClause).toString());
      log4j.debug("Hold count:" + ps.toString());
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

      // Hold Details
      ps1 = conn.prepareStatement((selectQuery.append(fromQuery).append(whereClause)
          .append(orderClause).append(" limit ").append(searchAttr.getInt("limit"))
          .append(" offset ").append(searchAttr.getInt("offset"))).toString());
      log4j.debug("Hold:" + ps1.toString());
      rs1 = ps1.executeQuery();
      while (rs1.next()) {
        json = new JSONObject();
        if (rs1.getDate("actiondate") != null) {
          date = df.format(rs1.getDate("actiondate"));
          date = dateYearFormat.format(df.parse(date));
          date = UtilityDAO.convertTohijriDate(date);
        }
        json.put("id", Utility.nullToEmpty(rs1.getString("id")));
        json.put("Sequence", "10");
        json.put("actions", "");
        json.put("appno", Utility.nullToEmpty(rs1.getString("trx_app_no")));
        json.put("actiontype", Utility.nullToEmpty(rs1.getString("action")));
        json.put("actionDate", Utility.nullToEmpty(date));
        json.put("Amt", Utility.nullToEmpty(rs1.getString("amount")));
        // EfinRDVTxnline rdvTxnLine = OBDal.getInstance().get(EfinRDVTxnline.class,
        // inpRDVTxnLineId);
        EfinRDVTransaction rdvTxn = OBDal.getInstance().get(EfinRDVTransaction.class, inpRDVTxnId);
        json.put("NetAmt", rdvTxn.getNetmatchAmt());
        json.put("holdtype", Utility.nullToEmpty(rs1.getString("efin_rdv_hold_types_id")));
        json.put("holdpercentage", Utility.nullToEmpty(rs1.getString("penaltypercent")));
        json.put("holdamount", Utility.nullToEmpty(rs1.getString("penaltyamt")));
        json.put("actionreason", Utility.nullToEmpty(rs1.getString("actionreason")));
        json.put("actionjustfication", Utility.nullToEmpty(rs1.getString("actionjustfication")));
        json.put("bpartnerId", Utility.nullToEmpty(rs1.getString("c_bpartner_id")));
        json.put("bpartnername", Utility.nullToEmpty(rs1.getString("bpname")));
        json.put("associatedbp", Utility.nullToEmpty(rs1.getString("bpname")));
        json.put("bpname", Utility.nullToEmpty(rs1.getString("bpnametxt")));
        json.put("freezehold", Utility.nullToEmpty(rs1.getString("freezepenalty")));
        json.put("amarsarfno", "0");
        json.put("amarsarfamount", "0");
        // json.put("accounttype", Utility.nullToEmpty(rs1.getString("penalty_account_type")));
        // json.put("uniquecodeId", Utility.nullToEmpty(rs1.getString("penalty_uniquecode")));
        // json.put("uniquecode", Utility.nullToEmpty(rs1.getString("em_efin_uniquecode")));
        // json.put("uniquecodehid", Utility.nullToEmpty(rs1.getString("em_efin_uniquecode")));
        // json.put("uniquename", Utility.nullToEmpty(rs1.getString("em_efin_uniquecodename")));
        json.put("deductiontype", Utility.nullToEmpty(rs1.getString("deductiontype")));
        json.put("holdrelId", "0");
        json.put("orgHoldAmt", "0");
        json.put("isTxn", Utility.nullToEmpty(rs1.getString("istxn")));
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

  @Override
  public int getHoldListCount(String clientId, String inpRDVTxnLineId) {
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
  //
  // @Override
  // public List<HoldActionVO> getHoldList(String clientId, String inpRDVTxnLineId,
  // HoldActionVO vo, int limit, int offset, String sortColName, String sortColType,
  // String searchFlag) {
  // PreparedStatement st = null;
  // ResultSet rs = null;
  // List<PenaltyActionVO> ls = new ArrayList<PenaltyActionVO>();
  // String sqlQuery = "";
  // DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
  // SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
  // String date = "";
  // try {
  //
  // sqlQuery = " select
  // efin_penalty_action_id,seqno,trx_app_no,action,Action_Date,Amount,efin_penalty_types_id,
  // Penalty_Percentage, Penalty_Amount, Action_Reason, Action_Justification,"
  // + "efin_penalty_action.c_bpartner_id,BP_Name, Freeze_Penalty,
  // c_invoice_id,Amarsarf_Amount,penalty_account_type,penalty_uniquecode,com.em_efin_uniquecode
  // ,com.em_efin_uniquecodename "
  // + "from efin_penalty_action left join c_validcombination com on
  // com.c_validcombination_id=efin_penalty_action.penalty_uniquecode where efin_rdvtxnline_id = ?
  // ";
  // /*
  // * if (searchFlag.equals("true")) { if (vo.getElement() != null) sqlQuery +=
  // * " and ln.payroll ilike '%" + vo.getElement() + "%'"; if (vo.getPercentage() != null)
  // * sqlQuery += " and ln.percentage ilike '%" + vo.getPercentage() + "%'"; if (vo.getValue() !=
  // * null) sqlQuery += " and ln.contractvalue ='" + vo.getValue() + "'";
  // *
  // * } if (sortColName != null && sortColName.equals("element")) sqlQuery +=
  // * " order by payroll " + sortColType + " limit " + limit + " offset " + offset; else if
  // * (sortColName != null && sortColName.equals("Percentage")) sqlQuery +=
  // * " order by percentage " + sortColType + " limit " + limit + " offset " + offset; else if
  // * (sortColName != null && sortColName.equals("value")) sqlQuery += " order by contractvalue "
  // * + sortColType + " limit " + limit + " offset " + offset; else sqlQuery +=
  // * " order by created " + sortColType + " limit " + limit + " offset " + offset;
  // * log4j.debug("DAO select Query:" + sqlQuery + ">> contractId:" + contractId);
  // */
  // st = conn.prepareStatement(sqlQuery);
  // st.setString(1, inpRDVTxnLineId);
  // rs = st.executeQuery();
  // log4j.debug("sqlQuery" + sqlQuery.toString());
  // while (rs.next()) {
  // PenaltyActionVO cVO = new PenaltyActionVO();
  // cVO.setEfin_penalty_action_id(rs.getString("efin_penalty_action_id"));
  // cVO.setSeqno(rs.getString("seqno"));
  // cVO.setTrx_app_no(rs.getString("trx_app_no"));
  // cVO.setAction(rs.getString("action"));
  // if (rs.getDate("Action_Date") != null) {
  // date = df.format(rs.getDate("Action_Date"));
  // date = dateYearFormat.format(df.parse(date));
  // date = UtilityDAO.convertTohijriDate(date);
  // cVO.setActionDate(date);
  // } else
  // cVO.setActionDate(null);
  //
  // cVO.setAmount(rs.getString("Amount"));
  // cVO.setPenaltyType(rs.getString("efin_penalty_types_id"));
  // cVO.setPenaltyPercentage(rs.getString("Penalty_Percentage"));
  // cVO.setPenaltyamount(rs.getString("Penalty_Amount"));
  // cVO.setActionReason(rs.getString("Action_Reason"));
  // cVO.setActionJustification(rs.getString("Action_Justification"));
  // cVO.setBpartnerid(rs.getString("c_bpartner_id"));
  // cVO.setBpartnername(rs.getString("BP_Name"));
  // cVO.setFreezePenalty(rs.getString("Freeze_Penalty"));
  // cVO.setInvoiceId(rs.getString("c_invoice_id") == null ? "" : rs.getString("c_invoice_id"));
  // cVO.setAmarsarfAmount(
  // rs.getString("Amarsarf_Amount") == null ? "0" : rs.getString("Amarsarf_Amount"));
  //
  // /*
  // * if(rs.getString("penalty_account_type")!=null &&
  // * rs.getString("penalty_account_type").equals("BJ")) cVO.setPenaltyaccount_type("Budget");
  // * else cVO.setPenaltyaccount_type("Budget Adjustment");
  // */
  //
  // cVO.setPenaltyaccount_type(rs.getString("penalty_account_type"));
  // cVO.setPenaltyuniquecode(
  // rs.getString("penalty_uniquecode") == null ? "" : rs.getString("penalty_uniquecode"));
  //
  // cVO.setUniquecodeName(rs.getString("em_efin_uniquecodename") == null ? ""
  // : rs.getString("em_efin_uniquecodename"));
  //
  // ls.add(cVO);
  // }
  // } catch (final SQLException e) {
  // log4j.error("", e);
  // } catch (final Exception e) {
  // log4j.error("", e);
  // } finally {
  // try {
  // st.close();
  // rs.close();
  // } catch (final SQLException e) {
  // log4j.error("", e);
  // }
  // }
  // return ls;
  // }

  @Override
  public List<RdvHoldActionVO> getbpartnername(String clientId, String rdvtrxlineId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String sql = "";
    List<RdvHoldActionVO> ls = new ArrayList<RdvHoldActionVO>();
    try {
      sql = "select c_bpartner.c_bpartner_id,concat(em_efin_documentno,'-',name) as bpname from c_bpartner   left join efin_penalty_action act on act.c_bpartner_id = c_bpartner.c_bpartner_id \n"
          + " where c_bpartner.ad_client_id=?  and act.efin_rdvtxnline_id= ? ";
      st = conn.prepareStatement(sql);
      st.setString(1, clientId);
      st.setString(2, rdvtrxlineId);
      log4j.debug("getbpartnername:" + st.toString());
      rs = st.executeQuery();
      while (rs.next()) {
        RdvHoldActionVO eVO = new RdvHoldActionVO();
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

  @Override
  public List<RdvHoldActionVO> getHoldType(String clientId, String action, String lang) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String sql = "";
    List<RdvHoldActionVO> ls = new ArrayList<RdvHoldActionVO>();
    try {
      if (lang.equals("ar_SA"))
        sql = "  select efin_rdv_hold_types_id, coalesce(lkln.arabicname,lkln.englishname) as name,typ.threshold from efin_rdv_hold_types  typ "
            + " join eut_deflookups_typeln lkln on lkln.eut_deflookups_typeln_id= typ.deductiontype and lkln.value not in ('90') join eut_deflookups_type lkhd on lkhd.eut_deflookups_type_id=lkln.eut_deflookups_type_id and lkhd.value = 'HOLD_TYPE' "
            + "where typ.maintain_enable='Y' " + " and typ.ad_client_id=?";
      else
        sql = "  select efin_rdv_hold_types_id,  lkln.englishname as name,typ.threshold from efin_rdv_hold_types  typ "
            + "join eut_deflookups_typeln lkln on lkln.eut_deflookups_typeln_id= typ.deductiontype and lkln.value not in ('90') join eut_deflookups_type lkhd on lkhd.eut_deflookups_type_id=lkln.eut_deflookups_type_id and lkhd.value = 'HOLD_TYPE' "
            + "where typ.maintain_enable='Y' " + " and typ.ad_client_id=?";
      if (action != null && action.equals("RM"))
        sql += " and  (typ.threshold is null or typ.threshold = 0)  ";
      sql = String.format(sql);
      st = conn.prepareStatement(sql);
      st.setString(1, clientId);
      log4j.debug("getHoldType:" + st.toString());
      rs = st.executeQuery();
      while (rs.next()) {
        RdvHoldActionVO eVO = new RdvHoldActionVO();
        eVO.setHoldId(Utility.nullToEmpty(rs.getString("efin_rdv_hold_types_id")));
        eVO.setHoldname(Utility.nullToEmpty(rs.getString("name")));
        eVO.setThershold(rs.getString("threshold") == null ? "0" : rs.getString("threshold"));
        ls.add(eVO);
      }
    } catch (final SQLException e) {
      log4j.error("Exception in getHoldType", e);
      return ls;
    } catch (final Exception e) {
      log4j.error("Exception in getHoldType", e);
      return ls;
    } finally {
      try {
        st.close();
        rs.close();
      } catch (final SQLException e) {
        log4j.error("Exception in getHoldType", e);
        return ls;
      }
    }
    return ls;
  }

  @Override
  public List<RdvHoldActionVO> getBudgetAdjustmentUniquecode(String clientId, String rdvtrxlineId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String sql = "";
    List<RdvHoldActionVO> ls = new ArrayList<RdvHoldActionVO>();
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
        RdvHoldActionVO eVO = new RdvHoldActionVO();
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

  @Override
  public List<RdvHoldActionVO> getinvoiceno(String clientId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String sql = "";
    List<RdvHoldActionVO> ls = new ArrayList<RdvHoldActionVO>();
    try {
      sql = "select c_invoice_id,documentno from c_invoice\n" + " where ad_client_id=?";
      st = conn.prepareStatement(sql);
      st.setString(1, clientId);
      log4j.debug("getinvoiceno:" + st.toString());
      rs = st.executeQuery();
      while (rs.next()) {
        RdvHoldActionVO eVO = new RdvHoldActionVO();
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

  @Override
  public String getHoldAction(String seqno, String trxappno, String clientId, String actiontype,
      String actionDate, String amount, String penalty_type, String penalty_per, String penalty_amt,
      String actreason, String actionjus, String bpartnerId, String bpname, String freezepenalty,
      String invoice, String amarsarfamount, String RDVTrxlineId, String penalty_account,
      String uniquecode, Boolean isRdvSaveAction) {

    String sql = "";
    Long number = Long.parseLong(seqno);

    try {
      OBContext.setAdminMode();
      EfinRDVTxnline rdvline = OBDal.getInstance().get(EfinRDVTxnline.class, RDVTrxlineId);
      EfinRdvHoldAction action = OBProvider.getInstance().get(EfinRdvHoldAction.class);
      action.setClient(OBDal.getInstance().get(Client.class, clientId));
      action.setSequenceNumber(number);
      action.setTxnApplicationNo(rdvline.getTrxappNo());
      action.setAction(actiontype);
      if (actionDate != null && actionDate != "")
        action.setActionDate(convertGregorian(actionDate));
      if (!isRdvSaveAction)
        action.setAmount(rdvline.getMatchAmt());
      else {
        action.setAmount(new BigDecimal(amount));
      }
      action.setEfinRdvHoldTypes(OBDal.getInstance().get(EfinRdvHoldTypes.class, penalty_type));
      if (penalty_per != null && penalty_per != "")
        action.setRDVHoldPercentage(new BigDecimal(penalty_per));
      action.setRDVHoldAmount(new BigDecimal(penalty_amt.replaceAll(",", "")));
      action.setActionReason(actreason);
      action.setActionJustification(actionjus);
      if (bpartnerId != null && bpartnerId != "")
        action.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, bpartnerId));
      action.setName(bpname);
      action.setEfinRdvtxnline(rdvline);
      if (freezepenalty.equals("Y")) {
        action.setFreezeRdvHold(true);
      } else {
        action.setFreezeRdvHold(false);
      }
      if (invoice != null) {
        action.setInvoice(
            OBDal.getInstance().get(org.openbravo.model.common.invoice.Invoice.class, invoice));
        action.setAmrasarfAmount(new BigDecimal(amarsarfamount));
      }
      // action.setRDVHoldAccountType(penalty_account);
      // if (uniquecode != null)
      // action
      // .setRDVHoldUniquecode(OBDal.getInstance().get(AccountingCombination.class, uniquecode));
      OBDal.getInstance().save(action);
      OBDal.getInstance().flush();

      insertHoldHeader(action, action.getEfinRdvtxnline(), action.getRDVHoldAmount(), null, null);
    } catch (final Exception e) {
      log4j.error("Exception in RdvHoldActionDAO,getHoldAction", e);

    } finally {
      OBContext.restorePreviousMode();
    }
    return sql;
  }

  @Override
  public String getHoldTxnAction(String seqno, String trxappno, String clientId, String actiontype,
      String actionDate, String amount, String penalty_type, String penalty_per, String hold_amt,
      String actreason, String actionjus, String bpartnerId, String bpname, String freezepenalty,
      String invoice, String amarsarfamount, String RDVTxnId, String penalty_account,
      String uniquecode, Boolean isRdvSaveAction) {
    String sql = "";
    Long number = Long.parseLong(seqno);

    try {
      OBContext.setAdminMode();

      // get matched lines
      EfinRDVTransaction transaction = OBDal.getInstance().get(EfinRDVTransaction.class, RDVTxnId);
      // BigDecimal totalMatch = transaction.getMatchAmt().subtract(transaction.getADVDeduct())
      // .subtract(transaction.getPenaltyAmt());
      BigDecimal totalMatch = transaction.getNetmatchAmt();
      List<EfinRDVTxnline> lineList = transaction.getEfinRDVTxnlineList();
      String ref = SequenceIdData.getUUID();
      for (EfinRDVTxnline line : lineList) {
        if (line.isMatch()) {
          // BigDecimal weigtage = (((line.getMatchAmt().subtract(line.getPenaltyAmt())
          // .subtract(line.getADVDeduct())).divide(totalMatch, 6, BigDecimal.ROUND_HALF_EVEN))
          // .multiply(new BigDecimal(hold_amt))).setScale(2, RoundingMode.HALF_UP);
          BigDecimal weigtage = (((line.getNetmatchAmt()).divide(totalMatch, 6,
              BigDecimal.ROUND_HALF_EVEN)).multiply(new BigDecimal(hold_amt))).setScale(2,
                  RoundingMode.HALF_UP);
          EfinRDVTxnline rdvline = line;
          EfinRdvHoldAction action = OBProvider.getInstance().get(EfinRdvHoldAction.class);
          action.setTxngroupref(ref);
          action.setClient(OBDal.getInstance().get(Client.class, clientId));
          action.setSequenceNumber(number);
          action.setTxnApplicationNo(transaction.getTXNVersion().toString());
          action.setAction(actiontype);
          if (actionDate != null && actionDate != "")
            action.setActionDate(convertGregorian(actionDate));
          if (!isRdvSaveAction)
            action.setAmount(rdvline.getMatchAmt());
          else {
            action.setAmount(new BigDecimal(amount));
          }
          action.setEfinRdvHoldTypes(OBDal.getInstance().get(EfinRdvHoldTypes.class, penalty_type));
          if (penalty_per != null && penalty_per != "")
            action.setRDVHoldPercentage(new BigDecimal(penalty_per));
          else
            action.setRDVHoldPercentage(new BigDecimal(0));
          action.setRDVHoldAmount(weigtage);
          action.setActionReason(actreason);
          action.setActionJustification(actionjus);
          if (bpartnerId != null && bpartnerId != "")
            action.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, bpartnerId));
          action.setName(bpname);
          action.setEfinRdvtxnline(rdvline);
          if (freezepenalty.equals("Y")) {
            action.setFreezeRdvHold(true);
          } else {
            action.setFreezeRdvHold(false);
          }
          if (invoice != null) {
            action.setInvoice(
                OBDal.getInstance().get(org.openbravo.model.common.invoice.Invoice.class, invoice));
            action.setAmrasarfAmount(new BigDecimal(amarsarfamount));
          }
          action.setTxn(true);
          // action.setRDVHoldAccountType(penalty_account);
          // if (uniquecode != null)
          // action
          // .setRDVHoldUniquecode(OBDal.getInstance().get(AccountingCombination.class,
          // uniquecode));
          OBDal.getInstance().save(action);
          OBDal.getInstance().flush();

          insertHoldHeader(action, action.getEfinRdvtxnline(), action.getRDVHoldAmount(), null,
              null);
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in RdvHoldActionDAO,getHoldAction", e);

    } finally {
      OBContext.restorePreviousMode();
    }
    return sql;
  }

  @Override
  public void updateHold(EfinRdvHoldAction holdaction, EfinRdvHoldTypes holdType,
      BigDecimal penaltyamt) {
    BigDecimal SumofAddRemPenAmt = BigDecimal.ZERO;
    try {
      SumofAddRemPenAmt = getSumofAddRemovePenaltyAmt(holdaction, holdType);

      OBQuery<EfinRdvHold> hold = OBDal.getInstance().createQuery(EfinRdvHold.class,
          " as e where e.efinRdv.id in ( select e.efinRdv.id from Efin_RDVTxn e where e.id='"
              + holdaction.getEfinRdvtxnline().getEfinRdvtxn().getId() + "') and e.rDVHoldType='"
              + holdaction.getEfinRdvHoldTypes().getDeductionType() + "'");
      hold.setMaxResult(1);
      log4j.debug("penalty:" + hold.getWhereAndOrderBy());
      if (hold.list().size() > 0) {
        EfinRdvHold upHold = hold.list().get(0);
        log4j.debug("getPenaltyAmount:" + penaltyamt);
        upHold.setRDVHoldApplied(SumofAddRemPenAmt);
        OBDal.getInstance().save(upHold);
      }

    } catch (final Exception e) {
      log4j.error("Exception in PenaltyAction", e);

    } finally {

    }
  }

  @Override
  public void updateoldPenalty(EfinRdvHoldAction holdAction, EfinRdvHoldTypes oldHoldTypeId,
      BigDecimal penaltyamt) {
    BigDecimal SumofAddRemPenAmt = BigDecimal.ZERO;

    try {
      SumofAddRemPenAmt = getSumofAddRemovePenaltyAmt(holdAction, oldHoldTypeId);

      if (oldHoldTypeId != null && !holdAction.getEfinRdvHoldTypes().equals(oldHoldTypeId)) {
        OBQuery<EfinPenalty> oldpenalty = OBDal.getInstance().createQuery(EfinPenalty.class,
            " as e where e.efinRdv.id in ( select e.efinRdv.id from Efin_RDVTxn e where e.id='"
                + holdAction.getEfinRdvtxnline().getEfinRdvtxn().getId() + "') and e.rDVHoldType='"
                + oldHoldTypeId.getDeductionType() + "'");
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

  public void deleteHoldHed(EfinRdvHoldAction holdaction) {
    List<EfinRdvHoldHeader> holdhedList = new ArrayList<EfinRdvHoldHeader>();
    JSONObject result = new JSONObject();
    try {
      OBContext.setAdminMode();
      Boolean canDelete = false;
      EfinRdvHoldHeader holdhd = null;
      result = getHoldAmt(holdaction.getEfinRdvtxnline(), "del", holdaction);
      OBQuery<EfinRdvHoldHeader> holdhed = OBDal.getInstance().createQuery(EfinRdvHoldHeader.class,
          " as e where e.efinRdvtxnline.id='" + holdaction.getEfinRdvtxnline().getId() + "'");
      holdhed.setMaxResult(1);
      holdhedList = holdhed.list();
      if (holdhedList.size() > 0) {
        holdhd = holdhedList.get(0);
        log4j.debug(
            "penhd.getEfinPenaltyActionList().size()" + holdhd.getEfinRdvHoldActionList().size());
        if (holdhd.getEfinRdvHoldActionList().size() == 1) {
          canDelete = true;
        } else {
          holdhd
              .setRDVHoldAmount(holdhd.getRDVHoldAmount().subtract(holdaction.getRDVHoldAmount()));
          holdhd.setUpdatedRdvHoldAmt(
              holdhd.getUpdatedRdvHoldAmt().subtract(holdaction.getRDVHoldAmount()));
          OBDal.getInstance().save(holdhd);
          OBDal.getInstance().flush();
        }
      }
      updatePenalty(result, holdaction);
      EfinRdvHoldAction oldHold = holdaction.getRDVHoldRel();
      if (oldHold != null) {
        oldHold.setReleased(false);
        OBDal.getInstance().save(oldHold);
        OBDal.getInstance().flush();

      }

      if (canDelete) {
        OBDal.getInstance().remove(holdhd);
        OBDal.getInstance().flush();
      }
      // OBDal.getInstance().remove(holdaction);
      OBDal.getInstance().getConnection().prepareStatement(" delete from efin_rdv_hold_action "
          + " where efin_rdv_hold_action_id='" + holdaction.getId() + "' ").execute();
      //

    } catch (final Exception e) {
      log4j.error("Exception in deletepenaltyHed", e);

    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @SuppressWarnings("rawtypes")
  public static BigDecimal getSumofAddRemovePenaltyAmt(EfinRdvHoldAction action,
      EfinRdvHoldTypes oldHoldtype) {
    BigDecimal sumOfAddRemPenAmt = BigDecimal.ZERO;
    String sqlQry = null;
    Query qry = null;
    try {
      sqlQry = " select distinct   coalesce(( select coalesce(sum(pen.rdv_hold_amount),0) from efin_rdv_hold_action pen    "
          + "  where pen.efin_rdvtxnline_id=act.efin_rdvtxnline_id   and pen.action='AD' "
          + " and pen.efin_rdv_hold_types_id=act.efin_rdv_hold_types_id  ) -"
          + "         ( select coalesce(sum(pen.rdv_hold_amount),0) from efin_rdv_hold_action pen      "
          + "  where pen.efin_rdvtxnline_id=act.efin_rdvtxnline_id "
          + "    and pen.action='RM'  and pen.efin_rdv_hold_types_id=act.efin_rdv_hold_types_id   ),0) "
          + " as total  from efin_rdv_hold_action act "
          + "    where act.efin_rdvtxnline_id=  ? and act.efin_rdv_hold_types_id= ?  ";// and
                                                                                       // pen.efin_penalty_action_id
                                                                                       // <> ?
      qry = OBDal.getInstance().getSession().createSQLQuery(sqlQry);
      qry.setParameter(0, action.getEfinRdvtxnline().getId());
      if (oldHoldtype == null && action.getEfinRdvHoldTypes() != null)
        qry.setParameter(1, action.getEfinRdvHoldTypes().getId());
      else if (oldHoldtype != null)
        qry.setParameter(1, oldHoldtype.getId());

      List queryList = qry.list();
      if (queryList != null && queryList.size() > 0) {
        log4j.debug("get:" + queryList.get(0));
        sumOfAddRemPenAmt = (BigDecimal) queryList.get(0);
      }
    } catch (final Exception e) {
      log4j.error("Exception in getSumofAddRemoveHoldAmt", e);

    } finally {

    }
    return sumOfAddRemPenAmt;

  }

  @Override
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

  @SuppressWarnings("rawtypes")
  public static JSONObject getHoldAmt(EfinRDVTxnline trxline, String actiontype,
      EfinRdvHoldAction holdaction) {
    JSONObject result = new JSONObject(), json = null;
    JSONArray array = new JSONArray();
    String sqlqry = null;
    Query qry = null;
    BigDecimal reduceAmt = BigDecimal.ZERO;
    try {
      result.put("totalamount", "0");
      sqlqry = "    select  act.efin_rdv_hold_types_id,  "
          + "               SUM(CASE WHEN act.action='AD'  THEN rdv_hold_amount ELSE 0 END) + "
          + "               SUM(CASE WHEN act.action='RM'  THEN rdv_hold_amount ELSE 0 END) as total "
          + "   from efin_rdv_hold_action act   "
          + "             join efin_rdvtxnline ln on ln.efin_rdvtxnline_id= act.efin_rdvtxnline_id  "
          + "            join efin_rdvtxn trx on trx.efin_rdvtxn_id=ln.efin_rdvtxn_id  "
          + "            join efin_rdv rdv on rdv.efin_rdv_id= trx.efin_rdv_id    where  rdv.efin_rdv_id= ?  ";
      // if(actiontype!=null && actiontype.equals("del"))
      // sqlqry += " and act.efin_penalty_action_id<> ?";
      sqlqry += "              group by act.efin_rdv_hold_types_id ";
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
            json.put("holdtype", row[0].toString());
            if (actiontype != null && actiontype.equals("del")) {
              if (holdaction.getEfinRdvHoldTypes() != null
                  && holdaction.getEfinRdvHoldTypes().getId().equals(row[0].toString())) {
                reduceAmt = new BigDecimal(row[1].toString())
                    .subtract(holdaction.getRDVHoldAmount());
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
        result.put("holdlist", array);
        result.put("totalamount", new BigDecimal(result.getString("totalamount"))
            .add(new BigDecimal(json.getString("amount"))));
      }
      log4j.debug("result;" + result);
    } catch (Exception e) {
      log4j.error("Exception in getPenaltyAmt", e);

    }
    return result;
  }

  @SuppressWarnings("rawtypes")
  public static BigDecimal getHoldheaderAmt(EfinRDVTxnline trxline) {
    String sqlqry = null;
    Query qry = null;
    BigDecimal calHoldheaderAmt = BigDecimal.ZERO;
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

      sqlqry = "  select (coalesce(sum(act.rdv_hold_amount),0)) as totpenamt , "
          + " act.efin_rdvtxnline_id from efin_rdv_hold_action act"
          + "     where act.efin_rdvtxnline_id=  ?  "
          + "      group by act.efin_rdvtxnline_id,act.efin_rdv_hold_types_id ";
      qry = OBDal.getInstance().getSession().createSQLQuery(sqlqry);
      qry.setParameter(0, trxline.getId());
      log4j.debug("qry:" + qry);
      List getHoldAmt = qry.list();
      if (getHoldAmt != null && getHoldAmt.size() > 0) {
        for (Iterator iterator = getHoldAmt.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          calHoldheaderAmt = calHoldheaderAmt.add(new BigDecimal(row[0].toString()));
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in getHoldheaderAmt", e);

    }
    return calHoldheaderAmt;
  }

  public void updatePenalty(JSONObject result, EfinRdvHoldAction holdaction) {
    JSONObject json = null;
    try {
      if (result != null) {
        JSONArray array = result.getJSONArray("holdlist");
        for (int i = 0; i < array.length(); i++) {
          json = array.getJSONObject(i);
          BigDecimal holdApplied = new BigDecimal(json.getString("amount"));

          EfinRdvHoldTypes penaltytype = OBDal.getInstance().get(EfinRdvHoldTypes.class,
              json.getString("holdtype"));
          OBQuery<EfinRdvHold> updateHoldQry = OBDal.getInstance().createQuery(EfinRdvHold.class,
              " as e where e.efinRdv.id in ( select e.efinRdv.id from Efin_RDVTxn e where e.id='"
                  + holdaction.getEfinRdvtxnline().getEfinRdvtxn().getId() + "') "
                  + " and e.rDVHoldType='" + penaltytype.getDeductionType().getId() + "'");
          updateHoldQry.setMaxResult(1);
          log4j.debug("penalty:" + updateHoldQry.getWhereAndOrderBy());
          if (updateHoldQry.list().size() > 0) {
            EfinRdvHold upoldpenalty = updateHoldQry.list().get(0);
            upoldpenalty.setRDVHoldApplied(new BigDecimal(json.getString("amount")));
            if (!(upoldpenalty.getOpeningHoldAmount().compareTo(BigDecimal.ZERO) == 0)) {
              upoldpenalty
                  .setRDVHoldRemaining(upoldpenalty.getOpeningHoldAmount().subtract(holdApplied));
            }
            OBDal.getInstance().save(upoldpenalty);

            if (holdApplied.compareTo(BigDecimal.ZERO) == 0) {
              deleteRdvHold(upoldpenalty.getId(), penaltytype, holdaction);
            }
          } else {
            insertRdvHold(holdaction, penaltytype, holdApplied);
          }
        }
        OBDal.getInstance().flush();
      }

    } catch (final Exception e) {
      log4j.error("Exception in updatePenalty", e);

    } finally {

    }
  }

  /**
   * Method to insert hold if hold does not exists based on RDV header
   * 
   * @param holdaction
   * @param holdType
   * @param holdApplied
   */
  public void insertRdvHold(EfinRdvHoldAction holdaction, EfinRdvHoldTypes holdType,
      BigDecimal holdApplied) {
    try {
      BigDecimal threshold = BigDecimal.ZERO;
      BigDecimal openholdAmt = BigDecimal.ZERO;
      BigDecimal percent = new BigDecimal("0.01");

      EfinRDV rdv = holdaction.getEfinRdvtxnline().getEfinRdvtxn().getEfinRdv();
      if (holdType.getThreshold() != null) {
        threshold = holdType.getThreshold().multiply(percent);
      }
      openholdAmt = threshold.multiply(rdv.getContractAmt());

      // insert hold
      EfinRdvHold hold = OBProvider.getInstance().get(EfinRdvHold.class);
      hold.setClient(holdaction.getClient());
      hold.setOrganization(holdaction.getOrganization());
      hold.setEfinRdv(rdv);
      hold.setRDVHoldType(holdType.getDeductionType());
      hold.setAlertStatus(rdv.getPenaltyStatus());
      hold.setRDVHoldApplied(holdApplied);
      if (!(openholdAmt.compareTo(BigDecimal.ZERO) == 0)) {
        hold.setRDVHoldRemaining(openholdAmt.subtract(holdApplied));
      }
      hold.setRDVHoldPercentage(holdType.getThreshold());
      hold.setOpeningHoldAmount(openholdAmt);
      OBDal.getInstance().save(hold);
      OBDal.getInstance().flush();

    } catch (final Exception e) {
      log4j.error("Exception in insertRdvHold", e);
    }
  }

  /**
   * Method to delete Hold if the hold type is not used in other lines
   * 
   * @param oldholdId
   * @param holdType
   * @param holdaction
   */
  public void deleteRdvHold(String oldholdId, EfinRdvHoldTypes holdType,
      EfinRdvHoldAction holdaction) {
    Boolean canRemove = Boolean.TRUE;
    try {
      EfinRDV rdv = holdaction.getEfinRdvtxnline().getEfinRdvtxn().getEfinRdv();
      OBQuery<EfinRdvHoldAction> holdAction = OBDal.getInstance().createQuery(
          EfinRdvHoldAction.class,
          " as e where e.efinRdvtxnline.efinRdv.id=:rdvId and e.efinRdvHoldTypes.id=:deductType "
              + "and e.efinRdvtxnline.id<>:currentlineId");
      holdAction.setNamedParameter("rdvId", rdv.getId());
      holdAction.setNamedParameter("deductType", holdType.getId());
      holdAction.setNamedParameter("currentlineId", holdaction.getEfinRdvtxnline().getId());

      if (holdAction.list().size() > 0) {
        canRemove = Boolean.FALSE;
      }
      if (canRemove) {
        EfinRdvHold hold = OBDal.getInstance().get(EfinRdvHold.class, oldholdId);
        OBDal.getInstance().remove(hold);
        OBDal.getInstance().flush();
      }
    } catch (final Exception e) {
      log4j.error("Exception in deleteRdvHold", e);
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
  public void insertHoldHeader(EfinRdvHoldAction holdaction, EfinRDVTxnline rdvtrxln,
      BigDecimal holdamt, EfinRdvHoldTypes oldHoldTypeId, String oldAction) {
    EfinRDVTransaction previousrdvtrx = null;
    EfinRDVTxnline previousrdxtrxln = null;
    BigDecimal prevpenhdAmt = BigDecimal.ZERO;
    JSONObject result = new JSONObject();
    try {
      OBContext.setAdminMode();
      // get json object of penalty amount on each penalty types based on rdv header and penalty
      // type
      result = getHoldAmt(holdaction.getEfinRdvtxnline(), null, holdaction);

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
        OBQuery<EfinRdvHoldHeader> prevpenhd = OBDal.getInstance().createQuery(
            EfinRdvHoldHeader.class, " as e where e.efinRdvtxnline.id='" + previousrdxtrxln.getId()
                + "' and e.efinRdvtxn.id='" + previousrdvtrx.getId() + "' ");
        prevpenhd.setMaxResult(1);
        if (prevpenhd.list().size() > 0) {
          prevpenhdAmt = prevpenhd.list().get(0).getUpdatedRdvHoldAmt();
        } else
          prevpenhdAmt = BigDecimal.ZERO;
      }

      // check if penalty header exists or not based on current rdv transaction line and rdv
      // transaction id
      OBQuery<EfinRdvHoldHeader> holdhdexisting = OBDal.getInstance().createQuery(
          EfinRdvHoldHeader.class, " as e where e.efinRdvtxnline.id='" + rdvtrxln.getId()
              + "' and e.efinRdvtxn.id='" + rdvtrxln.getEfinRdvtxn().getId() + "'");
      holdhdexisting.setMaxResult(1);
      // if exists penalty header update penalty amount and update penalty amount
      if (holdhdexisting.list().size() > 0) {
        // if difference of old penalty amount and new penalty amunt not zero or action changed, or
        // penalty type changed
        if (holdamt.compareTo(BigDecimal.ZERO) != 0
            || (oldAction != null && !oldAction.equals(holdaction.getAction()))
            || (oldHoldTypeId != null
                && !oldHoldTypeId.equals(holdaction.getEfinRdvHoldTypes().getId()))) {

          EfinRdvHoldHeader holdhd = holdhdexisting.list().get(0);
          // get penalty header amount
          holdamt = getHoldheaderAmt(holdaction.getEfinRdvtxnline());
          log4j.debug("penaltyamt:" + holdamt);
          holdhd.setRDVHoldAmount(holdamt);
          holdhd.setUpdatedRdvHoldAmt(prevpenhdAmt.add(holdamt));
          OBDal.getInstance().save(holdhd);

          holdaction.setEfinRdvHoldHeader(holdhd);
          OBDal.getInstance().save(holdhd);

          updatePenalty(result, holdaction);
          OBDal.getInstance().flush();

        }
      } else {
        EfinRdvHoldHeader penltyhd = OBProvider.getInstance().get(EfinRdvHoldHeader.class);
        penltyhd.setClient(holdaction.getClient());
        penltyhd.setOrganization(holdaction.getOrganization());
        penltyhd.setLineNo(holdaction.getEfinRdvtxnline().getTrxlnNo());
        penltyhd.setEfinRdvtxn(rdvtrxln.getEfinRdvtxn());
        penltyhd.setEfinRdvtxnline(rdvtrxln);
        penltyhd.setExistingRdvHold(prevpenhdAmt);
        penltyhd.setRDVHoldAmount(holdamt);
        penltyhd
            .setUpdatedRdvHoldAmt(penltyhd.getExistingRdvHold().add(penltyhd.getRDVHoldAmount()));
        OBDal.getInstance().save(penltyhd);
        holdaction.setEfinRdvHoldHeader(penltyhd);
        OBDal.getInstance().save(holdaction);

        if (result != null) {
          updatePenalty(result, holdaction);
        }
        OBDal.getInstance().flush();
      }

    } catch (final Exception e) {
      log4j.error("Exception in PenaltyAction", e);

    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @SuppressWarnings("rawtypes")
  public static String chkHoldtypeGoingdownNegativeval(EfinRDVTxnline line,
      BigDecimal currentPenaltyAmt, EfinRdvHoldTypes holdType, String actionId, String actiontype) {
    BigDecimal diffofexpentocurpen = BigDecimal.ZERO;
    BigDecimal existPenaltyAmt = BigDecimal.ZERO;
    String sqlqry = null;
    Query qry = null;
    try {
      if (line != null) {

        sqlqry = "  select  coalesce( sum(act.rdv_hold_amount),0) from efin_rdv_hold_action act "
            + " where act.efin_rdvtxnline_id in ( select efin_rdvtxnline_id from efin_rdvtxnline where efin_rdvtxn_id in ("
            + " select efin_rdvtxn_id from efin_rdvtxn  where  efin_rdv_id= ? ) and efin_rdvtxnline.trxln_no= ? )"
            + "  and act.efin_rdv_hold_types_id= ?  ";
        if (actionId != null)
          sqlqry += " and act.efin_rdv_hold_action_id <> ? ";
        qry = OBDal.getInstance().getSession().createSQLQuery(sqlqry);
        qry.setParameter(0, line.getEfinRdvtxn().getEfinRdv().getId());
        qry.setParameter(1, line.getTrxlnNo());
        qry.setParameter(2, holdType.getId());
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

  public static String chkHoldtypeGoingdownNegativevalTxn(EfinRDVTransaction txn,
      BigDecimal currentPenaltyAmt, EfinRdvHoldTypes holdType, String actionId, String actiontype) {
    BigDecimal diffofexpentocurpen = BigDecimal.ZERO;
    BigDecimal existPenaltyAmt = BigDecimal.ZERO;
    String sqlqry = null;
    Query qry = null;
    try {
      if (txn != null) {

        sqlqry = "  select  coalesce( sum(act.rdv_hold_amount),0) from efin_rdv_hold_action act "
            + " where act.efin_rdvtxnline_id in ( select efin_rdvtxnline_id from efin_rdvtxnline where efin_rdvtxn_id in ("
            + " select efin_rdvtxn_id from efin_rdvtxn  where  efin_rdv_id= ? )  )"
            + "  and act.efin_rdv_hold_types_id= ? and act.istxn='Y' ";
        if (actionId != null)
          sqlqry += " and act.txnGroupRef <> ? ";
        qry = OBDal.getInstance().getSession().createSQLQuery(sqlqry);
        qry.setParameter(0, txn.getEfinRdv().getId());
        qry.setParameter(1, holdType.getId());
        if (actionId != null)
          qry.setParameter(2, actionId);
        log4j.debug("qry12:" + qry.toString());
        @SuppressWarnings("rawtypes")
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

  @SuppressWarnings("rawtypes")
  public static String checkHoldTypeAlreadExistsInTxn(EfinRDVTransaction txn,
      EfinRdvHoldTypes holdType, String actionId, String actiontype) {
    String sqlqry = null;
    Query qry = null;
    try {
      if (txn != null) {

        sqlqry = "  select efin_rdv_hold_action_id from efin_rdv_hold_action act "
            + " left join efin_rdvtxnline txnln on txnln.efin_rdvtxnline_id = act.efin_rdvtxnline_id "
            + " where txnln.efin_rdvtxn_id= ? "
            + "  and act.efin_rdv_hold_types_id= ? and act.istxn='Y' and act.action= ?  ";

        if (actionId != null)
          sqlqry += " and act.txnGroupRef <> ? ";
        qry = OBDal.getInstance().getSession().createSQLQuery(sqlqry);
        qry.setParameter(0, txn.getId());
        qry.setParameter(1, holdType.getId());
        qry.setParameter(2, actiontype);
        if (actionId != null)
          qry.setParameter(3, actionId);
        log4j.debug("qry12:" + qry.toString());
        List chkHoldTypeExists = qry.list();
        if (chkHoldTypeExists != null && chkHoldTypeExists.size() > 0) {
          return "Y";
        } else
          return "N";

      }

    } catch (final Exception e) {
      log4j.error("Exception in checkHoldTypeAlreadExistsInTxn", e);

    } finally {

    }
    return "N";
  }

  @SuppressWarnings("resource")
  public synchronized static JSONObject getBusinessPartnerList(String clientId, String searchTerm,
      int pagelimit, int page, String roleId, String orgId) {
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
      fromQuery.append(" from c_bpartner bp  where isvendor= 'Y' and ad_org_id in ("
          + Utility.getChildOrg(clientId, orgId)
          + ")  and ad_org_id in( select ad_org_id  from ad_role_orgaccess   where ad_role_id  = ? and ad_client_id = ? )  "
          + "                and ad_client_id=?  ");

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

    } catch (

    final Exception e) {
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
              + "  where com.ad_org_id in (" + Utility.getChildOrg(clientId, orgId)
              + ")  and com.ad_org_id in( select ad_org_id  from ad_role_orgaccess   where ad_role_id  = ? and ad_client_id = ? )  "
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

        sqlqry = "  select  coalesce( sum(act.rdv_hold_amount),0) from efin_rdv_hold_action act "
            + "  where act.efin_rdvtxnline_id = ?  ";
        if (actionId != null)
          sqlqry += " and act.efin_rdv_hold_action_id <> ? ";

        qry = OBDal.getInstance().getSession().createSQLQuery(sqlqry);
        qry.setParameter(0, lineId);
        if (actionId != null)
          qry.setParameter(1, actionId);
        log4j.debug("qry:" + qry.toString());
        @SuppressWarnings("rawtypes")
        List getPenaltytyAmt = qry.list();
        totalMatchAmt = line.getMatchAmt().subtract(line.getADVDeduct())
            .subtract(line.getPenaltyAmt());
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

  public static String getTotalMatchAmtTxn(String txnId, BigDecimal currentPenaltyAmt,
      String actionId) {
    BigDecimal totalpenaltyAmt = BigDecimal.ZERO;
    BigDecimal existPenaltyAmt = BigDecimal.ZERO;
    BigDecimal totalMatchAmt = BigDecimal.ZERO;
    BigDecimal totalLineLevelHoldAmt = BigDecimal.ZERO;
    String sqlqry = null;
    Query qry = null;
    try {
      if (txnId != null) {
        EfinRDVTransaction txn = OBDal.getInstance().get(EfinRDVTransaction.class, txnId);

        //

        sqlqry = "select coalesce(sum(rdv_hold_amount),0) as amt from efin_rdv_hold_action act "
            + "    join efin_rdvtxnline ln on ln.efin_rdvtxnline_id = act.efin_rdvtxnline_id "
            + "    where ln.efin_rdvtxn_id =? and act.istxn='N'  group by ln.efin_rdvtxn_id";
        qry = OBDal.getInstance().getSession().createSQLQuery(sqlqry);
        qry.setParameter(0, txnId);
        log4j.debug("qry:" + qry.toString());
        @SuppressWarnings("rawtypes")
        List getLineLevelHoldAmt = qry.list();
        if (getLineLevelHoldAmt != null && getLineLevelHoldAmt.size() > 0) {
          totalLineLevelHoldAmt = (BigDecimal) getLineLevelHoldAmt.get(0);
        }

        //
        sqlqry = "select coalesce(sum(rdv_hold_amount),0) as amt from efin_rdv_hold_action act "
            + "    join efin_rdvtxnline ln on ln.efin_rdvtxnline_id = act.efin_rdvtxnline_id "
            + "    where ln.efin_rdvtxn_id =?  and act.istxn='Y' ";
        if (actionId != null)
          sqlqry += " and act.txngroupref <> ? ";

        sqlqry += " group by ln.efin_rdvtxn_id ";

        qry = OBDal.getInstance().getSession().createSQLQuery(sqlqry);
        qry.setParameter(0, txnId);
        if (actionId != null)
          qry.setParameter(1, actionId);

        log4j.debug("qry:" + qry.toString());
        @SuppressWarnings("rawtypes")
        List getPenaltytyAmt = qry.list();
        totalMatchAmt = txn.getMatchAmt().subtract(txn.getADVDeduct()).subtract(txn.getPenaltyAmt())
            .subtract(totalLineLevelHoldAmt);

        if (getPenaltytyAmt != null && getPenaltytyAmt.size() > 0) {
          existPenaltyAmt = (BigDecimal) getPenaltytyAmt.get(0);
          log4j.debug("existPenaltyAmt:" + existPenaltyAmt);
          totalpenaltyAmt = existPenaltyAmt.add(currentPenaltyAmt);
          // totalpenaltyAmt = currentPenaltyAmt;

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
    }
    return "N";
  }

}