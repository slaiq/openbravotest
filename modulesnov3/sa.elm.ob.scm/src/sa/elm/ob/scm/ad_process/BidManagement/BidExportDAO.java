package sa.elm.ob.scm.ad_process.BidManagement;

import java.util.HashMap;

public interface BidExportDAO {

  /**
   * Method to get the cell style based on PO Type
   * 
   * @param orderLineId
   * @return
   */
  public HashMap<Integer, String> getBidCellStyle(String orderLineId);

}
