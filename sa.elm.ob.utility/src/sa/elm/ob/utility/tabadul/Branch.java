package sa.elm.ob.utility.tabadul;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Branch {
	
	@JsonProperty("c_type")
	private String cType;
	
	@JsonProperty("city")
    private String city;
	
	@JsonProperty("phone")
    private String phone;
	
	@JsonProperty("ext")
    private String ext;
	
	@JsonProperty("fax")
    private String fax;
	
	@JsonProperty("fax_ext")
    private String faxExt;
	
	@JsonProperty("po_box")
    private String poBox;
	
	@JsonProperty("zip_code")
    private String zipCode;
	
	@JsonProperty("location")
    private String location;
	
	public String getcType() {
		return cType;
	}
	public void setcType(String cType) {
		this.cType = cType;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public String getExt() {
		return ext;
	}
	public void setExt(String ext) {
		this.ext = ext;
	}
	public String getFax() {
		return fax;
	}
	public void setFax(String fax) {
		this.fax = fax;
	}
	public String getFaxExt() {
		return faxExt;
	}
	public void setFaxExt(String faxExt) {
		this.faxExt = faxExt;
	}
	public String getPoBox() {
		return poBox;
	}
	public void setPoBox(String poBox) {
		this.poBox = poBox;
	}
	public String getZipCode() {
		return zipCode;
	}
	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
		
}
