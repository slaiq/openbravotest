package sa.elm.ob.utility.ad_process.RequestMoreInformation;

public class RequestMoreInformationVO {
  private String roleId;
  private String userId;
  private String department;
  private String rmiRequest;
  private String rmiResponse;
  private String UserName;
  private String RoleName;
  private String rmiReponseMsg;

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

  public String getUserName() {
    return UserName;
  }

  public void setUserName(String UserName) {
    this.UserName = UserName;
  }

  public String getRoleName() {
    return RoleName;
  }

  public void setRoleName(String RoleName) {
    this.RoleName = RoleName;
  }

  public String getdepartment() {
    return department;
  }

  public void setdepartment(String department) {
    this.department = department;
  }

  public String getrmiResponse() {
    return rmiResponse;
  }

  public void setrmiResponse(String rmiResponse) {
    this.rmiResponse = rmiResponse;
  }

  public String getrmiRequest() {
    return rmiRequest;
  }

  public void setrmiRequest(String rmiRequest) {
    this.rmiRequest = rmiRequest;
  }

  public String getRmiReponseMsg() {
    return rmiReponseMsg;
  }

  public void setRmiReponseMsg(String rmiReponseMsg) {
    this.rmiReponseMsg = rmiReponseMsg;
  }
}
