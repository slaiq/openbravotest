/*
 *************************************************************************
 * All Rights Reserved. 
 * Contributor(s):  ___Qualian________________________________.
 ************************************************************************
 */
package sa.elm.ob.finance.ad_callouts;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.xmlEngine.XmlDocument;

public class SL_Invoice_PriceList extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    if (vars.commandIn("DEFAULT")) {
      String strChanged = vars.getStringParameter("inpLastFieldChanged");
      if (log4j.isDebugEnabled())
        log4j.debug("CHANGED: " + strChanged);
      String strMPriceListID = vars.getStringParameter("inpmPricelistId");
      String strTabId = vars.getStringParameter("inpTabId");

      try {
        printPage(response, vars, strMPriceListID, strTabId);
      } catch (ServletException ex) {
        pageErrorCallOut(response);
      }
    } else
      pageError(response);
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars,
      String strMPriceListID, String strTabId) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();

    SLOrderPriceListData[] data = SLOrderPriceListData.select(this, strMPriceListID);
    StringBuffer resultado = new StringBuffer();
    resultado.append("var calloutName='SL_Invoice_PriceList';\n\n");
    resultado.append("var respuesta = new Array(");
    if (data != null && data.length > 0) {
      resultado.append("new Array(\"inpistaxincluded\", \"" + data[0].istaxincluded + "\"),\n");
      resultado.append("new Array(\"inpcCurrencyId\", \"" + 317 + "\")\n");
    }
    resultado.append(");\n");
    xmlDocument.setParameter("array", resultado.toString());
    xmlDocument.setParameter("frameName", "appFrame");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }
}
