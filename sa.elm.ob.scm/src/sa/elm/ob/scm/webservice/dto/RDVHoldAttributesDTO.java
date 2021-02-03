package sa.elm.ob.scm.webservice.dto;

import java.math.BigDecimal;

public class RDVHoldAttributesDTO {

  private String holdID;
  private BigDecimal holdPercentage;
  private BigDecimal openingHoldAmt;
  private BigDecimal holdApplied;
  private BigDecimal holdRemaining;
  private String holdStatus;
  private String rdvID;
  private String holdType;

  public String getHoldID() {
    return holdID;
  }

  public void setHoldID(String holdID) {
    this.holdID = holdID;
  }

  public BigDecimal getHoldPercentage() {
    return holdPercentage;
  }

  public void setHoldPercentage(BigDecimal holdPercentage) {
    this.holdPercentage = holdPercentage;
  }

  public BigDecimal getOpeningHoldAmt() {
    return openingHoldAmt;
  }

  public void setOpeningHoldAmt(BigDecimal openingHoldAmt) {
    this.openingHoldAmt = openingHoldAmt;
  }

  public BigDecimal getHoldApplied() {
    return holdApplied;
  }

  public void setHoldApplied(BigDecimal holdApplied) {
    this.holdApplied = holdApplied;
  }

  public BigDecimal getHoldRemaining() {
    return holdRemaining;
  }

  public void setHoldRemaining(BigDecimal holdRemaining) {
    this.holdRemaining = holdRemaining;
  }

  public String getHoldStatus() {
    return holdStatus;
  }

  public void setHoldStatus(String holdStatus) {
    this.holdStatus = holdStatus;
  }

  public String getRdvID() {
    return rdvID;
  }

  public void setRdvID(String rdvID) {
    this.rdvID = rdvID;
  }

  public String getHoldType() {
    return holdType;
  }

  public void setHoldType(String holdType) {
    this.holdType = holdType;
  }

}
