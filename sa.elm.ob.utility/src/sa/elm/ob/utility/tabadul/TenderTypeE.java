package sa.elm.ob.utility.tabadul;
/**
 * 
 * @author mrahim
 *
 */
public enum TenderTypeE {
	TENDER ("TR","public") , LIMITED ("LD","Invitations / Direct purchase"), DIRECT ("DR","Invitations / Direct purchase");
	
	private String tabadulType;
	private String ObType;
	
	private TenderTypeE(String obType, String tabadulType) {
		this.ObType = obType;
		this.tabadulType = tabadulType;
	}

	public String getTabadulType() {
		return tabadulType;
	}

	public void setTabadulType(String tabadulType) {
		this.tabadulType = tabadulType;
	}

	public String getObType() {
		return ObType;
	}

	public void setObType(String obType) {
		ObType = obType;
	}
}
