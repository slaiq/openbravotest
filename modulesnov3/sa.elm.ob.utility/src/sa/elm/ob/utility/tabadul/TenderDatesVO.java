package sa.elm.ob.utility.tabadul;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
/**
 * 
 * @author mrahim
 *
 */

public class TenderDatesVO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1571094140910697775L;
	private String faqDeliveryDateHijri;
	private String faqDeliveryDateGregorian;
	private String offerDeliveryDateHijri;
	private String offerDeliveryDateGregorian;
	private String offerDeliveryHour;
	private String offerDeliveryMinute;
	private String openEnvelopeDateHijri;
	private String openEnvelopeDateGregorian;
	private String openEnvelopeHour;
	private String openEnvelopeMinute;
	
	@XmlElement (name="faq_delivery_date_hijri")
	public String getFaqDeliveryDateHijri() {
		return faqDeliveryDateHijri;
	}
	public void setFaqDeliveryDateHijri(String faqDeliveryDateHijri) {
		this.faqDeliveryDateHijri = faqDeliveryDateHijri;
	}
	@XmlElement (name="faq_delivery_date")
	public String getFaqDeliveryDateGregorian() {
		return faqDeliveryDateGregorian;
	}
	
	public void setFaqDeliveryDateGregorian(String faqDeliveryDateGregorian) {
		this.faqDeliveryDateGregorian = faqDeliveryDateGregorian;
	}
	@XmlElement (name="offer_delivery_date_hijri")
	public String getOfferDeliveryDateHijri() {
		return offerDeliveryDateHijri;
	}
	public void setOfferDeliveryDateHijri(String offerDeliveryDateHijri) {
		this.offerDeliveryDateHijri = offerDeliveryDateHijri;
	}
	@XmlElement (name="offer_delivery_date")
	public String getOfferDeliveryDateGregorian() {
		return offerDeliveryDateGregorian;
	}
	public void setOfferDeliveryDateGregorian(String offerDeliveryDateGregorian) {
		this.offerDeliveryDateGregorian = offerDeliveryDateGregorian;
	}
	@XmlElement (name="open_env_date_hijri")
	public String getOpenEnvelopeDateHijri() {
		return openEnvelopeDateHijri;
	}
	public void setOpenEnvelopeDateHijri(String openEnvelopeDateHijri) {
		this.openEnvelopeDateHijri = openEnvelopeDateHijri;
	}
	@XmlElement (name="open_env_date")
	public String getOpenEnvelopeDateGregorian() {
		return openEnvelopeDateGregorian;
	}
	public void setOpenEnvelopeDateGregorian(String openEnvelopeDateGregorian) {
		this.openEnvelopeDateGregorian = openEnvelopeDateGregorian;
	}
	@XmlElement (name="open_env_time_hour")
	public String getOpenEnvelopeHour() {
		return openEnvelopeHour;
	}
	public void setOpenEnvelopeHour(String openEnvelopeHour) {
		this.openEnvelopeHour = openEnvelopeHour;
	}
	@XmlElement (name="open_env_time_minute")
	public String getOpenEnvelopeMinute() {
		return openEnvelopeMinute;
	}
	public void setOpenEnvelopeMinute(String openEnvelopeMinute) {
		this.openEnvelopeMinute = openEnvelopeMinute;
	}
	@XmlElement (name="offer_delivery_time_hour")
	public String getOfferDeliveryHour() {
		return offerDeliveryHour;
	}
	public void setOfferDeliveryHour(String offerDeliveryHour) {
		this.offerDeliveryHour = offerDeliveryHour;
	}
	@XmlElement (name="offer_delivery_time_minute")
	public String getOfferDeliveryMinute() {
		return offerDeliveryMinute;
	}
	public void setOfferDeliveryMinute(String offerDeliveryMinute) {
		this.offerDeliveryMinute = offerDeliveryMinute;
	}
}
