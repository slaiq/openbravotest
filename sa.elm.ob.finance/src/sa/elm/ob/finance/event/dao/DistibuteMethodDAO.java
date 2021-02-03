/*
 * All Rights Reserved By Qualian Technologies Pvt Ltd.
 */
package sa.elm.ob.finance.event.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;

import sa.elm.ob.finance.BudgetAdjustment;
import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EfinBudgetTransfertrx;

public class DistibuteMethodDAO {
  @SuppressWarnings("unused")
  private ConnectionProvider conn = null;

  private static final Logger LOG = Logger.getLogger(DistibuteMethodDAO.class);

  public DistibuteMethodDAO(ConnectionProvider con) {
    this.conn = con;
  }

  @SuppressWarnings("resource")
  public Boolean checkDistUniquecodePresntOrNot(EfinBudgetTransfertrx trxline,
      BudgetAdjustment adjline, ConnectionProvider conn, String headerId, EFINBudget budget,
      String distributeOrgIdheader) {
    String strQry = null, lineid = null, isdistcol = null, lntable = null,
        headerid = null, query = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    Boolean errorflag = false;
    try {
      if (trxline != null) {
        lineid = " efin_budget_transfertrxline_id ";
        headerid = " efin_budget_transfertrx_id ";
        isdistcol = " distribute ";
        lntable = " efin_budget_transfertrxline ";
      }
      if (adjline != null) {
        lineid = " efin_budgetadjline_id ";
        headerid = " efin_budgetadj_id ";
        isdistcol = " isdistribute ";
        lntable = " efin_budgetadjline ";
      }
      if (budget != null) {
        lineid = " efin_budgetlines_id ";
        headerid = " efin_budget_id ";
        isdistcol = " isdistribute ";
        lntable = " efin_budgetlines ";
      }

      strQry = " select  com.c_validcombination_id as comid , lncom.c_validcombination_id as lncomid ,ln."
          + lineid + " as lineid from " + lntable + " ln "
          + " left join c_validcombination lncom on lncom.c_validcombination_id= ln.c_validcombination_id "
          + " join efin_budget_ctrl_param para on para.ad_client_id=ln.ad_client_id "
          + " left join c_validcombination com on com.ad_org_id='" + distributeOrgIdheader + "'"
          + " and com.c_salesregion_id= para.budgetcontrol_costcenter and com.account_id= lncom.account_id "
          + " where 1=1   and  " + isdistcol + " ='Y'  " + "  and ln." + headerid + "= ? ";

      ps = conn.getPreparedStatement(strQry);
      ps.setString(1, headerId);
      LOG.debug("checkDistUniquecodePresntOrNot:" + ps.toString());
      rs = ps.executeQuery();
      while (rs.next()) {

        // update the line status
        if (trxline != null) {
          if (rs.getString("comid") == null && trxline != null) {
            query = "update efin_budget_transfertrxline set distribute_line_org = null,Distribute='N' where efin_budget_transfertrxline_id = ? ";
            ps = conn.getPreparedStatement(query);
            ps.setString(1, rs.getString("lineid"));
            ps.executeUpdate();
          } else {
            query = "update efin_budget_transfertrxline set distribute_line_org = ? where efin_budget_transfertrxline_id = ? ";
            ps = conn.getPreparedStatement(query);
            ps.setString(1, distributeOrgIdheader);
            ps.setString(2, rs.getString("lineid"));
            ps.executeUpdate();
          }

        }
        if (adjline != null) {
          if (rs.getString("comid") == null && adjline != null) {
            query = "update efin_budgetadjline set dislinkorg = null,isdistribute='N' where efin_budgetadjline_id = ? ";
            ps = conn.getPreparedStatement(query);
            ps.setString(1, rs.getString("lineid"));
            ps.executeUpdate();
          } else {
            query = "update efin_budgetadjline set dislinkorg = ? where efin_budgetadjline_id = ? ";
            ps = conn.getPreparedStatement(query);
            ps.setString(1, distributeOrgIdheader);
            ps.setString(2, rs.getString("lineid"));
            ps.executeUpdate();
          }
        }
        if (budget != null) {
          if (rs.getString("comid") == null && budget != null) {
            query = "update efin_budgetlines set dislinkorg = null,isdistribute='N' where efin_budgetlines_id = ? ";
            ps = conn.getPreparedStatement(query);
            ps.setString(1, rs.getString("lineid"));
            ps.executeUpdate();
          } else {
            query = "update efin_budgetlines set dislinkorg = ? where efin_budgetlines_id = ? ";
            ps = conn.getPreparedStatement(query);
            ps.setString(1, distributeOrgIdheader);
            ps.setString(2, rs.getString("lineid"));
            ps.executeUpdate();
          }
        }

      }

      if (!errorflag)
        return true;
      else
        return false;
    } catch (Exception e) {
      LOG.error("Exception in checkDistUniquecodePresntOrNot: ", e);
      OBDal.getInstance().rollbackAndClose();
      return false;
    } finally {
    }
  }
}