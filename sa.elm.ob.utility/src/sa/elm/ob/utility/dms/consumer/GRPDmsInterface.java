package sa.elm.ob.utility.dms.consumer;

import org.openbravo.model.common.invoice.Invoice;

import sa.elm.ob.utility.dms.consumer.dto.AddAttachmentResponseGRP;
import sa.elm.ob.utility.dms.consumer.dto.CreateAttachmentResponseGRP;
import sa.elm.ob.utility.dms.consumer.dto.DeleteAttachmentResponseGRP;
import sa.elm.ob.utility.dms.consumer.dto.DeleteRecordGRPResponse;
import sa.elm.ob.utility.dms.consumer.dto.GetAttachmentGRPResponse;
import sa.elm.ob.utility.dms.util.DMSXmlAttributes;

public interface GRPDmsInterface {

  /**
   * This method is used to sent document to DMS
   * 
   * @param profileURI
   * @param attachmentBase64
   * @param nodeID
   * @param documentName
   * @param description
   * @return {@link CreateAttachmentResponseGRP}
   */
  CreateAttachmentResponseGRP sendReportToDMS(String profileURI, String attachmentBase64,
      String nodeID, String documentName, String description, DMSXmlAttributes attributed);

  /**
   * This method is used to get attachment from the DMS
   * 
   * @param profileURI
   * @return {@link GetAttachmentGRPResponse}
   */

  GetAttachmentGRPResponse getReportFromDMS(String profileURI);

  /**
   * This method is used to delete the record in DMS
   * 
   * @param profileURI
   * @return {@link DeleteRecordGRPResponse}
   */
  DeleteRecordGRPResponse deleteRecordinDMS(Invoice invoice);

  /**
   * This method is used to add attachment in the existing record in DMS
   * 
   * @param profileURI
   * @return {@link AddAttachmentResponseGRP}
   */
  AddAttachmentResponseGRP addAttachmentinDMS(String profileURI, String attachmentBase64,
      String documentName, String description);

  /**
   * This method is used to delete the attachment in the dms
   * 
   * @param profileURI
   * @param attachmentPath
   * @return
   */

  DeleteAttachmentResponseGRP deleteAttachmentinDMS(String profileURI, String attachmentPath);

}
