package sa.elm.ob.utility.tabadul;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;

/**
 * 
 * @author mrahim
 *
 */

public class TenderBasicInfoVO implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4342346930943920219L;
	// Should be ENUM needs to be changed later on
	private String tenderType;
	private String tenderName;
	private String tenderNumber;
	private String govtTehnicalAgency;
	private Integer showInFront;
	private String description;
	private Integer price;
	private String proposalSubmissionAddress;
	private String openEnvelopesLocation; 

	@XmlElement (name = "type")
	public String getTenderType() {
		if (null != tenderType && tenderType.trim().length() > 0){
			if (TenderTypeE.TENDER.getObType().equals(tenderType)){
				return TenderTypeE.TENDER.getTabadulType() ; 
			}else{
				return TenderTypeE.DIRECT.getTabadulType() ;
			}
		}
		return tenderType;
	}
	public void setTenderType(String tenderType) {
		this.tenderType = tenderType;
	}
	@XmlElement (name = "title")
	public String getTenderName() {
		return tenderName;
	}
	public void setTenderName(String tenderName) {
		this.tenderName = tenderName;
	}
	@XmlElement (name = "ref_no")
	public String getTenderNumber() {
		return tenderNumber;
	}
	public void setTenderNumber(String tenderNumber) {
		this.tenderNumber = tenderNumber;
	}
	@XmlElement (name = "gatid")
	public String getGovtTehnicalAgency() {
		return govtTehnicalAgency;
	}
	public void setGovtTehnicalAgency(String govtTehnicalAgency) {
		this.govtTehnicalAgency = govtTehnicalAgency;
	}
	@XmlElement (name = "show_in_front")
	public Integer getShowInFront() {
		return showInFront;
	}
	public void setShowInFront(Integer showInFront) {
		this.showInFront = showInFront;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Integer getPrice() {
		return price;
	}
	public void setPrice(Integer price) {
		this.price = price;
	}
	@XmlElement (name = "offer_delivery_location")
	public String getProposalSubmissionAddress() {
		return proposalSubmissionAddress;
	}
	public void setProposalSubmissionAddress(String proposalSubmissionAddress) {
		this.proposalSubmissionAddress = proposalSubmissionAddress;
	}
	@XmlElement (name = "open_env_location")
	public String getOpenEnvelopesLocation() {
		return openEnvelopesLocation;
	}
	public void setOpenEnvelopesLocation(String openEnvelopesLocation) {
		this.openEnvelopesLocation = openEnvelopesLocation;
	}

}
