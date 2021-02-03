package sa.elm.ob.finance.dao;

import java.util.Map;

import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaElement;

/**
 * 
 * @author sathishkumar.P
 * 
 */

public class GLDimensionEventDAO {

  /**
   * this DAO class is used to update the preference value based on visible flag
   */

  /**
   * 
   * @param preferenceMap
   * @param dimension
   * @return true or false
   */

  public static boolean updatePreference(Map<String, String> preferenceMap,
      AcctSchemaElement dimension) {

    final OBQuery<Preference> preference = OBDal.getInstance().createQuery(Preference.class,
        "property='" + preferenceMap.get(dimension.getType()) + "'");

    if (preference.list().size() > 0) {
      Preference pref = preference.list().get(0);
      pref.setSearchKey(dimension.isEfinInvisible().toString());
      OBDal.getInstance().save(pref);
    }

    return true;

  }

}
