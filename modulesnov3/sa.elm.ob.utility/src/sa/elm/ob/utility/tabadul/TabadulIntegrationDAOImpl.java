package sa.elm.ob.utility.tabadul;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.Escmbidsuppliers;
import sa.elm.ob.scm.Escmsalesvoucher;
import sa.elm.ob.utility.util.Utility;

public class TabadulIntegrationDAOImpl implements TabadulIntegrationDAO {
  private Connection connection = null;
  private static String SEPARATOR = ":";
  private static String GAT_CONFIG_PARAM = "tabadul.gatid";
  private static String gregDateFormat = "dd/MM/yyyy";
  private Properties poolPropertiesConfig;
  private static final Logger log = LoggerFactory.getLogger(TabadulIntegrationDAOImpl.class);

  public TabadulIntegrationDAOImpl() {
    connection = getDbConnection();
  }

  @Override
  public TenderVO getTenderInformation(String bidManagementId, String status) throws SQLException {

    connection.setAutoCommit(true);

    TenderVO tenderVO = null;

    String tenderQuery = " SELECT B.bidtype,B.bidname, B.bidno,B.bidpurpose, B.rfpprice , B.submissionadd , B.envelopeadd, BD.quelastdate, BD.proposallastday,"
        + " BD.proposallastdaytime, BD.openenvday , BD.openenvdaytime , B.executionadd, B.documentaddress ,B.ESCM_BIDMGMT_ID , B.tabadulbidno , B.TABADUL_STATUS "
        + " FROM   ESCM_BIDMGMT B " + ", ESCM_BIDDATES BD "
        + " WHERE B.ESCM_BIDMGMT_ID = BD.ESCM_BIDMGMT_ID "
        + " and B.ESCM_BIDMGMT_ID = ? and BD.ISACTIVE = 'Y'";
    if (null != status) {
      tenderQuery += " AND B.TABADUL_STATUS = ? ";
    }

    Integer index = 0;
    PreparedStatement ps = null;
    ResultSet rs = null;

    try {
      ps = connection.prepareStatement(tenderQuery);
      ps.setString(++index, bidManagementId);
      if (null != status)
        ps.setString(++index, status);

      rs = ps.executeQuery();

      while (rs.next()) {
        tenderVO = new TenderVO();
        // fill step 1
        setTenderInformation(rs, tenderVO);

      }
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      throw e;
    }

    return tenderVO;
  }

  @Override
  public List<TenderVO> getTendersForAnnouncement(String announcementId, String status)
      throws SQLException {

    connection.setAutoCommit(true);
    TenderVO tenderVO = null;
    List<TenderVO> tendersList = new ArrayList<TenderVO>();

    String tenderQuery = " SELECT B.bidtype,B.bidname, B.bidno,B.bidpurpose, B.rfpprice , B.submissionadd , B.envelopeadd, BD.quelastdate, BD.proposallastday,"
        + " BD.proposallastdaytime, BD.openenvday , BD.openenvdaytime , B.executionadd, B.documentaddress ,B.ESCM_BIDMGMT_ID , B.tabadulbidno , B.TABADUL_STATUS "
        + " FROM   ESCM_BIDMGMT B " + ", ESCM_BIDDATES BD " + " , ESCM_ANNOUNCEMENTS A"
        + " WHERE B.ESCM_BIDMGMT_ID = BD.ESCM_BIDMGMT_ID"
        + " AND A.ESCM_BIDMGMT_ID = B.ESCM_BIDMGMT_ID " + " AND escm_annoucements_id = ?";
    if (null != status) {
      tenderQuery += " AND B.TABADUL_STATUS = ? ";
    }

    Integer index = 0;
    PreparedStatement ps = null;
    ResultSet rs = null;

    try {
      ps = connection.prepareStatement(tenderQuery);
      ps.setString(++index, announcementId);
      if (null != status)
        ps.setString(++index, status);

      rs = ps.executeQuery();

      while (rs.next()) {
        tenderVO = new TenderVO();
        // fill step 1
        setTenderInformation(rs, tenderVO);
        tendersList.add(tenderVO);

      }
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      throw e;
    }

    return tendersList;
  }

  @Override
  public String getAddressInformation(String addressId) throws SQLException {
    String address = "";

    String tenderQuery = " 	SELECT concat_ws(', ', COALESCE( NULLIF(CL.ADDRESS1,'') , '' ), COALESCE( NULLIF(CL.ADDRESS2,'') , '' ) , CR.NAME ,CT.NAME )"
        + " FROM ESCM_LOCATION EL "
        + " LEFT JOIN C_LOCATION CL ON EL.C_LOCATION_ID = CL.C_LOCATION_ID"
        + " LEFT JOIN C_CITY CT ON  EL.C_CITY_ID = CT.C_CITY_ID"
        + " LEFT JOIN C_REGION CR ON  EL.C_REGION_ID = CR.C_REGION_ID"
        + " WHERE ESCM_LOCATION_ID = ? ";

    PreparedStatement ps = null;
    ResultSet rs = null;

    try {
      ps = connection.prepareStatement(tenderQuery);
      ps.setString(1, addressId);

      rs = ps.executeQuery();

      while (rs.next()) {
        // fill step 1
        address = rs.getString(1);

      }
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      throw e;
    }

    return address;

  }

  /**
   * get the bid categories for Bid
   * 
   * @param bidManagementId
   * @return
   * @throws SQLException
   */
  public List<Integer> getBidCategories(String bidManagementId) throws SQLException {
    List<Integer> bidCategories = new ArrayList<Integer>();

    String tenderQuery = "  SELECT df.value from  escm_bid_categories bc,escm_deflookups_typeln df "
        + "  WHERE bc.escm_deflookups_typeln_id = df.escm_deflookups_typeln_id AND bc.ESCM_BIDMGMT_ID = ?";

    PreparedStatement ps = null;
    ResultSet rs = null;

    try {
      ps = connection.prepareStatement(tenderQuery);
      ps.setString(1, bidManagementId);

      rs = ps.executeQuery();

      while (rs.next()) {
        // fill step 1
        bidCategories.add(rs.getInt(1));

      }
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      throw e;
    }

    return bidCategories;

  }

  @Override
  public void updateTabadulFileStatus(String status, String fileId, String tabadulAttachmentId)
      throws Exception {

    String query = " UPDATE escm_tabadul_attachments SET STATUS = ?  ";

    if (null != fileId) {
      query += " , tabadul_file_no = ? ";
    }
    Integer index = 0;
    query += " WHERE escm_tabadul_attachments_id = ? ";
    PreparedStatement ps = null;
    try {
      ps = getDbConnection().prepareStatement(query);

      ps.setString(++index, status);
      if (null != fileId)
        ps.setString(++index, fileId);
      ps.setString(++index, tabadulAttachmentId);

      ps.executeUpdate();
    } catch (Exception e) {
      throw e;
    } finally {
      try {
        if (ps != null) {
          ps.close();
        }
      } catch (Exception e) {

      }

    }

  }

  @Override
  public List<AttachmentVO> getAttachments(String bidManagementId, String tableId, String status)
      throws SQLException {
    log.info("GET ATTACHMENTS ---> Parameters ---> bidId : " + bidManagementId + " Table id : "
        + tableId + " Status : " + status);
    List<AttachmentVO> attachementVOList = new ArrayList<AttachmentVO>();

    String attachementQuery = " SELECT NAME,PATH, escm_tabadul_attachments_id, TABADUL_FILE_NO,CF.C_FILE_ID  FROM C_FILE CF , escm_tabadul_attachments E WHERE "
        + " CF.C_FILE_ID =  E.C_FILE_ID  AND " + "  CF.AD_TABLE_ID = ? and CF.AD_RECORD_ID = ?";
    if (null != status) {
      attachementQuery += " AND E.STATUS = ? ";
    }

    log.info("GET ATTACHMENTS ---> Query : " + attachementQuery);
    PreparedStatement ps = null;
    ResultSet rs = null;
    AttachmentVO attachmentVO = null;
    try {
      ps = connection.prepareStatement(attachementQuery);
      ps.setString(1, tableId);
      ps.setString(2, bidManagementId);
      if (null != status) {
        ps.setString(3, status);
      }

      rs = ps.executeQuery();

      while (rs.next()) {
        attachmentVO = new AttachmentVO();
        attachmentVO.setFileName(rs.getString(1));
        attachmentVO.setFilePath(rs.getString(2));
        attachmentVO.setTabadulAttachmentId(rs.getString(3));
        attachmentVO.setFid(rs.getString(4));
        attachmentVO.setcFileId(rs.getString(5));

        attachementVOList.add(attachmentVO);

      }
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      throw e;
    }

    return attachementVOList;
  }

  @Override
  public String getTenderIdByBidManagementId(String bidManagementId) throws Exception {

    String tenderId = null;
    String getTenderQuery = " SELECT TABADULBIDNO FROM ESCM_BIDMGMT WHERE escm_bidmgmt_id = ? ";

    log.info("GET ATTACHMENTS ---> Query : " + getTenderQuery);
    PreparedStatement ps = null;
    ResultSet rs = null;

    try {
      ps = connection.prepareStatement(getTenderQuery);
      ps.setString(1, bidManagementId);

      rs = ps.executeQuery();

      while (rs.next()) {
        tenderId = rs.getString(1);
      }
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      throw e;
    }

    return tenderId;
  }

  /**
   * Set the tender Information
   * 
   * @param rs
   * @param tenderVO
   * @throws SQLException
   */
  private void setTenderInformation(ResultSet rs, TenderVO tenderVO) throws SQLException {

    setBasicInformation(rs, tenderVO);
    setTenderDates(rs, tenderVO);
    setTaxonomyLocation(rs, tenderVO);
    setTenderFiles(rs, tenderVO);
    tenderVO.setBidId(rs.getString(15));
    tenderVO.setStatus(rs.getString(17));
    if (null != rs.getString(16)) {
      tenderVO.setTenderInternalId(Integer.parseInt(rs.getString(16)));
    }

    setTenderCategories(tenderVO);

  }

  /**
   * Set the Basic Information for Tender
   * 
   * @param rs
   * @param tenderVO
   * @throws SQLException
   */
  private void setBasicInformation(ResultSet rs, TenderVO tenderVO) throws SQLException {
    tenderVO.getBasicInfo().setTenderType(rs.getString(1));
    tenderVO.getBasicInfo().setTenderName(rs.getString(2));
    tenderVO.getBasicInfo().setTenderNumber(rs.getString(3));
    tenderVO.getBasicInfo()
        .setGovtTehnicalAgency(getPoolPropertiesConfig().getProperty(GAT_CONFIG_PARAM));// TO DO
                                                                                        // reading
                                                                                        // from
                                                                                        // properties
                                                                                        // file
    tenderVO.getBasicInfo().setDescription(rs.getString(4));
    tenderVO.getBasicInfo().setPrice(rs.getInt(5));
    tenderVO.getBasicInfo().setProposalSubmissionAddress(rs.getString(6));
    tenderVO.getBasicInfo().setOpenEnvelopesLocation(rs.getString(7));
    if (null != rs.getString(16)) {
      tenderVO.setTenderInternalId(Integer.parseInt(rs.getString(16)));
    }
    tenderVO.getBasicInfo().setShowInFront(1);
  }

  /**
   * Get the database connection
   * 
   * @return
   */
  private Connection getDbConnection() {
    return OBDal.getInstance().getConnection();
  }

  /**
   * Set the Tender Dates data
   * 
   * @param rs
   * @param tenderVO
   * @throws SQLException
   */
  private void setTenderDates(ResultSet rs, TenderVO tenderVO) throws SQLException {
    // Call method here to convert gregorian date to hijri
    tenderVO.getTenderDates()
        .setFaqDeliveryDateHijri(Utility.convertGregToHijriTabadulPattern(rs.getDate(8)));
    tenderVO.getTenderDates()
        .setFaqDeliveryDateGregorian(Utility.convertGregDatePattern(gregDateFormat, rs.getDate(8)));
    tenderVO.getTenderDates()
        .setOfferDeliveryDateHijri(Utility.convertGregToHijriTabadulPattern(rs.getDate(9)));
    tenderVO.getTenderDates().setOfferDeliveryDateGregorian(
        Utility.convertGregDatePattern(gregDateFormat, rs.getDate(9)));
    tenderVO.getTenderDates().setOfferDeliveryHour(getDeliveryHour(rs.getString(10)));
    tenderVO.getTenderDates().setOfferDeliveryMinute(getDeliveryMinute(rs.getString(10)));
    tenderVO.getTenderDates().setOpenEnvelopeDateGregorian(
        Utility.convertGregDatePattern(gregDateFormat, rs.getDate(11)));
    tenderVO.getTenderDates()
        .setOpenEnvelopeDateHijri(Utility.convertGregToHijriTabadulPattern(rs.getDate(11)));
    tenderVO.getTenderDates().setOpenEnvelopeHour(getDeliveryHour(rs.getString(12)));
    tenderVO.getTenderDates().setOpenEnvelopeMinute(getDeliveryMinute(rs.getString(12)));

  }

  /**
   * Set the Taxonomy Location
   * 
   * @param rs
   * @param tenderVO
   * @throws SQLException
   */
  private void setTaxonomyLocation(ResultSet rs, TenderVO tenderVO) throws SQLException {

    tenderVO.getTaxonomyAndLocationVO().setExecuteLocation(rs.getString(13));

  }

  /**
   * Set the Tender Files
   * 
   * @param rs
   * @param tenderVO
   * @throws SQLException
   */
  private void setTenderFiles(ResultSet rs, TenderVO tenderVO) throws SQLException {

    tenderVO.getTenderFiles().setDeliveryLocation(rs.getString(14));
    tenderVO.getTenderFiles().setDownloadableDelivery(1);
  }

  /**
   * Set the tender categories
   * 
   * @param rs
   * @param tenderVO
   * @throws SQLException
   */
  private void setTenderCategories(TenderVO tenderVO) throws SQLException {
    List<Integer> bidCategoriesList = getBidCategories(tenderVO.getBidId());
    // Convert the List to Array of Integers
    Integer[] bidCategoriesArray = new Integer[bidCategoriesList.size()];
    bidCategoriesArray = bidCategoriesList.toArray(bidCategoriesArray);
    tenderVO.getTenderCategoriesVO().setTenderCategories(bidCategoriesArray);
    // tenderVO.getTenderCategoriesVO().setTenderCategories(new Integer [] {62});// Hard coded for
    // now needs to be changed later
  }

  /**
   * Return the hour portion
   * 
   * @param time
   * @return
   */
  private String getDeliveryHour(String time) {
    String[] hourMinute = time.split(SEPARATOR);
    return hourMinute[0];
  }

  /**
   * Return minute portion
   * 
   * @param time
   * @return
   */
  private String getDeliveryMinute(String time) {
    String[] hourMinute = time.split(SEPARATOR);
    return hourMinute[1];
  }

  /**
   * Getter for Properties Object
   * 
   * @return
   */
  public Properties getPoolPropertiesConfig() {
    if (null == poolPropertiesConfig)
      return OBPropertiesProvider.getInstance().getOpenbravoProperties();
    else
      return poolPropertiesConfig;
  }

  @Override
  public void updateTenderIdInBid(String tenderId, String status, String bidNumber)
      throws SQLException {
    EscmBidMgmt escmBidMgmt = null;
    OBQuery<EscmBidMgmt> bidQry = null;
    List<EscmBidMgmt> bidList = new ArrayList<EscmBidMgmt>();
    try {
      OBContext.setAdminMode(true);
      final String query = " as e where e.id=:value  ";
      bidQry = OBDal.getInstance().createQuery(EscmBidMgmt.class, query);
      bidQry.setNamedParameter("value", bidNumber);
      bidQry.setMaxResult(1);
      bidList = bidQry.list();
      if (bidList.size() > 0) {
        escmBidMgmt = bidList.get(0);
        if (null != tenderId && tenderId.trim().length() > 0) {
          escmBidMgmt.setTabadulTenderID(Long.parseLong(tenderId));
        } else {
          escmBidMgmt.setTabadulTenderID(null);
        }
        escmBidMgmt.setTabadulStatus(status);
        // escmBidMgmt.setTabadulPublishedOn(new Date ());

        OBDal.getInstance().save(escmBidMgmt);
      }
    } catch (OBException e) {
      log.error("Error while update tender id" + tenderId);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Update the status to Publish if the tender is published
   */
  @Override
  public void updateTenderFileStatus(String bidManagementId, String tableId) throws Exception {

    String attachementQuery = " UPDATE escm_tabadul_attachments e SET status = ? FROM c_file c WHERE "
        + " c.c_file_id = e.c_file_Id"
        + " and c.AD_TABLE_ID = ? and c.AD_RECORD_ID = ? AND status = 'UP'";

    PreparedStatement ps = null;
    try {
      ps = connection.prepareStatement(attachementQuery);
      ps.setString(1, TenderStatusE.PUBLISHED.getStatus());
      ps.setString(2, tableId);
      ps.setString(3, bidManagementId);

      ps.executeUpdate();
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      throw e;
    }

  }

  @Override
  public void updateAttachmentDescription(String cFileId, String description) throws Exception {

    String updateQuery = "UPDATE C_FILE SET TEXT = TEXT ||" + " '[" + description
        + "]' WHERE C_FILE_ID = ?";
    PreparedStatement ps = null;
    try {
      ps = connection.prepareStatement(updateQuery);
      ps.setString(1, cFileId);

      ps.executeUpdate();
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      throw e;
    }
  }

  @Override
  public List<EscmBidMgmt> getPurchasesForBidInTabadul(String tabadulStatus,
      Date bidProposalLastDay) {
    log.debug("getBidsByTabadulIntegrationStatus: Tabadul Status " + tabadulStatus);

    final String query = " as e join fetch e.escmBiddatesList as bl  where e.tabadulStatus = ? and bl.openenvday >= ? ";
    List<EscmBidMgmt> bidsList = null;
    List<Object> parametersList = new ArrayList<Object>();
    parametersList.add(tabadulStatus);
    parametersList.add(bidProposalLastDay);

    try {
      OBContext.setAdminMode();

      OBQuery<EscmBidMgmt> escmBidMgmt = OBDal.getInstance().createQuery(EscmBidMgmt.class, query,
          parametersList);
      bidsList = escmBidMgmt.list();

    } catch (OBException e) {
      log.error("Exception while checksameDocNowithSameBank:", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

    return bidsList;
  }

  @Override
  public BusinessPartner getBusinessPartner(String vendorId) {
    log.debug("isBusinessPartner: vendor Id :" + vendorId);

    final String query = " as e where e.escmTabadulid = ? ";
    List<Object> parametersList = new ArrayList<Object>();
    parametersList.add(vendorId);

    try {
      OBContext.setAdminMode();

      OBQuery<BusinessPartner> businessPartnerList = OBDal.getInstance()
          .createQuery(BusinessPartner.class, query, parametersList);
      return (businessPartnerList.list().size() > 0) ? businessPartnerList.list().get(0) : null;

    } catch (OBException e) {
      log.error("Exception while checksameDocNowithSameBank:", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  @Override
  public void saveRfpSalesRecord(Escmsalesvoucher escmsalesvoucher) {

    OBDal.getInstance().save(escmsalesvoucher);
  }

  @Override
  public void saveBidSupplierRecord(Escmbidsuppliers escmbidsuppliers) {

    OBDal.getInstance().save(escmbidsuppliers);

  }

  @Override
  public Boolean checkIfSalesRecordExists(String escmBidMgmtId, String supplierNumber) {
    Boolean exists = false;
    log.debug("<---START checkIfSalesRecordExists-->: EscmBidMgmtId :" + escmBidMgmtId
        + " Supplier Number : " + supplierNumber);

    final String query = " as e where e.supplierNumber.efinDocumentno = ? and e.escmBidmgmt.id = ? ";
    List<Object> parametersList = new ArrayList<Object>();
    parametersList.add(supplierNumber);
    parametersList.add(escmBidMgmtId);

    try {
      OBContext.setAdminMode();

      OBQuery<Escmsalesvoucher> salesVoucherList = OBDal.getInstance()
          .createQuery(Escmsalesvoucher.class, query, parametersList);
      if (salesVoucherList.list().size() > 0) {
        exists = true;
      }

    } catch (OBException e) {
      log.error("Exception while checksameDocNowithSameBank:", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return exists;
  }

  @Override
  public Boolean checkIfSupplierRecordAlreadyExists(String supplierNumber, String branchName,
      String escmBidMgmtId) {
    Boolean exists = false;
    log.debug("<---START checkIfSalesRecordExists-->: EscmBidMgmtId :" + escmBidMgmtId
        + " Supplier Number : " + supplierNumber);

    final String query = " as e where e.suppliernumber.searchKey = ? and e.escmBidmgmt.id = ? and e.branchname.name = ?";
    List<Object> parametersList = new ArrayList<Object>();
    parametersList.add(supplierNumber);
    parametersList.add(escmBidMgmtId);
    parametersList.add(branchName);

    try {
      OBContext.setAdminMode();

      OBQuery<Escmbidsuppliers> bidSuppliersList = OBDal.getInstance()
          .createQuery(Escmbidsuppliers.class, query, parametersList);
      if (bidSuppliersList.list().size() > 0) {
        exists = true;
      }

    } catch (OBException e) {
      log.error("Exception while checksameDocNowithSameBank:", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return exists;
  }

}
