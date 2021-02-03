package sa.elm.ob.finance.ad_callouts;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.project.Project;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * @author Gopalakrishnan on 03/08/2016
 * 
 */

public class FinancialAccountConfigurationCallout extends SimpleCallout {

  /**
   * Callout to update the uniqueCode Information in Financial Account/Account Configuration Tab
   */
  private static final long serialVersionUID = 1L;
  private static final Logger log = Logger.getLogger(FinancialAccountConfigurationCallout.class);

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    try {
      ConnectionProvider conn = new DalConnectionProvider(false);
      String language = OBContext.getOBContext().getLanguage().getLanguage();
      VariablesSecureApp vars = info.vars;
      String[] orgIds = null;
      String inpInIntrAcct = vars.getStringParameter("inpfinInIntransitAcct");
      String inpDepositAcct = vars.getStringParameter("inpfinDepositAcct");
      String inpInClearAcct = vars.getStringParameter("inpfinInClearAcct");
      String inpOutIntransitAcct = vars.getStringParameter("inpfinOutIntransitAcct");
      String inpWithdrawalAcct = vars.getStringParameter("inpfinWithdrawalAcct");
      String inpOutClearAcct = vars.getStringParameter("inpfinOutClearAcct");
      String inpOrgId = vars.getStringParameter("inpadOrgId");
      String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
      String inpfinInIntransitAcct = vars.getStringParameter("inpfinInIntransitAcct");
      Campaign campaign = null;
      String BudgetTypeId = "";
      String DepartmentId = "";
      String ProjectId = "";
      String ActivityId = "";
      String UserOneId = "";
      String UserTwoId = "";
      JSONObject obj = new JSONObject();
      // Common BudgetType
      OBQuery<Campaign> gencampaignList = OBDal.getInstance().createQuery(Campaign.class,
          "as e where e.efinBudgettype='F' and e.organization.id='" + inpOrgId + "'");
      if (gencampaignList.list().size() == 0) {
        String ParentQury = " select eut_parent_org('" + inpOrgId + "','"
            + OBContext.getOBContext().getCurrentClient().getId() + "')";
        Query parentresult = OBDal.getInstance().getSession().createSQLQuery(ParentQury);
        Object parentOrg = parentresult.list().get(0);
        orgIds = ((String) parentOrg).split(",");
        for (int i = 0; i < orgIds.length; i++) {
          OBQuery<Campaign> campaignList = OBDal.getInstance().createQuery(Campaign.class,
              "as e where e.efinBudgettype='F' and e.organization.id=" + orgIds[i] + "");
          if (campaignList.list().size() > 0) {
            campaign = campaignList.list().get(0);
            //BudgetTypeValue = campaign.getSearchKey();
            BudgetTypeId = campaign.getId();
            break;
          }
        }
      } else {
        campaign = gencampaignList.list().get(0);
        //BudgetTypeValue = campaign.getSearchKey();
        BudgetTypeId = campaign.getId();
      }
      /*
       * // common Department OBQuery<SalesRegion> salesRegionList =
       * OBDal.getInstance().createQuery(SalesRegion.class, "as e where e.efinTransactiondep='Y'");
       * if (salesRegionList.list().size() > 0) { DepartmentId =
       * salesRegionList.list().get(0).getId(); DepartmentValue =
       * salesRegionList.list().get(0).getSearchKey(); } else { info.addResult("ERROR",
       * String.format(Utility.messageBD(conn, "Efin_No_Defaultdept", language))); }
       */
      // common Organization
      //Organization org = OBDal.getInstance().get(Organization.class, inpOrgId);
     // OrgValue = org.getSearchKey();
      // common project
      OBQuery<Project> projectList = OBDal.getInstance().createQuery(Project.class,
          "as e where e.eFINDefault='Y'");
      if (projectList.list().size() > 0) {
        ProjectId = projectList.list().get(0).getId();
        //ProjectValue = projectList.list().get(0).getSearchKey();
      } else {
        info.addResult("ERROR",
            String.format(Utility.messageBD(conn, "Efin_No_Defaultproj", language)));
      }
      // common functional classification
      OBQuery<ABCActivity> activityList = OBDal.getInstance().createQuery(ABCActivity.class,
          "as e where e.efinIsdefault='Y'");
      if (activityList.list().size() > 0) {
       // ActivityValue = activityList.list().get(0).getSearchKey();
        ActivityId = activityList.list().get(0).getId();
      } else {
        info.addResult("ERROR",
            String.format(Utility.messageBD(conn, "Efin_No_Defaultfunclass", language)));
      }
      // common user1
      OBQuery<UserDimension1> userOneList = OBDal.getInstance().createQuery(UserDimension1.class,
          "as e where e.efinIsdefault='Y'");
      if (userOneList.list().size() > 0) {
        //UserOneValue = userOneList.list().get(0).getSearchKey();
        UserOneId = userOneList.list().get(0).getId();
      } else {
        info.addResult("ERROR",
            String.format(Utility.messageBD(conn, "Efin_No_Defaultfuture1", language)));
      }
      // common user2
      OBQuery<UserDimension2> userTwoList = OBDal.getInstance().createQuery(UserDimension2.class,
          "as e where e.efinIsdefault='Y'");
      if (userTwoList.list().size() > 0) {
       // UserTwoValue = userTwoList.list().get(0).getSearchKey();
        UserTwoId = userTwoList.list().get(0).getId();
      } else {
        info.addResult("ERROR",
            String.format(Utility.messageBD(conn, "Efin_No_Defaultfuture2", language)));
      }

      obj.put("Organization", inpOrgId);
      obj.put("Department", DepartmentId);
      obj.put("Budget_Type", BudgetTypeId);
      obj.put("Project", ProjectId);
      obj.put("Classification", ActivityId);
      obj.put("Future1", UserOneId);
      obj.put("Future2", UserTwoId);

      // set unique code
      if (inpLastFieldChanged.equals("inpfinInIntransitAcct")) {
        if (inpInIntrAcct.equals("") || inpInIntrAcct == null) {
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Efin_In_Intransit_Unique').setValue('')");
          // info.addResult("inpemEfinInIntransitUnique", "");
        } else {
          /*
           * ElementValue elementvalue = OBDal.getInstance() .get(AccountingCombination.class,
           * inpInIntrAcct).getAccount(); uniqueCode = OrgValue + "-" + DepartmentValue + "-" +
           * elementvalue.getSearchKey() + "-" + BudgetTypeValue + "-" + ProjectValue + "-" +
           * ActivityValue + "-" + UserOneValue + "-" + UserTwoValue; obj.put("Account",
           * elementvalue.getId()); obj.put("UniqueCode", uniqueCode); strValidCombinationId =
           * sa.elm.ob.utility.util.Utility.getValidCombination(obj);
           * info.addResult("inpemEfinInIntransitUnique", strValidCombinationId);
           */
          info.addResult("inpemEfinInIntransitUnique", inpInIntrAcct);
          info.addResult("inpemEfinDepositUnique", inpfinInIntransitAcct);
          info.addResult("inpfinDepositAcct", inpfinInIntransitAcct);
        }
      }
      if (inpLastFieldChanged.equals("inpfinDepositAcct")) {
        if (inpDepositAcct.length() != 32) {
          info.addResult("inpemEfinDepositUnique", "");
        } else {
          /*
           * ElementValue elementvalue = OBDal.getInstance() .get(AccountingCombination.class,
           * inpDepositAcct).getAccount(); uniqueCode = OrgValue + "-" + DepartmentValue + "-" +
           * elementvalue.getSearchKey() + "-" + BudgetTypeValue + "-" + ProjectValue + "-" +
           * ActivityValue + "-" + UserOneValue + "-" + UserTwoValue; obj.put("Account",
           * elementvalue.getId()); obj.put("UniqueCode", uniqueCode); strValidCombinationId =
           * sa.elm.ob.utility.util.Utility.getValidCombination(obj);
           * info.addResult("inpemEfinDepositUnique", strValidCombinationId);
           */
          info.addResult("inpemEfinDepositUnique", inpDepositAcct);
        }

      }
      if (inpLastFieldChanged.equals("inpfinInClearAcct")) {
        if (inpInClearAcct.length() != 32) {
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Efin_In_Clear_Unique').setValue('')");
          // info.addResult("inpemEfinInClearUnique", "");
        } else {
          /*
           * ElementValue elementvalue = OBDal.getInstance() .get(AccountingCombination.class,
           * inpInClearAcct).getAccount(); uniqueCode = OrgValue + "-" + DepartmentValue + "-" +
           * elementvalue.getSearchKey() + "-" + BudgetTypeValue + "-" + ProjectValue + "-" +
           * ActivityValue + "-" + UserOneValue + "-" + UserTwoValue; obj.put("Account",
           * elementvalue.getId()); obj.put("UniqueCode", uniqueCode); strValidCombinationId =
           * sa.elm.ob.utility.util.Utility.getValidCombination(obj);
           * info.addResult("inpemEfinInClearUnique", strValidCombinationId);
           */
          info.addResult("inpemEfinInClearUnique", inpInClearAcct);
        }

      }
      if (inpLastFieldChanged.equals("inpfinOutIntransitAcct")) {
        if (inpOutIntransitAcct.length() != 32) {
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Efin_Out_Intransit_Unique').setValue('')");
          // info.addResult("inpemEfinOutIntransitUnique", "");
        } else {
          /*
           * ElementValue elementvalue = OBDal.getInstance() .get(AccountingCombination.class,
           * inpOutIntransitAcct).getAccount(); uniqueCode = OrgValue + "-" + DepartmentValue + "-"
           * + elementvalue.getSearchKey() + "-" + BudgetTypeValue + "-" + ProjectValue + "-" +
           * ActivityValue + "-" + UserOneValue + "-" + UserTwoValue; obj.put("Account",
           * elementvalue.getId()); obj.put("UniqueCode", uniqueCode); strValidCombinationId =
           * sa.elm.ob.utility.util.Utility.getValidCombination(obj);
           * info.addResult("inpemEfinOutIntransitUnique", strValidCombinationId);
           */
          info.addResult("inpemEfinOutIntransitUnique", inpOutIntransitAcct);
        }

      }
      if (inpLastFieldChanged.equals("inpfinWithdrawalAcct")) {
        if (inpWithdrawalAcct.length() != 32) {
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Efin_Withdrawal_Unique').setValue('')");
          // info.addResult("inpemEfinWithdrawalUnique", "");
        } else {
          /*
           * ElementValue elementvalue = OBDal.getInstance() .get(AccountingCombination.class,
           * inpWithdrawalAcct).getAccount(); uniqueCode = OrgValue + "-" + DepartmentValue + "-" +
           * elementvalue.getSearchKey() + "-" + BudgetTypeValue + "-" + ProjectValue + "-" +
           * ActivityValue + "-" + UserOneValue + "-" + UserTwoValue; obj.put("Account",
           * elementvalue.getId()); obj.put("UniqueCode", uniqueCode); strValidCombinationId =
           * sa.elm.ob.utility.util.Utility.getValidCombination(obj);
           * info.addResult("inpemEfinWithdrawalUnique", strValidCombinationId);
           */
          info.addResult("inpemEfinWithdrawalUnique", inpWithdrawalAcct);

        }

      }
      if (inpLastFieldChanged.equals("inpfinOutClearAcct")) {
        if (inpOutClearAcct.length() != 32) {
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Efin_Out_Clear_Unique').setValue('')");
          // info.addResult("inpemEfinOutClearUnique", "");
        } else {
          /*
           * ElementValue elementvalue = OBDal.getInstance() .get(AccountingCombination.class,
           * inpOutClearAcct).getAccount(); uniqueCode = OrgValue + "-" + DepartmentValue + "-" +
           * elementvalue.getSearchKey() + "-" + BudgetTypeValue + "-" + ProjectValue + "-" +
           * ActivityValue + "-" + UserOneValue + "-" + UserTwoValue; obj.put("Account",
           * elementvalue.getId()); obj.put("UniqueCode", uniqueCode); strValidCombinationId =
           * sa.elm.ob.utility.util.Utility.getValidCombination(obj);
           * info.addResult("inpemEfinOutClearUnique", strValidCombinationId);
           */
          info.addResult("inpemEfinOutClearUnique", inpOutClearAcct);
        }
      }
    } catch (Exception e) {
      log.debug("error in FinancialAccountConfigurationCallout", e);
    }

  }
}
