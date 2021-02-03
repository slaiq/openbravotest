package sa.elm.ob.scm.charts.sla;

import java.util.List;

import org.openbravo.model.procurement.Requisition;

public interface RequisitionRepository {

	/**
	 * 
	 * @return list of Requisition with: 1) Approved status and Null Stag -> BR
	 *         approved but not assigned to next stage 2) Approved status and not
	 *         Closed stage -> BR in progress and not closed yet
	 */
	List<Requisition> getActiveRequisition();

}