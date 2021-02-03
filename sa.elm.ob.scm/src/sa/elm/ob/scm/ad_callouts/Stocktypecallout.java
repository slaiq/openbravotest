package sa.elm.ob.scm.ad_callouts;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.plm.ProductCategory;

/**
 * 
 * 
 */
public class Stocktypecallout extends SimpleCallout {
  private static Logger log = Logger.getLogger(CustodyDetailsCallout.class);
  /**
   */
  private static final long serialVersionUID = 1L;

  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpmProductCategoryId = vars.getStringParameter("inpmProductCategoryId");
    try {
      if (inpLastFieldChanged.equals("inpemEscmStockType")) {

      }
      if (inpLastFieldChanged.equals("inpmProductCategoryId")) {
        ProductCategory prdcat = OBDal.getInstance().get(ProductCategory.class,
            inpmProductCategoryId);
        if (prdcat.getEscmCusattributeset() != null)
          info.addResult("inpemEscmCusattribute", prdcat.getEscmCusattributeset().getId());
      }
    } catch (Exception e) {
      log.error("Exception in Stocktypecallout:", e);
    }

  }
}
