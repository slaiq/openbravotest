package sa.elm.ob.scm.webservice.dto;

import java.math.BigDecimal;
import java.util.List;

public class RDVTxnLineResponseDTO {

  private String productCategory;
  private String matchDate;
  private String applicationNo;
  private String item;
  private String itemDescription;
  private String uom;
  private String uniquecode;
  private String uniquecodeName;
  private String advanceMethod;
  private boolean isadvance;
  private BigDecimal existingMatchedQty;
  private BigDecimal existingMatchedAmt;
  private BigDecimal matchedQty;
  private BigDecimal matchedAmt;
  private BigDecimal unitPrice;
  private BigDecimal shippedDeliveredQty;
  private BigDecimal deliverAmt;
  private BigDecimal completionPercentage;
  private BigDecimal existingPenalty;
  private BigDecimal penaltyAmt;
  private BigDecimal advanceAmountRem;
  private BigDecimal existingAdvDeduction;
  private BigDecimal advanceDeduction;
  private BigDecimal holdAmt;
  private BigDecimal totalDeduction;
  private BigDecimal netMatchAmt;
  private BigDecimal existingHoldAmt;
  private BigDecimal taxAmt;

  private List<RDVPenaltyActionDTO> penaltyActionDTO;
  private List<RDVHoldActionDTO> holdActionDTO;

  public String getProductCategory() {
    return productCategory;
  }

  public void setProductCategory(String productCategory) {
    this.productCategory = productCategory;
  }

  public String getMatchDate() {
    return matchDate;
  }

  public void setMatchDate(String matchDate) {
    this.matchDate = matchDate;
  }

  public String getApplicationNo() {
    return applicationNo;
  }

  public void setApplicationNo(String applicationNo) {
    this.applicationNo = applicationNo;
  }

  public String getItem() {
    return item;
  }

  public void setItem(String item) {
    this.item = item;
  }

  public String getItemDescription() {
    return itemDescription;
  }

  public void setItemDescription(String itemDescription) {
    this.itemDescription = itemDescription;
  }

  public String getUom() {
    return uom;
  }

  public void setUom(String uom) {
    this.uom = uom;
  }

  public String getUniquecode() {
    return uniquecode;
  }

  public void setUniquecode(String uniquecode) {
    this.uniquecode = uniquecode;
  }

  public String getUniquecodeName() {
    return uniquecodeName;
  }

  public void setUniquecodeName(String uniquecodeName) {
    this.uniquecodeName = uniquecodeName;
  }

  public String getAdvanceMethod() {
    return advanceMethod;
  }

  public void setAdvanceMethod(String advanceMethod) {
    this.advanceMethod = advanceMethod;
  }

  public boolean isIsadvance() {
    return isadvance;
  }

  public void setIsadvance(boolean isadvance) {
    this.isadvance = isadvance;
  }

  public BigDecimal getExistingMatchedQty() {
    return existingMatchedQty;
  }

  public void setExistingMatchedQty(BigDecimal existingMatchedQty) {
    this.existingMatchedQty = existingMatchedQty;
  }

  public BigDecimal getExistingMatchedAmt() {
    return existingMatchedAmt;
  }

  public void setExistingMatchedAmt(BigDecimal existingMatchedAmt) {
    this.existingMatchedAmt = existingMatchedAmt;
  }

  public BigDecimal getMatchedQty() {
    return matchedQty;
  }

  public void setMatchedQty(BigDecimal matchedQty) {
    this.matchedQty = matchedQty;
  }

  public BigDecimal getMatchedAmt() {
    return matchedAmt;
  }

  public void setMatchedAmt(BigDecimal matchedAmt) {
    this.matchedAmt = matchedAmt;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(BigDecimal unitPrice) {
    this.unitPrice = unitPrice;
  }

  public BigDecimal getShippedDeliveredQty() {
    return shippedDeliveredQty;
  }

  public void setShippedDeliveredQty(BigDecimal shippedDeliveredQty) {
    this.shippedDeliveredQty = shippedDeliveredQty;
  }

  public BigDecimal getDeliverAmt() {
    return deliverAmt;
  }

  public void setDeliverAmt(BigDecimal deliverAmt) {
    this.deliverAmt = deliverAmt;
  }

  public BigDecimal getCompletionPercentage() {
    return completionPercentage;
  }

  public void setCompletionPercentage(BigDecimal completionPercentage) {
    this.completionPercentage = completionPercentage;
  }

  public BigDecimal getExistingPenalty() {
    return existingPenalty;
  }

  public void setExistingPenalty(BigDecimal existingPenalty) {
    this.existingPenalty = existingPenalty;
  }

  public BigDecimal getPenaltyAmt() {
    return penaltyAmt;
  }

  public void setPenaltyAmt(BigDecimal penaltyAmt) {
    this.penaltyAmt = penaltyAmt;
  }

  public BigDecimal getAdvanceAmountRem() {
    return advanceAmountRem;
  }

  public void setAdvanceAmountRem(BigDecimal advanceAmountRem) {
    this.advanceAmountRem = advanceAmountRem;
  }

  public BigDecimal getExistingAdvDeduction() {
    return existingAdvDeduction;
  }

  public void setExistingAdvDeduction(BigDecimal existingAdvDeduction) {
    this.existingAdvDeduction = existingAdvDeduction;
  }

  public BigDecimal getAdvanceDeduction() {
    return advanceDeduction;
  }

  public void setAdvanceDeduction(BigDecimal advanceDeduction) {
    this.advanceDeduction = advanceDeduction;
  }

  public BigDecimal getHoldAmt() {
    return holdAmt;
  }

  public void setHoldAmt(BigDecimal holdAmt) {
    this.holdAmt = holdAmt;
  }

  public BigDecimal getTotalDeduction() {
    return totalDeduction;
  }

  public void setTotalDeduction(BigDecimal totalDeduction) {
    this.totalDeduction = totalDeduction;
  }

  public BigDecimal getNetMatchAmt() {
    return netMatchAmt;
  }

  public void setNetMatchAmt(BigDecimal netMatchAmt) {
    this.netMatchAmt = netMatchAmt;
  }

  public BigDecimal getExistingHoldAmt() {
    return existingHoldAmt;
  }

  public void setExistingHoldAmt(BigDecimal existingHoldAmt) {
    this.existingHoldAmt = existingHoldAmt;
  }

  public BigDecimal getTaxAmt() {
    return taxAmt;
  }

  public void setTaxAmt(BigDecimal taxAmt) {
    this.taxAmt = taxAmt;
  }

  public List<RDVPenaltyActionDTO> getPenaltyActionDTO() {
    return penaltyActionDTO;
  }

  public void setPenaltyActionDTO(List<RDVPenaltyActionDTO> penaltyActionDTO) {
    this.penaltyActionDTO = penaltyActionDTO;
  }

  public List<RDVHoldActionDTO> getHoldActionDTO() {
    return holdActionDTO;
  }

  public void setHoldActionDTO(List<RDVHoldActionDTO> holdActionDTO) {
    this.holdActionDTO = holdActionDTO;
  }
}
