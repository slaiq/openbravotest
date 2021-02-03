package sa.elm.ob.utility.ad_process.ForwardRmiRoleAccess;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForwardRoleAccessDAOImpl implements ForwardRoleAccessDAO {

  private Connection conn = null;

  private static final Logger LOG = LoggerFactory.getLogger(ForwardRoleAccessDAOImpl.class);

  public ForwardRoleAccessDAOImpl(Connection con) {
    this.conn = con;
  }

  @Override
  public int forwardRoleAccessRemove(String clientId, String forwardrmiId) throws Exception {
    PreparedStatement st = null;
    ResultSet rs = null;
    int count = 0;
    try {
      OBContext.setAdminMode();
      st = conn.prepareStatement(
          "select eut_forwardaccessrole_del('" + clientId + "','" + forwardrmiId + "') ");
      rs = st.executeQuery();
      if (rs.next()) {
        count = 1;
      }

    } catch (Exception e) {
      count = 2;
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in forwardRoleAccessRemove ", e.getMessage());

    }
    return count;
  }

  @Override
  public int forwardAccessPreference(String clientId, String forwardrmiId, String windowId)
      throws Exception {
    PreparedStatement st = null;
    ResultSet rs = null;
    int count = 0;
    try {
      OBContext.setAdminMode();
      st = conn.prepareStatement("select eut_frowardprocess_preference(?,?,?) ");
      st.setString(1, clientId);
      st.setString(2, forwardrmiId);
      st.setString(3, windowId);
      rs = st.executeQuery();
      LOG.debug("query :" + st.toString());
      if (rs.next()) {
        count = 1;
      }

    } catch (Exception e) {
      count = 2;
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in forwardAccessPreference ", e.getMessage());

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

  @Override
  public int forwardAccessWindow(String clientId, String userId, String forwardrmiId,
      String doctype) throws Exception {
    PreparedStatement st = null;
    ResultSet rs = null;
    int count = 0;
    try {
      OBContext.setAdminMode();
      st = conn.prepareStatement("select eut_forward_access('" + clientId + "','" + userId + "','"
          + doctype + "','" + forwardrmiId + "') ");
      rs = st.executeQuery();
      LOG.debug("window :" + st.toString());
      if (rs.next()) {
        count = 1;
      }

    } catch (Exception e) {
      count = 2;
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in forwardAccessWindow ", e.getMessage());

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

  @Override
  public int forwardCheckBoxAccess(String clientId, String forwardrmiId) throws Exception {
    PreparedStatement st = null;
    ResultSet rs = null;
    int count = 0;
    try {
      OBContext.setAdminMode();
      st = conn.prepareStatement(
          "select eut_insert_forrolechk_access('" + clientId + "','" + forwardrmiId + "') ");
      rs = st.executeQuery();
      if (rs.next()) {
        count = 1;
      }

    } catch (Exception e) {
      count = 2;
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in forwardCheckBoxAccess ", e.getMessage());

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

  @Override
  public int requestMoreInfoAccessWindow(String clientId, String userId, String forwardrmiId,
      String doctype) throws Exception {
    PreparedStatement st = null;
    ResultSet rs = null;
    int count = 0;
    try {
      OBContext.setAdminMode();
      st = conn.prepareStatement("select eut_reqmoreinfo_access('" + clientId + "','" + userId
          + "','" + doctype + "','" + forwardrmiId + "') ");
      rs = st.executeQuery();
      LOG.debug("window :" + st.toString());
      if (rs.next()) {
        count = 1;
      }

    } catch (Exception e) {
      count = 2;
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in requestMoreInfoAccessWindow ", e.getMessage());

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

  @Override
  public int requestMoreInforRoleAccessRemove(String clientId, String forwardrmiId)
      throws Exception {
    PreparedStatement st = null;
    ResultSet rs = null;
    int count = 0;
    try {
      OBContext.setAdminMode();
      st = conn.prepareStatement(
          "select eut_rmiaccessrole_del('" + clientId + "','" + forwardrmiId + "') ");
      rs = st.executeQuery();
      if (rs.next()) {
        count = 1;
      }

    } catch (Exception e) {
      count = 2;
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in requestMoreInforRoleAccessRemove ", e.getMessage());

    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }
}
