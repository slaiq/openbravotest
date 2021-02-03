package sa.elm.ob.finance.event;

import java.math.BigDecimal;

import javax.enterprise.event.Observes;

import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.EfinRdvTxnLineRef;
import sa.elm.ob.utility.util.Constants;

/**
 * @author Gowtham V
 * 
 */
public class RdvLineEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinRDVTxnline.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private static final Logger LOG = LoggerFactory.getLogger(RdvLineEvent.class);
  BigDecimal qty = BigDecimal.ZERO;
  BigDecimal newqty = BigDecimal.ZERO;
  BigDecimal diff = BigDecimal.ZERO;
  BigDecimal preAdv = BigDecimal.ZERO;
  Query sqlQuery1 = null;
  String query = null;
  String sourceRef = "";
  String initReceipt = "";

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      EfinRDVTxnline line = (EfinRDVTxnline) event.getTargetInstance();
      BigDecimal advAmtRem = line.getEfinRdvtxn().getAdvamtRem();
      BigDecimal advAmtTransaction = line.getEfinRdvtxn().getADVDeduct();
      long version = line.getEfinRdvtxn().getTXNVersion();
      EfinRDV header = line.getEfinRdv();
      String receiveType = null;
      BigDecimal available_Amt = BigDecimal.ZERO;

      // should not allow to insert more than delivered qty.

      final Property matchQty = entities[0].getProperty(EfinRDVTxnline.PROPERTY_MATCHQTY);
      final Property lineStatus = entities[0].getProperty(EfinRDVTxnline.PROPERTY_LINESTATUS);
      final Property advDeduct = entities[0].getProperty(EfinRDVTxnline.PROPERTY_ADVDEDUCT);
      final Property match = entities[0].getProperty(EfinRDVTxnline.PROPERTY_MATCH);
      final Property matchAmt = entities[0].getProperty(EfinRDVTxnline.PROPERTY_MATCHAMT);
      final Property netMatchAmt = entities[0].getProperty(EfinRDVTxnline.PROPERTY_NETMATCHAMT);

      if (line.getEfinRdv() != null && line.getEfinRdv().getSalesOrder() != null
          && line.getEfinRdv().getSalesOrder().getEscmReceivetype() != null
          && line.getEfinRdv().getSalesOrder().getEscmReceivetype().equals("AMT")) {
        receiveType = Constants.AMOUNT_BASED;
      } else {

        if (line.getEfinRdv() != null && line.getEfinRdv().getTXNType().equals("POD")
            && line.getEfinRdv().getGoodsShipment() != null
            && line.getEfinRdv().getGoodsShipment().getEscmReceivetype() != null
            && line.getEfinRdv().getGoodsShipment().getEscmReceivetype().equals("AMT")) {
          receiveType = Constants.AMOUNT_BASED;
        } else {
          receiveType = Constants.QTY_BASED;
        }
      }

      if (event.getPreviousState(matchQty) != event.getCurrentState(matchQty)
          || event.getPreviousState(matchAmt) != event.getCurrentState(matchAmt)) {
        BigDecimal available_Qty = BigDecimal.ZERO;
        BigDecimal prevMatchQty = new BigDecimal(event.getPreviousState(matchQty).toString());

        BigDecimal prevMatchAmt = new BigDecimal(event.getPreviousState(matchAmt).toString());
        if (receiveType.equals(Constants.QTY_BASED)) {
          // new condition based on poreceipt reference.
          for (EfinRdvTxnLineRef ref : line.getEfinRdvTxnLineRefList()) {
            available_Qty = available_Qty.add(ref.getEscmInitialreceipt().getDeliveredQty()
                .subtract(ref.getEscmInitialreceipt().getMatchQty()));
          }
          available_Qty = available_Qty.add(prevMatchQty);
          if (!line.isAdvance() && line.getMatchQty().compareTo(available_Qty) > 0) {
            throw new OBException(OBMessageUtils.messageBD("Efin_RdvLine_QtyExceeds"));
          }

          // set line status
          if (available_Qty.compareTo(line.getMatchQty()) == 0) {
            event.setCurrentState(lineStatus, "FM");
          } else {
            event.setCurrentState(lineStatus, "PM");
          }
        } else {
          // new condition based on poreceipt reference.
          for (EfinRdvTxnLineRef ref : line.getEfinRdvTxnLineRefList()) {
            available_Amt = available_Amt.add(ref.getEscmInitialreceipt().getDeliveredAmt()
                .subtract(ref.getEscmInitialreceipt().getMatchAmt()));
          }
          available_Amt = available_Amt.add(prevMatchAmt);
          if (!line.isAdvance() && line.getMatchAmt().compareTo(available_Amt) > 0) {
            throw new OBException(OBMessageUtils.messageBD("Efin_RdvLine_AmtExceeds"));
          }
          // set line status
          if (available_Amt.compareTo(line.getMatchAmt()) == 0) {
            event.setCurrentState(lineStatus, "FM");
          } else {
            event.setCurrentState(lineStatus, "PM");
          }
        }

        // set match quantity in receipt
        BigDecimal prevQty1 = new BigDecimal(event.getPreviousState(matchQty).toString());
        BigDecimal diffQty1 = line.getMatchQty().subtract(prevQty1);
        BigDecimal tempQty = diffQty1;
        BigDecimal prevAmt = new BigDecimal(event.getPreviousState(matchAmt).toString());
        BigDecimal difAmt = line.getMatchAmt().subtract(prevAmt);
        BigDecimal tempAmt = difAmt;

        if (receiveType.equals(Constants.QTY_BASED)) {
          if (tempQty.compareTo(BigDecimal.ZERO) > 0) {
            OBQuery<EfinRdvTxnLineRef> lineRef = OBDal.getInstance().createQuery(
                EfinRdvTxnLineRef.class,
                "efinRdvtxnline.id=:txnLineId order by escmInitialreceipt.creationDate asc");
            lineRef.setNamedParameter("txnLineId", line.getId());
            if (line.getEfinRdvtxn().isWebservice()) {
              lineRef.setFilterOnReadableOrganization(false);
              lineRef.setFilterOnReadableClients(false);
            }

            for (EfinRdvTxnLineRef ref : lineRef.list()) {
              if (tempQty.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal AvaQty = ref.getAvailableQty().subtract(ref.getMatchQty());
                if (AvaQty.compareTo(BigDecimal.ZERO) > 0) {
                  if (tempQty.compareTo(AvaQty) <= 0) {
                    ref.setMatchQty(ref.getMatchQty().add(tempQty));
                    tempQty = tempQty.subtract(tempQty);
                  } else {
                    ref.setMatchQty(ref.getMatchQty().add(AvaQty));
                    tempQty = tempQty.subtract(AvaQty);
                  }
                }
              }
              OBDal.getInstance().save(ref);
            }
          } else if (tempQty.compareTo(BigDecimal.ZERO) < 0) {

            OBQuery<EfinRdvTxnLineRef> lineRef = OBDal.getInstance().createQuery(
                EfinRdvTxnLineRef.class,
                "efinRdvtxnline.id=:txnLineId order by escmInitialreceipt.creationDate desc");
            lineRef.setNamedParameter("txnLineId", line.getId());
            if (line.getEfinRdvtxn().isWebservice()) {
              lineRef.setFilterOnReadableOrganization(false);
              lineRef.setFilterOnReadableClients(false);
            }
            for (EfinRdvTxnLineRef ref : lineRef.list()) {
              if (tempQty.compareTo(BigDecimal.ZERO) < 0) {
                if (ref.getMatchQty().compareTo(BigDecimal.ZERO) > 0) {
                  if (tempQty.negate().compareTo(ref.getMatchQty()) <= 0) {
                    ref.setMatchQty(ref.getMatchQty().add(tempQty));
                    tempQty = tempQty.add(tempQty.negate());
                  } else {
                    tempQty = tempQty.add(ref.getMatchQty());
                    ref.setMatchQty(BigDecimal.ZERO);
                  }
                }
              }
              OBDal.getInstance().save(ref);
            }
          }
        } else {
          if (tempAmt.compareTo(BigDecimal.ZERO) > 0) {
            OBQuery<EfinRdvTxnLineRef> lineRef = OBDal.getInstance().createQuery(
                EfinRdvTxnLineRef.class,
                "efinRdvtxnline.id=:txnLineId order by escmInitialreceipt.creationDate asc");
            lineRef.setNamedParameter("txnLineId", line.getId());
            if (line.getEfinRdvtxn().isWebservice()) {
              lineRef.setFilterOnReadableOrganization(false);
              lineRef.setFilterOnReadableClients(false);
            }
            for (EfinRdvTxnLineRef ref : lineRef.list()) {
              if (tempAmt.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal AvaAmt = ref.getAvailableAmt().subtract(ref.getMatchAmt());
                if (AvaAmt.compareTo(BigDecimal.ZERO) > 0) {
                  if (tempAmt.compareTo(AvaAmt) <= 0) {
                    ref.setMatchAmt(ref.getMatchAmt().add(tempAmt));
                    tempAmt = tempAmt.subtract(tempAmt);
                  } else {
                    ref.setMatchAmt(ref.getMatchAmt().add(AvaAmt));
                    tempAmt = tempAmt.subtract(AvaAmt);
                  }
                }
              }
              OBDal.getInstance().save(ref);
            }
          } else if (tempAmt.compareTo(BigDecimal.ZERO) < 0) {

            OBQuery<EfinRdvTxnLineRef> lineRef = OBDal.getInstance().createQuery(
                EfinRdvTxnLineRef.class,
                "efinRdvtxnline.id=:txnLineId order by escmInitialreceipt.creationDate desc");
            lineRef.setNamedParameter("txnLineId", line.getId());
            if (line.getEfinRdvtxn().isWebservice()) {
              lineRef.setFilterOnReadableOrganization(false);
              lineRef.setFilterOnReadableClients(false);
            }
            for (EfinRdvTxnLineRef ref : lineRef.list()) {
              if (tempAmt.compareTo(BigDecimal.ZERO) < 0) {
                if (ref.getMatchAmt().compareTo(BigDecimal.ZERO) > 0) {
                  if (tempAmt.negate().compareTo(ref.getMatchAmt()) <= 0) {
                    ref.setMatchAmt(ref.getMatchAmt().add(tempAmt));
                    tempAmt = tempAmt.add(tempAmt.negate());
                  } else {
                    tempAmt = tempAmt.add(ref.getMatchAmt());
                    ref.setMatchAmt(BigDecimal.ZERO);
                  }
                }
              }
              OBDal.getInstance().save(ref);
            }
          }
        }

        // if exst line invoice cancelled and chnaged qty then should affect remaining lines exst
        // qty.
        if (line.getEfinRdvtxn().getTxnverStatus().equals("CL")
            || line.getEfinRdvtxn().getTxnverStatus().equals("DR")) {
          BigDecimal prevQty = new BigDecimal(event.getPreviousState(matchQty).toString());
          BigDecimal diffQty = line.getMatchQty().subtract(prevQty);
          BigDecimal diffAmt = line.getUnitCost().multiply(diffQty);
          // will write common trigger to update all draft version qty (Multiple draft version
          // change) - not commended
          for (EfinRDVTransaction txn : header.getEfinRDVTxnList()) {
            if (txn.getTXNVersion() > version) {

              /*
               * for (EfinRDVTxnline txnLn : txn.getEfinRDVTxnlineList()) { if
               * (txnLn.getItemDesc().equals(line.getItemDesc())) {
               * txnLn.setEximatchQty(txnLn.getEximatchQty().add(diffQty));
               * txnLn.setEximatchAmt(txnLn.getEximatchAmt().add(diffAmt));
               * OBDal.getInstance().save(txnLn); } }
               */

            }
          }

        }
      }

      // advance amt should not exceed over all advance paid.
      if (event.getPreviousState(advDeduct) != event.getCurrentState(advDeduct)) {
        preAdv = (BigDecimal) (event.getPreviousState(advDeduct));
        diff = line.getADVDeduct().subtract(preAdv);
        // if ((advAmt.add(diff)).compareTo(totalAdv) > 0) {

        // note: totalAdv will not work for legacy data so compare with advamtrem from transaction
        // tab and also compare adv amount based on transaction version bcz advamtrem will
        // update based on adv deduction in previous version
        if ((advAmtTransaction.add(diff)).compareTo(advAmtRem) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Advamt_Exceeds"));
        }
      }
      // if penalty is applied canot able to unmatch.
      if (event.getPreviousState(match) != event.getCurrentState(match)) {
        if (!line.isMatch() && line.getPenaltyAmt().compareTo(BigDecimal.ZERO) != 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Remove_Penalty"));
        }
      }

      if (line.getNetmatchAmt().compareTo(BigDecimal.ZERO) < 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_LineNetAmt_LessthanZero"));
      }

      if (line.isAdvance() && event.getPreviousState(matchAmt) != event.getCurrentState(matchAmt)) {
        BigDecimal preNetMatchAmt = new BigDecimal(event.getPreviousState(netMatchAmt).toString());
        BigDecimal totMatchAmount = line.getNetmatchAmt().subtract(preNetMatchAmt)
            .add(line.getEfinRdvtxn().getNetmatchAmt());
        BigDecimal poAdvance = line.getEfinRdv().getSalesOrder().getEscmAdvpaymntAmt();
        if (totMatchAmount.compareTo(poAdvance) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Advance_Overpay"));
        }
      }

      /*
       * if (line.getAccountingCombination() == null) { throw new
       * OBException(OBMessageUtils.messageBD("Efin_RDV_Uniqecode_Mandatory")); }
       */

    } catch (Exception e) {
      e.printStackTrace();
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while update RDVlines: " + e);
      }
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      EfinRDVTxnline line = (EfinRDVTxnline) event.getTargetInstance();

      if (line.getNetmatchAmt().compareTo(BigDecimal.ZERO) < 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_LineNetAmt_LessthanZero"));
      }
      /*
       * if (line.getAccountingCombination() == null) { throw new
       * OBException(OBMessageUtils.messageBD("Efin_RDV_Uniqecode_Mandatory")); }
       */
      if (line.isAdvance()) {
        BigDecimal totMatchAmount = line.getNetmatchAmt()
            .add(line.getEfinRdvtxn().getNetmatchAmt());
        BigDecimal poAdvance = line.getEfinRdv().getSalesOrder().getEscmAdvpaymntAmt();
        if (totMatchAmount.compareTo(poAdvance) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Advance_Overpay"));
        }
      }

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while insert RDVtxn: " + e);
      }
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      EfinRDVTxnline line = (EfinRDVTxnline) event.getTargetInstance();
      if (!line.getTxnverStatus().equals("DR")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_RDVDel_App"));
      }
      if (!line.getApprovalStatus().equals("DR") && !line.getApprovalStatus().equals("REJ")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_RDVDel_App"));
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while delete RDVtxnLine: " + e);
      }
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
