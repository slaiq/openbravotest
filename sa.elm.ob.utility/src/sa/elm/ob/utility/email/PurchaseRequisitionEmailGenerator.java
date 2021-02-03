package sa.elm.ob.utility.email;

import java.io.File;
import java.util.List;

import org.openbravo.email.EmailEventContentGenerator;

public class PurchaseRequisitionEmailGenerator implements EmailEventContentGenerator {

  @Override
  public boolean isValidEvent(String event, Object data) {
    return true;
  }

  @Override
  public String getSubject(Object data, String event) {
    return "تنبيه قادم من نظام إدارة الموارد الحكومية";
  }

  @Override
  public String getBody(Object data, String event) {

    return data.toString();
  }

  @Override
  public String getContentType() {
    return "text/html; charset=utf-8";
  }

  @Override
  public int getPriority() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public boolean preventsOthersExecution() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isAsynchronous() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public List<File> getAttachments(Object data, String event) {
    // TODO Auto-generated method stub
    return null;
  }

}
