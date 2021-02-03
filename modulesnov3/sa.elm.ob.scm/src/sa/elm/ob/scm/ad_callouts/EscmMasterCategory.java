package sa.elm.ob.scm.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.plm.ProductCategory;

public class EscmMasterCategory extends SimpleCallout {

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    if (inpLastFieldChanged.equals("inpemEscmProductCategory")) {
      String strMasterCategory = vars.getStringParameter("inpemEscmProductCategory");
      ProductCategory header = OBDal.getInstance().get(ProductCategory.class, strMasterCategory);
      if (header.getEscmCusattributeset() != null) {
        info.addResult("inpemEscmCusattributesetId", header.getEscmCusattributeset().getId());
      } else {
        info.addResult("inpemEscmCusattributesetId", null);
      }
    }
  }

}