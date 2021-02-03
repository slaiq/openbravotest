package sa.elm.ob.finance.ad_reports.Manualadjustment.header;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;

public class ManualAdjustmentReport extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.finance/jsp/Manualadjustment/ManualAdjustment.jsp";

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    try {
      String strReportName = "";
      String action = (request.getParameter("inpAction") == null ? "" : request
          .getParameter("inpAction"));

      VariablesSecureApp vars = new VariablesSecureApp(request);
      Connection con = getConnection();
      OBContext.setAdminMode();

      String inpClientId = "";
      inpClientId = vars.getClient();

      if (action.equals("")) {
        log4j.debug("action");
        // request.setAttribute("inpglJournalId", vars.getStringParameter("inpglJournalId"));
        request.setAttribute("inphDoctypelist", vars.getStringParameter("inphDoctypelist"));

        // Localization support
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.getRequestDispatcher(jspPage).include(request, response);
      } else if (action.equals("pdf")) {
        // String inpglJournalId = request.getParameter("inpglJournalId");
        String inpglJournalId = "";
        String inphDoctypelist = request.getParameter("Doctypelist");
        String inpRequestNo = request.getParameter("inpRequestNo");
        String sql = "";
        PreparedStatement st = null;
        ResultSet rs = null;

        if (inphDoctypelist.equals("gi")) {

          sql = "select c_doctype_id,gl_journal_id from gl_journal where documentno = ?  and ad_client_id=?  ";
          st = con.prepareStatement(sql);
          st.setString(1, inpRequestNo);
          st.setString(2, inpClientId);
          rs = st.executeQuery();

          if (rs.next()) {
            inphDoctypelist = rs.getString("c_doctype_id");
            inpglJournalId = rs.getString("gl_journal_id");

          }
        } else if (inphDoctypelist.equals("apa")) {

          sql = "select c_doctypetarget_id,c_invoice_id  from c_invoice where documentno = ? and ad_client_id=?  ";
          st = con.prepareStatement(sql);
          st.setString(1, inpRequestNo);
          st.setString(2, inpClientId);
          rs = st.executeQuery();

          if (rs.next()) {
            inphDoctypelist = rs.getString("c_doctypetarget_id");
            inpglJournalId = rs.getString("c_invoice_id");

          }
        }

        else if (inphDoctypelist.equals("recon")) {

          sql = "select fin_reconciliation_id  from fin_reconciliation where documentno = ? and ad_client_id=?  ";
          st = con.prepareStatement(sql);
          st.setString(1, inpRequestNo);
          st.setString(2, inpClientId);
          rs = st.executeQuery();

          if (rs.next()) {
            inpglJournalId = rs.getString("fin_reconciliation_id");

          }
        }

        else if (inphDoctypelist.equals("prje")) {

          sql = "select fin_finacc_transaction_id from fin_finacc_transaction where EM_Efin_Document_No = ? and posted='Y' and ad_client_id=?   and status='EFIN_CAN'";
          st = con.prepareStatement(sql);
          st.setString(1, inpRequestNo);
          st.setString(2, inpClientId);
          rs = st.executeQuery();

          if (rs.next()) {
            inpglJournalId = rs.getString("fin_finacc_transaction_id");

          }
        } else if (inphDoctypelist.equals("urje")) {
          sql = "select fin_reconciliation_id  from fin_reconciliation where documentno = ? and  Docstatus='EFIN_UREC' and ad_client_id=?  ";
          st = con.prepareStatement(sql);
          st.setString(1, inpRequestNo);
          st.setString(2, inpClientId);
          rs = st.executeQuery();

          if (rs.next()) {
            inpglJournalId = rs.getString("fin_reconciliation_id");

          }
        } else if (inphDoctypelist.equals("AR")) {

          inpglJournalId = inpRequestNo;
        }
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        log4j.debug("inpglJournalId:" + inpglJournalId);
        log4j.debug("inphDoctypelist value:" + inphDoctypelist);
        log4j.debug("inpRequestNo:" + inpRequestNo);
        parameters.put("inpGlJournalId", inpglJournalId);
        parameters.put("inphDoctypelist", inphDoctypelist);
        parameters.put("inpRequestNo", inpRequestNo);
        parameters.put("inpClientId", inpClientId);
        strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/Manualadjustment/GLAdjustment.jrxml";
        String strOutput = "pdf";
        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      } else if (action.equals("xls")) {
        String inpglJournalId = "";
        String inphDoctypelist = request.getParameter("Doctypelist");
        String inpRequestNo = request.getParameter("inpRequestNo");
        String sql = "";
        PreparedStatement st = null;
        ResultSet rs = null;
        if (inphDoctypelist.equals("gi")) {
          sql = "select c_doctype_id,gl_journal_id from gl_journal where documentno = ? and ad_client_id=?  ";
          st = con.prepareStatement(sql);
          st.setString(1, inpRequestNo);
          st.setString(2, inpClientId);
          rs = st.executeQuery();
          if (rs.next()) {
            inphDoctypelist = rs.getString("c_doctype_id");
            inpglJournalId = rs.getString("gl_journal_id");

          }
        } else if (inphDoctypelist.equals("apa")) {

          sql = "select c_doctypetarget_id,c_invoice_id  from c_invoice where documentno = ? and ad_client_id=?  ";
          st = con.prepareStatement(sql);
          st.setString(1, inpRequestNo);
          st.setString(2, inpClientId);
          rs = st.executeQuery();

          if (rs.next()) {
            inphDoctypelist = rs.getString("c_doctypetarget_id");
            inpglJournalId = rs.getString("c_invoice_id");

          }
        }

        else if (inphDoctypelist.equals("recon")) {

          sql = "select fin_reconciliation_id  from fin_reconciliation where documentno = ? and ad_client_id=? ";
          st = con.prepareStatement(sql);
          st.setString(1, inpRequestNo);
          st.setString(2, inpClientId);
          rs = st.executeQuery();

          if (rs.next()) {
            inpglJournalId = rs.getString("fin_reconciliation_id");

          }
        }

        else if (inphDoctypelist.equals("prje")) {

          sql = "select max(fin_finacc_transaction_id) as fin_finacc_transaction_id from fin_finacc_transaction where EM_Efin_Document_No = ? and posted='Y' and ad_client_id=?  ";
          st = con.prepareStatement(sql);
          st.setString(1, inpRequestNo);
          st.setString(2, inpClientId);
          rs = st.executeQuery();

          if (rs.next()) {
            inpglJournalId = rs.getString("fin_finacc_transaction_id");

          }
        } else if (inphDoctypelist.equals("urje")) {
          sql = "select fin_reconciliation_id  from fin_reconciliation where documentno = ? and  Docstatus='EFIN_UREC'  and ad_client_id=?  ";
          st = con.prepareStatement(sql);
          st.setString(1, inpRequestNo);
          st.setString(2, inpClientId);
          rs = st.executeQuery();

          if (rs.next()) {
            inpglJournalId = rs.getString("fin_reconciliation_id");

          }
        } else if (inphDoctypelist.equals("AR")) {

          inpglJournalId = inpRequestNo;
        }

        strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/Manualadjustment/GLAdjustment.jrxml";
        String strOutput = "xls";

        HashMap<String, Object> parameters = new HashMap<String, Object>();
        log4j.debug("inpglJournalId:" + inpglJournalId);
        log4j.debug("inphDoctypelist value:" + inphDoctypelist);
        log4j.debug("inpRequestNo:" + inpRequestNo);
        parameters.put("inpGlJournalId", inpglJournalId);
        parameters.put("inphDoctypelist", inphDoctypelist);
        parameters.put("inpRequestNo", inpRequestNo);
        parameters.put("inpClientId", inpClientId);
        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      }
    } catch (Exception e) {
      e.printStackTrace();
      log4j.error("Exception in ManualAdjustmentReport :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}