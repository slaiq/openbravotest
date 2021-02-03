package sa.elm.ob.scm.webservice.dto;

import java.math.BigDecimal;

public class PoReceiptLinesResponseDTO {

  private String receiptLineId;
  private String item;
  private String description;
  private String uom;
  private BigDecimal quantity;
  private BigDecimal unitprice;
  private BigDecimal receiveAmount;
  private BigDecimal percentageAchieved;
  private BigDecimal totalLineAmount;

  public String getReceiptLineId() {
    return receiptLineId;
  }

  public void setReceiptLineId(String receiptLineId) {
    this.receiptLineId = receiptLineId;
  }

  public String getItem() {
    return item;
  }

  public void setItem(String item) {
    this.item = item;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getUom() {
    return uom;
  }

  public void setUom(String uom) {
    this.uom = uom;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getUnitprice() {
    return unitprice;
  }

  public void setUnitprice(BigDecimal unitprice) {
    this.unitprice = unitprice;
  }

  public BigDecimal getReceiveAmount() {
    return receiveAmount;
  }

  public void setReceiveAmount(BigDecimal receiveAmount) {
    this.receiveAmount = receiveAmount;
  }

  public BigDecimal getPercentageAchieved() {
    return percentageAchieved;
  }

  public void setPercentageAchieved(BigDecimal percentageAchieved) {
    this.percentageAchieved = percentageAchieved;
  }

  public BigDecimal getTotalLineAmount() {
    return totalLineAmount;
  }

  public void setTotalLineAmount(BigDecimal totalLineAmount) {
    this.totalLineAmount = totalLineAmount;
  }

}
