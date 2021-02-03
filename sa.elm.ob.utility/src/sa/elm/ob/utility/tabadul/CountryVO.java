package sa.elm.ob.utility.tabadul;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CountryVO {
	
	private String id;
	private String name;
	private String nameAr;
	private String isoCode;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getNameAr() {
		return nameAr;
	}
	@JsonProperty("name_ar")
	public void setNameAr(String nameAr) {
		this.nameAr = nameAr;
	}
	public String getIsoCode() {
		return isoCode;
	}
	@JsonProperty("iso_code")
	public void setIsoCode(String isoCode) {
		this.isoCode = isoCode;
	}
	
}
