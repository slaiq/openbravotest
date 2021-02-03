package sa.elm.ob.utility.ad_process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveAccessProcessDAO {
  private Connection conn = null;

  private static final Logger LOG = LoggerFactory.getLogger(RemoveAccessProcessDAO.class);

  public RemoveAccessProcessDAO(Connection con) {
    this.conn = con;
  }

  public int getProcessedRecord(String clientId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    int count = 0;
    try {
      OBContext.setAdminMode();
      st = conn.prepareStatement("select eut_removeaccess_process('" + clientId + "')");
      rs = st.executeQuery();
      if (rs.next()) {
        count = 1;
      }

    } catch (Exception e) {
      count = 2;
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in getProcessedRecord ", e.getMessage());

    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
        }
      }
      OBContext.restorePreviousMode();
    }
    return count;
  }
}