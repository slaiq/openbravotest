package sa.elm.ob.finance.util.DAO;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.beanutils.BeanComparator;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.marketing.Campaign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.BudgetAdjustment;
import sa.elm.ob.finance.BudgetAdjustmentLine;
import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.finance.EFINFundsReqLine;
import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinBudgetTransfertrx;
import sa.elm.ob.finance.EfinBudgetTransfertrxline;
import sa.elm.ob.finance.ad_callouts.dao.FundsReqMangementDAO;
import sa.elm.ob.finance.util.budget.BudgetingUtilsService;
import sa.elm.ob.finance.util.budget.BudgetingUtilsServiceImpl;

/**
 * @author Priyanka Ranjan on 30/09/2017
 */

// Common method for Budget Revision,Budget Adjustment and Fund Requests Management

public class CommonValidationsDAO {

  private static final Logger log = LoggerFactory.getLogger(CommonValidationsDAO.class);
  private static String bcuOrgId = null;
  private static String costOrgId = null;

  /**
   * 
   * @param headerId
   * @param clientId
   * @param transactionType
   * @return count
   */
  public static int checkValidations(String headerId, String clientId, String transactionType,
      boolean isWarn) {
    int count = 0;
    Connection con = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      OBContext.setAdminMode();
      String query = "";
      con = OBDal.getInstance().getConnection();
      EfinBudgetControlParam budgetCntrl = null;
      EFINFundsReq req = null;
      // check the budget control parameters isready flag =true
      final OBQuery<EfinBudgetControlParam> controlParam = OBDal.getInstance().createQuery(
          EfinBudgetControlParam.class,
          "client.id='" + OBContext.getOBContext().getCurrentClient().getId() + "'");

      if (controlParam.list().size() > 0) {

        budgetCntrl = controlParam.list().get(0);
        if (budgetCntrl != null) {
          bcuOrgId = budgetCntrl.getBudgetcontrolunit().getId();
          costOrgId = budgetCntrl.getBudgetcontrolCostcenter().getId();
        }

      }

      if (transactionType != null) {

        if (transactionType.equals("BD")) {
          req = OBDal.getInstance().get(EFINFundsReq.class, headerId);
        }

        query = "select typ.em_efin_budgettype ,hd.c_campaign_id  as currentcamid , hd.efin_budgetint_id as budgetintid,"
            + "(select c_campaign_id from efin_budgetinquiry  where efin_budgetint_id =  hd. efin_budgetint_id "
            + "and c_campaign_id <> hd.c_campaign_id limit 1)as inqcamid  ";

        if (transactionType.equals("BR"))
          query += " from efin_budget_transfertrx hd ";
        if (transactionType.equals("BADJ"))
          query += " from efin_budgetadj hd ";
        if (transactionType.equals("BD"))
          query += " from efin_fundsreq hd ";

        query += "join c_campaign typ on typ.c_campaign_id=hd.c_campaign_id  where hd.ad_client_id = ? ";

        if (transactionType.equals("BR"))
          query += " and hd.efin_budget_transfertrx_id= ?  ";
        if (transactionType.equals("BADJ"))
          query += " and hd.efin_budgetadj_id= ?  ";
        if (transactionType.equals("BD"))
          query += " and hd.efin_fundsreq_id= ?  ";

        ps = con.prepareStatement(query);
        ps.setString(1, clientId);
        ps.setString(2, headerId);
        rs = ps.executeQuery();
        if (rs.next()) {
          if (rs.getString("em_efin_budgettype").equals("C")) {
            if (!isWarn) {
              if ((transactionType.equals("BD") && !req.isReserve())
                  || transactionType.equals("BADJ"))
                // check decrease amount exceed the cost budget funds available
                count = isCostDecreaseAmountExceedCostBudgetFundsAvail(headerId, clientId,
                    rs.getString("budgetintid"), transactionType, rs.getString("currentcamid"));
            }

            // Checking Cost Budget lines Decrease amount exceed from Funds Budget Current Budget
            // amount
            if (!transactionType.equals("BD")) {
              count += isCostBudgetDecreaseAmountExceedFundsBudget(headerId, clientId,
                  rs.getString("budgetintid"), transactionType, rs.getString("inqcamid"));
            }
          } else {

            // Checking distribution is done for cost budget account
            /*
             * if (transactionType.equals("BD")) { count =
             * isDistributionCostBudgetAvailable(headerId, clientId, rs.getString("budgetintid"),
             * transactionType, rs.getString("inqcamid"), con); } else { count =
             * isDistributionBudAdjBudRevCostBudgetAvailable(headerId, clientId,
             * rs.getString("budgetintid"), transactionType, rs.getString("inqcamid"), con,
             * budgetCntrl); }
             */
            if (!transactionType.equals("BD")) {
              // Checking Funds Budget lines amount exceed from Cost Budget lines amount
              count += isFundAmountExceedFromCostBudgetAmount(headerId, clientId,
                  rs.getString("budgetintid"), transactionType, rs.getString("inqcamid"));
            }
            if ((transactionType.equals("BD") && !req.isReserve())) {
              // checking funds budget decrease amount exceeds funds available
              if (!isWarn)
                count += isBudgetDecreaseAmountExceedsFundsAvail(headerId, clientId,
                    rs.getString("budgetintid"), transactionType, rs.getString("currentcamid"));
              log.debug("finalcount:" + count);
            }

          }
        }
      }
      return count;

    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  checkValidations " + e, e);
      }
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();

    }
    OBDal.getInstance().flush();
    return count;
  }

  /**
   * Check Reactivate funds validation for encumbrance.
   * 
   * @param headerId
   * @param clientId
   * @param transactionType
   * @return
   */
  public static int checkEncumReactivateValidations(String headerId, String clientId,
      String transactionType) {
    int count = 0;
    try {
      OBContext.setAdminMode();
      EfinBudgetManencum manEncumbarance = OBDal.getInstance().get(EfinBudgetManencum.class,
          headerId);
      // iterate each line in encumbrance
      for (EfinBudgetManencumlines encumLine : manEncumbarance.getEfinBudgetManencumlinesList()) {
        if (encumLine.getAPPAmt().compareTo(BigDecimal.ZERO) > 0) {
          count = 1;
        }
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  checkEncumReactivateValidations " + e, e);
      }
    }
    return count;
  }

  /**
   * Check funds validation for encumbrance.
   * 
   * @param headerId
   * @param clientId
   * @param transactionType
   * @return
   */
  @SuppressWarnings("unchecked")
  public static int checkEncumValidations(String headerId, String clientId,
      String transactionType) {
    int count = 0;
    boolean acctMatch = false;
    String department = "";
    List<EfinBudgetInquiry> budInqList = null;
    AccountingCombination temporyComb = null;
    BigDecimal remaing_FundsAva = BigDecimal.ZERO;

    try {
      OBContext.setAdminMode();
      EfinBudgetManencum manEncumbarance = OBDal.getInstance().get(EfinBudgetManencum.class,
          headerId);
      BeanComparator fieldComparator = new BeanComparator("accountElement.id");
      if (fieldComparator != null) {
        Collections.sort(manEncumbarance.getEfinBudgetManencumlinesList(), fieldComparator);
      }
      // iterate each line in encumbrance
      for (EfinBudgetManencumlines encumLine : manEncumbarance.getEfinBudgetManencumlinesList()) {
        acctMatch = false;
        OBQuery<EfinBudgetInquiry> budInq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
            "efinBudgetint.id='" + manEncumbarance.getBudgetInitialization().getId() + "'");
        // if isdepartment fund yes, then check dept level distribution acct.
        if (encumLine.getAccountingCombination().isEFINDepartmentFund()) {
          if (budInq.list() != null && budInq.list().size() > 0) {
            budInqList = budInq.list();
            for (EfinBudgetInquiry Enquiry : budInqList) {
              if (encumLine.getAccountingCombination() == Enquiry.getAccountingCombination()) {
                if (encumLine.getAmount().compareTo(Enquiry.getFundsAvailable()) > 0) {
                  // funds not available
                  encumLine.setCheckingStatus("FL");
                  encumLine.setFailureReason(OBMessageUtils.messageBD("Efin_Encum_Amt_Error"));
                  OBDal.getInstance().save(encumLine);
                  count = 1;
                }
                acctMatch = true;
              }
            }
            if (!acctMatch) {
              count = 1;
              encumLine.setCheckingStatus("FL");
              encumLine.setFailureReason(OBMessageUtils.messageBD("Efin_NoDist_Dept"));
              OBDal.getInstance().save(encumLine);
            }
          } else {
            count = 1;
            encumLine.setCheckingStatus("FL");
            encumLine.setFailureReason(OBMessageUtils.messageBD("Efin_NoDist_Dept"));
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
                budInqList = budInq.list();
                for (EfinBudgetInquiry Enquiry : budInqList) {

                  if (combination == Enquiry.getAccountingCombination()) {
                    if (temporyComb == null || !temporyComb.equals(combination)) {
                      temporyComb = combination;
                      remaing_FundsAva = Enquiry.getFundsAvailable();
                    }
                    if (encumLine.getAmount().compareTo(remaing_FundsAva) > 0) {
                      // funds not available
                      encumLine.setCheckingStatus("FL");
                      encumLine.setFailureReason(OBMessageUtils.messageBD("Efin_Encum_Amt_Error"));
                      OBDal.getInstance().save(encumLine);
                      count = 1;
                    } else {
                      remaing_FundsAva = remaing_FundsAva.subtract(encumLine.getAmount());
                    }
                    acctMatch = true;
                  }
                }
                if (!acctMatch) {
                  encumLine.setCheckingStatus("FL");
                  encumLine.setFailureReason(OBMessageUtils.messageBD("Efin_NoDist_Org"));
                  OBDal.getInstance().save(encumLine);
                  count = 1;
                }
              } else {
                encumLine.setCheckingStatus("FL");
                encumLine.setFailureReason(OBMessageUtils.messageBD("Efin_NoDist_Org"));
                OBDal.getInstance().save(encumLine);
                count = 1;
              }
            } else {
              encumLine.setCheckingStatus("FL");
              encumLine.setFailureReason(OBMessageUtils.messageBD("Efin_NoDist_Org"));
              OBDal.getInstance().save(encumLine);
              count = 1;
            }
          }
        }
      }

    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  checkEncumValidations " + e, e);
      }
    }
    return count;
  }

  /**
   * 
   * @param fundBudRevId
   * @param clientId
   * @param budInt
   * @param transactionType
   * @param tocampId
   * @return count
   */
  public static int isFundAmountExceedFromCostBudgetAmount(String fundBudRevId, String clientId,
      String budInt, String transactionType, String tocampId) {

    String query = "", lineid = null, hdtable = null, lntable = null, headerid = null;
    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    int count = 0;
    BudgetingUtilsService budUtil = new BudgetingUtilsServiceImpl();
    try {
      OBContext.setAdminMode();
      if (transactionType.equals("BR")) {
        lineid = " efin_budget_transfertrxline_id ";
        headerid = " efin_budget_transfertrx_id ";
        hdtable = " efin_budget_transfertrx";
        lntable = " efin_budget_transfertrxline ";
      }
      if (transactionType.equals("BADJ")) {
        lineid = " efin_budgetadjline_id ";
        headerid = " efin_budgetadj_id ";
        hdtable = " efin_budgetadj";
        lntable = " efin_budgetadjline ";
      }
      if (transactionType.equals("BD")) {
        lineid = " efin_fundsreqline_id ";
        headerid = " efin_fundsreq_id ";
        hdtable = " efin_fundsreq ";
        lntable = " efin_fundsreqline ";

      }

      query = " select ln." + lineid + " as lineid, hd." + headerid + " as headerid,"
          + " (cost.current_budget - coalesce(coalesce(cost.legacy_spent,0)+coalesce(cost.grp_spent,0), 0)) as costcbug, coalesce(ln.increase,0) as fundsincrease, cost.disdec_amt as costdisdec, cost.disinc_amt as costdisin,"
          + " funds.current_budget as fundscbug, funds.disdec_amt as fundsdisdec, funds.disinc_amt as fundsdisnc"
          + " from efin_budgetinquiry funds" + " join " + hdtable
          + " hd on  hd.efin_budgetint_id=funds.efin_budgetint_id" + " join " + lntable
          + " ln on hd." + headerid + " = ln." + headerid;

      if (transactionType != null
          && (transactionType.equals("BADJ") || transactionType.equals("BR")))
        query += " and ln.c_validcombination_id= funds.c_validcombination_id ";
      else
        query += " and ln.toaccount= funds.c_validcombination_id ";

      query += " join c_validcombination fundscb on ln.c_validcombination_id= fundscb.c_validcombination_id"
          + " join efin_budgetinquiry cost on cost.efin_budgetint_id = funds.efin_budgetint_id"
          + " and fundscb.em_efin_costcombination= cost.c_validcombination_id" + "  where 1=1 and"
          + " CASE WHEN funds.c_salesregion_id = '" + bcuOrgId + "'" + " THEN"
          + " (funds.current_budget + coalesce(ln.increase,0)) > (cost.current_budget - coalesce(coalesce(cost.legacy_spent,0)+coalesce(cost.grp_spent,0), 0)) " // +
          // funds.disinc_amt
          // -funds.disdec_amt
          // +
          // cost.disinc_amt
          // -cost.disdec_amt
          + " ELSE" + " (funds.funds_available+coalesce(ln.increase,0)) > (cost.funds_available)"
          + " END";

      if (transactionType != null
          && (transactionType.equals("BADJ") || transactionType.equals("BR")))
        query += " and coalesce(ln.increase,0) >0";

      query += " and hd." + headerid + "= ? ";

      ps = con.prepareStatement(query);
      ps.setString(1, fundBudRevId);
      log.debug("isFundAmountExceedFromCostBudgetAmount :" + ps.toString());
      rs = ps.executeQuery();
      while (rs.next()) {
        if (transactionType.equals("BR")) {
          EfinBudgetTransfertrxline line = OBDal.getInstance().get(EfinBudgetTransfertrxline.class,
              rs.getString("lineid"));

          String childAcctId = line.getAccountingCombination().getAccount().getId();
          if (!budUtil.isFundsOnlyAccount(childAcctId, clientId)) {
            String status = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_FundsCost");
            line.setStatus(status.replace("@", rs.getString("costcbug")));
            OBDal.getInstance().save(line);
            count += 1;
          }
        } else if (transactionType.equals("BADJ")) {
          BudgetAdjustmentLine line = OBDal.getInstance().get(BudgetAdjustmentLine.class,
              rs.getString("lineid"));

          String childAcctId = line.getAccountingCombination().getAccount().getId();
          if (!budUtil.isFundsOnlyAccount(childAcctId, clientId)) {
            String status = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_FundsCost");
            line.setFailureReason(status.replace("@", rs.getString("costcbug")));
            line.setAlertStatus("Failed");
            OBDal.getInstance().save(line);
            count += 1;
          }
        } else if (transactionType.equals("BD")) {
          EFINFundsReqLine line = OBDal.getInstance().get(EFINFundsReqLine.class,
              rs.getString("lineid"));

          String childAcctId = line.getToaccount().getAccount().getId();
          if (!budUtil.isFundsOnlyAccount(childAcctId, clientId)) {
            String status = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_FundsCost");
            line.setFailureReason(status.replace("@", rs.getString("costcbug")));
            line.setAlertStatus("FL");
            OBDal.getInstance().save(line);
            log.debug("count getFailureReason :" + line.getFailureReason());
            log.debug("count getAlertStatus :" + line.getAlertStatus());
            count += 1;
          }
        }
        log.debug("count funds :" + count);
        return count;
        // errorMsg = "@Efin_budget_Rev_Lines_Failed@";
      }

    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  isFundAmountExceedFromCostBudgetAmount " + e, e);
      }
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();

    }
    return count;
  }

  /**
   * 
   * @param fundBudRevId
   * @param clientId
   * @param budInt
   * @param transactionType
   * @return count
   */
  public static int isBudgetDecreaseAmountExceedsFundsAvail(String fundBudRevId, String clientId,
      String budInt, String transactionType, String currentCamId) {

    String query = "", lineid = null, hdtable = null, lntable = null, headerid = null;
    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    int count = 0;
    try {
      OBContext.setAdminMode();

      if (transactionType.equals("BR")) {
        lineid = " efin_budget_transfertrxline_id ";
        headerid = " efin_budget_transfertrx_id ";
        hdtable = " efin_budget_transfertrx";
        lntable = " efin_budget_transfertrxline ";
      }
      if (transactionType.equals("BADJ")) {
        lineid = " efin_budgetadjline_id ";
        headerid = " efin_budgetadj_id ";
        hdtable = " efin_budgetadj";
        lntable = " efin_budgetadjline ";
      }
      if (transactionType.equals("BD")) {
        lineid = " efin_fundsreqline_id ";
        headerid = " efin_fundsreq_id ";
        hdtable = " efin_fundsreq ";
        lntable = " efin_fundsreqline ";
      }
      query = "  select ln.increase,ln.decrease,ln." + lineid
          + " as lineid ,(CASE WHEN inq.c_salesregion_id = '" + bcuOrgId
          + "'  THEN  (inq.current_budget + inq.disinc_amt - inq.disdec_amt)  WHEN inq.c_salesregion_id = '"
          + costOrgId
          + "' THEN  (inq.current_budget + inq.depinc_amt - inq.depdec_amt) ELSE  inq.funds_available end) as funds_available, "
          + " inq.funds_available as funds_available1 from " + lntable + "ln " + " join " + hdtable
          + " hd on hd." + headerid + " = ln." + headerid + " join efin_budgetinquiry inq"
          + " on inq.efin_budgetint_id= hd.efin_budgetint_id " + "  where 1=1 ";
      if (transactionType != null
          && (transactionType.equals("BADJ") || transactionType.equals("BR")))
        query += " and ln.c_validcombination_id= inq.c_validcombination_id and  ln.increase=0 ";
      else
        query += " and ln.fromaccount= inq.c_validcombination_id ";
      query += " and ln." + headerid + " = ?" + " and inq.efin_budgetint_id=? and "
          + "  CASE WHEN inq.c_salesregion_id = '" + bcuOrgId
          + "' THEN (ln.decrease > (inq.current_budget + inq.disinc_amt - inq.disdec_amt) or  (ln.decrease >inq.funds_available)) "
          + "   WHEN inq.c_salesregion_id = '" + costOrgId
          + "' THEN (ln.decrease > (inq.current_budget + inq.depinc_amt - inq.depdec_amt)) "
          + "  ELSE (ln.decrease > inq.funds_available) "
          + "  END  and inq.ad_client_id =? and inq.c_campaign_id = ?  ";

      ps = con.prepareStatement(query);
      ps.setString(1, fundBudRevId);
      ps.setString(2, budInt);
      ps.setString(3, clientId);
      ps.setString(4, currentCamId);
      log.debug("isBudgetDecreaseAmountExceedsFundsAvail :" + ps.toString());
      rs = ps.executeQuery();
      while (rs.next()) {
        if (transactionType.equals("BR")) {
          EfinBudgetTransfertrxline line = OBDal.getInstance().get(EfinBudgetTransfertrxline.class,
              rs.getString("lineid"));
          String status = OBMessageUtils.messageBD("Efin_DecAmount_Greaterthan_FundsAmount");
          line.setStatus(status.replace("@", rs.getString("funds_available")));
          OBDal.getInstance().save(line);
        } else if (transactionType.equals("BADJ")) {
          BudgetAdjustmentLine line = OBDal.getInstance().get(BudgetAdjustmentLine.class,
              rs.getString("lineid"));
          String status = OBMessageUtils.messageBD("Efin_DecAmount_Greaterthan_FundsAmount");
          line.setFailureReason(status.replace("@", rs.getString("funds_available")));
          line.setAlertStatus("Failed");
          OBDal.getInstance().save(line);
        } else if (transactionType.equals("BD")) {
          EFINFundsReqLine line = OBDal.getInstance().get(EFINFundsReqLine.class,
              rs.getString("lineid"));
          String status = OBMessageUtils.messageBD("Efin_DecAmount_Greaterthan_FundsAmount");
          line.setFailureReason(status.replace("@", rs.getString("funds_available")));
          line.setAlertStatus("FL");
          OBDal.getInstance().save(line);
        }
        count += 1;
        return count;

      }

    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  isBudgetDecreaseAmountExceedsFundsAvail " + e, e);
      }
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();

    }
    return count;
  }

  /**
   * 
   * @param costBudRevId
   * @param clientId
   * @param budInt
   * @param transactionType
   * @param tocampId
   * @return count
   */
  public static int isCostBudgetDecreaseAmountExceedFundsBudget(String costBudRevId,
      String clientId, String budInt, String transactionType, String tocampId) {

    String query = "", lineid = null, hdtable = null, lntable = null, headerid = null;
    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    int count = 0;
    BudgetingUtilsService budUtil = new BudgetingUtilsServiceImpl();
    try {
      OBContext.setAdminMode();

      if (transactionType.equals("BR")) {
        lineid = " efin_budget_transfertrxline_id ";
        headerid = " efin_budget_transfertrx_id ";
        hdtable = " efin_budget_transfertrx";
        lntable = " efin_budget_transfertrxline ";
      }
      if (transactionType.equals("BADJ")) {
        lineid = " efin_budgetadjline_id ";
        headerid = " efin_budgetadj_id ";
        hdtable = " efin_budgetadj";
        lntable = " efin_budgetadjline ";
      }
      if (transactionType.equals("BD")) {
        lineid = " efin_fundsreqline_id ";
        headerid = " efin_fundsreq_id ";
        hdtable = " efin_fundsreq ";
        lntable = " efin_fundsreqline ";
      }

      query = " select ln." + lineid + " as lineid, hd." + headerid + " as headerid,"
          + " cost.current_budget as costcbug, coalesce(ln.decrease,0) as costdecrease, cost.disdec_amt as costdisdec, cost.disinc_amt as costdisinc,"
          + " funds.current_budget as fundscbug, funds.disdec_amt as fundsdisdec, funds.disinc_amt as fundsdisnc"
          + " from  efin_budgetinquiry cost" + " join " + hdtable
          + " hd on  hd.efin_budgetint_id=cost.efin_budgetint_id" + " join " + lntable
          + " ln on hd." + headerid + " = ln. " + headerid;

      if (transactionType != null
          && (transactionType.equals("BADJ") || transactionType.equals("BR")))
        query += " and ln.c_validcombination_id= cost.c_validcombination_id ";
      else
        query += " and ln.fromaccount= cost.c_validcombination_id ";

      query += " join c_validcombination fundscb on ln.c_validcombination_id= fundscb.em_efin_costcombination"
          + " join efin_budgetinquiry funds on cost.efin_budgetint_id = funds.efin_budgetint_id"
          + " and fundscb.c_validcombination_id= funds.c_validcombination_id" + " where 1=1 and";

      if (transactionType.equals("BR")) {
        query += " CASE" + " WHEN cost.c_salesregion_id = '" + bcuOrgId + "' THEN"
            + " (cost.current_budget-coalesce(ln.decrease,0)) < (funds.current_budget)" + " ELSE"
            + " (cost.funds_available-coalesce(ln.decrease,0)) < (funds.funds_available)" + " END";
      } else {
        query += " CASE" + " WHEN cost.c_salesregion_id = '" + bcuOrgId + "' THEN"
            + " (cost.current_budget-coalesce(ln.decrease) ) < (funds.current_budget )" + " ELSE"
            + " (cost.funds_available-coalesce(ln.decrease,0)) < (funds.funds_available)" + " END";
      }

      if (transactionType != null
          && (transactionType.equals("BADJ") || transactionType.equals("BR")))
        query += " and coalesce(ln.decrease,0) >0 ";

      query += "and hd." + headerid + "= ? ";

      ps = con.prepareStatement(query);
      ps.setString(1, costBudRevId);
      rs = ps.executeQuery();
      log.debug("Cost Budget Decrease amount is lesser than the funds budget currentamount Query:"
          + query.toString());
      while (rs.next()) {
        if (transactionType.equals("BR")) {
          EfinBudgetTransfertrxline line = OBDal.getInstance().get(EfinBudgetTransfertrxline.class,
              rs.getString("lineid"));
          if (!budUtil.isFundsOnlyAccount(line.getAccountingCombination().getAccount().getId(),
              line.getClient().getId())) {
            String status = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_CostFunds");
            line.setStatus(status.replace("@", rs.getString("fundscbug")));
            OBDal.getInstance().save(line);
            count += 1;
          }
        } else if (transactionType.equals("BADJ")) {
          BudgetAdjustmentLine line = OBDal.getInstance().get(BudgetAdjustmentLine.class,
              rs.getString("lineid"));
          if (!budUtil.isFundsOnlyAccount(line.getAccountingCombination().getAccount().getId(),
              line.getClient().getId())) {
            String status = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_CostFunds");
            line.setFailureReason(status.replace("@", rs.getString("fundscbug")));
            line.setAlertStatus("Failed");
            OBDal.getInstance().save(line);
            count += 1;
          }
        } else if (transactionType.equals("BD")) {
          EFINFundsReqLine line = OBDal.getInstance().get(EFINFundsReqLine.class,
              rs.getString("lineid"));
          String status = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_CostFunds");
          line.setFailureReason(status.replace("@", rs.getString("fundscbug")));
          line.setAlertStatus("FL");
          OBDal.getInstance().save(line);
          count += 1;
        }

        return count;
      }

    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  isCostBudgetDecreaseAmountExceedFundsBudget " + e, e);
      }
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();

    }
    return count;

  }

  /**
   * 
   * @param costBudRevId
   * @param clientId
   * @param budInt
   * @param transactionType
   * @return count
   */
  public static int isCostDecreaseAmountExceedCostBudgetFundsAvail(String costBudRevId,
      String clientId, String budInt, String transactionType, String currentCamId) {
    int count = 0;
    String query = "", lineid = null, hdtable = null, lntable = null, headerid = null;
    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      OBContext.setAdminMode();

      if (transactionType.equals("BR")) {
        lineid = " efin_budget_transfertrxline_id ";
        headerid = " efin_budget_transfertrx_id ";
        hdtable = " efin_budget_transfertrx";
        lntable = " efin_budget_transfertrxline ";
      }
      if (transactionType.equals("BADJ")) {
        lineid = " efin_budgetadjline_id ";
        headerid = " efin_budgetadj_id ";
        hdtable = " efin_budgetadj";
        lntable = " efin_budgetadjline ";
      }
      if (transactionType.equals("BD")) {
        lineid = " efin_fundsreqline_id ";
        headerid = " efin_fundsreq_id ";
        hdtable = " efin_fundsreq ";
        lntable = " efin_fundsreqline ";
      }

      query = "select inq.current_budget as current_budget1, (CASE WHEN inq.c_salesregion_id = '"
          + bcuOrgId
          + "' THEN (inq.current_budget + inq.disinc_amt - inq.disdec_amt)  WHEN inq.c_salesregion_id = '"
          + costOrgId
          + "' THEN  (inq.current_budget + inq.depinc_amt - inq.depdec_amt) ELSE inq.funds_available end) as current_budget ,"
          + " inq.funds_available as available ,ln.decrease as decrease,ln. " + lineid
          + " as lineid from " + lntable + " ln  join efin_budgetinquiry inq on ";
      if (transactionType != null
          && (transactionType.equals("BADJ") || transactionType.equals("BR")))
        query += "  ln.c_validcombination_id= inq.c_validcombination_id ";
      else
        query += "  ln.fromaccount= inq.c_validcombination_id ";

      query += " join " + hdtable + " hd on hd." + headerid + " =ln." + headerid + " where ln."
          + headerid + " =? and " + "  CASE WHEN inq.c_salesregion_id = '" + bcuOrgId
          + "' THEN (ln.decrease > (inq.current_budget + inq.disinc_amt - inq.disdec_amt)) "
          + "    WHEN inq.c_salesregion_id = '" + costOrgId
          + "' THEN (ln.decrease > (inq.current_budget + inq.depinc_amt - inq.depdec_amt)) "
          + "  ELSE (ln.decrease > inq.funds_available) "
          + "  END  and inq.ad_client_id =? and inq.c_campaign_id = ? and inq.efin_budgetint_id= ? ";

      if (transactionType != null
          && (transactionType.equals("BADJ") || transactionType.equals("BR")))
        query += "and ln.increase = 0 ";

      ps = con.prepareStatement(query);
      ps.setString(1, costBudRevId);
      ps.setString(2, clientId);
      ps.setString(3, currentCamId);
      ps.setString(4, budInt);
      log.debug("isCostDecreaseAmountExceedCostBudgetFundsAvail :" + ps.toString());
      rs = ps.executeQuery();
      while (rs.next()) {
        if (transactionType.equals("BR")) {
          EfinBudgetTransfertrxline line = OBDal.getInstance().get(EfinBudgetTransfertrxline.class,
              rs.getString("lineid"));

          String status = OBMessageUtils.messageBD("Efin_DecAmount_Greaterthan_CostAmount");
          line.setStatus(status.replace("@", rs.getString("current_budget")));
          OBDal.getInstance().save(line);

        } else if (transactionType.equals("BADJ")) {
          BudgetAdjustmentLine line = OBDal.getInstance().get(BudgetAdjustmentLine.class,
              rs.getString("lineid"));
          String status = OBMessageUtils.messageBD("Efin_DecAmount_Greaterthan_CostAmount");
          line.setFailureReason(status.replace("@", rs.getString("current_budget")));
          line.setAlertStatus("Failed");
          OBDal.getInstance().save(line);
        } else if (transactionType.equals("BD")) {
          EFINFundsReqLine line = OBDal.getInstance().get(EFINFundsReqLine.class,
              rs.getString("lineid"));
          String status = OBMessageUtils.messageBD("Efin_DecAmount_Greaterthan_CostAmount");
          line.setFailureReason(status.replace("@", rs.getString("current_budget")));
          line.setAlertStatus("FL");
          OBDal.getInstance().save(line);
        }
        count += 1;
        return count;

      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  isCostDecreaseAmountExceedCostBudgetFundsAvail " + e, e);
      }
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();

    }
    return count;

  }

  /**
   * 
   * @param trantype
   * @param headerId
   * @param clientId
   * @param actionType
   * @return count
   */
  public static int checkCommonVal(String trantype, String headerId, String clientId,
      String actionType, boolean isWarn) {
    int count1 = 0;
    try {
      OBContext.setAdminMode();
      if (intialStatusUpdate(trantype, headerId, clientId, isWarn) > 0) {
        if (actionType.equals("CO")) {
          count1 = checkValidations(headerId, clientId, trantype, isWarn);
        } else {
          count1 = checkReactivateValidations(headerId, clientId, trantype);
        }

      } else {
        count1 = 1;
      }
      log.debug("count1:" + count1);
    } catch (Exception e) {
      e.printStackTrace();
      if (log.isErrorEnabled()) {
        log.error("Exception in  checkCommonVal " + e, e);
      }
    } finally {
      OBContext.restorePreviousMode();

    }
    return count1;
  }

  /**
   * Common validation for encumbrance to update budget enquiry.
   * 
   * @param trantype
   * @param headerId
   * @param clientId
   * @param actionType
   * @return
   */
  public static int checkCommonValEncum(String trantype, String headerId, String clientId,
      String actionType) {
    int count1 = 0;
    try {
      OBContext.setAdminMode();
      if (intialStatusUpdate(trantype, headerId, clientId, false) > 0) {
        if (actionType.equals("CO")) {
          count1 = checkEncumValidations(headerId, clientId, trantype);
        } else {
          count1 = checkEncumReactivateValidations(headerId, clientId, trantype);
        }

      } else {
        count1 = 1;
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  checkCommonVal " + e, e);
      }
    } finally {
      OBContext.restorePreviousMode();

    }
    return count1;
  }

  /**
   * 
   * @param trantype
   * @param headerId
   * @param clientId
   * @return count
   */
  public static int intialStatusUpdate(String trantype, String headerId, String clientId,
      boolean isWarn) {
    int count = 0;
    try {
      OBContext.setAdminMode();

      if (trantype.equals("BR")) {
        OBQuery<EfinBudgetTransfertrxline> trxln = OBDal.getInstance().createQuery(
            EfinBudgetTransfertrxline.class,
            " as e where e.efinBudgetTransfertrx.id='" + headerId + "'");
        if (trxln.list().size() > 0) {
          for (EfinBudgetTransfertrxline ln : trxln.list()) {
            count++;
            if (!isWarn) {
              ln.setStatus("Success");
              OBDal.getInstance().save(ln);
            }
          }
        }
      }
      if (trantype.equals("BADJ")) {
        OBQuery<BudgetAdjustmentLine> budln = OBDal.getInstance().createQuery(
            BudgetAdjustmentLine.class, " as e where e.efinBudgetadj.id='" + headerId + "'");
        if (budln.list().size() > 0) {
          for (BudgetAdjustmentLine ln : budln.list()) {
            count++;
            if (!isWarn) {
              ln.setAlertStatus("Success");
              ln.setFailureReason(null);
              OBDal.getInstance().save(ln);
            }
          }
        }
      }
      if (trantype.equals("BD")) {
        OBQuery<EFINFundsReqLine> reqln = OBDal.getInstance().createQuery(EFINFundsReqLine.class,
            " as e where e.efinFundsreq.id='" + headerId + "'");
        if (reqln.list().size() > 0) {
          for (EFINFundsReqLine ln : reqln.list()) {
            count++;
            if (!isWarn) {
              ln.setAlertStatus("SCS");
              ln.setFailureReason(null);
              log.debug("sta:" + ln.getAlertStatus());
              OBDal.getInstance().save(ln);
            }
          }
        }
      }

      if (trantype.equals("ENC")) {
        OBQuery<EfinBudgetManencumlines> encln = OBDal.getInstance().createQuery(
            EfinBudgetManencumlines.class, " as e where e.manualEncumbrance.id='" + headerId + "'");
        if (encln.list().size() > 0) {
          for (EfinBudgetManencumlines ln : encln.list()) {
            count++;
            ln.setCheckingStatus("SCS");
            ln.setFailureReason(null);
            log.debug("sta:" + ln.getCheckingStatus());
            OBDal.getInstance().save(ln);
          }
        }
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  updating the status " + e, e);
      }
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  /**
   * 
   * @param costBudRevId
   * @param clientId
   * @param budInt
   * @param transactionType
   * @return count
   */
  public static int isReactivateCostIncreaseAmountExceedCostBudgetFundsAvail(String costBudRevId,
      String clientId, String budInt, String transactionType, String currentCampId) {
    int count = 0;
    String query = "", lineid = null, hdtable = null, lntable = null, headerid = null;
    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;

    try {
      OBContext.setAdminMode();

      if (transactionType.equals("BR")) {
        lineid = " efin_budget_transfertrxline_id ";
        headerid = " efin_budget_transfertrx_id ";
        hdtable = " efin_budget_transfertrx";
        lntable = " efin_budget_transfertrxline ";
      }
      if (transactionType.equals("BADJ")) {
        lineid = " efin_budgetadjline_id ";
        headerid = " efin_budgetadj_id ";
        hdtable = " efin_budgetadj";
        lntable = " efin_budgetadjline ";
      }
      if (transactionType.equals("BD")) {
        lineid = " efin_fundsreqline_id ";
        headerid = " efin_fundsreq_id ";
        hdtable = " efin_fundsreq ";
        lntable = " efin_fundsreqline ";
      }

      query = "select inq.current_budget as current_budget, inq.funds_available as available ,ln.decrease as decrease,ln. "
          + lineid + " as lineid from " + lntable + " ln  join efin_budgetinquiry inq on ";
      if (transactionType != null
          && (transactionType.equals("BADJ") || transactionType.equals("BR")))
        query += " ln.c_validcombination_id= inq.c_validcombination_id ";
      else
        query += " ln.toaccount= inq.c_validcombination_id ";
      query += " join " + hdtable + " hd on hd." + headerid + " =ln." + headerid + " where ln."
          + headerid + " =? and " + "  CASE WHEN inq.c_salesregion_id = '" + bcuOrgId
          + "' THEN (ln.increase  > (inq.current_budget + inq.disinc_amt - inq.disdec_amt)) "
          + "   WHEN inq.c_salesregion_id = '" + costOrgId
          + "' THEN ((ln.increase  > (inq.current_budget + inq.depinc_amt  - inq.depdec_amt )) or (ln.increase  > inq.funds_available)) "
          + "  ELSE (ln.increase  > inq.funds_available) END   and inq.ad_client_id =?   and inq.c_campaign_id = ? and inq.efin_budgetint_id= ? ";
      if (transactionType != null
          && (transactionType.equals("BADJ") || transactionType.equals("BR")))
        query += "  and  ln.decrease = 0 ";

      ps = con.prepareStatement(query);
      ps.setString(1, costBudRevId);
      ps.setString(2, clientId);
      ps.setString(3, currentCampId);
      ps.setString(4, budInt);

      log.debug("ps :" + ps.toString());
      rs = ps.executeQuery();
      while (rs.next()) {
        if (transactionType.equals("BR")) {
          EfinBudgetTransfertrxline line = OBDal.getInstance().get(EfinBudgetTransfertrxline.class,
              rs.getString("lineid"));

          String status = OBMessageUtils.messageBD("Efin_IncAmount_Greaterthan_CostAmount_React");
          line.setStatus(status.replace("@", rs.getString("available")));
          OBDal.getInstance().save(line);
        } else if (transactionType.equals("BADJ")) {
          BudgetAdjustmentLine line = OBDal.getInstance().get(BudgetAdjustmentLine.class,
              rs.getString("lineid"));
          String status = OBMessageUtils.messageBD("Efin_IncAmount_Greaterthan_CostAmount_React");
          line.setAlertStatus("Failed");
          line.setFailureReason(status.replace("@", rs.getString("available")));
          OBDal.getInstance().save(line);
        } else if (transactionType.equals("BD")) {
          EFINFundsReqLine line = OBDal.getInstance().get(EFINFundsReqLine.class,
              rs.getString("lineid"));
          String status = OBMessageUtils.messageBD("Efin_IncAmount_Greaterthan_CostAmount_React");
          line.setFailureReason(status.replace("@", rs.getString("available")));
          line.setAlertStatus("FL");
          OBDal.getInstance().save(line);
        }
        count += 1;
        return count;

      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  isCostDecreaseAmountExceedCostBudgetFundsAvail " + e, e);
      }
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();

    }
    return count;

  }

  /**
   * 
   * @param costBudRevId
   * @param clientId
   * @param budInt
   * @param transactionType
   * @param tocampId
   * @return count
   */
  public static int isReactivateCostBudgetDecreaseAmountExceedFundsBudget(String costBudRevId,
      String clientId, String budInt, String transactionType, String tocampId) {

    String query = "", lineid = null, hdtable = null, lntable = null, headerid = null;
    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    int count = 0;
    BudgetingUtilsService budUtil = new BudgetingUtilsServiceImpl();
    try {
      OBContext.setAdminMode();

      if (transactionType.equals("BR")) {
        lineid = " efin_budget_transfertrxline_id ";
        headerid = " efin_budget_transfertrx_id ";
        hdtable = " efin_budget_transfertrx";
        lntable = " efin_budget_transfertrxline ";
      }
      if (transactionType.equals("BADJ")) {
        lineid = " efin_budgetadjline_id ";
        headerid = " efin_budgetadj_id ";
        hdtable = " efin_budgetadj";
        lntable = " efin_budgetadjline ";
      }
      if (transactionType.equals("BD")) {
        lineid = " efin_fundsreqline_id ";
        headerid = " efin_fundsreq_id ";
        hdtable = " efin_fundsreq ";
        lntable = " efin_fundsreqline ";
      }

      query = " select ln." + lineid + " as lineid, hd." + headerid + " as headerid,"
          + " cost.current_budget as costcbug, coalesce(ln.increase,0) as costincrease,"
          + " cost.disdec_amt as costdisdec, cost.disinc_amt as costdisinc, funds.current_budget as fundscbug,"
          + " funds.disdec_amt as fundsdisdec, funds.disinc_amt as fundsdisnc"
          + " from efin_budgetinquiry cost" + " join " + hdtable
          + " hd on  hd.efin_budgetint_id=cost.efin_budgetint_id" + " join " + lntable
          + " ln on hd." + headerid + " = ln." + headerid;

      if (transactionType != null
          && (transactionType.equals("BADJ") || transactionType.equals("BR")))
        query += " and ln.c_validcombination_id= cost.c_validcombination_id ";
      else
        query += " and ln.toaccount= cost.c_validcombination_id ";

      query += " join c_validcombination fundscb on ln.c_validcombination_id= fundscb.em_efin_costcombination"
          + " join efin_budgetinquiry funds on cost.efin_budgetint_id = funds.efin_budgetint_id"
          + " and fundscb.c_validcombination_id= funds.c_validcombination_id" + " where 1=1 and ";

      if (transactionType.equals("BR")) {
        query += " CASE" + " WHEN cost.c_salesregion_id = '" + bcuOrgId + "' THEN"
            + " (cost.current_budget-coalesce(ln.increase,0)) < (funds.current_budget)" + " ELSE"
            + " (cost.funds_available-coalesce(ln.increase,0)) < (funds.funds_available)" + " END";
      } else {
        query += " CASE" + " WHEN cost.c_salesregion_id = '" + bcuOrgId + "' THEN"
            + " (cost.current_budget-coalesce(ln.increase,0) ) < (funds.current_budget)" + " ELSE"
            + " (cost.funds_available-coalesce(ln.increase,0)) < (funds.funds_available)" + " END";
      }

      if (transactionType != null
          && (transactionType.equals("BADJ") || transactionType.equals("BR")))
        query += " and coalesce(ln.increase,0) >0 ";

      query += " and hd." + headerid + "= ?";

      ps = con.prepareStatement(query);
      ps.setString(1, costBudRevId);

      log.debug(
          "Reactivating Cost Budget increase amount is lesser than the funds budget currentamount Query:"
              + query.toString());
      rs = ps.executeQuery();
      while (rs.next()) {
        if (transactionType.equals("BR")) {
          EfinBudgetTransfertrxline line = OBDal.getInstance().get(EfinBudgetTransfertrxline.class,
              rs.getString("lineid"));
          if (!budUtil.isFundsOnlyAccount(line.getAccountingCombination().getAccount().getId(),
              line.getClient().getId())) {
            String status = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_CostFunds");
            line.setStatus(status.replace("@", rs.getString("fundscbug")));
            OBDal.getInstance().save(line);
            count += 1;
          }
        } else if (transactionType.equals("BADJ")) {
          BudgetAdjustmentLine line = OBDal.getInstance().get(BudgetAdjustmentLine.class,
              rs.getString("lineid"));
          String status1 = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_CostFunds");
          line.setAlertStatus("Failed");
          line.setFailureReason(status1.replace("@", rs.getString("fundscbug")));
          OBDal.getInstance().save(line);
          count += 1;
        } else if (transactionType.equals("BD")) {
          EFINFundsReqLine line = OBDal.getInstance().get(EFINFundsReqLine.class,
              rs.getString("lineid"));
          String status = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_CostFunds");
          line.setFailureReason(status.replace("@", rs.getString("fundscbug")));
          line.setAlertStatus("FL");
          OBDal.getInstance().save(line);
          count += 1;
        }
        return count;
      }

    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  isCostBudgetDecreaseAmountExceedFundsBudget " + e, e);
      }
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();

    }
    return count;

  }

  /**
   * 
   * @param fundBudRevId
   * @param clientId
   * @param budInt
   * @param transactionType
   * @param tocampId
   * @return count
   */
  public static int isReactivateFundAmountExceedFromCostBudgetAmount(String fundBudRevId,
      String clientId, String budInt, String transactionType, String tocampId) {

    String query = "", lineid = null, hdtable = null, lntable = null, headerid = null;
    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    int count = 0;
    BudgetingUtilsService budUtil = new BudgetingUtilsServiceImpl();
    try {
      OBContext.setAdminMode();
      if (transactionType.equals("BR")) {
        lineid = " efin_budget_transfertrxline_id ";
        headerid = " efin_budget_transfertrx_id ";
        hdtable = " efin_budget_transfertrx";
        lntable = " efin_budget_transfertrxline ";
      }
      if (transactionType.equals("BADJ")) {
        lineid = " efin_budgetadjline_id ";
        headerid = " efin_budgetadj_id ";
        hdtable = " efin_budgetadj";
        lntable = " efin_budgetadjline ";
      }
      if (transactionType.equals("BD")) {
        lineid = " efin_fundsreqline_id ";
        headerid = " efin_fundsreq_id ";
        hdtable = " efin_fundsreq ";
        lntable = " efin_fundsreqline ";
      }

      query = " select ln." + lineid + " as lineid, hd." + headerid + " as  headerid, "
          + " cost.current_budget as costcbug, coalesce(ln.decrease,0) as fundsdecrease, cost.disdec_amt as costdisdec, cost.disinc_amt as costdisin,"
          + " funds.current_budget as fundscbug, funds.disdec_amt as fundsdisdec, funds.disinc_amt as fundsdisnc"
          + " from efin_budgetinquiry  funds" + " join  " + hdtable
          + "  hd on  hd.efin_budgetint_id=funds.efin_budgetint_id" + " join  " + lntable
          + "  ln on hd." + headerid + "  = ln." + headerid;

      if (transactionType != null
          && (transactionType.equals("BADJ") || transactionType.equals("BR")))
        query += " and ln.c_validcombination_id= funds.c_validcombination_id ";
      else
        query += " and ln.fromaccount= funds.c_validcombination_id ";

      query += " join c_validcombination fundscb on ln.c_validcombination_id= fundscb.c_validcombination_id"
          + " join efin_budgetinquiry cost on cost.efin_budgetint_id = funds.efin_budgetint_id"
          + " and fundscb.em_efin_costcombination= cost.c_validcombination_id" + " where 1=1"
          + " and" + " CASE WHEN funds.c_salesregion_id = '" + bcuOrgId + "' THEN";

      if (transactionType.equals("BR")) {
        query += " (funds.current_budget + coalesce(ln.decrease,0)) > (cost.current_budget)"
            + " ELSE"
            + " (funds.funds_available + coalesce(ln.decrease,0)) > (cost.funds_available)"
            + " END";
      } else {
        query += " (funds.current_budget - coalesce(ln.decrease,0)) > (cost.current_budget)"
            + " ELSE"
            + " (funds.funds_available + coalesce(ln.decrease,0)) > (cost.funds_available)"
            + " END";
      }

      if (transactionType != null
          && (transactionType.equals("BADJ") || transactionType.equals("BR")))
        query += " and coalesce(ln.decrease,0) >0";

      query += "and hd." + headerid + "= ? ";

      ps = con.prepareStatement(query);
      ps.setString(1, fundBudRevId);
      log.debug("ps :" + ps.toString());
      rs = ps.executeQuery();
      while (rs.next()) {
        if (transactionType.equals("BR")) {
          EfinBudgetTransfertrxline line = OBDal.getInstance().get(EfinBudgetTransfertrxline.class,
              rs.getString("lineid"));
          if (!budUtil.isFundsOnlyAccount(line.getAccountingCombination().getAccount().getId(),
              line.getClient().getId())) {
            String status = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_FundsCost");
            line.setStatus(status.replace("@", rs.getString("costcbug")));
            OBDal.getInstance().save(line);
            count += 1;
          }
        } else if (transactionType.equals("BADJ")) {
          BudgetAdjustmentLine line = OBDal.getInstance().get(BudgetAdjustmentLine.class,
              rs.getString("lineid"));
          if (!budUtil.isFundsOnlyAccount(line.getAccountingCombination().getAccount().getId(),
              line.getClient().getId())) {
            String status = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_FundsCost");
            line.setFailureReason(status.replace("@", rs.getString("costcbug")));
            line.setAlertStatus("Failed");
            OBDal.getInstance().save(line);
            count += 1;
          }
        } else if (transactionType.equals("BD")) {
          EFINFundsReqLine line = OBDal.getInstance().get(EFINFundsReqLine.class,
              rs.getString("lineid"));
          String status = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_FundsCost");
          line.setFailureReason(status.replace("@", rs.getString("costcbug")));
          line.setAlertStatus("FL");
          OBDal.getInstance().save(line);
          count += 1;
        }

        return count;
        // errorMsg = "@Efin_budget_Rev_Lines_Failed@";

      }

    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  isFundAmountExceedFromCostBudgetAmount " + e, e);
      }
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();

    }
    return count;
  }

  /**
   * 
   * @param fundBudRevId
   * @param clientId
   * @param budInt
   * @param transactionType
   * @return count
   */
  public static int isReactivateBudgetIncreaseAmountExceedsFundsAvail(String fundBudRevId,
      String clientId, String budInt, String transactionType, String currentCamId) {

    String query = "", lineid = null, hdtable = null, lntable = null, headerid = null;
    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    int count = 0;

    try {
      OBContext.setAdminMode();

      if (transactionType.equals("BR")) {
        lineid = " efin_budget_transfertrxline_id ";
        headerid = " efin_budget_transfertrx_id ";
        hdtable = " efin_budget_transfertrx";
        lntable = " efin_budget_transfertrxline ";
      }
      if (transactionType.equals("BADJ")) {
        lineid = " efin_budgetadjline_id ";
        headerid = " efin_budgetadj_id ";
        hdtable = " efin_budgetadj";
        lntable = " efin_budgetadjline ";
      }
      if (transactionType.equals("BD")) {
        lineid = " efin_fundsreqline_id ";
        headerid = " efin_fundsreq_id ";
        hdtable = " efin_fundsreq ";
        lntable = " efin_fundsreqline ";
      }
      query = "  select ln.increase,ln.decrease,ln." + lineid
          + " as lineid ,inq.funds_available from " + lntable + "ln " + " join " + hdtable
          + " hd on hd." + headerid + " = ln." + headerid + " join efin_budgetinquiry inq"
          + " on inq.efin_budgetint_id= hd.efin_budgetint_id " + "  where 1=1  ";
      if (transactionType != null
          && (transactionType.equals("BADJ") || transactionType.equals("BR")))
        query += "and ln.c_validcombination_id= inq.c_validcombination_id   and ln.decrease=0  ";
      else
        query += " and ln.toaccount= inq.c_validcombination_id ";

      query += "and ln. " + headerid + " = ?" + " and inq.efin_budgetint_id=?" + " and "
          + "  CASE WHEN inq.c_salesregion_id = '" + bcuOrgId
          + "' THEN (ln.increase > (inq.current_budget + inq.disinc_amt - inq.disdec_amt)) "
          + "   WHEN inq.c_salesregion_id = '" + costOrgId
          + "' THEN (ln.increase > (inq.current_budget + inq.depinc_amt - inq.depdec_amt -inq.encumbrance+inq.spent_amt)) "
          + "  ELSE (ln.increase > inq.funds_available) "
          + "  END  and inq.ad_client_id =?  and inq.c_campaign_id = ?   ";

      ps = con.prepareStatement(query);
      ps.setString(1, fundBudRevId);
      ps.setString(2, budInt);
      ps.setString(3, clientId);
      ps.setString(4, currentCamId);
      log.debug("funds decrease exceed amount:" + ps.toString());
      rs = ps.executeQuery();
      while (rs.next()) {
        if (transactionType.equals("BR")) {
          EfinBudgetTransfertrxline line = OBDal.getInstance().get(EfinBudgetTransfertrxline.class,
              rs.getString("lineid"));
          String status = OBMessageUtils.messageBD("Efin_DecAmount_Greaterthan_FundsAmount");
          line.setStatus(status.replace("@", rs.getString("funds_available")));
          OBDal.getInstance().save(line);
        } else if (transactionType.equals("BADJ")) {
          BudgetAdjustmentLine line = OBDal.getInstance().get(BudgetAdjustmentLine.class,
              rs.getString("lineid"));
          String status = OBMessageUtils.messageBD("Efin_DecAmount_Greaterthan_FundsAmount");
          line.setFailureReason(status.replace("@", rs.getString("funds_available")));
          line.setAlertStatus("Failed");
          OBDal.getInstance().save(line);
        } else if (transactionType.equals("BD")) {
          EFINFundsReqLine line = OBDal.getInstance().get(EFINFundsReqLine.class,
              rs.getString("lineid"));
          String status = OBMessageUtils.messageBD("Efin_DecAmount_Greaterthan_FundsAmount");
          line.setFailureReason(status.replace("@", rs.getString("funds_available")));
          line.setAlertStatus("FL");
          OBDal.getInstance().save(line);
        }
        count += 1;
        return count;

      }
      log.debug("count:" + count);
    } catch (Exception e) {
      e.printStackTrace();
      if (log.isErrorEnabled()) {
        log.error("Exception in  isBudgetDecreaseAmountExceedsFundsAvail " + e, e);
      }
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();

    }
    return count;
  }

  /**
   * 
   * @param headerId
   * @param clientId
   * @param transactionType
   * @return count
   */
  public static int checkReactivateValidations(String headerId, String clientId,
      String transactionType) {
    int count = 0;
    Connection con = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      OBContext.setAdminMode();
      String query = "";
      con = OBDal.getInstance().getConnection();

      // check the budget control parameters isready flag =true
      final OBQuery<EfinBudgetControlParam> controlParam = OBDal.getInstance().createQuery(
          EfinBudgetControlParam.class,
          "client.id='" + OBContext.getOBContext().getCurrentClient().getId() + "'");

      if (controlParam.list().size() > 0) {

        EfinBudgetControlParam budgetCntrl = controlParam.list().get(0);
        if (budgetCntrl != null) {
          bcuOrgId = budgetCntrl.getBudgetcontrolunit().getId();
          costOrgId = budgetCntrl.getBudgetcontrolCostcenter().getId();
        }

      }

      if (transactionType != null) {

        query = "select typ.em_efin_budgettype,hd.c_campaign_id  as currentcamid , hd.efin_budgetint_id as budgetintid,"
            + "(select c_campaign_id from efin_budgetinquiry  where efin_budgetint_id =  hd. efin_budgetint_id "
            + "and c_campaign_id <> hd.c_campaign_id limit 1)as inqcamid  ";

        if (transactionType.equals("BR"))
          query += " from efin_budget_transfertrx hd ";
        if (transactionType.equals("BADJ"))
          query += " from efin_budgetadj hd ";
        if (transactionType.equals("BD"))
          query += " from efin_fundsreq hd ";

        query += "join c_campaign typ on typ.c_campaign_id=hd.c_campaign_id  where hd.ad_client_id = ? ";

        if (transactionType.equals("BR"))
          query += " and hd.efin_budget_transfertrx_id= ?  ";
        if (transactionType.equals("BADJ"))
          query += " and hd.efin_budgetadj_id= ?  ";
        if (transactionType.equals("BD"))
          query += " and hd.efin_fundsreq_id= ?  ";

        ps = con.prepareStatement(query);
        ps.setString(1, clientId);
        ps.setString(2, headerId);
        rs = ps.executeQuery();
        if (rs.next()) {
          if (rs.getString("em_efin_budgettype").equals("C")) {

            // checking after reactivate, funds available should not be less than zero in cost
            // budget
            count = isReactivateCostIncreaseAmountExceedCostBudgetFundsAvail(headerId, clientId,
                rs.getString("budgetintid"), transactionType, rs.getString("currentcamid"));

            // Checking after reactivate, funds available in cost budget should greater than funds
            // available in funds budget
            if (!transactionType.equals("BD")) {
              count += isReactivateCostBudgetDecreaseAmountExceedFundsBudget(headerId, clientId,
                  rs.getString("budgetintid"), transactionType, rs.getString("inqcamid"));
            }

          } else {

            // checking after reactivate, funds budget amount doesnt exceeds CostBudgetAmount
            if (!transactionType.equals("BD")) {
              count = isReactivateFundAmountExceedFromCostBudgetAmount(headerId, clientId,
                  rs.getString("budgetintid"), transactionType, rs.getString("inqcamid"));
            }

            // checking after reactivate, funds available should not be less than zero
            count += isReactivateBudgetIncreaseAmountExceedsFundsAvail(headerId, clientId,
                rs.getString("budgetintid"), transactionType, rs.getString("currentcamid"));

          }
        }
      }
      log.debug("count:" + count);
    } catch (Exception e) {
      e.printStackTrace();
      if (log.isErrorEnabled()) {
        log.error("Exception in  checkValidations " + e, e);
      }
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();
    }
    OBDal.getInstance().flush();
    return count;
  }

  public static int isDistributionCostBudgetAvailable(String fundsId, String clientId,
      String budInt, String transactionType, String tocampId, Connection conn) {

    String query = "";
    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    int count = 0;
    boolean ischkAcctPresntinCostBudget = false;
    EFINFundsReq req = null;
    try {
      OBContext.setAdminMode();

      if (fundsId != null) {
        req = OBDal.getInstance().get(EFINFundsReq.class, fundsId);
      }
      ischkAcctPresntinCostBudget = chkAcctPresntinCostBudget(req, null, null, conn);

      if (ischkAcctPresntinCostBudget) {
        query = "   select ln.efin_fundsreqline_id as lineid from efin_fundsreqline ln "
            + " left join c_validcombination funds on funds.c_validcombination_id =ln.toaccount"
            + "  where  (funds.ad_org_id||funds.c_salesregion_id||funds.user2_id||funds.user1_id||funds.c_activity_id||funds.account_id||"
            + " funds.c_project_id||funds.c_bpartner_id)  not in ( select (cos.ad_org_id||cos.c_salesregion_id||cos.user2_id||"
            + " cos.user1_id||cos.c_activity_id||cos.c_elementvalue_id||cos.c_project_id||cos.c_bpartner_id ) from"
            + "  efin_budgetinquiry cos         where    efin_budgetint_id=? "
            + "   and c_campaign_id= ? )and  ln.efin_fundsreq_id= ?  ";

        ps = con.prepareStatement(query);
        ps.setString(1, budInt);
        ps.setString(2, tocampId);
        ps.setString(3, fundsId);
        log.debug("isDistributionCostBudgetAvailable :" + ps.toString());
        rs = ps.executeQuery();
        while (rs.next()) {
          EFINFundsReqLine line = OBDal.getInstance().get(EFINFundsReqLine.class,
              rs.getString("lineid"));
          String status = OBMessageUtils.messageBD("EFIN_FundsReq_DistNotDoneCostBud");
          line.setAlertStatus("FL");
          line.setFailureReason(status);
          OBDal.getInstance().save(line);
          count += 1;
          log.debug("count funds :" + count);
          return count;
        }
      } else {
        count = 0;
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  isDistributionCostBudgetAvailable " + e, e);
      }
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();

    }
    return count;
  }

  @SuppressWarnings("unused")
  public static int isDistributionBudAdjBudRevCostBudgetAvailable(String headerId, String clientId,
      String budInt, String transactionType, String tocampId, Connection conn,
      EfinBudgetControlParam budgetCntrl) {

    String query = "";
    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    int count = 0;
    boolean ischkAcctPresntinCostBudget = false;
    EfinBudgetTransfertrx trx = null;
    BudgetAdjustment adj = null;
    String lineid = "", headerid = "", hdtable = "", lntable = "";
    try {
      OBContext.setAdminMode();

      if (transactionType.equals("BR")) {
        lineid = " efin_budget_transfertrxline_id ";
        headerid = " efin_budget_transfertrx_id ";
        hdtable = " efin_budget_transfertrx";
        lntable = " efin_budget_transfertrxline ";
      }
      if (transactionType.equals("BADJ")) {
        lineid = " efin_budgetadjline_id ";
        headerid = " efin_budgetadj_id ";
        hdtable = " efin_budgetadj";
        lntable = " efin_budgetadjline ";
      }

      if (headerId != null) {
        trx = OBDal.getInstance().get(EfinBudgetTransfertrx.class, headerId);
        adj = OBDal.getInstance().get(BudgetAdjustment.class, headerId);

      }

      ischkAcctPresntinCostBudget = chkAcctPresntinCostBudget(null, trx, adj, conn);

      if (ischkAcctPresntinCostBudget) {
        query = "   select  ln." + lineid + " as lineid from " + lntable + " ln "
            + "     left join efin_budget_ctrl_param param on param.ad_client_id=ln.ad_client_id "
            + "       left join c_validcombination funds on funds.c_validcombination_id =ln.c_validcombination_id and ln.increase > 0 "
            + "         where ln." + headerid + " =? ";
        if (transactionType.equals("BR"))
          query += "         and ln.distribute='Y' and ln.distribute_line_org is not null  and (ln.distribute_line_org||?||";
        if (transactionType.equals("BADJ"))
          query += "   and ln.isdistribute='Y' and ln.dislinkorg is not null and (ln.dislinkorg||?||";
        query += "          funds.user2_id||funds.user1_id||funds.c_activity_id||funds.account_id|| "
            + "      funds.c_project_id||funds.c_bpartner_id)  not in ( select (cos.ad_org_id||?||cos.user2_id|| "
            + "      cos.user1_id||cos.c_activity_id||cos.c_elementvalue_id||cos.c_project_id||cos.c_bpartner_id ) from "
            + "       efin_budgetinquiry cos         where    efin_budgetint_id= ?  "
            + "        and c_campaign_id= ? ) ";

        ps = con.prepareStatement(query);
        ps.setString(1, headerId);
        ps.setString(2, budgetCntrl.getBudgetcontrolCostcenter().getId());
        ps.setString(3, budgetCntrl.getBudgetcontrolCostcenter().getId());
        ps.setString(4, budInt);
        ps.setString(5, tocampId);
        log.debug("isDistributionCostBudgetAvailable :" + ps.toString());
        rs = ps.executeQuery();
        while (rs.next()) {
          if (transactionType.equals("BR")) {
            EfinBudgetTransfertrxline line = OBDal.getInstance()
                .get(EfinBudgetTransfertrxline.class, rs.getString("lineid"));
            String status = OBMessageUtils.messageBD("EFIN_Fin_Failure") + ""
                + OBMessageUtils.messageBD("EFIN_FundsReq_DistNotDoneCostBud");
            line.setStatus(status);
            OBDal.getInstance().save(line);
            count += 1;
          }
          if (transactionType.equals("BADJ")) {
            BudgetAdjustmentLine line = OBDal.getInstance().get(BudgetAdjustmentLine.class,
                rs.getString("lineid"));
            String status = OBMessageUtils.messageBD("EFIN_FundsReq_DistNotDoneCostBud");
            line.setFailureReason(status);
            line.setAlertStatus("Failed");
            OBDal.getInstance().save(line);
            count += 1;
          }
          log.debug("count funds :" + count);
          return count;
        }
      } else {
        count = 0;
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  isDistributionBudAdjBudRevCostBudgetAvailable " + e, e);
      }
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();

    }
    return count;
  }

  public static boolean chkAcctPresntinCostBudget(EFINFundsReq req, EfinBudgetTransfertrx trx,
      BudgetAdjustment adj, Connection conn) {
    PreparedStatement ps = null;
    ResultSet rs = null;
    Boolean ischkAcctPrestinCostBudt = false;
    String query = null;
    try {

      query = " select acct.c_elementvalue_id from efin_budgettype_acct acct "
          + "left join c_campaign cam on cam.c_campaign_id= acct.c_campaign_id " + " where "
          + " acct.c_elementvalue_id "
          + " in (  select replace(unnest(string_to_array(eut_getparentacct(?,null),',') ::character varying []),'''','')) and "
          + " acct.ad_client_id= ?  and cam.em_efin_iscarryforward='Y'  ";

      if (req != null) {
        for (EFINFundsReqLine reln : req.getEFINFundsReqLineList()) {
          if (reln.getToaccount() != null) {
            ps = conn.prepareStatement(query);
            ps.setString(1, reln.getToaccount().getAccount().getId());
            ps.setString(2, reln.getClient().getId());
            rs = ps.executeQuery();
            if (rs.next()) {
              ischkAcctPrestinCostBudt = true;
            }
            if (ischkAcctPrestinCostBudt)
              break;
          }
        }
      }
      if (trx != null) {
        for (EfinBudgetTransfertrxline ln : trx.getEfinBudgetTransfertrxlineList()) {
          if (ln.getIncrease().compareTo(BigDecimal.ZERO) > 0) {
            ps = conn.prepareStatement(query);
            ps.setString(1, ln.getAccountingCombination().getAccount().getId());
            ps.setString(2, ln.getClient().getId());
            rs = ps.executeQuery();
            if (rs.next()) {
              ischkAcctPrestinCostBudt = true;
            }
            if (ischkAcctPrestinCostBudt)
              break;
          }
        }
      }
      if (adj != null) {
        for (BudgetAdjustmentLine ln : adj.getEfinBudgetAdjlineList()) {
          if (ln.getIncrease().compareTo(BigDecimal.ZERO) > 0) {
            ps = conn.prepareStatement(query);
            ps.setString(1, ln.getAccountingCombination().getAccount().getId());
            ps.setString(2, ln.getClient().getId());
            rs = ps.executeQuery();
            if (rs.next()) {
              ischkAcctPrestinCostBudt = true;
            }
            if (ischkAcctPrestinCostBudt)
              break;
          }
        }
      }
      return ischkAcctPrestinCostBudt;

    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  chkAcctPresntinCostBudget " + e, e);
      }
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();

    }
    return ischkAcctPrestinCostBudt;
  }

  public static JSONObject CommonFundsChecking(EfinBudgetIntialization initial,
      AccountingCombination com, BigDecimal Amount) {
    OBQuery<EfinBudgetInquiry> budInq = null;
    JSONObject result = new JSONObject();
    String message = null;
    List<AccountingCombination> acctcomlist = new ArrayList<AccountingCombination>();
    try {
      result.put("errorFlag", "1");
      budInq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class, "efinBudgetint.id='"
          + initial.getId() + "'  and accountingCombination.id='" + com.getId() + "'");
      // if isdepartment fund yes, then check dept level distribution acct.
      if (com.isEFINDepartmentFund()) {
        if (budInq.list() != null && budInq.list().size() > 0) {
          for (EfinBudgetInquiry Enquiry : budInq.list()) {
            OBDal.getInstance().refresh(Enquiry);

            if (com.getId().equals(Enquiry.getAccountingCombination().getId())) {
              if (Amount.compareTo(Enquiry.getFundsAvailable()) > 0) {
                // funds not available
                message = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Cost");
                message = message.replace("@", Enquiry.getFundsAvailable().toString());
                result.put("errorFlag", "0");
                result.put("message", message);
              }
              result.put("FA", Enquiry.getFundsAvailable());
            }
          }
        } else {
          message = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Cost");
          message = message.replace("@", "0");
          result.put("errorFlag", "0");
          result.put("message", message);
          result.put("FA", BigDecimal.ZERO);
        }
      }
      // if isdepartment fund No, then check Org level distribution acct.
      else {
        acctcomlist = getParentAccountCom(com, com.getClient().getId());

        if (acctcomlist != null && acctcomlist.size() > 0) {
          AccountingCombination combination = acctcomlist.get(0);

          budInq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class, "efinBudgetint.id='"
              + initial.getId() + "'  and accountingCombination.id='" + combination.getId() + "'");
          if (budInq.list() != null && budInq.list().size() > 0) {
            for (EfinBudgetInquiry Enquiry : budInq.list()) {
              OBDal.getInstance().refresh(Enquiry);
              if (combination.getId().equals(Enquiry.getAccountingCombination().getId())) {
                if (Amount.compareTo(Enquiry.getFundsAvailable()) > 0) {
                  // funds not available
                  message = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Cost");
                  message = message.replace("@", Enquiry.getFundsAvailable().toString());
                  result.put("errorFlag", "0");
                  result.put("message", message);
                }
                result.put("FA", Enquiry.getFundsAvailable());
              }
            }
          } else {
            message = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Cost");
            message = message.replace("@", "0");
            result.put("errorFlag", "0");
            result.put("message", message);
            result.put("FA", BigDecimal.ZERO);
          }
        } else {
          message = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Cost");
          message = message.replace("@", "0");
          result.put("errorFlag", "0");
          result.put("message", message);
          result.put("FA", BigDecimal.ZERO);
        }
      }

    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  CommonFundsChecking " + e, e);
        try {
          result.put("FA", BigDecimal.ZERO);
        } catch (JSONException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
      }
    }
    return result;
  }

  /**
   * get parent account for dept level - dept funds is "No"
   * 
   * @param com
   * @param clientId
   * @return
   */
  public static List<AccountingCombination> getParentAccountCom(AccountingCombination com,
      String clientId) {
    List<AccountingCombination> acctlist = new ArrayList<AccountingCombination>();
    String department = null;
    try {
      EfinBudgetControlParam budgContrparam = FundsReqMangementDAO.getControlParam(clientId);
      department = budgContrparam.getBudgetcontrolCostcenter().getId();
      OBQuery<AccountingCombination> accountCommQry = OBDal.getInstance().createQuery(
          AccountingCombination.class,
          "account.id= '" + com.getAccount().getId() + "'" + " and businessPartner.id='"
              + com.getBusinessPartner().getId() + "' " + "and salesRegion.id='" + department
              + "' and project.id = '" + com.getProject().getId() + "' " + "and salesCampaign.id='"
              + com.getSalesCampaign().getId() + "' " + "and activity.id='"
              + com.getActivity().getId() + "' and stDimension.id='" + com.getStDimension().getId()
              + "' " + " and ndDimension.id = '" + com.getNdDimension().getId() + "' "
              + " and organization.id = '" + com.getOrganization().getId() + "'");

      log.debug("accountCommQry:" + accountCommQry.getWhereAndOrderBy());
      if (accountCommQry.list().size() > 0)
        return accountCommQry.list();

    } catch (Exception e) {
      log.error("Exception in getParentAccountCom " + e.getMessage());
    }
    return acctlist;

  }

  /**
   * 
   * @param validcombinationId
   * @param budintId
   * @param budgetTypeId
   * @param clientId
   * @return costCurrentBudget
   */
  public static BigDecimal getCostCurrentBudget(String validcombinationId, String budintId,
      String budgetTypeId, String clientId) {
    BigDecimal costCurrentBudget = BigDecimal.ZERO;
    String query = "";
    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String costValidCombId = null;
    try {
      OBContext.setAdminMode();
      Campaign budtype = OBDal.getInstance().get(Campaign.class, budgetTypeId);
      if (budtype.getEfinBudgettype().equals("F")) {
        AccountingCombination validcomb = OBDal.getInstance().get(AccountingCombination.class,
            validcombinationId);
        if (validcomb.getEfinCostcombination() != null) {
          costValidCombId = validcomb.getEfinCostcombination().getId();

          query = "select Funds_Available from efin_budgetinquiry inq"
              + " join efin_budgetint hd on inq.efin_budgetint_id=hd.efin_budgetint_id"
              + " where hd.efin_budgetint_id=? and inq.c_validcombination_id=? and hd.ad_client_id=?";

          log.debug("Budget enquiry:" + query.toString());
          ps = con.prepareStatement(query);
          ps.setString(1, budintId);
          ps.setString(2, costValidCombId);
          ps.setString(3, clientId);
          rs = ps.executeQuery();
          if (rs.next()) {
            if (rs.getBigDecimal("Funds_Available") != null) {
              costCurrentBudget = rs.getBigDecimal("Funds_Available");
            } else {
              costCurrentBudget = BigDecimal.ZERO;
            }
          }
        }
      }
      log.debug("costCurrentBudget:" + costCurrentBudget);
      return costCurrentBudget;

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in getCostCurrentBudget " + e.getMessage(), e);
    } finally {

      OBContext.restorePreviousMode();
    }
    return costCurrentBudget;
  }

  /**
   * 
   * @param validcombinationId
   * @param budintId
   * @param budgetTypeId
   * @param clientId
   * @return fundCurrentBudget
   */
  public static BigDecimal getFundCurrentBudget(String validcombinationId, String budintId,
      String budgetTypeId, String clientId) {
    BigDecimal fundCurrentBudget = BigDecimal.ZERO;
    String query = "";
    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String fundValidCombId = null;
    try {
      OBContext.setAdminMode();
      Campaign budtype = OBDal.getInstance().get(Campaign.class, budgetTypeId);
      if (budtype.getEfinBudgettype().equals("C")) {
        AccountingCombination validcomb = OBDal.getInstance().get(AccountingCombination.class,
            validcombinationId);
        if (validcomb.getEfinFundscombination() != null) {
          fundValidCombId = validcomb.getEfinFundscombination().getId();

          query = "select Funds_Available from efin_budgetinquiry inq"
              + " join efin_budgetint hd on inq.efin_budgetint_id=hd.efin_budgetint_id"
              + " where hd.efin_budgetint_id=? and inq.c_validcombination_id=? and hd.ad_client_id=?";

          log.debug("Budget enquiry:" + query.toString());
          ps = con.prepareStatement(query);
          ps.setString(1, budintId);
          ps.setString(2, fundValidCombId);
          ps.setString(3, clientId);
          rs = ps.executeQuery();
          if (rs.next()) {
            if (rs.getBigDecimal("Funds_Available") != null) {
              fundCurrentBudget = rs.getBigDecimal("Funds_Available");
            } else {
              fundCurrentBudget = BigDecimal.ZERO;
            }
          }
        }
      }
      log.debug("fundCurrentBudget:" + fundCurrentBudget);
      return fundCurrentBudget;

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in getFundCurrentBudget " + e.getMessage(), e);
    } finally {

      OBContext.restorePreviousMode();
    }
    return fundCurrentBudget;
  }

  public static String getEncumbranceLineId(String strManualEncumbranceId,
      String strCombinationId) {
    String strLineId = "";
    try {
      OBQuery<EfinBudgetManencumlines> linesQuery = OBDal.getInstance().createQuery(
          EfinBudgetManencumlines.class,
          " where manualEncumbrance.id = :encumbranceId and accountingCombination.id = :combinationID ");
      linesQuery.setNamedParameter("encumbranceId", strManualEncumbranceId);
      linesQuery.setNamedParameter("combinationID", strCombinationId);

      if (linesQuery != null && linesQuery.list().size() > 0) {
        EfinBudgetManencumlines lines = linesQuery.list().get(0);
        strLineId = lines.getId();
      }

    } catch (Exception e) {
      log.error("Exception in getEncumbranceLineId " + e.getMessage(), e);
      e.printStackTrace();
    }
    return strLineId;
  }

  /**
   * Check UserRole are allowed to submit the record
   * 
   * @param tableName
   * @param userId
   * @param roleId
   * @param recordId
   * @param keyColumn
   * @return allowed as String
   */
  public static int checkUserRoleForSubmit(String tableName, String userId, String roleId,
      String recordId, String keyColumn) {
    int allowed = 0;
    try {
      OBContext.setAdminMode();
      StringBuilder queryBuilder = new StringBuilder();

      queryBuilder.append(" select count(").append(keyColumn).append(") as count from ")
          .append(tableName);
      queryBuilder.append(" where exists (select 1 from ").append(tableName);
      queryBuilder.append(" where ").append(keyColumn).append("=?");
      queryBuilder.append(" and (createdby = ? ");
      if ("gl_journal".equals(tableName) || "c_invoice".equals(tableName))
        queryBuilder.append(" or em_efin_ad_role_id=?)) ");
      else
        queryBuilder.append(" or ad_role_id=?)) ");
      queryBuilder.append(" and ").append(keyColumn).append(" = ?");

      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(queryBuilder.toString());
      query.setParameter(0, recordId);
      query.setParameter(1, userId);
      query.setParameter(2, roleId);
      query.setParameter(3, recordId);
      log.debug(" Query: " + query.getQueryString());

      if (query != null) {
        // List<Object> rows = query.list();
        if (query.list().size() > 0) {
          allowed = Integer.parseInt(query.list().get(0).toString());
        }
      }
    } catch (Exception e) {
      allowed = 0;
      log.error("Exception while checkUserRoleForSubmit(): ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return allowed;
  }
}
