package sa.elm.ob.scm.webservice.epm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AddProjectResponseDto {

  @JsonProperty("isSuccess")
  private boolean isSuccess;
  private AddProjectErrorResponse errors;

  public boolean isSuccess() {
    return isSuccess;
  }

  public void setSuccess(boolean isSuccess) {
    this.isSuccess = isSuccess;
  }

  public AddProjectErrorResponse getErrors() {
    return errors;
  }

  public void setErrors(AddProjectErrorResponse errors) {
    this.errors = errors;
  }

}

class AddProjectErrorResponse {
  private int Code;
  private String Description;

  public int getCode() {
    return Code;
  }

  public void setCode(int code) {
    Code = code;
  }

  public String getDescription() {
    return Description;
  }

  public void setDescription(String description) {
    Description = description;
  }

}
