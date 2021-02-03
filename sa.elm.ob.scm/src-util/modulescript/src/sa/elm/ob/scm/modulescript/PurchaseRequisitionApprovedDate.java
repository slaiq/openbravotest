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

package sa.elm.ob.scm.modulescript;

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
 * Task No 7991: Date and timestamp not updating properly in PR Action History
 * 
 */

public class PurchaseRequisitionApprovedDate extends ModuleScript {

  static Logger log4j = Logger.getLogger(PurchaseRequisitionApprovedDate.class);

  public void execute() {
    
    ConnectionProvider cp = getConnectionProvider();
    PreparedStatement st = null;
    String query = "";
    try {

          // Update Approved date with created date in PR Action History table
          query = "update ESCM_PurchaseReq_App_Hist set approveddate = created ";

          st = cp.getPreparedStatement(query);
          st.executeUpdate();
          
    } catch (Exception e) {
      log4j.error("Error in PurchaseRequisitionApprovedDate modulescript:" + e);
      handleError(e);
    } finally {
      try {
        if (st != null) {
          st.close();
        }
      } catch (Exception e) {
        handleError(e);
      }
    }

  }

  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("0B1224F8F87F4ED68E04C68BC423BC3A", null,
        new OpenbravoVersion(1, 0, 128));
  }
}