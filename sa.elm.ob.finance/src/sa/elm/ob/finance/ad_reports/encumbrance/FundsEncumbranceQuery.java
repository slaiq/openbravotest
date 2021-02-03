package sa.elm.ob.finance.ad_reports.encumbrance;

import java.math.BigDecimal;

public class FundsEncumbranceQuery {
  public BigDecimal fundsencumbranceamt;
  public BigDecimal fundsactualamt;

  public BigDecimal getFundsencumbranceamt() {
    return fundsencumbranceamt;
  }

  public void setFundsencumbranceamt(BigDecimal fundsencumbranceamt) {
    this.fundsencumbranceamt = fundsencumbranceamt;
  }

  public BigDecimal getFundsactualamt() {
    return fundsactualamt;
  }

  public void setFundsactualamt(BigDecimal fundsactualamt) {
    this.fundsactualamt = fundsactualamt;
  }

}