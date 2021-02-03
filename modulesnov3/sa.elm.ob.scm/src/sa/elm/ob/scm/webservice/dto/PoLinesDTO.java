package sa.elm.ob.scm.webservice.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class PoLinesDTO implements Serializable {

  private static final long serialVersionUID = 176143006788366002L;

  private String lineId;
  private String item;
  private String parentItem;
  private String description;
  private String UOM;
  private BigDecimal unitPrice;
  private BigDecimal qtyOrdered;
  private BigDecimal qtyReleaseed;
  private BigDecimal qtyPending;
  private BigDecimal amountReleased;
  private BigDecimal amountPending;

  public String getItem() {
    return item;
  }

  public void setItem(String item) {
    this.item = item;
  }

  public String getParentItem() {
    return parentItem;
  }

  public void setParentItem(String parentItem) {
    this.parentItem = parentItem;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getUOM() {
    return UOM;
  }

  public void setUOM(String uOM) {
    UOM = uOM;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(BigDecimal unitPrice) {
    this.unitPrice = unitPrice;
  }

  public BigDecimal getQtyOrdered() {
    return qtyOrdered;
  }

  public void setQtyOrdered(BigDecimal qtyOrdered) {
    this.qtyOrdered = qtyOrdered;
  }

  public BigDecimal getQtyReleaseed() {
    return qtyReleaseed;
  }

  public void setQtyReleaseed(BigDecimal qtyReleaseed) {
    this.qtyReleaseed = qtyReleaseed;
  }

  public BigDecimal getQtyPending() {
    return qtyPending;
  }

  public void setQtyPending(BigDecimal qtyPending) {
    this.qtyPending = qtyPending;
  }

  public BigDecimal getAmountReleased() {
    return amountReleased;
  }

  public void setAmountReleased(BigDecimal amountReleased) {
    this.amountReleased = amountReleased;
  }

  public BigDecimal getAmountPending() {
    return amountPending;
  }

  public void setAmountPending(BigDecimal amountPending) {
    this.amountPending = amountPending;
  }

  public String getLineId() {
    return lineId;
  }

  public void setLineId(String lineId) {
    this.lineId = lineId;
  }

}
