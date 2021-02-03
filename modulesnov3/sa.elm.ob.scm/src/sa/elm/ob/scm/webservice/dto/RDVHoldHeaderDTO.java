package sa.elm.ob.scm.webservice.dto;

import java.math.BigDecimal;

public class RDVHoldHeaderDTO {

  private String holdHeaderID;
  private Long txnVersion;
  private Long lineNo;
  private BigDecimal existingHold;
  private BigDecimal holdAmt;
  private BigDecimal updatedHoldAmt;
  private String rdvTxnID;
  private String holdType;
  private String rdvTxnLineID;

  public String getHoldHeaderID() {
    return holdHeaderID;
  }

  public void setHoldHeaderID(String holdHeaderID) {
    this.holdHeaderID = holdHeaderID;
  }

  public Long getTxnVersion() {
    return txnVersion;
  }

  public void setTxnVersion(Long txnVersion) {
    this.txnVersion = txnVersion;
  }

  public Long getLineNo() {
    return lineNo;
  }

  public void setLineNo(Long lineNo) {
    this.lineNo = lineNo;
  }

  public BigDecimal getExistingHold() {
    return existingHold;
  }

  public void setExistingHold(BigDecimal existingHold) {
    this.existingHold = existingHold;
  }

  public BigDecimal getHoldAmt() {
    return holdAmt;
  }

  public void setHoldAmt(BigDecimal holdAmt) {
    this.holdAmt = holdAmt;
  }

  public BigDecimal getUpdatedHoldAmt() {
    return updatedHoldAmt;
  }

  public void setUpdatedHoldAmt(BigDecimal updatedHoldAmt) {
    this.updatedHoldAmt = updatedHoldAmt;
  }

  public String getRdvTxnID() {
    return rdvTxnID;
  }

  public void setRdvTxnID(String rdvTxnID) {
    this.rdvTxnID = rdvTxnID;
  }

  public String getHoldType() {
    return holdType;
  }

  public void setHoldType(String holdType) {
    this.holdType = holdType;
  }

  public String getRdvTxnLineID() {
    return rdvTxnLineID;
  }

  public void setRdvTxnLineID(String rdvTxnLineID) {
    this.rdvTxnLineID = rdvTxnLineID;
  }

}