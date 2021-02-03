package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

import sa.elm.ob.scm.Escm_custody_transaction;
import sa.elm.ob.scm.MaterialIssueRequestCustody;

public class ReturnCustodyHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(ReturnCustodyHandler.class);

  @SuppressWarnings("resource")
  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub.
    JSONObject json = new JSONObject();
    PreparedStatement ps = null, ps1 = null;
    ResultSet rs = null, rs1 = null;
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      // VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject encumlines = jsonparams.getJSONObject("custody_details");
      JSONArray selectedlines = encumlines.getJSONArray("_selection");
      ShipmentInOut inout = null;
      ShipmentInOutLine inoutline = null;
      Connection conn = OBDal.getInstance().getConnection();
      long line = 10, custline = 10, custline2 = 0;
      final String inoutId = jsonRequest.getString("inpmInoutId");
      inout = OBDal.getInstance().get(ShipmentInOut.class, inoutId);
      List<Locator> locList = new ArrayList<Locator>();
      List<ShipmentInOutLine> shipmntList = new ArrayList<ShipmentInOutLine>();

      // delete exisiting lines
      if (selectedlines.length() > 0) {
        for (int a = 0; a < selectedlines.length(); a++) {
          JSONObject selectedRow = selectedlines.getJSONObject(a);
          log.debug("selectedRow:" + selectedRow);
          OBQuery<ShipmentInOutLine> chklineexistQry = OBDal.getInstance().createQuery(
              ShipmentInOutLine.class,
              "as e where e.shipmentReceipt.id=:inoutID and e.product.id=:prodID)");
          chklineexistQry.setMaxResult(1);
          chklineexistQry.setNamedParameter("inoutID", inoutId);
          chklineexistQry.setNamedParameter("prodID", selectedRow.getString("product"));
          log.debug("initial getWhereAndOrderBy:" + chklineexistQry.getWhereAndOrderBy());
          log.debug("initial size:" + chklineexistQry.list().size());

          shipmntList = chklineexistQry.list();
          if (shipmntList.size() > 0) {
            inoutline = shipmntList.get(0);
            inoutline.setUpdated(new java.util.Date());
            inoutline.setUpdatedBy(inout.getUpdatedBy());
            inoutline.setMovementQuantity(inoutline.getMovementQuantity()
                .add(new BigDecimal(selectedRow.getString("quantity"))));
            OBDal.getInstance().save(inoutline);
          } else {

            if (selectedRow.getString("product") != null) {
              ps = conn.prepareStatement(
                  " select coalesce(max(line),0)+10 as lineno from m_inoutline where m_inout_id=?");
              ps.setString(1, inoutId);
              rs = ps.executeQuery();
              if (rs.next()) {
                line = rs.getLong("lineno");
                log.debug("line:" + line);
              }
              /*
               * MaterialIssueRequestLine reqline = OBDal.getInstance().get(
               * MaterialIssueRequestLine.class, selectedRow.getString("escmMaterialReqln"));
               */
              Product objProduct = OBDal.getInstance().get(Product.class,
                  selectedRow.getString("product"));
              inoutline = OBProvider.getInstance().get(ShipmentInOutLine.class);
              inoutline.setClient(inout.getClient());
              inoutline.setOrganization(inout.getOrganization());
              inoutline.setCreationDate(new java.util.Date());
              inoutline.setCreatedBy(inout.getCreatedBy());
              inoutline.setUpdated(new java.util.Date());
              inoutline.setUpdatedBy(inout.getUpdatedBy());
              inoutline.setActive(true);
              inoutline.setShipmentReceipt(inout);
              inoutline.setLineNo(line);
              inoutline.setProduct(objProduct);
              inoutline.setEscmAdImage(objProduct.getImage());
              inoutline.setUOM(objProduct.getUOM());
              inoutline.setDescription(objProduct.getName());
              inoutline.setMovementQuantity(new BigDecimal(selectedRow.getString("quantity")));

              // inoutline.setEscmCustodyItem(reqline.isCustodyItem());
              // inoutline.setEscmTransaction("A");
              OBQuery<Locator> locator = OBDal.getInstance().createQuery(Locator.class,
                  " as e where e.warehouse.id='"
                      + (inout.getWarehouse() != null ? inout.getWarehouse().getId() : null)
                      + "' and e.default='Y' ");
              locator.setMaxResult(1);
              locList = locator.list();
              if (locList.size() > 0) {
                inoutline.setStorageBin(locList.get(0));
                log.debug("getStorageBin:" + inoutline.getStorageBin());

              } else
                inoutline.setStorageBin(null);
              OBDal.getInstance().save(inoutline);
              OBDal.getInstance().flush();
            }

          }
          // getting max line no count for line
          ps = conn.prepareStatement(
              " select coalesce(max(line),0)+10 as lineno from escm_custody_transaction where m_inoutline_id  = ? ");
          ps.setString(1, inoutline.getId());
          rs = ps.executeQuery();
          if (rs.next()) {
            custline = rs.getLong("lineno");
            log.debug("line:" + line);
          }
          // getting max line no count for line2
          ps = conn.prepareStatement(
              " select coalesce(max(line2),0)+10 as lineno from escm_custody_transaction where escm_mrequest_custody_id  = ? ");
          ps.setString(1, selectedRow.getString("id"));
          rs = ps.executeQuery();
          if (rs.next()) {
            custline2 = rs.getLong("lineno");
            log.debug("line:" + custline2);
          }

          Escm_custody_transaction custtransaction = OBProvider.getInstance()
              .get(Escm_custody_transaction.class);
          MaterialIssueRequestCustody custodydetial = OBDal.getInstance()
              .get(MaterialIssueRequestCustody.class, selectedRow.getString("id"));
          custtransaction.setClient(inout.getClient());
          custtransaction.setOrganization(inout.getOrganization());
          custtransaction.setCreationDate(new java.util.Date());
          custtransaction.setCreatedBy(inout.getCreatedBy());
          custtransaction.setUpdated(new java.util.Date());
          custtransaction.setUpdatedBy(inout.getUpdatedBy());
          custtransaction.setActive(true);
          custtransaction.setGoodsShipmentLine(inoutline);
          custtransaction.setLineNo(custline);
          custtransaction.setDocumentNo(inout.getDocumentNo());
          custtransaction.setEscmMrequestCustody(custodydetial);
          log.debug("reason:" + inout.getEscmIssuereason());
          custtransaction.setLine2(custline2);
          if (inout.getEscmReceivingtype().equals("INR"))
            custtransaction.setTransactionreason(inout.getEscmReturnreason().getCommercialName());
          else if (inout.getEscmReceivingtype().equals("IRT")) {
            ps1 = conn.prepareStatement(
                " select list.name  from ad_ref_list list where ad_reference_id='A692FD4437F84F1A990B7A60B058E9A8'"
                    + " and list.value = ?");
            ps1.setString(1, inout.getEscmIssuereason());
            rs1 = ps1.executeQuery();
            if (rs1.next()) {
              custtransaction.setTransactionreason(rs1.getString("name"));
            }
          }

          if (inout.getEscmIssuereason() != null && inout.getEscmIssuereason().equals("MA")) {
            custtransaction.setBtype("MA");
          } else {
            custtransaction.setBname(inout.getEscmBname());
            custtransaction.setBtype(inout.getEscmBtype());
          }
          if (inout.getEscmReceivingtype().equals("INR"))
            custtransaction.setTransactiontype("RE");
          if (inout.getEscmReceivingtype().equals("IRT")) {
            if (inout.getEscmIssuereason() != null && inout.getEscmIssuereason().equals("IS"))
              custtransaction.setTransactiontype("IRT");

            else
              custtransaction.setTransactiontype(inout.getEscmIssuereason());
          }
          if (inout.getEscmReceivingtype().equals("LD"))
            custtransaction.setTransactiontype("LD");

          custtransaction.setTransactionDate(inout.getMovementDate());
          custline++;
          OBDal.getInstance().save(custtransaction);
          OBDal.getInstance().flush();
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
      log.error("Exception in ReturnCustodyHandler :", e);
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
        if (rs1 != null) {
          rs1.close();
        }
        if (ps1 != null) {
          ps1.close();
        }
      } catch (Exception e) {
        log.error("Exception while closing the statement in ReturnCustodyHandler ", e);
      }
      OBContext.restorePreviousMode();
    }
  }
}
