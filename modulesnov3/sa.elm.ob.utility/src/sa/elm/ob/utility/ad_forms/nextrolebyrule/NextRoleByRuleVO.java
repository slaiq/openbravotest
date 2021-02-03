package sa.elm.ob.utility.ad_forms.nextrolebyrule;

public class NextRoleByRuleVO {
  private String nextRoleId;
  private String nextApprover;
  private String nextRoleName;
  private int roleSeqNo = 0;
  private int ruleSeqNo = 0;
  private String status;

  private String shortStatus;
  private String fullStatus;
  private boolean hasApproval;
  private String errorMsg;
  private String roleName;
  private String userName;
  private String orgName;

  public String getOrgName() {
    return orgName;
  }

  public void setOrgName(String orgName) {
    this.orgName = orgName;
  }

  public boolean isHasApproval() {
    return hasApproval;
  }

  public void setHasApproval(boolean hasApproval) {
    this.hasApproval = hasApproval;
  }

  public String getRoleName() {
    return roleName;
  }

  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getErrorMsg() {
    return errorMsg;
  }

  public void setErrorMsg(String errorMsg) {
    this.errorMsg = errorMsg;
  }

  public String getNextRoleId() {
    return nextRoleId;
  }

  public void setNextRoleId(String nextRoleId) {
    this.nextRoleId = nextRoleId;
  }

  public String getNextApprover() {
    return nextApprover;
  }

  public void setNextApprover(String nextApprover) {
    this.nextApprover = nextApprover;
  }

  public String getNextRoleName() {
    return nextRoleName;
  }

  public void setNextRoleName(String nextRoleName) {
    this.nextRoleName = nextRoleName;
  }

  public int getRoleSeqNo() {
    return roleSeqNo;
  }

  public void setRoleSeqNo(int roleSeqNo) {
    this.roleSeqNo = roleSeqNo;
  }

  public int getRuleSeqNo() {
    return ruleSeqNo;
  }

  public void setRuleSeqNo(int ruleSeqNo) {
    this.ruleSeqNo = ruleSeqNo;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getShortStatus() {
    return shortStatus;
  }

  public void setShortStatus(String shortStatus) {
    this.shortStatus = shortStatus;
  }

  public String getFullStatus() {
    return fullStatus;
  }

  public void setFullStatus(String fullStatus) {
    this.fullStatus = fullStatus;
  }

  public boolean hasApproval() {
    return hasApproval;
  }

  public void setApproval(boolean hasApproval) {
    this.hasApproval = hasApproval;
  }

}