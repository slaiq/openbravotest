package sa.elm.ob.finance.ad_reports.IntegratedCostBudgetInquiry.vo;

/**
 * 
 * @author Priyanka Ranjan on 22/04/2019
 * 
 */
// VO file for Integrated Cost Budget Inquiry Report

public class IntegratedCostBudgetInquiryVO {

  private String clientId;
  private String clientName;
  private String deptId;
  private String deptName;
  private String subAccountId;
  private String subAccountName;
  private String orgName = "";
  private String orgId = "";
  private String ledger = "";
  private String acctschemaId = "";
  private String acctschemaName = "";
  private String budgetTypeId = "";
  private String budgetTypeName = "";
  private String accountId = "";
  private String account = "";
  private String accountName = "";

  public String getAccountName() {
    return accountName;
  }

  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getAccount() {
    return account;
  }

  public void setAccount(String account) {
    this.account = account;
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

  public String getAcctschemaId() {
    return acctschemaId;
  }

  public void setAcctschemaId(String acctschemaId) {
    this.acctschemaId = acctschemaId;
  }

  public String getAcctschemaName() {
    return acctschemaName;
  }

  public void setAcctschemaName(String acctschemaName) {
    this.acctschemaName = acctschemaName;
  }

  public String getLedger() {
    return ledger;
  }

  public void setLedger(String ledger) {
    this.ledger = ledger;
  }

  public String getclientId() {
    return clientId;
  }

  public void setclientId(String clientId) {
    this.clientId = clientId;
  }

  public String getclientName() {
    return clientName;
  }

  public void setclientName(String clientName) {
    this.clientName = clientName;
  }

  public String getdeptId() {
    return deptId;
  }

  public void setdeptId(String deptId) {
    this.deptId = deptId;
  }

  public String getdeptName() {
    return deptName;
  }

  public void setdeptName(String deptName) {
    this.deptName = deptName;
  }

  public String getsubAccountId() {
    return subAccountId;
  }

  public void setsubAccountId(String subAccountId) {
    this.subAccountId = subAccountId;
  }

  public String getsubAccountName() {
    return subAccountName;
  }

  public void setsubAccountName(String subAccountName) {
    this.subAccountName = subAccountName;
  }

  public String getOrgName() {
    return orgName;
  }

  public void setOrgName(String orgName) {
    this.orgName = orgName;
  }

  public String getOrgId() {
    return orgId;
  }

  public void setOrgId(String orgId) {
    this.orgId = orgId;
  }
}