package sa.elm.ob.scm.webservice.dto;

import java.math.BigDecimal;

public class RDVPenaltyHeaderDTO {

  private String penaltyHeaderID;
  private Long txnVersion;
  private Long lineNo;
  private BigDecimal existingPenalty;
  private BigDecimal penaltyAmt;
  private BigDecimal updatedPenaltyAmt;
  private String rdvTxnID;
  private String penaltyType;
  private String rdvTxnLineID;

  public String getPenaltyHeaderID() {
    return penaltyHeaderID;
  }

  public void setPenaltyHeaderID(String penaltyHeaderID) {
    this.penaltyHeaderID = penaltyHeaderID;
  }

  public Long getTxnVersion() {
    return txnVersion;
  }

  public void setTxnVersion(Long long1) {
    this.txnVersion = long1;
  }

  public Long getLineNo() {
    return lineNo;
  }

  public void setLineNo(Long long1) {
    this.lineNo = long1;
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

  public BigDecimal getUpdatedPenaltyAmt() {
    return updatedPenaltyAmt;
  }

  public void setUpdatedPenaltyAmt(BigDecimal updatedPenaltyAmt) {
    this.updatedPenaltyAmt = updatedPenaltyAmt;
  }

  public String getRdvTxnID() {
    return rdvTxnID;
  }

  public void setRdvTxnID(String rdvTxnID) {
    this.rdvTxnID = rdvTxnID;
  }

  public String getPenaltyType() {
    return penaltyType;
  }

  public void setPenaltyType(String penaltyType) {
    this.penaltyType = penaltyType;
  }

  public String getRdvTxnLineID() {
    return rdvTxnLineID;
  }

  public void setRdvTxnLineID(String rdvTxnLineID) {
    this.rdvTxnLineID = rdvTxnLineID;
  }

}