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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;

import javax.inject.Inject;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.junit.AfterClass;
import org.junit.Test;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.Note;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.service.datasource.DataSourceService;
import org.openbravo.service.datasource.DataSourceServiceProvider;
import org.openbravo.service.json.JsonConstants;

/**
 * Test cases covering updates through standard datasource that include a persistence observer.
 * 
 * Observer is implemented in {@link OrderLineTestObserver}.
 * 
 * @author alostale
 *
 */
public class DatasourceEventObserver extends WeldBaseTest {

  @Inject
  private DataSourceServiceProvider dataSourceServiceProvider;

  enum ObserverExecutionType {
    OFF, UPDATE_DESCRIPTION, CREATE_NOTE, COUNT_LINES, UPDATE_PARENT
  };

  static ObserverExecutionType observerExecutionType = ObserverExecutionType.OFF;

  /** Updating order line without observer */
  @Test
  public void standardUpdateRequestWithoutObserver() throws JSONException {
    observerExecutionType = ObserverExecutionType.OFF;

    OrderLine ol = pickARandomOrderLine();

    String randomDescription = Long.toString(System.currentTimeMillis());
    JSONObject updatedOrder = datasourceUpdate(ol, randomDescription);

    assertDescription(ol, updatedOrder, randomDescription);
  }

  /** The observer updates line description to a fixed value */
  @Test
  public void observerCanModifyPropertyValues() throws JSONException {
    observerExecutionType = ObserverExecutionType.UPDATE_DESCRIPTION;

    OrderLine ol = pickARandomOrderLine();

    String randomDescription = Long.toString(System.currentTimeMillis());
    JSONObject updatedOrder = datasourceUpdate(ol, randomDescription);

    assertDescription(ol, updatedOrder, OrderLineTestObserver.FORCED_DESCRIPTION);
  }

  /** The observer creates a new note on order line */
  @Test
  public void observerCanAddNewObjects() throws JSONException {
    observerExecutionType = ObserverExecutionType.CREATE_NOTE;

    OrderLine ol = pickARandomOrderLine();
    int notesBeforeUpdate = countNotes(ol);

    String randomDescription = Long.toString(System.currentTimeMillis());
    JSONObject updatedOrder = datasourceUpdate(ol, randomDescription);

    int notesAfterUpdate = countNotes(ol);

    assertThat("number of notes", notesAfterUpdate, is(notesBeforeUpdate + 1));
    assertDescription(ol, updatedOrder, randomDescription);
  }

  /** Observer does ol.getSalesOrder().getOrderLineList(). Covers issue #32308 */
  @Test
  public void observerCanInstantiateObservedObject() throws JSONException {
    observerExecutionType = ObserverExecutionType.COUNT_LINES;

    OrderLine ol = pickARandomOrderLine();
    int numberOfLines = ol.getSalesOrder().getOrderLineList().size();

    String randomDescription = Long.toString(System.currentTimeMillis());
    JSONObject updatedOrder = datasourceUpdate(ol, randomDescription);

    assertDescription(ol, updatedOrder, OrderLineTestObserver.FORCED_DESCRIPTION + numberOfLines);
  }

  /** Observer updates order line's header */
  @Test
  public void observerCanUpdateParentObject() throws JSONException {
    observerExecutionType = ObserverExecutionType.UPDATE_PARENT;

    OrderLine ol = pickARandomOrderLine();

    String randomDescription = Long.toString(System.currentTimeMillis());
    datasourceUpdate(ol, randomDescription);

    Order order = ol.getSalesOrder();

    assertThat(order.getEntityName() + " - " + order.getId() + " actual description",
        (String) order.get(OrderLine.PROPERTY_DESCRIPTION),
        is(OrderLineTestObserver.FORCED_DESCRIPTION));
  }

  private OrderLine pickARandomOrderLine() {
    return (OrderLine) OBDal.getInstance().createCriteria(OrderLine.class).setMaxResults(1)
        .uniqueResult();
  }

  private JSONObject datasourceUpdate(OrderLine ol, String randomDescription) throws JSONException {
    final DataSourceService dataSource = dataSourceServiceProvider
        .getDataSource(OrderLine.ENTITY_NAME);

    JSONObject data = new JSONObject();
    data.put(JsonConstants.ID, ol.getId());
    data.put(OrderLine.PROPERTY_DESCRIPTION, randomDescription);

    JSONObject content = new JSONObject();
    content.put(JsonConstants.DATA, data);

    String resp = dataSource.update(new HashMap<String, String>(), content.toString());
    return new JSONObject(resp).getJSONObject(JsonConstants.RESPONSE_RESPONSE)
        .getJSONArray(JsonConstants.DATA).getJSONObject(0);
  }

  private void assertDescription(BaseOBObject obj, JSONObject updatedOrder,
      String expectedDescription) throws JSONException {
    OBDal.getInstance().commitAndClose();

    BaseOBObject refreshedBob = (BaseOBObject) OBDal.getInstance()
        .createCriteria(obj.getEntityName()).add(Restrictions.eq(BaseOBObject.ID, obj.getId()))
        .uniqueResult();
    assertThat(obj.getEntityName() + " - " + obj.getId() + " response description",
        updatedOrder.getString("description"), is(expectedDescription));
    assertThat(obj.getEntityName() + " - " + obj.getId() + " actual description",
        (String) refreshedBob.get(OrderLine.PROPERTY_DESCRIPTION), is(expectedDescription));
  }

  private int countNotes(BaseOBObject obj) {
    OBContext.setAdminMode(true);
    try {
      OBCriteria<Note> q = OBDal.getInstance().createCriteria(Note.class);
      q.add(Restrictions.eq(Note.PROPERTY_RECORD, obj.getId()));
      q.add(Restrictions.eq(Note.PROPERTY_TABLE,
          OBDal.getInstance().getProxy(Table.class, obj.getEntity().getTableId())));
      return q.count();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @AfterClass
  public static void reset() {
    observerExecutionType = ObserverExecutionType.OFF;
  }
}
