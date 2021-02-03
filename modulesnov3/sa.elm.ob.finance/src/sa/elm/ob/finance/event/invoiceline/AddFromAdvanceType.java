package sa.elm.ob.finance.event.invoiceline;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;

import sa.elm.ob.finance.EfinDistribution;
import sa.elm.ob.finance.efinDistributionLines;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;

/**
 * An event handler that inserts {@link InvoiceLine} based on the selected {@link EfinDistribution}
 * and {@link BusinessPartner}
 * 
 * @author Gopinagh.R
 */

public class AddFromAdvanceType extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Invoice.ENTITY_NAME) };

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

      final Entity invoiceEntity = ModelProvider.getInstance().getEntity(Invoice.ENTITY_NAME);

      Invoice invoice = (Invoice) event.getTargetInstance();
      final String strInvoiceType = PurchaseInvoiceSubmitUtils.getInvoiceType(invoice);
      Boolean isMultiEntity = Boolean.FALSE;
      List<efinDistributionLines> adjustmentAccounts = new ArrayList<efinDistributionLines>();

      isMultiEntity = AddFromAdvanceTypeDAO.isMultiEntity(invoice.getBusinessPartner());

      if ("PPI".equals(strInvoiceType) && !isMultiEntity) {
        adjustmentAccounts = AddFromAdvanceTypeDAO.getAdjustmentAccounts(invoice);

        if (adjustmentAccounts.size() > 0) {
          final Property invoiceLinesProperty = invoiceEntity
              .getProperty(Invoice.PROPERTY_INVOICELINELIST);
          @SuppressWarnings("unchecked")
          final List<Object> invoiceLines = (List<Object>) event
              .getCurrentState(invoiceLinesProperty);

          AddFromAdvanceTypeDAO.insertInvoiceLines(adjustmentAccounts, invoice, invoiceLines);
        }
      }

    } catch (Exception e) {
      log.error(" Exception while inserting invoice lines :" + e);
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @SuppressWarnings("unchecked")
  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();

      final Entity invoiceEntity = ModelProvider.getInstance().getEntity(Invoice.ENTITY_NAME);
      Boolean isMultiEntity = Boolean.FALSE;
      List<efinDistributionLines> adjustmentAccounts = new ArrayList<efinDistributionLines>();

      Invoice invoice = (Invoice) event.getTargetInstance();
      final String strInvoiceType = PurchaseInvoiceSubmitUtils.getInvoiceType(invoice);
      isMultiEntity = AddFromAdvanceTypeDAO.isMultiEntity(invoice.getBusinessPartner());

      final Property advanceType = entities[0].getProperty(Invoice.PROPERTY_EFINDISTRIBUTION);
      if ("PPI".equals(strInvoiceType) && !isMultiEntity) {
        if (!event.getPreviousState(advanceType).equals(event.getCurrentState(advanceType))) {
          List<InvoiceLine> invoiceLine = invoice.getInvoiceLineList();
          if (invoiceLine != null && invoiceLine.size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("Efin_cantchange_advancetype"));
          }

          Property invoiceLinesProperty = invoiceEntity
              .getProperty(Invoice.PROPERTY_INVOICELINELIST);
          @SuppressWarnings("unused")
          List<Object> invoiceLines = (List<Object>) event.getCurrentState(invoiceLinesProperty);

          adjustmentAccounts = AddFromAdvanceTypeDAO.getAdjustmentAccounts(invoice);

          if (adjustmentAccounts.size() > 0) {

            AddFromAdvanceTypeDAO.insertInvoiceLinesOnUpdate(adjustmentAccounts, invoice);
          }
        }
      }
    } catch (Exception e) {
      log.error(" Exception while inserting invoice lines :" + e);
      throw new OBException(e.getMessage());

    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
