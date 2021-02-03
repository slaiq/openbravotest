package sa.elm.ob.scm.actionHandler;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.procurement.Requisition;

import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.Escmbidsourceref;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.utility.util.Constants;

public class POContractAddProposalHandler extends BaseActionHandler {

  /**
   * This class is used to add proposal lines in purchase order
   */

  private static Logger log = Logger.getLogger(POContractAddProposalHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject json = new JSONObject();
    try {
      OBContext.setAdminMode();

      // declaring JSONObject & variables
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject purreqline = jsonparams.getJSONObject("Lines");
      JSONArray selectedlines = purreqline.getJSONArray("_selection");
      final String orderId = jsonRequest.getString("C_Order_ID");
      String description = null;
      String alertWindow = sa.elm.ob.scm.util.AlertWindow.contractUser;
      String windowId = "2ADDCB0DD2BF4F6DB13B21BBCCC3038C";
      HttpServletRequest request = RequestContext.get().getRequest();
      VariablesSecureApp vars = new VariablesSecureApp(request);
      String receiveType = null;
      // get the connection
      Connection conn = OBDal.getInstance().getConnection();

      Order poid = OBDal.getInstance().get(Order.class, orderId);
      /* Validate encumbrance cannot be empty if encumbrance method is manual */
      if (poid.getEFINEncumbranceMethod().equals("M")) {
        if (poid.getEfinBudgetManencum() == null) {
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD("ESCM_Enucm_Missing_Po"));
          json.put("message", erorMessage);
          return json;
        }
      }
      if (selectedlines.length() > 1) {
        OBDal.getInstance().rollbackAndClose();
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD("ESCM_Pur_Multiselection"));
        json.put("message", errorMessage);
        return json;
      }
      if (selectedlines.length() > 0) {
        JSONObject selectedRow = selectedlines.getJSONObject(0);
        EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class,
            selectedRow.getString("id"));
        if (proposal.getOrderEMEscmProposalmgmtIDList().size() > 0) {
          OBDal.getInstance().rollbackAndClose();
          JSONObject errorMessage = new JSONObject();
          errorMessage.put("severity", "error");
          errorMessage.put("text", OBMessageUtils.messageBD("Escm_PO_Created_Proposal"));
          json.put("message", errorMessage);
          return json;
        }
        if (proposal.isTaxLine() && proposal.getEfinTaxMethod() != null) {
          poid.setEscmIstax(proposal.isTaxLine());
          poid.setEscmTaxMethod(proposal.getEfinTaxMethod());
          OBDal.getInstance().save(poid);
        }
        if (proposal.getContractType() != null) {
          poid.setEscmContactType(proposal.getContractType());
          receiveType = POOrderContractAddLinesDAO
              .getReceiveType(proposal.getContractType().getId());
          poid.setEscmReceivetype(receiveType);
          OBDal.getInstance().save(poid);
        }
        // set Maintenace Project and Maintenance Contract No based on proposal source reference
        // when adding Proposal
        if (selectedlines.length() == 1) {
          OBQuery<EscmProposalsourceRef> sourceRef = OBDal.getInstance().createQuery(
              EscmProposalsourceRef.class,
              "as e where e.escmProposalmgmtLine.id in (select ln.id from "
                  + "Escm_Proposalmgmt_Line ln where ln.escmProposalmgmt.id=:propId)");
          sourceRef.setNamedParameter("propId", selectedRow.getString("id"));
          List<EscmProposalsourceRef> propSrclist = sourceRef.list();
          if (propSrclist.size() > 0) {
            EscmProposalsourceRef propSrcRef = propSrclist.get(0);
            if (propSrcRef.getRequisition() != null) {
              Requisition requistion = propSrcRef.getRequisition();
              poid.setEscmMaintenanceProject(requistion.getEscmMaintenanceProject());
              poid.setEscmMaintenanceCntrctNo(requistion.getESCMMaintenanceContractNo());
            }
          }
          // set Maintenace Project and Maintenance Contract No based on Bid source reference
          // when adding Proposal with bid reference
          EscmProposalMgmt propMgmt = OBDal.getInstance().get(EscmProposalMgmt.class,
              selectedRow.getString("id"));
          if (propMgmt.getEscmBidmgmt() != null) {
            OBQuery<Escmbidsourceref> bidSrcref = OBDal.getInstance().createQuery(
                Escmbidsourceref.class,
                "as e where  e.escmBidmgmtLine.id in (select bid.id from escm_bidmgmt_line bid where bid.escmBidmgmt.id=:bidId)");
            bidSrcref.setNamedParameter("bidId", propMgmt.getEscmBidmgmt().getId());
            List<Escmbidsourceref> bidSrcList = bidSrcref.list();
            if (bidSrcList.size() > 0) {
              Escmbidsourceref bidSrcRef = bidSrcList.get(0);
              if (bidSrcRef.getRequisition() != null) {
                Requisition Bidrequistion = bidSrcRef.getRequisition();
                poid.setEscmMaintenanceProject(Bidrequistion.getEscmMaintenanceProject());
                poid.setEscmMaintenanceCntrctNo(Bidrequistion.getESCMMaintenanceContractNo());
              }
            }
          }
        }

        proposal.setDocumentNo(poid);
        OBDal.getInstance().save(proposal);

        // Updating the PO reference in PEE(Proposal Attribute)
        // Fetching the PEE irrespective of Proposal Version
        OBQuery<EscmProposalAttribute> proposalAttr = OBDal.getInstance().createQuery(
            EscmProposalAttribute.class,
            " as a  join a.escmProposalevlEvent b where b.status='CO' and a.escmProposalmgmt.proposalno= :proposalID ");
        proposalAttr.setNamedParameter("proposalID", proposal.getProposalno());
        List<EscmProposalAttribute> proposalAttrList = proposalAttr.list();
        if (proposalAttrList.size() > 0) {
          EscmProposalAttribute proposalAttrObj = proposalAttrList.get(0);
          proposalAttrObj.setOrder(poid);
          OBDal.getInstance().save(proposalAttrObj);
        }

        int ordercount = POcontractAddproposalDAO.insertOrderline(conn, proposal, poid);
        if (ordercount == 1) {
          // send an alert to contract user when po is created
          description = sa.elm.ob.scm.properties.Resource
              .getProperty("scm.contractuser.alert", vars.getLanguage())
              .concat("" + proposal.getProposalno());
          AlertUtility.alertInsertBasedonPreference(poid.getId(),
              poid.getDocumentNo()
                  + ((poid.getEscmNotes() != null && !poid.getEscmNotes().equals("null")
                      && !poid.getEscmNotes().equals("")) ? "-" + poid.getEscmNotes() : ""),
              "ESCM_Contract_User", poid.getClient().getId(), description, "NEW", alertWindow,
              "scm.contractuser.alert", Constants.GENERIC_TEMPLATE, windowId, null);
          JSONObject successMessage = new JSONObject();
          successMessage.put("severity", "success");
          successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
          json.put("message", successMessage);
          return json;
        } else {
          OBDal.getInstance().rollbackAndClose();
          JSONObject errorMessage = new JSONObject();
          errorMessage.put("severity", "error");
          errorMessage.put("text", OBMessageUtils.messageBD("Escm_No_Item_Proposal"));
          json.put("message", errorMessage);
          return json;
        }

      }
      // setting error message
      else {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_POAddRecIns"));
        json.put("message", successMessage);
        return json;
      }
    } /*
       * catch (Exception e) { log.error("Exception in POContractAddProposalHandler :", e);
       * OBDal.getInstance().rollbackAndClose(); e.printStackTrace(); throw new OBException(e); }
       */
    catch (Exception e) {
      log.error("Exception in POContractAddProposalHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      JSONObject errorMessage = new JSONObject();
      try {
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
        json.put("message", errorMessage);
        return json;
      } catch (JSONException e1) {
        // TODO Auto-generated catch block
        log.error("Exception in POContractAddProposalHandler :", e1);
        throw new OBException(e1);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
