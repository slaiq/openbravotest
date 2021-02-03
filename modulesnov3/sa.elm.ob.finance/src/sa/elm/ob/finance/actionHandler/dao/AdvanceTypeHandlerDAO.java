package sa.elm.ob.finance.actionHandler.dao;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.project.Project;
import org.openbravo.model.sales.SalesRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinDistribution;
import sa.elm.ob.finance.efinDistributionLines;

/**
 * 
 * @author Poongodi 15/12/2017
 *
 */

public class AdvanceTypeHandlerDAO {
  private static final Logger LOG = LoggerFactory.getLogger(AdvanceTypeHandlerDAO.class);

  /**
   * This method is used to insert lines in advance Type window.
   * 
   * @param selectedlines
   * @param distributionId
   * @return 1,0
   */
  public static int insertlineinadvancetype(JSONArray selectedlines, String distributionId) {

    efinDistributionLines distLine = null;
    int count = 0;
    String uniquecodeName = "";
    try {
      OBContext.setAdminMode();
      EfinDistribution distribution = OBDal.getInstance().get(EfinDistribution.class,
          distributionId);

      if (selectedlines.length() == 0) {
        count = -1;
      } else if (selectedlines.length() > 0) {
        for (int line = 0; line < selectedlines.length(); line++) {
          JSONObject selectedRow = selectedlines.getJSONObject(line);
          distLine = OBProvider.getInstance().get(efinDistributionLines.class);
          distLine.setOrganization(
              OBDal.getInstance().get(Organization.class, selectedRow.getString("organization")));
          distLine.setClient(distribution.getClient());
          distLine.setEfinDistribution(distribution);
          distLine.setAccountingCombination(
              OBDal.getInstance().get(AccountingCombination.class, selectedRow.getString("id")));
          distLine.setAccountElement(
              OBDal.getInstance().get(ElementValue.class, selectedRow.getString("account")));
          distLine.setSalesCampaign(
              OBDal.getInstance().get(Campaign.class, selectedRow.getString("salesCampaign")));
          distLine.setSalesRegion(
              OBDal.getInstance().get(SalesRegion.class, selectedRow.getString("department")));
          distLine.setProject(
              OBDal.getInstance().get(Project.class, selectedRow.getString("subAccount")));
          distLine.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class,
              selectedRow.getString("businessPartner")));
          distLine.setActivity(OBDal.getInstance().get(ABCActivity.class,
              selectedRow.getString("functionalClassfication")));
          distLine.setStDimension(
              OBDal.getInstance().get(UserDimension1.class, selectedRow.getString("future1")));
          distLine.setNdDimension(
              OBDal.getInstance().get(UserDimension2.class, selectedRow.getString("ndDimension")));
          AccountingCombination acc = OBDal.getInstance().get(AccountingCombination.class,
              selectedRow.getString("id"));
          if (acc.getEfinUniquecodename() != null) {
            uniquecodeName = acc.getEfinUniquecodename();
            distLine.setADJUniquecodename(uniquecodeName);
          }

          OBDal.getInstance().save(distLine);
          OBDal.getInstance().flush();
        }
        count = 1;
      }
    }

    catch (

    Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception while inserting lines in advance type add lines process : ", e, e);
      }
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

}