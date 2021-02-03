package sa.elm.ob.scm.event;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;
import java.util.regex.Pattern;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
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
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.ESCMBGWorkbench;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.Escmbankguaranteedetail;
import sa.elm.ob.scm.event.dao.BankGuaranteeDetailEventDAO;

public class BankGuaranteeDetailEvent extends EntityPersistenceEventObserver {

  private static Logger log = Logger.getLogger(BankGuaranteeDetailEvent.class);
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Escmbankguaranteedetail.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) throws ParseException {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      Escmbankguaranteedetail bankguarantee = (Escmbankguaranteedetail) event.getTargetInstance();

      final Property internalno = entities[0]
          .getProperty(Escmbankguaranteedetail.PROPERTY_INTERNALNO);
      // final Property bgstatus =
      // entities[0].getProperty(Escmbankguaranteedetail.PROPERTY_BGSTATUS);
      final Property proposalId = entities[0]
          .getProperty(Escmbankguaranteedetail.PROPERTY_ESCMPROPOSALMGMT);
      final Property proposalAttId = entities[0]
          .getProperty(Escmbankguaranteedetail.PROPERTY_ESCMPROPOSALATTR);
      final Property isbgworkbench = entities[0]
          .getProperty(Escmbankguaranteedetail.PROPERTY_ISBGWORKBENCH);
      final Property extendexpdate = entities[0]
          .getProperty(Escmbankguaranteedetail.PROPERTY_EXTENDEXPDATEH);
      final Property workbench = entities[0]
          .getProperty(Escmbankguaranteedetail.PROPERTY_ESCMBGWORKBENCH);
      final Property seqNo = entities[0].getProperty(Escmbankguaranteedetail.PROPERTY_LINENO);
      EscmProposalMgmt proposal = null;
      // String sequence = "";
      // Boolean sequenceexists;
      // validateHeaderFields(bankguarantee);

      // while adding line in bank guarantee details tab in OEE, if OEE is completed then we should
      // not allow to add line

      if (bankguarantee.getEscmProposalAttr() != null
          && bankguarantee.getEscmBgworkbench() == null) {
        if ("CO".equals(
            bankguarantee.getEscmProposalAttr().getEscmOpenenvcommitee().getAlertStatus())) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_CantaddlineOEE"));
        }
      }

      // If it is saving from open envelope event , then we should create header
      if (bankguarantee.getEscmBgworkbench() == null) {
        ESCMBGWorkbench bgWorkbench = BankGuaranteeDetailEventDAO.createWorkBench(bankguarantee);

        if ("CO".equals(bgWorkbench.getBghdstatus())) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_CantaddlineBG"));
        }

        bankguarantee.setEscmBgworkbench(bgWorkbench);
        OBDal.getInstance().save(bgWorkbench);
        OBDal.getInstance().save(bankguarantee);

        event.setCurrentState(workbench, bgWorkbench);
        event.setCurrentState(internalno, bgWorkbench.getInternalNo());

        // Setting line no
        SQLQuery lineNoQry = OBDal.getInstance().getSession().createSQLQuery(
            "SELECT COALESCE(MAX(LINE),0)+10 FROM Escm_Bankguarantee_Detail  WHERE escm_proposal_attr_id =:attrid");
        lineNoQry.setParameter("attrid", bankguarantee.getEscmProposalAttr().getId());

        @SuppressWarnings("unchecked")
        List<Object> lineObj = lineNoQry.list();

        if (lineObj != null && lineObj.size() > 0) {
          BigDecimal lineNo = (BigDecimal) lineObj.get(0);
          event.setCurrentState(seqNo, lineNo.longValue());
        }

      }

      /** set new Spec No */
      /*
       * Task no:5336 sequence = Utility.getTransactionSequencewithclient("0",
       * bankguarantee.getClient().getId(), "BGD"); sequenceexists =
       * Utility.chkTransactionSequencewithclient(bankguarantee.getOrganization() .getId(),
       * bankguarantee.getClient().getId(), "BGD", sequence);
       * 
       * if (!sequenceexists) { throw new
       * OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence")); }
       *//** thorw the error if same sequence exists */
      /*
       * if (sequence.equals("false") || StringUtils.isEmpty(sequence)) { throw new
       * OBException(OBMessageUtils.messageBD("Escm_NoSequence")); } else {
       * event.setCurrentState(internalno, sequence); }
       */
      /** Task no :0005277 set bg status as Active **/
      // event.setCurrentState(bgstatus, "ACT");

      /** update the proposal attId, ProposalId for if we create bg under bg workbench window. */
      if (bankguarantee.getDocumentNo() != null || bankguarantee.getSalesOrder() != null
          || bankguarantee.getEscmProposalmgmt() != null) {
        proposal = bankguarantee.getEscmProposalmgmt();
      }

      // ** update proposal attId ,ProposalId for if we create bg under Order. *//*
      if (bankguarantee.getSalesOrder() != null
          && bankguarantee.getSalesOrder().getEscmProposalmgmt() != null) {
        proposal = OBDal.getInstance().get(EscmProposalMgmt.class,
            bankguarantee.getSalesOrder().getEscmProposalmgmt().getId());
        event.setCurrentState(proposalId, proposal);
        event.setCurrentState(proposalAttId, BankGuaranteeDetailEventDAO
            .getProposalAttribute(bankguarantee.getSalesOrder().getEscmProposalmgmt().getId()));
      }
      /** chk unique constraint for bank bg no and proposal no **/
      /*
       * Task No: 5336 if (bankguarantee.getEscmProposalmgmt() != null) {
       * OBQuery<Escmbankguaranteedetail> bankguaranteedet = OBDal.getInstance().createQuery(
       * Escmbankguaranteedetail.class, " client.id='" + bankguarantee.getClient().getId() +
       * "' and bankbgno='" + bankguarantee.getBankbgno() + "' and escmProposalmgmt.id = '" +
       * bankguarantee.getEscmProposalmgmt().getId() + "' "); if (bankguaranteedet.list().size() >
       * 0) { throw new OBException(OBMessageUtils.messageBD("Escm_BankBgNo")); } } else {
       */
      if (bankguarantee.getBankbgno() != null
          && BankGuaranteeDetailEventDAO.checkUniqueBGNo(bankguarantee)) {
        throw new OBException(OBMessageUtils.messageBD("Escm_BankBgNo"));
      }
      // }

      /** chk unique Constraint for documentno and bank name **/
      if (BankGuaranteeDetailEventDAO.checksameDocNowithSameBank(bankguarantee)) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_SameDocNOWithSameBankPre"));

      }
      if (bankguarantee.getBgstartdateh().compareTo(bankguarantee.getExpirydateh()) > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_BGStatdatGrtthanExpdate"));
      }

      // Task No. 0005277
      log.debug("proposal:" + proposal);
      if (bankguarantee.getEscmBgworkbench().getEscmProposalmgmt() != null
          && bankguarantee.getEscmBgworkbench().getBidNo() != null) {
        Boolean Result = BankGuaranteeDetailEventDAO.validBGDatewithOpenEnvel(bankguarantee,
            bankguarantee.getEscmBgworkbench().getEscmProposalmgmt());
        if (Result) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BGExpDatCompWithOpenEnv"));
        }
      }
      if (bankguarantee.getBankguaranteetype().equals("IBG")) {
        BankGuaranteeDetailEventDAO.validateBGValue(bankguarantee);
      }

      /** update isbgworkbench values as yes if record is created by BG Workbench window */
      if (bankguarantee.getBusinessPartner() != null) {
        event.setCurrentState(isbgworkbench, true);
      }

      /** check document no is mandatory when document type selected as P */
      /*
       * Task No: 5336 if (bankguarantee.getDocumentType() != null &&
       * bankguarantee.getDocumentType().equals("P")) { if (bankguarantee.getDocumentNo() == null) {
       * throw new OBException(OBMessageUtils.messageBD("ESCM_BGDocNoNotEmpty")); } }
       */

      /** check bank percentage is more than 100 if multi bank check for same documentno */
      /*
       * Task No: 0005277 if (bankguarantee.getBankPercentage() != null &&
       * (bankguarantee.getDocumentNo() != null || bankguarantee.getSalesOrder() != null ||
       * bankguarantee .getEscmProposalmgmt() != null)) { if
       * (!BankGuaranteeDetailEventDAO.checkBankPercentage(bankguarantee)) { throw new
       * OBException(OBMessageUtils.messageBD("ESCM_BankPercentage_Validation")); } }
       */

      if (bankguarantee.getInitialbg() != null) {
        if (new BigDecimal(bankguarantee.getInitialbg()).compareTo(new BigDecimal(100)) > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BGRateMorThanHundPer"));
        }
      }
      event.setCurrentState(extendexpdate, bankguarantee.getExpirydateh());

      if (bankguarantee.getEscmBgworkbench().isMultiBanks() != null
          && !bankguarantee.getEscmBgworkbench().isMultiBanks()) {
        if (bankguarantee.getEscmBgworkbench().getEscmBankguaranteeDetailList().size() > 0) {
          // throw new OBException(OBMessageUtils.messageBD("ESCM_BGISMultiBank"));
        }
      }

    } catch (OBException e) {
      log.debug("exception while creating Bankguarantee details" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.debug("exception while creating Bankguarantee details" + e);
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
      Escmbankguaranteedetail bankguarantee = (Escmbankguaranteedetail) event.getTargetInstance();
      final Property bankbgno = entities[0].getProperty(Escmbankguaranteedetail.PROPERTY_BANKBGNO);
      // final Property bankpercent = entities[0]
      // .getProperty(Escmbankguaranteedetail.PROPERTY_BANKPERCENTAGE);
      final Property intialBg = entities[0].getProperty(Escmbankguaranteedetail.PROPERTY_BGAMOUNT);
      final Property bgstartdateH = entities[0]
          .getProperty(Escmbankguaranteedetail.PROPERTY_BGSTARTDATEH);
      final Property expiredateh = entities[0]
          .getProperty(Escmbankguaranteedetail.PROPERTY_EXPIRYDATEH);
      // final Property documentNo = entities[0]
      // .getProperty(Escmbankguaranteedetail.PROPERTY_DOCUMENTNO);
      final Property bankName = entities[0].getProperty(Escmbankguaranteedetail.PROPERTY_BANKNAME);
      final Property foreignbankname = entities[0]
          .getProperty(Escmbankguaranteedetail.PROPERTY_FOREIGNBANKNAME);
      final Property initialbg = entities[0]
          .getProperty(Escmbankguaranteedetail.PROPERTY_INITIALBG);

      if ((event.getCurrentState(bankbgno) != null
          && !event.getCurrentState(bankbgno).equals(event.getPreviousState(bankbgno)))
          || (event.getCurrentState(foreignbankname) != null && !event
              .getCurrentState(foreignbankname).equals(event.getPreviousState(foreignbankname)))) {
        // validateHeaderFields(bankguarantee);
      }
      /** chk unique constraint for bank bg no and proposal no */
      if (event.getCurrentState(bankbgno) != null
          && !event.getCurrentState(bankbgno).equals(event.getPreviousState(bankbgno))) {
        /*
         * Task No: 5336 if (bankguarantee.getEscmProposalAttr() != null) {
         * OBQuery<Escmbankguaranteedetail> bankguaranteedet = OBDal.getInstance().createQuery(
         * Escmbankguaranteedetail.class, " client.id='" + bankguarantee.getClient().getId() +
         * "' and bankbgno='" + bankguarantee.getBankbgno() + "' and escmProposalAttr.id = '" +
         * bankguarantee.getEscmProposalAttr().getId() + "' "); if (bankguaranteedet.list().size() >
         * 0) { throw new OBException(OBMessageUtils.messageBD("Escm_BankBgNo")); } } else {
         */
        if (BankGuaranteeDetailEventDAO.checkUniqueBGNo(bankguarantee)) {
          throw new OBException(OBMessageUtils.messageBD("Escm_BankBgNo"));
          // }
        }
      }

      // Task No. 0005277
      if ((event.getCurrentState(bgstartdateH) != null
          && (!event.getCurrentState(bgstartdateH).equals(event.getPreviousState(bgstartdateH))))
          || (event.getCurrentState(expiredateh) != null && (!event.getCurrentState(expiredateh)
              .equals(event.getPreviousState(expiredateh))))) {
        if (bankguarantee.getBgstartdateh().compareTo(bankguarantee.getExpirydateh()) > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BGStatdatGrtthanExpdate"));
        }
        if (bankguarantee.getEscmBgworkbench().getEscmProposalmgmt() != null
            && bankguarantee.getEscmBgworkbench().getBidNo() != null) {
          Boolean Result = BankGuaranteeDetailEventDAO.validBGDatewithOpenEnvel(bankguarantee,
              bankguarantee.getEscmBgworkbench().getEscmProposalmgmt());
          if (Result) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_BGExpDatCompWithOpenEnv"));
          }
        }
      }
      if (bankguarantee.getBankguaranteetype().equals("IBG")) {
        if (event.getCurrentState(intialBg) != null
            && (!event.getCurrentState(intialBg).equals(event.getPreviousState(intialBg)))) {
          BankGuaranteeDetailEventDAO.validateBGValue(bankguarantee);
        }
      }

      /** check document no is mandatory when document type selected as P */
      /*
       * Task No: 5336 if (event.getCurrentState(documentNo) == null) { if
       * (bankguarantee.getDocumentType() != null && bankguarantee.getDocumentType().equals("P")) {
       * if (bankguarantee.getDocumentNo() == null) { throw new
       * OBException(OBMessageUtils.messageBD("ESCM_BGDocNoNotEmpty")); } } }
       */

      /** check bank percentage is more than 100 if multi bank check for same documentno */
      /*
       * Task No: 0005277 if (bankguarantee.getBankPercentage() != null &&
       * (bankguarantee.getDocumentNo() != null || bankguarantee.getSalesOrder() != null ||
       * bankguarantee .getEscmProposalmgmt() != null) && ((event.getCurrentState(bankpercent) !=
       * null && (!event.getCurrentState(bankpercent) .equals(event.getPreviousState(bankpercent))))
       * || (event.getCurrentState(documentNo) != null && (!event
       * .getCurrentState(documentNo).equals(event.getPreviousState(documentNo)))))) { if
       * (!BankGuaranteeDetailEventDAO.checkBankPercentage(bankguarantee)) { throw new
       * OBException(OBMessageUtils.messageBD("ESCM_BankPercentage_Validation")); } }
       */

      /** chk unique Constraint for documentno and bank name */
      if ((event.getCurrentState(bankName) != null
          && !event.getCurrentState(bankName).equals(event.getPreviousState(bankName)))) {
        if (BankGuaranteeDetailEventDAO.checksameDocNowithSameBank(bankguarantee)) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_SameDocNOWithSameBankPre"));
        }
      }
      if (event.getCurrentState(initialbg) != null
          && !event.getCurrentState(initialbg).equals(event.getPreviousState(initialbg))) {
        if (bankguarantee.getInitialbg() != null) {
          if (new BigDecimal(bankguarantee.getInitialbg()).compareTo(new BigDecimal(100)) > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_BGRateMorThanHundPer"));
          }
        }
      }

    } catch (OBException e) {
      log.debug("exception while updating BankGuaranteeDetails" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.debug("exception while updating BankGuaranteeDetails" + e);
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
      OBContext.setAdminMode();
      Escmbankguaranteedetail bankguarantee = (Escmbankguaranteedetail) event.getTargetInstance();

      if (bankguarantee.getEscmBgworkbench() != null
          && bankguarantee.getEscmBgworkbench().getBghdstatus() != null
          && bankguarantee.getEscmBgworkbench().getBghdstatus().equals("CO")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_BGCantDeleteLines"));
      }

    } catch (OBException e) {
      log.error("Exception while updating BG Extension:" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while updating BG Extension:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void validateHeaderFields(Escmbankguaranteedetail bankguarantee) {

    try {
      String foreignBnkName = bankguarantee.getForeignBankName() == null ? ""
          : bankguarantee.getForeignBankName();

      // check Bank BgNo has special characters
      // String field = "";
      // java.util.regex.Matcher m = null;
      Pattern p = Pattern.compile("[^A-Za-z0-9\\s]");
      log.debug("validateHeaderFields");
      if (p.matcher(foreignBnkName).find()) {

        // field = " in Foreign Bank Name";

        throw new OBException(OBMessageUtils.messageBD("ESCM_SplCharNotAllowed"));
      }
    } catch (OBException e) {
      log.debug("exception while bg workbench" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.debug("exception while validateHeaderFields" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
  }
}
