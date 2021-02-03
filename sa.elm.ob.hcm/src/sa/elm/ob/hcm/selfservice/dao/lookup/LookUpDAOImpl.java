package sa.elm.ob.hcm.selfservice.dao.lookup;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.geography.City;
import org.openbravo.model.common.geography.Country;
import org.openbravo.model.common.geography.Region;
import org.springframework.stereotype.Repository;

import sa.elm.ob.hcm.EhcmTitletype;

/**
 * 
 * @author mrahim
 *
 */
@Repository
public class LookUpDAOImpl implements LookUpDAO {

	@Override
	public List<Country> getCountries() {
		final String query = " as c  " ; 
		
		List <Country> countryList = new ArrayList<Country>();

		try {
	        OBContext.setAdminMode();
	        
	        OBQuery<Country> result = OBDal.getInstance().createQuery(Country.class, query);
	        result.setFilterOnReadableOrganization(false);
	        result.setFilterOnReadableClients(false);
	        countryList = result.list();
	        
	      } catch (OBException e) {
	        
	        throw new OBException(e.getMessage()); 
	      } finally {
	        OBContext.restorePreviousMode();
	      }
		
	    return countryList;
		
	}


	@Override
	public List<City> getCitiesByRegion(String regionId) {
		final String query = " as c where c.region.id = ? " ;
		List <City> cityList = new ArrayList<City>();
		
	    try {
	        OBContext.setAdminMode();
	        List<Object> parametersList = new ArrayList<Object>();
			parametersList.add(regionId);
			
	        OBQuery<City> result = OBDal.getInstance().createQuery(City.class, query,parametersList);
	        result.setFilterOnReadableOrganization(false);
	        result.setFilterOnReadableClients(false);
	        cityList = result.list();
	        
	      } catch (OBException e) {
	        
	        throw new OBException(e.getMessage()); 
	      } finally {
	        OBContext.restorePreviousMode();
	      }
		
		return cityList;
	}



	@Override
	public List<Region> getRegionsByCountry(String countryId) {
		final String query = " as c where c.country.id = ? " ; 
		
		List <Region> regionList = new ArrayList<Region>();
	    try {
	        OBContext.setAdminMode();
	        List<Object> parametersList = new ArrayList<Object>();
			parametersList.add(countryId);
			
	        OBQuery<Region> result = OBDal.getInstance().createQuery(Region.class, query,parametersList);
	        result.setFilterOnReadableOrganization(false);
	        result.setFilterOnReadableClients(false);
	        regionList = result.list();
	        
	      } catch (OBException e) {
	        
	        throw new OBException(e.getMessage()); 
	      } finally {
	        OBContext.restorePreviousMode();
	      }
		
	    return regionList;

	}


	@Override
	public List<EhcmTitletype> getTitles() {
		
		final String query = " as c  " ; 
		
		List <EhcmTitletype> titleList = new ArrayList<EhcmTitletype>();
	    try {
	        OBContext.setAdminMode();
	        
	        OBQuery<EhcmTitletype> result = OBDal.getInstance().createQuery(EhcmTitletype.class, query);
	        result.setFilterOnReadableOrganization(false);
	        result.setFilterOnReadableClients(false);
	        titleList = result.list();
	        
	      } catch (OBException e) {
	        
	        throw new OBException(e.getMessage()); 
	      } finally {
	        OBContext.restorePreviousMode();
	      }
		
	    return titleList;

	}

}
