package sa.elm.ob.hcm.services.payroll;

import org.springframework.stereotype.Service;

import sa.elm.ob.hcm.dto.payroll.BankDetailsDTO;
import sa.elm.ob.hcm.dto.payroll.EarningsAndDeductionsDTO;
import sa.elm.ob.hcm.dto.payroll.PaySlipDTO;
import sa.elm.ob.hcm.dto.payroll.SalaryCertificateRequestDTO;

/**
 * @author Gopalakrishnan
 * @author oalbader
 *
 */
@Service
public class PayrollInformationServiceImpl implements PayrollInformationService {

  @Override
  public PaySlipDTO getPaySlipInformation(String username) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public EarningsAndDeductionsDTO getEarningsAndDeductionsByPeriod(String username,
      String payrollPeriod) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public BankDetailsDTO getBankDetails(String username) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public SalaryCertificateRequestDTO submitSalaryCertificateRequest(String username,
      SalaryCertificateRequestDTO salaryCertificateRequestDTO) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public BankDetailsDTO submitChangeBankDetailsRequest(String username,
      BankDetailsDTO bankDetailsDTO) {
    // TODO Auto-generated method stub
    return null;
  }

}
