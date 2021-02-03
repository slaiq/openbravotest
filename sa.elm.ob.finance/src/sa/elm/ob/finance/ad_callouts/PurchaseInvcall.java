package sa.elm.ob.finance.ad_callouts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.invoice.Invoice;

import sa.elm.ob.finance.dao.UniqueCodeGen;
import sa.elm.ob.utility.util.Utility;

public class PurchaseInvcall extends SimpleCallout {

  /**
   * Callout to update the uniqueCode Information in Purchase invoice Window
   */
  private static final long serialVersionUID = 1L;
  final private static Logger log = Logger.getLogger(PurchaseInvcall.class);

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    try {
      VariablesSecureApp vars = info.vars;
      log.debug("inside callout non expense");
      String inpProject = vars.getStringParameter("inpcProjectId");
      String inpUser1 = vars.getStringParameter("inpuser1Id");
      String inpUser2 = vars.getStringParameter("inpuser2Id");
      String inpActivity = vars.getStringParameter("inpemEfinCActivityId");
      String inpAccount = vars.getStringParameter("inpemEfinCElementvalueId");
      String inpBudgetType = vars.getStringParameter("inpemEfinCCampaignId");
      String inpDepartment = vars.getStringParameter("inpemEfinCSalesregionId");
      String inpInv = vars.getStringParameter("inpcInvoiceId");
      String inpOrgId = vars.getStringParameter("inpadOrgId");
      String inpEntity = vars.getStringParameter("inpcBpartnerId");
      String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
      Connection conn = OBDal.getInstance().getConnection();
      UniqueCodeGen dao = new UniqueCodeGen(conn);
      PreparedStatement ps = null;
      Date ActDate = null;
      ResultSet rs = null;
      if (!StringUtils.isEmpty(inpInv)) {
        Invoice manEncumbarance = OBDal.getInstance().get(Invoice.class, inpInv);
        ActDate = manEncumbarance.getAccountingDate();
      }

      String uniquecode = dao.getUniqueCode(inpOrgId, inpDepartment, inpAccount, inpBudgetType,
          inpProject, inpActivity, inpUser1, inpUser2, inpEntity);
      // info.addResult("inpemEfinUniquecode", uniquecode);
      if (uniquecode != null && uniquecode.length() > 0) {
        if (!StringUtils.isEmpty(vars.getStringParameter("inpemEfinCElementvalueId")))
          info.addResult("inpemEfinUniquecode", uniquecode);
        else if (StringUtils.isEmpty(vars.getStringParameter("inpemEfinCElementvalueId")))
          info.addResult("inpemEfinUniquecode", uniquecode);
        try {
          OBContext.setAdminMode();
          ps = conn.prepareStatement(
              "select * from efin_buget_process ( ?,?,?,?,?,?,?,?,to_date(?,'dd-MM-yyyy'), ?)");

          // returns p_uniquecode,p_amount,p_budgetlines_id
          ps.setString(1, vars.getStringParameter("inpadOrgId"));
          if (!StringUtils.isEmpty(vars.getStringParameter("inpemEfinCElementvalueId")))
            ps.setString(2, vars.getStringParameter("inpemEfinCElementvalueId"));
          // else if(StringUtils.isEmpty(vars.getStringParameter("inpcElementvalueId")) &&
          // (!StringUtils.isEmpty(inpcValidCombinationID)))
          // ps.setString(2, inpAccount);
          ps.setString(3, vars.getStringParameter("inpcProjectId"));
          ps.setString(4, vars.getStringParameter("inpemEfinCSalesregionId"));
          ps.setString(5, vars.getStringParameter("inpemEfinCCampaignId"));
          ps.setString(6, vars.getStringParameter("inpemEfinCActivityId"));
          ps.setString(7, vars.getStringParameter("inpuser1Id"));
          ps.setString(8, vars.getStringParameter("inpuser2Id"));
          ps.setString(9, Utility.formatDate(ActDate));
          ps.setString(10, OBContext.getOBContext().getCurrentClient().getId());
          log.debug("result:" + ps.toString());
          rs = ps.executeQuery();
          if (rs.next()) {

            String amount = rs.getString("p_amount");
            log.debug("amount:" + amount);
            // log.debug("budgetLines:" + rs.getString("p_budgetlines_id"));
            if (!StringUtils.isEmpty(vars.getStringParameter("inpemEfinCElementvalueId"))) {
              info.addResult("inpemEfinFundsAvailable", amount);
              // info.addResult("inpefinBudgetlinesId", rs.getString("p_budgetlines_id"));

            } else if (StringUtils.isEmpty(vars.getStringParameter("inpemEfinAccountId"))) {
              info.addResult("inpemEfinFundsAvailable", amount);
            }

          } else
            info.addResult("inpemEfinFundsAvailable", "0.00");
          OBDal.getInstance().flush();

        } catch (SQLException e) {
          log.debug("Exception while handling PurchaseInvcall callout", e);
        } finally {
          OBContext.restorePreviousMode();
        }
      }
      // Making the secondaryBeneficiary as null if the payment beneficiary is updated to null
      if (inpLastFieldChanged.equals("inpcBpartnerId")) {
        if (StringUtils.isEmpty(inpEntity))
          info.addResult("inpemEfinBeneficiary2Id", "");
      }
    } catch (Exception e) {
      log4j.error("Exception in PurchaseInvcall: " + e);
    }
  }
}
