package sa.elm.ob.scm.actionHandler.UnifiedProposalCreatePO;

import java.math.BigDecimal;

import org.openbravo.erpCommon.utility.OBError;

public class UnifiedProposalCreatePOImpl implements UnifiedProposalCreatePO {

  @Override
  public OBError createPO(String proposalId, String clientId, String userId, String Lang,
      BigDecimal awardedAmt) {
    return UnifiedProposalCreatePODAO.createPO(proposalId, clientId, userId, Lang, awardedAmt);
  }

}
