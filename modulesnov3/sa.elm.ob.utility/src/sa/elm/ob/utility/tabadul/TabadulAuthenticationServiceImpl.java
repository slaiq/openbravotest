package sa.elm.ob.utility.tabadul;

import java.nio.charset.Charset;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.openbravo.base.session.OBPropertiesProvider;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
/**
 * 
 * @author mrahim
 *
 */
public class TabadulAuthenticationServiceImpl implements TabadulAuthenticationService {
	
	private static final String URL_CONFIG = "tabadul.api.url" ;
	private static final String AUTH_METHOD_URL = "authenticate" ;
	private static final String IS_AUTH_METHOD_URL = "is-authenticated" ;
	private Properties poolPropertiesConfig;
	private static final String BASIC_AUTH_USERNAME_CONFIG = "tabadul.basicauth.username" ;
	private static final String BASIC_AUTH_PWD_CONFIG = "tabadul.basicauth.password" ;
	private static final String SET_COOKIE_NAME = "Set-Cookie" ;
	private static final String COOKIE = "Cookie" ;
	private static final String CHAR_SET = "US-ASCII" ;
	private static final String AUTH_TYPE = "Basic " ;
	private static final String AUTH_HEADER = "Authorization" ;
	private static final String EMAIL_REQ_PARAMETER = "useremail" ;
	private static final String PWD_REQ_PARAMETER = "password" ;
	private static final String SEPARATOR = ":" ;
	private static String USER_CONFIG_PARAM = "tabadul.username" ; 
	private static String PWD_CONFIG_PARAM = "tabadul.password" ;
	 private static String USER_AUDIT_CONFIG_PARAM = "tabadul.audit.username" ; 
	 private static String PWD_AUDIT_CONFIG_PARAM = "tabadul.audit.password" ; 
	
	@Override
	public String authenticate(String userEmail, String password) {
		RestTemplate restTemplate = new RestTemplate();
		
		HttpHeaders headers = createHeaders(getPoolPropertiesConfig()
				.getProperty(BASIC_AUTH_USERNAME_CONFIG),
				getPoolPropertiesConfig().getProperty(BASIC_AUTH_PWD_CONFIG));
		
		MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
		map.add(EMAIL_REQ_PARAMETER, userEmail);
		map.add(PWD_REQ_PARAMETER, password);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

		ResponseEntity<String> response = restTemplate.postForEntity( getPoolPropertiesConfig().getProperty(URL_CONFIG) + AUTH_METHOD_URL, request , String.class );
		HttpHeaders responseHeaders = response.getHeaders();
	    String phpSessionId = responseHeaders.getFirst(SET_COOKIE_NAME);
		
		return phpSessionId;
	}

	@Override
	public String authenticate() {
		String userName = getPoolPropertiesConfig().getProperty(USER_CONFIG_PARAM);
		String password = getPoolPropertiesConfig().getProperty(PWD_CONFIG_PARAM);
		
		
		return authenticate(userName,password);
	}
	
	@Override
	public String authenticateAuditUser() {
		String userName = getPoolPropertiesConfig().getProperty(USER_AUDIT_CONFIG_PARAM);
		String password = getPoolPropertiesConfig().getProperty(PWD_AUDIT_CONFIG_PARAM);
		
		
		return authenticate(userName,password);

	}

	@SuppressWarnings("unused")
	@Override
	public Boolean isTokenValid(String sessionToken) {
		
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = createHeaders(getPoolPropertiesConfig()
				.getProperty(BASIC_AUTH_USERNAME_CONFIG),
				getPoolPropertiesConfig().getProperty(BASIC_AUTH_PWD_CONFIG));

		headers.add (COOKIE,sessionToken);
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(null, headers);

		ResponseEntity<String> response = restTemplate.postForEntity( URL_CONFIG + IS_AUTH_METHOD_URL, request , String.class );
		return null;
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
        
		return httpHeaders;
	}

	public static void main (String [] args){
		TabadulAuthenticationService tabadulAuthenticationService = new TabadulAuthenticationServiceImpl();
		tabadulAuthenticationService.authenticate("de@mot.gov.sa", "123456");
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

}
