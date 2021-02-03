package sa.elm.ob.utility.tabadul;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
/**
 * 
 * @author mrahim
 *
 */
public class TenderFilesVO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4306648098332920666L;
	private Boolean manualDelivery;
	private Integer downloadableDelivery;
	private String deliveryLocation;
	
	@XmlElement (name = "tender_files_delivery_method_manual")
	public Boolean getManualDelivery() {
		return manualDelivery;
	}
	public void setManualDelivery(Boolean manualDelivery) {
		this.manualDelivery = manualDelivery;
	}
	@XmlElement (name = "tender_files_delivery_method_downloadble")
	public Integer getDownloadableDelivery() {
		return downloadableDelivery;
	}
	public void setDownloadableDelivery(Integer downloadableDelivery) {
		this.downloadableDelivery = downloadableDelivery;
	}
	@XmlElement (name = "tender_files_delivery_location")
	public String getDeliveryLocation() {
		return deliveryLocation;
	}
	public void setDeliveryLocation(String deliveryLocation) {
		this.deliveryLocation = deliveryLocation;
	}
	
}
