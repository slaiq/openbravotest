package sa.elm.ob.finance.ad_process;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;

import sa.elm.ob.utility.gsb.adf.GSBADFCLFSVV1;
import sa.elm.ob.utility.gsb.adf.GetLoanInformationResponseStructure;
import sa.elm.ob.utility.gsb.adf.Interface1GetLoanInformationCommonErrorElementFaultMessage;
import sa.elm.ob.utility.gsb.redf.CommonErrorStructure;
import sa.elm.ob.utility.gsb.redf.GSBREDFLIVV2;
import sa.elm.ob.utility.gsb.redf.GetLoanDetailsResponseStructure;
import sa.elm.ob.utility.gsb.redf.Interface1;
import sa.elm.ob.utility.gsb.redf.Interface1GetLoanDetailsCommonErrorElementFaultMessage;
import sa.elm.ob.utility.gsb.sdb.GSBSDBLIVV1;
import sa.elm.ob.utility.gsb.sdb.GetLoanInfoResponseStructure;
import sa.elm.ob.utility.gsb.sdb.Interface1GetLoanInfoCommonErrorElementFaultMessage;
import sa.elm.ob.utility.util.Utility;

public class FundDeptEnquiryDeptServiceImpl implements FundDeptEnquiryDeptService {
  private final static String ADF_API_KEY = "GSB.adfApiKey";
  private final static String REDF_API_KEY = "GSB.redfApiKey";
  private final static String SDB_API_KEY = "GSB.sdbApiKey";

  @Override
  public GetLoanDetailsResponseStructure getRedfDetails(String CitizenId, String Dob) {
    GetLoanDetailsResponseStructure loanDetails = null;
    try {
      loanDetails = getRedfService().getLoanDetails(CitizenId, Dob);
    } catch (Interface1GetLoanDetailsCommonErrorElementFaultMessage e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return loanDetails;

  }

  @Override
  public GetLoanInformationResponseStructure getAdfDetails(String CitizenId) {
    GetLoanInformationResponseStructure loanDetails = null;
    try {
      loanDetails = getAdfService().getLoanInformation(CitizenId);
    } catch (Interface1GetLoanInformationCommonErrorElementFaultMessage e) {
      e.printStackTrace();
    }
    return loanDetails;
  }

  @Override
  public GetLoanInfoResponseStructure getSdbDetails(String CitizenId) {
    GetLoanInfoResponseStructure loanDetails = null;
    try {
      loanDetails = getSdbService().getLoanInfo(CitizenId);
    } catch (Interface1GetLoanInfoCommonErrorElementFaultMessage e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return loanDetails;
  }

  /**
   * Get the Library Interface
   * 
   * @return REDF Interface
   */
  private Interface1 getRedfService() {
    GSBREDFLIVV2 redfLibrary = new GSBREDFLIVV2();
    String apikey = Utility.getProperty(REDF_API_KEY);
    Interface1 redfInterface = redfLibrary.getGSBREDFLIVV1EP();

    Map<String, List<String>> requestHeaders = new HashMap<>();
    requestHeaders.put("apikey", Arrays.asList(apikey));
    BindingProvider bindingProvider = (BindingProvider) redfInterface;
    bindingProvider.getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS, requestHeaders);

    return redfInterface;
  }

  /**
   * 
   * @return adf Interface
   */
  private sa.elm.ob.utility.gsb.adf.Interface1 getAdfService() {
    GSBADFCLFSVV1 adfLibrary = new GSBADFCLFSVV1();
    String apikey = Utility.getProperty(ADF_API_KEY);
    sa.elm.ob.utility.gsb.adf.Interface1 adfInterace = adfLibrary.getGSBADFCLFSVV1EP();

    Map<String, List<String>> requestHeaders = new HashMap<>();
    requestHeaders.put("apikey", Arrays.asList(apikey));
    BindingProvider bindingProvider = (BindingProvider) adfInterace;
    bindingProvider.getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS, requestHeaders);

    return adfInterace;
  }

  /**
   * 
   * @return sdb Interface
   */
  private sa.elm.ob.utility.gsb.sdb.Interface1 getSdbService() {
    GSBSDBLIVV1 sdfLibrary = new GSBSDBLIVV1();
    String apikey = Utility.getProperty(SDB_API_KEY);
    sa.elm.ob.utility.gsb.sdb.Interface1 sdbInterface = sdfLibrary.getGSBSDBLIVV1EP();

    Map<String, List<String>> requestHeaders = new HashMap<>();
    requestHeaders.put("apikey", Arrays.asList(apikey));
    BindingProvider bindingProvider = (BindingProvider) sdbInterface;
    bindingProvider.getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS, requestHeaders);

    return sdbInterface;
  }

  @Override
  public CommonErrorStructure getErrorDetails(String citizenId, String dob) {
    CommonErrorStructure erroDetails = null;

    GetLoanDetailsResponseStructure loanDetails = null;
    try {
      loanDetails = getRedfService().getLoanDetails(citizenId, dob);
    } catch (Interface1GetLoanDetailsCommonErrorElementFaultMessage e) {
      erroDetails = e.getFaultInfo();
    }
    return erroDetails;
  }

  @Override
  public sa.elm.ob.utility.gsb.adf.CommonErrorStructure getErrorDetails(String citizenId) {
    sa.elm.ob.utility.gsb.adf.CommonErrorStructure errorDetails = null;
    try {
      GetLoanInformationResponseStructure loanDetails = getAdfService()
          .getLoanInformation(citizenId);
    } catch (Interface1GetLoanInformationCommonErrorElementFaultMessage e) {
      errorDetails = e.getFaultInfo();
    }
    return errorDetails;
  }

  @Override
  public sa.elm.ob.utility.gsb.sdb.CommonErrorStructure getErrorDetail(String citizenId) {
    sa.elm.ob.utility.gsb.sdb.CommonErrorStructure errorDetails = null;
    try {
      GetLoanInfoResponseStructure loanDetails = getSdbService().getLoanInfo(citizenId);
    } catch (Interface1GetLoanInfoCommonErrorElementFaultMessage e) {
      // TODO Auto-generated catch block
      errorDetails = e.getFaultInfo();
    }
    return errorDetails;
  }

}
