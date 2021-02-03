package sa.elm.ob.hcm.dto.payroll;

import java.io.Serializable;

import sa.elm.ob.hcm.selfservice.dto.GenericDTO;

public class BankDetailsDTO extends GenericDTO implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = -1254788775429820598L;

  private String name;
  private String branch;
  private String accountName;
  private String iban;
  private String dateFrom;
  private String dateTo;
  private String EmployeeId;
  private String BankId;

  public String getEmployeeId() {
    return EmployeeId;
  }

  public void setEmployeeId(String employeeId) {
    EmployeeId = employeeId;
  }

  public String getBankId() {
    return BankId;
  }

  public void setBankId(String bankId) {
    BankId = bankId;
  }

  public String getIban() {
    return iban;
  }

  public void setIban(String iban) {
    this.iban = iban;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getBranch() {
    return branch;
  }

  public void setBranch(String branch) {
    this.branch = branch;
  }

  public String getAccountName() {
    return accountName;
  }

  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  public String getDateFrom() {
    return dateFrom;
  }

  public void setDateFrom(String dateFrom) {
    this.dateFrom = dateFrom;
  }

  public String getDateTo() {
    return dateTo;
  }

  public void setDateTo(String dateTo) {
    this.dateTo = dateTo;
  }

}
