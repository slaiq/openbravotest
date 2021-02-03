package sa.elm.ob.scm.ad_callouts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;

import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmTechnicalevlEvent;

public class EscmProposalAttributeCallout extends SimpleCallout {
  private static Logger log = Logger.getLogger(EscmProposalAttributeCallout.class);
  private static final String infoMessage = "Escm_DiscountChanged_AfterTax";

  /**
   * Callout to netprice and discountamout
   */
  private static final long serialVersionUID = 1L;

  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpgrossUnitPrice = vars.getNumericParameter("inpgrossPrice");
    String inptechNegotiatedPrice = vars.getNumericParameter("inptechNegotiatedPrice");
    log4j.debug("inptechNegotiatedPrice" + inptechNegotiatedPrice);
    String inpescmProposalAttrId = vars.getStringParameter("inpescmProposalAttrId");
    String inpTeeIstax = vars.getStringParameter("inpteeIstax");
    String inpPeeIstax = vars.getStringParameter("inppeeIstax");
    DecimalFormat euroRelationFmt = org.openbravo.erpCommon.utility.Utility.getFormat(vars,
        "euroRelation");
    Integer roundOffconst = euroRelationFmt.getMaximumFractionDigits();

    String inpescmTechnicalevlEventId = vars.getStringParameter("inpescmTechnicalevlEventId");

    BigDecimal unitprice = BigDecimal.ZERO;
    BigDecimal negoprice = BigDecimal.ZERO;
    BigDecimal peeTotal = BigDecimal.ZERO;
    BigDecimal taxamt = BigDecimal.ZERO;
    BigDecimal taxTotal = BigDecimal.ZERO;
    String inpdiscount = vars.getStringParameter("inpdiscount");
    if (!inpgrossUnitPrice.equals(""))
      unitprice = new BigDecimal(inpgrossUnitPrice);

    BigDecimal discount = BigDecimal.ZERO;
    BigDecimal tech_discount = BigDecimal.ZERO;
    BigDecimal pee_tech_discount = BigDecimal.ZERO;
    String inptechnicalDiscount = vars.getStringParameter("inptechnicalDiscount");
    String inptechnicalDiscountamt = vars.getStringParameter("inptechnicalDiscountamt");
    String inppeeTechDiscountamt = vars.getStringParameter("inppeeTechDiscountamt");
    BigDecimal changed_tee_discount_amount = BigDecimal.ZERO;
    BigDecimal changed_pee_discount_amount = BigDecimal.ZERO;
    if (!inpdiscount.equals("")) {
      discount = new BigDecimal(inpdiscount);
    }
    if (!inptechnicalDiscount.equals("")) {
      tech_discount = new BigDecimal(inptechnicalDiscount);
    }
    if (!inptechnicalDiscountamt.equals("")) {
      changed_tee_discount_amount = new BigDecimal(inptechnicalDiscountamt.replace(",", ""));
    }
    if (!inppeeTechDiscountamt.equals("")) {
      changed_pee_discount_amount = new BigDecimal(inppeeTechDiscountamt.replace(",", ""));
    }
    String inppeeTechDiscount = vars.getStringParameter("inppeeTechDiscount");
    if (!inppeeTechDiscount.equals("")) {
      pee_tech_discount = new BigDecimal(inppeeTechDiscount);
    }
    BigDecimal difference = BigDecimal.ZERO;
    BigDecimal netPrice = BigDecimal.ZERO;
    BigDecimal discountamt = BigDecimal.ZERO;
    String parsedMessage = null;

    try {
      if (inpLastFieldChanged.equals("inpgrossPrice")
          || inpLastFieldChanged.equals("inpdiscount")) {
        if (!inpdiscount.equals("")) {
          discountamt = unitprice.subtract(unitprice
              .multiply(new BigDecimal(100).subtract(discount).divide(new BigDecimal(100))));
          netPrice = unitprice.subtract(discountamt);

        } else {
          netPrice = unitprice;
        }

        info.addResult("inpnetprice", netPrice);
        info.addResult("inpdiscountamt", discountamt);
      }
      if (inpLastFieldChanged.equals("inptechNegotiatedPrice")) {

        EscmTechnicalevlEvent techevent = OBDal.getInstance().get(EscmTechnicalevlEvent.class,
            inpescmTechnicalevlEventId);
        if (techevent.getEstimatedPrice() != null
            && (techevent.getEstimatedPrice().compareTo(BigDecimal.ZERO) > 0)) {
          difference = (techevent.getEstimatedPrice()
              .subtract(new BigDecimal(inptechNegotiatedPrice))
              .divide(techevent.getEstimatedPrice(), 3, RoundingMode.FLOOR))
                  .multiply(new BigDecimal(100));
          info.addResult("inptechVariation", difference);
        }

      }

      if (inpLastFieldChanged.equals("inptechnicalDiscountamt")
          || inpLastFieldChanged.equals("inptechnicalDiscount")) {
        EscmProposalAttribute attr = OBDal.getInstance().get(EscmProposalAttribute.class,
            inpescmProposalAttrId);
        if (attr.getTEETotalTaxamt().compareTo(BigDecimal.ZERO) > 0) {
          parsedMessage = Utility.messageBD(this, infoMessage,
              OBContext.getOBContext().getLanguage().getId());
          info.addResult("INFO", parsedMessage);
        }

      }
      if (inpLastFieldChanged.equals("inptechnicalDiscount")) {

        if (!inptechnicalDiscount.equals("") && tech_discount.compareTo(BigDecimal.ZERO) != 0) {// inptechprice
                                                                                                // //unitprice
          EscmProposalAttribute attr = OBDal.getInstance().get(EscmProposalAttribute.class,
              inpescmProposalAttrId);
          /*
           * discountamt = attr.getTechNegotiatedPrice().subtract(attr.getTechNegotiatedPrice()
           * .multiply(new BigDecimal(100).subtract(tech_discount).divide(new BigDecimal(100))));
           */
          for (EscmProposalmgmtLine objTechLine : attr.getEscmProposalmgmt()
              .getEscmProposalmgmtLineList()) {
            if (!objTechLine.isSummary()) {
              if (attr.getTEEEfinTaxMethod() != null
                  && !attr.getTEEEfinTaxMethod().isPriceIncludesTax()) {
                negoprice = negoprice
                    .add(objTechLine.getTechLineQty().multiply(objTechLine.getTEEInitUnitprice()));
              } else {
                negoprice = negoprice
                    .add(objTechLine.getTechLineQty().multiply(objTechLine.getNetUnitprice()));
              }

            }

          }
          /*
           * discountamt = attr.getNetPrice().subtract(attr.getNetPrice() .multiply(new
           * BigDecimal(100).subtract(tech_discount).divide(new BigDecimal(100))));
           */
          discountamt = negoprice.subtract(negoprice
              .multiply(new BigDecimal(100).subtract(tech_discount).divide(new BigDecimal(100))));

          netPrice = negoprice.subtract(discountamt);
          info.addResult("inptechNegotiatedPrice", netPrice);
          info.addResult("inptechnicalDiscountamt", discountamt);

        } else {
          EscmProposalAttribute attr = OBDal.getInstance().get(EscmProposalAttribute.class,
              inpescmProposalAttrId);
          for (EscmProposalmgmtLine objTechLine : attr.getEscmProposalmgmt()
              .getEscmProposalmgmtLineList()) {
            if (!objTechLine.isSummary()) {
              negoprice = negoprice
                  .add(objTechLine.getTechLineQty().multiply(objTechLine.getNetUnitprice()));
            }
          }
          info.addResult("inptechNegotiatedPrice", negoprice);
          info.addResult("inptechnicalDiscount", 0);
          info.addResult("inptechnicalDiscountamt", 0);
        }

      }

      else if (inpLastFieldChanged.equals("inptechnicalDiscountamt")) {

        if (!inptechnicalDiscountamt.equals("")
            && changed_tee_discount_amount.compareTo(BigDecimal.ZERO) != 0) {
          EscmProposalAttribute attr = OBDal.getInstance().get(EscmProposalAttribute.class,
              inpescmProposalAttrId);

          for (EscmProposalmgmtLine objTechLine : attr.getEscmProposalmgmt()
              .getEscmProposalmgmtLineList()) {
            if (!objTechLine.isSummary()) {
              if (attr.getTEEEfinTaxMethod() != null
                  && !attr.getTEEEfinTaxMethod().isPriceIncludesTax()) {
                negoprice = negoprice
                    .add(objTechLine.getTechLineQty().multiply(objTechLine.getTEEInitUnitprice()));
              } else {
                negoprice = negoprice
                    .add(objTechLine.getTechLineQty().multiply(objTechLine.getNetUnitprice()));
              }

            }

          }

          tech_discount = (changed_tee_discount_amount.multiply(new BigDecimal(100)))
              .divide(negoprice, 2, RoundingMode.HALF_EVEN);

          netPrice = negoprice.subtract(changed_tee_discount_amount);
          info.addResult("inptechNegotiatedPrice", netPrice);
          info.addResult("inptechnicalDiscount", tech_discount);

        } else {
          EscmProposalAttribute attr = OBDal.getInstance().get(EscmProposalAttribute.class,
              inpescmProposalAttrId);
          for (EscmProposalmgmtLine objTechLine : attr.getEscmProposalmgmt()
              .getEscmProposalmgmtLineList()) {
            if (!objTechLine.isSummary()) {
              negoprice = negoprice
                  .add(objTechLine.getTechLineQty().multiply(objTechLine.getNetUnitprice()));
            }
          }
          info.addResult("inptechNegotiatedPrice", negoprice);
          info.addResult("inptechnicalDiscount", 0);
          info.addResult("inptechnicalDiscountamt", 0);
        }

      }
      if (inpLastFieldChanged.equals("inppeeTechDiscount")
          || inpLastFieldChanged.equals("inppeeTechDiscountamt")) {
        EscmProposalAttribute attr = OBDal.getInstance().get(EscmProposalAttribute.class,
            inpescmProposalAttrId);
        if (attr.getPEETotalTaxamt().compareTo(BigDecimal.ZERO) > 0) {
          parsedMessage = Utility.messageBD(this, infoMessage,
              OBContext.getOBContext().getLanguage().getId());
          info.addResult("INFO", parsedMessage);
        }

      }
      if (inpLastFieldChanged.equals("inppeeTechDiscount")) {

        EscmProposalAttribute attr = OBDal.getInstance().get(EscmProposalAttribute.class,
            inpescmProposalAttrId);
        for (EscmProposalmgmtLine objTechLine : attr.getEscmProposalmgmt()
            .getEscmProposalmgmtLineList()) {
          if (!objTechLine.isSummary()) {
            if (objTechLine.getPeestatus() == null || (!objTechLine.getPeestatus().equals("CL"))) {
              // changed 6607
              if (attr.getEscmProposalmgmt().getProposalType().equals("DR")) {
                peeTotal = objTechLine.getPEEQty().multiply(objTechLine.getNegotUnitPrice());
                if (attr.getPEEEfinTaxMethod() != null
                    && !attr.getPEEEfinTaxMethod().isPriceIncludesTax()) {
                  negoprice = negoprice.add(peeTotal);
                } else {
                  negoprice = negoprice.add(peeTotal.add(objTechLine.getPEELineTaxamt()));
                }

              } else {
                if ((objTechLine.getPEENegotUnitPrice()
                    .compareTo(objTechLine.getTechUnitPrice()) == 0)
                    && (objTechLine.getPEEQty().compareTo(objTechLine.getTechLineQty()) == 0)) {
                  if (objTechLine.getEscmProposalmgmt().getEfinTaxMethod() != null && !objTechLine
                      .getEscmProposalmgmt().getEfinTaxMethod().isPriceIncludesTax()) {
                    negoprice = negoprice.add(
                        objTechLine.getTechLineTotal().subtract(objTechLine.getTEELineTaxamt()));
                  } else {
                    negoprice = negoprice.add(objTechLine.getTechLineTotal());
                  }

                } else {
                  negoprice = negoprice
                      .add(objTechLine.getPEEQty().multiply(objTechLine.getNegotUnitPrice()));
                }
              }
            }
          }
        }
        if (!inppeeTechDiscount.equals("") && pee_tech_discount.compareTo(BigDecimal.ZERO) != 0) {

          discountamt = negoprice.subtract(negoprice.multiply(
              new BigDecimal(100).subtract(pee_tech_discount).divide(new BigDecimal(100))));
          netPrice = negoprice.subtract(discountamt);

          // changed 6607
          // if (!attr.getEscmProposalmgmt().getProposalType().equals("DR")) {
          // discountamt = negoprice.subtract(negoprice.multiply(
          // new BigDecimal(100).subtract(pee_tech_discount).divide(new BigDecimal(100))));
          // netPrice = negoprice.subtract(discountamt);
          // } else {
          // discountamt = peenetprice.subtract(peenetprice.multiply(
          // new BigDecimal(100).subtract(pee_tech_discount).divide(new BigDecimal(100))));
          // netPrice = peenetprice.subtract(discountamt);
          //
          // }

          info.addResult("inpnegotiatedPrice", netPrice);
          info.addResult("inppeeTechDiscountamt", discountamt);
        } else {
          // changed 6607
          info.addResult("inpnegotiatedPrice", negoprice);
          info.addResult("inppeeTechDiscountamt", 0);
          info.addResult("inppeeTechDiscount", 0);
        }

      } else if (inpLastFieldChanged.equals("inppeeTechDiscountamt")) {

        EscmProposalAttribute attr = OBDal.getInstance().get(EscmProposalAttribute.class,
            inpescmProposalAttrId);
        for (EscmProposalmgmtLine objTechLine : attr.getEscmProposalmgmt()
            .getEscmProposalmgmtLineList()) {
          if (!objTechLine.isSummary()) {
            if (objTechLine.getPeestatus() == null || (!objTechLine.getPeestatus().equals("CL"))) {
              // changed 6607
              if (attr.getEscmProposalmgmt().getProposalType().equals("DR")) {
                peeTotal = objTechLine.getPEEQty().multiply(objTechLine.getNegotUnitPrice());
                if (attr.getPEEEfinTaxMethod() != null
                    && !attr.getPEEEfinTaxMethod().isPriceIncludesTax()) {
                  negoprice = negoprice.add(peeTotal);
                } else {
                  negoprice = negoprice.add(peeTotal.add(objTechLine.getPEELineTaxamt()));
                }

              } else {
                if ((objTechLine.getPEENegotUnitPrice()
                    .compareTo(objTechLine.getTechUnitPrice()) == 0)
                    && (objTechLine.getPEEQty().compareTo(objTechLine.getTechLineQty()) == 0)) {
                  if (objTechLine.getEscmProposalmgmt().getEfinTaxMethod() != null && !objTechLine
                      .getEscmProposalmgmt().getEfinTaxMethod().isPriceIncludesTax()) {
                    negoprice = negoprice.add(
                        objTechLine.getTechLineTotal().subtract(objTechLine.getTEELineTaxamt()));
                  } else {
                    negoprice = negoprice.add(objTechLine.getTechLineTotal());
                  }

                } else {
                  negoprice = negoprice
                      .add(objTechLine.getPEEQty().multiply(objTechLine.getNegotUnitPrice()));
                }
              }
            }
          }
        }
        if (!inppeeTechDiscountamt.equals("")
            && changed_pee_discount_amount.compareTo(BigDecimal.ZERO) != 0) {

          pee_tech_discount = (changed_pee_discount_amount.multiply(new BigDecimal(100)))
              .divide(negoprice, 2, RoundingMode.HALF_EVEN);

          netPrice = negoprice.subtract(changed_pee_discount_amount);

          // changed 6607
          // if (!attr.getEscmProposalmgmt().getProposalType().equals("DR")) {
          // discountamt = negoprice.subtract(negoprice.multiply(
          // new BigDecimal(100).subtract(pee_tech_discount).divide(new BigDecimal(100))));
          // netPrice = negoprice.subtract(discountamt);
          // } else {
          // discountamt = peenetprice.subtract(peenetprice.multiply(
          // new BigDecimal(100).subtract(pee_tech_discount).divide(new BigDecimal(100))));
          // netPrice = peenetprice.subtract(discountamt);
          //
          // }

          info.addResult("inpnegotiatedPrice", netPrice);
          info.addResult("inppeeTechDiscount", pee_tech_discount);
        } else {
          // changed 6607
          info.addResult("inpnegotiatedPrice", negoprice);
          info.addResult("inppeeTechDiscountamt", 0);
          info.addResult("inppeeTechDiscount", 0);
        }

      }

      // Set tax method as null and tax value as 0 when istax is unchecked
      if (inpLastFieldChanged.equals("inpteeIstax")) {
        if (inpTeeIstax.equals("N")) {
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('tee_efin_tax_method_id').setValue('')");
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('tee_total_taxamt').setValue(0)");
        }
      }
      if (inpLastFieldChanged.equals("inppeeIstax")) {
        if (inpPeeIstax.equals("N")) {
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('pee_efin_tax_method_id').setValue('')");
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('pee_total_taxamt').setValue(0)");
        }
      }

    } catch (Exception e) {
      log.error("Exception in EscmProposalAttributeCallout:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }

  }
}
