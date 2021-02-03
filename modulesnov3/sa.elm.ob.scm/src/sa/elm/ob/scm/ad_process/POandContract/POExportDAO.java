package sa.elm.ob.scm.ad_process.POandContract;

import java.util.HashMap;

public interface POExportDAO {

  /**
   * Method to get the cell style based on PO Type
   * 
   * @param orderLineId
   * @return
   */
  public HashMap<Integer, String> getPoCellStyle(String orderLineId);

}
