package sa.elm.ob.scm.webservice.dto;

import java.math.BigDecimal;

public class RDVHoldActionDTO {

  private String holdActionID;
  private String holdHeaderID;
  private String txnAppNo;
  private BigDecimal matchAmount;
  private String holdTypeID;
  private BigDecimal holdPercentage;
  private BigDecimal holdAmt;
  private String bpName;
  private BigDecimal amarsarfAmt;
  private String holdAcctType;
  private String holdUniqueCode;
  private String rdvTxnLineID;
  private String bpID;
  private String invoiceID;
  private String holdID;
  private Boolean isReleased;
  private String holdRelID;
  private BigDecimal releasedAmt;
  private BigDecimal enteredAmt;
  private Boolean isTxn;
  private String txnGroupRef;

  public String getHoldActionID() {
    return holdActionID;
  }

  public void setHoldActionID(String holdActionID) {
    this.holdActionID = holdActionID;
  }

  public String getHoldHeaderID() {
    return holdHeaderID;
  }

  public void setHoldHeaderID(String holdHeaderID) {
    this.holdHeaderID = holdHeaderID;
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

  public String getHoldTypeID() {
    return holdTypeID;
  }

  public void setHoldTypeID(String holdTypeID) {
    this.holdTypeID = holdTypeID;
  }

  public BigDecimal getHoldPercentage() {
    return holdPercentage;
  }

  public void setHoldPercentage(BigDecimal holdPercentage) {
    this.holdPercentage = holdPercentage;
  }

  public BigDecimal getHoldAmt() {
    return holdAmt;
  }

  public void setHoldAmt(BigDecimal holdAmt) {
    this.holdAmt = holdAmt;
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

  public String getHoldAcctType() {
    return holdAcctType;
  }

  public void setHoldAcctType(String holdAcctType) {
    this.holdAcctType = holdAcctType;
  }

  public String getHoldUniqueCode() {
    return holdUniqueCode;
  }

  public void setHoldUniqueCode(String holdUniqueCode) {
    this.holdUniqueCode = holdUniqueCode;
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

  public String getHoldID() {
    return holdID;
  }

  public void setHoldID(String holdID) {
    this.holdID = holdID;
  }

  public Boolean getIsReleased() {
    return isReleased;
  }

  public void setIsReleased(Boolean isReleased) {
    this.isReleased = isReleased;
  }

  public String getHoldRelID() {
    return holdRelID;
  }

  public void setHoldRelID(String holdRelID) {
    this.holdRelID = holdRelID;
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

  public Boolean getIsTxn() {
    return isTxn;
  }

  public void setIsTxn(Boolean isTxn) {
    this.isTxn = isTxn;
  }

  public String getTxnGroupRef() {
    return txnGroupRef;
  }

  public void setTxnGroupRef(String txnGroupRef) {
    this.txnGroupRef = txnGroupRef;
  }

}