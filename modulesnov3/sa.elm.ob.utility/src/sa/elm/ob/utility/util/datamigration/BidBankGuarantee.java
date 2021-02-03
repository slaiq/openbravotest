package sa.elm.ob.utility.util.datamigration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BidBankGuarantee {
	
	private String financialYear;
	private String bidNo;
	private String commercialNumber;
	private String supplierName;
	private String bgType;
	private String internalNumber;
	
	public String getInternalNumber() {
		return internalNumber;
	}
	public void setInternalNumber(String internalNumber) {
		this.internalNumber = internalNumber;
	}
	public String getBgType() {
		return bgType;
	}
	public void setBgType(String bgType) {
		this.bgType = bgType;
	}
	public String getSupplierName() {
		return supplierName;
	}
	public void setSupplierName(String supplierName) {
		this.supplierName = supplierName;
	}
	public String getCommercialNumber() {
		return commercialNumber;
	}
	public void setCommercialNumber(String commercialNumber) {
		this.commercialNumber = commercialNumber;
	}
	private HashMap<String,SupplierBankGuarantee> suppliers = new HashMap<String,SupplierBankGuarantee>();
	private List<Integer> lines = new ArrayList<Integer>();
	public String getBidNo() {
		return bidNo;
	}
	public void setBidNo(String bidNo) {
		this.bidNo = bidNo;
	}
	public String getFinancialYear() {
		return financialYear;
	}
	public void setFinancialYear(String financialYear) {
		this.financialYear = financialYear.substring(0, 4) + "/" + financialYear.substring(4);
	}
	public HashMap<String,SupplierBankGuarantee> getSuppliers() {
		return suppliers;
	}
	public void setSuppliers(HashMap<String,SupplierBankGuarantee> suppliers) {
		this.suppliers = suppliers;
	}
	public List<Integer> getLines() {
		return lines;
	}
	public void setLines(List<Integer> lines) {
		this.lines = lines;
	}

}
