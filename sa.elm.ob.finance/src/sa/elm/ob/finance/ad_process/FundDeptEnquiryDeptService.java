package sa.elm.ob.finance.ad_process;

import sa.elm.ob.utility.gsb.adf.GetLoanInformationResponseStructure;
import sa.elm.ob.utility.gsb.redf.CommonErrorStructure;
import sa.elm.ob.utility.gsb.redf.GetLoanDetailsResponseStructure;
import sa.elm.ob.utility.gsb.sdb.GetLoanInfoResponseStructure;

public interface FundDeptEnquiryDeptService {
  /**
   * 
   * @param CitizenId
   * @param Dob
   * @return details REDF details
   */
  GetLoanDetailsResponseStructure getRedfDetails(String citizenId, String dob);

  /**
   * 
   * @param CitizenId
   * @return ADF details
   */
  GetLoanInformationResponseStructure getAdfDetails(String citizenId);

  /**
   * 
   * @param CitizenId
   * @return SDB details
   */
  GetLoanInfoResponseStructure getSdbDetails(String citizenId);

  CommonErrorStructure getErrorDetails(String citizenId, String dob);

  sa.elm.ob.utility.gsb.adf.CommonErrorStructure getErrorDetails(String citizenId);

  sa.elm.ob.utility.gsb.sdb.CommonErrorStructure getErrorDetail(String citizenId);

}
