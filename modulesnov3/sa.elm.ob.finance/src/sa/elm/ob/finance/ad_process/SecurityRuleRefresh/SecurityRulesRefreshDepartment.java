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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinSecurityRules;
import sa.elm.ob.finance.EfinSecurityRulesdept;
import sa.elm.ob.finance.EfinSecurityRuleslines;
import sa.elm.ob.finance.EfinSecurityactpickV;
import sa.elm.ob.finance.EfinSecuritydeptpickV;
import sa.elm.ob.finance.SecurityRuleDepartment;

/**
 * @author Sowmiya N S
 */
@SuppressWarnings("unused")
public class SecurityRulesRefreshDepartment extends SecurityRulesRefresh {
  
  private static SecurityRulesRefreshDepartment securityRulesRefreshDepartment=null;
  public static SecurityRulesRefreshDepartment getRefreshdepartmentInstance() {
    
    if(securityRulesRefreshDepartment==null) {     
      securityRulesRefreshDepartment=new SecurityRulesRefreshDepartment();
    }
    return securityRulesRefreshDepartment;
  }
  private static final Logger log = LoggerFactory.getLogger("securityRulesRefreshDepartment");
  private String query;

@Override
public Boolean insertProcess(EfinSecurityRules rules) {

  log.debug("Success");
  Boolean department = false;
  department = insertDepartment(rules);
  
  if(department) {   
    return true;
  }
  return false;
}

@SuppressWarnings("unchecked")
private Boolean insertDepartment(EfinSecurityRules rules) {
  
  //Process to be done
  List<EfinSecurityRulesdept> rulesList = rules.getEfinSecurityRulesdeptList();
  List<String> rulesIncludeList = new ArrayList<>();
  List<String> rulesExcludeList = new ArrayList<>();
  List<String> rulesCodeList =new ArrayList<>();
  List<String> existingDepartmentList = new ArrayList<>();
  Long fromCode,toCode;
  String clientId=rules.getClient().getId();
    
  for(EfinSecurityRulesdept securityRulesLines:rulesList) {
    String type = securityRulesLines.getType();
   
    if(type.equals("EX")) {      
      //read data from c_salesregion value by using between functionality for from dept and to dept
      query = "SELECT  c_salesregion_id FROM c_salesregion WHERE to_number(value) between "
          + "(SELECT to_number(value) FROM c_salesregion WHERE c_salesregion_id=:fromvalue) AND "
          + "(SELECT to_number(value) FROM c_salesregion WHERE c_salesregion_id=:tovalue) and ad_client_id=:clientId";
      SQLQuery sqlQry=OBDal.getInstance().getSession().createSQLQuery(query.toString());
      sqlQry.setParameter("fromvalue", securityRulesLines.getFromdept().getId());
      sqlQry.setParameter("tovalue", securityRulesLines.getTodept().getId());
      sqlQry.setParameter("clientId", clientId);
      List<String> excludeRows = (ArrayList<String>) sqlQry.list();
      for(String strOB : excludeRows) {
        
       rulesExcludeList.add(strOB);
      }
    }
    
    else if(type.equals("IN")) {      
      //read data from c_salesregion value by using between functionality for from dept and to dept
      query = "SELECT  c_salesregion_id FROM c_salesregion WHERE to_number(value) between "
          + "(SELECT to_number(value) FROM c_salesregion WHERE c_salesregion_id=:fromvalue) AND "
          + "(SELECT to_number(value) FROM c_salesregion WHERE c_salesregion_id=:tovalue) and ad_client_id=:clientId";
      SQLQuery sqlQry=OBDal.getInstance().getSession().createSQLQuery(query.toString());
      sqlQry.setParameter("fromvalue", securityRulesLines.getFromdept().getId());
      sqlQry.setParameter("tovalue", securityRulesLines.getTodept().getId());
      sqlQry.setParameter("clientId", clientId);
      List<String> includeRows = (ArrayList<String>) sqlQry.list();
      for(String strOB : includeRows) {
        
       rulesIncludeList.add(strOB);
      }
    }
    
    else {     
      query = "SELECT c_salesregion_id FROM c_salesregion WHERE to_number(value) between :fromCode AND :toCode and ad_client_id=:clientId";
      SQLQuery sqlQry=OBDal.getInstance().getSession().createSQLQuery(query.toString());
      sqlQry.setParameter("fromCode", securityRulesLines.getFromCode());
      sqlQry.setParameter("toCode", securityRulesLines.getToCode());  
      sqlQry.setParameter("clientId", clientId);
      List<String> codeRows=(ArrayList<String>) sqlQry.list();
      
      for(String strOB : codeRows) {
       rulesCodeList.add(strOB);
      }
    }
  }
  
  //Get All values from departments tab
  query = "SELECT c_salesregion_id FROM efin_security_rules_dept where efin_security_rules_id=:ruleid and ad_client_id=:clientId";
  SQLQuery sqlQry=OBDal.getInstance().getSession().createSQLQuery(query.toString());
  sqlQry.setParameter("ruleid", rules.getId());
  sqlQry.setParameter("clientId", clientId);
  List<String> departmentRows=(ArrayList<String>) sqlQry.list();
  
  for(String strOB : departmentRows) {
    existingDepartmentList.add(strOB);
  }
  
  //Add All Include List and Remove all Exclude List
  rulesIncludeList.addAll(rulesCodeList);
  rulesExcludeList.addAll(existingDepartmentList);
  rulesIncludeList.removeAll(rulesExcludeList);
    
  //Remove Duplications
  Set<String> rulesDepartmentSet = new HashSet<>(rulesIncludeList);
  
  for(String rulesDept:rulesDepartmentSet) {   
    SecurityRuleDepartment securityRulesDepartment = OBProvider.getInstance().get(SecurityRuleDepartment.class);
    securityRulesDepartment.setClient(rules.getClient());
    securityRulesDepartment.setOrganization(rules.getOrganization());
    securityRulesDepartment.setDepartment(OBDal.getInstance().get(EfinSecuritydeptpickV.class, rulesDept));
    securityRulesDepartment.setActive(true);
    securityRulesDepartment.setCreatedBy(rules.getCreatedBy());
    securityRulesDepartment.setCreationDate(rules.getCreationDate());
    securityRulesDepartment.setUpdatedBy(rules.getUpdatedBy());
    securityRulesDepartment.setUpdated(rules.getUpdated());
    securityRulesDepartment.setRule(rules);
    OBDal.getInstance().save(securityRulesDepartment);
    OBDal.getInstance().flush();
  }   
  return true;
}  
}
