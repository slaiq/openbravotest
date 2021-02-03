package sa.elm.ob.utility.tabadul;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;


/**
 * Custom Response Handler to Check Exception 
 * @author mrahim
 *
 */
public class TabadulResponseErrorHandler implements ResponseErrorHandler{
	private static final Logger log = LoggerFactory.getLogger(TabadulResponseErrorHandler.class);

	@Override
    public void handleError(ClientHttpResponse response) throws IOException {
        log.error("Response error: {} {}", response.getStatusCode(), response.getStatusText());
    }

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return TabadulRestUtil.isError(response.getStatusCode());
    }
}
