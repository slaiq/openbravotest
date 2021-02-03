package sa.elm.ob.scm.ad_process.proposalevaluationevent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMProposalEvlEvent;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.EscmTechnicalevlEvent;
import sa.elm.ob.scm.ad_process.ProposalManagement.tax.ProposalTaxCalculationDAO;
import sa.elm.ob.scm.ad_process.ProposalManagement.tax.ProposalTaxCalculationDAOImpl;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Divya on 07/06/2017
 */

public class ProposalEvaluationEventProcess extends DalBaseProcess {

  /**
   * This servlet class was responsible for Proposal Evaluation Event Process.
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(ProposalEvaluationEventProcess.class);

  // private final OBError obError = new OBError();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    boolean errorFlag = false;
    String Proposallist = null, message = "";
    BigDecimal grossPrice = BigDecimal.ZERO;
    int count = 0;
    int proposallinecount = 0;
    Connection conn = OBDal.getInstance().getConnection();
    BigDecimal netPrice = BigDecimal.ZERO;
    @SuppressWarnings("unused")
    BigDecimal proposalgrossprice = BigDecimal.ZERO;
    boolean isPeriodOpen = true;
    int propCount = 0, peeCount = 0;
    int lineQty = 0;
    BigDecimal discount_Amount = BigDecimal.ZERO;
    BigDecimal final_NegPrice = BigDecimal.ZERO;
    BigDecimal discAmt = BigDecimal.ZERO;
    BigDecimal calculated_Discount_Amount = BigDecimal.ZERO;
    BigDecimal calculated_total_discount = BigDecimal.ZERO;
    BigDecimal calculated_gross_price = BigDecimal.ZERO;
    List<EscmProposalsourceRef> srcrefList = new ArrayList<EscmProposalsourceRef>();

    try {
      OBContext.setAdminMode();
      // ProposalTaxCalculationDAO dao = null;
      // declaring variables
      final String proposalEvaleventId = (String) bundle.getParams()
          .get("Escm_Proposalevl_Event_ID").toString();
      final String userId = (String) bundle.getContext().getUser();
      String proposalListId = null;
      String[] proposalsize = null;
      int proposalcount = 0;
      List<EscmProposalAttribute> attrlist = new ArrayList<EscmProposalAttribute>();
      // getting Proposal event object by using Proposal Event Id
      ESCMProposalEvlEvent event = OBDal.getInstance().get(ESCMProposalEvlEvent.class,
          proposalEvaleventId);
      String orgId = event.getOrganization().getId();
      // Check transaction period is opened or not before submitting record
      if ("CO".equals(event.getAction())) {
        isPeriodOpen = Utility.checkOpenPeriod(event.getDateHijri(),
            orgId.equals("0") ? vars.getOrg() : orgId, event.getClient().getId());
        if (!isPeriodOpen) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
          bundle.setResult(result);
          return;
        }
        for (EscmProposalAttribute attr : event.getEscmProposalAttrList()) {
          EscmProposalMgmt proposal = attr.getEscmProposalmgmt();
          if (!proposal.getProposalstatus().equals("ANY")
              && (!proposal.getProposalstatus().equals("CL")
                  && !proposal.getProposalType().equals("DR"))) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_ProposalStatusCheck@");
            bundle.setResult(result);
            return;
          }
        }
      }
      // Task no:8327 checking tax is defined or not , if not then throw error
      if (event.getStatus().equals("DR")) {
        for (EscmProposalAttribute attr : event.getEscmProposalAttrList()) {
          if (!attr.isPEEIstax()
              || (attr.isPEEIstax() && attr.getPEETotalTaxamt().compareTo(BigDecimal.ZERO) == 0)) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_TEETaxMandatory@");
            bundle.setResult(result);
            return;
          }
        }
      }
      // submit process start
      if (event.getStatus().equals("DR")) {
        if (event.getStatus().equals("CO")) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }

        // check Negotiated amount is not zero
        for (EscmProposalAttribute attr : event.getEscmProposalAttrList()) {
          if (attr.getNegotiatedPrice().compareTo(BigDecimal.ZERO) == 0) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_TEE_Amt_zero@");
            bundle.setResult(result);
            return;
          }
        }

        // Throw error if any one line need to Calculate the tax.
        for (EscmProposalAttribute attr : event.getEscmProposalAttrList()) {
          if (attr.isPEEIstax()) {
            Optional<EscmProposalmgmtLine> rslt = attr.getEscmProposalmgmt()
                .getEscmProposalmgmtLineList().stream()
                .filter(x -> (x.getPEELineTotal() != null
                    && x.getPEELineTotal().compareTo(BigDecimal.ZERO) > 0)
                    && x.getPEELineTaxamt().compareTo(BigDecimal.ZERO) == 0)
                .findAny();
            if (rslt.isPresent()) {
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_NeedToCalTax@");
              bundle.setResult(result);
              return;
            }
          }
        }

        // check atleast one line having in Proposal tab while submit
        if (event.getEscmProposalAttrList().size() == 0) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_Proposal_AtleaseLine@");
          bundle.setResult(result);
          return;
        }

        // check tee created before submit the PEE
        if (event.getBidNo() != null && event.getBidNo().getBidtype() != null
            && !event.getBidNo().getBidtype().equals("DR")) {
          OBQuery<EscmTechnicalevlEvent> techEvlevent = OBDal.getInstance().createQuery(
              EscmTechnicalevlEvent.class, " as e where e.bidNo.id=:bidID and e.status='CO' ");
          techEvlevent.setNamedParameter("bidID", event.getBidNo().getId());
          techEvlevent.setMaxResult(1);
          if (techEvlevent.list().size() == 0) {
            errorFlag = true;
            OBError result = OBErrorBuilder.buildMessage(null, "Error",
                "@ESCM_TEEMustCreateBefoPEE@");
            bundle.setResult(result);
            return;
          }
        }

        // check rank and Proposal status is filled or not while submit
        if (event.getEscmProposalAttrList().size() > 0) {
          for (EscmProposalAttribute att : event.getEscmProposalAttrList()) {
            if (att.getRank() == null) {
              errorFlag = true;
              Proposallist = (Proposallist == null ? att.getEscmProposalmgmt().getProposalno()
                  : Proposallist + "," + att.getEscmProposalmgmt().getProposalno());
            }
          }
          if (errorFlag) {
            message = OBMessageUtils.messageBD("ESCM_ProposalAttr_RankProStatusEmpty");
            message = message.replace("%", Proposallist);
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", message);
            bundle.setResult(result);
            return;
          }
        }

        // check all quantity is zero or not while submit
        if (event.getEscmProposalAttrList().size() > 0) {
          for (EscmProposalAttribute att : event.getEscmProposalAttrList()) {
            for (EscmProposalmgmtLine line : att.getEscmProposalmgmt()
                .getEscmProposalmgmtLineList()) {
              if (!(line.getPEEQty().equals(BigDecimal.ZERO)) && !line.isSummary()) {
                lineQty = 1;
              }
            }
            if (lineQty == 0) {
              message = OBMessageUtils.messageBD("ESCM_ProposalLine_QuantityEmpty");
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", message);
              bundle.setResult(result);
              return;
            }
          }
        }

        // without add/remove valid/invalid proposal , dont allow to submit the evaluation event.
        if (event.getEscmProposalAttrList().size() > 0) {
          if (event.getBidNo() != null && !event.getBidNo().getBidtype().equals("DR")) {
            // getting valid bg Proposal
            proposalListId = ProposalEvaluationDAO.getValidBG(event.getBidNo().getId(), conn);
            if (proposalListId != null) {
              proposalsize = proposalListId.split(",");
              proposalcount = proposalsize.length;
            } else
              proposalcount = 0;
            // compare valid proposal count and proposal count under evaluation event.if not same
            // then throw error to review the proposal
            if (proposalcount != event.getEscmProposalAttrList().size()) {
              errorFlag = true;
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@ESCM_ProEvlEventReviewProsal@");
              bundle.setResult(result);
              return;
            }
          }
        }

        if ("CO".equals(event.getAction()) && event.getBidNo() != null) {
          peeCount = event.getEscmProposalAttrList().size();
          String whereClause = "";
          if (event.getBidNo().getBidtype().equals("DR"))
            whereClause = "as e where e.escmBidmgmt.id= ? and e.proposalstatus in ('SUB', 'ANY')";
          else
            whereClause = "as e where e.escmBidmgmt.id= ? and e.proposalstatus in ('SUB', 'ANY', 'CL')";
          List<Object> paramLs = new ArrayList<Object>();
          paramLs.add(event.getBidNo());

          OBQuery<EscmProposalMgmt> prop = OBDal.getInstance().createQuery(EscmProposalMgmt.class,
              whereClause, paramLs);
          propCount = prop.list().size();
          if (peeCount != propCount) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_ProposalStatusCheck@");
            bundle.setResult(result);
            return;
          }
        }

        if (!errorFlag) {
          // update Proposal event status if we submit
          event.setUpdated(new java.util.Date());
          event.setUpdatedBy(OBDal.getInstance().get(User.class, userId));
          event.setStatus("CO");
          event.setAction("RE");
          boolean sequenceexists = false;

          if (event.getSpecNo() == null) {
            String sequence = Utility.getSpecificationSequence("0", "PEE");

            if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_NoSpecSequence@");
              bundle.setResult(result);
              return;
            } else {
              sequenceexists = Utility.chkSpecificationSequence("0", "PEE", sequence);
              if (!sequenceexists) {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@Escm_Duplicate_SpecNo@");
                bundle.setResult(result);
                return;
              }
              event.setSpecNo(sequence);
            }
          }
          OBDal.getInstance().save(event);
          OBDal.getInstance().flush();
          if (event.getEscmProposalAttrList().size() > 0) {
            for (EscmProposalAttribute attr : event.getEscmProposalAttrList()) {
              grossPrice = BigDecimal.ZERO;
              netPrice = BigDecimal.ZERO;
              // update Proposal attribute action
              attr.setAction("SA");
              // update proposal status & action
              EscmProposalMgmt proposal = attr.getEscmProposalmgmt();
              // proposal.setProposalstatus(attr.getProposalstatus());
              // if (proposal.getProposalstatus().equals("SHO"))
              proposal.setEscmDocaction("SA");
              proposal.setRank(attr.getRank());
              // proposal.setDiscountForTheDeal(attr.getProsalDiscount());
              // proposal.setDiscountAmount(attr.getProsalDiscountamt());
              // set the tax method and istax
              if (attr.getPEEEfinTaxMethod() != null && attr.isPEEIstax()) {
                EscmProposalMgmt promgmt = attr.getEscmProposalmgmt();
                if (!promgmt.isTaxLine() && promgmt.getEfinTaxMethod() == null) {
                  promgmt.setEfinTaxMethod(attr.getPEEEfinTaxMethod());
                  promgmt.setTaxLine(true);
                }

              }
              if (attr.getEscmProposalmgmt().getEscmProposalmgmtLineList().size() > 0) {
                proposallinecount = attr.getEscmProposalmgmt().getEscmProposalmgmtLineList().size();
                count = 0;
                for (EscmProposalmgmtLine objTechLine : attr.getEscmProposalmgmt()
                    .getEscmProposalmgmtLineList()) {
                  // check all line is cancelled in PEE
                  if (objTechLine.getPeestatus() != null
                      && objTechLine.getPeestatus().equals("CL")) {
                    count = count + 1;
                  }

                  if ((objTechLine.getPeestatus() == null
                      || !objTechLine.getPeestatus().equals("CL"))) {
                    objTechLine.setMovementQuantity(objTechLine.getPEEQty());
                    // update source ref quantity
                    OBQuery<EscmProposalsourceRef> ref = OBDal.getInstance().createQuery(
                        EscmProposalsourceRef.class,
                        " as e where e.escmProposalmgmtLine.id=:proposalLineID and e.requisitionLine is null");
                    ref.setNamedParameter("proposalLineID", objTechLine.getId());
                    srcrefList = ref.list();
                    if (srcrefList.size() > 0) {
                      EscmProposalsourceRef reference = srcrefList.get(0);
                      reference.setReservedQuantity(objTechLine.getMovementQuantity());
                      OBDal.getInstance().save(reference);
                    }
                    if (objTechLine.getPEELineTotal() != null)
                      objTechLine.setLineTotal(objTechLine.getPEELineTotal());
                    else
                      objTechLine.setLineTotal(BigDecimal.ZERO);
                    objTechLine.setNegotUnitPrice(objTechLine.getPEENegotUnitPrice());
                    objTechLine.setStatus(objTechLine.getPeestatus());

                    // Tax Amount
                    if (!objTechLine.isSummary()) {
                      if (objTechLine.getPEELineTotal() != null)
                        netPrice = netPrice.add(objTechLine.getPEELineTotal());
                      if (objTechLine.getEscmProposalmgmt().getEfinTaxMethod() != null
                          && !objTechLine.getEscmProposalmgmt().getEfinTaxMethod()
                              .isPriceIncludesTax()) {
                        grossPrice = grossPrice.add(((objTechLine.getMovementQuantity())
                            .multiply(objTechLine.getGrossUnitPrice()))
                                .add(objTechLine.getTaxAmount() == null ? BigDecimal.ZERO
                                    : objTechLine.getTaxAmount()));
                        objTechLine.setTaxAmount(objTechLine.getPEELineTaxamt());

                      } else {
                        grossPrice = grossPrice.add(((objTechLine.getMovementQuantity())
                            .multiply(objTechLine.getGrossUnitPrice())));
                        objTechLine.setTaxAmount(objTechLine.getPEELineTaxamt());

                      }
                      calculated_Discount_Amount = calculated_Discount_Amount
                          .add(objTechLine.getProposalDiscountAmount().add(objTechLine
                              .getTechDiscountamt().add(objTechLine.getPEETechDiscountamt())));
                    }

                    // update discount and discount amount in proposal line after PEE
                    proposalgrossprice = objTechLine.getGrossUnitPrice()
                        .multiply(objTechLine.getMovementQuantity());
                    if (objTechLine.getLineTotal() != null)
                      if (objTechLine.getLineTotal().compareTo(BigDecimal.ZERO) != 0
                          && !objTechLine.isSummary()) {
                        if (objTechLine.getEscmProposalmgmt().getProposalType().equals("DR"))
                          objTechLine.setDiscountmount(objTechLine.getProposalDiscountAmount()
                              .add(objTechLine.getTechDiscountamt())
                              .add(objTechLine.getPEETechDiscountamt()));
                        else {
                          BigDecimal proposal_GrossPrice = objTechLine.getGrossUnitPrice();
                          if (attr.getPEEEfinTaxMethod() != null
                              && attr.getPEEEfinTaxMethod().isPriceIncludesTax())
                            final_NegPrice = objTechLine.getPEENegotUnitPrice()
                                .add(objTechLine.getPEELineTaxamt().divide(objTechLine.getPEEQty(),
                                    2, RoundingMode.HALF_UP));
                          else
                            final_NegPrice = objTechLine.getPEENegotUnitPrice();

                          if (proposal_GrossPrice.compareTo(final_NegPrice) > 0) {
                            discount_Amount = objTechLine.getProposalDiscountAmount()
                                .add(objTechLine.getTechDiscountamt())
                                .add(objTechLine.getPEETechDiscountamt());
                          }

                          objTechLine.setDiscountmount(discount_Amount);
                        }
                        if (objTechLine.getDiscountmount().compareTo(BigDecimal.ZERO) > 0) {
                          if (objTechLine.getEscmProposalmgmt().getProposalType().equals("DR"))
                            objTechLine.setDiscount(objTechLine.getPEETechDiscount());
                          else {
                            if (attr.getPEEEfinTaxMethod() != null
                                && attr.getPEEEfinTaxMethod().isPriceIncludesTax())
                              discAmt = objTechLine.getGrossUnitPrice()
                                  .subtract(objTechLine.getPEENegotUnitPrice().add(
                                      objTechLine.getPEELineTaxamt().divide(objTechLine.getPEEQty(),
                                          2, RoundingMode.HALF_UP)));
                            else
                              discAmt = objTechLine.getGrossUnitPrice()
                                  .subtract(objTechLine.getPEENegotUnitPrice());
                            BigDecimal disc = BigDecimal.ZERO;
                            if (objTechLine.getGrossUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
                              disc = (discAmt.divide(objTechLine.getGrossUnitPrice(), 15,
                                  RoundingMode.HALF_UP)).multiply(new BigDecimal(100));
                            }
                            objTechLine.setDiscount(disc);
                          }
                        } else {
                          objTechLine.setDiscount(BigDecimal.ZERO);
                        }
                      } else {
                        objTechLine.setDiscountmount(BigDecimal.ZERO);
                        objTechLine.setDiscount(BigDecimal.ZERO);
                      }
                    // BigDecimal negUnitPricDiscAmt = objTechLine.getDiscount()
                    // .divide(new BigDecimal(100)).multiply(objTechLine.getGrossUnitPrice());
                    // Boolean isPriceInclOfTax = Boolean.FALSE;
                    // if (objTechLine.getEscmProposalmgmt().isTaxLine()) {
                    // isPriceInclOfTax = dao
                    // .isPriceInclusiveOfTax(objTechLine.getEscmProposalmgmt());
                    // }
                    // if (isPriceInclOfTax) {
                    // BigDecimal negTaxAmt = BigDecimal.ZERO;
                    // negTaxAmt = objTechLine.getTaxAmount()
                    // .divide(objTechLine.getMovementQuantity());
                    // objTechLine.setNegotUnitPrice(objTechLine.getGrossUnitPrice()
                    // .subtract(negUnitPricDiscAmt).subtract(negTaxAmt));
                    // } else
                    // objTechLine.setNegotUnitPrice(
                    // objTechLine.getGrossUnitPrice().subtract(negUnitPricDiscAmt));
                    // objTechLine.setLineTotal((objTechLine.getNegotUnitPrice()
                    // .multiply(objTechLine.getMovementQuantity()))
                    // .add(objTechLine.getTaxAmount()));
                    // end update discount and discount amount in proposal line after PEE
                  } else {
                    objTechLine.setStatus(objTechLine.getPeestatus());
                  }
                  OBDal.getInstance().save(objTechLine);
                }
                OBDal.getInstance().flush();
                // after all line cancel in PEE then update attr proposalstatus as cancel
                if (proposallinecount == count) {
                  attr.setProposalstatus("CL");
                  attr.getEscmProposalmgmt().setProposalstatus("CL");
                }
              }

              // chaned 6607
              // grossPrice = attr.getNetPrice();
              //
              List<EscmProposalmgmtLine> lineList = attr.getEscmProposalmgmt()
                  .getEscmProposalmgmtLineList().stream()
                  .filter(a -> (a.getPEETechDiscount() != null
                      && a.getPEETechDiscount().compareTo(BigDecimal.ZERO) > 0))
                  .collect(Collectors.toList());

              if (lineList.size() > 0) {

                // attr.setProsalDiscountamt(grossPrice.subtract(netPrice));
                attr.setProsalDiscountamt(calculated_Discount_Amount);

                if (attr.getProsalDiscountamt().compareTo(BigDecimal.ZERO) > 0) {
                  attr.setProsalDiscount(
                      (attr.getProsalDiscountamt().multiply(new BigDecimal("100")))
                          .divide(grossPrice, 3, RoundingMode.FLOOR));
                } else {
                  attr.setProsalDiscount(BigDecimal.ZERO);
                }
                attr.getEscmProposalmgmt().setDiscountForTheDeal(attr.getProsalDiscount());
                attr.getEscmProposalmgmt().setDiscountAmount(attr.getProsalDiscountamt());
              }
              OBDal.getInstance().save(proposal);
              OBDal.getInstance().save(attr);
            }
          }

          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          // return;
        }

      }
      // submit process end

      // reactive process start
      else if (event.getStatus().equals("CO")) {

        // chk already reactivated or not
        if (event.getStatus().equals("DR")) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }

        // chk approve process started for proposal management or not . if started dont need to
        // allow the reactivate in proposal evaluation event.
        attrlist = event.getEscmProposalAttrList();
        if (attrlist.size() > 0) {
          for (EscmProposalAttribute attr : attrlist) {
            if (attr.getEscmProposalmgmt() != null
                && !attr.getEscmProposalmgmt().getProposalappstatus().equals("INC")
                && !attr.getEscmProposalmgmt().getProposalappstatus().equals("REJ")
                && !attr.getEscmProposalmgmt().getProposalstatus().equals("CL")) {
              errorFlag = true;
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@ESCM_ProEvlEvent_Reactivate@");
              bundle.setResult(result);
              return;
            }
            if (attr.getEscmProposalmgmt() != null
                && !attr.getEscmProposalmgmt().getProposalstatus().equals("ANY")
                && !attr.getEscmProposalmgmt().getProposalstatus().equals("CL")) {
              errorFlag = true;
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@ESCM_ProEvlEventProc_Reactivate@");
              bundle.setResult(result);
              return;
            }
          }
        }

        // Remove PEE Tax and Update TEE Tax details in Proposal Management
        // For Limited and Tender Case
        if (event.getEscmProposalAttrList().get(0).getEscmProposalmgmt() != null
            && !event.getEscmProposalAttrList().get(0).getEscmProposalmgmt().getProposalType()
                .equals("DR")) {
          event.getEscmProposalAttrList().forEach(attr -> {
            if (attr.getEscmProposalmgmt() != null && attr.isPEEIstax()) {
              EscmProposalMgmt proposalMgmt = attr.getEscmProposalmgmt();
              proposalMgmt.getEscmProposalmgmtLineList().forEach(line -> {
                BigDecimal discount_amount = BigDecimal.ZERO;
                line.setNegotUnitPrice(line.getTechUnitPrice());
                line.setDiscountmount(
                    line.getTechDiscountamt().add(line.getProposalDiscountAmount()));

                if (attr.getTEEEfinTaxMethod() != null
                    && attr.getTEEEfinTaxMethod().isPriceIncludesTax())
                  discount_amount = line.getGrossUnitPrice()
                      .subtract(line.getTechUnitPrice().add(line.getTEELineTaxamt()
                          .divide(line.getTechLineQty(), 2, RoundingMode.HALF_UP)));
                else
                  discount_amount = line.getGrossUnitPrice().subtract(line.getTechUnitPrice());

                BigDecimal disc = BigDecimal.ZERO;
                if (line.getGrossUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
                  disc = (discount_amount.divide(line.getGrossUnitPrice(), 15,
                      RoundingMode.HALF_UP)).multiply(new BigDecimal(100));
                }
                line.setDiscount(disc);
                line.setTaxAmount(line.getTEELineTaxamt());
                line.setLineTotal(line.getTechLineTotal());
                // header
                if (attr.isTEEIstax())
                  proposalMgmt.setTaxLine(true);
                else
                  proposalMgmt.setTaxLine(false);
                proposalMgmt.setEfinTaxMethod(attr.getTEEEfinTaxMethod());
              });
            }
          });
        }

        // For Direct Case
        if (event.getEscmProposalAttrList().get(0).getEscmProposalmgmt() != null
            && event.getEscmProposalAttrList().get(0).getEscmProposalmgmt().getProposalType()
                .equals("DR")) {
          ProposalTaxCalculationDAO proposalTaxCalculationDAO = new ProposalTaxCalculationDAOImpl();
          DecimalFormat euroRelationFmt = org.openbravo.erpCommon.utility.Utility.getFormat(vars,
              "euroRelation");
          Integer decimalFormat = euroRelationFmt.getMaximumFractionDigits();
          event.getEscmProposalAttrList().forEach(attr -> {
            if (attr.getEscmProposalmgmt() != null && attr.isPEEIstax()
                && attr.getEscmProposalmgmt().isTaxidentify()) {
              EscmProposalMgmt proposalMgmt = attr.getEscmProposalmgmt();
              proposalMgmt.getEscmProposalmgmtLineList().forEach(line -> {
                if (line.getProposalDiscount().compareTo(BigDecimal.ZERO) > 0) {
                  line.setDiscount(line.getProposalDiscount());
                } else {
                  line.setDiscount(BigDecimal.ZERO);
                }
                if (line.getProposalDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                  line.setDiscountmount(line.getProposalDiscountAmount());
                } else {
                  line.setDiscountmount(BigDecimal.ZERO);
                }
                // Task 8098
                if (line.getProposalDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                  BigDecimal net_calculated_Price = (((line.getGrossUnitPrice()
                      .multiply(line.getBaselineQuantity()))
                          .subtract(line.getProposalDiscountAmount()))
                              .divide(line.getBaselineQuantity())).setScale(2,
                                  RoundingMode.HALF_UP);
                  line.setNegotUnitPrice(net_calculated_Price);
                } else {
                  line.setNegotUnitPrice(line.getNetprice());
                }

                line.setUnittax(BigDecimal.ZERO);
                proposalTaxCalculationDAO.insertTaxAmount(proposalMgmt, decimalFormat);
              });
            } else if (attr.getEscmProposalmgmt() != null && attr.isPEEIstax()
                && !attr.getEscmProposalmgmt().isTaxidentify()) {
              EscmProposalMgmt proposalMgmt = attr.getEscmProposalmgmt();
              proposalMgmt.setTaxLine(false);
              proposalMgmt.setEfinTaxMethod(null);
              proposalMgmt.setTotalTaxAmount(BigDecimal.ZERO);
            }
          });
        }

        if (!errorFlag) {
          // update Proposal event status if we reactivate
          event.setUpdated(new java.util.Date());
          event.setUpdatedBy(OBDal.getInstance().get(User.class, userId));
          event.setStatus("DR");
          event.setAction("CO");
          OBDal.getInstance().save(event);

          if (event.getEscmProposalAttrList().size() > 0) {
            for (EscmProposalAttribute attr : event.getEscmProposalAttrList()) {
              discount_Amount = BigDecimal.ZERO;
              grossPrice = BigDecimal.ZERO;
              netPrice = BigDecimal.ZERO;
              // update Proposal attribute action
              attr.setAction("CO");
              // update proposal status & action
              EscmProposalMgmt proposal = attr.getEscmProposalmgmt();
              if (!proposal.getProposalstatus().equals("CL")) {
                // if (!proposal.getProposalType().equals("DR")) {
                // proposal.setProposalstatus("TER");
                // } else {
                // proposal.setProposalstatus("SUB");
                // }
                proposal.setProposalstatus("ANY");
                proposal.setEscmDocaction("RE");
                proposal.setRank(attr.getRank());

                if (attr.getEscmProposalmgmt() != null
                    && attr.getEscmProposalmgmt().getEscmProposalmgmtLineList().size() > 0) {

                  for (EscmProposalmgmtLine objTechLine : attr.getEscmProposalmgmt()
                      .getEscmProposalmgmtLineList()) {
                    if (!objTechLine.isSummary()) {
                      calculated_gross_price = calculated_gross_price
                          .add((objTechLine.getMovementQuantity())
                              .multiply(objTechLine.getGrossUnitPrice()));
                      calculated_total_discount = calculated_total_discount.add(objTechLine
                          .getTechDiscountamt().add(objTechLine.getProposalDiscountAmount()));
                      if (proposal.getProposalType().equals("DR")) {
                        if (!proposal.isTaxLine()) {
                          grossPrice = grossPrice.add((objTechLine.getMovementQuantity())
                              .multiply(objTechLine.getGrossUnitPrice()));
                          // netPrice = netPrice.add(objTechLine.getLineTotal());

                          if (objTechLine.getProposalDiscountAmount()
                              .compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal calculated_netPrice = objTechLine.getMovementQuantity()
                                .multiply(objTechLine.getGrossUnitPrice())
                                .subtract(objTechLine.getProposalDiscountAmount());
                            netPrice = netPrice.add(calculated_netPrice);
                          } else {
                            netPrice = netPrice.add(objTechLine.getMovementQuantity()
                                .multiply(objTechLine.getGrossUnitPrice()));
                          }
                        }

                      } else {
                        if (!proposal.isTaxLine()) {
                          grossPrice = grossPrice.add((objTechLine.getTechLineQty())
                              .multiply(objTechLine.getGrossUnitPrice()));
                          netPrice = netPrice.add(objTechLine.getTechLineTotal());
                        }

                      }
                    }
                    // for parent update gross price,netprice also with same as negotunitprice
                    else {
                      if (proposal.getProposalType().equals("DR")) {
                        if (!proposal.isTaxLine()) {
                          objTechLine.setGrossUnitPrice(objTechLine.getGrossUnitPrice());
                          objTechLine.setNetprice(objTechLine.getGrossUnitPrice());
                        }

                      } else {
                        if (!proposal.isTaxLine()) {
                          objTechLine.setGrossUnitPrice(objTechLine.getTechUnitPrice());
                          objTechLine.setNetprice(objTechLine.getTechUnitPrice());
                        }

                      }
                    }
                    if (proposal.getProposalType().equals("DR")) {
                      if (!proposal.isTaxLine()) {

                        if (objTechLine.getProposalDiscount().compareTo(BigDecimal.ZERO) > 0) {
                          objTechLine.setDiscount(objTechLine.getProposalDiscount());
                        } else {
                          objTechLine.setDiscount(BigDecimal.ZERO);
                        }
                        if (objTechLine.getProposalDiscountAmount()
                            .compareTo(BigDecimal.ZERO) > 0) {
                          objTechLine.setDiscountmount(objTechLine.getProposalDiscountAmount());
                        } else {
                          objTechLine.setDiscountmount(BigDecimal.ZERO);
                        }
                        // objTechLine.setDiscount(BigDecimal.ZERO);
                        // objTechLine.setDiscountmount(BigDecimal.ZERO);
                        if (objTechLine.getProposalDiscountAmount()
                            .compareTo(BigDecimal.ZERO) > 0) {
                          BigDecimal calculated_netPrice = objTechLine.getMovementQuantity()
                              .multiply(objTechLine.getGrossUnitPrice())
                              .subtract(objTechLine.getProposalDiscountAmount());
                          BigDecimal calculated_unitPrice = calculated_netPrice
                              .divide(objTechLine.getMovementQuantity())
                              .setScale(2, RoundingMode.HALF_UP);
                          objTechLine.setNegotUnitPrice(calculated_unitPrice);
                          objTechLine.setLineTotal(
                              calculated_unitPrice.multiply(objTechLine.getMovementQuantity()));
                        } else {
                          objTechLine.setNegotUnitPrice(objTechLine.getGrossUnitPrice());
                          objTechLine.setLineTotal(objTechLine.getGrossUnitPrice()
                              .multiply(objTechLine.getMovementQuantity()));
                        }
                      }

                      if (objTechLine.getEscmBidmgmtLine() != null) {
                        objTechLine.setMovementQuantity(
                            objTechLine.getEscmBidmgmtLine().getMovementQuantity());
                      }
                      if (objTechLine.getStatus() != null && objTechLine.getStatus().equals("CL")) {
                        objTechLine.setStatus(null);
                      }
                    } else {
                      if (!proposal.isTaxLine()) {
                        BigDecimal discount_amount = BigDecimal.ZERO;
                        objTechLine.setLineTotal(objTechLine.getTechLineTotal());
                        objTechLine.setNegotUnitPrice(objTechLine.getTechUnitPrice());
                        objTechLine.setMovementQuantity(objTechLine.getTechLineQty());
                        objTechLine.setDiscountmount(objTechLine.getTechDiscountamt()
                            .add(objTechLine.getProposalDiscountAmount()));
                        if (attr.getTEEEfinTaxMethod() != null
                            && attr.getTEEEfinTaxMethod().isPriceIncludesTax())
                          discount_amount = objTechLine.getGrossUnitPrice().subtract(
                              objTechLine.getTechUnitPrice().add(objTechLine.getTEELineTaxamt()
                                  .divide(objTechLine.getTechLineQty(), 2, RoundingMode.HALF_UP)));
                        else
                          discount_amount = objTechLine.getGrossUnitPrice()
                              .subtract(objTechLine.getTechUnitPrice());
                        BigDecimal disc = BigDecimal.ZERO;
                        if (objTechLine.getGrossUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
                          disc = (discount_amount.divide(objTechLine.getGrossUnitPrice(), 15,
                              RoundingMode.HALF_UP)).multiply(new BigDecimal(100));
                        }
                        objTechLine.setDiscount(disc);

                        // objTechLine.setDiscount(objTechLine.getTechDiscount());
                        // objTechLine.setDiscountmount(objTechLine.getTechDiscountamt());
                        if (objTechLine.getStatus() != null
                            && objTechLine.getStatus().equals("CL")) {
                          objTechLine.setStatus(null);
                        }
                      }

                    }

                    // netPrice = netPrice.add(objTechLine.getLineTotal());
                    /*
                     * if (proposal.getProposalType().equals("DR")) {
                     * objTechLine.setLineTotal(objTechLine.getGrossUnitPrice()
                     * .multiply(objTechLine.getMovementQuantity()));
                     * objTechLine.setDiscount(BigDecimal.ZERO);
                     * objTechLine.setDiscountmount(BigDecimal.ZERO); } else { //
                     * objTechLine.setLineTotal(objTechLine.getTechLineTotal()); //
                     * objTechLine.setDiscount(objTechLine.getTechDiscount()); //
                     * objTechLine.setDiscountmount(objTechLine.getTechDiscountamt()); //
                     * objTechLine.setPEETechDiscount(BigDecimal.ZERO); //
                     * objTechLine.setPEETechDiscountamt(BigDecimal.ZERO); }
                     */

                  }

                  // changed 6607
                  // grossPrice = attr.getNetPrice();
                  //
                  // attr.setProsalDiscountamt(grossPrice.subtract(netPrice));
                  attr.setProsalDiscountamt(calculated_total_discount);

                  if (attr.getProsalDiscountamt().compareTo(BigDecimal.ZERO) > 0) {
                    attr.setProsalDiscount(
                        (attr.getProsalDiscountamt().multiply(new BigDecimal("100")))
                            .divide(calculated_gross_price, 3, RoundingMode.FLOOR));
                  } else {
                    attr.setProsalDiscount(BigDecimal.ZERO);
                  }
                  attr.getEscmProposalmgmt().setDiscountForTheDeal(attr.getProsalDiscount());
                  attr.getEscmProposalmgmt().setDiscountAmount(attr.getProsalDiscountamt());
                  // attr.setPEETechDiscount(BigDecimal.ZERO);
                  // attr.setPEETechDiscountamt(BigDecimal.ZERO);

                }
              }
              OBDal.getInstance().save(proposal);
              OBDal.getInstance().save(attr);
            }
          }
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          return;
        }

      } // reactive process end
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while ProposalEvaluationEventProcess: ", e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      bundle.setResult(result);
      return;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in ProposalEvaluationEventProcess Submit:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

}
