package sa.elm.ob.utility.dms.consumer.dto;

public class DMSNotifyResponseDTO {

  public boolean success;
  public String message;

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

}
