package sa.elm.ob.finance.actionHandler.RdvHoldRelease;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.EfinRdvHold;
import sa.elm.ob.finance.EfinRdvHoldAction;
import sa.elm.ob.finance.EfinRdvHoldHeader;
import sa.elm.ob.finance.EfinRdvHoldTypes;

public class HoldReleaseLineHandlerDAOImpl implements HoldReleaseLineHandlerDAO {
  private static final Logger LOG = LoggerFactory.getLogger(HoldReleaseLineHandlerDAOImpl.class);
  private static final String VERSION = "VER";
  private static final String REMOVE = "RM";

  public int insertHoldLines(BigDecimal holdAmt, String holdLineId, String newTxnLineId,
      long lineNo, String type, String txnId) {

    try {
      OBContext.setAdminMode();
      EfinRdvHoldAction holdobj = OBDal.getInstance().get(EfinRdvHoldAction.class, holdLineId);
      EfinRDVTxnline txnLine = null;
      List<EfinRDVTxnline> txnlineList = null;
      Long seqno = null;

      if (type.equals(VERSION)) {
        seqno = holdobj.getEfinRdvtxnline().getTrxlnNo();

        OBQuery<EfinRDVTxnline> txnline = OBDal.getInstance().createQuery(EfinRDVTxnline.class,
            "efinRdvtxn.id=:rdvtxnID and trxlnNo=:trxnlineNo");
        txnline.setNamedParameter("rdvtxnID", txnId);
        txnline.setNamedParameter("trxnlineNo", seqno);
        txnlineList = txnline.list();

        if (txnlineList != null && txnlineList.size() > 0) {
          txnLine = txnlineList.get(0);
        }
        OBDal.getInstance().save(txnLine);
      } else {
        txnLine = OBDal.getInstance().get(EfinRDVTxnline.class, newTxnLineId);
      }

      // set match.
      txnLine.setMatch(true);
      OBDal.getInstance().save(txnLine);

      // insert child alone
      EfinRdvHoldAction action = OBProvider.getInstance().get(EfinRdvHoldAction.class);
      action.setClient(holdobj.getClient());
      action.setSequenceNumber(lineNo);
      action.setTxnApplicationNo(txnLine.getTrxappNo());
      action.setAction(REMOVE);
      action.setActionDate(new Date());
      action.setAmount(txnLine.getMatchAmt());
      action.setEfinRdvHoldTypes(holdobj.getEfinRdvHoldTypes());
      action.setRDVHoldPercentage(holdobj.getRDVHoldPercentage());
      action.setRDVHoldAmount(holdAmt.negate());
      action.setActionReason("Release");
      action.setActionJustification("");
      action.setBusinessPartner(holdobj.getBusinessPartner());
      action.setName(holdobj.getName());
      action.setEfinRdvtxnline(txnLine);
      action.setFreezeRdvHold(holdobj.isFreezeRdvHold());
      action.setRDVHoldAccountType(holdobj.getRDVHoldAccountType());
      action.setRDVHoldUniquecode(holdobj.getRDVHoldUniquecode());
      action.setRDVHoldRel(holdobj);
      OBDal.getInstance().save(action);

      OBDal.getInstance().flush();

      // update release amount in old hold action
      holdobj.setReleasedAmount(holdobj.getReleasedAmount().add(holdAmt));
      if (holdobj.getReleasedAmount().compareTo(holdobj.getRDVHoldAmount()) == 0)
        holdobj.setReleased(true);
      OBDal.getInstance().save(holdobj);

      insertHoldReleaseHeader(action, action.getEfinRdvtxnline(), action.getRDVHoldAmount());

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception while inserting lines in hold release process : ", e);
      }
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return 1;
  }

  public void insertHoldReleaseHeader(EfinRdvHoldAction holdaction, EfinRDVTxnline rdvtrxln,
      BigDecimal rdvholdAmt) {

    EfinRDVTransaction previousrdvtrx = null;
    EfinRDVTxnline previousrdxtrxln = null;
    BigDecimal prevholdhdAmt = BigDecimal.ZERO;
    BigDecimal holdAmount = rdvholdAmt;
    EfinRdvHoldHeader existingHoldHeader = null;
    JSONObject result = new JSONObject();

    try {

      // get hold amount on each hold types based on RDV header and hold type
      result = getHoldAmt(holdaction.getEfinRdvtxnline(), null, holdaction);

      previousrdvtrx = getPrevRdvTransaction(rdvtrxln);

      if (previousrdvtrx != null && rdvtrxln.getProduct() != null) {
        previousrdxtrxln = getPrevRdvTransactionLine(previousrdvtrx.getId(), rdvtrxln);
      }

      if (previousrdvtrx != null && previousrdxtrxln != null) {
        prevholdhdAmt = getHeaderPrevHoldAmount(previousrdvtrx, previousrdxtrxln);
      }

      existingHoldHeader = getrdvHoldheader(rdvtrxln);

      // if exists hold header then update hold amount
      if (existingHoldHeader != null && holdAmount.compareTo(BigDecimal.ZERO) != 0) {

        EfinRdvHoldHeader holdheader = existingHoldHeader;

        holdAmount = getHoldheaderAmt(holdaction.getEfinRdvtxnline());
        holdheader.setRDVHoldAmount(holdAmount);
        holdheader.setUpdatedRdvHoldAmt(prevholdhdAmt.add(holdAmount));
        OBDal.getInstance().save(holdheader);
        holdaction.setEfinRdvHoldHeader(holdheader);
        OBDal.getInstance().save(holdaction);
        OBDal.getInstance().flush();

        updateHold(result, holdaction);

      } else {
        EfinRdvHoldHeader holdheader = OBProvider.getInstance().get(EfinRdvHoldHeader.class);
        holdheader.setClient(holdaction.getClient());
        holdheader.setOrganization(holdaction.getOrganization());
        holdheader.setLineNo(holdaction.getEfinRdvtxnline().getTrxlnNo());
        holdheader.setEfinRdvtxn(rdvtrxln.getEfinRdvtxn());
        holdheader.setEfinRdvtxnline(rdvtrxln);
        holdheader.setExistingRdvHold(prevholdhdAmt);
        holdheader.setRDVHoldAmount(holdAmount);
        holdheader.setUpdatedRdvHoldAmt(
            holdheader.getExistingRdvHold().add(holdheader.getRDVHoldAmount()));
        OBDal.getInstance().save(holdheader);
        OBDal.getInstance().flush();

        holdaction.setEfinRdvHoldHeader(holdheader);
        OBDal.getInstance().save(holdaction);

        if (result != null) {
          updateHold(result, holdaction);
        }
      }

    } catch (final Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in insertHoldReleaseHeader", e);
      }
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
    }

  }

  public EfinRDVTransaction getPrevRdvTransaction(EfinRDVTxnline rdvtrxln) {
    EfinRDVTransaction previousrdvtrx = null;

    try {
      OBQuery<EfinRDVTransaction> rdvtrx = OBDal.getInstance().createQuery(EfinRDVTransaction.class,
          " as e where e.id <>:rdvtrxId and e.efinRdv.id=:rdvId "
              + "and e.tXNVersion <:trxnversion order by created desc   ");
      rdvtrx.setNamedParameter("rdvtrxId", rdvtrxln.getEfinRdvtxn().getId());
      rdvtrx.setNamedParameter("rdvId", rdvtrxln.getEfinRdvtxn().getEfinRdv().getId());
      rdvtrx.setNamedParameter("trxnversion", rdvtrxln.getEfinRdvtxn().getTXNVersion());
      rdvtrx.setMaxResult(1);
      if (rdvtrx.list().size() > 0) {
        previousrdvtrx = rdvtrx.list().get(0);
      }
    } catch (Exception e) {
      LOG.error("Exception in getPrevRdvTransaction() " + e);
    }
    return previousrdvtrx;
  }

  @Override
  public EfinRDVTxnline getPrevRdvTransactionLine(String rdvtrxId, EfinRDVTxnline rdvtrxln) {
    EfinRDVTxnline previousrdxtrxln = null;

    try {
      if (!StringUtils.isEmpty(rdvtrxId) && rdvtrxln.getProduct() != null) {
        OBQuery<EfinRDVTxnline> prerdvtrxln = OBDal.getInstance().createQuery(EfinRDVTxnline.class,
            " as e where e.efinRdvtxn.id=:prevTransId and e.trxlnNo=:transLineNo");
        prerdvtrxln.setNamedParameter("prevTransId", rdvtrxId);
        prerdvtrxln.setNamedParameter("transLineNo", rdvtrxln.getTrxlnNo());
        prerdvtrxln.setMaxResult(1);
        if (prerdvtrxln.list().size() > 0) {
          previousrdxtrxln = prerdvtrxln.list().get(0);
        }
      }

    } catch (Exception e) {
      LOG.error("Exception in getPrevRdvTransactionLine() " + e);
    }
    return previousrdxtrxln;

  }

  @Override
  public BigDecimal getHeaderPrevHoldAmount(EfinRDVTransaction previousrdvtrx,
      EfinRDVTxnline previousrdxtrxln) {
    BigDecimal prevholdhdAmt = BigDecimal.ZERO;

    try {
      if (previousrdvtrx != null && previousrdxtrxln != null) {
        OBQuery<EfinRdvHoldHeader> prevpenhd = OBDal.getInstance().createQuery(
            EfinRdvHoldHeader.class,
            " as e where e.efinRdvtxnline.id=:prevTransLnId and e.efinRdvtxn.id=:prevrdvTransId ");
        prevpenhd.setNamedParameter("prevTransLnId", previousrdxtrxln.getId());
        prevpenhd.setNamedParameter("prevrdvTransId", previousrdvtrx.getId());
        prevpenhd.setMaxResult(1);
        if (prevpenhd.list().size() > 0) {
          prevholdhdAmt = prevpenhd.list().get(0).getUpdatedRdvHoldAmt();
        } else
          prevholdhdAmt = BigDecimal.ZERO;
      }
    } catch (Exception e) {
      LOG.error("Exception in getHeaderPrevHoldAmount() " + e);
    }
    return prevholdhdAmt;
  }

  @Override
  public EfinRdvHoldHeader getrdvHoldheader(EfinRDVTxnline rdvtrxln) {
    EfinRdvHoldHeader holdHeader = null;

    try {
      OBQuery<EfinRdvHoldHeader> penhdexisting = OBDal.getInstance().createQuery(
          EfinRdvHoldHeader.class,
          " as e where e.efinRdvtxnline.id=:rdvtransLnId and e.efinRdvtxn.id=:rdvtransId");
      penhdexisting.setNamedParameter("rdvtransLnId", rdvtrxln.getId());
      penhdexisting.setNamedParameter("rdvtransId", rdvtrxln.getEfinRdvtxn().getId());
      penhdexisting.setMaxResult(1);
      if (penhdexisting.list().size() > 0) {
        holdHeader = penhdexisting.list().get(0);
      }

    } catch (Exception e) {
      LOG.error("Exception in getrdvHoldheader() " + e);
      e.printStackTrace();
    }
    return holdHeader;
  }

  @SuppressWarnings("rawtypes")
  private BigDecimal getHoldheaderAmt(EfinRDVTxnline trxline) {
    String sqlqry = null;
    Query qry = null;
    BigDecimal calHoldheaderAmt = BigDecimal.ZERO;

    try {

      sqlqry = " select (coalesce(sum(act.RDV_Hold_Amount),0)) as totpenamt ,"
          + " act.efin_rdvtxnline_id from efin_rdv_hold_action act"
          + " where act.efin_rdvtxnline_id=  ? "
          + " group by act.efin_rdvtxnline_id,act.efin_rdv_hold_types_id ";
      qry = OBDal.getInstance().getSession().createSQLQuery(sqlqry);
      qry.setParameter(0, trxline.getId());

      LOG.debug("qry:" + qry);

      List getRdvHoldAmt = qry.list();

      if (getRdvHoldAmt != null && getRdvHoldAmt.size() > 0) {
        for (Object rdvHold : getRdvHoldAmt) {
          Object[] row = (Object[]) rdvHold;
          calHoldheaderAmt = calHoldheaderAmt.add(new BigDecimal(row[0].toString()));
        }
      }

    } catch (Exception e) {
      LOG.error("Exception in getHoldheaderAmt", e);
    }
    return calHoldheaderAmt;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public JSONObject getHoldAmt(EfinRDVTxnline trxline, String actiontype,
      EfinRdvHoldAction holdaction) {
    JSONObject result = new JSONObject(), json = null;
    JSONArray array = new JSONArray();
    String sqlqry = null;
    Query qry = null;
    BigDecimal reduceAmt = BigDecimal.ZERO;

    try {
      result.put("totalamount", "0");
      sqlqry = " select  act.efin_rdv_hold_types_id,  "
          + " SUM(CASE WHEN act.action='AD'  THEN RDV_Hold_Amount ELSE 0 END) + "
          + " SUM(CASE WHEN act.action='RM'  THEN RDV_Hold_Amount ELSE 0 END) as total "
          + " from efin_rdv_hold_action act   "
          + " join efin_rdvtxnline ln on ln.efin_rdvtxnline_id= act.efin_rdvtxnline_id  "
          + " join efin_rdvtxn trx on trx.efin_rdvtxn_id=ln.efin_rdvtxn_id  "
          + " join efin_rdv rdv on rdv.efin_rdv_id= trx.efin_rdv_id where  rdv.efin_rdv_id= ?  ";

      sqlqry += " group by act.efin_rdv_hold_types_id ";
      qry = OBDal.getInstance().getSession().createSQLQuery(sqlqry);
      LOG.debug("qry1" + qry);
      qry.setParameter(0, trxline.getEfinRdvtxn().getEfinRdv().getId());

      List getrdvHoldAmt = qry.list();
      if (getrdvHoldAmt != null && getrdvHoldAmt.size() > 0) {
        for (Object rdvHold : getrdvHoldAmt) {
          Object[] row = (Object[]) rdvHold;
          json = new JSONObject();
          if (row[0] != null) {
            json.put("holdtype", row[0].toString());
            if (actiontype != null && actiontype.equals("del")) {

              if (holdaction.getEfinRdvHoldTypes() != null
                  && holdaction.getEfinRdvHoldTypes().getId().equals(row[0].toString())) {

                reduceAmt = new BigDecimal(row[1].toString())
                    .subtract(holdaction.getRDVHoldAmount());
                json.put("amount", reduceAmt.toString());
              } else {
                json.put("amount", row[1].toString());
              }
            } else {
              json.put("amount", row[1].toString());
            }
            array.put(json);
          }
        }
        result.put("holdlist", array);
        result.put("totalamount", new BigDecimal(result.getString("totalamount"))
            .add(new BigDecimal(json.getString("amount"))));
      }
      LOG.debug("result;" + result);
    } catch (Exception e) {
      LOG.error("Exception in getHoldAmt", e);

    }
    return result;
  }

  public void updateHold(JSONObject result, EfinRdvHoldAction holdaction) {
    JSONObject json = null;

    try {
      if (result != null) {
        JSONArray array = result.getJSONArray("holdlist");
        for (int i = 0; i < array.length(); i++) {

          json = array.getJSONObject(i);
          BigDecimal holdApplied = new BigDecimal(json.getString("amount"));
          EfinRdvHoldTypes rdvHoldType = OBDal.getInstance().get(EfinRdvHoldTypes.class,
              json.getString("holdtype"));

          OBQuery<EfinRdvHold> uprdvholdQry = OBDal.getInstance().createQuery(EfinRdvHold.class,
              " as e where e.efinRdv.id in ( select e.efinRdv.id from Efin_RDVTxn e where e.id=:rdvtxnId) "
                  + " and e.rDVHoldType.id=:rdvHoldTypeId");
          uprdvholdQry.setNamedParameter("rdvtxnId",
              holdaction.getEfinRdvtxnline().getEfinRdvtxn().getId());
          uprdvholdQry.setNamedParameter("rdvHoldTypeId", rdvHoldType.getDeductionType().getId());
          uprdvholdQry.setMaxResult(1);

          LOG.debug("rdvHold:" + uprdvholdQry.getWhereAndOrderBy());

          if (uprdvholdQry.list().size() > 0) {
            EfinRdvHold upoldrdvhold = uprdvholdQry.list().get(0);
            upoldrdvhold.setRDVHoldApplied(holdApplied);
            if (!(upoldrdvhold.getOpeningHoldAmount().compareTo(BigDecimal.ZERO) == 0)) {
              upoldrdvhold
                  .setRDVHoldRemaining(upoldrdvhold.getOpeningHoldAmount().subtract(holdApplied));
            }
            OBDal.getInstance().save(upoldrdvhold);
          }
        }
      }

    } catch (final Exception e) {
      LOG.error("Exception in updateHold", e);
    } finally {

    }
  }

}
