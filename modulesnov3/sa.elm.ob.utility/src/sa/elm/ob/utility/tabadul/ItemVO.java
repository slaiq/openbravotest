package sa.elm.ob.utility.tabadul;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ItemVO implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7658341903690347506L;
	
	private Integer vendorId;
	private String purchaseStatus;
	private Double purchaseDate;
	private String sadadBillNo;
	private String attachmentStatus;
	private Integer withdrawnDate;
	private String offerStatus;
	private String techEvalStatus;
	private String techEvalComments;
	private String purchasesId;
	private Double awardingAmount;
	private String awardingType;
	private String offerNumber;
	private Double offerAmount;
	private List <OfferDetailVO> offerDetails;
	private List <GuaranteeVO> guarantees;
	private String invitationDate;
	
	@JsonProperty ("uid")
	public Integer getVendorId() {
		return vendorId;
	}
	public void setVendorId(Integer vendorId) {
		this.vendorId = vendorId;
	}
	@JsonProperty ("purchase_status")
	public String getPurchaseStatus() {
		return purchaseStatus;
	}
	public void setPurchaseStatus(String purchaseStatus) {
		this.purchaseStatus = purchaseStatus;
	}
	@JsonProperty ("purchase_date")
	public Double getPurchaseDate() {
		return purchaseDate;
	}
	public void setPurchaseDate(Double purchaseDate) {
		this.purchaseDate = purchaseDate;
	}
	@JsonProperty ("sadad_bill_no")
	public String getSadadBillNo() {
		return sadadBillNo;
	}
	public void setSadadBillNo(String sadadBillNo) {
		this.sadadBillNo = sadadBillNo;
	}
	@JsonProperty ("attachment_status")
	public String getAttachmentStatus() {
		return attachmentStatus;
	}
	public void setAttachmentStatus(String attachmentStatus) {
		this.attachmentStatus = attachmentStatus;
	}
	@JsonProperty ("withdrawn_date")
	public Integer getWithdrawnDate() {
		return withdrawnDate;
	}
	public void setWithdrawnDate(Integer withdrawnDate) {
		this.withdrawnDate = withdrawnDate;
	}
	@JsonProperty ("offer_status")
	public String getOfferStatus() {
		return offerStatus;
	}
	public void setOfferStatus(String offerStatus) {
		this.offerStatus = offerStatus;
	}
	@JsonProperty ("techeval_status")
	public String getTechEvalStatus() {
		return techEvalStatus;
	}
	public void setTechEvalStatus(String techEvalStatus) {
		this.techEvalStatus = techEvalStatus;
	}
	@JsonProperty ("techeval_comments")
	public String getTechEvalComments() {
		return techEvalComments;
	}
	public void setTechEvalComments(String techEvalComments) {
		this.techEvalComments = techEvalComments;
	}
	@JsonProperty ("utpid")
	public String getPurchasesId() {
		return purchasesId;
	}
	public void setPurchasesId(String purchasesId) {
		this.purchasesId = purchasesId;
	}
	@JsonProperty ("awarding_amount")
	public Double getAwardingAmount() {
		return awardingAmount;
	}
	public void setAwardingAmount(Double awardingAmount) {
		this.awardingAmount = awardingAmount;
	}
	@JsonProperty ("awarding_type")
	public String getAwardingType() {
		return awardingType;
	}
	public void setAwardingType(String awardingType) {
		this.awardingType = awardingType;
	}
	@JsonProperty ("offer_num")
	public String getOfferNumber() {
		return offerNumber;
	}
	public void setOfferNumber(String offerNumber) {
		this.offerNumber = offerNumber;
	}
	@JsonProperty ("offer_amount")
	public Double getOfferAmount() {
		return offerAmount;
	}
	public void setOfferAmount(Double offerAmount) {
		this.offerAmount = offerAmount;
	}
	@JsonProperty ("offer_details")
	public List<OfferDetailVO> getOfferDetails() {
		return offerDetails;
	}
	public void setOfferDetails(List<OfferDetailVO> offerDetails) {
		this.offerDetails = offerDetails;
	}
	@JsonProperty ("guarantees")
	public List<GuaranteeVO> getGuarantees() {
		return guarantees;
	}
	public void setGuarantees(List<GuaranteeVO> guarantees) {
		this.guarantees = guarantees;
	}
	@JsonProperty ("invitation_date")
	public String getInvitationDate() {
		return invitationDate;
	}
	public void setInvitationDate(String invitationDate) {
		this.invitationDate = invitationDate;
	}

	
	

}
