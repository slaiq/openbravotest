package sa.elm.ob.scm.ad_forms.updatesequence;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;

public class UpdateSequence extends HttpSecureAppServlet {
	private static final long serialVersionUID = 1L;
	private String jspPage = "../web/sa.elm.ob.scm/jsp/updatesequence/UpdateSequence.jsp";

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			String action = (request.getParameter("inpAction") == null ? "" : request.getParameter("inpAction"));
			UpdateSequenceDAO dao = new UpdateSequenceDAO(getConnection());
			VariablesSecureApp vars = new VariablesSecureApp(request);
		
			//if(action.equals("")) {								
		        request.setAttribute("Prefix", dao.getPrefix(vars.getClient()));      
		        request.setAttribute("Organization", dao.getOrg(vars.getClient(), vars.getRole()));
		        request.setAttribute("OrgList", dao.getOrgList(vars.getClient(), vars.getRole()));
		    //}else 
		   if (action.equals("Submit")) {
				String inpOrgId = request.getParameter("inpOrgid");		        
				String inpPrefix = request.getParameter("inpPrefix");
				JSONObject result= dao.updateSequence(inpOrgId, inpPrefix);
				request.setAttribute("result", result);
		    }
		}
		catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		finally {
			response.setContentType("text/html; charset=UTF-8");
			response.setCharacterEncoding("UTF-8");
			request.getRequestDispatcher(jspPage).include(request, response);
		}
	}
}