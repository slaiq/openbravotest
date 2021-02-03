package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;

import sa.elm.ob.utility.util.Utility;

/*
 * Inserting records in retuen to vendor tab and updating quantity in intial receipt.
 */
public class PartialCountingHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(POReceiptReturnHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject json = new JSONObject();
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject lines = jsonparams.getJSONObject("Addline2");
      JSONArray selectedlines = lines.getJSONArray("_selection");
      String warehouse = jsonRequest.getString("inpmWarehouseId");
      InventoryCountLine Invline = null;
      Locator locator = null;
      String firstBin = "";
      String secondBin = "";
      String defaultBinId = "";
      Connection conn = OBDal.getInstance().getConnection();
      long line = 0;
      int count = 0;
      final String currentHdId = jsonRequest.getString("inpmInventoryId");

      // if same product and storagebin combination is selected then throw error. Note: (if no bin
      // then it will take default bin for non stocked product)
      if (selectedlines.length() == 0) {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_POAddRecIns"));
        json.put("message", successMessage);
        return json;
      }
      for (int a = 0; a < selectedlines.length(); a++) {
        count = 0;
        for (int b = 0; b < selectedlines.length(); b++) {
          JSONObject firstrow = selectedlines.getJSONObject(a);
          JSONObject secondRow = selectedlines.getJSONObject(b);
          if (firstrow.getString("storageBin") == "null") {
            firstBin = Utility.GetDefaultBin(warehouse);
            if (firstBin.equals("")) {
              OBDal.getInstance().rollbackAndClose();
              JSONObject errorMessage = new JSONObject();
              errorMessage.put("severity", "error");
              errorMessage.put("text", OBMessageUtils.messageBD("Escm_No_DefaultBin"));
              json.put("message", errorMessage);
              return json;
            }
          } else
            firstBin = firstrow.getString("storageBin");
          if (secondRow.getString("storageBin") == "null") {
            secondBin = Utility.GetDefaultBin(warehouse);
            if (secondBin.equals("")) {
              OBDal.getInstance().rollbackAndClose();
              JSONObject errorMessage = new JSONObject();
              errorMessage.put("severity", "error");
              errorMessage.put("text", OBMessageUtils.messageBD("Escm_No_DefaultBin"));
              json.put("message", errorMessage);
              return json;
            }
          } else
            secondBin = secondRow.getString("storageBin");
          if (firstrow.getString("product").equals(secondRow.getString("product"))
              && firstBin.equals(secondBin)) {
            count = count + 1;
            if (count > 1) {
              OBDal.getInstance().rollbackAndClose();
              JSONObject errorMessage = new JSONObject();
              errorMessage.put("severity", "error");
              errorMessage.put("text",
                  OBMessageUtils.messageBD("Escm_Inventoyline_DuplicateProduct"));
              json.put("message", errorMessage);
              return json;
            }
          }

        }
      }

      // while adding lines seperately we need to check product is already present or not.
      for (int a = 0; a < selectedlines.length(); a++) {
        JSONObject selectedRow = selectedlines.getJSONObject(a);
        if (selectedRow.getString("storageBin") == "null") {
          defaultBinId = Utility.GetDefaultBin(warehouse);
          if (defaultBinId.equals("")) {
            OBDal.getInstance().rollbackAndClose();
            JSONObject errorMessage = new JSONObject();
            errorMessage.put("severity", "error");
            errorMessage.put("text", OBMessageUtils.messageBD("Escm_No_DefaultBin"));
            json.put("message", errorMessage);
            return json;
          }
        } else {
          defaultBinId = selectedRow.getString("storageBin");
        }
        OBQuery<InventoryCountLine> checkline = OBDal.getInstance().createQuery(
            InventoryCountLine.class,
            "product.id = '" + selectedRow.getString("product") + "' and storageBin.id = '"
                + defaultBinId + "' and physInventory.id='" + currentHdId + "'");
        if (checkline.list() != null && checkline.list().size() > 0) {
          OBDal.getInstance().rollbackAndClose();
          JSONObject errorMessage = new JSONObject();
          errorMessage.put("severity", "error");
          errorMessage.put("text", OBMessageUtils.messageBD("Escm_Inventoyline_DuplicateProduct"));
          json.put("message", errorMessage);
          return json;
        }
      }

      for (int a = 0; a < selectedlines.length(); a++) {
        JSONObject selectedRow = selectedlines.getJSONObject(a);
        log.debug("selectedRow:" + selectedRow);
        Product product = OBDal.getInstance().get(Product.class, selectedRow.getString("product"));
        UOM uom = OBDal.getInstance().get(UOM.class, selectedRow.getString("uOM"));
        if (selectedRow.getString("storageBin") == null
            || StringUtils.isEmpty(selectedRow.getString("storageBin"))
            || StringUtils.isBlank(selectedRow.getString("storageBin"))
            || selectedRow.getString("storageBin").equals("null")) {
          defaultBinId = Utility.GetDefaultBin(warehouse);
          if (defaultBinId.equals("")) {
            OBDal.getInstance().rollbackAndClose();
            JSONObject errorMessage = new JSONObject();
            errorMessage.put("severity", "error");
            errorMessage.put("text", OBMessageUtils.messageBD("Escm_No_DefaultBin"));
            json.put("message", errorMessage);
            return json;
          } else
            locator = OBDal.getInstance().get(Locator.class, defaultBinId);
        } else {
          locator = OBDal.getInstance().get(Locator.class, selectedRow.getString("storageBin"));
        }
        InventoryCount header = OBDal.getInstance().get(InventoryCount.class, currentHdId);
        ps = conn.prepareStatement(
            " select coalesce(max(line),0)+10 as lineno from m_inventoryline where m_inventory_id=?");
        ps.setString(1, currentHdId);
        rs = ps.executeQuery();
        if (rs.next()) {
          line = rs.getLong("lineno");
        }
        BigDecimal qty = new BigDecimal(selectedRow.getString("quantityOnHand"));
        Invline = OBProvider.getInstance().get(InventoryCountLine.class);
        Invline.setOrganization(header.getOrganization());
        Invline.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
        Invline.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
        Invline.setPhysInventory(header);
        Invline.setLineNo(line);
        Invline.setProduct(product);
        Invline.setEscmItemdesc(selectedRow.getString("product$name"));
        Invline.setUOM(uom);
        Invline.setStorageBin(locator);
        Invline.setBookQuantity(qty);
        Invline.setQuantityCount(BigDecimal.ZERO);
        Invline.setEscmProductStockV(selectedRow.getString("id"));
        OBDal.getInstance().save(Invline);
        OBDal.getInstance().flush();
      }
      JSONObject successMessage = new JSONObject();
      successMessage.put("severity", "success");
      successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
      json.put("message", successMessage);
      return json;

    } catch (Exception e) {
      log.error("Exception in PartialCountingHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (ps != null) {
          ps.close();
        }
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();
    }
  }
}
