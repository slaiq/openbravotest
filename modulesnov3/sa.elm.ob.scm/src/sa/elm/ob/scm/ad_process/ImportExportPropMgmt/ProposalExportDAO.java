package sa.elm.ob.scm.ad_process.ImportExportPropMgmt;

import java.util.HashMap;

public interface ProposalExportDAO {

  /**
   * Method to get the cell style based on Proposal Type
   * 
   * @param proposalLineId
   * @return
   */
  public HashMap<Integer, String> getProposalCellStyle(String proposalLineId);

}
