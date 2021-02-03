package sa.elm.ob.hcm.selfservice.dao.lookup;

import java.util.List;

import org.openbravo.model.common.geography.City;
import org.openbravo.model.common.geography.Country;
import org.openbravo.model.common.geography.Region;

import sa.elm.ob.hcm.EhcmTitletype;

/**
 * DAO for Lookup Data Access
 * @author mrahim
 *
 */
public interface LookUpDAO {
	/**
	 * Get All countries
	 * @return
	 */
	List<Country> getCountries ();
	/**
	 * Get All Cities
	 * @return
	 */
	List <City> getCitiesByRegion (String regionId);
	
	/**
	 * Get the cities for a country
	 * @param countryId
	 * @return
	 */
	List <Region> getRegionsByCountry (String countryId);
	
	/**
	 * Get the Titles
	 * @return
	 */
	List <EhcmTitletype> getTitles ();
	
}
