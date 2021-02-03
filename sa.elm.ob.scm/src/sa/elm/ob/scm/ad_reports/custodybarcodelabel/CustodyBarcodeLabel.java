package sa.elm.ob.scm.ad_reports.custodybarcodelabel;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustodyBarcodeLabel extends HttpSecureAppServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.scm/jsp/custodybarcodelabel/CustodyBarcodeLabel.jsp";
  private static final Logger log = LoggerFactory.getLogger(CustodyBarcodeLabel.class);

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    try {
      String action = (request.getParameter("inpAction") == null ? ""
          : request.getParameter("inpAction"));
      CustodyBarcodeLabelDAO dao = new CustodyBarcodeLabelDAO(getConnection());
      VariablesSecureApp vars = new VariablesSecureApp(request);
      String strReportName = "";
      if (action.equals("")) {
        request.setAttribute("MaterialIssueRequests", dao.getMIRNos(vars.getRole()));
        request.setAttribute("BeneficiaryTypeList", dao.getBeneficiaryTypeList());
        // request.setAttribute("TagsList", dao.getTagsList(vars.getRole()));

        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.getRequestDispatcher(jspPage).include(request, response);
      }
      // else if (action.equals("getTagList")) {
      // JSONObject jsob = new JSONObject();
      // jsob = dao.getTagsList(vars.getRole());
      // response.setContentType("application/json");
      // response.setCharacterEncoding("UTF-8");
      // response.setHeader("Cache-Control", "no-cache");
      // response.getWriter().write(jsob.toString());
      // }
      else if (action.equals("Submit")) {
        String inpMIRid = request.getParameter("inpMIRid");
        String inpBeneficiarytype = request.getParameter("inpBeneficiarytype");
        String inpBeneficiaryid = request.getParameter("inpBeneficiaryid");
        String inpTagFrom = request.getParameter("inpTagFromNo");
        String inpTagTo = request.getParameter("inpTagToNo");

        String params = "";
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        if (inpMIRid != null && !inpMIRid.equals("0") && !inpMIRid.equals("")) {
          params = " and mreqln.escm_material_request_id='" + inpMIRid + "' ";
        }
        if ((inpTagFrom != null && inpTagTo != null)
            && (!inpTagFrom.equals("") && !inpTagTo.equals(""))
            && (!inpTagFrom.equals("0") && !inpTagTo.equals("0"))) {
          params = " and mrcus.documentno>='" + inpTagFrom + "' and mrcus.documentno<='" + inpTagTo
              + "' ";

        }
        if ((inpBeneficiarytype != null && inpBeneficiaryid != null)
            && (!inpBeneficiarytype.equals("") && !inpBeneficiaryid.equals(""))
            && (!inpBeneficiarytype.equals("0") && !inpBeneficiaryid.equals("0"))) {
          params = " and mrcus.beneficiary_type='" + inpBeneficiarytype
              + "' and mrcus.beneficiary_name='" + inpBeneficiaryid + "'  and mrcus.status='IU' ";
        }
        params += " and mrcus.ad_client_id='" + vars.getClient() + "' ";
        /*
         * parameters.put("inpBeneficiaryType", inpBeneficiarytype);
         * parameters.put("inpBeneficiaryId", inpBeneficiaryid);
         */
        // parameters.put("inpRoleId", vars.getRole());
        parameters.put("params", params);
        log4j.debug("params>" + params);
        strReportName = "@basedesign@/sa/elm/ob/scm/ad_reports/custodybarcodelabel/CustodyBarcodeLabel.jrxml";
        String strOutput = "pdf";

        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      }
    } catch (Exception e) {
      log.error("error custody barcode report" + e);
      // TODO: handle exception
    } finally {

    }
  }
}