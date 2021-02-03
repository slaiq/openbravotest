package sa.elm.ob.hcm.selfservice.service.lookup;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.model.ad.system.Language;
import org.openbravo.model.common.geography.City;
import org.openbravo.model.common.geography.Country;
import org.openbravo.model.common.geography.CountryTrl;
import org.openbravo.model.common.geography.Region;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sa.elm.ob.hcm.EhcmTitletype;
import sa.elm.ob.hcm.selfservice.dao.lookup.LookUpDAO;
import sa.elm.ob.hcm.selfservice.dto.lookup.LookUpDTO;
import sa.elm.ob.hcm.util.SelfServiceConstants;

/**
 * 
 * @author mrahim
 *
 */
@Service
public class LookUpServiceImpl implements LookUpService {
	
	@Autowired
	private LookUpDAO lookUpDAO;

	@Override
	public List<LookUpDTO> getCountries() {

		LookUpDTO lookUpDTO = null;
		
		List <Country> countriesList = lookUpDAO.getCountries();
		List <LookUpDTO> countryDtoList = new ArrayList<LookUpDTO>();
		
		for (Country country : countriesList) {
			lookUpDTO = new LookUpDTO(country.getId(), null, country.getName());
			// This code is to get the arabic data from translation tables
			for (CountryTrl countryTrl : country.getCountryTrlList()) {
				Language language = countryTrl.getLanguage();
				if (null != language && language.getLanguage().trim().equals(SelfServiceConstants.ARABIC_KSA)) {
					lookUpDTO.setDescriptionAr(countryTrl.getName());
				}
			}
			
			countryDtoList.add(lookUpDTO);
		}
		
		return countryDtoList;
	}

	@Override
	public List<LookUpDTO> getCitiesByRegion(String regionId) {

		LookUpDTO lookUpDTO = null;
		
		List <City> citiesList = lookUpDAO.getCitiesByRegion(regionId);
		List <LookUpDTO> cityDtoList = new ArrayList<LookUpDTO>();
		
		for (City city : citiesList) {
			lookUpDTO = new LookUpDTO(city.getId(), null, city.getName());
			cityDtoList.add(lookUpDTO);
		}
		
		return cityDtoList;

	}

	@Override
	public List<LookUpDTO> getRegionsByCountry(String countryId) {

		LookUpDTO lookUpDTO = null;
		
		List <Region> regionList = lookUpDAO.getRegionsByCountry(countryId);
		List <LookUpDTO> regionDtoList = new ArrayList<LookUpDTO>();
		
		for (Region region : regionList) {
			lookUpDTO = new LookUpDTO(region.getId(), null, region.getName());
			regionDtoList.add(lookUpDTO);
		}
		
		return regionDtoList;
	}

	@Override
	public List<LookUpDTO> getTitles() {
		
		LookUpDTO lookUpDTO = null;
		
		List <EhcmTitletype> titleList = lookUpDAO.getTitles();
		List <LookUpDTO> regionDtoList = new ArrayList<LookUpDTO>();
		
		for (EhcmTitletype title : titleList) {
			lookUpDTO = new LookUpDTO(title.getId(), null, title.getName());
			regionDtoList.add(lookUpDTO);
		}
		
		return regionDtoList;

	}
}
