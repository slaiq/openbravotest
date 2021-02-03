package sa.elm.ob.scm.event.dao;

public interface ESCMProductContCatgEventDAO {
  /**
   * Method to know whether Contract Category is used in PO/PR/BID/PropMgmt.
   * 
   * @param cntrctCtgryId
   * @param productId
   * 
   * @return .
   */
  public Boolean isCntrctCtgryUsed(String cntrctCtgryId, String productId);
}
