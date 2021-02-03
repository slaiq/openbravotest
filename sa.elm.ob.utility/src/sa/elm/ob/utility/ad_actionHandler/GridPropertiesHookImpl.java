package sa.elm.ob.utility.ad_actionHandler;

import org.openbravo.client.kernel.reference.GridPropertiesHook;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.ad.ui.Field;

/**
 * This class is used to calculate width of field in grid view
 * 
 * @author Sathishkumar.P
 *
 */
public class GridPropertiesHookImpl implements GridPropertiesHook {

  @Override
  public String exec(ConnectionProvider conn, Field field) throws Exception {

    if (field.isEutIswidthbasedonlength() != null && field.isEutIswidthbasedonlength()) {
      if (field.getEutGridlength() != null && field.getEutGridlength() > 0) {
        return " , displaybasedonwidth:true , gridlength:" + field.getEutGridlength() + " ";
      } else {
        return " , displaybasedonwidth:false ";
      }
    } else {
      return " , displaybasedonwidth:false ";
    }

  }

}
