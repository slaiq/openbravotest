package sa.elm.ob.finance.event;

import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.gl.GLJournal;

import sa.elm.ob.utility.EutJournalApproval;

public class DeleteGLApprovalHist extends EntityPersistenceEventObserver {
	protected Logger log = Logger.getLogger(this.getClass());
	private static Entity[] entities = { ModelProvider.getInstance().getEntity(GLJournal.ENTITY_NAME) };

	@Override
	protected Entity[] getObservedEntities() {
		// TODO Auto-generated method stub
		return entities;
	}

	public void onDelete(@Observes EntityDeleteEvent event) {
		if(!isValidEvent(event)) {
			return;
		}
		try {
			GLJournal journal = OBDal.getInstance().get(GLJournal.class, event.getTargetInstance().getId());
			List<EutJournalApproval> approvalsList = journal.getEUTJOURNALAPPROVALList();

			if(approvalsList.size() > 0) {
				for (EutJournalApproval approval : approvalsList) {
					OBDal.getInstance().remove(approval);
				}

			}
		}
		catch (Exception e) {
			log.error(" Exception while Delete record in Journal Header: " + e);
			throw new OBException(e.getMessage());
		}
	}
}
