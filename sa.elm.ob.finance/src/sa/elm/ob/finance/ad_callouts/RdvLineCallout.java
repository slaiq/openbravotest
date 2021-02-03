package sa.elm.ob.finance.ad_callouts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.ad_callouts.dao.RdvLineCalloutDAO;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.util.CommonValidations;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Gowtham V
 */

public class RdvLineCallout extends SimpleCallout {

  /**
   * Callout to update the match amt fields
   */
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(RdvLineCallout.class);
  Integer roundoffConst = 2;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    BigDecimal unitprice = new BigDecimal(vars.getNumericParameter("inpunitPrice"));
    BigDecimal qty = new BigDecimal(vars.getNumericParameter("inpmatchQty"));
    BigDecimal matchAmt = new BigDecimal(vars.getNumericParameter("inpmatchAmt"));
    BigDecimal totdeduct = new BigDecimal(vars.getNumericParameter("inptotalDeduct"));
    BigDecimal penalty = new BigDecimal(vars.getNumericParameter("inppenaltyAmt"));
    BigDecimal holdAmt = new BigDecimal(vars.getNumericParameter("inpholdamt"));
    BigDecimal advdeduct = new BigDecimal(vars.getNumericParameter("inpadvDeduct"));
    BigDecimal maxQty = new BigDecimal(vars.getNumericParameter("inpmaxQty"));
    String inpmaxAmt = vars.getNumericParameter("inpmaxAmt");
    BigDecimal maxAmt = BigDecimal.ZERO;
    if (StringUtils.isNotEmpty(inpmaxAmt))
      maxAmt = new BigDecimal(inpmaxAmt);

    BigDecimal inpeximatchAmt = new BigDecimal(vars.getNumericParameter("inpeximatchAmt"));
    BigDecimal inpdeliverAmt = new BigDecimal(vars.getNumericParameter("inpdeliverAmt"));

    String advdedMethod = vars.getStringParameter("inpadvDeductMethod");
    String matchChk = vars.getStringParameter("inpmatch");

    String inpUniqueCode = vars.getStringParameter("inpcValidcombinationId");
    String budgetInit = vars.getStringParameter("inpefinBudgetintId");
    String txnLineId = vars.getStringParameter("inpefinRdvtxnlineId");
    String inpcOrderlineId = vars.getStringParameter("inpcOrderlineId");

    PreparedStatement ps = null;
    ResultSet rs = null;
    BigDecimal matchamt = BigDecimal.ZERO, newadvdeduct = BigDecimal.ZERO;
    EfinRDVTxnline line = OBDal.getInstance().get(EfinRDVTxnline.class, txnLineId);
    BigDecimal DeductPer = line.getEfinRdv().getADVDeductPercent();
    Date now = new Date();
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    String todaydate = dateFormat.format(now);
    String receiveType = null;
    BigDecimal revisionAmt = BigDecimal.ZERO;
    BigDecimal advDeduction = BigDecimal.ZERO;
    BigDecimal txnAdvAmtRem = BigDecimal.ZERO;
    BigDecimal otherLineAdvDed = BigDecimal.ZERO;
    boolean isPriceInclOfTax = false;
    BigDecimal taxAmount = BigDecimal.ZERO;
    BigDecimal taxPercent = BigDecimal.ZERO, tax = BigDecimal.ZERO;
    BigDecimal PERCENT = new BigDecimal("0.01");
    try {

      roundoffConst = line.getClient().getCurrency().getStandardPrecision().intValue();

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

      // if match chk box changed
      if (inpLastFieldChanged.equals("inpmatch") && !matchChk.equals("Y")) {
        info.addResult("inpmatchQty", BigDecimal.ZERO);
        info.addResult("inpmatchAmt", BigDecimal.ZERO);
        info.addResult("inpadvDeduct", BigDecimal.ZERO);
        info.addResult("inpnetmatchAmt", (matchamt.subtract(newadvdeduct.add(penalty).add(holdAmt)))
            .setScale(roundoffConst, RoundingMode.HALF_UP));
        info.addResult("inptotalDeduct",
            (newadvdeduct.add(penalty).add(holdAmt)).setScale(roundoffConst, RoundingMode.HALF_UP));
        // info.addResult("inptotalDeduct", BigDecimal.ZERO);
        // info.addResult("inpnetmatchAmt", BigDecimal.ZERO);
        info.addResult("inplineTaxamt", BigDecimal.ZERO);

      }

      if (inpcOrderlineId == null || inpcOrderlineId.isEmpty()) {
        // get orderlineid
        if (txnLineId != null && !txnLineId.isEmpty()) {
          EfinRDVTxnline trxLn = Utility.getObject(EfinRDVTxnline.class, txnLineId);
          if (trxLn.getSalesOrderLine() != null)
            inpcOrderlineId = trxLn.getSalesOrderLine().getId();

        }
      }
      if (inpLastFieldChanged.equals("inpmatch") && matchChk.equals("Y")) {
        if (receiveType != null && receiveType.equals(Constants.QTY_BASED)) {
          qty = maxQty;
          info.addResult("inpmatchQty", maxQty);
          if (inpcOrderlineId != null && !inpcOrderlineId.isEmpty()) {
            // old // matchamt = RdvLineCalloutDAO.getMatchedAmt(inpcOrderlineId, maxQty);
            // matchamt = inpdeliverAmt.subtract(inpeximatchAmt);
            matchamt = maxQty.multiply(unitprice); // added new
          } else {
            matchamt = (maxQty.multiply(unitprice));
          }
          info.addResult("inpmatchAmt", (matchamt.setScale(roundoffConst, RoundingMode.HALF_UP)));
        }
        // Task no:7286
        else {
          qty = BigDecimal.ONE;
          info.addResult("inpmatchQty", qty);
          if (StringUtils.isNotEmpty(inpcOrderlineId)) {
            OrderLine orderln = OBDal.getInstance().get(OrderLine.class, inpcOrderlineId);
            matchamt = inpdeliverAmt.subtract(inpeximatchAmt)
                .subtract(orderln.getEscmLegacyAmtDelivered());
          } else {
            matchamt = maxAmt;
          }
          info.addResult("inpmatchAmt", (matchamt.setScale(roundoffConst, RoundingMode.HALF_UP)));
        }
        // End
      } else {
        if (receiveType != null && receiveType.equals(Constants.QTY_BASED)) {
          if (inpcOrderlineId != null && !inpcOrderlineId.isEmpty()) {
            // matchamt = RdvLineCalloutDAO.getMatchedAmt(inpcOrderlineId, qty);
            // need to calculate from versions, so unit price may vary in po receipt.
            matchamt = qty.multiply(unitprice);
            // if (maxQty.compareTo(qty) == 0) {
            // matchamt = inpdeliverAmt.subtract(inpeximatchAmt);
            // }
          } else {
            if (line.isAdvance()) {
              matchamt = matchAmt;
            } else {
              matchamt = qty.multiply(unitprice);
            }
          }
        }
        // Task no:7286
        else {
          if (StringUtils.isNotEmpty(inpcOrderlineId)) {
            matchamt = matchAmt;
            if (maxAmt.compareTo(matchAmt) == 0) {
              OrderLine orderln = OBDal.getInstance().get(OrderLine.class, inpcOrderlineId);
              matchamt = inpdeliverAmt.subtract(inpeximatchAmt)
                  .subtract(orderln.getEscmLegacyAmtDelivered());
            }
          } else {
            matchamt = matchAmt;
          }
        }
        // End
      }
      List<EfinRDVTransaction> trxList = line.getEfinRdvtxn().getEfinRdv().getEfinRDVTxnList()
          .stream().filter(a -> a.isAdvancetransaction()).collect(Collectors.toList());
      // if match qty
      if (line.getEfinRdv().getTXNType().equals("PO") && !line.getEfinRdv().isNoadvance()
          && !line.getEfinRdvtxn().isEfinIsskipAdvdeduct()) {

        if (trxList.size() > 0 || (line.getEfinRdv().getLegacyAdvanceBalance() != null
            && line.getEfinRdv().getLegacyAdvanceBalance().compareTo(BigDecimal.ZERO) > 0)) {
          BigDecimal poTotalAmt = BigDecimal.ZERO, advPercentage = BigDecimal.ZERO;
          BigDecimal totalPoAdv = BigDecimal.ZERO, newAdvDeduct = BigDecimal.ZERO,
              newlegAdvPaid = BigDecimal.ZERO;
          // take latest orderId
          Order latestOrder = PurchaseInvoiceSubmitUtils
              .getLatestOrderComplete(line.getEfinRdv().getSalesOrder());

          if (advdedMethod.equals("PE")) {
            // advance deduction calculation based on base version taskno-7292
            if (inpcOrderlineId != null && !inpcOrderlineId.isEmpty()) {
              EfinRDV rdv = line.getEfinRdv();
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

              // adv deduction = matchAmount * adv per;
              // revisionAmt = matchamt.multiply((DeductPer.divide(new BigDecimal(100))));
              revisionAmt = matchamt.multiply(advPercentage.divide(new BigDecimal(100)));
              // sum of the advance deduction except current line
              otherLineAdvDed = RdvLineCalloutDAO.getOtherLineAdvDeduction(txnLineId);

              // current remaining amount is= total advance remaining - other line advance
              // deduction˙
              // txnAdvAmtRem = (trxLn.getEfinRdvtxn().getAdvamtRem().subtract(otherLineAdvDed));
              // the above calculation is not working because of multiple version draft, so changed
              // newAdvDeduct = line.getEfinRdv().getEfinRDVTxnList().stream()
              // .filter(a -> a.getId() != line.getEfinRdvtxn().getId()).map(a -> a.getADVDeduct())
              // .reduce(BigDecimal.ZERO, BigDecimal::add);
              newAdvDeduct = RdvLineCalloutDAO.gettotaladvDeduction(rdv.getId(), line.getId());
              newAdvDeduct = newAdvDeduct.setScale(roundoffConst, RoundingMode.HALF_UP);
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
                  isfullymatched = RdvLineCalloutDAO.chkFullyMatchedOrNot(latestOrder, qty,
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
              if (!line.isAdvance())
                newadvdeduct = matchamt.multiply((DeductPer.divide(new BigDecimal(100))));
            }
          } else if (advdedMethod.equals("ML") && !line.getEfinRdv().isNoadvance()) {
            newadvdeduct = advdeduct;
          }
        } else {

        }
      }

      if (inpLastFieldChanged.equals("inpmatchQty")
          || (inpLastFieldChanged.equals("inpmatch") && matchChk.equals("Y"))) {
        if (inpLastFieldChanged.equals("inpmatchQty")) {
          if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            info.addResult("inpmatch", false);
            info.addResult("inpmatchQty", BigDecimal.ZERO);
          }
        }
        info.addResult("inpmatchAmt", (matchamt.setScale(roundoffConst, RoundingMode.HALF_UP)));
        info.addResult("inpadvDeduct",
            (newadvdeduct.setScale(roundoffConst, RoundingMode.HALF_UP)));
        info.addResult("inptotalDeduct",
            (newadvdeduct.add(penalty).add(holdAmt)).setScale(roundoffConst, RoundingMode.HALF_UP));

        // Calculate tax
        if (inpcOrderlineId != null && !inpcOrderlineId.isEmpty()) {
          OrderLine orderLine = OBDal.getInstance().get(OrderLine.class, inpcOrderlineId);
          EfinRDVTxnline trxLn = Utility.getObject(EfinRDVTxnline.class, txnLineId);
          if (orderLine.getSalesOrder().getEscmTaxMethod() != null) {
            isPriceInclOfTax = orderLine.getSalesOrder().getEscmTaxMethod().isPriceIncludesTax();
            tax = new BigDecimal(orderLine.getSalesOrder().getEscmTaxMethod().getTaxpercent());
            taxPercent = tax.multiply(PERCENT);
            JSONObject taxObject = RdvLineCalloutDAO.calculateTax(trxLn, isPriceInclOfTax,
                (matchamt.subtract(newadvdeduct.add(penalty).add(holdAmt))), qty, taxPercent);
            taxAmount = new BigDecimal(taxObject.get("TaxAmount").toString());
            info.addResult("inplineTaxamt",
                (taxAmount.setScale(roundoffConst, RoundingMode.HALF_UP)));

          }

        }
        // info.addResult("inpadvamtRem", advAmtRem.subtract(diffAmt));

        info.addResult("inpnetmatchAmt", (matchamt.subtract(newadvdeduct.add(penalty).add(holdAmt)))
            .setScale(roundoffConst, RoundingMode.HALF_UP));

        ps = OBDal.getInstance().getConnection()
            .prepareStatement(" select eut_convert_to_hijri ('" + todaydate + "' ) as hijiri");
        rs = ps.executeQuery();
        if (rs.next()) {
          info.addResult("inpmatchDate", rs.getString("hijiri"));
        }
      }

      if (inpLastFieldChanged.equals("inpmatchAmt")
          || (inpLastFieldChanged.equals("inpmatchAmt") && matchChk.equals("Y"))) {
        if (inpLastFieldChanged.equals("inpmatchAmt")) {
          if (matchAmt.compareTo(BigDecimal.ZERO) <= 0) {
            info.addResult("inpmatch", false);
            info.addResult("inpmatchAmt", BigDecimal.ZERO);
          }
        }
        info.addResult("inpadvDeduct",
            (newadvdeduct.setScale(roundoffConst, RoundingMode.HALF_UP)));
        info.addResult("inptotalDeduct",
            (newadvdeduct.add(penalty).add(holdAmt).setScale(roundoffConst, RoundingMode.HALF_UP)));
        // Calculate tax
        if (inpcOrderlineId != null && !inpcOrderlineId.isEmpty()) {
          OrderLine orderLine = OBDal.getInstance().get(OrderLine.class, inpcOrderlineId);
          EfinRDVTxnline trxLn = Utility.getObject(EfinRDVTxnline.class, txnLineId);
          if (orderLine.getSalesOrder().getEscmTaxMethod() != null) {
            isPriceInclOfTax = orderLine.getSalesOrder().getEscmTaxMethod().isPriceIncludesTax();
            tax = new BigDecimal(orderLine.getSalesOrder().getEscmTaxMethod().getTaxpercent());
            taxPercent = tax.multiply(PERCENT);
            JSONObject taxObject = RdvLineCalloutDAO.calculateTax(trxLn, isPriceInclOfTax,
                (matchamt.subtract(newadvdeduct.add(penalty).add(holdAmt))), qty, taxPercent);
            taxAmount = new BigDecimal(taxObject.get("TaxAmount").toString());
            info.addResult("inplineTaxamt",
                (taxAmount.setScale(roundoffConst, RoundingMode.HALF_UP)));

          }

        }

        // info.addResult("inpadvamtRem", advAmtRem.subtract(diffAmt));

        info.addResult("inpnetmatchAmt", (matchamt.subtract(newadvdeduct.add(penalty).add(holdAmt)))
            .setScale(roundoffConst, RoundingMode.HALF_UP));

        ps = OBDal.getInstance().getConnection()
            .prepareStatement(" select eut_convert_to_hijri ('" + todaydate + "' ) as hijiri");
        rs = ps.executeQuery();
        if (rs.next()) {
          info.addResult("inpmatchDate", rs.getString("hijiri"));
        }
      }

      if (line.isAdvance()) {
        EfinRDVTxnline trxLn = Utility.getObject(EfinRDVTxnline.class, txnLineId);
        isPriceInclOfTax = line.getEfinRdv().getSalesOrder().getEscmTaxMethod()
            .isPriceIncludesTax();
        tax = new BigDecimal(line.getEfinRdv().getSalesOrder().getEscmTaxMethod().getTaxpercent());
        taxPercent = tax.multiply(PERCENT);
        JSONObject taxObject = RdvLineCalloutDAO.calculateTax(trxLn, isPriceInclOfTax, matchamt,
            qty, taxPercent);
        taxAmount = new BigDecimal(taxObject.get("TaxAmount").toString());
        info.addResult("inplineTaxamt", (taxAmount.setScale(roundoffConst, RoundingMode.HALF_UP)));
      }

      if (inpLastFieldChanged.equals("inppenaltyAmt") || inpLastFieldChanged.equals("inpadvDeduct")
          || inpLastFieldChanged.equals("inpholdamt")) {
        totdeduct = penalty.add(advdeduct).add(holdAmt);
        info.addResult("inptotalDeduct", totdeduct);
        // info.addResult("inpnetmatchAmt", matchamt.subtract(totdeduct));
        info.addResult("inptotalDeduct",
            (newadvdeduct.add(penalty).add(holdAmt)).setScale(roundoffConst, RoundingMode.HALF_UP));
        info.addResult("inpnetmatchAmt", (matchamt.subtract(newadvdeduct.add(penalty).add(holdAmt)))
            .setScale(roundoffConst, RoundingMode.HALF_UP));
      }

      if (inpLastFieldChanged.equals("inpcValidcombinationId")) {
        AccountingCombination accountingCombination = OBDal.getInstance()
            .get(AccountingCombination.class, inpUniqueCode);
        EfinBudgetIntialization budgetInittialization = OBDal.getInstance()
            .get(EfinBudgetIntialization.class, budgetInit);
        JSONObject funds = CommonValidations.getFundsAvailable(budgetInittialization,
            accountingCombination);
        if (accountingCombination != null && budgetInittialization != null) {
          info.addResult("inpfundsAvailable", funds.get("FA"));
        } else {
          info.addResult("inpfundsAvailable", funds.get("FA"));
        }
      }

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in RdvLineCallout() " + e, e);
      }
    }
  }
}