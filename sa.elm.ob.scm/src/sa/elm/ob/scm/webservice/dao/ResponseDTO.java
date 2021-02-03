package sa.elm.ob.scm.webservice.dao;

public class ResponseDTO {

  private String responseNo;
  private String status;
  private String poReceiptNo;
  private String rdvNo;
  private String rdvTrnNo;
  private String errorMsg;

  public String getResponseNo() {
    return responseNo;
  }

  public void setResponseNo(String responseNo) {
    this.responseNo = responseNo;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getPoReceiptNo() {
    return poReceiptNo;
  }

  public void setPoReceiptNo(String poReceiptNo) {
    this.poReceiptNo = poReceiptNo;
  }

  public String getRdvNo() {
    return rdvNo;
  }

  public void setRdvNo(String rdvNo) {
    this.rdvNo = rdvNo;
  }

  public String getRdvTrnNo() {
    return rdvTrnNo;
  }

  public void setRdvTrnNo(String rdvTrnNo) {
    this.rdvTrnNo = rdvTrnNo;
  }

  public String getErrorMsg() {
    return errorMsg;
  }

  public void setErrorMsg(String errorMsg) {
    this.errorMsg = errorMsg;
  }

  @Override
  public String toString() {
    return "PoReceiptNo = " + poReceiptNo + "; RDVNo =" + rdvNo + "; RDVTxnNo = " + rdvTrnNo;
  }

}
