package sa.elm.ob.finance.util;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import org.hibernate.SQLQuery;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.BudgetAdjustmentLine;
import sa.elm.ob.finance.EFINFundsReqLine;
import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.EfinBudgetTransfertrxline;

public class BudgetRevisionRuleValidationDAO {
  private static final Logger LOG = LoggerFactory.getLogger(BudgetRevisionRuleValidationDAO.class);

  @SuppressWarnings({ "unchecked" })
  public int checkRevisionRules(String windowCode, String headerId, String clientId,
      BigDecimal percentage, String str_operator, String budgetIntId, Boolean isWarn) {
    String lineid = null, headerid = null, lntable = null, str_query = "", select_query = "",
        whereClause = "";
    int returncount = 0;
    try {
      if (updateStatusBeforeValidation(windowCode, headerId, clientId) > 0) {

        if (windowCode.equals("BR")) {
          lineid = " efin_budget_transfertrxline_id ";
          headerid = " efin_budget_transfertrx_id ";
          lntable = " efin_budget_transfertrxline ";
        }
        if (windowCode.equals("BA")) {
          lineid = " efin_budgetadjline_id ";
          headerid = " efin_budgetadj_id ";
          lntable = " efin_budgetadjline ";
        }
        if (windowCode.equals("BD")) {
          lineid = " efin_fundsreqline_id ";
          headerid = " efin_fundsreq_id ";
          lntable = " efin_fundsreqline ";
        }
        select_query = " select t1." + lineid + " as lnid from " + lntable + " t1 "
            + " join efin_budgetinquiry t2 on ";
        if (windowCode != null && (windowCode.equals("BA") || windowCode.equals("BR")))
          select_query += "  t1.c_validcombination_id= t2.c_validcombination_id ";
        else
          select_query += "  t1.fromaccount= t2.c_validcombination_id ";
        whereClause = " where 1=1 and t2.efin_budgetint_id='" + budgetIntId + "' ";
        if (windowCode.equals("BA")) {
          whereClause += " and t1." + headerid + " =:headerId ";
          whereClause += " and ( (t1.increase > ((t2.current_budget * " + percentage.intValue()
              + ")/100)  )  ";
          whereClause += " or (t1.decrease > ((t2.current_budget * " + percentage.intValue()
              + ")/100) ) )";
          /*
           * if (str_operator.equals("LTET")) { whereClause +=
           * " and t1.decrease <= round((t2.rev_amount * " + percentage.intValue() + ")/100) "; }
           * else if (str_operator.equals("MTET")) { whereClause +=
           * " and t1.increase >= round((t2.rev_amount * " + percentage.intValue() + ")/100) "; }
           * else if (str_operator.equals("MT")) { whereClause +=
           * " and t1.increase > round((t2.rev_amount * " + percentage.intValue() + ")/100) "; }
           * else { whereClause += " and 1=1"; }
           */
        } else if (windowCode.equals("BD")) {
          whereClause += " and t1." + headerid + " =:headerId ";
          whereClause += " and ( (t1.increase > ((t2.current_budget * " + percentage.intValue()
              + ")/100)) ";
          whereClause += " or ( t1.decrease > ((t2.current_budget * " + percentage.intValue()
              + ")/100) ) )";

          /*
           * if (str_operator.equals("LTET")) { whereClause +=
           * " and t1.decrease <= round((t2.current_budget * " + percentage.intValue() + ")/100) ";
           * } else if (str_operator.equals("MTET")) { whereClause +=
           * " and t1.increase >= round((t2.current_budget * " + percentage.intValue() + ")/100) ";
           * } else if (str_operator.equals("MT")) { whereClause +=
           * " and t1.increase > round((t2.current_budget * " + percentage.intValue() + ")/100) "; }
           * else { whereClause += " and 1=1"; }
           */
        } else {
          whereClause += " and t1." + headerid + " =:headerId ";
          whereClause += " and ((t1.increase > ((t2.current_budget * " + percentage.intValue()
              + ")/100))";
          whereClause += " or ( t1.decrease > ((t2.current_budget * " + percentage.intValue()
              + ")/100) ) )";
          /*
           * if (str_operator.equals("LTET")) { whereClause +=
           * " and t1.decrease <= round((t2.rev_amount * " + percentage.intValue() + ")/100) "; }
           * else if (str_operator.equals("MTET")) { whereClause +=
           * " and t1.increase >= round((t2.rev_amount * " + percentage.intValue() + ")/100) "; }
           * else if (str_operator.equals("MT")) { whereClause +=
           * " and t1.increase > round((t2.rev_amount * " + percentage.intValue() + ")/100) "; }
           * else { whereClause += " and 1=1"; }
           */
        }
        str_query = select_query.concat(whereClause);
        SQLQuery sqlquery = OBDal.getInstance().getSession().createSQLQuery(str_query);
        sqlquery.setParameter("headerId", headerId);
        List<Object> rows = sqlquery.list();
        if (rows.size() > 0) {
          for (int i = 0; i < rows.size(); i++) {
            Object object = rows.get(i);
            if (windowCode.equals("BR")) {
              EfinBudgetTransfertrxline line = OBDal.getInstance()
                  .get(EfinBudgetTransfertrxline.class, object.toString());

              AccountingCombination acctComb = OBDal.getInstance().get(AccountingCombination.class,
                  line.getAccountingCombination().getId());
              if (acctComb != null) {
                OBQuery<EfinBudgetControlParam> budgCtrlParam = OBDal.getInstance().createQuery(
                    EfinBudgetControlParam.class, "as e where e.budgetcontrolunit.id ='"
                        + acctComb.getSalesRegion().getId() + "'");

                if (budgCtrlParam.list().size() == 0) {
                  String status = "";
                  if (isWarn) {
                    status = OBMessageUtils.messageBD("Efin_Warning") + ": "
                        + OBMessageUtils.messageBD("Efin_Budget_Revision_Rules");
                  } else {
                    status = OBMessageUtils.messageBD("Efin_Failed") + ": "
                        + OBMessageUtils.messageBD("Efin_Budget_Revision_Rules");
                  }

                  line.setStatus(status);
                  OBDal.getInstance().save(line);
                  returncount += 1;
                }
              }
            } else if (windowCode.equals("BA")) {
              BudgetAdjustmentLine line = OBDal.getInstance().get(BudgetAdjustmentLine.class,
                  object.toString());
              String status = "", alertStatus = "";
              if (isWarn) {
                status = OBMessageUtils.messageBD("Efin_Budget_Revision_Rules");
                alertStatus = OBMessageUtils.messageBD("Efin_Warning");
              } else {
                status = OBMessageUtils.messageBD("Efin_Budget_Revision_Rules");
                alertStatus = OBMessageUtils.messageBD("Efin_Failed");
              }
              line.setFailureReason(status);
              line.setAlertStatus(alertStatus);
              OBDal.getInstance().save(line);
              returncount += 1;
            } else if (windowCode.equals("BD")) {
              EFINFundsReqLine line = OBDal.getInstance().get(EFINFundsReqLine.class,
                  object.toString());
              String status = "", alertStatus = "";
              if (isWarn) {
                status = OBMessageUtils.messageBD("Efin_Budget_Revision_Rules");
                alertStatus = "WAR";
              } else {
                status = OBMessageUtils.messageBD("Efin_Budget_Revision_Rules");
                alertStatus = "FL";
              }
              line.setFailureReason(status);
              line.setAlertStatus(alertStatus);
              returncount += 1;
            }
          }
        }
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in  checkRevisionRules " + e, e);
      }
    }
    // TODO Auto-generated method stub
    return returncount;
  }

  public static int updateStatusBeforeValidation(String windowCode, String headerId,
      String clientId) {
    int count = 0;
    String query = "", header = null, tbName = null;
    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    try {
      OBContext.setAdminMode();

      if (windowCode.equals("BR")) {
        tbName = "efin_budget_transfertrxline";
        header = " efin_budget_transfertrx_id ";
        query = "update " + tbName + " set status  ='Success' ";
      }
      if (windowCode.equals("BA")) {
        tbName = "efin_budgetadjline";
        header = " efin_budgetadj_id ";
        query = "update " + tbName + " set status  ='Success', Failure_Reason=null ";
      }
      if (windowCode.equals("BD")) {
        tbName = "efin_fundsreqline";
        header = " efin_fundsreq_id ";
        query = "update " + tbName + " set status  ='SCS', Failure_Reason=null ";
      }
      query = query + " where " + header + "= ? ";
      ps = con.prepareStatement(query);
      ps.setString(1, headerId);
      LOG.debug("ps :" + ps.toString());
      count = ps.executeUpdate();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in  updating the status " + e, e);
      }
      return 0;
    } finally {
      // close db connection
      try {
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();

    }
    return count;
  }

}
