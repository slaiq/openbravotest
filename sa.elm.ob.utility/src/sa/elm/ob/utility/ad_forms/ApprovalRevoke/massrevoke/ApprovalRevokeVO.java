package sa.elm.ob.utility.ad_forms.ApprovalRevoke.massrevoke;

public class ApprovalRevokeVO {
  private String orgId;
  private String orgName;
  private String requester;
  private String nextrole;
  private String lastperfomer;
  private String status;
  private String docno;
  private String recordId;

  public String getRecordId() {
    return recordId;
  }

  public void setRecordId(String recordId) {
    this.recordId = recordId;
  }

  public String getOrgId() {
    return orgId;
  }

  public String getDocno() {
    return docno;
  }

  public void setDocno(String docno) {
    this.docno = docno;
  }

  public void setOrgId(String orgId) {
    this.orgId = orgId;
  }

  public String getOrgName() {
    return orgName;
  }

  public void setOrgName(String orgName) {
    this.orgName = orgName;
  }

  public String getRequester() {
    return requester;
  }

  public void setRequester(String requester) {
    this.requester = requester;
  }

  public String getNextrole() {
    return nextrole;
  }

  public void setNextrole(String nextrole) {
    this.nextrole = nextrole;
  }

  public String getLastperfomer() {
    return lastperfomer;
  }

  public void setLastperfomer(String lastperfomer) {
    this.lastperfomer = lastperfomer;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
