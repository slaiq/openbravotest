package sa.elm.ob.finance.ad_callouts;

import java.util.List;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;

public class FinancialAccountCallout extends SimpleCallout {

  /**
   * Callout to update the currency based on financial account currency
   */
  private static final long serialVersionUID = 1L;
  private static final Logger log = Logger.getLogger(FinancialAccountCallout.class);

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;

    String inpOrg = vars.getStringParameter("inpadOrgId");
    String inpClientId = vars.getStringParameter("inpadClientId");
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    Organization organization = null;
    Currency currency = null;
    log.debug("org:" + inpOrg);
    log.debug("inpLastFieldChanged:" + inpLastFieldChanged);
    if (inpLastFieldChanged.equals("inpadOrgId")) {

      organization = OBDal.getInstance().get(Organization.class, inpOrg);
      log.debug("getName:" + organization.getName());
      if (organization.getGeneralLedger() != null) {
        currency = organization.getGeneralLedger().getCurrency();
        log.debug("currency:" + currency.getId());
        info.addResult("inpcCurrencyId", currency.getId());
      } else {

        // get parent organization list
        String[] orgIds = null;
        SQLQuery query = OBDal.getInstance().getSession()
            .createSQLQuery("select eut_parent_org ('" + inpOrg + "','" + inpClientId + "')");
        @SuppressWarnings("unchecked")
        List<String> list = query.list();

        orgIds = list.get(0).split(",");

        for (int i = 0; i < orgIds.length; i++) {
          organization = OBDal.getInstance().get(Organization.class, orgIds[i].replace("'", ""));
          log.debug("organization:" + organization.getName());
          if (organization.getGeneralLedger() != null) {
            currency = organization.getGeneralLedger().getCurrency();
            log.debug("getGeneralLedger123:" + organization.getGeneralLedger());
            if (currency.getId() != null) {
              log.debug("currency:" + currency.getId());
              info.addResult("inpcCurrencyId", currency.getId());
            }
          }
        }

      }

    }
  }
}
