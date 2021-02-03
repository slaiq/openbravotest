package sa.elm.ob.finance.ad_process.paymentoutmof;

public class UploadMOFVO {	
	private String message;
	private int result;
	private String docNo;
	private String colName;
	private int uploadMoF;
	private String accType;
		
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public int getResult() {
		return result;
	}

	public void setResult(int result) {
		this.result = result;
	}

	public String getDocNo() {
		return docNo;
	}

	public void setDocNo(String docNo) {
		this.docNo = docNo;
	}

	public String getColName() {
		return colName;
	}

	public void setColName(String colName) {
		this.colName = colName;
	}

	public int getUploadMoF() {
		return uploadMoF;
	}

	public void setUploadMoF(int uploadMoF) {
		this.uploadMoF = uploadMoF;
	}

	public String getAccType() {
		return accType;
	}

	public void setAccType(String accType) {
		this.accType = accType;
	}
}
