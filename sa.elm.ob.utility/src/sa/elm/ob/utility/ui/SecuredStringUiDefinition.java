
package sa.elm.ob.utility.ui;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.reference.StringUIDefinition;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.Sqlc;
import org.openbravo.model.ad.ui.Field;

import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.SecuredStringDAO;

/**
 * 
 * @author sathish kumar
 * 
 *         This class is used to define the properties of secured string reference
 *
 */
public class SecuredStringUiDefinition extends StringUIDefinition {

  @Override
  public String getFieldProperties(Field field, boolean getValueFromSession) {
    String columnValue = "";
    RequestContext rq = RequestContext.get();
    if (getValueFromSession) {
      String inpColumnName = null;
      if (field.getProperty() != null && !field.getProperty().isEmpty()) {
        inpColumnName = "inp" + "_propertyField_"
            + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "") + "_"
            + field.getColumn().getDBColumnName();
      } else {
        inpColumnName = "inp" + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName());
      }
      columnValue = rq.getRequestParameter(inpColumnName);
    }
    if (columnValue == null || columnValue.equals("null")) {
      columnValue = "";
    }
    JSONObject jsnobject = new JSONObject();
    try {

      if (Constants.PURCHASE_REQUISITION_SECUREDFIELD.equals(field.getId())) {
        Field reqField = OBDal.getInstance().get(Field.class, Constants.PURCHASE_REQUISITION_ID);
        String requistionField = "inp"
            + Sqlc.TransformaNombreColumna(reqField.getColumn().getDBColumnName());
        Boolean isAllowed = SecuredStringDAO
            .isAllowedToDisplay(rq.getRequestParameter(requistionField));
        if (isAllowed) {
          jsnobject.put("value",
              new String(java.util.Base64.getDecoder().decode(columnValue.getBytes())));
          jsnobject.put("classicValue",
              new String(java.util.Base64.getDecoder().decode(columnValue.getBytes())));
        } else {
          jsnobject.put("value", "***");
          jsnobject.put("classicValue", "***");
        }
      }

    } catch (JSONException e) {
      log.error(
          "Couldn't get field property value for column " + field.getColumn().getDBColumnName());
    }
    return jsnobject.toString();
  }

  // disable hover as it would show useless raw-value
  @Override
  protected String getShowHoverGridFieldSettings(Field field) {
    return "";
  }

  @Override
  public String getGridFieldProperties(Field field) {
    return super.getGridFieldProperties(field) + ", canGroupBy: false";
  }

}
