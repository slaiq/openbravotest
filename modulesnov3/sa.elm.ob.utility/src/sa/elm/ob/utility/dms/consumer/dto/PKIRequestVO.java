package sa.elm.ob.utility.dms.consumer.dto;

public class PKIRequestVO {

  private String profileURI;
  private String grpRequestID;
  private String userId;
  private String documentName;
  private String approvalPosition;
  private String documentType;
  private int pageCount;

  public PKIRequestVO(String profileURI, String grpRequestID, String userId, String documentName,
      String approvalPosition, String documentType, int pageCount) {
    super();
    this.profileURI = profileURI;
    this.grpRequestID = grpRequestID;
    this.userId = userId;
    this.documentName = documentName;
    this.approvalPosition = approvalPosition;
    this.documentType = documentType;
    this.pageCount = pageCount;
  }

  public String getProfileURI() {
    return profileURI;
  }

  public void setProfileURI(String profileURI) {
    this.profileURI = profileURI;
  }

  public String getGrpRequestID() {
    return grpRequestID;
  }

  public void setGrpRequestID(String grpID) {
    this.grpRequestID = grpID;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getDocumentName() {
    return documentName;
  }

  public void setDocumentName(String documentName) {
    this.documentName = documentName;
  }

  public String getApprovalPosition() {
    return approvalPosition;
  }

  public void setApprovalPosition(String approvalPosition) {
    this.approvalPosition = approvalPosition;
  }

  public String getDocumentType() {
    return documentType;
  }

  public void setDocumentType(String documentType) {
    this.documentType = documentType;
  }

  public int getPageCount() {
    return pageCount;
  }

  public void setPageCount(int pageCount) {
    this.pageCount = pageCount;
  }

  @Override
  public String toString() {
    return "profileURI=" + profileURI + "\n, grpRequestID=" + grpRequestID + ", \n userId=" + userId
        + ", documentName=" + documentName + ", approvalPosition=" + approvalPosition
        + ", documentType=" + documentType + ",pagecount=" + pageCount;
  }

}
