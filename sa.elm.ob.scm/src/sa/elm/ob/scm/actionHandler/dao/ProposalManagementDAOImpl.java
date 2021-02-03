package sa.elm.ob.scm.actionHandler.dao;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;

import sa.elm.ob.scm.EscmProposalmgmtHist;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalsourceRef;

public class ProposalManagementDAOImpl implements ProposalManagementDAO {
  private static final Logger log = Logger.getLogger(ProposalManagementDAOImpl.class);

  /**
   * chk line already presented or not based on product id
   * 
   * @param strProposalId
   * @param productId
   * @return list
   */
  public List<EscmProposalmgmtLine> checkProductExistById(String strProposalId, String productId,
      String reqline) {
    try {
      OBContext.setAdminMode();
      List<EscmProposalmgmtLine> chklineexistQryList = new ArrayList<EscmProposalmgmtLine>();
      OBQuery<EscmProposalmgmtLine> chklineexistQry = OBDal.getInstance().createQuery(
          EscmProposalmgmtLine.class,
          "as e where e.escmProposalmgmt.id=:proposalId and e.product.id=:productId");
      chklineexistQry.setNamedParameter("proposalId", strProposalId);
      chklineexistQry.setNamedParameter("productId", productId);
      // chklineexistQry.setMaxResult(1);
      if (chklineexistQry != null && chklineexistQry.list().size() > 0) {
        for (EscmProposalmgmtLine ln : chklineexistQry.list()) {
          if (ln.getEscmProposalsourceRefList().get(0).getRequisitionLine() != null && ln
              .getEscmProposalsourceRefList().get(0).getRequisitionLine().getId().equals(reqline)) {
            chklineexistQryList = java.util.Arrays.asList(ln);
          }
        }
      }
      return chklineexistQryList;
    } catch (OBException e) {
      log.error("Exception while checkProductExistById:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * chk line already presented or not based on product name
   * 
   * @param strProposalId
   * @param desc
   * @return list
   */
  public List<EscmProposalmgmtLine> checkProductExistByName(String strProposalId, String desc,
      String reqline) {
    try {
      OBContext.setAdminMode();
      List<EscmProposalmgmtLine> chklinedescexistQryList = new ArrayList<EscmProposalmgmtLine>();
      OBQuery<EscmProposalmgmtLine> chklinedescexistQry = OBDal.getInstance().createQuery(
          EscmProposalmgmtLine.class,
          "as e where  e.escmProposalmgmt.id=:proposalId and e.description=:desc ");
      chklinedescexistQry.setNamedParameter("proposalId", strProposalId);
      chklinedescexistQry.setNamedParameter("desc", desc);
      // chklinedescexistQry.setMaxResult(1);
      if (chklinedescexistQry != null && chklinedescexistQry.list().size() > 0) {
        for (EscmProposalmgmtLine ln : chklinedescexistQry.list()) {
          if (ln.getEscmProposalsourceRefList().get(0).getRequisitionLine() != null && ln
              .getEscmProposalsourceRefList().get(0).getRequisitionLine().getId().equals(reqline)) {
            chklinedescexistQryList = java.util.Arrays.asList(ln);
          }
        }
      }
      return chklinedescexistQryList;
    } catch (OBException e) {
      log.error("Exception while checkProductExistByName:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get proposal source ref lines
   * 
   * @param reqLineId
   * @param proposalLineId
   * @return list
   */
  public List<EscmProposalsourceRef> getSourceRefLines(String reqLineId, String proposalLineId) {
    try {
      OBContext.setAdminMode();

      OBQuery<EscmProposalsourceRef> srcrefline = OBDal.getInstance().createQuery(
          EscmProposalsourceRef.class, "as e where e.escmProposalmgmtLine.id=:proposalLineId"
              + "and e.requisitionLine.id =:proposalLineId");
      srcrefline.setNamedParameter("requisitionLineId", reqLineId);
      srcrefline.setNamedParameter("proposalLineId", proposalLineId);
      srcrefline.setMaxResult(1);
      return srcrefline.list();
    } catch (OBException e) {
      log.error("Exception while getSourceRefLines:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get proposal history
   * 
   * @param propId
   * 
   * @return list
   */
  public List<EscmProposalmgmtHist> getProposalHist(String propId) {
    try {
      OBContext.setAdminMode();
      OBQuery<EscmProposalmgmtHist> history = OBDal.getInstance().createQuery(
          EscmProposalmgmtHist.class,
          " as e where e.escmProposalmgmt.id=:proposalID order by e.creationDate desc ");
      history.setNamedParameter("proposalID", propId);
      history.setMaxResult(1);
      return history.list();
    } catch (OBException e) {
      log.error("Exception while getProposalHist:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * check lines added from PR
   * 
   * @param propLnId
   * 
   * @return list
   */
  public List<EscmProposalsourceRef> checkLinesAddedFromPR(String propLnId) {
    try {
      OBContext.setAdminMode();
      OBQuery<EscmProposalsourceRef> proposalsrcref = OBDal.getInstance().createQuery(
          EscmProposalsourceRef.class,
          "escmProposalmgmtLine.id=:proposalLnId and requisitionLine.id is not null");
      proposalsrcref.setNamedParameter("proposalLnId", propLnId);
      return proposalsrcref.list();
    } catch (OBException e) {
      log.error("Exception while checkLinesAddedFromPR:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
