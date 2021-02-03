package sa.elm.ob.scm.actionHandler;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.springframework.core.io.FileSystemResource;

import sa.elm.ob.utility.tabadul.AttachmentVO;
import sa.elm.ob.utility.tabadul.TabadulActionsE;
import sa.elm.ob.utility.tabadul.TabadulAuthenticationServiceImpl;
import sa.elm.ob.utility.tabadul.TabadulIntegrationDAOImpl;
import sa.elm.ob.utility.tabadul.TabadulIntegrationServiceImpl;
import sa.elm.ob.utility.tabadul.TenderStatusE;
import sa.elm.ob.utility.tabadul.TenderVO;

/**
 * Announcement Summary Tabadul Integration
 * 
 * @author mrahim
 *
 */
public class AnnouncementTenderHandler extends BaseActionHandler {

  private static Logger log = Logger.getLogger(BidTenderHandler.class);
  private static String RECORD_ID = "Escm_Bidmgmt_ID";
  private static String USER_CONFIG_PARAM = "tabadul.username";
  private static String PWD_CONFIG_PARAM = "tabadul.password";
  private static String USER_AUDIT_CONFIG_PARAM = "tabadul.audit.username";
  private static String PWD_AUDIT_CONFIG_PARAM = "tabadul.audit.password";
  private static String ATTACH_ABSOLUTE_PATH = "attach.path";
  private static String BID_MGMT_TABLE_ID = "9500BBBFB8584B3783C1C9B9492FD7FE";
  private static String TABADUL_ACTION_PARAM = "createtender";
  private static String CREATE_TENDER_ERROR_MSG = "ESCM_CREATE_TENDER_ERROR_MSG";
  private static String PUBLISH_TENDER_ERROR_MSG = "ESCM_PUBLISH_TENDER_ERROR_MSG";
  private static String CANCEL_TENDER_ERROR_MSG = "ESCM_CANCEL_TENDER_ERROR_MSG";
  private static String DELETE_TENDER_ERROR_MSG = "ESCM_DELETE_TENDER_ERROR_MSG";
  private static String UPLOAD_TENDER_FILE_ERROR_MSG = "ESCM_UPLOAD_TENDER_FILE_ERROR_MSG";
  private static String PUBLISH_TENDER_FILE_ERROR_MSG = "ESCM_PUBLISH_TENDER_FILE_ERROR_MSG";
  private static String CANCEL_TENDER_FILE_ERROR_MSG = "ESCM_CANCEL_TENDER_FILE_ERROR_MSG";
  private static String DELETE_TENDER_FILE_ERROR_MSG = "ESCM_DELETE_TENDER_FILE_ERROR_MSG";
  private static String CANCEL_TENDER_FILE_NOT_PUBLISHED_ERROR_MSG = "ESCM_CANCEL_TENDER_FILE_NOT_PUBLISHED_ERROR_MSG";
  private static String PUBLISH_TENDER_FILE_NOT_UPLOADED_ERROR_MSG = "ESCM_PUBLISH_TENDER_FILE_NOT_UPLOADED_ERROR_MSG";
  private static String UPLOAD_TENDER_FILE_NOT_DRAFT_ERROR_MSG = "ESCM_UPLOAD_TENDER_FILE_NOT_DRAFT_ERROR_MSG";
  private static String DELETE_TENDER_FILE_NOT_UPLOADED_ERROR_MSG = "ESCM_DELETE_TENDER_FILE_NOT_UPLOADED_ERROR_MSG";

  private static String BUTTON_PARAMS_VALUE = "_params";
  private Properties poolPropertiesConfig;

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = new JSONObject();
    String message = "";
    try {
      OBContext.setAdminMode();
      jsonRequest = new JSONObject(content);
      final String bidManagementId = jsonRequest.getString(RECORD_ID);
      JSONObject params = (JSONObject) jsonRequest.get(BUTTON_PARAMS_VALUE);
      String action = (String) params.get(TABADUL_ACTION_PARAM);
      if (action.trim().equals(TabadulActionsE.CREATE_TENDER.getActionKey())
          || action.trim().equals(TabadulActionsE.UPDATE_TENDER.getActionKey())) {
        // message = createTender(bidManagementId,action);
      } else if (action.trim().equals(TabadulActionsE.PUBLISH_TENDER.getActionKey())) {
        message = publishTender(bidManagementId);
      } else if (action.trim().equals(TabadulActionsE.CANCEL_TENDER.getActionKey())) {
        message = cancelTender(bidManagementId);
      } else if (action.trim().equals(TabadulActionsE.UPLOAD_TENDER_FILE.getActionKey())) {
        message = uploadTenderFiles(bidManagementId);
      } else if (action.trim().equals(TabadulActionsE.PUBLISH_TENDER_FILE.getActionKey())) {
        message = publishTenderFiles(bidManagementId);
      } else if (action.trim().equals(TabadulActionsE.CANCEL_TENDER_FILE.getActionKey())) {
        message = cancelTenderFiles(bidManagementId);
      } else if (action.trim().equals(TabadulActionsE.DELETE_TENDER.getActionKey())) {
        message = deleteTender(bidManagementId);
      } else if (action.trim().equals(TabadulActionsE.DELETE_TENDER_FILE.getActionKey())) {
        message = deleteTenderFiles(bidManagementId);
      }
      if (null != message && message.trim().length() > 0) {
        jsonRequest.put("message", getErrorMessage(message));
      } else {
        jsonRequest.put("message", getSuccessMessage());
      }

      return jsonRequest;
    } catch (Exception e) {
      log.error("Exception in Tabadul Tender Handler :", e);
      // OBDal.getInstance().rollbackAndClose();
      try {
        jsonRequest.put("message", getErrorMessage(""));
      } catch (JSONException e1) {
        // TODO Auto-generated catch block
        log.error("Exception in Tabadul Tender Handler :", e1);
      }

    } finally {

      OBContext.restorePreviousMode();

    }

    return jsonRequest;

  }

  /**
   * Create the Tender
   * 
   * @param bidManagementId
   * @throws Exception
   */
  @SuppressWarnings("unused")
  private String createTenders(String announcementId, String action) throws Exception {
    String message = "";
    // MessageVO messageVO = new MessageVO();
    TabadulIntegrationDAOImpl tabaDaoImpl = new TabadulIntegrationDAOImpl();
    List<TenderVO> tendersList = null;
    if (action.equals(TabadulActionsE.CREATE_TENDER.getActionKey())) {
      tendersList = tabaDaoImpl.getTendersForAnnouncement(announcementId,
          TenderStatusE.DRAFT.getStatus());
    } else {
      tendersList = tabaDaoImpl.getTendersForAnnouncement(announcementId,
          TenderStatusE.UPLOADED.getStatus());
    }
    if (tendersList.size() == 0) {
      // return the message that tender must be draft or uploaded
      return message = CREATE_TENDER_ERROR_MSG;
    }
    // Now loop each tender
    for (TenderVO tenderVO : tendersList) {

      try {
        setAddressInformation(tenderVO);// set the Address Information
        createTender(tenderVO);// Create Tender in Tabadul
        // messageVO.setSuccessCount(messageVO.getSuccessCount()+ 1);
      } catch (Exception e) {
        e.printStackTrace();
        // messageVO.setFailureCount(messageVO.getFailureCount()+ 1);
      }

    }

    return message;

  }

  /**
   * Publish Tender
   * 
   * @param bidManagementId
   * @throws Exception
   */
  private String publishTender(String bidManagementId) throws Exception {
    String message = "";
    TabadulIntegrationDAOImpl tabaDaoImpl = new TabadulIntegrationDAOImpl();
    TenderVO tenderVO = tabaDaoImpl.getTenderInformation(bidManagementId,
        TenderStatusE.UPLOADED.getStatus());

    if (null == tenderVO) {
      return message = PUBLISH_TENDER_ERROR_MSG;
    }

    final String userName = getPoolPropertiesConfig().getProperty(USER_AUDIT_CONFIG_PARAM);
    final String password = getPoolPropertiesConfig().getProperty(PWD_AUDIT_CONFIG_PARAM);

    TabadulIntegrationServiceImpl tabadulIntegrationService = new TabadulIntegrationServiceImpl();
    TabadulAuthenticationServiceImpl tabadulAuthenticationService = new TabadulAuthenticationServiceImpl();
    tabadulIntegrationService.setTabadulAuthenticationService(tabadulAuthenticationService);
    String sessionToken = tabadulIntegrationService.initializeRequest(userName, password);

    TabadulIntegrationServiceImpl tabadulIntegrationServiceImpl = new TabadulIntegrationServiceImpl();
    tabadulIntegrationServiceImpl.publishTender(String.valueOf(tenderVO.getTenderInternalId()),
        sessionToken);
    tabaDaoImpl.updateTenderIdInBid(String.valueOf(tenderVO.getTenderInternalId()),
        TenderStatusE.PUBLISHED.getStatus(), tenderVO.getBidId());

    return message;
  }

  /**
   * Cancel the tender
   * 
   * @param bidManagementId
   * @throws Exception
   */
  private String cancelTender(String bidManagementId) throws Exception {
    String message = "";
    TabadulIntegrationDAOImpl tabaDaoImpl = new TabadulIntegrationDAOImpl();
    TenderVO tenderVO = tabaDaoImpl.getTenderInformation(bidManagementId,
        TenderStatusE.PUBLISHED.getStatus());

    if (null == tenderVO) {
      return message = CANCEL_TENDER_ERROR_MSG;
    }

    final String userName = getPoolPropertiesConfig().getProperty(USER_AUDIT_CONFIG_PARAM);
    final String password = getPoolPropertiesConfig().getProperty(PWD_AUDIT_CONFIG_PARAM);

    TabadulIntegrationServiceImpl tabadulIntegrationService = new TabadulIntegrationServiceImpl();
    TabadulAuthenticationServiceImpl tabadulAuthenticationService = new TabadulAuthenticationServiceImpl();
    tabadulIntegrationService.setTabadulAuthenticationService(tabadulAuthenticationService);
    String sessionToken = tabadulIntegrationService.initializeRequest(userName, password);

    TabadulIntegrationServiceImpl tabadulIntegrationServiceImpl = new TabadulIntegrationServiceImpl();
    tabadulIntegrationServiceImpl.cancelTender(String.valueOf(tenderVO.getTenderInternalId()),
        sessionToken);
    tabaDaoImpl.updateTenderIdInBid(null, TenderStatusE.DRAFT.getStatus(), tenderVO.getBidId());

    return message;
  }

  /**
   * Delete Tender
   */
  private String deleteTender(String bidManagementId) throws Exception {
    String message = "";
    TabadulIntegrationDAOImpl tabaDaoImpl = new TabadulIntegrationDAOImpl();
    TenderVO tenderVO = tabaDaoImpl.getTenderInformation(bidManagementId,
        TenderStatusE.UPLOADED.getStatus());

    if (null == tenderVO) {
      return message = DELETE_TENDER_ERROR_MSG;
    }

    final String userName = getPoolPropertiesConfig().getProperty(USER_AUDIT_CONFIG_PARAM);
    final String password = getPoolPropertiesConfig().getProperty(PWD_AUDIT_CONFIG_PARAM);

    TabadulIntegrationServiceImpl tabadulIntegrationService = new TabadulIntegrationServiceImpl();
    TabadulAuthenticationServiceImpl tabadulAuthenticationService = new TabadulAuthenticationServiceImpl();
    tabadulIntegrationService.setTabadulAuthenticationService(tabadulAuthenticationService);
    String sessionToken = tabadulIntegrationService.initializeRequest(userName, password);

    TabadulIntegrationServiceImpl tabadulIntegrationServiceImpl = new TabadulIntegrationServiceImpl();
    tabadulIntegrationServiceImpl.deleteTender(String.valueOf(tenderVO.getTenderInternalId()),
        sessionToken);
    tabaDaoImpl.updateTenderIdInBid(String.valueOf(tenderVO.getTenderInternalId()),
        TenderStatusE.DELETED.getStatus(), tenderVO.getBidId());

    return message;
  }

  /**
   * Uploads the tender files to Tabadul
   * 
   * @throws Exception
   */
  private String uploadTenderFiles(String bidManagementId) throws Exception {
    String message = "";
    final String attachmentAbsolutePath = getPoolPropertiesConfig()
        .getProperty(ATTACH_ABSOLUTE_PATH);
    FileSystemResource fileSystemResource = null;
    TabadulIntegrationServiceImpl tabadulIntegrationServiceImpl = new TabadulIntegrationServiceImpl();

    TabadulIntegrationDAOImpl tabadulIntegrationDAOImpl = new TabadulIntegrationDAOImpl();
    TenderVO tenderVO = tabadulIntegrationDAOImpl.getTenderInformation(bidManagementId, null);

    if (!(tenderVO.getStatus().trim().equals(TenderStatusE.UPLOADED.getStatus())
        || tenderVO.getStatus().trim().equals(TenderStatusE.PUBLISHED.getStatus()))) {
      return UPLOAD_TENDER_FILE_ERROR_MSG;
    }

    final String userName = getPoolPropertiesConfig().getProperty(USER_CONFIG_PARAM);
    final String password = getPoolPropertiesConfig().getProperty(PWD_CONFIG_PARAM);

    TabadulIntegrationServiceImpl tabadulIntegrationService = new TabadulIntegrationServiceImpl();
    TabadulAuthenticationServiceImpl tabadulAuthenticationService = new TabadulAuthenticationServiceImpl();
    tabadulIntegrationService.setTabadulAuthenticationService(tabadulAuthenticationService);
    String sessionToken = tabadulIntegrationService.initializeRequest(userName, password);

    // Get all files uploaded with bid
    List<AttachmentVO> attachements = tabadulIntegrationDAOImpl.getAttachments(bidManagementId,
        BID_MGMT_TABLE_ID, TenderStatusE.DRAFT.getStatus());
    if (attachements.size() == 0) {
      return UPLOAD_TENDER_FILE_NOT_DRAFT_ERROR_MSG;
    }

    // Read all attachments from disk
    for (AttachmentVO attachmentVO : attachements) {
      try {
        String filePath = attachmentAbsolutePath + attachmentVO.getFilePath()
            + attachmentVO.getFileName();
        fileSystemResource = new FileSystemResource(new File(filePath));
        // Now upload to tabadul
        String fileId = tabadulIntegrationServiceImpl.uploadTenderFile(
            String.valueOf(tenderVO.getTenderInternalId()), "attachment", fileSystemResource,
            sessionToken);
        // Update the status of attachment
        tabadulIntegrationDAOImpl.updateTabadulFileStatus(TenderStatusE.UPLOADED.getStatus(),
            fileId, attachmentVO.getTabadulAttachmentId());
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    return message;
  }

  /**
   * Publish Tender Files
   * 
   * @param bidManagementId
   * @throws Exception
   */
  private String publishTenderFiles(String bidManagementId) throws Exception {
    String message = "";
    TabadulIntegrationServiceImpl tabadulIntegrationServiceImpl = new TabadulIntegrationServiceImpl();

    TabadulIntegrationDAOImpl tabadulIntegrationDAOImpl = new TabadulIntegrationDAOImpl();
    TenderVO tenderVO = tabadulIntegrationDAOImpl.getTenderInformation(bidManagementId,
        TenderStatusE.PUBLISHED.getStatus());

    if (null == tenderVO) {
      return PUBLISH_TENDER_FILE_ERROR_MSG;
    }

    final String userName = getPoolPropertiesConfig().getProperty(USER_AUDIT_CONFIG_PARAM);
    final String password = getPoolPropertiesConfig().getProperty(PWD_AUDIT_CONFIG_PARAM);

    TabadulIntegrationServiceImpl tabadulIntegrationService = new TabadulIntegrationServiceImpl();
    TabadulAuthenticationServiceImpl tabadulAuthenticationService = new TabadulAuthenticationServiceImpl();
    tabadulIntegrationService.setTabadulAuthenticationService(tabadulAuthenticationService);
    String sessionToken = tabadulIntegrationService.initializeRequest(userName, password);

    // Get all files uploaded with bid
    List<AttachmentVO> attachements = tabadulIntegrationDAOImpl.getAttachments(bidManagementId,
        BID_MGMT_TABLE_ID, TenderStatusE.UPLOADED.getStatus());
    if (attachements.size() == 0) {
      return PUBLISH_TENDER_FILE_NOT_UPLOADED_ERROR_MSG;
    }
    // Read all attachments from disk
    for (AttachmentVO attachmentVO : attachements) {
      try {
        // Now upload to tabadul
        tabadulIntegrationServiceImpl.publishTenderFile(
            String.valueOf(tenderVO.getTenderInternalId()), attachmentVO.getFid(), sessionToken);
        // Update the status of attachment
        tabadulIntegrationDAOImpl.updateTabadulFileStatus(TenderStatusE.PUBLISHED.getStatus(), null,
            attachmentVO.getTabadulAttachmentId());
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    return message;
  }

  /**
   * Cancel Tender Files associated with Bid
   * 
   * @param bidManagementId
   * @throws Exception
   */
  private String cancelTenderFiles(String bidManagementId) throws Exception {
    String message = "";
    TabadulIntegrationServiceImpl tabadulIntegrationServiceImpl = new TabadulIntegrationServiceImpl();

    TabadulIntegrationDAOImpl tabadulIntegrationDAOImpl = new TabadulIntegrationDAOImpl();
    TenderVO tenderVO = tabadulIntegrationDAOImpl.getTenderInformation(bidManagementId,
        TenderStatusE.PUBLISHED.getStatus());

    if (null == tenderVO) {
      return CANCEL_TENDER_FILE_ERROR_MSG;
    }

    final String userName = getPoolPropertiesConfig().getProperty(USER_AUDIT_CONFIG_PARAM);
    final String password = getPoolPropertiesConfig().getProperty(PWD_AUDIT_CONFIG_PARAM);

    TabadulIntegrationServiceImpl tabadulIntegrationService = new TabadulIntegrationServiceImpl();
    TabadulAuthenticationServiceImpl tabadulAuthenticationService = new TabadulAuthenticationServiceImpl();
    tabadulIntegrationService.setTabadulAuthenticationService(tabadulAuthenticationService);
    String sessionToken = tabadulIntegrationService.initializeRequest(userName, password);

    // Get all files uploaded with bid
    List<AttachmentVO> attachements = tabadulIntegrationDAOImpl.getAttachments(bidManagementId,
        BID_MGMT_TABLE_ID, TenderStatusE.PUBLISHED.getStatus());
    if (attachements.size() == 0) {
      return CANCEL_TENDER_FILE_NOT_PUBLISHED_ERROR_MSG;
    }
    // Read all attachments from disk
    for (AttachmentVO attachmentVO : attachements) {
      try {
        // Now upload to tabadul
        tabadulIntegrationServiceImpl.cancelTenderFile(
            String.valueOf(tenderVO.getTenderInternalId()), attachmentVO.getFid(), sessionToken);
        // Update the status of attachment
        tabadulIntegrationDAOImpl.updateTabadulFileStatus(TenderStatusE.CANCELLED.getStatus(), null,
            attachmentVO.getTabadulAttachmentId());
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    return message;
  }

  /**
   * Delete Tender Files
   * 
   * @param bidManagementId
   * @return
   * @throws Exception
   */
  private String deleteTenderFiles(String bidManagementId) throws Exception {
    String message = "";
    TabadulIntegrationServiceImpl tabadulIntegrationServiceImpl = new TabadulIntegrationServiceImpl();

    TabadulIntegrationDAOImpl tabadulIntegrationDAOImpl = new TabadulIntegrationDAOImpl();
    TenderVO tenderVO = tabadulIntegrationDAOImpl.getTenderInformation(bidManagementId,
        TenderStatusE.UPLOADED.getStatus());

    if (null == tenderVO) {
      return DELETE_TENDER_FILE_ERROR_MSG;
    }

    final String userName = getPoolPropertiesConfig().getProperty(USER_AUDIT_CONFIG_PARAM);
    final String password = getPoolPropertiesConfig().getProperty(PWD_AUDIT_CONFIG_PARAM);

    TabadulIntegrationServiceImpl tabadulIntegrationService = new TabadulIntegrationServiceImpl();
    TabadulAuthenticationServiceImpl tabadulAuthenticationService = new TabadulAuthenticationServiceImpl();
    tabadulIntegrationService.setTabadulAuthenticationService(tabadulAuthenticationService);
    String sessionToken = tabadulIntegrationService.initializeRequest(userName, password);

    // Get all files uploaded with bid
    List<AttachmentVO> attachements = tabadulIntegrationDAOImpl.getAttachments(bidManagementId,
        BID_MGMT_TABLE_ID, TenderStatusE.UPLOADED.getStatus());
    if (attachements.size() == 0) {
      return DELETE_TENDER_FILE_NOT_UPLOADED_ERROR_MSG;
    }
    // Read all attachments from disk
    for (AttachmentVO attachmentVO : attachements) {
      try {
        // Now upload to tabadul
        tabadulIntegrationServiceImpl.cancelTenderFile(
            String.valueOf(tenderVO.getTenderInternalId()), attachmentVO.getFid(), sessionToken);
        // Update the status of attachment
        tabadulIntegrationDAOImpl.updateTabadulFileStatus(TenderStatusE.DELETED.getStatus(), null,
            attachmentVO.getTabadulAttachmentId());
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    return message;
  }

  /**
   * set the address information
   * 
   * @throws SQLException
   */
  private void setAddressInformation(TenderVO tenderVO) throws SQLException {
    TabadulIntegrationDAOImpl tabaDaoImpl = new TabadulIntegrationDAOImpl();
    tenderVO.getBasicInfo().setProposalSubmissionAddress(
        tabaDaoImpl.getAddressInformation(tenderVO.getBasicInfo().getProposalSubmissionAddress()));
    tenderVO.getBasicInfo().setOpenEnvelopesLocation(
        tabaDaoImpl.getAddressInformation(tenderVO.getBasicInfo().getOpenEnvelopesLocation()));
    tenderVO.getTenderFiles().setDeliveryLocation(
        tabaDaoImpl.getAddressInformation(tenderVO.getTenderFiles().getDeliveryLocation()));
    tenderVO.getTaxonomyAndLocationVO().setExecuteLocation("national");

  }

  /**
   * Get the success message
   * 
   * @return
   * @throws JSONException
   */
  private JSONObject getSuccessMessage() throws JSONException {
    JSONObject successMessage = new JSONObject();
    successMessage.put("severity", "success");
    successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));

    return successMessage;
  }

  /**
   * Get the success message
   * 
   * @return
   * @throws JSONException
   */
  private JSONObject getErrorMessage(String message) {
    JSONObject errorMessage = new JSONObject();
    try {
      errorMessage.put("severity", "error");
      if (null != message && message.trim().length() < 1) {
        errorMessage.put("text", OBMessageUtils.messageBD("ESCM_TABADUL_ERROR"));
      } else {
        errorMessage.put("text", OBMessageUtils.messageBD(message));
      }

    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return errorMessage;
  }

  /**
   * Create the tender by calling tabadul integration
   * 
   * @param tenderVOs
   */
  private String createTender(TenderVO tenderVO) throws Exception {
    TabadulIntegrationDAOImpl tabadulIntegrationDAOImpl = new TabadulIntegrationDAOImpl();
    final String userName = getPoolPropertiesConfig().getProperty(USER_CONFIG_PARAM);
    final String password = getPoolPropertiesConfig().getProperty(PWD_CONFIG_PARAM);

    TabadulIntegrationServiceImpl tabadulIntegrationService = new TabadulIntegrationServiceImpl();
    TabadulAuthenticationServiceImpl tabadulAuthenticationService = new TabadulAuthenticationServiceImpl();
    tabadulIntegrationService.setTabadulAuthenticationService(tabadulAuthenticationService);
    String sessionToken = tabadulIntegrationService.initializeRequest(userName, password);
    // Call the method here to create tender
    String tenderId = tabadulIntegrationService.createOrUpdateTender(sessionToken, tenderVO);
    // Call the method to update the tenderId in Bid Table
    tabadulIntegrationDAOImpl.updateTenderIdInBid(tenderId, TenderStatusE.UPLOADED.getStatus(),
        tenderVO.getBidId());

    return tenderId;
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

}
