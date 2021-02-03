package sa.elm.ob.scm.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMDefLookupsTypeLn;

/**
 * @author Mouli.K
 */

public class ProductContractCategoryCallout extends SimpleCallout {

  /**
   * Callout to update the Receive Type from the Reference Lookup based on the Contract Category
   * 
   */
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(ProductContractCategoryCallout.class);

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    final String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    final String contractCategoryNameId = vars.getStringParameter("inpcontractcategory");

    ESCMDefLookupsTypeLn refLookupLine = OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
        contractCategoryNameId);

    String receiveType = "";
    if (refLookupLine.getReceiveType() != null) {
      receiveType = refLookupLine.getReceiveType().getId();
    }

    try {

      // while changing the contract category name, respective receive type is updated.
      if (inpLastFieldChanged.equals("inpcontractcategory")) {
        info.addResult("inpreceivetype", receiveType);
      }

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error whie getting Product Contract Category Callout:", e);
      }
    }
  }
}
