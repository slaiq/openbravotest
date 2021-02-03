package sa.elm.ob.utility.tabadul;
/**
 * 
 * @author mrahim
 *
 */
public enum TenderStatusE {

	DRAFT ("DR"), UPLOADED ("UP") , PUBLISHED ("PUB") , CANCELLED ("CAN"), DELETED ("DT") , FAILED ("FAILED"); 
	
	private String status;
	
	private TenderStatusE(String status) {
		this.status = status;
	}

	public String getStatus() {
		return status;
	}


	public void setStatus(String status) {
		this.status = status;
	}

	
}
