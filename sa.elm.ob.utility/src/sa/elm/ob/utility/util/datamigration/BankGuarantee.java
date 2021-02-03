package sa.elm.ob.utility.util.datamigration;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BankGuarantee {
	
	private String bankName;
	private String bankCode;
	public String getBankCode() {
		return bankCode;
	}
	public void setBankCode(String bankCode) {
		this.bankCode = bankCode;
	}
	private String letterRefrenceNum;
	private BigDecimal amount = new BigDecimal(0);
	private LocalDate startDate;
	private LocalDate endDate;
	private String internalNo;
	private String guaGlitterBookNo;
	private int linNo;
	
	public String getBankName() {
		return bankName;
	}
	public void setBankName(String bankName) {
		this.bankName = bankName;
	}
	public String getLetterRefrenceNum() {
		return letterRefrenceNum;
	}
	public void setLetterRefrenceNum(String letterRefrenceNum) {
		this.letterRefrenceNum = letterRefrenceNum;
	}
	public BigDecimal getAmount() {
		return amount;
	}
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	public LocalDate getStartDate() {
		return startDate;
	}
	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}
	public LocalDate getEndDate() {
		return endDate;
	}
	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}
	public String getInternalNo() {
		return internalNo;
	}
	public void setInternalNo(String internalNo) {
		this.internalNo = internalNo;
	}
	public String getGuaGlitterBookNo() {
		return guaGlitterBookNo;
	}
	public void setGuaGlitterBookNo(String guaGlitterBookNo) {
		this.guaGlitterBookNo = guaGlitterBookNo;
	}
	public int getLinNo() {
		return linNo;
	}
	public void setLinNo(int linNo) {
		this.linNo = linNo;
	}
}
