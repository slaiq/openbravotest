package sa.elm.ob.finance.ad_callouts;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.marketing.Campaign;

import sa.elm.ob.finance.BudgetAdjustment;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;
import sa.elm.ob.finance.util.budget.BudgetingUtilsService;
import sa.elm.ob.finance.util.budget.BudgetingUtilsServiceImpl;

/**
 * @author Gopalakrishnan on 15/09/2017
 */

public class BudgetAdjustmentLinesCallout extends SimpleCallout {
  private static final String warningMessage = "Efin_fund_greaterthan_cost";
  private static final String warningMessage1 = "Efin_cost_lesserthan_fund";

  /**
   * Call out to update the read only field details in Budget Adjustment Details
   */
  private static final long serialVersionUID = 1L;

  @SuppressWarnings("rawtypes")
  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String uniqueCodeId = vars.getStringParameter("inpcValidcombinationId");
    String budgetAdjustmentId = vars.getStringParameter("inpefinBudgetadjId");
    String isdistribute = vars.getStringParameter("inpisdistribute");
    String ClientId = vars.getStringParameter("inpadClientId");
    String strCostCentre = "", parsedMessage = null;
    BigDecimal costCurrentBudget = BigDecimal.ZERO, fundCurrentBudget = BigDecimal.ZERO;
    String budgetinitial = vars.getStringParameter("inpefinBudgetintId");
    String campaignId = vars.getStringParameter("inpcCampaignId");
    // String yearId = "", budgetTypeid = "";
    String BudgettypeId = "";
    BigDecimal fundbudget = BigDecimal.ZERO, costbudget = BigDecimal.ZERO;
    EfinBudgetInquiry budgetline = null;
    boolean havingBudget = false;
    double increase = Double
        .parseDouble(StringUtils.isEmpty(vars.getStringParameter("inpincrease")) ? "0.00"
            : vars.getStringParameter("inpincrease").replaceAll(",", ""));
    double decrease = Double
        .parseDouble(StringUtils.isEmpty(vars.getStringParameter("inpdecrease")) ? "0.00"
            : vars.getStringParameter("inpdecrease").replaceAll(",", ""));
    BudgetAdjustment objAdjustment = OBDal.getInstance().get(BudgetAdjustment.class,
        budgetAdjustmentId);
    String str_budget_reference = objAdjustment.getEfinBudgetint() == null ? ""
        : objAdjustment.getEfinBudgetint().getId();
    try {
      OBQuery<EfinBudgetInquiry> lines = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
          " accountingCombination.id =:uniquecodeid and efinBudgetint.id=:reference");
      lines.setNamedParameter("uniquecodeid", uniqueCodeId);
      lines.setNamedParameter("reference", str_budget_reference);
      lines.setMaxResult(1);
      if (lines.list() != null && lines.list().size() > 0) {
        havingBudget = true;
        budgetline = lines.list().get(0);
      }
      if (inpLastFieldChanged.equals("inpisdistribute")) {
        if (isdistribute.equals("N")) {
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('dislinkorg').hide()");
          SQLQuery costCentreQuery = OBDal.getInstance().getSession().createSQLQuery(
              "select budgetcontrol_costcenter from efin_budget_ctrl_param where ad_client_Id = :clientID ");
          costCentreQuery.setParameter("clientID", ClientId);
          List costCenterList = costCentreQuery.list();
          if (costCentreQuery != null && costCenterList.size() > 0) {
            Object objCostCentre = costCenterList.get(0);
            if (objCostCentre != null) {
              strCostCentre = objCostCentre.toString();
            }
          }
          AccountingCombination AccCombination = OBDal.getInstance()
              .get(AccountingCombination.class, uniqueCodeId);
          String accountId = AccCombination.getAccount().getId();
          info.addSelect("inpdislinkorg");
          info.addSelectResult("", "", true);

          String strquery = "select ad_org_id,value,name from ad_org where ad_org_id in "
              + " (select ad_org_id from c_validcombination "
              + " where c_salesregion_id= :salesregionID and account_id= :accountID)  ";
          SQLQuery distOrgQuery1 = OBDal.getInstance().getSession().createSQLQuery(strquery);
          distOrgQuery1.setParameter("salesregionID", strCostCentre);
          distOrgQuery1.setParameter("accountID", accountId);
          @SuppressWarnings("unchecked")
          List<Object[]> distOrgList1 = (ArrayList<Object[]>) distOrgQuery1.list();

          if (distOrgList1 != null && distOrgList1.size() > 0) {
            for (int i = 0; i < distOrgList1.size(); i++) {
              Object[] objects = distOrgList1.get(i);
              if (objects != null) {
                info.addSelectResult(objects[0].toString(),
                    objects[1].toString() + " - " + objects[2].toString(), false);
              }

            }

          }

          info.endSelect();
          // info.addResult("JSEXECUTE", "form.getFieldFromColumnName('dislinkorg').setValue('')");
        } else {
          BudgetAdjustment budgetadj = OBDal.getInstance().get(BudgetAdjustment.class,
              budgetAdjustmentId);
          if (budgetadj.getDistributionLinkOrg() != null) {
            info.addResult("inpdislinkorg", budgetadj.getDistributionLinkOrg().getId());

          }
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('dislinkorg').show()");
        }
      }
      if (inpLastFieldChanged.equals("inpincrease")) {
        if (new BigDecimal(increase).compareTo(BigDecimal.ZERO) == 0) {
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('isdistribute').hide()");
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('dislinkorg').hide()");
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('dislinkorg').setValue('')");
        } else {
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('isdistribute').show()");
          if (isdistribute.equals("N")) {
            info.addResult("JSEXECUTE", "form.getFieldFromColumnName('dislinkorg').hide()");
            info.addResult("JSEXECUTE", "form.getFieldFromColumnName('dislinkorg').setValue('')");
          } else {
            info.addResult("JSEXECUTE", "form.getFieldFromColumnName('dislinkorg').show()");
          }

          // warning message for fund budget greater than cost budget
          BudgetAdjustment budadj = OBDal.getInstance().get(BudgetAdjustment.class,
              budgetAdjustmentId);
          if (budadj.getBudgetType() != null) {
            BudgettypeId = budadj.getBudgetType().getId();
            Campaign budtype = OBDal.getInstance().get(Campaign.class, BudgettypeId);
            BudgetingUtilsService budUtil = new BudgetingUtilsServiceImpl();

            if (budtype.getEfinBudgettype().equals("F")) {
              // get cost current budget
              costCurrentBudget = CommonValidationsDAO.getCostCurrentBudget(uniqueCodeId,
                  budgetinitial, campaignId, ClientId);
              AccountingCombination validcomb = OBDal.getInstance().get(AccountingCombination.class,
                  uniqueCodeId);
              if (!budUtil.isFundsOnlyAccount(validcomb.getAccount().getId(), ClientId)) {
                if (costCurrentBudget != null) {
                  fundbudget = budgetline.getCurrentBudget().add(new BigDecimal(increase))
                      .subtract(BigDecimal.ZERO);
                  if (fundbudget.compareTo(costCurrentBudget) > 0
                      && validcomb.getEfinCostcombination() != null) {
                    parsedMessage = Utility.messageBD(this, warningMessage,
                        OBContext.getOBContext().getLanguage().getId());
                    info.addResult("WARNING", parsedMessage);
                  }
                }
              }
            }
          }

        }

        info.addResult("inpdecrease", BigDecimal.ZERO);
        if (!havingBudget) {
          info.addResult("inpcurrentBudget",
              (BigDecimal.ZERO.add(new BigDecimal(increase)).subtract(BigDecimal.ZERO)));
          info.addResult("inporgBudgetRevised",
              (BigDecimal.ZERO.add(new BigDecimal(increase)).subtract(BigDecimal.ZERO)));
        } else {
          info.addResult("inpcurrentBudget", (budgetline.getCurrentBudget()
              .add(new BigDecimal(increase)).subtract(BigDecimal.ZERO)));
          info.addResult("inporgBudgetRevised",
              (budgetline.getREVAmount().add(new BigDecimal(increase)).subtract(BigDecimal.ZERO)));
        }

      }
      if (inpLastFieldChanged.equals("inpdecrease")) {
        if (new BigDecimal(decrease).compareTo(BigDecimal.ZERO) == 1) {
          info.addResult("inpisdistribute", false);
          info.addResult("inpdislinkorg", null);
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('isdistribute').hide()");
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('dislinkorg').hide()");
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('dislinkorg').setValue('')");
        } else {
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('isdistribute').show()");
          if (isdistribute.equals("N")) {
            info.addResult("JSEXECUTE", "form.getFieldFromColumnName('dislinkorg').hide()");
            info.addResult("JSEXECUTE", "form.getFieldFromColumnName('dislinkorg').setValue('')");
          } else {
            info.addResult("JSEXECUTE", "form.getFieldFromColumnName('dislinkorg').show()");
          }
        }
        info.addResult("inpincrease", BigDecimal.ZERO);
        if (!havingBudget) {
          info.addResult("inpcurrentBudget",
              (BigDecimal.ZERO.add(BigDecimal.ZERO).subtract(new BigDecimal(decrease))));
          info.addResult("inporgBudgetRevised",
              (BigDecimal.ZERO.add(BigDecimal.ZERO).subtract(new BigDecimal(decrease))));
        } else {
          info.addResult("inpcurrentBudget", (budgetline.getCurrentBudget().add(BigDecimal.ZERO)
              .subtract(new BigDecimal(decrease))));
          info.addResult("inporgBudgetRevised", (budgetline.getCurrentBudget().add(BigDecimal.ZERO)
              .subtract(new BigDecimal(decrease))));
        }

        // warning message for cost budget lesser than fund budget
        BudgetAdjustment budadj = OBDal.getInstance().get(BudgetAdjustment.class,
            budgetAdjustmentId);
        if (budadj.getBudgetType() != null) {
          BudgettypeId = budadj.getBudgetType().getId();
          Campaign budtype = OBDal.getInstance().get(Campaign.class, BudgettypeId);
          if (budtype.getEfinBudgettype().equals("C")) {
            // get fund current budget
            fundCurrentBudget = CommonValidationsDAO.getFundCurrentBudget(uniqueCodeId,
                budgetinitial, campaignId, ClientId);
            if (fundCurrentBudget != null) {
              costbudget = budgetline.getCurrentBudget().add(BigDecimal.ZERO)
                  .subtract(new BigDecimal(decrease));
              if (costbudget.compareTo(fundCurrentBudget) < 0) {
                parsedMessage = Utility.messageBD(this, warningMessage1,
                    OBContext.getOBContext().getLanguage().getId());
                info.addResult("WARNING", parsedMessage);
              }

            }
          }
        }

      }
      // To load Current Budget and Funds Available
      if (inpLastFieldChanged.equals("inpcValidcombinationId")) {
        AccountingCombination objCombination = OBDal.getInstance().get(AccountingCombination.class,
            uniqueCodeId);

        if (objCombination != null) {
          info.addResult("inpuniquecodename", objCombination.getEfinUniquecodename());
        }

        // check unique code exits
        // in budget
        OBQuery<EfinBudgetInquiry> lineListQuery = OBDal.getInstance().createQuery(
            EfinBudgetInquiry.class,
            " accountingCombination.id =:uniquecodeid and efinBudgetint.id=:reference");
        lineListQuery.setNamedParameter("uniquecodeid", uniqueCodeId);
        lineListQuery.setNamedParameter("reference", str_budget_reference);
        lineListQuery.setMaxResult(1);
        if (lineListQuery.list().size() > 0) {
          EfinBudgetInquiry objBudgetLine = lineListQuery.list().get(0);
          info.addResult("inporiginalBudget", objBudgetLine.getREVAmount());
          info.addResult("inpcurrentBudget", (objBudgetLine.getCurrentBudget()
              .add(new BigDecimal(increase)).subtract(new BigDecimal(decrease))));
          info.addResult("inporgBudgetRevised", (objBudgetLine.getREVAmount()
              .add(new BigDecimal(increase)).subtract(new BigDecimal(decrease))));
          info.addResult("inpefinBudgetinquiryId", objBudgetLine.getId());

        } else {
          info.addResult("inporiginalBudget", BigDecimal.ZERO);
          info.addResult("inpcurrentBudget",
              (BigDecimal.ZERO.add(new BigDecimal(increase)).subtract(new BigDecimal(decrease))));
          info.addResult("inporgBudgetRevised",
              (BigDecimal.ZERO.add(new BigDecimal(increase)).subtract(new BigDecimal(decrease))));
          info.addResult("inpefinBudgetinquiryId", null);
        }

      }
    } catch (Exception e) {
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }

  }
}
