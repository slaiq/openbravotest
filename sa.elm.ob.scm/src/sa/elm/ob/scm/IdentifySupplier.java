package sa.elm.ob.scm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;
import org.openbravo.dal.service.OBDal;

public class IdentifySupplier {
  private static final Logger log4j = Logger.getLogger(IdentifySupplier.class);

  /**
   * Identify UALQ Business Partner
   * 
   * @param clientId
   * @param bpartnerId
   * @return boolean
   */
  public static boolean identifyBPartner(String clientId, String bpartnerId) {
    PreparedStatement ps = null;
    ResultSet rs = null;
    boolean supplierId = false;
    try {
      ps = OBDal.getInstance().getConnection()
          .prepareStatement("select count(c_bpartner_id) as count from escm_deflookups_type lokup "
              + " left join escm_deflookups_typeln lokupln on lokupln.escm_deflookups_type_id=lokup.escm_deflookups_type_id "
              + " left join c_bpartner bp on bp.em_efin_documentno=lokupln.name "
              + " where reference='PP' and lokupln.value='UALQ' and lokup.ad_client_id=? "
              + " and c_bpartner_id=? ");

      ps.setString(1, clientId);
      ps.setString(2, bpartnerId);
      rs = ps.executeQuery();
      if (rs.next()) {
        if (rs.getInt("count") > 0)
          supplierId = true;
      }
    } catch (final Exception e) {
      log4j.error("Exception in identifyBPartner() : ", e);
      return supplierId;
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (ps != null) {
          ps.close();
        }
      } catch (Exception e) {

      }
    }
    return supplierId;
  }
}