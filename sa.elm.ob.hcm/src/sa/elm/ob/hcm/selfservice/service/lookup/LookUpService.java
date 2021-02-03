package sa.elm.ob.hcm.selfservice.service.lookup;

import java.util.List;

import sa.elm.ob.hcm.selfservice.dto.lookup.LookUpDTO;

/**
 * Responsible for Returning lookup data
 * @author mrahim
 *
 */
public interface LookUpService {
	
	/**
	 * Get Countries
	 * @return
	 */
	List <LookUpDTO> getCountries ();
	
	/**
	 * Get the Cities by Region
	 * @param countryId
	 * @param regionId
	 * @return
	 */
	List <LookUpDTO> getCitiesByRegion (String regionId);
	
	/**
	 * Get Regions By Country
	 * @return
	 */
	List <LookUpDTO> getRegionsByCountry (String countryId);
	
	/**
	 * Get the List of Titles
	 * @return
	 */
	List <LookUpDTO> getTitles ();
}
