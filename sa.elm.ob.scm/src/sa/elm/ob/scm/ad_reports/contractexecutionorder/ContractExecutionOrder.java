package sa.elm.ob.scm.ad_reports.contractexecutionorder;

public class ContractExecutionOrder {

  private String sequenceNo = "";
  private String subject = "";
  private String Refid = "";

  public String getSequenceNo() {
    return sequenceNo;
  }

  public void setSequenceNo(String id) {
    sequenceNo = id;
  }

  public String getId() {
    return Refid;
  }

  public void setId(String id) {
    Refid = id;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String name) {
    subject = name;
  }

}
