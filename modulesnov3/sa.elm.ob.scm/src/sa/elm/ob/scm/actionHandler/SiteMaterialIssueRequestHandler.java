package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;

import sa.elm.ob.scm.EscmInitialReceipt;
import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.MaterialIssueRequestHistory;
import sa.elm.ob.scm.MaterialIssueRequestLine;

public class SiteMaterialIssueRequestHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(SiteMaterialIssueRequestHandler.class);

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
      JSONObject encumlines = jsonparams.getJSONObject("Lines");
      JSONArray selectedlines = encumlines.getJSONArray("_selection");
      MaterialIssueRequestLine reqline = null;
      MaterialIssueRequest request = null;
      Connection conn = OBDal.getInstance().getConnection();
      long line = 10;
      final String MaterialReqId = jsonRequest.getString("inpescmMaterialRequestId");
      request = OBDal.getInstance().get(MaterialIssueRequest.class, MaterialReqId);
      boolean allowUpdate = false;
      String roleID = request.getRole().getId();

      // delete exisiting lines
      if (selectedlines.length() > 0) {
        // check current role is present in document rule or not
        if (request.getAlertStatus().equals("DR") && (!roleID.equals(vars.getRole()))) {
          if (request.getEUTNextRole() == null) {
            OBQuery<MaterialIssueRequestHistory> custransa = OBDal.getInstance().createQuery(
                MaterialIssueRequestHistory.class,
                " as e where e.escmMaterialRequest.id=:mirID order by e.creationDate desc ");
            custransa.setNamedParameter("mirID", request.getId());
            custransa.setMaxResult(1);
            if (custransa.list().size() > 0) {
              MaterialIssueRequestHistory custransaction = custransa.list().get(0);
              if (custransaction.getRequestreqaction().equals("REV")) {
                allowUpdate = true;
              }
            }
          }
          if (allowUpdate) {
            JSONObject successMessage = new JSONObject();
            successMessage.put("severity", "error");
            successMessage.put("text", OBMessageUtils.messageBD("Escm_AlreadyPreocessed_Approved"));
            json.put("message", successMessage);
            return json;
          }
        }

        // prevent user from creating more than 12 lines
        if (request.getEscmMaterialReqlnList().size() >= 12) {
          JSONObject successMessage = new JSONObject();
          successMessage.put("severity", "error");
          successMessage.put("text", OBMessageUtils.messageBD("ESCM_IR_Max_Line"));
          json.put("message", successMessage);
          return json;
        }

        for (int a = 0; a < selectedlines.length(); a++) {

          JSONObject selectedRow = selectedlines.getJSONObject(a);
          log.debug("selectedRow:" + selectedRow);
          OBQuery<MaterialIssueRequestLine> chklineexistQry = OBDal.getInstance().createQuery(
              MaterialIssueRequestLine.class,
              "as e where e.escmMaterialRequest.id=:mirID and e.escmInitialreceipt.id =:receiptID");
          chklineexistQry.setNamedParameter("mirID", MaterialReqId);
          chklineexistQry.setNamedParameter("receiptID", selectedRow.getString("id"));
          chklineexistQry.setMaxResult(1);
          log.debug("initial getWhereAndOrderBy:" + chklineexistQry.getWhereAndOrderBy());
          log.debug("initial size:" + chklineexistQry.list().size());

          if (chklineexistQry.list().size() > 0) {
            reqline = chklineexistQry.list().get(0);

            if (selectedRow.getString("reqqty") != null
                && !selectedRow.getString("reqqty").equals("null")
                && new BigDecimal(selectedRow.getString("reqqty"))
                    .compareTo(BigDecimal.ZERO) == 0) {
              OBDal.getInstance().getConnection()
                  .prepareStatement("delete from escm_material_reqln where escm_material_reqln_id='"
                      + reqline.getId() + "'")
                  .execute();
            } else {
              reqline.setUpdated(new java.util.Date());
              reqline.setUpdatedBy(OBContext.getOBContext().getUser());
              reqline.setRequestedQty(new BigDecimal(selectedRow.getString("reqqty")));
              EscmInitialReceipt objInitialReceipt = OBDal.getInstance()
                  .get(EscmInitialReceipt.class, selectedRow.getString("id"));
              BigDecimal availbleQty = objInitialReceipt.getQuantity()
                  .subtract(objInitialReceipt.getSitereqissuedqty());
              if (availbleQty.compareTo(new BigDecimal(selectedRow.getString("reqqty"))) >= 0) {
                reqline.setDeliveredQantity(new BigDecimal(selectedRow.getString("reqqty")));
              } else {
                reqline.setDeliveredQantity(availbleQty);
              }
              OBDal.getInstance().save(reqline);
            }
          } else {
            if (selectedRow.getString("reqqty") != null
                && !selectedRow.getString("reqqty").equals("null")
                && new BigDecimal(selectedRow.getString("reqqty")).compareTo(BigDecimal.ZERO) > 0) {
              ps = conn.prepareStatement(
                  " select coalesce(max(line),0)+10 as lineno from escm_material_reqln where escm_material_request_id=?");
              ps.setString(1, MaterialReqId);
              rs = ps.executeQuery();
              if (rs.next()) {
                line = rs.getLong("lineno");
                log.debug("line:" + line);
              }
              reqline = OBProvider.getInstance().get(MaterialIssueRequestLine.class);
              EscmInitialReceipt objInitialReceipt = OBDal.getInstance()
                  .get(EscmInitialReceipt.class, selectedRow.getString("id"));
              BigDecimal availbleQty = objInitialReceipt.getQuantity()
                  .subtract(objInitialReceipt.getSitereqissuedqty());
              if (availbleQty.compareTo(new BigDecimal(selectedRow.getString("reqqty"))) >= 0) {
                reqline.setDeliveredQantity(new BigDecimal(selectedRow.getString("reqqty")));
              } else {
                reqline.setDeliveredQantity(availbleQty);
              }
              reqline.setClient(request.getClient());
              reqline.setOrganization(request.getOrganization());
              reqline.setCreationDate(new java.util.Date());
              reqline.setUpdated(new java.util.Date());
              reqline.setUpdatedBy(OBContext.getOBContext().getUser());
              reqline.setCreatedBy(OBContext.getOBContext().getUser());
              reqline.setLineNo(line);
              reqline.setDescription(selectedRow.getString("description"));
              reqline.setRequestedQty(new BigDecimal(selectedRow.getString("reqqty")));
              Product prd = OBDal.getInstance().get(Product.class,
                  selectedRow.getString("product"));
              if (prd.getEscmStockType() != null) {
                reqline.setItemType(prd.getEscmStockType().getSearchKey());
              }
              if (prd.getImage() != null) {
                reqline.setImage(prd.getImage());
              }
              reqline.setUnitPrice(objInitialReceipt.getUnitprice());
              reqline.setProduct(
                  OBDal.getInstance().get(Product.class, selectedRow.getString("product")));
              reqline.setUOM(OBDal.getInstance().get(UOM.class, selectedRow.getString("uOM")));
              reqline.setEscmMaterialRequest(request);
              reqline.setEscmInitialreceipt(objInitialReceipt);
              OBDal.getInstance().save(reqline);
              OBDal.getInstance().flush();
            }
          }
        }
        OBDal.getInstance().flush();
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
      log.error("Exception in SiteMaterialIssueRequestHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      // close db connection
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
