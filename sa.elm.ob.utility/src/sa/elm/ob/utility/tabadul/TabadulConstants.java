package sa.elm.ob.utility.tabadul;

public class TabadulConstants {

  public enum Status {

    SUCCESS("success"), ERROR("error");

    @SuppressWarnings("unused")
    private String status;

    private Status(String status) {
      this.status = status;
    }
  }

  public enum ErrorCode {

    INVALID_USER("-2"), INVALID_VENDOR("-3");

    private String errorCode;

    private ErrorCode(String errorCode) {
      this.errorCode = errorCode;
    }

    public String getErrorCode() {
      return errorCode;
    }
  }
}
