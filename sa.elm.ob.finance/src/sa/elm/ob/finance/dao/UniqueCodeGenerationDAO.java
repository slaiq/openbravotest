package sa.elm.ob.finance.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaElement;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.project.Project;
import org.openbravo.model.sales.SalesRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author sathishkumar.P
 * 
 */

public final class UniqueCodeGenerationDAO {

  /**
   * This class is used to generate Unique code combination for accounts
   */

  private final static Logger LOG = LoggerFactory.getLogger(UniqueCodeGenerationDAO.class);
  private final static String SEPARATOR = "-";
  private final static String PROJECT = "PJ";
  private final static String USERDIMENSION1 = "U1";
  private final static String USERDIMENSION2 = "U2";
  private final static String ACTIVITY = "AY";
  private final static String BPARTNER = "BP";

  public static boolean generateCode(String clientId) {

    Connection connection = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    AcctSchema acctschema;
    Organization org;
    ElementValue acc;
    SalesRegion dp;
    Campaign budget;
    Project proj;
    ABCActivity act;
    BusinessPartner bp;
    UserDimension2 user2;
    UserDimension1 user1;
    String uniqueCodeName;

    int i = 0;

    try {
      connection = OBDal.getInstance().getConnection();
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("No Database Connection Available.Exception:" + e);
      }
      throw new OBException("Error while establishing connection");

    }

    Map<String, Object> sequenceNo = new ConcurrentHashMap<String, Object>();
    Map<String, Object> accountObjMap = new ConcurrentHashMap<String, Object>();

    final OBQuery<AcctSchemaElement> dimensions = OBDal.getInstance()
        .createQuery(AcctSchemaElement.class, "client.id='" + clientId + "'");

    if (dimensions.list().size() > 0) {
      for (AcctSchemaElement dim : dimensions.list()) {
        if (dim.getType().equals(PROJECT)) {
          sequenceNo.put(PROJECT, dim.getProject());
        }
        if (dim.getType().equals(USERDIMENSION1)) {
          sequenceNo.put(USERDIMENSION1, dim.getEfinUser1());
        }
        if (dim.getType().equals(USERDIMENSION2)) {
          sequenceNo.put(USERDIMENSION2, dim.getEfinUser2());
        }
        if (dim.getType().equals(ACTIVITY)) {
          sequenceNo.put(ACTIVITY, dim.getActivity());
        }
        if (dim.getType().equals(BPARTNER)) {
          sequenceNo.put(BPARTNER, dim.getBusinessPartner());
        }
      }
    }

    try {

      String query = "select  org.value as orgvalue, dep.value as depvalue, subacc.value as accvalue, proj.value as projvalue, budget.value as budvalue,"
          + "        bp.value as bpvalue,  act.value as actvalue,  u1.value as u1value, u2.value as u2value, gl.c_acctschema_id as glId , "
          + "        subacc.C_ElementValue_id as acctId, proj.c_project_id as projId, budget.c_campaign_id as budId, dep.C_Salesregion_id as depId,"
          + "        bp.c_bpartner_id as bpId, u1.user1_id as u1Id, u2.user2_id as u2Id , act.c_activity_id as actId, org.ad_org_id as orgid "
          + "       from c_campaign  budget "
          + "        join EFIN_BudgetType_Acct parentacc on budget.c_campaign_id = parentacc.c_campaign_id "
          + "        join ad_treenode treenode on parentacc.C_ElementValue_id = treenode.parent_id  "
          + "        join C_ElementValue subacc on treenode.node_id = subacc.C_ElementValue_id and elementlevel ='S' "
          + "        join EFIN_Costorgnization efinorg on subacc.C_ElementValue_id = efinorg.C_ElementValue_id and efinorg.isactive = 'Y' "
          + "        join EFIN_Costcenters costcen on efinorg.EFIN_Costorgnization_id = costcen.EFIN_Costorgnization_id and costcen.isactive = 'Y' "
          + "        join ad_org org on efinorg.org = org.ad_org_id "
          + "        join C_Salesregion dep on costcen.C_Salesregion_ID = dep.C_Salesregion_ID "
          + "        left join c_project  proj on subacc.em_efin_project_id= proj.c_project_id "
          + "        left join c_bpartner  bp on budget.ad_client_id = bp.ad_client_id "
          + "        left join user1  u1 on budget.ad_client_id = u1.ad_client_id "
          + "        left join user2  u2 on budget.ad_client_id = u2.ad_client_id "
          + "        left join c_activity  act on budget.ad_client_id = act.ad_client_id "
          + "        join c_acctschema gl on budget.ad_client_id = gl.ad_client_id "
          + "        where budget.ad_client_id =? order by subacc.value";
      ps = connection.prepareStatement(query);
      ps.setString(1, clientId);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Query" + ps);
      }
      rs = ps.executeQuery();
      while (rs.next()) {

        StringBuilder uniqueCode = new StringBuilder();

        if (accountObjMap.containsKey(rs.getString("glId"))) {
          acctschema = (AcctSchema) accountObjMap.get(rs.getString("glId"));
        } else {
          acctschema = OBDal.getInstance().get(AcctSchema.class, rs.getString("glId"));
          accountObjMap.put(rs.getString("glId"), acctschema);
        }

        if (accountObjMap.containsKey(rs.getString("orgid"))) {
          org = (Organization) accountObjMap.get(rs.getString("orgid"));
        } else {
          org = OBDal.getInstance().get(Organization.class, rs.getString("orgid"));
          accountObjMap.put(rs.getString("orgid"), org);
        }

        if (accountObjMap.containsKey(rs.getString("acctId"))) {
          acc = (ElementValue) accountObjMap.get(rs.getString("acctId"));
        } else {
          acc = OBDal.getInstance().get(ElementValue.class, rs.getString("acctId"));
          accountObjMap.put(rs.getString("acctId"), acc);
        }

        if (accountObjMap.containsKey(rs.getString("depId"))) {
          dp = (SalesRegion) accountObjMap.get(rs.getString("depId"));
        } else {
          dp = OBDal.getInstance().get(SalesRegion.class, rs.getString("depId"));
          accountObjMap.put(rs.getString("depId"), dp);
        }

        if (accountObjMap.containsKey(rs.getString("budId"))) {
          budget = (Campaign) accountObjMap.get(rs.getString("budId"));
        } else {
          budget = OBDal.getInstance().get(Campaign.class, rs.getString("budId"));
          accountObjMap.put(rs.getString("budId"), budget);
        }

        if (StringUtils.isEmpty(rs.getString("projvalue"))) {
          proj = (Project) sequenceNo.get("PJ");
        } else {
          if (accountObjMap.containsKey(rs.getString("projId"))) {
            proj = (Project) accountObjMap.get(rs.getString("projId"));
          } else {
            proj = OBDal.getInstance().get(Project.class, rs.getString("projId"));
            accountObjMap.put(rs.getString("projId"), proj);
          }

        }

        if (StringUtils.isEmpty(rs.getString("bpvalue"))) {
          bp = (BusinessPartner) sequenceNo.get("BP");
        } else {
          if (accountObjMap.containsKey(rs.getString("bpId"))) {
            bp = (BusinessPartner) accountObjMap.get(rs.getString("bpId"));
          } else {
            bp = OBDal.getInstance().get(BusinessPartner.class, rs.getString("bpId"));
            accountObjMap.put(rs.getString("bpId"), bp);
          }

        }

        if (StringUtils.isEmpty(rs.getString("actvalue"))) {
          act = (ABCActivity) sequenceNo.get("AY");
        } else {
          act = OBDal.getInstance().get(ABCActivity.class, rs.getString("actId"));
        }

        if (StringUtils.isEmpty(rs.getString("u1value"))) {
          user1 = (UserDimension1) sequenceNo.get("U1");
        } else {
          if (accountObjMap.containsKey(rs.getString("u1Id"))) {
            user1 = (UserDimension1) accountObjMap.get(rs.getString("u1Id"));
          } else {
            user1 = OBDal.getInstance().get(UserDimension1.class, rs.getString("u1Id"));
            accountObjMap.put(rs.getString("u1Id"), user1);
          }
        }

        if (StringUtils.isEmpty(rs.getString("u2value"))) {
          user2 = (UserDimension2) sequenceNo.get("U2");
        } else {
          if (accountObjMap.containsKey(rs.getString("u2Id"))) {
            user2 = (UserDimension2) accountObjMap.get(rs.getString("u2Id"));
          } else {
            user2 = OBDal.getInstance().get(UserDimension2.class, rs.getString("u2Id"));
            accountObjMap.put(rs.getString("u2Id"), user2);
          }
        }

        uniqueCode.append(rs.getString("orgvalue") + SEPARATOR);
        uniqueCode.append(rs.getString("depvalue") + SEPARATOR);
        uniqueCode.append(rs.getString("accvalue") + SEPARATOR);
        uniqueCode.append(proj.getSearchKey() + SEPARATOR);
        uniqueCode.append(rs.getString("budvalue") + SEPARATOR);
        uniqueCode.append(bp.getSearchKey() + SEPARATOR);
        uniqueCode.append(act.getSearchKey() + SEPARATOR);
        uniqueCode.append(user1.getSearchKey() + SEPARATOR);
        uniqueCode.append(user2.getSearchKey());

        OBCriteria<AccountingCombination> accountCombination = OBDal.getInstance()
            .createCriteria(AccountingCombination.class);
        accountCombination.add(
            Restrictions.eq(AccountingCombination.PROPERTY_EFINUNIQUECODE, uniqueCode.toString()));
        if (!(accountCombination.list() != null && accountCombination.list().size() > 0)) {
          uniqueCodeName = org.getName() + "-" + dp.getName() + "-" + acc.getName() + "-"
              + proj.getName() + "-" + budget.getName() + "-" + bp.getName() + "-" + act.getName()
              + "-" + user1.getName() + "-" + user2.getName();
          AccountingCombination account = OBProvider.getInstance().get(AccountingCombination.class);
          account.setAccountingSchema(acctschema);
          account.setAccount(acc);
          account.setBusinessPartner(bp);
          account.setSalesRegion(dp);
          account.setProject(proj);
          account.setSalesCampaign(budget);
          account.setActivity(act);
          account.setEfinUniqueCode(uniqueCode.toString());
          account.setNdDimension(user2);
          account.setStDimension(user1);
          account.setOrganization(org);
          account.setEfinUniquecodename(uniqueCodeName);
          account.setEFINAccountType("E");

          OBDal.getInstance().save(account);

          if (i % 100 == 0) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Count is" + i + "we are flushing and clearing session");
            }
            OBDal.getInstance().flush();
            OBDal.getInstance().getSession().clear();
          }
          i++;
        }

      }
      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Error while creating account combination", e);
      }
      return false;
    }

    try {
      if (rs != null) {
        rs.close();
      }
      if (connection != null) {
        connection.close();
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Error while creating account combination", e);
      }
      return false;
    }

    return true;

  }

}
