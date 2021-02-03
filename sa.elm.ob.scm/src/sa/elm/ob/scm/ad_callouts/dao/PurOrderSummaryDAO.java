package sa.elm.ob.scm.ad_callouts.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.utility.util.UtilityDAO;

public class PurOrderSummaryDAO {
  private static final Logger log = LoggerFactory.getLogger(PurOrderSummaryDAO.class);

  /**
   * Method to get supplier Date for corresponding proposal
   * 
   * @param strBidID
   * @return SupplierDate
   * @throws SQLException
   */
  public static String getSupplierDate(String ProposalId) throws SQLException {
    String ProposalDate = "";
    String SupplierDate = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      st = OBDal.getInstance().getConnection().prepareStatement(
          "select Supplier_Proposal_Date from escm_proposalmgmt where escm_proposalmgmt_id = ?");
      st.setString(1, ProposalId);
      rs = st.executeQuery();

      if (rs.next()) {
        ProposalDate = rs.getString("Supplier_Proposal_Date");

      }
      rs.close();
      st.close();
      st = OBDal.getInstance().getConnection()
          .prepareStatement("select to_char(eut_convert_to_hijri( ? )) as eut_convert_to_hijri ");
      st.setString(1, ProposalDate);

      rs = st.executeQuery();

      if (rs.next()) {
        SupplierDate = rs.getString("eut_convert_to_hijri");

      }
      rs.close();

    } catch (OBException e) {
      log.error("Exception while getSupplierDate" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      try {
        // close connection
        if (rs != null) {
          rs.close();
        }
        if (st != null) {
          st.close();
        }
      } catch (Exception e) {

      }
    }
    return SupplierDate;
  }

  /**
   * Method To get the bid name for corresponding bid number
   * 
   * @param bidno
   * @return
   * @throws SQLException
   */
  public static String getBidname(String bidno) throws SQLException {
    String Bidno = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      st = OBDal.getInstance().getConnection()
          .prepareStatement("select bidname from escm_bidmgmt where escm_bidmgmt_id = ?");
      st.setString(1, bidno);

      rs = st.executeQuery();
      if (rs.next()) {
        Bidno = rs.getString("bidname");
      }

    } catch (OBException e) {
      log.error("Exception while getBidname" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      // close connection
      try {
        if (st != null) {
          st.close();
        }
        if (rs != null) {
          rs.close();
        }
      } catch (Exception e) {
        log.error("Exception while closing the statement in getBidname() ", e);
      }
    }
    return Bidno;
  }

  /**
   * Method To get the Region based on city selection
   * 
   * @param city
   * @return
   * @throws SQLException
   */
  public static String getRegion(String city) throws SQLException {
    String regionId = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      st = OBDal.getInstance().getConnection()
          .prepareStatement("select c_region_id from c_city where c_city_id = ? ");
      st.setString(1, city);
      rs = st.executeQuery();
      if (rs.next()) {
        regionId = rs.getString("c_region_id");
      }

    } catch (OBException e) {
      log.error("Exception while getRegion" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      try {
        if (st != null)
          st.close();
        if (rs != null)
          rs.close();

      } catch (Exception e) {
        log.error("Exception while closing the statement in getRegion() ", e);
      }
    }
    return regionId;
  }

  /**
   * if the period type as month then calculate the contract end date
   * 
   * @param contractduration
   * @param periodtype
   * @param inpstartdate
   * @param inpenddate
   * @param inpClient
   * @return
   */
  public static String getContractDurationMonth(String contractduration, String periodtype,
      String inpstartdate, String inpenddate, String inpClient) {
    String enddate = "", startyear = "", startmonth = "", endMonth = "", endYear = "";
    int endyear = 0, endmonth = 0;

    try {
      if (periodtype.equals("MT")) {

        // startdate = inpstartdate.split("-")[0];
        startmonth = inpstartdate.split("-")[1];
        int years = Integer.valueOf(contractduration) / 12;
        int months = Integer.valueOf(contractduration) % 12;

        if (years > 0) {
          startyear = inpstartdate.split("-")[2];

          endyear = Integer.valueOf(startyear) + years;
          endYear = String.valueOf(endyear);

        } else if (years == 0) {
          endYear = inpstartdate.split("-")[2];

        }
        if (months > 0) {
          startmonth = inpstartdate.split("-")[1];

          int month = 12 - Integer.valueOf(startmonth);

          if (months > month) {
            endmonth = months - month;
            endyear = Integer.valueOf(endYear) + 1;
            endYear = String.valueOf(endyear);

          } else {
            endmonth = Integer.valueOf(startmonth) + months;
          }
          if (endmonth < 10) {
            endMonth = "0" + endmonth;
          } else
            endMonth = String.valueOf(endmonth);

        } else if (months == 0) {
          endMonth = inpstartdate.split("-")[1];

        }
        enddate = endYear + endMonth + inpstartdate.split("-")[0];
        enddate = getOneDayMinusHijiriDate(enddate, inpClient);
        // inpenddate = enddate;

      }
    } catch (OBException e) {
      log.error("Exception while getContractDuration" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
    return enddate;
  }

  /**
   * if the period type as day then calculate the contract end date
   * 
   * @param contractDuration
   * @param periodtype
   * @param inpstartdate
   * @param inpenddate
   * @param inpClient
   * @return
   * @throws SQLException
   */
  public static String getContractDurationday(String contractDuration, String periodtype,
      String inpstartdate, String inpenddate, String inpClient) throws SQLException {
    String startdate = "", enddate = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    Connection conn = OBDal.getInstance().getConnection();
    String contractduration = contractDuration;
    try {
      if (periodtype.equals("DT")) {
        contractduration = contractduration.equals("") ? "0" : contractduration;
        startdate = inpstartdate.split("-")[2] + inpstartdate.split("-")[1]
            + inpstartdate.split("-")[0];
        st = conn.prepareStatement(
            "select hijri_date from (select max(hijri_date)  as hijri_date from eut_hijri_dates where   hijri_date >= ? group by   hijri_date "
                + "    order by hijri_date asc   limit ? ) dual order by hijri_date desc limit 1   ");
        st.setString(1, startdate);
        // st.setString(2, inpClient);
        st.setInt(2, Integer.valueOf(contractduration));

        rs = st.executeQuery();
        if (rs.next()) {

          enddate = rs.getString("hijri_date").substring(6, 8) + "-"
              + rs.getString("hijri_date").substring(4, 6) + "-"
              + rs.getString("hijri_date").substring(0, 4);
          // inpenddate = enddate;

        }

      }

    } catch (OBException e) {
      log.error("Exception while getContractDurationday" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
    return enddate;
  }

  // To get the contractduration for corresponding contract enddate
  public static JSONObject getContractDurationdate(String contractduration, String periodtype,
      String inpstartdate, String inpenddate, String inpClient) throws SQLException {
    int years;
    int months = 0;
    int days = 0;
    String strNowmonth = "";
    String startdate = inpstartdate;
    JSONObject result = new JSONObject();
    try {
      String enddate = inpenddate.split("-")[2] + inpenddate.split("-")[1]
          + inpenddate.split("-")[0];
      enddate = getOneDayAddHijiriDate(enddate, inpClient);
      int startyear = Integer.parseInt(startdate.split("-")[2]);
      int startmonth = Integer.parseInt(startdate.split("-")[1]);
      int startday = Integer.parseInt(startdate.split("-")[0]);
      int endyear = Integer.parseInt(enddate.split("-")[2]);
      int endmonth = Integer.parseInt(enddate.split("-")[1]);
      int endday = Integer.parseInt(enddate.split("-")[0]);

      years = endyear - startyear;
      months = endmonth - startmonth;
      if (months < 0) {
        years--;
        months = 12 - startmonth + endmonth;
        if (endday < startday) {
          months--;
        }
      } else if (months == 0 && endday < startday) {
        years--;
        years = 11;

      }

      if (endday > startday) {
        days = endday - startday;

      } else if (endday < startday) {
        int today = endday;
        endmonth = endmonth - 1;
        if (endmonth < 10) {
          strNowmonth = "0" + String.valueOf(endmonth);
        } else {
          strNowmonth = String.valueOf(endmonth);
        }
        int maxCurrentDate = UtilityDAO.getDays(inpClient, endyear + strNowmonth);
        days = maxCurrentDate - startday + today;
      } else {
        days = 0;
        if (months == 12) {
          years++;
          months = 0;
        }
      }
      result.put("years", years);
      result.put("months", months);
      result.put("days", days);
    } catch (OBException e) {
      log.error("Exception while getContractDurationdate" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      log.error("Exception while getContractDurationdate" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
    return result;

  }

  /**
   * 
   * @param gregoriandate
   * @param clientId
   * @return
   */
  private static String getOneDayMinusHijiriDate(String gregoriandate, String clientId) {
    Query query = null;
    String strQuery = "", startdate = "";
    try {
      strQuery = " select  hijri_date from eut_hijri_dates  where hijri_date "
          + "<:gregDate order by hijri_date desc limit 1 ";
      query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      query.setParameter("gregDate", gregoriandate);
      if (query != null && query.list().size() > 0) {

        Object row = query.list().get(0);
        startdate = (String) row;

        startdate = startdate.substring(6, 8) + "-" + startdate.substring(4, 6) + "-"
            + startdate.substring(0, 4);

      }
    } catch (Exception e) {
      log.error("Exception in getOneDayMinusHijiriDate", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
    return startdate;
  }

  /**
   * 
   * @param gregoriandate
   * @param clientId
   * @return
   */
  private static String getOneDayAddHijiriDate(String gregoriandate, String clientId) {
    Query query = null;
    String strQuery = "", startdate = "";
    try {
      strQuery = " select  hijri_date from eut_hijri_dates where hijri_date >:gregDate "
          + " order by hijri_date asc limit 1 ";
      query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      query.setParameter("gregDate", gregoriandate);

      if (query != null && query.list().size() > 0) {

        Object row = query.list().get(0);
        startdate = (String) row;

        startdate = startdate.substring(6, 8) + "-" + startdate.substring(4, 6) + "-"
            + startdate.substring(0, 4);
      }
    } catch (Exception e) {
      log.error("Exception in getOneDayAddHijiriDate", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
    return startdate;
  }

  /**
   * Method to get gregorian date
   * 
   * @param HijriDate
   * @return corresponding gregorianDate
   */
  public static String getGregorianDate(String hijriDate) {
    String gregorianDate = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      st = OBDal.getInstance().getConnection().prepareStatement(
          "select to_char(eut_convertto_gregorian( ? )) as eut_convertto_gregorian ");
      st.setString(1, hijriDate);
      rs = st.executeQuery();

      if (rs.next()) {
        gregorianDate = rs.getString("eut_convertto_gregorian");

      }
      rs.close();

    } catch (OBException e) {
      log.error("Exception while getGregorianDate" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      log.error("Exception while getGregorianDate" + e);
    }
    return gregorianDate;
  }
}
