package sa.elm.ob.utility.tabadul;

import java.io.Serializable;

/**
 * Offer Details VO
 * @author mrahim
 *
 */
public class OfferDetailVO implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4438684691899567714L;
	private String notes;
	private Double amount;

	public String getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes = notes;
	}
	public Double getAmount() {
		return amount;
	}
	public void setAmount(Double amount) {
		this.amount = amount;
	}
	
	
}
