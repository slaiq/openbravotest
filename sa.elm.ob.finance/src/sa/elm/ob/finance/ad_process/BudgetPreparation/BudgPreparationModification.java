package sa.elm.ob.finance.ad_process.BudgetPreparation;

import java.io.IOException;
import java.math.BigDecimal;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;

import sa.elm.ob.finance.EfinBudgPrepLines;
import sa.elm.ob.utility.util.Utility;

public class BudgPreparationModification extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    RequestDispatcher dispatch = null;
    try {
      String action = (request.getParameter("act") == null ? "" : request.getParameter("act"));
      String strBudgPreparationId = vars.getStringParameter("inpefinBudgetPreparationId");
      log4j.debug("strBudgPreparationId:" + strBudgPreparationId);
      log4j.debug("action:" + action);
      if (action.equals("")) {
        request.setAttribute("inpBudgetPrepartationId", strBudgPreparationId);
        dispatch = request.getRequestDispatcher(
            "../web/sa.elm.ob.finance/jsp/BudgetPreparation/Modification.jsp");
      }
      if (action.equals("Modification")) {
        boolean success = false;
        BigDecimal budgetvalue = BigDecimal.ZERO;
        BigDecimal modifcation = BigDecimal.ZERO;
        String preparationId = request.getParameter("inpBudgetPrepartationId");
        String inpType = request.getParameter("inpType");
        String inpPercentage = request.getParameter("inpPercentage");
        String inpYears = request.getParameter("inpYears");

        OBQuery<EfinBudgPrepLines> lines = OBDal.getInstance().createQuery(EfinBudgPrepLines.class,
            " efinBudgetPreparation.id ='" + preparationId + "'");
        if (lines.list().size() > 0) {
          for (int i = 0; i < lines.list().size(); i++) {
            EfinBudgPrepLines line = lines.list().get(i);
            if (inpYears.equals("LB")) {
              budgetvalue = line.getLastyearamt();
              modifcation = budgetvalue
                  .multiply(new BigDecimal(inpPercentage).divide(new BigDecimal(100)));
            } else if (inpYears.equals("Avg2")) {
              budgetvalue = line.getSecondyearamt();
              modifcation = budgetvalue
                  .multiply(new BigDecimal(inpPercentage).divide(new BigDecimal(100)));
            } else if (inpYears.equals("Avg3")) {
              budgetvalue = line.getThirdyearamt();
              modifcation = budgetvalue
                  .multiply(new BigDecimal(inpPercentage).divide(new BigDecimal(100)));
            } else if (inpYears.equals("LYFA")) {
              budgetvalue = line.getLastyearfa();
              modifcation = budgetvalue
                  .multiply(new BigDecimal(inpPercentage).divide(new BigDecimal(100)));
            } else if (inpYears.equals("TYFA")) {
              budgetvalue = line.getTwoyearfa();
              modifcation = budgetvalue
                  .multiply(new BigDecimal(inpPercentage).divide(new BigDecimal(100)));
            }

            else if (inpYears.equals("THEFA")) {
              budgetvalue = line.getThreeyearfa();
              modifcation = budgetvalue
                  .multiply(new BigDecimal(inpPercentage).divide(new BigDecimal(100)));
            } else if (inpYears.equals("LYA")) {
              budgetvalue = line.getLastyearactual();
              modifcation = budgetvalue
                  .multiply(new BigDecimal(inpPercentage).divide(new BigDecimal(100)));
            }

            else if (inpYears.equals("TYAA")) {
              budgetvalue = line.getTwoyearactual();
              modifcation = budgetvalue
                  .multiply(new BigDecimal(inpPercentage).divide(new BigDecimal(100)));
            } else if (inpYears.equals("THEYAA")) {
              budgetvalue = line.getThreeyearactual();
              modifcation = budgetvalue
                  .multiply(new BigDecimal(inpPercentage).divide(new BigDecimal(100)));
            }

            if (inpType.equals("I")) {
              budgetvalue = budgetvalue.add(modifcation);
              line.setAmount(budgetvalue);
            } else {
              budgetvalue = budgetvalue.subtract(modifcation);
              line.setAmount(budgetvalue);
            }
            log4j.debug("getSecondyearamt:" + line.getAmount());
            OBDal.getInstance().save(line);
            if (!StringUtils.isEmpty(line.getId()))
              success = true;
          }
        } else {
          advisePopUpRefresh(request, response, "ERROR",
              Utility.getADMessage("OBUIAPP_Error", vars.getLanguage()),
              Utility.getADMessage("Efin_BudPrep_Add_Line", vars.getLanguage()));
        }
        if (success) {
          advisePopUpRefresh(request, response, "SUCCESS",
              Utility.getADMessage("OBUIAPP_Success", vars.getLanguage()),
              Utility.getADMessage("Efin_BudPrep_ModifySuccess", vars.getLanguage()));
        } else
          advisePopUpRefresh(request, response, "ERROR",
              Utility.getADMessage("OBUIAPP_Error", vars.getLanguage()),
              Utility.getADMessage("Efin_BudPrep_ModifyNotSuccess", vars.getLanguage()));

      }
    } catch (final Exception e) {
      dispatch = request.getRequestDispatcher("../web/jsp/PopupErrorPage.jsp");
      log4j.error("Exception in Modify the Budget Preparation : ", e);
    } finally {
      try {
        if (dispatch != null) {
          response.setContentType("text/html; charset=UTF-8");
          response.setCharacterEncoding("UTF-8");
          dispatch.include(request, response);
        }
      } catch (final Exception e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        log4j.error("Exception in Modify the Budget Preparation : ", e);
      }
    }
  }
}