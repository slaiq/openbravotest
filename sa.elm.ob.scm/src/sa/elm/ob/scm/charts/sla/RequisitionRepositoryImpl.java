package sa.elm.ob.scm.charts.sla;

import java.util.List;

import org.apache.log4j.Logger;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.procurement.Requisition;

public class RequisitionRepositoryImpl implements RequisitionRepository {

	private static final String CLOSED_STAGE = "CLS";
	private static final String APPROVED_STATUS = "ESCM_AP";
	private static final Logger logger = Logger.getLogger(RequisitionRepositoryImpl.class);

	/* (non-Javadoc)
	 * @see sa.elm.ob.finance.widget.RequisitionRepositoryInterface#getActiveRequisition()
	 */
	@Override
	public List<Requisition> getActiveRequisition() {

		OBQuery<Requisition> query = OBDal.getInstance().createQuery(Requisition.class,
				"as r where escmDocStatus =  :docStatus and r.escmStage != :stage and r.escmStage is NOT NULL )");
		query.setNamedParameter("docStatus", APPROVED_STATUS);
		query.setNamedParameter("stage", CLOSED_STAGE);
		logger.info("Number of requisitions to be processed for SLA Violation: " + query.list().size());
		return query.list();
	}
}
