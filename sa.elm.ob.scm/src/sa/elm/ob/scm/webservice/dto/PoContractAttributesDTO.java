package sa.elm.ob.scm.webservice.dto;

import java.math.BigDecimal;

public class PoContractAttributesDTO {
  
  private String contractStartDate;
  private String contractEndDate;
  private String contractOnBoardDate;
  private BigDecimal advancePercentage;
  private BigDecimal advanceAmount;

  
  public String getContractStartDate() {
    return contractStartDate;
  }
  public void setContractStartDate(String contractStartDate) {
    this.contractStartDate = contractStartDate;
  }
  public String getContractEndDate() {
    return contractEndDate;
  }
  public void setContractEndDate(String contractEndDate) {
    this.contractEndDate = contractEndDate;
  }
  public String getContractOnBoardDate() {
    return contractOnBoardDate;
  }
  public void setContractOnBoardDate(String contractOnBoardDate) {
    this.contractOnBoardDate = contractOnBoardDate;
  }
  public BigDecimal getAdvancePercentage() {
    return advancePercentage;
  }
  public void setAdvancePercentage(BigDecimal advancePercentage) {
    this.advancePercentage = advancePercentage;
  }
  public BigDecimal getAdvanceAmount() {
    return advanceAmount;
  }
  public void setAdvanceAmount(BigDecimal advanceAmount) {
    this.advanceAmount = advanceAmount;
  }

}
