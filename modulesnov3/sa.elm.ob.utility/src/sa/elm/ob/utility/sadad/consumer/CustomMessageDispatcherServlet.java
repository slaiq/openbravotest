package sa.elm.ob.utility.sadad.consumer;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import org.springframework.ws.transport.http.MessageDispatcherServlet;

/**
 * SOAP Web Service Dispatcher Servlet
 * 
 * @author mrahim
 *
 */
@WebServlet(name = "message-dispatcher", urlPatterns = "/soapws/*", initParams = {
    @WebInitParam(name = "transformWsdlLocations", value = "true") })
public class CustomMessageDispatcherServlet extends MessageDispatcherServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 3539125811810067425L;

}
