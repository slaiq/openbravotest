package sa.elm.ob.scm.webservice.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public class POHeaderDTO implements Serializable {

  private static final long serialVersionUID = 2822027778469212145L;

  private String orderId;
  private String contractNo;
  private String contractDate;
  private String contractType;
  private String receiveType;
  private String supplierName;
  private BigDecimal grossPoAmount;
  private BigDecimal netPoAmount;
  private List<PoLinesDTO> poLines;

  private PoContractAttributesDTO contractAttribute;
  private List<PoReceiptHeaderResponseDTO> poReceipt;
  private List<RDVHeaderResponseDTO> rdv;

  public String getContractNo() {
    return contractNo;
  }

  public void setContractNo(String contractNo) {
    this.contractNo = contractNo;
  }

  public String getContractDate() {
    return contractDate;
  }

  public void setContractDate(String contractDate) {
    this.contractDate = contractDate;
  }

  public String getReceiveType() {
    return receiveType;
  }

  public void setReceiveType(String receiveType) {
    this.receiveType = receiveType;
  }

  public String getContractType() {
    return contractType;
  }

  public void setContractType(String contractType) {
    this.contractType = contractType;
  }

  public String getSupplierName() {
    return supplierName;
  }

  public void setSupplierName(String supplierName) {
    this.supplierName = supplierName;
  }

  public BigDecimal getGrossPoAmount() {
    return grossPoAmount;
  }

  public void setGrossPoAmount(BigDecimal grossPoAmount) {
    this.grossPoAmount = grossPoAmount;
  }

  public BigDecimal getNetPoAmount() {
    return netPoAmount;
  }

  public void setNetPoAmount(BigDecimal netPoAmount) {
    this.netPoAmount = netPoAmount;
  }

  public List<PoLinesDTO> getPoLines() {
    return poLines;
  }

  public void setPoLines(List<PoLinesDTO> poLines) {
    this.poLines = poLines;
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  public PoContractAttributesDTO getContractAttribute() {
    return contractAttribute;
  }

  public void setContractAttribute(PoContractAttributesDTO contractAttribute) {
    this.contractAttribute = contractAttribute;
  }

  public List<PoReceiptHeaderResponseDTO> getPoReceipt() {
    return poReceipt;
  }

  public void setPoReceipt(List<PoReceiptHeaderResponseDTO> poReceipt) {
    this.poReceipt = poReceipt;
  }

  public List<RDVHeaderResponseDTO> getRdv() {
    return rdv;
  }

  public void setRdv(List<RDVHeaderResponseDTO> rdv) {
    this.rdv = rdv;
  }

}
