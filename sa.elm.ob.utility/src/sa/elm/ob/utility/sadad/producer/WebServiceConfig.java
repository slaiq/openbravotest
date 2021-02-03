package sa.elm.ob.utility.sadad.producer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.wsdl.wsdl11.SimpleWsdl11Definition;

/**
 * Web Services Configuration
 * @author mrahim
 *
 */
@Configuration
@EnableWs
public class WebServiceConfig {
	
	private static final String WSDL_LOCATION = "/sa/elm/ob/utility/sadad/producer/create_bill.wsdl" ;
	
	/**
	 * Creates the WSDL from the given XSD Schema
	 * @param sadadSchema
	 * @return
	 */
	@Bean(name = "sadad")
	public SimpleWsdl11Definition defaultWsdl11Definition() {
		SimpleWsdl11Definition wsdl11Definition = new SimpleWsdl11Definition();
	    wsdl11Definition.setWsdl(new ClassPathResource(WSDL_LOCATION));

		return wsdl11Definition;
	}

}
