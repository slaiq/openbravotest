package sa.elm.ob.scm.event;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
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
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;

import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalmgmtLineVersion;
import sa.elm.ob.scm.EscmProposalmgmtVersion;
import sa.elm.ob.scm.ad_process.ProposalManagement.ProposalManagementActionMethod;
import sa.elm.ob.scm.ad_process.ProposalManagement.tax.ProposalTaxCalculationDAO;
import sa.elm.ob.scm.ad_process.ProposalManagement.tax.ProposalTaxCalculationDAOImpl;
import sa.elm.ob.utility.EutForwardReqMoreInfo;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

public class ProposalMgmtEvent extends EntityPersistenceEventObserver {

  private static Logger log = Logger.getLogger(ProposalMgmtEvent.class);
  private static boolean isprocessset = false;

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EscmProposalMgmt.ENTITY_NAME) };
  private static final String PROPOSAL_WINDOW_ID = "CAF2D3EEF3B241018C8F65E8F877B29F";
  ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      EscmProposalMgmt proposalmgmt = (EscmProposalMgmt) event.getTargetInstance();
      final Property proposalSubmissionTime = entities[0]
          .getProperty(EscmProposalMgmt.PROPERTY_SUBMISSIONTIME);
      final Property ProposalNo = entities[0].getProperty(EscmProposalMgmt.PROPERTY_PROPOSALNO);
      final Property envelopeCount = entities[0]
          .getProperty(EscmProposalMgmt.PROPERTY_ENVELOPCOUNTS);
      final Property docaction = entities[0].getProperty(EscmProposalMgmt.PROPERTY_ESCMDOCACTION);
      final Property taxMethod = entities[0].getProperty(EscmProposalMgmt.PROPERTY_EFINTAXMETHOD);
      final Property baseProp = entities[0].getProperty(EscmProposalMgmt.PROPERTY_ESCMBASEPROPOSAL);
      final Property oldProposal = entities[0]
          .getProperty(EscmProposalMgmt.PROPERTY_ESCMOLDPROPOSAL);
      final Property encumbrance = entities[0]
          .getProperty(EscmProposalMgmt.PROPERTY_EFINENCUMBRANCE);
      final Property uniqueCode = entities[0].getProperty(EscmProposalMgmt.PROPERTY_EFINUNIQUECODE);
      final Property contCategory = entities[0].getProperty(EscmProposalMgmt.PROPERTY_CONTRACTTYPE);

      String sequence = "", message = "";
      Boolean sequenceexists = false, Result = false;
      String preferenceValue = "N";
      String budgetController = "N";

      String userId = OBContext.getOBContext().getUser().getId();
      String roldId = OBContext.getOBContext().getRole().getId();
      String clientId = OBContext.getOBContext().getCurrentClient().getId();

      // Check Budget Controller Preference
      // Check Budget Controller Preference

      try {
        budgetController = sa.elm.ob.utility.util.Preferences.getPreferenceValue(
            "ESCM_BudgetControl", true, clientId, proposalmgmt.getOrganization().getId(), userId,
            roldId, PROPOSAL_WINDOW_ID, "N");
        budgetController = (budgetController == null) ? "N" : budgetController;

      } catch (PropertyException e) {
        budgetController = "N";
        // log.error("Exception in getting budget controller :", e);
      }

      if (!budgetController.equals("Y") && proposalmgmt.getEUTForwardReqmoreinfo() != null) {// check
                                                                                             // for
        // temporary
        // preference
        String requester_user_id = proposalmgmt.getEUTForwardReqmoreinfo().getUserContact().getId();
        String requester_role_id = proposalmgmt.getEUTForwardReqmoreinfo().getRole().getId();
        budgetController = forwardDao.checkAndReturnTemporaryPreference("ESCM_BudgetControl",
            roldId, userId, clientId, proposalmgmt.getOrganization().getId(), PROPOSAL_WINDOW_ID,
            requester_user_id, requester_role_id);
      }

      // if Budget Controller Preference not enabled then the user will not able to change the
      // values of
      // Budget related fields
      if (!budgetController.equals("Y") && event.getCurrentState(oldProposal) == null) {
        if (event.getCurrentState(encumbrance) != null
            || event.getCurrentState(uniqueCode) != null) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_Not_BudgetController_Action"));
        }
      }

      if (event.getCurrentState(baseProp) == null) {
        // set new Spec No
        sequence = Utility.getTransactionSequence(proposalmgmt.getOrganization().getId(), "PMG");
        sequenceexists = Utility.chkTransactionSequence(proposalmgmt.getOrganization().getId(),
            "PMG", sequence);
        if (!sequenceexists) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
        }
        // set new Spec No
        if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
          throw new OBException(OBMessageUtils.messageBD("Escm_NoSequence"));
        } else {
          event.setCurrentState(ProposalNo, sequence);
        }
      }

      // check Bid and supplier is unique.
      if (event.getCurrentState(baseProp) == null) {
        Result = checkDuplicate(proposalmgmt);
        if (Result)
          throw new OBException(OBMessageUtils.messageBD("Escm_Bid_Supplier_Duplicate"));
      }
      // check bid proposals last day is not less than Proposal creation date
      // get the preference value
      try {
        preferenceValue = Preferences.getPreferenceValue("Escm_Proposal_Creation", true,
            proposalmgmt.getClient().getId(), proposalmgmt.getOrganization().getId(),
            OBContext.getOBContext().getUser().getId(), OBContext.getOBContext().getRole().getId(),
            Constants.PROPOSAL_MANAGEMENT_W);
        if (preferenceValue == null) {
          preferenceValue = "N";
        }
      } catch (PropertyException e) {
        preferenceValue = "N";
      }
      if (proposalmgmt.getEscmBidmgmt() != null) {
        message = getmaxbidproposallastdayandbidnumber(proposalmgmt);
        if (message != "Success" && (preferenceValue != null && preferenceValue.equals("N"))) {
          throw new OBException(message);
        }
      }

      if (proposalmgmt.getEscmBidmgmt() == null) {
        if (proposalmgmt.getProposalType().equals("TR")
            || proposalmgmt.getProposalType().equals("LD"))
          throw new OBException(OBMessageUtils.messageBD("Escm_NoBidNoTRAndLD"));
      }

      // check whether tax method is selected for calculating tax
      if (proposalmgmt.isTaxLine() && proposalmgmt.getEfinTaxMethod() == null) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoTaxMethod"));
      }

      // Check submissiontime entered is valid
      String submissionTime = event.getCurrentState(proposalSubmissionTime).toString();
      String propTime[] = submissionTime.split(":");

      if (Integer.parseInt(propTime[0]) > 23) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_NotValidSubTime"));
      }

      // check envelope count is digit
      String count = event.getCurrentState(envelopeCount).toString();
      if (!StringUtils.isNumeric(count)) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_NotvalidEnvCount"));
      }
      /* Validate encumbrance cannot be empty if encumbrance method is manual */
      if (proposalmgmt.getEFINEncumbranceMethod().equals("M")) {
        if (proposalmgmt.getEfinEncumbrance() == null) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_EncumNotEmptyForManual"));
        }
      }
      if (!proposalmgmt.isNeedEvaluation() && proposalmgmt.getProposalstatus().equals("DR")
          && proposalmgmt.getProposalappstatus().equals("INC")) {
        event.setCurrentState(docaction, "SA");
      }
      if (!proposalmgmt.isTaxLine()) {
        event.setCurrentState(taxMethod, null);
      }
      if (proposalmgmt.getProposalType().equals("DR")) {
        if (proposalmgmt.getEscmBidmgmt() == null
            && (proposalmgmt.getBidName() == null || proposalmgmt.getBidName().equals(""))) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BidNamecantbeempty"));
        }
      }
      // if issecondsupplieriban as checked then iban as mandatory
      if (proposalmgmt.isSecondsupplier()) {
        if (proposalmgmt.getIBAN() == null) {
          throw new OBException(OBMessageUtils.messageBD("Escm_second_Iban_Mandatory"));
        }
      }

      // checking whether the contract category is active or not
      if (proposalmgmt.getEscmBidmgmt() != null) {
        EscmBidMgmt bidmgmt = OBDal.getInstance().get(EscmBidMgmt.class,
            proposalmgmt.getEscmBidmgmt().getId());
        if (bidmgmt != null) {
          if (bidmgmt.getContractType() != null && !bidmgmt.getContractType().isActive()) {
            throw new OBException(OBMessageUtils.messageBD("Escm_Contract_Inactive_Bid"));
          } else {
            event.setCurrentState(contCategory, bidmgmt.getContractType());
          }
        }
      }

    } catch (OBException e) {
      log.error("exception while creating ProposalManagementEvent", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while creating ProposalManagementEvent", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) throws ParseException {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      EscmProposalMgmt proposalmgmt = (EscmProposalMgmt) event.getTargetInstance();
      Boolean Result = false;
      final Property BidNo = entities[0].getProperty(EscmProposalMgmt.PROPERTY_ESCMBIDMGMT);
      final Property envelopeCount = entities[0]
          .getProperty(EscmProposalMgmt.PROPERTY_ENVELOPCOUNTS);
      final Property supplier = entities[0].getProperty(EscmProposalMgmt.PROPERTY_SUPPLIER);
      final Property proposalSubmissionTime = entities[0]
          .getProperty(EscmProposalMgmt.PROPERTY_SUBMISSIONTIME);
      final Property proposalSubmissionDate = entities[0]
          .getProperty(EscmProposalMgmt.PROPERTY_SUBMISSIONDATE);
      final Property docaction = entities[0].getProperty(EscmProposalMgmt.PROPERTY_ESCMDOCACTION);
      final Property needEval = entities[0].getProperty(EscmProposalMgmt.PROPERTY_NEEDEVALUATION);
      final Property propTyp = entities[0].getProperty(EscmProposalMgmt.PROPERTY_PROPOSALTYPE);
      final Property taxMethod = entities[0].getProperty(EscmProposalMgmt.PROPERTY_EFINTAXMETHOD);
      final Property taxamount = entities[0].getProperty(EscmProposalMgmt.PROPERTY_TOTALTAXAMOUNT);
      final Property isTaxLine = entities[0].getProperty(EscmProposalMgmt.PROPERTY_ISTAXLINE);
      final Property baseProp = entities[0].getProperty(EscmProposalMgmt.PROPERTY_ESCMBASEPROPOSAL);
      final Property propStatus = entities[0].getProperty(EscmProposalMgmt.PROPERTY_PROPOSALSTATUS);
      final Property contCategory = entities[0].getProperty(EscmProposalMgmt.PROPERTY_CONTRACTTYPE);
      String message = "";
      String preferenceValue = "";
      // final UIDefinitionController.FormatDefinition formatDef =
      // UIDefinitionController.getInstance()
      // .getFormatDefinition("euro", "Relation");
      // DecimalFormat decimal = new DecimalFormat(formatDef.getFormat());
      // Integer decimalFormat = decimal.getMaximumFractionDigits();
      // Integer roundoffConst = 3;
      ProposalTaxCalculationDAO taxDao = new ProposalTaxCalculationDAOImpl();

      String userId = OBContext.getOBContext().getUser().getId();
      String roleId = OBContext.getOBContext().getRole().getId();
      String clientId = OBContext.getOBContext().getCurrentClient().getId();

      final Property encumbrance = entities[0]
          .getProperty(EscmProposalMgmt.PROPERTY_EFINENCUMBRANCE);
      final Property uniqueCode = entities[0].getProperty(EscmProposalMgmt.PROPERTY_EFINUNIQUECODE);
      final Property encumMethod = entities[0]
          .getProperty(EscmProposalMgmt.PROPERTY_EFINENCUMBRANCEMETHOD);
      final Property forwardId = entities[0]
          .getProperty(EscmProposalMgmt.PROPERTY_EUTFORWARDREQMOREINFO);
      final Property add_requisition = entities[0]
          .getProperty(EscmProposalMgmt.PROPERTY_ADDREQUISITION);
      String budgetController = "N";

      // Check Budget Controller Preference
      try {
        budgetController = sa.elm.ob.utility.util.Preferences.getPreferenceValue(
            "ESCM_BudgetControl", Boolean.TRUE, clientId, proposalmgmt.getOrganization().getId(),
            userId, roleId, PROPOSAL_WINDOW_ID, "N");
        budgetController = (budgetController == null) ? "N" : budgetController;
      } catch (PropertyException e) {
        budgetController = "N";
        // log.error("Exception in getting budget controller :", e);
      }
      // find out the forward reference record against the Proposal
      EutForwardReqMoreInfo objForward = proposalmgmt.getEUTForwardReqmoreinfo();

      if (objForward == null) {
        objForward = forwardDao.findForwardReferenceAgainstTheRecord(proposalmgmt.getId(), userId,
            roleId);
      }
      if (!budgetController.equals("Y") && objForward != null) {// check for
        // temporary
        // preference
        String requester_user_id = objForward.getUserContact().getId();
        String requester_role_id = objForward.getRole().getId();
        budgetController = forwardDao.checkAndReturnTemporaryPreference("ESCM_BudgetControl",
            roleId, userId, clientId, proposalmgmt.getOrganization().getId(), PROPOSAL_WINDOW_ID,
            requester_user_id, requester_role_id);

      }

      // if Budget Controller Preference not
      // enabled then the user will not able to change the
      // values of
      // Budget related fields

      if (!budgetController.equals("Y") && event.getCurrentState(add_requisition).equals("N")) {
        if ((event.getCurrentState(encumbrance) != null
            && !event.getCurrentState(encumbrance).equals(event.getPreviousState(encumbrance)))
            || (event.getCurrentState(encumMethod) != null
                && !event.getCurrentState(encumMethod).equals(event.getPreviousState(encumMethod)))
            || (event.getCurrentState(uniqueCode) != null
                && !event.getCurrentState(uniqueCode).equals(event.getPreviousState(uniqueCode)))) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_Not_BudgetController_Action"));
        }
      }

      // check Bid and supplier is unique.
      if (event.getCurrentState(baseProp) == null) {
        if (event.getCurrentState(BidNo) != event.getPreviousState(BidNo)
            || event.getCurrentState(supplier) != event.getPreviousState(supplier)) {
          Result = checkDuplicate(proposalmgmt);
        }
        if (Result)
          throw new OBException(OBMessageUtils.messageBD("Escm_Bid_Supplier_Duplicate"));
      }

      // Check submissiontime entered is valid
      if (event.getCurrentState(proposalSubmissionTime) != null
          && !event.getCurrentState(proposalSubmissionTime)
              .equals(event.getPreviousState(proposalSubmissionTime))) {
        String submissionTime = event.getCurrentState(proposalSubmissionTime).toString();
        String propTime[] = submissionTime.split(":");

        if (Integer.parseInt(propTime[0]) > 23) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_NotValidSubTime"));
        }
      }
      // check envelope count is digit
      String count = event.getCurrentState(envelopeCount).toString();
      if (!StringUtils.isNumeric(count)) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_NotvalidEnvCount"));
      }

      // check whether tax method is selected for calculating tax
      if (proposalmgmt.isTaxLine() && proposalmgmt.getEfinTaxMethod() == null) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoTaxMethod"));
      }

      // should not allow to chanage bid if have lines
      if (event.getCurrentState(BidNo) != null) {
        if (event.getCurrentState(BidNo) != event.getPreviousState(BidNo)
            && proposalmgmt.getEscmProposalmgmtLineList().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_BidChange_Notallowed"));
        }
      }
      /* Validate encumbrance cannot be empty if encumbrance method is manual */
      if (proposalmgmt.getEFINEncumbranceMethod() != null
          && proposalmgmt.getEFINEncumbranceMethod().equals("M")) {
        if (proposalmgmt.getEfinEncumbrance() == null) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_EncumNotEmptyForManual"));
        }
      }
      if (proposalmgmt.getProposalType() != null && proposalmgmt.getEscmBidmgmt() == null
          && !event.getCurrentState(propStatus).equals("DIS")
          && !event.getCurrentState(propStatus).equals("WD")
          && !event.getCurrentState(propStatus).equals("CL")) {
        if (proposalmgmt.getProposalType().equals("TR")
            || proposalmgmt.getProposalType().equals("LD"))
          throw new OBException(OBMessageUtils.messageBD("Escm_NoBidNoTRAndLD"));
      }
      // check bid proposals last day is not less than Proposal creation date
      // get the preference value
      try {
        preferenceValue = Preferences.getPreferenceValue("Escm_Proposal_Creation", true,
            proposalmgmt.getClient().getId(), proposalmgmt.getOrganization().getId(),
            OBContext.getOBContext().getUser().getId(), OBContext.getOBContext().getRole().getId(),
            Constants.PROPOSAL_MANAGEMENT_W);
        if (preferenceValue == null) {
          preferenceValue = "N";
        }

      } catch (PropertyException e) {
        preferenceValue = "N";
      }
      // Check Proposal Submission Date against Bid Last Date
      if (event.getCurrentState(BidNo) != null
          && (!event.getCurrentState(BidNo).equals(event.getPreviousState(BidNo)))) {
        message = getmaxbidproposallastdayandbidnumber(proposalmgmt);
        if (message != "Success" && (preferenceValue != null && preferenceValue.equals("N"))) {
          throw new OBException(message);
        }
      }

      if (proposalmgmt.getEscmBidmgmt() != null) {
        if (!event.getCurrentState(proposalSubmissionDate)
            .equals(event.getPreviousState(proposalSubmissionDate))
            || (!event.getCurrentState(proposalSubmissionTime)
                .equals(event.getPreviousState(proposalSubmissionTime)))) {
          message = getmaxbidproposallastdayandbidnumber(proposalmgmt);
          if (message != "Success" && (preferenceValue != null && preferenceValue.equals("N"))) {
            throw new OBException(message);
          }
        }
      }
      if (event.getCurrentState(propTyp).equals("TR")
          || event.getCurrentState(propTyp).equals("LD")) {
        event.setCurrentState(needEval, true);
      }
      if (!proposalmgmt.isNeedEvaluation() && proposalmgmt.getProposalstatus().equals("DR")
          && (proposalmgmt.getProposalappstatus().equals("INC")
              || proposalmgmt.getProposalappstatus().equals("REJ")
              || proposalmgmt.getProposalappstatus().equals("REA"))) {
        event.setCurrentState(docaction, "SA");
      } else if (proposalmgmt.isNeedEvaluation() && proposalmgmt.getProposalstatus().equals("DR")
          && (proposalmgmt.getProposalappstatus().equals("INC")
              || proposalmgmt.getProposalappstatus().equals("REJ")
              || proposalmgmt.getProposalappstatus().equals("REA"))) {
        event.setCurrentState(docaction, "CO");
      }
      if (event.getPreviousState(isTaxLine) != null
          && !event.getPreviousState(isTaxLine).equals(event.getCurrentState(isTaxLine))) {
        if (!proposalmgmt.isTaxLine()) {
          event.setCurrentState(taxMethod, null);
          event.setCurrentState(taxamount, BigDecimal.ZERO);
          List<EscmProposalmgmtLine> taxableLines = new ArrayList<EscmProposalmgmtLine>();
          taxableLines = taxDao.getProposalLines(proposalmgmt);
          // roundoffConst = decimalFormat;

          if (taxableLines.size() > 0) {
            for (EscmProposalmgmtLine line : taxableLines) {

              if (line.getProposalDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal netPrice = (((line.getGrossUnitPrice()
                    .multiply(line.getBaselineQuantity()))
                        .subtract(line.getProposalDiscountAmount()))
                            .divide(line.getBaselineQuantity(), 15, RoundingMode.HALF_UP))
                                .setScale(2, RoundingMode.HALF_UP);
                line.setNegotUnitPrice(netPrice);
                line.setNetUnitprice(netPrice);
                line.setUnitpricedis(netPrice);
                line.setLineTotal(netPrice.multiply(line.getBaselineQuantity()));
              } else {
                line.setNegotUnitPrice(line.getNetprice());
                line.setNetUnitprice(line.getNetprice());
                line.setUnitpricedis(line.getNetprice());
                line.setLineTotal(line.getNetprice().multiply(line.getBaselineQuantity()));
              }

              // line.setNegotUnitPrice(line.getNetprice());
              // line.setLineTotal(line.getNetprice().multiply(line.getMovementQuantity()).subtract(
              // line.getDiscountmount() == null ? BigDecimal.ZERO : line.getDiscountmount()));
              line.setTaxAmount(BigDecimal.ZERO);
              line.getEscmProposalmgmt().setCalculateTax(true);
              line.setProcess(true);
              OBDal.getInstance().save(line);
            }
          }
          // event.setCurrentState(isProcess, true);
          // proposalmgmt.setProcess(true);
          // OBDal.getInstance().save(proposalmgmt);

          taxDao.updateParentRecord(proposalmgmt);
        }
      }
      if (proposalmgmt.getProposalType().equals("DR")) {
        if (proposalmgmt.getEscmBidmgmt() == null
            && (proposalmgmt.getBidName() == null || proposalmgmt.getBidName().equals(""))) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BidNamecantbeempty"));
        }
      }
      // if issecondsupplieriban as checked then iban as mandatory
      if (proposalmgmt.isSecondsupplier()) {
        if (proposalmgmt.getIBAN() == null) {
          throw new OBException(OBMessageUtils.messageBD("Escm_second_Iban_Mandatory"));
        }
      }

      // checking whether the contract category is active or not
      if (proposalmgmt.getEscmBidmgmt() != null) {
        EscmBidMgmt bidmgmt = OBDal.getInstance().get(EscmBidMgmt.class,
            proposalmgmt.getEscmBidmgmt().getId());
        if (bidmgmt != null) {
          if (bidmgmt.getContractType() != null && !bidmgmt.getContractType().isActive()) {
            throw new OBException(OBMessageUtils.messageBD("Escm_Contract_Inactive_Bid"));
          } else {
            event.setCurrentState(contCategory, bidmgmt.getContractType());
          }
        }
      }
    } catch (OBException e) {
      log.error("exception while updating ProposalManagementEvent", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while updating ProposalManagementEvent", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      EscmProposalMgmt proposalmgmt = (EscmProposalMgmt) event.getTargetInstance();
      String prevProposalId = proposalmgmt.getEscmOldproposal() == null ? null
          : proposalmgmt.getEscmOldproposal().getId();

      if (proposalmgmt.getProposalstatus().equals("AWD")
          || proposalmgmt.getProposalstatus().equals("CL")
          || proposalmgmt.getProposalstatus().equals("DIS")
          || proposalmgmt.getProposalstatus().equals("WD")) {
        if (proposalmgmt.getEscmBaseproposal() == null
            || (proposalmgmt.getEscmBaseproposal() != null && proposalmgmt.isVersion())
            || (proposalmgmt.getEscmBaseproposal() != null && !proposalmgmt.isVersion()
                && !proposalmgmt.getEscmDocaction().equals("SA")
                && !proposalmgmt.getProposalappstatus().equals("REA")))
          throw new OBException(OBMessageUtils.messageBD("Escm_proposal_deletelines"));
      }

      if (proposalmgmt.isEfinIsbudgetcntlapp()) {
        if (proposalmgmt.getEscmProposalmgmtLineList().size() > 0) {
          JSONObject result = ProposalManagementActionMethod.isrevertEncumbrance(proposalmgmt);
          if (result != null && result.has("errorFlag") && result.getBoolean("errorFlag")) {
            throw new OBException(OBMessageUtils.messageBD(result.getString("message")));
          }
        }
      }

      if (proposalmgmt.getEscmProposalmgmtLnVerList().size() > 0) {
        for (EscmProposalmgmtLineVersion verObj : proposalmgmt.getEscmProposalmgmtLnVerList()) {
          OBDal.getInstance().remove(verObj);
        }
      }

      List<EscmProposalmgmtLine> lines = proposalmgmt.getEscmProposalmgmtLineList();
      proposalmgmt.getEscmProposalmgmtLineList().removeAll(lines);
      OBDal.getInstance().flush();

      if (proposalmgmt.getEscmProposalmgmtVerList().size() > 0) {
        for (EscmProposalmgmtVersion verObj : proposalmgmt.getEscmProposalmgmtVerList()) {
          OBDal.getInstance().remove(verObj);
        }
      }

      if (prevProposalId != null) {
        EscmProposalMgmt prevProposalmgmt = Utility.getObject(EscmProposalMgmt.class,
            prevProposalId);
        // prevProposalmgmt.setProposalappstatus("REA");
        prevProposalmgmt.setVersion(false);
        OBDal.getInstance().save(prevProposalmgmt);
      }

      OBDal.getInstance().save(proposalmgmt);

      // List<EscmProposalRegulation> regulationLine = new ArrayList<EscmProposalRegulation>();
      // regulationLine = proposalmgmt.getEscmProposalRegulationList();

      /*
       * // iterate regulation deocument to delete.
       * proposalmgmt.getEscmProposalRegulationList().removeAll(regulationLine); for
       * (EscmProposalRegulation regulation : regulationLine) {
       * OBDal.getInstance().remove(regulation); }
       */

    } catch (OBException e) {
      log.error("exception while deleting ProposalManagementEvent", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while deleting ProposalManagementEvent", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
  }

  /**
   * Method to check duplicate supplier
   * 
   * @param proposalmgmt
   * @return
   */
  public boolean checkDuplicate(EscmProposalMgmt proposalmgmt) {
    if (proposalmgmt.getEscmBidmgmt() != null) {
      OBQuery<EscmProposalMgmt> Duplicate = OBDal.getInstance().createQuery(EscmProposalMgmt.class,
          "escmBidmgmt.id=:bidID and supplier.id=:supplierID and proposalstatus<>'CL'");
      Duplicate.setNamedParameter("bidID", proposalmgmt.getEscmBidmgmt().getId());
      Duplicate.setNamedParameter("supplierID", proposalmgmt.getSupplier().getId());

      if (Duplicate.list().size() > 0) {
        return true;
      } else {
        return false;
      }
    }
    return false;
  }

  public static String getmaxbidproposallastdayandbidnumber(EscmProposalMgmt proposalmgmt) {
    String sqlquery = null, bidno = "", strproposallastday = "", proposallastdayhijri = "";
    Date proposallastday = null;
    Query query = null;
    String message = "";

    String proposalSubmissionDate = null;
    Date proposalSubDate = null;
    SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    DateFormat yearFormat = Utility.YearFormat;

    String preferenceValue = "N";
    try {
      preferenceValue = Preferences.getPreferenceValue("ESCM_AllowPastDate", true,
          proposalmgmt.getClient().getId(), proposalmgmt.getOrganization().getId(),
          OBContext.getOBContext().getUser().getId(), OBContext.getOBContext().getRole().getId(),
          Constants.PROPOSAL_MANAGEMENT_W);
      if (preferenceValue == null) {
        preferenceValue = "N";
      }
    } catch (PropertyException e) {
      preferenceValue = "N";
    }

    sqlquery = " select to_date(to_char(maxbd.proposallastday,'yyyy-MM-dd HH24:mi:ss'),'yyyy-MM-dd HH24:mi:ss') , bidm.bidno "
        + " ,eut_convert_to_hijri(to_char(maxbd.proposallastday,'yyyy-MM-dd')), max( maxbd.proposallastdaytime) "
        + " from ( select escm_bidmgmt_id, max(escm_biddates.proposallastday) as lastday from  escm_biddates group by escm_bidmgmt_id ) a "
        + " join escm_biddates maxbd on a.lastday= maxbd.proposallastday and a.escm_bidmgmt_id= maxbd.escm_bidmgmt_id "
        + " join escm_bidmgmt bidm on bidm.escm_bidmgmt_id=maxbd.escm_bidmgmt_id "
        + " where bidm.escm_bidmgmt_id=?  group by maxbd.proposallastday,bidm.bidno ";
    query = OBDal.getInstance().getSession().createSQLQuery(sqlquery);
    query.setParameter(0, proposalmgmt.getEscmBidmgmt().getId());
    @SuppressWarnings("unchecked")
    List<Object> list = query.list();
    if (query.list().size() > 0) {
      for (@SuppressWarnings("rawtypes")
      Iterator iterator = list.iterator(); iterator.hasNext();) {
        Object[] row = (Object[]) iterator.next();
        if (row[0] != null) {
          try {
            strproposallastday = row[0].toString();
            proposallastday = dateformat
                .parse(strproposallastday + " " + row[3].toString() + ":00");
            if (proposalmgmt.getSubmissiondate() != null
                && proposalmgmt.getSubmissiontime() != null) {
              proposalSubmissionDate = yearFormat.format(proposalmgmt.getSubmissiondate()) + " "
                  + proposalmgmt.getSubmissiontime() + ":00";
              proposalSubDate = dateformat.parse(proposalSubmissionDate);
            }

          } catch (ParseException e) {
            // TODO Auto-generated catch block
            log.error("exception while getmaxbidproposallastdayandbidnumber() "
                + "in ProposalManagementEvent", e);
          }
        }
        if (row[1] != null) {
          bidno = row[1].toString();
        }
        if (row[2] != null) {
          proposallastdayhijri = row[2].toString() + " " + row[3].toString() + ":00";
        }
      }
    }

    // Skip Past Date validation if SCM_AllowPastDate preference is present
    if (proposallastday != null && proposalSubDate != null
        && proposallastday.compareTo(proposalSubDate) < 0 && preferenceValue.equals("N")) {

      message = OBMessageUtils.messageBD("ESCM_BidProposallastday");
      message = message.replace("$", proposallastdayhijri).replace("%", bidno);

    } else {
      message = "Success";
    }
    return message;

  }
}
