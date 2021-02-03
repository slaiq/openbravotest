package sa.elm.ob.scm.ad_callouts;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.plm.Product;

/**
 * 
 * @author Gopalakrishnan on 23/03/2017
 * 
 */
public class CustodyDetailsCallout extends SimpleCallout {
  private static Logger log = Logger.getLogger(CustodyDetailsCallout.class);
  /**
   * Callout to update attribute id in Custody Request Details
   */
  private static final long serialVersionUID = 1L;

  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String strMProductID = vars.getStringParameter("inpmProductId");
    Product objProduct = OBDal.getInstance().get(Product.class, strMProductID);
    String strAttibute = "";
    try {
      log.debug("LastChanged:" + inpLastFieldChanged);
      if (inpLastFieldChanged.equals("inpmProductId")) {
        if (strMProductID != null) {
          // get custody attribute from product
          if (objProduct.getEscmCusattribute() != null) {
            strAttibute = objProduct.getEscmCusattribute().getId();
            info.addResult("inpmAttributesetId", strAttibute);
          }
        }
      }
      if (inpLastFieldChanged.equals("inpmAttributesetId")) {
        info.addResult("inpmAttributesetinstanceId", "");
      }

    } catch (Exception e) {
      log.error("Exception in CustodyDetailsCallout:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }

  }
}
