package sa.elm.ob.utility.dms.util;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.invoice.Invoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.utility.DMSIntegrationLog;
import sa.elm.ob.utility.EUTSignatureConfig;
import sa.elm.ob.utility.dms.consumer.dto.AddAttachmentResponseGRP;
import sa.elm.ob.utility.dms.consumer.dto.CreateAttachmentResponseGRP;
import sa.elm.ob.utility.dms.consumer.dto.DeleteAttachmentResponseGRP;
import sa.elm.ob.utility.dms.consumer.dto.DeleteRecordGRPResponse;
import sa.elm.ob.utility.dms.consumer.dto.GetAttachmentGRPResponse;
import sa.elm.ob.utility.dms.org.datacontract.schemas._2004._07.mot_services_dms.ArrayOfXmlAttribute;
import sa.elm.ob.utility.dms.org.datacontract.schemas._2004._07.mot_services_dms.Document;
import sa.elm.ob.utility.dms.org.datacontract.schemas._2004._07.mot_services_dms.Information;
import sa.elm.ob.utility.dms.org.datacontract.schemas._2004._07.mot_services_dms.ResponseRoot;
import sa.elm.ob.utility.dms.org.datacontract.schemas._2004._07.mot_services_dms.XmlAttribute;
import sa.elm.ob.utility.dms.org.tempuri.AddAttachmentRequest;
import sa.elm.ob.utility.dms.org.tempuri.CreateRecordWithAttachmentRequest;
import sa.elm.ob.utility.dms.org.tempuri.DeleteAttachmentRequest;
import sa.elm.ob.utility.dms.org.tempuri.DeleteRecordRequest;
import sa.elm.ob.utility.dms.org.tempuri.GetAttachmentRequest;
import sa.elm.ob.utility.dms.org.tempuri.GetRecordRequest;
import sa.elm.ob.utility.dms.org.tempuri.Response;

public class DMSUtility {

  private static final Logger log = LoggerFactory.getLogger(DMSUtility.class);

  private final static QName _Attachmentbase64_QNAME = new QName("http://tempuri.org/",
      "AttachmentBase64");
  private final static QName _ProfileURI_QNAME = new QName("http://tempuri.org/", "ProfileURI");
  private final static QName _NodeID_QNAME = new QName("http://tempuri.org/", "NodeID");
  private final static QName _DocumentName_QNAME = new QName("http://tempuri.org/", "DocumentName");
  private final static QName _Description_QNAME = new QName("http://tempuri.org/", "Description");
  private final static QName _AttributeName_QNAME = new QName("http://tempuri.org/",
      "AttributeName");
  private final static QName _AttributeValue_QNAME = new QName("http://tempuri.org/",
      "AttributeValue");
  private final static QName _xmlAttributes_QNAME = new QName("http://tempuri.org/",
      "XmlAttributes");

  /**
   * This method is used to form request to create attachment in DMS
   * 
   * @param profileURI
   * @param attachmentBase64
   * @param nodeID
   * @param documentName
   * @param description
   * @return
   */
  public static CreateRecordWithAttachmentRequest createRequestAttachment(String profileURI,
      String attachmentBase64, String nodeID, String documentName, String description,
      DMSXmlAttributes attributes) {
    CreateRecordWithAttachmentRequest request = new CreateRecordWithAttachmentRequest();
    try {
      log.debug("Inside the createRequestAttachment");
      request.setAttachmentBase64(
          new JAXBElement<String>(_Attachmentbase64_QNAME, String.class, attachmentBase64));
      request
          .setDescription(new JAXBElement<String>(_Description_QNAME, String.class, description));
      request.setDocumentName(
          new JAXBElement<String>(_DocumentName_QNAME, String.class, documentName + ".pdf"));
      request.setNodeID(new JAXBElement<String>(_NodeID_QNAME, String.class, nodeID));
      request.setProfileURI(new JAXBElement<String>(_ProfileURI_QNAME, String.class, profileURI));

      List<XmlAttribute> attrList = new ArrayList<>();
      ArrayOfXmlAttribute attr = new ArrayOfXmlAttribute();

      XmlAttribute documentType = new XmlAttribute();
      documentType.setAttributeName(
          new JAXBElement<String>(_AttributeName_QNAME, String.class, DMSConstants.ATTR_DOC_TYPE));
      documentType.setAttributeValue(new JAXBElement<String>(_AttributeValue_QNAME, String.class,
          attributes.getDocumentType()));
      attrList.add(documentType);

      XmlAttribute recordId = new XmlAttribute();
      documentType.setAttributeName(
          new JAXBElement<String>(_AttributeName_QNAME, String.class, DMSConstants.ATTR_RECORD_ID));
      documentType.setAttributeValue(
          new JAXBElement<String>(_AttributeValue_QNAME, String.class, attributes.getRecordId()));
      attrList.add(recordId);

      XmlAttribute documentNO = new XmlAttribute();
      documentType.setAttributeName(new JAXBElement<String>(_AttributeName_QNAME, String.class,
          DMSConstants.ATTR_DOCUMENT_NO));
      documentType.setAttributeValue(
          new JAXBElement<String>(_AttributeValue_QNAME, String.class, attributes.getDocumentNo()));
      attrList.add(documentNO);

      XmlAttribute userId = new XmlAttribute();
      documentType.setAttributeName(
          new JAXBElement<String>(_AttributeName_QNAME, String.class, DMSConstants.ATTR_CREATEBY));
      documentType.setAttributeValue(
          new JAXBElement<String>(_AttributeValue_QNAME, String.class, attributes.getCreatedBy()));
      attrList.add(userId);

      XmlAttribute processId = new XmlAttribute();
      documentType.setAttributeName(new JAXBElement<String>(_AttributeName_QNAME, String.class,
          DMSConstants.ATTR_PROCESS_ID));
      documentType.setAttributeValue(
          new JAXBElement<String>(_AttributeValue_QNAME, String.class, attributes.getProcessId()));
      attrList.add(processId);

      attr.getXmlAttribute().addAll(attrList);

      request.setXmlAttributes(new JAXBElement<ArrayOfXmlAttribute>(_xmlAttributes_QNAME,
          ArrayOfXmlAttribute.class, attr));

    } catch (Exception e) {
      log.error("Exception in creating request" + e.getMessage());
    }
    log.debug("End of the createRequestAttachment block");
    return request;
  }

  /**
   * This method is used to form request to get attachment
   * 
   * @param profileURI
   * @return
   */
  public static GetAttachmentRequest getattachment(String profileURI) {
    GetAttachmentRequest getRequest = new GetAttachmentRequest();
    try {
      log.debug("start of the getattachment block");
      getRequest
          .setProfileURI(new JAXBElement<String>(_ProfileURI_QNAME, String.class, profileURI));
    } catch (Exception e) {
      log.error("Exception in creating request" + e.getMessage());
    }
    log.debug("end of the getattachment block");
    return getRequest;
  }

  /**
   * This method is used to form delete record request
   * 
   * @param profileURI
   * @return
   */
  public static DeleteRecordRequest deleteRecordRequest(String profileURI) {

    DeleteRecordRequest getRequest = new DeleteRecordRequest();
    try {
      log.debug("start of the deleteRecordRequest block");
      getRequest
          .setProfileURI(new JAXBElement<String>(_ProfileURI_QNAME, String.class, profileURI));
    } catch (Exception e) {
      log.error("Exception in creating request" + e.getMessage());
    }
    log.debug("end of the deleteRecordRequest block");
    return getRequest;
  }

  /**
   * This method is used to form delete attachment request
   * 
   * @param profileURI
   * @return
   */
  public static DeleteAttachmentRequest deleteAttachmentRequest(String profileURI) {

    DeleteAttachmentRequest getRequest = new DeleteAttachmentRequest();
    try {
      log.debug("Start of the deleteAttachmentRequest block");
      getRequest
          .setProfileURI(new JAXBElement<String>(_ProfileURI_QNAME, String.class, profileURI));
    } catch (Exception e) {
      log.error("Exception in creating request" + e.getMessage());
    }
    log.debug("End of the deleteAttachmentRequest block");
    return getRequest;
  }

  /**
   * This method is used to get record request
   * 
   * @param profileURI
   * @return
   */
  public static GetRecordRequest getRecordRequest(String profileURI) {
    GetRecordRequest getRequest = new GetRecordRequest();
    try {
      log.debug("Start of the getRecordRequest block");
      getRequest
          .setProfileURI(new JAXBElement<String>(_ProfileURI_QNAME, String.class, profileURI));
    } catch (Exception e) {
      log.error("Exception in creating request" + e.getMessage());
    }
    log.debug("End of the getRecordRequest block");
    return getRequest;
  }

  /**
   * This method is used to create add attachment request
   * 
   * @param profileURI
   * @param attachmentBase64
   * @param nodeID
   * @param documentName
   * @param description
   * @return
   */
  public static AddAttachmentRequest addAttachmentRequest(String profileURI,
      String attachmentBase64, String documentName, String description) {
    AddAttachmentRequest request = new AddAttachmentRequest();
    try {
      log.debug("Start of the addAttachmentRequest block");
      request.setAttachmentBase64(
          new JAXBElement<String>(_Attachmentbase64_QNAME, String.class, attachmentBase64));
      request
          .setDescription(new JAXBElement<String>(_Description_QNAME, String.class, description));
      request.setDocumentName(
          new JAXBElement<String>(_DocumentName_QNAME, String.class, documentName));
      request.setProfileURI(new JAXBElement<String>(_ProfileURI_QNAME, String.class, profileURI));

    } catch (Exception e) {
      log.error("Exception in creating Add attachment request" + e.getMessage());
    }
    log.debug("end of the addAttachmentRequest block");

    return request;
  }

  /**
   * This method is used to create response from dms response object
   * 
   * @return {@link CreateAttachmentResponseGRP}
   */
  public static CreateAttachmentResponseGRP createResponse(Response response,
      CreateRecordWithAttachmentRequest req, String exceptionmsg) {
    CreateAttachmentResponseGRP grpResponse = new CreateAttachmentResponseGRP();
    String recordId = null;
    try {
      OBContext.setAdminMode();
      log.debug("start of the createResponse block");
      if (response != null) {
        if (response.isHasError()) {
          grpResponse.setHasError(true);
          grpResponse.setErrorMsg(response.getErrorMessage().toString());
        } else {
          ResponseRoot responseroot = response.getResponseRoot().getValue();
          Information information = responseroot.getInformation().getValue();
          Document doc = responseroot.getDocument().getValue();
          recordId = information.getMessage().getValue();
          grpResponse.setHasError(false);
          grpResponse.setRecordPath(getRecordNo(recordId));
          if (doc != null) {
            grpResponse.setAttachmentPath(doc.getURI().getValue());
          }
        }
        grpResponse.setResponse(DMSUtility.convertDMSResponsetoString(response));
      } else {
        grpResponse.setHasError(true);
        grpResponse.setErrorMsg(exceptionmsg);
      }
      grpResponse.setRequest(DMSUtility.convertDMSRequesttoString(req));
    } catch (Exception e) {
      log.error("eror while creating response " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    log.debug("end of the createResponse block");
    return grpResponse;
  }

  /**
   * This method is used to get the record number from message
   * 
   * Message will be in the format esdb:///DMS/TESTTABLE12/1213 In this 1213 will be record no
   * 
   * @param recordId
   * @return recordno
   */

  private static String getRecordNo(String recordId) {
    String recordNo = null;
    try {
      if (StringUtils.isNotBlank(recordId)) {
        String record[] = recordId.split("/");
        recordNo = record[record.length - 1];
      }
    } catch (Exception e) {
      log.error("Error while getting record no" + e.getMessage());
    }
    return recordNo;
  }

  /**
   * This method is used to log the history of dms in {@link DMSIntegrationLog} table
   * 
   * @param invoice
   * @param error
   * @param erroMsg
   * @param request
   * @param response
   */
  public static DMSIntegrationLog createDMSIntegrationLog(
      org.openbravo.model.common.invoice.Invoice invoice, String requestName,
      EfinRDVTransaction rdvtrxn) {
    DMSIntegrationLog dmsLog = OBProvider.getInstance().get(DMSIntegrationLog.class);
    int seqno = 0;
    try {
      log.debug("start of the createDMSIntegrationLog block");
      OBContext.setAdminMode();
      if (invoice != null) {
        seqno = invoice.getEutDmsintegrationLogList().size();
      } else if (rdvtrxn != null) {
        seqno = rdvtrxn.getEutDmsintegrationLogList().size();

      }
      dmsLog.setAlertStatus(DMSConstants.DMS_WAIT);
      dmsLog.setInvoice(invoice);
      dmsLog.setAttachmentpath(null);
      dmsLog.setSequenceNumber((Long.valueOf(seqno) + 1) * 10);
      dmsLog.setRequestname(requestName);
      dmsLog.setEfinRdvtxn(rdvtrxn);
      OBDal.getInstance().save(dmsLog);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log.debug("Error while creating dms intgeration log" + e.getMessage());
    }
    log.debug("end of the createDMSIntegrationLog block");
    return dmsLog;
  }

  public static String convertDMSRequesttoString(CreateRecordWithAttachmentRequest request) {
    String str = "";
    try {
      log.debug("start of the convertDMSRequesttoString block");
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      JAXBContext ctx = JAXBContext.newInstance(CreateRecordWithAttachmentRequest.class);
      Marshaller marshaller = ctx.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      marshaller.marshal(request, baos);
      str = baos.toString();
    } catch (Exception e) {
      e.printStackTrace();
    }
    log.debug("end of the convertDMSRequesttoString block");
    log.debug("convertDMSRequesttoString:" + str);
    if (str != null) {
      if (str.length() > 80000) {
        return str.substring(0, 79999);
      } else {
        return str;
      }
    }
    return str;
  }

  public static String convertDMSGetRequesttoString(GetAttachmentRequest request) {
    String str = "";
    try {
      log.debug("start of the convertDMSGetRequesttoString block");
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      JAXBContext ctx = JAXBContext.newInstance(GetAttachmentRequest.class);
      Marshaller marshaller = ctx.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      marshaller.marshal(request, baos);
      str = baos.toString();
    } catch (Exception e) {
      e.printStackTrace();
    }
    log.debug("end of the convertDMSGetRequesttoString block");
    log.debug("convertDMSGetRequesttoString:" + str);

    if (str != null) {
      if (str.length() > 80000) {
        return str.substring(0, 79999);
      } else {
        return str;
      }
    }
    return str;
  }

  public static String convertDMSResponsetoString(Response response) {
    String str = "";
    try {
      log.debug("start of the convertDMSResponsetoString block");
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      JAXBContext ctx = JAXBContext.newInstance(Response.class);
      Marshaller marshaller = ctx.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      marshaller.marshal(response, baos);
      str = baos.toString();
    } catch (Exception e) {
      e.printStackTrace();
    }
    log.debug("end of the convertDMSResponsetoString block");
    return str;
  }

  public static String convertDMSAddAttachmentRequesttoString(AddAttachmentRequest request) {
    String str = "";
    try {
      log.debug("start of the convertDMSAddAttachmentRequesttoString block");
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      JAXBContext ctx = JAXBContext.newInstance(AddAttachmentRequest.class);
      Marshaller marshaller = ctx.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      marshaller.marshal(request, baos);
      str = baos.toString();
    } catch (Exception e) {
      e.printStackTrace();
    }
    log.debug("end of the convertDMSAddAttachmentRequesttoString block");
    log.debug("convertDMSAddAttachmentRequesttoString:" + str);

    if (str != null) {
      if (str.length() > 80000) {
        return str.substring(0, 79999);
      } else {
        return str;
      }
    }
    return str;
  }

  public static String convertDMSDeleteRequesttoString(DeleteRecordRequest request) {
    String str = "";
    try {
      log.debug("start of the convertDMSDeleteRequesttoString block");

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      JAXBContext ctx = JAXBContext.newInstance(DeleteRecordRequest.class);
      Marshaller marshaller = ctx.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      marshaller.marshal(request, baos);
      str = baos.toString();
    } catch (Exception e) {
      e.printStackTrace();
    }
    log.debug("end of the convertDMSDeleteRequesttoString block");
    log.debug("convertDMSDeleteRequesttoString:" + str);

    if (str != null) {
      if (str.length() > 80000) {
        return str.substring(0, 79999);
      } else {
        return str;
      }
    }
    return str;
  }

  public static String convertDMSDeleteAttachmentRequesttoString(DeleteAttachmentRequest request) {
    String str = "";
    try {
      log.debug("start of the convertDMSDeleteAttachmentRequesttoString block");
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      JAXBContext ctx = JAXBContext.newInstance(DeleteAttachmentRequest.class);
      Marshaller marshaller = ctx.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      marshaller.marshal(request, baos);
      str = baos.toString();
    } catch (Exception e) {
      e.printStackTrace();
    }
    log.debug("end of the convertDMSDeleteAttachmentRequesttoString block");
    log.debug("convertDMSDeleteAttachmentRequesttoString:" + str);

    if (str != null) {
      if (str.length() > 80000) {
        return str.substring(0, 79999);
      } else {
        return str;
      }
    }
    return str;
  }

  /**
   * This method is used to get the position of signature to be signed
   * 
   * @param documentType
   * @param level
   * @return int
   */

  public static String getPKIPosition(String documentType, String level) {
    String position = DMSConstants.POSITION_LEFT;
    try {
      OBContext.setAdminMode();
      OBQuery<EUTSignatureConfig> signatureConfig = OBDal.getInstance()
          .createQuery(EUTSignatureConfig.class, "as e where e.documentType =:doctype");
      signatureConfig.setNamedParameter("doctype", documentType);
      java.util.List<EUTSignatureConfig> configList = signatureConfig.list();
      if (configList.size() > 0) {
        EUTSignatureConfig config = configList.get(0);
        if (config.getPosition1().equals(level)) {
          position = DMSConstants.POSITION_LEFT;
        } else if (config.getPosition2().equals(level)) {
          position = DMSConstants.POSITION_CENTER;
        } else {
          position = DMSConstants.POSITION_RIGHT;
        }
      }
    } catch (Exception e) {
      log.error("Error while getting PKIPosition" + e.getMessage());
    }
    return position;
  }

  /**
   * This method is used to create getresponse from dms response object
   * 
   * @return {@link CreateAttachmentResponseGRP}
   */
  public static GetAttachmentGRPResponse createGRPGetResponse(Response response,
      GetAttachmentRequest req, String exceptionMsg) {
    GetAttachmentGRPResponse grpResponse = new GetAttachmentGRPResponse();
    try {
      log.debug("start of the createGRPGetResponse block");
      OBContext.setAdminMode();
      if (response != null) {
        if (!response.isHasError()) {
          ResponseRoot root = response.getResponseRoot().getValue();
          Document document = root.getDocument().getValue();
          grpResponse.setError(false);
          grpResponse.setBase64Str(document.getFileBase64().getValue());
        } else {
          grpResponse.setError(true);
          grpResponse.setErrorMsg(response.getErrorMessage().getValue().toString());
        }
        grpResponse.setResponse(DMSUtility.convertDMSResponsetoString(response));
      } else {
        grpResponse.setError(true);
        grpResponse.setErrorMsg(exceptionMsg);
      }
      grpResponse.setRequest(DMSUtility.convertDMSGetRequesttoString(req));
    } catch (Exception e) {
      log.error("error in createGRPGetResponse" + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    log.debug("end of the createGRPGetResponse block");
    return grpResponse;
  }

  /**
   * This method is used to create Delete record response from dms response object
   * 
   * @return {@link DeleteRecordGRPResponse}
   */
  public static DeleteRecordGRPResponse createGRPDeleteRecordResponse(Response response,
      DeleteRecordRequest req, String exceptionMsg) {
    DeleteRecordGRPResponse grpResponse = new DeleteRecordGRPResponse();
    try {
      log.debug("start of the createGRPDeleteRecordResponse block");

      OBContext.setAdminMode();
      if (response != null) {
        if (!response.isHasError()) {
          ResponseRoot root = response.getResponseRoot().getValue();
          Information information = root.getInformation().getValue();
          grpResponse.setError(false);
          grpResponse.setOperationSuccess(information.isOperationSuccess());
        } else {
          grpResponse.setError(true);
          grpResponse.setErrorMsg(response.getErrorMessage().toString());
        }
        grpResponse.setResponse(DMSUtility.convertDMSResponsetoString(response));

      } else {
        grpResponse.setError(true);
        grpResponse.setErrorMsg(exceptionMsg);
      }
      grpResponse.setRequest(DMSUtility.convertDMSDeleteRequesttoString(req));
    } catch (Exception e) {
      OBContext.restorePreviousMode();
    }
    log.debug("end of the createGRPDeleteRecordResponse block");

    return grpResponse;
  }

  /**
   * This method is used to create Delete attachment response from dms response object
   * 
   * @return {@link CreateAttachmentResponseGRP}
   */
  public static DeleteAttachmentResponseGRP createGRPDeleteAttachmentResponse(Response response,
      DeleteAttachmentRequest req, String exceptionMsg) {
    DeleteAttachmentResponseGRP grpResponse = new DeleteAttachmentResponseGRP();
    try {
      log.debug("start of the createGRPDeleteAttachmentResponse block");

      OBContext.setAdminMode();
      if (response != null) {
        if (!response.isHasError()) {
          ResponseRoot root = response.getResponseRoot().getValue();
          Information information = root.getInformation().getValue();
          grpResponse.setError(false);
          grpResponse.setOperationSuccess(information.isOperationSuccess());
        } else {
          grpResponse.setError(true);
          grpResponse.setErrorMsg(response.getErrorMessage().toString());
        }
        grpResponse.setResponse(DMSUtility.convertDMSResponsetoString(response));

      } else {
        grpResponse.setError(true);
        grpResponse.setErrorMsg(exceptionMsg);
      }
      grpResponse.setRequest(DMSUtility.convertDMSDeleteAttachmentRequesttoString(req));
    } catch (Exception e) {

    } finally {
      OBContext.restorePreviousMode();
    }
    log.debug("end of the createGRPDeleteAttachmentResponse block");

    return grpResponse;
  }

  /**
   * This method is used to create response from dms add attachment response object
   * 
   * @return {@link CreateAttachmentResponseGRP}
   */
  public static AddAttachmentResponseGRP createAddAttachmentResponse(Response response,
      AddAttachmentRequest req, String exceptionmsg) {
    AddAttachmentResponseGRP grpResponse = new AddAttachmentResponseGRP();
    try {
      log.debug("start of the createAddAttachmentResponse block");
      OBContext.setAdminMode();
      if (response != null) {
        if (response.isHasError()) {
          grpResponse.setHasError(true);
          grpResponse.setErrorMsg(response.getErrorMessage().toString());
        } else {
          ResponseRoot responseroot = response.getResponseRoot().getValue();
          Information info = responseroot.getInformation().getValue();

          grpResponse.setHasError(false);
          if (info != null) {
            grpResponse.setAttachmentPath(info.getMessage().getValue());
          }
        }
        grpResponse.setResponse(DMSUtility.convertDMSResponsetoString(response));
      } else {
        grpResponse.setHasError(true);
        grpResponse.setErrorMsg(exceptionmsg);
      }
      grpResponse.setRequest(DMSUtility.convertDMSAddAttachmentRequesttoString(req));
    } catch (Exception e) {
      log.error("Error while createAddAttachmentResponse" + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    log.debug("end of the createAddAttachmentResponse block");
    return grpResponse;
  }

  /*
   * This method is used to update the status of old integration history once the delete record is
   * success
   * 
   * @param invoice
   * 
   * @param logId
   */

  public static void updateStatusinIntegrationLog(Invoice invoice, String logId) {
    try {
      OBContext.setAdminMode();

      List<DMSIntegrationLog> logList = invoice.getEutDmsintegrationLogList().stream()
          .filter(a -> !a.getId().equals(logId)).collect(Collectors.toList());

      for (DMSIntegrationLog logs : logList) {
        logs.setAlertStatus(DMSConstants.DMS_DELETED);
        OBDal.getInstance().save(logs);
      }

      OBDal.getInstance().flush();

    } catch (Exception e) {
      log.debug("Erroe while updating the status" + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
