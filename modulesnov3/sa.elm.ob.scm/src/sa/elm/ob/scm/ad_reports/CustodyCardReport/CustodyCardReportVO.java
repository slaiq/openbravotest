package sa.elm.ob.scm.ad_reports.CustodyCardReport;

public class CustodyCardReportVO {

  private String beneficiaryvalue;
  private String beneficiaryname;
  private String beneficiaryId;
  private String roleId;
  private String userId;
  private String currentDateHijri;

  public String getCurrentDateHijri() {
    return currentDateHijri;
  }

  public void setCurrentDateHijri(String currentDateHijri) {
    this.currentDateHijri = currentDateHijri;
  }

  public String getRoleId() {
    return roleId;
  }

  public void setRoleId(String roleId) {
    this.roleId = roleId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getBeneficiaryId() {
    return beneficiaryId;
  }

  public void setBeneficiaryId(String beneficiaryId) {
    this.beneficiaryId = beneficiaryId;
  }

  public String getBeneficiaryvalue() {
    return beneficiaryvalue;
  }

  public void setBeneficiaryvalue(String beneficiaryvalue) {
    this.beneficiaryvalue = beneficiaryvalue;
  }

  public String getBeneficiaryname() {
    return beneficiaryname;
  }

  public void setBeneficiaryname(String beneficiaryname) {
    this.beneficiaryname = beneficiaryname;
  }

}
