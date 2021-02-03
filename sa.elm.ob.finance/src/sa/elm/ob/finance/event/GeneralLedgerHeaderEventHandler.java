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
 * All portions are Copyright (C) 2012-2013 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  Qualian.
 ************************************************************************
 */

package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;

/**
 * 
 * @author sathishkumar.P
 * 
 */
public class GeneralLedgerHeaderEventHandler extends EntityPersistenceEventObserver {
  /**
   * This class is used to handle the events in C_AcctSchema
   */

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(AcctSchema.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();

      final OBQuery<AcctSchema> dimensions = OBDal.getInstance().createQuery(AcctSchema.class,
          "client.id='" + OBContext.getOBContext().getCurrentClient().getId() + "'");

      if (dimensions.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_GLConfig_onlyone"));
      }

    } catch (OBException e) {
      log.error(" Exception while saving in general ledger Header: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      AcctSchema dimension = (AcctSchema) event.getTargetInstance();
      if (dimension.isEfinIsready()) {
        throw new OBException(OBMessageUtils.messageBD("Efin_GLCannot_Delete"));
      }
    } catch (OBException e) {
      log.error("Exception while deleting in general ledger Header: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}