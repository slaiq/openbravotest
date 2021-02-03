package sa.elm.ob.finance.ad_process;

import java.io.IOException;
import java.math.BigDecimal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.erpCommon.utility.OBMessageUtils;

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
      String loanNumber = request.getParameter("inpLoanNo");
      String contractNumber = request.getParameter("inpContractNo");

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
          } else if (loanType.equals("agri")) {
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
                  if (objLoanInfo != null
                      && objLoanInfo.getContractNumber().equals(contractNumber)) {
                    loanAmount = objLoanInfo.getLoanAmount();
                    loanRemainingAmount = objLoanInfo.getRemainingAmount();
                    paidAmount = objLoanInfo.getPaidAmount();
                    citizenName = objLoanInfo.getClientName().getPersonFullName();
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
            sb.append("<LoanDetails>");
            sb.append("<citizenName>" + citizenName + "</citizenName>");
            sb.append("<loanAmount>" + loanAmount + "</loanAmount>");
            sb.append("<loanRemainingAmount>" + loanRemainingAmount + "</loanRemainingAmount>");
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
                  if (objLoan != null && objLoan.getLoanNumber().equals(loanNumber)) {
                    loanAmount = objLoan.getLoanAmount();
                    loanRemainingAmount = objLoan.getRemainingAmount();
                    paidAmount = objLoan.getPaidAmount();
                    citizenName = objLoanInfoStructure.getLoanerInfo().getLoanerName()
                        .getPersonFullName();
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

            sb.append("<LoanDetails>");
            sb.append("<citizenName>" + citizenName + "</citizenName>");
            sb.append("<loanAmount>" + loanAmount + "</loanAmount>");
            sb.append("<loanRemainingAmount>" + loanRemainingAmount + "</loanRemainingAmount>");
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
}