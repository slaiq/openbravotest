package sa.elm.ob.finance.ad_callouts.dao;

import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.financialmgmt.calendar.Period;

public class BudgetDefinitionCalloutDAO {

  /**
   * 
   * 
   * @author sathish kumar.p created on 04-10-2017
   *
   */

  public static Period getToPeriod(String yearId) {

    // get the last period for the corresponding year
    final OBQuery<Period> periodQry = OBDal.getInstance().createQuery(Period.class,
        "year.id = :yearID order by periodNo desc ");
    periodQry.setNamedParameter("yearID", yearId);
    if (periodQry.list().size() > 0) {
      Period period = periodQry.list().get(0);
      return period;
    }

    return null;
  }
}
