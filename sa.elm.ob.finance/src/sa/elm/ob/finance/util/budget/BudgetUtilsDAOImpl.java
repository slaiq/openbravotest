package sa.elm.ob.finance.util.budget;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.utility.Tree;
import org.openbravo.model.ad.utility.TreeNode;

public class BudgetUtilsDAOImpl implements BudgetUtilsDAO {
  private static final Logger log4j = Logger.getLogger(BudgetUtilsDAO.class);
  private static final String TREE_TYPE = "EV";

  @Override
  public String getParentAccount(String strElementValueID, String strClientID) {
    String strParentId = "";
    String strWhereClause = "", strTreeID = "";

    try {
      strWhereClause = " where client.id = :clientID  and node = :nodeID and tree.id = :treeID ";

      strTreeID = getTreeID(strClientID);

      if (StringUtils.isNotEmpty(strTreeID)) {

        OBQuery<TreeNode> nodeQuery = OBDal.getInstance().createQuery(TreeNode.class,
            strWhereClause);
        nodeQuery.setNamedParameter("clientID", strClientID);
        nodeQuery.setNamedParameter("nodeID", strElementValueID);
        nodeQuery.setNamedParameter("treeID", strTreeID);

        if (nodeQuery != null) {

          List<TreeNode> nodes = nodeQuery.list();

          if (nodes.size() > 0) {
            strParentId = nodes.get(0).getReportSet();
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception while getParentAccount() :" + e);
      e.printStackTrace();
    }
    return strParentId;
  }

  @Override
  public List<String> getChildren(String strElementValueID, String strClientID) {
    List<String> childElements = new ArrayList<String>();
    String strWhereClause = "", strTreeID = "";
    try {
      strTreeID = getTreeID(strClientID);

      strWhereClause = " where reportSet = :strElementValueID  and tree.id = :strTreeID ";
      if (StringUtils.isNotEmpty(strTreeID)) {
        OBQuery<TreeNode> nodeQuery = OBDal.getInstance().createQuery(TreeNode.class,
            strWhereClause);
        nodeQuery.setNamedParameter("strElementValueID", strElementValueID);
        nodeQuery.setNamedParameter("strTreeID", strTreeID);

        if (nodeQuery != null) {
          List<TreeNode> nodes = nodeQuery.list();

          if (nodes.size() > 0) {
            for (TreeNode treenode : nodes) {
              childElements.add(treenode.getNode());
              getChildren(treenode.getNode(), strClientID);
            }
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception while getChildren() :" + e);
      e.printStackTrace();
    }
    return childElements;
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<String> getProjectSummaryAccounts(String strClientID) {
    List<String> projectSummaryAccounts = new ArrayList<String>();
    StringBuilder queryBuilder = new StringBuilder();
    try {

      queryBuilder.append("  select e.accountElement.id from ");
      queryBuilder.append(" EFIN_BudgetType_Acct e where e.client.id = ? and ");
      queryBuilder.append("  e.salesCampaign.efinBudgettype = 'F' and e.accountElement.id in ");
      queryBuilder.append(" ( select c.accountElement.id from EFIN_BudgetType_Acct c ");
      queryBuilder.append("  where c.client.id = ? and c.salesCampaign.efinBudgettype = 'C') ");

      Query accountsQuery = OBDal.getInstance().getSession().createQuery(queryBuilder.toString());
      accountsQuery.setParameter(0, strClientID);
      accountsQuery.setParameter(1, strClientID);
      if (accountsQuery != null) {
        projectSummaryAccounts = accountsQuery.list();
      }

    } catch (Exception e) {
      log4j.error("Exception while getProjectSummaryAccounts(): " + e);
      e.printStackTrace();
    }

    return projectSummaryAccounts;
  }

  @Override
  public String getTreeID(String strClientID) {
    String strTreeID = "";
    try {
      String strWhereClause = "";

      strWhereClause = " where client.id = :clientID and typeArea =:treeType";

      OBQuery<Tree> treeQuery = OBDal.getInstance().createQuery(Tree.class, strWhereClause);
      treeQuery.setNamedParameter("clientID", strClientID);
      treeQuery.setNamedParameter("treeType", TREE_TYPE);

      if (treeQuery != null) {
        List<Tree> trees = treeQuery.list();
        if (trees.size() > 0) {
          strTreeID = trees.get(0).getId();
        }
      }

    } catch (Exception e) {
      log4j.error("Exception while getTreeID() :" + e);
      e.printStackTrace();
    }

    return strTreeID;
  }

}
