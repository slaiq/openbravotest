package sa.elm.ob.finance.ad_process.BudgetPreparation;

import javax.servlet.http.HttpServletRequest;

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
import org.openbravo.model.ad.system.Client;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EFINBudgetLines;
import sa.elm.ob.finance.EfinBudgPrepLines;
import sa.elm.ob.finance.EfinBudgetPreparation;

/**
 * @author Gopalakrishnan on 15/06/2016
 */

public class ConvertBudgetPreparationToBudget extends DalBaseProcess {

  /**
   * Converting Budget preparation to Budget Budget preparation Table(Efin_Budget_Preparation)
   */
  private static final Logger log = LoggerFactory.getLogger(ConvertBudgetPreparationToBudget.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    final String strPreparationId = (String) bundle.getParams().get("Efin_Budget_Preparation_ID")
        .toString();
    EfinBudgetPreparation objBudgetPrep = OBDal.getInstance().get(EfinBudgetPreparation.class,
        strPreparationId);
    String strAcctId = objBudgetPrep.getAccountElement().getId();
    String strBudType = objBudgetPrep.getSalesCampaign().getId();
    String strYearId = objBudgetPrep.getYear().getId();
    final String clientId = (String) bundle.getContext().getClient();
    long lineNo = 0;
    log.debug("entering into ConvertBudgetPreparationToBudget");
    try {
      /*
       * Insert the Budget Header
       */
      OBContext.setAdminMode();

      /*
       * Check if the budget present for the same year,budget type, accounting element
       */

      OBQuery<EFINBudget> budgetlist = OBDal.getInstance().createQuery(EFINBudget.class,
          " as e where e.accountElement.id= :accountElementID "
              + " and e.year.id= :yearID and e.salesCampaign.id= :salesCampaignID ");
      budgetlist.setNamedParameter("accountElementID", strAcctId);
      budgetlist.setNamedParameter("yearID", strYearId);
      budgetlist.setNamedParameter("salesCampaignID", strBudType);
      if (budgetlist.list().size() > 0) {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_BudgetPrep_Exists@");
        bundle.setResult(result);
        return;
      } else {

        if (objBudgetPrep != null) {
          objBudgetPrep.setConvertbudget(false);
          OBDal.getInstance().save(objBudgetPrep);
        }
        EFINBudget ObjBudget = OBProvider.getInstance().get(EFINBudget.class);
        ObjBudget.setClient(OBDal.getInstance().get(Client.class, clientId));
        ObjBudget.setActive(true);
        ObjBudget.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
        ObjBudget.setCreationDate(new java.util.Date());
        ObjBudget.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
        ObjBudget.setUpdated(new java.util.Date());
        ObjBudget.setOrganization(objBudgetPrep.getOrganization());
        ObjBudget.setGeneralLedger(objBudgetPrep.getGeneralLedger());
        ObjBudget.setYear(objBudgetPrep.getYear());
        ObjBudget.setBudgetname(objBudgetPrep.getBudgetname());
        ObjBudget.setSalesCampaign(objBudgetPrep.getSalesCampaign());
        ObjBudget.setAccountElement(objBudgetPrep.getAccountElement());
        ObjBudget.setFrmperiod(objBudgetPrep.getFrmperiod());
        ObjBudget.setToperiod(objBudgetPrep.getToperiod());
        ObjBudget.setBudgetControl("A");
        ObjBudget.setEfinBudgetPreparation(objBudgetPrep);
        ObjBudget.setAlertStatus("OP");
        ObjBudget.setTotalbudgetvalue(objBudgetPrep.getTotalBudgetValue());
        OBDal.getInstance().save(ObjBudget);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(ObjBudget);

        /*
         * Insert Budget Lines
         */
        lineNo = 10;
        for (EfinBudgPrepLines prepLines : objBudgetPrep.getEfinBudgPrepLinesList()) {
          EFINBudgetLines ObjBudgetLines = OBProvider.getInstance().get(EFINBudgetLines.class);
          ObjBudgetLines.setClient(OBDal.getInstance().get(Client.class, clientId));
          ObjBudgetLines.setActive(true);
          ObjBudgetLines.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
          ObjBudgetLines.setCreationDate(new java.util.Date());
          ObjBudgetLines.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
          ObjBudgetLines.setUpdated(new java.util.Date());
          ObjBudgetLines.setEfinBudget(ObjBudget);
          ObjBudgetLines.setUniquecode(prepLines.getUniqueCode());
          ObjBudgetLines.setAmount(prepLines.getAmount());
          ObjBudgetLines.setCurrentBudget(prepLines.getAmount());
          ObjBudgetLines.setFundsAvailable(prepLines.getAmount());
          ObjBudgetLines.setOrganization(prepLines.getOrganization());
          ObjBudgetLines.setSalesRegion(prepLines.getSalesRegion());
          ObjBudgetLines.setAccountElement(prepLines.getAccountElement());
          ObjBudgetLines.setSalesCampaign(prepLines.getSalesCampaign());
          ObjBudgetLines.setActivity(prepLines.getActivity());
          ObjBudgetLines.setStDimension(prepLines.getStDimension());
          ObjBudgetLines.setNdDimension(prepLines.getNdDimension());
          ObjBudgetLines.setProject(prepLines.getProject());
          ObjBudgetLines.setLineNo(lineNo);
          lineNo += 10;
          OBDal.getInstance().save(ObjBudgetLines);
          // all lines inserted
        }
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(ObjBudget);
      }

      OBDal.getInstance().commitAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_BudgetprepToBudget@");
      bundle.setResult(result);
    } catch (Exception e) {
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), t.getMessage());
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}
