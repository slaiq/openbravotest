package sa.elm.ob.finance.ad_process;

public enum EncumbranceTypeE {
  AMARSARF("AEE"), PROPOSALAWARD("PAE"), PREPAYMENT("AAE"), PURCHASEORDER("POE"), MODIFICATION(
      "MO"), ADDITIONAL("AET"), REQUISITION("PRE"), BID("BE"), DIRECT("DE"), TEMPORARY("TE");

  private String encumbranceType;

  private EncumbranceTypeE(String strEncumType) {
    this.setEncumbranceType(strEncumType);
  }

  public String getEncumbranceType() {
    return encumbranceType;
  }

  public void setEncumbranceType(String encumbranceType) {
    this.encumbranceType = encumbranceType;
  }
}
