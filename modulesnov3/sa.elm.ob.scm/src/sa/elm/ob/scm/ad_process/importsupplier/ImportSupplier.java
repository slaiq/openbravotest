package sa.elm.ob.scm.ad_process.importsupplier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.jfree.util.Log;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;

import sa.elm.ob.scm.ESCM_Certificates;
import sa.elm.ob.scm.actionHandler.SupplierController;
import sa.elm.ob.scm.actionHandler.TabadulIntegrationException;
import sa.elm.ob.scm.properties.Resource;

public class ImportSupplier extends HttpSecureAppServlet {

  private static final long serialVersionUID = 1L;
  String pageType = "";

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    JSONObject jsonResponse = new JSONObject();
    String lang = vars.getLanguage();
    StringBuilder certificateErrorMsg = new StringBuilder("");
    Boolean isInvalidCertificate = false;

    List<String> crNoList = new ArrayList<>();
    Map<String, String> certificateNumberMap = new HashMap<>();

    try {
      String strPopUp = request.getParameter("IsPopUpCall");
      if (StringUtils.isNotBlank(strPopUp)) {
        RequestDispatcher dispatch = request
            .getRequestDispatcher("../web/sa.elm.ob.scm/jsp/ImportSupplier/importSupplier.jsp");
        dispatch.forward(request, response);
      }

      if (request.getParameter("action") != null
          && request.getParameter("action").equals("importSupplier")) {
        String supplierCRN = request.getParameter("supplierCRN");
        if (StringUtils.isNotBlank(supplierCRN)) {
          SupplierController supplierController = new SupplierController();
          BusinessPartner partner = supplierController.getBusinessPartnerByCRN(supplierCRN);

          if (partner == null) {
            try {
              boolean isExpireDateWrong = false;
              BusinessPartner bus = supplierController.importSupplierByCRN(supplierCRN);
              for (ESCM_Certificates cr : bus.getESCMCertificatesList()) {
                if (cr.getCertificateName().getId().equals("C8E0B06D46C943A6B7DFA292AE1A4C6D")) {
                  if (cr.getRegistryExpiryDate() == null) {
                    isExpireDateWrong = true;
                  }
                }
                crNoList.add(cr.getCertificateNumber());

              }

              certificateNumberMap = supplierController.isCertificateNumberExists(crNoList,
                  bus.getId());

              if (certificateNumberMap.size() > 0) {
                isInvalidCertificate = true;
                for (String bp : certificateNumberMap.keySet()) {
                  certificateErrorMsg.append("  ");
                  certificateErrorMsg.append(OBMessageUtils.messageBD("Escm_duplicate_Cr")
                      .replace("%", bp).replace("$", certificateNumberMap.get(bp)));
                }
              }

              if (isExpireDateWrong) {
                jsonResponse.put("result", "Warning");
                jsonResponse.put("message",
                    Resource.getProperty("scm.import.supplier.incorrectExpireDate", lang));
              } else {
                if (isInvalidCertificate) {
                  jsonResponse.put("result", "Warning");
                  jsonResponse.put("message", certificateErrorMsg.toString());
                } else {
                  jsonResponse.put("result", "Success");
                  jsonResponse.put("message",
                      Resource.getProperty("scm.import.supplier.sucess", lang));
                }
              }
            } catch (TabadulIntegrationException e) {
              jsonResponse.put("result", "Error");
              jsonResponse.put("message", e.getMessage());
            } catch (Exception e) {
              jsonResponse.put("result", "Error");
              jsonResponse.put("message",
                  Resource.getProperty("scm.import.supplier.generalerror", lang));
            }
          } else {
            try {
              supplierController.updateSupplier(partner);
              jsonResponse.put("result", "Success");
              jsonResponse.put("message",
                  Resource.getProperty("scm.import.supplier.alreadyexist", lang));
            } catch (TabadulIntegrationException e) {
              jsonResponse.put("result", "Error");
              jsonResponse.put("message", e.getMessage());
            }

          }
        } else {
          jsonResponse.put("result", "Error");
          jsonResponse.put("message", Resource.getProperty("scm.import.supplier.invalidcrn", lang));
        }

        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsonResponse.toString());
      }
    } catch (Exception e) {
      Log.error("Internal server error while importing supplier", e);
    }

  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }
}
