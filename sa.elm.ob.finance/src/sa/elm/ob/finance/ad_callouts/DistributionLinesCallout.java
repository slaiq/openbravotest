package sa.elm.ob.finance.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

public class DistributionLinesCallout extends SimpleCallout {

  /**
   * 
   * @author poongodi on 14/12/2017
   *
   */
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpcValidCombinationID = vars.getStringParameter("inpcValidcombinationId");
    String inpexpUniquecode = vars.getStringParameter("inpexpenseUniquecode");
    if (inpLastFieldChanged.equals("inpcValidcombinationId")) {
      AccountingCombination combination = OBDal.getInstance().get(AccountingCombination.class,
          inpcValidCombinationID);
      info.addResult("inpadOrgId", combination.getOrganization().getId());
      if (combination.getAccount() != null)
        info.addResult("inpcElementvalueId", combination.getAccount().getId());
      if (combination.getProject() != null)
        info.addResult("inpcProjectId", combination.getProject().getId());
      if (combination.getBusinessPartner() != null)
        info.addResult("inpcBpartnerId", combination.getBusinessPartner().getId());
      if (combination.getActivity() != null)
        info.addResult("inpcActivityId", combination.getActivity().getId());
      if (combination.getStDimension() != null)
        info.addResult("inpuser1Id", combination.getStDimension().getId());
      if (combination.getNdDimension() != null)
        info.addResult("inpuser2Id", combination.getNdDimension().getId());
      if (combination.getSalesRegion() != null)
        info.addResult("inpcSalesregionId", combination.getSalesRegion().getId());
      if (combination.getSalesCampaign() != null)
        info.addResult("inpcCampaignId", combination.getSalesCampaign().getId());
      if (combination.getEfinUniquecodename() != null) {
        info.addResult("inpadjUniquecodename", combination.getEfinUniquecodename());
      }
    }
    if (inpLastFieldChanged.equals("inpexpenseUniquecode")) {
      AccountingCombination expcombination = OBDal.getInstance().get(AccountingCombination.class,
          inpexpUniquecode);
      if (inpexpUniquecode != null && !inpexpUniquecode.equals("")) {
        if (expcombination.getEfinUniquecodename() != null) {
          info.addResult("inpexpUniquecodename", expcombination.getEfinUniquecodename());
        }
      } else {
        info.addResult("inpexpUniquecodename", null);
      }

    }

  }
}
