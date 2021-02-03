package sa.elm.ob.utility.email;

import org.openbravo.email.EmailEventException;
import org.openbravo.email.EmailEventManager;

public class EmailManager {
	
	private static EmailEventManager emailEventManager = null;
	
	static {
		emailEventManager = org.openbravo.base.weld.WeldUtils.getInstanceFromStaticBeanManager(EmailEventManager.class);
	}

	public static boolean sendEmail(String event, final String recipient, Object data) throws EmailEventException {
			
		return emailEventManager.sendEmail(event, recipient, data);
	}

}
