package sa.elm.ob.finance.ad_process.BudgetPreparation;

import java.sql.Connection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinBudgPrepLines;
import sa.elm.ob.finance.EfinBudgetPreparation;
import sa.elm.ob.finance.EfinBudgetPreparationHistory;
import sa.elm.ob.finance.dao.AdvPaymentMngtDao;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.properties.Resource;

public class BudgetPreparationRework implements Process {
  private static final Logger log = Logger.getLogger(BudgetPreparationRework.class);
  private final OBError obError = new OBError();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    final String budgetpreparationId = (String) bundle.getParams().get("Efin_Budget_Preparation_ID")
        .toString();
    EfinBudgetPreparation budgetpreparation = OBDal.getInstance().get(EfinBudgetPreparation.class,
        budgetpreparationId);
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = budgetpreparation.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    EfinBudgetPreparation headerId = null;

    log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);

    boolean errorFlag = true;
    boolean allowUpdate = false;
    String errorMsg = "";
    int count = 0;

    log.debug("budgetpreparationId:" + budgetpreparationId);

    if (budgetpreparation.getAlertStatus().equals("APP")) {
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

          OBQuery<EfinBudgPrepLines> lines = OBDal.getInstance().createQuery(
              EfinBudgPrepLines.class, "efinBudgetPreparation.id = :budgetpreparationID ");
          lines.setNamedParameter("budgetpreparationID", budgetpreparationId);
          List<EfinBudgPrepLines> linesList = lines.list();
          count = linesList.size();

          if (count > 0) {
            EfinBudgetPreparation header = OBDal.getInstance().get(EfinBudgetPreparation.class,
                budgetpreparationId);

            if (header.getNextRole() != null) {
              java.util.List<EutNextRoleLine> li = header.getNextRole().getEutNextRoleLineList();
              for (int i = 0; i < li.size(); i++) {
                String role = li.get(i).getRole().getId();
                if (roleId.equals(role)) {
                  allowUpdate = true;
                }
              }
            }

            if (allowUpdate) {

              header.setUpdated(new java.util.Date());
              header.setUpdatedBy(OBContext.getOBContext().getUser());
              header.setAlertStatus("RW");
              header.setAction("CO");
              header.setNextRole(null);
              header.setBudgetprepareRevoke(false);
              log.debug("header:" + header.toString());
              OBDal.getInstance().save(header);
              OBDal.getInstance().flush();
              DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                  Resource.BUDGET_PREPARATION_RULE);
              headerId = header;
              log.debug("headerId:" + headerId.getId());
              if (!StringUtils.isEmpty(headerId.getId())) {
                count = insertBudgetPreparationApprover(OBDal.getInstance().getConnection(),
                    clientId, orgId, roleId, userId, headerId, comments, "REW");
              }
              if (count > 0 && !StringUtils.isEmpty(headerId.getId())) {
                obError.setType("Success");
                obError.setTitle("Success");
                obError.setMessage(OBMessageUtils.messageBD("EFin_BudgetPre_Rework"));
              }
              OBDal.getInstance().flush();
              OBDal.getInstance().commitAndClose();
            } else {
              errorFlag = false;
              errorMsg = OBMessageUtils.messageBD("Efin_AlreadyPreocessed_Approve");
              throw new OBException(errorMsg);
            }
          }

        } catch (Exception e) {
          log.error("exception :", e);

          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(errorMsg);
          bundle.setResult(obError);

          OBDal.getInstance().rollbackAndClose();

        }
      } else if (errorFlag == false) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(errorMsg);
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

  /**
   * This method is used to insert Budget Preparation Approver
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param headerId
   * @param comments
   * @param appstatus
   * @return
   */
  public static int insertBudgetPreparationApprover(Connection con, String clientId, String orgId,
      String roleId, String userId, EfinBudgetPreparation headerId, String comments,
      String appstatus) {
    AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
    String histId = null;
    try {
      OBContext.setAdminMode(true);

      EfinBudgetPreparationHistory hist = OBProvider.getInstance()
          .get(EfinBudgetPreparationHistory.class);
      hist.setClient(dao.getObject(Client.class, clientId));
      hist.setOrganization(dao.getObject(Organization.class, orgId));
      hist.setActive(true);
      hist.setCreatedBy(dao.getObject(User.class, userId));
      hist.setCreationDate(new java.util.Date());
      hist.setCreatedBy(dao.getObject(User.class, userId));
      hist.setUpdated(new java.util.Date());
      hist.setEfinBudgetPreparation(
          OBDal.getInstance().get(EfinBudgetPreparation.class, headerId.getId()));
      hist.setBudgetprepAction(appstatus);
      hist.setApprovedDate(new java.util.Date());
      hist.setComments(comments);

      OBDal.getInstance().save(hist);
      histId = hist.getId();
      if (histId != null)
        return 1;
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      log.error("Exception in insertBudgetApprover: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return 1;
  }
}
