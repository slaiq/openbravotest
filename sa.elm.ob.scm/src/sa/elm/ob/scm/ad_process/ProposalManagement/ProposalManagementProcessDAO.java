package sa.elm.ob.scm.ad_process.ProposalManagement;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.pricing.pricelist.PriceList;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmPurchaseOrderConfiguration;
import sa.elm.ob.scm.Escmbidconfiguration;
import sa.elm.ob.scm.Escmopenenvcommitee;
import sa.elm.ob.utility.EutDocappDelegateln;

public interface ProposalManagementProcessDAO {

  /**
   * Validate if PO created when cancel
   * 
   * @param proposalId
   * 
   * @return list
   */
  public List<Order> checkPOCreated(String proposalId);

  /**
   * get proposal attribute
   * 
   * @param proposalId
   * 
   * @return list
   */
  public Object getProposalAttr(String proposalId);

  /**
   * Based on configuration minvalue , getting purchase order type is purchase order /contract
   * 
   * @param orgId
   * @param totalAmt
   * @return list
   */
  public List<EscmPurchaseOrderConfiguration> getPOTypeBasedOnValue(String orgId,
      BigDecimal totalAmt);

  /**
   * get period start date
   * 
   * @param clientId
   * @return String
   */
  public String getPeriodStartDate(String clientId);

  /**
   * get budget reference from period date
   * 
   * @param clientId
   * @param startingDate
   * @return String
   */
  public String getBudgetFromPeriod(String clientId, String startingDate);

  /**
   * fetch Financial Year
   * 
   * @param clientId
   * @return String
   */
  public String getFinancialYear(String clientId);

  /**
   * fetching warehouse
   * 
   * @param clientID
   * 
   * @return list
   */
  public List<Warehouse> getWarehouse(String clientID);

  /**
   * fetching location
   * 
   * @param bpartnerID
   * 
   * @return list
   */
  public List<Location> getLocation(String bpartnerID);

  /**
   * fetching price list
   * 
   * @param clientID
   * 
   * @return list
   */
  public List<PriceList> getPriceList(String clientID);

  /**
   * fetching payment term
   * 
   * @param clientID
   * 
   * @return list
   */
  public List<PaymentTerm> getPaymentTerm(String clientID);

  /**
   * get transaction document
   * 
   * @param orgId
   * @param clientID
   * @return Object
   */
  public Object getTransactionDoc(String orgId, String clientId);

  /**
   * get bid configuration
   * 
   * @param clientID
   * @param orgId
   * @return list
   */
  public List<Escmbidconfiguration> getBidConfiguration(String clientId, String orgId);

  /**
   * get proposal source reference
   * 
   * @param propLnId
   * 
   * @return list
   */
  public List<Object> getProposalLnTotalQty(String propLnId);

  /**
   * get open envelop committe
   * 
   * @param bidID
   * 
   * @return list
   */
  public List<Escmopenenvcommitee> getOpenEnvCommitte(String bidID);

  /**
   * get proposal lines
   * 
   * @param proposalID
   * 
   * @return list
   */
  public List<EscmProposalmgmtLine> getProposalLines(String proposalID);

  /**
   * get User Role
   * 
   * @param roleID
   * 
   * @return list
   */
  public List<UserRoles> getUserRole(String roleID);

  /**
   * get delegation
   * 
   * @param roleID
   * @param currentDate
   * @param documentType
   * @return list
   */
  public List<EutDocappDelegateln> getDelegation(String roleID, Date currentDate,
      String documentType);

  /**
   * get encumbrance lines
   * 
   * @param encumID
   * @param acctID
   * 
   * @return list
   */
  public List<EfinBudgetManencumlines> getEncumLines(String encumID, String acctID);

  /**
   * 
   * @param proposalMgmt
   * @return
   */
  public String getmaxbidproposallastdayandbidnumber(EscmProposalMgmt proposalMgmt);

  /**
   * if proposal is associate with manual encumbrance or auto encumbrance( with out bid) then check
   * used amount is greater or zero
   * 
   * @param EscmProposalmgmtLine
   * @return if greater the used amount then return false
   */
  public boolean chkEncumbranceLineCancelValid(EscmProposalmgmtLine proposalMgmtLine);

  /**
   * check associated PR Full Qty used or partialy used or combine more than one Encumbrance
   * 
   * @param EscmProposalmgmtLine
   * @return Jsonobject of Encumbrance List, (Type-Split or Merge),PR is associated or Not
   */
  public JSONObject checkFullPRLineQtyUitlizeorNot(EscmProposalmgmtLine line);

  /**
   * check and update proposal pr full qty
   * 
   * @param proposalmgmtline
   * @param encumbrance
   * @param isChkFundsAppliedAmt
   * @param isreject
   * @return
   */
  public Boolean chkAndUpdateforProposalPRFullQty(EscmProposalmgmtLine proposalmgmtline,
      EfinBudgetManencum encumbrance, Boolean isChkFundsAppliedAmt, Boolean isreject);

  /**
   * update auto encumbrance value in budget enquiry ( with out bid in proposal)
   * 
   * @param EscmProposalmgmtLine
   */
  public void updateAutoEncumbrancechanges(EscmProposalmgmtLine proposalmgmtline, boolean isCancel);

  /**
   * This Method is used to revert the encumbrance stage
   * 
   * @param result
   * @param proposal
   * @param proposalmgmtline
   */
  public void revertEncumbranceStage(JSONObject result, EscmProposalMgmt proposal,
      EscmProposalmgmtLine proposalmgmtline);

  /**
   * get base revision no of proposal
   * 
   * @param proposalId
   * 
   * @return revno in Long
   */
  public Long checkBaseProposal(String proposalId);

  /**
   * get latest revision of proposal
   * 
   * @param baseproposalId
   * 
   * @return revno in Long
   */
  public Long getRevisionNo(String basePropId);

  /**
   * check and validate duplicate version
   * 
   * @param propId
   * @return true, if has duplicate
   */
  public boolean checkDuplicateVersion(String propId);

  /**
   * Checks the mandatory fields in PO
   * 
   * @param proposalId
   * @return mandatory list
   */
  public List<String> checkMandatoryfields(String proposalId);

  /**
   * check the PO is contract or not
   * 
   * @param totalAmount
   * @param orgId
   * @param clientId
   * @return true if it is contract
   */
  public boolean checkOrderIsContract(BigDecimal totalAmount, String orgId, String clientId);

  /**
   * Check minimum proposal is approved
   * 
   * @param proposal
   * @return
   */
  public boolean isMinProposalApproved(EscmProposalMgmt proposal);
}
