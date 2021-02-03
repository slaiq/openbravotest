package sa.elm.ob.utility.tabadul;

import org.springframework.core.io.FileSystemResource;


/**
 * Interface Definition for Tabadul Integration
 * @author mrahim
 *
 */
public interface TabadulIntegrationService {
	
	/**
	 * This method intializes request and authenticates the user with tabadul.
	 * @param userEmail
	 * @param password
	 * @return
	 */
	String initializeRequest (String userEmail, String password);
	
	/**
	 * Create Tender 
	 * @param sessionToken
	 * @param tenderVO
	 * @return
	 */
	String createOrUpdateTender (String sessionToken, TenderVO tenderVO) throws Exception;
	
	/**
	 * Delete Tender
	 * @param sessionToken
	 * @param tenderVO
	 * @return
	 * @throws Exception
	 */
	String deleteTender (String tenderId, String sessionToken) throws Exception;
	
	/**
	 * Delete the Tender File
	 * @param tenderId
	 * @param fileId
	 * @param sessionToken
	 * @return
	 * @throws Exception
	 */
	String deleteTenderFile (String tenderId,String fileId, String sessionToken) throws Exception;
	
	/**
	 * Update Tender File
	 * @param tenderId
	 * @param fileType
	 * @param fileBytes
	 */
	String uploadTenderFile (String tenderId, String fileType , FileSystemResource fileSystemResource, String sessionToken) throws Exception;
	
	
	/**
	 * Publish the tender
	 * @param tenderId
	 * @param sessionToken
	 * @throws Exception
	 */
	String publishTender(String tenderId, String sessionToken) throws Exception;
	
	/**
	 * Cancel Tender 
	 * @param tenderId
	 * @param sessionToken
	 * @return
	 * @throws Exception
	 */
	String cancelTender(String tenderId, String sessionToken) throws Exception;
	
	/**
	 * Publish the Tender File
	 * @param tenderId
	 * @param tenderFileId
	 * @param sessionToken
	 * @return
	 * @throws Exception
	 */
	String publishTenderFile(String tenderId, String tenderFileId, String sessionToken)    throws Exception;
	
	/**
	 * Cancel Tender File
	 * @param tenderId
	 * @param tenderFileId
	 * @param sessionToken
	 * @return
	 * @throws Exception
	 */
	String cancelTenderFile(String tenderId, String tenderFileId, String sessionToken)      throws Exception;
	
	/**
	 * Get the Supplier Information by CR Number
	 * @param crNumber
	 * @param sessionToken
	 * @return
	 */
	TabadulResponse getSupplierByCR (String crNumber, String sessionToken) throws Exception;
	
	TabadulResponse getSupplierByCR(String crNumber, boolean withAddress, String sessionToken) throws Exception;
	
	/**
	 * Extend the tender dates
	 * @param tenderVO
	 * @return
	 */
	String extendTenderDates (TenderVO tenderVO, String sessionToken) throws Exception;
	
	/**
	 * Approve Extend Tender Dates
	 * @param aid
	 * @return
	 * @throws Exception
	 */
	String approveExtendTenderDates (String aid, String sessionToken) throws Exception;
	
	CountryLookupResponse getCountries(String sessionToken) throws Exception;
	/**
	 * Get the purchases for supplied tender id
	 * @param tenderId
	 * @return
	 */
	PurchasesVO getPurchasesForTender (String tabadulTenderId, String sessionToken) throws Exception;
	
	/**
	 * Get the
	 * @param Supplier Id
	 * @param sessionToken
	 * @return
	 * @throws Exception
	 */
	SupplierVO getSupplierById (String vendorId, String sessionToken) throws Exception;
}
