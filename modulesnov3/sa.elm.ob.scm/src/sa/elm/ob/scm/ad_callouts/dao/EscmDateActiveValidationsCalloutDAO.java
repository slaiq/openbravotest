package sa.elm.ob.scm.ad_callouts.dao;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Priyanka Ranjan on 06/03/2018
 */
// DAO file for EscmDateActiveValidationsCallout

public class EscmDateActiveValidationsCalloutDAO {
  private static final Logger LOG = LoggerFactory
      .getLogger(EscmDateActiveValidationsCalloutDAO.class);

  /**
   * 
   * @return currentHijriDate
   */
  public static String getCurrentHijriDate() {
    String currentHijriDate = "", strQuery = "";
    Query query = null;
    try {
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      Date now = new Date();
      strQuery = " select eut_convert_to_hijri_timestamp('" + dateFormat.format(now) + "')";
      query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      @SuppressWarnings("unchecked")
      List<Object> queryList = query.list();
      if (queryList != null && queryList.size() > 0) {
        Object row = queryList.get(0);
        currentHijriDate = (String) row;
      }

    } catch (OBException e) {
      LOG.error("Exception while getCurrentHijriDate:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
    return currentHijriDate;
  }
}
