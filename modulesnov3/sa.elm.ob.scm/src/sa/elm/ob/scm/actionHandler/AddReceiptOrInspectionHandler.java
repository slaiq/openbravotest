package sa.elm.ob.scm.actionHandler;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

import sa.elm.ob.scm.EscmAddreceipt;

public class AddReceiptOrInspectionHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(AddReceiptOrInspectionHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject json = new JSONObject();
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject encumlines = jsonparams.getJSONObject("escm_addreceiptorinsepct");
      final String inoutId = jsonRequest.getString("inpmInoutId");
      JSONArray selectedlines = encumlines.getJSONArray("_selection");
      EscmAddreceipt addreceipt = null;
      ShipmentInOut inout = null;
      if (selectedlines.length() > 0) {
        for (int a = 0; a < selectedlines.length(); a++) {

          JSONObject selectedRow = selectedlines.getJSONObject(a);
          log.debug("selectedRow:" + selectedRow);
          inout = OBDal.getInstance().get(ShipmentInOut.class, inoutId);
          if (inout.getEscmReceivingtype().equals("INS")) {
            addreceipt = OBProvider.getInstance().get(EscmAddreceipt.class);
            addreceipt.setClient(inout.getClient());
            addreceipt.setOrganization(inout.getOrganization());
            addreceipt.setCreationDate(new java.util.Date());
            addreceipt.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
            addreceipt.setUpdated(new java.util.Date());
            addreceipt.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
            addreceipt.setActive(true);
            if (inout.getEscmReceivingtype().equals("INS")) {
              addreceipt.setReceipt(OBDal.getInstance().get(ShipmentInOut.class,
                  selectedRow.getString("goodsShipment")));
            }
            addreceipt.setGoodsShipment(OBDal.getInstance().get(ShipmentInOut.class, inoutId));

            OBDal.getInstance().save(addreceipt);
            OBDal.getInstance().flush();
          } else if (inout.getEscmReceivingtype().equals("RET")) {
            OBQuery<EscmAddreceipt> insqry = OBDal.getInstance().createQuery(EscmAddreceipt.class,
                "  as e where e.receipt.id=:receiptID and e.goodsShipment.escmReceivingtype='INS' "
                    + " and e.goodsShipment.escmDocstatus='CO'");
            insqry.setNamedParameter("receiptID", selectedRow.getString("goodsShipment"));
            if (insqry.list().size() > 0) {
              for (EscmAddreceipt ins : insqry.list()) {
                OBQuery<EscmAddreceipt> delqry = OBDal.getInstance().createQuery(
                    EscmAddreceipt.class,
                    "  as e where e.inspection.id=:inspecId and e.goodsShipment.escmReceivingtype='DEL' "
                        + " and e.goodsShipment.escmDocstatus='CO'");
                delqry.setNamedParameter("inspecId", ins.getGoodsShipment().getId());
                if (delqry.list().size() > 0) {
                  for (EscmAddreceipt del : delqry.list()) {
                    addreceipt = OBProvider.getInstance().get(EscmAddreceipt.class);
                    addreceipt.setClient(inout.getClient());
                    addreceipt.setOrganization(inout.getOrganization());
                    addreceipt.setCreationDate(new java.util.Date());
                    addreceipt.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
                    addreceipt.setUpdated(new java.util.Date());
                    addreceipt.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
                    addreceipt.setActive(true);
                    addreceipt.setReceipt(OBDal.getInstance().get(ShipmentInOut.class,
                        selectedRow.getString("goodsShipment")));
                    addreceipt
                        .setGoodsShipment(OBDal.getInstance().get(ShipmentInOut.class, inoutId));
                    addreceipt.setInspection(ins.getGoodsShipment());
                    addreceipt.setDelivery(del.getGoodsShipment());
                    OBDal.getInstance().save(addreceipt);
                  }
                } else {
                  addreceipt = OBProvider.getInstance().get(EscmAddreceipt.class);
                  addreceipt.setClient(inout.getClient());
                  addreceipt.setOrganization(inout.getOrganization());
                  addreceipt.setCreationDate(new java.util.Date());
                  addreceipt.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
                  addreceipt.setUpdated(new java.util.Date());
                  addreceipt.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
                  addreceipt.setActive(true);
                  addreceipt.setReceipt(OBDal.getInstance().get(ShipmentInOut.class,
                      selectedRow.getString("goodsShipment")));
                  addreceipt
                      .setGoodsShipment(OBDal.getInstance().get(ShipmentInOut.class, inoutId));
                  addreceipt.setInspection(ins.getGoodsShipment());
                  addreceipt.setDelivery(null);
                  OBDal.getInstance().save(addreceipt);
                }
              }
              OBDal.getInstance().flush();
            } else {
              addreceipt = OBProvider.getInstance().get(EscmAddreceipt.class);
              addreceipt.setClient(inout.getClient());
              addreceipt.setOrganization(inout.getOrganization());
              addreceipt.setCreationDate(new java.util.Date());
              addreceipt.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
              addreceipt.setUpdated(new java.util.Date());
              addreceipt.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
              addreceipt.setActive(true);
              addreceipt.setReceipt(OBDal.getInstance().get(ShipmentInOut.class,
                  selectedRow.getString("goodsShipment")));
              addreceipt.setGoodsShipment(OBDal.getInstance().get(ShipmentInOut.class, inoutId));
              addreceipt.setInspection(null);
              addreceipt.setDelivery(null);
              OBDal.getInstance().save(addreceipt);
              OBDal.getInstance().flush();
            }
          } else if (inout.getEscmReceivingtype().equals("DEL")) {

            OBQuery<EscmAddreceipt> addrec = OBDal.getInstance().createQuery(EscmAddreceipt.class,
                "  as e where e.goodsShipment.escmDocstatus='CO' and  e.goodsShipment.id=:goodsShipmentID");
            addrec.setNamedParameter("goodsShipmentID", selectedRow.getString("goodsShipment"));
            log.debug("list:" + addrec.list().size());
            if (addrec.list().size() > 0) {
              for (EscmAddreceipt rec : addrec.list()) {
                addreceipt = OBProvider.getInstance().get(EscmAddreceipt.class);
                addreceipt.setClient(inout.getClient());
                addreceipt.setOrganization(inout.getOrganization());
                addreceipt.setCreationDate(new java.util.Date());
                addreceipt.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
                addreceipt.setUpdated(new java.util.Date());
                addreceipt.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
                addreceipt.setActive(true);
                addreceipt.setReceipt(rec.getReceipt());
                addreceipt.setInspection(OBDal.getInstance().get(ShipmentInOut.class,
                    selectedRow.getString("goodsShipment")));
                addreceipt.setGoodsShipment(OBDal.getInstance().get(ShipmentInOut.class, inoutId));
                OBDal.getInstance().save(addreceipt);
                OBDal.getInstance().flush();
              }
            }
          }
        }
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "success");
        successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
        json.put("message", successMessage);
        return json;
      } else {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_POAddRecIns"));
        json.put("message", successMessage);
        return json;
      }

    } catch (Exception e) {
      log.error("Exception in AddReceiptOrInspectionHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
