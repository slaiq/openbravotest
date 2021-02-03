package sa.elm.ob.utility.util;

import java.io.File;
import java.util.List;

import org.openbravo.email.EmailEventContentGenerator;;

public class IntegrationEmailContentGenerator implements EmailEventContentGenerator {

	@Override
	public boolean isValidEvent(String event, Object data) {
		return "EVT_NAME".equals(event);
	}

	@Override
	public String getSubject(Object data, String event) {
		return "Test Email Subject";
	}

	@Override
	public String getBody(Object data, String event) {
		return "Test Email Body.";
	}

	@Override
	public String getContentType() {
		return "text/plain; charset=utf-8";
	}

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public boolean preventsOthersExecution() {
		return false;
	}

	@Override
	public boolean isAsynchronous() {
		return false;
	}

	@Override
	public List<File> getAttachments(Object data, String event) {
		return null;
	}

}
