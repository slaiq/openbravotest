package sa.elm.ob.finance.ad_callouts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;

public class ServiceItemName extends SimpleCallout {

  Logger log = Logger.getLogger(ServiceItemName.class);
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpserviceitem = vars.getStringParameter("inpserviceitem");
    Connection con = OBDal.getInstance().getConnection();
    try {
      log.debug("LastChanged:" + inpserviceitem);

      String name = null;
      if (inpLastFieldChanged.equals("inpserviceitem")) {

        PreparedStatement stmt = con
            .prepareStatement("select name from efin_lookup_line where efin_lookup_line_id = ?");
        stmt.setString(1, inpserviceitem);
        log.debug("inpserviceitem:" + inpserviceitem);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
          name = rs.getString("name");
        }
        info.addResult("inpserviceitemdesc", name);
      }

    } catch (Exception e) {
      e.printStackTrace();
      log.debug("Exception in ServiceItemName Callout:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }

  }
}