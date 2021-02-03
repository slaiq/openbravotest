package sa.elm.ob.utility.util.datamigration;

import java.util.ArrayList;

public class SupplierBankGuarantee {
	
	private String supplierId;
	private String supplierName;
	private ArrayList<BankGuarantee> banks = new ArrayList<BankGuarantee>();
	
	public String getSupplierId() {
		return supplierId;
	}
	public void setSupplierId(String supplierId) {
		this.supplierId = supplierId;
	}
	public ArrayList<BankGuarantee> getBanks() {
		return banks;
	}
	public void setBanks(ArrayList<BankGuarantee> banks) {
		this.banks = banks;
	}
	public String getSupplierName() {
		return supplierName;
	}
	public void setSupplierName(String supplierName) {
		this.supplierName = supplierName;
	}

}
