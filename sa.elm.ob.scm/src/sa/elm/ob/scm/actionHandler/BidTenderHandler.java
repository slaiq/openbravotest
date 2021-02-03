package sa.elm.ob.scm.actionHandler;

import java.io.File;
import java.sql.SQLException;
import java.text.MessageFormat;
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

import sa.elm.ob.scm.ad_process.BidManagement.dao.BidManagementDAO;
import sa.elm.ob.utility.tabadul.AttachmentVO;
import sa.elm.ob.utility.tabadul.MessageTypesE;
import sa.elm.ob.utility.tabadul.MessageVO;
import sa.elm.ob.utility.tabadul.TabadulActionsE;
import sa.elm.ob.utility.tabadul.TabadulAuthenticationService;
import sa.elm.ob.utility.tabadul.TabadulAuthenticationServiceImpl;
import sa.elm.ob.utility.tabadul.TabadulIntegrationDAO;
import sa.elm.ob.utility.tabadul.TabadulIntegrationDAOImpl;
import sa.elm.ob.utility.tabadul.TabadulIntegrationService;
import sa.elm.ob.utility.tabadul.TabadulIntegrationServiceImpl;
import sa.elm.ob.utility.tabadul.TenderStatusE;
import sa.elm.ob.utility.tabadul.TenderVO;

/**
 * Handler class for tabadul
 * 
 * @author mrahim
 *
 */
public class BidTenderHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(BidTenderHandler.class);
  private static String RECORD_ID = "Escm_Bidmgmt_ID";
  private static String USER_CONFIG_PARAM = "tabadul.username";
  private static String PWD_CONFIG_PARAM = "tabadul.password";
  private static String USER_AUDIT_CONFIG_PARAM = "tabadul.audit.username";
  private static String PWD_AUDIT_CONFIG_PARAM = "tabadul.audit.password";
  private static String ATTACH_ABSOLUTE_PATH = "attach.path";
  private static String MESSAGE_SEVERITY = "severity";
  private static String MESSAGE_TEXT = "text";
  private static String BID_MGMT_TABLE_ID = "9500BBBFB8584B3783C1C9B9492FD7FE";
  private static String TABADUL_ACTION_PARAM = "createtender";
  private static String CREATE_TENDER_SUCCESS_MSG = "ESCM_CREATE_TENDER_SUCCESS_MSG";
  private static String EXTEND_TENDER_SUCCESS_MSG = "ESCM_EXTEND_TENDER_SUCCESS_MSG";
  private static String EXTEND_TENDER_OPENENV_ERROR_MSG = "ESCM_EXTD_TR_OPENENV_ERROR_MSG";
  private static String UPLOAD_FILE_SUCCESS_MSG = "ESCM_UPLOAD_FILE_SUCCESS_MSG";
  private static String CANCEL_TENDER_ERROR_MSG = "ESCM_CANCEL_TENDER_ERROR_MSG";

  private static String BUTTON_PARAMS_VALUE = "_params";
  private Properties poolPropertiesConfig;

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequestMain = new JSONObject();
    JSONObject jsonRequestInner = new JSONObject();
    try {
      OBContext.setAdminMode();
      jsonRequestMain = new JSONObject(content);
      final String bidManagementId = jsonRequestMain.getString(RECORD_ID);
      JSONObject params = (JSONObject) jsonRequestMain.get(BUTTON_PARAMS_VALUE);
      String action = (String) params.get(TABADUL_ACTION_PARAM);
      log.info("Tabadul Action : ---> " + action + "Bid Id : ---->" + bidManagementId);
      if (action.trim().equals(TabadulActionsE.CREATE_TENDER.getActionKey())
          || action.trim().equals(TabadulActionsE.UPDATE_TENDER.getActionKey())) {
        jsonRequestInner = createAndPublishTender(bidManagementId);
      } else if (action.trim().equals(TabadulActionsE.UPLOAD_TENDER_FILE.getActionKey())) {
        jsonRequestInner = uploadTenderFiles(bidManagementId);
      } else {
        jsonRequestInner = extendTenderDates(bidManagementId);
      }

      jsonRequestMain.put("message", jsonRequestInner);
      // return jsonRequestMain;
    } catch (Exception e) {
      log.error("Exception in Tabadul Tender Handler :", e);
      try {
        jsonRequestMain.put("message",
            getResponseMessage("ESCM_TABADUL_ERROR", null, MessageTypesE.ERROR.getType()));
      } catch (JSONException e1) {
        log.error("Exception in Tabadul Tender Handler :", e1);
      }

    } finally {

      OBContext.restorePreviousMode();

    }

    return jsonRequestMain;

  }

  /**
   * Create the Tender and Publish Tender in one go
   * 
   * @param bidManagementId
   * @throws Exception
   */
  private JSONObject createAndPublishTender(String bidManagementId) throws Exception {
    log.info("<---Start createAndPublishTender--> + " + bidManagementId);
    TabadulIntegrationDAOImpl tabaDaoImpl = new TabadulIntegrationDAOImpl();
    TenderVO tenderVO = null;

    tenderVO = tabaDaoImpl.getTenderInformation(bidManagementId, null); // Get the tender
                                                                        // information
    log.info("Tender VO --->" + tenderVO);
    setAddressInformation(tenderVO);// set the Address Information
    log.info("Going to create Tender --->");
    MessageVO messageVO = createTender(tenderVO, bidManagementId);// Create Tender in Tabadul
    // check if the message is error or success
    if (messageVO.getIsError()) {
      return getResponseMessage(messageVO.getErrorMessageKey(), null,
          MessageTypesE.ERROR.getType());
    } else {
      String[] paramsArray = new String[2];
      paramsArray[0] = String.valueOf(messageVO.getTenderId());
      paramsArray[1] = String.valueOf(messageVO.getTotalFilesUploaded());

      return getResponseMessage(CREATE_TENDER_SUCCESS_MSG, paramsArray,
          MessageTypesE.SUCCESS.getType());
    }

  }

  /**
   * Extend Tender Dates
   * 
   * @param bidManagementId
   * @param action
   * @return
   * @throws Exception
   */
  private JSONObject extendTenderDates(String bidManagementId) throws Exception {
    log.info("<---Start extendTenderDates--> + " + bidManagementId);
    // If the open envelope created then do not cancell just send the message
    if (BidManagementDAO.isOpenEnvelopeCompleted(bidManagementId)) {
      return getResponseMessage(EXTEND_TENDER_OPENENV_ERROR_MSG, null,
          MessageTypesE.ERROR.getType());
    }
    TabadulIntegrationDAOImpl tabaDaoImpl = new TabadulIntegrationDAOImpl();
    TabadulIntegrationService tabadulIntegrationService = new TabadulIntegrationServiceImpl();

    TenderVO tenderVO = tabaDaoImpl.getTenderInformation(bidManagementId, null); // Get the tender
                                                                                 // information
    log.info("Tender VO --->" + tenderVO);
    setAddressInformation(tenderVO);// set the Address Information
    log.info("Going to extend Tender Dates --->");
    String aid = tabadulIntegrationService.extendTenderDates(tenderVO, getSessionToken(false));
    log.info("Approval Id --->" + aid);
    String status = tabadulIntegrationService.approveExtendTenderDates(aid, getSessionToken(true));
    log.info("Status --->" + status);
    tabaDaoImpl.updateTenderIdInBid(String.valueOf(tenderVO.getTenderInternalId()),
        TenderStatusE.PUBLISHED.getStatus(), bidManagementId);
    return getResponseMessage(EXTEND_TENDER_SUCCESS_MSG, null, MessageTypesE.SUCCESS.getType());

  }

  /**
   * Get the message to be displayed on UI
   * 
   * @param messageKey
   * @param parameters
   * @param messageType
   * @return
   * @throws JSONException
   */
  private JSONObject getResponseMessage(String messageKey, Object[] parameters, String messageType)
      throws JSONException {
    JSONObject responseMessage = new JSONObject();
    String message = OBMessageUtils.messageBD(messageKey);
    if (null != parameters && parameters.length > 0) {
      message = MessageFormat.format(message, parameters);
    }

    try {
      responseMessage.put(MESSAGE_SEVERITY, messageType);
      responseMessage.put(MESSAGE_TEXT, message);
    } catch (JSONException e) {
      log.debug(e.getMessage());
      responseMessage.put(MESSAGE_SEVERITY, messageType);
      responseMessage.put(MESSAGE_TEXT, OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      return responseMessage;
    }

    return responseMessage;

  }

  /**
   * Cancel the tender
   * 
   * @param bidManagementId
   * @throws Exception
   */
  @SuppressWarnings("unused")
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
   * Uploade the tender files
   * 
   * @param bidManagementId
   * @return
   * @throws Exception
   */
  private JSONObject uploadTenderFiles(String bidManagementId) throws Exception {
    TabadulIntegrationDAO tabadulIntegrationDAO = new TabadulIntegrationDAOImpl();
    TenderVO tenderVO = tabadulIntegrationDAO.getTenderInformation(bidManagementId, null);
    String sessionToken = getSessionToken(false);

    MessageVO messageVO = uploadTenderFiles(bidManagementId, sessionToken, tenderVO);
    String[] paramsArray = new String[1];

    paramsArray[0] = String.valueOf(messageVO.getTotalFilesUploaded());
    return getResponseMessage(UPLOAD_FILE_SUCCESS_MSG, paramsArray,
        MessageTypesE.SUCCESS.getType());
  }

  /**
   * Uploads the tender files to Tabadul
   * 
   * @throws Exception
   */
  private MessageVO uploadTenderFiles(String bidManagementId, String sessionToken,
      TenderVO tenderVO) throws Exception {
    MessageVO messageVO = new MessageVO();
    final String attachmentAbsolutePath = getPoolPropertiesConfig()
        .getProperty(ATTACH_ABSOLUTE_PATH);
    FileSystemResource fileSystemResource = null;
    TabadulIntegrationService tabadulIntegrationService = new TabadulIntegrationServiceImpl();
    TabadulIntegrationDAO tabadulIntegrationDAO = new TabadulIntegrationDAOImpl();

    // Get all files uploaded with bid
    List<AttachmentVO> attachements = tabadulIntegrationDAO.getAttachments(bidManagementId,
        BID_MGMT_TABLE_ID, TenderStatusE.DRAFT.getStatus());
    Integer totalFilesUploaded = 0;
    // Read all attachments from disk
    for (AttachmentVO attachmentVO : attachements) {
      try {
        String filePath = attachmentAbsolutePath + "/" + attachmentVO.getFilePath() + "/"
            + attachmentVO.getFileName();
        fileSystemResource = new FileSystemResource(new File(filePath));
        // Now upload to tabadul
        String fileId = tabadulIntegrationService.uploadTenderFile(
            String.valueOf(tenderVO.getTenderInternalId()), "attachment", fileSystemResource,
            sessionToken);
        // Now publish to tabadul
        if (null != tenderVO.getStatus()
            && tenderVO.getStatus().trim().equals(TenderStatusE.PUBLISHED.getStatus())) {
          tabadulIntegrationService.publishTenderFile(
              String.valueOf(tenderVO.getTenderInternalId()), fileId, getSessionToken(true));
        }
        // Update the status of attachment
        tabadulIntegrationDAO.updateTabadulFileStatus(TenderStatusE.PUBLISHED.getStatus(), fileId,
            attachmentVO.getTabadulAttachmentId());
        // Update the description in central attachments table
        tabadulIntegrationDAO.updateAttachmentDescription(attachmentVO.getcFileId(),
            "TABADUL STATUS : SUCCESS");
        // Increase the Count of Uploaded/ Published Files
        totalFilesUploaded++;
      } catch (Exception e) {
        // Update the file status as failed
        tabadulIntegrationDAO.updateTabadulFileStatus(TenderStatusE.FAILED.getStatus(), null,
            attachmentVO.getTabadulAttachmentId());
        // also need to update the file description
        tabadulIntegrationDAO.updateAttachmentDescription(attachmentVO.getcFileId(),
            "TABADUL STATUS : FAILED");
        e.printStackTrace();
      }
    }
    // Set the total files and total files uploaded statistics.
    messageVO.setTotalFiles(attachements.size());
    messageVO.setTotalFilesUploaded(totalFilesUploaded);

    return messageVO;
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
   * Create the tender by calling tabadul integration
   * 
   * @param tenderVOs
   */
  private MessageVO createTender(TenderVO tenderVO, String bidManagementId) throws Exception {
    MessageVO messageVO = null;
    TabadulIntegrationDAO tabadulIntegrationDAOImpl = new TabadulIntegrationDAOImpl();

    TabadulIntegrationService tabadulIntegrationService = new TabadulIntegrationServiceImpl();
    // Get the authentication token
    String sessionToken = getSessionToken(false);
    // Call the method here to create tender
    String tenderId = tabadulIntegrationService.createOrUpdateTender(sessionToken, tenderVO);
    tenderVO.setTenderInternalId(Integer.parseInt(tenderId));
    // Call the method to upload the files
    messageVO = uploadTenderFiles(bidManagementId, sessionToken, tenderVO);
    // Call the method to publish the tender
    tabadulIntegrationService.publishTender(tenderId, getSessionToken(true));
    // Call the method to update the tenderId in Bid Table
    tabadulIntegrationDAOImpl.updateTenderIdInBid(tenderId, TenderStatusE.PUBLISHED.getStatus(),
        tenderVO.getBidId());
    // Update the tender file statuses as published
    tabadulIntegrationDAOImpl.updateTenderFileStatus(bidManagementId, BID_MGMT_TABLE_ID);
    // Set the message statistics
    messageVO.setTenderId(Integer.parseInt(tenderId));

    return messageVO;
  }

  /**
   * Get the session token depending on the type of user
   * 
   * @param isAuditUser
   * @return
   */
  private String getSessionToken(Boolean isAuditUser) {
    TabadulAuthenticationService tabadulAuthenticationService = new TabadulAuthenticationServiceImpl();

    String userName = null;
    String password = null;

    if (isAuditUser) {
      userName = getPoolPropertiesConfig().getProperty(USER_AUDIT_CONFIG_PARAM);
      password = getPoolPropertiesConfig().getProperty(PWD_AUDIT_CONFIG_PARAM);

    } else {
      userName = getPoolPropertiesConfig().getProperty(USER_CONFIG_PARAM);
      password = getPoolPropertiesConfig().getProperty(PWD_CONFIG_PARAM);
    }

    return tabadulAuthenticationService.authenticate(userName, password);

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
