package sa.elm.ob.finance.ad_process.BudgetAddition;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DbUtility;

import sa.elm.ob.finance.EfinBudgetAdd;
import sa.elm.ob.finance.EfinBudgetAddLines;
import sa.elm.ob.utility.EutNextRoleLine;

/* Rework the Budget Addition*/
public class BudgetAdditionRework implements Process {

  private static final Logger log = Logger.getLogger(BudgetAdditionRework.class);
  private final OBError obError = new OBError();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    String BudgetAddId = (String) bundle.getParams().get("Efin_Budgetadd_ID");

    EfinBudgetAdd budgetAdd = OBDal.getInstance().get(EfinBudgetAdd.class, BudgetAddId);
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = budgetAdd.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    String appstatus = "";

    log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);

    boolean errorFlag = true;
    boolean allowUpdate = false;
    String errorMsg = "";
    int count = 0;

    // check current role is present in document rule or not
    if (budgetAdd.getStatus().equals("RW")) {
      if (budgetAdd.getNextRole() != null) {
        java.util.List<EutNextRoleLine> li = budgetAdd.getNextRole().getEutNextRoleLineList();
        for (int i = 0; i < li.size(); i++) {
          String role = li.get(i).getRole().getId();
          if (roleId.equals(role)) {
            allowUpdate = true;
          }
        }
      }
      if (!allowUpdate) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_AlreadyPreocessed_Approve@");
        bundle.setResult(result);
        return;
      }

    }
    // After Revoked by submiter if approver is try to Rework the same record then throw error
    // throw the error message while 2nd user try to Rework while 1st user already Approved that
    // record with same role
    if ((budgetAdd.getStatus().equals("APP")) || (budgetAdd.getStatus().equals("O"))) {
      OBDal.getInstance().rollbackAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "error",
          "@Efin_AlreadyPreocessed_Approve@");
      bundle.setResult(result);
      return;
    }

    try {
      if (errorFlag) {
        try {
          OBContext.setAdminMode(true);
          OBQuery<EfinBudgetAddLines> lines = OBDal.getInstance()
              .createQuery(EfinBudgetAddLines.class, "efinBudgetadd.id ='" + BudgetAddId + "'");
          count = lines.list().size();

          if (count > 0) {

            EfinBudgetAdd header = OBDal.getInstance().get(EfinBudgetAdd.class, BudgetAddId);

            header.setUpdated(new java.util.Date());
            header.setUpdatedBy(OBContext.getOBContext().getUser());
            header.setStatus("RW");
            header.setAction("CO");
            header.setNextRole(null);
            header.setRevoke(false);
            log.debug("header:" + header.toString());
            OBDal.getInstance().save(header);

            if (!StringUtils.isEmpty(header.getId())) {
              appstatus = "REW";
              BudgetAdditionProcess.insertBudgAddHistory(OBDal.getInstance().getConnection(),
                  clientId, orgId, roleId, userId, header, comments, appstatus, null);
            }
            if (count > 0 && !StringUtils.isEmpty(header.getId())) {
              OBError result = OBErrorBuilder.buildMessage(null, "success",
                  "@Efin_BudgAdd_Rework@");
              bundle.setResult(result);
              return;
            }
          }

        } catch (Exception e) {
          Throwable t = DbUtility.getUnderlyingSQLException(e);
          final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
              vars.getLanguage(), t.getMessage());
          bundle.setResult(error);

          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(errorMsg);
          bundle.setResult(obError);

          OBDal.getInstance().rollbackAndClose();

        }
      }
      bundle.setResult(obError);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      bundle.setResult(obError);
      log.error("exception :", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
