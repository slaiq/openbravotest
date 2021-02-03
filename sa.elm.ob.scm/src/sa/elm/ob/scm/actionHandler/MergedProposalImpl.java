package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.util.HashMap;

public class MergedProposalImpl implements MergedProposal {

  @Override
  public void updateAwardQtyinProposal(HashMap<String, BigDecimal> qtyLineMap,
      HashMap<String, BigDecimal> qtyMap) {
    MergedProposalDao.updateQty(qtyLineMap, qtyMap);
  }

  @Override
  public boolean validateQty(HashMap<String, BigDecimal> qtyMap,
      HashMap<String, BigDecimal> qtyLineMap) {
    return MergedProposalDao.validateQty(qtyMap, qtyLineMap);
  }

  @Override
  public void updateStatusInProposal(HashMap<String, BigDecimal> qtyMap, boolean awardfullqty) {
    MergedProposalDao.updateStatusInProposal(qtyMap, awardfullqty);
  }

}
