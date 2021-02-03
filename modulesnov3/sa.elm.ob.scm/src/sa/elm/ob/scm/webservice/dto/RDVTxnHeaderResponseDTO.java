package sa.elm.ob.scm.webservice.dto;

import java.math.BigDecimal;
import java.util.List;

public class RDVTxnHeaderResponseDTO {

  private String versionNo;
  private String certificateNo;
  private String certificateDate;
  private String versionDate;
  private String penalty;
  private String invoiceNo;
  private String amarsarafStatus;
  private boolean penaltyApplied;
  private boolean advance;
  private boolean isLegacy;
  private BigDecimal matchAmt;
  private BigDecimal penaltyDeduction;
  private BigDecimal advanceDeduction;
  private BigDecimal holdAmount;
  private BigDecimal totalDeduction;
  private BigDecimal netMatchAmt;
  private BigDecimal advanceMethod;
  private BigDecimal taxAmt;

  private List<RDVPenaltyHeaderDTO> penaltyHeaderDTO;
  private List<RDVHoldHeaderDTO> holdHeaderDTO;

  private List<RDVTxnLineResponseDTO> txnLine;

  public String getVersionNo() {
    return versionNo;
  }

  public void setVersionNo(String versionNo) {
    this.versionNo = versionNo;
  }

  public String getCertificateNo() {
    return certificateNo;
  }

  public void setCertificateNo(String certificateNo) {
    this.certificateNo = certificateNo;
  }

  public String getCertificateDate() {
    return certificateDate;
  }

  public void setCertificateDate(String certificateDate) {
    this.certificateDate = certificateDate;
  }

  public String getVersionDate() {
    return versionDate;
  }

  public void setVersionDate(String versionDate) {
    this.versionDate = versionDate;
  }

  public String getPenalty() {
    return penalty;
  }

  public void setPenalty(String penalty) {
    this.penalty = penalty;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public void setInvoiceNo(String invoiceNo) {
    this.invoiceNo = invoiceNo;
  }

  public String getAmarsarafStatus() {
    return amarsarafStatus;
  }

  public void setAmarsarafStatus(String amarsarafStatus) {
    this.amarsarafStatus = amarsarafStatus;
  }

  public boolean isPenaltyApplied() {
    return penaltyApplied;
  }

  public void setPenaltyApplied(boolean penaltyApplied) {
    this.penaltyApplied = penaltyApplied;
  }

  public boolean isAdvance() {
    return advance;
  }

  public void setAdvance(boolean advance) {
    this.advance = advance;
  }

  public boolean isLegacy() {
    return isLegacy;
  }

  public void setLegacy(boolean isLegacy) {
    this.isLegacy = isLegacy;
  }

  public BigDecimal getMatchAmt() {
    return matchAmt;
  }

  public void setMatchAmt(BigDecimal matchAmt) {
    this.matchAmt = matchAmt;
  }

  public BigDecimal getPenaltyDeduction() {
    return penaltyDeduction;
  }

  public void setPenaltyDeduction(BigDecimal penaltyDeduction) {
    this.penaltyDeduction = penaltyDeduction;
  }

  public BigDecimal getAdvanceDeduction() {
    return advanceDeduction;
  }

  public void setAdvanceDeduction(BigDecimal advanceDeduction) {
    this.advanceDeduction = advanceDeduction;
  }

  public BigDecimal getHoldAmount() {
    return holdAmount;
  }

  public void setHoldAmount(BigDecimal holdAmount) {
    this.holdAmount = holdAmount;
  }

  public BigDecimal getTotalDeduction() {
    return totalDeduction;
  }

  public void setTotalDeduction(BigDecimal totalDeduction) {
    this.totalDeduction = totalDeduction;
  }

  public BigDecimal getNetMatchAmt() {
    return netMatchAmt;
  }

  public void setNetMatchAmt(BigDecimal netMatchAmt) {
    this.netMatchAmt = netMatchAmt;
  }

  public BigDecimal getAdvanceMethod() {
    return advanceMethod;
  }

  public void setAdvanceMethod(BigDecimal advanceMethod) {
    this.advanceMethod = advanceMethod;
  }

  public BigDecimal getTaxAmt() {
    return taxAmt;
  }

  public void setTaxAmt(BigDecimal taxAmt) {
    this.taxAmt = taxAmt;
  }

  public List<RDVTxnLineResponseDTO> getTxnLine() {
    return txnLine;
  }

  public void setTxnLine(List<RDVTxnLineResponseDTO> txnLine) {
    this.txnLine = txnLine;
  }

  public List<RDVPenaltyHeaderDTO> getPenaltyHeaderDTO() {
    return penaltyHeaderDTO;
  }

  public void setPenaltyHeaderDTO(List<RDVPenaltyHeaderDTO> penaltyHeaderDTO) {
    this.penaltyHeaderDTO = penaltyHeaderDTO;
  }

  public List<RDVHoldHeaderDTO> getHoldHeaderDTO() {
    return holdHeaderDTO;
  }

  public void setHoldHeaderDTO(List<RDVHoldHeaderDTO> holdHeaderDTO) {
    this.holdHeaderDTO = holdHeaderDTO;
  }

}
