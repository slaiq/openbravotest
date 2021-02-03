package sa.elm.ob.utility.tabadul.services;

import sa.elm.ob.scm.actionHandler.SupplierController;
import sa.elm.ob.utility.tabadul.TabadulAuthenticationService;
import sa.elm.ob.utility.tabadul.TabadulIntegrationDAO;
import sa.elm.ob.utility.tabadul.TabadulIntegrationService;

/**
 * Interface service for BidPurchasesUpdate
 * @author mrahim
 *
 */
public interface BidPurchasesUpdateService {
	
	/**
	 * Update all the purchases made for bids in tabadul
	 */
	void updateBidPurchasesFromTabadul ();
	
	/**
	 * setter for tabadul Integration DAO
	 */
	void setTabadulIntegrationDAO (TabadulIntegrationDAO tabadulIntegrationDAO);
	
	/**
	 * setter for tabadul integration service
	 * @param tabadulIntegrationService
	 */
	void setTabadulIntegrationService (TabadulIntegrationService tabadulIntegrationService);
	
	/**
	 * Setter for Tabadul Authentication Service
	 * @param tabadulAuthenticationService
	 */
	void setTabadulAuthenticationService (TabadulAuthenticationService tabadulAuthenticationService);
	
	/**
	 * Set the supplier controller dependency
	 * @param supplierController
	 */
	void setSupplierController (SupplierController supplierController);
}
