package sa.elm.ob.scm.ad_callouts;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * 
 * @author qualian
 *
 */

@SuppressWarnings("serial")
public class MinMaxdatecallout extends SimpleCallout {

  @SuppressWarnings("resource")
  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = info.vars;

    String Date = "";
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String date = vars.getStringParameter(inpLastFieldChanged);
    PreparedStatement st = null;
    ResultSet rs = null;
    String strDate = "";
    try {

      st = OBDal.getInstance().getConnection().prepareStatement(
          "select to_char(eut_convertto_gregorian( ? )) as eut_convertto_gregorian ");
      st.setString(1, date);
      rs = st.executeQuery();
      if (rs.next()) {
        if (rs.getString("eut_convertto_gregorian") == null) {
          Date = date.split("-")[2];
          Integer num = Integer.parseInt(Date);
          if (num > 1500) {
            st = OBDal.getInstance().getConnection()
                .prepareStatement("select max(hijri_date) as maxdate from eut_hijri_dates");
            rs = st.executeQuery();
            if (rs.next()) {
              strDate = rs.getString("maxdate");
              st = OBDal.getInstance().getConnection().prepareStatement(
                  "select substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4) as hijiri from eut_hijri_dates "
                      + " where hijri_date = ? limit 1");
              st.setString(1, strDate);
              rs = st.executeQuery();
              if (rs.next()) {
                info.addResult(inpLastFieldChanged, rs.getString("hijiri"));
              }
            }

          }
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in MinMaxdatecallout  :", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {
        log4j.error("Exception while closing the statement in MinMaxdatecallout ", e);
      }

    }

  }
}
