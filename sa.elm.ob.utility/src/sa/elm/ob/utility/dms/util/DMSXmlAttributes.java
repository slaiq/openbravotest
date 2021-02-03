package sa.elm.ob.utility.dms.util;

public class DMSXmlAttributes {

  public String getDocumentType() {
    return documentType;
  }

  public void setDocumentType(String documentType) {
    this.documentType = documentType;
  }

  public String getRecordId() {
    return recordId;
  }

  public void setRecordId(String recordId) {
    this.recordId = recordId;
  }

  public String getDocumentNo() {
    return documentNo;
  }

  public void setDocumentNo(String documentNo) {
    this.documentNo = documentNo;
  }

  public String getProcessId() {
    return processId;
  }

  public void setProcessId(String processId) {
    this.processId = processId;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public DMSXmlAttributes(String documentType, String recordId, String documentNo, String processId,
      String createdBy) {
    super();
    this.documentType = documentType;
    this.recordId = recordId;
    this.documentNo = documentNo;
    this.processId = processId;
    this.createdBy = createdBy;
  }

  public String documentType;
  public String recordId;
  public String documentNo;
  public String processId;
  public String createdBy;

}
