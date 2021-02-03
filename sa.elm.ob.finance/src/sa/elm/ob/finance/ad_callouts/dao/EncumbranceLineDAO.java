package sa.elm.ob.finance.ad_callouts.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author qualian on 08/08/2017
 */

public class EncumbranceLineDAO {
  /**
   * This Access Layer class is responsible to do database operation in BudgetAddLineProcess process
   * Class
   */
  VariablesSecureApp vars = null;

  private final static Logger log = LoggerFactory.getLogger(EncumbranceLineDAO.class);

  /**
   * get parent account
   * 
   * @param childAcctId
   * @return
   */
  @SuppressWarnings("unused")
  public static List<ElementValue> getParentAcct(String childAcctId, Connection conn) {
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    List<ElementValue> elemlist = new ArrayList<ElementValue>();
    try {
      query = " select el.c_elementvalue_id  "
          + " from c_elementvalue  el "
          + " where  el.c_elementvalue_id in ( "
          + " select replace(unnest(string_to_array(eut_getparentacct(?,null),',')::character varying []),'''',''))  and el.em_efin_projacct='Y' ";
      if (query != null) {
        ps = conn.prepareStatement(query);
        ps.setString(1, childAcctId);
        log.debug("getParentAcct:" + ps.toString());
        rs = ps.executeQuery();
        while (rs.next()) {
          ElementValue elm = OBDal.getInstance().get(ElementValue.class,
              rs.getString("c_elementvalue_id"));
          elemlist.add(elm);
        }
        return elemlist;
      }
      return elemlist;
    } catch (Exception e) {
      log.error("Exception in getParentAcct " + e.getMessage());
      return elemlist;
    } finally {

      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
    }
  }

  @SuppressWarnings("unused")
  public static int chkprojectacctornot(String childAcctId, Connection conn) {
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    int countprojectacct = 0;
    try {
      query = " select count(el.c_elementvalue_id)as count  "
          + " from c_elementvalue  el "
          + " where  el.c_elementvalue_id in ( "
          + " select replace(unnest(string_to_array(eut_getparentacct(?,null),',')::character varying []),'''','')) and el.em_efin_projacct='Y' ";
      if (query != null) {
        ps = conn.prepareStatement(query);
        ps.setString(1, childAcctId);
        log.debug("chkprojectacctornot:" + ps.toString());
        rs = ps.executeQuery();
        if (rs.next()) {
          countprojectacct = rs.getInt("count");
          return countprojectacct;
        } else
          return countprojectacct;

      }
      return countprojectacct;
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Exception in chkprojectacctornot " + e.getMessage());
      return countprojectacct;
    } finally {

      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
    }
  }
}
