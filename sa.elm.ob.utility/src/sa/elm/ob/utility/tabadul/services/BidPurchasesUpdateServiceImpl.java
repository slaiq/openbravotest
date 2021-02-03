package sa.elm.ob.utility.tabadul.services;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCM_Certificates;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.Escmbidsuppliers;
import sa.elm.ob.scm.Escmsalesvoucher;
import sa.elm.ob.scm.actionHandler.SupplierController;
import sa.elm.ob.utility.tabadul.ItemVO;
import sa.elm.ob.utility.tabadul.PurchasesVO;
import sa.elm.ob.utility.tabadul.SupplierVO;
import sa.elm.ob.utility.tabadul.TabadulAuthenticationService;
import sa.elm.ob.utility.tabadul.TabadulAuthenticationServiceImpl;
import sa.elm.ob.utility.tabadul.TabadulIntegrationDAO;
import sa.elm.ob.utility.tabadul.TabadulIntegrationDAOImpl;
import sa.elm.ob.utility.tabadul.TabadulIntegrationService;
import sa.elm.ob.utility.tabadul.TabadulIntegrationServiceImpl;
import sa.elm.ob.utility.tabadul.TenderStatusE;
import sa.elm.ob.utility.tabadul.TenderTypeE;
import sa.elm.ob.utility.tabadul.util.BillTypeE;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author mrahim
 *
 */
public class BidPurchasesUpdateServiceImpl implements BidPurchasesUpdateService {

  private TabadulIntegrationDAO tabadulIntegrationDAO;
  private TabadulIntegrationService tabadulIntegrationService;
  private TabadulAuthenticationService tabadulAuthenticationService;
  private SupplierController supplierController;// should have been some service may be we will
                                                // refactor in future
  private static final Logger log = LoggerFactory.getLogger(BidPurchasesUpdateServiceImpl.class);

  @Override
  public void updateBidPurchasesFromTabadul() {
    log.info("<---- START updateBidPurchasesFromTabadul--->");
    PurchasesVO purchasesVO = null;
    // Get the list of bids which are published to tabadul
    List<EscmBidMgmt> listOfBidsPublished = tabadulIntegrationDAO
        .getPurchasesForBidInTabadul(TenderStatusE.PUBLISHED.getStatus(), new Date());
    log.info("Number of Bids in Published State--->" + listOfBidsPublished.size());
    // Get the authentication token from tabadul
    String sessionToken = tabadulAuthenticationService.authenticate();
    log.info("<---- Got Tabadul Authentication Token--->");
    // Loop through bids
    for (EscmBidMgmt escmBidMgmt : listOfBidsPublished) {
      log.info("<---- START Processing Bid Id : " + escmBidMgmt.getId());
      // Call the tabadul list purchases to get purchases
      try {
        purchasesVO = tabadulIntegrationService
            .getPurchasesForTender(String.valueOf(escmBidMgmt.getTabadulTenderID()), sessionToken);
        log.info("Purchases Object Received : " + purchasesVO);
        // Loop through all the purchases
        for (ItemVO purchase : purchasesVO.getItems()) {
          log.info(" <--- Check if Supplier exists as a business partner -->");
          // Check if the vendor exists in business partner or not
          BusinessPartner businessPartner = tabadulIntegrationDAO
              .getBusinessPartner(String.valueOf(purchase.getVendorId()));
          if (null == businessPartner) {
            log.info("<---Business Partner not found so Register Vendor--->");
            // Register the vendor as business partner
            businessPartner = registerVendorAsPartner(purchase.getVendorId(), sessionToken);
          }
          // check if the tender type is tender
          if (escmBidMgmt.getBidtype().trim().equals(TenderTypeE.TENDER.getObType())) {
            log.info("<---- START Bid Type Tender Processing--->");
            if (!tabadulIntegrationDAO.checkIfSalesRecordExists(escmBidMgmt.getId(),
                businessPartner.getEfinDocumentno())) {
              // Insert the data from tabadul in RFP Sales
              insertToRFPSales(purchase, escmBidMgmt, businessPartner);
            } else {
              log.error(" Sales RFP record already exists  ");
            }
          } else if (escmBidMgmt.getBidtype().trim().equals(TenderTypeE.DIRECT.getObType())) {
            log.info("<---- START Bid Type Direct Processing--->");
            Location location = getLatestLocation(businessPartner);
            if (!tabadulIntegrationDAO.checkIfSupplierRecordAlreadyExists(
                businessPartner.getSearchKey(), location.getName(), escmBidMgmt.getId())) {
              // Insert the data in bid window suppliers tab
              insertToBidSuppliers(escmBidMgmt, businessPartner, location);
            } else {
              log.error(" Supplier record already exists  ");
            }
          }
        }
        OBDal.getInstance().flush();
      } catch (Exception e) {
        log.error("<---Exception in Bid Processing-->", e.getMessage());
        OBDal.getInstance().rollbackAndClose();
      } finally {
        OBDal.getInstance().commitAndClose();
      }

    }
    log.info("<---- END updateBidPurchasesFromTabadul--->");

  }

  /**
   * Register the vendor as business partner
   * 
   * @param vendorId
   * @param sessionToken
   */
  private BusinessPartner registerVendorAsPartner(Integer vendorId, String sessionToken)
      throws Exception {
    // get the supplier
    SupplierVO supplierVO = tabadulIntegrationService.getSupplierById(String.valueOf(vendorId),
        sessionToken);
    // Insert the business partner
    return supplierController.importSupplierByCRN(supplierVO.getCr_number());

  }

  /**
   * Insert to RFP Sales
   * 
   * @param itemVO
   */
  private void insertToRFPSales(ItemVO purchase, EscmBidMgmt escmBid,
      BusinessPartner businessPartner) {
    Escmsalesvoucher escmsalesvoucher = new Escmsalesvoucher();

    escmsalesvoucher.setClient(escmBid.getClient());
    escmsalesvoucher.setOrganization(escmBid.getOrganization());

    escmsalesvoucher.setEscmBidmgmt(escmBid);
    escmsalesvoucher.setBidName(escmBid.getBidname());
    escmsalesvoucher.setRFPPriceSAR(escmBid.getRfpprice());
    escmsalesvoucher.setSalesdate(Utility.unixTimeStampToJava(purchase.getPurchaseDate()));
    escmsalesvoucher.setSupplierNumber(businessPartner);
    escmsalesvoucher.setSupplier(businessPartner.getName());

    // check for location attributes
    escmsalesvoucher
        .setPaymenttype(purchase.getSadadBillNo() != null ? BillTypeE.SADAD.getBillType()
            : BillTypeE.CHEQUE.getBillType());
    escmsalesvoucher.setPaymentDocno(purchase.getSadadBillNo());
    escmsalesvoucher.setAmountsar(escmBid.getRfpprice());
    // Document Dates need to be checked
    String date = Utility.convertGregDatePattern("yyyy-MM-dd", escmsalesvoucher.getSalesdate());
    escmsalesvoucher.setDocumentdategreg(date);
    escmsalesvoucher.setDocumentdateh(escmsalesvoucher.getSalesdate());
    // Fill the location
    Location location = getLatestLocation(businessPartner);
    escmsalesvoucher.setSupplierPhone(location.getPhone());
    escmsalesvoucher.setFax(location.getFax());
    escmsalesvoucher.setBranchName(location);

    ESCM_Certificates certificate = getCertificatesData(businessPartner);
    escmsalesvoucher.setCommercialRegisteryNo(certificate.getCertificateNumber());
    tabadulIntegrationDAO.saveRfpSalesRecord(escmsalesvoucher);

  }

  /**
   * Get the latest location from Business Partner
   * 
   * @param businessPartner
   * @return
   */
  private Location getLatestLocation(BusinessPartner businessPartner) {
    List<Location> locations = businessPartner.getBusinessPartnerLocationList();
    // Sort the collection and return the last one
    Collections.sort(locations, new Comparator<Location>() {
      public int compare(Location o1, Location o2) {
        return o1.getCreationDate().compareTo(o2.getCreationDate());
      }
    });

    if (locations.size() > 0) {
      return locations.get(locations.size() - 1);
    }

    return null;
  }

  /**
   * Fill the certificates data
   * 
   * @param escmsalesvoucher
   * @param businessPartner
   */
  private ESCM_Certificates getCertificatesData(BusinessPartner businessPartner) {
    List<ESCM_Certificates> certificatesList = businessPartner.getESCMCertificatesList();
    Collections.sort(certificatesList, new Comparator<ESCM_Certificates>() {
      public int compare(ESCM_Certificates o1, ESCM_Certificates o2) {
        return o1.getCreationDate().compareTo(o2.getCreationDate());
      }
    });

    if (certificatesList.size() > 0) {
      return certificatesList.get(certificatesList.size() - 1);
    }

    return null;
  }

  /**
   * Insert to bid suppliers tab
   * 
   * @param purchase
   * @param escmBidmgmt
   * @param businessPartner
   */
  private void insertToBidSuppliers(EscmBidMgmt escmBidmgmt, BusinessPartner businessPartner,
      Location location) {
    Escmbidsuppliers escmbidsupplier = new Escmbidsuppliers();
    escmbidsupplier.setEscmBidmgmt(escmBidmgmt);
    escmbidsupplier.setSuppliernumber(businessPartner);
    // Get Location from business partner

    escmbidsupplier.setBranchname(location);
    escmbidsupplier.setSupplierphone(location.getPhone());
    escmbidsupplier.setSupplierfax(location.getFax());
    escmbidsupplier.setSupplier(businessPartner.getName());
    escmbidsupplier.setLineNo(30L);// Need to check about this property

    tabadulIntegrationDAO.saveBidSupplierRecord(escmbidsupplier);
  }

  @Override
  public void setTabadulIntegrationDAO(TabadulIntegrationDAO tabadulIntegrationDAO) {
    this.tabadulIntegrationDAO = tabadulIntegrationDAO;

  }

  @Override
  public void setTabadulIntegrationService(TabadulIntegrationService tabadulIntegrationService) {
    this.tabadulIntegrationService = tabadulIntegrationService;

  }

  @Override
  public void setTabadulAuthenticationService(
      TabadulAuthenticationService tabadulAuthenticationService) {
    this.tabadulAuthenticationService = tabadulAuthenticationService;

  }

  public static void main(String[] args) throws Exception {
    BidPurchasesUpdateServiceImpl bidPurchasesUpdateServiceImpl = new BidPurchasesUpdateServiceImpl();
    bidPurchasesUpdateServiceImpl
        .setTabadulAuthenticationService(new TabadulAuthenticationServiceImpl());
    bidPurchasesUpdateServiceImpl.setTabadulIntegrationDAO(new TabadulIntegrationDAOImpl());
    bidPurchasesUpdateServiceImpl.setTabadulIntegrationService(new TabadulIntegrationServiceImpl());

    bidPurchasesUpdateServiceImpl.updateBidPurchasesFromTabadul();

  }

  @Override
  public void setSupplierController(SupplierController supplierController) {

    this.supplierController = supplierController;

  }
}
