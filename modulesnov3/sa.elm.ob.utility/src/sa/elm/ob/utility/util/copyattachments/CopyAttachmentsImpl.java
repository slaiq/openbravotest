package sa.elm.ob.utility.util.copyattachments;

import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyAttachmentsImpl implements CopyAttachmentsService {

  private static final Logger log = LoggerFactory.getLogger(CopyAttachmentsImpl.class);

  @Override
  public String getCopyAttachments(String oldRecordId, String newRecordId, String tableId,
      long newVersionNo) {

    try {

      OBContext.setAdminMode();
      String attachmentFolderPath = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty("attach.path");

      String newPath = CopyAttachmentDAO.splitPath(newRecordId, tableId);

      Long seqNo = new Long("10");
      Connection conn = OBDal.getInstance().getConnection();

      File sourceFile, destFile;

      ArrayList<Attachment> fileList = CopyAttachmentDAO.getFileList(oldRecordId, tableId);

      // create Directory for new attachments
      final File newDirectory = new File(attachmentFolderPath + "/" + newPath);
      newDirectory.mkdirs();

      for (Attachment attach : fileList) {
        String oldPath = null;
        if (attach.getEscmAttr() != null) {
          Attachment file = OBDal.getInstance().get(Attachment.class, attach.getEscmAttr());
          oldPath = CopyAttachmentDAO.splitPath(file.getRecord(), file.getTable().getId());
        } else {
          oldPath = CopyAttachmentDAO.splitPath(oldRecordId, tableId);
        }

        sourceFile = new File(attachmentFolderPath + "/" + oldPath, attach.getName());
        destFile = new File(attachmentFolderPath + "/" + newPath, attach.getName());
        FileUtils.copyFile(sourceFile, destFile);
        CopyAttachmentDAO.insertAttachmentReference(conn, attach, newPath, attach.getName(),
            newVersionNo, newRecordId, seqNo, tableId, oldRecordId);

        // CopyAttachmentDAO.insertAttachmentReference(attach, newPath, attach.getName(),
        // newVersionNo,
        // newRecordId, seqNo, tableId, oldRecordId);

        seqNo = seqNo + 10;

      }

      OBDal.getInstance().flush();

    } catch (Exception e) {
      OBContext.restorePreviousMode();
      log.debug("Exception while copying attachments", e);
    }

    return null;
  }

}
