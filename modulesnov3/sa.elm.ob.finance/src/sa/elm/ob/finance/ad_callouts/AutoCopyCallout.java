package sa.elm.ob.finance.ad_callouts;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.calendar.Year;

import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EFINBudgetLines;

/**
 * @author Gopalakrishnan on 17/05/2106
 */

public class AutoCopyCallout extends BaseActionHandler {
  private static Logger log4j = Logger.getLogger(AutoCopyCallout.class);

  protected JSONObject execute(Map<String, Object> parameters, String data) {
    try {
      OBContext.setAdminMode();
      JSONObject result = new JSONObject();
      final JSONObject jsonData = new JSONObject(data);
      Connection conn = OBDal.getInstance().getConnection();
      PreparedStatement st = null;
      ResultSet rs = null;
      String AutoCopyPeriod = null;
      String periodExists = "";
      String BudgetId = "";

      if (parameters.containsKey("budgetId")) {
        BudgetId = (String) parameters.get("budgetId");
      }

      if (jsonData.has("action")) {
        String action = jsonData.getString("action");
        // Get CarryForward Flag
        if ("getCarryForward".equals(action) && !BudgetId.isEmpty()) {
          EFINBudget budget = OBDal.getInstance().get(EFINBudget.class, BudgetId);
          if (budget != null) {
            Boolean budgetType = budget.getSalesCampaign().isEfinIscarryforward();
            result.put("isCarryForward", budgetType);
            result.put("status", budget.getAlertStatus());
          } else {
            result.put("isCarryForward", false);
            result.put("status", "");
          }

        }
        // Enter Into Auto Copy of Budget
        if ("setAutoCopy".equals(action) && !BudgetId.isEmpty()) {
          EFINBudget budget = OBDal.getInstance().get(EFINBudget.class, BudgetId);
          String currenFrmPeriod = budget.getFrmperiod().getId();
          log4j.debug("currenFrmPeriod:" + currenFrmPeriod);

          Period fromPeriod = OBDal.getInstance().get(Period.class, currenFrmPeriod);
          Date fromDate = fromPeriod.getStartingDate();
          String calendarId = fromPeriod.getYear().getCalendar().getId();

          if (budget.getEFINBudgetLinesList().size() > 0) {
            result.put("Message", "LineExists");
          } else {
            String query = " select c_period.c_period_id , c_period.startdate  from c_period "
                + " left join c_year on c_year.c_year_id=c_period.c_year_id "
                + " where c_period.enddate  < to_date('"
                + new SimpleDateFormat("dd-MM-yyyy").format(fromDate)
                + "','dd-MM-yyyy') and c_year.c_calendar_id='" + calendarId + "'"
                + " order by c_period.enddate desc limit 1";

            st = conn.prepareStatement(query);
            rs = st.executeQuery();
            if (rs.next()) {
              AutoCopyPeriod = rs.getString("c_period_id");
              periodExists = "1";
            }
            if (rs != null) {
              rs.close();
            }
            if (st != null) {
              st.close();
            }
            if (!periodExists.equals("1")) {
              result.put("Message", "PreviousPeriodNotExists");
            } else {
              log4j.debug("CarryForwardPeriod:" + AutoCopyPeriod);
              Period autoCopyPeriod = OBDal.getInstance().get(Period.class, AutoCopyPeriod);
              Year AutoCopyYear = autoCopyPeriod.getYear();

              String BudgetType = budget.getSalesCampaign().getId();
              String AcctElementId = budget.getAccountElement().getId();

              if (AutoCopyYear != null) {
                OBQuery<EFINBudget> copyBudget = OBDal.getInstance().createQuery(EFINBudget.class,
                    " as e where  e.year.id='" + AutoCopyYear.getId()
                        + "' and e.alertStatus='APP' and e.salesCampaign.id='" + BudgetType + ""
                        + "' and e.accountElement.id = '" + AcctElementId + "' ");
                copyBudget.setMaxResult(1);
                if (copyBudget.list().size() > 0) {
                  EFINBudget copybudgetVO = copyBudget.list().get(0);
                  if (copybudgetVO.getEFINBudgetLinesList().size() > 0) {
                    for (EFINBudgetLines copyLines : copybudgetVO.getEFINBudgetLinesList()) {
                      EFINBudgetLines copiedLines = (EFINBudgetLines) DalUtil.copy(copyLines);

                      copiedLines.setEfinBudget(budget);
                      copiedLines.setAmount(BigDecimal.ZERO);
                      OBDal.getInstance().save(copiedLines);
                      // All columns of BudgetLines Inserted

                    }
                  }
                  OBDal.getInstance().flush();
                  OBDal.getInstance().commitAndClose();
                  result.put("Message", "Success");
                } else {
                  result.put("Message", "PreviousBudgetNotExists");
                }

              }
            }
          }

        }
      }
      return result;
    } catch (Exception e) {
      log4j.error("Exception in AutoCopyCallout :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
