package sa.elm.ob.finance.ad_process.RDVProcess;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
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

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.exception.NoConnectionAvailableException;

import sa.elm.ob.finance.ad_process.RDVProcess.DAO.PenaltyActionDAO;
import sa.elm.ob.utility.util.UtilityDAO;

public class RdvApplyPenalty extends HttpSecureAppServlet {

  /**
   * Servlet implementation class to import the budget
   */
  private static final long serialVersionUID = 1L;

  private static final String TMP_DIR_PATH = "/tmp";
  private static final String DESTINATION_DIR_PATH = "/files";
  private File tmpDir = null, destinationDir = null;
  private static final String includeIn = "../web/sa.elm.ob.finance/jsp/RDVProcess/Bulkpenaltyrdv.jsp";

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
  // public PenaltyAction() {
  // super();
  // }

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
    PenaltyActionDAO dao = null;
    try {
      con = getConnection();
      dao = new PenaltyActionDAO(con);
    } catch (NoConnectionAvailableException e) {
    }
    OBContext.setAdminMode();
    AddDefaultPenaltyDAO penaltyDAO = new AddDefaultPenaltyDAOImpl();
    String inpAction = (request.getParameter("action") == null
        || "".equals(request.getParameter("action")) ? "" : request.getParameter("action"));
    if (inpAction.equals("")) {
      VariablesSecureApp vars = new VariablesSecureApp(request);
      String inpRDVTxnLineId = vars.getStringParameter("Efin_Rdvtxnline_ID");
      DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
      SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
      String date = df.format(new Date());
      try {
        date = dateYearFormat.format(df.parse(date));
      } catch (ParseException e) {
      }
      JSONObject selectedRecords = new JSONObject();
      date = UtilityDAO.convertTohijriDate(date);

      // JSONObject combinationObject = penaltyDAO.getDefaultUniqueCode(inpRDVTxnLineId,
      // vars.getClient());
      JSONArray transactionIds = new JSONArray();
      String selectedRecord = request.getParameter("inpselectedRecordsId").toString();
      String selectedRecordArray[] = selectedRecord.split(",");
      if (selectedRecordArray.length > 0) {
        for (int i = 0; i < selectedRecordArray.length; i++) {
          transactionIds.put(selectedRecordArray[0]);
        }
      } else {
        transactionIds.put(selectedRecord);
      }
      selectedRecords = penaltyDAO.getSelectedRecordsInformation(transactionIds);

      request.setAttribute("today", date);
      request.setAttribute("inpselectedRecordsId",
          request.getParameter("inpselectedRecordsId").toString());
      request.setAttribute("inpinvoiceNo", dao.getinvoiceno(vars.getClient()));
      request.setAttribute("inppenaltytype",
          dao.getpenaltyType(vars.getClient(), null, vars.getLanguage()));
      request.setAttribute("inpRDVTxnLineId", inpRDVTxnLineId);
      // EfinRDVTxnline rdvtrxln = OBDal.getInstance().get(EfinRDVTxnline.class, inpRDVTxnLineId);
      // request.setAttribute("inptrxappNo", rdvtrxln.getTrxappNo());
      try {
        request.setAttribute("inpmatamt",
            selectedRecords.has("matchAmt") ? selectedRecords.getString("matchAmt")
                : BigDecimal.ZERO);
        request.setAttribute("inpnetamt",
            selectedRecords.has("netMatchAmt") ? selectedRecords.getString("netMatchAmt")
                : BigDecimal.ZERO);
        request.setAttribute("inprdvtrxtype",
            selectedRecords.has("txnType") ? selectedRecords.getString("txnType") : null);
        request.setAttribute("inpverstatus",
            selectedRecords.has("txnVersionStatus") ? selectedRecords.getString("txnVersionStatus")
                : null);
        request.setAttribute("inplineuniquecode", "");
      } catch (JSONException e) {
      }
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
