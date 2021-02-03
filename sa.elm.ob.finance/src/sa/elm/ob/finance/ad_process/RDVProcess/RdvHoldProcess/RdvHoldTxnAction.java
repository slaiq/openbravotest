package sa.elm.ob.finance.ad_process.RDVProcess.RdvHoldProcess;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.exception.NoConnectionAvailableException;

import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author Gopalakrishnan on 28/02/2019
 *
 */
public class RdvHoldTxnAction extends HttpSecureAppServlet {

  /**
   * Servlet implementation class to perform hold action process on RDV
   */
  private static final long serialVersionUID = 1L;

  private static final String TMP_DIR_PATH = "/tmp";
  private static final String DESTINATION_DIR_PATH = "/files";
  private File tmpDir = null, destinationDir = null;
  private static final String includeIn = "../web/sa.elm.ob.finance/jsp/RdvHoldProcess/RdvHoldAction.jsp";

  public void init(ServletConfig config) {

    super.init(config);
    boolHist = false;

    tmpDir = new File(TMP_DIR_PATH);
    if (!tmpDir.isDirectory()) {
      new File(TMP_DIR_PATH).mkdir();
    }

    String realPath = getServletContext().getRealPath(DESTINATION_DIR_PATH);
    destinationDir = new File(realPath);
    if (!destinationDir.isDirectory()) {
      new File(realPath).mkdir();
    }
  }

  /**
   * @see HttpServlet#HttpServlet()
   */
  public RdvHoldTxnAction() {
    super();
      }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    Connection con = null;
    RdvHoldActionDAO dao = null;
    try {
      con = getConnection();
      dao = new RdvHoldActionDAOimpl(con);
    } catch (NoConnectionAvailableException e) {
    }
    OBContext.setAdminMode();
    String inpAction = (request.getParameter("action") == null
        || "".equals(request.getParameter("action")) ? "" : request.getParameter("action"));
    if (inpAction.equals("")) {
      VariablesSecureApp vars = new VariablesSecureApp(request);
      String inpRDVTxnId = vars.getStringParameter("Efin_Rdvtxn_ID");
      DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
      SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
      String date = df.format(new Date());
      try {
        date = dateYearFormat.format(df.parse(date));
      } catch (ParseException e) {
      }
      date = UtilityDAO.convertTohijriDate(date);

      request.setAttribute("today", date);
      request.setAttribute("inpholdtype",
          dao.getHoldType(vars.getClient(), null, vars.getLanguage()));
      request.setAttribute("inpRDVTxnId", inpRDVTxnId);
      request.setAttribute("inpinvoiceNo", dao.getinvoiceno(vars.getClient()));
      EfinRDVTransaction rdvtrx = OBDal.getInstance().get(EfinRDVTransaction.class, inpRDVTxnId);
      request.setAttribute("inptrxappNo", rdvtrx.getTXNVersion());
      request.setAttribute("inpmatamt", rdvtrx.getNetmatchAmt().add(rdvtrx.getHoldamount()));
      request.setAttribute("inpnetamt", rdvtrx.getNetmatchAmt());
      request.setAttribute("inprdvtrxtype", rdvtrx.getEfinRdv().getTXNType());
      request.setAttribute("inpverstatus", rdvtrx.getTxnverStatus());
      request.setAttribute("inplineno", "10");
      request.setAttribute("isTxn", "Y");
      // log4j.debug("inpbpartnername" + request.getAttribute("inpbpartnername"));
    }

    else if (inpAction.equals("Close")) {
      VariablesSecureApp vars = new VariablesSecureApp(request);

      printPageClosePopUp(response, vars);
    } else {
      request.setAttribute("Result", null);
    }

    request.getRequestDispatcher(includeIn).forward(request, response);

  }
}
