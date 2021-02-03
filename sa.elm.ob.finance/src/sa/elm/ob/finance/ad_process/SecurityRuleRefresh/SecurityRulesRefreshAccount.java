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
import sa.elm.ob.finance.EfinSecurityRulesAct;
import sa.elm.ob.finance.EfinSecurityRuleslines;
import sa.elm.ob.finance.EfinSecurityactpickV;

/**
 * @author Sowmiya N S
 */
@SuppressWarnings("unused")
public class SecurityRulesRefreshAccount extends SecurityRulesRefresh {  
  private static SecurityRulesRefreshAccount securityRulesRefreshAccount=null;
  
  public static SecurityRulesRefreshAccount getRefreshAccountInstance() {   
    
    if(securityRulesRefreshAccount==null) {     
      securityRulesRefreshAccount=new SecurityRulesRefreshAccount();
    }
    return securityRulesRefreshAccount;
  }
  
  private static final Logger log = LoggerFactory.getLogger("SecurityRulesRefreshAccount");
  private String query;

@Override
public Boolean insertProcess(EfinSecurityRules rules) {
  log.debug("Success");
  Boolean account = false;
  account = insertAccount(rules);
  
  if(account) {    
    return true;
  }
  return false;
}

@SuppressWarnings("unchecked")
private Boolean insertAccount(EfinSecurityRules rules) { 
  //Process to be done
  List<EfinSecurityRuleslines> rulesList = rules.getEfinSecurityRuleslinesList();
  List<String> rulesIncludeList = new ArrayList<>();
  List<String> rulesExcludeList = new ArrayList<>();
  List<String> rulesCodeList =new ArrayList<>();
  List<String> existingAccountList = new ArrayList<>();
  Long fromCode,toCode;
  String clientId=rules.getClient().getId();
    
  for(EfinSecurityRuleslines securityRulesLines:rulesList) {    
    String type = securityRulesLines.getType();
    
    if(type.equals("EX")) {
      //read data from c_element value by using between functionality for from act and to act
      query = "SELECT  c_elementvalue_id FROM c_elementvalue WHERE to_number(value) between "
          + "(SELECT to_number(value) FROM c_elementvalue WHERE c_elementvalue_id=:fromvalue) AND "
          + "(SELECT to_number(value) FROM c_elementvalue WHERE c_elementvalue_id=:tovalue) and ad_client_id=:clientId";
      SQLQuery sqlQry=OBDal.getInstance().getSession().createSQLQuery(query.toString());
      sqlQry.setParameter("fromvalue", securityRulesLines.getFromact().getId());
      sqlQry.setParameter("tovalue", securityRulesLines.getToact().getId());
      sqlQry.setParameter("clientId", clientId);
      List<String> excludeRows = (ArrayList<String>) sqlQry.list();
      
      for(String strOB : excludeRows) {        
       rulesExcludeList.add(strOB);
      }
    }
    
    else if(type.equals("IN")) {      
      //read data from c_element value by using between functionality for from act and to act
      query = "SELECT  c_elementvalue_id FROM c_elementvalue WHERE to_number(value) between "
          + "(SELECT to_number(value) FROM c_elementvalue WHERE c_elementvalue_id=:fromvalue) AND "
          + "(SELECT to_number(value) FROM c_elementvalue WHERE c_elementvalue_id=:tovalue) and ad_client_id=:clientId";
      SQLQuery sqlQry=OBDal.getInstance().getSession().createSQLQuery(query.toString());
      sqlQry.setParameter("fromvalue", securityRulesLines.getFromact().getId());
      sqlQry.setParameter("tovalue", securityRulesLines.getToact().getId());
      sqlQry.setParameter("clientId", clientId);
      List<String> includeRows = (ArrayList<String>) sqlQry.list();
      
      for(String strOB : includeRows) {        
       rulesIncludeList.add(strOB);
      }
    }
    
    else {     
      query = "SELECT c_elementvalue_id FROM c_elementvalue WHERE to_number(value) between :fromCode AND :toCode and ad_client_id=:clientId";
      SQLQuery sqlQry=OBDal.getInstance().getSession().createSQLQuery(query.toString());
      sqlQry.setParameter("fromCode", securityRulesLines.getFromCode());
      sqlQry.setParameter("toCode", securityRulesLines.getToCode());      
      sqlQry.setParameter("clientId", clientId);
      List<String> codeRows=sqlQry.list();
      
      for(String strOB : codeRows) {        
       rulesCodeList.add(strOB);
      }
    }
  }

  //Get All values from Accounts tab
  query = "SELECT c_elementvalue_id FROM Efin_Security_Rules_Act where efin_security_rules_id=:ruleid and ad_client_id=:clientId";
  SQLQuery sqlQry=OBDal.getInstance().getSession().createSQLQuery(query.toString());
  sqlQry.setParameter("ruleid", rules.getId());
  sqlQry.setParameter("clientId", clientId);
  List<String> accountRows=sqlQry.list();
  
  for(String strOB : accountRows) {   
    existingAccountList.add(strOB);
  }
  
  //Add All Include List and Remove all Exclude List
  rulesIncludeList.addAll(rulesCodeList);
  rulesIncludeList.removeAll(rulesExcludeList);
  rulesIncludeList.removeAll(existingAccountList);
    
  //Remove Duplication
  Set<String> rulesAccountSet = new HashSet<>(rulesIncludeList);
  
  for(String rulesAct:rulesAccountSet) {  
    EfinSecurityRulesAct securityRulesAccount = OBProvider.getInstance().get(EfinSecurityRulesAct.class);
    securityRulesAccount.setClient(rules.getClient());
    securityRulesAccount.setOrganization(rules.getOrganization());
    securityRulesAccount.setElementvalue(OBDal.getInstance().get(EfinSecurityactpickV.class, rulesAct));
    securityRulesAccount.setActive(true);
    securityRulesAccount.setCreatedBy(rules.getCreatedBy());
    securityRulesAccount.setCreationDate(rules.getCreationDate());
    securityRulesAccount.setUpdatedBy(rules.getUpdatedBy());
    securityRulesAccount.setUpdated(rules.getUpdated());
    securityRulesAccount.setEfinSecurityRules(rules);
    OBDal.getInstance().save(securityRulesAccount);
    OBDal.getInstance().flush();
  }   
  return true;
}  
}
