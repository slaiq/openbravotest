package org.openbravo.client.kernel.reference;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.ad.ui.Field;

/**
 * This class is used to define the extra grid properties
 * 
 * @author SathishKumar.P
 *
 */

public interface GridPropertiesHook {

  public String exec(ConnectionProvider conn, Field field) throws Exception;

}
