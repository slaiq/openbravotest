package sa.elm.ob.scm.actionHandler.dao;

import java.util.List;

import sa.elm.ob.scm.EscmProposalmgmtHist;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalsourceRef;

public interface ProposalManagementDAO {

  /**
   * chk line already presented or not based on product id
   * 
   * @param strProposalId
   * @param productId
   * @return list
   */
  public List<EscmProposalmgmtLine> checkProductExistById(String strProposalId, String productId, String reqline);

  /**
   * chk line already presented or not based on product name
   * 
   * @param strProposalId
   * @param desc
   * @return list
   */
  public List<EscmProposalmgmtLine> checkProductExistByName(String strProposalId, String desc,
      String reqline);

  /**
   * get proposal source ref lines
   * 
   * @param reqLineId
   * @param proposalLineId
   * @return list
   */
  public List<EscmProposalsourceRef> getSourceRefLines(String reqLineId, String proposalLineId);

  /**
   * get proposal history
   * 
   * @param propId
   * 
   * @return list
   */
  public List<EscmProposalmgmtHist> getProposalHist(String propId);

  /**
   * check lines added from PR
   * 
   * @param propLnId
   * 
   * @return list
   */
  public List<EscmProposalsourceRef> checkLinesAddedFromPR(String propLnId);
}
