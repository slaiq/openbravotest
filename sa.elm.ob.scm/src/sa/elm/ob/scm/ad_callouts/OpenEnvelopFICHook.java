package sa.elm.ob.scm.ad_callouts;

import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.application.window.FICExtension;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Tab;

import sa.elm.ob.scm.Escmopenenvcommitee;

public class OpenEnvelopFICHook implements FICExtension {

  public static String TAB_ID = "8095B818800446D795B8ADFEDE104733";

  @Override
  public void execute(String mode, Tab tab, Map<String, JSONObject> columnValues, BaseOBObject row,
      List<String> changeEventCols, List<JSONObject> calloutMessages, List<JSONObject> attachments,
      List<String> jsExcuteCode, Map<String, Object> hiddenInputs, int noteCount,
      List<String> overwrittenAuxiliaryInputs) {

    // Handles the callout for open envelop callout for contract type field
    if (!"NEW".equals(mode) && tab != null && row != null && TAB_ID.equals(tab.getId())
        && row.getId() != null) {
      Escmopenenvcommitee openEnvelopObj = OBDal.getInstance().get(Escmopenenvcommitee.class,
          row.getId());
      if (openEnvelopObj != null) {
        if ("DR".equals(openEnvelopObj.getAlertStatus()) && openEnvelopObj.getBidNo() != null
            && openEnvelopObj.getContractType() == null
            && openEnvelopObj.getBidNo().getContractType() == null) {
          String jscode = " if(form.view.isShowingForm){ form.getFieldFromColumnName('Contract_Type').setValue('') }else {form.view.viewGrid.processColumnValue(form.view.viewGrid.data.indexOf(form.view.viewGrid.getSelectedRecord()),'Contract_Type',[])}";
          jsExcuteCode.add(jscode);
        }

      }
    }

  }

}
