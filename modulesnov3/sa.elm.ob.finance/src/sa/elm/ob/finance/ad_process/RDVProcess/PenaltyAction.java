package sa.elm.ob.finance.ad_process.RDVProcess;

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

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.exception.NoConnectionAvailableException;

import sa.elm.ob.finance.EfinPenaltyAction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.ad_process.RDVProcess.DAO.PenaltyActionDAO;
import sa.elm.ob.utility.util.UtilityDAO;

public class PenaltyAction extends HttpSecureAppServlet {

  /**
   * Servlet implementation class to import the budget
   */
  private static final long serialVersionUID = 1L;

  private static final String TMP_DIR_PATH = "/tmp";
  private static final String DESTINATION_DIR_PATH = "/files";
  private File tmpDir = null, destinationDir = null;
  private static final String includeIn = "../web/sa.elm.ob.finance/jsp/RDVProcess/PenaltyAction.jsp";

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
  public PenaltyAction() {
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
    PenaltyActionDAO dao = null;
    try {
      con = getConnection();
      dao = new PenaltyActionDAO(con);
    } catch (NoConnectionAvailableException e) {
    }
    OBContext.setAdminMode();
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
      date = UtilityDAO.convertTohijriDate(date);
      AddDefaultPenaltyDAO penaltyDAO = new AddDefaultPenaltyDAOImpl();
      JSONObject combinationObject = penaltyDAO.getDefaultUniqueCode(inpRDVTxnLineId,
          vars.getClient());

      request.setAttribute("today", date);
      request.setAttribute("inpbpartnername",
          dao.getbpartnername(vars.getClient(), inpRDVTxnLineId));
      request.setAttribute("inppenaltytype",
          dao.getpenaltyType(vars.getClient(), null, vars.getLanguage()));
      request.setAttribute("inpRDVTxnLineId", inpRDVTxnLineId);
      request.setAttribute("inpinvoiceNo", dao.getinvoiceno(vars.getClient()));
      EfinRDVTxnline rdvtrxln = OBDal.getInstance().get(EfinRDVTxnline.class, inpRDVTxnLineId);
      request.setAttribute("inptrxappNo", rdvtrxln.getTrxappNo());
      request.setAttribute("inpmatamt", rdvtrxln.getMatchAmt());
      request.setAttribute("inpnetamt", rdvtrxln.getNetmatchAmt());
      try {
        request.setAttribute("inpuniquecode",
            dao.getBudgetAdjustmentUniquecode(vars.getClient(), inpRDVTxnLineId));
        request.setAttribute("inplineuniquecode",
            combinationObject.has("PenaltyCode") ? combinationObject.getString("PenaltyCode")
                : null);
        request.setAttribute("inplineuniquecodeId",
            combinationObject.has("PenaltyCodeId") ? combinationObject.getString("PenaltyCodeId")
                : null);
        request.setAttribute("inplineuniquecodeName",
            combinationObject.has("PenaltyCodeName")
                ? combinationObject.getString("PenaltyCodeName")
                : null);
        request.setAttribute("inpPenaltyAccountType",
            combinationObject.has("Type") ? combinationObject.getString("Type") : null);
      } catch (JSONException e) {

      }

      request.setAttribute("inprdvtrxtype", rdvtrxln.getEfinRdvtxn().getEfinRdv().getTXNType());
      request.setAttribute("inpverstatus", rdvtrxln.getEfinRdvtxn().getTxnverStatus());

      OBQuery<EfinPenaltyAction> action = OBDal.getInstance().createQuery(EfinPenaltyAction.class,
          " as e where e.efinRdvtxnline.id='" + inpRDVTxnLineId + "'");
      if (action.list().size() > 0) {
        int size = (action.list().size() * 10) + 10;
        request.setAttribute("inplineno", String.valueOf(size));
      } else
        request.setAttribute("inplineno", "10");
      log4j.debug("inpbpartnername" + request.getAttribute("inpbpartnername"));
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
