
package sa.elm.ob.scm.ad_process.ProposalManagement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.businessUtility.TabAttachments;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;

/**
 * 
 * @author Sathish kumar.P
 *
 */

public class CreatePoAttachmentUpload extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public static final String tabId = "D6115C9AF1DD4C4C9811D2A69E42878B";

  private static final String TMP_DIR_PATH = "/tmp";
  private static final String DESTINATION_DIR_PATH = "/files";
  private File tmpDir = null, destinationDir = null;

  @SuppressWarnings("rawtypes")
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    try {

      String fileValidationResponse = null;
      List<String> errorMsg = new ArrayList<>();
      Boolean isError = false;
      StringBuilder resultMsg = new StringBuilder("");
      String key = null;
      Boolean isRemoveAttachment = false;

      DiskFileItemFactory fileItemFactory = new DiskFileItemFactory();
      fileItemFactory.setSizeThreshold(100 * 124 * 124); // 1 MB
      fileItemFactory.setRepository(tmpDir);
      boolean isMultipart = ServletFileUpload.isMultipartContent(request);

      if (isMultipart) {
        File attachmentFile = null;
        HashMap<String, String> formValues = new HashMap<String, String>();
        ServletFileUpload upload = new ServletFileUpload(fileItemFactory);
        try {
          List items = upload.parseRequest(request);
          Iterator validationItr = items.iterator();
          Iterator uploadItr = items.iterator();
          final VariablesSecureApp vars = new VariablesSecureApp(request);

          // validate file before uploading
          while (validationItr.hasNext()) {
            FileItem item = (FileItem) validationItr.next();
            if (!item.isFormField()) {
              fileValidationResponse = validateUploadedFile(item, tabId);
              if (!fileValidationResponse.equalsIgnoreCase("SUCCESS")) {
                isError = true;
                if (fileValidationResponse.equalsIgnoreCase("INVALID_FILE_SIZE")) {
                  String message = Utility.messageBD(this, "em_escm_file_max_size",
                      vars.getLanguage());
                  errorMsg.add(message + " - " + item.getName());
                } else if (fileValidationResponse.equalsIgnoreCase("INVALID_FILE_EXT")) {
                  String message = Utility.messageBD(this, "em_escm_invalid_file_ext",
                      vars.getLanguage());
                  errorMsg.add(message + " - " + item.getName());
                } else if (fileValidationResponse.equalsIgnoreCase("INVALID_FILE_EXT_BID")) {
                  String message = Utility.messageBD(this, "ESCM_INVALID_FILE_EXT_BID",
                      vars.getLanguage());
                  errorMsg.add(message + " - " + item.getName());
                }
              }
            } else {
              formValues.put(item.getFieldName(), item.getString());
            }
          }

          key = formValues.get("inpKey");
          isRemoveAttachment = "true".equals(formValues.get("inpRemove")) ? true : false;

          if (isRemoveAttachment) {
            // delete the previous attachment attached
            CreatePoAttachmentDAO.deleteAttachment(key);
          }

          if (isError) {
            for (String str : errorMsg) {
              resultMsg.append(str);
              resultMsg.append(" <br/> ");
            }
            bdErrorGeneralPopUp(request, response, "", resultMsg.toString());
          } else {

            String fileDir = TabAttachments.getAttachmentDirectoryForNewAttachments(tabId, key);

            final File uploadedDir = new File(
                globalParameters.strFTPDirectory + "/createpoattachment/" + fileDir);
            if (!uploadedDir.exists())
              uploadedDir.mkdirs();

            while (uploadItr.hasNext()) {
              FileItem item = (FileItem) uploadItr.next();
              if (!item.isFormField()) {
                attachmentFile = new File(uploadedDir, item.getName());
                item.write(attachmentFile);
                log4j.debug("File Name:" + attachmentFile.getName());
                CreatePoAttachmentDAO.insertAttachment("/createpoattachment/" + fileDir, key,
                    item.getName());
              }
            }

            bdErrorGeneralPopUp(request, response, "",
                OBMessageUtils.messageBD("Escm_attachmentadded_success"));
          }

        } catch (Exception e) {
          log4j.error("Exception while uploading attachment in create posubmit popup" + e);
          throw new ServletException(e);
        }

      }

    } catch (Exception e) {
      log4j.error("Exception in createpoattachment upload : ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private String validateUploadedFile(FileItem file, String strTab) {
    String allowedFileExtentions = "";
    String validationResponse = "SUCCESS";
    final String inpName = "inpname";
    Properties props = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    // file extension for bid window
    if (strTab.equals("31960EC365D746A180594FFB7B403ABB")) {
      allowedFileExtentions = props.getProperty("file.allowed.bid.extensions");
    } else {
      allowedFileExtentions = props.getProperty("file.allowed.extentions");
    }

    final String maxFileUploadSizeInMbs = props.getProperty("file.max.size.mb");
    // Get the file data from path
    final String fileName = file.getName();
    final String fileExtention = FilenameUtils.getExtension(fileName);

    if (validateFileSize(file.getSize(), Integer.parseInt(maxFileUploadSizeInMbs))) {
      validationResponse = "INVALID_FILE_SIZE";
    } else if (!validateFileExtentions(fileExtention, allowedFileExtentions)
        && strTab.equals("31960EC365D746A180594FFB7B403ABB")) {
      validationResponse = "INVALID_FILE_EXT_BID";
    } else if (!validateFileExtentions(fileExtention, allowedFileExtentions)) {
      validationResponse = "INVALID_FILE_EXT";
    }

    return validationResponse;
  }

  /**
   * Checks if the file size is OK as per rules
   * 
   * @param bytes
   * @return
   */
  private boolean validateFileSize(Long fileSizeInBytes, Integer maxAllowedFileSize) {
    boolean isFileBiggerThenLimit = false;

    long fileSizeInKB = fileSizeInBytes / 1024;
    // Convert the KB to MegaBytes (1 MB = 1024 KBytes)
    long fileSizeInMB = fileSizeInKB / 1024;

    if (fileSizeInMB > maxAllowedFileSize) {
      isFileBiggerThenLimit = true;
    }

    return isFileBiggerThenLimit;
  }

  /**
   * Validate the file
   * 
   * @param fileExtention
   * @param allowedFileExtentionPattern
   * @return
   */
  private boolean validateFileExtentions(String fileExtention, String allowedFileExtentionPattern) {
    String[] allowedFileExtentionsArray = allowedFileExtentionPattern.split(",");
    for (int i = 0; i < allowedFileExtentionsArray.length; i++) {
      if (fileExtention.trim().equalsIgnoreCase(allowedFileExtentionsArray[i])) {
        return true;
      }
    }

    return false;
  }

}