package sa.elm.ob.hcm.dto.payroll;

import java.io.Serializable;
import java.util.Map;

import sa.elm.ob.hcm.selfservice.dto.GenericDTO;

public class EarningsAndDeductionsDTO extends GenericDTO implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 6244236915631316039L;

  private Map<String, String> earnings;
  private Map<String, String> deduction;
  private String payrollPeriod;
  private String payrollType;
  private String payrollStatus;
  private String basicSalary;
  private String totalEarnings;
  private String grossSalary;
  private String pension;
  private String totalDeductions;
  private String netSalary;

  public String getBasicSalary() {
    return basicSalary;
  }

  public void setBasicSalary(String basicSalary) {
    this.basicSalary = basicSalary;
  }

  public String getTotalEarnings() {
    return totalEarnings;
  }

  public void setTotalEarnings(String totalEarnings) {
    this.totalEarnings = totalEarnings;
  }

  public String getGrossSalary() {
    return grossSalary;
  }

  public void setGrossSalary(String grossSalary) {
    this.grossSalary = grossSalary;
  }

  public String getPension() {
    return pension;
  }

  public void setPension(String pension) {
    this.pension = pension;
  }

  public String getTotalDeductions() {
    return totalDeductions;
  }

  public void setTotalDeductions(String totalDeductions) {
    this.totalDeductions = totalDeductions;
  }

  public String getNetSalary() {
    return netSalary;
  }

  public void setNetSalary(String netSalary) {
    this.netSalary = netSalary;
  }

  public Map<String, String> getEarnings() {
    return earnings;
  }

  public void setEarnings(Map<String, String> earnings) {
    this.earnings = earnings;
  }

  public Map<String, String> getDeduction() {
    return deduction;
  }

  public void setDeduction(Map<String, String> deduction) {
    this.deduction = deduction;
  }

  public String getPayrollPeriod() {
    return payrollPeriod;
  }

  public void setPayrollPeriod(String payrollPeriod) {
    this.payrollPeriod = payrollPeriod;
  }

  public String getPayrollType() {
    return payrollType;
  }

  public void setPayrollType(String payrollType) {
    this.payrollType = payrollType;
  }

  public String getPayrollStatus() {
    return payrollStatus;
  }

  public void setPayrollStatus(String payrollStatus) {
    this.payrollStatus = payrollStatus;
  }

}
