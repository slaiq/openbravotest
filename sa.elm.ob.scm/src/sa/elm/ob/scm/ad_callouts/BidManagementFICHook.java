package sa.elm.ob.scm.ad_callouts;

import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.application.window.FICExtension;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Tab;

import sa.elm.ob.scm.EscmBidMgmt;

public class BidManagementFICHook implements FICExtension {

  public static String TAB_ID = "31960EC365D746A180594FFB7B403ABB";

  @Override
  public void execute(String mode, Tab tab, Map<String, JSONObject> columnValues, BaseOBObject row,
      List<String> changeEventCols, List<JSONObject> calloutMessages, List<JSONObject> attachments,
      List<String> jsExcuteCode, Map<String, Object> hiddenInputs, int noteCount,
      List<String> overwrittenAuxiliaryInputs) {

    // Handles the callout for bid management callout for bid class field
    if (!"NEW".equals(mode) && tab != null && row != null && TAB_ID.equals(tab.getId())) {
      EscmBidMgmt bid = OBDal.getInstance().get(EscmBidMgmt.class, row.getId());
      if (bid != null) {
        if (bid.getBidclass() == null && "DR".equals(bid.getBidappstatus())) {
          String jscode = "if(form.view.parentView!=undefined && form.view.parentView!=null){ "
              + "  var me = form.view.parentView.viewForm; "
              + "  me.getFieldFromColumnName('Bidclass').setValue(''); }else{ "
              + "if(form.view.isShowingForm){ form.getFieldFromColumnName('Bidclass').setValue('') }else {form.view.viewGrid.processColumnValue(form.view.viewGrid.data.indexOf(form.view.viewGrid.getSelectedRecord()),'Bidclass',[])}\n"
              + "}";
          jsExcuteCode.add(jscode);
        }

      }
    }

  }

}
