package sa.elm.ob.scm.webservice.epm.service;

import java.math.BigDecimal;

import org.openbravo.model.common.order.Order;

public interface EpmService {

	void addProject(Order order, Integer cityProjectId, BigDecimal valueApprovedExtracts);

	void addExtractProject(Order order, Integer cityProjectId);
}
