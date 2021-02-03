package sa.elm.ob.finance.ad_process;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author sathishkumar.P
 * 
 */

public class GLConfigurationReadyProcessDAO {
  /**
   * This Class is used to validate all necessary dimensions are defined and also default values are
   * defined for some of the dimensions(projects,FC,future1, future2)
   * 
   */
  private final static Logger LOG = LoggerFactory.getLogger(GLConfigurationReadyProcessDAO.class);

  /**
   * 
   * @param acctschemaID
   * @return error message if it is not validate else null
   */
  public static String validateDimension(String acctschemaID) {

    try {
      Map<String, String> mandatoryDimensions = new ConcurrentHashMap<String, String>();
      StringBuilder msg = new StringBuilder(OBMessageUtils.messageBD("EFIN_Dim_Errormsg"));

      mandatoryDimensions = addMandatoryDimensions(mandatoryDimensions);
      final OBQuery<AcctSchemaElement> dimensions = OBDal.getInstance()
          .createQuery(AcctSchemaElement.class, "accountingSchema.id='" + acctschemaID + "'");

      if (dimensions.list().size() > 0) {
        for (AcctSchemaElement dim : dimensions.list()) {
          if (mandatoryDimensions.containsKey(dim.getType().toUpperCase())) {
            mandatoryDimensions.remove(dim.getType().toUpperCase());
          }
        }
      }

      if (!mandatoryDimensions.isEmpty()) {
        for (String key : mandatoryDimensions.keySet()) {
          msg.append(",");
          msg.append(mandatoryDimensions.get(key));
        }
        return msg.toString().replaceFirst(",", "");
      } else {
        return null;
      }
    } catch (OBException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Exeception in validating dimension:" + e, e);
      }
      return OBMessageUtils.messageBD("EFIN_Process_fail");
    }
  }

  /**
   * 
   * @param mandatoryDimensions
   * @return Map with mandatory dimensions
   */
  private static Map<String, String> addMandatoryDimensions(
      Map<String, String> mandatoryDimensions) {
    // add mandatory Dimensions
    mandatoryDimensions.put("OO", "Organization");
    mandatoryDimensions.put("SR", "Department");
    mandatoryDimensions.put("MC", "Budget Type");
    mandatoryDimensions.put("AC", "Account");
    mandatoryDimensions.put("PJ", "Project");
    mandatoryDimensions.put("U1", "Future1");
    mandatoryDimensions.put("U2", "Future2");
    mandatoryDimensions.put("BP", "Entity");
    mandatoryDimensions.put("AY", "Functional Classification");

    return mandatoryDimensions;

  }

  /**
   * 
   * @param acctSchemaID
   * @return true
   */

  public static boolean insertPreference(String acctSchemaID) {
    OBContext.setAdminMode();

    Map<String, Boolean> visiblePropertiesMap = new ConcurrentHashMap<String, Boolean>();
    Map<String, String> preferenceMap = new ConcurrentHashMap<String, String>();

    visiblePropertiesMap = getVisibleProperties(visiblePropertiesMap, acctSchemaID);
    preferenceMap = getPreferenceMap(preferenceMap);

    org.openbravo.model.common.enterprise.Organization org = OBDal.getInstance()
        .get(org.openbravo.model.common.enterprise.Organization.class, "0");

    for (String pref : preferenceMap.keySet()) {

      final OBQuery<Preference> preferenceQry = OBDal.getInstance().createQuery(Preference.class,
          "property='" + preferenceMap.get(pref) + "' and client.id='"
              + OBContext.getOBContext().getCurrentClient().getId() + "'");

      if (preferenceQry.list().size() == 0) {
        Preference preference = OBProvider.getInstance().get(Preference.class);
        preference.setClient(OBContext.getOBContext().getCurrentClient());
        preference.setOrganization(org);
        preference.setProperty(preferenceMap.get(pref));
        preference.setSearchKey(visiblePropertiesMap.get(pref).toString());
        OBDal.getInstance().save(preference);
      }

    }

    OBDal.getInstance().flush();
    OBContext.restorePreviousMode();

    return true;
  }

  /**
   * 
   * @param preferenceMap
   * @return preferenceMap
   */

  private static Map<String, String> getPreferenceMap(Map<String, String> preferenceMap) {
    // add property for preference
    preferenceMap.put("BP", "Efin_bpdimension_visible");
    preferenceMap.put("PJ", "Efin_Projectdimension_visible");
    preferenceMap.put("U1", "Efin_future1dimension_visible");
    preferenceMap.put("U2", "Efin_future2dimension_visible");
    preferenceMap.put("AY", "Efin_functionalClassdimension_visible");
    return preferenceMap;
  }

  /**
   * 
   * @param visiblePropertiesMap
   * @param acctSchemaID
   * @return visiblePropertiesMap
   */

  private static Map<String, Boolean> getVisibleProperties(
      Map<String, Boolean> visiblePropertiesMap, String acctSchemaID) {

    final OBQuery<AcctSchemaElement> dimensions = OBDal.getInstance()
        .createQuery(AcctSchemaElement.class, "accountingSchema.id='" + acctSchemaID
            + "' and client.id='" + OBContext.getOBContext().getCurrentClient().getId() + "'");

    if (dimensions.list().size() > 0) {
      for (AcctSchemaElement dim : dimensions.list()) {
        visiblePropertiesMap.put(dim.getType(), dim.isEfinInvisible());
      }
    }
    return visiblePropertiesMap;
  }

}
