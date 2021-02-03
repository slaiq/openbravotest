package sa.elm.ob.scm.ad_callouts.dao;

import java.util.List;

import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenEnvelopeDAO {
  private static final Logger log = LoggerFactory.getLogger(OpenEnvelopeDAO.class);

  /**
   * Get Maximum open envelope day from Bid.
   * 
   * @param strBidID
   * @return OpenEnvelopday
   */
  public static String getBidOpenenvelopday(String strBidID) {
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
        @SuppressWarnings("unchecked")
        List<Object> queryList = query.list();
        if (queryList.size() > 0) {
          Object row = queryList.get(0);
          openenvelopday = (String) row;
        }
      }
      return openenvelopday;

    } catch (OBException e) {
      log.error("Exception while getBidOpenenvelopday" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
