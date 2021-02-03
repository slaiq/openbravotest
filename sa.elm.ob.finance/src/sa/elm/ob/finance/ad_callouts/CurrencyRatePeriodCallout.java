package sa.elm.ob.finance.ad_callouts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

import javax.servlet.ServletException;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.financialmgmt.calendar.Period;


@SuppressWarnings("serial")
public class CurrencyRatePeriodCallout extends SimpleCallout {

  /**
   * Callout to update the FromDate And ToDate Information in Conversation Rate Window
   */
  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    VariablesSecureApp vars = info.vars;
    String inpPeriod = vars.getStringParameter("inpemEfinPeriod");
    Period period = OBDal.getInstance().get(Period.class, inpPeriod);
    Date datein = period.getStartingDate();
    Date dateout = period.getEndingDate();
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement st = null, st1 = null;
    ResultSet rs = null, rs1 = null;

    String query = " select eut_convert_to_hijri_timestamp('" + datein + "')";
    String query1 = " select eut_convert_to_hijri_timestamp('" + dateout + "')";
    String datehijiri = "";
    String dateouthijri = "";
    try {
      st = conn.prepareStatement(query);
      rs = st.executeQuery();
      if (rs.next()) {
        datehijiri = rs.getString("eut_convert_to_hijri_timestamp");

      }
      st1 = conn.prepareStatement(query1);
      rs1 = st1.executeQuery();
      if (rs1.next()) {
        dateouthijri = rs1.getString("eut_convert_to_hijri_timestamp");
      }
    } catch (Exception e) {
      log4j.error("Exception in ConversionRatePeriodCallout :", e);
      throw new OBException(e);
    }

    info.addResult("inpvalidfrom", datehijiri);
    info.addResult("inpvalidto", dateouthijri);

  }
}
