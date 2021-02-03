package sa.elm.ob.finance.ad_reports.budgetdetails.vo;

public class BudgetDetailsReportVO {

  private String OrgId = "";
  private String OrgName = "";
  private String yearId = "";
  private String yearName = "";
  private String budgetTypeId = "";
  private String budgetTypeName = "";
  private String budgetGroupId = "";
  private String budgetGroupName = "";
  private String deptId = "";
  private String deptName = "";

  public String getDeptId() {
    return deptId;
  }

  public void setDeptId(String deptId) {
    this.deptId = deptId;
  }

  public String getDeptName() {
    return deptName;
  }

  public void setDeptName(String deptName) {
    this.deptName = deptName;
  }

  public String getOrgId() {
    return OrgId;
  }

  public void setOrgId(String orgId) {
    OrgId = orgId;
  }

  public String getOrgName() {
    return OrgName;
  }

  public void setOrgName(String orgName) {
    OrgName = orgName;
  }

  public String getBudgetGroupId() {
    return budgetGroupId;
  }

  public void setBudgetGroupId(String budgetGroupId) {
    this.budgetGroupId = budgetGroupId;
  }

  public String getBudgetGroupName() {
    return budgetGroupName;
  }

  public void setBudgetGroupName(String budgetGroupName) {
    this.budgetGroupName = budgetGroupName;
  }

  public String getBudgetTypeId() {
    return budgetTypeId;
  }

  public void setBudgetTypeId(String budgetTypeId) {
    this.budgetTypeId = budgetTypeId;
  }

  public String getBudgetTypeName() {
    return budgetTypeName;
  }

  public void setBudgetTypeName(String budgetTypeName) {
    this.budgetTypeName = budgetTypeName;
  }

  public String getYearId() {
    return yearId;
  }

  public void setYearId(String yearId) {
    this.yearId = yearId;
  }

  public String getYearName() {
    return yearName;
  }

  public void setYearName(String yearName) {
    this.yearName = yearName;
  }

}
