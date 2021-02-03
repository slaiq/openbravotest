package sa.elm.ob.scm.ad_callouts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.ad_process.Requisition.RequisitionDao;

public class EscmProposalLines extends SimpleCallout {
  private static Logger log = Logger.getLogger(EscmProposalLines.class);
  private static final String infoMessage = "Escm_DiscountChanged_AfterTax";
  /**
   * Callout to netprice and line net amount
   */
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpgrossUnitPrice = vars.getNumericParameter("inpgrossUnitPrice");
    String inpmovementqty = vars.getNumericParameter("inpmovementqty");
    String inpdiscount = vars.getNumericParameter("inpdiscount");
    String inpProduct = vars.getStringParameter("inpmProductId");
    String inpnotProvided = vars.getStringParameter("inpnotprovided");
    String inpapprovalDateHijiri = vars.getStringParameter("inpapprovalDateHijiri");
    String uniqueCode = vars.getStringParameter("inpemEfinCValidcombinationId");
    String budgetInit = vars.getStringParameter("inpemEfinBudgetinitialId");

    String inptechLineQty = vars.getNumericParameter("inptechLineQty");
    String proposalId = vars.getStringParameter("Escm_Proposalmgmt_ID");
    String inptechUnitPrice = vars.getNumericParameter("inptechUnitPrice");
    String TechLineTotal = vars.getNumericParameter("inptechLineTotal");
    String inplineTaxamt = vars.getNumericParameter("inplineTaxamt");
    String proposalLineId = vars.getStringParameter("inpescmProposalmgmtLineId");
    String openenvlpnetprice = vars.getNumericParameter("inpnetprice");
    String inppeeqty = vars.getNumericParameter("inppeeQty");
    String inppeeNegUnitPrice = vars.getNumericParameter("inppeeNegotUnitPrice");
    String inppropNegUnitPrice = vars.getNumericParameter("inpnegotUnitPrice");
    String inptechnicalDiscount = vars.getNumericParameter("inptechDiscount");
    String inptechnicalDiscountamt = vars.getNumericParameter("inptechDiscountamt");
    String inppeeTechDiscountamt = vars.getNumericParameter("inppeeTechDiscountamt");

    String inpteeLineTaxamt = vars.getNumericParameter("inpteeLineTaxamt");
    String inppeeLineTaxamt = vars.getNumericParameter("inppeeLineTaxamt");
    String inpdiscountmount = vars.getNumericParameter("inpdiscountmount");
    BigDecimal inptechNegotiatedPrice = BigDecimal.ZERO;
    BigDecimal inpTeeLineTaxamt = BigDecimal.ZERO;
    BigDecimal inpPeeLineTaxamt = BigDecimal.ZERO;
    BigDecimal taxamt = BigDecimal.ZERO;
    BigDecimal technical_discount_amount = BigDecimal.ZERO;
    BigDecimal pee_discount_amount = BigDecimal.ZERO;
    String parsedMessage = null;
    DecimalFormat euroRelationFmt = org.openbravo.erpCommon.utility.Utility.getFormat(vars,
        "euroRelation");
    Integer roundOffconst = euroRelationFmt.getMaximumFractionDigits();

    if (!inptechLineQty.equals("") && !inptechUnitPrice.equals("")) {
      inptechNegotiatedPrice = new BigDecimal(inptechLineQty)
          .multiply(new BigDecimal(inptechUnitPrice));
    }

    BigDecimal inptechprice = BigDecimal.ZERO;
    BigDecimal inptechqty = BigDecimal.ZERO;
    BigDecimal inptechunitprice = BigDecimal.ZERO;
    BigDecimal linenetprice = BigDecimal.ZERO;
    BigDecimal peeqty = BigDecimal.ZERO;
    BigDecimal inppeeNegotUnitPrice = BigDecimal.ZERO;
    BigDecimal inpdisamount = BigDecimal.ZERO;
    BigDecimal inpproposalNegotUnitPrice = BigDecimal.ZERO;
    BigDecimal pee_TaxAmt = BigDecimal.ZERO;
    BigDecimal proposal_TaxAmt = BigDecimal.ZERO;

    if (!inptechnicalDiscountamt.equals("")) {

      technical_discount_amount = new BigDecimal(inptechnicalDiscountamt.replace(",", ""));
    }
    if (!inppeeTechDiscountamt.equals("")) {
      pee_discount_amount = new BigDecimal(inppeeTechDiscountamt.replace(",", ""));
    }
    if (!inppeeNegUnitPrice.equals("")) {
      inppeeNegotUnitPrice = new BigDecimal(inppeeNegUnitPrice);
    }
    if (!inppropNegUnitPrice.equals("")) {
      inpproposalNegotUnitPrice = new BigDecimal(inppropNegUnitPrice);
    }
    if (!inppeeqty.equals("")) {
      peeqty = new BigDecimal(inppeeqty);
    }
    if (!inplineTaxamt.equals("")) {
      proposal_TaxAmt = new BigDecimal(inplineTaxamt);
    }

    if (!openenvlpnetprice.equals("")) {
      linenetprice = new BigDecimal(openenvlpnetprice);
    }
    if (!inptechLineQty.equals("")) {
      inptechqty = new BigDecimal(inptechLineQty);
    }
    if (!inptechUnitPrice.equals("")) {
      inptechunitprice = new BigDecimal(inptechUnitPrice);
    }
    if (inptechNegotiatedPrice.compareTo(BigDecimal.ZERO) != 0) {
      inptechprice = inptechNegotiatedPrice;
    }
    if (!inpdiscountmount.equals("")) {
      inpdisamount = new BigDecimal(inpdiscountmount);
    }
    if (!inpteeLineTaxamt.equals("")) {
      inpTeeLineTaxamt = new BigDecimal(inpteeLineTaxamt);
    }
    if (!inppeeLineTaxamt.equals("")) {
      inpPeeLineTaxamt = new BigDecimal(inppeeLineTaxamt);
    }

    BigDecimal unitprice = new BigDecimal(inpgrossUnitPrice);
    BigDecimal negotiatedunitPrice = new BigDecimal(vars.getNumericParameter("inpnegotUnitPrice"));
    BigDecimal qty = new BigDecimal(inpmovementqty);
    BigDecimal discount = BigDecimal.ZERO;
    BigDecimal fundsAvailable = BigDecimal.ZERO;
    PreparedStatement st = null;
    ResultSet rs = null;
    BigDecimal taxAmount = BigDecimal.ZERO;

    BigDecimal tech_discount = BigDecimal.ZERO;
    if (!inptechnicalDiscount.equals("")) {
      tech_discount = new BigDecimal(inptechnicalDiscount);
    }

    if (!inpdiscount.equals("")) {
      discount = new BigDecimal(inpdiscount);
    }
    BigDecimal netPrice = BigDecimal.ZERO;
    BigDecimal lineNet = BigDecimal.ZERO;
    BigDecimal discountamt = BigDecimal.ZERO;
    BigDecimal techLineNet = BigDecimal.ZERO;
    BigDecimal pee_tech_discount = BigDecimal.ZERO;
    BigDecimal teeLineTotal = BigDecimal.ZERO;
    BigDecimal TeeTaxAmt = BigDecimal.ZERO;
    OBContext.setAdminMode();
    String inppeeTechDiscount = vars.getStringParameter("inppeeTechDiscount");
    if (!inppeeTechDiscount.equals("")) {
      pee_tech_discount = new BigDecimal(inppeeTechDiscount);
    }

    if (!TechLineTotal.equals("")) {
      teeLineTotal = new BigDecimal(TechLineTotal);
    }

    try {

      if (inpLastFieldChanged.equals("inpgrossUnitPrice")
          || inpLastFieldChanged.equals("inpmovementqty")) {
        netPrice = unitprice;
        negotiatedunitPrice = netPrice;
        lineNet = negotiatedunitPrice.multiply(qty);
        info.addResult("inpnetprice", netPrice);
        info.addResult("inpnegotUnitPrice", negotiatedunitPrice);
        info.addResult("inpunitpricedis", netPrice);
        info.addResult("inplineTotal", lineNet);
        info.addResult("inptechBaseQty", qty);
        info.addResult("inptechLineQty", qty);
        info.addResult("inptechUnitPrice", negotiatedunitPrice);
        info.addResult("inpdiscountmount", BigDecimal.ZERO);
        info.addResult("inpdiscount", BigDecimal.ZERO);
        info.addResult("inpproposalDiscount", BigDecimal.ZERO);
        info.addResult("inpproposalDiscountAmount", BigDecimal.ZERO);
        teeLineTotal = lineNet
            .subtract((tech_discount.divide(new BigDecimal(100))).multiply(lineNet));
        info.addResult("inptechLineTotal", teeLineTotal);
        discountamt = (tech_discount.divide(new BigDecimal(100))).multiply(lineNet);
        info.addResult("inptechDiscountamt", discountamt);
        EscmProposalmgmtLine proposalmgmtLine = OBDal.getInstance().get(EscmProposalmgmtLine.class,
            proposalLineId);
        if (proposalmgmtLine != null && proposalmgmtLine.getEscmProposalmgmt().getTotalTaxAmount()
            .compareTo(BigDecimal.ZERO) > 0) {
          parsedMessage = Utility.messageBD(this, infoMessage,
              OBContext.getOBContext().getLanguage().getId());
          info.addResult("INFO", parsedMessage);
        }
        if (proposalmgmtLine != null
            && proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod() != null
            && proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod().isPriceIncludesTax()) {
          info.addResult("inplineTaxamt", 0);
        }

      }
      if (inpLastFieldChanged.equals("inppeeQty")
          || inpLastFieldChanged.equals("inppeeNegotUnitPrice")
          || inpLastFieldChanged.equals("inppeeTechDiscount")
          || inpLastFieldChanged.equals("inppeeTechDiscountamt")) {
        EscmProposalmgmtLine proposalmgmtLine = OBDal.getInstance().get(EscmProposalmgmtLine.class,
            proposalLineId);
        if (proposalmgmtLine != null
            && proposalmgmtLine.getPEELineTaxamt().compareTo(BigDecimal.ZERO) > 0) {
          parsedMessage = Utility.messageBD(this, infoMessage,
              OBContext.getOBContext().getLanguage().getId());
          info.addResult("INFO", parsedMessage);
        }
      }
      if (inpLastFieldChanged.equals("inppeeQty")) {// ||
        // inpLastFieldChanged.equals("inpdiscount")

        if (StringUtils.isNotEmpty(inppeeTechDiscount)
            && pee_tech_discount.compareTo(BigDecimal.ZERO) != 0) {
          EscmProposalmgmtLine proposalmgmtLine = OBDal.getInstance()
              .get(EscmProposalmgmtLine.class, proposalLineId);
          if (!proposalmgmtLine.getEscmProposalmgmt().getProposalType().equals("DR")) {
            if (proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod() != null
                && proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod().isPriceIncludesTax()) {

              pee_TaxAmt = proposalmgmtLine.getPEELineTaxamt().divide(proposalmgmtLine.getPEEQty(),
                  roundOffconst, RoundingMode.HALF_UP);
              taxamt = proposalmgmtLine.getTEELineTaxamt().divide(proposalmgmtLine.getTechLineQty(),
                  roundOffconst, RoundingMode.HALF_UP);
            }
            if ((inppeeNegotUnitPrice.compareTo(proposalmgmtLine.getTechUnitPrice()) == 0)
                && (peeqty.compareTo(proposalmgmtLine.getTechLineQty()) == 0)) {
              discountamt = (proposalmgmtLine.getTechLineTotal())
                  .multiply(pee_tech_discount.divide(new BigDecimal(100)));
              netPrice = (proposalmgmtLine.getTechLineTotal()).subtract(discountamt);

            } else {
              discountamt = (proposalmgmtLine.getTechUnitPrice().add(taxamt).multiply(peeqty))
                  .subtract(inppeeNegotUnitPrice.add(pee_TaxAmt).multiply(peeqty));
              netPrice = (inppeeNegotUnitPrice.add(pee_TaxAmt).multiply(peeqty));
            }
            info.addResult("inppeeLineTaxamt", pee_TaxAmt.multiply(peeqty));
            info.addResult("inppeeLineTotal", netPrice);
            info.addResult("inppeeTechDiscountamt", discountamt);
          } else {
            discountamt = (proposalmgmtLine.getNetprice().multiply(peeqty))
                .multiply(pee_tech_discount.divide(new BigDecimal(100)));
            // netPrice = (proposalmgmtLine.getNetprice().multiply(peeqty)).subtract(discountamt);
            netPrice = (inppeeNegotUnitPrice.multiply(peeqty));
            taxAmount = (proposalmgmtLine.getPEELineTaxamt().divide(proposalmgmtLine.getPEEQty(),
                roundOffconst, RoundingMode.HALF_UP)).multiply(peeqty);

            info.addResult("inppeeLineTaxamt", taxAmount);// taxAmount
            info.addResult("inppeeLineTotal", netPrice.add(taxAmount));// taxAmount
            info.addResult("inppeeTechDiscountamt", discountamt);
          }

        } else {
          if (StringUtils.isNotEmpty(inppeeTechDiscount)) {

          }

          /*
           * lineNet = negotiatedunitPrice.multiply(qty); info.addResult("inplineTotal", lineNet);
           */

          EscmProposalmgmtLine proposalmgmtLine = OBDal.getInstance()
              .get(EscmProposalmgmtLine.class, proposalLineId);
          if (!proposalmgmtLine.getEscmProposalmgmt().getProposalType().equals("DR")) {
            if ((inppeeNegotUnitPrice.compareTo(proposalmgmtLine.getTechUnitPrice()) == 0)
                && (peeqty.compareTo(proposalmgmtLine.getTechLineQty()) == 0)) {
              info.addResult("inppeeLineTotal", proposalmgmtLine.getTechLineTotal());
              lineNet = proposalmgmtLine.getTechLineTotal();
            } else {
              taxAmount = (proposalmgmtLine.getPEELineTaxamt().divide(proposalmgmtLine.getPEEQty(),
                  roundOffconst, RoundingMode.HALF_UP)).multiply(peeqty);

              info.addResult("inppeeLineTaxamt", taxAmount);
              lineNet = inppeeNegotUnitPrice.multiply(peeqty);
              info.addResult("inppeeLineTotal", lineNet.add(taxAmount));

            }

          } else {

            // Changing the qty calculate pee tax
            // Find the diff qty b/w proposal to pee

            taxAmount = (proposalmgmtLine.getTaxAmount().divide(
                proposalmgmtLine.getMovementQuantity(), roundOffconst, RoundingMode.HALF_UP))
                    .multiply(peeqty);

            info.addResult("inppeeLineTaxamt", taxAmount);
            info.addResult("inppeeLineTotal",
                (peeqty.multiply(inppeeNegotUnitPrice)).add(taxAmount));
          }
        }

      }

      // if negotiated unitprice changes then calcualte netamount = negotiatedprice * qty
      if (inpLastFieldChanged.equals("inppeeNegotUnitPrice")) {
        EscmProposalmgmtLine proposalmgmtLine = OBDal.getInstance().get(EscmProposalmgmtLine.class,
            proposalLineId);
        // changed 6607
        if (proposalmgmtLine.getEscmProposalmgmt().getProposalType().equals("DR")) {
          lineNet = inppeeNegotUnitPrice.multiply(peeqty);

        } else {
          if ((inppeeNegotUnitPrice.compareTo(proposalmgmtLine.getTechUnitPrice()) == 0)
              && peeqty.compareTo(proposalmgmtLine.getMovementQuantity()) == 0) {
            lineNet = proposalmgmtLine.getTechLineTotal();
          } else {
            lineNet = inppeeNegotUnitPrice.multiply(peeqty);
          }
        }
        info.addResult("inppeeLineTotal", lineNet);
        info.addResult("inppeeUnittax", BigDecimal.ZERO);
        if (proposalmgmtLine.getEscmProposalmgmt().getProposalType().equals("DR")) {
          if (proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod() != null
              && proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod().isPriceIncludesTax()) {
            taxAmount = proposalmgmtLine.getTaxAmount();
          } else {
            taxAmount = BigDecimal.ZERO;
          }

          discountamt = ((inpproposalNegotUnitPrice.multiply(peeqty)).add(taxAmount))
              .subtract(inppeeNegotUnitPrice.multiply(peeqty));
          discount = (discountamt
              .divide(((inpproposalNegotUnitPrice.multiply(peeqty)).add(taxAmount)), 15,
                  RoundingMode.HALF_UP)
              .multiply(new BigDecimal(100))).setScale(roundOffconst, RoundingMode.HALF_UP);

        } else {
          if (inppeeNegotUnitPrice.compareTo(proposalmgmtLine.getTechUnitPrice()) == 0) {
            discountamt = BigDecimal.ZERO;
            discount = BigDecimal.ZERO;
          } else {
            if (proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod() != null
                && proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod().isPriceIncludesTax())
              taxAmount = (proposalmgmtLine.getTEELineTaxamt()
                  .divide(proposalmgmtLine.getTechLineQty(), roundOffconst, RoundingMode.HALF_UP))
                      .multiply(peeqty);

            BigDecimal teePrice = (proposalmgmtLine.getTechUnitPrice().multiply(peeqty))
                .add(taxAmount);
            discountamt = teePrice.subtract(lineNet);
            discount = (discountamt.divide(teePrice, roundOffconst, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100)));
          }
        }

        info.addResult("inppeeTechDiscountamt", discountamt);
        info.addResult("inppeeTechDiscount", discount);
        // after change the pee tech price the tax amount as 0
        info.addResult("inppeeLineTaxamt", new BigDecimal(0));

      }
      if (inpLastFieldChanged.equals("inptechLineQty")
          || inpLastFieldChanged.equals("inptechUnitPrice")
          || inpLastFieldChanged.equals("inptechDiscountamt")
          || inpLastFieldChanged.equals("inptechDiscount")) {
        EscmProposalmgmtLine proposalmgmtLine = OBDal.getInstance().get(EscmProposalmgmtLine.class,
            proposalLineId);
        if (proposalmgmtLine != null
            && proposalmgmtLine.getTEELineTaxamt().compareTo(BigDecimal.ZERO) > 0) {
          parsedMessage = Utility.messageBD(this, infoMessage,
              OBContext.getOBContext().getLanguage().getId());
          info.addResult("INFO", parsedMessage);
        }
      }
      // if tech line qty or unit price changed update tech line total qty
      if (inpLastFieldChanged.equals("inptechUnitPrice")
          || inpLastFieldChanged.equals("inptechLineQty")) {
        String inpTechUnitPrice = vars.getNumericParameter("inptechUnitPrice");
        String inpTechLineQty = vars.getNumericParameter("inptechLineQty");
        BigDecimal techUnitPrice = new BigDecimal(inpTechUnitPrice);
        BigDecimal techLineQty = new BigDecimal(inpTechLineQty);
        if (inpLastFieldChanged.equals("inptechLineQty")) {
          EscmProposalmgmtLine proposalmgmtLine = OBDal.getInstance()
              .get(EscmProposalmgmtLine.class, proposalLineId);
          if (inpTeeLineTaxamt.compareTo(new BigDecimal(0)) > 0) {
            taxAmount = (proposalmgmtLine.getTEELineTaxamt()
                .divide(proposalmgmtLine.getTechLineQty(), 15, RoundingMode.HALF_UP))
                    .multiply(new BigDecimal(inptechLineQty));
            info.addResult("inpteeLineTaxamt",
                taxAmount.setScale(roundOffconst, RoundingMode.HALF_UP));
          }

        }
        techLineNet = techUnitPrice.multiply(techLineQty);
        info.addResult("inptechLineTotal", techLineNet.add(taxAmount));
        if (inpLastFieldChanged.equals("inptechUnitPrice")) {

          // discountamt = (linenetprice.multiply(qty)).subtract(qty.multiply(techUnitPrice));
          discountamt = (linenetprice.multiply(inptechqty))
              .subtract(inptechqty.multiply(techUnitPrice));
          discount = (discountamt
              .divide(linenetprice.multiply(inptechqty), 15, RoundingMode.HALF_UP)
              .multiply(new BigDecimal(100)).setScale(roundOffconst, RoundingMode.HALF_UP));
          info.addResult("inptechDiscountamt", discountamt);
          info.addResult("inptechDiscount", discount);
          // a.divide(b, 2, RoundingMode.HALF_UP)
          // after change the techunit price the tax amount as 0
          info.addResult("inpteeLineTaxamt", new BigDecimal(0));
        }
      }
      // if not provided checked then make price default to 0
      if (inpLastFieldChanged.equals("inpnotprovided")) {
        if (inpnotProvided.equals("Y")) {
          info.addResult("inpgrossUnitPrice", BigDecimal.ZERO);
          info.addResult("inpnetprice", BigDecimal.ZERO);
          info.addResult("inpnegotUnitPrice", BigDecimal.ZERO);
          info.addResult("inplineTotal", BigDecimal.ZERO);
          info.addResult("inplineTaxamt", 0);
          info.addResult("inptechUnitPrice", BigDecimal.ZERO);
        }
      }

      if (inpLastFieldChanged.equals("inpmProductId")) {
        Product product = OBDal.getInstance().get(Product.class, inpProduct);
        if (!inpProduct.equals("")) {
          info.addResult("inpdescription", product.getName());
          info.addResult("inpcUomId", product.getUOM().getId());
          info.addResult("inpmProductCategoryId", product.getProductCategory().getId());
        } else {
          info.addResult("inpdescription", "");
          info.addResult("inpcUomId", "");
          info.addResult("inpmProductCategoryId", "");
        }
      }
      if (inpLastFieldChanged.equals("inpapprovalDateHijiri")) {

        st = OBDal.getInstance().getConnection()
            .prepareStatement("select to_char(eut_convertto_gregorian('" + inpapprovalDateHijiri
                + "')) as eut_convertto_gregorian ");
        rs = st.executeQuery();

        if (rs.next()) {
          info.addResult("inpapprovalDateGreg", rs.getString("eut_convertto_gregorian"));

        }
        rs.close();
      }
      if (inpLastFieldChanged.equals("inptechDiscount")
          || inpLastFieldChanged.equals("inptechLineQty")) {
        EscmProposalmgmtLine proposalmgmtLine = OBDal.getInstance().get(EscmProposalmgmtLine.class,
            proposalLineId);

        if (!inptechnicalDiscount.equals("") && tech_discount.compareTo(BigDecimal.ZERO) != 0) {
          if (proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod() != null
              && proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod().isPriceIncludesTax()) {
            taxamt = proposalmgmtLine.getTaxAmount().divide(proposalmgmtLine.getMovementQuantity(),
                roundOffconst, RoundingMode.FLOOR);
          }

          TeeTaxAmt = taxamt.multiply(new BigDecimal(inptechLineQty));
          inptechprice = new BigDecimal(inptechLineQty).multiply(inpproposalNegotUnitPrice)
              .add(TeeTaxAmt);
          discountamt = inptechprice.subtract(inptechprice
              .multiply(new BigDecimal(100).subtract(tech_discount).divide(new BigDecimal(100))));
          netPrice = inptechprice.subtract(discountamt);

          inptechunitprice = inpproposalNegotUnitPrice.add(taxamt)
              .subtract(inpproposalNegotUnitPrice.add(taxamt)
                  .multiply(tech_discount.divide(new BigDecimal(100))));
          info.addResult("inptechLineTotal", netPrice);
          info.addResult("inptechDiscountamt", discountamt);
          if (inpLastFieldChanged.equals("inptechDiscount")) {
            info.addResult("inptechUnitPrice", inptechunitprice);
            // after change the techunit price the tax amount as 0
            info.addResult("inpteeLineTaxamt", BigDecimal.ZERO);
            info.addResult("inpteeUnittax", BigDecimal.ZERO);
          }

        } else if (inptechnicalDiscount.equals("")
            || tech_discount.compareTo(BigDecimal.ZERO) == 0) {
          if (inpLastFieldChanged.equals("inptechDiscount")) {
            info.addResult("inptechUnitPrice", proposalmgmtLine.getNegotUnitPrice());
            if (proposalmgmtLine.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
              if (proposalmgmtLine.getMovementQuantity()
                  .equals(proposalmgmtLine.getTechLineQty())) {
                TeeTaxAmt = proposalmgmtLine.getTaxAmount();
                info.addResult("inpteeLineTaxamt", TeeTaxAmt);
              } else {
                taxamt = proposalmgmtLine.getTaxAmount().divide(
                    proposalmgmtLine.getMovementQuantity(), roundOffconst, RoundingMode.FLOOR);
                TeeTaxAmt = taxamt.multiply(inptechqty);
                info.addResult("inpteeLineTaxamt", TeeTaxAmt);
              }

            } else {
              info.addResult("inpteeLineTaxamt", TeeTaxAmt);
            }
            info.addResult("inptechLineTotal",
                inptechqty.multiply(proposalmgmtLine.getNegotUnitPrice()).add(TeeTaxAmt));
            info.addResult("inptechDiscountamt", 0);
            /*
             * discountamt = (linenetprice.multiply(inptechqty))
             * .subtract(inptechqty.multiply(inptechunitprice)); discount = (discountamt
             * .divide(linenetprice.multiply(inptechqty), roundOffconst, RoundingMode.HALF_UP)
             * .multiply(new BigDecimal(100))); info.addResult("inptechDiscountamt", discountamt);
             * info.addResult("inptechDiscount", discount);
             */

          }
        }
      } else if (inpLastFieldChanged.equals("inptechDiscountamt")) {
        EscmProposalmgmtLine proposalmgmtLine = OBDal.getInstance().get(EscmProposalmgmtLine.class,
            proposalLineId);

        if (!inptechnicalDiscountamt.equals("")
            && technical_discount_amount.compareTo(BigDecimal.ZERO) != 0) {
          if (proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod() != null
              && proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod().isPriceIncludesTax()) {
            taxamt = proposalmgmtLine.getTaxAmount().divide(proposalmgmtLine.getMovementQuantity(),
                roundOffconst, RoundingMode.FLOOR);
          }

          TeeTaxAmt = taxamt.multiply(new BigDecimal(inptechLineQty));
          inptechprice = new BigDecimal(inptechLineQty).multiply(inpproposalNegotUnitPrice)
              .add(TeeTaxAmt);

          discount = technical_discount_amount.multiply(new BigDecimal(100)).divide(inptechprice, 2,
              RoundingMode.HALF_EVEN);
          netPrice = inptechprice.subtract(technical_discount_amount);

          inptechunitprice = inpproposalNegotUnitPrice.add(taxamt).subtract(
              inpproposalNegotUnitPrice.add(taxamt).multiply(discount.divide(new BigDecimal(100))));
          info.addResult("inptechLineTotal", netPrice);
          info.addResult("inptechDiscount", discount);
          if (inpLastFieldChanged.equals("inptechDiscountamt")) {
            info.addResult("inptechUnitPrice", inptechunitprice);
            // after change the techunit price the tax amount as 0
            info.addResult("inpteeLineTaxamt", BigDecimal.ZERO);
            info.addResult("inpteeUnittax", BigDecimal.ZERO);
          }

        } else if (inptechnicalDiscountamt.equals("")
            || technical_discount_amount.compareTo(BigDecimal.ZERO) == 0) {
          if (inpLastFieldChanged.equals("inptechDiscountamt")) {
            info.addResult("inptechUnitPrice", proposalmgmtLine.getNegotUnitPrice());
            if (proposalmgmtLine.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
              if (proposalmgmtLine.getMovementQuantity()
                  .equals(proposalmgmtLine.getTechLineQty())) {
                TeeTaxAmt = proposalmgmtLine.getTaxAmount();
                info.addResult("inpteeLineTaxamt", TeeTaxAmt);
              } else {
                taxamt = proposalmgmtLine.getTaxAmount().divide(
                    proposalmgmtLine.getMovementQuantity(), roundOffconst, RoundingMode.FLOOR);
                TeeTaxAmt = taxamt.multiply(inptechqty);
                info.addResult("inpteeLineTaxamt", TeeTaxAmt);
              }

            } else {
              info.addResult("inpteeLineTaxamt", TeeTaxAmt);
            }
            info.addResult("inptechLineTotal",
                inptechqty.multiply(proposalmgmtLine.getNegotUnitPrice()).add(TeeTaxAmt));
            info.addResult("inptechDiscountamt", 0);
          }
        }
      }
      if (inpLastFieldChanged.equals("inppeeTechDiscount")) {
        TeeTaxAmt = BigDecimal.ZERO;
        if (!inppeeTechDiscount.equals("") && pee_tech_discount.compareTo(BigDecimal.ZERO) != 0) {
          EscmProposalmgmtLine proposalmgmtLine = OBDal.getInstance()
              .get(EscmProposalmgmtLine.class, proposalLineId);

          BigDecimal inppeeNegotUnitPriceDiscAmt = BigDecimal.ZERO;
          // changed 6607
          if (proposalmgmtLine.getEscmProposalmgmt().getProposalType().equals("DR")) {
            if (proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod() != null
                && proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod().isPriceIncludesTax()) {
              taxamt = proposalmgmtLine.getTaxAmount().divide(
                  proposalmgmtLine.getMovementQuantity(), roundOffconst, RoundingMode.FLOOR);
            }
            TeeTaxAmt = taxamt.multiply(new BigDecimal(inppeeqty));
            teeLineTotal = peeqty.multiply(inpproposalNegotUnitPrice);// inppeeNegotUnitPrice
            teeLineTotal = teeLineTotal.add(TeeTaxAmt);
            discountamt = teeLineTotal.multiply(pee_tech_discount.divide(new BigDecimal(100)));
            netPrice = teeLineTotal.subtract(discountamt);

            inppeeNegotUnitPriceDiscAmt = inpproposalNegotUnitPrice.add(taxamt)
                .multiply(pee_tech_discount.divide(new BigDecimal(100)));
            netPrice = inpproposalNegotUnitPrice.add(taxamt).subtract(inppeeNegotUnitPriceDiscAmt)
                .setScale(roundOffconst, RoundingMode.FLOOR)
                .multiply(proposalmgmtLine.getMovementQuantity());

          } else {
            if (proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod() != null
                && proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod().isPriceIncludesTax()) {
              taxamt = proposalmgmtLine.getTEELineTaxamt().divide(proposalmgmtLine.getTechLineQty(),
                  roundOffconst, RoundingMode.HALF_UP);
            }
            if ((inppeeNegotUnitPrice.compareTo(proposalmgmtLine.getTechUnitPrice()) == 0)
                && (peeqty.compareTo(proposalmgmtLine.getTechLineQty()) == 0)) {
              if (proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod() != null
                  && !proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod()
                      .isPriceIncludesTax()) {
                teeLineTotal = proposalmgmtLine.getTechLineTotal()
                    .subtract(proposalmgmtLine.getTEELineTaxamt());
              } else {
                teeLineTotal = proposalmgmtLine.getTechLineTotal();
              }
              discountamt = teeLineTotal.multiply(pee_tech_discount.divide(new BigDecimal(100)));
              netPrice = teeLineTotal.subtract(discountamt);
              inppeeNegotUnitPriceDiscAmt = inpproposalNegotUnitPrice.add(taxamt)
                  .multiply(pee_tech_discount.divide(new BigDecimal(100)));

            } else {

              TeeTaxAmt = taxamt.multiply(new BigDecimal(inppeeqty));
              inptechprice = new BigDecimal(inppeeqty).multiply(proposalmgmtLine.getTechUnitPrice())
                  .add(TeeTaxAmt);
              discountamt = inptechprice.subtract(inptechprice.multiply(
                  new BigDecimal(100).subtract(pee_tech_discount).divide(new BigDecimal(100))));
              netPrice = inptechprice.subtract(discountamt);
              inppeeNegotUnitPriceDiscAmt = proposalmgmtLine.getTechUnitPrice().add(taxamt)
                  .multiply(pee_tech_discount.divide(new BigDecimal(100)));
              netPrice = proposalmgmtLine.getTechUnitPrice().add(taxamt)
                  .subtract(inppeeNegotUnitPriceDiscAmt)
                  .setScale(roundOffconst, RoundingMode.HALF_UP).multiply(peeqty);

            }
          }

          info.addResult("inppeeTechDiscountamt", discountamt);
          info.addResult("inppeeLineTotal", netPrice);
          info.addResult("inppeeNegotUnitPrice", inpproposalNegotUnitPrice.add(taxamt)
              .subtract(inppeeNegotUnitPriceDiscAmt).setScale(roundOffconst, RoundingMode.HALF_UP));
          // after change the pee tech price the tax amount as 0
          info.addResult("inppeeLineTaxamt", new BigDecimal(0));

        }

        else {

          EscmProposalmgmtLine proposalmgmtLine = OBDal.getInstance()
              .get(EscmProposalmgmtLine.class, proposalLineId);
          info.addResult("inppeeNegotUnitPrice", proposalmgmtLine.getNegotUnitPrice());
          if (proposalmgmtLine.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            if (proposalmgmtLine.getMovementQuantity().equals(proposalmgmtLine.getPEEQty())) {
              TeeTaxAmt = proposalmgmtLine.getTaxAmount();
              info.addResult("inppeeLineTaxamt", TeeTaxAmt);
            } else {
              taxamt = proposalmgmtLine.getTaxAmount().divide(
                  proposalmgmtLine.getMovementQuantity(), roundOffconst, RoundingMode.FLOOR);
              TeeTaxAmt = taxamt.multiply(peeqty);
              info.addResult("inppeeLineTaxamt", TeeTaxAmt);
            }

          } else {
            info.addResult("inppeeLineTaxamt", TeeTaxAmt);
          }
          info.addResult("inppeeLineTotal",
              peeqty.multiply(proposalmgmtLine.getNegotUnitPrice()).add(TeeTaxAmt));
          info.addResult("inppeeTechDiscountamt", 0);

          /*
           * if (proposalmgmtLine.getEscmProposalmgmt().getProposalType().equals("DR")) {
           * discountamt = ((proposalmgmtLine.getNetprice())
           * .multiply(proposalmgmtLine.getMovementQuantity()))
           * .subtract(peeqty.multiply(inppeeNegotUnitPrice)); teeLineTotal =
           * peeqty.multiply(inppeeNegotUnitPrice); } else {
           * 
           * discountamt = ((proposalmgmtLine.getTechUnitPrice())
           * .multiply(proposalmgmtLine.getMovementQuantity()))
           * .subtract(peeqty.multiply(inppeeNegotUnitPrice)); if
           * ((inppeeNegotUnitPrice.compareTo(proposalmgmtLine.getTechUnitPrice()) == 0) &&
           * (qty.compareTo(proposalmgmtLine.getTechLineQty()) == 0)) { teeLineTotal =
           * proposalmgmtLine.getTechLineTotal(); } else { teeLineTotal =
           * peeqty.multiply(inppeeNegotUnitPrice) .add(proposalmgmtLine.getPEELineTaxamt()); } }
           * discount = (discountamt.divide((proposalmgmtLine.getNetprice().multiply(peeqty)),
           * roundOffconst, RoundingMode.FLOOR).multiply(new BigDecimal(100)));
           * info.addResult("inppeeTechDiscountamt", discountamt);
           * info.addResult("inppeeTechDiscount", discount); info.addResult("inppeeLineTotal",
           * teeLineTotal);
           */

        }
      } else if (inpLastFieldChanged.equals("inppeeTechDiscountamt")) {
        TeeTaxAmt = BigDecimal.ZERO;
        if (!inppeeTechDiscountamt.equals("")
            && pee_discount_amount.compareTo(BigDecimal.ZERO) != 0) {
          EscmProposalmgmtLine proposalmgmtLine = OBDal.getInstance()
              .get(EscmProposalmgmtLine.class, proposalLineId);

          BigDecimal inppeeNegotUnitPriceDiscAmt = BigDecimal.ZERO;
          // changed 6607
          if (proposalmgmtLine.getEscmProposalmgmt().getProposalType().equals("DR")) {
            if (proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod() != null
                && proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod().isPriceIncludesTax()) {
              taxamt = proposalmgmtLine.getTaxAmount().divide(
                  proposalmgmtLine.getMovementQuantity(), roundOffconst, RoundingMode.FLOOR);
            }
            TeeTaxAmt = taxamt.multiply(new BigDecimal(inppeeqty));
            teeLineTotal = peeqty.multiply(inpproposalNegotUnitPrice);// inppeeNegotUnitPrice
            teeLineTotal = teeLineTotal.add(TeeTaxAmt);

            pee_tech_discount = pee_discount_amount.multiply(new BigDecimal(100))
                .divide(teeLineTotal, 2, RoundingMode.HALF_EVEN);

            netPrice = teeLineTotal.subtract(pee_discount_amount);

            inppeeNegotUnitPriceDiscAmt = inpproposalNegotUnitPrice.add(taxamt)
                .multiply(pee_tech_discount.divide(new BigDecimal(100)));

            netPrice = inpproposalNegotUnitPrice.add(taxamt).subtract(inppeeNegotUnitPriceDiscAmt)
                .setScale(roundOffconst, RoundingMode.FLOOR)
                .multiply(proposalmgmtLine.getMovementQuantity());

          } else {
            if (proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod() != null
                && proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod().isPriceIncludesTax()) {
              taxamt = proposalmgmtLine.getTEELineTaxamt().divide(proposalmgmtLine.getTechLineQty(),
                  roundOffconst, RoundingMode.HALF_UP);
            }
            if ((inppeeNegotUnitPrice.compareTo(proposalmgmtLine.getTechUnitPrice()) == 0)
                && (peeqty.compareTo(proposalmgmtLine.getTechLineQty()) == 0)) {
              if (proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod() != null
                  && !proposalmgmtLine.getEscmProposalmgmt().getEfinTaxMethod()
                      .isPriceIncludesTax()) {
                teeLineTotal = proposalmgmtLine.getTechLineTotal()
                    .subtract(proposalmgmtLine.getTEELineTaxamt());
              } else {
                teeLineTotal = proposalmgmtLine.getTechLineTotal();
              }

              pee_tech_discount = pee_discount_amount.multiply(new BigDecimal(100))
                  .divide(teeLineTotal, 2, RoundingMode.HALF_EVEN);
              netPrice = teeLineTotal.subtract(pee_discount_amount);
              inppeeNegotUnitPriceDiscAmt = inpproposalNegotUnitPrice.add(taxamt)
                  .multiply(pee_tech_discount.divide(new BigDecimal(100)));

            } else {

              TeeTaxAmt = taxamt.multiply(new BigDecimal(inppeeqty));
              inptechprice = new BigDecimal(inppeeqty).multiply(proposalmgmtLine.getTechUnitPrice())
                  .add(TeeTaxAmt);
              pee_tech_discount = pee_discount_amount.multiply(new BigDecimal(100))
                  .divide(inptechprice, 2, RoundingMode.HALF_EVEN);

              netPrice = inptechprice.subtract(pee_discount_amount);
              inppeeNegotUnitPriceDiscAmt = proposalmgmtLine.getTechUnitPrice().add(taxamt)
                  .multiply(pee_tech_discount.divide(new BigDecimal(100)));
              netPrice = proposalmgmtLine.getTechUnitPrice().add(taxamt)
                  .subtract(inppeeNegotUnitPriceDiscAmt)
                  .setScale(roundOffconst, RoundingMode.HALF_UP).multiply(peeqty);

            }
          }

          info.addResult("inppeeTechDiscount", pee_tech_discount);
          info.addResult("inppeeLineTotal", netPrice);
          info.addResult("inppeeNegotUnitPrice", inpproposalNegotUnitPrice.add(taxamt)
              .subtract(inppeeNegotUnitPriceDiscAmt).setScale(roundOffconst, RoundingMode.HALF_UP));
          // after change the pee tech price the tax amount as 0
          info.addResult("inppeeLineTaxamt", new BigDecimal(0));

        }

        else {

          EscmProposalmgmtLine proposalmgmtLine = OBDal.getInstance()
              .get(EscmProposalmgmtLine.class, proposalLineId);
          info.addResult("inppeeNegotUnitPrice", proposalmgmtLine.getNegotUnitPrice());
          if (proposalmgmtLine.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            if (proposalmgmtLine.getMovementQuantity().equals(proposalmgmtLine.getPEEQty())) {
              TeeTaxAmt = proposalmgmtLine.getTaxAmount();
              info.addResult("inppeeLineTaxamt", TeeTaxAmt);
            } else {
              taxamt = proposalmgmtLine.getTaxAmount().divide(
                  proposalmgmtLine.getMovementQuantity(), roundOffconst, RoundingMode.FLOOR);
              TeeTaxAmt = taxamt.multiply(peeqty);
              info.addResult("inppeeLineTaxamt", TeeTaxAmt);
            }

          } else {
            info.addResult("inppeeLineTaxamt", TeeTaxAmt);
          }
          info.addResult("inppeeLineTotal",
              peeqty.multiply(proposalmgmtLine.getNegotUnitPrice()).add(TeeTaxAmt));
          info.addResult("inppeeTechDiscountamt", 0);

        }
      }

      if (inpLastFieldChanged.equals("inpemEfinCValidcombinationId")) {
        if (uniqueCode.equals("")) {
          info.addResult("inpemEfinFundsAvailable", BigDecimal.ZERO);
          info.addResult("inpemEfinUniquecodename", "");
        } else {
          AccountingCombination dimention = OBDal.getInstance().get(AccountingCombination.class,
              uniqueCode);
          if (budgetInit != null) {
            fundsAvailable = RequisitionDao.getAutoEncumFundsAvailable(uniqueCode, budgetInit);
            info.addResult("inpemEfinFundsAvailable", fundsAvailable);

            info.addResult("inpemEfinUniquecodename", dimention.getEfinUniquecodename());
            log.debug("inpLastFieldChanged:" + dimention.getEfinUniquecodename());
          }
        }
      }

      if (inpLastFieldChanged.equals("inpgrossUnitPrice")
          || inpLastFieldChanged.equals("inpmovementqty")
          || inpLastFieldChanged.equals("inpnegotUnitPrice")) {
        Boolean isPriceInclOfTax = Boolean.FALSE;
        BigDecimal PERCENT = new BigDecimal("0.01");
        BigDecimal lineTotal = BigDecimal.ZERO, tax = BigDecimal.ZERO;
        BigDecimal taxPercent = BigDecimal.ZERO;
        EscmProposalMgmt proposalMgmt = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
        if (proposalMgmt.isTaxLine()) {
          isPriceInclOfTax = proposalMgmt.getEfinTaxMethod() == null ? Boolean.TRUE
              : proposalMgmt.getEfinTaxMethod().isPriceIncludesTax();
          if (!isPriceInclOfTax) {
            tax = proposalMgmt.getEfinTaxMethod() == null ? BigDecimal.ZERO
                : new BigDecimal(proposalMgmt.getEfinTaxMethod().getTaxpercent());
            taxPercent = tax.multiply(PERCENT);
            lineTotal = (negotiatedunitPrice.multiply(qty))
                .subtract(inpdisamount == null ? BigDecimal.ZERO : inpdisamount);
            taxAmount = lineTotal.multiply(taxPercent);
            info.addResult("inplineTotal", (lineTotal.add(taxAmount)));
            info.addResult("inplineTaxamt", taxAmount);
          }
        }
        if (inpLastFieldChanged.equals("inpnegotUnitPrice")) {
          if (proposalMgmt.getTotalTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            parsedMessage = Utility.messageBD(this, infoMessage,
                OBContext.getOBContext().getLanguage().getId());
            info.addResult("INFO", parsedMessage);

          }
          info.addResult("inplineTaxamt", BigDecimal.ZERO);
          info.addResult("inpunittax", BigDecimal.ZERO);
          lineNet = inpproposalNegotUnitPrice.multiply(qty);
          info.addResult("inpunitpricedis", inpproposalNegotUnitPrice);
          info.addResult("inplineTotal", lineNet);
          if (proposalMgmt.getVersionNo() != null) {
            info.addResult("inpdiscount", BigDecimal.ZERO);
            info.addResult("inpdiscountmount", BigDecimal.ZERO);
            info.addResult("inpproposalDiscount", BigDecimal.ZERO);
            info.addResult("inpproposalDiscountAmount", BigDecimal.ZERO);
          }
        }

      }

      if (inpLastFieldChanged.equals("inplineTaxamt")) {
        if (StringUtils.isEmpty(inplineTaxamt)) {
          info.addResult("inplineTaxamt", BigDecimal.ZERO);
        }
        EscmProposalMgmt proposalMgmt = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
        proposalMgmt.setCalculateTax(false);
        OBDal.getInstance().save(proposalMgmt);
        OBDal.getInstance().flush();
      }
      if (inpLastFieldChanged.equals("inplineTaxamt")) {
        if (StringUtils.isEmpty(inplineTaxamt)) {
          info.addResult("inplineTaxamt", BigDecimal.ZERO);
        }
      }
      // to Calculate 'line total' in TEE_line when tax amount changes
      if (inpLastFieldChanged.equals("inpteeLineTaxamt")) {
        info.addResult("inptechLineTotal",
            (inptechqty.multiply(inptechunitprice)).add(inpTeeLineTaxamt));
      }
      // to Calculate 'line total' in PEE_line when tax amount changes
      if (inpLastFieldChanged.equals("inppeeLineTaxamt")) {
        info.addResult("inppeeLineTotal",
            (peeqty.multiply(inppeeNegotUnitPrice)).add(inpPeeLineTaxamt));
      }

      // in proposal line level discount change
      if (inpLastFieldChanged.equals("inpdiscount")
          || inpLastFieldChanged.equals("inpdiscountmount")) {
        EscmProposalMgmt proposalMgmt = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
        if (proposalMgmt != null
            && proposalMgmt.getTotalTaxAmount().compareTo(new BigDecimal(0)) > 0) {
          parsedMessage = Utility.messageBD(this, infoMessage,
              OBContext.getOBContext().getLanguage().getId());
          info.addResult("INFO", parsedMessage);
        }
        BigDecimal v_Line_Total = BigDecimal.ZERO;
        BigDecimal v_discountamt = BigDecimal.ZERO;
        BigDecimal v_total = BigDecimal.ZERO;
        BigDecimal v_unitprice = BigDecimal.ZERO;
        BigDecimal v_discount = BigDecimal.ZERO;
        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal unitPrice = BigDecimal.ZERO;
        EscmProposalmgmtLine objLine = OBDal.getInstance().get(EscmProposalmgmtLine.class,
            proposalLineId);
        quantity = objLine != null ? objLine.getMovementQuantity() : qty;
        unitPrice = objLine != null ? objLine.getGrossUnitPrice() : unitprice;

        if (inpLastFieldChanged.equals("inpdiscountmount")) {
          discount = BigDecimal.ZERO;
        }
        v_Line_Total = quantity.multiply(unitPrice).setScale(roundOffconst, RoundingMode.HALF_UP);
        if (inpLastFieldChanged.equals("inpdiscount")) {
          v_discountamt = (v_Line_Total).multiply(discount.divide(new BigDecimal(100)));
          v_discount = discount;
        } else {
          v_discount = (inpdisamount.multiply(new BigDecimal(100))).divide(v_Line_Total, 2,
              RoundingMode.HALF_EVEN);
          v_discountamt = inpdisamount;
        }

        v_total = v_Line_Total.subtract(v_discountamt);
        v_unitprice = v_total.divide(quantity, roundOffconst, RoundingMode.HALF_UP);

        if (inpLastFieldChanged.equals("inpdiscount"))
          info.addResult("inpdiscountmount", v_discountamt);
        else
          info.addResult("inpdiscount", v_discount);
        info.addResult("inpproposalDiscount", v_discount);
        info.addResult("inpproposalDiscountAmount", v_discountamt);
        info.addResult("inplineTotal", v_total);
        info.addResult("inplineTaxamt", BigDecimal.ZERO);
        info.addResult("inpnegotUnitPrice", v_unitprice);
      }
    } catch (

    Exception e) {
      log.error("Exception in proposal lines callout:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      // close connection
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {
        log4j.error("Exception while closing the statement in EscmBiddatesCallout ", e);
      }
      OBContext.restorePreviousMode();
    }

  }
}
