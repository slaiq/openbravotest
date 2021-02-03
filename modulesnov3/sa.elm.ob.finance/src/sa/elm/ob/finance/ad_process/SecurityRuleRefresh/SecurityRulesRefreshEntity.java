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
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinSecurityRules;
import sa.elm.ob.finance.EfinSecurityRulesbpartner;
import sa.elm.ob.finance.SecurityRulesBpartner;



/**
 * @author Rashika VS on 19/06/2018
 */
@SuppressWarnings("unused")
public class SecurityRulesRefreshEntity extends SecurityRulesRefresh {
  private static SecurityRulesRefreshEntity securityRulesRefreshEntity=null;
  public static SecurityRulesRefreshEntity getRefreshEntityInstance() {
    if(securityRulesRefreshEntity==null) {
      securityRulesRefreshEntity=new SecurityRulesRefreshEntity();
    }
    return securityRulesRefreshEntity;
  }
  private static final Logger log = LoggerFactory.getLogger("SecurityRulesRefreshEntity");
  
  private String queryInclude,queryExclude,queryExisting,query;

@Override
public Boolean insertProcess(EfinSecurityRules rules) {
  // TODO Auto-generated method stub
  log.debug("Success");
  Boolean entity = false;
  entity = insertEntity(rules);
  if(entity) {
    return true;
  }
  return false;
}

@SuppressWarnings("unchecked")
private Boolean insertEntity(EfinSecurityRules rules) {
  //Process to be done
  List<EfinSecurityRulesbpartner> rulesList = rules.getEfinSecurityRulesbpartnerList();
  List<String> rulesIncludeList = new ArrayList<>();
  List<String> rulesExcludeList = new ArrayList<>();
  List<String> rulesCodeList =new ArrayList<>();
  List<String> existingEntityList = new ArrayList<>();
  Long fromCode,toCode;
  String clientId=rules.getClient().getId();  
  for(EfinSecurityRulesbpartner securityRulesbpartner:rulesList) {
    String type = securityRulesbpartner.getType();
   
    //Create Exclude List 
    if(type.equals("EX")) {
      //read data from c_bpartner by using between functionality for fromBpartner to toBpartner
      //add exclude list
      queryExclude = "SELECT  c_bpartner_id FROM c_bpartner WHERE to_number(em_efin_documentno) between "
          + "(SELECT to_number(em_efin_documentno) FROM c_bpartner WHERE c_bpartner_id=:fromvalue) AND "
          + "(SELECT to_number(em_efin_documentno) FROM c_bpartner WHERE c_bpartner_id=:tovalue) AND ad_client_id=:clientId";
      SQLQuery sqlQry=OBDal.getInstance().getSession().createSQLQuery(queryExclude.toString());
      sqlQry.setParameter("fromvalue", securityRulesbpartner.getFrombpartner().getId());
      sqlQry.setParameter("tovalue", securityRulesbpartner.getTobpartner().getId());
      sqlQry.setParameter("clientId", clientId);
      List<String> ExcludeRows = (ArrayList<String>) sqlQry.list();
      for(String strOB : ExcludeRows) {
       rulesExcludeList.add(strOB);
      }
    }
    
    //Create Include List
    else if(type.equals("IN")) {
      //read data from c_bpartner value by using between functionality for fromBpartner to toBpartner
      //add include list
      queryInclude = "SELECT  c_bpartner_id FROM c_bpartner WHERE to_number(em_efin_documentno) between "
          + "(SELECT to_number(em_efin_documentno) FROM c_bpartner WHERE c_bpartner_id=:fromvalue) AND "
          + "(SELECT to_number(em_efin_documentno) FROM c_bpartner WHERE c_bpartner_id=:tovalue) AND ad_client_id=:clientId";
      SQLQuery sqlQry=OBDal.getInstance().getSession().createSQLQuery(queryInclude.toString());
      sqlQry.setParameter("fromvalue", securityRulesbpartner.getFrombpartner().getId());
      sqlQry.setParameter("tovalue", securityRulesbpartner.getTobpartner().getId());
      sqlQry.setParameter("clientId", clientId);
      List<String> includeRows = (ArrayList<String>) sqlQry.list();
      for(String strOB : includeRows) {
       rulesIncludeList.add(strOB);
      }
    }
    
    //Create IncludeCode List
    else {
      query = "SELECT c_bpartner_id FROM c_bpartner WHERE to_number(em_efin_documentno) between :fromCode AND :toCode AND ad_client_id=:clientId";
      SQLQuery sqlQry=OBDal.getInstance().getSession().createSQLQuery(query.toString());
      sqlQry.setParameter("fromCode", securityRulesbpartner.getFromcode());
      sqlQry.setParameter("toCode", securityRulesbpartner.getTocode());
      sqlQry.setParameter("clientId", clientId);
      List<String> codeRows=(ArrayList<String>)sqlQry.list();
      for(String strOB : codeRows) {
       rulesCodeList.add(strOB);
      }
    }
  }

  //Get All values from Entity tab
  queryExisting = "SELECT c_bpartner_id FROM efin_security_rules_bp where efin_security_rules_id=:ruleid AND ad_client_id=:clientId";
  SQLQuery sqlQry=OBDal.getInstance().getSession().createSQLQuery(queryExisting.toString());
  sqlQry.setParameter("ruleid", rules.getId());
  sqlQry.setParameter("clientId", clientId);
  List<String> EntityRows=(ArrayList<String>)sqlQry.list();
  for(String strOB : EntityRows) {
    existingEntityList.add(strOB);
  }
  
  //Add All Include List and Remove all Exclude List
  rulesIncludeList.addAll(rulesCodeList);
  rulesIncludeList.removeAll(rulesExcludeList);
  rulesIncludeList.removeAll(existingEntityList);  
  Set<String> rulesEntitySet = new HashSet<>(rulesIncludeList);
  for(String rulesBp:rulesEntitySet) {
    SecurityRulesBpartner securityRulesEntity = OBProvider.getInstance().get(SecurityRulesBpartner.class);
    securityRulesEntity.setClient(rules.getClient());
    securityRulesEntity.setOrganization(rules.getOrganization());
    securityRulesEntity.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, rulesBp));
    securityRulesEntity.setActive(true);
    securityRulesEntity.setCreatedBy(rules.getCreatedBy());
    securityRulesEntity.setCreationDate(rules.getCreationDate());
    securityRulesEntity.setUpdatedBy(rules.getUpdatedBy());
    securityRulesEntity.setUpdated(rules.getUpdated());
    securityRulesEntity.setRule(rules);
    OBDal.getInstance().save(securityRulesEntity);
    OBDal.getInstance().flush();
  }   
  return true;
}
  
}