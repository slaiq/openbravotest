/*
 *************************************************************************
 * All Rights Reserved.
 * Contributor(s):  Qualian
 ************************************************************************
 */
package sa.elm.ob.finance.event.dao;

import java.sql.PreparedStatement;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Priyanka Ranjan on 13/10/2017
 * 
 */
// AccountTreeEvent DAO file

public class AccountTreeElementValueEventDAO {
  private static final Logger LOG = LoggerFactory.getLogger(AccountTreeElementValueEventDAO.class);

  public static void updateDeptFund(String elementid, String clientid, ConnectionProvider conn,
      boolean isdeptfund) {
    PreparedStatement query = null;
    String strQuery = "";
    try {
      OBContext.setAdminMode();

      strQuery = "update c_elementvalue set em_efin_isdeptfund=? where c_elementvalue_id in (select replace(unnest(string_to_array "
          + " (eut_getchildacct(?),',')::character varying []),'''','')) and ad_client_id=? and "
          + " c_elementvalue_id not in (select account_id from c_validcombination where em_efin_uniquecode is not null and ad_client_id=?)";

      query = conn.getPreparedStatement(strQuery);
      if (isdeptfund) {
        query.setString(1, "Y");
      } else {
        query.setString(1, "N");
      }
      query.setString(2, elementid);
      query.setString(3, clientid);
      query.setString(4, clientid);

      query.executeUpdate();

    } catch (Exception e) {
      LOG.error("Exception while updateDeptFund:" + e, e);
      throw new OBException(e.getMessage());
    } finally {
      // close connection
      try {
        if (query != null) {
          query.close();
        }
      } catch (Exception e) {

      }
      OBContext.restorePreviousMode();
    }

  }
}
