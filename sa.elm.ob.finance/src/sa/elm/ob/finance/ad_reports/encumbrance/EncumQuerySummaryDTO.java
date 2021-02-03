package sa.elm.ob.finance.ad_reports.encumbrance;

import java.math.BigDecimal;

public class EncumQuerySummaryDTO {

  public String fundsuniq;
  public String costuniq;
  public String uniquecode;
  public String validcombid;
  public BigDecimal notinvoiceamount;
  public BigDecimal unpaidinvoice;
  public BigDecimal paidinvoice;
  public BigDecimal postedinvoice;
  public BigDecimal encumbrance;
  public BigDecimal actual;
  public BigDecimal amountinvoiced;
  public String account;
  public String budgettype;
  public Boolean encumbrancecheck;
  public BigDecimal directfundencumbrance;
  public BigDecimal paidinvoiceamountcost;
  public BigDecimal unpaidinvoiceamountcost;
  public BigDecimal postinvoiceamountcost;
  public BigDecimal journalamount;
  public BigDecimal original_budget;
  public BigDecimal current_budget;
  public BigDecimal legacy_spent;

  public BigDecimal getLegacy_spent() {
    return legacy_spent;
  }

  public void setLegacy_spent(BigDecimal legacy_spent) {
    this.legacy_spent = legacy_spent;
  }

  public BigDecimal getOriginal_budget() {
    return original_budget;
  }

  public void setOriginal_budget(BigDecimal original_budget) {
    this.original_budget = original_budget;
  }

  public BigDecimal getCurrent_budget() {
    return current_budget;
  }

  public void setCurrent_budget(BigDecimal current_budget) {
    this.current_budget = current_budget;
  }

  public BigDecimal getJournalamount() {
    return journalamount;
  }

  public void setJournalamount(BigDecimal journalamount) {
    this.journalamount = journalamount;
  }

  public String getFundsuniq() {
    return fundsuniq;
  }

  public void setFundsuniq(String fundsuniq) {
    this.fundsuniq = fundsuniq;
  }

  public String getCostuniq() {
    return costuniq;
  }

  public void setCostuniq(String costuniq) {
    this.costuniq = costuniq;
  }

  public String getUniquecode() {
    return uniquecode;
  }

  public void setUniquecode(String uniquecode) {
    this.uniquecode = uniquecode;
  }

  public String getValidcombid() {
    return validcombid;
  }

  public void setValidcombid(String validcombid) {
    this.validcombid = validcombid;
  }

  public BigDecimal getEncumbrance() {
    return encumbrance;
  }

  public void setEncumbrance(BigDecimal encumbrance) {
    this.encumbrance = encumbrance;
  }

  public BigDecimal getActual() {
    return actual;
  }

  public void setActual(BigDecimal actual) {
    this.actual = actual;
  }

  public BigDecimal getAmountinvoiced() {
    return amountinvoiced;
  }

  public void setAmountinvoiced(BigDecimal amountinvoiced) {
    this.amountinvoiced = amountinvoiced;
  }

  public String getAccount() {
    return account;
  }

  public void setAccount(String account) {
    this.account = account;
  }

  public String getBudgettype() {
    return budgettype;
  }

  public void setBudgettype(String budgettype) {
    this.budgettype = budgettype;
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

  public BigDecimal getNotinvoiceamount() {
    return notinvoiceamount;
  }

  public void setNotinvoiceamount(BigDecimal notinvoiceamount) {
    this.notinvoiceamount = notinvoiceamount;
  }

  public Boolean getEncumbrancecheck() {
    return encumbrancecheck;
  }

  public void setEncumbrancecheck(Boolean encumbrancecheck) {
    this.encumbrancecheck = encumbrancecheck;
  }

  public BigDecimal getDirectfundencumbrance() {
    return directfundencumbrance;
  }

  public void setDirectfundencumbrance(BigDecimal directfundencumbrance) {
    this.directfundencumbrance = directfundencumbrance;
  }

  public BigDecimal getPaidinvoiceamountcost() {
    return paidinvoiceamountcost;
  }

  public void setPaidinvoiceamountcost(BigDecimal paidinvoiceamountcost) {
    this.paidinvoiceamountcost = paidinvoiceamountcost;
  }

  public BigDecimal getUnpaidinvoiceamountcost() {
    return unpaidinvoiceamountcost;
  }

  public void setUnpaidinvoiceamountcost(BigDecimal unpaidinvoiceamountcost) {
    this.unpaidinvoiceamountcost = unpaidinvoiceamountcost;
  }

  public BigDecimal getPostinvoiceamountcost() {
    return postinvoiceamountcost;
  }

  public void setPostinvoiceamountcost(BigDecimal postinvoiceamountcost) {
    this.postinvoiceamountcost = postinvoiceamountcost;
  }

}
