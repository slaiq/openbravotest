
package sa.elm.ob.utility.dms.consumer.dto;

public class DeleteAttachmentResponseGRP {

  public boolean isError() {
    return isError;
  }

  public void setError(boolean isError) {
    this.isError = isError;
  }

  public String getResponse() {
    return response;
  }

  public void setResponse(String response) {
    this.response = response;
  }

  public boolean isOperationSuccess() {
    return operationSuccess;
  }

  public void setOperationSuccess(boolean operationSuccess) {
    this.operationSuccess = operationSuccess;
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

  public String errorMsg;
  public String response;
  public boolean isError;
  public boolean operationSuccess;
  public String request;

}
