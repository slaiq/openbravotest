package sa.elm.ob.utility.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.utility.Tree;
import org.openbravo.model.ad.utility.TreeNode;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;

public class OrgUtils {
  private static Logger log = Logger.getLogger(OrgUtils.class);
  private static String MINISTER = "0100001-0200100";

  public static JSONObject updateParentDept() {
    JSONObject resultObject = new JSONObject();
    List<BusinessPartner> bpList = new ArrayList<BusinessPartner>();
    try {
      OBContext.setAdminMode();
      bpList = getEmployeesWithoutParent();
      if (bpList.size() > 0)
        resultObject = updateEmployeeParent(bpList);
      else {
        resultObject.put("result", "S");
        resultObject.put("message", "Update Success");
      }
    } catch (Exception e) {
      log.error("Exception while importDepartments: ", e);
      try {
        resultObject.put("result", "E");
        resultObject.put("message", e.getMessage());
      } catch (JSONException e1) {
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return resultObject;
  }

  private static JSONObject updateEmployeeParent(List<BusinessPartner> bpList) {
    JSONObject resultObject = new JSONObject();
    try {
      OBContext.setAdminMode();
      String strParentCode = "";
      for (BusinessPartner businessPartner : bpList) {
        strParentCode = getParentOrg(businessPartner.getEhcmDepartmentCode());

        businessPartner.setEhcmParentOrg(strParentCode);
        OBDal.getInstance().save(businessPartner);
      }
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

      resultObject.put("result", "S");
      resultObject.put("message", "Update done");
    } catch (Exception e) {
      log.error("Exception while updateEmployeeParent: ", e);
      OBDal.getInstance().rollbackAndClose();
      try {
        resultObject.put("result", "E");
        resultObject.put("message", e.getMessage());
      } catch (JSONException e1) {
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return resultObject;
  }

  public static String getParentOrg(String ehcmDepartmentCode) {
    String strEmpParent = "", strHQ = "", strMinisterOrgId = "";
    List<String> hqChildren = new ArrayList<String>();

    try {
      OBContext.setAdminMode();
      strHQ = getHQOrg();
      hqChildren = getChildren(strHQ);
      strMinisterOrgId = getMinisterOrgId();
      return getParentOrganization(ehcmDepartmentCode, strHQ, strMinisterOrgId, hqChildren);
    } catch (Exception e) {
      log.error("Exception while getParentOrg: ", e);
      return strEmpParent;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private static String getParentOrganization(String departmentCode, String hQ,
      String ministerOrgId, List<String> hqChildren) {
    String strEmpParent;
    String strParentId;
    List<Organization> orgList;
    OBQuery<Organization> obQuery = OBDal.getInstance().createQuery(Organization.class,
        " where  searchKey = :orgCode ");
    obQuery.setNamedParameter("orgCode", departmentCode);
    orgList = obQuery.list();
    if (orgList.size() > 0) {
      Organization org = orgList.get(0);
      strEmpParent = org.getId();
      strParentId = OBContext.getOBContext().getOrganizationStructureProvider()
          .getParentOrg(strEmpParent);
      if (StringUtils.isBlank(strParentId))
        strParentId = "";
      if (strParentId.equals(ministerOrgId)) {
        return org.getSearchKey();
      } else if (hqChildren.contains(strEmpParent) || hQ.equals(strEmpParent)) {
        return org.getSearchKey();
      } else {
        departmentCode = getParentCode(strEmpParent);
        return getParentOrganization(departmentCode, hQ, ministerOrgId, hqChildren);
      }
    }
    return "";
  }

  private static String getMinisterOrgId() {
    String ministerOrgId = "";
    OBQuery<Organization> ministerQuery = OBDal.getInstance().createQuery(Organization.class,
        " where  searchKey = :orgCode ");
    ministerQuery.setNamedParameter("orgCode", MINISTER);
    if (ministerQuery != null) {
      List<Organization> ministerList = ministerQuery.list();
      if (ministerList != null && ministerList.size() > 0) {
        ministerOrgId = ministerList.get(0).getId();
      }
    }
    return ministerOrgId;
  }

  private static String getParentCode(String strNodeId) {
    StringBuilder queryBuilder = new StringBuilder();
    List<TreeNode> nodeList = new ArrayList<TreeNode>();
    String strParentCode = "", orgId = "";

    try {
      OBContext.setAdminMode();
      String strTreeId = getTreeId();

      if (StringUtils.isNotBlank(strTreeId)) {
        queryBuilder = new StringBuilder();
        queryBuilder.append("  node = :strNodeId and  tree.id  = :treeId ");

        OBQuery<TreeNode> nodeQuery = OBDal.getInstance().createQuery(TreeNode.class,
            queryBuilder.toString());
        nodeQuery.setNamedParameter("strNodeId", strNodeId);
        nodeQuery.setNamedParameter("treeId", strTreeId);

        if (nodeQuery != null) {
          nodeList = nodeQuery.list();
          if (nodeList.size() > 0) {
            orgId = nodeList.get(0).getReportSet();
            if (StringUtils.isNotBlank(orgId))
              strParentCode = Utility.getObject(Organization.class, orgId).getSearchKey();
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception while getChildNodeList: ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return strParentCode;
  }

  private static List<String> getChildren(String strHQ) {
    List<String> childrenList = new ArrayList<String>();
    List<TreeNode> nodeList = new ArrayList<TreeNode>();
    try {
      OBContext.setAdminMode();
      nodeList = getChildNodeList(strHQ);
      if (nodeList.size() > 0) {
        for (TreeNode node : nodeList) {
          childrenList.add(node.getNode());
        }
      }
    } catch (Exception e) {
      log.error("Exception while getChildren: ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return childrenList;
  }

  private static List<TreeNode> getChildNodeList(String strParentId) {
    StringBuilder queryBuilder = new StringBuilder();
    List<TreeNode> nodeList = new ArrayList<TreeNode>();
    try {
      OBContext.setAdminMode();
      String strTreeId = getTreeId();

      if (StringUtils.isNotBlank(strTreeId)) {
        queryBuilder = new StringBuilder();
        queryBuilder.append("  reportSet = :parentId and  tree.id  = :treeId ");

        OBQuery<TreeNode> nodeQuery = OBDal.getInstance().createQuery(TreeNode.class,
            queryBuilder.toString());
        nodeQuery.setNamedParameter("parentId", strParentId);
        nodeQuery.setNamedParameter("treeId", strTreeId);

        if (nodeQuery != null) {
          nodeList = nodeQuery.list();
        }
      }
    } catch (Exception e) {
      log.error("Exception while getChildNodeList: ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return nodeList;
  }

  private static String getHQOrg() {
    String strHQ = "";
    List<TreeNode> hqOrgList = new ArrayList<TreeNode>();
    try {
      OBContext.setAdminMode();
      hqOrgList = getChildNodeList("0");
      if (hqOrgList.size() > 0) {
        strHQ = hqOrgList.get(0).getNode();
      }
    } catch (Exception e) {
      strHQ = "";
      log.error("Exception while getHQOrg: ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return strHQ;
  }

  private static String getTreeId() {
    String strTreeId = "";
    try {
      OBContext.setAdminMode();
      List<Tree> treeList = new ArrayList<Tree>();
      StringBuilder queryBuilder = new StringBuilder();
      queryBuilder.append(" where client.id = :clientId and typeArea = 'OO' ");

      OBQuery<Tree> orgTreeQuery = OBDal.getInstance().createQuery(Tree.class,
          queryBuilder.toString());
      orgTreeQuery.setNamedParameter("clientId",
          OBContext.getOBContext().getCurrentClient().getId());

      if (orgTreeQuery != null) {
        treeList = orgTreeQuery.list();
        if (treeList.size() > 0) {
          strTreeId = treeList.get(0).getId();
        }
      }
    } catch (Exception e) {
      log.error("Exception while getTreeId(): ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return strTreeId;
  }

  private static List<BusinessPartner> getEmployeesWithoutParent() {
    List<BusinessPartner> bpList = new ArrayList<BusinessPartner>();
    StringBuilder whereClause = new StringBuilder();
    OBQuery<BusinessPartner> empQuery = null;
    try {
      OBContext.setAdminMode();
      whereClause.append(
          "  employee ='Y' and length(coalesce(ehcmDepartmentCode,''))  > 0 and ehcmParentOrg is null ");
      empQuery = OBDal.getInstance().createQuery(BusinessPartner.class, whereClause.toString());
      if (empQuery != null) {
        bpList = empQuery.list();
      }
    } catch (Exception e) {
      bpList = new ArrayList<BusinessPartner>();
      log.error("Exception while getEmployeesWithoutParent: ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return bpList;
  }
}
