/*
 * All Rights Reserved By Qualian Technologies Pvt Ltd.
 */
package sa.elm.ob.finance.event.dao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.SQLQuery;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.BudgetAdjustmentLine;
import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EFINBudgetLines;
import sa.elm.ob.finance.EFINBudgetTypeAcct;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetTransfertrxline;
import sa.elm.ob.finance.ad_process.budget.BudgetDAO;
import sa.elm.ob.finance.util.budget.BudgetingUtilsService;
import sa.elm.ob.finance.util.budget.BudgetingUtilsServiceImpl;

/**
 * @author Gowtham.V
 */
public class BudgetLinesDAO {
  private static final Logger LOG = LoggerFactory.getLogger(BudgetLinesDAO.class);

  /**
   * This method is used to insert budget lines with valid combination while onSave of budget
   * header.
   * 
   * @param clientId
   * @param budget
   * @param Department
   * @return 0 -->No lines added 1 -->Success 2 -->Error.
   */
  public static int insertBudegtLines(String clientId, EFINBudget budget, String department,
      String organization, List<Object> budgetLineList) {
    final List<Object> parameters = new ArrayList<Object>();
    final BigDecimal amount = BigDecimal.ZERO;
    long lineNo = 10;
    int returnValue = 0;
    try {

      // get valid combinations from account combination.
      final OBQuery<AccountingCombination> validCombination = OBDal.getInstance().createQuery(
          AccountingCombination.class,
          "salesCampaign.id = ? and salesRegion.id = ? and organization.id = ? and efinUniqueCode <> '' "
              + " and account.id in (select e.node from ADTreeNode as e where e.reportSet= ? and e.client.id= ? ) "
              + " and account.accountType = 'E' "
              + "and id not in (select e.accountingCombination.id from EFIN_BudgetLines as e where efinBudget.id = ? )");
      parameters.add(budget.getSalesCampaign().getId());
      parameters.add(department);
      parameters.add(organization);
      parameters.add(budget.getAccountElement().getId());
      parameters.add(clientId);
      parameters.add(budget.getId());
      validCombination.setParameters(parameters);
      List<AccountingCombination> validCombinationList = validCombination.list();
      if (validCombinationList != null && validCombinationList.size() > 0) {
        for (AccountingCombination uniquecode : validCombinationList) {
          final EFINBudgetLines budgetLine = OBProvider.getInstance().get(EFINBudgetLines.class);
          budgetLine.setOrganization(uniquecode.getOrganization());
          budgetLine.setAccountElement(uniquecode.getAccount());
          budgetLine.setEfinBudget(budget);
          budgetLine.setActivity(uniquecode.getActivity());
          budgetLine.setStDimension(uniquecode.getStDimension());
          budgetLine.setNdDimension(uniquecode.getNdDimension());
          budgetLine.setSalesRegion(uniquecode.getSalesRegion());
          budgetLine.setSalesCampaign(uniquecode.getSalesCampaign());
          budgetLine.setBusinessPartner(uniquecode.getBusinessPartner());
          budgetLine.setProject(uniquecode.getProject());
          budgetLine.setUniquecode(uniquecode.getEfinUniqueCode());
          budgetLine.setAccountingCombination(uniquecode);
          budgetLine.setUniqueCodeName(uniquecode.getEfinUniquecodename());
          budgetLine.setLineNo(lineNo);
          budgetLine.setAmount(amount);
          lineNo += 10;
          budgetLineList.add(budgetLine);
          // OBDal.getInstance().save(budgetLine);
        }
        returnValue = 1; // Success.
      } else {
        returnValue = 0; // No lines.
      }
      return returnValue;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in insertBudegtLines() " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
      return 2; // process failed.
    }
  }

  /**
   * Check whether having lines or not.
   * 
   * @param budget
   * @return
   */
  @SuppressWarnings("unchecked")
  public static int getHeaderhaveLine(EFINBudget budget) {
    try {
      List<Object> line = null;
      SQLQuery budgetline = OBDal.getInstance().getSession()
          .createSQLQuery("select EFIN_BudgetLines_id  from EFIN_BudgetLines line"
              + " join EFIN_Budget head on head.EFIN_Budget_ID=line.EFIN_Budget_ID"
              + " where head.EFIN_Budget_ID= :budgetID ");
      budgetline.setParameter("budgetID", budget.getId());
      line = budgetline.list();
      if (line != null && line.size() > 0) {
        return line.size();
      } else {
        return 0;
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in delete event in budget " + e, e);
      }
      return 0;
    }
  }

  /**
   * Check funds budget combination is already added or not.
   * 
   * @param budget
   * @return
   */
  @SuppressWarnings("unchecked")
  public static int checkFundsBudgetCombination(EFINBudget budget) {
    try {
      List<Object> line = null;
      SQLQuery fundsquery = OBDal.getInstance().getSession().createSQLQuery(
          "select efin_budget_id from efin_budget bud join c_campaign cam on bud.c_campaign_id = cam.c_campaign_id where cam.em_efin_budgettype='F' and c_year_id ='"
              + budget.getYear().getId() + "' and c_elementvalue_id='"
              + budget.getAccountElement().getId() + "' and bud.c_campaign_id='"
              + budget.getSalesCampaign().getId() + "' and bud.ad_client_id ='"
              + budget.getClient().getId() + "'");
      line = fundsquery.list();
      if (line != null && line.size() > 0) {
        return line.size();
      } else {
        return 0;
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in checking funds combination in budget header " + e, e);
      }
      return 0;
    }
  }

  /**
   * Check cost budget combination is already added or not.
   * 
   * @param budget
   * @return
   */
  @SuppressWarnings("unchecked")
  public static int checkCostBudgetCombination(EFINBudget budget) {
    try {
      List<Object> line = null;
      SQLQuery costquery = OBDal.getInstance().getSession().createSQLQuery(
          "select efin_budget_id from efin_budget bud join c_campaign cam on bud.c_campaign_id = cam.c_campaign_id where cam.em_efin_budgettype='C' and c_year_id ='"
              + budget.getYear().getId() + "' and c_elementvalue_id='"
              + budget.getAccountElement().getId() + "' and bud.c_campaign_id='"
              + budget.getSalesCampaign().getId() + "' and bud.ad_client_id ='"
              + budget.getClient().getId() + "'");
      line = costquery.list();
      if (line != null && line.size() > 0) {
        return line.size();
      } else {
        return 0;
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in checking cost combination in budget header " + e, e);
      }
      return 0;
    }
  }

  /**
   * To check cost budget is already created or not.
   * 
   * @param budget
   * @return
   */
  public static boolean checkCostBudgetAlreadyCreated(EFINBudget budget) {
    try {
      final List<Object> parameters = new ArrayList<Object>();
      OBQuery<EFINBudgetTypeAcct> budgetTypeAcct = OBDal.getInstance().createQuery(
          EFINBudgetTypeAcct.class, "salesCampaign.efinBudgettype ='C' and accountElement.id = '"
              + budget.getAccountElement().getId() + "'");
      if (budgetTypeAcct.list() != null && budgetTypeAcct.list().size() > 0) {
        OBQuery<EFINBudget> costBudget = OBDal.getInstance().createQuery(EFINBudget.class,
            "efinBudgetint.id = ? and accountElement.id = ? and salesCampaign.efinBudgettype ='C' ");
        parameters.add(budget.getEfinBudgetint().getId());
        parameters.add(budget.getAccountElement().getId());
        costBudget.setParameters(parameters);
        if (costBudget.list() == null || costBudget.list().size() == 0) {
          return true;
        }
      }
      return false;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in checking cost budget created or not." + e, e);
      }
      return false;
    }
  }

  /**
   * To check funds budget created for corresponding cost budget
   * 
   * @param budget
   * @return
   */
  public static boolean checkFundsBudgetCreated(EFINBudget budget) {
    BudgetingUtilsService budUtil = new BudgetingUtilsServiceImpl();
    try {
      final List<Object> parameters = new ArrayList<Object>();
      final List<Object> parameter = new ArrayList<Object>();

      OBQuery<EFINBudgetTypeAcct> budgetTypeAcct = OBDal.getInstance().createQuery(
          EFINBudgetTypeAcct.class, "salesCampaign.efinBudgettype ='F' and accountElement.id = '"
              + budget.getAccountElement().getId() + "'");
      if (budgetTypeAcct.list() != null && budgetTypeAcct.list().size() > 0) {
        OBQuery<EFINBudget> fundsBudget = OBDal.getInstance().createQuery(EFINBudget.class,
            "efinBudgetint.id = ? and accountElement.id = ? and salesCampaign.efinBudgettype ='F' ");
        parameters.add(budget.getEfinBudgetint().getId());
        parameters.add(budget.getAccountElement().getId());
        fundsBudget.setParameters(parameters);
        if (fundsBudget.list() != null && fundsBudget.list().size() > 0) {
          // get funds current budget for cost.
          for (EFINBudgetLines cstLine : budget.getEFINBudgetLinesList()) {
            if (!budUtil.isFundsOnlyAccount(cstLine.getAccountElement().getId(),
                cstLine.getClient().getId())) {
              String fundsLine = BudgetDAO.getCostsFundsBudget(cstLine);
              if (fundsLine != null) {
                OBQuery<EFINBudgetLines> lineExist = OBDal.getInstance().createQuery(
                    EFINBudgetLines.class,
                    "accountingCombination.id = ? and efinBudget.efinBudgetint.id = ?");
                parameter.add(fundsLine);
                parameter.add(budget.getEfinBudgetint().getId());
                lineExist.setParameters(parameter);
                if (lineExist.list() != null && lineExist.list().size() > 0) {
                  return true;
                } else {
                  parameter.clear();
                }
              }
            }
          }
        }
      }
      return false;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in checking funds budget created or not in budegtlinedao." + e, e);
      }
      return false;
    }
  }

  /**
   * 
   * @param lineList
   * @return
   */
  public static boolean checkBudgetUsedorNot(List<EFINBudgetLines> lineList) {
    try {
      // check budget line is used in encumbrance.
      for (EFINBudgetLines line : lineList) {
        LOG.debug("manual:" + line.getEfinBudgetManencumlinesList());
        if (line.getEfinBudgetManencumlinesList() != null
            && line.getEfinBudgetManencumlinesList().size() > 0) {
          return true;
        }
      }
      // check budget line is used in encumbrance.
      for (EFINBudgetLines line : lineList) {
        LOG.debug("adjustment:" + line.getEfinBudgetAdjlineList());
        if (line.getEfinBudgetAdjlineList() != null && line.getEfinBudgetAdjlineList().size() > 0) {
          return true;
        }
      }
      // check budget line used in revision.
      for (EFINBudgetLines line : lineList) {
        LOG.debug("revision:" + line.getEfinBudgetTransfertrxlineList());
        if (line.getEfinBudgetTransfertrxlineList() != null
            && line.getEfinBudgetTransfertrxlineList().size() > 0) {
          return true;
        }
      }
      // check budget line used in gljournal.
      for (EFINBudgetLines line : lineList) {
        LOG.debug("journal:" + line.getFinancialMgmtGLJournalLineEMEfinBudgetlinesIDList());
        if (line.getFinancialMgmtGLJournalLineEMEfinBudgetlinesIDList() != null
            && line.getFinancialMgmtGLJournalLineEMEfinBudgetlinesIDList().size() > 0) {
          return true;
        }
      }
      return false;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in checking budget used in any transactions or not." + e, e);
      }
      return true;
    }
  }

  /**
   * check budget is used in inquiry or not
   * 
   * @param header
   * @return
   */
  /*
   * public static boolean checkBudgetUsedInquiry(EFINBudget header) { try { String query =
   * "select ln.efin_budgetlines_id from efin_budgetlines ln join efin_budgetinquiry inq on inq.efin_budgetlines_id = ln.efin_budgetlines_id where ln.efin_budget_id =:budId and (inq.encumbrance <> 0 or spent_amt <> 0 or revinc_amt <> 0 or revdec_amt <> 0 or obinc_amt <> 0 or obdec_amt <> 0 or disinc_amt <> 0 or disdec_amt <> 0)"
   * ; SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
   * sqlQuery.setParameter("budId", header.getId()); List<String> lineIds =
   * header.getEFINBudgetLinesList().stream().map(a -> a.getId()) .collect(Collectors.toList()); if
   * (sqlQuery.list() != null && sqlQuery.list().size() > 0) { return true; } else {
   * OBCriteria<EfinBudgetInquiry> inq = OBDal.getInstance()
   * .createCriteria(EfinBudgetInquiry.class); inq.add(
   * Restrictions.eq(EfinBudgetInquiry.PROPERTY_EFINBUDGETINT, header.getEfinBudgetint()));
   * 
   * if (inq.list() != null && inq.list().size() > 0) { for (EfinBudgetInquiry e :
   * inq.list().stream() .filter(a -> lineIds.contains(a.getBudgetLines().getId()))
   * .collect(Collectors.toList())) { OBDal.getInstance().remove(e); } }
   * OBDal.getInstance().flush(); } } catch (Exception e) { if (LOG.isErrorEnabled()) {
   * LOG.error("Exception in checking budget used in inquiry or not." + e, e); } } return false; }
   */

  @SuppressWarnings("rawtypes")
  public static boolean checkBudgetUsedInquiry(EFINBudget header) {
    try {
      String query = "select inq.efin_budgetinquiry_id from efin_budgetlines ln "
          + " join efin_budget bud on bud.efin_budget_id = ln.efin_budget_id "
          + " join efin_budgetinquiry inq on inq.c_validcombination_id = ln.c_validcombination_id and inq.efin_budgetint_id = bud.efin_budgetint_id "
          + " where ln.efin_budget_id =:budId and ln.ad_client_id =:client and "
          + " (inq.encumbrance <> 0 or spent_amt <> 0 or revinc_amt <> 0 or revdec_amt <> 0 or obinc_amt <> 0 or obdec_amt <> 0 or disinc_amt <> 0 or disdec_amt <> 0 )";
      SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
      sqlQuery.setParameter("budId", header.getId());
      sqlQuery.setParameter("client", header.getClient().getId());

      if (sqlQuery.list() != null && sqlQuery.list().size() > 0) {
        return true;
      } else {
        // delete lines from inquiry.
        query = "select inq.efin_budgetinquiry_id from efin_budgetlines ln "
            + " join efin_budget bud on bud.efin_budget_id = ln.efin_budget_id "
            + " join efin_budgetinquiry inq on inq.c_validcombination_id = ln.c_validcombination_id and inq.efin_budgetint_id = bud.efin_budgetint_id "
            + " where ln.efin_budget_id =:budId and ln.ad_client_id =:client ";
        sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
        sqlQuery.setParameter("budId", header.getId());
        sqlQuery.setParameter("client", header.getClient().getId());
        List queryList = sqlQuery.list();
        if (sqlQuery != null && queryList.size() > 0) {
          for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
            Object row = iterator.next();
            if (row != null) {
              EfinBudgetInquiry inq = OBDal.getInstance().get(EfinBudgetInquiry.class,
                  row.toString());
              OBQuery<BudgetAdjustmentLine> adjLine = OBDal.getInstance().createQuery(
                  BudgetAdjustmentLine.class, "budgetInquiryLine.id = '" + inq.getId() + "'");
              List<BudgetAdjustmentLine> adjLineList = adjLine.list();
              if (adjLineList != null && adjLineList.size() > 0) {
                return true;
              } else {
                OBDal.getInstance().remove(inq);
              }
            }
          }
        }
        return false;
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in checking budget used in inquiry or not." + e, e);
      }
    }
    return false;
  }

  /**
   * 
   * @param header
   * @param fundId
   * @return boolean
   */
  public static boolean checkBudgetDisUsedInquiry(EFINBudget header, String fundId) {
    boolean distributefrom = false, distributeto = false, withoutdistribute = false, isUsed = false;

    try {
      String query = "select inq.efin_budgetinquiry_id from efin_budgetlines ln "
          + " join efin_budget bud on bud.efin_budget_id = ln.efin_budget_id "
          + " join efin_budgetinquiry inq on inq.c_validcombination_id = ln.c_validcombination_id and inq.efin_budgetint_id = bud.efin_budgetint_id "
          + " where ln.efin_budget_id =:budId and ln.ad_client_id =:client and "
          + " (inq.encumbrance <> 0 or spent_amt <> 0 or revinc_amt <> 0 or revdec_amt <> 0 or obinc_amt <> 0 or obdec_amt <> 0 or disinc_amt <> 0 or disdec_amt <> 0 ) and "
          + " (ln.isdistribute='N' or ( ln.isdistribute='Y' and ln.dislinkorg is null) )";
      SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
      sqlQuery.setParameter("budId", header.getId());
      sqlQuery.setParameter("client", header.getClient().getId());

      if (sqlQuery.list() != null && sqlQuery.list().size() > 0) {
        withoutdistribute = true;

      }

      String query1 = "select inq.efin_budgetinquiry_id from efin_fundsreqline ln "
          + " join efin_fundsreq hd on hd.efin_fundsreq_id = ln.efin_fundsreq_id "
          + " join efin_budgetinquiry inq on inq.c_validcombination_id = ln.toaccount and inq.efin_budgetint_id = hd.efin_budgetint_id "
          + " where ln.efin_fundsreq_id =:fundreqId and ln.ad_client_id =:client and "
          + " (inq.encumbrance <> 0 or spent_amt <> 0 or revinc_amt <> 0 or revdec_amt <> 0 or obinc_amt <> 0 or obdec_amt <> 0 or disinc_amt<>ln.increase or disdec_amt <> 0 ) ";
      SQLQuery sqlQuery1 = OBDal.getInstance().getSession().createSQLQuery(query1);
      sqlQuery1.setParameter("fundreqId", fundId);
      sqlQuery1.setParameter("client", header.getClient().getId());

      if (sqlQuery1.list() != null && sqlQuery1.list().size() > 0) {
        distributeto = true;

      }
      String query2 = "select inq.efin_budgetinquiry_id from efin_fundsreqline ln "
          + " join efin_fundsreq hd on hd.efin_fundsreq_id = ln.efin_fundsreq_id "
          + " join efin_budgetinquiry inq on inq.c_validcombination_id = ln.fromaccount and inq.efin_budgetint_id = hd.efin_budgetint_id "
          + " where ln.efin_fundsreq_id =:fundreqId and ln.ad_client_id =:client and "
          + " (inq.encumbrance <> 0 or spent_amt <> 0 or revinc_amt <> 0 or revdec_amt <> 0 or obinc_amt <> 0 or obdec_amt <> 0 or disinc_amt<>0 or disdec_amt<>ln.decrease) ";
      SQLQuery sqlQuery2 = OBDal.getInstance().getSession().createSQLQuery(query2);
      sqlQuery2.setParameter("fundreqId", fundId);
      sqlQuery2.setParameter("client", header.getClient().getId());

      if (sqlQuery2.list() != null && sqlQuery2.list().size() > 0) {
        distributefrom = true;
      }
      if (!distributeto && !withoutdistribute && !distributefrom) {
        isUsed = false;
      } else {
        isUsed = true;
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in checking budget distribution used in inquiry or not." + e, e);
      }
    }
    return isUsed;
  }

  /**
   * 
   * @param header
   * @param fundId
   * @return boolean
   */
  @SuppressWarnings("rawtypes")
  public static boolean deleteBudgetEnquiry(EFINBudget header, String fundId) {
    try {
      // delete lines from inquiry.
      String query = "select inq.efin_budgetinquiry_id from efin_budgetlines ln "
          + " join efin_budget bud on bud.efin_budget_id = ln.efin_budget_id "
          + " join efin_budgetinquiry inq on inq.c_validcombination_id = ln.c_validcombination_id and inq.efin_budgetint_id = bud.efin_budgetint_id "
          + " where ln.efin_budget_id =:budId and ln.ad_client_id =:client  and (ln.isdistribute='N' or ( ln.isdistribute='Y' and ln.dislinkorg is null) )";
      SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
      sqlQuery.setParameter("budId", header.getId());
      sqlQuery.setParameter("client", header.getClient().getId());
      List queryList = sqlQuery.list();

      if (sqlQuery != null && queryList.size() > 0) {
        for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
          Object row = iterator.next();
          if (row != null) {
            EfinBudgetInquiry inq = OBDal.getInstance().get(EfinBudgetInquiry.class,
                row.toString());
            OBDal.getInstance().remove(inq);
          }
        }
      }
      String query1 = "select inq.efin_budgetinquiry_id from efin_fundsreqline ln "
          + " join efin_fundsreq hd on hd.efin_fundsreq_id = ln.efin_fundsreq_id "
          + " join efin_budgetinquiry inq on inq.c_validcombination_id = ln.toaccount and inq.efin_budgetint_id = hd.efin_budgetint_id "
          + " where ln.efin_fundsreq_id =:funsreqId and ln.ad_client_id =:client ";
      SQLQuery sqlQuery1 = OBDal.getInstance().getSession().createSQLQuery(query1);
      sqlQuery1.setParameter("funsreqId", fundId);
      sqlQuery1.setParameter("client", header.getClient().getId());

      List queryList1 = sqlQuery1.list();

      if (sqlQuery1 != null && queryList1.size() > 0) {
        for (Iterator iterator = queryList1.iterator(); iterator.hasNext();) {
          Object row = iterator.next();
          if (row != null) {
            EfinBudgetInquiry inq = OBDal.getInstance().get(EfinBudgetInquiry.class,
                row.toString());

            OBDal.getInstance().remove(inq);
            OBDal.getInstance().flush();
          }
        }
      }
      String query2 = "select inq.efin_budgetinquiry_id from efin_fundsreqline ln "
          + " join efin_fundsreq hd on hd.efin_fundsreq_id = ln.efin_fundsreq_id "
          + " join efin_budgetinquiry inq on inq.c_validcombination_id = ln.fromaccount and inq.efin_budgetint_id = hd.efin_budgetint_id "
          + " where ln.efin_fundsreq_id =:funsreqId and ln.ad_client_id =:client ";
      SQLQuery sqlQuery2 = OBDal.getInstance().getSession().createSQLQuery(query2);
      sqlQuery2.setParameter("funsreqId", fundId);
      sqlQuery2.setParameter("client", header.getClient().getId());
      List queryList2 = sqlQuery2.list();
      if (sqlQuery2 != null && queryList2.size() > 0) {
        for (Iterator iterator = queryList2.iterator(); iterator.hasNext();) {
          Object row = iterator.next();
          if (row != null) {
            EfinBudgetInquiry inq = OBDal.getInstance().get(EfinBudgetInquiry.class,
                row.toString());
            OBDal.getInstance().remove(inq);
          }
        }
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in deleteBudgetEnquiry." + e, e);
      }
    }
    return false;
  }

  /**
   * To check funds uniquecode is create in budget enquiry
   * 
   * @param budget
   * @return true or false
   */
  public static boolean checkFundsIsCreated(EFINBudget budget) {
    BudgetingUtilsService budUtil = new BudgetingUtilsServiceImpl();
    try {

      for (EFINBudgetLines budgetLines : budget.getEFINBudgetLinesList()) {
        if (!budUtil.isFundsOnlyAccount(budgetLines.getAccountElement().getId(),
            budgetLines.getClient().getId())) {
          AccountingCombination fundsUniqcode = budgetLines.getAccountingCombination()
              .getEfinFundscombination();
          if (fundsUniqcode != null) {
            OBQuery<EfinBudgetInquiry> budgetinquiry = OBDal.getInstance().createQuery(
                EfinBudgetInquiry.class, "accountingCombination.id='" + fundsUniqcode.getId()
                    + "' and efinBudgetint.id = '" + budget.getEfinBudgetint().getId() + "' ");
            if (budgetinquiry.list() != null && budgetinquiry.list().size() > 0) {
              return true;
            }
          }
        }
      }
      return false;

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in checking funds budget created or not in budegtlinedao." + e, e);
      }
      return false;
    }
  }

  /**
   * 
   * @param line
   * @return
   */
  public static BigDecimal getTransferamount(EfinBudgetTransfertrxline line)

  {
    BigDecimal transferAmount = BigDecimal.ZERO;
    try {
      String query = "select sum(amount) from efin_rdv_budgtransfer where efin_budget_transfertrxline_id =:LineId";
      SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
      sqlQuery.setParameter("LineId", line.getId());
      List<Object> object = sqlQuery.list();
      if (object != null && object.size() > 0) {
        Object row = object.get(0);
        transferAmount = (BigDecimal) row;
      }

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in getTransferamount" + e, e);
      }
    }
    return transferAmount;

  }
}