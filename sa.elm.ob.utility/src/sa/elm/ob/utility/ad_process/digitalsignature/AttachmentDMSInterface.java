package sa.elm.ob.utility.ad_process.digitalsignature;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Base64;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.erpCommon.businessUtility.TabAttachments;
import org.openbravo.model.ad.utility.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This interface is used to add the attachment in DMS Server
 * 
 * @author Sathishkumar.P
 *
 */
public abstract class AttachmentDMSInterface {

  private static final Logger log = LoggerFactory.getLogger(AttachmentDMSInterface.class);

  abstract void addAttachment(String tabId, String recordId, String... attachmentId);

  /**
   * This method is used to get the attachment file as base64
   * 
   * @param attachment
   * @return
   */

  public static String getBase64String(Attachment attachment) {
    String attachmentFolder = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("attach.path");
    String fileDir = TabAttachments.getAttachmentDirectory(attachment.getTable().getId(),
        attachment.getRecord(), attachment.getName());
    String fileDirPath = attachmentFolder + "/" + fileDir;
    final File file = new File(fileDirPath, attachment.getName());
    if (file.exists()) {
      try {
        String responseData;
        String extension = FilenameUtils.getExtension(file.getName());
        if ("pdf".equals(extension)) {
          try (InputStream in = new FileInputStream(file)) {
            byte[] bytes = IOUtils.toByteArray(in);
            responseData = Base64.getEncoder().encodeToString(bytes);
          }
          return responseData;
        } else {
          return null;
        }
      } catch (Exception e) {
        throw new OBException("//Error while removing file", e);
      }

    } else {
      log.warn("No file was removed as file could not be found");
    }
    return null;
  }

}
