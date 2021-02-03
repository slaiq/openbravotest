package sa.elm.ob.finance.ad_process.RDVProcess.vo;

public class RDVHoldProcessVO {

  private String roleId;
  private String userId;

  public RDVHoldProcessVO(String roleId, String userId) {
    this.userId = userId;
    this.roleId = roleId;
  }

  public boolean equals(Object obj) {
    // TODO Auto-generated method stub
    if (obj instanceof RDVHoldProcessVO) {
      RDVHoldProcessVO temp = (RDVHoldProcessVO) obj;
      if (this.userId.equals(temp.userId) && this.roleId.equals(temp.roleId))
        return true;
    }
    return false;

  }

  public int hashCode() {
    // TODO Auto-generated method stub

    return (this.userId.hashCode() + this.roleId.hashCode());
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

  public String toString() {
    return "userId: " + userId + "  roleId: " + roleId;
  }

}
