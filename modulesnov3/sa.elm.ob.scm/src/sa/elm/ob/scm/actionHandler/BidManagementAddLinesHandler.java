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
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;

import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmRequisitionlineV;
import sa.elm.ob.scm.Escmbidmgmtline;
import sa.elm.ob.scm.Escmbidsourceref;
import sa.elm.ob.scm.actionHandler.dao.BidManagementAddLinesDAO;
import sa.elm.ob.scm.ad_process.BidManagement.dao.BidManagementDAO;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * This class is used to add requisition lines in bid management
 * 
 * @author qualian-divya
 */
public class BidManagementAddLinesHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(BidManagementAddLinesHandler.class);
  Boolean updatenewqtyflag = false, updateqtyflag = false;

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject json = new JSONObject();
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      OBContext.setAdminMode();

      // declaring JSONObject & variables
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject purreqline = jsonparams.getJSONObject("Lines");
      JSONArray selectedlines = purreqline.getJSONArray("_selection");
      EscmBidMgmt bidmgt = null;
      Escmbidmgmtline bidmgtline = null;
      long line = 10;
      final String bidmgtId = jsonRequest.getString("Escm_Bidmgmt_ID");
      String reqId = "";
      Boolean prAlreadyExistProposal = false;
      Boolean prAlreadyExistPO = false;
      // getting the bid management object by using bid management Id.
      bidmgt = Utility.getObject(EscmBidMgmt.class, bidmgtId);

      // get the connection
      Connection conn = OBDal.getInstance().getConnection();

      // delete existing lines
      if (selectedlines.length() > 0) {

        /*
         * // check all the selected record belongs to same Agency. result =
         * BidManagementAddLinesDAO.checkSameAgency(selectedlines, bidmgt); if (result) { JSONObject
         * erorMessage = new JSONObject(); erorMessage.put("severity", "error");
         * erorMessage.put("text", OBMessageUtils.messageBD("Escm_PR_Agency_Mismatch"));
         * json.put("message", erorMessage); return json; }
         */

        for (int a = 0; a < selectedlines.length(); a++) {
          JSONObject selectedRow = selectedlines.getJSONObject(a);
          updateqtyflag = false;

          RequisitionLine reqline = Utility.getObject(RequisitionLine.class,
              selectedRow.getString("id"));
          reqId = reqline.getRequisition().getId();
          EscmRequisitionlineV parentLine = reqline.getEscmParentlineno();

          // Check selected PR lines are already added in Proposal
          prAlreadyExistProposal = BidManagementAddLinesDAO.prAlreadyExistProposal(reqId);
          if (prAlreadyExistProposal) {
            JSONObject errorMessage = new JSONObject();
            errorMessage.put("severity", "error");
            errorMessage.put("text", OBMessageUtils.messageBD("ESCM_PR_AlreadyAddedProposal"));
            json.put("message", errorMessage);
            return json;
          }
          // Check selected PR lines are already added in PO
          prAlreadyExistPO = BidManagementAddLinesDAO.prAlreadyExistPO(reqId);
          if (prAlreadyExistPO) {
            JSONObject errorMessage = new JSONObject();
            errorMessage.put("severity", "error");
            errorMessage.put("text", OBMessageUtils.messageBD("ESCM_PR_AlreadyAddedPO"));
            json.put("message", errorMessage);
            return json;
          }

          // We used sql here because previously inserted line can't be retrieved via hql until
          // process is finished
          line = UtilityDAO.getLineNo("escm_bidmgmt_line", bidmgtId, "lineNo", "escmBidmgmt.id");
          if (parentLine == null) {
            // chk line already presented or not based on product id
            bidmgtline = BidManagementDAO.getBidManagementLineOnProductId(bidmgtId,
                selectedRow.getString("product"));

            // chk line already presented or not based on product NAME
            if (selectedRow.getString("linedescription") != null) {
              bidmgtline = BidManagementDAO.getBidManagementLineOnProductName(bidmgtId,
                  selectedRow.getString("linedescription"));
            }

            // if line already exists then chk source ref is same and if same just update the qty in
            // line level as well as sourceref
            if (bidmgtline != null) {
              updateqtyflag = true;

              // if entered qty is zero then delete the lines from bid manangemnt line as well as
              // source ref
              if (new BigDecimal(selectedRow.getString("quantity"))
                  .compareTo(BigDecimal.ZERO) == 0) {
                List<Escmbidsourceref> bidSourRedLs = BidManagementDAO
                    .getBidSourcRefLine(bidmgtline.getId(), selectedRow.getString("id"));

                if (bidSourRedLs.size() > 0) {
                  Escmbidsourceref sourcerefline = bidSourRedLs.get(0);
                  // delete the source ref line
                  OBDal.getInstance().remove(sourcerefline);
                  OBDal.getInstance().flush();

                  // update the bid manangement line qty
                  bidmgtline.setUpdated(new java.util.Date());
                  bidmgtline.setUpdatedBy(bidmgt.getUpdatedBy());
                  bidmgtline.setMovementQuantity(bidmgtline.getMovementQuantity()
                      .subtract(sourcerefline.getReservedQuantity()));
                  OBDal.getInstance().save(bidmgtline);

                  // delete the bid manangement line , if bid managment line qty is zero
                  if (bidmgtline.getMovementQuantity().compareTo(BigDecimal.ZERO) == 0)
                    OBDal.getInstance().remove(bidmgtline);
                }
              }
              // if entered qty greater than zero than update the soruce ref qty / insert a record
              // and
              // update the bid management line qty
              else {
                BidManagementAddLinesDAO.insertsourceref(bidmgtline,
                    selectedRow.getString("requisition"), selectedRow.getString("id"),
                    selectedRow.getString("unitPrice"), selectedRow.getString("needByDate"),
                    selectedRow.getString("department"), selectedRow.getString("quantity"),
                    selectedRow.getString("linedescription"), updateqtyflag, conn);
              }
            }
            // if line is not presented with selected product then insert a new line in bid
            // management
            // line as well as source ref
            else {
              if (new BigDecimal(selectedRow.getString("quantity"))
                  .compareTo(BigDecimal.ZERO) > 0) {
                // get the next line no based on bid management id
                Product objProduct = Utility.getObject(Product.class,
                    selectedRow.getString("product"));
                bidmgtline = Utility.getEntity(Escmbidmgmtline.class);
                bidmgtline.setClient(bidmgt.getClient());
                bidmgtline.setOrganization(bidmgt.getOrganization());
                bidmgtline.setCreationDate(new java.util.Date());
                bidmgtline.setCreatedBy(bidmgt.getCreatedBy());
                bidmgtline.setUpdated(new java.util.Date());
                bidmgtline.setUpdatedBy(bidmgt.getUpdatedBy());
                bidmgtline.setActive(true);
                bidmgtline.setEscmBidmgmt(bidmgt);
                bidmgtline.setLineNo(line);
                bidmgtline.setProduct(objProduct);
                bidmgtline.setProductCategory(reqline.getEscmProdcate());
                bidmgtline.setParentline(null);
                bidmgtline.setUOM(reqline.getUOM());
                bidmgtline.setDescription(selectedRow.getString("linedescription"));
                bidmgtline.setMovementQuantity(new BigDecimal(selectedRow.getString("quantity")));
                bidmgtline.setManual(false);
                if (selectedRow.getString("uniqueCode") != null)
                  bidmgtline.setAccountingCombination(OBDal.getInstance()
                      .get(AccountingCombination.class, selectedRow.getString("uniqueCode")));

                OBDal.getInstance().save(bidmgtline);
                OBDal.getInstance().flush();

                // insert a record in bid management source ref
                BidManagementAddLinesDAO.insertsourceref(bidmgtline,
                    selectedRow.getString("requisition"), selectedRow.getString("id"),
                    selectedRow.getString("unitPrice"), selectedRow.getString("needByDate"),
                    selectedRow.getString("department"), selectedRow.getString("quantity"),
                    selectedRow.getString("linedescription"), updateqtyflag, conn);
              }
            }
          } else {
            if (parentLine != null) {
              // check selected line is already exists, if exists update the qty
              if (!BidManagementAddLinesDAO.checkSelectedLineAlreadyExists(reqline, bidmgt,
                  selectedRow, conn)) {
                // if line is not already present then check line's parent is already present
                // if present then insert the selected line in already exists hierarchy
                // if parent is not already exist, then insert its whole hierarchy
                if (new BigDecimal(selectedRow.getString("quantity"))
                    .compareTo(BigDecimal.ZERO) > 0) {
                  ArrayList<String> parentList = new ArrayList<String>();
                  parentList.add(reqline.getId());
                  BidManagementAddLinesDAO.getParentLines(reqline, parentList, bidmgt, line, conn,
                      selectedRow);

                }
              }
            }
          }
        }
        OBDal.getInstance().flush();

        // update line description if source ref having different description
        for (Escmbidmgmtline bidline : bidmgt.getEscmBidmgmtLineList()) {
          String desc = BidManagementAddLinesDAO.getBidSourcDescription(bidline.getId());
          bidline.setDescription(desc);

          OBDal.getInstance().save(bidline);
          OBDal.getInstance().flush();
        }
        if (!"".equals(reqId)) {
          Requisition req = Utility.getObject(Requisition.class, reqId);
          if (req.getEscmContactType() != null) {
            bidmgt.setContractType(req.getEscmContactType());
          }
        }
        OBDal.getInstance().save(bidmgt);
        OBDal.getInstance().flush();
        // setting success message
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "success");
        successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
        json.put("message", successMessage);
        return json;
      }
      // setting error message
      else {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_POAddRecIns"));
        json.put("message", successMessage);
        return json;
      }

    } catch (Exception e) {
      log.error("Exception in BidManagementAddLinesHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      JSONObject errorMessage = new JSONObject();
      try {
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
        json.put("message", errorMessage);
        return json;
      } catch (JSONException e1) {
        // TODO Auto-generated catch block
        log.error("Exception in BidManagementAddLinesHandler :", e1);
        throw new OBException(e1);
      }
    } finally {
      try {
        // close connection
        if (rs != null) {
          rs.close();
        }
        if (ps != null) {
          ps.close();
        }
      } catch (Exception e) {
        JSONObject errorMessage = new JSONObject();
        try {
          errorMessage.put("severity", "error");
          errorMessage.put("text", OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
          json.put("message", errorMessage);
          return json;
        } catch (JSONException e1) {
          // TODO Auto-generated catch block
          log.error("Exception in BidManagementAddLinesHandler :", e1);
          throw new OBException(e1);
        }
      }
      OBContext.restorePreviousMode();
    }
  }
}
