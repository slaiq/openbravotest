package sa.elm.ob.scm.webservice.vehiclesystem.dto;

import java.io.Serializable;

import sa.elm.ob.scm.webservice.vehiclesystem.constant.VehicleSystemConstants;

public class GenericResponse<T> implements Serializable {

  private String status = VehicleSystemConstants.FAILED;
  private T data;
  private String errorMessage;
  private String exceptionMessage;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public T getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getExceptionMessage() {
    return exceptionMessage;
  }

  public void setExceptionMessage(String exceptionMessage) {
    this.exceptionMessage = exceptionMessage;
  }

  public void setErrorAndExceptionMessage(String errorMessage, String exceptionMessage) {
    setErrorMessage(errorMessage);
    setExceptionMessage(exceptionMessage);
  }

}
