package sa.elm.ob.finance.ad_reports.encumbrance;

import java.math.BigDecimal;

public class EncumbranceDTO {

  public String budgetType;
  public String encumbranceNo;
  public String encumbranceMethod;
  public String encumbranceType;
  public Boolean isInvoiced;
  public String costEncumbranceNo;
  public BigDecimal amountInvoiced;
  public BigDecimal fundsEncumbranceAmount;
  public BigDecimal fundsActualAmount;
  public Boolean isValid;

  public String getBudgetType() {
    return budgetType;
  }

  public void setBudgetType(String budgetType) {
    this.budgetType = budgetType;
  }

  public String getEncumbranceNo() {
    return encumbranceNo;
  }

  public void setEncumbranceNo(String encumbranceNo) {
    this.encumbranceNo = encumbranceNo;
  }

  public String getEncumbranceType() {
    return encumbranceType;
  }

  public void setEncumbranceType(String encumbranceType) {
    this.encumbranceType = encumbranceType;
  }

  public Boolean getIsInvoiced() {
    return isInvoiced;
  }

  public void setIsInvoiced(Boolean isInvoiced) {
    this.isInvoiced = isInvoiced;
  }

  public String getCostEncumbranceNo() {
    return costEncumbranceNo;
  }

  public void setCostEncumbranceNo(String costEncumbranceNo) {
    this.costEncumbranceNo = costEncumbranceNo;
  }

  public BigDecimal getAmountInvoiced() {
    return amountInvoiced;
  }

  public void setAmountInvoiced(BigDecimal amountInvoiced) {
    this.amountInvoiced = amountInvoiced;
  }

  public BigDecimal getFundsEncumbranceAmount() {
    return fundsEncumbranceAmount;
  }

  public void setFundsEncumbranceAmount(BigDecimal fundsEncumbranceAmount) {
    this.fundsEncumbranceAmount = fundsEncumbranceAmount;
  }

  public BigDecimal getFundsActualAmount() {
    return fundsActualAmount;
  }

  public void setFundsActualAmount(BigDecimal fundsActualAmount) {
    this.fundsActualAmount = fundsActualAmount;
  }

  public Boolean getIsValid() {
    return isValid;
  }

  public void setIsValid(Boolean isValid) {
    this.isValid = isValid;
  }

  public String getEncumbranceMethod() {
    return encumbranceMethod;
  }

  public void setEncumbranceMethod(String encumbranceMethod) {
    this.encumbranceMethod = encumbranceMethod;
  }

}
