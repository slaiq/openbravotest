package sa.elm.ob.finance.ad_callouts;

import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

public class FinancialAccFetchGainLossAcc extends SimpleCallout {

  /**
   * Callout to update the Gain and Loss account based on general ledger
   */
  private static final long serialVersionUID = 1L;
  private static final Logger log = Logger.getLogger(FinancialAccFetchGainLossAcc.class);

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;

    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpAcctSchemaId = vars.getStringParameter("inpcAcctschemaId");

    log.debug("inpLastFieldChanged:" + inpLastFieldChanged);
    if (inpLastFieldChanged.equals("inpcAcctschemaId")) {
      String gainAcc = "", lossAcc = "", feeAcc = "";
      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(
          "select b_revaluationgain_acct, b_revaluationloss_acct, b_expense_acct from C_AcctSchema_Default "
              + "where c_acctschema_id=?");
      @SuppressWarnings("unchecked")
      List<String> list = query.setString(0, inpAcctSchemaId).list();
      if (query != null && list.size() > 0) {
        for (@SuppressWarnings("rawtypes")
        Iterator iterator = list.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          gainAcc = row[0].toString();
          lossAcc = row[1].toString();
          feeAcc = row[2].toString();
        }
      }
      log.debug("gain n loss>>" + gainAcc + "," + lossAcc);
      info.addResult("inpfinBankrevaluationgainAcct", gainAcc);
      info.addResult("inpfinBankrevaluationlossAcct", lossAcc);
      info.addResult("inpfinBankfeeAcct", feeAcc);
    }
  }
}
