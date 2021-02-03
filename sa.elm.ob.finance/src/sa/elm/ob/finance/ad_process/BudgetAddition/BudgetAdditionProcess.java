package sa.elm.ob.finance.ad_process.BudgetAddition;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EFINBudgetLines;
import sa.elm.ob.finance.EfinBudgetAdd;
import sa.elm.ob.finance.EfinBudgetAddAppHist;
import sa.elm.ob.finance.EfinBudgetAddLines;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.properties.Resource;

public class BudgetAdditionProcess extends DalBaseProcess {

  /**
   * Budget Addition submit and Approve Process and track the detail on History Window
   */
  private static final Logger log = LoggerFactory.getLogger(BudgetAdditionProcess.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    log.debug("entering into BudgetAdditionProcess");
    try {
      OBContext.setAdminMode();
      String BudgetAddId = (String) bundle.getParams().get("Efin_Budgetadd_ID");

      /* Get the status&Action */
      EfinBudgetAdd budgetAdd = OBDal.getInstance().get(EfinBudgetAdd.class, BudgetAddId);
      log.debug("DocStatus:" + budgetAdd.getStatus());
      log.debug("DocAction:" + budgetAdd.getAction());
      String DocStatus = budgetAdd.getStatus();
      String DocAction = budgetAdd.getAction();

      /* Assign the Bundle Parameters */
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = budgetAdd.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      String comments = (String) bundle.getParams().get("comments").toString();
      String appstatus = "", header = null;
      boolean errorFlag = false;
      BigDecimal Zero = new BigDecimal(0.00);

      boolean allowUpdate = false;

      // check current role is present in document rule or not
      if (!budgetAdd.getStatus().equals("O") && !budgetAdd.getStatus().equals("RW")) {
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
      // throw the error message while 2nd user try to approve while 1st user already reworked that
      // record with same role
      if ((!vars.getUser().equals(budgetAdd.getCreatedBy().getId()))
          && budgetAdd.getStatus().equals("RW")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_AlreadyPreocessed_Approve@");
        bundle.setResult(result);
        return;
      }

      // After Revoked by submiter if approver is try to Approve the same record then throw error
      if ((!vars.getUser().equals(budgetAdd.getCreatedBy().getId()))
          && budgetAdd.getStatus().equals("O")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_AlreadyPreocessed_Approve@");
        bundle.setResult(result);
        return;
      }

      OBQuery<EfinBudgetAddLines> lines = OBDal.getInstance().createQuery(EfinBudgetAddLines.class,
          " as e where e.efinBudgetadd.id = :BudgetaddID ");
      lines.setNamedParameter("BudgetaddID", BudgetAddId);
      if (lines.list().size() == 0) {
        errorFlag = false;
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_AddLines_Submit@");
        bundle.setResult(result);
        return;
      }

      if (budgetAdd.getTotalBudgetValue().compareTo(Zero) == 0) {
        errorFlag = false;
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Budgvalue_Zero@");
        bundle.setResult(result);
        return;
      }

      /* Budget Addition Submit Process */
      if ((DocStatus.equals("O") || DocStatus.equals("RW")) && DocAction.equals("CO")) {

        /* check already unique coded exist or not in Budget */
        errorFlag = uniquecodeExistorNot(OBDal.getInstance().getConnection(), clientId, orgId,
            roleId, userId, budgetAdd);

        /*
         * check if budget is Funds or Cost. If funds then check added uniquecode is exist in Cost
         * budget or not.
         */
        if (!errorFlag) {
          errorFlag = chkUCexistCB(OBDal.getInstance().getConnection(), clientId, orgId, roleId,
              userId, budgetAdd);
          if (!errorFlag) {
            appstatus = "SUB";

            /* check approver while submit the budget addition */
            header = checkApprover(OBDal.getInstance().getConnection(), clientId, orgId, roleId,
                userId, BudgetAddId, comments, appstatus);
            if (header != null) {
              OBError result = OBErrorBuilder.buildMessage(null, "success",
                  "@Efin_BudgAdd_Success@");
              bundle.setResult(result);
              return;
            }
          }
          /* If exist then throw the errror */
          else {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "Process Failed.Please Check Failure Reason");
            bundle.setResult(result);
            return;
          }
        }

        /* If exist then throw the error */
        else {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_BudgetAddLine_UCExist@");
          bundle.setResult(result);
          return;
        }
      }
      /* Budget Addition Approve Process */
      else if (DocAction.equals("AP") && DocStatus.equals("IA")) {

        /* check already unique coded exist or not in Budget */
        errorFlag = uniquecodeExistorNot(OBDal.getInstance().getConnection(), clientId, orgId,
            roleId, userId, budgetAdd);

        // If not exist do the further Process
        if (!errorFlag) {
          appstatus = "APP";

          // check approver while approve the budget addition
          header = checkApprover(OBDal.getInstance().getConnection(), clientId, orgId, roleId,
              userId, BudgetAddId, comments, appstatus);
          if (header != null) {
            OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_BudgAdd_Approve@");
            bundle.setResult(result);
            return;
          }
        } else {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_BudgetAddLine_UCExist@");
          bundle.setResult(result);
          return;
        }

      }

    } catch (Exception e) {
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), t.getMessage());
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method is used to check already same line are exists or not in Budget Lines
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param budgetAdd
   * @return
   */
  public static boolean uniquecodeExistorNot(Connection con, String clientId, String orgId,
      String roleId, String userId, EfinBudgetAdd budgetAdd) {
    boolean errorFlag = false;
    try {
      OBContext.setAdminMode(true);
      OBQuery<EfinBudgetAddLines> budgetaddlinechk = OBDal.getInstance().createQuery(
          EfinBudgetAddLines.class, "efinBudgetadd.id= :BudgetaddID and client.id = :clientID");
      budgetaddlinechk.setNamedParameter("BudgetaddID", budgetAdd.getId());
      budgetaddlinechk.setNamedParameter("clientID", budgetAdd.getClient().getId());
      List<EfinBudgetAddLines> budgetaddlinechkList = budgetaddlinechk.list();
      if (budgetaddlinechkList.size() > 0) {
        for (int i = 0; i < budgetaddlinechkList.size(); i++) {
          EfinBudgetAddLines line = budgetaddlinechkList.get(i);
          OBQuery<EFINBudgetLines> budgetchk = OBDal.getInstance()
              .createQuery(EFINBudgetLines.class, "id='" + line.getId() + "' and client.id = '"
                  + line.getClient().getId() + "' and uniquecode='" + line.getUniqueCode() + "'");
          if (budgetchk.list().size() > 0) {
            errorFlag = true;
            return errorFlag;
          }
        }
      }

    } catch (Exception e) {
      log.error("Exception in insertManEncumHistory: ", e);
      OBDal.getInstance().rollbackAndClose();
      errorFlag = true;
      return errorFlag;
    } finally {
      OBContext.restorePreviousMode();
    }
    return errorFlag;
  }

  /**
   * check Unique Code is exist in Cost Budget or not
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param budgetAdd
   * @return
   */
  @SuppressWarnings("resource")
  public static boolean chkUCexistCB(Connection con, String clientId, String orgId, String roleId,
      String userId, EfinBudgetAdd budgetAdd) {
    boolean errorFlag = false;
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = "";
    try {
      OBContext.setAdminMode(true);
      // Get the Budget Addition Lines Detail
      OBQuery<EfinBudgetAddLines> budaddlinechk = OBDal.getInstance()
          .createQuery(EfinBudgetAddLines.class, "efinBudgetadd.id='" + budgetAdd.getId()
              + "' and client.id = '" + budgetAdd.getClient().getId() + "'");
      if (budaddlinechk.list().size() > 0) {
        for (int i = 0; i < budaddlinechk.list().size(); i++) {
          // check choosen Budget is Funds Budget or not.
          OBQuery<EFINBudget> fundsbudget = OBDal.getInstance().createQuery(EFINBudget.class,
              "id='" + budgetAdd.getBudget().getId() + "' and client.id = '"
                  + budgetAdd.getClient().getId() + "'");
          log.debug("fundsbudget:" + fundsbudget.list().size());
          if (fundsbudget.list().size() > 0) {
            EFINBudget fundsbug = fundsbudget.list().get(0);
            log.debug("isEfinIscarryforward:" + fundsbug.getSalesCampaign().isEfinIscarryforward());

            // if its funds budget check added uniquecode is exist in cost budget or not.
            if (!fundsbug.getSalesCampaign().isEfinIscarryforward()) {

              // get the cost budget detail.
              OBQuery<EFINBudget> costbudget = OBDal.getInstance().createQuery(EFINBudget.class,
                  " year.id='" + fundsbug.getYear().getId() + "' and accountElement.id='"
                      + fundsbug.getAccountElement().getId() + "' and client.id = '"
                      + budgetAdd.getClient().getId() + "' and id not in ('" + fundsbug.getId()
                      + "')");
              if (costbudget.list().size() > 0) {
                EFINBudget costbug = costbudget.list().get(0);
                query = " select funds.efin_budgetaddlines_id from efin_budgetaddlines funds "
                    + " where (funds.ad_org_id||funds.c_salesregion_id||funds.user2_id||funds.user1_id||funds.c_activity_id||funds.c_elementvalue_id||funds.c_project_id)  not in ( select (cos.ad_org_id||cos.c_salesregion_id||cos.user2_id||cos.user1_id||cos.c_activity_id||cos.c_elementvalue_id||cos.c_project_id ) from efin_budgetlines cos where cos.efin_budget_id ='"
                    + costbug.getId() + "' " + " ) and funds.efin_budgetadd_id ='"
                    + budgetAdd.getId() + "' ";
                log.debug("Query:" + query.toString());
                ps = con.prepareStatement(query);
                rs = ps.executeQuery();
                while (rs.next()) {
                  EfinBudgetAddLines FundsBudgetAddLines = OBDal.getInstance()
                      .get(EfinBudgetAddLines.class, rs.getString("efin_budgetaddlines_id"));
                  FundsBudgetAddLines.setCheckingStaus("FL");
                  FundsBudgetAddLines.setFailureReason("Combination doesn't exists in '"
                      + costbug.getYear().getFiscalYear() + "'' cost budget");
                  errorFlag = true;
                }

                query = " select funds.efin_budgetaddlines_id , cos.current_budget from   (select funds.efin_budgetaddlines_id ,funds.amount ,funds.ad_org_id,funds.c_salesregion_id,funds.user2_id,funds.user1_id,funds.c_activity_id,funds.c_elementvalue_id,"
                    + "  funds.c_project_id from efin_budgetaddlines  funds where funds.efin_budgetadd_id = ?) funds   join (select cos .efin_budgetlines_id ,cos.ad_org_id,cos.c_salesregion_id,cos.user2_id,cos.user1_id,cos.c_activity_id,cos.c_elementvalue_id,cos.c_project_id,cos.current_budget  "
                    + " from efin_budgetlines cos    where cos.efin_budget_id = ?) cos    on (funds.ad_org_id||funds.c_salesregion_id||funds.user2_id||funds.user1_id||funds.c_activity_id||funds.c_elementvalue_id||  funds.c_project_id )= (cos.ad_org_id||cos.c_salesregion_id||cos.user2_id||cos.user1_id|| "
                    + "  cos.c_activity_id||cos.c_elementvalue_id||cos.c_project_id ) where  funds.amount > cos.current_budget ";
                ps = con.prepareStatement(query);
                ps.setString(1, budgetAdd.getId());
                ps.setString(2, costbug.getId());
                log.debug("Query:" + query.toString());

                rs = ps.executeQuery();
                while (rs.next()) {
                  EfinBudgetAddLines FundsBudgetAddLines = OBDal.getInstance()
                      .get(EfinBudgetAddLines.class, rs.getString("efin_budgetaddlines_id"));
                  FundsBudgetAddLines.setCheckingStaus("FL");
                  String status = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_FundsCost");
                  FundsBudgetAddLines
                      .setFailureReason(status.replace("@", rs.getString("current_budget")));
                  errorFlag = true;
                }
              }
            } else
              errorFlag = false;
            return errorFlag;
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in insertManEncumHistory: ", e);
      OBDal.getInstance().rollbackAndClose();
      errorFlag = true;
      return errorFlag;
    } finally {
      // close connection
      try {
        if (rs != null) {
          rs.close();
        }
        if (ps != null) {
          ps.close();
        }
      } catch (Exception e) {
        log.error("Exception in closing connection : " + e);
      }
      OBContext.restorePreviousMode();
    }
    return errorFlag;
  }

  /**
   * This method is used to Check Approver while Submitting or Approving the Budget Addition
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param budgetAdddId
   * @param comments
   * @param appstatus
   * @return
   */
  public static String checkApprover(Connection con, String clientId, String orgId, String roleId,
      String userId, String budgetAdddId, String comments, String appstatus) {
    String headerId = null;
    long lineNo = 0;
    String pendingapproval = "";
    PreparedStatement ps = null;
    ResultSet rs = null;

    try {
      OBContext.setAdminMode(true);
      EfinBudgetAdd header = OBDal.getInstance().get(EfinBudgetAdd.class, budgetAdddId);

      NextRoleByRuleVO nextApproval = NextRoleByRule.getNextRole(con, clientId, orgId, roleId,
          userId, Resource.BUDGET_ENTRY_RULE, 0.00);
      EutNextRole nextRole = null;
      if (nextApproval != null && nextApproval.hasApproval()) {
        nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
        header.setUpdated(new java.util.Date());
        header.setUpdatedBy(OBContext.getOBContext().getUser());
        if ((header.getStatus().equals("RW") || header.getStatus().equals("O"))
            && header.getAction().equals("CO")) {
          header.setRevoke(true);
        } else
          header.setRevoke(false);
        header.setStatus("IA");
        header.setNextRole(nextRole);
        header.setAction("AP");
        pendingapproval = nextApproval.getStatus();
      } else {
        header.setUpdated(new java.util.Date());
        header.setUpdatedBy(OBContext.getOBContext().getUser());
        header.setStatus("APP");
        header.setNextRole(null);
        header.setAction("PD");
        header.setRevoke(false);
      }
      OBDal.getInstance().save(header);
      headerId = header.getId();

      // if approved then we need to insert the Records into Original Budget
      if (header.getStatus().equals("APP")) {

        /* Insert Budget Lines */
        ps = con.prepareStatement(
            "SELECT COALESCE(MAX(LINE),0)+10 AS line FROM EFIN_BUDGETLINES WHERE EFIN_BUDGET_ID='"
                + header.getBudget().getId() + "'");
        rs = ps.executeQuery();
        if (rs.next()) {
          lineNo = rs.getLong("line");
        }
        OBQuery<EfinBudgetAddLines> lines = OBDal.getInstance()
            .createQuery(EfinBudgetAddLines.class, "efinBudgetadd.id='" + headerId + "'");
        for (int i = 0; i < lines.list().size(); i++) {
          EfinBudgetAddLines addline = lines.list().get(i);
          EFINBudgetLines line = OBProvider.getInstance().get(EFINBudgetLines.class);
          line.setClient(addline.getClient());
          line.setOrganization(addline.getOrganization());
          line.setActive(true);
          line.setCreatedBy(addline.getCreatedBy());
          line.setCreationDate(new java.util.Date());
          line.setUpdatedBy(addline.getUpdatedBy());
          line.setUpdated(new java.util.Date());
          line.setEfinBudget(addline.getEfinBudgetadd().getBudget());
          line.setUniquecode(addline.getUniqueCode());
          line.setAmount(addline.getAmount());
          line.setCurrentBudget(addline.getAmount());
          line.setFundsAvailable(addline.getAmount());
          line.setOrganization(addline.getOrganization());
          line.setSalesRegion(addline.getSalesRegion());
          line.setAccountElement(addline.getAccountElement());
          line.setSalesCampaign(addline.getSalesCampaign());
          line.setActivity(addline.getActivity());
          line.setStDimension(addline.getStDimension());
          line.setNdDimension(addline.getNdDimension());
          line.setProject(addline.getProject());
          line.setLineNo(lineNo);
          line.setEfinBudgetadd(header);
          line.setDescription(addline.getDescription());
          lineNo += 10;
          log.debug("line:" + line.toString());
          OBDal.getInstance().save(line);
        }

      }
      if (!StringUtils.isEmpty(headerId)) {
        /* Insert Budget Addition history */
        insertBudgAddHistory(OBDal.getInstance().getConnection(), clientId, orgId, roleId, userId,
            header, comments, appstatus, pendingapproval);
        updateStatus(OBDal.getInstance().getConnection(), clientId, orgId, roleId, userId, header);
      }

    } catch (Exception e) {
      log.error("Exception in insertManEncumHistory: ", e);
      OBDal.getInstance().rollbackAndClose();
      return headerId;
    }

    finally {
      try {
        // close connection
        if (rs != null) {
          rs.close();
        }
        if (ps != null) {
          ps.close();
        }
      } catch (Exception e) {
        log.error("Exception in closing conection :" + e);
      }
      OBContext.restorePreviousMode();
    }
    return headerId;
  }

  /**
   * This method Insert Budget Addition history
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param header
   * @return
   */
  public static int updateStatus(Connection con, String clientId, String orgId, String roleId,
      String userId, EfinBudgetAdd header) {
    try {
      OBContext.setAdminMode(true);

      OBQuery<EfinBudgetAddLines> lines = OBDal.getInstance().createQuery(EfinBudgetAddLines.class,
          "efinBudgetadd.id='" + header.getId() + "'");
      for (int i = 0; i < lines.list().size(); i++) {
        EfinBudgetAddLines line = lines.list().get(i);
        EfinBudgetAddLines addline = OBDal.getInstance().get(EfinBudgetAddLines.class,
            line.getId());

        addline.setUpdatedBy(header.getUpdatedBy());
        addline.setUpdated(new java.util.Date());
        addline.setCheckingStaus("SCS");
        addline.setFailureReason("");
        OBDal.getInstance().save(addline);
      }

    } catch (Exception e) {
      log.error("Exception in insertManEncumHistory: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return 1;
  }

  /**
   * This method is used to insert budget Addition history
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param header
   * @param comments
   * @param appstatus
   * @param pendingapproval
   * @return
   */
  public static int insertBudgAddHistory(Connection con, String clientId, String orgId,
      String roleId, String userId, EfinBudgetAdd header, String comments, String appstatus,
      String pendingapproval) {
    String histId = null;
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    vars = new VariablesSecureApp(request);
    try {
      OBContext.setAdminMode(true);

      EfinBudgetAddAppHist hist = OBProvider.getInstance().get(EfinBudgetAddAppHist.class);
      hist.setClient(header.getClient());
      hist.setOrganization(header.getOrganization());
      hist.setActive(true);
      // set CreatedBy with current user
      hist.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
      hist.setCreationDate(new java.util.Date());
      // set UpdatedBy with current user
      hist.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
      hist.setUpdated(new java.util.Date());
      hist.setEfinBudgetadd(header);
      hist.setBudgetaddAction(appstatus);
      hist.setApprovedDate(new java.util.Date());
      hist.setComments(comments);
      if (!appstatus.equals("REV")) {
        hist.setPendingOn(pendingapproval);
      }
      log.debug("hist:" + hist.toString());
      OBDal.getInstance().save(hist);
      histId = hist.getId();
      if (histId != null)
        return 1;
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      log.error("Exception in insertManEncumHistory: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return 1;
  }
}
