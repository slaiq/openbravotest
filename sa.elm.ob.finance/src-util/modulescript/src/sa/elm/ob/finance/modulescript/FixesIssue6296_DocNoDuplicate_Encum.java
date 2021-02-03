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
 * @author Gopinagh. R
 */

/*
 * Fetch the encumbrances that are duplicated and randomize the doc no for the duplicated record.
 * 
 */

public class FixesIssue6296_DocNoDuplicate_Encum extends ModuleScript {

  static Logger log4j = Logger.getLogger(FixesIssue6296_DocNoDuplicate_Encum.class);

  public void execute() {
    StringBuilder queryBuilder = new StringBuilder();
    ConnectionProvider cp = getConnectionProvider();
    PreparedStatement st = null, st1 = null;
    ;
    ResultSet rs = null, rs1 = null;
    String documentId = "", clientId = "", orgId = "", yearId = "", docNo = "";
    int nextAssigned;

    try {
      queryBuilder.append(
          "select e.efin_budget_manencum_id,e.documentno, e.ad_client_id, e.ad_org_id, (select c_year_id from c_period where ad_client_id =e.ad_client_id and  e.dateacct between c_period.startdate and c_period.enddate  limit 1) as c_year_id from efin_budget_manencum e, efin_budget_manencum f ");
      queryBuilder.append(
          " where e.documentno = f.documentno and e.created > f.created and e.ad_client_id = f.ad_client_id ");
      queryBuilder.append("  union "
          + "  select e.efin_budget_manencum_id,e.documentno, e.ad_client_id, e.ad_org_id, (select c_year_id from c_period where ad_client_id =e.ad_client_id and e.dateacct between c_period.startdate and c_period.enddate  limit 1) as c_year_id "
          + "  from efin_budget_manencum e  where  (length(e.documentno) >9 ) ");
      queryBuilder.append(" order by documentno ");

      st = cp.getPreparedStatement(queryBuilder.toString());
      rs = st.executeQuery();

      while (rs.next()) {
        documentId = UtilSql.getValue(rs, "efin_budget_manencum_id");
        clientId = UtilSql.getValue(rs, "ad_client_id");
        orgId = UtilSql.getValue(rs, "ad_org_id");
        yearId = UtilSql.getValue(rs, "c_year_id");

        st1 = cp.getPreparedStatement("select efin_getdocseqnextassigned(?,?,?) as documentno");

        st1.setString(1, "efin_budget_manencum");
        st1.setString(2, yearId);
        st1.setString(3, clientId);

        rs1 = st1.executeQuery();

        if (rs1.next()) {

          if (StringUtils.isNotEmpty(UtilSql.getValue(rs1, "documentno"))) {
            nextAssigned = Integer.parseInt(UtilSql.getValue(rs1, "documentno"));

            updateDocumentNo(documentId, clientId, orgId, nextAssigned, cp);
          }
        }
      }

    } catch (Exception e) {
      log4j.error("Error while updating duplicates :" + e);
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

  private void updateDocumentNo(String revId, String clientId, String orgId, int docNo,
      ConnectionProvider cp) {
    PreparedStatement st = null;
    String query = "";
    try {
      query = "update efin_budget_manencum  set documentno  = ? where efin_budget_manencum_id = ? ";

      st = cp.getPreparedStatement(query);
      st.setInt(1, docNo);
      st.setString(2, revId);

      st.executeUpdate();

    } catch (Exception e) {
      log4j.error("Error while updateDocumentNo :" + e);
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
    return new ModuleScriptExecutionLimits("B0A58AE7D0994414B2B315E0A7087044", null,
        new OpenbravoVersion(1, 0, 39));
  }
}