package sa.elm.ob.finance.actionHandler;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.service.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

public class EfinEncumbarnceRevision extends BaseProcessActionHandler {

  private static final Logger log = LoggerFactory.getLogger(EfinEncumbarnceRevision.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    try {
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      // getting Revision Date
      String Revisiondate = jsonparams.getString("Efin_Revison_Date");
      String manuencumId = null, message = "";
      EfinBudgetManencum manEncumbrance = null;

      JSONObject encumlines = jsonparams.getJSONObject("Efin_Encumbrance_Revision");
      JSONArray selectedlines = encumlines.getJSONArray("_selection");

      boolean errorflag = false;
      SimpleDateFormat YearFormat = new SimpleDateFormat("yyyy");
      SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy-MM-dd");
      SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date revDate = null;
      try {
        revDate = JsonUtils.createDateFormat().parse(Revisiondate);
      } catch (ParseException e) {
        log.error("Exception : " + e);
      }

      // Future Date is not allowed in Revision Date
      Date revisionDate = yearFormat.parse(Revisiondate);
      String strrevDate = Utility.convertToGregorian(dateFormat.format(revisionDate));
      Date revGregDate = yearFormat.parse(strrevDate);

      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));

      if (revGregDate.compareTo(todaydate) > 0) {
        JSONObject errormsg = new JSONObject();
        errormsg.put("severity", "error");
        errormsg.put("text",
            OBMessageUtils.parseTranslation("@Efin_FutureDate_NotAllowed_InRevisionDate@"));
        jsonResponse.put("message", errormsg);
        errorflag = true;
        return jsonResponse;
      }

      String revyear = YearFormat.format(revDate);

      // getting selected records in Revision Pop up.
      for (int i = 0; i < selectedlines.length(); i++) {
        JSONObject selectedRow = selectedlines.getJSONObject(i);
        String revAmount = selectedRow.getString("refamount");
        String decrease = selectedRow.getString("rEVDecrease");
        BigDecimal amount = BigDecimal.ZERO;

        if (new BigDecimal(decrease).compareTo(BigDecimal.ZERO) == 0
            && new BigDecimal(revAmount).compareTo(BigDecimal.ZERO) == 0) {
          JSONObject errormsg = new JSONObject();
          errormsg.put("severity", "error");
          errormsg.put("text", OBMessageUtils.parseTranslation("@Efin_Man_Encum_Zero@"));
          jsonResponse.put("message", errormsg);
          errorflag = true;
          return jsonResponse;
        }

        if (new BigDecimal(decrease).compareTo(BigDecimal.ZERO) > 0) {
          amount = new BigDecimal(decrease).negate();
        } else {
          amount = new BigDecimal(revAmount);
        }
        manuencumId = selectedRow.getString("manualEncumbrance");
        String manencumlineId = selectedRow.getString("id");

        manEncumbrance = OBDal.getInstance().get(EfinBudgetManencum.class, manuencumId);

        Date ActDate = manEncumbrance.getAccountingDate();

        SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
        String hijridate = UtilityDAO.convertTohijriDate(dateYearFormat.format(ActDate));
        String[] hijridte = hijridate.split("-");
        int hijyr = Integer.parseInt(hijridte[2]);
        // checking revision year and account year
        if (Integer.parseInt(revyear) > hijyr || Integer.parseInt(revyear) < hijyr) {
          errorflag = true;
        }

        errorflag = preValidation(manencumlineId, ActDate, amount, manEncumbrance);
      }
      if (errorflag) {
        JSONObject errormsg = new JSONObject();
        errormsg.put("severity", "error");
        errormsg.put("text", OBMessageUtils.parseTranslation("@Efin_Man_Encum_Failed@"));
        jsonResponse.put("message", errormsg);
        errorflag = true;
        return jsonResponse;

      }

      if (!errorflag) {

        for (int i = 0; i < selectedlines.length(); i++) {
          JSONObject selectedRow = selectedlines.getJSONObject(i);
          manuencumId = selectedRow.getString("manualEncumbrance");
          String revAmount = selectedRow.getString("refamount");
          String decrease = selectedRow.getString("rEVDecrease");
          BigDecimal amount = BigDecimal.ZERO;
          if (new BigDecimal(decrease).compareTo(BigDecimal.ZERO) > 0) {
            amount = new BigDecimal(decrease).negate();
          } else {
            amount = new BigDecimal(revAmount);
          }
          String manencumlineId = selectedRow.getString("id");
          String description = (selectedRow.getString("linedesc").equals("null") ? ""
              : selectedRow.getString("linedesc"));
          manEncumbrance = OBDal.getInstance().get(EfinBudgetManencum.class, manuencumId);

          // update the manualEncumbrance lines
          EfinBudgetManencumlines encumbranceline = OBDal.getInstance()
              .get(EfinBudgetManencumlines.class, manencumlineId);
          /*
           * Trigger changes encumbranceline.setUpdated(new java.util.Date());
           * encumbranceline.setUpdatedBy(OBContext.getOBContext().getUser());
           * encumbranceline.setRevamount(encumbranceline.getRevamount().add(amount));
           * encumbranceline.setRemainingAmount(
           * encumbranceline.getOriginalamount().subtract(encumbranceline.getUsedAmount()));
           * encumbranceline.setOriginalamount(encumbranceline.getOriginalamount().add(amount));
           * log.debug("encumbranceline:" + encumbranceline.toString());
           */

          // Remaining amount updation in header.
          EfinBudgetManencum header = OBDal.getInstance().get(EfinBudgetManencum.class,
              encumbranceline.getManualEncumbrance().getId());
          header.setRemainingamt(header.getRemainingamt().add(amount));
          OBDal.getInstance().save(encumbranceline);
          OBDal.getInstance().save(header);

          EfinBudManencumRev manEncumRev = OBProvider.getInstance().get(EfinBudManencumRev.class);

          if (!StringUtils.isEmpty(encumbranceline.getId())) {
            // insert into Manual Encumbrance Revision Table
            manEncumRev.setClient(OBContext.getOBContext().getCurrentClient());
            manEncumRev.setOrganization(OBDal.getInstance().get(Organization.class,
                encumbranceline.getOrganization().getId()));
            manEncumRev.setActive(true);
            manEncumRev.setUpdatedBy(OBContext.getOBContext().getUser());
            manEncumRev.setCreationDate(new java.util.Date());
            manEncumRev.setCreatedBy(OBContext.getOBContext().getUser());
            manEncumRev.setUpdated(new java.util.Date());
            manEncumRev.setUniqueCode(encumbranceline.getUniquecode());
            manEncumRev.setManualEncumbranceLines(
                OBDal.getInstance().get(EfinBudgetManencumlines.class, encumbranceline.getId()));

            DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
            String s = df.format(revDate);
            String gregDate = Utility.convertToGregorian(s);
            Date revDate1 = null;
            try {
              revDate1 = JsonUtils.createDateFormat().parse(gregDate);
            } catch (Exception e) {
              log.debug("Date is not converted properly");
            }

            manEncumRev.setRevdate(revDate1);
            manEncumRev.setDescription(description);
            manEncumRev.setStatus("APP");
            manEncumRev.setRevamount(amount);
            manEncumRev.setEncumbranceType("MO");
            manEncumRev.setAccountingCombination(encumbranceline.getAccountingCombination());
            OBDal.getInstance().save(manEncumRev);
          }

          // update into budget inquiry encumbrance value
          // Trigger changes updateBudgetInquiry(encumbranceline, manEncumbrance, amount);
        }
      }

      if (!errorflag) {

        OBDal.getInstance().flush();
        OBDal.getInstance().commitAndClose();
        JSONObject errormsg = new JSONObject();
        message = OBMessageUtils.parseTranslation("@Efin_Man_Rev_Success@");
        errormsg.put("severity", "success");
        errormsg.put("text", message);
        jsonResponse.put("message", errormsg);
        return jsonResponse;
      }

    } catch (

    JSONException e) {
      log.debug("Exception handling in Encumbrance Revision", e);
    } catch (ParseException e) {
      log.debug("Exception handling in Encumbrance Revision", e);
    }
    return jsonResponse;
  }

  /**
   * Checks if updating revision amount exceeds the funds available or lesser than remaining amount.
   * 
   * @param selected
   *          encumbrancelineId, accounting date , updating revision amount
   * @return 1 if updating revision amount satisfy the all condition otherwise return 0.
   * @throws ParseException
   */
  public static boolean preValidation(String ManEncumLineId, Date ActDate, BigDecimal revamt,
      EfinBudgetManencum encumbrance) throws ParseException {

    // Pre-validation on funds available
    String message = "";
    boolean errorflag = false;
    try {
      EfinBudgetManencumlines encumlines = OBDal.getInstance().get(EfinBudgetManencumlines.class,
          ManEncumLineId);

      BigDecimal dbRemAmt = encumlines.getRevamount().subtract(encumlines.getAPPAmt())
          .subtract(encumlines.getUsedAmount());

      if ((revamt.compareTo(BigDecimal.ZERO) < 0) && (revamt.negate().compareTo(dbRemAmt) > 0)) {
        log.debug("Updating The Funds in Line");
        message = OBMessageUtils.parseTranslation("@Efin_ManEncu_Rev_LessRemAmt@");
        message = message.replace("%", dbRemAmt.toString());
        encumlines.setCheckingStatus("FL");
        encumlines.setFailureReason(message);
        errorflag = true;
        OBDal.getInstance().save(encumlines);
        OBDal.getInstance().flush();
      }

      if (revamt.compareTo(BigDecimal.ZERO) > 0) {
        errorflag = chkFundsAvailforRevInc(encumlines, encumbrance, revamt);
      }

      if (!errorflag) {
        log.debug("Updating The Success");
        encumlines.setCheckingStatus("SCS");
        encumlines.setFailureReason(null);
        OBDal.getInstance().save(encumlines);
        OBDal.getInstance().flush();
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in PreValidation Of Encumbarance", e.getMessage());
      return errorflag;
    }
    return errorflag;

  }

  public static void updateBudgetInquiry(EfinBudgetManencumlines encumLine,
      EfinBudgetManencum manEncumbarance, BigDecimal revamount) {
    String department = "";
    EfinBudgetInquiry allDept = null;
    OBQuery<EfinBudgetInquiry> budInq = null;
    try {
      budInq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
          "efinBudgetint.id= :budgetintID and accountingCombination.account.id= :accountID ");
      budInq.setNamedParameter("budgetintID", manEncumbarance.getBudgetInitialization().getId());
      budInq.setNamedParameter("accountID",
          encumLine.getAccountingCombination().getAccount().getId());

      if (encumLine.getAccountingCombination().isEFINDepartmentFund()) {
        if (budInq.list() != null && budInq.list().size() > 0) {
          for (EfinBudgetInquiry Enquiry : budInq.list()) {
            if (encumLine.getAccountingCombination() == Enquiry.getAccountingCombination()) {
              Enquiry.setEncumbrance(Enquiry.getEncumbrance().add(revamount));
              OBDal.getInstance().save(Enquiry);
              if (Enquiry.getParent() != null) {
                Enquiry.getParent()
                    .setEncumbrance(Enquiry.getParent().getEncumbrance().add(revamount));
                allDept = Enquiry.getParent();
                OBDal.getInstance().save(Enquiry);
              }
              if (allDept.getParent() != null) {
                allDept.getParent()
                    .setEncumbrance(allDept.getParent().getEncumbrance().add(revamount));
                OBDal.getInstance().save(allDept);
              }
            }
          }
        }
      } else {
        OBQuery<EfinBudgetControlParam> bcp = OBDal.getInstance()
            .createQuery(EfinBudgetControlParam.class, " as e where e.client.id= :clientID");
        bcp.setNamedParameter("clientID", manEncumbarance.getClient().getId());
        if (bcp.list() != null && bcp.list().size() > 0) {
          department = bcp.list().get(0).getBudgetcontrolCostcenter().getId();

          OBQuery<AccountingCombination> accountCombination = OBDal.getInstance().createQuery(
              AccountingCombination.class,
              "account.id= :accID and businessPartner.id= :businesspartnerID "
                  + " and salesRegion.id= :salesRegionId and project.id = :projectID and salesCampaign.id= :salesCampaignID "
                  + " and activity.id= :activityID and stDimension.id= :stDimenisonID and ndDimension.id = :ndDimenisonID "
                  + " and organization.id = :orgID ");
          accountCombination.setNamedParameter("accID", encumLine.getAccountElement().getId());
          accountCombination.setNamedParameter("businesspartnerID",
              encumLine.getAccountingCombination().getBusinessPartner().getId());
          accountCombination.setNamedParameter("salesRegionId", department);
          accountCombination.setNamedParameter("projectID",
              encumLine.getAccountingCombination().getProject().getId());
          accountCombination.setNamedParameter("salesCampaignID",
              encumLine.getAccountingCombination().getSalesCampaign().getId());
          accountCombination.setNamedParameter("activityID",
              encumLine.getAccountingCombination().getActivity().getId());
          accountCombination.setNamedParameter("stDimenisonID",
              encumLine.getAccountingCombination().getStDimension().getId());
          accountCombination.setNamedParameter("ndDimenisonID",
              encumLine.getAccountingCombination().getNdDimension().getId());
          accountCombination.setNamedParameter("orgID",
              encumLine.getAccountingCombination().getOrganization().getId());
          if (accountCombination.list() != null && accountCombination.list().size() > 0) {
            AccountingCombination combination = accountCombination.list().get(0);

            budInq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
                "efinBudgetint.id= :efinBudgetinID and accountingCombination.account.id= :accID ");
            budInq.setNamedParameter("efinBudgetinID",
                manEncumbarance.getBudgetInitialization().getId());
            budInq.setNamedParameter("accID",
                encumLine.getAccountingCombination().getAccount().getId());
            if (budInq.list() != null && budInq.list().size() > 0) {
              for (EfinBudgetInquiry Enquiry : budInq.list()) {
                if (combination.getId().equals(Enquiry.getAccountingCombination().getId())) {
                  Enquiry.setEncumbrance(Enquiry.getEncumbrance().add(revamount));
                  OBDal.getInstance().save(Enquiry);
                  if (Enquiry.getParent() != null) {
                    Enquiry.getParent()
                        .setEncumbrance(Enquiry.getParent().getEncumbrance().add(revamount));
                    OBDal.getInstance().save(Enquiry);
                  }
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception inupdateBudgetInquiry", e.getMessage());
    }
  }

  public static boolean chkFundsAvailforRevInc(EfinBudgetManencumlines encumLine,
      EfinBudgetManencum manEncumbarance, BigDecimal revamount) throws ParseException {
    boolean errorflag = false;
    String department = "", message = null;
    try {
      if (revamount.compareTo(BigDecimal.ZERO) > 0) {
        OBQuery<EfinBudgetInquiry> budInq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
            "efinBudgetint.id='" + manEncumbarance.getBudgetInitialization().getId()
                + "'  and accountingCombination.account.id='"
                + encumLine.getAccountingCombination().getAccount().getId() + "'");
        // if isdepartment fund yes, then check dept level distribution acct.
        if (encumLine.getAccountingCombination().isEFINDepartmentFund()) {
          if (budInq.list() != null && budInq.list().size() > 0) {
            for (EfinBudgetInquiry Enquiry : budInq.list()) {
              if (encumLine.getAccountingCombination() == Enquiry.getAccountingCombination()) {
                if (revamount.compareTo(Enquiry.getFundsAvailable()) > 0) {
                  // funds not available
                  errorflag = true;
                  encumLine.setCheckingStatus("FL");
                  message = OBMessageUtils.messageBD("Efin_ManEncu_Rev_GreatFunds");
                  message = message.replace("%", Enquiry.getFundsAvailable().toString());
                  encumLine.setFailureReason(message);
                  OBDal.getInstance().save(encumLine);
                }
              }
            }
          } else {
            errorflag = true;
            encumLine.setCheckingStatus("FL");
            message = OBMessageUtils.messageBD("Efin_ManEncu_Rev_GreatFunds");
            message = message.replace("%", "0");
            encumLine.setFailureReason(message);
            OBDal.getInstance().save(encumLine);
          }
        }

        // if isdepartment fund No, then check Org level distribution acct.
        else {
          OBQuery<EfinBudgetControlParam> bcp = OBDal.getInstance()
              .createQuery(EfinBudgetControlParam.class, "");
          if (bcp.list() != null && bcp.list().size() > 0) {
            department = bcp.list().get(0).getBudgetcontrolCostcenter().getId();
            // getorg level uniquecode
            OBQuery<AccountingCombination> accountCombination = OBDal.getInstance().createQuery(
                AccountingCombination.class,
                "account.id= '" + encumLine.getAccountElement().getId() + "'"
                    + " and businessPartner.id='"
                    + encumLine.getAccountingCombination().getBusinessPartner().getId() + "' "
                    + "and salesRegion.id='" + department + "' and project.id = '"
                    + encumLine.getAccountingCombination().getProject().getId() + "' "
                    + "and salesCampaign.id='"
                    + encumLine.getAccountingCombination().getSalesCampaign().getId() + "' "
                    + "and activity.id='"
                    + encumLine.getAccountingCombination().getActivity().getId()
                    + "' and stDimension.id='"
                    + encumLine.getAccountingCombination().getStDimension().getId() + "' "
                    + "and ndDimension.id = '"
                    + encumLine.getAccountingCombination().getNdDimension().getId() + "' "
                    + "and organization.id = '"
                    + encumLine.getAccountingCombination().getOrganization().getId() + "'");

            if (accountCombination.list() != null && accountCombination.list().size() > 0) {
              AccountingCombination combination = accountCombination.list().get(0);

              if (budInq.list() != null && budInq.list().size() > 0) {
                for (EfinBudgetInquiry Enquiry : budInq.list()) {
                  if (combination.getId().equals(Enquiry.getAccountingCombination().getId())) {
                    if (revamount.compareTo(Enquiry.getFundsAvailable()) > 0) {
                      // funds not available
                      errorflag = true;
                      encumLine.setCheckingStatus("FL");
                      message = OBMessageUtils.messageBD("Efin_ManEncu_Rev_GreatFunds");
                      message = message.replace("%", Enquiry.getFundsAvailable().toString());
                      encumLine.setFailureReason(message);
                      OBDal.getInstance().save(encumLine);
                    }
                  }
                }
              }
            } else {
              errorflag = true;
              encumLine.setCheckingStatus("FL");
              message = OBMessageUtils.messageBD("Efin_ManEncu_Rev_GreatFunds");
              message = message.replace("%", "0");
              encumLine.setFailureReason(message);
              OBDal.getInstance().save(encumLine);
            }

          }
        }
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception chkFundsAvailforRevInc", e.getMessage());
      return errorflag;
    }
    return errorflag;
  }
}
