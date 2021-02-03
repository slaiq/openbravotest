package sa.elm.ob.finance.ad_callouts.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HijiridateDAO {
  private static final Logger log = LoggerFactory.getLogger(HijiridateDAO.class);

  /**
   * 
   * @param trxdate
   * @return month
   * @throws SQLException
   */
  // To get the gregorian month in 'MMM' format based on Trx date
  public static String getGregorianPeriod(String trxdate) throws SQLException {
    String Trxdate = "";
    String month = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      OBContext.setAdminMode();
      st = OBDal.getInstance().getConnection().prepareStatement(
          "select to_char(eut_convertto_gregorian('" + trxdate + "')) as eut_convertto_gregorian ");
      rs = st.executeQuery();

      if (rs.next()) {
        Trxdate = rs.getString("eut_convertto_gregorian");
      }
      Date date1 = new SimpleDateFormat("dd-MM-yyyy").parse(Trxdate);
      Format formatter = new SimpleDateFormat("MMM-yyyy");
      month = formatter.format(date1);

    } catch (OBException e) {
      log.error("Exception while getGregorianPeriod" + e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      log.error("Exception while getGregorianPeriod" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return month;
  }

  /**
   * get year_ID for greorian date.
   * 
   * @param date
   *          -gregorian
   * @param clientId
   * @return
   */
  public static String getYearId(Date date, String clientId) {
    PreparedStatement ps = null;
    ResultSet rs = null;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String yearId = null;
    try {
      ps = OBDal.getInstance().getConnection()
          .prepareStatement("select c_year_id from c_period where to_date('"
              + dateFormat.format(date) + "','yyyy-MM-dd')"
              + " between to_date(to_char(startdate,'dd-MM-yyyy'),'dd-MM-yyyy') and to_date(to_char(enddate,'dd-MM-yyyy'),'dd-MM-yyyy') "
              + "  and ad_client_id ='" + clientId + "'");
      log.debug("ps:" + ps.toString());
      rs = ps.executeQuery();
      if (rs.next()) {
        yearId = rs.getString("c_year_id");
        log.debug("yearId:" + yearId);
      }
    } catch (Exception e) {
      log.error("Exception in getYearId " + e.getMessage());
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
        // TODO: handle exception
      }
    }
    return yearId;
  }

  /**
   * 
   * @param date
   * @param clientId
   * @return startingDate
   */
  public static String getPeriodStartDate(Date date, String clientId) {
    PreparedStatement ps = null;
    ResultSet rs = null;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String startingDate = null;
    try {
      ps = OBDal.getInstance().getConnection().prepareStatement(
          "select to_char(startdate,'dd-MM-yyyy') as startingDate from c_period where to_date('"
              + dateFormat.format(date) + "','yyyy-MM-dd')"
              + " between to_date(to_char(startdate,'dd-MM-yyyy'),'dd-MM-yyyy') and to_date(to_char(enddate,'dd-MM-yyyy'),'dd-MM-yyyy') "
              + "  and ad_client_id ='" + clientId + "'");
      log.debug("ps:" + ps.toString());
      rs = ps.executeQuery();
      if (rs.next()) {
        startingDate = rs.getString("startingDate");
        log.debug("startingDate:" + startingDate);
      }
    } catch (Exception e) {
      log.error("Exception in getPeriodStartDate " + e.getMessage());
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
        log.error("Exception in closing connection" + e);
      }
    }
    return startingDate;
  }
}