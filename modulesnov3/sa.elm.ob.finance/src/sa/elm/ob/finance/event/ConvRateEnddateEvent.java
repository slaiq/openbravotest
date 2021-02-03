package sa.elm.ob.finance.event;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.model.common.currency.ConversionRate;

public class ConvRateEnddateEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ConversionRate.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onSave(@Observes EntityNewEvent event) {

    if (!isValidEvent(event)) {
      return;
    }
    final Property enddate = entities[0].getProperty(ConversionRate.PROPERTY_VALIDTODATE);

    String strDate = "2077-11-16";
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    try {
      Date edate = df.parse(strDate);
      event.setCurrentState(enddate, edate);
    } catch (Exception e) {
      log.error("Exception in ConversionRateEventHandler ", e);
    }
  }

}
