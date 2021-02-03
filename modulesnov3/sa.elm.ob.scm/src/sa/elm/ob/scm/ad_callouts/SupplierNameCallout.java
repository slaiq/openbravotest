package sa.elm.ob.scm.ad_callouts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

/**
 * Callout to update supplier details in Bid Management
 * 
 * @author qualian
 *
 */

@SuppressWarnings("serial")
public class SupplierNameCallout extends SimpleCallout {
  private String bpLocationId = "";
  private String locationId = "";
  private String bpPhone = "";
  private String bpFax = "";

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = info.vars;
    Connection conn = OBDal.getInstance().getConnection();
    String inpsupplierno = vars.getStringParameter("inpsuppliernumber");
    String inpbranchname = vars.getStringParameter("inpbranchname");
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String query = "";
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {

      if (inpLastFieldChanged.equals("inpsuppliernumber")) {
        query = "select name  from c_bpartner where c_bpartner_id = ? ";
        ps = conn.prepareStatement(query);
        ps.setString(1, inpsupplierno);
        rs = ps.executeQuery();
        if (rs.next()) {
          info.addResult("inpsuppliername", rs.getString("name"));
        } else {
          info.addResult("inpsuppliername", null);
        }
        getBPLocation(inpsupplierno, null);
        info.addResult("inpbranchname", bpLocationId);
        info.addResult("inpcLocationId", locationId);
        info.addResult("inpsupplierphone", bpPhone);
        info.addResult("inpsupplierfax", bpFax);
      }
      if (inpLastFieldChanged.equals("inpbranchname")) {
        getBPLocation(null, inpbranchname);
        info.addResult("inpcLocationId", locationId);
        info.addResult("inpsupplierphone", bpPhone);
        info.addResult("inpsupplierfax", bpFax);
      }
    } catch (Exception e) {
      log4j.error("Exception in SupplierNameCallout  :", e);
      throw new OBException(e);
    } finally {
      // close connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
        log4j.error("Exception while closing the statement in SupplierNameCallout ", e);
      }

    }
  }

  public void getBPLocation(String bpartnerId, String bpartnerLocId) {
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = "";
    try {
      query = "select c_location_id, phone, fax, c_bpartner_location_id, * from c_bpartner_location";
      if (bpartnerId != null)
        query += " where c_bpartner_id = ?";
      else if (bpartnerLocId != null)
        query += " where c_bpartner_location_id = ?";
      query += " order by created limit 1";
      ps = OBDal.getInstance().getConnection().prepareStatement(query);
      if (bpartnerId != null)
        ps.setString(1, bpartnerId);
      else if (bpartnerLocId != null)
        ps.setString(1, bpartnerLocId);
      rs = ps.executeQuery();
      if (rs.next()) {
        bpLocationId = rs.getString("c_bpartner_location_id");
        locationId = rs.getString("c_location_id");
        bpPhone = rs.getString("phone");
        bpFax = rs.getString("fax");
      }
    } catch (Exception e) {
      log4j.error("Exception in SupplierNameCallout  :", e);
    } finally {
      // close connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
        log4j.error("Exception while closing the statement in SupplierNameCallout ", e);
      }

    }
  }
}
