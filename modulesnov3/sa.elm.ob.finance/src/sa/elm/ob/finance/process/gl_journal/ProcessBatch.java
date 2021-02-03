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
 * All portions are Copyright (C) 2010-2012 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package sa.elm.ob.finance.process.gl_journal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.financialmgmt.gl.GLBatch;
import org.openbravo.model.financialmgmt.gl.GLJournal;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

public class ProcessBatch extends DalBaseProcess {

	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {
		int successCont = 0;
		int errorCont = 0;
		String errorMsg = "";
		boolean errorFlag = false;
		// Recover context and variables
		ConnectionProvider conn = bundle.getConnection();
		VariablesSecureApp varsAux = bundle.getContext().toVars();
		HttpServletRequest request = RequestContext.get().getRequest();

		OBContext.setOBContext(varsAux.getUser(), varsAux.getRole(), varsAux.getClient(), varsAux.getOrg());
		VariablesSecureApp vars = new VariablesSecureApp(request);
		String sql = "";
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {

			// retrieve the parameters from the bundle
			final String batchId = (String) bundle.getParams().get("GL_JournalBatch_ID");

			GLBatch batch = OBDal.getInstance().get(GLBatch.class, batchId);

			/* check sum of same unique code  debit value is more than funds available */
			sql = " select em_efin_uniquecode, sum(amtacctdr) as sum,em_efin_budgetlines_id,  coalesce(ln.funds_available,0.00) as funds_available  from gl_journalbatch  bat left join gl_journal jo on jo.gl_journalbatch_id= bat.gl_journalbatch_id "
					+ "left join gl_journalline jl on jl.gl_journal_id= jo.gl_journal_id left join efin_budgetlines ln on ln.efin_budgetlines_id =jl.em_efin_budgetlines_id " + " where bat.gl_journalbatch_id  ='" + batchId
					+ "'  and  amtacctdr > 0   and em_efin_budgetlines_id is not null  group  by em_efin_uniquecode,em_efin_budgetlines_id, ln.funds_available";
			ps = conn.getPreparedStatement(sql);
			rs = ps.executeQuery();
			while (rs.next()) {

				if(rs.getBigDecimal("funds_available").compareTo(rs.getBigDecimal("sum")) < 0) {
					errorFlag = true;
					errorMsg = "Process Failed:Sum of debits" + rs.getInt("sum") + "  is greater than the budget's funds available " + rs.getInt("funds_available") + "  for the code " + rs.getString("em_efin_uniquecode") + "";
				}
			}
			if(!errorFlag) {
				// Process the Batch
				for (GLJournal journal : batch.getFinancialMgmtGLJournalList()) {
					if(!journal.isProcessed() && journal.getDocumentStatus().equals("DR")) {
						// Recover again the object to avoid problems with Dal
						journal = OBDal.getInstance().get(GLJournal.class, journal.getId());
						ProcessBundle pb = new ProcessBundle("CC92F947C77D48EEAA444B7105F8C020", vars).init(conn);
						HashMap<String, Object> parameters = new HashMap<String, Object>();
						parameters.put("GL_Journal_ID", journal.getId());
						pb.setParams(parameters);
						OBError myMessage = null;
						// Process each Joural
						FIN_AddPaymentFromJournal myProcess = new FIN_AddPaymentFromJournal();
						myProcess.execute(pb);
						myMessage = (OBError) pb.getResult();

						if(myMessage.getType().equals("Error")) {
							errorCont++;
							if(!"".equals(errorMsg)) {
								errorMsg = errorMsg + "<br />";
							}
							errorMsg = errorMsg + "@FIN_JournalBatchErrorProcess@ " + journal.getDocumentNo() + ". " + myMessage.getMessage();
						}
						else {
							successCont++;
						}
					}
				}
			}
			// OBError is also used for successful results
			final OBError msg = new OBError();

			if(errorCont == 0 && !errorFlag) {
				msg.setType("Success");
				msg.setTitle("@Success@");
				batch = OBDal.getInstance().get(GLBatch.class, batchId);
				batch.setProcessed(true);
				batch.setUpdated(new Date());
				OBDal.getInstance().flush();
			}
			else if((errorCont > 0 && successCont == 0) || errorFlag) {
				msg.setType("Error");
				msg.setTitle("@Error@");
				msg.setMessage(errorMsg);
			}
			else {
				msg.setType("Warning");
				msg.setTitle("@Warning@");
				msg.setMessage(errorMsg);
			}

			bundle.setResult(msg);

		}
		catch (final OBException e) {
			final OBError msg = new OBError();
			msg.setType("Error");
			msg.setMessage(e.getMessage());
			OBDal.getInstance().rollbackAndClose();
			bundle.setResult(msg);
		}

	}
}
