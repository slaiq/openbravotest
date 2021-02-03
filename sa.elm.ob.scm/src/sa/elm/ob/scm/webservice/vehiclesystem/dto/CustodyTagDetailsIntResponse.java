package sa.elm.ob.scm.webservice.vehiclesystem.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class CustodyTagDetailsIntResponse implements Serializable {

  private String organization;
  private String tagNo;
  private String currentBeneficiaryType;
  private String currentBeneficiaryIDName;
  private BigDecimal itemCode;
  private String itemName;
  private String itemDescription;
  private BigDecimal quantity;
  private String attributeSet;
  private String status;
  private String mainCategory;
  private String subCategory;
  private String serialNumber;
  private String plateNumber;
  private String bodyNumber;
  private String factoryYear;
  private String tradeMark;
  private String custodyDescription;
  private String cylinderNumber;
  private String fuelType;
  private String color;

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
    return currentBeneficiaryType;
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

  public BigDecimal getItemCode() {
    return itemCode;
  }

  public void setItemCode(BigDecimal itemCode) {
    this.itemCode = itemCode;
  }

  public String getItemName() {
    return itemName;
  }

  public void setItemName(String itemName) {
    this.itemName = itemName;
  }

  public String getItemDescription() {
    return itemDescription;
  }

  public void setItemDescription(String itemDescription) {
    this.itemDescription = itemDescription;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public String getAttributeSet() {
    return attributeSet;
  }

  public void setAttributeSet(String attributeSet) {
    this.attributeSet = attributeSet;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getMainCategory() {
    return mainCategory;
  }

  public void setMainCategory(String mainCategory) {
    this.mainCategory = mainCategory;
  }

  public String getSubCategory() {
    return subCategory;
  }

  public void setSubCategory(String subCategory) {
    this.subCategory = subCategory;
  }

  public String getSerialNumber() {
    return serialNumber;
  }

  public void setSerialNumber(String serialNumber) {
    this.serialNumber = serialNumber;
  }

  public String getPlateNumber() {
    return plateNumber;
  }

  public void setPlateNumber(String plateNumber) {
    this.plateNumber = plateNumber;
  }

  public String getBodyNumber() {
    return bodyNumber;
  }

  public void setBodyNumber(String bodyNumber) {
    this.bodyNumber = bodyNumber;
  }

  public String getFactoryYear() {
    return factoryYear;
  }

  public void setFactoryYear(String factoryYear) {
    this.factoryYear = factoryYear;
  }

  public String getTradeMark() {
    return tradeMark;
  }

  public void setTradeMark(String tradeMark) {
    this.tradeMark = tradeMark;
  }

  public String getCustodyDescription() {
    return custodyDescription;
  }

  public void setCustodyDescription(String custodyDescription) {
    this.custodyDescription = custodyDescription;
  }

  public String getCylinderNumber() {
    return cylinderNumber;
  }

  public void setCylinderNumber(String cylinderNumber) {
    this.cylinderNumber = cylinderNumber;
  }

  public String getFuelType() {
    return fuelType;
  }

  public void setFuelType(String fuelType) {
    this.fuelType = fuelType;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

}
