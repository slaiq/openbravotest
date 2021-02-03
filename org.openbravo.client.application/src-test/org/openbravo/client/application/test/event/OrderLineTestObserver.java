/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.test.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.Note;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;

/**
 * Test persistence observer used by {@link DatasourceEventObserver} to ensure observer is correctly
 * invoked and works fine together with datasource update invocations.
 * 
 * @author alostale
 *
 */
public class OrderLineTestObserver extends EntityPersistenceEventObserver {
  static final String FORCED_DESCRIPTION = "test description";
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(OrderLine.ENTITY_NAME) };

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    switch (DatasourceEventObserver.observerExecutionType) {
    case OFF:
      return;
    case UPDATE_DESCRIPTION:
      event.setCurrentState(entities[0].getProperty(OrderLine.PROPERTY_DESCRIPTION),
          FORCED_DESCRIPTION);
      break;
    case CREATE_NOTE:
      final OrderLine orderLine = (OrderLine) event.getTargetInstance();
      Note newNote = OBProvider.getInstance().get(Note.class);
      newNote.setTable(OBDal.getInstance()
          .getProxy(Table.class, orderLine.getEntity().getTableId()));
      newNote.setRecord(orderLine.getId());
      newNote.setNote("test");
      OBDal.getInstance().save(newNote);
      break;
    case COUNT_LINES:
      int numOfLines = ((OrderLine) event.getTargetInstance()).getSalesOrder().getOrderLineList()
          .size();
      event.setCurrentState(entities[0].getProperty(OrderLine.PROPERTY_DESCRIPTION),
          FORCED_DESCRIPTION + numOfLines);
      break;
    case UPDATE_PARENT:
      Order order = ((OrderLine) event.getTargetInstance()).getSalesOrder();
      order.setDescription(FORCED_DESCRIPTION);
      break;
    }
  }

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }
}
