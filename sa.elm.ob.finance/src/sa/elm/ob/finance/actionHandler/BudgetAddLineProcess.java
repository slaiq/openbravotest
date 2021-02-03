package sa.elm.ob.finance.actionHandler;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.project.Project;

import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EFINBudgetLines;
import sa.elm.ob.finance.EFINBudgetTypeAcct;

public class BudgetAddLineProcess extends BaseProcessActionHandler {

  final private static Logger log = Logger.getLogger(BudgetAddLineProcess.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    Connection conn = OBDal.getInstance().getConnection();
    BudgetAddLineDAO addLineDao = new BudgetAddLineDAO(conn);
    try {
      OBContext.setAdminMode(true);
      JSONObject jsonRequest = new JSONObject(content);
      // Get the Params value
      log.debug("entering into Add line process");
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      final String budgetId = jsonRequest.getString("inpefinBudgetId");
      final String orgId = jsonparams.getString("AD_Org_ID");
      final String deptId = jsonparams.getString("C_SalesRegion_ID");
      final String budgTypeId = jsonparams.getString("Budget type");
      final String projectId = jsonparams.getString("C_Project_ID");
      final String activityId = (jsonparams.getString("FunClassification") == "null" ? "0"
          : (jsonparams.getString("FunClassification") == null ? null
              : jsonparams.getString("FunClassification")));
      final String future1 = (jsonparams.getString("Future1") == "null" ? "0"
          : (jsonparams.getString("Future1") == null ? null : jsonparams.getString("Future1")));
      final String future2 = (jsonparams.getString("Future2") == "null" ? "0"
          : (jsonparams.getString("Future1") == null ? null : jsonparams.getString("Future2")));
      final String projectAll = jsonparams.getString("allProject");
      final String funclassAll = jsonparams.getString("allFunctionalityclasses");
      final String future1All = jsonparams.getString("allFuture1");
      final String future2All = jsonparams.getString("allFuture2");
      boolean insert = true;

      long lineNo = 0;

      EFINBudget budgethd = OBDal.getInstance().get(EFINBudget.class, budgetId);
      // check account associate with budget type
      OBQuery<EFINBudgetTypeAcct> budgacct = OBDal.getInstance()
          .createQuery(EFINBudgetTypeAcct.class, " salesCampaign.id='" + budgTypeId + "'");

      // get line no
      OBQuery<EFINBudgetLines> budgline = OBDal.getInstance().createQuery(EFINBudgetLines.class,
          " efinBudget.id= :budgetID order by lineNo desc");
      budgline.setNamedParameter("budgetID", budgetId);
      budgline.setMaxResult(1);
      if (budgline.list().size() > 0) {
        EFINBudgetLines lineno = budgline.list().get(0);
        lineNo = lineno.getLineNo() + 10;
      } else {
        lineNo = 10;
      }

      // check account is associate with Selected BudgetType
      if (budgacct.list().size() > 0) {
        Project objProject = null;
        ABCActivity objFunctionalClassification = null;
        UserDimension1 objFuture1 = null;
        UserDimension2 objFuture2 = null;
        String strDepartment = "", strProject = "", strFunctionalCls = "", strUser1 = "",
            strUser2 = "";

        // validation part
        // check with budget Account == yes and allow budgeting ==no

        ElementValue objBudgetAccount = budgethd.getAccountElement();
        if (objBudgetAccount.isEfinAllowBudgeting() && !objBudgetAccount.isEfinProjacct()) {
          if (projectAll.equals("true")) {
            insert = false;
          }
          if (funclassAll.equals("true")) {
            insert = false;
          }
          if (future1All.equals("true")) {
            insert = false;
          }
          if (future2All.equals("true")) {
            insert = false;
          }
          if (projectAll.equals("false")) {
            objProject = OBDal.getInstance().get(Project.class, projectId);
            if (!objProject.isEFINDefault()) {
              insert = false;
            }
          }
          if (funclassAll.equals("false")) {
            objFunctionalClassification = OBDal.getInstance().get(ABCActivity.class, activityId);
            if (!objFunctionalClassification.isEfinIsdefault()) {
              insert = false;
            }
          }
          if (future1All.equals("false")) {
            objFuture1 = OBDal.getInstance().get(UserDimension1.class, future1);
            if (!objFuture1.isEfinIsdefault()) {
              insert = false;
            }
          }
          if (future2All.equals("false")) {
            objFuture2 = OBDal.getInstance().get(UserDimension2.class, future2);
            if (!objFuture2.isEfinIsdefault()) {
              insert = false;
            }
          }
          if (insert) {
            insert = addLineDao.isNonProjectAccountInsert(budgetId, budgTypeId, jsonparams, lineNo);
          } else {

          }
        } else if (objBudgetAccount.isEfinAllowBudgeting() && objBudgetAccount.isEfinProjacct()) {
          // Department List
          strDepartment = "'" + deptId + "'";
          // Project List
          if (!StringUtils.isEmpty(projectAll)) {
            if (projectAll.equals("true")) {
              OBQuery<Project> projectQuery = OBDal.getInstance().createQuery(Project.class,
                  " active=true and organization.id= :orgID and eFINDefault='N' order by value asc ");
              projectQuery.setNamedParameter("orgID", orgId);
              List<Project> projectList = projectQuery.list();
              if (projectList.size() == 0) {
                projectQuery = OBDal.getInstance().createQuery(Project.class,
                    " active=true and organization.id='0' and eFINDefault='N' order by value asc ");
                projectList = projectQuery.list();
              }

              if (projectList.size() > 0) {
                List<String> projects = new ArrayList<String>();
                for (Project project : projectList) {
                  projects.add("'" + project.getId() + "'");
                }
                strProject = StringUtils.join(projects, ",");
              }
            } else if (projectAll.equals("false")) {
              Project objCheckProject = OBDal.getInstance().get(Project.class, projectId);
              if (objCheckProject.isEFINDefault()) {
                insert = false;
              }
              strProject = "'" + projectId + "'";
            }
          }
          if (StringUtils.isEmpty(strProject)) {
            insert = false;
          }
          // Functional Classification List
          if (!StringUtils.isEmpty(funclassAll)) {
            if (funclassAll.equals("true")) {
              OBQuery<ABCActivity> functionQuery = OBDal.getInstance().createQuery(
                  ABCActivity.class,
                  " active=true and organization.id= :orgID and efinIsdefault='N' order by value asc ");
              functionQuery.setNamedParameter("orgID", orgId);
              List<ABCActivity> functionList = functionQuery.list();
              if (functionList.size() == 0) {
                functionQuery = OBDal.getInstance().createQuery(ABCActivity.class,
                    " active=true and organization.id='0' and efinIsdefault='N' order by value asc ");
                functionList = functionQuery.list();
              }

              if (functionList.size() > 0) {
                List<String> functioncls = new ArrayList<String>();
                for (ABCActivity functioncl : functionList) {
                  functioncls.add("'" + functioncl.getId() + "'");
                }
                strFunctionalCls = StringUtils.join(functioncls, ",");
              }
            } else if (funclassAll.equals("false")) {
              ABCActivity objCheckFcls = OBDal.getInstance().get(ABCActivity.class, activityId);
              if (objCheckFcls.isEfinIsdefault()) {
                insert = false;
              }
              strFunctionalCls = "'" + activityId + "'";
            }
          }
          if (StringUtils.isEmpty(strFunctionalCls)) {
            insert = false;
          }
          // User1 List
          if (!StringUtils.isEmpty(future1All)) {
            if (future1All.equals("true")) {
              OBQuery<UserDimension1> user1Query = OBDal.getInstance().createQuery(
                  UserDimension1.class,
                  " active=true and organization.id= :orgID and efinIsdefault='N' order by value asc ");
              user1Query.setNamedParameter("orgID", orgId);
              List<UserDimension1> useroneList = user1Query.list();
              if (useroneList.size() == 0) {
                user1Query = OBDal.getInstance().createQuery(UserDimension1.class,
                    " active=true and organization.id='0' and efinIsdefault ='N' order by value asc ");
                useroneList = user1Query.list();
              }

              if (useroneList.size() > 0) {
                List<String> userones = new ArrayList<String>();
                for (UserDimension1 userone : useroneList) {
                  userones.add("'" + userone.getId() + "'");
                }
                strUser1 = StringUtils.join(userones, ",");
              }
            } else if (future1All.equals("false")) {
              UserDimension1 objCheckUser1 = OBDal.getInstance().get(UserDimension1.class, future1);
              if (objCheckUser1.isEfinIsdefault()) {
                insert = false;
              }
              strUser1 = "'" + future1 + "'";
            }
          }
          if (StringUtils.isEmpty(strUser1)) {
            insert = false;
          }
          // User2 List
          if (!StringUtils.isEmpty(future2All)) {
            if (future2All.equals("true")) {
              OBQuery<UserDimension2> user2Query = OBDal.getInstance().createQuery(
                  UserDimension2.class,
                  " active=true and organization.id= :orgID and efinIsdefault='N' order by value asc ");
              user2Query.setNamedParameter("orgID", orgId);
              List<UserDimension2> userTwoList = user2Query.list();
              if (userTwoList.size() == 0) {
                user2Query = OBDal.getInstance().createQuery(UserDimension2.class,
                    " active=true and organization.id='0' and efinIsdefault='N' order by value asc ");
                userTwoList = user2Query.list();
              }

              if (userTwoList.size() > 0) {
                List<String> usertwos = new ArrayList<String>();
                for (UserDimension2 usertwo : userTwoList) {
                  usertwos.add("'" + usertwo.getId() + "'");
                }
                strUser2 = StringUtils.join(usertwos, ",");
              }
            } else if (future2All.equals("false")) {
              UserDimension2 objCheckUser2 = OBDal.getInstance().get(UserDimension2.class, future2);
              if (objCheckUser2.isEfinIsdefault()) {
                insert = false;
              }
              strUser2 = "'" + future2 + "'";
            }
          }
          if (StringUtils.isEmpty(strUser2)) {
            insert = false;
          }
          if (insert) {
            insert = addLineDao.isProjectAccountInsert(budgetId, budgTypeId, jsonparams, lineNo,
                strDepartment, strProject, strUser1, strUser2, strFunctionalCls);
          }
        }
        if (!insert) {
          JSONObject errormsg = new JSONObject();
          errormsg.put("severity", "error");
          errormsg.put("text", OBMessageUtils.messageBD("Efin_BudgetLine_NotInsert"));
          jsonResponse.put("message", errormsg);
        } else {
          JSONObject errormsg = new JSONObject();
          errormsg.put("severity", "success");
          errormsg.put("text", OBMessageUtils.messageBD("Efin_BudgetLine_Insert"));
          jsonResponse.put("message", errormsg);
        }
        // throw error
      } else {
        JSONObject errormsg = new JSONObject();
        errormsg.put("severity", "error");
        errormsg.put("text", OBMessageUtils.messageBD("Efin_BudgLine_NotDefAcct"));
        jsonResponse.put("message", errormsg);
      }
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

    } catch (final Exception e) {
      e.printStackTrace();
      JSONObject errormsg = new JSONObject();
      try {
        errormsg.put("severity", "error");
        errormsg.put("text",
            errormsg.put("text", OBMessageUtils.messageBD("Efin_BudgetLn_NotInsert")));
        jsonResponse.put("message", errormsg);
      } catch (JSONException e1) {
        e.printStackTrace();
        OBDal.getInstance().rollbackAndClose();
        log.error("exception :", e1);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonResponse;
  }
}
