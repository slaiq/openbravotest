package sa.elm.ob.scm.ad_callouts;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.Escmbankguaranteedetail;
import sa.elm.ob.scm.ad_callouts.dao.BGWorkbenchDAO;
import sa.elm.ob.scm.ad_callouts.dao.PurOrderSummaryDAO;
import sa.elm.ob.scm.event.dao.BankGuaranteeDetailEventDAO;
import sa.elm.ob.utility.util.UtilityDAO;

public class BankGuaranteeWorkbenchCallout extends SimpleCallout {

  /**
   * Callout for Bank Guarantee Workbench
   */
  private static final long serialVersionUID = 1L;
  private static final Logger log = LoggerFactory.getLogger(BankGuaranteeWorkbenchCallout.class);

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    // declare the session variables
    String strbankGuaranteeId = info.vars.getStringParameter("inpescmBankguaranteeDetailId");
    String strExpireDate = info.vars.getStringParameter("inpexpirydateh");
    String strReqExpiryDate = info.vars.getStringParameter("inpreqexpiryDate");
    String strExPeriodDays = info.vars.getStringParameter("inpextperiodDays");
    String inpLastFieldChanged = info.vars.getStringParameter("inpLastFieldChanged");
    String inpclient = info.vars.getStringParameter("inpadClientId");
    String inpBGAmount = info.vars.getNumericParameter("inpbgamount");
    String inpreducedAmt = info.vars.getNumericParameter("inpreducedAmt");
    String inpreductionPercentage = info.vars.getNumericParameter("inpreductionPercentage");
    String inpinitialbg = info.vars.getNumericParameter("inpinitialbg");
    String inpdocumentAmount = info.vars.getNumericParameter("inpdocumentAmount");
    String inpbgstartdateh = info.vars.getStringParameter("inpbgstartdateh");
    String inpstartdategre = info.vars.getStringParameter("inpbgstartdategre");
    String inpescmBgExtensionId = info.vars.getStringParameter("inpescmBgExtensionId");
    String inpbankguaranteetype = info.vars.getStringParameter("inpbankguaranteetype");
    // String proposalattid = null;
    String bidid = null, strtodayDate = null, maxBGExtensionDate = null, bidTermValue = null,
        branchAddress = null;
    String inpexpirydategre = info.vars.getStringParameter("inpexpirydategre");
    String inpdocumentType = info.vars.getStringParameter("inpdocumentType");
    String inpdocumentNo = info.vars.getStringParameter("inpdocumentNo");
    String inpescmBgAmtrevisionId = info.vars.getStringParameter("inpescmBgAmtrevisionId");
    String inpescmProposalmgmtId = info.vars.getStringParameter("inpescmProposalmgmtId");
    String inpescmProposalAttrId = info.vars.getStringParameter("inpescmProposalAttrId");
    String inpbankBranch = info.vars.getStringParameter("inpbankBranch");
    String strBidID = info.vars.getStringParameter("inpescmBidmgmtId");
    String periodType = info.vars.getStringParameter("inpextperiodType");
    String perioddayenddate = "";
    String inpforeignBank = info.vars.getStringParameter("inpforeignBank");

    String inpcOrderId = info.vars.getStringParameter("inpcOrderId");
    BigDecimal revisedBGAmt = BigDecimal.ZERO, reducedAmt = BigDecimal.ZERO,
        reducedPercentage = BigDecimal.ZERO, bgAmt = BigDecimal.ZERO, netBGAmt = BigDecimal.ZERO,
        documentAmount = BigDecimal.ZERO;
    BigInteger inpexprireIn = BigInteger.ZERO;
    BigDecimal lineno = BigDecimal.ZERO;
    String bidId = null, bidName = "";

    int years, mulyear = 0;
    int months = 0;
    int days = 0;
    SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");

    JSONObject result = new JSONObject();
    JSONObject obj = new JSONObject();
    try {
      OBContext.setAdminMode();
      EscmProposalAttribute proposalat = null;
      log.debug("inpLastFieldChanged:" + inpLastFieldChanged);

      Escmbankguaranteedetail bg = OBDal.getInstance().get(Escmbankguaranteedetail.class,
          strbankGuaranteeId);

      // if bid is change then bring the bid details
      if (inpLastFieldChanged.equals("inpescmBidmgmtId")) {
        if (strBidID != null && !strBidID.equals("")) {
          EscmBidMgmt bidMgmt = OBDal.getInstance().get(EscmBidMgmt.class, strBidID);
          info.addResult("inpbidname", bidMgmt.getBidname());
        }
        if (strBidID == null || strBidID.equals("")) {
          info.addResult("inpbidname", "");
        }
      }

      if (inpLastFieldChanged.equals("inpadOrgId")) {
        // info.addResult("inpcontactname", BGWorkbenchDAO.getBgSpeciallistRole(info.vars.getUser(),
        // info.vars.getRole(), inpclient, inpadOrgId));
        String contactName = BGWorkbenchDAO.getDefaultContactName(info.vars.getUser());
        if (contactName != null) {
          info.addResult("inpcontactname", contactName);
        }

        // set document amt if orderid is changed
        if (inpcOrderId != null && !inpcOrderId.equals("null")
            && StringUtils.isNotEmpty(inpcOrderId)) {
          Order ord = OBDal.getInstance().get(Order.class, inpcOrderId);
          if (ord.getGrandTotalAmount() != null) {
            info.addResult("inpdocumentAmount", ord.getGrandTotalAmount());
          }
        }
        // set document amt if proposalid is changed
        if (inpescmProposalmgmtId != null && !inpescmProposalmgmtId.equals("null")
            && StringUtils.isNotEmpty(inpescmProposalmgmtId)
            && StringUtils.isNotEmpty(inpcOrderId)) {
          EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class,
              inpescmProposalmgmtId);
          if (proposal.getTotalamount() != null) {
            info.addResult("inpdocumentAmount", proposal.getTotalamount());
          }
        }

        // set document amt if proposal attribute is changed
        if (inpescmProposalAttrId != null && !inpescmProposalAttrId.equals("null")
            && StringUtils.isNotEmpty(inpescmProposalAttrId) && StringUtils.isNotEmpty(inpcOrderId)
            && StringUtils.isNotEmpty(inpescmProposalmgmtId)) {
          EscmProposalAttribute prosalatt = OBDal.getInstance().get(EscmProposalAttribute.class,
              inpescmProposalAttrId);
          if (prosalatt != null && prosalatt.getEscmProposalmgmt() != null
              && prosalatt.getEscmProposalmgmt().getTotalamount() != null) {
            info.addResult("inpdocumentAmount", prosalatt.getEscmProposalmgmt().getTotalamount());
          }
        }
      }

      if (inpLastFieldChanged.equals("inpcBpartnerId")) {
        if (inpdocumentNo != null && inpdocumentNo.equals("") && inpdocumentNo.equals("null"))
          info.addResult("inpcBpartnerLocationId",
              BGWorkbenchDAO.getbpartnerId(inpdocumentNo, inpdocumentType));
      }
      if (info.vars.getStringParameter("inpescmBidmgmtId") != null)
        bidid = info.vars.getStringParameter("inpescmBidmgmtId");

      // if (info.vars.getStringParameter("inpescmProposalAttrId") != null)
      // proposalattid = info.vars.getStringParameter("inpescmProposalAttrId");

      // convert the bg startdate hijiri_date to gregorian_date
      if (inpLastFieldChanged.equals("inpbgstartdateh")) {

        info.addResult("inpbgstartdategre", UtilityDAO.convertToGregorian_tochar(inpbgstartdateh));
        if (StringUtils.isNotBlank(strExpireDate) && StringUtils.isNotEmpty(strExpireDate)
            && StringUtils.isNotBlank(inpbgstartdateh) && StringUtils.isNotEmpty(inpbgstartdateh)) {
          inpexprireIn = BGWorkbenchDAO.getExtendPeriodDays(inpbgstartdateh, strExpireDate,
              inpclient, null);
          info.addResult("inpbgduration", inpexprireIn);

          if ("IBG".equals(inpbankguaranteetype)) {
            Boolean isGreater = BGWorkbenchDAO.getNoofDays(
                UtilityDAO.convertToGregorian_tochar(inpbgstartdateh), inpexpirydategre);

            if (!isGreater) {
              info.addResult("WARNING", OBMessageUtils.messageBD("ESCM_periodlessthan90"));
            }
          }
        }

      }

      // convert the bg startdate gregorian_date to hijiri_date
      if (inpLastFieldChanged.equals("inpbgstartdategre")) {
        DateFormat dateyearFormat = new SimpleDateFormat("dd-MM-yyyy");
        DateFormat dateyearForm = new SimpleDateFormat("yyyy-MM-dd");
        String startDateGre = inpstartdategre;
        try {
          inpstartdategre = dateyearForm.format(dateyearFormat.parse(inpstartdategre));

          info.addResult("inpbgstartdateh", UtilityDAO.convertToHijriDate(inpstartdategre));

          if (StringUtils.isNotEmpty(inpbankguaranteetype) && "IBG".equals(inpbankguaranteetype)) {
            Boolean isGreater = BGWorkbenchDAO.getNoofDays(startDateGre,
                dateyearFormat.format(dateyearFormat.parse(inpexpirydategre)));

            if (!isGreater) {
              info.addResult("WARNING", OBMessageUtils.messageBD("ESCM_periodlessthan90"));
            }
          }
        }

        catch (ParseException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }

      // convert the bg expiry startdate gregorian_date to hijiri_date
      if (inpLastFieldChanged.equals("inpexpirydategre")) {
        DateFormat dateyearFormat = new SimpleDateFormat("dd-MM-yyyy");
        DateFormat dateyearForm = new SimpleDateFormat("yyyy-MM-dd");
        String expiryDateGre = inpexpirydategre;
        try {
          inpexpirydategre = dateyearForm.format(dateyearFormat.parse(inpexpirydategre));
          info.addResult("inpexpirydateh", UtilityDAO.convertToHijriDate(inpexpirydategre));

          if (StringUtils.isNotEmpty(inpbankguaranteetype) && "IBG".equals(inpbankguaranteetype)) {
            Boolean isGreater = BGWorkbenchDAO.getNoofDays(
                dateyearFormat.format(dateyearFormat.parse(inpstartdategre)), expiryDateGre);

            if (!isGreater) {
              info.addResult("WARNING", OBMessageUtils.messageBD("ESCM_periodlessthan90"));
            }
          }
        } catch (ParseException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }

      // calculate the Requested Expiry Date based on BG Expiry date and Requested Extend Period
      // Days
      if (inpLastFieldChanged.equals("inpextperiodDays")
          || inpLastFieldChanged.equals("inpextperiodType")) {
        String reqExpirydate = null;
        Date expDate = null;
        if (bg.getExtendExpdateh() != null) {
          expDate = bg.getExtendExpdateh();
        } else {
          expDate = bg.getExpirydateh();
        }
        maxBGExtensionDate = BGWorkbenchDAO.getmaximumbgextensiondate(strbankGuaranteeId,
            inpescmBgExtensionId);
        if (maxBGExtensionDate == null)
          maxBGExtensionDate = UtilityDAO.convertTohijriDate(dateYearFormat.format(expDate));
        // Task No.7624
        if (StringUtils.isNotEmpty(strExPeriodDays) && StringUtils.isNotEmpty(maxBGExtensionDate)
            && !strExPeriodDays.equals("0")) {
          if (periodType.equals("MT")) {
            reqExpirydate = PurOrderSummaryDAO.getContractDurationMonth(strExPeriodDays, periodType,
                maxBGExtensionDate, "", inpclient);

            info.addResult("inpreqexpiryDate", reqExpirydate);
            perioddayenddate = reqExpirydate;
          } else {
            reqExpirydate = BGWorkbenchDAO.getRequestedExpirydate(strExpireDate, strExPeriodDays,
                inpclient, maxBGExtensionDate);
            info.addResult("inpreqexpiryDate", reqExpirydate);
            perioddayenddate = reqExpirydate;
          }
        } else {
          info.addResult("inpreqexpiryDate", "");
        }
      }

      // calculate the Requested Extend Period Days based on BG Expiry date and Requested Expiry
      // Date
      if (inpLastFieldChanged.equals("inpreqexpiryDate")) {
        maxBGExtensionDate = BGWorkbenchDAO.getmaximumbgextensiondate(strbankGuaranteeId,
            inpescmBgExtensionId);
        if (maxBGExtensionDate == null)
          maxBGExtensionDate = UtilityDAO
              .convertTohijriDate(dateYearFormat.format(bg.getExtendExpdateh()));
        // Task No.7624
        if (StringUtils.isNotEmpty(maxBGExtensionDate) && StringUtils.isNotEmpty(strExPeriodDays)) {
          if (!perioddayenddate.equals(strReqExpiryDate)) {
            result = PurOrderSummaryDAO.getContractDurationdate(strExPeriodDays, periodType,
                maxBGExtensionDate, strReqExpiryDate, inpclient);

            years = Integer.parseInt(result.getString("years"));
            months = Integer.parseInt(result.getString("months"));
            days = Integer.parseInt(result.getString("days"));
            log4j.debug("years" + years);
            log4j.debug("months" + months);
            log4j.debug("days" + days);
            if (years == 0 && months > 0 && days == 0) {
              info.addResult("inpextperiodType", "MT");
              info.addResult("inpextperiodDays", months);
            } else if (months == 0 && years > 0 && days == 0) {
              mulyear = years * 12;
              info.addResult("inpextperiodType", "MT");
              info.addResult("inpextperiodDays", mulyear);
            } else if (months > 0 && years > 0 && days == 0) {
              months = years * 12 + months;
              info.addResult("inpextperiodType", "MT");
              info.addResult("inpextperiodDays", months);
            } else if (months == 0 && years == 0 && days == 0) {
              info.addResult("inpextperiodType", "D");
              info.addResult("inpextperiodDays", "1");
            } else if ((months >= 0 && years == 0 && (days >= 1 || days <= 1))
                || (months > 0 && years > 0 && days > 0)
                || (months == 0 && years > 0 && days > 0)) {
              String sql = "select count(distinct hijri_date) as total from eut_hijri_dates  where hijri_date > ? and hijri_date <= ? ";
              SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(sql);
              query.setParameter(0, maxBGExtensionDate.split("-")[2]
                  + maxBGExtensionDate.split("-")[1] + maxBGExtensionDate.split("-")[0]);
              query.setParameter(1, strReqExpiryDate.split("-")[2] + strReqExpiryDate.split("-")[1]
                  + strReqExpiryDate.split("-")[0]);
              if (query.list().size() > 0) {
                Object row = (Object) query.list().get(0);
                BigInteger total = (BigInteger) row;
                info.addResult("inpextperiodType", "DT");
                info.addResult("inpextperiodDays", total);
              }

            }
          }

        } // else {
        // BigInteger reqExpirydays = BGWorkbenchDAO.getExtendPeriodDays(strExpireDate,
        // strReqExpiryDate, inpclient, maxBGExtensionDate);
        // info.addResult("inpextperiodDays", reqExpirydays);
        // }
      }

      // calculate the Reduction Percentage& Net BG Amount based on Reduced Amt & BG Amount
      if (inpLastFieldChanged.equals("inpreducedAmt")) {
        reducedAmt = new BigDecimal(inpreducedAmt);
        bgAmt = new BigDecimal(inpBGAmount);
        revisedBGAmt = BGWorkbenchDAO.getRevisedAmt(strbankGuaranteeId, inpescmBgAmtrevisionId);

        if (revisedBGAmt.compareTo(BigDecimal.ZERO) == 0) {
          revisedBGAmt = bgAmt;
        }
        reducedPercentage = ((reducedAmt.multiply(new BigDecimal(100))).divide(revisedBGAmt, 3,
            RoundingMode.DOWN));
        netBGAmt = revisedBGAmt.subtract(reducedAmt);
        info.addResult("inpreductionPercentage", reducedPercentage);
        if (netBGAmt.compareTo(BigDecimal.ZERO) >= 0)
          info.addResult("inpnetBgamt", netBGAmt);
        else
          info.addResult("inpnetBgamt", "0.00");
      }

      // calculate the Reduced Amt& Net BG Amount based on Reduction Percentage & BG Amount
      if (inpLastFieldChanged.equals("inpreductionPercentage")) {
        reducedPercentage = new BigDecimal(inpreductionPercentage);
        bgAmt = new BigDecimal(inpBGAmount);
        revisedBGAmt = BGWorkbenchDAO.getRevisedAmt(strbankGuaranteeId, inpescmBgAmtrevisionId);
        if (revisedBGAmt.compareTo(BigDecimal.ZERO) == 0) {
          revisedBGAmt = bgAmt;
        }

        reducedAmt = revisedBGAmt.multiply(reducedPercentage.divide(new BigDecimal(100)));
        netBGAmt = revisedBGAmt.subtract(reducedAmt);
        info.addResult("inpreducedAmt", reducedAmt);
        if (netBGAmt.compareTo(BigDecimal.ZERO) >= 0)
          info.addResult("inpnetBgamt", netBGAmt);
        else
          info.addResult("inpnetBgamt", "0.00");
      }

      // get the bg Rate based on BGType
      if (inpLastFieldChanged.equals("inpbankguaranteetype")
          || inpLastFieldChanged.equals("inpescmBidmgmtId")) {
        bidTermValue = BGWorkbenchDAO.getbidTermsValue(bidid, inpbankguaranteetype, inpclient);
        info.addResult("inpinitialbg", bidTermValue);
      }

      // based on document no , bring document amount, multi banks and currency.
      if (inpLastFieldChanged.equals("inpdocumentNo")
          || inpLastFieldChanged.equals("inpbankname")) {
        if (inpdocumentNo != null && StringUtils.isNotEmpty(inpdocumentNo)
            && !inpdocumentNo.equals("null")) {
          obj = BGWorkbenchDAO.getDocDetails(inpdocumentNo, inpdocumentType);

          try {
            if (obj.has("linetotal"))
              documentAmount = new BigDecimal(obj.getString("linetotal"));
            else
              documentAmount = new BigDecimal(0.00);
            if (obj.has("bidId")) {
              bidId = obj.getString("bidId");
              bidName = obj.getString("bidName");
            } else {
              bidId = null;
            }

            info.addResult("inpdocumentAmount", documentAmount);
            info.addResult("inpescmBidmgmtId", bidId);
            info.addResult("inpcBpartnerId", obj.getString("SupplierId"));
            info.addResult("inpcCurrencyId", obj.getString("currencyId"));
            info.addResult("inpbidname", bidName);
            info.addResult("inpcBpartnerLocationId",
                BGWorkbenchDAO.getbpartnerId(inpdocumentNo, inpdocumentType));

            // get line no
            lineno = BGWorkbenchDAO.getLineno(inpdocumentNo);
            info.addResult("inpline", lineno);

            if (inpdocumentType.equals("P")) {
              proposalat = BankGuaranteeDetailEventDAO.getProposalAttribute(inpdocumentNo);
              info.addResult("inpescmProposalmgmtId", inpdocumentNo);
              if (proposalat != null)
                info.addResult("inpescmProposalAttrId", proposalat.getId());
            } else if (inpdocumentType.equals("POC")) {

              Order ord = OBDal.getInstance().get(Order.class, inpdocumentNo);
              if (ord.getEscmProposalmgmt() != null) {
                EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class,
                    ord.getEscmProposalmgmt().getId());
                proposalat = BankGuaranteeDetailEventDAO.getProposalAttribute(proposal.getId());

                info.addResult("inpescmProposalmgmtId", proposal.getId());
                if (proposalat != null)
                  info.addResult("inpescmProposalAttrId", proposalat.getId());
              }
              info.addResult("inpcOrderId", inpdocumentNo);

            }
            if (bidId != null) {
              bidTermValue = BGWorkbenchDAO.getbidTermsValue(bidId, inpbankguaranteetype,
                  inpclient);
              info.addResult("inpinitialbg", bidTermValue);
            }
          } catch (JSONException e) {
            log.error("Exception while get the Document Details in BG callout:", e);
            throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
          }
        } else {
          info.addResult("inpescmBidmgmtId", null);
        } /*
           * else info.addResult("inpdocumentAmount", "0.00");
           */
      }

      // update bank address if branch changes.
      if (inpLastFieldChanged.equals("inpbankBranch")) {
        branchAddress = BGWorkbenchDAO.getBranchAddress(inpbankBranch);
        info.addResult("inpbankAddress", branchAddress);
      }

      // calculate bg amount
      if (inpLastFieldChanged.equals("inpbankPercentage")
          || inpLastFieldChanged.equals("inpinitialbg")) {
        documentAmount = new BigDecimal(inpdocumentAmount);
        if (inpLastFieldChanged.equals("inpinitialbg")) {
          if (inpbankguaranteetype.equals("FBG") || inpbankguaranteetype.equals("DPG")) {
            BigDecimal initialbgper = new BigDecimal(inpinitialbg).setScale(2,
                RoundingMode.HALF_UP);
            inpinitialbg = initialbgper.toString();
            info.addResult("inpinitialbg", inpinitialbg);
          }
        }
      }

      // calculate the Expire in based on BG Expiry date and Extend Expiry Date
      if (inpLastFieldChanged.equals("inpexpirydateh")) {
        // convert the bg expiry date hijiri_date to gregorian_date
        strtodayDate = UtilityDAO.convertTohijriDate(dateYearFormat.format(new Date()));
        info.addResult("inpexpirydategre", UtilityDAO.convertToGregorian_tochar(strExpireDate));
        inpexprireIn = BGWorkbenchDAO.getExtendPeriodDays(strtodayDate, strExpireDate, inpclient,
            null);
        info.addResult("inpexprireIn", inpexprireIn);
        inpexprireIn = BGWorkbenchDAO.getExtendPeriodDays(inpbgstartdateh, strExpireDate, inpclient,
            null);
        info.addResult("inpbgduration", inpexprireIn);

        if (StringUtils.isNotEmpty(inpbankguaranteetype) && "IBG".equals(inpbankguaranteetype)) {
          Boolean isGreater = BGWorkbenchDAO.getNoofDays(inpstartdategre,
              UtilityDAO.convertToGregorian_tochar(strExpireDate));

          if (!isGreater) {
            info.addResult("WARNING", OBMessageUtils.messageBD("ESCM_periodlessthan90"));
          }
        }

      }
      // if foreign bank is unchecked that time foreign bank as null
      if (inpLastFieldChanged.equals("inpforeignBank")) {
        if (inpforeignBank.equals("N")) {
          info.addResult("inpforeignBankName", null);
        }
      }

    } catch (Exception e) {
      log.error("Exception in BankGuaranteeWorkbenchCallout:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
