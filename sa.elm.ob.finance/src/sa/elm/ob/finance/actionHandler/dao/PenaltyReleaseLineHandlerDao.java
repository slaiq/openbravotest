package sa.elm.ob.finance.actionHandler.dao;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinPenaltyAction;
import sa.elm.ob.finance.EfinPenaltyHeader;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.ad_process.RDVProcess.DAO.PenaltyActionDAO;

/**
 * 
 * @author Gowtham.V
 *
 */
public class PenaltyReleaseLineHandlerDao {
  private static final Logger LOG = LoggerFactory.getLogger(PenaltyReleaseLineHandlerDao.class);

  /**
   * This method is used to insert lines in penalty through add line process.
   * 
   * @param selectedlines
   * @param penaltyLineId
   * @return 1,0
   */
  public static int insertPenaltyLines(BigDecimal penaltyAmt, String penaltyLineId,
      String newTxnLineId, long lineNo, String type, String txnId) {
    try {
      OBContext.setAdminMode();
      EfinPenaltyAction penaltyOld = OBDal.getInstance().get(EfinPenaltyAction.class,
          penaltyLineId);
      EfinRDVTxnline txnLine = null;
      Long seqno = null;
      if (type.equals("VER")) {
        seqno = penaltyOld.getEfinRdvtxnline().getTrxlnNo();
        OBQuery<EfinRDVTxnline> txnline = OBDal.getInstance().createQuery(EfinRDVTxnline.class,
            "efinRdvtxn.id=:rdvtxnID and trxlnNo=:trxlnNo");
        txnline.setNamedParameter("rdvtxnID", txnId);
        txnline.setNamedParameter("trxlnNo", seqno);
        List<EfinRDVTxnline> txnlineList = txnline.list();
        if (txnlineList != null && txnlineList.size() > 0) {
          txnLine = txnlineList.get(0);
        }
        OBDal.getInstance().save(txnLine);
      } else {
        txnLine = OBDal.getInstance().get(EfinRDVTxnline.class, newTxnLineId);
      }
      // chk already have penalty header.
      /*
       * EfinRDVTxnline txnLine = OBDal.getInstance().get(EfinRDVTxnline.class, newTxnLineId); if
       * (txnLine.getEfinPenaltyHeaderList() != null && txnLine.getEfinPenaltyHeaderList().size() >
       * 0) {
       */

      // set match.
      txnLine.setMatch(true);
      OBDal.getInstance().save(txnLine);

      // insert child alone
      EfinPenaltyAction action = OBProvider.getInstance().get(EfinPenaltyAction.class);
      action.setClient(penaltyOld.getClient());
      action.setSequenceNumber(lineNo);
      action.setTRXAppNo(txnLine.getTrxappNo());
      action.setAction("RM");
      action.setActionDate(new Date());
      action.setAmount(txnLine.getMatchAmt());
      action.setEfinPenaltyTypes(penaltyOld.getEfinPenaltyTypes());
      action.setPenaltyPercentage(penaltyOld.getPenaltyPercentage());
      action.setPenaltyAmount(penaltyAmt.negate());
      action.setActionReason("Release");
      action.setActionJustification("");
      action.setBusinessPartner(penaltyOld.getBusinessPartner());
      action.setName(penaltyOld.getName());
      action.setEfinRdvtxnline(txnLine);
      action.setFreezePenalty(penaltyOld.isFreezePenalty());
      action.setPenaltyAccountType(penaltyOld.getPenaltyAccountType());
      action.setPenaltyUniquecode(penaltyOld.getPenaltyUniquecode());
      action.setPenaltyRel(penaltyOld);
      OBDal.getInstance().save(action);

      OBDal.getInstance().flush();

      // update release amount in old penalty action
      penaltyOld.setReleasedamt(penaltyOld.getReleasedamt().add(penaltyAmt));
      if (penaltyOld.getReleasedamt().compareTo(penaltyOld.getPenaltyAmount()) == 0)
        penaltyOld.setReleased(true);
      OBDal.getInstance().save(penaltyOld);

      insertPenaltyReleaseHeader(action, action.getEfinRdvtxnline(), action.getPenaltyAmount());

      /*
       * } else { // insert parent and child both
       * 
       * }
       */

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception while inserting lines in penalty release process : ", e);
      }
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return 1;
  }

  /**
   * Insert penalty header and calculate penalty amt.
   * 
   * @param penaltyaction
   * @param rdvtrxln
   * @param penaltyamt
   */
  public static void insertPenaltyReleaseHeader(EfinPenaltyAction penaltyaction,
      EfinRDVTxnline rdvtrxln, BigDecimal penaltyamt) {
    EfinRDVTransaction previousrdvtrx = null;
    EfinRDVTxnline previousrdxtrxln = null;
    BigDecimal prevpenhdAmt = BigDecimal.ZERO;
    JSONObject result = new JSONObject();
    try {

      // get json object of penalty amount on each penalty types based on rdv header and penalty
      // type
      result = PenaltyActionDAO.getPenaltyAmt(penaltyaction.getEfinRdvtxnline(), null,
          penaltyaction);

      // get previous rdv transaction based on current rdv transaction line , created desc , not
      // inculding current version id , limit 1
      OBQuery<EfinRDVTransaction> rdvtrx = OBDal.getInstance().createQuery(EfinRDVTransaction.class,
          " as e where e.id <> :rdvtrxID and e.efinRdv.id=:rdvID and e.tXNVersion < :tXNVersion order by created desc   ");
      rdvtrx.setNamedParameter("rdvtrxID", rdvtrxln.getEfinRdvtxn().getId());
      rdvtrx.setNamedParameter("rdvID", rdvtrxln.getEfinRdvtxn().getEfinRdv().getId());
      rdvtrx.setNamedParameter("tXNVersion", rdvtrxln.getEfinRdvtxn().getTXNVersion());
      rdvtrx.setMaxResult(1);
      if (rdvtrx.list().size() > 0) {
        previousrdvtrx = rdvtrx.list().get(0);
      }

      // get previous rdv transaction line based on previous trx version and current rdv line
      // product id
      if (previousrdvtrx != null && rdvtrxln.getProduct() != null) {
        OBQuery<EfinRDVTxnline> prerdvtrxln = OBDal.getInstance().createQuery(EfinRDVTxnline.class,
            " as e where e.efinRdvtxn.id=:RdvtxnID and e.trxlnNo=:trxlnNo ");// and
        // e.product.id='"+rdvtrxln.getProduct().getId()+"'
        prerdvtrxln.setNamedParameter("RdvtxnID", previousrdvtrx.getId());
        prerdvtrxln.setNamedParameter("trxlnNo", rdvtrxln.getTrxlnNo());
        prerdvtrxln.setMaxResult(1);
        if (prerdvtrxln.list().size() > 0) {
          previousrdxtrxln = prerdvtrxln.list().get(0);
        }
      }
      // based on previous rdv transaction and rdv transaction line , get the penalty header prvious
      // penalty amount values
      if (previousrdvtrx != null && previousrdxtrxln != null) {
        OBQuery<EfinPenaltyHeader> prevpenhd = OBDal.getInstance().createQuery(
            EfinPenaltyHeader.class,
            " as e where e.efinRdvtxnline.id=:RdvtxnlineID and e.efinRdvtxn.id=:RdvtxnID ");
        prevpenhd.setNamedParameter("RdvtxnlineID", previousrdxtrxln.getId());
        prevpenhd.setNamedParameter("RdvtxnID", previousrdvtrx.getId());
        prevpenhd.setMaxResult(1);
        if (prevpenhd.list().size() > 0) {
          prevpenhdAmt = prevpenhd.list().get(0).getUpdatedPenaltyAmt();
        } else
          prevpenhdAmt = BigDecimal.ZERO;
      }

      // check if penalty header exists or not based on current rdv transaction line and rdv
      // transaction id
      OBQuery<EfinPenaltyHeader> penhdexisting = OBDal.getInstance().createQuery(
          EfinPenaltyHeader.class,
          " as e where e.efinRdvtxnline.id=:RdvtxnlineID and e.efinRdvtxn.id=:RdvtxnID ");
      penhdexisting.setNamedParameter("RdvtxnlineID", rdvtrxln.getId());
      penhdexisting.setNamedParameter("RdvtxnID", rdvtrxln.getEfinRdvtxn().getId());
      penhdexisting.setMaxResult(1);
      // if exists penalty header update penalty amount and update penalty amount
      if (penhdexisting.list().size() > 0) {
        // if difference of old penalty amount and new penalty amunt not zero or action changed, or
        // penalty type changed
        if (penaltyamt.compareTo(BigDecimal.ZERO) != 0) {

          EfinPenaltyHeader penltyhd = penhdexisting.list().get(0);
          // get penalty header amount
          penaltyamt = PenaltyActionDAO.getPenaltyheaderAmt(penaltyaction.getEfinRdvtxnline());
          penltyhd.setPenaltyAmount(penaltyamt);
          penltyhd.setUpdatedPenaltyAmt(prevpenhdAmt.add(penaltyamt));
          OBDal.getInstance().save(penltyhd);
          penaltyaction.setEfinPenaltyHeader(penltyhd);
          OBDal.getInstance().save(penaltyaction);
          OBDal.getInstance().flush();

          PenaltyActionDAO.updatePenalty(result, penaltyaction);
        }
      } else {
        EfinPenaltyHeader penltyhd = OBProvider.getInstance().get(EfinPenaltyHeader.class);
        penltyhd.setClient(penaltyaction.getClient());
        penltyhd.setOrganization(penaltyaction.getOrganization());
        penltyhd.setLineNo(penaltyaction.getEfinRdvtxnline().getTrxlnNo());
        penltyhd.setEfinRdvtxn(rdvtrxln.getEfinRdvtxn());
        penltyhd.setEfinRdvtxnline(rdvtrxln);
        penltyhd.setExistingPenalty(prevpenhdAmt);
        penltyhd.setPenaltyAmount(penaltyamt);
        penltyhd
            .setUpdatedPenaltyAmt(penltyhd.getExistingPenalty().add(penltyhd.getPenaltyAmount()));
        OBDal.getInstance().save(penltyhd);
        OBDal.getInstance().flush();
        penaltyaction.setEfinPenaltyHeader(penltyhd);
        OBDal.getInstance().save(penaltyaction);
        if (result != null) {
          PenaltyActionDAO.updatePenalty(result, penaltyaction);
        }
      }

    } catch (final Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in PenaltyAction", e);
      }
      e.printStackTrace();
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
    }
  }

}
