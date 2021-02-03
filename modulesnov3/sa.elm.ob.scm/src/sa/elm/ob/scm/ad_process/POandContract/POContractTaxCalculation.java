package sa.elm.ob.scm.ad_process.POandContract;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.servlet.http.HttpServletRequest;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ad_callouts.dao.POContractSummaryTotPOChangeDAO;
import sa.elm.ob.scm.ad_process.POandContract.dao.POContractSummaryDAO;

/**
 * @author Divya on 21/02/2019
 */

public class POContractTaxCalculation extends DalBaseProcess {
  /**
   * This servlet is responsible to calculate the tax in Purchase order
   */
  private static final Logger log = LoggerFactory.getLogger(POContractTaxCalculation.class);
  private final static OBError obError = new OBError();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    try {
      OBContext.setAdminMode();
      Integer roundoffConst = OBContext.getOBContext().getCurrentClient().getCurrency()
          .getStandardPrecision().intValue();
      final String strOrderId = (String) bundle.getParams().get("C_Order_ID").toString();
      Order order = OBDal.getInstance().get(Order.class, strOrderId);
      roundoffConst = order.getClient().getCurrency().getStandardPrecision().intValue();

      // variable declaration
      BigDecimal taxAmount = BigDecimal.ZERO;
      BigDecimal lineNetAmt = BigDecimal.ZERO;
      BigDecimal unitPrice = BigDecimal.ZERO;
      BigDecimal Qty = BigDecimal.ZERO;
      BigDecimal taxPercent = BigDecimal.ZERO;
      BigDecimal LineNetAmtWithoutTax = BigDecimal.ZERO;
      BigDecimal grossLineNetAmt = BigDecimal.ZERO;
      BigDecimal changeValue = BigDecimal.ZERO;

      // DecimalFormat euroRelationFmt = org.openbravo.erpCommon.utility.Utility.getFormat(vars,
      // "euroRelation");
      // roundoffConst = euroRelationFmt.getMaximumFractionDigits();

      // Get Line Percent Lookup Id
      String amtChangeType = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGTYP", "01");
      // Get Line amount Lookup Id
      String percentageChangeType = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGTYP",
          "02");
      String decreaseFactor = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGFACT",
          "01");
      String IncreaseFactor = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGFACT",
          "02");

      if (order.isEscmIstax()) {

        if (order.getEscmTaxMethod() != null) {

          taxPercent = new BigDecimal(order.getEscmTaxMethod().getTaxpercent());
          if (order.getOrderLineList().size() > 0) {
            for (OrderLine orderline : order.getOrderLineList()) {
              if (!orderline.isEscmIssummarylevel()
                  && orderline.getLineNetAmount().compareTo(BigDecimal.ZERO) > 0) {
                lineNetAmt = orderline.getLineNetAmount();
                Qty = orderline.getOrderedQuantity();
                grossLineNetAmt = orderline.getUnitPrice().multiply(orderline.getOrderedQuantity());
                // exclusive

                if (!order.getEscmTaxMethod().isPriceIncludesTax()) {
                  // LINE NET AMOUNT CALCULATION IF CHANGE FACTOR APPLY or else gross line net
                  // amount
                  // if (orderline.getEscmPoChangeValue() != null
                  // && orderline.getEscmPoChangeValue().compareTo(BigDecimal.ZERO) > 0
                  // && orderline.getEscmPoChangeType() != null
                  // && orderline.getEscmPoChangeFactor() != null) {
                  //
                  // changeValue = orderline.getEscmPoChangeValue();
                  //
                  // if ((orderline.getEscmPoChangeFactor() != null
                  // && orderline.getEscmPoChangeFactor().getId().equals(decreaseFactor))) {
                  //
                  // BigDecimal remAmt = BigDecimal.ZERO;
                  //
                  // remAmt = POContractSummaryTotPOChangeDAO.remainingAmtLines(orderline);
                  // if (orderline.getEscmPoChangeType() != null
                  // && orderline.getEscmPoChangeType().getId().equals(amtChangeType)) {
                  // changeValue = orderline.getEscmPoChangeValue();
                  // } else if (orderline.getEscmPoChangeType() != null
                  // && orderline.getEscmPoChangeType().getId().equals(percentageChangeType)) {
                  // changeValue = remAmt.multiply(
                  // (orderline.getEscmPoChangeValue()).divide(new BigDecimal("100")));
                  // }
                  //
                  // }
                  // lineNetAmt = POContractSummaryTotPOChangeDAO.calculateLineUpdatedAmt(
                  // orderline.getEscmPoChangeType().getId(),
                  // orderline.getEscmPoChangeFactor().getId(), changeValue, grossLineNetAmt);
                  // } else {
                  // lineNetAmt = grossLineNetAmt;
                  // }
                  //
                  // // TAX AMOUNT CALCULATION
                  // taxAmount = lineNetAmt.multiply(
                  // taxPercent.divide(new BigDecimal(100), roundoffConst, RoundingMode.HALF_UP))
                  // .setScale(roundoffConst, RoundingMode.HALF_UP);
                  //
                  // orderline.setEscmLineTaxamt(taxAmount);
                  // orderline.setLineNetAmount(lineNetAmt.add(orderline.getEscmLineTaxamt()));
                  // orderline.setEscmLineTotalUpdated(grossLineNetAmt);
                  // log.debug("netamount " + orderline.getLineNetAmount());
                  // orderline.getSalesOrder().setEscmCalculateTaxlines(true);
                  // OBDal.getInstance().save(orderline);
                  // OBDal.getInstance().flush();
                  POContractSummaryDAO.updateTaxAndChangeValue(false, orderline.getSalesOrder(),
                      orderline);
                }
                // inclusive
                else {
                  // inclusive tax

                  POContractSummaryDAO.updateTaxAndChangeValue(false, orderline.getSalesOrder(),
                      orderline);

                  //
                  // // calculate the gross line net amount
                  //
                  // if (orderline.getEscmPoChangeValue() != null &&
                  //
                  // orderline.getEscmPoChangeValue().compareTo(BigDecimal.ZERO) > 0) {
                  // // amount - change type
                  // if (orderline.getEscmPoChangeType() != null
                  // && orderline.getEscmPoChangeType().getId().equals(amtChangeType)) {
                  // // increase factor
                  // if (orderline.getEscmPoChangeFactor() != null
                  // && orderline.getEscmPoChangeFactor().getId().equals(IncreaseFactor)) {
                  // grossLineNetAmt = lineNetAmt.subtract(orderline.getEscmPoChangeValue());
                  // }
                  // // decrease factor else
                  // if (orderline.getEscmPoChangeFactor() != null
                  // && orderline.getEscmPoChangeFactor().getId().equals(decreaseFactor)) {
                  // grossLineNetAmt = lineNetAmt.add(orderline.getEscmPoChangeValue());
                  // }
                  // }
                  //
                  // // percentage - change type
                  // else if (orderline.getEscmPoChangeType() != null
                  // && orderline.getEscmPoChangeType().getId().equals(percentageChangeType)) {
                  // // increase factor
                  //
                  // if (orderline.getEscmPoChangeFactor() != null
                  // && orderline.getEscmPoChangeFactor().getId().equals(IncreaseFactor)) {
                  // grossLineNetAmt = lineNetAmt.divide(
                  // BigDecimal.ONE.add(orderline.getEscmPoChangeValue()
                  // .divide(new BigDecimal(100), roundoffConst, RoundingMode.HALF_UP)),
                  // roundoffConst, RoundingMode.HALF_UP);
                  // }
                  // // decrease factor else
                  //
                  // if (orderline.getEscmPoChangeFactor() != null
                  // && orderline.getEscmPoChangeFactor().getId().equals(decreaseFactor)) {
                  // grossLineNetAmt = lineNetAmt.divide(
                  // BigDecimal.ONE.subtract(orderline.getEscmPoChangeValue()
                  // .divide(new BigDecimal(100), roundoffConst, RoundingMode.HALF_UP)),
                  // roundoffConst, RoundingMode.HALF_UP);
                  // }
                  // }
                  // } else {
                  // grossLineNetAmt = lineNetAmt
                  // .divide(orderline.getOrderedQuantity(), roundoffConst, RoundingMode.HALF_UP)
                  // .multiply(orderline.getOrderedQuantity());
                  // }
                  //
                  // LineNetAmtWithoutTax = lineNetAmt.divide(BigDecimal.ONE.add(
                  // taxPercent.divide(new BigDecimal(100), roundoffConst, RoundingMode.HALF_UP)),
                  // roundoffConst, RoundingMode.HALF_UP);
                  // taxAmount = lineNetAmt.subtract(LineNetAmtWithoutTax);
                  // orderline.setEscmLineTaxamt(taxAmount);
                  // orderline.setEscmLineTotalUpdated(lineNetAmt.subtract(taxAmount)
                  // .subtract(lineNetAmt.subtract(grossLineNetAmt)));
                  // unitPrice = orderline.getEscmLineTotalUpdated().divide(Qty, 15,
                  // RoundingMode.HALF_UP);
                  // orderline.setUnitPrice(unitPrice.setScale(roundoffConst,
                  // RoundingMode.HALF_UP));
                  // changeValue = lineNetAmt.subtract(grossLineNetAmt);
                  // grossLineNetAmt = orderline.getUnitPrice()
                  // .multiply(orderline.getOrderedQuantity());
                  // lineNetAmt = Qty.multiply(unitPrice).add(taxAmount).add(changeValue)
                  // .setScale(roundoffConst, RoundingMode.HALF_UP);
                  // //
                  // /*
                  // * if (orderline.getEscmPoChangeType() != null &&
                  // * orderline.getEscmPoChangeType().getId().equals(amtChangeType)) { changeValue
                  // =
                  // * orderline.getEscmPoChangeValue(); } else if (orderline.getEscmPoChangeType()
                  // !=
                  // * null && orderline.getEscmPoChangeType().getId().equals(percentageChangeType))
                  // {
                  // * changeValue = lineNetAmt
                  // * .multiply((orderline.getEscmPoChangeValue()).divide(new BigDecimal("100")));
                  // }
                  // * lineNetAmt = lineNetAmt.add(changeValue).setScale(roundoffConst,
                  // * RoundingMode.HALF_UP); //
                  // */
                  // orderline.setLineNetAmount(lineNetAmt);
                  // orderline.setEscmLineTotalUpdated(grossLineNetAmt);
                  // orderline.getSalesOrder().setEscmCalculateTaxlines(true);
                  // OBDal.getInstance().save(orderline);
                  // OBDal.getInstance().flush();
                  //
                  //
                }

              }
            }
            obError.setType("Success");
            obError.setTitle("Success");
            obError.setMessage(OBMessageUtils.messageBD("ESCM_TaxCalSucess"));
            bundle.setResult(obError);
            return;
          } else {
            obError.setType("Error");
            obError.setTitle("Error");
            obError.setMessage(OBMessageUtils.messageBD("ESCM_NoLineToCalTax"));
            bundle.setResult(obError);
          }
        } else {
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(OBMessageUtils.messageBD("ESCM_TaxMethodISEmpty"));
          bundle.setResult(obError);
          return;
        }

      } else {
        if (order.getOrderLineList().size() > 0) {
          // -----------------------
          for (OrderLine orderline : order.getOrderLineList()) {
            if (!orderline.isEscmIssummarylevel()
                && orderline.getLineNetAmount().compareTo(BigDecimal.ZERO) > 0) {
              // exclusive
              if (!order.getEscmTaxMethod().isPriceIncludesTax()) {
                lineNetAmt = orderline.getLineNetAmount().subtract(orderline.getEscmLineTaxamt());
                orderline.setLineNetAmount(lineNetAmt);
                orderline.setEscmLineTaxamt(BigDecimal.ZERO);
                OBDal.getInstance().save(orderline);
                OBDal.getInstance().flush();
              }
              // inclusive
              else {
                grossLineNetAmt = orderline.getEscmLineTotalUpdated()
                    .add(orderline.getEscmLineTaxamt());
                unitPrice = grossLineNetAmt.divide(orderline.getOrderedQuantity(), roundoffConst,
                    RoundingMode.HALF_UP);
                orderline.setEscmLineTotalUpdated(orderline.getOrderedQuantity().multiply(unitPrice)
                    .setScale(roundoffConst, RoundingMode.HALF_UP));
                // orderline.setLineGrossAmount(grossLineNetAmt);
                orderline.setUnitPrice(unitPrice);
                orderline.setEscmLineTaxamt(BigDecimal.ZERO);
                OBDal.getInstance().save(orderline);
                OBDal.getInstance().flush();
              }

            }
          }
          // to set null value in Tax method
          order.setEscmTaxMethod(null);
          OBDal.getInstance().save(order);
          OBDal.getInstance().flush();
          // -----------------------
          obError.setType("Success");
          obError.setTitle("Success");
          obError.setMessage(OBMessageUtils.messageBD("ESCM_TaxRmvSucess"));
          bundle.setResult(obError);
          return;
        } else {
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(OBMessageUtils.messageBD("ESCM_NoLineToCalTax"));
          bundle.setResult(obError);
        }
      }

    } catch (Exception e) {
      log.error("Exeception in Calculating tax in PO  Process:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      if (OBContext.getOBContext().isInAdministratorMode())
        OBContext.restorePreviousMode();
    }
  }
}
