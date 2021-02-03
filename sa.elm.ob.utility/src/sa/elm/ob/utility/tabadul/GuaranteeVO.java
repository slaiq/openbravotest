package sa.elm.ob.utility.tabadul;

import java.io.Serializable;

/**
 * 
 * @author mrahim
 *
 */
public class GuaranteeVO implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3398194785059715096L;

	private Integer id;
	private Boolean documentGuaranteeLetterActive;
	private String bankName;
	private String number;
	private String amount;
	private Integer period;
	private Integer startDateJulian;
	private Integer expiredateJulian;
	private String startHijri;
	private String expireHijri;
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public Boolean getDocumentGuaranteeLetterActive() {
		return documentGuaranteeLetterActive;
	}
	public void setDocumentGuaranteeLetterActive(Boolean documentGuaranteeLetterActive) {
		this.documentGuaranteeLetterActive = documentGuaranteeLetterActive;
	}
	public String getBankName() {
		return bankName;
	}
	public void setBankName(String bankName) {
		this.bankName = bankName;
	}
	public String getNumber() {
		return number;
	}
	public void setNumber(String number) {
		this.number = number;
	}
	public String getAmount() {
		return amount;
	}
	public void setAmount(String amount) {
		this.amount = amount;
	}
	public Integer getPeriod() {
		return period;
	}
	public void setPeriod(Integer period) {
		this.period = period;
	}
	public Integer getStartDateJulian() {
		return startDateJulian;
	}
	public void setStartDateJulian(Integer startDateJulian) {
		this.startDateJulian = startDateJulian;
	}
	public Integer getExpiredateJulian() {
		return expiredateJulian;
	}
	public void setExpiredateJulian(Integer expiredateJulian) {
		this.expiredateJulian = expiredateJulian;
	}
	public String getStartHijri() {
		return startHijri;
	}
	public void setStartHijri(String startHijri) {
		this.startHijri = startHijri;
	}
	public String getExpireHijri() {
		return expireHijri;
	}
	public void setExpireHijri(String expireHijri) {
		this.expireHijri = expireHijri;
	}
	
}
