package sa.elm.ob.scm.ad_callouts;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;

import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.ad_callouts.dao.RequisitionHeaderCalloutDAO;
import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.ad_callouts.dao.PurOrderSummaryDAO;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author qualian
 *
 */

public class PurorderContractSummaryCallout extends SimpleCallout {

  private static Logger log = Logger.getLogger(PurorderContractSummaryCallout.class);

  /**
   * Callout for C_order table
   */

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpescmProposalmgmtId = vars.getStringParameter("inpemEscmProposalmgmtId");
    PreparedStatement st = null;
    ResultSet rs = null;
    String inpbidno = vars.getStringParameter("inpemEscmBidmgmtId");
    String contractduration = vars.getStringParameter("inpemEscmContractduration").replaceAll(",",
        "");
    String periodtype = vars.getStringParameter("inpemEscmPeriodtype");
    String inpstartdate = vars.getStringParameter("inpemEscmContractstartdate");
    String inpenddate = vars.getStringParameter("inpemEscmContractenddate");
    String inpclient = vars.getStringParameter("inpadClientId");
    String inpcity = vars.getStringParameter("inpemEscmCCityId");
    String onboarddateh = vars.getStringParameter("inpemEscmOnboarddateh");
    String encumId = vars.getStringParameter("inpemEfinBudgetManencumId");
    String inpadClientId = vars.getStringParameter("inpadClientId");
    String inpadRoleId = vars.getRole();
    String inpdateordered = vars.getStringParameter("inpdateordered");
    Connection conn = OBDal.getInstance().getConnection();
    String perioddayenddate = "";
    String gregDate = "";
    JSONObject result = new JSONObject();
    int years, mulyear = 0;
    int months = 0;
    int days = 0;
    Date startdate = null, enddate = null;
    String strSecondSupplier = vars.getStringParameter("inpemEscmSecondsupplier");
    String jscode = "";
    String isselectedIban = vars.getStringParameter("inpemEscmIssecondsupplier");
    String isadvpayment = vars.getStringParameter("inpemEscmIsadvancepayment");
    String inpContactType = vars.getStringParameter("inpemEscmContactType");

    try {

      OBContext.setAdminMode();

      if (StringUtils.isNotEmpty(inpstartdate) && StringUtils.isNotEmpty(inpenddate)) {
        startdate = new SimpleDateFormat("dd-MM-yyyy").parse(inpstartdate);
        enddate = new SimpleDateFormat("dd-MM-yyyy").parse(inpenddate);
        if (startdate.after(enddate)) {
          info.addResult("ERROR", OBMessageUtils.messageBD("Escm_ContractEnddate"));
          return;
        }
      }

      // Fetch the Advance Payment % & amount from contract category configuration
      if (inpLastFieldChanged.equals("inpemEscmIsadvancepayment")) {
        if (isadvpayment.equals("Y")) {
          ESCMDefLookupsTypeLn reflookuplnObj = OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
              inpContactType);
          info.addResult("inpemEscmAdvpaymntPercntge",
              reflookuplnObj.getAdvancePaymentPer() != null ? reflookuplnObj.getAdvancePaymentPer()
                  : BigDecimal.ZERO);
        } else {
          info.addResult("inpemEscmAdvpaymntPercntge", BigDecimal.ZERO);
        }
      }

      // To get the suppier date for corresponding proposal
      if (inpLastFieldChanged.equals("inpemEscmProposalmgmtId")) {
        String Date = PurOrderSummaryDAO.getSupplierDate(inpescmProposalmgmtId);
        info.addResult("inpemEscmProposaldate", Date);

      }

      // To get the bidname date for corresponding bidnumber
      if (inpLastFieldChanged.equals("inpemEscmBidmgmtId")) {
        String Bidno = PurOrderSummaryDAO.getBidname(inpbidno);
        info.addResult("inpemEscmProjectname", Bidno);

      }

      if (inpLastFieldChanged.equals("inpemEscmContractstartdate")) {
        info.addResult("inpemEscmOnboarddateh", inpstartdate);
        if (StringUtils.isNotEmpty(inpstartdate)) {
          gregDate = PurOrderSummaryDAO.getGregorianDate(inpstartdate);
          if (StringUtils.isNotEmpty(gregDate)) {
            info.addResult("inpemEscmOnboarddategreg", gregDate);
          }
        } else {
          info.addResult("inpemEscmOnboarddategreg", null);
        }
      }
      // if the periodtype as month or day then calculate the contract enddate
      if (inpLastFieldChanged.equals("inpemEscmContractduration")
          || inpLastFieldChanged.equals("inpemEscmPeriodtype")
          || inpLastFieldChanged.equals("inpemEscmContractstartdate")) {

        if (periodtype.equals("MT") && StringUtils.isNotEmpty(inpstartdate)
            && StringUtils.isNotEmpty(contractduration)) {
          String Contractdate = PurOrderSummaryDAO.getContractDurationMonth(contractduration,
              periodtype, inpstartdate, "", inpclient);

          info.addResult("inpemEscmContractenddate", Contractdate);
        } else {
          String Contractday = PurOrderSummaryDAO.getContractDurationday(contractduration,
              periodtype, inpstartdate, inpenddate, inpclient);
          info.addResult("inpemEscmContractenddate", Contractday);
          perioddayenddate = Contractday;
        }

      }
      // To get the contractduration for corresponding contract enddate

      if (inpLastFieldChanged.equals("inpemEscmContractenddate")) {
        if (!perioddayenddate.equals(inpenddate)) {
          result = PurOrderSummaryDAO.getContractDurationdate(contractduration, periodtype,
              inpstartdate, inpenddate, inpclient);

          years = Integer.parseInt(result.getString("years"));
          months = Integer.parseInt(result.getString("months"));
          days = Integer.parseInt(result.getString("days"));
          log4j.debug("years" + years);
          log4j.debug("months" + months);
          log4j.debug("days" + days);
          if (years == 0 && months > 0 && days == 0) {
            info.addResult("inpemEscmPeriodtype", "MT");
            info.addResult("inpemEscmContractduration", months);
          } else if (months == 0 && years > 0 && days == 0) {
            mulyear = years * 12;
            info.addResult("inpemEscmPeriodtype", "MT");
            info.addResult("inpemEscmContractduration", mulyear);
          } else if (months > 0 && years > 0 && days == 0) {
            months = years * 12 + months;
            info.addResult("inpemEscmPeriodtype", "MT");
            info.addResult("inpemEscmContractduration", months);
          } else if (months == 0 && years == 0 && days == 0) {
            info.addResult("inpemEscmPeriodtype", "D");
            info.addResult("inpemEscmContractduration", "1");
          } else if ((months >= 0 && years == 0 && (days >= 1 || days <= 1))
              || (months > 0 && years > 0 && days > 0)) {

            st = conn.prepareStatement(
                " select count(distinct hijri_date) as total from eut_hijri_dates  where hijri_date > ? and hijri_date <= ?");
            st.setString(1, inpstartdate.split("-")[2] + inpstartdate.split("-")[1]
                + inpstartdate.split("-")[0]);
            st.setString(2,
                inpenddate.split("-")[2] + inpenddate.split("-")[1] + inpenddate.split("-")[0]);
            // st.setString(3, inpclient);
            rs = st.executeQuery();
            if (rs.next()) {
              info.addResult("inpemEscmPeriodtype", "DT");
              info.addResult("inpemEscmContractduration", (rs.getInt("total") + 1));

            }

          }
        }
      }
      if (inpLastFieldChanged.equals("inpemEscmCCityId")) {
        if (inpcity != null && !inpcity.equals("")) {
          String regionId = PurOrderSummaryDAO.getRegion(inpcity);
          if (regionId != null) {
            info.addResult("inpemEscmCRegionId", regionId);
          }
        } else {
          info.addResult("inpemEscmCRegionId", null);
        }
      }

      // To get On Board Gregorian date for corresponding On Board hijri date
      if (inpLastFieldChanged.equals("inpemEscmOnboarddateh")) {
        if (StringUtils.isNotEmpty(onboarddateh)) {
          gregDate = PurOrderSummaryDAO.getGregorianDate(onboarddateh);
          if (StringUtils.isNotEmpty(gregDate)) {
            info.addResult("inpemEscmOnboarddategreg", gregDate);
          }
        } else {
          info.addResult("inpemEscmOnboarddategreg", null);
        }

      }
      // set uniquecode while changing encumbrance
      if (inpLastFieldChanged.equals("inpemEfinBudgetManencumId")) {
        // getting budget initial id based on transaction date
        if (encumId != null && !encumId.equals("")) {
          EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class, encumId);
          info.addResult("inpemEscmManualEncumNo", encum.getDocumentNo());
          String uniqueCode = RequisitionHeaderCalloutDAO.getUniqueCode(encumId, inpadClientId,
              inpadRoleId);
          if (uniqueCode != null) {
            jscode = "form.getFieldFromColumnName('EM_Efin_C_Validcombination_ID').setValue('"
                + uniqueCode + "');";
            jscode += "form.doChangeFICCall('EM_Efin_C_Validcombination_ID');";
            info.addResult("JSEXECUTE", jscode);
          } else
            info.addResult("JSEXECUTE",
                "form.getFieldFromColumnName('EM_Efin_C_Validcombination_ID').setValue('')");
        } else {
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Efin_C_Validcombination_ID').setValue('')");
        }
      }
      // set Budget Definition while changing Financial Year
      if (inpLastFieldChanged.equals("inpemEscmFinanyear")) {
        if (inpdateordered != null) {
          EfinBudgetIntialization budInit = UtilityDAO.getBudgetInitial(inpdateordered,
              inpadClientId);
          if (budInit != null) {
            info.addResult("inpemEfinBudgetintId", budInit.getId());
          } else {
            info.addResult("inpemEfinBudgetintId", null);
          }
        }
      }
      // set the subcontractor field while change the joint venture supplier
      if (inpLastFieldChanged.equals("inpemEscmSecondsupplier")) {
        BusinessPartner second_supplier = OBDal.getInstance().get(BusinessPartner.class,
            strSecondSupplier);
        if (second_supplier != null) {
          info.addResult("inpemEscmSubcontractors", second_supplier.getName());
          // set the second branch name
          OBQuery<Location> bpLocation = OBDal.getInstance().createQuery(Location.class,
              "as e where e.businessPartner.id =:supplier");
          bpLocation.setNamedParameter("supplier", strSecondSupplier);
          if (bpLocation != null && bpLocation.list().size() > 0) {
            jscode = "form.getFieldFromColumnName('EM_Escm_Second_Branchname').setValue('"
                + bpLocation.list().get(0).getId() + "');";
            jscode += "form.doChangeFICCall('EM_Escm_Second_Branchname');";
            info.addResult("JSEXECUTE", jscode);
          }
        } else {
          info.addResult("inpemEscmSubcontractors", null);
          info.addResult("inpemEscmIssecondsupplier", false);
        }

      }
      // if second supplier iban as false then set the iban as empty
      if (inpLastFieldChanged.equals("inpemEscmIssecondsupplier")) {
        if (isselectedIban.equals("N")) {
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Escm_Second_Iban').setValue('')");
        }
      }

    } catch (Exception e) {
      log.error("Exception in PurorderContractSummaryCallout:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      // close connection
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {
        log4j.error("Exception while closing the statement in PurorderContractSummaryCallout ", e);
      }
      OBContext.restorePreviousMode();
    }
  }
}
