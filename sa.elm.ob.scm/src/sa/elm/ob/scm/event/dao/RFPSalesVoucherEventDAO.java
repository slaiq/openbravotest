package sa.elm.ob.scm.event.dao;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.Escmsalesvoucher;

/**
 * 
 * This class is used to handle dao activities of Salesvoucher Event.
 */
public class RFPSalesVoucherEventDAO {
  private static final Logger log = LoggerFactory.getLogger(RFPSalesVoucherEventDAO.class);

  /**
   * Check old combination of (Bid,Supplier) already used in proposal.
   * 
   * @param salesVoucher
   * @return true if exists
   */
  public static boolean checkRFPExists(Escmsalesvoucher salesVoucher, String supplier) {
    try {
      OBContext.setAdminMode();
      OBQuery<EscmProposalMgmt> proposal = OBDal.getInstance().createQuery(EscmProposalMgmt.class,
          " escmBidmgmt.id=:bidId and supplier.id=:supplierno");
      proposal.setNamedParameter("bidId", salesVoucher.getEscmBidmgmt().getId());
      proposal.setNamedParameter("supplierno", supplier);
      if (proposal.list() != null && proposal.list().size() > 0) {
        return true;
      }
      return false;
    } catch (OBException e) {
      log.error("Exception while checking rfp already used in proposal:", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}