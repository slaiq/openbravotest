
package sa.elm.ob.finance.ad_reports.encumbrance;

import java.math.BigDecimal;

public class EncumbranceQueryDTO {

  public String efinbudgetmanencumid;
  public String budgettype;
  public String encumbranceno;
  public String encumbrancemethod;
  public String encumbrancetype;
  public Boolean isinvoiced;
  public String costencumbranceno;
  public BigDecimal amountinvoiced;

  public String getEncumbranceId() {
    return efinbudgetmanencumid;
  }

  public void setEncumbranceId(String encumbranceId) {
    this.efinbudgetmanencumid = encumbranceId;
  }

  public String getBudgettype() {
    return budgettype;
  }

  public void setBudgettype(String budgettype) {
    this.budgettype = budgettype;
  }

  public String getEncumbranceno() {
    return encumbranceno;
  }

  public void setEncumbranceno(String encumbranceno) {
    this.encumbranceno = encumbranceno;
  }

  public String getEncumbrancemethod() {
    return encumbrancemethod;
  }

  public void setEncumbrancemethod(String encumbrancemethod) {
    this.encumbrancemethod = encumbrancemethod;
  }

  public String getEncumbrancetype() {
    return encumbrancetype;
  }

  public void setEncumbrancetype(String encumbrancetype) {
    this.encumbrancetype = encumbrancetype;
  }

  public Boolean getIsinvoiced() {
    return isinvoiced;
  }

  public void setIsinvoiced(Boolean isinvoiced) {
    this.isinvoiced = isinvoiced;
  }

  public String getCostencumbranceno() {
    return costencumbranceno;
  }

  public void setCostencumbranceno(String costencumbranceno) {
    this.costencumbranceno = costencumbranceno;
  }

  public BigDecimal getAmountinvoiced() {
    return amountinvoiced;
  }

  public void setAmountinvoiced(BigDecimal amountinvoiced) {
    this.amountinvoiced = amountinvoiced;
  }

}
