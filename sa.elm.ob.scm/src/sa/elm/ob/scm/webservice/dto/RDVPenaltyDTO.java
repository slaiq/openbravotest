package sa.elm.ob.scm.webservice.dto;

import java.math.BigDecimal;

public class RDVPenaltyDTO {

  private String penaltyId;
  private BigDecimal penaltyAmount;
  private String bpartnerId;
  private String uniqueCode;
  private String actionDate;
  private String actionReason;
  private String actionJustification;

  public String getPenaltyId() {
    return penaltyId;
  }

  public void setPenaltyId(String penaltyId) {
    this.penaltyId = penaltyId;
  }

  public BigDecimal getPenaltyAmount() {
    return penaltyAmount;
  }

  public void setPenaltyAmount(BigDecimal penaltyAmount) {
    this.penaltyAmount = penaltyAmount;
  }

  public String getBpartnerId() {
    return bpartnerId;
  }

  public void setBpartnerId(String bpartnerId) {
    this.bpartnerId = bpartnerId;
  }

  public String getUniqueCode() {
    return uniqueCode;
  }

  public void setUniqueCode(String uniqueCode) {
    this.uniqueCode = uniqueCode;
  }

  public String getActionDate() {
    return actionDate;
  }

  public void setActionDate(String actionDate) {
    this.actionDate = actionDate;
  }

  public String getActionReason() {
    return actionReason;
  }

  public void setActionReason(String actionReason) {
    this.actionReason = actionReason;
  }

  public String getActionJustification() {
    return actionJustification;
  }

  public void setActionJustification(String actionJustification) {
    this.actionJustification = actionJustification;
  }

}
