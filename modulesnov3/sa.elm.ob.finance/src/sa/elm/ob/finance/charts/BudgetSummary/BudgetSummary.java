package sa.elm.ob.finance.charts.BudgetSummary;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openbravo.base.secureApp.HttpSecureAppServlet;

/**
 * 
 *  @author Gopalakrishnan on 20/09/2016
 *
 */

public class BudgetSummary extends HttpSecureAppServlet
{
	private static final long serialVersionUID = 1L;
	private String includeIn = "../web/sa.elm.ob.finance/jsp/charts/BudgetSummary/BudgetSummary.jsp";
	
	public void doGet(HttpServletRequest request , HttpServletResponse response) throws ServletException, IOException
	{
		doPost(request,response);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		request.getRequestDispatcher(includeIn).forward(request, response);
	}
	
}