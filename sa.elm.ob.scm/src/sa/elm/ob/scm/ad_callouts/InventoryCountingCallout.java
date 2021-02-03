package sa.elm.ob.scm.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;

public class InventoryCountingCallout extends SimpleCallout {

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String strOrgid = info.vars.getStringParameter("inpadOrgId");
    if (inpLastFieldChanged.equals("inpadOrgId")) {
      OBQuery<InventoryCount> header = OBDal.getInstance().createQuery(InventoryCount.class,
          "organization.id='" + strOrgid + "' order by updated desc");
      if (header != null && header.list().size() > 0) {
        header.setMaxResult(1);
        InventoryCount inv = header.list().get(0);
        info.addResult("inpemEscmWarehousekeeper", inv.getESCMWarehouseKeeper());
        info.addResult("inpemEscmInventorymgr", inv.getESCMInventoryMgr());
        info.addResult("inpemEscmMember1", inv.getEscmMember1());
        info.addResult("inpemEscmMember2", inv.getEscmMember2());
        info.addResult("inpemEscmMember3", inv.getEscmMember3());
        info.addResult("inpemEscmMember4", inv.getEscmMember4());
      }
    }
  }

}