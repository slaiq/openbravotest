package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmPurchaseOrderConfiguration;
import sa.elm.ob.scm.ad_callouts.PurchaseAgreementCalloutDAO;
import sa.elm.ob.scm.ad_callouts.dao.PurOrderSummaryDAO;
import sa.elm.ob.scm.ad_process.ProposalManagement.CreatePoAttachmentDAO;
import sa.elm.ob.scm.ad_process.ProposalManagement.ProposalManagementProcessDAO;
import sa.elm.ob.scm.ad_process.ProposalManagement.ProposalManagementProcessDAOImpl;

public class CreatePOSubmitValidations extends BaseActionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(CreatePOSubmitValidations.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject result = new JSONObject();

    PreparedStatement st = null;
    ResultSet rs = null;
    Connection conn = OBDal.getInstance().getConnection();
    int years, mulyear = 0;
    int months = 0;
    int days = 0;
    ProposalManagementProcessDAO proposalDAO = new ProposalManagementProcessDAOImpl();
    try {
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      OBContext.setAdminMode(true);
      JSONObject jsonRequest = new JSONObject(content);

      if (jsonRequest.getString("action").equals("getConractDuration")) {
        String proposalId = null;
        String contractStartDate = null;
        String contractEndDate = null;
        String contractDuration = null;
        String periodType = null;

        if (jsonRequest.has("proposalId") && jsonRequest.has("contractStartDate")
            && jsonRequest.has("contractEndDate")) {
          proposalId = jsonRequest.getString("proposalId");
          contractStartDate = jsonRequest.getString("contractStartDate");

          contractEndDate = jsonRequest.getString("contractEndDate");
          contractDuration = jsonRequest.getString("contractDuration");
          periodType = jsonRequest.getString("periodType");
          if (contractStartDate != null && contractEndDate != null) {
            result = PurOrderSummaryDAO.getContractDurationdate(contractDuration, periodType,
                contractStartDate, contractEndDate, vars.getClient());

            years = Integer.parseInt(result.getString("years"));
            months = Integer.parseInt(result.getString("months"));
            days = Integer.parseInt(result.getString("days"));
            LOG.debug("years" + years);
            LOG.debug("months" + months);
            LOG.debug("days" + days);

            if (years == 0 && months > 0 && days == 0) {
              result.put("periodType", "MT");
              result.put("duration", months);
            } else if (months == 0 && years > 0 && days == 0) {
              mulyear = years * 12;
              result.put("periodType", "MT");
              result.put("duration", mulyear);
            } else if (months > 0 && years > 0 && days == 0) {
              months = years * 12 + months;
              result.put("periodType", "MT");
              result.put("duration", months);
            } else if (months == 0 && years == 0 && days == 0) {
              result.put("periodType", "D");
              result.put("duration", "1");
            }

            else if ((months >= 0 && years == 0 && (days >= 1 || days <= 1))
                || (months > 0 && years > 0 && days > 0)) {

              st = conn.prepareStatement(
                  " select count(distinct hijri_date) as total from eut_hijri_dates  where hijri_date > ? and hijri_date <= ?");
              st.setString(1, contractStartDate.split("-")[2] + contractStartDate.split("-")[1]
                  + contractStartDate.split("-")[0]);
              st.setString(2, contractEndDate.split("-")[2] + contractEndDate.split("-")[1]
                  + contractEndDate.split("-")[0]);
              // st.setString(3, inpclient);
              rs = st.executeQuery();
              if (rs.next()) {
                result.put("periodType", "DT");
                result.put("duration", (rs.getInt("total") + 1));
              }
            }
          }

        }
        return result;
      }

      else if (jsonRequest.getString("action").equals("getConractEndDateByContractDuration")) {

        String proposalId = null;
        String contractStartDate = null;
        String contractDuration = null;
        String periodType = null;

        if (jsonRequest.has("proposalId") && jsonRequest.has("contractStartDate")
            && jsonRequest.has("contractDuration") && jsonRequest.has("periodType")) {

          proposalId = jsonRequest.getString("proposalId");
          contractStartDate = jsonRequest.getString("contractStartDate");
          contractDuration = jsonRequest.getString("contractDuration");
          periodType = jsonRequest.getString("periodType");

          if (periodType.equals("MT") && StringUtils.isNotEmpty(contractStartDate)
              && StringUtils.isNotEmpty(contractDuration)) {
            String Contractdate = PurOrderSummaryDAO.getContractDurationMonth(contractDuration,
                periodType, contractStartDate, "", vars.getClient());
            result.put("contractEndDate", Contractdate);

          } else {
            String Contractday = PurOrderSummaryDAO.getContractDurationday(contractDuration,
                periodType, contractStartDate, "", vars.getClient());
            result.put("contractEndDate", Contractday);
          }

          return result;
        }

      }

      else if (jsonRequest.getString("action").equals("getConractDurationByStartDate")) {
        String proposalId = null;
        String contractStartDate = null;
        String contractEndDate = null;
        String contractDuration = null;
        String periodType = null;

        if (jsonRequest.has("proposalId") && jsonRequest.has("contractStartDate")
            && jsonRequest.has("contractEndDate")) {
          proposalId = jsonRequest.getString("proposalId");
          contractStartDate = jsonRequest.getString("contractStartDate");

          contractEndDate = jsonRequest.getString("contractEndDate");
          contractDuration = jsonRequest.getString("contractDuration");
          periodType = jsonRequest.getString("periodType");

          if (periodType.equals("MT") && StringUtils.isNotEmpty(contractStartDate)
              && StringUtils.isNotEmpty(contractDuration)) {
            String Contractdate = PurOrderSummaryDAO.getContractDurationMonth(contractDuration,
                periodType, contractStartDate, "", vars.getClient());

            result.put("contractEndDate", Contractdate);

          } else {
            String Contractday = PurOrderSummaryDAO.getContractDurationday(contractDuration,
                periodType, contractStartDate, contractEndDate, vars.getClient());
            result.put("contractEndDate", Contractday);
            // perioddayenddate = Contractday;
          }

          String gregDate = PurOrderSummaryDAO.getGregorianDate(contractStartDate);
          if (StringUtils.isNotEmpty(gregDate)) {
            result.put("gregDate", gregDate);
          }
          return result;
        }
      }

      else if (jsonRequest.getString("action").equals("getGregorianDate")) {
        String proposalId = null;
        String onBoardDateH = null;
        String gregDate = null;
        if (jsonRequest.has("onBoardDateh")) {
          proposalId = jsonRequest.getString("proposalId");
          onBoardDateH = jsonRequest.getString("onBoardDateh");
          gregDate = PurOrderSummaryDAO.getGregorianDate(onBoardDateH);
          if (StringUtils.isNotEmpty(gregDate)) {
            result.put("gregDate", gregDate);
          }
        }
        return result;
      }

      else if (jsonRequest.getString("action").equals("getPoAmountCalcPer")) {
        String proposalId = null;
        BigDecimal totalAmount = BigDecimal.ZERO;
        String percent = null;
        if (jsonRequest.has("advPaymentPer")
        // && jsonRequest.has("totalAmount")
        ) {
          proposalId = jsonRequest.getString("proposalId");
          EscmProposalMgmt proposalMgmt = OBDal.getInstance().get(EscmProposalMgmt.class,
              proposalId);
          percent = jsonRequest.getString("advPaymentPer");
          if (percent != null) {
            if (proposalMgmt.getProposalstatus().equals("PAWD")) {
              totalAmount = proposalMgmt.getAwardamount()
                  .multiply(new BigDecimal(percent).divide(new BigDecimal(100)));
            } else {
              totalAmount = proposalMgmt.getTotalamount()
                  .multiply(new BigDecimal(percent).divide(new BigDecimal(100)));
            }
            result.put("amount", totalAmount);
          }

        }
        return result;
      }

      else if (jsonRequest.getString("action").equals("getPoAmountCalcAmount")) {
        String proposalId = null;
        BigDecimal totalAmount = BigDecimal.ZERO;
        String amount = null;

        if (jsonRequest.has("advPaymentAmt")
        // && jsonRequest.has("totalAmount")
        ) {
          proposalId = jsonRequest.getString("proposalId");
          EscmProposalMgmt proposalMgmt = OBDal.getInstance().get(EscmProposalMgmt.class,
              proposalId);
          amount = jsonRequest.getString("advPaymentAmt");
          if (amount != null) {
            if (proposalMgmt.getProposalstatus().equals("PAWD")) {
              totalAmount = (new BigDecimal(amount).multiply(new BigDecimal(100)))
                  .divide(proposalMgmt.getAwardamount(), 2, RoundingMode.HALF_UP);
            } else {
              totalAmount = (new BigDecimal(amount).multiply(new BigDecimal(100)))
                  .divide(proposalMgmt.getTotalamount(), 2, RoundingMode.HALF_UP);
            }
            result.put("amount", totalAmount);
          }
        }
      }

      else if (jsonRequest.getString("action").equals("getPoType")) {
        String proposalId = null;
        String purchaseOrderType = "PUR";
        String proposalAttrId = null;
        List<EscmPurchaseOrderConfiguration> config = new ArrayList<EscmPurchaseOrderConfiguration>();
        if (jsonRequest.has("proposalId") && jsonRequest.has("totalAmount")
            && jsonRequest.has("orgId")) {
          proposalId = jsonRequest.getString("proposalId");
        }
        if (jsonRequest.has("proposalAttrId") && (jsonRequest.getString("proposalAttrId") != null
            && jsonRequest.getString("proposalAttrId") != "null")) {
          proposalAttrId = jsonRequest.getString("proposalAttrId");
          EscmProposalAttribute proposalAttr = OBDal.getInstance().get(EscmProposalAttribute.class,
              proposalAttrId);
          if (proposalAttr.getEscmProposalmgmt() != null)
            proposalId = proposalAttr.getEscmProposalmgmt().getId();

        }

        // Delete old attachment if any

        CreatePoAttachmentDAO.deleteAttachment(proposalId);

        EscmProposalMgmt proposalMgmt = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);

        config = proposalDAO.getPOTypeBasedOnValue(proposalMgmt.getOrganization().getId(),
            proposalMgmt.getTotalamount());
        if (config.size() > 0) {
          purchaseOrderType = config.get(0).getOrdertype();
        } else {
          EscmPurchaseOrderConfiguration configuration = PurchaseAgreementCalloutDAO
              .checkDocTypeConfigwithAmt(OBContext.getOBContext().getCurrentClient().getId(),
                  proposalMgmt.getOrganization().getId(), proposalMgmt.getTotalamount());
          if (configuration != null) {
            purchaseOrderType = configuration.getOrdertype();
          }
        }
        result.put("poType", purchaseOrderType);
        result.put("proposalType", proposalMgmt.getProposalType());
        return result;

      }

    } catch (

    Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in POChangeActionDurationCalc :", e);
      }
      e.printStackTrace();
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    }
    return result;
  }

}
