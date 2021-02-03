package sa.elm.ob.scm.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

public class ProductImageCallout extends SimpleCallout{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected void execute(CalloutInfo info) throws ServletException {
		// TODO Auto-generated method stub
		VariablesSecureApp vars = info.vars;
	    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
	    String strMProductID = vars.getStringParameter("inpmProductId");
	    
	    if (inpLastFieldChanged.equals("inpmProductId")){
	    	if (strMProductID != null) {
	    		info.addResult("inpdescription", "235A3AA1B2C544EF9420D456D42EACD8");
	    	}else {
	    		info.addResult("inpdescription", "");
			}
	    }
	}
	
}