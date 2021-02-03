package org.openbravo.client.kernel.reference;

import java.util.Iterator;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.ad.ui.Field;

public class GridPropertiesHookCaller {

  @javax.inject.Inject
  @Any
  private Instance<GridPropertiesHook> hooks;

  public String executeHook(ConnectionProvider conn, Field field) throws Exception {
    String gridProperties = executeHooks(conn, field);
    return gridProperties;
  }

  private String executeHooks(ConnectionProvider conn, Field field) throws Exception {
    StringBuilder gridProperties = new StringBuilder();
    for (Iterator<GridPropertiesHook> procIter = hooks.iterator(); procIter.hasNext();) {
      GridPropertiesHook proc = procIter.next();
      gridProperties.append(proc.exec(conn, field));
    }
    return gridProperties.toString();
  }

}
