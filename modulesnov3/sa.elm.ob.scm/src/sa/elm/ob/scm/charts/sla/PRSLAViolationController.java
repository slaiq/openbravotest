package sa.elm.ob.scm.charts.sla;

import java.io.IOException;
import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PRSLAViolationController extends HttpSecureAppServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		handleRequest(request, response);
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		doPost(request, response);
	}

	private void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		RequisitionFacadeImpl purchaseRequisitionFacade = new RequisitionFacadeImpl();
		Hashtable<String, Integer> slaViolationList = purchaseRequisitionFacade.getSlaViolation();
		ObjectMapper mapper = new ObjectMapper();
		response.getWriter().write(mapper.writeValueAsString(slaViolationList));
	}

}
