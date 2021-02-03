package sa.elm.ob.scm.ad_process.printreport;

public class PrintReportVO {
  private String seqNo;
  private String awardLookUp;
  private String awardLookUpLnId;
  private String value;
  private String contractLookUp;
  private int page;
  private int totalPages;
  private int count;
  private String memberName;
  private String memberSignature;
  private String Directlookup;

  public String getDirectlookup() {
    return Directlookup;
  }

  public void setDirectlookup(String directlookup) {
    this.Directlookup = directlookup;
  }

  public String getSeqNo() {
    return seqNo;
  }

  public void setSeqNo(String seqNo) {
    this.seqNo = seqNo;
  }

  public String getAwardLookUp() {
    return awardLookUp;
  }

  public void setAwardLookUp(String awardLookUp) {
    this.awardLookUp = awardLookUp;
  }

  public String getContractLookUp() {
    return contractLookUp;
  }

  public void setContractLookUp(String contractLookUp) {
    this.contractLookUp = contractLookUp;
  }

  public int getPage() {
    return page;
  }

  public void setPage(int page) {
    this.page = page;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public int getTotalPages() {
    return totalPages;
  }

  public void setTotalPages(int totalPages) {
    this.totalPages = totalPages;
  }

  public String getAwardLookUpLnId() {
    return awardLookUpLnId;
  }

  public void setAwardLookUpLnId(String awardLookUpLnId) {
    this.awardLookUpLnId = awardLookUpLnId;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getMemberName() {
    return memberName;
  }

  public void setMemberName(String memberName) {
    this.memberName = memberName;
  }

  public String getMemberSignature() {
    return memberSignature;
  }

  public void setMemberSignature(String memberSignature) {
    this.memberSignature = memberSignature;
  }

}
