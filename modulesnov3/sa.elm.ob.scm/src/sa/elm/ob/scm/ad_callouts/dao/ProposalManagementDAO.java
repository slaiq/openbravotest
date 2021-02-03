package sa.elm.ob.scm.ad_callouts.dao;

import java.util.List;

import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProposalManagementDAO {
  private static final Logger log = LoggerFactory.getLogger(ProposalManagementDAO.class);

  /**
   * 
   * @param strBidID
   * @return OpenEnvelopday or System Date
   */
  @SuppressWarnings("unchecked")
  public static String getBidOpenenvelopdayOrSystemDate(String strBidID) {
    String openenvelopday = "";

    try {
      OBContext.setAdminMode();
      if (strBidID != null && !strBidID.equals("")) {
        Query query = null;
        String strQuery = "";

        strQuery = "select eut_convert_to_hijri(to_char((select coalesce(max(openenvday), now()) "
            + "from escm_biddates where escm_bidmgmt_id  =? ),'YYYY-MM-DD'))";
        query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
        query.setParameter(0, strBidID);
        List<Object> queryList = query.list();
        if (query != null && queryList.size() > 0) {
          Object row = queryList.get(0);
          openenvelopday = (String) row;
        }
      } else {
        Query query = null;
        String strQuery = "";
        strQuery = "select eut_convert_to_hijri(to_char(now(),'YYYY-MM-DD')) from dual";
        query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
        List<Object> queryList = query.list();
        if (query != null && queryList.size() > 0) {
          Object row = queryList.get(0);
          openenvelopday = (String) row;
        }
      }
      return openenvelopday;

    } catch (OBException e) {
      log.error("Exception while getBidOpenenvelopdayOrSystemDate:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
      // OBDal.getInstance().getSession().clear();
    }
  }

  /**
   * 
   * @param effectiveFrom
   * @param clientid
   * @return EffectiveToDate
   */
  public static String getDateforEffectiveTo(String effectiveFrom, String clientid) {
    String EffectiveToDate = "";
    try {
      OBContext.setAdminMode();
      String[] dateParts = effectiveFrom.split("-");
      String hijiridate = dateParts[2] + dateParts[1] + dateParts[0];
      Query query = null;
      String strQuery = "";
      strQuery = "select hijri_date from (select max(hijri_date) as hijri_date from eut_hijri_dates "
          + "where hijri_date >=? group by hijri_date order by hijri_date limit 90) "
          + "dual order by hijri_date desc limit 1";
      query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      query.setParameter(0, hijiridate);
      @SuppressWarnings("unchecked")
      List<Object> querylist = query.list();
      if (query != null && querylist.size() > 0) {
        Object row = querylist.get(0);
        EffectiveToDate = (String) row;
        EffectiveToDate = EffectiveToDate.substring(6, 8) + "-" + EffectiveToDate.substring(4, 6)
            + "-" + EffectiveToDate.substring(0, 4);

      } else {
        EffectiveToDate = effectiveFrom;
      }
      return EffectiveToDate;
    } catch (OBException e) {
      log.error("Exception while getDateforEffectiveTo:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
      // OBDal.getInstance().getSession().clear();
    }
  }

}
