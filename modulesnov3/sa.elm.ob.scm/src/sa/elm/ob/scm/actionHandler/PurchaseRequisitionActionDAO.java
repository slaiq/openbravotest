package sa.elm.ob.scm.actionHandler;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.model.procurement.Requisition;

import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmProposalMgmt;

/**
 * 
 * @author Gopalakrishnan on 20/06/2020
 * 
 *         Interface to perform the actions on database for PR Action process
 *
 */
public interface PurchaseRequisitionActionDAO {
  /**
   * 
   * @param objRequistion
   * @return the created Bid Object with document details
   */
  EscmBidMgmt createAutoBidFromPR(Requisition objRequistion, VariablesSecureApp vars,
      String contractCat);

  /**
   * 
   * @param objRequistion
   * @return the created Proposal Object with document details
   */
  EscmProposalMgmt createAutoProposalFromPR(Requisition objRequistion, VariablesSecureApp vars,
      String is_pee_required, String supplier);

  /**
   * 
   * @param objRequisition
   * @return true when the PR is returned successfully else false
   */
  Boolean returnPR(Requisition objRequisition, VariablesSecureApp vars, String comments);

}
