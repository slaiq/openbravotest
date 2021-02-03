package sa.elm.ob.finance.actionHandler;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.project.Project;
import org.openbravo.model.sales.SalesRegion;

import sa.elm.ob.finance.EFINBudgetLines;
import sa.elm.ob.finance.EfinNonexpenseLines;
import sa.elm.ob.finance.dao.AdvPaymentMngtDao;

/**
 * 
 * @author qualian
 *
 */
public class EfinNonexpenseAddLineActionHandler extends BaseProcessActionHandler {
  /**
   * Generating uniquecode for Nonexpense accounts in add line popup
   */
  final private static Logger log = Logger.getLogger(EfinBudgetAddLineActionHandler.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    try {
      OBContext.setAdminMode(true);
      JSONObject jsonRequest = new JSONObject(content);
      // Get the Params value
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      // final String budgetId = jsonRequest.getString("inpefinBudgetId");
      final String orgId = jsonparams.getString("AD_Org_ID");
      final String deptId = jsonparams.getString("C_SalesRegion_ID");
      final String accId = jsonparams.getString("C_Elementvalue_ID");
      final String budgTypeId = jsonparams.getString("C_Campaign");
      final String projectId = jsonparams.getString("C_Project_ID");
      final String activityId = (jsonparams.getString("FunClassification") == "null" ? "0"
          : (jsonparams.getString("FunClassification") == null ? null
              : jsonparams.getString("FunClassification")));
      final String future1 = (jsonparams.getString("Future1") == "null" ? "0"
          : (jsonparams.getString("Future1") == null ? null : jsonparams.getString("Future1")));
      final String future2 = (jsonparams.getString("Future2") == "null" ? "0"
          : (jsonparams.getString("Future1") == null ? null : jsonparams.getString("Future2")));
      final String orgAll = jsonparams.getString("allOrganization");
      final String deptAll = jsonparams.getString("allDepartment");
      final String accAll = jsonparams.getString("allElement");
      final String projectAll = jsonparams.getString("allProject");
      final String funclassAll = jsonparams.getString("allFunctionalityclasses");
      final String future1All = jsonparams.getString("allFuture1");
      final String future2All = jsonparams.getString("allFuture2");
      boolean insert = true;
      long lineNo = 0;
      Campaign budType = OBDal.getInstance().get(Campaign.class, budgTypeId);

      OBQuery<Organization> orgall = null;
      OBQuery<SalesRegion> deptall = null;
      OBQuery<ElementValue> accall = null;
      OBQuery<Project> projectall = null;
      OBQuery<ABCActivity> funclassall = null;
      OBQuery<UserDimension1> future1all = null;
      OBQuery<UserDimension2> future2all = null;
      // OBQuery<ElementValue> node = null;
      JSONObject result = new JSONObject(), json = null;
      // Organization List
      if (!org.apache.commons.lang.StringUtils.isEmpty(orgAll)) {
        JSONArray jsonarr = new JSONArray();
        if (orgAll.equals("true")) {
          orgall = OBDal.getInstance().createQuery(Organization.class,
              " active=true and id <>'0' and id in ( select  rol.organization.id from ADRoleOrganization rol  where  rol.role.id ='"
                  + vars.getRole() + "') order by searchKey asc ");
        } else if (orgAll.equals("false")) {
          orgall = OBDal.getInstance().createQuery(Organization.class,
              " active=true and id='" + orgId + "' order by searchKey asc ");
        }
        if (orgall != null) {
          if (orgall.list().size() > 0) {
            for (int i = 0; i < orgall.list().size(); i++) {
              Organization org = orgall.list().get(i);
              json = new JSONObject();
              json.put("orgId", org.getId());
              json.put("orgKey", org.getSearchKey());
              jsonarr.put(json);
            }
            log.debug("orglist all:" + jsonarr);
            result.put("orgList", jsonarr);
          }
        }
      }
      JSONObject jsonorg = result;
      JSONArray jsonArray = jsonorg.getJSONArray("orgList");
      // Department List
      if (!StringUtils.isEmpty(deptAll)) {
        JSONObject jsondept = null;
        JSONArray jsonarr = new JSONArray();

        if (deptAll.equals("true")) {
          deptall = OBDal.getInstance().createQuery(SalesRegion.class,
              " active=true order by value asc ");
        } else if (deptAll.equals("false")) {
          deptall = OBDal.getInstance().createQuery(SalesRegion.class,
              " active=true and id='" + deptId + "' order by searchKey asc ");
        }
        dept: for (int i = 0; i < jsonArray.length(); i++) {
          json = jsonArray.getJSONObject(i);
          if (deptall.list().size() > 0) {
            for (int j = 0; j < deptall.list().size(); j++) {
              SalesRegion deptorg = deptall.list().get(j);
              if (json.getString("orgId").equals(deptorg.getOrganization().getId())
                  || (deptAll.equals("false") && deptorg.isDefault())) {
                if ((deptAll.equals("false") && deptorg.isDefault()) && orgAll.equals("true")) {
                  jsondept = new JSONObject();
                  jsondept.put("deptId", deptorg.getId());
                  jsondept.put("deptKey", deptorg.getSearchKey());
                  jsondept.put("deptOrg", json.getString("orgId"));
                  jsondept.put("deptOrgKey", json.getString("orgKey"));
                  jsondept.put("deptDefault", deptorg.isDefault());
                  jsonarr.put(jsondept);
                  result.put("deptList", jsonarr);
                  break dept;
                } else {
                  jsondept = new JSONObject();
                  jsondept.put("deptId", deptorg.getId());
                  jsondept.put("deptKey", deptorg.getSearchKey());
                  jsondept.put("deptOrg", json.getString("orgId"));
                  jsondept.put("deptOrgKey", json.getString("orgKey"));
                  jsondept.put("deptDefault", deptorg.isDefault());
                  jsonarr.put(jsondept);
                }
              }
            }
            result.put("deptList", jsonarr);
            // log.debug("arrayjson:"+jsonarr);
          }
        }
      }
      // Account List
      if (!StringUtils.isEmpty(accAll)) {
        JSONObject jsonacc = null;
        JSONArray jsonarr = new JSONArray();

        if (accAll.equals("true")) {
          accall = OBDal.getInstance().createQuery(ElementValue.class,
              "elementLevel='S' and active=true order by value asc ");
        } else if (accAll.equals("false")) {
          accall = OBDal.getInstance().createQuery(ElementValue.class,
              " active=true and id='" + accId + "' order by searchKey asc ");
        }
        if (accall != null) {
          if (accall.list() != null && accall.list().size() > 0) {
            for (int j = 0; j < accall.list().size(); j++) {
              ElementValue acc = accall.list().get(j);
              OBQuery<EFINBudgetLines> budget = OBDal.getInstance()
                  .createQuery(EFINBudgetLines.class, "accountElement.id='" + acc.getId() + "'");
              if (budget.list().size() == 0) {
                jsonacc = new JSONObject();
                jsonacc.put("acctId", acc.getId());
                jsonacc.put("acctKey", acc.getSearchKey());
                jsonacc.put("acctOrg", acc.getOrganization().getId());
                jsonacc.put("acctOrgKey", acc.getOrganization().getSearchKey());
                // jsonacc.put("acctDefault", deptorg.isDefault());
                jsonarr.put(jsonacc);
              }

            }
            result.put("acctList", jsonarr);
            // log.debug("arrayjson:"+jsonarr);
          }
        }

      }
      // Project List
      if (!StringUtils.isEmpty(projectAll)) {
        JSONObject jsonpro = null;
        JSONArray jsonarr = new JSONArray();

        if (projectAll.equals("true")) {
          projectall = OBDal.getInstance().createQuery(Project.class,
              " active=true order by value asc ");
        } else if (projectAll.equals("false")) {
          projectall = OBDal.getInstance().createQuery(Project.class,
              " active=true and id='" + projectId + "' order by searchKey asc ");
        }
        project: for (int i = 0; i < jsonArray.length(); i++) {
          json = jsonArray.getJSONObject(i);
          if (projectall.list().size() > 0) {
            for (int j = 0; j < projectall.list().size(); j++) {
              Project proj = projectall.list().get(j);
              if (json.getString("orgId").equals(proj.getOrganization().getId())
                  || (projectAll.equals("false") && proj.isEFINDefault())) {
                if ((projectAll.equals("false") && proj.isEFINDefault()) && orgAll.equals("true")) {
                  jsonpro = new JSONObject();
                  jsonpro.put("projId", proj.getId());
                  jsonpro.put("projKey", proj.getSearchKey());
                  jsonpro.put("proDefault", proj.isEFINDefault());
                  jsonpro.put("proOrg", json.getString("orgId"));
                  jsonpro.put("proDefault", proj.isEFINDefault());
                  jsonarr.put(jsonpro);
                  result.put("projList", jsonarr);
                  break project;
                } else {
                  jsonpro = new JSONObject();
                  jsonpro.put("projId", proj.getId());
                  jsonpro.put("projKey", proj.getSearchKey());
                  jsonpro.put("proDefault", proj.isEFINDefault());
                  jsonpro.put("proOrg", json.getString("orgId"));
                  jsonpro.put("proDefault", proj.isEFINDefault());
                  jsonarr.put(jsonpro);
                }
              }
            }
            result.put("projList", jsonarr);
          }
        }
      }
      // Functional Classification List
      if (!StringUtils.isEmpty(funclassAll)) {
        JSONObject jsonact = null;
        JSONArray jsonarr = new JSONArray();
        if (funclassAll.equals("true")) {
          funclassall = OBDal.getInstance().createQuery(ABCActivity.class,
              " active=true order by searchKey asc ");
        } else if (funclassAll.equals("false")) {
          funclassall = OBDal.getInstance().createQuery(ABCActivity.class,
              " active=true and id='" + activityId + "' order by searchKey asc ");
        }
        funclass: for (int i = 0; i < jsonArray.length(); i++) {
          json = jsonArray.getJSONObject(i);
          if (funclassall.list().size() > 0) {
            for (int j = 0; j < funclassall.list().size(); j++) {
              ABCActivity act = funclassall.list().get(j);
              if (json.getString("orgId").equals(act.getOrganization().getId())
                  || (funclassAll.equals("false") && act.isEfinIsdefault())) {
                if ((funclassAll.equals("false") && act.isEfinIsdefault())
                    && orgAll.equals("true")) {
                  jsonact = new JSONObject();
                  jsonact.put("actId", act.getId());
                  jsonact.put("actKey", act.getSearchKey());
                  jsonact.put("actDefault", act.isEfinIsdefault());
                  jsonact.put("actOrg", act.getOrganization().getId());
                  jsonact.put("actDefault", act.isEfinIsdefault());
                  jsonarr.put(jsonact);
                  result.put("actList", jsonarr);
                  break funclass;
                } else {
                  jsonact = new JSONObject();
                  jsonact.put("actId", act.getId());
                  jsonact.put("actKey", act.getSearchKey());
                  jsonact.put("actDefault", act.isEfinIsdefault());
                  jsonact.put("actOrg", act.getOrganization().getId());
                  jsonact.put("actDefault", act.isEfinIsdefault());
                  jsonarr.put(jsonact);
                }
              }
            }
            result.put("actList", jsonarr);
          }
        }
      }
      // User1 List
      if (!StringUtils.isEmpty(future1All)) {
        JSONObject jsonuser1 = null;
        JSONArray jsonarr = new JSONArray();
        if (future1All.equals("true")) {
          future1all = OBDal.getInstance().createQuery(UserDimension1.class,
              " active=true order by searchKey asc ");
        } else if (future1All.equals("false")) {
          future1all = OBDal.getInstance().createQuery(UserDimension1.class,
              " active=true and id='" + future1 + "' order by searchKey asc ");
        }
        user1: for (int i = 0; i < jsonArray.length(); i++) {
          json = jsonArray.getJSONObject(i);
          if (future1all.list().size() > 0) {
            for (int j = 0; j < future1all.list().size(); j++) {
              log.debug("fut1List size:" + future1all.list().size());
              UserDimension1 fut1 = future1all.list().get(j);
              if (json.getString("orgId").equals(fut1.getOrganization().getId())
                  || (future1All.equals("false") && fut1.isEfinIsdefault())) {
                log.debug("fut1List 1st:");
                if ((future1All.equals("false") && fut1.isEfinIsdefault())
                    && orgAll.equals("true")) {
                  log.debug("fut1List 2nd:");
                  jsonuser1 = new JSONObject();
                  jsonuser1.put("fut1Id", fut1.getId());
                  jsonuser1.put("fut1Key", fut1.getSearchKey());
                  jsonuser1.put("fut1Default", fut1.isEfinIsdefault());
                  jsonuser1.put("fut1Org", fut1.getOrganization().getId());
                  jsonuser1.put("fut1Default", fut1.isEfinIsdefault());
                  jsonarr.put(jsonuser1);
                  result.put("fut1List", jsonarr);
                  break user1;
                } else {
                  log.debug("fut1List 3rd:");
                  jsonuser1 = new JSONObject();
                  jsonuser1.put("fut1Id", fut1.getId());
                  jsonuser1.put("fut1Key", fut1.getSearchKey());
                  jsonuser1.put("fut1Default", fut1.isEfinIsdefault());
                  jsonuser1.put("fut1Org", fut1.getOrganization().getId());
                  jsonuser1.put("fut1Default", fut1.isEfinIsdefault());
                  jsonarr.put(jsonuser1);
                }
              }
            }
            result.put("fut1List", jsonarr);
            log.debug("fut1List:" + jsonarr);
          }
        }
      }
      // User2 List
      if (!StringUtils.isEmpty(future2All)) {
        JSONObject jsonuser2 = null;
        JSONArray jsonarr = new JSONArray();
        if (future2All.equals("true")) {
          future2all = OBDal.getInstance().createQuery(UserDimension2.class,
              " active=true order by value asc ");

        } else if (future2All.equals("false")) {
          future2all = OBDal.getInstance().createQuery(UserDimension2.class,
              " active=true and id='" + future2 + "' order by searchKey asc ");
        }
        user2: for (int i = 0; i < jsonArray.length(); i++) {
          json = jsonArray.getJSONObject(i);
          if (future2all.list().size() > 0) {
            for (int j = 0; j < future2all.list().size(); j++) {
              UserDimension2 fut2 = future2all.list().get(j);
              if (json.getString("orgId").equals(fut2.getOrganization().getId())
                  || (future2All.equals("false") && fut2.isEfinIsdefault())) {
                if ((future2All.equals("false") && fut2.isEfinIsdefault())
                    && orgAll.equals("true")) {
                  jsonuser2 = new JSONObject();
                  jsonuser2.put("fut2Id", fut2.getId());
                  jsonuser2.put("fut2Key", fut2.getSearchKey());
                  jsonuser2.put("fut2Default", fut2.isEfinIsdefault());
                  jsonuser2.put("fut2Org", fut2.getOrganization().getId());
                  jsonuser2.put("fut2Default", fut2.isEfinIsdefault());
                  jsonarr.put(jsonuser2);
                  result.put("fut2List", jsonarr);
                  break user2;
                } else {
                  jsonuser2 = new JSONObject();
                  jsonuser2.put("fut2Id", fut2.getId());
                  jsonuser2.put("fut2Key", fut2.getSearchKey());
                  jsonuser2.put("fut2Default", fut2.isEfinIsdefault());
                  jsonuser2.put("fut2Org", fut2.getOrganization().getId());
                  jsonuser2.put("fut2Default", fut2.isEfinIsdefault());
                  jsonarr.put(jsonuser2);
                }
              }
            }
            result.put("fut2List", jsonarr);
          }
        }
      }

      JSONObject all = result, objorg = null;
      JSONArray jsonArrayorg = all.getJSONArray("orgList");
      // JSONArray jsonArraydept = all.getJSONArray("deptList");

      // checking account is wrongly mapped with project and account not mapped with project
      log.debug("result:" + result.toString());
      // if error not getting insert the line
      if (insert) {
        log.debug("jsonArraydept:" + jsonArrayorg.length());
        for (int j = 0; j < jsonArrayorg.length(); j++) {
          objorg = jsonArrayorg.getJSONObject(j);

          JSONArray jsonArraydept = all.getJSONArray("deptList");
          for (int d = 0; d < jsonArraydept.length(); d++) {
            JSONObject objdept = jsonArraydept.getJSONObject(d);
            if (objdept.getString("deptDefault").equals("false"))
              if (!objdept.getString("deptOrg").equals(objorg.getString("orgId")))
                continue;
            JSONArray jsonArrayproj = all.getJSONArray("projList");
            for (int p = 0; p < jsonArrayproj.length(); p++) {
              JSONObject objproj = jsonArrayproj.getJSONObject(p);
              if (objproj.getString("proDefault").equals("false"))
                if (!objproj.getString("proOrg").equals(objorg.getString("orgId")))
                  continue;
              JSONArray jsonArrayfunclass = all.getJSONArray("actList");
              for (int fc = 0; fc < jsonArrayfunclass.length(); fc++) {
                JSONObject objfc = jsonArrayfunclass.getJSONObject(fc);
                if (objfc.getString("actDefault").equals("false"))
                  if (!objfc.getString("actOrg").equals(objorg.getString("orgId")))
                    continue;
                JSONArray jsonArrayfu1 = all.getJSONArray("fut1List");
                for (int f1 = 0; f1 < jsonArrayfu1.length(); f1++) {
                  JSONObject objf1 = jsonArrayfu1.getJSONObject(f1);
                  if (objf1.getString("fut1Default").equals("false"))
                    if (!objf1.getString("fut1Org").equals(objorg.getString("orgId")))
                      continue;
                  JSONArray jsonArrayfu2 = all.getJSONArray("fut2List");
                  for (int f2 = 0; f2 < jsonArrayfu2.length(); f2++) {
                    JSONObject objf2 = jsonArrayfu2.getJSONObject(f2);
                    if (objf2.getString("fut2Default").equals("false"))
                      if (!objf2.getString("fut2Org").equals(objorg.getString("orgId")))
                        continue;
                    JSONArray jsonArrayacct = all.getJSONArray("acctList");
                    for (int a = 0; a < jsonArrayacct.length(); a++) {
                      JSONObject objacct = jsonArrayacct.getJSONObject(a);

                      EfinNonexpenseLines line = OBProvider.getInstance()
                          .get(EfinNonexpenseLines.class);
                      try {
                        // Check Already same dimension is exists or not.if exists Iterate the next
                        // value.
                        String uniquecode = objorg.getString("orgKey") + "-"
                            + objdept.getString("deptKey") + "-" + objacct.getString("acctKey")
                            + "-" + budType.getSearchKey() + "-" + objproj.getString("projKey")
                            + "-" + objfc.getString("actKey") + "-" + objf1.getString("fut1Key")
                            + "-" + objf2.getString("fut2Key");
                        OBQuery<EfinNonexpenseLines> duplicate = OBDal.getInstance().createQuery(
                            EfinNonexpenseLines.class, " uniqueCode ='" + uniquecode + "'");
                        if (duplicate.list().size() > 0)
                          continue;
                        log.debug("unique codeee:" + uniquecode);
                        line.setClient(dao.getObject(Client.class, vars.getClient()));
                        line.setOrganization(
                            dao.getObject(Organization.class, objorg.getString("orgId")));
                        line.setUniqueCode(uniquecode);
                        line.setAccountElement(OBDal.getInstance().get(ElementValue.class,
                            objacct.getString("acctId")));
                        line.setActivity(
                            OBDal.getInstance().get(ABCActivity.class, objfc.getString("actId")));
                        line.setProject(
                            OBDal.getInstance().get(Project.class, objproj.getString("projId")));
                        line.setStDimension(OBDal.getInstance().get(UserDimension1.class,
                            objf1.getString("fut1Id")));
                        line.setNdDimension(OBDal.getInstance().get(UserDimension2.class,
                            objf2.getString("fut2Id")));
                        line.setSalesRegion(OBDal.getInstance().get(SalesRegion.class,
                            objdept.getString("deptId")));
                        line.setSalesCampaign(OBDal.getInstance().get(Campaign.class, budgTypeId));
                        lineNo += 10;
                        OBDal.getInstance().save(line);
                      } catch (NullPointerException ex) {
                        ex.printStackTrace();
                      }
                    }
                  }
                }
              }
            }
          }
        }
        if (lineNo >= 10) {
          JSONObject errormsg = new JSONObject();
          errormsg.put("severity", "success");
          errormsg.put("text", OBMessageUtils.messageBD("Efin_BudgetLine_Insert"));
          jsonResponse.put("message", errormsg);
        } else {
          JSONObject errormsg = new JSONObject();
          errormsg.put("severity", "error");
          errormsg.put("text", OBMessageUtils.messageBD("Efin_BudgetLine_NotInsert"));
          jsonResponse.put("message", errormsg);
        }
      }
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (final Exception e) {
      log.error("exception :", e);
      JSONObject errormsg = new JSONObject();
      try {
        errormsg.put("severity", "error");
        errormsg.put("text",
            errormsg.put("text", OBMessageUtils.messageBD("Efin_BudgetLn_NotInsert")));
        jsonResponse.put("message", errormsg);
      } catch (JSONException e1) {
        OBDal.getInstance().rollbackAndClose();
        log.error("exception :", e1);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonResponse;
  }
}
