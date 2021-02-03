package sa.elm.ob.finance.ad_process.RDVProcess.DAO;

/**
 * 
 * @author Kiruthika
 * 
 */
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.model.common.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.EfinRdvTxnLineRef;
import sa.elm.ob.finance.ad_callouts.dao.RdvLineCalloutDAO;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.utility.util.Constants;

/**
 * This DAO class is used to update the Match Flag, Matched Quantity, Matched Amount and Net Match
 * Amount in RDV lines when Match All button is clicked
 */
public class RDVMatchAllDAO {
  private static final Logger LOG = LoggerFactory.getLogger(RDVMatchAllDAO.class);

  public static OBError matchAll(String efin_Rdvtxn_ID, String isDefaultPenalty) {

    String receiveType = null;
    List<EfinRDVTxnline> txnline = null;
    EfinRDVTransaction txn = OBDal.getInstance().get(EfinRDVTransaction.class, efin_Rdvtxn_ID);

    OBDal.getInstance().refresh(txn);

    final String query = " as e where e.efinRdvtxn.id =:txnId order by trxlnNo asc";

    OBQuery<EfinRDVTxnline> rdvLineQry = OBDal.getInstance().createQuery(EfinRDVTxnline.class,
        query);
    rdvLineQry.setNamedParameter("txnId", efin_Rdvtxn_ID);
    rdvLineQry.setFilterOnActive(true);
    rdvLineQry.setFilterOnReadableOrganization(false);
    rdvLineQry.setFilterOnReadableClients(false);

    if (txn.isWebservice()) {
      txnline = rdvLineQry.list();
    } else {
      OBQuery<EfinRDVTxnline> txnLineQry = OBDal.getInstance().createQuery(EfinRDVTxnline.class,
          query);
      txnLineQry.setNamedParameter("txnId", efin_Rdvtxn_ID);
      txnline = txnLineQry.list();
    }

    List<EfinRDVTransaction> trxList = txn.getEfinRdv().getEfinRDVTxnList().stream()
        .filter(a -> a.isAdvancetransaction()).collect(Collectors.toList());

    OBError result;
    if (txnline.size() == 0) {
      result = OBErrorBuilder.buildMessage(null, "error", "@Efin_MatchAllNoLinesError@");
      return result;
    }

    if ((txnline.size() == 1) && (txnline.get(0).isAdvance())) {
      result = OBErrorBuilder.buildMessage(null, "error", "@Efin_MatchAllError@");
      return result;
    }

    Boolean isMatched = true, isSummaryLevel = false;
    BigDecimal revisionAmt = BigDecimal.ZERO;
    BigDecimal advDeduction = BigDecimal.ZERO;
    BigDecimal txnAdvAmtRem = BigDecimal.ZERO;
    Boolean isPriceInclOfTax = false;
    BigDecimal taxAmount = BigDecimal.ZERO;
    BigDecimal taxPercent = BigDecimal.ZERO, tax = BigDecimal.ZERO;
    BigDecimal PERCENT = new BigDecimal("0.01");
    int roundoffConst = txn.getClient().getCurrency().getStandardPrecision().intValue();
    OBQuery<EfinRDVTxnline> matchedLines = OBDal.getInstance().createQuery(EfinRDVTxnline.class,
        "efinRdvtxn.id = :efinRdvtxnID  and match = :matched and summaryLevel = :isSummaryLevel");
    matchedLines.setNamedParameter("efinRdvtxnID", efin_Rdvtxn_ID);
    matchedLines.setNamedParameter("matched", isMatched);
    matchedLines.setNamedParameter("isSummaryLevel", isSummaryLevel);
    if (txn.isWebservice()) {
      matchedLines.setFilterOnReadableOrganization(false);
      matchedLines.setFilterOnReadableClients(false);
    }

    List<EfinRDVTxnline> matchedLinesList = matchedLines.list();

    OBQuery<EfinRDVTxnline> allLines = OBDal.getInstance().createQuery(EfinRDVTxnline.class,
        "efinRdvtxn.id = :efinRdvtxnID and summaryLevel = :isSummaryLevel");
    allLines.setNamedParameter("efinRdvtxnID", efin_Rdvtxn_ID);
    allLines.setNamedParameter("isSummaryLevel", isSummaryLevel);
    if (txn.isWebservice()) {
      allLines.setFilterOnReadableOrganization(false);
      allLines.setFilterOnReadableClients(false);
    }

    List<EfinRDVTxnline> allLinesList = allLines.list();

    if (allLinesList.size() == matchedLinesList.size()) {
      if ("Y".equals(isDefaultPenalty)) {
        result = OBErrorBuilder.buildMessage(null, "error", "@Efin_AllLinesMatched@");
        result.setType("Warning");
        return result;
      } else {
        result = OBErrorBuilder.buildMessage(null, "error", "@Efin_AllLinesMatchedwithoutpenalty@");
        result.setType("Warning");
        return result;
      }
    }

    EfinRDVTxnline line = null;
    for (EfinRDVTxnline index : txnline) {
      line = index;

      if (!line.isAdvance() && !line.isSummaryLevel()) {
        BigDecimal unitprice = line.getUnitCost();
        BigDecimal penalty = line.getPenaltyAmt();
        BigDecimal hold = line.getHoldamt();
        BigDecimal advdeduct = line.getADVDeduct();
        BigDecimal maxQty = line.getMaxQty();
        BigDecimal maxAmt = line.getMaxAmt();
        String advdedMethod = line.getApplicableMethod();

        BigDecimal matchamt = BigDecimal.ZERO, newadvdeduct = BigDecimal.ZERO;

        BigDecimal DeductPer = line.getEfinRdv().getADVDeductPercent();
        BigDecimal otherLineAdvDed = BigDecimal.ZERO;
        line.setMatch(true);

        BigDecimal remQty = BigDecimal.ZERO;
        BigDecimal remAmt = BigDecimal.ZERO;

        for (EfinRdvTxnLineRef ref : line.getEfinRdvTxnLineRefList()) {
          OBDal.getInstance().refresh(ref.getEscmInitialreceipt());
          EfinRDVTxnline trxnLn = ref.getEfinRdvtxnline();
          remQty = remQty.add(ref.getEscmInitialreceipt().getDeliveredQty()
              .subtract(ref.getEscmInitialreceipt().getMatchQty().subtract(trxnLn.getMatchQty())));
          remAmt = remAmt.add(ref.getEscmInitialreceipt().getDeliveredAmt()
              .subtract(ref.getEscmInitialreceipt().getMatchAmt().subtract(trxnLn.getMatchAmt())));
        }
        // maxQty = line.getMaxQty();
        maxQty = remQty;
        maxAmt = remAmt;
        if (line.getSalesOrderLine() != null
            && line.getSalesOrderLine().getSalesOrder().getEscmReceivetype().equals("AMT"))
          line.setMatchQty(new BigDecimal("1"));
        else
          line.setMatchQty(maxQty);
        line.setMatchAmt(maxAmt);
        unitprice = line.getUnitCost();

        // checking order is amt based or qty based Task No.7286
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
        if (receiveType != null && receiveType.equals(Constants.QTY_BASED)) {
          if (line.getSalesOrderLine() != null) {
            String inpcOrderlineId = line.getSalesOrderLine().getId();
            // matchamt = RdvLineCalloutDAO.getMatchedAmt(inpcOrderlineId, maxQty);
            matchamt = line.getMatchQty().multiply(unitprice);
            if (maxQty.compareTo(line.getMatchQty()) == 0) {
              matchamt = (line.getMatchQty().multiply(unitprice));
            }
          } else
            matchamt = maxQty.multiply(unitprice);
        } else {
          if (line.getSalesOrderLine() != null) {
            matchamt = maxAmt;
            if (maxAmt.compareTo(line.getMatchAmt()) == 0) {
              // matchamt = line.getDeliverAmt().subtract(line.getEximatchAmt())
              // .subtract(line.getSalesOrderLine().getEscmLegacyAmtDelivered());
              // matchamt = line.getMatchAmt()
              // .subtract(line.getSalesOrderLine().getEscmLegacyAmtDelivered());
              matchamt = line.getMatchAmt();
            }
          } else {
            matchamt = maxAmt;
          }
        }
        line.setMatchAmt(matchamt.setScale(roundoffConst, RoundingMode.HALF_UP));
        if (line.getEfinRdv().getTXNType().equals("PO") && !line.getEfinRdv().isNoadvance()
            && !line.getEfinRdvtxn().isEfinIsskipAdvdeduct()) {
          if (trxList.size() > 0 || (line.getEfinRdv().getLegacyAdvanceBalance() != null
              && line.getEfinRdv().getLegacyAdvanceBalance().compareTo(BigDecimal.ZERO) > 0)) {
            BigDecimal poTotalAmt = BigDecimal.ZERO, advPercentage = BigDecimal.ZERO;
            BigDecimal totalPoAdv = BigDecimal.ZERO, newAdvDeduct = BigDecimal.ZERO,
                newlegAdvPaid = BigDecimal.ZERO;
            Order latestOrder = PurchaseInvoiceSubmitUtils
                .getLatestOrderComplete(line.getEfinRdv().getSalesOrder());
            if (advdedMethod.equals("PE")) {
              // advance deduction calculation based on base version taskno-7292
              if (line.getSalesOrderLine() != null) {
                // adv deduction = matchAmount * adv per;
                // take latest orderId
                if (latestOrder != null) {
                  poTotalAmt = latestOrder.getGrandTotalAmount();
                }
                // if (line.getEfinRdv().getLegacyTotaladvPaid() != null
                // && line.getEfinRdv().getLegacyAdvanceBalance().compareTo(BigDecimal.ZERO) > 0) {
                // advPercentage = (line.getEfinRdv().getLegacyTotaladvPaid().divide(poTotalAmt, 15,
                // RoundingMode.HALF_UP));
                // } else if (trxList.size() > 0) {
                // advPercentage = (trxList.get(0).getNetmatchAmt().divide(poTotalAmt, 15,
                // RoundingMode.HALF_UP));
                // }
                advPercentage = latestOrder.getEscmAdvpaymntPercntge();
                revisionAmt = matchamt.multiply(advPercentage.divide(new BigDecimal(100)));

                // revisionAmt = matchamt.multiply((DeductPer.divide(new BigDecimal(100))));
                // sum of the advance deduction except current line
                otherLineAdvDed = RdvLineCalloutDAO.getOtherLineAdvDeduction(line.getId());
                // current remaining amount is= total advance remaining - other line advance
                // deduction
                // txnAdvAmtRem = (txn.getAdvamtRem().subtract(otherLineAdvDed));
                // the above calculation is not working because of multiple version draft, so
                // changed
                EfinRDV rdv = line.getEfinRdv();
                newAdvDeduct = RdvLineCalloutDAO.gettotaladvDeduction(rdv.getId(), line.getId());
                newAdvDeduct = newAdvDeduct.setScale(roundoffConst, RoundingMode.HALF_UP);
                // newAdvDeduct = line.getEfinRdv().getEfinRDVTxnList().stream()
                // .filter(a -> a.getId() != txn.getId()).map(a -> a.getADVDeduct())
                // .reduce(BigDecimal.ZERO, BigDecimal::add);
                newlegAdvPaid = rdv.getLegacyTotaladvPaid().subtract(rdv.getLegacyAdvanceBalance());
                if (latestOrder.getEscmLegacyAdvPaymentAmt() != null
                    && latestOrder.getEscmLegacyAdvPaymentAmt().compareTo(BigDecimal.ZERO) > 0) {
                  totalPoAdv = latestOrder.getEscmLegacyAdvPaymentAmt();
                } else {
                  totalPoAdv = latestOrder.getEscmAdvpaymntAmt();
                }
                txnAdvAmtRem = (totalPoAdv.subtract(newAdvDeduct.add(newlegAdvPaid)))
                    .subtract(otherLineAdvDed);
                // if remaining advance amount more than advance deduction then have to take
                // remaining amount as advance deduction
                if (txnAdvAmtRem.compareTo(revisionAmt) > 0) {
                  advDeduction = revisionAmt;
                } else {
                  advDeduction = txnAdvAmtRem;
                }
                if (receiveType != null && line.getEfinRdv().getSalesOrder() != null) {

                  Boolean isfullymatched = false;
                  if (receiveType.equals(Constants.QTY_BASED)) {
                    isfullymatched = RdvLineCalloutDAO.chkFullyMatchedOrNot(latestOrder, maxQty,
                        BigDecimal.ZERO, receiveType, line);
                  } else {
                    isfullymatched = RdvLineCalloutDAO.chkFullyMatchedOrNot(latestOrder,
                        BigDecimal.ZERO, matchamt, receiveType, line);
                  }
                  if (isfullymatched) {
                    advDeduction = txnAdvAmtRem;
                  }
                }

                newadvdeduct = advDeduction;
              } // end- advance deduction calculation based on base version taskno-7292
              else {
                newadvdeduct = matchamt.multiply((DeductPer.divide(new BigDecimal(100))));
              }
            } else if (advdedMethod.equals("ML") && !line.getEfinRdv().isNoadvance()) {
              newadvdeduct = advdeduct;
            }
          }
        }
        line.setADVDeduct(newadvdeduct.setScale(roundoffConst, RoundingMode.HALF_UP));
        line.setTotalDeduct(
            (newadvdeduct.add(penalty).add(hold)).setScale(roundoffConst, RoundingMode.HALF_UP));
        line.setNetmatchAmt((matchamt.subtract(newadvdeduct.add(penalty).add(hold)))
            .setScale(roundoffConst, RoundingMode.HALF_UP));

        // calculate Tax
        if (line.getSalesOrderLine() != null) {
          if (line.getSalesOrderLine().getSalesOrder().getEscmTaxMethod() != null) {
            isPriceInclOfTax = line.getSalesOrderLine().getSalesOrder().getEscmTaxMethod()
                .isPriceIncludesTax();
            tax = new BigDecimal(
                line.getSalesOrderLine().getSalesOrder().getEscmTaxMethod().getTaxpercent());
            taxPercent = tax.multiply(PERCENT);
            JSONObject taxObject = RdvLineCalloutDAO.calculateTax(line, isPriceInclOfTax,
                line.getNetmatchAmt(), maxQty, taxPercent);
            try {
              taxAmount = new BigDecimal(taxObject.get("TaxAmount").toString());
            } catch (JSONException e) {
            }

          }
        }
        line.setLineTaxamt(taxAmount);

        OBDal.getInstance().save(line);
      }
    }
    OBDal.getInstance().flush();
    result = OBErrorBuilder.buildMessage(null, "success", "@Efin_RDV_MatchAll@");
    return result;
  }
}