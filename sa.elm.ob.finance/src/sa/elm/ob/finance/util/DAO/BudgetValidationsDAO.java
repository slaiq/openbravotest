/*
 *************************************************************************
 * All Rights Reserved.
 * Contributor(s):  Qualian
 ************************************************************************
 */
package sa.elm.ob.finance.util.DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EFINBudgetLines;
import sa.elm.ob.finance.EFINBudgetTypeAcct;
import sa.elm.ob.finance.util.budget.BudgetingUtilsService;
import sa.elm.ob.finance.util.budget.BudgetingUtilsServiceImpl;

/**
 * @author Priyanka Ranjan on 30/09/2017
 */

// Common method for Budget Validation
public class BudgetValidationsDAO {

  private static final Logger LOG = LoggerFactory.getLogger(BudgetValidationsDAO.class);
  private static int count = 0;

  /**
   * 
   * @param headerId
   * @param clientId
   * @return count
   */
  public static int BudgetValidations(String headerId, String clientId) {
    Connection con = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      OBContext.setAdminMode();
      boolean isCostAvail = false;
      String query = "";
      con = OBDal.getInstance().getConnection();
      int count = 0;

      EFINBudget budget = OBDal.getInstance().get(EFINBudget.class, headerId);
      if (budget.getSalesCampaign().getEfinBudgettype().equals("F")) {
        // check inactive lines contains or not.
        count = isAccountActive(headerId, clientId);
        if (count == 0) {

          // check account is present in budget type cost also
          isCostAvail = checkCostBudgetIsavailable(budget.getAccountElement().getId(), clientId);
          if (isCostAvail) {

            // Checking Cost Budget Available for budget Initialization With Same accounting
            // Element
            query = "select bud.efin_budget_id from efin_budget bud "
                + "join c_campaign typ on typ.c_campaign_id=bud.c_campaign_id"
                + " where  typ.em_efin_budgettype ='C' and bud.c_elementvalue_id='"
                + budget.getAccountElement().getId() + "' and bud.efin_budgetint_id='"
                + budget.getEfinBudgetint().getId()
                + "' and bud.status='APP' and bud.ad_client_id='" + clientId + "' ";
            LOG.debug("CostBudgetQuery:" + query.toString());
            ps = con.prepareStatement(query);
            rs = ps.executeQuery();
            if (rs.next()) {
              // Checking Funds Budget Extra Account Combination from Cost Budget
              count = isExtraAccountCombination(budget.getId(), clientId,
                  budget.getEfinBudgetint().getId());

              // Checking Funds Budget lines amount exceed from Cost Budget lines amount
              count = count + isAmountExceedFromCostBudgetAmount(budget.getId(), clientId,
                  budget.getEfinBudgetint().getId());

            }
            OBDal.getInstance().flush();
          }
        } else {
          // error- for inactive lines.
          count = 1;
          return count;
        }
      }
      return count;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in  BudgetValidations " + e, e);
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
   * 
   * @param ParentAccount
   * @param client
   * @return true or false
   */
  public static boolean checkCostBudgetIsavailable(String ParentAccount, String client) {
    OBQuery<EFINBudgetTypeAcct> budtype = OBDal.getInstance().createQuery(EFINBudgetTypeAcct.class,
        "accountElement='" + ParentAccount
            + "' and salesCampaign.efinBudgettype='C' and client.id='" + client + "'");
    if (budtype.list() != null && budtype.list().size() > 0) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * 
   * @param headerid
   * @param clientId
   * @param budInt
   * @return count
   */
  public static int isExtraAccountCombination(String headerid, String clientId, String budInt) {
    String query = "";
    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    int count = 0;
    try {
      OBContext.setAdminMode();
      Boolean isFundsOnly = Boolean.FALSE;
      BudgetingUtilsService serviceDAO = new BudgetingUtilsServiceImpl();

      query = "select funds.efin_budgetlines_id from efin_budgetlines funds"
          + " join efin_budget hd on hd.efin_budget_id=funds.efin_budget_id"
          + " join c_validcombination vc on funds.c_validcombination_id=vc.c_validcombination_id"
          + " where vc.em_efin_costcombination not in (select c_validcombination_id from efin_budgetinquiry cos"
          + " where cos.efin_budgetint_id =?) and funds.amount <> 0 and hd.efin_budget_id = ? and hd.ad_client_id=?";

      LOG.debug("extraLinesQuery:" + query.toString());
      ps = con.prepareStatement(query);
      ps.setString(1, budInt);
      ps.setString(2, headerid);
      ps.setString(3, clientId);
      rs = ps.executeQuery();
      while (rs.next()) {
        EFINBudgetLines fundsBudgetLines = OBDal.getInstance().get(EFINBudgetLines.class,
            rs.getString("efin_budgetlines_id"));
        isFundsOnly = Boolean.FALSE;

        if (fundsBudgetLines != null) {
          isFundsOnly = serviceDAO.isFundsOnlyAccount(fundsBudgetLines.getAccountElement().getId(),
              fundsBudgetLines.getClient().getId());
        }

        if (!isFundsOnly) {
          fundsBudgetLines.setCheckingStaus("FL");
          fundsBudgetLines.setCheckingStausFailure(
              OBMessageUtils.messageBD("Efin_Combination_doesnot_Exist_inCost"));
          count = 2;
        }
      }

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in  isExtraAccountCombination " + e, e);
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
   * @param headerid
   * @param clientId
   * @param budInt
   * @return count
   */
  public static int isAmountExceedFromCostBudgetAmount(String headerid, String clientId,
      String budInt) {

    String query = "";
    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    int count = 0;
    BudgetingUtilsService budUtil = new BudgetingUtilsServiceImpl();
    try {
      OBContext.setAdminMode();
      query = "select funds.efin_budgetlines_id as lineid,funds.amount as fundsamount ,cos.current_budget as costamount "
          + " from efin_budgetlines funds "
          + " join efin_budget hd on hd.efin_budget_id=funds.efin_budget_id "
          + " join c_validcombination vc on funds.c_validcombination_id=vc.c_validcombination_id "
          + " join efin_budgetinquiry cos on vc.em_efin_costcombination=cos.c_validcombination_id "
          + " and cos.efin_budgetint_id = ? "
          + " where  1=1 and funds.amount > cos.current_budget and funds.amount <> 0 and hd.efin_budget_id=? and hd.ad_client_id=?";

      LOG.debug("extraAmountQury:" + query.toString());
      ps = con.prepareStatement(query);
      ps.setString(1, budInt);
      ps.setString(2, headerid);
      ps.setString(3, clientId);
      rs = ps.executeQuery();
      while (rs.next()) {
        EFINBudgetLines FundsBudgetLines = OBDal.getInstance().get(EFINBudgetLines.class,
            rs.getString("lineid"));
        if (!budUtil.isFundsOnlyAccount(FundsBudgetLines.getAccountElement().getId(), clientId)) {
          FundsBudgetLines.setCheckingStaus("FL");
          String status = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_FundsCost");
          FundsBudgetLines.setCheckingStausFailure(status.replace("@", rs.getString("costamount")));
          count = 2;
        } else {
          FundsBudgetLines.setCheckingStaus("SCS");
          FundsBudgetLines.setCheckingStausFailure("");
        }
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in  isAmountExceedFromCostBudgetAmount " + e, e);
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
   * @param headerid
   * @param clientId
   * @return count
   */
  public static int isAccountActive(String headerid, String clientId) {
    int count = 0;
    try {
      OBContext.setAdminMode();
      OBQuery<EFINBudgetLines> lines = OBDal.getInstance().createQuery(EFINBudgetLines.class,
          " as e where e.efinBudget.id ='" + headerid + "' and e.client.id='" + clientId
              + "' and e.active='N'");
      // lines.setFilterOnActive(false);
      count = lines.list().size();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in  isAccountActive " + e, e);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }
}
