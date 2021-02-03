package sa.elm.ob.scm.ad_callouts;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * Callout to update Bid dates in Bid Management
 * 
 * @author qualian
 *
 */

@SuppressWarnings("serial")
public class EscmBiddatesCallout extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    VariablesSecureApp vars = info.vars;
    String inpquesdate = vars.getStringParameter("inpquelastdate");
    String inpProposalDate = vars.getStringParameter("inpproposallastday");
    String inpOpenDate = vars.getStringParameter("inpopenenvday");
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");

    PreparedStatement st = null;
    ResultSet rs = null;
    try {

      if (inpLastFieldChanged.equals("inpquelastdate")) {
        st = OBDal.getInstance().getConnection()
            .prepareStatement("select to_char(eut_convertto_gregorian('" + inpquesdate
                + "')) as eut_convertto_gregorian ");
        rs = st.executeQuery();

        if (rs.next()) {
          info.addResult("inpcorgredate", rs.getString("eut_convertto_gregorian"));

        }
        rs.close();
        if (st != null)
          st.close();
      }
      if (inpLastFieldChanged.equals("inpproposallastday")) {
        st = OBDal.getInstance().getConnection()
            .prepareStatement("select to_char(eut_convertto_gregorian('" + inpProposalDate
                + "')) as eut_convertto_gregorian ");
        rs = st.executeQuery();

        if (rs.next()) {
          info.addResult("inpproposalgredate", rs.getString("eut_convertto_gregorian"));

        }
        rs.close();
      }
      if (inpLastFieldChanged.equals("inpopenenvday")) {
        updateEnvGregDate(inpOpenDate, info);
        // st = OBDal.getInstance().getConnection()
        // .prepareStatement("select to_char(eut_convertto_gregorian('" + inpOpenDate
        // + "')) as eut_convertto_gregorian ");
        // rs = st.executeQuery();
        //
        // if (rs.next()) {
        // info.addResult("inpenvgregdate", rs.getString("eut_convertto_gregorian"));
        //
        // }
        // rs.close();
      }
      if (inpLastFieldChanged.equals("inpproposallastday")) {
        info.addResult("inpopenenvday", inpProposalDate);
        updateEnvGregDate(inpProposalDate, info);
      }
    } catch (Exception e) {
      log4j.error("Exception in EscmBiddatesCallout  :", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      // close connection
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {
        log4j.error("Exception while closing the statement in EscmBiddatesCallout ", e);
      }
    }

  }

  private void updateEnvGregDate(String hijriDate, CalloutInfo info) throws SQLException {
    PreparedStatement st = null;
    ResultSet rs = null;
    st = OBDal.getInstance().getConnection().prepareStatement(
        "select to_char(eut_convertto_gregorian('" + hijriDate + "')) as eut_convertto_gregorian ");
    rs = st.executeQuery();

    if (rs.next()) {
      info.addResult("inpenvgregdate", rs.getString("eut_convertto_gregorian"));

    }
    rs.close();
    st.close();
  }
}
