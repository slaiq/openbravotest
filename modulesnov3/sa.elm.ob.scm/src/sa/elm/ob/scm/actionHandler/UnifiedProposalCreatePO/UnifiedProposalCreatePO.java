package sa.elm.ob.scm.actionHandler.UnifiedProposalCreatePO;

import java.math.BigDecimal;

import org.openbravo.erpCommon.utility.OBError;

public interface UnifiedProposalCreatePO {

  /**
   * This method is used to create PO from selected proposals
   * 
   * @param proposalId
   * @param clientId
   * @param userId
   * @param Lang
   * @param awardedAmt
   * @return
   */
  public OBError createPO(String proposalId, String clientId, String userId, String Lang,
      BigDecimal awardedAmt);

}
