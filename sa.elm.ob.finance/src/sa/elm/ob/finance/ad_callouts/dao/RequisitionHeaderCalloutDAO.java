package sa.elm.ob.finance.ad_callouts.dao;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinEncControl;

/**
 * @author qualian on 08/08/2017
 */

public class RequisitionHeaderCalloutDAO {
  /**
   * This Access Layer class is responsible to do database operation in Requisition Process Class
   */
  VariablesSecureApp vars = null;

  private final static Logger log = LoggerFactory.getLogger(RequisitionHeaderCalloutDAO.class);

  /**
   * Check Manual Encumbrance has list size 1 and if yes set it as default id
   * 
   * @param budgetIntId
   * @return encumId in String
   */
  public static String getManualEncumId(String budgetIntId) {
    String encumId = null;
    String query = " as e where (e.documentStatus='CO' and e.encumType='PRE' and e.encumMethod='M' "
        + " and e.encumStage ='PRE' and e.amount <> e.appliedAmount and e.budgetInitialization.id=? )";
    List<EfinBudgetManencum> budgetManencumList = null;
    List<Object> parametersList = new ArrayList<Object>();
    parametersList.add(budgetIntId);
    try {
      OBContext.setAdminMode();

      OBQuery<EfinBudgetManencum> budgetEncum = OBDal.getInstance()
          .createQuery(EfinBudgetManencum.class, query, parametersList);
      budgetManencumList = budgetEncum.list();
      log.debug("menum size>" + budgetManencumList.size());
      if (budgetManencumList.size() > 0 && budgetManencumList.size() == 1) {
        EfinBudgetManencum mencum = budgetManencumList.get(0);
        encumId = mencum.getId();
      }
    } catch (OBException e) {
      log.error("Exception while getManualEncumId:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return encumId;
  }

  /**
   * Check Manual Encumbrance has list size 1 and if yes set it as default id;
   * 
   * @param budgetIntId
   * @return encumId in String
   */
  public static String getUniqueCode(String inpemEfinBudgetManencumId, String clientId,
      String roleId) {
    String uniqueCode = null;
    String whereClause = " as e where e.id in (select e.accountingCombination.id from Efin_Budget_Manencumlines as e  where e.manualEncumbrance.id = ?) "
        + " and e.salesRegion.id not in (select e.budgetcontrolCostcenter.id "
        + "from efin_budget_ctrl_param as e where e.client.id=?) "
        + "and e.salesRegion.id not in (select e.budgetcontrolunit.id  from efin_budget_ctrl_param as e where e.client.id=?) "
        + "and e.salesRegion.id in (select e.department.id from Efin_Security_RulesDepartment e join e.rule ru where ru.efinProcessbutton = 'Y' "
        + "and ru.id = ( select r.efinSecurityRules.id from ADRole r where r.id=?)  and  e.client.id=?)  "
        + "and e.salesCampaign.id in (select e.budgetType.id from Efin_Security_Rules_Budtype e join e.rule ru where ru.efinProcessbutton = 'Y' "
        + "and ru.id = ( select r.efinSecurityRules.id from ADRole r where r.id=?)  and  e.client.id=?) "
        + "and e.account.id in ( select e.elementvalue.id from Efin_Security_Rules_Act e join e.efinSecurityRules ru  where ru.efinProcessbutton = 'Y' "
        + "and ru.id = ( select r.efinSecurityRules.id from ADRole r where r.id=?)  and e.client.id=?)  "
        + "and e.account.elementLevel = 'S' and e.project.id in (select e.project.id from Efin_Security_RulesProject e join e.rule ru  "
        + "where ru.efinProcessbutton = 'Y'    and ru.id = ( select r.efinSecurityRules.id from ADRole r where r.id=?)  "
        + "and  e.client.id=?) "
        + "and e.activity.id in (select e.functionalClassification.id from Efin_Security_RulesActivity e join e.rule ru   "
        + "where ru.efinProcessbutton = 'Y'    and ru.id = ( select r.efinSecurityRules.id from ADRole r where r.id=?)  and  e.client.id=?) "
        + "and e.stDimension.id in (select e.future1.id from Efin_Security_Rules_Fut1 e join e.rule ru   where ru.efinProcessbutton = 'Y' "
        + "and ru.id = ( select r.efinSecurityRules.id from ADRole r where r.id=?)  and  e.client.id=?) "
        + "and e.ndDimension.id in (select e.future2.id from Efin_Security_Rules_Fut2 e join e.rule ru   where ru.efinProcessbutton = 'Y' "
        + "and ru.id = ( select r.efinSecurityRules.id from ADRole r where r.id=?)  and  e.client.id=?)  "
        + "and e.businessPartner.id in ( select e.businessPartner.id from Efin_Security_Rules_Bp e join e.rule ru where ru.efinProcessbutton = 'Y' "
        + "and ru.id = ( select r.efinSecurityRules.id from ADRole r where r.id=? ) and e.client.id=?) ";

    List<AccountingCombination> uniqueCodeList = null;
    List<Object> parametersList = new ArrayList<Object>();
    parametersList.add(inpemEfinBudgetManencumId);
    parametersList.add(clientId);
    parametersList.add(clientId);
    parametersList.add(roleId);
    parametersList.add(clientId);
    parametersList.add(roleId);
    parametersList.add(clientId);
    parametersList.add(roleId);
    parametersList.add(clientId);
    parametersList.add(roleId);
    parametersList.add(clientId);
    parametersList.add(roleId);
    parametersList.add(clientId);
    parametersList.add(roleId);
    parametersList.add(clientId);
    parametersList.add(roleId);
    parametersList.add(clientId);
    parametersList.add(roleId);
    parametersList.add(clientId);
    try {
      OBContext.setAdminMode();

      OBQuery<AccountingCombination> uniqueCodeQry = OBDal.getInstance()
          .createQuery(AccountingCombination.class, whereClause, parametersList);
      uniqueCodeList = uniqueCodeQry.list();
      log.debug("ucode size>" + uniqueCodeList.size());
      if (uniqueCodeList.size() > 0 && uniqueCodeList.size() == 1) {
        AccountingCombination uCode = uniqueCodeList.get(0);
        uniqueCode = uCode.getId();
      }
      log.debug("uniqueCode>" + uniqueCode);
    } catch (OBException e) {
      log.error("Exception while getUniqueCode:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return uniqueCode;
  }

  /**
   * This method is used to get encumbrance control disabled count of PR
   * 
   * @param PRTYPE
   * 
   *          return count of encumbrance control which is disabled count with type 'PRE'
   */

  public static int getEncumbranceControlCount(String type) {
    try {
      OBQuery<EfinEncControl> efincontrolList = OBDal.getInstance().createQuery(
          EfinEncControl.class, "as e where e.encumbranceType=:type and e.typeList=:prtype");
      efincontrolList.setFilterOnReadableClients(true);
      efincontrolList.setNamedParameter("type", "PRE");
      efincontrolList.setFilterOnActive(true);
      List<EfinEncControl> list = efincontrolList.list();
      if (list != null && list.size() > 0) {
        return list.size();
      } else {
        return 0;
      }
    } catch (Exception e) {
      log.debug(e.getMessage());
      return 1;
    }
  }

  /**
   * This method is used to update the issecured flag in requistion
   * 
   */
  public static boolean updateSecuredString(String type) {
    try {
      OBQuery<Preference> efincontrolList = OBDal.getInstance().createQuery(Preference.class,
          "as e where e.attribute='SECUREDUNITPRICE' and e.searchKey=:type");
      efincontrolList.setFilterOnReadableClients(true);
      efincontrolList.setNamedParameter("type", type);
      efincontrolList.setFilterOnActive(true);
      List<Preference> list = efincontrolList.list();
      if (list != null && list.size() > 0) {
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      log.debug(e.getMessage());
      return false;
    }
  }
}
