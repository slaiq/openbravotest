package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
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
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;

import sa.elm.ob.scm.EscmOrderSourceRef;
import sa.elm.ob.scm.EscmOrgView;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.EscmRequisitionlineV;
import sa.elm.ob.scm.actionHandler.dao.BidManagementAddLinesDAO;
import sa.elm.ob.scm.actionHandler.dao.ProposalManagementDAO;
import sa.elm.ob.scm.actionHandler.dao.ProposalManagementDAOImpl;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author gopalakrishnan on 24/08/2017
 * 
 */
public class AddPurchaseRequisitionProposal extends BaseActionHandler {
  /**
   * This is responsible to add Purchase Requisition lines in Proposal management Window
   */
  private static Logger log = Logger.getLogger(AddPurchaseRequisitionProposal.class);
  Boolean updatenewqtyflag = false, updateqtyflag = false;

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject json = new JSONObject();
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      OBContext.setAdminMode();

      // get the connection
      Connection conn = OBDal.getInstance().getConnection();
      AddPurchaseRequisitionProposalDAO dao = new AddPurchaseRequisitionProposalDAO(conn);
      // declaring JSONObject & variables
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject purreqline = jsonparams.getJSONObject("Lines");
      JSONArray selectedlines = purreqline.getJSONArray("_selection");
      EscmProposalMgmt objProposal = null;
      EscmProposalmgmtLine objProposalLine = null;
      boolean result = false;
      long line = 10;
      final String strProposalId = jsonRequest.getString("Escm_Proposalmgmt_ID");
      String reqId = "";
      Boolean prAlreadyExistBid = false;
      Boolean prAlreadyExistPO = false;

      ProposalManagementDAO proposalDAO = new ProposalManagementDAOImpl();
      // getting Proposal
      objProposal = OBDal.getInstance().get(EscmProposalMgmt.class, strProposalId);
      // delete exisiting lines
      if (selectedlines.length() > 0) {

        // check all the selected record belongs to same department.
        result = dao.checkSameDept(selectedlines);
        /*
         * if (result) { JSONObject erorMessage = new JSONObject(); erorMessage.put("severity",
         * "error"); erorMessage.put("text", OBMessageUtils.messageBD("Escm_PR_Dept_Mismatch"));
         * json.put("message", erorMessage); return json; }
         */

        // check all the selected record belongs to same Agency.
        String agencyId = dao.checkSameAgency(selectedlines, objProposal);
        /*
         * if (result) { JSONObject erorMessage = new JSONObject(); erorMessage.put("severity",
         * "error"); erorMessage.put("text", OBMessageUtils.messageBD("Escm_PR_Agency_Mismatch"));
         * json.put("message", erorMessage); return json; }
         */

        // set agency org
        // Organization agencyOrg = reqline.getRequisition().getEscmAgencyorg();
        if (!result && agencyId != null) {
          objProposal.setAgencyorg(OBDal.getInstance().get(EscmOrgView.class, agencyId));
        } else {
          objProposal.setAgencyorg(null);
        }
        for (int a = 0; a < selectedlines.length(); a++) {
          JSONObject selectedRow = selectedlines.getJSONObject(a);
          updateqtyflag = false;

          RequisitionLine reqline = OBDal.getInstance().get(RequisitionLine.class,
              selectedRow.getString("id"));
          reqId = reqline.getRequisition().getId();
          EscmRequisitionlineV parentLine = reqline.getEscmParentlineno();

          // Check selected PR lines are already added in Bid
          prAlreadyExistBid = AddPurchaseRequisitionProposalDAO.prAlreadyExistBid(reqId);
          if (prAlreadyExistBid) {
            JSONObject errorMessage = new JSONObject();
            errorMessage.put("severity", "error");
            errorMessage.put("text", OBMessageUtils.messageBD("ESCM_PR_AlreadyAddedBid"));
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

          if (parentLine == null) {
            // chk line already presented or not based on product id
            List<EscmProposalmgmtLine> chklineexistQryList = proposalDAO.checkProductExistById(
                strProposalId, selectedRow.getString("product"), reqline.getId());
            // chk line already presented or not based on product NAME
            List<EscmProposalmgmtLine> chklinedescexistQryList = proposalDAO
                .checkProductExistByName(strProposalId,
                    selectedRow.getString("linedescription").replace("'", "''"), reqline.getId());

            // check added qty is greater than remaining qty (awarded qty)
            if (reqline.getQuantity().subtract(reqline.getEscmAwardedQty())
                .compareTo(new BigDecimal(selectedRow.getString("quantity"))) < 0) {
              JSONObject msg = new JSONObject();
              msg.put("severity", "error");
              msg.put("text", OBMessageUtils.messageBD("ESCM_PurReqQtygreaterthanremainqty")
                  .replace("#", reqline.getRequisition().getDocumentNo()));
              json.put("message", msg);
              return json;
            }

            // if line already exists then chk source ref is same and if same just update the qty
            // in
            // line level as well as sourceref
            if ((chklineexistQryList != null && chklineexistQryList.size() > 0)
                || (chklinedescexistQryList != null && chklinedescexistQryList.size() > 0)) {
              if (chklineexistQryList.size() > 0) {
                objProposalLine = chklineexistQryList.get(0);
              } else if (chklinedescexistQryList.size() > 0) {
                objProposalLine = chklinedescexistQryList.get(0);
              }
              updateqtyflag = true;

              // if entered qty is zero then delete the lines
              // from orderline as well as
              // source ref

              if (new BigDecimal(selectedRow.getString("quantity"))
                  .compareTo(BigDecimal.ZERO) == 0) {
                List<EscmProposalsourceRef> srcreflineList = proposalDAO
                    .getSourceRefLines(selectedRow.getString("id"), objProposalLine.getId());
                if (srcreflineList.size() > 0) {
                  // delete the source ref line
                  EscmProposalsourceRef sourcerefline = srcreflineList.get(0);
                  OBDal.getInstance().remove(sourcerefline);
                  OBDal.getInstance().flush();

                  // update the order line qty
                  objProposalLine.setUpdated(new java.util.Date());
                  objProposalLine.setUpdatedBy(objProposal.getUpdatedBy());
                  objProposalLine.setMovementQuantity(((objProposalLine.getMovementQuantity())
                      .subtract(sourcerefline.getReservedQuantity())));
                  OBDal.getInstance().save(objProposalLine);

                  // delete the Order line , if order line qty is zero
                  if ((objProposalLine.getMovementQuantity()).compareTo(BigDecimal.ZERO) == 0) {
                    OBDal.getInstance().remove(objProposalLine);
                  }

                }
              }
              // if entered qty greater than zero than update the ref qty / insert a record and
              // update the order line qty
              else {
                AddPurchaseRequisitionProposalDAO.insertsourceref(objProposalLine,
                    selectedRow.getString("requisition"), selectedRow.getString("id"),
                    selectedRow.getString("unitPrice"), selectedRow.getString("needByDate"),
                    selectedRow.getString("department"), selectedRow.getString("quantity"),
                    selectedRow.getString("linedescription"), updateqtyflag);
              }
            }
            // if line is not presented with selected product then insert a new line in order
            // line as well as source ref
            else {
              if (new BigDecimal(selectedRow.getString("quantity"))
                  .compareTo(BigDecimal.ZERO) > 0) {

                // checking in po soruce ref whether the pr is already present are not
                OBQuery<EscmOrderSourceRef> orderSourceRef = OBDal.getInstance().createQuery(
                    EscmOrderSourceRef.class, "as e where e.requisition.id= :purReqId");
                orderSourceRef.setNamedParameter("purReqId", selectedRow.getString("requisition"));
                if (orderSourceRef.list().size() == 0) {

                  // get the next line no based on Order id
                  line = UtilityDAO.getLineNo("Escm_Proposalmgmt_Line", strProposalId, "lineNo",
                      "escmProposalmgmt");
                  // final SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(
                  // "select coalesce(max(line),0)+10 as lineno from escm_proposalmgmt_line where
                  // escm_proposalmgmt_id=:id");
                  // query.setParameter("id", strProposalId);
                  // line = ((BigDecimal) (Object) query.list().get(0)).longValue();

                  Product objProduct = OBDal.getInstance().get(Product.class,
                      selectedRow.getString("product"));
                  objProposalLine = OBProvider.getInstance().get(EscmProposalmgmtLine.class);
                  objProposalLine.setClient(objProposal.getClient());
                  objProposalLine.setOrganization(objProposal.getOrganization());
                  objProposalLine.setCreationDate(new java.util.Date());
                  objProposalLine.setCreatedBy(objProposal.getCreatedBy());
                  objProposalLine.setUpdated(new java.util.Date());
                  objProposalLine.setUpdatedBy(objProposal.getUpdatedBy());
                  objProposalLine.setActive(true);
                  objProposalLine.setEscmProposalmgmt(objProposal);
                  objProposalLine.setLineNo(line);
                  objProposalLine.setManual(false);
                  objProposalLine.setEFINUniqueCode(reqline.getEfinCValidcombination());
                  objProposalLine.setEFINUniqueCodeName(reqline.getEfinUniquecodename());
                  if (objProduct != null)
                    objProposalLine.setProduct(objProduct);
                  else
                    objProposalLine.setProduct(null);
                  objProposalLine.setUOM(reqline.getUOM());
                  objProposalLine.setNegotUnitPrice(reqline.getUnitPrice());
                  objProposalLine.setGrossUnitPrice(reqline.getUnitPrice());
                  objProposalLine.setNetprice(reqline.getUnitPrice());
                  if (reqline.getUnitPrice() != null)
                    objProposalLine.setLineTotal(reqline.getUnitPrice()
                        .multiply(new BigDecimal(selectedRow.getString("quantity"))));
                  objProposalLine.setDescription(selectedRow.getString("linedescription"));
                  objProposalLine
                      .setMovementQuantity((new BigDecimal(selectedRow.getString("quantity"))));
                  OBDal.getInstance().save(objProposalLine);
                  OBDal.getInstance().flush();

                  // insert a record in proposal source reference
                  AddPurchaseRequisitionProposalDAO.insertsourceref(objProposalLine,
                      selectedRow.getString("requisition"), selectedRow.getString("id"),
                      selectedRow.getString("unitPrice"), selectedRow.getString("needByDate"),
                      selectedRow.getString("department"), selectedRow.getString("quantity"),
                      selectedRow.getString("linedescription"), updateqtyflag);
                } else {
                  // throw error message as pr is already added in po
                  JSONObject successMessage = new JSONObject();
                  successMessage.put("severity", "error");
                  successMessage.put("text", OBMessageUtils.messageBD("ESCM_PurReq_AlreadyAdded")
                      .replace("@", "Purchase Order and Contract Summary"));
                  json.put("message", successMessage);
                  return json;
                }
              }

            }
          } else {
            // check selected line is already exists, if exists update the qty
            if (!AddPurchaseRequisitionProposalDAO.checkSelectedLineAlreadyExists(reqline,
                objProposal, selectedRow, conn)) {
              // if line is not already present then check line's parent is already present
              // if present then insert the selected line in already exists hierarchy
              // if parent is not already exist, then insert its whole hierarchy
              if (new BigDecimal(selectedRow.getString("quantity"))
                  .compareTo(BigDecimal.ZERO) > 0) {
                ArrayList<String> treeList = new ArrayList<String>();
                treeList.add(reqline.getId());
                line = AddPurchaseRequisitionProposalDAO.getLineNo(conn, strProposalId);
                AddPurchaseRequisitionProposalDAO.getParentLines(reqline, treeList, objProposal,
                    line, conn, selectedRow);
              }
            }
          }
        }
        objProposal.setADDRequisition(true);
        objProposal.setUpdated(new Date());
        if (!"".equals(reqId)) {
          Requisition req = Utility.getObject(Requisition.class, reqId);
          if (req.getEscmContactType() != null) {
            objProposal.setContractType(req.getEscmContactType());
          }
        }
        OBDal.getInstance().save(objProposal);
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
      log.error("Exception in Proposal Management Add Purchase Requisition:", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      OBDal.getInstance().commitAndClose();
      OBDal.getInstance().getSession().clear();
      try {
        // close connection
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
