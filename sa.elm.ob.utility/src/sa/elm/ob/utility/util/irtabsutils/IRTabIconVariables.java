package sa.elm.ob.utility.util.irtabsutils;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

public abstract class IRTabIconVariables {
  protected int enable = 0;
  protected String receivingType = "";
  protected int count = 0;
  protected Boolean ispreference = false;

  public int getEnable() {
    return enable;
  }

  public void setEnable(int enable) {
    this.enable = enable;
  }

  public String getReceivingType() {
    return receivingType;
  }

  public void setReceivingType(String receivingType) {
    this.receivingType = receivingType;
  }

  public abstract void getIconVariables(HttpServletRequest request, JSONObject jsonData);
}
