package sa.elm.ob.hcm.services.payroll;

import sa.elm.ob.hcm.dto.payroll.BankDetailsDTO;
import sa.elm.ob.hcm.dto.payroll.EarningsAndDeductionsDTO;
import sa.elm.ob.hcm.dto.payroll.PaySlipDTO;
import sa.elm.ob.hcm.dto.payroll.SalaryCertificateRequestDTO;

/**
 * Payroll Information Service
 * 
 * @author Gopalakrishnan
 * @author oalbader
 *
 */
public interface PayrollInformationService {

  /**
   * Get All the Information for Pay Slip including Employment Information, Earnings, Deductions and
   * Summary
   * 
   * @param username
   * @return
   */
  PaySlipDTO getPaySlipInformation(String username);

  /**
   * Get only Earnings and Deductions based on Payroll Period.
   * 
   * @param payrollPeriod
   * @return
   */
  EarningsAndDeductionsDTO getEarningsAndDeductionsByPeriod(String username, String payrollPeriod);

  /**
   * @param username
   * @return
   */
  BankDetailsDTO getBankDetails(String username);

  /**
   * @param username
   * @param salaryCertificateRequestDTO
   * @return
   */
  SalaryCertificateRequestDTO submitSalaryCertificateRequest(String username,
      SalaryCertificateRequestDTO salaryCertificateRequestDTO);

  /**
   * @param username
   * @param bankDetailsDTO
   * @return
   */
  BankDetailsDTO submitChangeBankDetailsRequest(String username, BankDetailsDTO bankDetailsDTO);

}
