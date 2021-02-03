package sa.elm.ob.scm.actionHandler;

import org.openbravo.erpCommon.utility.OBMessageUtils;

@SuppressWarnings("serial")
public class TabadulIntegrationException extends Exception {

  @SuppressWarnings("unused")
  private String errorCode;

  public TabadulIntegrationException(String errorCode) {
    super(OBMessageUtils.messageBD(errorCode));
    this.errorCode = errorCode;
  }
}
