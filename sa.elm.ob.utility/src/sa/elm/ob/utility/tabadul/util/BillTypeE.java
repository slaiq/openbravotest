package sa.elm.ob.utility.tabadul.util;

/**
 * Enumeration for Bill Types 
 * @author mrahim
 *
 */
public enum BillTypeE {
	SADAD ("Sadad") , CHEQUE ("Cheque");
	
	private String billType;
	
	
	private BillTypeE(String billType) {
		this.billType = billType;
	}


	public String getBillType() {
		return billType;
	}


	public void setBillType(String billType) {
		this.billType = billType;
	}


}
