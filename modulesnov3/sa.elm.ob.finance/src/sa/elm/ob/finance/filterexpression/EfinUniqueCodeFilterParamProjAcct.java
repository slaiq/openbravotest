/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2012 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package sa.elm.ob.finance.filterexpression;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.utility.TreeNode;

public class EfinUniqueCodeFilterParamProjAcct implements FilterExpression {
  StringBuilder whereClause = new StringBuilder();

  @Override
  public String getExpression(Map<String, String> requestMap) {
    try {
      OBContext.setAdminMode();
      String budgetTypeId = requestMap.get("C_Campaign_ID");
      String projectId = requestMap.get("C_Project_ID");
      String accountParentId = requestMap.get("em_efin_accountparent");
      String query = "";
      if (!StringUtils.isEmpty(projectId) && !projectId.equalsIgnoreCase("null")
          && !StringUtils.isEmpty(budgetTypeId) && !budgetTypeId.equalsIgnoreCase("null")) {
        // Filter account based on project and budget type
        query = "e.id in (select distinct ac.account.id from FinancialMgmtAccountingCombination ac where ac.salesCampaign.id = '"
            + budgetTypeId + "' and ac.project.id = '" + projectId
            + "' and ac.salesCampaign is not null and ac.project is not null)";
      } else if (!StringUtils.isEmpty(projectId) && !projectId.equalsIgnoreCase("null")) {
        // Filter account based on project
        query = "e.id in (select distinct ac.account.id from FinancialMgmtAccountingCombination ac where ac.project.id = '"
            + projectId + "' and ac.project is not null)";
      } else if (!StringUtils.isEmpty(budgetTypeId) && !budgetTypeId.equalsIgnoreCase("null")) {
        // Filter account based on budget type
        query = "e.id in (select distinct ac.account.id from FinancialMgmtAccountingCombination ac where ac.salesCampaign.id = '"
            + budgetTypeId + "' and ac.salesCampaign is not null)";
      } else {
        // Load all accounts in account dimension
        query = "e.id in (select distinct ac.account.id from FinancialMgmtAccountingCombination ac)";
      }

     whereClause.append(query);
     if (!StringUtils.isEmpty(accountParentId) && !accountParentId.equalsIgnoreCase("null")){
      whereClause.append(" and e.id in ( select node from ADTreeNode where reportSet = '"+accountParentId+"' )");
      checkParent(accountParentId);
     }
     return whereClause.toString();
    
     }catch (OBException e) {
        e.printStackTrace();
        throw new OBException(e.getMessage());
      }
      finally {
        OBContext.restorePreviousMode();
      }
     }
  
    public Boolean isParentId( String nodeID ) {
      
      OBQuery<TreeNode> nodeIsParent = OBDal.getInstance().createQuery(TreeNode.class,"reportSet = :nodeID ");
      nodeIsParent.setNamedParameter("nodeID", nodeID);
      List<TreeNode> nodeIsParentList = nodeIsParent.list();
      if ( nodeIsParentList.size() > 0 ) {
        return true;
      }
      return false;
    }
    
    public void checkParent (String parentID) {
      
      OBQuery<TreeNode> parent = OBDal.getInstance().createQuery(TreeNode.class,"reportSet = :accountParentId ");
      parent.setNamedParameter("accountParentId", parentID);
      
      List<TreeNode> parentList = parent.list();
      
      if ( parentList.size() > 0) {

        for (TreeNode index : parentList) {
          String nodeID = index.getNode();
          if ( isParentId(nodeID) ) {
            whereClause.append(" or e.id in ( select node from ADTreeNode where reportSet = '"+nodeID+"' )");
            checkParent(nodeID);
          }  
        }  
      }
    }
  
}
