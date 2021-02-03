package sa.elm.ob.finance.actionHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
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

import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EFINBudgetLines;
import sa.elm.ob.finance.EFINBudgetTypeAcct;
import sa.elm.ob.finance.dao.AdvPaymentMngtDao;

public class EfinBudgetAddLineActionHandler extends BaseProcessActionHandler {

  final private static Logger log = Logger.getLogger(EfinBudgetAddLineActionHandler.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    PreparedStatement ps = null;
    Connection conn = OBDal.getInstance().getConnection();
    String organizationId = null;
    String tempParentOrgId = "";
    String removefirstOrgId = null;
    ResultSet rs = null;
    String childactlist = null;
    try {
      OBContext.setAdminMode(true);
      JSONObject jsonRequest = new JSONObject(content);
      // Get the Params value
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      final String budgetId = jsonRequest.getString("inpefinBudgetId");
      final String orgId = jsonparams.getString("AD_Org_ID");
      final String deptId = jsonparams.getString("C_SalesRegion_ID");
      final String budgTypeId = jsonparams.getString("Budget type");
      final String projectId = jsonparams.getString("C_Project_ID");
      final String activityId = (jsonparams.getString("FunClassification") == "null" ? "0"
          : (jsonparams.getString("FunClassification").equals(null) ? null
              : jsonparams.getString("FunClassification")));
      final String future1 = (jsonparams.getString("Future1") == "null" ? "0"
          : (jsonparams.getString("Future1").equals(null) ? null
              : jsonparams.getString("Future1")));
      final String future2 = (jsonparams.getString("Future2") == "null" ? "0"
          : (jsonparams.getString("Future1").equals(null) ? null
              : jsonparams.getString("Future2")));
      final String orgAll = jsonparams.getString("allOrganization");
      final String deptAll = jsonparams.getString("allDepartment");
      final String projectAll = jsonparams.getString("allProject");
      final String funclassAll = jsonparams.getString("allFunctionalityclasses");
      final String future1All = jsonparams.getString("allFuture1");
      final String future2All = jsonparams.getString("allFuture2");
      boolean insert = true;
      String acountcode = "";
      Campaign budType = OBDal.getInstance().get(Campaign.class, budgTypeId);

      OBQuery<ElementValue> qry = null;
      BigDecimal amount = BigDecimal.ZERO;
      long lineNo = 0;

      EFINBudget budgethd = OBDal.getInstance().get(EFINBudget.class, budgetId);
      // check account associate with budget type
      OBQuery<EFINBudgetTypeAcct> budgacct = OBDal.getInstance()
          .createQuery(EFINBudgetTypeAcct.class, " salesCampaign.id= :salesCampaignID");
      budgacct.setNamedParameter("salesCampaignID", budgTypeId);
      // get line no
      OBQuery<EFINBudgetLines> budgline = OBDal.getInstance().createQuery(EFINBudgetLines.class,
          " efinBudget.id= :efinBudgetID order by lineNo desc");
      budgline.setNamedParameter("efinBudgetID", budgetId);
      budgline.setMaxResult(1);
      if (budgline.list().size() > 0) {
        EFINBudgetLines lineno = budgline.list().get(0);
        lineNo = lineno.getLineNo() + 10;
      } else {
        lineNo = 10;
      }

      // check account is associate with Selected BudgetType
      if (budgacct.list().size() > 0) {

        OBQuery<Organization> orgall = null;
        OBQuery<SalesRegion> deptall = null;
        OBQuery<Project> projectall = null;
        OBQuery<ABCActivity> funclassall = null;
        OBQuery<UserDimension1> future1all = null;
        OBQuery<UserDimension2> future2all = null;
        OBQuery<ElementValue> node = null;
        JSONObject result = new JSONObject(), json = null;
        JSONObject msg = new JSONObject();
        JSONObject mapwrong = new JSONObject();
        JSONObject mapnot = new JSONObject();
        JSONArray array = new JSONArray();
        JSONArray array1 = new JSONArray();
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
                    || proj.getOrganization().getId().equals("0")
                    || (projectAll.equals("false") && proj.isEFINDefault())) {
                  if ((projectAll.equals("false") && proj.isEFINDefault())
                      && orgAll.equals("true")) {
                    jsonpro = new JSONObject();
                    jsonpro.put("projId", proj.getId());
                    jsonpro.put("projKey", proj.getSearchKey());
                    jsonpro.put("proDefault", proj.isEFINDefault());
                    jsonpro.put("proOrg", proj.getOrganization().getId());
                    jsonpro.put("proDefault", proj.isEFINDefault());
                    jsonarr.put(jsonpro);
                    result.put("projList", jsonarr);
                    break project;
                  } else {
                    jsonpro = new JSONObject();
                    jsonpro.put("projId", proj.getId());
                    jsonpro.put("projKey", proj.getSearchKey());
                    jsonpro.put("proDefault", proj.isEFINDefault());
                    jsonpro.put("proOrg", proj.getOrganization().getId());
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
        for (int j = 0; j < jsonArrayorg.length(); j++) {

          objorg = jsonArrayorg.getJSONObject(j);
          EFINBudget budget = OBDal.getInstance().get(EFINBudget.class, budgetId);
          SQLQuery childactqry = OBDal.getInstance().getSession().createSQLQuery(
              " select eut_getchildacct('" + budget.getAccountElement().getId() + "')");
          if (childactqry.list().size() > 0) {
            childactlist = (String) childactqry.list().get(0);
          }
          qry = OBDal.getInstance().createQuery(ElementValue.class,
              " id in (" + childactlist + ") and elementLevel='S'  and organization.id ='"
                  + objorg.getString("orgId") + "'   order by value asc  ");
          qry.setFilterOnReadableOrganization(false);
          if (qry.list().size() == 0) {
            // get parent organization list
            ps = conn.prepareStatement("select eut_parent_org ('" + objorg.getString("orgId")
                + "','" + vars.getClient() + "')");
            rs = ps.executeQuery();
            if (rs.next()) {
              organizationId = rs.getString("eut_parent_org");
              removefirstOrgId = organizationId.replaceFirst("'" + objorg.getString("orgId") + "',",
                  "");
            }
            if (!tempParentOrgId.equals(removefirstOrgId)) {
              qry = OBDal.getInstance().createQuery(ElementValue.class,
                  " id in ( " + childactlist + ") and elementLevel='S'  and organization.id  in ("
                      + organizationId + ")   order by value asc  ");
              qry.setFilterOnReadableOrganization(false);
              tempParentOrgId = removefirstOrgId;
            }
          }

          if (qry.list().size() > 0) {
            for (int k = 0; k < qry.list().size(); k++) {
              ElementValue elmlist = qry.list().get(k);
              // getting parent value
              node = OBDal.getInstance().createQuery(ElementValue.class,
                  " id in ( select reportSet from ADTreeNode where  node='" + elmlist.getId()
                      + "' ) ");

              ElementValue parentacct = node.list().get(0);
              // directly check the budget account mark as isproject or not
              ElementValue budgetAccount = budget.getAccountElement();

              if (budgetAccount.isEfinProjacct().equals(false)) {
                if (elmlist.getEfinProject() != null) {
                  insert = false;
                  if (!acountcode.equals(elmlist.getSearchKey())) {
                    acountcode += elmlist.getSearchKey();
                    mapwrong = new JSONObject();
                    mapwrong.put("id", elmlist.getId());
                    mapwrong.put("msg", "Wrongly Map");
                    mapwrong.put("code", elmlist.getSearchKey());
                    mapwrong.put("parentid", parentacct.getId());
                    mapwrong.put("parentKey", parentacct.getSearchKey());
                    array.put(mapwrong);
                  }
                  msg.put("WrongMap", array);
                  continue;
                } else {
                  continue;
                }
              } else if (budgetAccount.isEfinProjacct().equals(true)) {
                if (elmlist.getEfinProject() == null) {
                  insert = false;
                  if (!acountcode.equals(elmlist.getSearchKey())) {
                    acountcode += elmlist.getSearchKey() + "(" + parentacct.getSearchKey() + "), ";
                    mapnot = new JSONObject();
                    mapnot.put("id", elmlist.getId());
                    mapnot.put("msg", "Not Map");
                    mapnot.put("code", elmlist.getSearchKey());
                    mapnot.put("parentid", parentacct.getId());
                    mapnot.put("parentKey", parentacct.getSearchKey());
                    array1.put(mapnot);
                  }
                  msg.put("NotMapList", array1);
                  continue;
                } else {
                  continue;
                }
              }
            }
          }
          // log.debug("acountcode:" + acountcode);
          if (!insert) {
            continue;
          }
        }
        log.debug("result:" + result.toString());
        // if error not getting insert the line
        if (insert) {
          log.debug("jsonArraydept:" + jsonArrayorg.length());
          for (int j = 0; j < jsonArrayorg.length(); j++) {
            objorg = jsonArrayorg.getJSONObject(j);
            qry = OBDal.getInstance().createQuery(ElementValue.class,
                " id in (" + childactlist + ") and elementLevel='S'  and organization.id ='"
                    + objorg.getString("orgId") + "'   order by value asc  ");
            qry.setFilterOnReadableOrganization(false);
            if (qry.list().size() == 0) {

              // get parent organization list
              ps = conn.prepareStatement("select eut_parent_org ('" + objorg.getString("orgId")
                  + "','" + vars.getClient() + "')");
              rs = ps.executeQuery();
              if (rs.next()) {
                organizationId = rs.getString("eut_parent_org");
              }

              qry = OBDal.getInstance().createQuery(ElementValue.class,
                  " id in (" + childactlist + ") and elementLevel='S'  and organization.id  in ("
                      + organizationId + ")   order by value asc  ");
              qry.setFilterOnReadableOrganization(false);
            }
            if (qry.list().size() > 0) {
              for (int k = 0; k < qry.list().size(); k++) {
                ElementValue elmlist = qry.list().get(k);
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
                      if (!objproj.getString("proOrg").equals(objorg.getString("orgId"))
                          && !objproj.getString("proOrg").equals("0"))
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
                          EFINBudgetLines line = OBProvider.getInstance()
                              .get(EFINBudgetLines.class);
                          try {
                            // Check Already same dimension is exists or not.if exists Iterate the
                            // next value.
                            String uniquecode = objorg.getString("orgKey") + "-"
                                + objdept.getString("deptKey") + "-" + elmlist.getSearchKey() + "-"
                                + budType.getSearchKey() + "-" + objproj.getString("projKey") + "-"
                                + objfc.getString("actKey") + "-" + objf1.getString("fut1Key") + "-"
                                + objf2.getString("fut2Key");
                            OBQuery<EFINBudgetLines> duplicate = OBDal.getInstance()
                                .createQuery(EFINBudgetLines.class, " uniquecode ='" + uniquecode
                                    + "' and efinBudget.id ='" + budgetId + "'");
                            if (duplicate.list().size() > 0)
                              continue;
                            // log.debug("unique codeee:"+uniquecode);
                            node = OBDal.getInstance().createQuery(ElementValue.class,
                                " id in ( select reportSet from ADTreeNode where  node='"
                                    + elmlist.getId() + "' ) ");
                            ElementValue parentacct = budgethd.getAccountElement();
                            // if(objproj.getString("proOrg").equals(objorg.getString("orgId"))){
                            if (projectAll.equals("true")) {
                              if (parentacct.isEfinProjacct().equals(true)) {
                                if (objproj.getString("proDefault").equals("true"))
                                  continue;
                                else if (objproj.getString("proDefault").equals("false")) {
                                  if (elmlist.getEfinProject().getId()
                                      .equals(objproj.getString("projId")))
                                    line.setProject(OBDal.getInstance().get(Project.class,
                                        objproj.getString("projId")));
                                  else
                                    continue;
                                }
                              } else if (parentacct.isEfinProjacct().equals(false)) {
                                if (objproj.getString("proDefault").equals("true")) {
                                  line.setProject(OBDal.getInstance().get(Project.class,
                                      objproj.getString("projId")));
                                } else
                                  continue;
                              }
                            } else if (projectAll.equals("false")) {
                              if (objproj.getString("proDefault").equals("true")) {
                                if (parentacct.isEfinProjacct().equals(false)) {
                                  line.setProject(OBDal.getInstance().get(Project.class,
                                      objproj.getString("projId")));
                                } else
                                  continue;
                              } else if (objproj.getString("proDefault").equals("false")) {
                                if (parentacct.isEfinProjacct().equals(true)) {
                                  if (elmlist.getEfinProject().getId()
                                      .equals(objproj.getString("projId")))
                                    line.setProject(OBDal.getInstance().get(Project.class,
                                        objproj.getString("projId")));
                                  else
                                    continue;
                                } else
                                  continue;
                              }
                            }
                            // }else
                            // continue;
                            line.setClient(dao.getObject(Client.class, vars.getClient()));
                            line.setOrganization(
                                dao.getObject(Organization.class, objorg.getString("orgId")));
                            line.setUniquecode(uniquecode);
                            line.setAccountElement(
                                OBDal.getInstance().get(ElementValue.class, elmlist.getId()));
                            line.setAmount(amount);
                            line.setLineNo(lineNo);
                            line.setEfinBudget(OBDal.getInstance().get(EFINBudget.class, budgetId));
                            line.setActivity(OBDal.getInstance().get(ABCActivity.class,
                                objfc.getString("actId")));
                            line.setStDimension(OBDal.getInstance().get(UserDimension1.class,
                                objf1.getString("fut1Id")));
                            line.setNdDimension(OBDal.getInstance().get(UserDimension2.class,
                                objf2.getString("fut2Id")));
                            line.setSalesRegion(OBDal.getInstance().get(SalesRegion.class,
                                objdept.getString("deptId")));
                            line.setSalesCampaign(
                                OBDal.getInstance().get(Campaign.class, budgTypeId));
                            OBDal.getInstance().save(line);
                            lineNo += 10;
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
          }
          if (lineNo > 10) {
            JSONObject errormsg = new JSONObject();
            errormsg.put("severity", "success");
            errormsg.put("text", OBMessageUtils.messageBD("Efin_BudgetLine_Insert"));
            jsonResponse.put("message", errormsg);
          } else if (lineNo == 10) {
            JSONObject errormsg = new JSONObject();
            errormsg.put("severity", "error");
            errormsg.put("text", OBMessageUtils.messageBD("Efin_BudgetLine_NotInsert"));
            jsonResponse.put("message", errormsg);
          }
        }
        // throw error
        else if (!insert) {
          String errorwrongmap = "", errornotmap = "";
          JSONObject errormsg = msg;
          JSONArray errorarr = null;
          JSONArray errormap = null;
          HashMap<String, List<JSONObject>> hm = new HashMap<String, List<JSONObject>>();
          HashMap<String, List<JSONObject>> hm1 = new HashMap<String, List<JSONObject>>();
          if (errormsg.has("WrongMap") && errormsg.getJSONArray("WrongMap") != null) {
            errorarr = errormsg.getJSONArray("WrongMap");
            if (errorarr.length() > 0) {
              for (int p = 0; p < errorarr.length(); p++) {
                JSONObject objproj = errorarr.getJSONObject(p);

                if (hm.containsKey(
                    objproj.getString("parentid") + "-" + objproj.getString("parentKey"))) {
                  if (!hm.containsValue(objproj)) {
                    hm.get(objproj.getString("parentid") + "-" + objproj.getString("parentKey"))
                        .add(objproj);
                  }
                } else {
                  List<JSONObject> list = new ArrayList<JSONObject>();
                  list.add(objproj);
                  hm.put(objproj.getString("parentid") + "-" + objproj.getString("parentKey"),
                      list);
                }
              }

              for (Map.Entry<String, List<JSONObject>> entry : hm.entrySet()) {
                String[] parentId = entry.getKey().split("-");
                for (JSONObject object : entry.getValue()) {
                  errorwrongmap = errorwrongmap + "," + object.getString("code");
                }
                errorwrongmap = "Accounts " + errorwrongmap.replaceFirst(",", "")
                    + " under the group of " + parentId[1] + " is wrongly mapped with project.";
              }
            }
          }
          if (errormsg.has("NotMapList") && errormsg.getJSONArray("NotMapList") != null) {
            errormap = errormsg.getJSONArray("NotMapList");
            if (errormap.length() > 0) {
              for (int p = 0; p < errormap.length(); p++) {
                JSONObject objproj = errormap.getJSONObject(p);
                if (hm1.containsKey(
                    objproj.getString("parentid") + "-" + objproj.getString("parentKey"))) {
                  if (!hm.containsValue(objproj.get("code")))
                    hm1.get(objproj.getString("parentid") + "-" + objproj.getString("parentKey"))
                        .add(objproj);
                } else {
                  List<JSONObject> list = new ArrayList<JSONObject>();
                  list.add(objproj);
                  hm1.put(objproj.getString("parentid") + "-" + objproj.getString("parentKey"),
                      list);
                }
              }
              for (Map.Entry<String, List<JSONObject>> entry : hm1.entrySet()) {
                String[] parentId = entry.getKey().split("-");
                for (JSONObject object : entry.getValue()) {
                  errornotmap = errornotmap + "," + object.getString("code");
                }
                errornotmap = " </br> Accounts " + errornotmap.replaceFirst(",", "")
                    + " under the group of " + parentId[1] + " is not mapped with project.";
              }
            }
          }
          JSONObject errortext = new JSONObject();
          errortext.put("severity", "error");
          errortext.put("text", errorwrongmap + " " + errornotmap);
          jsonResponse.put("message", errortext);
          return jsonResponse;
        }
      } else {
        JSONObject errormsg = new JSONObject();
        errormsg.put("severity", "error");
        errormsg.put("text", OBMessageUtils.messageBD("Efin_BudgLine_NotDefAcct"));
        jsonResponse.put("message", errormsg);
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
