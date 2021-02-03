
/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package sa.elm.ob.finance.ad_process.RDVProcess.hook;

import java.util.Iterator;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;

import sa.elm.ob.finance.EfinRDVTransaction;

/**
 * 
 * @author sathishkumar.P
 *
 */
public class RDVSubmitCompletionHookCaller {

  @Inject
  @Any
  private Instance<RDVSubmitCompletionHook> hooks;

  /**
   * This method is used to execute the hooks post process when RDV transaction submit,revoke,reject
   * 
   * @param strInvoiceType
   * @param invoice
   * @return
   * @throws Exception
   */
  public JSONObject executeHook(EfinRDVTransaction rdvtxn, JSONObject paramters,
      VariablesSecureApp vars, org.openbravo.database.ConnectionProvider conn) throws Exception {
    JSONObject result = executeHooks(rdvtxn, paramters, vars, conn);
    return result;
  }

  private JSONObject executeHooks(EfinRDVTransaction rdvtxn, JSONObject paramters,
      VariablesSecureApp vars, org.openbravo.database.ConnectionProvider conn) throws Exception {
    JSONObject result = new JSONObject();
    for (Iterator<RDVSubmitCompletionHook> procIter = hooks.iterator(); procIter.hasNext();) {
      RDVSubmitCompletionHook proc = procIter.next();
      result = proc.exec(rdvtxn, paramters, vars, conn);
    }
    return result;
  }

}
