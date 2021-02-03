package sa.elm.ob.utility.util;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;

public class UtilityAjax extends HttpSecureAppServlet {
	private static final long serialVersionUID = 1L;

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		Connection con = null;
		try {
			con = getConnection();
			String action = (request.getParameter("action") == null ? "" : request.getParameter("action"));
			if("getJSDateFormat".equals(action)) {
				JSONObject result = new JSONObject();
				try {
					result.put("jsDateFormat", Utility.dateFormatJS);
					result.put("jsDateTimeFormat", Utility.dateTimeFormatJS);
				}
				catch (final Exception e) {
					log4j.error("Exception in UtilityAjax - getJSDateFormat : ", e);
				}
				finally {
					response.setContentType("application/json");
					response.setCharacterEncoding("UTF-8");
					response.setHeader("Cache-Control", "no-cache");
					response.getWriter().write(result.toString());
				}
			}
			else if("GetProductUnit".equals(action)) {
				JSONObject result = null;
				try {
					result = UtilityDAO.getProductUoM(request.getParameter("id"));
				}
				catch (final Exception e) {
					log4j.error("Exception in UtilityAjax - GetProductUnit : ", e);
				}
				finally {
					response.setContentType("application/json");
					response.setCharacterEncoding("UTF-8");
					response.setHeader("Cache-Control", "no-cache");
					response.getWriter().write(result.toString());
				}
			}
		}
		catch (final Exception e) {
			log4j.error("Error in UtilityAjax : ", e);
		}
		finally {
			try {
				con.close();
			}
			catch (final SQLException e) {
				log4j.error("Error in UtilityAjax : ", e);
			}
		}
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		doPost(request, response);
	}

	public String getServletInfo() {
		return "UtilityAjax Servlet";
	}
}