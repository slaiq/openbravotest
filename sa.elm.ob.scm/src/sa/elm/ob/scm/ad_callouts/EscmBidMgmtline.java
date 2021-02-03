package sa.elm.ob.scm.ad_callouts;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author qualian
 *
 */
public class EscmBidMgmtline extends SimpleCallout {
  private static Logger log = Logger.getLogger(EscmBidMgmtline.class);
  /**
   * Callout to update the line Details in Bid Management
   */
  private static final long serialVersionUID = 1L;

  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String strMProductID = vars.getStringParameter("inpmProductId");
    // String isSummary = vars.getStringParameter("inpissummarylevel");
    String uniqueCode = vars.getStringParameter("inpcValidcombinationId");
    try {
      log.debug("LastChanged:" + inpLastFieldChanged);
      if (inpLastFieldChanged.equals("inpmProductId")) {
        if (strMProductID != null) {
          Product product = Utility.getObject(Product.class, strMProductID);
          if (product != null) {
            info.addResult("inpcUomId", product.getUOM().getId());
            info.addResult("inpdescription", product.getName());
            info.addResult("inpmProductCategoryId", product.getProductCategory().getId());
          } else {
            info.addResult("inpcUomId", "");
            info.addResult("inpdescription", "");
            info.addResult("inpmProductCategoryId", "");
          }
        } else {
          info.addResult("inpcUomId", "");
          info.addResult("inpdescription", "");
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('M_Product_Category_ID').setValue('')");
        }
      }

      if (inpLastFieldChanged.equals("inpcValidcombinationId")) {
        if (uniqueCode.equals("")) {
          info.addResult("inpuniquecodename", "");
        } else {
          AccountingCombination dimention = OBDal.getInstance().get(AccountingCombination.class,
              uniqueCode);
          info.addResult("inpuniquecodename", dimention.getEfinUniquecodename());
          log.debug("inpLastFieldChanged:" + dimention.getEfinUniquecodename());
        }
      }

    } catch (Exception e) {
      log.debug("Exception in EscmBidMgmtline item callout:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }

  }
}
