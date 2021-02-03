package sa.elm.ob.utility.dms.notifyuser.exceptions;

public class DmsNotifyUserException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  /**
   * 
   * @author Gopalakrishnan Represents the Notify User Exception
   */

  private String messageKey = "";

  public String getMessageKey() {
    return messageKey;
  }

  public void setMessageKey(String messageKey) {
    this.messageKey = messageKey;
  }

  public DmsNotifyUserException(String message) {
    super(message);
  }
}