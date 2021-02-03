package sa.elm.ob.utility.tabadul;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * VO for Tender
 * @author mrahim
 *
 */
@XmlRootElement(name = "root")
public class TenderVO implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 9038486623901862350L;
	private String bidId;
	private Integer tenderInternalId;
	private TenderBasicInfoVO basicInfo;
	private TenderDatesVO tenderDates;
	private TaxonomyAndLocationVO taxonomyAndLocationVO;
	private TenderCategoriesVO tenderCategoriesVO;
	private TenderFilesVO tenderFiles;
	private String status;
	


	public TenderVO () {
		basicInfo = new TenderBasicInfoVO();
		tenderDates = new TenderDatesVO();
		taxonomyAndLocationVO = new TaxonomyAndLocationVO();
		tenderFiles = new TenderFilesVO();
		tenderCategoriesVO = new TenderCategoriesVO();
	}

	@XmlElement (name = "tid")
	public Integer getTenderInternalId() {
		return tenderInternalId;
	}

	public void setTenderInternalId(Integer tenderInternalId) {
		this.tenderInternalId = tenderInternalId;
	}
	@XmlElement (name = "step1")
	public TenderBasicInfoVO getBasicInfo() {
		return basicInfo;
	}

	public void setBasicInfo(TenderBasicInfoVO basicInfo) {
		this.basicInfo = basicInfo;
	}
	@XmlElement (name = "step2")
	public TenderDatesVO getTenderDates() {
		return tenderDates;
	}
	public void setTenderDates(TenderDatesVO tenderDates) {
		this.tenderDates = tenderDates;
	}
	@XmlElement (name = "step3")
	public TaxonomyAndLocationVO getTaxonomyAndLocationVO() {
		return taxonomyAndLocationVO;
	}

	public void setTaxonomyAndLocationVO(TaxonomyAndLocationVO taxonomyAndLocationVO) {
		this.taxonomyAndLocationVO = taxonomyAndLocationVO;
	}
	@XmlElement (name = "step4")
	public TenderCategoriesVO getTenderCategoriesVO() {
		return tenderCategoriesVO;
	}

	public void setTenderCategoriesVO(TenderCategoriesVO tenderCategoriesVO) {
		this.tenderCategoriesVO = tenderCategoriesVO;
	}
	@XmlElement (name = "step5")
	public TenderFilesVO getTenderFiles() {
		return tenderFiles;
	}

	public void setTenderFiles(TenderFilesVO tenderFiles) {
		this.tenderFiles = tenderFiles;
	}

	@XmlTransient
	public String getBidId() {
		return bidId;
	}

	public void setBidId(String bidId) {
		this.bidId = bidId;
	}
	@XmlTransient
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
