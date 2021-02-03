package sa.elm.ob.utility.tabadul;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
/**
 * 
 * @author mrahim
 *
 */
public class TaxonomyAndLocationVO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4783204135588287407L;
	private String executeLocation;
	private String executeLocationText;
	private Integer [] tenderTaxononies;
	
	@XmlElement (name = "execution_location")
	public String getExecuteLocation() {
		return executeLocation;
	}
	public void setExecuteLocation(String executeLocation) {
		this.executeLocation = executeLocation;
	}
	@XmlElement (name = "execute_location_text")
	public String getExecuteLocationText() {
		return executeLocationText;
	}
	public void setExecuteLocationText(String executeLocationText) {
		this.executeLocationText = executeLocationText;
	}
	@XmlElement (name = "tenders_taxonomies")
	public Integer[] getTenderTaxononies() {
		return tenderTaxononies;
	}
	public void setTenderTaxononies(Integer[] tenderTaxononies) {
		this.tenderTaxononies = tenderTaxononies;
	}

}
