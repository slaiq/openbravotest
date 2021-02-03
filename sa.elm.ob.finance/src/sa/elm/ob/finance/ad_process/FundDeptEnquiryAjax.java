package sa.elm.ob.finance.ad_process;

import java.io.IOException;
import java.math.BigDecimal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;

import sa.elm.ob.utility.gsb.adf.GetLoanInformationResponseStructure;
import sa.elm.ob.utility.gsb.adf.LoanInfoStructure;
import sa.elm.ob.utility.gsb.redf.CommonErrorStructure;
import sa.elm.ob.utility.gsb.redf.GetLoanDetailsResponseStructure;
import sa.elm.ob.utility.gsb.sdb.GetLoanInfoResponseStructure;
import sa.elm.ob.utility.gsb.sdb.LoanDetailsStructure;
import sa.elm.ob.utility.gsb.sdb.LoanInfoListStructure;

public class FundDeptEnquiryAjax extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @SuppressWarnings("static-access")
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    try {

      String action = (request.getParameter("action") == null ? ""
          : request.getParameter("action"));
      String loanType = request.getParameter("inpLoanType");
      String citizenId = request.getParameter("inpCitizenId");
      String contractNumber = "";
      String str_loan_remain_amt = "";
      String str_loanNumber = "";

      String dob = request.getParameter("inpdob");
      BigDecimal loanAmount = BigDecimal.ZERO;
      BigDecimal loanRemainingAmount = BigDecimal.ZERO;
      BigDecimal paidAmount = BigDecimal.ZERO;
      String citizenName = "";
      String scheduled = "";
      String demanded = "";
      String loanerType = "";
      String errorCode = "", errorCode_ob = "";
      String errorMessage = "";
      String module_prefix = "EFIN_";

      FundDeptEnquiryDeptService loanDetails = new FundDeptEnquiryDeptServiceImpl();
      if (action.equals("getLoanDetails")) {
        StringBuffer sb = new StringBuffer();
        try {
          // Fetch the Real Estate fund details
          if (loanType.equals("rst")) {
            GetLoanDetailsResponseStructure rstObj = null;
            CommonErrorStructure errorRstObj = loanDetails.getErrorDetails(citizenId, dob);

            if (errorRstObj == null) {
              rstObj = loanDetails.getRedfDetails(citizenId, dob);
              citizenName = rstObj == null ? ""
                  : (rstObj.getGetLoanDetailsResponseDetailObject() == null ? ""
                      : (rstObj.getGetLoanDetailsResponseDetailObject().getCitizenName() == null
                          ? ""
                          : rstObj.getGetLoanDetailsResponseDetailObject().getCitizenName()));
              scheduled = rstObj == null ? ""
                  : (rstObj.getGetLoanDetailsResponseDetailObject() == null ? ""
                      : (rstObj.getGetLoanDetailsResponseDetailObject().getIsScheduled() == null
                          ? ""
                          : rstObj.getGetLoanDetailsResponseDetailObject().getIsScheduled()
                              .toString()));
              demanded = rstObj == null ? ""
                  : (rstObj.getGetLoanDetailsResponseDetailObject() == null ? ""
                      : (rstObj.getGetLoanDetailsResponseDetailObject().getIsDemanded() == null ? ""
                          : rstObj.getGetLoanDetailsResponseDetailObject().getIsDemanded()
                              .toString()));
              loanerType = rstObj == null ? ""
                  : (rstObj.getGetLoanDetailsResponseDetailObject() == null ? ""
                      : (rstObj.getGetLoanDetailsResponseDetailObject().getLoanerType() == null ? ""
                          : rstObj.getGetLoanDetailsResponseDetailObject().getLoanerType()
                              .toString()));
              if (rstObj.getServiceError() != null) {
                CommonErrorStructure errorValidResponse = rstObj.getServiceError();
                errorCode = errorValidResponse.getCode();
                errorCode_ob = module_prefix.concat("R").concat(errorCode);
                errorMessage = OBMessageUtils.messageBD(errorCode_ob);
              }

            } else {
              errorCode = errorRstObj.getCode();
              errorCode_ob = module_prefix.concat("R").concat(errorCode);
              errorMessage = OBMessageUtils.messageBD(errorCode_ob);
            }
            if (StringUtils.isEmpty(citizenName) || StringUtils.isBlank(citizenName)) {
              citizenName = getGrpCitizenName(citizenId);
            }
            sb.append("<LoanDetails>");
            sb.append("<citizenName>" + citizenName + "</citizenName>");
            sb.append("<loanAmount>" + loanAmount + "</loanAmount>");
            sb.append("<loanRemainingAmount>" + loanRemainingAmount + "</loanRemainingAmount>");
            sb.append("<scheduled>" + scheduled + "</scheduled>");
            sb.append("<demanded>" + demanded + "</demanded>");
            sb.append("<loanerType>" + loanerType + "</loanerType>");
            sb.append("<paidAmount>" + paidAmount + "</paidAmount>");
            sb.append("<errorCode>" + errorCode + "</errorCode>");
            sb.append("<errorMessage>" + errorMessage + "</errorMessage>");
            sb.append("</LoanDetails>");
          }

          else if (loanType.equals("agri")) {
            // Fetch the agiculture development fund
            // before calling the actual methods verify the error info
            sa.elm.ob.utility.gsb.adf.CommonErrorStructure errorAdf = loanDetails
                .getErrorDetails(citizenId);
            if (errorAdf == null) {
              GetLoanInformationResponseStructure adfObj = loanDetails.getAdfDetails(citizenId);
              if (adfObj.getGetLoanInformationResponseDetailObject() != null
                  && adfObj.getGetLoanInformationResponseDetailObject().getLoanInfo().size() > 0) {
                for (LoanInfoStructure objLoanInfo : adfObj
                    .getGetLoanInformationResponseDetailObject().getLoanInfo()) {
                  if (objLoanInfo != null && objLoanInfo.getContractNumber() != null
                      && (objLoanInfo.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0)) {

                    contractNumber = contractNumber.concat(objLoanInfo.getContractNumber())
                        .concat(",");
                    loanAmount = objLoanInfo.getLoanAmount();
                    loanRemainingAmount = objLoanInfo.getRemainingAmount();
                    paidAmount = objLoanInfo.getPaidAmount();

                    String remainingAmount = loanRemainingAmount.toString().replace(",", "");
                    str_loan_remain_amt = str_loan_remain_amt.concat(remainingAmount).concat(",");
                    if (StringUtils.isEmpty(citizenName)) {
                      citizenName = objLoanInfo.getClientName() == null ? ""
                          : (objLoanInfo.getClientName().getPersonFullName() == null ? ""
                              : objLoanInfo.getClientName().getPersonFullName());
                    }
                  }
                }
              }
              // find the error message in the valid response
              if (adfObj.getServiceError() != null) {
                sa.elm.ob.utility.gsb.adf.CommonErrorStructure errorValidResponse = adfObj
                    .getServiceError();
                errorCode = errorValidResponse.getCode();
                errorCode_ob = module_prefix.concat("A").concat(errorCode);
                errorMessage = OBMessageUtils.messageBD(errorCode_ob);
              }
            } else {
              errorCode = errorAdf.getCode();
              errorCode_ob = module_prefix.concat("A").concat(errorCode);
              errorMessage = OBMessageUtils.messageBD(errorCode_ob);
            }
            if (StringUtils.isEmpty(citizenName) || StringUtils.isBlank(citizenName)) {
              citizenName = getGrpCitizenName(citizenId);
            }
            // removing the comma at trailing place
            contractNumber = removeTrailingCommas(contractNumber);
            str_loan_remain_amt = removeTrailingCommas(str_loan_remain_amt);

            sb.append("<LoanDetails>");
            sb.append("<citizenName>" + citizenName + "</citizenName>");
            sb.append("<loanAmount>" + loanAmount + "</loanAmount>");
            sb.append("<contractNumber>" + contractNumber + "</contractNumber>");
            sb.append("<loanRemainingAmount>" + str_loan_remain_amt + "</loanRemainingAmount>");
            sb.append("<paidAmount>" + paidAmount + "</paidAmount>");
            sb.append("<errorCode>" + errorCode + "</errorCode>");
            sb.append("<errorMessage>" + errorMessage + "</errorMessage>");
            sb.append("</LoanDetails>");
          } else if (loanType.equals("scsb")) {
            // fetch the scsb bank details
            // before calling the actual methods verify the error info
            sa.elm.ob.utility.gsb.sdb.CommonErrorStructure errorScsb = loanDetails
                .getErrorDetail(citizenId);
            if (errorScsb == null) {
              GetLoanInfoResponseStructure scsbObj = loanDetails.getSdbDetails(citizenId);
              if (scsbObj.getGetLoanInfoResponseDetailObject() != null
                  && scsbObj.getGetLoanInfoResponseDetailObject().getLoanInfo().size() > 0) {
                LoanInfoListStructure objLoanInfo = scsbObj.getGetLoanInfoResponseDetailObject();
                // LoanDetailsStructure objLoanInfo = scsbObj.getGetLoanInfoResponseDetailObject()
                // .getLoanInfo().get(0).getLoanDetails();
                for (sa.elm.ob.utility.gsb.sdb.LoanInfoStructure objLoanInfoStructure : objLoanInfo
                    .getLoanInfo()) {

                  LoanDetailsStructure objLoan = objLoanInfoStructure.getLoanDetails();
                  if (objLoan != null && objLoan.getLoanNumber() != null
                      && (objLoan.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0)) {

                    str_loanNumber = str_loanNumber.concat(objLoan.getLoanNumber()).concat(",");

                    loanAmount = objLoan.getLoanAmount();
                    loanRemainingAmount = objLoan.getRemainingAmount();
                    String remainingAmount = loanRemainingAmount.toString().replace(",", "");
                    str_loan_remain_amt = str_loan_remain_amt.concat(remainingAmount).concat(",");

                    paidAmount = objLoan.getPaidAmount();
                    if (StringUtils.isEmpty(citizenName)) {
                      citizenName = objLoanInfoStructure.getLoanerInfo() == null ? ""
                          : (objLoanInfoStructure.getLoanerInfo().getLoanerName() == null ? ""
                              : (objLoanInfoStructure.getLoanerInfo().getLoanerName()
                                  .getPersonFullName() == null ? ""
                                      : objLoanInfoStructure.getLoanerInfo().getLoanerName()
                                          .getPersonFullName()));
                    }

                  }

                }
              }
              // find the error message in the valid response
              if (scsbObj.getServiceError() != null) {
                sa.elm.ob.utility.gsb.sdb.CommonErrorStructure errorValidResponse = scsbObj
                    .getServiceError();
                errorCode = errorValidResponse.getCode();
                errorCode_ob = module_prefix.concat("S").concat(errorCode);
                errorMessage = OBMessageUtils.messageBD(errorCode_ob);
              }
            } else {
              errorCode = errorScsb.getCode();
              errorCode_ob = module_prefix.concat("S").concat(errorCode);
              errorMessage = OBMessageUtils.messageBD(errorCode_ob);
            }
            if (StringUtils.isEmpty(citizenName) || StringUtils.isBlank(citizenName)) {
              citizenName = getGrpCitizenName(citizenId);
            }
            // removing the comma at trailing place
            str_loanNumber = removeTrailingCommas(str_loanNumber);
            str_loan_remain_amt = removeTrailingCommas(str_loan_remain_amt);

            sb.append("<LoanDetails>");
            sb.append("<citizenName>" + citizenName + "</citizenName>");
            sb.append("<loanAmount>" + loanAmount + "</loanAmount>");
            sb.append("<loanNumber>" + str_loanNumber + "</loanNumber>");
            sb.append("<loanRemainingAmount>" + str_loan_remain_amt + "</loanRemainingAmount>");
            sb.append("<paidAmount>" + paidAmount + "</paidAmount>");
            sb.append("<errorCode>" + errorCode + "</errorCode>");
            sb.append("<errorMessage>" + errorMessage + "</errorMessage>");
            sb.append("</LoanDetails>");

          }
        } catch (final Exception e) {
          log4j.error("Exception in  FundDeptsEnquiryAjax : ", e);
        } finally {
          response.setContentType("text/xml");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(sb.toString());
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in FundDeptsEnquiryAjax : ", e);
    }
  }

  /**
   * 
   * @param contractNumber
   * @return string after removed the trailing commas
   */
  private String removeTrailingCommas(String inputValue) {
    String outputValue = inputValue;
    if (inputValue.indexOf(",") != -1) {
      outputValue = inputValue.substring(0, inputValue.lastIndexOf(","));
    }
    return outputValue;
  }

  /**
   * 
   * @param citizenId
   * @return Citizen name Details
   */
  private String getGrpCitizenName(String citizenId) {
    String citizenName = "";
    try {
      OBContext.setAdminMode(true);
      OBQuery<BusinessPartner> bpQry = OBDal.getInstance().createQuery(BusinessPartner.class,
          " as e where e.searchKey=:searchKey");
      bpQry.setNamedParameter("searchKey", citizenId);
      if (bpQry.list().size() > 0) {
        BusinessPartner objBp = bpQry.list().get(0);
        citizenName = objBp.getName();
      }

    } catch (Exception e) {
      log4j.error("Exception in getGrpCitizenName: ", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
    return citizenName;
  }
  /**
   * 
   */
}