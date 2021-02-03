package sa.elm.ob.scm.webservice.dto;

import java.util.List;

public class PoReceiptHeaderDTO {

  private String requestNo;
  private String orderId;
  private String poContractNo;
  private String legacyContractNo;
  private String receiveType;
  private String receiveDate;
  private String receivedBy;

  private List<RDVHoldDTO> bulkHoldDTO;
  private List<PoReceiptLineDTO> lineDTO;
  private List<RDVPenaltyDTO> bulkPenaltyDTO;

  public String getPoContractNo() {
    return poContractNo;
  }

  public void setPoContractNo(String poContractNo) {
    this.poContractNo = poContractNo;
  }

  public String getReceiveType() {
    return receiveType;
  }

  public void setReceiveType(String receiveType) {
    this.receiveType = receiveType;
  }

  public String getReceiveDate() {
    return receiveDate;
  }

  public void setReceiveDate(String receiveDate) {
    this.receiveDate = receiveDate;
  }

  public String getReceivedBy() {
    return receivedBy;
  }

  public void setReceivedBy(String receivedBy) {
    this.receivedBy = receivedBy;
  }

  public List<PoReceiptLineDTO> getLineDTO() {
    return lineDTO;
  }

  public void setLineDTO(List<PoReceiptLineDTO> linesList) {
    this.lineDTO = linesList;
  }

  public List<RDVPenaltyDTO> getBulkPenaltyDTO() {
    return bulkPenaltyDTO;
  }

  public void setBulkPenaltyDTO(List<RDVPenaltyDTO> bulkPenaltyList) {
    this.bulkPenaltyDTO = bulkPenaltyList;
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  public String getRequestNo() {
    return requestNo;
  }

  public void setRequestNo(String requestNo) {
    this.requestNo = requestNo;
  }

  public List<RDVHoldDTO> getBulkHoldDTO() {
    return bulkHoldDTO;
  }

  public void setBulkHoldDTO(List<RDVHoldDTO> bulkHoldDTO) {
    this.bulkHoldDTO = bulkHoldDTO;
  }

  public String getLegacyContractNo() {
    return legacyContractNo;
  }

  public void setLegacyContractNo(String legacyContractNo) {
    this.legacyContractNo = legacyContractNo;
  }
}
