package sa.elm.ob.finance.ad_reports.encumbrance;

import java.math.BigDecimal;

public class EncumbranceSummaryDTO {

  public String budgettype;
  public String account;
  public String uniquecode;
  public BigDecimal encumbranceamount;
  public BigDecimal actualamount;
  public BigDecimal unpaidinvoice;
  public BigDecimal paidinvoice;
  public BigDecimal postedinvoice;
  public BigDecimal notinvoicedamount;
  public BigDecimal unpaidinvoicefromcost;
  public BigDecimal paidinvoicefromcost;
  public BigDecimal directfundsencumbranceamount;
  public BigDecimal fundsinvoiceunpaid;
  public BigDecimal fundsinvoicepaid;
  public BigDecimal fundsActualamount;
  public BigDecimal costActualAmount;
  public Boolean encumbrancecheck;
  public Boolean fundscostencumbrancecheck;
  public BigDecimal journal_amount;
  public BigDecimal current_budget;
  public BigDecimal original_budget;
  public BigDecimal legacy_spent;

  public BigDecimal getLegacy_spent() {
    return legacy_spent;
  }

  public void setLegacy_spent(BigDecimal legacy_spent) {
    this.legacy_spent = legacy_spent;
  }

  public BigDecimal getCurrent_budget() {
    return current_budget;
  }

  public void setCurrent_budget(BigDecimal current_budget) {
    this.current_budget = current_budget;
  }

  public BigDecimal getOriginal_budget() {
    return original_budget;
  }

  public void setOriginal_budget(BigDecimal original_budget) {
    this.original_budget = original_budget;
  }

  public BigDecimal getJournal_amount() {
    return journal_amount;
  }

  public void setJournal_amount(BigDecimal journal_amount) {
    this.journal_amount = journal_amount;
  }

  public String getBudgettype() {
    return budgettype;
  }

  public void setBudgettype(String budgettype) {
    this.budgettype = budgettype;
  }

  public String getAccount() {
    return account;
  }

  public void setAccount(String account) {
    this.account = account;
  }

  public String getUniquecode() {
    return uniquecode;
  }

  public void setUniquecode(String uniquecode) {
    this.uniquecode = uniquecode;
  }

  public BigDecimal getEncumbranceamount() {
    return encumbranceamount;
  }

  public void setEncumbranceamount(BigDecimal encumbranceamount) {
    this.encumbranceamount = encumbranceamount;
  }

  public BigDecimal getActualamount() {
    return actualamount;
  }

  public void setActualamount(BigDecimal actualamount) {
    this.actualamount = actualamount;
  }

  public BigDecimal getNotinvoicedamount() {
    return notinvoicedamount;
  }

  public void setNotinvoicedamount(BigDecimal notinvoicedamount) {
    this.notinvoicedamount = notinvoicedamount;
  }

  public BigDecimal getFundsActualamount() {
    return fundsActualamount;
  }

  public void setFundsActualamount(BigDecimal fundsActualamount) {
    this.fundsActualamount = fundsActualamount;
  }

  public BigDecimal getUnpaidinvoice() {
    return unpaidinvoice;
  }

  public void setUnpaidinvoice(BigDecimal unpaidinvoice) {
    this.unpaidinvoice = unpaidinvoice;
  }

  public BigDecimal getPaidinvoice() {
    return paidinvoice;
  }

  public void setPaidinvoice(BigDecimal paidinvoice) {
    this.paidinvoice = paidinvoice;
  }

  public BigDecimal getPostedinvoice() {
    return postedinvoice;
  }

  public void setPostedinvoice(BigDecimal postedinvoice) {
    this.postedinvoice = postedinvoice;
  }

  public Boolean getEncumbrancecheck() {
    return encumbrancecheck;
  }

  public void setEncumbrancecheck(Boolean encumbrancecheck) {
    this.encumbrancecheck = encumbrancecheck;
  }

  public BigDecimal getUnpaidinvoicefromcost() {
    return unpaidinvoicefromcost;
  }

  public void setUnpaidinvoicefromcost(BigDecimal unpaidinvoicefromcost) {
    this.unpaidinvoicefromcost = unpaidinvoicefromcost;
  }

  public BigDecimal getPaidinvoicefromcost() {
    return paidinvoicefromcost;
  }

  public void setPaidinvoicefromcost(BigDecimal paidinvoicefromcost) {
    this.paidinvoicefromcost = paidinvoicefromcost;
  }

  public BigDecimal getDirectfundsencumbranceamount() {
    return directfundsencumbranceamount;
  }

  public void setDirectfundsencumbranceamount(BigDecimal directfundsencumbranceamount) {
    this.directfundsencumbranceamount = directfundsencumbranceamount;
  }

  public BigDecimal getFundsinvoiceunpaid() {
    return fundsinvoiceunpaid;
  }

  public void setFundsinvoiceunpaid(BigDecimal fundsinvoiceunpaid) {
    this.fundsinvoiceunpaid = fundsinvoiceunpaid;
  }

  public BigDecimal getFundsinvoicepaid() {
    return fundsinvoicepaid;
  }

  public void setFundsinvoicepaid(BigDecimal fundsinvoicepaid) {
    this.fundsinvoicepaid = fundsinvoicepaid;
  }

  public BigDecimal getCostActualAmount() {
    return costActualAmount;
  }

  public void setCostActualAmount(BigDecimal costActualAmount) {
    this.costActualAmount = costActualAmount;
  }

  public Boolean getFundscostencumbrancecheck() {
    return fundscostencumbrancecheck;
  }

  public void setFundscostencumbrancecheck(Boolean fundscostencumbrancecheck) {
    this.fundscostencumbrancecheck = fundscostencumbrancecheck;
  }

}
