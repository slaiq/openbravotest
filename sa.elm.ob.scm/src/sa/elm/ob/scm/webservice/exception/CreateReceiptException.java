package sa.elm.ob.scm.webservice.exception;

import org.apache.log4j.Logger;

/**
 * This class is used to represent
 * 
 * @author Sathishkumar.P
 *
 */

public class CreateReceiptException extends Exception {

  private static final long serialVersionUID = 2182707934876477019L;

  public CreateReceiptException() {
    super();
    getLogger().error("Exception", this);
  }

  public CreateReceiptException(String message) {
    super(message);
  }

  public CreateReceiptException(String message, Throwable cause) {
    this(message, true);
  }

  public CreateReceiptException(String message, boolean logException) {
    super(message);
    if (logException) {
      getLogger().error(message, this);
    }
  }

  protected Logger getLogger() {
    return Logger.getLogger(this.getClass());
  }

}
