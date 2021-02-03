package sa.elm.ob.finance.ad_process;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.calendar.Year;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;
import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EFINBudgetLines;

/**
 * @author Gopalakrishnan on 21/05/2106
 */

public class CarryForward extends DalBaseProcess {

	/**
	 * Carry Forward process on Budget Window
	 */
	private static final Logger log = LoggerFactory.getLogger(CarryForward.class);

	@Override
	public void doExecute(ProcessBundle bundle) throws Exception {
		// TODO Auto-generated method stub

		HttpServletRequest request = RequestContext.get().getRequest();
		VariablesSecureApp vars = new VariablesSecureApp(request);
		Connection conn = OBDal.getInstance().getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		String CarryForwardPeriod = null;
		String periodExists = "";
		// Create carry Forward
		log.debug("entering into CarryForward");
		try {
			OBContext.setAdminMode();
			String budgetId = (String) bundle.getParams().get("Efin_Budget_ID");
			log.debug("budgetId:" + budgetId);
			EFINBudget budget = OBDal.getInstance().get(EFINBudget.class, budgetId);
			String currenFrmPeriod = budget.getFrmperiod().getId();
			log.debug("currenFrmPeriod:" + currenFrmPeriod);

			Period fromPeriod = OBDal.getInstance().get(Period.class, currenFrmPeriod);
			Date fromDate = fromPeriod.getStartingDate();
			String calendarId = fromPeriod.getYear().getCalendar().getId();
			//Carry Forward
			if(budget.getEFINBudgetLinesList().size() > 0) {

				OBDal.getInstance().rollbackAndClose();
				OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_LinesExists@");
				bundle.setResult(result);
				return;
			}
			else {
				String query = " select c_period.c_period_id , c_period.startdate  from c_period " + " left join c_year on c_year.c_year_id=c_period.c_year_id " + " where c_period.enddate  < to_date('" + new SimpleDateFormat("dd-MM-yyyy").format(fromDate)
						+ "','dd-MM-yyyy') and c_year.c_calendar_id='" + calendarId + "'" + " order by c_period.enddate desc limit 1";
				ps = conn.prepareStatement(query);
				rs = ps.executeQuery();
				if(rs.next()) {
					CarryForwardPeriod = rs.getString("c_period_id");
					periodExists = "1";
				}
				if(!periodExists.equals("1")) {
					OBDal.getInstance().rollbackAndClose();
					OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_PeriodsNotExists@");
					bundle.setResult(result);
					return;
				}
				else {
					log.debug("CarryForwardPeriod:" + CarryForwardPeriod);
					Period autoCopyPeriod = OBDal.getInstance().get(Period.class, CarryForwardPeriod);
					Year AutoCopyYear = autoCopyPeriod.getYear();
					log.debug("AutoCopyYear:" + AutoCopyYear.getFiscalYear());
					String BudgetType = budget.getSalesCampaign().getId();
					String accountElement = budget.getAccountElement().getId();
					if(AutoCopyYear != null) {
						OBQuery<EFINBudget> copyBudget = OBDal.getInstance().createQuery(EFINBudget.class, " as e where  e.year.id='" + AutoCopyYear.getId() + "' and e.alertStatus='APP' and e.salesCampaign.id='" + BudgetType + "' and e.accountElement.id='"+accountElement+"'");
						copyBudget.setMaxResult(1);
						if(copyBudget.list().size() > 0) {
							EFINBudget copybudgetVO = copyBudget.list().get(0);
							if(copybudgetVO.getEFINBudgetLinesList().size() > 0) {
								for (EFINBudgetLines copyLines : copybudgetVO.getEFINBudgetLinesList()) {
									EFINBudgetLines copiedLines = (EFINBudgetLines) DalUtil.copy(copyLines,false);
									copiedLines.setEfinBudget(budget);
									copiedLines.setAmount(copyLines.getFundsAvailable());
									copiedLines.setCurrentBudget(copyLines.getFundsAvailable());
									copiedLines.setEncumbrance(BigDecimal.ZERO);
									copiedLines.setIncreaseAmt(BigDecimal.ZERO);
									copiedLines.setDecreaseAmt(BigDecimal.ZERO);
									copiedLines.setAmountSpent(BigDecimal.ZERO);
									copiedLines.setFundsAvailable(copyLines.getFundsAvailable());
									OBDal.getInstance().save(copiedLines);
									//All columns of BudgetLines Inserted
								}
							}
							OBDal.getInstance().flush();
							OBDal.getInstance().commitAndClose();
							OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_AutoCopied_Success@");
							bundle.setResult(result);
						}
						else {
							OBDal.getInstance().rollbackAndClose();
							OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_BudgetNotExists@");
							bundle.setResult(result);
							return;
						}

					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			Throwable t = DbUtility.getUnderlyingSQLException(e);
			final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars, vars.getLanguage(), t.getMessage());
			bundle.setResult(error);
		}
		finally {
			OBContext.restorePreviousMode();
		}

	}

}
