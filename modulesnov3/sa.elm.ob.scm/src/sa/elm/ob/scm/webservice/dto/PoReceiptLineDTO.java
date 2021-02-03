package sa.elm.ob.scm.webservice.dto;

import java.math.BigDecimal;
import java.util.List;

public class PoReceiptLineDTO {

  private String poLineId;
  private String itemNo;
  private String itemDescription;
  private BigDecimal unitPrice;
  private BigDecimal amountOrdered;
  private BigDecimal amountPending;
  private BigDecimal amountReleased;
  private BigDecimal amountToRelease;
  private BigDecimal qtyOrdered;
  private BigDecimal qtyReleased;
  private BigDecimal qtyPending;
  private BigDecimal qtyToRelease;

  private List<RDVPenaltyDTO> penaltyDTO;
  private List<RDVHoldDTO> holdDTO;

  public String getPoLineId() {
    return poLineId;
  }

  public void setPoLineId(String poLineId) {
    this.poLineId = poLineId;
  }

  public String getItemNo() {
    return itemNo;
  }

  public void setItemNo(String itemNo) {
    this.itemNo = itemNo;
  }

  public String getItemDescription() {
    return itemDescription;
  }

  public void setItemDescription(String itemDescription) {
    this.itemDescription = itemDescription;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(BigDecimal unitPrice) {
    this.unitPrice = unitPrice;
  }

  public BigDecimal getAmountOrdered() {
    return amountOrdered;
  }

  public void setAmountOrdered(BigDecimal amountOrdered) {
    this.amountOrdered = amountOrdered;
  }

  public BigDecimal getAmountPending() {
    return amountPending;
  }

  public void setAmountPending(BigDecimal amountPending) {
    this.amountPending = amountPending;
  }

  public BigDecimal getAmountReleased() {
    return amountReleased;
  }

  public void setAmountReleased(BigDecimal amountReleased) {
    this.amountReleased = amountReleased;
  }

  public BigDecimal getAmountToRelease() {
    return amountToRelease;
  }

  public void setAmountToRelease(BigDecimal amountToRelease) {
    this.amountToRelease = amountToRelease;
  }

  public BigDecimal getQtyOrdered() {
    return qtyOrdered;
  }

  public void setQtyOrdered(BigDecimal qtyOrdered) {
    this.qtyOrdered = qtyOrdered;
  }

  public BigDecimal getQtyReleased() {
    return qtyReleased;
  }

  public void setQtyReleased(BigDecimal qtyReleased) {
    this.qtyReleased = qtyReleased;
  }

  public BigDecimal getQtyPending() {
    return qtyPending;
  }

  public void setQtyPending(BigDecimal qtyPending) {
    this.qtyPending = qtyPending;
  }

  public BigDecimal getQtyToRelease() {
    return qtyToRelease;
  }

  public void setQtyToRelease(BigDecimal qtyToRelease) {
    this.qtyToRelease = qtyToRelease;
  }

  public List<RDVPenaltyDTO> getPenaltyDTO() {
    return penaltyDTO;
  }

  public void setPenaltyDTO(List<RDVPenaltyDTO> penaltyDTO) {
    this.penaltyDTO = penaltyDTO;
  }

  public List<RDVHoldDTO> getHoldDTO() {
    return holdDTO;
  }

  public void setHoldDTO(List<RDVHoldDTO> holdDTO) {
    this.holdDTO = holdDTO;
  }

}
