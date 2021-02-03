/*
 *************************************************************************
 * All Rights Reserved.
 * Contributor(s):  Qualian
 ************************************************************************
 */
package sa.elm.ob.finance.event.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINCostcenters;
import sa.elm.ob.finance.EFINCostorgnization;

/**
 * 
 * @author Priyanka Ranjan on 06/09/2017
 * 
 */
// CostCenterLinkingLineOrgEvent DAO file

public class CostCenterLinkingLineOrgEventDAO {

  private static final Logger LOG = LoggerFactory.getLogger(CostCenterLinkingLineOrgEventDAO.class);

  /**
   * 
   * @param costorg
   */
  // check records in cost center tab for selected organization and set the value of enable based on
  // org
  public static void updatecostcenterenablevalue(EFINCostorgnization costorg, boolean isactive) {
    try {
      OBContext.setAdminMode();
      if (costorg.getEFINCostcentersList() != null) {
        List<EFINCostcenters> costCenter = costorg.getEFINCostcentersList();

        for (EFINCostcenters department : costCenter) {
          department.setActive(isactive);
          OBDal.getInstance().save(department);
        }
      }
    } catch (OBException e) {
      LOG.error("Exception while updatecostcenterenablevalue:" + e, e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /**
   * 
   * @param elementid
   * @param clientid
   * @param orgid
   * @param conn
   */
  // Update enable value in all child account's org and also respective cost center based on
  // selected organization

  public static void updatecostorganizationenablevalue(String elementid, String clientid,
      String orgid, ConnectionProvider conn, boolean isactive) {
    PreparedStatement query = null;
    ResultSet rs = null;
    String strQuery = "", active = "", costorgid = "";
    try {
      OBContext.setAdminMode();

      strQuery = "select efin_costorgnization_id from efin_costorgnization where c_elementvalue_id in "
          + "(select c_elementvalue_id from c_elementvalue where "
          + "c_elementvalue_id in (select replace(unnest(string_to_array"
          + "(eut_getchildacct(?),',')::character varying []),'''',''))) and ad_client_id=? and org=? and isactive=?";

      query = conn.getPreparedStatement(strQuery);
      query.setString(1, elementid);
      query.setString(2, clientid);
      query.setString(3, orgid);

      if (isactive) {
        active = "N";
        query.setString(4, active);
      } else {
        active = "Y";
        query.setString(4, active);
      }

      rs = query.executeQuery();

      while (rs.next()) {
        costorgid = rs.getString("efin_costorgnization_id");

        strQuery = " update efin_costorgnization set isactive=?,enabledisable =? where efin_costorgnization_id= ? ";
        query = conn.getPreparedStatement(strQuery);

        if (isactive) {
          active = "Y";
          query.setString(1, active);
          query.setString(2, active);
        } else {
          active = "N";
          query.setString(1, active);
          query.setString(2, active);
        }
        query.setString(3, costorgid);
        query.executeUpdate();

        // update all enable value for cost center records for selected org
        /*
         * updatecostcenterenablevalue(OBDal.getInstance().get(EFINCostorgnization.class,
         * costorgid), isactive);
         */
      }
    } catch (OBException e) {
      LOG.error("Exception while updatecostorganizationenablevalue:" + e, e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e.getMessage());
    } catch (SQLException e) {
      LOG.error("Exception while updatecostorganizationenablevalue:" + e, e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      LOG.error("Exception while updatecostorganizationenablevalue:" + e, e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}
