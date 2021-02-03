package sa.elm.ob.scm.webservice.dto;

import java.math.BigDecimal;

public class RDVHoldDTO {

  private String holdcode;
  private BigDecimal holdAmount;
  private String bpartnerId;
  private String actionReason;
  private String actionJustification;
  private String itemNo;

  public BigDecimal getHoldAmount() {
    return holdAmount;
  }

  public void setHoldAmount(BigDecimal holdAmount) {
    this.holdAmount = holdAmount;
  }

  public String getBpartnerId() {
    return bpartnerId;
  }

  public void setBpartnerId(String bpartnerId) {
    this.bpartnerId = bpartnerId;
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

  public String getHoldcode() {
    return holdcode;
  }

  public void setHoldcode(String holdcode) {
    this.holdcode = holdcode;
  }

  public String getItemNo() {
    return itemNo;
  }

  public void setItemNo(String itemNo) {
    this.itemNo = itemNo;
  }

}
