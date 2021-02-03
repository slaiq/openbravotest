package sa.elm.ob.scm.webservice.pki.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;

import org.apache.commons.io.IOUtils;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.model.ad.utility.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PkiFileUtility {

  private final Logger log4j = LoggerFactory.getLogger(PkiFileUtility.class);

  public void saveFileDataAfterDecoding(Attachment attachment, String data, String fileName)
      throws FileNotFoundException, Exception {

    log4j.info("saveFileDataAfterDecoding -----------> start");
    try {
      String attachmentFolder = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty("attach.path");

      // directory path
      String fileDirPath = new StringBuilder().append(attachmentFolder).append("/")
          .append(attachment.getPath()).toString();

      // path with file name
      String orginalFilePathWithName = new StringBuilder().append(attachmentFolder).append("/")
          .append(attachment.getPath()).append("/").append(fileName).toString();

      // decode file data from encoded data
      byte[] bytes = Base64.getDecoder().decode(data);

      // back-up file if exist ----- start
      // try {
      // String backupFileName = fileDirPath + "/" +
      // System.currentTimeMillis() + fileName;
      // if (new File(orginalFilePathWithName).renameTo(new
      // File(backupFileName))) {
      // System.out.println("File renamed");
      // }
      // } catch (Exception e) {
      // log4j.error(" Rename file Exception : " + e.toString());
      // }
      // back-up file if exist ----- end

      // write file in path
      try (OutputStream out = new FileOutputStream(orginalFilePathWithName)) {
        out.write(bytes);
      }
    } catch (Exception exception) {
      log4j.error("saveFileDataAfterDecoding Exception : " + exception.toString());
      throw new Exception(exception);
    }
    log4j.info("saveFileDataAfterDecoding -----------> end");
  }

  public String getFileEncodedBytes(Attachment attachment) throws FileNotFoundException, Exception {
    log4j.info("getFileEncodedBytes -----------> start");

    String responseData = "";
    String attachmentFolder = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("attach.path");

    String fileDirPath = attachmentFolder + "/" + attachment.getPath() + "/" + attachment.getName();

    // get file encoded bytes
    responseData = getEncodeBytesData(fileDirPath);
    log4j.info("getFileEncodedBytes -----------> end");
    return responseData;
  }

  public String getUserEncodedBytes(String imageId) throws FileNotFoundException, Exception {
    log4j.info("getUserEncodedBytes -----------> start");
    String encodedSignature = null;
    try {
      encodedSignature = Base64.getEncoder()
          .encodeToString(org.openbravo.erpCommon.utility.Utility.getImage(imageId));
    } catch (Exception exception) {
      log4j.error("Erroe while getting user signature :" + exception.getMessage());
    }
    return encodedSignature;
  }

  public String getLinceseEncodedByte() throws FileNotFoundException, Exception {
    log4j.info("getLinceseEncodedByte -----------> start");

    String responseData = "";
    String attachmentFolder = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("license.path");

    String fileDirPath = attachmentFolder + "/"
        + OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("license.name");

    // get file encoded bytes
    responseData = getEncodeBytesData(fileDirPath);
    log4j.info("getLinceseEncodedByte -----------> end");
    return responseData;
  }

  private String getEncodeBytesData(String fileDirPath) throws IOException, FileNotFoundException {
    String responseData;
    try (InputStream in = new FileInputStream(new File(fileDirPath))) {
      byte[] bytes = IOUtils.toByteArray(in);
      responseData = Base64.getEncoder().encodeToString(bytes);
    }
    return responseData;
  }
}
