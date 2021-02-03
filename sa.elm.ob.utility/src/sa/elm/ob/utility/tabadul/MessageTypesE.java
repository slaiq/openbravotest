package sa.elm.ob.utility.tabadul;

public enum MessageTypesE {
	
	ERROR ("error"), SUCCESS ("success") , WARNING ("warning");
	
	private String type;
	
	private MessageTypesE (String type){
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	

}
