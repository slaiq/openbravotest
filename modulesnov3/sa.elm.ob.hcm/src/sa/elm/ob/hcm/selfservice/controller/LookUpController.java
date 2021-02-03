package sa.elm.ob.hcm.selfservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import sa.elm.ob.hcm.selfservice.dto.lookup.LookUpDTO;
import sa.elm.ob.hcm.selfservice.service.lookup.LookUpService;

/**
 * Rest Controller for all the Look Up's
 * @author mrahim
 *
 */
@RestController
@RequestMapping ("openerp/hr")
public class LookUpController {

	@Autowired
	private LookUpService lookUpService;
	
	/**
	 * Get the List of Countries
	 * @return
	 */
	@RequestMapping(value = "/lookup/country", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<List<LookUpDTO>> getCountries() {
		
		List<LookUpDTO> countries = lookUpService.getCountries();
		
		return new ResponseEntity<List<LookUpDTO>>(countries,HttpStatus.OK);
	}

	/**
	 * Get the List of Cities
	 * @return
	 */
	@RequestMapping(value = "/lookup/cities/{regionId}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<List<LookUpDTO>> getCitiesByRegion(@PathVariable("regionId") String regionId) {
		
		List<LookUpDTO> cities = lookUpService.getCitiesByRegion(regionId);
		
		return new ResponseEntity<List<LookUpDTO>>(cities,HttpStatus.OK);
	}

	/**
	 * Get the List of Regions
	 * @return
	 */
	@RequestMapping(value = "/lookup/regions/{countryId}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<List<LookUpDTO>> getRegions(@PathVariable("countryId") String countryId) {
		
		List<LookUpDTO> regions = lookUpService.getRegionsByCountry(countryId);
		
		return new ResponseEntity<List<LookUpDTO>>(regions,HttpStatus.OK);
	}

	/**
	 * Get the List of Titles
	 * @return
	 */
	@RequestMapping(value = "/lookup/titles", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<List<LookUpDTO>> getTitles() {
		
		List<LookUpDTO> titles = lookUpService.getTitles();
		
		return new ResponseEntity<List<LookUpDTO>>(titles,HttpStatus.OK);
	}

}
