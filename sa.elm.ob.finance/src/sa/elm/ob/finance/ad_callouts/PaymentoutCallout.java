package sa.elm.ob.finance.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.businesspartner.BankAccount;

public class PaymentoutCallout extends SimpleCallout {
	private static final long serialVersionUID = -8469446494495266203L;

	@Override
	protected void execute(CalloutInfo info) throws ServletException {
		VariablesSecureApp vars = info.vars;
		String inpsupbankId = vars.getStringParameter("inpemEfinBpBankaccountId");
		String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
		log4j.debug("inpsupbankId:" + inpsupbankId);
		if(inpLastFieldChanged.equals("inpemEfinBpBankaccountId")) {
			BankAccount bankaccount = OBDal.getInstance().get(BankAccount.class, inpsupbankId);
 			info.addResult("inpemEfinSupbankiban", bankaccount == null ? "" : bankaccount.getIBAN());
		}

	}
}
