package sa.elm.ob.scm.webservice.dto;

import java.math.BigDecimal;

public class RDVPenaltyActionDTO {

  private String penaltyActionID;
  private String penaltyHeaderID;
  private String txnAppNo;
  private BigDecimal matchAmount;
  private String penaltyTypeID;
  private BigDecimal penaltyPercentage;
  private BigDecimal penaltyAmt;
  private String bpName;
  private BigDecimal amarsarfAmt;
  private String penaltyAcctType;
  private String penaltyUniqueCode;
  private String rdvTxnLineID;
  private String bpID;
  private String invoiceID;
  private String penaltyID;
  private Boolean isReleased;
  private String penaltyRelID;
  private BigDecimal releasedAmt;
  private BigDecimal enteredAmt;

  public String getPenaltyActionID() {
    return penaltyActionID;
  }

  public void setPenaltyActionID(String penaltyActionID) {
    this.penaltyActionID = penaltyActionID;
  }

  public String getPenaltyHeaderID() {
    return penaltyHeaderID;
  }

  public void setPenaltyHeaderID(String penaltyHeaderID) {
    this.penaltyHeaderID = penaltyHeaderID;
  }

  public String getTxnAppNo() {
    return txnAppNo;
  }

  public void setTxnAppNo(String txnAppNo) {
    this.txnAppNo = txnAppNo;
  }

  public BigDecimal getMatchAmount() {
    return matchAmount;
  }

  public void setMatchAmount(BigDecimal matchAmount) {
    this.matchAmount = matchAmount;
  }

  public String getPenaltyTypeID() {
    return penaltyTypeID;
  }

  public void setPenaltyTypeID(String penaltyTypeID) {
    this.penaltyTypeID = penaltyTypeID;
  }

  public BigDecimal getPenaltyPercentage() {
    return penaltyPercentage;
  }

  public void setPenaltyPercentage(BigDecimal penaltyPercentage) {
    this.penaltyPercentage = penaltyPercentage;
  }

  public BigDecimal getPenaltyAmt() {
    return penaltyAmt;
  }

  public void setPenaltyAmt(BigDecimal penaltyAmt) {
    this.penaltyAmt = penaltyAmt;
  }

  public String getBpName() {
    return bpName;
  }

  public void setBpName(String bpName) {
    this.bpName = bpName;
  }

  public BigDecimal getAmarsarfAmt() {
    return amarsarfAmt;
  }

  public void setAmarsarfAmt(BigDecimal amarsarfAmt) {
    this.amarsarfAmt = amarsarfAmt;
  }

  public String getPenaltyAcctType() {
    return penaltyAcctType;
  }

  public void setPenaltyAcctType(String penaltyAcctType) {
    this.penaltyAcctType = penaltyAcctType;
  }

  public String getPenaltyUniqueCode() {
    return penaltyUniqueCode;
  }

  public void setPenaltyUniqueCode(String penaltyUniqueCode) {
    this.penaltyUniqueCode = penaltyUniqueCode;
  }

  public String getRdvTxnLineID() {
    return rdvTxnLineID;
  }

  public void setRdvTxnLineID(String rdvTxnLineID) {
    this.rdvTxnLineID = rdvTxnLineID;
  }

  public String getBpID() {
    return bpID;
  }

  public void setBpID(String bpID) {
    this.bpID = bpID;
  }

  public String getInvoiceID() {
    return invoiceID;
  }

  public void setInvoiceID(String invoiceID) {
    this.invoiceID = invoiceID;
  }

  public String getPenaltyID() {
    return penaltyID;
  }

  public void setPenaltyID(String penaltyID) {
    this.penaltyID = penaltyID;
  }

  public Boolean getIsReleased() {
    return isReleased;
  }

  public void setIsReleased(Boolean isReleased) {
    this.isReleased = isReleased;
  }

  public String getPenaltyRelID() {
    return penaltyRelID;
  }

  public void setPenaltyRelID(String penaltyRelID) {
    this.penaltyRelID = penaltyRelID;
  }

  public BigDecimal getReleasedAmt() {
    return releasedAmt;
  }

  public void setReleasedAmt(BigDecimal releasedAmt) {
    this.releasedAmt = releasedAmt;
  }

  public BigDecimal getEnteredAmt() {
    return enteredAmt;
  }

  public void setEnteredAmt(BigDecimal enteredAmt) {
    this.enteredAmt = enteredAmt;
  }

}