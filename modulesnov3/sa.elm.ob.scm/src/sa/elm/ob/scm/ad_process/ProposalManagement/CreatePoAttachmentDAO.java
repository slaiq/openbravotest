package sa.elm.ob.scm.ad_process.ProposalManagement;

import java.io.File;
import java.sql.Connection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.utility.Attachment;

import sa.elm.ob.utility.util.copyattachments.CopyAttachmentDAO;

public class CreatePoAttachmentDAO {

  private static Logger log = Logger.getLogger(CreatePoAttachmentDAO.class);

  /**
   * This method is used to create temporary attachment for po created using create po and submit
   * button
   * 
   * @param path
   * @param proposalId
   * @param fileName
   */
  public static void insertAttachment(String path, String proposalId, String fileName) {
    try {
      OBContext.setAdminMode();
      Attachment attachment = OBProvider.getInstance().get(Attachment.class);
      attachment.setPath(path);
      attachment.setName(fileName);
      attachment.setEutSourceid(proposalId);
      attachment.setTable(OBDal.getInstance().get(Table.class, "259"));
      attachment.setRecord(proposalId);
      attachment.setSequenceNumber(Long.valueOf("10"));
      OBDal.getInstance().save(attachment);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log.error("Error while adding attachment" + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /**
   * This method is used to delete the attachment
   * 
   * @param proposalId
   */
  public static void deleteAttachment(String proposalId) {

    try {
      OBContext.setAdminMode();

      String attachmentFolder = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty("attach.path");

      OBQuery<Attachment> attach = OBDal.getInstance().createQuery(Attachment.class,
          "as e where e.eutSourceid=:proposalId and e.table.id ='259' ");
      attach.setNamedParameter("proposalId", proposalId);
      List<Attachment> attachList = attach.list();
      for (Attachment attachment : attachList) {
        File file = new File(attachmentFolder + "/createpoattachment/" + attachment.getPath() + "/"
            + attachment.getName());
        file.delete();
        OBDal.getInstance().remove(attachment);
      }
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log.error("Error while delete attachment" + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  public static void copyAttachment(String recordId, String newRecordId, String tableId)
      throws Exception {

    try {
      OBContext.setAdminMode();
      String attachmentFolderPath = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty("attach.path");

      String newPath = CopyAttachmentDAO.splitPath(newRecordId, tableId);

      Long seqNo = new Long("10");
      Connection conn = OBDal.getInstance().getConnection();

      File sourceFile, destFile;

      OBQuery<Attachment> attachQry = OBDal.getInstance().createQuery(Attachment.class,
          "as e where e.eutSourceid=:proposalId and e.table.id ='259' ");
      attachQry.setNamedParameter("proposalId", recordId);
      List<Attachment> fileList = attachQry.list();

      // create Directory for new attachments
      final File newDirectory = new File(attachmentFolderPath + "/" + newPath);
      newDirectory.mkdirs();

      for (Attachment attach : fileList) {
        sourceFile = new File(attachmentFolderPath + "/" + attach.getPath(), attach.getName());
        destFile = new File(attachmentFolderPath + "/" + newPath, attach.getName());
        FileUtils.copyFile(sourceFile, destFile);
        CopyAttachmentDAO.insertAttachmentReference(conn, attach, newPath, attach.getName(), 0,
            newRecordId, seqNo, tableId, null);
        seqNo = seqNo + 10;
      }

      OBDal.getInstance().flush();
    } catch (Exception e) {
      log.error("Error while copying attchment" + e.getMessage());
      throw new Exception(e.getMessage());
    }

  }

}
