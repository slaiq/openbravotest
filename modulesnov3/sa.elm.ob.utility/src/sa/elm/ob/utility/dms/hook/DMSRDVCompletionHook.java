package sa.elm.ob.utility.dms.hook;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.application.report.ReportingUtils;
import org.openbravo.client.application.report.ReportingUtils.ExportType;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.utils.Replace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.pdf.PdfReader;

import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.ad_process.RDVProcess.hook.RDVSubmitCompletionHook;
import sa.elm.ob.utility.DMSIntegrationLog;
import sa.elm.ob.utility.dms.consumer.GRPDmsImplementation;
import sa.elm.ob.utility.dms.consumer.GRPDmsInterface;
import sa.elm.ob.utility.dms.consumer.dto.AddAttachmentResponseGRP;
import sa.elm.ob.utility.dms.consumer.dto.CreateAttachmentResponseGRP;
import sa.elm.ob.utility.dms.consumer.dto.PKIRequestVO;
import sa.elm.ob.utility.dms.util.DMSConstants;
import sa.elm.ob.utility.dms.util.DMSUtility;
import sa.elm.ob.utility.dms.util.DMSXmlAttributes;
import sa.elm.ob.utility.dms.util.GetServiceAccount;

public class DMSRDVCompletionHook implements RDVSubmitCompletionHook {

  private static final Logger log = LoggerFactory.getLogger(DMSRDVCompletionHook.class);
  private static int pageCount = 0;

  @Override
  public JSONObject exec(EfinRDVTransaction rdv, JSONObject paramters, VariablesSecureApp vars,
      ConnectionProvider conn) throws Exception {
    JSONObject result = new JSONObject();
    try {
      log.debug("Entering in RDV completion Hook");

      EfinRDV rdvheader = rdv.getEfinRdv();

      String preferenceValue = "";
      String status = paramters.getString("status");
      String documentType = paramters.getString("documentType");
      String userId = paramters.getString("userId");
      String tabId = paramters.getString("tabId");
      String documentName = "RDV_" + rdvheader.getDocumentNo() + "_" + rdv.getTXNVersion() + ".pdf";
      String responseData = "";
      boolean isFinalLevelApproval = paramters.getBoolean("isFinalLevelApprove");

      String profileURI = GetServiceAccount.getProperty(DMSConstants.DMS_PROFILE_URI);
      String nodeID = GetServiceAccount.getProperty(DMSConstants.DMS_NODE_ID);

      GRPDmsInterface dmsGRP = new GRPDmsImplementation();
      CreateAttachmentResponseGRP response = null;
      AddAttachmentResponseGRP addResponse = null;

      List<String> attachmentPathList = new ArrayList<String>();
      List<String> documentNameList = new ArrayList<String>();
      List<String> processIdList = new ArrayList<String>();

      try {
        preferenceValue = org.openbravo.erpCommon.businessUtility.Preferences.getPreferenceValue(
            "Eut_AllowDMSIntegration", true, vars.getClient(), rdv.getOrganization().getId(), null,
            null, null);

        preferenceValue = (preferenceValue == null) ? "N" : preferenceValue;
      } catch (PropertyException e) {
        preferenceValue = "N";
      }

      log.debug("Preference value= " + preferenceValue);

      if ("Y".equals(preferenceValue) && OBContext.getOBContext().getUser().isEutIssignrequired()) {

        if ("submit".equals(status)) {
          log.debug("inside submit block");

          File file = generateReport(rdv);
          rdv.setEUTDocumentType(documentType);
          rdv.setEutApprovalPosition("1");
          OBDal.getInstance().save(rdv);
          OBDal.getInstance().flush();

          DMSIntegrationLog dmslog = DMSUtility.createDMSIntegrationLog(null,
              DMSConstants.DMS_CREATE, rdv);
          DMSXmlAttributes dmsAttributes = new DMSXmlAttributes(documentType, rdv.getId(),
              rdvheader.getDocumentNo(), dmslog.getId(), userId);

          try (InputStream inp = new FileInputStream(file)) {
            byte[] bytes = IOUtils.toByteArray(inp);
            responseData = Base64.getEncoder().encodeToString(bytes);
            file.delete();
          } catch (Exception e) {
            log.error("Exception in Generating report: ", e);
            e.printStackTrace();
            OBDal.getInstance().rollbackAndClose();
          }

          if (rdv.getEutDmsrecordpath() == null) {
            log.debug("inside create record and attachment block ");
            response = dmsGRP.sendReportToDMS(profileURI, responseData, nodeID,
                rdvheader.getDocumentNo() + "-" + rdv.getTXNVersion(),
                rdvheader.getDocumentNo() + "-" + rdv.getTXNVersion(), dmsAttributes);
            log.debug("inside create record and attachmentblock response" + response);
            if (response != null) {
              setCreateResponse(response, dmslog, userId, documentName, rdv, documentType, tabId,
                  vars, attachmentPathList, documentNameList, processIdList);
            }
          } else {
            log.debug("inside add attachment block ");
            addResponse = dmsGRP.addAttachmentinDMS(profileURI.concat(rdv.getEutDmsrecordpath()),
                responseData, documentName, documentName);
            log.debug("inside add attachment block response" + addResponse);
            if (addResponse != null) {
              setAddAttachmentResponse(addResponse, dmslog, userId, documentName, rdv, documentType,
                  tabId, vars, attachmentPathList, documentNameList, processIdList);
            }
          }
          OBDal.getInstance().flush();
        } else if (status.equals("approve")) {
          log.debug("inside approve block");

          if (isFinalLevelApproval) {
            log.debug("inside isFinalLevelApproval block");
            OBQuery<Attachment> attachQry = OBDal.getInstance().createQuery(Attachment.class,
                "as e where e.record = :recordId and e.eutDmsAttachpath is not null and e.table.id ='B4146A5918884533B13F57A574EFF9D5' order by e.creationDate desc");
            attachQry.setNamedParameter("recordId", rdv.getId());
            List<Attachment> fileList = attachQry.list();
            for (Attachment attach : fileList) {
              String extension = FilenameUtils.getExtension(attach.getName());
              if ("pdf".equals(extension)) {
                attachmentPathList.add(attach.getEutDmsAttachpath());
                documentNameList.add(attach.getName());
                processIdList.add(attach.getId());
              }

            }
          }

          if (rdv.getEutAttachPath() == null) {
            File file = generateReport(rdv);
            DMSIntegrationLog dmslog = DMSUtility.createDMSIntegrationLog(null,
                DMSConstants.DMS_CREATE, rdv);
            DMSXmlAttributes dmsAttributes = new DMSXmlAttributes(documentType, rdv.getId(),
                rdvheader.getDocumentNo(), dmslog.getId(), userId);

            try (InputStream inp = new FileInputStream(file)) {
              byte[] bytes = IOUtils.toByteArray(inp);
              responseData = Base64.getEncoder().encodeToString(bytes);
              file.delete();

              if (rdv.getEutDmsrecordpath() == null) {
                log.debug("inside create record and attachment block ");
                response = dmsGRP.sendReportToDMS(profileURI, responseData, nodeID,
                    rdvheader.getDocumentNo() + "-" + rdv.getTXNVersion(),
                    rdvheader.getDocumentNo() + "-" + rdv.getTXNVersion(), dmsAttributes);
                log.debug("inside create record and attachmentblock response" + response);
                if (response != null) {
                  setCreateResponse(response, dmslog, userId, documentName, rdv, documentType,
                      tabId, vars, attachmentPathList, documentNameList, processIdList);
                }
              } else {
                log.debug("inside add attachment block ");
                addResponse = dmsGRP.addAttachmentinDMS(
                    profileURI.concat(rdv.getEutDmsrecordpath()), responseData, documentName,
                    documentName);
                log.debug("inside add attachment block response" + addResponse);
                if (addResponse != null) {
                  setAddAttachmentResponse(addResponse, dmslog, userId, documentName, rdv,
                      documentType, tabId, vars, attachmentPathList, documentNameList,
                      processIdList);
                }
              }
            } catch (Exception e) {
              log.error("Exception in Generating report: ", e);
              e.printStackTrace();
              OBDal.getInstance().rollbackAndClose();
            }
            OBDal.getInstance().flush();
          } else {
            log.debug("inside adding sign in the attachment block response" + addResponse);

            DMSIntegrationLog dmslog = DMSUtility.createDMSIntegrationLog(null,
                DMSConstants.DMS_UPDATE, rdv);

            PKIRequestVO pkiRequest = null;

            if (rdv.getEutApprovalPosition() != null) {
              rdv.setEutApprovalPosition(
                  String.valueOf(Integer.parseInt(rdv.getEutApprovalPosition()) + 1));
              OBDal.getInstance().save(rdv);
            }

            if (attachmentPathList.size() > 0) {
              String attachmentName = attachmentPathList.stream().collect(Collectors.joining(","));
              String documentNme = documentNameList.stream().collect(Collectors.joining(","));
              String processId = processIdList.stream().collect(Collectors.joining(","));

              pkiRequest = new PKIRequestVO(rdv.getEutAttachPath() + "," + attachmentName,
                  dmslog.getId() + "," + processId, userId, documentName + "," + documentNme,
                  rdv.getEutApprovalPosition(), documentType, pageCount);
            } else {
              pkiRequest = new PKIRequestVO(rdv.getEutAttachPath(), dmslog.getId(), userId,
                  documentName, rdv.getEutApprovalPosition(), documentType, pageCount);
            }

            vars.setAdditionalData(tabId, pkiRequest);
            dmslog.setPkirequest(pkiRequest.toString());
            dmslog.setGrprequestid(pkiRequest.getGrpRequestID());
            dmslog.setPagecount(new Long(pageCount));
            dmslog.setProfileuri(pkiRequest.getProfileURI());
            dmslog.setDocumentname(pkiRequest.getDocumentName());
            dmslog.setApprovalposition(rdv.getEutApprovalPosition());

            OBDal.getInstance().save(dmslog);
            OBDal.getInstance().flush();
          }

        }
      }

    } catch (Exception e) {
      log.error("Error in dmsrdvcompletionhook" + e.getMessage());
    }
    return result;
  }

  /**
   * This method is used to generate report of rdv summary
   * 
   * @param rdv
   * @return
   */
  public static File generateReport(EfinRDVTransaction rdv) {
    HashMap<String, Object> designParameters = new HashMap<String, Object>();
    HashMap<Object, Object> exportParameters = new HashMap<Object, Object>();
    String strOutput = "pdf", imageFlag = "N";
    String strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/RDVSummary/RDVSummary_1.jrxml";
    File outputFile = null;
    try {

      log.debug("Generating rdv report starts");
      ConnectionProvider conn = new DalConnectionProvider(false);
      final ExportType expType = ExportType.getExportType(strOutput.toUpperCase());
      String strBaseDesign = PurchaseInvoiceSubmitUtils.getBaseDesignPath();
      strReportName = Replace.replace(strReportName, "@basedesign@", strBaseDesign);
      String directory = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty("attach.path");
      OrganizationInformation objInfo = rdv.getOrganization().getOrganizationInformationList()
          .get(0);
      // check org have image
      if (objInfo != null) {
        if (objInfo.getYourCompanyDocumentImage() != null) {
          imageFlag = "Y";
        }
      }

      EfinRDVTransaction transaction = rdv;
      String filePathWithName = directory + "/" + "RDVSummary" + "-"
          + rdv.getEfinRdv().getDocumentNo() + "-" + rdv.getTXNVersion() + "." + strOutput;
      designParameters.put("BASE_DESIGN", strBaseDesign);
      designParameters.put("inpImageFlag", imageFlag);
      designParameters.put("inpOrgId", transaction.getOrganization().getId());
      designParameters.put("Version_Id", transaction.getId());
      designParameters.put("Efin_Rdv_ID", transaction.getEfinRdv().getId());
      designParameters.put("TXN_Type", transaction.getEfinRdv().getTXNType());

      outputFile = new File(filePathWithName);

      ReportingUtils.exportJR(strReportName, expType, designParameters, outputFile, false, conn,
          null, exportParameters);

      PdfReader pdfreader = new PdfReader(filePathWithName);
      pageCount = pdfreader.getNumberOfPages();

    } catch (Exception e) {
      log.error("Error while generating report of rdv" + e.getMessage());
    }
    return outputFile;
  }

  public static void setCreateResponse(CreateAttachmentResponseGRP response,
      DMSIntegrationLog dmslog, String userId, String documentName, EfinRDVTransaction rdv,
      String documentType, String tabId, VariablesSecureApp vars, List<String> attachmentList,
      List<String> documentList, List<String> processList) {
    try {
      OBContext.setAdminMode();
      log.debug("inside setCreateResponse");

      if (!response.isHasError()) {
        PKIRequestVO pkiRequest = null;
        if (attachmentList.size() > 0) {
          String attachmentName = attachmentList.stream().collect(Collectors.joining(","));
          String documentNme = documentList.stream().collect(Collectors.joining(","));
          String processId = processList.stream().collect(Collectors.joining(","));

          pkiRequest = new PKIRequestVO(response.getAttachmentPath() + "," + attachmentName,
              dmslog.getId() + "," + processId, userId, documentName + "," + documentNme,
              rdv.getEutApprovalPosition(), documentType, pageCount);
        } else {
          pkiRequest = new PKIRequestVO(response.getAttachmentPath(), dmslog.getId(), userId,
              documentName, rdv.getEutApprovalPosition(), documentType, pageCount);
        }
        vars.setAdditionalData(tabId, pkiRequest);
        dmslog.setPkirequest(pkiRequest.toString());
        dmslog.setGrprequestid(pkiRequest.getGrpRequestID());
        dmslog.setPagecount(new Long(pageCount));
        dmslog.setProfileuri(pkiRequest.getProfileURI());
        dmslog.setDocumentname(pkiRequest.getDocumentName());
        dmslog.setApprovalposition(rdv.getEutApprovalPosition());
        dmslog.setResponsemessage(response.getResponse());
        dmslog.setAlertStatus(DMSConstants.DMS_SUCCESS);
        dmslog.setAttachmentpath(response.getAttachmentPath());
        dmslog.setRequest(response.getRequest());
        dmslog.setResponsemessage(response.getResponse());
        rdv.setEutAttachPath(response.getAttachmentPath());
        rdv.setEutDmsrecordpath(response.getRecordPath());
        OBDal.getInstance().save(rdv);
        OBDal.getInstance().save(dmslog);
      } else {
        dmslog.setGrprequestid(processList.stream().collect(Collectors.joining(",")));
        dmslog.setProfileuri(attachmentList.stream().collect(Collectors.joining(",")));
        dmslog.setDocumentname(documentList.stream().collect(Collectors.joining(",")));
        dmslog.setApprovalposition(rdv.getEutApprovalPosition());
        dmslog.setResponsemessage(response.getErrorMsg());
        dmslog.setRequest(response.getRequest());
        dmslog.setAlertStatus(DMSConstants.DMS_FAILED);
        OBDal.getInstance().save(dmslog);
      }

    } catch (Exception e) {
      log.error("Error while creating response " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static void setAddAttachmentResponse(AddAttachmentResponseGRP response,
      DMSIntegrationLog dmslog, String userId, String documentName, EfinRDVTransaction rdv,
      String documentType, String tabId, VariablesSecureApp vars, List<String> attachmentList,
      List<String> documentList, List<String> processList) {
    try {
      OBContext.setAdminMode();
      log.debug("inside setAddAttachmentResponse");

      if (!response.isHasError()) {
        PKIRequestVO pkiRequest = null;
        if (attachmentList.size() > 0) {
          String attachmentName = attachmentList.stream().collect(Collectors.joining(","));
          String documentNme = documentList.stream().collect(Collectors.joining(","));
          String processId = processList.stream().collect(Collectors.joining(","));
          pkiRequest = new PKIRequestVO(response.getAttachmentPath() + "," + attachmentName,
              dmslog.getId() + "," + processId, userId, documentName + "," + documentNme,
              rdv.getEutApprovalPosition(), documentType, pageCount);
        } else {
          pkiRequest = new PKIRequestVO(response.getAttachmentPath(), dmslog.getId(), userId,
              documentName, rdv.getEutApprovalPosition(), documentType, pageCount);
        }
        vars.setAdditionalData(tabId, pkiRequest);
        dmslog.setRequestname(DMSConstants.DMS_ADD);
        dmslog.setPkirequest(pkiRequest.toString());
        dmslog.setGrprequestid(pkiRequest.getGrpRequestID());
        dmslog.setPagecount(new Long(pageCount));
        dmslog.setProfileuri(pkiRequest.getProfileURI());
        dmslog.setDocumentname(pkiRequest.getDocumentName());
        dmslog.setApprovalposition(rdv.getEutApprovalPosition());
        dmslog.setResponsemessage(response.getResponse());
        dmslog.setAlertStatus(DMSConstants.DMS_SUCCESS);
        dmslog.setAttachmentpath(response.getAttachmentPath());
        dmslog.setRequest(response.getRequest());
        dmslog.setResponsemessage(response.getResponse());
        rdv.setEutAttachPath(response.getAttachmentPath());
        OBDal.getInstance().save(rdv);
        OBDal.getInstance().save(dmslog);
      } else {
        dmslog.setGrprequestid(processList.stream().collect(Collectors.joining(",")));
        dmslog.setProfileuri(attachmentList.stream().collect(Collectors.joining(",")));
        dmslog.setDocumentname(documentList.stream().collect(Collectors.joining(",")));
        dmslog.setPagecount(new Long(pageCount));
        dmslog.setApprovalposition(rdv.getEutApprovalPosition());
        dmslog.setResponsemessage(response.getErrorMsg());
        dmslog.setRequest(response.getRequest());
        dmslog.setAlertStatus(DMSConstants.DMS_FAILED);
        OBDal.getInstance().save(dmslog);
      }
    } catch (Exception e) {
      log.error("Error while creating response " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
