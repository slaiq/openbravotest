package sa.elm.ob.utility.tabadul;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;

/**
 * Tender Categories
 * @author mrahim
 *
 */
public class TenderCategoriesVO implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 2921017847770313932L;
	private Integer [] tenderCategories;
	
	@XmlElement (name = "tenders_categories")
	public Integer[] getTenderCategories() {
		return tenderCategories;
	}

	public void setTenderCategories(Integer[] tenderCategories) {
		this.tenderCategories = tenderCategories;
	}

}
