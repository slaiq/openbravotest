package sa.elm.ob.scm.event.dao;

/**
 * Interface for all Location related DB Operations
 * 
 * @author Priyanka Ranjan 11-04-2018
 *
 */
public interface EscmLocationEventDAO {
  /**
   * get city in which selected region is associated
   * 
   * @param countryId
   * @param regionId
   * @param clientId
   * @return
   * @throws Exception
   */
  int getCityWithRegion(String countryId, String regionId, String clientId) throws Exception;

  /**
   * get region from city tab
   * 
   * @param cityId
   * @param clientId
   * @return
   * @throws Exception
   */
  String getRegion(String cityId, String clientId) throws Exception;

  /**
   * check location is already linked with any organization
   * 
   * @param locationId
   * @param clientId
   * @return
   * @throws Exception
   */
  boolean checklocationlinkedwithorg(String locationId, String clientId) throws Exception;

}
