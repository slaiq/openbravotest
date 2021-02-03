package sa.elm.ob.finance.ad_callouts;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.project.Project;

import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EFINBudgetTypeAcct;
import sa.elm.ob.finance.dao.UniqueCodeGen;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;
import sa.elm.ob.finance.util.budget.BudgetingUtilsService;
import sa.elm.ob.finance.util.budget.BudgetingUtilsServiceImpl;

/**
 * @author Gopalakrishnan on 11/05/2106
 */

public class BudgetLinesCallout extends SimpleCallout {

  /**
   * Callout to update the uniqueCode Information in BudgetLines Window
   */
  private static final long serialVersionUID = 1L;
  private static final Logger log = Logger.getLogger(BudgetLinesCallout.class);
  private static final String warningMessage = "Efin_fund_greaterthan_cost";

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    Connection conn = OBDal.getInstance().getConnection();
    UniqueCodeGen dao = new UniqueCodeGen(conn);

    String inpProject = vars.getStringParameter("inpcProjectId");
    String inpUser1 = vars.getStringParameter("inpuser1Id");
    String inpUser2 = vars.getStringParameter("inpuser2Id");
    String inpActivity = vars.getStringParameter("inpcActivityId");
    String inpAccount = vars.getStringParameter("inpcElementvalueId");
    String inpBudgetType = vars.getStringParameter("inpcCampaignId");
    String inpDepartment = vars.getStringParameter("inpcSalesregionId");
    String inpBudget = vars.getStringParameter("inpefinBudgetId");
    String inpOrgId = vars.getStringParameter("inpadOrgId");
    String inpEntity = vars.getStringParameter("inpcBpartnerId");
    String inpisdistribute = vars.getStringParameter("inpisdistribute");
    String ClientId = vars.getStringParameter("inpadClientId");
    String validcombination = vars.getStringParameter("inpcValidcombinationId");
    String budgetinitial = vars.getStringParameter("inpefinBudgetintId");
    String campaignId = vars.getStringParameter("inpcCampaignId");

    String parsedMessage = null;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    ElementValue parentAccount = null;
    String parentId = "", strCostCentre = "";
    BigDecimal costCurrentBudget = BigDecimal.ZERO, incamount = BigDecimal.ZERO;

    if (vars.getStringParameter("inpamount") != "") {
      incamount = new BigDecimal(vars.getNumericParameter("inpamount"));
    } else {
      incamount = BigDecimal.ZERO;
    }
    log.debug("inpLastFieldChanged:" + inpLastFieldChanged);
    if (inpLastFieldChanged.equals("inpamount")) {
      /*
       * if (vars.getStringParameter("inpamount") == "" || vars.getStringParameter("inpamount") ==
       * null) { info.addResult("inpamount", "0"); info.addResult("inpcurrentBudget", "0");
       * info.addResult("inpfundsAvailable", "0"); } else { info.addResult("inpcurrentBudget",
       * vars.getStringParameter("inpamount")); info.addResult("inpfundsAvailable",
       * vars.getStringParameter("inpamount"));
       */
      EFINBudget bud = OBDal.getInstance().get(EFINBudget.class, inpBudget);
      BudgetingUtilsService serviceDAO = new BudgetingUtilsServiceImpl();
      Boolean isFundsOnlyAccount = serviceDAO.isFundsOnlyAccount(inpAccount, ClientId);

      if ((bud.getAccountElement() != null)
          & (bud.getSalesCampaign().getEfinBudgettype().equals("F")) && (!isFundsOnlyAccount)) {
        OBQuery<EFINBudgetTypeAcct> budtypeacc = OBDal.getInstance().createQuery(
            EFINBudgetTypeAcct.class,
            " as e where e.accountElement.id = :accountElementID and e.client.id = :clientID "
                + " and e.salesCampaign.efinBudgettype = 'C'");
        budtypeacc.setNamedParameter("accountElementID", bud.getAccountElement().getId());
        budtypeacc.setNamedParameter("clientID", ClientId);
        if (budtypeacc.list().size() > 0 && budtypeacc != null) {
          costCurrentBudget = CommonValidationsDAO.getCostCurrentBudget(validcombination,
              budgetinitial, campaignId, ClientId);
          log.debug("costCurrentBudget:" + costCurrentBudget);
          if (costCurrentBudget != null) {
            if (incamount.compareTo(costCurrentBudget) > 0) {
              parsedMessage = Utility.messageBD(this, warningMessage,
                  OBContext.getOBContext().getLanguage().getId());
              info.addResult("WARNING", parsedMessage);
            }

          }
        }
      }
    }
    if (inpLastFieldChanged.equals("inpcElementvalueId"))

    {
      EFINBudget budget = OBDal.getInstance().get(EFINBudget.class, inpBudget);
      parentId = budget.getAccountElement().getId();
      // account tree structure has been changed, so we cant check immediate parent.better get
      // parent account from budget header.
      /*
       * OBQuery<TreeNode> account = OBDal.getInstance().createQuery(TreeNode.class,
       * "as e where e.node='" + inpAccount + "'"); if(account.list().size() > 0) { parentId =
       * account.list().get(0).getReportSet();
       */
      parentAccount = OBDal.getInstance().get(ElementValue.class, parentId);
      if (parentAccount != null) {
        if (parentAccount.isEfinProjacct()) {
          ElementValue Childaccount = OBDal.getInstance().get(ElementValue.class, inpAccount);
          if (Childaccount != null && Childaccount.getEfinProject() != null) {
            OBQuery<Project> project = OBDal.getInstance().createQuery(Project.class,
                "as e where e.id ='" + Childaccount.getEfinProject().getId()
                    + "' and e.organization.id in('" + inpOrgId + "','0')");
            if (project.list().size() > 0 && project != null) {
              info.addResult("inpcProjectId", project.list().get(0).getId());
            } else {
              info.addResult("inpcProjectId", null);
            }
          } else {
            info.addResult("inpcProjectId", null);
          }
        } else {
          ElementValue Childaccount = OBDal.getInstance().get(ElementValue.class, inpAccount);
          if (Childaccount.getEfinProject() == null) {
            OBQuery<Project> projectList = OBDal.getInstance().createQuery(Project.class,
                "em_efin_isdefault='Y'");
            if (projectList.list().size() > 0) {
              info.addResult("inpcProjectId", projectList.list().get(0).getId());
            }
          } else {
            info.addResult("inpcProjectId", null);
          }
        }
        /* } */
      }

    }
    String uniquecode = dao.getUniqueCode(inpOrgId, inpDepartment, inpAccount, inpBudgetType,
        inpProject, inpActivity, inpUser1, inpUser2, inpEntity);
    if (uniquecode != null && uniquecode.length() > 0) {
      info.addResult("inpuniquecode", uniquecode);
    } else {
      info.addResult("inpuniquecode", "");
    }
    if (inpLastFieldChanged.equals("inpisdistribute")) {

      EFINBudget budget = OBDal.getInstance().get(EFINBudget.class, inpBudget);
      if (inpisdistribute.equals("Y") && budget.getDistributionLinkOrg() != null) {
        info.addResult("inpdislinkorg", budget.getDistributionLinkOrg().getId());
      } else {
        SQLQuery costCentreQuery = OBDal.getInstance().getSession().createSQLQuery(
            "select budgetcontrol_costcenter from efin_budget_ctrl_param where ad_client_Id = :clientID ");
        costCentreQuery.setParameter("clientID", ClientId);
        @SuppressWarnings("rawtypes")
        List costCenterList = costCentreQuery.list();
        if (costCentreQuery != null && costCenterList.size() > 0) {
          Object objCostCentre = costCenterList.get(0);
          if (objCostCentre != null) {
            strCostCentre = objCostCentre.toString();
          }
        }
        info.addSelect("inpdislinkorg");
        info.addSelectResult("", "", true);

        String strquery = "select ad_org_id,value,name from ad_org where ad_org_id in (select ad_org_id from c_validcombination "
            + " where c_salesregion_id = :salesregionID and account_id = :accountID)  ";
        SQLQuery distOrgQuery1 = OBDal.getInstance().getSession().createSQLQuery(strquery);
        distOrgQuery1.setParameter("salesregionID", strCostCentre);
        distOrgQuery1.setParameter("accountID", inpAccount);
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
      }
    }

  }
}
