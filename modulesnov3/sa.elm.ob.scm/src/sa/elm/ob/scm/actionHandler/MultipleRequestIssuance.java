package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.plm.Product;

import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.MaterialIssueRequestHistory;
import sa.elm.ob.scm.MaterialIssueRequestLine;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Gopalakrishnan on 24/05/2017
 * 
 */
public class MultipleRequestIssuance extends BaseActionHandler {
  private static Logger log = Logger.getLogger(MultipleRequestIssuance.class);

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
      JSONObject mirLines = jsonparams.getJSONObject("Mutiple_Issuance");
      JSONArray selectedlines = mirLines.getJSONArray("_selection");
      Connection conn = OBDal.getInstance().getConnection();
      int avStock = 0;
      final String mRequestId = jsonRequest.getString("inpescmMaterialRequestId");
      MaterialIssueRequest objMIR = OBDal.getInstance().get(MaterialIssueRequest.class, mRequestId);
      log.debug("mRequestId:" + mRequestId);
      String ProductName = "", stockProductName = "";
      String documentNo = "";
      Boolean isGeneric = false;

      // frame main json object

      JSONObject mainObj = new JSONObject();
      JSONArray mainArray = new JSONArray();

      // delete exisiting lines
      if (selectedlines.length() > 0) {
        for (int a = 0; a < selectedlines.length(); a++) {
          JSONObject selectedRow = selectedlines.getJSONObject(a);
          Product objProduct = OBDal.getInstance().get(Product.class,
              selectedRow.getString("product"));
          // identify generic Product
          if (!objProduct.isStocked() && !objProduct.isPurchase()
              && objProduct.getEscmStockType().getSearchKey().equals("CUS")) {
            isGeneric = Boolean.TRUE;
          }

          // get available stock for product
          ps = conn.prepareStatement(
              "select sum(de.qtyonhand) as aQty, prd.m_product_id from m_storage_detail de "
                  + " join m_locator loc on loc.m_locator_id=de.m_locator_id "
                  + " join m_warehouse wh on wh.m_warehouse_id=loc.m_warehouse_id "
                  + " join m_product prd on prd.m_product_id = de.m_product_id "
                  + " join escm_deflookups_typeln t2 on t2.escm_deflookups_typeln_id=prd.em_escm_stock_type "
                  + " where prd.m_product_id=? and ( isstocked <> 'N' and ispurchased <> 'N' )"
                  + " and wh.m_warehouse_id=? " + " group by prd.m_product_id  ");
          ps.setString(1, selectedRow.getString("product"));
          ps.setString(2, selectedRow.getString("warehouse"));
          rs = ps.executeQuery();
          if (rs.next()) {
            avStock = rs.getInt("aQty");
            log.debug("avStock:" + avStock);
          }
          // check issue Qty is higher than pending qty

          if (new BigDecimal(selectedRow.getString("multiissueqty"))
              .compareTo(new BigDecimal(selectedRow.getString("pendingQty"))) > 0) {
            if (StringUtils.isEmpty(ProductName)) {
              ProductName = objProduct.getName();
            } else {
              ProductName = ProductName + "," + objProduct.getName();
            }

          } else if ((!isGeneric) && new BigDecimal(selectedRow.getString("multiissueqty"))
              .compareTo(new BigDecimal(avStock)) > 0) {
            if (StringUtils.isEmpty(stockProductName)) {
              stockProductName = objProduct.getName() + "(available stock " + avStock + ")";
            } else {
              stockProductName = ProductName + "," + objProduct.getName() + "(available stock "
                  + avStock + ")";
            }

          } else if (new BigDecimal(selectedRow.getString("multiissueqty"))
              .compareTo(new BigDecimal("0")) <= 0) {
            JSONObject successMessage = new JSONObject();
            successMessage.put("severity", "error");
            successMessage.put("text",
                OBMessageUtils.messageBD("Escm_qty_negative").replace("@", objProduct.getName()));
            json.put("message", successMessage);
            return json;
          } else {
            if (mainObj.has(selectedRow.getString("warehouse"))) {
              mainArray = mainObj.getJSONArray(selectedRow.getString("warehouse"));
              JSONObject newJson = new JSONObject();
              newJson.put("product", selectedRow.getString("product"));
              newJson.put("qty", selectedRow.getString("multiissueqty"));
              newJson.put("lineId", selectedRow.getString("id"));
              mainArray.put(newJson);

            } else {
              mainArray = new JSONArray();
              JSONObject newJson = new JSONObject();
              newJson.put("product", selectedRow.getString("product"));
              newJson.put("qty", selectedRow.getString("multiissueqty"));
              newJson.put("lineId", selectedRow.getString("id"));
              mainArray.put(newJson);
              mainObj.put(selectedRow.getString("warehouse"), mainArray);
            }
          }

        }

        // issue qty higher than pending qty.
        if (StringUtils.isNotEmpty(ProductName)) {
          JSONObject successMessage = new JSONObject();
          successMessage.put("severity", "error");
          successMessage.put("text",
              OBMessageUtils.messageBD("Escm_issue_Qty(High)").replace("@", ProductName));
          json.put("message", successMessage);
          return json;
        } else if (StringUtils.isNotEmpty(stockProductName)) {
          JSONObject successMessage = new JSONObject();
          successMessage.put("severity", "error");
          successMessage.put("text",
              OBMessageUtils.messageBD("Escm_stock_less").replace("@", stockProductName));
          json.put("message", successMessage);
          return json;
        } else {
          Iterator<?> keys = mainObj.keys();
          objMIR.setMultiissued(true);
          while (keys.hasNext()) {
            String wareHousekey = (String) keys.next();
            if ("null".equals(wareHousekey)) {
              JSONObject successMessage = new JSONObject();
              successMessage.put("severity", "error");
              successMessage.put("text", OBMessageUtils.messageBD("ESCM_MULTIISSUANCE_WH"));
              json.put("message", successMessage);
              return json;
            }

            Warehouse objWH = OBDal.getInstance().get(Warehouse.class, wareHousekey);
            // create new MIR
            MaterialIssueRequest objCloneMIR = (MaterialIssueRequest) DalUtil.copy(objMIR, false);
            if (objWH.getEscmWarehouseType().equals("RTW")) {
              // sequence = Utility.getSpecificationSequence(objMIR.getOrganization().getId(),
              // "IRT");
              documentNo = Utility.getTransactionSequence(objMIR.getOrganization().getId(), "IRT");
            } else {
              // sequence = Utility.getSpecificationSequence(objMIR.getOrganization().getId(),
              // "MIR");
              documentNo = Utility.getTransactionSequence(objMIR.getOrganization().getId(), "MIR");
            }
            objCloneMIR.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
            objCloneMIR.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
            objCloneMIR.setWarehouse(objWH);
            objCloneMIR.setDocumentNo(documentNo);
            objCloneMIR.setSpecNo(null);
            objCloneMIR.setREQParent(objMIR.getId());
            objCloneMIR.setMultiissued(false);
            if (objWH.getEscmWarehouseType().equals("RTW")) {
              objCloneMIR.setEscmAction("CO");
              if (objMIR.getBeneficiaryType().equals("D"))
                objCloneMIR.setEscmIssuereason("ID");
              else if (objMIR.getBeneficiaryType().equals("E"))
                objCloneMIR.setEscmIssuereason("IE");
              else if (objMIR.getBeneficiaryType().equals("GD"))
                objCloneMIR.setEscmIssuereason("REW");
              else if (objMIR.getBeneficiaryType().equals("OHLD"))
                objCloneMIR.setEscmIssuereason("LD");
              else if (objMIR.getBeneficiaryType().equals("S"))
                objCloneMIR.setEscmIssuereason("ISS");
              else if (objMIR.getBeneficiaryType().equals("MA"))
                objCloneMIR.setEscmIssuereason("MA");
              else if (objMIR.getBeneficiaryType().equals("SA"))
                objCloneMIR.setEscmIssuereason("S");
              else if (objMIR.getBeneficiaryType().equals("OB"))
                objCloneMIR.setEscmIssuereason("OB");
            } else {
              // objCloneMIR.setSpecNo(sequence);
              // objCloneMIR.setAlertStatus("DR");
              objCloneMIR.setEscmAction("CO");
            }
            objCloneMIR.setDescription(
                OBMessageUtils.messageBD("Escm_MutipleIssuance_Created") + objMIR.getSpecNo());
            objCloneMIR.setAlertStatus("DR");

            OBDal.getInstance().save(objCloneMIR);

            JSONArray parsewhArray = mainObj.getJSONArray(wareHousekey);
            for (int i = 0; i < parsewhArray.length(); i++) {
              JSONObject parseObj = parsewhArray.getJSONObject(i);
              // get material Issue request Lines and create new MIR Line
              MaterialIssueRequestLine objMirLine = OBDal.getInstance()
                  .get(MaterialIssueRequestLine.class, parseObj.get("lineId"));
              Product objExistingPrd = OBDal.getInstance().get(Product.class,
                  parseObj.get("product"));
              objMirLine.setPendingQty(objMirLine.getPendingQty()
                  .subtract(new BigDecimal(parseObj.get("qty").toString())));
              OBDal.getInstance().save(objMirLine);
              MaterialIssueRequestLine objCloneMirLine = (MaterialIssueRequestLine) DalUtil
                  .copy(objMirLine, false);
              objCloneMirLine.setDeliveredQantity(new BigDecimal(parseObj.get("qty").toString()));
              objCloneMirLine.setEscmMaterialRequest(objCloneMIR);
              objCloneMirLine.setWarehouse(objWH);
              objCloneMirLine.setProduct(objExistingPrd);
              objCloneMirLine.setDescription(objExistingPrd.getName());
              objCloneMirLine.setGenericProduct(objMirLine.getGenericProduct());
              objCloneMirLine.setMultiissuance(true);
              objCloneMirLine.setGeneric(objMirLine.isGeneric());
              objCloneMirLine.setParentLineid(objMirLine);
              objCloneMirLine.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
              objCloneMirLine.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
              OBDal.getInstance().save(objCloneMirLine);
              // Task 5213 Removed for Mainwarehouse
              /*
               * // Inserting transaction for warehouse and storage bin if
               * (objCloneMirLine.getDeliveredQantity().compareTo(BigDecimal.ZERO) == 1 &
               * !objWH.getEscmWarehouseType().equals("RTW")) { MaterialTransaction trans =
               * OBProvider.getInstance().get(MaterialTransaction.class);
               * trans.setOrganization(objCloneMIR.getOrganization());
               * trans.setClient(objCloneMIR.getClient());
               * trans.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
               * trans.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
               * trans.setCreationDate(new java.util.Date()); trans.setUpdated(new
               * java.util.Date());
               * 
               * OBQuery<Locator> locator = OBDal.getInstance().createQuery( Locator.class,
               * " as e where e.warehouse.id='" + objCloneMIR.getWarehouse().getId() +
               * "' and e.default='Y' "); locator.setMaxResult(1); if (locator.list().size() > 0) {
               * trans.setStorageBin(locator.list().get(0)); } else { trans.setStorageBin(null); }
               * 
               * trans.setMovementType("ESCM_MI"); trans.setEscmTransactiontype("MIT");
               * 
               * Product mproduct = OBDal.getInstance().get(Product.class,
               * objCloneMirLine.getProduct().getId()); trans.setProduct(mproduct);
               * trans.setMovementDate(objCloneMIR.getTransactionDate());
               * 
               * trans.setMovementQuantity(objCloneMirLine.getDeliveredQantity().negate());
               * trans.setEscmMaterialReqln(objCloneMirLine);
               * 
               * trans.setUOM(OBDal.getInstance().get(UOM.class, objCloneMirLine.getUOM().getId()));
               * OBDal.getInstance().save(trans); }
               */

              // insert Custody Records only for non-return warehouse
              // why ???
              /*
               * if (!objWH.getEscmWarehouseType().equals("RTW")) {
               * 
               * String existingDocNo = "1000000001"; // final Approval Flow // entry in Transaction
               * // get Line List // get recent tag number OBQuery<MaterialIssueRequestCustody>
               * objCustodyQry = OBDal.getInstance() .createQuery(
               * MaterialIssueRequestCustody.class, "as e where e.organization.id='" +
               * objWH.getOrganization().getId() + "' order by creationDate desc");
               * objCustodyQry.setMaxResult(1); if (objCustodyQry.list().size() > 0) {
               * MaterialIssueRequestCustody recentObj = objCustodyQry.list().get(0); if
               * (recentObj.getDocumentNo() != null &&
               * StringUtils.isNotEmpty(recentObj.getDocumentNo())) existingDocNo =
               * String.valueOf(Integer.parseInt(recentObj.getDocumentNo()) + 1); } // make custody
               * for only custody products String query =
               * " select escm_material_reqln_id from escm_material_reqln ln " +
               * " join m_product prd on prd.m_product_id=ln.m_product_id " +
               * " join (select escm_deflookups_typeln_id from escm_deflookups_type lt " +
               * " join escm_deflookups_typeln ltl on ltl.escm_deflookups_type_id=lt.escm_deflookups_type_id "
               * +
               * " where lt.reference='PST' and ltl.value='CUS') cusref on cusref.escm_deflookups_typeln_id=prd.em_escm_stock_type "
               * + " where ln.escm_material_request_id='" + objMIR.getId() +
               * "' and prd.m_product_id='" + objMirLine.getProduct().getId() + "'"; ps =
               * conn.prepareStatement(query); rs = ps.executeQuery(); while (rs.next()) { // no
               * custody line insert the custodies line for (int j = 1; j <=
               * Integer.valueOf(parseObj.get("qty").toString()); j++) { // get existing tag no
               * MaterialIssueRequestCustody objCustody = OBProvider.getInstance().get(
               * MaterialIssueRequestCustody.class); Product objProduct =
               * OBDal.getInstance().get(Product.class, objCloneMirLine.getProduct().getId());
               * objCustody.setProductCategory(objProduct.getProductCategory());
               * objCustody.setDocumentNo(existingDocNo); objCustody.setQuantity(BigDecimal.ONE);
               * objCustody.setDescription(objCloneMirLine.getDescription());
               * objCustody.setAlertStatus("IU");
               * objCustody.setOrganization(objCloneMirLine.getOrganization());
               * objCustody.setEscmMaterialReqln(objCloneMirLine);
               * objCustody.setProduct(objProduct); if (objProduct.getEscmCusattribute() != null)
               * objCustody.setAttributeSet(objProduct.getEscmCusattribute());
               * objCustody.setBeneficiaryType(objCloneMIR.getBeneficiaryType());
               * objCustody.setBeneficiaryIDName(objCloneMIR.getBeneficiaryIDName());
               * OBDal.getInstance().save(objCustody); // create Transaction
               * Escm_custody_transaction objCustodyhistory = OBProvider.getInstance().get(
               * Escm_custody_transaction.class); objCustodyhistory.setLineNo(Long.valueOf(10));
               * objCustodyhistory.setDocumentNo(objCloneMIR.getSpecNo());
               * objCustodyhistory.setOrganization(objCustody.getOrganization());
               * objCustodyhistory.setBname(objCloneMIR.getBeneficiaryIDName());
               * objCustodyhistory.setBtype(objCloneMIR.getBeneficiaryType());
               * objCustodyhistory.setEscmMrequestCustody(objCustody);
               * objCustodyhistory.setTransactionDate(objCloneMIR.getTransactionDate());
               * objCustodyhistory.setTransactiontype("IE"); objCustodyhistory.setProcessed(true);
               * objCustodyhistory.setLine2(Long.valueOf(10));
               * objCustodyhistory.setDocumentNo(objCloneMIR.getSpecNo());
               * 
               * OBDal.getInstance().save(objCustodyhistory); OBDal.getInstance().flush();
               * existingDocNo = String.valueOf(Integer.parseInt(existingDocNo) + 1);
               * 
               * }
               * 
               * } }
               */
            }
            OBDal.getInstance().save(objCloneMIR);
            // insert history
            for (MaterialIssueRequestHistory objHistory : objMIR.getEscmMaterialrequestHistList()) {
              MaterialIssueRequestHistory objCloneHistory = (MaterialIssueRequestHistory) DalUtil
                  .copy(objHistory, false);
              objCloneHistory.setEscmMaterialRequest(objCloneMIR);
              objCloneHistory.setComments(OBMessageUtils.messageBD("Escm_Created_MIRhistory"));
              OBDal.getInstance().save(objCloneHistory);

            }
            OBDal.getInstance().flush();

          }
        }
        // OBDal.getInstance().flush();
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
      log.error("Exception in MultipleRequestIssuance :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      // close connection
      try {
        if (rs != null) {
          rs.close();
        }
      } catch (Exception e) {

      }
      OBContext.restorePreviousMode();
    }
  }
}
