package sa.elm.ob.finance.ad_callouts;

import java.sql.Connection;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

import sa.elm.ob.finance.dao.UniqueCodeGen;

public class NonexpenseCallout extends SimpleCallout {

  /**
   * Callout to update the uniqueCode Information in NonExpense account Window
   */
  private static final long serialVersionUID = 1L;
  final private static Logger log = Logger.getLogger(NonexpenseCallout.class);

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    try {
      VariablesSecureApp vars = info.vars;
      log.debug("inside callout non expense");
      String inpProject = vars.getStringParameter("inpcProjectId");
      String inpUser1 = vars.getStringParameter("inpuser1Id");
      String inpUser2 = vars.getStringParameter("inpuser2Id");
      String inpActivity = vars.getStringParameter("inpcActivityId");
      String inpAccount = vars.getStringParameter("inpcElementvalueId");
      String inpBudgetType = vars.getStringParameter("inpcCampaignId");
      String inpDepartment = vars.getStringParameter("inpcSalesregionId");
      String inpOrgId = vars.getStringParameter("inpadOrgId");
      String inpEntity = vars.getStringParameter("inpcBpartnerId");

      Connection conn = OBDal.getInstance().getConnection();
      UniqueCodeGen dao = new UniqueCodeGen(conn);

      String uniquecode = dao.getUniqueCode(inpOrgId, inpDepartment, inpAccount, inpBudgetType,
          inpProject, inpActivity, inpUser1, inpUser2, inpEntity);
      info.addResult("inpuniquecode", uniquecode);
    } catch (Exception e) {
      log4j.error("Exception in NonexpenseCallout: " + e);
    }

  }
}
