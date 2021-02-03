package sa.elm.ob.utility.dms.consumer.dto;

public class GetAttachmentGRPResponse {

  public boolean isError;
  public String base64Str;
  public String errorMsg;
  public String request;
  public String response;

  public String getBase64Str() {
    return base64Str;
  }

  public void setBase64Str(String base64Str) {
    this.base64Str = base64Str;
  }

  public String getResponse() {
    return response;
  }

  public void setResponse(String response) {
    this.response = response;
  }

  public boolean isError() {
    return isError;
  }

  public void setError(boolean isError) {
    this.isError = isError;
  }

  public String getErrorMsg() {
    return errorMsg;
  }

  public void setErrorMsg(String errorMsg) {
    this.errorMsg = errorMsg;
  }

  public String getRequest() {
    return request;
  }

  public void setRequest(String request) {
    this.request = request;
  }

}
