package sa.elm.ob.utility.ad_process.Forward;

public class ForwardAlertVO {

  private String roleId;
  private String userId;

  public ForwardAlertVO(String roleId, String userId) {
    this.userId = userId;
    this.roleId = roleId;
  }

  public boolean equals(Object obj) {
    if (obj instanceof ForwardAlertVO) {
      ForwardAlertVO temp = (ForwardAlertVO) obj;
      if (this.userId.equals(temp.userId) && this.roleId.equals(temp.roleId))
        return true;
    }
    return false;

  }

  public int hashCode() {

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
