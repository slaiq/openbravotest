package sa.elm.ob.utility.sadad.consumer;
/**
 * Service Interface for Sadad Integration
 * @author mrahim
 *
 */


import sa.elm.ob.utility.sadad.consumer.org.tempuri.AddSaddadBillRequest;
import sa.elm.ob.utility.sadad.consumer.org.tempuri.AddSaddadBillResponse;
import sa.elm.ob.utility.sadad.consumer.org.tempuri.DeleteSaddadBillRequest;
import sa.elm.ob.utility.sadad.consumer.org.tempuri.DeleteSaddadBillResponse;
import sa.elm.ob.utility.sadad.consumer.org.tempuri.GetSaddadBillRequest;
import sa.elm.ob.utility.sadad.consumer.org.tempuri.GetSaddadBillResponse;

public interface SadadIntegrationService {
	/**
	 * Creates a new Bill in MOT Sadad
	 * @param saddadBillRequest
	 * @return
	 */
	AddSaddadBillResponse createNewBill (AddSaddadBillRequest saddadBillRequest);
	/**
	 * Get the Status of Bill 
	 * @param saddadBillRequest
	 * @return
	 */
	GetSaddadBillResponse getSadadBillStatus (GetSaddadBillRequest saddadBillRequest);
	
	/**
	 * Deletes the Bill in MOT Sadad
	 * @param deleteSaddadBillRequest
	 * @return
	 */
	DeleteSaddadBillResponse deleteBill (DeleteSaddadBillRequest deleteSaddadBillRequest);
}
