package sa.elm.ob.scm.ad_callouts;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;

import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;

/**
 * 
 * @author Gopalakrishnan
 *
 */

public class ProposalManagementDiscountChanges extends SimpleCallout {
  private static Logger log = Logger.getLogger(ProposalManagementDiscountChanges.class);
  private static final String infoMessage = "Escm_DiscountChanged_AfterTax";
  /**
   * Callout to update discount percentage and discount amount in proposal screens
   */
  private static final long serialVersionUID = 1L;

  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpfinaldiscountamount = vars.getStringParameter("inpfinaldiscountamount");
    String inpescmProposalId = vars.getStringParameter("inpescmProposalmgmtId");

    BigDecimal negoprice = BigDecimal.ZERO;
    BigDecimal proposal_total = BigDecimal.ZERO;

    BigDecimal proposal_discount = BigDecimal.ZERO;

    BigDecimal proposal_discount_amount = BigDecimal.ZERO;
    if (!inpfinaldiscountamount.equals("")) {
      proposal_discount_amount = new BigDecimal(inpfinaldiscountamount.replace(",", ""));
    }
    String inpfinaldiscount = vars.getStringParameter("inpfinaldiscount");
    if (!inpfinaldiscount.equals("")) {
      proposal_discount = new BigDecimal(inpfinaldiscount);
    }

    BigDecimal netPrice = BigDecimal.ZERO;
    BigDecimal discountamt = BigDecimal.ZERO;
    String parsedMessage = null;

    try {

      if (inpLastFieldChanged.equals("inpfinaldiscount")
          || inpLastFieldChanged.equals("inpfinaldiscountamount")) {
        EscmProposalMgmt obj_proposal = OBDal.getInstance().get(EscmProposalMgmt.class,
            inpescmProposalId);
        if (obj_proposal != null
            && obj_proposal.getTotalTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
          parsedMessage = Utility.messageBD(this, infoMessage,
              OBContext.getOBContext().getLanguage().getId());
          info.addResult("INFO", parsedMessage);

        }

      }

      if (inpLastFieldChanged.equals("inpfinaldiscount")) {

        EscmProposalMgmt obj_proposal = OBDal.getInstance().get(EscmProposalMgmt.class,
            inpescmProposalId);
        for (EscmProposalmgmtLine objTechLine : obj_proposal.getEscmProposalmgmtLineList()) {
          if (!objTechLine.isSummary()) {
            if (objTechLine.getPeestatus() == null || (!objTechLine.getPeestatus().equals("CL"))) {

              proposal_total = objTechLine.getMovementQuantity()
                  .multiply(objTechLine.getGrossUnitPrice());
              // if (obj_proposal.getEfinTaxMethod() != null
              // && !obj_proposal.getEfinTaxMethod().isPriceIncludesTax()) {
              negoprice = negoprice.add(proposal_total);
              // } else {
              // negoprice = negoprice.add(proposal_total.add(objTechLine.getTaxAmount()));
              // }

            }
          }
        }
        if (!inpfinaldiscount.equals("") && proposal_discount.compareTo(BigDecimal.ZERO) != 0) {

          discountamt = negoprice.subtract(negoprice.multiply(
              new BigDecimal(100).subtract(proposal_discount).divide(new BigDecimal(100))));
          netPrice = negoprice.subtract(discountamt);

          if (netPrice.compareTo(BigDecimal.ZERO) < 0) {
            info.addResult("ERROR", OBMessageUtils.messageBD("ESCM_DISCOUNT_ISMORE"));
          }
          info.addResult("inpnegotiatedPrice", netPrice);
          info.addResult("inpfinaldiscountamount", discountamt);
          info.addResult("inptotpoafterchngprice", netPrice);
          info.addResult("inptotalamount", netPrice);

        } else {
          // changed 6607
          info.addResult("inpnegotiatedPrice", negoprice);
          info.addResult("inpfinaldiscountamount", 0);
          info.addResult("inpfinaldiscount", 0);
          info.addResult("inptotpoafterchngprice", negoprice);
          info.addResult("inptotalamount", negoprice);

        }

      } else if (inpLastFieldChanged.equals("inpfinaldiscountamount")) {

        EscmProposalMgmt obj_proposal = OBDal.getInstance().get(EscmProposalMgmt.class,
            inpescmProposalId);
        for (EscmProposalmgmtLine objTechLine : obj_proposal.getEscmProposalmgmtLineList()) {
          if (!objTechLine.isSummary()) {
            if (objTechLine.getPeestatus() == null || (!objTechLine.getPeestatus().equals("CL"))) {
              // changed 6607
              proposal_total = objTechLine.getMovementQuantity()
                  .multiply(objTechLine.getGrossUnitPrice());
              // if (obj_proposal.getEfinTaxMethod() != null
              // && !obj_proposal.getEfinTaxMethod().isPriceIncludesTax()) {
              // negoprice = negoprice.add(proposal_total);
              // } else {
              negoprice = negoprice.add(proposal_total);
              // }

            }
          }
        }
        if (!inpfinaldiscountamount.equals("")
            && proposal_discount_amount.compareTo(BigDecimal.ZERO) != 0) {

          proposal_discount = (proposal_discount_amount.multiply(new BigDecimal(100)))
              .divide(negoprice, 2, RoundingMode.HALF_EVEN);

          netPrice = negoprice.subtract(proposal_discount_amount);

          if (netPrice.compareTo(BigDecimal.ZERO) < 0) {
            info.addResult("ERROR", OBMessageUtils.messageBD("ESCM_DISCOUNT_ISMORE"));
          }
          info.addResult("inpnegotiatedPrice", netPrice);
          info.addResult("inpfinaldiscount", proposal_discount);
          info.addResult("inptotpoafterchngprice", netPrice);
          info.addResult("inptotalamount", netPrice);
        } else {
          info.addResult("inpnegotiatedPrice", negoprice);
          info.addResult("inpfinaldiscount", 0);
          info.addResult("inpfinaldiscountamount", 0);
          info.addResult("inptotpoafterchngprice", negoprice);
          info.addResult("inptotalamount", negoprice);
        }

      }

    } catch (Exception e) {

      e.printStackTrace();
      log.error("Exception in ProposalManagementDiscountChanges:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }

  }
}
