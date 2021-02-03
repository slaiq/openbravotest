package sa.elm.ob.finance.reports.MonthlyClosingReport;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.calendar.Year;

/**
 * 
 * @author gopalakrishnan on 27/12/2016
 * 
 */
public class MonthlyClosingReportDAO {
  private Connection conn = null;
  VariablesSecureApp vars = null;
  private static Logger log4j = Logger.getLogger(MonthlyClosingReportDAO.class);

  public MonthlyClosingReportDAO(Connection con) {
    this.conn = con;
  }

  /**
   * 
   * @param adOrgClient
   *          adUserClient
   * @return acctSchemaList
   */
  public List<AcctSchema> getSchema(String adOrgClient, String adUserClient) {
    OBQuery<AcctSchema> schemaList = null;
    try {
      schemaList = OBDal.getInstance().createQuery(
          AcctSchema.class,
          "as e where e.client.id in (" + adUserClient + ") and e.organization.id in ("
              + adOrgClient + ") order by e.name");
    } catch (Exception e) {
      log4j.debug("Exception getSchema :" + e);
    }
    return schemaList.list();
  }

  public List<Year> getYear(String adOrgClient, String adUserClient) {
    OBQuery<Year> yearList = null;
    try {
      yearList = OBDal.getInstance().createQuery(
          Year.class,
          "as e where e.client.id in (" + adUserClient + ") and e.organization.id in ("
              + adOrgClient + ") order by e.fiscalYear ");
    } catch (Exception e) {
      log4j.debug("Exception  getYear:" + e);
    }
    return yearList.list();
  }

  public List<Period> getPeriod(String yearId) {
    OBQuery<Period> periodList = null;
    try {
      periodList = OBDal.getInstance().createQuery(Period.class,
          "as e where e.year.id='" + yearId + "' order by creationDate");
    } catch (Exception e) {
      log4j.debug("Exception  getPeriod:" + e);
    }
    return periodList.list();
  }

  /**
   * 
   * @param startingDate
   * @param AcctSchemaId
   * @return opening Balance
   */
  public BigDecimal getopeningBalance(String OrgId, String clientId, String acctOrg,
      String acctshemaId, String yearStartDate, String periodStDate) {
    BigDecimal openingBalance = BigDecimal.ZERO;
    PreparedStatement st = null;
    ResultSet rs = null;
    try {

      /*
       * query =
       * 
       * query =
       * "select coalesce(sum((COALESCE(f.AMTACCTDR,0) - COALESCE(f.AMTACCTCR, 0))),0) AS qty from fact_acct f "
       * + " where f.c_acctschema_id ='" + AcctSchemaId + "' " + " and f.dateacct < to_date('" +
       * startingDate + "','dd-MM-yyyy') ";
       */

      st = conn
          .prepareStatement("select coalesce(sum((COALESCE(intialamtdr,0) - COALESCE(intialamtcr, 0))),0) AS qty"
              + " from ("
              + " SELECT COALESCE(AMTACCTCR,0) as intialamtcr, COALESCE(AMTACCTDR,0) as intialamtdr"
              + " FROM C_ElementValue m, Fact_Acct f "
              + " WHERE m.AD_Org_ID IN("
              + OrgId
              + ") AND m.AD_Client_ID IN("
              + clientId
              + ") AND f.C_ACCTSCHEMA_ID = ? AND f.FACTACCTTYPE <> 'R' AND f.FACTACCTTYPE <> 'C'"
              + " AND m.C_ElementValue_ID = f.Account_ID AND f.AD_ORG_ID IN ("
              + acctOrg
              + ") AND m.accounttype IN ('A','L','O')"
              + " AND f.DATEACCT < to_date('"
              + yearStartDate + "','dd-MM-yyyy')) aa");

      st.setString(1, acctshemaId);

      log4j.debug("OpeningBalanceQuery:" + st.toString());
      rs = st.executeQuery();
      if (rs.next()) {
        openingBalance = rs.getBigDecimal("qty");
      }

    } catch (final SQLException e) {
      log4j.error("", e);
      return openingBalance;
    } catch (final Exception e) {
      log4j.error("", e);
      return openingBalance;
    } finally {
      try {
        st.close();
      } catch (final SQLException e) {
        log4j.error("", e);
        return openingBalance;
      }
    }
    return openingBalance;
  }
}
