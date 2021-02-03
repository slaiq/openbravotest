package sa.elm.ob.finance.ad_reports.IntegratedCostBudgetInquiry.dao;

import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;

import sa.elm.ob.finance.ad_reports.IntegratedCostBudgetInquiry.vo.IntegratedCostBudgetInquiryVO;

/**
 * 
 * @author Priyanka Ranjan on 22/04/2019
 * 
 */
// Interface for Integrated Cost Budget Inquiry Report

public interface IntegratedCostBudgetInquiryDAO {

  /**
   * 
   * @param OrgId
   * @param ClientId
   * @return
   * @throws Exception
   */
  JSONObject getOrganization(String OrgId, String ClientId, String searchTerm, int pagelimit,
      int page) throws Exception;

  /**
   * 
   * @param OrgId
   * @param ClientId
   * @param schemaId
   * @return
   * @throws Exception
   */
  List<IntegratedCostBudgetInquiryVO> getAcctSchema(String OrgId, String ClientId, String schemaId)
      throws Exception;

  /**
   * get budget type
   * 
   * @param OrgId
   * @param ClientId
   * @param RoleId
   * @return budget type id and name
   * @throws Exception
   */
  List<IntegratedCostBudgetInquiryVO> getBudgetType(String OrgId, String ClientId, String RoleId)
      throws Exception;

  /**
   * 
   * @param vars
   * @param BudgetTypeId
   * @param ClientId
   * @param OrgId
   * @param parentAccountId
   * @param deptId
   * @param subAccountId
   * @return
   * @throws Exception
   */
  JSONObject selectLines(VariablesSecureApp vars, String BudgetTypeId, String ClientId,
      String OrgId, String parentAccountId, String deptId, String subAccountId) throws Exception;

  /**
   * get client information
   * 
   * @param clientId
   * @return client id and name
   * @throws Exception
   */
  public List<IntegratedCostBudgetInquiryVO> getClientInfo(String clientId) throws Exception;

  /**
   * get parent account - issummary ='Y'
   * 
   * @param clientId
   * @return parent account
   * @throws Exception
   */
  JSONObject getParentAccount(String clientId, String searchTerm, int pagelimit, int page)
      throws Exception;

  /**
   * get department
   * 
   * @param clientId
   * @param orgId
   * @return dept
   * @throws Exception
   */
  JSONObject getDepartment(String clientId, String searchTerm, int pagelimit, int page)
      throws Exception;

  /**
   * get sub account (project)
   * 
   * @param clientId
   * @return subaccount
   * @throws Exception
   */
  JSONObject getSubAccount(String clientId, String searchTerm, int pagelimit, int page,
      String accountId) throws Exception;

  /**
   * 
   * @param parameter
   * @return SubAccount Id Associated with Account
   * @throws Exception
   */
  JSONObject getSubAccountAgainstAccount(String account_id) throws Exception;

}
