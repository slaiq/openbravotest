package sa.elm.ob.scm.webservice.dto;

import java.util.List;

public class PoReceiptHeaderResponseDTO {

  private String receiptId;
  private String specNo;
  private String transactionType;
  private String transactionDate;
  private String supplierName;
  private String documentNo;
  private String documentDate;
  private String warehouse;
  private String receivetype;
  private String site;

  private List<PoReceiptLinesResponseDTO> lineDTO;

  public String getReceiptId() {
    return receiptId;
  }

  public void setReceiptId(String receiptId) {
    this.receiptId = receiptId;
  }

  public String getSpecNo() {
    return specNo;
  }

  public void setSpecNo(String specNo) {
    this.specNo = specNo;
  }

  public String getTransactionType() {
    return transactionType;
  }

  public void setTransactionType(String transactionType) {
    this.transactionType = transactionType;
  }

  public String getTransactionDate() {
    return transactionDate;
  }

  public void setTransactionDate(String transactionDate) {
    this.transactionDate = transactionDate;
  }

  public String getSupplierName() {
    return supplierName;
  }

  public void setSupplierName(String supplierName) {
    this.supplierName = supplierName;
  }

  public String getDocumentNo() {
    return documentNo;
  }

  public void setDocumentNo(String documentNo) {
    this.documentNo = documentNo;
  }

  public String getDocumentDate() {
    return documentDate;
  }

  public void setDocumentDate(String documentDate) {
    this.documentDate = documentDate;
  }

  public String getWarehouse() {
    return warehouse;
  }

  public void setWarehouse(String warehouse) {
    this.warehouse = warehouse;
  }

  public String getReceivetype() {
    return receivetype;
  }

  public void setReceivetype(String receivetype) {
    this.receivetype = receivetype;
  }

  public String getSite() {
    return site;
  }

  public void setSite(String site) {
    this.site = site;
  }

  public List<PoReceiptLinesResponseDTO> getLineDTO() {
    return lineDTO;
  }

  public void setLineDTO(List<PoReceiptLinesResponseDTO> lineDTO) {
    this.lineDTO = lineDTO;
  }

}
