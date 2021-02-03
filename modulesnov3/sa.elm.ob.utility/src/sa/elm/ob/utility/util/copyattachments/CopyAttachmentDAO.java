package sa.elm.ob.utility.util.copyattachments;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;

import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.utility.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyAttachmentDAO {

  private static final Logger log = LoggerFactory.getLogger(CopyAttachmentDAO.class);

  /**
   * This method is used to get the list of file path name attached in transactions
   * 
   * @param recordId
   * @param tableId
   * @return
   */

  public static ArrayList<Attachment> getFileList(String recordId, String tableId) {

    ArrayList<Attachment> fileNameList = new ArrayList<>();

    try {

      OBQuery<Attachment> obQry = OBDal.getInstance().createQuery(Attachment.class,
          "as e where e.table.id=:tableId and e.record=:recordId order by e.creationDate desc");

      obQry.setNamedParameter("tableId", tableId);
      obQry.setNamedParameter("recordId", recordId);
      obQry.setFilterOnReadableClients(false);
      obQry.setFilterOnReadableOrganization(false);

      for (Attachment files : obQry.list()) {
        fileNameList.add(files);
      }
    } catch (Exception e) {
      log.debug("Exception while getting the attachments", e.getMessage());
    }

    return fileNameList;

  }

  /**
   * Splits the path name component so that the resulting path name is 3 characters long sub
   * directories. For example 12345 is splitted to 123/45
   * 
   * @param origname
   *          Original name
   * @return splitted name.
   */
  public static String splitPath(String origname, String tableId) {
    String newname = "";
    for (int i = 0; i < origname.length(); i += 3) {
      if (i != 0) {
        newname += "/";
      }
      newname += origname.substring(i, Math.min(i + 3, origname.length()));
    }
    return tableId + "/" + newname;
  }

  /**
   * This method is used to insert attachment reference in c_file for new version
   * 
   * @param newPath
   * @param fileName
   * @param version
   */

  public static void insertAttachmentReference(Connection conn, Attachment attach, String newPath,
      String fileName, long version, String recordId, Long seqNo, String tableId,
      String oldRecordId) {
    String insertQry = null;
    PreparedStatement insertps = null;
    final String cFileId = SequenceIdData.getUUID();
    int updateCount = 0;

    try {

      insertQry = "INSERT INTO C_FILE (C_FILE_ID, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE, CREATED, CREATEDBY, "
          + "        UPDATED, UPDATEDBY, AD_TABLE_ID, AD_RECORD_ID, NAME, SEQNO, PATH,EM_EUT_SOURCEID,EM_EUT_VERSION_NO,TEXT)"
          + "        VALUES(?, ?, ?, 'Y', now(), ?, "
          + "        now(), ?, ?, ?, ?, (SELECT COALESCE(MAX(SEQNO), 0) + 10 FROM C_FILE WHERE AD_TABLE_ID = '"
          + tableId + "' AND AD_RECORD_ID='" + recordId + "'),?,?,?,?)";
      insertps = conn.prepareStatement(insertQry);
      insertps.setString(1, cFileId);
      insertps.setString(2, attach.getClient().getId());
      insertps.setString(3, attach.getOrganization().getId());
      insertps.setString(4, attach.getCreatedBy().getId());
      insertps.setString(5, attach.getUpdatedBy().getId());
      insertps.setString(6, tableId);
      insertps.setString(7, recordId);
      insertps.setString(8, fileName);
      insertps.setString(9, newPath);
      insertps.setString(10, oldRecordId);
      insertps.setLong(11, version);
      insertps.setString(12, attach.getText());
      updateCount = insertps.executeUpdate();

    } catch (Exception e) {
      log.debug("Exception while getting the inserting attachment reference", e.getMessage());
    }

  }

}
