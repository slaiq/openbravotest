package sa.elm.ob.scm.event.dao;

import org.hibernate.Query;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.procurement.Requisition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * This class is used to handle dao activities of Purchase Requisition Header Event.
 */
public class RequisitionEventDAO {
  private static final Logger log = LoggerFactory.getLogger(RFPSalesVoucherEventDAO.class);

  /**
   * 
   * @param req
   * @return PRReqDoc
   */

  public static String updatePRReqDoc(Requisition req) {
    String strquery = null;
    String PRReqDoc = null;
    Query query = null;
    try {
      OBContext.setAdminMode();
      String type = null;
      if (req.getEscmProcesstype().equals("PB")) {
        type = "TR";
      } else if (req.getEscmProcesstype().equals("LB")) {
        type = "LD";
      }
      strquery = "select string_agg(lkpln.name, ', ') as lkpsname "
          + " from escm_prrequireddoc_hdr reqhdr "
          + " left join escm_prrequireddoc_lns reqln on reqhdr.escm_prrequireddoc_hdr_id=reqln.escm_prrequireddoc_hdr_id "
          + " left join escm_deflookups_typeln lkpln on reqln.escm_deflookups_typeln_id=lkpln.escm_deflookups_typeln_id "
          + " where reqhdr.isactive='Y' and reqhdr.ad_client_id=:clientID and reqhdr.process_type=:processType ";

      query = OBDal.getInstance().getSession().createSQLQuery(strquery);
      query.setParameter("clientID", req.getClient().getId());
      query.setParameter("processType", type);
      if (query != null && query.list().size() > 0) {
        log.debug("geto" + query.list().get(0));
        Object row = query.list().get(0);
        PRReqDoc = (String) row;
        return PRReqDoc;
      }
    } catch (Exception e) {
      log.debug("exception while updatePRReqDoc" + e);
    } finally {
      OBContext.restorePreviousMode();
      // OBDal.getInstance().getSession().clear();
    }
    return PRReqDoc;
  }
}
