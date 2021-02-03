package sa.elm.ob.utility.tabadul;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * VO to hold the Purchases data returned by Get Purchases List for a tender
 * @author mrahim
 *
 */
public class PurchasesVO implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6691035257278282536L;
	
	List <ItemVO> items = new ArrayList<ItemVO>();

	public List<ItemVO> getItems() {
		return items;
	}

	public void setItems(List<ItemVO> items) {
		this.items = items;
	}

}
