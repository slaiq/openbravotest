package sa.elm.ob.scm.event.dao;

import java.math.BigDecimal;
import java.util.List;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.uom.UOM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.Escmbidconfiguration;
import sa.elm.ob.scm.Escmbiddates;
import sa.elm.ob.scm.Escmbidmgmtline;
import sa.elm.ob.scm.Escmbidsourceref;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * This class is used to handle dao activities of Bid Event.
 */
public class BidEventDAO {
  private static final Logger log = LoggerFactory.getLogger(BidEventDAO.class);

  /**
   * Get Bid Config count and validate bid type should be unique
   * 
   * @param bidconfiguration
   * @return configCount in int
   */
  public static int getBidConfigCount(Escmbidconfiguration bidconfiguration) {
    int configCount = 0;
    try {
      OBContext.setAdminMode();
      OBQuery<Escmbidconfiguration> bidconfig = OBDal.getInstance().createQuery(
          Escmbidconfiguration.class,
          " as e where e.organization.id='" + bidconfiguration.getOrganization().getId()
              + "' and e.bidType = '" + bidconfiguration.getBidType() + "'");
      configCount = bidconfig.list().size();
    } catch (OBException e) {
      log.error("Exception while getBidConfigCount:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return configCount;
  }

  /**
   * Update Bid Date Properties
   * 
   * @param bidId
   * @param bidDateId
   */
  public static void setBidDate(String bidId, String bidDateId) {
    try {
      OBContext.setAdminMode();
      OBQuery<Escmbiddates> objDates = OBDal.getInstance().createQuery(Escmbiddates.class,
          "as e where e.id <> '" + bidDateId + "' and e.escmBidmgmt.id='" + bidId + "'");
      objDates.setFilterOnActive(false);
      if (objDates.list().size() > 0) {
        for (Escmbiddates date : objDates.list()) {
          date.setActive(false);
          date.setApproved(true);
          OBDal.getInstance().save(date);
        }
      }
    } catch (OBException e) {
      log.error("Exception while setBidDate:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Get Bid SoucRef qty and validate with bid line qty
   * 
   * @param bidmgmtLineId
   * @return list
   */
  public static List<Escmbidsourceref> getSourRefList(String bidmgmtLineId) {
    try {
      OBContext.setAdminMode();
      OBQuery<Escmbidsourceref> ref = OBDal.getInstance().createQuery(Escmbidsourceref.class,
          " as e where e.escmBidmgmtLine.id='" + bidmgmtLineId + "'");
      log.debug("list:" + ref.list().size());
      return ref.list();
    } catch (OBException e) {
      log.error("Exception while getSourRefList:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * check Bid Sour Ref count to add qty
   * 
   * @param bidMgmtLineId
   * @return srfCount in int
   */
  public static int getBidSrfCount(String bidMgmtLineId) {
    int srfCount = 0;
    try {
      OBContext.setAdminMode();
      OBQuery<Escmbidsourceref> manualRef = OBDal.getInstance().createQuery(Escmbidsourceref.class,
          " as e where e.escmBidmgmtLine.id='" + bidMgmtLineId + "' and e.requisitionLine=null ");
      manualRef.setMaxResult(1);
      srfCount = manualRef.list().size();
    } catch (OBException e) {
      log.error("Exception while getBidSrfCount:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return srfCount;
  }

  /**
   * Set Bid Sour Ref properties
   * 
   * @param bidmgmtline
   * @param existinSourceLine
   * @return Object
   */
  public static Object setSourRef(Escmbidmgmtline bidmgmtline, Long existinSourceLine) {
    try {
      OBContext.setAdminMode();
      final Escmbidsourceref srcref = OBProvider.getInstance().get(Escmbidsourceref.class);

      srcref.setEscmBidmgmtLine(bidmgmtline);
      srcref.setReservedQuantity(bidmgmtline.getMovementQuantity());
      srcref.setLineNo(existinSourceLine);
      return srcref;
    } catch (OBException e) {
      log.error("Exception while setSourRef:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Get Parent line count and validate it that cannot be changed
   * 
   * @param oldParentId
   * @return lineCount in int
   */
  public static int getBidMgmtLineCount(String oldParentId) {
    int lineCount = 0;
    try {
      OBContext.setAdminMode();
      final OBQuery<Escmbidmgmtline> EscmbidmgmtlineList = OBDal.getInstance().createQuery(
          Escmbidmgmtline.class, "parentline.id='" + oldParentId + "'");
      lineCount = EscmbidmgmtlineList.list().size();
    } catch (OBException e) {
      log.error("Exception while getBidMgmtLineCount:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return lineCount;
  }

  /**
   * Set summary level if the line is parent
   * 
   * @param bidMgmtLineParentId
   */
  public static void setBidLineParent(String bidMgmtLineParentId) {
    try {
      OBContext.setAdminMode();
      Escmbidmgmtline bidLine = Utility.getObject(Escmbidmgmtline.class, bidMgmtLineParentId);
      bidLine.setSummarylevel(true);
      bidLine.setMovementQuantity(new BigDecimal(1));

      // get uom with edicode 'ea'
      final OBQuery<UOM> uomList = OBDal.getInstance().createQuery(UOM.class, "eDICode='EA'");

      if (uomList.list().size() > 0) {
        bidLine.setUOM(uomList.list().get(0));
      }
      // while updating the quantity in parent , update the source reference qty also to 1
      OBQuery<Escmbidsourceref> ref = OBDal.getInstance().createQuery(Escmbidsourceref.class,
          " as e where e.escmBidmgmtLine.id='" + bidLine.getId() + "'");
      List<Escmbidsourceref> sourceRefList = ref.list();
      log.debug("list:" + sourceRefList.size());
      if (sourceRefList.size() > 0) {
        Escmbidsourceref reference = sourceRefList.get(0);
        reference.setReservedQuantity(new BigDecimal("1"));
        OBDal.getInstance().save(reference);
      }
      OBDal.getInstance().save(bidLine);
    } catch (OBException e) {
      log.error("Exception while getBidMgmtLineCount:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Get parent line count to set summary level
   * 
   * @param bidmgmtline
   * @return lineCount in int
   */
  public static int getBidMgmtParentLineCount(Escmbidmgmtline bidmgmtline) {
    int lineCount = 0;
    try {
      OBContext.setAdminMode();
      OBQuery<Escmbidmgmtline> chkLineExists = OBDal.getInstance().createQuery(
          Escmbidmgmtline.class,
          "as e where e.escmBidmgmt.id = '" + bidmgmtline.getEscmBidmgmt().getId()
              + "' and e.parentline.id ='" + bidmgmtline.getParentline().getId() + "'");
      chkLineExists.setMaxResult(1);
      lineCount = chkLineExists.list().size();
    } catch (OBException e) {
      log.error("Exception while getBidMgmtLineCount:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return lineCount;
  }
}
