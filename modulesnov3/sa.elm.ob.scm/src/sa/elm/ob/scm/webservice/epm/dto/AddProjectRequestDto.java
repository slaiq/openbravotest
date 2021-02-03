package sa.elm.ob.scm.webservice.epm.dto;

import java.math.BigDecimal;

public class AddProjectRequestDto {

  private Integer projectPlaceId;
  private String name;
  private String year; // 2019,
  private String contractSignDate_Georgian;// 2019-10-29,
  private BigDecimal projectApprovedValue;
  private BigDecimal valueFinalContract;
  private String numberOfBaptism;
  private BigDecimal valueApprovedExtracts;
  private String gRPNumber;

  public AddProjectRequestDto() {

  }

  public Integer getProjectPlaceId() {
    return projectPlaceId;
  }

  public void setProjectPlaceId(Integer projectPlaceId) {
    this.projectPlaceId = projectPlaceId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getYear() {
    return year;
  }

  public void setYear(String year) {
    this.year = year;
  }

  public String getContractSignDate_Georgian() {
    return contractSignDate_Georgian;
  }

  public void setContractSignDate_Georgian(String contractSignDate_Georgian) {
    this.contractSignDate_Georgian = contractSignDate_Georgian;
  }

  public BigDecimal getProjectApprovedValue() {
    return projectApprovedValue;
  }

  public void setProjectApprovedValue(BigDecimal projectApprovedValue) {
    this.projectApprovedValue = projectApprovedValue;
  }

  public BigDecimal getValueFinalContract() {
    return valueFinalContract;
  }

  public void setValueFinalContract(BigDecimal valueFinalContract) {
    this.valueFinalContract = valueFinalContract;
  }

  public String getNumberOfBaptism() {
    return numberOfBaptism;
  }

  public void setNumberOfBaptism(String numberOfBaptism) {
    this.numberOfBaptism = numberOfBaptism;
  }

  public BigDecimal getValueApprovedExtracts() {
    return valueApprovedExtracts;
  }

  public void setValueApprovedExtracts(BigDecimal valueApprovedExtracts) {
    this.valueApprovedExtracts = valueApprovedExtracts;
  }

  public String getgRPNumber() {
    return gRPNumber;
  }

  public void setgRPNumber(String gRPNumber) {
    this.gRPNumber = gRPNumber;
  }

}
