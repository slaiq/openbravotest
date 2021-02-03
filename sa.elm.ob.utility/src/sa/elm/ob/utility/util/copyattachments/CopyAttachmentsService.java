package sa.elm.ob.utility.util.copyattachments;

public interface CopyAttachmentsService {

  /**
   * This method is used to get the copy the attachments in newer version
   * 
   * @param oldRecordId
   * @param tableId
   * @param newRecordId
   * @return
   */
  public String getCopyAttachments(String oldRecordId, String newRecordId, String tableId,
      long newVersionNo);

}
