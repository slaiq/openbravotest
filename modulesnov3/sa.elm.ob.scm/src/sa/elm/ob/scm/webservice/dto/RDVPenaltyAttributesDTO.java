package sa.elm.ob.scm.webservice.dto;

import java.math.BigDecimal;

public class RDVPenaltyAttributesDTO {

  private String penaltyID;
  private BigDecimal penaltyPercentage;
  private BigDecimal openingPenaltyAmt;
  private BigDecimal penaltyApplied;
  private BigDecimal penaltyRemaining;
  private String penaltyStatus;
  private String rdvID;
  private String penaltyType;

  public String getPenaltyID() {
    return penaltyID;
  }

  public void setPenaltyID(String penaltyID) {
    this.penaltyID = penaltyID;
  }

  public BigDecimal getPenaltyPercentage() {
    return penaltyPercentage;
  }

  public void setPenaltyPercentage(BigDecimal penaltyPercentage) {
    this.penaltyPercentage = penaltyPercentage;
  }

  public BigDecimal getOpeningPenaltyAmt() {
    return openingPenaltyAmt;
  }

  public void setOpeningPenaltyAmt(BigDecimal openingPenaltyAmt) {
    this.openingPenaltyAmt = openingPenaltyAmt;
  }

  public BigDecimal getPenaltyApplied() {
    return penaltyApplied;
  }

  public void setPenaltyApplied(BigDecimal penaltyApplied) {
    this.penaltyApplied = penaltyApplied;
  }

  public BigDecimal getPenaltyRemaining() {
    return penaltyRemaining;
  }

  public void setPenaltyRemaining(BigDecimal penaltyRemaining) {
    this.penaltyRemaining = penaltyRemaining;
  }

  public String getPenaltyStatus() {
    return penaltyStatus;
  }

  public void setPenaltyStatus(String penaltyStatus) {
    this.penaltyStatus = penaltyStatus;
  }

  public String getRdvID() {
    return rdvID;
  }

  public void setRdvID(String rdvID) {
    this.rdvID = rdvID;
  }

  public String getPenaltyType() {
    return penaltyType;
  }

  public void setPenaltyType(String penaltyType) {
    this.penaltyType = penaltyType;
  }

}
