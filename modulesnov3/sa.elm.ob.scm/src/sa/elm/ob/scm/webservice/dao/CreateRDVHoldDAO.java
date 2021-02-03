package sa.elm.ob.scm.webservice.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.BusinessPartner;

import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.EfinRdvHold;
import sa.elm.ob.finance.EfinRdvHoldAction;
import sa.elm.ob.finance.EfinRdvHoldHeader;
import sa.elm.ob.finance.EfinRdvHoldTypes;
import sa.elm.ob.scm.webservice.constants.WebserviceConstants;
import sa.elm.ob.scm.webservice.dto.PoReceiptHeaderDTO;
import sa.elm.ob.scm.webservice.dto.PoReceiptLineDTO;
import sa.elm.ob.scm.webservice.dto.RDVHoldDTO;
import sa.elm.ob.scm.webservice.exception.CreateReceiptException;
import sa.elm.ob.utility.util.Utility;

/**
 * This class is used to add RDV Hold for rdv transaction line
 * 
 * @author Sathishkumar.P
 *
 */

public class CreateRDVHoldDAO {

  private static Logger log4j = Logger.getLogger(CreateRDVHoldDAO.class);

  /**
   * 
   * 
   * @param seqno
   * @param trxappno
   * @param clientId
   * @param actiontype
   * @param actionDate
   * @param amount
   * @param penalty_type
   * @param penalty_per
   * @param penalty_amt
   * @param actreason
   * @param actionjus
   * @param bpartnerId
   * @param bpname
   * @param freezepenalty
   * @param invoice
   * @param amarsarfamount
   * @param RDVTrxlineId
   * @param penalty_account
   * @param uniquecode
   * @param isRdvSaveAction
   * @return
   * @throws Exception
   */

  public static String getHoldAction(Long seqno, String trxappno, String clientId,
      String actiontype, String amount, String penalty_type, String penalty_per, String penalty_amt,
      String actreason, String actionjus, String bpartnerId, String bpname, String freezepenalty,
      String invoice, String amarsarfamount, String RDVTrxlineId, String penalty_account,
      String uniquecode, Boolean isRdvSaveAction) throws Exception {

    String sql = "";
    Long number = seqno;

    try {
      OBContext.setAdminMode();
      EfinRDVTxnline rdvline = OBDal.getInstance().get(EfinRDVTxnline.class, RDVTrxlineId);

      // Create Hold action
      EfinRdvHoldAction action = OBProvider.getInstance().get(EfinRdvHoldAction.class);
      action.setClient(OBDal.getInstance().get(Client.class, clientId));
      action.setSequenceNumber(number);
      action.setTxnApplicationNo(rdvline.getTrxappNo());
      action.setAction(actiontype);
      action.setActionDate(new Date());
      if (!isRdvSaveAction)
        action.setAmount(rdvline.getMatchAmt());
      else {
        action.setAmount(new BigDecimal(amount));
      }
      action.setEfinRdvHoldTypes(OBDal.getInstance().get(EfinRdvHoldTypes.class, penalty_type));
      if (penalty_per != null && penalty_per != "")
        action.setRDVHoldPercentage(new BigDecimal(penalty_per));
      action.setRDVHoldAmount(new BigDecimal(penalty_amt));
      action.setActionReason(actreason);
      action.setActionJustification(actionjus);
      if (bpartnerId != null && bpartnerId != "")
        action.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, bpartnerId));
      action.setName(bpname);
      action.setEfinRdvtxnline(rdvline);
      if (freezepenalty.equals("Y")) {
        action.setFreezeRdvHold(true);
      } else {
        action.setFreezeRdvHold(false);
      }
      if (invoice != null) {
        action.setInvoice(
            OBDal.getInstance().get(org.openbravo.model.common.invoice.Invoice.class, invoice));
        action.setAmrasarfAmount(new BigDecimal(amarsarfamount));
      }
      OBDal.getInstance().save(action);
      OBDal.getInstance().flush();

      // insert hold header
      insertHoldHeader(action, action.getEfinRdvtxnline(), action.getRDVHoldAmount(), null, null);

    } catch (final Exception e) {
      log4j.error("Exception in RdvHoldActionDAO,getHoldAction", e);
      throw new Exception("Error while creating hold action" + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return sql;
  }

  @SuppressWarnings("unlikely-arg-type")
  public static void insertHoldHeader(EfinRdvHoldAction holdaction, EfinRDVTxnline rdvtrxln,
      BigDecimal holdamt, EfinRdvHoldTypes oldHoldTypeId, String oldAction) {
    EfinRDVTransaction previousrdvtrx = null;
    EfinRDVTxnline previousrdxtrxln = null;
    BigDecimal prevpenhdAmt = BigDecimal.ZERO;
    JSONObject result = new JSONObject();
    try {
      OBContext.setAdminMode();

      result = getHoldAmt(holdaction.getEfinRdvtxnline(), null, holdaction);

      OBQuery<EfinRDVTransaction> rdvtrx = OBDal.getInstance().createQuery(EfinRDVTransaction.class,
          " as e where e.id <>'" + rdvtrxln.getEfinRdvtxn().getId() + "'" + "  and e.efinRdv.id='"
              + rdvtrxln.getEfinRdvtxn().getEfinRdv().getId() + "' " + "and e.tXNVersion < '"
              + rdvtrxln.getEfinRdvtxn().getTXNVersion() + "' order by created desc   ");
      rdvtrx.setMaxResult(1);
      rdvtrx.setFilterOnReadableClients(false);
      rdvtrx.setFilterOnReadableOrganization(false);
      if (rdvtrx.list().size() > 0) {
        previousrdvtrx = rdvtrx.list().get(0);
      }

      if (previousrdvtrx != null
          && (rdvtrxln.getProduct() != null || rdvtrxln.getItemDesc() != null)) {
        OBQuery<EfinRDVTxnline> prerdvtrxln = OBDal.getInstance().createQuery(EfinRDVTxnline.class,
            " as e where e.efinRdvtxn.id='" + previousrdvtrx.getId() + "' and e.trxlnNo='"
                + rdvtrxln.getTrxlnNo() + "'");// and
                                               // e.product.id='"+rdvtrxln.getProduct().getId()+"'
        prerdvtrxln.setMaxResult(1);
        if (prerdvtrxln.list().size() > 0) {
          previousrdxtrxln = prerdvtrxln.list().get(0);
        }
      }

      if (previousrdvtrx != null && previousrdxtrxln != null) {
        OBQuery<EfinRdvHoldHeader> prevpenhd = OBDal.getInstance().createQuery(
            EfinRdvHoldHeader.class, " as e where e.efinRdvtxnline.id='" + previousrdxtrxln.getId()
                + "' and e.efinRdvtxn.id='" + previousrdvtrx.getId() + "' ");
        prevpenhd.setMaxResult(1);
        if (prevpenhd.list().size() > 0) {
          prevpenhdAmt = prevpenhd.list().get(0).getUpdatedRdvHoldAmt();
        } else
          prevpenhdAmt = BigDecimal.ZERO;
      }

      OBQuery<EfinRdvHoldHeader> holdhdexisting = OBDal.getInstance().createQuery(
          EfinRdvHoldHeader.class, " as e where e.efinRdvtxnline.id='" + rdvtrxln.getId()
              + "' and e.efinRdvtxn.id='" + rdvtrxln.getEfinRdvtxn().getId() + "'");
      holdhdexisting.setMaxResult(1);
      holdhdexisting.setFilterOnReadableClients(false);
      holdhdexisting.setFilterOnReadableOrganization(false);
      // if exists penalty header update penalty amount and update penalty amount
      if (holdhdexisting.list().size() > 0) {
        // if difference of old penalty amount and new penalty amunt not zero or action changed, or
        // penalty type changed
        if (holdamt.compareTo(BigDecimal.ZERO) != 0
            || (oldAction != null && !oldAction.equals(holdaction.getAction()))
            || (oldHoldTypeId != null
                && !oldHoldTypeId.equals(holdaction.getEfinRdvHoldTypes().getId()))) {

          EfinRdvHoldHeader holdhd = holdhdexisting.list().get(0);
          // get penalty header amount
          BigDecimal holdamount = getHoldheaderAmt(holdaction.getEfinRdvtxnline());
          log4j.debug("penaltyamt:" + holdamount);
          holdhd.setRDVHoldAmount(holdamount);
          holdhd.setUpdatedRdvHoldAmt(prevpenhdAmt.add(holdamount));
          OBDal.getInstance().save(holdhd);

          holdaction.setEfinRdvHoldHeader(holdhd);
          OBDal.getInstance().save(holdhd);

          updatePenalty(result, holdaction);
          OBDal.getInstance().flush();

        }
      } else {
        EfinRdvHoldHeader penltyhd = OBProvider.getInstance().get(EfinRdvHoldHeader.class);
        penltyhd.setClient(holdaction.getClient());
        penltyhd.setOrganization(holdaction.getOrganization());
        penltyhd.setLineNo(holdaction.getEfinRdvtxnline().getTrxlnNo());
        penltyhd.setEfinRdvtxn(rdvtrxln.getEfinRdvtxn());
        penltyhd.setEfinRdvtxnline(rdvtrxln);
        penltyhd.setExistingRdvHold(prevpenhdAmt);
        penltyhd.setRDVHoldAmount(holdamt);
        penltyhd
            .setUpdatedRdvHoldAmt(penltyhd.getExistingRdvHold().add(penltyhd.getRDVHoldAmount()));
        OBDal.getInstance().save(penltyhd);
        holdaction.setEfinRdvHoldHeader(penltyhd);
        OBDal.getInstance().save(holdaction);

        if (result != null) {
          updatePenalty(result, holdaction);
        }
        OBDal.getInstance().flush();
      }

    } catch (final Exception e) {
      e.printStackTrace();
      log4j.error("Exception in PenaltyAction", e);

    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @SuppressWarnings("rawtypes")
  public static JSONObject getHoldAmt(EfinRDVTxnline trxline, String actiontype,
      EfinRdvHoldAction holdaction) {
    JSONObject result = new JSONObject(), json = null;
    JSONArray array = new JSONArray();
    String sqlqry = null;
    Query qry = null;
    BigDecimal reduceAmt = BigDecimal.ZERO;
    try {
      result.put("totalamount", "0");
      sqlqry = "    select  act.efin_rdv_hold_types_id,  "
          + "               SUM(CASE WHEN act.action='AD'  THEN rdv_hold_amount ELSE 0 END) + "
          + "               SUM(CASE WHEN act.action='RM'  THEN rdv_hold_amount ELSE 0 END) as total "
          + "   from efin_rdv_hold_action act   "
          + "             join efin_rdvtxnline ln on ln.efin_rdvtxnline_id= act.efin_rdvtxnline_id  "
          + "            join efin_rdvtxn trx on trx.efin_rdvtxn_id=ln.efin_rdvtxn_id  "
          + "            join efin_rdv rdv on rdv.efin_rdv_id= trx.efin_rdv_id    where  rdv.efin_rdv_id= ?  ";

      sqlqry += "              group by act.efin_rdv_hold_types_id ";
      qry = OBDal.getInstance().getSession().createSQLQuery(sqlqry);
      log4j.debug("qry1" + qry);
      qry.setParameter(0, trxline.getEfinRdvtxn().getEfinRdv().getId());

      List getPenaltytyAmt = qry.list();
      if (getPenaltytyAmt != null && getPenaltytyAmt.size() > 0) {
        for (Iterator iterator = getPenaltytyAmt.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
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
      log4j.debug("result;" + result);
    } catch (Exception e) {
      log4j.error("Exception in getPenaltyAmt", e);

    }
    return result;
  }

  @SuppressWarnings("rawtypes")
  public static BigDecimal getHoldheaderAmt(EfinRDVTxnline trxline) {
    String sqlqry = null;
    Query qry = null;
    BigDecimal calHoldheaderAmt = BigDecimal.ZERO;
    try {

      sqlqry = "  select (coalesce(sum(act.rdv_hold_amount),0)) as totpenamt , "
          + " act.efin_rdvtxnline_id from efin_rdv_hold_action act"
          + "     where act.efin_rdvtxnline_id=  ?  "
          + "      group by act.efin_rdvtxnline_id,act.efin_rdv_hold_types_id ";
      qry = OBDal.getInstance().getSession().createSQLQuery(sqlqry);
      qry.setParameter(0, trxline.getId());
      log4j.debug("qry:" + qry);
      List getHoldAmt = qry.list();
      if (getHoldAmt != null && getHoldAmt.size() > 0) {
        for (Iterator iterator = getHoldAmt.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          calHoldheaderAmt = calHoldheaderAmt.add(new BigDecimal(row[0].toString()));
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in getHoldheaderAmt", e);

    }
    return calHoldheaderAmt;
  }

  public static void updatePenalty(JSONObject result, EfinRdvHoldAction holdaction)
      throws Exception {
    JSONObject json = null;
    try {
      if (result != null) {
        JSONArray array = result.getJSONArray("holdlist");
        for (int i = 0; i < array.length(); i++) {
          json = array.getJSONObject(i);
          BigDecimal holdApplied = new BigDecimal(json.getString("amount"));

          EfinRdvHoldTypes penaltytype = OBDal.getInstance().get(EfinRdvHoldTypes.class,
              json.getString("holdtype"));

          OBQuery<EfinRdvHold> updateHoldQry = OBDal.getInstance().createQuery(EfinRdvHold.class,
              " as e where e.efinRdv.id ='"
                  + holdaction.getEfinRdvtxnline().getEfinRdvtxn().getEfinRdv().getId() + "' "
                  + " and e.rDVHoldType.id='" + penaltytype.getDeductionType().getId() + "'");
          updateHoldQry.setMaxResult(1);
          updateHoldQry.setFilterOnActive(true);
          updateHoldQry.setFilterOnReadableOrganization(false);
          updateHoldQry.setFilterOnReadableClients(false);

          log4j.debug("penalty:" + updateHoldQry.getWhereAndOrderBy());
          if (updateHoldQry.list().size() > 0) {
            EfinRdvHold upoldpenalty = updateHoldQry.list().get(0);
            upoldpenalty.setRDVHoldApplied(new BigDecimal(json.getString("amount")));
            if (!(upoldpenalty.getOpeningHoldAmount().compareTo(BigDecimal.ZERO) == 0)) {
              upoldpenalty
                  .setRDVHoldRemaining(upoldpenalty.getOpeningHoldAmount().subtract(holdApplied));
            }
            OBDal.getInstance().save(upoldpenalty);

            if (holdApplied.compareTo(BigDecimal.ZERO) == 0) {
              deleteRdvHold(upoldpenalty.getId(), penaltytype, holdaction);
            }
          } else {
            insertRdvHold(holdaction, penaltytype, holdApplied);
          }
        }
        OBDal.getInstance().flush();
      }

    } catch (final Exception e) {
      log4j.error("Exception in updatePenalty", e);
      throw new Exception(e.getMessage());
    } finally {

    }
  }

  /**
   * Method to insert hold if hold does not exists based on RDV header
   * 
   * @param holdaction
   * @param holdType
   * @param holdApplied
   */
  public static void insertRdvHold(EfinRdvHoldAction holdaction, EfinRdvHoldTypes holdType,
      BigDecimal holdApplied) {
    try {
      BigDecimal threshold = BigDecimal.ZERO;
      BigDecimal openholdAmt = BigDecimal.ZERO;
      BigDecimal percent = new BigDecimal("0.01");

      EfinRDV rdv = holdaction.getEfinRdvtxnline().getEfinRdvtxn().getEfinRdv();
      if (holdType.getThreshold() != null) {
        threshold = holdType.getThreshold().multiply(percent);
      }
      openholdAmt = threshold.multiply(rdv.getContractAmt());

      // insert hold
      EfinRdvHold hold = OBProvider.getInstance().get(EfinRdvHold.class);
      hold.setClient(holdaction.getClient());
      hold.setOrganization(holdaction.getOrganization());
      hold.setEfinRdv(rdv);
      hold.setRDVHoldType(holdType.getDeductionType());
      hold.setAlertStatus(rdv.getPenaltyStatus());
      hold.setRDVHoldApplied(holdApplied);
      if (!(openholdAmt.compareTo(BigDecimal.ZERO) == 0)) {
        hold.setRDVHoldRemaining(openholdAmt.subtract(holdApplied));
      }
      hold.setRDVHoldPercentage(holdType.getThreshold());
      hold.setOpeningHoldAmount(openholdAmt);
      OBDal.getInstance().save(hold);
      OBDal.getInstance().flush();

    } catch (final Exception e) {
      log4j.error("Exception in insertRdvHold", e);
    }
  }

  /**
   * Method to delete Hold if the hold type is not used in other lines
   * 
   * @param oldholdId
   * @param holdType
   * @param holdaction
   */
  public static void deleteRdvHold(String oldholdId, EfinRdvHoldTypes holdType,
      EfinRdvHoldAction holdaction) {
    Boolean canRemove = Boolean.TRUE;
    try {
      EfinRDV rdv = holdaction.getEfinRdvtxnline().getEfinRdvtxn().getEfinRdv();
      OBQuery<EfinRdvHoldAction> holdAction = OBDal.getInstance().createQuery(
          EfinRdvHoldAction.class,
          " as e where e.efinRdvtxnline.efinRdv.id=:rdvId and e.efinRdvHoldTypes.id=:deductType "
              + "and e.efinRdvtxnline.id<>:currentlineId");
      holdAction.setNamedParameter("rdvId", rdv.getId());
      holdAction.setNamedParameter("deductType", holdType.getId());
      holdAction.setNamedParameter("currentlineId", holdaction.getEfinRdvtxnline().getId());

      if (holdAction.list().size() > 0) {
        canRemove = Boolean.FALSE;
      }
      if (canRemove) {
        EfinRdvHold hold = OBDal.getInstance().get(EfinRdvHold.class, oldholdId);
        OBDal.getInstance().remove(hold);
        OBDal.getInstance().flush();
      }
    } catch (final Exception e) {
      log4j.error("Exception in deleteRdvHold", e);
    }
  }

  public static Date convertGregorian(String hijridate) {
    log4j.debug("hi:" + hijridate);
    String gregDate = Utility.convertToGregorian(hijridate);
    log4j.debug("gregDate:" + gregDate);
    Date greDate = null;
    try {
      DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
      greDate = df1.parse(gregDate);
      log4j.debug("greDate:" + greDate);
    } catch (Exception e) {
      log4j.error("Exception creating multiple transactions from payments", e);

    }
    return greDate;

  }

  /**
   * This method is used to get the lines from Rdv transaction version
   * 
   * @param efin_Rdvtxn_ID
   * @return
   */

  public static List<EfinRDVTxnline> getTransactionLineList(String efin_Rdvtxn_ID) {

    try {
      final String query = " as e where e.efinRdvtxn.id =:txnId";

      OBQuery<EfinRDVTxnline> rdvLineQry = OBDal.getInstance().createQuery(EfinRDVTxnline.class,
          query);
      rdvLineQry.setNamedParameter("txnId", efin_Rdvtxn_ID);
      rdvLineQry.setFilterOnActive(true);
      rdvLineQry.setFilterOnReadableOrganization(false);
      rdvLineQry.setFilterOnReadableClients(false);

      return rdvLineQry.list();
    } catch (Exception e) {
      log4j.debug("Error while getting transaction line from RDV version" + e.getMessage());
      return null;
    }

  }

  /**
   * This method is used to get the rdvtxnLine based on orderline id
   * 
   * @param lineList
   * @param id
   * @return
   * @throws Exception
   */
  public static EfinRDVTxnline getTxnLineBasedOnId(List<EfinRDVTxnline> lineList, String id)
      throws Exception {

    try {

      Optional<EfinRDVTxnline> line = lineList.stream()
          .filter(a -> a.getSalesOrderLine() != null && a.getSalesOrderLine().getId().equals(id))
          .findFirst();

      if (line.isPresent()) {
        return line.get();
      }

    } catch (Exception e) {
      log4j.debug("Error while getting transaction line based on orderline Id " + e.getMessage());
      throw new Exception(e.getMessage());
    }
    return null;

  }

  public static String getHoldTxnAction(String seqno, String trxappno, String clientId,
      String actiontype, String actionDate, String amount, String penalty_type, String penalty_per,
      String hold_amt, String actreason, String actionjus, String bpartnerId, String bpname,
      String freezepenalty, String invoice, String amarsarfamount, String RDVTxnId,
      String penalty_account, String uniquecode, Boolean isRdvSaveAction) {
    String sql = "";
    Long number = Long.parseLong(seqno);

    try {
      OBContext.setAdminMode();

      // get matched lines
      EfinRDVTransaction transaction = OBDal.getInstance().get(EfinRDVTransaction.class, RDVTxnId);
      OBDal.getInstance().refresh(transaction);
      // BigDecimal totalMatch = transaction.getMatchAmt().subtract(transaction.getADVDeduct())
      // .subtract(transaction.getPenaltyAmt());
      BigDecimal totalMatch = transaction.getNetmatchAmt();
      List<EfinRDVTxnline> lineList = transaction.getEfinRDVTxnlineList();
      String ref = SequenceIdData.getUUID();
      for (EfinRDVTxnline line : lineList) {
        if (line.isMatch()) {
          // BigDecimal weigtage = (((line.getMatchAmt().subtract(line.getPenaltyAmt())
          // .subtract(line.getADVDeduct())).divide(totalMatch, 6, BigDecimal.ROUND_HALF_EVEN))
          // .multiply(new BigDecimal(hold_amt))).setScale(2, RoundingMode.HALF_UP);
          BigDecimal weigtage = (((line.getNetmatchAmt()).divide(totalMatch, 6,
              BigDecimal.ROUND_HALF_EVEN)).multiply(new BigDecimal(hold_amt))).setScale(2,
                  RoundingMode.HALF_UP);
          EfinRDVTxnline rdvline = line;
          EfinRdvHoldAction action = OBProvider.getInstance().get(EfinRdvHoldAction.class);
          action.setTxngroupref(ref);
          action.setClient(OBDal.getInstance().get(Client.class, clientId));
          action.setSequenceNumber(number);
          action.setTxnApplicationNo(transaction.getTXNVersion().toString());
          action.setAction(actiontype);
          if (actionDate != null && actionDate != "")
            action.setActionDate(convertGregorian(actionDate));
          if (!isRdvSaveAction)
            action.setAmount(rdvline.getMatchAmt());
          else {
            action.setAmount(new BigDecimal(amount));
          }
          action.setEfinRdvHoldTypes(OBDal.getInstance().get(EfinRdvHoldTypes.class, penalty_type));
          if (penalty_per != null && penalty_per != "")
            action.setRDVHoldPercentage(new BigDecimal(penalty_per));
          else
            action.setRDVHoldPercentage(new BigDecimal(0));
          action.setRDVHoldAmount(weigtage);
          action.setActionReason(actreason);
          action.setActionJustification(actionjus);
          if (bpartnerId != null && bpartnerId != "")
            action.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, bpartnerId));
          action.setName(bpname);
          action.setEfinRdvtxnline(rdvline);
          if (freezepenalty.equals("Y")) {
            action.setFreezeRdvHold(true);
          } else {
            action.setFreezeRdvHold(false);
          }
          if (invoice != null) {
            action.setInvoice(
                OBDal.getInstance().get(org.openbravo.model.common.invoice.Invoice.class, invoice));
            action.setAmrasarfAmount(new BigDecimal(amarsarfamount));
          }
          action.setTxn(true);
          // action.setRDVHoldAccountType(penalty_account);
          // if (uniquecode != null)
          // action
          // .setRDVHoldUniquecode(OBDal.getInstance().get(AccountingCombination.class,
          // uniquecode));
          OBDal.getInstance().save(action);
          OBDal.getInstance().flush();

          insertHoldHeader(action, action.getEfinRdvtxnline(), action.getRDVHoldAmount(), null,
              null);
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in RdvHoldActionDAO,getHoldAction", e);
      OBDal.getInstance().rollbackAndClose();

    } finally {
      OBContext.restorePreviousMode();
    }
    return sql;
  }

  public static void addBulkHold(PoReceiptHeaderDTO order, EfinRDV rdv, EfinRDVTransaction rdvTxn)
      throws CreateReceiptException, Exception {

    List<RDVHoldDTO> bulkHoldList = order.getBulkHoldDTO();

    try {
      if (bulkHoldList != null && bulkHoldList.size() > 0) {
        OBContext.setAdminMode();

        OBDal.getInstance().refresh(rdvTxn);

        BigDecimal totalHoldAmt = BigDecimal.ZERO;
        totalHoldAmt = order.getBulkHoldDTO().stream().map(a -> a.getHoldAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (rdvTxn.getNetmatchAmt().subtract(totalHoldAmt).compareTo(BigDecimal.ZERO) < 0) {
          throw new CreateReceiptException(OBMessageUtils.messageBD("Escm_BulkAmtGreater"), true);
        }

        Long seqNo = (long) 10;
        for (RDVHoldDTO bulkHold : bulkHoldList) {

          // check hold id and amount is correct
          OBQuery<EfinRdvHoldTypes> type = OBDal.getInstance().createQuery(EfinRdvHoldTypes.class,
              " as e join e.deductionType as d where d.code =:holdType");
          type.setNamedParameter("holdType", bulkHold.getHoldcode());
          type.setFilterOnReadableClients(false);
          type.setFilterOnReadableOrganization(false);
          type.setMaxResult(1);
          List<EfinRdvHoldTypes> holdTypeList = type.list();

          EfinRdvHoldTypes holdtype = holdTypeList.get(0);

          if (holdTypeList.size() == 0) {
            throw new CreateReceiptException(String.format(
                OBMessageUtils.messageBD("Escm_holdtypepresent"), bulkHold.getHoldcode()), true);
          }

          if (bulkHold.getHoldAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CreateReceiptException(
                OBMessageUtils.messageBD("Escm_InvalidBulkHoldRequest"), true);
          }

          getHoldTxnAction(seqNo.toString(), rdvTxn.getTXNVersion().toString(),
              rdvTxn.getClient().getId(), WebserviceConstants.HOLD_ADD, null, null,
              holdtype.getId(), "", bulkHold.getHoldAmount().toString(), bulkHold.getActionReason(),
              bulkHold.getActionJustification(), bulkHold.getBpartnerId(), "", "N", null, "0.00",
              rdvTxn.getId(), "E", null, false);
          seqNo++;
        }
      }
    } catch (CreateReceiptException e) {
      log4j.error("Exception in addBulkHold" + e);
      OBDal.getInstance().rollbackAndClose();
      throw new CreateReceiptException(e.getMessage());
    } catch (Exception e) {
      log4j.error("Exception in addBulkHold" + e);
      OBDal.getInstance().rollbackAndClose();
      throw new Exception(e.getMessage());
    }
  }

  /**
   * This method is used to insert hold from web service request
   * 
   * @param rdv
   * @param rdvVersion
   * @param lineDTO
   */

  public static void insertHold(EfinRDV rdv, EfinRDVTransaction rdvVersion,
      List<PoReceiptLineDTO> lineDTO) throws CreateReceiptException, Exception {
    try {
      List<EfinRDVTxnline> lineList = CreateRDVHoldDAO.getTransactionLineList(rdvVersion.getId());

      if (lineList.size() > 0) {

        for (PoReceiptLineDTO lineDto : lineDTO) {
          Long seqNo = Long.valueOf("0");
          if (lineDto.getHoldDTO() != null && !lineDto.getHoldDTO().isEmpty()
              && lineDto.getHoldDTO().size() > 0) {
            EfinRDVTxnline line = CreateRDVHoldDAO.getTxnLineBasedOnId(lineList,
                lineDto.getPoLineId());

            BigDecimal totalHoldAmt = lineDto.getHoldDTO().stream().map(a -> a.getHoldAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (line.getNetmatchAmt().subtract(totalHoldAmt).compareTo(BigDecimal.ZERO) < 0) {
              throw new CreateReceiptException(OBMessageUtils.messageBD("Escm_holdamtgreater")
                  + line.getSalesOrderLine().getId());
            }

            for (RDVHoldDTO holdDto : lineDto.getHoldDTO()) {
              // check hold id and amount is correct
              OBQuery<EfinRdvHoldTypes> type = OBDal.getInstance().createQuery(
                  EfinRdvHoldTypes.class,
                  " as e join e.deductionType as d where d.code =:holdType");
              type.setNamedParameter("holdType", holdDto.getHoldcode());
              type.setFilterOnReadableClients(false);
              type.setFilterOnReadableOrganization(false);
              type.setMaxResult(1);
              List<EfinRdvHoldTypes> holdTypeList = type.list();

              if (holdTypeList.size() == 0) {
                throw new CreateReceiptException(String.format(
                    OBMessageUtils.messageBD("Escm_holdtypepresent"), holdDto.getHoldcode()), true);
              }

              EfinRdvHoldTypes holdtype = holdTypeList.get(0);

              if (holdDto.getHoldAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CreateReceiptException(
                    OBMessageUtils.messageBD("Escm_invalidHoldRequest") + lineDto.getPoLineId(),
                    true);
              }

              // Temp fix
              // TO DO --> handle threshold hold in upcoming release
              if (holdtype != null && holdtype.getThreshold() != null
                  && holdtype.getThreshold().compareTo(BigDecimal.ZERO) > 0) {
                throw new CreateReceiptException(
                    String.format(OBMessageUtils.messageBD("Escm_thresholdnotallowed"),
                        holdtype.getDeductionType().getEnglishName()),
                    true);
              }

              CreateRDVHoldDAO.getHoldAction(seqNo, line.getTrxappNo(), line.getClient().getId(),
                  WebserviceConstants.HOLD_ADD, null, holdtype.getId(), "",
                  holdDto.getHoldAmount().toString(), holdDto.getActionReason(),
                  holdDto.getActionJustification(), holdDto.getBpartnerId(), null, "N", null,
                  "0.00", line.getId(), "E", null, false);

              seqNo++;
            }

          }

        }

      }
    } catch (CreateReceiptException e) {
      log4j.error("Exception in insertHold", e);
      OBDal.getInstance().rollbackAndClose();
      throw new CreateReceiptException(e.getMessage());
    } catch (Exception e) {
      log4j.error("Exception in insertHold", e);
      OBDal.getInstance().rollbackAndClose();
      throw new Exception(e.getMessage());
    }
  }
}
