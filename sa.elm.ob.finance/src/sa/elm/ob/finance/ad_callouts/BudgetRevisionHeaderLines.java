/*
 *************************************************************************
 * All Rights Reserved.
 * Contributor(s):  Qualian
 ************************************************************************
 */
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetTransfertrx;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;
import sa.elm.ob.finance.util.budget.BudgetingUtilsService;
import sa.elm.ob.finance.util.budget.BudgetingUtilsServiceImpl;

/**
 * @author Poongodi on 26/09/2017
 */

public class BudgetRevisionHeaderLines extends SimpleCallout {

  private static final Logger LOG = LoggerFactory.getLogger(BudgetRevisionHeaderLines.class);
  private static final String warningMessage = "Efin_fund_greaterthan_cost";
  private static final String warningMessage1 = "Efin_cost_lesserthan_fund";

  /**
   * Callout to update the Decrease and increase amount Window
   */
  private static final long serialVersionUID = 1L;

  @SuppressWarnings("rawtypes")
  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    log4j.debug("inpLastFieldChanged" + inpLastFieldChanged);
    String budRevHeaderId = vars.getStringParameter("inpefinBudgetTransfertrxId");
    String validcombination = vars.getStringParameter("inpcValidcombinationId");
    String budgetinitial = vars.getStringParameter("inpefinBudgetintId");
    String campaignId = vars.getStringParameter("inpcCampaignId");
    String inpdistribute = vars.getStringParameter("inpdistribute");
    String accountcombinationid = "", budgettypeFund = "", CostSearchKey = "", campaignid = "",
        replacefundtocost = "", Costcombination = "", accountcombination = "",
        budgetinquirylineID = "";
    String inpdistributionOrg = vars.getStringParameter("inpdistributeOrg");
    String ClientId = vars.getStringParameter("inpadClientId");
    String strCostCentre = "", strDistOrg = null, parsedMessage = null, BudgettypeId = "";
    BigDecimal costCurrentBudget = BigDecimal.ZERO, incamount = BigDecimal.ZERO,
        fundbudget = BigDecimal.ZERO, fundCurrentBudget = BigDecimal.ZERO,
        costbudget = BigDecimal.ZERO;
    if (vars.getStringParameter("inpincrease") != "") {
      incamount = new BigDecimal(vars.getNumericParameter("inpincrease"));
    } else {
      incamount = BigDecimal.ZERO;
    }
    EfinBudgetInquiry budgetRevline = null;

    // get budgetint id
    EfinBudgetTransfertrx objRevision = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
        budRevHeaderId);
    String budgetInt = objRevision.getEfinBudgetint() == null ? ""
        : objRevision.getEfinBudgetint().getId();

    double increase = Double
        .parseDouble(StringUtils.isEmpty(vars.getStringParameter("inpincrease")) ? "0.00"
            : vars.getStringParameter("inpincrease").replaceAll(",", ""));
    double decrease = Double
        .parseDouble(StringUtils.isEmpty(vars.getStringParameter("inpdecrease")) ? "0.00"
            : vars.getStringParameter("inpdecrease").replaceAll(",", ""));
    BudgetingUtilsService budUtil = new BudgetingUtilsServiceImpl();
    try {
      OBContext.setAdminMode();
      OBQuery<EfinBudgetInquiry> lines = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
          " accountingCombination.id =:uniquecodeid and efinBudgetint.id=:reference");
      lines.setNamedParameter("uniquecodeid", validcombination);
      lines.setNamedParameter("reference", budgetInt);
      lines.setMaxResult(1);
      if (lines.list() != null && lines.list().size() > 0) {
        budgetRevline = lines.list().get(0);
      }
      if (inpLastFieldChanged.equals("inpincrease")) {

        if (incamount.compareTo(new BigDecimal(0)) == 0) {
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('distribute').hide()");
        } else {
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('distribute').show()");

          // warning message for fund budget greater than cost budget
          EfinBudgetTransfertrx budrev = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
              budRevHeaderId);
          if (budrev.getSalesCampaign() != null) {
            BudgettypeId = budrev.getSalesCampaign().getId();
            Campaign budtype = OBDal.getInstance().get(Campaign.class, BudgettypeId);

            if (budtype.getEfinBudgettype().equals("F")) {
              // get cost current budget
              costCurrentBudget = CommonValidationsDAO.getCostCurrentBudget(validcombination,
                  budgetinitial, campaignId, ClientId);
              AccountingCombination validcomb = OBDal.getInstance().get(AccountingCombination.class,
                  validcombination);
              if (!budUtil.isFundsOnlyAccount(validcomb.getAccount().getId(), ClientId)) {
                if (costCurrentBudget != null) {
                  fundbudget = budgetRevline.getCurrentBudget().add(new BigDecimal(increase))
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
      }
      if (inpLastFieldChanged.equals("inpdecrease")) {
        info.addResult("inpincrease", BigDecimal.ZERO);
        info.addResult("JSEXECUTE", "form.getFieldFromColumnName('distribute').hide()");
        info.addResult("inpdistribute", false);
        info.addResult("inpdistributeLineOrg", null);

        // warning message for cost budget lesser than fund budget
        EfinBudgetTransfertrx budrev = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
            budRevHeaderId);
        if (budrev.getSalesCampaign() != null) {
          BudgettypeId = budrev.getSalesCampaign().getId();
          Campaign budtype = OBDal.getInstance().get(Campaign.class, BudgettypeId);
          if (budtype.getEfinBudgettype().equals("C")) {
            if (!budUtil.isFundsOnlyAccount(
                budgetRevline.getAccountingCombination().getAccount().getId(), ClientId)) {
              // get fund current budget
              fundCurrentBudget = CommonValidationsDAO.getFundCurrentBudget(validcombination,
                  budgetinitial, campaignId, ClientId);
              if (fundCurrentBudget != null) {
                costbudget = budgetRevline.getCurrentBudget().add(BigDecimal.ZERO)
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
      }

      // To load Current Budget and Funds Available
      if (inpLastFieldChanged.equals("inpcValidcombinationId")) {
        EfinBudgetTransfertrx budRev = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
            budRevHeaderId);
        if (budRev.getSalesCampaign().getEfinBudgettype().equals("F")) {
          OBQuery<EfinBudgetInquiry> budgetinquiry = OBDal.getInstance().createQuery(
              EfinBudgetInquiry.class,
              "accountingCombination.id='" + validcombination + "' and efinBudgetint.id = '"
                  + budgetinitial + "' and salesCampaign.id = '" + campaignId + "' ");
          if (budgetinquiry.list() != null && budgetinquiry.list().size() > 0) {
            accountcombinationid = budgetinquiry.list().get(0).getId();

          }

          EfinBudgetInquiry budgetinquiryline = null;
          budgetinquiryline = OBDal.getInstance().get(EfinBudgetInquiry.class,
              accountcombinationid);
          AccountingCombination AccCombination = OBDal.getInstance()
              .get(AccountingCombination.class, validcombination);
          if (budgetinquiryline != null) {
            info.addResult("inpcurrentBudget", budgetinquiryline.getCurrentBudget());
            info.addResult("inpfundsAvailable", budgetinquiryline.getFundsAvailable());
            info.addResult("inpuniquecodename", budgetinquiryline.getUniqueCodeName());
          } else {
            info.addResult("inpcurrentBudget", 0);
            info.addResult("inpfundsAvailable", 0);
            info.addResult("inpuniquecodename", AccCombination.getEfinUniquecodename());
          }

          String UniqueCodeAcc = AccCombination.getEfinUniqueCode();
          budgettypeFund = UniqueCodeAcc.split("-")[4];
          OBQuery<Campaign> budtype = OBDal.getInstance().createQuery(Campaign.class,
              "efinBudgettype='C'");
          if (budtype.list() != null && budtype.list().size() > 0) {
            CostSearchKey = budtype.list().get(0).getSearchKey();
            campaignid = budtype.list().get(0).getId();
            // Replace Budget type(search key) fund to cost in unique code
            replacefundtocost = budgettypeFund.substring(0).replace(budgettypeFund.substring(0),
                CostSearchKey);
            // set uniquecode combination with 'cost' budget type
            Costcombination = UniqueCodeAcc.split("-")[0] + "-" + UniqueCodeAcc.split("-")[1] + "-"
                + UniqueCodeAcc.split("-")[2] + "-" + UniqueCodeAcc.split("-")[3] + "-"
                + replacefundtocost + "-" + UniqueCodeAcc.split("-")[5] + "-"
                + UniqueCodeAcc.split("-")[6] + "-" + UniqueCodeAcc.split("-")[7] + "-"
                + UniqueCodeAcc.split("-")[8];

            OBQuery<AccountingCombination> validcom = OBDal.getInstance().createQuery(
                AccountingCombination.class, "efinUniqueCode='" + Costcombination + "'");

            if (validcom.list() != null && validcom.list().size() > 0) {
              accountcombination = validcom.list().get(0).getId();
            }
            OBQuery<EfinBudgetInquiry> budline = OBDal.getInstance().createQuery(
                EfinBudgetInquiry.class,
                "accountingCombination.id= :accountingCombinationID and efinBudgetint.id = :efinBudgetintID "
                    + " and salesCampaign.id = :salesCampaignID");
            budline.setNamedParameter("accountingCombinationID", accountcombination);
            budline.setNamedParameter("efinBudgetintID", budgetinitial);
            budline.setNamedParameter("salesCampaignID", campaignid);
            if (budline.list() != null && budline.list().size() > 0) {
              budgetinquirylineID = budline.list().get(0).getId();
            }
            EfinBudgetInquiry budgetinqu = null;
            budgetinqu = OBDal.getInstance().get(EfinBudgetInquiry.class, budgetinquirylineID);
            if (budgetinqu != null) {
              info.addResult("inpcostcurrentbudget", budgetinqu.getFundsAvailable());
            } else {
              info.addResult("inpcostcurrentbudget", null);
            }
          }
        } else {

          OBQuery<EfinBudgetInquiry> budgetinquiry = OBDal.getInstance().createQuery(
              EfinBudgetInquiry.class,
              "accountingCombination.id= :accountingCombinationID and efinBudgetint.id = :efinBudgetintID"
                  + " and salesCampaign.id = :salesCampaignID ");
          budgetinquiry.setNamedParameter("accountingCombinationID", validcombination);
          budgetinquiry.setNamedParameter("efinBudgetintID", budgetinitial);
          budgetinquiry.setNamedParameter("salesCampaignID", campaignid);
          if (budgetinquiry.list() != null && budgetinquiry.list().size() > 0) {
            accountcombinationid = budgetinquiry.list().get(0).getId();

          }
          EfinBudgetInquiry budgetinquiryline = null;
          budgetinquiryline = OBDal.getInstance().get(EfinBudgetInquiry.class,
              accountcombinationid);
          AccountingCombination AccCombination = OBDal.getInstance()
              .get(AccountingCombination.class, validcombination);
          if (budgetinquiryline != null) {
            info.addResult("inpcurrentBudget", budgetinquiryline.getCurrentBudget());
            info.addResult("inpcostcurrentbudget", budgetinquiryline.getFundsAvailable());
            info.addResult("inpuniquecodename", budgetinquiryline.getUniqueCodeName());
          } else {
            info.addResult("inpcurrentBudget", 0);
            info.addResult("inpcostcurrentbudget", 0);
            info.addResult("inpuniquecodename", AccCombination.getEfinUniquecodename());
          }

          String UniqueCodeAcc = AccCombination.getEfinUniqueCode();
          budgettypeFund = UniqueCodeAcc.split("-")[4];
          OBQuery<Campaign> budtype = OBDal.getInstance().createQuery(Campaign.class,
              "efinBudgettype='F'");
          if (budtype.list() != null && budtype.list().size() > 0) {
            CostSearchKey = budtype.list().get(0).getSearchKey();
            campaignid = budtype.list().get(0).getId();
            // Replace Budget type(search key) cost to fund in unique code
            replacefundtocost = budgettypeFund.substring(0).replace(budgettypeFund.substring(0),
                CostSearchKey);
            // set uniquecode combination with 'fund' budget type
            Costcombination = UniqueCodeAcc.split("-")[0] + "-" + UniqueCodeAcc.split("-")[1] + "-"
                + UniqueCodeAcc.split("-")[2] + "-" + UniqueCodeAcc.split("-")[3] + "-"
                + replacefundtocost + "-" + UniqueCodeAcc.split("-")[5] + "-"
                + UniqueCodeAcc.split("-")[6] + "-" + UniqueCodeAcc.split("-")[7] + "-"
                + UniqueCodeAcc.split("-")[8];
            OBQuery<AccountingCombination> validcom = OBDal.getInstance().createQuery(
                AccountingCombination.class, "efinUniqueCode='" + Costcombination + "'");

            if (validcom.list() != null && validcom.list().size() > 0) {
              accountcombination = validcom.list().get(0).getId();
            }
            OBQuery<EfinBudgetInquiry> budline = OBDal.getInstance().createQuery(
                EfinBudgetInquiry.class,
                "accountingCombination.id= :accountingCombinationID and efinBudgetint.id = :efinBudgetintID"
                    + " and salesCampaign.id = :salesCampaignID ");
            budline.setNamedParameter("accountingCombinationID", accountcombination);
            budline.setNamedParameter("efinBudgetintID", budgetinitial);
            budline.setNamedParameter("salesCampaignID", campaignid);
            if (budline.list() != null && budline.list().size() > 0) {
              budgetinquirylineID = budline.list().get(0).getId();
            }
            EfinBudgetInquiry budgetinqu = null;
            budgetinqu = OBDal.getInstance().get(EfinBudgetInquiry.class, budgetinquirylineID);
            if (budgetinqu != null) {
              info.addResult("inpfundsAvailable", budgetinqu.getFundsAvailable());
            } else {
              info.addResult("inpfundsAvailable", null);
            }
          }

        }

        AccountingCombination validcomb = OBDal.getInstance().get(AccountingCombination.class,
            validcombination);
        if (!budUtil.isFundsOnlyAccount(validcomb.getAccount().getId(), ClientId)) {
          // get cost current budget
          costCurrentBudget = CommonValidationsDAO.getCostCurrentBudget(validcombination,
              budgetinitial, campaignId, ClientId);
          if (costCurrentBudget != null) {
            if (incamount.compareTo(costCurrentBudget) > 0) {
              parsedMessage = Utility.messageBD(this, warningMessage,
                  OBContext.getOBContext().getLanguage().getId());
              info.addResult("WARNING", parsedMessage);
            }
          }
        }

      }
      if (inpLastFieldChanged.equals("inpcValidcombinationId")
          || inpLastFieldChanged.equals("inpdistribute")) {
        log4j.debug("Lastchanged" + inpLastFieldChanged);
        // Fetch cost centre
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
        AccountingCombination AccCombination = OBDal.getInstance().get(AccountingCombination.class,
            validcombination);
        String accountId = AccCombination.getAccount().getId();
        String query = "select ad_org_id from ad_org where ad_org_id in (select ad_org_id from c_validcombination "
            + "where c_salesregion_id = '" + strCostCentre + "' and account_id ='" + accountId
            + "') and ad_org_id = '" + inpdistributionOrg + "' ";
        SQLQuery distOrgQuery = OBDal.getInstance().getSession().createSQLQuery(query);
        List distOrgList = distOrgQuery.list();
        if (distOrgQuery != null && distOrgList.size() > 0) {
          Object objDistOrg = distOrgList.get(0);
          if (objDistOrg != null) {
            strDistOrg = objDistOrg.toString();
          }
          log4j.debug("strDistOrg" + strDistOrg);
        } else {
          info.addResult("inpdistributeLineOrg", "");
        }
        if (strDistOrg != null) {
          if (inpdistribute.equals("Y"))
            info.addResult("inpdistributeLineOrg", strDistOrg);

        }
        if (inpdistribute.equals("N")) {
          info.addSelect("inpdistributeLineOrg");
          info.addSelectResult("", "", true);

          String strquery = "select ad_org_id,value,name from ad_org where ad_org_id in (select ad_org_id from c_validcombination "
              + " where c_salesregion_id = :salesRegionId and account_id = :accountId)  ";
          SQLQuery distOrgQuery1 = OBDal.getInstance().getSession().createSQLQuery(strquery);
          distOrgQuery1.setParameter("salesRegionId", strCostCentre);
          distOrgQuery1.setParameter("accountId", accountId);
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
        }

      }
    } catch (Exception e) {
      LOG.error("Exception while BudgetRevisionHeaderLines Callout:" + e, e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }

  }

}
