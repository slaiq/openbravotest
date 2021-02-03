package sa.elm.ob.utility.tabadul;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Branches {
	
	private Branch branch;

	@JsonProperty("companybranch")
	public Branch getBranch() {
		return branch;
	}

	public void setBranch(Branch branch) {
		this.branch = branch;
	}

}
