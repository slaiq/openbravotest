package sa.elm.ob.scm.ad_callouts;

import java.math.BigDecimal;
import java.util.Date;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.ad_callouts.BudgetAdjustmentCallout;
import sa.elm.ob.utility.util.Utility;

/**
 * Callout for Bid Management Header
 * 
 * @author qualian
 *
 */

@SuppressWarnings("serial")
public class EscmBidManagementCallout extends SimpleCallout {
  private static final Logger log = LoggerFactory.getLogger(EscmBidManagementCallout.class);
  private static final String strBidWindowId = "E509200618424FD099BAB1D4B34F96B8";

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = info.vars;
    String inprfpprice = vars.getStringParameter("inprfpprice").toString();
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    // String inpbidtypeclass = vars.getStringParameter("inpbidtypeclass");
    String inpbidtype = vars.getStringParameter("inpbidtype");
    // String strOrgID = vars.getStringParameter("inpadOrgId");
    String inpadClientId = vars.getStringParameter("inpadClientId");
    String budgInitialId = null;
    String inpadOrgId = vars.getStringParameter("inpadOrgId");
    // String inpdocumentaddress = vars.getStringParameter("inpdocumentaddress");
    String inpBudgetAmount = vars.getStringParameter("inpapprovedbudget");
    Organization org = Utility.getObject(Organization.class, inpadOrgId);
    String value = "";
    try {

      if (inpLastFieldChanged.equals("inpapprovedbudget")) {
        if (StringUtils.isEmpty(inpBudgetAmount)) {
          info.addResult("inpapprovedbudget", BigDecimal.ZERO);
        }
      }

      if (inpLastFieldChanged.equals("inpadOrgId")) {
        Date endDate = new Date();
        // getting budget initial id based on transaction date
        budgInitialId = BudgetAdjustmentCallout.getBudgetDefinitionForStartDate(endDate,
            inpadClientId, strBidWindowId);
        if (budgInitialId != null)
          info.addResult("inpemEfinBudgetinitialId", budgInitialId);
        else
          info.addResult("inpemEfinBudgetinitialId", null);
      }
      if (inpLastFieldChanged.equals("inprfpprice")) {
        if (inprfpprice.length() > 9) {
          value = inprfpprice.replace(",", "").substring(0, 9);
          info.addResult("inprfpprice", value);
        }
      }
      // based one selected bidtype, value will be save in "bidtypeclass" field
      if (inpLastFieldChanged.equals("inpbidtype")) {
        info.addResult("inpbidtypeclass", inpbidtype);
      }

      if (inpLastFieldChanged.equals("inpadOrgId")) {
        info.addResult("inpdocumentaddress", org.getEhcmEscmLoc().getId());
        info.addResult("inpsubmissionadd", org.getEhcmEscmLoc().getId());
        info.addResult("inpenvelopeadd", org.getEhcmEscmLoc().getId());

        // Dont select any thing in bid class as default
        String jscode = " if(form.view.isShowingForm){ form.getFieldFromColumnName('Bidclass').setValue('') }else {form.view.viewGrid.processColumnValue(form.view.viewGrid.data.indexOf(form.view.viewGrid.getSelectedRecord()),'Bidclass',[])}";
        info.addResult("JSEXECUTE", jscode);
      }

    } catch (Exception e) {
      log.error("Exception in EscmBidManagementCallout  :", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }

  }
}
