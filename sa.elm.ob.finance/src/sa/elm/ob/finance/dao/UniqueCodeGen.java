package sa.elm.ob.finance.dao;

import java.sql.Connection;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.project.Project;
import org.openbravo.model.sales.SalesRegion;

public class UniqueCodeGen {
  Connection conn = null;
  private static Logger log = Logger.getLogger(UniqueCodeGen.class);

  public UniqueCodeGen(Connection conn) {
    this.conn = conn;
  }

  public String getUniqueCode(String OrgId, String DeptId, String ElementValueId, String BudgTypeId,
      String ProjectId, String funtionalClassId, String future1Id, String future2Id,
      String entityId) {
    String uniqueCode = null;
    String orgKey = null, deptKey = null, acctKey = null, budgTypeKey = null, projKey = null,
        funclassKey = null, future1Key = null, future2Key = null, entityKey = null;
    try {

      if (!StringUtils.isEmpty(OrgId)) {
        Organization bud = OBDal.getInstance().get(Organization.class, OrgId);
        orgKey = bud.getSearchKey();
      }
      if (!StringUtils.isEmpty(DeptId)) {
        SalesRegion department = OBDal.getInstance().get(SalesRegion.class, DeptId);
        deptKey = department.getSearchKey();

      }
      if (!StringUtils.isEmpty(ElementValueId)) {
        ElementValue account = OBDal.getInstance().get(ElementValue.class, ElementValueId);
        acctKey = account.getSearchKey();
      }
      if (!StringUtils.isEmpty(BudgTypeId)) {
        Campaign Budget = OBDal.getInstance().get(Campaign.class, BudgTypeId);
        budgTypeKey = Budget.getSearchKey();

      }

      if (!StringUtils.isEmpty(ProjectId)) {
        Project project = OBDal.getInstance().get(Project.class, ProjectId);
        projKey = project.getSearchKey();
      }

      if (!StringUtils.isEmpty(funtionalClassId)) {
        ABCActivity activity = OBDal.getInstance().get(ABCActivity.class, funtionalClassId);
        funclassKey = activity.getSearchKey();

      }
      if (!StringUtils.isEmpty(future1Id)) {
        UserDimension1 user1 = OBDal.getInstance().get(UserDimension1.class, future1Id);
        future1Key = user1.getSearchKey();

      }
      if (!StringUtils.isEmpty(future2Id)) {
        UserDimension2 user2 = OBDal.getInstance().get(UserDimension2.class, future2Id);
        future2Key = user2.getSearchKey();
      }
      if (!StringUtils.isEmpty(entityId)) {
        BusinessPartner entity = OBDal.getInstance().get(BusinessPartner.class, entityId);
        entityKey = entity.getEfinDocumentno();
      }
      if (StringUtils.isEmpty(uniqueCode)) {
        if ((!StringUtils.isEmpty(OrgId)) && (!StringUtils.isEmpty(DeptId))
            && (!StringUtils.isEmpty(ElementValueId)) && (!StringUtils.isEmpty(BudgTypeId))
            && (!StringUtils.isEmpty(ProjectId)) && (!StringUtils.isEmpty(funtionalClassId))
            && (!StringUtils.isEmpty(future1Id))
            && (!StringUtils.isEmpty(future2Id) && (!StringUtils.isEmpty(entityKey)))) {
          uniqueCode = orgKey + "-" + deptKey + "-" + acctKey + "-" + projKey + "-" + budgTypeKey
              + "-" + entityKey + "-" + funclassKey + "-" + future1Key + "-" + future2Key;
        } else
          uniqueCode = "";
      }
      return uniqueCode;
    } catch (final Exception e) {
      log.error("Exception in getUniqueCode :", e);
    }
    return uniqueCode;

  }
}
