package sa.elm.ob.scm.webservice.dto;

import java.math.BigDecimal;
import java.util.List;

public class RDVHeaderResponseDTO {

  private String rdvId;
  private String txnHeaderNo;
  private String txnType;
  private String txnDate;
  private String orderNo;
  private String poReceiptNo;
  private String supplierNo;
  private String poEncumbranceNo;
  private String bpBankNo;
  private String bankName;
  private String iban;
  private String penaltyStatus;
  private String advanceMethod;
  private BigDecimal totalContractAmount;
  private BigDecimal totalPenaltyLevied;
  private BigDecimal totalAdvanceDeducted;
  private BigDecimal totalDeduction;
  private BigDecimal totalUninvoicedTxnRaised;
  private BigDecimal totalUnpaidInvoiceRaised;
  private BigDecimal advancePercentage;
  private BigDecimal totalPaymentsMade;
  private BigDecimal totalContractAmountRemaining;
  private BigDecimal totalAdvance;
  private BigDecimal legacyHoldAmt;
  private BigDecimal applicationHoldAmt;
  private BigDecimal totalHoldAmt;
  private BigDecimal totalReceivedAmt;
  private BigDecimal totalPaidAmtLegacy;
  private BigDecimal openingAdvanceBalance;
  private BigDecimal totalPenaltyDeducted;
  private BigDecimal legacyTotalAdvanceDeducted;

  private List<RDVPenaltyAttributesDTO> penaltyDTO;
  private List<RDVHoldAttributesDTO> holdDTO;

  private List<RDVTxnHeaderResponseDTO> txnVersion;

  public String getRdvId() {
    return rdvId;
  }

  public void setRdvId(String rdvId) {
    this.rdvId = rdvId;
  }

  public String getTxnHeaderNo() {
    return txnHeaderNo;
  }

  public void setTxnHeaderNo(String txnHeaderNo) {
    this.txnHeaderNo = txnHeaderNo;
  }

  public String getTxnType() {
    return txnType;
  }

  public void setTxnType(String txnType) {
    this.txnType = txnType;
  }

  public String getTxnDate() {
    return txnDate;
  }

  public void setTxnDate(String txnDate) {
    this.txnDate = txnDate;
  }

  public String getOrderNo() {
    return orderNo;
  }

  public void setOrderNo(String orderNo) {
    this.orderNo = orderNo;
  }

  public String getPoReceiptNo() {
    return poReceiptNo;
  }

  public void setPoReceiptNo(String poReceiptNo) {
    this.poReceiptNo = poReceiptNo;
  }

  public String getSupplierNo() {
    return supplierNo;
  }

  public void setSupplierNo(String supplierNo) {
    this.supplierNo = supplierNo;
  }

  public String getPoEncumbranceNo() {
    return poEncumbranceNo;
  }

  public void setPoEncumbranceNo(String poEncumbranceNo) {
    this.poEncumbranceNo = poEncumbranceNo;
  }

  public String getBpBankNo() {
    return bpBankNo;
  }

  public void setBpBankNo(String bpBankNo) {
    this.bpBankNo = bpBankNo;
  }

  public String getBankName() {
    return bankName;
  }

  public void setBankName(String bankName) {
    this.bankName = bankName;
  }

  public String getIban() {
    return iban;
  }

  public void setIban(String iban) {
    this.iban = iban;
  }

  public String getPenaltyStatus() {
    return penaltyStatus;
  }

  public void setPenaltyStatus(String penaltyStatus) {
    this.penaltyStatus = penaltyStatus;
  }

  public String getAdvanceMethod() {
    return advanceMethod;
  }

  public void setAdvanceMethod(String advanceMethod) {
    this.advanceMethod = advanceMethod;
  }

  public BigDecimal getTotalContractAmount() {
    return totalContractAmount;
  }

  public void setTotalContractAmount(BigDecimal totalContractAmount) {
    this.totalContractAmount = totalContractAmount;
  }

  public BigDecimal getTotalPenaltyLevied() {
    return totalPenaltyLevied;
  }

  public void setTotalPenaltyLevied(BigDecimal totalPenaltyLevied) {
    this.totalPenaltyLevied = totalPenaltyLevied;
  }

  public BigDecimal getTotalAdvanceDeducted() {
    return totalAdvanceDeducted;
  }

  public void setTotalAdvanceDeducted(BigDecimal totalAdvanceDeducted) {
    this.totalAdvanceDeducted = totalAdvanceDeducted;
  }

  public BigDecimal getTotalDeduction() {
    return totalDeduction;
  }

  public void setTotalDeduction(BigDecimal totalDeduction) {
    this.totalDeduction = totalDeduction;
  }

  public BigDecimal getTotalUninvoicedTxnRaised() {
    return totalUninvoicedTxnRaised;
  }

  public void setTotalUninvoicedTxnRaised(BigDecimal totalUninvoicedTxnRaised) {
    this.totalUninvoicedTxnRaised = totalUninvoicedTxnRaised;
  }

  public BigDecimal getTotalUnpaidInvoiceRaised() {
    return totalUnpaidInvoiceRaised;
  }

  public void setTotalUnpaidInvoiceRaised(BigDecimal totalUnpaidInvoiceRaised) {
    this.totalUnpaidInvoiceRaised = totalUnpaidInvoiceRaised;
  }

  public BigDecimal getAdvancePercentage() {
    return advancePercentage;
  }

  public void setAdvancePercentage(BigDecimal advancePercentage) {
    this.advancePercentage = advancePercentage;
  }

  public BigDecimal getTotalPaymentsMade() {
    return totalPaymentsMade;
  }

  public void setTotalPaymentsMade(BigDecimal totalPaymentsMade) {
    this.totalPaymentsMade = totalPaymentsMade;
  }

  public BigDecimal getTotalContractAmountRemaining() {
    return totalContractAmountRemaining;
  }

  public void setTotalContractAmountRemaining(BigDecimal totalContractAmountRemaining) {
    this.totalContractAmountRemaining = totalContractAmountRemaining;
  }

  public BigDecimal getTotalAdvance() {
    return totalAdvance;
  }

  public void setTotalAdvance(BigDecimal totalAdvance) {
    this.totalAdvance = totalAdvance;
  }

  public BigDecimal getLegacyHoldAmt() {
    return legacyHoldAmt;
  }

  public void setLegacyHoldAmt(BigDecimal legacyHoldAmt) {
    this.legacyHoldAmt = legacyHoldAmt;
  }

  public BigDecimal getApplicationHoldAmt() {
    return applicationHoldAmt;
  }

  public void setApplicationHoldAmt(BigDecimal applicationHoldAmt) {
    this.applicationHoldAmt = applicationHoldAmt;
  }

  public BigDecimal getTotalHoldAmt() {
    return totalHoldAmt;
  }

  public void setTotalHoldAmt(BigDecimal totalHoldAmt) {
    this.totalHoldAmt = totalHoldAmt;
  }

  public BigDecimal getTotalReceivedAmt() {
    return totalReceivedAmt;
  }

  public void setTotalReceivedAmt(BigDecimal totalReceivedAmt) {
    this.totalReceivedAmt = totalReceivedAmt;
  }

  public BigDecimal getTotalPaidAmtLegacy() {
    return totalPaidAmtLegacy;
  }

  public void setTotalPaidAmtLegacy(BigDecimal totalPaidAmtLegacy) {
    this.totalPaidAmtLegacy = totalPaidAmtLegacy;
  }

  public BigDecimal getOpeningAdvanceBalance() {
    return openingAdvanceBalance;
  }

  public void setOpeningAdvanceBalance(BigDecimal openingAdvanceBalance) {
    this.openingAdvanceBalance = openingAdvanceBalance;
  }

  public BigDecimal getTotalPenaltyDeducted() {
    return totalPenaltyDeducted;
  }

  public void setTotalPenaltyDeducted(BigDecimal totalPenaltyDeducted) {
    this.totalPenaltyDeducted = totalPenaltyDeducted;
  }

  public BigDecimal getLegacyTotalAdvanceDeducted() {
    return legacyTotalAdvanceDeducted;
  }

  public void setLegacyTotalAdvanceDeducted(BigDecimal legacyTotalAdvanceDeducted) {
    this.legacyTotalAdvanceDeducted = legacyTotalAdvanceDeducted;
  }

  public List<RDVTxnHeaderResponseDTO> getTxnVersion() {
    return txnVersion;
  }

  public void setTxnVersion(List<RDVTxnHeaderResponseDTO> txnVersion) {
    this.txnVersion = txnVersion;
  }

  public List<RDVPenaltyAttributesDTO> getPenaltyDTO() {
    return penaltyDTO;
  }

  public void setPenaltyDTO(List<RDVPenaltyAttributesDTO> penaltyDTO) {
    this.penaltyDTO = penaltyDTO;
  }

  public List<RDVHoldAttributesDTO> getHoldDTO() {
    return holdDTO;
  }

  public void setHoldDTO(List<RDVHoldAttributesDTO> holdDTO) {
    this.holdDTO = holdDTO;
  }

}
