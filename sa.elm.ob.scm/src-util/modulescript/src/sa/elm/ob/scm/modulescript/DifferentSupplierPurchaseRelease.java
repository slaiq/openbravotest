/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2010 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package sa.elm.ob.finance.modulescript;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;

/**
 * 
 * @author Kiruthika
 */

/*
 * If the supplier is different in Purchase Release header from lines, header supplier is changed to lines supplier.
 * 
 */

public class DifferentSupplierPurchaseRelease extends ModuleScript {

  static Logger log4j = Logger.getLogger(DifferentSupplierPurchaseRelease.class);

  public void execute() {
    StringBuilder queryBuilder = new StringBuilder();
    ConnectionProvider cp = getConnectionProvider();
    PreparedStatement st = null, st1 = null;
    
    ResultSet rs = null, rs1 = null;
    
    String supplierId = null, orderId = null;
    String query = "";
    try {
      queryBuilder.append(
          "select distinct ordln.c_bpartner_id, ordln.c_order_id from c_order ord join c_orderline ordln on ord.c_order_id = ordln.c_order_id ");
      queryBuilder.append(
          "where EM_Escm_Ordertype = 'PUR_REL' and ord.c_bpartner_id <> ordln.c_bpartner_id");

      st = cp.getPreparedStatement(queryBuilder.toString());
      rs = st.executeQuery();

      while (rs.next()) {
        if (StringUtils.isNotEmpty(UtilSql.getValue(rs, "c_bpartner_id")) && 
                StringUtils.isNotEmpty(UtilSql.getValue(rs, "c_order_id"))) {
          supplierId = UtilSql.getValue(rs, "c_bpartner_id");
          orderId = UtilSql.getValue(rs, "c_order_id");

          query = "update c_order set c_bpartner_id = ? where c_order_id = ? ";

          st1 = cp.getPreparedStatement(query);
          st1.setString(1, supplierId);
          st1.setString(2, orderId);

          st1.executeUpdate();
          
        }
      }

    } catch (Exception e) {
      log4j.error("Error in DifferentSupplierPurchaseRelease modulescript:" + e);
      handleError(e);
    } finally {
      try {
        if (st != null) {
          st.close();
        }
        if (rs != null) {
          rs.close();
        }
      } catch (Exception e) {
        handleError(e);
      }
    }

  }

  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("0B1224F8F87F4ED68E04C68BC423BC3A", null,
        new OpenbravoVersion(1, 0, 116));
  }
}