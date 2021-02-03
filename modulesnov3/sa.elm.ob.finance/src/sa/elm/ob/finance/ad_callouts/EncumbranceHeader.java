package sa.elm.ob.finance.ad_callouts;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.Efin_UserManager;
import sa.elm.ob.finance.event.dao.EncumbranceEventDao;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Gopalakrishnan on 04/06/2106
 */

public class EncumbranceHeader extends SimpleCallout {

  /**
   * Callout to update the fields Information in Manual Encumbrance Window
   */
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(EncumbranceHeader.class);
  private static final String windowId = "87CD157057C64D66A4A4BE4CD248116B";

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    final String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    final String encumType = vars.getStringParameter("inpencumType");
    String inpdateacct = vars.getStringParameter("inpdateacct"), budgInitialId = null;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String inpadClientId = vars.getStringParameter("inpadClientId");

    try {

      if (inpLastFieldChanged.equals("inpadOrgId") || (inpLastFieldChanged.equals("inpdateacct"))) {
        String dateacct = UtilityDAO.convertToGregorian(inpdateacct);
        Date endDate = dateFormat.parse(dateacct);
        LOG.debug("endDate:" + dateFormat.format(endDate));
        // getting budget initial id based on transaction date
        budgInitialId = BudgetAdjustmentCallout.getBudgetDefinitionForStartDate(endDate,
            inpadClientId, windowId);
        if (budgInitialId != null)
          info.addResult("inpefinBudgetintId", budgInitialId);
        else
          info.addResult("inpefinBudgetintId", null);

        if (inpLastFieldChanged.equals("inpdateacct")) {
          info.addResult("inptrxdate", vars.getStringParameter("inpdateacct"));
        }
      }

      if (inpLastFieldChanged.equals("inpencumType")) {
        if (encumType.equals("PRE") || encumType.equals("POE") || encumType.equals("PAE")
            || encumType.equals("AEE") || encumType.equals("BE")) {
          info.addResult("inpencumStage", vars.getStringParameter("inpencumType"));
        } else {
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('Encum_Stage').setValue('')");
        }
        info.addResult("inpencumTypeText", encumType);
      }

      // set default value as usermanager costcenter for requesting department.
      if (inpLastFieldChanged.equals("inpadOrgId")) {
        List<Efin_UserManager> usermanager = EncumbranceEventDao
            .getCostCenter(OBContext.getOBContext().getUser().getId());

        if (usermanager != null && usermanager.size() > 0) {
          info.addResult("inpcSalesregionId", usermanager.get(0).getDepartment().getId());
        }
      }

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error whie getting encum stage encumbrance callout:", e);
      }
    }
  }
}
