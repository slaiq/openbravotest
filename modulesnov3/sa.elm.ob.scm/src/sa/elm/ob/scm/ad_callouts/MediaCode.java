package sa.elm.ob.scm.ad_callouts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;

@SuppressWarnings("serial")
public class MediaCode extends SimpleCallout {
  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = info.vars;
    Connection conn = OBDal.getInstance().getConnection();

    String inpLookUpLineId = vars.getStringParameter("inpmediatype");
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");

    String query = "";
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {

      if (inpLastFieldChanged.equals("inpmediatype")) {
        query = "select value from escm_deflookups_typeln where escm_deflookups_typeln_id= '"
            + inpLookUpLineId + "' ";
        ps = conn.prepareStatement(query);
        rs = ps.executeQuery();
        if (rs.next()) {
          info.addResult("inpmediacode", rs.getString("value"));
          if (rs.getString("value").equals("OLA")) {
            info.addResult("inpmedianame", null);
          } else {
            info.addResult("inponlinemedia", null);
          }
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in MediaCode Callout  :", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      // close connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
        log4j.error("Exception while closing the statement in MediaCode callout ", e);
      }

    }

  }

}
