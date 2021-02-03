
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
 * All portions are Copyright (C) 2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  
 *************************************************************************
 */
package sa.elm.ob.finance.ad_process.PurchaseInvoice.hook;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.model.common.invoice.Invoice;

/**
 * Interface to be used to extend the Purchase Invoice Functionality and do some extra functionality
 * at the end of invoice submit process
 * 
 * @author Sathishkumar.p
 * 
 */
public interface PurchaseInvoiceCompletionHook {

  public JSONObject exec(String strInvoiceType, Invoice invoice, JSONObject paramters,
      VariablesSecureApp vars, org.openbravo.database.ConnectionProvider conn) throws Exception;
}
