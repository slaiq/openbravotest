package sa.elm.ob.utility.tabadul;

import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.codec.binary.Base64;
import org.openbravo.base.session.OBPropertiesProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;


public class TabadulIntegrationServiceImpl implements TabadulIntegrationService {

	private static final Logger log = LoggerFactory.getLogger(TabadulIntegrationServiceImpl.class);
	private TabadulAuthenticationService tabadulAuthenticationService;

	/**
	 * Properties object for reading from config file
	 */
	private Properties poolPropertiesConfig;
	
	private static final String CHAR_SET = "US-ASCII" ;
	private static final String AUTH_TYPE = "Basic " ;
	private static final String AUTH_HEADER = "Authorization" ;
	private static final String SEPARATOR = ":" ;
	private static final String URL_CONFIG = "tabadul.api.url" ;
	private static final String UPDATE_METHOD_URL = "update-tender" ;
	private static final String GET_SUPPLIER_BY_CR_METHOD_URL = "get-vendor-by-crnumber" ;
	private static final String GET_VENDOR_BY_ID_METHOD_URL = "get-vendor-by-id" ;
	private static final String COUNTRY_LOOKUP_METHOD_URL = "countries-lookup" ;
	private static final String UPDATE_FILE_METHOD_URL = "upload-file" ;
	private static final String BASIC_AUTH_USERNAME_CONFIG = "tabadul.basicauth.username" ;
	private static final String BASIC_AUTH_PWD_CONFIG = "tabadul.basicauth.password" ;
	private static final String UPDATE_DATA_REQ_PARAM = "data" ;
	private static final String CR_NO_REQ_PARAM = "cr-number" ;
	private static final String VENDOR_ID_REQ_PARAM = "uid" ;
	private static final String WITH_ADDRESS_REQ_PARAM = "withaddress" ;
	private static final String TID_REQ_PARAM = "tid" ;
	private static final String UPDATE_FILE_TYPE_REQ_PARAM = "type" ;
	private static final String UPDATE_FILE_FILE_REQ_PARAM = "file" ;
	private static final String COOKIE_PARAM = "Cookie" ;
	private static final String PUBLISH_TENDER_URL = "publish-tender";
	private static final String CANCEL_TENDER_URL = "cancel-tender";
	private static final String DELETE_TENDER_URL = "delete-tender";
	private static final String CANCEL_TENDER_FILE_URL = "cancel-file";
	private static final String DELETE_TENDER_FILE_URL = "delete-file";
	private static final String EXTEND_TENDER_DATE_URL = "extend-date";
	private static final String LIST_PURCHASES_URL = "list-purchases";
	private static final String APPROVE_EXTEND_TENDER_DATE_URL = "approve-extend-date";
	private static final String PUBLISH_TENDER_FILE_URL = "publish-tender-file";
	private static final String FID_REQ_PARAM = "fid";
	
	@Override
	public String initializeRequest(String userEmail, String password) {

		return tabadulAuthenticationService.authenticate(userEmail, password);
	}
	
	/**
	 * Check internally if the session token is valid
	 * @param sessionToken
	 * @return
	 */
	@SuppressWarnings("unused")
	private Boolean isTokenValid (String sessionToken) {
		return tabadulAuthenticationService.isTokenValid(sessionToken);
	}

	@Override
	public TabadulResponse getSupplierByCR(String crNumber, String sessionToken) throws Exception {
		return getSupplierByCR(crNumber, false, sessionToken);
	}

	@Override
	public TabadulResponse getSupplierByCR(String crNumber, boolean withAddress, String sessionToken) throws Exception {
		TabadulResponse tabadulResponse = null;
		String updateUrl = getPoolPropertiesConfig().getProperty(URL_CONFIG) + GET_SUPPLIER_BY_CR_METHOD_URL;
		
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new TabadulResponseErrorHandler());
		HttpHeaders headers = createHeaders(getPoolPropertiesConfig().getProperty(BASIC_AUTH_USERNAME_CONFIG),
				getPoolPropertiesConfig().getProperty(BASIC_AUTH_PWD_CONFIG));
		headers.add (COOKIE_PARAM,sessionToken);
		
		MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
		map.add(CR_NO_REQ_PARAM, crNumber);
		
		if (withAddress) {
			map.add(WITH_ADDRESS_REQ_PARAM, "yes");
		} else {
			map.add(WITH_ADDRESS_REQ_PARAM, "no");			
		}
		
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
		
		ResponseEntity<String> response = restTemplate.postForEntity( updateUrl, request , String.class );
		log.info ("Get Supplier By CR : " + crNumber + " Response : " + response.getBody());
		
		ObjectMapper mapper = new ObjectMapper();

		if (TabadulRestUtil.isError(response.getStatusCode())) {
			tabadulResponse = mapper.readValue(response.getBody(), TabadulResponse.class);
		}else {
			SupplierVO supplierVO = mapper.readValue(response.getBody(), SupplierVO.class);
			tabadulResponse = new TabadulResponse();
			tabadulResponse.setSupplierVO(supplierVO);
		}
		
		return tabadulResponse;
	}
	@Override
	public String createOrUpdateTender(String sessionToken, TenderVO tenderVO) throws Exception{
		
		String updateUrl = getPoolPropertiesConfig().getProperty(URL_CONFIG) + UPDATE_METHOD_URL;
		String data = convertObjectToXML(tenderVO);
		log.info("Create Tender XML --->" + data);
		data = new String(data.getBytes("UTF-8"), "ISO-8859-1");
		
		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = createHeaders(getPoolPropertiesConfig().getProperty(BASIC_AUTH_USERNAME_CONFIG),
				getPoolPropertiesConfig().getProperty(BASIC_AUTH_PWD_CONFIG));
		headers.add (COOKIE_PARAM,sessionToken);
		
		MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
		map.add(UPDATE_DATA_REQ_PARAM, data);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

		ResponseEntity<String> response = restTemplate.postForEntity( updateUrl, request , String.class );
		
		ObjectMapper mapper = new ObjectMapper();
		
		TabadulResponse tabadulResponse = mapper.readValue(response.getBody(), TabadulResponse.class);
		
		return tabadulResponse.getTid();
	}
	
	/**
	 * Generic method to convert object to XML
	 * @param object
	 * @return
	 * @throws JAXBException
	 */
	
	private String convertObjectToXML (Object object) throws JAXBException {
		
		JAXBContext context = JAXBContext.newInstance(object.getClass());
		
		StringWriter sw = new StringWriter();
        Marshaller m = context.createMarshaller();
       
        //for pretty-print XML in JAXB
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.setProperty(Marshaller.JAXB_FRAGMENT, true);
        m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        m.marshal(object, sw);

		
		return sw.toString() ;
		
	}
	
	/**
	 * Create the Header for Basic Authentication
	 * @param username
	 * @param password
	 * @return
	 */
	private HttpHeaders createHeaders(String username, String password){
		
		HttpHeaders httpHeaders = new HttpHeaders();
		String auth = username + SEPARATOR + password;
        byte[] encodedAuth = Base64.encodeBase64( 
           auth.getBytes(Charset.forName(CHAR_SET)) );
        String authHeader = AUTH_TYPE + new String( encodedAuth );
        httpHeaders.set( AUTH_HEADER, authHeader );
        httpHeaders.set("Accept-Charset", "utf-8");
        //httpHeaders.setContentType(MediaType.parseMediaType("text/plain;charset=UTF-8"));
        
        
		return httpHeaders;
	}

	@Override
	public String uploadTenderFile(String tenderId, String fileType, FileSystemResource fileSystemResource , String sessionToken) throws Exception {

		String updateUrl = getPoolPropertiesConfig().getProperty(URL_CONFIG) + UPDATE_FILE_METHOD_URL;
		
		List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
        acceptableMediaTypes.add(MediaType.MULTIPART_FORM_DATA);
        
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = createHeaders(getPoolPropertiesConfig().getProperty(BASIC_AUTH_USERNAME_CONFIG),
				getPoolPropertiesConfig().getProperty(BASIC_AUTH_PWD_CONFIG));
		headers.add (COOKIE_PARAM,sessionToken);
		headers.setAccept(acceptableMediaTypes);
		
		MultiValueMap<String, Object> map= new LinkedMultiValueMap<String, Object>();
		map.add(TID_REQ_PARAM, tenderId);
		map.add(UPDATE_FILE_TYPE_REQ_PARAM, fileType);
		map.add(UPDATE_FILE_FILE_REQ_PARAM, fileSystemResource);
		

		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(map, headers);
	 

		ResponseEntity<String> response = restTemplate.postForEntity( updateUrl, request , String.class );
		
		ObjectMapper mapper = new ObjectMapper();
		
		TabadulResponse tabadulResponse = mapper.readValue(response.getBody(), TabadulResponse.class);

		return tabadulResponse.getFid();
		
	}
	
	@Override
	public String publishTenderFile(String tenderId, String tenderFileId,String sessionToken) throws Exception {
		
		String publishFileUrl = getPoolPropertiesConfig().getProperty(URL_CONFIG)+ PUBLISH_TENDER_FILE_URL;
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = createHeaders(getPoolPropertiesConfig().getProperty(BASIC_AUTH_USERNAME_CONFIG),getPoolPropertiesConfig().getProperty(BASIC_AUTH_PWD_CONFIG));
		headers.add(COOKIE_PARAM, sessionToken);

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
		map.add(TID_REQ_PARAM, tenderId);
		map.add(FID_REQ_PARAM, tenderFileId);

		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(map, headers);

		ResponseEntity<String> response = restTemplate.postForEntity(publishFileUrl, request, String.class);

		ObjectMapper mapper = new ObjectMapper();

		TabadulResponse tabadulResponse = mapper.readValue(response.getBody(),TabadulResponse.class);

		return tabadulResponse.getStatus();
	}

	@Override
	public String cancelTenderFile(String tenderId, String tenderFileId,String sessionToken) throws Exception {
		
		String cancelFileUrl = getPoolPropertiesConfig().getProperty(URL_CONFIG) + CANCEL_TENDER_FILE_URL;
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = createHeaders(getPoolPropertiesConfig().getProperty(BASIC_AUTH_USERNAME_CONFIG),getPoolPropertiesConfig().getProperty(BASIC_AUTH_PWD_CONFIG));
		headers.add(COOKIE_PARAM, sessionToken);

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
		map.add(TID_REQ_PARAM, tenderId);
		map.add(FID_REQ_PARAM, tenderFileId);

		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(map, headers);

		ResponseEntity<String> response = restTemplate.postForEntity(cancelFileUrl, request, String.class);

		ObjectMapper mapper = new ObjectMapper();

		TabadulResponse tabadulResponse = mapper.readValue(response.getBody(),TabadulResponse.class);

		return tabadulResponse.getStatus();

	}

	@Override
	public String publishTender(String tenderId, String sessionToken) throws Exception {

	    String publishUrl = getPoolPropertiesConfig().getProperty(URL_CONFIG) + PUBLISH_TENDER_URL;
	    RestTemplate restTemplate = new RestTemplate();
	    HttpHeaders headers = createHeaders(
	        getPoolPropertiesConfig().getProperty(BASIC_AUTH_USERNAME_CONFIG),
	        getPoolPropertiesConfig().getProperty(BASIC_AUTH_PWD_CONFIG));
	    headers.add(COOKIE_PARAM, sessionToken);

	    MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
	    map.add(TID_REQ_PARAM, tenderId);

	    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(
	        map, headers);

	    ResponseEntity<String> response = restTemplate.postForEntity(publishUrl, request, String.class);

	    ObjectMapper mapper = new ObjectMapper();

	    TabadulResponse tabadulResponse = mapper.readValue(response.getBody(), TabadulResponse.class);

	    return tabadulResponse.getStatus();

	  }
	
	@Override
	public String cancelTender(String tenderId, String sessionToken) throws Exception {

	    String publishUrl = getPoolPropertiesConfig().getProperty(URL_CONFIG) + CANCEL_TENDER_URL;
	    RestTemplate restTemplate = new RestTemplate();
	    HttpHeaders headers = createHeaders(
	        getPoolPropertiesConfig().getProperty(BASIC_AUTH_USERNAME_CONFIG),
	        getPoolPropertiesConfig().getProperty(BASIC_AUTH_PWD_CONFIG));
	    headers.add(COOKIE_PARAM, sessionToken);

	    MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
	    map.add(TID_REQ_PARAM, tenderId);

	    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(
	        map, headers);

	    ResponseEntity<String> response = restTemplate.postForEntity(publishUrl, request, String.class);

	    ObjectMapper mapper = new ObjectMapper();

	    TabadulResponse tabadulResponse = mapper.readValue(response.getBody(), TabadulResponse.class);

	    return tabadulResponse.getStatus();

	  }
	
	@Override
	public String deleteTender(String tenderId, String sessionToken) throws Exception {
	    String deleteUrl = getPoolPropertiesConfig().getProperty(URL_CONFIG) + DELETE_TENDER_URL;
	    RestTemplate restTemplate = new RestTemplate();
	    HttpHeaders headers = createHeaders(
	        getPoolPropertiesConfig().getProperty(BASIC_AUTH_USERNAME_CONFIG),
	        getPoolPropertiesConfig().getProperty(BASIC_AUTH_PWD_CONFIG));
	    headers.add(COOKIE_PARAM, sessionToken);

	    MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
	    map.add(TID_REQ_PARAM, tenderId);

	    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(
	        map, headers);

	    ResponseEntity<String> response = restTemplate.postForEntity(deleteUrl, request, String.class);

	    ObjectMapper mapper = new ObjectMapper();

	    TabadulResponse tabadulResponse = mapper.readValue(response.getBody(), TabadulResponse.class);

	    return tabadulResponse.getStatus();


	}

	@Override
	public String deleteTenderFile(String tenderId, String fileId,String sessionToken) throws Exception {
		String deleteFileUrl = getPoolPropertiesConfig().getProperty(URL_CONFIG) + DELETE_TENDER_FILE_URL;
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = createHeaders(getPoolPropertiesConfig().getProperty(BASIC_AUTH_USERNAME_CONFIG),getPoolPropertiesConfig().getProperty(BASIC_AUTH_PWD_CONFIG));
		headers.add(COOKIE_PARAM, sessionToken);

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
		map.add(TID_REQ_PARAM, tenderId);
		map.add(FID_REQ_PARAM, fileId);

		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(map, headers);

		ResponseEntity<String> response = restTemplate.postForEntity(deleteFileUrl, request, String.class);

		ObjectMapper mapper = new ObjectMapper();

		TabadulResponse tabadulResponse = mapper.readValue(response.getBody(),TabadulResponse.class);

		return tabadulResponse.getStatus();
	}

	@Override
	public PurchasesVO getPurchasesForTender(String tabadulTenderId, String sessionToken) throws Exception {
		String extendDateUrl = getPoolPropertiesConfig().getProperty(URL_CONFIG) + LIST_PURCHASES_URL;
		
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = createHeaders(getPoolPropertiesConfig().getProperty(BASIC_AUTH_USERNAME_CONFIG),
				getPoolPropertiesConfig().getProperty(BASIC_AUTH_PWD_CONFIG));
		headers.add (COOKIE_PARAM,sessionToken);
		
		MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
		map.add(TID_REQ_PARAM, tabadulTenderId);
		
		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(map, headers);

		ResponseEntity<String> response = restTemplate.postForEntity( extendDateUrl, request , String.class );
		
		ObjectMapper mapper = new ObjectMapper();
		
		PurchasesVO purchasesVO = mapper.readValue(response.getBody(), PurchasesVO.class);

		return purchasesVO;
	}

	@Override
	public String extendTenderDates(TenderVO tenderVO, String sessionToken) throws Exception{
		String extendDateUrl = getPoolPropertiesConfig().getProperty(URL_CONFIG) + EXTEND_TENDER_DATE_URL;
		
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = createHeaders(getPoolPropertiesConfig().getProperty(BASIC_AUTH_USERNAME_CONFIG),
				getPoolPropertiesConfig().getProperty(BASIC_AUTH_PWD_CONFIG));
		headers.add (COOKIE_PARAM,sessionToken);
		
		MultiValueMap<String, Object> map= getExtendTenderDatesParams(tenderVO);

		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(map, headers);

		ResponseEntity<String> response = restTemplate.postForEntity( extendDateUrl, request , String.class );
		
		ObjectMapper mapper = new ObjectMapper();
		
		TabadulResponse tabadulResponse = mapper.readValue(response.getBody(), TabadulResponse.class);
		
		return tabadulResponse.getAid() ;

	}
	
	@Override
	public SupplierVO getSupplierById(String vendorId, String sessionToken) throws Exception {
		
		String updateUrl = getPoolPropertiesConfig().getProperty(URL_CONFIG) + GET_VENDOR_BY_ID_METHOD_URL;
		
		RestTemplate restTemplate = new RestTemplate();
		
		HttpHeaders headers = createHeaders(getPoolPropertiesConfig().getProperty(BASIC_AUTH_USERNAME_CONFIG),
				getPoolPropertiesConfig().getProperty(BASIC_AUTH_PWD_CONFIG));
		headers.add (COOKIE_PARAM,sessionToken);
		
		MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
		map.add(VENDOR_ID_REQ_PARAM, vendorId);
		
		
		map.add(WITH_ADDRESS_REQ_PARAM, "yes");
		
		
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
		
		ResponseEntity<String> response = restTemplate.postForEntity( updateUrl, request , String.class );
		
		ObjectMapper mapper = new ObjectMapper();

		SupplierVO supplierVO = mapper.readValue(response.getBody(), SupplierVO.class);
		
		return supplierVO;
	
	}

	
	@Override
	public String approveExtendTenderDates(String aid, String sessionToken) throws Exception {
		String extendDateUrl = getPoolPropertiesConfig().getProperty(URL_CONFIG) + APPROVE_EXTEND_TENDER_DATE_URL;
		
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = createHeaders(getPoolPropertiesConfig().getProperty(BASIC_AUTH_USERNAME_CONFIG),
				getPoolPropertiesConfig().getProperty(BASIC_AUTH_PWD_CONFIG));
		headers.add (COOKIE_PARAM,sessionToken);
		
		MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
		map.add(TabadulParametersE.EXTEND_APPROVAL_ID.getParameterName(), aid);

		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(map, headers);

		ResponseEntity<String> response = restTemplate.postForEntity( extendDateUrl, request , String.class );
		
		ObjectMapper mapper = new ObjectMapper();
		
		TabadulResponse tabadulResponse = mapper.readValue(response.getBody(), TabadulResponse.class);
		
		return tabadulResponse.getStatus();
		
	}


	/**
	 * Get all parameters in Map
	 * @param tenderVO
	 * @return
	 */
	private MultiValueMap <String, Object> getExtendTenderDatesParams (TenderVO tenderVO) throws Exception{
		
		MultiValueMap<String, Object> map= new LinkedMultiValueMap<String, Object>();
		TenderDatesVO tenderDatesVO = tenderVO.getTenderDates();
		map.add(TabadulParametersE.TENDER_ID.getParameterName(), String.valueOf(tenderVO.getTenderInternalId()));
		map.add(TabadulParametersE.FAQ_DELIVERY_DATE_HIJRI.getParameterName(),  convertHijriStringFormat(tenderDatesVO.getFaqDeliveryDateHijri()));
		map.add(TabadulParametersE.FAQ_DELIVERY_DATE.getParameterName(),convertGregorianStringFormat(tenderDatesVO.getFaqDeliveryDateGregorian()));
		map.add(TabadulParametersE.OFFER_DELIVERY_DATE_HIJRI.getParameterName(), convertHijriStringFormat(tenderDatesVO.getOfferDeliveryDateHijri()));
		map.add(TabadulParametersE.OFFER_DELIVERY_DATE.getParameterName(), convertGregorianStringFormat(tenderDatesVO.getOfferDeliveryDateGregorian()));
		map.add(TabadulParametersE.OFFER_DELIVERY_TIME_HOUR.getParameterName(), tenderDatesVO.getOfferDeliveryHour());
		map.add(TabadulParametersE.OFFER_DELIVERY_TIME_MINUTE.getParameterName(),tenderDatesVO.getOfferDeliveryMinute());
		map.add(TabadulParametersE.OPEN_ENV_DATE_HIJRI.getParameterName(), convertHijriStringFormat(tenderDatesVO.getOpenEnvelopeDateHijri()));
		map.add(TabadulParametersE.OPEN_ENV_DATE.getParameterName(), convertGregorianStringFormat(tenderDatesVO.getOpenEnvelopeDateGregorian()));
		map.add(TabadulParametersE.OPEN_ENV_TIME_HOUR.getParameterName(), tenderDatesVO.getOpenEnvelopeHour());
		map.add(TabadulParametersE.OPEN_ENV_TIME_MINUTE.getParameterName(), tenderDatesVO.getOpenEnvelopeMinute());
		
		
		
		return map;
	}
	/**
	 * Converts to tabadul extend date format
	 * @param date
	 * @return
	 */
	private String convertHijriStringFormat (String date) {
		
		String [] dateSplit = date.split("/");
		
		return dateSplit [2] + "-" + dateSplit [1] + "-" +dateSplit [0]; 
	}
	
	/**
	 * Converts to tabadul extend date format
	 * @param date
	 * @return
	 */
	private String convertGregorianStringFormat (String date) {
		
		date = date.replace("/", "-");
		
		return date ;
	}

	/**
	 * Getter for Properties Object
	 * @return
	 */
	public Properties getPoolPropertiesConfig() {
		if (null == poolPropertiesConfig)
			return  OBPropertiesProvider.getInstance().getOpenbravoProperties();
		else
			return poolPropertiesConfig;
	}
	
	public TabadulAuthenticationService getTabadulAuthenticationService() {
		return tabadulAuthenticationService;
	}

	public void setTabadulAuthenticationService(
			TabadulAuthenticationService tabadulAuthenticationService) {
		this.tabadulAuthenticationService = tabadulAuthenticationService;
	}
	
	public static void main (String [] args) throws Exception{
		
		TabadulIntegrationServiceImpl tabadulIntegrationServiceImpl = new TabadulIntegrationServiceImpl();
		TabadulAuthenticationService tabadulAuthenticationService = new TabadulAuthenticationServiceImpl();
		tabadulIntegrationServiceImpl.setTabadulAuthenticationService(tabadulAuthenticationService);
		String sessionToken = tabadulAuthenticationService.authenticate("de@mot.gov.sa","123456");
		tabadulIntegrationServiceImpl.getSupplierByCR("1010104371", sessionToken);
		
		//String sessionToken = tabadulAu
		
//		FileSystemResource fileSystemResource = new FileSystemResource(new File ("c:/temp/test.pdf"));
//		
//		tabadulIntegrationServiceImpl.uploadTenderFile("2916", "attachment", fileSystemResource, sessionToken);
		
	//	SupplierVO supplierVO = tabadulIntegrationServiceImpl.getSupplierByCR ("1010254233",sessionToken);
		
		
	}

	@Override
	public CountryLookupResponse getCountries(String sessionToken) throws Exception {
		CountryLookupResponse countryLookupResponse = null;
		String updateUrl = getPoolPropertiesConfig().getProperty(URL_CONFIG) + COUNTRY_LOOKUP_METHOD_URL;
		
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new TabadulResponseErrorHandler());
		HttpHeaders headers = createHeaders(getPoolPropertiesConfig().getProperty(BASIC_AUTH_USERNAME_CONFIG),
				getPoolPropertiesConfig().getProperty(BASIC_AUTH_PWD_CONFIG));
		headers.add (COOKIE_PARAM,sessionToken);
		
		MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
		
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
		
		ResponseEntity<String> response = restTemplate.postForEntity( updateUrl, request , String.class );
		
		ObjectMapper mapper = new ObjectMapper();

		if (TabadulRestUtil.isError(response.getStatusCode())) {
			countryLookupResponse = mapper.readValue(response.getBody(), CountryLookupResponse.class);
		}else {
			CountriesVO countries = mapper.readValue(response.getBody(), CountriesVO.class);
			countryLookupResponse = new CountryLookupResponse();
			countryLookupResponse.setCountries(countries);
		}
		
		return countryLookupResponse;
	}

}
