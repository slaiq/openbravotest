package sa.elm.ob.finance.filterexpression;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.Organization;

/**
 * @author Gopalakrishnan on 11/08/2016
 * 
 */
public class EfinAddPaymentFilterExpression implements FilterExpression {

  private final static Logger log4j = Logger.getLogger(EfinAddPaymentFilterExpression.class);

  @Override
  public String getExpression(Map<String, String> requestMap) {
    String strCurrentParam = "";
    try {
      strCurrentParam = requestMap.get("currentParam");
      String orgId = null;

      // Get the Organization
      OBQuery<Organization> org = OBDal.getInstance().createQuery(Organization.class,
          " id <>'0' order by searchKey asc ");
      List<Organization>orgList = org.list();
      if (orgList.size() > 0) {
        Organization org1 = orgList.get(0);
        orgId = org1.getId();
      }

      if (strCurrentParam.equals("AD_Org_ID"))
        return orgId;

    } catch (Exception e) {
      log4j.debug("Error getting the default value of Organization" + strCurrentParam + " "
          + e.getMessage());
      return null;
    }
    return null;
  }
}
