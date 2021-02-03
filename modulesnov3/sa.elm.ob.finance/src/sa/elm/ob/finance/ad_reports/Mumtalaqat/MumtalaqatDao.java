package sa.elm.ob.finance.ad_reports.Mumtalaqat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import sa.elm.ob.scm.ad_process.printreport.PrintReportVO;
import sa.elm.ob.scm.ad_reports.CustodyCardReport.CustodyCardReportDAO;

public class MumtalaqatDao {

  private Connection conn = null;
  private static Logger log4j = Logger.getLogger(CustodyCardReportDAO.class);

  public MumtalaqatDao(Connection con) {
    this.conn = con;
  }

  /**
   * 
   * @param clientId
   * @param sortColName
   * @param sortType
   * @param rows
   * @param page
   * @param searchFlag
   * @param type
   * @return lkUpLs
   */
  @SuppressWarnings("rawtypes")
  public List<PrintReportVO> getLookups(String clientId, String sortColName, String sortType,
      int rows, int pageNo, String searchFlag) {
    List<PrintReportVO> lkUpLs = null;
    PrintReportVO rtnVO = null;
    SQLQuery lkupQuery = null;
    StringBuffer query = null;
    int count = 0, totalPages = 0, start = 0;
    int page = pageNo;
    try {
      OBContext.setAdminMode(true);
      query = new StringBuffer();

      query.append(
          "select lkupln.value, lkupln.line as seqno, lkupln.description, lkupln.name, lkupln.efin_lookup_line_id as lookuplnid "
              + " from efin_lookup_type lkup left join efin_lookup_line lkupln on lkup.efin_lookup_type_ID=lkupln.efin_lookup_type_id "
              + " where lkup.reference=? and lkup.ad_client_id='" + clientId + "' ");

      if (sortColName != null)
        query.append(" order by " + sortColName + " " + sortType);

      lkupQuery = OBDal.getInstance().getSession().createSQLQuery(query.toString());
      lkupQuery.setParameter(0, "REPORTLKUP");
      log4j.info("getlookups->" + query.toString());
      if (lkupQuery != null) {
        count = lkupQuery.list().size();
        if (count > 0) {
          totalPages = (int) (count) / rows;

          if ((int) (count) % rows > 0)
            totalPages = totalPages + 1;
          start = (page - 1) * rows;

          if (page > totalPages) {
            page = totalPages;
            start = (page - 1) * rows;
          }
        } else {
          totalPages = 0;
          page = 0;
        }
        lkupQuery.setFirstResult(start);
        lkupQuery.setMaxResults(rows);
        lkUpLs = new ArrayList<PrintReportVO>();
        if (lkupQuery.list().size() > 0) {
          for (Iterator iterator = lkupQuery.list().listIterator(); iterator.hasNext();) {
            Object[] objects = (Object[]) iterator.next();
            rtnVO = new PrintReportVO();
            rtnVO.setPage(page);
            rtnVO.setTotalPages(totalPages);
            rtnVO.setCount(count);

            // rtnVO.setSeqNo(objects[1] == null ? "" : objects[1].toString());
            rtnVO.setSeqNo(objects[0] == null ? "" : objects[0].toString());
            rtnVO.setAwardLookUp(objects[3] == null ? "" : objects[3].toString());
            rtnVO.setAwardLookUpLnId(objects[4] == null ? "" : objects[4].toString());
            rtnVO.setValue(objects[0] == null ? "" : objects[0].toString());
            log4j.info("id>" + rtnVO.getAwardLookUpLnId() + "," + rtnVO.getSeqNo());
            lkUpLs.add(rtnVO);
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in get lookup ", e);
    } finally {
      OBContext.restorePreviousMode();
      OBDal.getInstance().getSession().clear();
    }
    return lkUpLs;
  }

  public synchronized JSONObject getFinancialYear(String clientId, String searchTerm, int pagelimit,
      int page) {
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    int totalRecords = 0;
    try {
      jsob = new JSONObject();
      StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(),
          fromQuery = new StringBuilder();

      countQuery.append(" select count(c_year_id) as count ");
      selectQuery.append(" select c_year_id as yearId, description as value ");
      fromQuery.append(" from c_year where ad_client_id = ?");

      if (searchTerm != null && !searchTerm.equals(""))
        fromQuery.append(" and description ilike '%" + searchTerm.toLowerCase() + "%'");

      st = conn.prepareStatement(countQuery.append(fromQuery).toString());
      st.setString(1, clientId);

      log4j.debug("benfilist:" + st.toString());
      log4j.debug("qry>>" + st.toString());
      rs = st.executeQuery();
      if (rs.next())
        totalRecords = rs.getInt("count");
      jsob.put("totalRecords", totalRecords);
      if (rs != null)
        rs.close();
      if (st != null)
        st.close();

      if (totalRecords > 0) {
        st = conn.prepareStatement(
            (selectQuery.append(fromQuery)).toString() + " order by value limit ? offset ? ");
        st.setString(1, clientId);
        st.setInt(2, pagelimit);
        st.setInt(3, (page - 1) * pagelimit);

        log4j.debug("year list:" + st.toString());
        rs = st.executeQuery();

        while (rs.next()) {
          JSONObject jsonData = new JSONObject();
          jsonData.put("id", rs.getString("yearId"));
          jsonData.put("recordIdentifier", rs.getString("value"));
          jsonArray.put(jsonData);
        }
      }
      if (jsonArray.length() > 0)
        jsob.put("data", jsonArray);
      else
        jsob.put("data", "");

    } catch (final Exception e) {
      log4j.error("Exception in financial year :", e);
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

  public synchronized JSONObject getRegion(String clientId, String searchTerm, int pagelimit,
      int page) {
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    int totalRecords = 0;
    try {
      jsob = new JSONObject();
      StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(),
          fromQuery = new StringBuilder();

      countQuery.append(" select count(lkupln.efin_lookup_line_id) as count ");
      selectQuery
          .append(" select lkupln.name as value, lkupln.efin_lookup_line_id as lookuplnid  ");
      fromQuery.append(
          " from efin_lookup_type lkup left join efin_lookup_line lkupln on lkup.efin_lookup_type_ID=lkupln.efin_lookup_type_id  where lkup.reference=? and lkup.ad_client_id=?");

      if (searchTerm != null && !searchTerm.equals(""))
        fromQuery.append(" and description ilike '%" + searchTerm.toLowerCase() + "%'");

      st = conn.prepareStatement(countQuery.append(fromQuery).toString());
      st.setString(1, "MREGION");
      st.setString(2, clientId);

      log4j.debug("benfilist:" + st.toString());
      log4j.debug("qry>>" + st.toString());
      rs = st.executeQuery();
      if (rs.next())
        totalRecords = rs.getInt("count");
      jsob.put("totalRecords", totalRecords);
      if (rs != null)
        rs.close();
      if (st != null)
        st.close();

      if (totalRecords > 0) {
        st = conn.prepareStatement(
            (selectQuery.append(fromQuery)).toString() + " order by value limit ? offset ? ");
        st.setString(1, "MREGION");
        st.setString(2, clientId);
        st.setInt(3, pagelimit);
        st.setInt(4, (page - 1) * pagelimit);

        log4j.debug("year list:" + st.toString());
        rs = st.executeQuery();

        while (rs.next()) {
          JSONObject jsonData = new JSONObject();
          jsonData.put("id", rs.getString("lookuplnid"));
          jsonData.put("recordIdentifier", rs.getString("value"));
          jsonArray.put(jsonData);
        }
      }
      if (jsonArray.length() > 0)
        jsob.put("data", jsonArray);
      else
        jsob.put("data", "");

    } catch (final Exception e) {
      log4j.error("Exception in region year :", e);
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

  public synchronized JSONObject getPaymentBeneficiaryList(String clientId, String searchTerm,
      int pagelimit, int page) {
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    int totalRecords = 0;
    try {
      jsob = new JSONObject();
      StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(),
          fromQuery = new StringBuilder();

      countQuery.append(" select count(lines.c_bpartner_id ) as count ");
      selectQuery.append(" select distinct lines.c_bpartner_id as bpid , name as value");
      fromQuery.append(" from c_invoiceline lines "
          + "join c_invoice invoice on lines.c_invoice_id = invoice.c_invoice_id "
          + "join c_bpartner bp on bp.c_bpartner_id = lines.c_bpartner_id "
          + " where invoice.em_efin_c_salesregion_id='1B477717BFE044E48C240D705A97AFE4' and  invoice.ad_client_id = ? ");

      if (searchTerm != null && !searchTerm.equals(""))
        fromQuery.append(" and name ilike '%" + searchTerm.toLowerCase() + "%'");

      st = conn.prepareStatement(countQuery.append(fromQuery).toString());
      st.setString(1, clientId);

      log4j.debug("benfilist:" + st.toString());
      log4j.debug("qry>>" + st.toString());
      rs = st.executeQuery();
      if (rs.next())
        totalRecords = rs.getInt("count");
      jsob.put("totalRecords", totalRecords);
      if (rs != null)
        rs.close();
      if (st != null)
        st.close();

      if (totalRecords > 0) {
        st = conn.prepareStatement(
            (selectQuery.append(fromQuery)).toString() + " order by value limit ? offset ? ");
        st.setString(1, clientId);
        st.setInt(2, pagelimit);
        st.setInt(3, (page - 1) * pagelimit);

        log4j.debug("bp list:" + st.toString());
        rs = st.executeQuery();

        while (rs.next()) {
          JSONObject jsonData = new JSONObject();
          jsonData.put("id", rs.getString("bpid"));
          jsonData.put("recordIdentifier", rs.getString("value"));
          jsonArray.put(jsonData);
        }
      }
      if (jsonArray.length() > 0)
        jsob.put("data", jsonArray);
      else
        jsob.put("data", "");

    } catch (final Exception e) {
      log4j.error("Exception in payment beneficairy year :", e);
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

  public synchronized JSONObject getInvoiceList(String clientId, String searchTerm, int pagelimit,
      int page, String bpId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    int totalRecords = 0;
    try {
      jsob = new JSONObject();
      StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(),
          fromQuery = new StringBuilder();

      countQuery.append(" select count(invoice.c_invoice_id) as count ");
      selectQuery.append(" select distinct invoice.c_invoice_id as invId , documentno as value");
      fromQuery.append(" from c_invoice invoice  "
          + " join c_invoiceline lines on lines.c_invoice_id = invoice.c_invoice_id "
          + " join c_bpartner bp on bp.c_bpartner_id = lines.c_bpartner_id "
          + " where invoice.em_efin_c_salesregion_id='1B477717BFE044E48C240D705A97AFE4' and  invoice.ad_client_id = ?  and lines.c_bpartner_id = ?");

      if (searchTerm != null && !searchTerm.equals(""))
        fromQuery.append(" and documentno ilike '%" + searchTerm.toLowerCase() + "%'");

      st = conn.prepareStatement(countQuery.append(fromQuery).toString());
      st.setString(1, clientId);
      st.setString(2, bpId);

      log4j.debug("getInvoiceList:" + st.toString());
      log4j.debug("qry>>" + st.toString());
      rs = st.executeQuery();
      if (rs.next())
        totalRecords = rs.getInt("count");
      jsob.put("totalRecords", totalRecords);
      if (rs != null)
        rs.close();
      if (st != null)
        st.close();

      if (totalRecords > 0) {
        st = conn.prepareStatement(
            (selectQuery.append(fromQuery)).toString() + " order by value limit ? offset ? ");
        st.setString(1, clientId);
        st.setString(2, bpId);
        st.setInt(3, pagelimit);
        st.setInt(4, (page - 1) * pagelimit);

        log4j.debug("invoice list:" + st.toString());
        rs = st.executeQuery();

        while (rs.next()) {
          JSONObject jsonData = new JSONObject();
          jsonData.put("id", rs.getString("invId"));
          jsonData.put("recordIdentifier", rs.getString("value"));
          jsonArray.put(jsonData);
        }
      }
      if (jsonArray.length() > 0)
        jsob.put("data", jsonArray);
      else
        jsob.put("data", "");

    } catch (final Exception e) {
      log4j.error("Exception in getInvoiceList :", e);
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

}