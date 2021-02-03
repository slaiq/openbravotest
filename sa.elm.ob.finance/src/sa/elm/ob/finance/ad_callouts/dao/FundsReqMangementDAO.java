package sa.elm.ob.finance.ad_callouts.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.calendar.Year;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.finance.EFINFundsReqLine;
import sa.elm.ob.finance.EfinBudgetControlParam;

/**
 * @author Divya on 15/07/2017
 */

public class FundsReqMangementDAO {
  /**
   * This Access Layer class is responsible to do database operation in Funds Request Management
   */
  VariablesSecureApp vars = null;

  private final static Logger log = LoggerFactory.getLogger(FundsReqMangementDAO.class);

  /**
   * This method is used to get control param
   * 
   * @param clientId
   * @return
   */
  public static EfinBudgetControlParam getControlParam(String clientId) {
    EfinBudgetControlParam controlPrmObj = null;
    List<EfinBudgetControlParam> controlParamList = new ArrayList<EfinBudgetControlParam>();
    try {
      OBContext.setAdminMode();

      OBQuery<EfinBudgetControlParam> controlParamObj = OBDal.getInstance()
          .createQuery(EfinBudgetControlParam.class, " as e where e.client.id= :clientID ");
      controlParamObj.setNamedParameter("clientID", clientId);
      controlParamObj.setMaxResult(1);
      controlParamList = controlParamObj.list();
      if (controlParamList.size() > 0) {
        controlPrmObj = controlParamList.get(0);
        return controlPrmObj;
      }
      return controlPrmObj;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in getAgencyHQOrg " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return controlPrmObj;
  }

  /**
   * This method is used to get budget year
   * 
   * @param fundsReq
   * @return
   */
  public static Year getBudgetYear(EFINFundsReq fundsReq) {
    final List<Object> parameters = new ArrayList<Object>();
    Year yearObj = null;
    List<Year> yearList = new ArrayList<Year>();

    try {
      OBContext.setAdminMode();
      OBQuery<Year> yearQueryObj = OBDal.getInstance().createQuery(Year.class,
          " as e where e.id in ( "
              + " select per.year.id from FinancialMgmtPeriod per where per.client.id= ? and to_date(?,'yyyy-MM-dd')   "
              + " between to_date(to_char(per.startingDate,'yyyy-MM-dd'),'yyyy-MM-dd')     and to_date(to_char(per.endingDate,'yyyy-MM-dd'),'yyyy-MM-dd') )"
              + " and e.client.id=?");
      parameters.add(fundsReq.getClient().getId());
      parameters.add(fundsReq.getAccountingDate());
      parameters.add(fundsReq.getClient().getId());
      yearQueryObj.setParameters(parameters);
      log.debug("getParameters:" + yearQueryObj.getParameters());
      log.debug("calendarObj:" + yearQueryObj.getWhereAndOrderBy());
      log.debug("list:" + yearQueryObj.list().size());
      if (yearQueryObj.list().size() > 0) {
        yearList = yearQueryObj.list();
        yearObj = yearList.get(0);
        return yearObj;
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in getBudgetYear " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return yearObj;
  }

  /**
   * This method is used to get year ID
   * 
   * @param date
   * @param conn
   * @param clientId
   * @return
   */
  public static String getYearId(Date date, Connection conn, String clientId) {
    PreparedStatement ps = null;
    ResultSet rs = null;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String yearId = null;
    try {
      ps = conn.prepareStatement("select c_year_id from c_period where to_date('"
          + dateFormat.format(date) + "','yyyy-MM-dd')"
          + " between to_date(to_char(startdate,'dd-MM-yyyy'),'dd-MM-yyyy') and to_date(to_char(enddate,'dd-MM-yyyy'),'dd-MM-yyyy') "
          + "  and ad_client_id ='" + clientId + "'");
      log.debug("ps:" + ps.toString());

      rs = ps.executeQuery();
      if (rs.next()) {
        yearId = rs.getString("c_year_id");
        log.debug("yearId:" + yearId);
      }
    } catch (Exception e) {
      log.error("Exception in getYearId " + e.getMessage());
    } finally {
      // close connection
      try {
        if (rs != null) {
          rs.close();
        }
        if (ps != null) {
          ps.close();
        }
      } catch (Exception e) {
        log.error("Excepion in closing connection" + e);
      }
    }
    return yearId;
  }

  /**
   * This method is used to get budget line details
   * 
   * @param acctCombId
   * @param fundsReqId
   * @param conn
   * @return
   */
  public static JSONObject getBudgetLineDetail(String acctCombId, String fundsReqId,
      Connection conn) {
    Year yearObj = null;
    String strQuery = null;
    JSONObject result = new JSONObject();
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      OBContext.setAdminMode();
      EFINFundsReq fundsReq = OBDal.getInstance().get(EFINFundsReq.class, fundsReqId);
      yearObj = getBudgetYear(fundsReq);
      if (yearObj != null) {

        strQuery = " select bug.ad_org_id as bugorgid ,bug.efin_budget_id,bug.c_elementvalue_id,ln.funds_available, "
            + " ln.c_validcombination_id,ln.current_budget ,ln.uniquecode from efin_budget bug "
            + " join efin_budgetlines ln on ln.efin_budget_id=bug.efin_budget_id  "
            + " where bug.c_campaign_id = ? and bug.c_year_id  =? and bug.ad_client_id = ? "
            + " and bug.c_elementvalue_id in (select  replace(unnest(string_to_array "
            + " (eut_getparentacct((select account_id from c_validcombination "
            + " where c_validcombination_id=?),null),',')::character varying []),'''','')) "
            + " and ln.c_validcombination_id=? ";
        ps = conn.prepareStatement(strQuery);
        ps.setString(1, fundsReq.getSalesCampaign().getId());
        ps.setString(2, fundsReq.getYear().getId());
        ps.setString(3, fundsReq.getClient().getId());
        ps.setString(4, acctCombId);
        ps.setString(5, acctCombId);
        log.debug("getBudgetLineDetail:" + ps.toString());
        rs = ps.executeQuery();
        if (rs.next()) {
          result.put("fundsavailable", rs.getString("funds_available"));
          result.put("currentBudget", rs.getString("current_budget"));
          result.put("c_elementvalue_id", rs.getString("c_elementvalue_id"));
          result.put("c_validcombination_id", rs.getString("c_validcombination_id"));
          result.put("efin_budget_id", rs.getString("efin_budget_id"));

        }
        return result;
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in getBudgetLineDetail " + e.getMessage());
    } finally {
      // close connection
      try {
        if (rs != null) {
          rs.close();
        }
        if (ps != null) {
          ps.close();
        }
        OBContext.restorePreviousMode();
      } catch (Exception e) {
        log.error("Exception in closing connection :" + e);
      }
    }
    return result;
  }

  /**
   * This method is used to get budget Inquiry line detail
   * 
   * @param acctCombId
   * @param fundsReqId
   * @param conn
   * @return
   */
  public static JSONObject getBudgetInquiryLineDetail(String acctCombId, String fundsReqId,
      Connection conn) {
    String strQuery = null;
    JSONObject result = new JSONObject();
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      OBContext.setAdminMode();
      EFINFundsReq fundsReq = OBDal.getInstance().get(EFINFundsReq.class, fundsReqId);

      strQuery = "    select buginq.ad_org_id as bugorgid ,buginq.c_elementvalue_id,buginq.funds_available, "
          + " buginq.c_validcombination_id,buginq.current_budget ,buginq.uniquecode   "
          + " from efin_budgetinquiry  buginq "
          + " join efin_budgetint budinit on budinit.efin_budgetint_id=buginq.efin_budgetint_id "
          + " where budinit.efin_budgetint_id = ?  "
          + " and budinit.ad_client_id = ? and buginq.c_validcombination_id= ? ";
      ps = conn.prepareStatement(strQuery);
      ps.setString(1, fundsReq.getEfinBudgetint().getId());
      ps.setString(2, fundsReq.getClient().getId());
      ps.setString(3, acctCombId);
      log.debug("getBudgetLineDetail:" + ps.toString());
      rs = ps.executeQuery();
      if (rs.next()) {
        result.put("fundsavailable", rs.getString("funds_available"));
        result.put("currentBudget", rs.getString("current_budget"));
        result.put("c_elementvalue_id", rs.getString("c_elementvalue_id"));
        result.put("c_validcombination_id", rs.getString("c_validcombination_id"));
      }
      return result;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in getBudgetLineDetail " + e.getMessage());
    } finally {
      // close connection
      try {
        if (rs != null) {
          rs.close();
        }
        if (ps != null) {
          ps.close();
        }
        OBContext.restorePreviousMode();
      } catch (Exception e) {
        log.error("Exception in closing connection :" + e);
      }
    }
    return result;
  }

  /**
   * This method is used to check From account present or not
   * 
   * @param fundsReqId
   * @param toAcctComObj
   * @return
   */
  public static EFINFundsReqLine chkFrmAcctPrestorNot(String fundsReqId,
      AccountingCombination toAcctComObj) {

    EFINFundsReqLine fromAcctreqLine = null;
    final List<Object> parameters = new ArrayList<Object>();
    List<EFINFundsReqLine> reqlnList = new ArrayList<EFINFundsReqLine>();
    try {
      OBContext.setAdminMode();
      if (toAcctComObj != null) {
        OBQuery<EFINFundsReqLine> reqlineQry = OBDal.getInstance()
            .createQuery(EFINFundsReqLine.class, " as req  where req.fromaccount.id "
                + " in ( select e.id from  FinancialMgmtAccountingCombination e where e.account.id=? "
                + " and e.salesCampaign.id= ? and e.activity.id=? and e.stDimension.id= ? and e.ndDimension.id=? and e.businessPartner.id= ? "
                + " and e.project.id= ?  and e.client.id = ? ) and req.efinFundsreq.id= ?  and req.distType='DIST'");
        parameters.add(toAcctComObj.getAccount().getId());
        parameters.add(toAcctComObj.getSalesCampaign().getId());
        parameters.add(toAcctComObj.getActivity().getId());
        parameters.add(toAcctComObj.getStDimension().getId());
        parameters.add(toAcctComObj.getNdDimension().getId());
        parameters.add(toAcctComObj.getBusinessPartner().getId());
        parameters.add(toAcctComObj.getProject().getId());
        parameters.add(toAcctComObj.getClient().getId());
        parameters.add(fundsReqId);

        reqlineQry.setParameters(parameters);
        reqlineQry.setMaxResult(1);
        reqlnList = reqlineQry.list();
        log.debug("getParameters:" + reqlineQry.getParameters());
        log.debug("calendarObj:" + reqlineQry.getWhereAndOrderBy());
        log.debug("list:" + reqlnList.size());
        if (reqlnList.size() > 0) {
          fromAcctreqLine = reqlnList.get(0);
          return fromAcctreqLine;
        }
      }
      return fromAcctreqLine;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in chkFrmAcctPrestorNot " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return fromAcctreqLine;
  }

  /**
   * This method is evaluate From account equals To account
   * 
   * @param fromAcctObj
   * @param toAcctComObj
   * @return
   */
  public static Boolean chkFrmAcctEquToAcct(AccountingCombination fromAcctObj,
      AccountingCombination toAcctComObj) {
    final List<Object> parameters = new ArrayList<Object>();
    List<AccountingCombination> combList = new ArrayList<AccountingCombination>();
    try {
      OBContext.setAdminMode();
      if (toAcctComObj != null) {
        OBQuery<AccountingCombination> combQry = OBDal.getInstance()
            .createQuery(AccountingCombination.class, " as comb  where comb.id "
                + " in ( select e.id from  FinancialMgmtAccountingCombination e where e.account.id=? "
                + " and e.salesCampaign.id= ? and e.activity.id=? and e.stDimension.id= ? and e.ndDimension.id=? and e.businessPartner.id= ? "
                + " and e.project.id= ?  and e.client.id = ? ) and comb.id=? ");
        parameters.add(fromAcctObj.getAccount().getId());
        parameters.add(fromAcctObj.getSalesCampaign().getId());
        parameters.add(fromAcctObj.getActivity().getId());
        parameters.add(fromAcctObj.getStDimension().getId());
        parameters.add(fromAcctObj.getNdDimension().getId());
        parameters.add(fromAcctObj.getBusinessPartner().getId());
        parameters.add(fromAcctObj.getProject().getId());
        parameters.add(fromAcctObj.getClient().getId());
        parameters.add(toAcctComObj.getId());
        combQry.setParameters(parameters);
        combQry.setMaxResult(1);
        combList = combQry.list();
        log.debug("getParameters:" + combQry.getParameters());
        log.debug("calendarObj:" + combQry.getWhereAndOrderBy());
        log.debug("list:" + combList.size());
        if (combList.size() > 0) {
          return true;
        } else
          return false;
      }
      return false;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in chkFrmAcctEquToAcct " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return true;
  }

  /**
   * Get all To account entered in line corresponding to from account
   * 
   * @param fundsReqId
   * @param toAcctComObj
   * @return
   */

  public static List<EFINFundsReqLine> getToAccountBasedOnFromAcct(String fundsReqId,
      AccountingCombination toAcctComObj) {

    final List<Object> parameters = new ArrayList<Object>();
    List<EFINFundsReqLine> reqlnList = new ArrayList<EFINFundsReqLine>();
    try {
      OBContext.setAdminMode();
      if (toAcctComObj != null) {
        OBQuery<EFINFundsReqLine> reqlineQry = OBDal.getInstance()
            .createQuery(EFINFundsReqLine.class, " as req  where req.toaccount.id "
                + " in ( select e.id from  FinancialMgmtAccountingCombination e where e.account.id=? "
                + " and e.salesCampaign.id= ? and e.activity.id=? and e.stDimension.id= ? and e.ndDimension.id=? and e.businessPartner.id= ? "
                + " and e.project.id= ?  and e.client.id = ? ) and req.efinFundsreq.id= ?  and req.distType='DIST'");
        parameters.add(toAcctComObj.getAccount().getId());
        parameters.add(toAcctComObj.getSalesCampaign().getId());
        parameters.add(toAcctComObj.getActivity().getId());
        parameters.add(toAcctComObj.getStDimension().getId());
        parameters.add(toAcctComObj.getNdDimension().getId());
        parameters.add(toAcctComObj.getBusinessPartner().getId());
        parameters.add(toAcctComObj.getProject().getId());
        parameters.add(toAcctComObj.getClient().getId());
        parameters.add(fundsReqId);

        reqlineQry.setParameters(parameters);
        reqlnList = reqlineQry.list();
        log.debug("reqlineQry:" + reqlineQry);
        if (reqlnList.size() > 0) {
          return reqlnList;
        }
      }
      return reqlnList;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in getToAccountBasedOnFromAcct " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return reqlnList;
  }

  /**
   * To check same "from account" exists for same FRM account in previous lines
   * 
   * @param fundsRequestId
   * @param accountId
   * @return true - same account exists for same FRM document
   */
  public static Boolean checkSameAccountExistsForSameFRM(String accountId, String fundsRequestId) {

    final List<Object> parameters = new ArrayList<Object>();
    try {
      OBQuery<EFINFundsReqLine> obquery = OBDal.getInstance().createQuery(EFINFundsReqLine.class,
          "as e where  e.fromaccount is not null "
              + "and e.fromaccount.id=? and e.efinFundsreq.id=? ");
      parameters.add(accountId);
      parameters.add(fundsRequestId);
      obquery.setParameters(parameters);
      if (obquery.list().size() > 0) {
        return true;
      }

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in checkSameAccountExistsForSameFRM" + e.getMessage());
    }
    return false;
  }
}
