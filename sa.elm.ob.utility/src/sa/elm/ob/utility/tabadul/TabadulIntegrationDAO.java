package sa.elm.ob.utility.tabadul;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.openbravo.model.common.businesspartner.BusinessPartner;

import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.Escmbidsuppliers;
import sa.elm.ob.scm.Escmsalesvoucher;

/**
 * Interface for all tabadul related DB Operations
 * @author mrahim
 *
 */
public interface TabadulIntegrationDAO {
	/**
	 * Get the tender information and puts in Tender VO
	 * @param bidManagementId
	 * @return
	 * @throws SQLException
	 */
	TenderVO getTenderInformation (String bidManagementId, String status)  throws SQLException;
	
	/**
	 * Get the tenders information for Annoucement Summary
	 * @param bidManagementId
	 * @param status
	 * @return
	 * @throws SQLException
	 */
	List<TenderVO> getTendersForAnnouncement (String announcementId, String status)  throws SQLException;
	
	/**
	 * Get the concated address value as per tabadul format
	 * @param addressId
	 * @return
	 * @throws SQLException
	 */
	String getAddressInformation (String addressId)  throws SQLException;
	
	/**
	 * Update the tender id in bid
	 * @param tenderId
	 * @param bidNumber
	 * @throws SQLException
	 */
	void updateTenderIdInBid (String tenderId, String status ,String bidNumber) throws SQLException;
	
	/**
	 * Get all the attachments for Bid
	 * @param bidManagementId
	 * @param tableId
	 * @return
	 * @throws SQLException
	 */
	List <AttachmentVO> getAttachments (String bidManagementId, String tableId, String status) throws SQLException;
	
	/**
	 * Update the File Status
	 * @param status
	 * @param fileId
	 */
	void updateTabadulFileStatus (String status, String fileId, String tabadulAttachmentId)throws Exception;
	
	/**
	 * Update status of all files to published
	 * @return
	 */
	void updateTenderFileStatus (String bidManagementId, String tableId) throws Exception;
	
	/**
	 * 
	 * @param cFileId
	 * @param description
	 */
	void updateAttachmentDescription (String cFileId, String description) throws Exception;
	
	/**
	 * Get the tender by Bid Management Id
	 * @param bidManagementId
	 * @return
	 */
	String getTenderIdByBidManagementId (String bidManagementId) throws Exception;
	
	/**
	 * 
	 * Get the Purchases for Bids from tabadul till a specfic date
	 * @param tabadulStatus
	 * @return
	 */
	List<EscmBidMgmt> getPurchasesForBidInTabadul (String tabadulStatus, Date bidProposalLastDay);
	
	/**
	 * Check if the provided vendor id is registered as Business Partner
	 * @param vendorId
	 * @return
	 */
	BusinessPartner getBusinessPartner(String vendorId);
	
	/**
	 * 
	 * @param escmsalesvoucher
	 */
	void saveRfpSalesRecord (Escmsalesvoucher escmsalesvoucher);
	
	/**
	 * Save the Bid Supplier record
	 * @param escmbidsuppliers
	 */
	void saveBidSupplierRecord (Escmbidsuppliers escmbidsuppliers);
	
	/**
	 * Checks if the sales record already exists
	 * @param escmBidMgmtId
	 * @param supplierNumber
	 * @param clientId
	 * @return
	 */
	Boolean checkIfSalesRecordExists (String escmBidMgmtId, String supplierNumber);
	
	/**
	 * Check if supplier record already exists 
	 * @param supplierNumber
	 * @param branchName
	 * @param escmBidMgmtId
	 * @return
	 */
	Boolean checkIfSupplierRecordAlreadyExists (String supplierNumber, String branchName, String escmBidMgmtId);
}
