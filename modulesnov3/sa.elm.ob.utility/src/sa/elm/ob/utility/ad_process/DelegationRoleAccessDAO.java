package sa.elm.ob.utility.ad_process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.FormAccess;
import org.openbravo.model.ad.access.ProcessAccess;
import org.openbravo.model.ad.access.WindowAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.utility.EUT_ListAccess;
import sa.elm.ob.utility.EutDocappDelegate;
import sa.elm.ob.utility.EutDocappDelegateln;

public class DelegationRoleAccessDAO {
  private Connection conn = null;

  private static final Logger LOG = LoggerFactory.getLogger(DelegationRoleAccessDAO.class);

  public DelegationRoleAccessDAO(Connection con) {
    this.conn = con;
  }

  /**
   * Used to delete the previous access in less than today date
   * 
   * @param ClientId
   * @return
   */
  public int delegationRoleDelete(String clientId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    int count = 0;
    try {
      OBContext.setAdminMode();
      st = conn.prepareStatement("select eut_delRole_DeleteProcess('" + clientId + "') ");
      rs = st.executeQuery();
      if (rs.next()) {
        count = 1;
      }

    } catch (Exception e) {
      count = 2;
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in delegationRoleDelete ", e.getMessage());

    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  /**
   * It will give create preference to delegated role with respect to the delegated from role
   * 
   * @param recalc
   * 
   * @param ClientId
   * @return
   */

  public int delegationAccessPreference(String clientId, String recalc) {
    PreparedStatement st = null;
    ResultSet rs = null;
    int count = 0;
    try {
      OBContext.setAdminMode();
      st = conn.prepareStatement(
          "select eut_delprocess_preference('" + clientId + "','" + recalc + "') ");
      rs = st.executeQuery();
      if (rs.next()) {
        count = 1;
      }

    } catch (Exception e) {
      count = 2;
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in DelegationRoleProcess ", e.getMessage());

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

  /**
   * It will give access to all forms, process,window,list to the delegated user
   * 
   * @param recalc
   * 
   * @param ClientId
   * @return 1 --success
   */
  public int delegationAccessWindow(String clientId, String userId, String recalc) {
    PreparedStatement st = null;
    ResultSet rs = null;
    int count = 0;
    try {
      OBContext.setAdminMode();
      st = conn.prepareStatement(
          "select eut_delegation_access('" + clientId + "','" + userId + "','" + recalc + "') ");
      rs = st.executeQuery();
      if (rs.next()) {
        count = 1;
      }

    } catch (Exception e) {
      count = 2;
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in DelegationAccessWindow ", e.getMessage());

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

  /**
   * It will give access to all check boxes to the delegated role
   * 
   * @param clientId
   * @return 1 --success
   */
  public int delegationCheckBoxAccess(String clientId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    int count = 0;
    try {
      OBContext.setAdminMode();
      st = conn.prepareStatement("select eut_insert_rolecheck_access('" + clientId + "') ");
      rs = st.executeQuery();
      if (rs.next()) {
        count = 1;
      }

    } catch (Exception e) {
      count = 2;
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in delegationCheckBoxAccess ", e.getMessage());

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

  /**
   * It will update the process flag to 'Y'
   * 
   * @param clientId
   * @return 1 --success
   */
  public int delegationUpdateProcessFlag(String clientId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    int count = 0;
    try {
      OBContext.setAdminMode();
      st = conn.prepareStatement("select eut_delAccess_UpdateProflag('" + clientId + "') ");
      rs = st.executeQuery();
      if (rs.next()) {
        count = 1;
      }

    } catch (Exception e) {
      count = 2;
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in delegationUpdateProcessFlag ", e.getMessage());

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

  /**
   * It will check whether recalculate is 'Y'
   * 
   * @param clientId
   * @return true --success
   */
  public Boolean delegationRecalculate(String clientId) {
    List<EutDocappDelegate> delegationList = null;
    Boolean recal = false;
    try {
      OBContext.setAdminMode();

      OBQuery<EutDocappDelegate> delegation = OBDal.getInstance().createQuery(
          EutDocappDelegate.class,
          "as del where to_date(to_char(del.date,'yyyy-MM-dd'),'yyyy-MM-dd') <"
              + "  to_date(to_char(current_date(),'yyyy-MM-dd'),'yyyy-MM-dd') and del.client.id =:client");
      delegation.setNamedParameter("client", clientId);
      delegationList = delegation.list();
      if (delegationList.size() > 0) {
        for (EutDocappDelegate delegateObj : delegationList) {
          for (EutDocappDelegateln delegateLnObj : delegateObj.getEutDocappDelegatelnList()) {
            OBQuery<WindowAccess> windowaccess = OBDal.getInstance().createQuery(WindowAccess.class,
                "as e where e.client.id =:client and e.eutDocappDelegateln.id=:delId and e.eutRecalculate='Y'");
            windowaccess.setNamedParameter("delId", delegateLnObj.getId());
            windowaccess.setNamedParameter("client", clientId);

            if (windowaccess.list().size() > 0) {
              recal = true;
            } else {

              OBQuery<ProcessAccess> obprocessaccess = OBDal.getInstance().createQuery(
                  ProcessAccess.class,
                  "as e where e.client.id =:client and e.eUTDelegationLine.id=:delId and e.eutRecalculate='Y'");
              obprocessaccess.setNamedParameter("delId", delegateLnObj.getId());
              obprocessaccess.setNamedParameter("client", clientId);

              if (obprocessaccess.list().size() > 0) {
                recal = true;
              } else {

                OBQuery<EUT_ListAccess> listaccess = OBDal.getInstance().createQuery(
                    EUT_ListAccess.class,
                    "as e where e.client.id =:client and e.docappDelegateln.id=:delId and e.recalculate='Y'");
                listaccess.setNamedParameter("client", clientId);
                listaccess.setNamedParameter("delId", delegateLnObj.getId());

                if (listaccess.list().size() > 0) {
                  recal = true;
                } else {

                  OBQuery<FormAccess> formaccess = OBDal.getInstance().createQuery(FormAccess.class,
                      "as e where e.client.id =:client and e.eUTDelegationLine.id=:delId and e.eutRecalculate='Y'");
                  formaccess.setNamedParameter("client", clientId);
                  formaccess.setNamedParameter("delId", delegateLnObj.getId());

                  if (formaccess.list().size() > 0) {
                    recal = true;
                  } else {

                    OBQuery<org.openbravo.model.ad.access.ProcessAccess> processaccess = OBDal
                        .getInstance()
                        .createQuery(org.openbravo.model.ad.access.ProcessAccess.class,
                            "as e where e.client.id =:client and e.eUTDelegationLine.id=:delId and e.eutRecalculate='Y'");
                    processaccess.setNamedParameter("client", clientId);
                    processaccess.setNamedParameter("delId", delegateLnObj.getId());

                    if (processaccess.list().size() > 0) {
                      recal = true;
                    }
                  }
                }
              }
            }
            if (recal.equals(true)) {
              Boolean delegateLnId = delegation(delegateLnObj);
              return delegateLnId;
            }
          }
        }
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in delegationRecalculate " + e.getMessage());

    } finally {
      OBContext.restorePreviousMode();
    }
    return false;
  }

  /**
   * It checks whether a record is available in approval delegation that lies within the current
   * date
   * 
   * @param DelegationLine
   * @return true --success
   */
  private boolean delegation(EutDocappDelegateln delegateLnObj) {
    try {
      OBQuery<EutDocappDelegateln> delegateList = OBDal.getInstance().createQuery(
          EutDocappDelegateln.class,
          "as e  join e.eUTDocappDelegate as delhd  where delhd.client.id =:client and "
              + "(to_Date(to_char(delhd.fromDate,'yyyy-MM-dd'),'yyyy-MM-dd') <= "
              + "to_date(to_char(current_timestamp(),'yyyy-MM-dd'),'yyyy-MM-dd') and "
              + "to_Date(to_char(delhd.date,'yyyy-MM-dd'),'yyyy-MM-dd') >= "
              + "to_date(to_char(current_timestamp(),'yyyy-MM-dd'),'yyyy-MM-dd')) and e.role.id=:roleId ");
      delegateList.setNamedParameter("client", delegateLnObj.getClient().getId());
      delegateList.setNamedParameter("roleId", delegateLnObj.getRole().getId());

      if (delegateList.list().size() > 0) {
        return true;
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in delegationRecalculate " + e.getMessage());

    }
    return false;
  }
}
