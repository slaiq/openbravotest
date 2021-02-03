package sa.elm.ob.utility.tabadul;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;


public class SupplierVO implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 607829320918253585L;
	private Integer uid;
	private String vendor_type_code;
	private String cr_number;
	private String c_name;
	private String c_create_hijri;
	private String c_expire_hijri;
	private String cr_activity;
	private String board_of_management;
	private String owner_name;
	private String nat;
	private String first_name;
	private String second_name;
	private String family_name;
	private String nationality;
	private String user_NAT;
	private String job;
	private String mobile;
	private String phone;
	private String ext;
	private String cocMemberChamberCity;
	private String estLaborOfficeId;
	private String estSequenceNumber;
	private String gosiRegistrationID;
	private String licenseID;
	private Branches branches;
	
	
	public Integer getUid() {
		return uid;
	}
	public void setUid(Integer uid) {
		this.uid = uid;
	}
	public String getVendor_type_code() {
		return vendor_type_code;
	}
	public void setVendor_type_code(String vendor_type_code) {
		this.vendor_type_code = vendor_type_code;
	}
	public String getCr_number() {
		return cr_number;
	}
	public void setCr_number(String cr_number) {
		this.cr_number = cr_number;
	}
	public String getC_name() {
		return c_name;
	}
	public void setC_name(String c_name) {
		this.c_name = c_name;
	}
	public String getC_create_hijri() {
		return c_create_hijri;
	}
	public void setC_create_hijri(String c_create_hijri) {
		this.c_create_hijri = c_create_hijri;
	}
	public String getC_expire_hijri() {
		return c_expire_hijri;
	}
	public void setC_expire_hijri(String c_expire_hijri) {
		this.c_expire_hijri = c_expire_hijri;
	}
	public String getCr_activity() {
		return cr_activity;
	}
	public void setCr_activity(String cr_activity) {
		this.cr_activity = cr_activity;
	}
	public String getBoard_of_management() {
		return board_of_management;
	}
	public void setBoard_of_management(String board_of_management) {
		this.board_of_management = board_of_management;
	}
	public String getOwner_name() {
		return owner_name;
	}
	public void setOwner_name(String owner_name) {
		this.owner_name = owner_name;
	}

	@JsonProperty("NAT")
	public String getNat() {
		return nat;
	}
	public void setNat(String nat) {
		this.nat = nat;
	}
	public String getFirst_name() {
		return first_name;
	}
	public void setFirst_name(String first_name) {
		this.first_name = first_name;
	}
	public String getSecond_name() {
		return second_name;
	}
	public void setSecond_name(String second_name) {
		this.second_name = second_name;
	}
	public String getFamily_name() {
		return family_name;
	}
	public void setFamily_name(String family_name) {
		this.family_name = family_name;
	}
	public String getNationality() {
		return nationality;
	}
	public void setNationality(String nationality) {
		this.nationality = nationality;
	}
	public String getUser_NAT() {
		return user_NAT;
	}
	public void setUser_NAT(String user_NAT) {
		this.user_NAT = user_NAT;
	}
	public String getJob() {
		return job;
	}
	public void setJob(String job) {
		this.job = job;
	}
	public String getMobile() {
		return mobile;
	}
	public void setMobile(String mobile) {
		this.mobile = mobile;
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
	public String getCocMemberChamberCity() {
		return cocMemberChamberCity;
	}
	public void setCocMemberChamberCity(String cocMemberChamberCity) {
		this.cocMemberChamberCity = cocMemberChamberCity;
	}
	@JsonProperty("EstLaborOfficeId")
	public String getEstLaborOfficeId() {
		return estLaborOfficeId;
	}
	public void setEstLaborOfficeId(String estLaborOfficeId) {
		this.estLaborOfficeId = estLaborOfficeId;
	}
	@JsonProperty("EstSequenceNumber")
	public String getEstSequenceNumber() {
		return estSequenceNumber;
	}
	public void setEstSequenceNumber(String estSequenceNumber) {
		this.estSequenceNumber = estSequenceNumber;
	}
	@JsonProperty("GosiRegistrationID")
	public String getGosiRegistrationID() {
		return gosiRegistrationID;
	}
	public void setGosiRegistrationID(String gosiRegistrationID) {
		this.gosiRegistrationID = gosiRegistrationID;
	}
	@JsonProperty("LicenseID")
	public String getLicenseID() {
		return licenseID;
	}
	public void setLicenseID(String licenseID) {
		this.licenseID = licenseID;
	}
	@JsonProperty("Companybranches")
	public Branches getBranches() {
		return branches;
	}
	public void setBranches(Branches branches) {
		this.branches = branches;
	}
	
}