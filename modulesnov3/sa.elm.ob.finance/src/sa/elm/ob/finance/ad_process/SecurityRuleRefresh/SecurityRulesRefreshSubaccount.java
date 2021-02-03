package sa.elm.ob.finance.ad_process.SecurityRuleRefresh;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.SQLQuery;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinSecurityRules;
import sa.elm.ob.finance.EfinSecurityRulesproj;
import sa.elm.ob.finance.EfinSecurityprojpickV;
import sa.elm.ob.finance.SecurityRuleProject;

/**
 * @author Rashika VS on 18/06/2018
 */
@SuppressWarnings("unused")
public class SecurityRulesRefreshSubaccount extends SecurityRulesRefresh {
  private static SecurityRulesRefreshSubaccount SecurityRulesRefreshSubaccount=null;
  public static SecurityRulesRefreshSubaccount getRefreshSubaccountInstance() {
    if(SecurityRulesRefreshSubaccount==null) {
      SecurityRulesRefreshSubaccount=new SecurityRulesRefreshSubaccount();
    }
    return SecurityRulesRefreshSubaccount;
  }
  private static final Logger log = LoggerFactory.getLogger("SecurityRulesRefreshSubaccount");
  
  private String query;

@Override
public Boolean insertProcess(EfinSecurityRules rules) {
  // TODO Auto-generated method stub
  log.debug("Success");
  Boolean subAccount = false;
  subAccount = insertSubAccount(rules);
  if(subAccount) {
    return true;
  }
  return false;
}

@SuppressWarnings("unchecked")
private Boolean insertSubAccount(EfinSecurityRules rules) {
  //Process to be done
  List<EfinSecurityRulesproj> rulesList = rules.getEfinSecurityRulesprojList();
  List<String> rulesIncludeList = new ArrayList<>();
  List<String> rulesExcludeList = new ArrayList<>();
  List<String> rulesCodeList =new ArrayList<>();
  List<String> existingSubAccountList = new ArrayList<>();
  Long fromCode,toCode;
  String clientId=rules.getClient().getId();
  for(EfinSecurityRulesproj securityRulesProj:rulesList) {
    String type = securityRulesProj.getType();
    
    //Create Exclude List 
    if(type.equals("EX")) {
      //read data from c_project value by using between functionality for fromProject and toProject
      //add exclude list
  
      query = "SELECT  c_project_id FROM c_project WHERE to_number(value) between "
          + "(SELECT to_number(value) FROM c_project WHERE c_project_id=:fromvalue) AND "
          + "(SELECT to_number(value) FROM c_project WHERE c_project_id=:tovalue) AND ad_client_id=:clientId";
      SQLQuery sqlQry=OBDal.getInstance().getSession().createSQLQuery(query.toString());
      sqlQry.setParameter("fromvalue", securityRulesProj.getFromproject().getId());
      sqlQry.setParameter("tovalue", securityRulesProj.getToproject().getId());
      sqlQry.setParameter("clientId", clientId);
      List<String> ExcludeRows = (ArrayList<String>) sqlQry.list();
      for(String strOB : ExcludeRows) {
       rulesExcludeList.add(strOB);
      }
    }
      
    
    
    //Create Include List
    else if(type.equals("IN")) {
      //read data from c_project value by using between functionality for fromProject and toProject
      //add include list
   
      query = "SELECT  c_project_id FROM c_project WHERE to_number(value) between "
          + "(SELECT to_number(value) FROM c_project WHERE c_project_id=:fromvalue) AND "
          + "(SELECT to_number(value) FROM c_project WHERE c_project_id=:tovalue) AND ad_client_id=:clientId";
      SQLQuery sqlQry=OBDal.getInstance().getSession().createSQLQuery(query.toString());
      sqlQry.setParameter("fromvalue", securityRulesProj.getFromproject().getId());
      sqlQry.setParameter("tovalue", securityRulesProj.getToproject().getId());
      sqlQry.setParameter("clientId", clientId);
      List<String> includeRows = (ArrayList<String>) sqlQry.list();
      for(String strOB : includeRows) {
       rulesIncludeList.add(strOB);
      }
    }
    
    //Create IncludeCode List
    else {
      
      query = "SELECT c_project_id FROM c_project WHERE to_number(value) between :fromCode AND :toCode AND ad_client_id=:clientId";
      SQLQuery sqlQry=OBDal.getInstance().getSession().createSQLQuery(query.toString());
      sqlQry.setParameter("fromCode", securityRulesProj.getFromcode());
      sqlQry.setParameter("toCode", securityRulesProj.getTocode()); 
      sqlQry.setParameter("clientId", clientId);
      List<String> codeRows=(ArrayList<String>)sqlQry.list();
      for(String strOB : codeRows) {
       rulesCodeList.add(strOB);
      
      }
    }
  }

  //Get All values from SubAccount tab
  query = "SELECT c_project_id FROM Efin_Security_Rules_Proj where efin_security_rules_id=:ruleid AND ad_client_id=:clientId";
  SQLQuery sqlQry=OBDal.getInstance().getSession().createSQLQuery(query.toString());
  sqlQry.setParameter("ruleid", rules.getId());
  sqlQry.setParameter("clientId", clientId);
  
  List<String> strSubAccountRows=(ArrayList<String>)sqlQry.list();
  for(String strOB : strSubAccountRows) {
    existingSubAccountList.add(strOB);
  }
  
  //Add All Include List and Remove all Exclude List
  rulesIncludeList.addAll(rulesCodeList);
  rulesIncludeList.removeAll(rulesExcludeList);
  rulesIncludeList.removeAll(existingSubAccountList);
    
  Set<String> rulesSubAccountSet = new HashSet<>(rulesIncludeList);
  for(String rulesSubAct:rulesSubAccountSet) {
    SecurityRuleProject securityRulesSubAccount = OBProvider.getInstance().get(SecurityRuleProject.class);
    securityRulesSubAccount.setClient(rules.getClient());
    securityRulesSubAccount.setOrganization(rules.getOrganization());
    securityRulesSubAccount.setProject(OBDal.getInstance().get(EfinSecurityprojpickV.class, rulesSubAct));
    securityRulesSubAccount.setActive(true);
    securityRulesSubAccount.setCreatedBy(rules.getCreatedBy());
    securityRulesSubAccount.setCreationDate(rules.getCreationDate());
    securityRulesSubAccount.setUpdatedBy(rules.getUpdatedBy());
    securityRulesSubAccount.setUpdated(rules.getUpdated());
    securityRulesSubAccount.setRule(rules);
    OBDal.getInstance().save(securityRulesSubAccount);
    OBDal.getInstance().flush();
  }   
  return true;
}
  
}