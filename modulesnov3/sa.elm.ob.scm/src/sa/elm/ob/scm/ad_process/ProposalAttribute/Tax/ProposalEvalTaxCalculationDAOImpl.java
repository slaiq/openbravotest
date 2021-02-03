package sa.elm.ob.scm.ad_process.ProposalAttribute.Tax;

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
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;

import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;

/**
 * @author Poongodi on 19/06/2019
 */

public class ProposalEvalTaxCalculationDAOImpl implements ProposalEvalTaxCalculationDAO {
  private static final Logger log4j = Logger.getLogger(ProposalEvalTaxCalculationDAOImpl.class);
  private Boolean isPriceInclOfTax = Boolean.FALSE;

  BigDecimal PERCENT = new BigDecimal("0.01");
  HttpServletRequest request = RequestContext.get().getRequest();
  Integer roundoffConst = 3;

  @Override
  public OBError insertTaxAmount(EscmProposalAttribute proposalAttr, EscmProposalMgmt proposalMgmt,
      Integer decimalFormat) {
    OBError result = new OBError();
    BigDecimal taxAmount = BigDecimal.ZERO;
    BigDecimal lineAmount = BigDecimal.ZERO;

    try {
      if (proposalMgmt != null) {

        List<EscmProposalmgmtLine> taxableLines = new ArrayList<EscmProposalmgmtLine>();

        isPriceInclOfTax = isPriceInclusiveOfTax(proposalMgmt);

        taxableLines = getProposalLines(proposalMgmt);
        roundoffConst = decimalFormat;

        if (taxableLines.size() > 0) {
          for (EscmProposalmgmtLine line : taxableLines) {

            JSONObject taxObject = calculateTax(line);
            taxAmount = new BigDecimal(taxObject.get("TaxAmount").toString());
            lineAmount = new BigDecimal(taxObject.get("LineAmount").toString());

            if (isPriceInclOfTax) {
              line.setPEENegotUnitPrice(lineAmount);
              line.setPEELineTotal((lineAmount.multiply(line.getPEEQty())).add(taxAmount));
            } else {
              line.setPEELineTotal(lineAmount);
            }
            line.setPEELineTaxamt(taxAmount);
            proposalAttr.setPEECalculateTaxlines(true);

            OBDal.getInstance().save(line);
            OBDal.getInstance().save(proposalAttr);

          }

          OBDal.getInstance().flush();
          OBDal.getInstance().commitAndClose();
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

  @Override
  public Boolean isPriceInclusiveOfTax(EscmProposalMgmt proposalMgmt) {
    Boolean isInclusive = Boolean.FALSE;
    EscmProposalAttribute Attr = null;
    try {
      if (proposalMgmt != null) {

        OBQuery<EscmProposalAttribute> proposalAttr = OBDal.getInstance().createQuery(
            EscmProposalAttribute.class, "as e where e.escmProposalmgmt.id = :proposalID ");
        proposalAttr.setNamedParameter("proposalID", proposalMgmt.getId());
        if (proposalAttr != null && proposalAttr.list().size() > 0) {
          Attr = proposalAttr.list().get(0);

        }
        isInclusive = Attr.getPEEEfinTaxMethod().isPriceIncludesTax();

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
      if (isPriceInclOfTax) {
        lineTotal = (proposalLine.getPEENegotUnitPrice().multiply(proposalLine.getPEEQty()))
            .add(proposalLine.getPEELineTaxamt());
        // lineTotal = proposalLine.getPEEInitUnitprice().multiply(proposalLine.getPEEQty())
        // .subtract(proposalLine.getPEETechDiscountamt());
      } else {
        // lineTotal = (proposalLine.getPEEInitUnitprice().multiply(proposalLine.getPEEQty()))
        // .subtract(proposalLine.getTaxAmount()).subtract(proposalLine.getPEETechDiscountamt());
        lineTotal = (proposalLine.getPEENegotUnitPrice().multiply(proposalLine.getPEEQty()));
      }

      proposalMgmt = proposalLine.getEscmProposalmgmt();
      if (proposalLine.getEscmProposalmgmt().isTaxLine()) {
        taxPercent = getTaxPercent(proposalMgmt);
      } else {
        taxPercent = getTaxPercentFromProposalAttr(proposalMgmt);
      }
      quantity = proposalLine.getPEEQty();

      if (isPriceInclOfTax) {
        taxFactor = taxFactor.add(taxPercent);
        amtwithoutTax = lineTotal.divide(taxFactor, roundoffConst, RoundingMode.HALF_UP);
        taxAmount = lineTotal.subtract(amtwithoutTax);
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
        e1.printStackTrace();
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

      if (proposalMgmt != null && proposalMgmt.getEfinTaxMethod() != null) {
        tax = new BigDecimal(proposalMgmt.getEfinTaxMethod().getTaxpercent());
        taxPercent = tax.multiply(PERCENT);
      }

    } catch (Exception e) {
      log4j.error("Exception in getTaxPercent() ", e);
    }

    return taxPercent;
  }

  @Override
  public BigDecimal getTaxPercentFromProposalAttr(EscmProposalMgmt proposalMgmt) {
    BigDecimal taxPercent = BigDecimal.ZERO, tax = BigDecimal.ZERO;
    try {
      EscmProposalAttribute Attr = null;
      if (proposalMgmt != null) {
        OBQuery<EscmProposalAttribute> proposalAttr = OBDal.getInstance().createQuery(
            EscmProposalAttribute.class, "as e where e.escmProposalmgmt.id = :proposalID ");
        proposalAttr.setNamedParameter("proposalID", proposalMgmt.getId());
        if (proposalAttr != null && proposalAttr.list().size() > 0) {
          Attr = proposalAttr.list().get(0);

        }
        tax = new BigDecimal(Attr.getPEEEfinTaxMethod().getTaxpercent());
        taxPercent = tax.multiply(PERCENT);
      }

    } catch (Exception e) {
      log4j.error("Exception in getTaxPercentFromProposalAttr() ", e);
    }

    return taxPercent;
  }

}