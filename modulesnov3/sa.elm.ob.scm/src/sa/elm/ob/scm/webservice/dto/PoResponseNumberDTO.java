
package sa.elm.ob.scm.webservice.dto;

import java.io.Serializable;
import java.util.List;

public class PoResponseNumberDTO implements Serializable {

  private static final long serialVersionUID = 9012063072529621056L;

  private String responseNumber;
  private List<POHeaderDTO> orderDTO;

  public String getResponseNumber() {
    return responseNumber;
  }

  public void setResponseNumber(String requestNumber) {
    this.responseNumber = requestNumber;
  }

  public List<POHeaderDTO> getOrderDTO() {
    return orderDTO;
  }

  public void setOrderDTO(List<POHeaderDTO> headerDTO) {
    this.orderDTO = headerDTO;
  }

}
