package sa.elm.ob.scm.webservice.vehiclesystem.dto;

import java.io.Serializable;

/**
 * 
 * @author Gopal KS
 *
 */
public class ProductDetailsIntRequest implements Serializable {

  private static final long serialVersionUID = -2076261718289883743L;
  private String organization;
  private String tagNo;
  private String currentBeneficiaryType;
  private String currentBeneficiaryIDName;
  private String newBeneficiaryIDName;
  private String itemCode;
  // private String itemName;
  // private String itemDescription;
  // private BigDecimal quantity;
  // private String attributeSet;
  // private String status;
  // private String mainCategory;
  // private String subCategory;
  // private String serialNumber;
  // private String plateNumber;
  // private String bodyNumber;
  // private String factoryYear;
  // private String tradeMark;
  // private String custodyDescription;
  // private BigDecimal cylinderNumber;
  // private BigDecimal fuelType;
  // private String color;

  public String getNewBeneficiaryIDName() {
    return newBeneficiaryIDName;
  }

  public void setNewBeneficiaryIDName(String newBeneficiaryIDName) {
    this.newBeneficiaryIDName = newBeneficiaryIDName;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  public String getTagNo() {
    return tagNo;
  }

  public void setTagNo(String tagNo) {
    this.tagNo = tagNo;
  }

  public String getCurrentBeneficiaryType() {
    return currentBeneficiaryType == null || currentBeneficiaryType.trim().equalsIgnoreCase("")
        ? "Employee"
        : "Employee";
  }

  public void setCurrentBeneficiaryType(String currentBeneficiaryType) {
    this.currentBeneficiaryType = currentBeneficiaryType;
  }

  public String getCurrentBeneficiaryIDName() {
    return currentBeneficiaryIDName;
  }

  public void setCurrentBeneficiaryIDName(String currentBeneficiaryIDName) {
    this.currentBeneficiaryIDName = currentBeneficiaryIDName;
  }

  public String getItemCode() {
    return itemCode;
  }

  public void setItemCode(String itemCode) {
    this.itemCode = itemCode;
  }

}
