package sa.elm.ob.scm.ad_actionbutton.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;

import sa.elm.ob.utility.util.Utility;

/**
 * @author Gopalakrishnan created on 22/02/2017
 */

public class ReceiptInspectionDAO {
  private Connection conn = null;

  private static Logger log4j = Logger.getLogger(ReceiptInspectionDAO.class);

  public ReceiptInspectionDAO(Connection con) {
    this.conn = con;
  }

  public JSONObject getInspectedRecord(String clientId, String inpReceiptId,
      JSONObject searchAttr) {

    JSONObject resultObject = new JSONObject(), json = null, json1 = null;
    StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(),
        fromQuery = new StringBuilder(), whereClause = new StringBuilder(),
        groupClause = new StringBuilder(), orderClause = new StringBuilder();
    JSONArray cellarray = new JSONArray();
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONArray jsonarray = new JSONArray(), jsonarray1 = new JSONArray();
    String tempProductId = "";
    DecimalFormat df = new DecimalFormat("#.00");
    try {
      OBContext.setAdminMode();
      resultObject.put("page", "0");
      resultObject.put("total", "0");
      resultObject.put("records", "0");
      resultObject.put("rows", cellarray);
      countQuery.append(
          "select totalRecord as totalRecord from (select count(distinct ins.escm_initialreceipt_id) as totalRecord ");
      selectQuery.append(
          " select line,id,id1,product,initialQty,status,qty,inspectiondate,qualitycode,inspectedby,notes,uom,m_product_id from ");
      selectQuery.append(
          "( select rec.line,rec.escm_initialreceipt_id as id1 ,ins.escm_inspection_id as id ,prd.name as product,prd.m_product_id , (rec.quantity-coalesce(rec.ir_return_qty,0)) as initialQty,case when ins.status='A' then "
              + "'Accept' else 'Reject' end as status ");
      selectQuery.append(
          " ,ins.quantity as qty,(select eut_convert_to_hijri(to_char(ins.inspectiondate,'yyyy-MM-dd'))) as inspectiondate ,ins.qualitycode,ins.inspectedby,ins.notes,uom.name as uom  ");
      fromQuery.append(
          "from escm_inspection ins left join  escm_initialreceipt  rec on rec.escm_initialreceipt_id=ins.escm_initialreceipt_id ");
      fromQuery.append(
          "left join m_product prd on prd.m_product_id =rec.m_product_id left join c_uom uom on uom.c_uom_id=rec.c_uom_id  where 1=1  and prd.em_escm_noinspection='N' and producttype='I' and isstocked='Y' ");
      if (StringUtils.length(inpReceiptId) == 32) {
        whereClause.append(" and ins.m_inout_id='" + inpReceiptId + "'");
      }
      if (searchAttr.has("search") && Boolean.valueOf(searchAttr.getString("search"))) {
        if (searchAttr.has("Item") && searchAttr.getString("Item").length() > 0)
          whereClause.append("and prd.name ilike '%").append(searchAttr.getString("Item"))
              .append("%' ");

      }
      whereClause.append(") as main");

      orderClause.append(" order by line,m_product_id,status,");
      if (searchAttr.getString("sortName").equals("Item")) {
        orderClause.append("product ").append(searchAttr.getString("sortType"));
      } else {
        orderClause.append(" product asc");
      }
      // Getting Count
      if (searchAttr.has("limit") && searchAttr.getInt("limit") == 0) {
        int offset = 0, totalPage = 0, totalRecord = 0;
        int rows = Integer.parseInt(searchAttr.getString("rows"));
        int page = Integer.parseInt(searchAttr.getString("page"));

        st = conn.prepareStatement((countQuery.append(fromQuery).append(whereClause)).toString());
        log4j.debug("get ReceiptCount" + st.toString());
        rs = st.executeQuery();
        if (rs.next()) {
          totalRecord = rs.getInt("totalRecord");
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
        }

        resultObject.put("page", page);
        resultObject.put("total", totalPage);
        resultObject.put("records", totalRecord);

        searchAttr.put("limit", rows);
        searchAttr.put("offset", offset);
      }
      if (rs != null) {
        rs.close();
      }
      if (st != null) {
        st.close();
      }
      st = conn
          .prepareStatement((selectQuery.append(fromQuery).append(whereClause).append(groupClause)
              .append(orderClause).append(" limit ").append(searchAttr.getInt("limit"))
              .append(" offset ").append(searchAttr.getInt("offset"))).toString());
      log4j.debug(" getreceipt:" + st.toString());
      rs = st.executeQuery();
      while (rs.next()) {
        log4j.debug(" m_product_id:" + rs.getString("m_product_id"));
        /*
         * if (tempProductId != null && tempProductId.equals(rs.getString("m_product_id"))) {
         * jsonarray = json.getJSONArray("attr"); json1 = new JSONObject();
         * json1.put("inspectionId", Utility.nullToEmpty(rs.getString("id"))); json1.put("status",
         * Utility.nullToEmpty(rs.getString("status"))); json1.put("qty",
         * Utility.nullToEmpty(rs.getString("qty"))); json1.put("inspectiondate",
         * Utility.nullToEmpty(rs.getString("inspectiondate"))); json1.put("qualitycode",
         * Utility.nullToEmpty(rs.getString("qualitycode"))); json1.put("inspectedby",
         * Utility.nullToEmpty(rs.getString("inspectedby"))); json1.put("notes",
         * Utility.nullToEmpty(rs.getString("notes"))); jsonarray.put(json1); } else { json = new
         * JSONObject();
         * 
         * json.put("id", rs.getString("m_product_id")); json.put("name", rs.getString("product"));
         * json.put("initialQty", rs.getString("initialQty")); json.put("uom", rs.getString("uom"));
         * json1 = new JSONObject(); json1.put("inspectionId",
         * Utility.nullToEmpty(rs.getString("id"))); json1.put("status",
         * Utility.nullToEmpty(rs.getString("status"))); json1.put("qty",
         * Utility.nullToEmpty(rs.getString("qty"))); json1.put("inspectiondate",
         * Utility.nullToEmpty(rs.getString("inspectiondate"))); json1.put("qualitycode",
         * Utility.nullToEmpty(rs.getString("qualitycode"))); json1.put("inspectedby",
         * Utility.nullToEmpty(rs.getString("inspectedby"))); json1.put("notes",
         * Utility.nullToEmpty(rs.getString("notes"))); jsonarray.put(json1); json.put("attr",
         * jsonarray); tempProductId = rs.getString("m_product_id"); cellarray.put(json); }
         */
        log4j.debug(" id:" + rs.getString("id"));
        json = new JSONObject();
        json.put("id", rs.getString("id"));
        json.put("multiselect", "1");
        json.put("initialId", rs.getString("id1"));
        json.put("line", rs.getString("line"));
        json.put("name", rs.getString("product"));
        json.put("initialid", rs.getString("id1"));
        json.put("initialQty", df.format(rs.getBigDecimal("initialQty")));
        json.put("uom", rs.getString("uom"));
        json.put("m_product_id", rs.getString("m_product_id"));
        json.put("status", rs.getString("status"));
        json.put("qty", rs.getString("qty"));
        json.put("inspectiondate", Utility.nullToEmpty(rs.getString("inspectiondate")));
        json.put("qualitycode", Utility.nullToEmpty(rs.getString("qualitycode")));
        json.put("inspectedby", Utility.nullToEmpty(rs.getString("inspectedby")));
        json.put("notes", Utility.nullToEmpty(rs.getString("notes")));
        if (StringUtils.isNotEmpty(tempProductId) && StringUtils.isNotBlank(tempProductId)
            && tempProductId.equals(rs.getString("m_product_id"))) {
          json1 = new JSONObject();
          jsonarray = new JSONArray();
          JSONObject json2 = new JSONObject();
          jsonarray1 = new JSONArray();
          json2.put("display", "none");
          jsonarray1.put(json2);
          // json1.put("id", jsonarray1);
          json1.put("multiselect", jsonarray1);
          json1.put("name", jsonarray1);
          json1.put("line", jsonarray1);
          json1.put("initialQty", jsonarray1);
          json1.put("uom", jsonarray1);
          json1.put("m_product_id", jsonarray1);
          jsonarray.put(json1);
          json.put("attr", jsonarray);

        } else {
          json1 = new JSONObject();
          JSONObject json2 = new JSONObject();
          jsonarray1 = new JSONArray();
          jsonarray = new JSONArray();
          json2.put("rowspan", "2");
          jsonarray1.put(json2);
          // json1.put("id", jsonarray1);
          json1.put("multiselect", jsonarray1);
          json1.put("name", jsonarray1);
          json1.put("line", jsonarray1);
          json1.put("initialQty", jsonarray1);
          json1.put("uom", jsonarray1);
          json1.put("m_product_id", jsonarray1);
          jsonarray.put(json1);
          json.put("attr", jsonarray);
          tempProductId = rs.getString("m_product_id");

        }
        cellarray.put(json);
        log4j.debug(" cellarray:" + cellarray);

      }
      log4j.debug(" cellarray:" + cellarray);
      resultObject.put("rows", cellarray);

    } catch (Exception e) {
      log4j.error("Error while fetching the Associated Import Cost", e);

    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (st != null) {
          st.close();
        }
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();
    }

    return resultObject;
  }
}
