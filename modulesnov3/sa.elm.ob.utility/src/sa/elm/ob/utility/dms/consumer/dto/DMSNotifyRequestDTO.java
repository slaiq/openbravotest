package sa.elm.ob.utility.dms.consumer.dto;

public class DMSNotifyRequestDTO {

  public String attachmentPath;
  public String grpProcessId;
  public boolean isError;
  public String errorMessage;

  public String getAttachmentPath() {
    return attachmentPath;
  }

  @Override
  public String toString() {
    return "attachmentPath=" + attachmentPath + ", grpProcessId=" + grpProcessId + ", isError="
        + isError + ", errorMessage=" + errorMessage;
  }

  public void setAttachmentPath(String attachmentPath) {
    this.attachmentPath = attachmentPath;
  }

  public String getGrpProcessId() {
    return grpProcessId;
  }

  public void setGrpProcessId(String grpProcessId) {
    this.grpProcessId = grpProcessId;
  }

  public boolean isError() {
    return isError;
  }

  public void setError(boolean isError) {
    this.isError = isError;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

}
