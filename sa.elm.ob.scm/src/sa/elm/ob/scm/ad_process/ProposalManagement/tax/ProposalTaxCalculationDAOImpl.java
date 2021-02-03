package sa.elm.ob.scm.ad_process.ProposalManagement.tax;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;

import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;

/**
 * @author Rashika.V.S on 21-02-2019
 */

public class ProposalTaxCalculationDAOImpl implements ProposalTaxCalculationDAO {
  private static final Logger log4j = Logger.getLogger(ProposalTaxCalculationDAOImpl.class);
  private Boolean isPriceInclOfTax = Boolean.FALSE;

  BigDecimal PERCENT = new BigDecimal("0.01");
  HttpServletRequest request = RequestContext.get().getRequest();
  Integer roundoffConst = 3;

  @Override
  public OBError insertTaxAmount(EscmProposalMgmt proposalMgmt, Integer decimalFormat) {
    OBError result = new OBError();
    BigDecimal taxAmount = BigDecimal.ZERO;
    BigDecimal lineAmount = BigDecimal.ZERO;
    BigDecimal discountamt = BigDecimal.ZERO;
    BigDecimal discount = BigDecimal.ZERO;
    int a = 0;
    try {
      if (proposalMgmt != null) {

        List<EscmProposalmgmtLine> taxableLines = new ArrayList<EscmProposalmgmtLine>();
        if (proposalMgmt.isTaxLine()) {
          isPriceInclOfTax = isPriceInclusiveOfTax(proposalMgmt);
        }
        taxableLines = getProposalLines(proposalMgmt);
        roundoffConst = decimalFormat;

        if (taxableLines.size() > 0) {
          for (EscmProposalmgmtLine line : taxableLines) {
            if (proposalMgmt.isTaxLine()) {
              JSONObject taxObject = calculateTax(line);
              taxAmount = new BigDecimal(taxObject.get("TaxAmount").toString());
              lineAmount = new BigDecimal(taxObject.get("LineAmount").toString());

              line.setTaxAmount(taxAmount);
              line.getEscmProposalmgmt().setCalculateTax(true);
              line.getEscmProposalmgmt().setTaxidentify(true);
              if (isPriceInclOfTax) {
                line.setNegotUnitPrice(lineAmount);
                BigDecimal line_total = (line.getNegotUnitPrice().add(line.getTaxAmount()
                    .divide(line.getMovementQuantity(), 15, RoundingMode.HALF_UP))
                    .multiply(line.getMovementQuantity()));

                line.setLineTotal(line_total.setScale(roundoffConst, RoundingMode.HALF_UP));
              } else {
                line.setLineTotal(lineAmount);
              }

              if (line.getEscmProposalmgmt().isVersion()
                  && (line.getPEENegotUnitPrice().compareTo(line.getNegotUnitPrice()) > 0)
                  && line.getDiscount().compareTo(new BigDecimal(0)) == 0) {
                BigDecimal negUnitPricDiscAmt = BigDecimal.ZERO;
                BigDecimal negUnitPrice = BigDecimal.ZERO;

                if (isPriceInclOfTax) {
                  BigDecimal peeTotalAmount = (line.getPEENegotUnitPrice()
                      .add(line.getPEEUnittax())).multiply(line.getPEEQty());
                  discountamt = (peeTotalAmount).subtract((line.getNegotUnitPrice()
                      .add(line.getTaxAmount().divide(line.getMovementQuantity(), 15,
                          RoundingMode.HALF_UP))
                      .multiply(line.getMovementQuantity())).setScale(roundoffConst,
                          RoundingMode.HALF_UP));
                  discount = (discountamt.divide((peeTotalAmount), 15, RoundingMode.HALF_UP)
                      .multiply(new BigDecimal(100))).setScale(2, RoundingMode.HALF_UP);

                } else {
                  BigDecimal peeTotalAmount = line.getPEENegotUnitPrice()
                      .multiply(line.getPEEQty());
                  discountamt = (peeTotalAmount)
                      .subtract(line.getNegotUnitPrice().multiply(line.getMovementQuantity()));
                  discount = (discountamt.divide((peeTotalAmount), 15, RoundingMode.HALF_UP)
                      .multiply(new BigDecimal(100))).setScale(2, RoundingMode.HALF_UP);

                }
                line.setLineTotal(
                    (line.getNegotUnitPrice().multiply(line.getMovementQuantity())).add(taxAmount));

                line.setDiscount(discount);
                line.setDiscountmount(discountamt);
                line.getEscmProposalmgmt().setDiscountAmount(
                    line.getEscmProposalmgmt().getDiscountAmount().add(discountamt));
              }
              // setting isprocess as yes while calculating tax
              line.setProcess(true);
              OBDal.getInstance().save(line);
            } else {
              line.setNegotUnitPrice(line.getNetprice());
              line.setLineTotal(line.getNetprice().multiply(line.getMovementQuantity()).subtract(
                  line.getDiscountmount() == null ? BigDecimal.ZERO : line.getDiscountmount()));
              line.setTaxAmount(BigDecimal.ZERO);
              line.getEscmProposalmgmt().setCalculateTax(true);
              line.getEscmProposalmgmt().setTaxidentify(true);
              line.setProcess(true);
              OBDal.getInstance().save(line);
            }

          }
          OBDal.getInstance().flush();
          // update parent records
          updateParentRecord(proposalMgmt);

          // to set null in Tax_method and Zero in total Tax amount when IstaxLine = 'N'
          if (!proposalMgmt.isTaxLine()) {
            proposalMgmt.setEfinTaxMethod(null);
            proposalMgmt.setTotalTaxAmount(BigDecimal.ZERO);
            OBDal.getInstance().save(proposalMgmt);
          }

          OBDal.getInstance().flush();
          // OBDal.getInstance().commitAndClose();
          result = OBErrorBuilder.buildMessage(null, "success", "@ESCM_TaxCal_Completed@");
        }
      }
    } catch (OBException e) {
      result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      log4j.error("Exception in insertTaxAmount(): ", e);
    } catch (Exception e) {
      result = OBErrorBuilder.buildMessage(null, "error", "@HB_INTERNAL_ERROR@");
      log4j.error("Exception in insertTaxAmount() ", e);
    }

    return result;
  }

  public void updateParentRecord(EscmProposalMgmt proposalMgmt) {
    int d = 0;
    try {
      List<EscmProposalmgmtLine> lineList = proposalMgmt.getEscmProposalmgmtLineList();

      List<EscmProposalmgmtLine> parentList = lineList.stream().filter(a -> a.isSummary())
          .collect(Collectors.toList());
      List<EscmProposalmgmtLine> childList = lineList.stream()
          .filter(a -> !a.isSummary() && a.getParentLineNo() != null
              && (a.getStatus() == null || !a.getStatus().equals("CL")))
          .collect(Collectors.toList());
      List<EscmProposalmgmtLine> peeChildList = lineList.stream()
          .filter(a -> !a.isSummary() && a.getParentLineNo() != null
              && (a.getPeestatus() == null || !a.getPeestatus().equals("CL")))
          .collect(Collectors.toList());

      for (EscmProposalmgmtLine line : parentList) {

        BigDecimal lineTotal = BigDecimal.valueOf(
            childList.stream().filter(a -> a.getParentLineNo().getId().equals(line.getId()))
                .mapToDouble(a -> a.getLineTotal().doubleValue()).sum());
        BigDecimal teeLineTotal = BigDecimal.valueOf(
            childList.stream().filter(a -> a.getParentLineNo().getId().equals(line.getId()))
                .mapToDouble(a -> a.getTechLineTotal().doubleValue()).sum());
        BigDecimal taxAmt = BigDecimal.valueOf(
            childList.stream().filter(a -> a.getParentLineNo().getId().equals(line.getId()))
                .mapToDouble(a -> a.getTaxAmount().doubleValue()).sum());
        BigDecimal teeTaxAmt = BigDecimal.valueOf(
            childList.stream().filter(a -> a.getParentLineNo().getId().equals(line.getId()))
                .mapToDouble(a -> a.getTEELineTaxamt().doubleValue()).sum());
        BigDecimal peeTaxAmt = BigDecimal.valueOf(
            childList.stream().filter(a -> a.getParentLineNo().getId().equals(line.getId()))
                .mapToDouble(a -> a.getPEELineTaxamt().doubleValue()).sum());
        BigDecimal teeInitunitprice = BigDecimal.valueOf(childList.stream()
            .filter(a -> a.getParentLineNo().getId().equals(line.getId()))
            .mapToDouble(a -> a.getTEEInitUnitprice().multiply(a.getTechLineQty()).doubleValue())
            .sum());
        BigDecimal peeInitunitprice = BigDecimal.valueOf(childList.stream()
            .filter(a -> a.getParentLineNo().getId().equals(line.getId()))
            .mapToDouble(a -> a.getPEEInitUnitprice().multiply(a.getPEEQty()).doubleValue()).sum());
        BigDecimal peeLineTotal = BigDecimal.valueOf(peeChildList.stream()
            .filter(a -> a.getParentLineNo().getId().equals(line.getId()))
            .mapToDouble(a -> a.getPEEInitUnitprice().multiply(a.getPEEQty()).doubleValue()).sum());

        line.setGrossUnitPrice(lineTotal);
        line.setNetprice(lineTotal);
        line.setNegotUnitPrice(lineTotal);
        line.setLineTotal(lineTotal);
        line.setTechLineTotal(teeLineTotal);
        line.setPEELineTotal(peeLineTotal);
        line.setTechUnitPrice(teeLineTotal);
        line.setTaxAmount(taxAmt);
        line.setTEELineTaxamt(teeTaxAmt);
        line.setPEELineTaxamt(peeTaxAmt);
        line.setTEEInitUnitprice(teeInitunitprice);
        line.setPEEInitUnitprice(peeInitunitprice);
        OBDal.getInstance().save(line);

      }

      // String qry = "select
      // COALESCE(sum(lineTotal),0),COALESCE(sum(techLineTotal),0),COALESCE(sum(taxAmount),0), "
      // + " COALESCE(sum(tEELineTaxamt),0), COALESCE(sum(pEELineTaxamt),0) "
      // + " ,
      // COALESCE(sum(tEEInitUnitprice*techLineQty),0),COALESCE(sum(pEEInitUnitprice*pEEQty),0) "
      // + " ,parentLineNo.id from Escm_Proposalmgmt_Line "
      // + " where escmProposalmgmt.id=? and parentLineNo.id is not null and COALESCE((status),'')
      // <> 'CL'"
      // + " group by parentLineNo.id ";
      // Query query = OBDal.getInstance().getSession().createQuery(qry);
      // query.setParameter(0, proposalMgmt.getId());
      // if (query.list().size() > 0) {
      // for (Object resultObj : query.list()) {
      // final Object[] values = (Object[]) resultObj;
      // BigDecimal totalSumLineTotal = (BigDecimal) values[0];
      // BigDecimal totalSumTeeLineTotal = (BigDecimal) values[1];
      // BigDecimal totalSumofTaxAmt = (BigDecimal) values[2];
      // BigDecimal totalSumofTeeTaxAmt = (BigDecimal) values[3];
      // BigDecimal totalSumofPeeTaxAmt = (BigDecimal) values[4];
      // BigDecimal sumofTeeInitialUnitPRice = (BigDecimal) values[5];
      // BigDecimal sumofPeeInitialUnitPRice = (BigDecimal) values[6];
      // String parentLineId = (String) values[7];
      // String updateQry = " update Escm_Proposalmgmt_Line set grossUnitPrice =?, netprice =?
      // ,negotUnitPrice = ?"
      // + ", lineTotal = ? ,techLineTotal = ?, techUnitPrice = ?, taxAmount = ?,tEELineTaxamt = ?,"
      // + " pEELineTaxamt = ?,tEEInitUnitprice=?" + " ,pEEInitUnitprice=? where id = ? ";
      //
      // Query updateQuery = OBDal.getInstance().getSession().createQuery(updateQry);
      // updateQuery.setParameter(0, totalSumLineTotal);
      // updateQuery.setParameter(1, totalSumLineTotal);
      // updateQuery.setParameter(2, totalSumLineTotal);
      // updateQuery.setParameter(3, totalSumLineTotal);
      // updateQuery.setParameter(4, totalSumTeeLineTotal);
      // updateQuery.setParameter(5, totalSumTeeLineTotal);
      //
      // updateQuery.setParameter(6, totalSumofTaxAmt);
      // updateQuery.setParameter(7, totalSumofTeeTaxAmt);
      // updateQuery.setParameter(8, totalSumofPeeTaxAmt);
      // updateQuery.setParameter(9, sumofTeeInitialUnitPRice);
      // updateQuery.setParameter(10, sumofPeeInitialUnitPRice);
      // updateQuery.setParameter(11, parentLineId);
      // updateQuery.executeUpdate();
      //
      // }
      // }
      // String peeTotalQry = " select sum(pEELineTotal),parentLineNo.id "
      // + " from Escm_Proposalmgmt_Line where escmProposalmgmt.id=? "
      // + " and parentLineNo.id is not null and (peestatus is null or peestatus !='CL') "
      // + " group by parentLineNo.id";
      // Query peeTotalQryUpdate = OBDal.getInstance().getSession().createQuery(peeTotalQry);
      // peeTotalQryUpdate.setParameter(0, proposalMgmt.getId());
      // if (peeTotalQryUpdate.list().size() > 0) {
      // for (Object resultObj : peeTotalQryUpdate.list()) {
      // final Object[] values = (Object[]) resultObj;
      // BigDecimal totPeeLineTotal = (BigDecimal) values[0];
      // String parentLineId = (String) values[1];
      // String updateQry = " update Escm_Proposalmgmt_Line set pEELineTotal = ?, "
      // + " pEENegotUnitPrice = ? where id = ? ";
      // Query updateQuery = OBDal.getInstance().getSession().createQuery(updateQry);
      // updateQuery.setParameter(0, totPeeLineTotal);
      // updateQuery.setParameter(1, totPeeLineTotal);
      // updateQuery.setParameter(2, parentLineId);
      // updateQuery.executeUpdate();
      // }
      // }

    } catch (Exception e) {
      log4j.error("Exception in updateParentRecord() ", e);
    }
  }

  @Override
  public Boolean isPriceInclusiveOfTax(EscmProposalMgmt proposalMgmt) {
    Boolean isInclusive = Boolean.FALSE;

    try {
      if (proposalMgmt != null) {
        isInclusive = proposalMgmt.getEfinTaxMethod().isPriceIncludesTax();
      }
    } catch (Exception e) {
      log4j.error("Exception in isPriceInclusiveOfTax() ", e);
    }
    return isInclusive;
  }

  @Override
  public List<EscmProposalmgmtLine> getProposalLines(EscmProposalMgmt proposalMgmt) {
    List<EscmProposalmgmtLine> lines = null;
    try {
      if (proposalMgmt != null) {
        lines = proposalMgmt.getEscmProposalmgmtLineList().stream()
            .filter(a -> a.getLineTotal().compareTo(BigDecimal.ZERO) > 0 && !a.isSummary())
            .collect(Collectors.toList());
      }
    } catch (Exception e) {
      log4j.error("Exception while getLines: ", e);
    }
    return lines;
  }

  @Override
  public JSONObject calculateTax(EscmProposalmgmtLine proposalLine) {
    JSONObject taxObject = new JSONObject();
    BigDecimal taxPercent = BigDecimal.ZERO;
    BigDecimal taxFactor = BigDecimal.ONE;
    BigDecimal taxAmount = BigDecimal.ZERO;
    BigDecimal amtwithoutTax = BigDecimal.ZERO;
    BigDecimal lineTotal = BigDecimal.ZERO;
    BigDecimal quantity = BigDecimal.ZERO;
    BigDecimal negotiatedamt = BigDecimal.ZERO;
    EscmProposalMgmt proposalMgmt = null;

    try {
      if (proposalLine.getEscmProposalmgmt().getEfinTaxMethod() != null
          && proposalLine.getEscmProposalmgmt().getEfinTaxMethod().isPriceIncludesTax()) {
        lineTotal = ((proposalLine.getNegotUnitPrice().add(proposalLine.getUnittax()))
            .multiply(proposalLine.getMovementQuantity())).add(proposalLine.getRounddiffTax()); // getting
                                                                                                // error
                                                                                                // while
                                                                                                // calculate
                                                                                                // tax
        // again
        // and again in proposal screen,so adding diff while calcluating tax once again ex: gross
        // unit price 30
        // and qty is 2
      } else {
        lineTotal = proposalLine.getNegotUnitPrice().multiply(proposalLine.getMovementQuantity());

      }

      proposalMgmt = proposalLine.getEscmProposalmgmt();
      taxPercent = getTaxPercent(proposalMgmt);
      quantity = proposalLine.getMovementQuantity();

      if (isPriceInclOfTax) {
        taxFactor = taxFactor.add(taxPercent);
        amtwithoutTax = lineTotal.divide(taxFactor, 15, RoundingMode.HALF_UP);
        taxAmount = (lineTotal.subtract(amtwithoutTax)).setScale(roundoffConst,
            RoundingMode.HALF_UP);
        negotiatedamt = amtwithoutTax.divide(quantity, roundoffConst, RoundingMode.HALF_UP);
        taxObject.put("LineAmount", negotiatedamt);

      } else {
        taxAmount = lineTotal.multiply(taxPercent);
        taxObject.put("LineAmount", lineTotal.add(taxAmount));
      }
      taxObject.put("TaxAmount", taxAmount);

    } catch (Exception e) {
      try {
        taxObject.put("LineAmount", BigDecimal.ZERO);
        taxObject.put("TaxAmount", BigDecimal.ZERO);
      } catch (JSONException e1) {
        log4j.error("Exception in calculateTax() ", e1);
      }
      log4j.error("Exception in calculateTax() ", e);
    }

    return taxObject;
  }

  public BigDecimal getOriginalLineAmount(EscmProposalmgmtLine proposalLine) {
    BigDecimal lineNetAmt = BigDecimal.ZERO;
    BigDecimal oldAmount = BigDecimal.ZERO, taxAmt = BigDecimal.ZERO;
    try {
      taxAmt = proposalLine.getTaxAmount();

      if (isPriceInclOfTax) {
        oldAmount = proposalLine.getNegotUnitPrice();
        lineNetAmt = oldAmount.add(taxAmt);
      } else {
        oldAmount = proposalLine.getLineTotal();
        lineNetAmt = oldAmount.subtract(taxAmt);
      }

    } catch (Exception e) {
      log4j.error("Exception in getOriginalLineNetAmount() ", e);
    }
    return lineNetAmt;
  }

  @Override
  public BigDecimal getTaxPercent(EscmProposalMgmt proposalMgmt) {
    BigDecimal taxPercent = BigDecimal.ZERO, tax = BigDecimal.ZERO;
    try {

      if (proposalMgmt != null) {
        tax = new BigDecimal(proposalMgmt.getEfinTaxMethod().getTaxpercent());
        taxPercent = tax.multiply(PERCENT);
      }

    } catch (Exception e) {
      log4j.error("Exception in getTaxPercent() ", e);
    }

    return taxPercent;
  }

  @Override
  public Boolean checkproposalTaxCalculated(EscmProposalMgmt proposalMgmt) {
    Boolean recalculateTax = Boolean.FALSE;
    try {
      List<EscmProposalmgmtLine> taxableLines = new ArrayList<EscmProposalmgmtLine>();

      if (proposalMgmt.isTaxLine() != null && proposalMgmt.isTaxLine()) {
        taxableLines = getProposalLines(proposalMgmt);

        if (taxableLines.size() > 0) {
          for (EscmProposalmgmtLine line : taxableLines) {
            if ((line.getTaxAmount() == null)
                || (line.getTaxAmount().compareTo(BigDecimal.ZERO) == 0)) {
              recalculateTax = Boolean.TRUE;
              break;
            }
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in checkproposalTaxCalculated() ", e);
    }

    return recalculateTax;
  }
}