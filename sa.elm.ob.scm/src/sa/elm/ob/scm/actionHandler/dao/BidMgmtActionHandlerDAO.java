package sa.elm.ob.scm.actionHandler.dao;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;

import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.Escmbidsuppliers;
import sa.elm.ob.scm.Escmsalesvoucher;

public interface BidMgmtActionHandlerDAO {

  /**
   * if action is OEE creation/ display, will check if OEE exists for this bid then it will redirect
   * to OEE screen else create the OEE automatically and then redirect to OEE window
   * 
   * @param strBidId
   * @return
   */
  public JSONObject checkorcreateOEE(String strBidId);

  /**
   * if action is PEE creation/ display, will check if PEE exists for this bid then it will redirect
   * to PEE screen else create the PEE automatically and then redirect to PEE window
   * 
   * @param strBidId
   * @return
   */

  public JSONObject createPee(String strBidId);

  /**
   * create the proposal from the bid screen based on proposal action
   * 
   * @param strBidId
   * @param proposalAction
   * @param cBpartnerId
   * @return
   */
  public JSONObject createProposal(String strBidId, String proposalAction, String cBpartnerId,
      String contractCategoryID);

  /**
   * insert the proposal
   * 
   * @param cBpartnerId
   * @param bidMgmtObj
   * @param suppliers
   * @param rfpSuppliers
   * @return
   */
  public JSONObject insertNewProposal(String cBpartnerId, EscmBidMgmt bidMgmtObj,
      Escmbidsuppliers suppliers, Escmsalesvoucher rfpSuppliers, String contractCategoryID);

  /**
   * create the proposal based on proposal action from the OEE screen
   * 
   * @param openEnvelopId
   * @param proposalAction("
   *          proposal create from list, proposal creation by selecting supplier")
   * @param cBpartnerId
   * @return
   */
  public JSONObject createProposalFromOEE(String openEnvelopId, String proposalAction,
      String cBpartnerId, String contractCategoryID);

  /**
   * if action is TEE creation/ display, will check if TEE exists for this bid then it will redirect
   * to TEE screen else create the TEE automatically and then redirect to OEE window
   * 
   * @param strBidId
   * @return
   */
  public JSONObject checkorcreateTEE(String strBidId);

  /**
   * check validation before deleting bid related transactions , if po is created / proposal is
   * awarded and approved then it will not allow to remove the transactions
   * 
   * @param strBidId
   * @param removeTransaction("Until
   *          OEE,Until TEE,Until PEE,Until Proposal")
   * @return
   */
  public JSONObject checkValidationBeforeRemTran(String strBidId, String removeTransaction);

  /**
   * if validation is fine then remove the transaction based on option("Until OEE,Until TEE,Until
   * PEE,Until Proposal")
   * 
   * @param bidManagementId
   * @param removeTransaction
   * @param vars
   * @return
   */
  public JSONObject removeTransactions(String bidManagementId, String removeTransaction,
      VariablesSecureApp vars);

}
